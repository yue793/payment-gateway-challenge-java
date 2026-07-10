package com.checkout.payment.gateway.client;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.checkout.payment.gateway.exception.BankCommunicationException;
import com.checkout.payment.gateway.model.bank.AcquiringBankRequest;
import com.checkout.payment.gateway.model.bank.AcquiringBankResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Integration tests for AcquiringBankClient.
 * 
 * These tests verify communication with the real bank simulator running via docker-compose.
 * Tests are automatically skipped if the bank simulator is not available.
 * 
 * To run these tests with docker-compose:
 * 
 * $ docker-compose up
 * $ ./gradlew test --tests *Integration*
 */
@SpringBootTest
@TestPropertySource(properties = "acquiring-bank.url=http://localhost:8080")
class AcquiringBankClientIntegrationTest {

  @Autowired
  private AcquiringBankClient client;

  @Autowired
  private RestTemplate restTemplate;

  private AcquiringBankRequest paymentRequest;

  @BeforeAll
  static void checkBankAvailability() {
    // Skip all tests in this class if bank simulator is not running
    // The simulator requires a proper POST request, so we just check basic connectivity
    try {
      RestTemplate template = new RestTemplate();
      // Try to connect to the simulator's root - will fail with 404 if running, connection error if not
      try {
        template.getForObject("http://localhost:8080/", String.class);
      } catch (Exception e) {
        // If we get an HTTP error (404, etc), simulator is running; if connection refused, it's not
        if (e instanceof org.springframework.web.client.ResourceAccessException) {
          throw e; // Re-throw connection errors
        }
        // Otherwise, server is responding (even with 404) so it's running
      }
    } catch (Exception e) {
      assumeTrue(false, "Bank simulator not available at http://localhost:8080. "
          + "Start docker-compose up and try again. Error: " + e.getClass().getSimpleName());
    }
  }

  @BeforeEach
  void setUp() {
    paymentRequest = new AcquiringBankRequest(
        "4111111111111111",  // Card ending in 1 (odd) will be authorized
        "12/2025",
        "USD",
        10000,
        123
    );
  }

  /**
   * Test successful payment authorization.
   * 
   * Scenario: Card ending with odd digit should be authorized by bank simulator.
   * Verification: Response indicates authorized with auth code.
   */
  @Test
  void testSuccessfulPaymentAuthorization() {
    AcquiringBankResponse response = client.submitPayment(paymentRequest);

    assert response != null : "Response should not be null";
    assert response.isAuthorised() : "Payment should be authorized";
    assert response.getAuthCode() != null : "Auth code should be present";
    assert response.getStatusCode().equals("000") : "Status code should be 000";
  }

  /**
   * Test declined payment.
   * 
   * Scenario: Card ending with even digit should be declined by bank simulator.
   * Verification: Response indicates declined with empty auth code.
   */
  @Test
  void testDeclinedPayment() {
    paymentRequest.setCardNumber("4111111111111112");  // Ending in 2 (even) will decline

    AcquiringBankResponse response = client.submitPayment(paymentRequest);

    assert response != null : "Response should not be null";
    assert !response.isAuthorised() : "Payment should be declined";
    assert response.getAuthCode().isEmpty() : "Auth code should be empty";
  }

  /**
   * Test server error with retry exhaustion.
   * 
   * Scenario: Card ending in 0 triggers 503 Service Unavailable from bank simulator.
   * Verification: Client retries until exhausted, throws retryable exception.
   */
  @Test
  void testServerError_ExhaustsRetries() {
    paymentRequest.setCardNumber("4111111111111110");  // Ending in 0 triggers 503

    try {
      client.submitPayment(paymentRequest);
      assert false : "Should have thrown BankCommunicationException";
    } catch (BankCommunicationException e) {
      assert e.isRetryable() : "503 errors should be retryable";
    }
  }
}

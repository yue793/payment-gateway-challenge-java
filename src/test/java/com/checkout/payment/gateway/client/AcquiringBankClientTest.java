package com.checkout.payment.gateway.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.exception.BankCommunicationException;
import com.checkout.payment.gateway.exception.BankIntegrationException;
import com.checkout.payment.gateway.model.bank.AcquiringBankRequest;
import com.checkout.payment.gateway.model.bank.AcquiringBankResponse;
import java.net.ConnectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for AcquiringBankClient.
 * 
 * Tests verify:
 * - Successful payment authorization and declined payments
 * - Automatic retries for server errors (5xx) and connection failures
 * - No retries for validation errors (4xx)
 * - Proper exception handling
 */
@ExtendWith(MockitoExtension.class)
class AcquiringBankClientTest {

  @Mock
  private RestTemplate restTemplate;

  private AcquiringBankClientConfig config;
  private AcquiringBankRetryPolicy retryPolicy;
  private AcquiringBankClient client;

  private AcquiringBankRequest validPaymentRequest;

  @BeforeEach
  void setUp() {
    config = new AcquiringBankClientConfig();
    config.setBankBaseUrl("http://test-bank");
    config.setPaymentEndpoint("/payments");

    retryPolicy = new AcquiringBankRetryPolicy(2, 10, 100, 2.0);
    client = new AcquiringBankClient(restTemplate, config, retryPolicy);

    validPaymentRequest = new AcquiringBankRequest(
        "4111111111111111",
        "12/2025",
        "USD",
        10000,
        123
    );
  }

  @Test
  void testSuccessfulPayment_Authorized() {
    // Arrange
    AcquiringBankResponse successResponse = new AcquiringBankResponse(true, "AUTH123", "000");
    ResponseEntity<AcquiringBankResponse> responseEntity = 
        new ResponseEntity<>(successResponse, HttpStatus.OK);
    when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
        .thenReturn(responseEntity);

    // Act
    AcquiringBankResponse response = client.submitPayment(validPaymentRequest);

    // Assert
    assertNotNull(response);
    assertTrue(response.isAuthorised());
    assertEquals("AUTH123", response.getAuthCode());
    verify(restTemplate, times(1)).postForEntity(anyString(), any(), any(Class.class));
  }

  @Test
  void testSuccessfulPayment_Declined() {
    // Arrange
    AcquiringBankResponse declinedResponse = new AcquiringBankResponse(false, "", "102");
    ResponseEntity<AcquiringBankResponse> responseEntity = 
        new ResponseEntity<>(declinedResponse, HttpStatus.OK);
    when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
        .thenReturn(responseEntity);

    // Act
    AcquiringBankResponse response = client.submitPayment(validPaymentRequest);

    // Assert
    assertNotNull(response);
    assertFalse(response.isAuthorised());
    verify(restTemplate, times(1)).postForEntity(anyString(), any(), any(Class.class));
  }

  @Test
  void testValidationError_NoRetry() {
    // Arrange - 400 Bad Request (validation error, not retryable)
    when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

    // Act & Assert
    BankCommunicationException ex = assertThrows(
        BankCommunicationException.class,
        () -> client.submitPayment(validPaymentRequest)
    );

    assertFalse(ex.isRetryable());
    verify(restTemplate, times(1)).postForEntity(anyString(), any(), any(Class.class));
  }

  @Test
  void testServerError_RetriedSuccessfully() {
    // Arrange - First fails with 500, second succeeds
    AcquiringBankResponse successResponse = new AcquiringBankResponse(true, "AUTH456", "000");
    ResponseEntity<AcquiringBankResponse> successEntity = 
        new ResponseEntity<>(successResponse, HttpStatus.OK);

    when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
        .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
        .thenReturn(successEntity);

    // Act
    AcquiringBankResponse response = client.submitPayment(validPaymentRequest);

    // Assert
    assertNotNull(response);
    assertTrue(response.isAuthorised());
    verify(restTemplate, times(2)).postForEntity(anyString(), any(), any(Class.class));
  }

  @Test
  void testConnectionError_RetriedSuccessfully() {
    // Arrange - First fails with connection error, second succeeds
    AcquiringBankResponse successResponse = new AcquiringBankResponse(true, "AUTH789", "000");
    ResponseEntity<AcquiringBankResponse> successEntity = 
        new ResponseEntity<>(successResponse, HttpStatus.OK);

    when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
        .thenThrow(new ResourceAccessException("Connection refused", new ConnectException()))
        .thenReturn(successEntity);

    // Act
    AcquiringBankResponse response = client.submitPayment(validPaymentRequest);

    // Assert
    assertNotNull(response);
    assertTrue(response.isAuthorised());
    verify(restTemplate, times(2)).postForEntity(anyString(), any(), any(Class.class));
  }

  @Test
  void testServerError_ExhaustedRetries() {
    // Arrange - All attempts fail
    when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
        .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

    // Act & Assert
    BankCommunicationException ex = assertThrows(
        BankCommunicationException.class,
        () -> client.submitPayment(validPaymentRequest)
    );

    assertTrue(ex.isRetryable());
    // Verify retries: initial + 2 retries = 3 calls
    verify(restTemplate, times(3)).postForEntity(anyString(), any(), any(Class.class));
  }

  @Test
  void testNullRequest_ThrowsException() {
    // Act & Assert
    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> client.submitPayment(null)
    );

    assertEquals("Payment request cannot be null", ex.getMessage());
    verify(restTemplate, times(0)).postForEntity(anyString(), any(), any(Class.class));
  }

  @Test
  void testUnexpectedError_NoRetry() {
    // Arrange
    when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
        .thenThrow(new RuntimeException("Unexpected error"));

    // Act & Assert
    BankIntegrationException ex = assertThrows(
        BankIntegrationException.class,
        () -> client.submitPayment(validPaymentRequest)
    );

    verify(restTemplate, times(1)).postForEntity(anyString(), any(), any(Class.class));
  }
}

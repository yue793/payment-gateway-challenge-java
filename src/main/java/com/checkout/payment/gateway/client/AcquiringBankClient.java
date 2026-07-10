package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.BankCommunicationException;
import com.checkout.payment.gateway.exception.BankIntegrationException;
import com.checkout.payment.gateway.model.bank.AcquiringBankRequest;
import com.checkout.payment.gateway.model.bank.AcquiringBankResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for communicating with the acquiring bank.
 * 
 * This component handles:
 * - Sending payment requests to the bank
 * - Receiving and parsing bank responses
 * - Handling transient failures with automatic retry logic
 * - Logging (without sensitive data like card numbers or CVV)
 * - Mapping bank errors to appropriate exceptions
 * 
 * The client uses exponential backoff with jitter for retries to ensure robust
 * communication with the bank service.
 */
@Component
public class AcquiringBankClient {
  private static final Logger LOG = LoggerFactory.getLogger(AcquiringBankClient.class);

  private final RestTemplate restTemplate;
  private final AcquiringBankClientConfig config;
  private final AcquiringBankRetryPolicy retryPolicy;

  /**
   * Constructs an AcquiringBankClient with the provided dependencies.
   *
   * @param restTemplate the RestTemplate configured with appropriate timeouts
   * @param config the bank configuration containing the bank URL and endpoint
   * @param retryPolicy the retry policy for handling transient failures
   */
  public AcquiringBankClient(RestTemplate restTemplate, AcquiringBankClientConfig config,
      AcquiringBankRetryPolicy retryPolicy) {
    this.restTemplate = restTemplate;
    this.config = config;
    this.retryPolicy = retryPolicy;
  }

  /**
   * Submits a payment request to the acquiring bank with automatic retry on transient failures.
   * 
   * This method:
   * 1. Validates the request is not null
   * 2. Makes the HTTP request to the bank
   * 3. Handles transient failures (5xx, connection errors) with exponential backoff retry
   * 4. Returns the bank response on success
   * 
   * @param paymentRequest the payment request to send to the bank
   * @return the bank's response containing authorization status and auth code
   * @throws BankCommunicationException if communication fails after all retries
   * @throws BankIntegrationException if an unexpected error occurs
   * @throws IllegalArgumentException if the payment request is null
   */
  public AcquiringBankResponse submitPayment(AcquiringBankRequest paymentRequest) {
    if (paymentRequest == null) {
      throw new IllegalArgumentException("Payment request cannot be null");
    }

    LOG.debug("Submitting payment to acquiring bank - Amount: {}, Currency: {}", 
        paymentRequest.getAmount(), paymentRequest.getCurrency());

    int attemptNumber = 0;
    Exception lastException = null;

    while (attemptNumber <= retryPolicy.getMaxRetries()) {
      try {
        return executePaymentRequest(paymentRequest);
      } catch (BankCommunicationException ex) {
        // Only retry if error is marked as retryable
        if (!ex.isRetryable() || attemptNumber >= retryPolicy.getMaxRetries()) {
          LOG.error("Bank communication failed. Attempt {}/{}", 
              attemptNumber + 1, retryPolicy.getMaxRetries() + 1);
          throw ex;
        }

        lastException = ex;
        long delayMillis = retryPolicy.calculateBackoffMillis(attemptNumber);
        retryPolicy.logRetryAttempt(attemptNumber, delayMillis, ex);

        try {
          retryPolicy.sleep(delayMillis);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new BankCommunicationException("Retry interrupted", ie, false);
        }

        attemptNumber++;
      }
    }

    // Should not reach here, but fail-safe
    throw new BankCommunicationException(
        "Failed after " + (attemptNumber) + " attempts",
        lastException,
        false
    );
  }

  /**
   * Executes a single payment request to the bank without retry logic.
   *
   * @param paymentRequest the payment request to send
   * @return the bank's response
   * @throws BankCommunicationException if the request fails (retryable)
   * @throws BankIntegrationException if an unexpected error occurs
   */
  private AcquiringBankResponse executePaymentRequest(AcquiringBankRequest paymentRequest) {
    try {
      String bankUrl = config.getBankPaymentEndpoint();
      LOG.debug("Sending payment request to: {}", bankUrl);

      ResponseEntity<AcquiringBankResponse> response = restTemplate.postForEntity(
          bankUrl,
          paymentRequest,
          AcquiringBankResponse.class
      );

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        AcquiringBankResponse bankResponse = response.getBody();
        LOG.info("Bank payment processed - Status: {}, AuthCode: {}",
            bankResponse.isAuthorised() ? "AUTHORIZED" : "DECLINED",
            bankResponse.getAuthCode());
        return bankResponse;
      }

      throw new BankIntegrationException(
          "Unexpected response status from bank: " + response.getStatusCode()
      );

    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      // Bank returned error (4xx or 5xx)
      boolean isRetryable = ex.getStatusCode().is5xxServerError();
      LOG.warn("Bank error: {} - {}", ex.getStatusCode(), ex.getMessage());
      throw new BankCommunicationException(
          "Bank error: " + ex.getStatusCode(),
          ex,
          isRetryable
      );

    } catch (ResourceAccessException ex) {
      // Connection/network error - retryable
      LOG.warn("Connection error with bank: {}", ex.getMessage());
      throw new BankCommunicationException(
          "Connection error: " + ex.getMessage(),
          ex,
          true
      );

    } catch (Exception ex) {
      // Unexpected error
      LOG.error("Unexpected error during bank communication", ex);
      throw new BankIntegrationException(
          "Unexpected error: " + ex.getMessage(),
          ex
      );
    }
  }
}

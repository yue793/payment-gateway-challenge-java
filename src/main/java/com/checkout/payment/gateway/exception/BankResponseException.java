package com.checkout.payment.gateway.exception;

/**
 * Exception thrown when the acquiring bank returns an invalid or unparseable response.
 * This indicates a problem with the response format or structure, not with the payment itself.
 * This exception typically should not be retried as the issue is with the response format.
 */
public class BankResponseException extends BankIntegrationException {
  public BankResponseException(String message) {
    super(message);
  }

  public BankResponseException(String message, Throwable cause) {
    super(message, cause);
  }
}

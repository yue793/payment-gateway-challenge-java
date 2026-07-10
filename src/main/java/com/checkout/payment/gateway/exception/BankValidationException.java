package com.checkout.payment.gateway.exception;

/**
 * Exception thrown when the acquiring bank rejects a payment.
 * This exception indicates that the payment request was rejected by the bank
 * and typically should not be retried without modification.
 */
public class BankValidationException extends BankIntegrationException {
  public BankValidationException(String message) {
    super(message);
  }

  public BankValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}

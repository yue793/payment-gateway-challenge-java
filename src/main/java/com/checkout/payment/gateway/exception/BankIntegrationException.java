package com.checkout.payment.gateway.exception;

/**
 * Base exception for all acquiring bank integration failures.
 * This is the parent exception for all bank-related errors in the payment processing flow.
 */
public class BankIntegrationException extends RuntimeException {
  public BankIntegrationException(String message) {
    super(message);
  }

  public BankIntegrationException(String message, Throwable cause) {
    super(message, cause);
  }
}

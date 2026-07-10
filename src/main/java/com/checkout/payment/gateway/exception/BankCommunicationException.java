package com.checkout.payment.gateway.exception;

/**
 * Exception thrown when communication with the acquiring bank fails.
 * This exception is used for transient failures like connection timeouts,
 * network errors, or temporary service unavailability (5xx responses).
 * Such failures are typically retryable.
 */
public class BankCommunicationException extends BankIntegrationException {
  private final boolean retryable;

  public BankCommunicationException(String message, boolean retryable) {
    super(message);
    this.retryable = retryable;
  }

  public BankCommunicationException(String message, Throwable cause, boolean retryable) {
    super(message, cause);
    this.retryable = retryable;
  }

  /**
   * Indicates whether this communication failure can be safely retried.
   *
   * @return true if the operation can be retried, false otherwise
   */
  public boolean isRetryable() {
    return retryable;
  }
}

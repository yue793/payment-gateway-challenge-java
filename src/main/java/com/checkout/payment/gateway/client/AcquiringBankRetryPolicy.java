package com.checkout.payment.gateway.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements retry logic for transient failures in bank communication.
 * 
 * This policy uses exponential backoff with jitter to avoid thundering herd problems.
 * Retries are only attempted for idempotent, retryable operations.
 */
public class AcquiringBankRetryPolicy {
  private static final Logger LOG = LoggerFactory.getLogger(AcquiringBankRetryPolicy.class);

  private final int maxRetries;
  private final long initialBackoffMillis;
  private final long maxBackoffMillis;
  private final double backoffMultiplier;

  /**
   * Constructs a retry policy with default settings.
   * Default: 3 retries, 100ms initial backoff, 5000ms max backoff, 2x multiplier
   */
  public AcquiringBankRetryPolicy() {
    this(3, 100, 5000, 2.0);
  }

  /**
   * Constructs a retry policy with custom settings.
   *
   * @param maxRetries maximum number of retry attempts (must be >= 0)
   * @param initialBackoffMillis initial backoff delay in milliseconds
   * @param maxBackoffMillis maximum backoff delay in milliseconds
   * @param backoffMultiplier multiplier for exponential backoff (must be > 1.0)
   */
  public AcquiringBankRetryPolicy(int maxRetries, long initialBackoffMillis, 
      long maxBackoffMillis, double backoffMultiplier) {
    if (maxRetries < 0) {
      throw new IllegalArgumentException("maxRetries must be >= 0");
    }
    if (backoffMultiplier <= 1.0) {
      throw new IllegalArgumentException("backoffMultiplier must be > 1.0");
    }
    this.maxRetries = maxRetries;
    this.initialBackoffMillis = initialBackoffMillis;
    this.maxBackoffMillis = maxBackoffMillis;
    this.backoffMultiplier = backoffMultiplier;
  }

  /**
   * Calculates the backoff delay for a given attempt number using exponential backoff with jitter.
   *
   * @param attemptNumber the current attempt number (0-based)
   * @return the delay in milliseconds before the next retry
   */
  public long calculateBackoffMillis(int attemptNumber) {
    if (attemptNumber < 0 || attemptNumber > maxRetries) {
      throw new IllegalArgumentException("Invalid attempt number: " + attemptNumber);
    }

    // Calculate exponential backoff: initialBackoff * (multiplier ^ attemptNumber)
    long exponentialBackoff = (long) (initialBackoffMillis * Math.pow(backoffMultiplier, attemptNumber));
    
    // Cap at max backoff
    long cappedBackoff = Math.min(exponentialBackoff, maxBackoffMillis);
    
    // Add jitter: random value between 0 and cappedBackoff to avoid thundering herd
    long jitter = (long) (Math.random() * cappedBackoff);
    
    return jitter;
  }

  /**
   * Sleeps for the specified duration, handling InterruptedException gracefully.
   *
   * @param delayMillis the duration to sleep in milliseconds
   * @throws InterruptedException if the current thread is interrupted
   */
  public void sleep(long delayMillis) throws InterruptedException {
    if (delayMillis > 0) {
      Thread.sleep(delayMillis);
    }
  }

  /**
   * Logs the retry attempt with useful information (without sensitive data).
   *
   * @param attemptNumber the current attempt number (0-based)
   * @param delayMillis the delay before the next retry in milliseconds
   * @param lastException the exception that triggered the retry
   */
  public void logRetryAttempt(int attemptNumber, long delayMillis, Exception lastException) {
    LOG.warn(
        "Retrying bank communication - Attempt {}/{}, Delay: {}ms, Reason: {}",
        attemptNumber + 1,
        maxRetries + 1,
        delayMillis,
        lastException.getMessage()
    );
  }

  public int getMaxRetries() {
    return maxRetries;
  }
}

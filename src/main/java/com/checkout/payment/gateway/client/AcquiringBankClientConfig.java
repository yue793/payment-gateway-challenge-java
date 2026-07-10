package com.checkout.payment.gateway.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the acquiring bank integration.
 * 
 * This class manages all configuration properties required for bank communication,
 * including endpoints, credentials, and retry policies.
 */
@Configuration
public class AcquiringBankClientConfig {
  
  @Value("${acquiring-bank.url:http://localhost:8080}")
  private String bankBaseUrl;

  @Value("${acquiring-bank.payment-endpoint:/payments}")
  private String paymentEndpoint;

  @Value("${acquiring-bank.retry.max-retries:3}")
  private int maxRetries;

  @Value("${acquiring-bank.retry.initial-backoff-millis:100}")
  private long initialBackoffMillis;

  @Value("${acquiring-bank.retry.max-backoff-millis:5000}")
  private long maxBackoffMillis;

  @Value("${acquiring-bank.retry.backoff-multiplier:2.0}")
  private double backoffMultiplier;

  /**
   * Gets the full bank payment endpoint URL.
   *
   * @return the complete URL for the bank payment endpoint
   */
  public String getBankPaymentEndpoint() {
    return bankBaseUrl + paymentEndpoint;
  }

  /**
   * Creates the retry policy bean with configured values.
   *
   * @return a configured AcquiringBankRetryPolicy instance
   */
  @Bean
  public AcquiringBankRetryPolicy acquiringBankRetryPolicy() {
    return new AcquiringBankRetryPolicy(
        maxRetries,
        initialBackoffMillis,
        maxBackoffMillis,
        backoffMultiplier
    );
  }

  // Getters for configuration properties
  public String getBankBaseUrl() {
    return bankBaseUrl;
  }

  public void setBankBaseUrl(String bankBaseUrl) {
    this.bankBaseUrl = bankBaseUrl;
  }

  public String getPaymentEndpoint() {
    return paymentEndpoint;
  }

  public void setPaymentEndpoint(String paymentEndpoint) {
    this.paymentEndpoint = paymentEndpoint;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public long getInitialBackoffMillis() {
    return initialBackoffMillis;
  }

  public void setInitialBackoffMillis(long initialBackoffMillis) {
    this.initialBackoffMillis = initialBackoffMillis;
  }

  public long getMaxBackoffMillis() {
    return maxBackoffMillis;
  }

  public void setMaxBackoffMillis(long maxBackoffMillis) {
    this.maxBackoffMillis = maxBackoffMillis;
  }

  public double getBackoffMultiplier() {
    return backoffMultiplier;
  }

  public void setBackoffMultiplier(double backoffMultiplier) {
    this.backoffMultiplier = backoffMultiplier;
  }
}

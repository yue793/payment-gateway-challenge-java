package com.checkout.payment.gateway.constants;

import java.util.Set;

public final class PaymentValidationConstants {

  public static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "GBP", "EUR");
  public static final String CARD_NUMBER_PATTERN = "\\d{14,19}";
  public static final String CVV_PATTERN = "\\d{3,4}";
  public static final int MIN_EXPIRY_MONTH = 1;
  public static final int MAX_EXPIRY_MONTH = 12;

  private PaymentValidationConstants() {
  }
}
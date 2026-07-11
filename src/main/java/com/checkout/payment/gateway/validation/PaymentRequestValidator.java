package com.checkout.payment.gateway.validation;

import static com.checkout.payment.gateway.constants.PaymentValidationConstants.CARD_NUMBER_PATTERN;
import static com.checkout.payment.gateway.constants.PaymentValidationConstants.CVV_PATTERN;
import static com.checkout.payment.gateway.constants.PaymentValidationConstants.MAX_EXPIRY_MONTH;
import static com.checkout.payment.gateway.constants.PaymentValidationConstants.MIN_EXPIRY_MONTH;
import static com.checkout.payment.gateway.constants.PaymentValidationConstants.SUPPORTED_CURRENCIES;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.time.DateTimeException;
import java.time.Year;
import java.time.YearMonth;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestValidator {

  public boolean isValid(PostPaymentRequest paymentRequest) {
    if (paymentRequest == null) {
      return false;
    }

    if (paymentRequest.getCardNumber() == null
        || !paymentRequest.getCardNumber().matches(CARD_NUMBER_PATTERN)) {
      return false;
    }

    if (paymentRequest.getExpiryMonth() == null
        || paymentRequest.getExpiryMonth() < MIN_EXPIRY_MONTH
        || paymentRequest.getExpiryMonth() > MAX_EXPIRY_MONTH) {
      return false;
    }

    if (paymentRequest.getExpiryYear() == null
        || paymentRequest.getExpiryYear() < Year.now().getValue()) {
      return false;
    }

    try {
      if (!YearMonth.of(paymentRequest.getExpiryYear(), paymentRequest.getExpiryMonth())
          .isAfter(YearMonth.now())) {
        return false;
      }
    } catch (DateTimeException ex) {
      return false;
    }

    if (paymentRequest.getCurrency() == null
        || !SUPPORTED_CURRENCIES.contains(paymentRequest.getCurrency().toUpperCase(Locale.ROOT))) {
      return false;
    }

    if (paymentRequest.getAmount() == null || paymentRequest.getAmount() <= 0) {
      return false;
    }

    return paymentRequest.getCvv() != null && paymentRequest.getCvv().matches(CVV_PATTERN);
  }
}
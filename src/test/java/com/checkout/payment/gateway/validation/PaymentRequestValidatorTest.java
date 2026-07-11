package com.checkout.payment.gateway.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.time.Year;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentRequestValidatorTest {

  private PaymentRequestValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PaymentRequestValidator();
  }

  @Test
  void isValidReturnsTrueForValidRequest() {
    PostPaymentRequest request = validRequest();
    assertTrue(validator.isValid(request));
  }

  @Test
  void isValidReturnsFalseForUnsupportedCurrency() {
    PostPaymentRequest request = validRequest();
    request.setCurrency("AUD");
    assertFalse(validator.isValid(request));
  }

  @Test
  void isValidReturnsFalseForInvalidCardNumber() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber("4111ABC");
    assertFalse(validator.isValid(request));
  }

  @Test
  void isValidReturnsFalseForExpiredCard() {
    PostPaymentRequest request = validRequest();
    request.setExpiryYear(Year.now().getValue() - 1);
    assertFalse(validator.isValid(request));
  }

  private PostPaymentRequest validRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111111");
    request.setExpiryMonth(12);
    request.setExpiryYear(Year.now().getValue() + 1);
    request.setCurrency("USD");
    request.setAmount(1050);
    request.setCvv("123");
    return request;
  }
}
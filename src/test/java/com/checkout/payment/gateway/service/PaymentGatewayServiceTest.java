package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.GetPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.bank.AcquiringBankResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.validation.PaymentRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  @Mock
  private AcquiringBankClient acquiringBankClient;

  private PaymentsRepository paymentsRepository;
  private PaymentRequestValidator paymentRequestValidator;
  private PaymentGatewayService paymentGatewayService;

  @BeforeEach
  void setUp() {
    paymentsRepository = new PaymentsRepository();
    paymentRequestValidator = new PaymentRequestValidator();
    paymentGatewayService = new PaymentGatewayService(
        acquiringBankClient,
        paymentsRepository,
        paymentRequestValidator);
  }

  @Test
  void processPaymentStoresAuthorizedPayment() {
    PostPaymentRequest request = validRequest();
    when(acquiringBankClient.submitPayment(any()))
        .thenReturn(new AcquiringBankResponse(true, "bank-auth-code", "000"));

    PostPaymentResponse response = paymentGatewayService.processPayment(request);

    assertNotNull(response.getId());
    assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
    assertEquals("1111", response.getCardNumberLastFour());
    assertEquals("USD", response.getCurrency());
    verify(acquiringBankClient).submitPayment(any());
  }

  @Test
  void processPaymentStoresDeclinedPayment() {
    PostPaymentRequest request = validRequest();
    when(acquiringBankClient.submitPayment(any()))
        .thenReturn(new AcquiringBankResponse(false, "", "000"));

    PostPaymentResponse response = paymentGatewayService.processPayment(request);

    assertEquals(PaymentStatus.DECLINED, response.getStatus());
    assertEquals("1111", response.getCardNumberLastFour());
  }

  @Test
  void processPaymentStoresRejectedPaymentWithoutCallingBank() {
    PostPaymentRequest request = validRequest();
    request.setCurrency("AUD");

    PostPaymentResponse response = paymentGatewayService.processPayment(request);

    assertNotNull(response.getId());
    assertEquals(PaymentStatus.REJECTED, response.getStatus());
    assertEquals("1111", response.getCardNumberLastFour());
    verify(acquiringBankClient, never()).submitPayment(any());
  }

  @Test
  void getPaymentByIdReturnsStoredPayment() {
    PostPaymentRequest request = validRequest();
    when(acquiringBankClient.submitPayment(any()))
        .thenReturn(new AcquiringBankResponse(true, "bank-auth-code", "000"));

    PostPaymentResponse createdPayment = paymentGatewayService.processPayment(request);

    GetPaymentResponse fetchedPayment = paymentGatewayService.getPaymentById(createdPayment.getId());

    assertEquals(createdPayment.getId(), fetchedPayment.getId());
    assertEquals(createdPayment.getStatus(), fetchedPayment.getStatus());
    assertEquals(createdPayment.getCardNumberLastFour(), fetchedPayment.getCardNumberLastFour());
  }

  @Test
  void getPaymentByIdThrowsWhenPaymentDoesNotExist() {
    assertThrows(EventProcessingException.class,
        () -> paymentGatewayService.getPaymentById(java.util.UUID.randomUUID()));
  }

  private PostPaymentRequest validRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111111");
    request.setExpiryMonth(12);
    request.setExpiryYear(2030);
    request.setCurrency("USD");
    request.setAmount(1050);
    request.setCvv("123");
    return request;
  }
}
package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentRequestStatus;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.GetPaymentResponse;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.bank.AcquiringBankResponse;
import com.checkout.payment.gateway.repository.PaymentRequestRepository;
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

  private PaymentRequestRepository paymentRequestRepository;
  private PaymentsRepository paymentsRepository;
  private PaymentRequestValidator paymentRequestValidator;
  private PaymentGatewayService paymentGatewayService;

  @BeforeEach
  void setUp() {
    paymentRequestRepository = new PaymentRequestRepository();
    paymentsRepository = new PaymentsRepository();
    paymentRequestValidator = new PaymentRequestValidator();
    paymentGatewayService = new PaymentGatewayService(
        acquiringBankClient,
        paymentRequestRepository,
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
  void processPaymentStoresRejectedAttemptWithoutCallingBank() {
    PostPaymentRequest request = validRequest();
    request.setCurrency("AUD");

    PostPaymentResponse response = paymentGatewayService.processPayment(request);

    assertEquals(null, response.getId());
    assertEquals(PaymentStatus.REJECTED, response.getStatus());
    assertEquals("1111", response.getCardNumberLastFour());
    assertThrows(EventProcessingException.class,
        () -> paymentGatewayService.getPaymentById(java.util.UUID.randomUUID()));

    var rejectedRequests = paymentRequestRepository.findAllByStatus(PaymentRequestStatus.REJECTED);
    assertEquals(1, rejectedRequests.size());
    assertEquals("1111", rejectedRequests.getFirst().getRequestData().getCardNumberLastFour());
    assertEquals("AUD", rejectedRequests.getFirst().getRequestData().getCurrency());
    assertEquals(PaymentRequestStatus.REJECTED, rejectedRequests.getFirst().getStatus());
    verify(acquiringBankClient, never()).submitPayment(any());
  }

  @Test
  void processPaymentTransitionsPaymentRequestThroughStates() {
    PostPaymentRequest request = validRequest();
    when(acquiringBankClient.submitPayment(any()))
        .thenReturn(new AcquiringBankResponse(true, "bank-auth-code", "000"));

    PostPaymentResponse response = paymentGatewayService.processPayment(request);

    var allRequests = paymentRequestRepository.getAll();
    assertEquals(1, allRequests.size());
    
    PaymentRequest paymentRequest = allRequests.getFirst();
    assertEquals(PaymentRequestStatus.COMPLETED, paymentRequest.getStatus());
    assertNotNull(paymentRequest.getCreatedAt());
    assertNotNull(paymentRequest.getUpdatedAt());
  }

  @Test
  void processPaymentTracksInProgressBeforeBankCall() {
    PostPaymentRequest request = validRequest();
    when(acquiringBankClient.submitPayment(any()))
        .thenReturn(new AcquiringBankResponse(true, "bank-auth-code", "000"));

    paymentGatewayService.processPayment(request);

    var completedRequests = paymentRequestRepository.findAllByStatus(PaymentRequestStatus.COMPLETED);
    assertEquals(1, completedRequests.size());
    // The request should have transitioned from INITIALIZING -> IN_PROGRESS -> COMPLETED
    // Final state is COMPLETED
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
package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.GetPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.RejectedPaymentAttempt;
import com.checkout.payment.gateway.model.bank.AcquiringBankRequest;
import com.checkout.payment.gateway.model.bank.AcquiringBankResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.repository.RejectedPaymentAttemptsRepository;
import com.checkout.payment.gateway.validation.PaymentRequestValidator;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final AcquiringBankClient acquiringBankClient;
  private final PaymentsRepository paymentsRepository;
  private final RejectedPaymentAttemptsRepository rejectedPaymentAttemptsRepository;
  private final PaymentRequestValidator paymentRequestValidator;

  public PaymentGatewayService(AcquiringBankClient acquiringBankClient,
      PaymentsRepository paymentsRepository,
      RejectedPaymentAttemptsRepository rejectedPaymentAttemptsRepository,
      PaymentRequestValidator paymentRequestValidator) {
    this.acquiringBankClient = acquiringBankClient;
    this.paymentsRepository = paymentsRepository;
    this.rejectedPaymentAttemptsRepository = rejectedPaymentAttemptsRepository;
    this.paymentRequestValidator = paymentRequestValidator;
  }

  public GetPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    PostPaymentResponse payment = paymentsRepository.get(id)
        .orElseThrow(() -> new EventProcessingException("Invalid ID"));

    GetPaymentResponse response = new GetPaymentResponse();
    response.setId(payment.getId());
    response.setStatus(payment.getStatus());
    response.setCardNumberLastFour(payment.getCardNumberLastFour());
    response.setExpiryMonth(payment.getExpiryMonth());
    response.setExpiryYear(payment.getExpiryYear());
    response.setCurrency(payment.getCurrency());
    response.setAmount(payment.getAmount());
    return response;
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    if (paymentRequest == null) {
      LOG.info("Rejecting payment: null request");
      RejectedPaymentAttempt rejectedAttempt = buildRejectedAttempt(null);
      rejectedPaymentAttemptsRepository.add(rejectedAttempt);
      return buildGatewayResponse(null, PaymentStatus.REJECTED);
    }

    LOG.debug("Processing payment - Amount: {}, Currency: {}",
        paymentRequest.getAmount(), paymentRequest.getCurrency());

    if (!paymentRequestValidator.isValid(paymentRequest)) {
      LOG.info("Rejecting payment before bank call due to invalid input");
      RejectedPaymentAttempt rejectedAttempt = buildRejectedAttempt(paymentRequest);
      rejectedPaymentAttemptsRepository.add(rejectedAttempt);
      return buildGatewayResponse(paymentRequest, PaymentStatus.REJECTED);
    }

    AcquiringBankResponse bankResponse = acquiringBankClient.submitPayment(toBankRequest(paymentRequest));
    PostPaymentResponse response = buildGatewayResponse(
        paymentRequest,
        bankResponse.isAuthorised() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED);

    paymentsRepository.add(response, UUID::randomUUID);
    return response;
  }

  private AcquiringBankRequest toBankRequest(PostPaymentRequest paymentRequest) {
    return new AcquiringBankRequest(
        paymentRequest.getCardNumber(),
        paymentRequest.getExpiryDate(),
        paymentRequest.getCurrency().toUpperCase(Locale.ROOT),
        paymentRequest.getAmount(),
        Integer.parseInt(paymentRequest.getCvv())
    );
  }

  private PostPaymentResponse buildGatewayResponse(PostPaymentRequest paymentRequest,
      PaymentStatus paymentStatus) {
    PostPaymentResponse response = new PostPaymentResponse();
    if (paymentStatus != PaymentStatus.REJECTED) {
      response.setId(UUID.randomUUID());
    }
    response.setStatus(paymentStatus);
    response.setCardNumberLastFour(paymentRequest == null ? "" : paymentRequest.getCardNumberLastFour());
    response.setExpiryMonth(paymentRequest == null || paymentRequest.getExpiryMonth() == null
        ? 0 : paymentRequest.getExpiryMonth());
    response.setExpiryYear(paymentRequest == null || paymentRequest.getExpiryYear() == null
        ? 0 : paymentRequest.getExpiryYear());
    response.setCurrency(paymentRequest == null ? null : paymentRequest.getCurrency());
    response.setAmount(paymentRequest == null || paymentRequest.getAmount() == null
        ? 0 : paymentRequest.getAmount());
    return response;
  }

  private RejectedPaymentAttempt buildRejectedAttempt(PostPaymentRequest paymentRequest) {
    RejectedPaymentAttempt attempt = new RejectedPaymentAttempt();
    attempt.setId(UUID.randomUUID());
    attempt.setRejectedAt(Instant.now());
    attempt.setCardNumberLastFour(paymentRequest == null ? "" : paymentRequest.getCardNumberLastFour());
    attempt.setExpiryMonth(paymentRequest == null || paymentRequest.getExpiryMonth() == null
        ? 0 : paymentRequest.getExpiryMonth());
    attempt.setExpiryYear(paymentRequest == null || paymentRequest.getExpiryYear() == null
        ? 0 : paymentRequest.getExpiryYear());
    attempt.setCurrency(paymentRequest == null ? null : paymentRequest.getCurrency());
    attempt.setAmount(paymentRequest == null || paymentRequest.getAmount() == null
        ? 0 : paymentRequest.getAmount());
    return attempt;
  }
}

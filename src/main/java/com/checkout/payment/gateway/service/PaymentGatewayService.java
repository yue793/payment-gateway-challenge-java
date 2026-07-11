package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentRequestStatus;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.GetPaymentResponse;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.bank.AcquiringBankRequest;
import com.checkout.payment.gateway.model.bank.AcquiringBankResponse;
import com.checkout.payment.gateway.repository.PaymentRequestRepository;
import com.checkout.payment.gateway.repository.PaymentsRepository;
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
  private final PaymentRequestRepository paymentRequestRepository;
  private final PaymentsRepository paymentsRepository;
  private final PaymentRequestValidator paymentRequestValidator;

  public PaymentGatewayService(AcquiringBankClient acquiringBankClient,
      PaymentRequestRepository paymentRequestRepository,
      PaymentsRepository paymentsRepository,
      PaymentRequestValidator paymentRequestValidator) {
    this.acquiringBankClient = acquiringBankClient;
    this.paymentRequestRepository = paymentRequestRepository;
    this.paymentsRepository = paymentsRepository;
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
    UUID requestId = UUID.randomUUID();

    // Step 1: Create and save request in INITIALIZING state
    PaymentRequest pr = new PaymentRequest();
    pr.setId(requestId);
    pr.setStatus(PaymentRequestStatus.INITIALIZING);
    pr.setRequestData(paymentRequest);
    pr.setCreatedAt(Instant.now());
    paymentRequestRepository.save(pr);

    if (paymentRequest == null) {
      LOG.info("Rejecting payment request {}: null request", requestId);
      pr.setStatus(PaymentRequestStatus.REJECTED);
      pr.setRejectionReason("Null payment request");
      paymentRequestRepository.save(pr);
      return buildGatewayResponse(null, PaymentStatus.REJECTED);
    }

    LOG.debug("Processing payment request {} - Amount: {}, Currency: {}",
        requestId, paymentRequest.getAmount(), paymentRequest.getCurrency());

    // Step 2: Validate and reject if invalid
    if (!paymentRequestValidator.isValid(paymentRequest)) {
      LOG.info("Rejecting payment request {}: validation failed", requestId);
      pr.setStatus(PaymentRequestStatus.REJECTED);
      pr.setRejectionReason("Invalid payment request");
      paymentRequestRepository.save(pr);
      return buildGatewayResponse(paymentRequest, PaymentStatus.REJECTED);
    }

    // Step 3: Move to IN_PROGRESS before calling bank
    LOG.debug("Payment request {} moving to IN_PROGRESS, calling acquiring bank", requestId);
    pr.setStatus(PaymentRequestStatus.IN_PROGRESS);
    paymentRequestRepository.save(pr);

    // Step 4: Call acquiring bank
    AcquiringBankResponse bankResponse = acquiringBankClient.submitPayment(toBankRequest(paymentRequest));

    // Step 5: Mark as COMPLETED and create Payment record
    LOG.debug("Payment request {} completed with bank response: authorized={}", requestId, bankResponse.isAuthorised());
    pr.setStatus(PaymentRequestStatus.COMPLETED);
    paymentRequestRepository.save(pr);

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
}


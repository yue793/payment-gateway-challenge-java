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

  public PostPaymentResponse processPayment(PostPaymentRequest postPaymentRequest) {
    UUID requestId = UUID.randomUUID();

    // Step 1: Create and save request in INITIALIZING state
    PaymentRequest pr = new PaymentRequest();
    pr.setId(requestId);
    pr.setStatus(PaymentRequestStatus.INITIALIZING);
    pr.setRequestData(postPaymentRequest);
    pr.setCreatedAt(Instant.now());
    paymentRequestRepository.save(pr);

    LOG.debug("Processing payment request {} - Amount: {}, Currency: {}",
        requestId, postPaymentRequest != null ? postPaymentRequest.getAmount() : "null",
        postPaymentRequest != null ? postPaymentRequest.getCurrency() : "null");

    // Step 2: Validate and reject if invalid or null
    if (postPaymentRequest == null || !paymentRequestValidator.isValid(postPaymentRequest)) {
      String rejectionReason = postPaymentRequest == null ? "Null payment request" : "Invalid payment request";
      LOG.info("Rejecting payment request {}: {}", requestId, rejectionReason);
      pr.setStatus(PaymentRequestStatus.REJECTED);
      pr.setRejectionReason(rejectionReason);
      paymentRequestRepository.save(pr);
      return buildGatewayResponse(postPaymentRequest, PaymentStatus.REJECTED);
    }

    // Step 3: Move to IN_PROGRESS before calling bank
    LOG.debug("Payment request {} moving to IN_PROGRESS, calling acquiring bank", requestId);
    pr.setStatus(PaymentRequestStatus.IN_PROGRESS);
    paymentRequestRepository.save(pr);

    // Step 4: Call acquiring bank
    AcquiringBankResponse bankResponse = acquiringBankClient.submitPayment(toBankRequest(postPaymentRequest));

    // Step 5: Mark as RECEIVED (response received but not yet persisted)
    LOG.debug("Payment request {} received bank response: authorized={}, marking RECEIVED", requestId, bankResponse.isAuthorised());
    pr.setStatus(PaymentRequestStatus.RECEIVED);
    paymentRequestRepository.save(pr);

    // Step 6: Create Payment record and mark as COMPLETED
    PostPaymentResponse response = buildGatewayResponse(
        postPaymentRequest,
        bankResponse.isAuthorised() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED);

    paymentsRepository.add(response, UUID::randomUUID);

    pr.setStatus(PaymentRequestStatus.COMPLETED);
    paymentRequestRepository.save(pr);
    LOG.debug("Payment request {} completed and persisted", requestId);

    return response;
  }

  private AcquiringBankRequest toBankRequest(PostPaymentRequest postPaymentRequest) {
    return new AcquiringBankRequest(
        postPaymentRequest.getCardNumber(),
        postPaymentRequest.getExpiryDate(),
        postPaymentRequest.getCurrency().toUpperCase(Locale.ROOT),
        postPaymentRequest.getAmount(),
        Integer.parseInt(postPaymentRequest.getCvv())
    );
  }

  private PostPaymentResponse buildGatewayResponse(PostPaymentRequest postPaymentRequest,
      PaymentStatus paymentStatus) {
    PostPaymentResponse response = new PostPaymentResponse();
    if (paymentStatus != PaymentStatus.REJECTED) {
      response.setId(UUID.randomUUID());
    }
    response.setStatus(paymentStatus);
    response.setCardNumberLastFour(postPaymentRequest == null ? "" : postPaymentRequest.getCardNumberLastFour());
    response.setExpiryMonth(postPaymentRequest == null || postPaymentRequest.getExpiryMonth() == null
        ? 0 : postPaymentRequest.getExpiryMonth());
    response.setExpiryYear(postPaymentRequest == null || postPaymentRequest.getExpiryYear() == null
        ? 0 : postPaymentRequest.getExpiryYear());
    response.setCurrency(postPaymentRequest == null ? null : postPaymentRequest.getCurrency());
    response.setAmount(postPaymentRequest == null || postPaymentRequest.getAmount() == null
        ? 0 : postPaymentRequest.getAmount());
    return response;
  }
}


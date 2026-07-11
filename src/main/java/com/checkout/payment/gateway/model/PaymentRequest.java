package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentRequestStatus;
import java.time.Instant;
import java.util.UUID;

public class PaymentRequest {

  private UUID id;
  private PaymentRequestStatus status;
  private PostPaymentRequest requestData;
  private Instant createdAt;
  private Instant updatedAt;
  private String rejectionReason;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public PaymentRequestStatus getStatus() {
    return status;
  }

  public void setStatus(PaymentRequestStatus status) {
    this.status = status;
    this.updatedAt = Instant.now();
  }

  public PostPaymentRequest getRequestData() {
    return requestData;
  }

  public void setRequestData(PostPaymentRequest requestData) {
    this.requestData = requestData;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public void setRejectionReason(String rejectionReason) {
    this.rejectionReason = rejectionReason;
  }
}

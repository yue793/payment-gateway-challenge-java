package com.checkout.payment.gateway.enums;

public enum PaymentRequestStatus {
  INITIALIZING("Initializing"),
  REJECTED("Rejected"),
  IN_PROGRESS("InProgress"),
  COMPLETED("Completed");

  private final String name;

  PaymentRequestStatus(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}

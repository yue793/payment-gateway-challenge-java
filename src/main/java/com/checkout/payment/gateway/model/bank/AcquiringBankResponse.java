package com.checkout.payment.gateway.model.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object representing a payment response from the acquiring bank.
 * This DTO encapsulates the response from the bank and is used internally for
 * processing the payment result. It is separate from the API response models
 * to maintain loose coupling between bank communication and API contracts.
 */
public class AcquiringBankResponse {
  @JsonProperty("authorized")
  private boolean authorised;

  @JsonProperty("authorization_code")
  private String authCode;

  @JsonProperty(value = "status_code", required = false)
  private String statusCode = "000";

  /**
   * Constructs an AcquiringBankResponse with the provided details.
   *
   * @param authorised whether the payment was authorized by the bank
   * @param authCode the authorization code returned by the bank
   * @param statusCode the status code indicating the outcome
   */
  public AcquiringBankResponse(boolean authorised, String authCode, String statusCode) {
    this.authorised = authorised;
    this.authCode = authCode;
    this.statusCode = statusCode;
  }

  // Default constructor for deserialization
  public AcquiringBankResponse() {
  }

  public boolean isAuthorised() {
    return authorised;
  }

  public void setAuthorised(boolean authorised) {
    this.authorised = authorised;
  }

  public String getAuthCode() {
    return authCode;
  }

  public void setAuthCode(String authCode) {
    this.authCode = authCode;
  }

  public String getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(String statusCode) {
    this.statusCode = statusCode;
  }

  @Override
  public String toString() {
    return "AcquiringBankResponse{" +
        "authorised=" + authorised +
        ", authCode='" + authCode + '\'' +
        ", statusCode='" + statusCode + '\'' +
        '}';
  }
}

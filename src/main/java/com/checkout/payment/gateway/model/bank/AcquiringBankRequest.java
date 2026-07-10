package com.checkout.payment.gateway.model.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object representing a payment request to the acquiring bank.
 * This DTO is separate from the API's PostPaymentRequest to maintain loose coupling
 * between the external API contract and the bank integration.
 * 
 * This allows the bank integration to evolve independently of changes to our API.
 */
public class AcquiringBankRequest {
  @JsonProperty("card_number")
  private String cardNumber;

  @JsonProperty("expiry_date")
  private String expiryDate;

  private String currency;
  private int amount;
  private int cvv;

  /**
   * Constructs an AcquiringBankRequest with the provided payment details.
   *
   * @param cardNumber the full or partial card number (as expected by the bank)
   * @param expiryDate the card expiry date in MM/YYYY format
   * @param currency the ISO 4217 currency code (e.g., "USD", "GBP")
   * @param amount the payment amount in the smallest currency unit (e.g., cents for USD)
   * @param cvv the card security code
   */
  public AcquiringBankRequest(String cardNumber, String expiryDate, String currency, int amount, int cvv) {
    this.cardNumber = cardNumber;
    this.expiryDate = expiryDate;
    this.currency = currency;
    this.amount = amount;
    this.cvv = cvv;
  }

  // Default constructor for deserialization
  public AcquiringBankRequest() {
  }

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public String getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(String expiryDate) {
    this.expiryDate = expiryDate;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public int getCvv() {
    return cvv;
  }

  public void setCvv(int cvv) {
    this.cvv = cvv;
  }

  @Override
  public String toString() {
    return "AcquiringBankRequest{" +
        "expiryDate='" + expiryDate + '\'' +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        '}';
  }
}

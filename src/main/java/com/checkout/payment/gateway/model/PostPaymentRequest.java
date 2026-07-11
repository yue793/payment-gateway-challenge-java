package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class PostPaymentRequest implements Serializable {

  @JsonProperty("card_number")
  private String cardNumber;

  @JsonProperty("expiry_month")
  private Integer expiryMonth;

  @JsonProperty("expiry_year")
  private Integer expiryYear;

  private String currency;

  private Integer amount;

  private String cvv;

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(Integer expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(Integer expiryYear) {
    this.expiryYear = expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }

  public String getExpiryDate() {
    return String.format("%02d/%d", expiryMonth, expiryYear);
  }

  public String getCardNumberLastFour() {
    if (cardNumber == null || cardNumber.length() < 4) {
      return "";
    }

    return cardNumber.substring(cardNumber.length() - 4);
  }

  @Override
  public String toString() {
    return "PostPaymentRequest{" +
        "cardNumber='****" + (cardNumber == null ? "" : getCardNumberLastFour()) + '\'' +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        '}';
  }
}

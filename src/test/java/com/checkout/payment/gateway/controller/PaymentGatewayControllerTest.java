package com.checkout.payment.gateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(PaymentGatewayController.class)
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private PaymentGatewayService paymentGatewayService;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PaymentResponse payment = new PaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2030);
    payment.setCardNumberLastFour("4321");

    when(paymentGatewayService.getPaymentById(payment.getId())).thenReturn(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payments/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    UUID paymentId = UUID.randomUUID();
    when(paymentGatewayService.getPaymentById(paymentId))
        .thenThrow(new EventProcessingException("Invalid ID"));

    mvc.perform(MockMvcRequestBuilders.get("/payments/" + paymentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Page not found"));
  }

  @Test
  void whenPaymentRequestIsValidThenCreatedPaymentIsReturned() throws Exception {
    PaymentResponse payment = new PaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setCardNumberLastFour("1111");
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2030);
    payment.setCurrency("USD");
    payment.setAmount(1050);

    when(paymentGatewayService.processPayment(any())).thenReturn(payment);

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "4111111111111111",
                  "expiry_month": 12,
                  "expiry_year": 2030,
                  "currency": "USD",
                  "amount": 1050,
                  "cvv": "123"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("1111"));
  }

  @Test
  void whenPaymentRequestIsInvalidThenBadRequestIsReturnedWithoutCallingService() throws Exception {
    PaymentResponse rejectedPayment = new PaymentResponse();
    rejectedPayment.setStatus(PaymentStatus.REJECTED);
    rejectedPayment.setCardNumberLastFour("11");
    rejectedPayment.setExpiryMonth(0);
    rejectedPayment.setExpiryYear(2020);
    rejectedPayment.setCurrency("AUD");
    rejectedPayment.setAmount(0);

    when(paymentGatewayService.processPayment(any())).thenReturn(rejectedPayment);

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "4111ABC",
                  "expiry_month": 0,
                  "expiry_year": 2020,
                  "currency": "AUD",
                  "amount": 0,
                  "cvv": "12"
                }
                """))
        .andExpect(status().isCreated())
              .andExpect(jsonPath("$.status").value("Rejected"))
              .andExpect(jsonPath("$.id").doesNotExist());

    verify(paymentGatewayService).processPayment(any());
  }
}

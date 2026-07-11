package com.checkout.payment.gateway.controller;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "acquiring-bank.url=http://localhost:8080")
class PaymentGatewayApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeAll
  static void checkBankAvailability() {
    try {
      new RestTemplate().getForObject("http://localhost:8080/", String.class);
    } catch (ResourceAccessException ex) {
      assumeTrue(false,
          "Bank simulator not available at http://localhost:8080. Start docker-compose up first.");
    } catch (Exception ignored) {
      // Any HTTP response means simulator is reachable.
    }
  }

  @Test
  void processPayment_Authorized_AndGetById_ReturnsStoredPayment() throws Exception {
    MvcResult postResult = mockMvc.perform(post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validPayload("4111111111111111", "USD", 1050, "123")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("1111"))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(1050))
        .andReturn();

    JsonNode postBody = objectMapper.readTree(postResult.getResponse().getContentAsString());
    String paymentId = postBody.get("id").asText();

    mockMvc.perform(get("/payments/{id}", paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(paymentId))
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("1111"))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(1050));
  }

  @Test
  void processPayment_Declined_ReturnsDeclined() throws Exception {
    mockMvc.perform(post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validPayload("4111111111111112", "USD", 1050, "123")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Declined"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("1112"));
  }

  @Test
  void processPayment_InvalidInput_ReturnsRejectedWithoutBankCall() throws Exception {
    mockMvc.perform(post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validPayload("4111111111111111", "AUD", 1050, "123")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("1111"));
  }

  @Test
  void processPayment_BankUnavailablePath_ReturnsBadGateway() throws Exception {
    mockMvc.perform(post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validPayload("4111111111111110", "USD", 1050, "123")))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Unable to process payment"));
  }

  @Test
  void processPayment_MalformedPayload_ReturnsBadRequest() throws Exception {
    mockMvc.perform(post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"card_number\":\"4111111111111111\",\"expiry_month\":12,"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid payment request payload"));
  }

  @Test
  void getPaymentByUnknownId_ReturnsNotFound() throws Exception {
    mockMvc.perform(get("/payments/{id}", "11111111-1111-1111-1111-111111111111"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Page not found"));
  }

  private String validPayload(String cardNumber, String currency, int amount, String cvv) {
    return String.format("""
        {
          "card_number": "%s",
          "expiry_month": 12,
          "expiry_year": 2030,
          "currency": "%s",
          "amount": %d,
          "cvv": "%s"
        }
        """, cardNumber, currency, amount, cvv);
  }
}
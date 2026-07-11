package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(EventProcessingException.class)
  public ResponseEntity<ErrorResponse> handleException(EventProcessingException ex) {
    LOG.error("Exception happened", ex);
    return new ResponseEntity<>(new ErrorResponse("Page not found"),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadablePayload(HttpMessageNotReadableException ex) {
    return new ResponseEntity<>(new ErrorResponse("Invalid payment request payload"),
        HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({BankCommunicationException.class, BankIntegrationException.class})
  public ResponseEntity<ErrorResponse> handleBankException(RuntimeException ex) {
    LOG.error("Bank integration error", ex);
    return new ResponseEntity<>(new ErrorResponse("Unable to process payment"),
        HttpStatus.BAD_GATEWAY);
  }
}

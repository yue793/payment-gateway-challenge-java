package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.enums.PaymentRequestStatus;
import com.checkout.payment.gateway.model.PaymentRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRequestRepository {

  private final ConcurrentMap<UUID, PaymentRequest> paymentRequests =
      new ConcurrentHashMap<>();

  public void save(PaymentRequest paymentRequest) {
    paymentRequests.put(paymentRequest.getId(), paymentRequest);
  }

  public Optional<PaymentRequest> findById(UUID id) {
    return Optional.ofNullable(paymentRequests.get(id));
  }

  public List<PaymentRequest> findAllByStatus(PaymentRequestStatus status) {
    return paymentRequests.values().stream()
        .filter(pr -> pr.getStatus() == status)
        .collect(Collectors.toList());
  }

  public List<PaymentRequest> getAll() {
    return List.copyOf(paymentRequests.values());
  }
}

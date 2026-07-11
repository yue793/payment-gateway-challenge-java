package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.PaymentResponse;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentsRepository {

  private final ConcurrentMap<UUID, PaymentResponse> payments = new ConcurrentHashMap<>();

  public void add(PaymentResponse payment, Supplier<UUID> idGenerator) {
    while (payments.putIfAbsent(payment.getId(), payment) != null) {
      payment.setId(idGenerator.get());
    }
  }

  public Optional<PaymentResponse> get(UUID id) {
    return Optional.ofNullable(payments.get(id));
  }

}

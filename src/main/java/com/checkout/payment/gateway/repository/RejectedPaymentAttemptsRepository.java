package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.RejectedPaymentAttempt;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
public class RejectedPaymentAttemptsRepository {

  private final ConcurrentMap<UUID, RejectedPaymentAttempt> rejectedAttempts =
      new ConcurrentHashMap<>();

  public void add(RejectedPaymentAttempt rejectedPaymentAttempt) {
    rejectedAttempts.put(rejectedPaymentAttempt.getId(), rejectedPaymentAttempt);
  }

  public List<RejectedPaymentAttempt> getAll() {
    return List.copyOf(rejectedAttempts.values());
  }
}
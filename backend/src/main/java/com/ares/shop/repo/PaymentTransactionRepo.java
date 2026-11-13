package com.ares.shop.repo;

import com.ares.shop.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PaymentTransactionRepo extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByProviderAndExternalId(String provider, String externalId);
    List<PaymentTransaction> findByOrderCodeOrderByCreatedAtDesc(String orderCode);
}

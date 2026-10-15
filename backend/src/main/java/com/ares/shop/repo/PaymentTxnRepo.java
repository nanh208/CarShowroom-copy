package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface PaymentTxnRepo extends JpaRepository<PaymentTxn, Long> { Optional<PaymentTxn> findByTxnRef(String r); List<PaymentTxn> findByStatus(String s); List<PaymentTxn> findByOrderCodeOrderByIdDesc(String c); List<PaymentTxn> findAllByOrderByIdDesc(); }

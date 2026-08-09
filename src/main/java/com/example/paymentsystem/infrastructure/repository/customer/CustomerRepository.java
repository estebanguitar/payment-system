package com.example.paymentsystem.infrastructure.repository.customer;

import com.example.paymentsystem.domain.entity.customer.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 고객 식별자 기준의 고객 원장 저장과 조회를 제공한다. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /** 외부 고객 식별자로 고객 원장을 조회한다. */
    Optional<Customer> findByCustomerId(String customerId);

    /** 동일 고객 식별자의 원장 존재 여부를 확인한다. */
    boolean existsByCustomerId(String customerId);
}

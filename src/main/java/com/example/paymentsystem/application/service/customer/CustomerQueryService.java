package com.example.paymentsystem.application.service.customer;

import com.example.paymentsystem.shared.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.shared.application.exception.ApplicationException;
import com.example.paymentsystem.domain.entity.customer.Customer;
import com.example.paymentsystem.infrastructure.repository.customer.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 고객 존재 확인과 단건 조회를 제공한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerQueryService {
    private final CustomerRepository customerRepository;

    /** 식별자에 해당하는 고객을 반환하고 없으면 업무 오류를 발생시킨다. */
    public Customer getCustomer(String customerId) {
        return customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.CUSTOMER_NOT_FOUND));
    }
}

package com.example.paymentsystem.payment.application.service;

import com.example.paymentsystem.payment.application.dto.PaymentResult;

import com.example.paymentsystem.shared.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.shared.application.exception.ApplicationException;
import com.example.paymentsystem.payment.infrastructure.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 결제 엔티티를 트랜잭션 밖으로 노출하지 않고 결과 DTO로 조회한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentResultService {
    private final PaymentRepository paymentRepository;

    /** 결제 ID에 해당하는 최신 결제 결과를 반환한다. */
    public PaymentResult get(Long paymentId) {
        return PaymentResult.from(paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.PAYMENT_NOT_FOUND)));
    }
}

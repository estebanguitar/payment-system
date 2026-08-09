package com.example.paymentsystem.application.service.cancel;

import com.example.paymentsystem.application.dto.cancel.PaymentCancelResult;

import com.example.paymentsystem.shared.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.shared.application.exception.ApplicationException;
import com.example.paymentsystem.domain.entity.cancel.PaymentCancel;
import com.example.paymentsystem.infrastructure.repository.cancel.PaymentCancelRepository;
import com.example.paymentsystem.infrastructure.repository.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 취소와 원 결제의 최신 상태를 하나의 결과로 조회한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentCancelResultService {
    private final PaymentCancelRepository cancelRepository;
    private final PaymentRepository paymentRepository;

    /** 취소 ID로 취소와 원 결제 결과를 반환한다. */
    public PaymentCancelResult get(Long cancelId) {
        PaymentCancel cancel = cancelRepository.findById(cancelId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.CANCEL_NOT_FOUND));
        return PaymentCancelResult.from(cancel, paymentRepository.findById(cancel.getPaymentId())
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.PAYMENT_NOT_FOUND)));
    }

    /** PG 호출에 필요한 취소 엔티티를 읽기 전용으로 조회한다. */
    public PaymentCancel getEntity(Long cancelId) {
        return cancelRepository.findById(cancelId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.CANCEL_NOT_FOUND));
    }
}

package com.example.paymentsystem.service.payment;

import com.example.paymentsystem.dto.payment.PaymentResult;
import com.example.paymentsystem.dto.payment.query.CustomerPaymentView;
import com.example.paymentsystem.dto.payment.query.OperationsPaymentView;
import com.example.paymentsystem.dto.common.PageResult;
import com.example.paymentsystem.dto.payment.query.PaymentSearchCondition;
import com.example.paymentsystem.dto.payment.query.PaymentCancelView;
import com.example.paymentsystem.dto.payment.query.WalletTransactionView;
import com.example.paymentsystem.common.exception.ApplicationErrorCode;
import com.example.paymentsystem.common.exception.ApplicationException;
import com.example.paymentsystem.domain.payment.Payment;
import com.example.paymentsystem.repository.cancellation.PaymentCancelRepository;
import com.example.paymentsystem.repository.payment.PaymentRepository;
import com.example.paymentsystem.repository.payment.PaymentSpecifications;
import com.example.paymentsystem.repository.pg.PgResponseLogRepository;
import com.example.paymentsystem.repository.wallet.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 고객 소유권 조회와 운영자 복합 결제 검색을 제공한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentQueryService {
    private final PaymentRepository paymentRepository;
    private final PaymentCancelRepository cancelRepository;
    private final WalletTransactionRepository transactionRepository;
    private final PgResponseLogRepository pgLogRepository;

    /** 고객 소유 결제 한 건과 취소 이력을 조회한다. */
    public CustomerPaymentView getCustomerPayment(String customerId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndCustomerId(paymentId, customerId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.PAYMENT_NOT_FOUND));
        return CustomerPaymentView.builder().payment(PaymentResult.from(payment))
                .cancellations(cancelRepository.findAllByPaymentIdOrderByCreatedAtAsc(paymentId)
                        .stream().map(PaymentCancelView::from).toList()).build();
    }

    /** 고객 결제 목록을 생성일 내림차순의 1-based 페이지로 조회한다. */
    public PageResult<PaymentResult> getCustomerPayments(String customerId, int page, int size) {
        validatePage(page, size);
        return PageResult.from(paymentRepository.findAllByCustomerId(customerId,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"))).map(PaymentResult::from));
    }

    /** 운영자 조건을 검증하고 복합 조건 결제 목록을 조회한다. */
    public PageResult<PaymentResult> search(PaymentSearchCondition condition) {
        validatePage(condition.getPage(), condition.getSize());
        if (condition.getStartDate() != null && condition.getEndDate() != null
                && condition.getStartDate().isAfter(condition.getEndDate())) {
            throw new ApplicationException(ApplicationErrorCode.INVALID_DATE_RANGE);
        }
        return PageResult.from(paymentRepository.findAll(PaymentSpecifications.withCondition(condition),
                PageRequest.of(condition.getPage() - 1, condition.getSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt"))).map(PaymentResult::from));
    }

    /** 운영자 상세 조회에 취소·지갑 거래·PG 로그 식별자를 조합한다. */
    public OperationsPaymentView getOperationsPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.PAYMENT_NOT_FOUND));
        return OperationsPaymentView.builder().payment(PaymentResult.from(payment))
                .cancellations(cancelRepository.findAllByPaymentIdOrderByCreatedAtAsc(paymentId)
                        .stream().map(PaymentCancelView::from).toList())
                .walletTransactions(transactionRepository.findAllByPaymentIdOrderByCreatedAtAsc(paymentId)
                        .stream().map(WalletTransactionView::from).toList())
                .pgResponseLogIds(pgLogRepository.findAllByPaymentIdOrderByReceivedAtAsc(paymentId)
                        .stream().map(log -> log.getId()).toList()).build();
    }

    private static void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new ApplicationException(ApplicationErrorCode.INVALID_PAGE_REQUEST);
        }
    }
}

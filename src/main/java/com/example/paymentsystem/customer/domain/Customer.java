package com.example.paymentsystem.customer.domain;

import com.example.paymentsystem.shared.domain.exception.DomainErrorCode;
import com.example.paymentsystem.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 고객 식별 정보와 생성·수정 시각을 보관하는 고객 원장이다. */
@Getter
@Entity
@Table(name = "customer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true, length = 64)
    private String customerId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 128)
    private String email;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Customer(String customerId, String name, String email, LocalDateTime now) {
        this.customerId = requireText(customerId, "고객 식별자는 필수입니다.");
        this.name = requireText(name, "고객 이름은 필수입니다.");
        this.email = email;
        this.createdAt = requireNow(now);
        this.updatedAt = now;
    }

    /** 필수 고객 정보를 검증하고 신규 고객을 생성한다. */
    public static Customer create(String customerId, String name, String email, LocalDateTime now) {
        return new Customer(customerId, name, email, now);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, message);
        }
        return value;
    }

    private static LocalDateTime requireNow(LocalDateTime now) {
        if (now == null) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, "현재 시각은 필수입니다.");
        }
        return now;
    }
}

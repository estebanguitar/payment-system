package com.example.paymentsystem.dto.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 지갑 생성 요청 값을 검증한다. */
@Getter
@NoArgsConstructor
public class CreateWalletRequest {
    @NotBlank
    @Size(max = 64)
    private String customerId;
}

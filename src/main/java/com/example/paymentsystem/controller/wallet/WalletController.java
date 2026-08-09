package com.example.paymentsystem.controller.wallet;

import com.example.paymentsystem.service.wallet.WalletService;
import com.example.paymentsystem.common.response.ApiResponse;
import com.example.paymentsystem.dto.wallet.CreateWalletRequest;
import com.example.paymentsystem.dto.wallet.TopUpWalletRequest;
import com.example.paymentsystem.dto.wallet.WalletResponse;
import com.example.paymentsystem.dto.wallet.WalletTopUpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 지갑 생성·조회·충전 HTTP 요청을 애플리케이션 계층에 전달한다. */
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "지갑 API")
public class WalletController {
    private final WalletService walletService;

    /** 고객 지갑을 생성하며 고객당 하나의 지갑 제약은 서비스에서 보장한다. */
    @PostMapping
    @Operation(summary = "지갑 생성")
    public ApiResponse<WalletResponse> create(@Valid @RequestBody CreateWalletRequest request) {
        return ApiResponse.success(WalletResponse.from(walletService.createWallet(request.getCustomerId())));
    }

    /** 고객 식별자로 현재 지갑 잔액을 조회한다. */
    @GetMapping("/{customerId}")
    @Operation(summary = "지갑 조회")
    public ApiResponse<WalletResponse> get(@PathVariable String customerId) {
        return ApiResponse.success(WalletResponse.from(walletService.getWallet(customerId)));
    }

    /** 멱등키를 적용해 고객 지갑을 충전한다. */
    @PostMapping("/{customerId}/top-up")
    @Operation(summary = "지갑 충전")
    public ApiResponse<WalletTopUpResponse> topUp(@PathVariable String customerId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TopUpWalletRequest request) {
        return ApiResponse.success(WalletTopUpResponse.from(
                walletService.topUp(request.toCommand(customerId, idempotencyKey))));
    }
}

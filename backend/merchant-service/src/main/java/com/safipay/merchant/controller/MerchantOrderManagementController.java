package com.safipay.merchant.controller;

import com.safipay.merchant.dto.request.UpdateMerchantOrderStatusRequest;
import com.safipay.merchant.dto.response.ApiResponse;
import com.safipay.merchant.dto.response.MerchantOrderManagementResponse;
import com.safipay.merchant.service.MerchantOrderManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/api/merchants/{merchantId}/orders"
)
@RequiredArgsConstructor
public class MerchantOrderManagementController {

    private final MerchantOrderManagementService orderService;


    @GetMapping
    public ResponseEntity<
            ApiResponse<
                    List<MerchantOrderManagementResponse>
            >
    > getOrders(
            @PathVariable
            String merchantId,

            @AuthenticationPrincipal
            String ownerUserId
    ) {

        List<MerchantOrderManagementResponse> orders =
                orderService.getMerchantOrders(
                        merchantId,
                        ownerUserId
                );


        return ResponseEntity.ok(
                ApiResponse.success(
                        orders
                )
        );
    }


    @GetMapping("/{orderId}")
    public ResponseEntity<
            ApiResponse<
                    MerchantOrderManagementResponse
            >
    > getOrder(
            @PathVariable
            String merchantId,

            @PathVariable
            String orderId,

            @AuthenticationPrincipal
            String ownerUserId
    ) {

        MerchantOrderManagementResponse order =
                orderService.getMerchantOrder(
                        merchantId,
                        orderId,
                        ownerUserId
                );


        return ResponseEntity.ok(
                ApiResponse.success(
                        order
                )
        );
    }


    @PutMapping("/{orderId}/status")
    public ResponseEntity<
            ApiResponse<
                    MerchantOrderManagementResponse
            >
    > updateStatus(
            @PathVariable
            String merchantId,

            @PathVariable
            String orderId,

            @Valid
            @RequestBody
            UpdateMerchantOrderStatusRequest request,

            @AuthenticationPrincipal
            String ownerUserId
    ) {

        MerchantOrderManagementResponse order =
                orderService.updateStatus(
                        merchantId,
                        orderId,
                        request.getStatus(),
                        ownerUserId
                );


        return ResponseEntity.ok(
                ApiResponse.success(
                        order
                )
        );
    }
}
package com.safipay.fraud.service;

import com.safipay.fraud.dto.FraudCheckRequest;
import com.safipay.fraud.dto.FraudCheckResponse;
import com.safipay.fraud.model.FraudRecord;
import com.safipay.fraud.repository.FraudRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FraudService {

    private final FraudRecordRepository repo;

    public FraudCheckResponse checkFraud(FraudCheckRequest req) {

        UUID walletId = req.getWalletId();
        BigDecimal amount = req.getAmount();

        boolean flagged = false;

        // ------------------------------------------
        // R1 — Too many attempts in 1 minute (DB)
        // ------------------------------------------
        long attemptsLastMinute = repo.countRecentAttempts(walletId, LocalDateTime.now().minusMinutes(1));

        if (attemptsLastMinute > 5) {
            flagged = true;
        }

        // ------------------------------------------
        // R2 — Amount spike detection
        // ------------------------------------------
        var history = repo.findByWalletIdOrderByTimestampDesc(walletId);

        if (!history.isEmpty()) {
            BigDecimal lastAmount = history.get(0).getAmount();

            if (amount.compareTo(lastAmount.multiply(BigDecimal.valueOf(5))) > 0) {
                flagged = true;
            }
        }

        // ------------------------------------------
        // R3 — Static hard limit
        // ------------------------------------------
        if (amount.compareTo(BigDecimal.valueOf(10000)) > 0) {
            flagged = true;
        }

        // ------------------------------------------
        // R4 — Too many frauds in history
        // ------------------------------------------
        long frauds = history.stream().filter(FraudRecord::isFlagged).count();
        if (frauds > 3) {
            flagged = true;
        }

        // Save audit record
        FraudRecord record = new FraudRecord();
        record.setWalletId(walletId);
        record.setAmount(amount);
        record.setFlagged(flagged);
        repo.save(record);

        return new FraudCheckResponse(flagged);
    }
}

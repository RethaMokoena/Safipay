package com.safipay.fraud.service;

import com.safipay.fraud.dto.request.EvaluateTransactionRequest;
import com.safipay.fraud.model.FraudEvaluation;
import com.safipay.fraud.model.UserRiskProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based fraud scoring engine.
 *
 * Each rule contributes a score delta (0–100 scale).
 * Scores are summed and capped at 100.
 * Thresholds determine the final RiskLevel and FraudDecision.
 *
 * Rules are intentionally simple for an MVP — designed to be
 * extended with ML scoring, device fingerprinting, etc.
 */
@Component
@Slf4j
public class FraudRuleEngine {

    @Value("${fraud.risk.low-threshold:30}")
    private int lowThreshold;

    @Value("${fraud.risk.medium-threshold:60}")
    private int mediumThreshold;

    @Value("${fraud.risk.high-threshold:80}")
    private int highThreshold;

    @Value("${fraud.velocity.max-transactions-per-hour:10}")
    private int maxTxPerHour;

    @Value("${fraud.velocity.max-amount-per-hour:10000}")
    private long maxAmountPerHour;

    @Value("${fraud.velocity.max-amount-per-day:50000}")
    private long maxAmountPerDay;

    @Value("${fraud.block-on-high-risk:true}")
    private boolean blockOnHighRisk;

    // ── Entry point ───────────────────────────────────────────────

    public EvaluationResult evaluate(EvaluateTransactionRequest req, UserRiskProfile profile,
                                     long txCountLastHour, BigDecimal amountLastHour, BigDecimal amountLastDay,
                                     long blockedCountLastHour) {

        List<String> triggeredRules = new ArrayList<>();
        int score = 0;

        // ── RULE 1: Blacklisted user (immediate block) ─────────────
        if (profile.getIsBlacklisted()) {
            triggeredRules.add("USER_BLACKLISTED");
            score += 100;
        }

        // ── RULE 2: User already has open fraud alerts ──────────────
        if (profile.getOpenAlertCount() > 0) {
            triggeredRules.add("OPEN_FRAUD_ALERTS");
            score += 20 * Math.min(profile.getOpenAlertCount(), 3);
        }

        // ── RULE 3: Velocity — too many transactions this hour ──────
        if (txCountLastHour >= maxTxPerHour) {
            triggeredRules.add("VELOCITY_TX_COUNT");
            score += 25;
        } else if (txCountLastHour >= maxTxPerHour * 0.7) {
            triggeredRules.add("VELOCITY_TX_COUNT_WARNING");
            score += 10;
        }

        // ── RULE 4: Velocity — hourly amount limit ──────────────────
        if (amountLastHour.compareTo(BigDecimal.valueOf(maxAmountPerHour)) >= 0) {
            triggeredRules.add("VELOCITY_AMOUNT_HOURLY");
            score += 25;
        }

        // ── RULE 5: Velocity — daily amount limit ───────────────────
        if (amountLastDay.compareTo(BigDecimal.valueOf(maxAmountPerDay)) >= 0) {
            triggeredRules.add("VELOCITY_AMOUNT_DAILY");
            score += 20;
        }

        // ── RULE 6: Large transaction relative to user's average ────
        BigDecimal avg = profile.getAvgTransactionAmount();
        if (avg != null && avg.compareTo(BigDecimal.ZERO) > 0) {
            double ratio = req.getAmount().doubleValue() / avg.doubleValue();
            if (ratio > 10) {
                triggeredRules.add("AMOUNT_10X_ABOVE_AVERAGE");
                score += 30;
            } else if (ratio > 5) {
                triggeredRules.add("AMOUNT_5X_ABOVE_AVERAGE");
                score += 15;
            }
        }

        // ── RULE 7: Absolute large amount threshold ─────────────────
        if (req.getAmount().compareTo(BigDecimal.valueOf(50000)) >= 0) {
            triggeredRules.add("LARGE_AMOUNT_50K");
            score += 20;
        } else if (req.getAmount().compareTo(BigDecimal.valueOf(20000)) >= 0) {
            triggeredRules.add("LARGE_AMOUNT_20K");
            score += 10;
        }

        // ── RULE 8: Repeated failed transactions (probe behaviour) ──
        if (blockedCountLastHour >= 3) {
            triggeredRules.add("REPEATED_FAILURES");
            score += 30;
        }

        // ── RULE 9: New device detected ─────────────────────────────
        if (req.getDeviceId() != null && profile.getLastKnownDeviceId() != null
                && !req.getDeviceId().equals(profile.getLastKnownDeviceId())) {
            triggeredRules.add("NEW_DEVICE");
            score += 10;
        }

        // ── RULE 10: New IP address ─────────────────────────────────
        if (req.getIpAddress() != null && profile.getLastKnownIpAddress() != null
                && !req.getIpAddress().equals(profile.getLastKnownIpAddress())) {
            triggeredRules.add("NEW_IP_ADDRESS");
            score += 5;
        }

        // ── RULE 11: First-ever transaction from this user ──────────
        if (profile.getTotalTransactions() == 0) {
            triggeredRules.add("FIRST_TRANSACTION");
            score += 5; // Slight elevation — no baseline yet
        }

        // Cap score at 100
        score = Math.min(score, 100);

        FraudEvaluation.RiskLevel riskLevel = classifyRisk(score);
        FraudEvaluation.FraudDecision decision = makeDecision(riskLevel);
        String reason = buildReason(triggeredRules, score, riskLevel);

        log.debug("Fraud eval for tx {}: score={} level={} decision={} rules={}",
                req.getTransactionRef(), score, riskLevel, decision, triggeredRules);

        return new EvaluationResult(score, riskLevel, decision, triggeredRules, reason);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private FraudEvaluation.RiskLevel classifyRisk(int score) {
        if (score >= highThreshold) return FraudEvaluation.RiskLevel.CRITICAL;
        if (score >= mediumThreshold) return FraudEvaluation.RiskLevel.HIGH;
        if (score >= lowThreshold) return FraudEvaluation.RiskLevel.MEDIUM;
        return FraudEvaluation.RiskLevel.LOW;
    }

    private FraudEvaluation.FraudDecision makeDecision(FraudEvaluation.RiskLevel level) {
        return switch (level) {
            case CRITICAL -> FraudEvaluation.FraudDecision.BLOCKED;
            case HIGH -> blockOnHighRisk
                    ? FraudEvaluation.FraudDecision.BLOCKED
                    : FraudEvaluation.FraudDecision.REVIEW;
            case MEDIUM -> FraudEvaluation.FraudDecision.REVIEW;
            case LOW -> FraudEvaluation.FraudDecision.APPROVED;
        };
    }

    private String buildReason(List<String> rules, int score, FraudEvaluation.RiskLevel level) {
        if (rules.isEmpty()) return "No risk signals detected";
        return String.format("Risk score %d (%s) — triggered: %s", score, level,
                String.join(", ", rules));
    }

    // ── Result record ─────────────────────────────────────────────

    public record EvaluationResult(
            int score,
            FraudEvaluation.RiskLevel riskLevel,
            FraudEvaluation.FraudDecision decision,
            List<String> triggeredRules,
            String reason
    ) {}
}

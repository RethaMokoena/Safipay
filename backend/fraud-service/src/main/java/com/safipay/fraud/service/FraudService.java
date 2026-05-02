package com.safipay.fraud.service;

import com.safipay.fraud.dto.request.*;
import com.safipay.fraud.dto.response.*;
import com.safipay.fraud.exception.FraudException;
import com.safipay.fraud.model.*;
import com.safipay.fraud.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FraudService {

    private final FraudEvaluationRepository evaluationRepo;
    private final FraudAlertRepository alertRepo;
    private final UserRiskProfileRepository profileRepo;
    private final FraudRuleEngine ruleEngine;

    // ── Core evaluation ───────────────────────────────────────────

    /**
     * Main entry point — called by payment-service, merchant-service, stokvel-service
     * before processing any financial transaction.
     *
     * Returns a decision: APPROVED, REVIEW, or BLOCKED.
     */
    public FraudEvaluationResponse evaluateTransaction(EvaluateTransactionRequest req) {
        // Get or create user risk profile
        UserRiskProfile profile = profileRepo.findById(req.getUserId())
                .orElseGet(() -> profileRepo.save(UserRiskProfile.builder()
                        .userId(req.getUserId()).build()));

        // Gather velocity signals from recent evaluations
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        LocalDateTime oneDayAgo  = LocalDateTime.now().minusDays(1);

        long txCountLastHour = evaluationRepo.countByUserIdSince(req.getUserId(), oneHourAgo);
        BigDecimal amountLastHour = evaluationRepo.sumAmountByUserIdSince(req.getUserId(), oneHourAgo);
        BigDecimal amountLastDay  = evaluationRepo.sumAmountByUserIdSince(req.getUserId(), oneDayAgo);
        long blockedLastHour = evaluationRepo.countBlockedByUserIdSince(req.getUserId(), oneHourAgo);

        // Run the rule engine
        FraudRuleEngine.EvaluationResult result = ruleEngine.evaluate(
                req, profile, txCountLastHour, amountLastHour, amountLastDay, blockedLastHour);

        // Persist evaluation record
        FraudEvaluation evaluation = evaluationRepo.save(FraudEvaluation.builder()
                .transactionRef(req.getTransactionRef())
                .userId(req.getUserId())
                .amount(req.getAmount())
                .transactionType(req.getTransactionType())
                .riskScore(result.score())
                .riskLevel(result.riskLevel())
                .decision(result.decision())
                .triggeredRules(String.join(",", result.triggeredRules()))
                .decisionReason(result.reason())
                .ipAddress(req.getIpAddress())
                .deviceId(req.getDeviceId())
                .recipientUserId(req.getRecipientUserId())
                .merchantId(req.getMerchantId())
                .build());

        // Raise alerts for high-risk evaluations
        if (result.riskLevel() == FraudEvaluation.RiskLevel.HIGH
                || result.riskLevel() == FraudEvaluation.RiskLevel.CRITICAL) {
            raiseAlerts(evaluation, result, profile);
        }

        // Update user risk profile
        updateProfile(profile, req, result);

        log.info("Fraud evaluation {} for user {}: score={} decision={}",
                evaluation.getId(), req.getUserId(), result.score(), result.decision());

        return toEvaluationResponse(evaluation, result.triggeredRules());
    }

    // ── Alert management ──────────────────────────────────────────

    public FraudAlertResponse resolveAlert(String alertId, ResolveAlertRequest req, String resolvedBy) {
        FraudAlert alert = alertRepo.findById(alertId)
                .orElseThrow(() -> new FraudException("Alert not found: " + alertId));

        if (alert.getStatus() != FraudAlert.AlertStatus.OPEN
                && alert.getStatus() != FraudAlert.AlertStatus.UNDER_REVIEW) {
            throw new FraudException("Alert is already resolved: " + alert.getStatus());
        }

        alert.setStatus(req.getResolution());
        alert.setResolvedBy(resolvedBy);
        alert.setResolutionNotes(req.getNotes());

        // If confirmed fraud — escalate user risk tier
        if (req.getResolution() == FraudAlert.AlertStatus.RESOLVED_FRAUD) {
            profileRepo.findById(alert.getUserId()).ifPresent(p -> {
                p.setRiskTier(UserRiskProfile.RiskTier.HIGH);
                p.setOpenAlertCount(Math.max(0, p.getOpenAlertCount() - 1));
                profileRepo.save(p);
                log.warn("User {} risk tier escalated to HIGH after confirmed fraud", alert.getUserId());
            });
        } else if (req.getResolution() == FraudAlert.AlertStatus.RESOLVED_LEGITIMATE
                || req.getResolution() == FraudAlert.AlertStatus.DISMISSED) {
            profileRepo.findById(alert.getUserId()).ifPresent(p -> {
                p.setOpenAlertCount(Math.max(0, p.getOpenAlertCount() - 1));
                profileRepo.save(p);
            });
        }

        return toAlertResponse(alertRepo.save(alert));
    }

    // ── Blacklisting ──────────────────────────────────────────────

    public UserRiskProfileResponse blacklistUser(String userId) {
        UserRiskProfile profile = profileRepo.findById(userId)
                .orElseGet(() -> profileRepo.save(UserRiskProfile.builder().userId(userId).build()));
        profile.setIsBlacklisted(true);
        profile.setRiskTier(UserRiskProfile.RiskTier.BLACKLISTED);
        log.warn("User {} has been BLACKLISTED", userId);
        return toProfileResponse(profileRepo.save(profile));
    }

    public UserRiskProfileResponse unblacklistUser(String userId) {
        UserRiskProfile profile = profileRepo.findById(userId)
                .orElseThrow(() -> new FraudException("Profile not found: " + userId));
        profile.setIsBlacklisted(false);
        profile.setRiskTier(UserRiskProfile.RiskTier.MEDIUM);
        log.info("User {} has been removed from blacklist", userId);
        return toProfileResponse(profileRepo.save(profile));
    }

    // ── Queries ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FraudAlertResponse> getOpenAlerts(int page, int size) {
        return alertRepo.findByStatusOrderByCreatedAtDesc(FraudAlert.AlertStatus.OPEN, PageRequest.of(page, size))
                .stream().map(this::toAlertResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserRiskProfileResponse getUserProfile(String userId) {
        return toProfileResponse(profileRepo.findById(userId)
                .orElseThrow(() -> new FraudException("Risk profile not found: " + userId)));
    }

    @Transactional(readOnly = true)
    public List<FraudEvaluationResponse> getUserHistory(String userId, int page, int size) {
        return evaluationRepo.findByUserIdOrderByEvaluatedAtDesc(userId, PageRequest.of(page, size))
                .stream().map(e -> toEvaluationResponse(e, List.of(e.getTriggeredRules() != null
                        ? e.getTriggeredRules().split(",") : new String[0])))
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────

    private void raiseAlerts(FraudEvaluation evaluation, FraudRuleEngine.EvaluationResult result,
                              UserRiskProfile profile) {
        for (String rule : result.triggeredRules()) {
            FraudAlert.AlertType alertType = mapRuleToAlertType(rule);
            if (alertType == null) continue;

            alertRepo.save(FraudAlert.builder()
                    .evaluation(evaluation)
                    .userId(evaluation.getUserId())
                    .alertType(alertType)
                    .description(String.format("Rule '%s' triggered. Score: %d. Tx: %s",
                            rule, result.score(), evaluation.getTransactionRef()))
                    .build());
        }

        profile.setOpenAlertCount(profile.getOpenAlertCount() +
                (int) result.triggeredRules().stream().filter(r -> mapRuleToAlertType(r) != null).count());
    }

    private void updateProfile(UserRiskProfile profile, EvaluateTransactionRequest req,
                                FraudRuleEngine.EvaluationResult result) {
        if (result.decision() != FraudEvaluation.FraudDecision.BLOCKED) {
            // Update running average
            int total = profile.getTotalTransactions();
            BigDecimal newAvg = profile.getAvgTransactionAmount()
                    .multiply(BigDecimal.valueOf(total))
                    .add(req.getAmount())
                    .divide(BigDecimal.valueOf(total + 1), 2, RoundingMode.HALF_UP);

            profile.setTotalTransactions(total + 1);
            profile.setAvgTransactionAmount(newAvg);
            profile.setTotalTransactionVolume(profile.getTotalTransactionVolume().add(req.getAmount()));
        }

        if (req.getDeviceId() != null) profile.setLastKnownDeviceId(req.getDeviceId());
        if (req.getIpAddress() != null) profile.setLastKnownIpAddress(req.getIpAddress());
        profile.setLastTransactionAt(LocalDateTime.now());

        // Escalate risk tier based on score
        if (result.riskLevel() == FraudEvaluation.RiskLevel.CRITICAL
                && profile.getRiskTier() != UserRiskProfile.RiskTier.BLACKLISTED) {
            profile.setRiskTier(UserRiskProfile.RiskTier.HIGH);
        }

        profileRepo.save(profile);
    }

    private FraudAlert.AlertType mapRuleToAlertType(String rule) {
        return switch (rule) {
            case "VELOCITY_TX_COUNT", "VELOCITY_TX_COUNT_WARNING" -> FraudAlert.AlertType.VELOCITY_BREACH;
            case "VELOCITY_AMOUNT_HOURLY", "VELOCITY_AMOUNT_DAILY" -> FraudAlert.AlertType.VELOCITY_BREACH;
            case "LARGE_AMOUNT_50K", "LARGE_AMOUNT_20K",
                    "AMOUNT_10X_ABOVE_AVERAGE", "AMOUNT_5X_ABOVE_AVERAGE" -> FraudAlert.AlertType.LARGE_AMOUNT;
            case "REPEATED_FAILURES" -> FraudAlert.AlertType.MULTIPLE_FAILURES;
            case "NEW_DEVICE" -> FraudAlert.AlertType.DEVICE_ANOMALY;
            case "NEW_IP_ADDRESS" -> FraudAlert.AlertType.GEOGRAPHIC_ANOMALY;
            case "USER_BLACKLISTED" -> FraudAlert.AlertType.ACCOUNT_TAKEOVER;
            case "OPEN_FRAUD_ALERTS" -> FraudAlert.AlertType.UNUSUAL_PATTERN;
            default -> null;
        };
    }

    // ── Mappers ───────────────────────────────────────────────────

    private FraudEvaluationResponse toEvaluationResponse(FraudEvaluation e, List<String> rules) {
        return FraudEvaluationResponse.builder()
                .id(e.getId()).transactionRef(e.getTransactionRef())
                .userId(e.getUserId()).amount(e.getAmount())
                .transactionType(e.getTransactionType()).riskScore(e.getRiskScore())
                .riskLevel(e.getRiskLevel()).decision(e.getDecision())
                .triggeredRules(rules).decisionReason(e.getDecisionReason())
                .evaluatedAt(e.getEvaluatedAt()).build();
    }

    private FraudAlertResponse toAlertResponse(FraudAlert a) {
        return FraudAlertResponse.builder()
                .id(a.getId())
                .evaluationId(a.getEvaluation() != null ? a.getEvaluation().getId() : null)
                .userId(a.getUserId()).alertType(a.getAlertType())
                .status(a.getStatus()).description(a.getDescription())
                .resolvedBy(a.getResolvedBy()).resolutionNotes(a.getResolutionNotes())
                .createdAt(a.getCreatedAt()).updatedAt(a.getUpdatedAt()).build();
    }

    private UserRiskProfileResponse toProfileResponse(UserRiskProfile p) {
        return UserRiskProfileResponse.builder()
                .userId(p.getUserId()).riskTier(p.getRiskTier())
                .avgTransactionAmount(p.getAvgTransactionAmount())
                .totalTransactions(p.getTotalTransactions())
                .totalTransactionVolume(p.getTotalTransactionVolume())
                .transactionsLastHour(p.getTransactionsLastHour())
                .amountLastHour(p.getAmountLastHour()).amountLastDay(p.getAmountLastDay())
                .isBlacklisted(p.getIsBlacklisted()).openAlertCount(p.getOpenAlertCount())
                .lastTransactionAt(p.getLastTransactionAt()).updatedAt(p.getUpdatedAt()).build();
    }
}

package com.safipay.stokvel.service;

import com.safipay.stokvel.dto.request.*;
import com.safipay.stokvel.dto.response.*;
import com.safipay.stokvel.exception.StokvelException;
import com.safipay.stokvel.model.*;
import com.safipay.stokvel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StokvelService {

    private final StokvelRepository stokvelRepo;
    private final StokvelMemberRepository memberRepo;
    private final ContributionRepository contributionRepo;
    private final PayoutRepository payoutRepo;
    private final WalletClient walletClient;
    private final FraudClient fraudClient;
    private final LedgerClient ledgerClient;
    private final WebhookClient webhookClient;

    // ── Create / Join / Activate ──────────────────────────────────

    public StokvelResponse create(CreateStokvelRequest req, String adminUserId) {
        if (stokvelRepo.existsByName(req.getName()))
            throw new StokvelException("Stokvel name already taken: " + req.getName());

        Stokvel s = Stokvel.builder()
            .name(req.getName()).description(req.getDescription()).type(req.getType())
            .contributionAmount(req.getContributionAmount())
            .contributionFrequency(req.getContributionFrequency())
            .maxMembers(req.getMaxMembers()).adminUserId(adminUserId).build();
        s = stokvelRepo.save(s);

        memberRepo.save(StokvelMember.builder()
            .stokvel(s).userId(adminUserId).payoutOrder(0).build());

        log.info("Created stokvel '{}' by {}", s.getName(), adminUserId);
        return toResponse(s);
    }

    public StokvelResponse join(Long stokvelId, String userId) {
        Stokvel s = get(stokvelId);
        if (s.getStatus() != Stokvel.StokvelStatus.FORMING)
            throw new StokvelException("Stokvel is not accepting members");
        if (memberRepo.existsByStokvelIdAndUserId(stokvelId, userId))
            throw new StokvelException("Already a member of this stokvel");

        long count = memberRepo.countByStokvelIdAndStatus(stokvelId, StokvelMember.MemberStatus.ACTIVE);
        if (count >= s.getMaxMembers())
            throw new StokvelException("Stokvel has reached its maximum member count");

        memberRepo.save(StokvelMember.builder()
            .stokvel(s).userId(userId).payoutOrder((int) count).build());

        if (count + 1 >= s.getMaxMembers()) {
            s.setStatus(Stokvel.StokvelStatus.ACTIVE);
            stokvelRepo.save(s);
            log.info("Stokvel {} auto-activated (full)", stokvelId);
        }
        return toResponse(s);
    }

    public StokvelResponse activate(Long stokvelId, String adminUserId) {
        Stokvel s = get(stokvelId);
        assertAdmin(s, adminUserId);
        if (s.getStatus() != Stokvel.StokvelStatus.FORMING)
            throw new StokvelException("Stokvel is not in FORMING state");
        if (memberRepo.countByStokvelIdAndStatus(stokvelId, StokvelMember.MemberStatus.ACTIVE) < 2)
            throw new StokvelException("Need at least 2 members to activate");

        s.setStatus(Stokvel.StokvelStatus.ACTIVE);
        return toResponse(stokvelRepo.save(s));
    }

    // ── Contribution ──────────────────────────────────────────────

    public ContributionResponse contribute(Long stokvelId, String userId, ContributeRequest req) {
        Stokvel s = get(stokvelId);
        if (s.getStatus() != Stokvel.StokvelStatus.ACTIVE)
            throw new StokvelException("Stokvel is not active");
        assertMember(stokvelId, userId);

        if (req.getAmount().compareTo(s.getContributionAmount()) != 0)
            throw new StokvelException("Contribution must be exactly R" + s.getContributionAmount());

        int cycle = currentCycle(stokvelId, s);
        if (contributionRepo.existsByStokvelIdAndUserIdAndCycleNumber(stokvelId, userId, cycle))
            throw new StokvelException("You have already contributed for cycle " + cycle);

        // Fraud check
        String fraudDecision = fraudClient.evaluate(
            "CONTRIB-" + stokvelId + "-" + userId, userId, req.getAmount(), "STOKVEL_CONTRIBUTION");
        if ("BLOCKED".equals(fraudDecision))
            throw new StokvelException("Contribution blocked by fraud prevention");

        // Debit member wallet
        walletClient.debit(userId, req.getAmount(), req.getTransactionId(),
            "Stokvel contribution: " + s.getName());

        Contribution c = contributionRepo.save(Contribution.builder()
            .stokvel(s).userId(userId).amount(req.getAmount())
            .status(Contribution.ContributionStatus.CONFIRMED)
            .transactionId(req.getTransactionId()).cycleNumber(cycle).build());

        s.setTotalPoolBalance(s.getTotalPoolBalance().add(req.getAmount()));
        stokvelRepo.save(s);

        ledgerClient.postContribution(userId, String.valueOf(stokvelId), req.getAmount(), c.getId().toString());
        webhookClient.fireEvent("stokvel.contribution",
                userId, String.valueOf(stokvelId),
                String.format("{\"stokvelId\":%d,\"userId\":\"%s\",\"amount\":%s,\"cycle\":%d}",
                        stokvelId, userId, req.getAmount(), cycle));

        log.info("Contribution R{} recorded for {} in stokvel {} (cycle {})",
            req.getAmount(), userId, stokvelId, cycle);
        return toContribResponse(c);
    }

    // ── ROSCA Payout ──────────────────────────────────────────────

    public PayoutResponse triggerRosca(Long stokvelId, String adminUserId) {
        Stokvel s = get(stokvelId);
        assertAdmin(s, adminUserId);
        if (s.getType() != Stokvel.StokvelType.ROSCA)
            throw new StokvelException("Only ROSCA stokvels use rotation payouts");
        if (s.getStatus() != Stokvel.StokvelStatus.ACTIVE)
            throw new StokvelException("Stokvel must be ACTIVE");

        List<StokvelMember> members = memberRepo.findByStokvelIdOrderByPayoutOrder(stokvelId);
        int idx = s.getCurrentPayoutIndex();
        if (idx >= members.size()) {
            s.setStatus(Stokvel.StokvelStatus.COMPLETED);
            stokvelRepo.save(s);
            throw new StokvelException("All ROSCA cycles are complete");
        }

        StokvelMember recipient = members.get(idx);
        if (recipient.getHasReceivedPayout())
            throw new StokvelException("Member at index " + idx + " has already been paid out");

        long activeCount = members.stream()
            .filter(m -> m.getStatus() == StokvelMember.MemberStatus.ACTIVE).count();
        BigDecimal payoutAmount = s.getContributionAmount().multiply(BigDecimal.valueOf(activeCount));

        if (s.getTotalPoolBalance().compareTo(payoutAmount) < 0)
            throw new StokvelException("Insufficient pool balance for payout. " +
                "Available: R" + s.getTotalPoolBalance() + ", Required: R" + payoutAmount);

        // Credit recipient wallet
        walletClient.credit(recipient.getUserId(), payoutAmount,
            "PAYOUT-" + stokvelId + "-" + (idx + 1),
            "ROSCA payout from " + s.getName() + " (cycle " + (idx + 1) + ")");

        Payout p = payoutRepo.save(Payout.builder()
            .stokvel(s).recipientUserId(recipient.getUserId()).amount(payoutAmount)
            .status(Payout.PayoutStatus.COMPLETED).type(Payout.PayoutType.ROSCA_ROTATION)
            .cycleNumber(idx + 1).build());

        recipient.setHasReceivedPayout(true);
        memberRepo.save(recipient);

        s.setTotalPoolBalance(s.getTotalPoolBalance().subtract(payoutAmount));
        s.setCurrentPayoutIndex(idx + 1);
        if (idx + 1 >= members.size()) {
            s.setStatus(Stokvel.StokvelStatus.COMPLETED);
            log.info("Stokvel {} ROSCA complete — all members paid", stokvelId);
        }
        stokvelRepo.save(s);

        ledgerClient.postPayout(String.valueOf(stokvelId), recipient.getUserId(),
            payoutAmount, p.getId().toString());
        webhookClient.fireEvent("stokvel.payout",
                recipient.getUserId(), String.valueOf(stokvelId),
                String.format("{\"stokvelId\":%d,\"recipientId\":\"%s\",\"amount\":%s,\"cycle\":%d}",
                        stokvelId, recipient.getUserId(), payoutAmount, idx + 1));

        log.info("ROSCA payout R{} to {} in stokvel {}", payoutAmount, recipient.getUserId(), stokvelId);
        return toPayoutResponse(p);
    }

    // ── Pool Withdrawal ───────────────────────────────────────────

    public PayoutResponse poolWithdraw(Long stokvelId, String recipientUserId,
                                        PoolWithdrawalRequest req, String adminUserId) {
        Stokvel s = get(stokvelId);
        assertAdmin(s, adminUserId);
        if (s.getType() != Stokvel.StokvelType.POOL)
            throw new StokvelException("Pool withdrawals only apply to POOL type stokvels");
        assertMember(stokvelId, recipientUserId);
        if (s.getTotalPoolBalance().compareTo(req.getAmount()) < 0)
            throw new StokvelException("Insufficient pool balance");

        walletClient.credit(recipientUserId, req.getAmount(),
            "POOL-WITHDRAW-" + stokvelId, "Pool withdrawal from " + s.getName());

        Payout p = payoutRepo.save(Payout.builder()
            .stokvel(s).recipientUserId(recipientUserId).amount(req.getAmount())
            .status(Payout.PayoutStatus.COMPLETED).type(Payout.PayoutType.POOL_WITHDRAWAL)
            .cycleNumber(payoutRepo.findByStokvelId(stokvelId).size() + 1).build());

        s.setTotalPoolBalance(s.getTotalPoolBalance().subtract(req.getAmount()));
        stokvelRepo.save(s);

        ledgerClient.postPayout(String.valueOf(stokvelId), recipientUserId,
            req.getAmount(), p.getId().toString());

        return toPayoutResponse(p);
    }

    // ── Queries ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StokvelResponse> getAll() {
        return stokvelRepo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StokvelResponse getById(Long id) { return toResponse(get(id)); }

    @Transactional(readOnly = true)
    public List<StokvelResponse> getMine(String userId) {
        return stokvelRepo.findByMemberUserId(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContributionResponse> getContributions(Long stokvelId, String userId) {
        assertMember(stokvelId, userId);
        return contributionRepo.findByStokvelId(stokvelId).stream()
            .map(this::toContribResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PayoutResponse> getPayouts(Long stokvelId, String userId) {
        assertMember(stokvelId, userId);
        return payoutRepo.findByStokvelId(stokvelId).stream()
            .map(this::toPayoutResponse).collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────

    Stokvel get(Long id) {
        return stokvelRepo.findById(id)
            .orElseThrow(() -> new StokvelException("Stokvel not found: " + id));
    }

    void assertAdmin(Stokvel s, String userId) {
        if (!s.getAdminUserId().equals(userId))
            throw new StokvelException("Only the stokvel admin can perform this action");
    }

    void assertMember(Long stokvelId, String userId) {
        if (!memberRepo.existsByStokvelIdAndUserId(stokvelId, userId))
            throw new StokvelException("You are not a member of this stokvel");
    }

    private int currentCycle(Long stokvelId, Stokvel s) {
        long active = s.getMembers().stream()
            .filter(m -> m.getStatus() == StokvelMember.MemberStatus.ACTIVE).count();
        int cycle = 1;
        while (contributionRepo.countByStokvelIdAndCycleNumberAndStatus(
                stokvelId, cycle, Contribution.ContributionStatus.CONFIRMED) >= active) {
            cycle++;
        }
        return cycle;
    }

    // ── Mappers ───────────────────────────────────────────────────

    private StokvelResponse toResponse(Stokvel s) {
        long count = memberRepo.countByStokvelIdAndStatus(s.getId(), StokvelMember.MemberStatus.ACTIVE);
        List<MemberResponse> members = memberRepo
            .findByStokvelIdAndStatus(s.getId(), StokvelMember.MemberStatus.ACTIVE)
            .stream().map(m -> MemberResponse.builder()
                .id(m.getId()).userId(m.getUserId()).status(m.getStatus())
                .payoutOrder(m.getPayoutOrder()).hasReceivedPayout(m.getHasReceivedPayout())
                .joinedAt(m.getJoinedAt()).build())
            .collect(Collectors.toList());

        return StokvelResponse.builder()
            .id(s.getId()).name(s.getName()).description(s.getDescription())
            .type(s.getType()).status(s.getStatus())
            .contributionAmount(s.getContributionAmount())
            .contributionFrequency(s.getContributionFrequency())
            .maxMembers(s.getMaxMembers()).currentMemberCount((int) count)
            .adminUserId(s.getAdminUserId()).totalPoolBalance(s.getTotalPoolBalance())
            .currentPayoutIndex(s.getCurrentPayoutIndex())
            .members(members).createdAt(s.getCreatedAt()).build();
    }

    private ContributionResponse toContribResponse(Contribution c) {
        return ContributionResponse.builder()
            .id(c.getId()).stokvelId(c.getStokvel().getId()).userId(c.getUserId())
            .amount(c.getAmount()).status(c.getStatus()).transactionId(c.getTransactionId())
            .cycleNumber(c.getCycleNumber()).contributedAt(c.getContributedAt()).build();
    }

    private PayoutResponse toPayoutResponse(Payout p) {
        return PayoutResponse.builder()
            .id(p.getId()).stokvelId(p.getStokvel().getId())
            .recipientUserId(p.getRecipientUserId()).amount(p.getAmount())
            .status(p.getStatus()).type(p.getType())
            .cycleNumber(p.getCycleNumber()).processedAt(p.getProcessedAt()).build();
    }
}

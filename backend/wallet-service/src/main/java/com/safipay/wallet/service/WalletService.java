package com.safipay.wallet.service;

import com.safipay.wallet.dto.request.*;
import com.safipay.wallet.dto.response.*;
import com.safipay.wallet.exception.WalletException;
import com.safipay.wallet.model.*;
import com.safipay.wallet.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WalletService {

    private final WalletRepository walletRepo;
    private final TransactionRepository txRepo;

    public WalletResponse createWallet(String userId) {

        if (userId == null || userId.isBlank()) {
            throw new WalletException("User ID cannot be null or blank");
        }

        if (walletRepo.existsByUserId(userId)) {
            throw new WalletException(
                    "Wallet already exists for user: " + userId
            );
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .build();

        wallet = walletRepo.save(wallet);

        log.info(
                "Created wallet {} for user {}",
                wallet.getId(),
                userId
        );

        return toWalletResponse(wallet);
    }

    /**
     * Used when a user registers.
     *
     * If the user already has a wallet, the existing wallet is returned.
     * Otherwise, a new wallet is automatically created.
     */
//     public WalletResponse createWalletIfAbsent(String userId) {

//         log.info(
//                 "Checking whether wallet exists for userId={}",
//                 userId
//         );

//         if (userId == null || userId.isBlank()) {

//             log.error(
//                     "Cannot create wallet because userId is null or blank"
//             );

//             throw new WalletException(
//                     "User ID cannot be null or blank"
//             );
//         }

//         return walletRepo.findByUserId(userId)
//                 .map(wallet -> {

//                     log.info(
//                             "Wallet already exists. walletId={}, userId={}",
//                             wallet.getId(),
//                             userId
//                     );

//                     return toWalletResponse(wallet);
//                 })
//                 .orElseGet(() -> {

//                     log.info(
//                             "No wallet exists for userId={}. Creating wallet...",
//                             userId
//                     );

//                     Wallet wallet = Wallet.builder()
//                             .userId(userId)
//                             .build();

//                     log.info(
//                             "Wallet before save: userId={}, balance={}, lockedBalance={}, currency={}, status={}",
//                             wallet.getUserId(),
//                             wallet.getBalance(),
//                             wallet.getLockedBalance(),
//                             wallet.getCurrency(),
//                             wallet.getStatus()
//                     );

//                     try {

//                         wallet = walletRepo.save(wallet);

//                         log.info(
//                                 "Wallet saved successfully. walletId={}, userId={}",
//                                 wallet.getId(),
//                                 userId
//                         );

//                     } catch (Exception ex) {

//                         log.error(
//                                 "Failed to save wallet for userId={}",
//                                 userId,
//                                 ex
//                         );

//                         throw ex;
//                     }

//                     return toWalletResponse(wallet);
//                 });
//     }


public WalletResponse createWalletIfAbsent(
        String userId,
        String userEmail
) {

    log.info(
            "Checking whether wallet exists for userId={}",
            userId
    );

    if (userId == null || userId.isBlank()) {
        throw new WalletException(
                "User ID cannot be null or blank"
        );
    }

    if (userEmail == null || userEmail.isBlank()) {
        throw new WalletException(
                "User email cannot be null or blank"
        );
    }

    return walletRepo.findByUserId(userId)
            .map(wallet -> {

                /*
                 * Keep the denormalized email synchronized.
                 */
                if (!userEmail.equals(wallet.getUserEmail())) {

                    log.info(
                            "Updating wallet email for userId={}",
                            userId
                    );

                    wallet.setUserEmail(userEmail);
                    wallet = walletRepo.save(wallet);
                }

                log.info(
                        "Wallet already exists. walletId={}, userId={}",
                        wallet.getId(),
                        userId
                );

                return toWalletResponse(wallet);
            })
            .orElseGet(() -> {

                log.info(
                        "No wallet exists for userId={}. Creating wallet...",
                        userId
                );

                Wallet wallet = Wallet.builder()
                        .userId(userId)
                        .userEmail(userEmail)
                        .build();

                log.info(
                        "Creating wallet for userId={}, email={}",
                        userId,
                        userEmail
                );

                try {

                    wallet = walletRepo.save(wallet);

                    log.info(
                            "Wallet saved successfully. walletId={}, userId={}",
                            wallet.getId(),
                            userId
                    );

                } catch (Exception ex) {

                    log.error(
                            "Failed to save wallet for userId={}",
                            userId,
                            ex
                    );

                    throw ex;
                }

                return toWalletResponse(wallet);
            });
}

    @Transactional(readOnly = true)
    public WalletResponse getWallet(String userId) {

        Wallet wallet = getWalletByUser(userId);

        return toWalletResponse(wallet);
    }

    public WalletResponse topUp(
            String userId,
            TopUpRequest req
    ) {

        Wallet wallet = getWalletByUser(userId);

        BigDecimal before = wallet.getBalance();

        wallet.setBalance(
                before.add(req.getAmount())
        );

        wallet = walletRepo.save(wallet);

        saveTransaction(
                wallet,
                req.getAmount(),
                before,
                wallet.getBalance(),
                Transaction.TransactionType.CREDIT,
                Transaction.TransactionStatus.COMPLETED,
                req.getReferenceId(),
                "Top up"
        );

        log.info(
                "Wallet {} topped up with R{}",
                wallet.getId(),
                req.getAmount()
        );

        return toWalletResponse(wallet);
    }

    public WalletResponse transfer(
            String senderUserId,
            TransferRequest req
    ) {

        if (senderUserId.equals(req.getRecipientUserId())) {
            throw new WalletException(
                    "Cannot transfer to yourself"
            );
        }

        Wallet sender
                = getWalletByUser(senderUserId);

        Wallet recipient
                = getWalletByUser(req.getRecipientUserId());

        if (sender.getAvailableBalance()
                .compareTo(req.getAmount()) < 0) {

            throw new WalletException(
                    "Insufficient balance"
            );
        }

        BigDecimal senderBefore
                = sender.getBalance();

        BigDecimal recipientBefore
                = recipient.getBalance();

        sender.setBalance(
                senderBefore.subtract(req.getAmount())
        );

        recipient.setBalance(
                recipientBefore.add(req.getAmount())
        );

        walletRepo.save(sender);
        walletRepo.save(recipient);

        String description
                = req.getDescription() != null
                ? req.getDescription()
                : "Transfer";

        saveTransaction(
                sender,
                req.getAmount(),
                senderBefore,
                sender.getBalance(),
                Transaction.TransactionType.DEBIT,
                Transaction.TransactionStatus.COMPLETED,
                null,
                description,
                req.getRecipientUserId()
        );

        saveTransaction(
                recipient,
                req.getAmount(),
                recipientBefore,
                recipient.getBalance(),
                Transaction.TransactionType.CREDIT,
                Transaction.TransactionStatus.COMPLETED,
                null,
                description,
                senderUserId
        );

        log.info(
                "Transfer R{} from user {} to user {}",
                req.getAmount(),
                senderUserId,
                req.getRecipientUserId()
        );

        return toWalletResponse(sender);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(
            String userId,
            int page,
            int size
    ) {

        Wallet wallet = getWalletByUser(userId);

        return txRepo
                .findByWalletIdOrderByCreatedAtDesc(
                        wallet.getId(),
                        PageRequest.of(page, size)
                )
                .stream()
                .map(this::toTxResponse)
                .collect(Collectors.toList());
    }

    /**
     * Internal service operation. Used by payment-service, stokvel-service,
     * etc.
     */
    public void internalCredit(
            String userId,
            BigDecimal amount,
            String referenceId,
            String description
    ) {

        Wallet wallet
                = getWalletByUser(userId);

        BigDecimal before
                = wallet.getBalance();

        wallet.setBalance(
                before.add(amount)
        );

        walletRepo.save(wallet);

        saveTransaction(
                wallet,
                amount,
                before,
                wallet.getBalance(),
                Transaction.TransactionType.CREDIT,
                Transaction.TransactionStatus.COMPLETED,
                referenceId,
                description
        );

        log.info(
                "Internal credit R{} applied to wallet {}",
                amount,
                wallet.getId()
        );
    }

    /**
     * Internal service operation. Used by payment-service, stokvel-service,
     * etc.
     */
    public void internalDebit(
            String userId,
            BigDecimal amount,
            String referenceId,
            String description
    ) {

        Wallet wallet
                = getWalletByUser(userId);

        if (wallet.getAvailableBalance()
                .compareTo(amount) < 0) {

            throw new WalletException(
                    "Insufficient balance for: "
                    + referenceId
            );
        }

        BigDecimal before
                = wallet.getBalance();

        wallet.setBalance(
                before.subtract(amount)
        );

        walletRepo.save(wallet);

        saveTransaction(
                wallet,
                amount,
                before,
                wallet.getBalance(),
                Transaction.TransactionType.DEBIT,
                Transaction.TransactionStatus.COMPLETED,
                referenceId,
                description
        );

        log.info(
                "Internal debit R{} applied to wallet {}",
                amount,
                wallet.getId()
        );
    }

    private Wallet getWalletByUser(
            String userId
    ) {

        return walletRepo
                .findByUserId(userId)
                .orElseThrow(
                        () -> new WalletException(
                                "Wallet not found for user: "
                                + userId
                        )
                );
    }

    private void saveTransaction(
            Wallet wallet,
            BigDecimal amount,
            BigDecimal before,
            BigDecimal after,
            Transaction.TransactionType type,
            Transaction.TransactionStatus status,
            String referenceId,
            String description
    ) {

        saveTransaction(
                wallet,
                amount,
                before,
                after,
                type,
                status,
                referenceId,
                description,
                null
        );
    }

    private void saveTransaction(
            Wallet wallet,
            BigDecimal amount,
            BigDecimal before,
            BigDecimal after,
            Transaction.TransactionType type,
            Transaction.TransactionStatus status,
            String referenceId,
            String description,
            String counterpartyUserId
    ) {

        Transaction transaction
                = Transaction.builder()
                        .wallet(wallet)
                        .amount(amount)
                        .balanceBefore(before)
                        .balanceAfter(after)
                        .type(type)
                        .status(status)
                        .referenceId(referenceId)
                        .description(description)
                        .counterpartyUserId(counterpartyUserId)
                        .build();

        txRepo.save(transaction);
    }

    private WalletResponse toWalletResponse(
            Wallet wallet
    ) {

        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .userEmail(wallet.getUserEmail())
                .balance(wallet.getBalance())
                .lockedBalance(wallet.getLockedBalance())
                .availableBalance(
                        wallet.getAvailableBalance()
                )
                .currency(wallet.getCurrency())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .build();
    }

    private TransactionResponse toTxResponse(
            Transaction transaction
    ) {

        return TransactionResponse.builder()
                .id(transaction.getId())
                .walletId(
                        transaction.getWallet().getId()
                )
                .amount(transaction.getAmount())
                .balanceBefore(
                        transaction.getBalanceBefore()
                )
                .balanceAfter(
                        transaction.getBalanceAfter()
                )
                .type(transaction.getType())
                .status(transaction.getStatus())
                .referenceId(
                        transaction.getReferenceId()
                )
                .description(
                        transaction.getDescription()
                )
                .counterpartyUserId(
                        transaction.getCounterpartyUserId()
                )
                .createdAt(
                        transaction.getCreatedAt()
                )
                .build();
    }
}

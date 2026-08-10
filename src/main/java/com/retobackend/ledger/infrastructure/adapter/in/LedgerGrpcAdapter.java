package com.retobackend.ledger.infrastructure.adapter.in;

import com.retobackend.ledger.application.port.in.CompensateTransactionUseCase;
import com.retobackend.ledger.application.port.in.ConfirmTransactionUseCase;
import com.retobackend.ledger.application.port.in.ReserveFundsUseCase;
import com.retobackend.ledger.domain.exception.AccountNotFoundException;
import com.retobackend.ledger.domain.exception.InsufficientFundsException;
import com.retobackend.ledger.domain.exception.ReservationNotFoundException;
import com.retobackend.ledger.domain.model.AccountId;
import com.retobackend.ledger.domain.model.Money;
import com.retobackend.ledger.grpc.*;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;

@GrpcService
@RequiredArgsConstructor
public class LedgerGrpcAdapter implements LedgerService {

    private final ReserveFundsUseCase reserveFundsUseCase;
    private final ConfirmTransactionUseCase confirmTransactionUseCase;
    private final CompensateTransactionUseCase compensateTransactionUseCase;

    @Override
    public Uni<ReserveFundsResponse> reserveFunds(ReserveFundsRequest request) {
        return reserveFundsUseCase.reserve(
                        request.getTransactionId(),
                        AccountId.of(request.getSourceAccount()),
                        AccountId.of(request.getTargetAccount()),
                        Money.of(BigDecimal.valueOf(request.getAmount())))
                .map(v -> ReserveFundsResponse.newBuilder()
                        .setTransactionId(request.getTransactionId())
                        .setStatus(Status.STATUS_RESERVED)
                        .setMessage("Fondos reservados correctamente")
                        .build())
                .onFailure(InsufficientFundsException.class).recoverWithItem(ex ->
                        ReserveFundsResponse.newBuilder()
                                .setTransactionId(request.getTransactionId())
                                .setStatus(Status.STATUS_INSUFFICIENT_FUNDS)
                                .setMessage(ex.getMessage())
                                .build())
                .onFailure(AccountNotFoundException.class).recoverWithItem(ex ->
                        ReserveFundsResponse.newBuilder()
                                .setTransactionId(request.getTransactionId())
                                .setStatus(Status.STATUS_UNKNOWN)
                                .setMessage(ex.getMessage())
                                .build());
    }

    @Override
    public Uni<ConfirmTransactionResponse> confirmTransaction(ConfirmTransactionRequest request) {
        return confirmTransactionUseCase.confirm(request.getTransactionId())
                .map(v -> ConfirmTransactionResponse.newBuilder()
                        .setTransactionId(request.getTransactionId())
                        .setStatus(Status.STATUS_CONFIRMED)
                        .setMessage("Transacción confirmada")
                        .build())
                .onFailure(ReservationNotFoundException.class).recoverWithItem(ex ->
                        ConfirmTransactionResponse.newBuilder()
                                .setTransactionId(request.getTransactionId())
                                .setStatus(Status.STATUS_UNKNOWN)
                                .setMessage(ex.getMessage())
                                .build());
    }

    @Override
    public Uni<CompensateTransactionResponse> compensateTransaction(CompensateTransactionRequest request) {
        return compensateTransactionUseCase.compensate(request.getTransactionId(), request.getReason())
                .map(v -> CompensateTransactionResponse.newBuilder()
                        .setTransactionId(request.getTransactionId())
                        .setStatus(Status.STATUS_COMPENSATED)
                        .build())
                .onFailure(ReservationNotFoundException.class).recoverWithItem(ex ->
                        CompensateTransactionResponse.newBuilder()
                                .setTransactionId(request.getTransactionId())
                                .setStatus(Status.STATUS_UNKNOWN)
                                .build());
    }
}
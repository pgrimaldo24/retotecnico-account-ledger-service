package com.retobackend.ledger.infrastructure.adapter.in;

import com.retobackend.ledger.grpc.*;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class LedgerGrpcAdapterIT {

    @GrpcClient
    LedgerService ledgerClient;

    @Test
    void shouldReserveFundsForExistingAccount() {
        ReserveFundsResponse response = reserveFunds("tx-integration-001", "ACC-100", "ACC-200", 100.00);

        assertEquals(Status.STATUS_RESERVED, response.getStatus());
    }

    @Test
    void shouldRejectReservationForNonexistentAccount() {
        ReserveFundsRequest request = ReserveFundsRequest.newBuilder()
                .setTransactionId("tx-integration-002")
                .setSourceAccount("ACC-999")
                .setTargetAccount("ACC-200")
                .setAmount(50.00)
                .setCurrency("USD")
                .build();

        ReserveFundsResponse response = ledgerClient.reserveFunds(request)
                .await().indefinitely();

        assertEquals(Status.STATUS_UNKNOWN, response.getStatus());
    }

    @Test
    void shouldRejectReservationWhenFundsAreInsufficient() {
        ReserveFundsRequest request = ReserveFundsRequest.newBuilder()
                .setTransactionId("tx-integration-003")
                .setSourceAccount("ACC-200")
                .setTargetAccount("ACC-100")
                .setAmount(1_000_000.00)
                .setCurrency("USD")
                .build();

        ReserveFundsResponse response = ledgerClient.reserveFunds(request)
                .await().indefinitely();

        assertEquals(Status.STATUS_INSUFFICIENT_FUNDS, response.getStatus());
    }

    @Test
    void shouldConfirmTransactionAfterReservation() {
        reserveFunds("tx-integration-confirm-001", "ACC-100", "ACC-200", 10.00);

        ConfirmTransactionResponse response = confirmSkippingSimulatedFailures("tx-integration-confirm-001");

        assertEquals(Status.STATUS_CONFIRMED, response.getStatus());
    }

    @Test
    void shouldRejectConfirmForNonexistentReservation() {
        ConfirmTransactionResponse response = confirmSkippingSimulatedFailures("tx-integration-confirm-missing");

        assertEquals(Status.STATUS_UNKNOWN, response.getStatus());
    }

    @Test
    void shouldCompensateTransactionAfterReservation() {
        reserveFunds("tx-integration-compensate-001", "ACC-100", "ACC-200", 10.00);

        CompensateTransactionRequest request = CompensateTransactionRequest.newBuilder()
                .setTransactionId("tx-integration-compensate-001")
                .setReason("timeout aguas arriba")
                .build();

        CompensateTransactionResponse response = ledgerClient.compensateTransaction(request)
                .await().indefinitely();

        assertEquals(Status.STATUS_COMPENSATED, response.getStatus());
    }

    @Test
    void shouldRejectCompensationForNonexistentReservation() {
        CompensateTransactionRequest request = CompensateTransactionRequest.newBuilder()
                .setTransactionId("tx-integration-compensate-missing")
                .setReason("motivo de prueba")
                .build();

        CompensateTransactionResponse response = ledgerClient.compensateTransaction(request)
                .await().indefinitely();

        assertEquals(Status.STATUS_UNKNOWN, response.getStatus());
    }

    private ReserveFundsResponse reserveFunds(String transactionId, String source, String target, double amount) {
        ReserveFundsRequest request = ReserveFundsRequest.newBuilder()
                .setTransactionId(transactionId)
                .setSourceAccount(source)
                .setTargetAccount(target)
                .setAmount(amount)
                .setCurrency("USD")
                .build();

        return ledgerClient.reserveFunds(request).await().indefinitely();
    }

    /**
     * confirmTransaction propaga el fallo simulado (25%, ver LedgerApplicationService)
     * como un error gRPC real para que el circuit breaker del orquestador lo detecte.
     * Reintenta hasta obtener una respuesta de negocio real.
     */
    private ConfirmTransactionResponse confirmSkippingSimulatedFailures(String transactionId) {
        ConfirmTransactionRequest request = ConfirmTransactionRequest.newBuilder()
                .setTransactionId(transactionId)
                .build();

        for (int attempt = 0; attempt < 100; attempt++) {
            try {
                return ledgerClient.confirmTransaction(request).await().indefinitely();
            } catch (StatusRuntimeException simulatedFailure) {
                // fallo simulado esperado; reintentar
            }
        }
        throw new IllegalStateException("No se pudo completar confirmTransaction tras varios reintentos");
    }
}

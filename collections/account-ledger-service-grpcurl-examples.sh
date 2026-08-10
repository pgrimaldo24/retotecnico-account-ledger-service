#!/usr/bin/env bash
# ============================================================================
# account-ledger-service — Ejemplos de prueba con grpcurl
# ============================================================================
# El Ledger está CERRADO al exterior (solo gRPC interno, sin REST), por lo
# que una colección Postman tradicional no aplica directamente. grpcurl es
# el equivalente de cURL para servicios gRPC y permite probar el contrato
# ledger.proto de forma manual y reproducible.
#
# Requisitos previos:
#   1. Instalar grpcurl: https://github.com/fullstorydev/grpcurl
#      - macOS:   brew install grpcurl
#      - Windows: choco install grpcurl  (o descarga el binario del release)
#      - Linux:   go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest
#
#   2. account-ledger-service corriendo en modo dev:
#      cd account-ledger-service && mvnw.cmd quarkus:dev
#
# Cada comando usa -proto + -import-path para describir el contrato sin
# necesidad de habilitar el servicio de reflexión gRPC en el servidor.
# ============================================================================

PROTO_PATH="src/main/proto"
PROTO_FILE="ledger.proto"
HOST="localhost:9001"

echo "============================================"
echo "1. ReserveFunds - Reserva exitosa"
echo "============================================"
grpcurl -plaintext \
  -import-path "${PROTO_PATH}" \
  -proto "${PROTO_FILE}" \
  -d '{
    "transaction_id": "grpcurl-demo-001",
    "source_account": "ACC-100",
    "target_account": "ACC-200",
    "amount": 100.00,
    "currency": "USD"
  }' \
  ${HOST} com.retobackend.ledger.LedgerService/ReserveFunds

echo ""
echo "============================================"
echo "2. ReserveFunds - Rechazo por fondos insuficientes"
echo "============================================"
grpcurl -plaintext \
  -import-path "${PROTO_PATH}" \
  -proto "${PROTO_FILE}" \
  -d '{
    "transaction_id": "grpcurl-demo-002",
    "source_account": "ACC-200",
    "target_account": "ACC-100",
    "amount": 999999.00,
    "currency": "USD"
  }' \
  ${HOST} com.retobackend.ledger.LedgerService/ReserveFunds

echo ""
echo "============================================"
echo "3. ReserveFunds - Cuenta inexistente"
echo "============================================"
grpcurl -plaintext \
  -import-path "${PROTO_PATH}" \
  -proto "${PROTO_FILE}" \
  -d '{
    "transaction_id": "grpcurl-demo-003",
    "source_account": "ACC-999",
    "target_account": "ACC-100",
    "amount": 10.00,
    "currency": "USD"
  }' \
  ${HOST} com.retobackend.ledger.LedgerService/ReserveFunds

echo ""
echo "============================================"
echo "4. ConfirmTransaction - Requiere una reserva previa"
echo "   (usa el transaction_id del paso 1 si obtuviste STATUS_RESERVED)"
echo "   NOTA: 25% de probabilidad de fallo simulado (UNAVAILABLE)."
echo "   Ejecuta este comando varias veces para observar ambos casos."
echo "============================================"
grpcurl -plaintext \
  -import-path "${PROTO_PATH}" \
  -proto "${PROTO_FILE}" \
  -d '{ "transaction_id": "grpcurl-demo-001" }' \
  ${HOST} com.retobackend.ledger.LedgerService/ConfirmTransaction

echo ""
echo "============================================"
echo "5. CompensateTransaction - Rollback manual de una reserva"
echo "============================================"
grpcurl -plaintext \
  -import-path "${PROTO_PATH}" \
  -proto "${PROTO_FILE}" \
  -d '{
    "transaction_id": "grpcurl-demo-001",
    "reason": "Prueba manual de compensacion via grpcurl"
  }' \
  ${HOST} com.retobackend.ledger.LedgerService/CompensateTransaction

echo ""
echo "============================================"
echo "Fin de las pruebas. Revisa la consola de account-ledger-service"
echo "para ver los logs con traceId de cada operacion ejecutada."
echo "============================================"

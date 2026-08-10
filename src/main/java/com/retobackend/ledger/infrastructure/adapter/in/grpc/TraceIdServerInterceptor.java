package com.retobackend.ledger.infrastructure.adapter.in.grpc;

import io.grpc.*;
import io.quarkus.grpc.GlobalInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.MDC;

@GlobalInterceptor
@ApplicationScoped
public class TraceIdServerInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> TRACE_ID_KEY =
            Metadata.Key.of("trace-id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String traceId = headers.get(TRACE_ID_KEY);
        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {
            @Override
            public void onMessage(ReqT message) {
                if (traceId != null) {
                    MDC.put("traceId", traceId);
                }
                try {
                    super.onMessage(message);
                } finally {
                    MDC.remove("traceId");
                }
            }
        };
    }
}
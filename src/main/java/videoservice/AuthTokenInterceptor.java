package videoservice;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.jsonwebtoken.*;

import java.nio.charset.StandardCharsets;


public class AuthTokenInterceptor implements ServerInterceptor {
    private final byte[] signingKey = loadSigningKey();
    private static final Metadata.Key<String> AUTH_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final String BEARER = "Bearer ";
    private static final Context.Key<String> CLIENT_ID_KEY =
            Context.key("client-id");

    private byte[] loadSigningKey() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null) {
            throw new IllegalStateException("JWT_SECRET environment variable not set");
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next
    ) {
        String value = headers.get(AUTH_KEY);
        Status status;

        if (value == null) {
            status = Status.UNAUTHENTICATED.withDescription("Authorization token is missing");
        } else if (!value.startsWith(BEARER)) {
            status = Status.UNAUTHENTICATED.withDescription("Unknown authorization type");
        } else {
            try {
                String token = value.substring(BEARER.length()).trim();
                JwtParser parser = Jwts.parser().setSigningKey(signingKey);
                Jws<Claims> claims = parser.parseClaimsJws(token);
                Context ctx = Context.current()
                        .withValue(CLIENT_ID_KEY, claims.getBody().getSubject());
                return Contexts.interceptCall(ctx, call, headers, next);
            } catch (JwtException e) {
                status = Status.UNAUTHENTICATED
                        .withDescription("Invalid token: " + e.getMessage())
                        .withCause(e);
            }
        }

        call.close(status, headers);
        // return no-op listener to satisfy the API
        return new ServerCall.Listener<ReqT>() { };
    }
}
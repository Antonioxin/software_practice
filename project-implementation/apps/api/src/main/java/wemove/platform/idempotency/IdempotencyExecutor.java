package wemove.platform.idempotency;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import wemove.platform.*;
import wemove.platform.api.ApiException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

/**
 * Transaction-external coordinator. The actor lock deliberately serializes commands by account. No
 * claim can commit independently of its business result and audit. Legacy data is never deleted.
 */
@Component
public class IdempotencyExecutor {
    @org.springframework.beans.factory.annotation.Autowired private TransactionProbe probe;
    private final UnitOfWork work;
    private final IdentityPort identity;
    private final IdempotencyRecordRepository records;
    private final ObjectMapper mapper;

    public IdempotencyExecutor(
            UnitOfWork work,
            IdentityPort identity,
            IdempotencyRecordRepository records,
            ObjectMapper mapper) {
        this.work = work;
        this.identity = identity;
        this.records = records;
        this.mapper = mapper;
    }

    public <T> Result<T> execute(
            UUID actor,
            String operation,
            UUID key,
            String resource,
            Object body,
            Class<T> type,
            int status,
            String legacyHash,
            Supplier<T> command) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException(
                    "Idempotency coordinator must be called outside a transaction");
        String digest = hash(Map.of("method", "POST", "path", resource, "body", body));
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return work.run(
                        () -> {
                            identity.lockActiveActor(actor);
                            var family =
                                    records.findByActorIdAndIdempotencyKey(actor, key).stream()
                                            .filter(
                                                    r ->
                                                            r.getOperationId().equals(operation)
                                                                    || r.getOperationId()
                                                                            .startsWith(
                                                                                    operation
                                                                                            + ":"))
                                            .toList();
                            if (family.size() > 1) throw conflict();
                            if (!family.isEmpty()) {
                                var saved = family.getFirst();
                                boolean legacy = saved.getSchemaVersion() == 1;
                                boolean targetMatches =
                                        saved.getOperationId().equals(operation)
                                                || resource.contains(
                                                        saved.getOperationId()
                                                                .substring(operation.length() + 1));
                                if (!targetMatches
                                        || !saved.getRequestHash()
                                                .equals(legacy ? legacyHash : digest))
                                    throw conflict();
                                if (!saved.getExecutionState().equals("COMPLETED"))
                                    throw inProgress();
                                return new Result<>(decode(saved.getResponseJson(), type), true);
                            }
                            var claim =
                                    new IdempotencyRecordEntity(
                                            actor,
                                            operation,
                                            key,
                                            digest,
                                            "null",
                                            status,
                                            Instant.now());
                            claim.begin(resource);
                            claim = records.saveAndFlush(claim);
                            T value = command.get();
                            claim.complete(encode(value), status, Instant.now());
                            records.flush();
                            probe.hit("idempotency.completed");
                            return new Result<>(value, false);
                        });
            } catch (PessimisticLockingFailureException ex) {
                if (attempt == 2) throw inProgress();
            }
        }
        throw inProgress();
    }

    public String hash(Object body) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            encode(sorted(mapper.valueToTree(body)))
                                                    .getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private JsonNode sorted(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = mapper.createObjectNode();
            TreeSet<String> keys = new TreeSet<>();
            node.fieldNames().forEachRemaining(keys::add);
            keys.forEach(k -> result.set(k, sorted(node.get(k))));
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = mapper.createArrayNode();
            node.forEach(v -> result.add(sorted(v)));
            return result;
        }
        return node;
    }

    private String encode(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private <T> T decode(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private ApiException conflict() {
        return new ApiException(
                HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "该请求标识已有不同内容或多个历史目标，请查询原结果。");
    }

    private ApiException inProgress() {
        return new ApiException(HttpStatus.CONFLICT, "REQUEST_IN_PROGRESS", "请求结果尚未确认，请使用原请求重试。");
    }

    public record Result<T>(T value, boolean replayed) {}
}

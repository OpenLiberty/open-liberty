package io.smallrye.reactive.messaging.providers.extension;

import io.smallrye.reactive.messaging.EmitterConfiguration;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.smallrye.reactive.messaging.providers.i18n.ProviderExceptions.ex;

/**
 * Implementation of the emitter pattern.
 *
 * @param <T> the type of payload sent by the emitter.
 */
public class EmitterImpl<T> extends AbstractEmitter<T> implements Emitter<T> {

    public EmitterImpl(EmitterConfiguration config, long defaultBufferSize) {
        super(config, defaultBufferSize);
    }

    @Override
    public synchronized CompletionStage<Void> send(T payload) {
        if (payload == null) {
            throw ex.illegalArgumentForNullValue();
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        emit(Message.of(payload)
                .withAck(() -> {
                    future.complete(null);
                    return CompletableFuture.completedFuture(null);
                }).withNack(reason -> {
                    future.completeExceptionally(reason);
                    return CompletableFuture.completedFuture(null);
                }));
        return future;
    }

    @Override
    public synchronized <M extends Message<? extends T>> void send(M msg) {
        if (msg == null) {
            throw ex.illegalArgumentForNullValue();
        }
        emit(msg);
    }

}

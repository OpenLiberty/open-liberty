/*
 * Copyright 2022 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * This is a derivative work based on Smallrye Reactive Messaging EmitterImpl
 * https://github.com/smallrye/smallrye-reactive-messaging/blob/4.10.1/smallrye-reactive-messaging-provider/src/main/java/io/smallrye/reactive/messaging/providers/extension/EmitterImpl.java
 *
 * Changelog
 * Replace SmallRye Reactive Messaging ContextAwareMessage with MicroProfile Reactive Messaging Message to remove requirement to include Vert.x
 *
 * @param <T> the type of payload sent by the emitter.
 */
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
    public CompletionStage<Void> send(T payload) {
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
    public <M extends Message<? extends T>> void send(M msg) {
        if (msg == null) {
            throw ex.illegalArgumentForNullValue();
        }
        emit(msg);
    }

}

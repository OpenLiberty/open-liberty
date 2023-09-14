package io.openliberty.microprofile.reactive.messaging30.internal;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.osgi.service.component.annotations.Component;

import io.openliberty.microprofile.reactive.messaging.internal.interfaces.MessageAccess;

@Component
public class MessageAccess30 implements MessageAccess {

    @Override
    public <T> Message<T> create(T payload, Supplier<CompletionStage<Void>> ackFunction,
                                 Function<Throwable, CompletionStage<Void>> nackFunction) {
        return Message.of(payload, ackFunction, nackFunction);
    }

    @Override
    public void nack(Message<?> message, Throwable exception) {
        message.nack(exception);
    }

}

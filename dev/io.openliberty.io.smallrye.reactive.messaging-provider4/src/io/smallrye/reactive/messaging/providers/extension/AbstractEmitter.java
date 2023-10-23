package io.smallrye.reactive.messaging.providers.extension;

import static io.smallrye.reactive.messaging.providers.i18n.ProviderExceptions.ex;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.reactive.messaging.EmitterConfiguration;
import io.smallrye.reactive.messaging.MessagePublisherProvider;
import io.smallrye.reactive.messaging.providers.helpers.BroadcastHelper;
import io.smallrye.reactive.messaging.providers.helpers.NoStackTraceException;
import io.smallrye.reactive.messaging.providers.metrics.MetricDecorator;
import io.smallrye.reactive.messaging.providers.metrics.MicrometerDecorator;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

public abstract class AbstractEmitter<T> implements MessagePublisherProvider<T> {
    public static final NoStackTraceException NO_SUBSCRIBER_EXCEPTION = new NoStackTraceException("Unable to process message - no subscriber");
    protected final AtomicReference<MultiEmitter<? super Message<? extends T>>> internal = new AtomicReference<>();
    protected final Multi<Message<? extends T>> publisher;

    protected final String name;

    protected final AtomicReference<Throwable> synchronousFailure = new AtomicReference<>();
    private final OnOverflow.Strategy overflow;

    private final ReentrantLock lock = new ReentrantLock();

    @SuppressWarnings("unchecked")
    public AbstractEmitter(EmitterConfiguration config, long defaultBufferSize) {
        this.name = config.name();
        this.overflow = config.overflowBufferStrategy();
        if (defaultBufferSize <= 0) {
            throw ex.illegalArgumentForDefaultBuffer();
        }

        Consumer<MultiEmitter<? super Message<? extends T>>> deferred = fe -> {
            MultiEmitter<? super Message<? extends T>> previous = internal.getAndSet(fe);
            if (previous != null) {
                previous.complete();
            }
        };

        Multi<Message<? extends T>> tempPublisher = getPublisherForStrategy(config.overflowBufferStrategy(),
                                                                            config.overflowBufferSize(),
                                                                            defaultBufferSize, deferred);

        Instance<MetricDecorator> metric = CDI.current().select(MetricDecorator.class);
        Instance<MicrometerDecorator> micrometer = CDI.current().select(MicrometerDecorator.class);

        if (metric.isResolvable()) {
            tempPublisher = (Multi<Message<? extends T>>) metric.get().decorate(tempPublisher, config.name(), false);
        }
        if (micrometer.isResolvable()) {
            tempPublisher = (Multi<Message<? extends T>>) micrometer.get().decorate(tempPublisher, config.name(), false);
        }

        if (config.broadcast()) {
            publisher = (Multi<Message<? extends T>>) BroadcastHelper.broadcastPublisher(tempPublisher, config.numberOfSubscriberBeforeConnecting());
        } else {
            publisher = tempPublisher;
        }
    }

    public void complete() {
        lock.lock();
        try {
            MultiEmitter<? super Message<? extends T>> emitter = verify();
            if (emitter != null) {
                emitter.complete();
            }
        } finally {
            lock.unlock();
        }
    }

    public void error(Exception e) {
        if (e == null) {
            throw ex.illegalArgumentForException("null");
        }
        lock.lock();
        try {
            MultiEmitter<? super Message<? extends T>> emitter = verify();
            if (emitter != null) {
                emitter.fail(e);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isCancelled() {
        MultiEmitter<? super Message<? extends T>> emitter = internal.get();
        return emitter == null || emitter.isCancelled();
    }

    public boolean hasRequests() {
        MultiEmitter<? super Message<? extends T>> emitter = internal.get();
        return !isCancelled() && emitter.requested() > 0;
    }

    Multi<Message<? extends T>> getPublisherForStrategy(OnOverflow.Strategy overFlowStrategy, long bufferSize,
                                                        long defaultBufferSize,
                                                        Consumer<MultiEmitter<? super Message<? extends T>>> deferred) {
        if (overFlowStrategy == null) {
            overFlowStrategy = OnOverflow.Strategy.BUFFER;
        }
        switch (overFlowStrategy) {
            case BUFFER:
                if (bufferSize > 0) {
                    return ThrowingEmitter.create(deferred, bufferSize);
                } else {
                    return ThrowingEmitter.create(deferred, defaultBufferSize);
                }

            case UNBOUNDED_BUFFER:
                return Multi.createFrom().emitter(deferred, BackPressureStrategy.BUFFER);

            case THROW_EXCEPTION:
                return ThrowingEmitter.create(deferred, 0);

            case DROP:
                return Multi.createFrom().emitter(deferred, BackPressureStrategy.DROP);

            case FAIL:
                return Multi.createFrom().emitter(deferred, BackPressureStrategy.ERROR);

            case LATEST:
                return Multi.createFrom().emitter(deferred, BackPressureStrategy.LATEST);

            case NONE:
                return Multi.createFrom().emitter(deferred, BackPressureStrategy.IGNORE);

            default:
                throw ex.illegalArgumentForBackPressure(overFlowStrategy);
        }
    }

    /**
     * Creates the stream when using the default buffer size.
     *
     * @param defaultBufferSize the default buffer size
     * @param stream            the upstream
     * @return the stream.
     */
    Multi<Message<? extends T>> getPublisherUsingBufferStrategy(long defaultBufferSize,
                                                                Multi<Message<? extends T>> stream) {
        int size = (int) defaultBufferSize;
        return stream.onOverflow().buffer(size - 2).onFailure().invoke(synchronousFailure::set);
    }

    @Override
    public Publisher<Message<? extends T>> getPublisher() {
        return publisher;
    }

    protected void emit(Message<? extends T> message) {
        if (message == null) {
            throw ex.illegalArgumentForNullValue();
        }
        lock.lock();
        try {
            MultiEmitter<? super Message<? extends T>> emitter = verify();
            if (emitter == null) {
                if (overflow == OnOverflow.Strategy.DROP) {
                    // There are no subscribers, but because we use the DROP strategy, just ignore the event.
                    // However, nack the message, so the sender can be aware of the rejection.
                    message.nack(NO_SUBSCRIBER_EXCEPTION);
                }
                return;
            }
            if (synchronousFailure.get() != null) {
                throw ex.incomingNotFoundForEmitter(synchronousFailure.get());
            }
            if (emitter.isCancelled()) {
                throw ex.illegalStateForDownstreamCancel();
            }
            emitter.emit(message);
            if (synchronousFailure.get() != null) {
                throw ex.illegalStateForEmitterWhileEmitting(synchronousFailure.get());
            }
        } finally {
            lock.unlock();
        }
    }

    protected MultiEmitter<? super Message<? extends T>> verify() {
        MultiEmitter<? super Message<? extends T>> emitter = internal.get();
        if (emitter == null) {
            if (overflow == OnOverflow.Strategy.DROP) {
                // Just ignore the signal in this case, the message would have been dropped anyway.
                return null;
            } else {
                throw ex.noEmitterForChannel(name);
            }
        }
        if (emitter.isCancelled()) {
            throw ex.illegalStateForCancelledSubscriber(name);
        }
        return emitter;
    }
}
/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.microprofile.reactive.streams.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.spi.Graph;
import org.eclipse.microprofile.reactive.streams.spi.Stage;
import org.eclipse.microprofile.reactive.streams.spi.UnsupportedStageException;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * A built graph.
 * <p>
 * This class is the main class responsible for building and running graphs.
 * <p>
 * Each stage in the graph is provided with {@link StageInlet}'s and {@link StageOutlet}'s to any inlets and outlets it
 * may have. Stages that feed into each other will be joined by a {@link StageOutletInlet}. On
 * {@link Publisher} and {@link Subscriber} ends of the graph, as
 * well as for publisher and subscriber stages, the ports are {@link SubscriberInlet} and {@link PublisherOutlet}.
 * <p>
 * So in general, a graph is a series of stages, each separated by {@link StageOutletInlet}, and started/ended by
 * {@link SubscriberInlet} and {@link PublisherOutlet} when the ends are open.
 * <p>
 * The graph itself is an executor. This executor guarantees that all operations submitted to it are run serially, on
 * a backed thread pool. All signals into the graph must be submitted to this executor. The executor also handles
 * exceptions, any exceptions caught by the executor will result in the entire graph shutting down.
 */
public class BuiltGraph implements Executor {

    private static final int DEFAULT_BUFFER_HIGH_WATERMARK = 8;
    private static final int DEFAULT_BUFFER_LOW_WATERMARK = 4;

    private final Executor mutex;
    private final Deque<Signal> signals = new ArrayDeque<>();
    private final Set<Port> ports = new LinkedHashSet<>();
    private final Set<GraphStage> stages = new LinkedHashSet<>();

    private BuiltGraph(Executor threadPool) {
        this.mutex = new MutexExecutor(threadPool);
    }

    private static Builder newBuilder(Executor mutex) {
        BuiltGraph logic = new BuiltGraph(mutex);
        return logic.new Builder();
    }

    /**
     * Build a pubisher graph.
     */
    public static <T> Publisher<T> buildPublisher(Executor mutex, Graph graph) {
        return newBuilder(mutex).buildGraph(graph, Shape.PUBLISHER).publisher();
    }

    /**
     * Build a subscriber graph.
     */
    public static <T, R> CompletionSubscriber<T, R> buildSubscriber(Executor mutex, Graph graph) {
        return newBuilder(mutex).buildGraph(graph, Shape.SUBSCRIBER).subscriber();
    }

    /**
     * Build a processor graph.
     */
    public static <T, R> Processor<T, R> buildProcessor(Executor mutex, Graph graph) {
        return newBuilder(mutex).buildGraph(graph, Shape.PROCESSOR).processor();
    }

    /**
     * Build a closed graph.
     */
    public static <T> CompletionStage<T> buildCompletion(Executor mutex, Graph graph) {
        return newBuilder(mutex).buildGraph(graph, Shape.CLOSED).completion();
    }

    /**
     * Build a sub stage inlet.
     */
    public <T> SubStageInlet<T> buildSubInlet(Graph graph) {
        return new Builder().buildGraph(graph, Shape.INLET).inlet();
    }

    /**
     * Used to indicate the shape of the graph we're building.
     */
    private enum Shape {
        PUBLISHER, SUBSCRIBER, PROCESSOR, CLOSED, INLET
    }

    /**
     * A builder.
     * <p>
     * Builders are used both to build new graphs, as well as to add sub streams to an existing graph.
     */
    private class Builder {
        /**
         * The first subscriber of this graph. If this graph has a processor or subscriber shape, then by the time the
         * graph is ready to be built, this will be non null.
         */
        private Subscriber firstSubscriber;
        /**
         * Last publisher for this graph. If this graph has a processor or publisher shape, then by the time the graph is
         * ready to be built, this will be non null.
         */
        private Publisher lastPublisher;
        /**
         * The last inlet for the graph. Is this graph has an inlet shape, then by the time the graph is ready to be built,
         * this will be non null.
         */
        private StageInlet lastInlet;
        /**
         * The result for the graph. If this graph has a subscriber or closed shape, then by the time the graph is ready to
         * be built, this will be non null.
         */
        private CompletableFuture result;
        /**
         * The stages that have been added to the graph by this builder.
         */
        private final List<GraphStage> builderStages = new ArrayList<>();
        /**
         * The ports that have been added to the graph by this builder.
         */
        private final List<Port> builderPorts = new ArrayList<>();

        /**
         * Build the graph.
         */
        private Builder buildGraph(Graph graph, Shape shape) {

            // If we're building a subscriber or closed graph, instantiate the result.
            if (shape == Shape.SUBSCRIBER || shape == Shape.CLOSED) {
                result = new CompletableFuture();
            }

            Collection<Stage> graphStages = graph.getStages();
            // Special case - an empty graph. This should result in an identity processor.
            // To build this, we use a single map stage with the identity function.
            if (graphStages.isEmpty()) {
                graphStages = Collections.singleton(new Stage.Map(Function.identity()));
            }

            // In the loop below, we need to compare each pair of consecutive stages, to work out what sort of inlet/outlet
            // needs to be between them. Publisher, Subscriber and Processor stages get treated specially, since they need
            // to feed in to/out of not an inlet, but a subscriber/publisher. So, we're looking for the following patterns:
            // * A publisher or processor stage to a subscriber or processor stage - no inlet/outlet is needed, these can
            //   feed directly to each other, and we connect them using a connector stage.
            // * A publisher or processor stage to an inlet stage, these get connected using a SubscriberInlet
            // * An outlet stage to a subscriber or processor stage, these get connected using a PublisherOutlet
            // * An outlet stage to an inlet stage, these get connected using a StageOutletInlet
            // Finally we need to consider the ends of the graph - if the first stage has no inlet, then no port is needed
            // there. Otherwise, we need a SubscriberInlet. And if the last stage has no outlet, then no port is needed there,
            // otherwise, we need a PublisherOutlet.
            //
            // As we iterate through the graph, we need to know what the previous stage is to be able to work out which port
            // to instantiate, and we need to keep a reference to either the previous inlet or publisher, so that we can
            // pass it to the next stage that we construct.
            Stage previousStage = null;
            StageInlet previousInlet = null;
            Publisher previousPublisher = null;

            for (Stage stage : graphStages) {

                StageOutlet currentOutlet = null;
                StageInlet currentInlet = null;
                Publisher currentPublisher = null;
                Subscriber currentSubscriber = null;

                // If this is the first stage in the graph
                if (previousStage == null) {
                    if (isSubscriber(stage)) {
                        // It's a subscriber, we don't create an inlet, instead we use it directly as the first subscriber
                        // of this graph.
                        if (stage instanceof Stage.SubscriberStage) {
                            firstSubscriber = ((Stage.SubscriberStage) stage).getRsSubscriber();
                        } else if (stage instanceof Stage.ProcessorStage) {
                            firstSubscriber = ((Stage.ProcessorStage) stage).getRsProcessor();
                        }
                    } else if (stage.hasInlet()) {
                        // Otherwise if it has an inlet, we need to create a subscriber inlet as the first subscriber.
                        SubscriberInlet inlet = addPort(createSubscriberInlet());
                        currentInlet = inlet;
                        firstSubscriber = inlet;
                    }
                } else {
                    if (isPublisher(previousStage)) {
                        if (isSubscriber(stage)) {
                            // We're connecting a publisher to a subscriber, don't create any port, just record what the current
                            // publisher is.
                            if (stage instanceof Stage.SubscriberStage) {
                                currentSubscriber = ((Stage.SubscriberStage) stage).getRsSubscriber();
                            } else {
                                currentSubscriber = ((Stage.ProcessorStage) stage).getRsProcessor();
                            }
                        } else {
                            // We're connecting a publisher to an inlet, create a subscriber inlet for that.
                            SubscriberInlet inlet = addPort(createSubscriberInlet());
                            currentInlet = inlet;
                            currentSubscriber = inlet;
                        }
                    } else {
                        if (isSubscriber(stage)) {
                            // We're connecting an outlet to a subscriber, create a publisher outlet for that.
                            PublisherOutlet outlet = addPort(new PublisherOutlet(BuiltGraph.this));
                            currentOutlet = outlet;
                            currentPublisher = outlet;
                        } else {
                            // We're connecting an outlet to an inlet
                            StageOutletInlet outletInlet = addPort(new StageOutletInlet(BuiltGraph.this));
                            currentOutlet = outletInlet.new Outlet();
                            currentInlet = outletInlet.new Inlet();
                        }
                    }

                    // Now that we know the inlet/outlet/subscriber/publisher for the previous stage, we can instantiate it
                    addStage(previousStage, previousInlet, previousPublisher, currentOutlet, currentSubscriber);
                }

                previousStage = stage;
                previousInlet = currentInlet;
                previousPublisher = currentPublisher;
            }

            // Now we need to handle the last stage
            if (previousStage != null) {
                if (isPublisher(previousStage)) {
                    if (shape == Shape.INLET) {
                        // Last stage is a publisher, and we need to produce a sub stream inlet
                        SubscriberInlet subscriberInlet = addPort(createSubscriberInlet());
                        lastInlet = subscriberInlet;
                        addStage(previousStage, null, null, null, subscriberInlet);
                    } else {
                        // Last stage is a publisher, and we need a publisher, no need to handle it, we just set it to be
                        // the last publisher.
                        if (previousStage instanceof Stage.PublisherStage) {
                            lastPublisher = ((Stage.PublisherStage) previousStage).getRsPublisher();
                        } else {
                            lastPublisher = ((Stage.ProcessorStage) previousStage).getRsProcessor();
                        }
                    }
                } else if (previousStage.hasOutlet()) {
                    StageOutlet outlet;
                    if (shape == Shape.INLET) {
                        // We need to produce an inlet, and the last stage has an outlet, so create an outlet inlet for that
                        StageOutletInlet outletInlet = addPort(new StageOutletInlet(BuiltGraph.this));
                        lastInlet = outletInlet.new Inlet();
                        outlet = outletInlet.new Outlet();
                    } else {
                        // Otherwise we must be producing a publisher, to create a publisher outlet for that.
                        PublisherOutlet publisherOutlet = addPort(new PublisherOutlet(BuiltGraph.this));
                        outlet = publisherOutlet;
                        lastPublisher = publisherOutlet;
                    }
                    // And add the stage
                    addStage(previousStage, previousInlet, previousPublisher, outlet, null);
                } else {
                    // There's no outlet or publisher, just wire it to the previous stage
                    addStage(previousStage, previousInlet, previousPublisher, null, null);
                }
            }

            return this;
        }

        /**
         * Verify that the ports in this builder are ready to start receiving signals - that is, that they all have their
         * listeners set.
         */
        private void verifyReady() {
            // Verify that the ports have listeners etc
            for (Port port : builderPorts) {
                port.verifyReady();
            }
            ports.addAll(builderPorts);
        }

        /**
         * Start the stages on this listener
         */
        private void startGraph() {
            execute(() -> {
                for (GraphStage stage : builderStages) {
                    stages.add(stage);
                    stage.postStart();
                }
            });
        }

        private <T> SubStageInlet<T> inlet() {
            Objects.requireNonNull(lastInlet, "Not an inlet graph");
            assert result == null;
            assert firstSubscriber == null;
            assert lastPublisher == null;

            return new SubStageInlet(lastInlet, builderStages, builderPorts);
        }

        Publisher publisher() {
            Objects.requireNonNull(lastPublisher, "Not a publisher graph");
            assert result == null;
            assert firstSubscriber == null;
            assert lastInlet == null;

            verifyReady();
            startGraph();

            return lastPublisher;
        }

        CompletionSubscriber subscriber() {
            Objects.requireNonNull(firstSubscriber, "Not a subscriber graph");
            Objects.requireNonNull(result, "Not a subscriber graph");
            assert lastPublisher == null;
            assert lastInlet == null;

            verifyReady();
            startGraph();

            return CompletionSubscriber.of(firstSubscriber, result);
        }

        CompletionStage completion() {
            Objects.requireNonNull(result, "Not a completion graph");
            assert lastPublisher == null;
            assert firstSubscriber == null;
            assert lastInlet == null;

            verifyReady();
            startGraph();

            return result;
        }

        Processor processor() {
            Objects.requireNonNull(lastPublisher, "Not a processor graph");
            Objects.requireNonNull(firstSubscriber, "Not a processor graph");
            assert result == null;

            verifyReady();
            startGraph();

            return new WrappedProcessor(firstSubscriber, lastPublisher);
        }

        /**
         * Add a stage.
         * <p>
         * The stage will be inspected to see what type of stage it is, and it will be created using the passed in inlet,
         * publisher, outlet or subscriber, according to what it needs.
         * <p>
         * It is up to the caller of this method to ensure that the right combination of inlet/publisher/outlet/subscriber
         * are not null for the stage it's creating.
         */
        private void addStage(Stage stage, StageInlet inlet, Publisher publisher, StageOutlet outlet,
                              Subscriber subscriber) {

            // Inlets
            if (!stage.hasInlet()) {
                if (stage instanceof Stage.Of) {
                    addStage(new OfStage(BuiltGraph.this, outlet, ((Stage.Of) stage).getElements()));
                } else if (stage instanceof Stage.Concat) {
                    Stage.Concat concat = (Stage.Concat) stage;
                    addStage(new ConcatStage(BuiltGraph.this, buildSubInlet(concat.getFirst()), buildSubInlet(concat.getSecond()), outlet));
                } else if (stage instanceof Stage.PublisherStage) {
                    addStage(new ConnectorStage(BuiltGraph.this, ((Stage.PublisherStage) stage).getRsPublisher(), subscriber));
                } else if (stage instanceof Stage.Failed) {
                    addStage(new FailedStage(BuiltGraph.this, outlet, ((Stage.Failed) stage).getError()));
                } else {
                    throw new UnsupportedStageException(stage);
                }

                // Inlet/Outlets
            } else if (stage.hasOutlet()) {
                if (stage instanceof Stage.Map) {
                    addStage(new MapStage(BuiltGraph.this, inlet, outlet, ((Stage.Map) stage).getMapper()));
                } else if (stage instanceof Stage.Filter) {
                    addStage(new FilterStage(BuiltGraph.this, inlet, outlet, ((Stage.Filter) stage).getPredicate()));
                } else if (stage instanceof Stage.TakeWhile) {
                    Predicate predicate = ((Stage.TakeWhile) stage).getPredicate();
                    addStage(new TakeWhileStage(BuiltGraph.this, inlet, outlet, predicate));
                } else if (stage instanceof Stage.FlatMap) {
                    addStage(new FlatMapStage(BuiltGraph.this, inlet, outlet, ((Stage.FlatMap) stage).getMapper()));
                } else if (stage instanceof Stage.FlatMapCompletionStage) {
                    addStage(new FlatMapCompletionStage(BuiltGraph.this, inlet, outlet, ((Stage.FlatMapCompletionStage) stage).getMapper()));
                } else if (stage instanceof Stage.FlatMapIterable) {
                    addStage(new FlatMapIterableStage(BuiltGraph.this, inlet, outlet, ((Stage.FlatMapIterable) stage).getMapper()));
                } else if (stage instanceof Stage.ProcessorStage) {
                    Processor processor = ((Stage.ProcessorStage) stage).getRsProcessor();
                    addStage(new ConnectorStage(BuiltGraph.this, publisher, processor));
                    addStage(new ConnectorStage(BuiltGraph.this, processor, subscriber));
                } else if (stage instanceof Stage.Distinct) {
                    addStage(new DistinctStage(BuiltGraph.this, inlet, outlet));
                } else if (stage instanceof Stage.Limit) {
                    addStage(new LimitStage(BuiltGraph.this, inlet, outlet, ((Stage.Limit) stage).getLimit()));
                } else if (stage instanceof Stage.Skip) {
                    addStage(new SkipStage<>(BuiltGraph.this, inlet, outlet, ((Stage.Skip) stage).getSkip()));
                } else if (stage instanceof Stage.DropWhile) {
                    addStage(new DropWhileStage(BuiltGraph.this, inlet, outlet, ((Stage.DropWhile) stage).getPredicate()));
                } else if (stage instanceof Stage.Peek) {
                    addStage(new PeekStage<>(BuiltGraph.this, inlet, outlet, ((Stage.Peek) stage).getConsumer()));
                } else if (stage instanceof Stage.OnComplete) {
                    addStage(new OnCompleteStage<>(BuiltGraph.this, inlet, outlet, ((Stage.OnComplete) stage).getAction()));
                } else if (stage instanceof Stage.OnError) {
                    addStage(new OnErrorStage<>(BuiltGraph.this, inlet, outlet, ((Stage.OnError) stage).getConsumer()));
                } else if (stage instanceof Stage.OnTerminate) {
                    addStage(new OnTerminateStage<>(BuiltGraph.this, inlet, outlet, ((Stage.OnTerminate) stage).getAction()));
                } else if (stage instanceof Stage.OnErrorResume) {
                    addStage(new OnErrorResumeStage(BuiltGraph.this, inlet, outlet, ((Stage.OnErrorResume) stage).getFunction()));
                } else if (stage instanceof Stage.OnErrorResumeWith) {
                    addStage(new OnErrorResumeWithStage(BuiltGraph.this, inlet, outlet, ((Stage.OnErrorResumeWith) stage).getFunction()));
                } else {
                    throw new UnsupportedStageException(stage);
                }

                // Outlets
            } else {
                if (stage instanceof Stage.Collect) {
                    addStage(new CollectStage(BuiltGraph.this, inlet, result, ((Stage.Collect) stage).getCollector()));
                } else if (stage instanceof Stage.FindFirst) {
                    addStage(new FindFirstStage(BuiltGraph.this, inlet, result));
                } else if (stage instanceof Stage.Cancel) {
                    addStage(new CancelStage(BuiltGraph.this, inlet, result));
                } else if (stage instanceof Stage.SubscriberStage) {
                    // We need to capture termination, to do that we insert a CaptureTerminationStage between this and the
                    // previous stage.
                    if (inlet == null) {
                        SubscriberInlet subscriberInlet = addPort(createSubscriberInlet());
                        if (publisher != null) {
                            addStage(new ConnectorStage(BuiltGraph.this, publisher, subscriberInlet));
                        }
                        inlet = subscriberInlet;
                    }
                    PublisherOutlet publisherOutlet = addPort(new PublisherOutlet(BuiltGraph.this));
                    addStage(new CaptureTerminationStage(BuiltGraph.this, inlet, publisherOutlet, result));
                    addStage(new ConnectorStage(BuiltGraph.this, publisherOutlet, ((Stage.SubscriberStage) stage).getRsSubscriber()));
                } else {
                    throw new UnsupportedStageException(stage);
                }
            }
        }

        private SubscriberInlet createSubscriberInlet() {
            return new SubscriberInlet(BuiltGraph.this, DEFAULT_BUFFER_HIGH_WATERMARK, DEFAULT_BUFFER_LOW_WATERMARK);
        }

        private <T extends Port> T addPort(T port) {
            builderPorts.add(port);
            return port;
        }

        private void addStage(GraphStage stage) {
            builderStages.add(stage);
        }

        private boolean isSubscriber(Stage stage) {
            return stage instanceof Stage.SubscriberStage || stage instanceof Stage.ProcessorStage;
        }

        private boolean isPublisher(Stage stage) {
            return stage instanceof Stage.PublisherStage || stage instanceof Stage.ProcessorStage;
        }

    }

    /**
     * Execute a signal on this graphs execution context.
     * <p>
     * This is the entry point for all external signals into the graph. The passed in command will be run with exclusion
     * from all other signals on this graph. Any exceptions thrown by the command will cause the graph to be terminated
     * with a failure.
     * <p>
     * Commands are also allowed to (synchronously) emit unrolled signals, by adding them to the signals queue.
     * Unrolled signals are used for breaking infinite recursion scenarios. This method will drain all unrolled signals
     * (including subsequent signals emitted by the unrolled signals themselves) after invocation of the command.
     *
     * @param command The command to execute in this graphs execution context.
     */
    @Override
    public void execute(Runnable command) {
        mutex.execute(() -> {
            try {
                // First execute the runnable
                command.run();

                // Now drain a maximum of 32 signals from the queue
                int signalsDrained = 0;
                while (!signals.isEmpty() && signalsDrained < 32) {
                    signalsDrained++;
                    signals.removeFirst().signal();
                }

                // If there were more than 32 unrolled signals, we resubmit
                // to the executor to allow us to receive external signals
                if (!signals.isEmpty()) {
                    execute(() -> {
                    });
                }

            } catch (Throwable t) {
                // shut down the stream
                streamFailure(t);
                // Clear remaining signals
                signals.clear();
            }
        });
    }

    private void streamFailure(Throwable error) {
        // todo handle better
        error.printStackTrace();
        for (Port port : ports) {
            try {
                port.onStreamFailure(error);
            } catch (Exception e) {
                // Ignore
            }
        }
        ports.clear();
    }

    /**
     * Enqueue a signal to be executed serially after the current signal processing finishes.
     */
    void enqueueSignal(Signal signal) {
        signals.add(signal);
    }

    /**
     * An inlet for connecting a sub stage.
     * <p>
     * This stage captures close signals, and removes the stages and ports from the graph so as to avoid leaking memory.
     */
    final class SubStageInlet<T> implements StageInlet<T> {
        private final StageInlet<T> delegate;
        private final List<GraphStage> subStages;
        private final List<Port> subStagePorts;

        private SubStageInlet(StageInlet<T> delegate, List<GraphStage> subStages, List<Port> subStagePorts) {
            this.delegate = delegate;
            this.subStages = subStages;
            this.subStagePorts = subStagePorts;
        }

        void start() {
            subStagePorts.forEach(Port::verifyReady);
            ports.addAll(subStagePorts);
            for (GraphStage stage : subStages) {
                stages.add(stage);
                stage.postStart();
            }
        }

        private void shutdown() {
            // Do it in a signal, this ensures that if shutdown happens while something is iterating through
            // the ports, we don't get a concurrent modification exception.
            enqueueSignal(() -> {
                stages.removeAll(subStages);
                ports.removeAll(subStagePorts);
            });
        }

        @Override
        public void pull() {
            delegate.pull();
        }

        @Override
        public boolean isPulled() {
            return delegate.isPulled();
        }

        @Override
        public boolean isAvailable() {
            return delegate.isAvailable();
        }

        @Override
        public boolean isClosed() {
            return delegate.isClosed();
        }

        @Override
        public void cancel() {
            delegate.cancel();
        }

        @Override
        public T grab() {
            return delegate.grab();
        }

        @Override
        public void setListener(InletListener listener) {
            delegate.setListener(new InletListener() {
                @Override
                public void onPush() {
                    listener.onPush();
                }

                @Override
                public void onUpstreamFinish() {
                    listener.onUpstreamFinish();
                    shutdown();
                }

                @Override
                public void onUpstreamFailure(Throwable error) {
                    listener.onUpstreamFailure(error);
                    shutdown();
                }
            });
        }
    }

}

package com.ibm.ws.microprofile.reactive.streams;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import org.eclipse.microprofile.reactive.streams.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.spi.Graph;
import org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine;
import org.eclipse.microprofile.reactive.streams.spi.UnsupportedStageException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.lightbend.microprofile.reactive.streams.zerodep.BuiltGraph;
import com.lightbend.microprofile.reactive.streams.zerodep.ReactiveStreamsEngineImpl;

@Component(name = "com.ibm.ws.microprofile.reactive.streams.ReactiveStreamsEngineImpl", service = { ReactiveStreamsEngine.class }, property = { "service.vendor=IBM" }, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class WASReactiveStreamsEngineImpl extends ReactiveStreamsEngineImpl implements ReactiveStreamsEngine {

    private static final TraceComponent tc = Tr.register(ReactiveStreamsEngineImpl.class);

    private static ReactiveStreamsEngine singleton = null;

    @Reference(target = "(component.name=com.ibm.ws.threading)")
    private static ExecutorService serverExecutor;

    private ExecutorService executor = null;

    /**
     * @param executor
     */
    public WASReactiveStreamsEngineImpl(ExecutorService ex) {
        // We have a fallback for unit testing
        this.executor = (ex != null) ? ex : ForkJoinPool.commonPool();
    }

    @Override
    public <T> Publisher<T> buildPublisher(Graph graph) throws UnsupportedStageException {
        return BuiltGraph.buildPublisher(executor, graph);
    }

    @Override
    public <T, R> CompletionSubscriber<T, R> buildSubscriber(Graph graph) throws UnsupportedStageException {
        return BuiltGraph.buildSubscriber(executor, graph);
    }

    @Override
    public <T, R> Processor<T, R> buildProcessor(Graph graph) throws UnsupportedStageException {
        return BuiltGraph.buildProcessor(executor, graph);
    }

    @Override
    public <T> CompletionStage<T> buildCompletion(Graph graph) throws UnsupportedStageException {
        return BuiltGraph.buildCompletion(executor, graph);
    }

    /**
     * @return
     */
    public static ReactiveStreamsEngine getEngine() {
        if (singleton == null) {
            singleton = new WASReactiveStreamsEngineImpl(serverExecutor);
        }
        return singleton;
    }

}

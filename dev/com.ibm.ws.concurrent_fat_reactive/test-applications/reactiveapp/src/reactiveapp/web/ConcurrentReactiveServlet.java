/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package reactiveapp.web;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import javax.naming.NamingException;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/ConcurrentReactiveServlet")
public class ConcurrentReactiveServlet extends FATServlet {
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(5);

    @Resource(name = "java:comp/env/concurrent/executorRef")
    private ManagedExecutorService executor;

    @Resource
    private ContextService contextSvcDefault;

    //Handler for Flow.Subscriber throwing an exception in onNext
    BiConsumer<? super Flow.Subscriber<? super ContextCDL>, ? super Throwable> handler = (sub, t) -> {
        AssertionError ae = new AssertionError("Context was not available in Subscriber");
        ae.initCause(t);
        throw ae;
    };

    /**
     * Test that context is available at different points in a flow (publisher, subscriber)
     * when using a ManagerExecutor.
     *
     * @throws Throwable
     */
    @Test
    public void basicFlowTest() throws Exception {
        final ContextCDLImpl continueLatch = new ContextCDLImpl(1);
        ThreadSubscriber subscriber = new ThreadSubscriber();
        try (ThreadPublisher publisher = new ThreadPublisher(executor, handler)) {
            publisher.subscribe(subscriber);
            publisher.offer(continueLatch, null);

            if (!continueLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS)) {
                if (publisher.getClosedException() != null)
                    throw (AssertionError) new AssertionError("Context was not available in Publisher").initCause(publisher.getClosedException());
                else
                    throw new AssertionError("Timed out waiting for CountDownLatch");
            }
        }

        //Check for context in onComplete
        for (long currentTime = Instant.now().toEpochMilli(),
                        endTime = currentTime + TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS); currentTime < endTime; currentTime = Instant.now().toEpochMilli()) {
            if (subscriber.onCompleteResult != null) {
                if (subscriber.onCompleteResult instanceof NamingException) {
                    throw (AssertionError) new AssertionError("Context was not available in Subscriber onComplete").initCause((NamingException) subscriber.onCompleteResult);
                } else {
                    break;
                }
            }
        }
        if (subscriber.onCompleteResult == null)
            throw new AssertionError("Timed out waiting for subscriber.onCompleteResult");

        //Check for context in onError
        try (ThreadPublisher publisher = new ThreadPublisher(executor, handler)) {
            publisher.subscribe(subscriber);
            publisher.closeExceptionally(new Throwable("Ignored Exception"));
        }
        for (long currentTime = Instant.now().toEpochMilli(),
                        endTime = currentTime + TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS); currentTime < endTime; currentTime = Instant.now().toEpochMilli()) {
            if (subscriber.onErrorResult != null) {
                if (subscriber.onErrorResult instanceof NamingException) {
                    throw (AssertionError) new AssertionError("Context was not available in Subscriber onError").initCause((NamingException) subscriber.onErrorResult);
                } else {
                    break;
                }
            }
        }
        if (subscriber.onErrorResult == null)
            throw new AssertionError("Timed out waiting for subscriber.onErrorResult");
    }

    /**
     * Test that context is available using a Managed Executor in a more complicated flow:
     * Publisher -> Processor -> Subscriber
     */
    @Test
    public void processorFlowTest() throws Exception {
        final ContextCDLImpl continueLatch = new ContextCDLImpl(2);

        try (ThreadPublisher publisher = new ThreadPublisher(executor, handler); ThreadProcessor processor = new ThreadProcessor(executor, handler);) {
            ThreadSubscriber subscriber = new ThreadSubscriber();

            publisher.subscribe(processor);
            processor.subscribe(subscriber);
            publisher.offer(continueLatch, null);

            if (!continueLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS)) {
                if (publisher.getClosedException() != null)
                    throw (AssertionError) new AssertionError("Context was not available in Publisher").initCause(publisher.getClosedException());
                else if (processor.getClosedException() != null)
                    throw (AssertionError) new AssertionError("Context was not available in Processor").initCause(publisher.getClosedException());
                else
                    throw new AssertionError("Timed out occurred waiting for CountDownLatch");
            }

        }
    }

    /**
     * Test that an object contextualized with Context Service has access to context in a Flow
     * Publisher -> Processor -> Subscriber
     */
    @Test
    public void contextualizedItemTest() throws Exception {
        ContextCDL contextualCDL = contextSvcDefault.createContextualProxy(new ContextCDLImpl(2), ContextCDL.class);
        ExecutorService es = Executors.newFixedThreadPool(3); //Non-managed executor to test Context Service

        try (ThreadPublisher publisher = new ThreadPublisher(es, handler); ThreadProcessor processor = new ThreadProcessor(es, handler);) {

            ThreadSubscriber subscriber = new ThreadSubscriber();

            publisher.subscribe(processor);
            processor.subscribe(subscriber);
            publisher.offer(contextualCDL, null);

            if (!contextualCDL.await(TIMEOUT_NS, TimeUnit.NANOSECONDS)) {
                if (publisher.getClosedException() != null)
                    throw (AssertionError) new AssertionError("Context was not available in Publisher").initCause(publisher.getClosedException());
                else if (processor.getClosedException() != null)
                    throw (AssertionError) new AssertionError("Context was not available in Processor").initCause(publisher.getClosedException());
                else
                    throw new AssertionError("Timed out waiting for CountDownLatch");
            }

        }

    }

    /**
     * Test that a contextualized ThreadSubscriber has access to Context.
     * Publisher -> Subscriber
     */
    @Test
    public void contextualizedFlowTest() throws Exception {
        Flow.Subscriber<ContextCDL> subProxy = contextSvcDefault.createContextualProxy(new ThreadSubscriber(), Flow.Subscriber.class);

        ExecutorService es = Executors.newFixedThreadPool(3); //Non-managed executor to test Context Service
        final ContextCDLImpl continueLatch = new ContextCDLImpl(1);
        try (ThreadPublisher publisher = new ThreadPublisher(es, handler)) {

            publisher.subscribe(subProxy);
            publisher.offer(continueLatch, null);

            if (!continueLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS)) {
                if (publisher.getClosedException() != null)
                    throw (AssertionError) new AssertionError("Context was not available in Publisher").initCause(publisher.getClosedException());
                else
                    throw new AssertionError("Timed out waiting for CountDownLatch, context may not have been available on Subscriber");
            }
        }
    }

    /**
     * Test that a ContextualSubscriber has access to Context.
     * Publisher -> Subscriber
     */
    @Test
    public void contextualSubscriberTest() throws Exception {
        ThreadSubscriber subscriber = new ThreadSubscriber();
        Subscriber<ContextCDL> contextualSubscriber = contextSvcDefault.contextualSubscriber(subscriber);

        ExecutorService es = Executors.newFixedThreadPool(3); //Non-managed executor to test Context Service
        final ContextCDLImpl continueLatch = new ContextCDLImpl(1);
        try (ThreadPublisher publisher = new ThreadPublisher(es, handler)) {

            publisher.subscribe(contextualSubscriber);
            publisher.offer(continueLatch, null);

            if (!continueLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS)) {
                if (publisher.getClosedException() != null)
                    throw (AssertionError) new AssertionError("Context was not available in Publisher").initCause(publisher.getClosedException());
                else
                    throw new AssertionError("Timed out waiting for CountDownLatch, context may not have been available on Subscriber");
            }
        }

        //Check for context in onComplete
        for (long currentTime = Instant.now().toEpochMilli(),
                        endTime = currentTime + TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS); currentTime < endTime; currentTime = Instant.now().toEpochMilli()) {
            if (subscriber.onCompleteResult != null) {
                if (subscriber.onCompleteResult instanceof NamingException) {
                    throw (AssertionError) new AssertionError("Context was not available in Subscriber onComplete").initCause((NamingException) subscriber.onCompleteResult);
                } else {
                    break;
                }
            }
        }
        if (subscriber.onCompleteResult == null)
            throw new AssertionError("Timed out waiting for subscriber.onCompleteResult");

        //Check for context in onError
        try (ThreadPublisher publisher = new ThreadPublisher(executor, handler)) {
            publisher.subscribe(subscriber);
            publisher.closeExceptionally(new Throwable("Ignored Exception"));
        }
        for (long currentTime = Instant.now().toEpochMilli(),
                        endTime = currentTime + TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS); currentTime < endTime; currentTime = Instant.now().toEpochMilli()) {
            if (subscriber.onErrorResult != null) {
                if (subscriber.onErrorResult instanceof NamingException) {
                    throw (AssertionError) new AssertionError("Context was not available in Subscriber onError").initCause((NamingException) subscriber.onErrorResult);
                } else {
                    break;
                }
            }
        }
        if (subscriber.onErrorResult == null)
            throw new AssertionError("Timed out waiting for subscriber.onErrorResult");
    }

    /**
     * Test that context is available in a ContextualProcessor and it's following ContextualSubscriber:
     * Publisher -> Processor -> Subscriber
     */
    @Test
    public void contextualProcessorTest() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(3); //Non-managed executor to test Context Service

        final ContextCDLImpl continueLatch = new ContextCDLImpl(2);

        try (ThreadPublisher publisher = new ThreadPublisher(es, handler); ThreadProcessor processor = new ThreadProcessor(es, handler);) {

            Processor<ContextCDL, ContextCDL> contextualProcessor = contextSvcDefault.contextualProcessor(processor);
            Subscriber<ContextCDL> contextualSubscriber = contextSvcDefault.contextualSubscriber(new ThreadSubscriber());

            publisher.subscribe(contextualProcessor);
            contextualProcessor.subscribe(contextualSubscriber);
            publisher.offer(continueLatch, null);

            if (!continueLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS)) {
                if (publisher.getClosedException() != null)
                    throw (AssertionError) new AssertionError("Context was not available in Publisher").initCause(publisher.getClosedException());
                else if (processor.getClosedException() != null)
                    throw (AssertionError) new AssertionError("Context was not available in Processor").initCause(publisher.getClosedException());
                else
                    throw new AssertionError("Timed out waiting for CountDownLatch");
            }

        }
    }

    /**
     * Test that context can be accessed in an Operator and a Flow.Subscriber subscribed to a Multi
     *
     */
    @Test
    public void mutinyTest() throws Exception {
        final ContextCDLImpl continueLatch = new ContextCDLImpl(2);

        MutinySubscriber ms = new MutinySubscriber();

        Multi.createFrom().item(continueLatch).emitOn(executor).call(cdl -> {
            try {
                cdl.checkContext();
                cdl.countDown();
                return Uni.createFrom().item(cdl);
            } catch (NamingException e) {
                return Uni.createFrom().failure(new AssertionError("Context unavailable in function").initCause(e));
            }
        }).subscribe().withSubscriber(ms);

        Object o = ms.getResult().get();
        if (o instanceof AssertionError) {
            throw (AssertionError) o;
        }

        if (!continueLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS))
            throw new AssertionError("Timed out waiting for CountDownLatch, context may not have been available");
    }

}

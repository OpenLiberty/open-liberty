/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.microprofile.reactive.streams.test.context;

import static com.ibm.ws.microprofile.reactive.streams.test.suite.FATSuite.MP50_RS30_ID;
import static com.ibm.ws.microprofile.reactive.streams.test.suite.FATSuite.MP60_RS30_ID;
import static com.ibm.ws.microprofile.reactive.streams.test.suite.FATSuite.MP61_RS30_ID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;

/**
 * Test that reactive streams are run with the correct thread context.
 * <p>
 * In RSO 1.0, streams are automatically run asynchronously so we have to test that they're run with the correct context.
 * <p>
 * In RSO 3.0, we no longer do any asynchronous execution automatically, so these tests should pass trivially because everything runs on the original thread.
 * <p>
 * We have some tests which use blocking operations which only run on RSO 1.0. (Best practice is that reactive streams should not include blocking operations and should instead
 * call operations that run asynchronously and return a CompletionStage. RSO 1.0 can cope with blocking operations which run immediately when a stream is started but RSO 3.0
 * expects the user to manually run the run method asynchronously if this is required.)
 */
@WebServlet("/ReactiveStreamsContextTest")
public class ReactiveStreamsContextTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    Principal principle;

    @Inject
    IntegerSubscriber integerSubscriber;

    @Inject
    ThreadContextBean threadContextBean;

    /*
     * Another simple test that plumbs a list to Subscriber
     */
    @Test
    public void helloReactiveWorld() throws Throwable {

        assertNotNull("Servlet Principle is null", principle);
        System.out.println(principle.toString());

        PublisherBuilder<Integer> data = ReactiveStreams.of(1, 2, 3, 4, 5);
        ProcessorBuilder<Integer, Integer> filter = ReactiveStreams.<Integer> builder().dropWhile(t -> t < 3);

        data.via(filter).to(integerSubscriber).run();

        int loops = 0;
        while (!integerSubscriber.isComplete() && loops++ < 10 * 60 * 5) {
            Thread.sleep(100);
        }

        Throwable error = integerSubscriber.getError();
        if (error != null) {
            throw error;
        }

        ArrayList<Integer> results = integerSubscriber.getResults();
        assertEquals(3, results.size());
        for (int i = 0; i < 3; i++) {
            int res = results.get(i);
            assertEquals(i + 3, res);
        }
    }

    @Test
    public void testGetCdiInStream() throws Exception {
        CompletionStage<CDI<Object>> result = ReactiveStreams.of(1)
                        .map((x) -> threadContextBean.getCdi())
                        .findFirst()
                        .run()
                        .thenApply(Optional::get);
        CompletionStageResult.from(result).assertResult(instanceOf(CDI.class));
    }

    @Test
    public void testGetCdiAfterResult() throws Exception {
        CompletableFuture<Void> latch = new CompletableFuture<>();
        CompletionStage<CDI<Object>> result = ReactiveStreams.of(1, 2, 3, 4, 5)
                        .flatMapCompletionStage(i -> latch.thenApply(x -> i))
                        .collect(Collectors.toList())
                        .run()
                        .thenApply((x) -> threadContextBean.getCdi());

        latch.complete(null);

        CompletionStageResult.from(result).assertResult(instanceOf(CDI.class));
    }

    @Test
    @SkipForRepeat({ MP50_RS30_ID, MP60_RS30_ID, MP61_RS30_ID })
    public void testGetCdiAfterResultBlocking() throws Exception {
        CompletableFuture<Void> latch = new CompletableFuture<>();
        CompletionStage<CDI<Object>> result = ReactiveStreams.of(1, 2, 3, 4, 5)
                        .map(blockingWaitFor(latch))
                        .collect(Collectors.toList())
                        .run()
                        .thenApply((x) -> threadContextBean.getCdi());

        latch.complete(null);

        CompletionStageResult.from(result).assertResult(instanceOf(CDI.class));
    }

    @Test
    public void testGetBeanManagerViaJndiInStream() {
        CompletionStage<BeanManager> result = ReactiveStreams.of(1)
                        .map((x) -> threadContextBean.getBeanManagerViaJndi())
                        .findFirst()
                        .run()
                        .thenApply(Optional::get);
        CompletionStageResult.from(result).assertResult(instanceOf(BeanManager.class));
    }

    @Test
    public void testGetBeanManagerViaJndiAfterResult() {
        CompletableFuture<Void> latch = new CompletableFuture<>();
        CompletionStage<BeanManager> result = ReactiveStreams.of(1)
                        .flatMapCompletionStage(i -> latch.thenApply(x -> i))
                        .findFirst()
                        .run()
                        .thenApply((x) -> threadContextBean.getBeanManagerViaJndi());

        latch.complete(null);

        CompletionStageResult.from(result).assertResult(instanceOf(BeanManager.class));
    }

    @Test
    @SkipForRepeat({ MP50_RS30_ID, MP60_RS30_ID, MP61_RS30_ID })
    public void testGetBeanManagerViaJndiAfterResultBlocking() {
        CompletableFuture<Void> latch = new CompletableFuture<>();
        CompletionStage<BeanManager> result = ReactiveStreams.of(1)
                        .map(blockingWaitFor(latch))
                        .findFirst()
                        .run()
                        .thenApply((x) -> threadContextBean.getBeanManagerViaJndi());

        latch.complete(null);

        CompletionStageResult.from(result).assertResult(instanceOf(BeanManager.class));
    }

    @Test
    public void testLoadClassFromTcclInStream() {
        CompletionStage<Class<?>> result = ReactiveStreams.of(1)
                        .map((x) -> threadContextBean.loadClassWithTccl())
                        .findFirst()
                        .run()
                        .thenApply(Optional::get);
        CompletionStageResult.from(result).assertResult(equalTo(ThreadContextBean.class));
    }

    @Test
    public void testLoadClassFromTcclAfterResult() {
        CompletableFuture<Void> latch = new CompletableFuture<>();
        CompletionStage<Class<?>> result = ReactiveStreams.of(1)
                        .flatMapCompletionStage(i -> latch.thenApply(x -> i))
                        .findFirst()
                        .run()
                        .thenApply((x) -> threadContextBean.loadClassWithTccl());
        latch.complete(null);
        CompletionStageResult.from(result).assertResult(equalTo(ThreadContextBean.class));
    }

    @Test
    @SkipForRepeat({ MP50_RS30_ID, MP60_RS30_ID, MP61_RS30_ID })
    public void testLoadClassFromTcclAfterResultBlocking() {
        CompletableFuture<Void> latch = new CompletableFuture<>();
        CompletionStage<Class<?>> result = ReactiveStreams.of(1)
                        .map(blockingWaitFor(latch))
                        .findFirst()
                        .run()
                        .thenApply((x) -> threadContextBean.loadClassWithTccl());
        latch.complete(null);
        CompletionStageResult.from(result).assertResult(equalTo(ThreadContextBean.class));
    }

    @Test
    public void testGetConfigValueFromInjectedBeanInStream() {
        CompletionStage<String> result = ReactiveStreams.of(1)
                        .map((x) -> threadContextBean.getConfigValueFromInjectedBean())
                        .findFirst()
                        .run()
                        .thenApply(Optional::get);
        CompletionStageResult.from(result).assertResult(is("foobar"));
    }

    @Test
    public void testGetConfigValueFromInjectedBeanAfterResult() {
        CompletableFuture<Void> latch = new CompletableFuture<>();
        CompletionStage<String> result = ReactiveStreams.of(1)
                        .flatMapCompletionStage(i -> latch.thenApply(x -> i))
                        .findFirst()
                        .run()
                        .thenApply((x) -> threadContextBean.getConfigValueFromInjectedBean());
        latch.complete(null);
        CompletionStageResult.from(result).assertResult(is("foobar"));
    }

    @Test
    @SkipForRepeat({ MP50_RS30_ID, MP60_RS30_ID, MP61_RS30_ID })
    public void testGetConfigValueFromInjectedBeanAfterResultBlocking() {
        CompletableFuture<Void> latch = new CompletableFuture<>();
        CompletionStage<String> result = ReactiveStreams.of(1)
                        .map(blockingWaitFor(latch))
                        .findFirst()
                        .run()
                        .thenApply((x) -> threadContextBean.getConfigValueFromInjectedBean());
        latch.complete(null);
        CompletionStageResult.from(result).assertResult(is("foobar"));
    }

    private <T> Function<T, T> blockingWaitFor(Future<?> latch) {
        return (t) -> {
            try {
                latch.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return t;
        };
    }

}

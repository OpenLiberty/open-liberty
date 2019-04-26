/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.streams.test.context;

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

import componenttest.app.FATServlet;

/**
 *
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
                        .map(waitFor(latch))
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
                        .map(waitFor(latch))
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
                        .map(waitFor(latch))
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
                        .map(waitFor(latch))
                        .findFirst()
                        .run()
                        .thenApply((x) -> threadContextBean.getConfigValueFromInjectedBean());
        latch.complete(null);
        CompletionStageResult.from(result).assertResult(is("foobar"));
    }

    private <T> Function<T, T> waitFor(Future<?> latch) {
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

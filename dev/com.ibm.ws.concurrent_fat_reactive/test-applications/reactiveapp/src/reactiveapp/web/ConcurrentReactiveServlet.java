/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import componenttest.app.FATServlet;
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

    /**
     * Test that context is available at different points in a flow (publisher, subscriber)
     * when using a ManagerExecutor.
     */
    @Test
    public void basicFlowTest() throws Exception {
        final ContextCDLImpl continueLatch = new ContextCDLImpl(1);
        try (ThreadPublisher publisher = new ThreadPublisher(executor)) {
            ThreadSubscriber subscriber = new ThreadSubscriber();
            publisher.subscribe(subscriber);
            publisher.offer(continueLatch, null);

            if (publisher.getClosedException() != null)
                throw (AssertionError) new AssertionError("Context was not available in Publisher").initCause(publisher.getClosedException());
            if (subscriber.getClosedException() != null)
                throw (AssertionError) new AssertionError("Context was not available in Subscriber").initCause(subscriber.getClosedException());

            assertTrue("Timeout occurred waiting for CountDownLatch", continueLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        }
    }

    /**
     * Test that context is available using a Managed Executor in a more complicated flow:
     * Publisher -> Processor -> Subscriber
     */
    @Test
    public void processorFlowTest() throws Exception {
        final ContextCDLImpl continueLatch = new ContextCDLImpl(2);

        try (ThreadPublisher publisher = new ThreadPublisher(executor); ThreadProcessor processor = new ThreadProcessor(executor);) {
            ThreadSubscriber subscriber = new ThreadSubscriber();

            publisher.subscribe(processor);
            processor.subscribe(subscriber);
            publisher.offer(continueLatch, null);

            if (publisher.getClosedException() != null)
                throw (AssertionError) new AssertionError("Context was not available in Publisher").initCause(publisher.getClosedException());
            if (processor.getClosedException() != null)
                throw (AssertionError) new AssertionError("Context was not available in Processor").initCause(publisher.getClosedException());
            if (subscriber.getClosedException() != null)
                throw (AssertionError) new AssertionError("Context was not available in Subscriber").initCause(subscriber.getClosedException());

            assertTrue("Timeout occurred waiting for CountDownLatch", continueLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        }
    }

    /**
     * Test that an object contextualized with Context Service has access to context in a Flow
     * Publisher -> Processor -> Subscriber
     */
    @Test
    public void contextualizedFlowTest() throws Exception {
        ContextCDL contextualCDL = contextSvcDefault.createContextualProxy(new ContextCDLImpl(2), ContextCDL.class);
        ExecutorService es = Executors.newFixedThreadPool(3); //Non-managed executor to test Context Service

        try (ThreadPublisher publisher = new ThreadPublisher(es); ThreadProcessor processor = new ThreadProcessor(es);) {

            ThreadSubscriber subscriber = new ThreadSubscriber();

            publisher.subscribe(processor);
            processor.subscribe(subscriber);
            publisher.offer(contextualCDL, null);

            if (publisher.getClosedException() != null)
                throw (AssertionError) new AssertionError("Context was not available in Publisher").initCause(publisher.getClosedException());
            if (processor.getClosedException() != null)
                throw (AssertionError) new AssertionError("Context was not available in Processor").initCause(publisher.getClosedException());
            if (subscriber.getClosedException() != null)
                throw (AssertionError) new AssertionError("Context was not available in Subscriber").initCause(subscriber.getClosedException());

            assertTrue("Timeout occurred waiting for CountDownLatch", contextualCDL.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        }

    }

}

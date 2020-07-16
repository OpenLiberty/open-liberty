/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.faulttolerance30.internal.test.context;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Tests for the integration of Fault Tolerance and MP Context Propagation
 */
@SuppressWarnings("serial")
@WebServlet("/contextTest")
public class ContextTestServlet extends FATServlet {

    @Inject
    private ContextTestBean contextTestBean;

    @Inject
    private RequestScopedBean requestScopedBean;

    @Test
    public void testCustomContextAsync() throws InterruptedException, ExecutionException, TimeoutException {
        String myContextValue = "testValue";
        TestContextProvider.setValue(myContextValue);

        contextTestBean.runInAsync(() -> {
            assertEquals(myContextValue, TestContextProvider.getValue());
        }).get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testCustomContextFallback() throws InterruptedException, ExecutionException, TimeoutException {
        String myContextValue = "testValueFallback";
        TestContextProvider.setValue(myContextValue);

        contextTestBean.runInFallback(() -> {
            assertEquals(myContextValue, TestContextProvider.getValue());
        }).get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testCustomContextCsAction() throws InterruptedException, ExecutionException, TimeoutException {
        String myContextValue = "testValueCsAction";
        TestContextProvider.setValue(myContextValue);

        CompletableFuture<Void> latch = new CompletableFuture<>();

        CompletableFuture<Void> stage = contextTestBean
                        .waitOnLatchCS(latch)
                        .thenRun(() -> {
                            assertEquals(myContextValue, TestContextProvider.getValue());
                        })
                        .toCompletableFuture();

        latch.complete(null);

        stage.get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testRequestContextAsync() throws InterruptedException, ExecutionException, TimeoutException {
        int id = requestScopedBean.getId();

        contextTestBean.runInAsync(() -> {
            assertEquals(id, requestScopedBean.getId());
        }).get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testRequestContextFallback() throws InterruptedException, ExecutionException, TimeoutException {
        int id = requestScopedBean.getId();

        contextTestBean.runInFallback(() -> {
            assertEquals(id, requestScopedBean.getId());
        }).get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testRequestContextCsAction() throws InterruptedException, ExecutionException, TimeoutException {
        int id = requestScopedBean.getId();

        CompletableFuture<Void> latch = new CompletableFuture<>();

        CompletableFuture<Void> stage = contextTestBean.waitOnLatchCS(latch)
                        .thenRun(() -> {
                            assertEquals(id, requestScopedBean.getId());
                        })
                        .toCompletableFuture();

        latch.complete(null);

        stage.get(5, TimeUnit.SECONDS);
    }
}

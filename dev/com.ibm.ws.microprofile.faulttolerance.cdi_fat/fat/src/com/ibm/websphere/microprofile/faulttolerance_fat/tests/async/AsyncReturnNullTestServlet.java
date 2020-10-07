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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.async;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/AsyncReturnNullTest")
public class AsyncReturnNullTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    AsyncMethodsReturnNullBean nullReturnBean;

    @Inject
    AsyncClassReturnNullBean nullReturnClassBean;

    @Test
    public void testAsyncFutureReturnNull() throws Exception {
        try {
            nullReturnBean.getNullFuture().get();
            fail("No exception thrown");
        } catch (ExecutionException e) {
            checkCorrectErrorMessage(e, "Future");
        }
    }

    @Test
    public void testAsyncCompletionStageReturnNull() throws Exception {
        try {
            nullReturnBean.getNullCompletionStage().toCompletableFuture().get();
            fail("No exception thrown");
        } catch (ExecutionException e) {
            checkCorrectErrorMessage(e, "CompletionStage");
        }
    }

    @Test
    public void testAsyncClassFutureReturnNull() throws Exception {
        try {
            nullReturnClassBean.getNullFuture().get();
            fail("No exception thrown");
        } catch (ExecutionException e) {
            checkCorrectErrorMessage(e, "Future");
        }
    }

    @Test
    public void testAsyncClassCompletionStageReturnNull() throws Exception {
        try {
            nullReturnClassBean.getNullCompletionStage().toCompletableFuture().get();
            fail("No exception thrown");
        } catch (ExecutionException e) {
            checkCorrectErrorMessage(e, "CompletionStage");
        }
    }

    @Test
    public void testAsyncRetryOnNullFuture() throws Exception {
        try {
            nullReturnBean.getNullFutureRetry().get();
            fail("No exception thrown");
        } catch (ExecutionException e) {
            assertEquals("The @Asynchronous method should not retry if null is returned", 1, nullReturnBean.getFutureRetryCounter());
        }

    }

    @Test
    public void testAsyncRetryOnNullCompletionStage() throws Exception {
        try {
            nullReturnBean.getNullCompletionStageRetry().toCompletableFuture().get();
            fail("No exception thrown");
        } catch (ExecutionException e) {
            assertEquals("The @Asynchronous method should not retry if null is returned", 1, nullReturnBean.getCompletionStageRetryCounter());
        }
    }

    public void checkCorrectErrorMessage(ExecutionException e, String type) {
        assertThat(e.getCause(), instanceOf(NullPointerException.class));
        assertThat(e.getCause().getMessage(), containsString("CWMFT0003W"));
        if (type.equals("Future")) {
            assertThat(e.getCause().getMessage(), containsString("getNullFuture"));
        } else if (type.equals("CompletionStage")) {
            assertThat(e.getCause().getMessage(), containsString("getNullCompletionStage"));
        }
    }

}

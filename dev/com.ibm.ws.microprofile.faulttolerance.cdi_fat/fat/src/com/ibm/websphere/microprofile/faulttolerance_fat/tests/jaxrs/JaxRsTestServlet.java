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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.jaxrs;

import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants.TEST_TIMEOUT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet("/JaxRsTest")
public class JaxRsTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    TestClientBean bean;

    @Test
    public void testJaxRsWorking() throws InterruptedException, ExecutionException, TimeoutException {
        String result = bean.callWorkingEndpoint().toCompletableFuture().get(TEST_TIMEOUT, MILLISECONDS);
        assertEquals("OK", result);
    }

    @Test
    public void testJaxRsFailing() throws InterruptedException, ExecutionException, TimeoutException {
        String result = bean.callFailingEndpoint().toCompletableFuture().get(TEST_TIMEOUT, MILLISECONDS);
        assertEquals("FALLBACK", result);
    }

    @Test
    @Mode(TestMode.FULL)
    public void testJaxRsSlow() {
        CompletionStage<String> result = bean.callSlowEndpoint();
        assertThrows(org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class, result);
    }

    private void assertThrows(Class<? extends Throwable> exception, CompletionStage<?> completionStage) {
        try {
            completionStage.toCompletableFuture().get(TEST_TIMEOUT, MILLISECONDS);
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(exception));
        } catch (InterruptedException e) {
            throw new AssertionError("Interrupted while waiting for result", e);
        } catch (TimeoutException e) {
            throw new AssertionError("Timed out while waiting for result", e);
        }
    }

}

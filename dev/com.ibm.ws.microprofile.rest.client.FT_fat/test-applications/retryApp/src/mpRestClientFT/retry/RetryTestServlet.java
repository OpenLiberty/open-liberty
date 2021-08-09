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
package mpRestClientFT.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/RetryTestServlet")
public class RetryTestServlet extends FATServlet {
    private final static Logger _log = Logger.getLogger(RetryTestServlet.class.getName());

    final static String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/retryApp/";

    @RestClient
    @Inject
    RetryClient client;

    @Inject
    Driver driver;
    /**
     * Tests multi-stage CompletionStage (async) from a Rest Client.
     * Tests baseUri API.
     * 
     */
    @Test
    public void testRetryOnceOnFailThenSucceed(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        RetryRemoteResource.failThenSucceed.set(1);
        String success = client.failThenSucceed();
        assertEquals("Success", success);
        assertEquals(3, RetryRemoteResource.failThenSucceed.get());
    }

    @Test
    public void testRetryOnceOnFailThenSucceed_separateMethod(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        RetryRemoteResource.failThenSucceed.set(1);
        String success = driver.failThenSucceed();
        assertEquals("Success", success);
        assertEquals(3, RetryRemoteResource.failThenSucceed.get());
    }

    @Test
    public void testCustomLoggingInterceptorInvoked(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        LoggableInterceptor.invocations.clear();
        client.alwaysSucceed();
        assertEquals(1, LoggableInterceptor.invocations.size());
        Integer invocationCount = LoggableInterceptor.invocations.get(RetryClient.class.getName()+"."+"alwaysSucceed");
        assertNotNull(invocationCount);
        assertEquals(1, (int) invocationCount);
    }

    @Test
    public void testFallbackMethod(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        assertEquals("Success", client.useFallbackMethod());
    }

    @Test
    public void testDefaultFallbackMethod(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        assertEquals("defaultFallback", client.useDefaultFallbackMethod());
    }

    @Test
    public void testFallbackHandlerClass(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        assertEquals("MyFallbackClass", client.useDefaultFallbackClass());
    }
}
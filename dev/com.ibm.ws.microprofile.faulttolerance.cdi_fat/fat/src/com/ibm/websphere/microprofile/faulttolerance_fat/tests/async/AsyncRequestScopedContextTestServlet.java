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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ContextNotActiveException;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import junit.framework.Assert;

@WebServlet("/AsyncRequestScopedContextTest")
public class AsyncRequestScopedContextTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    AsyncRequestScopedBean requestScopedBean;

    @Inject
    AsyncApplicationScopedBean applicationScopedBean;

    @Resource
    ManagedExecutorService threadPool;

    /**
     * If the request context is not set, asynchronously calling a @RequestScoped bean from an @ApplicationScoped bean
     * would throw a ContextNotActiveException
     */
    @Test
    public void testAsyncRequestContext() throws Exception {
        Future<String> future = applicationScopedBean.callRequestScopedBeanAsynchronously();
        String returned = future.get(5, TimeUnit.SECONDS);
        Assert.assertEquals("@RequestScoped test string", returned);
    }

    /**
     * This test uses a ManagedExecutorService object to generate and run requests on separate threads.
     * It does this to simulate methods running asynchronously, not through the @Asynchronous annotation.
     * The test runs the methods with FT annotations to ensure they do not set the context where it shouldn't be set
     */
    @Test
    public void testAsyncRequestContextNotAccidentlySet() throws Exception {

        // Test that the ManagedExecutorService (threadPool) actually works
        Future<String> future1 = threadPool.submit(() -> applicationScopedBean.testMethod());
        String returned1 = future1.get(5, TimeUnit.SECONDS);
        Assert.assertEquals("@ApplicationScoped test string", returned1);

        // The request context shouldn't be active on a second thread created by the ManagedExecutorService (threadPool)
        // Calling a @RequestScoped bean should throw a ContextNotActiveException
        try {
            Future<String> future3 = threadPool.submit(() -> requestScopedBean.getString());
            future3.get(5, TimeUnit.SECONDS);
            fail("No exception thrown");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(ContextNotActiveException.class));
        }

        // Even with fault tolerance configured, calling the @RequestScoped bean should still throw a ContextNotActiveException
        try {
            Future<String> future4 = threadPool.submit(() -> applicationScopedBean.callRequestScopedBeanWithFTConfigured());
            future4.get(5, TimeUnit.SECONDS);
            fail("No exception thrown");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(ContextNotActiveException.class));
        }
    }
}

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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.ejb;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.EJBAccessException;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/async-ejb")
public class AsyncEJBServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    AsyncEJBThreadContextBean threadContextBean;

    @Test
    public void testAsyncEjbSecurity(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Future<String> value = threadContextBean.callSecuredEjb();
        try {
            value.get();
            fail("Able to access EJB without logging in");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(EJBAccessException.class));
        }

        try {
            req.login("test", "test");
            Future<String> value2 = threadContextBean.callSecuredEjb();
            assertThat(value2.get(), is("OK"));
        } finally {
            req.logout();
        }
    }

    @Test
    public void testAsyncEjbPrincipal(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try {
            req.login("test", "test");
            Future<Principal> value = threadContextBean.getEjbPrincipal();
            assertThat(value.get().getName(), equalTo("test"));
        } finally {
            req.logout();
        }
    }

    /**
     * Test that the thread context is set during a fallback method
     */
    @Test
    public void testAsyncEjbSecurityFallback(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Future<String> value = threadContextBean.fallbackToSecuredEjb();
        try {
            value.get();
            fail("Able to access EJB without logging in");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(EJBAccessException.class));
        }

        try {
            req.login("test", "test");
            Future<String> value2 = threadContextBean.fallbackToSecuredEjb();
            assertThat(value2.get(), is("OK"));
        } finally {
            req.logout();
        }
    }

}

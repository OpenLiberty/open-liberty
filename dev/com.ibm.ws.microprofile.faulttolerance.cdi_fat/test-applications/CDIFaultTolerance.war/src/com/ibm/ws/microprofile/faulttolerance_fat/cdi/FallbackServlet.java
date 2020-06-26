/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.suite.BasicTest;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.FallbackBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.FallbackBeanWithoutRetry;
import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;
import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class Test
 */
@WebServlet("/fallback")
public class FallbackServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    FallbackBean bean;

    @Inject
    FallbackBeanWithoutRetry beanWithoutRetry;

    @Test
    public void testFallback(HttpServletRequest request,
                             HttpServletResponse response) throws ServletException, IOException, ConnectException, NoSuchMethodException, SecurityException {
        //should be retried twice and then fallback and we get a result
        Connection connection = bean.connectA();
        String data = connection.getData();
        assertThat(data, equalTo("Fallback for: connectA - data!"));
        assertThat("Call count", bean.getConnectCountA(), is(3));
    }

    @BasicTest
    @Test
    public void testFallbackWithoutRetry(HttpServletRequest request,
                                         HttpServletResponse response) throws ServletException, IOException, ConnectException, NoSuchMethodException, SecurityException {
        //should fallback immediately
        Connection connection = beanWithoutRetry.connectA();
        String data = connection.getData();
        assertThat(data, equalTo("Fallback for: connectA - data!"));
        assertThat("Call count", beanWithoutRetry.getConnectCountA(), is(1));
    }

    @Test
    public void testFallbackHandlerConfig() throws ConnectException {
        // Fallback handler overridden as FallbackHandler2 in config
        Connection connection = beanWithoutRetry.connectB();
        assertThat(connection.getData(), containsString("MyFallbackHandler2"));
    }

    @Test
    public void testFallbackMethodConfig() throws ConnectException {
        // Fallback method overriden as connectFallback2 in config
        Connection connection = beanWithoutRetry.connectC();
        assertThat(connection.getData(), equalTo("connectFallback2"));
    }

    @Test
    public void testFallbackAsync() throws Exception {
        Future<Connection> future = bean.connectC();
        Connection connection = future.get();
        assertThat("Result data", connection.getData(), equalTo("AsyncFallbackHandler: connectC"));
        assertThat("Call count", bean.getConnectCountC(), equalTo(3));
    }

    @Test
    public void testFallbackMethodAsync() throws Exception {
        Future<Connection> future = bean.connectD();
        Connection connection = future.get();
        assertThat("Result data", connection.getData(), equalTo("fallbackAsync"));
        assertThat("Call count", bean.getConnectCountD(), equalTo(3));
    }

}

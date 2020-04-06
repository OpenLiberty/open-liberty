/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@MinimumJavaLevel(javaLevel = 7)
public class CDITests extends LoggingTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_cdiServer");

    protected WebResponse verifyResponse(String resource, String expectedResponse) throws Exception {
        return SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), resource, expectedResponse);
    }

    protected WebResponse verifyResponse(WebBrowser wb, String resource, String[] expectedResponses, String[] unexpectedResponses) throws Exception {
        return SHARED_SERVER.verifyResponse(wb, resource, expectedResponses, unexpectedResponses);
    }

    @Test
    @Mode(TestMode.LITE)
    public void testServletInjection() throws Exception {
        WebBrowser wb = createWebBrowserForTestCase();
        String[] expected = { "Test Exit", "ConstructorBean::Servlet", "MethodBean::Servlet", "ServletFieldBean", "ProducerInjected::Servlet", "postConstructCalled::Servlet" };
        String[] unexpected = { "Test Exception" };
        verifyResponse(wb, "/CDI12Test/CDIServletInjected", expected, unexpected);
    }

    @Test
    @Mode(TestMode.LITE)
    public void testServletIntercepor() throws Exception {
        WebBrowser wb = createWebBrowserForTestCase();
        String[] expected = { "Test Passed! InterceptedBean : ServiceMethodInterceptor was called.",
                             "Test Passed! CDIServletIntercepted : SendResponseInterceptor was called.",
                             "Test Passed! InterceptedBean : ServiceMethodInterceptor was called." };
        String[] unexpected = { "Test Failed!" };
        verifyResponse(wb, "/CDI12Test/CDIServletIntercepted", expected, unexpected);
    }

    @Test
    @Mode(TestMode.LITE)
    public void testAsyncListeenerCDI() throws Exception {
        WebBrowser wb = createWebBrowserForTestCase();
        String[] expected = { "onStartAsync :class cdi.beans.ConstructorBean:",
                             "onStartAsync :class cdi.beans.MethodBean:",
                             "onStartAsync :class cdi.beans.AsyncListenerFieldBean:",
                             "onStartAsync :Interceptor was called:",
                             "onStartAsync ::postConstructCalled:",
                             "onComplete :class cdi.beans.ConstructorBean:",
                             "onComplete :class cdi.beans.MethodBean:",
                             "onComplete :class cdi.beans.AsyncListenerFieldBean:",
                             "onComplete :Interceptor was called:",
                             "onStartAsync ::postConstructCalled:" };
        String[] unexpected = { "Test Exception" };
        verifyResponse(wb, "/CDI12Test/CDIAsyncServlet", expected, unexpected);
    }
}

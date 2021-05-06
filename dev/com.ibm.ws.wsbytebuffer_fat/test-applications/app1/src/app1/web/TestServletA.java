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
package app1.web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet("/TestServletA")
public class TestServletA extends FATServlet {

    // By extending FATServlet and using @TestServlet in the client side test class, @Test annotations
    // can be added directly to the test servlet.
    // In this test servlet, each @Test method is invoked in its own HTTP GET request.

    @Test
    public void basicTest() throws Exception {
        System.out.println("Test is running in an HttpServlet");
        Assert.assertTrue("Can also use JUnit assertions", true);
    }

    @Test
    public void testHttpServletRequest(HttpServletRequest request, HttpServletResponse resp) throws Exception {
        System.out.println("You can also use thee (HttpServletRequest, HttpServletResponse) signature " +
                           " on a test method if you need to access the underlying HTTP request/response");
        System.out.println("Got HTTP params: " + request.getParameterMap());
        resp.getWriter().println("Running test method 'testHttpServletRequest'");
    }

    @Test
    @Mode(TestMode.LITE)
    public void liteTest() throws Exception {
        System.out.println("LITE test is running.");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testFull() throws Exception {
        System.out.println("This test should only run in Full or higher mode!");
    }

    @Test
    @Mode(TestMode.QUARANTINE)
    public void testQuarantine() throws Exception {
        System.out.println("This test should only run in Quarantine mode!");
    }
}

/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.openliberty.wsoc.util.wsoc.WsocTest;

/**
 *
 */
public class SingleRequestClientTestRunner extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 8917212525932407301L;

    private static final Logger LOG = Logger.getLogger(SingleRequestClientTestRunner.class.getName());

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {

        String className = req.getParameter("classname");
        String testName = req.getParameter("testname");
        PrintWriter pw = res.getWriter();
        String host = (req.getParameter("targethost") != null) ? req.getParameter("targethost") : req.getServerName();
        int port = (req.getParameter("targetport") != null) ? Integer.valueOf(req.getParameter("targetport")) : req.getServerPort();
        int secureport = (req.getParameter("secureport") != null) ? Integer.valueOf(req.getParameter("secureport")) : 0;

        boolean secure = (req.getParameter("secure") != null) ? true : false;
        res.setStatus(200);

        LOG.info("Running outbound websocket test " + testName + " + to " + host + " on port " + port);

        if (testName == null) {
            pw.println("Failure:  No testname parameter provided so no test to run...");
            LOG.info("Failure:  No testname parameter provided so no test to run...");
            return;
        }

        try {

            WsocTest wt;
            if (secure) {
                wt = new WsocTest(host, secureport, secure);
                wt.setAltPort(port);
            }
            else {
                wt = new WsocTest(host, port, secure);

            }

            Object o = createTestClass("io.openliberty.wsoc.tests.all." + className, wt);

            Method method = o.getClass().getMethod(testName);
            method.invoke(o);

            pw.print("SuccessfulTest");
            LOG.info("Successful test");

        } catch (Exception e) {
            Throwable f = e.getCause();
            if (f != null) {
                f.printStackTrace(pw);
                f.printStackTrace();
            }
            else {
                e.printStackTrace();
                e.printStackTrace(pw);
            }
        } catch (Throwable f) {
            f.printStackTrace();
            f.printStackTrace(pw);
        }

        pw.close();
        LOG.info("Closing output, and returning from doGet");
    }

    private Object createTestClass(String className, WsocTest wt) {

        Object o = null;

        Class[] argsClass = new Class[] { WsocTest.class };

        Object[] theArgs = new Object[] { wt };
        try {
            Class<?> theclass = Class.forName(className);
            Constructor<?> constructor = theclass.getConstructor(argsClass);
            o = constructor.newInstance(theArgs);
        } catch (Exception e) {
            e.printStackTrace();
        };

        return o;

    }
}

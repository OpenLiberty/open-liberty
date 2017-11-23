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
package com.ibm.ws.cdi12.fat.tests;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

public class CDIManagedBeanInterceptorTest extends LoggingTest {

    private static LibertyServer server;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */

    @Override
    protected ShutDownSharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {

        server = LibertyServerFactory.getStartedLibertyServer("cdi12ManagedBeanTestServer");
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application managedBeanApp started");
    }

    @Test
    public void testManagedBeanInterceptor() throws Exception {

        HttpUtils.findStringInUrl(server, "/managedBeanApp",
                                  "MyNonCDIInterceptor:AroundConstruct called injectedInt:16"
                                                             + " MyCDIInterceptor:AroundConstruct called injectedStr:HelloYou"
                                                             + " MyNonCDIInterceptor:PostConstruct called injectedInt:16"
                                                             + " MyCDIInterceptor:PostConstruct called injectedStr:HelloYou"
                                                             + " MyManagedBean called postConstruct()"
                                                             + " MyNonCDIInterceptor:AroundInvoke called injectedInt:16"
                                                             + " MyCDIInterceptor:AroundInvoke called injectedStr:HelloYou");

    }

    @Test
    @ExpectedFFDC({ "com.ibm.websphere.ejbcontainer.EJBStoppedException" })
    public void preDestroyTest() throws Exception {
        if (server != null) {
            //We wont hit the pre-destroy if we don't trigger the servlet.
            HttpUtils.findStringInUrl(server, "/managedBeanApp", "Begin output");
            server.setMarkToEndOfLog();
            server.restartDropinsApplication("managedBeanApp.war");
            List<String> lines = server.findStringsInLogs("PreDestory");
            Assert.assertEquals("Unexpected number of lines: " + lines.toString(), 3, lines.size());

            Pattern p = Pattern.compile("@PreDestory called (MyNonCDIInterceptor|MyCDIInterceptor|MyManagedBean)");
            for (String line : lines) {
                Assert.assertTrue("Unexpected line: " + line, p.matcher(line).find());
            }
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}

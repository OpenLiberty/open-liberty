/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

public class CDIManagedBeanInterceptorTest extends LoggingTest {

    private static LibertyServer server;
    private static boolean hasSetUp = false;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */

    @Override
    protected SharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {

        if (!hasSetUp){
            hasSetUp = true;
            WebArchive managedBeanApp = ShrinkWrap.create(WebArchive.class, "managedBeanApp.war")
                        .addClass("com.ibm.ws.cdi.test.managedbean.CounterUtil")
                        .addClass("com.ibm.ws.cdi.test.managedbean.ManagedBeanServlet")
                        .addClass("com.ibm.ws.cdi.test.managedbean.MyEJBBean")
                        .addClass("com.ibm.ws.cdi.test.managedbean.interceptors.MyInterceptorBase")
                        .addClass("com.ibm.ws.cdi.test.managedbean.interceptors.MyNonCDIInterceptor")
                        .addClass("com.ibm.ws.cdi.test.managedbean.interceptors.MyCDIInterceptorBinding")
                        .addClass("com.ibm.ws.cdi.test.managedbean.interceptors.MyCDIInterceptor")
                        .addClass("com.ibm.ws.cdi.test.managedbean.MyEJBBeanLocal")
                        .addClass("com.ibm.ws.cdi.test.managedbean.MyManagedBean")
                        .add(new FileAsset(new File("test-applications/managedBeanApp.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");

            server = LibertyServerFactory.getStartedLibertyServer("cdi12ManagedBeanTestServer");
            ShrinkHelper.exportDropinAppToServer(server, managedBeanApp);
            server.waitForStringInLogUsingMark("CWWKZ0001I.*Application managedBeanApp started");


        } else {

           server = LibertyServerFactory.getStartedLibertyServer("cdi12ManagedBeanTestServer");
        }
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
            Assert.assertTrue("Failed to restart the app. This was probably a hicup in the test environment.", server.restartDropinsApplication("managedBeanApp.war"));
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

/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.tests;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class CDIManagedBeanInterceptorTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12ManagedBeanTestServer";
    public static final String MANAGED_BEAN_APP_NAME = "managedBeanApp";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE9, EERepeatActions.EE10, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi.ejb.apps.managedbean.ManagedBeanServlet.class, contextRoot = MANAGED_BEAN_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive managedBeanApp = ShrinkWrap.create(WebArchive.class, MANAGED_BEAN_APP_NAME + ".war")
                                              .addClass(com.ibm.ws.cdi.ejb.apps.managedbean.CounterUtil.class)
                                              .addClass(com.ibm.ws.cdi.ejb.apps.managedbean.ManagedBeanServlet.class)
                                              .addClass(com.ibm.ws.cdi.ejb.apps.managedbean.MyEJBBean.class)
                                              .addClass(com.ibm.ws.cdi.ejb.apps.managedbean.interceptors.MyInterceptorBase.class)
                                              .addClass(com.ibm.ws.cdi.ejb.apps.managedbean.interceptors.MyNonCDIInterceptor.class)
                                              .addClass(com.ibm.ws.cdi.ejb.apps.managedbean.interceptors.MyCDIInterceptorBinding.class)
                                              .addClass(com.ibm.ws.cdi.ejb.apps.managedbean.interceptors.MyCDIInterceptor.class)
                                              .addClass(com.ibm.ws.cdi.ejb.apps.managedbean.MyEJBBeanLocal.class)
                                              .addClass(com.ibm.ws.cdi.ejb.apps.managedbean.MyManagedBean.class)
                                              .add(new FileAsset(new File("test-applications/" + MANAGED_BEAN_APP_NAME + ".war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");

        ShrinkHelper.exportDropinAppToServer(server, managedBeanApp, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @Test
    @ExpectedFFDC({ "com.ibm.websphere.ejbcontainer.EJBStoppedException" })
    public void preDestroyTest() throws Exception {
        //We wont hit the pre-destroy if we don't trigger the servlet.
        runTest(server, MANAGED_BEAN_APP_NAME, "testManagedBeanInterceptor");
        server.setMarkToEndOfLog();
        Assert.assertTrue("Failed to restart the app. This was probably a hicup in the test environment.", server.restartDropinsApplication("managedBeanApp.war"));
        List<String> lines = server.findStringsInLogs("PreDestory");
        Assert.assertEquals("Unexpected number of lines: " + lines.toString(), 3, lines.size());

        Pattern p = Pattern.compile("@PreDestory called (MyNonCDIInterceptor|MyCDIInterceptor|MyManagedBean)");
        for (String line : lines) {
            Assert.assertTrue("Unexpected line: " + line, p.matcher(line).find());
        }
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}

/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.mb.injection.ManagedBeansInjectionServlet;
import com.ibm.ws.mb.interceptor.web.ManagedBeansInterceptorServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class ManagedBeansCdiTest extends FATServletClient {

    public static final String EJB32_WEB_NAME = "ManagedBeanInterceptorWeb";
    public static final String CDI_WEB_NAME = "ManagedBeanInjectionWeb";

    @Server("ManagedBeansCdiServer")
    @TestServlets({ @TestServlet(servlet = ManagedBeansInterceptorServlet.class, contextRoot = EJB32_WEB_NAME),
                    @TestServlet(servlet = ManagedBeansInjectionServlet.class, contextRoot = CDI_WEB_NAME) })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("ManagedBeansCdiServer"))
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers("ManagedBeansCdiServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### ManagedBeanInterceptorApp.ear
        WebArchive ManagedBeanInterceptorWeb = ShrinkHelper.buildDefaultApp("ManagedBeanInterceptorWeb.war", "com.ibm.ws.mb.interceptor.*");
        EnterpriseArchive ManagedBeanInterceptorApp = ShrinkWrap.create(EnterpriseArchive.class, "ManagedBeanInterceptorApp.ear");
        ManagedBeanInterceptorApp.addAsModule(ManagedBeanInterceptorWeb);
        ManagedBeanInterceptorApp = (EnterpriseArchive) ShrinkHelper.addDirectory(ManagedBeanInterceptorApp, "test-applications/ManagedBeanInterceptorApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, ManagedBeanInterceptorApp);

        //#################### ManagedBeanInjectionWeb.war
        WebArchive ManagedBeanInjectionWeb = ShrinkHelper.buildDefaultApp("ManagedBeanInjectionWeb.war", "com.ibm.ws.mb.injection");

        ShrinkHelper.exportDropinAppToServer(server, ManagedBeanInjectionWeb);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}

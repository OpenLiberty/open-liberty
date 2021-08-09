/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.legacy.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.ejb2x.base.cache.web.StatefulOnceServlet;
import com.ibm.ejb2x.base.cache.web.StatefulTranServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class CacheTest {

    @Server("com.ibm.ws.ejbcontainer.legacy.server.notrace")
    @TestServlets({ @TestServlet(servlet = StatefulOnceServlet.class, contextRoot = "StatefulCacheWeb"),
                    @TestServlet(servlet = StatefulTranServlet.class, contextRoot = "StatefulCacheWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.legacy.server.notrace")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.legacy.server.notrace"));

    @BeforeClass
    public static void setUp() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();
        // Use ShrinkHelper to build the ears

        //StatefulCacheEJB.jar StatefulCacheWeb.war

        JavaArchive StatefulCacheEJB = ShrinkHelper.buildJavaArchive("StatefulCacheEJB.jar", "com.ibm.ejb2x.base.cache.ejb.");
        ShrinkHelper.addDirectory(StatefulCacheEJB, "test-applications/StatefulCacheEJB.jar/resources");

        WebArchive StatefulCacheWeb = ShrinkHelper.buildDefaultApp("StatefulCacheWeb.war", "com.ibm.ejb2x.base.cache.web.");

        EnterpriseArchive StatefulCacheApp = ShrinkWrap.create(EnterpriseArchive.class, "StatefulCacheApp.ear");
        StatefulCacheApp.addAsModules(StatefulCacheEJB, StatefulCacheWeb);
        ShrinkHelper.addDirectory(StatefulCacheApp, "test-applications/StatefulCacheApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, StatefulCacheApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
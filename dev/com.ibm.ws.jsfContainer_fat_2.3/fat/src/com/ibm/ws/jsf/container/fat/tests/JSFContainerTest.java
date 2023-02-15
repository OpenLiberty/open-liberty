/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat.tests;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf.container.fat.FATSuite;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class JSFContainerTest extends FATServletClient {

    public static final String MOJARRA_APP = "jsfApp";
    public static final String MYFACES_APP = "jsfApp_myfaces";

    @Server("jsf.container.2.3_fat")
    public static LibertyServer server;

    private static boolean isEE10;

    @BeforeClass
    public static void setUp() throws Exception {

        isEE10 = JakartaEE10Action.isActive();

        WebArchive mojarraApp = ShrinkHelper.buildDefaultApp(MOJARRA_APP, "jsf.container.bean");
        mojarraApp = FATSuite.addMojarra(mojarraApp);
        if (!isEE10) {
            mojarraApp.addPackage("jsf.container.bean.jsf23");
        }
        mojarraApp = (WebArchive) ShrinkHelper.addDirectory(mojarraApp, "publish/files/permissions");
        ShrinkHelper.exportToServer(server, "dropins", mojarraApp);

        WebArchive myfacesApp = ShrinkHelper.buildDefaultApp(MYFACES_APP, "jsf.container.bean");
        ShrinkHelper.addDirectory(myfacesApp, "test-applications/" + MOJARRA_APP + "/resources");
        if (!isEE10) {
            myfacesApp.addPackage("jsf.container.bean.jsf23");
        }
        myfacesApp = FATSuite.addMyFaces(myfacesApp);
        myfacesApp = (WebArchive) ShrinkHelper.addDirectory(myfacesApp, "publish/files/permissions");
        ShrinkHelper.exportToServer(server, "dropins", myfacesApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testCDIBean_Mojarra() throws Exception {
        HttpUtils.findStringInReadyUrl(server, '/' + MOJARRA_APP + "/TestBean.jsf",
                                       "CDI Bean value:",
                                       ":CDIBean::PostConstructCalled::EJB-injected::Resource-injected:");
    }

    // ManagedBeans are no longer supported in Faces 4.0.
    @SkipForRepeat(SkipForRepeat.EE10_FEATURES)
    @Test
    public void testJSFBean_Mojarra() throws Exception {
        // Note that Mojarra does not support injecting @EJB into a JSF @ManagedBean
        HttpUtils.findStringInReadyUrl(server, '/' + MOJARRA_APP + "/TestBean.jsf",
                                       "JSF Bean value:",
                                       ":JSFBean::PostConstructCalled:");
    }

    @Test
    public void testCDIBean_MyFaces() throws Exception {
        HttpUtils.findStringInReadyUrl(server, '/' + MYFACES_APP + "/TestBean.jsf",
                                       "CDI Bean value:",
                                       ":CDIBean::PostConstructCalled::EJB-injected::Resource-injected:");
    }

    // ManagedBeans are no longer supported in Faces 4.0.
    @SkipForRepeat(SkipForRepeat.EE10_FEATURES)
    @Test
    public void testJSFBean_MyFaces() throws Exception {
        HttpUtils.findStringInReadyUrl(server, '/' + MYFACES_APP + "/TestBean.jsf",
                                       "JSF Bean value:",
                                       ":JSFBean::PostConstructCalled::EJB-injected:");
    }
}

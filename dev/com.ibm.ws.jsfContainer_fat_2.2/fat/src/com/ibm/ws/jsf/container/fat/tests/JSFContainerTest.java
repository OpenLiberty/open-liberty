/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class JSFContainerTest extends FATServletClient {

    public static final String MOJARRA_APP = "jsfApp";
    public static final String MYFACES_APP = "jsfApp_myfaces";

    @Server("jsf.container.2.2_fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive mojarraApp = ShrinkHelper.buildDefaultApp(MOJARRA_APP, "jsf.container.bean");
        mojarraApp = (WebArchive) ShrinkHelper.addDirectory(mojarraApp, "test-applications/" + MOJARRA_APP + "/resources");
        mojarraApp = FATSuite.addMojarra(mojarraApp);
        ShrinkHelper.exportToServer(server, "dropins", mojarraApp);

        WebArchive myfacesApp = ShrinkHelper.buildDefaultApp(MYFACES_APP, "jsf.container.bean");
        ShrinkHelper.addDirectory(myfacesApp, "test-applications/" + MOJARRA_APP + "/resources");
        myfacesApp = FATSuite.addMyFaces(myfacesApp);
        ShrinkHelper.exportToServer(server, "dropins", myfacesApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testCDIBean_Mojarra() throws Exception {
        HttpUtils.findStringInReadyUrl(server, '/' + MOJARRA_APP + "/TestBean.jsf",
                                       "CDI Bean value:",
                                       ":CDIBean::PostConstructCalled::EJB-injected::Resource-injected:");
    }

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

    @Test
    public void testJSFBean_MyFaces() throws Exception {
        HttpUtils.findStringInReadyUrl(server, '/' + MYFACES_APP + "/TestBean.jsf",
                                       "JSF Bean value:",
                                       ":JSFBean::PostConstructCalled::EJB-injected:");
    }
}

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
package com.ibm.ws.jsf.container.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

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
        WebArchive mojarraApp = ShrinkWrap.create(WebArchive.class, MOJARRA_APP + ".war")
                        .addPackages(true, "jsf.container")
                        .addAsWebResource(new File("test-applications/jsfApp/resources/TestBean.xhtml"));
        mojarraApp = FATSuite.addMojarra(mojarraApp);
        ShrinkHelper.exportToServer(server, "dropins", mojarraApp);

        WebArchive myfacesApp = ShrinkWrap.create(WebArchive.class, MYFACES_APP + ".war")
                        .addPackages(true, "jsf.container")
                        .addAsWebResource(new File("test-applications/jsfApp/resources/TestBean.xhtml"));
        mojarraApp = FATSuite.addMyFaces(myfacesApp);
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
                                       ":CDIBean::PostConstructCalled:");
    }

    @Test
    public void testJSFBean_Mojarra() throws Exception {
        HttpUtils.findStringInReadyUrl(server, '/' + MOJARRA_APP + "/TestBean.jsf",
                                       "JSF Bean value:",
                                       ":JSFBean::PostConstructCalled:");
    }

    @Test
    public void testCDIBean_MyFaces() throws Exception {
        HttpUtils.findStringInReadyUrl(server, '/' + MYFACES_APP + "/TestBean.jsf",
                                       "CDI Bean value:",
                                       ":CDIBean::PostConstructCalled:");
    }

    @Test
    public void testJSFBean_MyFaces() throws Exception {
        HttpUtils.findStringInReadyUrl(server, '/' + MYFACES_APP + "/TestBean.jsf",
                                       "JSF Bean value:",
                                       ":JSFBean::PostConstructCalled:");
    }
}

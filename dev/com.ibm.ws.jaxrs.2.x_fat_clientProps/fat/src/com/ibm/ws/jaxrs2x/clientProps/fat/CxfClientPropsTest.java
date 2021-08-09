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
package com.ibm.ws.jaxrs2x.clientProps.fat;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jaxrs2x.cxfClientProps.CxfClientPropsTestServlet;

import com.ibm.ws.jaxrs2x.clientProps.proxy.HttpProxyServer;

@RunWith(FATRunner.class)
public class CxfClientPropsTest extends FATServletClient {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES());

    static final String appName = "cxfClientPropsApp";

    @Server("jaxrs20.cxfClientProps")
    @TestServlet(servlet = CxfClientPropsTestServlet.class, contextRoot = appName)
    public static LibertyServer server;
    private final static String proxyPort = "8888";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(appName, "jaxrs2x.cxfClientProps");
        ShrinkHelper.exportDropinAppToServer(server, app);
        server.addInstalledAppForValidation(appName);
        server.startServer();
        HttpProxyServer.startHttpProxyServer(Integer.valueOf(proxyPort));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
        HttpProxyServer.stopHttpProxyServer(Integer.valueOf(proxyPort));
    }

}
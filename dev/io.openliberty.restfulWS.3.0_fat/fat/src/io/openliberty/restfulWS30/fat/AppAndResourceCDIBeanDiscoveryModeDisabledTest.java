/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package io.openliberty.restfulWS30.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.restfulWS30.fat.appandresource.AppAndResourceTestServlet;

/**
 * Tests whether a class can be both an <code>Application</code> subclass
 * <em>and<em> a resource class.
 */
@RunWith(FATRunner.class)
public class AppAndResourceCDIBeanDiscoveryModeDisabledTest extends FATServletClient {

    public static final String APP_NAME = "appandresource";
    public static final String SERVER_NAME = APP_NAME;
    private static final String BEANS_XML = "<beans xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n"
                    + "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                    + "       xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/beans_2_0.xsd\""
                    + "       bean-discovery-mode=\"none\" version=\"2.0\"></beans>";

    @Server(SERVER_NAME)
    @TestServlet(servlet = AppAndResourceTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsWebInfResource(new StringAsset(BEANS_XML), "beans.xml")
                        .addPackages(true, AppAndResourceTestServlet.class.getPackage());
        

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
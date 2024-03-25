/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
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
package com.ibm.ws.jsonp.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jsonp.app.custom.web.CustomAppJSONPServlet;
import jsonp.app.web.JSONPServlet;
import jsonp.lib.web.CustomLibJSONPServlet;

@RunWith(FATRunner.class)
public class BasicJSONPTest extends FATServletClient {

    public static final String APP_JSONP = "JSONPWAR";
    public static final String APP_CUSTOM = "customAppJSONPWAR";
    public static final String APP_CUSTOM_LIB = "customLibJSONPWAR";

    @Server("jsonp.fat.basic")
    @TestServlets({
                    @TestServlet(servlet = JSONPServlet.class, path = APP_JSONP + "/JSONPServlet"),
                    @TestServlet(servlet = CustomAppJSONPServlet.class, path = APP_CUSTOM + "/CustomAppJSONPServlet"),
                    @TestServlet(servlet = CustomLibJSONPServlet.class, path = APP_CUSTOM_LIB + "/CustomLibJSONPServlet"),
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive customAppJSONPWAR = ShrinkWrap.create(WebArchive.class, "customAppJSONPWAR.war")
                        .addAsServiceProvider(javax.json.spi.JsonProvider.class, jsonp.app.custom.provider.JsonProviderImpl.class)
                        .addPackages(true, "jsonp.app.custom");
        ShrinkHelper.exportDropinAppToServer(server, customAppJSONPWAR);

        JavaArchive customLibJSONPProvider = ShrinkWrap.create(JavaArchive.class, "customLibJSONPProvider.jar")
                        .addAsServiceProvider(javax.json.spi.JsonProvider.class, jsonp.lib.provider.JsonProviderImpl.class)
                        .addPackage("jsonp.lib.provider");
        ShrinkHelper.exportToServer(server, "JSONPProviderLib", customLibJSONPProvider);

        WebArchive customLibJSONPWAR = ShrinkWrap.create(WebArchive.class, "customLibJSONPWAR.war")
                        .addPackage("jsonp.lib.web");
        ShrinkHelper.exportAppToServer(server, customLibJSONPWAR);

        WebArchive jsonpWar = ShrinkWrap.create(WebArchive.class, APP_JSONP + ".war")
                        .addAsWebInfResource(new File("test-applications/JSONPWAR.war/resources/WEB-INF/json_read_test_data.js"))
                        .addPackage("jsonp.app.web");
        ShrinkHelper.exportDropinAppToServer(server, jsonpWar);

        server.startServer();
    }
}

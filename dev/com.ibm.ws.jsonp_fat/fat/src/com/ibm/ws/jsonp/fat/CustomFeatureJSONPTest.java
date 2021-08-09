/*******************************************************************************
 * Copyright (c) 2014,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsonp.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jsonp.app.feature.web.CustomFeatureJSONPServlet;

@RunWith(FATRunner.class)
public class CustomFeatureJSONPTest extends FATServletClient {

    private static final String APP_NAME = "customFeatureJSONPWAR";
    private static final String FEATURE_NAME = "customJsonpProvider-1.0.mf";
    private static final String BUNDLE_NAME = "com.ibm.ws.jsonp.feature.provider.1.0.jar";

    @Server("jsonp.fat.customFeature")
    @TestServlet(servlet = CustomFeatureJSONPServlet.class, path = APP_NAME + "/CustomFeatureJSONPServlet")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/" + FEATURE_NAME);
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/" + BUNDLE_NAME);

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "jsonp.app.feature.web");
        ShrinkHelper.exportToServer(server, "dropins", app);
        server.addInstalledAppForValidation(APP_NAME);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
        server.uninstallUserBundle(BUNDLE_NAME);
        server.uninstallUserFeature(FEATURE_NAME);
    }
}

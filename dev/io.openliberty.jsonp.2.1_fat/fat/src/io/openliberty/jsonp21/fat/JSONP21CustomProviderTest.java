/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.jsonp21.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.jsonp21.fat.web.BundledProviderServlet;
import io.openliberty.jsonp21.fat.web.FeatureProviderServlet;

@RunWith(FATRunner.class)
public class JSONP21CustomProviderTest extends FATServletClient {

    @Server("jsonp2.1.customProvider.fat")
    @TestServlets({
                    @TestServlet(servlet = FeatureProviderServlet.class, contextRoot = "ProviderFeatureApp"),
                    @TestServlet(servlet = BundledProviderServlet.class, contextRoot = "ProviderAppBundledApp"),
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive providerFeatureApp = ShrinkWrap.create(WebArchive.class, "ProviderFeatureApp.war")
                        .addClass(FeatureProviderServlet.class);
        ShrinkHelper.exportAppToServer(server, providerFeatureApp);
        server.addInstalledAppForValidation("ProviderFeatureApp");

        String johnzonJarPath = server.getServerSharedPath() + "resources/johnzon/2.1.0/johnzon-core.jar";
        WebArchive providerAppBundledApp = ShrinkWrap.create(WebArchive.class, "ProviderAppBundledApp.war")
                        .addClass(BundledProviderServlet.class)
                        .addAsLibrary(new File(johnzonJarPath));
        ShrinkHelper.exportAppToServer(server, providerAppBundledApp);
        server.addInstalledAppForValidation("ProviderAppBundledApp");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}

/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;
import com.ibm.ws.cdi.extension.apps.observer.ObserverExtension;
import com.ibm.ws.cdi.extension.apps.observer.ObserverTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class ObserverTest extends FATServletClient {

    public static final String APP_NAME = "observeProcessProducerMethod";
    public static final String SERVER_NAME = "observerServer";

    @Server("observerServer")
    @TestServlet(servlet = ObserverTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        WebArchive observeProcessProducerMethod = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        observeProcessProducerMethod.addPackage(ObserverTestServlet.class.getPackage());
        CDIArchiveHelper.addCDIExtensionService(observeProcessProducerMethod, ObserverExtension.class);
        CDIArchiveHelper.addBeansXML(observeProcessProducerMethod, DiscoveryMode.ANNOTATED);

        ShrinkHelper.exportDropinAppToServer(server, observeProcessProducerMethod, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}

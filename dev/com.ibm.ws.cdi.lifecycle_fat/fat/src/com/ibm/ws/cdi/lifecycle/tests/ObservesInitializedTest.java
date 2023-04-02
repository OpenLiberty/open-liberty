/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.lifecycle.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.lifecycle.apps.observesInitializedManifestJar.ManifestAutostartObserver;
import com.ibm.ws.cdi.lifecycle.apps.observesInitializedWar.ObservesInitializedTestServlet;
import com.ibm.ws.cdi.lifecycle.apps.observesInitializedWar.WarAutostartObserver;
import com.ibm.ws.cdi.lifecycle.apps.observesInitializedWebInfJar.WebInfAutostartObserver;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class ObservesInitializedTest extends FATServletClient {

    @TestServlet(contextRoot = "ObservesInitialized", servlet = ObservesInitializedTestServlet.class)
    @Server("cdi12BasicServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        JavaArchive observesInitializedWebInfJar = ShrinkWrap.create(JavaArchive.class, "ObservesInitializedWebInf.jar")
                                                             .addPackages(true, WebInfAutostartObserver.class.getPackage())
                                                             .addAsManifestResource(WebInfAutostartObserver.class.getResource("beans.xml"), "beans.xml");

        JavaArchive observesInitializedManifestJar = ShrinkWrap.create(JavaArchive.class, "ObservesInitializedManifest.jar")
                                                               .addPackages(true, ManifestAutostartObserver.class.getPackage())
                                                               .addAsManifestResource(ManifestAutostartObserver.class.getResource("beans.xml"), "beans.xml");

        WebArchive observesInitializedWar = ShrinkWrap.create(WebArchive.class, "ObservesInitialized.war")
                                                      .addPackages(true, WarAutostartObserver.class.getPackage())
                                                      .addAsManifestResource(WarAutostartObserver.class.getPackage(), "MANIFEST.MF", "MANIFEST.MF")
                                                      .addAsWebInfResource(WarAutostartObserver.class.getResource("beans.xml"), "beans.xml")
                                                      .addAsLibrary(observesInitializedWebInfJar);

        EnterpriseArchive observesInitializedEar = ShrinkWrap.create(EnterpriseArchive.class, "ObservesInitialized.ear")
                                                             .addAsModule(observesInitializedWar)
                                                             .addAsModule(observesInitializedManifestJar);

        ShrinkHelper.exportDropinAppToServer(server, observesInitializedEar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

}

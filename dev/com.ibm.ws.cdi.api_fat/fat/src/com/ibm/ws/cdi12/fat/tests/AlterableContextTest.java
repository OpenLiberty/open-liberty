/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.cdi12.fat.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi12.alterablecontext.test.AlterableContextTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * These tests use a runtime feature to destroy a contextual object.
 * The test passes if that object exists before destroy is called
 * and is null afterwards
 */

@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
@RunWith(FATRunner.class)
public class AlterableContextTest extends FATServletClient {

    public static final String APP_NAME = "alterableContextApp";

    @Server("cdi12AlterableContextServer")
    @TestServlet(servlet = AlterableContextTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive alterableContextExtension = ShrinkWrap.create(JavaArchive.class, "alterableContextExtension.jar");
        alterableContextExtension.addClass("com.ibm.ws.cdi12.alterablecontext.test.extension.DirtySingleton");
        alterableContextExtension.addClass("com.ibm.ws.cdi12.alterablecontext.test.extension.AlterableContextBean");
        alterableContextExtension.addClass("com.ibm.ws.cdi12.alterablecontext.test.extension.AlterableContextExtension");
        alterableContextExtension.add(new FileAsset(new File("test-applications/alterableContextExtension.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")),
                                      "/META-INF/services/javax.enterprise.inject.spi.Extension");
        alterableContextExtension.add(new FileAsset(new File("test-applications/alterableContextExtension.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        WebArchive alterableContextApp = ShrinkWrap.create(WebArchive.class, "alterableContextApp.war");
        alterableContextApp.addClass("com.ibm.ws.cdi12.alterablecontext.test.AlterableContextTestServlet");
        alterableContextApp.add(new FileAsset(new File("test-applications/alterableContextApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        alterableContextApp.addAsLibrary(alterableContextExtension);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "alterableContextsApp.ear");
        ear.add(new FileAsset(new File("test-applications/alterableContextsApp.ear/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml");
        ear.add(new FileAsset(new File("test-applications/alterableContextsApp.ear/resources/META-INF/application.xml")), "/META-INF/application.xml");
        ear.addAsModule(alterableContextApp);

        ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}

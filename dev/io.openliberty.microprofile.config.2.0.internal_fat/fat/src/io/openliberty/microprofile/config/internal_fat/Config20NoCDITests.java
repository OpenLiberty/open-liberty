/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.config.internal_fat.apps.noCDI.NoCDITestServlet;

@RunWith(FATRunner.class)
public class Config20NoCDITests extends FATServletClient {

    public static final String NO_CDI_APP_NAME = "noCDIApp";

    public static final String SERVER_NAME = "Config20NoCDIServer";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.LATEST);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = NoCDITestServlet.class, contextRoot = NO_CDI_APP_NAME)

    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        PropertiesAsset noCDIConfigSource = new PropertiesAsset()
                        .addProperty(NoCDITestServlet.NO_CDI_TEST_KEY, NoCDITestServlet.NO_CDI_TEST_VALUE);
        WebArchive noCDIWar = ShrinkWrap.create(WebArchive.class, NO_CDI_APP_NAME + ".war")
                        .addPackages(true, NoCDITestServlet.class.getPackage())
                        .addAsResource(noCDIConfigSource, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, noCDIWar, DeployOptions.SERVER_ONLY);

        server.startServer();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}

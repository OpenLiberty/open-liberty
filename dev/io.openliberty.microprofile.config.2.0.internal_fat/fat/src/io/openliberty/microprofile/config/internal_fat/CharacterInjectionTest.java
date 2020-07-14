/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfigActions;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfigActions.Version;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.config.internal_fat.apps.characterInjection.CharacterInjectionServlet;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class CharacterInjectionTest extends FATServletClient {

    public static final String APP_NAME = "characterInjectionApp";
    public static final String SERVER_NAME = "Config20Server";

    @ClassRule
    public static RepeatTests r = RepeatConfigActions.repeat(SERVER_NAME, Version.CONFIG20_EE8);

    @Server(SERVER_NAME)
    @TestServlet(servlet = CharacterInjectionServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        PropertiesAsset config = new PropertiesAsset().addProperty("char1", "a");

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, CharacterInjectionServlet.class.getPackage())
                        .addAsResource(config, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}

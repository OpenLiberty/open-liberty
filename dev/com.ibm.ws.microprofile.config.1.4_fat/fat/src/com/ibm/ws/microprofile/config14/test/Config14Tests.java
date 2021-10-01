/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.config14.test.apps.characterInjection.CharacterInjectionServlet;
import com.ibm.ws.microprofile.config14.test.apps.optional_observer.OptionalObserverServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class Config14Tests extends FATServletClient {

    public static final String CHAR_INJECTION_APP_NAME = "characterInjectionApp";
    public static final String OPTIONAL_OBSERVER_APP_NAME = "optionalObserverApp";
    public static final String SERVER_NAME = "Config14Server";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.MP50, MicroProfileActions.MP33, MicroProfileActions.LATEST);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = CharacterInjectionServlet.class, contextRoot = CHAR_INJECTION_APP_NAME),
                    @TestServlet(servlet = OptionalObserverServlet.class, contextRoot = OPTIONAL_OBSERVER_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        PropertiesAsset config = new PropertiesAsset().addProperty("char1", "a");

        WebArchive charInjectionWar = ShrinkWrap.create(WebArchive.class, CHAR_INJECTION_APP_NAME + ".war")
                        .addPackages(true, CharacterInjectionServlet.class.getPackage())
                        .addAsResource(config, "META-INF/microprofile-config.properties");

        WebArchive optionalObserverWar = ShrinkWrap.create(WebArchive.class, OPTIONAL_OBSERVER_APP_NAME + ".war")
                        .addPackages(true, OptionalObserverServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, charInjectionWar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, optionalObserverWar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}

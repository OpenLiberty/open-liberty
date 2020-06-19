/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.test;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfigActions;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfigActions.Version;
import com.ibm.ws.microprofile.config14.test.apps.badobserver.BadObserverServlet;
import com.ibm.ws.microprofile.config14.test.apps.characterInjection.CharacterInjectionServlet;
import com.ibm.ws.microprofile.config14.test.apps.optional_observer.OptionalObserverServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class Config14Tests extends FATServletClient {

    public static final String BAD_OBSERVER_APP_NAME = "badObserverApp";
    public static final String CHAR_INJECTION_APP_NAME = "characterInjectionApp";
    public static final String OPTIONAL_OBSERVER_APP_NAME = "optionalObserverApp";
    public static final String SERVER_NAME = "Config14Server";

    @ClassRule
    public static RepeatTests r = RepeatConfigActions.repeat(SERVER_NAME, Version.LATEST);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = BadObserverServlet.class, contextRoot = BAD_OBSERVER_APP_NAME),
                    @TestServlet(servlet = CharacterInjectionServlet.class, contextRoot = CHAR_INJECTION_APP_NAME),
                    @TestServlet(servlet = OptionalObserverServlet.class, contextRoot = OPTIONAL_OBSERVER_APP_NAME) })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, BAD_OBSERVER_APP_NAME + ".war")
                        .addPackages(true, BadObserverServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war1, DeployOptions.SERVER_ONLY);

        PropertiesAsset config = new PropertiesAsset().addProperty("char1", "a");

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, CHAR_INJECTION_APP_NAME + ".war")
                        .addPackages(true, CharacterInjectionServlet.class.getPackage())
                        .addAsResource(config, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war2, DeployOptions.SERVER_ONLY);

        WebArchive war3 = ShrinkWrap.create(WebArchive.class, OPTIONAL_OBSERVER_APP_NAME + ".war")
                        .addPackages(true, OptionalObserverServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war3, DeployOptions.SERVER_ONLY);

        server.startServer(true, false);//Don't validate, the app is going to throw a DeploymentException
    }

    @Test
    public void testBadObserver() throws Exception {
        List<String> msgs = server.findStringsInLogs("java.util.NoSuchElementException: CWMCG0015E: The property DOESNOTEXIST was not found in the configuration.");
        assertTrue("NoSuchElementException message not found", msgs.size() > 0);
        msgs = server.findStringsInLogs("org.jboss.weld.exceptions.DeploymentException: WELD-001408: Unsatisfied dependencies for type String with qualifiers @ConfigProperty");
        assertTrue("DeploymentException message not found", msgs.size() > 0);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKZ0002E", "CWMCG5005E");
        //CWWKZ0002E: An exception occurred while starting the application badObserverApp
        //CWMCG5005E: The InjectionPoint dependency was not resolved for the Observer method: private static final void com.ibm.ws.microprofile.config14.test.apps.badobserver.TestObserver.observerMethod(java.lang.Object,java.lang.String).
    }

}

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
package com.ibm.ws.microprofile.config20.test;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfigActions;
import com.ibm.ws.microprofile.config20.test.apps.badobserver.BadObserverServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE) //Temp change for testing
public class BadObserverTest extends FATServletClient {

    public static final String APP_NAME = "badObserverApp";
    public static final String SERVER_NAME = "Config20Server";

    @ClassRule
    public static RepeatTests r = RepeatConfigActions.repeatConfig20(SERVER_NAME);

    @Server(SERVER_NAME)
    @TestServlet(servlet = BadObserverServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, BadObserverServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        server.startServer(true, false);//Don't validate, the app is going to throw a DeploymentException
        System.out.println("here");

    }

    @Test
    public void testBadObserver() throws Exception {
        List<String> msgs = server.findStringsInLogs("No Config Value exists for required property DOESNOTEXIST");
        assertTrue("ConfigException message not found", msgs.size() > 0);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKZ0002E", "CWMCG5005E");
        //CWWKZ0002E: An exception occurred while starting the application badObserverApp
        //CWMCG5005E: The InjectionPoint dependency was not resolved for the Observer method: private static final void com.ibm.ws.microprofile.config20.test.apps.badobserver.TestObserver.observerMethod(java.lang.Object,java.lang.String).
    }

}

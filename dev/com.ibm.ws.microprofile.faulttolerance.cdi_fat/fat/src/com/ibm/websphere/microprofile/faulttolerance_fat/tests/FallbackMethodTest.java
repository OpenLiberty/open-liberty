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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.SimpleSearch;
import com.ibm.websphere.microprofile.faulttolerance_fat.validation.ValidationTest;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.faulttolerance.fat.repeat.RepeatFaultTolerance;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test to check we can actually call the fallback methods we find
 * <p>
 * If this test fails to deploy, check the results of {@link ValidationTest} to see if a particular combination is rejected as invalid
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class FallbackMethodTest extends FATServletClient {

    @ClassRule
    public static RepeatTests r = RepeatFaultTolerance.repeatDefault("FaultToleranceMultiModule");

    @Server(value = "FaultToleranceMultiModule")
    @TestServlet(servlet = FallbackMethodServlet.class, contextRoot = "ftFallbackMethod")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftFallbackMethod.war")
                        .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                        .addClass(FallbackMethodServlet.class)
                        .addPackage(SimpleSearch.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

}

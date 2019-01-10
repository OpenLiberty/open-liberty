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
package com.ibm.ws.jaxb.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jaxb.thirdparty.web.ThirdPartyJAXBTestServlet;

@RunWith(FATRunner.class)
@SkipForRepeat("JAXB-2.3")
@MaximumJavaLevel(javaLevel = 8) // Do not run these tests on JDK 9+ where there is no JAX-B api/impl in the JDK
public class ThirdPartyJAXBTest extends FATServletClient {

    private static final String SERVER = "jaxb_fat.no-jaxb-feature";
    private static final String APP_NAME = "thirdPartyJaxbApp";

    // Iterate over some features that tend to use JAX-B internally to ensure they don't leak into the app classloader space
    @ClassRule
    public static RepeatTests r = RepeatTests.with(new FeatureReplacementAction("ldapRegistry-3.0").withID("LDAP").forServers(SERVER))
                    .andWith(new FeatureReplacementAction("ldapRegistry-3.0", "jpa-2.1").withID("JPA").forServers(SERVER))
                    .andWith(new FeatureReplacementAction("jpa-2.1", "jaxrs-2.0").withID("JAXRS").forServers(SERVER));

    @Server(SERVER)
    @TestServlet(servlet = ThirdPartyJAXBTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "jaxb.thirdparty.web");
        server.startServer(RepeatTestFilter.CURRENT_REPEAT_ACTION + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE9967W");
    }
}

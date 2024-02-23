/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.visibility.tests.validatorInJar;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.visibility.tests.validatorInJar.jar.AppScopedBean;
import com.ibm.ws.cdi.visibility.tests.validatorInJar.war.ValidatorInJarTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ValidatorInJarTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12ValidatorInJarServer";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE11, EERepeatActions.EE7);

    public static final String TEST_VALIDATOR_APP_NAME = "TestValidatorInJar";

    @Server(SERVER_NAME)
    @TestServlet(servlet = ValidatorInJarTestServlet.class, contextRoot = TEST_VALIDATOR_APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        JavaArchive TestValidatorInJar = ShrinkWrap.create(JavaArchive.class, "TestValidatorInJar.jar")
                                                   .addPackage(AppScopedBean.class.getPackage())
                                                   .addAsManifestResource(AppScopedBean.class.getResource("beans.xml"), "beans.xml");

        WebArchive TestValidatorInJarWar = ShrinkWrap.create(WebArchive.class, "TestValidatorInJar.war")
                                                     .addPackage(ValidatorInJarTestServlet.class.getPackage())
                                                     .setManifest(ValidatorInJarTestServlet.class.getResource("MANIFEST.MF"))
                                                     .addAsWebInfResource(ValidatorInJarTestServlet.class.getResource("beans.xml"), "beans.xml")
                                                     .addAsLibraries(new File("lib/jaxrsvalidator").listFiles());

        EnterpriseArchive testValidatorInJarEAR = ShrinkWrap.create(EnterpriseArchive.class, "TestValidatorInJar.ear")
                                                            .addAsModule(TestValidatorInJarWar)
                                                            .addAsModule(TestValidatorInJar);

        ShrinkHelper.exportDropinAppToServer(server, testValidatorInJarEAR, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}

/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.cdi.test.basic.injection.WebServ;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ValidatorInJarTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12ValidatorInJarServer";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE7);

    public static final String TEST_VALIDATOR_APP_NAME = "TestValidatorInJar";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = WebServ.class, contextRoot = TEST_VALIDATOR_APP_NAME) }) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            JavaArchive TestValidatorInJar = ShrinkWrap.create(JavaArchive.class, "TestValidatorInJar.jar")
                                                       .addClass(com.ibm.cdi.test.basic.injection.jar.AppScopedBean.class);

            WebArchive TestValidatorInJarWar = ShrinkWrap.create(WebArchive.class, "TestValidatorInJar.war")
                                                         .addClass(com.ibm.cdi.test.basic.injection.WebServ.class)
                                                         .addAsManifestResource(new File("test-applications/TestValidatorInJar.war/resources/META-INF/MANIFEST.MF"))
                                                         .add(new FileAsset(new File("test-applications/TestValidatorInJar.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                                                         .add(new FileAsset(new File("test-applications/TestValidatorInJar.war/resources/WEB-INF/lib/jaxrs-analyzer-0.9.jar")),
                                                              "/WEB-INF/lib/jaxrs-analyzer-0.9.jar");

            EnterpriseArchive testValidatorInJarEAR = ShrinkWrap.create(EnterpriseArchive.class, "TestValidatorInJar.ear")
                                                                .add(new FileAsset(new File("test-applications/TestValidatorInJar.ear/resources/META-INF/application.xml")),
                                                                     "/META-INF/application.xml")
                                                                .addAsModule(TestValidatorInJarWar)
                                                                .addAsModule(TestValidatorInJar);

            ShrinkHelper.exportDropinAppToServer(server, testValidatorInJarEAR, DeployOptions.SERVER_ONLY);
        }
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}

/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi20.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE8;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;  
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import builtinAnnoApp.web.BuiltinAnnoServlet;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests; 
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * These tests verify the use of Built-in annotation literals in CDI2.0 as per http://docs.jboss.org/cdi/spec/2.0/cdi-spec-with-assertions.html#built_in_annotation_literals
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class BuiltinAnnoLiteralsTest extends FATServletClient {
    
    public static final String SERVER_NAME = "cdi20BuiltinAnnoServer";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE8);

    public static final String APP_NAME = "builtinAnnoLiteralsApp";

    @Server(SERVER_NAME)
    @TestServlets({ @TestServlet(servlet = BuiltinAnnoServlet.class, contextRoot = APP_NAME) }) //LITE
    
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
            
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                    .addPackages(true, "builtinAnnoApp.web")
                                    .add(new FileAsset(new File("test-applications/" + APP_NAME + "/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                                    .addAsManifestResource(new File("test-applications/" + APP_NAME + "/resources/META-INF/services/javax.enterprise.inject.spi.Extension"),
                                                           "services/javax.enterprise.inject.spi.Extension")
                                    .addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/META-INF/beans.xml"),"beans.xml") // NEEDS TO GO IN WEB-INF in a war
                                    .addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/index.jsp"));
        
        ShrinkHelper.exportAppToServer(server, app, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}

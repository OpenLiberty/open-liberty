/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.implicit.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7_FULL;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test that wars can be implicit bean archives.
 */
@RunWith(FATRunner.class)
public class ImplicitWarTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12ImplicitServer";

    private static final String IMPLICIT_WAR_APP_NAME = "implicitWarApp";

    @Server("cdi12ImplicitServer")
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi.beansxml.implicit.apps.servlets.ImplicitWarServlet.class, contextRoot = IMPLICIT_WAR_APP_NAME)//LITE
    })
    public static LibertyServer server;

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE7_FULL);

    @BeforeClass
    public static void buildShrinkWrap() throws Exception {

        JavaArchive utilLib = ShrinkWrap.create(JavaArchive.class, "utilLib.jar")
                                        .addClass(com.ibm.ws.cdi.beansxml.implicit.utils.SimpleAbstract.class)
                                        .add(new FileAsset(new File("test-applications/utilLib.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        WebArchive implicitWarApp = ShrinkWrap.create(WebArchive.class, IMPLICIT_WAR_APP_NAME + ".war")
                                              .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.servlets.ImplicitWarServlet.class)
                                              .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.beans.AnnotatedBean.class)
                                              .addAsLibrary(utilLib);

        ShrinkHelper.exportDropinAppToServer(server, implicitWarApp, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }
}

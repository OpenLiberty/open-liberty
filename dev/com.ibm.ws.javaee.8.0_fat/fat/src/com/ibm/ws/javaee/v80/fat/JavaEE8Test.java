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
package com.ibm.ws.javaee.v80.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.PrivHelper;
import javaee8.web.WebProfile8TestServlet;

@RunWith(FATRunner.class)
public class JavaEE8Test extends FATServletClient {

    @ClassRule
    public static RepeatTests repeat = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction()
                                    .removeFeature("javaee-8.0")
                                    .addFeature("webProfile-8.0")
                                    .withID("webProfile8"));

    public static final String APP_NAME = "javaee8App";

    @Server("javaee8.fat")
    @TestServlet(servlet = WebProfile8TestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, PrivHelper.JAXB_PERMISSION);
        EnterpriseArchive earApp = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                        .addAsModule(ShrinkHelper.buildDefaultApp(APP_NAME, "javaee8.web.*"));
        earApp = (EnterpriseArchive) ShrinkHelper.addDirectory(earApp, "test-applications/javaee8Ear/resources/");
        ShrinkHelper.exportDropinAppToServer(server, earApp);

        String consoleName = JavaEE8Test.class.getSimpleName();
        if (RepeatTestFilter.CURRENT_REPEAT_ACTION != null)
            consoleName += '_' + RepeatTestFilter.CURRENT_REPEAT_ACTION;
        server.startServer(consoleName + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE0280E"); // TODO: tracked by OpenLiberty issue #4857
    }
}
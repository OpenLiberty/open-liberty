/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jakartaee10.internal.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.jakartaee10.internal.apps.jakartaee10.web.WebProfile10TestServlet;

@RunWith(FATRunner.class)
public class JakartaEE10Test extends FATServletClient {

    @ClassRule
    public static RepeatTests repeat = RepeatTests
                    .with(new FeatureReplacementAction()
                                    .removeFeature("webProfile-10.0")
                                    .addFeature("jakartaee-10.0")
                                    .withID("jakartaee10")) //LITE
                    .andWith(new FeatureReplacementAction()
                                    .removeFeature("jakartaee-10.0")
                                    .addFeature("webProfile-10.0")
                                    .withID("webProfile10")
                                    .fullFATOnly());

    public static final String APP_NAME = "webProfile10App";

    @Server("jakartaee10.fat")
    @TestServlet(servlet = WebProfile10TestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        war.addPackages(true, WebProfile10TestServlet.class.getPackage());
        war.addAsWebInfResource(WebProfile10TestServlet.class.getPackage(), "persistence.xml", "classes/META-INF/persistence.xml");

        EnterpriseArchive earApp = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        earApp.setApplicationXML(WebProfile10TestServlet.class.getPackage(), "application.xml");
        earApp.addAsModule(war);
        ShrinkHelper.exportDropinAppToServer(server, earApp, DeployOptions.SERVER_ONLY);

        String consoleName = JakartaEE10Test.class.getSimpleName() + RepeatTestFilter.getRepeatActionsAsString();
        server.startServer(consoleName + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE0280E"); // TODO: tracked by OpenLiberty issue #4857
    }
}
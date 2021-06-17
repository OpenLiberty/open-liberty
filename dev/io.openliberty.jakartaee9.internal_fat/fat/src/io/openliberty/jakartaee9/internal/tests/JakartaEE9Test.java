/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jakartaee9.internal.tests;

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
import io.openliberty.jakartaee9.internal.apps.jakartaee9.web.WebProfile9TestServlet;

@RunWith(FATRunner.class)
public class JakartaEE9Test extends FATServletClient {

    @ClassRule
    public static RepeatTests repeat = RepeatTests
                    .with(new FeatureReplacementAction()
                                    .removeFeature("webProfile-9.1")
                                    .addFeature("jakartaee-9.1")
                                    .withID("jakartaee91")) //LITE
                    .andWith(new FeatureReplacementAction()
                                    .removeFeature("jakartaee-9.1")
                                    .addFeature("webProfile-9.1")
                                    .withID("webProfile91")
                                    .fullFATOnly());

    public static final String APP_NAME = "webProfile9App";

    @Server("jakartaee9.fat")
    @TestServlet(servlet = WebProfile9TestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        war.addPackages(true, WebProfile9TestServlet.class.getPackage());
        war.addAsWebInfResource(WebProfile9TestServlet.class.getPackage(), "persistence.xml", "classes/META-INF/persistence.xml");

        EnterpriseArchive earApp = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        earApp.setApplicationXML(WebProfile9TestServlet.class.getPackage(), "application.xml");
        earApp.addAsModule(war);
        ShrinkHelper.exportDropinAppToServer(server, earApp, DeployOptions.SERVER_ONLY);

        String consoleName = JakartaEE9Test.class.getSimpleName() + RepeatTestFilter.getRepeatActionsAsString();
        server.startServer(consoleName + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE0280E"); // TODO: tracked by OpenLiberty issue #4857
    }
}
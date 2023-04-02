/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jakartaee10.internal.tests;

import java.util.Set;

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

    private static Set<String> getCompatFeatures() {
        Set<String> compatFeatures = EE10FeatureCompatibilityTest.getAllCompatibleFeatures();
        // remove features so that they don't cause feature conflicts.
        compatFeatures.remove("jdbc-4.0");
        compatFeatures.remove("jdbc-4.1");
        compatFeatures.remove("jdbc-4.2");
        compatFeatures.remove("sessionCache-1.0");
        compatFeatures.remove("facesContainer-4.0");
        compatFeatures.remove("jsonbContainer-3.0");
        compatFeatures.remove("jsonpContainer-2.1");
        compatFeatures.remove("passwordUtilities-1.1");
        compatFeatures.remove("persistenceContainer-3.1");

        // remove client features
        compatFeatures.remove("jakartaeeClient-10.0");
        compatFeatures.remove("appSecurityClient-1.0");

        // remove acmeCA-2.0 since it requires additional resources and configuration
        compatFeatures.remove("acmeCA-2.0");

        // remove noship features
        compatFeatures.remove("checkpoint-1.0");
        compatFeatures.remove("jcacheContainer-1.1");
        compatFeatures.remove("netty-1.0");
        compatFeatures.remove("noShip-1.0");
        compatFeatures.remove("scim-2.0");
        return compatFeatures;
    }

    private static final String ALL_COMPAT_FEATURES = "AllEE10CompatFeatures";
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
                                    .fullFATOnly())
                    .andWith(new FeatureReplacementAction()
                                    .removeFeature("webProfile-10.0")
                                    .removeFeature("jakartaee-10.0")
                                    .addFeatures(getCompatFeatures())
                                    .withID(ALL_COMPAT_FEATURES)
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
        if (RepeatTestFilter.isRepeatActionActive(ALL_COMPAT_FEATURES)) {
            // set it to 15 minutes.
            server.setServerStartTimeout(15 * 60 * 1000L);
        }
        server.startServer(consoleName + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        String[] toleratedWarnErrors;
        if (!RepeatTestFilter.isRepeatActionActive(ALL_COMPAT_FEATURES)) {
            toleratedWarnErrors = new String[] { "SRVE0280E" };// TODO: SRVE0280E tracked by OpenLiberty issue #4857
        } else {
            toleratedWarnErrors = new String[] { "SRVE0280E", // TODO: SRVE0280E tracked by OpenLiberty issue #4857
                                                 "CWWKS5207W", // The remaining ones relate to config not done for the server / app
                                                 "CWWWC0002W",
                                                 "CWMOT0010W",
                                                 "TRAS4352W" // Only happens when running with WebSphere Liberty image due to an auto feature
            };
        }
        server.stopServer(toleratedWarnErrors);
    }
}
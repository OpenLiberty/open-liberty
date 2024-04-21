/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jakartaee11.internal.tests;

import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
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
import io.openliberty.jakartaee11.internal.apps.jakartaee11.web.WebProfile11TestServlet;
import io.openliberty.jakartaee11.internal.tests.util.FATFeatureResolver;

@RunWith(FATRunner.class)
public class JakartaEE11Test extends FATServletClient {

    private static Set<String> getCompatFeatures(boolean openLibertyOnly) {
        Set<String> compatFeatures = EE11FeatureCompatibilityTest.getAllCompatibleFeatures(openLibertyOnly);
        // remove features so that they don't cause feature conflicts.
        compatFeatures.remove("jdbc-4.0");
        compatFeatures.remove("jdbc-4.1");
        compatFeatures.remove("jdbc-4.2");
        compatFeatures.remove("sessionCache-1.0");
        compatFeatures.remove("facesContainer-4.1");
        compatFeatures.remove("jsonbContainer-3.0");
        compatFeatures.remove("jsonpContainer-2.1");
        compatFeatures.remove("passwordUtilities-1.1");
        compatFeatures.remove("persistenceContainer-3.2");

        // remove client features
        compatFeatures.remove("jakartaeeClient-11.0");
        compatFeatures.remove("appSecurityClient-1.0");

        // remove acmeCA-2.0 since it requires additional resources and configuration
        compatFeatures.remove("acmeCA-2.0");

        // remove noship features
        compatFeatures.remove("data-1.1");
        compatFeatures.remove("jcacheContainer-1.1");
        compatFeatures.remove("netty-1.0");
        compatFeatures.remove("noShip-1.0");
        compatFeatures.remove("scim-2.0");

        // noship, still in development
        compatFeatures.remove("mpReactiveStreams-3.0");
        compatFeatures.remove("mpReactiveMessaging-3.0");

        // remove logAnalysis-1.0.  It depends on hpel being configured
        compatFeatures.remove("logAnalysis-1.0");

        return compatFeatures;
    }

    public static EE11Features getFeatures() {
        return ee11Features;
    }

    //

    private static final String COMPAT_OL_FEATURES = "EE11CompatFeatures_OL";
    private static final String COMPAT_WL_FEATURES = "EE11CompatFeatures_WL";

    @ClassRule
    public static RepeatTests repeat;

    static {
        Set<String> olCompatFeatures = getFeatures().getExtendedCompatibleFeatures(EE11Features.OPEN_LIBERTY_ONLY);
        Set<String> wlCompatFeatures = getFeatures().getExtendedCompatibleFeatures(!EE11Features.OPEN_LIBERTY_ONLY);

        RepeatTests useRepeat = RepeatTests
                        .with(new FeatureReplacementAction()
                                        .removeFeature("webProfile-11.0")
                                        .addFeature("jakartaee-11.0")
                                        .withID("jakartaee11")
                                        .fullFATOnly())
                        .andWith(new FeatureReplacementAction()
                                        .removeFeature("jakartaee-11.0")
                                        .addFeature("webProfile-11.0")
                                        .withID("webProfile11")
                                        .fullFATOnly())
                        .andWith(new FeatureReplacementAction()
                                        .removeFeature("webProfile-11.0")
                                        .removeFeature("jakartaee-11.0")
                                        .addFeatures(olCompatFeatures)
                                        .withID(COMPAT_OL_FEATURES)); //LITE

        if (!olCompatFeatures.equals(wlCompatFeatures)) {
            Set<String> featuresToAdd = new HashSet<>();
            for (String feature : wlCompatFeatures) {
                if (!olCompatFeatures.contains(feature)) {
                    featuresToAdd.add(feature);
                }
            }
            useRepeat = useRepeat.andWith(new FeatureReplacementAction()
                            .addFeatures(featuresToAdd)
                            .withID(COMPAT_WL_FEATURES)
                            .fullFATOnly());
        }

        repeat = useRepeat;
    }

    public static final String APP_NAME = "webProfile11App";

    @Server("jakartaee11.fat")
    @TestServlet(servlet = WebProfile11TestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        war.addPackages(true, WebProfile11TestServlet.class.getPackage());
        war.addAsWebInfResource(WebProfile11TestServlet.class.getPackage(), "persistence.xml", "classes/META-INF/persistence.xml");

        EnterpriseArchive earApp = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        earApp.setApplicationXML(WebProfile11TestServlet.class.getPackage(), "application.xml");
        earApp.addAsModule(war);
        ShrinkHelper.exportDropinAppToServer(server, earApp, DeployOptions.SERVER_ONLY);

        String consoleName = JakartaEE11Test.class.getSimpleName() + RepeatTestFilter.getRepeatActionsAsString();
        if (RepeatTestFilter.isRepeatActionActive(COMPAT_WL_FEATURES)) {
            server.setServerStartTimeout(15 * 60 * 1000L); // 15 min
        }
        server.startServer(consoleName + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        String[] toleratedWarnErrors;
        if (RepeatTestFilter.isRepeatActionActive(COMPAT_OL_FEATURES)) {
            toleratedWarnErrors = new String[] { "SRVE0280E", // TODO: SRVE0280E tracked by OpenLiberty issue #4857
                                                 "CWWKS5207W", // The remaining ones relate to config not done for the server / app
                                                 "CWWWC0002W",
                                                 "CWMOT0010W",
                                                 "CWWKE0701E", // TODO: Issue 28226: Fix this or verify that it is expected
                                                 "TRAS4352W" // Only happens when running with WebSphere Liberty image due to an auto feature
            };

        } else if (RepeatTestFilter.isRepeatActionActive(COMPAT_WL_FEATURES)) {
            toleratedWarnErrors = new String[] { "SRVE0280E", // TODO: SRVE0280E tracked by OpenLiberty issue #4857
                                                 "CWWKS5207W", // The remaining ones relate to config not done for the server / app
                                                 "CWWWC0002W",
                                                 "CWMOT0010W",
                                                 "CWWKE0701E", // TODO: Issue 28226: Fix this or verify that it is expected
                                                 "CWWKG0033W", // related to missing config for collectives
                                                 "CWSJY0035E", // wmqJmsClient.rar.location variable not in the server.xml
                                                 "CWWKE0701E", // wmqJmsClient.rar.location variable not in the server.xml
                                                 "TRAS4352W", // Only happens when running with WebSphere Liberty image due to an auto feature
                                                 "CWWKB0758E" // zosAutomaticRestartManager-1.0 error due to missing SAF configuration
            };

        } else {
            toleratedWarnErrors = new String[] { "SRVE0280E" };// TODO: SRVE0280E tracked by OpenLiberty issue #4857
        }

        server.stopServer(toleratedWarnErrors);
    }
}

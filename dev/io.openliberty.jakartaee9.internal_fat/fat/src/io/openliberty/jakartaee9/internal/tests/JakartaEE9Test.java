/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
package io.openliberty.jakartaee9.internal.tests;

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
import io.openliberty.jakartaee9.internal.apps.jakartaee9.web.WebProfile9TestServlet;
import io.openliberty.jakartaee9.internal.tests.util.FATFeatureResolver;

/**
 * Test startup of the EE9 convenience features.
 *
 * Run up to four tests:
 *
 * <ul>
 * <li>Run with "webProfile-9.1".</li>
 * <li>Run with "jakartaee-9.1".</li>
 * <li>Run with all EE 9 compatible features (for open-liberty).</li>
 * <li>Run with all EE 9 compatible features (for WAS liberty).</li>
 * </ul>
 *
 * Skip the fourth case if WAS liberty has the same compatible
 * features as open liberty.
 */
@RunWith(FATRunner.class)
public class JakartaEE9Test extends FATServletClient {

    public static EE9Features ee9Features;

    static {
        try {
            ee9Features = new EE9Features(FATFeatureResolver.getInstallRoot());
        } catch (Exception e) {
            Assert.fail("Feature initialization error: " + e);
        }
    }

    public static EE9Features getFeatures() {
        return ee9Features;
    }

    //

    private static final String COMPAT_OL_FEATURES = "EE9CompatFeatures_OL";
    private static final String COMPAT_WL_FEATURES = "EE9CompatFeatures_WL";

    @ClassRule
    public static RepeatTests repeat;

    static {
        Set<String> olCompatFeatures = getFeatures().getExtendedCompatibleFeatures(EE9Features.OPEN_LIBERTY_ONLY);
        Set<String> wlCompatFeatures = getFeatures().getExtendedCompatibleFeatures(!EE9Features.OPEN_LIBERTY_ONLY);

        RepeatTests useRepeat = RepeatTests
                        .with(new FeatureReplacementAction()
                                        .removeFeature("webProfile-9.1")
                                        .addFeature("jakartaee-9.1")
                                        .withID("jakartaee91")
                                        .fullFATOnly()) // FULL
                        .andWith(new FeatureReplacementAction()
                                        .removeFeature("jakartaee-9.1")
                                        .addFeature("webProfile-9.1")
                                        .withID("webProfile91")
                                        .fullFATOnly()) // FULL
                        .andWith(new FeatureReplacementAction()
                                        .removeFeature("webProfile-9.1")
                                        .removeFeature("jakartaee-9.1")
                                        .addFeatures(olCompatFeatures)
                                        .withID(COMPAT_OL_FEATURES)); // LITE

        Set<String> featuresToAdd = new HashSet<>();
        for (String feature : wlCompatFeatures) {
            if (!olCompatFeatures.contains(feature)) {
                featuresToAdd.add(feature);
            }
        }
        if (!featuresToAdd.isEmpty()) {
            useRepeat = useRepeat.andWith(new FeatureReplacementAction()
                            .addFeatures(featuresToAdd)
                            .withID(COMPAT_WL_FEATURES)
                            .fullFATOnly());
        }

        repeat = useRepeat;
    }

    static {
        try {
            FATFeatureResolver.setup();
        } catch (Exception e) {
            Assert.fail("Feature initialization error: " + e);
        }
    }

    //

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
        if (RepeatTestFilter.isRepeatActionActive(COMPAT_WL_FEATURES) || RepeatTestFilter.isRepeatActionActive(COMPAT_OL_FEATURES)) {
            server.setServerStartTimeout(15 * 60 * 1000L); // 15 MIN
        }
        server.startServer(consoleName + ".log");
        server.waitForSSLStart();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        String[] toleratedWarnErrors;
        if (RepeatTestFilter.isRepeatActionActive(COMPAT_OL_FEATURES)) {
            toleratedWarnErrors = new String[] { "SRVE0280E", // TODO: SRVE0280E tracked by OpenLiberty issue #4857
                                                 "CWWKS5207W", // The remaining ones relate to config not done for the server / app
                                                 "CWMOT0010W",
                                                 "TRAS4352W" // Only happens when running with WebSphere Liberty image due to an auto feature
            };

        } else if (RepeatTestFilter.isRepeatActionActive(COMPAT_WL_FEATURES)) {
            toleratedWarnErrors = new String[] { "SRVE0280E", // TODO: SRVE0280E tracked by OpenLiberty issue #4857
                                                 "CWWKS5207W", // The remaining ones relate to config not done for the server / app
                                                 "CWMOT0010W",
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
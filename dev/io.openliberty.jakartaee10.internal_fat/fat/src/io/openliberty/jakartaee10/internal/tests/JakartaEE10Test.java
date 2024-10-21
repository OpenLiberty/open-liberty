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
package io.openliberty.jakartaee10.internal.tests;

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
import io.openliberty.jakartaee10.internal.apps.jakartaee10.web.WebProfile10TestServlet;
import io.openliberty.jarkartaee10.internal.tests.util.FATFeatureResolver;

/**
 * Test startup of the EE9 convenience features.
 *
 * Run up to four tests:
 *
 * <ul>
 * <li>Run with "webProfile-10.0".</li>
 * <li>Run with "jakartaee-10.0".</li>
 * <li>Run with all EE 10 compatible features (for open-liberty).</li>
 * <li>Run with all EE 10 compatible features (for WAS liberty).</li>
 * </ul>
 *
 * Skip the fourth case if WAS liberty has the same compatible
 * features as open liberty.
 */
@RunWith(FATRunner.class)
public class JakartaEE10Test extends FATServletClient {

    public static EE10Features ee10Features;

    static {
        try {
            ee10Features = new EE10Features(FATFeatureResolver.getInstallRoot());
        } catch (Exception e) {
            Assert.fail("Feature initialization error: " + e);
        }
    }

    public static EE10Features getFeatures() {
        return ee10Features;
    }

    //

    private static final String COMPAT_OL_FEATURES = "EE10CompatFeatures_OL";
    private static final String COMPAT_WL_FEATURES = "EE10CompatFeatures_WL";

    @ClassRule
    public static RepeatTests repeat;

    static {
        Set<String> olCompatFeatures = getFeatures().getExtendedCompatibleFeatures(EE10Features.OPEN_LIBERTY_ONLY);
        Set<String> wlCompatFeatures = getFeatures().getExtendedCompatibleFeatures(!EE10Features.OPEN_LIBERTY_ONLY);

        RepeatTests useRepeat = RepeatTests
                        .with(new FeatureReplacementAction()
                                        .removeFeature("webProfile-10.0")
                                        .addFeature("jakartaee-10.0")
                                        .withID("jakartaee10")
                                        .fullFATOnly()) // FULL
                        .andWith(new FeatureReplacementAction()
                                        .removeFeature("jakartaee-10.0")
                                        .addFeature("webProfile-10.0")
                                        .withID("webProfile10")
                                        .fullFATOnly()) // FULL
                        .andWith(new FeatureReplacementAction()
                                        .removeFeature("webProfile-10.0")
                                        .removeFeature("jakartaee-10.0")
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
        if (RepeatTestFilter.isRepeatActionActive(COMPAT_WL_FEATURES)) {
            server.setServerStartTimeout(15 * 60 * 1000L); // 15 minutes
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
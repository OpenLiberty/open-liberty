/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import java.nio.file.Paths;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.microprofile.telemetry.internal_fat.apps.userfeature.UserFeatureServlet;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;
import junit.framework.Assert;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class TelemetryUserFeatureTest extends FATServletClient {

    public static final String APP_NAME = "TelemetryUserFeatureTestApp";
    public static final String SERVER_NAME = "Telemetry10UserFeature";
    public static final String FEATURE_NAME = "telemetry.user.feature-1.0";
    public static final String FEATURE_JAKARTA_NAME = "telemetry.user.feature-2.0";
    public static final String WAB_FEATURE_NAME = "telemetry.user.wab-1.0";
    public static final String WAB_FEATURE_JAKARTA_NAME = "telemetry.user.wab-2.0";
    public static final String BUNDLE_NAME = "telemetry.user.feature";
    public static final String BUNDLE_JAKARTA_NAME = "telemetry.user.feature-jakarta";
    public static final String WAB_BUNDLE_NAME = "telemetry.user.wab";
    public static final String WAB_BUNDLE_JAKARTA_NAME = "telemetry.user.wab-jakarta";
    public static final String BUNDLE_PATH = "publish/bundles/";

    private static final String SEVER_XML_SNIPPET = "serverxmlsnippet.xml";
    private static final String JAVAX_SNIPPET = "javax/" + SEVER_XML_SNIPPET;
    private static final String JAKARTA_SNIPPET = "jakarta/" + SEVER_XML_SNIPPET;

    private static final String USER_WAB_CONTEXT_ROOT = "telemetryuserwab";

    @TestServlet(contextRoot = APP_NAME, servlet = UserFeatureServlet.class)
    @Server(SERVER_NAME)
    public static LibertyServer server;

    //Telemetry 20 because we're only testing runtime mode
    @ClassRule
    public static RepeatTests r = TelemetryActions.telemetry20Repeats(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {

        System.out.println("Install the user feature bundles...");

        if (JakartaEEAction.isEE9OrLaterActive()) {
            installBundle(BUNDLE_JAKARTA_NAME, BUNDLE_NAME);
            installBundle(WAB_BUNDLE_JAKARTA_NAME, WAB_BUNDLE_NAME);

            server.installUserFeature(FEATURE_JAKARTA_NAME);
            server.installUserFeature(WAB_FEATURE_JAKARTA_NAME);

            //Rather than force all the other tests to be aware of our user feature so a feature replacement action
            //can modify it. We'll use an includes in the server.xml and slide in a snippet with the right version
            server.copyFileToLibertyServerRoot(JAKARTA_SNIPPET);
        } else {
            installBundle(BUNDLE_NAME, BUNDLE_NAME);
            installBundle(WAB_BUNDLE_NAME, WAB_BUNDLE_NAME);

            server.installUserFeature(FEATURE_NAME);
            server.installUserFeature(WAB_FEATURE_NAME);

            server.copyFileToLibertyServerRoot(JAVAX_SNIPPET);
        }

        // Don't enable otel sdk here. Use server.env so we test a runtime instance
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(UserFeatureServlet.class.getPackage());

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);

        server.addEnvVar("OTEL_SDK_DISABLED", "false");

        server.startServer();
    }

    private static void installBundle(String bundleName, String bundleNameWithoutJakarta) throws Exception {
        String fullPath = BUNDLE_PATH + bundleName;
        LocalFile bundleFile = new LocalFile(fullPath);
        if (bundleFile.exists()) {
            bundleFile.delete();
        }

        if (JakartaEEAction.isEE9OrLaterActive()) {
            JakartaEEAction.transformApp(Paths.get(BUNDLE_PATH + bundleNameWithoutJakarta + ".jar"), Paths.get(fullPath + ".jar"));
        }

        server.installUserBundle(bundleName);
    }

    @Test
    public void testTelemetryEnabledInWAB() throws Exception {
        String wabOutput = new HttpRequest(server, "/" + USER_WAB_CONTEXT_ROOT + "/servletInsideWab")
                        .expectCode(200)
                        .run(String.class);

        Assert.assertEquals("telemetry enabled: true", wabOutput.trim());
    }

    @Test
    public void testTelemetryFilterInApp() throws Exception {
        String appOutput = new HttpRequest(server, "/" + APP_NAME + "/servletInsideApp")
                        .expectCode(200)
                        .run(String.class);

        Assert.assertEquals("telemetry enabled: true", appOutput.trim());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();

        System.out.println("Unnstall the user feature bundles...");
        if (JakartaEEAction.isEE9OrLaterActive()) {
            server.uninstallUserBundle(BUNDLE_JAKARTA_NAME);
            server.uninstallUserFeature(FEATURE_JAKARTA_NAME);
            server.uninstallUserBundle(WAB_BUNDLE_JAKARTA_NAME);
            server.uninstallUserFeature(WAB_FEATURE_JAKARTA_NAME);
        } else {
            server.uninstallUserBundle(BUNDLE_NAME);
            server.uninstallUserFeature(FEATURE_NAME);
            server.uninstallUserBundle(WAB_BUNDLE_NAME);
            server.uninstallUserFeature(WAB_FEATURE_NAME);
        }
        server.deleteFileFromLibertyServerRoot(SEVER_XML_SNIPPET);
    }

}
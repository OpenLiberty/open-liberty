/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi40.internal.fat.bce;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.DISABLE_VALIDATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.cdi40.internal.fat.bce.doesnotimplement.ExtensionNotImplementingBce;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;

/**
 * Tests for error cases with build compatible extensions
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class BuildCompatibleExtensionsErrorTest {

    private static final String STATE_CHANGE_EXCEPTION = "com.ibm.ws.container.service.state.StateChangeException";
    private static final String CDI_EXCEPTION = "com.ibm.ws.cdi.CDIException";
    private static final String CLASS_NOT_FOUND_EXCEPTION = "java.lang.ClassNotFoundException";
    private static final String CLASS_CAST_EXCEPTION = "java.lang.ClassCastException";

    public static final String SERVER_NAME = "cdiBceTestServer";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE11);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWWKZ0002E", //Failed to start app
                          "CWOWB1013E", //for testBceMissing
                          "CWOWB1014E"  //for testBceDoesNotImplementInterface
        );
    }

    @Test
    @ExpectedFFDC({ STATE_CHANGE_EXCEPTION,
                    CDI_EXCEPTION,
                    CLASS_NOT_FOUND_EXCEPTION,
    })
    public void testBceMissing() throws Exception {
        try {
            WebArchive warMissingBce = ShrinkWrap.create(WebArchive.class, "warMissingBce.war")
                                                 .addAsServiceProvider(BuildCompatibleExtension.class.getName(), "io.openliberty.MissingExtension");
            server.setMarkToEndOfLog();
            ShrinkHelper.exportDropinAppToServer(server, warMissingBce, DeployOptions.SERVER_ONLY, DISABLE_VALIDATION);

            // Wait for app to either start or fail to start
            assertNotNull(server.waitForStringInLogUsingMark("CWWKZ000[1,2]E"));

            // Check for error that the BCE is not loadable
            String logLine = server.waitForStringInLogUsingMark("CWOWB1013E");
            // Check that the error includes the BCE class name
            assertThat(logLine, containsString("io.openliberty.MissingExtension"));
        } finally {
            server.deleteFileFromLibertyServerRoot("dropins/warMissingBce.war");
        }
    }

    @Test
    @ExpectedFFDC({ STATE_CHANGE_EXCEPTION,
                    CDI_EXCEPTION,
                    CLASS_CAST_EXCEPTION,
    })
    public void testBceDoesNotImplementInterface() throws Exception {
        try {
            WebArchive warBceDoesNotImplement = ShrinkWrap.create(WebArchive.class, "warBceDoesNotImplement.war")
                                                          .addClass(ExtensionNotImplementingBce.class)
                                                          .addAsServiceProvider(BuildCompatibleExtension.class, ExtensionNotImplementingBce.class);
            server.setMarkToEndOfLog();
            ShrinkHelper.exportDropinAppToServer(server, warBceDoesNotImplement, DeployOptions.SERVER_ONLY, DISABLE_VALIDATION);

            // Wait for app to either start or fail to start
            assertNotNull(server.waitForStringInLogUsingMark("CWWKZ000[1,2]E"));

            // Check for error that the BCE does not implement the BCE interface
            String logLine = server.waitForStringInLogUsingMark("CWOWB1014E");
            // Check that the error includes the BCE class name
            assertThat(logLine, containsString(ExtensionNotImplementingBce.class.getName()));
        } finally {
            server.deleteFileFromLibertyServerRoot("dropins/warBceDoesNotImplement.war");
        }
    }

}

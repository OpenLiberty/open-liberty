/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

package io.openliberty.checkpoint.fat;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.fat.passwordutil.web.DefaultPrincipalMappingServlet;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import junit.framework.AssertionFailedError;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@SkipIfCheckpointNotSupported
public class PasswordUtilsTest extends FATServletClient {
    private static final String APP_NAME = "DefaultPrincipalMappingApp";
    private static final String APP_PACKAGE = "io.openliberty.checkpoint.fat.passwordutil.web";
    private static final String SERVER_NAME = "com.ibm.ws.security.auth.data.fat.dpm.pu11";

    @Server(SERVER_NAME)
    @TestServlet(servlet = DefaultPrincipalMappingServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, APP_PACKAGE);
        server.addInstalledAppForValidation(APP_NAME);
        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, (s) -> {
            // ACTION before restore
            // set the env for AUTO_USER_NAME and AUTO_USER_PASSWORD
            Map<String, String> envMap = new HashMap<>();
            envMap.put("AUTH_USER_NAME", "testUser");
            envMap.put("AUTH_USER_PASSWORD", "testPassword");
            try {
                FATSuite.configureEnvVariable(s, envMap);
            } catch (Exception e) {
                AssertionFailedError failed = new AssertionFailedError(e.getMessage());
                failed.initCause(e);
                throw failed;
            }
        });
        server.startServer(PasswordUtilsTest.class.getSimpleName() + ".log");
        server.checkpointRestore();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}

/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.fat.passwordutil.web.DefaultPrincipalMappingServlet;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import junit.framework.AssertionFailedError;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@CheckpointTest
public class PasswordUtilsTest extends FATServletClient {
    private static final String APP_NAME = "DefaultPrincipalMappingApp";
    private static final String APP_PACKAGE = "io.openliberty.checkpoint.fat.passwordutil.web";
    private static final String SERVER_NAME = "checkpointPasswordUtilities";

    @Server(SERVER_NAME)
    @TestServlet(servlet = DefaultPrincipalMappingServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction("appSecurity-3.0", "appSecurity-1.0").removeFeature("passwordUtilities-1.1")
                                    .addFeature("passwordUtilities-1.0")
                                    .withID("version1.0")
                                    .forServers(SERVER_NAME)
                                    .fullFATOnly());

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, APP_PACKAGE);
        server.addInstalledAppForValidation(APP_NAME);
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, (s) -> {
            // ACTION before restore
            // set the env for AUTO_USER_NAME and AUTO_USER_PASSWORD
            Map<String, String> envMap = new HashMap<>();
            envMap.put("AUTH_USER_NAME", "testUser");
            envMap.put("AUTH_USER_PASSWORD",
                       "{aes}ARAXt8l79yOF6iRwLqS2Skvu9JwGfke14pWWKKg1ZMHROoHojIL6ekKo7TLJFbYEIqlORBeFU4RAfTsyIJUJfXf1AWl/J/hpZHaDZsG/k9+bejJEZk15jSVoJRtr9+KJEAllGWBmloXLbMDcD+2j"); //testPassword
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

/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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

package io.openliberty.checkpoint.fat.appclientsupport;

import static io.openliberty.checkpoint.fat.appclientsupport.FATSuite.transformApp;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class AppClientSupportTest {
    private static Class<?> c = AppClientSupportTest.class;

    private static final String app = "InjectionApp.ear";

    @Rule
    public TestName testName = new TestName();

    private static LibertyClient client = LibertyClientFactory.getLibertyClient("clientInjection");
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("serverInjection");

    @BeforeClass
    public static void beforeClass() throws Exception {
        transformApp(server.getServerRoot(), app);
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true, null);
        server.startServer();
        transformApp(client.getClientRoot(), app);
        client.startClient();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }

    /**
     * Tests that a remote EJB is injected when using a
     * java:global lookup.
     */
    @Test
    public void injectGlobal_EJB() throws Exception {
        checkInjection();
    }

    /**
     * Tests that a remote EJB is injected.
     */
    @Test
    public void inject_EJB() throws Exception {
        checkInjection();
    }

    // Assisted by watsonx Code Assistant
    /**
     * This method tests the injection of CDI.
     */
    @Test
    public void inject_CDI() throws Exception {
        checkInjection();
    }

    private void checkInjection() throws Exception {
        String methodName = testName.getMethodName();
        int idx = -1;
        if ((idx = methodName.indexOf("_EE")) != -1) {
            methodName = methodName.substring(0, idx);
        }
        List<String> strings = client.findStringsInCopiedLogs(methodName + "-PASSED");
        Log.info(c, methodName, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + methodName, strings != null && strings.size() >= 1);
    }

}

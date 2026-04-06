/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat.springboot.data;

import static io.openliberty.checkpoint.fat.springboot.data.FATSuite.configureEnvVariable;
import static io.openliberty.checkpoint.fat.springboot.data.FATSuite.doSpringBootDataTest;
import static io.openliberty.checkpoint.fat.springboot.data.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.springboot.data.FATSuite.setUp;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
@MinimumJavaLevel(javaLevel = 17)
public class SpringBootDeployDataTests40 extends FATServletClient {
    private static final String APP_NAME = "io.openliberty.checkpoint.springboot.fat40.data.app-1.0.0.war";
    @Rule
    public TestName testName = new TestName();

    @Server("checkpointSpringBootData40")
    public static LibertyServer server;

    @Before
    public void setUpServer() throws Exception {
        TestMethod testMethod = getTestMethod(TestMethod.class, testName);
        CheckpointPhase testPhase = switch (testMethod) {
            case testSpringBootAfterAppStart -> CheckpointPhase.AFTER_APP_START;
            case testSpringBootBeforeAppStart -> CheckpointPhase.BEFORE_APP_START;
            default -> throw new IllegalArgumentException("Unexpected value: " + getTestMethodSimpleName(testName));
        };
        setUp(server, APP_NAME, false, testPhase, testMethod.toString());
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
        configureEnvVariable(server, Map.of());
    }

    @Test
    @Ignore // need to investigate why the datasource username/password don't get replaced on restore
    public void testSpringBootAfterAppStart() throws Exception {
        doSpringBootDataTest(server);
    }

    @Test
    public void testSpringBootBeforeAppStart() throws Exception {
        doSpringBootDataTest(server);
    }

    static enum TestMethod {
        testSpringBootAfterAppStart,
        testSpringBootBeforeAppStart,
        unknown
    }
}

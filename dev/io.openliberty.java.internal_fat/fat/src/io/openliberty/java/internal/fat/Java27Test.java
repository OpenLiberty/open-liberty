/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.java.internal.fat;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 27)
public class Java27Test extends FATServletClient {

    public static final String APP_NAME = "io.openliberty.java.internal_fat_27";

    @Server("java27-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // NOTE: This FAT uses a pre-compiled application which is compiled at the bytecode
        // level of JDK 27, which is higher than what our build systems normally use.
        // Code source files for this WAR file can be found in the src-reference/java27 directory at the root of this FAT.
        // The full project for building the required WAR file can be found here:
        // https://github.com/OpenLiberty/open-liberty-misc/tree/main/io.openliberty.java.internal_fat_27
        server.addInstalledAppForValidation(APP_NAME);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test JEP 527 — Post-Quantum Hybrid Key Exchange for TLS 1.3.
     * X25519MLKEM768 is the primary hybrid group mandated by JEP 527 and is hard-asserted.
     * SecP256r1MLKEM768 and SecP384r1MLKEM1024 are optional — present on some JDK builds
     * but not all (e.g. absent on Oracle JDK 27 EA). The WAR logs them as
     * "Hybrid group optional (not present on this JDK build)" when absent.
     * Supported by Oracle JDK 27 and IBM Semeru SR8 FP55+ (APAR IJ55706).
     */
    @Test
    public void testPostQuantumTLS() throws Exception {
        String appResponse = HttpUtils.getHttpResponseAsString(server, APP_NAME + '/');
        assertContains(appResponse, "Beginning JEP 527 testing: Post-Quantum Hybrid Key Exchange for TLS 1.3");
        assertContains(appResponse, "Hybrid group present: X25519MLKEM768");
        assertContains(appResponse, "Leaving JEP 527 testing");
    }

    /**
     * Test JEP 534 — Compact Object Headers by Default.
     * Verifies that UseCompactObjectHeaders is enabled via HotSpotDiagnosticMXBean.
     * The flag is explicitly set to true in jvm.options to make the test self-contained.
     * Non-HotSpot JVMs skip this check gracefully.
     */
    @Test
    public void testCompactObjectHeaders() throws Exception {
        String appResponse = HttpUtils.getHttpResponseAsString(server, APP_NAME + '/');
        assertContains(appResponse, "Beginning JEP 534 testing: Compact Object Headers by Default");
        assertTrue("Expected JEP 534 SUCCESS or skip notice",
                   appResponse.contains("SUCCESS: Compact object headers are active (UseCompactObjectHeaders=true)")
                   || appResponse.contains("skipping JEP 534 check"));
        assertContains(appResponse, "Leaving JEP 534 testing");
    }

    /**
     * Test JEP 536 — JFR In-Process Data Redaction.
     * Verifies that a system property whose key matches the default *password* filter
     * is recorded as [REDACTED] in a JFR recording rather than in plain text.
     * On JVMs where JEP 536 is not yet active the servlet logs a NOTE instead of failing.
     * The property -Djep536.test.password=test-fixture-value is set in jvm.options.
     */
    @Test
    public void testJFRDataRedaction() throws Exception {
        String appResponse = HttpUtils.getHttpResponseAsString(server, APP_NAME + '/');
        assertContains(appResponse, "Beginning JEP 536 testing: JFR In-Process Data Redaction");
        assertTrue("Expected JEP 536 SUCCESS or NOTE",
                   appResponse.contains("SUCCESS: JEP 536 redacted")
                   || appResponse.contains("NOTE:"));
        assertContains(appResponse, "Leaving JEP 536 testing");
        assertContains(appResponse, "<<< EXIT SUCCESSFUL");
    }

    /**
     * Test the full Java 27 application end-to-end.
     */
    @Test
    public void testJava27App() throws Exception {
        String appResponse = HttpUtils.getHttpResponseAsString(server, APP_NAME + '/');
        assertContains(appResponse, "Beginning Java 27 testing");
        assertContains(appResponse, "Leaving Java 27 testing");
        assertContains(appResponse, "<<< EXIT SUCCESSFUL");
    }

    private static void assertContains(String str, String lookFor) {
        assertTrue("Expected to find string '" + lookFor + "' but it was not found in the response: " + str, str.contains(lookFor));
    }
}

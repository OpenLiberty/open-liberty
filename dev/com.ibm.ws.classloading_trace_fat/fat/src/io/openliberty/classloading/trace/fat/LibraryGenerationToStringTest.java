/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.classloading.trace.fat;

import static io.openliberty.classloading.classpath.fat.FATSuite.TRACE_TEST_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TRACE_TEST_EAR;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test to verify LibraryGeneration.toString() format in trace output
 * 
 * This test validates that when a shared library is loaded, the trace output
 * shows the LibraryGeneration object with the format: LibraryGeneration@[hex][libraryId]
 */
@RunWith(FATRunner.class)
public class LibraryGenerationToStringTest extends FATServletClient {

    private static final String SERVER_NAME = "traceTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Export complex EAR to server (reuse existing test infrastructure)
        ShrinkHelper.exportAppToServer(server, TRACE_TEST_EAR, DeployOptions.SERVER_ONLY);

        // Start the server - this will trigger library loading
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE9967W");
        }
    }

    /**
     * Test that verifies LibraryGeneration.toString() format in trace output
     * 
     * Expected format: LibraryGeneration@[hexadecimal][libraryId]
     * Example: LibraryGeneration@506dd778[testSharedLibrary]
     * 
     * This test:
     * 1. Waits for the publishGeneration Entry trace message
     * 2. Verifies the trace contains LibraryGeneration object
     * 3. Validates the toString() format includes the library ID in brackets
     */
    @Test
    public void testLibraryGenerationToStringFormat() throws Exception {
        // Wait for the LibraryGeneration object in trace (appears after publishGeneration Entry)
        String traceEntry = server.waitForStringInTrace("LibraryGeneration@.*\\[testSharedLibrary\\]");
        assertNotNull("Should find 'LibraryGeneration@...[testSharedLibrary]' in trace file", traceEntry);

        System.out.println("Found trace entry: " + traceEntry);

        // Verify the trace contains LibraryGeneration object reference
        assertTrue("Trace should contain 'LibraryGeneration@'",
                   traceEntry.contains("LibraryGeneration@"));

        // Pattern to match: LibraryGeneration@[hex][libraryId]
        // Example: LibraryGeneration@506dd778[testSharedLibrary]
        Pattern pattern = Pattern.compile("LibraryGeneration@[a-f0-9]+\\[.+\\]");
        assertTrue("Trace should match pattern 'LibraryGeneration@[hex][libraryId]'",
                   pattern.matcher(traceEntry).find());

        // Verify the library ID is present in brackets
        assertTrue("Trace should contain library ID 'testSharedLibrary' in brackets",
                   traceEntry.contains("[testSharedLibrary]"));

        System.out.println("SUCCESS: LibraryGeneration.toString() format verified in trace output");
    }

    /**
     * Test that verifies the LibraryGeneration toString was successfully invoked
     * This is validated by the first test finding the toString output
     */
    @Test
    public void testTraceComponentIsSharedLibrary() throws Exception {
        // The first test already verified LibraryGeneration.toString() appears in trace
        // This test just confirms the trace infrastructure is working
        String traceEntry = server.waitForStringInTrace("LibraryGeneration@.*\\[testSharedLibrary\\]");
        assertNotNull("Should find LibraryGeneration toString output in trace", traceEntry);

        System.out.println("SUCCESS: LibraryGeneration.toString() was invoked and logged to trace");
    }
}

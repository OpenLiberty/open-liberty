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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 26)
@MaximumJavaLevel(javaLevel = 26)
public class Java26Test extends FATServletClient {

    public static final String APP_NAME = "io.openliberty.java.internal_fat_26";

    @Server("java26-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // NOTE: This FAT uses a pre-compiled application which is compiled at the bytecode
        // level of JDK 26, which is higher than what our build systems normally use
        // Code source files for this WAR file this FAT uses can be found in the src-reference/java26 directory at the root of this FAT
        // The full project for building the required WAR file can be found here: https://github.com/OpenLiberty/open-liberty-misc/tree/main/io.openliberty.java.internal_fat_26
        server.addInstalledAppForValidation(APP_NAME);
        // Server is started in each test method to allow for different JVM options
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Server is stopped in each test method's finally block
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test JEP 500 with --illegal-final-field-mutation=deny mode (strict enforcement)
     * This is the future default behavior where final field mutations are blocked.
     */
    @Test
    public void testJava26AppWithDenyMode() throws Exception {
        server.startServer();
        try{
        String appResponse = HttpUtils.getHttpResponseAsString(server, APP_NAME + '/');
        assertContains(appResponse, "Beginning Java 26 testing");
        assertContains(appResponse, "Beginning JEP 500 testing: Prepare to Make Final Mean Final");
        assertContains(appResponse, "Testing final field mutation protection");
        assertContains(appResponse, "SUCCESS: IllegalAccessException caught - running in DENY mode");
        assertContains(appResponse, "Final field mutation was blocked as expected");
        assertContains(appResponse, "RESULT: Final field remained immutable (value: Original)");
        assertContains(appResponse, "JEP 500 Test Summary:");
        assertContains(appResponse, "Mode detected: DENY");
        assertContains(appResponse, "Testing HTTP/3 Support (JEP 517)");
        assertContains(appResponse, "HTTP Client created with HTTP/3 support");
        assertContains(appResponse, "<<< EXIT SUCCESSFUL");
        } finally {
            server.stopServer();
        }
        
    }

    /**
     * Test JEP 500 with --illegal-final-field-mutation=warn mode (permissive with warnings)
     * This is the current Java 26 default behavior where mutations are allowed but warnings are issued.
     * Note: 'warn' is the default mode, so we just remove the --illegal-final-field-mutation flag.
     */
    @Test
    public void testJava26AppWithWarnMode() throws Exception {
        
        // Update JVM options to use default warn mode (remove --illegal-final-field-mutation=deny)
        server.setJvmOptions(java.util.Arrays.asList("--enable-preview"));
        
        server.startServer();
        
       try{
            String appResponse = HttpUtils.getHttpResponseAsString(server, APP_NAME + '/');
             // In warn mode, the mutation should succeed
        assertContains(appResponse, "Beginning Java 26 testing");
        assertContains(appResponse, "Beginning JEP 500 testing: Prepare to Make Final Mean Final");
                assertTrue("Should contain WARN mode result",
                           appResponse.contains("RESULT: Mutation succeeded in WARN mode - field value changed to"));
        assertContains(appResponse, "Check server logs for JEP 500 warning messages");
        assertContains(appResponse, "RESULT: Mutation succeeded in WARN mode");
        assertContains(appResponse, "JEP 500 Test Summary:");
        assertContains(appResponse, "Mode detected: WARN");
        assertContains(appResponse, "Mutation blocked: false");
        
    
        // In warn mode, the JVM prints warnings to System.err, which Liberty redirects to console.log
        // Expected warning formats (JEP 500):
        // 1. "WARNING: Final field f in p.C has been mutated by class com.foo.Bar.caller in module N"
        // 2. "WARNING: Use --enable-final-field-mutation=N to avoid a warning"
        // 3. "WARNING: Mutating final fields will be blocked in a future release unless final field mutation is enabled"
        
        
        // Now check for the specific detailed mutation warning using waitForStringInLog
        // This should match: "WARNING: Final field name in class ... has been mutated reflectively by class ..."
        String detailedWarning = server.waitForStringInLog("WARNING.*Final field.*has been mutated.*TestService", 5000, server.getConsoleLogFile());
        assertNotNull("Expected to find detailed JVM warning about final field mutation in console.log", detailedWarning);
        
        // Verify HTTP/3 test still works
        assertContains(appResponse, "Testing HTTP/3 Support (JEP 517)");
        assertContains(appResponse, "HTTP Client created with HTTP/3 support");
        assertContains(appResponse, "<<< EXIT SUCCESSFUL");
       }finally {
            server.stopServer();
             // Restore original JVM options for subsequent tests
            server.setJvmOptions(java.util.Arrays.asList("--enable-preview", "--illegal-final-field-mutation=deny"));
        }
    }

    private static void assertContains(String str, String lookFor) {
        assertTrue("Expected to find string '" + lookFor + "' but it was not found in the string: " + str, str.contains(lookFor));
    }
}


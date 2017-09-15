/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class BadConfigTests {

    @Test
    public void testBadPortConfig() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.bad");
        server.startServer("badconfig.log");

        try {
            // Start the server with bad configuration
            assertNotNull("There should be an error during server start", server.waitForStringInLog("CWWKG0075E.*missingbvt.prop.HTTP_default.*"));
            assertNotNull("The server should start", server.waitForStringInLog("CWWKF0011I.*"));
            assertNull("There should not be a cached instance used", server.waitForStringInLog("CWWKG0076W", 1000));

            // Update the configuration to good values
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("badconfig/goodConfig.xml");

            assertNotNull("The config should be updated", server.waitForStringInLogUsingMark("CWWKG0017I.*"));
            assertNull("There should not be a validation error", server.waitForStringInLogUsingMark("CWWKG0075E", 1000));

            // Update at runtime to an invalid configuration. The cached values should still be used
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("badconfig/badHttp.xml");

            assertNotNull("There should be an error during server start", server.waitForStringInLogUsingMark("CWWKG0075E.*missingbvt.prop.HTTP_default.*"));
            assertNotNull("There should be a cached instance used", server.waitForStringInLogUsingMark("CWWKG0076W"));
            assertNotNull("The config should be updated", server.waitForStringInLogUsingMark("CWWKG0017I.*"));

            // Update a singleton (<logging/>) at runtime. The cached values should still be used
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("badconfig/badLogging.xml");

            assertNotNull("There should be a warning during server start", server.waitForStringInLogUsingMark("CWWKG0083W(?=.*maxFiles)(?=.*-1)(?=.*2)"));
            assertNotNull("The config should be updated", server.waitForStringInLogUsingMark("CWWKG0017I.*"));

        } finally {
            server.stopServer();
        }

    }

    @Test
    public void testUniqueValueConflictConfig() throws Exception {

        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.uniquevalueconflict");
        server.startServer("uniquevalueconflict.log");
        try {
            // Update the configuration to good values
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("badconfig/UniqueValueConflict.xml");

            assertNotNull(" Distinct values specified for the attribute", server.waitForStringInLogUsingMark("CWWKG0031E.*"));
            assertNotNull(" ConfigUpdateException does not occur", server.waitForStringInLogUsingMark("CWWKG0074E.*"));
        }

        finally {
            server.stopServer();
        }

    }

    @Test
    public void testInvalidOptionInJDBCConfig() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.invalidJDBCoption");
        server.startServer("invalidJDBCoption.log");

        assertNotNull("The server should start", server.waitForStringInLog("CWWKF0011I.*"));

        // Clear log offsets so we can search for all warning messages
        server.resetLogOffsets();

        try {
            List<String> matches = server.findStringsInLogs("CWWKG0032W.*createDatabase.*whatever.*");
            // Start the server with invalid configuration and ensure that the warning is printed exactly once
            assertEquals("There should be exactly one (1) warning for createDatabase", 1, matches.size());
        } finally {
            server.stopServer();
        }

    }

    @Test
    @ExpectedFFDC("java.net.MalformedURLException")
    public void testInvalidOptionalInclude() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.invalidOptionalInclude");
        server.startServer("invalidOptionalInclude.log");

        try {
            // Start the server, skipping the optional include with a bad protocol - ensure warning message is printed
            assertNotNull("There should be an error during server start", server.waitForStringInLog("CWWKG0084W.*bogus.*"));
        } finally {
            server.stopServer();
        }

    }
}

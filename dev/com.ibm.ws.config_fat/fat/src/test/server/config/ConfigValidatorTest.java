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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests the XML signing and validation functions, ensuring correct behavior when signing is enforced
 * and invalid documents are encountered.
 */
public class ConfigValidatorTest {

    // Since we have tracing enabled give server longer timeout to start up.
    private static final long SERVER_START_TIMEOUT = 30 * 1000;

    @Test
    @ExpectedFFDC({ "com.ibm.websphere.config.ConfigValidationException", "java.lang.ClassNotFoundException" })
    public void testValidator() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.validator");
        String jarName = "com.ibm.ws.config.ext_1.0." + server.getMicroVersion() + ".jar";
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");

        // Backup fragment and bring in embedder validation fragment
        LibertyFileManager.renameLibertyFile(server.getMachine(),
                                             server.getInstallRoot() + "/lib/" + jarName,
                                             server.getInstallRoot() + "/lib/" + jarName + ".bak");
        server.copyFileToLibertyInstallRoot("lib", "validator/" + jarName);

        try {
            server.setServerStartTimeout(SERVER_START_TIMEOUT);

            server.startServer("goodSignature.log", true, true, false);
            assertNotNull("Configuration validation should be issued",
                          server.waitForStringInLog("CWWKG0043I:.*EmbeddedXMLConfigValidator"));

            // Copy in invalid server.xml, trigger refresh
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("validator/bad-signature.xml");
            assertNotNull("An error message should be issued with invalid config refresh",
                          server.waitForStringInLog("CWWKG0047E:.*"));
            assertNotNull("A warning message should be issued with invalid config refresh",
                          server.waitForStringInLog("CWWKG0057W:.*"));
            assertTrue(server.isStarted());

            // Stop server, don't clean up.
            server.stopServer(false);

            try {
                server.startServerExpectFailure("badSignature.log", false, false);
            } catch (Exception e) {
                System.out.println("Caught exception of type " + e.getClass());
            }
            assertNotNull("An error message should be issued on invalid config startup",
                          server.waitForStringInLog("CWWKG0047E:.*"));
            assertNotNull("A fatal message should be issued on invalid config startup",
                          server.waitForStringInLog("CWWKG0044E:.*"));
            assertNotNull("The server should be stopped after invalid config startup",
                          server.waitForStringInLog("CWWKE0036I:.*"));

            // Stop server, don't clean up.
            server.stopServer(false);

            // Copy in invalid server.xml
            server.setServerConfigurationFile("validator/dropins-enabled.xml");

            try {
                server.startServerExpectFailure("dropinsEnabled.log", false, false);
            } catch (Exception e) {
                System.out.println("Caught exception of type " + e.getClass());
            }
            assertNotNull("There should be a message about a valid signature",
                          server.waitForStringInLog("CWWKG0055I:.*"));
            assertNotNull("There should be a fatal message because dropins are enabled",
                          server.waitForStringInLog("CWWKG0056E:.*"));
            assertNotNull("The server should be stopped after invalid config startup",
                          server.waitForStringInLog("CWWKE0036I:.*"));

        } finally {
            // Just in case one of the conditions above failed, make sure the server
            // stops. Skip the server archive here (it would get skipped for already stopped)
            server.stopServer(false);

            // Make sure we *do* capture the data for these attempts
            server.postStopServerArchive();

            // Restore default validation fragment
            LibertyFileManager.deleteLibertyFile(server.getMachine(),
                                                 server.getInstallRoot() + "/lib/" + jarName);
            LibertyFileManager.renameLibertyFile(server.getMachine(),
                                                 server.getInstallRoot() + "/lib/" + jarName + ".bak",
                                                 server.getInstallRoot() + "/lib/" + jarName);
        }
    }

}

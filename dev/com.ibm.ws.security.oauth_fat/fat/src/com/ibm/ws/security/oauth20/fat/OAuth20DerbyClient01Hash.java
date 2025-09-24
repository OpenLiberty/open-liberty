/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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
package com.ibm.ws.security.oauth20.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.JavaInfo.Vendor;
import com.ibm.websphere.simplicity.log.Log;

@RunWith(FATRunner.class)
public class OAuth20DerbyClient01Hash extends OAuth20Client01Common {

    private static final Class<?> thisClass = OAuth20DerbyClient01Hash.class;

    /**
     * TestDescription:
     *
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code" . In this scenario, the OAuth client is
     * registered using database, instead of using XML file. Derby database is
     * used for storing the registered clients. In this scenario, the autoauthz
     * parameter is set to true, so the resource owner does not receive the
     * consent form from the authorization server. The test verifies that the
     * Oauth code flow, using the authorization grant type of "authorization
     * code" works correctly with JDBC database client.
     *
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup(DERBY_STORE_SERVER);
        assertNotNull("The application oAuth20DerbySetup failed to start",
                      server.waitForStringInLog("CWWKZ0001I.*oAuth20DerbySetup"));
    }

    /**
     * Temporary workaround for RTC 305962 on IBM Java8 FIPS SOE builds where beta guard is not enabled and the default iterations value 2048 is used.
     * TODO: Once the beta guard is removed from Semeru FIPS SOE builds, the workaround will also be removed and the enhanced value will be used for both java with FIPS.
     *
     */ 
    @Override
    @Test
    @AllowedFFDC({ "java.sql.SQLRecoverableException" })
    public void testOAuthDerbyCodeFlow() throws Exception {
        super.testOAuthDerbyCodeFlow();
  
        JavaInfo javaInfo = JavaInfo.forServer(server);
        Log.info(thisClass, "testOAuthDerbyCodeFlow", "javaInfo.majorVersion() is: " + javaInfo.majorVersion() + ", and " + "isIBMJVM() is: " + server.isIBMJVM());
        boolean isIBMJVMGreaterOrEqualTo11 = ((javaInfo.majorVersion() >= 11) && (server.isIBMJVM()));
        boolean isSemeruFips = (server.isFIPS140_3EnabledAndSupported() && isIBMJVMGreaterOrEqualTo11) ;
        String iterations = isSemeruFips ? "210000" : "2048"; 

        String msg = checkDerbyEntry("http://" + server.getHostname() + ":" + server.getHttpDefaultPort(), server.getHttpDefaultPort(), "dclient01", "OAuthConfigDerby");
        assertNotNull("Servlet should have returned a secret type", msg);
        assertEquals("Secret type is incorrect in the database.", "hash", msg);

        msg = checkDerbyIteration("http://" + server.getHostname() + ":" + server.getHttpDefaultPort(), server.getHttpDefaultPort(), "dclient01", "OAuthConfigDerby");
        assertNotNull("Servlet should have returned an iteration type for " + clientID, msg);
        assertEquals("Iteration is incorrect in the database for client " + clientID, iterations, msg);

        msg = checkDerbyAlgorithm("http://" + server.getHostname() + ":" + server.getHttpDefaultPort(), server.getHttpDefaultPort(), "dclient01", "OAuthConfigDerby");
        assertNotNull("Servlet should have returned an algorithm type for " + clientID, msg);
        assertEquals("Algorithm is incorrect in the database for client " + clientID, "PBKDF2WithHmacSHA512", msg);

        // Go through flow a second time to ensure we can still login -- the plain text password on the pre-populated database be converted to a hash
        testOAuthCommonCodeFlow();
    }

}


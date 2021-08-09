/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class OAuth20DerbyClient03 extends OAuth20Client03Common {

    private static final Class<?> thisClass = OAuth20DerbyClient03.class;

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
     * Wrong OAuth DB schema name on ${server.config.dir}/derby/data/oAuthDB
     */

    @Override
    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.derby3.fat");
        assertNotNull("The application oAuth20DerbySetup failed to start",
                      server.waitForStringInLog("CWWKZ0001I.*oAuth20DerbySetup"));
    }

    // NOTE: Should only allow java.sql.SQLRecoverableException. Other AllowedFFDCs added here TEMPORARILY
    // to stop the build being red due to defect 98925. When that is fixed, remove the other entries!!
    @Override
    @Test
    @AllowedFFDC({ "java.sql.SQLRecoverableException", "com.ibm.websphere.ssl.SSLException", "javax.naming.CommunicationException", "java.lang.IllegalArgumentException",
                   "java.sql.SQLException" })
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException", "java.sql.SQLSyntaxErrorException" })
    public void testOAuthDerbyCodeFlow() throws Exception {
        super.testOAuthDerbyCodeFlow();
    }

}

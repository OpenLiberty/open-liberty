/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;

@MinimumJavaLevel(javaLevel = 8)
@RunWith(FATRunner.class)
public class OAuth20CustomStoreClient01Hash extends OAuth20Client01Common {

    private static final Class<?> thisClass = OAuth20CustomStoreClient01Hash.class;

    /**
     * TestDescription:
     *
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code" . In this scenario, the OAuth client is
     * registered using database, instead of using XML file. MongoDB database is
     * used for storing the registered clients. In this scenario, the autoauthz
     * parameter is set to true, so the resource owner does not receive the
     * consent form from the authorization server. The test verifies that the
     * Oauth code flow, using the authorization grant type of "authorization
     * code" works correctly with a MongoDB backend, using a user feature to implement
     * the CustomStore.
     *
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup(MONGO_STORE_FEATURE_SERVER);
        assertNotNull("The application oAuth20MongoSetup failed to start",
                      server.waitForStringInLog("CWWKZ0001I.*oAuth20MongoSetup")); // This is the setup servlet that creates users directly in the DB
    }

    @Override
    @Test
    public void testOAuthCustomStoreCodeFlow() throws Exception {
        super.testOAuthCustomStoreCodeFlow();

        String msg = checkMongoEntry("http://" + server.getHostname() + ":" + server.getHttpDefaultPort(), server.getHttpDefaultPort(), "dclient01");
        assertNotNull("Servlet should have returned a secret type", msg);
        assertEquals("Secret type is incorrect in the database.", "hash", msg);

        msg = checkMongoIteration("http://" + server.getHostname() + ":" + server.getHttpDefaultPort(), server.getHttpDefaultPort(), "dclient01");
        assertNotNull("Servlet should have returned an iteration type for " + clientID, msg);
        assertEquals("Iteration is incorrect in the database for client " + clientID, "2048", msg);

        msg = checkMongoAlgorithm("http://" + server.getHostname() + ":" + server.getHttpDefaultPort(), server.getHttpDefaultPort(), "dclient01");
        assertNotNull("Servlet should have returned an algorithm type for " + clientID, msg);
        assertEquals("Algorithm is incorrect in the database for client " + clientID, "PBKDF2WithHmacSHA512", msg);

        super.testOAuthCommonCodeFlow();
    }

}

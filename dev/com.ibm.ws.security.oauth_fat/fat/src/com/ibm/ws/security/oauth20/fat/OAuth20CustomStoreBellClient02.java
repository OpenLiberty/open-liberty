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

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class OAuth20CustomStoreBellClient02 extends OAuth20Client02Common {

    private static final Class<?> thisClass = OAuth20CustomStoreBellClient02.class;

    /**
     * TestDescription:
     *
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "resource owner password credentials" . In this scenario, the OAuth
     * client is registered using database, instead of using XML file. Derby
     * database is used for storing the registered clients. In this scenario,
     * the autoauthz parameter is set to true, so the resource owner does not
     * receive the consent form from the authorization server. The test verifies
     * that the Oauth code flow, using the authorization grant type of "resource
     * owner password credentials" works correctly with JDBC database client.
     *
     */

    @Override
    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup(MONGO_STORE_BELL_SERVER2);
        assertNotNull("The application oAuth20MongoSetup failed to start",
                      server.waitForStringInLog("CWWKZ0001I.*oAuth20MongoSetup")); // This is the setup servlet that creates users directly in the DB
    }

    @Override
    @Test
    public void testOAuthCustomStoreResourceOwnerFlow() throws Exception {
        super.testOAuthCustomStoreResourceOwnerFlow();
    }

}

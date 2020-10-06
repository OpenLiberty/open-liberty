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
package com.ibm.ws.security.mp.jwt12.fat.sharedTests;

import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwt12FatConstants;
import com.ibm.ws.security.mp.jwt12.fat.utils.MP12ConfigSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will verify that we get the correct behavior when we
 * have mp-config defined as environment variables.
 * The classes that extend this class will set the environment variables in a variety of
 * valid ways.
 */

/**
 * This class varies from MPJwtGoodMPConfigAsEnvVars because we can't use a different sigAlg with MpJwt 1.1 without specifying the signature algorithm value - it'll default to
 * RS256 and there is no way to specify the sigAlg with an mp.config.properties attr.
 * So, the test with NO mpJwt config will fail with an sigAlg mismatch
 */
@RunWith(FATRunner.class)
public class GenericEnvVarsAndSystemPropertiesTests extends MPJwt12MPConfigTests {

    public static LibertyServer resourceServer;

    protected static String headerValue = MpJwt12FatConstants.AUTHORIZATION;
    protected static String cookieName = MpJwt12FatConstants.TOKEN_TYPE_BEARER;
    protected static String sigAlgorithm = MpJwt12FatConstants.SIGALG_RS256;
    // don't need an audience instance as we don't need that info when we build the app request

    public static void commonMpJwt12Setup(LibertyServer requestedServer, String config, String header, String name, String audience, String algorithm,
                                          MPConfigLocation where) throws Exception {

        resourceServer = requestedServer;

        headerValue = header;
        cookieName = name;
        sigAlgorithm = algorithm;

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        MP12ConfigSettings mpConfigSettings = new MP12ConfigSettings(MP12ConfigSettings.PublicKeyLocationNotSet, MP12ConfigSettings.PublicKeyNotSet, MP12ConfigSettings.IssuerNotSet, MpJwt12FatConstants.X509_CERT, header, name, audience, algorithm);
        setUpAndStartRSServerForTests(resourceServer, config, mpConfigSettings, where);
        // don't restore servers between test cases
        skipRestoreServerTracker.addServer(resourceServer);

    }

    /**
     * The server will be started with all mp-config properties correctly set in environment variables.
     * The server.xml has a valid mp_jwt config specified.
     * The config settings should come from server.xml.
     * The test should run successfully .
     *
     * @throws Exception
     */
    public void genericGoodTest() throws Exception {

        resourceServer.restoreServerConfigurationAndWaitForApps();
        // the builder we'll use has the same name as the signature algorithm
        standard12TestFlow(sigAlgorithm, resourceServer, MpJwt12FatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT,
                         MpJwt12FatConstants.NO_MP_CONFIG_IN_APP_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP, headerValue,
                         cookieName);

    }

    public void genericBadTest(String config, Expectations expectations) throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, config);
        // the builder we'll use has the same name as the signature algorithm
        standard12TestFlow(sigAlgorithm, resourceServer, MpJwt12FatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT,
                         MpJwt12FatConstants.NO_MP_CONFIG_IN_APP_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP, headerValue,
                         cookieName, expectations);

    }

}

/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.mp.jwt21.fat.sharedTests;

import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt12FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt21FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.sharedTests.MPJwt21MPConfigTests;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MP12ConfigSettings;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MP21ConfigSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will verify that we get the correct behavior when we
 * have mp-config defined as environment variables or system properties.
 * The classes that extend this class will set the environment variables/system properties in a variety of
 * valid ways.
 */

@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class GenericEnvVarsAndSystemPropertiesTests extends MPJwt21MPConfigTests {

    public static LibertyServer resourceServer;

    protected static int tokenAge = 0;
    protected static int clockSkew = 300;
    protected static String keyManagementKeyAlias = "";

    public static void commonMpJwt21Setup(LibertyServer requestedServer, String config, int inTokenAge, int inClockSkew, String inKeyMgmtKeyAlias, MPConfigLocation where) throws Exception {

        resourceServer = requestedServer;

        tokenAge = inTokenAge;
        clockSkew = inClockSkew;
        keyManagementKeyAlias = inKeyMgmtKeyAlias;

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml", false);

        MP21ConfigSettings mpConfigSettings = new MP21ConfigSettings(MP21ConfigSettings.PublicKeyLocationNotSet, MP21ConfigSettings.PublicKeyNotSet, MP21ConfigSettings.IssuerNotSet, MPJwt12FatConstants.X509_CERT, MP12ConfigSettings.DefaultHeader, MP12ConfigSettings.DefaultCookieName, MP12ConfigSettings.AudiencesNotSet, MP12ConfigSettings.DefaultAlgorithm, MP12ConfigSettings.DecryptKeyLocNotSet, tokenAge, clockSkew, keyManagementKeyAlias);
        setUpAndStartRSServerForTests(resourceServer, config, mpConfigSettings, where);
        // don't restore servers between test cases
        skipRestoreServerTracker.addServer(resourceServer);

    }

    /**
     * The server will be started with all mp-config properties correctly set in environment variables or system properties.
     * The server.xml omits the tested mp_jwt config specified.
     * The test should behave as expected for the value specified as either env or system properties.
     * ie: if this is a test of tokenAge, we should timeout because we picked up the short tokenAge value
     *
     * @throws Exception
     */
    public void genericUsePropTest(int sleepTime, Expectations expectations) throws Exception {
        genericUsePropTest(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, sleepTime, expectations);
    }

    // shouldn't need to pass the builder
    public void genericUsePropTest(String builderId, int sleepTime, Expectations expectations) throws Exception {

        resourceServer.restoreServerConfigurationAndWaitForApps();

        standard21TestFlow(builderId, resourceServer, MPJwt21FatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT, MPJwt21FatConstants.NO_MP_CONFIG_IN_APP_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP, sleepTime, expectations);

    }

    public void genericServerXmlOverridePropTest(String config, int sleepTime) throws Exception {
        genericServerXmlOverridePropTest(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, config, sleepTime, null);
    }

    public void genericServerXmlOverridePropTest(String config, int sleepTime, Expectations expectations) throws Exception {
        genericServerXmlOverridePropTest(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, config, sleepTime, expectations);
    }

    public void genericServerXmlOverridePropTest(String builderId, String config, int sleepTime, Expectations expectations) throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, config);
        standard21TestFlow(builderId, resourceServer, MPJwt21FatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT, MPJwt21FatConstants.NO_MP_CONFIG_IN_APP_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP, sleepTime, expectations);

    }

}

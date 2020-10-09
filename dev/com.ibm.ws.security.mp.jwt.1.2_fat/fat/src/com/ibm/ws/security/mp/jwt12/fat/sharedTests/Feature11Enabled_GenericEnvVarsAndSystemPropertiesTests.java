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

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.jwt.fat.mpjwt.MpJwt12FatConstants;
import com.ibm.ws.security.mp.jwt12.fat.utils.MP12ConfigSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will verify that we get the correct behavior when we
 * have mp-config defined as environment variables or system properties
 * We'll test with a server.xml that will NOT have a mpJwt config, the app will NOT have mp-config specified
 * Therefore, we'll be able to show that the config is coming from the environment variables or system properties
 * We will reconfigure the server to add config attributes to allow specific tests to get past failures tested by
 * other tests in this class.
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class Feature11Enabled_GenericEnvVarsAndSystemPropertiesTests extends MPJwt12MPConfigTests {

    public static Class<?> thisClass = Feature11Enabled_GenericEnvVarsAndSystemPropertiesTests.class;

    // extending class will define the server (they use different server instances (with/without jvm.options))
    public static LibertyServer resourceServer;

    public static void commonMpJwt12Setup(LibertyServer requestedServer, MPConfigLocation where) throws Exception {

        resourceServer = requestedServer;

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        MP12ConfigSettings mpConfigSettings = new MP12ConfigSettings(MP12ConfigSettings.PublicKeyLocationNotSet, MP12ConfigSettings.PublicKeyNotSet, MP12ConfigSettings.IssuerNotSet, MpJwt12FatConstants.X509_CERT, MpJwt12FatConstants.COOKIE, "myJwtCookie", "client01, client02", MpJwt12FatConstants.SIGALG_ES256);
        setUpAndStartRSServerForTests(resourceServer, "rs_server_AltConfigNotInApp_11ServerXmlConfig.xml", mpConfigSettings, where);

    }

    /**
     * The mp config properties set as env vars/system properties indicate that the token should be passed as a cookie with name "myJwtCookie"
     * Pass the token as a cookie with the name "myJwtCookie".
     * Show that with the mpJwt-1.1 feature, the runtime can't find the token (it only looks in the header)
     *
     * @throws Exception
     */
    @Test
    public void Feature11Enabled_GenericEnvVarsAndSystemPropertiesTests_HeaderCookie_test() throws Exception {

        standard12TestFlow(MpJwt12FatConstants.SIGALG_ES256, resourceServer, MpJwt12FatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT,
                         MpJwt12FatConstants.NO_MP_CONFIG_IN_APP_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP, MpJwt12FatConstants.COOKIE,
                         "myJwtCookie", setMissingTokenExpectations(resourceServer));
    }

    /**
     * The mp config properties set as env vars/system properties indicate that the token should be passed as a cookie with name "myJwtCookie"
     * Pass the token as Bearer in the Authorization header - this is how mpJwt-1.1 expects it.
     * The server does not have an audience configured - the mp config properties set as env vars/system properties has the audience set, but
     * since mpJwt-1.1 should not know about that, we should expect a failure stating that the audience is NOT valid.
     *
     * @throws Exception
     */
    @Test
    public void Feature11Enabled_GenericEnvVarsAndSystemPropertiesTests_Audience_test() throws Exception {

        standard12TestFlow(MpJwt12FatConstants.SIGALG_ES256, resourceServer, MpJwt12FatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT,
                         MpJwt12FatConstants.NO_MP_CONFIG_IN_APP_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP, MpJwt12FatConstants.AUTHORIZATION,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER, setBadAudiencesExpectations(resourceServer));
    }

    /**
     * The mp config properties set as env vars/system properties indicate that the signature algorithm should be ES256.
     * Create a token using ES256 and set the key to an ES256 cert. The algorithm mp config property will be ignored, so,
     * the runtime will use the default value of RS256.
     * Expect the request to fail as the algorithm and cert won't match.
     * (We reconfigure the server to add the correct audiences to the config (without that, we'd fail before the algorithm check)
     *
     * @throws Exception
     */
    @Test
    public void Feature11Enabled_GenericEnvVarsAndSystemPropertiesTests_Algorithm_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigNotInApp_11ServerXmlConfig_withAudiences.xml");
        standard12TestFlow(MpJwt12FatConstants.SIGALG_ES256, resourceServer, MpJwt12FatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT,
                         MpJwt12FatConstants.NO_MP_CONFIG_IN_APP_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP, MpJwt12FatConstants.AUTHORIZATION,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER, setBadCertExpectations(resourceServer, KeyMismatch));
    }

    /**
     * All mpJwt-1.2 mp config properties set as env vars/system properties are set, but should be ignored. This test
     * overrides all of those values in server.xml just to show that with those env vars/system properties set, we'll
     * have access with the server.xml config attrs set.
     *
     * @throws Exception
     */
    @Test
    public void Feature11Enabled_GenericEnvVarsAndSystemPropertiesTests_overridAll_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigNotInApp_11ServerXmlConfig_withAudiencesAndSigAlg.xml");
        standard12TestFlow(MpJwt12FatConstants.SIGALG_ES256, resourceServer, MpJwt12FatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT,
                         MpJwt12FatConstants.NO_MP_CONFIG_IN_APP_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP, MpJwt12FatConstants.AUTHORIZATION,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER, null);
    }

}

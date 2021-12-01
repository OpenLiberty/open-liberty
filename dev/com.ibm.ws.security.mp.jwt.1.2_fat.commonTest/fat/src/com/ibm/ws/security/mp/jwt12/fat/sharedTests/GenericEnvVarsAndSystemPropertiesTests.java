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

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt12FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.sharedTests.MPJwt12MPConfigTests;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MP12ConfigSettings;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

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
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class GenericEnvVarsAndSystemPropertiesTests extends MPJwt12MPConfigTests {

    public static LibertyServer resourceServer;

    protected static String headerValue = MPJwt12FatConstants.AUTHORIZATION;
    protected static String cookieName = MPJwt12FatConstants.TOKEN_TYPE_BEARER;
    protected static String sigAlgorithm = MPJwt12FatConstants.SIGALG_RS256;
    // don't need an audience instance as we don't need that info when we build the app request

    public static void commonMpJwt12Setup(LibertyServer requestedServer, String config, String header, String name, String audience, String algorithm, String decryptKeyLoc,
                                          MPConfigLocation where) throws Exception {

        resourceServer = requestedServer;

        headerValue = header;
        cookieName = name;
        sigAlgorithm = algorithm;

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml", false);

        if (decryptKeyLoc != null && decryptKeyLoc.startsWith("builderId")) {
            String builderId = decryptKeyLoc.split(":")[1];
            decryptKeyLoc = SecurityFatHttpUtils.getServerUrlBase(jwtBuilderServer) + "jwt/ibm/api/" + builderId + "/jwk";
        }
        MP12ConfigSettings mpConfigSettings = new MP12ConfigSettings(MP12ConfigSettings.PublicKeyLocationNotSet, MP12ConfigSettings.PublicKeyNotSet, MP12ConfigSettings.IssuerNotSet, MPJwt12FatConstants.X509_CERT, header, name, audience, algorithm, decryptKeyLoc);
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
        genericGoodTest(sigAlgorithm);
    }

    public void genericGoodTest(String builderId) throws Exception {

        resourceServer.restoreServerConfigurationAndWaitForApps();
        // the builder we'll use has the same name as the signature algorithm
        standard12TestFlow(builderId, resourceServer, MPJwt12FatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT,
                           MPJwt12FatConstants.NO_MP_CONFIG_IN_APP_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP, headerValue,
                           cookieName);

    }

    public void genericBadTest(String config, Expectations expectations) throws Exception {
        genericBadTest(sigAlgorithm, config, expectations);
    }

    public void genericBadTest(String builderId, String config, Expectations expectations) throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, config);
        // the builder we'll use has the same name as the signature algorithm
        standard12TestFlow(builderId, resourceServer, MPJwt12FatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT,
                           MPJwt12FatConstants.NO_MP_CONFIG_IN_APP_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP, headerValue,
                           cookieName, expectations);

    }

    /**
     * generic test that builds token with rs256 for both signature and algorithm, but, caller specifies the value for the
     * key management key alg and/or content encryption alg.
     * Caller will set up mp config properties expecting rs256 encryption and signing in either env vars or system properties
     * We can't specify values other than the defaults for these 2 attributes in the builder config, so, we need to build our own token
     *
     * @param keyMgmtAlg - requested key management key alg to use when building the token
     * @param contentEncryptAlg - requested content encryption algorithm to use when building the token
     * @throws Exception
     */
    public void genericDecryptOtherKeyMgmtAlgOrOtherContentEncryptAlg(String keyMgmtAlg, String contentEncryptAlg) throws Exception {

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair(JwtConstants.PARAM_UPN, defaultUser));
        // add more args
        String encryptKey = JwtKeyTools.getComplexPublicKeyForSigAlg(jwtBuilderServer, MPJwt12FatConstants.SIGALG_RS256);

        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_KEY_MGMT_ALG, keyMgmtAlg));
        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_ENCRYPT_KEY, encryptKey));
        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_CONTENT_ENCRYPT_ALG, contentEncryptAlg));
        String token = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "sign_RS256_enc_RS256", extraClaims);

        useToken(token,
                 buildAppUrl(resourceServer, MPJwt12FatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT,
                             MPJwt12FatConstants.NO_MP_CONFIG_IN_APP_APP),
                 MPJwt12FatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP, MPJwt12FatConstants.AUTHORIZATION,
                 MPJwt12FatConstants.TOKEN_TYPE_BEARER, null);
    }
}

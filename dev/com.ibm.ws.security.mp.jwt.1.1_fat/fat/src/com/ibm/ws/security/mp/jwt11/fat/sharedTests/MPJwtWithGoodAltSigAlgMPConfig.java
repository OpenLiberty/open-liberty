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
package com.ibm.ws.security.mp.jwt11.fat.sharedTests;

import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.servers.ServerInstanceUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.utils.MP11ConfigSettings;

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
public class MPJwtWithGoodAltSigAlgMPConfig extends MPJwt11MPConfigTests {

    public static LibertyServer resourceServer;

    private static String sigAlg = null;
    protected static final String JwksUriFlag = "jwksuri";

    public static void commonSetup(LibertyServer requestedServer, String requestedSigAlg, String location, String key, MPConfigLocation where) throws Exception {

        resourceServer = requestedServer;

        sigAlg = requestedSigAlg;

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        // when building the jwksuri, we need the real port of the builder - caller just
        if (location != null && location.equals(JwksUriFlag)) {
            location = resolvedJwksUri(jwtBuilderServer, MP11ConfigSettings.jwksUri).replace("defaultJWT", sigAlg);
        }

        MP11ConfigSettings mpConfigSettings = new MP11ConfigSettings(location, key, MP11ConfigSettings.IssuerNotSet, MpJwtFatConstants.X509_CERT);
        setUpAndStartRSServerForTests(resourceServer, "rs_server_AltConfigNotInApp_goodSigAlgServerXmlConfig.xml", mpConfigSettings, where);

        // set signatureAlgorithm attribute in server.xml
        ServerInstanceUtils.setOneVar(resourceServer, "sigAlg", requestedSigAlg);
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

        standard11TestFlow(sigAlg, resourceServer, MpJwtFatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT,
                         MpJwtFatConstants.NO_MP_CONFIG_IN_APP_APP, MpJwtFatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP);

    }

}

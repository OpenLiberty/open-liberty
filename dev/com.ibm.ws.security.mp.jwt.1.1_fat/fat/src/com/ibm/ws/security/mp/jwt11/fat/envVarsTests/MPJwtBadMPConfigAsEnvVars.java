/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt11.fat.envVarsTests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.sharedTests.MPJwt11MPConfigTests;
import com.ibm.ws.security.mp.jwt11.fat.utils.MP11ConfigSettings;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will verify that we get the correct behavior when we
 * have a bad mp-config defined as environment variables.
 * We'll test with a server.xml that does not have an mp_jwt config and one that
 * has a valid mp_jwt config.
 * The app will NOT have mp-config specified.
 * Therefore, we'll be able to show that the config is coming from the environment variables when there is no
 * server config, and the server config when it exists.
 *
 **/

@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class MPJwtBadMPConfigAsEnvVars extends MPJwt11MPConfigTests {

    public static Class<?> thisClass = MPJwtBadMPConfigAsEnvVars.class;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat")
    public static LibertyServer resourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        MP11ConfigSettings mpConfigSettings = new MP11ConfigSettings("badKeyLocation", "badPublicKey", "badIssuer", MpJwtFatConstants.X509_CERT);
        setUpAndStartRSServerForTests(resourceServer, "rs_server_AltConfigNotInApp_noServerXmlConfig.xml", mpConfigSettings, MPConfigLocation.ENV_VAR);

    }

    /**
     * The server will be started with all mp-config properties set to bad values in environment variables.
     * The server.xml has a valid mp_jwt config specified.
     * The config settings should come from server.xml.
     * The test should run successfully .
     *
     * @throws Exception
     */
    @Test
    public void MPJwtBadMPConfigAsEnvVars_GoodMpJwtConfigSpecifiedInServerXml() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigNotInApp_goodServerXmlConfig.xml");
        standard11TestFlow(resourceServer, MpJwtFatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT,
                         MpJwtFatConstants.NO_MP_CONFIG_IN_APP_APP, MpJwtFatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP);

    }

    /**
     * The server will be started with all mp-config properties set to bad values in environment variables.
     * The server.xml has NO mp_jwt config specified.
     * The config settings should come from the env vars.
     * The test should fail
     *
     * @throws Exception
     */
    @Test
    public void MPJwtBadMPConfigAsEnvVars_MpJwtConfigNotSpecifiedInServerXml() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT,
                         MpJwtFatConstants.NO_MP_CONFIG_IN_APP_APP, MpJwtFatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP,
                         setBadIssuerExpectations(resourceServer));

    }

}

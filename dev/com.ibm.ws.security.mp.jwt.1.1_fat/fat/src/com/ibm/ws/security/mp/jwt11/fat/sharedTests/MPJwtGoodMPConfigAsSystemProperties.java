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
package com.ibm.ws.security.mp.jwt11.fat.sharedTests;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will verify that we get the correct behavior when we
 * have mp-config defined as system properties.
 * The classes that extend this class will set the system properties in a variety of
 * valid ways.
 */

@RunWith(FATRunner.class)
public class MPJwtGoodMPConfigAsSystemProperties extends MPJwt11MPConfigTests {

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat.jvmOptions")
    public static LibertyServer resourceServer;

    /**
     * The server will be started with all mp-config properties correctly configured in the jvm.options file.
     * The server.xml has a valid mp_jwt config specified.
     * The config settings should come from server.xml.
     * The test should run successfully .
     *
     * @throws Exception
     */
    @Test
    public void MPJwtGoodMPConfigAsSystemProperties_GoodMpJwtConfigSpecifiedInServerXml() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigNotInApp_goodServerXmlConfig.xml");
        standard11TestFlow(resourceServer, MpJwtFatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT,
                         MpJwtFatConstants.NO_MP_CONFIG_IN_APP_APP, MpJwtFatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP);

    }

    /**
     * The server will be started with all mp-config properties correctly configured in the jvm.options file.
     * The server.xml has NO mp_jwt config specified.
     * The config settings should come from the system properties (defined in jvm.options).
     * The test should run successfully.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtGoodMPConfigAsSystemProperties_MpJwtConfigNotSpecifiedInServerXml() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT,
                         MpJwtFatConstants.NO_MP_CONFIG_IN_APP_APP, MpJwtFatConstants.MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP);

    }

}

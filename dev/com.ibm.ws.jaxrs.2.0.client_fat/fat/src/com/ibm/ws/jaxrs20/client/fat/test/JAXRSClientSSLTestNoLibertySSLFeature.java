/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.client.fat.test;


import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class JAXRSClientSSLTestNoLibertySSLFeature extends JAXRSClientSSLTestNoLibertySSLCfg {

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname,
                                                       "com.ibm.ws.jaxrs20.client.JAXRSClientSSL.client",
                                                       "com.ibm.ws.jaxrs20.client.JAXRSClientSSL.service");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            serverNoSSL.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        
        List<String> features = new ArrayList<>(serverNoSSL.getServerConfiguration().getFeatureManager().getFeatures());
        features.add("ssl-1.0");
        serverNoSSL.changeFeatures(features);

        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        // Pause for the smarter planet message
        assertNotNull("The smarter planet message did not get printed on serverNoSSL",
                      serverNoSSL.waitForStringInLog("CWWKF0011I"));
        
        // Pause for the smarter planet message
        assertNotNull("The smarter planet message did not get printed on server",
                      server.waitForStringInLog("CWWKF0011I"));

        // wait for LTPA key to be available to avoid CWWKS4000E
        assertNotNull("CWWKS4105I.* not received on server",
                      server.waitForStringInLog("CWWKS4105I.*"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        JAXRSClientSSLTestNoLibertySSLCfg.tearDown();
    }
}

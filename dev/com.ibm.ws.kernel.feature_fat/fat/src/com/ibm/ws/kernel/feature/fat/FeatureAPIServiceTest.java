package com.ibm.ws.kernel.feature.fat;

/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class FeatureAPIServiceTest {
    static final Class<?> c = FeatureAPIServiceTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.api.service");

    @Test
    public void testAddServiceApi() throws Exception {
        server.startServer();

        assertNotNull("The application did not appear to have been installed.", server.waitForStringInLog("CWWKZ0001I.* test.service.consumer"));
        assertNotNull("Failed to grant access to the ApiService.", server.waitForStringInLog("ApiService - SUCCESS", 5000));
        assertNotNull("Failed to deny access to the NotApiService.", server.waitForStringInLog("NotApiService - SUCCESS", 5000));
    }

    @BeforeClass
    public static void installUserFeature() throws Exception {
        server.installUserFeature("test.service.provider-1.0");
        server.installUserBundle("test.service.provider_1.0.0");
        try {
            RemoteFile featureFile = server.getFileFromLibertyInstallRoot("/lib/features/test.service.provider-1.0.mf");
            if (featureFile.exists()) {
                Log.info(c, "installUserFeature", "Found unexpected system feature: " + featureFile.toString());
                server.uninstallSystemFeature("test.service.provider-1.0");
            }
        } catch (FileNotFoundException e) {
            // ignore; expected
        }
    }

    @AfterClass
    public static void uninstallUserFeature() throws Exception {
        try {
            RemoteFile featureFile = server.getFileFromLibertyInstallRoot("/lib/features/test.service.provider-1.0.mf");
            if (featureFile.exists()) {
                Log.info(c, "uninstallUserFeature", "Found unexpected system feature: " + featureFile.toString());
            }
        } catch (FileNotFoundException e) {
            // ignore; expected
        }
        server.uninstallUserFeature("test.service.provider-1.0");
        server.uninstallUserBundle("test.service.provider_1.0.0");
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}
/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testing.opentracing.test;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 */
@RunWith(FATRunner.class)
public class OpentracingRestClientTCKLauncher {

    private static final String FEATURE_NAME = "com.ibm.ws.opentracing.mock-1.3.mf";
    private static final String BUNDLE_NAME = "com.ibm.ws.opentracing.mock-1.3.jar";

    @Server("OpentracingRestClientTCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        setUpServer();
        server.startServer();
    }
    
    private static void setUpServer() throws Exception {
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/" + FEATURE_NAME);
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/" + BUNDLE_NAME);
    }


    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchOpentracingRestClientTck() throws Exception {
        MvnUtils.runTCKMvnCmd(server, "com.ibm.ws.opentracing.1.3_fat", this.getClass() + ":launchOpentracingRestClientTck", "rest-client-tck-suite.xml", Collections.emptyMap(), Collections.emptySet());
    }
}

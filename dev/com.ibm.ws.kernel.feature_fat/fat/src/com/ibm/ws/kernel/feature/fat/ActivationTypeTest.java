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
package com.ibm.ws.kernel.feature.fat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class ActivationTypeTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.activation.type");

    @Test
    public void testActivationType() throws Exception {

        server.startServer();
        server.waitForStringInLogUsingMark("test.activation.type.parallel.system: true");

        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("test.activation.sequential.system-1.0"));
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        server.waitForStringInLogUsingMark("test.activation.type.sequential.system: false");

        // Note that usr/product features are never parallel activated
        server.setMarkToEndOfLog();
        // Our new server.xml is the same size as the old one, so wait a couple seconds to make
        // sure the config runtime will recognize the change.
        Thread.sleep(2000);
        server.changeFeatures(Arrays.asList("usr:test.activation.parallel.user-1.0"));
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        server.waitForStringInLogUsingMark("test.activation.type.parallel.user: false");

        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("usr:test.activation.sequential.user-1.0"));
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        server.waitForStringInLogUsingMark("test.activation.type.sequential.user: false");
    }

    @BeforeClass
    public static void installFeatures() throws Exception {
        server.installSystemFeature("test.activation.parallel.system-1.0");
        server.installSystemBundle("test.activation.type.parallel.system");
        server.installSystemFeature("test.activation.sequential.system-1.0");
        server.installSystemBundle("test.activation.type.sequential.system");

        server.installUserFeature("test.activation.parallel.user-1.0");
        server.installUserBundle("test.activation.type.parallel.user");
        server.installUserFeature("test.activation.sequential.user-1.0");
        server.installUserBundle("test.activation.type.sequential.user");
    }

    @AfterClass
    public static void uninstallFeatures() throws Exception {
        server.uninstallSystemFeature("test.activation.parallel.system-1.0");
        server.uninstallSystemBundle("test.activation.type.parallel.system");
        server.uninstallSystemFeature("test.activation.sequential.system-1.0");
        server.uninstallSystemBundle("test.activation.type.sequential.system");

        server.uninstallUserFeature("test.activation.parallel.user-1.0");
        server.uninstallUserBundle("test.activation.type.parallel.user");
        server.uninstallUserFeature("test.activation.sequential.user-1.0");
        server.uninstallUserBundle("test.activation.type.sequential.user");
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class DelayedVariableTests extends ServletRunner {

    private static final String CONTEXT_ROOT = "varmergedconfig";

    @Override
    protected String getContextRoot() {
        return CONTEXT_ROOT;
    }

    @Override
    protected String getServletMapping() {
        return "delayedVarTests";
    }

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.delayedVar");

    @Test
    public void testVariableDelay() throws Exception {
        test(server);
    }

    @BeforeClass
    public static void setUpForMergedConfigTests() throws Exception {
        //copy the config feature into the server features location
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/delayedVariable-1.0.mf");

        //copy the bundle into the server lib location
        server.copyFileToLibertyInstallRoot("lib", "bundles/test.config.variables_1.0.0.jar");

        server.startServer("delayedVariables.log");
        //make sure the URL is available
        assertNotNull(server.waitForStringInLog("CWWKT0016I.*" + CONTEXT_ROOT));
        assertNotNull(server.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer();
        server.deleteFileFromLibertyInstallRoot("lib/features/configfatlibertyinternals-1.0.mf");
        server.deleteFileFromLibertyInstallRoot("lib/test.config.variables_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot("lib/features/delayedVariable-1.0.mf");
    }

}

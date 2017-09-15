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
package com.ibm.ws.microprofile.health.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.LITE)
public class HealthTest extends LoggingTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("CDIHealth");

    @Test
    public void test() throws Exception {
        if (!SHARED_SERVER.getLibertyServer().isStarted())
            SHARED_SERVER.getLibertyServer().startServer();

        assertNotNull("Kernel did not start", SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKE0002I"));
        assertNotNull("Server did not start", SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKF0011I"));

        assertNotNull("FeatureManager should report update is complete",
                      SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKF0008I"));
        SHARED_SERVER.getLibertyServer().stopServer();

    }

    /** {@inheritDoc} */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }
}
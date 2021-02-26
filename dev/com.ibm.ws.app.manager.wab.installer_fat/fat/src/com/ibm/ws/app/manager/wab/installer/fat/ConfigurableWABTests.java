/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.wab.installer.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

/**
 *
 */
@RunWith(FATRunner.class)
public class ConfigurableWABTests extends AbstractWABTests {

    private static final String WAB1 = "/wab1";
    private static final String WAB2 = "/wab2";
    private static final String SERVLET1 = "/servlet1";
    private static final String SERVLET2 = "/servlet2";
    private static final String SERVLET3 = "/servlet3";
    private static final String RESTART = "/restart";
    private static final String SWITCH = "/switch";
    private static final String SWITCH_TARGET = "/switchTarget";
    private static final String CONFLICT = "/conflict";
    private static final String OUTPUT_SERVLET1 = "service: test.wab1.Servlet1";
    private static final String OUTPUT_SERVLET2 = "service: test.wab2.Servlet2";
    private static final String OUTPUT_SERVLET3 = "service: test.wab3.Servlet3";
    private static final String OUTPUT_RESTART = "SUCCESS service: test.wab1.Restart";
    private static final String OUTPUT_SWITCH = "SUCCESS service: test.wab1.Switch";

    private static final String CONFIG_MULTIPLE = CONFIGS + "testMultple.xml";
    private static final String CONFIG_CONFLICT = CONFIGS + "testConflict.xml";

    @BeforeClass
    public static void startServer() throws Exception {
        server.startServer(ConfigurableWABTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null && server.isStarted()) {
            // we expect a conflict error from the conflict test
            server.stopServer("CWWKZ0208E");
        }
    }

    @Test
    public void testDefaultConfiguration() throws Exception {
        setConfiguration(CONFIG_DEFAULT);
        checkWAB(PRODUCT1 + SERVLET1, OUTPUT_SERVLET1);
        checkWAB(PRODUCT2 + SERVLET1, OUTPUT_SERVLET1);
    }

    @Test
    public void testMultipleConfiguration() throws Exception {
        setConfiguration(CONFIG_MULTIPLE);
        checkWAB(PRODUCT1 + WAB1 + SERVLET1, OUTPUT_SERVLET1);
        checkWAB(PRODUCT2 + WAB1 + SERVLET1, OUTPUT_SERVLET1);
        checkWAB(PRODUCT1 + WAB2 + SERVLET2, OUTPUT_SERVLET2);
        checkWAB(PRODUCT2 + WAB2 + SERVLET2, OUTPUT_SERVLET2);
    }

    @Test
    public void testConflictConfiguration() throws Exception {
        setConfiguration(CONFIG_CONFLICT);
        // the WAB that won should still be serviceable
        checkWAB(CONFLICT + SERVLET1, OUTPUT_SERVLET1);
    }

    @Test
    public void testNoConfiguredPathRestart() throws Exception {
        setConfiguration(CONFIG_DEFAULT);
        checkWAB(PRODUCT1 + RESTART, OUTPUT_RESTART);
    }

    @Test
    public void testSwitch() throws Exception {
        setConfiguration(CONFIG_DEFAULT);
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB2", OUTPUT_SWITCH);
        checkWAB(SWITCH_TARGET + SERVLET2, OUTPUT_SERVLET2);
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB3", OUTPUT_SWITCH);
        checkWAB(SWITCH_TARGET + SERVLET3, OUTPUT_SERVLET3);
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB2", OUTPUT_SWITCH);
        checkWAB(SWITCH_TARGET + SERVLET2, OUTPUT_SERVLET2);
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB3", OUTPUT_SWITCH);
    }

}

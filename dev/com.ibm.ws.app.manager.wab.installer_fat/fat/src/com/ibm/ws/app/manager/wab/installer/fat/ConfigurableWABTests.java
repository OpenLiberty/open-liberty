/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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
package com.ibm.ws.app.manager.wab.installer.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;

/**
 *
 */
@RunWith(FATRunner.class)
public class ConfigurableWABTests extends AbstractWABTests {

    private static final Class<?> c = ConfigurableWABTests.class;
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

    private static final String[] MESSAGES_MULTIPLE = new String[] { APP_AVAIL + "/product1/wab1/",
                                                                     APP_AVAIL + "/product1/wab2/",
                                                                     APP_AVAIL + "/product2/wab1/",
                                                                     APP_AVAIL + "/product2/wab2/"
    };

    //private static final String APP_AVAIL_CFG = "";
    private static final String CONFIG_MULTIPLE = CONFIGS + "testMultple.xml";
    private static final String CONFIG_CONFLICT = CONFIGS + "testConflict.xml";
    private static final String CONFIG_RESET = CONFIGS + "testReset.xml";;

    @BeforeClass
    public static void startServer() throws Exception {
        server.setServerConfigurationFile(CONFIG_RESET);
        server.startServer(ConfigurableWABTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null && server.isStarted()) {
            // ignore conflict error from the conflict test
            // ignore warning about virtual host not found
            server.stopServer("CWWKZ0208E", "SRVE9956W");
        }
    }

    @After
    public void clear() throws Exception {
        Log.info(c, name.getMethodName(), "AfterTest: remove all products using " + CONFIG_RESET);
        setConfiguration(CONFIG_RESET);
        Log.info(c, name.getMethodName(), "AfterTest: finished setConfiguration to " + CONFIG_RESET);
    }

    @Test
    public void testDefaultConfiguration() throws Exception {
        setConfiguration(CONFIG_DEFAULT, MESSAGES_DEFAULT);
        checkWAB(PRODUCT1 + SERVLET1, OUTPUT_SERVLET1);
        checkWAB(PRODUCT2 + SERVLET1, OUTPUT_SERVLET1);
    }

    @Test
    public void testMultipleConfiguration() throws Exception {
        setConfiguration(CONFIG_MULTIPLE, MESSAGES_MULTIPLE);
        checkWAB(PRODUCT1 + WAB1 + SERVLET1, OUTPUT_SERVLET1);
        checkWAB(PRODUCT2 + WAB1 + SERVLET1, OUTPUT_SERVLET1);
        checkWAB(PRODUCT1 + WAB2 + SERVLET2, OUTPUT_SERVLET2);
        checkWAB(PRODUCT2 + WAB2 + SERVLET2, OUTPUT_SERVLET2);
    }

    @Test
    public void testConflictConfiguration() throws Exception {
        String[] expectedMsgs = { APP_AVAIL + CONFLICT,
                                  "CWWKZ0208E:.*with context root " + CONFLICT + " cannot be installed"
        };
        setConfiguration(CONFIG_CONFLICT, expectedMsgs);
        // the WAB that won should still be serviceable
        checkWAB(CONFLICT + SERVLET1, OUTPUT_SERVLET1);
    }

    @Test
    public void testNoConfiguredPathRestart() throws Exception {
        setConfiguration(CONFIG_DEFAULT, MESSAGES_DEFAULT);
        checkWAB(PRODUCT1 + RESTART, OUTPUT_RESTART);
    }

    @Test
    public void testSwitch() throws Exception {
        setConfiguration(CONFIG_DEFAULT, MESSAGES_DEFAULT);
        server.setMarkToEndOfLog();
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB2", OUTPUT_SWITCH);
        assertNotNull(server.waitForStringInLogUsingMark(APP_AVAIL + SWITCH_TARGET));
        checkWAB(SWITCH_TARGET + SERVLET2, OUTPUT_SERVLET2);

        server.setMarkToEndOfLog();
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB3", OUTPUT_SWITCH);
        assertNotNull(server.waitForStringInLogUsingMark(APP_AVAIL + SWITCH_TARGET));
        checkWAB(SWITCH_TARGET + SERVLET3, OUTPUT_SERVLET3);

        server.setMarkToEndOfLog();
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB2", OUTPUT_SWITCH);
        assertNotNull(server.waitForStringInLogUsingMark(APP_AVAIL + SWITCH_TARGET));
        checkWAB(SWITCH_TARGET + SERVLET2, OUTPUT_SERVLET2);

        server.setMarkToEndOfLog();
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB3", OUTPUT_SWITCH);
        assertNotNull(server.waitForStringInLogUsingMark(APP_AVAIL + SWITCH_TARGET));
        checkWAB(SWITCH_TARGET + SERVLET3, OUTPUT_SERVLET3);
    }
}

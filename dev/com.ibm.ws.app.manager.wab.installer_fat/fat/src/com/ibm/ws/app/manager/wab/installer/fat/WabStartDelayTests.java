/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.wab.installer.fat;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

/**
 *
 */
@RunWith(FATRunner.class)
public class WabStartDelayTests extends AbstractWABTests {

    private static final String SERVLET1 = "/servlet1";
    private static final String OUTPUT_SERVLET1 = "service: test.wab1.Servlet1";

    private static final String CONFIG_DELAY = CONFIGS + "testWabStartDelay.xml";

    @BeforeClass
    public static void startServer() throws Exception {
        setBootStrapProperties();
        server.setServerConfigurationFile(CONFIG_DELAY);
        server.startServer(WabStartDelayTests.class.getSimpleName() + ".log");
    }

    private static void setBootStrapProperties() throws Exception {
        File bootStrapPropertiesFile = new File(server.getFileFromLibertyServerRoot("bootstrap.properties").getAbsolutePath());
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(bootStrapPropertiesFile)) {
            props.load(in);
        }
        props.put("wab.test.delay", "10000");
        try (OutputStream out = new FileOutputStream(bootStrapPropertiesFile)) {
            props.store(out, "");
        }
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null && server.isStarted()) {
            // we expect no errors
            server.stopServer();
        }
    }

    @Test
    public void testDelay() throws Exception {
        assertNotNull("Didn't find Done Blocking message.", server.waitForStringInLogUsingLastOffset("Done Blocking:"));
        assertNotNull("No TCP message.", server.waitForStringInLogUsingLastOffset("CWWKO0219I"));
        assertNotNull("No server started message.", server.waitForStringInLogUsingLastOffset("CWWKF0011I"));
        checkWAB(PRODUCT1 + SERVLET1, OUTPUT_SERVLET1);
    }
}

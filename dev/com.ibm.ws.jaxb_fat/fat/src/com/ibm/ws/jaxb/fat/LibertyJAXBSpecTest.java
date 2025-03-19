/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.jaxb.fat;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class LibertyJAXBSpecTest {

    private static final String APP_NAME = "jaxbSpecApp";

    @Server("jaxbspec_fat")
    public static LibertyServer server;

    private static final int CONN_TIMEOUT = 5;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "jaxb.web", "jaxb.xmlnsform.unqualified", "jaxb.xmlnsform.qualified", "jaxb.xmlnsform.servlet");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @After
    public void afterTest() throws Exception {
        server.stopServer();
    }

    /*
     * Test com.sun.xml.bind.backupWithParentNamespace which is introduced to JAXB 2.3.9
     * Skipping EE9 and EE10 because they don't use this JAXB version.
     * This property doesn't work for them.
     */
    @Test
    @MinimumJavaLevel(javaLevel = 9)
    @SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES })
    public void testBackupWithParentNamespaceTrue_Servlet() throws Exception {
        Map<String, String> map = server.getJvmOptionsAsMap();
        map.put("-Dcom.sun.xml.bind.backupWithParentNamespace", "true");
        server.setJvmOptions(map);
        server.startServer();
        server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME);

        String testResultString = runTestServlet();
        if (testResultString != null) {
            assertTrue("When backupWithParentNamespace property is applied, an element with a prefix from a XmlNsForm.QUALIFIED package should not be null",
                       testResultString.contains("qpwp:value")); // qpwp: Qualified Person With Prefix
            assertTrue("When backupWithParentNamespace property is applied, an element without a prefix from a XmlNsForm.QUALIFIED package should not be null",
                       testResultString.contains("qpnp:value")); // qpnp: Qualified Person No Prefix
            assertTrue("When backupWithParentNamespace property is applied, an element with a prefix from a XmlNsForm.UNQUALIFIED package should be null",
                       testResultString.contains("upwp:null")); // upwp: Unqualified Person With Prefix
            assertTrue("When backupWithParentNamespace property is applied, an element without a prefix from a XmlNsForm.UNQUALIFIED package should not be null",
                       testResultString.contains("upnp:value")); // upnp: Unqualified Person No Prefix
        }
    }

    /*
     * Tests JAXB specs to see if ElementFormDefault setting is honored
     */
    @Test
    public void testBackupWithParentNamespaceFalse() throws Exception {
        Map<String, String> map = server.getJvmOptionsAsMap();
        map.clear();
        server.setJvmOptions(map);
        server.startServer();
        server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME);

        String testResultString = runTestServlet();
        if (testResultString != null) {
            assertTrue("Violation of JAXB spec, an element with a prefix from a XmlNsForm.QUALIFIED package should not be null",
                       testResultString.contains("qpwp:value")); // qpwp: Qualified Person With Prefix
            assertTrue("Violation of JAXB spec, an element without a prefix from a XmlNsForm.QUALIFIED package should be null",
                       testResultString.contains("qpnp:null")); // qpnp: Qualified Person No Prefix
            assertTrue("Violation of JAXB spec, an element with a prefix from a XmlNsForm.UNQUALIFIED package should be null",
                       testResultString.contains("upwp:null")); // upwp: Unqualified Person With Prefix
            assertTrue("Violation of JAXB spec, an element without a prefix from a XmlNsForm.UNQUALIFIED package should not be null",
                       testResultString.contains("upnp:value")); // upnp: Unqualified Person No Prefix
        }
    }

    /*
     * Runs the test servlet and return all results in a string with specific keys
     */
    private String runTestServlet() throws Exception {
        server.copyFileToLibertyServerRoot("Person.xml"); //Read the XML file to unmarshall

        String servletUrl = new StringBuilder("http://").append(server.getHostname())
                        .append(":")
                        .append(server.getHttpDefaultPort())
                        .append("/")
                        .append(APP_NAME)
                        .append("/JaxbSpecTest")
                        .toString(); // Build servlet url
        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(servletUrl), HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String result = br.readLine();
        if (result != null && result.contains("XML not found")) {
            assertTrue("Person.xml file is not found", false);
            result = null;
        }
        return result;
    }
}

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
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxws.fat.util.ExplodedShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This test is to verify the custom binding file could override Endpoint address and EJB based Web Services context root.
 */
@RunWith(FATRunner.class)
@SkipForRepeat("jaxws-2.3")
public class WsBndEndpointOverrideTest_Lite {
    private static final int CONN_TIMEOUT = 5;

    @Server("EJBinWarEndpointAddressOverrideServer")
    public static LibertyServer EJBinWarEndpointAddressOverrideServer;

    protected static String testFilesRootDir = "WsBndEndpointOverrideTest";

    @BeforeClass
    public static void setUp() throws Exception {
        JavaArchive bean = ShrinkHelper.buildJavaArchive("helloEJBServer", "com.ibm.ws.jaxws.test.hello.ejb.server");
        WebArchive app = ShrinkHelper.buildDefaultApp("helloServer", "com.ibm.ws.jaxws.test.wsr.server",
                                                      "com.ibm.ws.jaxws.test.wsr.server.impl");
        app.addAsLibrary(bean);
        ExplodedShrinkHelper.explodedArchiveToDestination(EJBinWarEndpointAddressOverrideServer, app, "apps");
    }

    @After
    public void tearDown() throws Exception {
        if (EJBinWarEndpointAddressOverrideServer.isStarted()) {
            EJBinWarEndpointAddressOverrideServer.stopServer();
        }
    }

    protected String getServletResponse(String servletUrl) throws Exception {
        URL url = new URL(servletUrl);
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String result = br.readLine();
        String line;
        while ((line = br.readLine()) != null) {
            result += line;
        }
        return result;
    }

    /**
     * TestDescription: override the Endpoint address of both EJB based Web Services and POJO Web Services in a Web application using binding file.
     * Condition:
     * - A helloEJBinWarServer.war
     * - Config the two endpoint addresses in binding file: helloEJBinWarServer.war/WEB-INF/ibm-ws-bnd.xml
     * Result:
     * - response contains the port welcome message "Hello! This is a CXF Web Service!"
     */
    @Test
    public void testEJBinWarEndpointAddressOverride() throws Exception {
        String testFilesDir = testFilesRootDir + "/testEJBinWarEndpointAddressOverride";

        EJBinWarEndpointAddressOverrideServer.copyFileToLibertyServerRoot("apps/helloServer.war/WEB-INF", testFilesDir + "/ibm-ws-bnd.xml");

        EJBinWarEndpointAddressOverrideServer.startServer();
        // Pause for application to start successfully
        EJBinWarEndpointAddressOverrideServer.waitForStringInLog("CWWKZ0001I.*helloServer");

        String result1 = getServletResponse(getBaseURL(EJBinWarEndpointAddressOverrideServer) + "/helloServer/hi");
        assertTrue("Can not access the target port, the return result is: " + result1, result1.contains("Hello! This is a CXF Web Service!"));

        String result2 = getServletResponse(getBaseURL(EJBinWarEndpointAddressOverrideServer) + "/helloServer/hiPeople");
        assertTrue("Can not access the target port, the return result is: " + result2, result2.contains("Hello! This is a CXF Web Service!"));

        EJBinWarEndpointAddressOverrideServer.stopServer(false);

        EJBinWarEndpointAddressOverrideServer.deleteFileFromLibertyServerRoot("server.xml");
    }

    protected String getBaseURL(LibertyServer server) {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
    }

}

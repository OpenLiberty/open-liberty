/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.client.feature;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 * Test addition and removal of features.
 */
public class DynamicFeatureTest {

    private static SharedOutputManager outputMgr;
    private static final String port = System.getProperty("HTTP_default", "8000");
    private static final String profile = System.getProperty("profile", "com.ibm.ws.kernel.feature.bvt");

    private static WsResource server_xml;
    private static WsResource server_add_xml;
    private static WsResource server_remove_xml;
    private static WsResource server_user_add_xml;
    private static WsResource server_product_add_xml;

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        WsLocationAdmin locSvc = (WsLocationAdmin) SharedLocationManager.createImageLocations(profile);
        server_xml = locSvc.resolveResource("server.xml");
        server_add_xml = locSvc.resolveResource("server-add.xml");
        server_remove_xml = locSvc.resolveResource("server-remove.xml");
        server_user_add_xml = locSvc.resolveResource("server-user-add.xml");
        server_product_add_xml = locSvc.resolveResource("server-product-add.xml");

    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is = null;
        String firstLine = null;
        try {
            is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            firstLine = br.readLine();
            System.out.println("Response message:\n\t" + firstLine);
            String line;
            while (null != (line = br.readLine())) {
                System.out.println("\t" + line);
            }
        } finally {
            tryToClose(is);
        }
        return firstLine;
    }

    /**
     * Test writing 1 single byte.
     */
    @Test
    public void testDynamicFeatures() {
        final String m = "testDynamicFeatures";
        HttpURLConnection conn = null;
        try {
            server_xml.put(server_add_xml.getChannel());

            URL url = new URL("http://localhost:" + port + "/feature/add");
            System.out.println("URL=" + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            String line = readResponse(conn);
            assertEquals("ADD FEATURE SUCCESS", line);

            url = new URL("http://localhost:" + port + "/feature/service/add?feature=osgiConsole-1.0");
            System.out.println("URL=" + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            line = readResponse(conn);
            assertEquals("LIBERTY FEATURE SERVICE FOUND", line);

            server_xml.put(server_remove_xml.getChannel());

            url = new URL("http://localhost:" + port + "/feature/remove");
            System.out.println("URL=" + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            line = readResponse(conn);
            assertEquals("REMOVE FEATURE SUCCESS", line);

            url = new URL("http://localhost:" + port + "/feature/service/remove?feature=osgiConsole-1.0");
            System.out.println("URL=" + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            line = readResponse(conn);
            assertEquals("LIBERTY FEATURE SERVICE UNREGISTERED", line);

            server_xml.put(server_user_add_xml.getChannel());

            url = new URL("http://localhost:" + port + "/feature/useradd");
            System.out.println("URL=" + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            line = readResponse(conn);
            assertEquals("ADD USER FEATURE SUCCESS", line);

            url = new URL("http://localhost:" + port + "/feature/service/add?feature=usr:usertest");
            System.out.println("URL=" + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            line = readResponse(conn);
            assertEquals("LIBERTY FEATURE SERVICE FOUND", line);

            server_xml.put(server_remove_xml.getChannel());

            url = new URL("http://localhost:" + port + "/feature/userremove");
            System.out.println("URL=" + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            line = readResponse(conn);
            assertEquals("REMOVE USER FEATURE SUCCESS", line);

            url = new URL("http://localhost:" + port + "/feature/service/remove?feature=usr:usertest");
            System.out.println("URL=" + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            line = readResponse(conn);
            assertEquals("LIBERTY FEATURE SERVICE UNREGISTERED", line);

            server_xml.put(server_product_add_xml.getChannel());

            url = new URL("http://localhost:" + port + "/feature/productadd");
            System.out.println("URL=" + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            line = readResponse(conn);
            assertEquals("ADD PRODUCT FEATURE SUCCESS", line);

            url = new URL("http://localhost:" + port + "/feature/service/add?feature=testproduct:prodtest-1.0");
            System.out.println("URL=" + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            line = readResponse(conn);
            assertEquals("LIBERTY FEATURE SERVICE FOUND", line);

            server_xml.put(server_remove_xml.getChannel());

            url = new URL("http://localhost:" + port + "/feature/productremove");
            System.out.println("URL=" + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            line = readResponse(conn);
            assertEquals("REMOVE PRODUCT FEATURE SUCCESS", line);

            url = new URL("http://localhost:" + port + "/feature/service/remove?feature=testproduct:prodtest-1.0");
            System.out.println("URL=" + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            line = readResponse(conn);
            assertEquals("LIBERTY FEATURE SERVICE UNREGISTERED", line);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            tryToDisconnect(conn);
        }
    }

    @Trivial
    @FFDCIgnore(IOException.class)
    private void tryToDisconnect(HttpURLConnection conn) {
        if (conn != null) {
            conn.disconnect();
        }
    }

    @Trivial
    @FFDCIgnore(IOException.class)
    private void tryToClose(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ioe) {
                // ignore.
            }
        }
    }
}

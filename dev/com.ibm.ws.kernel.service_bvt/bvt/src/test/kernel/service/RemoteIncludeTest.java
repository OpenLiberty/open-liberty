/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.kernel.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.location.internal.RemoteResource;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 *
 */
public class RemoteIncludeTest {
    private static SharedOutputManager outputMgr;
    private static final String port = System.getProperty("HTTP_default", "8010");
    private static final String profile = System.getProperty("profile", "com.ibm.ws.kernel.service_bvt");

    private static WsResource server_xml;
    private static WsResource server_include_xml;
    private static WsResource remote_resource;

    Mockery context = new JUnit4Mockery();

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
        server_include_xml = locSvc.resolveResource("server-include.xml");
        remote_resource = locSvc.resolveResource("http://localhost:" + port + "/config/include");

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
     * Test including a remote resource
     */
    @Test
    public void testRemoteInclude() {
        final String m = "testDynamicFeatures";
        HttpURLConnection conn = null;
        try {
            server_xml.put(server_include_xml.getChannel());

            URL url = new URL("http://localhost:" + port + "/config/isAdded");
            System.out.println("URL=" + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            String line = readResponse(conn);
            assertEquals("ADD FEATURE SUCCESS", line);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            tryToDisconnect(conn);
        }
    }

    /**
     * Test getting the last modified time on the resource
     */
    @Test
    public void testLastModified() {
        final String m = "testLastModified";
        HttpURLConnection conn = null;
        try {

            long time1 = remote_resource.getLastModified();
            long time2 = remote_resource.getLastModified();
            assertEquals("Last modified should not change", time1, time2);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            tryToDisconnect(conn);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.RemoteResource#newResource(String, String)}
     * 
     * @throws IOException
     */
    @Test(expected = java.io.IOException.class)
    public void testConstructResourceBadURL() throws IOException {
        URL url = new URL("http://testConstructResourceBadURL");

        new RemoteResource(url).get();
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.RemoteResource#getChild()}
     */
    @Test(expected = java.lang.UnsupportedOperationException.class)
    public void testGetChild() {
        remote_resource.getChild(null);
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.RemoteResource#getParent()}
     */
    @Test(expected = java.lang.UnsupportedOperationException.class)
    public void testGetParent() throws Exception {
        remote_resource.getParent();
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.RemoteResource#put(ReadableByteChannel)}
     */
    @Test(expected = java.lang.UnsupportedOperationException.class)
    public void testPut() throws Exception {
        final ReadableByteChannel rbc = context.mock(ReadableByteChannel.class);
        remote_resource.put(rbc);
    }

    @Test(expected = java.lang.UnsupportedOperationException.class)
    public void testPutStream() throws Exception {
        final InputStream is = null;
        remote_resource.put(is);
    }

    @Test(expected = java.lang.NullPointerException.class)
    public void testMoveToWithNull() throws IOException {
        remote_resource.moveTo(null);
    }

    @Test(expected = java.lang.IllegalArgumentException.class)
    public void testMoveToSelf() throws IOException {
        remote_resource.moveTo(remote_resource);
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.RemoteResource#resolveRelative(String)}
     */
    @Test
    public void testResolveWithNull() throws Exception {
        assertNull(remote_resource.resolveRelative(null));
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.RemoteResource#resolveRelative(String)}
     */
    @Test
    public void testResolve() throws Exception {
        assertNull(remote_resource.resolveRelative("somePath"));
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

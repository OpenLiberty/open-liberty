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
package com.ibm.ws.kernel.service.location.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.channels.ReadableByteChannel;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;

/**
 *
 */
public class RemoteResourceTest {
    private static SharedOutputManager outputMgr;
    private static final String profile = System.getProperty("profile", "com.ibm.ws.kernel.service");

    private static WsResource remote_resource;

    static WsLocationAdmin locSvc;

    static Mockery context = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private static final String testBuildDir = System.getProperty("test.buildDir", "generated");
    public static final String TEST_DATA_DIR = testBuildDir + "/test/rrtest data";
    public static final File TEST_DATA_FILE = new File(TEST_DATA_DIR);

    static URLConnection mockURLConnection = context.mock(URLConnection.class);

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
            @Override
            public URLStreamHandler createURLStreamHandler(String protocol) {
                URLStreamHandler retVal = null;
                if (protocol.equals("http")) {
                    retVal = new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL u) throws IOException {
                            return mockURLConnection;
                        }
                    };
                }
                return retVal;
            }
        });

        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
        TEST_DATA_FILE.mkdirs();
        locSvc = (WsLocationAdmin) SharedLocationManager.createLocations(TEST_DATA_DIR, profile);

        context.checking(new Expectations() {
            {
                oneOf(mockURLConnection).getInputStream();
                will(returnValue(new ByteArrayInputStream("meaningless test data to cache".getBytes("UTF8"))));
            }
        });
        remote_resource = locSvc.resolveResource("http://localhost:8010/config/include");
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

    /**
     * Test getting the last modified time on the resource
     */
    @Test
    public void testLastModified() {
        final String m = "testLastModified";
        HttpURLConnection conn = null;
        try {

            context.checking(new Expectations() {
                {
                    oneOf(mockURLConnection).getLastModified();
                    will(returnValue(1L));
                    oneOf(mockURLConnection).getLastModified();
                    will(returnValue(1L));
                }
            });

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

        context.checking(new Expectations() {
            {
                allowing(mockURLConnection).getInputStream();
                will(throwException(new java.net.UnknownHostException()));
            }
        });
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

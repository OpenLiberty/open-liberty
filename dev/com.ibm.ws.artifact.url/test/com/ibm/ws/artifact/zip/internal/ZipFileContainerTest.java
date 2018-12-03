/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.internal;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.url.URLStreamHandlerSetter;

import test.common.SharedOutputManager;

import com.ibm.ws.artifact.url.internal.WSJarURLStreamHandler;
import com.ibm.ws.artifact.zip.cache.ZipCachingService;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;

/**
 * TODO: this class could be simplified by using the java.nio.file package
 * (Path, Files, etc.) but that requires Java 7.
 */
public class ZipFileContainerTest {

    private static SharedOutputManager outputMgr;
    private final static File SOME_JAR = new File("resources/test/somejar.jar");
    private final static File SOME_JAR_IN_DIR_WITH_SPACES = new File("resources/test with spaces/somejar.jar");

    private final static WSJarURLStreamHandler HANDLER = new WSJarURLStreamHandler();

    private final static ContainerFactoryHolder MOCK_CFH = new ContainerFactoryHolder() {

        @Override
        public ArtifactContainerFactory getContainerFactory() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ZipCachingService getZipCachingService() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public BundleContext getBundleContext() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean useJarUrls() {
            // return false here to ensure we are testing wsjar protocol
            return false;
        }

    };

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor: 
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {

            @Override
            public URLStreamHandler createURLStreamHandler(String protocol) {
                return HANDLER;
            }

        });
        try {
            HANDLER.parseURL(new URLStreamHandlerSetter() {

                @Override
                public void setURL(URL u, String protocol, String host, int port, String file, String ref) {
                    // call set(String protocol, String host, int port, String file, String ref)
                    try {
                        Class<?> urlClass = u.getClass();
                        Method m = urlClass.getDeclaredMethod("set", String.class, String.class, int.class, String.class, String.class);
                        m.setAccessible(true);
                        m.invoke(u, protocol, host, port, file, ref);
                    } catch (Throwable t) {
                        throw new RuntimeException("unable to set fields on url");
                    }
                }

                @Override
                public void setURL(URL u, String protocol, String host, int port, String authority, String userInfo, String path, String query, String ref) {
                    // call set(String protocol, String host, int port, String authority, String userInfo, String path, String query, String ref)
                    try {
                        Class<?> urlClass = u.getClass();
                        Method m = urlClass.getDeclaredMethod("set", String.class, String.class, int.class, String.class, String.class, String.class, String.class, String.class);
                        m.setAccessible(true);
                        m.invoke(u, protocol, host, port, authority, userInfo, path, query, ref);
                    } catch (Throwable t) {
                        throw new RuntimeException("unable to set fields on url");
                    }
                }
            }, null, null, -1, -1);
        } catch (NullPointerException ex) {
            //expected - we are just trying to prime the HANDLER - not create a real URL
        }
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

    @Test
    public void testSpaceInPathToJar() throws Exception {
        File f = null;
        try {
            f = File.createTempFile("testSpaceInPathToJar", ".zip"); // used for zfc ctor to avoid NPE
            ZipFileContainer zfc = new ZipFileContainer(null, f, MOCK_CFH);
            URI uri = zfc.createEntryUri("/META-INF/somefile.txt", SOME_JAR_IN_DIR_WITH_SPACES);

            URL wsjarUrl = uri.toURL();
            URLConnection conn = HANDLER.openConnection(wsjarUrl);
            assertNotNull("unable to open connection to url, " + uri.toASCIIString(), conn);
        } finally {
            if (f != null && f.exists()) {
                f.delete();
            }
        }
    }

    @Test
    public void testSpaceInJarEntry() throws Exception {
        File f = null;
        try {
            f = File.createTempFile("testSpaceInJarEntry", ".zip"); // used for zfc ctor to avoid NPE
            ZipFileContainer zfc = new ZipFileContainer(null, f, MOCK_CFH);
            URI uri = zfc.createEntryUri("/META-INF/some file with spaces.txt", SOME_JAR);

            URL wsjarUrl = uri.toURL();
            URLConnection conn = HANDLER.openConnection(wsjarUrl);
            assertNotNull("unable to open connection to url, " + uri.toASCIIString(), conn);
        } finally {
            if (f != null && f.exists()) {
                f.delete();
            }
        }
    }
}

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
package com.ibm.ws.kernel.internal.classloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Enumeration;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import test.common.SharedOutputManager;

public class JarFileClassLoaderTest {

    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    private JarFileClassLoader getFileURLClassLoader(boolean verify) throws Exception {
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        File badJar = new File(testClassesDir + "/test data/signed/bad.jar");
        assertTrue(badJar.exists());
        return new JarFileClassLoader(new URL[] { badJar.toURI().toURL() }, verify, null);
    }

    @Test
    public void testDirectoryClassPath() throws Exception {
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        File directory = new File(testClassesDir + "/test data/");
        assertTrue(directory.exists());
        JarFileClassLoader classloader = new JarFileClassLoader(new URL[] { directory.toURI().toURL() }, false, null);
        assertNotNull(classloader.getResource("signed/bad.jar"));
    }

    @Ignore
    public void testFileURL() throws Exception {
        JarFileClassLoader loader = null;

        loader = getFileURLClassLoader(true);
        testClassLoaderVerify(loader);

        loader = getFileURLClassLoader(false);
        testClassLoaderNoVerify(loader);
    }

    private JarFileClassLoader getNonFileURLClassLoader(boolean verify) throws Exception {
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        final File badJar = new File(testClassesDir + "/test data/signed/bad.jar");
        assertTrue(badJar.exists());
        URLStreamHandler handler = (new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return new URLConnection(u) {
                    @Override
                    public void connect() throws IOException {}

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return new FileInputStream(badJar);
                    }
                };
            }
        });
        URL url = new URL("foo", "", -1, badJar.getAbsolutePath(), handler);
        return new JarFileClassLoader(new URL[] { url }, verify, null);
    }

    @Ignore
    public void testNonFileURL() throws Exception {
        JarFileClassLoader loader = null;

        loader = getNonFileURLClassLoader(true);
        System.out.println(loader);
        System.out.println(loader.getURLs());
        testClassLoaderVerify(loader);

        loader = getNonFileURLClassLoader(false);
        System.out.println(loader);
        System.out.println(loader.getURLs());
        testClassLoaderNoVerify(loader);
    }

    private void testClassLoaderNoVerify(JarFileClassLoader loader) throws Exception {
        // test loadClass()
        assertNotNull(loader.loadClass("org.eclipse.equinox.metatype.Extendable"));

        // test getResource()
        URL resource = loader.getResource("org/eclipse/equinox/metatype/EquinoxMetaTypeService.class");
        assertNotNull(resource);
        assertNotNull(resource.openStream());

        // test getResources()
        Enumeration<URL> resources = loader.getResources("org/eclipse/equinox/metatype/EquinoxMetaTypeInformation.class");
        assertNotNull(resources);
        assertTrue(resources.hasMoreElements());
        assertNotNull(resources.nextElement().openStream());
    }

    private void testClassLoaderVerify(JarFileClassLoader loader) throws Exception {
        // test loadClass()
        try {
            loader.loadClass("org.eclipse.equinox.metatype.Extendable");
            fail("Did not throw SecurityException");
        } catch (SecurityException e) {
            // we expect SecurityException
        }

        // test getResource()
        URL resource = loader.getResource("org/eclipse/equinox/metatype/EquinoxMetaTypeService.class");
        assertNotNull(resource);
        try {
            resource.openStream();
            fail("Did not throw SecurityException");
        } catch (SecurityException e) {
            // we expect SecurityException
        }

        // test getResources()
        Enumeration<URL> resources = loader.getResources("org/eclipse/equinox/metatype/EquinoxMetaTypeInformation.class");
        assertNotNull(resources);
        assertTrue(resources.hasMoreElements());
        try {
            resources.nextElement().openStream();
            fail("Did not throw SecurityException");
        } catch (SecurityException e) {
            // we expect SecurityException
        }
    }

    private ClassLoader getResourceTestURLClassLoader(boolean urlLoader) throws MalformedURLException {
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        File parentJar = new File(testClassesDir + "/test data/resources/resources1.jar");
        assertTrue("No parent jar: " + parentJar, parentJar.exists());
        File childJar = new File(testClassesDir + "/test data/resources/resources2.jar");
        assertTrue("No child jar: " + childJar, childJar.exists());
        URLClassLoader parent = new URLClassLoader(new URL[] { parentJar.toURI().toURL() });
        URL[] childUrls = new URL[] { childJar.toURI().toURL() };
        if (urlLoader) {
            return new BootstrapChildFirstURLClassloader(childUrls, parent);
        } else {
            return new BootstrapChildFirstJarClassloader(childUrls, parent);
        }
    }

    @Test
    public void testChildFirstGetResource() throws MalformedURLException {
        doTestChildFirstGetResource(getResourceTestURLClassLoader(true));
        doTestChildFirstGetResource(getResourceTestURLClassLoader(false));
    }

    public void doTestChildFirstGetResource(ClassLoader loader) {
        URL bootResource = loader.getResource("com/ibm/ws/kernel/boot/test.properties");
        assertNotNull("no boot resource found.", bootResource);
        URL testResource = loader.getResource("resources/test.properties");
        assertNotNull("no test resource found.", testResource);
        assertResource(bootResource, "1");
        assertResource(testResource, "2");
    }

    private void assertResource(URL resource, String expected) {
        Properties props = new Properties();
        try {
            props.load(resource.openStream());
        } catch (IOException e) {
            fail("Failed to load resource: " + e.toString());
        }
        assertEquals("Wrong resource test value", expected, props.getProperty("test"));
    }

    @Test
    public void testChildFirstGetResources() throws MalformedURLException {
        doTestChildFirstGetResources(getResourceTestURLClassLoader(true));
        doTestChildFirstGetResources(getResourceTestURLClassLoader(false));
    }

    public void doTestChildFirstGetResources(ClassLoader loader) {
        try {
            Enumeration<URL> bootResources = loader.getResources("com/ibm/ws/kernel/boot/test.properties");
            assertNotNull("no boot resource found.", bootResources);
            Enumeration<URL> testResources = loader.getResources("resources/test.properties");
            assertNotNull("no test resource found.", testResources);
            assertResources(bootResources, "1", "2");
            assertResources(testResources, "2", "1");
        } catch (IOException e) {
            fail("Failed to get resources: " + e.toString());
        }
    }

    private void assertResources(Enumeration<URL> testResources, String... expected) {
        for (String expectedValue : expected) {
            assertResource(testResources.nextElement(), expectedValue);
        }
    }

}

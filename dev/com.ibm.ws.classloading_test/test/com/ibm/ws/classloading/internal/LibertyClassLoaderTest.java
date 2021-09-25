/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.internal.TestUtil.buildMockContainer;
import static com.ibm.ws.classloading.internal.TestUtil.createAppClassloader;
import static com.ibm.ws.classloading.internal.TestUtil.getClassLoadingService;
import static com.ibm.ws.classloading.internal.TestUtil.getOtherClassesURL;
import static com.ibm.ws.classloading.internal.TestUtil.getServletJarURL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;

import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration;
import com.ibm.ws.classloading.internal.TestUtil.ClassSource;
import com.ibm.ws.kernel.boot.classloader.ClassLoaderHook;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.classloading.ClassLoadingServiceException;
import com.ibm.wsspi.classloading.GatewayConfiguration;

import test.common.SharedOutputManager;

public class LibertyClassLoaderTest {
    @Rule
    public final SharedOutputManager outputManager = SharedOutputManager.getInstance();

    AppClassLoader loader;

    @Before
    public void createClassLoaderForOtherClasses() throws Exception {
        this.loader = createAppClassloader(this.getClass().getName() + ".child-last", getOtherClassesURL(ClassSource.A), false);
    }

    @Test
    public void testClassNotFound() {
        assertNull(loader.findClassBytes("non.existent.Class", "non/existent/Class.class"));
    }

    @Test
    public void testClassFormatError() throws Exception {
        Container c = buildMockContainer("testClasses", getOtherClassesURL(ClassSource.A));

        loader = new AppClassLoader(loader.getParent(), loader.config, Arrays.asList(c), (DeclaredApiAccess) (loader.getParent()), null, null, new GlobalClassloadingConfiguration()) {
            @Override
            protected com.ibm.ws.classloading.internal.AppClassLoader.ByteResourceInformation findClassBytes(String className, String resourceName,
                                                                                                             ClassLoaderHook hook) throws IOException {
                throw new IOException();
            }
        };

        try {
            loader.findClassBytes("test", "test.class");
            fail("call should have thrown an exception");
        } catch (ClassFormatError e) {
            assertTrue("Should have traced an error", outputManager.checkForStandardErr("CWWKL0002E"));
        }
    }

    /**
     * Validates that we only load the Manifest one time for a particular jar.
     * 
     * @throws Exception
     */
    @Test
    public void testManifestLoadedOnce() throws Exception {
        Container c = buildMockContainer("testJar", TestUtil.getTestJarURL());

        List<Manifest> manifests = new ArrayList<>();
        loader = new AppClassLoader(null, loader.config, Arrays.asList(c), (DeclaredApiAccess) (loader.getParent()), null, null, new GlobalClassloadingConfiguration()) {
            @Override
            public Package definePackage(String name, Manifest manifest, URL sealBase) throws IllegalArgumentException {
                manifests.add(manifest);
                return super.definePackage(name, manifest, sealBase);
            }
        };

        loader.loadClass("test.StringReturner");

        loader.loadClass("test2.Package2Class");

        assertEquals(2, manifests.size());

        assertSame(manifests.get(0), manifests.get(1));
    }

    @Test
    public void testFailedToResolveGatewayBundle() throws IOException, BundleException, InvalidSyntaxException {
        // find the servlet jar
        URL[] urlsForParentClassLoader = { getServletJarURL() };
        // get a classloader service that thinks it is in a framework
        ClassLoader parentLoader = new URLClassLoader(urlsForParentClassLoader);
        // create a service that will generate resolution errors
        ClassLoadingServiceImpl service = getClassLoadingService(parentLoader, true);
        // configure up a classloader
        GatewayConfiguration gwConfig = service.createGatewayConfiguration();
        ClassLoaderConfiguration config = service.createClassLoaderConfiguration();
        ClassLoaderIdentity identity = service.createIdentity("UnitTest", "unresolvable");
        config.setId(identity);

        // create a class loader that we expect to fail to resolve the gateway bundle
        try {
            service.createTopLevelClassLoader(Collections.<Container> emptyList(), gwConfig, config);
            fail("Expected failure to resolve gateway bundle");
        } catch (ClassLoadingServiceException e) {
            // expected
            assertTrue("Cause should be a bundle exception: " + e.getCause(), e.getCause() instanceof BundleException);
        }
    }
}

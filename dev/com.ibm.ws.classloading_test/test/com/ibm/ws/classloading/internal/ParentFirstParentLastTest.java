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
import static com.ibm.ws.classloading.internal.TestUtil.getClassLoadingService;
import static com.ibm.ws.classloading.internal.TestUtil.getOtherClassesURL;
import static com.ibm.ws.classloading.internal.TestUtil.getTestClassesURL;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.classloading.internal.TestUtil.ClassSource;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.classloading.GatewayConfiguration;

/**
 * Testing the parent-first/parent-last classloading options. These tests work by creating a parent classloader that points to the classes within this project and a child
 * classloader that points to the classes within the {@link ClassSource#A} folder of the com.ibm.ws.classloading_test.jarA project.
 */
public class ParentFirstParentLastTest {
    @Rule
    public final SharedOutputManager outputManager = SharedOutputManager.getInstance();

    static ClassLoadingService cls;
    static ClassLoader parentLastLoader;
    static ClassLoader parentFirstLoader;

    @BeforeClass
    public static void createClassLoaders() throws Exception {
        ClassLoader parentLoader = new URLClassLoader(new URL[] { getTestClassesURL() });
        cls = getClassLoadingService(parentLoader);

        final URL otherClassesURL = getOtherClassesURL(ClassSource.A);
        GatewayConfiguration gwConfig = cls.createGatewayConfiguration()
                        .setApplicationName("ParentFirstParentLastTest")
                        .setDynamicImportPackage(Collections.unmodifiableList(Arrays.asList("*")));
        ClassLoaderConfiguration parentLastConfig = cls.createClassLoaderConfiguration()
                        .setDelegateToParentAfterCheckingLocalClasspath(true)
                        .setId(cls.createIdentity("ParentFirstParentLast", "parentLastLoader"));
        ClassLoaderConfiguration parentFirstConfig = cls.createClassLoaderConfiguration()
                        .setDelegateToParentAfterCheckingLocalClasspath(false)
                        .setId(cls.createIdentity("ParentFirstParentLast", "parentFirstLoader"));

        parentLastLoader = cls.createTopLevelClassLoader(asList(buildMockContainer("ParentLast", otherClassesURL)), gwConfig, parentLastConfig);
        parentFirstLoader = cls.createTopLevelClassLoader(asList(buildMockContainer("ParentFirst", otherClassesURL)), gwConfig, parentFirstConfig);
    }

    /**
     * Test to make sure that when there is an identical class in the child
     * and parent classloaders then it will use the child classloader when
     * it should.
     */
    @Test
    public void testParentLastClassloading() throws Exception {
        Object result = parentLastLoader.loadClass("test.StringReturner").getMethod("getString").invoke(null);
        assertEquals("Should get string from child loader", "Output from child", result);
    }

    /**
     * Test to make sure that when there is an identical class in the child
     * and parent classloaders then it will use the parent classloader when
     * it should.
     */
    @Test
    public void testParentFirstClassloading() throws Exception {
        Object string = parentFirstLoader.loadClass("test.StringReturner").getMethod("getString").invoke(null);
        assertEquals("Should get string from parent loader", "Output from parent", string);
    }

    /**
     * Test to make sure if a class exists in the parent but not the child
     * then it will load it from the parent even on child first.
     */
    @Test
    public void testDelegateToParent() throws Exception {
        Object clazz = parentLastLoader.loadClass("test.OnlyInParent");
        assertNotNull("The class should have been found by the parent classloader", clazz);
    }

    /**
     * Test to make sure if a class exists in the child but not the parent
     * then it will load it from the child even on parent first.
     */
    @Test
    public void testDelegateToChild() throws Exception {
        Object clazz = parentFirstLoader.loadClass("test.OnlyInChild");
        assertNotNull("The class should have been found by the parent classloader", clazz);
    }

    /**
     * This tests that you can load a resource from the child when using a
     * parent last classloader.
     */
    @Test
    public void testParentLastResourceLoading() throws Exception {
        InputStream is = parentLastLoader.getResourceAsStream("test/TestResource.txt");
        String content = streamToString(is);
        assertEquals("Should get resource from child loader", "Test Resource In Child", content.trim());
    }

    /**
     * This tests that you can load a resource from the parent when using a
     * parent first classloader.
     */
    @Test
    public void testParentFirstResourceLoading() throws Exception {
        InputStream is = parentFirstLoader.getResourceAsStream("test/TestResource.txt");
        String content = streamToString(is);
        assertEquals("Should get resource from parent loader", "Test Resource In Parent", content.trim());
    }

    @Test
    public void testParentLastClassLoaderHasGatewayClassLoaderAsParent() throws Exception {
        ClassLoader parent = parentLastLoader.getParent();
        assertNotNull("The parentLast class loader should have a non-null parent", parent);
        assertEquals("The parent loader should be a gateway class loader", GatewayClassLoader.class, parent.getClass());
    }

    @Test
    public void testParentFirstClassLoaderHasGatewayClassLoaderAsParent() throws Exception {
        ClassLoader parent = parentFirstLoader.getParent();
        assertNotNull("The parentFirst class loader should have a non-null parent", parent);
        assertEquals("The parent loader should be a gateway class loader", GatewayClassLoader.class, parent.getClass());
    }

    private String streamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();
        return sb.toString();
    }
}

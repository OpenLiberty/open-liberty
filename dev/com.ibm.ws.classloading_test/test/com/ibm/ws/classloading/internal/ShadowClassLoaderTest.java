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
import static com.ibm.ws.classloading.internal.TestUtil.getServletJarURL;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.classloading.internal.TestUtil.ClassSource;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.classloading.GatewayConfiguration;

public class ShadowClassLoaderTest {
    @Rule
    public SharedOutputManager outputManager = SharedOutputManager.getInstance();

    private static final ClassLoader BOOTSTRAP_CLASS_LOADER = Object.class.getClassLoader();

    private ClassLoadingServiceImpl service;
    private URLClassLoader parentClassLoader;
    private final LinkedList<AppClassLoader> originalLoaders = new LinkedList<AppClassLoader>();
    private final LinkedList<ShadowClassLoader> shadowLoaders = new LinkedList<ShadowClassLoader>();

    @Before
    public void setUp() throws Exception {
        parentClassLoader = new URLClassLoader(new URL[] { getServletJarURL() });
        service = getClassLoadingService(parentClassLoader);
        ClassLoaderIdentity idA = service.createIdentity("UnitTest", this.getClass().getName() + ".A");
        ClassLoaderIdentity idB = service.createIdentity("UnitTest", this.getClass().getName() + ".B");
        ClassLoaderIdentity idB2 = service.createIdentity("UnitTest", this.getClass().getName() + ".B2");

        synchronized (this) { // classloader for the A classes
            GatewayConfiguration gwConfig = service.createGatewayConfiguration();
            ClassLoaderConfiguration config = service.createClassLoaderConfiguration();
            config.setId(idA);
            // retrieve the classloader, making sure it can see the 'other' set of classes
            URL otherClassesURL = getOtherClassesURL(ClassSource.A);
            System.out.println(otherClassesURL);

            Container c = buildMockContainer("OtherClasses", otherClassesURL);

            originalLoaders.add(createTopLevelClassLoader(Arrays.asList(c), gwConfig, config));
            shadowLoaders.add(service.getShadowClassLoader(originalLoaders.getLast()));
        }
        synchronized (this) { // child classloader for the B classes
            ClassLoaderConfiguration config = service.createClassLoaderConfiguration();
            config.setId(idB);
            config.setParentId(idA);
            // retrieve the classloader, making sure it can see the 'other' set of classes
            URL otherClassesURL = getOtherClassesURL(ClassSource.B);
            System.out.println(otherClassesURL);

            Container c = buildMockContainer("otherclasses-sourceb", otherClassesURL);

            originalLoaders.add(createChildClassLoader(Arrays.asList(c), config));
            shadowLoaders.add(service.getShadowClassLoader(originalLoaders.getLast()));
        }
        synchronized (this) { // child-first classloader for the B classes
            ClassLoaderConfiguration config = service.createClassLoaderConfiguration();
            config.setId(idB2);
            config.setParentId(idA);
            config.setDelegateToParentAfterCheckingLocalClasspath(true);
            // retrieve the classloader, making sure it can see the 'other' set of classes
            URL otherClassesURL = getOtherClassesURL(ClassSource.B);
            System.out.println(otherClassesURL);

            Container c = buildMockContainer("otherclasses-sourceb-cfirst", otherClassesURL);

            originalLoaders.add(createChildClassLoader(Arrays.asList(c), config));
            shadowLoaders.add(service.getShadowClassLoader(originalLoaders.getLast()));
        }
    }

    @Test
    public void testNonLibertyClasses() throws Exception {
        for (ShadowClassLoader shadow : shadowLoaders) {
            expectToReuse("java.lang.String", shadow, BOOTSTRAP_CLASS_LOADER);
            expectToReuse("java.util.Map$Entry", shadow, BOOTSTRAP_CLASS_LOADER);
            expectNotToLoad("java.lang.Spring", shadow, ClassNotFoundException.class);
            expectToReuse("javax.servlet.Servlet", shadow, parentClassLoader);
        }
    }

    @Test
    public void testParentLastClassLoader() throws Exception {
        expectToReuse("javax.servlet.Servlet", shadowLoaders.getLast(), parentClassLoader);
    }

    @Test
    public void testNonSystemClasses() throws Exception {
        System.out.println(org.junit.Assert.class.getClassLoader());
        for (ShadowClassLoader shadow : shadowLoaders) {
            expectToLoad("test.OuterClass", shadow);
            expectToLoad("test.OuterClass$NestedClass", shadow);
            expectNotToLoad("test.OuterClass$NestedClassUnloadable", shadow, ExceptionInInitializerError.class);
            expectNotToLoad("test.OuterClass$NestedClassUnloadableChild", shadow, NoClassDefFoundError.class);
        }
    }

    protected AppClassLoader createTopLevelClassLoader(List<Container> classPath, GatewayConfiguration gwConfig, ClassLoaderConfiguration config) {
        return service.createTopLevelClassLoader(classPath, gwConfig, config);
    }

    protected AppClassLoader createBundleAddOnClassLoader(List<File> classPath, ClassLoader gwClassLoader, ClassLoaderConfiguration config) {
        return service.createBundleAddOnClassLoader(classPath, gwClassLoader, config);
    }

    protected AppClassLoader createChildClassLoader(List<Container> classpath, ClassLoaderConfiguration config) {
        return service.createChildClassLoader(classpath, config);
    }

    private void expectToReuse(String name, ShadowClassLoader shadow, ClassLoader original) throws ClassNotFoundException {
        Class<?> c1 = original == null ? null : original.loadClass(name);
        Class<?> c = shadow.loadClass(name);
        assertNotNull("Should be able to load " + name, c);
        assertSame("Class should be loaded by the original class loader", original, c.getClassLoader());
    }

    private void expectToLoad(String name, ShadowClassLoader shadow) throws ClassNotFoundException {
        System.out.printf("try to load %s by %s%n", name, shadow);
        Class<?> c = shadow.loadClass(name);
        System.out.printf("%s loaded by %s%n", c, c.getClassLoader());
        assertNotNull("Should be able to load " + name, c);
        assertSame("Class should have shadow classloader as its loader", ShadowClassLoader.class, c.getClassLoader().getClass());
    }

    private void expectNotToLoad(String name, ShadowClassLoader shadow, Class<? extends Throwable> exceptionClass) throws ClassNotFoundException {
        try {
            System.out.printf("try to load %s by %s%n", name, shadow);
            Class<?> c = Class.forName(name, true, shadow);
            System.out.printf("%s should not be loaded by %s%n", c, c.getClassLoader());
            fail("Expected ClassNotFoundException but loadClass(" + name + ") returned " + c);
        } catch (Throwable t) {
            if (!!!exceptionClass.isAssignableFrom(t.getClass()))
                rethrow(t);
        }
    }

    // hacky trick to rethrow anything
    private void rethrow(Throwable t) {
        this.<Error> rethrowGeneric(t);
    }

    @SuppressWarnings("unchecked")
    // use the parametric type T to fool the compiler into re-throwing anything at all
    private <T extends Throwable> void rethrowGeneric(Throwable t) throws T {
        throw (T) t; // cast to T compiles away to nothing!
    }
}

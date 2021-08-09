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
import static com.ibm.ws.classloading.internal.TestUtil.getTestJarURL;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.classloading.internal.GetLibraryAction.Availability;
import com.ibm.ws.classloading.internal.TestUtil.ClassSource;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.classloading.ClassTransformer;

public class ClassTransformationTest {
    @Rule
    public final SharedOutputManager outputManager = SharedOutputManager.getInstance();

    static final ClassTransformer IDENTITY_TRANSFORMER = new ClassTransformer() {
        @Override
        public byte[] transformClass(String name, byte[] bytes, CodeSource source, ClassLoader loader) {
            return bytes;
        }
    };
    ClassLoadingService service;
    ClassLoader parentLoader, parentFirstLoader, parentLastLoader;

    @Before
    public void createClassLoaderForOtherClasses() throws Exception {
        // find the servlet jar
        URL[] urlsForParentClassLoader = { getServletJarURL() };
        parentLoader = new URLClassLoader(urlsForParentClassLoader);
        service = getClassLoadingService(parentLoader);
        parentFirstLoader = createOtherLoader(false);
        parentLastLoader = createOtherLoader(true);
    }

    private ClassLoader createOtherLoader(boolean parentLast) throws MalformedURLException {
        final String name = parentLast ? "ParentLast" : "ParentFirst";
        URL otherClassesURL = getOtherClassesURL(ClassSource.A);
        Container otherClassesURLContainer = buildMockContainer(name, otherClassesURL);
        return service.createTopLevelClassLoader
                        (Arrays.asList(otherClassesURLContainer),
                         service.createGatewayConfiguration(),
                         service.createClassLoaderConfiguration()
                                         .setDelegateToParentAfterCheckingLocalClasspath(parentLast)
                                         .setId(service.createIdentity(this.getClass().getName(), name))
                        );
    }

    @Test
    public void testNoTransformationChildLast() throws Exception {
        checkDummyServletClass(parentFirstLoader);
    }

    @Test
    public void testNoTransformationChildFirst() throws Exception {
        checkDummyServletClass(parentLastLoader);
    }

    @Test
    public void testIdentityTransformationChildLast() throws Exception {
        service.registerTransformer(IDENTITY_TRANSFORMER, parentFirstLoader);
        checkDummyServletClass(parentFirstLoader);
    }

    @Test
    public void testIdentityTransformationChildFirst() throws Exception {
        service.registerTransformer(IDENTITY_TRANSFORMER, parentLastLoader);
        checkDummyServletClass(parentLastLoader);
    }

    private void checkDummyServletClass(ClassLoader loader) throws Exception {
        Class<?> servletClass = parentLoader.loadClass("javax.servlet.Servlet");
        Class<?> dummyServletClass = loader.loadClass("test.DummyServlet");
        Object o = dummyServletClass.newInstance();
        servletClass.cast(o);
    }

    @Test
    public void testCommonLibraryClassLoader() throws Exception {
        // create the library loader
        AppClassLoader commonClassloader = createAppClassloader("2", getOtherClassesURL(ClassSource.A), false);
        // Mock up the library behaviour
        final String libID = "commonLib";
        final GetLibraryAction getLibraries = new GetLibraryAction()
                        .mockCommonLibrary(libID, commonClassloader, Availability.ASYNC);

        // create the class loader that uses the common library
        AppClassLoader loader = createAppClassloader("1", getTestJarURL(), false, getLibraries);
        service.registerTransformer(IDENTITY_TRANSFORMER, loader);
        // try to load a class
        Class<?> clazz = loader.loadClass("test.DummyServlet");
        Assert.assertSame(commonClassloader, clazz.getClassLoader());
        System.out.println(clazz.getClassLoader());
    }
}

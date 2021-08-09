/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.internal.TestUtil.getClassLoadingService;
import static com.ibm.ws.classloading.internal.TestUtil.getTestJarURL;
import static com.ibm.wsspi.classloading.ApiType.API;
import static com.ibm.wsspi.classloading.ApiType.SPEC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.library.Library;

public class ProtectionDomainTest {

    private final Mockery mockery = new JUnit4Mockery();

    private static final ClassLoader BOOTSTRAP_CLASS_LOADER = Object.class.getClassLoader();

    private ClassLoadingServiceImpl service;

    @Before
    public void setUp() throws Exception {
        service = getClassLoadingService(BOOTSTRAP_CLASS_LOADER);
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testGetCodeSourceGetLocation() throws Exception {
        ClassLoaderIdentity id = service.createIdentity("UnitTest", this.getClass().getName());
        ClassLoaderConfiguration config = service.createClassLoaderConfiguration();
        config.setId(id);

        config.setProtectionDomain(new ProtectionDomain(new CodeSource(new File(".").toURI().toURL(), (Certificate[]) null), null));

        List<Container> containers = new ArrayList<Container>();
        MockContainer testJarContainer = new MockContainer("testJar", getTestJarURL());
        containers.add(testJarContainer);

        URL thisClassURL = this.getClass().getProtectionDomain().getCodeSource().getLocation();
        MockContainer unittestClassDir = new MockContainer("unittestClassDir", thisClassURL);
        containers.add(unittestClassDir);

        DeclaredApiAccess access = new DeclaredApiAccess() {

            @Override
            public EnumSet<ApiType> getApiTypeVisibility() {
                return null;
            }
        };
        AppClassLoader appClassLoader = new AppClassLoader(BOOTSTRAP_CLASS_LOADER, config, containers, access, null, null, new GlobalClassloadingConfiguration());

        Class<?> testJarClass = appClassLoader.loadClass("test.StringReturner");
        String location = testJarClass.getProtectionDomain().getCodeSource().getLocation().toString();
        assertTrue("Test class's protection domain's code source location is incorrect - expecting file://**/test.jar, found: " + location, location.contains("test.jar"));

        Class<?> thisClass = appClassLoader.loadClass(this.getClass().getName());
        location = thisClass.getProtectionDomain().getCodeSource().getLocation().toString();
        assertEquals("This class's protection domain's code source location is incorrect", thisClassURL.toString(), location);
    }

    /**
     * This tests cases like common shared libraries where the config uses a default ProtectionDomain.
     * Currently, non-default PDs for common libraries are only specified if the user has specified a
     * <code>&lt;javaPermission .../&gt;</code> element in their config that specifies the common library's
     * path in it's <em>codebase</em> attribute. This test will verify that we can get an accurate codebase
     * location even if the javaPermission is not set in the config.
     */
    @Test
    public void testGetCodeSourceGetLocationWhenConfigDoesNotSpecifyPD() throws Exception {
        ClassLoaderIdentity id = service.createIdentity("UnitTest", this.getClass().getName());
        ClassLoaderConfiguration config = service.createClassLoaderConfiguration();
        config.setId(id);

        List<Container> containers = new ArrayList<Container>();
        MockContainer testJarContainer = new MockContainer("testJar", getTestJarURL());
        containers.add(testJarContainer);

        URL thisClassURL = this.getClass().getProtectionDomain().getCodeSource().getLocation();
        MockContainer unittestClassDir = new MockContainer("unittestClassDir", thisClassURL);
        containers.add(unittestClassDir);

        DeclaredApiAccess access = new DeclaredApiAccess() {

            @Override
            public EnumSet<ApiType> getApiTypeVisibility() {
                return null;
            }
        };
        AppClassLoader appClassLoader = new AppClassLoader(BOOTSTRAP_CLASS_LOADER, config, containers, access, null, null, new GlobalClassloadingConfiguration());

        Class<?> testJarClass = appClassLoader.loadClass("test.StringReturner");
        String location = testJarClass.getProtectionDomain().getCodeSource().getLocation().toString();
        assertTrue("Test class's protection domain's code source location is incorrect - expecting file://**/test.jar, found: " + location, location.contains("test.jar"));

        Class<?> thisClass = appClassLoader.loadClass(this.getClass().getName());
        location = thisClass.getProtectionDomain().getCodeSource().getLocation().toString();
        assertEquals("This class's protection domain's code source location is incorrect", thisClassURL.toString(), location);
    }

    @Test
    public void testGetSharedLibraryClassLoaderWhenLibraryUsesFilesInsteadOfFileset() throws Exception {
        final Collection<File> files = new ArrayList<File>();
        File testJar = new File("test.jar");
        files.add(testJar);
        final Library lib = mockery.mock(Library.class);
        mockery.checking(new Expectations() {
            {
                allowing(lib).id();
                will(returnValue("testSharedLibrary"));
                allowing(lib).getApiTypeVisibility();
                will(returnValue(EnumSet.of(API, SPEC)));
                allowing(lib).getFilesets();
                will(returnValue(Collections.emptyList()));
                one(lib).getFiles();
                will(returnValue(files));
            }
        });

        Map<String, ProtectionDomain> protectionDomainMap = new HashMap<String, ProtectionDomain>();
        CodeSource codesource = new CodeSource(new URL("file://test.jar"), (Certificate[]) null);
        protectionDomainMap.put(testJar.getPath(), new ProtectionDomain(codesource, new Permissions()));
        service.setSharedLibraryProtectionDomains(protectionDomainMap);
        AppClassLoader appClassLoader = service.getSharedLibraryClassLoader(lib);
        String location = appClassLoader.config.getProtectionDomain().getCodeSource().getLocation().toString();

        assertTrue("Test protection domain's code source location is incorrect - expecting file://**/test.jar, found: " + location, location.contains("test.jar"));
    }
}

/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.internal.TestUtil.createAppClassloader;
import static com.ibm.ws.classloading.internal.TestUtil.getClassLoadingService;
import static com.ibm.ws.classloading.internal.TestUtil.getTestClassesURL;
import static com.ibm.ws.classloading.internal.TestUtil.getTestJarURL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

public class PackageDefinitionsTest {
    @Rule
    public final SharedOutputManager outputManager = SharedOutputManager.getInstance();

    /**
     * Test to make sure that the package is defined after calling loadClass when there is no transformer registered
     */
    @Test
    public void testPackageDeclaration() throws Exception {
        AppClassLoader loader = createAppClassloader(this.getClass().getName() + ".jar-loader", getTestJarURL(), true);
        testJarPackageDefinitions(loader);
    }

    /**
     * Test to make sure that the package is defined after calling loadClass when there is a transformer defined when using a class in a jar
     */
    @Test
    public void testPackageDeclarationWithTransformerOnJar() throws Exception {
        AppClassLoader loader = createAppClassloader(this.getClass().getName() + ".jar-loader-transform", getTestJarURL(), true);
        registerNoOpTransformer(loader);
        testJarPackageDefinitions(loader);
    }

    /**
     * Test to make sure that the package is defined after calling loadClass when there is a transformer defined when using a class in a directory
     */
    @Test
    public void testPackageDefinitionWithTransformOnDir() throws Exception {
        AppClassLoader loader = createAppClassloader(this.getClass().getName() + ".dir-loader", getTestClassesURL(), true);
        registerNoOpTransformer(loader);
        testPackageDefinitionsNotSet(loader);
    }

    /**
     * This test makes sure that when shadowing a directory the package is correctly defined by not having any properties set on it
     */
    @Test
    public void testPackageDefinitionsFromShadowLoaderOnDir() throws Exception {
        AppClassLoader loader = createAppClassloader(this.getClass().getName() + ".shadowed-dir", getTestClassesURL(), true);
        ClassLoadingServiceImpl service = getClassLoadingService(loader.getParent());
        ShadowClassLoader shadowLoader = service.getShadowClassLoader(loader);

        // The ShadowClassLoader doesn't know about the JAR that the class comes from so can't define the package in the same way so just make sure it's not null
        testPackageDefinitionsNotSet(shadowLoader);
    }

    /**
     * This test makes sure that when shadowing a directory the package is correctly defined by having any properties set on it from the manifest
     */
    @Test
    public void testPackageDefinitionsFromShadowLoaderOnJar() throws Exception {
        AppClassLoader loader = createAppClassloader(this.getClass().getName() + ".shadowed-jar", getTestJarURL(), true);
        ClassLoadingServiceImpl service = getClassLoadingService(loader.getParent());
        ShadowClassLoader shadowLoader = service.getShadowClassLoader(loader);
        testJarPackageDefinitions(shadowLoader);
    }

    /**
     * This util makes sure none of the package settings are set on the package defined by the supplied loader but that the package is not null.
     */
    private void testPackageDefinitionsNotSet(ClassLoader loader) throws AssertionError, ClassNotFoundException {
        Package package_ = getPackage(loader);
        assertNotNull("The package was null on the class", package_);
        assertNull("A value was set on the specification title even though it came from a dir on disk", package_.getSpecificationTitle());
        assertNull("A value was set on the specification version even though it came from a dir on disk", package_.getSpecificationVersion());
        assertNull("A value was set on the specification vendor even though it came from a dir on disk", package_.getSpecificationVendor());
        assertNull("A value was set on the implementation title even though it came from a dir on disk", package_.getImplementationTitle());
        assertNull("A value was set on the implementation version even though it came from a dir on disk", package_.getImplementationVersion());
        assertNull("A value was set on the implementation vendor even though it came from a dir on disk", package_.getImplementationVendor());
        assertFalse("A value was set on the sealed even though it came from a dir on disk", package_.isSealed());
    }

    /**
     * This will load the test.OnlyInParent class from the loader and make sure that the package defined on it matches the one in the manifest for the test.jar
     */
    private void testJarPackageDefinitions(ClassLoader loader) throws AssertionError, ClassNotFoundException {
        Package package_ = getPackage(loader);
        assertNotNull("The package was null on the class", package_);
        assertEquals("The package did not have the value of specification title from the manifest", "Classloading Test Resources", package_.getSpecificationTitle());
        assertEquals("The package did not have the value of specification version from the manifest", "1.0", package_.getSpecificationVersion());
        assertEquals("The package did not have the value of specification vendor from the manifest", "IBM", package_.getSpecificationVendor());
        assertEquals("The package did not have the value of implementation title from the manifest", "test", package_.getImplementationTitle());
        assertEquals("The package did not have the value of implementation version from the manifest", "foo1", package_.getImplementationVersion());
        assertEquals("The package did not have the value of implementation vendor from the manifest", "IBM", package_.getImplementationVendor());
        assertTrue("The package did not have the value of sealed from the manifest", package_.isSealed());
    }

    /**
     * Gets the package for the test.OnlyInParent class as defined by the supplied loader
     */
    private Package getPackage(ClassLoader loader) throws ClassNotFoundException {
        Class<?> class_ = loader.loadClass("test.OnlyInParent");
        Package package_ = class_.getPackage();
        return package_;
    }

    /**
     * Registers a transformer that does a no-op on the class (it just returns the original bytes)
     */
    private void registerNoOpTransformer(AppClassLoader loader) {
        loader.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String name, Class<?> clazz,
                                    ProtectionDomain pd, byte[] bytes) {
                return bytes;
            }
        });
    }
}

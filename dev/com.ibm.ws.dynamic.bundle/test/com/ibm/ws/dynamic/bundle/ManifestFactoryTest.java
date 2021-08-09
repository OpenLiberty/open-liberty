/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.dynamic.bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 *
 */
public class ManifestFactoryTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {}

    private static ManifestFactory getDefaultManifestFactory() {
        ManifestFactory mf = new ManifestFactory();
        mf.setBundleName("default");
        mf.setBundleSymbolicName("com.ibm.default");
        return mf;
    }

    private static Attributes makeManifestAttrs(String methodName, Object value) throws Exception {
        ManifestFactory mf = getDefaultManifestFactory();

        Method method = ManifestFactory.class.getMethod(methodName, value.getClass());
        Manifest m = ((ManifestFactory) method.invoke(mf, value)).createManifest();
        return m.getMainAttributes();
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#setManifestVersion(java.lang.String)}.
     */
    @Test
    public void testSetManifestVersion() throws Exception {
        Attributes attrs = makeManifestAttrs("setManifestVersion", "2.0");
        assertEquals("Generated manifest contained incorrect manifest version", "2", attrs.getValue(Constants.BUNDLE_MANIFESTVERSION));
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#setBundleName(java.lang.String)}.
     */
    @Test
    public void testSetBundleName() throws Exception {
        Attributes attrs = makeManifestAttrs("setBundleName", "com.ibm.my.awesome.bundle");
        assertEquals("Generated manifest contained incorrect bundle name", "com.ibm.my.awesome.bundle", attrs.getValue(Constants.BUNDLE_NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#setBundleVersion(org.osgi.framework.Version)}.
     */
    @Test
    public void testSetBundleVersion() throws Exception {
        Attributes attrs = makeManifestAttrs("setBundleVersion", new Version(17, 15, 6));
        assertEquals("Generated manifest contained incorrect bundle version", "17.15.6", attrs.getValue(Constants.BUNDLE_VERSION));
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#setBundleVendor(java.lang.String)}.
     */
    @Test
    public void testSetBundleVendor() throws Exception {
        Attributes attrs = makeManifestAttrs("setBundleVendor", "Who else?");
        assertEquals("Generated manifest contained incorrect bundle vendor", "Who else?", attrs.getValue(Constants.BUNDLE_VENDOR));
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#setBundleDescription(java.lang.String)}.
     */
    @Test
    public void testSetBundleDescription() throws Exception {
        Attributes attrs = makeManifestAttrs("setBundleDescription", "My very special bundle");
        assertEquals("Generated manifest contained incorrect bundle description", "My very special bundle", attrs.getValue(Constants.BUNDLE_DESCRIPTION));
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#setBundleManifestVersion(java.lang.String)}.
     */
    @Test
    public void testSetBundleManifestVersion() throws Exception {
        Attributes attrs = makeManifestAttrs("setBundleManifestVersion", "5.13.12");
        assertEquals("Generated manifest contained incorrect bundle manifest version", "5.13.12", attrs.getValue(Constants.BUNDLE_MANIFESTVERSION));
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#setBundleSymbolicName(java.lang.String)}.
     */
    @Test
    public void testSetBundleSymbolicName() throws Exception {
        Attributes attrs = makeManifestAttrs("setBundleSymbolicName", "com.ibm.my.unique.bundle");
        assertEquals("Generated manifest contained incorrect bundle symbolic name", "com.ibm.my.unique.bundle", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#importPackages(java.lang.String[])}.
     */
    @Test
    public void testImportPackagesStringArray() throws Exception {
        final String[] pkgs = new String[] { "org.my.third.party.pkg",
                                            "com.ibm.my.ibm.api",
                                            "javax.spec.pkg" };

        String importedPackages = makeManifestAttrs("importPackages", pkgs).getValue(Constants.IMPORT_PACKAGE);

        for (String pkg : pkgs) {
            assertTrue("Did not find expected import package", importedPackages.contains(pkg));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#requireBundles(java.lang.String[])}.
     */
    @Test
    public void testRequireBundlesStringArray() throws Exception {
        final String[] bundles = new String[] { "org.my.third.party.bundle",
                                               "com.ibm.my.ibm.api.bundle",
                                               "javax.spec.bundle" };

        String requiredBundles = makeManifestAttrs("requireBundles", bundles).getValue(Constants.REQUIRE_BUNDLE);

        for (String bundle : bundles) {
            assertTrue("Did not find expected import package", requiredBundles.contains(bundle));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#dynamicallyImportPackages(java.lang.String[])}.
     */
    @Test
    public void testDynamicallyImportPackagesStringArray() throws Exception {
        final String[] pkgs = new String[] { "org.my.third.party.pkg",
                                            "com.ibm.my.ibm.api",
                                            "javax.spec.pkg",
                                            "*;mandAttr=something" };

        String dynamicImportedPackages = makeManifestAttrs("dynamicallyImportPackages", pkgs).getValue(Constants.DYNAMICIMPORT_PACKAGE);

        for (String pkg : pkgs) {
            assertTrue("Did not find expected dynamically imported package", dynamicImportedPackages.contains(pkg));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#declareServiceComponents(java.lang.String[])}.
     */
    //@Test
    public void testDeclareServiceComponentsStringArray() throws Exception {
        //TODO:
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#addAttributeValues(java.lang.String, java.lang.Object[])}.
     */
    //@Test
    public void testAddAttributeValues() throws Exception {
        //TODO:
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#importPackages(java.lang.Iterable)}.
     */
    @Test
    public void testImportPackagesIterableOfString() throws Exception {
        final String[] pkgs = new String[] { "org.my.third.party.pkg",
                                            "com.ibm.my.ibm.api",
                                            "javax.spec.pkg" };

        List<String> pkgsList = Arrays.asList(pkgs);

        Attributes attrs = getDefaultManifestFactory().importPackages(pkgsList).createManifest().getMainAttributes();
        String importedPackages = attrs.getValue(Constants.IMPORT_PACKAGE);

        for (String pkg : pkgs) {
            assertTrue("Did not find expected import package", importedPackages.contains(pkg));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#requireBundles(java.lang.Iterable)}.
     */
    @Test
    public void testRequireBundlesIterableOfString() throws Exception {
        final String[] bundles = new String[] { "org.my.third.party.bundle",
                                               "com.ibm.my.ibm.api.bundle",
                                               "javax.spec.bundle" };
        List<String> bundlesList = Arrays.asList(bundles);

        Attributes attrs = getDefaultManifestFactory().requireBundles(bundlesList).createManifest().getMainAttributes();
        String requiredBundles = attrs.getValue(Constants.REQUIRE_BUNDLE);

        for (String bundle : bundles) {
            assertTrue("Did not find expected import package", requiredBundles.contains(bundle));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#dynamicallyImportPackages(java.lang.Iterable)}.
     */
    @Test
    public void testDynamicallyImportPackagesIterableOfString() throws Exception {
        final String[] pkgs = new String[] { "org.my.third.party.pkg",
                                            "com.ibm.my.ibm.api",
                                            "javax.spec.pkg",
                                            "*;mandAttr=something" };
        List<String> pkgsList = Arrays.asList(pkgs);

        Attributes attrs = getDefaultManifestFactory().dynamicallyImportPackages(pkgsList).createManifest().getMainAttributes();
        String dynamicImportedPackages = attrs.getValue(Constants.DYNAMICIMPORT_PACKAGE);

        for (String pkg : pkgs) {
            assertTrue("Did not find expected dynamically imported package", dynamicImportedPackages.contains(pkg));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#declareServiceComponents(java.lang.Iterable)}.
     */
    //@Test
    public void testDeclareServiceComponentsIterableOfString() throws Exception {
        //TODO:
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#addManifestAttribute(java.lang.String, java.lang.Iterable)}.
     */
    @Test
    public void testAddManifestAttribute() throws Exception {
        ManifestFactory mf = getDefaultManifestFactory();
        List<String> values = Arrays.asList("MyCustomValue");
        mf.addManifestAttribute("IBM-MyCustomAttr", values);
        Attributes attrs = mf.createManifest().getMainAttributes();
        assertEquals("Generated manifest did not contain expected custom attribute", "MyCustomValue", attrs.getValue("IBM-MyCustomAttr"));
    }

    /**
     * Test method for {@link com.ibm.ws.dynamic.bundle.ManifestFactory#setLazyActivation(boolean)}.
     */
    @Test
    public void testSetLazyActivation() throws Exception {
        ManifestFactory mf = getDefaultManifestFactory();
        mf.setLazyActivation(true);
        Attributes attrs = mf.createManifest().getMainAttributes();
        assertEquals("Generated manifest contained incorrect activation policy", "lazy", attrs.getValue(Constants.BUNDLE_ACTIVATIONPOLICY));

        mf.setLazyActivation(false);
        attrs = mf.createManifest().getMainAttributes();
        assertNull("Generated manifest contained a lazy activation policy when none was expected", attrs.getValue(Constants.BUNDLE_ACTIVATIONPOLICY));
    }

}

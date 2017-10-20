/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class BundleManifestTest {

    private JUnit4Mockery context;

    @Before
    public void setUp() {
        context = new JUnit4Mockery();
    }

    @Test
    public void getVendor() {
        String vendor = "Alice";
        Bundle bundle = makeBundle("Bundle-Vendor", vendor);
        BundleManifest parser = new BundleManifest(bundle);
        String actualVendor = parser.getBundleVendor();
        assertEquals("Wrong vendor.", vendor, actualVendor);

    }

    @Test
    public void testGetPrivatePackagesForSinglePackage() {
        String packageName = "birthday.present";
        Bundle bundle = makeBundle("Private-Package", packageName);
        BundleManifest parser = new BundleManifest(bundle);
        Set<String> packages = parser.getPrivatePackages();
        assertEqualsOrderless("Even a trivial private package was not parsed correctly", new String[] { packageName }, packages);
    }

    @Test
    public void testGetPrivatePackagesForRealisticCase() {
        String packageName = "com.ibm.ws.config.internal,com.ibm.ws.config.internal.cm,com.ibm.ws.config.internal.metatype,com.ibm.ws.config.internal.resources,com.ibm.ws.config.internal.schema,com.ibm.ws.config.internal.services,com.ibm.ws.config.internal.xml,com.ibm.ws.config.internal.xml.validator";
        String[] expectedPackages = { "com.ibm.ws.config.internal", "com.ibm.ws.config.internal.cm", "com.ibm.ws.config.internal.metatype", "com.ibm.ws.config.internal.resources",
                                      "com.ibm.ws.config.internal.schema", "com.ibm.ws.config.internal.services", "com.ibm.ws.config.internal.xml",
                                      "com.ibm.ws.config.internal.xml.validator" };
        Bundle bundle = makeBundle("Private-Package", packageName);
        BundleManifest parser = new BundleManifest(bundle);
        Set<String> packages = parser.getPrivatePackages();
        assertEqualsOrderless("The private package header was not parsed correctly", expectedPackages, packages);
    }

    @Test
    public void testGetExportedPackagesForEmptyPackage() {
        String packageName = "";
        Bundle bundle = makeBundle("Export-Package", packageName);
        BundleManifest parser = new BundleManifest(bundle);
        Set<String> packages = parser.getExportedPackages();
        assertEqualsOrderless("An empty exported package broke our parsing", new String[] {}, packages);
    }

    @Test
    public void testGetExportedPackagesForSinglePackage() {
        String packageName = "christmas.present";
        Bundle bundle = makeBundle("Export-Package", packageName);
        BundleManifest parser = new BundleManifest(bundle);
        Set<String> packages = parser.getExportedPackages();
        assertEqualsOrderless("Even a trivial exported package was not parsed correctly", new String[] { packageName }, packages);
    }

    @Test
    public void testGetExportedPackagesForRealisticCase() {
        String packageName = "org.osgi.jmx;version=\"1.0.0\";uses:=\"javax.management.openmbean\",org.osgi.jmx.framework;version=\"1.5.0\";uses:=\"org.osgi.jmx,javax.management.openmbean\",org.osgi.jmx.service.cm;version=\"1.3.0\";uses:=\"javax.management.openmbean\",org.osgi.jmx.service.permissionadmin;version=\"1.2.0\";uses:=\"org.osgi.jmx\",org.osgi.jmx.service.provisioning;version=\"1.2.0\";uses:=\"javax.management.openmbean\",org.osgi.jmx.service.useradmin;version=\"1.1.0\";uses:=\"org.osgi.jmx,javax.management.openmbean\"";

        String[] expectedPackages = { "org.osgi.jmx", "org.osgi.jmx.framework", "org.osgi.jmx.service.cm", "org.osgi.jmx.service.permissionadmin",
                                      "org.osgi.jmx.service.provisioning", "org.osgi.jmx.service.useradmin" };
        Bundle bundle = makeBundle("Export-Package", packageName);
        BundleManifest parser = new BundleManifest(bundle);
        Set<String> packages = parser.getExportedPackages();
        assertEqualsOrderless("The exported package header was not parsed correctly", expectedPackages, packages);
    }

    /**
     * Compares two collections for equality, ignoring the original order of elements
     */
    private void assertEqualsOrderless(String message, String[] expected, Collection<String> actual) {
        // Compare by sorting before comparing

        // Put into a set to eliminate duplicates
        Set<String> actualSet = convertToSet(actual);
        Set<String> expectedSet = convertToSet(expected);

        // Convert to a list so we get ordering
        List<String> expectedList = convertToSortedList(expectedSet);
        List<String> actualList = convertToSortedList(actualSet);

        assertEquals(message, expectedList, actualList);

    }

    private List<String> convertToSortedList(Collection<String> collection) {
        List<String> list = new ArrayList<String>();
        list.addAll(collection);
        Collections.sort(list);

        return list;
    }

    private Set<String> convertToSet(String[] array) {

        Set<String> set = new HashSet<String>();
        for (String s : array) {
            set.add(s);
        }
        return set;
    }

    private Set<String> convertToSet(Collection<String> collection) {
        Set<String> set = new HashSet<String>();
        set.addAll(collection);
        return set;
    }

    private Bundle makeBundle(String headerName, String value) {
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(headerName, value);

        final Bundle bundle = context.mock(Bundle.class);

        context.checking(new Expectations() {
            {
                // To reduce the verbosity of our expectations, use a map for all the service properties
                allowing(bundle).getHeaders("");
                will(returnValue(props));

            }
        });
        return bundle;

    }
}

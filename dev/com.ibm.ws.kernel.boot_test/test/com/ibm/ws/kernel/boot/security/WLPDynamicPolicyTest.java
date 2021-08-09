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
package com.ibm.ws.kernel.boot.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class WLPDynamicPolicyTest {

    private static URL testURL1;
    private static URL testURL2;
    private static URL unlistedURL;
    private static List<URL> urls;

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private Policy policy;
    private WLPDynamicPolicy dynamicPolicy;
    private final Permission allPermission = new AllPermission();
    private PermissionsCombiner permissionsCombiner;
    private PermissionCollection combinedPermissions;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        testURL1 = new URL("file:///testURL1");
        testURL2 = new URL("file:///testURL2");
        unlistedURL = new URL("file:///unlistedURL");
        urls = new ArrayList<URL>();
        urls.add(testURL1);
        urls.add(testURL2);
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Before
    public void setUp() throws Exception {
        policy = Policy.getPolicy();
        dynamicPolicy = new WLPDynamicPolicy(policy, urls);
        permissionsCombiner = mockery.mock(PermissionsCombiner.class);
    }

    @After
    public void tearDown() throws Exception {
        Policy.setPolicy(policy);
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.boot.internal.WLPDynamicPolicy#getPermissions(java.security.CodeSource)}.
     */
    @Test
    public void testGetPermissionsCodeSource_AllPermission() {
        CodeSource codesource = new CodeSource(testURL1, (java.security.cert.Certificate[]) null);
        PermissionCollection pemissionCollection = dynamicPolicy.getPermissions(codesource);
        assertTrue("The AllPermission must be granted to the codesource.", pemissionCollection.implies(allPermission));
    }

    @Test
    public void testGetPermissionsCodeSource_AllPermissionForDifferentCodeSource() {
        CodeSource codesource = new CodeSource(testURL2, (java.security.cert.Certificate[]) null);
        PermissionCollection pemissionCollection = dynamicPolicy.getPermissions(codesource);
        assertTrue("The AllPermission must be granted to the codesource.", pemissionCollection.implies(allPermission));
    }

    @Test
    public void testGetPermissionsCodeSource_AllPermissionForCodeSourceNotListedAndNoStaticPolicy() throws Exception {
        dynamicPolicy = new WLPDynamicPolicy(null, urls);
        CodeSource codesource = new CodeSource(unlistedURL, (java.security.cert.Certificate[]) null);
        PermissionCollection pemissionCollection = dynamicPolicy.getPermissions(codesource);
        assertTrue("The AllPermission must be granted to the codesource.", pemissionCollection.implies(allPermission));
    }

    @Test
    public void testGetPermissionsCodeSource_StaticPermissionsForCodeSourceNotListed() throws Exception {
        CodeSource codesource = new CodeSource(unlistedURL, (java.security.cert.Certificate[]) null);
        PermissionCollection pemissionCollection = dynamicPolicy.getPermissions(codesource);
        List<Permission> staticPermissions = Collections.list(policy.getPermissions(codesource).elements());
        assertTrue("The static permissions must be granted to the codesource.", staticPermissions.equals(Collections.list(pemissionCollection.elements())));
    }

    @Test
    public void testGetPermissionsCodeSource_StaticPermissionsForNullCodeSource() throws Exception {
        dynamicPolicy.setPermissionsCombiner(permissionsCombiner);
        CodeSource codesource = null;

        try {
            List<Permission> staticPermissions = Collections.list(policy.getPermissions(codesource).elements());
            PermissionCollection pemissionCollection = dynamicPolicy.getPermissions(codesource);
            assertTrue("The static permissions must be granted to the codesource.", staticPermissions.equals(Collections.list(pemissionCollection.elements())));
        } catch (NullPointerException e) {
            // Ignore NPE from policy.getPermissions(codesource) since Oracle's JDK does not support a null code source.
        }
    }

    @Test
    public void testGetPermissionsCodeSource_StaticPermissionsForNullLocation() throws Exception {
        dynamicPolicy.setPermissionsCombiner(permissionsCombiner);
        CodeSource codesource = new CodeSource(null, (java.security.cert.Certificate[]) null);
        List<Permission> staticPermissions = Collections.list(policy.getPermissions(codesource).elements());
        PermissionCollection pemissionCollection = dynamicPolicy.getPermissions(codesource);
        assertTrue("The static permissions must be granted to the codesource.", staticPermissions.equals(Collections.list(pemissionCollection.elements())));
    }

    @Test
    public void testGetPermissionsCodeSource_CombinedPermissionsForCodeSourceNotListed() throws Exception {
        dynamicPolicy.setPermissionsCombiner(permissionsCombiner);

        CodeSource codesource = new CodeSource(unlistedURL, (java.security.cert.Certificate[]) null);
        createPermissionsCombinerExpectations(codesource);
        PermissionCollection pemissionCollection = dynamicPolicy.getPermissions(codesource);
        assertEquals("The combined permissions must be granted to the codesource.", combinedPermissions, pemissionCollection);
    }

    private void createPermissionsCombinerExpectations(final CodeSource codesource) {
        combinedPermissions = mockery.mock(PermissionCollection.class);
        mockery.checking(new Expectations() {
            {
                one(permissionsCombiner).getCombinedPermissions(with(any(PermissionCollection.class)), with(codesource));
                will(returnValue(combinedPermissions));
            }
        });
    }

}

/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.test;

import static com.ibm.ws.lars.testutils.BasicChecks.checkCopyFields;
import static com.ibm.ws.lars.testutils.ReflectionTricks.reflectiveCallAnyTypes;
import static com.ibm.ws.repository.common.enums.ResourceType.INSTALL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.common.enums.DownloadPolicy;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.resources.ProductResource;
import com.ibm.ws.repository.resources.internal.ProductResourceImpl;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.Asset;

public class ProductResourceTest {

    @Rule
    public final RepositoryFixture fixture = FatUtils.getRestFixture();

    private final RepositoryConnection repoConnection = fixture.getAdminConnection();

    @Test
    public void testIsDownloadable() throws IOException {
        ProductResource product = WritableResourceFactory.createProduct(repoConnection, INSTALL);
        assertEquals("Products should be downloadable",
                     DownloadPolicy.ALL, product.getDownloadPolicy());
    }

    // TODO: Create tools test case
    // write copyfields methods to copy over the hacked edition for products
    @Test
    public void testCopyFields() throws IllegalArgumentException, IllegalAccessException, InstantiationException, NoSuchMethodException, SecurityException, InvocationTargetException, IOException {
        ProductResourceImpl left = new ProductResourceImpl(repoConnection);
        ProductResourceImpl right = new ProductResourceImpl(repoConnection);
        left.setType(ResourceType.INSTALL);
        right.setType(ResourceType.INSTALL);
        checkCopyFields(left, right);
    }

    /**
     * Checks that the appliesToFilter isn't used and that two addons do match when they have matching appliesTo's.
     *
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Test
    public void testMatchWithRegeneratedAppliesToWithFakeAppliesTo() throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        ProductResourceImpl addon1 = new ProductResourceImpl(null);
        addon1.setAppliesTo("com.ibm.websphere.appserver;productEditions=\"Base\", anotherProduct");
        Asset ass = (Asset) reflectiveCallAnyTypes(addon1, "getAsset", null, null);
        AppliesToFilterInfo atfi = new AppliesToFilterInfo();
        atfi.setEditions(Collections.singletonList("Random non matching edition"));
        ass.getWlpInformation().setAppliesToFilterInfo(Collections.singleton(atfi));
        addon1.setType(ResourceType.ADDON);

        ProductResourceImpl addon2 = new ProductResourceImpl(null);
        addon2.setAppliesTo("com.ibm.websphere.appserver;productEditions=\"Base\", anotherProduct");
        addon2.setType(ResourceType.ADDON);

        assertEquals("Matching data should match", addon1.createMatchingData(), addon2.createMatchingData());
    }

    /**
     * We'll ceate an appliesToFilter that matches but that shouldn't be used, instead a new appliesToFilter
     * should be created, and that should not match
     *
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Test
    public void testMatchWithRegeneratedAppliesToWithNonMatchingProducts() throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        ProductResourceImpl addon1 = new ProductResourceImpl(null);
        addon1.setAppliesTo("com.ibm.websphere.appserver;productEditions=\"Base\"");
        Asset ass1 = (Asset) reflectiveCallAnyTypes(addon1, "getAsset", null, null);
        AppliesToFilterInfo atfi1 = new AppliesToFilterInfo();
        atfi1.setEditions(Collections.singletonList("Fake Match"));
        ass1.getWlpInformation().setAppliesToFilterInfo(Collections.singleton(atfi1));
        addon1.setType(ResourceType.ADDON);

        ProductResourceImpl addon2 = new ProductResourceImpl(null);
        addon2.setAppliesTo("com.ibm.websphere.appserver;productEditions=\"ND\"");
        Asset ass2 = (Asset) reflectiveCallAnyTypes(addon1, "getAsset", null, null);
        AppliesToFilterInfo atfi2 = new AppliesToFilterInfo();
        atfi2.setEditions(Collections.singletonList("Fake Match"));
        ass2.getWlpInformation().setAppliesToFilterInfo(Collections.singleton(atfi2));
        addon2.setType(ResourceType.ADDON);

        assertFalse("Matching data should not match", addon1.createMatchingData().equals(addon2.createMatchingData()));
    }

    /**
     * This checks that the order of entries in the appliesTo doesn't matter.
     *
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Test
    public void testMatchWithRegeneratedAppliesToWithDifferentOrderProducts() throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        ProductResourceImpl addon1 = new ProductResourceImpl(null);
        addon1.setAppliesTo("com.ibm.websphere.appserver;productEditions=\"Base\";productVersion=8.5.5.0, anotherProduct");
        addon1.setType(ResourceType.ADDON);

        ProductResourceImpl addon2 = new ProductResourceImpl(null);
        addon2.setAppliesTo("anotherProduct, com.ibm.websphere.appserver;productEditions=\"Base\";productVersion=8.5.5.0");
        addon2.setType(ResourceType.ADDON);

        assertEquals("Matching data should match", addon1.createMatchingData(), addon2.createMatchingData());
    }

    /**
     * This checks that the order of the fields within an entry in the appliesTo doesn't matter.
     *
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Test
    public void testMatchWithRegeneratedAppliesToWithDifferentOrderWithinAProduct() throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        ProductResourceImpl addon1 = new ProductResourceImpl(null);
        addon1.setType(ResourceType.ADDON);
        addon1.setAppliesTo("com.ibm.websphere.appserver;productEditions=\"Base\"; productVersion=8.5.5.0, anotherProduct");

        ProductResourceImpl addon2 = new ProductResourceImpl(null);
        addon2.setType(ResourceType.ADDON);
        addon2.setAppliesTo("com.ibm.websphere.appserver; productVersion=8.5.5.0;productEditions=\"Base\", anotherProduct");

        assertEquals("Matching data should match", addon1.createMatchingData(), addon2.createMatchingData());
    }

    /**
     * Test an addon doesn't match an install
     */
    @Test
    public void testDifferentTypesDontMatch() {
        ProductResourceImpl addon = new ProductResourceImpl(null);
        addon.setType(ResourceType.ADDON);

        ProductResourceImpl install = new ProductResourceImpl(null);
        install.setType(ResourceType.INSTALL);

        assertFalse(addon.createMatchingData().equals(install.createMatchingData()));
    }

    /**
     * Test addons with different names don't match
     */
    @Test
    public void testDifferentNamesDontMatch() {
        ProductResourceImpl addon1 = new ProductResourceImpl(null);
        addon1.setType(ResourceType.ADDON);
        addon1.setName("addon1");

        ProductResourceImpl addon2 = new ProductResourceImpl(null);
        addon2.setType(ResourceType.ADDON);
        addon2.setName("addon2");

        assertFalse(addon1.createMatchingData().equals(addon2.createMatchingData()));
    }

    /**
     * Test assets with different platform info don't match
     */
    @Test
    public void testDifferentPlatformInfoDontMatch() {
        ProductResourceImpl windows = new ProductResourceImpl(null);
        windows.setType(INSTALL);
        windows.setGenericRequirements("osgi.native; filter:=\"(&(osgi.native.processor=x86-64)(osgi.native.osname=Win32))\"");

        ProductResourceImpl linux = new ProductResourceImpl(null);
        linux.setType(INSTALL);
        linux.setGenericRequirements("osgi.native; filter:=\"(&(osgi.native.processor=x86-64)(osgi.native.osname=Linux))\"");

        assertFalse(windows.createMatchingData().equals(linux.createMatchingData()));
    }
}

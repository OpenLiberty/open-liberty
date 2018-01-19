/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resolver.internal.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osgi.resource.Capability;

import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.SimpleProductDefinition;
import com.ibm.ws.repository.connections.liberty.ProductInfoProductDefinition;
import com.ibm.ws.repository.resolver.ResolverTestUtils;
import com.ibm.ws.repository.resolver.internal.LibertyVersion;
import com.ibm.ws.repository.resolver.internal.namespace.ProductNamespace;
import com.ibm.ws.repository.resolver.internal.resource.ProductResource;

/**
 * Tests for {@link ProductResource}
 */
public class ProductResourceTest {

    /**
     * Test to make sure that a product has the expected capabilities
     * 
     * @throws IOException
     * @throws ProductInfoParseException
     */
    @Test
    public void testCapabilities() throws IOException, ProductInfoParseException {
        List<ProductDefinition> productDefinitions = new ArrayList<ProductDefinition>();
        String product1Id = "product1";
        String product1Edition = "edition1";
        String product1Version = "1.0.0.0";
        String product1License = "License1";
        String product1InstallType = "install1";
        productDefinitions.add(new ProductInfoProductDefinition(ResolverTestUtils.createProductInfo(product1Id, product1Edition, product1Version, product1License,
                                                                                                    product1InstallType)));

        // Make sure some nulls are ok, but some fields are required
        String product2Id = "product2";
        String product2Edition = "edition2";
        String product2Version = "2.0.0.0";
        String product2License = null;
        String product2InstallType = null;
        productDefinitions.add(new SimpleProductDefinition(product2Id, product2Version, product2InstallType, product2License, product2Edition));

        ProductResource testObject = ProductResource.createInstance(productDefinitions);
        List<Capability> capabilities = testObject.getCapabilities(ProductNamespace.PRODUCT_NAMESPACE);
        assertEquals("There should be one capability for each product info", 2, capabilities.size());
        for (Capability capability : capabilities) {
            String capabilityId = (String) capability.getAttributes().get(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE);
            if (product1Id.equals(capabilityId)) {
                checkCapability(capability, product1Id, product1Edition, product1Version, product1License, product1InstallType);
            } else if (product2Id.equals(capabilityId)) {
                checkCapability(capability, product2Id, product2Edition, product2Version, product2License, product2InstallType);
            } else {
                fail("Unexpected capability with ID " + capabilityId);
            }
        }
    }

    /**
     * Check the attributes have been set on the capability
     * 
     * @param capability
     * @param id
     * @param edition
     * @param version
     * @param license
     * @param installType
     */
    private void checkCapability(Capability capability, String id, String edition, String version, String license, String installType) {
        Map<String, Object> attributes = capability.getAttributes();
        assertEquals("The ID should match the one in the product info", id, attributes.get(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE));
        assertEquals("The edition should match the one in the product info", edition, attributes.get(ProductNamespace.CAPABILITY_EDITION_ATTRIBUTE));
        assertEquals("The version should match the one in the product info", LibertyVersion.valueOf(version), attributes.get(ProductNamespace.CAPABILITY_VERSION_ATTRIBUTE));
        assertEquals("The license should match the one in the product info", license, attributes.get(ProductNamespace.CAPABILITY_LICENSE_TYPE_ATTRIBUTE));
        assertEquals("The install type should match the one in the product info", installType, attributes.get(ProductNamespace.CAPABILITY_INSTALL_TYPE_ATTRIBUTE));
    }

}

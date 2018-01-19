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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import com.ibm.ws.repository.resolver.ProductRequirementInformation;
import com.ibm.ws.repository.resolver.internal.namespace.ProductNamespace;
import com.ibm.ws.repository.resolver.internal.resource.ProductRequirement;

/**
 * Tests on the {@link ProductRequirement} class.
 */
public class ProductRequirementTest {

    /**
     * This test makes sure a filter is created when all the attributes are set in an applies to
     * 
     * @throws InvalidSyntaxException
     */
    @Test
    public void testAllAttributesInAppliesTo() throws InvalidSyntaxException {
        String appliesTo = "com.ibm.websphere.appserver; productVersion=8.5.next.beta; productInstallType=Archive; productEdition=\"BASE,DEVELOPERS,EXPRESS,ND\"; productLicenseType=ILAN";
        ProductRequirement testObject = new ProductRequirement(appliesTo);
        String filterString = testObject.getDirectives().get(ProductNamespace.REQUIREMENT_FILTER_DIRECTIVE);
        assertNotNull("A filter string should have been created", filterString);

        // Make sure all the options are set in the filter string
        assertTrue("There should be an option for " + ProductNamespace.CAPABILITY_EDITION_ATTRIBUTE + " in the filter string but it is: " + filterString,
                   filterString.contains(ProductNamespace.CAPABILITY_EDITION_ATTRIBUTE));
        assertTrue("There should be an option for " + ProductNamespace.CAPABILITY_INSTALL_TYPE_ATTRIBUTE + " in the filter string but it is: " + filterString,
                   filterString.contains(ProductNamespace.CAPABILITY_INSTALL_TYPE_ATTRIBUTE));
        assertTrue("There should be an option for " + ProductNamespace.CAPABILITY_LICENSE_TYPE_ATTRIBUTE + " in the filter string but it is: " + filterString,
                   filterString.contains(ProductNamespace.CAPABILITY_LICENSE_TYPE_ATTRIBUTE));
        assertTrue("There should be an option for " + ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE + " in the filter string but it is: " + filterString,
                   filterString.contains(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE));
        assertTrue("There should be an option for " + ProductNamespace.CAPABILITY_VERSION_ATTRIBUTE + " in the filter string but it is: " + filterString,
                   filterString.contains(ProductNamespace.CAPABILITY_VERSION_ATTRIBUTE));

        // Life is too short to try to do an equality check and make sure it is definitely the right thing so just match it against some attributes
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(ProductNamespace.CAPABILITY_EDITION_ATTRIBUTE, "BASE");
        attributes.put(ProductNamespace.CAPABILITY_INSTALL_TYPE_ATTRIBUTE, "Archive");
        attributes.put(ProductNamespace.CAPABILITY_LICENSE_TYPE_ATTRIBUTE, "ILAN");
        attributes.put(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE, "com.ibm.websphere.appserver");
        attributes.put(ProductNamespace.CAPABILITY_VERSION_ATTRIBUTE, "8.5.next.beta");

        Filter filter = FrameworkUtil.createFilter(filterString);
        assertTrue("The attributes:\n" + attributes + "\n should match the filter: " + filter, filter.matches(attributes));

        // Try with a different edition should be an or
        attributes.put(ProductNamespace.CAPABILITY_EDITION_ATTRIBUTE, "DEVELOPERS");
        assertTrue("The attributes:\n" + attributes + "\n should match the filter: " + filter, filter.matches(attributes));

        // Now try with an invalid value for something
        attributes.put(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE, "com.ibm.wibble");
        assertFalse("The attributes:\n" + attributes + "\n should not match the filter because the ID is wrong: " + filter, filter.matches(attributes));

        List<String> editions = Arrays.asList("BASE,DEVELOPERS,EXPRESS,ND".split(","));
        assertEquals("The product information should be generated",
                     Collections.singletonList(new ProductRequirementInformation("[8.5.next.beta, 8.5.next.beta]", "com.ibm.websphere.appserver", "Archive", "ILAN", editions)),
                     testObject.getProductInformation());
    }

    /**
     * This test makes sure a filter is created when there is more than one product in the applies to string. It also tests that the other attributes don't mess things up when they
     * are missing as they are all optional.
     * 
     * @throws InvalidSyntaxException
     */
    @Test
    public void testMultipleProductsAppliesTo() throws InvalidSyntaxException {
        String appliesTo = "com.ibm.websphere.appserver, com.ibm.notappserver";
        ProductRequirement testObject = new ProductRequirement(appliesTo);
        String filterString = testObject.getDirectives().get(ProductNamespace.REQUIREMENT_FILTER_DIRECTIVE);
        assertNotNull("A filter string should have been created", filterString);

        // Make sure only the id is set
        assertFalse("There should not be an option for " + ProductNamespace.CAPABILITY_EDITION_ATTRIBUTE + " in the filter string but it is: " + filterString,
                    filterString.contains(ProductNamespace.CAPABILITY_EDITION_ATTRIBUTE));
        assertFalse("There should not be an option for " + ProductNamespace.CAPABILITY_INSTALL_TYPE_ATTRIBUTE + " in the filter string but it is: " + filterString,
                    filterString.contains(ProductNamespace.CAPABILITY_INSTALL_TYPE_ATTRIBUTE));
        assertFalse("There should not be an option for " + ProductNamespace.CAPABILITY_LICENSE_TYPE_ATTRIBUTE + " in the filter string but it is: " + filterString,
                    filterString.contains(ProductNamespace.CAPABILITY_LICENSE_TYPE_ATTRIBUTE));
        assertTrue("There should be an option for " + ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE + " in the filter string but it is: " + filterString,
                   filterString.contains(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE));
        assertFalse("There should not be an option for " + ProductNamespace.CAPABILITY_VERSION_ATTRIBUTE + " in the filter string but it is: " + filterString,
                    filterString.contains(ProductNamespace.CAPABILITY_VERSION_ATTRIBUTE));

        // Now make sure it matches on valid strings and doesn't on invalid ones
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE, "com.ibm.websphere.appserver");
        Filter filter = FrameworkUtil.createFilter(filterString);
        assertTrue("The attributes:\n" + attributes + "\n should match the filter: " + filter, filter.matches(attributes));

        attributes.put(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE, "com.ibm.notappserver");
        assertTrue("The attributes:\n" + attributes + "\n should match the filter: " + filter, filter.matches(attributes));

        attributes.put(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE, "com.ibm.wibble");
        assertFalse("The attributes:\n" + attributes + "\n should match the filter: " + filter, filter.matches(attributes));

        List<ProductRequirementInformation> products = new ArrayList<ProductRequirementInformation>();
        products.add(new ProductRequirementInformation(null, "com.ibm.websphere.appserver", null, null, Collections.<String> emptyList()));
        products.add(new ProductRequirementInformation(null, "com.ibm.notappserver", null, null, Collections.<String> emptyList()));
        assertEquals("The product information should be generated", products, testObject.getProductInformation());
    }

    /**
     * This test makes sure a filter is created correctly when the applies to version is a range
     * 
     * @throws InvalidSyntaxException
     */
    @Test
    public void testAppliesToVersionRange() throws InvalidSyntaxException {
        String appliesTo = "com.ibm.websphere.appserver; productVersion=8.5.5.0+";
        ProductRequirement testObject = new ProductRequirement(appliesTo);
        String filterString = testObject.getDirectives().get(ProductNamespace.REQUIREMENT_FILTER_DIRECTIVE);
        assertNotNull("A filter string should have been created", filterString);

        // Make sure all the options are set in the filter string
        assertTrue("There should be an option for " + ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE + " in the filter string but it is: " + filterString,
                   filterString.contains(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE));
        assertTrue("There should be an option for " + ProductNamespace.CAPABILITY_VERSION_ATTRIBUTE + " in the filter string but it is: " + filterString,
                   filterString.contains(ProductNamespace.CAPABILITY_VERSION_ATTRIBUTE));

        // Life is too short to try to do an equality check and make sure it is definitely the right thing so just match it against some attributes
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE, "com.ibm.websphere.appserver");
        attributes.put(ProductNamespace.CAPABILITY_VERSION_ATTRIBUTE, "8.5.5.0");

        Filter filter = FrameworkUtil.createFilter(filterString);
        assertTrue("The attributes:\n" + attributes + "\n should match the filter: " + filter, filter.matches(attributes));

        // Try with a higher version
        attributes.put(ProductNamespace.CAPABILITY_VERSION_ATTRIBUTE, "8.5.5.1");
        assertTrue("The attributes:\n" + attributes + "\n should match the filter: " + filter, filter.matches(attributes));

        // Now try with an invalid version
        attributes.put(ProductNamespace.CAPABILITY_VERSION_ATTRIBUTE, "8.4.9.9");
        assertFalse("The attributes:\n" + attributes + "\n should not match the filter because the version is wrong: " + filter, filter.matches(attributes));

        assertEquals("The product information should be generated",
                     Collections.singletonList(new ProductRequirementInformation("8.5.5.0", "com.ibm.websphere.appserver", null, null, Collections.<String> emptyList())),
                     testObject.getProductInformation());
    }

}

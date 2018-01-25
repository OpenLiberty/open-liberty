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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Requirement;

import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants;
import com.ibm.ws.repository.resolver.internal.namespace.ProductNamespace;
import com.ibm.ws.repository.resolver.internal.resource.FeatureResource;
import com.ibm.ws.repository.resolver.internal.resource.RequirementImpl;
import com.ibm.ws.repository.resolver.internal.resource.SampleResource;

/**
 * Tests for the {@link SampleResource} class.
 */
public class SampleResourceTest {

    /**
     * Test to make sure that {@link SampleResource#createInstance(com.ibm.ws.repository.resources.internal.SampleResourceImpl)} creates an instance with the right capability.
     */
    @Test
    public void testCapabilitiesOnMassiveSample() {
        // Build the input data
        String shortName = "notTheShortestOfShortNames";
        com.ibm.ws.repository.resources.internal.SampleResourceImpl sampleResource = new com.ibm.ws.repository.resources.internal.SampleResourceImpl(null);
        sampleResource.setShortName(shortName);

        // Run test
        SampleResource testObject = SampleResource.createInstance(sampleResource);

        // Make sure the capabilities are correct
        assertEquals("A sample should only provide one capability", 1, testObject.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).size());
        Map<String, Object> capabilityAttributes = testObject.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0).getAttributes();
        assertEquals("The capability identity should be set to the short name", shortName,
                     capabilityAttributes.get(IdentityNamespace.IDENTITY_NAMESPACE));
        assertEquals("The capability short name should match the one supplied on the sample", shortName,
                     capabilityAttributes.get(InstallableEntityIdentityConstants.CAPABILITY_SHORT_NAME_ATTRIBUTE));
        assertEquals("The capability lower short name should match the one supplied on the sample", shortName.toLowerCase(),
                     capabilityAttributes.get(InstallableEntityIdentityConstants.CAPABILITY_LOWER_CASE_SHORT_NAME_ATTRIBUTE));
        assertNull("The capability version should be null", capabilityAttributes.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
        assertEquals("The capability type should be sample", InstallableEntityIdentityConstants.TYPE_SAMPLE,
                     capabilityAttributes.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
    }

    /**
     * Test to make sure that {@link FeatureResource#createInstance(SampleResource)} creates an instance with the right requirements.
     * 
     * @throws InvalidSyntaxException
     */
    @Test
    public void testRequirementsOnMassiveFeature() throws InvalidSyntaxException {
        // Build the input data
        String requireFeature1 = "RequiredFeature1";
        String requireFeature2 = "RequiredFeature2";
        List<String> requiredFeatures = new ArrayList<String>();
        requiredFeatures.add(requireFeature1);
        requiredFeatures.add(requireFeature2);
        String requireProductId = "com.ibm.ws";
        com.ibm.ws.repository.resources.internal.SampleResourceImpl sampleResource = new com.ibm.ws.repository.resources.internal.SampleResourceImpl(null);
        sampleResource.setShortName("sample");
        sampleResource.setRequireFeature(requiredFeatures);
        sampleResource.setAppliesTo(requireProductId);

        // Run test
        SampleResource testObject = SampleResource.createInstance(sampleResource);
        Collection<Map<String, String>> featuresAttributes = new HashSet<Map<String, String>>();
        for (String requiredFeature : requiredFeatures) {
            Map<String, String> featureAttributes = new HashMap<String, String>();
            featureAttributes.put(IdentityNamespace.IDENTITY_NAMESPACE, requiredFeature);
            featureAttributes.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, InstallableEntityIdentityConstants.TYPE_FEATURE);
            featuresAttributes.add(featureAttributes);
        }
        assertEquals("A sample should provide one requirement for the product and one for each of the required features",
                     1 + requiredFeatures.size(),
                     testObject.getRequirements(null).size());
        assertEquals("A sample should provide one installable entity requirement for each of its required features", requiredFeatures.size(),
                     testObject.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE).size());
        assertEquals("A sample should provide one product requirements", 1,
                     testObject.getRequirements(ProductNamespace.PRODUCT_NAMESPACE).size());
        List<Requirement> featureRequirements = testObject.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE);
        for (Requirement requirement : featureRequirements) {
            Iterator<Map<String, String>> attributeIterator = featuresAttributes.iterator();
            boolean found = false;
            Filter filter = FrameworkUtil.createFilter(requirement.getDirectives().get(IdentityNamespace.REQUIREMENT_FILTER_DIRECTIVE));
            while (attributeIterator.hasNext()) {
                Map<String, String> attributes = attributeIterator.next();
                if (filter.matches(attributes)) {
                    attributeIterator.remove();
                    assertEquals("The name should match the identity of the feature", attributes.get(IdentityNamespace.IDENTITY_NAMESPACE),
                                 ((RequirementImpl) requirement).getName());
                    found = true;
                    break;
                }
            }
            assertTrue("A requirement had a filter that did not match any of the expected required entities, the filter was: " + filter, found);
        }
        assertTrue("All of the expected requirements should have been matched", featuresAttributes.isEmpty());
        List<Requirement> productRequirements = testObject.getRequirements(ProductNamespace.PRODUCT_NAMESPACE);
        Filter productFilter = FrameworkUtil.createFilter(productRequirements.get(0).getDirectives().get(IdentityNamespace.REQUIREMENT_FILTER_DIRECTIVE));
        Map<String, Object> productAttributes = new HashMap<String, Object>();
        productAttributes.put(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE, requireProductId);
        assertTrue("The filter should match a product with the right ID, filter is: " + productFilter, productFilter.matches(productAttributes));
    }

}

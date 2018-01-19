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
package com.ibm.ws.repository.resolver.internal.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.junit.Test;
import org.osgi.resource.Namespace;

import com.ibm.ws.repository.resolver.internal.resource.GenericMetadataRequirement;

public class GenericMetadataRequirementTest {

    /**
     * Tests that the {@link GenericMetadataRequirement} gets all its info from a supplied {@link GenericMetadata} object.
     */
    @Test
    public void test() {
        String namespace = "test.namespace";
        GenericMetadata metadata = new GenericMetadata(namespace);
        metadata.getDirectives().put("d-key", "d:ream");
        metadata.getAttributes().put("a-key", "a-door");
        GenericMetadataRequirement testObject = new GenericMetadataRequirement(metadata);
        assertEquals("The directives should match those on the metadata", Collections.singletonMap("d-key", "d:ream"), testObject.getDirectives());
        assertEquals("The attributes should match those on the metadata", Collections.singletonMap("a-key", "a-door"), testObject.getAttributes());
        assertEquals("The namespace should match those on the metadata", namespace, testObject.getNamespace());
    }

    /**
     * Tests the name doesn't blow up when there is no filter
     */
    @Test
    public void testGetNameNoFilter() {
        GenericMetadata metadata = new GenericMetadata(null);
        GenericMetadataRequirement testObject = new GenericMetadataRequirement(metadata);
        assertNull("The name should be null when no filter is set", testObject.getName());
    }

    /**
     * Tests the name is set when there is a filter without an identity attribute
     */
    @Test
    public void testGetNameFilterNoIdentity() {
        GenericMetadata metadata = new GenericMetadata(null);
        String filterString = "(foo=bar)";
        metadata.getDirectives().put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filterString);
        GenericMetadataRequirement testObject = new GenericMetadataRequirement(metadata);
        assertEquals("The name should be set to the filter string when it doesn't contain an identity", filterString, testObject.getName());
    }

    /** Tests if you have a normal filter from an auto feature then the name is set to the identity of the required feature */
    @Test
    public void testGetNameGoodFilter() {
        GenericMetadata metadata = new GenericMetadata(null);
        String symbolicName = "symbolicNameOfFeature";
        metadata.getDirectives().put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(&(type=osgi.subsystem.feature)(osgi.identity=" + symbolicName + "))");
        GenericMetadataRequirement testObject = new GenericMetadataRequirement(metadata);
        assertEquals("The name should be set to the symbolic name of the identity in the filter", symbolicName, testObject.getName());
    }
}

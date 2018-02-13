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
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants;
import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants.NameAttributes;
import com.ibm.ws.repository.resolver.internal.resource.LpmResource;
import com.ibm.ws.repository.resolver.internal.resource.RequirementImpl;

/**
 * Tests for the {@link LpmResource} class
 */
public class LpmResourceTest {

    /**
     * Test to make sure if just a symbolic name is entered then the correct capability is created.
     * 
     * @throws InvalidSyntaxException
     */
    @Test
    public void testSymbolicNameOnly() throws InvalidSyntaxException {
        String symbolicName = "symbolic.name.value";
        LpmResource resource = LpmResource.createInstance(symbolicName, NameAttributes.SYMBOLIC_NAME, InstallableEntityIdentityConstants.TYPE_FEATURE);
        List<Requirement> requirements = resource.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE);
        assertEquals("There should only be a single requirement created", 1, requirements.size());
        assertEquals("The name of the requirement should match the symbolic name", symbolicName, ((RequirementImpl) requirements.get(0)).getName());
        Filter filter = FrameworkUtil.createFilter(requirements.get(0).getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(IdentityNamespace.IDENTITY_NAMESPACE, symbolicName);
        attributes.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, InstallableEntityIdentityConstants.TYPE_FEATURE);
        attributes.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version("1.0.0.0"));
        assertTrue("The filter should match attributes with the symbolic name set", filter.matches(attributes));

        attributes.put(IdentityNamespace.IDENTITY_NAMESPACE, "other");
        assertFalse("The filter should not match attributes with the symbolic name set to a different value", filter.matches(attributes));
    }

    /**
     * Test to make sure if just a short name is entered then the correct capability is created.
     * 
     * @throws InvalidSyntaxException
     */
    @Test
    public void testShortNameOnly() throws InvalidSyntaxException {
        String shortName = "shortNameValue";
        LpmResource resource = LpmResource.createInstance(shortName, NameAttributes.SHORT_NAME, InstallableEntityIdentityConstants.TYPE_FEATURE);
        List<Requirement> requirements = resource.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE);
        assertEquals("There should only be a single requirement created", 1, requirements.size());
        assertEquals("The name of the requirement should match the short name", shortName, ((RequirementImpl) requirements.get(0)).getName());
        Filter filter = FrameworkUtil.createFilter(requirements.get(0).getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(InstallableEntityIdentityConstants.CAPABILITY_SHORT_NAME_ATTRIBUTE, shortName);
        attributes.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, InstallableEntityIdentityConstants.TYPE_FEATURE);
        assertTrue("The filter should match attributes with the short name set", filter.matches(attributes));

        attributes.put(InstallableEntityIdentityConstants.CAPABILITY_SHORT_NAME_ATTRIBUTE, "other");
        assertFalse("The filter should not match attributes with the short name set to a different value", filter.matches(attributes));
    }

    /**
     * Test to make sure if just a lower case short name is entered then the correct capability is created.
     * 
     * @throws InvalidSyntaxException
     */
    @Test
    public void testLowerCaseShortNameOnly() throws InvalidSyntaxException {
        String shortName = "shortNameValue";
        LpmResource resource = LpmResource.createInstance(shortName, NameAttributes.CASE_INSENSITIVE_SHORT_NAME, InstallableEntityIdentityConstants.TYPE_SAMPLE);
        List<Requirement> requirements = resource.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE);
        assertEquals("There should only be a single requirement created", 1, requirements.size());
        assertEquals("The name of the requirement should match the short name", shortName, ((RequirementImpl) requirements.get(0)).getName());
        Filter filter = FrameworkUtil.createFilter(requirements.get(0).getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(InstallableEntityIdentityConstants.CAPABILITY_LOWER_CASE_SHORT_NAME_ATTRIBUTE, shortName.toLowerCase());
        attributes.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, InstallableEntityIdentityConstants.TYPE_SAMPLE);
        assertTrue("The filter should match attributes with the lower case short name set", filter.matches(attributes));

        attributes.put(InstallableEntityIdentityConstants.CAPABILITY_LOWER_CASE_SHORT_NAME_ATTRIBUTE, "other");
        assertFalse("The filter should not match attributes with the lower case short name set to a different value", filter.matches(attributes));

        attributes.put(InstallableEntityIdentityConstants.CAPABILITY_LOWER_CASE_SHORT_NAME_ATTRIBUTE, shortName);
        assertFalse("The filter should not match attributes with the lower case short name set to a different case", filter.matches(attributes));
    }

    /**
     * Test to make sure if a symbolic name and version is entered then the correct capability is created.
     * 
     * @throws InvalidSyntaxException
     */
    @Test
    public void testSymbolicNameAndVersionOnly() throws InvalidSyntaxException {
        String symbolicName = "symbolic.name.value";
        String version = "1.0.0.0";
        LpmResource resource = LpmResource.createInstance(symbolicName + "/" + version, NameAttributes.SYMBOLIC_NAME, InstallableEntityIdentityConstants.TYPE_FEATURE);
        List<Requirement> requirements = resource.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE);
        assertEquals("There should only be a single requirement created", 1, requirements.size());
        Filter filter = FrameworkUtil.createFilter(requirements.get(0).getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(IdentityNamespace.IDENTITY_NAMESPACE, symbolicName);
        attributes.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, InstallableEntityIdentityConstants.TYPE_FEATURE);
        attributes.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version(version));
        assertTrue("The filter should match attributes with the symbolic name and version set", filter.matches(attributes));

        attributes.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version("2.0.0.0"));
        assertFalse("The filter should not match attributes with the version set to a different value", filter.matches(attributes));
    }

    /**
     * Test to make sure the LpmResource doesn't match a iFix ever.
     * 
     * @throws InvalidSyntaxException
     */
    @Test
    public void testIFixesNotMatched() throws InvalidSyntaxException {
        String symbolicName = "symbolic.name.value";
        LpmResource resource = LpmResource.createInstance(symbolicName, NameAttributes.SYMBOLIC_NAME, InstallableEntityIdentityConstants.TYPE_FEATURE);
        List<Requirement> requirements = resource.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE);
        assertEquals("There should only be a single requirement created", 1, requirements.size());
        Filter filter = FrameworkUtil.createFilter(requirements.get(0).getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(IdentityNamespace.IDENTITY_NAMESPACE, symbolicName);
        attributes.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, InstallableEntityIdentityConstants.TYPE_IFIX);
        assertFalse("The filter should not match attributes with type filter", filter.matches(attributes));
    }

}

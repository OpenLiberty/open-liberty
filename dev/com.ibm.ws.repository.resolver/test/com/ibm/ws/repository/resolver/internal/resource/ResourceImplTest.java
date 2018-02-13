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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import com.ibm.ws.repository.resolver.internal.resource.CapabilityImpl;
import com.ibm.ws.repository.resolver.internal.resource.RequirementImpl;
import com.ibm.ws.repository.resolver.internal.resource.ResourceImpl;

/**
 * Tests for {@link ResourceImpl}.
 */
public class ResourceImplTest {

    /**
     * A test to make sure that you can get requirements out of a ResourceImpl
     */
    @Test
    public void testGetRequirements() {
        // Add two requirements in different namespaces and make sure that you can do a get all or get single or get none
        Mockery mockery = new Mockery();
        final String requirement1Namespace = "namespace1";
        final String requirement2Namespace = "namespace2";
        final Requirement requirement1 = mockery.mock(Requirement.class, "requirement1");
        final Requirement requirement2 = mockery.mock(Requirement.class, "requirement2");
        mockery.checking(new Expectations() {
            {
                allowing(requirement1).getNamespace();
                will(returnValue(requirement1Namespace));
                allowing(requirement2).getNamespace();
                will(returnValue(requirement2Namespace));
            }
        });
        List<Requirement> requirements = new ArrayList<Requirement>();
        requirements.add(requirement1);
        requirements.add(requirement2);
        List<Capability> capabilities = Collections.emptyList();
        ResourceImpl testObject = new ResourceImpl(capabilities, requirements, ResourceImpl.LOCATION_INSTALL, null);

        List<Requirement> namespace1Requirements = testObject.getRequirements(requirement1Namespace);
        assertEquals("There should be one requirement in each namespace", 1, namespace1Requirements.size());
        assertEquals("The requirement in namespace 1 should be the one with namespace 1", requirement1, namespace1Requirements.get(0));
        try {
            namespace1Requirements.add(requirement2);
            fail("The spec dictates that the list of requirements returned from getRequirements is immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        List<Requirement> namespace2Requirements = testObject.getRequirements(requirement2Namespace);
        assertEquals("There should be one requirement in each namespace", 1, namespace2Requirements.size());
        assertEquals("The requirement in namespace 2 should be the one with namespace 2", requirement2, namespace2Requirements.get(0));
        try {
            namespace2Requirements.add(requirement1);
            fail("The spec dictates that the list of requirements returned from getRequirements is immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        // Try the null case - should return all
        List<Requirement> allRequirements = testObject.getRequirements(null);
        assertEquals("Passing null into the getRequirements should return all", requirements, allRequirements);
        try {
            allRequirements.add(requirement1);
            fail("The spec dictates that the list of requirements returned from getRequirements is immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        // Finally try with an unknown namespace
        List<Requirement> noRequirements = testObject.getRequirements("unknown");
        assertNotNull("Passing in an unknown namespace should return an empty list not null", noRequirements);
        assertTrue("Passing in an unknown namespace should return an empty list", noRequirements.isEmpty());
        try {
            noRequirements.add(requirement1);
            fail("The spec dictates that the list of requirements returned from getRequirements is immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        // Make sure nothing untoward happened in the mocking
        mockery.assertIsSatisfied();
    }

    /**
     * A test to make sure that you can get capabilities out of a ResourceImpl
     */
    @Test
    public void testGetCapabilities() {
        // Add two capabilities in different namespaces and make sure that you can do a get all or get single or get none
        Mockery mockery = new Mockery();
        final String capability1Namespace = "namespace1";
        final String capability2Namespace = "namespace2";
        final Capability capability1 = mockery.mock(Capability.class, "capability1");
        final Capability capability2 = mockery.mock(Capability.class, "capability2");
        mockery.checking(new Expectations() {
            {
                allowing(capability1).getNamespace();
                will(returnValue(capability1Namespace));
                allowing(capability2).getNamespace();
                will(returnValue(capability2Namespace));
            }
        });
        List<Requirement> requirements = null;
        List<Capability> capabilities = new ArrayList<Capability>();
        capabilities.add(capability1);
        capabilities.add(capability2);
        ResourceImpl testObject = new ResourceImpl(capabilities, requirements, ResourceImpl.LOCATION_INSTALL, null);

        List<Capability> namespace1Capabilities = testObject.getCapabilities(capability1Namespace);
        assertEquals("There should be one capability in each namespace", 1, namespace1Capabilities.size());
        assertEquals("The capability in namespace 1 should be the one with namespace 1", capability1, namespace1Capabilities.get(0));
        try {
            namespace1Capabilities.add(capability2);
            fail("The spec dictates that the list of capabilities returned from getCapabilities is immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        List<Capability> namespace2Capabilities = testObject.getCapabilities(capability2Namespace);
        assertEquals("There should be one capability in each namespace", 1, namespace2Capabilities.size());
        assertEquals("The capability in namespace 2 should be the one with namespace 2", capability2, namespace2Capabilities.get(0));
        try {
            namespace2Capabilities.add(capability1);
            fail("The spec dictates that the list of capabilities returned from getCapabilities is immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        // Try the null case - should return all
        List<Capability> allCapabilities = testObject.getCapabilities(null);
        assertEquals("Passing null into the getCapabilities should return all", capabilities, allCapabilities);
        try {
            allCapabilities.add(capability1);
            fail("The spec dictates that the list of capabilities returned from getCapabilities is immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        // Finally try with an unknown namespace
        List<Capability> noCapabilities = testObject.getCapabilities("unknown");
        assertNotNull("Passing in an unknown namespace should return an empty list not null", noCapabilities);
        assertTrue("Passing in an unknown namespace should return an empty list", noCapabilities.isEmpty());
        try {
            noCapabilities.add(capability1);
            fail("The spec dictates that the list of capabilities returned from getCapabilities is immutable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        // Make sure nothing untoward happened in the mocking
        mockery.assertIsSatisfied();
    }

    /**
     * Test that when two resources have equal capabilities and requirements in the same install location they are equal
     */
    @Test
    public void testEquals() {
        Mockery mockery = new Mockery();
        final String namespace = "namespace1";
        final Capability capability = mockery.mock(Capability.class);
        final Requirement requirement = mockery.mock(Requirement.class);
        mockery.checking(new Expectations() {
            {
                allowing(capability).getNamespace();
                will(returnValue(namespace));
                allowing(requirement).getNamespace();
                will(returnValue(namespace));
            }
        });
        List<Requirement> requirements = new ArrayList<Requirement>();
        requirements.add(requirement);
        List<Capability> capabilities = new ArrayList<Capability>();
        capabilities.add(capability);
        ResourceImpl testObject1 = new ResourceImpl(capabilities, requirements, ResourceImpl.LOCATION_INSTALL, null);
        ResourceImpl testObject2 = new ResourceImpl(capabilities, requirements, ResourceImpl.LOCATION_INSTALL, null);

        assertTrue("Test object 1 should equal test object 2", testObject1.equals(testObject2));
        assertTrue("Test object 2 should equal test object 1", testObject2.equals(testObject1));
        assertEquals("The hash codes on the two test objects should match", testObject1.hashCode(), testObject2.hashCode());

        // Make sure nothing untoward happened in the mocking
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that if you have an instance of {@link CapabilityImpl} or {@link RequirementImpl} in the list of capabilities or requirements then it doesn't cause a
     * stack overflow as they both have relationships to each other that the equals method must check.
     */
    @Test
    public void testEqualsWithCapabilityImpl() {
        RequirementImpl requirement = new BlankRequirementImpl();
        List<Requirement> requirements = Collections.singletonList((Requirement) requirement);
        CapabilityImpl capability1 = new BlankCapabilityImpl();
        List<Capability> capabilities = Collections.singletonList((Capability) capability1);
        ResourceImpl testObject1 = new ResourceImpl(capabilities, requirements, ResourceImpl.LOCATION_INSTALL, null);

        // For the second object use exactly the same requirement list but a second capability list that should be equal
        CapabilityImpl capability2 = new BlankCapabilityImpl();
        List<Capability> capabilities2 = Collections.singletonList((Capability) capability2);
        ResourceImpl testObject2 = new ResourceImpl(capabilities2, requirements, ResourceImpl.LOCATION_INSTALL, null);

        assertTrue("Test object 1 should equal test object 2", testObject1.equals(testObject2));
        assertTrue("Test object 2 should equal test object 1", testObject2.equals(testObject1));
        assertEquals("The hash codes on the two test objects should match", testObject1.hashCode(), testObject2.hashCode());

        // Also check that starting with an equals call on the capability objects doesn't blow up the resource impl
        assertTrue("capability 1 should equal capability 2", capability1.equals(capability2));
        assertTrue("capability 2 should equal capability 1", capability2.equals(capability1));
    }

    /**
     * This test makes sure that if you have two different capabilities on two resources then equals returns false.
     */
    @Test
    public void testEqualsWithDifferentCapabilities() {
        RequirementImpl requirement = new BlankRequirementImpl();
        List<Requirement> requirements = Collections.singletonList((Requirement) requirement);
        CapabilityImpl capability1 = new BlankCapabilityImpl();
        List<Capability> capabilities = Collections.singletonList((Capability) capability1);
        ResourceImpl testObject1 = new ResourceImpl(capabilities, requirements, ResourceImpl.LOCATION_INSTALL, null);

        // For the second object use exactly the same requirement list but a second capability list that should be equal
        CapabilityImpl capability2 = new BlankCapabilityImpl() {
            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.repository.resolver.internal.resource.ResourceImplTest.BlankCapabilityImpl#getDirectives()
             */
            @Override
            public Map<String, String> getDirectives() {
                return Collections.singletonMap("foo", "bar");
            }
        };
        List<Capability> capabilities2 = Collections.singletonList((Capability) capability2);
        ResourceImpl testObject2 = new ResourceImpl(capabilities2, requirements, ResourceImpl.LOCATION_INSTALL, null);

        assertFalse("Test object 1 should not equal test object 2", testObject1.equals(testObject2));
        assertFalse("Test object 2 should not equal test object 1", testObject2.equals(testObject1));
    }

    /**
     * This test makes sure that if you have two different requirements on two resources then equals returns false.
     */
    @Test
    public void testEqualsWithDifferentRequirements() {
        RequirementImpl requirement = new BlankRequirementImpl();
        List<Requirement> requirements = Collections.singletonList((Requirement) requirement);
        CapabilityImpl capability1 = new BlankCapabilityImpl();
        List<Capability> capabilities = Collections.singletonList((Capability) capability1);
        ResourceImpl testObject1 = new ResourceImpl(capabilities, requirements, ResourceImpl.LOCATION_INSTALL, null);

        // For the second object use exactly the same requirement list but a second capability list that should be equal
        RequirementImpl requirement2 = new BlankRequirementImpl() {
            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.repository.resolver.internal.resource.ResourceImplTest.BlankCapabilityImpl#getDirectives()
             */
            @Override
            public Map<String, String> getDirectives() {
                return Collections.singletonMap("foo", "bar");
            }
        };
        List<Requirement> requirements2 = Collections.singletonList((Requirement) requirement2);
        ResourceImpl testObject2 = new ResourceImpl(capabilities, requirements2, ResourceImpl.LOCATION_INSTALL, null);

        assertFalse("Test object 1 should not equal test object 2", testObject1.equals(testObject2));
        assertFalse("Test object 2 should not equal test object 1", testObject2.equals(testObject1));
    }

    /**
     * This test makes sure that if you have two different locations on two resources then equals returns false.
     */
    @Test
    public void testEqualsWithDifferentLocation() {
        RequirementImpl requirement = new BlankRequirementImpl();
        List<Requirement> requirements = Collections.singletonList((Requirement) requirement);
        CapabilityImpl capability1 = new BlankCapabilityImpl();
        List<Capability> capabilities = Collections.singletonList((Capability) capability1);
        ResourceImpl testObject1 = new ResourceImpl(capabilities, requirements, ResourceImpl.LOCATION_INSTALL, null);
        ResourceImpl testObject2 = new ResourceImpl(capabilities, requirements, ResourceImpl.LOCATION_REPOSITORY, null);

        assertFalse("Test object 1 should not equal test object 2", testObject1.equals(testObject2));
        assertFalse("Test object 2 should not equal test object 1", testObject2.equals(testObject1));
    }

    /**
     * This test makes sure that if you have a null location then equals returns false.
     */
    @Test
    public void testEqualsWithNullLocation() {
        RequirementImpl requirement = new BlankRequirementImpl();
        List<Requirement> requirements = Collections.singletonList((Requirement) requirement);
        CapabilityImpl capability1 = new BlankCapabilityImpl();
        List<Capability> capabilities = Collections.singletonList((Capability) capability1);
        ResourceImpl testObject1 = new ResourceImpl(capabilities, requirements, ResourceImpl.LOCATION_INSTALL, null);
        ResourceImpl testObject2 = new ResourceImpl(capabilities, requirements, null, null);

        assertFalse("Test object 1 should not equal test object 2", testObject1.equals(testObject2));
        assertFalse("Test object 2 should not equal test object 1", testObject2.equals(testObject1));
    }

    /**
     * This test makes sure that if you pass in a {@link CapabilityImpl} or {@link RequirementImpl} then the reference to the resource is set on it.
     */
    @Test
    public void testSetResource() {
        RequirementImpl requirement = new BlankRequirementImpl();
        List<Requirement> requirements = Collections.singletonList((Requirement) requirement);
        CapabilityImpl capability = new BlankCapabilityImpl();
        List<Capability> capabilities = Collections.singletonList((Capability) capability);
        ResourceImpl testObject = new ResourceImpl(capabilities, requirements, ResourceImpl.LOCATION_INSTALL, null);
        assertEquals("The reference to the resource should have been set on the requirement", testObject, requirement.getResource());
        assertEquals("The reference to the resource should have been set on the capability", testObject, capability.getResource());
    }

    /**
     * Extension to {@link RequirementImpl} that returns <code>null</code> from all the abstract methods.
     */
    private class BlankRequirementImpl extends RequirementImpl {

        /**
         * @param name
         */
        protected BlankRequirementImpl() {
            super(null);
        }

        @Override
        public String getNamespace() {
            return null;
        }

        @Override
        public Map<String, String> getDirectives() {
            return null;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return null;
        }

    }

    /**
     * Extension to {@link CapabilityImpl} that returns <code>null</code> from all the abstract methods.
     */
    private class BlankCapabilityImpl extends CapabilityImpl {

        @Override
        public String getNamespace() {
            return null;
        }

        @Override
        public Map<String, String> getDirectives() {
            return null;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return null;
        }

    }
}

/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resolver.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.HostedCapability;

import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.SimpleProductDefinition;
import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants;
import com.ibm.ws.repository.resolver.internal.resource.ProductRequirement;
import com.ibm.ws.repository.resolver.internal.resource.ProductResource;

/**
 * Unit tests for {@link RepositoryResolveContext}, there are also FATs in MassiveResolveContextFatTest
 */
public class RepositoryResolveContextUnitTest {

    private static final String DEFAULT_ATTR_NAME = "foo";
    private static final String DEFAULT_ATTR_VALUE = "bar";
    private static final String DEFAULT_NAMESPACE = "defaultNamespace";
    private Mockery mockery = null;

    @Before
    public void createMockery() {
        this.mockery = new Mockery();
    }

    @After
    public void testMockery() {
        this.mockery.assertIsSatisfied();
    }

    /**
     * Test to make sure if there is an installed resource that provides a capability then that is always picked first.
     */
    @Test
    public void testInstalledFirst() {
        // Create a capability in both the repo and the installation that satisfies a requirement
        Requirement requirement = mockRequirement();
        Resource installedResource = mockResourceWithCapability("installed");
        Resource repoResource = mockResourceWithCapability("repo");

        List<Resource> productResources = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, productResources, Collections.singletonList(installedResource), Collections.singletonList(repoResource), null);
        List<Capability> foundCapabilities = testObject.findProviders(requirement);
        assertEquals("There should be one match to the installed capability", 1, foundCapabilities.size());
        assertEquals("The match should be to the installed capability", installedResource.getCapabilities(null).get(0), foundCapabilities.get(0));
    }

    /**
     * Test that requirements and capabilities with different namespaces do not match
     */
    @Test
    public void testDifferentNamespaces() {
        Requirement requirement = mockRequirement();
        Resource installedResource = mockResourceWithCapability("installed", "other");
        List<Resource> empty = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, empty, Collections.singletonList(installedResource), empty, null);
        List<Capability> foundCapabilities = testObject.findProviders(requirement);
        assertEquals("The namespace did not match so no match should be found", 0, foundCapabilities.size());
    }

    /**
     * Test a requirement and capability with a <code>null</code> namespace still match.
     */
    @Test
    public void testNullNamespaces() {
        Requirement requirement = mockRequirement(null);
        Resource installedResource = mockResourceWithCapability("installed", null);

        List<Resource> empty = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, empty, Collections.singletonList(installedResource), empty, null);
        List<Capability> foundCapabilities = testObject.findProviders(requirement);
        assertEquals("There should be one match to the capability with a null namespace", 1, foundCapabilities.size());
        assertEquals("The match should be to the capability with a null namespace", installedResource.getCapabilities(null).get(0), foundCapabilities.get(0));
    }

    /**
     * Test to make sure if there is a product resource that provides a capability then that is always picked first.
     */
    @Test
    public void testProductFirst() {
        // Create a capability in the product, the repo and the installation that satisfies a requirement
        Requirement requirement = mockRequirement();
        Resource productResource = mockResourceWithCapability("product");
        Resource installedResource = mockResourceWithCapability("installed");
        Resource repoResource = mockResourceWithCapability("repo");

        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, Collections.singletonList(productResource), Collections.singletonList(installedResource), Collections.singletonList(repoResource), null);
        List<Capability> foundCapabilities = testObject.findProviders(requirement);
        assertEquals("There should be one match to the product capability", 1, foundCapabilities.size());
        assertEquals("The match should be to the product capability", productResource.getCapabilities(null).get(0), foundCapabilities.get(0));
    }

    /**
     * Test to make sure that if there is a resource from the repo providing a capability then it is selected.
     */
    @Test
    public void testRepo() {
        Requirement requirement = mockRequirement();
        Resource repoResource = mockResourceWithCapability("repo");

        List<Resource> empty = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, empty, empty, Collections.singletonList(repoResource), null);
        List<Capability> foundCapabilities = testObject.findProviders(requirement);
        assertEquals("There should be one match to the repo capability", 1, foundCapabilities.size());
        assertEquals("The match should be to the repo capability", repoResource.getCapabilities(null).get(0), foundCapabilities.get(0));
    }

    /**
     * Test to make sure that if nothing provides a resource then an empty list is returned.
     */
    @Test
    public void testNotFound() {
        Requirement requirement = mockRequirement();
        Resource repoResource = mockResourceWithCapability("repo", DEFAULT_NAMESPACE, "differentValue");

        List<Resource> empty = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, empty, empty, Collections.singletonList(repoResource), null);
        List<Capability> foundCapabilities = testObject.findProviders(requirement);
        assertEquals("There shouldn't be a match as the capability has a different attribute value", 0, foundCapabilities.size());
    }

    /**
     * Test to make sure that is effective returns <code>true</code>.
     */
    @Test
    public void testIsEffective() {
        Requirement requirement = mockRequirement();
        List<Resource> empty = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, empty, empty, empty, null);
        assertTrue("All requirements should always be effective", testObject.isEffective(requirement));
    }

    /**
     * Test to make sure that add hosted capabilities doesn't die horribly.
     */
    @Test
    public void testHostedCapabilities() {
        List<Resource> empty = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, empty, empty, empty, null);
        Capability capability = mockCapability("existing", DEFAULT_NAMESPACE, DEFAULT_ATTR_VALUE);
        List<Capability> capabilities = new ArrayList<Capability>();
        capabilities.add(capability);
        HostedCapability hostedCapability = mockHostedCapability("hosted");
        int index = testObject.insertHostedCapability(capabilities, hostedCapability);
        assertTrue("The index should be set to a valid value", index != -1);
        assertEquals("The capability at the location specified by the index should be the hosted one", hostedCapability, capabilities.get(index));
    }

    /**
     * Test to make sure that get wirings returns something sensible.
     */
    @Test
    public void testGetWirings() {
        List<Resource> empty = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, empty, empty, empty, null);
        assertNotNull("The wirings should not be null", testObject.getWirings());
        assertEquals("The wirings should be empty", 0, testObject.getWirings().size());
    }

    /**
     * Test to make sure that get mandatory and optional resources returns the resources we are looking for.
     */
    @Test
    public void testGetRequirements() {
        List<Resource> empty = Collections.emptyList();
        Collection<Resource> mandatory = Collections.singleton(mockResourceWithCapability("required"));
        Collection<Resource> optional = Collections.singleton(mockResourceWithCapability("optional"));
        RepositoryResolveContext testObject = new RepositoryResolveContext(mandatory, optional, empty, empty, empty, null);
        assertEquals("There should be a single mandatory resource", 1, testObject.getMandatoryResources().size());
        assertEquals("The mandatory resource should be the one supplied to the constructor", mandatory, testObject.getMandatoryResources());
        assertEquals("The optional resource should be the one supplied to the constructor", optional, testObject.getOptionalResources());
    }

    /**
     * Test to make sure that a requirement with an invalid filter doesn't blow up the whole system.
     */
    @Test
    public void testInvalidFilter() {
        Requirement requirement = mockRequirement(DEFAULT_NAMESPACE, "((" + DEFAULT_ATTR_NAME + "=" + DEFAULT_ATTR_VALUE + ")");
        Resource repoResource = mockResourceWithCapability("repo");

        List<Resource> empty = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, empty, empty, Collections.singletonList(repoResource), null);
        List<Capability> foundCapabilities = testObject.findProviders(requirement);
        assertEquals("There should not be a match to the repo capability as the filters requirement had too many \"(\"", 0, foundCapabilities.size());
    }

    @Test
    public void testFiltering() throws Exception {
        final Requirement requirement = mockRequirement();
        final Resource repoResource = mockResourceWithCapability("repo");
        final ResolutionFilter filter = mockery.mock(ResolutionFilter.class);
        mockery.checking(new Expectations() {
            {
                allowing(filter).allowResolution(requirement, repoResource.getCapabilities(null));
                will(returnValue(false));
            }
        });
        List<Resource> empty = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, empty, empty, Collections.singletonList(repoResource), null);
        testObject.addFilter(filter);

        List<Capability> foundCapabilities = testObject.findProviders(requirement);

        assertTrue("The result should have been filtered", foundCapabilities.isEmpty());
    }

    /**
     * Test to make sure an auto feature is loaded from the list of installed features.
     */
    @Test
    public void testAutoFeatureIsLoadedFromInstall() {
        Requirement requirement = mockRequirement();
        requirement.getDirectives().put(IdentityNamespace.REQUIREMENT_CLASSIFIER_DIRECTIVE, InstallableEntityIdentityConstants.CLASSIFIER_AUTO);
        Resource installResource = mockResourceWithCapability("install");

        List<Resource> empty = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, empty, Collections.singletonList(installResource), empty, null);
        List<Capability> foundCapabilities = testObject.findProviders(requirement);
        assertEquals("There should be one match to the repo capability", 1, foundCapabilities.size());
        assertEquals("The match should be to the repo capability", installResource.getCapabilities(null).get(0), foundCapabilities.get(0));
    }

    /**
     * Test to make sure if you tell the resolve context to resolve auto features from the main repository.
     */
    @Test
    public void testAutoFeatureIsLoadedFromRepo() {
        Requirement requirement = mockRequirement();
        requirement.getDirectives().put(IdentityNamespace.REQUIREMENT_CLASSIFIER_DIRECTIVE, InstallableEntityIdentityConstants.CLASSIFIER_AUTO);
        Resource repoResource = mockResourceWithCapability("repo");

        List<Resource> empty = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, empty, empty, Collections.singletonList(repoResource), null);
        List<Capability> foundCapabilities = testObject.findProviders(requirement);
        assertEquals("There should be one match to the repo capability", 1, foundCapabilities.size());
        assertEquals("The match should be to the repo capability", repoResource.getCapabilities(null).get(0), foundCapabilities.get(0));
    }

    /**
     * Test that when a requirement requires a range and the beta is installed then it works ok. Test for defect 176390.
     */
    @Test
    public void testCanFindBetaProduct() {
        // The key thing is that a sample with version 8.5.5.0+ works with a beta product so instead of using mocks actually construct these objects
        ProductRequirement requirement = new ProductRequirement("com.ibm.websphere.appserver; productVersion=8.5.5.0+; productInstallType=Archive");
        Resource product = ProductResource.createInstance(Collections.singleton((ProductDefinition) new SimpleProductDefinition("com.ibm.websphere.appserver", "2015.5.0.0", "Archive", "ILAR", "EARLY_ACCESS")));
        List<Resource> empty = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, Collections.singletonList(product), empty, empty, null);
        List<Capability> foundCapabilities = testObject.findProviders(requirement);
        assertEquals(1, foundCapabilities.size());
        assertEquals(product.getCapabilities(null).get(0), foundCapabilities.get(0));
    }

    /**
     * Similar to {@link #testCanFindBetaProduct()} this is another test for defect 176390 that makes sure if you have a sample that applies to 8.5.5.2+ then it will work with
     * product 8.5.5.10 - if an alphabetic comparison is used this would fail.
     */
    @Test
    public void testCanFindProductWithDifferentQualifier() {
        // The key thing is that a sample with version 8.5.5.2+ works with a 8.5.5.10 product so instead of using mocks actually construct these objects
        ProductRequirement requirement = new ProductRequirement("com.ibm.websphere.appserver; productVersion=8.5.5.2+; productInstallType=Archive");
        Resource product = ProductResource.createInstance(Collections.singleton((ProductDefinition) new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.10", "Archive", "ILAN", "BASE")));
        List<Resource> empty = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, Collections.singletonList(product), empty, empty, null);
        List<Capability> foundCapabilities = testObject.findProviders(requirement);
        assertEquals(1, foundCapabilities.size());
        assertEquals(product.getCapabilities(null).get(0), foundCapabilities.get(0));
    }

    /**
     * The negative case for defect 176390 - make sure if a sample is for 8.5.5.2+ then it won't match on 8.5.5.0.
     */
    @Test
    public void testTooLowProductIsntFound() {
        // The key thing is that a sample with version 8.5.5.2+ works with a product with a lower version so instead of using mocks actually construct these objects
        ProductRequirement requirement = new ProductRequirement("com.ibm.websphere.appserver; productVersion=8.5.5.2+; productInstallType=Archive");
        Resource product = ProductResource.createInstance(Collections.singleton((ProductDefinition) new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.0", "Archive", "ILAN", "BASE")));
        List<Resource> empty = Collections.emptyList();
        RepositoryResolveContext testObject = new RepositoryResolveContext(null, null, Collections.singletonList(product), empty, empty, null);
        List<Capability> foundCapabilities = testObject.findProviders(requirement);
        assertEquals(0, foundCapabilities.size());
    }

    /**
     * Mock a resource with a single capability with an attribute with the name set to {@value #DEFAULT_ATTR_NAME} and the value set to {@value #DEFAULT_ATTR_VALUE}
     *
     * @param name The name of the resource, must not be the same for two calls to this method.
     * @return
     */
    private Resource mockResourceWithCapability(String name) {
        return mockResourceWithCapability(name, DEFAULT_NAMESPACE);
    }

    /**
     * Mock a resource with a single capability with an attribute with the name set to {@value #DEFAULT_ATTR_NAME} and the value set to the supplied value. The namespace
     * will be set on the capability to the supplied value and getCapabilities on the resource with {@value #DEFAULT_NAMESPACE} will be allowed but will return <code>null</code>.
     *
     * @param name
     * @param namespace
     * @param attributeValue
     * @return
     */
    private Resource mockResourceWithCapability(String name, final String namespace, final String attributeValue) {
        final Resource resource = mockery.mock(Resource.class, name + "Resource");
        final Capability capability = mockCapability(name, namespace, attributeValue);
        mockery.checking(new Expectations() {
            {
                allowing(resource).getCapabilities(namespace);
                will(returnValue(Collections.singletonList(capability)));
                if (!DEFAULT_NAMESPACE.equals(namespace)) {
                    allowing(resource).getCapabilities(DEFAULT_NAMESPACE);
                    will(returnValue(Collections.emptyList()));
                }
                allowing(resource).getCapabilities(null);
                will(returnValue(Collections.singletonList(capability)));
                allowing(capability).getResource();
                will(returnValue(resource));
            }
        });
        return resource;
    }

    private Capability mockCapability(final String name, final String namespace, final String attributeValue) {
        final Capability capability = mockery.mock(Capability.class, name + "Capability");
        final Map<String, String> capabilityAttributes = new HashMap<String, String>();
        capabilityAttributes.put(DEFAULT_ATTR_NAME, attributeValue);
        mockery.checking(new Expectations() {
            {
                allowing(capability).getNamespace();
                will(returnValue(namespace));

                allowing(capability).getAttributes();
                will(returnValue(capabilityAttributes));
            }
        });
        return capability;
    }

    /**
     * @param name
     * @return
     */
    private HostedCapability mockHostedCapability(String name) {
        final HostedCapability capability = mockery.mock(HostedCapability.class, name + "Capability");
        final Map<String, String> capabilityAttributes = new HashMap<String, String>();
        capabilityAttributes.put(DEFAULT_ATTR_NAME, DEFAULT_ATTR_VALUE);
        mockery.checking(new Expectations() {
            {
                allowing(capability).getNamespace();
                will(returnValue(DEFAULT_NAMESPACE));

                allowing(capability).getAttributes();
                will(returnValue(capabilityAttributes));
            }
        });
        return capability;
    }

    /**
     * Mock a resource with a single capability with an attribute with the name set to {@value #DEFAULT_ATTR_NAME} and the value set to {@value #DEFAULT_ATTR_VALUE}. The namespace
     * will be set on the capability to the supplied value and getCapabilities on the resource with {@value #DEFAULT_NAMESPACE} will be allowed but will return <code>null</code>.
     *
     * @param name The name of the resource, must not be the same for two calls to this method.
     * @return
     */
    private Resource mockResourceWithCapability(String name, final String namespace) {
        return mockResourceWithCapability(name, namespace, DEFAULT_ATTR_VALUE);
    }

    /**
     * Creates a requirement with the filter set to ({@value #DEFAULT_ATTR_NAME}={@value #DEFAULT_ATTR_VALUE}) and the namespace set to the supplied value
     *
     * @param namespace
     * @return
     */
    private Requirement mockRequirement(final String namespace, final String filter) {
        final Requirement requirement = mockery.mock(Requirement.class);
        final Map<String, Object> requirementDirectives = new HashMap<String, Object>();
        requirementDirectives.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
        mockery.checking(new Expectations() {
            {
                allowing(requirement).getNamespace();
                will(returnValue(namespace));

                allowing(requirement).getDirectives();
                will(returnValue(requirementDirectives));
            }
        });
        return requirement;
    }

    /**
     * Creates a requirement with the filter set to ({@value #DEFAULT_ATTR_NAME}={@value #DEFAULT_ATTR_VALUE}) and the namespace set to the supplied value
     *
     * @param namespace
     * @return
     */
    private Requirement mockRequirement(final String namespace) {
        return mockRequirement(namespace, "(" + DEFAULT_ATTR_NAME + "=" + DEFAULT_ATTR_VALUE + ")");
    }

    /**
     * Creates a requirement with the filter set to ({@value #DEFAULT_ATTR_NAME}={@value #DEFAULT_ATTR_VALUE}) and the namespace set to {@value #DEFAULT_NAMESPACE}
     *
     * @return
     */
    private Requirement mockRequirement() {
        return mockRequirement(DEFAULT_NAMESPACE);
    }

}

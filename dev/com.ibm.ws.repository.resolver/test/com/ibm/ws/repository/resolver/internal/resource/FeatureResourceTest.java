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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Requirement;

import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.resolver.ResolverTestUtils;
import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants;
import com.ibm.ws.repository.resolver.internal.namespace.ProductNamespace;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;

/**
 * Tests for the {@link FeatureResource} class.
 */
public class FeatureResourceTest {

    /**
     * Test to make sure that the {@link FeatureResource#createInstance(ProvisioningFeatureDefinition)} method sets up a capability on the feature.
     */
    @Test
    public void testCapabilitiesOnInstalledFeature() {
        Mockery mockery = new Mockery();
        final String provideFeature = "Feature";
        final Version version = new Version("1.0.0.201310090819");
        final String shortName = "shortName";
        final ProvisioningFeatureDefinition featureDefinition = ResolverTestUtils.mockSimpleFeatureDefinition(mockery, provideFeature, version, shortName);

        FeatureResource testObject = FeatureResource.createInstance(featureDefinition);
        checkCapaibility(version, provideFeature, shortName, testObject);

        // Also check the mockery object didn't flag anything odd
        mockery.assertIsSatisfied();
    }

    /**
     * Test to make sure that the {@link FeatureResource#createInstance(ProvisioningFeatureDefinition)} method sets up the requirements on the feature.
     *
     * @throws InvalidSyntaxException
     */
    @Test
    @Ignore("We don't add requirements on an installed feature because we aren't trying to resolve it but this means we can't create a full wiring with the installed features or check they are valid but I'm not sure that is a problem... (made this decision after writing the test though so thought I'd leave it in place as the test is more complicated than the code!)")
    public void testRequirementsOnInstalledFeature() throws InvalidSyntaxException {
        Mockery mockery = new Mockery();
        final String requireFeature = "Dependency";
        final String requireProductId = "com.ibm.ws";
        final ProvisioningFeatureDefinition featureDefinition = mockery.mock(ProvisioningFeatureDefinition.class);
        final com.ibm.ws.kernel.feature.provisioning.FeatureResource requiredFeatureResource = mockery.mock(com.ibm.ws.kernel.feature.provisioning.FeatureResource.class);
        final Collection<com.ibm.ws.kernel.feature.provisioning.FeatureResource> requiredFeatures = Collections.singleton(requiredFeatureResource);
        mockery.checking(new Expectations() {
            {
                allowing(featureDefinition).getConstituents(SubsystemContentType.FEATURE_TYPE);
                will(returnValue(requiredFeatures));
                allowing(featureDefinition).getHeader("IBM-AppliesTo");
                will(returnValue(requireProductId));
                allowing(requiredFeatureResource).getSymbolicName();
                will(returnValue(requireProductId));
                allowing(featureDefinition).getSymbolicName();
                allowing(featureDefinition).getIbmShortName();
                allowing(featureDefinition).getVersion();
            }
        });

        FeatureResource testObject = FeatureResource.createInstance(featureDefinition);
        Collection<String> requiredFixes = Collections.emptySet();
        checkRequirements(requireProductId, testObject, requiredFixes, Collections.singleton(new ObjectAutoPair<String>(requireFeature, false)));

        // Also check the mockery object didn't flag anything odd
        mockery.assertIsSatisfied();
    }

    /**
     * Test to make sure that {@link FeatureResource#createInstance(ProvisioningFeatureDefinition)} doesn't add any requirements for an installed feature.
     */
    @Test
    public void testNoRequirementsOnInstalledFeature() {
        Mockery mockery = new Mockery();
        final ProvisioningFeatureDefinition featureDefinition = mockery.mock(ProvisioningFeatureDefinition.class);
        mockery.checking(new Expectations() {
            {
                allowing(featureDefinition).getSymbolicName();
                allowing(featureDefinition).getIbmShortName();
                allowing(featureDefinition).getVersion();
                allowing(featureDefinition).getHeader(with(any(String.class)));
            }
        });

        FeatureResource testObject = FeatureResource.createInstance(featureDefinition);
        assertEquals("An installed feature should not provide any requirements", 0,
                     testObject.getRequirements(null).size());

        // Also check the mockery object didn't flag anything odd
        mockery.assertIsSatisfied();
    }

    /**
     * Test to make sure that {@link FeatureResource#createInstance(EsaResourceImpl)} creates an instance with the right capability.
     */
    @Test
    public void testCapabilitiesOnMassiveFeature() {
        // Build the input data
        String version = "1.0.1.201310100833";
        String featureSymbolicName = "FeatureInMassive";
        String featureShortName = "notTheShortestOfShortNames";
        EsaResourceImpl esaResource = createSimpleEsaResource(version, featureSymbolicName, featureShortName, null);

        // Run test
        FeatureResource testObject = FeatureResource.createInstance(esaResource);
        checkCapaibility(new Version(version), featureSymbolicName, featureShortName, testObject);
    }

    /**
     * Test to make sure that {@link FeatureResource#createInstance(EsaResourceImpl)} creates an instance with the right requirements.
     *
     * @throws InvalidSyntaxException
     */
    @Test
    public void testRequirementsOnMassiveFeature() throws InvalidSyntaxException {
        // Build the input data
        String requireFeature1 = "RequiredFeature1";
        String requireFeature2 = "RequiredFeature2";
        String iFix1 = "iFix1";
        String iFix2 = "iFix2";
        String requireProductId = "com.ibm.ws";
        EsaResourceImpl easResource = new EsaResourceImpl(null);
        easResource.setProvideFeature("providedFeature");
        easResource.setVersion("1.0.0.0");
        easResource.addRequireFeature(requireFeature1);
        easResource.addRequireFeature(requireFeature2);
        easResource.addRequireFix(iFix1);
        easResource.addRequireFix(iFix2);
        easResource.setAppliesTo(requireProductId);

        // Run test
        FeatureResource testObject = FeatureResource.createInstance(easResource);
        Collection<ObjectAutoPair<String>> requiredFeatures = new HashSet<ObjectAutoPair<String>>();
        requiredFeatures.add(new ObjectAutoPair<String>(requireFeature1, false));
        requiredFeatures.add(new ObjectAutoPair<String>(requireFeature2, false));
        checkRequirements(requireProductId, testObject, easResource.getRequireFix(), requiredFeatures);
    }

    /**
     * Test to make sure that if an esa resource doesn't have a version then it is set to 0 when created from a massive esa
     */
    @Test
    public void testDefaultVersionOnMassive() {
        // Build the input data
        EsaResourceImpl easResource = new EsaResourceImpl(null);
        easResource.setProvideFeature("feature");

        // Run test
        FeatureResource testObject = FeatureResource.createInstance(easResource);
        checkCapaibility(Version.emptyVersion, "feature", null, testObject);
    }

    /**
     * Test to make sure that if an massive esa doesn't have a version then it is set to 0 when created from an installed feature
     */
    @Test
    public void testDefaultVersionOnInstalled() {
        // Build the input data
        Mockery mockery = new Mockery();
        final String provideFeature = "Feature";

        // FeatureDefinitions automatically set the version to empty
        final Version version = Version.emptyVersion;
        final String shortName = "shortName";
        final ProvisioningFeatureDefinition featureDefinition = ResolverTestUtils.mockSimpleFeatureDefinition(mockery, provideFeature, version, shortName);

        // Run test
        FeatureResource testObject = FeatureResource.createInstance(featureDefinition);
        checkCapaibility(Version.emptyVersion, provideFeature, shortName, testObject);

        // Also check the mockery object didn't flag anything odd
        mockery.assertIsSatisfied();
    }

    /**
     * Tests the ordering of a feature is correct
     */
    @Test
    public void testOrdering() {
        Mockery mockery = new Mockery();
        List<FeatureResource> orderedList = new ArrayList<FeatureResource>();
        List<FeatureResource> unorderedList = new ArrayList<FeatureResource>();

        String symbolicName1 = "aaa";
        String symbolicName2 = "bbb";
        String version1 = "1.0.0.0";
        String version2 = "2.0.0.0";

        FeatureResource installedFeature1Version1 = FeatureResource.createInstance(ResolverTestUtils.mockSimpleFeatureDefinition(mockery, symbolicName1, new Version(version1),
                                                                                                                                 null));
        FeatureResource repoFeature1Version1 = FeatureResource.createInstance(createSimpleEsaResource(version1, symbolicName1, null, null));
        FeatureResource repoFeature1Version2 = FeatureResource.createInstance(createSimpleEsaResource(version2, symbolicName1, null, null));
        FeatureResource repoFeature1Version0 = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName1, null, null));
        FeatureResource repoFeature2Version1 = FeatureResource.createInstance(createSimpleEsaResource(version1, symbolicName2, null, null));
        FeatureResource repoFeature2Version2 = FeatureResource.createInstance(createSimpleEsaResource(version2, symbolicName2, null, null));

        unorderedList.add(repoFeature2Version1);
        unorderedList.add(installedFeature1Version1);
        unorderedList.add(repoFeature1Version1);
        unorderedList.add(repoFeature2Version2);
        unorderedList.add(repoFeature1Version2);
        unorderedList.add(repoFeature1Version0);

        // Should be ordered to be convenient to ResolveContext i.e. the best match is the lowest index
        orderedList.add(installedFeature1Version1);
        orderedList.add(repoFeature1Version2);
        orderedList.add(repoFeature1Version1);
        orderedList.add(repoFeature1Version0);
        orderedList.add(repoFeature2Version2);
        orderedList.add(repoFeature2Version1);

        Collections.sort(unorderedList);

        assertEquals("The unordered list should have been sorted into the same order as the ordered list", orderedList, unorderedList);

        compareElementsInOrderedList(orderedList);
    }

    /**
     * <p>This test makes sure that features are ordered correctly. The rules are:</p>
     * <ol>
     * <li>specific versions beat version ranges (8.5.5.6 is picked before 8.5.5.6+)</li>
     * <li>higher minimum versions beat lower ones (8.5.5.6+ is picked before 8.5.5.5+) </li>
     * </ol>
     * <p>To achieve this the following ordering is expected:</p>
     * <ol>
     * <li>8.5.5.6</li>
     * <li>8.5.5.5</li>
     * <li>8.5.5.4</li>
     * <li>8.5.5.7+</li>
     * <li>8.5.5.6+</li>
     * <li>8.5.5.3+</li>
     * <li>no version</li>
     * <li>no applies to</li>
     * </ol>
     */
    @Test
    public void testOrderingForDifferentAppliesToValues() {
        List<FeatureResource> orderedList = new ArrayList<FeatureResource>();
        List<FeatureResource> unorderedList = new ArrayList<FeatureResource>();

        String symbolicName1 = "aaa";

        // Throw in something with a different symbolic name to make sure that it is used first
        String symbolicName2 = "bbb";
        String version1 = "1.0.0.0";
        // Throw in something with a different version to make sure that it is used first
        String version2 = "2.0.0.0";

        FeatureResource feature8556V1 = FeatureResource.createInstance(createSimpleEsaResource(version1, symbolicName1, null,
                                                                                               "com.ibm.websphere.appserver; productVersion=8.5.5.6"));
        FeatureResource feature8556V2 = FeatureResource.createInstance(createSimpleEsaResource(version2, symbolicName1, null,
                                                                                               "com.ibm.websphere.appserver; productVersion=8.5.5.6"));
        FeatureResource feature8557Plus = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName1, null,
                                                                                                 "com.ibm.websphere.appserver; productVersion=8.5.5.7+"));
        FeatureResource feature8556Plus = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName1, null,
                                                                                                 "com.ibm.websphere.appserver; productVersion=8.5.5.6+"));
        FeatureResource feature8555 = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName1, null, "com.ibm.websphere.appserver; productVersion=8.5.5.5"));
        FeatureResource feature8554 = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName1, null, "com.ibm.websphere.appserver; productVersion=8.5.5.4"));
        FeatureResource feature8553Plus = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName1, null,
                                                                                                 "com.ibm.websphere.appserver; productVersion=8.5.5.3+"));
        FeatureResource featureNoVersion = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName1, null, "com.ibm.websphere.appserver"));
        FeatureResource featureNoAppliesTo = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName1, null, null));
        FeatureResource featureB = FeatureResource.createInstance(createSimpleEsaResource(version1, symbolicName2, null, "com.ibm.websphere.appserver; productVersion=8.5.5.6"));

        unorderedList.add(featureB);
        unorderedList.add(feature8556V1);
        unorderedList.add(feature8557Plus);
        unorderedList.add(feature8556V2);
        unorderedList.add(feature8555);
        unorderedList.add(feature8554);
        unorderedList.add(featureNoVersion);
        unorderedList.add(featureNoAppliesTo);
        unorderedList.add(feature8553Plus);
        unorderedList.add(feature8556Plus);

        // Should be ordered to be convenient to ResolveContext i.e. the best match is the lowest index
        orderedList.add(feature8556V2);
        orderedList.add(feature8556V1);
        orderedList.add(feature8555);
        orderedList.add(feature8554);
        orderedList.add(feature8557Plus);
        orderedList.add(feature8556Plus);
        orderedList.add(feature8553Plus);
        orderedList.add(featureNoVersion);
        orderedList.add(featureNoAppliesTo);
        orderedList.add(featureB);

        Collections.sort(unorderedList);

        List<String> newOrder = new ArrayList<String>();
        for (FeatureResource res : unorderedList) {
            newOrder.add(res.toString() + "::" + res.getRequirements(ProductNamespace.PRODUCT_NAMESPACE));
        }
        assertEquals("The unordered list should have been sorted into the same order as the ordered list, ordering is now:\n" + newOrder, orderedList, unorderedList);

        compareElementsInOrderedList(orderedList);
    }

    @Test
    public void testIsAutoFeatureThatShouldBeInstalledWhenSatisfiedOnRepoFeatureButManualInstall() throws Exception {
        String version = "1.0.1.201310100833";
        String featureSymbolicName = "FeatureInMassive";
        String featureShortName = "notTheShortestOfShortNames";
        EsaResourceImpl esaResource = createSimpleEsaResource(version, featureSymbolicName, featureShortName, null);
        esaResource.setProvisionCapability("wibble");
        esaResource.setInstallPolicy(InstallPolicy.MANUAL);

        FeatureResource featureResource = FeatureResource.createInstance(esaResource);

        assertFalse(featureResource.isAutoFeatureThatShouldBeInstalledWhenSatisfied());
    }

    @Test
    public void testIsAutoFeatureThatShouldBeInstalledWhenSatisfiedOnRepoFeatureInstalledWhenSatisified() throws Exception {
        String version = "1.0.1.201310100833";
        String featureSymbolicName = "FeatureInMassive";
        String featureShortName = "notTheShortestOfShortNames";
        EsaResourceImpl esaResource = createSimpleEsaResource(version, featureSymbolicName, featureShortName, null);
        esaResource.setProvisionCapability("wibble");
        esaResource.setInstallPolicy(InstallPolicy.WHEN_SATISFIED);

        FeatureResource featureResource = FeatureResource.createInstance(esaResource);

        assertTrue(featureResource.isAutoFeatureThatShouldBeInstalledWhenSatisfied());
    }

    @Test
    public void testNotAutoFeatureOnRepoFeature() throws Exception {
        String version = "1.0.1.201310100833";
        String featureSymbolicName = "FeatureInMassive";
        String featureShortName = "notTheShortestOfShortNames";
        EsaResourceImpl esaResource = createSimpleEsaResource(version, featureSymbolicName, featureShortName, null);

        FeatureResource featureResource = FeatureResource.createInstance(esaResource);

        assertFalse(featureResource.isAutoFeatureThatShouldBeInstalledWhenSatisfied());
    }

    @Test
    public void testIsAutoFeatureThatShouldBeInstalledWhenSatisfiedOnInstalledFeatureButManualInstall() throws Exception {
        Mockery mockery = new Mockery();
        final String provideFeature = "Feature";
        final Version version = new Version("1.0.0.201310090819");
        final String shortName = "shortName";
        final ProvisioningFeatureDefinition featureDefinition = ResolverTestUtils.mockSimpleFeatureDefinition(mockery, provideFeature, version, shortName);

        FeatureResource featureResource = FeatureResource.createInstance(featureDefinition);

        assertFalse(featureResource.isAutoFeatureThatShouldBeInstalledWhenSatisfied());
    }

    @Test
    public void testIsAutoFeatureThatShouldBeInstalledWhenSatisfiedOnInstalledFeatureInstalledWhenSatisified() throws Exception {
        Mockery mockery = new Mockery();
        final String provideFeature = "Feature";
        final Version version = new Version("1.0.0.201310090819");
        final String shortName = "shortName";
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("IBM-Provision-Capability", "wibble");
        headers.put("IBM-Install-Policy", "when-satisfied");
        final ProvisioningFeatureDefinition featureDefinition = mockFeatureDefinitionWithHeaders(mockery, provideFeature, version, shortName, headers);

        FeatureResource featureResource = FeatureResource.createInstance(featureDefinition);

        assertTrue(featureResource.isAutoFeatureThatShouldBeInstalledWhenSatisfied());
    }

    private ProvisioningFeatureDefinition mockFeatureDefinitionWithHeaders(Mockery mockery, final String provideFeature, final Version version, final String shortName,
                                                                           Map<String, String> headers) {
        final ProvisioningFeatureDefinition featureDefinition = mockery.mock(ProvisioningFeatureDefinition.class, provideFeature);
        mockery.checking(new Expectations() {
            {
                allowing(featureDefinition).getSymbolicName();
                will(returnValue(provideFeature));
                allowing(featureDefinition).getIbmShortName();
                will(returnValue(shortName));
                allowing(featureDefinition).getVersion();
                will(returnValue(version));
            }
        });
        for (final Map.Entry<String, String> entry : headers.entrySet()) {
            mockery.checking(new Expectations() {
                {
                    allowing(featureDefinition).getHeader(entry.getKey());
                    will(returnValue(entry.getValue()));
                }
            });
        }
        return featureDefinition;
    }

    @Test
    public void testNotAutoFeatureOnInstalledFeature() throws Exception {
        Mockery mockery = new Mockery();
        final String provideFeature = "Feature";
        final Version version = new Version("1.0.0.201310090819");
        final String shortName = "shortName";
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("IBM-Provision-Capability", "wibble");
        headers.put("IBM-Install-Policy", null);
        final ProvisioningFeatureDefinition featureDefinition = mockFeatureDefinitionWithHeaders(mockery, provideFeature, version, shortName, headers);

        FeatureResource featureResource = FeatureResource.createInstance(featureDefinition);

        assertFalse(featureResource.isAutoFeatureThatShouldBeInstalledWhenSatisfied());
    }

    /**
     * Compares all the elements in the list against each other
     *
     * @param orderedList
     */
    private void compareElementsInOrderedList(List<FeatureResource> orderedList) {
        for (int i = 0; i < orderedList.size() - 1; i++) {
            FeatureResource res1 = orderedList.get(i);
            testComparableSame(res1, res1);
            for (int j = i + 1; j < orderedList.size(); j++) {
                testComparableDifferent(res1, orderedList.get(j));
            }
        }

    }

    /**
     * The appliesTo string allows you to define a feature that applies to multiple different products. This makes life a little difficult in terms of ordering! As explained in the
     * class level JavaDoc for {@link FeatureResource} we do enough for our use case even if there are some rubbish (and unlikely) edge cases. This test makes sure that we stick to
     * what the JavaDoc states.
     */
    @Test
    public void testOrderingOnAppliesToWithDifferentProducts() {
        String symbolicName = "aaa";
        String productName1 = "com.ibm.websphere.appserver";
        String productName2 = "com.ibm.websphere.other";

        FeatureResource featureBothProducts8556OnOne = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName, null, productName1 + "; productVersion=8.5.5.6, "
                                                                                                                                        + productName2
                                                                                                                                        + "; productVersion=1.0.0.0"));
        FeatureResource featureBothProducts8556OnOne2OnTwo = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName, null, productName1
                                                                                                                                              + "; productVersion=8.5.5.6, "
                                                                                                                                              + productName2
                                                                                                                                              + "; productVersion=2.0.0.0"));
        FeatureResource featureBothProducts8555OnOneOtherOrder = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName, null, productName2
                                                                                                                                                  + "; productVersion=1.0.0.0, "
                                                                                                                                                  + productName1
                                                                                                                                                  + "; productVersion=8.5.5.5"));
        FeatureResource featureProduct1Only8557 = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName, null, productName1 + "; productVersion=8.5.5.7"));
        FeatureResource featureBothProducts8554OnOne2OnTwo = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName, null, productName1
                                                                                                                                              + "; productVersion=8.5.5.4, "
                                                                                                                                              + productName2
                                                                                                                                              + "; productVersion=2.0.0.0"));
        FeatureResource featureBothProducts8555PlusOnOne = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName, null, productName1
                                                                                                                                            + "; productVersion=8.5.5.5+, "
                                                                                                                                            + productName2
                                                                                                                                            + "; productVersion=1.0.0.0"));
        FeatureResource featureProduct2Only = FeatureResource.createInstance(createSimpleEsaResource(null, symbolicName, null, productName2 + "; productVersion=1.0.0.0"));

        testComparableSame(featureBothProducts8556OnOne, featureBothProducts8556OnOne2OnTwo);
        testComparableDifferent(featureBothProducts8556OnOne, featureBothProducts8555OnOneOtherOrder);
        testComparableDifferent(featureProduct1Only8557, featureBothProducts8556OnOne);
        testComparableDifferent(featureBothProducts8556OnOne, featureBothProducts8554OnOne2OnTwo);
        testComparableDifferent(featureBothProducts8556OnOne, featureBothProducts8555OnOneOtherOrder);
        testComparableDifferent(featureBothProducts8556OnOne, featureBothProducts8555PlusOnOne);
        testComparableDifferent(featureBothProducts8556OnOne, featureProduct2Only);
        testComparableDifferent(featureBothProducts8556OnOne2OnTwo, featureBothProducts8555OnOneOtherOrder);
        testComparableDifferent(featureProduct1Only8557, featureBothProducts8556OnOne2OnTwo);
        testComparableDifferent(featureBothProducts8556OnOne2OnTwo, featureBothProducts8554OnOne2OnTwo);
        testComparableDifferent(featureBothProducts8556OnOne2OnTwo, featureBothProducts8555PlusOnOne);
        testComparableDifferent(featureBothProducts8556OnOne2OnTwo, featureProduct2Only);
        testComparableDifferent(featureProduct1Only8557, featureBothProducts8555OnOneOtherOrder);
        testComparableDifferent(featureBothProducts8554OnOne2OnTwo, featureBothProducts8555OnOneOtherOrder);
        testComparableDifferent(featureBothProducts8555OnOneOtherOrder, featureBothProducts8555PlusOnOne);
        testComparableSame(featureBothProducts8555OnOneOtherOrder, featureProduct2Only);
        testComparableDifferent(featureProduct1Only8557, featureBothProducts8554OnOne2OnTwo);
        testComparableDifferent(featureProduct1Only8557, featureBothProducts8555PlusOnOne);
        testComparableDifferent(featureProduct1Only8557, featureProduct2Only);
        testComparableDifferent(featureBothProducts8554OnOne2OnTwo, featureBothProducts8555PlusOnOne);
        testComparableDifferent(featureBothProducts8554OnOne2OnTwo, featureProduct2Only);
        testComparableDifferent(featureProduct2Only, featureBothProducts8555PlusOnOne);
    }

    /**
     * Tests that comparing the two objects returns a positive or negative int depending on order
     *
     * @param lower
     * @param higher
     */
    private void testComparableDifferent(FeatureResource lower, FeatureResource higher) {
        assertTrue("Higher should be higher than lower: " + higher + ", " + lower, higher.compareTo(lower) > 0);
        assertTrue("Lower should be lower than higher: " + higher + ", " + lower, lower.compareTo(higher) < 0);
    }

    /**
     * Tests that comparing the two objects returns 0
     *
     * @param feature1
     * @param feature2
     */
    private void testComparableSame(FeatureResource feature1, FeatureResource feature2) {
        assertEquals("The two features should compare to be the same: " + feature1 + ", " + feature2, 0, feature1.compareTo(feature2));
        assertEquals("The two features should compare to be the same: " + feature1 + ", " + feature2, 0, feature2.compareTo(feature1));

    }

    /**
     * Test a requirement is added for auto features
     *
     * @throws InvalidSyntaxException
     */
    @Test
    public void testAutoFeatureRequirement() throws InvalidSyntaxException {
        // Build the input data
        String autoFeature1 = "RequiredFeature1";
        String autoFeature2 = "RequiredFeature2";
        String requireProductId = "com.ibm.ws";
        EsaResourceImpl easResource = new EsaResourceImpl(null);
        easResource.setProvideFeature("providedFeature");
        easResource.setVersion("1.0.0.0");
        easResource.setAppliesTo(requireProductId);
        String autoFeatureString = "osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=" + autoFeature1
                                   + "))\", osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=" + autoFeature2 + "))\"";
        easResource.setProvisionCapability(autoFeatureString);

        // Run test
        FeatureResource testObject = FeatureResource.createInstance(easResource);
        Collection<ObjectAutoPair<String>> requiredFeatures = new HashSet<ObjectAutoPair<String>>();
        requiredFeatures.add(new ObjectAutoPair<String>(autoFeature1, true));
        requiredFeatures.add(new ObjectAutoPair<String>(autoFeature2, true));
        Collection<String> empty = Collections.emptySet();
        checkRequirements(requireProductId, testObject, empty, requiredFeatures);
    }

    /**
     * @param version
     * @param featureSymbolicName
     * @param featureShortName
     * @param appliesTo
     * @return
     */
    private EsaResourceImpl createSimpleEsaResource(String version, String featureSymbolicName, String featureShortName, String appliesTo) {
        EsaResourceImpl easResource = new EsaResourceImpl(null);
        easResource.setVersion(version);
        easResource.setProvideFeature(featureSymbolicName);
        easResource.setShortName(featureShortName);
        easResource.setAppliesTo(appliesTo);
        return easResource;
    }

    /**
     * Utility method to test the requirements created on a test object
     *
     * @param requireProductId
     * @param testObject
     * @param requiredFixes
     * @param requiredFeatures
     * @throws InvalidSyntaxException
     */
    private void checkRequirements(String requireProductId, FeatureResource testObject, Collection<String> requiredFixes,
                                   Collection<ObjectAutoPair<String>> requiredFeatures) throws InvalidSyntaxException {
        Collection<ObjectAutoPair<Map<String, String>>> fixesAndFeaturesAttributes = new HashSet<ObjectAutoPair<Map<String, String>>>();
        for (String requiredFix : requiredFixes) {
            Map<String, String> fixAttributes = new HashMap<String, String>();
            fixAttributes.put(IdentityNamespace.IDENTITY_NAMESPACE, requiredFix);
            fixAttributes.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, InstallableEntityIdentityConstants.TYPE_IFIX);
            fixesAndFeaturesAttributes.add(new ObjectAutoPair<Map<String, String>>(fixAttributes, false));
        }
        for (ObjectAutoPair<String> requiredFeature : requiredFeatures) {
            Map<String, String> featureAttributes = new HashMap<String, String>();
            featureAttributes.put(IdentityNamespace.IDENTITY_NAMESPACE, requiredFeature.object);
            featureAttributes.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, InstallableEntityIdentityConstants.TYPE_FEATURE);
            fixesAndFeaturesAttributes.add(new ObjectAutoPair<Map<String, String>>(featureAttributes, requiredFeature.isAuto));
        }
        assertEquals("A feature should provide one requirement for the product and one for each of the required features and fixes",
                     1 + requiredFixes.size() + requiredFeatures.size(),
                     testObject.getRequirements(null).size());
        assertEquals("A feature should provide one installable entity requirement for each of its required features and fixes", requiredFixes.size() + requiredFeatures.size(),
                     testObject.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE).size());
        assertEquals("A feature should provide one product requirements", 1,
                     testObject.getRequirements(ProductNamespace.PRODUCT_NAMESPACE).size());
        List<Requirement> featureRequirements = testObject.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE);
        for (Requirement requirement : featureRequirements) {
            Iterator<ObjectAutoPair<Map<String, String>>> attributeIterator = fixesAndFeaturesAttributes.iterator();
            boolean found = false;
            Filter filter = FrameworkUtil.createFilter(requirement.getDirectives().get(IdentityNamespace.REQUIREMENT_FILTER_DIRECTIVE));
            while (attributeIterator.hasNext()) {
                ObjectAutoPair<Map<String, String>> attributesAndAuto = attributeIterator.next();
                Map<String, String> attributes = attributesAndAuto.object;
                if (filter.matches(attributes)) {
                    attributeIterator.remove();
                    assertEquals("The name should match the identity of the fix or feature", attributes.get(IdentityNamespace.IDENTITY_NAMESPACE),
                                 ((RequirementImpl) requirement).getName());
                    if (attributesAndAuto.isAuto) {
                        assertEquals("The requirement should be as an auto feature", InstallableEntityIdentityConstants.CLASSIFIER_AUTO,
                                     requirement.getDirectives().get(IdentityNamespace.REQUIREMENT_CLASSIFIER_DIRECTIVE));
                    } else {
                        assertNull("The requirement should not be as an auto feature", requirement.getDirectives().get(IdentityNamespace.REQUIREMENT_CLASSIFIER_DIRECTIVE));
                    }
                    found = true;
                    break;
                }
            }
            assertTrue("A requirement had a filter that did not match any of the expected required entities, the filter was: " + filter, found);
        }
        assertTrue("All of the expected requirements should have been matched", fixesAndFeaturesAttributes.isEmpty());
        List<Requirement> productRequirements = testObject.getRequirements(ProductNamespace.PRODUCT_NAMESPACE);
        Filter productFilter = FrameworkUtil.createFilter(productRequirements.get(0).getDirectives().get(IdentityNamespace.REQUIREMENT_FILTER_DIRECTIVE));
        Map<String, Object> productAttributes = new HashMap<String, Object>();
        productAttributes.put(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE, requireProductId);
        assertTrue("The filter should match a product with the right ID, filter is: " + productFilter, productFilter.matches(productAttributes));
    }

    private static class ObjectAutoPair<T extends Object> {

        private final T object;
        private final boolean isAuto;

        /**
         * @param object
         * @param isAuto
         */
        public ObjectAutoPair(T object, boolean isAuto) {
            super();
            this.object = object;
            this.isAuto = isAuto;
        }

    }

    /**
     * Checks that a feature resource has a single capability for the provided feature.
     *
     * @param version
     * @param featureSymbolicName
     * @param featureShortName
     * @param testObject
     */
    private void checkCapaibility(Version version, String featureSymbolicName, String featureShortName, FeatureResource testObject) {
        assertEquals("A feature should only provide one capability", 1, testObject.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).size());
        Map<String, Object> capabilityAttributes = testObject.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0).getAttributes();
        assertEquals("The capability symbolic name should match the one supplied on the feature", featureSymbolicName,
                     capabilityAttributes.get(IdentityNamespace.IDENTITY_NAMESPACE));
        assertEquals("The capability short name should match the one supplied on the feature", featureShortName,
                     capabilityAttributes.get(InstallableEntityIdentityConstants.CAPABILITY_SHORT_NAME_ATTRIBUTE));
        assertEquals("The capability version should match the one supplied on the feature", version,
                     capabilityAttributes.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
        assertEquals("The capability type should be feature", InstallableEntityIdentityConstants.TYPE_FEATURE,
                     capabilityAttributes.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
    }

}

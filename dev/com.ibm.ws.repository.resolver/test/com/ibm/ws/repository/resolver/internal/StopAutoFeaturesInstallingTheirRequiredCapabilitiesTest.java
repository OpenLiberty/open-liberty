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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.junit.Test;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants;
import com.ibm.ws.repository.resolver.internal.resource.FeatureResource;
import com.ibm.ws.repository.resolver.internal.resource.GenericMetadataRequirement;
import com.ibm.ws.repository.resolver.internal.resource.InstallableEntityRequirement;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;

public class StopAutoFeaturesInstallingTheirRequiredCapabilitiesTest {

    private final StopAutoFeaturesInstallingTheirRequiredCapabilities testObject = new StopAutoFeaturesInstallingTheirRequiredCapabilities();

    @Test
    public void testAutoFeaturesRequiringCapabilityFiltered() throws Exception {
        Requirement requirement = givenAnAutoFeatureProvisionCapabilityRequirement();
        List<Capability> capabilities = givenACapabilityFromANormalFeature();

        boolean result = whenTheRequirementAndCapabilitiesAreFiltered(requirement, capabilities);

        thenTheRequirementShouldBeFiltered(result);
    }

    @Test
    public void testAutoFeaturesRequiringCapabilityFilteredEvenWithoutCapability() throws Exception {
        Requirement requirement = givenAnAutoFeatureProvisionCapabilityRequirement();
        List<Capability> capabilities = givenAnEmptyListOfPotentialCapabilities();

        boolean result = whenTheRequirementAndCapabilitiesAreFiltered(requirement, capabilities);

        thenTheRequirementShouldBeFiltered(result);
    }

    @Test
    public void testAutoFeaturesRequiringOtherFeatureAllowed() throws Exception {
        Requirement requirement = givenAnAutoFeatureRequiringAFeatureThroughSubsystemContents();
        List<Capability> capabilities = givenACapabilityFromANormalFeature();

        boolean result = whenTheRequirementAndCapabilitiesAreFiltered(requirement, capabilities);

        thenTheRequirementShouldBeAllowed(result);
    }

    @Test
    public void testAutoFeaturesWithProvisionCapabilitiesToOtherAutoFeaturesAreAllowed() throws Exception {
        Requirement requirement = givenAnAutoFeatureProvisionCapabilityRequirement();
        List<Capability> capabilities = givenACapabilityFromAnAutoFeatureThatIsInstalledWhenSatisified();

        boolean result = whenTheRequirementAndCapabilitiesAreFiltered(requirement, capabilities);

        thenTheRequirementShouldBeAllowed(result);
    }

    @Test
    public void testAutoFeaturesWithProvisionCapabilitiesToOtherAutoFeaturesThatAreManualInstallAreFiltered() throws Exception {
        Requirement requirement = givenAnAutoFeatureProvisionCapabilityRequirement();
        List<Capability> capabilities = givenACapabilityFromAnAutoFeatureThatIsInstalledManually();

        boolean result = whenTheRequirementAndCapabilitiesAreFiltered(requirement, capabilities);

        thenTheRequirementShouldBeFiltered(result);
    }

    @Test
    public void testAutoFeaturesWithProvisionCapabilitiesToNormalFeaturesIsFiltered() throws Exception {
        Requirement requirement = givenAnAutoFeatureProvisionCapabilityRequirement();
        List<Capability> capabilities = givenACapabilityFromANormalFeature();

        boolean result = whenTheRequirementAndCapabilitiesAreFiltered(requirement, capabilities);

        thenTheRequirementShouldBeFiltered(result);
    }

    /**
     * @return
     */
    private List<Capability> givenACapabilityFromANormalFeature() {
        EsaResourceImpl esaResource = new EsaResourceImpl(null);
        esaResource.setProvideFeature("wibble");
        FeatureResource resource = FeatureResource.createInstance(esaResource);

        return resource.getCapabilities(null);
    }

    private List<Capability> givenACapabilityFromAnAutoFeatureThatIsInstalledWhenSatisified() {
        EsaResourceImpl esaResource = new EsaResourceImpl(null);
        esaResource.setProvideFeature("wibble");
        esaResource.setProvisionCapability("osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=fish))\"");
        esaResource.setInstallPolicy(InstallPolicy.WHEN_SATISFIED);
        FeatureResource resource = FeatureResource.createInstance(esaResource);

        return resource.getCapabilities(null);
    }

    private List<Capability> givenACapabilityFromAnAutoFeatureThatIsInstalledManually() {
        EsaResourceImpl esaResource = new EsaResourceImpl(null);
        esaResource.setProvideFeature("wibble");
        esaResource.setProvisionCapability("osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=fish))\"");
        esaResource.setInstallPolicy(InstallPolicy.MANUAL);
        FeatureResource resource = FeatureResource.createInstance(esaResource);

        return resource.getCapabilities(null);
    }

    private Requirement givenAnAutoFeatureRequiringAFeatureThroughSubsystemContents() {
        return new InstallableEntityRequirement("wibble", InstallableEntityIdentityConstants.TYPE_FEATURE);
    }

    private Requirement givenAnAutoFeatureProvisionCapabilityRequirement() {
        GenericMetadata metadata = ManifestHeaderProcessor.parseRequirementString("osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=wibble))\"").get(0);
        Requirement requirement = new GenericMetadataRequirement(metadata);
        requirement.getDirectives().put(IdentityNamespace.REQUIREMENT_CLASSIFIER_DIRECTIVE, InstallableEntityIdentityConstants.CLASSIFIER_AUTO);
        return requirement;
    }

    private List<Capability> givenAnEmptyListOfPotentialCapabilities() {
        return Collections.<Capability> emptyList();
    }

    private boolean whenTheRequirementAndCapabilitiesAreFiltered(Requirement requirement, List<Capability> capabilities) {
        return testObject.allowResolution(requirement, capabilities);
    }

    private void thenTheRequirementShouldBeFiltered(boolean result) {
        assertFalse(result);
    }

    private void thenTheRequirementShouldBeAllowed(boolean result) {
        assertTrue(result);
    }

}

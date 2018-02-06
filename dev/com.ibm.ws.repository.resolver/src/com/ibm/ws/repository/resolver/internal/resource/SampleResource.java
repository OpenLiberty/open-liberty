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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.transport.model.Asset;

/**
 * <p>
 * This is a resource for a sample (note, this includes both product samples and OSIs). It will give a {@link Capability} for the sample it provides and have a {@link Requirement}
 * on the product that it applies to and each of the features it requires.
 * </p>
 * <p>
 * There is no public constructor for this class, instead there is a factory method that can be called for samples in massive then the {@link Asset} representation of it should be
 * passed to {@link #createInstance(SampleResource)}. There is no way to construct an instance of this resource based on a sample installed on the file system. </p>
 */
public class SampleResource extends ResourceImpl {

    /** The name of the sample to look for */
    private final String sampleName;

    /**
     * Create an instance of this class for a sample that is in Massive.
     * 
     * @param massiveSample The sample in massive
     * @return The instance
     */
    public static SampleResource createInstance(com.ibm.ws.repository.resources.SampleResource massiveSample) {
        List<Requirement> requirements = new ArrayList<Requirement>();

        /*
         * Put the requirement onto the product first, this way this is the first requirement we find is on the product so if this sample isn't applicable then we report that
         * rather than the dependent features. See defect 129171 for more details.
         */
        String appliesTo = massiveSample.getAppliesTo();
        if (appliesTo != null && !appliesTo.isEmpty()) {
            requirements.add(new ProductRequirement(appliesTo));
        }

        // And also have one requirement for each of the required features
        Collection<String> requiredFeatures = massiveSample.getRequireFeature();
        if (requiredFeatures != null) {
            for (String requiredFeature : requiredFeatures) {
                requirements.add(new InstallableEntityRequirement(requiredFeature, InstallableEntityIdentityConstants.TYPE_FEATURE));
            }
        }

        return new SampleResource(massiveSample.getShortName(), massiveSample.getShortName().toLowerCase(), requirements, LOCATION_REPOSITORY, massiveSample);
    }

    private SampleResource(String shortName, String lowerCaseShortName, List<Requirement> requirements, String location,
                           RepositoryResource massiveResource) {
        super(createCapabilities(shortName, lowerCaseShortName), requirements, location, massiveResource);
        this.sampleName = shortName;
    }

    /**
     * Creates a single capability for the sample with the supplied details
     * 
     * @param shortName The short name of the sample, must not be <code>null</code>
     * @param lowerCaseShortName The lower case version of the short name, must not be <code>null</code>
     * @return The list of the single capability for this sample
     */
    private static List<Capability> createCapabilities(String shortName, String lowerCaseShortName) {
        List<Capability> capabilities = new ArrayList<Capability>();

        // Just a single capability for the sample
        capabilities.add(new InstallableEntityCapability(shortName, shortName, lowerCaseShortName, null, InstallableEntityIdentityConstants.TYPE_SAMPLE));
        return capabilities;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "SampleResource [name=" + sampleName + "]";
    }

}

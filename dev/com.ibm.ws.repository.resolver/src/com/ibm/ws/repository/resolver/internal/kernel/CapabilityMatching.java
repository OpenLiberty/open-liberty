/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resolver.internal.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.service.subsystem.SubsystemConstants;

import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

/**
 * Static methods for evaluating and matching the IBM-Provision-Capability header on features
 */
public class CapabilityMatching {

    /**
     * Find which of the passed features are used to satisfy the ProvisionCapability of a feature.
     * <p>
     * The returned list will be a subset of {@code features}
     * <p>
     * If the feature has no ProvisionCapability requirements, then an empty list will be returned.
     *
     * @param feature the feature with the ProvisionCapability header
     * @param features the features which are available be used to satisfy the ProvisionCapabiliy requirements
     * @return a list of features which were actually used to satisfy the ProvisionCapability requirements
     */
    public static List<ProvisioningFeatureDefinition> findFeaturesSatisfyingCapability(ProvisioningFeatureDefinition feature,
                                                                                       Collection<? extends ProvisioningFeatureDefinition> features) {
        String capabilityString;
        if (feature instanceof KernelResolverEsa) {
            capabilityString = ((KernelResolverEsa) feature).getResource().getProvisionCapability();
        } else {
            capabilityString = feature.getHeader("IBM-Provision-Capability");
        }

        return matchCapability(capabilityString, features).getFeatures();
    }

    /**
     * Attempt to match ProvisionCapability requirements against a list of features
     *
     * @param capabilityString the ProvisionCapability requirements
     * @param features the list of features to match against
     * @return a {@link CapabilityMatchingResult} specifying whether all requirements were satisfied and which features were used to satisfy them
     */
    public static CapabilityMatchingResult matchCapability(String capabilityString, Collection<? extends ProvisioningFeatureDefinition> features) {

        if (capabilityString == null) {
            CapabilityMatchingResult result = new CapabilityMatchingResult();
            result.capabilitySatisfied = true;
            result.features = Collections.emptyList();
            return result;
        }

        CapabilityMatchingResult result = new CapabilityMatchingResult();
        result.capabilitySatisfied = true;
        result.features = new ArrayList<>();

        List<FeatureCapabilityInfo> capabilityMaps = createCapabilityMaps(features);
        for (Filter filter : createFilterList(capabilityString)) {
            boolean matched = false;
            for (FeatureCapabilityInfo capabilityInfo : capabilityMaps) {
                if (filter.matches(capabilityInfo.capabilities)) {
                    matched = true;
                    result.getFeatures().add(capabilityInfo.feature);
                    break;
                }
            }

            // If any of the filters in the provision capability header don't match, we're not satisfied
            if (!matched) {
                result.capabilitySatisfied = false;
            }
        }

        return result;
    }

    private static List<FeatureCapabilityInfo> createCapabilityMaps(Collection<? extends ProvisioningFeatureDefinition> features) {
        List<FeatureCapabilityInfo> result = new ArrayList<>();

        for (ProvisioningFeatureDefinition feature : features) {
            FeatureCapabilityInfo capabilityInfo = new FeatureCapabilityInfo();
            capabilityInfo.feature = feature;
            capabilityInfo.capabilities = new HashMap<>();
            capabilityInfo.capabilities.put(IdentityNamespace.IDENTITY_NAMESPACE, feature.getSymbolicName());
            capabilityInfo.capabilities.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
            result.add(capabilityInfo);
        }

        return result;
    }

    private static List<Filter> createFilterList(String capabilityHeader) {
        List<GenericMetadata> metadatas = ManifestHeaderProcessor.parseCapabilityString(capabilityHeader);
        List<Filter> result = new ArrayList<>();

        for (GenericMetadata metadata : metadatas) {
            String filterString = metadata.getDirectives().get(IdentityNamespace.REQUIREMENT_FILTER_DIRECTIVE);
            if (IdentityNamespace.IDENTITY_NAMESPACE.equals(metadata.getNamespace()) && filterString != null) {
                try {
                    result.add(FrameworkUtil.createFilter(filterString));
                } catch (InvalidSyntaxException e) {
                    throw new IllegalArgumentException("invalid provisionCapabiliy requirement: " + filterString, e);
                }
            }
        }

        return result;
    }

    public static class CapabilityMatchingResult {
        private List<ProvisioningFeatureDefinition> features;
        private boolean capabilitySatisfied;

        /**
         * @return whether the provision capability string was satisfied by the supplied features
         */
        public boolean isCapabilitySatisfied() {
            return capabilitySatisfied;
        }

        /**
         * @return the features used to satisfy the capability string
         */
        public List<ProvisioningFeatureDefinition> getFeatures() {
            return features;
        }
    }

    private static class FeatureCapabilityInfo {
        ProvisioningFeatureDefinition feature;
        Map<String, String> capabilities;
    }
}

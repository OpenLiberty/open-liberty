/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resolver.internal.kernel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.service.subsystem.SubsystemConstants;

import com.ibm.ws.kernel.feature.AppForceRestart;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.HeaderElementDefinition;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.resources.EsaResource;

/**
 * Presents a feature from the repository as a {@link ProvisioningFeatureDefinition}
 * <p>
 * Wraps an {@link EsaResource} and implements {@link ProvisioningFeatureDefinition} to allow a repository feature to be resolved using the kernel resolver
 */
public class KernelResolverEsa implements ProvisioningFeatureDefinition {

    public static class CapabilityMatchingResult {
        List<ProvisioningFeatureDefinition> features;
        boolean capabilitySatisfied;
    }

    public static class FeatureCapabilityInfo {
        ProvisioningFeatureDefinition feature;
        Map<String, String> capabilities;
    }

    private final EsaResource esaResource;

    public KernelResolverEsa(EsaResource esaResource) {
        if (esaResource == null) {
            throw new NullPointerException();
        }
        this.esaResource = esaResource;
    }

    /**
     * Get the EsaResource that this instance is wrapping
     *
     * @return the EsaResource
     */
    public EsaResource getResource() {
        return esaResource;
    }

    @Override
    public String getSymbolicName() {
        return esaResource.getProvideFeature();
    }

    @Override
    public String getFeatureName() {
        String name = esaResource.getShortName();
        if (name == null) {
            name = esaResource.getProvideFeature();
        }
        return name;
    }

    @Override
    public EnumSet<ProcessType> getProcessTypes() {
        return EnumSet.of(ProcessType.SERVER);
    }

    @Override
    public Visibility getVisibility() {
        // When resolving features for install, we don't care about visibility
        // However, the kernel resolver requires that the features requested by the user are public
        // To subvert this check, make all features report as public
        return Visibility.PUBLIC;
    }

    @Override
    public String getBundleRepositoryType() {
        return "";
    }

    @Override
    public Collection<FeatureResource> getConstituents(SubsystemContentType type) {
        if (type != SubsystemContentType.FEATURE_TYPE) {
            return Collections.emptySet();
        }

        if (esaResource.getRequireFeatureWithTolerates() == null) {
            return Collections.emptySet();
        }

        List<FeatureResource> result = new ArrayList<>();

        for (Entry<String, Collection<String>> requirement : esaResource.getRequireFeatureWithTolerates().entrySet()) {
            String symbolicName = requirement.getKey();
            Collection<String> tolerates = requirement.getValue();

            if (tolerates == null) {
                tolerates = Collections.emptyList();
            }

            result.add(new KernelResolverRequirement(symbolicName, tolerates));
        }

        return result;
    }

    @Override
    public String getIbmShortName() {
        return esaResource.getShortName();
    }

    @Override
    public boolean isAutoFeature() {
        return esaResource.getInstallPolicy() == InstallPolicy.WHEN_SATISFIED;
    }

    @Override
    public boolean isCapabilitySatisfied(Collection<ProvisioningFeatureDefinition> features) {
        return matchCapability(features).capabilitySatisfied;
    }

    @Override
    public Version getVersion() {
        try {
            return Version.parseVersion(esaResource.getVersion());
        } catch (IllegalArgumentException ex) {
            return Version.emptyVersion;
        }
    }

    /**
     * Find which of the passed features are used to satisfy the ProvisionCapability of this feature.
     * <p>
     * The returned list will be a subset of {@code features}
     * <p>
     * If this feature has no ProvisionCapability requirements, then an empty list will be returned.
     *
     * @param features the features which are available be used to satisfy the ProvisionCapabiliy requirements
     * @return a list of features which were actually used to satisfy the ProvisionCapability requirements
     */
    public List<ProvisioningFeatureDefinition> findFeaturesSatisfyingCapability(Collection<? extends ProvisioningFeatureDefinition> features) {
        return matchCapability(features).features;
    }

    /**
     * Attempt to match the ProvisionCapability requirements against the given list of features
     *
     * @param features the list of features to match against
     * @return a {@link CapabilityMatchingResult} specifying whether all requirements were satisfied and which features were used to satisfy them
     */
    private CapabilityMatchingResult matchCapability(Collection<? extends ProvisioningFeatureDefinition> features) {

        String capabilityString = esaResource.getProvisionCapability();
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
        for (Filter filter : createFilterList()) {
            boolean matched = false;
            for (FeatureCapabilityInfo capabilityInfo : capabilityMaps) {
                if (filter.matches(capabilityInfo.capabilities)) {
                    matched = true;
                    result.features.add(capabilityInfo.feature);
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

    private List<FeatureCapabilityInfo> createCapabilityMaps(Collection<? extends ProvisioningFeatureDefinition> features) {
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

    private List<Filter> createFilterList() {
        List<GenericMetadata> metadatas = ManifestHeaderProcessor.parseCapabilityString(esaResource.getProvisionCapability());
        List<Filter> result = new ArrayList<>();

        for (GenericMetadata metadata : metadatas) {
            String filterString = metadata.getDirectives().get(IdentityNamespace.REQUIREMENT_FILTER_DIRECTIVE);
            if (IdentityNamespace.IDENTITY_NAMESPACE.equals(metadata.getNamespace()) && filterString != null) {
                try {
                    result.add(FrameworkUtil.createFilter(filterString));
                } catch (InvalidSyntaxException e) {
                    throw new IllegalArgumentException("Esa " + esaResource.getProvideFeature() + " contains invalid provisionCapabiliy requirement: " + filterString, e);
                }
            }
        }

        return result;
    }

    @Override
    public boolean isSingleton() {
        return esaResource.isSingleton();
    }

    @Override
    public String getApiServices() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AppForceRestart getAppForceRestart() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isKernel() {
        return false;
    }

    @Override
    public File getFeatureChecksumFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getFeatureDefinitionFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHeader(String arg0, Locale arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHeader(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<HeaderElementDefinition> getHeaderElements(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIbmFeatureVersion() {
        return 0;
    }

    @Override
    public Collection<String> getIcons() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<File> getLocalizationFiles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSupersededBy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSuperseded() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSupportedFeatureVersion() {
        throw new UnsupportedOperationException();
    }

}

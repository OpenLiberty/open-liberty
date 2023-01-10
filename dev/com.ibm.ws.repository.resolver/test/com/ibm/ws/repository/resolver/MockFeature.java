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
package com.ibm.ws.repository.resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.osgi.framework.Version;

import com.ibm.ws.kernel.feature.AppForceRestart;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.HeaderElementDefinition;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.repository.resolver.internal.kernel.CapabilityMatching;
import com.ibm.ws.repository.resolver.internal.kernel.KernelResolverRequirement;

/**
 *
 */
public class MockFeature implements ProvisioningFeatureDefinition {

    private final String symbolicName;
    private Visibility visibility = Visibility.PRIVATE;
    private final List<FeatureResource> featureDependencies = new ArrayList<>();
    private String provisionCapability;

    public MockFeature(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    /**
     * @param visibility the visibility to set
     */
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    /**
     * Add a dependency on another feature
     *
     * @param symbolicName the symbolic name of the dependency
     * @param tolerates the tolerated versions of the dependency
     */
    public void addDependency(String symbolicName, String... tolerates) {
        featureDependencies.add(new KernelResolverRequirement(symbolicName, Arrays.asList(tolerates)));
    }

    public void setProvisionCapability(String provisionCapability) {
        this.provisionCapability = provisionCapability;
    }

    @Override
    public String getSymbolicName() {
        return symbolicName;
    }

    @Override
    public String getFeatureName() {
        return symbolicName;
    }

    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public Collection<FeatureResource> getConstituents(SubsystemContentType type) {
        if (type != SubsystemContentType.FEATURE_TYPE) {
            return Collections.emptySet();
        }
        return featureDependencies;
    }

    @Override
    public Version getVersion() {
        return Version.emptyVersion;
    }

    @Override
    public EnumSet<ProcessType> getProcessTypes() {
        return EnumSet.of(ProcessType.SERVER);
    }

    @Override
    public boolean isKernel() {
        return false;
    }

    @Override
    public String getBundleRepositoryType() {
        return "";
    }

    @Override
    public String getIbmShortName() {
        return null;
    }

    @Override
    public boolean isCapabilitySatisfied(Collection<ProvisioningFeatureDefinition> featureDefinitionsToCheck) {
        return CapabilityMatching.matchCapability(provisionCapability, featureDefinitionsToCheck).isCapabilitySatisfied();
    }

    @Override
    public boolean isAutoFeature() {
        return provisionCapability != null;
    }

    @Override
    public boolean isSingleton() {
        return true;
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
    public String getHeader(String header) {
        if ("IBM-Provision-Capability".equals(header)) {
            return provisionCapability;
        }
        return null;
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

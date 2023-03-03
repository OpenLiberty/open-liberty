/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.osgi.framework.Version;

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
        if (esaResource.getVisibility() == null) {
            return Visibility.PRIVATE;
        }
        switch (esaResource.getVisibility()) {
            case PUBLIC:
                return Visibility.PUBLIC;
            case PROTECTED:
                return Visibility.PROTECTED;
            case INSTALL:
                return Visibility.INSTALL;
            case PRIVATE:
                return Visibility.PRIVATE;
            default:
                throw new IllegalArgumentException("Invalid visibility: " + esaResource.getVisibility());
        }
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
        return CapabilityMatching.matchCapability(esaResource.getProvisionCapability(), features).isCapabilitySatisfied();
    }

    @Override
    public Version getVersion() {
        try {
            return Version.parseVersion(esaResource.getVersion());
        } catch (IllegalArgumentException ex) {
            return Version.emptyVersion;
        }
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

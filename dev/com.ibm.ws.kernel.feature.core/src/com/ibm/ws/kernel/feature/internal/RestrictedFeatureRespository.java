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
package com.ibm.ws.kernel.feature.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Repository;

/**
 *
 */
public class RestrictedFeatureRespository implements Repository {
    private final Repository repo;
    private final Collection<String> restricted;
    private final Collection<String> restrictedAttempts = new ArrayList<String>();

    public RestrictedFeatureRespository(Repository repo, Collection<String> restricted) {
        this.repo = repo;
        this.restricted = restricted;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.kernel.feature.resolver.FeatureResolver.Repository#getAutoFeatures()
     */
    @Override
    public Collection<ProvisioningFeatureDefinition> getAutoFeatures() {
        return repo.getAutoFeatures();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.kernel.feature.resolver.FeatureResolver.Repository#getFeature(java.lang.String)
     */
    @Override
    public ProvisioningFeatureDefinition getFeature(String featureName) {
        ProvisioningFeatureDefinition result = repo.getFeature(featureName);
        if (result == null) {
            return null;
        }
        if (restricted.contains(result.getSymbolicName())) {
            // record the restricted attemp
            if (!restrictedAttempts.contains(result.getSymbolicName())) {
                restrictedAttempts.add(result.getSymbolicName());
            }
            return null;
        }
        return result;
    }

    Collection<String> getRestrictedFeatureAttempts() {
        return restrictedAttempts;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.kernel.feature.resolver.FeatureResolver.Repository#getConfiguredTolerates(java.lang.String)
     */
    @Override
    public List<String> getConfiguredTolerates(String baseSymbolicName) {
        return repo.getConfiguredTolerates(baseSymbolicName);
    }

}

/*******************************************************************************
 * Copyright (c) 2015,2024 IBM Corporation and others.
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
package com.ibm.ws.kernel.feature.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Repository;

public class RestrictedFeatureRespository implements Repository {
    public RestrictedFeatureRespository(Repository repo, Collection<String> restricted) {
        this.repo = repo;

        this.restricted = restricted;
        this.restrictedAttempts = new ArrayList<String>();
    }

    private final Repository repo;

    @Override
    public Collection<ProvisioningFeatureDefinition> getAutoFeatures() {
        return repo.getAutoFeatures();
    }

    @Override
    public List<String> getConfiguredTolerates(String baseSymbolicName) {
        return repo.getConfiguredTolerates(baseSymbolicName);
    }

    //

    private final Collection<String> restricted;
    private final Collection<String> restrictedAttempts;

    @Override
    public Collection<ProvisioningFeatureDefinition> select(Predicate<ProvisioningFeatureDefinition> selector) {
        return repo.select((ProvisioningFeatureDefinition def) -> (!restricted.contains(def.getSymbolicName()) && selector.test(def)));
    }

    @Override
    public Collection<ProvisioningFeatureDefinition> getFeatures() {
        return repo.select((ProvisioningFeatureDefinition def) -> (!restricted.contains(def.getSymbolicName())));
    }

    @Override
    public ProvisioningFeatureDefinition getFeature(String featureName) {
        ProvisioningFeatureDefinition featureDef = repo.getFeature(featureName);
        if (featureDef == null) {
            return null;
        }

        String featureSymName = featureDef.getSymbolicName();
        if (!restricted.contains(featureSymName)) {
            return featureDef;
        }

        if (!restrictedAttempts.contains(featureSymName)) {
            restrictedAttempts.add(featureSymName);
        }
        return null;
    }

    Collection<String> getRestrictedFeatureAttempts() {
        return restrictedAttempts;
    }
}

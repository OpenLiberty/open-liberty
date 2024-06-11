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
import java.util.Map;

import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Repository;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Selector;

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

    @Override
    public Map<String, SubsystemFeatureDefinitionImpl> getCompatibilityFeatures() {
        return repo.getCompatibilityFeatures();
    }

    //

    public static List<ProvisioningFeatureDefinition> select(Repository repository, Selector<ProvisioningFeatureDefinition> selector) {
        return select(repository.getFeatures(), selector);
    }

    public static List<ProvisioningFeatureDefinition> select(List<ProvisioningFeatureDefinition> defs, Selector<ProvisioningFeatureDefinition> selector) {
        List<ProvisioningFeatureDefinition> selected = new ArrayList<>(defs.size());
        for (ProvisioningFeatureDefinition def : defs) {
            if ((selector == null) || selector.test(def)) {
                selected.add(def);
            }
        }
        return selected;
    }

    private final Collection<String> restricted;
    private final Collection<String> restrictedAttempts;

    @Override
    public List<ProvisioningFeatureDefinition> getFeatures() {
        return select(repo,
                      new Selector<ProvisioningFeatureDefinition>() {
                          @Override
                          public boolean test(ProvisioningFeatureDefinition def) {
                              return (!restricted.contains(def.getSymbolicName()));
                          }
                      });
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

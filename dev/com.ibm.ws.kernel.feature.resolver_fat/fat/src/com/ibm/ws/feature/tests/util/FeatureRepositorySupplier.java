/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.feature.tests.util;

import java.util.Collection;

import com.ibm.ws.kernel.feature.internal.util.VerifyDelta;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;

public class FeatureRepositorySupplier implements VerifyDelta.FeatureSupplier {
    public FeatureRepositorySupplier(FeatureResolver.Repository repo) {
        this.repo = repo;
    }

    private final FeatureResolver.Repository repo;

    @Override
    public String getVisibility(String featureName) {
        ProvisioningFeatureDefinition featureDef = repo.getFeature(featureName);
        return ((featureDef == null) ? "MISSING" : featureDef.getVisibility().toString());
    }

    public static final String NO_SHIP_SYM_NAME = "io.openliberty.noShip-1.0";

    public static final String NO_SHIP_SHORT_NAME = "noShip-1.0";

    @Override
    public boolean isNoShip(String featureName) {
        return (featureName.equals(NO_SHIP_SYM_NAME) || featureName.equals(NO_SHIP_SHORT_NAME));
    }

    @Override
    public boolean dependsOnNoShip(String featureName) {
        ProvisioningFeatureDefinition featureDef = repo.getFeature(featureName);
        if (featureDef == null) {
            return false;
        }

        Collection<FeatureResource> constituents = featureDef.getConstituents(SubsystemContentType.FEATURE_TYPE);
        for (FeatureResource constituent : constituents) {
            String symName = constituent.getSymbolicName();
            if ((symName != null) && symName.equals(NO_SHIP_SYM_NAME)) {
                return true;
            }
        }
        return false;
    }
}
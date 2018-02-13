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

import java.util.List;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants;
import com.ibm.ws.repository.resolver.internal.resource.FeatureResource;

public class StopAutoFeaturesInstallingTheirRequiredCapabilities implements ResolutionFilter {

    /** {@inheritDoc} */
    @Override
    public boolean allowResolution(Requirement requirement, List<Capability> potentialProviders) {
        if (!isRequirementAnAutoFeatureRequiringAProvisionCapability(requirement)) {
            return true;
        }
        if (potentialProviders.isEmpty()) {
            return false;
        }
        Capability capability = potentialProviders.get(0);
        return isCapabilitySuppliedByAnAllowedResource(capability);
    }

    private boolean isRequirementAnAutoFeatureRequiringAProvisionCapability(Requirement requirement) {
        return InstallableEntityIdentityConstants.CLASSIFIER_AUTO.equals(requirement.getDirectives().get(IdentityNamespace.REQUIREMENT_CLASSIFIER_DIRECTIVE));
    }

    private boolean isCapabilitySuppliedByAnAllowedResource(Capability capability) {
        Resource resource = capability.getResource();
        if (resource instanceof FeatureResource) {
            if (!((FeatureResource) resource).isAutoFeatureThatShouldBeInstalledWhenSatisfied()) {
                return false;
            }
        }
        return true;
    }

}

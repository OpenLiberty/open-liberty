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
package com.ibm.ws.kernel.feature.internal.subsystem;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.kernel.feature.FeatureDefinition;
import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils.ProvisioningDetails;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.wsspi.logging.Introspector;

/**
 * Introspector implementation that prints server feature metadata
 * in the event of a dump
 */

@Component
public class FeatureIntrospector implements Introspector {

    private final FeatureProvisioner provisioner;

    @Activate
    public FeatureIntrospector(@Reference FeatureProvisioner provisioner) {
        this.provisioner = provisioner;
    }

    @Override
    public String getIntrospectorName() {
        return "FeatureIntrospector";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Lists all the features enabled in the server.";
    }

    /**
     * Requests the set of features that are installed on the server and outputs
     * relevant data
     *
     * @param PrintWriter associated with the feature introspection file
     */
    @Override
    public void introspect(PrintWriter out) throws Exception {
        Set<String> features = this.provisioner.getInstalledFeatures();
        for (String feature : features) {
            FeatureDefinition featureDef = this.provisioner.getFeatureDefinition(feature);
            out.println(featureDef.getFeatureName());
            out.println("    Visibility: " + featureDef.getVisibility());
            out.println("    Version: " + featureDef.getVersion());
            out.println("    Simple Name: " + featureDef.getFeatureName());
            out.println("    Symbolic Name: " + featureDef.getSymbolicName());
            if (featureDef instanceof ProvisioningFeatureDefinition) {

                ProvisioningFeatureDefinition proFeatDef = (ProvisioningFeatureDefinition) featureDef;

                String bundleRepType = proFeatDef.getBundleRepositoryType();
                out.println("    Bundle Repository Type: " + (bundleRepType == null || bundleRepType.isEmpty() ? "core" : bundleRepType));
                out.println("    Auto Feature: " + proFeatDef.isAutoFeature());
                out.println("    Singleton: " + proFeatDef.isSingleton());

                if (proFeatDef instanceof SubsystemFeatureDefinitionImpl) {
                    SubsystemFeatureDefinitionImpl subSysFeatureDef = (SubsystemFeatureDefinitionImpl) proFeatDef;
                    ProvisioningDetails details = subSysFeatureDef.getProvisioningDetails();
                    boolean detailsAreSet = false;
                    try {
                        //Create provisioning details if not already set
                        if (details == null) {
                            try {
                                details = new ProvisioningDetails(subSysFeatureDef.getImmutableAttributes().featureFile, null);
                                details.setImmutableAttributes(subSysFeatureDef.getImmutableAttributes());
                                subSysFeatureDef.setProvisioningDetails(details);
                                detailsAreSet = true;
                            } catch (IOException e) {
                                //AutoFFDC is fine here
                            }
                        }
                        boolean isSuperseded = details.isSuperseded();
                        out.println("    Superseded: " + isSuperseded);
                        if (isSuperseded) {
                            out.println("        Superseded by: " + details.getSupersededBy());
                        }
                        Collection<FeatureResource> constituents = details.getConstituents(null);
                        out.println("    Constituents: ");
                        for (FeatureResource constituent : constituents) {
                            List<String> tolerates = constituent.getTolerates();
                            String toleratesString = tolerates == null ? "" : ": tolerates:=" + tolerates;
                            out.println("        " + constituent.getSymbolicName() + " " + toleratesString);
                            out.println("            " + constituent.getAttributes().toString());
                        }
                    } finally {
                        if (detailsAreSet) {
                            subSysFeatureDef.setProvisioningDetails(null);
                        }
                    }
                }
            }
        }
    }
}
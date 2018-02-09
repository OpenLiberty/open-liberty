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
package com.ibm.ws.install.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.omg.CORBA.IntHolder;

import com.ibm.ws.install.internal.asset.UninstallAsset;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;

/**
 * This class provides dependency checking for the features.
 */
public class FeatureDependencyChecker {

    /**
     * Verify the name is on the uninstall list
     *
     * @param name symbolic name of the feature
     * @param list list of the uninstalling features
     * @return true if the feature is going to be uninstalled, otherwise, return false.
     */
    public boolean toBeUninstalled(String name, List<UninstallAsset> list) {
        for (UninstallAsset asset : list) {
            String featureName = InstallUtils.getShortName(asset.getProvisioningFeatureDefinition());
            if (asset.getName().equals(name) ||
                (featureName != null && featureName.equals(name))) {
                InstallLogUtils.getInstallLogger().log(Level.FINEST, "The dependent feature is specified to be uninstalled : " + featureName);
                return true;
            }
        }
        return false;
    }

    public boolean toBeUninstalled(Collection<ProvisioningFeatureDefinition> features, List<UninstallAsset> list) {
        for (ProvisioningFeatureDefinition feature : features) {
            if (!!!toBeUninstalled(feature.getSymbolicName(), list)) {
                return false;
            }
        }
        return true;
    }

    public Collection<ProvisioningFeatureDefinition> getNotToBeUninstall(Collection<ProvisioningFeatureDefinition> features, List<UninstallAsset> list) {
        Collection<ProvisioningFeatureDefinition> notToBeUninstalled = new ArrayList<ProvisioningFeatureDefinition>();
        for (ProvisioningFeatureDefinition p : features) {
            if (!!!toBeUninstalled(p.getSymbolicName(), list)) {
                notToBeUninstalled.add(p);
            }
        }
        return notToBeUninstalled;
    }

    /**
     * Verfiy whether the feature is uninstallable and there is no other installed
     * features still require this feature.
     *
     * @param uninstallAsset feature to be uninstalled
     * @param installedFeatures installed features
     * @return the name/symbolic name of the feature which still requires the uninstalling feature.
     *         Return null if there is no other features still require the uninstalling feature.
     */
    public Collection<ProvisioningFeatureDefinition> isUninstallable(UninstallAsset uninstallAsset, Collection<ProvisioningFeatureDefinition> installedFeatureDefinitions,
                                                                     Collection<String> uninstallInstallFeatures, boolean isChecking) {
        Collection<ProvisioningFeatureDefinition> requiredByTheseFeatures = requiresThisFeature(uninstallAsset.getProvisioningFeatureDefinition().getSymbolicName(),
                                                                                                installedFeatureDefinitions, uninstallInstallFeatures, isChecking);
        Collection<ProvisioningFeatureDefinition> temp = new ArrayList<ProvisioningFeatureDefinition>();
        while (requiredByTheseFeatures.size() > temp.size()) {
            temp.addAll(requiredByTheseFeatures);
            for (ProvisioningFeatureDefinition p : temp) {
                InstallLogUtils.getInstallLogger().log(Level.FINEST,
                                                       "The uninstalling feature : " + uninstallAsset.getProvisioningFeatureDefinition().getSymbolicName() + " is required by "
                                                                     + p.getSymbolicName());
                InstallLogUtils.getInstallLogger().log(Level.FINEST, "Determine additional dependency for feature : " + p.getSymbolicName());
                Collection<ProvisioningFeatureDefinition> required = requiresThisFeature(p.getSymbolicName(),
                                                                                         installedFeatureDefinitions, uninstallInstallFeatures, isChecking);
                if (!!!required.isEmpty()) {
                    for (ProvisioningFeatureDefinition pp : required) {
                        if (!!!requiredByTheseFeatures.contains(pp)) {
                            InstallLogUtils.getInstallLogger().log(Level.FINEST, "Found additional dependent feature : " + pp.getSymbolicName());
                            requiredByTheseFeatures.add(pp);
                        }
                    }
                }
            }
        }
        return requiredByTheseFeatures;
    }

    /**
     * Determine the order of the features according to their dependency
     * Use DFS approach to solve the Topological Sort problem.
     * visited map will be used to store the order of the asset
     * the DFS will search for all the assets without required features, and work its way backward to find the order.
     *
     * @param list list of the features
     * @return the sorted feature list according to the feature dependency
     */
    public List<UninstallAsset> determineOrder(List<UninstallAsset> list) {
        Map<String, Integer> visited = new HashMap<String, Integer>();
        Map<String, UninstallAsset> assetsMap = new HashMap<String, UninstallAsset>();
        for (UninstallAsset ua : list) {
            assetsMap.put(ua.getProvisioningFeatureDefinition().getSymbolicName(), ua);
        }
        IntHolder order = new IntHolder(list.size());
        for (UninstallAsset ua : list) {
            if (!!!visited.containsKey(ua.getProvisioningFeatureDefinition().getSymbolicName()))
                DFS(ua, visited, assetsMap, order);
        }

        Collections.sort(list, new FeatureDependencyComparator(visited));
        return list;
    }

    private void DFS(UninstallAsset asset, Map<String, Integer> visited, Map<String, UninstallAsset> assetsMap, IntHolder order) {

        visited.put(asset.getProvisioningFeatureDefinition().getSymbolicName(), -1);
        for (FeatureResource fr : asset.getProvisioningFeatureDefinition().getConstituents(null)) {
            UninstallAsset ua = assetsMap.get(fr.getSymbolicName());
            if (ua != null && !!!visited.containsKey(ua.getProvisioningFeatureDefinition().getSymbolicName()))
                DFS(ua, visited, assetsMap, order);
        }
        visited.put(asset.getProvisioningFeatureDefinition().getSymbolicName(), order.value);
        order.value--;

    }

    private static Collection<ProvisioningFeatureDefinition> requiresThisFeature(String symbolicName, Collection<ProvisioningFeatureDefinition> installedFeatureDefinitions,
                                                                                 Collection<String> uninstallingInstallFeatures, boolean isChecking) {
        Collection<ProvisioningFeatureDefinition> requiredByThese = requiresThisFeatureByVisibility(symbolicName, installedFeatureDefinitions, null);
        Collection<ProvisioningFeatureDefinition> includePublicOnly = new ArrayList<ProvisioningFeatureDefinition>();
        for (ProvisioningFeatureDefinition requiredByThis : requiredByThese) {
            if (requiredByThis.getVisibility() == null || (!!!requiredByThis.getVisibility().equals(Visibility.PUBLIC)
                                                           && !!!requiredByThis.getVisibility().equals(Visibility.INSTALL))) {
                Collection<ProvisioningFeatureDefinition> requiredByThesePublic = requiresThisFeatureByVisibility(requiredByThis.getSymbolicName(), installedFeatureDefinitions,
                                                                                                                  Visibility.PUBLIC);
                if (requiredByThesePublic.isEmpty()) {
                    InstallLogUtils.getInstallLogger().log(Level.FINEST, "Cannot locate the public feature which requires this feature : " + requiredByThis.getSymbolicName());
                } else {
                    includePublicOnly.addAll(requiredByThesePublic);
                }
            } else if (isChecking && requiredByThis.getVisibility() != null && requiredByThis.getVisibility().equals(Visibility.INSTALL)) {
                InstallLogUtils.getInstallLogger().log(Level.FINEST, "Ignore feature with install visibility during prereq checking : " + requiredByThis.getSymbolicName());
            } else if (!!!isChecking && requiredByThis.getVisibility() != null && requiredByThis.getVisibility().equals(Visibility.INSTALL)
                       && uninstallingInstallFeatures != null
                       && (uninstallingInstallFeatures.isEmpty() || uninstallingInstallFeatures.contains(requiredByThis.getSymbolicName()))) {
                InstallLogUtils.getInstallLogger().log(Level.FINEST, "Ignore feature with install visibility : " + requiredByThis.getSymbolicName());
            } else {
                includePublicOnly.add(requiredByThis);
            }
        }
        return includePublicOnly;
    }

    private static Collection<ProvisioningFeatureDefinition> requiresThisFeatureByVisibility(String symbolicName,
                                                                                             Collection<ProvisioningFeatureDefinition> installedFeatureDefinitions,
                                                                                             Visibility visibility) {
        Collection<ProvisioningFeatureDefinition> requiredByThese = new ArrayList<ProvisioningFeatureDefinition>();
        for (ProvisioningFeatureDefinition fd : installedFeatureDefinitions) {
            if (visibility == null || (fd.getVisibility() != null && fd.getVisibility().equals(visibility))
                || (fd.getVisibility() != null && fd.getVisibility().equals(Visibility.INSTALL))) {
                if (fd.getSymbolicName() != symbolicName) {
                    for (FeatureResource fr : fd.getConstituents(null)) {
                        SubsystemContentType type = fr.getType();
                        if (SubsystemContentType.FEATURE_TYPE == type) {
                            if (symbolicName.equals(fr.getSymbolicName())) {
                                requiredByThese.add(fd);
                            }
                        }
                    }
                }
            }
        }
        return requiredByThese;
    }

    @SuppressWarnings("serial")
    static class FeatureDependencyComparator implements Comparator<UninstallAsset>, Serializable {

        private Map<String, Integer> orderMap;

        public FeatureDependencyComparator() {}

        public FeatureDependencyComparator(Map<String, Integer> orderMap) {
            this.orderMap = orderMap;
        }

        /** {@inheritDoc} */
        @Override
        public int compare(UninstallAsset feature1, UninstallAsset feature2) {
            int feature1Order = orderMap.get(feature1.getProvisioningFeatureDefinition().getSymbolicName());
            int feature2Order = orderMap.get(feature2.getProvisioningFeatureDefinition().getSymbolicName());
            if (feature1Order < feature2Order)
                return -1;
            else if (feature1Order == feature2Order)
                return 0;
            else
                return 1;
        }
    }
}

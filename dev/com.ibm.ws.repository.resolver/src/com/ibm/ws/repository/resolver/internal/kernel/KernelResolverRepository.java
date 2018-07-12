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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.Version;

import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.resources.ApplicableToProduct;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;

/**
 * Implementation of {@link FeatureResolver.Repository} which is backed by a collection of {@link EsaResource}s.
 */
public class KernelResolverRepository implements FeatureResolver.Repository {

    /**
     * Sorts features in descending order by their versions (i.e. most recent version first)
     * <p>
     * Features without a version are put after those with a version.
     */
    private final static Comparator<ProvisioningFeatureDefinition> VERSION_COMPARATOR = new VersionComparator();

    private final Map<String, List<ProvisioningFeatureDefinition>> symbolicNameToFeature = new HashMap<>();
    private final Map<String, String> publicNameToSymbolicName = new HashMap<>();
    private final List<ProvisioningFeatureDefinition> autoFeatures = new ArrayList<>();
    private final Map<String, Version> symbolicNameToPreferredVersion = new HashMap<>();
    private final Map<String, List<ApplicableToProduct>> nameToNonApplicableResources = new HashMap<>();

    private final Collection<ProductDefinition> productDefinitions;
    private final RepositoryConnectionList repositoryConnection;

    public KernelResolverRepository(Collection<ProductDefinition> productDefinitions, RepositoryConnectionList repositoryConnection) {
        this.repositoryConnection = repositoryConnection;

        if (productDefinitions == null) {
            this.productDefinitions = Collections.emptyList();
        } else {
            this.productDefinitions = productDefinitions;
        }
    }

    public void addFeatures(Collection<? extends EsaResource> esas) {
        for (EsaResource esa : esas) {
            addFeature(esa);
        }
    }

    public void addFeature(EsaResource esa) {
        KernelResolverEsa resolverEsa = new KernelResolverEsa(esa);
        addFeature(resolverEsa);
    }

    public void addInstalledFeatures(Collection<? extends ProvisioningFeatureDefinition> features) {
        for (ProvisioningFeatureDefinition feature : features) {
            addFeature(feature);
        }
    }

    public void addFeature(ProvisioningFeatureDefinition feature) {
        List<ProvisioningFeatureDefinition> featureList = symbolicNameToFeature.get(feature.getSymbolicName());
        if (featureList == null) {
            featureList = new ArrayList<>();
            symbolicNameToFeature.put(feature.getSymbolicName(), featureList);
        }

        // If the repository contains any installed features with this symbolic name,
        // don't add the new feature
        if (listContainsInstalledFeatures(featureList)) {
            return;
        }

        // If we already have a feature with this symbolic name and version, ignore the duplicate
        if (listContainsDuplicate(featureList, feature)) {
            return;
        }

        // If this is an installed feature, wipe out any repository features added earlier
        if (!(feature instanceof KernelResolverEsa)) {
            featureList.clear();
        }

        featureList.add(feature);
        Collections.sort(featureList, VERSION_COMPARATOR);

        // It's essential that we can always look up a feature by its feature name, the kernel resolver relies on this
        publicNameToSymbolicName.put(feature.getFeatureName().toLowerCase(), feature.getSymbolicName());

        if (feature.getVisibility() == Visibility.PUBLIC || feature.getVisibility() == Visibility.INSTALL) {
            publicNameToSymbolicName.put(feature.getSymbolicName().toLowerCase(), feature.getSymbolicName());
            if (feature.getIbmShortName() != null) {
                publicNameToSymbolicName.put(feature.getIbmShortName().toLowerCase(), feature.getSymbolicName());
            }
        }

        if (feature.isAutoFeature()) {
            autoFeatures.add(feature);
        }
    }

    /**
     * Checks whether {@code featureList} contains a feature with the same name and version as {@code feature}.
     *
     * @param featureList the list of features
     * @param feature the feature
     * @return {@code true} if {@code featureList} contains a feature with the same symbolic name and version as {@code feature}, otherwise {@code false}
     */
    private boolean listContainsDuplicate(List<ProvisioningFeatureDefinition> featureList, ProvisioningFeatureDefinition feature) {
        for (ProvisioningFeatureDefinition f : featureList) {
            if (hasSameSymbolicNameAndVersion(feature, f)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSameSymbolicNameAndVersion(ProvisioningFeatureDefinition f1, ProvisioningFeatureDefinition f2) {
        if (f1.getSymbolicName() == null) {
            if (f2.getSymbolicName() != null) {
                return false;
            }
        } else if (!f1.getSymbolicName().equals(f2.getSymbolicName())) {
            return false;
        }

        if (f1.getVersion() == null) {
            if (f2.getVersion() != null) {
                return false;
            }
        } else if (!f1.getVersion().equals(f2.getVersion())) {
            return false;
        }
        return true;
    }

    @Override
    public Collection<ProvisioningFeatureDefinition> getAutoFeatures() {
        return Collections.unmodifiableList(autoFeatures);
    }

    @Override
    public List<String> getConfiguredTolerates(String featureName) {
        return Collections.emptyList();
    }

    @Override
    public ProvisioningFeatureDefinition getFeature(String featureName) {
        ProvisioningFeatureDefinition feature = getCachedFeature(featureName);

        if (feature == null) {
            cacheFeaturesForName(featureName);
            feature = getCachedFeature(featureName);
        }

        return feature;
    }

    /**
     * Get a feature by name, but without going and checking the remote repository if we don't know about it
     *
     * @see #getFeature(String)
     */
    private ProvisioningFeatureDefinition getCachedFeature(String featureName) {
        List<ProvisioningFeatureDefinition> featureList = symbolicNameToFeature.get(featureName);

        if (featureList == null) {
            featureName = publicNameToSymbolicName.get(featureName.toLowerCase());
            if (featureName != null) {
                featureList = symbolicNameToFeature.get(featureName);
            }
        }

        if (featureList == null || featureList.isEmpty()) {
            return null;
        }

        return getPreferredVersion(featureName, featureList);
    }

    /**
     * Go to the repository (if available) and find all the features for the given name
     * <p>
     * Each feature that's applicable is then added to the repository, the rest are added to {@link #nameToNonApplicableResources}
     * <p>
     * This is needed to allow us to find features with a blank appliesTo (which aren't returned by {@link RepositoryConnectionList#getMatchingEsas(ProductDefinition)}) and to give
     * better error messages where the user has tried to install a feature which is available but which isn't applicable to their products.
     *
     * @param featureName the short or symbolic name of the feature
     */
    private void cacheFeaturesForName(String featureName) {
        if (nameToNonApplicableResources.containsKey(featureName)) {
            return; // We've already cached all features for this name
        }

        if (repositoryConnection != null) {
            List<ApplicableToProduct> nonApplicable = new ArrayList<>();

            for (RepositoryResource resource : getResourcesForName(featureName)) {
                if (!isApplicable(resource)) {
                    nonApplicable.add((ApplicableToProduct) resource); // Safe cast as both samples and features are ApplicableToProducts
                } else if (resource.getType() == ResourceType.FEATURE) {
                    addFeature((EsaResource) resource);
                }
            }

            nameToNonApplicableResources.put(featureName, nonApplicable);
        }
    }

    /**
     * Fetch the feature and sample resources that match the given name, whether or not they're applicable to the current product
     *
     * @param resourceName the short or symbolic name to look for
     * @return the resources that match that name
     */
    private List<RepositoryResource> getResourcesForName(String resourceName) {
        List<RepositoryResource> results = new ArrayList<>();
        try {
            results.addAll(repositoryConnection.getMatchingEsas(FilterableAttribute.SYMBOLIC_NAME, resourceName));
            results.addAll(repositoryConnection.getMatchingEsas(FilterableAttribute.LOWER_CASE_SHORT_NAME, resourceName.toLowerCase()));
            results.addAll(repositoryConnection.getMatchingSamples(FilterableAttribute.LOWER_CASE_SHORT_NAME, resourceName.toLowerCase()));
        } catch (RepositoryBackendException e) {
            // Don't worry if we fail to contact the repository
            // worst case is we report a resource as missing rather than available on another version/edition
        }
        return results;
    }

    /**
     * Return whether the given resource is applicable to the current product definitions.
     *
     * @param resource the repository resource
     * @return {@code true} if the resource is applicable, otherwise {@code false}
     */
    private boolean isApplicable(RepositoryResource resource) {
        if (resource instanceof ApplicableToProduct) {
            if (((ApplicableToProduct) resource).getAppliesTo() == null) {
                return true; // No appliesTo -> applicable
            }
        }
        return ((RepositoryResourceImpl) resource).doesResourceMatch(productDefinitions, null);
    }

    /**
     * Return all features and samples in the repository with the given name which don't apply to the installed products
     *
     * @param resourceName a short or symbolic name
     * @return the features and samples which don't apply to the installed products
     */
    public Collection<ApplicableToProduct> getNonApplicableResourcesForName(String resourceName) {
        List<ApplicableToProduct> result = nameToNonApplicableResources.get(resourceName);

        if (result == null) {
            // We don't expect this to happen, if we're looking for non-applicable resources, it's because we failed to resolve it earlier
            cacheFeaturesForName(resourceName);
            result = nameToNonApplicableResources.get(resourceName);
        }

        if (result == null) {
            // Still null, very odd
            return Collections.emptySet();
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Return all the features in the repository, except where two features have the same symbolic name
     * <p>
     * If two features exist with the same symbolic name, only the one that would be returned from {@link #getFeature(String)} will be returned here.
     *
     * @return a collection features
     */
    public Collection<ProvisioningFeatureDefinition> getAllFeatures() {
        List<ProvisioningFeatureDefinition> result = new ArrayList<>();
        for (Entry<String, List<ProvisioningFeatureDefinition>> entry : symbolicNameToFeature.entrySet()) {
            result.add(getPreferredVersion(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * Set the preferred version of the given feature
     * <p>
     * This is for when a user requests that a specific version should be installed.
     * <p>
     * If the feature is not in the repository or the version does not parse, this method does nothing.
     * <p>
     * When a preferred version is set, {@link #getFeature(String)} will return the preferred version if available, unless another version is already installed.
     *
     * @param featureName the short or symbolic feature name
     * @param version the version
     */
    public void setPreferredVersion(String featureName, String version) {
        if (!symbolicNameToFeature.containsKey(featureName)) {
            featureName = publicNameToSymbolicName.get(featureName);
        }

        if (featureName != null) {
            try {
                Version v = Version.parseVersion(version);
                symbolicNameToPreferredVersion.put(featureName, v);
            } catch (IllegalArgumentException ex) {
            }
        }
    }

    /**
     * Clear any preferred versions set with {@link #setPreferredVersion(String, String)}
     */
    public void clearPreferredVersions() {
        symbolicNameToPreferredVersion.clear();
    }

    /**
     * Find the preferred version of a feature from the list of features
     * <p>
     * The decision is made by consulting {@link #symbolicNameToPreferredVersion} to find out whether the user has configured a preferred version. If so, look for a feature with
     * that version.
     * <p>
     * If no preferred version has been configured for this symbolic name, or if the preferred version cannot be found in the list, return the latest version.
     *
     * @param symbolicName the symbolic name of the feature
     * @param featureList the list of features, which should all have the same symbolic name
     * @return the best feature from the list
     */
    private ProvisioningFeatureDefinition getPreferredVersion(String symbolicName, List<ProvisioningFeatureDefinition> featureList) {
        Version preferredVersion = symbolicNameToPreferredVersion.get(symbolicName);
        ProvisioningFeatureDefinition result = null;

        if (preferredVersion != null) {
            for (ProvisioningFeatureDefinition feature : featureList) {
                if (preferredVersion.equals(feature.getVersion())) {
                    result = feature;
                    break;
                }
            }
        }

        if (result == null) {
            result = featureList.iterator().next();
        }

        return result;
    }

    /**
     * Return whether the list of features contain any from an installed product (rather than from the repository)
     * <p>
     * This is done by checking whether the {@link ProvisioningFeatureDefinition} is actually a {@link KernelResolverEsa} (which wraps an {@link EsaResource}).
     *
     * @param featureList the list of features to check
     * @return {@code false} if all the features in the list are of type {@link KernelResolverEsa}, otherwise returns {@code true}
     */
    private static boolean listContainsInstalledFeatures(List<ProvisioningFeatureDefinition> featureList) {
        for (ProvisioningFeatureDefinition feature : featureList) {
            if (!(feature instanceof KernelResolverEsa)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Sorts features in descending order by their versions (i.e. most recent version first)
     * <p>
     * Features without a version are put after those with a version.
     */
    private static class VersionComparator implements Comparator<ProvisioningFeatureDefinition> {

        @Override
        public int compare(ProvisioningFeatureDefinition o1, ProvisioningFeatureDefinition o2) {
            Version v1 = o1.getVersion();
            Version v2 = o2.getVersion();
            if (v1 == null) {
                if (v2 == null) {
                    return 0;
                } else {
                    return 1;
                }
            } else {
                return -v1.compareTo(v2);
            }
        }

    }

}

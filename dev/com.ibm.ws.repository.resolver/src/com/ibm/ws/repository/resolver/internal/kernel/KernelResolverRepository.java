/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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
@SuppressWarnings("restriction")
// Ignore restricted use of RepositoryResourceImpl.
// It's ok here because the resolver doesn't run inside OSGi.
public class KernelResolverRepository implements FeatureResolver.Repository {

    private Collection<ProvisioningFeatureDefinition> allFeatureCache;

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

    // @formatter:off

    /**
     * Add a feature to the resolver repository.
     *
     * Accept at most one installed feature. Addition of the first
     * installed feature removes and previously added ESA features.
     *
     * Sort features which have the same symbolic name (which must be
     * ESA features) by version, from highest to lowest.
     *
     * Map the feature name to the feature symbolic name, map the feature
     * short name to the feature symbolic name, and map the feature symbolic
     * name in all lower case to the feature symbolic name.
     *
     * If the feature is an auto-feature, add it to the collection of
     * auto-features.
     * 
     * Clear the preferred features collection. It must be recomputed.
     *
     * @param feature The feature which is to be added.
     */
    public void addFeature(ProvisioningFeatureDefinition feature) {
        String symbolicName = feature.getSymbolicName();

        List<ProvisioningFeatureDefinition> features =
            symbolicNameToFeature.computeIfAbsent(symbolicName,
                                                  (String useName) -> new ArrayList<>());

        // Don't add the feature if an installed feature is already present
        // with the symbolic name.
        //
        // If the list was just created, it cannot contain any installed features.

        if (containsInstalledFeatures(features)) {
            return;
        }

        // If the new feature is an installed feature, clear out the features
        // already present.
        //
        // Otherwise, if we already have an ESA feature with this symbolic name
        // and version, ignore the duplicate

        if (!(feature instanceof KernelResolverEsa)) {
            features.clear();
        } else if (containsDuplicate(features, feature)) {
            // TODO: How is this possible?  That would mean
            //       the features have the same symbolic names and
            //       different versions.  But isn't the version a part
            //       of the symbolic name?
            //
            // For example:
            //   Subsystem-Name: Java Servlets 4.0
            //   IBM-ShortName: servlet-4.0
            //   Subsystem-SymbolicName: com.ibm.websphere.appserver.servlet-4.0;
            //    visibility:=public;
            //    singleton:=true

            return;
        }

        features.add(feature);

        if (features.size() > 1) {
            // From highest to lowest.  Only possible when there are multiple
            // ESA features present.
            Collections.sort(features, KernelResolverRepository::compare);
        }

        // It's essential that we can always look up a feature by its feature name,
        // the kernel resolver relies on this.
        publicNameToSymbolicName.put(feature.getFeatureName().toLowerCase(), symbolicName);

        if ((feature.getVisibility() == Visibility.PUBLIC) ||
            (feature.getVisibility() == Visibility.INSTALL)) {

            publicNameToSymbolicName.put(symbolicName.toLowerCase(), symbolicName);

            if (feature.getIbmShortName() != null) {
                publicNameToSymbolicName.put(feature.getIbmShortName().toLowerCase(), symbolicName);
            }
        }

        if (feature.isAutoFeature()) {
            autoFeatures.add(feature);
        }

        allFeatureCache = null;
    }

    // @formatter:on

    /**
     * Compare two features by version, from highest to lowest.
     * (This inverts the usual comparison result.)
     *
     * Features without a version are put after those with a version.
     *
     * See {@link Version#compareTo}.
     *
     * @param o1 A feature which is to be compared.
     * @param o2 Another feature which is to be compared.
     *
     * @return The comparison result of the versions of the features, inverted.
     */
    private static int compare(ProvisioningFeatureDefinition o1, ProvisioningFeatureDefinition o2) {
        Version v1 = o1.getVersion();
        Version v2 = o2.getVersion();
        if (v1 == null) {
            if (v2 == null) {
                return 0;
            } else {
                return 1; // The one with a version is higher.
            }
        } else {
            if (v2 == null) {
                return -1; // The one with a version is higher.
            } else {
                return -v1.compareTo(v2); // Inverted!
            }
        }
    }

    @Override
    public Collection<ProvisioningFeatureDefinition> getAutoFeatures() {
        return Collections.unmodifiableList(autoFeatures);
    }

    @Override
    public List<String> getConfiguredTolerates(String featureName) {
        return Collections.emptyList();
    }

    /**
     * Answer all features of this repository.
     *
     * Answer the features in the order as provided by {@link #getAllFeatures()}.
     *
     * A new result collection is obtained on each invocation.
     *
     * What meaning the result collection has depends on whether there are
     * any ESA features present. Multiple ESA features can be present with
     * the same
     *
     * @return All of the features of this repository.
     */
    @Override
    public List<ProvisioningFeatureDefinition> getFeatures() {
        int numFeatures = 0;
        for (List<ProvisioningFeatureDefinition> features : symbolicNameToFeature.values()) {
            numFeatures += features.size();
        }
        List<ProvisioningFeatureDefinition> allFeatures = new ArrayList<>(numFeatures);
        for (List<ProvisioningFeatureDefinition> features : symbolicNameToFeature.values()) {
            allFeatures.addAll(features);
        }
        return allFeatures;
    }

    /**
     * Select features of this repository.
     *
     * Answer the features in the order as provided by {@link #getAllFeatures()}.
     *
     * A new result collection is obtained on each invocation, even when obtaining
     * the entire collection of features.
     *
     * @param selector The selector of features. If null, select all features.
     *
     * @return The selected features.
     */
    // @Override
    public List<ProvisioningFeatureDefinition> select(FeatureResolver.Selector<ProvisioningFeatureDefinition> selector) {
        // DO NOT USE 'getAllFeatures': That selects the preferred version of each feature.

        if (selector == null) {
            return getFeatures();
        }

        List<ProvisioningFeatureDefinition> selected = new ArrayList<>();
        for (List<ProvisioningFeatureDefinition> features : symbolicNameToFeature.values()) {
            for (ProvisioningFeatureDefinition feature : features) {
                if (selector.test(feature)) {
                    selected.add(feature);
                }
            }
        }
        return selected;
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
     * @param featureName the feature name
     * @return the feature with the given name, or {@code null} if we don't know about it
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
     * Answer the preferred versions of features in the repository.
     *
     * See {@link #getPreferredVersion(String, List)}.
     *
     * @return The preferred versions of all features in the repository.
     */
    public Collection<ProvisioningFeatureDefinition> getAllFeatures() {
        if ( allFeatureCache == null ) {
            List<ProvisioningFeatureDefinition> preferred = new ArrayList<>();
            symbolicNameToFeature.forEach(
                (String symbolicName, List<ProvisioningFeatureDefinition> features) ->
                    preferred.add(getPreferredVersion(symbolicName, features)));
            allFeatureCache = preferred;
        }
        return allFeatureCache;
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
     * @param version     the version
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
     * @param featureList  the list of features, which should all have the same symbolic name
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
     * Tell if the features contain any from an installed product (rather than
     * from the repository).
     *
     * This is done by testing if the feature is a {@link KernelResolverEsa},
     * which wraps an {@link EsaResource}.
     *
     * @param features the list of features to check
     *
     * @return True or false telling if any of the features is not a {@link KernelResolverEsa}.
     */
    private static boolean containsInstalledFeatures(List<ProvisioningFeatureDefinition> features) {
        for (ProvisioningFeatureDefinition feature : features) {
            if (!(feature instanceof KernelResolverEsa)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tell if any feature of a supplied list has the same symbolic name and version
     * as a supplied feature.
     *
     * @param features Features which are to be tested.
     * @param f1       The feature to test against.
     *
     * @return If any of the features has the same symbolic name and version as the
     *         supplied feature.
     */
    private static boolean containsDuplicate(List<ProvisioningFeatureDefinition> features,
                                             ProvisioningFeatureDefinition f1) {
        for (ProvisioningFeatureDefinition f2 : features) {
            if (isDuplicate(f1, f2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tell if the symbolic name and version of the two features are the same.
     *
     * @param f1 A feature to test.
     * @param f2 Another feature to test.
     *
     * @return True or false telling if the symbolic name and version of the two
     *         features are the same.
     */
    private static boolean isDuplicate(ProvisioningFeatureDefinition f1,
                                       ProvisioningFeatureDefinition f2) {

        String f1Sym = f1.getSymbolicName();
        String f2Sym = f2.getSymbolicName();

        if (f1Sym == null) {
            if (f2Sym != null) {
                return false;
            } else {
                // Compare versions
            }
        } else if ((f2Sym == null) || !f1Sym.equals(f2Sym)) {
            return false;
        } else {
            // Compare versions
        }

        Version f1Version = f1.getVersion();
        Version f2Version = f2.getVersion();

        if (f1Version == null) {
            return (f2Version == null);
        } else {
            return ((f2Version != null) && f1Version.equals(f2Version));
        }
    }
}

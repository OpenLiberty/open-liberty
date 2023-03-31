/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package com.ibm.ws.repository.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.ws.kernel.feature.internal.FeatureResolverImpl;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Chain;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.product.utility.extension.IFixUtils;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.resolver.RepositoryResolutionException.MissingRequirement;
import com.ibm.ws.repository.resolver.internal.ResolutionMode;
import com.ibm.ws.repository.resolver.internal.kernel.KernelResolverEsa;
import com.ibm.ws.repository.resolver.internal.kernel.KernelResolverRepository;
import com.ibm.ws.repository.resources.ApplicableToProduct;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.SampleResource;

/**
 * Resolves a list of names into lists of {@link RepositoryResource} to be installed
 * <p>
 * Note: Some methods have package visibility to allow unit testing
 */
public class RepositoryResolver {

    // ---
    // Static data which won't change after initialization
    KernelResolverRepository resolverRepository;
    Collection<ProvisioningFeatureDefinition> installedFeatures;
    Collection<ProvisioningFeatureDefinition> kernelFeatures;
    Map<String, SampleResource> sampleIndex;
    Collection<EsaResource> repoFeatures;
    Collection<SampleResource> repoSamples;
    RepositoryConnectionList repoConnections;
    Collection<ProductDefinition> installDefinition;
    Map<String, Collection<Chain>> featureConflicts;

    // ---
    // Fields computed during resolution
    /**
     * The feature names we will pass to the kernel resolver
     */
    Set<String> featureNamesToResolve;

    /**
     * The names passed to {@link #resolve(Collection)} which we think are features (rather than samples)
     */
    Set<String> requestedFeatureNames;

    /**
     * The list of samples the user has requested to install
     */
    List<SampleResource> samplesToInstall;

    /**
     * Map from symbolic name to feature for all features returned as resolved by the kernel resolver
     * <p>
     * Keyset is mutually exclusive with {@link #featuresMissing} and {@link #requirementsFoundForOtherProducts}
     */
    Map<String, ProvisioningFeatureDefinition> resolvedFeatures;

    /**
     * The list of extra features for which we need to generate install lists
     */
    List<ProvisioningFeatureDefinition> additionalInstallListRoots;

    /**
     * List of requested features that were reported missing by the kernel resolver and weren't found in the repository applicable to another product.
     * <p>
     * May include sample names if they were missing
     * <p>
     * Mutually exclusive with {@link #resolvedFeatures} and {@link #requirementsFoundForOtherProducts}
     */
    List<String> featuresMissing;

    /**
     * List of resources which would have resolved a failed dependency but they apply to the wrong product
     */
    List<ApplicableToProduct> resourcesWrongProduct;

    /**
     * List of requirements which couldn't be resolved but for which we found a solution that applied to the wrong product
     * <p>
     * Each requirement will be a symbolic name, feature name or sample name
     * <p>
     * Mutually exclusive with {@link #resolvedFeatures} and {@link #featuresMissing}
     */
    Set<String> requirementsFoundForOtherProducts;

    /**
     * Subset of the requirements that the user gave us which we couldn't resolve
     */
    List<String> missingTopLevelRequirements;

    /**
     * List of all the missing requirements we've found so far
     */
    List<MissingRequirement> missingRequirements;

    /**
     * <p>
     * Construct a new instance of a resolver for a current install. Note that calls to this method will load all of the resources from the repository so this can be a relatively
     * expensive operation and may block the thread for a few seconds so care should be taken when called from a UI.</p>
     * <p>In order to process what is already installed several details about the liberty installation must be passed in as parameters. These can all be obtained from utility
     * methods within the Liberty installation so suggested usage is as follows:</p>
     * <code>
     * // Get the information about the installed products (i.e. version and editions)</br>
     * Collection&lt;ProductInfo&gt; installedProducts = ProductInfo.getAllProductInfo();</br>
     * Collection&lt;ProductDefinition&gt; installedProductDefinitions = new HashSet&lt;ProductDefinition&gt;;</br>
     * for (ProductInfo productInfo : installedProducts) {</br>
     * &nbsp;&nbsp;installedProductDefinitions.add(new ProductInfoProductDefinition(productInfo));</br>
     * }</br>
     * </br>
     * // Get the information about the installed features</br>
     * ManifestFileProcessor mfp = new ManifestFileProcessor(); </br>
     * Map<String, ProvisioningFeatureDefinition> features = mfp.getFeatureDefinitions();</br>
     * Collection&lt;ProvisioningFeatureDefinition&gt; installedFeatures = features.values();</br>
     * </br>
     * // Get the information about the installed iFixes</br>
     * CommandConsole commandConsole = new DefaultCommandConsole(System.console(), System.out, System.err);</br>
     * Collection<IFixInfo> installedIFixes = IFixUtils.getInstalledIFixes(Utils.getInstallDir(), commandConsole);</br>
     * </br>
     * // Create the resolver</br>
     * RepositoryResolver resolver = RepositoryResolver.resolve(installedProductDefinitions,</br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;installedFeatures, installedFixes, loginInfo);</br>
     * </code>
     *
     * @param installDefinition Information about the product(s) installed such as ID and edition. Must not be <code>null</code>.
     * @param installedFeatures The features that are installed. Must not be <code>null</code>.
     * @param installedIFixes   No longer used, parameter retained for backwards compatibility
     * @param repoConnections   The connection to the repository
     * @throws RepositoryException If there is a connection error with the Massive repository
     * @see ProductInfo#getAllProductInfo()
     * @see IFixUtils#getInstalledIFixes(java.io.File, com.ibm.ws.product.utility.CommandConsole)
     */
    public RepositoryResolver(Collection<ProductDefinition> installDefinition,
                              Collection<ProvisioningFeatureDefinition> installedFeatures,
                              Collection<IFixInfo> installedIFixes,
                              RepositoryConnectionList repoConnections) throws RepositoryException {

        this.repoConnections = repoConnections;
        this.installDefinition = installDefinition;
        fetchFromRepository(installDefinition);
        this.installedFeatures = installedFeatures;
        this.kernelFeatures = getKernelFeatures(installedFeatures);
        indexSamples();
    }

    /**
     * Package constructor for unit tests
     * <p>
     * Allows resolution to be tested without connecting to a repository
     *
     * @param installedFeatures the features which are installed
     * @param repoFeatures      the features available in the repository
     * @param repoSamples       the samples available in the repository
     */
    RepositoryResolver(Collection<ProvisioningFeatureDefinition> installedFeatures,
                       Collection<? extends EsaResource> repoFeatures,
                       Collection<? extends SampleResource> repoSamples) {
        this.repoFeatures = new ArrayList<>(repoFeatures);
        this.repoSamples = new ArrayList<>(repoSamples);
        this.installedFeatures = installedFeatures;
        this.kernelFeatures = getKernelFeatures(installedFeatures);
        this.installDefinition = Collections.emptySet();

        indexSamples();
    }

    /**
     * Populates {@link #repoFeatures} and {@link #repoSamples} with resources from the repository which apply to the install definition.
     *
     * @param installDefinition information about the product(s) installed such as ID and edition. Must not be {@code null}.
     *
     * @throws RepositoryException if there's a problem connecting to the repository
     */
    @SuppressWarnings("unchecked")
    void fetchFromRepository(Collection<ProductDefinition> installDefinition) throws RepositoryException {
        Collection<ResourceType> interestingTypes = new HashSet<ResourceType>();
        interestingTypes.add(ResourceType.FEATURE);
        interestingTypes.add(ResourceType.OPENSOURCE);
        interestingTypes.add(ResourceType.PRODUCTSAMPLE);
        Map<ResourceType, Collection<? extends RepositoryResource>> resources = repoConnections.getResources(installDefinition, interestingTypes, null);

        Collection<EsaResource> features = (Collection<EsaResource>) resources.get(ResourceType.FEATURE);
        if (features != null) {
            repoFeatures = features;
        } else {
            repoFeatures = Collections.emptySet();
        }

        repoSamples = new ArrayList<>();
        Collection<SampleResource> samples = (Collection<SampleResource>) resources.get(ResourceType.PRODUCTSAMPLE);
        if (samples != null) {
            repoSamples.addAll(samples);
        }

        Collection<SampleResource> osiSamples = (Collection<SampleResource>) resources.get(ResourceType.OPENSOURCE);
        if (osiSamples != null) {
            repoSamples.addAll(osiSamples);
        }
    }

    void initializeResolverRepository(Collection<ProductDefinition> installDefintion) {
        resolverRepository = new KernelResolverRepository(installDefintion, repoConnections);
        resolverRepository.addInstalledFeatures(installedFeatures);
        resolverRepository.addFeatures(repoFeatures);
    }

    Collection<ProvisioningFeatureDefinition> getKernelFeatures(Collection<ProvisioningFeatureDefinition> installedFeatures) {
        List<ProvisioningFeatureDefinition> kernelFeatures = new ArrayList<>();
        for (ProvisioningFeatureDefinition feature : installedFeatures) {
            if (feature.isKernel()) {
                kernelFeatures.add(feature);
            }
        }
        return kernelFeatures;
    }

    /**
     * Populates {@link #sampleIndex}
     */
    void indexSamples() {
        sampleIndex = new HashMap<>();
        for (SampleResource sample : repoSamples) {
            if (sample.getShortName() != null) {
                sampleIndex.put(sample.getShortName().toLowerCase(), sample);
            }
        }
    }

    /**
     * Takes a list of feature and sample names that the user wants to install and returns the {@link RepositoryResource}s that should be installed.
     * <p>
     * This method attempts to install everything that could be needed once the requested features are installed. This means:
     * <ul>
     * <li>If a requested feature has a dependency which tolerates multiple versions of another feature, <b>all</b> tolerated versions of that other feature which are not installed
     * will be returned
     * <li>If an installed feature has a dependency which tolerates multiple versions of another feature, <b>all</b> tolerated versions of that other feature which are not
     * installed will be returned
     * <li>All autofeatures which can have their capabilities met by the combination of features already installed and those to be installed will be returned
     * </ul>
     * <p>
     * Note that the last two points mean that this method can return features which are in no-way related to the names in {@code toResolve}.
     * <p>
     * Note that this method will return as many features as necessary to ensure that after installing the resources returned from this method, any of the features installed which
     * are compatible will work if listed together in the server.xml and any relevant auto-features will be present. This will likely be more features than are required for
     * specific scenarios.
     * <p>
     * If you want to install just the <i>minimal</i> set of features required to start a server with a given set of features, you should use {@link #resolveAsSet(Collection)}
     * instead.
     *
     * @param toResolve A collection of the identifiers of the resources to resolve. It should be in the form:</br>
     *                      <code>{name}/{version}</code></br>
     *                      <p>Where the <code>{name}</code> can be either the symbolic name, short name or lower case short name of the resource and <code>/{version}</code> is
     *                      optional. The collection may contain a mixture of symbolic names and short names. Must not be <code>null</code> or empty.</p>
     * @return <p>A collection of ordered lists of {@link RepositoryResource}s to install. Each list represents a collection of resources that must be installed together or not
     *         at all. They should be installed in the iteration order of the list(s). Note that if a resource is required by multiple different resources then it will appear in
     *         multiple lists. For instance if you have requested to install A and B and A requires N which requires M and O whereas B requires Z that requires O then the returned
     *         collection will be (represented in JSON):</p>
     *         <code>
     *         [[M, O, N, A],[O, Z, B]]
     *         </code>
     *         <p>This method will not return <code>null</code> although it may return an empty collection if there isn't anything to install (i.e. it resolves to resources that
     *         are already installed)</p>
     *         <p>Every auto-feature will have it's own list in the collection, this is to stop the failure to install either an auto feature or one of it's dependencies from
     *         stopping everything from installing. Therefore if you have features A and B that are required to provision auto feature C and you ask to resolve A and B then this
     *         method will return:</p>
     *         <code>
     *         [[A],[B],[A,B,C]]
     *         </code>
     *
     * @throws RepositoryResolutionException If the resource cannot be resolved
     */
    public Collection<List<RepositoryResource>> resolve(Collection<String> toResolve) throws RepositoryResolutionException {
        return resolve(toResolve, ResolutionMode.IGNORE_CONFLICTS);
    }

    /**
     * Resolve a single name
     * <p>
     * Identical to {@code resolve(Collections.singleton(toResolve))}.
     *
     * @see #resolve(Collection)
     *
     * @param toResolve the identifier of the resource to resolve
     * @return A collection of ordered lists of resources to install
     * @throws RepositoryResolutionException
     */
    public Collection<List<RepositoryResource>> resolve(String toResolve) throws RepositoryResolutionException {
        return resolve(Collections.singleton(toResolve));
    }

    /**
     * Takes a list of feature names that the user wants to install and returns a minimal set of the {@link RepositoryResource}s that should be installed to allow those features to
     * start together in one server.
     * <p>
     * This method uses the same resolution logic that is used by the kernel at server startup to decide which features to start. Therefore calling this method with a list of
     * feature names and installing the resources returned will guarantee that a server which has the same list of feature names in its server.xml will start.
     * <p>
     * The caller must provide the full set of features from the server.xml, including those that are already installed, so that tolerated dependencies and auto-features can be
     * resolved correctly.
     * <p>
     * This method will fail if there's no valid set of dependencies for the required features that doesn't include conflicting versions of singleton features.
     * <p>
     * For example, {@code resolve(Arrays.asList("javaee-7.0", "javaee-8.0"))} would work but {@code resolveAsSet(Arrays.asList("javaee-7.0", "javaee-8.0"))} would fail because
     * javaee-7.0 and javaee-8.0 contain features which conflict with each other (and other versions are not tolerated).
     * <p>
     * This method guarantees that it will return all the features required to start the requested features but will not ensure that the requested features will work with features
     * which were already installed but were not requested in the call to this method.
     * <p>
     * For example, if {@code ejbLite-3.2} is already installed and {@code resolve(Arrays.asList("cdi-2.0"))} is called, it will not return the autofeature which would be required
     * for {@code cdi-2.0} and {@code ejbLite-3.2} to work together.
     *
     * @param toResolve A collection of the identifiers of the resources to resolve. It should be in the form:</br>
     *                      <code>{name}/{version}</code></br>
     *                      <p>Where the <code>{name}</code> can be either the symbolic name, short name or lower case short name of the resource and <code>/{version}</code> is
     *                      optional. The collection may contain a mixture of symbolic names and short names. Must not be <code>null</code> or empty.</p>
     * @return <p>A collection of ordered lists of {@link RepositoryResource}s to install. Each list represents a collection of resources that must be installed together or not
     *         at all. They should be installed in the iteration order of the list(s). Note that if a resource is required by multiple different resources then it will appear in
     *         multiple lists. For instance if you have requested to install A and B and A requires N which requires M and O whereas B requires Z that requires O then the returned
     *         collection will be (represented in JSON):</p>
     *         <code>
     *         [[M, O, N, A],[O, Z, B]]
     *         </code>
     *         <p>This will not return <code>null</code> although it may return an empty collection if there isn't anything to install (i.e. it resolves to resources that are
     *         already installed)</p>
     *         <p>Every auto-feature will have it's own list in the collection, this is to stop the failure to install either an auto feature or one of it's dependencies from
     *         stopping everything from installing. Therefore if you have features A and B that are required to provision auto feature C and you ask to resolve A and B then this
     *         method will return:</p>
     *         <code>
     *         [[A],[B],[A,B,C]]
     *         </code>
     *
     * @throws RepositoryResolutionException If the resource cannot be resolved
     */
    public Collection<List<RepositoryResource>> resolveAsSet(Collection<String> toResolve) throws RepositoryResolutionException {
        return resolve(toResolve, ResolutionMode.DETECT_CONFLICTS);
    }

    Collection<List<RepositoryResource>> resolve(Collection<String> toResolve, ResolutionMode resolutionMode) throws RepositoryResolutionException {
        initResolve();
        initializeResolverRepository(installDefinition);

        processNames(toResolve);

        if (resolutionMode == ResolutionMode.DETECT_CONFLICTS) {
            // Call the kernel resolver to determine the features needed
            resolveFeaturesAsSet();
        } else {
            // Resolve all dependencies of installed features
            resolveFeaturesBasic();
            // Basic resolve auto-features satisfied by installed + resolved features
            resolveAutoFeatures();
        }

        // Find any any features which aren't direct dependencies of a requested feature
        // Can happen if resolveAsSet is used to install some features
        // and then resolve is used to install more features in the same server
        computeAdditionalInstallListRoots();

        Collection<List<RepositoryResource>> installLists = createInstallLists();

        reportErrors();
        return installLists;

    }

    /**
     * Initialize all the fields used for a resolution
     */
    void initResolve() {
        featureNamesToResolve = new HashSet<>();
        samplesToInstall = new ArrayList<>();
        resolvedFeatures = new HashMap<>();
        requestedFeatureNames = new HashSet<>();
        featuresMissing = new ArrayList<>();
        resourcesWrongProduct = new ArrayList<>();
        requirementsFoundForOtherProducts = new HashSet<>();
        missingTopLevelRequirements = new ArrayList<>();
        missingRequirements = new ArrayList<>();
        resolverRepository = null;
        featureConflicts = new HashMap<>();
    }

    /**
     * Populates {@link #samplesToInstall} and {@link #featureNamesToResolve} by processing the list of names to resolve and identifying which are samples.
     *
     * @param namesToResolve the list of names to resolve
     */
    void processNames(Collection<String> namesToResolve) {
        for (String name : namesToResolve) {
            SampleResource sample = sampleIndex.get(name.toLowerCase());
            if (sample != null) {
                // Found a sample, add it and any required features
                samplesToInstall.add(sample);
                if (sample.getRequireFeature() != null) {
                    featureNamesToResolve.addAll(sample.getRequireFeature());
                }
            } else {
                // Name didn't match any samples, assume it's a feature name
                NameAndVersion nameAndVersion = splitRequestedNameAndVersion(name);
                if (nameAndVersion.version != null) {
                    resolverRepository.setPreferredVersion(nameAndVersion.name, nameAndVersion.version);
                }

                featureNamesToResolve.add(nameAndVersion.name);
                requestedFeatureNames.add(nameAndVersion.name);
            }
        }
    }

    /**
     * Users can request a specific version to be installed using the format {@code myFeature/1.2.0}.
     * <p>
     * This method splits the string around the first slash character and returns the results.
     *
     * @param nameAndVersion the feature name, as passed into {@link #resolve(Collection)}
     * @return the split name and version. The version component may be null.
     */
    private NameAndVersion splitRequestedNameAndVersion(String nameAndVersion) {
        String[] parts = nameAndVersion.split("/");
        if (parts.length > 2) {
            throw new IllegalArgumentException("Only one \"/\" symbol is allowed in the resourceString but it was " + nameAndVersion);
        }

        if (parts.length == 2) {
            return new NameAndVersion(parts[0], parts[1]);
        } else {
            return new NameAndVersion(nameAndVersion, null);
        }
    }

    /**
     * Uses the kernel resolver to resolve {@link #featureNamesToResolve} and populates {@link #resolvedFeatures} with the result.
     *
     * Populates:
     * - {@link #resolvedFeatures}
     * - {@link #featuresMissing}
     * - {@link #requirementsFoundForOtherProducts}
     * - {@link #resourcesWrongProduct}
     */
    void resolveFeaturesAsSet() {
        FeatureResolver resolver = new FeatureResolverImpl();
        Result result = resolver.resolveFeatures(resolverRepository, kernelFeatures, featureNamesToResolve, Collections.<String> emptySet(), false);

        featureConflicts.putAll(result.getConflicts());

        for (String name : result.getResolvedFeatures()) {
            ProvisioningFeatureDefinition feature = resolverRepository.getFeature(name);
            resolvedFeatures.put(feature.getSymbolicName(), feature);
        }

        for (String missingFeature : result.getMissing()) {
            recordMissingFeature(missingFeature);
        }
    }

    /**
     * Resolves {@link #featureNamesToResolve} using a simple traversal of the dependency tree
     *
     * Populates:
     * - {@link #resolvedFeatures}
     * - {@link #featuresMissing}
     * - {@link #requirementsFoundForOtherProducts}
     * - {@link #resourcesWrongProduct}
     */
    void resolveFeaturesBasic() {
        // For each feature to resolve
        //   Look up the PFD
        //   walk the dependency tree, adding each PFD to the resolvedFeatures set
        //   If all features from a tolerated dependency are missing, log as missing
        for (String name : featureNamesToResolve) {
            ProvisioningFeatureDefinition feature = resolverRepository.getFeature(name);
            if (feature == null) {
                recordMissingFeature(name);
                continue;
            }
            FeatureTreeWalker.walkOver(resolverRepository)
                             .forEach(f -> resolvedFeatures.put(f.getSymbolicName(), f))
                             .onMissingDependency((f, dep) -> recordMissingFeature(dep.getSymbolicName()))
                             .walkDepthFirst(feature);
        }

        // Walk dependencies of all installed features as well to ensure we install all their tolerated dependencies
        for (ProvisioningFeatureDefinition feature : installedFeatures) {
            FeatureTreeWalker.walkOver(resolverRepository)
                             .forEach(f -> resolvedFeatures.put(f.getSymbolicName(), f))
                             .onMissingDependency((f, dep) -> recordMissingFeature(dep.getSymbolicName()))
                             .useAutofeatureProvisionAsDependency(false)
                             .walkDepthFirst(feature);
        }
    }

    /**
     * Finds any autofeatures which are satisfied by the installed and resolved features and adds them to the resolvedFeatures set
     * <p>
     * This is only used for basic resolve because the kernel resolver will find autofeatures for us when using resolveAsSet
     */
    void resolveAutoFeatures() {

        HashSet<ProvisioningFeatureDefinition> resolvedAndInstalled = new HashSet<>();
        resolvedAndInstalled.addAll(installedFeatures);
        resolvedAndInstalled.addAll(resolvedFeatures.values());

        FeatureTreeWalker autofeatureWalker = FeatureTreeWalker.walkOver(resolverRepository)
                                                               .forEach(f -> {
                                                                   resolvedFeatures.put(f.getSymbolicName(), f);
                                                                   resolvedAndInstalled.add(f);
                                                               })
                                                               .onMissingDependency((f, dep) -> recordMissingFeature(dep.getSymbolicName()))
                                                               .useAutofeatureProvisionAsDependency(false);

        // Autofeatures can satisfy other autofeatures, so we loop,
        // looking for more satisfied autofeatures until we stop finding any
        int oldSize = 0;
        while (resolvedAndInstalled.size() > oldSize) {
            oldSize = resolvedAndInstalled.size();
            for (ProvisioningFeatureDefinition feature : resolverRepository.getAutoFeatures()) {
                EsaResource esa = getResource(feature);
                if (esa == null) {
                    continue; // Ignore already installed features
                }

                if (resolvedAndInstalled.contains(feature)) {
                    continue; // Ignore features we've already resolved
                }

                if (esa.getInstallPolicy() != InstallPolicy.WHEN_SATISFIED) {
                    continue; // Ignore autofeatures which need manual installation
                }

                if (feature.isCapabilitySatisfied(resolvedAndInstalled)) {
                    // Found a satisfied autofeature, add it and all its dependencies
                    // (only its real dependencies, not features which satisfy its provision capability requirement)
                    autofeatureWalker.walkDepthFirst(feature);
                }
            }
        }
    }

    /**
     * Situations can arise where a resolved feature is not a dependency of a requested feature or an autofeature.
     * <p>
     * This can happen when doing a basic resolve and an already installed feature has a tolerated dependency which is not installed and not required by the requested feature. This
     * tolerated dependency still needs to be installed.
     * <p>
     * This method finds such features and works out a minimal set to use as roots for install lists to ensure that every resolved feature gets installed
     */
    private void computeAdditionalInstallListRoots() {
        HashSet<ProvisioningFeatureDefinition> additionalRootCandidates = new HashSet<>(resolvedFeatures.values());
        FeatureTreeWalker removeCandidateWalker = FeatureTreeWalker.walkOver(resolvedFeatures)
                                                                   .forEach(additionalRootCandidates::remove);

        // Remove already installed features
        additionalRootCandidates.removeAll(installedFeatures);

        // Remove dependencies of requested features and samples
        for (String name : featureNamesToResolve) {
            ProvisioningFeatureDefinition feature = resolverRepository.getFeature(name);
            if (feature == null) {
                continue;
            }
            removeCandidateWalker.walkDepthFirst(feature);
        }

        // Remove dependencies of autofeatures
        for (ProvisioningFeatureDefinition feature : resolvedFeatures.values()) {
            if (feature.isAutoFeature()) {
                removeCandidateWalker.walkDepthFirst(feature);
            }
        }

        // For each remaining candidate, remove all of its dependencies, but not itself
        // Candidates will still be removed if they are dependencies of another candidate
        // Note: Copy additionalRootCandidates to avoid ConcurrentModificationException
        for (ProvisioningFeatureDefinition feature : new ArrayList<>(additionalRootCandidates)) {
            // Ignore this feature if it's already been removed as a dependency of another feature
            // This avoids having two candidates remove each other if the graph contains a loop
            if (additionalRootCandidates.contains(feature)) {
                // Remove ourself and our dependencies
                removeCandidateWalker.walkDepthFirst(feature);
                // Add ourself back in
                additionalRootCandidates.add(feature);
            }
        }

        additionalInstallListRoots = new ArrayList<>(additionalRootCandidates);
    }

    private void recordMissingFeature(String missingFeature) {
        Collection<ApplicableToProduct> featureOtherProducts = resolverRepository.getNonApplicableResourcesForName(missingFeature);
        if (featureOtherProducts.isEmpty()) {
            featuresMissing.add(missingFeature);
        } else {
            requirementsFoundForOtherProducts.add(missingFeature);
            for (ApplicableToProduct feature : featureOtherProducts) {
                resourcesWrongProduct.add(feature);
            }
        }
    }

    /**
     * Create the install lists for the resources which we were asked to resolve
     *
     * @return the install lists
     */
    List<List<RepositoryResource>> createInstallLists() {
        List<List<RepositoryResource>> installLists = new ArrayList<>();

        // Create install list for each sample
        for (SampleResource sample : samplesToInstall) {
            installLists.add(createInstallList(sample));
        }

        // Create install list for each requested feature
        for (String featureName : requestedFeatureNames) {
            List<RepositoryResource> installList = createInstallList(featureName);
            // May get an empty list if the requested feature is already installed
            if (!installList.isEmpty()) {
                installLists.add(installList);
            }
        }

        // Create install list for each autofeature which wasn't explicitly requested (otherwise we'd have covered it above) and isn't already installed
        for (ProvisioningFeatureDefinition feature : resolvedFeatures.values()) {
            if (feature.isAutoFeature() && !requestedFeatureNames.contains(feature.getSymbolicName())) {
                installLists.add(createInstallList(feature.getSymbolicName()));
            }
        }

        for (ProvisioningFeatureDefinition feature : additionalInstallListRoots) {
            installLists.add(createInstallList(feature.getSymbolicName()));
        }

        return installLists;
    }

    /**
     * Create a list of resources which should be installed in order to install the given sample.
     * <p>
     * The install list consists of all the dependencies which are needed by {@code resource}, ordered so that each resource in the list comes after its dependencies.
     *
     * @param resource the resource which is to be installed
     * @return the ordered list of resources to install
     */
    List<RepositoryResource> createInstallList(SampleResource resource) {
        List<MissingRequirement> missingRequirements = new ArrayList<>();

        AtomicBoolean allDependenciesResolved = new AtomicBoolean(true);
        ArrayList<ProvisioningFeatureDefinition> installList = new ArrayList<>();
        if (resource.getRequireFeature() != null) {

            List<ProvisioningFeatureDefinition> rootFeatures = new ArrayList<>();

            for (String featureName : resource.getRequireFeature()) {
                // Check that the sample actually exists
                ProvisioningFeatureDefinition feature = resolverRepository.getFeature(featureName);
                if (feature == null) {
                    allDependenciesResolved.set(false);
                    // Unless we know it exists but applies to another product, note the missing requirement as well
                    if (featuresMissing.contains(featureName)) {
                        missingRequirements.add(new MissingRequirement(featureName, resource));
                    }
                } else {
                    rootFeatures.add(feature);
                }
            }

            // We do a breadth first walk, adding features to the list. If we find a feature a second time, we move it to the end of the list.
            // This creates a list ordered by their deepest occurrence in the tree which ensures that all the dependencies of a feature
            // come after it in the list.
            FeatureTreeWalker.walkOver(resolvedFeatures)
                             .forEach(f -> {
                                 // Move the feature to the end if it's already in the list
                                 // to ensure it comes after everything that depends on it
                                 installList.remove(f);
                                 installList.add(f);
                             })
                             .onMissingDependency((f, dependency) -> {
                                 if (featuresMissing.contains(dependency.getSymbolicName())) {
                                     // The dependency was totally missing, add it to the list of missing requirements
                                     missingRequirements.add(new MissingRequirement(dependency.getSymbolicName(), getResource(f)));
                                 }
                                 allDependenciesResolved.set(false);
                             })
                             .walkBreadthFirst(rootFeatures);
        }

        if (!allDependenciesResolved.get()) {
            missingTopLevelRequirements.add(resource.getShortName());
            this.missingRequirements.addAll(missingRequirements);
        }

        // Remove installed features from the list and convert to EsaResources
        ArrayList<RepositoryResource> resourceInstallList = new ArrayList<>();
        for (ProvisioningFeatureDefinition installFeature : installList) {
            EsaResource featureResource = getResource(installFeature);
            if (featureResource != null) {
                resourceInstallList.add(featureResource);
            }
        }

        // Our walk ensures that every feature comes before its dependencies, but we actually need it to be the other way around
        Collections.reverse(resourceInstallList);

        // Add the sample itself to the end of the install list
        resourceInstallList.add(resource);

        return resourceInstallList;
    }

    /**
     * Create a list of resources which should be installed in order to install the given featutre.
     * <p>
     * The install list consists of all the dependencies which are needed by {@code esa}, ordered so that each resource in the list comes after its dependencies.
     *
     * @param featureName the feature name (as provided to {@link #resolve(Collection)}) for which to create an install list
     * @return the ordered list of resources to install, will be empty if the feature cannot be found or is already installed
     */
    List<RepositoryResource> createInstallList(String featureName) {
        // Find the feature by name (featureName may be the short or the symbolic name, so we need to use resolverRepository)
        ProvisioningFeatureDefinition feature = resolverRepository.getFeature(featureName);

        // Check that the requested feature was actually resolved
        if (feature != null) {
            feature = resolvedFeatures.get(feature.getSymbolicName());
        }

        if (feature == null) {
            // Feature missing
            missingTopLevelRequirements.add(featureName);
            if (featuresMissing.contains(featureName)) {
                missingRequirements.add(new MissingRequirement(featureName, null));
            }
            return Collections.emptyList();
        }

        List<MissingRequirement> missingRequirements = new ArrayList<>();
        List<ProvisioningFeatureDefinition> installList = new ArrayList<>();
        AtomicBoolean foundAll = new AtomicBoolean(true);

        // We do a breadth first walk, adding features to the list. If we find a feature a second time, we move it to the end of the list.
        // This creates a list ordered by their deepest occurrence in the tree which ensures that all the dependencies of a feature
        // come after it in the list.
        FeatureTreeWalker.walkOver(resolvedFeatures)
                         .forEach(f -> {
                             // Move the feature to the end if it's already in the list
                             // to ensure it comes after everything that depends on it
                             installList.remove(f);
                             installList.add(f);
                         })
                         .onMissingDependency((f, dependency) -> {
                             if (featuresMissing.contains(dependency.getSymbolicName())) {
                                 // The dependency was totally missing, add it to the list of missing requirements
                                 missingRequirements.add(new MissingRequirement(dependency.getSymbolicName(), getResource(f)));
                             }
                             foundAll.set(false);
                         })
                         .walkBreadthFirst(feature);

        if (!foundAll.get()) {
            missingTopLevelRequirements.add(featureName);
            this.missingRequirements.addAll(missingRequirements);
        }

        // Remove installed features from the list and convert to EsaResources
        ArrayList<RepositoryResource> resourceInstallList = new ArrayList<>();
        for (ProvisioningFeatureDefinition installFeature : installList) {
            EsaResource resource = getResource(installFeature);
            if (resource != null) {
                resourceInstallList.add(resource);
            }
        }

        // Our walk ensures that every feature comes before its dependencies, but we actually need it to be the other way around
        Collections.reverse(resourceInstallList);

        return resourceInstallList;
    }

    /**
     * Get the EsaResource for a feature from the repository
     *
     * @param feature the feature
     * @return the EsaResource, or {@code null} if the feature is not from the repository
     */
    private EsaResource getResource(ProvisioningFeatureDefinition feature) {
        if (!(feature instanceof KernelResolverEsa)) {
            return null;
        }
        KernelResolverEsa esaFeature = (KernelResolverEsa) feature;
        return esaFeature.getResource();
    }

    /**
     * If any errors occurred during resolution, throw a {@link RepositoryResolutionException}
     *
     * @throws RepositoryResolutionException if any errors occurred during resolution
     */
    private void reportErrors() throws RepositoryResolutionException {
        if (resourcesWrongProduct.isEmpty() && missingTopLevelRequirements.isEmpty() && missingRequirements.isEmpty() && featureConflicts.isEmpty()) {
            // Everything went fine!
            return;
        }

        Set<ProductRequirementInformation> missingProductInformation = new HashSet<>();

        for (ApplicableToProduct esa : resourcesWrongProduct) {
            missingRequirements.add(new MissingRequirement(esa.getAppliesTo(), (RepositoryResource) esa));
            missingProductInformation.addAll(ProductRequirementInformation.createFromAppliesTo(esa.getAppliesTo()));
        }

        List<String> missingRequirementNames = new ArrayList<>();
        for (MissingRequirement req : missingRequirements) {
            missingRequirementNames.add(req.getRequirementName());
        }

        throw new RepositoryResolutionException(null, missingTopLevelRequirements, missingRequirementNames, missingProductInformation, missingRequirements, featureConflicts);
    }

    static class NameAndVersion {

        public NameAndVersion(String name, String version) {
            super();
            this.name = name;
            this.version = version;
        }

        private final String name;
        private final String version;
    }

}

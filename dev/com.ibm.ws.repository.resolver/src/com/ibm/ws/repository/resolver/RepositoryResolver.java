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
package com.ibm.ws.repository.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.kernel.feature.internal.FeatureResolverImpl;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.product.utility.extension.IFixUtils;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.resolver.RepositoryResolutionException.MissingRequirement;
import com.ibm.ws.repository.resolver.RepositoryResolver.ResolvedFeatureSearchResult.ResultCategory;
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
    Map<String, SampleResource> sampleIndex;
    Collection<EsaResource> repoFeatures;
    Collection<SampleResource> repoSamples;
    RepositoryConnectionList repoConnections;

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
     */
    Map<String, ProvisioningFeatureDefinition> resolvedFeatures;

    /**
     * List of requested features that were reported missing by the kernel resolver and weren't found in the repository applicable to another product.
     * <p>
     * May include sample names if they were missing
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
     * Construct a new instance of a resolver for a current install. Note that calls to this method will load all of the resources from Massive so this is a relatively expensive
     * operation and may block the thread for a few seconds so care should be taken when called from a UI.</p>
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
     * MassiveResolver resolver = MassiveResolver.resolve(installedProductDefinitions,</br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;installedFeatures, installedFixes, loginInfo);</br>
     * </code>
     *
     * @param installDefinition Information about the product(s) installed such as ID and edition. Must not be <code>null</code>.
     * @param installedFeatures The features that are installed. Must not be <code>null</code>.
     * @param installedIFixes No longer used, parameter retained for backwards compatibility
     * @param massiveUserId The user ID to use when logging into Massive. Must not be <code>null</code>.
     * @param massivePassword The password to use when logging into Massive. Must not be <code>null</code>.
     * @param massiveApiKey The API key to use when logging into Massive. Must not be <code>null</code>.
     * @throws RepositoryException If there is a connection error with the Massive repository
     * @see ProductInfo#getAllProductInfo()
     * @see IFixUtils#getInstalledIFixes(java.io.File, com.ibm.ws.product.utility.CommandConsole)
     */
    public RepositoryResolver(Collection<ProductDefinition> installDefinition,
                              Collection<ProvisioningFeatureDefinition> installedFeatures,
                              Collection<IFixInfo> installedIFixes,
                              RepositoryConnectionList repoConnections) throws RepositoryException {

        this.repoConnections = repoConnections;
        fetchFromRepository(installDefinition);
        initializeResolverRepository(installedFeatures, installDefinition);
        this.installedFeatures = installedFeatures;
        indexSamples();
    }

    /**
     * Package constructor for unit tests
     * <p>
     * Allows resolution to be tested without connecting to a repository
     */
    RepositoryResolver(Collection<ProvisioningFeatureDefinition> installedFeatures,
                       Collection<? extends EsaResource> repoFeatures,
                       Collection<? extends SampleResource> repoSamples) {
        this.repoFeatures = new ArrayList<>(repoFeatures);
        this.repoSamples = new ArrayList<>(repoSamples);
        this.installedFeatures = installedFeatures;

        initializeResolverRepository(installedFeatures, Collections.<ProductDefinition> emptySet());
        indexSamples();
    }

    /**
     * Populates {@link #repoFeatures} and {@link #repoSamples} with resources from the repository which apply to the install definition.
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

    void initializeResolverRepository(Collection<ProvisioningFeatureDefinition> installedFeatures, Collection<ProductDefinition> installDefintion) {
        resolverRepository = new KernelResolverRepository(installDefintion, repoConnections);
        resolverRepository.addInstalledFeatures(installedFeatures);
        resolverRepository.addFeatures(repoFeatures);
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
     * <p>Will resolve a collection of resources from Massive. This method will find the resources specified by the <code>toResolve</code> parameter and any dependencies that they
     * require and return a list of the resources in massive that need to be installed. If any of the dependencies are already installed they will not be returned in the list.</p>
     * <p>Note that this will use the information about what is installed and in the Massive repository as was the case when this object was created so care should be taken that
     * this is still up to date (i.e. no features or fixes have been installed since this object was created).</p>
     * <p>This will also return a list of resources for any auto features that have their required provision capabilities satisfied either by what is already installed, the new
     * features being installed or a combination of the two. Note that this means auto features that are in no way related to what you have asked to resolve could be returned by
     * this method if they are satisfied by the current installation.</p>
     * <p>This method may return conflicting versions of singleton features (it's valid to have conflicting versions installed but they can't be started together). If you don't
     * want that, you may want to use {@link #resolveAsSet(Collection)} instead.</p>
     *
     * @param toResolve A collection of the identifiers of the resources to resolve. It should be in the form:</br>
     *            <code>{name}/{version}</code></br>
     *            <p>Where the <code>{name}</code> can be either the symbolic name, short name or lower case short name of the resource and <code>/{version}</code> is optional. The
     *            collection may contain a mixture of symbolic names and short names. Must not be <code>null</code> or empty.</p>
     * @return <p>A collection of ordered lists of {@link RepositoryResource}s to install. Each list represents a collection of resources that must be installed together or not
     *         at
     *         all. They should be installed in the iteration order of the list(s). Note that if a resource is required by multiple different resources then it will appear in
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
    public Collection<List<RepositoryResource>> resolve(Collection<String> toResolve) throws RepositoryResolutionException {
        return resolve(toResolve, ResolutionMode.IGNORE_CONFLICTS);
    }

    /**
     * Resolve a single name
     * <p>
     * Identical to {@code resolve(Collections.singleton(toResolve))}.
     */
    public Collection<List<RepositoryResource>> resolve(String toResolve) throws RepositoryResolutionException {
        return resolve(Collections.singleton(toResolve));
    }

    /**
     * As {@link #resolve(Collection)} except this method assumes that all the resources should start together on one server.
     * <p>
     * The resolution result will not include more than one version of a singleton feature.
     * <p>
     * This method of resolution can be used when you want to install all the features necessary to start a server.
     * <p>
     * Note that to use this method, you must provide the full set of features from the server.xml, including those that are already installed. Failure to do this may result in
     * an incorrect set of features being installed.
     * <p>
     * Also note that this method will fail if there's no valid set of dependencies for the required features that doesn't include conflicting versions of singleton features.
     * <p>
     * For example, {@code resolve(Arrays.asList("javaee-7.0", "javaee-8.0"))} would work but {@code resolveAsSet(Arrays.asList("javaee-7.0", "javaee-8.0"))} would fail because
     * javaee-7.0 and javaee-8.0 contain features which conflict with each other (and other versions are not tolerated).
     *
     * @param toResolve A collection of the identifiers of the resources to resolve. It should be in the form:</br>
     *            <code>{name}/{version}</code></br>
     *            <p>Where the <code>{name}</code> can be either the symbolic name, short name or lower case short name of the resource and <code>/{version}</code> is optional. The
     *            collection may contain a mixture of symbolic names and short names. Must not be <code>null</code> or empty.</p>
     * @return <p>A collection of ordered lists of {@link RepositoryResource}s to install. Each list represents a collection of resources that must be installed together or not
     *         at
     *         all. They should be installed in the iteration order of the list(s). Note that if a resource is required by multiple different resources then it will appear in
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

        processNames(toResolve);
        findAutofeatureDependencies();
        if (resolutionMode == ResolutionMode.IGNORE_CONFLICTS) {
            requireInstalledFeaturesWhenResolving();
        }

        resolveFeatures(resolutionMode);
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
        resolverRepository.clearPreferredVersions();
    }

    /**
     * Populates {@link #samplesToInstall} and {@link #featureNamesToResolve} by processing the list of names to resolve and identifying which are samples.
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
     * If any of the requested features are auto-features, find the set of features that satisfy their provisionCapability header and add those to the list of feature names to
     * resolve.
     * <p>
     * This is necessary because the kernel resolver will ignore the provision capability header if the feature has been specifically requested, but that's not usually helpful at
     * install time.
     */
    void findAutofeatureDependencies() {
        // If the user has requested an autofeature to be resolved, we want to treat the features mentioned in its provisionCapability header as dependencies
        ArrayList<String> autofeatureDependencies = new ArrayList<>();
        for (String featureName : featureNamesToResolve) {
            ProvisioningFeatureDefinition feature = resolverRepository.getFeature(featureName);
            if (feature != null && feature.isAutoFeature() && feature instanceof KernelResolverEsa) {
                Collection<ProvisioningFeatureDefinition> dependencies = ((KernelResolverEsa) feature).findFeaturesSatisfyingCapability(resolverRepository.getAllFeatures());
                for (ProvisioningFeatureDefinition dependency : dependencies) {
                    autofeatureDependencies.add(dependency.getSymbolicName());
                }
            }
        }

        featureNamesToResolve.addAll(autofeatureDependencies);
    }

    /**
     * Uses the kernel resolver to resolve {@link #featureNamesToResolve} and populates {@link #resolvedFeatures} with the result.
     */
    void resolveFeatures(ResolutionMode mode) {
        boolean allowMultipleVersions = mode == ResolutionMode.IGNORE_CONFLICTS ? true : false;
        FeatureResolver resolver = new FeatureResolverImpl();
        Result result = resolver.resolveFeatures(resolverRepository, featureNamesToResolve, Collections.<String> emptySet(), allowMultipleVersions);
        for (String name : result.getResolvedFeatures()) {
            ProvisioningFeatureDefinition feature = resolverRepository.getFeature(name);
            resolvedFeatures.put(feature.getSymbolicName(), feature);
        }

        for (String missingFeature : result.getMissing()) {
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
    }

    /**
     * Require that all installed features are included in the resolution result
     * <p>
     * This allows autofeatures which depend on an already installed feature to be resolved.
     * <p>
     * This method can't be used when detecting conflicts between singleton features, as we might already have conflicting features installed (which is fine, as long as they're not
     * started together in a running server).
     */
    void requireInstalledFeaturesWhenResolving() {
        // We need to include installed features in the resolution to allow all autofeatures to resolve
        for (ProvisioningFeatureDefinition installedFeature : installedFeatures) {
            featureNamesToResolve.add(installedFeature.getSymbolicName());
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
            if (feature.isAutoFeature() && !requestedFeatureNames.contains(feature.getSymbolicName()) && feature instanceof KernelResolverEsa) {
                installLists.add(createInstallList(feature.getSymbolicName()));
            }
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
        Map<String, Integer> maxDistanceMap = new HashMap<>();
        List<MissingRequirement> missingRequirements = new ArrayList<>();

        boolean allDependenciesResolved = true;
        if (resource.getRequireFeature() != null) {
            for (String featureName : resource.getRequireFeature()) {

                // Check that the sample actually exists
                ProvisioningFeatureDefinition feature = resolverRepository.getFeature(featureName);
                if (feature == null) {
                    allDependenciesResolved = false;
                    // Unless we know it exists but applies to another product, note the missing requirement as well
                    if (!requirementsFoundForOtherProducts.contains(featureName)) {
                        missingRequirements.add(new MissingRequirement(featureName, resource));
                    }
                }

                // Build distance map and check dependencies
                allDependenciesResolved &= populateMaxDistanceMap(maxDistanceMap, featureName, 1, new HashSet<ProvisioningFeatureDefinition>(), missingRequirements);
            }
        }

        if (!allDependenciesResolved) {
            missingTopLevelRequirements.add(resource.getShortName());
            this.missingRequirements.addAll(missingRequirements);
        }

        ArrayList<RepositoryResource> installList = new ArrayList<>();

        installList.addAll(convertFeatureNamesToResources(maxDistanceMap.keySet()));
        Collections.sort(installList, byMaxDistance(maxDistanceMap));

        installList.add(resource);

        return installList;
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
        ProvisioningFeatureDefinition feature = resolverRepository.getFeature(featureName);
        if (feature == null) {
            // Feature missing
            missingTopLevelRequirements.add(featureName);
            // If we didn't find this feature in another product, we need to record it as missing
            if (!requirementsFoundForOtherProducts.contains(featureName)) {
                missingRequirements.add(new MissingRequirement(featureName, null));
            }
            return Collections.emptyList();
        }

        if (!(feature instanceof KernelResolverEsa)) {
            // Feature already installed
            return Collections.emptyList();
        }

        EsaResource esa = ((KernelResolverEsa) feature).getResource();

        Map<String, Integer> maxDistanceMap = new HashMap<>();
        List<MissingRequirement> missingRequirements = new ArrayList<>();
        boolean foundAllDependencies = populateMaxDistanceMap(maxDistanceMap, esa.getProvideFeature(), 0, new HashSet<ProvisioningFeatureDefinition>(), missingRequirements);

        if (!foundAllDependencies) {
            missingTopLevelRequirements.add(featureName);
            this.missingRequirements.addAll(missingRequirements);
        }

        ArrayList<RepositoryResource> installList = new ArrayList<>();

        installList.addAll(convertFeatureNamesToResources(maxDistanceMap.keySet()));
        Collections.sort(installList, byMaxDistance(maxDistanceMap));

        return installList;
    }

    /**
     * Returns a comparator which sorts EsaResources by their values from {@code maxDistanceMap} from greatest to smallest
     * <p>
     * Resources whose symbolic names do not appear in the map or which are not EsaResources are assigned a value of zero meaning that they will appear last in a sorted list.
     *
     * @param maxDistanceMap map from symbolic name to the length of the longest dependency chain from a specific point
     * @return compariator which sorts based on the maxDistance of resources
     */
    static Comparator<RepositoryResource> byMaxDistance(final Map<String, Integer> maxDistanceMap) {
        return new Comparator<RepositoryResource>() {

            @Override
            public int compare(RepositoryResource o1, RepositoryResource o2) {
                return Integer.compare(getDistance(o2), getDistance(o1));
            }

            private int getDistance(RepositoryResource res) {
                if (res.getType() == ResourceType.FEATURE) {
                    Integer distance = maxDistanceMap.get(((EsaResource) res).getProvideFeature());
                    return distance == null ? 0 : distance;
                } else {
                    return 0;
                }
            }

        };
    }

    /**
     * Build a map which maps feature symbolic names to the length of the longest dependency chain from the starting point to that feature
     * <p>
     * This method adds {@code featureName} to the map with {@code currentDistance} and then recurses through its dependencies, repeating this operation. It will stop if it
     * encounters a feature which is already installed or if it encounters a dependency loop.
     * <p>
     * The result of this operation is useful for building install lists as the features to be installed can be sorted by the longest dependency chain in descending order to ensure
     * that the dependencies of a feature are installed before the feature itself.
     * <p>
     * Example. To find the longest dependency chain for all dependent features of com.example.featureA, the following code can be used:
     * <p>
     * <code>
     *
     * <pre>
     * Map<String, Integer> distanceMap = new HashMap<>();
     * populateMaxDistanceMap(distanceMap, "com.example.featureA", 0, new HashSet&lt;ProvisioningFeatureDefinition&gt;());
     * </pre>
     *
     * </code>
     * <p>
     * Having done this, {@code distanceMap.keySet()} gives the set of all the dependencies of com.example.featureA. {@code distanceMap.get("com.example.featureB")} gives
     * the length of the longest dependency chain from featureA to featureB.
     *
     * @param maxDistanceMap the map to be populated
     * @param featureName the current feature
     * @param currentDistance the distance to use for the current feature
     * @param currentStack the set of feature names already in the current dependency chain (used to detect loops)
     * @return true if all requirements were found, false otherwise
     */
    boolean populateMaxDistanceMap(Map<String, Integer> maxDistanceMap, String featureName, int currentDistance, Set<ProvisioningFeatureDefinition> currentStack,
                                   List<MissingRequirement> missingRequirements) {
        ProvisioningFeatureDefinition feature = resolvedFeatures.get(featureName);
        if (!(feature instanceof KernelResolverEsa)) {
            // Feature is already installed
            return true;
        }

        if (currentStack.contains(feature)) {
            // We've hit a dependency loop
            return true;
        }

        boolean result = true;

        currentStack.add(feature);

        KernelResolverEsa featureEsa = (KernelResolverEsa) feature;

        Integer oldDistance = maxDistanceMap.get(feature.getSymbolicName());
        if (oldDistance == null || oldDistance < currentDistance) {
            maxDistanceMap.put(feature.getSymbolicName(), currentDistance);
        }

        for (FeatureResource dependency : feature.getConstituents(SubsystemContentType.FEATURE_TYPE)) {
            ResolvedFeatureSearchResult searchResult = findResolvedDependency(dependency);

            if (searchResult.category == ResultCategory.FOUND) {
                // We found the dependency, continue populating the distance map
                result &= populateMaxDistanceMap(maxDistanceMap, searchResult.symbolicName, currentDistance + 1, currentStack, missingRequirements);
            } else if (searchResult.category == ResultCategory.MISSING) {
                // The dependency was totally missing, add it to the list of missing requirements
                missingRequirements.add(new MissingRequirement(dependency.getSymbolicName(), featureEsa.getResource()));
                result = false;
            } else {
                // The dependency was found for another product. That missing requirement is already recorded elsewhere.
                result = false;
            }
        }

        // Find autofeature dependencies
        for (ProvisioningFeatureDefinition dependency : featureEsa.findFeaturesSatisfyingCapability(resolvedFeatures.values())) {
            result &= populateMaxDistanceMap(maxDistanceMap, dependency.getSymbolicName(), currentDistance + 1, currentStack, missingRequirements);
        }

        currentStack.remove(feature);

        return result;
    }

    /**
     * Find the actual resolved feature from a dependency with tolerates
     * <p>
     * Tries each of the tolerated versions in order until it finds one that exists in the set of resolved features.
     * <p>
     * Three types of results are possible:
     * <ul>
     * <li><b>FOUND</b>: We found the required feature</li>
     * <li><b>FOUND_WRONG_PRODUCT</b>: We found the required feature, but it was for the wrong product</li>
     * <li><b>MISSING</b>: We did not find the required feature. The {@code symbolicName} field of the result will be {@code null}</li>
     * </ul>
     *
     * @param featureResource the dependency definition to resolve
     * @return the result of the search
     */
    ResolvedFeatureSearchResult findResolvedDependency(FeatureResource featureResource) {
        ProvisioningFeatureDefinition feature = resolvedFeatures.get(featureResource.getSymbolicName());
        if (feature != null) {
            return new ResolvedFeatureSearchResult(ResultCategory.FOUND, feature.getSymbolicName());
        }

        if (requirementsFoundForOtherProducts.contains(featureResource.getSymbolicName())) {
            return new ResolvedFeatureSearchResult(ResultCategory.FOUND_WRONG_PRODUCT, featureResource.getSymbolicName());
        }

        String baseName = getFeatureBaseName(featureResource.getSymbolicName());
        for (String toleratedVersion : featureResource.getTolerates()) {
            String featureName = baseName + toleratedVersion;

            feature = resolvedFeatures.get(featureName);
            if (feature != null) {
                return new ResolvedFeatureSearchResult(ResultCategory.FOUND, feature.getSymbolicName());
            }

            if (requirementsFoundForOtherProducts.contains(featureName)) {
                return new ResolvedFeatureSearchResult(ResultCategory.FOUND_WRONG_PRODUCT, featureName);
            }
        }

        return new ResolvedFeatureSearchResult(ResultCategory.MISSING, null);
    }

    /**
     * Removes the version from the end of a feature symbolic name
     * <p>
     * The version is presumed to start after the last dash character in the name.
     * <p>
     * E.g. {@code getFeatureBaseName("com.example.featureA-1.0")} returns {@code "com.example.featureA-"}
     *
     * @param nameAndVersion the feature symbolic name
     * @return the feature symbolic name with any version stripped
     */
    String getFeatureBaseName(String nameAndVersion) {
        int dashPosition = nameAndVersion.lastIndexOf('-');
        if (dashPosition != -1) {
            return nameAndVersion.substring(0, dashPosition + 1);
        } else {
            return nameAndVersion;
        }
    }

    /**
     * Convert a collection of feature names into a list of EsaResources
     * <p>
     * If a feature with the given name is not found, or if it corresponds to a feature which is already installed, it is ignored. Therefore, this method can be used to convert a
     * list of names from the kernel resolver into a list of esas which need to be installed.
     *
     * @param names the feature names to find
     * @return a list comprised of the corresponding EsaResource for each name in {@code names}, if one exists
     */
    List<EsaResource> convertFeatureNamesToResources(Collection<String> names) {
        List<EsaResource> results = new ArrayList<>();

        for (String name : names) {
            ProvisioningFeatureDefinition feature = resolverRepository.getFeature(name);
            if (feature instanceof KernelResolverEsa) {
                results.add(((KernelResolverEsa) feature).getResource());
            }
        }

        return results;
    }

    /**
     * If any errors occurred during resolution, throw a {@link RepositoryResolutionException}
     */
    private void reportErrors() throws RepositoryResolutionException {
        if (resourcesWrongProduct.isEmpty() && missingTopLevelRequirements.isEmpty() && missingRequirements.isEmpty()) {
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

        throw new RepositoryResolutionException(null, missingTopLevelRequirements, missingRequirementNames, missingProductInformation, missingRequirements);
    }

    enum ResolutionMode {
        DETECT_CONFLICTS,
        IGNORE_CONFLICTS,
    }

    static class ResolvedFeatureSearchResult {

        public ResolvedFeatureSearchResult(ResultCategory category, String symbolicName) {
            this.category = category;
            this.symbolicName = symbolicName;
        }

        enum ResultCategory {
            FOUND,
            FOUND_WRONG_PRODUCT,
            MISSING
        }

        ResultCategory category;
        String symbolicName;
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

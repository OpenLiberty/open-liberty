/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.resolver.ResolverImpl;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

import com.ibm.ws.kernel.feature.internal.generator.ManifestFileProcessor;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.product.utility.extension.IFixUtils;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.resolver.RepositoryResolutionException.MissingRequirement;
import com.ibm.ws.repository.resolver.internal.FixFeatureComparator;
import com.ibm.ws.repository.resolver.internal.RepositoryResolveContext;
import com.ibm.ws.repository.resolver.internal.StopAutoFeaturesInstallingTheirRequiredCapabilities;
import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants;
import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants.NameAttributes;
import com.ibm.ws.repository.resolver.internal.resource.FeatureResource;
import com.ibm.ws.repository.resolver.internal.resource.IFixResource;
import com.ibm.ws.repository.resolver.internal.resource.LpmResource;
import com.ibm.ws.repository.resolver.internal.resource.ProductRequirement;
import com.ibm.ws.repository.resolver.internal.resource.ProductResource;
import com.ibm.ws.repository.resolver.internal.resource.RequirementImpl;
import com.ibm.ws.repository.resolver.internal.resource.ResourceImpl;
import com.ibm.ws.repository.resolver.internal.resource.SampleResource;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.IfixResource;
import com.ibm.ws.repository.resources.RepositoryResource;

/**
 * <p>This class contains methods for resolving resources from Massive. As it is expensive to construct instances of this class it is recommended that instances of this class are
 * cached and re-used if the user of the API requires multiple resolve calls to be made. As the object will load the resources from the Massive repository when it is loaded then a
 * very long lived instance of this class could risk becoming stale if the repository was updated between construction time and resolving time so if this is the case then the user
 * of the API may consider refreshing their cache but this should not be an issue for relatively short lived objects.</p>
 * <p>If a client installs content after calling resolve then the instance should be discarded and recreated to reflect the new state of the installed resources.<p/>
 */
public class RepositoryResolver {

    private final RepositoryConnectionList loginInfo;
    private final List<Resource> installedProductResources;
    private final List<Resource> installedEntities;
    private final List<Resource> repoResources;
    private final List<Resource> repoIFixResources;
    private final Collection<Resource> autoFeatures;
    private final static FixFeatureComparator FIX_FEATURE_COMPARATOR = new FixFeatureComparator();

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
     * @param installedIFixes The iFixes that are installed. Must not be <code>null</code>.
     * @param massiveUserId The user ID to use when logging into Massive. Must not be <code>null</code>.
     * @param massivePassword The password to use when logging into Massive. Must not be <code>null</code>.
     * @param massiveApiKey The API key to use when logging into Massive. Must not be <code>null</code>.
     * @throws RepositoryException If there is a connection error with the Massive repository
     * @see ProductInfo#getAllProductInfo()
     * @see ManifestFileProcessor#getFeatureDefinitions()
     * @see IFixUtils#getInstalledIFixes(java.io.File, com.ibm.ws.product.utility.CommandConsole)
     */
    public RepositoryResolver(Collection<ProductDefinition> installDefinition,
                              Collection<ProvisioningFeatureDefinition> installedFeatures,
                              Collection<IFixInfo> installedIFixes,
                              RepositoryConnectionList repoConnections) throws RepositoryException {
        this.loginInfo = repoConnections;
        // Process all of the product infos and turn them into OSGi resources
        this.installedProductResources = new ArrayList<Resource>();
        this.installedProductResources.add(ProductResource.createInstance(installDefinition));

        // Process all of the installed features and iFixes and turn them into OSGi resources
        this.installedEntities = new ArrayList<Resource>();
        Collection<String> installedFeaturesSymbolicNames = new HashSet<String>();
        for (ProvisioningFeatureDefinition featureDefinition : installedFeatures) {
            this.installedEntities.add(FeatureResource.createInstance(featureDefinition));
            installedFeaturesSymbolicNames.add(featureDefinition.getSymbolicName());
        }
        for (IFixInfo iFixInfo : installedIFixes) {
            this.installedEntities.add(IFixResource.createInstance(iFixInfo));
        }

        // Get all of the resources we are interested in out of massive
        Collection<ResourceType> interestingTypes = new HashSet<ResourceType>();
        interestingTypes.add(ResourceType.FEATURE);
        interestingTypes.add(ResourceType.IFIX);
        interestingTypes.add(ResourceType.OPENSOURCE);
        interestingTypes.add(ResourceType.PRODUCTSAMPLE);
        Map<ResourceType, Collection<? extends RepositoryResource>> resources = repoConnections.getResources(installDefinition, interestingTypes, null);
        @SuppressWarnings("unchecked")
        Collection<EsaResource> esaMassiveResources = (Collection<EsaResource>) resources.get(ResourceType.FEATURE);
        List<FeatureResource> featureResources = new ArrayList<FeatureResource>();
        this.autoFeatures = new HashSet<Resource>();
        if (esaMassiveResources != null) {
            for (EsaResource esaMassiveResource : esaMassiveResources) {
                FeatureResource featureResource = FeatureResource.createInstance(esaMassiveResource);
                featureResources.add(featureResource);

                /*
                 * If this is an auto feature we want to see if it is satisfied by the set of features that will be installed after this resolution and return it if it is,
                 * therefore add it to a set of optional resources to resolve
                 */
                if (featureResource.isAutoFeatureThatShouldBeInstalledWhenSatisfied()
                    && !installedFeaturesSymbolicNames.contains(esaMassiveResource.getProvideFeature())) {
                    this.autoFeatures.add(featureResource);
                }
            }
        }
        Collections.sort(featureResources);

        @SuppressWarnings("unchecked")
        Collection<com.ibm.ws.repository.resources.SampleResource> sampleMassiveResources = (Collection<com.ibm.ws.repository.resources.SampleResource>) resources.get(ResourceType.OPENSOURCE);
        @SuppressWarnings("unchecked")
        Collection<com.ibm.ws.repository.resources.SampleResource> productMassiveResources = (Collection<com.ibm.ws.repository.resources.SampleResource>) resources.get(ResourceType.PRODUCTSAMPLE);
        if (sampleMassiveResources == null) {
            sampleMassiveResources = productMassiveResources;
        } else if (productMassiveResources != null) {
            sampleMassiveResources.addAll(productMassiveResources);
        }
        List<SampleResource> sampleResources = new ArrayList<SampleResource>();
        if (sampleMassiveResources != null) {
            for (com.ibm.ws.repository.resources.SampleResource sampleMassiveResource : sampleMassiveResources) {
                SampleResource sampleResource = SampleResource.createInstance(sampleMassiveResource);
                sampleResources.add(sampleResource);
            }
        }

        @SuppressWarnings("unchecked")
        Collection<IfixResource> ifixMassiveResources = (Collection<IfixResource>) resources.get(ResourceType.IFIX);
        List<IFixResource> sortableRepoIFixResources = new ArrayList<IFixResource>();
        if (ifixMassiveResources != null) {
            for (IfixResource ifixMassiveResource : ifixMassiveResources) {
                sortableRepoIFixResources.add(IFixResource.createInstance(ifixMassiveResource));
            }
        }
        Collections.sort(sortableRepoIFixResources);

        // Sortable lists need to be typed to instances of comparable but interface for MassiveResolveContext is typed to Resource so need to make a copy to keep Java compiler happy
        this.repoIFixResources = new ArrayList<Resource>(sortableRepoIFixResources);

        this.repoResources = new ArrayList<Resource>(featureResources);
        this.repoResources.addAll(sampleResources);
        this.repoResources.addAll(repoIFixResources);
    }

    /**
     * <p>Will resolve a collection of resources from Massive. This method will find the resources specified by the <code>toResolve</code> parameter and any dependencies that they
     * require and return a list of the resources in massive that need to be installed. If any of the dependencies are already installed they will not be returned in the list.</p>
     * <p>Note that this will use the information about what is installed and in the Massive repository as was the case when this object was created so care should be taken that
     * this is still up to date (i.e. no features or fixes have been installed since this object was created).</p>
     * <p>This will also return a list of resources for any auto features that have their required provision capabilities satisfied either by what is already installed, the new
     * features being installed or a combination of the two. Note that this means auto features that are in no way related to what you have asked to resolve could be returned by
     * this method if they are satisfied by the current installation.</p>
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
        // Create the resolve context
        Map<Resource, List<Wire>> wirings = null;
        // Use a null executor to avoid multi-threaded (see task 200860)
        Resolver resolver = new ResolverImpl(null, null);

        /*
         * There are lots of ways that we could look for the asset: by symbolic name, short name or a case insensitive short name and this varies according to the type (feature vs.
         * sample). For each name to resolve, iterate through these
         * possibilities in preferential order, until we find a matching resource. Note that we're not trying to resolve the resource at this point, just check if we can find it.
         * 
         * Once we've turned the list of names into a list of resources, we attempt to resolve that set of resources and report any failures with a helpful error message.
         * 
         * Use a linked hash map to search in the best order, feature first then samples.
         */
        Map<String, Collection<NameAttributes>> typesAndAttributesToSearch = new LinkedHashMap<String, Collection<NameAttributes>>();
        typesAndAttributesToSearch.put(InstallableEntityIdentityConstants.TYPE_FEATURE, Arrays.asList(NameAttributes.values()));
        typesAndAttributesToSearch.put(InstallableEntityIdentityConstants.TYPE_SAMPLE, InstallableEntityIdentityConstants.SAMPLES_NAME_ATTRIBUTES);
        RepositoryResolveContext existingResolveContext = new RepositoryResolveContext(Collections.<Resource> emptySet(), Collections.<Resource> emptySet(), this.installedProductResources, this.installedEntities, this.repoResources, this.loginInfo);
        Collection<Resource> resourcesToResolve = new HashSet<Resource>();

        for (String toResolveString : toResolve) {
            Resource toResolveResource = null;

            typeAndAttributeLoop: for (Map.Entry<String, Collection<NameAttributes>> typesAndAttributesEntry : typesAndAttributesToSearch.entrySet()) {
                String type = typesAndAttributesEntry.getKey();
                Collection<NameAttributes> attributesToMatch = typesAndAttributesEntry.getValue();
                for (NameAttributes attributeToMatch : attributesToMatch) {
                    toResolveResource = LpmResource.createInstance(toResolveString, attributeToMatch, type);

                    boolean allRequirementsCanBeMet = true;
                    for (Requirement requirement : toResolveResource.getRequirements(null)) {
                        if (existingResolveContext.findProviders(requirement).isEmpty()) {
                            allRequirementsCanBeMet = false;
                            break;
                        }
                    }

                    if (allRequirementsCanBeMet) {
                        // We can find the current resource within the set of available resources
                        // the current toResolveResource should be valid so stop looking
                        break typeAndAttributeLoop;
                    }
                }
            }

            // Finally, add our resource to the list of resources to resolve
            // It's important to add it, even if we know it can't resolved. When resolution fails, we'll gather up all the problems to return to the user.
            resourcesToResolve.add(toResolveResource);
        }

        RepositoryResolveContext resolveContext = new RepositoryResolveContext(resourcesToResolve, Collections.<Resource> emptySet(), this.installedProductResources, this.installedEntities, this.repoResources, this.loginInfo);
        try {
            wirings = resolver.resolve(resolveContext);
        } catch (ResolutionException e) {
            // Create our own exception from this resolution failure with slightly more information on it
            Collection<String> missingTopLevelFeatures = this.getNamesOfRequirements(e.getUnresolvedRequirements());
            Collection<String> requirementsNotFound = this.getNamesOfRequirements(resolveContext.getRequirementsNotFound());
            Collection<MissingRequirement> requirementResourcesNotFound = this.getNamesAndResourcesOfRequirements(resolveContext.getRequirementsNotFound());
            Collection<ProductRequirementInformation> productsNotFound = this.getProductsNotFoundFromRequirements(resolveContext.getRequirementsNotFound());
            throw new RepositoryResolutionException(e, missingTopLevelFeatures, requirementsNotFound, productsNotFound, requirementResourcesNotFound);
        }

        /*
         * Now wire up all of the auto features as well. We do this in a second resolve call so that we can limit the available resources to those that are either already
         * installed or those that are about to be installed after this resolution. This is because otherwise the requirements from the auto features would pull in their
         * dependencies from the repository and always install itself and their dependencies. This way they will only be installed if everything they need is already there rather
         * than just available in the repo. Note that we pass the auto features in as an optional dependency, if it can't be resolved that is fine we just won't install it.
         */
        if (this.autoFeatures != null && !this.autoFeatures.isEmpty()) {
            List<Resource> resolvedAndInstalledResources = new ArrayList<Resource>(installedEntities);
            for (Resource resource : wirings.keySet()) {
                if (!resolvedAndInstalledResources.contains(resource)) {
                    resolvedAndInstalledResources.add(resource);
                }
            }

            RepositoryResolveContext autoFeatureResolveContext = new RepositoryResolveContext(Collections.<Resource> emptySet(), this.autoFeatures, this.installedProductResources, resolvedAndInstalledResources, this.repoResources, this.loginInfo);
            // We don't want an auto feature pulling in all of it's required features into the install list but do allow it to pull in other ifixes from the repo
            autoFeatureResolveContext.addFilter(new StopAutoFeaturesInstallingTheirRequiredCapabilities());

            try {
                Map<Resource, List<Wire>> autoFeatureWirings = resolver.resolve(autoFeatureResolveContext);
                wirings.putAll(autoFeatureWirings);
            } catch (ResolutionException e) {
                /*
                 * I don't think this can happen as we don't have any mandatory resources. Also, we were just trying to sort out the auto wiring features not the ones the user
                 * actually asked for so just ignore this as we have resolved everything the user asked us to resolve by the time we get here.
                 */
            }
        }

        Collection<List<RepositoryResource>> installLists = this.convertWiringsToInstallLists(wirings, resourcesToResolve);
        return installLists;
    }

    /**
     * Convert a map of wirings into a collection of lists for what to install. You get one list per thing that the user asked to resolve (assuming there are resources to install
     * for it) and one per auto feature that resolved.
     * 
     * @param wirings The wirings containing relationships between features
     * @param resourcesAskedFor The resources the user of the API asked to resolve
     * @return A collection of ordered lists stating what needs installing
     */
    private Collection<List<RepositoryResource>> convertWiringsToInstallLists(Map<Resource, List<Wire>> wirings, Collection<Resource> resourcesAskedFor) {
        /*
         * Convert the map of wirings into an ordered list for the installer to install the resource in. They want to have a list for each thing being installed, i.e. if you ask to
         * install A and B which both rely on C then they want two lists, one with C and A in and one with C and B in and in that order as the dependencies need to be installed
         * first.
         * 
         * As we know what the user asked to install we start at the top and work down the list of dependencies that it has. Care is taken to make sure if there are two paths to a
         * resource then it is only added to the list once. We also need to have a list for each auto feature that is being installed but it would be nicer to merge these with the
         * top level resources if they correspond.
         * 
         * Things are slightly complicated by the fact the list will contain features and fixes. If we imagine a situation where you have feature A requiring feature B and fix Z,
         * in order to be useful to A Z must be fixing B but there is no knowledge of this in Z (fixes never say what features they fix) so all we have to go on is that we need to
         * install Z before A. To make sure we get the install order right for each dependency install features first then fixes.
         */
        Collection<List<RepositoryResource>> resourcesToInstall = new HashSet<List<RepositoryResource>>();

        /*
         * Start at the top with the resources the user asked for, this should be a LPMResource that should only ever have one wiring from it, we need to recursively go down
         * its requirements (through it's wires) until we have added all it's requirements to a list.
         */
        for (Resource resourceAskedFor : resourcesAskedFor) {
            List<RepositoryResource> installList = new ArrayList<RepositoryResource>();
            this.addRequiredDependenciesToInstallList(wirings, resourceAskedFor, installList, new HashSet<Resource>());
            this.addResourceList(installList, resourcesToInstall);
        }

        /*
         * Repeat for the auto features, these get their own list as per the JavaDoc of the resolve method. Note that not all of the auto features will of been satisified but this
         * will be caught by the addInstallListForResources as there will be no wirings to a resource with a MassiveResource attached to it.
         * 
         * Auto features are slightly different to the ones that the user asked for as we first load them from the repo so they are FeatureResources with a MassiveResource attached
         * (that needs to be installed if it resolved) whereas the ones the user asks for are LpmResources where you have to go one down the tree before you install, unfortunately
         * this difference means it's hard to common up the first level of code so duplicate very similar code.
         */
        for (Resource autoFeature : this.autoFeatures) {
            // Check it resolved first!
            if (wirings.containsKey(autoFeature)) {
                List<RepositoryResource> installList = new ArrayList<RepositoryResource>();
                if (autoFeature instanceof ResourceImpl) {
                    RepositoryResource autoFeatureMassiveResource = ((ResourceImpl) autoFeature).getResource();
                    if (autoFeatureMassiveResource != null) {
                        installList.add(autoFeatureMassiveResource);
                    }
                }
                this.addRequiredDependenciesToInstallList(wirings, autoFeature, installList, new HashSet<Resource>());
                this.addResourceList(installList, resourcesToInstall);
            }
        }

        return resourcesToInstall;
    }

    /**
     * If the new install list is not empty this will remove any duplicates from it and add it to the resources to install set.
     * 
     * @param installList A list of resources to install
     * @param resourcesToInstall A collection of all of the lists of resources to install
     */
    private void addResourceList(List<RepositoryResource> installList, Collection<List<RepositoryResource>> resourcesToInstall) {
        if (!installList.isEmpty()) {
            /*
             * Remove duplicates, easier doing this in a copy of the list so that we can make sure we always keep the first time something appears and don't have to worry
             * about ConcurrentModificationExceptions
             */
            List<RepositoryResource> installListCopy = new ArrayList<RepositoryResource>(installList.size());
            for (RepositoryResource massiveResource : installList) {
                if (!installListCopy.contains(massiveResource)) {
                    installListCopy.add(massiveResource);
                }
            }
            resourcesToInstall.add(installListCopy);
        }
    }

    /**
     * Add the dependencies from the supplied resource to the list of things to install. Features at any level are added before any fixes at that level and it is recursive so any
     * recursive dependencies will be added to the start of the list. This may add entries more than once to the list.
     * 
     * @param wirings The wirings containing relationships between features
     * @param resource The resource to find the things to install for
     * @param installList The list of resources that need installing
     * @param resourcesProcessedInThisStack This is a set of the resources that have been processed in the stack of recursive calls to this method. It is used for cirle checking
     *            but note that it is ok to have forks so a resource can be processed twice by this method, just not in the same stack of recursive calls. Must not be
     *            <code>null</code>.
     * @param object
     */
    private void addRequiredDependenciesToInstallList(Map<Resource, List<Wire>> wirings, Resource resource, List<RepositoryResource> installList,
                                                      Set<Resource> resourcesProcessedInThisStack) {
        resourcesProcessedInThisStack.add(resource);
        List<Wire> wires = wirings.get(resource);
        List<ResourceImpl> thingsToInstall = new ArrayList<ResourceImpl>();
        for (Wire wire : wires) {
            /*
             * We need to make sure that iFixes are installed after all of the features at the same level as them so do a first pass through to get everything from this level we
             * need to install
             */
            Resource wireProvider = wire.getProvider();

            // If we hit an installed resource (i.e. not a ResourceImpl with a massive resource) then we can stop iterating down the tree as everything it needs must be installed.
            if (wireProvider instanceof ResourceImpl) {
                ResourceImpl wireProviderResourceImpl = ((ResourceImpl) wireProvider);
                RepositoryResource massiveResource = wireProviderResourceImpl.getResource();
                if (massiveResource != null && !resourcesProcessedInThisStack.contains(wireProviderResourceImpl)) {
                    /*
                     * Only add it if this is hasn't been done in this recursive call yet - we clone the set with each recursive call as forks are ok but circles aren't so a
                     * resource can be added twice. We need to add it twice as we need to make sure that it is installed after all the things that depend on it, see test
                     * testOrderingOnMultipleLongPaths.
                     * is ok.
                     */
                    thingsToInstall.add(wireProviderResourceImpl);
                }
            }
        }
        // Now sort so that the fixes are in the list first
        Collections.sort(thingsToInstall, FIX_FEATURE_COMPARATOR);

        /*
         * Add all of these resources to the start of the install list we add fixes to the start first then we add features to the start of the install list so the features will be
         * installed before the fixes (as the fixes might fix the features).
         */
        for (ResourceImpl resourceImpl : thingsToInstall) {
            installList.add(0, resourceImpl.getResource());
        }

        /*
         * Now add their dependencies do this in a separate iterator as if you have a forked path you want to do it after everything at this level has been added. For example:
         * 
         * A requires B and C
         * B requires D
         * C requires D
         * 
         * If we just had one for loop to add the resource to the install list then add its dependencies then when we added B we would then also add D in the recursive call
         * before then adding C. We therefore need two for loops, first to add B and C, then another to make the recursive call to add the dependency to D. If that makes as much
         * sense to read as it did to write then you can always check out the testChainedFeatureDependenciesWithMultipleRoutes test in the JUnit which requires this to be done in
         * two goes round the thingsToInstallList.
         */
        for (ResourceImpl resourceImpl : thingsToInstall) {
            // Clone the resourcesProcessedInThisStack for each dependency as forks are ok but circles aren't
            this.addRequiredDependenciesToInstallList(wirings, resourceImpl, installList, new HashSet<Resource>(resourcesProcessedInThisStack));
        }
    }

    /**
     * This method will get the names of all of the requirements. If a requirement is a {@link RequirementImpl} then it will use {@link RequirementImpl#getName()} otherwise it will
     * call {@link Requirement#toString()}.
     * 
     * @param requirements The list of requirements to search
     * @return The collection of requirement names
     */
    private Collection<String> getNamesOfRequirements(Collection<Requirement> requirements) {
        Collection<String> requirementNames = new HashSet<String>();
        for (Requirement requirement : requirements) {
            // Can only get a nice name out of one of our requirements
            if (requirement instanceof RequirementImpl) {
                requirementNames.add(((RequirementImpl) requirement).getName());
            } else {
                // Not sure this can ever happen but still record it was missing if it does!
                requirementNames.add(requirement.toString());
            }
        }
        return requirementNames;
    }

    /**
     * This method will get the names and owning resources of all of the requirements. If a requirement is a {@link RequirementImpl} then it will use
     * {@link RequirementImpl#getName()} for the name and if {@link RequirementImpl#getResource()} returns a {@link ResourceImpl} then it will set
     * {@link ResourceImpl#getResource()} for the resource otherwise it will call {@link Requirement#toString()} for the name
     * and use <code>null</code> for the resource.
     * 
     * @param requirements The list of requirements to search
     * @return The collection of requirement names
     */
    private Collection<MissingRequirement> getNamesAndResourcesOfRequirements(Collection<Requirement> requirements) {
        Collection<MissingRequirement> requirementResources = new HashSet<MissingRequirement>();
        for (Requirement requirement : requirements) {
            // Can only get a nice name out of one of our requirements
            if (requirement instanceof RequirementImpl) {
                RequirementImpl requirementImpl = (RequirementImpl) requirement;
                Resource owningResource = requirementImpl.getResource();
                RepositoryResource owningMassiveResource = null;
                if (owningResource instanceof ResourceImpl) {
                    owningMassiveResource = ((ResourceImpl) owningResource).getResource();
                }
                requirementResources.add(new MissingRequirement(requirementImpl.getName(), owningMassiveResource));
            } else {
                // Not sure this can ever happen but still record it was missing if it does!
                requirementResources.add(new MissingRequirement(requirement.toString(), null));
            }
        }
        return requirementResources;
    }

    /**
     * This will search the supplied collection of requirements looking for instances of {@link ProductRequirement} and when found will append the list of products to the return
     * value.
     * 
     * @param requirements The list of requirements to search
     * @return The collection of products, may be empty but will not be <code>null</code>
     */
    private Collection<ProductRequirementInformation> getProductsNotFoundFromRequirements(Collection<Requirement> requirements) {
        Collection<ProductRequirementInformation> products = new HashSet<ProductRequirementInformation>();
        for (Requirement requirement : requirements) {
            if (requirement instanceof ProductRequirement) {
                products.addAll(((ProductRequirement) requirement).getProductInformation());
            }
        }
        return products;
    }

    /**
     * <p>Fully equivalent to calling:</p>
     * <code>MassiveResolver.resolve(Collections.singleton(toResolve));</br>
     * </code>
     * 
     * @see #resolve(Collection)
     */
    public Collection<List<RepositoryResource>> resolve(String toResolve) throws RepositoryException, RepositoryResolutionException {
        return this.resolve(Collections.singleton(toResolve));
    }

}
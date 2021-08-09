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

package com.ibm.ws.repository.connections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.ws.repository.common.enums.FilterPredicate;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.LicenseType;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.connections.internal.AbstractRepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.resources.AdminScriptResource;
import com.ibm.ws.repository.resources.ConfigSnippetResource;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.IfixResource;
import com.ibm.ws.repository.resources.ProductResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.SampleResource;
import com.ibm.ws.repository.resources.ToolResource;
import com.ibm.ws.repository.resources.internal.ProductResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl.MatchResult;
import com.ibm.ws.repository.resources.internal.ResourceCollector;
import com.ibm.ws.repository.resources.internal.ResourceCollector.DuplicatePolicy;

/**
 * This class represents a list of RepositoryConnection objects used to connect to repositories.
 * Do not use a RepositoryConnection inside multiple RepositoryConnectionList objects as setting a userAgent in the RepositoryConnectionList object
 * will update each RepositoryConnection it contains.
 */
public class RepositoryConnectionList extends ArrayList<RepositoryConnection> {

    /** SerialVesionUID */
    private static final long serialVersionUID = -4525841713558054978L;

    private String _userAgent;

    /**
     * Creates an empty RepositoryConnectionList
     */
    public RepositoryConnectionList() {}

    /**
     * Creates a new RepositoryConnectionList, populating it with the contents of the supplied list.
     *
     * @param list the list of RepositoryConnections
     */
    public RepositoryConnectionList(List<? extends RepositoryConnection> list) {
        addAll(list);
    }

    /**
     * Creates a new RepositoryConnectionList, populating it with a single RepositoryConnection
     *
     * @param repoConnection the RepositoryConnection
     */
    public RepositoryConnectionList(RepositoryConnection repoConnection) {
        add(repoConnection);
    }

    /**
     * Adds a new RepositoryConnection - the userAgent of this collection will automatically be pushed into
     * the connection if it is a RestRepositoryConnection {@inheritDoc}
     */
    @Override
    public boolean add(RepositoryConnection repoConnection) {
        boolean ret = super.add(repoConnection);
        if (repoConnection instanceof RestRepositoryConnection) {
            ((RestRepositoryConnection) repoConnection).setUserAgent(_userAgent);
        }
        return ret;
    }

    /**
     * @return the userAgent
     */
    public String getUserAgent() {
        return _userAgent;
    }

    /**
     * This object is used to hold a collection of {@link RepositoryConnection} objects, calling
     * this method will set the userAgent into ALL the RestRepositoryConnection objects in the collection.
     *
     * @param userAgent The userAgent to put into the first LoginInfoEntry in the collection
     */
    public void setUserAgent(String userAgent) {
        _userAgent = userAgent;
        for (RepositoryConnection repoConnection : this) {
            if (repoConnection instanceof RestRepositoryConnection) {
                ((RestRepositoryConnection) repoConnection).setUserAgent(_userAgent);
            }
        }
    }

    /*
     * ------------------------------------------------------------------------------------------------
     * The following methods all iterate through the list of repository connections and perform
     * the same action on each, merging the results together
     * ------------------------------------------------------------------------------------------------
     */

    private interface RepositoryInvoker<T extends RepositoryResource> {
        Collection<T> performActionOnRepository(RepositoryConnection connection) throws RepositoryBackendException;
    }

    private <T extends RepositoryResource> Collection<T> cycleThroughRepositories(RepositoryInvoker<T> invoker) throws RepositoryBackendException {
        return performCycle(invoker, new ResourceCollector<T>(DuplicatePolicy.FORBID_DUPLICATES));
    }

    private <T extends RepositoryResource> Collection<T> cycleThroughRepositoriesWithDupes(RepositoryInvoker<T> invoker) throws RepositoryBackendException {
        return performCycle(invoker, new ResourceCollector<T>(DuplicatePolicy.ALLOW_DUPLICATES));
    }

    private <T extends RepositoryResource> Collection<T> performCycle(RepositoryInvoker<T> invoker, ResourceCollector<T> resources) throws RepositoryBackendException {
        if (this.size() == 1) {
            // Nothing to iterate over, just return the first result directly
            return invoker.performActionOnRepository(this.get(0));
        }
        for (RepositoryConnection repoConnection : this) {
            Collection<T> reses = invoker.performActionOnRepository(repoConnection);
            for (T res : reses) {
                resources.add(res);
            }
        }
        return resources.getResourceCollection();
    }

    /**
     * Find the features that match the supplied search string, ProductDefinition and Visibility
     *
     * @param searchString
     * @param definition
     * @param visible
     * @return a collection of matching feature resources
     * @throws RepositoryException
     */
    @SuppressWarnings("unchecked")
    public Collection<EsaResource> findMatchingEsas(String searchString,
                                                    ProductDefinition definition,
                                                    Visibility visible) throws RepositoryException {
        Collection<? extends RepositoryResource> resources = findResources(searchString, Collections.singleton(definition), Collections.singleton(ResourceType.FEATURE), visible);
        return (Collection<EsaResource>) resources;
    }

    @SuppressWarnings("unchecked")
    public Collection<EsaResource> getMatchingEsas(ProductDefinition definition) throws RepositoryBackendException {
        Collection<EsaResource> ret = (Collection<EsaResource>) getResources(Collections.singleton(definition), Collections.singleton(ResourceType.FEATURE),
                                                                             null).get(ResourceType.FEATURE);
        if (ret == null) {
            ret = Collections.emptySet();
        }
        return ret;
    }

    /**
     * Get all features in the repositories that match the provided ProductDefinition (normally of the machine you are on)
     * and the supplied visibility setting.
     *
     * @param definition
     * @param visible
     * @return A collection of matching features
     * @throws RepositoryBackendException
     */
    @SuppressWarnings("unchecked")
    public Collection<EsaResource> getMatchingEsas(ProductDefinition definition, Visibility visible) throws RepositoryBackendException {
        Collection<EsaResource> ret = (Collection<EsaResource>) getResources(Collections.singleton(definition), Collections.singleton(ResourceType.FEATURE),
                                                                             visible).get(ResourceType.FEATURE);
        if (ret == null) {
            ret = Collections.emptySet();
        }
        return ret;
    }

    /**
     * This will obtain the addons from the repository that match the specified product definition
     *
     * @param definition The product definition to match against
     * @return Returns a Collection of {@link ProductResourceImpl} objects that represent the addons that match the product
     *         definition supplied
     * @throws RepositoryBackendException If there was a problem connecting to the repository
     */
    public Collection<ProductResource> getMatchingAddons(ProductDefinition definition) throws RepositoryBackendException {
        @SuppressWarnings("unchecked")
        Collection<ProductResourceImpl> addonList = (Collection<ProductResourceImpl>) getAllResources(ResourceType.ADDON);
        Collection<ProductResource> matchedAddons = new ArrayList<ProductResource>();
        for (ProductResourceImpl addon : addonList) {
            if (addon.matches(definition) == MatchResult.MATCHED) {
                matchedAddons.add(addon);
            }
        }
        return matchedAddons;
    }

    /**
     * This will return any ESAs that match the supplied <code>identifier</code>.
     * The matching is done on any of the symbolic name, short name or lower case short name
     * of the resource.
     *
     * @param attribute The attribute to match against
     * @param identifier The identifier to look for
     * @return The EsaResources that match the identifier
     * @throws RepositoryBackendException
     */
    public Collection<EsaResource> getMatchingEsas(final FilterableAttribute attribute, final String identifier) throws RepositoryBackendException {
        Collection<EsaResource> resources = cycleThroughRepositories(new RepositoryInvoker<EsaResource>() {
            @Override
            public Collection<EsaResource> performActionOnRepository(RepositoryConnection connection) throws RepositoryBackendException {
                return connection.getMatchingEsas(attribute, identifier);
            }
        });
        return resources;
    }

    /**
     * This will return any ESAs that match the supplied <code>identifier</code>.
     * The matching is done on any of the symbolic name, short name or lower case short name
     * of the resource.
     *
     * @param attribute The attribute to match against
     * @param identifier The identifier to look for
     * @return The EsaResources that match the identifier
     * @throws RepositoryBackendException
     */
    public Collection<SampleResource> getMatchingSamples(final FilterableAttribute attribute, final String identifier) throws RepositoryBackendException {
        Collection<SampleResource> resources = cycleThroughRepositories(new RepositoryInvoker<SampleResource>() {
            @Override
            public Collection<SampleResource> performActionOnRepository(RepositoryConnection connection) throws RepositoryBackendException {
                return connection.getMatchingSamples(attribute, identifier);
            }
        });

        return resources;
    }

    @SuppressWarnings("unchecked")
    public Collection<SampleResource> getMatchingSamples(ProductDefinition definition) throws RepositoryBackendException {
        Collection<ResourceType> types = new ArrayList<ResourceType>();
        types.add(ResourceType.OPENSOURCE);
        types.add(ResourceType.PRODUCTSAMPLE);

        Map<ResourceType, Collection<? extends RepositoryResource>> resourceMap = getResources(Collections.singleton(definition), types, null);

        Collection<SampleResource> samples = new ArrayList<SampleResource>();
        for (Collection<? extends RepositoryResource> sampleResource : resourceMap.values()) {
            samples.addAll((Collection<SampleResource>) sampleResource);
        }
        return samples;
    }

    /**
     * Returns all resources which match the supplied set of FilterPredicate objects.
     */
    public Collection<RepositoryResource> getMatchingResources(final FilterPredicate... predicates) throws RepositoryBackendException {
        Collection<RepositoryResource> resources = cycleThroughRepositories(new RepositoryInvoker<RepositoryResource>() {
            @Override
            public Collection<RepositoryResource> performActionOnRepository(RepositoryConnection connection) throws RepositoryBackendException {
                return connection.getMatchingResources(predicates);
            }
        });
        return resources;
    }

    /**
     * This method gets all the resources in this list of repositories. If a resource is found in multiple repositories
     * then the only the first one found will be returned (the search order is the order of repositories added to
     * this list).
     *
     * @return A {@link Collection} of {@link RepositoryResourceImpl} object
     * @throws RepositoryBackendException
     */
    public Collection<? extends RepositoryResource> getAllResources() throws RepositoryBackendException {
        Collection<RepositoryResource> resources = cycleThroughRepositories(new RepositoryInvoker<RepositoryResource>() {
            @SuppressWarnings("unchecked")
            @Override
            public Collection<RepositoryResource> performActionOnRepository(RepositoryConnection connection) throws RepositoryBackendException {
                return (Collection<RepositoryResource>) connection.getAllResources();
            }
        });
        return resources;
    }

    /**
     * This method gets all the resources in this list of repositories, this list may contain dupes if the same
     * asset is found in multiple repositories.
     *
     * @return A {@link Collection} of {@link RepositoryResourceImpl} object
     * @throws RepositoryBackendException
     */
    public Collection<? extends RepositoryResource> getAllResourcesWithDupes() throws RepositoryBackendException {
        Collection<RepositoryResource> resources = cycleThroughRepositoriesWithDupes(new RepositoryInvoker<RepositoryResource>() {
            @SuppressWarnings("unchecked")
            @Override
            public Collection<RepositoryResource> performActionOnRepository(RepositoryConnection connection) throws RepositoryBackendException {
                return (Collection<RepositoryResource>) connection.getAllResourcesWithDupes();
            }
        });
        return resources;
    }

    /**
     * Gets all resources of the specified type from the repositories.
     *
     * @return A {@link Collection} of {@link RepositoryResourceImpl} object
     * @throws RepositoryBackendException
     */
    public Collection<? extends RepositoryResource> getAllResources(final ResourceType type) throws RepositoryBackendException {
        Collection<RepositoryResource> resources = cycleThroughRepositories(new RepositoryInvoker<RepositoryResource>() {
            @SuppressWarnings("unchecked")
            @Override
            public Collection<RepositoryResource> performActionOnRepository(RepositoryConnection connection) throws RepositoryBackendException {
                return (Collection<RepositoryResource>) connection.getAllResources(type);
            }
        });
        return resources;
    }

    /**
     * Gets all resources of the specified type from the repositories.
     *
     * @return A {@link Collection} of {@link RepositoryResourceImpl} object
     * @throws RepositoryBackendException
     */
    public Collection<? extends RepositoryResource> getAllResourcesWithDupes(final ResourceType type) throws RepositoryBackendException {
        Collection<RepositoryResource> resources = cycleThroughRepositoriesWithDupes(new RepositoryInvoker<RepositoryResource>() {
            @SuppressWarnings("unchecked")
            @Override
            public Collection<RepositoryResource> performActionOnRepository(RepositoryConnection connection) throws RepositoryBackendException {
                return (Collection<RepositoryResource>) connection.getAllResourcesWithDupes(type);
            }
        });
        return resources;
    }

    /**
     * Gets all resources of the specified type and license type from the repositories.
     *
     * @param licenseType The {@link LicenseType} of the resources to obtain
     * @param type The {@link ResourceType} of resource to obtain
     * @return A {@link Collection} of {@link RepositoryResourceImpl} object
     * @throws RepositoryBackendException
     */
    public Collection<? extends RepositoryResource> getAllResources(LicenseType licenseType,
                                                                    ResourceType type) throws RepositoryBackendException {
        Collection<? extends RepositoryResource> allResources = getAllResources(type);

        if (licenseType != null) {
            Collection<RepositoryResource> licensedResources = new ArrayList<RepositoryResource>();

            for (RepositoryResource resource : getAllResources(type)) {
                if (licenseType.equals(resource.getLicenseType())) {
                    licensedResources.add(resource);
                }
            }

            return licensedResources;
        } else {
            return allResources;
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<AdminScriptResource> getAllAdminScripts() throws RepositoryBackendException {
        return (Collection<AdminScriptResource>) getAllResources(ResourceType.ADMINSCRIPT);
    }

    @SuppressWarnings("unchecked")
    public Collection<ConfigSnippetResource> getAllConfigSnippets() throws RepositoryBackendException {
        return (Collection<ConfigSnippetResource>) getAllResources(ResourceType.CONFIGSNIPPET);
    }

    /**
     * This methods returns a list of all features in the supplied repositories
     *
     * @return A list of resources representing the features in the repositories.
     * @throws RepositoryBackendException
     */
    @SuppressWarnings("unchecked")
    public Collection<EsaResource> getAllFeatures() throws RepositoryBackendException {
        return (Collection<EsaResource>) getAllResources(ResourceType.FEATURE);
    }

    /**
     * This method returns a Collection of all features in the repositories
     * with the specified license type.
     *
     * @Param licenseType the type of license in question - IPLA, ILAN, etc
     * @return A list of resources representing the features in the repositories
     *         with the requested license type
     * @throws RepositoryBackendException
     */
    @SuppressWarnings("unchecked")
    public Collection<EsaResource> getAllFeatures(LicenseType licenseType) throws RepositoryBackendException {
        return (Collection<EsaResource>) getAllResources(licenseType, ResourceType.FEATURE);
    }

    /**
     *
     * @return
     * @throws RepositoryBackendException
     */
    @SuppressWarnings("unchecked")
    public Collection<IfixResource> getAllIfixes() throws RepositoryBackendException {
        return (Collection<IfixResource>) getAllResources(ResourceType.IFIX);
    }

    @SuppressWarnings("unchecked")
    public Collection<ProductResource> getAllProducts() throws RepositoryBackendException {
        Collection<ResourceType> types = new HashSet<ResourceType>();
        types.add(ResourceType.INSTALL);
        types.add(ResourceType.ADDON);

        Map<ResourceType, Collection<? extends RepositoryResource>> productMap = getResources(Collections.<ProductDefinition> emptySet(), types, null);

        Collection<ProductResource> products = new ArrayList<ProductResource>();

        for (Collection<? extends RepositoryResource> productResources : productMap.values()) {
            products.addAll((Collection<? extends ProductResource>) productResources);
        }
        return products;
    }

    /**
     * Get all products with a given license type
     *
     * @param licenseType
     * @throws RepositoryBackendException
     */
    @SuppressWarnings("unchecked")
    public Collection<ProductResourceImpl> getAllProducts(LicenseType licenseType) throws RepositoryBackendException {
        Collection<ProductResourceImpl> product = (Collection<ProductResourceImpl>) getAllResources(licenseType, ResourceType.INSTALL);
        Collection<ProductResourceImpl> addons = (Collection<ProductResourceImpl>) getAllResources(licenseType, ResourceType.ADDON);
        product.addAll(addons);
        return product;
    }

    /**
     * @return
     * @throws RepositoryBackendException
     */
    @SuppressWarnings("unchecked")
    public Collection<SampleResource> getAllSamples() throws RepositoryBackendException {
        Collection<ResourceType> types = new HashSet<ResourceType>();
        types.add(ResourceType.PRODUCTSAMPLE);
        types.add(ResourceType.OPENSOURCE);

        Map<ResourceType, Collection<? extends RepositoryResource>> sampleMap = getResources(Collections.<ProductDefinition> emptySet(), types, null);

        Collection<SampleResource> samples = new ArrayList<SampleResource>();

        for (Collection<? extends RepositoryResource> sampleResources : sampleMap.values()) {
            samples.addAll((Collection<? extends SampleResource>) sampleResources);
        }
        return Collections.unmodifiableCollection(samples);
    }

    /**
     *
     * @throws RepositoryBackendException
     */
    @SuppressWarnings("unchecked")
    public Collection<ToolResource> getAllTools() throws RepositoryBackendException {
        return (Collection<ToolResource>) getAllResources(ResourceType.TOOL);
    }

    /**
     * Get all products with a given license type
     *
     * @param licenseType
     * @throws RepositoryBackendException
     */
    @SuppressWarnings("unchecked")
    public Collection<ToolResource> getAllTools(LicenseType licenseType) throws RepositoryBackendException {
        return (Collection<ToolResource>) getAllResources(licenseType, ResourceType.TOOL);
    }

    public <T extends RepositoryResourceImpl> T getResourceWithVanityRelativeURL(String vanityRelativeURL) throws RepositoryBackendException {
        @SuppressWarnings("unchecked")
        Collection<T> resources = (Collection<T>) getAllResources();
        for (T res : resources) {
            if (res.getVanityURL().equals(vanityRelativeURL)) {
                return res;
            }
        }
        return null;
    }

    public Collection<RepositoryResourceImpl> getAllResourcesWithVanityRelativeURL(String vanityRelativeURL) throws RepositoryBackendException {
        ResourceCollector<RepositoryResourceImpl> hits = new ResourceCollector<RepositoryResourceImpl>(DuplicatePolicy.ALLOW_DUPLICATES);

        @SuppressWarnings("unchecked")
        Collection<RepositoryResourceImpl> resources = (Collection<RepositoryResourceImpl>) getAllResources();

        for (RepositoryResourceImpl res : resources) {
            if (res.getVanityURL().equals(vanityRelativeURL)) {
                hits.add(res);
            }
        }
        return hits.getResourceCollection();
    }

    /**
     * Gets all resources of the specified <code>types</code> from the repositories returning only those that are relevant to the <code>productDefinitions</code>.
     *
     * @param productDefinitions The products that these resources will be installed into. Can be <code>null</code> or empty indicating resources for any product should be obtained
     *            (they will just be filtered by type).
     * @param types The {@link ResourceType} of resource to obtain. <code>null</code> indicates that all types should be obtained.
     * @param visibility The {@link Visibility} of resources to obtain. <code>null</code> indicates that resources with any visibility should be obtained. This is only relevant if
     *            {@link ResourceType#FEATURE} is one of the <code>types</code> being obtained, it is ignored for other types.
     * @return A Map mapping the type to the {@link Collection} of {@link RepositoryResourceImpl} object. There may be a <code>null</code> collection for a supplied type, this
     *         indicates
     *         no assets of that type were found
     * @throws RepositoryBackendException
     */
    public Map<ResourceType, Collection<? extends RepositoryResource>> getResources(Collection<ProductDefinition> productDefinitions,
                                                                                    Collection<ResourceType> types,
                                                                                    Visibility visibility) throws RepositoryBackendException {
        // If there's only one connection, just return the result from it
        if (this.size() == 1) {
            return this.get(0).getResources(productDefinitions, types, visibility);
        }

        // Otherwise, we need to merge the results
        Map<ResourceType, ResourceCollector<RepositoryResource>> combinedMap = new HashMap<ResourceType, ResourceCollector<RepositoryResource>>();

        for (RepositoryConnection connection : this) {
            AbstractRepositoryConnection conn = (AbstractRepositoryConnection) connection;

            Map<ResourceType, Collection<? extends RepositoryResource>> singleRepo = conn.getResources(productDefinitions, types, visibility);
            for (Entry<ResourceType, Collection<? extends RepositoryResource>> entry : singleRepo.entrySet()) {

                Collection<? extends RepositoryResource> singleType = entry.getValue();

                ResourceCollector<RepositoryResource> overallType = combinedMap.get(entry.getKey());
                if (overallType == null) {
                    overallType = new ResourceCollector<RepositoryResource>(DuplicatePolicy.FORBID_DUPLICATES);
                    combinedMap.put(entry.getKey(), overallType);
                }

                overallType.addAll(singleType);
            }
        }

        Map<ResourceType, Collection<? extends RepositoryResource>> returnMap = new HashMap<ResourceType, Collection<? extends RepositoryResource>>();

        for (Entry<ResourceType, ResourceCollector<RepositoryResource>> entry : combinedMap.entrySet()) {
            returnMap.put(entry.getKey(), entry.getValue().getResourceCollection());
        }

        return returnMap;
    }

    /**
     * Searches for resources that contained <code>searchTerm</code> from the repository returning only those that are relevant to the <code>productDefinitions</code>.
     *
     * @param searchTerm The word(s) to search for.
     * @param productDefinitions The products that these resources will be installed into. Can be <code>null</code> or empty indicating resources for any product should be obtained
     *            (they will just be filtered by type).
     * @param types The {@link ResourceType} of resource to obtain. <code>null</code> indicates that all types should be obtained.
     * @param visibility The {@link Visibility} of resources to obtain. <code>null</code> indicates that resources with any visibility should be obtained. This is only relevant if
     *            {@link ResourceType#FEATURE} is one of the <code>types</code> being obtained, it is ignored for other types.
     * @return A LinkedHashSet of resources that match the search term for this product. They will be ordered according to the order returned from the repository which should be in
     *         priority order.
     * @throws RepositoryBackendException If there is an error connecting to one of the repositories.
     */
    public Collection<? extends RepositoryResource> findResources(final String searchTerm,
                                                                  final Collection<ProductDefinition> productDefinitions,
                                                                  final Collection<ResourceType> types,
                                                                  final Visibility visibility) throws RepositoryBackendException {
        return cycleThroughRepositories(new RepositoryInvoker<RepositoryResource>() {
            @SuppressWarnings("unchecked")
            @Override
            public Collection<RepositoryResource> performActionOnRepository(RepositoryConnection connection) throws RepositoryBackendException {
                return (Collection<RepositoryResource>) connection.findResources(searchTerm, productDefinitions, types, visibility);
            }
        });
    }

}

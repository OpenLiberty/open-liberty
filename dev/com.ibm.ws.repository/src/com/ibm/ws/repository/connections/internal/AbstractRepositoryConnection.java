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
package com.ibm.ws.repository.connections.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.ibm.ws.repository.common.enums.FilterPredicate;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBackendIOException;
import com.ibm.ws.repository.exceptions.RepositoryBackendRequestFailureException;
import com.ibm.ws.repository.exceptions.RepositoryBadDataException;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.SampleResource;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.internal.ResourceCollector;
import com.ibm.ws.repository.resources.internal.ResourceCollector.DuplicatePolicy;
import com.ibm.ws.repository.resources.internal.ResourceFactory;
import com.ibm.ws.repository.resources.internal.SampleResourceImpl;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.Asset;

/**
 *
 */
public abstract class AbstractRepositoryConnection implements RepositoryConnection {

    @Override
    public boolean isRepositoryAvailable() {
        try {
            checkRepositoryStatus();
        } catch (IOException ex) {
            // Catch exception, and return false to say repo is not available. Use checkRepositoryStatus
            // to see more information about why a repo is not available instead of this method
            return false;
        } catch (RequestFailureException e) {
            // Catch exception, and return false to say repo is not available. Use checkRepositoryStatus
            // to see more information about why a repo is not available instead of this method
            return false;
        }
        return true;
    }

    @Override
    public void checkRepositoryStatus() throws IOException, RequestFailureException {
        createClient().checkRepositoryStatus();
    }

    @Override
    public RepositoryResource getResource(String id) throws RepositoryBackendException, RepositoryBadDataException {
        RepositoryReadableClient client = createClient();
        RepositoryResourceImpl res = null;
        try {
            Asset ass = client.getAsset(id);
            if (null != ass) {
                res = ResourceFactory.getInstance().createResourceFromAsset(ass, this);
            }
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Failed to create resource from Asset", ioe, this);
        } catch (BadVersionException bvx) {
            throw new RepositoryBadDataException("BadVersion reading attachment", id, bvx);
        } catch (RequestFailureException bfe) {
            throw new RepositoryBackendRequestFailureException(bfe, this);
        }
        return res;
    }

    @Override
    public Collection<RepositoryResource> getAllResources() throws RepositoryBackendException {
        return getAllResources(new ResourceCollector<RepositoryResource>(DuplicatePolicy.FORBID_DUPLICATES));
    }

    @Override
    public Collection<RepositoryResource> getAllResourcesWithDupes() throws RepositoryBackendException {
        return getAllResources(new ResourceCollector<RepositoryResource>(DuplicatePolicy.ALLOW_DUPLICATES));
    }

    @Override
    public Collection<RepositoryResource> getAllResources(ResourceType type) throws RepositoryBackendException {
        return getAllResources(type, new ResourceCollector<RepositoryResource>(DuplicatePolicy.FORBID_DUPLICATES));
    }

    @Override
    public Collection<RepositoryResource> getAllResourcesWithDupes(ResourceType type) throws RepositoryBackendException {
        return getAllResources(type, new ResourceCollector<RepositoryResource>(DuplicatePolicy.ALLOW_DUPLICATES));
    }

    @Override
    public Map<ResourceType, Collection<? extends RepositoryResource>> getResources(Collection<ProductDefinition> productDefinitions, Collection<ResourceType> types,
                                                                                    Visibility visibility) throws RepositoryBackendException {
        /*
         * We've been asked to get all the resources that apply to a set of products. Some of the values we can filter on on the server and some we can't. Those we can filter on
         * are:
         * -- Product ID
         * -- Version (see comment below)
         * -- Visibility (see other comment below)
         *
         * The fields we can't filter on are:
         * -- Edition: no edition = all editions and you can't search for a missing field
         * -- InstallType: not install type = all install types and you can't search for a missing field
         *
         * Version is special as there are two ways we version content in the repository. It may have a specific version (minVersion=maxVersion) or a just a specific version (no
         * maxVersion). As we can't search for a missing field there is an additional field stored on the applies to filter info to allow us to search for the absence of a max
         * version. All massive queries use AND so we first search for the specific version then for the absence of the max version.
         *
         * Visibility is also special as it only applies to features. If you are getting anything other than features and put the visibility on the URL then it will come back with
         * zero hits (or only hits for features) so we can only do this efficiently in Massive if only searching for features, otherwise have to do it here.
         *
         * Once we have all the results back then feed it into the matches method to a) ensure that the content that just has a min version is in the right range b) also filter out
         * anything from fields that we can't filter on the server.
         */
        Visibility visibilityForMassiveFilter = null;
        if (visibility != null) {
            if (types != null && types.size() == 1 && types.contains(ResourceType.FEATURE)) {
                // As per comment above, can only use the visibility on the URL filter if we are only looking for features
                visibilityForMassiveFilter = visibility;
            }
        }

        Collection<String> productIds = new HashSet<String>();
        Collection<String> productVersions = new HashSet<String>();
        if (productDefinitions != null) {
            for (ProductDefinition productDefinition : productDefinitions) {
                String id = productDefinition.getId();
                if (id != null) {
                    productIds.add(id);
                }
                String version = productDefinition.getVersion();
                if (version != null) {
                    productVersions.add(version);
                }
            }
        }
        ResourceCollector<RepositoryResourceImpl> resources = new ResourceCollector<RepositoryResourceImpl>(DuplicatePolicy.FORBID_DUPLICATES);
        try {
            RepositoryReadableClient client = createClient();
            // We may end up with duplicate assets from these two calls but that is ok as we are using the ResourceList as the collection of resources which removes duplicates
            Collection<Asset> assets = client.getAssets(types, productIds, visibilityForMassiveFilter, productVersions);
            assets.addAll(client.getAssetsWithUnboundedMaxVersion(types, productIds, visibilityForMassiveFilter));
            for (Asset asset : assets) {
                resources.add(ResourceFactory.getInstance().createResourceFromAsset(asset, this));
            }
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Failed to obtain the assets from massive", ioe, this);
        } catch (RequestFailureException e) {
            throw new RepositoryBackendRequestFailureException(e, this);
        }

        // This will have returned some assets that aren't valid due to version or stuff we can't filter on so run it through a local filtering as well
        Map<ResourceType, Collection<? extends RepositoryResource>> returnMap = new HashMap<ResourceType, Collection<? extends RepositoryResource>>();
        for (RepositoryResourceImpl massiveResource : resources.getResourceCollection()) {
            if (!massiveResource.doesResourceMatch(productDefinitions, visibility)) {
                continue;
            }

            ResourceType type = massiveResource.getType();
            @SuppressWarnings("unchecked")
            Collection<RepositoryResource> resourcesOfType = (Collection<RepositoryResource>) returnMap.get(type);
            if (resourcesOfType == null) {
                resourcesOfType = new ArrayList<RepositoryResource>();
                returnMap.put(type, resourcesOfType);
            }
            resourcesOfType.add(massiveResource);
        }
        return returnMap;
    }

    @Override
    public Collection<SampleResource> getMatchingSamples(FilterableAttribute attribute, String identifier) throws RepositoryBackendException {
        ResourceCollector<SampleResource> results = new ResourceCollector<SampleResource>(DuplicatePolicy.FORBID_DUPLICATES);

        Map<FilterableAttribute, Collection<String>> filters = new HashMap<FilterableAttribute, Collection<String>>();

        // We want to filter on samples (both open source and product)
        filters.put(FilterableAttribute.TYPE, Arrays.asList(ResourceType.OPENSOURCE.getValue(), ResourceType.PRODUCTSAMPLE.getValue()));

        // We also want to filter on the attribute passed in to us
        filters.put(attribute, Collections.singleton(identifier));

        RepositoryReadableClient client = createClient();
        try {
            Collection<Asset> assets = client.getFilteredAssets(filters);
            for (Asset ass : assets) {
                SampleResourceImpl res = (SampleResourceImpl) ResourceFactory.getInstance().createResourceFromAsset(ass, this);
                results.add(res);
            }
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Failed to obtain the assets from massive", ioe, this);
        } catch (RequestFailureException e) {
            throw new RepositoryBackendRequestFailureException(e, this);
        }
        return results.getResourceCollection();
    }

    @Override
    public Collection<EsaResource> getMatchingEsas(FilterableAttribute attribute, String identifier) throws RepositoryBackendException {
        ResourceCollector<EsaResource> resources = new ResourceCollector<EsaResource>(DuplicatePolicy.FORBID_DUPLICATES);

        Map<FilterableAttribute, Collection<String>> filters = new HashMap<FilterableAttribute, Collection<String>>();
        filters.put(FilterableAttribute.TYPE, Collections.singleton(ResourceType.FEATURE.getValue()));
        filters.put(attribute, Collections.singleton(identifier));

        RepositoryReadableClient client = createClient();
        try {
            Collection<Asset> assets = client.getFilteredAssets(filters);
            for (Asset ass : assets) {
                EsaResourceImpl res = (EsaResourceImpl) ResourceFactory.getInstance().createResourceFromAsset(ass, this);
                resources.add(res);
            }
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Failed to obtain the assets from massive", ioe, this);
        } catch (RequestFailureException e) {
            throw new RepositoryBackendRequestFailureException(e, this);
        }
        return resources.getResourceCollection();
    }

    @Override
    public Collection<RepositoryResource> getMatchingResources(FilterPredicate... predicates) throws RepositoryBackendException {

        List<RepositoryResource> resources = new ArrayList<RepositoryResource>();

        Map<FilterableAttribute, Collection<String>> filters = new HashMap<FilterableAttribute, Collection<String>>();
        for (FilterPredicate predicate : predicates) {
            filters.put(predicate.getAttribute(), predicate.getValues());
        }

        RepositoryReadableClient client = createClient();
        try {
            Collection<Asset> assets = client.getFilteredAssets(filters);
            for (Asset ass : assets) {
                RepositoryResource res = ResourceFactory.getInstance().createResourceFromAsset(ass, this);
                resources.add(res);
            }
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Failed to obtain the assets from massive", ioe, this);
        } catch (RequestFailureException e) {
            throw new RepositoryBackendRequestFailureException(e, this);
        }
        return resources;

    }

    @Override
    public Collection<? extends RepositoryResource> findResources(String searchTerm, Collection<ProductDefinition> productDefinitions,
                                                                  Collection<ResourceType> types,
                                                                  Visibility visibility) throws RepositoryBackendException {
        /*
         * Unlike the getResources method where we can do lots of nice filtering on the server to ensure we only get resources that are relevant to the current product when you do
         * a search you can only filter on fields that are also indexed. Currently this is a very limited subset (type is the only one that is useful for us here) so just run the
         * search then filter here... at least it makes the implementation of this slightly simpler than the getResources below!
         */

        ResourceCollector<RepositoryResource> resources = new ResourceCollector<RepositoryResource>(DuplicatePolicy.FORBID_DUPLICATES);

        // If we have an empty search string we can defer to the faster getResources method
        if (null == searchTerm || searchTerm.isEmpty()) {
            Map<ResourceType, Collection<? extends RepositoryResource>> mapResults = getResources(productDefinitions, types, visibility);
            for (Collection<? extends RepositoryResource> mapEntry : mapResults.values()) {
                resources.addAll(mapEntry);
            }
        } else {
            if (this instanceof RestRepositoryConnection) {
                RepositoryReadableClient client = createClient();

                List<Asset> assets;
                try {
                    assets = client.findAssets(searchTerm, types);
                } catch (IOException ioe) {
                    throw new RepositoryBackendIOException("Failed to obtain the assets from massive", ioe, this);
                } catch (RequestFailureException e) {
                    throw new RepositoryBackendRequestFailureException(e, this);
                }
                for (Asset ass : assets) {
                    RepositoryResourceImpl resource = ResourceFactory.getInstance().createResourceFromAsset(ass, this);
                    if (resource.doesResourceMatch(productDefinitions, visibility)) {
                        resources.add(resource);
                    }
                }
            }
        }
        return resources.getResourceCollection();
    }

    /**
     * Create client to the repository. This is implemented by the concrete types of connection
     *
     * @param entry
     */
    public abstract RepositoryReadableClient createClient();

    private Collection<RepositoryResource> getAllResources(ResourceCollector<RepositoryResource> resources) throws RepositoryBackendException {
        RepositoryReadableClient client = createClient();
        Collection<Asset> assets;
        try {
            assets = client.getAllAssets();
            for (Asset ass : assets) {
                resources.add(ResourceFactory.getInstance().createResourceFromAsset(ass, this));
            }
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Failed to obtain the assets from massive", ioe, this);
        } catch (RequestFailureException e) {
            throw new RepositoryBackendRequestFailureException(e, this);
        }
        return resources.getResourceCollection();
    }

    private Collection<RepositoryResource> getAllResources(ResourceType type, ResourceCollector<RepositoryResource> resources) throws RepositoryBackendException {
        RepositoryReadableClient client = createClient();
        Collection<Asset> assets;
        try {
            assets = client.getAssets(type);
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Failed to obtain the assets from massive", ioe, this);
        } catch (RequestFailureException e) {
            throw new RepositoryBackendRequestFailureException(e, this);
        }
        for (Asset ass : assets) {
            RepositoryResource res = ResourceFactory.getInstance().createResourceFromAsset(ass, this);
            resources.add(res);
        }
        return resources.getResourceCollection();
    }

}

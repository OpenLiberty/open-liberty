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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import com.ibm.ws.repository.common.enums.FilterPredicate;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBadDataException;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.SampleResource;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;

/**
 * Abstract class to represent a repository connection. Derived classes will provide implementation specifics.
 */
public interface RepositoryConnection {

    /**
     * Checks if the repository is available.
     *
     * @return True if the repository is available and false otherwise
     */
    public boolean isRepositoryAvailable();

    /**
     * Checks the repository availability
     *
     * @return This will return void if all is ok but will throw an exception if
     *         there are any problems
     */
    public void checkRepositoryStatus() throws IOException, RequestFailureException;

    /**
     * Gets the location of the repository
     */
    public String getRepositoryLocation();

    /**
     * Gets the specific resource specified by the supplied id
     *
     * @return The resource for the specified id, returns null if no asset was found with the specified id
     * @throws RepositoryBackendException
     * @throws RepositoryBadDataException
     */
    public RepositoryResource getResource(String id) throws RepositoryBackendException, RepositoryBadDataException;

    /**
     * This will return any Samples that match the supplied <code>identifier</code>. The matching is done on the same attributes as can be used in the name on a string passed to
     * the {@link MassiveResolver#resolve(String)} method, namely it is either the short name or lower case short name of the resource.
     *
     * @param attribute The attribute to match against
     * @param identifier The identifier to look for
     * @param loginInfo The repository login information to load the resources from
     * @return The EsaResources that match the identifier
     * @throws RepositoryBackendException
     */
    public Collection<SampleResource> getMatchingSamples(FilterableAttribute attribute, String identifier) throws RepositoryBackendException;

    /**
     * This will return any ESAs that match the supplied <code>identifier</code>. The matching is done on the same attributes as can be used in the name on a string passed to the
     * {@link MassiveResolver#resolve(String)} method, namely it is either the symbolic name, short name or lower case short name of the resource.
     *
     * @param attribute The attribute to match against
     * @param identifier The identifier to look for
     * @param loginInfo The repository login information to load the resources from
     * @return The EsaResources that match the identifier
     * @throws RepositoryBackendException
     */
    public Collection<EsaResource> getMatchingEsas(FilterableAttribute attribute, String identifier) throws RepositoryBackendException;

    /**
     * This method will return all resources that match the supplied filter logic
     *
     * @param predicate The filters + logic to apply
     * @return The resources of any type that match the filters
     * @throws RepositoryBackendException
     */
    public Collection<RepositoryResource> getMatchingResources(FilterPredicate... predicate) throws RepositoryBackendException;

    /**
     * This method gets all the resources in this repository
     *
     * @return A {@link Collection} of {@link RepositoryResourceImpl} object
     * @throws RepositoryBackendException
     */
    public Collection<? extends RepositoryResource> getAllResources() throws RepositoryBackendException;

    /**
     * Gets all resources of the specified type from all the repositories specified
     *
     * @return A {@link Collection} of {@link RepositoryResourceImpl} object
     * @throws RepositoryBackendException
     */
    public Collection<? extends RepositoryResource> getAllResources(ResourceType type) throws RepositoryBackendException;

    /**
     * This method gets all the resources in this repository
     *
     * @return A {@link Collection} of {@link RepositoryResourceImpl} object
     * @throws RepositoryBackendException
     */
    public Collection<? extends RepositoryResource> getAllResourcesWithDupes() throws RepositoryBackendException;

    /**
     * Gets all resources of the specified type from all the repositories specified
     *
     * @return A {@link Collection} of {@link RepositoryResourceImpl} object
     * @throws RepositoryBackendException
     */
    public Collection<? extends RepositoryResource> getAllResourcesWithDupes(ResourceType type) throws RepositoryBackendException;

    /**
     * Gets all resources of the specified <code>types</code> from Massive returning only those that are relevant to the <code>productDefinitions</code>.
     *
     * @param productDefinitions The products that these resources will be installed into. Can be <code>null</code> or empty indicating resources for any product should be obtained
     *            (they will just be filtered by type).
     * @param types The {@link RepositoryResourceImpl.ResourceType} of resource to obtain. <code>null</code> indicates that all types should be obtained.
     * @param visibility The {@link Visibility} of resources to obtain. <code>null</code> indicates that resources with any visibility should be obtained. This is only relevant if
     *            {@link ResourceType#FEATURE} is one of the <code>types</code> being obtained, it is ignored for other types.
     * @param RepositoryConnectionList the login information for the repository. Must not be <code>null</code>.
     * @return A Map mapping the type to the {@link Collection} of {@link RepositoryResourceImpl} object. There may be a <code>null</code> collection for a supplied type, this
     *         indicates
     *         no assets of that type were found
     * @throws RepositoryBackendException
     */
    public Map<ResourceType, Collection<? extends RepositoryResource>> getResources(Collection<ProductDefinition> productDefinitions, Collection<ResourceType> types,
                                                                                    Visibility visibility) throws RepositoryBackendException;

    /**
     * Searches for resources that contained <code>searchTerm</code> from the repository returning only those that are relevant to the <code>productDefinitions</code>.
     *
     * @param searchTerm The word(s) to search for.
     * @param productDefinitions The products that these resources will be installed into. Can be <code>null</code> or empty indicating resources for any product should be obtained
     *            (they will just be filtered by type).
     * @param types The {@link RepositoryResourceImpl.ResourceType} of resource to obtain. <code>null</code> indicates that all types should be obtained.
     * @param visibility The {@link Visibility} of resources to obtain. <code>null</code> indicates that resources with any visibility should be obtained. This is only relevant if
     *            {@link ResourceType#FEATURE} is one of the <code>types</code> being obtained, it is ignored for other types.
     * @param RepositoryConnectionList the login information for the repository. Must not be <code>null</code>.
     * @return A LinkedHashSet of resources that match the search term for this product. They will be ordered according to the order returned from the repository which should be in
     *         priority order.
     * @throws RepositoryBackendException If there is an error connecting to one of the repositories.
     */
    public Collection<? extends RepositoryResource> findResources(String searchTerm, Collection<ProductDefinition> productDefinitions,
                                                                  Collection<ResourceType> types,
                                                                  Visibility visibility) throws RepositoryBackendException;
}

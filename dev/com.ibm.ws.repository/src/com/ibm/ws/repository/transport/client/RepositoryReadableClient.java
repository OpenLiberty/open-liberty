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
package com.ibm.ws.repository.transport.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;

/**
 *
 */
public interface RepositoryReadableClient extends RepositoryClient {

    /**
     * @param assetId The id of the asset to get
     * @return The asset represented by the supplied id
     */
    public Asset getAsset(final String assetId) throws IOException, BadVersionException, RequestFailureException;

    /**
     * @return Gets a list of all the assets in this repository.
     */
    public Collection<Asset> getAllAssets() throws IOException, RequestFailureException;

    /**
     * This method will return all of the assets of a specific type in Massive.
     * It will just return a summary of each asset and not include any {@link Attachment}s.
     *
     * @param type
     *            The type to look for, if <code>null</code> this method behaves
     *            in the same way as {@link #getAllAssets()}
     * @return A collection of the assets of that type
     * @throws IOException
     * @throws RequestFailureException
     */
    public Collection<Asset> getAssets(final ResourceType type) throws IOException, RequestFailureException;

    /**
     * This method will return all of the assets matching specific filters in Massive.
     * It will just return a summary of each asset and not include any {@link Attachment}s.
     *
     * @param types
     *            The types to look for or <code>null</code> will return all types
     * @param productIds The product IDs to look for. Should not be <code>null</code> although supplying this will return assets for any product ID
     * @param visibility The visibility to look for or <code>null</code> will return all visibility values (or none)
     * @param productVersions The values of the minimum version in the appliesToFilterInfo to look for
     * @return A collection of the assets of that type
     * @throws IOException
     * @throws RequestFailureException
     */
    public Collection<Asset> getAssets(final Collection<ResourceType> types, final Collection<String> productIds,
                                       final Visibility visibility, final Collection<String> productVersions) throws IOException, RequestFailureException;

    /**
     * This method will return all of the assets matching specific filters in Massive that do not have a maximum version in their applies to filter info.
     * It will just return a summary of each asset and not include any {@link Attachment}s.
     *
     * @param types
     *            The types to look for or <code>null</code> will return all types
     * @param productIds The product IDs to look for. Should not be <code>null</code> although supplying this will return assets for any product ID
     * @param visibility The visibility to look for or <code>null</code> will return all visibility values (or none)
     * @return A collection of the assets of that type
     * @throws IOException
     * @throws RequestFailureException
     */
    public Collection<Asset> getAssetsWithUnboundedMaxVersion(final Collection<ResourceType> types,
                                                              final Collection<String> rightProductIds,
                                                              final Visibility visibility) throws IOException, RequestFailureException;

    /**
     * @param assetId The id of the asset which contains the attachment
     * @param attachmentId The id of the attachment within the asset
     * @return
     */
    public InputStream getAttachment(final Asset asset, final Attachment attachment) throws IOException, BadVersionException, RequestFailureException;

    /**
     * Find assets based on the <code>searchString</code>.
     *
     * NOTE: TODO at the moment this only works when called against an unauthenticated Client
     * due to a problem with how the stores are defined (the company values are defined
     * incorrectly).
     *
     * @param searchString The string to search for
     * @param types The types to filter the results for
     * @return The assets that match the search string and type
     * @throws IOException
     * @throws RequestFailureException
     */
    public List<Asset> findAssets(final String searchString, final Collection<ResourceType> types) throws IOException, RequestFailureException;

    /**
     * This will obtain assets from Massive using the supplied filters. The map can contain <code>null</code> or empty collections of values, in which case they will not be used in
     * the filter.
     *
     * @param filters A map of attributes to filter on mapped to the values to use
     * @return The filtered assets
     * @throws IOException
     * @throws RequestFailureException
     */
    public Collection<Asset> getFilteredAssets(final Map<FilterableAttribute, Collection<String>> filters) throws IOException, RequestFailureException;

    /**
     * Checks the repository availability
     *
     * @return This will return void if all is ok but will throw an exception if
     *         there are any problems
     * @throws RequestFailureException
     */
    public void checkRepositoryStatus() throws IOException, RequestFailureException;

}

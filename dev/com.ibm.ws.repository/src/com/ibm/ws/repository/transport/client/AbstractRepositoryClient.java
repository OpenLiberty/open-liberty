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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;

/**
 *
 */
public abstract class AbstractRepositoryClient implements RepositoryReadableClient {

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
    @Override
    public Collection<Asset> getAssets(final Collection<ResourceType> types, final Collection<String> productIds, final Visibility visibility,
                                       final Collection<String> productVersions) throws IOException, RequestFailureException {
        return getFilteredAssets(types, productIds, visibility, productVersions, false);
    }

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
    @Override
    public Collection<Asset> getAssetsWithUnboundedMaxVersion(final Collection<ResourceType> types, final Collection<String> rightProductIds,
                                                              final Visibility visibility) throws IOException, RequestFailureException {
        return getFilteredAssets(types, rightProductIds, visibility, null, true);
    }

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
    @Override
    public Collection<Asset> getAssets(final ResourceType type) throws IOException, RequestFailureException {
        Collection<ResourceType> types;
        if (type == null) {
            types = null;
        } else {
            types = Collections.singleton(type);
        }
        return getFilteredAssets(types, null, null, null, false);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<Asset> getFilteredAssets(final Map<FilterableAttribute, Collection<String>> filters) throws IOException, RequestFailureException {
        // Were any filters defined?
        if (filters == null || allFiltersAreEmpty(filters)) {
            return getAllAssets();
        }

        Collection<Asset> allAssets = getAllAssets();
        Collection<Asset> filtered = new ArrayList<Asset>();

        assetLoop: for (Asset asset : allAssets) {
            filterAttribLoop: for (Entry<FilterableAttribute, Collection<String>> entry : filters.entrySet()) {
                FilterableAttribute attrib = entry.getKey();
                // List of values we are looking for
                Collection<String> values = entry.getValue();
                // List of values in the asset
                Collection<String> assetValues = getValues(attrib, asset);

                if (values != null && values.size() != 0) {
                    // Check each required value and see if the asset has it
                    for (String filterValue : values) {
                        // if we find a match stop checking this attribute and move to next attribute
                        if (assetValues.contains(filterValue)) {
                            continue filterAttribLoop;
                        }
                    }
                    // We never found a match for this attribute so move to next asset
                    continue assetLoop;
                }
            }
            filtered.add(asset);
        }

        return filtered;
    }

    @Override
    public List<Asset> findAssets(final String searchString, final Collection<ResourceType> types) throws IOException, RequestFailureException {
        Collection<Asset> assets = getAssets(types, null, null, null);
        List<Asset> foundAssets = new ArrayList<Asset>();
        for (Asset ass : assets) {
            if ((ass.getName() != null && ass.getName().contains(searchString))
                || (ass.getDescription() != null && ass.getDescription().contains(searchString))
                || (ass.getShortDescription() != null && ass.getShortDescription().contains(searchString))) {
                foundAssets.add(ass);
            }
        }
        return foundAssets;
    }

    /**
     * Implementation for the filtered get methods {@link #getAssets(Collection, String, Visibility, String)} and
     * {@link #getAssetsWithUnboundedMaxVersion(Collection, String, Visibility)}.
     *
     * @param types
     *            The types to look for or <code>null</code> will return all types
     * @param productIds The product IDs to look for or <code>null</code> will return assets for any product ID
     * @param visibility The visibility to look for or <code>null</code> will return all visibility values (or none)
     * @param productVersions The value of the minimum version in the appliesToFilterInfo to look for
     * @param unboundedMaxVersion <code>true</code> if
     * @return
     * @throws IOException
     * @throws RequestFailureException
     */
    protected Collection<Asset> getFilteredAssets(Collection<ResourceType> types, Collection<String> productIds, Visibility visibility, Collection<String> productVersions,
                                                  boolean unboundedMaxVersion) throws IOException, RequestFailureException {
        Map<FilterableAttribute, Collection<String>> filters = new HashMap<FilterableAttribute, Collection<String>>();
        if (types != null && !types.isEmpty()) {
            Collection<String> typeValues = new HashSet<String>();
            for (ResourceType type : types) {
                typeValues.add(type.getValue());
            }
            filters.put(FilterableAttribute.TYPE, typeValues);
        }
        filters.put(FilterableAttribute.PRODUCT_ID, productIds);
        if (visibility != null) {
            filters.put(FilterableAttribute.VISIBILITY, Collections.singleton(visibility.toString()));
        }
        filters.put(FilterableAttribute.PRODUCT_MIN_VERSION, productVersions);

        if (unboundedMaxVersion) {
            filters.put(FilterableAttribute.PRODUCT_HAS_MAX_VERSION, Collections.singleton(Boolean.FALSE.toString()));
        }
        return getFilteredAssets(filters);
    }

    /**
     * Returns <code>true</code> if all the filters are empty.
     *
     * @param filters
     * @return
     */
    protected boolean allFiltersAreEmpty(Map<FilterableAttribute, Collection<String>> filters) {
        for (Map.Entry<FilterableAttribute, Collection<String>> filter : filters.entrySet()) {
            Collection<String> values = filter.getValue();
            if (values != null && !values.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private interface AppliesToFilterGetter {
        public String getValue(final AppliesToFilterInfo atfi);
    }

    protected Collection<String> getValues(final FilterableAttribute attrib, final Asset asset) {
        String ret;
        switch (attrib) {
            case LOWER_CASE_SHORT_NAME:
                ret = asset.getWlpInformation().getLowerCaseShortName();
                return (ret == null ? Collections.<String> emptyList() : Collections.singleton(ret));
            case PRODUCT_HAS_MAX_VERSION:
                return getFromAppliesTo(asset, new AppliesToFilterGetter() {
                    @Override
                    public String getValue(AppliesToFilterInfo atfi) {
                        return atfi.getHasMaxVersion();
                    }
                });
            case PRODUCT_ID:
                return getFromAppliesTo(asset, new AppliesToFilterGetter() {
                    @Override
                    public String getValue(AppliesToFilterInfo atfi) {
                        return atfi.getProductId() != null ? atfi.getProductId() : null;
                    }
                });
            case PRODUCT_MIN_VERSION:
                return getFromAppliesTo(asset, new AppliesToFilterGetter() {
                    @Override
                    public String getValue(AppliesToFilterInfo atfi) {
                        return atfi.getMinVersion() != null ? atfi.getMinVersion().getValue() : null;
                    }
                });
            case SHORT_NAME:
                ret = asset.getWlpInformation().getShortName();
                return (ret == null ? Collections.<String> emptyList() : Collections.singleton(ret));
            case SYMBOLIC_NAME:
                return asset.getWlpInformation().getProvideFeature() == null ? Collections.<String> emptyList() : asset.getWlpInformation().getProvideFeature();
            case TYPE:
                ret = asset.getType() == null ? null : asset.getType().getValue();
                return (ret == null ? Collections.<String> emptyList() : Collections.singleton(ret));
            case VISIBILITY:
                ret = asset.getWlpInformation().getVisibility() == null ? null : asset.getWlpInformation().getVisibility().toString();
                return (ret == null ? Collections.<String> emptyList() : Collections.singleton(ret));
            case VANITY_URL:
                ret = asset.getWlpInformation().getVanityRelativeURL() == null ? null : asset.getWlpInformation().getVanityRelativeURL();
                return (ret == null ? Collections.<String> emptyList() : Collections.singleton(ret));
            default:
                return null;

        }
    }

    /**
     * Utility method to cycle through the applies to filters info and collate the values found
     *
     * @param asset
     * @param getter
     * @return
     */
    private static Collection<String> getFromAppliesTo(final Asset asset, final AppliesToFilterGetter getter) {
        Collection<AppliesToFilterInfo> atfis = asset.getWlpInformation().getAppliesToFilterInfo();
        Collection<String> ret = new ArrayList<String>();
        if (atfis != null) {
            for (AppliesToFilterInfo at : atfis) {
                if (at != null) {
                    String val = getter.getValue(at);
                    if (val != null) {
                        ret.add(val);
                    }
                }
            }
        }
        return ret;
    }
}

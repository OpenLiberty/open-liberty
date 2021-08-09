/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.simplicity.config.wim;

import javax.xml.bind.annotation.XmlElement;

import com.ibm.websphere.simplicity.config.ConfigElement;

/**
 * Configuration for the following nested elements:
 *
 * <ul>
 * <li>ldapRegistry --> ldapCache</li>
 * </ul>
 */
public class LdapCache extends ConfigElement {

    private AttributesCache attributesCache;
    private SearchResultsCache searchResultsCache;

    public LdapCache() {}

    public LdapCache(AttributesCache attributesCache, SearchResultsCache searchResultsCache) {
        this.attributesCache = attributesCache;
        this.searchResultsCache = searchResultsCache;
    }

    /**
     * @return the attributesCache
     */
    public AttributesCache getAttributesCache() {
        return attributesCache;
    }

    /**
     * @return the searchResultsCache
     */
    public SearchResultsCache getSearchResultsCache() {
        return searchResultsCache;
    }

    /**
     * @param attributesCache the attributesCache to set
     */
    @XmlElement(name = "attributesCache")
    public void setAttributesCache(AttributesCache attributesCache) {
        this.attributesCache = attributesCache;
    }

    /**
     * @param searchResultsCache the searchResultsCache to set
     */
    @XmlElement(name = "searchResultsCache")
    public void setSearchResultsCache(SearchResultsCache searchResultsCache) {
        this.searchResultsCache = searchResultsCache;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (attributesCache != null) {
            sb.append("attributesCache=\"").append(attributesCache).append("\" ");;
        }
        if (searchResultsCache != null) {
            sb.append("searchResultsCache=\"").append(searchResultsCache).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}
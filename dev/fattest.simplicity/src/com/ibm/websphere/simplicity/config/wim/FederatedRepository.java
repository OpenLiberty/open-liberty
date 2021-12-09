/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.simplicity.config.wim;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.ibm.websphere.simplicity.config.ConfigElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;

/**
 * Configuration for the following top-level elements:
 *
 * <ul>
 * <li>federatedRepository</li>
 * </ul>
 */
public class FederatedRepository extends ConfigElement {

    private ConfigElementList<ExtendedProperty> extendedProperties;
    private Integer maxSearchResults;
    private Integer pageCacheSize;
    private String pageCacheTimeout;
    private Realm primaryRealm;
    private ConfigElementList<Realm> realms;
    private String searchTimeout;
    private ConfigElementList<SupportedEntityType> supportedEntityTypes;
    private String failedLoginDelayMin;
    private String failedLoginDelayMax;

    /**
     * @return the extendedProperty
     */
    public ConfigElementList<ExtendedProperty> getExtendedProperties() {
        return (extendedProperties == null) ? (extendedProperties = new ConfigElementList<ExtendedProperty>()) : extendedProperties;
    }

    /**
     * @return the maxSearchResults
     */
    public Integer getMaxSearchResults() {
        return maxSearchResults;
    }

    /**
     * @return the pageCacheSize
     */
    public Integer getPageCacheSize() {
        return pageCacheSize;
    }

    /**
     * @return the pageCacheTimeout
     */
    public String getPageCacheTimeout() {
        return pageCacheTimeout;
    }

    /**
     * @return the primaryRealm
     */
    public Realm getPrimaryRealm() {
        return primaryRealm;
    }

    /**
     * @return the realm
     */
    public ConfigElementList<Realm> getRealms() {
        return (realms == null) ? (realms = new ConfigElementList<Realm>()) : realms;
    }

    /**
     * @return the searchTimeout
     */
    public String getSearchTimeout() {
        return searchTimeout;
    }

    /**
     * @return the supportedEntityType
     */
    public ConfigElementList<SupportedEntityType> getSupportedEntityTypes() {
        return (supportedEntityTypes == null) ? (supportedEntityTypes = new ConfigElementList<SupportedEntityType>()) : supportedEntityTypes;
    }

    /**
     * Get the failedLoginDelayMin, it is a string because the metatype type is duration
     *
     * @return failedLoginDelayMin
     */
    public String getFailedLoginDelayMin() {
        return failedLoginDelayMin;
    }

    /**
     * Get the failedLoginDelayMax, it is a string because the metatype type is duration
     *
     * @return failedLoginDelayMax
     */
    public String getFailedLoginDelayMax() {
        return failedLoginDelayMax;
    }

    /**
     * @param extendedProperty the extendedProperty to set
     */
    @XmlElement(name = "extendedProperty")
    public void setExtendedProperties(ConfigElementList<ExtendedProperty> extendedProperties) {
        this.extendedProperties = extendedProperties;
    }

    /**
     * @param maxSearchResults the maxSearchResults to set
     */
    @XmlAttribute(name = "maxSearchResults")
    public void setMaxSearchResults(Integer maxSearchResults) {
        this.maxSearchResults = maxSearchResults;
    }

    /**
     * @param pageCacheSize the pageCacheSize to set
     */
    @XmlAttribute(name = "pageCacheSize")
    public void setPageCacheSize(Integer pageCacheSize) {
        this.pageCacheSize = pageCacheSize;
    }

    /**
     * @param pageCacheTimeout the pageCacheTimeout to set
     */
    @XmlAttribute(name = "pageCacheTimeout")
    public void setPageCacheTimeout(String pageCacheTimeout) {
        this.pageCacheTimeout = pageCacheTimeout;
    }

    /**
     * @param primaryRealm the primaryRealm to set
     */
    @XmlElement(name = "primaryRealm")
    public void setPrimaryRealm(Realm primaryRealm) {
        this.primaryRealm = primaryRealm;
    }

    /**
     * @param realm the realm to set
     */
    @XmlElement(name = "realm")
    public void setRealms(ConfigElementList<Realm> realms) {
        this.realms = realms;
    }

    /**
     * @param searchTimeout the searchTimeout to set
     */
    @XmlAttribute(name = "searchTimeout")
    public void setSearchTimeout(String searchTimeout) {
        this.searchTimeout = searchTimeout;
    }

    /**
     * @param supportedEntityType the supportedEntityType to set
     */
    @XmlElement(name = "supportedEntityType")
    public void setSupportedEntityTypes(ConfigElementList<SupportedEntityType> supportedEntityTypes) {
        this.supportedEntityTypes = supportedEntityTypes;
    }

    /**
     * Set the failedLoginDelayMin, it is a string because the metatype type is duration
     *
     * @param failedLoginDelayMin
     */
    @XmlAttribute(name = "failedLoginDelayMin")
    public void setFailedLoginDelayMin(String failedLoginDelayMin) {
        this.failedLoginDelayMin = failedLoginDelayMin;
    }

    /**
     * Set the failedLoginDelayMax, it is a string because the metatype type is duration
     *
     * @param failedLoginDelayMax
     */
    @XmlAttribute(name = "failedLoginDelayMax")
    public void setFailedLoginDelayMax(String failedLoginDelayMax) {
        this.failedLoginDelayMax = failedLoginDelayMax;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (extendedProperties != null) {
            sb.append("extendedProperties=\"").append(extendedProperties).append("\" ");;
        }
        if (maxSearchResults != null) {
            sb.append("maxSearchResults=\"").append(maxSearchResults).append("\" ");;
        }
        if (pageCacheSize != null) {
            sb.append("pageCacheSize=\"").append(pageCacheSize).append("\" ");;
        }
        if (pageCacheTimeout != null) {
            sb.append("pageCacheTimeout=\"").append(pageCacheTimeout).append("\" ");;
        }
        if (primaryRealm != null) {
            sb.append("primaryRealm=\"").append(primaryRealm).append("\" ");;
        }
        if (realms != null) {
            sb.append("realms=\"").append(realms).append("\" ");;
        }
        if (searchTimeout != null) {
            sb.append("searchTimeout=\"").append(searchTimeout).append("\" ");;
        }
        if (supportedEntityTypes != null) {
            sb.append("supportedEntityTypes=\"").append(supportedEntityTypes).append("\" ");;
        }
        if (failedLoginDelayMin != null) {
            sb.append("failedLoginDelayMin=\"").append(failedLoginDelayMin).append("\"");
        }
        if (failedLoginDelayMax != null) {
            sb.append("failedLoginDelayMax=\"").append(failedLoginDelayMax).append("\"");
        }

        sb.append("}");

        return sb.toString();
    }
}

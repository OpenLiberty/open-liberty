/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.websphere.simplicity.config.cache;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.ibm.websphere.simplicity.config.ConfigElement;
import com.ibm.websphere.simplicity.config.dsprops.Properties;

/**
 * Configuration for the following top-level elements:
 *
 * <ul>
 * <li>cacheManager</li>
 * </ul>
 */
public class CacheManager extends ConfigElement {
    private String cachingProviderRef;
    private String uri;
    private Properties props;

    public CacheManager() {}

    /**
     *
     * @param id
     * @param cachingProviderRef
     * @param uri
     */
    public CacheManager(String id, String cachingProviderRef, String uri) {
        this.cachingProviderRef = cachingProviderRef;
        this.uri = uri;
        setId(id);
    }

    /**
     * @param cachingProviderRef the cachingProviderRef to set
     */
    @XmlAttribute(name = "cachingProviderRef")
    public void setCachingProviderRef(String cachingProviderRef) {
        this.cachingProviderRef = cachingProviderRef;
    }

    /**
     * @return the cachingProviderRef
     */
    public String getCachingProviderRef() {
        return cachingProviderRef;
    }

    /**
     * @param uri the uri to set
     */
    @XmlAttribute(name = "uri")
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param p the Properties to set
     */
    @XmlElement(name = "properties")
    public void setProps(Properties p) {
        props = p;
    }

    /**
     * @return the properties
     */
    public Properties getProps() {
        return props;
    }
}

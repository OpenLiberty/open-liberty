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

import com.ibm.websphere.simplicity.config.ConfigElement;

/**
 * Configuration for the following top-level elements:
 *
 * <ul>
 * <li>cachingProvider</li>
 * </ul>
 */
public class CachingProvider extends ConfigElement {
    private String commonLibraryRef;
    private String jCacheLibraryRef;
    private String providerClass;

    public CachingProvider() {}

    public CachingProvider(String id, String jCacheLibraryRef, String commonLibraryRef, String providerClass) {
        this.commonLibraryRef = commonLibraryRef;
        this.jCacheLibraryRef = jCacheLibraryRef;
        this.providerClass = providerClass;
        setId(id);
    }

    /**
     * @param commonLibraryRef the commonLibraryRef to set
     */
    @XmlAttribute(name = "commonLibraryRef")
    public void setCommonLibraryRef(String commonLibraryRef) {
        this.commonLibraryRef = commonLibraryRef;
    }

    /**
     * @return the commonLibraryRef
     */
    public String getCommonLibraryRef() {
        return commonLibraryRef;
    }

    /**
     * @param jCacheLibraryRef the jCacheLibraryRef to set
     */
    @XmlAttribute(name = "jCacheLibraryRef")
    public void setJCacheLibraryRef(String jCacheLibraryRef) {
        this.jCacheLibraryRef = jCacheLibraryRef;
    }

    /**
     * @return the jCacheLibraryRef
     */
    public String getJCacheLibraryRef() {
        return jCacheLibraryRef;
    }

    /**
     * @param providerClass the providerClass to set
     */
    @XmlAttribute(name = "providerClass")
    public void setProviderClass(String providerClass) {
        this.providerClass = providerClass;
    }

    /**
     * @return the providerClass
     */
    public String getProviderClass() {
        return providerClass;
    }
}

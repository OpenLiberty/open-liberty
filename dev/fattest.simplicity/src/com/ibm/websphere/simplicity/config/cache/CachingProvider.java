/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
    private String libraryRef;
    private String providerClass;

    public CachingProvider() {}

    public CachingProvider(String id, String libraryRef, String providerClass) {
        this.libraryRef = libraryRef;
        this.providerClass = providerClass;
        setId(id);
    }

    /**
     * @param libraryRef the libraryRef to set
     */
    @XmlAttribute(name = "libraryRef")
    public void setLibraryRef(String libraryRef) {
        this.libraryRef = libraryRef;
    }

    /**
     * @return the libraryRef
     */
    public String getLibraryRef() {
        return libraryRef;
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

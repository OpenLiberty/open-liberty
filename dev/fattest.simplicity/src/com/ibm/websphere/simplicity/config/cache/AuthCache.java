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
 * <li>authCache</li>
 * </ul>
 */
public class AuthCache extends ConfigElement {

    private String cacheRef;

    public AuthCache() {}

    public AuthCache(String cacheRef) {
        this.cacheRef = cacheRef;
    }

    /**
     * @param cacheRef the cacheRef to set
     */
    @XmlAttribute(name = "cacheRef")
    public void setCacheRef(String cacheRef) {
        this.cacheRef = cacheRef;
    }

    /**
     * @return the cacheRef
     */
    public String getCacheRef() {
        return cacheRef;
    }
}

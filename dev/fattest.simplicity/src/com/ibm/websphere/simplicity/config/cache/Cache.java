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
 * <li>cache</li>
 * </ul>
 */
public class Cache extends ConfigElement {
    private String name;
    private String cacheManagerRef;

    public Cache() {}

    public Cache(String id, String name, String cacheManagerRef) {
        this.name = name;
        this.cacheManagerRef = cacheManagerRef;
        setId(id);
    }

    /**
     * @param name the name to set
     */
    @XmlAttribute(name = "name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param cacheManagerRef the cacheManagerRef to set
     */
    @XmlAttribute(name = "cacheManagerRef")
    public void setCacheManagerRef(String cacheManagerRef) {
        this.cacheManagerRef = cacheManagerRef;
    }

    /**
     * @return the cacheManagerRef
     */
    public String getCacheManagerRef() {
        return cacheManagerRef;
    }

}

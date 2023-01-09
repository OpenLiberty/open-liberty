/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
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
package com.ibm.websphere.simplicity.config;

import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Represents the <bell> element in server.xml
 */
public class Bell extends ConfigElement {

    private final Class<?> c = Bell.class;

    private String libraryRef;

    private Set<String> service;

    private String spiVisibility;

    @XmlAttribute(required = true)
    public String getLibraryRef() {
        return libraryRef;
    }

    public void setLibraryRef(String libraryRef) {
        this.libraryRef = libraryRef;
    }

    @XmlAttribute
    public Set<String> getService() {
        return service;
    }

    public void setService(Set<String> service) {
        this.service = service;
    }

    @XmlAttribute
    public String getSpiVisibility() {
        return spiVisibility;
    }

    public void setSpiVisibility(String spiVisibility) {
        this.spiVisibility = spiVisibility;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        // attributes
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (getLibraryRef() != null)
            buf.append("libraryRef=").append(getLibraryRef()).append(' ');
        if (getService() != null)
            buf.append("service=").append(getService()).append(' ');
        if (getSpiVisibility() != null)
            buf.append("spiVisibility=").append(getSpiVisibility()).append(' ');
        buf.append('}');
        return buf.toString();
    }
}

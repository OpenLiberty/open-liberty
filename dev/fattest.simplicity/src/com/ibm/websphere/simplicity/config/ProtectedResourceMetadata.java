/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Configuration for the following elements:
 *
 * <ul>
 * <li>openidConnectClient->protectedResourceMetadata</li>
 * </ul>
 */
public class ProtectedResourceMetadata extends ConfigElement {
    private String advertisedScopes;
    private String jwtBuilderRef;

    /**
     * @return the advertisedScopes
     */
    public String getAdvertisedScopes() {
        return advertisedScopes;
    }

    /**
     * @param names the advertisedScopes to set
     */
    @XmlAttribute(name = "advertisedScopes")
    public void setAdvertisedScopes(String advertisedScopes) {
        this.advertisedScopes = advertisedScopes;
    }

    /**
     * @return the jwtBuilderRef
     */
    public String getJwtBuilderRef() {
        return jwtBuilderRef;
    }

    /**
     * @param name the jwtBuilderRef to set
     */
    @XmlAttribute(name = "jwtBuilderRef")
    public void setJwtBuilderRef(String jwtBuilderRef) {
        this.jwtBuilderRef = jwtBuilderRef;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (advertisedScopes != null) {
            sb.append("advertisedScopes=\"").append(advertisedScopes).append("\" ");
        }
        if (jwtBuilderRef != null) {
            sb.append("jwtBuilderRef=\"").append(jwtBuilderRef).append("\" ");
        }

        sb.append("}");

        return sb.toString();
    }
}

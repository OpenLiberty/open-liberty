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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Configuration for the following elements:
 *
 * <ul>
 * <li>oidcLogin->jwt</li>
 * <li>oauth2Login->jwt</li>
 * </ul>
 */
public class Jwt extends ConfigElement {
    private String builder;
    private String claims;

    /**
     * @return the builder
     */
    public String getBuilder() {
        return builder;
    }

    /**
     * @param builder the builder to set
     */
    @XmlAttribute(name = "builder")
    public void setBuilder(String builder) {
        this.builder = builder;
    }

    /**
     * @return the claims
     */
    public String getClaims() {
        return claims;
    }

    /**
     * @param claims the claims to set
     */
    @XmlElement(name = "claims")
    public void setClaims(String claims) {
        this.claims = claims;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (builder != null) {
            sb.append("builder=\"").append(builder).append("\" ");
        }
        if (claims != null) {
            sb.append("claims=\"").append(claims).append("\" ");
        }

        sb.append("}");

        return sb.toString();
    }
}

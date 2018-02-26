/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.config.ConfigElement;

/**
 * Configuration for the following nested elements:
 *
 * <ul>
 * <li>ldapRegistry --> groupConfiguration --> membershipAttribute</li>
 * </ul>
 */
public class MembershipAttribute extends ConfigElement {
    private String name;
    private String scope;

    public MembershipAttribute() {}

    public MembershipAttribute(String name, String scope) {
        this.name = name;
        this.scope = scope;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * @param name the name to set
     */
    @XmlAttribute(name = "name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param scope the scope to set
     */
    @XmlAttribute(name = "scope")
    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (name != null) {
            sb.append("name=\"").append(name).append("\" ");;
        }
        if (scope != null) {
            sb.append("scope=\"").append(scope).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}

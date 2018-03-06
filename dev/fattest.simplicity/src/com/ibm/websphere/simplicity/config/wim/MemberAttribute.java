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
 * <li>ldapRegistry --> groupConfiguration --> memberAttribute</li>
 * </ul>
 */
public class MemberAttribute extends ConfigElement {
    private String dummyMember;
    private String name;
    private String objectClass;
    private String scope;

    public MemberAttribute() {}

    public MemberAttribute(String dummyMember,
                           String name,
                           String objectClass,
                           String scope) {
        this.dummyMember = dummyMember;
        this.name = name;
        this.objectClass = objectClass;
        this.scope = scope;
    }

    /**
     * @return the dummyMember
     */
    public String getDummyMember() {
        return dummyMember;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the objectClass
     */
    public String getObjectClass() {
        return objectClass;
    }

    /**
     * @return the scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * @param dummyMember the dummyMember to set
     */
    @XmlAttribute(name = "dummyMember")
    public void setDummyMember(String dummyMember) {
        this.dummyMember = dummyMember;
    }

    /**
     * @param name the name to set
     */
    @XmlAttribute(name = "name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param objectClass the objectClass to set
     */
    @XmlAttribute(name = "objectClass")
    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
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
        if (objectClass != null) {
            sb.append("objectClass=\"").append(objectClass).append("\" ");;
        }
        if (scope != null) {
            sb.append("scope=\"").append(scope).append("\" ");;
        }
        if (dummyMember != null) {
            sb.append("dummyMember=\"").append(dummyMember).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}

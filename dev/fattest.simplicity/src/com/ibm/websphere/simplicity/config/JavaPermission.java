/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 */
public class JavaPermission extends ConfigElement {

    private String actions;
    private String className;
    private String codeBase;
    private String name;
    private String principalName;
    private String principalType;
    private Boolean restriction;
    private String signedBy;

    /**
     * @return the actions
     */
    public String getActions() {
        return actions;
    }

    /**
     * @return the className
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return the codeBase
     */
    public String getCodeBase() {
        return codeBase;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the principalName
     */
    public String getPrincipalName() {
        return principalName;
    }

    /**
     * @return the principalType
     */
    public String getPrincipalType() {
        return principalType;
    }

    /**
     * @return the restriction
     */
    public Boolean getRestriction() {
        return restriction;
    }

    /**
     * @return the signedBy
     */
    public String getSignedBy() {
        return signedBy;
    }

    /**
     * @param actions the actions to set
     */
    @XmlAttribute(name = "actions")
    public void setActions(String actions) {
        this.actions = actions;
    }

    /**
     * @param className the className to set
     */
    @XmlAttribute(name = "className")
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @param codeBase the codeBase to set
     */
    @XmlAttribute(name = "codeBase")
    public void setCodeBase(String codeBase) {
        this.codeBase = codeBase;
    }

    /**
     * @param name the name to set
     */
    @XmlAttribute(name = "name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param principalName the principalName to set
     */
    @XmlAttribute(name = "principalName")
    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    /**
     * @param principalType the principalType to set
     */
    @XmlAttribute(name = "principalType")
    public void setPrincipalType(String principalType) {
        this.principalType = principalType;
    }

    /**
     * @param restriction the restriction to set
     */
    @XmlAttribute(name = "restriction")
    public void setRestriction(Boolean restriction) {
        this.restriction = restriction;
    }

    /**
     * @param restriction the signedBy to set
     */
    @XmlAttribute(name = "signedBy")
    public void setSignedBy(String signedBy) {
        this.signedBy = signedBy;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("JavaPermission{");
        if (actions != null)
            buf.append("actions=\"" + actions + "\" ");
        if (className != null)
            buf.append("className=\"" + className + "\" ");
        if (codeBase != null)
            buf.append("codeBase=\"" + codeBase + "\" ");
        if (getId() != null)
            buf.append("id=\"" + getId() + "\" ");
        if (name != null)
            buf.append("name=\"" + name + "\" ");
        if (principalName != null)
            buf.append("principalName=\"" + principalName + "\" ");
        if (restriction != null)
            buf.append("restriction=\"" + restriction + "\" ");
        if (signedBy != null)
            buf.append("signedBy=\"" + signedBy + "\" ");

        buf.append("}");
        return buf.toString();
    }
}

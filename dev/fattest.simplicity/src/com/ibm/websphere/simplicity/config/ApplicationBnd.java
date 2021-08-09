/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
import javax.xml.bind.annotation.XmlElement;

/**
 * Binds general deployment information included in the application to specific resources. See /com.ibm.ws.javaee.dd/resources/OSGI-INF/metatype/metatype.xml
 * 
 * @author Tim Burns
 * 
 */
public class ApplicationBnd extends ConfigElement {

    private String version;
    @XmlElement(name = "security-role")
    private ConfigElementList<SecurityRole> securityRoles;

    /**
     * @return the version of the application bindings specification.
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * @param version the version of the application bindings specification.
     */
    @XmlAttribute
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Retrieves the users for this role
     * 
     * @return the users for this role
     */
    public ConfigElementList<SecurityRole> getSecurityRoles() {
        if (this.securityRoles == null) {
            this.securityRoles = new ConfigElementList<SecurityRole>();
        }
        return this.securityRoles;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("ApplicationBnd{");
        if (version != null)
            buf.append("version=\"" + version + "\" ");
        if (this.securityRoles != null)
            for (SecurityRole securityRole : this.securityRoles)
                buf.append(securityRole.toString() + ",");
        buf.append("}");

        return buf.toString();
    }

    @Override
    public ApplicationBnd clone() throws CloneNotSupportedException {
        ApplicationBnd clone = (ApplicationBnd) super.clone();
        if (this.securityRoles != null) {
            clone.securityRoles = new ConfigElementList<SecurityRole>();
            for (SecurityRole securityRole : this.securityRoles)
                clone.securityRoles.add((SecurityRole) securityRole.clone());
        }
        return clone;
    }

}

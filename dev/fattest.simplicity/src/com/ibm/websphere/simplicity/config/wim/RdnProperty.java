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

import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.ibm.websphere.simplicity.config.ConfigElement;

/**
 * Configuration for the following nested elements:
 *
 * <ul>
 * <li>ldapRegistry --> ldapEntityType -- rdnProperty</li>
 * </ul>
 */
public class RdnProperty extends ConfigElement {

    private String name;
    private Set<String> objectClasses;

    public RdnProperty() {}

    public RdnProperty(String name, String[] objectClasses) {
        this.name = name;

        if (objectClasses != null && objectClasses.length > 0) {
            this.objectClasses = new TreeSet<String>();

            for (String objectClass : objectClasses) {
                this.objectClasses.add(objectClass);
            }
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the objectClasses
     */
    public Set<String> getObjectClasses() {
        return (objectClasses == null) ? (objectClasses = new TreeSet<String>()) : objectClasses;
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
    @XmlElement(name = "objectClass")
    public void setObjectClasses(Set<String> objectClasses) {
        this.objectClasses = objectClasses;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (name != null) {
            sb.append("name=\"").append(name).append("\" ");;
        }
        if (objectClasses != null) {
            sb.append("objectClasses=\"").append(objectClasses).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
 * <li>ldapRegistry --> ldapEntityType</li>
 * </ul>
 */
public class LdapEntityType extends ConfigElement {

    private String name;
    private Set<String> objectClasses;
    private Set<String> searchBases;
    private String searchFilter;

    public LdapEntityType() {}

    public LdapEntityType(String name, String searchFilter, String[] objectClasses, String[] searchBases) {
        this.name = name;
        this.searchFilter = searchFilter;

        if (objectClasses != null && objectClasses.length > 0) {
            this.objectClasses = new TreeSet<String>();

            for (String objectClass : objectClasses) {
                this.objectClasses.add(objectClass);
            }
        }

        if (searchBases != null && searchBases.length > 0) {
            this.searchBases = new TreeSet<String>();

            for (String searchBase : searchBases) {
                this.searchBases.add(searchBase);
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
     * @return the searchBases
     */
    public Set<String> getSearchBases() {
        return (searchBases == null) ? (searchBases = new TreeSet<String>()) : searchBases;
    }

    /**
     * @return the searchFilter
     */
    public String getSearchFilter() {
        return searchFilter;
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

    /**
     * @param searchBase the searchBase to set
     */
    @XmlElement(name = "searchBase")
    public void setSearchBases(Set<String> searchBases) {
        this.searchBases = searchBases;
    }

    /**
     * @param searchFilter the searchFilter to set
     */
    @XmlAttribute(name = "searchFilter")
    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
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
        if (searchBases != null) {
            sb.append("searchBases=\"").append(searchBases).append("\" ");;
        }
        if (searchFilter != null) {
            sb.append("searchFilter=\"").append(searchFilter).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}
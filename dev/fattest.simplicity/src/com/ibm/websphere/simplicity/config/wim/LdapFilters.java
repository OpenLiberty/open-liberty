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

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.simplicity.config.ConfigElement;

/**
 * Configuration for the following nested elements:
 *
 * <ul>
 * <li>ldapRegistry --> activedFilters</li>
 * <li>ldapRegistry --> customFilters</li>
 * <li>ldapRegistry --> domino50Filters</li>
 * <li>ldapRegistry --> edirectoryFilters</li>
 * <li>ldapRegistry --> idsFilters</li>
 * <li>ldapRegistry --> iplanetFilters</li>
 * <li>ldapRegistry --> netscapeFilters</li>
 * <li>ldapRegistry --> securewayFilters</li>
 * </ul>
 */
public class LdapFilters extends ConfigElement {

    private String groupFilter;
    private String groupIdMap;
    private String groupMemberIdMap;
    private String userFilter;
    private String userIdMap;

    public LdapFilters() {}

    public LdapFilters(String userFilter, String groupFilter, String userIdMap, String groupIdMap, String groupMemberIdMap) {
        this.userFilter = userFilter;
        this.groupFilter = groupFilter;
        this.userIdMap = userIdMap;
        this.groupIdMap = groupIdMap;
        this.groupMemberIdMap = groupMemberIdMap;
    }

    /**
     * @return the groupFilter
     */
    public String getGroupFilter() {
        return groupFilter;
    }

    /**
     * @return the groupIdMap
     */
    public String getGroupIdMap() {
        return groupIdMap;
    }

    /**
     * @return the groupMemberIdMap
     */
    public String getGroupMemberIdMap() {
        return groupMemberIdMap;
    }

    /**
     * @return the userFilter
     */
    public String getUserFilter() {
        return userFilter;
    }

    /**
     * @return the userIdMap
     */
    public String getUserIdMap() {
        return userIdMap;
    }

    /**
     * @param groupFilter the groupFilter to set
     */
    @XmlAttribute(name = "groupFilter")
    public void setGroupFilter(String groupFilter) {
        this.groupFilter = groupFilter;
    }

    /**
     * @param groupIdMap the groupIdMap to set
     */
    @XmlAttribute(name = "groupIdMap")
    public void setGroupIdMap(String groupIdMap) {
        this.groupIdMap = groupIdMap;
    }

    /**
     * @param groupMemberIdMap the groupMemberIdMap to set
     */
    @XmlAttribute(name = "groupMemberIdMap")
    public void setGroupMemberIdMap(String groupMemberIdMap) {
        this.groupMemberIdMap = groupMemberIdMap;
    }

    /**
     * @param userFilter the userFilter to set
     */
    @XmlAttribute(name = "userFilter")
    public void setUserFilter(String userFilter) {
        this.userFilter = userFilter;
    }

    /**
     * @param userIdMap the userIdMap to set
     */
    @XmlAttribute(name = "userIdMap")
    public void setUserIdMap(String userIdMap) {
        this.userIdMap = userIdMap;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (groupFilter != null) {
            sb.append("groupFilter").append(groupFilter).append("\" ");;
        }
        if (groupIdMap != null) {
            sb.append("groupIdMap=\"").append(groupIdMap).append("\" ");;
        }
        if (groupMemberIdMap != null) {
            sb.append("groupMemberIdMap=\"").append(groupMemberIdMap).append("\" ");;
        }
        if (userFilter != null) {
            sb.append("userFilter=\"").append(userFilter).append("\" ");;
        }
        if (userIdMap != null) {
            sb.append("userIdMap=\"").append(userIdMap).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}
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
import javax.xml.bind.annotation.XmlElement;

import com.ibm.websphere.simplicity.config.ConfigElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;

/**
 * Configuration for the following nested elements:
 *
 * <ul>
 * <li>federatedRepository --> primaryRealm --> participatingBaseEntry</li>
 * <li>federatedRepository --> realm --> participatingBaseEntry</li>
 * </ul>
 */
public class Realm extends ConfigElement {

    private Boolean allowOpIfRepoDown;
    private DefaultParents defaultParents;
    private String delimiter;
    private RealmPropertyMapping groupDisplayNameMapping;
    private RealmPropertyMapping groupSecurityNameMapping;
    private String name;
    private ConfigElementList<BaseEntry> participatingBaseEntries;
    private RealmPropertyMapping uniqueGroupIdMapping;
    private RealmPropertyMapping uniqueUserIdMapping;
    private RealmPropertyMapping userDisplayNameMapping;
    private RealmPropertyMapping userSecurityNameMapping;

    public Realm() {}

    public Realm(String name, ConfigElementList<BaseEntry> participatingBaseEntries) {
        this.name = name;
        this.participatingBaseEntries = participatingBaseEntries;
    }

    /**
     * @return the allowOpIfRepoDown
     */
    public Boolean getAllowOpIfRepoDown() {
        return allowOpIfRepoDown;
    }

    /**
     * @return the defaultParents
     */
    public DefaultParents getDefaultParents() {
        return defaultParents;
    }

    /**
     * @return the delimiter
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * @return the groupDisplayNameMapping
     */
    public RealmPropertyMapping getGroupDisplayNameMapping() {
        return groupDisplayNameMapping;
    }

    /**
     * @return the groupSecurityNameMapping
     */
    public RealmPropertyMapping getGroupSecurityNameMapping() {
        return groupSecurityNameMapping;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the participatingBaseEntries
     */
    public ConfigElementList<BaseEntry> getParticipatingBaseEntries() {
        return (participatingBaseEntries == null) ? (participatingBaseEntries = new ConfigElementList<BaseEntry>()) : participatingBaseEntries;
    }

    /**
     * @return the uniqueGroupIdMapping
     */
    public RealmPropertyMapping getUniqueGroupIdMapping() {
        return uniqueGroupIdMapping;
    }

    /**
     * @return the uniqueUserIdMapping
     */
    public RealmPropertyMapping getUniqueUserIdMapping() {
        return uniqueUserIdMapping;
    }

    /**
     * @return the userDisplayNameMapping
     */
    public RealmPropertyMapping getUserDisplayNameMapping() {
        return userDisplayNameMapping;
    }

    /**
     * @return the userSecurityNameMapping
     */
    public RealmPropertyMapping getUserSecurityNameMapping() {
        return userSecurityNameMapping;
    }

    /**
     * @param allowOpIfRepoDown the allowOpIfRepoDown to set
     */
    @XmlAttribute(name = "allowOpIfRepoDown")
    public void setAllowOpIfRepoDown(Boolean allowOpIfRepoDown) {
        this.allowOpIfRepoDown = allowOpIfRepoDown;
    }

    /**
     * @param defaultParents the defaultParents to set
     */
    @XmlElement(name = "defaultParents")
    public void setDefaultParents(DefaultParents defaultParents) {
        this.defaultParents = defaultParents;
    }

    /**
     * @param delimiter the delimiter to set
     */
    @XmlAttribute(name = "delimiter")
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * @param groupDisplayNameMapping the groupDisplayNameMapping to set
     */
    @XmlElement(name = "groupDisplayNameMapping")
    public void setGroupDisplayNameMapping(RealmPropertyMapping groupDisplayNameMapping) {
        this.groupDisplayNameMapping = groupDisplayNameMapping;
    }

    /**
     * @param groupSecurityNameMapping the groupSecurityNameMapping to set
     */
    @XmlElement(name = "groupSecurityNameMapping")
    public void setGroupSecurityNameMapping(RealmPropertyMapping groupSecurityNameMapping) {
        this.groupSecurityNameMapping = groupSecurityNameMapping;
    }

    /**
     * @param name the name to set
     */
    @XmlAttribute(name = "name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param participatingBaseEntry the participatingBaseEntries to set
     */
    @XmlElement(name = "participatingBaseEntry")
    public void setParticipatingBaseEntries(ConfigElementList<BaseEntry> participatingBaseEntries) {
        this.participatingBaseEntries = participatingBaseEntries;
    }

    /**
     * @param uniqueGroupIdMapping the uniqueGroupIdMapping to set
     */
    @XmlElement(name = "uniqueGroupIdMapping")
    public void setUniqueGroupIdMapping(RealmPropertyMapping uniqueGroupIdMapping) {
        this.uniqueGroupIdMapping = uniqueGroupIdMapping;
    }

    /**
     * @param uniqueUserIdMapping the uniqueUserIdMapping to set
     */
    @XmlElement(name = "uniqueUserIdMapping")
    public void setUniqueUserIdMapping(RealmPropertyMapping uniqueUserIdMapping) {
        this.uniqueUserIdMapping = uniqueUserIdMapping;
    }

    /**
     * @param userDisplayNameMapping the userDisplayNameMapping to set
     */
    @XmlElement(name = "userDisplayNameMapping")
    public void setUserDisplayNameMapping(RealmPropertyMapping userDisplayNameMapping) {
        this.userDisplayNameMapping = userDisplayNameMapping;
    }

    /**
     * @param userSecurityNameMapping the userSecurityNameMapping to set
     */
    @XmlElement(name = "userSecurityNameMapping")
    public void setUserSecurityNameMapping(RealmPropertyMapping userSecurityNameMapping) {
        this.userSecurityNameMapping = userSecurityNameMapping;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (allowOpIfRepoDown != null) {
            sb.append("allowOpIfRepoDown=\"").append(allowOpIfRepoDown).append("\" ");;
        }
        if (defaultParents != null) {
            sb.append("defaultParents=\"").append(defaultParents).append("\" ");;
        }
        if (delimiter != null) {
            sb.append("delimiter=\"").append(delimiter).append("\" ");;
        }
        if (groupDisplayNameMapping != null) {
            sb.append("groupDisplayNameMapping=\"").append(groupDisplayNameMapping).append("\" ");;
        }
        if (groupSecurityNameMapping != null) {
            sb.append("groupSecurityNameMapping=\"").append(groupSecurityNameMapping).append("\" ");;
        }
        if (name != null) {
            sb.append("name=\"").append(name).append("\" ");;
        }
        if (participatingBaseEntries != null) {
            sb.append("participatingBaseEntry=\"").append(participatingBaseEntries).append("\" ");;
        }
        if (uniqueGroupIdMapping != null) {
            sb.append("uniqueGroupIdMapping=\"").append(uniqueGroupIdMapping).append("\" ");;
        }
        if (uniqueUserIdMapping != null) {
            sb.append("uniqueUserIdMapping=\"").append(uniqueUserIdMapping).append("\" ");;
        }
        if (userDisplayNameMapping != null) {
            sb.append("userDisplayNameMapping=\"").append(userDisplayNameMapping).append("\" ");;
        }
        if (userSecurityNameMapping != null) {
            sb.append("userSecurityNameMapping=\"").append(userSecurityNameMapping).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}
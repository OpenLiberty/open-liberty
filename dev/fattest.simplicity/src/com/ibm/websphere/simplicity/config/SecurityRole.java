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
import javax.xml.bind.annotation.XmlType;

/**
 * A role that is mapped to users and groups in a domain user registry. See /com.ibm.ws.javaee.dd/resources/OSGI-INF/metatype/metatype.xml
 * 
 */
public class SecurityRole extends ConfigElement {

    public abstract static class SecurityRoleElement extends ConfigElement {

        private String name;
        private String accessId;

        public String getName() {
            return this.name;
        }

        @XmlAttribute
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return A access ID in the general form id:realmName/uniqueId. A value will be generated if one is not specified.
         */
        public String getAccessId() {
            return accessId;
        }

        /**
         * @param accessId A access ID in the general form id:realmName/uniqueId. A value will be generated if one is not specified.
         */
        @XmlAttribute(name = "access-id")
        public void setAccessId(String accessId) {
            this.accessId = accessId;
        }

        @Override
        public String toString() {
            StringBuffer buf = new StringBuffer(this.getClass().getSimpleName());
            buf.append("{");
            if (name != null)
                buf.append("name=\"" + name + "\" ");
            if (accessId != null)
                buf.append("accessId=\"" + accessId + "\" ");
            buf.append("}");
            return buf.toString();
        }

    }

    /**
     * Represents group configuration for a security-role. Nested inside parent to distinguish from group elements of other types. See
     * /com.ibm.ws.javaee.dd/resources/OSGI-INF/metatype/metatype.xml
     * 
     */
    @XmlType(name = "SecurityRoleGroup")
    public static class Group extends SecurityRoleElement {

        @Override
        public Group clone() throws CloneNotSupportedException {
            return (Group) super.clone();
        }

    }

    /**
     * Represents user configuration for a security-role. Nested inside parent to distinguish from user elements of other types. See
     * /com.ibm.ws.javaee.dd/resources/OSGI-INF/metatype/metatype.xml
     * 
     */
    @XmlType(name = "SecurityRoleUser")
    public static class User extends SecurityRoleElement {

        @Override
        public User clone() throws CloneNotSupportedException {
            return (User) super.clone();
        }

    }

    @XmlElement(name = "user")
    private ConfigElementList<User> users;
    @XmlElement(name = "group")
    private ConfigElementList<Group> groups;
    @XmlElement(name = "special-subject")
    private ConfigElementList<SpecialSubject> specialSubjects;
    private String name;

    /**
     * @return the name of this role.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name the name this role.
     */
    @XmlAttribute
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the users for this role
     * 
     * @return the users for this role
     */
    public ConfigElementList<User> getUsers() {
        if (this.users == null) {
            this.users = new ConfigElementList<User>();
        }
        return this.users;
    }

    /**
     * Retrieves the groups for this role
     * 
     * @return the groups for this role
     */
    public ConfigElementList<Group> getGroups() {
        if (this.groups == null) {
            this.groups = new ConfigElementList<Group>();
        }
        return this.groups;
    }

    /**
     * Retrieves the special subjects for this role
     * 
     * @return the users for this role
     */
    public ConfigElementList<SpecialSubject> getSpecialSubjects() {
        if (this.specialSubjects == null) {
            this.specialSubjects = new ConfigElementList<SpecialSubject>();
        }
        return this.specialSubjects;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("SecurityRole{");
        if (name != null) {
            buf.append("name=\"" + name + "\" ");
        }
        if (this.users != null) {
            for (User user : this.users) {
                buf.append(user.toString() + ",");
            }
        }
        if (this.groups != null) {
            for (Group group : this.groups) {
                buf.append(group.toString() + ",");
            }
        }
        if (this.specialSubjects != null) {
            for (SpecialSubject specialSubject : this.specialSubjects) {
                buf.append(specialSubject.toString() + ",");
            }
        }
        buf.append("}");
        return buf.toString();
    }

    @Override
    public SecurityRole clone() throws CloneNotSupportedException {
        SecurityRole clone = (SecurityRole) super.clone();
        if (this.users != null) {
            clone.users = new ConfigElementList<User>();
            for (User user : this.users) {
                clone.users.add(user.clone());
            }
        }
        if (this.groups != null) {
            clone.groups = new ConfigElementList<Group>();
            for (Group group : this.groups) {
                clone.groups.add(group.clone());
            }
        }
        if (this.specialSubjects != null) {
            clone.specialSubjects = new ConfigElementList<SpecialSubject>();
            for (SpecialSubject specialSubject : this.specialSubjects) {
                clone.specialSubjects.add(specialSubject.clone());
            }
        }
        return clone;
    }

}

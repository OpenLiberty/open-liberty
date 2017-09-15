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
 * A basic security registry. See /com.ibm.ws.security.registry.basic/resources/OSGI-INF/metatype/metatype.xml
 * 
 */
public class BasicRegistry extends ConfigElement {

    /**
     * Represents group configuration for a basic registry. Nested inside parent to distinguish from group elements of other types. See
     * /com.ibm.ws.security.registry.basic/resources/OSGI-INF/metatype/metatype.xml
     * 
     */
    @XmlType(name = "BasicRegistryGroup")
    public static class Group extends ConfigElement {

        /**
         * Represents member configuration for a basic registry. Nested inside parent to distinguish from member elements of other types. See
         * /com.ibm.ws.security.registry.basic/resources/OSGI-INF/metatype/metatype.xml
         * 
         */
        @XmlType(name = "BasicRegistryGroupMember")
        public static class Member extends ConfigElement {

            private String name;

            public String getName() {
                return this.name;
            }

            @XmlAttribute
            public void setName(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                StringBuffer buf = new StringBuffer(this.getClass().getSimpleName());
                buf.append("{");
                if (name != null)
                    buf.append("name=\"" + name + "\" ");
                buf.append("}");
                return buf.toString();
            }

            @Override
            public Member clone() throws CloneNotSupportedException {
                return (Member) super.clone();
            }

        }

        private String name;
        @XmlElement(name = "member")
        private ConfigElementList<Member> members;

        public String getName() {
            return this.name;
        }

        @XmlAttribute
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Retrieves the members in this group
         * 
         * @return the members in this group
         */
        public ConfigElementList<Member> getMembers() {
            if (this.members == null) {
                this.members = new ConfigElementList<Member>();
            }
            return this.members;
        }

        @Override
        public String toString() {
            StringBuffer buf = new StringBuffer(this.getClass().getSimpleName());
            buf.append("{");
            if (name != null)
                buf.append("name=\"" + name + "\" ");
            if (this.members != null) {
                for (Member member : this.members) {
                    buf.append(member.toString() + ",");
                }
            }
            buf.append("}");
            return buf.toString();
        }

        @Override
        public Group clone() throws CloneNotSupportedException {
            Group clone = (Group) super.clone();
            if (this.members != null) {
                clone.members = new ConfigElementList<Member>();
                for (Member member : this.members) {
                    clone.members.add(member.clone());
                }
            }
            return clone;
        }

    }

    /**
     * Represents user configuration for a basic registry. Nested inside parent to distinguish from user elements of other types. See
     * /com.ibm.ws.security.registry.basic/resources/OSGI-INF/metatype/metatype.xml
     * 
     */
    @XmlType(name = "BasicRegistryUser")
    public static class User extends ConfigElement {

        private String name;
        private String password;

        public String getName() {
            return this.name;
        }

        @XmlAttribute
        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return this.password;
        }

        @XmlAttribute
        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public String toString() {
            StringBuffer buf = new StringBuffer(this.getClass().getSimpleName());
            buf.append("{");
            if (name != null)
                buf.append("name=\"" + name + "\" ");
            if (password != null)
                buf.append("password=\"" + password + "\" ");
            buf.append("}");
            return buf.toString();
        }

        @Override
        public User clone() throws CloneNotSupportedException {
            return (User) super.clone();
        }

    }

    @XmlElement(name = "user")
    private ConfigElementList<User> users;
    @XmlElement(name = "group")
    private ConfigElementList<Group> groups;
    private String realm;

    public String getRealm() {
        return this.realm;
    }

    @XmlAttribute
    public void setRealm(String realm) {
        this.realm = realm;
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

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(this.getClass().getSimpleName());
        buf.append("{");
        if (this.realm != null) {
            buf.append("realm=\"" + this.realm + "\" ");
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
        buf.append("}");
        return buf.toString();
    }

    @Override
    public BasicRegistry clone() throws CloneNotSupportedException {
        BasicRegistry clone = (BasicRegistry) super.clone();
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
        return clone;
    }

}

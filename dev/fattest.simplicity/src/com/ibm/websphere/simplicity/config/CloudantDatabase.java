/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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

import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;

/**
 * Represents the <cloudantDatabase> element in server.xml
 */
public class CloudantDatabase extends ConfigElement implements ModifiableConfigElement {
    // attributes
    private String cloudantRef;
    private String create;
    private String databaseName;
    private String jndiName;
    private String fatModify;

    // nested elements
    @XmlElement(name = "cloudant")
    private ConfigElementList<Cloudant> cloudants;

    // getters for attributes
    public String getCloudantRef() {
        return cloudantRef;
    }

    public String getCreate() {
        return create;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getJndiName() {
        return jndiName;
    }

    public String getFatModify() {
        return fatModify;
    }

    // getters for nested elements
    public ConfigElementList<Cloudant> getCloudants() {
        return cloudants == null ? (cloudants = new ConfigElementList<Cloudant>()) : cloudants;
    }

    // setters for attributes
    @XmlAttribute
    public void setCloudantRef(String value) {
        cloudantRef = value;
    }

    @XmlAttribute
    public void setCreate(String value) {
        create = value;
    }

    @XmlAttribute
    public void setDatabaseName(String value) {
        databaseName = value;
    }

    @XmlAttribute
    public void setJndiName(String value) {
        jndiName = value;
    }

    @XmlAttribute(name = "fat.modify")
    public void setFatModify(String fatModify) {
        this.fatModify = fatModify;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        // attributes
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (cloudantRef != null)
            buf.append("cloudantRef=").append(cloudantRef).append(' ');
        if (create != null)
            buf.append("create=").append(create).append(' ');
        if (databaseName != null)
            buf.append("databaseName=").append(databaseName).append(' ');
        if (jndiName != null)
            buf.append("jndiName=").append(jndiName).append(' ');
        // nested elements
        if (cloudants != null)
            buf.append(cloudants).append(' ');
        buf.append('}');
        return buf.toString();
    }

    @Override
    public void modify(ServerConfiguration config) throws Exception {
        if (fatModify == null || !fatModify.toLowerCase().equals("true"))
            return;

        Bootstrap b = Bootstrap.getInstance();
        setDatabaseName(b.getValue(BootstrapProperty.DB_NAME.getPropertyName()));
    }
}

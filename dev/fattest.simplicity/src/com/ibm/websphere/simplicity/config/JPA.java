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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

public class JPA extends ConfigElement {
    private String defaultJtaDataSourceJndiName;
    private String defaultNonJtaDataSourceJndiName;
    private String defaultPersistenceProvider;
    private String entityManagerPoolCapacity;

    public String getDefaultJtaDataSourceJndiName() {
        return defaultJtaDataSourceJndiName;
    }

    public String getDefaultNonJtaDataSourceJndiName() {
        return defaultNonJtaDataSourceJndiName;
    }

    public String getDefaultPersistenceProvider() {
        return defaultPersistenceProvider;
    }

    public String getEntityManagerPoolCapacity() {
        return entityManagerPoolCapacity;
    }

    @XmlAttribute
    public void setDefaultJtaDataSourceJndiName(String value) {
        defaultJtaDataSourceJndiName = value;
    }

    @XmlAttribute
    public void setDefaultNonJtaDataSourceJndiName(String value) {
        defaultNonJtaDataSourceJndiName = value;
    }

    @XmlAttribute
    public void setDefaultPersistenceProvider(String value) {
        defaultPersistenceProvider = value;
    }

    @XmlAttribute
    public void setEntityManagerPoolCapacity(String value) {
        entityManagerPoolCapacity = value;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("jpa{");
        buf.append("defaultJtaDataSourceJndiName=\"" + defaultJtaDataSourceJndiName + "\"");
        if (defaultNonJtaDataSourceJndiName != null)
            buf.append("defaultNonJtaDataSourceJndiName=\"" + defaultNonJtaDataSourceJndiName + "\" ");
        if (defaultPersistenceProvider != null)
            buf.append("defaultPersistenceProvider=\"" + defaultPersistenceProvider + "\" ");
        if (entityManagerPoolCapacity != null)
            buf.append("entityManagerPoolCapacity=\"" + entityManagerPoolCapacity + "\" ");
        buf.append("}");
        return buf.toString();
    }
}

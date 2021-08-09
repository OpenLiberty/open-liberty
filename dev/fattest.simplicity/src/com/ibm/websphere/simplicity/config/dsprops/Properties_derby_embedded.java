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
package com.ibm.websphere.simplicity.config.dsprops;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.simplicity.config.DataSourceProperties;

/**
 * Lists data source properties specific to this driver.
 */
public class Properties_derby_embedded extends DataSourceProperties {
    private String connectionAttributes;
    private String createDatabase;
    private String shutdownDatabase;

    @Override
    public String getElementName() {
        return DERBY_EMBEDDED;
    }

    @XmlAttribute(name = "connectionAttributes")
    public void setConnectionAttributes(String connectionAttributes) {
        this.connectionAttributes = connectionAttributes;
    }

    public String getConnectionAttributes() {
        return this.connectionAttributes;
    }

    @XmlAttribute(name = "createDatabase")
    public void setCreateDatabase(String createDatabase) {
        this.createDatabase = createDatabase;
    }

    public String getCreateDatabase() {
        return this.createDatabase;
    }

    @XmlAttribute(name = "shutdownDatabase")
    public void setShutdownDatabase(String shutdownDatabase) {
        this.shutdownDatabase = shutdownDatabase;
    }

    public String getShutdownDatabase() {
        return this.shutdownDatabase;
    }

    /**
     * Returns a String listing the properties and their values used on this
     * data source.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("{");
        if (connectionAttributes != null)
            buf.append("connectionAttributes=\"" + connectionAttributes + "\" ");
        if (createDatabase != null)
            buf.append("createDatabase=\"" + createDatabase + "\" ");
        if (super.getDatabaseName() != null)
            buf.append("databaseName=\"" + super.getDatabaseName() + "\" ");
        if (super.getLoginTimeout() != null)
            buf.append("loginTimeout=\"" + super.getLoginTimeout() + "\" ");
        if (super.getPassword() != null)
            buf.append("password=\"" + super.getPassword() + "\" ");
        if (shutdownDatabase != null)
            buf.append("shutdownDatabase=\"" + shutdownDatabase + "\" ");
        if (super.getUser() != null)
            buf.append("user=\"" + super.getUser() + "\" ");
        buf.append("}");
        return buf.toString();
    }
}
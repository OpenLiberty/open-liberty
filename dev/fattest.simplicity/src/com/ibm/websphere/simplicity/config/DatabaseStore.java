/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
 * databaseStore element representation.
 */
public class DatabaseStore extends ConfigElement {

    /** Config reference to the authData attribute. */
    private String authDataRef;

    @XmlElement(name = "authData")
    private ConfigElementList<AuthData> authDatas;

    /** Config reference to the dataSource attribute. */
    private String dataSourceRef;

    @XmlElement(name = "dataSource")
    private ConfigElementList<DataSource> dataSources;

    private String keyGenerationStrategy;

    /** The database schema name attribute. */
    private String schema;

    /** The table name prefix attribute. */
    private String tablePrefix;

    /** The create tables indicator. */
    private String createTables;

    /**
     * Set the authDataRef attribute.
     * 
     * @param authDataRef The authDataRef attribute to be set.
     */
    @XmlAttribute
    public void setAuthDataRef(String authDataRef) {
        this.authDataRef = authDataRef;
    }

    /**
     * Returns the authDataRef attribute value.
     * 
     * @return The authDataRef attribute value.
     */
    public String getAuthDataRef() {
        return authDataRef;
    }

    public ConfigElementList<AuthData> getAuthDatas() {
        return authDatas == null ? (authDatas = new ConfigElementList<AuthData>()) : authDatas;
    }

    /**
     * Sets the dataSourceRef attribute.
     * 
     * @param dataSourceRef The set attribute.
     */
    @XmlAttribute
    public void setDataSourceRef(String dataSourceRef) {
        this.dataSourceRef = dataSourceRef;
    }

    /**
     * Returns the dataSourceRef attribute value.
     * 
     * @return The dataSourceRef attribute value.
     */
    public String getDataSourceRef() {
        return dataSourceRef;
    }

    public ConfigElementList<DataSource> getDataSources() {
        return dataSources == null ? (dataSources = new ConfigElementList<DataSource>()) : dataSources;
    }

    @XmlAttribute
    public void setKeyGenerationStrategy(String keyGenerationStrategy) {
        this.keyGenerationStrategy = keyGenerationStrategy;
    }

    public String getKeyGenerationStrategy() {
        return keyGenerationStrategy;
    }

    /**
     * Sets the schema name attribute value.
     * 
     * @param schema The schema name attribute value.
     */
    @XmlAttribute
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Returns the schema name attribute value.
     * 
     * @return The schema name attribute value.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Sets the table prefix name attribute value.
     * 
     * @param tablePrefix the table prefix name attribute value.
     */
    @XmlAttribute
    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    /**
     * Returns the table prefix name.
     * 
     * @return The table prefix name.
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    /**
     * Sets the create tables indicator attribute value.
     * 
     * @param createTables The create tables indicator.
     */
    @XmlAttribute
    public void setCreateTables(String createTables) {
        this.createTables = createTables;
    }

    /**
     * Returns the create tables indicator.
     * 
     * @return The create tables indicator.
     */
    public String getCreateTables() {
        return createTables;
    }

    /**
     * Returns a String representation of this object.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("DatabaseStore {");
        if (super.getId() != null)
            buf.append("id=\"" + super.getId() + "\", ");
        if (keyGenerationStrategy != null)
            buf.append("keyGenerationStrategy=\"" + keyGenerationStrategy + "\", ");
        if (tablePrefix != null)
            buf.append("tablePrefix=\"" + tablePrefix + "\", ");
        if (schema != null)
            buf.append("schema=\"" + schema + "\", ");
        if (dataSourceRef != null)
            buf.append("dataSourceRef=\"" + dataSourceRef + "\", ");
        if (authDataRef != null)
            buf.append("authDataRef=\"" + authDataRef + "\", ");
        if (createTables != null)
            buf.append("createTables=\"" + createTables + "\", ");
        if (dataSources != null)
            buf.append(dataSources).append(' ');
        if (authDatas != null)
            buf.append(authDatas).append(' ');
        buf.append("}");
        return buf.toString();
    }
}

/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.ibm.websphere.simplicity.config.dsprops.Properties;
import com.ibm.websphere.simplicity.config.dsprops.Properties_datadirect_sqlserver;
import com.ibm.websphere.simplicity.config.dsprops.Properties_db2_i_native;
import com.ibm.websphere.simplicity.config.dsprops.Properties_db2_i_toolbox;
import com.ibm.websphere.simplicity.config.dsprops.Properties_db2_jcc;
import com.ibm.websphere.simplicity.config.dsprops.Properties_derby_client;
import com.ibm.websphere.simplicity.config.dsprops.Properties_derby_embedded;
import com.ibm.websphere.simplicity.config.dsprops.Properties_informix;
import com.ibm.websphere.simplicity.config.dsprops.Properties_informix_jcc;
import com.ibm.websphere.simplicity.config.dsprops.Properties_microsoft_sqlserver;
import com.ibm.websphere.simplicity.config.dsprops.Properties_oracle;
import com.ibm.websphere.simplicity.config.dsprops.Properties_oracle_ucp;
import com.ibm.websphere.simplicity.config.dsprops.Properties_postgresql;
import com.ibm.websphere.simplicity.config.dsprops.Properties_sybase;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;

/**
 * Defines a data source in the server configuration
 *
 * @author Tim Burns
 *
 */
public class DataSource extends ConfigElement {

    private static final Class<DataSource> c = DataSource.class;

    private String jndiName;
    private String jdbcDriverRef;
    private String connectionManagerRef;
    private String propertiesRef;
    private String type;
    private String connectionSharing;
    private String isolationLevel;
    private String statementCacheSize;
    private String transactional;
    private String beginTranForResultSetScrollingAPIs;
    private String beginTranForVendorAPIs;
    private String commitOrRollbackOnCleanup;
    private String containerAuthDataRef;
    private String enableConnectionCasting;
    private String enableMultithreadedAccessDetection;
    private String onConnect;
    private String queryTimeout;
    private String recoveryAuthDataRef;
    private String syncQueryTimeoutWithTransactionTimeout;
    private String supplementalJDBCTrace;
    private String validationTimeout;
    protected ServerConfiguration parentConfig;
    private String badProperty;
    private String fatModify;

    // nested elements
    @XmlElement(name = "connectionManager")
    private ConfigElementList<ConnectionManager> connectionManagers;

    @XmlElement(name = "containerAuthData")
    private ConfigElementList<AuthData> containerAuthDatas;

    @XmlElement(name = "heritageSettings")
    private ConfigElementList<HeritageSettings> heritageSettings;

    @XmlElement(name = "identifyException")
    private ConfigElementList<IdentifyException> identifyExceptions;

    @XmlElement(name = "jdbcDriver")
    private ConfigElementList<JdbcDriver> jdbcDrivers;

    @XmlElement(name = "onConnect")
    private LinkedHashSet<String> onConnects;

    @XmlElement(name = "recoveryAuthData")
    private ConfigElementList<AuthData> recoveryAuthDatas;

    @XmlElement(name = DataSourceProperties.DB2_I_NATIVE)
    private ConfigElementList<Properties_db2_i_native> db2iNativeProps;

    @XmlElement(name = DataSourceProperties.DB2_I_TOOLBOX)
    private ConfigElementList<Properties_db2_i_toolbox> db2iToolboxProps;

    @XmlElement(name = DataSourceProperties.DB2_JCC)
    private ConfigElementList<Properties_db2_jcc> db2JccProps;

    @XmlElement(name = DataSourceProperties.DERBY_EMBEDDED)
    private ConfigElementList<Properties_derby_embedded> derbyEmbeddedProps;

    @XmlElement(name = DataSourceProperties.DERBY_CLIENT)
    private ConfigElementList<Properties_derby_client> derbyNetClientProps;

    @XmlElement(name = DataSourceProperties.GENERIC)
    private ConfigElementList<Properties> genericProps;

    @XmlElement(name = DataSourceProperties.INFORMIX_JCC)
    private ConfigElementList<Properties_informix_jcc> informixJccProps;

    @XmlElement(name = DataSourceProperties.INFORMIX_JDBC)
    private ConfigElementList<Properties_informix> informixJdbcProps;

    @XmlElement(name = DataSourceProperties.ORACLE_JDBC)
    private ConfigElementList<Properties_oracle> oracleProps;

    @XmlElement(name = DataSourceProperties.ORACLE_UCP)
    private ConfigElementList<Properties_oracle_ucp> oracleUcpProps;

    @XmlElement(name = DataSourceProperties.POSTGRESQL)
    private ConfigElementList<Properties_postgresql> postgresqlProps;

    @XmlElement(name = DataSourceProperties.DATADIRECT_SQLSERVER)
    private ConfigElementList<Properties_datadirect_sqlserver> sqlServerDataDirectProps;

    @XmlElement(name = DataSourceProperties.MICROSOFT_SQLSERVER)
    private ConfigElementList<Properties_microsoft_sqlserver> sqlServerProps;

    @XmlElement(name = DataSourceProperties.SYBASE)
    private ConfigElementList<Properties_sybase> sybaseProps;

    public ConfigElementList<ConnectionManager> getConnectionManagers() {
        return connectionManagers == null ? (connectionManagers = new ConfigElementList<ConnectionManager>()) : connectionManagers;
    }

    public ConfigElementList<AuthData> getContainerAuthDatas() {
        return containerAuthDatas == null ? (containerAuthDatas = new ConfigElementList<AuthData>()) : containerAuthDatas;
    }

    public ConfigElementList<HeritageSettings> getHeritageSettings() {
        return heritageSettings == null ? (heritageSettings = new ConfigElementList<HeritageSettings>()) : heritageSettings;
    }

    public ConfigElementList<IdentifyException> getIdentifyExceptions() {
        return identifyExceptions == null ? (identifyExceptions = new ConfigElementList<IdentifyException>()) : identifyExceptions;
    }

    /**
     * @return the JNDI name of this data source
     */
    public String getJndiName() {
        return this.jndiName;
    }

    /**
     * @param jndiName the JNDI name of this data source
     */
    @XmlAttribute
    public void setJndiName(String jndiName) {
        this.jndiName = ConfigElement.getValue(jndiName);
    }

    /**
     * @return the ID of the JDBC Driver to used for this data source
     */
    public String getJdbcDriverRef() {
        return this.jdbcDriverRef;
    }

    /**
     * @param jdbcDriverRef the ID of the JDBC Driver to used for this data source
     */
    @XmlAttribute
    public void setJdbcDriverRef(String jdbcDriverRef) {
        this.jdbcDriverRef = ConfigElement.getValue(jdbcDriverRef);
    }

    public ConfigElementList<JdbcDriver> getJdbcDrivers() {
        return jdbcDrivers == null ? (jdbcDrivers = new ConfigElementList<JdbcDriver>()) : jdbcDrivers;
    }

    public Set<String> getOnConnects() {
        return onConnects == null ? (onConnects = new LinkedHashSet<String>()) : onConnects;
    }

    public ConfigElementList<AuthData> getRecoveryAuthDatas() {
        return recoveryAuthDatas == null ? (recoveryAuthDatas = new ConfigElementList<AuthData>()) : recoveryAuthDatas;
    }

    public String getDataSourcePropertiesUsedAlias() {
        if (db2iNativeProps != null)
            return DataSourceProperties.DB2_I_NATIVE;
        else if (db2iToolboxProps != null)
            return DataSourceProperties.DB2_I_TOOLBOX;
        else if (db2JccProps != null)
            return DataSourceProperties.DB2_JCC;
        else if (derbyEmbeddedProps != null)
            return DataSourceProperties.DERBY_EMBEDDED;
        else if (derbyNetClientProps != null)
            return DataSourceProperties.DERBY_CLIENT;
        else if (genericProps != null)
            return DataSourceProperties.GENERIC;
        else if (informixJccProps != null)
            return DataSourceProperties.INFORMIX_JCC;
        else if (informixJdbcProps != null)
            return DataSourceProperties.INFORMIX_JDBC;
        else if (oracleProps != null)
            return DataSourceProperties.ORACLE_JDBC;
        else if (oracleUcpProps != null)
            return DataSourceProperties.ORACLE_UCP;
        else if (postgresqlProps != null)
            return DataSourceProperties.POSTGRESQL;
        else if (sqlServerDataDirectProps != null)
            return DataSourceProperties.DATADIRECT_SQLSERVER;
        else if (sqlServerProps != null)
            return DataSourceProperties.MICROSOFT_SQLSERVER;
        else if (sybaseProps != null)
            return DataSourceProperties.SYBASE;
        else
            return null;
    }

    public Set<DataSourceProperties> getDataSourceProperties() {
        Set<DataSourceProperties> nestedPropElements = new HashSet<DataSourceProperties>();
        if (this.db2iNativeProps != null)
            nestedPropElements.addAll(this.db2iNativeProps);
        if (this.db2iToolboxProps != null)
            nestedPropElements.addAll(this.db2iToolboxProps);
        if (this.db2JccProps != null)
            nestedPropElements.addAll(this.db2JccProps);
        if (this.derbyEmbeddedProps != null)
            nestedPropElements.addAll(this.derbyEmbeddedProps);
        if (this.derbyNetClientProps != null)
            nestedPropElements.addAll(this.derbyNetClientProps);
        if (this.genericProps != null)
            nestedPropElements.addAll(this.genericProps);
        if (this.informixJccProps != null)
            nestedPropElements.addAll(this.informixJccProps);
        if (this.informixJdbcProps != null)
            nestedPropElements.addAll(this.informixJdbcProps);
        if (this.oracleProps != null)
            nestedPropElements.addAll(this.oracleProps);
        if (this.oracleUcpProps != null)
            nestedPropElements.addAll(this.oracleUcpProps);
        if (this.postgresqlProps != null)
            nestedPropElements.addAll(this.postgresqlProps);
        if (this.sqlServerDataDirectProps != null)
            nestedPropElements.addAll(this.sqlServerDataDirectProps);
        if (this.sqlServerProps != null)
            nestedPropElements.addAll(this.sqlServerProps);
        if (this.sybaseProps != null)
            nestedPropElements.addAll(this.sybaseProps);
        return nestedPropElements;
    }

    public void clearDataSourceDBProperties() {
        if (this.db2iNativeProps != null)
            this.db2iNativeProps.clear();
        if (this.db2iToolboxProps != null)
            this.db2iToolboxProps.clear();
        if (this.db2JccProps != null)
            this.db2JccProps.clear();
        if (this.derbyEmbeddedProps != null)
            this.derbyEmbeddedProps.clear();
        if (this.derbyNetClientProps != null)
            this.derbyNetClientProps.clear();
        if (this.genericProps != null)
            this.genericProps.clear();
        if (this.informixJccProps != null)
            this.informixJccProps.clear();
        if (this.informixJdbcProps != null)
            this.informixJdbcProps.clear();
        if (this.oracleProps != null)
            this.oracleProps.clear();
        if (this.oracleUcpProps != null)
            this.oracleUcpProps.clear();
        if (this.postgresqlProps != null)
            this.postgresqlProps.clear();
        if (this.sqlServerDataDirectProps != null)
            this.sqlServerDataDirectProps.clear();
        if (this.sqlServerProps != null)
            this.sqlServerProps.clear();
        if (this.sybaseProps != null)
            this.sybaseProps.clear();
    }

    /**
     * Retrieves custom properties configured on this instance, if any already exist
     *
     * @return custom properties configured on this instance, if any already exist
     */
    public ConfigElementList<Properties> getProperties() {
        return this.genericProps == null ? (genericProps = new ConfigElementList<Properties>()) : genericProps;
    }

    public ConfigElementList<Properties_datadirect_sqlserver> getProperties_datadirect_sqlserver() {
        return this.sqlServerDataDirectProps == null ? (sqlServerDataDirectProps = new ConfigElementList<Properties_datadirect_sqlserver>()) : sqlServerDataDirectProps;
    }

    public ConfigElementList<Properties_db2_i_native> getProperties_db2_i_native() {
        return this.db2iNativeProps == null ? (db2iNativeProps = new ConfigElementList<Properties_db2_i_native>()) : db2iNativeProps;
    }

    public ConfigElementList<Properties_db2_i_toolbox> getProperties_db2_i_toolbox() {
        return this.db2iToolboxProps == null ? (db2iToolboxProps = new ConfigElementList<Properties_db2_i_toolbox>()) : db2iToolboxProps;
    }

    public ConfigElementList<Properties_db2_jcc> getProperties_db2_jcc() {
        return this.db2JccProps == null ? (db2JccProps = new ConfigElementList<Properties_db2_jcc>()) : db2JccProps;
    }

    public ConfigElementList<Properties_derby_client> getProperties_derby_client() {
        return this.derbyNetClientProps == null ? (derbyNetClientProps = new ConfigElementList<Properties_derby_client>()) : derbyNetClientProps;
    }

    public ConfigElementList<Properties_derby_embedded> getProperties_derby_embedded() {
        return this.derbyEmbeddedProps == null ? (derbyEmbeddedProps = new ConfigElementList<Properties_derby_embedded>()) : derbyEmbeddedProps;
    }

    public ConfigElementList<Properties_informix_jcc> getProperties_informix_jcc() {
        return this.informixJccProps == null ? (informixJccProps = new ConfigElementList<Properties_informix_jcc>()) : informixJccProps;
    }

    public ConfigElementList<Properties_informix> getProperties_informix() {
        return this.informixJdbcProps == null ? (informixJdbcProps = new ConfigElementList<Properties_informix>()) : informixJdbcProps;
    }

    public ConfigElementList<Properties_microsoft_sqlserver> getProperties_microsoft_sqlserver() {
        return this.sqlServerProps == null ? (sqlServerProps = new ConfigElementList<Properties_microsoft_sqlserver>()) : sqlServerProps;
    }

    public ConfigElementList<Properties_oracle> getProperties_oracle() {
        return this.oracleProps == null ? (oracleProps = new ConfigElementList<Properties_oracle>()) : oracleProps;
    }

    public ConfigElementList<Properties_oracle_ucp> getProperties_oracle_ucp() {
        return this.oracleUcpProps == null ? (oracleUcpProps = new ConfigElementList<Properties_oracle_ucp>()) : oracleUcpProps;
    }

    public ConfigElementList<Properties_postgresql> getProperties_postgresql() {
        return this.postgresqlProps == null ? (postgresqlProps = new ConfigElementList<Properties_postgresql>()) : postgresqlProps;
    }

    public ConfigElementList<Properties_sybase> getProperties_sybase() {
        return this.sybaseProps == null ? (sybaseProps = new ConfigElementList<Properties_sybase>()) : sybaseProps;
    }

    public String getConnectionManagerRef() {
        return connectionManagerRef;
    }

    @XmlAttribute
    public void setConnectionManagerRef(String connectionManagerRef) {
        this.connectionManagerRef = connectionManagerRef;
    }

    public String getPropertiesRef() {
        return propertiesRef;
    }

    @XmlAttribute
    public void setPropertiesRef(String propertiesRef) {
        this.propertiesRef = propertiesRef;
    }

    public String getType() {
        return type;
    }

    @XmlAttribute
    public void setType(String type) {
        this.type = type;
    }

    public String getConnectionSharing() {
        return connectionSharing;
    }

    @XmlAttribute
    public void setConnectionSharing(String connectionSharing) {
        this.connectionSharing = connectionSharing;
    }

    public String getIsolationLevel() {
        return isolationLevel;
    }

    @XmlAttribute
    public void setIsolationLevel(String isolationLevel) {
        this.isolationLevel = isolationLevel;
    }

    public String getStatementCacheSize() {
        return statementCacheSize;
    }

    @XmlAttribute
    public void setStatementCacheSize(String statementCacheSize) {
        this.statementCacheSize = statementCacheSize;
    }

    public String getTransactional() {
        return transactional;
    }

    @XmlAttribute
    public void setTransactional(String transactional) {
        this.transactional = transactional;
    }

    public String getBeginTranForResultSetScrollingAPIs() {
        return beginTranForResultSetScrollingAPIs;
    }

    @XmlAttribute
    public void setBeginTranForResultSetScrollingAPIs(String beginTranForResultSetScrollingAPIs) {
        this.beginTranForResultSetScrollingAPIs = beginTranForResultSetScrollingAPIs;
    }

    public String getBeginTranForVendorAPIs() {
        return beginTranForVendorAPIs;
    }

    @XmlAttribute
    public void setBeginTranForVendorAPIs(String beginTranForVendorAPIs) {
        this.beginTranForVendorAPIs = beginTranForVendorAPIs;
    }

    public String getCommitOrRollbackOnCleanup() {
        return commitOrRollbackOnCleanup;
    }

    @XmlAttribute
    public void setCommitOrRollbackOnCleanup(String commitOrRollbackOnCleanup) {
        this.commitOrRollbackOnCleanup = commitOrRollbackOnCleanup;
    }

    public String getContainerAuthDataRef() {
        return containerAuthDataRef;
    }

    @XmlAttribute
    public void setContainerAuthDataRef(String containerAuthDataRef) {
        this.containerAuthDataRef = containerAuthDataRef;
    }

    public String getEnableConnectionCasting() {
        return enableConnectionCasting;
    }

    @XmlAttribute
    public void setEnableConnectionCasting(String enableConnectionCasting) {
        this.enableConnectionCasting = enableConnectionCasting;
    }

    public String getEnableMultithreadedAccessDetection() {
        return enableMultithreadedAccessDetection;
    }

    @XmlAttribute
    public void setEnableMultithreadedAccessDetection(String enableMultithreadedAccessDetection) {
        this.enableMultithreadedAccessDetection = enableMultithreadedAccessDetection;
    }

    public String getOnConnect() {
        return onConnect;
    }

    public String getQueryTimeout() {
        return queryTimeout;
    }

    @XmlAttribute
    public void setOnConnect(String onConnect) {
        this.onConnect = onConnect;
    }

    @XmlAttribute
    public void setQueryTimeout(String queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public String getRecoveryAuthDataRef() {
        return recoveryAuthDataRef;
    }

    @XmlAttribute
    public void setRecoveryAuthDataRef(String recoveryAuthDataRef) {
        this.recoveryAuthDataRef = recoveryAuthDataRef;
    }

    public String getSyncQueryTimeoutWithTransactionTimeout() {
        return syncQueryTimeoutWithTransactionTimeout;
    }

    @XmlAttribute
    public void setSyncQueryTimeoutWithTransactionTimeout(String syncQueryTimeoutWithTransactionTimeout) {
        this.syncQueryTimeoutWithTransactionTimeout = syncQueryTimeoutWithTransactionTimeout;
    }

    public String getSupplementalJDBCTrace() {
        return supplementalJDBCTrace;
    }

    @XmlAttribute
    public void setSupplementalJDBCTrace(String supplementalJDBCTrace) {
        this.supplementalJDBCTrace = supplementalJDBCTrace;
    }

    public String getValidationTimeout() {
        return validationTimeout;
    }

    @XmlAttribute
    public void setValidationTimeout(String validationTimeout) {
        this.validationTimeout = validationTimeout;
    }

    public String getBadProperty() {
        return badProperty;
    }

    @XmlAttribute
    public void setBadProperty(String badProperty) {
        this.badProperty = badProperty;
    }

    @XmlAttribute(name = "fat.modify")
    public void setFatModify(String fatModify) {
        this.fatModify = fatModify;
    }

    public String getFatModify() {
        return fatModify;
    }

    public void replaceDatasourceProperties(DataSourceProperties props) {
        this.clearDataSourceDBProperties();

        if (props instanceof Properties_db2_i_native)
            this.getProperties_db2_i_native().add((Properties_db2_i_native) props);
        else if (props instanceof Properties_db2_i_toolbox)
            this.getProperties_db2_i_toolbox().add((Properties_db2_i_toolbox) props);
        else if (props instanceof Properties_db2_jcc)
            this.getProperties_db2_jcc().add((Properties_db2_jcc) props);
        else if (props instanceof Properties_derby_embedded)
            this.getProperties_derby_embedded().add((Properties_derby_embedded) props);
        else if (props instanceof Properties_derby_client)
            this.getProperties_derby_client().add((Properties_derby_client) props);
        else if (props instanceof Properties)
            this.getProperties().add((Properties) props);
        else if (props instanceof Properties_informix_jcc)
            this.getProperties_informix_jcc().add((Properties_informix_jcc) props);
        else if (props instanceof Properties_informix)
            this.getProperties_informix().add((Properties_informix) props);
        else if (props instanceof Properties_oracle)
            this.getProperties_oracle().add((Properties_oracle) props);
        else if (props instanceof Properties_oracle_ucp)
            this.getProperties_oracle_ucp().add((Properties_oracle_ucp) props);
        else if (props instanceof Properties_postgresql)
            this.getProperties_postgresql().add((Properties_postgresql) props);
        else if (props instanceof Properties_datadirect_sqlserver)
            this.getProperties_datadirect_sqlserver().add((Properties_datadirect_sqlserver) props);
        else if (props instanceof Properties_microsoft_sqlserver)
            this.getProperties_microsoft_sqlserver().add((Properties_microsoft_sqlserver) props);
        else if (props instanceof Properties_sybase)
            this.getProperties_sybase().add((Properties_sybase) props);
    }

    /**
     * Returns a string containing a list of the property names and their values
     * for this DataSource object.
     *
     * @return String representing the data held
     */
    @Override
    public String toString() {
        Set<DataSourceProperties> properties = getDataSourceProperties();
        StringBuffer buf = new StringBuffer("DataSource{");
        buf.append("id=\"" + (getId() == null ? "" : getId()) + "\" ");
        if (beginTranForResultSetScrollingAPIs != null)
            buf.append("beginTranForResultSetScrollingAPIs=\"" + beginTranForResultSetScrollingAPIs + "\" ");
        if (beginTranForVendorAPIs != null)
            buf.append("beginTranForVendorAPIs=\"" + beginTranForVendorAPIs + "\" ");
        if (commitOrRollbackOnCleanup != null)
            buf.append("commitOrRollbackOnCleanup=\"" + commitOrRollbackOnCleanup + "\" ");
        if (connectionManagerRef != null)
            buf.append("connectionManagerRef=\"" + connectionManagerRef + "\" ");
        if (connectionSharing != null)
            buf.append("connectionSharing=\"" + connectionSharing + "\" ");
        if (containerAuthDataRef != null)
            buf.append("containerAuthDataRef=\"" + containerAuthDataRef + "\" ");
        if (enableConnectionCasting != null)
            buf.append("enableConnectionCasting=\"" + enableConnectionCasting + "\" ");
        if (enableMultithreadedAccessDetection != null)
            buf.append("enableMultithreadedAccessDetection=\"" + enableMultithreadedAccessDetection + "\" ");
        if (isolationLevel != null)
            buf.append("isolationLevel=\"" + isolationLevel + "\" ");
        if (jdbcDriverRef != null)
            buf.append("jdbcDriverRef=\"" + jdbcDriverRef + "\" ");
        if (jndiName != null)
            buf.append("jndiName=\"" + jndiName + "\" ");
        if (propertiesRef != null)
            buf.append("propertiesRef=\"" + propertiesRef + "\" ");
        if (onConnect != null)
            buf.append("onConnect=\"" + onConnects + "\" ");
        if (queryTimeout != null)
            buf.append("queryTimeout=\"" + queryTimeout + "\" ");
        if (recoveryAuthDataRef != null)
            buf.append("recoveryAuthDataRef=\"" + recoveryAuthDataRef + "\" ");
        if (statementCacheSize != null)
            buf.append("statementCacheSize=\"" + statementCacheSize + "\" ");
        if (supplementalJDBCTrace != null)
            buf.append("supplementalJDBCTrace=\"" + supplementalJDBCTrace + "\" ");
        if (syncQueryTimeoutWithTransactionTimeout != null)
            buf.append("syncQueryTimeoutWithTransactionTimeout=\"" + syncQueryTimeoutWithTransactionTimeout + "\" ");
        if (transactional != null)
            buf.append("transactional=\"" + transactional + "\" ");
        if (type != null)
            buf.append("type=\"" + type + "\" ");
        if (validationTimeout != null)
            buf.append("validationTimeout=\"" + validationTimeout + "\" ");
        buf.append(properties == null ? "no vendor properties" : properties.toString());
        if (connectionManagers != null)
            buf.append(connectionManagers).append(' ');
        if (containerAuthDatas != null)
            buf.append(containerAuthDatas).append(' ');
        if (heritageSettings != null)
            buf.append(heritageSettings).append(' ');
        if (identifyExceptions != null)
            buf.append(identifyExceptions).append(' ');
        if (jdbcDrivers != null)
            buf.append(jdbcDrivers).append(' ');
        if (onConnects != null)
            buf.append("onConnects:").append(onConnects).append(' ');
        if (recoveryAuthDatas != null)
            buf.append(recoveryAuthDatas).append(' ');
        buf.append("}");
        return buf.toString();
    }

    /**
     * Update <dataSource> from bootstrapping.properties.
     * <ul>
     * <li>Update nested <jdbcDriver>.
     * <li>Remove the existing <dataSource> nested properties entries.
     * <li>Create a <dataSource> nested <properties> entry based on bootstrapping.properties
     * and add it to the <dataSource>.
     *
     */
    public void updateDataSourceFromBootstrap(ServerConfiguration config) throws Exception {
        Log.entering(c, "updateDataSourceFromBootstrap");
        Bootstrap bs = Bootstrap.getInstance();
        if (bs.getValue(BootstrapProperty.DB_DRIVERNAME.getPropertyName()) == null) {
            return;
        }

        Log.info(c, "updateDataSourceFromBootstrap", "Updating dataSource " + getId());
        for (JdbcDriver jdbcDriver : getJdbcDrivers()) {
            Log.info(c, "updateDataSourceFromBootstrap", "Update nested JDBC Driver");
            if (!"false".equalsIgnoreCase(jdbcDriver.getFatModify()))
                jdbcDriver.updateJdbcDriverFromBootstrap(config);
        }

        Log.info(c, "updateDataSourceFromBootstrap", "Delete properties and create new one");

        DataSourceProperties dsp = getDataSourceProperties().iterator().next();
        boolean hasUser = (dsp.getUser() != null);
        boolean hasPass = (dsp.getPassword() != null);

        String database_hostname = bs.getValue(BootstrapProperty.DB_HOSTNAME.getPropertyName());
        String database_port = bs.getValue(BootstrapProperty.DB_PORT.getPropertyName());
        String database_name = bs.getValue(BootstrapProperty.DB_NAME.getPropertyName());
        String database_user1 = (hasUser ? bs.getValue(BootstrapProperty.DB_USER1.getPropertyName()) : null);
        String database_password1 = (hasPass ? bs.getValue(BootstrapProperty.DB_PASSWORD1.getPropertyName()) : null);
        String database_drivername = bs.getValue(BootstrapProperty.DB_DRIVERNAME.getPropertyName());
        String database_vendorname = bs.getValue(BootstrapProperty.DB_VENDORNAME.getPropertyName());
        String database_servername = bs.getValue(BootstrapProperty.DB_IFXSERVERNAME.getPropertyName());

        clearDataSourceDBProperties();

        if (database_vendorname.equalsIgnoreCase(BootstrapProperty.DB_DERBYEMB.getPropertyName())
            && (database_drivername.equalsIgnoreCase(BootstrapProperty.DB_DERBY_EMBEDDED_DRIVER.getPropertyName())
                || database_drivername.equalsIgnoreCase(BootstrapProperty.DB_DERBY_EMBEDDED_DRIVER_40.getPropertyName()))) {
            // TODO Derby Embedded is not yet supported as a specified database
            Properties_derby_embedded properties = new Properties_derby_embedded();
            properties.setCreateDatabase("create");
            properties.setDatabaseName("${shared.resource.dir}/data/" + database_name);
            properties.setPassword(database_password1);
            properties.setPortNumber(database_port); // may not need this
            properties.setUser(database_user1);
            properties.setServerName(database_hostname);
            derbyEmbeddedProps = new ConfigElementList<Properties_derby_embedded>();
            derbyEmbeddedProps.add(properties);
            throw new Exception("Database or driver not yet supported");
        } else if (database_vendorname.equalsIgnoreCase(BootstrapProperty.DB_DERBYNET.getPropertyName())
                   && (database_drivername.equalsIgnoreCase(BootstrapProperty.DB_DERBY_NETWORK_CLIENT_DRIVER.getPropertyName())
                       || database_drivername.equalsIgnoreCase(BootstrapProperty.DB_DERBY_NETWORK_CLIENT_DRIVER_40.getPropertyName()))) {
            // TODO Derby Network is not yet supported as a specified database
            Properties_derby_client properties = new Properties_derby_client();
            properties.setCreateDatabase("create");
            properties.setDatabaseName("${shared.resource.dir}/data/" + database_name);
            properties.setPassword(database_password1);
            properties.setPortNumber(database_port);
            properties.setUser(database_user1);
            properties.setServerName(database_hostname);
            derbyNetClientProps = new ConfigElementList<Properties_derby_client>();
            derbyNetClientProps.add(properties);
            throw new Exception("Database or driver not yet supported");
        } else if (database_vendorname.equalsIgnoreCase(BootstrapProperty.DB_DB2.getPropertyName())
                   && database_drivername.equalsIgnoreCase(BootstrapProperty.DB_DB2_JCC_DRIVER.getPropertyName())) {
            Properties_db2_jcc properties = new Properties_db2_jcc();
            properties.setDatabaseName(database_name);
            properties.setPassword(database_password1);
            properties.setPortNumber(database_port);
            properties.setUser(database_user1);
            properties.setServerName(database_hostname);
            db2JccProps = new ConfigElementList<Properties_db2_jcc>();
            db2JccProps.add(properties);
        } else if (database_vendorname.equalsIgnoreCase(BootstrapProperty.DB_DB2_ISERIES.getPropertyName())
                   && database_drivername.equalsIgnoreCase(BootstrapProperty.DB_DB2_INATIVE_DRIVER.getPropertyName())) {
            // TODO iSeries is not yet supported as a specified database
            Properties_db2_i_native properties = new Properties_db2_i_native();
            // TODO finish setting the properties
            db2iNativeProps = new ConfigElementList<Properties_db2_i_native>();
            db2iNativeProps.add(properties);
            throw new Exception("Database or driver not yet supported");
        } else if (database_vendorname.equalsIgnoreCase(BootstrapProperty.DB_DB2_ISERIES.getPropertyName())
                   && database_drivername.equalsIgnoreCase(BootstrapProperty.DB_DB2_ITOOLBOX_DRIVER.getPropertyName())) {
            // TODO iSeries is not yet supported as a specified database
            Properties_db2_i_toolbox properties = new Properties_db2_i_toolbox();
            // TODO finish setting the properties
            db2iToolboxProps = new ConfigElementList<Properties_db2_i_toolbox>();
            db2iToolboxProps.add(properties);
            throw new Exception("Database or driver not yet supported");
        } else if (database_vendorname.equalsIgnoreCase(BootstrapProperty.DB_DB2_ZOS.getPropertyName())
                   && database_drivername.equalsIgnoreCase(BootstrapProperty.DB_DB2_JCC_DRIVER.getPropertyName())) {
            // TODO zOS is not yet supported as a specified database
            Properties_db2_jcc properties = new Properties_db2_jcc();
            // TODO finish setting the properties
            db2JccProps = new ConfigElementList<Properties_db2_jcc>();
            db2JccProps.add(properties);
            throw new Exception("Database or driver not yet supported");
        } else if (database_vendorname.equalsIgnoreCase(BootstrapProperty.DB_INFORMIX.getPropertyName())
                   && database_drivername.equalsIgnoreCase(BootstrapProperty.DB_DB2_JCC_DRIVER.getPropertyName())) {
            Properties_informix_jcc properties = new Properties_informix_jcc();
            properties.setDatabaseName(database_name);
            properties.setPassword(database_password1);
            properties.setPortNumber(database_port);
            properties.setUser(database_user1);
            properties.setServerName(database_hostname);
            informixJccProps = new ConfigElementList<Properties_informix_jcc>();
            informixJccProps.add(properties);
        } else if (database_vendorname.equalsIgnoreCase(BootstrapProperty.DB_INFORMIX.getPropertyName())
                   && database_drivername.equalsIgnoreCase(BootstrapProperty.DB_INFORMIX_DRIVER.getPropertyName())) {
            Properties_informix properties = new Properties_informix();
            properties.setDatabaseName(database_name);
            properties.setPassword(database_password1);
            properties.setPortNumber(database_port);
            properties.setUser(database_user1);
            properties.setServerName(database_servername);
            properties.setIfxIFXHOST(database_hostname);
            informixJdbcProps = new ConfigElementList<Properties_informix>();
            informixJdbcProps.add(properties);
        } else if (database_vendorname.equalsIgnoreCase(BootstrapProperty.DB_ORACLE.getPropertyName())
                   && database_drivername.equalsIgnoreCase(BootstrapProperty.DB_ORACLE_DRIVER.getPropertyName())) {
            Properties_oracle properties = new Properties_oracle();
            String URL = "jdbc:oracle:thin:@//" + database_hostname + ":" + database_port + "/" + database_name;
            properties.setPassword(database_password1);
            properties.setUser(database_user1);
            properties.setURL(URL);
            oracleProps = new ConfigElementList<Properties_oracle>();
            oracleProps.add(properties);
        } else if (database_vendorname.equalsIgnoreCase(BootstrapProperty.DB_ORACLE.getPropertyName())
                   && database_drivername.equalsIgnoreCase("OracleUCP ???")) {
            // TODO add processing for Oracle UCP at some point in the future
            Properties_oracle_ucp properties = new Properties_oracle_ucp();
            String URL = "jdbc:oracle:thin:@//" + database_hostname + ":" + database_port + "/" + database_name;
            properties.setPassword(database_password1);
            properties.setUser(database_user1);
            properties.setURL(URL);
            oracleUcpProps = new ConfigElementList<Properties_oracle_ucp>();
            oracleUcpProps.add(properties);
        } else if (database_vendorname.equalsIgnoreCase(BootstrapProperty.DB_SQLSERVER.getPropertyName())
                   && database_drivername.equalsIgnoreCase(BootstrapProperty.DB_SQLSERVER_DRIVER.getPropertyName())) {
            Properties_microsoft_sqlserver properties = new Properties_microsoft_sqlserver();
            properties.setDatabaseName(database_name);
            properties.setPassword(database_password1);
            properties.setPortNumber(database_port);
            properties.setUser(database_user1);
            properties.setServerName(database_hostname);
            sqlServerProps = new ConfigElementList<Properties_microsoft_sqlserver>();
            sqlServerProps.add(properties);
        } else if (database_vendorname.equalsIgnoreCase(BootstrapProperty.DB_SQLSERVER.getPropertyName())
                   && database_drivername.equalsIgnoreCase(BootstrapProperty.DB_DATADIRECT_DRIVER.getPropertyName())) {
            Properties_datadirect_sqlserver properties = new Properties_datadirect_sqlserver();
            properties.setDatabaseName(database_name);
            properties.setPassword(database_password1);
            properties.setPortNumber(database_port);
            properties.setUser(database_user1);
            properties.setServerName(database_hostname);
            sqlServerDataDirectProps = new ConfigElementList<Properties_datadirect_sqlserver>();
            sqlServerDataDirectProps.add(properties);
        } else if (database_vendorname.equalsIgnoreCase(BootstrapProperty.DB_SYBASE.getPropertyName())
                   && (database_drivername.equalsIgnoreCase(BootstrapProperty.DB_SYBASE_DRIVER6.getPropertyName())
                       || database_drivername.equalsIgnoreCase(BootstrapProperty.DB_SYBASE_DRIVER7.getPropertyName()))) {
            Properties_sybase properties = new Properties_sybase();
            properties.setDatabaseName(database_name);
            properties.setPassword(database_password1);
            properties.setPortNumber(database_port);
            properties.setUser(database_user1);
            properties.setServerName(database_hostname);
            sybaseProps = new ConfigElementList<Properties_sybase>();
            sybaseProps.add(properties);
        } else {
            throw new Exception("Database or driver not supported");
        }
        Log.exiting(c, "updateDataSourceFromBootstrap");
    }

}

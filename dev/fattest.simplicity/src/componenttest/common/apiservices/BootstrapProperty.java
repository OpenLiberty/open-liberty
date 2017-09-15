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
package componenttest.common.apiservices;

/**
 * This enum contains the valid properties and property keys for the bootstrapping properties file.
 * Property "keys" end with the String "KEY" (ex: HOST_NAME) and are used to identify what entity
 * a property value belongs to.<br>
 * Ex: hostName=machine.ibm.com identifies the name of the machine referenced by key hostName
 */
public enum BootstrapProperty {

    // Properties
    ALIAS("alias"),
    BASE_PRODUCT_VERSION("baseProductVersion"),
    BUILD_DATE("buildDate"),
    BUILD_LEVEL("buildLevel"),
    CELL_NAME("cellName"),
    CLUSTER_NAME("clusterName"),
    CONFIG_PATH("configPath"),
    CONN_TYPE("connType"),
    CONN_PORT("connPort"),
    DEFAULT_ENCODING("defaultEncoding"),
    EXEC_FILE_SUFFIX("execFileSuffix"),
    FILE_SEPARATOR("fileSeparator"),
    FINALIZED("finalized"),
    HOSTNAME("hostname"),
    INSTALL("install"),
    INSTALL_ROOT("installRoot"),
    INSTALL_TYPE("installType"),
    JVM_ARGS("jvmargs"),
    LINE_SEPARATOR("lineSeparator"),
    LOCAL_WSADMIN("local.wsadmin"),
    MACHINE("machine"),
    NODE_NAME("nodeName"),
    OBJECT_NAME("objectName"),
    OS("operatingSystem"),
    OS_VERSION("osVersion"),
    PASSWORD("password"),
    PATH_SEPARATOR("pathSeparator"),
    PLUGIN_CONFIG_PATH("pluginConfigPath"),
    PLUGIN_INSTALL_ROOT("pluginInstallRoot"),
    PORT("port"),
    PORT_HOST("portHost"),
    PROFILE_PATH("profilePath"),
    PROFILE_NAME("profileName"),
    RAW_OS_NAME("rawOSName"),
    ROOT_NODE_HOSTNAME("rootNodeHostname"),
    ROOT_NODE_PROFILE_PATH("rootNodeProfilePath"),
    ROOT_NODE_INSTALL_PATH("rootNodeInstallPath"),
    SECURITY_ENABLED("securityEnabled"),
    SERVER("server"),
    SERVER_NAME("serverName"),
    SERVER_TYPE("serverType"),
    TEMP_DIR("tempDir"),
    TOPOLOGY_TYPE("topology"),
    USER("user"),
    WAS_USERNAME("WASUsername"),
    WAS_PASSWORD("WASPassword"),
    WAS_PRODUCT("wasProduct"),
    WAS_PRODUCT_ID("wasProductId"),
    WAS_PRODUCT_NAME("wasProductName"),
    WAS_VERSION("wasVersion"),
    WEB_SERVER_START_CMD("webServerStartCommand"),
    WEB_SERVER_START_CMD_ARGS("webServerStartCommandArgs"),
    WEB_SERVER_STOP_CMD("webServerStopCommand"),
    WEB_SERVER_STOP_CMD_ARGS("webServerStopCommandArgs"),
    WSADMIN_CONFIG_ID("wsadminConfigId"),

    // Bootstrap Keys
    WAS_CELL_KEY("was.cell."),
    WAS_CLUSTER_KEY("was.cluster."),
    WAS_CLUSTER_MEMBER_KEY("was.cluster.member."),
    WAS_INSTALL_KEY("was.install."),
    WAS_NODE_KEY("was.node."),
    WAS_PRODUCT_KEY("was.product."),
    WAS_SERVER_KEY("was.server."),

    // 134018-start

    WEB_SERVER_HOST_NAME("webserver1.hostName"),
    WEB_SERVER_PORT_NUMBER("webserver1.PortNumber"),
    WEB_SERVER_INSTALL_ROOT("webserver1.InstallRoot"),
    WEB_SERVER_PLUGINS_INSTALL_ROOT("webserver1.PluginsInstallRoot"),
    WEB_SERVER_USERNAME("webserver1.username"),
    WEB_SERVER_PASSWORD("webserver1.password"),

    //134018 -- end

    SERVER_JAVA_HOME("JavaHome"),
    HOST_NAME("hostName"),
    LIBERTY_INSTALL_PATH("libertyInstallPath"),
    HOST_USER("user"),
    HOST_PASSWORD("password"),
    OSGI_PORT("osgi.console"),

    // Bootstrap Keys
    MACHINE_KEY("machine"),
    LIBERTY_INSTALL_KEY("liberty.install"),

    // SSH properties
    KEYSTORE("keystore"),
    PASSPHRASE("passphrase"),

    DATA("data"),
    PROPERTY_SEP("."),

    // Database properties
    // Values needed for each type of database can be found in prereq.dbtest 
    // properties files for databases and jdbc drivers
    LIBERTY_DBJARS("liberty.db_jars"),
    // location of the jdbc jars.  this is set by run-fat-bucket.xml
    DB_HOSTNAME("database.hostname"),
    // name of machine database is located on
    DB_HOME("database.home"),
    // location of the database on the machine
    DB_PORT("database.port"),
    DB_PORT_SECURE("database.port.secure"),
    // database port number
    DB_NAME("database.name"),
    // database name
    DB_USER1("database.user1"),
    // database user 1
    DB_PASSWORD1("database.password1"),
    // database user 1 password
    DB_USER2("database.user2"),
    // database user 2
    DB_PASSWORD2("database.password2"),
    // database user 2 password
    DB_DRIVERNAME("database.drivername"),
    // database jdbc driver name
    DB_DRIVERNAME_DEFAULT("Derby"),
    // database jdbc driver name to default to if not specified in bootstrapping.properties
    DB_DRIVERVERSION("database.driverversion"),
    // database jdbc driver version
    DB_DRIVERVERSION_DEFAULT("10.8"),
    // database jdbc driver version to default to if not specified in bootstrapping.properties
    DB_JDBCJAR("database.jdbcjar"),
    // space separated list of jdbc driver jars
    DB_IFXSERVERNAME("database.servername"),
    // Informix host name
    DB_DBAUSER("database.adminuser"),
    // database dba user
    DB_DBAPASSWORD("database.adminpassword"),
    // database dba user password
    DB_MACHINEUSER("database.machineuser"),
    // database machine user
    DB_MACHINEPWD("database.machinepwd"),
    // database machine user password
    DB_DROPANDCREATE("database.dropandcreate"),
    // drop and create option
    DB_VENDORNAME("database.vendorname"),
    // database vendor name
    DB_VENDORVERSION("database.vendorversion"),
    // database vendor version
    DB_ACCOUNT("database.account"),

    // Database types
    DB_DERBYEMB("DerbyEmbedded"),
    DB_DERBYNET("DerbyNetwork"),
    DB_DB2("DB2"),
    DB_DB2_ISERIES("DB2iSeries"),
    DB_DB2_ZOS("DB2zOS"),
    DB_INFORMIX("Informix"),
    DB_ORACLE("Oracle"),
    DB_SQLSERVER("SQLServer"),
    DB_SYBASE("Sybase"),
    DB_CLOUDANT("Cloudant"),

    // JDBC types, names are taken from Moonstone WFM resources
    DB_DATADIRECT_DRIVER("DataDirect_Connect_for_JDBC"),
    DB_DERBY_EMBEDDED_DRIVER("Derby_Embedded"),
    DB_DERBY_EMBEDDED_DRIVER_40("Derby_Embedded_40"),
    DB_DERBY_NETWORK_CLIENT_DRIVER("Derby_Network_Client"),
    DB_DERBY_NETWORK_CLIENT_DRIVER_40("Derby_Network_Client_40"),
    DB_DB2_JCC_DRIVER("DB2"),
    DB_DB2_INATIVE_DRIVER("DB2_iNative"),
    DB_DB2_ITOOLBOX_DRIVER("DB2_iToolbox"),
    DB_INFORMIX_DRIVER("Informix"),
    DB_ORACLE_DRIVER("Oracle"),
    DB_SQLSERVER_DRIVER("Microsoft_SQL_Server_JDBC_Driver"),
    DB_SYBASE_DRIVER6("jConnect-6_0"),
    DB_SYBASE_DRIVER7("jConnect-7_0");

    public static BootstrapProperty fromPropertyName(String prop) {
        for (BootstrapProperty boot : BootstrapProperty.values()) {
            if (boot.getPropertyName().equalsIgnoreCase(prop)) {
                return boot;
            }
        }
        return null;
    }

    private String propertyName;

    private BootstrapProperty(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * Get the String value of the property that is read from and written to the bootstrapping
     * properties file.
     * 
     * @return The String value of the property
     */
    public String getPropertyName() {
        return this.propertyName;
    }

    @Override
    public String toString() {
        return this.propertyName;
    }
}

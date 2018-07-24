/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.internal;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.rsadapter.AdapterUtil;

/**
 * Contains information about known JDBC drivers.
 */
public class JDBCDrivers {
    private static final TraceComponent tc = Tr.register(JDBCDrivers.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * Constants for the positions of each type of data source within the classNames arrays.
     */
    static final int DATA_SOURCE = 0, CONNECTION_POOL_DATA_SOURCE = 1, XA_DATA_SOURCE = 2, NUM_TYPES = 3;

    /**
     * Ordered map of upper-case key to data source implementation class names.
     * The key is a pattern found in the JAR or ZIP file names for the driver.
     * Value arrays are the following:
     *  : javax.sql.DataSource
     *  : javax.sql.ConnectionPoolDataSource
     *  : javax.sql.XADataSource
     */
    private static final Map<String, String[]> classNamesByKey = new LinkedHashMap<String, String[]>();

    /**
     * Map of factory PID to data source implementation class names.
     * Value arrays are the following:
     *  : javax.sql.DataSource
     *  : javax.sql.ConnectionPoolDataSource
     *  : javax.sql.XADataSource
     */
    private static final Map<String, String[]> classNamesByPID = new HashMap<String, String[]>();

    static {
        String[] classes;

        // DB2 (and Informix) JCC driver
        classes = new String[] {
                                "com.ibm.db2.jcc.DB2DataSource",
                                "com.ibm.db2.jcc.DB2ConnectionPoolDataSource",
                                "com.ibm.db2.jcc.DB2XADataSource"
        };
        classNamesByPID.put("com.ibm.ws.jdbc.dataSource.properties.db2.jcc", classes);
        classNamesByPID.put("com.ibm.ws.jdbc.dataSource.properties.informix.jcc", classes);
        classNamesByKey.put("DB2JCC", classes);

        // Oracle Universal Connection Pooling
        classes = new String[] {
                                "oracle.ucp.jdbc.PoolDataSourceImpl",
                                null,
                                "oracle.ucp.jdbc.PoolXADataSourceImpl"
        };
        classNamesByPID.put("com.ibm.ws.jdbc.dataSource.properties.oracle.ucp", classes);
        classNamesByKey.put("UCP.JAR", classes);

        // Oracle JDBC driver
        classes = new String[] {
                                "oracle.jdbc.pool.OracleDataSource",
                                "oracle.jdbc.pool.OracleConnectionPoolDataSource",
                                "oracle.jdbc.xa.client.OracleXADataSource"
        };
        classNamesByPID.put("com.ibm.ws.jdbc.dataSource.properties.oracle", classes);
        classNamesByKey.put("OJDBC", classes);

        // Microsoft JDBC driver
        classes = new String[] {
                                "com.microsoft.sqlserver.jdbc.SQLServerDataSource",
                                "com.microsoft.sqlserver.jdbc.SQLServerConnectionPoolDataSource",
                                "com.microsoft.sqlserver.jdbc.SQLServerXADataSource"
        };
        classNamesByPID.put("com.ibm.ws.jdbc.dataSource.properties.microsoft.sqlserver", classes);
        classNamesByKey.put("MSSQL-JDBC", classes);
        classNamesByKey.put("SQLJDBC", classes);

        // Informix JDBC driver
        classes = new String[] {
                                "com.informix.jdbcx.IfxDataSource",
                                "com.informix.jdbcx.IfxConnectionPoolDataSource",
                                "com.informix.jdbcx.IfxXADataSource"
        };
        classNamesByPID.put("com.ibm.ws.jdbc.dataSource.properties.informix", classes);
        classNamesByKey.put("IFXJDBC", classes);

        // Derby Network Client
        classes = new String[] {
                                "org.apache.derby.jdbc.ClientDataSource40",
                                "org.apache.derby.jdbc.ClientConnectionPoolDataSource40",
                                "org.apache.derby.jdbc.ClientXADataSource40"
        };
        classNamesByPID.put("com.ibm.ws.jdbc.dataSource.properties.derby.client", classes);
        classNamesByKey.put("DERBYCLIENT.JAR", classes);

        // Derby Embedded
        classes = new String[] {
                                "org.apache.derby.jdbc.EmbeddedDataSource40",
                                "org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource40",
                                "org.apache.derby.jdbc.EmbeddedXADataSource40",
        };
        classNamesByPID.put("com.ibm.ws.jdbc.dataSource.properties.derby.embedded", classes);
        classNamesByKey.put("DERBY", classes);

        // iSeries Toolbox driver
        classes = new String[] {
                                "com.ibm.as400.access.AS400JDBCDataSource",
                                "com.ibm.as400.access.AS400JDBCConnectionPoolDataSource",
                                "com.ibm.as400.access.AS400JDBCXADataSource"
        };
        classNamesByPID.put("com.ibm.ws.jdbc.dataSource.properties.db2.i.toolbox", classes);
        classNamesByKey.put("JT400.JAR", classes);

        // iSeries Native driver
        classes = new String[] {
                                "com.ibm.db2.jdbc.app.UDBDataSource",
                                "com.ibm.db2.jdbc.app.UDBConnectionPoolDataSource",
                                "com.ibm.db2.jdbc.app.UDBXADataSource"
        };
        classNamesByPID.put("com.ibm.ws.jdbc.dataSource.properties.db2.i.native", classes);
        classNamesByKey.put("DB2_CLASSES", classes);

        // MariaDB
        String className = "org.mariadb.jdbc.MySQLDataSource";
        classes = new String[] { className, className, className };
        classNamesByKey.put("MARIADB-JAVA-CLIENT", classes);

        // MySQL before version 6.0
        classes = new String[] {
                                "com.mysql.jdbc.jdbc2.optional.MysqlDataSource",                                
                                "com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource",
                                "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource"
        };
        classNamesByKey.put("MYSQL-CONNECTOR-JAVA-2", classes);
        classNamesByKey.put("MYSQL-CONNECTOR-JAVA-3", classes);
        classNamesByKey.put("MYSQL-CONNECTOR-JAVA-5", classes);
        
        // MySQL after version 6.0
        classes = new String[] {
                                "com.mysql.cj.jdbc.MysqlDataSource",                                
                                "com.mysql.cj.jdbc.MysqlConnectionPoolDataSource",
                                "com.mysql.cj.jdbc.MysqlXADataSource"
        };
        classNamesByKey.put("MYSQL-CONNECTOR-JAVA-", classes);

        // Sybase JDBC 4 driver
        classes = new String[] {
                                "com.sybase.jdbc4.jdbc.SybDataSource",
                                "com.sybase.jdbc4.jdbc.SybConnectionPoolDataSource",
                                "com.sybase.jdbc4.jdbc.SybXADataSource"
        };
        classNamesByKey.put("JCONN4", classes);

        // Sybase JDBC 3 driver
        classes = new String[] {
                                "com.sybase.jdbc3.jdbc.SybDataSource",
                                "com.sybase.jdbc3.jdbc.SybConnectionPoolDataSource",
                                "com.sybase.jdbc3.jdbc.SybXADataSource"
        };
        classNamesByKey.put("JCONN3", classes);

        // Sybase JDBC 2 driver
        classes = new String[] {
                                "com.sybase.jdbc2.jdbc.SybDataSource",
                                "com.sybase.jdbc2.jdbc.SybConnectionPoolDataSource",
                                "com.sybase.jdbc2.jdbc.SybXADataSource"
        };
        classNamesByKey.put("JCONN2", classes);

        // Solid DB
        classes = new String[] {
                                "solid.jdbc.SolidDataSource",
                                "solid.jdbc.SolidConnectionPoolDataSource",
                                "solid.jdbc.xa.SolidXADataSource"
        };
        classNamesByKey.put("SOLIDDRIVER", classes);

        // DataDirect Connect for JDBC / SQL Server
        className = "com.ddtek.jdbcx.sqlserver.SQLServerDataSource";
        classes = new String[] { className, className, className };
        classNamesByPID.put("com.ibm.ws.jdbc.dataSource.properties.datadirect.sqlserver", classes);
        classNamesByKey.put("SQLSERVER.JAR", classes);

        // DataDirect Connect for JDBC / Oracle
        className = "com.ddtek.jdbcx.oracle.OracleDataSource";
        classes = new String[] { className, className, className };
        classNamesByKey.put("ORACLE.JAR", classes);

        // DataDirect SequeLink JDBC driver
        className = "com.ddtek.jdbcx.sequelink.SequeLinkDataSource";
        classes = new String[] { className, className, className };
        classNamesByKey.put("SLJC.JAR", classes);
        
        // PostgreSQL JDBC driver
        classes = new String[] { "org.postgresql.ds.PGSimpleDataSource", 
                                 "org.postgresql.ds.PGConnectionPoolDataSource", 
                                 "org.postgresql.xa.PGXADataSource" };
        classNamesByKey.put("POSTGRESQL", classes);
        
        // H2 Database JDBC driver
        className = "org.h2.jdbcx.JdbcDataSource";
        classes = new String[] { className, className, className };
        classNamesByKey.put("H2-", classes);
    }

    /**
     * Utility method that determines if some text is found as a substring within the contents of a list.
     * 
     * @param list items through which to search.
     * @param value text for which to search.
     * @return true if found, otherwise false.
     */
    private static final boolean contains(Collection<String> list, String value) {
        for (String item : list)
            if (item.contains(value))
                return true;
        return false;
    }

    /**
     * Infer the vendor implementation class name for javax.sql.ConnectionPoolDataSource based on the JAR or ZIP names.
     * A best effort is made for the known vendors.
     * 
     * @param fileNames all upper case names of JAR and/or ZIP files for the JDBC driver.
     * @return name of the vendor implementation class for javax.sql.ConnectionPoolDataSource. Null if unknown.
     */
    static String getConnectionPoolDataSourceClassName(Collection<String> fileNames) {

        for (Map.Entry<String, String[]> entry : classNamesByKey.entrySet())
            if (contains(fileNames, entry.getKey())) {
                String[] classNames = entry.getValue();
                return classNames == null ? null : classNames[1];
            }

        return null;
    }

    /**
     * Infer the vendor implementation class name for javax.sql.ConnectionPoolDataSource based on the PID of the vendor properties.
     * A best effort is made for the known vendors.
     * 
     * @param vendorPropertiesPID factory pid of the JDBC vendor properties.
     * @return name of vendor implementation class for javax.sql.ConnectionPoolDataSource. Null if unknown.
     */
    static String getConnectionPoolDataSourceClassName(String vendorPropertiesPID) {
        String[] classNames = classNamesByPID.get(vendorPropertiesPID);
        return classNames == null ? null : classNames[1];
    }

    /**
     * Infer the vendor implementation class name for javax.sql.DataSource based on the JAR or ZIP names.
     * A best effort is made for the known vendors.
     * 
     * @param fileNames all upper case names of JAR and/or ZIP files for the JDBC driver.
     * @return name of the vendor implementation class for javax.sql.DataSource. Null if unknown.
     */
    static String getDataSourceClassName(Collection<String> fileNames) {

        for (Map.Entry<String, String[]> entry : classNamesByKey.entrySet())
            if (contains(fileNames, entry.getKey())) {
                String[] classNames = entry.getValue();
                return classNames == null ? null : classNames[0];
            }

        return null;
    }

    /**
     * Infer the vendor implementation class name for javax.sql.DataSource based on the PID of the vendor properties.
     * A best effort is made for the known vendors.
     * 
     * @param vendorPropertiesPID factory pid of the JDBC vendor properties.
     * @return name of vendor implementation class for javax.sql.DataSource. Null if unknown.
     */
    static String getDataSourceClassName(String vendorPropertiesPID) {
        String[] classNames = classNamesByPID.get(vendorPropertiesPID);
        return classNames == null ? null : classNames[0];
    }

    /**
     * Infer the vendor implementation class name for javax.sql.XADataSource based on the JAR or ZIP names.
     * A best effort is made for the known vendors.
     * 
     * @param fileNames all upper case names of JAR and/or ZIP files for the JDBC driver.
     * @return name of the vendor implementation class for javax.sql.XADataSource. Null if unknown.
     */
    static String getXADataSourceClassName(Collection<String> fileNames) {

        for (Map.Entry<String, String[]> entry : classNamesByKey.entrySet())
            if (contains(fileNames, entry.getKey())) {
                String[] classNames = entry.getValue();
                return classNames == null ? null : classNames[2];
            }

        return null;
    }

    /**
     * Infer the vendor implementation class name for javax.sql.XADataSource based on the PID of the vendor properties.
     * A best effort is made for the known vendors.
     * 
     * @param vendorPropertiesPID factory pid of the JDBC vendor properties.
     * @return name of vendor implementation class for javax.sql.XADataSource. Null if unknown.
     */
    static String getXADataSourceClassName(String vendorPropertiesPID) {
        String[] classNames = classNamesByPID.get(vendorPropertiesPID);
        return classNames == null ? null : classNames[2];
    }

    /**
     * Infer the vendor implementation class name of the specified type(s) based on the java.sql.Driver.
     * This includes:
     * <li>comparing known types with the java.sql.Driver implementation's package name.
     * <li>TODO swapping Driver --> [type]DataSource and seeing if it loads
     * <li>TODO scanning JAR file for class names that include [type]DataSource
     * 
     * @param loader class loader from which to load JDBC driver classes 
     * @param ordered list of data source types (see type constants in this class) indicating precedence
     * @return name of vendor data source implementation class. Null if unknown.
     */
    static String inferDataSourceClassFromDriver(ClassLoader loader, int... types) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isDebugEnabled())
            Tr.debug(tc, "infer from driver", loader, Arrays.toString(types));

        // TODO gate this method on hidden property
        String[] found = new String[NUM_TYPES];

        // Load JDBC driver class from service registry and use the package to infer the data source class
        ServiceLoader<Driver> serviceLoader = loader == null ? ServiceLoader.load(Driver.class) : ServiceLoader.load(Driver.class, loader);
        if (serviceLoader != null) {
            for (Iterator<Driver> it = serviceLoader.iterator(); found[types[0]] == null && it.hasNext(); ) {
                Driver driver = it.next();
                String driverClassName = driver.getClass().getName();

                // Truncate the deepest subpackage to allow for the possibility that the Driver impl
                // might be in a different subpackage than the data source impls.
                int lastDot = driverClassName.lastIndexOf('.');
                if (lastDot > 0) {
                    int dot = driverClassName.lastIndexOf('.', lastDot - 1);
                    dot = dot <= 10 ? lastDot : dot; // avoid packages that are so short they might be generic (org.apache.) 
                    String driverPackage = driverClassName.substring(0, dot);

                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "infer from " + driverClassName, driverPackage);

                    for (String[] classNames : classNamesByKey.values())
                        for (int type : types)
                            if (classNames[type] != null && classNames[type].startsWith(driverPackage))
                                if (found[type] == null) {
                                    try {
                                        loader.loadClass(classNames[type]);
                                        found[type] = classNames[type];

                                        if (trace && tc.isDebugEnabled())
                                            Tr.debug(tc, "found type " + type + ": " + classNames[type]);

                                        if (type == types[0])
                                            break; // found an impl class of the highest precedence type
                                    } catch (ClassNotFoundException x) {
                                        if (trace && tc.isDebugEnabled())
                                            Tr.debug(tc, classNames[type] + " not found on " + loader);
                                    }
                                }
                }
            }
        }

        for (int type : types)
            if (found[type] != null)
                return found[type];
        return null;
    }
}

/*******************************************************************************
 * Copyright (c) 1997, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.store.db;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;

import javax.sql.DataSource;
import javax.transaction.Transaction;

import com.ibm.ws.LocalTransaction.InconsistentLocalTranException;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.LocalTransaction.RolledbackException;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStatistics;
import com.ibm.ws.session.store.common.BackedHashMap;
import com.ibm.ws.session.store.common.BackedSession;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.session.IStore;

//PK78174 BEGIN
//import com.ibm.wsspi.runtime.service.WsServiceRegistry;
//import com.ibm.ws.runtime.service.Repository;
//PK78174 END

/**
 * This is the "hashtable" implementation for the direct to database session
 * model.
 */

public class DatabaseHashMap extends BackedHashMap {

    static final String varList = "id, propid, appname, listenercnt, lastaccess, creationtime, maxinactivetime, username, small, medium, large";
    static final String TABLE_NAME = "sessions";
    String tableName = TABLE_NAME;
    String dbid;
    String dbpwd;
    private transient DatabaseStoreService databaseStoreService;

    // this is set to true for multirow in DatabaseHashMapMR if additional conditions are satisfied
    boolean appDataTablesPerThread = false;

    //  PK71265
    String delPropall;

    //set to true when initDBSettings completes
    boolean initialized = false;
    boolean tryingToInitialize = false;
    //PK55900 : make sure tableName is only constructed once in initDBSettings()
    boolean firstInitialize = true;

    IStore _iStore;
    SessionManagerConfig _smc;

    static final int SOMEBIGSIZE = 2100000; // Used for missing DataSource
    static final int SMALLCOL_SIZE_DB2 = 3122;
    static final int SMALLCOL_SIZE_DB2_8K = 7218;
    static final int SMALLCOL_SIZE_DB2_16K = 15410;
    static final int SMALLCOL_SIZE_DB2_32K = 31794;
    static final int MEDIUMCOL_SIZE_DB2 = 32700; /* size of a long varchar */
    static final int MEDIUMCOL_SIZE_DB2_ZOS = 28869; /* LIDB2775.25 zOS merge */
    static final int LARGECOL_SIZE_DB2 = 2097152; /* 2M BLOB */

    static final int SMALLCOL_SIZE_ORACLE = 2000;
    static final int MEDIUMCOL_SIZE_ORACLE = 2097152; /* 2M long raw */
    static final int MEDIUMCOL_SIZE_ORACLE_MR = 10485760; /* 10M long raw */
    static final int LARGECOL_SIZE_ORACLE = 1; /* This shouldn't be used, maybe change this to a BLOB */

    static final int SMALLCOL_SIZE_SYBASE = 10485760; /* set to 10M since to force use of small column since no size is associated with a column */
    static final int MEDIUMCOL_SIZE_SYBASE = 1; /* This shouldn't be used */
    static final int LARGECOL_SIZE_SYBASE = 1; /* This shouldn't be used */

    static final int SMALLCOL_SIZE_INFORMIX = 10485760; /* set to 10M since to force use of small column since no size is associated with a column */
    static final int MEDIUMCOL_SIZE_INFORMIX = 1; /* This shouldn't be used */
    static final int LARGECOL_SIZE_INFORMIX = 1; /* This shouldn't be used */

    // a bunch of strings used to construct sql commands
    //All initialization is done in initializeSQL_Strings()
    static final String idCol = "id";
    static final String propCol = "propid";
    static final String appCol = "appname";
    static final String listenCol = "listenercnt";
    static final String lastAccCol = "lastaccess";
    static final String userCol = "username";
    static final String maxInactCol = "maxinactivetime";
    static final String comma = " , ";
    static final String equals = " = ? ";
    static final String smallCol = "small";
    static final String medCol = "medium";
    static final String lgCol = "large";
    static final String upId = " where id = ? and propid = ? and appname = ? ";
    static final String setSmallNull = "small = NULL";
    static final String setMediumNull = "medium = NULL";
    static final String setLargeNull = "large = NULL";

    String insNoProp;
    String insSm;
    String insMed;
    String insLg;
    String getOneNoUpdate;
    String getOneNoUpdateNonDB2;
    String upBase;
    String asyncUpdate;
    String getProp;
    String getPropNotDB2;
    String delProp;
    String insSmProp;
    String insMedProp;
    String insLgProp;
    String selMed;
    String selLg;
    String dropIt;
    String readLastAccess;
    String selectForUpdate;
    String insForInval;
    String delPrimaryRowInval;
    String findAllKeys;
    String readPrimitiveData;
    String readPrimitiveDataDb2;
    String delOne;
    String selDelNoListener;
    String selNukerString;
    String optUpdate;
    String optUpdatePrimRow;
    String remoteInvalAll;

    transient DataSource dataSource; // findbugs for 106329

    int smallColSize = SOMEBIGSIZE;
    int mediumColSize;
    int largeColSize;
    boolean usingOracle = false;

    //For AS400,default is "QEJBSESSON"
    String as400_collection = null; // 93418
    String collectionName = null; //used in as400 and 0S390
    String qualifierNameWhenCustomSchemaIsSet = null; //PM27191
    boolean usingAS400DB2 = false;
    boolean usingSybase = false;
    boolean usingDB2 = false; //*87472
    boolean usingDB2Connect = false; //for DB2Connect(OS390/AS400)
    int dbConnectVersion = 5;
    boolean usingSQLServer = false;
    boolean usingInformix = false;
    boolean usingCloudScape = false;
    boolean usingDerby = false;
    boolean usingDB2zOS = false; // LIDB2775.25 zOS
    boolean usingSolidDB = false;
    boolean usingPostgreSQL = false;
    transient DatabaseHandler dbHandler = null;

    private Hashtable suspendedTransactions = null;
    private static final long serialVersionUID = -4653089886686024589L;
    private static final ComponentMetaDataAccessorImpl cmda = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();

    private static final String methodClassName = "DatabaseHashMap";

    private static final int SET_USER_INFO = 0;
    private static final int INIT_DB_SETTINGS = 1;
    private static final int GET_TABLE_DEFINITION = 2;
    private static final int CREATE_TABLE = 3;
    private static final int GET_DATA_SOURCE = 4;
    private static final int REMOVE_PERSISTED_SESSION = 5;
    private static final int DO_INVALIDATIONS = 6;
    private static final int POLL_FOR_INVALID_SESSIONS_WITH_LISTENERS = 7;
    private static final int CLOSE_RESULT_SET = 8;
    private static final int CLOSE_STATEMENT = 9;
    private static final int CLOSE_CONNECTION = 10;
    private static final int GET_CONNECTION = 11;
    private static final int SUSPEND_TRANSACTION = 12;
    private static final int RESUME_TRANSACTION = 13;
    private static final int GET_VALUE = 14;
    private static final int UPDATE_LAST_ACCESS_TIME = 15;
    private static final int OVERQUAL_LAST_ACCESS_TIME_UPDATE = 16;
    private static final int READ_FROM_EXTERNAL = 17;
    private static final int IS_PRESENT = 18;
    private static final int INSERT_SESSION = 19;
    private static final int HANDLE_PROPERTY_HITS = 20;
    private static final int PERSIST_SESSION = 21;
    private static final int SERIALIZE_APP_DATA = 22;
    private static final int WRITE_CACHED_LAST_ACCESSED_TIMES = 23;
    private static final int GET_COLLECTION_NAME = 24;
    private static final int ORACLE_GET_VALUE = 25;
    private static final int SET_MAX_INACT_TO_ZERO = 26;
    private static final int PERFORM_INVALIDATION = 27;
    private static final int PROCESS_INVALID_LISTENERS = 28;
    private static final int UPDATE_NUKER_TIME_STAMP = 29;
    private static final int BEGIN_DB_CONTEXT = 30;
    private static final int DOES_INDEX_EXISTS_DISTRIBUTED = 31;
    private static final int DOES_INDEX_EXISTS_ISERIES = 32;
    private static final int IS_TABLE_MARKED_VOLATILE = 33;

    private static final String methodNames[] = { "setUserInfo", "initDBSettings", "getTableDefinition", "createTable", "getDataSource",
                                                 "removePersistedSession", "doInvalidations", "pollForInvalidSessionsWithListeners", "closeResultSet", "closeStatement",
                                                 "closeConnection", "getConnection", "suspendTransaction", "resumeTransaction", "getValue",
                                                 "updateLastAccessTime", "overQualLastAccessTimeUpdate", "readFromExternal", "isPresent", "insertSession",
                                                 "handlePropertyHits", "persistSession", "serializeAppData", "writeCachedLastAccessedTimes", "getCollectionName",
                                                 "oracleGetValue", "setMaxInactToZero", "performInvalidation", "processInvalidListeners", "updateNukerTimeStamp",
                                                 "beginDBContext", "doesIndexExistsDistributed", "doesIndexExistsISeries", "isTableMarkedVolatile" };

    /*
     * Constructor
     */
    public DatabaseHashMap(IStore store, SessionManagerConfig smc, DatabaseStoreService databaseStoreService) {
        super(store, smc);
        this.databaseStoreService = databaseStoreService;
        _iStore = store;
        _smc = smc;
        if (smc.getTableNameValue() != null) {
            tableName = smc.getTableNameValue();
        }
        suspendedTransactions = new Hashtable();
        getDataSource();
        initDBSettings();
    }

    /*
     * getAppDataTablesPerThread - returns the boolean
     * only true for mulitrow db if other conditions are met - see constructor for DatabaseHashMapMR
     */
    @Override
    public boolean getAppDataTablesPerThread() {
        return appDataTablesPerThread;
    }

    protected DatabaseStoreService getDatabaseStoreService() {
        return this.databaseStoreService;
    }

    /*
     * stores userid/passwd information
     */
    void setUserInfo() {
        String smcDBID = _smc.getSessionDBID();
        if (smcDBID != null && smcDBID.indexOf("::") != -1) {
            try {
                dbid = smcDBID.substring(0, smcDBID.indexOf("::"));
                collectionName = smcDBID.substring(smcDBID.indexOf("::") + 2);
                if (collectionName.indexOf("$V") > 0) {
                    collectionName = collectionName.substring(0, collectionName.indexOf("$V") - 1);
                    String dbConnectVersionName = smcDBID.substring(smcDBID.indexOf("$V") + 2);
                    dbConnectVersion = (int) (new Double(dbConnectVersionName)).doubleValue();
                }
            } catch (Exception e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.store.db.DatabaseHashMap.setUserInfo", "241", this);
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[SET_USER_INFO], "CommonMessage.exception", e);
            }
        } else
            dbid = smcDBID;

        dbpwd = _smc.getSessionDBPWD();
    }

    /*
     * Initializes database settings
     */
    private void initDBSettings() {
        synchronized (this) {

            Connection tbcon = null;
            String url = null;
            try {
                setUserInfo();
                //passing true because we're doing initDBSettings
                tbcon = getConnection(true);
                if (tbcon == null)
                    return;
                DatabaseMetaData dmd = tbcon.getMetaData();

                // cmd pok_PQ97422 rollup
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    String dbProductName = dmd.getDatabaseProductName();
                    String dbProductVersion = dmd.getDatabaseProductVersion();
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[INIT_DB_SETTINGS], "dbProductName: " + dbProductName);
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[INIT_DB_SETTINGS], "dbProductVersion: " + dbProductVersion);
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[INIT_DB_SETTINGS], "dbUserName: " + dmd.getUserName());
                } // cmd end of pok_PQ97422 rollup

                int dbCode = DBPortability.getDBCode(dmd);
                if (dbCode == DBPortability.ORACLE) {
                    smallColSize = SMALLCOL_SIZE_ORACLE;
                    
                    if (_smc.isUsingMultirow())
                        mediumColSize = MEDIUMCOL_SIZE_ORACLE_MR;
                    else
                        mediumColSize = MEDIUMCOL_SIZE_ORACLE;
                    
                    largeColSize = LARGECOL_SIZE_ORACLE;
                    usingOracle = true;

                } else if (dbCode == DBPortability.SYBASE) {
                    smallColSize = SMALLCOL_SIZE_SYBASE;
                    mediumColSize = MEDIUMCOL_SIZE_SYBASE;
                    largeColSize = LARGECOL_SIZE_SYBASE;
                    usingSybase = true;
                } else if (dbCode == DBPortability.MICROSOFT_SQLSERVER) {
                    //SQLSERVER and SYBASE behaves the sameway  for binary objects
                    smallColSize = SMALLCOL_SIZE_SYBASE;
                    mediumColSize = MEDIUMCOL_SIZE_SYBASE;
                    largeColSize = LARGECOL_SIZE_SYBASE;
                    usingSQLServer = true;
                } else if (dbCode == DBPortability.INFORMIX) {
                    smallColSize = SMALLCOL_SIZE_INFORMIX;
                    mediumColSize = MEDIUMCOL_SIZE_INFORMIX;
                    largeColSize = LARGECOL_SIZE_INFORMIX;
                    usingInformix = true;
                } else if (dbCode == DBPortability.CLOUDSCAPE) {
                    smallColSize = SMALLCOL_SIZE_INFORMIX;
                    mediumColSize = MEDIUMCOL_SIZE_INFORMIX;
                    largeColSize = LARGECOL_SIZE_INFORMIX;
                    usingCloudScape = true;
                } else if (dbCode == DBPortability.POSTGRESQL) {
                    dbHandler = new PostgreSQLHandler();
                    smallColSize = dbHandler.getSmallColumnSize();
                    mediumColSize = dbHandler.getMediumColumnSize();
                    largeColSize = dbHandler.getLargeColumnSize();
                    usingPostgreSQL = true;
                } else if (dbCode == DBPortability.MYSQL) {
                    dbHandler = new MySQLHandler();
                    smallColSize = dbHandler.getSmallColumnSize();
                    mediumColSize = dbHandler.getMediumColumnSize();
                    largeColSize = dbHandler.getLargeColumnSize();
                } //HANDLING DERBY BELOW SO AS TO NOT CHANGE THE BEHAVIOR
                else {
                    // venkat 89199 increased row size capability
                    if (_smc.getRowSize() == 4) {
                        smallColSize = SMALLCOL_SIZE_DB2;
                    } else if (_smc.getRowSize() == 8) {
                        smallColSize = SMALLCOL_SIZE_DB2_8K;
                    } else if (_smc.getRowSize() == 16) {
                        smallColSize = SMALLCOL_SIZE_DB2_16K;
                    } else if (_smc.getRowSize() == 32) {
                        smallColSize = SMALLCOL_SIZE_DB2_32K;
                    }
                    mediumColSize = MEDIUMCOL_SIZE_DB2;
                    largeColSize = LARGECOL_SIZE_DB2;
                    usingDB2 = true;

                    //For SolidDB, which is a subset of DB2
                    if (dbCode == DBPortability.SOLIDDB) {
                        usingSolidDB = true;
                    }
                    //for OS390
                    if (dbCode == DBPortability.DB2_CONNECT) {
                        smallColSize = SMALLCOL_SIZE_DB2;
                        usingDB2Connect = true;
                        // usingDB2 = false; cmd PK03943 - shouldn't set to false here
                    }
                    // LIDB2775.25 zSO start block
                    else if (dbCode == DBPortability.DB2_zOS) {
                        smallColSize = SMALLCOL_SIZE_DB2;
                        mediumColSize = MEDIUMCOL_SIZE_DB2_ZOS;
                        usingDB2Connect = true;
                        usingDB2zOS = true;
                    } // LIDB2775.25 zOS end block

                    //For AS400
                    else if (dbCode == DBPortability.DB2_AS400) {
                        smallColSize = SMALLCOL_SIZE_DB2;
                        mediumColSize = 28898; //d154211
                        usingAS400DB2 = true;

                        url = dmd.getURL();
                    } else if (dbCode == DBPortability.DERBY) {
                        //treat like db2 except for volatile
                        usingDB2 = false;
                        usingDerby = true;
                    }
                }

            } catch (SQLException se) {
                com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.initConnPool", "344", this);
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[INIT_DB_SETTINGS], "DatabaseHashMap.createTableError");
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[INIT_DB_SETTINGS], "CommonMessage.exception", se);
            } finally {
                if (tbcon != null)
                    closeConnection(tbcon);
            }

            if (usingAS400DB2 && firstInitialize) { //added firstInitialize for PK55900
                // if (collectionName == null) //PK78174  so we won't overwrite the collectionName set at setUserInfo
                //     collectionName = getCollectionName(url);
                // tableName = collectionName + "." + tableName;
                if (collectionName == null) { //PK78174  so we won't overwrite the collectionName set at setUserInfo
                    int index = tableName.indexOf(".");
                    if (index != -1) { // SessionTableName = "schema_name.table_name"
                        collectionName = tableName.substring(0, index);
                    } else {
                        collectionName = getCollectionName(url);
                        tableName = collectionName + "." + tableName;
                    }
                } else {
                    tableName = collectionName + "." + tableName;
                }
                firstInitialize = false;
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[INIT_DB_SETTINGS], "AS400DB2 Table Name value = ", tableName);
                }
            } else if (usingDB2zOS) { // LIDB2775.25 zOS
                if (_smc.getTableNameValue() != null) {
                    tableName = _smc.getTableNameValue(); // LIDB2775.25 zOS
                }
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[INIT_DB_SETTINGS], "DB2 Table Name value = ", tableName);
                }
            } else if (usingDB2Connect && firstInitialize) {
                tableName = collectionName + "." + tableName;
                firstInitialize = false;
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[INIT_DB_SETTINGS], "DB2Connect Table Name value = ", tableName);
                }
            }
            initializeSQL_Strings();
            if (!SessionManagerConfig.is_zOS()) {
                createTable();
            }
        }

        initialized = true;

    }

    /*
     * Get the session table definition if exists.It also checks
     * If existing table matches what session manager is looking for
     */
    private boolean getTableDefinition(Connection tbcon) throws SQLException {
        boolean defExists = false;
        boolean smallExists = false;
        boolean mediumExists = false;
        boolean largeExists = false;

        //informix size calculation doesn't
        //work. i.e rs1.getInt("COLUMN_SIZE") doesn't work
        //if (usingInformix)
        //    return false;

        DatabaseMetaData dmd = tbcon.getMetaData();
        String tbName = tableName;
        String qualifierName = null;

        if (usingAS400DB2 || usingDB2Connect) {
            //tbName = TABLE_NAME.toUpperCase();
            int index = tableName.indexOf(".");
            if (index != -1) {
                tbName = tableName.substring(index + 1).toUpperCase();
            } else {
                tbName = tableName.toUpperCase();
            }
            if (collectionName != null)
                qualifierName = collectionName;
        } else if (usingDB2 || usingDerby || usingOracle) { // cmd 162172
            tbName = tbName.toUpperCase();
            if (dbid != null) {
                qualifierName = dbid.toUpperCase(); // cmd PQ81615
            }

            if (_smc.isUsingCustomSchemaName()) { //PM27191
                if (usingDB2) {
                    java.sql.Statement s = null;
                    ResultSet rs1 = null;
                    s = tbcon.createStatement();
                    s.execute("VALUES (CURRENT SCHEMA)");
                    rs1 = s.getResultSet();
                    while (rs1.next()) {
                        qualifierNameWhenCustomSchemaIsSet = rs1.getString("1");
                    }
                    if (qualifierNameWhenCustomSchemaIsSet != null) {
                        qualifierName = qualifierNameWhenCustomSchemaIsSet;
                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_TABLE_DEFINITION],
                                                                "Database being used is DB2 and UsingCustomSchemaName is set to true. The following qualifier name obtained from " +
                                                                                "running the query VALUES (CURRENT SCHEMA) will be used for subsequent queries in this method: "
                                                                                + qualifierNameWhenCustomSchemaIsSet);
                        }
                    }
                } // Oracle case to be handled later
            } //PM27191 END
        } else if (usingPostgreSQL) {
            qualifierName = dmd.getUserName();
        }
        
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_TABLE_DEFINITION], "Qualifier Name = " + qualifierName + " Table Name = " + tbName);
        }
	
        ResultSet rs1 = dmd.getColumns(null, qualifierName, tbName, "%");
        try {
            while (rs1.next()) {
                String columnname = rs1.getString("COLUMN_NAME");
                int columnsize = rs1.getInt("COLUMN_SIZE");
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_TABLE_DEFINITION], "COLUMN_NAME = " + columnname + " COLUMN_SIZE = " + Integer.toString(columnsize));
                }
                if (columnname.equalsIgnoreCase("SMALL")) {
                    smallColSize = columnsize;
                    smallExists = true;
                }
                if (columnname.equalsIgnoreCase("MEDIUM")) {
                    if (!usingOracle) { // cmd 162172 long raw or Blob
                                        // does not give proper size from
                                        // COLUMN_SIZE data for Oracle
                        mediumColSize = columnsize;
                    }
                    mediumExists = true;
                }
                if (columnname.equalsIgnoreCase("LARGE")) {
                    largeColSize = columnsize;
                    largeExists = true;
                }
                defExists = true;
            }

            if (defExists) {
                if (smallExists && mediumExists && largeExists) {
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_TABLE_DEFINITION], "Table exists with all the required columns");
                    }
                } else {
                    //Flag the error
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_TABLE_DEFINITION], "DatabaseHashMap.wrongTableDef");
                }
            }
        } finally {
            closeResultSet(rs1);
        }
        return defExists;
    }

    /*
     * For creating the table
     */
    void createTable() {
        Connection con = null;
        java.sql.Statement s = null;

        //PK55900 We can pass in true to the getConnection method since this method,
        //createTable, is only called from the initDBSettings method.
        con = getConnection(true);
        if (con == null) {
            return;
        }

        try {

            try {
                s = con.createStatement();
                if (!getTableDefinition(con)) {
                    if (usingOracle) {
                        if (_smc.isUseOracleBlob()) { // cmd LI1963 start
                            s.executeUpdate("create table "
                                            + tableName
                                            + " (id varchar(128) not null, propid varchar(128) not null, appname varchar(128) not null, listenercnt smallint, lastaccess integer, creationtime integer, maxinactivetime integer, username varchar(256), small raw("
                                            + SMALLCOL_SIZE_ORACLE + "), medium BLOB, large raw(1))");
                        } else {
                            s.executeUpdate("create table "
                                            + tableName
                                            + " (id varchar(128) not null, propid varchar(128) not null, appname varchar(128) not null, listenercnt smallint, lastaccess integer, creationtime integer, maxinactivetime integer, username varchar(256), small raw("
                                            + SMALLCOL_SIZE_ORACLE + "), medium long raw, large raw(1))");
                        } // cmd LI1963 end
                    } else if (usingAS400DB2) {
                        try { //if using AS400, create collection first
                            s.executeUpdate("CREATE COLLECTION " + collectionName);
                        } catch (Exception e) {
                            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.store.db.DatabaseHashMap.createTable", "470", con);
                        }
                        s.executeUpdate("create table "
                                        + tableName
                                        + " (id varchar(128) not null, propid varchar(128) not null, appname varchar(128) not null, listenercnt smallint, lastaccess bigint, creationtime bigint, maxinactivetime integer, username varchar(256), small varchar("
                                        + SMALLCOL_SIZE_DB2 + ") for bit data, medium long varchar for bit data, large BLOB(2M))");
                    } else if (usingSybase) {
                        s.executeUpdate("create table "
                                        + tableName
                                        + " (id varchar(128) not null, propid varchar(128) not null, appname varchar(128) not null, listenercnt smallint null, lastaccess numeric(21,0) null, creationtime numeric(21,0) null, maxinactivetime numeric(10,0) null, username varchar(255) null, small image null, medium image null, large image null)");
                    } else if (usingSQLServer) {
                        s.executeUpdate("create table "
                                        + tableName
                                        + " (id varchar(128) not null, propid varchar(128) not null, appname varchar(128) not null, listenercnt smallint null, lastaccess decimal(21,0) null, creationtime decimal(21,0) null, maxinactivetime integer null, username varchar(255) null, small image null, medium image null, large image null)");
                    } else if (usingInformix) {
                        s.executeUpdate("create table "
                                        + tableName
                                        + " (id varchar(128) not null, propid varchar(128) not null, appname varchar(128) not null, listenercnt smallint, lastaccess int8, creationtime int8, maxinactivetime integer, username varchar(255), small BYTE, medium BYTE, large BYTE)");
                    } else if (usingCloudScape) {
                        s.executeUpdate("create table "
                                        + tableName
                                        + " (id varchar(128) not null, propid varchar(128) not null, appname varchar(128) not null, listenercnt smallint, lastaccess bigint, creationtime bigint, maxinactivetime integer, username varchar(255), small LONG VARBINARY , medium char(1) , large char(1))");
                    } else if (dbHandler != null) {
                        dbHandler.createTable(s, tableName);
                    } else if (!usingDB2Connect && !usingDB2zOS) { // LIDB2775.25 zOS
                        String tableSpaceName = " ";
                        // a little overkill for this test
                        String configTableSpaceName = _smc.getTableSpaceName();
                        if (configTableSpaceName != null && !configTableSpaceName.equals("") && configTableSpaceName.length() != 0)
                            tableSpaceName = " in " + configTableSpaceName;
                        if (usingSolidDB)
                            s.executeUpdate("create table "
                                        + tableName
                                        + " (id varchar(128) not null, propid varchar(128) not null, appname varchar(128) not null, listenercnt smallint, lastaccess bigint, creationtime bigint, maxinactivetime integer, username varchar(256), small varchar("
                                        + smallColSize + "), medium long varchar, large BLOB(2M)) " + tableSpaceName);
                        else
                            s.executeUpdate("create table "
                                        + tableName
                                        + " (id varchar(128) not null, propid varchar(128) not null, appname varchar(128) not null, listenercnt smallint, lastaccess bigint, creationtime bigint, maxinactivetime integer, username varchar(256), small varchar("
                                        + smallColSize + ") for bit data, medium long varchar for bit data, large BLOB(2M)) " + tableSpaceName);
                    }
                }
                //            } catch (com.ibm.ejs.cm.portability.TableAlreadyExistsException eee) {
                //                // Do nothing since all is well!!
                //                com.ibm.ws.ffdc.FFDCFilter.processException(eee, "com.ibm.ws.session.store.db.DatabaseHashMap.createTable", "495", this);
            } catch (SQLException err) {
                com.ibm.ws.ffdc.FFDCFilter.processException(err, "com.ibm.ws.session.store.db.DatabaseHashMap.createTable", "497", this);
                if (!usingCloudScape) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[CREATE_TABLE], "CommonMessage.exception", err);
                } else {
                    if (err.getErrorCode() != 30000)
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[CREATE_TABLE], "CommonMessage.exception", err);
                }
            }

            if (!_smc.isSessionTableSkipIndexCreation()) { // PM37139

                //Creating index in a seperate step as we have seen customers missing this
                //step when they create the table manually
                try {
                    if (s == null)
                        s = con.createStatement();
                    if (usingSybase) {
                        s.executeUpdate("create unique index sess_index on " + tableName + " (id, propid, appname)");
                        s.executeUpdate("alter table sessions lock datarows");
                    } else if (usingSolidDB) {
                        s.executeUpdate("create unique index sess_index on " + tableName + " (id, propid, appname)");
                    } else if (usingAS400DB2) {
                        mediumColSize = mediumColSize - 2; //d154211
                        //PK56991: If multiple cluster members startup at the same time the
                        // combination of these DDLs can lock up for a substantial duration.
                        // We now execute these only if they do not exist.
                        if (!doesIndexExists(con, "sess_index")) {
                            s.executeUpdate("create unique index " + collectionName + ".sess_index on " + tableName + " (id,propid,appname)"); //tableName is already in the form of "collectionName.tableName"
                            //PK56991 comment out marking table as volatile. The DB2 chief architect on iSeries
                            //team informed that VOLATILE clause is only compatible and improve performance on LUW
                            //s.executeUpdate("alter table " + tableName + " volatile");
                        } else if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[CREATE_TABLE], "Skip index creation");
                        }
                    } else if (dbHandler != null) {
                        dbHandler.createIndex(con, s, tableName);
                    } else if (!usingDB2Connect && !usingDB2zOS) { // LIDB2775.25
                        //PK56991: Refer to detailed comment above
                        if (!doesIndexExists(con, "sess_index")) {
                            s.executeUpdate("create unique index sess_index on " + tableName + " (id, propid, appname)");//PK86373: changed sesscmd_index to sess_index
                        } else if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[CREATE_TABLE], "Skip index creation");
                        }
                        //PK56991: We are marking the the session table as volatile if it is not
                        //done yet.
                        if (usingDB2 && !isTableMarkedVolatile(con)) {
                            s.executeUpdate("alter table " + tableName + " volatile");
                        } else if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[CREATE_TABLE], "Skip marking table volatile");
                        }

                    }
                } catch (SQLException err) {
                    //Don't do any thing if it is only index already exists
                    //err.printStackTrace();
                    com.ibm.ws.ffdc.FFDCFilter.processException(err, "com.ibm.ws.session.store.db.DatabaseHashMap.createTable", "526", con);
                }
            } // END PM37139
        } finally {
            if (s != null)
                closeStatement(s);
                closeConnection(con); //findbugs for 106329
        }
    }

    /*
     * Get the datasource
     */
    DataSource getDataSource() {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[GET_DATA_SOURCE]);
        }

        if (dataSource != null)
            return dataSource;

        try {

            //            Properties p = new Properties();
            //
            //            p.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
            //            InitialContext ic = new javax.naming.InitialContext(p);
            beginDBContext(); // PK06395/d321615
            //            dataSource = (DataSource) ic.lookup(_smc.getJNDIDataSourceName());

            ResourceConfig rc = this.getDatabaseStoreService().getResourceConfigFactory().createResourceConfig("javax.sql.DataSource");
            rc.setResAuthType(ResourceConfig.AUTH_CONTAINER);
            rc.setSharingScope(ResourceConfig.SHARING_SCOPE_SHAREABLE);
            rc.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
            //            ResRef.APPLICATION, // resAuth - org.eclipse.jst.j2ee.common.ResAuthTypeBase.APPLICATION=
            //            ResRef.SHAREABLE, // resSharingScope - org.eclipse.jst.j2ee.common.ResSharingScopeType.SHAREABLE
            //            ResRef.TRANSACTION_READ_COMMITTED); // resIsolationLevel - com.ibm.ejs.models.base.extensions.commonext.IsolationLevelKind.TRANSACTION_READ_COMMITTED
            dataSource = (DataSource) this.getDatabaseStoreService().getDataSourceFactory().createResource(rc);

            //            dataSource = (DataSource)SessionMgrComponentImpl.getDataSourceFactory().createResource(null); // direct JNDI lookup
            endDBContext(); // PK06395/d321615
            return dataSource;

        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.store.db.DatabaseHashMap.getDataSource", "558", this);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_DATA_SOURCE], "DatabaseHashMap.dataSrcErr");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_DATA_SOURCE], "CommonMessage.exception", e);
            dataSource = null;
        }
        return null;
    }

    /*
     * actually remove a session from the database
     */
    protected void removePersistedSession(String id) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[REMOVE_PERSISTED_SESSION], id);
        }
        Connection con;
        PreparedStatement ps = null;
        boolean psClose = false;

        //If the app calls invalidate, it may not be removed from the local cache yet.
        superRemove(id);

        con = getConnection(false);
        if (con == null) {
            return;
        }
        try {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[REMOVE_PERSISTED_SESSION], "before upd " + id);
            }
            ps = con.prepareStatement(delOne);
            ps.setString(1, id);
            ps.setString(2, _iStore.getId());
            ps.executeUpdate();
            ps.close();
            psClose = true;
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[REMOVE_PERSISTED_SESSION], "after upd " + id);
            }
            addToRecentlyInvalidatedList(id);
        } catch (SQLException se) {
            //            if (isStaleConnectionException(se)) {
            //                com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.removePersistedSession", "619", id);
            //            } else {
            com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.removePersistedSession", "621", id);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[REMOVE_PERSISTED_SESSION], "DatabaseHashMap.removeSessionsError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[REMOVE_PERSISTED_SESSION], "CommonMessage.exception", se);
            //            }
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.store.db.DatabaseHashMap.removePersistedSession", "626", id);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[REMOVE_PERSISTED_SESSION], "DatabaseHashMap.removeSessionsError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[REMOVE_PERSISTED_SESSION], "CommonMessage.exception", e);
        } finally {
            if (!psClose && ps != null)
                closeStatement(ps);
            closeConnection(con);
        }
    }

    /*
     * this method removes timed out sessions that do not require listener processin
     */
    void doInvalidations(Connection nukerCon) throws SQLException {
        //create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[DO_INVALIDATIONS]);
        }
        String id = null;
        long lastAccessTime = 0;
        long createTime = 0;
        ResultSet rs = null;
        PreparedStatement delNukeStatement = null;
        PreparedStatement scanPropPs = null;
        PreparedStatement delNuke2 = null;
        PreparedStatement subDel = null;
        ResultSet rs2 = null;
        boolean delClose = false;
        boolean scanClose = false;
        boolean subClose = false;
        boolean del2Close = false;

        try {
            int oldIsolationLevel = 0;

            long now = System.currentTimeMillis();
            String appName = getIStore().getId();

            String selDelNoListener = this.selDelNoListener;

            if (usingDB2 || usingDerby) {
                selDelNoListener = selDelNoListener + " for read only";
                oldIsolationLevel = nukerCon.getTransactionIsolation();
                nukerCon.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_UNCOMMITTED);
            }

            delNukeStatement = nukerCon.prepareStatement(selDelNoListener);
            delNukeStatement.setString(1, appName);
            setPSLong(delNukeStatement, 2, now);

            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "executing read/uncom scan");
            }

            rs = delNukeStatement.executeQuery();

            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "after uncom query ");
            }

            //get all the candidates eligible for invalidation
            Hashtable primaryRows = new Hashtable();
            Hashtable creationTimes = new Hashtable();
            while (rs.next()) {
                id = rs.getString(1);
                lastAccessTime = rs.getLong(2);
                createTime = rs.getLong(3);
                primaryRows.put(id, new Long(lastAccessTime));
                creationTimes.put(id, new Long(createTime));
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "executing read/uncom scan, recording " + id);
                }
                if ((now + _smc.getInvalidationCheckInterval() * 1000) <= System.currentTimeMillis()) {
                    //If the scan is taking more than pollInterval,just break and
                    //invalidate the sessions so far determined
                    break;
                }
            }

            rs.close();
            delNukeStatement.close();
            delClose = true;

            /*
             * PK71265: Remove the section retrieving attribute rows as session manager
             * will remove the attribute rows in a single query.
             *
             *
             * if (isTraceOn&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
             * LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "before read of ucom scan results ");
             * }
             *
             * Enumeration enum1 = primaryRows.keys();
             *
             *
             * Hashtable multiRowProps = new Hashtable();
             * while (_smc.isUsingMultirow() && enum1.hasMoreElements()) {
             * scanPropPs = null;
             * scanPropPs = nukerCon.prepareStatement(findAllKeys);
             * id = (String) enum1.nextElement();
             * scanPropPs.setString(1, id);
             * scanPropPs.setString(2, appName);
             * rs2 = scanPropPs.executeQuery();
             * Vector propVec = new Vector();
             * while (rs2.next()) {
             * String propid = rs2.getString(propCol);
             * propVec.addElement(propid);
             * if (isTraceOn&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
             * LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "executing read/uncom scan," + "recording for " + id + " prop " +
             * propid);
             * LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "propVec currently " + propVec.toString());
             * }
             * }
             * multiRowProps.put(id, propVec);
             * if (isTraceOn&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
             * LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "ht currently " + multiRowProps.toString());
             * }
             * rs2.close();
             * scanPropPs.close();
             * scanClose = true;
             * }
             *
             * if (isTraceOn&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
             * LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "after read of ucom scan results, befor commit ");
             * }
             */

            if (usingDB2) {
                nukerCon.setTransactionIsolation(oldIsolationLevel);
            }

            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "delete primary row vector " + primaryRows);
                if (primaryRows != null) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "with size reported " + primaryRows.size());
                }
            }
            //loop through each eligible candidate
            Enumeration enum1 = primaryRows.keys();
            int deleteCount = 0;
            BackedSession pmiStatSession = null; // fake session for pmi stats
            SessionStatistics pmiStats = null;
            while (enum1.hasMoreElements()) {
                id = (String) enum1.nextElement();
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "current index into delete primary row vector " + id);
                }

                now = System.currentTimeMillis();
                long lastAccess = ((Long) primaryRows.get(id)).longValue();
                delNuke2 = nukerCon.prepareStatement(delPrimaryRowInval);
                delNuke2.setString(1, id);
                delNuke2.setString(2, appName);
                setPSLong(delNuke2, 3, lastAccess);

                if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "delete for " + id);
                }

                int rc = delNuke2.executeUpdate();
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "after del of  " + id);
                }

                delNuke2.close();
                del2Close = true;

                if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "num of rows deleted " + rc);
                }

                if (rc > 0) {
                    //delete sub rows
                    if (_smc.isUsingMultirow()) {

                        /*
                         * PK71265 : Rather than removing the associated rows which persists the attributes individually.
                         * We will remove all associated rows in one query for an invalid non-listeners sessions, which
                         * is the same way we handle assoicated rows for invalid listeners sessions
                         *
                         * Vector vec2 = (Vector) multiRowProps.get(id);
                         * if (isTraceOn&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                         * LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "delete vector for sub rows " + vec2);
                         * if (vec2 != null) {
                         * LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "delete vector for sub rows " + " with reported size " +
                         * vec2.size());
                         * }
                         * }
                         * if (vec2 != null) {
                         * for (int i2 = 0; i2 < vec2.size(); i2++) {
                         * if (isTraceOn&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                         * LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "current index in delete sub rows is " + i2);
                         * }
                         * String propid = (String) vec2.elementAt(i2);
                         * subDel = nukerCon.prepareStatement(delProp);
                         * subDel.setString(1, id);
                         * subDel.setString(2, propid);
                         * subDel.setString(3, appName);
                         * if (isTraceOn&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                         * LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "before deleting subrow " + propid + " for session " +
                         * id);
                         * }
                         *
                         * subDel.executeUpdate();
                         * subDel.close();
                         * subClose = true;
                         * }
                         * }
                         * }
                         *
                         * }
                         */
                        //PK71265 starts
                        subDel = nukerCon.prepareStatement(delPropall);
                        subDel.setString(1, id);
                        subDel.setString(2, appName);

                        int rc1 = subDel.executeUpdate();
                        subDel.close();
                        subClose = true;

                        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "DatabaseHashMap:doInvalidations - for session: " + id +
                                                                                                                            " appName: " + appName + "number of rows deleted: "
                                                                                                                            + rc1);
                        }
                        //PK71265 ends

                    } //if (_smc.isUsingMultirow())
                }// if (rc>0)

                /*
                 * We already did cleanUpCache during the invalidation processing. Therefore, the session is no longer
                 * in our cache, and superRemove should return null. We need to create a temporary Session with an
                 * accurate Id and creationTime to send to the PMI counters.
                 */
                superRemove(id);
                createTime = ((Long) creationTimes.get(id)).longValue();
                //we don't want to retrieve session, so use a fake one for pmi
                if (pmiStatSession == null)
                    pmiStatSession = new DatabaseSession();
                if (pmiStats == null)
                    pmiStats = _iStore.getSessionStatistics();
                pmiStatSession.setId(id);
                pmiStatSession.setCreationTime(createTime);
                if (pmiStats != null) {
                    pmiStats.sessionDestroyed(pmiStatSession);
                    pmiStats.sessionDestroyedByTimeout(pmiStatSession);
                }
                deleteCount++;

            }
        } catch (SQLException se) {
            //            if (isStaleConnectionException(se)) {
            //                com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.doInvalidations", "867", this);
            //                if (isTraceOn&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            //                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_INVALIDATIONS], "StaleConnectionException");
            //                }
            //                throw se;
            //            } else {
            com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.doInvalidations", "872", this);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[DO_INVALIDATIONS], "DatabaseHashMap.doInvalidationsError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[DO_INVALIDATIONS], "CommonMessage.sessionid", id);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[DO_INVALIDATIONS], "CommonMessage.exception", se);
            throw se;
            //            }
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.store.db.DatabaseHashMap.doInvalidations", "877", this);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[DO_INVALIDATIONS], "DatabaseHashMap.doInvalidationsError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[DO_INVALIDATIONS], "CommonMessage.exception", e);
        } finally {
            if (!delClose && delNukeStatement != null) {
                closeResultSet(rs);
                closeStatement(delNukeStatement);
            }
            if (!scanClose && scanPropPs != null) {
                closeResultSet(rs2);
                closeStatement(scanPropPs);
            }
            if (!del2Close && delNuke2 != null) {
                closeStatement(delNuke2);
            }
            if (!subClose && subDel != null) {
                closeStatement(subDel);
            }
        }
    }

    /*
     * this method determine the set of sessions with session listeners which
     * need to be invalidated
     */
    Enumeration pollForInvalidSessionsWithListeners(Connection nukerCon) throws SQLException {
        //create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[POLL_FOR_INVALID_SESSIONS_WITH_LISTENERS]);
        }
        String appName = getIStore().getId(); // bhSessionContext.getAppName();
        Hashtable ret = new Hashtable();
        String id = null;
        PreparedStatement selectNukerStatement = null;
        ResultSet rs = null;
        boolean statementClose = false;
        try {
            long now = System.currentTimeMillis();

            String selNukerString = this.selNukerString;

            if (usingDB2 || usingDerby)
                selNukerString = selNukerString + " for read only";

            int oldIsolationLevel = 0;
            // to reduce shared locks on select, set auto commit on

            //To reduce the shared locks happening on select, temporarily change isolation level
            if (usingDB2 || usingDerby) {
                oldIsolationLevel = nukerCon.getTransactionIsolation();
                nukerCon.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_UNCOMMITTED);
            }

            selectNukerStatement = nukerCon.prepareStatement(selNukerString);
            selectNukerStatement.setString(1, appName);
            setPSLong(selectNukerStatement, 2, now);

            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[POLL_FOR_INVALID_SESSIONS_WITH_LISTENERS], "exec query for " + appName);
            }

            rs = selectNukerStatement.executeQuery();

            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[POLL_FOR_INVALID_SESSIONS_WITH_LISTENERS], "after query for " + appName
                                                                                                                                        + " processing results");
            }

            while (rs.next()) {

                id = rs.getString(1);
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[POLL_FOR_INVALID_SESSIONS_WITH_LISTENERS], "processing " + id);
                }
                BackedSession s = new DatabaseSession(this, id, _iStore.getStoreCallback()); //(DatabaseSession)getSessionWrapper(id);

                s.initSession(_iStore);
                s.setIsValid(true);
                s.setIsNew(false);
                s.updateLastAccessTime(rs.getLong(2));
                s.setCreationTime(rs.getLong(3));
                s.internalSetMaxInactive(rs.getInt(4));
                s.internalSetUser(rs.getString(5));
                s.setListenerFlag(rs.getShort(6));

                ret.put(id, s);
            }

            rs.close();
            selectNukerStatement.close();
            statementClose = true;

            if (usingDB2) {
                nukerCon.setTransactionIsolation(oldIsolationLevel);
            }

            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[POLL_FOR_INVALID_SESSIONS_WITH_LISTENERS], "close/commit ");
            }
        } catch (SQLException se) {
            //            if (isStaleConnectionException(se)) {
            //                com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.pollForInvalids", id, nukerCon);
            //                if (isTraceOn&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            //                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[POLL_FOR_INVALID_SESSIONS_WITH_LISTENERS], "StaleConnectionException");
            //                }
            //                throw se;
            //            } else {
            com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.pollForInvalids", id, nukerCon);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[POLL_FOR_INVALID_SESSIONS_WITH_LISTENERS], "DatabaseHashMap.pollForInvalidsError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[POLL_FOR_INVALID_SESSIONS_WITH_LISTENERS], "CommonMessage.sessionid", id);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[POLL_FOR_INVALID_SESSIONS_WITH_LISTENERS], "CommonMessage.exception", se);
            throw se;
            //            }
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.store.db.DatabaseHashMap.pollForInvalids", id, nukerCon);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[POLL_FOR_INVALID_SESSIONS_WITH_LISTENERS], "DatabaseHashMap.pollForInvalidsError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[POLL_FOR_INVALID_SESSIONS_WITH_LISTENERS], "CommonMessage.exception", e);
        } finally {
            if (!statementClose && selectNukerStatement != null) {
                closeResultSet(rs);
                closeStatement(selectNukerStatement);
            }
        }

        return ret.elements();
    }

    /*
     * Close the resultset
     */
    static void closeResultSet(ResultSet rs) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[CLOSE_RESULT_SET], "closing " + rs);
        }
        try {
            rs.close();
        } catch (Throwable t) {
            com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.session.store.db.DatabaseHashMap.closeResultSet", "1018");
        }
    }

    /*
     * Close the statement
     */
    static void closeStatement(Statement ps) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[CLOSE_STATEMENT], "closing " + ps);
        }
        try {
            ps.close();
        } catch (Throwable t) {
            com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.session.store.db.DatabaseHashMap.closeStatement", "1031");
        }
    }

    /*
     * close the connection
     */
    void closeConnection(Connection con) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[CLOSE_CONNECTION], "closing " + con);
        }

        try {
            con.close();
        } catch (Throwable t) {
            com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.session.store.db.DatabaseHashMap.closeConnection", "1056", con);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[CLOSE_CONNECTION], "CommonMessage.exception", t);
        }

        endDBContext(); // PK06395/d321615
        resumeTransaction();
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[CLOSE_CONNECTION], "closed " + con);
        }
    }

    /*
     * Get connection
     *
     * @param fromInit - true if we were called from initDBSettings (this prevents an infinite loop)
     */
    Connection getConnection(boolean fromInit) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[GET_CONNECTION]);
        }
        Connection conn = null;
        boolean exceptionOccured = false;

        if (dataSource == null)
            getDataSource();

        if (dataSource != null) {

            //Try getting connection thrice when stale connection happens
            //if it fails, give it up
            int tries = 0;
            suspendTransaction();
            beginDBContext(); // PK06395/d321615
            while (tries < _smc.getConnectionRetryCount()+1) {
                try {
                    tries++;
                    exceptionOccured = false;
                    //
                    //		            if (!SessionManagerConfig.is_zOS()) {	  // LIDB2775.25 zOS
                    //                        conn = dataSource.getConnection(dbid, dbpwd);
                    //		            } else {
                    // in liberty profile, depend on data source for user/password
                    conn = dataSource.getConnection(); // LIDB2775.25 zOS

                    if (dbHandler != null && !dbHandler.isConnectionValid(conn)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_CONNECTION], "Stale connection detected.");
                        continue;
                    }

                    //		            }
                    conn.setAutoCommit(true);
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
                        LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[GET_CONNECTION], "Connection-isolation-level" + conn.getTransactionIsolation());
                    }
                    //if we did not successully initialize the database settings due to the database being down on startup, do it now
                    //fromInit tells us if we came from initDBSettings: We don't want to call this the first time when initDBSettings calls getConnection
                    if (!initialized && !fromInit && !tryingToInitialize) {
                        synchronized (this) {
                            tryingToInitialize = true;
                            if (!initialized) {
                                initDBSettings();
                            }
                            tryingToInitialize = false;
                        }
                    }
                    return conn;

                } catch (Throwable th) {
                    exceptionOccured = true;
                    //                    if (isStaleConnectionException(th)) {
                    //                        com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.session.store.db.DatabaseHashMap.getConnection", "1081", this);
                    //                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    //                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[UPDATE_LAST_ACCESS_TIME], "StaleConnectionException");
                    //                        }
                    //                        continue; // try up to 3 times
                    //                    } else {
                    //                        exceptionOccured = true;
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_CONNECTION], "CommonMessage.exception", th);
                    //                        break; // bail out now
                    //                    }
                    continue; // try up to 3 times
                }
            }

        } else {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.WARNING, methodClassName, methodNames[GET_CONNECTION], "DatabaseHashMap.getConnectionError");
        }
        if (exceptionOccured) {
            endDBContext(); // PK06395/d321615
            resumeTransaction();
        }
        return null;

    }

    /*
     * Suspend any global transaction
     */
    protected void suspendTransaction() {
        //create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[SUSPEND_TRANSACTION]);
        }
        Object[] suspendedTx = { null, null }; // PM56632
        //        UOWCurrent uowCurrent = TransactionManagerFactory.getUOWCurrent();
        UOWCurrent uowCurrent = this.getDatabaseStoreService().getUOWCurrent();
        UOWCoordinator uowCoord = uowCurrent.getUOWCoord();
        //        LocalTransactionCurrent ltCurrent = TransactionManagerFactory.getLocalTransactionCurrent(); //d120870.2
        LocalTransactionCurrent ltCurrent = this.getDatabaseStoreService().getLocalTransactionCurrent();

        if (uowCoord == null) {
            // There is no Global or Local Transaction
            // We must start a LocalTransaction
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[SUSPEND_TRANSACTION], "No Global or Local Transaction exists");
            }
        } else if (!uowCoord.isGlobal()) {
            // There is a LocalTransaction, it must be suspended
            // and a new LocalTransaction begun
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[SUSPEND_TRANSACTION], "LocalTransaction is active so suspend");
            }
            // Suspend the LocalTransaction and return the LocalTransactionCoordinator as an Object
            suspendedTx[0] = (Object) ltCurrent.suspend();

        } else {
            // else we have a global transaction
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[SUSPEND_TRANSACTION], "Global Transaction is Active");
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[SUSPEND_TRANSACTION], "Global Transaction is Active so suspend");
            }
            try {
                //                suspendedTx = TransactionManagerFactory.getTransactionManager().suspend();
                suspendedTx[0] = this.getDatabaseStoreService().getEmbeddableWebSphereTransactionManager().suspend();
            } catch (Throwable th) {
                // Only required as JTA specifies exception, but implementation never throws one
            }
            // START: PM56632 need to check if there is a local transaction since global suspend does not suspend local transaction
            uowCoord = this.getDatabaseStoreService().getUOWCurrent().getUOWCoord();
            if (uowCoord != null && !uowCoord.isGlobal()) {
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[SUSPEND_TRANSACTION], "LocalTransaction is active after global suspend");
                }
                // Suspend the LocalTransaction and return the LocalTransactionCoordinator as an Object
                suspendedTx[1] = (Object) ltCurrent.suspend();
            }
            // END: PM56632
        }

        if (suspendedTx[0] != null || suspendedTx[1] != null)
            suspendedTransactions.put(Thread.currentThread(), suspendedTx);

        try {
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[SUSPEND_TRANSACTION], "Starting a Local Transaction");
            }
            //            ltCurrent.begin();
            boolean boundaryIsAS = false; // true if the boundary is ActivitySession; false if the boundary is BeanMethod
            boolean unresActionIsCommit = false; // true if the unresolved action is commit; false if it is rollback
            boolean resolverIsCAB = false; // true if the resolver is ContainerAtBoundary; false if it is Application
            //          The following Starts a new LTC scope and associates it with the current thread.
            //          The configuration of the LTC is determined by the caller rather than via J2EE component metadata.
            //          Throws IllegalStateException if the LocalTransactionCoordinator is not in a
            //          valid state to execute the operation, for example if a global transaction is active.
            ltCurrent.begin(boundaryIsAS, unresActionIsCommit, resolverIsCAB);
        } catch (Exception ex) {
            // Absorb any exception to ensure we still exit cleanly and resume
            // any suspended LTC in postInvoke.
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[SUSPEND_TRANSACTION], "DatabaseHashMap.cantstartLTC");
        }

        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[SUSPEND_TRANSACTION]);
        }
    }

    /**
     * Resume any suspended transaction
     */
    protected void resumeTransaction() {
        //create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[RESUME_TRANSACTION]);
        }
        //        LocalTransactionCurrent ltCurrent = TransactionManagerFactory.getLocalTransactionCurrent(); //d120870.2
        LocalTransactionCurrent ltCurrent = this.getDatabaseStoreService().getLocalTransactionCurrent();
        LocalTransactionCoordinator coord = ltCurrent.getLocalTranCoord();
        if (coord != null) {
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[RESUME_TRANSACTION], "Complete the Local Transaction");
            }

            try {
                // Clean-up the Tx
                coord.cleanup();
            } catch (InconsistentLocalTranException ex) {
                // Absorb any exception from cleanup - it doesn't really
                // matter if there are inconsistencies in cleanup.
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[RESUME_TRANSACTION], "InconsistentLocalTranException", ex);
            } catch (RolledbackException rbe) {
                // We need to inform the user that completion
                // was affected by a call to setRollbackOnly
                // so rethrow as a ServletException.
                //
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[RESUME_TRANSACTION], "DatabaseHashMap.localRollBack", rbe);
            }
        }

        Object[] suspendedTx = (Object[]) suspendedTransactions.remove(Thread.currentThread()); // PM56632
        if (suspendedTx != null) {
            for (int i = suspendedTx.length - 1; i >= 0; i--) { // LTC resume, then global transaction
                Object susTrans = suspendedTx[i];
                if (susTrans != null) {
                    if (susTrans instanceof Transaction) {
                        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[RESUME_TRANSACTION], "Resume the suspended Global Transaction");
                        }

                        Transaction tx = (Transaction) susTrans;

                        try { // remove null check findbugs 106329
                            //                        TransactionManagerFactory.getTransactionManager().resume(tx);
                            this.getDatabaseStoreService().getEmbeddableWebSphereTransactionManager().resume(tx);
                        } catch (Throwable ex) {
                            // Absorb all possible JTA resume exceptions
                            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.session.store.db.DatabaseHashMap.resumeGlobalTransaction", "1210", this);
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[RESUME_TRANSACTION], "CommonMessage.exception", ex);
                        }

                    } else if (susTrans instanceof LocalTransactionCoordinator) {
                        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[RESUME_TRANSACTION], "Resume the suspended Local Transaction");
                        }
                        try {
                            coord = (LocalTransactionCoordinator) susTrans;
                            ltCurrent.resume(coord);
                        } catch (IllegalStateException ex) {
                            // We must be running under a received global tran.
                            // We should never have needed to suspend an LTC in preInvoke under
                            // these circumstances but the up-chain webapp may have started
                            // a global tran by a back-door route. Take a relaxed
                            // approach and just end the LTC, which is what should
                            // have happened when the global tran was started.
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[RESUME_TRANSACTION], "IllegalStateException", ex);
                            try {
                                // Clean-up the Tx
                                coord.cleanup();
                            } catch (InconsistentLocalTranException iltex) {
                                // Absorb any exception from cleanup - it doesn't really
                                // matter if there are inconsistencies in cleanup.
                                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[RESUME_TRANSACTION], "InconsistentLocalTranException", iltex);
                            } catch (RolledbackException rbe) {
                                // We need to inform the user that completion
                                // was affected by a call to setRollbackOnly
                                // so rethrow as a ServletException.
                                //
                                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[RESUME_TRANSACTION], "DatabaseHashMap.localRollBack", rbe);
                            }
                        }
                    }
                }
            }
        } // PM56632

        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[RESUME_TRANSACTION]);
        }
    }

    protected boolean available(InputStream in) throws java.io.IOException {
        if (usingSQLServer) {
            return true;
        } else {
            return in.available() > 0;
        }
    }

    /*
     * To load all the properties for a given session
     * Overridden in multi-row case
     */
    protected Object getAllValues(BackedSession sess) {
        //called by BackedSession.getSwappableListeners
        return getValue(sess.getId(), sess);
    }

    /*
     * this method supports the session.getValue()
     */
    protected Object getValue(String id, BackedSession s) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[GET_VALUE]);
        }
        Connection conn = null;
        PreparedStatement prop_stmt = null;
        ResultSet rs = null;
        boolean psClose = false;
        String sessId = s.getId();

        Object tmp = null;
        conn = getConnection(false);

        if (conn == null) {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[GET_VALUE], "unable to get con when processing session " + sessId + " for prop " + id);
            }
            return null;
        }

        try {

            InputStream is_large;
            InputStream is_medium;
            InputStream is_small;

            BufferedInputStream bis_small;
            BufferedInputStream bis_medium;
            BufferedInputStream bis_large;

            rs = null;

            if (usingDB2 || usingDerby)
                prop_stmt = conn.prepareStatement(getProp);
            else
                prop_stmt = conn.prepareStatement(getPropNotDB2);

            prop_stmt.setString(1, sessId);
            prop_stmt.setString(2, id);
            prop_stmt.setString(3, getIStore().getId());

            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_VALUE], "before query for " + id + " session " + sessId);
            }

            rs = prop_stmt.executeQuery();

            if (!rs.next()) {
                rs.close();
                prop_stmt.close();
                psClose = true;
                return null;
            }

            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_VALUE], "read results after query (FOUND) for " + id + " session " + sessId);
            }

            //Oracle Behaviour:
            // 1)Returns non null stream for long raw even when no data is present. This
            //   voilates JDBC spec. we have bug report into Oracle. Bug id is BUG#1255136
            // 2)Also we cannot use available() for long raw to check to see if any data is present
            //  or not. JDBC doesn't manadate to implement available().
            //
            // Once, Oracle fixes 1), we can use getBinaryStream() which makes coding easy
            //       by not creating byte array and then again creating binary stream from it.
            if (usingOracle) {
                tmp = oracleGetValue(rs, s);
            } else {
                long startTime = System.currentTimeMillis();
                long readSize = 0;
                is_small = rs.getBinaryStream(smallCol);
                boolean found = false;

                if (is_small != null) {
                    if (available(is_small)) {
                        readSize = is_small.available();
                        bis_small = new BufferedInputStream(is_small);
                        try {
                            tmp = ((DatabaseStore) getIStore()).getLoader().loadObject(bis_small);
                        } catch (ClassNotFoundException ce) {
                            com.ibm.ws.ffdc.FFDCFilter.processException(ce, "com.ibm.ws.session.store.db.DatabaseHashMap.getValue", "1335", s);
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_VALUE], "BackedHashtable.classNotFoundError");
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_VALUE], "CommonMessage.sessionid", id);
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_VALUE], "CommonMessage.exception", ce);
                        }
                        bis_small.close();
                        is_small.close();
                        found = true;

                    } else
                        is_small.close();
                }

                if (!found) {
                    is_medium = rs.getBinaryStream(medCol);
                    if (is_medium != null) {
                        if (available(is_medium)) {
                            readSize = is_medium.available();
                            bis_medium = new BufferedInputStream(is_medium);
                            try {
                                tmp = ((DatabaseStore) getIStore()).getLoader().loadObject(bis_medium);
                            } catch (ClassNotFoundException ce) {

                                com.ibm.ws.ffdc.FFDCFilter.processException(ce, "com.ibm.ws.session.store.db.DatabaseHashMap.getValue", "1360", s);
                                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_VALUE], "BackedHashtable.classNotFoundError");
                                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_VALUE], "CommonMessage.sessionid", id);
                                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_VALUE], "CommonMessage.exception", ce);
                            }
                            bis_medium.close();
                            is_medium.close();
                            found = true;
                        } else
                            is_medium.close();
                    }
                }

                //  Only look in the large column for DB2; PM54094: ensure large column not check by Sybase

                if (!found && !usingSybase) {
                    /*
                     * PK77100.
                     * Issue: the semantics expected by WAS from the java.io.InputStream method available() and that provided by the JDBC driver
                     * implementation differ. After session manager executes the query to retrieve an existing object from the database,
                     * session manager has a mechanism to validate whether there is any data remaining for an InputStream by checking
                     * java.io.InputStream.available(). From the JDBC driver standpoint this means the number of bytes currently available
                     * from this InputStream that have already been brought over to the client side but not yet consumed by the application.
                     * When fullyMaterializeLobData=false, no data is initially brought over to the client-side with the SELECT, just the locator.
                     * Initially (i.e. before the first read), available() will return 0. There are no data bytes available that don't require a
                     * blocking read (i.e. flow to the engine) to acquire
                     *
                     * Solution: Calling ResultSet.getBlob() will result into the materialization to the client-side. The subsequent usage of
                     * Blob.length()determines there is any data retrieving from the LARGE Column
                     */
                    //is_large = rs.getBinaryStream(lgCol);

                    Blob getBlobData = rs.getBlob(lgCol);

                    if (getBlobData != null) {
                        if (getBlobData.length() > 0) {
                            is_large = getBlobData.getBinaryStream();

                            if (is_large != null) {
                                //if (available(is_large)) {    // commented out by PK77100
                                //readSize = is_large.available(); // commented out by PK77100
                                readSize = getBlobData.length();
                                bis_large = new BufferedInputStream(is_large);
                                try {
                                    tmp = ((DatabaseStore) getIStore()).getLoader().loadObject(bis_large);
                                } catch (ClassNotFoundException ce) {

                                    com.ibm.ws.ffdc.FFDCFilter.processException(ce, "com.ibm.ws.session.store.db.DatabaseHashMap.getValue", "1387", s);
                                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_VALUE], "BackedHashtable.classNotFoundError");
                                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_VALUE], "CommonMessage.sessionid", id);
                                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_VALUE], "CommonMessage.exception", ce);
                                }
                                bis_large.close();
                                is_large.close();
                                found = true;
                            }//if (is_large != null)
                             //} else   //commented out by PK77100
                             //  is_large.close();  // commented out by PK77100, we only require to close the Binary Stream if the stream is available
                        }//if (getBlobData.length()){
                    }//if (getBlobData!=null)
                } // if (!found)
                SessionStatistics pmiStats = _iStore.getSessionStatistics();
                if (pmiStats != null) {
                    pmiStats.readTimes(readSize, System.currentTimeMillis() - startTime);
                }
            }

            rs.close();
            prop_stmt.close();
            psClose = true;
        } catch (SQLException se) {
            //            if (isStaleConnectionException(se)) {
            //                com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.getValue", "1408", s);
            //                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.WARNING, methodClassName, methodNames[GET_VALUE], "StaleConnectionException");
            //            } else {
            com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.getValue", "1411", s);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_VALUE], "DatabaseHashMap.getValueErrBH");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_VALUE], "CommonMessage.sessionid", sessId + " " + id);
            // spit out sesisonid + property name
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_VALUE], "CommonMessage.exception", se);
            //            }
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.store.db.DatabaseHashMap.getValue", "1417", s);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_VALUE], "DatabaseHashMap.getValueErrBH");
            // PQ87534  LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodName, "CommonMessage.object", s.toString());
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_VALUE], "CommonMessage.exception", e);

        } finally {
            if (!psClose && prop_stmt != null) {
                closeResultSet(rs);
                closeStatement(prop_stmt);
            }
            closeConnection(conn);
        }

        return tmp;
    }

    /*
     * Update the lastAccessedTime in the db
     */
    protected int updateLastAccessTime(BackedSession sess, long nowTime) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[UPDATE_LAST_ACCESS_TIME]);
        }
        Connection con = getConnection(false);
        if (con == null)
            return 1;

        PreparedStatement s = null;
        String id = sess.getId();
        int rowsRet = 0;
        try {
            s = con.prepareStatement(asyncUpdate);
            setPSLong(s, 1, nowTime);
            s.setString(2, id);
            s.setString(3, id);
            s.setString(4, getIStore().getId()); //getId returns the appName

            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[UPDATE_LAST_ACCESS_TIME], "before lastacc upd for " + id);
            }

            rowsRet = s.executeUpdate();
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                if (rowsRet > 0) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[UPDATE_LAST_ACCESS_TIME], "after upd (row changed " + id);
                } else {
                    //Ooops..someother jvm  has invalidated this
                    //reset the value, so that concurrent access to this session won't try
                    //update the database again.
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[UPDATE_LAST_ACCESS_TIME], "row does not exist for " + id);
                }
            }

        } catch (Throwable th) {
            //            if (isStaleConnectionException(th)) {
            //                com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.session.store.db.DatabaseHashMap.updateLastAccessTime", "1472", sess);
            //                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            //                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[UPDATE_LAST_ACCESS_TIME], "StaleConnectionException");
            //                }
            //            } else {
            com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.session.store.db.DatabaseHashMap.updateLastAccessTime", "1476", sess);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[UPDATE_LAST_ACCESS_TIME], "CommonMessage.sessionid", id);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[UPDATE_LAST_ACCESS_TIME], "CommonMessage.exception", th);
            //            }
        } finally {
            if (s != null) {
                closeStatement(s);
            }
            closeConnection(con); //findbugs 106329
        }

        return rowsRet;
    }

    /*
     * overQualLastAccessTimeUpdate
     * Attempts to update the last access time ensuring the old value matches.
     * This verifies that the copy we have in cache is still valid.
     */
    protected int overQualLastAccessTimeUpdate(BackedSession sess, long nowTime) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer(" lastAccessTime= ");
            sb.append(sess.getCurrentAccessTime());
            sb.append("; nowTime= ").append(nowTime);
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[OVERQUAL_LAST_ACCESS_TIME_UPDATE], sb);
        }
        Connection con = null;
        PreparedStatement s = null;
        String id = sess.getId();
        int rowsRet = 1; //Assume that it will be cache hit.
        try {

            // PQ76937 BEGIN
            synchronized (sess) {
                con = getConnection(false);
                if (con == null) {
                    return 1;
                    /*
                     * REG if the database goes down, we will not update the lastAccessTime because we still want to find a match
                     * when the database does come back up. I do see a problem if the database goes down, we keep hitting a session
                     * and then because we haven't updated the lastAccessTime, our invalidator thread invalidates the session.
                     */
                }

                s = con.prepareStatement(optUpdatePrimRow);
                setPSLong(s, 1, nowTime);
                s.setString(2, id);
                s.setString(3, id);
                s.setString(4, sess.getAppName());
                //the currentAccessTime has not been updated so this is really the lastAccessTime until we call updateLastAccessTime
                setPSLong(s, 5, sess.getCurrentAccessTime());
                rowsRet = s.executeUpdate();

                if (rowsRet > 0) {
                    sess.updateLastAccessTime(nowTime);
                }
            }
            // PQ76937 END

            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                if (rowsRet > 0) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[OVERQUAL_LAST_ACCESS_TIME_UPDATE], "after upd (row changed " + id);
                } else {
                    //It might be either cache hit or session is not in cache
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[OVERQUAL_LAST_ACCESS_TIME_UPDATE],
                                                        "It might be either cache hit or session is not in cache for  " + id);
                }
            }
        } catch (Throwable th) {
            //           if (isStaleConnectionException(th)) {
            //               com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.session.store.db.DatabaseHashMap.overQualLastAccessTimeUpdate", "1527", sess);
            //               if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            //                   LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[OVERQUAL_LAST_ACCESS_TIME_UPDATE], "StaleConnectionException");
            //               }
            //           } else {
            com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.session.store.db.DatabaseHashMap.overQualLastAccessTimeUpdate", "1531", sess);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[OVERQUAL_LAST_ACCESS_TIME_UPDATE], "CommonMessage.sessionid", id);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[OVERQUAL_LAST_ACCESS_TIME_UPDATE], "CommonMessage.exception", th);
            //           }
        } finally {
            if (s != null) {
                closeStatement(s);
            }
            if (con != null) {
                closeConnection(con);
            }
        }

        return rowsRet;
    }

    /*
     * readFromExternal - for DB we create a DatabaseSession object containing only
     * the primitives, no application data
     */
    protected BackedSession readFromExternal(String id) {

        PreparedStatement s = null;
        String appName = getIStore().getId();
        Connection con = getConnection(false);

        if (con == null) {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[READ_FROM_EXTERNAL], "unable to get con when processing session " + id);
            }
            return null;
        }

        DatabaseSession sess = null;

        try {
            sess = readPrimitives(id, appName, con);
        } catch (Throwable th) {
            //            if (isStaleConnectionException(th)) {
            //                com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.session.store.db.DatabaseHashMap.readFromExternal", "1608", sess);
            //                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.WARNING, methodClassName, methodNames[READ_FROM_EXTERNAL], "StaleConnectionException");
            //            } else {
            com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.session.store.db.DatabaseHashMap.readFromExternal", "1611", sess);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[READ_FROM_EXTERNAL], "DatabaseHashMap.selectAndLockError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[READ_FROM_EXTERNAL], "CommonMessage.sessionid", id);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[READ_FROM_EXTERNAL], "CommonMessage.exception", th);
            //            }
        } finally {
            if (s != null) {
                closeStatement(s);
            }
            closeConnection(con); //findbugs 106329
        }
        return sess;
    }

    /*
     * Reads given session with primitives, but does not read attribute data
     */
    DatabaseSession readPrimitives(String id, String appName, Connection con) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        DatabaseSession sess = null;
        try {
            if (usingDB2 || usingDerby) {
                ps = con.prepareStatement(readPrimitiveDataDb2);
            } else {
                ps = con.prepareStatement(readPrimitiveData);
            }
            ps.setString(1, id);
            ps.setString(2, id);
            ps.setString(3, appName);

            rs = ps.executeQuery();
            if (rs.next()) {
                sess = new DatabaseSession(this, id, ((DatabaseStore) getIStore()).getStoreCallback());
                long lastaccess = rs.getLong(1);
                long createTime = rs.getLong(2);
                int maxInact = rs.getInt(3);
                String userName = rs.getString(4);
                short listenerflag = rs.getShort(5);

                sess.updateLastAccessTime(lastaccess);
                sess.setCreationTime(createTime);
                sess.internalSetMaxInactive(maxInact);
                sess.internalSetUser(userName);
                sess.setIsValid(true);
                sess.setListenerFlag(listenerflag);
            }
        } finally {
            if (rs != null)
                closeResultSet(rs);
            if (ps != null)
                closeStatement(ps);
        }

        return sess;
    }

    /*
     * Determines if the input session id already exists in the database. If it is
     * found to be in use, we'll reuse the same id for the new session in a different app.
     */
    protected boolean isPresent(String id) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[IS_PRESENT]);
        }
        if (isPresentInRecentlyInvalidatedList(id))
            return true;

        PreparedStatement s = null;
        ResultSet rs = null;
        Connection con = null;
        boolean sessionIdInUse = false;

        con = getConnection(false);

        if (con == null) {
            return false;
        }

        try {

            if (usingDB2 || usingDerby) {
                s = con.prepareStatement(getOneNoUpdate);
            } else {
                s = con.prepareStatement(getOneNoUpdateNonDB2);
            }

            s.setString(1, id);
            s.setString(2, id);

            rs = s.executeQuery();

            if (rs.next()) {
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
                    LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[IS_PRESENT], "Found sessionid in Database");
                }
                sessionIdInUse = true;
            }
            return sessionIdInUse;
        } catch (SQLException se) {
            //            if (isStaleConnectionException(se)) {
            //                com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.isPresent", "1709", this);
            //                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            //                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[IS_PRESENT], "StaleConnectionException");
            //                }
            //            } else {
            com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.isPresent", "1715", this);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[IS_PRESENT], "DatabaseHashMap.selectNoUpdateError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[IS_PRESENT], "CommonMessage.sessionid", id);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[IS_PRESENT], "CommonMessage.exception", se);
            //            }
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.store.db.DatabaseHashMap.isPresent", "1720", this);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[IS_PRESENT], "DatabaseHashMap.selectNoUpdateError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[IS_PRESENT], "CommonMessage.sessionid", id);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[IS_PRESENT], "CommonMessage.exception", e);
        } finally {
            if (rs != null)
                closeResultSet(rs);
            if (s != null)
                closeStatement(s);

            closeConnection(con);
        }

        return false;
    }

    /*
     * Insert the new session - should not already exist
     */
    protected void insertSession(BackedSession d2) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[INSERT_SESSION]);
        }
        d2.update = new StringBuffer();
        Connection conn = null;
        PreparedStatement ps = null;
        boolean psClose = false;

        conn = getConnection(false);

        if (conn == null) {
            return;
        }

        try {

            d2.update.append(insNoProp);
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[INSERT_SESSION], "updating Primary Row - NO properties");
            }

            ps = conn.prepareStatement(d2.update.toString());
            String id = d2.getId();

            ps.setString(1, id);
            ps.setString(2, id);
            ps.setString(3, d2.getAppName());

            listenerFlagUpdate(d2);

            ps.setShort(4, d2.listenerFlag);
            long tmpCreationTime = d2.getCreationTime();
            d2.setLastWriteLastAccessTime(tmpCreationTime);
            setPSLong(ps, 5, tmpCreationTime);
            setPSLong(ps, 6, tmpCreationTime);

            ps.setInt(7, d2.getMaxInactiveInterval());
            ps.setString(8, d2.getUserName());

            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[INSERT_SESSION], "before upd " + d2.update.toString() + " for sess " + id);
            }

            ps.executeUpdate();
            d2.needToInsert = false;
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[INSERT_SESSION], "after upd " + d2.update.toString() + " for sess " + id);
            }

            ps.close();
            psClose = true;
            removeFromRecentlyInvalidatedList(d2.getId());
        } catch (SQLException se) {
            //            if (isStaleConnectionException(se)) {
            //                com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.insertSession", "1794", d2);
            //                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.WARNING, methodClassName, methodNames[INSERT_SESSION], "StaleConnectionException");
            //            } else if (isDuplicateKeyException(se)) {
            //                com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.insertSession", "1797", d2);
            //                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            //                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[INSERT_SESSION], "Duplicate Key Exception");
            //                }
            //                d2.duplicateIdDetected = true;
            //            } else {
            com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.insertSession", "1803", d2);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[INSERT_SESSION], "DatabaseHashMap.ejbCreateError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[INSERT_SESSION], "CommonMessage.object", d2.toString());
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[INSERT_SESSION], "CommonMessage.miscData", "  Update SQL " + d2.update);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[INSERT_SESSION], "CommonMessage.exception", se);
            //            }
        } catch (Exception ee) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ee, "com.ibm.ws.session.store.db.DatabaseHashMap.insertSession", "1810", d2);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[INSERT_SESSION], "DatabaseHashMap.ejbCreateError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[INSERT_SESSION], "CommonMessage.exception", ee);
        } finally {
            if (!psClose && ps != null) {
                closeStatement(ps);
            }
            closeConnection(conn); //findbugs 106329
        }

        d2.update = null;
        d2.userWriteHit = false;
        d2.maxInactWriteHit = false;
        d2.listenCntHit = false;
    }

    /*
     * isDuplicateKeyException
     * Determine whether the input SQL Exception represents a duplicate key error
     */
    //    private boolean isDuplicateKeyException(SQLException sqle) {
    //        boolean rc = false;
    //        if (sqle instanceof com.ibm.websphere.ce.cm.DuplicateKeyException) {
    ////        if (sqle instanceof com.ibm.ejs.cm.portability.DuplicateKeyException) {
    //            // the above exception is what we get with the legacy WAS 4 datasource
    //            rc = true;
    //        } else {
    ////            since the dataSource is wrapped by a proxy, this check will no longer work.  Need to investigate/test.
    ////            if  (dataSource instanceof com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource &&
    ////                    ((com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource)dataSource).getDataStoreHelper().isDuplicateKey(sqle)) {
    ////                // if we have a new datasource, then isDuplicateKey will figure out if the exception represents a duplicate key error
    ////                // it could be the new JDBC 4.0 exception SQLIntegrityContraintViolationException
    ////                rc = true;
    ////            }
    //        }
    //        return rc;
    //    }

    /*
     * isStaleConnectionException
     * Determine whether the input throwable represents a stale connection error
     */
    //    protected boolean isStaleConnectionException(Throwable th) {
    //        boolean rc = false;
    //        if (th instanceof com.ibm.websphere.ce.cm.StaleConnectionException) {
    //            // the above exception is what we get before JDBC 4.0..it is thrown by legacy and new datasources
    //            rc = true;
    //        } else {
    ////          since the dataSource is wrapped by a proxy, this check will no longer work.  Need to investigate/test.
    ////            if  (dataSource instanceof com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource &&
    ////                    th instanceof SQLException &&
    ////                    ((com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource)dataSource).getDataStoreHelper().isConnectionError((SQLException)th)) {
    ////                // if we have a new datasource, then isConnectionError will detect if we have one of
    ////                // the new JDBC 4.0 exceptions, such as SQLRecoverableException, that represents a stale connection error
    ////                rc = true;
    ////            }
    //        }
    //        return rc;
    //    }
    /*
     * Build the sql string needed to update the row
     */
    boolean handlePropertyHits(BackedSession d2, Thread t, int buflen) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[HANDLE_PROPERTY_HITS]);
        }
        boolean rc = true;

        if (buflen <= smallColSize) {
            d2.update.append(smallCol).append(equals).append(comma).append(setMediumNull).append(comma).append(setLargeNull);
        } else {
            if (buflen <= mediumColSize) {
                d2.update.append(setSmallNull).append(comma).append(medCol).append(equals).append(comma).append(setLargeNull);
            } else {
                if (buflen <= largeColSize) {
                    d2.update.append(setSmallNull).append(comma).append(setMediumNull).append(comma).append(lgCol).append(equals);
                } else {
                    rc = false;
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "DatabaseHashMap.db2LongVarCharErr");
                }
            }
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[HANDLE_PROPERTY_HITS], Boolean.valueOf(rc));
        }
        return rc;
    }

    /*
     * Persist the session change - session must already exist in database
     */
    protected boolean persistSession(BackedSession d2, boolean propHit) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[PERSIST_SESSION]);
        }

        Connection con = null;
        String id = d2.getId();
        Thread t = Thread.currentThread();
        PreparedStatement ps = null;
        boolean psClose = false;
        byte[] objbuf = null;
        int objbufLength = 0;

        try {
            // build update string based on what has changed
            d2.update = new StringBuffer();
            boolean didFirstCol = false;

            d2.update.append(upBase);
            if (d2.userWriteHit) {
                if (didFirstCol) {
                    d2.update.append(comma);
                }
                d2.update.append(userCol).append(equals);
                didFirstCol = true;
            }

            if (d2.maxInactWriteHit) {
                if (didFirstCol) {
                    d2.update.append(comma);
                } else {
                    didFirstCol = true;
                }
                d2.update.append(maxInactCol).append(equals);
            }

            if (d2.listenCntHit) {
                if (didFirstCol) {
                    d2.update.append(comma);
                } else {
                    didFirstCol = true;
                }
                d2.update.append(listenCol).append(equals);
            }

            if (!_smc.getEnableEOSWrite() || _smc.getScheduledInvalidation()) {
                if (didFirstCol) {
                    d2.update.append(comma);
                } else {
                    didFirstCol = true;
                }
                d2.update.append(lastAccCol).append(equals);
            }

            if (propHit) {
                if (!_smc.isUsingMultirow()) {
                    if (didFirstCol) {
                        d2.update.append(comma);
                    } else {
                        didFirstCol = true;
                    }
                    objbuf = serializeAppData(d2);
                    if (objbuf != null) {
                        objbufLength = objbuf.length;
                    }
                }

                boolean success = handlePropertyHits(d2, t, objbufLength);
                if (!success) { //325643
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PERSIST_SESSION], "returning false after handlePropertyHits");
                    }
                    return false; // 325643
                }
            }

            // if nothing changed, then just return
            if (!didFirstCol) {
                d2.update = null;
                d2.userWriteHit = false;
                d2.maxInactWriteHit = false;
                d2.listenCntHit = false;
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
                    LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[PERSIST_SESSION], "true - Nothing changed");
                }
                return true;
            } else {
                d2.update.append(upId);
            }

            con = getConnection(false);
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PERSIST_SESSION], "thread " + t + " is dealing with session " + id);
            }

            if (con == null) {
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
                    LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[PERSIST_SESSION], "null connection on id " + id);
                }
                return false;
            }

            ps = con.prepareStatement(d2.update.toString());
            long startTime = System.currentTimeMillis();
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PERSIST_SESSION], "doing a sql update of " + d2.update.toString());
            }
            int colcnt = 0;

            if (d2.userWriteHit) {
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PERSIST_SESSION], "update username " + d2.getUserName());
                }
                colcnt++;
                ps.setString(colcnt, d2.getUserName());
                d2.userWriteHit = false;
            }

            if (d2.maxInactWriteHit) {
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PERSIST_SESSION], "update maxinactive " + d2.getMaxInactiveInterval());
                }
                colcnt++;
                ps.setInt(colcnt, d2.getMaxInactiveInterval());
                d2.maxInactWriteHit = false;
            }

            if (d2.listenCntHit) {
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PERSIST_SESSION], "update listerncnt " + d2.listenerFlag);
                }
                colcnt++;
                ps.setShort(colcnt, d2.listenerFlag);
                d2.listenCntHit = false;
            }

            ByteArrayInputStream bis = null;
            long time = d2.getCurrentAccessTime();
            if (!_smc.getEnableEOSWrite() || _smc.getScheduledInvalidation()) {
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PERSIST_SESSION], "update last write time to DB !!!!!!! " + time);
                }
                colcnt++;
                // shouldn't get a -1 time since already stored...
                // I'm assuming lastAccess has been updated in the session object
                setPSLong(ps, colcnt, time);
                d2.setLastWriteLastAccessTime(time);

            }

            // not done in multirow, MR's handlePropertyHits will reset flag
            if (propHit && !_smc.isUsingMultirow()) {
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PERSIST_SESSION], "update  " + time); // cmd 200713
                }
                colcnt++;

                if ((!usingInformix) && (!_smc.isUseOracleBlob())) {
                    ps.setBytes(colcnt, objbuf);
                } else {

                    bis = new ByteArrayInputStream(objbuf);
                    ps.setBinaryStream(colcnt, (InputStream) bis, objbufLength);
                }
            }

            colcnt++;
            ps.setString(colcnt, id);

            colcnt++;
            ps.setString(colcnt, id);

            colcnt++; //*dbc2.2
            ps.setString(colcnt, d2.getAppName()); //*dbc2.2

            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PERSIST_SESSION], "before upd " + d2.update.toString() + " for sess " + id);
            }

            ps.executeUpdate();

            if (objbuf != null && propHit && !_smc.isUsingMultirow()) {
                SessionStatistics pmiStats = _iStore.getSessionStatistics();
                if (pmiStats != null) {
                    pmiStats.writeTimes(objbuf.length, System.currentTimeMillis() - startTime);
                }
            }

            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PERSIST_SESSION], "Just wrote out Primary Row ");
            }
            ps.close();
            psClose = true;

            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PERSIST_SESSION], "thread " + t + " has sent updates for " + id);
            }
        } catch (SQLException se) {
            //            if (isStaleConnectionException(se)) {
            //                com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.persistSession", "2094", d2);
            //                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            //                    LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[PERSIST_SESSION], "StaleConnectionException");
            //                }
            //            } else {
            com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMap.persistSession", "2099", d2);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[PERSIST_SESSION], "DatabaseHashMap.ejbStoreError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[PERSIST_SESSION], "CommonMessage.object", d2.toString());
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[PERSIST_SESSION], "CommonMessage.miscData", " Update string: " + d2.update);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[PERSIST_SESSION], "CommonMessage.exception", se);
            //            }
            return false;
        } catch (Exception ee) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ee, "com.ibm.ws.session.store.db.DatabaseHashMap.ejbStore", "1994", d2);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[PERSIST_SESSION], "DatabaseHashMap.ejbStoreError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[PERSIST_SESSION], "CommonMessage.object", d2.toString());
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[PERSIST_SESSION], "CommonMessage.exception", ee);
            return false;
        } finally {
            if (!psClose && ps != null)
                closeStatement(ps);
            if (con != null)
                closeConnection(con);
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[PERSIST_SESSION], Boolean.valueOf(true));
        }
        return true;
    }

    private SerializationService getSerializationService() {
        return this.getDatabaseStoreService().getSerializationService();
    }

    protected ObjectOutputStream createObjectOutputStream(OutputStream output) throws IOException {
        return getSerializationService().createObjectOutputStream(output);
    }

    /*
     * serializeAppData - returns a byte array form of the swappableData
     * This method not called for multiRow.
     */
    private byte[] serializeAppData(BackedSession d2) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[SERIALIZE_APP_DATA]);
        }
        // return either the byte array input stream for the app
        // data or a vector of byte array input streams if their is a
        // row for each piece of app data
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        byte[] objbuf = null;

        try {
            Map<Object, Object> ht = null;
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[SERIALIZE_APP_DATA], "get swappableData and convert to byte array");
            }
            synchronized (d2) {
                ht = d2.getSwappableData();
            }

            // serialize session (app data only) into byte array buffer
            baos = new ByteArrayOutputStream();
            oos = createObjectOutputStream(baos);
            oos.writeObject(ht);
            oos.flush();
            objbuf = baos.toByteArray();
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[SERIALIZE_APP_DATA], "success - size of byte array is " + objbuf.length);
            }

            oos.close();
            baos.close();
        } catch (ConcurrentModificationException cme) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.INFO, methodClassName, methodNames[SERIALIZE_APP_DATA], "DatabaseHashMap.deferWrite", /* + d2.deferWriteUntilNextTick */
                                                d2.getId());
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.store.db.DatabaseHashMap.commonSetup", "2052", d2);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[SERIALIZE_APP_DATA], "DatabaseHashMap.commonSetupError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[SERIALIZE_APP_DATA], "CommonMessage.exception", e);
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[SERIALIZE_APP_DATA]);
        }
        return objbuf;
    }

    /*
     * writeCachedLastAccessedTimes - if we have manual writes of time-based writes, we cache the last
     * accessed times and only write them to the persistent store prior to the inval thread running.
     */
    void writeCachedLastAccessedTimes(Connection nukerCon) throws Exception {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[WRITE_CACHED_LAST_ACCESSED_TIMES]);
        }

        // create a copy for the updates, then atomically clear the table
        // the hashtable table clone is shallow, it will not dup the keys/elements
        Hashtable updTab = (Hashtable) cachedLastAccessedTimes.clone();
        cachedLastAccessedTimes.clear();
        Enumeration updEnum = updTab.keys();
        String id = null;
        PreparedStatement ps = null;
        boolean psClose = false;

        while (updEnum.hasMoreElements()) {

            id = (String) updEnum.nextElement();
            Long timeObj = (Long) updTab.get(id);
            long time = timeObj.longValue();
            try {
                ps = nukerCon.prepareStatement(asyncUpdate);
                setPSLong(ps, 1, time);
                ps.setString(2, id);
                ps.setString(3, id);
                ps.setString(4, getIStore().getId());

                ps.executeUpdate();
                ps.close();
                psClose = true;
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[WRITE_CACHED_LAST_ACCESSED_TIMES], "handleAsyncUpdates"
                                                                                                                                    + " - Updating LastAccess for " + id);
                }
            } catch (Exception se1) {
                //                if (isStaleConnectionException(se1)) {
                //                    com.ibm.ws.ffdc.FFDCFilter.processException(se1, "com.ibm.ws.session.store.db.DatabaseHashMap.writeCachedLastAccessedTimes", "2102", id);
                //                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                //                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[WRITE_CACHED_LAST_ACCESSED_TIMES], "StaleConnectionException");
                //                    }
                //                    throw se1;
                //                } else {
                com.ibm.ws.ffdc.FFDCFilter.processException(se1, "com.ibm.ws.session.store.db.DatabaseHashMap.writeCachedLastAccessedTimes", "2109", id);
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[WRITE_CACHED_LAST_ACCESSED_TIMES], "DatabaseHashMap.handleAsyncError");
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[WRITE_CACHED_LAST_ACCESSED_TIMES], "CommonMessage.sessionid", id);
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[WRITE_CACHED_LAST_ACCESSED_TIMES], "CommonMessage.exception", se1);
                throw se1;
                //                }
            } finally {
                if (!psClose && ps != null) {
                    closeStatement(ps);
                }
            }
        }

    }

    /*
     * Get the collection name used by the table
     *
     * AS400 specific code provided by ISeries team
     * Technical contact: Art Smet/Rochestor
     */
    String getCollectionName(String url) {
        final String SEPARATOR = "\\";
        String cell = null;
        String node = null;
        String serverName = null;
        int index = -1;

        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[GET_COLLECTION_NAME], "url: " + url);
        }

        //lets look to see if we can use the url.  This most likely means the native driver
        if (url != null && url.indexOf("jdbc:db2") != -1) {
            String collection = url;

            index = collection.indexOf(";");
            if (index != -1) {
                collection = collection.substring(0, index);
            }
            index = collection.indexOf("//");
            if (index != -1) {
                collection = collection.substring(index + 2);
            }
            index = collection.indexOf("/");
            if (index != -1) {
                collection = collection.substring(index + 1).trim();
                if (collection.length() != 0) {
                    as400_collection = collection;
                }
            }
        } else {
            //using toolbox driver

            //            try {
            //            	/*
            //                String unformatted = com.ibm.websphere.runtime.ServerName.getFullName();
            //
            //                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            //                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_COLLECTION_NAME], "Full server name retrieved: " + unformatted);
            //                }
            //
            //                //we need everything or nothing, so do no checking here until we're done.
            //                //if we something winds up null, then we're toast anyway, so take the exception
            //                //and use the defaults in the catch below.
            //                index = unformatted.indexOf(SEPARATOR);
            //                cell = unformatted.substring(0, index);
            //                unformatted = unformatted.substring(index + SEPARATOR.length());
            //
            //                index = unformatted.indexOf(SEPARATOR);
            //                node = unformatted.substring(0, index);
            //
            //                serverName = unformatted.substring(index + SEPARATOR.length());
            //
            //                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            //                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_COLLECTION_NAME], "Using cell name: " + cell);
            //                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_COLLECTION_NAME], "Using node name: " + node);
            //                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_COLLECTION_NAME], "Using server name: " + serverName);
            //                }
            //
            //                if ((cell == null) || (node == null) || (serverName == null))
            //                    throw new Exception("Unable to retrieve cell, node, or serverName.");
            //
            //                com.ibm.ws.runtime.service.Repository repository = com.ibm.ws.runtime.service.RepositoryFactory.createRepository(System.getProperty("user.install.root") + "/config", cell, node, serverName);
            //
            //                  */
            //                // com.ibm.ws.runtime.service.Repository repository = com.ibm.ws.runtime.service.RepositoryFactory.createRepository(System.getProperty("user.install.root") + "/config", cell, node, serverName);
            //                // PK78174 this instanceof com.ibm.wsspi.runtime.component.WsComponent
            //                Repository repository = (Repository) WsServiceRegistry.getService(this, Repository.class);
            //
            //                if (repository == null)
            //                    throw new Exception("Unable to acquire a reference to the repository.");
            //
            //                String jndiDSName = _smc.getJNDIDataSourceName();
            //                if ((as400_collection = retrieveDBSessionCollection(repository, com.ibm.ws.runtime.service.ConfigRoot.SERVER, jndiDSName)) == null) {
            //                    if ((as400_collection = retrieveDBSessionCollection(repository, com.ibm.ws.runtime.service.ConfigRoot.NODE, jndiDSName)) == null) {
            //                        if ((as400_collection = retrieveDBSessionCollection(repository, com.ibm.ws.runtime.service.ConfigRoot.CELL, jndiDSName)) == null) {
            //                        	if ((as400_collection = retrieveDBSessionCollection(repository, com.ibm.ws.runtime.service.ConfigRoot.CLUSTER, jndiDSName)) == null) {
            //                        		throw new Exception("DataSource not configured with JNDI name: " + jndiDSName);
            //                        	}
            //                        }
            //                    }
            //                }
            //            } catch (Exception ex) {
            //                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.WARNING, methodClassName, methodNames[GET_COLLECTION_NAME], "CommonMessage.exception", ex);
            //            }
        }

        if (as400_collection == null) {
            String installLibrary = System.getProperty("was.install.library");
            if (installLibrary == null) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.WARNING, methodClassName, methodNames[GET_COLLECTION_NAME], "CommonMessage.miscData",
                                                    "was.install.library not set.  Using collection QEJBASSN for session persistance.");
                as400_collection = "QEJBASSN";
            } else {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.WARNING, methodClassName, methodNames[GET_COLLECTION_NAME], "CommonMessage.miscData",
                                                    "using was.install.library to derive collection name for session persistance.");
                as400_collection = installLibrary + "SN";
                if (as400_collection.length() > 10) {
                    as400_collection = as400_collection.substring(0, 10);
                }
            }
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[GET_COLLECTION_NAME], "Session Persistance will use the collection: " + as400_collection);
        }
        return as400_collection;
    }

    /*
     * AS400 specific code provided by ISeries team
     * Technical contact: Art Smet/Rochestor
     */
    //    private String retrieveDBSessionCollection(com.ibm.ws.runtime.service.Repository repository, int level, String jndiDSName) throws Exception {
    //
    //        String libs = null, jndiName = null;
    //        List myList = null;
    //        // begin LIDB4119-35.01
    //        try {
    //            ConfigService service = (ConfigService)ComponentUtil.getService(this, ConfigService.class);
    //            myList = service.getDocumentObjects(service.getScope(level), "resources.xml");
    //
    //        } catch (Exception ex) {
    //            myList = null;
    //        }
    //        if (myList == null)
    //            return(String) null;
    //
    //        for (int i = 0; i < myList.size(); i++) {
    //            ConfigObject myResource = (ConfigObject)myList.get(i);
    //            if (myResource.instanceOf(CT_JDBCProvider.URI, CT_JDBCProvider.NAME)) {
    //                java.util.List myList2 = myResource.getObjectList(CT_JDBCProvider.FACTORIES_NAME);
    //                for (int j = 0; j < myList2.size(); j++) {
    //                    ConfigObject myResource2 = (ConfigObject)myList2.get(j);
    //                    if (myResource2.instanceOf(CT_DataSource.URI, CT_DataSource.NAME)) {
    //                        jndiName = myResource2.getString(CT_DataSource.JNDINAME_NAME, CT_DataSource.JNDINAME_DEFAULT);
    //                        if (jndiName != null && jndiName.equals(jndiDSName)) {
    //                        	ConfigObject propertySet = myResource2.getObject(CT_DataSource.PROPERTYSET_NAME);
    //                            java.util.List myList3 = propertySet.getObjectList(CT_J2EEResourcePropertySet.RESOURCEPROPERTIES_NAME);
    //                            for (int k = 0; k < myList3.size(); k++) {
    //                                ConfigObject myResource3 = (ConfigObject)myList3.get(k);
    //                            	String propertyName = myResource3.getString(CT_J2EEResourceProperty.NAME_NAME, CT_J2EEResourceProperty.NAME_DEFAULT);
    //                                if (propertyName.equals("libraries")) {
    //                                	libs = myResource3.getString(CT_J2EEResourceProperty.VALUE_NAME, CT_J2EEResourceProperty.VALUE_DEFAULT);
    //                                    if (libs != null) libs = libs.trim();
    //                                    // end LIDB4119-35.01
    //                                    if (libs != null && !libs.equals("")) {
    //                                        int comma = libs.indexOf(",");
    //                                        //result will be the first library they listed, if any were listed.
    //                                        String result = libs.substring(0, (comma == -1 ? libs.length() : comma)).trim();
    //
    //                                        //just in case they manually entered nothing or only *LIBL
    //                                        if (result != null && !result.equals("") && !result.equalsIgnoreCase("*LIBL")) {
    //                                            return result;
    //                                        }
    //                                    }
    //                                    //libraries property specified, but the value was not valid.  We must bail out the rest of the process
    //                                    throw new Exception("Invalid or missing libraries property specified on datasource.");
    //                                }
    //                            }
    //                        }
    //                    }
    //                }
    //            }
    //        }
    //        return null;
    //    }

    /*
     * No need to read from the large Column since it should never be
     * used on Oracle
     */
    private Object oracleGetValue(ResultSet rsltset, BackedSession s) {
        Object tmp = null;
        try {
            long startTime = System.currentTimeMillis();
            long readSize = 0;
            BufferedInputStream bis = null;
            ByteArrayInputStream bais = null;
            byte[] b = rsltset.getBytes(smallCol);
            if (b == null) {
                if (!_smc.isUseOracleBlob()) {
                    // not using blob, use getBytes to get data from medium
                    b = rsltset.getBytes(medCol);
                } else { // using blob, get from Blob column
                    Blob blob = rsltset.getBlob(medCol);
                    if (blob != null) {
                        bis = new BufferedInputStream(blob.getBinaryStream());
                        readSize = blob.length();
                    }
                }
            }
            if (b != null && b.length > 0) { // got data from small or non-Blob medium
                                             // convert to BufferedInputStream
                readSize = b.length;
                bais = new ByteArrayInputStream(b);
                bis = new BufferedInputStream(bais);
            }

            if (bis != null) { // we successfully retrieved some data
                try {
                    tmp = ((DatabaseStore) getIStore()).getLoader().loadObject(bis);
                } catch (ClassNotFoundException ce) {

                    com.ibm.ws.ffdc.FFDCFilter.processException(ce, "com.ibm.ws.session.store.db.DatabaseHashMap.oracleGetValue", "2321", s);
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[ORACLE_GET_VALUE], "BackedHashtable.classNotFoundError");
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[ORACLE_GET_VALUE], "CommonMessage.sessionid", s.getId());
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[ORACLE_GET_VALUE], "CommonMessage.exception", ce);
                }
                bis.close();
                if (bais != null)
                    bais.close();
                SessionStatistics pmiStats = _iStore.getSessionStatistics();
                if (pmiStats != null) {
                    pmiStats.readTimes(readSize, System.currentTimeMillis() - startTime);
                }
            }
        } catch (Throwable e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.store.db.DatabaseHashMap.oracleGetValue", "2334", s);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[ORACLE_GET_VALUE], "DatabaseHashMap.oracleGetValueError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[ORACLE_GET_VALUE], "CommonMessage.exception", e);
        }
        return tmp;
    }

    /*
     * For 5.0 there can now be a unique datasource for each databasesessionContext/backedhashtable
     * Looks like tableName and other vars are always hardcoded in SessionConstants...
     * so strings can stay static
     * However, in AS400 environment, if these fields can be different in each bht then these
     * strings must no longer be static!!!
     */
    protected void initializeSQL_Strings() {
        final String createCol = "creationtime";
        final String commonPreListener = " and (listenercnt = " + BackedSession.HTTP_SESSION_BINDING_LISTENER + " OR listenercnt = "
                                         + BackedSession.HTTP_SESSION_BINDING_AND_ACTIVATION_LISTENER
                                         + " ) and maxinactivetime >= 0 and (maxinactivetime < ((? - lastaccess) / 1000.0)))";//PK90548
        final String commonPreNoListener = " and (listenercnt = " + BackedSession.HTTP_SESSION_NO_LISTENER + " OR listenercnt = " + BackedSession.HTTP_SESSION_ACTIVATION_LISTENER
                                           + " ) and maxinactivetime >= 0 and (maxinactivetime < ((? - lastaccess) / 1000.0)))";//PK90548
        final String upBaseAcc = "update " + tableName + " set " + lastAccCol + " = ? ";

        remoteInvalAll = "update " + tableName + " set maxinactivetime = 0 where id = ? and propid = ? and appname = ? and maxinactivetime != 0";

        getOneNoUpdate = "select " + maxInactCol + "," + lastAccCol + ", " + appCol + " from " + tableName + " where id = ? and propid = ? for read only";
        getOneNoUpdateNonDB2 = "select " + maxInactCol + "," + lastAccCol + ", " + appCol + " from " + tableName + " where id = ? and propid = ?";

        upBase = "update " + tableName + " set ";
        asyncUpdate = upBaseAcc + " where id = ? and propid = ?  and appname = ?";
        optUpdate = upBaseAcc + " where id = ? and propid = ?  and appname = ? and " + lastAccCol + " = ? ";
        optUpdatePrimRow = upBaseAcc + " where id = ? and propid = ?  and appname = ? and " + lastAccCol + " = ? ";

        insNoProp = "insert into " + tableName + " (" + varList + ") values (?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL)";
        insForInval = "insert into " + tableName + " (" + idCol + "," + propCol + "," + appCol + "," + lastAccCol + "," + maxInactCol + ") values (?, ?, ?, ?, ?)";
        insSm = "insert into " + tableName + " (" + varList + ") values (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL)";
        insMed = "insert into " + tableName + " (" + varList + ") values (?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, NULL)";
        insLg = "insert into " + tableName + " (" + varList + ") values (?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?)";

        getProp = "select small, medium, large from  " + tableName + "  where id = ? and propid = ? and appname = ? for read only";

        //PK71265
        delPropall = "delete from  " + tableName + " where id = ? and propid <> id and appname = ?";

        getPropNotDB2 = "select small, medium, large from  " + tableName + "  where id = ? and propid = ? and appname = ?";

        delProp = "delete from " + tableName + " where id = ? and propid = ? and appname = ?";

        insSmProp = "insert into " + tableName + " (id, propid, small, appname) values (?, ?, ?, ?)";

        insMedProp = "insert into " + tableName + " (id, propid, medium, appname) values (?, ?, ?, ?)";

        insLgProp = "insert into " + tableName + " (id, propid, large, appname) values (?, ?, ?, ?)";

        selMed = "select medium from " + tableName;
        selLg = "select large from " + tableName;

        readLastAccess = "select " + lastAccCol + " from " + tableName + " where id = ? and propid = ? and appname = ?";

        selectForUpdate = "select " + lastAccCol + " from " + tableName + " where id = ? and propid = ? and appname = ? for update of lastaccess";
        delPrimaryRowInval = "delete from  " + tableName + " where id = ? and propid = id  and appname = ? and lastaccess = ?";

        readPrimitiveData = "select " + lastAccCol + ", " + createCol + " ," + maxInactCol + "," + userCol + "," + listenCol + " from " + tableName
                            + " where id = ? and propid = ? and appname = ?";
        readPrimitiveDataDb2 = readPrimitiveData + " for read only";
        delOne = "delete from " + tableName + " where id = ?  and appname = ?";
        selDelNoListener = "select id," + lastAccCol + ", " + createCol + " from  " + tableName + " where (appname = ? " + commonPreNoListener;
        selNukerString = "select id," + lastAccCol + ", " + createCol + "," + maxInactCol + "," + userCol + "," + listenCol + " from  " + tableName + " where ( appname = ? "
                         + commonPreListener;
    }

    /*
     * setMaxInactToZero - called to set the max inactive time to zero for remote invalidateAll.
     * This will result in the session being invalidated by the next run of the background
     * invalidator.
     */
    int setMaxInactToZero(String sessId, String appName) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[SET_MAX_INACT_TO_ZERO], "for " + sessId + " and app " + appName);
        }
        int rc = 0;
        PreparedStatement psRemoteInval = null;
        Connection con = getConnection(false);
        if (con == null) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[SET_MAX_INACT_TO_ZERO], "DatabaseHashMap.nullConnection");
            return 0;
        }
        try {
            psRemoteInval = con.prepareStatement(remoteInvalAll);
            psRemoteInval.setString(1, sessId);
            psRemoteInval.setString(2, sessId);
            psRemoteInval.setString(3, appName);
            rc = psRemoteInval.executeUpdate();
        } catch (Throwable t) {
            com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.session.store.db.DatabaseHashMap.setMaxInactToZero", "2417", this);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[SET_MAX_INACT_TO_ZERO], "CommonMessage.exception", t);
        } finally {
            if (psRemoteInval != null)
                closeStatement(psRemoteInval);
            closeConnection(con);
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[SET_MAX_INACT_TO_ZERO], "for " + sessId + " returning " + rc);
        }
        return rc;
    }

    /*
     * Perform invalidation - called to perform background invalidation processing
     */
    protected void performInvalidation() {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[PERFORM_INVALIDATION]);
        }

        long now = System.currentTimeMillis();
        PreparedStatement ps1 = null;
        ResultSet rs1 = null;
        PreparedStatement ps2 = null;

        boolean ps1Closed = false;
        boolean ps2Closed = false;

        Connection nukerCon = null;

        // do some initializations
        String appName = getIStore().getId(); //bhSessionContext.getAppName();

        boolean doInvals = false;
        boolean doDatabaseInval = doScheduledInvalidation();
        nukerCon = getConnection(false);

        if (nukerCon == null) {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[PERFORM_INVALIDATION], "Connection is null");
            }
            return;
        }

        try {

            // handle last acc times for manual update irregardless
            // of whether this thread will scan for time outs
            if (!_smc.getEnableEOSWrite()) {
                writeCachedLastAccessedTimes(nukerCon);
            }

            //check the value of doDatabaseInval
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE,methodClassName, methodNames[PERFORM_INVALIDATION], "doDatabaseInval="+doDatabaseInval);
            }

            if (doDatabaseInval) {
                ps1 = nukerCon.prepareStatement(readLastAccess);
                ps1.setString(1, appName);
                ps1.setString(2, appName);
                ps1.setString(3, appName);
                rs1 = ps1.executeQuery();

                boolean rowExists = false;
                long lastTime = 0L;
                if (rs1.next()) {
                    rowExists = true;
                    lastTime = rs1.getLong(1);
                }
                rs1.close();
                ps1.close();
                ps1Closed = true;

                //check the value of rowExists
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE,methodClassName, methodNames[PERFORM_INVALIDATION], "rowExists="+rowExists);
                }

                if (rowExists) {
                    long lastCheck = now - _smc.getInvalidationCheckInterval() * 1000;

                    //check the value of lastCheck,lastTime,now
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE,methodClassName, methodNames[PERFORM_INVALIDATION], "lastCheck="+lastCheck+",lastTime="+lastTime+",now="+now);
                    }

                    //if no other server tried to process invalidation within the interval, this will be true
                    //similar to updateNukerTimeStamp, but we have an extra check here to test the last access time hasn't changed
                    //PK30585 - added future time check
                    if ((lastCheck >= lastTime) || (lastTime > now)) {
                        ps2 = nukerCon.prepareStatement(optUpdate);
                        setPSLong(ps2, 1, now);
                        ps2.setString(2, appName);
                        ps2.setString(3, appName);
                        ps2.setString(4, appName);
                        setPSLong(ps2, 5, lastTime);
                        int res = ps2.executeUpdate();
                        if (res > 0)
                            doInvals = true;
                    }
                } else {
                    //If we are here means, this is the first time this web module is
                    //trying to invalidation of sessions
                    ps2 = nukerCon.prepareStatement(insForInval);
                    ps2.setString(1, appName);
                    ps2.setString(2, appName);
                    ps2.setString(3, appName);
                    setPSLong(ps2, 4, now);
                    ps2.setInt(5, -1);
                    try {
                        ps2.executeUpdate();
                        doInvals = true;
                    } catch (SQLException sqle) {
                        //                        if (isDuplicateKeyException(sqle)) {
                        //                            // ignore duplicate key errror here - can only happen in very small timing window but causes no harm
                        //                            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        //                                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PERFORM_INVALIDATION], "Duplicate key inserted");
                        //                            }
                        //                        } else {
                        throw sqle; // to be re-caught and logged below
                        //                        }
                    }
                }

                if (ps2 != null)
                    ps2.close();
                ps2Closed = true;

                //check the value of doInvals
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE,methodClassName, methodNames[PERFORM_INVALIDATION], "doInvals="+doInvals);
                }

                if (doInvals) {

                    //Process the non-listener sessions first
                    doInvalidations(nukerCon);

                    //Read in all the sessions with listeners that need to be invalidated
                    Enumeration e = pollForInvalidSessionsWithListeners(nukerCon);
                    processInvalidListeners(e, nukerCon);

                }

            }

        } catch (Throwable t) {
            com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.session.store.db.DatabaseHashMap.performInvalidation", "2537", this);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[PERFORM_INVALIDATION], "DatabaseHashMap.performInvalidationError");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[PERFORM_INVALIDATION], "CommonMessage.exception", t);
        } finally {
            if (!ps1Closed && rs1 != null)
                closeResultSet(rs1);
            if (!ps1Closed && ps1 != null)
                closeStatement(ps1);
            if (!ps2Closed && ps2 != null)
                closeStatement(ps2);
            closeConnection(nukerCon);
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[PERFORM_INVALIDATION]);
        }
    }

    /*
     * Process invalid session with listeners
     */
    void processInvalidListeners(Enumeration enum1, Connection nukerCon) throws Exception {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[PROCESS_INVALID_LISTENERS]);
        }

        DatabaseSession s;
        PreparedStatement delNuke = null;
        boolean delNukeClose = false;
        PreparedStatement ps = null;
        boolean psClose = false;
        long now = System.currentTimeMillis();
        //handle the subset of session listerners
        int deleteCount = 0;
        while (enum1.hasMoreElements()) {
            s = (DatabaseSession) enum1.nextElement();
            String id = s.getId();
            long lastAccess = s.getCurrentAccessTime(); // try using lastTouch again..

            try {
                // get the session ready and read in any listeners
                s.setIsNew(false);
                s.getSwappableListeners(BackedSession.HTTP_SESSION_BINDING_LISTENER);

                delNuke = nukerCon.prepareStatement(delPrimaryRowInval);
                delNuke.setString(1, id);
                delNuke.setString(2, s.getAppName());
                setPSLong(delNuke, 3, lastAccess);

                int rc = delNuke.executeUpdate();
                delNuke.close();
                delNukeClose = true;

                // only invalidate those which have not been accessed since
                // check in computeInvalidList
                if (rc > 0) {
                    // return of session done as a result of this call
                    s.internalInvalidate(true);

                    if (_smc.isUsingMultirow()) {
                        ps = nukerCon.prepareStatement(delOne);
                        ps.setString(1, id);
                        ps.setString(2, s.getAppName());
                        ps.executeUpdate();
                        ps.close();
                        psClose = true;
                    }
                    deleteCount++;

                }

                /*
                 * REG : Changed this on 11/03/2006 from
                 * "if ((now + _smc.getInvalidationCheckInterval() * 1000) >= System.currentTimeMillis()) {"
                 * because we don't want to update this on every invalidation with a listener that is processed.
                 * We'll only update this if we're getting close.
                 *
                 * Processing Invalidation Listeners could take a long time. We should update the
                 * NukerTimeStamp so that another server in this cluster doesn't kick off invalidation
                 * while we are still processing. We only want to update the time stamp if we are getting
                 * close to the time when it will expire. Therefore, we are going to do it after we're 1/2 way there.
                 */
                if ((now + _smc.getInvalidationCheckInterval() * (1000 / 2)) < System.currentTimeMillis()) {
                    updateNukerTimeStamp(nukerCon, getIStore().getId());
                    now = System.currentTimeMillis();
                }
            } catch (Exception e) {
                //                if (isStaleConnectionException(e)) {
                //                    com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.store.db.DatabaseHashMap.processInvalidListeners", "2620", s);
                //                    throw e;
                //                } else {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.store.db.DatabaseHashMap.processInvalidListeners", "2623", s);
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[PROCESS_INVALID_LISTENERS], "DatabaseHashMap.invalidateError");
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[PROCESS_INVALID_LISTENERS], "CommonMessage.exception", e);
                throw e;
                //                }
            } finally {
                if (!delNukeClose && delNuke != null)
                    closeStatement(delNuke);
                if (!psClose && ps != null)
                    closeStatement(ps);
            }
        }

    }

    /*
     * updateNukerTimeStamp
     * When running in a clustered environment, there could be multiple machines processing invalidation.
     * This method updates the last time the invalidation was run. A server should not try to process invalidation if
     * it was already done within the specified time interval for that app.
     */
    private void updateNukerTimeStamp(Connection nukerCon, String appName) throws SQLException {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[UPDATE_NUKER_TIME_STAMP], "appName=" + appName);
        }
        PreparedStatement ps = null;
        long now = System.currentTimeMillis();
        try {
            ps = nukerCon.prepareStatement(asyncUpdate);
            setPSLong(ps, 1, now);
            ps.setString(2, appName);
            ps.setString(3, appName);
            ps.setString(4, appName);
            ps.executeUpdate();

        } finally {
            if (ps != null)
                closeStatement(ps);
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[UPDATE_NUKER_TIME_STAMP], "appName=" + appName);
        }
    }

    /*
     * loadOneValue
     * For multirow db, attempts to get the requested attr from the db
     * Returns null if attr doesn't exist or we're not running multirow
     * After PM90293, we consider populatedAppData as well
     * populatedAppData is true when session is new or when the entire session is read into memory
     * in those cases, we don't want to go to the backend to retrieve the attribute
     */
    protected Object loadOneValue(String attrName, BackedSession sess) {
        Object value = null;
        if (_smc.isUsingMultirow() && !((DatabaseSession)sess).getPopulatedAppData()) { //PM90293
            value = getValue(attrName, sess);
        }
        return value;
    }

    /*
     * Sets long value on a preparedstatement
     */
    final void setPSLong(PreparedStatement ps, int index, long value) throws SQLException {
        if (usingDB2Connect || usingSQLServer || usingDB2zOS) { // LIDB2775.25 zOS
            ps.setBigDecimal(index, BigDecimal.valueOf(value));
        } else {
            ps.setLong(index, value);
        }
    }

    /*
     * PK06395/d321615 BEGIN
     * Create dummy resource-ref for non-J2EE application, such as WAS session manager, to locate a datasource
     * using non-direct JDNI lookup, and use the database resource like J2EE applications.
     */
    private void beginDBContext() {
        //        if (cmda == null) {  // can't handle this
        //            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.WARNING, methodClassName, methodNames[BEGIN_DB_CONTEXT], "DatabaseHashMap.cmdaNull");
        //            return;
        //        }
        ////        * A CustomContainerComponentMetaData is a specialized, mutable ContainerComponentMetaData
        ////        * that allows updating some of the meta-data properties after the meta-data is in-use.
        ////        * The values that can be overridden are those that are provided on this interface or
        ////        * one of the inner-interfaces.<p>
        ////        *
        ////        * Each encompassing meta-data type has it's own limitations on what may be changed
        ////        * and when.  Following these simple rules will eliminate the majority of any issues:
        ////        * <ul>
        ////        * <li>Always use the ComponentMetaDataAccessorImpl.beginContext() with an endContext()
        ////        * <li>When accessing J2C resources always use the get/use/close pattern to avoid connection handle problems.
        ////        * <li>Try to do all J2C work within a transactional context (see the Extension Helper service)
        ////        * <li>When updating meta-data fields, try to make the changes before using any meta-data consumers to avoid any
        ////        *     potential conflicts.
        ////        * </ul>
        ////        *
        ////        * Note:  Any setter methods inherited from ContainerComponentMetaData will apply to the
        ////        * parent (wrapped) ComponentMetaData object.
        //        CustomContainerComponentMetaData gcmd = null;
        //        ComponentMetaData compMetaData = cmda.getComponentMetaData();
        //        if (compMetaData != null) {
        //            gcmd = new CustomContainerComponentMetaDataImpl(compMetaData);
        //        } else {
        //            gcmd = new CustomContainerComponentMetaDataImpl();
        //        }
        //
        ////        * The local transaction config data that allows overriding
        ////        * some of the data values.
        //        CustomContainerComponentMetaData.CustomLocalTranCfg ltCfg = gcmd.getCustomLocalTranConfigData();
        //        ltCfg.setValueBoundary(LocalTranConfigData.BOUNDARY_BEAN_METHOD); // com.ibm.ejs.models.base.extensions.commonext.localtran.LocalTransactionBoundaryKind.BEAN_METHOD; // 1
        //        ltCfg.setValueResolver(LocalTranConfigData.RESOLVER_APPLICATION); // com.ibm.ejs.models.base.extensions.commonext.localtran.LocalTransactionResolverKind.APPLICATION; // 0
        //        ltCfg.setValueUnresolvedAction(LocalTranConfigData.UNRESOLVED_ROLLBACK); // com.ibm.ejs.models.base.extensions.commonext.localtran.LocalTransactionUnresolvedActionKind.ROLLBACK; // 1
        //
        ////      * This ResRefList implementation is a simplified version that only handles
        ////      * simulated/programmatic resource-refs...  Resource-refs can be added
        ////      * at any time.
        //        CustomContainerComponentMetaData.CustomResRefList rrList = gcmd.getCustomResourceRefList();
        //        rrList.addResRef("Session Persistance Custom JDBC Res-Ref", // description
        //                         _smc.getJNDIDataSourceName(), // name
        //                         _smc.getJNDIDataSourceName(), // jndiName
        //                         "javax.resource.cci.ConnectionFactory", // type
        //                         ResRef.APPLICATION, // resAuth - org.eclipse.jst.j2ee.common.ResAuthTypeBase.APPLICATION=
        //                         ResRef.SHAREABLE, // resSharingScope - org.eclipse.jst.j2ee.common.ResSharingScopeType.SHAREABLE
        //                         ResRef.TRANSACTION_READ_COMMITTED); // resIsolationLevel - com.ibm.ejs.models.base.extensions.commonext.IsolationLevelKind.TRANSACTION_READ_COMMITTED
        //
        //        cmda.beginContext(gcmd);
    }

    /*
     * endDBContext
     */
    private void endDBContext() {
        //        if (cmda != null) {
        //            cmda.endContext();
        //        }
    }

    //  PK56991 starts
    /**
     * PK56991 we are checking if there is an existing index for the session table
     * in distributed and iSeries platforms. The same test does not apply to zOS
     * system as if we ask the customer to create the session index manually during
     * table creation. Session manager does not create session index.
     */
    private boolean doesIndexExists(Connection con, String indexName) {

        if (usingDB2) {
            if (usingAS400DB2) {
                return doesIndexExistsiSeries(con, indexName);
            }
            return doesIndexExistsDistributed(con, indexName);
        } else
            return false;
    }

    /**
     * PK56991 checking if session index already exists in the DB2 table in distributed
     * platform. There are two booleans: indexExists is set to true if the session index
     * exists. indexColDefCorrect is set to true if all three index columns exists. Session manager
     * will issue a warning if the session index exists with any missing index column.
     * Session manager will return true to the caller if the session index exists
     * */
    //
    private boolean doesIndexExistsDistributed(Connection con, String indexName) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[DOES_INDEX_EXISTS_DISTRIBUTED]);
        }
        boolean indexExists = false, indexColDefCorrect = false;
        String tblName = tableName, qualifierName = null, colNames = null, sqlQueryCol = null, sqlQueryIndex = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        tblName = tblName.toUpperCase();
        if (_smc.isUsingCustomSchemaName()) { //PM27191
            qualifierName = qualifierNameWhenCustomSchemaIsSet;
        } else if (dbid != null) {
            qualifierName = dbid.toUpperCase();
        }

        sqlQueryCol = "select ColNames from syscat.indexes " +
                      "where IndName = '" + indexName.toUpperCase() + "' and " +
                      "TabName = '" + tblName + "' and UniqueRule = 'U'";
        if (qualifierName != null)
            sqlQueryCol += " and tabschema = '" + qualifierName + "'";
        sqlQueryCol += " for read only";
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DOES_INDEX_EXISTS_DISTRIBUTED], "Sql: " + sqlQueryCol);
        }

        try {
            ps = con.prepareStatement(sqlQueryCol);
            rs = ps.executeQuery();
            if (rs.next()) {
                colNames = rs.getString(1);
                // if the resultset and colNames are non-null, we now know session index exists in the syscat catalog
                // Let us now check if the index definition is what the session manager would have created
                indexExists = true;

                if (colNames != null) {
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DOES_INDEX_EXISTS_DISTRIBUTED], colNames);
                    }
                    if (colNames.indexOf(idCol.toUpperCase()) != -1) {
                        if (colNames.indexOf(propCol.toUpperCase()) != -1) {
                            if (colNames.indexOf(appCol.toUpperCase()) != -1) {
                                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DOES_INDEX_EXISTS_DISTRIBUTED],
                                                                        "Index Column Definition is correct");
                                }
                                // colExists is set to true if all columns exist
                                indexColDefCorrect = true;
                            }
                        }
                    }
                    //we issue the warning here as if the index exists and if
                    //any index column is missing
                    if (!indexColDefCorrect) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.WARNING, methodClassName, methodNames[DOES_INDEX_EXISTS_DISTRIBUTED], "DatabaseHashMap.IndexIncorrect");
                    }
                }
            }
        } catch (Throwable th) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[DOES_INDEX_EXISTS_DISTRIBUTED], "CommonMessage.exception", th);
        } finally {
            if (rs != null) {
                closeResultSet(rs);
            }
            if (ps != null) {
                closeStatement(ps);
            }
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[DOES_INDEX_EXISTS_DISTRIBUTED], indexExists);
        }
        return indexExists;
    }

    /**
     * PK56991
     * checking if session index already exists in the DB2 table on iSeries
     * platform. There are two booleans: indexExists_iSeries is set to true if the
     * session index exists. indexColDefCorrect is set to true if all three index columns exists.
     * In a specific scenario if session index exists with incorrect index defintion.
     * Session manager will issue a warning, return true to the caller method, createTable().
     * and skip the process of session index creation as if an session index already exists
     * The warning is issued to notify the recovery process. Customer requires to remove an existing
     * session index with incorrect index definition from the table manually and restarts the
     * application server. During server restart, session manager will create a new session index
     * with correct column definition.
     * Unlike distributed platform we conduct two queries to check Index and IndexColumns
     * as the information of Index and IndexColumn locate in different catalogs
     * */

    private boolean doesIndexExistsiSeries(Connection con, String indexName) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[DOES_INDEX_EXISTS_ISERIES]);
        }
        boolean indexExists_iSeries = false;

        //Introduce noQualifiertblName and set it to "SESSIONS" instead of using tableName since tableName is set to
        //qualifierName.tableName which is different from the one using in iSeries platform
        String sysIndexes = null, sysKeys = null, noQualifiertblName = tableName, qualifierName = null, sqlQueryIndex = null, sqlQueryCol = null, returnIndexName = null, index_colNames = null, schemaSeparator = ".";

        int index = tableName.indexOf(".");
        if (index != -1) {
            noQualifiertblName = tableName.substring(index+1);
        }
	
        PreparedStatement ps = null, ps1 = null;
        ResultSet rs = null, rs1 = null;
        int counter = 0;

        sysIndexes = "QSYS2".concat(schemaSeparator).concat("sysindexes");
        sysKeys = "QSYS2".concat(schemaSeparator).concat("syskeys");

        //Keep the following line for future reference
        //SELECT INDEX_NAME FROM QSYS2.SYSINDEXES  WHERE INDEX_NAME='SESS_INDEX' AND TABLE_NAME='SESSIONS'
        //AND IS_UNIQUE='U' AND TABLE_SCHEMA='KPW51BSSSN'FOR READ ONLY
        sqlQueryIndex = "select index_name from " + sysIndexes + " where Index_Name = '"
                        + indexName.toUpperCase() + "' and " + "Table_Name = '" + noQualifiertblName
                        + "' and IS_UNIQUE = 'U'";

        //Keep the following line for future reference
        //SELECT COLUMN_NAME FROM qsys2.SYSKEYS  WHERE INDEX_NAME='SESS_INDEX' AND INDEX_SCHEMA='KPW51BSSSN'
        sqlQueryCol = "select COLUMN_NAME from " + sysKeys +
                      " where INDEX_NAME = '" + indexName.toUpperCase() + "'";

        if (collectionName != null) {
            sqlQueryIndex += " and table_schema = '" + collectionName.toUpperCase() + "'";
            sqlQueryCol += " and index_schema = '" + collectionName.toUpperCase() + "'";
        }
        sqlQueryIndex += " for read only";
        sqlQueryCol += " for read only";

        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DOES_INDEX_EXISTS_ISERIES], "sqlQueryIndex: " + sqlQueryIndex);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DOES_INDEX_EXISTS_ISERIES], "sqlQueryCol: " + sqlQueryCol);
        }

        //checking if the index exists first
        try {
            ps = con.prepareStatement(sqlQueryIndex);
            rs = ps.executeQuery();

            if (rs.next()) { //ResultSet returning the possible SESS_INDEX
                returnIndexName = rs.getString(1);
                if (returnIndexName != null) {
                    if (returnIndexName.indexOf(indexName.toUpperCase()) != -1) {
                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DOES_INDEX_EXISTS_ISERIES], "index: " + returnIndexName + " exists");
                        }
                        indexExists_iSeries = true;
                    } else if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DOES_INDEX_EXISTS_ISERIES], "index: " + returnIndexName + " does not exist");
                    }
                }
            } else {
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DOES_INDEX_EXISTS_ISERIES], "ResultSet is null");
                }
            }
        } catch (Throwable th) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[DOES_INDEX_EXISTS_ISERIES], "CommonMessage.exception", th);
        } finally {
            if (rs != null) {
                closeResultSet(rs);
            }
            if (ps != null) {
                closeStatement(ps);
            }
        }
        //If the session index exists, we will check if the index definition is
        //what the session manager would have created
        if (indexExists_iSeries) {
            try {
                ps1 = con.prepareStatement(sqlQueryCol);
                rs1 = ps1.executeQuery();
                while (rs1.next()) {
                    String extractedColumn = rs1.getString(1);
                    if (extractedColumn != null) {
                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DOES_INDEX_EXISTS_ISERIES], "extractedColumn : " + extractedColumn);
                        }
                        if ((extractedColumn.indexOf(idCol.toUpperCase()) != -1) ||
                                (extractedColumn.indexOf(propCol.toUpperCase()) != -1) ||
                                   (extractedColumn.indexOf(appCol.toUpperCase()) != -1)) {

                            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DOES_INDEX_EXISTS_ISERIES], "column is found : " + extractedColumn);
                            }
                            counter++;
                        }// end if (extractedColumn......)
                    }// end if (extractedColumn != null)
                }//end while
                 //we issue the warning here as if the index exists and any
                 //index column is missing
                if (counter < 3) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.WARNING, methodClassName, methodNames[DOES_INDEX_EXISTS_ISERIES], "DatabaseHashMap.IndexIncorrect");
                }
            } catch (Throwable th) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[DOES_INDEX_EXISTS_ISERIES], "CommonMessage.exception", th);
            } finally {
                if (rs1 != null) {
                    closeResultSet(rs1);
                }
                if (ps1 != null) {
                    closeStatement(ps1);
                }
            }
        }// end  if (indexExists_iSeries)
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[DOES_INDEX_EXISTS_ISERIES], indexExists_iSeries);
        }
        return indexExists_iSeries;
    }

    /**
     * PK56991 we are checking if the session table in DB2 is marked as Volatile.
     * We are skipping the same check on iSeries and zOS as VOLATILE is only
     * compatiable and improve DB2 performance on Linux, Unix and Windows
     * platforms (LUW) as information provided by Mark Anderson (
     * DB2 chief architect on iSeries)
     *
     * @param con
     * @return
     */

    private boolean isTableMarkedVolatile(Connection con) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[IS_TABLE_MARKED_VOLATILE]);
        }
        boolean isMarkedVolatile = false;

        String tblName = tableName, qualifierName = null, sqlQuery = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        tblName = tblName.toUpperCase();

        if (_smc.isUsingCustomSchemaName()) { //PM27191
            qualifierName = qualifierNameWhenCustomSchemaIsSet;
        } else if (dbid != null) {
            qualifierName = dbid.toUpperCase();
        }
        sqlQuery = "select 1 from syscat.tables " +
                   "where TabName = '" + tblName + "' and Volatile = 'C' ";
        if (qualifierName != null)
            sqlQuery += " and tabschema = '" + qualifierName + "'";
        sqlQuery += " for read only";
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[CREATE_TABLE], "Sql: " + sqlQuery);
        }
        try {
            ps = con.prepareStatement(sqlQuery);
            rs = ps.executeQuery();
            if (rs.next()) {
                isMarkedVolatile = true;
            }
        } catch (Throwable th) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[CREATE_TABLE], "CommonMessage.exception", th);
        } finally {
            if (rs != null) {
                closeResultSet(rs);
            }
            if (ps != null) {
                closeStatement(ps);
            }
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[IS_TABLE_MARKED_VOLATILE], isMarkedVolatile);
        }
        return isMarkedVolatile;
    }
    //PK56991 ends
}

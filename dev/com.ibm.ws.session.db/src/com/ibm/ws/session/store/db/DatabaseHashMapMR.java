/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
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
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.logging.Level;

import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStatistics;
import com.ibm.ws.session.store.common.BackedSession;
import com.ibm.wsspi.session.IStore;

/*
 * This is the "hashtable" implementation for the direct to database session
 * multirow model.
 */

public class DatabaseHashMapMR extends DatabaseHashMap {

    String findProps;
    String upAnyProp;
    String insAnyProp;
    int smallPropType;
    int mediumPropType;
    int largePropType;
    boolean multirowInitialized = false;
    private static final long serialVersionUID = -1215532807296297361L;
    private static final String methodClassName = "DatabaseHashMapMR";

    private static final int HANDLE_PROPERTY_HITS = 0;
    private static final int GET_ALL_VALUES = 1;
    private static final int INIT_MULTIROW_DB_TYPES = 2;
    private static final String methodNames[] = { "handlePropertyHits", "getAllValues", "initMultirowDBTypes"};

    /*
     * Constructor
     */
    public DatabaseHashMapMR(IStore store, SessionManagerConfig smc, DatabaseStoreService databaseStoreService) {
        super(store, smc, databaseStoreService);
        // We know we're running multi-row..if not writeAllProperties and not time-based writes,
        // we must keep the app data tables per thread (rather than per session)
        appDataTablesPerThread = (!_smc.writeAllProperties() && !_smc.getEnableTimeBasedWrite());
    }

    private void initMultirowDBTypes(Connection conn) throws SQLException {

        if (!usingOracle) { // START PM99783
            PreparedStatement testPS  = null;
            try {
                testPS = conn.prepareStatement(upAnyProp);
                smallPropType = testPS.getParameterMetaData().getParameterType(1);
                mediumPropType = testPS.getParameterMetaData().getParameterType(2);
                largePropType = testPS.getParameterMetaData().getParameterType(3);
                multirowInitialized = true;
            } finally {
                if (testPS!=null) {
                    testPS.close();
                }
            }
        } else {  // Oracle DB does not support getParameterType, hence need to find column types old fashion way, code lifted from getTableDefinition and modified

            boolean smallExists = false;
            boolean mediumExists = false;
            boolean largeExists = false;

            DatabaseMetaData dmd = conn.getMetaData();
            String tbName = tableName;
            String qualifierName = null;

            tbName = tbName.toUpperCase(); // note code in getTableDefinition converts tbName and qualifier to uppercase for Oracle case, hence do same thing here to be consistent
            if (dbid != null) {
                qualifierName = dbid.toUpperCase(); // cmd PQ81615
            }

            ResultSet rs1 = dmd.getColumns(null, qualifierName, tbName, "%");
            try {
                while (rs1.next()) {
                        String columnname = rs1.getString("COLUMN_NAME");
                        int columnType = rs1.getInt("DATA_TYPE");
                        if (columnname.equalsIgnoreCase("SMALL")) {
                                smallPropType = columnType;
                                smallExists = true;
                        }
                        if (columnname.equalsIgnoreCase("MEDIUM")) {
                                mediumPropType = columnType;
                                mediumExists = true;
                        }
                        if (columnname.equalsIgnoreCase("LARGE")) {
                                largePropType = columnType;
                                largeExists = true;
                        }
                }

                if (smallExists && mediumExists && largeExists) {
                        multirowInitialized = true;
                } else {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[INIT_MULTIROW_DB_TYPES], "DatabaseHashMap.wrongTableDef");
                        throw new SQLException();
                }
            } finally {
                closeResultSet(rs1);
            }

        } // END PM99783

    }

    /*
     * Handle property hits
     * 325643 - returning a boolean...multirow version always returns true
     * single-row version in BackedHashtable.java returns false if session
     * size is too big
     */
    boolean handlePropertyHits(BackedSession d2, Thread t, int len) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[HANDLE_PROPERTY_HITS]);
        }

        Enumeration vEnum = null;
        boolean doWrite = false;

        Connection conn = null;
        String id = d2.getId();
        String propid = null;
        PreparedStatement batchUpdatePS = null;
        PreparedStatement batchInsertPS = null;
        PreparedStatement batchDeletePS = null;
        LinkedList<DatabaseMRHelper> insertBatch = new LinkedList<DatabaseMRHelper>();

        boolean updateClose = false;
        boolean insertClose = false;
        boolean delClose = false;

        conn = getConnection(false);
        if (conn == null) {
            return true;
        }
        if (!multirowInitialized) {
            try {
                initMultirowDBTypes(conn);
            } catch (SQLException e) {
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "problem getting types", e); //PM99783
                }
                if (conn != null)
                    closeConnection(conn);
                return false;
            }
        }

        try {
            // we are not synchronized here - were not in old code either
            Hashtable sht = null;
            if (_smc.writeAllProperties()) {
                Hashtable ht = (Hashtable) d2.getSwappableData();
                vEnum = ht.keys();
                doWrite = true;
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "doing app changes for ALL mSwappable Data " + ht.toString());
                }
            } else {
                if (d2.appDataChanges != null) {
                    if (appDataTablesPerThread) {
                        if ((sht = (Hashtable) d2.appDataChanges.get(t)) != null) {
                            doWrite = true;
                            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "doing app changes for " + id + " on thread "
                                                                                                                                    + t);
                            }
                            if (sht != null) {
                                vEnum = sht.keys();
                            } else {
                                vEnum = (new Hashtable()).keys();
                            }
                        }
                    } else { // appDataTablesPerSession
                        doWrite = true;
                        vEnum = d2.appDataChanges.keys();
                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "doing app changes for TimeBasedWrite");
                        }
                    }
                }

            }

            if (doWrite) {
                int enumCount = 0;
                batchUpdatePS = conn.prepareStatement(upAnyProp);
                while (vEnum.hasMoreElements()) {
                    enumCount++;
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "enumCount is now " + enumCount);
                    }
                    long startTime = System.currentTimeMillis();
                    //*start *dbc2.2

                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "about to do vEnum.nextElement");
                    }

                    propid = (String) vEnum.nextElement();
                    if (id.equals(propid)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.WARNING, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "DatabaseHashMapMR.AttributeEqualsSessionIdWarning",
                                                            propid);
                        continue;
                    } else {
                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "it worked and the propid is: " + propid);
                        }
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    ObjectOutputStream oos = createObjectOutputStream(baos);
                    oos.writeObject(d2.getSwappableData().get(propid));
                    oos.flush();

                    int size = baos.size();
                    byte[] objbuf = baos.toByteArray();
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "size for " + propid + " is " + size);
                    }

                    oos.close();
                    baos.close();

                    int rowsRet = 0;

                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "before update " + propid + " for session " + id);
                    }

                    // based on the amount of data, we choose whether to go to the
                    // small, medium, or large columns (nulling out whatever is
                    // in the other columns).  We first attempt an update.  If a
                    // row count > 0 is returned, we are done.  Otherwise, the
                    // row does not yet exist so we insert.

                    if (size <= smallColSize) {
                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "storing in small which can handle up to "
                                                                                                                                + smallColSize + " bytes");
                        }

                        if (!usingInformix) {
                            batchUpdatePS.setBytes(1, objbuf);
                            batchUpdatePS.setNull(2, mediumPropType);
                            batchUpdatePS.setNull(3, largePropType);
                        } else {
                            ByteArrayInputStream bis = new ByteArrayInputStream(objbuf);
                            batchUpdatePS.setBinaryStream(1, bis, objbuf.length);
                            batchUpdatePS.setNull(2, mediumPropType);
                            batchUpdatePS.setNull(3, largePropType);
                        }
                        batchUpdatePS.setString(4, id);
                        batchUpdatePS.setString(5, propid);
                        batchUpdatePS.setString(6, getAppName());
                        batchUpdatePS.addBatch();
                        //in case we need to do an insert on this afterwards
                        //<string, string, byte[]>
                        DatabaseMRHelper helper = new DatabaseMRHelper();
                        helper.setId(id);
                        helper.setPropId(propid);
                        helper.setObject(objbuf);
                        if (usingInformix) {
                            helper.setUseStream(true);
                        }
                        helper.setAppName(getAppName());
                        helper.setSize(DatabaseMRHelper.SMALL);
                        insertBatch.add(helper);

                        // note, the old value in the medium column is left alone
                        // since further access when look at the small column first
                    } else if (size <= mediumColSize) {
                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "storing in medium which can handle up to "
                                                                                                                                + mediumColSize + " bytes");
                        }

                        if ((!usingInformix) && (!_smc.isUseOracleBlob())) {
                            batchUpdatePS.setNull(1, smallPropType);
                            batchUpdatePS.setBytes(2, objbuf);
                            batchUpdatePS.setNull(3, largePropType);
                        } else {
                            ByteArrayInputStream bis = new ByteArrayInputStream(objbuf);
                            batchUpdatePS.setNull(1, smallPropType);
                            batchUpdatePS.setBinaryStream(2, bis, objbuf.length);
                            batchUpdatePS.setNull(3, largePropType);
                        }
                        batchUpdatePS.setString(4, id);
                        batchUpdatePS.setString(5, propid);
                        batchUpdatePS.setString(6, getAppName());

                        batchUpdatePS.addBatch();
                        DatabaseMRHelper helper = new DatabaseMRHelper();
                        helper.setId(id);
                        helper.setPropId(propid);
                        helper.setObject(objbuf);
                        if (usingInformix || _smc.isUseOracleBlob()) {
                            helper.setUseStream(true);
                        }
                        helper.setAppName(getAppName());
                        helper.setSize(DatabaseMRHelper.MEDIUM);
                        insertBatch.add(helper);
                    } else if (size <= largeColSize) {
                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "storing in large which can handle up to "
                                                                                                                                + largeColSize + " bytes");
                        }
                        // note, the old value in the largecolumn is left alone
                        // since further access will look at the small column and then the
                        // medium column prior to the large column

                        if (!usingInformix) {
                            batchUpdatePS.setNull(1, smallPropType);
                            batchUpdatePS.setNull(2, mediumPropType);
                            batchUpdatePS.setBytes(3, objbuf);
                        } else {
                            ByteArrayInputStream bis = new ByteArrayInputStream(objbuf);
                            batchUpdatePS.setNull(1, smallPropType);
                            batchUpdatePS.setNull(2, mediumPropType);
                            batchUpdatePS.setBinaryStream(3, bis, objbuf.length);
                        }
                        batchUpdatePS.setString(4, id);
                        batchUpdatePS.setString(5, propid);
                        batchUpdatePS.setString(6, getAppName());

                        batchUpdatePS.addBatch();

                        DatabaseMRHelper helper = new DatabaseMRHelper();
                        helper.setId(id);
                        helper.setPropId(propid);
                        helper.setObject(objbuf);
                        if (usingInformix) {
                            //ByteArrayInputStream bis = new ByteArrayInputStream(objbuf);
                            //inssps.setBinaryStream(3, bis, objbuf.length);
                            helper.setUseStream(true);
                        }
                        helper.setAppName(getAppName());
                        helper.setSize(DatabaseMRHelper.LARGE);
                        insertBatch.add(helper);
                    } else { // too big 325643
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "DatabaseHashMapMR.db2LongVarCharErr");
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "DatabaseHashMapMR.propertyTooBig", propid);
                    }

                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "after commit " + propid + " for session " + id);
                    }
                    SessionStatistics pmiStats = _iStore.getSessionStatistics();
                    if (pmiStats != null) {
                        pmiStats.writeTimes(objbuf.length, System.currentTimeMillis() - startTime);
                    }

                }
                int updateCount = 0; //PM99783
                int[] results = null;
                if (enumCount>0) {
                    results = batchUpdatePS.executeBatch();
                    if (usingOracle) { //PM99783 when on Oracle DB, executeBatch() returns -2 (SUCCESS_NO_INFO), might be related to BatchPerformanceWorkaround Oracle property, hence we getUpdateCount instead
                        updateCount = batchUpdatePS.getUpdateCount();
                    }
                } else {
                    results = new int[0]; //skip trying to insert
                }
                batchUpdatePS.close();
                updateClose=true;
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "after " + results.length + " batch update(s), total update count : " + updateCount);
                }
                boolean needToInsert = false;
                int batchInsertPSCount = results.length;
                // PI53220 Oracle 11 returns -2 (SUCCESS_NO_INFO); PI57327 Oracle 12 returns successful results.
                if (results[0] == java.sql.Statement.SUCCESS_NO_INFO && updateCount > 0 && (_smc.writeAllProperties() || batchInsertPSCount == updateCount))
                {
                    batchInsertPSCount = batchInsertPSCount - updateCount; // Oracle only : when Write All option or no insert cases
                }
                for (int i=0;i<batchInsertPSCount;i++) {
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "For batch # " + (i+1) + ", update result : " + results[i]);
                    }
                    if (results[i]<=0) { //no rows updated (PM99783)
                        if (!needToInsert) {
                            needToInsert = true;
                            batchInsertPS = conn.prepareStatement(insAnyProp);
                        }
                        DatabaseMRHelper updateRow = insertBatch.get(i);
                        batchInsertPS.setString(1, updateRow.getId());
                        batchInsertPS.setString(2, updateRow.getPropId());
                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "Insert attribute " + updateRow.getPropId());
                        }
                        switch (updateRow.getSize()) {
                        case DatabaseMRHelper.SMALL:
                            if (!updateRow.isUseStream()) {
                                batchInsertPS.setBytes(3, updateRow.getObject());
                                batchInsertPS.setNull(4, mediumPropType);
                                batchInsertPS.setNull(5, largePropType);
                            } else {
                                byte[] objbuf = updateRow.getObject();
                                ByteArrayInputStream bis = new ByteArrayInputStream(objbuf);
                                batchInsertPS.setBinaryStream(3, bis, objbuf.length);
                                batchInsertPS.setNull(4, mediumPropType);
                                batchInsertPS.setNull(5, largePropType);
                            }
                            break;
                        case DatabaseMRHelper.MEDIUM:
                            if (!updateRow.isUseStream()) {
                                batchInsertPS.setNull(3, smallPropType);
                                batchInsertPS.setBytes(4, updateRow.getObject());
                                batchInsertPS.setNull(5, largePropType);
                            } else {
                                byte[] objbuf = updateRow.getObject();
                                ByteArrayInputStream bis = new ByteArrayInputStream(objbuf);
                                batchInsertPS.setNull(3, smallPropType);
                                batchInsertPS.setBinaryStream(4, bis, objbuf.length);
                                batchInsertPS.setNull(5, largePropType);
                            }
                            break;
                        case DatabaseMRHelper.LARGE:
                            if (!updateRow.isUseStream()) {
                                batchInsertPS.setNull(3, smallPropType);
                                batchInsertPS.setNull(4, mediumPropType);
                                batchInsertPS.setBytes(5, updateRow.getObject());
                            } else {
                                byte[] objbuf = updateRow.getObject();
                                ByteArrayInputStream bis = new ByteArrayInputStream(objbuf);
                                batchInsertPS.setNull(3, smallPropType);
                                batchInsertPS.setNull(4, mediumPropType);
                                batchInsertPS.setBinaryStream(5, bis, objbuf.length);
                            }
                            break;
                        default:
                            break;
                        }

                        batchInsertPS.setString(6, updateRow.getAppName());
                        batchInsertPS.addBatch();
                    }
                }
                //try inserts
                results = new int[0];
                if (needToInsert) {
                    try {
                        results = batchInsertPS.executeBatch();
                    } catch (java.sql.BatchUpdateException e) {
                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "Continue insert due to BatchPerformanceWorkaround enabled");
                        }
                    }
                    batchInsertPS.close();
                    insertClose=true;
                }
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    int problemCntr = 0;
                    for (int i:results) {
                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "Insert result : " + i);
                        }
                        if (usingOracle && i == java.sql.Statement.SUCCESS_NO_INFO) { //PM99783
                            //Oracle executeBatch() returns -2 (SUCCESS_NO_INFO)
                        }
                        else if (i < 0) {
                            problemCntr++;
                        }
                    }
                    if (problemCntr>0) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "Problem updating/creating "+ problemCntr + " attributes");
                    }
                }
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "Exitting Loop ... all properties writes are done");
                }
                if (appDataTablesPerThread) {
                    if (d2.appDataChanges != null)
                        d2.appDataChanges.remove(t);
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "remove thread from appDataChanges for thread: " + t);
                    }
                } else { //appDataTablesPerSession
                    if (d2.appDataChanges != null)
                        d2.appDataChanges.clear();
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "clearing appDataChanges");
                    }
                }
            }

            // see if any properties were REMOVED.
            // if so, process them

            Enumeration vEnum2 = null;

            if (d2.appDataRemovals != null) {
                if (!appDataTablesPerThread) { // appDataTablesPerSession
                    vEnum2 = d2.appDataRemovals.keys();
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "doing app removals for " + id + " on ALL threads");
                    }
                } else { //appDataTablesPerThread
                    if ((sht = (Hashtable) d2.appDataRemovals.get(t)) != null) {
                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "doing app removals for " + id + " on thread " + t);
                        }
                        if (sht != null) {
                            vEnum2 = sht.keys();
                        } else {
                            vEnum2 = (new Hashtable()).keys();
                        }
                    }
                }

                if (vEnum2 != null && vEnum2.hasMoreElements()) {
                    batchDeletePS = conn.prepareStatement(delProp);
                    while (vEnum2.hasMoreElements()) {
                        propid = (String) vEnum2.nextElement();
                        if (id.equals(propid)) {
                            //since the attribute was never stored on the database, this will do nothing.
                            //We don't even need a warning because they're removing it.
                            continue;
                        } else {
                            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "deleting prop " + propid + " for session "
                                                                                                                                    + id);
                            }
                        }
                        batchDeletePS.setString(1, id);
                        batchDeletePS.setString(2, propid);
                        batchDeletePS.setString(3, getAppName()); //*dbc2.2
                        batchDeletePS.addBatch();

                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "after remove, before commit " + propid
                                                                                                                                + " for session " + id);
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "after commit " + propid + " for session " + id);
                        }
                    }
                    int results[] = batchDeletePS.executeBatch();
                    batchDeletePS.close();
                    delClose = true;
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        int problemCntr = 0;
                        for (int i:results) {
                            if (i<=0) {
                                problemCntr++;
                            }
                        }
                        if (problemCntr>0) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "There was a problem deleting " + problemCntr + " rows");
                        }
                    }
                }

                if (!appDataTablesPerThread) { // appDataTablesPerSession
                    if (d2.appDataRemovals != null)
                        d2.appDataRemovals.clear();
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "clearing appDataRemovals");
                    }
                } else { //appDataTablesPerThread
                    if (d2.appDataRemovals != null)
                        d2.appDataRemovals.remove(t);
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "remove thread from appDataRemovals: " + t);
                    }
                }
            }
        } catch (SQLException se) {
            //            if (isStaleConnectionException(se)) {
            //                com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMapMR.handlePropertyHits", "422", d2);
            //                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "StaleConnectionException");
            //            } else {
            com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMapMR.handlePropertyHits", "428", d2);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "DatabaseHashMapMR.propHitErr");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "CommonMessage.exception", se);
            //            }
        } catch (Exception ee) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ee, "com.ibm.ws.session.store.db.DatabaseHashMapMR.handlePropertyHits", "444", d2);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "DatabaseHashMapMR.propHitErr");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[HANDLE_PROPERTY_HITS], "CommonMessage.exception", ee);
        } finally {
            if (!updateClose && batchUpdatePS!=null) {
                closeStatement(batchUpdatePS);
            }
            if (!insertClose && batchInsertPS!=null) {
                closeStatement(batchInsertPS);
            }
            if (!delClose && batchDeletePS != null) {
                closeStatement(batchDeletePS);
            }
            if (conn != null) {
                closeConnection(conn);
            }
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[HANDLE_PROPERTY_HITS], Boolean.valueOf(true));
        }
        return true; // 325643
    }

    /*
     * Initialize SQL Strings
     */
    protected void initializeSQL_Strings() {
        super.initializeSQL_Strings();

        findProps = "select propid, small, medium, large from " + tableName + "  where id = ? and propid <> ? and appname = ?";

        findAllKeys = "select propid from  " + tableName + "  where id = ? and propid <> id and appname = ?";

//        upSmProp = "update " + tableName + " set small = ?, medium = NULL, large = NULL where id = ? and propid = ? and appname = ?";
//        upMedProp = "update " + tableName + " set small = NULL, medium = ?, large = NULL where id = ? and propid = ? and appname = ?";
//        upLgProp = "update " + tableName + " set small = NULL, medium = NULL, large = ? where id = ? and propid = ? and appname = ?";

        upAnyProp = "update " + tableName + " set small = ?, medium = ?, large = ? where id = ? and propid = ? and appname = ?";
        insAnyProp  = "insert into " + tableName + " (id, propid, small, medium, large, appname) values (?, ?, ?, ?, ?, ?)";

        insNoProp = "insert into " + tableName + " (id, propid, appname, listenercnt, lastaccess, creationtime, maxinactivetime, username) values (?, ?, ?, ?, ?, ?, ?, ?)";
        findProps = "select propid, small, medium, large from " + tableName + " where id = ? and propid <> ? and appname = ? "; //*dbc2.2
    }

    /*
     * To load all the properties
     */
    protected Object getAllValues(BackedSession sess) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[GET_ALL_VALUES]);
        }

        Connection conn = getConnection(false);
        String id = sess.getId();

        if (conn == null) {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[GET_ALL_VALUES], null);
            }
            return null;
        }

        long startTime = System.currentTimeMillis();
        long readSize = 0;

        PreparedStatement s = null;
        ResultSet rs = null;
        Hashtable h = new Hashtable();
        try {
            s = conn.prepareStatement(findProps);
            s.setString(1, id);
            s.setString(2, id);
            s.setString(3, getAppName());
            rs = s.executeQuery();

            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_ALL_VALUES], "querying database for properties");
            }

            while (rs.next()) {

                // start PM36949: If an attribute is already in appDataRemovals or appDataChanges, then the attribute was already retrieved from the db.  Skip retrieval from the db here.
                if (sess.appDataRemovals != null && sess.appDataRemovals.containsKey(rs.getString(DatabaseHashMap.propCol))) { //

                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_ALL_VALUES], "Found property: "
                                                                                                                      + rs.getString(DatabaseHashMap.propCol)
                                                                                                                      + " in appDataRemovals, skipping db query for this prop");
                    }
                    continue;

                } else if (sess.appDataChanges != null && sess.appDataChanges.containsKey(rs.getString(DatabaseHashMap.propCol))) {

                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_ALL_VALUES], "Found property: "
                                                                                                                      + rs.getString(DatabaseHashMap.propCol)
                                                                                                                      + " in appDataChanges, skipping db query for this prop");
                    }
                    continue;
                } // end PM36949

                byte[] b = rs.getBytes(smallCol);
                if (b == null) {
                    b = rs.getBytes(medCol);
                }
                if (b == null) {
                    b = rs.getBytes(lgCol);
                }

                ByteArrayInputStream bais = new ByteArrayInputStream(b);
                BufferedInputStream bis = new BufferedInputStream(bais);
                Object obj = null;
                try {
                    obj = ((DatabaseStore) getIStore()).getLoader().loadObject(bis);
                    readSize += b.length;
                } catch (ClassNotFoundException ce) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ce, "com.ibm.ws.session.store.db.DatabaseHashMapMR.getAllValues", "864", sess);
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_ALL_VALUES], "DatabaseHashMapMR.getSwappableListenersErr");
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_ALL_VALUES], "CommonMessage.sessionid", id);
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_ALL_VALUES], "CommonMessage.exception", ce);
                }

                bis.close();
                bais.close();

                if (obj != null) {
                    if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_ALL_VALUES],
                                                            "put prop in mSwappableData: " + rs.getString(DatabaseHashMap.propCol));
                    }
                    h.put(rs.getString(propCol), obj);
                }
            }
            SessionStatistics pmiStats = _iStore.getSessionStatistics();
            if (pmiStats != null) {
                pmiStats.readTimes(readSize, System.currentTimeMillis() - startTime);
            }
        } catch (SQLException se) {
            com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.session.store.db.DatabaseHashMapMR.getAllValues", "885", sess);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_ALL_VALUES], "DatabaseHashMapMR.checkListErr");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_ALL_VALUES], "CommonMessage.object", sess.toString());
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_ALL_VALUES], "CommonMessage.exception", se);
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.store.db.DatabaseHashMapMR.getAllValues", "892", sess);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_ALL_VALUES], "DatabaseHashMapMR.checkListErr");
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_ALL_VALUES], "CommonMessage.object", sess.toString());
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodNames[GET_ALL_VALUES], "CommonMessage.exception", e);
        } finally {
            if (rs != null)
                closeResultSet(rs);
            if (s != null)
                closeStatement(s);
            closeConnection(conn);
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[GET_ALL_VALUES], h);
        }
        return h;
    }
}

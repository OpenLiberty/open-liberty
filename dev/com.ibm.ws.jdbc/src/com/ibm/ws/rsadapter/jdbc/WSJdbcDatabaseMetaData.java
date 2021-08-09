/*******************************************************************************
 * Copyright (c) 2001, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Wrapper;
import java.util.ArrayList;

import org.osgi.framework.Version;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.AdapterUtil;

/**
 * This class wraps a DatabaseMetaData.
 */
public class WSJdbcDatabaseMetaData extends WSJdbcObject implements DatabaseMetaData {
    private static final TraceComponent tc = Tr.register(WSJdbcDatabaseMetaData.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * Warning: Currently the getJDBCMajor/MinorVersion depends on mDataImpl NOT being
     * reassociated for this object due to caching values.
     */
    protected DatabaseMetaData mDataImpl;

    private int cachedJDBCMajorVerion = -1;

    /**
     * Create a WebSphere DatabaseMetaData wrapper.
     * 
     * @param metaDataImpl the JDBC DatabaseMetaData implementation class to be wrapped.
     * @param connWrapper the WebSphere JDBC Connection wrapper creating this MetaData.
     */
    public WSJdbcDatabaseMetaData(DatabaseMetaData metaDataImpl, WSJdbcConnection connWrapper)
        throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "<init>", AdapterUtil.toString(metaDataImpl), connWrapper);

        mDataImpl = metaDataImpl;
        mcf = connWrapper.mcf;
        init(connWrapper); 
        childWrappers = new ArrayList<Wrapper>(8); 

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>");
    }

    public boolean allProceduresAreCallable() throws SQLException {
        try {
            return mDataImpl.allProceduresAreCallable();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.allProceduresAreCallable", "68", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean allTablesAreSelectable() throws SQLException {
        try {
            return mDataImpl.allTablesAreSelectable();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.allTablesAreSelectable", "88", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        try {
            return mDataImpl.autoCommitFailureClosesAllResultSets();
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".autoCommitFailureClosesAllResultSets", "152", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("DatabaseMetaData.autoCommitFailureClosesAllResultSets", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".autoCommitFailureClosesAllResultSets", "170", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "autoCommitFailureClosesAllResultSets", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".autoCommitFailureClosesAllResultSets", "177", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "autoCommitFailureClosesAllResultSets", err);
            throw err;
        }
    }

    /**
     * Perform any wrapper-specific close logic. This method is called by the default
     * WSJdbcObject close method.
     * 
     * @param closeWrapperOnly boolean flag to indicate that only wrapper-closure activities
     *            should be performed, but close of the underlying object is unnecessary.
     * 
     * @return SQLException the first error to occur while closing the object.
     */
    final protected SQLException closeWrapper(boolean closeWrapperOnly) 
    {
        parentWrapper.childWrapper = null;
        mDataImpl = null;
        return null;
    }

    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        try {
            return mDataImpl.dataDefinitionCausesTransactionCommit();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.dataDefinitionCausesTransactionCommit", "167", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        try {
            return mDataImpl.dataDefinitionIgnoredInTransactions();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.dataDefinitionIgnoredInTransactions", "187", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean deletesAreDetected(int type) throws SQLException {
        try {
            return mDataImpl.deletesAreDetected(type);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.deletesAreDetected", "207", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        try {
            return mDataImpl.doesMaxRowSizeIncludeBlobs();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.doesMaxRowSizeIncludeBlobs", "227", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public ResultSet getBestRowIdentifier(String catalog, String schema, String table,
                                          int scope, boolean nullable) throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getBestRowIdentifier(catalog, schema, table, scope, nullable);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getBestRowIdentifier", "254", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public ResultSet getCatalogs() throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getCatalogs();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getCatalogs", "280", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public String getCatalogSeparator() throws SQLException {
        try {
            return mDataImpl.getCatalogSeparator();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getCatalogSeparator", "300", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public String getCatalogTerm() throws SQLException {
        try {
            return mDataImpl.getCatalogTerm();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getCatalogTerm", "320", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public ResultSet getClientInfoProperties() throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getClientInfoProperties();
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getClientInfoProperties", "383", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("DatabaseMetaData.getClientInfoProperties", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getClientInfoProperties", "418", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getClientInfoProperties", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getClientInfoProperties", "425", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getClientInfoProperties", err);
            throw err;
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public ResultSet getColumnPrivileges(String catalog, String schema,
                                         String table, String colNamePattern) throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getColumnPrivileges(catalog, schema, table, colNamePattern);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getColumnPrivileges", "347", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public ResultSet getColumns(String catalog, String schPattern,
                                String tblNamePattern, String colNamePattern) throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getColumns(catalog, schPattern, tblNamePattern, colNamePattern);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getColumns", "374", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = (Connection) parentWrapper;

        if (state == State.CLOSED || conn == null)
            throw createClosedException("Connection"); 

        return conn;
    }

    /**
     * @return the Connection wrapper for this object, or null if none is available.
     */
    final protected WSJdbcObject getConnectionWrapper() 
    {
        return parentWrapper;
    }

    public ResultSet getCrossReference(String pCatalog, String pSchema,
                                       String pTable, String fCatalog, String fSchema, String fTable) throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getCrossReference(pCatalog, pSchema, pTable, fCatalog, fSchema, fTable);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getCrossReference", "411", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public String getDatabaseProductName() throws SQLException {
        try {
            return mDataImpl.getDatabaseProductName();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getDatabaseProductName", "431", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public String getDatabaseProductVersion() throws SQLException {
        try {
            return mDataImpl.getDatabaseProductVersion();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getDatabaseProductVersion", "451", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getDefaultTransactionIsolation() throws SQLException {
        try {
            return mDataImpl.getDefaultTransactionIsolation();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getDefaultTransactionIsolation", "471", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getDriverMajorVersion() {
        // This method cannot throw SQLException.  We throw RuntimeException if closed.

        try {
            return mDataImpl.getDriverMajorVersion();
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.

            if (state == State.CLOSED)
                throw new RuntimeException(
                                AdapterUtil.getNLSMessage("OBJECT_CLOSED", "Connection"));

            throw nullX;
        }
    }

    public int getDriverMinorVersion() {
        // This method cannot throw SQLException.  We throw RuntimeException if closed.

        try {
            return mDataImpl.getDriverMinorVersion();
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.

            if (state == State.CLOSED)
                throw new RuntimeException(
                                AdapterUtil.getNLSMessage("OBJECT_CLOSED", "Connection"));

            throw nullX;
        }
    }

    public String getDriverName() throws SQLException {
        try {
            return mDataImpl.getDriverName();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getDriverName", "513", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public String getDriverVersion() throws SQLException {
        try {
            return mDataImpl.getDriverVersion();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getDriverVersion", "533", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public ResultSet getExportedKeys(String catalog, String schema, String table)
                    throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getExportedKeys(catalog, schema, table);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getExportedKeys", "560", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public String getExtraNameCharacters() throws SQLException {
        try {
            return mDataImpl.getExtraNameCharacters();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getExtraNameCharacters", "580", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public ResultSet getFunctionColumns(String catalog, String schemaPattern,
                                        String functionNamePattern, String columnNamePattern) throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getFunctionColumns", "734", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("DatabaseMetaData.getFunctionColumns", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getFunctionColumns", "820", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getFunctionColumns", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getFunctionColumns", "827", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getFunctionColumns", err);
            throw err;
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public ResultSet getFunctions(String catalog, String schemaPattern,
                                  String functionNamePattern) throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getFunctions(catalog, schemaPattern, functionNamePattern);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getFunctions", "759", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("DatabaseMetaData.getFunctions", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getFunctions", "942", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getFunctions", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getFunctions", "949", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getFunctions", err);
            throw err;
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public String getIdentifierQuoteString() throws SQLException {
        try {
            return mDataImpl.getIdentifierQuoteString();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getIdentifierQuoteString", "600", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public ResultSet getImportedKeys(String catalog, String schema, String table)
                    throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getImportedKeys(catalog, schema, table);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getImportedKeys", "627", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public ResultSet getIndexInfo(String catalog, String schema, String table,
                                  boolean unique, boolean approximate) throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getIndexInfo(catalog, schema, table, unique, approximate);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getIndexInfo", "654", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    /**
     * @return the underlying JDBC implementation object which we are wrapping.
     */
    final protected Wrapper getJDBCImplObject() 
    {
        return mDataImpl;
    }

    public int getMaxBinaryLiteralLength() throws SQLException {
        try {
            return mDataImpl.getMaxBinaryLiteralLength();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxBinaryLiteralLength", "674", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxCatalogNameLength() throws SQLException {
        try {
            return mDataImpl.getMaxCatalogNameLength();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxCatalogNameLength", "694", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxCharLiteralLength() throws SQLException {
        try {
            return mDataImpl.getMaxCharLiteralLength();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxCharLiteralLength", "714", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxColumnNameLength() throws SQLException {
        try {
            return mDataImpl.getMaxColumnNameLength();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxColumnNameLength", "734", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxColumnsInGroupBy() throws SQLException {
        try {
            return mDataImpl.getMaxColumnsInGroupBy();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxColumnsInGroupBy", "754", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxColumnsInIndex() throws SQLException {
        try {
            return mDataImpl.getMaxColumnsInIndex();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxColumnsInIndex", "774", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxColumnsInOrderBy() throws SQLException {
        try {
            return mDataImpl.getMaxColumnsInOrderBy();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxColumnsInOrderBy", "794", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxColumnsInSelect() throws SQLException {
        try {
            return mDataImpl.getMaxColumnsInSelect();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxColumnsInSelect", "814", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxColumnsInTable() throws SQLException {
        try {
            return mDataImpl.getMaxColumnsInTable();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxColumnsInTable", "834", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxConnections() throws SQLException {
        try {
            return mDataImpl.getMaxConnections();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxConnections", "854", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxCursorNameLength() throws SQLException {
        try {
            return mDataImpl.getMaxCursorNameLength();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxCursorNameLength", "874", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxIndexLength() throws SQLException {
        try {
            return mDataImpl.getMaxIndexLength();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxIndexLength", "894", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxProcedureNameLength() throws SQLException {
        try {
            return mDataImpl.getMaxProcedureNameLength();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxProcedureNameLength", "914", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxRowSize() throws SQLException {
        try {
            return mDataImpl.getMaxRowSize();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxRowSize", "934", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxSchemaNameLength() throws SQLException {
        try {
            return mDataImpl.getMaxSchemaNameLength();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxSchemaNameLength", "954", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxStatementLength() throws SQLException {
        try {
            return mDataImpl.getMaxStatementLength();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxStatementLength", "974", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxStatements() throws SQLException {
        try {
            return mDataImpl.getMaxStatements();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxStatements", "994", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxTableNameLength() throws SQLException {
        try {
            return mDataImpl.getMaxTableNameLength();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxTableNameLength", "1014", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxTablesInSelect() throws SQLException {
        try {
            return mDataImpl.getMaxTablesInSelect();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxTablesInSelect", "1034", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxUserNameLength() throws SQLException {
        try {
            return mDataImpl.getMaxUserNameLength();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getMaxUserNameLength", "1054", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public String getNumericFunctions() throws SQLException {
        try {
            return mDataImpl.getNumericFunctions();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getNumericFunctions", "1088", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public ResultSet getPrimaryKeys(String catalog, String schema, String table)
                    throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getPrimaryKeys(catalog, schema, table);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getPrimaryKeys", "1115", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public ResultSet getProcedureColumns(String catalog, String schemaPattern,
                                         String procNamePattern, String colNamePattern) throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getProcedureColumns(catalog, schemaPattern, procNamePattern, colNamePattern);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getProcedureColumns", "1143", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public ResultSet getProcedures(String catalog, String schemaPattern,
                                   String procNamePattern) throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getProcedures(catalog, schemaPattern, procNamePattern);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getProcedures", "1170", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public String getProcedureTerm() throws SQLException {
        try {
            return mDataImpl.getProcedureTerm();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getProcedureTerm", "1190", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public RowIdLifetime getRowIdLifetime() throws SQLException {
        try {
            return mDataImpl.getRowIdLifetime();
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getRowIdLifetime", "1437", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("DatabaseMetaData.getRowIdLifetime", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getRowIdLifetime", "1453", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getRowIdLifetime", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getRowIdLifetime", "1460", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getRowIdLifetime", err);
            throw err;
        }
    }

    public ResultSet getSchemas() throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getSchemas();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getSchemas", "1216", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getSchemas(catalog, schemaPattern);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getSchemas", "1452", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("DatabaseMetaData.getSchemas", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getSchemas", "1713", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getSchemas", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getSchemas", "1720", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getSchemas", err);
            throw err;
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public String getSchemaTerm() throws SQLException {
        try {
            return mDataImpl.getSchemaTerm();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getSchemaTerm", "1236", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public String getSearchStringEscape() throws SQLException {
        try {
            return mDataImpl.getSearchStringEscape();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getSearchStringEscape", "1256", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public String getSQLKeywords() throws SQLException {
        try {
            return mDataImpl.getSQLKeywords();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getSQLKeywords", "1276", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public String getStringFunctions() throws SQLException {
        try {
            return mDataImpl.getStringFunctions();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getStringFunctions", "1296", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public String getSystemFunctions() throws SQLException {
        try {
            return mDataImpl.getSystemFunctions();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getSystemFunctions", "1316", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public ResultSet getTablePrivileges(String catalog, String schemaPattern,
                                        String tableNamePattern) throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getTablePrivileges(catalog, schemaPattern, tableNamePattern);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getTablePrivileges", "1343", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public ResultSet getTables(String catalog, String schemaPattern,
                               String tableNamePattern, String[] types) throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getTables(catalog, schemaPattern, tableNamePattern, types);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getTables", "1370", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public ResultSet getTableTypes() throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getTableTypes();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getTableTypes", "1396", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public String getTimeDateFunctions() throws SQLException {
        try {
            return mDataImpl.getTimeDateFunctions();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getTimeDateFunctions", "1415", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @return the trace component for the WSJdbcDatabaseMetaData.
     */
    final protected TraceComponent getTracer() 
    {
        return tc;
    }

    public ResultSet getTypeInfo() throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getTypeInfo();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getTypeInfo", "1450", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public ResultSet getUDTs(String catalog, String schemaPattern,
                             String typeNamePattern, int[] types) throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getUDTs(catalog, schemaPattern, typeNamePattern, types);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getUDTs", "1477", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public String getURL() throws SQLException {
        try {
            return mDataImpl.getURL();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getURL", "1497", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public String getUserName() throws SQLException {
        try {
            return mDataImpl.getUserName();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getUserName", "1517", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public ResultSet getVersionColumns(String catalog, String schema, String table)
                    throws SQLException {
        ResultSet rset;

        try {
            rset = mDataImpl.getVersionColumns(catalog, schema, table);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getVersionColumns", "1544", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }

    public boolean insertsAreDetected(int type) throws SQLException {
        try {
            return mDataImpl.insertsAreDetected(type);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.insertsAreDetected", "1564", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Collects FFDC information specific to this JDBC wrapper. Formats this information to
     * the provided FFDC logger. This method is used by introspectAll to collect any wrapper
     * specific information.
     * 
     * @param info FFDCLogger on which to record the FFDC information.
     */
    @Override
    protected void introspectWrapperSpecificInfo(com.ibm.ws.rsadapter.FFDCLogger info) 
    {
        info.append("Underlying DatabaseMetaData: " + AdapterUtil.toString(mDataImpl),
                    mDataImpl);
    }

    public boolean isCatalogAtStart() throws SQLException {
        try {
            return mDataImpl.isCatalogAtStart();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.isCatalogAtStart", "1584", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean isReadOnly() throws SQLException {
        try {
            return mDataImpl.isReadOnly();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.isReadOnly", "1604", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean nullPlusNonNullIsNull() throws SQLException {
        try {
            return mDataImpl.nullPlusNonNullIsNull();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.nullPlusNonNullIsNull", "1624", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean nullsAreSortedAtEnd() throws SQLException {
        try {
            return mDataImpl.nullsAreSortedAtEnd();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.nullsAreSortedAtEnd", "1644", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean nullsAreSortedAtStart() throws SQLException {
        try {
            return mDataImpl.nullsAreSortedAtStart();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.nullsAreSortedAtStart", "1664", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean nullsAreSortedHigh() throws SQLException {
        try {
            return mDataImpl.nullsAreSortedHigh();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.nullsAreSortedHigh", "1684", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean nullsAreSortedLow() throws SQLException {
        try {
            return mDataImpl.nullsAreSortedLow();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.nullsAreSortedLow", "1704", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean othersDeletesAreVisible(int type) throws SQLException {
        try {
            return mDataImpl.othersDeletesAreVisible(type);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.othersDeletesAreVisible", "1724", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean othersInsertsAreVisible(int type) throws SQLException {
        try {
            return mDataImpl.othersInsertsAreVisible(type);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.othersInsertsAreVisible", "1744", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean othersUpdatesAreVisible(int type) throws SQLException {
        try {
            return mDataImpl.othersUpdatesAreVisible(type);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.othersUpdatesAreVisible", "1764", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean ownDeletesAreVisible(int type) throws SQLException {
        try {
            return mDataImpl.ownDeletesAreVisible(type);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.ownDeletesAreVisible", "1784", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean ownInsertsAreVisible(int type) throws SQLException {
        try {
            return mDataImpl.ownInsertsAreVisible(type);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.ownInsertsAreVisible", "1804", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean ownUpdatesAreVisible(int type) throws SQLException {
        try {
            return mDataImpl.ownUpdatesAreVisible(type);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.ownUpdatesAreVisible", "1824", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }


    public boolean storesLowerCaseIdentifiers() throws SQLException {
        try {
            return mDataImpl.storesLowerCaseIdentifiers();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.storesLowerCaseIdentifiers", "1844", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @param runtimeX a RuntimeException which occurred, indicating the wrapper may be closed.
     * 
     * @throws SQLRecoverableException if the wrapper is closed and exception mapping is disabled. 
     * 
     * @return the RuntimeException to throw if it isn't.
     */
    final protected RuntimeException runtimeXIfNotClosed(RuntimeException runtimeX) throws SQLException 
    {
        if (state == State.CLOSED)
            throw createClosedException("Connection"); 

        return runtimeX;
    }

    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        try {
            return mDataImpl.storesLowerCaseQuotedIdentifiers();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.storesLowerCaseQuotedIdentifiers", "1864", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean storesMixedCaseIdentifiers() throws SQLException {
        try {
            return mDataImpl.storesMixedCaseIdentifiers();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.storesMixedCaseIdentifiers", "1885", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        try {
            return mDataImpl.storesMixedCaseQuotedIdentifiers();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.storesMixedCaseQuotedIdentifiers", "1905", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean storesUpperCaseIdentifiers() throws SQLException {
        try {
            return mDataImpl.storesUpperCaseIdentifiers();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.storesUpperCaseIdentifiers", "1925", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        try {
            return mDataImpl.storesUpperCaseQuotedIdentifiers();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.storesUpperCaseQuotedIdentifiers", "1945", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        try {
            return mDataImpl.supportsAlterTableWithAddColumn();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsAlterTableWithAddColumn", "1965", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        try {
            return mDataImpl.supportsAlterTableWithDropColumn();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsAlterTableWithDropColumn", "1985", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        try {
            return mDataImpl.supportsANSI92EntryLevelSQL();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsANSI92EntryLevelSQL", "2005", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsANSI92FullSQL() throws SQLException {
        try {
            return mDataImpl.supportsANSI92FullSQL();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsANSI92FullSQL", "2025", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        try {
            return mDataImpl.supportsANSI92IntermediateSQL();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsANSI92IntermediateSQL", "2045", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsBatchUpdates() throws SQLException {
        try {
            return mDataImpl.supportsBatchUpdates();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsBatchUpdates", "2065", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        try {
            return mDataImpl.supportsCatalogsInDataManipulation();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsCatalogsInDataManipulation", "2085", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        try {
            return mDataImpl.supportsCatalogsInIndexDefinitions();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsCatalogsInIndexDefinitions", "2105", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        try {
            return mDataImpl.supportsCatalogsInPrivilegeDefinitions();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsCatalogsInPrivilegeDefinitions", "2125", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        try {
            return mDataImpl.supportsCatalogsInProcedureCalls();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsCatalogsInProcedureCalls", "2145", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        try {
            return mDataImpl.supportsCatalogsInTableDefinitions();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsCatalogsInTableDefinitions", "2165", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsColumnAliasing() throws SQLException {
        try {
            return mDataImpl.supportsColumnAliasing();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsColumnAliasing", "2185", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsConvert() throws SQLException {
        try {
            return mDataImpl.supportsConvert();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsConvert", "2205", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsConvert(int fromType, int toType) throws SQLException {
        try {
            return mDataImpl.supportsConvert(fromType, toType);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsConvert", "2225", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsCoreSQLGrammar() throws SQLException {
        try {
            return mDataImpl.supportsCoreSQLGrammar();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsCoreSQLGrammar", "2245", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsCorrelatedSubqueries() throws SQLException {
        try {
            return mDataImpl.supportsCorrelatedSubqueries();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsCorrelatedSubqueries", "2265", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
        try {
            return mDataImpl.supportsDataDefinitionAndDataManipulationTransactions();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsDataDefinitionAndDataManipulationTransactions", "2286", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
        try {
            return mDataImpl.supportsDataManipulationTransactionsOnly();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsDataManipulationTransactionsOnly", "2306", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        try {
            return mDataImpl.supportsDifferentTableCorrelationNames();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsDifferentTableCorrelationNames", "2326", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsExpressionsInOrderBy() throws SQLException {
        try {
            return mDataImpl.supportsExpressionsInOrderBy();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsExpressionsInOrderBy", "2346", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsExtendedSQLGrammar() throws SQLException {
        try {
            return mDataImpl.supportsExtendedSQLGrammar();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsExtendedSQLGrammar", "2366", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsFullOuterJoins() throws SQLException {
        try {
            return mDataImpl.supportsFullOuterJoins();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsFullOuterJoins", "2386", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsGroupBy() throws SQLException {
        try {
            return mDataImpl.supportsGroupBy();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsGroupBy", "2406", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsGroupByBeyondSelect() throws SQLException {
        try {
            return mDataImpl.supportsGroupByBeyondSelect();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsGroupByBeyondSelect", "2426", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsGroupByUnrelated() throws SQLException {
        try {
            return mDataImpl.supportsGroupByUnrelated();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsGroupByUnrelated", "2446", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        try {
            return mDataImpl.supportsIntegrityEnhancementFacility();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsIntegrityEnhancementFacility", "2466", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsLikeEscapeClause() throws SQLException {
        try {
            return mDataImpl.supportsLikeEscapeClause();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsLikeEscapeClause", "2486", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsLimitedOuterJoins() throws SQLException {
        try {
            return mDataImpl.supportsLimitedOuterJoins();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsLimitedOuterJoins", "2506", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsMinimumSQLGrammar() throws SQLException {
        try {
            return mDataImpl.supportsMinimumSQLGrammar();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsMinimumSQLGrammar", "2526", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        try {
            return mDataImpl.supportsMixedCaseIdentifiers();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsMixedCaseIdentifiers", "2546", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        try {
            return mDataImpl.supportsMixedCaseQuotedIdentifiers();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsMixedCaseQuotedIdentifiers", "2566", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsMultipleResultSets() throws SQLException {
        try {
            return mDataImpl.supportsMultipleResultSets();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsMultipleResultSets", "2586", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsMultipleTransactions() throws SQLException {
        try {
            return mDataImpl.supportsMultipleTransactions();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsMultipleTransactions", "2606", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsNonNullableColumns() throws SQLException {
        try {
            return mDataImpl.supportsNonNullableColumns();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsNonNullableColumns", "2626", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        try {
            return mDataImpl.supportsOpenCursorsAcrossCommit();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsOpenCursorsAcrossCommit", "2646", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        try {
            return mDataImpl.supportsOpenCursorsAcrossRollback();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsOpenCursorsAcrossRollback", "2666", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        try {
            return mDataImpl.supportsOpenStatementsAcrossCommit();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsOpenStatementsAcrossCommit", "2686", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        try {
            return mDataImpl.supportsOpenStatementsAcrossRollback();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsOpenStatementsAcrossRollback", "2706", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsOrderByUnrelated() throws SQLException {
        try {
            return mDataImpl.supportsOrderByUnrelated();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsOrderByUnrelated", "2726", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsOuterJoins() throws SQLException {
        try {
            return mDataImpl.supportsOuterJoins();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsOuterJoins", "2746", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsPositionedDelete() throws SQLException {
        try {
            return mDataImpl.supportsPositionedDelete();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsPositionedDelete", "2766", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsPositionedUpdate() throws SQLException {
        try {
            return mDataImpl.supportsPositionedUpdate();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsPositionedUpdate", "2786", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
        try {
            return mDataImpl.supportsResultSetConcurrency(type, concurrency);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsResultSetConcurrency", "2807", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsResultSetType(int type) throws SQLException {
        try {
            return mDataImpl.supportsResultSetType(type);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsResultSetType", "2827", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsSchemasInDataManipulation() throws SQLException {
        try {
            return mDataImpl.supportsSchemasInDataManipulation();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsSchemasInDataManipulation", "2847", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        try {
            return mDataImpl.supportsSchemasInIndexDefinitions();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsSchemasInIndexDefinitions", "2867", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        try {
            return mDataImpl.supportsSchemasInPrivilegeDefinitions();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsSchemasInPrivilegeDefinitions", "2887", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        try {
            return mDataImpl.supportsSchemasInProcedureCalls();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsSchemasInProcedureCalls", "2907", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        try {
            return mDataImpl.supportsSchemasInTableDefinitions();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsSchemasInTableDefinitions", "2927", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsSelectForUpdate() throws SQLException {
        try {
            return mDataImpl.supportsSelectForUpdate();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsSelectForUpdate", "2947", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        try {
            return mDataImpl.supportsStoredFunctionsUsingCallSyntax();
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".supportsStoredFunctionsUsingCallSyntax", "3241", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("DatabaseMetaData.supportsStoredFunctionsUsingCallSyntax", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".supportsStoredFunctionsUsingCallSyntax", "3544", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "supportsStoredFunctionsUsingCallSyntax", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".supportsStoredFunctionsUsingCallSyntax", "3551", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "supportsStoredFunctionsUsingCallSyntax", err);
            throw err;
        }
    }

    public boolean supportsStoredProcedures() throws SQLException {
        try {
            return mDataImpl.supportsStoredProcedures();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsStoredProcedures", "2967", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsSubqueriesInComparisons() throws SQLException {
        try {
            return mDataImpl.supportsSubqueriesInComparisons();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsSubqueriesInComparisons", "2987", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsSubqueriesInExists() throws SQLException {
        try {
            return mDataImpl.supportsSubqueriesInExists();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsSubqueriesInExists", "3007", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsSubqueriesInIns() throws SQLException {
        try {
            return mDataImpl.supportsSubqueriesInIns();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsSubqueriesInIns", "3027", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        try {
            return mDataImpl.supportsSubqueriesInQuantifieds();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsSubqueriesInQuantifieds", "3047", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsTableCorrelationNames() throws SQLException {
        try {
            return mDataImpl.supportsTableCorrelationNames();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsTableCorrelationNames", "3067", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
        try {
            return mDataImpl.supportsTransactionIsolationLevel(level);
        } catch (SQLException ex) {
            // No FFDC needed... at least until we have a chance to check. 

            if (level == Connection.TRANSACTION_NONE 
                || level == Connection.TRANSACTION_READ_UNCOMMITTED 
                || level == Connection.TRANSACTION_READ_COMMITTED 
                || level == Connection.TRANSACTION_REPEATABLE_READ 
                || level == Connection.TRANSACTION_SERIALIZABLE) 
                FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsTransactionIsolationLevel", "3088", this);

            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsTransactions() throws SQLException {
        try {
            return mDataImpl.supportsTransactions();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsTransactions", "3108", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsUnion() throws SQLException {
        try {
            return mDataImpl.supportsUnion();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsUnion", "3128", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean supportsUnionAll() throws SQLException {
        try {
            return mDataImpl.supportsUnionAll();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsUnionAll", "3148", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean updatesAreDetected(int type) throws SQLException {
        try {
            return mDataImpl.updatesAreDetected(type);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.updatesAreDetected", "3168", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean usesLocalFilePerTable() throws SQLException {
        try {
            return mDataImpl.usesLocalFilePerTable();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.usesLocalFilePerTable", "3188", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean usesLocalFiles() throws SQLException {
        try {
            return mDataImpl.usesLocalFiles();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.usesLocalFiles", "3208", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#getAttributes(String, String, String, String)
     */
    public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
        try {
            ResultSet rset;
            rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(mDataImpl.getAttributes(catalog, schemaPattern, typeNamePattern, attributeNamePattern), this);
            childWrappers.add(rset);
            return rset;
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "getAttributes");

            SQLException se = AdapterUtil.notSupportedX("getAttributes", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getAttributes", "3279", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#getDatabaseMajorVersion()
     */
    public int getDatabaseMajorVersion() throws SQLException {
        try {
            return mDataImpl.getDatabaseMajorVersion();
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "getDatabaseMajorVersion");

            SQLException se = AdapterUtil.notSupportedX("getDatabaseMajorVersion", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getDatabaseMajorVersion", "3292", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#getDatabaseMinorVersion()
     */
    public int getDatabaseMinorVersion() throws SQLException {
        try {
            return mDataImpl.getDatabaseMinorVersion();
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "getDatabaseMinorVersion");

            SQLException se = AdapterUtil.notSupportedX("getDatabaseMinorVersion", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getDatabaseMinorVersion", "3310", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#getJDBCMajorVersion()
     */
    public int getJDBCMajorVersion() throws SQLException {
        try {
            if(cachedJDBCMajorVerion != -1)
                return cachedJDBCMajorVerion;
            int majorVersion_driver = mDataImpl.getJDBCMajorVersion();
            int majorVersion_was = mcf.jdbcRuntime.getVersion().getMajor();
            cachedJDBCMajorVerion = (majorVersion_driver > majorVersion_was) ? majorVersion_was : majorVersion_driver;
            return cachedJDBCMajorVerion;
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "getJDBCMajorVersion");

            SQLException se = AdapterUtil.notSupportedX("getJDBCMajorVersion", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getJDBCMajorVersion", "3325", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#getJDBCMinorVersion()
     */
    public int getJDBCMinorVersion() throws SQLException {
        try {
            // Need to also consider the JDBCMajorVersion in case we have a scenario like: wasVersion=4.1; driverVersion=3.7
            Version jdbcVersion_was = mcf.jdbcRuntime.getVersion();
            int jdbcMinorVersion_driver = mDataImpl.getJDBCMinorVersion();
            int jdbcMajorVersion_driver = (cachedJDBCMajorVerion == -1) ? getJDBCMajorVersion() : cachedJDBCMajorVerion;
            Version jdbcVersion_driver = new Version(jdbcMajorVersion_driver, jdbcMinorVersion_driver, 0);
            
            // Return minor version of the lesser version
            if(jdbcVersion_was.compareTo(jdbcVersion_driver) < 0)
                return jdbcVersion_was.getMinor();
            else
                return jdbcMinorVersion_driver;
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "getJDBCMinorVersion");

            SQLException se = AdapterUtil.notSupportedX("getJDBCMinorVersion", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getJDBCMinorVersion", "3340", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#getResultSetHoldability()
     */
    public int getResultSetHoldability() throws SQLException {
        try {
            return mDataImpl.getResultSetHoldability();
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "getResultSetHoldability");

            SQLException se = AdapterUtil.notSupportedX("getResultSetHoldability", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getResultSetHoldability", "3355", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#getSQLStateType()
     */
    public int getSQLStateType() throws SQLException {
        try {
            return mDataImpl.getSQLStateType();
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "getSQLStateType");

            SQLException se = AdapterUtil.notSupportedX("getSQLStateType", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getSQLStateType", "3370", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#getSuperTables(String, String, String)
     */
    public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        try {
            ResultSet rset;
            rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(mDataImpl.getSuperTables(catalog, schemaPattern, tableNamePattern), this);
            childWrappers.add(rset);
            return rset;
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "getSuperTables");

            SQLException se = AdapterUtil.notSupportedX("getSuperTables", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getSuperTables", "3385", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#getSuperTypes(String, String, String)
     */
    public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
        try {
            ResultSet rset;
            rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(mDataImpl.getSuperTypes(catalog, schemaPattern, typeNamePattern), this);
            childWrappers.add(rset);
            return rset;
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "getSuperTypes");

            SQLException se = AdapterUtil.notSupportedX("getSuperTypes", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.getSuperTypes", "3399", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#locatorsUpdateCopy()
     */
    public boolean locatorsUpdateCopy() throws SQLException {
        try {
            return mDataImpl.locatorsUpdateCopy();
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "locatorsUpdateCopy");

            SQLException se = AdapterUtil.notSupportedX("locatorsUpdateCopy", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.locatorsUpdateCopy", "3414", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys()
     */
    public boolean supportsGetGeneratedKeys() throws SQLException {
        try {
            return mDataImpl.supportsGetGeneratedKeys();
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "supportsGetGeneratedKeys");

            SQLException se = AdapterUtil.notSupportedX("supportsGetGeneratedKeys", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsGetGeneratedKeys", "3429", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsNamedParameters()
     */
    public boolean supportsNamedParameters() throws SQLException {
        try {
            return mDataImpl.supportsNamedParameters();
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "supportsNamedParameters");

            SQLException se = AdapterUtil.notSupportedX("supportsNamedParameters", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsNamedParameters", "3444", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsResultSetHoldability(int)
     */
    public boolean supportsResultSetHoldability(int holdability) throws SQLException {
        try {
            return mDataImpl.supportsResultSetHoldability(holdability);
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "supportsResultSetHoldability");

            SQLException se = AdapterUtil.notSupportedX("supportsResultSetHoldability", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsResultSetHoldability", "3460", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSavepoints()
     */
    public boolean supportsSavepoints() throws SQLException {
        try {
            return mDataImpl.supportsSavepoints();
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "supportsSavepoints");

            SQLException se = AdapterUtil.notSupportedX("supportsSavepoints", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsSavepoints", "3475", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsStatementPooling()
     */
    public boolean supportsStatementPooling() throws SQLException {
        // Always return TRUE because the application server provides statement pooling.

        // The following is from the JDBC 4.0 specification:

        // "An application may find out whether a data source supports statement pooling by
        //  calling the DatabaseMetaData method supportsStatementPooling. If the
        //  return value is true, the application can then choose to use PreparedStatement
        //  objects knowing that they are being pooled."

        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsMultipleOpenResults()
     */
    public boolean supportsMultipleOpenResults() throws SQLException {
        try {
            return mDataImpl.supportsMultipleOpenResults();
        } catch (java.lang.AbstractMethodError ame) {
            // JDBC driver does not support JDBC 3.0
            Tr.warning(tc, "UNSUPPORTED_JDBC30_METHOD", "supportsMultipleOpenResults");

            SQLException se = AdapterUtil.notSupportedX("supportsMultipleOpenResults", ame); 
            throw WSJdbcUtil.mapException(this, se);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData.supportsMultipleOpenResults", "3505", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean generatedKeyAlwaysReturned() throws SQLException {
        // jdbc 4.1 method
        throw new SQLFeatureNotSupportedException();
    }

    public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        // jdbc 4.1 method
        throw new SQLFeatureNotSupportedException();
    }
}

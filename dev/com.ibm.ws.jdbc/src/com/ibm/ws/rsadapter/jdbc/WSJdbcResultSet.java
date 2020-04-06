/*******************************************************************************
 * Copyright (c) 2001, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc;

import java.io.Closeable; 
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob; 
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId; 
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML; 
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Wrapper; 
import java.util.Calendar;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.AdapterUtil;

/**
 * This class wraps a JDBC ResultSet.
 */
public class WSJdbcResultSet extends WSJdbcObject implements ResultSet {
    private static final TraceComponent tc = Tr.register(WSJdbcResultSet.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /** The underlying ResultSet object. */
    protected ResultSet rsetImpl; 

    protected String sql;

    /**
     * Create a WebSphere ResultSet wrapper.
     * 
     * @param rsImpl the JDBC ResultSet implementation class to be wrapped.
     * @param parent the Statement or DatabaseMetaData wrapper creating this ResultSet.
     */
    public WSJdbcResultSet(ResultSet rsImpl, WSJdbcObject parent) 
    {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "<init>", AdapterUtil.toString(rsImpl), parent);

        mcf = parent.mcf;
        rsetImpl = rsImpl;
        init(parent); 

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>");
    }

    /**
     * Moves the cursor to the given row number in the result set.
     * 
     * If the row number is positive, the cursor moves to the given row number with respect to the beginning of the result
     * set. The first row is row 1, the second is row 2, and so on.
     * 
     * If the given row number is negative, the cursor moves to an absolute row position with respect to the end of the
     * result set. For example, calling absolute(-1) positions the cursor on the last row, absolute(-2) indicates the
     * next-to-last row, and so on.
     * 
     * An attempt to position the cursor beyond the first/last row in the result set leaves the cursor before/after the
     * first/last row, respectively.
     * 
     * Note: Calling absolute(1) is the same as calling first(). Calling absolute(-1) is the same as calling last().
     * 
     * @param arg0 given row number
     * @return
     *         true if the cursor is on the result set; false otherwise
     * @throws SQLException if a database access error occurs or row is 0, or result set type is
     *             TYPE_FORWARD_ONLY.
     */
    public boolean absolute(int arg0) throws SQLException {
        try {
            if (dsConfig.get().beginTranForResultSetScrollingAPIs)
                getConnectionWrapper().beginTransactionIfNecessary();

            return rsetImpl.absolute(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.absolute", "93", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Moves the cursor to the end of the result set, just after the last row. Has no effect if the result set contains no rows.
     * 
     * @throws SQLException if a database access error occurs or the result set type is TYPE_FORWARD_ONLY
     */
    public void afterLast() throws SQLException {
        try {
            if (dsConfig.get().beginTranForResultSetScrollingAPIs)
                getConnectionWrapper().beginTransactionIfNecessary();

            rsetImpl.afterLast();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.afterLast", "118", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Moves the cursor to the front of the result set, just before the first row. Has no effect if the result set contains no
     * rows.
     * 
     * @throws SQLException if a database access error occurs or the result set type is TYPE_FORWARD_ONLY
     */
    public void beforeFirst() throws SQLException {
        try {
            if (dsConfig.get().beginTranForResultSetScrollingAPIs)
                getConnectionWrapper().beginTransactionIfNecessary();

            rsetImpl.beforeFirst();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.beforeFirst", "144", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Cancels the updates made to a row. This method may be called after calling an updateXXX method(s)
     * and before calling updateRow to rollback the updates made to a row. If no updates have been made or updateRow
     * has already been called, then this method has no effect.
     * 
     * @throws SQLException if a database access error occurs or if called when on the insert row.
     */
    public void cancelRowUpdates() throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "cancelRowUpdates");

        try {
            parentWrapper.parentWrapper.beginTransactionIfNecessary(); 

            rsetImpl.cancelRowUpdates();

        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.cancelRowUpdates", "183", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * After this call getWarnings returns null until a new warning is reported for this ResultSet.
     * 
     * @throws SQLException if a database access error occurs.
     */
    public void clearWarnings() throws SQLException {
        try {
            rsetImpl.clearWarnings();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.clearWarnings", "208", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
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
    protected SQLException closeWrapper(boolean closeWrapperOnly) 
    {
        // Indicate the result is closed by setting the parent object's result set to null.
        // This will allow us to be garbage collected.

        //  - Since we use childWrapper for the first result set, 
        // so we first compare childWrapper object. 

        if (parentWrapper.childWrapper == this) {
            parentWrapper.childWrapper = null;
        } else {
            // This result set must be stored in childWrappers.
            parentWrapper.childWrappers.remove(this);
        }

        if (closeWrapperOnly) { 
            // skip close of implementation object.  (Statement.getMoreResults will do it.)
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
                Tr.debug(this, tc, "closeWrapper skipping close of ResultSet implementation object"); 
            }
        } else { 
            try // Close the JDBC driver ResultSet implementation object.
            {
                rsetImpl.close();
            } catch (SQLException closeX) {
                FFDCFilter.processException(closeX,
                                            "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.closeWrapper", "246", this);

                rsetImpl = null;
                return WSJdbcUtil.mapException(this, closeX);
            }

        }
        
        // Check if the parent object is a WSJdbcStatement, and closeOnCompletion is enabled
        if(parentWrapper != null && parentWrapper instanceof WSJdbcStatement && ((WSJdbcStatement)parentWrapper).closeOnCompletion)
        {
            WSJdbcStatement parentStmt = (WSJdbcStatement) parentWrapper;
            // If the parent Statement has no more child objects, close the Statement
            if(parentStmt.childWrapper == null && (parentStmt.childWrappers == null || parentStmt.childWrappers.isEmpty()))
            {
                try{
                    parentStmt.close();
                } catch(SQLException closeX){
                    FFDCFilter.processException(closeX,
                                                "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.closeWrapper", "281", this);
                    rsetImpl = null;
                    return WSJdbcUtil.mapException(this, closeX);
                }
            }
        }

        rsetImpl = null;
        return null;
    }

    /**
     * Deletes the current row from the result set and the underlying database. Cannot be
     * called when on the insert row.
     * 
     * @throws SQLException if a database access error occurs or if called when on the insert
     *             row.
     */
    public void deleteRow() throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "deleteRow");

        try {
            parentWrapper.parentWrapper.beginTransactionIfNecessary(); 

            rsetImpl.deleteRow();

        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.deleteRow", "355", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Maps the given Resultset column name to its ResultSet column index.
     * 
     * @param columnName - the name of the column
     * @return
     *         the column index
     * @throws SQLException if a database access error occurs.
     */
    public int findColumn(String arg0) throws SQLException {
        try {
            return rsetImpl.findColumn(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.findColumn", "384", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Moves the cursor to the first row in the result set.
     * 
     * @return
     *         true if the cursor is on a valid row; false if there are no rows in the result set
     * @throws SQLException if a database access error occurs or the result set type is TYPE_FORWARD_ONLY
     */
    public boolean first() throws SQLException {
        try {
            if (dsConfig.get().beginTranForResultSetScrollingAPIs)
                getConnectionWrapper().beginTransactionIfNecessary();

            return rsetImpl.first();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.first", "411", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets an SQL ARRAY value from the current row of this ResultSet object.
     * 
     * @param i - the first column is 1, the second is 2, ...
     * @return
     *         an Array object representing the SQL ARRAY value in the specified column.
     */
    public Array getArray(int arg0) throws SQLException {
        try {
            Array ra = rsetImpl.getArray(arg0); 
            if (ra != null && freeResourcesOnClose)
                arrays.add(ra); 
            return ra; 
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getArray", "438", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets an SQL ARRAY value in the current row of this ResultSet object.
     * 
     * @param colName - the name of the column from which to retrieve the value
     * @return
     *         an Array object representing the SQL ARRAY value in the specified column.
     */
    public Array getArray(String arg0) throws SQLException {
        try {
            Array ra = rsetImpl.getArray(arg0); 
            if (ra != null && freeResourcesOnClose)
                arrays.add(ra); 
            return ra; 
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getArray", "465", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public InputStream getAsciiStream(int arg0) throws SQLException {
        try {
            InputStream stream = rsetImpl.getAsciiStream(arg0); 
            if (stream != null && freeResourcesOnClose)
                resources.add(stream); 
            return stream; 
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getAsciiStream", "501", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public InputStream getAsciiStream(String arg0) throws SQLException {
        try {
            InputStream stream = rsetImpl.getAsciiStream(arg0); 
            if (stream != null && freeResourcesOnClose)
                resources.add(stream); 
            return stream; 
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getAsciiStream", "537", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.math.BigDecimal object with full precision.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return
     *         the column value (full precision); if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs.
     */
    public BigDecimal getBigDecimal(int arg0) throws SQLException {
        try {
            return rsetImpl.getBigDecimal(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getBigDecimal", "566", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.math.BigDecimal object.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            scale - the number of digits to the right of the decimal
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs
     * @deprecated
     */
    public BigDecimal getBigDecimal(int arg0, int arg1) throws SQLException {
        try {
            return rsetImpl.getBigDecimal(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getBigDecimal", "596", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.math.BigDecimal object with full precision.
     * 
     * @param columnName - the column name
     * @return
     *         the column value (full precision); if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs.
     */
    public BigDecimal getBigDecimal(String arg0) throws SQLException {
        try {
            return rsetImpl.getBigDecimal(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getBigDecimal", "625", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.math.BigDecimal object.
     * 
     * @param columnName - the SQL name of the column
     *            scale - the number of digits to the right of the decimal
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs
     * @deprecated
     */
    public BigDecimal getBigDecimal(String arg0, int arg1) throws SQLException {
        try {
            return rsetImpl.getBigDecimal(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getBigDecimal", "656", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public InputStream getBinaryStream(int arg0) throws SQLException {
        try {
            InputStream stream = rsetImpl.getBinaryStream(arg0); 
            if (stream != null && freeResourcesOnClose)
                resources.add(stream); 
            return stream; 
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getBinaryStream", "691", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public InputStream getBinaryStream(String arg0) throws SQLException {
        try {
            InputStream stream = rsetImpl.getBinaryStream(arg0); 
            if (stream != null && freeResourcesOnClose)
                resources.add(stream); 
            return stream; 
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getBinaryStream", "727", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets a BLOB value in the current row of this ResultSet object.
     * 
     * @param i - the first column is 1, the second is 2, ...
     * @return
     *         a Blob object representing the SQL BLOB value in the specified column.
     */
    public Blob getBlob(int arg0) throws SQLException {
        try {
            Blob blob = rsetImpl.getBlob(arg0); 
            if (blob != null && freeResourcesOnClose)
                blobs.add(blob); 
            return blob; 
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getBlob", "754", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java boolean.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return
     *         the column value; if the value is SQL NULL, the result is false
     * @throws SQLException if a database access error occurs
     */
    public boolean getBoolean(int arg0) throws SQLException {
        try {
            return rsetImpl.getBoolean(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getBoolean", "784", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java boolean.
     * 
     * @param columnName - the SQL name of the column
     * @return
     *         the column value; if the value is SQL NULL, the result is false
     * @throws SQLException if a database access error occurs.
     */
    public boolean getBoolean(String arg0) throws SQLException {
        try {
            return rsetImpl.getBoolean(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getBoolean", "813", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java byte.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return
     *         the column value; if the value is SQL NULL, the result is 0
     * @throws SQLException if a database access error occurs
     */
    public byte getByte(int arg0) throws SQLException {
        try {
            return rsetImpl.getByte(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getByte", "842", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java byte.
     * 
     * @param columnName - the SQL name of the column
     * @return
     *         the column value; if the value is SQL NULL, the result is 0
     * @throws SQLException if a database access error occurs.
     */
    public byte getByte(String arg0) throws SQLException {
        try {
            return rsetImpl.getByte(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getByte", "871", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java byte array. The bytes represent the raw values returned by
     * the driver.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs
     */
    public byte[] getBytes(int arg0) throws SQLException {
        try {
            return rsetImpl.getBytes(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getBytes", "901", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java byte array. The bytes represent the raw values returned by
     * the driver.
     * 
     * @param columnName - the SQL name of the column
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs.
     */
    public byte[] getBytes(String arg0) throws SQLException {
        try {
            return rsetImpl.getBytes(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getBytes", "931", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets a BLOB value in the current row of this ResultSet object.
     * 
     * @param colName - the name of the column from which to retrieve the value
     * @return
     *         a Blob object representing the SQL BLOB value in the specified column.
     */
    public Blob getBlob(String arg0) throws SQLException {
        try {
            Blob blob = rsetImpl.getBlob(arg0); 
            if (blob != null && freeResourcesOnClose)
                blobs.add(blob); 
            return blob; 
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getBlob", "958", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.io.Reader.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     */
    public Reader getCharacterStream(int arg0) throws SQLException {
        try {
            Reader reader = rsetImpl.getCharacterStream(arg0); 
            if (reader != null && freeResourcesOnClose)
                resources.add(reader); 
            return reader; 
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getCharacterStream", "983", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.io.Reader.
     * 
     * @param columnName - the name of the column
     * @return
     *         the value in the specified column as a java.io.Reader
     */
    public Reader getCharacterStream(String arg0) throws SQLException {
        try {
            Reader reader = rsetImpl.getCharacterStream(arg0); 
            if (reader != null && freeResourcesOnClose)
                resources.add(reader); 
            return reader; 
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getCharacterStream", "1010", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets a CLOB value in the current row of this ResultSet object.
     * 
     * @param i - the first column is 1, the second is 2, ...
     * @return
     *         a Clob object representing the SQL CLOB value in the specified column.
     */
    public Clob getClob(int arg0) throws SQLException {
        try {
            Clob clob = rsetImpl.getClob(arg0); 
            if (clob != null && freeResourcesOnClose)
                clobs.add(clob); 
            return clob; 
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getClob", "1037", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets a CLOB value in the current row of this ResultSet object.
     * 
     * @param colName - the name of the column from which to retrieve the value
     * @return
     *         a Clob object representing the SQL CLOB value in the specified column.
     */
    public Clob getClob(String arg0) throws SQLException {
        try {
            Clob clob = rsetImpl.getClob(arg0); 
            if (clob != null && freeResourcesOnClose)
                clobs.add(clob); 
            return clob; 
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getClob", "1064", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Returns the concurrency mode of this result set. The concurrency used is determined by the statement that
     * created the result set.
     * 
     * @return
     *         the concurrency type, CONCUR_READ_ONLY or CONCUR_UPDATABLE
     * @throws SQLException if a database access error occurs.
     */
    public int getConcurrency() throws SQLException {
        try {
            return rsetImpl.getConcurrency();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getConcurrency", "1092", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @return the Connection wrapper for this object, or null if none is available.
     * 
     * @throws NullPointerException if the wrapper is closed.
     */
    final protected WSJdbcObject getConnectionWrapper() 
    {
        return parentWrapper.parentWrapper;
    }

    /**
     * Gets the name of the SQL cursor used by this ResultSet.
     * 
     * In SQL, a result table is retrieved through a cursor that is named. The current row of a result can be updated or
     * deleted using a positioned update/delete statement that references the cursor name. To insure that the cursor has the
     * proper isolation level to support update, the cursor's select statement should be of the form 'select for update'. If the
     * 'for update' clause is omitted the positioned updates may fail.
     * 
     * JDBC supports this SQL feature by providing the name of the SQL cursor used by a ResultSet. The current row of a
     * ResultSet is also the current row of this SQL cursor.
     * 
     * Note: If positioned update is not supported a SQLException is thrown
     * 
     * @return
     *         the ResultSet's SQL cursor name
     * @throws SQLException if a database access error occurs.
     */
    public String getCursorName() throws SQLException {
        try {
            return rsetImpl.getCursorName();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getCursorName", "1129", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.sql.Date object.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs
     */
    public Date getDate(int arg0) throws SQLException {
        try {
            return rsetImpl.getDate(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getDate", "1158", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.sql.Date object. This method uses the given
     * calendar to construct an appropriate millisecond value for the Date if the underlying database does not store
     * timezone information.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            cal - the calendar to use in constructing the date
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs.
     */
    public Date getDate(int arg0, Calendar arg1) throws SQLException {
        try {
            return rsetImpl.getDate(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getDate", "1190", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.sql.Date object.
     * 
     * @param columnName - the SQL name of the column
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs
     */
    public Date getDate(String arg0) throws SQLException {
        try {
            return rsetImpl.getDate(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getDate", "1219", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.sql.Date object. This method uses the given calendar to
     * construct an appropriate millisecond value for the Date, if the underlying database does not store timezone
     * information.
     * 
     * @param columnName - the SQL name of the column from which to retrieve the value
     *            cal - the calendar to use in constructing the date
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs.
     */
    public Date getDate(String arg0, Calendar arg1) throws SQLException {
        try {
            return rsetImpl.getDate(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getDate", "1251", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java double.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return
     *         the column value; if the value is SQL NULL, the result is 0
     * @throws SQLException if a database access error occurs
     */
    public double getDouble(int arg0) throws SQLException {
        try {
            return rsetImpl.getDouble(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getDouble", "1280", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java double.
     * 
     * @param columnName - the SQL name of the column
     * @return
     *         the column value; if the value is SQL NULL, the result is 0
     * @throws SQLException if a database access error occurs.
     */
    public double getDouble(String arg0) throws SQLException {
        try {
            return rsetImpl.getDouble(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getDouble", "1309", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Returns the fetch direction for this result set.
     * 
     * @return
     *         the current fetch direction for this result set
     * @throws SQLException if a database access error occurs.
     */
    public int getFetchDirection() throws SQLException {
        try {
            return rsetImpl.getFetchDirection();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getFetchDirection", "1336", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Returns the fetch size for this result set.
     * 
     * @return
     *         the current fetch size for this result set
     * @throws SQLException if a database access error occurs.
     */
    public int getFetchSize() throws SQLException {
        try {
            return rsetImpl.getFetchSize();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getFetchSize", "1363", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java float.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return
     *         the column value; if the value is SQL NULL, the result is 0
     * @throws SQLException if a database access error occurs
     */
    public float getFloat(int arg0) throws SQLException {
        try {
            return rsetImpl.getFloat(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getFloat", "1392", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java float.
     * 
     * @param columnName - the SQL name of the column
     * @return
     *         the column value; if the value is SQL NULL, the result is 0
     * @throws SQLException if a database access error occurs.
     */
    public float getFloat(String arg0) throws SQLException {
        try {
            return rsetImpl.getFloat(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getFloat", "1422", this);
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
    public int getHoldability() throws SQLException {
        try {
            return rsetImpl.getHoldability(); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getHoldability", "1375", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.getHoldability", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getHoldability", "1394", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getHoldability", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getHoldability", "1401", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getHoldability", err);
            throw err;
        }
    }

    /**
     * Gets the value of a column in the current row as a Java int.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return
     *         the column value; if the value is SQL NULL, the result is 0
     * @throws SQLException - if a database access error occurs
     */
    public int getInt(int arg0) throws SQLException {
        try {
            return rsetImpl.getInt(arg0);//@WAN
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getInt", "1451", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java int.
     * 
     * @param columnName - the SQL name of the column
     * @return
     *         the column value; if the value is SQL NULL, the result is 0
     * @throws SQLException if a database access error occurs.
     */
    public int getInt(String arg0) throws SQLException {
        try {
            return rsetImpl.getInt(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getInt", "1480", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @return the underlying JDBC implementation object which we are wrapping.
     */
    final protected Wrapper getJDBCImplObject() 
    {
        return rsetImpl;
    }

    /**
     * Gets the value of a column in the current row as a Java long.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return
     *         the column value; if the value is SQL NULL, the result is 0
     * @throws SQLException if a database access error occurs
     */
    public long getLong(int arg0) throws SQLException {
        try {
            return rsetImpl.getLong(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getLong", "1509", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java long.
     * 
     * @param columnName - the SQL name of the column
     * @return
     *         the column value; if the value is SQL NULL, the result is 0
     * @throws SQLException if a database access error occurs.
     */
    public long getLong(String arg0) throws SQLException {
        try {
            return rsetImpl.getLong(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getLong", "1538", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Retrieves the number, types and properties of a ResultSet's columns.
     * 
     * @return
     *         the description of a ResultSet's columns
     * @throws SQLException if a database access error occurs.
     */
    public ResultSetMetaData getMetaData() throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "getMetaData");

        // First, check if a ResultSetMetaData wrapper for this ResultSet already exists.

        ResultSetMetaData rsetMData = null;

        try // get a meta data
        {
            rsetMData = rsetImpl.getMetaData();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getMetaData", "1579", this);
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "getMetaData", "Exception");
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "getMetaData", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "getMetaData", rsetMData);
        return rsetMData;
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public Reader getNCharacterStream(int i) throws SQLException {
        try {
            return rsetImpl.getNCharacterStream(i); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getNCharacterStream", "1563", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.getNCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getNCharacterStream", "1598", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getNCharacterStream", "1605", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        try {
            return rsetImpl.getNCharacterStream(columnLabel); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getNCharacterStream", "1592", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.getNCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getNCharacterStream", "1643", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getNCharacterStream", "1650", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public NClob getNClob(int columnIndex) throws SQLException {
        try {
            NClob clob = rsetImpl.getNClob(columnIndex); 
            if (clob != null && freeResourcesOnClose)
                clobs.add(clob); 
            return clob; 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getNClob", "1621", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.getNClob", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getNClob", "1637", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getNClob", "1644", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public NClob getNClob(String columnLabel) throws SQLException {
        try {
            NClob clob = rsetImpl.getNClob(columnLabel); 
            if (clob != null && freeResourcesOnClose)
                clobs.add(clob); 
            return clob; 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getNClob", "1664", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.getNClob", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getNClob", "1680", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getNClob", "1687", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public String getNString(int i) throws SQLException {
        try {
            return rsetImpl.getNString(i); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getNString", "1621", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.getNString", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getNString", "1774", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNString", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getNString", "1781", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNString", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public String getNString(String columnLabel) throws SQLException {
        try {
            return rsetImpl.getNString(columnLabel); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getNString", "1650", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.getNString", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getNString", "1819", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNString", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getNString", "1826", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNString", err);
            throw err;
        }
    }

    /**
     * Gets the value of a column in the current row as a Java object.
     * 
     * This method will return the value of the given column as a Java object. The type of the Java object will be the
     * default Java object type corresponding to the column's SQL type, following the mapping for built-in types specified
     * in the JDBC spec.
     * 
     * This method may also be used to read datatabase-specific abstract data types. JDBC 2.0 In the JDBC 2.0 API, the
     * behavior of method getObject is extended to materialize data of SQL user-defined types. When the a column
     * contains a structured or distinct value, the behavior of this method is as if it were a call to: getObject(columnIndex,
     * this.getStatement().getConnection().getTypeMap()).
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return
     *         a java.lang.Object holding the column value
     * @throws SQLException if a database access error occurs.
     */
    public Object getObject(int arg0) throws SQLException {
        try {
            Object result = rsetImpl.getObject(arg0);

            addFreedResources(result);

            return result;
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getObject", "1617", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Returns the value of a column in the current row as a Java object. This method uses the given Map object
     * for the custom mapping of the SQL structured or distinct type that is being retrieved.
     * 
     * @param i - the first column is 1, the second is 2, ...
     *            map - the mapping from SQL type names to Java classes
     * @return
     *         an object representing the SQL value
     */
    public Object getObject(int arg0, Map<String, Class<?>> arg1) throws SQLException 
    {
        try {
            Object result = rsetImpl.getObject(arg0, arg1);

            addFreedResources(result);

            return result;
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getObject", "1646", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java object.
     * 
     * This method will return the value of the given column as a Java object. The type of the Java object will be the
     * default Java object type corresponding to the column's SQL type, following the mapping for built-in types specified
     * in the JDBC spec.
     * 
     * This method may also be used to read datatabase-specific abstract data types. JDBC 2.0 In the JDBC 2.0 API, the
     * behavior of method getObject is extended to materialize data of SQL user-defined types. When the a column
     * contains a structured or distinct value, the behavior of this method is as if it were a call to: getObject(columnIndex,
     * this.getStatement().getConnection().getTypeMap()).
     * 
     * @param columnName - the SQL name of the column
     * @return
     *         a java.lang.Object holding the column value.
     * @throws SQLException if a database access error occurs.
     */
    public Object getObject(String arg0) throws SQLException {
        try {
            Object result = rsetImpl.getObject(arg0);

            addFreedResources(result);

            return result;
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getObject", "1684", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Returns the value in the specified column as a Java object. This method uses the specified Map object for
     * custom mapping if appropriate.
     * 
     * @param colName - the name of the column from which to retrieve the value
     *            map - the mapping from SQL type names to Java classes
     * @return
     *         an object representing the SQL value in the specified column
     */
    public Object getObject(String arg0, Map<String, Class<?>> arg1) throws SQLException 
    {
        try {
            Object result = rsetImpl.getObject(arg0, arg1);

            addFreedResources(result);

            return result;
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getObject", "1713", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets a REF(<structured-type>) column value from the current row.
     * 
     * @param i - the first column is 1, the second is 2, ...
     * @return
     *         a Ref object representing an SQL REF value
     */
    public Ref getRef(int arg0) throws SQLException {
        try {
            return rsetImpl.getRef(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getRef", "1740", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets a REF(<structured-type>) column value from the current row.
     * 
     * @param colName - the column name
     * @return
     *         a Ref object representing the SQL REF value in the specified column
     */
    public Ref getRef(String arg0) throws SQLException {
        try {
            return rsetImpl.getRef(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getRef", "1767", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Retrieves the current row number. The first row is number 1, the second number 2, and so on.
     * 
     * @return
     *         the current row number; 0 if there is no current row
     * @throws SQLException if a database access error occurs.
     */
    public int getRow() throws SQLException {
        try {
            return rsetImpl.getRow();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getRow", "1808", this);
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
    public RowId getRowId(int columnIndex) throws SQLException {
        try {
            return rsetImpl.getRowId(columnIndex);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getRowId", "1973", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.getRowId", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getRowId", "1989", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getRowId", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getRowId", "1996", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getRowId", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public RowId getRowId(String columnLabel) throws SQLException {
        try {
            return rsetImpl.getRowId(columnLabel);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getRowId", "2016", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.getRowId", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getRowId", "2032", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getRowId", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getRowId", "2039", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getRowId", err);
            throw err;
        }
    }

    /**
     * Gets the value of a column in the current row as a Java short.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return
     *         the column value; if the value is SQL NULL, the result is 0
     * @throws SQLException if a database access error occurs
     */
    public short getShort(int arg0) throws SQLException {
        try {
            return rsetImpl.getShort(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getShort", "1837", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java short.
     * 
     * @param columnName - the SQL name of the column
     * @return
     *         the column value; if the value is SQL NULL, the result is 0
     * @throws SQLException if a database access error occurs.
     */
    public short getShort(String arg0) throws SQLException {
        try {
            return rsetImpl.getShort(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getShort", "1866", this);
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
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        try {
            SQLXML xml = rsetImpl.getSQLXML(columnIndex); 
            if (xml != null && freeResourcesOnClose)
                xmls.add(xml); 
            return xml; 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getSQLXML", "2115", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.getSQLXML", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getSQLXML", "2131", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getSQLXML", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getSQLXML", "2138", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getSQLXML", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        try {
            SQLXML xml = rsetImpl.getSQLXML(columnLabel); 
            if (xml != null && freeResourcesOnClose)
                xmls.add(xml); 
            return xml; 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getSQLXML", "2158", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.getSQLXML", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getSQLXML", "2174", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getSQLXML", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getSQLXML", "2181", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getSQLXML", err);
            throw err;
        }
    }

    /**
     * Returns the Statement that produced this ResultSet object. If the result set was generated some other
     * way, such as by a DatabaseMetaData method, this method returns null.
     * 
     * @return
     *         the Statment that produced the result set or null if the result set was produced some other way
     * @throws SQLException if a database access error occurs.
     */
    public Statement getStatement() throws SQLException {
        // The parent of a ResultSet may be a Statement or a MetaData.
        // For ResultSets created by MetaDatas, the getStatement method should return null,
        // unless the result set is closed.

        if (state == State.CLOSED || parentWrapper == null)
            throw createClosedException("ResultSet"); 

        if (parentWrapper instanceof WSJdbcDatabaseMetaData)
            return null;

        return (Statement) parentWrapper;
    }

    /**
     * Gets the value of a column in the current row as a Java String.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs.
     */
    public String getString(int arg0) throws SQLException {
        try {
            return rsetImpl.getString(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getString", "1926", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a Java String.
     * 
     * @param columnName - the SQL name of the column
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs
     */
    public String getString(String arg0) throws SQLException {
        try {
            return rsetImpl.getString(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getString", "1955", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.sql.Time object.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs
     */
    public Time getTime(int arg0) throws SQLException {
        try {
            return rsetImpl.getTime(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getTime", "1984", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.sql.Time object. This method uses the given calendar to
     * construct an appropriate millisecond value for the Time if the underlying database does not store timezone
     * information.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            cal - the calendar to use in constructing the time
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs.
     */
    public Time getTime(int arg0, Calendar arg1) throws SQLException {
        try {
            return rsetImpl.getTime(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getTime", "2016", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.sql.Time object.
     * 
     * @param columnName - the SQL name of the column
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs.
     */
    public Time getTime(String arg0) throws SQLException {
        try {
            return rsetImpl.getTime(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getTime", "2045", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.sql.Time object. This method uses the given calendar to
     * construct an appropriate millisecond value for the Time if the underlying database does not store timezone
     * information.
     * 
     * @param columnName - the SQL name of the column
     *            cal - the calendar to use in constructing the time
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs.
     */
    public Time getTime(String arg0, Calendar arg1) throws SQLException {
        try {
            return rsetImpl.getTime(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getTime", "2077", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.sql.Timestamp object.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs
     */
    public Timestamp getTimestamp(int arg0) throws SQLException {
        try {
            return rsetImpl.getTimestamp(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getTimestamp", "2106", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.sql.Timestamp object. This method uses the given calendar
     * to construct an appropriate millisecond value for the Timestamp if the underlying database does not store timezone
     * information.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            cal - the calendar to use in constructing the timestamp
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs.
     */
    public Timestamp getTimestamp(int arg0, Calendar arg1) throws SQLException {
        try {
            return rsetImpl.getTimestamp(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getTimestamp", "2138", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.sql.Timestamp object.
     * 
     * @param columnName - the SQL name of the column
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs.
     */
    public Timestamp getTimestamp(String arg0) throws SQLException {
        try {
            return rsetImpl.getTimestamp(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getTimestamp", "2167", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gets the value of a column in the current row as a java.sql.Timestamp object. This method uses the given calendar
     * to construct an appropriate millisecond value for the Timestamp if the underlying database does not store timezone
     * information.
     * 
     * @param columnName - the SQL name of the column
     *            cal - the calendar to use in constructing the timestamp
     * @return
     *         the column value; if the value is SQL NULL, the result is null
     * @throws SQLException if a database access error occurs.
     */
    public Timestamp getTimestamp(String arg0, Calendar arg1) throws SQLException {
        try {
            return rsetImpl.getTimestamp(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getTimestamp", "2199", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @return the trace component for the WSJdbcResultSet.
     */
    final protected TraceComponent getTracer() 
    {
        return tc;
    }

    /**
     * Returns the type of this result set. The type is determined by the statement that created the result set.
     * 
     * @return
     *         TYPE_FORWARD_ONLY, TYPE_SCROLL_INSENSITIVE, or TYPE_SCROLL_SENSITIVE
     * @throws SQLException if a database access error occurs.
     */
    public int getType() throws SQLException {
        try {
            return rsetImpl.getType();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getType", "2235", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public InputStream getUnicodeStream(int arg0) throws SQLException {
        try {
            @SuppressWarnings("deprecation")
            InputStream stream = rsetImpl.getUnicodeStream(arg0); 
            if (stream != null && freeResourcesOnClose)
                resources.add(stream); 
            return stream; 
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getUnicodeStream", "2273", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public InputStream getUnicodeStream(String arg0) throws SQLException {
        try {
            @SuppressWarnings("deprecation")
            InputStream stream = rsetImpl.getUnicodeStream(arg0); 
            if (stream != null && freeResourcesOnClose)
                resources.add(stream); 
            return stream; 
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getUnicodeStream", "2312", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * The first warning reported by calls on this ResultSet is returned. Subsequent ResultSet warnings will be chained to
     * this SQLWarning.
     * 
     * The warning chain is automatically cleared each time a new row is read.
     * 
     * Note: This warning chain only covers warnings caused by ResultSet methods. Any warning caused by statement
     * methods (such as reading OUT parameters) will be chained on the Statement object.
     * 
     * @return
     *         the first SQLWarning or null
     * @throws SQLException if a database access error occurs.
     */
    public SQLWarning getWarnings() throws SQLException {
        try {
            return rsetImpl.getWarnings();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getWarnings", "2345", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Inserts the contents of the insert row into the result set and the database. Must be on
     * the insert row when this method is called.
     * 
     * @throws SQLException if a database access error occurs, if called when not on the insert
     *             row, or if not all of non-nullable columns in the insert row have been given a
     *             value.
     */
    public void insertRow() throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "insertRow");

        try {
            parentWrapper.parentWrapper.beginTransactionIfNecessary();

            rsetImpl.insertRow();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.insertRow", "2398", this);
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
        info.append("Underlying ResultSet: " + AdapterUtil.toString(rsetImpl), rsetImpl);
    }

    /**
     * Indicates whether the cursor is after the last row in the result set.
     * 
     * @return
     *         true if the cursor is after the last row, false otherwise. Returns false when the result set contains no rows.
     * @throws SQLException if a database access error occurs.
     */
    public boolean isAfterLast() throws SQLException {
        try {
            return rsetImpl.isAfterLast();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.isAfterLast", "2425", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Indicates whether the cursor is before the first row in the result set.
     * 
     * @return
     *         true if the cursor is before the first row, false otherwise. Returns false when the result set contains no rows.
     * @throws SQLException if a database access error occurs.
     */
    public boolean isBeforeFirst() throws SQLException {
        try {
            return rsetImpl.isBeforeFirst();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.isBeforeFirst", "2452", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Indicates whether the cursor is on the first row of the result set.
     * 
     * @return
     *         true if the cursor is on the first row, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean isFirst() throws SQLException {
        try {
            return rsetImpl.isFirst();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.isFirst", "2479", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Indicates whether the cursor is on the last row of the result set. Note: Calling the method isLast may be expensive
     * because the JDBC driver might need to fetch ahead one row in order to determine whether the current row is the
     * last row in the result set.
     * 
     * @return
     *         true if the cursor is on the last row, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean isLast() throws SQLException {
        try {
            return rsetImpl.isLast();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.isLast", "2508", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Moves the cursor to the last row in the result set.
     * 
     * @return
     *         true if the cursor is on a valid row; false if there are no rows in the result set
     * @throws SQLException if a database access error occurs or the result set type is TYPE_FORWARD_ONLY.
     */
    public boolean last() throws SQLException {
        try {
            if (dsConfig.get().beginTranForResultSetScrollingAPIs)
                getConnectionWrapper().beginTransactionIfNecessary();

            return rsetImpl.last();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.last", "2535", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Moves the cursor to the remembered cursor position, usually the current row. This method has no effect
     * if the cursor is not on the insert row.
     * 
     * @throws SQLException if a database access error occurs or the result set is not updatable.
     */
    public void moveToCurrentRow() throws SQLException {
        try {
            if (dsConfig.get().beginTranForResultSetScrollingAPIs)
                getConnectionWrapper().beginTransactionIfNecessary();

            rsetImpl.moveToCurrentRow();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.moveToCurrentRow", "2561", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Moves the cursor to the insert row. The current cursor position is remembered while the cursor is
     * positioned on the insert row. The insert row is a special row associated with an updatable result set. It is
     * essentially a buffer where a new row may be constructed by calling the updateXXX methods prior to inserting the
     * row into the result set. Only the updateXXX, getXXX, and insertRow methods may be called when the cursor is on
     * the insert row. All of the columns in a result set must be given a value each time this method is called before
     * calling insertRow. The method updateXXX must be called before a getXXX method can be called on a column
     * value.
     * 
     * @throws SQLException if a database access error occurs or the result set is not updatable.
     */
    public void moveToInsertRow() throws SQLException {
        try {
            if (dsConfig.get().beginTranForResultSetScrollingAPIs)
                getConnectionWrapper().beginTransactionIfNecessary();

            rsetImpl.moveToInsertRow();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.moveToInsertRow", "2592", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Moves the cursor down one row from its current position. A ResultSet cursor is initially positioned before the first
     * row; the first call to next makes the first row the current row; the second call makes the second row the current
     * row, and so on.
     * 
     * If an input stream is open for the current row, a call to the method next will implicitly close it. The ResultSet's
     * warning chain is cleared when a new row is read.
     * 
     * @return
     *         true if the new current row is valid; false if there are no more rows
     * @throws SQLException if a database access error occurs
     */
    public boolean next() throws SQLException {
        try {
            if (dsConfig.get().beginTranForResultSetScrollingAPIs)
                getConnectionWrapper().beginTransactionIfNecessary();

            boolean moreRows = rsetImpl.next();            
            return moreRows;
        } catch (SQLException ex) {
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Moves the cursor to the previous row in the result set.
     * 
     * Note: previous() is not the same as relative(-1) because it makes sense to callprevious() when there is no
     * current row.
     * 
     * @return
     *         true if the cursor is on a valid row; false if it is off the result set
     * @throws SQLException if a database access error occurs or the result set type is TYPE_FORWARD_ONLY
     */
    public boolean previous() throws SQLException {
        try {
            if (dsConfig.get().beginTranForResultSetScrollingAPIs)
                getConnectionWrapper().beginTransactionIfNecessary();

            return rsetImpl.previous();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.previous", "2654", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Refreshes the current row with its most recent value in the database. Cannot be called
     * when on the insert row. The refreshRow method provides a way for an application to
     * explicitly tell the JDBC driver to refetch a row(s) from the database. An application
     * may want to call refreshRow when caching or prefetching is being done by the JDBC driver
     * to fetch the latest value of a row from the database. The JDBC driver may actually
     * refresh multiple rows at once if the fetch size is greater than one. All values are
     * refetched subject to the transaction isolation level and cursor sensitivity. If
     * refreshRow is called after calling updateXXX, but before calling updateRow, then the
     * updates made to the row are lost. Calling the method refreshRow frequently will likely
     * slow performance.
     * 
     * @throws SQLException if a database access error occurs or if called when on the insert
     *             row.
     */
    public void refreshRow() throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "refreshRow");

        try {
            parentWrapper.parentWrapper.beginTransactionIfNecessary(); 

            rsetImpl.refreshRow();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.refreshRow", "2714", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Moves the cursor a relative number of rows, either positive or negative. Attempting to move beyond the first/last
     * row in the result set positions the cursor before/after the the first/last row. Calling relative(0) is valid, but does
     * not change the cursor position.
     * 
     * Note: Calling relative(1) is different from calling next() because is makes sense to call next() when there is
     * no current row, for example, when the cursor is positioned before the first row or after the last row of the result
     * set.
     * 
     * @return
     *         true if the cursor is on a row; false otherwise
     * @throws SQLException if a database access error occurs, there is no current row, or the result set type is
     *             TYPE_FORWARD_ONLY
     */
    public boolean relative(int arg0) throws SQLException {
        try {
            if (dsConfig.get().beginTranForResultSetScrollingAPIs)
                getConnectionWrapper().beginTransactionIfNecessary();

            return rsetImpl.relative(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.relative", "2748", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Indicates whether a row has been deleted. A deleted row may leave a visible "hole" in a result set. This
     * method can be used to detect holes in a result set. The value returned depends on whether or not the result set can
     * detect deletions.
     * 
     * @return
     *         true if a row was deleted and deletions are detected
     * @throws SQLException if a database access error occurs.
     */
    public boolean rowDeleted() throws SQLException {
        try {
            return rsetImpl.rowDeleted();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.rowDeleted", "2777", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Indicates whether the current row has had an insertion. The value returned depends on whether or not the
     * result set can detect visible inserts.
     * 
     * @return
     *         true if a row has had an insertion and insertions are detected
     * @throws SQLException if a database access error occurs.
     */
    public boolean rowInserted() throws SQLException {
        try {
            return rsetImpl.rowInserted();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.rowInserted", "2805", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Indicates whether the current row has been updated. The value returned depends on whether or not the
     * result set can detect updates.
     * 
     * @return
     *         true if the row has been visibly updated by the owner or another, and updates are detected
     * @throws SQLException if a database access error occurs.
     */
    public boolean rowUpdated() throws SQLException {
        try {
            return rsetImpl.rowUpdated();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.rowUpdated", "2833", this);
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
            throw createClosedException("ResultSet"); 

        return runtimeX;
    }

    /**
     * Gives a hint as to the direction in which the rows in this result set will be processed. The initial value is
     * determined by the statement that produced the result set. The fetch direction may be changed at any time.
     * 
     * @param direction fetch direction
     * @throws SQLException if a database access error occurs or the result set type is TYPE_FORWARD_ONLY and the
     *             fetch direction is not FETCH_FORWARD.
     */
    public void setFetchDirection(int direction) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setFetchDirection", AdapterUtil.getFetchDirectionString(direction));

        try {
            rsetImpl.setFetchDirection(direction);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.setFetchDirection", "2860", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Gives the JDBC driver a hint as to the number of rows that should be fetched from the database when
     * more rows are needed for this result set. If the fetch size specified is zero, the JDBC driver ignores the value and is
     * free to make its own best guess as to what the fetch size should be. The default value is set by the statement that
     * created the result set. The fetch size may be changed at any time.
     * 
     * @param rows - the number of rows to fetch
     * @throws SQLException if a database access error occurs or the condition 0 <= rows <= this.getMaxRows() is not
     *             satisfied.
     */
    public void setFetchSize(int rows) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setFetchSize", rows);

        try {
            rsetImpl.setFetchSize(rows);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.setFetchSize", "2891", this);
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
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        try {
            rsetImpl.updateAsciiStream(columnIndex, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateAsciiStream", "2922", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateAsciiStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateAsciiStream", "3280", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateAsciiStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateAsciiStream", "3287", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateAsciiStream", err);
            throw err;
        }
    }

    /**
     * Updates a column with an ascii stream value. The updateXXX methods are used to update column values
     * in the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     *            length - the length of the stream
     * @throws SQLException if a database access error occurs.
     */
    public void updateAsciiStream(int arg0, InputStream arg1, int arg2) throws SQLException {
        try {
            rsetImpl.updateAsciiStream(arg0, arg1, arg2);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateAsciiStream", "2922", this);
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
    public void updateAsciiStream(int i, InputStream x, long length) throws SQLException {
        try {
            rsetImpl.updateAsciiStream(i, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateAsciiStream", "2981", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateAsciiStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateAsciiStream", "3355", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateAsciiStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateAsciiStream", "3362", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateAsciiStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        try {
            rsetImpl.updateAsciiStream(columnLabel, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateAsciiStream", "3010", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateAsciiStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateAsciiStream", "3400", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateAsciiStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateAsciiStream", "3407", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateAsciiStream", err);
            throw err;
        }
    }

    /**
     * Updates a column with an ascii stream value. The updateXXX methods are used to update column values
     * in the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     *            length - of the stream
     * @throws SQLException if a database access error occurs.
     */
    public void updateAsciiStream(String arg0, InputStream arg1, int arg2) throws SQLException {
        try {
            rsetImpl.updateAsciiStream(arg0, arg1, arg2);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateAsciiStream", "2953", this);
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
    public void updateAsciiStream(String columnLabel, InputStream x, long length)
                    throws SQLException {
        try {
            rsetImpl.updateAsciiStream(columnLabel, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateAsciiStream", "3070", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateAsciiStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateAsciiStream", "3476", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateAsciiStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateAsciiStream", "3483", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateAsciiStream", err);
            throw err;
        }
    }

    /**
     * Updates a column with a BigDecimal value. The updateXXX methods are used to update column values
     * in the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateBigDecimal(int arg0, BigDecimal arg1) throws SQLException {
        try {
            rsetImpl.updateBigDecimal(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateBigDecimal", "2983", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a BigDecimal value. The updateXXX methods are used to update column values
     * in the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateBigDecimal(String arg0, BigDecimal arg1) throws SQLException {
        try {
            rsetImpl.updateBigDecimal(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateBigDecimal", "3013", this);
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
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        try {
            rsetImpl.updateBinaryStream(columnIndex, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateBinaryStream", "3157", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateBinaryStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateBinaryStream", "3579", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBinaryStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateBinaryStream", "3586", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBinaryStream", err);
            throw err;
        }
    }

    /**
     * Updates a column with a binary stream value. The updateXXX methods are used to update column values
     * in the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     *            length - the length of the stream
     * @throws SQLException if a database access error occurs.
     */
    public void updateBinaryStream(int arg0, InputStream arg1, int arg2) throws SQLException {
        try {
            rsetImpl.updateBinaryStream(arg0, arg1, arg2);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateBinaryStream", "3044", this);
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
    public void updateBinaryStream(int i, InputStream x, long length) throws SQLException {
        try {
            rsetImpl.updateBinaryStream(i, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateBinaryStream", "3216", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateBinaryStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateBinaryStream", "3654", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBinaryStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateBinaryStream", "3661", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBinaryStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        try {
            rsetImpl.updateBinaryStream(columnLabel, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateBinaryStream", "3245", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateBinaryStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateBinaryStream", "3699", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBinaryStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateBinaryStream", "3706", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBinaryStream", err);
            throw err;
        }
    }

    /**
     * Updates a column with a binary stream value. The updateXXX methods are used to update column values
     * in the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     *            length - of the stream
     * @throws SQLException if a database access error occurs.
     */
    public void updateBinaryStream(String arg0, InputStream arg1, int arg2) throws SQLException {
        try {
            rsetImpl.updateBinaryStream(arg0, arg1, arg2);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateBinaryStream", "3075", this);
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
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        try {
            rsetImpl.updateBinaryStream(columnLabel, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateBinaryStream", "3304", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateBinaryStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateBinaryStream", "3774", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBinaryStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateBinaryStream", "3781", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBinaryStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateBlob(int i, InputStream x) throws SQLException {
        try {
            rsetImpl.updateBlob(i, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateBlob", "3333", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateBlob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateBlob", "3819", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBlob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateBlob", "3826", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBlob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateBlob(int i, InputStream x, long length) throws SQLException {
        try {
            rsetImpl.updateBlob(i, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateBlob", "3362", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateBlob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateBlob", "3864", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBlob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateBlob", "3871", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBlob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateBlob(String columnLabel, InputStream x) throws SQLException {
        try {
            rsetImpl.updateBlob(columnLabel, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateBlob", "3391", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateBlob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateBlob", "3909", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBlob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateBlob", "3916", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBlob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateBlob(String columnLabel, InputStream x, long length) throws SQLException {
        try {
            rsetImpl.updateBlob(columnLabel, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateBlob", "3420", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateBlob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateBlob", "3954", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBlob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateBlob", "3961", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateBlob", err);
            throw err;
        }
    }

    /**
     * Updates a column with a boolean value. The updateXXX methods are used to update column values in
     * the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateBoolean(int arg0, boolean arg1) throws SQLException {
        try {
            rsetImpl.updateBoolean(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateBoolean", "3105", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a boolean value. The updateXXX methods are used to update column values in
     * the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateBoolean(String arg0, boolean arg1) throws SQLException {
        try {
            rsetImpl.updateBoolean(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateBoolean", "3135", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a byte value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateByte(int arg0, byte arg1) throws SQLException {
        try {
            rsetImpl.updateByte(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateByte", "3165", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a byte value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateByte(String arg0, byte arg1) throws SQLException {
        try {
            rsetImpl.updateByte(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateByte", "3195", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a byte array value. The updateXXX methods are used to update column values in
     * the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateBytes(int arg0, byte[] arg1) throws SQLException {
        try {
            rsetImpl.updateBytes(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateBytes", "3225", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a byte array value. The updateXXX methods are used to update column values in
     * the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateBytes(String arg0, byte[] arg1) throws SQLException {
        try {
            rsetImpl.updateBytes(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateBytes", "3255", this);
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
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        try {
            rsetImpl.updateCharacterStream(columnIndex, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateCharacterStream", "3623", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateCharacterStream", "4173", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateCharacterStream", "4180", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateCharacterStream", err);
            throw err;
        }
    }

    /**
     * Updates a column with a character stream value. The updateXXX methods are used to update column
     * values in the current row, or the insert row. The updateXXX methods do not update the underlying database; instead
     * the updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     *            length - the length of the stream
     * @throws SQLException if a database access error occurs.
     */
    public void updateCharacterStream(int arg0, Reader arg1, int arg2) throws SQLException {
        try {
            rsetImpl.updateCharacterStream(arg0, arg1, arg2);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateCharacterStream", "3286", this);
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
    public void updateCharacterStream(int i, Reader x, long length) throws SQLException {
        try {
            rsetImpl.updateCharacterStream(i, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateCharacterStream", "3682", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateCharacterStream", "4248", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateCharacterStream", "4255", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateCharacterStream(String columnLabel, Reader x) throws SQLException {
        try {
            rsetImpl.updateCharacterStream(columnLabel, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateCharacterStream", "3711", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateCharacterStream", "4293", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateCharacterStream", "4300", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateCharacterStream", err);
            throw err;
        }
    }

    /**
     * Updates a column with a character stream value. The updateXXX methods are used to update column
     * values in the current row, or the insert row. The updateXXX methods do not update the underlying database; instead
     * the updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     *            length - of the stream
     * @throws SQLException if a database access error occurs.
     */
    public void updateCharacterStream(String arg0, Reader arg1, int arg2) throws SQLException {
        try {
            rsetImpl.updateCharacterStream(arg0, arg1, arg2);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateCharacterStream", "3317", this);
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
    public void updateCharacterStream(String columnLabel, Reader x, long length) throws SQLException {
        try {
            rsetImpl.updateCharacterStream(columnLabel, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateCharacterStream", "3770", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateCharacterStream", "4368", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateCharacterStream", "4375", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateClob(int i, Reader x) throws SQLException {
        try {
            rsetImpl.updateClob(i, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateClob", "3799", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateClob", "4413", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateClob", "4420", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateClob(int i, Reader x, long length) throws SQLException {
        try {
            rsetImpl.updateClob(i, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateClob", "3828", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateClob", "4458", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateClob", "4465", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateClob(String columnLabel, Reader x) throws SQLException {
        try {
            rsetImpl.updateClob(columnLabel, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateClob", "3857", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateClob", "4503", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateClob", "4510", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateClob(String columnLabel, Reader x, long length) throws SQLException {
        try {
            rsetImpl.updateClob(columnLabel, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateClob", "3886", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateClob", "4548", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateClob", "4555", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateClob", err);
            throw err;
        }
    }

    /**
     * Updates a column with a Date value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateDate(int arg0, Date arg1) throws SQLException {
        try {
            rsetImpl.updateDate(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateDate", "3347", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a Date value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateDate(String arg0, Date arg1) throws SQLException {
        try {
            rsetImpl.updateDate(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateDate", "3377", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a Double value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateDouble(int arg0, double arg1) throws SQLException {
        try {
            rsetImpl.updateDouble(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateDouble", "3407", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a double value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateDouble(String arg0, double arg1) throws SQLException {
        try {
            rsetImpl.updateDouble(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateDouble", "3437", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a float value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateFloat(int arg0, float arg1) throws SQLException {
        try {
            rsetImpl.updateFloat(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateFloat", "3467", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a float value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateFloat(String arg0, float arg1) throws SQLException {
        try {
            rsetImpl.updateFloat(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateFloat", "3497", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with an integer value. The updateXXX methods are used to update column values in
     * the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateInt(int arg0, int arg1) throws SQLException {
        try {
            rsetImpl.updateInt(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateInt", "3527", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with an integer value. The updateXXX methods are used to update column values in
     * the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateInt(String arg0, int arg1) throws SQLException {
        try {
            rsetImpl.updateInt(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateInt", "3557", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a long value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateLong(int arg0, long arg1) throws SQLException {
        try {
            rsetImpl.updateLong(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateLong", "3587", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a long value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateLong(String arg0, long arg1) throws SQLException {
        try {
            rsetImpl.updateLong(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateLong", "3617", this);
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
    public void updateNCharacterStream(int i, Reader reader) throws SQLException {
        try {
            rsetImpl.updateNCharacterStream(i, reader); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateNCharacterStream", "4205", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateNCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateNCharacterStream", "4883", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateNCharacterStream", "4890", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateNCharacterStream(int i, Reader reader, long length) throws SQLException {
        try {
            rsetImpl.updateNCharacterStream(i, reader, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateNCharacterStream", "4234", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateNCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateNCharacterStream", "4928", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateNCharacterStream", "4935", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateNCharacterStream(String columnLabel, Reader reader)
                    throws SQLException {
        try {
            rsetImpl.updateNCharacterStream(columnLabel, reader); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateNCharacterStream", "4264", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateNCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateNCharacterStream", "4974", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateNCharacterStream", "4981", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateNCharacterStream(String columnLabel, Reader reader, long length)
                    throws SQLException {
        try {
            rsetImpl.updateNCharacterStream(columnLabel, reader, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateNCharacterStream", "4294", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateNCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateNCharacterStream", "5020", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateNCharacterStream", "5027", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        try {
            rsetImpl.updateNClob(columnIndex, nClob);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateNClob", "4581", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateNClob", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateNClob", "4597", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateNClob", "4604", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateNClob(int i, Reader x) throws SQLException {
        try {
            rsetImpl.updateNClob(i, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateNClob", "4323", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateNClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateNClob", "5108", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateNClob", "5115", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateNClob(int i, Reader x, long length) throws SQLException {
        try {
            rsetImpl.updateNClob(i, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateNClob", "4352", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateNClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateNClob", "5153", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateNClob", "5160", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateNClob(java.lang.String columnLabel, NClob nClob) throws SQLException {
        try {
            rsetImpl.updateNClob(columnLabel, nClob);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateNClob", "4682", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateNClob", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateNClob", "4698", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateNClob", "4705", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateNClob(String columnLabel, Reader x) throws SQLException {
        try {
            rsetImpl.updateNClob(columnLabel, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateNClob", "4381", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateNClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateNClob", "5241", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateNClob", "5248", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateNClob(String columnLabel, Reader x, long length) throws SQLException {
        try {
            rsetImpl.updateNClob(columnLabel, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateNClob", "4410", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateNClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateNClob", "5286", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateNClob", "5293", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateNString(int i, String x) throws SQLException {
        try {
            rsetImpl.updateNString(i, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateNString", "4439", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateNString", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateNString", "5331", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNString", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateNString", "5338", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNString", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateNString(String columnLabel, String x) throws SQLException {
        try {
            rsetImpl.updateNString(columnLabel, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateNString", "4468", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateNString", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateNString", "5376", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNString", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateNString", "5383", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateNString", err);
            throw err;
        }
    }

    /**
     * Give a nullable column a null value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @throws SQLException if a database access error occurs.
     */
    public void updateNull(int arg0) throws SQLException {
        try {
            rsetImpl.updateNull(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateNull", "3646", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a null value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     * @throws SQLException if a database access error occurs.
     */
    public void updateNull(String arg0) throws SQLException {
        try {
            rsetImpl.updateNull(arg0);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateNull", "3675", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with an Object value. The updateXXX methods are used to update column values in
     * the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     * @throws SQLException if a database access error occurs
     */
    public void updateObject(int arg0, Object arg1) throws SQLException {
        try {
            rsetImpl.updateObject(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateObject", "3705", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with an Object value. The updateXXX methods are used to update column values in
     * the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     *            scale - For java.sql.Types.DECIMAL or java.sql.Types.NUMERIC types this is the number of digits after
     *            the decimal. For all other types this value will be ignored.
     * @throws SQLException if a database access error occurs.
     */
    public void updateObject(int arg0, Object arg1, int arg2) throws SQLException {
        try {
            rsetImpl.updateObject(arg0, arg1, arg2);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateObject", "3737", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with an Object value. The updateXXX methods are used to update column values in
     * the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateObject(String arg0, Object arg1) throws SQLException {
        try {
            rsetImpl.updateObject(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateObject", "3767", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with an Object value. The updateXXX methods are used to update column values in
     * the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     *            scale - For java.sql.Types.DECIMAL or java.sql.Types.NUMERIC types this is the number of digits after
     *            the decimal. For all other types this value will be ignored.
     * @throws SQLException if a database access error occurs.
     */
    public void updateObject(String arg0, Object arg1, int arg2) throws SQLException {
        try {
            rsetImpl.updateObject(arg0, arg1, arg2);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateObject", "3799", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates the underlying database with the new contents of the current row. Cannot be
     * called when on the insert row.
     * 
     * @throws SQLException if a database access error occurs or if called when on the insert
     *             row.
     */
    public void updateRow() throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "updateRow");

        try {
            parentWrapper.parentWrapper.beginTransactionIfNecessary(); 

            rsetImpl.updateRow();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateRow", "3851", this);
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
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        try {
            rsetImpl.updateRowId(columnIndex, x);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateRowId", "5064", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateRowId", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateRowId", "5080", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateRowId", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateRowId", "5087", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateRowId", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        try {
            rsetImpl.updateRowId(columnLabel, x);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateRowId", "5107", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateRowId", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateRowId", "5123", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateRowId", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateRowId", "5130", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateRowId", err);
            throw err;
        }
    }

    /**
     * Updates a column with a short value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateShort(int arg0, short arg1) throws SQLException {
        try {
            rsetImpl.updateShort(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateShort", "3881", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a short value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateShort(String arg0, short arg1) throws SQLException {
        try {
            rsetImpl.updateShort(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateShort", "3911", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a String value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateString(int arg0, String arg1) throws SQLException {
        try {
            rsetImpl.updateString(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateString", "3941", this);
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
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        try {
            rsetImpl.updateSQLXML(columnIndex, xmlObject);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateSQLXML", "5237", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateSQLXML", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateSQLXML", "5253", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateSQLXML", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateSQLXML", "5260", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateSQLXML", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        try {
            rsetImpl.updateSQLXML(columnLabel, xmlObject);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".updateSQLXML", "5280", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("ResultSet.updateSQLXML", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".updateSQLXML", "5296", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateSQLXML", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".updateSQLXML", "5303", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "updateSQLXML", err);
            throw err;
        }
    }

    /**
     * Updates a column with a String value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateString(String arg0, String arg1) throws SQLException {
        try {
            rsetImpl.updateString(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateString", "3971", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a Time value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateTime(int arg0, Time arg1) throws SQLException {
        try {
            rsetImpl.updateTime(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateTime", "4001", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a Time value. The updateXXX methods are used to update column values in the
     * current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateTime(String arg0, Time arg1) throws SQLException {
        try {
            rsetImpl.updateTime(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateTime", "4031", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a Timestamp value. The updateXXX methods are used to update column values in
     * the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnIndex - the first column is 1, the second is 2, ...
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateTimestamp(int arg0, Timestamp arg1) throws SQLException {
        try {
            rsetImpl.updateTimestamp(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateTimestamp", "4061", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Updates a column with a Timestamp value. The updateXXX methods are used to update column values in
     * the current row, or the insert row. The updateXXX methods do not update the underlying database; instead the
     * updateRow or insertRow methods are called to update the database.
     * 
     * @param columnName - the name of the column
     *            x - the new column value
     * @throws SQLException if a database access error occurs.
     */
    public void updateTimestamp(String arg0, Timestamp arg1) throws SQLException {
        try {
            rsetImpl.updateTimestamp(arg0, arg1);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateTimestamp", "4091", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Reports whether the last column read had a value of SQL NULL. Note that you must first call getXXX on a column
     * to try to read its value and then call wasNull() to see if the value read was SQL NULL.
     * 
     * @return
     *         true if last column read was SQL NULL and false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean wasNull() throws SQLException {
        try {
            return rsetImpl.wasNull();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.wasNull", "4119", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    //  starts
    /**
     * <p>Retrieves the value of the designated column in the current row of this ResultSet
     * object as a java.net.URL object in the Java programming language. </p>
     * 
     * @param columnIndex the index of the column 1 is the first, 2 is the second,...
     * 
     * @return the column value as a java.net.URL object; if the value is SQL NULL, the value
     *         returned is null in the Java programming language
     * 
     * @exception SQLException If a database access error occurs or if a URL is malformed
     */
    public java.net.URL getURL(int columnIndex) throws SQLException { 
        try {
            return rsetImpl.getURL(columnIndex);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getURL", "3935", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Retrieves the value of the designated column in the current row of this ResultSet
     * object as a java.net.URL object in the Java programming language. </p>
     * 
     * @param columnName the SQL name of the column
     * 
     * @return the column value as a java.net.URL object; if the value is SQL NULL, the value
     *         returned is null in the Java programming language
     * 
     * @exception SQLException If a database access error occurs or if a URL is malformed
     */
    public java.net.URL getURL(String columnName) throws SQLException { 
        try {
            return rsetImpl.getURL(columnName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.getURL", "3964", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Updates the designated column with a java.sql.Array value. The updater methods
     * are used to update column values in the current row or the insert row. The updater
     * methods do not update the underlying database; instead the updateRow or insertRow
     * methods are called to update the database. </p>
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param array the new column value
     * 
     * @exception SQLException If a database access error occurs
     */
    public void updateArray(int i, java.sql.Array array) throws SQLException { 

        try {
            rsetImpl.updateArray(i, array);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateArray", "3993", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Updates the designated column with a java.sql.Array value. The updater methods
     * are used to update column values in the current row or the insert row. The updater
     * methods do not update the underlying database; instead the updateRow or insertRow
     * methods are called to update the database. </p>
     * 
     * @param columnName the name of the column
     * @param array the new column value
     * 
     * @exception SQLException If a database access error occurs
     */
    public void updateArray(String columnName, java.sql.Array array) throws SQLException { 

        try {
            rsetImpl.updateArray(columnName, array);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateArray", "4009", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Updates the designated column with a java.sql.Blob value. The updater methods
     * are used to update column values in the current row or the insert row. The updater
     * methods do not update the underlying database; instead the updateRow or insertRow
     * methods are called to update the database.</p>
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param b the new column value
     * 
     * @exception SQLException If a database access error occurs
     */
    public void updateBlob(int columnIndex, java.sql.Blob b) throws SQLException { 

        try {
            rsetImpl.updateBlob(columnIndex, b);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateBlob", "4038", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Updates the designated column with a java.sql.Blob value. The updater methods
     * are used to update column values in the current row or the insert row. The updater
     * methods do not update the underlying database; instead the updateRow or insertRow
     * methods are called to update the database.</p>
     * 
     * @param columnName the name of the column
     * @param b the new column value
     * 
     * @exception SQLException If a database access error occurs
     */
    public void updateBlob(String columnName, java.sql.Blob b) throws SQLException { 

        try {
            rsetImpl.updateBlob(columnName, b);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateBlob", "4066", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Updates the designated column with a java.sql.Clob value. The updater methods
     * are used to update column values in the current row or the insert row. The updater
     * methods do not update the underlying database; instead the updateRow or insertRow
     * methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param c the new column value
     * 
     * @exception SQLException If a database access error occurs
     */
    public void updateClob(int i, java.sql.Clob c) throws SQLException { 
        try {
            rsetImpl.updateClob(i, c);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateClob", "4094", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Updates the designated column with a java.sql.Clob value. The updater methods
     * are used to update column values in the current row or the insert row. The updater
     * methods do not update the underlying database; instead the updateRow or insertRow
     * methods are called to update the database.
     * 
     * @param columnName the name of the column
     * @param c the new column value
     * 
     * @exception SQLException If a database access error occurs
     */
    public void updateClob(String columnName, java.sql.Clob c) throws SQLException { 
        try {
            rsetImpl.updateClob(columnName, c);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateClob", "4123", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Updates the designated column with a java.sql.Ref value. The updater methods
     * are used to update column values in the current row or the insert row. The updater
     * methods do not update the underlying database; instead the updateRow or insertRow
     * methods are called to update the database. </p>
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param r the new column value
     * 
     * @excpetion SQLException If a database access error occurs
     */
    public void updateRef(int columnIndex, java.sql.Ref r) throws SQLException { 
        try {
            rsetImpl.updateRef(columnIndex, r);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateRef", "4152", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Updates the designated column with a java.sql.Ref value. The updater methods
     * are used to update column values in the current row or the insert row. The updater
     * methods do not update the underlying database; instead the updateRow or insertRow
     * methods are called to update the database. </p>
     * 
     * @param columnName the name of the column
     * @param r the new column value
     * 
     * @excpetion SQLException If a database access error occurs
     */
    public void updateRef(String columnName, java.sql.Ref r) throws SQLException { 
        try {
            rsetImpl.updateRef(columnName, r);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet.updateRef", "4180", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    protected void addFreedResources(Object result){
        if (freeResourcesOnClose)
            if (result instanceof Closeable)
                resources.add((Closeable) result);
            else if (result instanceof Array)
                arrays.add((Array) result);
            else if (result instanceof Blob)
                blobs.add((Blob) result);
            else if (result instanceof Clob)
                clobs.add((Clob) result);
            else if (result instanceof SQLXML)
                xmls.add((SQLXML) result);
    }

    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }
}
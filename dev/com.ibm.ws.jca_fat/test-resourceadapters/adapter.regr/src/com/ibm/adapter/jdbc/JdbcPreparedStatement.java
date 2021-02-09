/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;

import com.ibm.adapter.AdapterUtil;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * This class is a wrapper class of PreparedStatement.<p>
 */
public class JdbcPreparedStatement extends JdbcStatement implements PreparedStatement {
    private static final TraceComponent tc = Tr.register(JdbcPreparedStatement.class);

    /** The underlying PreparedStatement object. */
    PreparedStatement pstmtImpl;

    /** Number of statement parameters. Updated as parameters are set. */
    // d149533.1
    int numParameters;

    /** List of parameters (first 32) which are set on this statement. */
    // d149533.1
    int parameterList1;

    /** List of remaining parameters which are set on this statement. */
    // d149533.1
    ArrayList parameterList2;

    /**
     * Do not use. Constructor exists only for CallableStatement wrapper.
     */
    JdbcPreparedStatement() {}

    /**
     * Create a WebSphere PreparedStatement wrapper. This constructor should only be used if
     * statement caching is disabled.
     *
     * @param pstmtImplObject the JDBC PreparedStatement implementation class to be wrapped.
     * @param connWrapper the WebSphere JDBC Connection wrapper creating this statement.
     */
    JdbcPreparedStatement(
                          PreparedStatement pstmtImplObject,
                          JdbcConnection connWrapper) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "<init>",
                     new Object[] {
                                    AdapterUtil.toString(pstmtImplObject),
                                    connWrapper });

        stmtImpl = pstmtImpl = pstmtImplObject;
        parentWrapper = connWrapper;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);
    }

    @Override
    public void addBatch() throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "addBatch");

        try {
            pstmtImpl.addBatch();
        } catch (SQLException ex) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "getMoreResults",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void clearParameters() throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "clearParameters");

        try {
            pstmtImpl.clearParameters();
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "clearParameters", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Delegate to the parent closeWrapper method. This method is used by
     * CallableStatement.closeWrapper to avoid caching statements.
     *
     * @throws SQLException if an error occurs while closing.
     */
    final SQLException closeStatementWrapper() {
        pstmtImpl = null;
        return super.closeWrapper();
    }

    /**
     * Perform any wrapper-specific close logic. This method is called by the default
     * WSJdbcObject close method.
     *
     * @return SQLException the first error to occur while closing the object.
     */
    @Override
    SQLException closeWrapper() {
        SQLException sqlX = null;

        // Indicate the statement is closed by setting the parent object's statement to
        // null.  This will allow us to be garbage collected.

        try // Connection wrapper can close at any time.
        {
            parentWrapper.childWrappers.remove(this);
        } catch (RuntimeException runtimeX) {
            if (parentWrapper.state != CLOSED)
                throw runtimeX;
        }

        try {
            // Since we cannot return the statement to the cache, close it instead.
            pstmtImpl.close();
        } catch (SQLException closeX) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "ERR_CLOSING_OBJECT",
                         new Object[] { pstmtImpl, closeX });

            if (sqlX == null)
                sqlX = closeX;
        }

        stmtImpl = null;
        pstmtImpl = null;

        parameterList2 = null;

        return sqlX;
    }

    /**
     * Track the known number of statement parameters and whether or not each parameter is
     * filled in. This is done to substitute for the clearParameters functionality needed when
     * statement caching is enabled to avoid reusing previous parameter values. [d149533.1]
     */
    final void countParameter(int paramIndex) {
        // To improve performance, parameters 1 to 32 are kept in the bits of an int.

        if (paramIndex <= 32) {
            if (paramIndex > numParameters)
                numParameters = paramIndex;
            parameterList1 |= 1 << (paramIndex - 1);
        }

        // Everything over 32 is tracked in an array.

        else {
            // The array is only created if we need it.

            if (parameterList2 == null)
                parameterList2 = new ArrayList(paramIndex - 32);

            // The array is also expanded as needed.

            if (paramIndex > numParameters) {
                parameterList2.ensureCapacity(
                                              (numParameters = paramIndex) - 32);
                for (int i = paramIndex - 32 - parameterList2.size(); i > 0; i--)
                    parameterList2.add(null);
            }

            // A value of 'null' indicates a parameter IS NOT set.  Any other value, like
            // 'Boolean.TRUE' indicates a paramter IS set.

            parameterList2.set(paramIndex - 33, Boolean.TRUE);
        }
    }

    @Override
    public boolean execute() throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "execute", this);

        boolean result;

        try {
            // Synchronize to make sure the transaction cannot be ended until after the
            // statement completes.

            synchronized (parentWrapper.syncObject) {
                parentWrapper.beginTransactionIfNecessary();

                if (rsImpl != null)
                    closeResultSets();

                result = pstmtImpl.execute();
            }
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "execute", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "execute", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "execute", result ? "QUERY" : "UPDATE");
        return result;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "executeQuery", this);

        try {
            // Synchronize to make sure the transaction cannot be ended until after the
            // statement completes.

            synchronized (parentWrapper.syncObject) {
                parentWrapper.beginTransactionIfNecessary();

                if (rsImpl != null)
                    closeResultSets();

                rsImpl = pstmtImpl.executeQuery();

            }
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeQuery", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeQuery", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "executeQuery", rsImpl);
        return rsImpl;
    }

    @Override
    public int executeUpdate() throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "executeUpdate", this);

        int numUpdates = 0;

        try {
            // Synchronize to make sure the transaction cannot be ended until after the
            // statement completes.

            synchronized (parentWrapper.syncObject) {
                parentWrapper.beginTransactionIfNecessary();

                if (rsImpl != null)
                    closeResultSets();

                numUpdates = pstmtImpl.executeUpdate();
            }
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeUpdate", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeUpdate", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "executeUpdate", new Integer(numUpdates));
        return numUpdates;
    }

    /**
     * @return the index of the first parameter in the list which is not set.
     */
    private final int findParamIndex(int paramList) {
        int indexNotSet = 1;
        for (int x = 1; (paramList & x) != 0; x <<= 1)
            indexNotSet++;
        return indexNotSet;
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getMetaData", this);

        // For some drivers, the ResultSetMetaData may be obtained WITHOUT having an open
        // ResultSet.  So we cannot store the ResultSetMetaData for the PreparedStatement on
        // the ResultSet wrapper as done previously.  The ResultSetMetaData will now be kept
        // in the PreparedStatement wrapper's "childWrapper" field.  ResultSets will be kept
        // in the "childWrappers" list.

        // First, check if a ResultSetMetaData wrapper for this ResultSet already exists.
        try // get a new meta data
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getMetaData");
            return pstmtImpl.getMetaData();
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getMetaData", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getMetaData", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

    }

    /**
     * @return the trace component for the WSJdbcPreparedStatement.
     */
    @Override
    TraceComponent getTracer() {
        return tc;
    }

    /**
     * Collects FFDC information specific to this JDBC wrapper. Formats this information to
     * the provided FFDC logger. This method is used by introspectAll to collect any wrapper
     * specific information.
     *
     * @param info FFDCLogger on which to record the FFDC information.
     */
    @Override
    void introspectWrapperSpecificInfo(com.ibm.ws.rsadapter.FFDCLogger info) {
        super.introspectWrapperSpecificInfo(info);

        info.append("Number of known statement parameters: " + numParameters);

        StringBuffer sb = new StringBuffer(Integer.toBinaryString(parameterList1)).reverse();

        if (parameterList2 != null)
            try {
                for (int i = 0; i < parameterList2.size(); i++)
                    sb.append(parameterList2.get(i) == null ? '0' : '1');
            } catch (RuntimeException runtimeX) {

            }

        info.append("Statement parameter indicators [1, 2, ...]", sb);
    }

    @Override
    public void setArray(int i, Array x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setArray #" + i);

        try {
            pstmtImpl.setArray(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setAsciiStream(int i, java.io.InputStream x, int length) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setAsciiStream #" + i, new Integer(length));

        try {
            pstmtImpl.setAsciiStream(i, x, length);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setBigDecimal(int i, java.math.BigDecimal x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setBigDecimal #" + i);

        try {
            pstmtImpl.setBigDecimal(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setBinaryStream(int i, java.io.InputStream x, int length) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setBinaryStream #" + i, new Integer(length));

        try {
            pstmtImpl.setBinaryStream(i, x, length);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setBlob(int i, Blob x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setBlob #" + i);

        try {
            pstmtImpl.setBlob(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setBoolean(int i, boolean x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setBoolean #" + i);

        try {
            pstmtImpl.setBoolean(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setByte(int i, byte x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setByte #" + i);

        try {
            pstmtImpl.setByte(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setBytes(int i, byte[] x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setBytes #" + i);

        try {
            pstmtImpl.setBytes(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setCharacterStream(int i, java.io.Reader x, int length) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setCharacterStream #" + i, new Integer(length));

        try {
            pstmtImpl.setCharacterStream(i, x, length);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setClob(int i, Clob x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setClob #" + i);

        try {
            pstmtImpl.setClob(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setDate(int i, Date x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setDate #" + i);

        try {
            pstmtImpl.setDate(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setDate(int i, Date x, Calendar cal) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setDate with Calendar #" + i);

        try {
            pstmtImpl.setDate(i, x, cal);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setDouble(int i, double x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setDouble #" + i);

        try {
            pstmtImpl.setDouble(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setFloat(int i, float x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setFloat #" + i);

        try {
            pstmtImpl.setFloat(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setInt(int i, int x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setInt #" + i);

        try {
            pstmtImpl.setInt(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setLong(int i, long x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setLong #" + i);

        try {
            pstmtImpl.setLong(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setNull(int i, int sqlType) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "setNull #" + i,
                     AdapterUtil.getSQLTypeString(sqlType));

        try {
            pstmtImpl.setNull(i, sqlType);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setNull(int i, int sqlType, String typeName) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "setNull #" + i,
                     new Object[] {
                                    AdapterUtil.getSQLTypeString(sqlType),
                                    typeName });

        try {
            pstmtImpl.setNull(i, sqlType, typeName);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setObject(int i, Object x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setObject #" + i);

        try {
            pstmtImpl.setObject(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setObject(int i, Object x, int targetSqlType) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setObject #" + i, new Integer(targetSqlType));

        try {
            pstmtImpl.setObject(i, x, targetSqlType);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setObject(int i, Object x, int targetSqlType, int scale) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "setObject #" + i,
                     new Object[] { new Integer(targetSqlType), new Integer(scale) });

        try {
            pstmtImpl.setObject(i, x, targetSqlType, scale);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setRef(int i, Ref x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setRef #" + i);

        try {
            pstmtImpl.setRef(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setShort(int i, short x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setShort #" + i);

        try {
            pstmtImpl.setShort(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setString(int i, String x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setString #" + i);

        try {
            pstmtImpl.setString(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setTime(int i, Time x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setTime #" + i);

        try {
            pstmtImpl.setTime(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setTime(int i, Time x, Calendar cal) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setTime with Calendar #" + i);

        try {
            pstmtImpl.setTime(i, x, cal);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setTimestamp(int i, Timestamp x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setTimestamp #" + i);

        try {
            pstmtImpl.setTimestamp(i, x);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setTimestamp(int i, Timestamp x, Calendar cal) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setTimestamp with Calendar #" + i);

        try {
            pstmtImpl.setTimestamp(i, x, cal);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public void setUnicodeStream(int i, java.io.InputStream x, int length) throws SQLException {
        /*
         * if (tc.isDebugEnabled())
         * Tr.debug(tc, "setUnicodeStream #" + i, new Integer(length));
         *
         * try {
         * pstmtImpl.setUnicodeStream(i, x, length);
         * }
         * catch (SQLException ex) {
         * if (tc.isDebugEnabled())
         * Tr.debug(
         * tc,
         * "setUnicodeStream",
         * new Object[] {
         * "SQL STATE:  " + ex.getSQLState(),
         * "ERROR CODE: " + ex.getErrorCode(),
         * ex });
         * throw AdapterUtil.mapException(this, ex);
         * }
         * catch (NullPointerException nullX) {
         * // No FFDC code needed; we might be closed.
         * throw runtimeXIfNotClosed(nullX);
         * }
         */

    }

    // LIDB???? starts
    /**
     * <p>Sets the designated parameter to the given java.net.URL value. The driver
     * converts this to an SQL DATALINK value when it sends it to the database.</p>
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param url the java.net.URL object to be set
     *
     * @exception SQLException If a database access error occurs
     */
    @Override
    public void setURL(int i, java.net.URL url) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setURL", new Object[] {
                                                  new Integer(i), url });

        try {
            pstmtImpl.setURL(i, url);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Method getParameterMetaData.
     * <p>Retrieves the number, types and properties of this PreparedStatement object's
     * parameters. </p>
     *
     * @return a ParameterMetaData object that contains information about the number,
     *         types and properties of this PreparedStatement object's parameters
     *
     * @throws SQLException If a database access error occurs
     */
    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getParameterMetaData", this);
        ParameterMetaData pmd = null;

        try {
            pmd = pstmtImpl.getParameterMetaData();
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getMetaData", "Exception, details in FFDC");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getMetaData", "Exception, details in FFDC");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getParameterMetaData", pmd);
        return pmd;
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {}

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {}

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {}

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {}

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {}

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {}

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {}

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {}

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {}

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {}

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {}

    @Override
    public void setNClob(int x, java.io.Reader reader) throws SQLException {}//end

    @Override
    public void setBlob(int x, java.io.InputStream is) throws SQLException {}//end

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {}

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {}

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {}

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {}

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {}

}

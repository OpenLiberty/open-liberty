/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.ws.ejbcontainer.fat.rar.core.AdapterUtil;

/**
 * Wrapper class for CallableStatement object. <p>
 */
public class JdbcCallableStatement extends JdbcPreparedStatement implements CallableStatement {
    private final static String CLASSNAME = JdbcCallableStatement.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** The underlying CallableStatement object. */
    private CallableStatement cstmtImpl;

    /**
     * Create a WebSphere CallableStatement wrapper.
     *
     * @param cstmtImplObject the JDBC CallableStatement implementation class to be wrapped.
     * @param connWrapper the WebSphere JDBC Connection wrapper creating this statement.
     */
    JdbcCallableStatement(CallableStatement cstmtImplObject, JdbcConnection connWrapper) {
        svLogger.entering(CLASSNAME, "<init>", new Object[] {
                                                              AdapterUtil.toString(cstmtImplObject),
                                                              connWrapper });
        stmtImpl = pstmtImpl = cstmtImpl = cstmtImplObject;
        parentWrapper = connWrapper;
        childWrappers = new java.util.Vector(1);
        svLogger.exiting(CLASSNAME, "<init>", this);
    }

    /**
     * Perform any wrapper-specific close logic. This method is called by the default
     * JdbcObject close method.
     *
     * @return SQLException the first error to occur while closing the object.
     */
    @Override
    final SQLException closeWrapper() {
        cstmtImpl = null;
        // Delegate to the Statement wrapper, not the PreparedStatement wrapper, since the
        // PreparedStatement wrapper likes to cache stuff and we don't want that.
        return closeStatementWrapper();
    }

    @Override
    public Array getArray(int i) throws SQLException {
        try {
            return cstmtImpl.getArray(i);
        } catch (SQLException ex) {
            svLogger.info("getArray - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public BigDecimal getBigDecimal(int i) throws SQLException {
        try {
            return cstmtImpl.getBigDecimal(i);
        } catch (SQLException ex) {
            svLogger.info("getBigDecimal - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public BigDecimal getBigDecimal(int i, int scale) throws SQLException {
        return null;
    }

    @Override
    public Blob getBlob(int i) throws SQLException {
        try {
            return cstmtImpl.getBlob(i);
        } catch (SQLException ex) {
            svLogger.info("getBlob - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public boolean getBoolean(int i) throws SQLException {
        try {
            return cstmtImpl.getBoolean(i);
        } catch (SQLException ex) {
            svLogger.info("getBoolean - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public byte getByte(int i) throws SQLException {
        try {
            return cstmtImpl.getByte(i);
        } catch (SQLException ex) {
            svLogger.info("getByte - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public byte[] getBytes(int i) throws SQLException {
        try {
            return cstmtImpl.getBytes(i);
        } catch (SQLException ex) {
            svLogger.info("getBytes - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public Clob getClob(int i) throws SQLException {
        try {
            return cstmtImpl.getClob(i);
        } catch (SQLException ex) {
            svLogger.info("getClob - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public Date getDate(int i) throws SQLException {
        try {
            return cstmtImpl.getDate(i);
        } catch (SQLException ex) {
            svLogger.info("getDate - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public Date getDate(int i, Calendar cal) throws SQLException {
        try {
            return cstmtImpl.getDate(i, cal);
        } catch (SQLException ex) {
            svLogger.info("getDate - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public double getDouble(int i) throws SQLException {
        try {
            return cstmtImpl.getDouble(i);
        } catch (SQLException ex) {
            svLogger.info("getDouble - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public float getFloat(int i) throws SQLException {
        try {
            return cstmtImpl.getFloat(i);
        } catch (SQLException ex) {
            svLogger.info("getFloat - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public int getInt(int i) throws SQLException {
        try {
            return cstmtImpl.getInt(i);
        } catch (SQLException ex) {
            svLogger.info("getInt - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public long getLong(int i) throws SQLException {
        try {
            return cstmtImpl.getLong(i);
        } catch (SQLException ex) {
            svLogger.info("getLong - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public Object getObject(int i) throws SQLException {
        try {
            return cstmtImpl.getObject(i);
        } catch (SQLException ex) {
            svLogger.info("getObject - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public Object getObject(int i, Map map) throws SQLException {
        try {
            return cstmtImpl.getObject(i, map);
        } catch (SQLException ex) {
            svLogger.info("getObject - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public Ref getRef(int i) throws SQLException {
        try {
            return cstmtImpl.getRef(i);
        } catch (SQLException ex) {
            svLogger.info("getRef - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public short getShort(int i) throws SQLException {
        try {
            return cstmtImpl.getShort(i);
        } catch (SQLException ex) {
            svLogger.info("getShort - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public String getString(int i) throws SQLException {
        try {
            return cstmtImpl.getString(i);
        } catch (SQLException ex) {
            svLogger.info("getString - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public Time getTime(int i) throws SQLException {
        try {
            return cstmtImpl.getTime(i);
        } catch (SQLException ex) {
            svLogger.info("getTime - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public Time getTime(int i, Calendar cal) throws SQLException {
        try {
            return cstmtImpl.getTime(i, cal);
        } catch (SQLException ex) {
            svLogger.info("getTime - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public Timestamp getTimestamp(int i) throws SQLException {
        try {
            return cstmtImpl.getTimestamp(i);
        } catch (SQLException ex) {
            svLogger.info("getTimestamp - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public Timestamp getTimestamp(int i, Calendar cal) throws SQLException {
        try {
            return cstmtImpl.getTimestamp(i, cal);
        } catch (SQLException ex) {
            svLogger.info("getTimestamp - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void registerOutParameter(int i, int sqlType) throws SQLException {
        svLogger.entering(CLASSNAME, "registerOutParameter", new Object[] {
                                                                            this,
                                                                            new Integer(i),
                                                                            AdapterUtil.getSQLTypeString(sqlType) });

        try {
            cstmtImpl.registerOutParameter(i, sqlType);
        } catch (SQLException ex) {
            svLogger.info("setUnicodeStream - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void registerOutParameter(int i, int sqlType, int scale) throws SQLException {
        svLogger.entering(CLASSNAME, "registerOutParameter", new Object[] {
                                                                            this,
                                                                            new Integer(i),
                                                                            AdapterUtil.getSQLTypeString(sqlType),
                                                                            new Integer(scale) });

        try {
            cstmtImpl.registerOutParameter(i, sqlType, scale);
        } catch (SQLException ex) {
            svLogger.info("registerOutParameter - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void registerOutParameter(int i, int sqlType, String typeName) throws SQLException {
        svLogger.entering(CLASSNAME, "registerOutParameter", new Object[] {
                                                                            this,
                                                                            new Integer(i),
                                                                            AdapterUtil.getSQLTypeString(sqlType),
                                                                            typeName });

        try {
            cstmtImpl.registerOutParameter(i, sqlType, typeName);
        } catch (SQLException ex) {
            svLogger.info("registerOutParameter - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public boolean wasNull() throws SQLException {
        try {
            return cstmtImpl.wasNull();
        } catch (SQLException ex) {
            svLogger.info("wasNull - SQL STATE:  " + ex.getSQLState() + ", ERROR CODE: " + ex.getErrorCode());
            throw ex;
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
    }

    @Override
    public void setURL(String parameterName, URL val) throws SQLException {
    }

    @Override
    public void setNull(String parameterName, int sqlType) throws SQLException {
    }

    @Override
    public void setBoolean(String parameterName, boolean x) throws SQLException {
    }

    @Override
    public void setByte(String parameterName, byte x) throws SQLException {
    }

    @Override
    public void setShort(String parameterName, short x) throws SQLException {
    }

    @Override
    public void setInt(String parameterName, int x) throws SQLException {
    }

    @Override
    public void setLong(String parameterName, long x) throws SQLException {
    }

    @Override
    public void setFloat(String parameterName, float x) throws SQLException {
    }

    @Override
    public void setDouble(String parameterName, double x) throws SQLException {
    }

    @Override
    public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
    }

    @Override
    public void setString(String parameterName, String x) throws SQLException {
    }

    @Override
    public void setBytes(String parameterName, byte[] x) throws SQLException {
    }

    @Override
    public void setDate(String parameterName, Date x) throws SQLException {
    }

    @Override
    public void setTime(String parameterName, Time x) throws SQLException {
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
    }

    @Override
    public void setObject(String parameterName, Object x) throws SQLException {
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
    }

    @Override
    public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
    }

    @Override
    public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
    }

    @Override
    public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
    }

    @Override
    public Array getArray(String parameterName) throws SQLException {
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Blob getBlob(String parameterName) throws SQLException {
        return null;
    }

    @Override
    public boolean getBoolean(String parameterName) throws SQLException {
        return false;
    }

    @Override
    public byte getByte(String parameterName) throws SQLException {
        return 0;
    }

    @Override
    public byte[] getBytes(String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Clob getClob(String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Date getDate(String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Date getDate(String parameterName, Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public double getDouble(String parameterName) throws SQLException {
        return 0;
    }

    @Override
    public float getFloat(String parameterName) throws SQLException {
        return 0;
    }

    @Override
    public int getInt(String parameterName) throws SQLException {
        return 0;
    }

    @Override
    public long getLong(String parameterName) throws SQLException {
        return 0;
    }

    @Override
    public Object getObject(String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Object getObject(String parameterName, Map map) throws SQLException {
        return null;
    }

    @Override
    public Ref getRef(String parameterName) throws SQLException {
        return null;
    }

    @Override
    public short getShort(String parameterName) throws SQLException {
        return 0;
    }

    @Override
    public String getString(String s) throws SQLException {
        return null;
    }

    @Override
    public Time getTime(String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Time getTime(String parameterName, Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public Timestamp getTimestamp(String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public URL getURL(int parameterIndex) throws SQLException {
        return null;
    }

    @Override
    public URL getURL(String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Reader getCharacterStream(String parameterName) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Reader getNCharacterStream(String parameterName) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NClob getNClob(int parameterIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NClob getNClob(String parameterName) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNString(int parameterIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNString(String parameterName) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RowId getRowId(int parameterIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RowId getRowId(String parameterName) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLXML getSQLXML(String parameterName) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setBlob(String parameterName, Blob x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setClob(String parameterName, Clob x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setNClob(String parameterName, NClob value) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setNString(String parameterName, String value) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setRowId(String parameterName, RowId x) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setNClob(java.lang.String x, java.io.Reader reader) throws SQLException {
    }

    @Override
    public void setBlob(java.lang.String x, java.io.InputStream inputstream) throws SQLException {
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
    }

    @Override
    public void setClob(String parameterName, Reader reader) throws SQLException {
    }

    // @Override
    @Override
    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        // Stub for JDBC 4.1
        return null;
    }

    // @Override
    @Override
    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        // Stub for JDBC 4.1
        return null;
    }
}
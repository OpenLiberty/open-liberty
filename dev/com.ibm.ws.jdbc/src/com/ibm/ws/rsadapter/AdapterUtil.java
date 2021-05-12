/*******************************************************************************
 * Copyright (c) 2001, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException; 
import java.sql.SQLNonTransientConnectionException; 
import java.sql.SQLRecoverableException; 
import java.sql.SQLTransientConnectionException; 
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ResourceAllocationException;
import javax.resource.spi.SecurityException;

import com.ibm.websphere.ce.cm.ConnectionWaitTimeoutException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection; 
import com.ibm.ws.rsadapter.jdbc.WSJdbcTracer;
import com.ibm.ws.rsadapter.jdbc.WSJdbcUtil;
import com.ibm.ws.rsadapter.impl.DatabaseHelper;
import com.ibm.ws.rsadapter.impl.WSConnectionEvent;
import com.ibm.ws.rsadapter.impl.WSManagedConnectionFactoryImpl; 
import com.ibm.ws.rsadapter.impl.WSRdbManagedConnectionImpl;
import com.ibm.wsspi.library.Library;

/**
 * This is a utility class containing static methods for use throughout the JDBC integration code.
 */
public class AdapterUtil {
    public static final String NLS_FILE = "com.ibm.ws.rsadapter.resources.IBMDataStoreAdapterNLS"; 
    public static final String TRACE_GROUP = "RRA";

    private static final TraceComponent tc = Tr.register(AdapterUtil.class, TRACE_GROUP, NLS_FILE);

    /**
     * Sets a system property that provides a transaction manager wrapper for
     * DB2 T2 to synchronize with the underlying unit of work.
     * Setting this property here guarantees that it will be set prior to the
     * JCC driver being loaded.
     */
    static {
        AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.setProperty("com.ibm.db2.java.TMClassName", "com.ibm.wsspi.zos.tx.RRSTXSynchronizationManager");
            }
        });
    }

    /**
     * com.ddtek.jdbc.extensions.ExtConstants.TRANSACTION_SNAPSHOT
     */
    public static final int TRANSACTION_SNAPSHOT = 16;

    /**
     * com.microsoft.sqlserver.jdbc.SQLServerConnection.TRANSACTION_SNAPSHOT
     */
    public static final int TRANSACTION_SS_SNAPSHOT = 4096;

    /**
     * com.microsoft.sqlserver.jdbc.SQLServerResultSet.CONCUR_SS_SCROLL_LOCKS
     */
    public static final int CONCUR_SS_SCROLL_LOCKS = 1009;

    /**
     * com.microsoft.sqlserver.jdbc.SQLServerResultSet.CONCUR_SS_OPTIMISTIC_CCVAL
     */
    public static final int CONCUR_SS_OPTIMISTIC_CCVAL = 1010;

    /**
     * com.microsoft.sqlserver.jdbc.SQLServerResultSet.TYPE_SS_SCROLL_DYNAMIC
     */
    public static final int TYPE_SS_SCROLL_DYNAMIC = 1006;

    /**
     * com.microsoft.sqlserver.jdbc.SQLServerResultSet.TYPE_SS_DIRECT_FORWARD_ONLY
     */
    public static final int TYPE_SS_DIRECT_FORWARD_ONLY = 2003;

    /**
     * com.microsoft.sqlserver.jdbc.SQLServerResultSet.TYPE_SS_SERVER_CURSOR_FORWARD_ONLY
     */
    public static final int TYPE_SS_SERVER_CURSOR_FORWARD_ONLY = 2004;

    /** The end-of-line character. */
    public static final String EOLN = String.format("%n");

    /** A blank string for indenting. */
    private static final String INDENT = "                                 ";

    /** Limits the number of times the user is warned about using begin/endRequest. */
    private static volatile boolean warnedAboutBeginAndEndRequest;

    /**
     * Create an XAException with a translated error message and an XA error code.
     * The XAException constructors provided by the XAException API allow only for either an
     * error message or an XA error code to be specified. This method constructs an
     * XAException with both. The error message is created from the NLS key and arguments.
     * 
     * @param key the NLS key.
     * @param args Object or Object[] listing parameters for the message; can be null if none.
     * @param xaErrorCode the XA error code.
     * 
     * @return a newly constructed XAException with the specified XA error code and a
     *         descriptive error message.
     * 
     */
    public static XAException createXAException(String key, Object args, int xaErrorCode) {
        XAException xaX = new XAException(
                        args == null ? getNLSMessage(key) :
                        args instanceof Object[] ? getNLSMessage(key, (Object[]) args) :
                        getNLSMessage(key, args));

        xaX.errorCode = xaErrorCode;

        return xaX;
    }

    /**
     * Retrieves the exception chained to the specified Throwable.
     * For the purposes of this method, chained exception accessors are defined as 
     * any zero-parameter method returning a Throwable and having a name of: 
     * get*Cause, get*Exception, get*Error, get*Warning. 
     * 
     * If multiple accessors exist, each is tried until one returns a non-null result. 
     * 
     * @param th the Exception or Error to retrieve the chained exception of.
     * 
     * @return the chained exception, or null if there is no chained exception.
     */
    public static Throwable getChainedException(Throwable th) {
        java.lang.reflect.Method[] methods = th.getClass().getMethods();

        // Need to keep looking if we find a valid method but it returns null. For some
        // throwables, like ResourceException, getLinkedException will be null but
        // getCause will be non-null, or vice versa. 

        Throwable chainedX = null; 

        for (int i = 0; i < methods.length && chainedX == null; i++)

            if (Throwable.class.isAssignableFrom(methods[i].getReturnType()) &&
                !methods[i].getName().equals("getSQLException") && 
                methods[i].getName().startsWith("get") && 
                methods[i].getParameterTypes().length == 0 && 
                (methods[i].getName().endsWith("Cause") || 
                 methods[i].getName().endsWith("Exception") || 
                 methods[i].getName().endsWith("Error") || 
                methods[i].getName().endsWith("Warning"))) 

                try {
                    chainedX = (Throwable) methods[i].invoke(th, (Object[]) null); 
                } catch (Throwable ex) {
                    // No FFDC code needed; ignore.
                }

        return chainedX; 
    }

    /**
     * Display the java.sql.ResultSet concurrency mode constant corresponding to the value
     * supplied.
     * 
     * @param level a valid java.sql.ResultSet concurrency mode constant.
     * 
     * @return the name of the constant, or a string indicating the constant is unknown.
     */
    public static String getConcurrencyModeString(int concurrency) {
        switch (concurrency) {
            case ResultSet.CONCUR_READ_ONLY:
                return "CONCUR READ ONLY (" + concurrency + ')'; 

            case ResultSet.CONCUR_UPDATABLE:
                return "CONCUR UPDATABLE (" + concurrency + ')'; 

            case CONCUR_SS_SCROLL_LOCKS: 
                return "CONCUR SS SCROLL LOCKS (" + concurrency + ')'; 

            case CONCUR_SS_OPTIMISTIC_CCVAL: 
                return "CONCUR SS OPTIMISTIC CCVAL (" + concurrency + ')'; 
        }

        return "UNKNOWN RESULT SET CONCURRENCY (" + concurrency + ')'; 
    }

    /**
     * Display the name of the ConnectionEvent constant given the event id.
     * 
     * @param eventID a valid ConnectionEvent constant value.
     * 
     * @return The name of the ConnectionEvent constant, or a string indicating the constant
     *         is unknown.
     */
    public static String getConnectionEventString(int eventID) {
        switch (eventID) {

            case ConnectionEvent.CONNECTION_CLOSED: 
                return "CONNECTION CLOSED (" + eventID + ')'; 

            case ConnectionEvent.LOCAL_TRANSACTION_STARTED: 
                return "LOCAL TRANSACTION STARTED (" + eventID + ')'; 

            case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED: 
                return "LOCAL TRANSACTION COMMITTED (" + eventID + ')'; 

            case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK: 
                return "LOCAL TRANSACTION ROLLEDBACK (" + eventID + ')'; 

            case ConnectionEvent.CONNECTION_ERROR_OCCURRED: 
                return "CONNECTION ERROR OCCURRED (" + eventID + ')'; 

            case WSConnectionEvent.CONNECTION_ERROR_OCCURRED_NO_EVENT:
                return "CONNECTION ERROR OCCURRED NO EVENT (" + eventID + ')';

            case WSConnectionEvent.SINGLE_CONNECTION_ERROR_OCCURRED:
                return "SINGLE CONNECTION ERROR OCCURRED (" + eventID + ')';
        }

        return "UNKNOWN CONNECTION EVENT CONSTANT (" + eventID + ')'; 
    }

    /**
     * Display the java.sql.ResultSet fetch direction constant corresponding to the value
     * supplied.
     * 
     * @param level a valid java.sql.ResultSet fetch direction constant.
     * 
     * @return the name of the constant, or a string indicating the constant is unknown.
     */
    public static String getFetchDirectionString(int direction) {
        switch (direction) {
            case ResultSet.FETCH_FORWARD:
                return "FETCH FORWARD (" + direction + ')'; 

            case ResultSet.FETCH_REVERSE:
                return "FETCH REVERSE (" + direction + ')'; 

            case ResultSet.FETCH_UNKNOWN:
                return "FETCH UNKNOWN (" + direction + ')'; 
        }

        return "UNRECOGNIZED FETCH DIRECTION CONSTANT (" + direction + ')'; 
    }

    /**
     * Display the java.sql.Connection isolation level constant corresponding to the value
     * supplied.
     * 
     * @param level a valid java.sql.Connection transaction isolation level constant.
     * 
     * @return the name of the constant, or a string indicating the constant is unknown.
     */
    public static String getIsolationLevelString(int level) {
        switch (level) {
            case Connection.TRANSACTION_NONE:
                return "NONE (" + level + ')'; 

            case Connection.TRANSACTION_READ_UNCOMMITTED:
                return "READ UNCOMMITTED (" + level + ')'; 

            case Connection.TRANSACTION_READ_COMMITTED:
                return "READ COMMITTED (" + level + ')'; 

            case Connection.TRANSACTION_REPEATABLE_READ:
                return "REPEATABLE READ (" + level + ')'; 

            case Connection.TRANSACTION_SERIALIZABLE:
                return "SERIALIZABLE (" + level + ')'; 
               
            case TRANSACTION_SNAPSHOT: 
            case TRANSACTION_SS_SNAPSHOT: 
                return "SNAPSHOT (" + level + ')'; 
        }

        return "UNKNOWN ISOLATION LEVEL CONSTANT (" + level + ')'; 
    }

    /**
     * Retrieve the message corresponding to the supplied key from the IBMDataStoreAdapterNLS
     * properties file. If the message cannot be found, the key is returned.
     * 
     * @param key a valid message key from IBMDataStoreAdapterNLS.properties.
     * 
     * @return a translated message.
     */
    public static final String getNLSMessage(String key) {
        return Tr.formatMessage(tc, key);
    }

    /**
     * Retrieve the message corresponding to the supplied key from the IBMDataStoreAdapterNLS
     * properties file. If the message cannot be found, the key is returned.
     * 
     * @param key a valid message key from IBMDataStoreAdapterNLS.properties.
     * @param args a list of parameters to include in the translatable message.
     * 
     * @return a translated message.
     */
    public static final String getNLSMessage(String key, Object... args) {
        return Tr.formatMessage(tc, key, args);
    }

    /**
     * Display the java.sql.ResultSet result set type constant corresponding to the value
     * supplied.
     * 
     * @param level a valid java.sql.ResultSet result set type constant.
     * 
     * @return the name of the constant, or a string indicating the constant is unknown.
     */
    public static String getResultSetTypeString(int type) {
        switch (type) {
            case ResultSet.TYPE_FORWARD_ONLY:
                return "TYPE FORWARD ONLY (" + type + ')'; 

            case ResultSet.TYPE_SCROLL_INSENSITIVE:
                return "TYPE SCROLL INSENSITIVE (" + type + ')'; 

            case ResultSet.TYPE_SCROLL_SENSITIVE:
                return "TYPE SCROLL SENSITIVE (" + type + ')'; 

            case TYPE_SS_SCROLL_DYNAMIC: 
                return "TYPE SS SCROLL DYNAMIC (" + type + ')'; 

            case TYPE_SS_DIRECT_FORWARD_ONLY: 
                return "TYPE SS DIRECT FORWARD ONLY (" + type + ')'; 

            case TYPE_SS_SERVER_CURSOR_FORWARD_ONLY: 
                return "TYPE SS SERVER CURSOR FORWARD ONLY (" + type + ')'; 
        }

        return "UNKNOWN RESULT SET TYPE CONSTANT (" + type + ')'; 
    }

    /**
     * Display the java.sql.Types SQL Type constant corresponding to the value supplied.
     * 
     * @param level a valid java.sql.Types SQL Type constant.
     * 
     * @return the name of the constant, or a string indicating the constant is unknown.
     */
    public static String getSQLTypeString(int sqlType) {
        switch (sqlType) {
            case Types.ARRAY:
                return "ARRAY (" + sqlType + ')'; 

            case Types.BIGINT:
                return "BIGINT (" + sqlType + ')'; 

            case Types.BINARY:
                return "BINARY (" + sqlType + ')'; 

            case Types.BIT:
                return "BIT (" + sqlType + ')'; 

            case Types.BLOB:
                return "BLOB (" + sqlType + ')'; 

            case Types.BOOLEAN: 
                return "BOOLEAN (" + sqlType + ')'; 

            case Types.CHAR:
                return "CHAR (" + sqlType + ')'; 

            case Types.CLOB:
                return "CLOB (" + sqlType + ')'; 

            case Types.DATALINK: 
                return "DATALINK (" + sqlType + ')'; 

            case Types.DATE:
                return "DATE (" + sqlType + ')'; 

            case Types.DECIMAL:
                return "DECIMAL (" + sqlType + ')'; 

            case Types.DISTINCT:
                return "DISTINCT (" + sqlType + ')'; 

            case Types.DOUBLE:
                return "DOUBLE (" + sqlType + ')'; 

            case Types.FLOAT:
                return "FLOAT (" + sqlType + ')'; 

            case Types.INTEGER:
                return "INTEGER (" + sqlType + ')'; 

            case Types.JAVA_OBJECT:
                return "JAVA OBJECT (" + sqlType + ')'; 

            case Types.LONGNVARCHAR: 
                return "LONGNVARCHAR (" + sqlType + ')'; 

            case Types.LONGVARBINARY:
                return "LONGVARBINARY (" + sqlType + ')'; 

            case Types.LONGVARCHAR:
                return "LONGVARCHAR (" + sqlType + ')'; 

            case Types.NCHAR: 
                return "NCHAR (" + sqlType + ')'; 

            case Types.NCLOB: 
                return "NCLOB (" + sqlType + ')'; 

            case Types.NULL:
                return "NULL (" + sqlType + ')'; 

            case Types.NUMERIC:
                return "NUMERIC (" + sqlType + ')'; 

            case Types.NVARCHAR: 
                return "NVARCHAR (" + sqlType + ')'; 

            case Types.OTHER:
                return "OTHER (" + sqlType + ')'; 

            case Types.REAL:
                return "REAL (" + sqlType + ')'; 

            case Types.REF:
                return "REF (" + sqlType + ')'; 

            case Types.ROWID: 
                return "ROWID (" + sqlType + ')'; 

            case Types.SMALLINT:
                return "SMALLINT (" + sqlType + ')'; 

            case Types.SQLXML: 
                return "SQLXML (" + sqlType + ')'; 

            case Types.STRUCT:
                return "STRUCT (" + sqlType + ')'; 

            case Types.TIME:
                return "TIME (" + sqlType + ')'; 

            case Types.TIMESTAMP:
                return "TIMESTAMP (" + sqlType + ')'; 

            case Types.TINYINT:
                return "TINYINT (" + sqlType + ')'; 

            case Types.VARBINARY:
                return "VARBINARY (" + sqlType + ')'; 

            case Types.VARCHAR:
                return "VARCHAR (" + sqlType + ')'; 
        }

        return "UNKNOWN SQL TYPE (" + sqlType + ')'; 
    }

    /**
     * Used by trace facilities to print out XAException code as a string
     * 
     * @param code a valid javax.transaction.xa.XAException error code constant.
     * 
     * @return the name of the constant, or a string indicating the constant is unknown.
     */
    public static String getXAExceptionCodeString(int code) {

        // Note, two cases are commented out below.  This is because in the implementation of
        // XAException, they have defined two constants to be the same int (or so it would
        // seem).

        // This is because XA_RBBASE and XA_RBEND define the lower and upper bounds of the
        // XA_RB error codes.  These two are correctly commented out below.

        switch (code) {
            case XAException.XA_RBTRANSIENT:
                return "XA_RBTRANSIENT (" + code + ')'; 
            case XAException.XA_RBROLLBACK:
                return "XA_RBROLLBACK (" + code + ')'; 
            case XAException.XA_HEURCOM:
                return "XA_HEURCOM (" + code + ')'; 
            case XAException.XA_HEURHAZ:
                return "XA_HEURHAZ (" + code + ')'; 
            case XAException.XA_HEURMIX:
                return "XA_HEURMIX (" + code + ')'; 
            case XAException.XA_HEURRB:
                return "XA_HEURRB (" + code + ')'; 
            case XAException.XA_NOMIGRATE:
                return "XA_NOMIGRATE (" + code + ')'; 
                //case XAException.XA_RBBASE:
                //    return "XA_RBBASE (" + code + ')'; 
            case XAException.XA_RBCOMMFAIL:
                return "XA_RBCOMMFAIL (" + code + ')'; 
            case XAException.XA_RBDEADLOCK:
                return "XA_RBDEADLOCK (" + code + ')'; 
                //case XAException.XA_RBEND:
                //    return "XA_RBEND (" + code + ')'; 
            case XAException.XA_RBINTEGRITY:
                return "XA_RBINTEGRITY (" + code + ')'; 
            case XAException.XA_RBOTHER:
                return "XA_RBOTHER (" + code + ')'; 
            case XAException.XA_RBPROTO:
                return "XA_RBPROTO (" + code + ')'; 
            case XAException.XA_RBTIMEOUT:
                return "XA_RBTIMEOUT (" + code + ')'; 
            case XAException.XA_RDONLY:
                return "XA_RDONLY (" + code + ')'; 
            case XAException.XA_RETRY:
                return "XA_RETRY (" + code + ')'; 
            case XAException.XAER_ASYNC:
                return "XAER_ASYNC (" + code + ')'; 
            case XAException.XAER_DUPID:
                return "XAER_DUPID (" + code + ')'; 
            case XAException.XAER_INVAL:
                return "XAER_INVAL (" + code + ')'; 
            case XAException.XAER_NOTA:
                return "XAER_NOTA (" + code + ')'; 
            case XAException.XAER_OUTSIDE:
                return "XAER_OUTSIDE (" + code + ')'; 
            case XAException.XAER_PROTO:
                return "XAER_PROTO (" + code + ')'; 
            case XAException.XAER_RMERR:
                return "XAER_RMERR (" + code + ')'; 
            case XAException.XAER_RMFAIL:
                return "XAER_RMFAIL (" + code + ')'; 

        }
        return "UNKNOWN XA EXCEPTION CODE (" + code + ')'; 
    }

    /**
     * Display the javax.xa.XAResource end flag constant corresponding to the
     * value supplied.
     * 
     * @param level a valid javax.xa.XAResource end flag constant.
     * 
     * @return the name of the constant, or a string indicating the constant is unknown.
     */
    public static String getXAResourceEndFlagString(int flag) {
        switch (flag) {
            case XAResource.TMFAIL:
                return "TMFAIL (" + flag + ')'; 

            case XAResource.TMSUCCESS:
                return "TMSUCCESS (" + flag + ')'; 

            case XAResource.TMSUSPEND:
                return "TMSUSPEND (" + flag + ')'; 
        }

        return "UNKNOWN XA RESOURCE END FLAG (" + flag + ')'; 
    }

    /**
     * Display the javax.xa.XAResource recover flag constant corresponding to the
     * value supplied.
     * 
     * @param level a valid javax.xa.XAResource recover flag constant.
     * 
     * @return the name of the constant, or a string indicating the constant is unknown.
     */
    public static String getXAResourceRecoverFlagString(int flag) {
        switch (flag) {
            case XAResource.TMENDRSCAN:
                return "TMENDRSCAN (" + flag + ')'; 

            case XAResource.TMNOFLAGS:
                return "TMNOFLAGS (" + flag + ')'; 

            case XAResource.TMSTARTRSCAN:
                return "TMSTARTRSCAN (" + flag + ')'; 

            case XAResource.TMSTARTRSCAN + XAResource.TMENDRSCAN:
                return "TMSTARTRSCAN + TMENDRSCAN (" + flag + ')'; 

        }

        return "UNKNOWN XA RESOURCE RECOVER FLAG (" + flag + ')'; 
    }

    /**
     * Display the javax.xa.XAResource start flag constant corresponding to the
     * value supplied.
     * 
     * @param level a valid javax.xa.XAResource start flag constant.
     * 
     * @return the name of the constant, or a string indicating the constant is unknown.
     */
    public static String getXAResourceStartFlagString(int flag) {
        switch (flag) {
            case XAResource.TMJOIN:
                return "TMJOIN (" + flag + ')'; 

            case XAResource.TMNOFLAGS:
                return "TMNOFLAGS (" + flag + ')'; 

            case XAResource.TMRESUME:
                return "TMRESUME (" + flag + ')';

            case 0x8000:
                return "SSTRANSTIGHTLYCPLD (" + flag + ')'; // Microsoft SQL Server JDBC driver

            case 0x10000:
                return "ORATRANSLOOSE (" + flag + ')'; // Oracle JDBC driver

            case 0x800000:
                return "TMLCS (" + flag + ')'; // DB2 JCC driver
        }

        return "UNKNOWN XA RESOURCE START FLAG (" + flag + ')'; 
    }

    /**
     * Display the javax.xa.XAResource Resource Manager vote constant corresponding to the
     * value supplied.
     * 
     * @param level a valid javax.xa.XAResource vote constant.
     * 
     * @return the name of the constant, or a string indicating the constant is unknown.
     */
    public static String getXAResourceVoteString(int vote) {
        switch (vote) {
            case XAResource.XA_OK:
                return "XA_OK (" + vote + ')'; 

            case XAResource.XA_RDONLY:
                return "XA_RDONLY (" + vote + ')'; 
        }

        return "UNKNOWN XA RESOURCE VOTE (" + vote + ')'; 
    }

    /**
     * Utility method that determines if a SQLException inherits from the specified legacy exception class.
     *
     * @param x exception that might extend the legacy exception class.
     * @return true if an instance of the legacy exception type, otherwise false.
     */
    private static boolean isLegacyException(SQLException x, String className) {
        for (Class<?> c = x.getClass(); c != null; c = c.getSuperclass())
            if (c.getName().equals(className))
                return true;
        return false;
    }

    private static final String os = AccessController.doPrivileged(new PrivilegedAction<String>() {
        @Override
        public String run() {
            return System.getProperty("os.name");
        }
    });

    /**
     * Indicates if the current JVM is running on a z/OS platform.
     * 
     * @return true if the current JVM is running on a z/OS platform, false otherwise
     */
    public static final boolean isZOS() {
        // NOTE: This is a temporary implementation due to the fact that most of the TWAS code for
        // this component was ported as is into Liberty. Going forward, this method must be deleted 
        // or replaced by platform agnostic checks whose decision is based on service availability
        // rather than platform.
        return os != null && (os.equalsIgnoreCase("OS/390") || os.equalsIgnoreCase("z/OS"));
    }

    /**
     * Determine if two objects, either of which may be null, are equal.
     * 
     * @param obj1 one object.
     * @param obj2 another object.
     * 
     * @return true if the objects are equal or are both null, otherwise false.
     */
    public static final boolean match(Object obj1, Object obj2) {
        return obj1 == obj2 || (obj1 != null && obj1.equals(obj2));
    }

    /**
     * Creates an exception indicating the requested operation is not supported.
     * 
     * @param operation the name of the operation that isn't supported.
     * @param cause the error that allowed us to determine the operation is not supported,
     *            or NULL, if determined by some other mechanism.
     *            The error is chained to the exception created by this method.
     * 
     * @return the exception.
     */
    public static final SQLException notSupportedX(String operation, Throwable cause) {
        SQLException sqlX = new SQLFeatureNotSupportedException( 
        getNLSMessage("FEATURE_NOT_IMPLEMENTED", operation));
        if (cause != null)
            sqlX.initCause(cause);
        return sqlX;
    }
    
    /**
     * Formats an exception's stack trace as a String.
     * 
     * @param th a throwable object (Exception or Error)
     * 
     * @return String containing the exception's stack trace.
     */
    public static String stackTraceToString(Throwable th) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for (int depth = 0; depth < 10 && th != null; depth++) {
            th.printStackTrace(pw);
            Throwable cause = th.getCause();
            if (cause != null && cause != th) {
                pw.append("-------- chained exception -------").append(EOLN);
            }
            th = cause;
        }
        return sw.toString();
    }

    /**
     * Create a SQLRecoverableException.
     * 
     * @return the exception.
     */
    public static final SQLException staleX() {
        String message = getNLSMessage("INVALID_CONNECTION");
        return new SQLRecoverableException(message);
    }

    /**
     * Logs a warning message indicating that Connection.beginRequest and Connection.endRequest
     * methods are suppressed.
     */
    public static final void suppressBeginAndEndRequest() {
        if (!warnedAboutBeginAndEndRequest) {
            warnedAboutBeginAndEndRequest = true;

            StringBuilder stack = new StringBuilder();
            for (StackTraceElement frame : new Exception().getStackTrace())
                stack.append(EOLN).append(frame.toString());

            Tr.warning(tc, "DSRA8790_SUPPRESS_BEGIN_END_REQUEST", stack);
        }
    }

    /**
     * Translates a SQLException from the database. The exception mapping methods
     * have been rewritten to account for the error detection model and to consolidate
     * the RRA exception mapping routines into a single place.
     * See the AdapterUtil.mapException method.
     * 
     * @param SQLException se - the SQLException from the database
     * @param mapper a managed connection or managed connection factory capable of mapping
     *            the exception. If NULL is provided, the exception cannot be mapped.
     *            If stale statement handling or connection error handling is required then a
     *            managed connection must be provided.
     * @param boolean sendEvent - if true, throw an exception if the SQLException is a
     *        connectionError event
     * @return ResourceException - the translated SQLException wrapped in a
     *         ResourceException
     */
    public static ResourceException translateSQLException(
                                                                  SQLException se,
                                                                  Object mapper,
                                                                  boolean sendEvent,
                                                                  Class<?> caller) {
        return (ResourceException) mapException(
                                                        new DataStoreAdapterException("DSA_ERROR", se, caller, se.getMessage()),
                                                        null,
                                                        mapper,
                                                        sendEvent);
    }

    /**
     * Converts a String to a Properties object using the load method on java.util.Properties.
     * A semicolon (;) may be used in place of the end-of-line character to delimit property
     * entries.
     * 
     * @param properString the String to convert to a Properties object.
     * 
     * @return the resulting Properties object.
     * 
     * @throws IOException if an error occurs reading the String.
     */
    public static Properties toProperties(String properString) throws java.io.IOException {
        Properties p = new Properties();

        p.load(new java.io.ByteArrayInputStream(properString.replaceAll(";", EOLN).getBytes()));

        return p;
    }

    /**
     * Converts a ResourceException to a SQLException. Returns the first chained exception
     * found to be a SQLException or ConnectionWaitTimeoutException. If none of the
     * chained exceptions are SQLExceptions or ConnectionWaitTimeoutExceptions, a new
     * SQLException is created containing information from the ResourceException.
     * 
     * @param resX a javax.resource.ResourceException
     * 
     * @return a SQLException
     */
    public static SQLException toSQLException(ResourceException resX)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "toSQLException", resX);

        SQLException sqlX = null;

        for (Throwable linkedX = resX; linkedX != null; linkedX = getChainedException(linkedX)) {
            if (linkedX instanceof SQLException) {
                sqlX = (SQLException) linkedX;
                break;
            }
            // The J2C connection manager raises a special exception subclass.
            else if (linkedX instanceof ResourceAllocationException
                     && linkedX.getClass().getName().equals("com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException")) {
                // The following exception was changed from throwing an SQLTransientConnectionException 
                // to a ConnectionWaitTimeoutException instead to allow for traditional WebSphere applications to work since they expect a 
                // ConnectionWaitTimeoutException instead.  This will continue to work with existing liberty applications as well since 
                // SQLTransientConnectionException is the parent class of ConnectionWaitTimeoutException.  
                // The ConnectionWaitTimeoutException is linked to another ConnectionWaitTimeoutException for backwards compatibility for liberty applications
                // that were traversing the previous SQLTransientConnectionException to get to the underlying ConnectionWaitTimeoutException
                SQLException sqlX2 = new ConnectionWaitTimeoutException(linkedX.getMessage());
                // Keeping the original SQLState and SQLCode from the previous SQLTransientConnectionException
                sqlX = new ConnectionWaitTimeoutException(linkedX.getMessage(), "08001", 0, sqlX2);
                break;
            }
        }

        // If neither the original exception nor any of the chained exceptions were found to
        // be SQLExceptions or ConnectionWaitTimeoutExceptions, just convert the original
        // exception to a SQLException.
        if (sqlX == null) {
            // use the original exception to create a SQL Exception
            if (tc.isEventEnabled())
                Tr.event(tc, "Converting ResourceException to a SQLException");

            sqlX = new SQLException(resX.getMessage()); 
            sqlX.initCause(resX); 
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "toSQLException", getStackTraceWithState(sqlX)); 

        return sqlX;
    }

    /**
     * Converts any generic Exception to a SQLException.
     * 
     * @param ex the exception to convert.
     * 
     * @return SQLException containing the original Exception class, message, and stack trace.
     */
    public static SQLException toSQLException(Throwable ex) {
        if (ex == null)
            return null; 

        if (ex instanceof SQLException)
            return (SQLException) ex;

        if (ex instanceof ResourceException)
            return toSQLException((ResourceException) ex);

        // Link the original exception to the new SQLException. 

        SQLException sqlX = new SQLException(ex.getClass().getName() + ": " + ex.getMessage());
        sqlX.initCause(ex);

        return sqlX;
    }

    /**
     * @param bytes an array of bytes.
     * 
     * @return a Hexadecimal String representing the byte array.
     * 
     */
    public static String toString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        if (bytes != null)
            for (int i = 0; i < bytes.length; i++) {
                int b = bytes[i] < 0 ? 0x100 + bytes[i] : bytes[i];
                sb.append(Integer.toHexString(b / 0x10))
                                .append(Integer.toHexString(b % 0x10))
                                .append(' ');
            }

        return new String(sb);
    }

    /**
     * Implements a toString for a map that can have array type values
     * 
     * @param map the map
     * @return String.
     */
    public static String toString(Map<?, ?> map) {
        if (map == null)
            return null;

        StringBuilder sb = new StringBuilder('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (first)
                first = false;
            else
                sb.append(", ");
            sb.append(entry.getKey()).append('=');
            Object value = entry.getValue();
            if (value instanceof Object[])
                sb.append(Arrays.toString((Object[]) value));
            else if (value != null && value.getClass().isArray()) {
                // primitive array
                int length = Array.getLength(value);
                sb.append('[');
                boolean f = true;
                for (int i = 0; i < length; i++) {
                    if (f)
                        f = false;
                    else
                        sb.append(", ");
                    sb.append(Array.get(value, i));
                }
                sb.append(']');
            } else
                sb.append(value);
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Implements the standard toString method for objects on which toString has been
     * overridden.
     * 
     * @param obj the Object to convert to a String.
     * 
     * @return String equivalent to what would be returned by Object.toString(),
     *         or null, if the object is null.
     */
    public static String toString(Object obj) {
        if (obj == null)
            return null;

        if (Proxy.isProxyClass(obj.getClass()) 
            && Proxy.getInvocationHandler(obj) instanceof WSJdbcTracer) 
            return obj.toString(); 

        return new StringBuffer(obj.getClass().getName())
                        .append('@')
                        .append(Integer.toHexString(System.identityHashCode(obj))) 
                        .toString();
    }

    /**
     * @param xid the XID.
     * 
     * @return a nicely formatted String representing the XID.
     * 
     */
    public static String toString(Xid xid) {
        StringBuilder sb = new StringBuilder();

        if (xid != null)
            sb.append(toString((Object) xid))
                            .append(EOLN)
                            .append(INDENT)
                            .append(xid)
                            .append(EOLN)
                            .append(INDENT)
                            .append("Global Transaction ID: ")
                            .append(toString(xid.getGlobalTransactionId()))
                            .append(EOLN)
                            .append(INDENT)
                            .append("Branch Qualifier:      ")
                            .append(toString(xid.getBranchQualifier()))
                            .append(EOLN)
                            .append(INDENT)
                            .append("Format ID:             ")
                            .append(xid.getFormatId());

        return new String(sb);
    }

    /**
     * This method returns a StringBuffer which includes SQL state, error code and stack trace of the
     * passed-in SQLException and all its linked exception.
     * 
     * @return StringBuffer a StringBuffer which includes SQL state, error code and stack trace
     */
    public static StringBuilder getStackTraceWithState(SQLException sqle) {
        SQLException sqlX = sqle;
        boolean isLinkedThrowable = false;

        StringBuilder trace = new StringBuilder(EOLN); 

        SQLException tempSqlX;
        do {
            StringWriter s = new StringWriter();
            PrintWriter p = new PrintWriter(s);
            sqlX.printStackTrace(p);

            // If it is a linked exception, append link information.
            if (isLinkedThrowable)
                trace.append(
                             "---- Begin backtrace for Nested Throwables").append(
                                                                                  EOLN);

            // Append SQLState, error code and stack trace.
            trace.append("SQL STATE:  " + sqlX.getSQLState()).append(
                                                                     EOLN);
            trace.append("ERROR CODE: " + sqlX.getErrorCode()).append(
                                                                      EOLN);
            trace.append(s.toString());

            // set isLinkedThrowable to true
            isLinkedThrowable = true;
            tempSqlX = sqlX.getNextException();
        } while ((tempSqlX != sqlX) && ((sqlX = tempSqlX) != null)); 

        return trace;

    }

    /**
     * Display the java.sql.Statement ResultSet close constant corresponding to the value
     * supplied.
     * 
     * @param value a valid ResultSet close value
     * 
     * @return the name of the constant, or a string indicating the constant is unknown.
     */
    public static String getResultSetCloseString(int value) {
        switch (value) {
            case Statement.CLOSE_ALL_RESULTS:
                return "CLOSE ALL RESULTS (" + value + ')'; 

            case Statement.CLOSE_CURRENT_RESULT:
                return "CLOSE CURRENT RESULT (" + value + ')'; 

            case Statement.KEEP_CURRENT_RESULT:
                return "KEEP CURRENT RESULT (" + value + ')'; 
        }

        return "UNKNOWN CLOSE RESULTSET CONSTANT (" + value + ')'; 
    }

    /**
     * Display the java.sql.ResultSet cursor holdability constant corresponding to the value
     * supplied.
     * 
     * @param value a valid cursor holdability value
     * 
     * @return the name of the constant, or a string indicating the constant is unknown.
     */
    public static String getCursorHoldabilityString(int value) {
        switch (value) {
            case ResultSet.CLOSE_CURSORS_AT_COMMIT:
                return "CLOSE CURSORS AT COMMIT (" + value + ')'; 

            case ResultSet.HOLD_CURSORS_OVER_COMMIT:
                return "HOLD CURSORS OVER COMMIT (" + value + ')'; 

            case 0:
                return "DEFAULT CURSOR HOLDABILITY VALUE (" + value + ')'; 
        }

        return "UNKNOWN CURSOR HOLDABILITY CONSTANT (" + value + ')'; 
    }

    //  - provide auto generated key support
    /**
     * Display the auto genereated key constant corresponding to the value
     * supplied.
     * 
     * @param value a auto genereated key value
     * 
     * @return the name of the constant, or a string indicating the constant is unknown.
     */
    public static String getAutoGeneratedKeyString(int autoGeneratedKey) {
        switch (autoGeneratedKey) {
            case Statement.NO_GENERATED_KEYS:
                return "NO GENERATED KEYS (" + autoGeneratedKey + ')'; 

            case Statement.RETURN_GENERATED_KEYS:
                return "RETURN GENERATED KEYS (" + autoGeneratedKey + ')'; 
        }
        return "UNKNOWN AUTO GENERATED KEYS CONSTANT (" + autoGeneratedKey + ')'; 
    }

    /**
     * The AdapterUtil.mapException method handles all exception mapping scenarios for the
     * OpenLiberty JDBC integration layer.
     * 
     * @param x - the exception to map. The exception must either be a SQLException or
     *            a DataStoreAdapterException with a chained SQLException. If the exception is
     *            any other type, then it will not be mapped. This parameter is required.
     * 
     * @param handle - the connection handle
     *            on which the connection error occurred. The connection handle is used when
     *            submitting connection error events to the managed connection, and to perform
     *            connection handle-specific logic for stale statements. If a connection handle
     *            is not supplied then these are done at a more basic level using the managed
     *            connection, or skipped altogether if there is also no managed connection.
     *            This parameter is optional.
     * 
     * @param mapper - the managed connection or managed connection factory containing the
     *            data store helper and error detection model which are used to map the
     *            exception. The managed connection is used to send connection error events
     *            and perform stale statement logic if no connection handle is provided.
     *            This parameter is required.
     * 
     * @param sendEvent - controls whether a connection error event is fired to registered
     *            connection event listeners. If no connection handle or managed connection are
     *            supplied then this value is ignored.
     * 
     * @return the mapped exception, according to the DatabaseHelper or DataStoreHelper,
     *         the identifyException configuration, and the replaceExceptions heritage setting.
     *         If no mapping is found, or if not enabled, or if insufficient
     *         parameters are provided, then the original exception is returned.
     *         The exception returned is of the same type as the exception parameter
     *         supplied. In the case of DataStoreAdapterException, this method maps the
     *         linked exception and returns the original DataStoreAdapterException.
     * 
     */
    public static Exception mapException(Exception x, WSJdbcConnection handle, Object mapper, boolean sendEvent) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "mapException", x.getClass().getName(), toString(handle), toString(mapper), sendEvent ? "EVENT" : "NO EVENT");

        WSRdbManagedConnectionImpl mc = mapper instanceof WSRdbManagedConnectionImpl ?
                        (WSRdbManagedConnectionImpl) mapper : null;

        WSManagedConnectionFactoryImpl mcf = mapper instanceof WSManagedConnectionFactoryImpl || mc == null ?
                        (WSManagedConnectionFactoryImpl) mapper : mc.getManagedConnectionFactory();

        DatabaseHelper iHelper = mcf == null ? null : mcf.getHelper();

        DataStoreAdapterException dsae = x instanceof DataStoreAdapterException ?
                        (DataStoreAdapterException) x : null;

        SecurityException securityX = null;

        SQLException sqlX =
                        x instanceof SQLException ? (SQLException) x :
                                        dsae != null && dsae.getCause() instanceof SQLException ? (SQLException) dsae.getCause() :
                                                        null;

        boolean isStaleStatement = false;
        boolean mapsToStaleConnection = false;
        SQLException mappedX;
        boolean alreadyMapped = sqlX != null && isLegacyException(sqlX, "com.ibm.websphere.ce.cm.PortableSQLException");

        if (mcf == null                          // no access to helper or replaceExceptions config
         || sqlX == null                         // nothing to map
         || alreadyMapped                        // already mapped
         || dsae != null && dsae.beenMapped())   // already mapped
        {
            mappedX = sqlX;
        }
        else
        {
            if (iHelper.dataStoreHelper == null) {
                mappedX = sqlX;
                mapsToStaleConnection = iHelper.isConnectionError(sqlX);
            } else {
                mappedX = iHelper.mapException(sqlX);
                mapsToStaleConnection = isLegacyException(mappedX, IdentifyExceptionAs.StaleConnection.legacyClassName);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, mappedX == sqlX ? "not replaced" : ("mapped to " + mappedX.getClass().getName()));
                // Legacy code does not replace BatchUpdateException
                if (sqlX instanceof BatchUpdateException)
                    mappedX = sqlX;
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "stale? " + mapsToStaleConnection + "; original exception is:", getStackTraceWithState(sqlX));

            // Check for stale statement

            if (iHelper.dataStoreHelper == null)
                isStaleStatement = iHelper.isStaleStatement(sqlX);
            else if (isLegacyException(mappedX, IdentifyExceptionAs.StaleStatement.legacyClassName))
                try {
                    isStaleStatement = true;
                    SQLException m = mappedX;
                    mappedX = AccessController.doPrivileged((PrivilegedExceptionAction<SQLException>) () -> {
                        @SuppressWarnings("unchecked")
                        Class<? extends SQLException> StaleConnectionException = (Class<? extends SQLException>)
                            m.getClass().getClassLoader().loadClass(IdentifyExceptionAs.StaleConnection.legacyClassName);
                        return StaleConnectionException.getConstructor(SQLException.class).newInstance(m.getNextException());
                    });
                } catch (PrivilegedActionException privX) {
                    FFDCFilter.processException(privX, AdapterUtil.class.getName(), "1210");
                }

            if (isStaleStatement) {
                if (handle == null) {
                    if (mc != null)
                        mc.clearStatementCache();
                } else
                    WSJdbcUtil.handleStaleStatement(handle);
            }

            // Check if the replaceExceptions heritage setting permits the mapped exception to be returned
            // to the application, or only used internally by the application server.

            if (mcf.dsConfig.get().heritageReplaceExceptions)
            {
                if (dsae != null && mappedX != sqlX)
                    dsae.setLinkedException(mappedX);
            }
            else // not enabled, use the original exception
                mappedX = sqlX;

        }

        // For DB2, SQLTransientConnectionException with error code -4498 is actually
        // a recoverable exception. Switch it to SQLRecoverableException. 
        if (mappedX instanceof SQLTransientConnectionException
            && iHelper != null && iHelper.failoverOccurred(sqlX)) {
            // Before:  transientX n--> nextX
            // After:   recoverX   n--> nextX
            //                     c--> transientX n--> nextX

            SQLException nextX = mappedX.getNextException();
            mappedX = new SQLRecoverableException(
                            mappedX.getMessage(), mappedX.getSQLState(), mappedX.getErrorCode(), mappedX);
            mappedX.setNextException(nextX);
        }

        // Check for authentication errors with DB2 trusted context.
        boolean isAuthenticationError = mcf != null && iHelper.isAnAuthorizationException(mappedX);

        // Look for fatal connection errors.
        // SQLRecoverableException and SQLNonTransientConnectionException indicate
        // bad connections.  SQLTransientConnectionException indicates a temporary
        // error that might disappear if the same operation is retried, even without
        // starting a new transaction.

        if ((sqlX instanceof SQLRecoverableException
             || sqlX instanceof SQLNonTransientConnectionException
             || isAuthenticationError
             || mapsToStaleConnection) 
            && !alreadyMapped
            && !isStaleStatement) // it's a statement error, not a connection error
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, isAuthenticationError ?
                                "Detected an authentication error" :
                                "Detected a connection error.");

            if (dsae != null && isAuthenticationError)
                securityX = new SecurityException(dsae.getMessage(), mappedX);

            if (sendEvent && (dsae == null || !dsae.beenMapped())) {
                if (handle != null)
                    handle.fireConnectionErrorEvent(sqlX, true);

                else if (mc != null)
                    mc.processConnectionErrorOccurredEvent(null, sqlX);
            }
        }

        // Regardless of whether we actually found an exception to map to, mark the
        // DataStoreAdapterException as having already been through the mapping process.

        if (dsae != null)
            dsae.setBeenMapped();

        Exception result =
                        securityX != null ? securityX :
                                        dsae != null ? dsae :
                                                        mappedX != null ? mappedX :
                                                                        x;
        if (tc.isEntryEnabled())
            Tr.exit(tc, "mapException", (mappedX == null ? x : mappedX).getClass().getName());

        return result;
    }

    /**
     * Translates a SQLException from the database. Exception mapping code is
     * now consolidated into AdapterUtil.mapException.
     * 
     * @param SQLException se - the SQLException from the database
     * 
     * @param mapper the managed connection or managed connection factory capable of
     *            mapping the exception. If this value is NULL, then exception mapping is
     *            not performed. If stale statement handling or connection error handling
     *            is required then the managed connection must be supplied.
     * 
     * @return SQLException - the mapped SQLException
     */
    public static SQLException mapSQLException(SQLException se, Object mapper) {
        return (SQLException) mapException(se, null, mapper, true);
    }


    //   method used to get the logging level based on a

    /**
     * This method returns the Level based on a string representation fo the level
     * possbile values are:<br>
     * ALL <br>
     * SEVERE (highest value)<br>
     * WARNING<br>
     * INFO<br>
     * CONFIG<br>
     * FINE<br>
     * FINER<br>
     * FINEST (lowest value)<br>
     */
    public static Level getLevelBasedOnName(String level) {
        Level _level = Level.INFO; // default
        if (level == null)
            return _level; // default is all

        char firstLetter = level.charAt(0);
        switch (firstLetter) {
            case 'i':
            case 'I':
                _level = Level.INFO;
                break;
            case 'A':
            case 'a':
                _level = Level.ALL;
                break;
            case 's':
            case 'S':
                _level = Level.SEVERE;
                break;
            case 'w':
            case 'W':
                _level = Level.WARNING;
                break;
            case 'c':
            case 'C':
                _level = Level.CONFIG;
                break;
            case 'f':
            case 'F':
                // now here i will match the whole word
                if (level.equalsIgnoreCase("fine"))
                    _level = Level.FINE;
                if (level.equalsIgnoreCase("finer"))
                    _level = Level.FINER;
                if (level.equalsIgnoreCase("finest"))
                    _level = Level.FINEST;
                break;
            default:
                break;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "The level returned is: ", _level);

        return _level;
    }

    public static boolean matchGSSName(GSSName gn1, GSSName gn2) {
        try {
            if (gn1 == gn2 ||
                (gn1 != null && gn1.equals(gn2)))
                return true;

            return false;
        } catch (GSSException e) {
            //NOFFDC needed
            // display the exception which should display all that is needed.
            if (tc.isEventEnabled())
                Tr.event(tc, "Comparing GSSNAmes received an exception (will return false, to indicate no match): ", e);

            // assume no match so will just return false 
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "adapterUtil matchGSSName return with exception ", false);
            return false;
        }
    }
    
    public static ClassLoader getClassLoaderWithPriv(final Library lib) {
        if(System.getSecurityManager() == null)
            return lib.getClassLoader();
        else 
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return lib.getClassLoader();
                }
            });
    }
    
    public static Class<?> forNameWithPriv(final String className, final boolean initialize, final ClassLoader loader) throws ClassNotFoundException {
        if(System.getSecurityManager() == null)
            return Class.forName(className, initialize, loader);
        else
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                    @Override
                    public Class<?> run() throws ClassNotFoundException {
                        return Class.forName(className, initialize, loader);
                    }
                });
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof ClassNotFoundException)
                    throw (ClassNotFoundException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
    }
}
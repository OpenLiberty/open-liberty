/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.work.WorkEvent;
import javax.transaction.Status;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import com.ibm.adapter.message.FVTMessageProvider;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * This is a utility class containing static methods for use throughout the
 * Adapter code.
 */
public class AdapterUtil {
    public static final String NLS_FILE = "IBMDataStoreAdapterNLS";
    public static final String TRACE_GROUP = "RRA";

    private static final TraceComponent tc = Tr.register(AdapterUtil.class);

    /** NLS object for retrieving error messages */

    /** The WebSphere SQL State. */
    public static final String WS_SQL_STATE = "WS000";

    /** new attributes for d126740 */

    public final static int APPLICATION_J2EE_VERSION_UNKNOWN = 0;

    public final static int APPLICATION_J2EE_VERSION_1_2 = 12;
    public final static int APPLICATION_J2EE_VERSION_1_3 = 13;

    public final static int EJB_MODULE_J2EE_VERSION_UNKNOWN = 0;
    public final static int EJB_MODULE_J2EE_VERSION_1_1 = 11;
    public final static int EJB_MODULE_J2EE_VERSION_2_0 = 20;

    public final static int EJB_CMP_VERSION_UNKNOWN = 0;
    public final static int EJB_CMP_VERSION_1_1 = 11;
    public final static int EJB_CMP_VERSION_2_0 = 20;

    // Added begin by gburli - 06/08/2004
    public static final int DEFAULT_EMPTY_MC_SET = 100;
    public static final int ALL_MC_INVALID = 110;
    public static final int LAST_MC_INVALID = 120;
    public static final int ODD_MC_INVALID = 130;

    // 06/13/04: swai begin
    public static final int ENTIRE_POOL_PURGEPOLICY = 135;
    // 06/13/04: swai end

    public static final int EVEN_MC_INVALID = 140;
    public static final int EXCEPTION_MC_INVALID = 150;
    public static final int NULL_MC_INVALID = 160;
    public static final int NULL_WITH_MC_INVALID = 170;
    public static String message = "";

    // 06/08/2004: gburli
    // Need to new the HashSet.
    public static Set failedMCSet = new HashSet();
    public static Set MCSet = new HashSet();

    // Set default invalidConnFlag to default value
    private static int invalidConnFlag = DEFAULT_EMPTY_MC_SET;
    // Added end by gburli - 06/08/2004

    // added by gburli - 01/03/2005
    public static final int THROTTLING_MESSAGEINFLOW_SUPPORT = 180;
    public static final int NO_THROTTLING_SUPPORT = 190;
    // Set throttlingSupportFlag to test the condition
    private static int throttlingSupportFlag = THROTTLING_MESSAGEINFLOW_SUPPORT;
    private static String messageAck = null;
    // added by gburli - 01/03/2005

    private final static String prepend = "eis/";
    private final static String postpend = "_CMP";
    /* d126740 end */

    protected static java.util.HashMap errorMap = new HashMap(13);
    // holds the predefined error codes

    private static boolean sharingViolation = false;
    private static boolean killServer = false;
    private static String serverPid = null;

    // @alvinso.3
    /*
     * 06/02/04: swai static String xaRecoveryAliasOutbound = null;
     *
     * static boolean getXARecoveryTokenOutbound = false;
     *
     * static String xaRecoveryAliasInbound = null;
     *
     * static boolean getXARecoveryTokenInbound = false;
     */

    /**
     * Retrieves the exception chained to the specified Throwable.
     *
     * @param th
     *               the Exception or Error to retrieve the chained exception of.
     *
     * @return the chained exception, or null if there is no chained exception.
     */
    public static Throwable getChainedException(Throwable th) {
        java.lang.reflect.Method[] methods = th.getClass().getMethods();

        for (int i = 0; i < methods.length; i++)
            if (Throwable.class.isAssignableFrom(methods[i].getReturnType())
                && !methods[i].getName().equals("fillInStackTrace")
                && methods[i].getParameterTypes().length == 0)
                try {
                    java.lang.Object[] obj = null;
                    return (Throwable) methods[i].invoke(th, obj);
                } catch (Throwable ex) {
                    // No FFDC code needed; ignore.
                }

        return null;
    }

    /**
     * Display the name of the ConnectionEvent constant given the event id.
     *
     * @param eventID
     *                    a valid ConnectionEvent constant value.
     *
     * @return The name of the ConnectionEvent constant, or a string indicating
     *         the constant is unknown.
     */
    public static String getConnectionEventString(int eventID) {
        switch (eventID) {
            case 1:
                return "CONNECTION CLOSED";

            case 2:
                return "LOCAL TRANSACTION STARTED";

            case 3:
                return "LOCAL TRANSACTION COMMITTED";

            case 4:
                return "LOCAL TRANSACTION ROLLEDBACK";

            case 5:
                return "CONNECTION ERROR OCCURRED";

        }

        return "UNKNOWN CONNECTION EVENT: " + eventID;
    }

    /**
     * Display the java.sql.ResultSet concurrency mode constant corresponding to
     * the value supplied.
     *
     * @param level
     *                  a valid java.sql.ResultSet concurrency mode constant.
     *
     * @return the name of the constant, or a string indicating the constant is
     *         unknown.
     */
    public static String getConcurrencyModeString(int concurrency) {
        switch (concurrency) {
            case ResultSet.CONCUR_READ_ONLY:
                return "CONCUR READ ONLY";

            case ResultSet.CONCUR_UPDATABLE:
                return "CONCUR UPDATABLE";
        }

        return "UNKNOWN RESULT SET CONCURRENCY: " + concurrency;
    }

    /**
     * @return the current javax.transaction.Status constant corresponding to
     *         the Transaction Manager status. If we get an error checking the
     *         status, we return STATUS_UNKNOWN.
     */
    public static final int getGlobalTranStatus() {
        return Status.STATUS_UNKNOWN;
    }

    /**
     * Display the javax.transaction.Status constant corresponding to the
     * current transaction status from the Transaction Manager.
     *
     * @return the name of the constant.
     */
    public static final String getGlobalTranStatusAsString() {
        return getGlobalTranStatusString(getGlobalTranStatus());
    }

    /**
     * Display the javax.transaction.Status constant corresponding to the value
     * supplied.
     *
     * @param status
     *                   a valid javax.transaction.Status constant.
     *
     * @return the name of the constant, or a string indicating the constant is
     *         unknown.
     */
    public static String getGlobalTranStatusString(int status) {
        switch (status) {
            case javax.transaction.Status.STATUS_ACTIVE:
                return "STATUS ACTIVE";

            case javax.transaction.Status.STATUS_COMMITTED:
                return "STATUS COMMITTED";

            case javax.transaction.Status.STATUS_COMMITTING:
                return "STATUS COMMITTING";

            case javax.transaction.Status.STATUS_MARKED_ROLLBACK:
                return "STATUS MARKED ROLLBACK";

            case javax.transaction.Status.STATUS_NO_TRANSACTION:
                return "STATUS NO TRANSACTION";

            case javax.transaction.Status.STATUS_PREPARED:
                return "STATUS PREPARED";

            case javax.transaction.Status.STATUS_PREPARING:
                return "STATUS PREPARING";

            case javax.transaction.Status.STATUS_ROLLEDBACK:
                return "STATUS ROLLEDBACK";

            case javax.transaction.Status.STATUS_ROLLING_BACK:
                return "STATUS ROLLING BACK";

            case javax.transaction.Status.STATUS_UNKNOWN:
                return "STATUS UNKNOWN";
        }

        return "UNKNOWN GLOBAL TRANSACTION STATUS: " + status;
    }

    /**
     * Display the java.sql.Connection isolation level constant corresponding to
     * the value supplied.
     *
     * @param level
     *                  a valid java.sql.Connection transaction isolation level
     *                  constant.
     *
     * @return the name of the constant, or a string indicating the constant is
     *         unknown.
     */
    public static String getIsolationLevelString(int level) {
        switch (level) {
            case Connection.TRANSACTION_NONE:
                return "NONE";

            case Connection.TRANSACTION_READ_UNCOMMITTED:
                return "READ UNCOMMITTED";

            case Connection.TRANSACTION_READ_COMMITTED:
                return "READ COMMITTED";

            case Connection.TRANSACTION_REPEATABLE_READ:
                return "REPEATABLE READ";

            case Connection.TRANSACTION_SERIALIZABLE:
                return "SERIALIZABLE";
        }

        return "UNKNOWN ISOLATION LEVEL: " + level;
    }

    /**
     * Display the java.sql.ResultSet result set type constant corresponding to
     * the value supplied.
     *
     * @param level
     *                  a valid java.sql.ResultSet result set type constant.
     *
     * @return the name of the constant, or a string indicating the constant is
     *         unknown.
     */
    public static String getResultSetTypeString(int type) {
        switch (type) {
            case ResultSet.TYPE_FORWARD_ONLY:
                return "TYPE FORWARD ONLY";

            case ResultSet.TYPE_SCROLL_INSENSITIVE:
                return "TYPE SCROLL INSENSITIVE";

            case ResultSet.TYPE_SCROLL_SENSITIVE:
                return "TYPE SCROLL SENSITIVE";
        }

        return "UNKNOWN RESULT SET TYPE: " + type;
    }

    /**
     * Display the java.sql.Types SQL Type constant corresponding to the value
     * supplied.
     *
     * @param level
     *                  a valid java.sql.Types SQL Type constant.
     *
     * @return the name of the constant, or a string indicating the constant is
     *         unknown.
     */
    public static String getSQLTypeString(int sqlType) {
        switch (sqlType) {
            case Types.ARRAY:
                return "ARRAY";

            case Types.BIGINT:
                return "BIGINT";

            case Types.BINARY:
                return "BINARY";

            case Types.BIT:
                return "BIT";

            case Types.BLOB:
                return "BLOB";

            case Types.CHAR:
                return "CHAR";

            case Types.CLOB:
                return "CLOB";

            case Types.DATE:
                return "DATE";

            case Types.DECIMAL:
                return "DECIMAL";

            case Types.DISTINCT:
                return "DISTINCT";

            case Types.DOUBLE:
                return "DOUBLE";

            case Types.FLOAT:
                return "FLOAT";

            case Types.INTEGER:
                return "INTEGER";

            case Types.JAVA_OBJECT:
                return "JAVA OBJECT";

            case Types.LONGVARBINARY:
                return "LONGVARBINARY";

            case Types.LONGVARCHAR:
                return "LONGVARCHAR";

            case Types.NULL:
                return "NULL";

            case Types.NUMERIC:
                return "NUMERIC";

            case Types.OTHER:
                return "OTHER";

            case Types.REAL:
                return "REAL";

            case Types.REF:
                return "REF";

            case Types.SMALLINT:
                return "SMALLINT";

            case Types.STRUCT:
                return "STRUCT";

            case Types.TIME:
                return "TIME";

            case Types.TIMESTAMP:
                return "TIMESTAMP";

            case Types.TINYINT:
                return "TINYINT";

            case Types.VARBINARY:
                return "VARBINARY";

            case Types.VARCHAR:
                return "VARCHAR";
        }

        return "UNKNOWN SQL TYPE: " + sqlType;
    }

    // This method was copied from Deb's SPI code.
    /**
     * Used by trace facilities to print out XAException code as a string
     *
     * @param code
     *                 a valid javax.transaction.xa.XAException error code constant.
     *
     * @return the name of the constant, or a string indicating the constant is
     *         unknown.
     */
    public static String getXAExceptionCodeString(int code) {

        // Note, two cases are commented out below. This is because in the
        // implementation of
        // XAException, they have defined two constants to be the same int (or
        // so it would
        // seem).

        // This is because XA_RBBASE and XA_RBEND define the lower and upper
        // bounds of the
        // XA_RB error codes. These two are correctly commented out below.

        switch (code) {
            case XAException.XA_RBTRANSIENT:
                return "XA_RBTRANSIENT";
            case XAException.XA_RBROLLBACK:
                return "XA_RBROLLBACK";
            case XAException.XA_HEURCOM:
                return "XA_HEURCOM";
            case XAException.XA_HEURHAZ:
                return "XA_HEURHAZ";
            case XAException.XA_HEURMIX:
                return "XA_HEURMIX";
            case XAException.XA_HEURRB:
                return "XA_HEURRB";
            case XAException.XA_NOMIGRATE:
                return "XA_NOMIGRATE";
            // case XAException.XA_RBBASE:
            // return "XA_RBBASE";
            case XAException.XA_RBCOMMFAIL:
                return "XA_RBCOMMFAIL";
            case XAException.XA_RBDEADLOCK:
                return "XA_RBDEADLOCK";
            // case XAException.XA_RBEND:
            // return "XA_RBEND";
            case XAException.XA_RBINTEGRITY:
                return "XA_RBINTEGRITY";
            case XAException.XA_RBOTHER:
                return "XA_RBOTHER";
            case XAException.XA_RBPROTO:
                return "XA_RBPROTO";
            case XAException.XA_RBTIMEOUT:
                return "XA_RBTIMEOUT";
            case XAException.XA_RDONLY:
                return "XA_RDONLY";
            case XAException.XA_RETRY:
                return "XA_RETRY";
            case XAException.XAER_ASYNC:
                return "XAER_ASYNC";
            case XAException.XAER_DUPID:
                return "XAER_DUPID";
            case XAException.XAER_INVAL:
                return "XAER_INVAL";
            case XAException.XAER_NOTA:
                return "XAER_NOTA";
            case XAException.XAER_OUTSIDE:
                return "XAER_OUTSIDE";
            case XAException.XAER_PROTO:
                return "XAER_PROTO";
            case XAException.XAER_RMERR:
                return "XAER_RMERR";
            case XAException.XAER_RMFAIL:
                return "XAER_RMFAIL";

        }
        return "UNKNOWN XA EXCEPTION CODE: " + code;
    }

    /**
     * Display the javax.xa.XAResource end flag constant corresponding to the
     * value supplied.
     *
     * @param level
     *                  a valid javax.xa.XAResource end flag constant.
     *
     * @return the name of the constant, or a string indicating the constant is
     *         unknown.
     */
    public static String getXAResourceEndFlagString(int flag) {
        switch (flag) {
            case XAResource.TMFAIL:
                return "TMFAIL";

            case XAResource.TMSUCCESS:
                return "TMSUCCESS";

            case XAResource.TMSUSPEND:
                return "TMSUSPEND";
        }

        return "UNKNOWN XA RESOURCE END FLAG: " + flag;
    }

    /**
     * Display the javax.xa.XAResource recover flag constant corresponding to
     * the value supplied.
     *
     * @param level
     *                  a valid javax.xa.XAResource recover flag constant.
     *
     * @return the name of the constant, or a string indicating the constant is
     *         unknown.
     */
    public static String getXAResourceRecoverFlagString(int flag) {
        switch (flag) {
            case XAResource.TMENDRSCAN:
                return "TMENDRSCAN";

            case XAResource.TMNOFLAGS:
                return "TMNOFLAGS";

            case XAResource.TMSTARTRSCAN:
                return "TMSTARTRSCAN";
        }

        return "UNKNOWN XA RESOURCE RECOVER FLAG: " + flag;
    }

    /**
     * Display the javax.xa.XAResource start flag constant corresponding to the
     * value supplied.
     *
     * @param level
     *                  a valid javax.xa.XAResource start flag constant.
     *
     * @return the name of the constant, or a string indicating the constant is
     *         unknown.
     */
    public static String getXAResourceStartFlagString(int flag) {
        switch (flag) {
            case XAResource.TMJOIN:
                return "TMJOIN";

            case XAResource.TMNOFLAGS:
                return "TMNOFLAGS";

            case XAResource.TMRESUME:
                return "TMRESUME";
        }

        return "UNKNOWN XA RESOURCE START FLAG: " + flag;
    }

    /**
     * Display the javax.xa.XAResource Resource Manager vote constant
     * corresponding to the value supplied.
     *
     * @param level
     *                  a valid javax.xa.XAResource vote constant.
     *
     * @return the name of the constant, or a string indicating the constant is
     *         unknown.
     */
    public static String getXAResourceVoteString(int vote) {
        switch (vote) {
            case XAResource.XA_OK:
                return "XA_OK";

            case XAResource.XA_RDONLY:
                return "XA_RDONLY";
        }

        return "UNKNOWN XA RESOURCE VOTE: " + vote;
    }

    /**
     * @return a new Properties object with the password value hidden.
     */
    public static Properties hidePassword(Properties props) {
        Properties p = props;

        if (p != null) {
            p = (Properties) p.clone();

            if (p.containsKey("pqassword"))
                p.setProperty("password", "******");
        }

        return p;
    }

    /**
     * @return true if a global transaction is active, otherwise false.
     */
    public static final boolean inGlobalTransaction() {
        return false;
    }

    /**
     * Determine if two objects, either of which may be null, are equal.
     *
     * @param obj1
     *                 one object.
     * @param obj2
     *                 another object.
     *
     * @return true if the objects are equal or are both null, otherwise false.
     */
    public static final boolean match(Object obj1, Object obj2) {
        return obj1 == obj2 || (obj1 != null && obj1.equals(obj2));
    }

    /**
     * Formats a SQLWarning as a String, including the stack trace and linked
     * warnings.
     *
     * @param sqlW
     *                 a SQLWarning.
     *
     * @return a properly formatted string.
     */
    public static String sqlWarningToString(SQLWarning sqlW) {
        if (sqlW == null)
            return null;

        java.io.StringWriter writer = new java.io.StringWriter();
        sqlW.printStackTrace(new java.io.PrintWriter(writer));

        StringBuffer buffer = new StringBuffer(writer.toString());

        SQLWarning nextW = sqlW.getNextWarning();

        if (nextW != null && nextW != sqlW)
            buffer.append("-------- linked warning -------\n")
                            .append(
                                    sqlWarningToString(nextW));

        return buffer.toString();
    }

    /**
     * Formats an exception's stack trace as a String.
     *
     * @param th
     *               a throwable object (Exception or Error)
     *
     * @return String containing the exception's stack trace.
     */
    public static String stackTraceToString(Throwable th) {
        java.io.StringWriter writer = new java.io.StringWriter();
        th.printStackTrace(new java.io.PrintWriter(writer));

        // Determine if there are any chained exceptions.

        StringBuffer buffer = new StringBuffer(writer.toString());

        Throwable chainedX = getChainedException(th);

        if (chainedX != null && chainedX != th)
            buffer.append("-------- chained exception -------\n")
                            .append(
                                    stackTraceToString(chainedX));

        return buffer.toString();
    }

    /**
     * Formats a Throwable into a String containing the error message and error
     * messages for all chained exceptions.
     *
     * @param th
     *               a throwable object (Exception or Error)
     *
     * @return String containing the exception message.
     */
    public static String toErrorMessage(Throwable th) {
        String message = th.getMessage();
        StringBuffer buffer = new StringBuffer(message == null ? "" : message);

        // Determine if there are any chained exceptions.

        Throwable chainedX = getChainedException(th);

        if (chainedX != null && chainedX != th)
            buffer.append(" Chained ")
                            .append(chainedX.getClass().getName())
                            .append(": ")
                            .append(toErrorMessage(chainedX));

        return buffer.toString();
    }

    /**
     * Converts a String to a Properties object using the load method on
     * java.util.Properties. A semicolon (;) may be used in place of the
     * end-of-line character to delimit property entries.
     *
     * @param properString
     *                         the String to convert to a Properties object.
     *
     * @return the resulting Properties object.
     *
     * @throws IOException
     *                         if an error occurs reading the String.
     */
    public static Properties toProperties(String properString) throws java.io.IOException {
        Properties p = new Properties();
        p.load(new java.io.ByteArrayInputStream(properString.replace(';', '\n')
                        .getBytes()));

        return p;
    }

    /**
     * Converts a ResourceException to a SQLException. Returns the next linked
     * exception if it is a SQLException. Otherwise, creates a new SQLException
     * containing information from the ResourceException.
     *
     * @param resX
     *                 a javax.resource.ResourceException
     *
     * @return a SQLException
     */
    public static SQLException toSQLException(ResourceException resX) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "toSQLException", resX);

        SQLException sqlX;
        Exception linkedX = resX.getLinkedException();

        // if the linked exception is a SQL Exception, just use that one

        if (linkedX instanceof SQLException) {
            sqlX = (SQLException) linkedX;
        } else // use the original exception to create a SQL Exception
        {
            if (tc.isEventEnabled())
                Tr.event(tc, "Converting ResourceException to a SQLException");

            sqlX = new SQLException("Resource exception error code:"
                                    + resX.getErrorCode() + " Stack trace:"
                                    + stackTraceToString(resX));
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "toSQLException",
                    new Object[] { "SQL STATE:  " + sqlX.getSQLState(),
                                   "ERROR CODE: " + sqlX.getErrorCode(), sqlX });

        return sqlX;
    }

    /**
     * Converts any generic Exception to a SQLException.
     *
     * @param ex
     *               the exception to convert.
     *
     * @return SQLException containing the original Exception class, message,
     *         and stack trace.
     */
    public static SQLException toSQLException(Throwable ex) {
        if (ex instanceof SQLException)
            return (SQLException) ex;

        return new SQLException("Covert the exception to SQLException: "
                                + "Exception: " + ex.getClass().getName()
                                + ", exception message: " + ex.getMessage() + "Stack trace: "
                                + stackTraceToString(ex));
    }

    /**
     * Implements the standard toString method for objects on which toString has
     * been overridden.
     *
     * @param obj
     *                the Object to convert to a String.
     *
     * @return String equivalent to what would be returned by Object.toString(),
     *         or null, if the object is null.
     */
    public static String toString(Object obj) {
        if (obj == null)
            return null;

        return new StringBuffer(obj.getClass().getName()).append('@')
                        .append(Integer.toHexString(obj.hashCode()))
                        .toString();
    }

    private static SQLException mapExceptionHelper(SQLException e) {
        return e;
    }

    /**
     * This method maps the input SQLException to a matching WebSphere
     * sqlException if any.<br>
     * If there is no matched exception, the original exception is returned.<br>
     * If no mapping is desired, user could override this method and just return
     * any original exception as is(without mapping)
     *
     * @param e
     *              SQLException
     * @return SQLException mapped one For example, StaleConnectionException.
     */
    public final static SQLException mapException(SQLException e) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "mapException(SQLException)", e);

        return (mapExceptionHelper(e));
    }

    /**
     * This method returns mapping for a given exception if one is found. First
     * the user defined map is searched followed by the predefined DB2 map. If
     * nothing is found, the search continues in the parent class -
     * GenericHelper. The search is done in the following order:
     * <OL>
     * <LI>first the errorCode is searched
     * <LI>then the SQLState.
     * </OL>
     * <p>
     * If a user wants to override the mapping of a specific Exception, they can
     * override this method and either return the same exception class or return
     * null. In the mapexception() which calls this method if the map found is
     * null then the original exception is returned.
     *
     * A user can also cancel the mapping by returning null for that exception
     * map.
     *
     * @param e
     *              The SQLException we need to find a mapping class for
     * @return java.lang.Class - The mapped class
     */
    public static final Class findMappingClass(SQLException e) {
        Object eClass = null;
        String sqlState = null;

        if (tc.isEntryEnabled())
            Tr.entry(tc, "findMappingClass for: " + e);

        if (e != null) {

            // If we didn't get anything, try the predefined map
            eClass = errorMap.get(new Integer(e.getErrorCode())); // f112570

            if (eClass != null) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "findMappingClass: return ", eClass);
                return (Class) eClass;
            }

            if ((sqlState = e.getSQLState()) != null)
                eClass = errorMap.get(sqlState); // f112570
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "findMappingClass: return ", eClass);
        return (Class) eClass;
    }

    /**
     * This method returns a StringBuffer which includes SQL state, error code
     * and stack trace of the passed-in SQLException and all its linked
     * exception.
     *
     * @return StringBuffer a StringBuffer which includes SQL state, error code
     *         and stack trace
     */
    public static StringBuffer getStackTraceWithState(SQLException sqle) {
        SQLException sqlX = sqle;
        boolean isLinkedThrowable = false;

        String lineSeparator = System.getProperty("line.separator");
        StringBuffer trace = new StringBuffer(lineSeparator);

        do {
            StringWriter s = new StringWriter();
            PrintWriter p = new PrintWriter(s);
            sqlX.printStackTrace(p);

            // If it is a linked exception, append link information.
            if (isLinkedThrowable)
                trace.append("---- Begin backtrace for Nested Throwables")
                                .append(lineSeparator);

            // Append SQLState, error code and stack trace.
            trace.append("SQL STATE:  " + sqlX.getSQLState())
                            .append(
                                    lineSeparator);
            trace.append("ERROR CODE: " + sqlX.getErrorCode())
                            .append(
                                    lineSeparator);
            trace.append(s.toString());

            // set isLinkedThrowable to true
            isLinkedThrowable = true;
        } while ((sqlX = sqlX.getNextException()) != null);

        return trace;

    }

    /**
     * Display the java.sql.Statement close resultset constant corresponding to
     * the value supplied.
     *
     * @param value
     *                  a valid close resultset value
     *
     * @return the name of the constant, or a string indicating the constant is
     *         unknown.
     */
    public static String getResultSetCloseString(int value) {
        switch (value) {
            case Statement.CLOSE_ALL_RESULTS:
                return "CLOSE ALL RESULTS";

            case Statement.CLOSE_CURRENT_RESULT:
                return "CLOSE CURRENT RESULT";

            case Statement.KEEP_CURRENT_RESULT:
                return "KEEP CURRENT RESULT";
        }

        return "UNKNOWN CLOSE RESULTSET VALUE: " + value;
    }

    /**
     * Display the java.sql.ResultSet cursor holdability constant corresponding
     * to the value supplied.
     *
     * @param value
     *                  a valid cursor holdability value
     *
     * @return the name of the constant, or a string indicating the constant is
     *         unknown.
     */
    public static String getCursorHoldabilityString(int value) {
        switch (value) {
            case ResultSet.CLOSE_CURSORS_AT_COMMIT:
                return "CLOSE CURSORS AT COMMIT";

            case ResultSet.HOLD_CURSORS_OVER_COMMIT:
                return "HOLD CURSORS OVER COMMIT";

            case 0:
                return "DEFAULT CURSOR HOLDABILITY VALUE";
        }

        return "UNKNOWN CURSOR HOLDABILITY VALUE: " + value;
    }

    /**
     * Display the auto genereated key constant corresponding to the value
     * supplied.
     *
     * @param value
     *                  a auto genereated key value
     *
     * @return the name of the constant, or a string indicating the constant is
     *         unknown.
     */
    public static String getAutoGeneratedKeyString(int autoGeneratedKey) {
        switch (autoGeneratedKey) {
            case Statement.NO_GENERATED_KEYS:
                return "NO GENRATED KEYS";

            case Statement.RETURN_GENERATED_KEYS:
                return "RETURN GENERATED KEYS";
        }
        return "UNKNOWN AUTO GENERATED KEY VALUE: " + autoGeneratedKey;
    }

    /**
     * Display the work status string.
     *
     * @param status
     *                   the work status
     *
     * @return the status string indicating the work status
     */
    public static String getWorkStatusString(int status) {
        switch (status) {
            case WorkEvent.WORK_ACCEPTED:
                return "WORK_ACCEPTED (" + WorkEvent.WORK_ACCEPTED + ")";

            case WorkEvent.WORK_COMPLETED:
                return "WORK_COMPLETED (" + WorkEvent.WORK_COMPLETED + ")";

            case WorkEvent.WORK_REJECTED:
                return "WORK_REJECTED (" + WorkEvent.WORK_REJECTED + ")";

            case WorkEvent.WORK_STARTED:
                return "WORK_STARTED (" + WorkEvent.WORK_STARTED + ")";

            default:
                return "Unknow status (" + status + ")";
        }

    }

    /**
     * Display the work type string.
     *
     * @param type
     *                 the work type
     *
     * @return the work type string indicating the work type
     */
    public static String getWorkTypeString(int type) {
        switch (type) {
            case FVTMessageProvider.DO_WORK:
                return "DO_WORK(" + FVTMessageProvider.DO_WORK + ")";

            case FVTMessageProvider.NO_WORK:
                return "NO_WORK(" + FVTMessageProvider.NO_WORK + ")";

            case FVTMessageProvider.SCHEDULE_WORK:
                return "SCHEDULE_WORK(" + FVTMessageProvider.SCHEDULE_WORK + ")";

            case FVTMessageProvider.START_WORK:
                return "START_WORK(" + FVTMessageProvider.START_WORK + ")";

            default:
                return "Unknow work type (" + type + ")";
        }

    }

    // @alvinso.1
    /**
     * Get the boolean value of sharingViolation. getConnection will throw
     * SharingViolationException if it is set to true.
     *
     * @return boolean to signal getConnection to throw
     *         SharingViolationException or not
     */
    public static boolean getSharingViolation() {

        return sharingViolation;
    }

    /**
     * Set the boolean value of sharingViolation. getConnection will throw
     * SharingViolationException if it is set to true.
     *
     * @param violation
     *                      set to true to signal getConnection to throw
     *                      SharingViolationException
     *
     */
    public static void setSharingViolation(boolean violation) {

        sharingViolation = violation;
    }

    // @alvinso.2
    /**
     * Get the boolean value of killServer. fvt.adapter.spi.XAResourceImpl will
     * kill the server process if it is set to true.
     *
     * @return boolean
     */
    public static boolean getKillServer() {

        return killServer;
    }

    /**
     * Set the boolean value of killServer. fvt.adapter.spi.XAResourceImpl will
     * kill the server process if it is set to true.
     *
     * @param boolean
     *
     */
    public static void setkillServer(boolean kill) {

        killServer = kill;
    }

    /**
     * Get the String value of server process id.
     *
     * @return String
     */
    public static String getServerPid() {

        return serverPid;
    }

    /**
     * Set the String value of server pid.
     *
     * @param String
     *
     */
    public static void setServerPid(String pid) {
        serverPid = pid;
    }

    /**
     * Kill the appserver process.
     *
     */
    public static void killServerProcess() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "killServerProcess()");

        try {
            String cmd = "kill " + serverPid;

            if (tc.isDebugEnabled())
                Tr.debug(tc, "kill server process : " + cmd);

            Process run = null;
            run = Runtime.getRuntime().exec(cmd);
            run.waitFor();
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, " *Exception : killServerProcess()", e);
        }
    }

    // Added begin by gburli - LI2110.97 - 06/08/2004
    /**
     * Set all invalid ManagedConnection.
     * fvt.adapter.spi.ManagedConnectionFactoryImpl will send invalid
     * connections to this method.
     *
     */

    public static void addInvalidMCToSet(ManagedConnection FailedMCs) throws Exception {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setInvalidMCs()");
        failedMCSet.add(FailedMCs);
    }

    /**
     * Set all invalid ManagedConnection. Destroy method of
     * fvt.adapter.spi.ManagedConnectionImpl will send invalid connections to
     * this method.
     *
     */
    public static void addMCToSet(ManagedConnection MCs) throws Exception {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "addMCToSet()");
        MCSet.add(MCs);
    }

    /**
     * Get the Invalid set of ManagedConnections.
     *
     * @param Set
     *
     */
    public static Set getMCSet() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getMCSet()");
        return MCSet;
    }

    /**
     * Get the Invalid set of ManagedConnections.
     *
     * @param Set
     *
     */

    public static Set getInvalidMCSet() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getInvalidMCSet()");
        return failedMCSet;
    }

    /**
     * Empty the Invalid set of ManagedConnections got from getInvalidConnection
     * method.
     *
     * @param Set
     *
     */

    public static Set emptyInvalidMCSet() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "emptyInvalidMCSet");
        if (!failedMCSet.isEmpty())
            failedMCSet.clear();
        return failedMCSet;
    }

    /**
     * Empty the Invalid set of ManagedConnections got from destroy method.
     *
     * @param Set
     *
     */

    public static Set emptyMCSet() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "emptyMCSet");
        if (!MCSet.isEmpty())
            MCSet.clear();
        return MCSet;
    }

    /**
     * Set the condition for testcase
     *
     *
     */

    public static void setInvalidConnFlag(int flag) {
        invalidConnFlag = flag;
    }

    /**
     * Set the condition for test in fvt.adapter.spi.ManagedConnectionImpl will
     * send invalid connections to this method.
     *
     *
     */

    public static int getInvalidConnFlag() {
        return invalidConnFlag;
    }

    // Added end by gburli - LI2110.97 - 06/08/2004

    // Added begins by gburli - LI2110.01 - 01/03/2005
    /**
     * Set the condition for testcase
     *
     *
     */

    public static void setThrottlingSupportFlag(int flag) {
        throttlingSupportFlag = flag;
    }

    /**
     * Set the condition for test in fvt.adapter.spi.ManagedConnectionImpl will
     * send invalid connections to this method.
     *
     *
     */

    public static int getThrottlingSupportFlag() {
        return throttlingSupportFlag;
    }

    public static void setMessageFromRA(String msg) {
        messageAck = msg;
    }

    /**
     * Set the condition for test in fvt.adapter.spi.ManagedConnectionImpl will
     * send invalid connections to this method.
     *
     *
     */

    public static String getMessageFromRA() {
        return messageAck;
    }
    // Added ends by gburli - LI2110.01 - 01/03/2005

    // @alvinso.3
    /*
     * 06/02/04: swai /**
     *
     * @return
     *
     * public static String getXaRecoveryAliasInbound() { return
     * xaRecoveryAliasInbound; }
     *
     * /**
     *
     * @return
     *
     * public static String getXaRecoveryAliasOutbound() { return
     * xaRecoveryAliasOutbound; }
     *
     * /**
     *
     * @param string
     *
     * public static void setXaRecoveryAliasInbound(String string) {
     * xaRecoveryAliasInbound = string; }
     *
     * /**
     *
     * @param string
     *
     * public static void setXaRecoveryAliasOutbound(String string) {
     * xaRecoveryAliasOutbound = string; }
     *
     * /**
     *
     * @return
     *
     * public static boolean isGetXARecoveryTokenInbound() { return
     * getXARecoveryTokenInbound; }
     *
     * /**
     *
     * @return
     *
     * public static boolean isGetXARecoveryTokenOutbound() { return
     * getXARecoveryTokenOutbound; }
     *
     * /**
     *
     * @param b
     *
     * public static void setGetXARecoveryTokenInbound(boolean b) {
     * getXARecoveryTokenInbound = b; }
     *
     * /**
     *
     * @param b
     *
     * public static void setGetXARecoveryTokenOutbound(boolean b) {
     * getXARecoveryTokenOutbound = b; }
     */

}

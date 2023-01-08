/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.custom.jdbc.impl;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.util.List;

import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.recoverylog.spi.TraceConstants;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * This abstract class encapsulates logic to determine whether a SQL operation has encountered a transient condition in an HA RDBMS environment
 * and to retry operations if the condition was transient.
 *
 * Child classes of SQLRetry provide the specific code that should be retried.
 *
 */
public abstract class SQLRetry {

    private static final TraceComponent tc = Tr.register(SQLRetry.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    private Throwable _nonTransientException;
    private static boolean _logRetriesEnabled;

    private static final int DEFAULT_TRANSIENT_RETRY_SLEEP_TIME = 10000; // In milliseconds, ie 10 seconds
    private static final int DEFAULT_TRANSIENT_RETRY_ATTEMPTS = 180; // We'll keep retrying for 30 minutes. Excessive?
    private static int _transientRetrySleepTime = DEFAULT_TRANSIENT_RETRY_SLEEP_TIME;
    private static int _transientRetryAttempts = DEFAULT_TRANSIENT_RETRY_ATTEMPTS;
    private static final int LIGHTWEIGHT_TRANSIENT_RETRY_SLEEP_TIME = 1000; // In milliseconds, ie 1 second
    private static final int LIGHTWEIGHT_TRANSIENT_RETRY_ATTEMPTS = 2; // We'll keep retrying for 2 seconds in the lightweight case
    private static int _lightweightRetrySleepTime = LIGHTWEIGHT_TRANSIENT_RETRY_SLEEP_TIME;
    private static int _lightweightRetryAttempts = LIGHTWEIGHT_TRANSIENT_RETRY_ATTEMPTS;

    /**
     * This method provides drives the retry loop in retryAfterSQLException and reports and handles the outcome.
     *
     * @param recoveryLog
     * @param serverName
     * @param currentSqlEx
     * @param batchSQLOperation
     * @param transientRetryAttempts
     * @param transientRetrySleepTime
     * @param sqlTransientErrorHandlingEnabled
     * @return
     */
    public boolean retryAndReport(SQLRetriableLog recoveryLog, String serverName, SQLException currentSqlEx) {
        boolean theReturn = retryAndReport(recoveryLog, serverName, currentSqlEx, _transientRetryAttempts, _transientRetrySleepTime);
        return theReturn;
    }

    public boolean retryAndReport(SQLRetriableLog recoveryLog, String serverName, SQLException currentSqlEx, int retryAttempts, int retrySleepTime) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "retryAndReport ", new Object[] { recoveryLog, serverName, currentSqlEx, retryAttempts, retrySleepTime });
        boolean failAndReport = true;
        if (currentSqlEx != null) {
            // Set the exception that will be reported
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Set the exception that will be reported: " + currentSqlEx);
            _nonTransientException = currentSqlEx;
            // The following method will reset "_nonTransientException" if it cannot recover
            failAndReport = retryAfterSQLException(recoveryLog, currentSqlEx, retryAttempts, retrySleepTime);
        }

        // We've been through the while loop
        if (failAndReport) {
            Tr.debug(tc, "Cannot recover from SQLException when " + getOperationDescription() + " for server " + serverName + " Exception: "
                         + _nonTransientException);
        } else {
            Tr.debug(tc, "Have recovered from SQLException when " + getOperationDescription() + " server " + serverName);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "retryAndReport", !failAndReport);
        return !failAndReport;
    }

    public boolean retryAfterSQLException(SQLRetriableLog retriableLog, SQLException sqlex) {
        boolean failAndReport = retryAfterSQLException(retriableLog, sqlex, _transientRetryAttempts, _transientRetrySleepTime);
        return failAndReport;
    }

    /**
     * The retryAfterSQLException method provides a retry loop around a piece of code, encapsulated by retryCode(), that can be retried when a transient
     * RDBMS error condition has been encountered.
     *
     * @param retriableLog
     * @param sqlex
     * @param transientRetryAttempts
     * @param transientRetrySleepTime
     * @return
     */
    public boolean retryAfterSQLException(SQLRetriableLog retriableLog, SQLException sqlex, int retryAttempts, int retrySleepTime) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "retryAfterSQLException ", retriableLog, sqlex, retryAttempts, retrySleepTime);

        boolean shouldRetry = true;
        boolean failAndReport = false;
        int operationRetries = 0;
        int initialIsolation = 0;

        Connection conn = null;

        while (shouldRetry && !failAndReport) {
            // Should we attempt to reconnect? This method works through the set of SQL exceptions and will
            // return TRUE if we determine that a transient DB error has occurred
            if (FrameworkState.isStopping()) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Not retrying because the server is stopping");
                failAndReport = true;
            } else if (operationRetries++ < retryAttempts) {
                // We havent exceeded the number of retry attempts

                initialIsolation = Connection.TRANSACTION_REPEATABLE_READ;

                // Iterate through possible sqlcodes
                shouldRetry = isSQLErrorTransient(sqlex); // For batch, calling code needs: sqlTransientErrorHandlingEnabled && recoveryLog.isSQLErrorTransient(sqlex);

                if (shouldRetry) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Try to reexecute the SQL, attempt number: " + operationRetries);

                    // Re-execute the SQL
                    try {
                        // Get a connection to database via its datasource
                        conn = retriableLog.getConnection();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Acquired connection in Database retry scenario");
                        initialIsolation = retriableLog.prepareConnectionForBatch(conn);
                        // Retry logic here
                        retryCode(conn);
                        conn.commit();
                        // The Operation has executed successfully and we can continue processing
                        shouldRetry = false;
                    } catch (SQLException sqlex2) {
                        // We've caught another SQLException. Assume that we've retried the connection too soon.
                        // Make sure we inspect the latest exception
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "reset the sqlex to " + sqlex2);
                        sqlex = sqlex2;
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "sleeping for " + retrySleepTime + " millisecs");
                        try {
                            Thread.sleep(retrySleepTime);
                        } catch (InterruptedException ie) {
                        }
                    } catch (Throwable exc) {
                        // Not a SQLException, break out of the loop and report the exception
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Failed got exception: ", exc);

                        failAndReport = true;
                        _nonTransientException = exc;
                    } finally {
                        if (conn != null) {
                            // Used for retrying on log open and force operations
                            if (shouldRetry) {
                                // Attempt a rollback. If it fails, trace the failure but allow processing to continue
                                try {
                                    conn.rollback();
                                } catch (Throwable exc) {
                                    // Trace the exception
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Rollback Failed, when handling SQLException, got exception: " + exc);
                                }
                            }
                            // Attempt a close. If it fails, trace the failure but allow processing to continue
                            try {
                                retriableLog.closeConnectionAfterBatch(conn, initialIsolation);
                            } catch (Throwable exc) {
                                // Trace the exception
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Close Failed, when handling SQLException, got exception: " + exc);
                            }
                        } else if (tc.isDebugEnabled())
                            Tr.debug(tc, "Connection was NULL");
                    }
                } else
                    failAndReport = true;
            } else {
                // We have exceeded the number of retry attempts
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Exceeded number of retry attempts");
                failAndReport = true;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "retryAfterSQLException", failAndReport);
        return failAndReport;
    }

    public void setNonTransientException(Throwable nonTransientException) {
        _nonTransientException = nonTransientException;
    }

    public Throwable getNonTransientException() {
        return _nonTransientException;
    }

    /**
     * This method, implemented by child classes will provide the logic that will be retried.
     *
     * @param conn
     * @throws SQLException
     */
    public abstract void retryCode(Connection conn) throws SQLException, Exception;

    /**
     * Return a string that describes the nature of the work done by an implementing class.
     *
     * @return
     */
    public abstract String getOperationDescription();

    //------------------------------------------------------------------------------
    // Method: SQLRetry.isSQLErrorTransient
    //------------------------------------------------------------------------------
    /**
     * Determine whether we have encountered a potentially transient SQL error condition. If so we can retry the SQL.
     *
     * This method is at the heart of the design for retrying SQL operations. If a SQL call receives a SQLTransientException, then we will re-execute the call (acquiring a new
     * Connection on each occasion) a configured number of times with a configured time interval until the call succeeds or we give up, report the exception and invalidate the
     * recovery log.
     *
     * This design has been (optionally) extended to retry on any SQLException if the server.xml enableLogRetries parameter has been set.
     *
     * Note that we have considered the specific situation where a call receives a SQLException on commit() and the call is re-executed. It is not possible to know whether the
     * commit operation failed or succeeded, so failure is assumed. The impact on this situation occurring in forceSections() is as follows.
     *
     * In the forceSections() code an RMLT may comprise a set of inserts, updates and deletes. In the (potential) case where a SQLException was thrown but the commit operation
     * was completed by the RDBMS, then, after acquiring a new Connection and taking the HADB lock...
     *
     * - re-execution of the inserts would result in a set of duplicate rows in the db table
     * - re-execution of the updates would be a no-op, ie setting columns in a row to identical values
     * - re-execution of the deletes would also be a no-op, the second attempt would simply delete no rows.
     *
     * The potentially sticky thing here is the duplicate rows in the db table. In practice, this does not appear to be an issue - recovery will create one set of runtime
     * objects from the duplicate rows and deletion at runtime will clear out all appropriate rows.
     *
     * Finally, behaviour can be altered by specifying lists of sqlcodes that should be handled differently.
     *
     * - if enableLogRetries has NOT been set (the original behaviour)
     *
     * --- A customer can set a list of retriable sqlcodes.
     * ------ If the current sqlcode is found in the list, then the operation will be retried.
     * ------ If the current exception is a BatchUpdateException and one of its chained SQLExceptions has an sqlcode in the list, then the operation will be retried.
     * --- If the list is empty, then the operation will be retried if the SQLException is a SQLTransientException or if the SQLException is a BatchUpdateException
     * and one of its chained SQLExceptions is a SQLTransientException
     *
     * - if enableLogRetries has been set (new behaviour)
     *
     * --- A customer can set a list of non-retriable sqlcodes.
     * ------ If the current sqlcode is found in the list, then the operation will not be retried.
     * ------ If the current exception is a BatchUpdateException and one of its chained SQLExceptions has an sqlcode in the list, then the operation will not be retried.
     * --- If the list is empty, then the operation will be retried.
     *
     * @return true if the error is transient.
     */
    protected static boolean isSQLErrorTransient(SQLException sqlex) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isSQLErrorTransient ", new Object[] { sqlex });

        boolean retryBatch = false;

        boolean isRetriableSqlCodeList = false;
        boolean isNonRetriableSqlCodeList = false;
        List<Integer> retriableSqlCodesList = null;
        List<Integer> nonRetriableSqlCodesList = null;
        boolean delveIntoException = true;
        int sqlErrorCode = sqlex.getErrorCode();

        if (tc.isEventEnabled()) {
            Tr.event(tc, " SQL exception:");
            Tr.event(tc, " Message: " + sqlex.getMessage());
            Tr.event(tc, " SQLSTATE: " + sqlex.getSQLState());
            Tr.event(tc, " Error code: " + sqlErrorCode);
        }

        // Determine whether to retry the operation based on the type of SQLException and its sqlcode.
        if (!_logRetriesEnabled) {
            // This is the original behaviour, where the enableLogRetries attribute has not been set in server.xml. In this scenario,
            // the retriableSqlCodesList may come into play, where specific codes that should be retried have been specified.

            // Check whether specific retriable sqlcodes have been configured
            retriableSqlCodesList = ConfigurationProviderManager.getConfigurationProvider().getRetriableSqlCodes();
            boolean foundRetriableSqlCode = false;
            if (retriableSqlCodesList != null && !retriableSqlCodesList.isEmpty()) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "There are retriable sqlcodes in " + retriableSqlCodesList);
                isRetriableSqlCodeList = true;
                foundRetriableSqlCode = isErrorCodeInCodeList(retriableSqlCodesList, sqlErrorCode);
            }

            if (foundRetriableSqlCode || sqlex instanceof SQLTransientException) {
                retryBatch = true;
                delveIntoException = false;
            }
        } else {
            // This is the new behaviour, where the enableLogRetries attribute has been set in server.xml. In this scenario,
            // the nonRetriableSqlCodesList may come into play, where specific codes that should NOT be retried have been specified.

            // Check whether specific non-retriable sqlcodes have been configured
            nonRetriableSqlCodesList = ConfigurationProviderManager.getConfigurationProvider().getNonRetriableSqlCodes();
            if (nonRetriableSqlCodesList != null && !nonRetriableSqlCodesList.isEmpty()) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "There are non-retriable sqlcodes in " + nonRetriableSqlCodesList);
                isNonRetriableSqlCodeList = true;
                if (isErrorCodeInCodeList(nonRetriableSqlCodesList, sqlErrorCode)) {
                    // We have found this exception set in the non-retriable list. We will not retry and we won't do any further digging
                    retryBatch = false;
                    delveIntoException = false;
                } else {
                    // We haven't found a non-retriable exception so we set the retry flag to true. But it is still possible that
                    // a non-retriable exception may be nested in a BatchUpdateException, so we set the delve flag to true
                    retryBatch = true;
                    delveIntoException = true;
                }
            } else {
                // There is no list of non-retriable exceptions
                retryBatch = true;
                delveIntoException = false;
            }
        }

        if (delveIntoException && sqlex instanceof BatchUpdateException) {
            if (tc.isDebugEnabled()) {
                if (sqlex instanceof SQLTransientException) {
                    Tr.debug(tc, "Exception is not considered transient but does implement SQLTransientException!");
                }
            }

            BatchUpdateException buex = (BatchUpdateException) sqlex;
            Tr.event(tc, "BatchUpdateException: Update Counts - ");
            int[] updateCounts = buex.getUpdateCounts();
            for (int i = 0; i < updateCounts.length; i++) {
                Tr.event(tc, "   Statement " + i + ":" + updateCounts[i]);
            }
            SQLException nextex = buex.getNextException();
            while (nextex != null) {
                sqlErrorCode = nextex.getErrorCode();
                if (tc.isEventEnabled()) {
                    Tr.event(tc, " SQL exception:");
                    Tr.event(tc, " Message: " + nextex.getMessage());
                    Tr.event(tc, " SQLSTATE: " + nextex.getSQLState());
                    Tr.event(tc, " Error code: " + sqlErrorCode);
                }

                // The behaviour here depends on whether the logRetriesEnabled flag has been set.
                if (!_logRetriesEnabled) {
                    // This is the original behaviour though we may also need to check the retriable code list too
                    if (nextex instanceof SQLTransientException) {
                        retryBatch = true;
                        break;
                    }

                    // Check the list if there is one
                    if (isRetriableSqlCodeList) {
                        if (isErrorCodeInCodeList(retriableSqlCodesList, sqlErrorCode)) {
                            retryBatch = true;
                            break;
                        }
                    }
                } else {
                    // This is the new behaviour, we need to check the non-retriable code list
                    if (isNonRetriableSqlCodeList) {
                        if (isErrorCodeInCodeList(nonRetriableSqlCodesList, sqlErrorCode)) {
                            retryBatch = false;
                            break;
                        }
                    }
                }

                nextex = nextex.getNextException();
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isSQLErrorTransient", retryBatch);

        return retryBatch;
    }

    /**
     * Traverse a list of sql error codes to see if it contains a specific value, sqlErrorCode.
     *
     * @param sqlCodesList
     * @param sqlErrorCode
     * @return
     */
    private static boolean isErrorCodeInCodeList(List<Integer> sqlCodesList, int sqlErrorCode) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isErrorCodeInCodeList ", new Object[] { sqlCodesList, sqlErrorCode });
        boolean codeIsInList = false;

        if (sqlCodesList == null || sqlCodesList.isEmpty()) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The list is null or empty");
        } else {
            if (sqlCodesList.contains(sqlErrorCode)) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "The error code is in the list");
                codeIsInList = true;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isErrorCodeInCodeList", codeIsInList);
        return codeIsInList;
    }

    /**
     * @return the _logRetriesEnabled
     */
    public static boolean isLogRetriesEnabled() {
        return _logRetriesEnabled;
    }

    /**
     * @param logRetriesEnabled the _logRetriesEnabled to set
     */
    public static void setLogRetriesEnabled(boolean logRetriesEnabled) {
        SQLRetry._logRetriesEnabled = logRetriesEnabled;
    }

    /**
     * @return the _transientRetrySleepTime
     */
    public static int getTransientRetrySleepTime() {
        return _transientRetrySleepTime;
    }

    /**
     * @param transientRetrySleepTime the _transientRetrySleepTime to set
     */
    public static void setTransientRetrySleepTime(int transientRetrySleepTime) {
        SQLRetry._transientRetrySleepTime = transientRetrySleepTime;
    }

    /**
     * @return the _transientRetryAttempts
     */
    public static int getTransientRetryAttempts() {
        return _transientRetryAttempts;
    }

    /**
     * @param _transientRetryAttempts the _transientRetryAttempts to set
     */
    public static void setTransientRetryAttempts(int transientRetryAttempts) {
        SQLRetry._transientRetryAttempts = transientRetryAttempts;
    }

    /**
     * @return the _lightweightRetrySleepTime
     */
    public static int getLightweightRetrySleepTime() {
        return _lightweightRetrySleepTime;
    }

    /**
     * @param lightweightRetrySleepTime the _lightweightRetrySleepTime to set
     */
    public static void setLightweightRetrySleepTime(int lightweightRetrySleepTime) {
        SQLRetry._lightweightRetrySleepTime = lightweightRetrySleepTime;
    }

    /**
     * @return the _lightweightRetryAttempts
     */
    public static int getLightweightRetryAttempts() {
        return _lightweightRetryAttempts;
    }

    /**
     * @param lightweightRetryAttempts the _lightweightRetryAttempts to set
     */
    public static void setLightweightRetryAttempts(int lightweightRetryAttempts) {
        SQLRetry._lightweightRetryAttempts = lightweightRetryAttempts;
    }
}

/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.custom.jdbc.impl;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.recoverylog.spi.TraceConstants;

/**
 * This abstract class encapsulates logic to determine whether a SQL operation has encountered a transient condition in an HA RDBMS environment
 * and to retry operations if the condition was transient.
 *
 * Child classes of SQLHADBRetry provide the specific code that should be retried.
 *
 */
public abstract class SQLHADBRetry {

    private static final TraceComponent tc = Tr.register(SQLHADBRetry.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    private Throwable _nonTransientException = null;

    /**
     * This method provides drives the retry loop in retryAfterSQLException and reports and handles the outcome.
     *
     * @param recoveryLog
     * @param dataSource
     * @param serverName
     * @param currentSqlEx
     * @param batchSQLOperation
     * @param transientRetryAttempts
     * @param transientRetrySleepTime
     * @param sqlTransientErrorHandlingEnabled
     * @return
     */
    public boolean retryAndReport(SQLMultiScopeRecoveryLog recoveryLog, DataSource dataSource, String serverName, SQLException currentSqlEx,
                                  int transientRetryAttempts, int transientRetrySleepTime) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "retryAndReport ", new Object[] { recoveryLog, dataSource, serverName, currentSqlEx, transientRetryAttempts,
                                                           transientRetrySleepTime });
        boolean failAndReport = true;
        if (currentSqlEx != null) {
            // Set the exception that will be reported
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Set the exception that will be reported: " + currentSqlEx);
            _nonTransientException = currentSqlEx;
            // The following method will reset "_nonTransientException" if it cannot recover
            failAndReport = retryAfterSQLException(recoveryLog, dataSource, currentSqlEx, transientRetryAttempts, transientRetrySleepTime);
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

    /**
     * The retryAfterSQLException method provides a retry loop around a piece of code, encapsulated by retryCode(), that can be retried when a transient
     * RDBMS error condition has been encountered.
     *
     * @param recoveryLog
     * @param dataSource
     * @param sqlex
     * @param transientRetryAttempts
     * @param transientRetrySleepTime
     * @return
     */
    public boolean retryAfterSQLException(SQLMultiScopeRecoveryLog recoveryLog, DataSource dataSource, SQLException sqlex,
                                          int transientRetryAttempts, int transientRetrySleepTime) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "retryAfterSQLException ", new Object[] { recoveryLog, dataSource, sqlex, transientRetryAttempts,
                                                                   transientRetrySleepTime });
        boolean shouldRetry = true;
        boolean failAndReport = false;
        int operationRetries = 0;
        int initialIsolation = 0;

        Connection conn = null;

        while (shouldRetry && !failAndReport) {
            // Should we attempt to reconnect? This method works through the set of SQL exceptions and will
            // return TRUE if we determine that a transient DB error has occurred
            if (operationRetries < transientRetryAttempts) {
                // We havent exceeded the number of retry attempts

                initialIsolation = Connection.TRANSACTION_REPEATABLE_READ;

                // Iterate through possible sqlcodes
                shouldRetry = recoveryLog.isSQLErrorTransient(sqlex); // For batch, calling code needs: sqlTransientErrorHandlingEnabled && recoveryLog.isSQLErrorTransient(sqlex);

                operationRetries++;
                if (shouldRetry) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Try to reexecute the SQL using connection from DS: " + dataSource + ", attempt number: " + operationRetries);
                    if (dataSource != null) {
                        // Re-execute the SQL
                        try {
                            // Get a connection to database via its datasource
                            conn = dataSource.getConnection();

                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Acquired connection in Database retry scenario");
                            initialIsolation = recoveryLog.prepareConnectionForBatch(conn);
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
                                Tr.debug(tc, "sleeping for " + transientRetrySleepTime + " millisecs");
                            try {
                                Thread.sleep(transientRetrySleepTime);
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
                                    recoveryLog.closeConnectionAfterBatch(conn, initialIsolation);
                                } catch (Throwable exc) {
                                    // Trace the exception
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Close Failed, when handling SQLException, got exception: " + exc);
                                }
                            } else if (tc.isDebugEnabled())
                                Tr.debug(tc, "Connection was NULL");
                        }
                    } else {
                        // This is unexpected and catastrophic, the reference to the DataSource is null
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "NULL DataSource reference");
                        failAndReport = true;
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
}

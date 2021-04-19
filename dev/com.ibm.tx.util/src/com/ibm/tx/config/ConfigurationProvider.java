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

package com.ibm.tx.config;

import java.util.logging.Level;

import com.ibm.tx.util.alarm.AlarmManager;
import com.ibm.wsspi.resource.ResourceFactory;

public interface ConfigurationProvider {
    /**
     * <p>
     * The maximum period, in seconds, that a transaction may be active. If completion
     * processing for a transaction has not begun within the timeout period that
     * transaction will be marked rollback only.
     * </p>
     *
     * @return The maximum period, in seconds, that a transaction may be active.
     */
    public int getTotalTransactionLifetimeTimeout();

    /**
     * <p>
     * The maximum period, in seconds, of a transaction's lifetime for which a client
     * may be inactive before the transaction will be marked rollback only.
     *
     * @return The client inactivity timeout period, in seconds.
     */
    public int getClientInactivityTimeout();

    /**
     * <p>
     * The maximum value in seconds for the timeout period of transactions that are
     * begun programatically or imported into the transaction manager. Should a timeout
     * be greater than the maximum it will be capped to this maximum.
     * </p>
     *
     * @return The maximum allowed timeout value, in seconds, for imported or
     *         programatically begun transactions.
     */
    public int getMaximumTransactionTimeout();

    /**
     * <p>
     * In the event of a transient failure from a resource manager, the heuristic retry
     * limit controls the number of attempts that will be made to deliver the
     * outcome of a transaction (i.e. commit or rollback) to a resource manager before
     * the transaction manager makes a heuristic decision.
     * </p>
     *
     * @return The maximum number of attempts to be made to deliver a transaction's
     *         outcome to a resource manager. A value of zero indicates that the outcome
     *         delivery should be retried for ever.
     */
    //TODO Consider if zero is appropriate for retry forever; should it be -1?
    //     Zero could then indicate that the outcome delivery should not be retried.
    public int getHeuristicRetryLimit();

    /**
     * <p>
     * Determines the period, in seconds, between attempts to deliver a transaction's
     * outcome to a resource manager in the event of a transient failure. A value
     * of zero or less results in a transaction manager default being used.
     * </p>
     *
     * @return The period, in seconds, to wait between retry attempts.
     */
    public int getHeuristicRetryInterval();

    /**
     * <p>
     * In the event of a heuristic decision being required, specifies
     * that the transaction service should commit the transaction.
     * </p>
     */
    public static final int HEURISTIC_COMPLETION_DIRECTION_COMMIT = 0;

    /**
     * <p>
     * In the event of a heuristic decision being required, specifies
     * that the transaction service should rollback the transaction.
     * </p>
     */
    public static final int HEURISTIC_COMPLETION_DIRECTION_ROLLBACK = 1;

    /**
     * <p>
     * In the event of a heuristic decision being required, specifies
     * that the transaction service should await manual intervention
     * to determine the completion direction.
     * </p>
     */
    // TODO Is a manual heuristic completion direction appropriate in a non-WAS environment?
    //      If we decide it is we'll have to provide a programattic way of
    //      getting at heuristic transactions so that they can be committed
    //      or rolled back.
    public static final int HEURISTIC_COMPLETION_DIRECTION_MANUAL = 2;

    /**
     * <p>
     * Returns the completion direction, i.e. commit, rollback, or manual, for
     * transactions that complete heuristically.
     * </p>
     *
     * @return The completion direction for heuristic transactions
     *
     * @see #HEURISTIC_COMPLETION_DIRECTION_COMMIT
     * @see #HEURISTIC_COMPLETION_DIRECTION_ROLLBACK
     * @see #HUERISTIC_COMPLETION_DIRECTION_MANUAL
     */
    public int getHeuristicCompletionDirection();

    /**
     * <p>
     * Returns the completion direction, i.e. commit, rollback, or manual, for
     * transactions that complete heuristically as a human-readable String.
     * </p>
     *
     * @return The completion direction for heuristic transactions
     */
    public String getHeuristicCompletionDirectionAsString();

    /**
     * <p>
     * Returns the name of the directoy to be used to store the transaction
     * service's log files. The directoy name may be relative or absolute
     * and will be created if it does not already exist.
     * </p>
     *
     * @return The name of the directory in which the transaction logs will be stored.
     */
    public String getTransactionLogDirectory();

    public String getServerName();

    /**
     * <p>
     * Returns the size of the transaction log in kilobytes (KB). The minimum value
     * is 64KB; any value less than that will be rounded up to this minimum.
     * </p>
     *
     * @return The transaction log size in KB.
     */
    // TODO Do we want to allow the configuration of two log sizes? One for the transaction log and one for the partner log?
    public int getTransactionLogSize();

    // TODO WAS-specific LPS setting? Do we want to support LPS in the componentised TM?
    public boolean isLoggingForHeuristicReportingEnabled();

    public boolean isAcceptHeuristicHazard();

    /**
     * An implementation of this method should return the AlarmManager implementation
     * to be used by the transaction manager. <code>null</code> may be
     * returned and indicates that the transaction manager should use its default
     * implementation.
     *
     * @return The AlarmManager implementation to be used by the transaction manager
     */
    public AlarmManager getAlarmManager();

    public RuntimeMetaDataProvider getRuntimeMetaDataProvider();

    public void shutDownFramework();

    public Level getTraceLevel();

    /**
     * The Maximum Shutdown Delay configuration is an integer number of seconds
     * with the following semantics:<br>
     *
     * >=0 shutdown() will wait a maximum of this long for transactions to complete<br>
     * <0 shutdown() will wait indefinitely for transactions to complete
     *
     * @return The configured delay
     */
    public int getDefaultMaximumShutdownDelay();

    /**
     * The Audit Recovery flag indicates whether recovery processing should output audit
     * messages indicating xa resource xid processing during recovery. When no audit
     * recovery is specified, only a single message indicating recovery in progress is
     * output together with the number of transactions recovered, unless an error occurs.
     */
    public boolean getAuditRecovery(); // @PM07874A

    public boolean isWaitForRecovery();

    public boolean isRecoverOnStartup();

    public boolean isOnePCOptimization();

    /**
     * This method is provided for Liberty integration. The Liberty
     * com.ibm.ws.transaction bundle retrieves the resource factory
     * from the configuration.
     */
    public ResourceFactory getResourceFactory();

    /**
     * Returns whether we will propagate the transaction timeout to XAResources
     */
    public boolean getPropagateXAResourceTransactionTimeout();

    /**
     * @return
     */
    public String getRecoveryIdentity();

    public String getRecoveryGroup();

    public boolean isShutdownOnLogFailure();

    public String getLeaseCheckStrategy();

    public int getLeaseCheckInterval();

    public int getLeaseLength();

    public int getLeaseRenewalThreshold();

    /**
     * Sets the applId of the server.
     *
     * @param name The applId. Non-recoverable servers may have an applId but no name.
     */
    public void setApplId(byte[] name);

    /**
     * Returns the applId of the server.
     * <p>
     * Non-recoverable servers may have an applid but not a name.
     *
     * @return The applId of the server.
     */
    public byte[] getApplId();

    /**
     * Return true if the Tran recovery logs are to be stored in a database.
     *
     * @return
     */
    public boolean isSQLRecoveryLog();

    /**
     * Return true if this ConfigurationProvider has dependencies on other Declarative Services.
     *
     * @return
     */
    public boolean needToCoordinateServices();

    /**
     * Return true when the peer locking scheme is to be enabled for the Tran recovery logs that are stored in a database.
     *
     * @return
     */
    public boolean enableHADBPeerLocking();

    /**
     * Configures the length of time between heartbeats when the peer locking scheme is enabled for the Tran recovery logs that are stored in a database.
     *
     * @return
     */
    public int getTimeBetweenHeartbeats();

    /**
     * Configures the length of time before a peer Tran recovery log is deemed to be stale when the peer locking scheme is enabled for Tran recovery logs that are stored in a
     * database.
     *
     * @return
     */
    public int getPeerTimeBeforeStale();

    /**
     * Configures the length of time between retries for HADB transient errors for standard operations where the Tran recovery logs are stored in a database.
     *
     * @return
     */
    public int getStandardTransientErrorRetryTime();

    /**
     * Configures the number of retries for HADB transient errors for standard operations where the Tran recovery logs are stored in a database.
     *
     * @return
     */
    public int getStandardTransientErrorRetryAttempts();

    /**
     * Configures the length of time between retries for HADB transient errors for lightweight operations where the Tran recovery logs are stored in a database.
     *
     * @return
     */
    public int getLightweightTransientErrorRetryTime();

    /**
     * Configures the number of retries for HADB transient errors for lightweight operations where the Tran recovery logs are stored in a database.
     *
     * @return
     */
    public int getLightweightTransientErrorRetryAttempts();
}

package com.ibm.tx.jta.embeddable.config;

/*******************************************************************************
 * Copyright (c) 2010, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.util.Map;
import java.util.logging.Level;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.RuntimeMetaDataProvider;
import com.ibm.tx.jta.util.alarm.AlarmManagerImpl;
import com.ibm.tx.util.alarm.AlarmManager;
import com.ibm.wsspi.resource.ResourceFactory;

public class EmbeddableConfigurationProviderImpl implements ConfigurationProvider {
    private static final TraceComponent tc = Tr.register(EmbeddableConfigurationProviderImpl.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    // these are copied and duplicated from RegisteredResources!
    public static final int WSAT_PREPARE_ORDER_CONCURRENT = 0; // "concurrent"
    public static final int WSAT_PREPARE_ORDER_BEFORE = 1; // "before"
    public static final int WSAT_PREPARE_ORDER_AFTER = 2; // "after"

    private static final String PROPERTY_NAME_PREFIX = "com.ibm.websphere.tx.";

    public static final String MAXIMUM_TRANSACTION_TIMEOUT = PROPERTY_NAME_PREFIX + "maximumTransactionTimeout";
    public static final String HEURISTIC_RETRY_WAIT = PROPERTY_NAME_PREFIX + "heuristicRetryWait";
    public static final String HEURISTIC_RETRY_LIMIT = PROPERTY_NAME_PREFIX + "heuristicRetryLimit";
    public static final String CLIENT_INACTIVITY_TIMEOUT = PROPERTY_NAME_PREFIX + "clientInactivityTimeout";
    public static final String TOTAL_TRAN_LIFETIME_TIMEOUT = PROPERTY_NAME_PREFIX + "totalTranLifetimeTimeout";
    public static final String LPS_HEURISTIC_COMPLETION = PROPERTY_NAME_PREFIX + "LPSHeuristicCompletion";
    public static final String ENABLE_LOGGING_FOR_HEURISTIC_REPORTING = PROPERTY_NAME_PREFIX + "enableLoggingForHeuristicReporting";
    public static final String ACCEPT_HEURISTIC_HAZARD = PROPERTY_NAME_PREFIX + "acceptHeuristicHazard";
    public static final String RECOVER_ON_STARTUP = PROPERTY_NAME_PREFIX + "recoverOnStartup";
    public static final String ONEPC_OPTIMIZATION = PROPERTY_NAME_PREFIX + "OnePCOptimization";
    public static final String WAIT_FOR_RECOVERY = PROPERTY_NAME_PREFIX + "waitForRecovery";
    public static final String TRAN_LOG_DIRECTORY = PROPERTY_NAME_PREFIX + "tranLogDirectory";
    public static final String TRAN_LOG_SIZE = PROPERTY_NAME_PREFIX + "tranLogSize";
    public static final String AUDIT_RECOVERY = PROPERTY_NAME_PREFIX + "auditRecovery"; // @PM07874A
    public static final String PROPAGATE_XARESOURCE_TIMEOUT = PROPERTY_NAME_PREFIX + "propagateXAResourceTransactionTimeout";
    private static final String WSAT_PREPARE_ORDER = PROPERTY_NAME_PREFIX + "wsatPrepareOrder";

    private static AlarmManager _alarmManager = new AlarmManagerImpl();
    private static int _clientInactivityTimeout;
    private static int _heuristicRetryInterval;
    private static int _heuristicRetryLimit;
    private static boolean _enableLoggingForHeuristicReporting;
    private static boolean _acceptHeuristicHazard;
    private static boolean _recoverOnStartup;
    private static boolean _shutdownOnLogFailure = true;
    private static boolean _OnePCOptimization;
    private static boolean _waitForRecovery;
    private static int _totalTranLifetimeTimeout;
    private static int _maximumTransactionTimeout;
    private static int _tranLogSize;
    private static String _heuristicCompletionDirectionString;
    private static String _tranLogDirectory;
    private static int _heuristicCompletionDirection = HEURISTIC_COMPLETION_DIRECTION_ROLLBACK;
    private static boolean _auditRecovery = true; // @PM07874A
    private static boolean _propagateXAResourceTransactionTimeout;
    private static String _wsatPrepareOrderString;
    private static int _wsatPrepareOrder = WSAT_PREPARE_ORDER_CONCURRENT;

    private final RuntimeMetaDataProvider _runtimeMetaDataProvider = new EmbeddableRuntimeMetaDataProviderImpl(this);

    public EmbeddableConfigurationProviderImpl(Map<String, Object> properties) {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        _acceptHeuristicHazard = Boolean.valueOf((String) properties.get(ACCEPT_HEURISTIC_HAZARD));
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, ACCEPT_HEURISTIC_HAZARD + " = " + _acceptHeuristicHazard);

        _recoverOnStartup = Boolean.valueOf((String) properties.get(RECOVER_ON_STARTUP));
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, RECOVER_ON_STARTUP + " = " + _recoverOnStartup);

        _OnePCOptimization = Boolean.valueOf((String) properties.get(ONEPC_OPTIMIZATION));
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, ONEPC_OPTIMIZATION + " = " + _OnePCOptimization);

        _waitForRecovery = Boolean.valueOf((String) properties.get(WAIT_FOR_RECOVERY));
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, WAIT_FOR_RECOVERY + " = " + _waitForRecovery);

        _enableLoggingForHeuristicReporting = Boolean.valueOf((String) properties.get(ENABLE_LOGGING_FOR_HEURISTIC_REPORTING));
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, ENABLE_LOGGING_FOR_HEURISTIC_REPORTING + " = " + _enableLoggingForHeuristicReporting);

        _heuristicCompletionDirectionString = (String) properties.get(LPS_HEURISTIC_COMPLETION);

        if ("COMMIT".equalsIgnoreCase(_heuristicCompletionDirectionString)) {
            _heuristicCompletionDirection = HEURISTIC_COMPLETION_DIRECTION_COMMIT;
        } else if ("MANUAL".equalsIgnoreCase(_heuristicCompletionDirectionString)) {
            _heuristicCompletionDirection = HEURISTIC_COMPLETION_DIRECTION_MANUAL;
        } else {
            _heuristicCompletionDirectionString = "ROLLBACK";
            if (traceOn && tc.isDebugEnabled())
                Tr.debug(tc, LPS_HEURISTIC_COMPLETION + " = ROLLBACK");
        }
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, LPS_HEURISTIC_COMPLETION + " = " + _heuristicCompletionDirectionString);

        String tmp = (String) properties.get(TRAN_LOG_SIZE);
        _tranLogSize = Integer.parseInt(tmp != null ? tmp : "0");
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, TRAN_LOG_SIZE + " = " + _tranLogSize);

        tmp = (String) properties.get(TOTAL_TRAN_LIFETIME_TIMEOUT);
        _totalTranLifetimeTimeout = Integer.parseInt(tmp != null ? tmp : "0");
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, TOTAL_TRAN_LIFETIME_TIMEOUT + " = " + _totalTranLifetimeTimeout);

        tmp = (String) properties.get(CLIENT_INACTIVITY_TIMEOUT);
        _clientInactivityTimeout = Integer.parseInt(tmp != null ? tmp : "0");
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, CLIENT_INACTIVITY_TIMEOUT + " = " + _clientInactivityTimeout);

        tmp = (String) properties.get(HEURISTIC_RETRY_LIMIT);
        _heuristicRetryLimit = Integer.parseInt(tmp != null ? tmp : "0");
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, HEURISTIC_RETRY_LIMIT + " = " + _heuristicRetryLimit);

        tmp = (String) properties.get(HEURISTIC_RETRY_WAIT);
        _heuristicRetryInterval = Integer.parseInt(tmp != null ? tmp : "0");
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, HEURISTIC_RETRY_WAIT + " = " + _heuristicRetryInterval);

        tmp = (String) properties.get(MAXIMUM_TRANSACTION_TIMEOUT);
        _maximumTransactionTimeout = Integer.parseInt(tmp != null ? tmp : "0");
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, MAXIMUM_TRANSACTION_TIMEOUT + " = " + _maximumTransactionTimeout);

        _tranLogDirectory = (String) properties.get(TRAN_LOG_DIRECTORY);
        if (null == _tranLogDirectory || _tranLogDirectory.isEmpty()) {
            _tranLogDirectory = System.getProperty("user.dir");
        }
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, TRAN_LOG_DIRECTORY + " = " + _tranLogDirectory);

        _auditRecovery = Boolean.valueOf((String) properties.get(AUDIT_RECOVERY));
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, AUDIT_RECOVERY + " = " + _auditRecovery);

        _propagateXAResourceTransactionTimeout = Boolean.valueOf((String) properties.get(PROPAGATE_XARESOURCE_TIMEOUT));
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, PROPAGATE_XARESOURCE_TIMEOUT + " = " + _propagateXAResourceTransactionTimeout);

        _wsatPrepareOrderString = (String) properties.get(WSAT_PREPARE_ORDER);
        if ("before".equalsIgnoreCase(_wsatPrepareOrderString)) {
            _wsatPrepareOrder = WSAT_PREPARE_ORDER_BEFORE;
        } else if ("after".equalsIgnoreCase(_wsatPrepareOrderString)) {
            _wsatPrepareOrder = WSAT_PREPARE_ORDER_AFTER;
        } else {
            _wsatPrepareOrder = WSAT_PREPARE_ORDER_CONCURRENT;
        }
        if (traceOn && tc.isDebugEnabled())
            Tr.debug(tc, "WSAT_PREPARE_ORDER = " + _wsatPrepareOrder);
    }

    @Override
    public AlarmManager getAlarmManager() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getAlarmManager", _alarmManager);
        return _alarmManager;
    }

    @Override
    public int getClientInactivityTimeout() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getClientInactivityTimeout", _clientInactivityTimeout);
        return _clientInactivityTimeout;
    }

    @Override
    public int getHeuristicCompletionDirection() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getHeuristicCompletionDirection", _heuristicCompletionDirection);
        return _heuristicCompletionDirection;
    }

    @Override
    public String getHeuristicCompletionDirectionAsString() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getHeuristicCompletionDirectionAsString", _heuristicCompletionDirectionString);
        return _heuristicCompletionDirectionString;
    }

    @Override
    public int getHeuristicRetryInterval() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getHeuristicRetryInterval", _heuristicRetryInterval);
        return _heuristicRetryInterval;
    }

    @Override
    public int getHeuristicRetryLimit() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getHeuristicRetryLimit", _heuristicRetryLimit);
        return _heuristicRetryLimit;
    }

    @Override
    public int getMaximumTransactionTimeout() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getMaximumTransactionTimeout", _maximumTransactionTimeout);
        return _maximumTransactionTimeout;
    }

    @Override
    public RuntimeMetaDataProvider getRuntimeMetaDataProvider() {
        return _runtimeMetaDataProvider;
    }

    @Override
    public int getTotalTransactionLifetimeTimeout() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getTotalTransactionLifetimeTimeout", _totalTranLifetimeTimeout);
        return _totalTranLifetimeTimeout;
    }

    @Override
    public String getTransactionLogDirectory() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getTransactionLogDirectory", _tranLogDirectory);
        return _tranLogDirectory;
    }

    @Override
    public int getTransactionLogSize() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getTransactionLogSize", _tranLogSize);
        return _tranLogSize;
    }

    @Override
    public boolean isRecoverOnStartup() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isRecoverOnStartup", _recoverOnStartup);
        return _recoverOnStartup;
    }

    @Override
    public boolean isShutdownOnLogFailure() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isRecoverOnStartup", _shutdownOnLogFailure);
        return _shutdownOnLogFailure;
    }

    @Override
    public boolean isOnePCOptimization() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isOnePCOptimization", _OnePCOptimization);
        return _OnePCOptimization;
    }

    @Override
    public boolean isWaitForRecovery() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isWaitForRecovery", _waitForRecovery);
        return _waitForRecovery;
    }

    @Override
    public boolean isAcceptHeuristicHazard() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isAcceptHeuristicHazard", _acceptHeuristicHazard);
        return _acceptHeuristicHazard;
    }

    @Override
    public boolean isLoggingForHeuristicReportingEnabled() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isLoggingForHeuristicReportingEnabled", _enableLoggingForHeuristicReporting);
        return _enableLoggingForHeuristicReporting;
    }

    public static void setMaximumTransactionTimeout(int maximumTimeout) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setMaximumTransactionTimeout", maximumTimeout);
        _maximumTransactionTimeout = maximumTimeout;
    }

    public static void setClientInactivityTimeout(int timeout) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setClientInactivityTimeout", timeout);
        _clientInactivityTimeout = timeout;
    }

    public static void setTotalTransactionLifetimeTimeout(int timeout) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setTotalTransactionLifetimeTimeout", timeout);
        _totalTranLifetimeTimeout = timeout;
    }

    @Override
    public Level getTraceLevel() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getTraceLevel", Level.OFF);
        return Level.OFF;
    }

    @Override
    public int getDefaultMaximumShutdownDelay() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getDefaultMaximumShutdownDelay", 0);
        return 0;
    }

    @Override
    public boolean getAuditRecovery() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "auditRecovery", _auditRecovery);
        return _auditRecovery;
    }

    /**
     * This method is provided for Liberty integration. In tWAS
     * we'll return null.
     */
    @Override
    public ResourceFactory getResourceFactory() {
        return null;
    }

    @Override
    public boolean getPropagateXAResourceTransactionTimeout() {
        return _propagateXAResourceTransactionTimeout;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getRecoveryIdentity()
     */
    @Override
    public String getRecoveryIdentity() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getRecoveryGroup()
     */
    @Override
    public String getRecoveryGroup() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getServerName()
     */
    @Override
    public String getServerName() {
        return "";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#setApplId(byte[])
     */
    @Override
    public void setApplId(byte[] name) {
        // noop
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getApplId()
     */
    @Override
    public byte[] getApplId() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#shutDownFramework()
     */
    @Override
    public void shutDownFramework() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getLeaseCheckStrategy()
     */
    @Override
    public String getLeaseCheckStrategy() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getLeaseCheckInterval()
     */
    @Override
    public int getLeaseCheckInterval() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getLeaseLength()
     */
    @Override
    public int getLeaseLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#isSQLRecoveryLog()
     */
    @Override
    public boolean isSQLRecoveryLog() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#needToCoordinateServices()
     */
    @Override
    public boolean needToCoordinateServices() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#enableHADBPeerLocking()
     */
    @Override
    public boolean enableHADBPeerLocking() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getTimeBetweenHeartbeats()
     */
    @Override
    public int getTimeBetweenHeartbeats() {
        // TODO Auto-generated method stub
        return 5;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getPeerTimeBeforeStale()
     */
    @Override
    public int getPeerTimeBeforeStale() {
        // TODO Auto-generated method stub
        return 10;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getLightweightTransientErrorRetryTime()
     */
    @Override
    public int getLightweightTransientErrorRetryTime() {
        return 1;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getLightweightTransientErrorRetryAttempts()
     */
    @Override
    public int getLightweightTransientErrorRetryAttempts() {
        return 2;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getStandardTransientErrorRetryTime()
     */
    @Override
    public int getStandardTransientErrorRetryTime() {
        return 10;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getStandardTransientErrorRetryAttempts()
     */
    @Override
    public int getStandardTransientErrorRetryAttempts() {
        return 180;
    }

    @Override
    public int getLeaseRenewalThreshold() {
        return 90;
    }
}

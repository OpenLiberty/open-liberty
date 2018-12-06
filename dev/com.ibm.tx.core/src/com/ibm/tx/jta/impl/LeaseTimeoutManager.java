package com.ibm.tx.jta.impl;

/*******************************************************************************
 * Copyright (c) 2002, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.ArrayList;

import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.util.alarm.Alarm;
import com.ibm.tx.util.alarm.AlarmListener;
import com.ibm.tx.util.alarm.AlarmManager;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.recoverylog.spi.LibertyRecoveryDirectorImpl;
import com.ibm.ws.recoverylog.spi.RecoveryAgent;
import com.ibm.ws.recoverylog.spi.RecoveryDirector;
import com.ibm.ws.recoverylog.spi.RecoveryDirectorImpl;
import com.ibm.ws.recoverylog.spi.RecoveryFailedException;
import com.ibm.ws.recoverylog.spi.SharedServerLeaseLog;

/**
 * Manage the lease timer. Based on other timers in the codebase.
 * TODO rationalise
 */
public class LeaseTimeoutManager {
    private static final TraceComponent tc = Tr.register(
                                                         LeaseTimeoutManager.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private static TimeoutInfo _info;

    public static void setTimeout(SharedServerLeaseLog leaseLog, String recoveryIdentity, String recoveryGroup, RecoveryAgent recoveryAgent,
                                  RecoveryDirector recoveryDirector,
                                  int seconds) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setTimeout",
                     new Object[] { leaseLog, recoveryIdentity, recoveryAgent, seconds });

        // Stop any existing timeout
        stopTimeout();

        _info = new TimeoutInfo(leaseLog, recoveryIdentity, recoveryGroup, recoveryAgent, recoveryDirector, seconds);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "setTimeout", _info);
    }

    /**
     * This class records information for a timeout for a transaction.
     */
    private static class TimeoutInfo implements AlarmListener {
        protected final SharedServerLeaseLog _leaseLog;
        protected String _recoveryIdentity;
        protected String _recoveryGroup;
        protected RecoveryAgent _recoveryAgent;
        protected RecoveryDirector _recoveryDirector;
        protected final int _duration;

        private Alarm _alarm;

        private final AlarmManager _alarmManager = ConfigurationProviderManager.getConfigurationProvider().getAlarmManager();

        protected TimeoutInfo(SharedServerLeaseLog leaseLog, String recoveryIdentity, String recoveryGroup, RecoveryAgent recoveryAgent, RecoveryDirector recoveryDirector,
                              int duration) {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "TimeoutInfo", leaseLog);

            _leaseLog = leaseLog;
            _duration = duration;
            _recoveryIdentity = recoveryIdentity;
            _recoveryGroup = recoveryGroup;

            _recoveryAgent = recoveryAgent;
            _recoveryDirector = recoveryDirector;

            _alarm = _alarmManager.scheduleAlarm(_duration * 1000l, this, null);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "TimeoutInfo");
        }

        /**
         * Takes appropriate action for a timeout.
         * The entry in the pendingTimeouts hashtable will be removed by
         * the transaction completion code.
         */
        @Override
        public void alarm(Object alarmContext) {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "alarm", _leaseLog);

//            Tr.audit(tc, "WTRN0108I: " +
//                         "Update " + _recoveryIdentity + " lease and check the leases of other servers");
            // Update the lease when we pop
            try {
                if (_leaseLog.lockLocalLease(_recoveryIdentity)) {
                    _leaseLog.updateServerLease(_recoveryIdentity, _recoveryGroup, false);

                    _leaseLog.releaseLocalLease(_recoveryIdentity);
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Could not lock lease for " + _recoveryIdentity);
                }
            } catch (Exception e) {
                //TODO:
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Swallow exception " + e);
            }

            // Check if other servers need recovering
            if (_recoveryAgent != null) {
                ArrayList<String> peersToRecover = _recoveryAgent.processLeasesForPeers(_recoveryIdentity, _recoveryGroup);
                if (_recoveryDirector != null && _recoveryDirector instanceof RecoveryDirectorImpl) {
                    try {
                        ((LibertyRecoveryDirectorImpl) _recoveryDirector).peerRecoverServers(_recoveryAgent, _recoveryIdentity, peersToRecover);
                    } catch (RecoveryFailedException e) {
                        FFDCFilter.processException(e, "com.ibm.tx.jta.impl.LeaseTimeoutManager.alarm", "146", this);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Swallow exception " + e);
                    }
                }
            }

            // Respawn the alarm
            _alarm = _alarmManager.scheduleAlarm(_duration * 1000l, this, null);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "alarm");
        }

        public void cancelAlarm() {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "cancelAlarm", _alarm);

            if (_alarm != null) {
                _alarm.cancel();
                _alarm = null;
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "cancelAlarm");
        }
    }

    /**
     *
     */
    public static void stopTimeout() {
        if (_info != null) {
            _info.cancelAlarm();
        }
    }
}
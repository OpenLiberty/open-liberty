package com.ibm.tx.jta.impl;

/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
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
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.recoverylog.spi.RecoveryAgent;
import com.ibm.ws.recoverylog.spi.RecoveryDirector;
import com.ibm.ws.recoverylog.spi.RecoveryDirectorImpl;
import com.ibm.ws.recoverylog.spi.RecoveryFailedException;
import com.ibm.ws.recoverylog.spi.SharedServerLeaseLog;

public class LeaseTimeoutManager {
    private static final TraceComponent tc = Tr.register(
                                                         LeaseTimeoutManager.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private static String _recoveryIdentity;
    private static String _recoveryGroup;

    private static LeaseRenewer _renewer;
    private static LeaseChecker _checker;

    public static void setTimeouts(SharedServerLeaseLog leaseLog, String recoveryIdentity, String recoveryGroup, RecoveryAgent recoveryAgent, RecoveryDirector recoveryDirector,
                                   int leaseLength, int leaseCheckInterval) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setTimeouts",
                     new Object[] { leaseLog, recoveryIdentity, recoveryGroup, recoveryAgent, recoveryDirector, leaseLength, leaseCheckInterval });

        _recoveryIdentity = recoveryIdentity;
        _recoveryGroup = recoveryGroup;

        _renewer = new LeaseRenewer(leaseLength, leaseLog);
        _checker = new LeaseChecker(leaseCheckInterval, recoveryAgent, recoveryDirector);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setTimeouts");
    }

    private static class LeaseRenewer implements AlarmListener {

        private final SharedServerLeaseLog _leaseLog;
        private Alarm _alarm;

        private LeaseRenewer(int delay, SharedServerLeaseLog leaseLog) {
            _leaseLog = leaseLog;

            schedule(delay);
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.tx.util.alarm.AlarmListener#alarm(java.lang.Object)
         */
        @Override
        public void alarm(Object delay) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "LeaseRenewal",
                         new Object[] { _recoveryIdentity, _recoveryGroup });

            try {
                if (_leaseLog.lockLocalLease(_recoveryIdentity)) {
                    _leaseLog.updateServerLease(_recoveryIdentity, _recoveryGroup, false);

                    _leaseLog.releaseLocalLease(_recoveryIdentity);
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Could not lock lease for " + _recoveryIdentity);
                }
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Swallow exception " + e);
            }

            schedule((int) delay);
        }

        void schedule(int delay) {
            _alarm = ConfigurationProviderManager.getConfigurationProvider().getAlarmManager().scheduleAlarm(delay * 1000l, this, delay);
        }

        void cancel() {
            if (_alarm != null) {
                _alarm.cancel();
                _alarm = null;
            }
        }
    }

    private static class LeaseChecker implements AlarmListener {

        private final RecoveryAgent _recoveryAgent;
        private final RecoveryDirector _recoveryDirector;
        private Alarm _alarm;

        private LeaseChecker(int delay, RecoveryAgent recoveryAgent, RecoveryDirector recoveryDirector) {
            _recoveryAgent = recoveryAgent;
            _recoveryDirector = recoveryDirector;

            schedule(delay);
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.tx.util.alarm.AlarmListener#alarm(java.lang.Object)
         */
        @Override
        public void alarm(Object delay) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "LeaseCheck",
                         new Object[] { _recoveryGroup });

            if (_recoveryAgent != null) {
                ArrayList<String> peersToRecover = _recoveryAgent.processLeasesForPeers(_recoveryIdentity, _recoveryGroup);
                if (_recoveryDirector != null && _recoveryDirector instanceof RecoveryDirectorImpl) {
                    try {
                        ((RecoveryDirectorImpl) _recoveryDirector).peerRecoverServers(_recoveryAgent, _recoveryIdentity, peersToRecover);
                    } catch (RecoveryFailedException e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Swallow exception " + e);
                    }
                }
            }

            schedule((int) delay);
        }

        void schedule(int delay) {
            _alarm = ConfigurationProviderManager.getConfigurationProvider().getAlarmManager().scheduleAlarm(delay * 1000l, this, delay);
        }

        void cancel() {
            if (_alarm != null) {
                _alarm.cancel();
                _alarm = null;
            }
        }
    }

    /**
     *
     */
    public static void stopTimeout() {
        if (_renewer != null) {
            _renewer.cancel();
        }
        if (_checker != null) {
            _checker.cancel();
        }
    }
}
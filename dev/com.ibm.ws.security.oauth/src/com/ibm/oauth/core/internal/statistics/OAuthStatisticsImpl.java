/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.statistics;

import java.util.HashMap;
import java.util.Map;

import com.ibm.oauth.core.api.statistics.OAuthStatistic;
import com.ibm.oauth.core.api.statistics.OAuthStatistics;

public class OAuthStatisticsImpl implements OAuthStatistics {

    Map<String, OAuthStatisticImpl> _stats;

    public OAuthStatisticsImpl() {
        _stats = new HashMap<String, OAuthStatisticImpl>();
    }

    public synchronized OAuthStatistic getCounter(String statName) {
        OAuthStatisticImpl result = null;
        OAuthStatisticImpl currentStat = _stats.get(statName);
        if (currentStat != null) {
            result = new OAuthStatisticImpl(currentStat);
            result.setToNow();
        }
        return result;
    }

    public double getAverageTurnaroundTimeMilliseconds(OAuthStatistic t1,
            OAuthStatistic t2) {
        OAuthStatistic start = t1;
        OAuthStatistic end = t2;
        double result = 0.0;

        /*
         * cater to callers who happen to pass the params in the unintended
         * order
         */
        if (t1.getTimestamp().after(t2.getTimestamp())) {
            start = t2;
            end = t1;
        }

        long totalTxnCount = end.getCount() - start.getCount();
        if (totalTxnCount > 0) {
            long totalElapsedTime = end.getElapsedTime().subtract(
                    start.getElapsedTime()).longValue();
            if (totalElapsedTime > 0) {
                result = ((double) totalElapsedTime) / ((double) totalTxnCount);
            }
        }

        return result;
    }

    public double getTransactionsPerSecond(OAuthStatistic t1, OAuthStatistic t2) {
        OAuthStatistic start = t1;
        OAuthStatistic end = t2;
        double result = 0.0;

        /*
         * cater to callers who happen to pass the params in the unintended
         * order
         */
        if (t1.getTimestamp().after(t2.getTimestamp())) {
            start = t2;
            end = t1;
        }

        long totalTxnCount = end.getCount() - start.getCount();
        if (totalTxnCount > 0) {
            long elapsedTimeMilliseconds = end.getTimestamp().getTime()
                    - start.getTimestamp().getTime();
            if (elapsedTimeMilliseconds > 0) {
                result = ((double) totalTxnCount * 1000.0)
                        / ((double) elapsedTimeMilliseconds);
            }
        }

        return result;
    }

    public synchronized void addMeasurement(OAuthStatHelper statHelper) {
        String statName = statHelper.getName();
        OAuthStatisticImpl statImpl = _stats.get(statName);
        if (statImpl == null) {
            statImpl = new OAuthStatisticImpl(statName);
            _stats.put(statName, statImpl);
        }
        statImpl.addMeasurement(statHelper.getElapsedTime());
    }
}

/*******************************************************************************
 * Copyright (c) 2023,2026 IBM Corporation and others.
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
package com.ibm.ws.test.featurestart;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.ToLongFunction;

public class FeaturesStartTiming {
    public static void logInfo(String m, String msg) {
        FeaturesStartTestBase.logInfo(m, msg);
    }

    // There was a discussion of time-limiting the essential steps.
    // However, on some of the very slow FYRE hardware, these can take
    // several minutes or more.
    //
    // Also, the entire FAT is time-limited.

    public interface TimingAction {
        void run() throws Exception;
    }

    public interface TimingProducer<T> {
        T run() throws Exception;
    }    

    public static class TimingResult {
        public TimingResult(String shortName) {
            this.name = shortName;
            this.bucketResult = initialResults(); 
        }

        //

        public final String name;        

        public String getName() {
            return name;
        }

        //

        public enum TimingBucket {
            UPDATE,
            START,
            PID,
            VERIFY,
            STOP,
            KILL,
            POST
        }

        private static EnumMap<TimingBucket, Long> initialResults() {
            EnumMap<TimingBucket, Long> initialResults = new EnumMap<>(TimingBucket.class);
            Long zeroLong = Long.valueOf(0L);
            for ( TimingBucket timingBucket : TimingBucket.values() ) {
                initialResults.put(timingBucket, zeroLong);
            }
            return initialResults;
        }

        protected final EnumMap<TimingBucket, Long> bucketResult;

        public Long put(TimingBucket timingBucket, Long deltaNs) {
            return bucketResult.put(timingBucket, deltaNs);
        }

        public Long get(TimingBucket timingBucket) {
            return bucketResult.get(timingBucket);
        }

        public long getTotalNs() {
            long totalNs = 0;
            for ( Long result : bucketResult.values() ) {
                totalNs += result;
            }
            return totalNs;
        }

        //

        public long getTimeNs() {
            return System.nanoTime();
        }

        public long getTimeNs(long initialNs) {
            return getTimeNs() - initialNs;
        }

        //

        public Exception runUpdate(TimingAction action) {
            return run(TimingBucket.UPDATE, action);
        }

        public Long getUpdateNs() {
            return get(TimingBucket.UPDATE);
        }

        public Exception runStart(TimingAction action) {
            return run(TimingBucket.START, action);
        }

        public Long getStartNs() {
            return get(TimingBucket.START);
        }        

        public Exception runVerify(TimingAction action) {
            return run(TimingBucket.VERIFY, action);
        }

        public Long getVerifyNs() {
            return get(TimingBucket.VERIFY);
        }        

        public String runPid(TimingProducer<String> action) throws Exception {
            return runProducer(TimingBucket.PID, action);
        }

        public Long getPidNs() {
            return get(TimingBucket.PID);
        }        

        public Exception runStop(TimingAction action) {
            return run(TimingBucket.STOP, action);
        }

        public Long getStopNs() {
            return get(TimingBucket.STOP);
        }        

        public Exception runKill(TimingAction action) {
            return run(TimingBucket.KILL, action);
        }        

        public Long getKillNs() {
            return get(TimingBucket.KILL);
        }        

        public Exception runPost(TimingAction action) {
            return run(TimingBucket.POST, action);
        }
        
        public Long getPostNs() {
            return get(TimingBucket.POST);
        }

        public <T> T runProducer(TimingBucket bucket, TimingProducer<T> action) throws Exception {            
            long initialNs = getTimeNs();
            try {
                return action.run();
            } finally {
                long deltaNs = getTimeNs(initialNs);
                put(bucket, deltaNs); 
            }
        }

        public Exception run(TimingBucket bucket, TimingAction action) {            
            long initialNs = getTimeNs();
            try {
                action.run();
                return null;
            } catch ( Exception e ) {
                return e;
            } finally {
                long deltaNs = getTimeNs(initialNs);
                put(bucket, deltaNs); 
            }
        }

        //

        public void display(String m) {
            StringBuilder builder = new StringBuilder();

            builder.append("Feature [ " + getName() + " ]:");
            logInfo(m, builder.toString());
            builder.setLength(0);
            
            builder.append("    ");

            builder.append(format("Update", getUpdateNs()));
            builder.append(", ");

            builder.append(format("Start", getStartNs()));
            builder.append(", ");

            builder.append(format("PID", getPidNs()));
            builder.append(", ");

            builder.append(format("Verify", getVerifyNs()));
            builder.append(',');

            logInfo(m, builder.toString());
            builder.setLength(0);

            builder.append("    ");

            builder.append(format("Stop", getStopNs()));
            builder.append(", ");

            builder.append(format("Kill", getKillNs()));
            builder.append(", ");
            
            builder.append(format("Post", getPostNs()));
            builder.append(", ");            

            logInfo(m, builder.toString());
            builder.setLength(0);
            
            builder.append("    ");

            builder.append(format("Total", getTotalNs()));

            logInfo(m, builder.toString());
            builder.setLength(0);            
        }
    }

    public static class TimingSummary {
        public final String description;

        public TimingSummary(String description) {
            this.description = description;
        }

        public int count = 0;
        public long sum = 0L;
        public long avg = UNSET_NS;

        public long min = UNSET_NS;
        public String minShort = null;

        public long max = UNSET_NS;
        public String maxShort = null;
    }

    public static TimingSummary statistics(String description, Map<String, TimingResult> timingResults, ToLongFunction<TimingResult> producer) {
        TimingSummary summary = new TimingSummary(description);

        for (Map.Entry<String, TimingResult> resultEntry : timingResults.entrySet()) {
            String shortName = resultEntry.getKey();
            TimingResult result = resultEntry.getValue();
            long stat = producer.applyAsLong(result);

            if (stat == UNSET_NS) {
                continue;
            }

            summary.count++;
            summary.sum += stat;

            if ((summary.min == UNSET_NS) || (stat < summary.min)) {
                summary.min = stat;
                summary.minShort = shortName;
            }
            if ((summary.max == UNSET_NS) || (stat > summary.max)) {
                summary.max = stat;
                summary.maxShort = shortName;
            }
        }

        if (summary.count != 0) {
            summary.avg = summary.sum / summary.count;
        }

        return summary;
    }

    public static final long NS_IN_SEC = 1000000000;
    public static final long UNSET_NS = -1L;

    public static long sum(long... toAdd) {
        long sum = 0L;
        for (long nextToAdd : toAdd) {
            if (nextToAdd != UNSET_NS) {
                sum += nextToAdd;
            }
        }
        return sum;
    }

    public static final String nsAsSec(long ns) {
        return String.format("%.4f", Float.valueOf(((float) ns) / NS_IN_SEC));
    }

    public static String format(String description, long ns) {
        String nsText;
        if (ns == UNSET_NS) {
            nsText = "**UNSET**";
        } else {
            nsText = nsAsSec(ns);
        }

        return description + " [ " + nsText + " ]";
    }

    public static String formatStat(String description, long stat, String shortName) {
        if (stat == UNSET_NS) {
            return description + " [ **UNSET** ]";
        } else {
            return description + " [ " + nsAsSec(stat) + " ] ( " + shortName + " )";
        }
    }        
}

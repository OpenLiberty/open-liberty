/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package com.ibm.ws.kernel.service.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * API for getting cpu info about the system
 */
public class CpuInfo {

    /**
     * Trace component for this class.
     */
    private final static TraceComponent tc = Tr.register(CpuInfo.class);

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("Liberty-kernel-CpuInfo");
            t.setDaemon(true);
            return t;
        }
    });
    private final static CpuInfo INSTANCE = new CpuInfo();

    // the integer form of AvailableProcessors is returned by getAvailableProcessors()
    private final AtomicInteger AVAILABLE_PROCESSORS_INTEGER = new AtomicInteger(-1);
    // The float form of AvailableProcessors is used to calculate JavaCpuUsage
    // because some environments (e.g. containers) support fractional cpu allocation.
    // Since there is no AtomicFloat, we simulate one by multiplying by 100 before 'set'
    // and dividing by 100.0 after 'get'
    private final AtomicInteger AVAILABLE_PROCESSORS_FLOAT = new AtomicInteger(-1);
    private final CPUCount cpuCount;
    // For CPU usage calculation
    // Initialized lazily to avoid CPU usage during startup.
    private CpuInfoAccessor osmx;
    private final int cpuNSFactor;
    private long lastProcessCPUTime = 0;
    private double lastProcessCpuUsage = -1;
    private long lastSystemTimeMillis = -1;
    private final IntervalTask activeTask;

    private static final long INTERVAL = 10; // in minutes
    private static final int CHECKPOINT_CPUS = 4;
    private static Collection<AvailableProcessorsListener> listeners = Collections.synchronizedCollection(new HashSet<AvailableProcessorsListener>());

    private CpuInfo() {
        activeTask = new IntervalTask();
        executor.scheduleAtFixedRate(activeTask, INTERVAL, INTERVAL, TimeUnit.MINUTES);
        cpuCount = new CPUCount();
        int runtimeAvailableProcessors = Runtime.getRuntime().availableProcessors();
        float fileSystemAvailableProcessors = getAvailableProcessorsFromFilesystemFloat();

        if (fileSystemAvailableProcessors <= 0 || fileSystemAvailableProcessors > runtimeAvailableProcessors) {
            AVAILABLE_PROCESSORS_INTEGER.set(runtimeAvailableProcessors);
            AVAILABLE_PROCESSORS_FLOAT.set(runtimeAvailableProcessors * 100);
        } else {
            AVAILABLE_PROCESSORS_INTEGER.set(roundUpToNextInt(fileSystemAvailableProcessors));
            AVAILABLE_PROCESSORS_FLOAT.set((int) (fileSystemAvailableProcessors * 100));
        }

        CheckpointPhase phase = CheckpointPhase.getPhase();
        if (phase != CheckpointPhase.INACTIVE) {
            phase.addMultiThreadedHook(activeTask);
            if (AVAILABLE_PROCESSORS_INTEGER.get() > CHECKPOINT_CPUS) {
                // set processors to a low number if we're doing a checkpoint to reduce restore times due to potential high number of minimum threads
                AVAILABLE_PROCESSORS_INTEGER.set(CHECKPOINT_CPUS);
                AVAILABLE_PROCESSORS_FLOAT.set(CHECKPOINT_CPUS * 100);
            }
        }
        int nsFactor = 1;
        // Adjust for J9 cpuUsage units change from hundred-nanoseconds to nanoseconds in IBM Java8sr5
        //
        // Cannot use JavaInfo.vendor() == JavaInfo.Vendor.IBM because that returns true on Semeru 8 as well
        // and the format of the string is 1.8.0_xxx-bxx so it will comes out as 8.0.0 so we will
        // erroneously think it is less than Java 8 SR5.  So using this isSystemClassAvailable check for
        // a class that is available on IBM Java 8, but not Semeru 8.
        if (JavaInfo.isSystemClassAvailable("com.ibm.security.auth.module.Krb5LoginModule")) {
            int majorVersion = JavaInfo.majorVersion();
            int minorVersion = JavaInfo.minorVersion();
            int serviceRelease = JavaInfo.serviceRelease();
            if (majorVersion == 8 && minorVersion == 0 && serviceRelease < 5) {
                nsFactor = 100;
            }

            if (tc.isEventEnabled()) {
                Tr.event(tc, "IBM Java level check", ("majorVersion: " + majorVersion + ", minorVersion: " + minorVersion +
                                                      ", serviceRelease: " + serviceRelease + ", cpuNSFactor: " + nsFactor));
            }
        }
        cpuNSFactor = nsFactor;
    }

    private synchronized double getSystemCPU() {
        double cpuUsage = -1;

        // Get the system cpu usage
        try {
            if (osmx == null) {
                osmx = createCpuInfoAccessor();
            }
            cpuUsage = osmx.getSystemCpuLoad();
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getSystemCPU");
        }
        // if we get back a negative value, it means cpuUsage is not available
        // otherwise, normalize to present as percentage
        if (cpuUsage >= 0) {
            cpuUsage *= 100;
            if (cpuUsage > 100) {
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "getSystemCPU error", ("system CPU out-of-range: " + cpuUsage));
                }
                cpuUsage = -1;
            }
        }

        return cpuUsage;
    }

    private synchronized double getProcessCPU() {
        // update process cpu usage at most once every 500 ms
        long currentTimeMs = System.currentTimeMillis();
        if (currentTimeMs - lastSystemTimeMillis < 500)
            return lastProcessCpuUsage;

        double cpuUsage = -1;
        long processCpuTime = -1;
        // Get the CPU time from the mbean
        try {
            if (osmx == null) {
                osmx = createCpuInfoAccessor();
            }
            processCpuTime = osmx.getProcessCpuTime();
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getProcessCPU");
        }

        if (processCpuTime != -1) {
            // processCpuTime is in nanos, so need to convert from millis
            long d1 = (currentTimeMs - lastSystemTimeMillis) * 1000000;
            long d2 = processCpuTime - lastProcessCPUTime;
            cpuUsage = (double) d2 / d1;
            cpuUsage = (cpuUsage / (AVAILABLE_PROCESSORS_FLOAT.floatValue() / 100.0)) * cpuNSFactor * 100;

            lastSystemTimeMillis = currentTimeMs;
            lastProcessCPUTime = processCpuTime;
        }

        if (cpuUsage > 100) {
            // This may not be an error - some virtualized systems allow process cpu to exceed
            // 100% in some cases. So we will return the cpuUsage as reported to us, and provide
            // the trace event so this can be observed for diagnosis if the need arises.
            if (tc.isEventEnabled()) {
                Tr.event(tc, "getProcessCPU anomaly", ("process CPU out-of-range: " + cpuUsage));
            }
        }
        lastProcessCpuUsage = cpuUsage;
        return cpuUsage;
    }

    // utility below parses cpu limits info from Docker files
    private static float getAvailableProcessorsFromFilesystemFloat() {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        float availableProcessorsFloat = -1;

        //Check for docker files
        String periodFileLocation = File.separator + "sys" + File.separator + "fs" + File.separator + "cgroup" + File.separator + "cpu" + File.separator + "cpu.cfs_period_us";
        String quotaFileLocation = File.separator + "sys" + File.separator + "fs" + File.separator + "cgroup" + File.separator + "cpu" + File.separator + "cpu.cfs_quota_us";
        File cfsPeriod = new File(periodFileLocation);
        File cfsQuota = new File(quotaFileLocation);
        if (cfsPeriod.exists() && cfsQuota.exists()) { //Found docker files
            //Read quota
            try {
                String quotaContents = readFile(cfsQuota);
                float quotaFloat = Float.parseFloat(quotaContents);
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "quotaFloat = " + quotaFloat);
                if (quotaFloat >= 0) {
                    //Read period
                    String periodContents = readFile(cfsPeriod);
                    float periodFloat = Float.parseFloat(periodContents);
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "periodFloat = " + periodFloat);
                    if (periodFloat != 0) {
                        availableProcessorsFloat = quotaFloat / periodFloat;
                        availableProcessorsFloat = roundToTwoDecimalPlaces(availableProcessorsFloat);
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Calculated availableProcessors: " + availableProcessorsFloat + ". period=" + periodFloat + ", quota=" + quotaFloat);
                    }
                }
            } catch (Throwable e) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Caught exception: " + e.getMessage() + ". Using number of processors reported by java");
                availableProcessorsFloat = -1;
            }
        } else {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "Files " + quotaFileLocation + " : " + cfsQuota.exists());
                Tr.debug(tc, "Files " + periodFileLocation + " : " + cfsPeriod.exists());
            }
        }

        return availableProcessorsFloat;
    }

    private static String readFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        BufferedReader buf = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line = buf.readLine();
        StringBuilder sb = new StringBuilder();
        while (line != null) {
            sb.append(line).append("\n");
            line = buf.readLine();
        }

        buf.close();
        is.close();
        return sb.toString();
    }

    private static float roundToTwoDecimalPlaces(float f) {
        BigDecimal bd = new BigDecimal(f);
        bd = bd.setScale(2, RoundingMode.DOWN);
        return bd.floatValue();
    }

    private static int roundUpToNextInt(float f) {
        BigDecimal bd = new BigDecimal(f);
        bd = bd.setScale(0, RoundingMode.UP);
        return bd.intValue();
    }

    /**
     * Returns the number of hardware threads (aka cpus) available to this Java process
     *
     * @return int available processors
     */
    public static CPUCount getAvailableProcessors() {
        return INSTANCE.cpuCount;
    }

    /**
     * Returns the cpu usage by this Java process in the last interval
     *
     * @return double process cpu usage (returns -1 if info not available)
     */
    public static double getJavaCpuUsage() {
        return INSTANCE.getProcessCPU();
    }

    /**
     * Returns the cpu usage on the system (all cpus, all processes) in the last interval
     *
     * @return double system cpu usage (returns -1 if info not available)
     */
    public static double getSystemCpuUsage() {
        return INSTANCE.getSystemCPU();
    }

    public static void addAvailableProcessorsListener(AvailableProcessorsListener listener) {
        listeners.add(listener);
    }

    public static void removeAvailableProcessorsListener(AvailableProcessorsListener listener) {
        listeners.remove(listener);
    }

    public static CpuInfoAccessor createCpuInfoAccessor() {
        java.lang.management.OperatingSystemMXBean mbean = ManagementFactory.getOperatingSystemMXBean();

        if (mbean == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning NullCpuInfoAccessor");
            }
            return new NullCpuInfoAccessor();
        }
        try {
            if (JavaInfo.isSystemClassAvailable("com.ibm.lang.management.OperatingSystemMXBean")) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Returning IBMJavaCpuInfoAccessor");
                }
                return new IBMJavaCpuInfoAccessor(mbean);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning ModernJavaCpuInfoAccessor");
            }
            return new ModernJavaCpuInfoAccessor(mbean);

        } catch (NoClassDefFoundError e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning StandardAPICpuInfoAccessor");
            }
            return new StandardAPICpuInfoAccessor(mbean);
        }

    }

    private static interface CpuInfoAccessor {
        public long getProcessCpuTime();

        public double getSystemCpuLoad();
    }

    private static class NullCpuInfoAccessor implements CpuInfoAccessor {
        @Override
        public long getProcessCpuTime() {
            return -1;
        }

        @Override
        public double getSystemCpuLoad() {
            return -1;
        }
    }

    private static class IBMJavaCpuInfoAccessor implements CpuInfoAccessor {
        private final com.ibm.lang.management.OperatingSystemMXBean mbean;

        public IBMJavaCpuInfoAccessor(java.lang.management.OperatingSystemMXBean jvmMbean) {
            mbean = (com.ibm.lang.management.OperatingSystemMXBean) jvmMbean;
        }

        @Override
        public long getProcessCpuTime() {
            return mbean.getProcessCpuTime();
        }

        @Override
        public double getSystemCpuLoad() {
            return mbean.getSystemCpuLoad();
        }
    }

    private static class ModernJavaCpuInfoAccessor implements CpuInfoAccessor {
        private final com.sun.management.OperatingSystemMXBean mbean;

        public ModernJavaCpuInfoAccessor(java.lang.management.OperatingSystemMXBean jvmMbean) {
            mbean = (com.sun.management.OperatingSystemMXBean) jvmMbean;
        }

        @Override
        public long getProcessCpuTime() {
            return mbean.getProcessCpuTime();
        }

        @Override
        public double getSystemCpuLoad() {
            return mbean.getSystemCpuLoad();
        }
    }

    private static class StandardAPICpuInfoAccessor implements CpuInfoAccessor {
        private final ObjectName objectName;
        private final MBeanServer mBeanServer;

        public StandardAPICpuInfoAccessor(java.lang.management.OperatingSystemMXBean jvmMbean) {
            objectName = jvmMbean.getObjectName();
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        }

        @Override
        public long getProcessCpuTime() {
            try {
                return (Long) mBeanServer.getAttribute(objectName, "ProcessCpuTime");
            } catch (Exception e) {
                return -1L;
            }
        }

        @Override
        public double getSystemCpuLoad() {
            try {
                return (Double) mBeanServer.getAttribute(objectName, "SystemCpuLoad");
            } catch (Exception e) {
                return -1;
            }
        }
    }

    private static long lastChecked = System.currentTimeMillis();

    private static final long CATCH_UP_INTERVAL = 30000;

    /**
     * Timer task that queries available process cpus.
     */
    class IntervalTask implements Runnable, CheckpointHook {

        @Override
        // In checkpoint mode force initialization of osmx variable to complete
        // prior to checkpoint dump. (this avoids possible deadlock on restore).
        public void prepare() {
            synchronized (CpuInfo.this) {
                try {
                    if (osmx == null) {
                        osmx = createCpuInfoAccessor();
                    }
                } catch (Exception e) {
                    // Fail the checkpoint to avoid possible deadlock on restore
                    FFDCFilter.processException(e, getClass().getName(), "prepare()");
                    throw new RuntimeException("e.getMessage()", e);
                }
            }
        }

        @Override
        public void restore() {
            run();
        }

        @Override
        public void run() {
            long current = System.currentTimeMillis();
            if (current - lastChecked < CATCH_UP_INTERVAL) {
                // on restore, we can get called back successively for every missed interval
                // avoid the penalty of checking the Runtime in these cases
                lastChecked = current;
                return;
            }

            // find available processors
            int runtimeAvailableProcessors = Runtime.getRuntime().availableProcessors();
            float fileSystemAvailableProcessors = getAvailableProcessorsFromFilesystemFloat();

            int newAvailableProcessorsInt;
            int newAvailableProcessorsFloat;
            if (fileSystemAvailableProcessors <= 0 || fileSystemAvailableProcessors > runtimeAvailableProcessors) {
                newAvailableProcessorsInt = runtimeAvailableProcessors;
                newAvailableProcessorsFloat = runtimeAvailableProcessors * 100;
            } else {
                newAvailableProcessorsInt = roundUpToNextInt(fileSystemAvailableProcessors);
                newAvailableProcessorsFloat = (int) (fileSystemAvailableProcessors * 100);
            }

            int currentNumberOfProcessorsInt = AVAILABLE_PROCESSORS_INTEGER.get();
            int currentNumberOfProcessorsFloat = AVAILABLE_PROCESSORS_FLOAT.get();

            if (currentNumberOfProcessorsFloat != newAvailableProcessorsFloat) {
                AVAILABLE_PROCESSORS_FLOAT.set(newAvailableProcessorsFloat);
            }
            if (currentNumberOfProcessorsInt != newAvailableProcessorsInt) {
                if (AVAILABLE_PROCESSORS_INTEGER.compareAndSet(currentNumberOfProcessorsInt, newAvailableProcessorsInt)) {
                    notifyListeners(newAvailableProcessorsInt);
                }
            }

            lastChecked = System.currentTimeMillis();
        }

        public void notifyListeners(int processors) {
            // copy the set of listeners to call.
            // 1 We don't want to hold a lock while calling the listeners
            // 2 We need to avoid iterating over listeners collection while calling the listeners because they
            //   may get removed as part of calling them, causing a ConcurrentModificationException
            Collection<AvailableProcessorsListener> listenersCopy = new ArrayList<>(listeners);
            for (AvailableProcessorsListener listener : listenersCopy) {
                try {
                    listener.setAvailableProcessors(processors);
                } catch (Throwable t) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Caught exception: " + t.getMessage() + ".");
                    FFDCFilter.processException(t, getClass().getName(), "notifyListeners");
                }
            }
        }
    }

    @Trivial
    public class CPUCount {
        public int get() {
            return AVAILABLE_PROCESSORS_INTEGER.get();
        }

        @Override
        public String toString() {
            return Integer.toString(AVAILABLE_PROCESSORS_INTEGER.get());
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    private final AtomicInteger AVAILABLE_PROCESSORS = new AtomicInteger(-1);
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
    private static Collection<AvailableProcessorsListener> listeners = Collections.synchronizedCollection(new HashSet<AvailableProcessorsListener>());

    private CpuInfo() {
        activeTask = new IntervalTask();
        executor.scheduleAtFixedRate(activeTask, INTERVAL, INTERVAL, TimeUnit.MINUTES);
        cpuCount = new CPUCount();
        CheckpointPhase phase = CheckpointPhase.getPhase();
        if (phase != CheckpointPhase.INACTIVE) {
            phase.addMultiThreadedHook(activeTask);
        }
        int runtimeAvailableProcessors = Runtime.getRuntime().availableProcessors();
        int fileSystemAvailableProcessors = getAvailableProcessorsFromFilesystem();

        if (fileSystemAvailableProcessors <= 0 || fileSystemAvailableProcessors > runtimeAvailableProcessors) {
            AVAILABLE_PROCESSORS.set(runtimeAvailableProcessors);
        } else {
            AVAILABLE_PROCESSORS.set(fileSystemAvailableProcessors);
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
            cpuUsage = (cpuUsage / AVAILABLE_PROCESSORS.get()) * cpuNSFactor * 100;

            lastSystemTimeMillis = currentTimeMs;
            lastProcessCPUTime = processCpuTime;
        }

        if (cpuUsage > 100) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "getProcessCPU error", ("process CPU out-of-range: " + cpuUsage));
            }
            cpuUsage = -1;
        }
        lastProcessCpuUsage = cpuUsage;
        return cpuUsage;
    }

    // utility below parses cpu limits info from Docker files
    private static int getAvailableProcessorsFromFilesystem() {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        Double availableProcessorsDouble = null;
        int availableProcessorsInt = -1;

        //Check for docker files
        String periodFileLocation = File.separator + "sys" + File.separator + "fs" + File.separator + "cgroup" + File.separator + "cpu" + File.separator + "cpu.cfs_period_us";
        String quotaFileLocation = File.separator + "sys" + File.separator + "fs" + File.separator + "cgroup" + File.separator + "cpu" + File.separator + "cpu.cfs_quota_us";
        File cfsPeriod = new File(periodFileLocation);
        File cfsQuota = new File(quotaFileLocation);
        if (cfsPeriod.exists() && cfsQuota.exists()) { //Found docker files
            //Read quota
            try {
                String quotaContents = readFile(cfsQuota);
                double quotaFloat = new Double(quotaContents);
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "quotaFloat = " + quotaFloat);
                if (quotaFloat >= 0) {
                    //Read period
                    String periodContents = readFile(cfsPeriod);
                    double periodFloat = new Double(periodContents);
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "periodFloat = " + periodFloat);
                    if (periodFloat != 0) {
                        availableProcessorsDouble = quotaFloat / periodFloat;
                        availableProcessorsDouble = roundToTwoDecimalPlaces(availableProcessorsDouble);
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Calculated availableProcessors: " + availableProcessorsDouble + ". period=" + periodFloat + ", quota=" + quotaFloat);
                    }
                }
            } catch (Throwable e) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Caught exception: " + e.getMessage() + ". Using number of processors reported by java");
                availableProcessorsDouble = null;
            }
        } else {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "Files " + quotaFileLocation + " : " + cfsQuota.exists());
                Tr.debug(tc, "Files " + periodFileLocation + " : " + cfsPeriod.exists());
            }
        }

        availableProcessorsInt = (availableProcessorsDouble == null) ? -1 : availableProcessorsDouble.intValue();

        // make sure any z.xy cpu quota was not rounded down (especially to 0 ...) during int conversion
        if (availableProcessorsDouble != null && availableProcessorsDouble > availableProcessorsInt)
            availableProcessorsInt++;

        return availableProcessorsInt;
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

    private static Double roundToTwoDecimalPlaces(Double d) {
        BigDecimal bd = new BigDecimal(d);
        bd = bd.setScale(2, RoundingMode.DOWN);
        return bd.doubleValue();
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
            int fileSystemAvailableProcessors = getAvailableProcessorsFromFilesystem();

            int newAvailableProcessors;
            if (fileSystemAvailableProcessors <= 0 || fileSystemAvailableProcessors > runtimeAvailableProcessors) {
                newAvailableProcessors = runtimeAvailableProcessors;
            } else {
                newAvailableProcessors = fileSystemAvailableProcessors;
            }

            int currentNumberOfProcessors = AVAILABLE_PROCESSORS.get();
            if (currentNumberOfProcessors != newAvailableProcessors) {
                if (AVAILABLE_PROCESSORS.compareAndSet(currentNumberOfProcessors, newAvailableProcessors)) {
                    notifyListeners(newAvailableProcessors);
                }
            }
            lastChecked = System.currentTimeMillis();
        }

        public void notifyListeners(int processors) {
            synchronized (listeners) {
                for (AvailableProcessorsListener listener : listeners) {
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
    }

    @Trivial
    public class CPUCount {
        public int get() {
            return AVAILABLE_PROCESSORS.get();
        }

        @Override
        public String toString() {
            return Integer.toString(AVAILABLE_PROCESSORS.get());
        }
    }
}

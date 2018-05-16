/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.util;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Provides information about the underlying operating system. This class might
 * be used in things like FAT suites, so avoid using Tr.
 */
public class MemoryInformation {
    /**
     * Get the singleton for the API.
     * By default:
     * 1) Cache total RAM after first calculation,
     * 2) Use the heavy weight but more accurate method to calculate available RAM.
     *
     * @return Singleton to access memory information.
     */
    public static MemoryInformation instance() {
        return instance;
    }

    /**
     * Get the total number of bytes of visible RAM. If there are issues
     * trying to determine this number, a {@code MemoryInformationException}
     * is thrown.
     *
     * @return Bytes of visible RAM.
     * @throws MemoryInformationException Error retrieving information.
     */
    public synchronized long getTotalMemory() throws MemoryInformationException {
        if (cacheTotalRam && cachedTotalRam != -1) {

            return cachedTotalRam;
        }

        long result = -1;

        // First we try the JDK since it's probably most efficient
        try {
            result = getTotalMemoryJDK();
        } catch (MemoryInformationException e) {
            // It's okay that this fails since we'll fall back to the switch below.
        }

        if (result == -1) {
            switch (OperatingSystem.instance().getOperatingSystemType()) {
                case Linux:
                    result = getTotalMemoryLinux();
                    break;
                case AIX:
                    result = getTotalMemoryAix();
                    break;
                case Mac:
                    result = getTotalMemoryMac();
                    break;
                case Windows:
                    result = getTotalMemoryWindows();
                    break;
                case HPUX:
                    result = getTotalMemoryHp();
                    break;
                case IBMi:
                    result = getTotalMemoryIBMi();
                    break;
                case Solaris:
                    result = getTotalMemorySolaris();
                    break;
                case zOS:
                    result = getTotalMemoryzOS();
                    break;
                default:
                    throw getNotImplementedException();
            }
        }

        if (cacheTotalRam) {
            cachedTotalRam = result;
        }
        return result;
    }

    /**
     * Get an estimate of the number of bytes that can be made available for
     * application usage (e.g. minus transient file cache in RAM). It should
     * be assumed that this is an expensive operation.
     *
     * @return Bytes of available RAM.
     * @throws MemoryInformationException Error retrieving information.
     */
    public long getAvailableMemory() throws MemoryInformationException {

        // Unlike getTotalMemory, we don't just ask the JDK because
        // its calculation is very naïve and doesn't take into account
        // transient caches. Unless the user explicitly wants this.

        if (useLightweightAvailableRam) {
            try {
                return getFreeMemoryJDK();
            } catch (MemoryInformationException e) {
                // It's okay that this fails since we'll fall back to the switch below.
            }
        }

        switch (OperatingSystem.instance().getOperatingSystemType()) {
            case Linux:
                return getAvailableMemoryLinux();
            case AIX:
                return getAvailableMemoryAix();
            case Mac:
                return getAvailableMemoryMac();
            case Windows:
                return getAvailableMemoryWindows();
            case HPUX:
                return getAvailableMemoryHp();
            case IBMi:
                return getAvailableMemoryIBMi();
            case Solaris:
                return getAvailableMemorySolaris();
            case zOS:
                return getAvailableMemoryzOS();
            default:
                throw getNotImplementedException();
        }
    }

    /**
     * Get a ratio of an estimate of the number of bytes that can be made
     * available for application usage (e.g. minus transient file cache in RAM)
     * to the total number of bytes of visible RAM. It should be assumed that
     * this is an expensive operation.
     *
     * @return Ratio of available RAM to total RAM.
     * @throws MemoryInformationException Error retrieving information.
     */
    public float getAvailableMemoryRatio() throws MemoryInformationException {
        long totalMemory = getTotalMemory();
        long availableMemory = getAvailableMemory();
        return (float) ((double) availableMemory / (double) totalMemory);
    }

    /**
     * Create a new instance of the API.
     * By default:
     * 1) Cache total RAM after first calculation,
     * 2) Use the heavy weight but more accurate method to calculate available RAM.
     */
    public MemoryInformation() {
        this(true, false);
    }

    /**
     * Create a new instance of the API with various options.
     *
     * @param cacheTotalRam
     *            Cache total RAM after first calculation.
     * @param useLightweightAvailableRam
     *            Use the less accurate but more lightweight method to calculate available RAM.
     */
    public MemoryInformation(boolean cacheTotalRam, boolean useLightweightAvailableRam) {
        this.cacheTotalRam = cacheTotalRam;
        this.useLightweightAvailableRam = useLightweightAvailableRam;
    }

    public static final long BYTES_IN_KB = 1024L;
    public static final long BYTES_IN_MB = 1048576L;
    public static final long BYTES_IN_GB = 1073741824L;
    public static final long BYTES_IN_TB = 1099511627776L;
    public static final long BYTES_IN_PB = 1125899906842624L;
    public static final long BYTES_IN_EB = 1152921504606846976L;

    public static long inferBytes(String line) throws MemoryInformationException {
        long modifier = 1;
        line = line.toLowerCase();
        if (line.contains("kb")) {
            modifier = BYTES_IN_KB;
            line = line.replaceAll("kb", "");
        } else if (line.contains("kilobytes")) {
            modifier = BYTES_IN_KB;
            line = line.replaceAll("kilobytes", "");
        } else if (line.contains("mb")) {
            modifier = BYTES_IN_MB;
            line = line.replaceAll("mb", "");
        } else if (line.contains("megabytes")) {
            modifier = BYTES_IN_MB;
            line = line.replaceAll("megabytes", "");
        } else if (line.contains("gb")) {
            modifier = BYTES_IN_GB;
            line = line.replaceAll("gb", "");
        } else if (line.contains("gigabytes")) {
            modifier = BYTES_IN_GB;
            line = line.replaceAll("gigabytes", "");
        } else if (line.contains("tb")) {
            modifier = BYTES_IN_TB;
            line = line.replaceAll("tb", "");
        } else if (line.contains("terabytes")) {
            modifier = BYTES_IN_TB;
            line = line.replaceAll("terabytes", "");
        } else if (line.contains("pb")) {
            modifier = BYTES_IN_PB;
            line = line.replaceAll("pb", "");
        } else if (line.contains("petabytes")) {
            modifier = BYTES_IN_PB;
            line = line.replaceAll("petabytes", "");
        } else if (line.contains("eb")) {
            modifier = BYTES_IN_EB;
            line = line.replaceAll("eb", "");
        } else if (line.contains("exabytes")) {
            modifier = BYTES_IN_EB;
            line = line.replaceAll("exabytes", "");
        } else {
            line = line.replaceAll("b", "");
            line = line.replaceAll("bytes", "");
        }
        return Long.parseLong(line.trim()) * modifier;
    }

    private final boolean cacheTotalRam;
    private long cachedTotalRam = -1;
    private final boolean useLightweightAvailableRam;

    private final static MemoryInformation instance = new MemoryInformation();
    private static final Pattern spacePattern = Pattern.compile("\\s+");

    private MemoryInformationException getNotImplementedException() {
        return new MemoryInformationException("Unimplemented operating system " + OperatingSystem.instance().getOperatingSystemType() +
                                              " (" + System.getProperty("os.name") + ")");
    }

    private long getTotalMemoryLinux() throws MemoryInformationException {
        try {
            // https://www.kernel.org/doc/Documentation/filesystems/proc.txt
            List<String> lines = OperatingSystem.readAllLines("/proc/meminfo");
            for (String meminfoLine : lines) {
                if (meminfoLine.startsWith("MemTotal:")) {
                    // MemTotal: Total usable ram (i.e. physical ram minus a few reserved
                    // bits and the kernel binary code)
                    return parseLinuxMeminfoLine(meminfoLine);
                }
            }
            throw new MemoryInformationException("Expected memory information missing in /proc/meminfo: " + lines);
        } catch (IOException e) {
            throw new MemoryInformationException(e);
        }
    }

    private long parseLinuxMeminfoLine(String line) throws MemoryInformationException {
        line = line.substring(line.indexOf(':') + 1).trim();
        int i = line.lastIndexOf(' ');
        int modifier = 1;
        if (i != -1) {
            String modifierString = line.substring(i + 1);
            line = line.substring(0, i);
            if (modifierString.equals("kB")) {
                modifier = 1024;
            } else {
                throw new MemoryInformationException("Unknown meminfo modifier " + modifierString);
            }
        }
        return Long.parseLong(line) * modifier;
    }

    private long getAvailableMemoryLinux() throws MemoryInformationException {
        try {
            // https://www.kernel.org/doc/Documentation/filesystems/proc.txt

            // Newer versions of Linux have a MemAvailable line so we can just
            // return that immediately if we find it. Otherwise, we figure it
            // out from other lines.

            // https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/commit/?id=34e431b0ae398fc54ea69ff85ec700722c9da773

            long fileCache = 0;
            long slabReclaimable = 0;
            long buffers = 0;
            long cached = 0;
            long free = 0;
            List<String> lines = OperatingSystem.readAllLines("/proc/meminfo");
            for (String meminfoLine : lines) {
                if (meminfoLine.startsWith("MemAvailable:")) {
                    // MemAvailable: An estimate of how much memory is available for starting new
                    // applications, without swapping. Calculated from MemFree,
                    // SReclaimable, the size of the file LRU lists, and the low
                    // watermarks in each zone.
                    // The estimate takes into account that the system needs some
                    // page cache to function well, and that not all reclaimable
                    // slab will be reclaimable, due to items being in use. The
                    // impact of those factors will vary from system to system.

                    return parseLinuxMeminfoLine(meminfoLine);
                } else if (meminfoLine.startsWith("MemFree:")) {
                    // MemFree: The sum of LowFree+HighFree
                    free = parseLinuxMeminfoLine(meminfoLine);
                } else if (meminfoLine.startsWith("Cached:")) {
                    // Cached: in-memory cache for files read from the disk (the
                    // pagecache). Doesn't include SwapCached
                    cached = parseLinuxMeminfoLine(meminfoLine);
                } else if (meminfoLine.startsWith("Buffers:")) {
                    buffers = parseLinuxMeminfoLine(meminfoLine);
                } else if (meminfoLine.startsWith("SReclaimable:")) {
                    // SReclaimable: Part of Slab, that might be reclaimed, such as caches
                    slabReclaimable = parseLinuxMeminfoLine(meminfoLine);
                } else if (meminfoLine.startsWith("Active(file):")) {
                    fileCache += parseLinuxMeminfoLine(meminfoLine);
                } else if (meminfoLine.startsWith("Inactive(file):")) {
                    fileCache += parseLinuxMeminfoLine(meminfoLine);
                }
            }

            // No MemAvailable, so fallback:

            long watermark_low = 0;
            for (String zoneinfoLine : OperatingSystem.readAllLines("/proc/zoneinfo")) {
                zoneinfoLine = zoneinfoLine.trim();
                if (zoneinfoLine.startsWith("low")) {
                    watermark_low += Long.parseLong(zoneinfoLine.substring(3).trim());
                }
            }
            watermark_low *= OperatingSystem.getPageSize();

            long availableFallback = 0;

            if (fileCache > 0 && slabReclaimable > 0 && watermark_low > 0) {
                fileCache -= Math.min(fileCache / 2, watermark_low);
                availableFallback = free - watermark_low + fileCache + slabReclaimable
                                    - Math.min(slabReclaimable / 2, watermark_low);
            } else {
                availableFallback = free + buffers + cached;
                availableFallback = applySwappiness(availableFallback);
            }

            if (availableFallback > 0) {
                return availableFallback;
            }

            throw new MemoryInformationException("Expected memory information missing in /proc/meminfo: " + lines);
        } catch (IOException | OperatingSystemException e) {
            throw new MemoryInformationException(e);
        }
    }

    private long applySwappiness(long cached) throws IOException, MemoryInformationException {
        int swappiness = getSwappiness();
        if (swappiness > 0) {
            // https://lwn.net/Articles/83588/

            // If swappiness is < 50, then in the worst case of full memory, under no
            // distress, page cache should be pushed out
            if (swappiness >= 50) {
                // It's unclear what the probability is of pushing page cache out if
                // swap_tendency >= 100, so we just assume 1.0
                cached = 0;
            }
        }
        return cached;
    }

    private static int cachedSwappiness = -1;

    public static synchronized int getSwappiness() throws MemoryInformationException {
        if (cachedSwappiness == -1) {
            try {
                List<String> swappinessLines = OperatingSystem.readAllLines("/proc/sys/vm/swappiness");
                if (swappinessLines.size() == 1) {
                    cachedSwappiness = Integer.parseInt(swappinessLines.get(0));
                } else {
                    throw new MemoryInformationException("Unexpected contents of /proc/sys/vm/swappiness: " + swappinessLines);
                }
            } catch (IOException e) {
                throw new MemoryInformationException(e);
            }
        }
        return cachedSwappiness;
    }

    private long getTotalMemoryMac() throws MemoryInformationException {
        try {
            List<String> lines = OperatingSystem.executeProgram("/usr/sbin/sysctl", "hw.memsize");
            if (lines.size() == 1) {
                String line = lines.get(0);
                int i = line.indexOf(": ");
                if (i != -1) {
                    return Long.parseLong(line.substring(i + 2));
                } else {
                    throw new MemoryInformationException("Unexpected output: " + line);
                }
            } else {
                throw new MemoryInformationException("Unexpected response from sysctl: " + lines);
            }
        } catch (OperatingSystemException e) {
            throw new MemoryInformationException(e);
        }
    }

    private long getAvailableMemoryMac() throws MemoryInformationException {
        try {
            long available = 0;
            List<String> vmstatLines = OperatingSystem.executeProgram("/usr/bin/vm_stat");
            for (String vmstatLine : vmstatLines) {
                if (vmstatLine.startsWith("Pages free:")) {
                    available += processMacVmStatLine(vmstatLine);
                } else if (vmstatLine.startsWith("Pages inactive:")) {
                    available += processMacVmStatLine(vmstatLine);
                } else if (vmstatLine.startsWith("Pages speculative:")) {
                    available += processMacVmStatLine(vmstatLine);
                } else if (vmstatLine.startsWith("File-backed pages:")) {
                    // This maps to "Cached Files" which is:
                    // Cached Files: Memory that was recently used by apps and is now available for
                    // use by other apps. For example, if you've been using Mail and then quit Mail,
                    // the RAM that Mail was using becomes part of the memory used by cached files,
                    // which then becomes available to other apps. If you open Mail again before its
                    // cached-files memory is used (overwritten) by another app, Mail opens more
                    // quickly because that memory is quickly converted back to app memory without
                    // having to load its contents from your startup drive.
                    // https://support.apple.com/en-us/HT201464
                    available += processMacVmStatLine(vmstatLine);
                }
            }

            if (available <= 0) {
                throw new MemoryInformationException("Unexpected output of vm_stat: " + vmstatLines);
            }

            available *= OperatingSystem.getPageSize();
            return available;
        } catch (OperatingSystemException e) {
            throw new MemoryInformationException(e);
        }
    }

    private long processMacVmStatLine(String line) {
        line = line.substring(line.indexOf(':') + 1).trim();
        // Remove the period at the end
        line = line.substring(0, line.length() - 1);
        return Long.parseLong(line);
    }

    private long getTotalMemoryWindows() throws MemoryInformationException {
        try {
            List<String> lines = OperatingSystem.executeProgram("wmic", "os", "get", "totalvisiblememorysize", "/format:list");
            for (String line : lines) {
                // https://msdn.microsoft.com/en-us/library/aa394239(v=vs.85).aspx
                if (line.startsWith("TotalVisibleMemorySize=")) {
                    return processWmicLine(line) * 1024;
                }
            }
            throw new MemoryInformationException("Unexpected response from wmic: " + lines);
        } catch (OperatingSystemException e) {
            throw new MemoryInformationException(e);
        }
    }

    private long processWmicLine(String line) {
        return Long.parseLong(line.substring(line.indexOf('=') + 1));
    }

    private long getAvailableMemoryWindows() throws MemoryInformationException {
        try {
            long available = 0;
            List<String> lines = OperatingSystem.executeProgram("wmic", "path", "Win32_PerfFormattedData_PerfOS_Memory", "get",
                                                                "/format:list");

            // https://technet.microsoft.com/en-ca/aa394268(v=vs.71)

            for (String line : lines) {
                if (line.startsWith("AvailableBytes=")) {
                    available += processWmicLine(line);
                } else if (line.startsWith("CacheBytes=")) {
                    available += processWmicLine(line);
                }
            }

            if (available <= 0) {
                throw new MemoryInformationException("Unexpected output of wmic: " + lines);
            }

            return available;
        } catch (OperatingSystemException e) {
            throw new MemoryInformationException(e);
        }
    }

    private long getTotalMemoryAix() throws MemoryInformationException {
        try {
            List<String> lines = OperatingSystem.executeProgram("/usr/sbin/lsattr", "-El", "sys0");
            for (String line : lines) {
                // https://www.ibm.com/support/knowledgecenter/en/ssw_aix_72/com.ibm.aix.cmds3/lsattr.htm
                // "realmem [...] Amount of usable physical memory in Kbytes"
                if (line.startsWith("realmem")) {
                    line = line.substring(7);
                    line = line.substring(0, line.indexOf("Amount"));
                    line = line.trim();
                    return Long.parseLong(line) * 1024;
                }
            }
            throw new MemoryInformationException("Unexpected response from lsattr: " + lines);
        } catch (OperatingSystemException e) {
            throw new MemoryInformationException(e);
        }
    }

    private long getAvailableMemoryAix() throws MemoryInformationException {
        try {
            List<String> lines = OperatingSystem.executeProgram("/usr/bin/svmon", "-O", "summary=basic,unit=KB");

            // https://www.ibm.com/support/knowledgecenter/ssw_aix_72/com.ibm.aix.prftools/idprftools_svmon_repd_gl.htm

            for (String line : lines) {
                if (line.startsWith("memory")) {
                    String[] pieces = spacePattern.split(line);
                    if (pieces.length >= 7) {
                        return Long.parseLong(pieces[6]) * 1024;
                    } else {
                        throw new MemoryInformationException("Expected memory information missing in svmon: " + lines);
                    }
                }
            }

            throw new MemoryInformationException("Expected memory information missing in svmon: " + lines);
        } catch (OperatingSystemException e) {
            throw new MemoryInformationException(e);
        }
    }

    private long getTotalMemoryHp() throws MemoryInformationException {

        // adb requires root and dmesg rolls, so we can't use either. Approximate
        // using vmstat.
        //
        // https://support.hpe.com/hpsc/doc/public/display?docId=emr_na-c00959008

        try {
            List<String> lines = OperatingSystem.executeProgram("vmstat", "1", "1");
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().startsWith("r")) {
                    String[] pieces = spacePattern.split(lines.get(i + 2));
                    return (Long.parseLong(pieces[3]) + Long.parseLong(pieces[4])) * 1024;
                }
            }

            throw new MemoryInformationException("Unexpected response from vmstat: " + lines);
        } catch (OperatingSystemException e) {
            throw new MemoryInformationException(e);
        }
    }

    private long getAvailableMemoryHp() throws MemoryInformationException {
        try {
            List<String> lines = OperatingSystem.executeProgram("vmstat", "1", "1");
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().startsWith("r")) {
                    String[] pieces = spacePattern.split(lines.get(i + 2));
                    return Long.parseLong(pieces[4]) * 1024;
                }
            }

            throw new MemoryInformationException("Expected memory information missing in vmstat: " + lines);
        } catch (OperatingSystemException e) {
            throw new MemoryInformationException(e);
        }
    }

    private long getTotalMemoryIBMi() throws MemoryInformationException {
        // Available in QWCRSSTS but inaccessible without a native layer.
        return getTotalMemoryJDK();
    }

    private long getAvailableMemoryIBMi() throws MemoryInformationException {
        return getFreeMemoryJDK();
    }

    private long getTotalMemorySolaris() throws MemoryInformationException {

        // "To find how much physical memory is installed on the system, use the prtconf
        // command in Solaris. For example:
        //
        // -bash-3.00# prtconf|grep Memory
        // Memory size: 1024 Megabytes"
        //
        // http://www.oracle.com/technetwork/server-storage/solaris/solaris-memory-135224.html

        try {
            List<String> lines = OperatingSystem.executeProgram("prtconf");
            for (String line : lines) {
                if (line.toLowerCase().startsWith("memory")) {
                    line = line.substring(line.indexOf(':') + 1).trim();
                    return inferBytes(line);
                }
            }
            throw new MemoryInformationException("Unexpected response from prtdiag: " + lines);
        } catch (OperatingSystemException e) {
            throw new MemoryInformationException(e);
        }
    }

    private long getAvailableMemorySolaris() throws MemoryInformationException {
        // "Look at the free column (the unit is KB). [...] on Solaris 8 or later, the
        // free memory shown in the vmstat output includes the free list and the cache
        // list. The free list is the amount of memory that is actually free. This is
        // memory that has no association with any file or process. The cache list is
        // also the free memory; it is the majority of the file system cache. The cache
        // list is linked to the free list; if the free list is exhausted, then memory
        // pages will be taken from the head of the cache list."
        //
        // http://www.oracle.com/technetwork/server-storage/solaris/solaris-memory-135224.html

        try {
            List<String> lines = OperatingSystem.executeProgram("vmstat", "1", "1");
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().startsWith("r")) {
                    String[] pieces = spacePattern.split(lines.get(i + 2));
                    return Long.parseLong(pieces[4]) * 1024;
                }
            }

            throw new MemoryInformationException("Expected memory information missing in vmstat: " + lines);
        } catch (OperatingSystemException e) {
            throw new MemoryInformationException(e);
        }
    }

    private long getTotalMemoryzOS() throws MemoryInformationException {
        // DISPLAY M=STOR could get this but this can't
        // simply be executed through something like tsocmd.
        // Testing shows that IBM Java on z/OS calculates total
        // RAM nearly correctly:
        // Total Memory (bytes) = 4212011008
        // Matching M=STOR:
        //   ONLINE-NOT RECONFIGURABLE
        //   0M-4096M
        return getTotalMemoryJDK();
    }

    private long getAvailableMemoryzOS() throws MemoryInformationException {
        // There are REXX programs like IAXDMEM although those don't
        // always seem to be available. IPCS has RSMDATA SUMMARY, but
        // it would be crazy to start IPCS just to get memory stats.
        // USS ps doesn't have RSS. We could dynamically create a mimic
        // of IAXDMEM and execute through tsocmd. This is too complicated
        // without a native layer.

        long result = getFreeMemoryJDK();

        // Testing has shown that IBM Java returns -1
        if (result <= 0) {
            throw new MemoryInformationException("Estimate of available memory not available on z/OS");
        }

        return result;
    }

    private static OperatingSystemMXBean osMxBean;
    private static Method methodGetTotalPhysicalMemorySize;
    private static Method methodGetFreePhysicalMemorySize;

    private synchronized long getTotalMemoryJDK() throws MemoryInformationException {
        if (osMxBean == null) {
            osMxBean = ManagementFactory.getOperatingSystemMXBean();
        }
        try {
            if (methodGetTotalPhysicalMemorySize == null) {
                try {
                    methodGetTotalPhysicalMemorySize = osMxBean.getClass().getMethod("getTotalPhysicalMemorySize");
                } catch (NoSuchMethodException nsme) {
                    methodGetTotalPhysicalMemorySize = osMxBean.getClass().getMethod("getTotalPhysicalMemory");
                }

                // This is needed for some JDKs since implementation is private
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        methodGetTotalPhysicalMemorySize.setAccessible(true);
                        return null;
                    }
                });
            }

            return (Long) methodGetTotalPhysicalMemorySize.invoke(osMxBean);

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                        | SecurityException e) {
            throw new MemoryInformationException(e);
        }
    }

    private synchronized long getFreeMemoryJDK() throws MemoryInformationException {
        if (osMxBean == null) {
            osMxBean = ManagementFactory.getOperatingSystemMXBean();
        }
        try {
            if (methodGetFreePhysicalMemorySize == null) {
                methodGetFreePhysicalMemorySize = osMxBean.getClass().getMethod("getFreePhysicalMemorySize");

                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        methodGetFreePhysicalMemorySize.setAccessible(true);
                        return null;
                    }
                });
            }

            return (Long) methodGetFreePhysicalMemorySize.invoke(osMxBean);

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                        | SecurityException e) {
            throw new MemoryInformationException(e);
        }
    }

    public static void main(String... args) throws Throwable {
        out(MemoryInformation.class.getName() + " started");
        out("Operating System: " + OperatingSystem.instance().getOperatingSystemType());
        out("Total memory: " + instance().getTotalMemory());
        out("Available memory: " + instance().getAvailableMemory());
        out("Available memory %: " + instance().getAvailableMemoryRatio() * 100.0);
    }

    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static void out(String message) {
        System.out.println("[" + df.format(new Date()) + "] " + message);
    }
}

/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.filemonitor.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.filemonitor.internal.UpdateMonitor.MonitorType;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * The monitor holder wraps a registered FileMonitor service:
 * it manages the ScheduledFuture used to perform the resource scan requested/described
 * by the FileMonitor.
 *
 * When a FileMonitor is registered/bound to the core service, a new MonitorHolder is
 * created. If a service reference changes (because its properties changed),
 * it will be re-bound: in that case, {@link #update} is called to consume the
 * new service properties.
 */
public abstract class MonitorHolder implements Runnable {
    /**  */
    static final int TIME_TO_WAIT_FOR_COPY_TO_COMPLETE = 100;

    static final int NUMBER_OF_EXCEPTIONS_BEFORE_DISABLING_MONITOR = 3;

    static final TraceComponent tc = Tr.register(MonitorHolder.class);

    private final static Pattern INTERVAL_STRING = Pattern.compile("(\\d+)(\\w+)");

    // These are used to hold unnotified/unrequested filesystem
    // changes, so they can be used on subsequent calls
    // to the externalScan
    Set<File> unnotifiedFileCreates = new HashSet<File>();
    Set<File> unnotifiedFileDeletes = new HashSet<File>();
    Set<File> unnotifiedFileModifies = new HashSet<File>();

    public enum MonitorState {
        UNKNOWN, INIT, INITIALIZING, ACTIVE, DESTROY, DESTROYED
    };

    private final AtomicInteger monitorState = new AtomicInteger(MonitorState.UNKNOWN.ordinal());

    /**
     * Lock guarding file scan, update, and destroy: ensures only one thread
     * is actively working with the (unsynchronized) list of UpdateMonitors.
     */
    private final Lock scanLock = new ReentrantLock();

    /**
     * Skip starting another scan if one is already in progress
     * (includes notification of monitor)
     */
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);

    /** CoreService implementation: provides access to bound/required services */
    private final CoreService coreService;

    /** Associated bound FileMonitor service reference: used to extract service properties */
    private final ServiceReference<FileMonitor> monitorRef;

    /**
     * Map of monitored {@link File}s to the {@link UpdateMonitor} that
     * manages the actual detection of modified resources.
     */
    private final Map<UpdateMonitor, UpdateMonitor> updateMonitors;

    /**
     * If true, any monitored directories will be scanned recursively
     *
     * @see FileMonitor#MONITOR_RECURSE
     */
    private boolean monitorRecurse;

    /**
     * If true, any monitored directories will also monitor themselves, not just their content.
     *
     * @see FileMonitor#MONITOR_INCLUDE_SELF;
     */
    private boolean monitorSelf;

    /**
     * Filter used to restrict notification of modified resources
     *
     * @see FileMonitor#MONITOR_FILTER
     */
    protected String monitorFilter;

    /**
     * Scan interval
     *
     * @see FileMonitor#MONITOR_INTERVAL
     */
    private long monitorInterval;
    /**
     * Scan interval time unit
     *
     * @see FileMonitor#MONITOR_INTERVAL
     */
    private TimeUnit monitorTimeUnit;

    /** Associated bound FileMonitor service reference: used to extract service properties */
    private FileMonitor monitor;

    /** Did any of the properties other than monitorInterval change? **/
    private boolean needsBaseline;

    /**
     * {@link ScheduledFuture} created when the recurring task is scheduled
     * with the {@link ScheduledExecutorService}.
     *
     * @see ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)
     */
    private ScheduledFuture<?> scheduledFuture = null;

    /**
     * A count of how many consecutive times the FileMonitor has barfed on us. We'll give
     * a monitor a few chances to handle (different) changes in case timing glitches cause an
     * intermittent error, but if it keeps throwing exceptions we'll disable that monitor.
     * This doesn't need to be atomic since the consequences of changing it inconsistently
     * on multiple threads are pretty minor.
     */
    private int exceptionCount = 0;

    /**
     * @param coreService
     * @param monitorRef
     * @param cacheRoot
     */
    public MonitorHolder(CoreService coreService, ServiceReference<FileMonitor> monitorRef) {
        if (coreService == null)
            throw new NullPointerException("CoreService must be non-null");
        if (monitorRef == null)
            throw new NullPointerException("FileMonitor reference must be non-null");

        this.coreService = coreService;
        this.monitorRef = monitorRef;
        this.updateMonitors = new HashMap<UpdateMonitor, UpdateMonitor>();
        this.needsBaseline = true;

        initProperties(monitorRef);
    }

    private void initProperties(ServiceReference<FileMonitor> monitorRef) {
        Object value;

        // Monitor filter should be a String or null
        value = monitorRef.getProperty(FileMonitor.MONITOR_FILTER);
        if (value != null && !(value instanceof String)) {
            Tr.warning(tc, "badFilter", value);
            throw new IllegalArgumentException("Invalid monitor filter: value=" + value);
        }
        if (monitorFilter == null) {
            needsBaseline |= value != null;
        } else {
            needsBaseline |= !monitorFilter.equals(value);
        }

        this.monitorFilter = (String) value;

        // Monitor recurse should be a representation of a boolean (see MetatypeUtils method)
        value = monitorRef.getProperty(FileMonitor.MONITOR_RECURSE);
        boolean b = MetatypeUtils.parseBoolean(monitorRef.getProperty(Constants.SERVICE_PID),
                                               FileMonitor.MONITOR_RECURSE, value, false);
        needsBaseline |= monitorRecurse != b;
        this.monitorRecurse = b;

        // Monitor self should be a representation of a boolean (see MetatypeUtils method)
        value = monitorRef.getProperty(FileMonitor.MONITOR_INCLUDE_SELF);
        b = MetatypeUtils.parseBoolean(monitorRef.getProperty(Constants.SERVICE_PID),
                                       FileMonitor.MONITOR_INCLUDE_SELF, value, false);
        needsBaseline |= monitorSelf != b;
        this.monitorSelf = b;

        long interval = 10;
        TimeUnit unit = TimeUnit.MILLISECONDS;

        // Monitor interval: 12m or 2ms .. see javadoc for MONITOR_INTERVAL
        // Continue to support basic units (in addition to pre-converted long)
        // for programmatic file monitors
        value = monitorRef.getProperty(FileMonitor.MONITOR_INTERVAL);

        if (value == null) {
            //no interval property was provided set the interval to 0
            //i.e. no scheduled scan
            interval = 0;
        } else if (value.getClass().isAssignableFrom(Long.class)) {
            interval = (Long) value;
        } else if (value instanceof String) {
            String intervalString = (String) value;
            Matcher m = INTERVAL_STRING.matcher(intervalString);
            if (m.matches()) {
                // either of these could throw it's own Illegal argument exception
                // if one of the component parts is bad.
                interval = parseInterval(m.group(1), intervalString);
                unit = parseTimeUnit(m.group(2), intervalString);
            } else {
                Tr.warning(tc, "badInterval", value);
                throw new IllegalArgumentException("Invalid monitor interval: value=" + value);
            }
        } else {
            Tr.warning(tc, "badInterval", value);
            throw new IllegalArgumentException("Invalid monitor interval: value=" + value);
        }

        monitorInterval = interval;
        monitorTimeUnit = unit;

        // Mark in init state
        monitorState.set(MonitorState.INIT.ordinal());
    }

    /**
     * Deferred initialization: this is called when/while the core service is active. Services
     * registered before core service registration have only the constructor called, when
     * the core service is later activated, init will be called on those services.
     *
     * Just to be safe (and to make the core service code simpler), this method should
     * tolerate being called twice.
     */
    public void init() {
        // Only do initialization if we're in INIT state coming into this method
        if (monitorState.compareAndSet(MonitorState.INIT.ordinal(), MonitorState.INITIALIZING.ordinal())) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Monitor holder init, initializing");
            Object value;
            Collection<String> collection;
            Collection<File> baseline = new HashSet<File>();
            Map<UpdateMonitor, UpdateMonitor> oldMap = null;

            value = monitorRef.getProperty(FileMonitor.MONITOR_FILES);
            collection = Collections.emptyList(); // default will be empty list, use this to establish type for parse collection
            Object servicePid = monitorRef.getProperty(Constants.SERVICE_PID);

            boolean performBaseline = false;
            try {
                scanLock.lock(); // Lock for updateMonitors modification

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Monitor holder init, scan lock obtained");
                if (!updateMonitors.isEmpty()) {
                    oldMap = new HashMap<UpdateMonitor, UpdateMonitor>(updateMonitors);
                    updateMonitors.clear();
                }

                LinkedList<UpdateMonitor> newUpdateMonitors = new LinkedList<UpdateMonitor>();

                // Create/Configure update monitors for files
                collection = MetatypeUtils.parseStringCollection(servicePid, FileMonitor.MONITOR_FILES, value, collection);
                for (String location : collection) {
                    try {
                        File file = new File(coreService.getLocationService().resolveString(location));

                        UpdateMonitor um = createUpdateMonitor(file, MonitorType.FILE, monitorFilter);

                        // Remove/preserve existing/matching monitors from the old map.
                        UpdateMonitor prevMonitor = oldMap == null ? null : oldMap.remove(um);

                        if (prevMonitor != null) {
                            // Preserve existing update monitors
                            updateMonitors.put(prevMonitor, prevMonitor);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "Monitor holder init, updating existing u monitor " + um);
                            // need to destroy the um we just created because it is not going to be used
                            um.destroy();
                        } else {
                            newUpdateMonitors.add(um);
                            needsBaseline = true;
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "Monitor holder init, adding new u monitor " + um);
                        }

                    } catch (RuntimeException e) {
                        // FFDC generated for runtime exception
                        Tr.warning(tc, "createMonitorException", location, e.getLocalizedMessage());
                    }
                }

                // Create/Configure update monitors for directories
                value = monitorRef.getProperty(FileMonitor.MONITOR_DIRECTORIES);
                collection = Collections.emptyList(); // default will be empty list, use this to establish type for parse collection
                collection = MetatypeUtils.parseStringCollection(servicePid, FileMonitor.MONITOR_DIRECTORIES, value, collection);

                monitor = coreService.getReferencedMonitor(monitorRef);
                if (monitor != null) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Monitor holder init, monitor not null");
                    for (String location : collection) {
                        try {
                            File file = new File(coreService.getLocationService().resolveString(location));
                            MonitorType type = null;
                            if (monitorRecurse) {
                                if (monitorSelf) {
                                    type = MonitorType.DIRECTORY_RECURSE_SELF;
                                } else {
                                    type = MonitorType.DIRECTORY_RECURSE;
                                }
                            } else {
                                if (monitorSelf) {
                                    type = MonitorType.DIRECTORY_SELF;
                                } else {
                                    type = MonitorType.DIRECTORY;
                                }

                            }

                            // NOTE: File caching is disabled. if/when we want it back, we can start
                            // checking against a minimum interval before setting the value.
                            UpdateMonitor um = createUpdateMonitor(file, type, monitorFilter);

                            // Remove/preserve existing/matching monitors from the old map.
                            UpdateMonitor prevMonitor = oldMap == null ? null : oldMap.remove(um);

                            if (prevMonitor != null) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    Tr.debug(tc, "Monitor holder init, found existing dir update monitor " + um);

                                // Preserve existing update monitors
                                updateMonitors.put(prevMonitor, prevMonitor);
                                // need to destroy the um we just created because it is not going to be used
                                um.destroy();
                            } else {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    Tr.debug(tc, "Monitor holder init, adding new dir update monitor " + um);
                                newUpdateMonitors.add(um);
                                needsBaseline = true;
                            }

                        } catch (RuntimeException e) {
                            // FFDC generated for runtime exception
                            Tr.warning(tc, "createMonitorException", location, e.getLocalizedMessage());
                        }
                    }

                    if (needsBaseline || !!!oldMap.isEmpty()) {
                        // new properties or new/removed filesets -> new baseline
                        for (UpdateMonitor um : updateMonitors.keySet()) {
                            um.init(baseline);
                        }
                        for (UpdateMonitor um : newUpdateMonitors) {
                            um.init(baseline);
                            updateMonitors.put(um, um);
                        }
                        needsBaseline = true;
                    }

                    // Mark state as active, and schedule the task
                    monitorState.set(MonitorState.ACTIVE.ordinal());
                }
                performBaseline = needsBaseline;
                needsBaseline = false;
            } finally {
                scanLock.unlock();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Monitor holder init, scan lock released");
            }

            if (monitor != null && FrameworkState.isValid()) {
                // Notify (new) registered FileMonitor of the initial set of files that
                // match their configuration. For "refreshes" the new resources will show
                // up in the next scan (as add/remove/delete)
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Monitor holder init, calling initComplete");
                if (performBaseline) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Baseline: " + baseline);
                    monitor.onBaseline(baseline);
                }

                start();

                // destroy old monitors
                if (oldMap != null && !oldMap.isEmpty()) {
                    for (UpdateMonitor um : oldMap.keySet()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Monitor holder init, destroying old monitor " + um);
                        um.destroy();
                    }
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Monitor holder init, monitor missing");
                //monitor really shouldn't be null at this stage.. if it is then we need
                //to shut up shop, as we cannot function.
                monitorState.set(MonitorState.DESTROY.ordinal());
                stop();
                doDestroy();
            }

        }
    }

    protected abstract UpdateMonitor createUpdateMonitor(File file, MonitorType type, String monitorFilter);

    /**
     * Method to refresh/update the monitor properties after its already been initialized before.
     *
     * @param cacheRoot
     */
    public synchronized void refresh(File cacheRoot) {
        monitorState.set(MonitorState.INIT.ordinal());
        stop();
        initProperties(monitorRef);
        init();
    }

    /**
     * Start monitoring the file collection
     */
    private synchronized void start() {
        // only reschedule if monitor is still active (not in the process of being initialized or destroyed
        // don't schedule a task if there are no monitors or the monitorInterval was set to 0
        // don't schedule a task if the framework is stopping
        if (scheduledFuture == null
            && monitorState.get() == MonitorState.ACTIVE.ordinal()
            && !updateMonitors.isEmpty()
            && monitorInterval != 0
            && FrameworkState.isValid()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Scan task scheduled");
            }
            scheduledFuture = coreService.getScheduler().scheduleWithFixedDelay(this, monitorInterval, monitorInterval, monitorTimeUnit);
        }
    }

    /**
     * Reschedule the task: in the case the ScheduledExecutor service is
     * replaced, we'll have to cancel the existing future, and reschedule
     * it with the new service.
     */
    public synchronized void reschedule() {
        stop();
        start();
    }

    /**
     * This is called when a service is being unregistered because _this_ bundle
     * or the framework is stopping, and not because the FileMonitor service
     * has been unregistered (in which case {@link MonitorHolder#destroy()} would
     * be called instead).
     */
    public synchronized void stop() {
        if (scheduledFuture != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Scan task cancelled");
            }
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
    }

    /**
     * Called when a service is unregistered or replaced because the
     * bundle that created/registered the ServiceReference is stopping
     * (unless the framework itself is also stopping, in which
     * case {@link #stop()} will be called instead.
     *
     * Resources should be cleaned up, future cancelled, etc.
     */
    public void destroy() {
        // if we are in init state then switch to destroy, this will do nothing if this is unsafe
        monitorState.compareAndSet(MonitorState.INIT.ordinal(), MonitorState.DESTROY.ordinal());
        // indicate monitor should be destroyed.
        if (monitorState.compareAndSet(MonitorState.ACTIVE.ordinal(), MonitorState.DESTROY.ordinal())) {
            // Stop the scheduled scan task
            stop();

            // attempt to really destroy: may "fail" if lock cannot be obtained
            doDestroy();
        }
    }

    /**
     * Will actually destroy the monitors if and only if the monitor state has already been set to DESTROY.
     */
    private boolean doDestroy() {
        // Try to obtain the scan lock to destroy all associated monitors
        // Other users of the scanLock should check the destroy flag
        // before they release the lock to determine whether or not
        // resources should be destroyed
        if (scanLock.tryLock()) {
            try {
                if (monitorState.compareAndSet(MonitorState.DESTROY.ordinal(), MonitorState.DESTROYED.ordinal())) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, "Destroy file monitor");
                    }

                    for (UpdateMonitor m : updateMonitors.keySet()) {
                        m.destroy();
                    }
                    updateMonitors.clear();
                    // Clear any unnotified file changes, these will become invalid
                    // as the monitors are destroyed.
                    unnotifiedFileCreates.clear();
                    unnotifiedFileDeletes.clear();
                    unnotifiedFileModifies.clear();

                    return true; // monitor was destroyed
                }
            } finally {
                scanLock.unlock();
            }
        }

        // Also return true if monitor was already destroyed
        return monitorState.get() == MonitorState.DESTROYED.ordinal();
    }

    private boolean isStopped = false;

    // Stop scanning for changes during server quiesce period
    void serverStopping() {
        this.isStopped = true;
        stop();
    }

    @Override
    public void run() {
        scheduledScan();
    }

    /**
     * Perform the scan on monitored resources. This method checks the
     * destroy flag before the scan begins, and after the scan completes,
     * to ensure that any scheduled destruction is carried out as soon as
     * reasonable.
     * <p>
     * This is not a trivial method -- but we use the trival annotation to prevent
     * entry/exit trace for every invocation.
     */
    @Trivial
    @FFDCIgnore(InterruptedException.class)
    void scheduledScan() {
        // Don't perform a scheduled scan if this monitor holder is paused
        if (isStopped)
            return;

        // 152229: Changed this code to get the monitor type locally.  That is, now we save the monitor type in the constructor.
        // We used to get the monitor type here by monitorRef.getProperty(FileMonitor.MONITOR_TYPE)). That caused a
        // ConcurrentModificationException because of interference from the JMocked FileMonitor in the unit test code.
        // Don't do anything if this is an external monitor
        if (FileMonitor.MONITOR_TYPE_EXTERNAL.equals(monitorRef.getProperty(FileMonitor.MONITOR_TYPE))) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "scheduledScan - RETURN early - external monitor");
            }
            return;
        }

        // Don't do anything if the framework is stopping. Allow normal component cleanup
        // to deactivate/clean up the scheduled tasks, but make this a no-op if the
        // server is shutting down.
        if (FrameworkState.isStopping()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "scheduledScan - RETURN early - framework stopping");
            }
            return;
        }

        // Don't do anything unless we can set scanInProgress to true
        // Use this to prevent scanning while a scan is in progress. Monitor notification must happen
        // outside of the lock to prevent deadlocks.
        if (!scanInProgress.compareAndSet(false, true)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "scheduledScan - RETURN early - already scan in progress?");
            }
            return;
        }

        try {
            Set<File> created = new HashSet<File>();
            Set<File> deleted = new HashSet<File>();
            Set<File> modified = new HashSet<File>();

            // Try to obtain the scan lock -- this might fail if the monitor configuration is being updated
            if (scanLock.tryLock()) {
                try {
                    // Always try destroy when we obtain the lock: it will return true if this is in destroy or destroyed state
                    // Also (after we have tried doDestroy) ensure that we are in active state
                    if (!doDestroy() && (monitorState.get() == MonitorState.ACTIVE.ordinal())) {
                        if (coreService.isDetailedScanTraceEnabled() && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "File monitor scan: begin", updateMonitors);
                        }

                        scanForUpdates(created, deleted, modified);

                        if (!created.isEmpty() || !modified.isEmpty() || !deleted.isEmpty()) {
                            // Check again, make sure there have been no further changes since the scan we just
                            // ran (we don't want to read the files until any updates are complete, files may be
                            // in process of being copied).
                            // what seems to be the vogue is to do this check to make sure nothing moved twice.
                            // i.e. keep the re-check interval at 100ms, but require two clean go-rounds before
                            // delivering the all clear.
                            boolean oneClean = false;
                            boolean twoClean = false;

                            List<File> createdCheck = new ArrayList<File>();
                            List<File> deletedCheck = new ArrayList<File>();
                            List<File> modifiedCheck = new ArrayList<File>();

                            do {
                                // Wait for 100 ms before checking again to give files time to finish
                                // copying if they are mid copy. Note this may not work for copying
                                // large files via programs like FTP where the copy may pause or
                                // if an OS creates the file and sets the size/last modified before
                                // the copy completes, but it should fix it for smaller files or for the
                                // test environment where some files are streamed over rather than copied.
                                try {
                                    // Only used once and not sure it needs to be configurable so didn't create a
                                    // constant for the delay period.
                                    Thread.sleep(TIME_TO_WAIT_FOR_COPY_TO_COMPLETE);
                                } catch (InterruptedException ex) {
                                }

                                // Clear the lists, want a clean set rather than appending to existing to check
                                // if this loop is "update free". Do not clear the deletedCreatedCheck or
                                // deletedModifiedCheck as these need to track status over multiple loops.
                                createdCheck.clear();
                                deletedCheck.clear();
                                modifiedCheck.clear();

                                scanForUpdates(createdCheck, deletedCheck, modifiedCheck);
                                resolveChangesForScheduledScan(created, deleted, modified, createdCheck, deletedCheck, modifiedCheck);

                                if (createdCheck.isEmpty() && modifiedCheck.isEmpty() && deletedCheck.isEmpty()) {
                                    // This run was clean-- hooray!
                                    if (oneClean) {
                                        twoClean = true; // <-- loop exit condition
                                    } else {
                                        oneClean = true; // <-- hopefully only one more time through
                                    }
                                } else {
                                    oneClean = false; // bummer.
                                }

                                // Keep going until we have two 100ms intervals with no changes
                                // (AND the runtime/framework is still happy)
                            } while (!twoClean && FrameworkState.isValid());
                        }
                    }
                } catch (RuntimeException e) {
                    // TODO: MUST CATCH exceptions here (to at least get FFDC)... ick
                } finally {
                    try {
                        doDestroy(); // always attempt destroy while we hold the lock
                    } finally {
                        scanLock.unlock();
                    }
                }

                if (!created.isEmpty() || !modified.isEmpty() || !deleted.isEmpty()) {
                    // changes were discovered: trace & call the registered file monitor
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "File monitor scan: end; resources changed",
                                 created.size() + " created",
                                 modified.size() + " modified",
                                 deleted.size() + " deleted",
                                 "running=" + FrameworkState.isValid());
                    }

                    // Even if we do get into a scan, make sure the framework is still good before we
                    // push the notification of updates-- Avoid propagating change notification
                    // while components that might react to them are being shut down

                    if (FrameworkState.isValid()) {
                        try {
                            monitor.onChange(created, modified, deleted);
                            // If the monitor handled the call cleanly, reset our exception count
                            exceptionCount = 0;
                        } catch (RuntimeException e) {
                            // FFDC instrumentation will go here
                            // Catch the exception so it doesn't kill the whole scheduler

                            exceptionCount++;
                            Tr.warning(tc, "fileMonitorException", created, modified, deleted, monitor.getClass(), e.getLocalizedMessage());
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "scheduledScan - exceptionCount=" + exceptionCount);
                            }

                            // If the monitor has thrown exceptions a few times in a row abandon
                            // monitoring for it
                            if (exceptionCount >= NUMBER_OF_EXCEPTIONS_BEFORE_DISABLING_MONITOR) {
                                Tr.warning(tc, "fileMonitorDisabled", NUMBER_OF_EXCEPTIONS_BEFORE_DISABLING_MONITOR, monitor.getClass());
                                // Reset the exceptionCount just in case we get re-enabled by outside forces for some unknown reason
                                exceptionCount = 0;
                                destroy();
                            }

                        }
                    } else {
                        //no framework, we should try to cleanup.
                        stop();
                    }
                } else if (coreService.isDetailedScanTraceEnabled() && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    // If super detailed trace is enabled, we trace the begin/end of all file scans
                    Tr.debug(this, tc, "File monitor scan: end; no changes");
                }
            } // end if tryLock
        } finally {
            scanInProgress.set(false);
        }
    }

    /**
     * Behaves similarly to {@link #scheduledScan()}, with the following key differences:
     * <ul>
     * <li>This method takes a list of files we are being notified about.
     * Only the relevant subset of these files will be passed to the {@link FileMonitor}</li>
     * <li>This method does <em>not</em> wait for a clear 100ms without
     * any file changes because all the relevant changes have already
     * happened before this method has been called (e.g by the tooling
     * via an MBean)</li>
     * <li>This method will wait for the scanLock rather than exiting out if another thread
     * holds the lock. scheduledScan can short circuit out because the lock holding thread
     * will pick up the changes. Here, each call to the method may have different arguments
     * so we need to make sure each call is handled.</li>
     * </ul>
     *
     * @param notifiedCreated The canonical paths of any created files
     * @param notifiedDeleted The canonical paths of any deleted files
     * @param notifiedModified The canonical paths of any modified files
     * @param doFilterPaths The filter indicator. If true, input paths are filtered against pending file events.
     *            If false, all pending file events are processed.
     * @param listenerFilter The filter string that allows only those listeners with a matching id to be called to process the event.
     */
    void externalScan(Set<File> notifiedCreated, Set<File> notifiedDeleted, Set<File> notifiedModified, boolean doFilterPaths, String listenerFilter) {
        // Don't perform the external scan if this monitor holder is paused
        if (isStopped)
            return;

        // only do anything if this is an 'external' monitor
        if (!!!FileMonitor.MONITOR_TYPE_EXTERNAL.equals(monitorRef.getProperty(FileMonitor.MONITOR_TYPE)))
            return;

        // Give monitoring activity on other threads a chance to catch up before requesting a scan
        // (This is most likely to affect unit test behaviour rather than mbean invocations, but be safe)
        Thread.yield();

        // Multiple threads can call the FileNotificationMBean simultaneously so we need to lock
        scanLock.lock();
        try {
            // Always try destroy when we obtain the lock: it will return true if this is in destroy or destroyed state
            // Also (after we have tried doDestroy) ensure that we are in active state
            if (!doDestroy() && (monitorState.get() == MonitorState.ACTIVE.ordinal())) {
                if (coreService.isDetailedScanTraceEnabled() && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "File monitor scan: begin", updateMonitors);
                }

                List<File> actualCreated = new ArrayList<File>();
                List<File> actualDeleted = new ArrayList<File>();
                List<File> actualModified = new ArrayList<File>();

                scanForUpdates(actualCreated, actualDeleted, actualModified);

                // use the correct case forms of the files we found in our internal scan
                Set<File> created = PathUtils.fixPathFiles(actualCreated);
                Set<File> deleted = PathUtils.fixPathFiles(actualDeleted);
                Set<File> modified = PathUtils.fixPathFiles(actualModified);

                // SPI PathUtils.fixpathFiles returns an empty collection if the file
                // list is empty, create an actual set so we can add to it later if needed
                if (created == Collections.EMPTY_SET)
                    created = new HashSet<File>();
                if (deleted == Collections.EMPTY_SET)
                    deleted = new HashSet<File>();
                if (modified == Collections.EMPTY_SET)
                    modified = new HashSet<File>();

                // Take the previously unnotified/unrequested changes
                // and resolve them against the result of the latest
                // filesystem scan to make sure they are still
                // valid
                resolveChangesForExternalScan(unnotifiedFileCreates,
                                              unnotifiedFileDeletes,
                                              unnotifiedFileModifies,
                                              created,
                                              deleted,
                                              modified);

                // Now merge the result of the current filesystem scan with
                // previous unnotified changes. This represents the complete
                // set of valid/current choices they can now notify about
                created.addAll(unnotifiedFileCreates);
                deleted.addAll(unnotifiedFileDeletes);
                modified.addAll(unnotifiedFileModifies);

                // We are going to rebuild these lists from anything left over in the next block
                unnotifiedFileCreates.clear();
                unnotifiedFileDeletes.clear();
                unnotifiedFileModifies.clear();

                // If a filter was specified, all pending updates are to be processed.
                if (doFilterPaths) {
                    // Now take the notified changes and compare it against all the possible
                    // valid choices, unrequested changes are placed into the unnotified set
                    // so they can be used by the caller on subsequent calls
                    filterSets(created, notifiedCreated, unnotifiedFileCreates);
                    filterSets(deleted, notifiedDeleted, unnotifiedFileDeletes);
                    filterSets(modified, notifiedModified, unnotifiedFileModifies);
                }

                if (!created.isEmpty() || !modified.isEmpty() || !deleted.isEmpty()) {
                    // changes were discovered: trace & call the registered file monitor
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "File monitor scan: end; resources changed",
                                 created.size() + " created",
                                 modified.size() + " modified",
                                 deleted.size() + " deleted");
                    }

                    if (monitor != null) {
                        try {
                            // If we are processing all pending events, call the extended version of the FileMonitor.
                            if (!doFilterPaths && monitor instanceof com.ibm.ws.kernel.filemonitor.FileMonitor) {
                                ((com.ibm.ws.kernel.filemonitor.FileMonitor) monitor).onChange(created, modified, deleted, listenerFilter);
                            } else {
                                monitor.onChange(created, modified, deleted);
                            }

                        } catch (RuntimeException e) {
                            // FFDC instrumentation will go here
                            // Catch the exception so we can FFDC it
                            // Don't increment the exception counter since this is externally triggered
                            Tr.warning(tc, "fileMonitorException", created, modified, deleted, monitor.getClass(), e.getLocalizedMessage());
                        }
                    }
                } else if (coreService.isDetailedScanTraceEnabled() && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    // If super detailed trace is enabled, we trace the begin/end of all file scans
                    Tr.debug(this, tc, "File monitor scan: end; no changes");
                }
            }
        } catch (RuntimeException e) {
            // TODO: MUST CATCH exceptions here (to at least get FFDC)... ick
        } finally {
            try {
                doDestroy(); // always attempt destroy while we hold the lock
            } finally {
                scanLock.unlock();
            }
        }

    }

    /**
     * @param created
     * @param deleted
     * @param modified
     * @param createdCheck
     * @param deletedCheck
     * @param modifiedCheck
     */
    private void resolveChangesForScheduledScan(Set<File> created, Set<File> deleted, Set<File> modified, List<File> createdCheck, List<File> deletedCheck,
                                                List<File> modifiedCheck) {

        // Note any change to the way state is handled in this code must also be
        // updated in resolveChangesForExternalScan

        // State change loop to track the status of changed files.
        // D = Delete, C = Create, M = modified, - = not in any list, X = not possible
        // Example, "CM = C" would mean create followed by a modify would
        // result in the file being in the create list
        // C = C
        // D = D
        // M = M
        // CC = X
        // CD = -
        // CM = C
        // DC = M
        // DD = X
        // DM = X
        // MC = X
        // MD = D
        // MM = M

        // If a file has been deleted then remove it from created / modified
        // list (if they exist - null op if they don't). It can't already
        // exist in deleted (can't delete it twice in a row). If the file
        // was in the create step then deleting it moves it back to the
        // "empty" state, it won't exist in any list. If the file was in the
        // modified state or not in any state, then it did exist before so
        // should now be put into the delete state and removed from the modified
        // state.

        for (File f : deletedCheck) {
            if (!created.remove(f)) { // Covers CD
                modified.remove(f); // Covers MD or just D
                deleted.add(f);
            }
        }

        // If a file has recently been created, check if it has been removed
        // before, if so then it should be listed as modified (deleted then
        // created would means its modified from original). If it hasn't
        // been removed before then add it to the created list (it can't already
        // exist there as it can't be created twice in a row).
        for (File f : createdCheck) {
            if (deleted.remove(f)) {
                modified.add(f); // Covers DC
            } else {
                created.add(f); // Covers C
            }
        }

        // If a file has just been modified then check if it had been created just before,
        // if this is the case do nothing (i.e. leave it in created list only). If not
        // then add it to the modified list if it doesn't already exist in there.
        for (File f : modifiedCheck) {
            if (!!!created.contains(f) && !!!modified.contains(f)) { // Covers CM and MM
                modified.add(f); // Covers M
            }
        }

    }

    /**
     *
     * This method takes previously unnotified/unrequested changes (creates, deletes, modifies) and
     * compares it against the latest filesystem scan. Both the current scan results and the
     * unnotified changes are then corrected so that only currently valid choices remain.
     *
     * @param unnotifiedFileCreates
     * @param unnotifiedFileDeletes
     * @param unnotifiedFileModifies
     * @param created
     * @param deleted
     * @param modified
     */
    private void resolveChangesForExternalScan(Set<File> unnotifiedFileCreates,
                                               Set<File> unnotifiedFileDeletes,
                                               Set<File> unnotifiedFileModifies,
                                               Set<File> created,
                                               Set<File> deleted,
                                               Set<File> modified) {

        // Note any change to the way state is handled in this code must also be
        // updated in resolveChangesForScheduledScan

        // State change loop to track the status of changed files.
        // D = Delete, C = Create, M = modified, - = not in any list, X = not possible
        // Example, "CM = C" would mean create followed by a modify would
        // result in the file being in the create list
        // C = C
        // D = D
        // M = M
        // CC = X
        // CD = -
        // CM = C
        // DC = M
        // DD = X
        // DM = X
        // MC = X
        // MD = D
        // MM = M

        // Use Iterator so we can safely remove elements from Set during iteration
        for (Iterator<File> i = unnotifiedFileCreates.iterator(); i.hasNext();) {
            File file = i.next();
            if (deleted.contains(file)) {
                // Case CD
                deleted.remove(file);
                i.remove();
            }

            if (modified.contains(file)) {
                // Case CM
                modified.remove(file);
            }
        }

        // Use Iterator so we can safely remove elements from Set during iteration
        for (Iterator<File> i = unnotifiedFileModifies.iterator(); i.hasNext();) {
            File file = i.next();
            if (deleted.contains(file)) {
                // Case MD
                deleted.remove(file);
                i.remove();
                unnotifiedFileDeletes.add(file);
            }
        }

        // Use Iterator so we can safely remove elements from Set during iteration
        for (Iterator<File> i = unnotifiedFileDeletes.iterator(); i.hasNext();) {
            File file = i.next();
            if (created.contains(file)) {
                // Case DC
                created.remove(file);
                i.remove();
                unnotifiedFileModifies.add(file);
            }
        }
    }

    /**
     *
     * This method takes the set of all available filesystem changes that have occurred
     * and then filters the set down to only the notified/requested changes and the parents
     * of the notified/requested changes.
     *
     * @param availableChanges
     * @param notifiedChanges
     * @param unnotifiedChanges
     */
    private void filterSets(Set<File> availableChanges, Set<File> notifiedChanges, Set<File> unnotifiedChanges) {

        // Use Iterator so we can safely remove elements from Set during iteration
        for (Iterator<File> i = availableChanges.iterator(); i.hasNext();) {
            File fileChange = i.next();
            if ((!!!notifiedChanges.contains(fileChange) && !!!isParentFile(notifiedChanges, fileChange))) {
                i.remove();
                unnotifiedChanges.add(fileChange);
            }
        }
    }

    /**
     * @param notifiedChanges
     * @param fileChange
     * @return
     */
    private boolean isParentFile(Set<File> notifiedChanges, File fileChange) {

        for (File thisUpdate : notifiedChanges) {
            try {
                //only directories can be parents of notifications, but deletions will be gone, so we can't determine if they were..
                if (fileChange.isDirectory() || !fileChange.exists()) {
                    //get the path of the directory, and slash terminate it if not already done.
                    String testFilePath = fileChange.getCanonicalPath();
                    if (!testFilePath.endsWith(File.separator)) {
                        testFilePath += File.separator;
                    }
                    //if actual creation is a parent in the hierarchy for this notification, retain the actual creation.
                    if (thisUpdate.getCanonicalPath().startsWith(testFilePath)) {
                        return true;
                    }
                }
            } catch (IOException e) {
                // getCanonicalPath failed..
                // do not add the file to the parents list.
            }
        }
        return false;
    }

    /**
     * Find changes to monitored resources.
     * Not thread safe: please ensure you're calling this only from
     * one thread (i.e. within the scanLock)
     *
     * @param created the list to which created files will be added
     * @param deleted the list to which deleted files will be added
     * @param modified the list to which modified files will be added
     */
    private void scanForUpdates(Collection<File> created, Collection<File> deleted, Collection<File> modified) {
        for (UpdateMonitor m : updateMonitors.keySet()) {
            try {
                m.scanForUpdates(created, modified, deleted);
            } catch (RuntimeException e) {
                // Don't let one monitor mess up the others, but do FFDC any internal issues
                // FFDC will be auto-generated
            }
        }
    }

    /**
     * @param unitString Value of {@link FileMonitor#MONITOR_INTERVAL} property
     * @return parsed value
     */
    @Trivial
    protected static TimeUnit parseTimeUnit(String unitString, String fullValue) {
        if ("ms".equalsIgnoreCase(unitString)) {
            return TimeUnit.MILLISECONDS;
        } else if ("s".equalsIgnoreCase(unitString)) {
            return TimeUnit.SECONDS;
        } else if ("m".equalsIgnoreCase(unitString)) {
            return TimeUnit.MINUTES;
        } else if ("h".equalsIgnoreCase(unitString)) {
            return TimeUnit.HOURS;
        }
        Tr.warning(tc, "badInterval", FileMonitor.MONITOR_INTERVAL, fullValue);
        throw new IllegalArgumentException("Invalid time unit (" + unitString + ") from " + fullValue);
    }

    /**
     * @param intervalString
     * @return parsed value
     */
    @Trivial
    protected static long parseInterval(String intervalString, String fullValue) {
        try {
            return Long.parseLong(intervalString);
        } catch (NumberFormatException nfe) {
            Tr.warning(tc, "badInterval", FileMonitor.MONITOR_INTERVAL, fullValue);
            throw new IllegalArgumentException("Invalid interval (" + intervalString + ") from " + fullValue);
        }
    }

    /**
     * Processes file refresh operations for specific listeners.
     *
     * @param doFilterPaths The filter indicator. If true, input paths are filtered against pending file events.
     *            If false, all pending file events are processed.
     * @param listenerFilter The filter string that allows only those listeners with a matching id to be called to process the event.
     */
    void processFileRefresh(boolean doFilterPaths, String listenerFilter) {
        externalScan(null, null, null, doFilterPaths, listenerFilter);
    }

}
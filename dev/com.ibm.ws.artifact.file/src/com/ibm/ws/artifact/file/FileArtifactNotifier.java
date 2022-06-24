/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.file;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.artifact.ArtifactListenerSelector;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.DefaultArtifactNotification;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 *
 */
public class FileArtifactNotifier implements ArtifactNotifier {
    private final ArtifactContainer root;
    private final ContainerFactoryHolder cfh;

    private final Hashtable<String, Object> serviceProperties = new Hashtable<String, Object>();
    private ServiceRegistration<FileMonitor> service = null;
    private ServiceRegistration<FileMonitor> nonRecurseService = null;
    private final Hashtable<String, Object> nonRecurseServiceProperties = new Hashtable<String, Object>();

    private final Map<String, Collection<ArtifactListenerSelector>> listeners;
    private final Map<String, Collection<ArtifactListenerSelector>> nonRecurselisteners;
    private final Set<String> pathsBeingMonitored;
    private final Set<String> nonRecursePathsBeingMonitored;

    private final String rootAbsolutePath;

    private Long interval;
    private String notificationType;

    public FileArtifactNotifier(ArtifactContainer root, ContainerFactoryHolder cfh, String absPath) {
        this.root = root;
        this.rootAbsolutePath = PathUtils.fixPathString(absPath);
        this.cfh = cfh;
        this.listeners = new ConcurrentHashMap<String, Collection<ArtifactListenerSelector>>();
        this.nonRecurselisteners = new ConcurrentHashMap<String, Collection<ArtifactListenerSelector>>();
        this.pathsBeingMonitored = new HashSet<String>();
        this.nonRecursePathsBeingMonitored = new HashSet<String>();
    }

    private void verifyTargets(ArtifactNotification target) throws IllegalArgumentException {
        if (target.getContainer().getRoot() != root) {
            throw new IllegalArgumentException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean registerForNotifications(ArtifactNotification targets, ArtifactListener callbackObject) throws IllegalArgumentException {
        verifyTargets(targets);

        Set<String> pathsToMonitor = new HashSet<String>();
        Set<String> nonRecursePathsToMonitor = new HashSet<String>();

        for (String path : targets.getPaths()) {
            ArtifactListenerSelector artifactSelectorCallback = new ArtifactListenerSelector(callbackObject);
            addTarget(path, artifactSelectorCallback, pathsToMonitor, nonRecursePathsToMonitor);
        }

        updateMonitoredPaths(pathsToMonitor, null, nonRecursePathsToMonitor, null);

        return true;
    }

    @Override
    public synchronized boolean removeListener(ArtifactListener callbackObject) {
        // Wrap the input artifact listener in a ArtifactListenerSelector.
        ArtifactListenerSelector listenerSelectorCallbackObject = new ArtifactListenerSelector(callbackObject);

        //this isn't too frequent an operation, so the implementation is a little unoptimal ;p
        boolean success = false;

        //find all the places where the listener is registered..
        //2 pass. pass#1 find affected paths.
        Set<String> pathsToRemove = new HashSet<String>();
        for (Map.Entry<String, Collection<ArtifactListenerSelector>> listenersByPath : listeners.entrySet()) {
            for (ArtifactListenerSelector listener : listenersByPath.getValue()) {
                if (listener.equals(listenerSelectorCallbackObject)) {
                    pathsToRemove.add(listenersByPath.getKey());
                }
            }
        }

        Set<String> pathsToReallyRemove = new HashSet<String>(pathsToRemove.size());
        //2 pass. pass#2 process affected paths.
        for (String path : pathsToRemove) {
            Collection<ArtifactListenerSelector> listenersForPath = listeners.get(path);
            if (listenersForPath != null) {
                if (listenersForPath.size() == 1) {
                    //only person listening to this path just left..
                    listeners.remove(path);
                    pathsToReallyRemove.add(path);
                } else {
                    //other parties still care about this path..
                    listenersForPath.remove(listenerSelectorCallbackObject);
                }
            }
        }

        //find all the places where the listener is registered..
        //2 pass. pass#1 find affected paths.
        Set<String> nonRecursePathsToRemove = new HashSet<String>();
        for (Map.Entry<String, Collection<ArtifactListenerSelector>> listenersByPath : nonRecurselisteners.entrySet()) {
            for (ArtifactListenerSelector listener : listenersByPath.getValue()) {
                if (listener.equals(listenerSelectorCallbackObject)) {
                    nonRecursePathsToRemove.add(listenersByPath.getKey());
                }
            }
        }

        Set<String> nonRecursePathsToReallyRemove = new HashSet<String>(pathsToRemove.size());
        //2 pass. pass#2 process affected paths.
        for (String path : nonRecursePathsToRemove) {
            Collection<ArtifactListenerSelector> listenersForPath = nonRecurselisteners.get(path);
            if (listenersForPath != null) {
                if (listenersForPath.size() == 1) {
                    //only person listening to this path just left..
                    nonRecurselisteners.remove(path);
                    nonRecursePathsToReallyRemove.add(path);
                } else {
                    //other parties still care about this path..
                    listenersForPath.remove(listenerSelectorCallbackObject);
                }
            }
        }

        //get rid of anything that no-one is listening to anymore..
        updateMonitoredPaths(null, pathsToReallyRemove, null, nonRecursePathsToReallyRemove);

        //the success is if we removed the listener, which is based on if
        //we found it in the set..
        if (pathsToRemove.size() > 0 || nonRecursePathsToReallyRemove.size() > 0) {
            success = true;
        }
        return success;
    }

    private void addTarget(String path, ArtifactListenerSelector listener, Set<String> pathsToMonitor, Set<String> nonRecursePathsToMonitor) {
        Map<String, Collection<ArtifactListenerSelector>> l = listeners;
        boolean nonRecurse = false;
        if (path.startsWith("!")) {
            path = path.substring(1);
            nonRecurse = true;
            l = nonRecurselisteners;
        }

        boolean pathIsNew = true;
        for (String lpath : l.keySet()) {
            if (path.equals(lpath) || (!nonRecurse && path.startsWith(lpath + "/"))) {
                pathIsNew = false;
            }
        }

        Collection<ArtifactListenerSelector> list = l.get(path);
        if (list == null) {
            list = new ConcurrentLinkedQueue<ArtifactListenerSelector>();
            l.put(path, list);
        }
        list.add(listener);

        if (pathIsNew) {
            if (nonRecurse) {
                nonRecursePathsToMonitor.add(path);
            } else {
                pathsToMonitor.add(path);
            }
        }
    }

    private void rebuildPaths() {
        pathsBeingMonitored.clear();
        pathsBeingMonitored.addAll(listeners.keySet());
    }

    private void updateMonitoredPaths(Set<String> pathsToAdd, Set<String> pathsToRemove, Set<String> nonRecursePathsToAdd, Set<String> nonRecursePathsToRemove) {
        boolean updateNeeded = false;
        if (pathsToAdd != null && !pathsToAdd.isEmpty()) {
            pathsBeingMonitored.addAll(pathsToAdd);
            updateNeeded = true;
        }
        if (pathsToRemove != null && !pathsToRemove.isEmpty()) {
            //removal is tricky, as the path space in pathsBeingMonitored is collapsed
            //with subpaths having been removed if parents were being monitored.
            //so before we can remove the requested paths, we have to rebuild the path space.
            rebuildPaths();
            //now we can remove the paths, the path space will be collapsed again before the
            //service is updated.
            //Note that currently, the paths are already removed, as the listener set we rebuild
            //the paths from has already removed the affected paths. We keep this line for
            //safety, as it's relatively cheap.
            pathsBeingMonitored.removeAll(pathsToRemove);
            updateNeeded = true;
        }
        if (updateNeeded) {
            collapsePaths();
            updateFileMonitorService();
        }

        boolean recurseUpdateNeeded = false;
        if (nonRecursePathsToAdd != null && !nonRecursePathsToAdd.isEmpty()) {
            if (nonRecursePathsBeingMonitored.addAll(nonRecursePathsToAdd)) {
                recurseUpdateNeeded = true;
            }
        }
        if (nonRecursePathsToRemove != null && !nonRecursePathsToRemove.isEmpty()) {
            if (nonRecursePathsBeingMonitored.removeAll(nonRecursePathsToRemove)) {
                recurseUpdateNeeded = true;
            }
        }
        if (recurseUpdateNeeded) {
            updateNonRecurseFileMonitorService();
        }
    }

    private void collapsePaths() {
        //file monitor will listen recursively to dirs, so no point in listening to
        //children and to parents..
        Set<String> subPathsToRemove = new HashSet<String>();
        //compare each path, against all the others, mark each of the others for
        //removal if it is a subPath. Additionally, do not process identified subPaths.
        for (String path : pathsBeingMonitored) {
            if (!subPathsToRemove.contains(path)) {
                for (String testAgainst : pathsBeingMonitored) {
                    if (!subPathsToRemove.contains(testAgainst)) {
                        //append a / so the compare will match subpaths only.
                        String pathToCompare = path.equals("/") ? "/" : (path + "/");
                        //skip self..
                        if (path != testAgainst && path.length() != testAgainst.length() && testAgainst.startsWith(pathToCompare)) {
                            subPathsToRemove.add(testAgainst);
                        }
                    }
                }
            }
        }
        pathsBeingMonitored.removeAll(subPathsToRemove);
    }

    static class FileArtifactMonitor implements com.ibm.ws.kernel.filemonitor.FileMonitor {

        FileArtifactNotifier owner;
        boolean recurse;

        public FileArtifactMonitor(FileArtifactNotifier owner, boolean recurse) {
            this.owner = owner;
            this.recurse = recurse;
        }

        /** {@inheritDoc} */
        @Override
        public void onBaseline(Collection<File> baseline) {
            owner.initComplete(baseline, recurse);
        }

        /** {@inheritDoc} */
        @Override
        public void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
            owner.scanComplete(createdFiles, modifiedFiles, deletedFiles, recurse);
        }

        /** {@inheritDoc} */
        @Override
        public void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles, String filter) {
            owner.scanComplete(createdFiles, modifiedFiles, deletedFiles, recurse, filter);
        }
    }

    private final FileArtifactMonitor nonRecurseMonitor = new FileArtifactMonitor(this, false);
    private final FileArtifactMonitor recurseMonitor = new FileArtifactMonitor(this, true);

    @FFDCIgnore(IllegalStateException.class)
    private synchronized void updateFileMonitorService() {

        if (service == null) {
            if (FrameworkState.isStopping()) {
                return;
            }
            serviceProperties.put(Constants.SERVICE_VENDOR, "IBM");
            // Adding INTERNAL parameter MONITOR_IDENTIFICATION_NAME to identify this monitor.
            serviceProperties.put(com.ibm.ws.kernel.filemonitor.FileMonitor.MONITOR_IDENTIFICATION_NAME, "com.ibm.ws.kernel.monitor.artifact");

            Long newInterval = 5000L; //5 seconds default
            if (interval != null) {
                newInterval = interval;
            }
            serviceProperties.put(FileMonitor.MONITOR_INTERVAL, "" + newInterval + "ms");
            String type = notificationType;
            if (type == null) {
                type = FileMonitor.MONITOR_TYPE_TIMED;
            }
            serviceProperties.put(FileMonitor.MONITOR_TYPE, type);

            //upconvert the set to absolute paths under root.
            Set<String> absDirPathsToMonitor = new HashSet<String>(pathsBeingMonitored.size());
            Set<String> absFilePathsToMonitor = new HashSet<String>(pathsBeingMonitored.size());
            for (String path : pathsBeingMonitored) {
                String absPath = PathUtils.fixPathString(new File(this.rootAbsolutePath, path));
                //attempting to see if file monitor will tell us of monitored dir creation
                //wondering if removing the trailing / from the path will make it check it as a file..
                String filePath = absPath.endsWith(File.separator) ? absPath.substring(0, absPath.length() - 1) : absPath;
                absDirPathsToMonitor.add(absPath);
                absFilePathsToMonitor.add(filePath);
            }
            serviceProperties.put(FileMonitor.MONITOR_DIRECTORIES, absDirPathsToMonitor);
            serviceProperties.put(FileMonitor.MONITOR_FILES, absFilePathsToMonitor);

            // needs to be a recursive search so we can watch subdirs for changes
            serviceProperties.put(FileMonitor.MONITOR_RECURSE, true);

            // set the monitor to also include the monitored dir in the result set.
            serviceProperties.put(FileMonitor.MONITOR_INCLUDE_SELF, true);

            // find all types of file (including folders)
            serviceProperties.put(FileMonitor.MONITOR_FILTER, ".*");

            try {
                BundleContext ctx = cfh.getBundleContext();
                service = ctx.registerService(FileMonitor.class, recurseMonitor, serviceProperties);
            } catch (IllegalStateException ise) {
                // Artifact bundle has stopped
            }
        } else {
            if (pathsBeingMonitored.size() > 0) {
                //upconvert the set to absolute paths under root.
                Set<String> absDirPathsToMonitor = new HashSet<String>(pathsBeingMonitored.size());
                Set<String> absFilePathsToMonitor = new HashSet<String>(pathsBeingMonitored.size());
                for (String path : pathsBeingMonitored) {
                    String absPath = PathUtils.fixPathString(new File(this.rootAbsolutePath, path));
                    String filePath = absPath.endsWith(File.separator) ? absPath.substring(0, absPath.length() - 1) : absPath;
                    absDirPathsToMonitor.add(absPath);
                    absFilePathsToMonitor.add(filePath);
                }
                serviceProperties.put(FileMonitor.MONITOR_DIRECTORIES, absDirPathsToMonitor);
                serviceProperties.put(FileMonitor.MONITOR_FILES, absFilePathsToMonitor);

                service.setProperties(serviceProperties);
            } else {
                try {
                    service.unregister();
                } catch (IllegalStateException ise) {
                    // Artifact bundle has stopped
                }
                service = null;
            }
        }
    }

    @FFDCIgnore(IllegalStateException.class)
    private synchronized void updateNonRecurseFileMonitorService() {

        if (nonRecurseService == null) {
            if (FrameworkState.isStopping()) {
                return;
            }
            BundleContext ctx = cfh.getBundleContext();
            nonRecurseServiceProperties.put(Constants.SERVICE_VENDOR, "IBM");
            //Adding INTERNAL parameter MONITOR_IDENTIFICATION_NAME to identify this monitor
            nonRecurseServiceProperties.put(com.ibm.ws.kernel.filemonitor.FileMonitor.MONITOR_IDENTIFICATION_NAME, "com.ibm.ws.kernel.monitor.artifact");

            Long newInterval = 5000L; //5 seconds default
            if (interval != null) {
                newInterval = interval;
            }
            nonRecurseServiceProperties.put(FileMonitor.MONITOR_INTERVAL, "" + newInterval + "ms");
            String type = notificationType;
            if (type == null) {
                type = FileMonitor.MONITOR_TYPE_TIMED;
            }
            nonRecurseServiceProperties.put(FileMonitor.MONITOR_TYPE, type);

            //upconvert the set to absolute paths under root.
            Set<String> absDirPathsToMonitor = new HashSet<String>(nonRecursePathsBeingMonitored.size());
            Set<String> absFilePathsToMonitor = new HashSet<String>(nonRecursePathsBeingMonitored.size());
            for (String path : nonRecursePathsBeingMonitored) {
                String absPath = PathUtils.fixPathString(new File(this.rootAbsolutePath, path));
                String filePath = absPath.endsWith(File.separator) ? absPath.substring(0, absPath.length() - 1) : absPath;
                absDirPathsToMonitor.add(absPath);
                absFilePathsToMonitor.add(filePath);
            }
            nonRecurseServiceProperties.put(FileMonitor.MONITOR_DIRECTORIES, absDirPathsToMonitor);
            nonRecurseServiceProperties.put(FileMonitor.MONITOR_FILES, absFilePathsToMonitor);

            // must not be a recursive search
            nonRecurseServiceProperties.put(FileMonitor.MONITOR_RECURSE, false);

            // set the monitor to also include the monitored dir in the result set.
            nonRecurseServiceProperties.put(FileMonitor.MONITOR_INCLUDE_SELF, true);

            // find all types of file (including folders)
            nonRecurseServiceProperties.put(FileMonitor.MONITOR_FILTER, ".*");
            try {
                nonRecurseService = ctx.registerService(FileMonitor.class, nonRecurseMonitor, nonRecurseServiceProperties);
            } catch (IllegalStateException ise) {
                // Artifact bundle stopped
            }
        } else {
            if (pathsBeingMonitored.size() > 0) {
                //upconvert the set to absolute paths under root.
                Set<String> absDirPathsToMonitor = new HashSet<String>(nonRecursePathsBeingMonitored.size());
                Set<String> absFilePathsToMonitor = new HashSet<String>(nonRecursePathsBeingMonitored.size());
                for (String path : pathsBeingMonitored) {
                    String absPath = PathUtils.fixPathString(new File(this.rootAbsolutePath, path));
                    String filePath = absPath.endsWith(File.separator) ? absPath.substring(0, absPath.length() - 1) : absPath;
                    absDirPathsToMonitor.add(absPath);
                    absFilePathsToMonitor.add(filePath);
                }
                nonRecurseServiceProperties.put(FileMonitor.MONITOR_DIRECTORIES, absDirPathsToMonitor);
                nonRecurseServiceProperties.put(FileMonitor.MONITOR_FILES, absFilePathsToMonitor);

                nonRecurseService.setProperties(nonRecurseServiceProperties);
            } else {
                try {
                    nonRecurseService.unregister();
                } catch (IllegalStateException ise) {
                    // Artifact bundle stopped
                }
                nonRecurseService = null;
            }
        }
    }

    public void initComplete(Collection<File> baseline, boolean recurse) {
        //No-op.
    }

    //convert java.io.files into relative paths under the root container.
    private Set<String> convertAbsToRelative(Collection<File> files) {
        Set<String> result = new HashSet<String>();
        for (File f : files) {
            String fAbsPath = PathUtils.fixPathString(f); //assumption here that f is a dir, and path will end in File.seperator
            if (fAbsPath.startsWith(rootAbsolutePath)) {
                String absPath = fAbsPath; // canonicalPath(f);
                absPath = absPath.substring(rootAbsolutePath.length());
                if ("\\".equals(File.separator)) {
                    absPath = absPath.replace('\\', '/');
                }
                //handle / case. (path will have been the same, so the substring created emptystring)
                if (absPath.length() == 0) {
                    absPath = "/";
                }
                result.add(absPath);
            } else
                throw new IllegalStateException(fAbsPath + " " + rootAbsolutePath);
        }
        return result;
    }

    //convert paths starting with prefix into notificatons
    private ArtifactNotification collectNotificationsForPrefix(String prefix, Set<String> paths) {
        Set<String> gatheredPaths = new HashSet<String>();
        if ("/".equals(prefix)) {
            gatheredPaths.addAll(paths);
        } else {
            for (String path : paths) {
                if (path.startsWith(prefix + "/") || path.equals(prefix))
                    gatheredPaths.add(path);
            }
        }
        return new DefaultArtifactNotification(root, gatheredPaths);
    }

    public void scanComplete(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles, boolean recurse) {
        scanComplete(createdFiles, modifiedFiles, deletedFiles, recurse, null);
    }

    public void scanComplete(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles, boolean recurse, String filter) {

        //the monitor for this File Container has called us back..
        Map<String, Collection<ArtifactListenerSelector>> l = recurse ? this.listeners : this.nonRecurselisteners;

        //process the Files back to relative paths within this container.
        Set<String> created = convertAbsToRelative(createdFiles);
        Set<String> modified = convertAbsToRelative(modifiedFiles);
        Set<String> deleted = convertAbsToRelative(deletedFiles);

        for (Map.Entry<String, Collection<ArtifactListenerSelector>> listenersForPath : l.entrySet()) {
            String path = listenersForPath.getKey();
            ArtifactNotification createdForPath = collectNotificationsForPrefix(path, created);
            ArtifactNotification modifiedForPath = collectNotificationsForPrefix(path, modified);
            ArtifactNotification deletedForPath = collectNotificationsForPrefix(path, deleted);
            //only notify if we have paths to notify for!
            if (!createdForPath.getPaths().isEmpty() || !modifiedForPath.getPaths().isEmpty() || !deletedForPath.getPaths().isEmpty()) {
                for (ArtifactListenerSelector listener : listenersForPath.getValue()) {
                    listener.notifyEntryChange(createdForPath, deletedForPath, modifiedForPath, filter);
                }
            }
        }

        //assumption #1:
        //  File Artifact Impl if monitoring a dir, represents the entire dir. (no fake artifact roots).
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean setNotificationOptions(long interval, boolean useMBean) {
        //return false if current settings match args.
        Long compareInterval = Long.valueOf(interval);
        if (compareInterval.equals(this.interval) && ((useMBean && FileMonitor.MONITOR_TYPE_EXTERNAL.equals(this.notificationType))
                                                      ||
                                                      (!useMBean && FileMonitor.MONITOR_TYPE_TIMED.equals(this.notificationType)))) {
            return true;
        }
        this.interval = compareInterval;
        this.notificationType = useMBean ? FileMonitor.MONITOR_TYPE_EXTERNAL : FileMonitor.MONITOR_TYPE_TIMED;

        //only issue the update if we have a service registered.
        if (this.service != null) {
            updateFileMonitorService();
            updateNonRecurseFileMonitorService();
        }

        return true;
    }
}

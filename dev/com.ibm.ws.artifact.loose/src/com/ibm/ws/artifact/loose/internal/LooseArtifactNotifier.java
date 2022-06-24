/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.loose.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.artifact.ArtifactListenerSelector;
import com.ibm.ws.artifact.loose.internal.LooseArchive.EntryInfo;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.DefaultArtifactNotification;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 *
 */
public class LooseArtifactNotifier implements ArtifactNotifier, com.ibm.ws.kernel.filemonitor.FileMonitor {

    private final LooseArchive root;
    private final List<EntryInfo> entries;

    private final Hashtable<String, Object> serviceProperties = new Hashtable<String, Object>();
    private final Hashtable<String, Object> nonRecurseServiceProperties = new Hashtable<String, Object>();

    private Long interval;
    private String notificationType;
    private final BundleContext ctx;
    private ServiceRegistration<FileMonitor> service = null;
    private ServiceRegistration<FileMonitor> nonRecurseService = null;

    private final NavigableMap<String, Set<String>> filesToMonitor = new TreeMap<String, Set<String>>(new PathUtils.PathComparator());
    private final Set<String> filesBeingMonitored = new HashSet<String>();
    private final NavigableMap<String, Set<String>> dirsToMonitor = new TreeMap<String, Set<String>>(new PathUtils.PathComparator());
    private final Set<String> dirsBeingMonitored = new HashSet<String>();

    private class Registration {
        ArtifactListenerSelector listener;
        Collection<String> paths;
    }

    private final Collection<Registration> listeners = new CopyOnWriteArrayList<Registration>();
    private final Set<String> pathsBeingMonitored = new HashSet<String>();

    private final LooseArtifactNotifier parent;
    // children keeps the reference to LooseArtifactNotifier for nested LooseArchive
    private final Map<EntryInfo, LooseArtifactNotifier> children = new HashMap<EntryInfo, LooseArtifactNotifier>();
    private final Collection<LooseArtifactNotifier> enabledChildren = new ArrayList<LooseArtifactNotifier>();
    private final String pathInParent;

    //this notifier works a bit differently to the others, as the information it requires to initiate
    //the filemonitors, is only available long after the construction of the LooseArchive to which it belongs.
    //thus for this Notifier, the creation is delayed until the 'getArtifactNotifier' call is placed to the owning LooseArchive.
    public LooseArtifactNotifier(LooseArchive owner, List<EntryInfo> entries, BundleContext ctx, LooseArtifactNotifier parent, String pathInParent) {
        this.root = owner;
        this.entries = entries;
        this.ctx = ctx;
        this.parent = parent;
        this.pathInParent = pathInParent;
        //build a map of virtual location to physical file/dirs to be notified.
        for (EntryInfo info : entries) {
            String path = info.getVirtualLocation();
            Set<String> files = filesToMonitor.get(path);
            if (files == null) {
                files = new HashSet<String>();
                filesToMonitor.put(path, files);
            }
            Set<String> dirs = dirsToMonitor.get(path);
            if (dirs == null) {
                dirs = new HashSet<String>();
                dirsToMonitor.put(path, dirs);
            }
            //have this entry info contribute its physical information to the monitored sets.
            info.addMonitoringPaths(files, dirs, path);
        }
    }

    private void verifyTargets(ArtifactNotification targets) throws IllegalArgumentException {
        if (targets.getContainer().getRoot() != root) {
            throw new IllegalArgumentException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean registerForNotifications(ArtifactNotification targets, ArtifactListener callbackObject) throws IllegalArgumentException {
        verifyTargets(targets);
        //remember this listener..
        Registration r = new Registration();
        r.listener = new ArtifactListenerSelector(callbackObject);
        //copy the paths, rather than using the user supplied collection, as we don't trust it to remain immutable.
        Collection<String> srcPaths = targets.getPaths();
        r.paths = new ArrayList<String>(srcPaths.size());

        for (String path : srcPaths) {
            //we store the paths with any  ! prefixes intact.
            r.paths.add(path);
        }
        listeners.add(r);

        //add all the paths requested to the monitored set
        //alternatively here, could use rebuild paths, & clear & rebuild from every listener in listeners.
        pathsBeingMonitored.addAll(r.paths);

        reprocessChildrenNotifiers();

        processMonitoredPathList();

        return true;
    }

    private synchronized void reprocessChildrenNotifiers() {
        for (EntryInfo entry : children.keySet()) {
            LooseArtifactNotifier childNotifier = children.get(entry);
            String monitoredPath = this.getEntryMonitoredPath(entry);
            if (monitoredPath != null) {
                if (!enabledChildren.contains(childNotifier)) {
                    Collection<String> paths = new HashSet<String>();
                    paths.add(monitoredPath);
                    ArtifactNotification artifactNotification = new DefaultArtifactNotification(childNotifier.root, paths);
                    childNotifier.registerForNotifications(artifactNotification, new ArtifactListener() {
                        @Override
                        public void notifyEntryChange(ArtifactNotification added, ArtifactNotification removed, ArtifactNotification modified) {
                            // no operation in this listener as the main action is to notify its parent listener
                            // when the child LooseArchive is changed
                        }
                    });
                    enabledChildren.add(children.get(entry));
                }
            } else {
                if (enabledChildren.contains(childNotifier)) {
                    // We only added one listener for each child notifier
                    childNotifier.removeListener(((Registration) childNotifier.listeners.toArray()[0]).listener.getListener());
                    enabledChildren.remove(childNotifier);
                }
            }
        }
    }

    private String getEntryMonitoredPath(EntryInfo entry) {
        if (entry != null) {
            if (this.pathsBeingMonitored.contains("/")) {
                return "/";
            }
            for (String path : this.pathsBeingMonitored) {
                boolean isRecursive = path.startsWith("!");
                String fixedPath = isRecursive ? path.substring(1) : path;
                if (isRecursive && entry.matches(fixedPath)) {
                    return "!/";
                }

                if (!isRecursive && (entry.matches(fixedPath, false) || entry.isBeneath(fixedPath))) {
                    return "/";
                }
            }
        }
        return null;
    }

    private void processMonitoredPathList() {
        Set<String> fileSubPaths = new HashSet<String>();
        Set<String> dirSubPaths = new HashSet<String>();

        //the root path requires us to monitor the xml file itself if current LooseArtifactNotifier monitors the root
        //LooseArchive (i.e., pathInParent == null)
        //if the xml is removed the loose app is gone.
        if (pathInParent == null && (pathsBeingMonitored.contains("/") || pathsBeingMonitored.contains("!/"))) {
            fileSubPaths.add(root.getXMLFile().getAbsolutePath());
        }

        //the recursive root path of / is a special case
        //this is not a problem, as / means 'everything'..
        if (pathsBeingMonitored.contains("/")) {
            for (Set<String> s : filesToMonitor.values()) {
                fileSubPaths.addAll(s);
            }
            for (Set<String> s : dirsToMonitor.values()) {
                dirSubPaths.addAll(s);
            }
        } else {
            //rebuild the file/dir sets
            for (String path : pathsBeingMonitored) {
                String fixedPath = path.startsWith("!") ? path.substring(1) : path;
                //go through already processed entries to see if we've already processed the one we have now
                //this finds nodes that handle the path, or below the path being searched.
                for (EntryInfo ei : entries) {
                    if (ei.matches(fixedPath, false)) {
                        ei.addMonitoringPaths(fileSubPaths, dirSubPaths, path);
                    }
                }
                //go through already processed entries to see if the current one we have (pathAndName) falls beneath them
                //this finds nodes that live beneath the path being searched.
                for (EntryInfo ei : entries) {
                    if (ei.isBeneath(fixedPath)) {
                        ei.addMonitoringPaths(fileSubPaths, dirSubPaths, path);
                    }
                }
            }
        }

        //clean the sets of any dirty data supplied via the entries
        fileSubPaths.remove(null);
        fileSubPaths.remove("");
        dirSubPaths.remove(null);
        dirSubPaths.remove("");

        //only update the service if this registration has caused the paths changed..
        if (!isEqualsCollection(filesBeingMonitored, fileSubPaths) || !isEqualsCollection(dirsBeingMonitored, dirSubPaths)) {
            filesBeingMonitored.clear();
            filesBeingMonitored.addAll(fileSubPaths);
            dirsBeingMonitored.clear();
            dirsBeingMonitored.addAll(dirSubPaths);
            //update the service
            updateFileMonitorService();
        }
    }

    private boolean isEqualsCollection(Collection<String> left, Collection<String> right) {
        return left.containsAll(right) && right.containsAll(left);
    }

    private void rebuildPaths() {
        pathsBeingMonitored.clear();
        for (Registration r : listeners) {
            pathsBeingMonitored.addAll(r.paths);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean removeListener(ArtifactListener listenerToRemove) {
        ArtifactListenerSelector listenerSelectorToRemove = new ArtifactListenerSelector(listenerToRemove);
        List<Registration> rToRemove = new ArrayList<Registration>();
        for (Registration r : listeners) {
            if (r.listener.equals(listenerSelectorToRemove)) {
                rToRemove.add(r);
            }
        }
        listeners.removeAll(rToRemove);
        rebuildPaths();
        reprocessChildrenNotifiers();
        processMonitoredPathList();
        return rToRemove.size() > 0;
    }

    private synchronized void updateFileMonitorService() {
        //split the files out to recurse & non recurse..
        Set<String> nonRecurseFiles = new HashSet<String>();
        Set<String> recurseFiles = new HashSet<String>();
        Set<String> nonRecurseDirs = new HashSet<String>();
        Set<String> recurseDirs = new HashSet<String>();
        for (String file : filesBeingMonitored) {
            if (file.startsWith("!")) {
                nonRecurseFiles.add(file.substring(1));
            } else {
                recurseFiles.add(file);
            }
        }
        for (String dir : dirsBeingMonitored) {
            if (dir.startsWith("!")) {
                nonRecurseDirs.add(dir.substring(1));
            } else {
                recurseDirs.add(dir);
            }
        }
        updateRecurseFileMonitorService(recurseDirs, recurseFiles);
        updateNonRecurseFileMonitorService(nonRecurseDirs, nonRecurseFiles);
    }

    @FFDCIgnore(IllegalStateException.class)
    private synchronized void updateRecurseFileMonitorService(Set<String> dirs, Set<String> files) {
        if (service == null) {
            serviceProperties.put(Constants.SERVICE_VENDOR, "IBM");
            serviceProperties.put(com.ibm.ws.kernel.filemonitor.FileMonitor.MONITOR_IDENTIFICATION_NAME, "com.ibm.ws.kernel.monitor.artifact");

            Long newInterval = 5000L; //default of 5seconds.
            if (interval != null) {
                newInterval = interval;
            }

            serviceProperties.put(FileMonitor.MONITOR_INTERVAL, "" + newInterval + "ms");
            String type = notificationType;
            if (type == null) {
                type = FileMonitor.MONITOR_TYPE_TIMED;
            }
            serviceProperties.put(FileMonitor.MONITOR_TYPE, type);

            serviceProperties.put(FileMonitor.MONITOR_DIRECTORIES, dirs);
            serviceProperties.put(FileMonitor.MONITOR_FILES, files);

            // needs to be a recursive search so we can watch subdirs for changes
            serviceProperties.put(FileMonitor.MONITOR_RECURSE, true);

            // set the monitor to also include the monitored dir in the result set.
            serviceProperties.put(FileMonitor.MONITOR_INCLUDE_SELF, true);

            // find all types of file (including folders)
            serviceProperties.put(FileMonitor.MONITOR_FILTER, ".*");
            try {
                service = ctx.registerService(FileMonitor.class, this, serviceProperties);
            } catch (IllegalStateException ise) {
                // Artifact bundle stopped
            }
        } else {
            if ((dirs.size() + files.size()) > 0) {
                serviceProperties.put(FileMonitor.MONITOR_DIRECTORIES, dirs);
                serviceProperties.put(FileMonitor.MONITOR_FILES, files);

                service.setProperties(serviceProperties);
            } else {
                try {
                    service.unregister();
                } catch (IllegalStateException ise) {
                    // Artifact bundle stopped
                }
                service = null;
            }
        }
    }

    @FFDCIgnore(IllegalStateException.class)
    private synchronized void updateNonRecurseFileMonitorService(Set<String> dirs, Set<String> files) {
        if (nonRecurseService == null) {
            nonRecurseServiceProperties.put(Constants.SERVICE_VENDOR, "IBM");
            serviceProperties.put(com.ibm.ws.kernel.filemonitor.FileMonitor.MONITOR_IDENTIFICATION_NAME, "com.ibm.ws.kernel.monitor.artifact");

            Long newInterval = 5000L; // 5 seconds default
            if (interval != null) {
                newInterval = interval;
            }
            nonRecurseServiceProperties.put(FileMonitor.MONITOR_INTERVAL, newInterval);
            String type = notificationType;
            if (type == null) {
                type = FileMonitor.MONITOR_TYPE_TIMED;
            }
            nonRecurseServiceProperties.put(FileMonitor.MONITOR_TYPE, type);

            nonRecurseServiceProperties.put(FileMonitor.MONITOR_DIRECTORIES, dirs);
            nonRecurseServiceProperties.put(FileMonitor.MONITOR_FILES, files);

            //set this service to use no recursion..
            nonRecurseServiceProperties.put(FileMonitor.MONITOR_RECURSE, false);

            // set the monitor to also include the monitored dir in the result set.
            nonRecurseServiceProperties.put(FileMonitor.MONITOR_INCLUDE_SELF, true);

            // find all types of file (including folders)
            nonRecurseServiceProperties.put(FileMonitor.MONITOR_FILTER, ".*");

            try {
                nonRecurseService = ctx.registerService(FileMonitor.class, this, nonRecurseServiceProperties);
            } catch (IllegalStateException ise) {
                // Artifact bundle stopped
            }
        } else {
            if ((dirs.size() + files.size()) > 0) {
                nonRecurseServiceProperties.put(FileMonitor.MONITOR_DIRECTORIES, dirs);
                nonRecurseServiceProperties.put(FileMonitor.MONITOR_FILES, files);

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

    /** {@inheritDoc} */
    @Override
    public synchronized boolean setNotificationOptions(long interval, boolean useMBean) {
        for (LooseArtifactNotifier childNotification : children.values()) {
            childNotification.setNotificationOptions(interval, useMBean);
        }

        Long compareInterval = Long.valueOf(interval);
        if (compareInterval.equals(this.interval) && ((useMBean && FileMonitor.MONITOR_TYPE_EXTERNAL.equals(this.notificationType))
                                                      ||
                                                      (!useMBean && FileMonitor.MONITOR_TYPE_TIMED.equals(this.notificationType)))) {
            return true;
        }
        this.interval = compareInterval;
        this.notificationType = useMBean ? FileMonitor.MONITOR_TYPE_EXTERNAL : FileMonitor.MONITOR_TYPE_TIMED;

        //only issue the update if we have a service registered.
        if (this.service != null || this.nonRecurseService != null) {
            updateFileMonitorService();
        }

        return true;
    }

    private ArtifactNotification filterSetByPaths(Set<String> paths, Collection<String> filterBy) {
        Set<String> pathsToAdd = new HashSet<String>();
        for (String path : paths) {
            for (String filter : filterBy) {
                boolean nonRecurse = filter.startsWith("!");
                if (nonRecurse) {
                    filter = filter.substring(1);
                }

                if (filter.equals("/") || path.startsWith(filter + "/") || path.equals(filter)) {
                    if (!nonRecurse) {
                        //easy case, just add path, it starts with the right stuff..
                        pathsToAdd.add(path);
                    } else {
                        //needs a bit more care.. user asked for filter, and immediate children..
                        //strip the filter from the front.. this should always work.. as path has to be at least one char, as does filter.
                        String fragment = path.substring(filter.length());
                        //if resulting fragment has no path seps, notify on it.
                        // <1 lets / exist as first char or not at all
                        if (fragment.indexOf("/") < 1) {
                            pathsToAdd.add(path);
                        }
                    }
                }
            }
        }
        ArtifactNotification result = new DefaultArtifactNotification(root, pathsToAdd);
        return result;
    }

    private Map<String, List<EntryInfo>> buildOwnerMapOfEntryInfo(Collection<File> files) {
        String xmlPath = root.getXMLFile().getAbsolutePath();
        Map<String, List<EntryInfo>> result = new HashMap<String, List<EntryInfo>>();
        Set<String> absPath = new HashSet<String>();
        for (File f : files) {
            //the xmlPath isnt known by the entry infos.. so we handle it here.
            if (f.getAbsolutePath().equals(xmlPath)) {
                result.put("/", null);
            } else {
                //convert notified path using the info inside the entry infos.
                for (EntryInfo info : entries) {
                    absPath.clear();
                    info.addMonitoringPaths(absPath, absPath, info.getVirtualLocation());
                    if (absPath.size() > 0) {
                        String path = f.getAbsolutePath();
                        for (String compare : absPath) {
                            //convert both paths to use just "/" for now..
                            compare = new File(compare).getAbsolutePath().replace(File.separatorChar, '/');
                            path = path.replace(File.separatorChar, '/');

                            if (compare.endsWith(File.separator)) {
                                compare = compare.substring(0, compare.length() - 1);
                            }
                            if (path.startsWith(compare + "/") || path.equals(compare)) {
                                boolean pathEquals = path.equals(compare);
                                //convert the path into a container path, using compare as the root.
                                path = path.substring(compare.length());

                                //path can be empty now, if the path were equals..
                                //but if it were a submatch, we need to add the "/" to maintain
                                //path integrity.
                                if (!pathEquals) {
                                    if (!path.startsWith("/")) {
                                        path = "/" + path;
                                    }
                                }

                                String vloc = info.getVirtualLocation();
                                if (vloc.endsWith("/")) {
                                    vloc = vloc.substring(0, vloc.length() - 1);
                                }
                                //now add on the info location..
                                path = vloc + path;
                                //finally ask the info if the created path is a match
                                //this will filter the excludes
                                if (info.matches(path, false)) {
                                    if (!result.containsKey(path)) {
                                        result.put(path, new ArrayList<EntryInfo>());
                                    }
                                    result.get(path).add(info);
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private Map<String, List<EntryInfo>> buildAffectedMapOfEntryInfo(Map<String, List<EntryInfo>> owner) {
        Map<String, List<EntryInfo>> result = new HashMap<String, List<EntryInfo>>();

        for (String path : owner.keySet()) {
            for (EntryInfo info : entries) {
                if (info.matches(path, false) || info.isBeneath(path)) {
                    if (!result.containsKey(path)) {
                        result.put(path, new ArrayList<EntryInfo>());
                    }
                    result.get(path).add(info);
                }
            }
        }

        return result;
    }

    private enum Operation {
        ADDED, DELETED, MODIFIED
    };

    private void correctNotifications(Set<String> rAdded, Set<String> rModified, Set<String> rDeleted, Map<String, List<EntryInfo>> affectedMap,
                                      Map<String, List<EntryInfo>> ownerMap, Operation op) {
        for (Map.Entry<String, List<EntryInfo>> affected : affectedMap.entrySet()) {
            boolean xmlSpecialCase = false;
            int firstExistingIndex = Integer.MAX_VALUE;
            int firstMatchingIndex = Integer.MAX_VALUE;
            int existingMatchCount = 0;
            String path = affected.getKey();
            List<EntryInfo> ownerList = ownerMap.get(path);
            if (ownerList == null) {
                //special case, means the xml file for this archive was affected.
                xmlSpecialCase = true;
            } else {
                int index = -1;
                for (EntryInfo ei : affected.getValue()) {
                    index++;
                    if (ei.matches(affected.getKey(), true)) {
                        existingMatchCount++;
                        if (firstExistingIndex == Integer.MAX_VALUE) {
                            firstExistingIndex = index;
                        }
                    }
                    if (firstMatchingIndex == Integer.MAX_VALUE && ownerList.contains(ei)) {
                        firstMatchingIndex = index;
                    }
                    //do not bug out early.. as we need to update the existingMatchCount
                }
            }
            if (xmlSpecialCase) {
                //xmlSpecialCase is for the root xml, we just pass the path through.
                switch (op) {
                    case ADDED: {
                        rAdded.add(path);
                        break;
                    }
                    case MODIFIED: {
                        rModified.add(path);
                        break;
                    }
                    case DELETED: {
                        rDeleted.add(path);
                        break;
                    }
                }
            } else {
                //not the xml file, need to assess if the event needs mutating.
                if (firstMatchingIndex == Integer.MAX_VALUE) {
                    //the code does not allow for firstMatching being max value at this stage.
                    FFDCFilter.processException(new IllegalStateException("EntryInfo match not found"), getClass().getName(), "entryInfoMatchNotFound");
                } else {
                    switch (op) {
                        case ADDED: {
                            if (firstExistingIndex < firstMatchingIndex) {
                                //the node that matched, is further along than a node that knows about the file already
                                //so this add is obscured, as the preceeding node will still own the file in the vfs
                            } else if (firstExistingIndex > firstMatchingIndex) {
                                //the node that matched, is before nodes that knew of the file before.
                                //so this add is only a modify, as we already knew about this file.
                                rModified.add(path);
                            } else if (firstExistingIndex == firstMatchingIndex) {
                                //the node that matched, is the first node we find in search order.
                                //this add needs more care.. it is only an add if there were
                                //no other matches for the entry..
                                if (existingMatchCount > 1) {
                                    rModified.add(path);
                                } else {
                                    rAdded.add(path);
                                }
                            } else if (firstExistingIndex == Integer.MAX_VALUE) {
                                //there is no node that could claim to own the path
                                //odd.. we were told the path exists, yet no node agrees..
                                FFDCFilter.processException(new IllegalStateException("No matching EntryInfo for path"), getClass().getName(), "entryInfoMatchNotFoundForAdd");
                            }
                            break;
                        }
                        case DELETED: {
                            if (firstExistingIndex == Integer.MAX_VALUE) {
                                //there is no node remaining that could claim to own the path
                                rDeleted.add(path);
                            } else if (firstExistingIndex < firstMatchingIndex) {
                                //the delete is obscured, because a preceeding ei knows the path still exists.
                            } else if (firstExistingIndex > firstMatchingIndex) {
                                //the match was a preceeding ei, so the delete has uncovered a new version of the path
                                rModified.add(path);
                            } else if (firstExistingIndex == firstMatchingIndex) {
                                //the match came from the same node that claimed it had been deleted
                                //this means although deleted, it's still there.. so report as notified.
                                rModified.add(path);
                            }

                            break;
                        }
                        case MODIFIED: {
                            if (firstExistingIndex == Integer.MAX_VALUE) {
                                //there is no node that could claim to own the path
                                //for a modified, this is an FFDC, as it means the path is gone.
                                FFDCFilter.processException(new IllegalStateException("EntryInfo match not found"), getClass().getName(), "entryInfoMatchNotFoundForModify");
                            } else if (firstExistingIndex < firstMatchingIndex) {
                                //the modify is obscured, because a preceeding ei knows the path still exists.
                            } else if (firstExistingIndex > firstMatchingIndex) {
                                FFDCFilter.processException(new IllegalStateException("EntryInfo match not found"), getClass().getName(), "entryInfoMatchNotFoundForModify2");
                            } else if (firstExistingIndex == firstMatchingIndex) {
                                //the match came from the same node that claimed it had been deleted
                                //this means although deleted, it's still there.. so report as notified.
                                rModified.add(path);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private void constructRelativeNotificationPaths(Set<String> rAdded, Set<String> rModified, Set<String> rDeleted, Collection<File> createdFiles, Collection<File> modifiedFiles,
                                                    Collection<File> deletedFiles) {
        Map<String, List<EntryInfo>> ownerAdded = buildOwnerMapOfEntryInfo(createdFiles);
        Map<String, List<EntryInfo>> ownerModified = buildOwnerMapOfEntryInfo(modifiedFiles);
        Map<String, List<EntryInfo>> ownerDeleted = buildOwnerMapOfEntryInfo(deletedFiles);

        Map<String, List<EntryInfo>> affectedAdded = buildAffectedMapOfEntryInfo(ownerAdded);
        Map<String, List<EntryInfo>> affectedModified = buildAffectedMapOfEntryInfo(ownerModified);
        Map<String, List<EntryInfo>> affectedDeleted = buildAffectedMapOfEntryInfo(ownerDeleted);

        correctNotifications(rAdded, rModified, rDeleted, affectedAdded, ownerAdded, Operation.ADDED);
        correctNotifications(rAdded, rModified, rDeleted, affectedModified, ownerModified, Operation.MODIFIED);
        correctNotifications(rAdded, rModified, rDeleted, affectedDeleted, ownerDeleted, Operation.DELETED);

    }

    /** {@inheritDoc} */
    @Override
    public synchronized void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
        onChange(createdFiles, modifiedFiles, deletedFiles, null);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles, String filter) {
        //scan the collections of absolute paths, and resolve them into sets of vfs locations
        //this involves not just abs->relative translation but also
        // - removing obscured notifications
        // - converting deleted into modified as appropriate
        Set<String> fRelativeAddedSet = new HashSet<String>();
        Set<String> fRelativeModifiedSet = new HashSet<String>();
        Set<String> fRelativeRemovedSet = new HashSet<String>();
        constructRelativeNotificationPaths(fRelativeAddedSet, fRelativeModifiedSet, fRelativeRemovedSet, createdFiles, modifiedFiles, deletedFiles);

        //if any of the sets are not empty, we may need to push a notification to parent.
        if (parent != null && (!fRelativeAddedSet.isEmpty() || !fRelativeModifiedSet.isEmpty() || !fRelativeRemovedSet.isEmpty())) {
            //notify as a modified of pathInParent.
            parent.notifyListenersByPath(Collections.<String> emptySet(), Collections.<String> singleton(pathInParent), Collections.<String> emptySet(), filter);
        }

        //now we notify all our listeners.
        notifyListenersByPath(fRelativeAddedSet, fRelativeModifiedSet, fRelativeRemovedSet, filter);
    }

    private void notifyListenersByPath(Set<String> relativeAddedSet, Set<String> relativeModifedSet, Set<String> relativeRemovedSet, String filter) {
        for (Registration r : listeners) {
            ArtifactNotification added = filterSetByPaths(relativeAddedSet, r.paths);
            ArtifactNotification removed = filterSetByPaths(relativeRemovedSet, r.paths);
            ArtifactNotification modified = filterSetByPaths(relativeModifedSet, r.paths);
            if (added.getPaths().size() > 0 || removed.getPaths().size() > 0 || modified.getPaths().size() > 0) {
                r.listener.notifyEntryChange(added, removed, modified, filter);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onBaseline(Collection<File> baseline) {
        //no-op
    }

    public void addChild(EntryInfo entryInfo, LooseArtifactNotifier notifier) {
        this.children.put(entryInfo, notifier);
    }

    public String getId() {
        return null;
    }
}

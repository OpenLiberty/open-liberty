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
package com.ibm.ws.artifact.zip.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.ZipEntry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactListener;
import com.ibm.wsspi.artifact.DefaultArtifactNotification;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;

/**
 *
 */
public class ZipFileArtifactNotifier implements ArtifactNotifier, FileMonitor, ArtifactListener {

    private final ZipFileContainer root;
    private final ContainerFactoryHolder cfh;

    private final Hashtable<String, Object> serviceProperties = new Hashtable<String, Object>();
    private ServiceRegistration<FileMonitor> service = null;

    private boolean listenerRegistered;

    private final Map<String, Collection<ArtifactListener>> listeners;
    private final Set<String> pathsBeingMonitored;

    private final String rootAbsolutePath;
    private final ArtifactEntry entryInParent;
    private final ArtifactNotification emptyNotification;

    private Long interval;
    private String notificationType;

    //case where we have a zip file being monitored
    public ZipFileArtifactNotifier(ZipFileContainer root, ContainerFactoryHolder cfh, String absPath) {
        this.root = root;
        this.rootAbsolutePath = absPath;
        this.entryInParent = null;
        this.cfh = cfh;
        this.listeners = new ConcurrentHashMap<String, Collection<ArtifactListener>>();
        this.pathsBeingMonitored = new HashSet<String>();
        this.listenerRegistered = false;
        this.emptyNotification = new DefaultArtifactNotification(root, Collections.unmodifiableCollection(Collections.<String> emptySet()));
    }

    //case where the zip file is only available as an entry in the parent container.
    public ZipFileArtifactNotifier(ZipFileContainer root, ContainerFactoryHolder cfh, ArtifactEntry entryInParent) {
        this.root = root;
        this.rootAbsolutePath = null;
        this.entryInParent = entryInParent;
        this.cfh = cfh;
        this.listeners = new ConcurrentHashMap<String, Collection<ArtifactListener>>();
        this.pathsBeingMonitored = new HashSet<String>();
        this.listenerRegistered = false;
        this.emptyNotification = new DefaultArtifactNotification(root, Collections.unmodifiableCollection(Collections.<String> emptySet()));
    }

    private void verifyTargets(ArtifactNotification targets) throws IllegalArgumentException {
        if (targets.getContainer().getRoot() != root) {
            //thrown back to user via the registerForNotifications method.
            //javadoc for that method explicitly includes this exception
            throw new IllegalArgumentException();
        }
    }

    private void rebuildPaths() {
        pathsBeingMonitored.clear();
        pathsBeingMonitored.addAll(listeners.keySet());
    }

    private void updateMonitoredPaths(Set<String> pathsToAdd, Set<String> pathsToRemove) {
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
            updateFileMonitorService();
        }
    }

    private boolean addTarget(String path, ArtifactListener listener) {
        boolean pathIsNew = true;
        for (String lpath : listeners.keySet()) {
            if (path.equals(lpath) || path.startsWith(lpath + "/")) {
                pathIsNew = false;
            }
        }

        Collection<ArtifactListener> list = this.listeners.get(path);
        if (list == null) {
            list = new ConcurrentLinkedQueue<ArtifactListener>();
            this.listeners.put(path, list);
        }
        list.add(listener);

        return pathIsNew;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean registerForNotifications(ArtifactNotification targets, ArtifactListener callbackObject) throws IllegalArgumentException {
        verifyTargets(targets);

        Set<String> pathsToMonitor = new HashSet<String>();

        for (String path : targets.getPaths()) {

            //the zip impl doesnt need to worry about non recursive monitors.. as it doesnt have those overheads.
            if (path.startsWith("!")) {
                path = path.substring(1);
            }

            boolean addToMonitorList = addTarget(path, callbackObject);
            if (addToMonitorList) {
                pathsToMonitor.add(path);
            }
        }

        updateMonitoredPaths(pathsToMonitor, null);

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeListener(ArtifactListener listenerToRemove) {
        //this isn't too frequent an operation, so the implementation is a little unoptimal ;p
        boolean success = false;

        //find all the places where the listener is registered..
        //2 pass. pass#1 find affected paths.
        Set<String> pathsToRemove = new HashSet<String>();
        for (Map.Entry<String, Collection<ArtifactListener>> listenersByPath : listeners.entrySet()) {
            for (ArtifactListener listener : listenersByPath.getValue()) {
                if (listener == listenerToRemove) {
                    pathsToRemove.add(listenersByPath.getKey());
                }
            }
        }
        Set<String> pathsToReallyRemove = new HashSet<String>(pathsToRemove.size());
        //2 pass. pass#2 process affected paths.
        for (String path : pathsToRemove) {
            Collection<ArtifactListener> listenersForPath = listeners.get(path);
            if (listenersForPath.size() == 1) {
                //only person listening to this path just left.. 
                listeners.remove(path);
                pathsToReallyRemove.add(path);
            } else {
                //other parties still care about this path.. 
                listenersForPath.remove(listenerToRemove);
            }
        }

        //get rid of anything that no-one is listening to anymore..
        updateMonitoredPaths(null, pathsToReallyRemove);

        //the success is if we removed the listener, which is based on if
        //we found it in the set.. 
        if (pathsToRemove.size() > 0) {
            success = true;
        }
        return success;

    }

    private synchronized void updateFileMonitorService() {
        //the zip file monitor is either on the zipfile.. 
        //  OR
        //is managed via the artifact listener on the parent entry.
        if (rootAbsolutePath != null) {
            updateFileMonitorServiceForZipFile();
        } else {
            updateArtifactListenerForEntry();
        }

    }

    private synchronized void updateArtifactListenerForEntry() {
        //unlike the file notifier, we only need to..
        // - if anyone is listening to us, we listen to us in the parent.
        // - if no-one is listening to us, we stop listening to the parent.
        if (pathsBeingMonitored.size() > 0 && !listenerRegistered) {
            //we need to hook ourselves to the artifact notifier of the parent.
            ArtifactContainer notificationContainer = entryInParent.getRoot();
            ArtifactNotification notificationForSelfInParent = new DefaultArtifactNotification(notificationContainer, Collections.singleton(entryInParent.getPath()));
            ArtifactNotifier notifierForEntryInParent = notificationContainer.getArtifactNotifier();
            listenerRegistered = notifierForEntryInParent.registerForNotifications(notificationForSelfInParent, this);
            //since a listener just registered, we should kick the 
            //zip container to cause it to build it's intial index.
            //(this can ffdc if the zip has already been deleted.. )
            root.listEntries();
        } else if (pathsBeingMonitored.size() == 0 && listenerRegistered) {
            //unhook us from the artifact notfier of the parent.
            ArtifactContainer notificationContainer = entryInParent.getRoot();
            ArtifactNotifier notifierForEntryInParent = notificationContainer.getArtifactNotifier();
            notifierForEntryInParent.removeListener(this);
        }
    }

    @FFDCIgnore(IllegalStateException.class)
    private synchronized void updateFileMonitorServiceForZipFile() {
        if (service == null) {
            try {
                BundleContext ctx = cfh.getBundleContext();
                serviceProperties.put(Constants.SERVICE_VENDOR, "IBM");

                Long newInterval = 5000L; // 5 seconds .. 
                if (interval != null) {
                    newInterval = interval;
                }
                serviceProperties.put(FileMonitor.MONITOR_INTERVAL, "" + newInterval + "ms");
                String type = notificationType;
                if (type == null) {
                    type = FileMonitor.MONITOR_TYPE_TIMED;
                }
                serviceProperties.put(FileMonitor.MONITOR_TYPE, type);

                //Zip file monitor ONLY monitors the zip file itself.
                serviceProperties.put(FileMonitor.MONITOR_DIRECTORIES, new String[] { rootAbsolutePath });
                serviceProperties.put(FileMonitor.MONITOR_FILES, new String[] { rootAbsolutePath });

                // needs to be a recursive search so we can watch subdirs for changes
                serviceProperties.put(FileMonitor.MONITOR_RECURSE, true);
                // find all types of file (including folders)
                serviceProperties.put(FileMonitor.MONITOR_FILTER, ".*");

                service = ctx.registerService(FileMonitor.class, this, serviceProperties);

                //since a listener just registered, we should kick the 
                //zip container to cause it to build it's intial index.
                //(this can ffdc if the zip has already been deleted.. )
                root.listEntries();
            } catch (IllegalStateException ise) {
                // Artifact bundle stopped
            }
        } else {
            //no need to update paths if the service is already registered, as it's monitoring
            //the zip archive, not paths inside it.
            if (pathsBeingMonitored.size() == 0) {
                try {
                    service.unregister();
                } catch (IllegalStateException ise) {
                    // Artifact bundle stopped
                }
                service = null;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean setNotificationOptions(long interval, boolean useMBean) {
        //we cannot set options if we are using the parent monitor.
        if (rootAbsolutePath == null) {
            return false;
        }

        //return false if current settings match args.
        Long compareInterval = Long.valueOf(interval);
        if (compareInterval.equals(this.interval) && (
            (useMBean && FileMonitor.MONITOR_TYPE_EXTERNAL.equals(this.notificationType))
            ||
            (!useMBean && FileMonitor.MONITOR_TYPE_TIMED.equals(this.notificationType))
            )) {
            return true;
        }
        this.interval = compareInterval;
        this.notificationType = useMBean ? FileMonitor.MONITOR_TYPE_EXTERNAL : FileMonitor.MONITOR_TYPE_TIMED;

        //only issue the update if we have a service registered.
        if (this.service != null) {
            updateFileMonitorService();
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void onBaseline(Collection<File> baseline) {
        //no-op.
    }

    /** {@inheritDoc} */
    @Override
    public void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
        //TODO: confirm the logic here during testing.. perhaps created can occur? what notification is issued for a delete/recreate op?
        //figure out the state from the args..
        boolean modified = false;
        if (modifiedFiles.size() > 0) {
            modified = true;
        }
        if (deletedFiles.size() > 0) {
            //we only monitor one file.. it can't be deleted AND modified!!
            if (modified == true) {
                throw new IllegalStateException(modifiedFiles.toString() + deletedFiles.toString());
            }
            modified = false;
        }
        if (createdFiles.size() > 0) {
            //we only monitor one file, and it can't go away and come back!!

            //      If we want to handle replacing a zip backed container after the zip has been deleted.. 
            //      HERE is where we would start to handle that situation.. 
            //  
            //      We could go up to the ZipFileContainer we have a reference to as this.root, and cause it to reinitialise
            //      based on the new zip data now created. But there are questions that need resolving for this to occur.
            //
            //      - How should existing container/entry instances representing the old jar function after the replace?
            //         - if the zip is the same content, they could probably be fixed up to cope.
            //         - if the zip has new content, it'll be iterable/getable just fine.
            //         - if the zip has less content, instances of the content removed will act in an undefined manner, 
            //           maybe this is acceptable given the existence of the notification api.
            //      - What if the new zip file isn't zip data, it could be
            //         - a file with the same name, but non-zip data (either corrupt, or perhaps incompletely written data?)
            //           all existing entries would need to fail somehow, and the root container also.. 
            //           there is no real suitable failure mechanism for a Container that has gone awol in this manner.
            //         - a directory with the name of the zip
            //           - in an ideal world, things would just keep working magically and delegate to new file container equivs.. 
            //             this is a scary scary (scary!) thing to make happen. 
            //
            //       For now, the decision is, we ignore the recreate
            throw new IllegalStateException(createdFiles.toString());
        }

        notifyAllListeners(modified);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyEntryChange(ArtifactNotification added, ArtifactNotification removed, ArtifactNotification modified) {
        //figure out the state from the args..
        boolean modifiedflag = false;
        if (modified.getPaths().size() > 0) {
            modifiedflag = true;
        }
        if (removed.getPaths().size() > 0) {
            //we only monitor one file.. it can't be deleted AND modified!!
            if (modifiedflag == true) {
                throw new IllegalStateException(modified.toString() + removed.toString());
            }
            modifiedflag = false;
        }
        if (added.getPaths().size() > 0) {
            //we only monitor one file, and it can't go away and come back!!
            throw new IllegalStateException(added.toString());
        }
        notifyAllListeners(modifiedflag);
    }

    private ArtifactNotification collectNotificationsForPath(String pathAndName) {
        Collection<String> gatheredPaths = new ArrayList<String>();
        NavigableMap<String, ZipEntry> allEntries = root.listEntries();
        if ("/".equals(pathAndName)) {
            //root is a special case, it is never represented within the allEntries list, 
            //so we can't use subMap, but since / means everything, we can add the entire set.
            //lastly, allEntries never contains "/" and we need to add that also..
            for (String key : allEntries.keySet()) {
                gatheredPaths.add('/' + key);
            }
            gatheredPaths.add("/");
        } else {
            //Use submap to obtain a set of all paths that would exist at or below the requested path.
            //This works because the allEntries map is sorted using our comparator, which means when we
            //request the subMap from 'path' to 'path+0' we receive the set of all paths which are
            //either below the requested path in the path hierarchy, or are equal to the requested path.
            //
            //"blah" + 0 is the next possible directory name in alphabetical order: 
            //we want to find everything up to but not including the next directory, 
            //so anything *strictly* between "blah" and "blah" + 0 in the order created by the comparator
            //is a child of "blah"
            String firstKey = pathAndName.substring(1); // strip off /
            SortedMap<String, ZipEntry> subEntries = allEntries.subMap(firstKey, firstKey + 0);
            for (String key : subEntries.keySet()) {
                gatheredPaths.add('/' + key);
            }
        }
        return new DefaultArtifactNotification(root, gatheredPaths);
    }

    private void notifyAllListeners(boolean modified) {
        for (Map.Entry<String, Collection<ArtifactListener>> listenersForPath : listeners.entrySet()) {
            ArtifactNotification paths = collectNotificationsForPath(listenersForPath.getKey());
            if (paths.getPaths().size() > 0) {
                for (ArtifactListener listener : listenersForPath.getValue()) {
                    if (modified) {
                        listener.notifyEntryChange(emptyNotification, emptyNotification, paths);
                    } else {
                        listener.notifyEntryChange(emptyNotification, paths, emptyNotification);
                    }
                }
            }
        }
    }
}

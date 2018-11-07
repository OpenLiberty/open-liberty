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
package com.ibm.ws.artifact.overlay.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ibm.ws.artifact.ArtifactListenerSelector;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactListener;
import com.ibm.wsspi.artifact.DefaultArtifactNotification;

/**
 *
 */
public class DirectoryBasedOverlayNotifier implements ArtifactNotifier, com.ibm.ws.artifact.ArtifactNotifierExtension.ArtifactListener {
    private final DirectoryBasedOverlayContainerImpl root;
    private final ArtifactContainer overlayContainer;
    private final ArtifactContainer overlaidContainer;

    private boolean listeningToContainers;

    private final Map<String, Collection<ArtifactListenerSelector>> listeners;
    private final Set<String> pathsMonitored = new HashSet<String>();

    private final DirectoryBasedOverlayNotifier parentNotifier;
    private final String pathOfEntryInParent;

    private String id;

    public DirectoryBasedOverlayNotifier(DirectoryBasedOverlayContainerImpl root, ArtifactContainer artifactContainer, DirectoryBasedOverlayNotifier parent,
                                         ArtifactEntry entryInParent) {
        this.root = root;
        this.overlayContainer = artifactContainer;
        this.overlaidContainer = root.getContainerBeingOverlaid();
        this.listeningToContainers = false;
        this.parentNotifier = parent;
        if (entryInParent != null)
            this.pathOfEntryInParent = entryInParent.getPath();
        else
            this.pathOfEntryInParent = null;
        this.listeners = new ConcurrentHashMap<String, Collection<ArtifactListenerSelector>>();
    }

    private void verifyTargets(ArtifactNotification targets) throws IllegalArgumentException {
        if (targets.getContainer().getRoot() != root) {
            throw new IllegalArgumentException();
        }
    }

    private boolean addTarget(String path, ArtifactListenerSelector listener) {
        boolean pathIsNew = true;
        for (String lpath : listeners.keySet()) {
            if (path.equals(lpath) || path.startsWith(lpath + "/")) {
                pathIsNew = false;
            }
        }

        Collection<ArtifactListenerSelector> list = this.listeners.get(path);
        if (list == null) {
            list = new ConcurrentLinkedQueue<ArtifactListenerSelector>();
            this.listeners.put(path, list);
        }
        list.add(listener);

        return pathIsNew;
    }

    private void collapsePaths(Collection<String> input) {
        //file monitor will listen recursively to dirs, so no point in listening to
        //children and to parents..
        Set<String> subPathsToRemove = new HashSet<String>();
        //compare each path, against all the others, mark each of the others for
        //removal if it is a subPath. Additionally, do not process identified subPaths.
        for (String path : input) {
            if (path.startsWith("!"))
                continue;
            if (!subPathsToRemove.contains(path)) {
                for (String testAgainst : input) {
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
        input.removeAll(subPathsToRemove);
        return;
    }

    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean registerForNotifications(ArtifactNotification targets, ArtifactListener callbackObject) throws IllegalArgumentException {
        verifyTargets(targets);

        //build the aggregate set of paths to monitor from the caller's args.
        //addTarget will always add the listener, but will return false if the
        //path is already in the monitor list..
        Set<String> pathsToMonitor = new HashSet<String>();
        for (String path : targets.getPaths()) {
            //do not remove the ! paths here.. as they must be passed onto the other containers.
            ArtifactListenerSelector artifactSelectorCallback = new ArtifactListenerSelector(callbackObject);
            boolean addToMonitorList = addTarget(path, artifactSelectorCallback);
            if (addToMonitorList) {
                pathsToMonitor.add(path);
            }
        }

        //figure out the new minset of paths to monitor.
        pathsToMonitor.addAll(pathsMonitored);
        collapsePaths(pathsToMonitor);

        //if we're already listening, we need to stop briefly to update the list..
        if (listeningToContainers) {
            overlayContainer.getArtifactNotifier().removeListener(this);
            overlaidContainer.getArtifactNotifier().removeListener(this);
        }
        pathsMonitored.clear();
        pathsMonitored.addAll(pathsToMonitor);
        overlayContainer.getArtifactNotifier().registerForNotifications(new DefaultArtifactNotification(overlayContainer, pathsMonitored), this);
        overlaidContainer.getArtifactNotifier().registerForNotifications(new DefaultArtifactNotification(overlaidContainer, pathsMonitored), this);
        listeningToContainers = true;

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean removeListener(ArtifactListener listenerToRemove) {
        // Wrap the input artifact listener in a ArtifactListenerSelector.
        ArtifactListenerSelector listenerSelectorToRemove = new ArtifactListenerSelector(listenerToRemove);

        //this isn't too frequent an operation, so the implementation is a little unoptimal ;p
        boolean success = false;

        //find all the places where the listener is registered..
        //2 pass. pass#1 find affected paths.
        Set<String> pathsToRemove = new HashSet<String>();
        for (Map.Entry<String, Collection<ArtifactListenerSelector>> listenersByPath : listeners.entrySet()) {
            for (ArtifactListenerSelector listener : listenersByPath.getValue()) {
                if (listener.equals(listenerSelectorToRemove)) {
                    pathsToRemove.add(listenersByPath.getKey());
                }
            }
        }
        //2 pass. pass#2 process affected paths.
        for (String path : pathsToRemove) {
            Collection<ArtifactListenerSelector> listenersForPath = listeners.get(path);
            if (listenersForPath.size() == 1) {
                //only person listening to this path just left..
                listeners.remove(path);
            } else {
                //other parties still care about this path..
                listenersForPath.remove(listenerSelectorToRemove);
            }
        }

        pathsMonitored.clear();
        pathsMonitored.addAll(listeners.keySet());
        collapsePaths(pathsMonitored);
        if (listeningToContainers) {
            overlayContainer.getArtifactNotifier().removeListener(this);
            overlaidContainer.getArtifactNotifier().removeListener(this);
        }

        if (pathsMonitored.size() > 0) {
            overlayContainer.getArtifactNotifier().registerForNotifications(new DefaultArtifactNotification(overlayContainer, pathsMonitored), this);
            overlaidContainer.getArtifactNotifier().registerForNotifications(new DefaultArtifactNotification(overlaidContainer, pathsMonitored), this);
        } else {
            listeningToContainers = false;
        }

        if (pathsToRemove.size() > 0) {
            success = true;
        }
        return success;
    }

    /** {@inheritDoc} */
    @Override
    public boolean setNotificationOptions(long interval, boolean useMBean) {
        //can just pass these thru, the other impl's will not trigger listening until a registration is placed.
        overlaidContainer.getArtifactNotifier().setNotificationOptions(interval, useMBean);
        overlayContainer.getArtifactNotifier().setNotificationOptions(interval, useMBean);
        return true;
    }

    private Set<String> filterOverlaidPaths(Collection<String> notificationPaths) {
        Set<String> paths = new HashSet<String>();
        for (String path : notificationPaths) {
            //do not strip / if present =)
            if (!root.isOverlaid(path) && !root.isMasked(path)) {
                paths.add(path);
            }
        }
        return paths;
    }

    private Set<String> filterExistingPaths(Collection<String> notificationPaths) {
        Set<String> paths = new HashSet<String>();
        for (String path : notificationPaths) {
            //we can't mask, or overlay "/"
            if (!"/".equals(path)) {
                //can't pass "/" to getEntry, is now filtered above.
                if (root.getContainerBeingOverlaid().getEntry(path) != null && !root.isMasked(path)) {
                    paths.add(path);
                }
            }
        }
        return paths;
    }

    private Set<String> filterMaskedPaths(Collection<String> notificationPaths) {
        Set<String> paths = new HashSet<String>();
        for (String path : notificationPaths) {
            //do not strip / if present =)
            if (!root.isMasked(path)) {
                paths.add(path);
            }
        }
        return paths;
    }

    /** {@inheritDoc} */
    @Override
    public void notifyEntryChange(ArtifactNotification added, ArtifactNotification removed, ArtifactNotification modified) {
        notifyEntryChange(added, removed, modified, null);
    }

    /** {@inheritDoc} */
    @Override
    //TODO: invoke parent overlay notifyEntryChange for the entry representing this overlay in parent, if this overlay is a nested overlay.
    public void notifyEntryChange(ArtifactNotification added, ArtifactNotification removed, ArtifactNotification modified, String filter) {
        //the overlay will call us via this method as the mask set is manipulated.
        //if we have no listeners, we have no need to do anything.
        if (listeners.isEmpty())
            return;

        //added/removed/modified will all have the same container set for any invocation, so we can just test 'added'
        if (added.getContainer() == overlayContainer) {
            //remove any paths from the notification that were already present in the original container.
            //eg.
            //  if added to overlay and already present in base, convert to modified.
            //  if removed from overlay and already present in base, convert to modified.
            //  if modified in overlay, leave as modified.
            Set<String> filteredAdd = filterExistingPaths(added.getPaths());
            Set<String> filteredRemoved = filterExistingPaths(removed.getPaths());

            //remove the converted from the set..
            Set<String> newAdd = new HashSet(filterMaskedPaths(added.getPaths()));
            newAdd.removeAll(filteredAdd);

            Set<String> newRemoved = new HashSet(filterMaskedPaths(removed.getPaths()));
            newRemoved.removeAll(filteredRemoved);

            Set<String> newModified = new HashSet<String>(modified.getPaths().size());
            //filter out masked from the modified set..
            newModified.addAll(filterMaskedPaths(modified.getPaths()));
            //add in the add/deletes that are being converted..
            newModified.addAll(filteredAdd);
            newModified.addAll(filteredRemoved);

            //now iterate the registered listeners, and invoke them with the subsets that they cared about..
            notifyAllListeners(newAdd, newRemoved, newModified, filter);
        }
        if (added.getContainer() == overlaidContainer) {
            //remove any paths from the notification that are overlaid.
            Set<String> filteredAdd = filterOverlaidPaths(added.getPaths());
            Set<String> filteredRemoved = filterOverlaidPaths(removed.getPaths());
            Set<String> filteredModified = filterOverlaidPaths(modified.getPaths());

            //now iterate the registered listeners, and invoke them with the subsets that they cared about..
            notifyAllListeners(filteredAdd, filteredRemoved, filteredModified, filter);
        }
        if (added.getContainer() == root) {
            //this is a special invocation coming in from the mask unmask logic..
            //there is no need to prefilter the paths here.
            notifyAllListeners(added.getPaths(), removed.getPaths(), modified.getPaths(), filter);
        }

    }

    //convert paths starting with prefix into notificatons
    private ArtifactNotification collectNotificationsForPrefix(String prefix, Collection<String> paths) {
        Set<String> gatheredPaths = new HashSet<String>();
        boolean notRecurse = prefix.startsWith("!");
        if (notRecurse) {
            prefix = prefix.substring(1);
        }

        if ("/".equals(prefix)) {
            gatheredPaths.addAll(paths);
        } else {
            for (String path : paths) {
                if (path.startsWith(prefix + "/") || path.equals(prefix)) {
                    if (notRecurse) {
                        //needs a bit more care.. user asked for prefix, and immediate children..
                        //strip the prefix from the front.. this should always work.. as path has to be at least one char, as does prefix.
                        String fragment = path.substring(prefix.length());
                        //if resulting fragment has no path seps, notify on it.
                        // <1 lets / exist as first char or not at all
                        if (fragment.indexOf("/") < 1) {
                            gatheredPaths.add(path);
                        }
                    } else {
                        gatheredPaths.add(path);
                    }
                }
            }
        }
        return new DefaultArtifactNotification(root, gatheredPaths);
    }

    private void notifyAllListeners(Collection<String> created, Collection<String> deleted, Collection<String> modified, String filter) {
        //if we have changed.. and we have a parent, tell the parent the entry changed.
        //this helps simulate jar behavior
        if (this.parentNotifier != null) {
            this.parentNotifier.notifyAllListeners(Collections.<String> emptySet(), Collections.<String> emptySet(), Collections.<String> singleton(pathOfEntryInParent), filter);
        }

        for (Map.Entry<String, Collection<ArtifactListenerSelector>> listenersForPath : listeners.entrySet()) {
            String path = listenersForPath.getKey();
            ArtifactNotification createdForPath = collectNotificationsForPrefix(path, created);
            ArtifactNotification modifiedForPath = collectNotificationsForPrefix(path, modified);
            ArtifactNotification deletedForPath = collectNotificationsForPrefix(path, deleted);
            //only notify if we have paths to notify for!
            if (!createdForPath.getPaths().isEmpty() || !modifiedForPath.getPaths().isEmpty() || !deletedForPath.getPaths().isEmpty()) {
                for (ArtifactListenerSelector listener : listenersForPath.getValue()) {
                    // If there is no filter, or the artifact listener id does not match the filter, skip the notification.
                    if (filter != null) {
                        String id = listener.getId();
                        if (!(filter.equals(id))) {
                            continue;
                        }
                    }

                    //Invoke the notifier.. here we are branching out to code not part of the File artifact impl.
                    listener.notifyEntryChange(createdForPath, deletedForPath, modifiedForPath);
                }
            }
        }
    }
}

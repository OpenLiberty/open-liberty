/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.artifact.zip.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactListener;
import com.ibm.wsspi.artifact.DefaultArtifactNotification;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;

/**
 * Notifier for zip file containers.
 *
 * TODO: Use of the notifier as a listener seems problematic.
 *
 *       There is a potential problem of the notifier never being removed.
 *
 *       There is a second problem of the root container not being a unique
 *       representation of the zip file.  The artifact file system does not
 *       guarantee that containers or container entries are unique.
 */
public class ZipFileArtifactNotifier implements ArtifactNotifier, FileMonitor, ArtifactListener {
    /**
     * Create a notifier for a zip file container which has an already
     * extracted archive file.  This can be a root-of-roots zip container
     * or an exposed non-root zip container.
     *
     * @param rootContainer The root container.
     * @param a_rootPath The absolute path to the zip file of the
     *     root container.
     */
    public ZipFileArtifactNotifier(ZipFileContainer rootContainer, String a_rootPath) {

        this.emptyNotification = new DefaultArtifactNotification(
            rootContainer,
            Collections.<String> emptySet() );

        this.rootContainer = rootContainer; // Never null.

        this.a_exposedRootPath = a_rootPath; // Never null for an exposed notifier.
        this.entryInEnclosingContainer = null; // Always null for an exposed notifier.
        // Note that the root container may have an enclosing container.
        // That is set to null since the notifier won't use it, since the
        // exposed file is directly monitored.

        this.serviceProperties = new Hashtable<String, Object>();
        this.listenerRegistered = false; // Not used for an exposed notifier.

        this.listenersLock = new ListenersLock();
        this.listeners = new HashMap<String, Collection<ArtifactListener>>();
        this.coveringPaths = new HashSet<String>();
    }

    /**
     * Create a notifier for a zip file container which is not yet extracted
     * to disk.  This happens only when the zip container is an non-exposed
     * container.  The zip container may eventually be extracted, but the
     * notifier will not ever monitor it, as the temporary extraction location
     * is not expected to be modified by the user.
     *
     * @param rootContainer The root zip container.
     * @param entryInEnclosingContainer The root zip container as an
     *     enclosed entry.
     */
    public ZipFileArtifactNotifier(
        ZipFileContainer rootContainer,
        ArtifactEntry entryInEnclosingContainer) {

        this.emptyNotification = new DefaultArtifactNotification(
            rootContainer,
            Collections.<String> emptySet() );

        this.rootContainer = rootContainer; // Never null.

        this.a_exposedRootPath = null; // Always null for a non-exposed notifier.
        this.entryInEnclosingContainer = entryInEnclosingContainer; // Never null for a non-exposed notifier.

        this.serviceProperties = null; // Not used for an non-exposed notifier.
        this.listenerRegistered = false; // False until a listener is registered.

        this.listenersLock = new ListenersLock();
        this.listeners = new HashMap<String, Collection<ArtifactListener>>();
        this.coveringPaths = new HashSet<String>();
    }

    //

    // An empty notification specific to this notifier.
    //
    // An empty collection of changes with an added link to the
    // root container.
    //
    // Created once and cached.  Change sets are split into
    // added/removed/updated subsets, and two of three of these
    // are usually empty.

    private final ArtifactNotification emptyNotification;

    //

    private final ZipFileContainer rootContainer;

    // The exposed path of the root container, and the enclosed
    // entry of the container. The root path is null for a zip
    // type container which is not exposed on disk.  The enclosed
    // entry is null if the zip container has an exposed zip file,
    // which is always true if the zip container is a root-of-roots
    // container, but is also true if the zip container is a nested
    // container which exposed file on disk.

    private final String a_exposedRootPath;
    private final ArtifactEntry entryInEnclosingContainer;

    @Trivial
    private boolean isExposedNotifier() {
        return ( a_exposedRootPath == null );
    }

    @Trivial
    private ContainerFactoryHolder getContainerFactoryHolder() {
        return rootContainer.getContainerFactoryHolder();
    }

    private void loadZipEntries() {
        // Entries are forced to ensure they are accurate if
        // and when change notification is received.
        //
        // For example, if an update to an non-enclosed JAR removes
        // an entry which has a registered listener, and the entries
        // are not populated until the update is performed, the
        // entries will be generated on the updated JAR, which
        // won't have the entry which was removed, and the notifier
        // won't notify the listener for the entry.
        //
        // The latest this can be deferred is to when a listener
        // is registered.

        @SuppressWarnings("unused")
        Map.Entry<String, ZipEntry>[] allZipEntries = rootContainer.getZipEntries();
    }

    //

    /**
     * Control parameter: If false, a timed monitor is
     * used.  If true, explicit notifications are received,
     * usually from a managed bean.
     *
     * Default to false -- use a timed monitor.
     */
    private boolean monitorIsExplicit = false;

    /**
     * Control parameter: The monitoring interval.
     *
     * Default this to 5000 milliseconds.
     *
     * Unused unless the monitor is timed.
     */
    private long monitorIntervalMilliSec = 5000L;

    // The active monitor, for an exposed notifier.
    private final Hashtable<String, Object> serviceProperties;
    private ServiceRegistration<FileMonitor> service;

    // Link to the enclosing notifier, for a non-exposed notifier.
    private boolean listenerRegistered;

    /**
     * Notification API: Update notification options.  Tell if the update was
     * accepted. See {@link ArtifactNotifier#setNotificationOptions(long, boolean)}.
     *
     * Do nothing for an enclosed zip type container.  These are not monitored.
     *
     * @param newMonitorIntervalMilliSec The new monitor interval.
     * @param newMonitorIsExplicit True or false telling if external (managed
     *     bean) based notification is used, or if internal (timed) notification
     *     is used.
     */
    @Override
    public boolean setNotificationOptions(
        long newMonitorIntervalMilliSec,
        boolean newMonitorIsExplicit) {

        synchronized ( listenersLock ) {
            // Notification for an non-exposed zip container are received
            // from the parent container.  That parent container notifier
            // controls the notification options.  Notification options
            // on this enclosed notifier are unused.

            if ( isExposedNotifier() ) {
                return false; // The options were not accepted.
            }

            if ( (newMonitorIntervalMilliSec == monitorIntervalMilliSec) &&
                 (newMonitorIsExplicit == monitorIsExplicit) ) {
                return true; // The options were accepted, but have no effect.
            }

            monitorIsExplicit = newMonitorIsExplicit;
            monitorIntervalMilliSec = newMonitorIntervalMilliSec;

            updateMonitorService();

            return true; // The options were accepted, and had an effect.
        }
    }

    @Trivial
    private void updateMonitor() {
        if ( !isExposedNotifier() ) {
            updateMonitorService();
        } else {
            updateEnclosingMonitor();
        }
    }

    /**
     * Update the monitor service according to whether any listeners
     * are registered.  That is, if any covering paths are present.
     *
     * When listeners are registered, register the file monitor as
     * a service.  When no listeners are registered, unregister the
     * file monitor as a service.
     *
     * Registration as a service is only done when the notifier is
     * an exposed notifier.  For a non-exposed notifier, see
     * {@link #updateEnclosingMonitor}.
     */
    @FFDCIgnore(IllegalStateException.class)
    private void updateMonitorService() {
        if ( !coveringPaths.isEmpty() ) {
            if ( service == null ) {
                try {
                    // If we are shutting down, we want to generate the exception quickly.
                    BundleContext bundleContext = getContainerFactoryHolder().getBundleContext();
                    // throws 'IllegalStateException'

                    setServiceProperties();
                    service = bundleContext.registerService(FileMonitor.class, this, serviceProperties);

                    // See comments on 'loadZipEntries' for why the entries must be loaded now.
                    loadZipEntries();

                } catch ( IllegalStateException e ) {
                    // Ignore; the framework is shutting down.
                }
            } else {
                // Do nothing: There is already a service registration.
            }

        } else {
            if ( service != null ) {
                try {
                    service.unregister();
                } catch ( IllegalStateException e ) {
                    // Ignore; framework is shutting down.
                }
                service = null;
            } else {
                // Do nothing: There is already no service registration.
            }
        }
    }

    /**
     * Update the enclosing monitor according to whether any listeners
     * are registered.  That is, if any covering paths are present.
     *
     * When listeners are registered, register the this notifier as
     * a listener on the enclosing entry.  When no listeners are
     * registered, remove this notifier as a listener on the enclosing
     * entry.
     *
     * Registration as a listener is only done when the notifier is
     * a non-exposed notifier.  For an exposed notifier, see
     * {@link #updateMonitorService}.
     */
    private void updateEnclosingMonitor() {
        if ( !coveringPaths.isEmpty() ) {
            if ( !listenerRegistered ) {
                // This container is not yet registered to the enclosing container.
                // Register this container.

                ArtifactContainer enclosingRootContainer = entryInEnclosingContainer.getRoot();

                // The path to register is the path of the enclosing entry.
                ArtifactNotification enclosingNotification = new DefaultArtifactNotification(
                    enclosingRootContainer,
                    Collections.singleton( entryInEnclosingContainer.getPath() ) );

                ArtifactNotifier enclosingRootNotifier = enclosingRootContainer.getArtifactNotifier();

                // The enclosing container generally will accept the registration
                // request.  Just in case it doesn't, set the registration flag
                // based on the registration result.
                listenerRegistered = enclosingRootNotifier.registerForNotifications(enclosingNotification, this);

                // The result is that any change to the enclosing container reaches
                // this notifier through 'notifyEntryChange'.

                // See comments on 'loadZipEntries' for why the entries must be loaded now.
                loadZipEntries();

            } else {
                // Do nothing: The enclosing entry was already registered
                // to the enclosing notifier.
            }

        } else {
            if ( listenerRegistered ) {
                // There are no listener registrations active on this container.
                // Remove the registration of this listener.
                //
                // This listener should be registered exactly once to the enclosing
                // container: Removing all registrations of this listener should remove
                // that one registration, with no addition, unwanted registration changes.

                ArtifactContainer enclosingRootContainer = entryInEnclosingContainer.getRoot();
                ArtifactNotifier enclosingNotifier = enclosingRootContainer.getArtifactNotifier();
                enclosingNotifier.removeListener(this);

            } else {
                // Do nothing: The enclosing monitor already did not have a registration
                // for this container.
            }
        }
    }

    private void setServiceProperties() {
        serviceProperties.put(Constants.SERVICE_VENDOR, "IBM");

        String notificationType =
                ( monitorIsExplicit ? FileMonitor.MONITOR_TYPE_EXTERNAL : FileMonitor.MONITOR_TYPE_TIMED );
            serviceProperties.put(FileMonitor.MONITOR_TYPE, notificationType);

        serviceProperties.put(FileMonitor.MONITOR_INTERVAL, Long.toString(monitorIntervalMilliSec) + "ms");

        // Monitor the root file ..

        serviceProperties.put(FileMonitor.MONITOR_FILES, new String[] { a_exposedRootPath });

        // But also monitor the root as a directory???
        //
        // TODO: Why set the directories, recurse, and filter flags?
        //       This monitor will only ever be used for a zip file.

        serviceProperties.put(FileMonitor.MONITOR_DIRECTORIES, new String[] { a_exposedRootPath });
        serviceProperties.put(FileMonitor.MONITOR_RECURSE, Boolean.TRUE);
        serviceProperties.put(FileMonitor.MONITOR_FILTER, ".*");
    }

    //

    private class ListenersLock {
        // EMPTY
    }
    private final ListenersLock listenersLock;
    private final Map<String, Collection<ArtifactListener>> listeners;
    private final Set<String> coveringPaths;

    /**
     * Notification API: Register a listener with the paths which are
     * specified by the registration object.
     * See {@link ArtifactNotifier#registerForNotifications}.
     *
     * The registration object must be linked to the root container of
     * of this notification.  If the registration is not linked to the
     * root container, an <code>IllegalArgumentException</code> is thrown.
     *
     * The new registration must not duplicate an existing registration.
     *
     * @param newListenerPaths A notification which has the paths to which
     *    to register the listener.
     * @param The listener which is to be registered to the specified
     *     paths.
     *
     * @return True or false, telling if the registration was accepted.
     *     This implementation always answers true.
     */
    @Override
    public boolean registerForNotifications(
        ArtifactNotification newListenerPaths,
        ArtifactListener newListener) {

        if ( newListenerPaths.getContainer().getRoot() != rootContainer ) {
            throw new IllegalArgumentException();
        }

        synchronized ( listenersLock ) {
            boolean addedUncoveredPaths = false;

            for ( String newListenerPath : newListenerPaths.getPaths() ) {
                // Handle non-recursive listener registration as recursive registration.
                if ( newListenerPath.startsWith("!") ) {
                    newListenerPath = newListenerPath.substring(1);
                }

                if ( registerListener(newListenerPath, newListener) ) {
                    addedUncoveredPaths = true;
                }
            }

            if ( addedUncoveredPaths ) {
                updateMonitor();
            }
        }

        return true;
    }

    /**
     * Register a listener to a specified path.
     *
     * Registration has two effects: The listener is put into a table
     * which maps the listener to specified path.  The path of the
     * listener are added to the covering paths collection, possibly
     * causing newly covered paths to be removed from the collection.
     * If the new path is already covered, the covering paths
     * collection is not changed.
     *
     * @param newPath The path to which to register the listener.
     * @param newListener The listener which is to be registered.
     *
     * @return True or false telling if the uncovered paths collection
     *     was updated.
     */
    private boolean registerListener(String newPath, ArtifactListener newListener) {
        boolean updatedCoveringPaths = addCoveringPath(newPath);

        Collection<ArtifactListener> listenersForPath = listeners.get(newPath);
        if ( listenersForPath == null ) {
            // Each listeners collection is expected to be small.
            listenersForPath = new LinkedList<ArtifactListener>();
            listeners.put(newPath, listenersForPath);
        }
        listenersForPath.add(newListener);

        return ( updatedCoveringPaths );
    }

    /**
     * Add a path to the covering paths collection.
     *
     * Do nothing if a path in the collection covers the new path.
     *
     * Add the path and remove any paths covered by the new path
     * if the path is not covered by a path in the collection.
     *
     * @param newPath The path to add to the covering paths collection.
     *
     * @return Answer true or false telling if the covering paths collection
     *     was modified.
     */
    private boolean addCoveringPath(String newPath) {
        int newLen = newPath.length();

        Iterator<String> useCoveringPaths = coveringPaths.iterator();
        boolean isCovered = false;
        boolean isCovering = false;
        while ( !isCovered && useCoveringPaths.hasNext() ) {
            String coveringPath = useCoveringPaths.next();
            int coveringLen = coveringPath.length();

            if ( coveringLen < newLen ) {
                if ( isCovering ) {
                    continue; // Can't be covered.
                } else {
                    if ( newPath.regionMatches(0, coveringPath, 0, coveringLen) ) {
                        if ( newPath.charAt(coveringLen) == '/' ) {
                            isCovered = true; // Covered: "covering/child" vs "covering"
                            break; // No need to continue: Can't be any additional relationships to find.
                        } else {
                            continue; // Dissimilar: "coveringX" vs "covering"
                        }
                    } else {
                        continue; // Dissimilar: "coverXngX" vs "covering"
                    }
                }
            } else if ( coveringLen == newLen ) {
                if ( isCovering ) {
                    continue; // Can't be covered
                } else {
                    if ( newPath.regionMatches(0, coveringPath, 0, coveringLen) ) {
                        isCovered = true; // Covered: "covering" vs "covering"
                        break; // No need to continue: Can't be any additional relationships to find.
                    } else {
                        continue; // "covering" vs "coverXng"
                    }
                }
            } else { // coveringLen > newLen
                if ( newPath.regionMatches(0, coveringPath, 0, newLen) ) {
                    if ( coveringPath.charAt(newLen) == '/' ) {
                        isCovering = true;
                        useCoveringPaths.remove(); // Covering: "covering" vs "covering/child"
                        continue; // Look for other independent children: "covering/child1" and "covering/child2"
                    } else {
                        continue; // Dissimilar: "covering" vs "coveringX"
                    }
                } else {
                    continue; // Dissimilar: "covering" vs "coverXngX"
                }
            }
        }

        if ( !isCovered ) {
            coveringPaths.add(newPath);
        }

        return !isCovered;
    }

    /**
     * Notification API: Remove all registrations of a listener.
     * See {@link ArtifactNotifier#removeListener(ArtifactListener)}.
     *
     * @param listenerToRemove The listener which is to be removed.
     *
     * @return True or false telling if removal of the listener
     *     caused at least one path which had a registered listener
     *     to no longer have a registered listener.
     */
    @Override
    public boolean removeListener(ArtifactListener listenerToRemove) {
        synchronized(listenersLock) {
            List<String> pathsToRemove = new ArrayList<String>(1);
            for ( Map.Entry<String, Collection<ArtifactListener>> listenersEntry : listeners.entrySet() ) {
                for ( ArtifactListener listener : listenersEntry.getValue() ) {
                    if ( listener == listenerToRemove ) {
                        pathsToRemove.add( listenersEntry.getKey() );
                        break;
                    }
                }
            }

            Iterator<String> usePathsToRemove = pathsToRemove.iterator();
            while ( usePathsToRemove.hasNext() ) {
                String pathToRemove = usePathsToRemove.next();
                Collection<ArtifactListener> listenersForPath = listeners.get(pathToRemove);
                if ( listenersForPath.size() == 1 ) {
                    listeners.remove(pathToRemove); // The last listner for the path.
                } else {
                    listenersForPath.remove(listenerToRemove);
                    usePathsToRemove.remove();
                }
            }

            if ( !pathsToRemove.isEmpty() ) {
                // TODO: Clearing and rebuilding the covering paths collection
                //       Not optimal, but hard to do better, and not necessary
                //       since removals should be rare and the registrations
                //       are expected to be small.
                //
                // Each path which was removed which is a covering path
                // may uncover paths.  Those need to be found and added
                // to the covering paths collection.
                //
                // But, not all paths which were covered should be added,
                // as some the newly uncovered paths may cover other of
                // the newly uncovered paths.
                //
                // Also, an uncovered path which is discovered may itself
                // be a path to removed which hasn't yet been processed.

                coveringPaths.clear();

                for ( String listenerPath : listeners.keySet() ) {
                    @SuppressWarnings("unused")
                    boolean addedUncoveredPath = addCoveringPath(listenerPath);
                }

                // Don't know if the covering paths collection was perturbed.
                // Assume that it was.
                updateMonitor();
            }

            return ( !pathsToRemove.isEmpty() );
        }
    }

    // Monitor callbacks ..

    /**
     * File monitor API. Callback for the initial registration.
     * See {@link FileMonitor#onBaseline(Collection)}.
     *
     * This callback is ignored.
     *
     * @param baseline The baseline files.  This is expected to
     *     be the singleton of the root zip file.
     */
    // @Trivial // Allow this to be logged: A singleton is expected.
    @Override
    public void onBaseline(Collection<File> baseline) {
        // Ignore
    }

    /**
     * File monitor API. Callback for a change. See {@link FileMonitor#onChange}.
     *
     * See {@link #validateNotification(Collection, Collection, Collection)}
     * for change cases.
     *
     * @param addedFiles The files which the monitor determined to have been added.
     * @param updatedFiles The files which the monitor determined to have been updated.
     * @param removedFiles The files which the monitor determined to have been removed.
     */
    // @Trivial // Allow this to be logged: One entry in one change collection is expected.
    @Override
    public void onChange(
        Collection<File> addedFiles,
        Collection<File> updatedFiles,
        Collection<File> removedFiles) {

        boolean isUpdate = !updatedFiles.isEmpty();
        if ( addedFiles.isEmpty() && !isUpdate && removedFiles.isEmpty() ) {
            return;
        }
        validateNotification(addedFiles, updatedFiles, removedFiles);

        notifyAllListeners(isUpdate);
    }

    /**
     * Listener API: Receive change notification from the enclosing notifier.
     * See {@link ArtifactListener#notifyEntryChange}.
     *
     * The listener is wired by {@link #updateEnclosingMonitor}, which registers
     * this notifier as a listener to changes to the enclosing entry from the
     * notifier of the enclosing root container.
     *
     * The notification which is received is expected to have exactly one path
     * across all of the change collections, for that one element to be either
     * a remove change or an update change, and for the path to be the path
     * of the enclosing entry of this notifier.
     *
     * Enables registration of the root container to the notifier of its
     * enclosing container, for the case of a non-exposed root container.
     *
     * Changes are not expected for non-exposed root containers.  These are
     * only available on-disk as temporary files, which are not user accessible
     * and are not expected to be modified.
     *
     * See {@link #validateNotification(Collection, Collection, Collection)}
     * for change cases.
     *
     * @param addedNotification Paths of entries which have been added.
     * @param removedNotification Paths of entries which have been removed.
     * @param updatedNotification Paths of entries which have been updated.
     */
    // @Trivial // Allow this to be logged: One entry in one change collection is expected.
    @Override
    public void notifyEntryChange(
            ArtifactNotification addedNotification,
            ArtifactNotification removedNotification,
            ArtifactNotification updatedNotification) {

        Collection<String> addedPaths = addedNotification.getPaths();
        Collection<String> removedPaths = removedNotification.getPaths();
        Collection<String> updatedPaths = updatedNotification.getPaths();

        boolean isUpdate = !updatedPaths.isEmpty();
        if ( addedPaths.isEmpty() && !isUpdate && removedPaths.isEmpty() ) {
            return;
        }

        validateNotification(addedPaths, updatedPaths, removedPaths);

        notifyAllListeners(isUpdate);
    }

    /**
     * Validate change data, which is expected to be collections of files
     * or collections of entry paths.
     *
     * Since the single root zip file is registered, the change is expected
     * to be a single element in exactly one of the change collections.
     *
     * Addition is not supported, and is handled by throwing an exception.
     *
     * Although unexpected, all three change collections being empty is
     * supported, and is ignored.
     *
     * Having a removal and an update is not expected, and is handled by
     * throwing an exception.
     */
    // @Trivial // Allow this to be logged: One entry in one change collection is expected.
    private void validateNotification(Collection<?> added, Collection<?> removed, Collection<?> updated) {
        boolean isAddition = !added.isEmpty();
        boolean isRemoval = !removed.isEmpty();
        boolean isUpdate = !updated.isEmpty();

        if ( !isAddition && !isRemoval && !isUpdate ) {
            // Allow this, since it was allowed before.
            // throw new IllegalStateException("Disallowed null zip file modification");
        } else if ( isAddition ) {
            throw new IllegalStateException( "Disallowed zip file addition [ " + added.toString() + " ]" );
        } else if ( isUpdate && isRemoval ) {
            throw new IllegalStateException(
                "Disallowed zip file update [ " + updated.toString() + " ]" +
                " at the same time as a removal [ " + removed.toString() + " ]");
        }
    }

    /**
     * A notification, which was either an update to the entire zip, or was
     * the removal of the entire zip file, was received.  For each listener
     * that is registered, collect the paths for that listener and forward
     * the notification.
     *
     * The notification which is forwarded to each listener is of the same
     * type -- update or removal -- as the entire zip notification.
     *
     * TODO: We *could* try to provide finer notification in case of a whole
     *       zip update.  That could be done by comparing the the prior zip
     *       entries with the current zip entries, for example, by comparing
     *       the zip entry lengths, last modified times, and sizes.
     *
     * @param isUpdate Control parameter: Tell if the notification was an
     *     update of the entire zip file, or a removal of the entire zip file.
     */
    private void notifyAllListeners(boolean isUpdate) {
        // Can't reuse the registered paths collection across the loop
        // because the listener notification can do processing in a new
        // thread.  Reusing the registered paths could cause a collision
        // between the listener thread with this notification processing
        // thread.

        // TODO: Should the notification step be separated from the listener detection step?
        //       That might create large data structures, but would prevent the listeners
        //       from being locked during a possibly extensive steps of the listeners handling
        //       their notifications.

        synchronized( listenersLock ) {
            for ( Map.Entry<String, Collection<ArtifactListener>> listenersEntry : listeners.entrySet() ) {
                List<String> a_registeredPaths = new ArrayList<String>();
                collectRegisteredPaths( listenersEntry.getKey(), a_registeredPaths );
                if ( a_registeredPaths.isEmpty() ) {
                    continue;
                }

                ArtifactNotification registeredPaths =
                    new DefaultArtifactNotification(rootContainer, a_registeredPaths);

                for ( ArtifactListener listener : listenersEntry.getValue() ) {
                    // Additions, removals, updates
                    if ( isUpdate ) {
                        listener.notifyEntryChange(emptyNotification, emptyNotification, registeredPaths);
                    } else {
                        listener.notifyEntryChange(emptyNotification, registeredPaths, emptyNotification);
                    }
                }
            }
        }
    }

    @Trivial // Don't log this: The paths collection could be very large.
    private void collectRegisteredPaths(String a_path, List<String> a_paths) {
        Map.Entry<String, ZipEntry>[] allZipEntries = rootContainer.getZipEntries();

        if ( a_path.isEmpty() || ((a_path.length() == 1) && (a_path.charAt(0) == '/')) ) {
            for ( Map.Entry<String, ZipEntry> entry : allZipEntries ) {
                a_paths.add( "/" + entry.getKey() );
            }
            a_paths.add("/");

        } else {
            String r_path = a_path.substring(1);
            int r_pathLen = r_path.length();

            int location = rootContainer.locatePath(allZipEntries, r_path);
            boolean isExact;
            if ( location < 0 ) {
                location = (location + 1) * -1; // Inexact
                isExact = false;
            } else {
                location++;
                isExact = true;
            }

            while ( location < allZipEntries.length ) {
                String r_nextPath = allZipEntries[location].getKey();
                int r_nextPathLen = r_nextPath.length();

                if ( r_nextPathLen <= r_pathLen ) {
                    break;
                } else if ( !(r_nextPath.regionMatches(0, r_path, 0, r_pathLen) && (r_nextPath.charAt(r_pathLen) == '/')) ) {
                    break;
                } else {
                    a_paths.add("/" + r_nextPath);
                    location++;
                }
            }

            if ( isExact ) {
                a_paths.add("/" + r_path);
            }
        }
    }
}

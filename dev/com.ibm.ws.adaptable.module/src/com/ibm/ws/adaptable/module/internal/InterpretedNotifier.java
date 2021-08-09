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
package com.ibm.ws.adaptable.module.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.adaptable.module.structure.StructureHelper;
import com.ibm.wsspi.adaptable.module.DefaultNotification;
import com.ibm.wsspi.adaptable.module.InterpretedContainer;
import com.ibm.wsspi.adaptable.module.Notifier;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactNotification;

/**
 * <p>
 * This class will handle notifications coming from an {@link InterpretedContainer}. For the actual registering against an {@link ArtifactNotification} it delegates to the
 * {@link Notifier} for the adaptable container that it is interpreting. It's role is two fold:
 * </p>
 * <ul>
 * <li>To correct the paths when this is the notifier for a fake root. This means that if you have a path on disk of /a/b/c but b is a fake root then when registering for
 * notifications on c the call to
 * {@link #registerForNotifications(com.ibm.wsspi.adaptable.module.Notifier.Notification, com.ibm.wsspi.adaptable.module.Notifier.NotificationListener)} will be made with a path
 * of "/c" but needs to be passed to the {@link ArtifactNotifier} as a path of "/a/b/c". Conversely when notifying the {@link NotificationListener} of the change the path will be
 * set to "/c" even though the path returned from the ArtifactListener was "/a/b/c"</li>
 * <li>To detect when a notification is fired from within a different "fake" root. Using the same setup as above, if we register for notifications against "/" and "c" changes then
 * we actually want to receive a notification that "/a/b" has been modified because this is a new fake root. This will mirror the situation where you have an zip archive and
 * something is added, removed or changed within the archive but because it is just a zip file you just get a notification that the archive has changed. Note that this requirement
 * means that an instance of this class should <b>always</b> be used for an {@link InterpretedContainer} even if the container isn't a child of a fake root because it needs to
 * guard against changing fake roots.</li>
 * </ul>
 */
public class InterpretedNotifier implements Notifier {

    /** <code>true</code> if the container this notifier is for is a fake root */
    private final boolean isNotifierForFakeRoot;
    /** This is the delegate Notifier from the adaptable Contaiener that will communicate with the artifact API notification framework */
    private final Notifier delegateNotifier;
    /**
     * This is the path in the real root that the interpreted root this notifier is acting for is at. This will be <code>null</code> if {@link #isNotifierForFakeRoot} is
     * <code>false</code>
     */
    private final String pathInRealRoot;
    /**
     * A map of listeners that this notifier is managing. The key is the listener registered with this notifier and the value is a {@link CorrectInterpretedPathListener} that is
     * registered with the {@link #delegateNotifier}.
     */
    private final Map<NotificationListener, CorrectInterpretedPathListener> listeners = new HashMap<NotificationListener, CorrectInterpretedPathListener>();
    /** This is the root container that this notifier is for. Note that this may be a fake root because of the structure helper */
    private final InterpretedContainer rootContainer;
    /** The structure helper that defines the structure for the Interpreted container this sits over */
    private final StructureHelper structureHelper;
    /**
     * This is the ArtifactContainer that is at the root of the interpreted container. Note that this may not return <code>true</code> for {@link ArtifactContainer#isRoot()}
     * because it is a fake root due to the structure helper
     */
    private final ArtifactContainer rootArtifactContainer;

    /**
     * Construct a new instance of this notifier.
     *
     * @param rootContainer This is the root container that this interpreted notifier acts on. Note that this may be a fake root because of the structure helper
     * @param rootDelegate This is the ArtifactContainer that is at the root of the interpreted container. Note that this may not return <code>true</code> for
     *            {@link ArtifactContainer#isRoot()} because it is a fake root due to the structure helper
     * @param structureHelper The structure helper for the interpreted container that this notifier is for
     */
    public InterpretedNotifier(InterpretedContainer rootContainer, ArtifactContainer rootDelegate, StructureHelper structureHelper) {
        super();

        /*
         * Use a NotifierImpl delegate to actually communicate with the artifact API notification framework, this is just done to keep this class a little simple as it now only
         * needs to worry about one type of Notification object.
         */
        this.delegateNotifier = new NotifierImpl(rootDelegate, rootContainer);

        // For convenience later on work out if this is acting on a fake root now, this will make it easier when calculating if we need to change paths
        String rootDelegatePath = rootDelegate.getPath();
        this.isNotifierForFakeRoot = !("/".equals(rootDelegatePath));
        this.pathInRealRoot = this.isNotifierForFakeRoot ? rootDelegatePath : null;
        this.rootContainer = rootContainer;
        this.rootArtifactContainer = rootDelegate;
        this.structureHelper = structureHelper;
    }

    /** {@inheritDoc} */
    @Override
    public boolean registerForNotifications(Notification targets, NotificationListener callbackObject) throws IllegalArgumentException {
        // Convert the targets to the right root, only need to do this if we have a fake root
        final Notification rootNotification;
        if (this.isNotifierForFakeRoot) {
            // Have a fake root so convert the paths, need to also move ! to the front of the path to indicate a non-recursive listener
            Collection<String> newTargetPaths = new HashSet<String>();
            for (String oldPath : targets.getPaths()) {
                final StringBuilder newPath = new StringBuilder();
                if (oldPath.startsWith("!")) {
                    newPath.append("!");
                    oldPath = oldPath.substring(1);
                }
                newPath.append(pathInRealRoot);
                // Notification paths don't end in "/", except for root.
                // "/x.jar" + "/" should result in "/x.jar", not "/x.jar/".
                if (!oldPath.equals("/")) {
                    newPath.append(oldPath);
                }
                newTargetPaths.add(newPath.toString());
            }
            rootNotification = new DefaultNotification(targets.getContainer(), newTargetPaths);
        } else {
            /*
             * We aren't a fake root so we can use the original set of targets, even if the targets live within a child fake root that is ok as the targets in the artifact API
             * will be the same, we'll sort out converting the child fake root when we get a notification back
             */
            rootNotification = targets;
        }

        /*
         * Instead of registering the callbackObject directly, wrap it in a CorrectInterpretedPath listener that will handle converting paths given by the artifact layer to the
         * correct state as seen by the interpreted layer
         */
        CorrectInterpretedPathListener interpretedListener = new CorrectInterpretedPathListener(callbackObject);

        // Now register against our delegate, this will also validate the targets
        boolean isRegistered = this.delegateNotifier.registerForNotifications(rootNotification, interpretedListener);
        if (isRegistered) {
            // We could register with the delegate so keep track of the listener (in case we need to unregister it later)
            this.listeners.put(callbackObject, interpretedListener);
        }
        return isRegistered;
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeListener(NotificationListener listenerToRemove) {
        // Find the listener we actually registered and remove that
        NotificationListener interpretedListener = this.listeners.remove(listenerToRemove);
        if (interpretedListener != null) {
            return this.delegateNotifier.removeListener(interpretedListener);
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean setNotificationOptions(long interval, boolean useMBean) {
        // Straight pass through to our delegate
        return this.delegateNotifier.setNotificationOptions(interval, useMBean);
    }

    /**
     * This listener will correct any paths returned from the delegate Notifier so that they are correct for the interpreted container. If this is a fake root it will remove the
     * part of the path up to the root and if the notification is fired within a child fake root then it will change it to a modification on the fake root itself rather than its
     * internals.
     */
    private class CorrectInterpretedPathListener implements com.ibm.ws.adaptable.module.NotifierExtension.NotificationListener {

        /** This is the listener to callback to with the corrected paths */
        private final NotificationListener listener;
        /** This is the length of the path in the real root that this fake root is at. Will be <code>null</code> if {@link #isNotifierForFakeRoot} is <code>false</code>. */
        private final int lengthOfPathInRoot;

        /** The id associated with the registered notification listener */
        private String id;

        /**
         * @param listener
         */
        public CorrectInterpretedPathListener(NotificationListener listener) {
            super();
            this.listener = listener;
            this.lengthOfPathInRoot = (isNotifierForFakeRoot) ? pathInRealRoot.length() : -1;

            // If the listener has an Id, save it.
            if (listener instanceof com.ibm.ws.adaptable.module.NotifierExtension.NotificationListener) {
                id = ((com.ibm.ws.adaptable.module.NotifierExtension.NotificationListener) listener).getId();
            }
        }

        /** {@inheritDoc} */
        @Override
        public void notifyEntryChange(Notification added, Notification removed, Notification modified) {
            /*
             * For each path we need to do two things:
             * 1. See if the path is valid according to the structure helper, if the event is happening in a child fake root then we need to just register a modified against that
             * fake root
             * 2. For valid paths modify the path to remove the part that this fake root is at in the artifact root
             */
            Set<String> changedFakeRoots = new HashSet<String>();
            Notification interpretedAdded = this.correctPaths(added, changedFakeRoots);
            Notification interpretedRemoved = this.correctPaths(removed, changedFakeRoots);
            Notification interpretedModified = this.correctPaths(modified, changedFakeRoots);
            interpretedModified.getPaths().addAll(changedFakeRoots);
            this.listener.notifyEntryChange(interpretedAdded, interpretedRemoved, interpretedModified);
        }

        /**
         * This method will correct the paths in the supplied <code>notification</code> so that they are interpreted. It will change them so they are absolute paths to any fake
         * root this interpreted notifier is working on and if any of the paths are events that happen in a fake root then it will add them to the <code>changedFakeRoots</code>.
         *
         * @param notification The notification to correct the paths on
         * @param changedFakeRoots A set of child fake root paths that should be added to if any of the paths in the notification have happened within a fake root
         * @return A new notification with the corrected paths and all of the child fake roots removed. The container will be the rootContainer for this notifier
         */
        private Notification correctPaths(Notification notification, Set<String> changedFakeRoots) {
            Collection<String> interpretedPaths = new HashSet<String>();
            for (String rootPath : notification.getPaths()) {
                // If we are in a fake root then we need to remove the first part of the path from the root notification off so that we return the path from the fake root up
                String correctedPath;
                if (isNotifierForFakeRoot) {
                    correctedPath = rootPath.substring(this.lengthOfPathInRoot);

                    // Correct this for if we are the root of the fake root as in this case the above will have set it to ""
                    correctedPath = correctedPath.isEmpty() ? "/" : correctedPath;
                } else {
                    // Not a fake root so no need for correction
                    correctedPath = rootPath;
                }

                // See if this path is a valid one according to the structure helper, this is testing if the notification is coming from a child fake root
                if (structureHelper.isValid(rootArtifactContainer, rootPath)) {
                    // It is valid so no child fake root, can just use the corrected path
                    interpretedPaths.add(correctedPath);
                } else {
                    /*
                     * The notification is coming from a fake root. We therefore need to send out a notification saying that fake root has changed rather than stating what part
                     * inside that fake root has changed. To do this we find the fake root. There is an isRoot method on the structure helper but the artifact container not
                     * actually exist (it could be a delete notification) so we can't use it. Instead we need to walk up the string paths testing isValid.
                     *
                     * Currently we have the "correctedPath" that is the absolute path from this notifiers root (which may itself be fake!). First split this path into it's
                     * constituent parts, as it starts with a "/" the first split will be an empty string which should be ignored.
                     */
                    String[] pathParts = correctedPath.split("/");

                    // Use a path relative to the rootArtifactContainer so seed the path that we are testing with the first (non-empty string) path part
                    StringBuffer pathToTest = new StringBuffer(pathParts[1]);

                    /*
                     * We'll keep track of the last valid path as this is what we'll use as the path to notify against. The first part of the correct string is always valid because
                     * it could point to the child fake root's entry but not at something inside the child fake root itself as there is no child part to the path.
                     */
                    String lastValidPath = pathToTest.toString();

                    /*
                     * Now build up the path starting from the first child element (at i=2) testing if that child is valid, we only update lastValidPath if the child is valid,
                     * otherwise we have found the fake root and can break out
                     */
                    for (int i = 2; i < pathParts.length; i++) {
                        pathToTest.append("/");
                        pathToTest.append(pathParts[i]);
                        String newPathToTest = pathToTest.toString();
                        if (!structureHelper.isValid(rootArtifactContainer, newPathToTest)) {
                            break;
                        }
                        lastValidPath = newPathToTest;
                    }
                    // Make this path absolute (to this root) and add it to the set of paths that have changed (NOT the set of interpretedPaths)
                    changedFakeRoots.add("/" + lastValidPath);
                }
            }
            Notification interpretedNotification = new DefaultNotification(rootContainer, interpretedPaths);
            return interpretedNotification;
        }

        /**
         * Returns the associate callback listener's ID.
         *
         * @return The associate callback listener's ID.
         */
        @Override
        public String getId() {
            return id;
        }
    }
}

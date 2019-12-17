/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.internal;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.metadata.ComponentMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ComponentMetaData;

/**
 * Tracks application components and the managed threads they create.
 */
public class ThreadGroupTracker implements ComponentMetaDataListener {
    /**
     * Reference to the deferrable scheduled executor.
     */
    private ScheduledExecutorService deferrableScheduledExecutor;

    /**
     * Thread groups categorized by Java EE name and managed thread factory identifier.
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ThreadGroup>> metadataIdentifierToThreadGroups = new ConcurrentHashMap<String, ConcurrentHashMap<String, ThreadGroup>>();

    /**
     * The metadata identifier service.
     */
    private MetaDataIdentifierService metadataIdentifierService;

    /**
     * Server access control context, which we can use to run certain privileged operations that aren't available to application threads.
     */
    AccessControlContext serverAccessControlContext;

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context DeclarativeService defined/populated component context
     */
    protected void activate(ComponentContext context) {
        serverAccessControlContext = AccessController.getContext();
    }

    /**
     * @see com.ibm.ws.container.service.metadata.ComponentMetaDataListener#componentMetaDataCreated(com.ibm.ws.container.service.metadata.MetaDataEvent)
     */
    @Override
    @Trivial
    public void componentMetaDataCreated(MetaDataEvent<ComponentMetaData> event) {}

    /**
     * @see com.ibm.ws.container.service.metadata.ComponentMetaDataListener#componentMetaDataDestroyed(com.ibm.ws.container.service.metadata.MetaDataEvent)
     */
    @Override
    public void componentMetaDataDestroyed(MetaDataEvent<ComponentMetaData> event) {
        String identifier = metadataIdentifierService.getMetaDataIdentifier(event.getMetaData());
        ConcurrentHashMap<String, ThreadGroup> threadFactoryToThreadGroup = metadataIdentifierToThreadGroups.remove(identifier);
        if (threadFactoryToThreadGroup != null) {
            Collection<ThreadGroup> groupsToDestroy = threadFactoryToThreadGroup.values();
            if (!groupsToDestroy.isEmpty())
                AccessController.doPrivileged(new InterruptAndDestroyThreadGroups(groupsToDestroy, deferrableScheduledExecutor), serverAccessControlContext);
        }
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context DeclarativeService defined/populated component context
     */
    protected void deactivate(ComponentContext context) {}

    /**
     * Returns the thread group to use for the specified application component.
     * 
     * @param jeeName name of the application component
     * @param threadFactoryName unique identifier for the thread factory
     * @param parentGroup parent thread group
     * @return child thread group for the application component. Null if the application component isn't active.
     * @throws IllegalStateException if the application component is not available.
     */
    ThreadGroup getThreadGroup(String identifier, String threadFactoryName, ThreadGroup parentGroup) {
        ConcurrentHashMap<String, ThreadGroup> threadFactoryToThreadGroup = metadataIdentifierToThreadGroups.get(identifier);
        if (threadFactoryToThreadGroup == null)
            if (metadataIdentifierService.isMetaDataAvailable(identifier)) {
                threadFactoryToThreadGroup = new ConcurrentHashMap<String, ThreadGroup>();
                ConcurrentHashMap<String, ThreadGroup> added = metadataIdentifierToThreadGroups.putIfAbsent(identifier, threadFactoryToThreadGroup);
                if (added != null)
                    threadFactoryToThreadGroup = added;
            } else
                throw new IllegalStateException(identifier.toString());

        ThreadGroup group = threadFactoryToThreadGroup.get(threadFactoryName);
        if (group == null)
            group = AccessController.doPrivileged(new CreateThreadGroupIfAbsentAction(parentGroup, threadFactoryName, identifier, threadFactoryToThreadGroup),
                                                  serverAccessControlContext);
        return group;
    }

    /**
     * Declarative Services method for setting the deferrable scheduled executor service
     * 
     * @param svc the service
     */
    protected void setDeferrableScheduledExecutor(ScheduledExecutorService svc) {
        deferrableScheduledExecutor = svc;
    }

    /**
     * Declarative Services method for setting the metadata identifier service.
     * 
     * @param svc the service
     */
    protected void setMetadataIdentifierService(MetaDataIdentifierService svc) {
        metadataIdentifierService = svc;
    }

    /**
     * Invoke this method when destroying a ManagedThreadFactory in order to interrupt all managed threads
     * that it created.
     * 
     * @param threadFactoryName unique identifier for the managed thread factory.
     */
    void threadFactoryDestroyed(String threadFactoryName, ThreadGroup parentGroup) {
        Collection<ThreadGroup> groupsToDestroy = new LinkedList<ThreadGroup>();
        for (ConcurrentHashMap<String, ThreadGroup> threadFactoryToThreadGroup : metadataIdentifierToThreadGroups.values()) {
            ThreadGroup group = threadFactoryToThreadGroup.remove(threadFactoryName);
            if (group != null)
                groupsToDestroy.add(group);
        }
        groupsToDestroy.add(parentGroup);
        AccessController.doPrivileged(new InterruptAndDestroyThreadGroups(groupsToDestroy, deferrableScheduledExecutor), serverAccessControlContext);
    }

    /**
     * Declarative Services method for unsetting the deferrable scheduled executor service
     * 
     * @param svc the service
     */
    protected void unsetDeferrableScheduledExecutor(ScheduledExecutorService svc) {
        deferrableScheduledExecutor = null;
    }

    /**
     * Declarative Services method for unsetting the metadata service.
     * 
     * @param ref reference to the service
     */
    protected void unsetMetadataIdentifierService(MetaDataIdentifierService svc) {
        metadataIdentifierService = null;
    }

    /**
     * Privileged action that creates a new thread group if one doesn't already exist.
     */
    @Trivial
    private static class CreateThreadGroupIfAbsentAction implements PrivilegedAction<ThreadGroup> {
        private static final TraceComponent tc = Tr.register(CreateThreadGroupIfAbsentAction.class);

        private final String identifier;
        private final ThreadGroup parentGroup;
        private final String threadFactoryName;
        private final ConcurrentHashMap<String, ThreadGroup> threadFactoryToThreadGroup;

        /**
         * Construct a privileged action that creates a thread group if one doesn't already exist for the
         * combination of thread factory/application component.
         * 
         * @param parentGroup thread group for the managed thread factory
         * @param threadFactoryName name of the managed thread factory
         * @param jeeName name for the application component
         * @param threadFactoryToThreadGroup map of thread factory names to thread groups (all of which correspond to the application component)
         */
        private CreateThreadGroupIfAbsentAction(ThreadGroup parentGroup, String threadFactoryName, String identifier,
                                                ConcurrentHashMap<String, ThreadGroup> threadFactoryToThreadGroup) {
            this.identifier = identifier;
            this.parentGroup = parentGroup;
            this.threadFactoryName = threadFactoryName;
            this.threadFactoryToThreadGroup = threadFactoryToThreadGroup;
        }

        /**
         * Create a thread group if absent.
         */
        @Override
        public ThreadGroup run() {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            if (trace && tc.isEntryEnabled())
                Tr.entry(this, tc, "run", threadFactoryName, identifier, parentGroup);

            ThreadGroup newGroup = new ThreadGroup(parentGroup, threadFactoryName + ' ' + identifier + " Thread Group");
            newGroup.setDaemon(parentGroup.isDaemon());
            newGroup.setMaxPriority(parentGroup.getMaxPriority());

            ThreadGroup group = threadFactoryToThreadGroup.putIfAbsent(threadFactoryName, newGroup);

            if (group == null)
                group = newGroup;
            else
                newGroup.destroy();

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run", group);
            return group;
        }
    }

    /**
     * Privileged action that interrupts and destroys thread groups.
     */
    @Trivial
    private static class InterruptAndDestroyThreadGroups implements PrivilegedAction<Void> {
        private static final TraceComponent tc = Tr.register(InterruptAndDestroyThreadGroups.class);

        /**
         * Interval between retries of thread groups destroy.
         */
        private static final long DESTROY_RETRY_INTERVAL_MS = 2000;

        /**
         * Thread groups to destroy.
         */
        private final Collection<ThreadGroup> groups;

        /**
         * Executor that can schedule retries of thread group destroy if some threads haven't completed yet.
         */
        private final ScheduledExecutorService scheduledExecutor;

        /**
         * Construct a privileged action to destroy the specified thread groups.
         * 
         * @param groups thread groups to destroy.
         * @param scheduledExecutor executor that can schedule retries of thread group destroy if some threads haven't completed yet.
         */
        private InterruptAndDestroyThreadGroups(Collection<ThreadGroup> groups, ScheduledExecutorService scheduledExecutor) {
            this.groups = groups;
            this.scheduledExecutor = scheduledExecutor;
        }

        /**
         * Interrupt and destroy thread groups
         */
        @FFDCIgnore(IllegalThreadStateException.class)
        @Override
        public Void run() {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            if (trace && tc.isEntryEnabled())
                Tr.entry(this, tc, "run", groups);

            for (Iterator<ThreadGroup> it = groups.iterator(); it.hasNext();) {
                ThreadGroup group = it.next();
                boolean remove = true;
                group.interrupt();
                if (!group.isDestroyed())
                    try {
                        group.destroy();
                    } catch (IllegalThreadStateException x) {
                        remove = group.isDestroyed();
                    }
                if (remove)
                    it.remove();
            }

            // Reschedule if we couldn't destroy all of the thread groups
            if (!groups.isEmpty())
                scheduledExecutor.schedule(Executors.callable(this), DESTROY_RETRY_INTERVAL_MS, TimeUnit.MILLISECONDS);

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run", "remaining: " + groups);
            return null;
        }
    }
}
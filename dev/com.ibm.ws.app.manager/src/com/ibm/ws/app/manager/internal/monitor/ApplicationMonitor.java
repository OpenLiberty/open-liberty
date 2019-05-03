/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal.monitor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.AppMessageHelper;
import com.ibm.ws.app.manager.NotificationHelper;
import com.ibm.ws.app.manager.internal.ApplicationInstallInfo;
import com.ibm.ws.classloading.ClassLoadingButler;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.DefaultNotification;
import com.ibm.wsspi.adaptable.module.Notifier;
import com.ibm.wsspi.adaptable.module.Notifier.Notification;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationMonitoringInformation;

/**
 * Application monitoring service. It monitors all installed applications for changes and acts accordingly
 */
@Component(service = ApplicationMonitor.class, immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM")
public class ApplicationMonitor {
    private final static TraceComponent tc = Tr.register(ApplicationMonitor.class);

    public interface UpdateHandler {
        public void handleMonitorUpdate(boolean shouldRemove);
    }

    private final AtomicReference<ApplicationMonitorConfig> _config = new AtomicReference<ApplicationMonitorConfig>();
    private final AtomicReference<ExecutorService> _executorService = new AtomicReference<ExecutorService>();

    /** This is a map of ArtifactListeners mapped to the PID of the app they are listening to */
    private final ConcurrentHashMap<String, ApplicationListeners> _appListeners = new ConcurrentHashMap<String, ApplicationListeners>();

    @Activate
    protected void activate(ComponentContext ctx, Map<String, Object> config) {
        ctx.getBundleContext();
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        //
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx, int reason) {
        // Stop all listeners.
        for (ApplicationListeners listeners : _appListeners.values()) {
            listeners.stopListeners(true);
        }
    }

    @Reference(name = "executorService")
    protected void setExecutorService(ScheduledExecutorService executorService) {
        _executorService.set(executorService);
    }

    protected void unsetExecutorService(ScheduledExecutorService executorService) {
        _executorService.set(null);
    }

    public ApplicationMonitorConfig getConfig() {
        return _config.get();
    }

    /**
     * Starts the service with properties
     *
     * @param properties - a collection of optional variables set by the user in the server xml
     */
    public void refresh(ApplicationMonitorConfig config) {

        _config.set(config);

        UpdateTrigger trigger = config.getUpdateTrigger();

        if (trigger != UpdateTrigger.DISABLED) {
            // Start all existing listeners
            for (ApplicationListeners listeners : _appListeners.values()) {
                listeners.startListeners(config.getPollingRate(), config.getUpdateTrigger() == UpdateTrigger.MBEAN);
            }
        } else {
            // Stop all existing listeners
            for (ApplicationListeners listeners : _appListeners.values()) {
                listeners.stopListeners(false);
            }
        }
    }

    /**
     * adds an application's information to the update monitor
     */
    @FFDCIgnore(value = UnableToAdaptException.class)
    public void addApplication(ApplicationInstallInfo installInfo) {
        // ...and now create the new... start by asking the handler what needs monitoring
        final Collection<Notification> notificationsToMonitor;
        final boolean listenForRootStructuralChanges;
        ApplicationMonitoringInformation ami = installInfo.getApplicationMonitoringInformation();
        if (ami != null) {
            notificationsToMonitor = ami.getNotificationsToMonitor();
            listenForRootStructuralChanges = ami.isListeningForRootStructuralChanges();
        } else {
            notificationsToMonitor = null;
            listenForRootStructuralChanges = true;
        }

        try {
            // Now create the listeners for these notifications
            ApplicationListeners listeners = new ApplicationListeners(installInfo.getUpdateHandler(), _executorService);

            /*
             * Go through all of the notifications to monitor and create a listener for it. Note we also always create a different type of listener for root so if the app is
             * deleted we
             * know about it. Optionally (based on the handler) this might also monitor for files being added or removed from the root to trigger an update, i.e. if a WAR gets
             * added to
             * the root of an EAR.
             */
            if (notificationsToMonitor != null) {
                for (Notification notificationToMonitor : notificationsToMonitor) {
                    ApplicationListener listener = new ApplicationListener(notificationToMonitor, listeners, installInfo);
                    listeners.addListener(listener);
                }

                // If the handler did request monitoring then we still need a non-recursive handler to check root
                listeners.addListener(new RootApplicationListener(installInfo.getContainer(), listenForRootStructuralChanges, listeners));
            } else {
                /*
                 * If the handler didn't give us any information about what to monitor then monitor the whole application, note we use another type of listener again here that will
                 * monitor for root deletions or updates to any part of the application
                 */
                listeners.addListener(new CompleteApplicationListener(installInfo.getContainer(), listeners));
            }

            ApplicationListeners old = _appListeners.put(installInfo.getPid(), listeners);

            if (old != null) {
                old.stopListeners(true);
            }

            // If we're actively scanning, start the new listener
            ApplicationMonitorConfig config = _config.get();
            if (config.getUpdateTrigger() != UpdateTrigger.DISABLED) {
                listeners.startListeners(config.getPollingRate(), config.getUpdateTrigger() == UpdateTrigger.MBEAN);
            }
        } catch (UnableToAdaptException e) {
            // Ignore, we just won't monitor this application but do put out a warning message
            AppMessageHelper.get(installInfo.getHandler()).warning("APPLICATION_MONITORING_FAIL", installInfo.getName());
        }
    }

    /**
     * Removes an application from the update monitor
     *
     * @param pid
     */
    public void removeApplication(String pid) {
        // remove the application listener from the set we know about and stop it
        ApplicationListeners listeners = _appListeners.remove(pid);

        //check that the app is known, this can be run after the app is already removed.
        if (listeners != null) {
            listeners.stopListeners(true);
        }
    }

    /**
     * This class keeps track of information about the listeners registered for a single application. This enables you to track multiple listeners against different roots within
     * the application in one place and also stores some information about the application itself (such as the location).
     */
    private static final class ApplicationListeners {

        private final ApplicationMonitor.UpdateHandler updateHandler;
        private final AtomicReference<ExecutorService> executorService;
        /** This is the collection of listeners for this application */
        private final Collection<BaseApplicationListener> listeners = new HashSet<BaseApplicationListener>();

        private FutureTask<Object> future = null;
        private EventType eventType;
        private boolean start = true;

        /**
         * Constructs a new instance of the application listeners.
         *
         * @param applicationLocation The location for this application
         */
        public ApplicationListeners(ApplicationMonitor.UpdateHandler updateHandler, AtomicReference<ExecutorService> executorService) {
            this.updateHandler = updateHandler;
            this.executorService = executorService;
        }

        /**
         * This will add a new listener to the collection of listeners for this application.
         *
         * @param listener The new listener
         */
        private void addListener(BaseApplicationListener listener) {
            listeners.add(listener);
        }

        /**
         * This will stop all of the listeners registered for this application.
         */
        private void stopListeners(boolean terminal) {
            synchronized (this) {
                start = !!!terminal;
            }
            for (BaseApplicationListener listener : listeners) {
                listener.stop();
            }
        }

        /**
         * This will start all of the listeners registered for this application. It will also update the listener to use the current settings for {@link #pollingInterval} and
         * {@link #useMBean}
         */
        private void startListeners(long interval, boolean useMBean) {
            final boolean doStart;
            synchronized (this) {
                doStart = start;
            }
            if (doStart) {
                for (BaseApplicationListener listener : listeners) {
                    listener.start(interval, useMBean);
                }
            }
        }

        /**
         * This method will queue up an event to be done on the application being managed by this object. It will not perform the event immediately, instead it will wait 200ms to
         * see if a second request comes in, this way if an update and delete happen simultaneously (or through two different listeners) then only one update is sent back to the
         * application configuration manager.
         *
         * @param eventToPerform
         */
        public synchronized void queueEvent(final EventType eventToPerform) {
            // See if we are half way through another future, if we are cancel it
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }

            // REMOVE is more important than UPDATE so pick this one if it is set
            if (eventType == null || eventType != EventType.REMOVE) {
                eventType = eventToPerform;
            }

            ExecutorService executor = executorService.get();
            if (executor != null) {
                // Now create the future that will do the sleep waiting for a second request
                future = new FutureTask<Object>(new Callable<Object>() {
                    @Override
                    @FFDCIgnore(value = InterruptedException.class)
                    public Object call() {
                        try {
                            Thread.sleep(200);
                            executeEvent();
                        } catch (InterruptedException e) {
                        }
                        return null;
                    }
                });

                // Now execute the future
                executor.execute(future);
            }
        }

        /**
         * This method will execute whichever event is currently stored on the {@link #eventType} at the moment.
         */
        private synchronized void executeEvent() {
            if ((eventType != null) && (start)) {
                // Run the event
                updateHandler.handleMonitorUpdate(eventType == EventType.REMOVE);

                /*
                 * Now null the future and event type on the app listeners so that a future call to queue event won't think there is a delete that needs to be performed, we do it
                 * in here to guarantee that it will be done before the next queue event can happen
                 */
                future = null;
                eventType = null;
            }
        }
    }

    private enum EventType {
        UPDATE,
        REMOVE
    }

    private static abstract class BaseApplicationListener implements com.ibm.ws.adaptable.module.NotifierExtension.NotificationListener {

        /** This is the notifier that the listener is registered against */
        protected final Notifier applicationNotifier;
        /** <code>true</code> if this listener is currently listening to the application */
        protected final AtomicBoolean isListening = new AtomicBoolean(false);
        /** Information about which containers notification mechanism we should be using and which entries and containers within it we should be listening to */
        protected final Notification monitoringInformation;

        protected final ApplicationListeners listeners;

        /** This listener's ID */
        private final String id = "com.ibm.ws.app.listener";

        /**
         * Constructs a new instance of this listener and creates the notifier to which we'll be registered. It does not actually start the listener though.
         *
         * @param applicationProperties The properties for the application being monitored
         * @param monitoringContainerInformation Information about which containers notification mechanism we should be using and which entries and containers within it we should
         *            be listening to
         * @throws UnableToAdaptException If we cannot adapt the root container to a {@link Notifier}
         */
        public BaseApplicationListener(Notification monitoringInformation,
                                       ApplicationListeners listeners) throws UnableToAdaptException {
            this.monitoringInformation = monitoringInformation;
            this.applicationNotifier = monitoringInformation.getContainer().adapt(Notifier.class);
            this.listeners = listeners;
        }

        /**
         * This will start the listener by registering it with the {@link Notifier}. It will also update the notifier to use the current settings for {@link #pollingInterval} and
         * {@link #useMBean}.
         */
        private synchronized void start(long interval, boolean useMBean) {
            applicationNotifier.setNotificationOptions(interval, useMBean);
            if (isListening.compareAndSet(false, true)) {
                applicationNotifier.registerForNotifications(monitoringInformation, this);

            }
        }

        /**
         * This will stop the listener and unregister it from the {@link Notifier}
         */
        private synchronized void stop() {
            if (isListening.compareAndSet(true, false)) {
                applicationNotifier.removeListener(this);
            }
        }

        /**
         * Returns this listener's ID.
         *
         * @return This listener's ID.
         */
        public String getId() {
            return id;
        }

    }

    /**
     * This notification listener only listens for deletes of the root (which causes the app to be deleted) or deletes and additions directly under the root (which cause an app
     * update). It uses a non-recursive monitor against root (path of "!/")
     */
    private static final class RootApplicationListener extends BaseApplicationListener {

        private final boolean listenForRootStructuralChanges;

        /**
         * Constructs a new instance of this listener and creates the notifier to which we'll be registered. It does not actually start the listener though.
         *
         * @param applicationProperties The properties for the application being monitored
         * @param monitoringContainerInformation Information about which containers notification mechanism we should be using and which entries and containers within it we should
         *            be listening to
         * @throws UnableToAdaptException If we cannot adapt the root container to a {@link Notifier}
         */
        public RootApplicationListener(Container container,
                                       boolean listenForRootStructuralChanges, ApplicationListeners listeners) throws UnableToAdaptException {
            super(new DefaultNotification(container, "!/"), listeners);
            this.listenForRootStructuralChanges = listenForRootStructuralChanges;
        }

        /** {@inheritDoc} */
        @Override
        public void notifyEntryChange(Notification added, Notification removed, Notification modified) {
            // We are listening to the whole application so if removed includes "/" we need to remove the app
            if (removed.getPaths().contains("/")) {
                listeners.queueEvent(EventType.REMOVE);
            } else if (this.listenForRootStructuralChanges && (!added.getPaths().isEmpty() || !removed.getPaths().isEmpty())) {
                // We are listening for structural changes to this application (i.e. WARs being added/removed to the root
                // of an EAR) and one of these has occurred so update the application
                listeners.queueEvent(EventType.UPDATE);
            } else if (modified.getPaths().contains("/")) {
                // The root element itself has changed, this will happen, for instance if the Loose XML itself has changed,
                // in which case we need to do an update of the app as anything might of happened!
                listeners.queueEvent(EventType.UPDATE);
            }

        }

    }

    /**
     * This class will listen for changes within the application that indicate that the application needs updating. It is responsible for registering itself against the
     * notification framework when start is called.
     */
    private static final class ApplicationListener extends BaseApplicationListener {

        private final static String[] MINOR_UPDATE_FILE_EXTENSIONS;
        static {
            String builtInPatterns = ".class,.jsp,.jspx,.jsw,.jsv,.jspf,.tld,.tag";
            String userPatterns = System.getProperty("com.ibm.ws.app.manager.minorUpdateFileExtensions");
            String minorUpdatePatterns = builtInPatterns + (userPatterns == null ? "" : "," + userPatterns);
            MINOR_UPDATE_FILE_EXTENSIONS = minorUpdatePatterns.split(",");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "minor file patterns: ", new Object[] { MINOR_UPDATE_FILE_EXTENSIONS });
            }

        }

        private final ApplicationInstallInfo appInfo;
        private final ClassLoadingButler butler;

        /**
         * Constructs a new instance of this listener and creates the notifier to which we'll be registered. It does not actually start the listener though.
         *
         * @param applicationProperties The properties for the application being monitored
         * @param monitoringContainerInformation Information about which containers notification mechanism we should be using and which entries and containers within it we should
         *            be listening to
         * @throws UnableToAdaptException If we cannot adapt the root container to a {@link Notifier} of if we cannot adapt the app's container to a ClassLoadingButler
         */
        public ApplicationListener(Notification monitoringInformation,
                                   ApplicationListeners listeners, ApplicationInstallInfo appInfo) throws UnableToAdaptException {
            super(monitoringInformation, listeners);
            this.appInfo = appInfo;
            this.butler = appInfo.getContainer().adapt(ClassLoadingButler.class);
        }

        private boolean isMinorUpdate(Notification modified) {
            // if there are no paths to process, this is a minor update
            if (modified.getPaths().isEmpty()) {
                return true;
            }

            // Check for files with file extensions that designate them as a
            // minor (or potentially minor in the case of *.class files) change
            // that does not require a restart - i.e. changes to JSP files
            // should not require a restart.  Changes to a web.xml should.
            for (String path : modified.getPaths()) {
                boolean hasMinorExtension = false;
                for (String pattern : MINOR_UPDATE_FILE_EXTENSIONS) {
                    if (path.toLowerCase().endsWith(pattern)) {
                        hasMinorExtension = true;
                        break;
                    }
                }

                if (!hasMinorExtension) {
                    return false;
                }
            }

            // Now check to see if the classes being modified can do so without
            // requiring a restart.  This is only possible if class redefining
            // is enabled AND if the changes to the class allows it to be
            // redefined - usually this means just changes to the internals of
            // a method body.
            boolean isMinor = butler.redefineClasses(modified);

            if (isMinor) {
                String msg = AppMessageHelper.get(appInfo.getHandler()).formatMessage("APPLICATION_UPDATED", appInfo.getName());
                NotificationHelper.broadcastChange(appInfo.getMBeanNotifier(), appInfo.getMBeanName(), "application.update", Boolean.TRUE, msg);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, msg);
                }
            }
            return isMinor;

        }

        /** {@inheritDoc} */
        @Override
        public void notifyEntryChange(Notification added, Notification removed, Notification modified) {

            if (!added.getPaths().isEmpty() || !removed.getPaths().isEmpty() || !isMinorUpdate(modified)) {
                // Something has changed so we need to update the application. We don't know if we are a root listener so don't remove the app if "/" is deleted
                listeners.queueEvent(EventType.UPDATE);
            }
        }

    }

    /**
     * This class will listen for changes within the application that indicate that the application needs updating. It will also monitor the root directory and if it is deleted
     * delete the application. It is responsible for registering itself against the notification framework when start is called. It registers using a recursive notification against
     * the root (a path of "!")
     */
    private static final class CompleteApplicationListener extends BaseApplicationListener {

        /**
         * Constructs a new instance of this listener and creates the notifier to which we'll be registered. It does not actually start the listener though.
         *
         * @param applicationProperties The properties for the application being monitored
         * @param monitoringContainerInformation Information about which containers notification mechanism we should be using and which entries and containers within it we should
         *            be listening to
         * @throws UnableToAdaptException If we cannot adapt the root container to a {@link Notifier}
         */
        public CompleteApplicationListener(Container container,
                                           ApplicationListeners listeners) throws UnableToAdaptException {
            super(new DefaultNotification(container, "/"), listeners);
        }

        /** {@inheritDoc} */
        @Override
        public void notifyEntryChange(Notification added, Notification removed, Notification modified) {
            // We are listening to the whole application so if removed includes "/" we need to remove the app
            if (removed.getPaths().contains("/")) {
                listeners.queueEvent(EventType.REMOVE);
            } else if (!added.getPaths().isEmpty() || !removed.getPaths().isEmpty() || !modified.getPaths().isEmpty()) {
                // Otherwise it's an update if anything else happened
                listeners.queueEvent(EventType.UPDATE);
            }
        }

    }

}
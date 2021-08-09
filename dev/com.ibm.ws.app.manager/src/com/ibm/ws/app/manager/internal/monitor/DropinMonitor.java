/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal.monitor;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
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
import com.ibm.ws.app.manager.internal.AppManagerConstants;
import com.ibm.ws.app.manager.internal.lifecycle.ServiceReg;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * App manager file monitoring service which monitors a given directory for applications being added/deleted and starts/stops them as
 * appropriate.
 */
@Component(service = DropinMonitor.class, immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM")
public class DropinMonitor {
    private static final TraceComponent _tc = Tr.register(DropinMonitor.class, new String[] { "applications", com.ibm.ws.app.manager.internal.AppManagerConstants.TRACE_GROUP },
                                                          com.ibm.ws.app.manager.internal.AppManagerConstants.TRACE_MESSAGES,
                                                          "com.ibm.ws.app.manager.internal.monitor.DropinMonitor");

    private BundleContext _ctx;
    private WsLocationAdmin locationService;
    protected ConfigurationAdmin configAdmin;
    protected final AtomicReference<File> monitoredDirectory = new AtomicReference<File>();
    private final AtomicBoolean createdMonitoredDir = new AtomicBoolean();
    protected final AtomicReference<ApplicationMonitorConfig> _config = new AtomicReference<ApplicationMonitorConfig>();
    protected final ConcurrentMap<String, ServiceReg<FileMonitor>> _monitors = new ConcurrentHashMap<String, ServiceReg<FileMonitor>>();
    protected final ConcurrentMap<String, Configuration> _configs = new ConcurrentHashMap<String, Configuration>();
    private final ServiceReg<FileMonitor> _coreMonitor = new ServiceReg<FileMonitor>();

    @Activate
    protected void activate(ComponentContext ctx) {
        _ctx = ctx.getBundleContext();
        _coreMonitor.setProperties(new Hashtable<String, Object>());
        _coreMonitor.setProperty(Constants.SERVICE_VENDOR, "IBM");
        _coreMonitor.setProperty(FileMonitor.MONITOR_RECURSE, false);
        _coreMonitor.setProperty(FileMonitor.MONITOR_FILTER, ".*"); // find all types of file (including folders)

        // delete all existing configurations installed by any previous instance of this class
        deleteAllConfiguredApplications();
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx, int reason) {
        stop();
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        //
    }

    @Reference(name = "locationService")
    protected void setLocationService(WsLocationAdmin locationService) {
        this.locationService = locationService;
    }

    protected void unsetLocationService(WsLocationAdmin locationService) {
    }

    @Reference(name = "configAdmin")
    protected void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    protected void unsetConfigAdmin(ConfigurationAdmin configAdmin) {
    }

    private class FileMonitorImpl implements FileMonitor {
        private final String _type;
        private final String _filePath;

        public FileMonitorImpl() {
            this(null, null);
        }

        public FileMonitorImpl(String type, String filePath) {
            _type = type;
            _filePath = filePath;
        }

        /**
         * {@inheritDoc}
         *
         * This is called when the initial scan completes on setup and returns a list of files already in
         * the dropins directory. It starts these applications.
         */
        @Override
        public void onBaseline(Collection<File> currentFiles) {
            processNewFiles(currentFiles);
        }

        /**
         * @param currentFiles
         * @return
         */
        /**
         * {@inheritDoc}
         *
         * This method is called each time a scan of the dropins directory completes.
         *
         */
        @Override
        public void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
            for (File f : deletedFiles) {
                if (_tc.isEventEnabled()) {
                    Tr.event(_tc, "File '" + f.getName() + "' removed from monitoring directory " + monitoredDirectory.get().getAbsolutePath());
                }
                ServiceReg<FileMonitor> mon = _monitors.remove(f.getAbsolutePath());
                if (mon == null) {
                    stopApplication(f);
                } else if (_type != null) {
                    mon.unregister();
                }
            }
            processNewFiles(createdFiles);
        }

        private void processNewFiles(Collection<File> currentFiles) {
            for (File f : currentFiles) {
                if (_filePath != null && _filePath.equals(f.getAbsolutePath())) {
                    continue;
                }
                if (_tc.isEventEnabled()) {
                    Tr.event(_tc, "File '" + f.getName() + "' added to monitoring directory " + monitoredDirectory.get().getAbsolutePath());
                }
                String name = f.getName();
                if (f.isDirectory() && name.indexOf('.') == -1 && _type == null) {
                    String filePath = f.getAbsolutePath();
                    ServiceReg<FileMonitor> mon = new ServiceReg<FileMonitor>();
                    if (_monitors.putIfAbsent(filePath, mon) == null) {
                        mon.setProperties(new Hashtable<String, Object>());
                        mon.setProperty(Constants.SERVICE_VENDOR, "IBM");
                        mon.setProperty(FileMonitor.MONITOR_INTERVAL, _config.get().getPollingRate());
                        mon.setProperty(FileMonitor.MONITOR_RECURSE, false);
                        mon.setProperty(FileMonitor.MONITOR_INCLUDE_SELF, true);
                        mon.setProperty(FileMonitor.MONITOR_FILTER, ".*"); // find all types of file (including folders)
                        mon.setProperty(FileMonitor.MONITOR_DIRECTORIES, new String[] { filePath });

                        // Don't register new file monitors while we're shutting down
                        if (FrameworkState.isStopping())
                            return;
                        mon.register(_ctx, FileMonitor.class, new FileMonitorImpl(name, filePath));
                    }
                } else {
                    if (!!!f.isHidden()) {
                        //is current file a directory
                        if (f.isDirectory() || f.isFile()) {
                            startApplication(f, _type);
                        } else {
                            Tr.error(_tc, "UNABLE_TO_DETERMINE_APPLICATION_TYPE", name);
                        }
                    } else {
                        if (_tc.isDebugEnabled()) {
                            Tr.debug(_tc, "ignoring hidden file in dropins dir called", f);
                        }
                    }
                }
            }
        }

    }

    /**
     * Initialize the dropins manager based on configured properties
     *
     * @param properties
     * @return
     */
    public synchronized void refresh(ApplicationMonitorConfig config) {
        if (config != null && config.isDropinsMonitored()) {
            //ApplicationMonitorConfig prevConfig = _config.getAndSet(config);
            _config.set(config);

            // keep track of the old monitored directory to see if we need to uninstall apps
            File previousMonitoredDirectory = null;
            boolean createdPreviousDirectory = createdMonitoredDir.get();

            String newMonitoredFolder = config.getLocation();

            //if the user set the monitored folder location to be empty or it is somehow null
            if ((newMonitoredFolder != null) && (!newMonitoredFolder.equals(""))) {
                previousMonitoredDirectory = updateMonitoredDirectory(newMonitoredFolder);
            }

            if (!!!_coreMonitor.isRegistered()) {
                stopRemovedApplications();
                // The service has not been started yet.
                // load the pids for applications already setup before starting the service fully

                configureCoreMonitor(config);
                _coreMonitor.register(_ctx, FileMonitor.class, new FileMonitorImpl());
                Tr.audit(_tc, "APPLICATION_MONITOR_STARTED", newMonitoredFolder);
            } else if (!!!monitoredDirectory.get().equals(previousMonitoredDirectory)) {
                // The directory has changed so stop the old applications
                stopAllStartedApplications();

                // Need to re-register because file monitor doesn't appear to work from modified events.
                _coreMonitor.unregister();

                // Update the registration with new config before registering the service again
                configureCoreMonitor(config);
                _coreMonitor.register(_ctx, FileMonitor.class, new FileMonitorImpl());

                // Tidy up old location if we built it
                tidyUpMonitoredDirectory(createdPreviousDirectory, previousMonitoredDirectory);
            }
        } else {
            // the monitoring service has been disabled: stop/deregister the service
            stopAllStartedApplications();
            stop();
        }
    }

    /**
     * Set the properties for the _coreMonitor that are modifiable. All others
     * are set when this component activates ( {@link DropinMonitor#activate()} ).
     *
     * @param config
     */
    private void configureCoreMonitor(ApplicationMonitorConfig config) {
        _coreMonitor.setProperty(FileMonitor.MONITOR_INTERVAL, config.getPollingRate());
        _coreMonitor.setProperty(FileMonitor.MONITOR_DIRECTORIES, new String[] { monitoredDirectory.get().getAbsolutePath() });
    }

    private void stopAllStartedApplications() {
        for (Configuration config : _configs.values()) {
            try {
                config.delete();
            } catch (IOException e) {
                // FFDC and move on.
            }
        }
        _configs.clear();
    }

    /**
     * Stops the service and all applications started by it
     */
    public synchronized void stop() {

        _coreMonitor.unregister();

        for (ServiceReg<FileMonitor> reg : _monitors.values()) {
            reg.unregister();
        }

        _monitors.clear();

        // Tidy up old location if we built it
        tidyUpMonitoredDirectory(createdMonitoredDir.get(), monitoredDirectory.get());
    }

    /**
     * Clean up the monitored directory if we built it and it is empty.
     * (i.e. clean up after ourselves if we can)
     *
     */
    private void tidyUpMonitoredDirectory(boolean createdDir, File dirToCleanup) {
        if (createdDir && dirToCleanup != null) {
            File[] fileListing = dirToCleanup.listFiles();
            if (fileListing == null || fileListing.length == 0) {
                if (!!!dirToCleanup.delete()) {
                    Tr.error(_tc, "MONITOR_DIR_CLEANUP_FAIL", dirToCleanup);
                } else if (_tc.isDebugEnabled()) {
                    //put the message that we were successful in debug so that we know it worked in automated testing
                    Tr.debug(_tc, "Server deleted the old dropins directory " + dirToCleanup);
                }
            }
        }
    }

    /**
     * Takes a file and a pid and will stop the file with that PID from running, and remove the pid to app
     * mapping from the app pid mapper
     *
     * @param ca
     *
     * @param currentFile the application's file (can be file or directory)
     */
    private void stopApplication(File currentFile) {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, "Stopping dropin application '" + currentFile.getName() + "'");
        }
        String filePath = getAppLocation(currentFile);

        try {
            Configuration config = _configs.remove(filePath);
            if (config != null) {
                config.delete();
            }
        } catch (Exception e) {
            getAppMessageHelper(null, filePath).error("MONITOR_APP_STOP_FAIL", currentFile.getName());
        }
    }

    /**
     * @param currentFile
     * @return
     */
    private String getAppLocation(File currentFile) {
        String filePath = currentFile.getAbsolutePath();

        // Strip off any .xml bits before doing any further manipulation
        if (filePath.endsWith(".xml")) {
            filePath = filePath.substring(0, filePath.length() - 4);
        }
        return filePath;
    }

    /**
     * Returns the message helper for the specified application handler type.
     *
     * @param type     application handler type
     * @param fileName file name from which type can be inferred if not specified
     * @return the message helper for the specified application handler type.
     */
    private AppMessageHelper getAppMessageHelper(String type, String fileName) {
        if (type == null && fileName != null) {
            String[] parts = fileName.split("[\\\\/]");
            if (parts.length > 0) {
                String last = parts[parts.length - 1];
                int dot = last.indexOf('.');
                type = dot >= 0 ? last.substring(dot + 1) : parts.length > 1 ? parts[parts.length - 2] : null;
            }
        }
        if (type != null)
            try {
                String filter = FilterUtils.createPropertyFilter(AppManagerConstants.TYPE, type.toLowerCase());
                @SuppressWarnings("rawtypes")
                Collection<ServiceReference<ApplicationHandler>> refs = _ctx.getServiceReferences(ApplicationHandler.class, filter);
                if (refs.size() > 0) {
                    @SuppressWarnings("rawtypes")
                    ServiceReference<ApplicationHandler> ref = refs.iterator().next();
                    ApplicationHandler<?> appHandler = _ctx.getService(ref);
                    try {
                        return AppMessageHelper.get(appHandler);
                    } finally {
                        _ctx.ungetService(ref);
                    }
                }
            } catch (InvalidSyntaxException x) {
            }
        return AppMessageHelper.get(null);
    }

    /**
     * Takes a file and an optional file type, and updates the file. If no type is given it will use the
     * extension of the file given.
     *
     * @param currentFile the file of the application to install. Can be a directory
     * @param type        the type of the application.
     *
     */
    private void startApplication(File currentFile, String type) {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, "Starting dropin application '" + currentFile.getName() + "'");
        }

        // Check to make sure that the app isn't configured in server.xml.
        try {
            Configuration[] configuredApps = configAdmin.listConfigurations(AppManagerConstants.APPLICATION_FACTORY_FILTER);
            if (configuredApps != null) {
                for (Configuration c : configuredApps) {
                    Dictionary<String, Object> properties = c.getProperties();
                    String location = (String) properties.get(AppManagerConstants.LOCATION);
                    if (location != null && monitoredDirectory.get() != null) {
                        File configuredFile = new File(locationService.resolveString(location));
                        if (configuredFile != null && currentFile.compareTo(configuredFile) == 0) {
                            Tr.warning(_tc, "dropins.app.also.configured", location);
                            return;
                        }
                    }
                }
            }
        } catch (IOException | InvalidSyntaxException e1) {
            // Just FFDC
        }

        String filePath = getAppLocation(currentFile);

        try {
            Configuration config = _configs.get(filePath);

            // This is a new application we don't know about so we need to create it.
            if (config == null) {
                Hashtable<String, Object> appConfigSettings = new Hashtable<String, Object>();

                appConfigSettings.put("location", filePath);
                if (type != null) {
                    appConfigSettings.put("type", type);
                }

                //for application monitor to know if an application is installed by dropin monitor we need to add a unique variable
                appConfigSettings.put(AppManagerConstants.AUTO_INSTALL_PROP, true);

                //get a handle on the configuration object
                config = configAdmin.createFactoryConfiguration(AppManagerConstants.APPLICATIONS_PID);
                config.update(appConfigSettings);
                _configs.put(filePath, config);

            }
        } catch (Exception e) {
            getAppMessageHelper(type, filePath).error("MONITOR_APP_START_FAIL", filePath);
        }

    }

    /**
     * Not thread safe: only call from threadsafe method
     *
     * @param newMonitoredFolder
     * @return
     */
    private File updateMonitoredDirectory(String newMonitoredFolder) {

        File oldDir = monitoredDirectory.get();
        File newDir = new File(newMonitoredFolder);

        // If it is relative, resolve against server dir
        if (!!!newDir.isAbsolute()) {
            newMonitoredFolder = locationService.resolveString(WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR + newMonitoredFolder);
            newDir = new File(newMonitoredFolder);
        }

        if (!!!newDir.equals(oldDir)) {
            if (monitoredDirectory.compareAndSet(oldDir, newDir)) {
                for (ServiceReg<FileMonitor> mon : _monitors.values()) {
                    mon.unregister();
                }
                _monitors.clear();

                if (!!!newDir.exists()) {
                    createdMonitoredDir.set(newDir.mkdirs());
                }
            } else {
                oldDir = null;
            }
        } else {
            oldDir = null;
        }

        return oldDir;
    }

    /**
     * Call this method to stop all applications started by the dropin monitor (for example if dropins is disabled
     * on a server startup but the applications it started last server run are still there).
     */
    private void deleteAllConfiguredApplications() {
        try {
            Configuration[] configs = configAdmin.listConfigurations("(&(service.factoryPid=" + AppManagerConstants.APPLICATIONS_PID + ")("
                                                                     + AppManagerConstants.AUTO_INSTALL_PROP + "=true))");
            if (configs != null) {
                for (Configuration c : configs) {
                    try {
                        c.delete();
                    } catch (Exception e) {
                        // Move on, but FFDC
                    }
                }
            }
        } catch (IOException e) {
            // Hmm, I guess this is a bad place to be, not sure what to do. I guess I just FFDC and move on.
            // A clean start will fix this issue.
        } catch (InvalidSyntaxException e) {
            // This should never happen, if it does we want the FFDC
        }
    }

    private void stopRemovedApplications() {
        for (String loc : _configs.keySet()) {
            File f = new File(loc);
            if (!!!f.exists()) {
                f = new File(loc + ".xml");
                if (!!!f.exists()) {
                    Configuration config = _configs.remove(loc);
                    try {
                        config.delete();
                    } catch (IOException e) {
                        // FFDC and move on.
                    }
                }
            }
        }
    }
}
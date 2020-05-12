/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.internal;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.filemonitor.FileBasedActionable;
import com.ibm.ws.security.filemonitor.SecurityFileMonitor;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.security.ltpa.TokenFactory;

/**
 * DS service class representing an LTPA token configuration.
 * <p>
 * Note this class does not provide anything via DS until the LTPAKeyCreator
 * is finished creating the LTPA keys.
 * <p>
 * This class collaborates very closely with the LTPAKeyCreator.
 *
 * @see LTPAKeyCreateTask
 */
public class LTPAConfigurationImpl implements LTPAConfiguration, FileBasedActionable {

    private static final TraceComponent tc = Tr.register(LTPAConfigurationImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static final String KEY_LOCATION_SERVICE = "locationService";
    static final String KEY_EXECUTOR_SERVICE = "executorService";
    static final String KEY_CHANGE_SERVICE = "ltpaKeysChangeNotifier";
    static final String DEFAULT_CONFIG_LOCATION = "${server.config.dir}/resources/security/ltpa.keys";
    static final String DEFAULT_OUTPUT_LOCATION = "${server.output.dir}/resources/security/ltpa.keys";
    static final String KEY_AUTH_FILTER_REF = "authFilterRef";
    private final AtomicServiceReference<WsLocationAdmin> locationService = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_SERVICE);
    private final AtomicServiceReference<ExecutorService> executorService = new AtomicServiceReference<ExecutorService>(KEY_EXECUTOR_SERVICE);
    private final AtomicServiceReference<LTPAKeysChangeNotifier> ltpaKeysChangeNotifierService = new AtomicServiceReference<LTPAKeysChangeNotifier>(KEY_CHANGE_SERVICE);
    private ServiceRegistration<LTPAConfiguration> registration = null;
    private volatile ComponentContext cc = null;
    private LTPAKeyCreateTask createTask;
    private TokenFactory factory;
    private LTPAKeyInfoManager ltpaKeyInfoManager;
    private String keyImportFile;
    @Sensitive
    private String keyPassword;
    private long keyTokenExpiration;
    private long monitorInterval;
    private SecurityFileMonitor ltpaFileMonitor;
    private ServiceRegistration<FileMonitor> ltpaFileMonitorRegistration;
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();;
    private final WriteLock writeLock = reentrantReadWriteLock.writeLock();
    private final ReadLock readLock = reentrantReadWriteLock.readLock();
    private String authFilterRef;

    protected void setExecutorService(ServiceReference<ExecutorService> ref) {
        executorService.setReference(ref);
    }

    protected void unsetExecutorService(ServiceReference<ExecutorService> ref) {
        executorService.unsetReference(ref);
    }

    protected void setLocationService(ServiceReference<WsLocationAdmin> reference) {
        locationService.setReference(reference);
    }

    protected void unsetLocationService(ServiceReference<WsLocationAdmin> reference) {
        locationService.unsetReference(reference);
    }

    protected void setLtpaKeysChangeNotifier(ServiceReference<LTPAKeysChangeNotifier> ref) {
        ltpaKeysChangeNotifierService.setReference(ref);
    }

    protected void unsetLtpaKeysChangeNotifier(ServiceReference<LTPAKeysChangeNotifier> ref) {
        ltpaKeysChangeNotifierService.unsetReference(ref);
    }

    protected void activate(ComponentContext context, Map<String, Object> props) {
        cc = context;
        locationService.activate(context);
        executorService.activate(context);
        ltpaKeysChangeNotifierService.activate(context);

        loadConfig(props);
        setupRuntimeLTPAInfrastructure();
    }

    private void loadConfig(Map<String, Object> props) {
        keyImportFile = (String) props.get(CFG_KEY_IMPORT_FILE);
        SerializableProtectedString sps = (SerializableProtectedString) props.get(CFG_KEY_PASSWORD);
        keyPassword = sps == null ? null : new String(sps.getChars());
        keyTokenExpiration = (Long) props.get(CFG_KEY_TOKEN_EXPIRATION);
        monitorInterval = (Long) props.get(CFG_KEY_MONITOR_INTERVAL);
        authFilterRef = (String) props.get(KEY_AUTH_FILTER_REF);
        resolveActualKeysFileLocation();
    }

    private void resolveActualKeysFileLocation() {
        if (isInDefaultOutputLocation()) {
            WsResource keysFileInServerConfig = locationService.getServiceWithException().resolveResource(DEFAULT_CONFIG_LOCATION);
            if (keysFileInServerConfig != null && keysFileInServerConfig.exists()) {
                String expandedKeysFileInServerConfig = locationService.getServiceWithException().resolveString(DEFAULT_CONFIG_LOCATION);
                keyImportFile = expandedKeysFileInServerConfig;
            }
        }
    }

    private boolean isInDefaultOutputLocation() {
        String expandedKeysFileInServerOutput = locationService.getServiceWithException().resolveString(DEFAULT_OUTPUT_LOCATION);
        return keyImportFile.equals(expandedKeysFileInServerOutput);
    }

    /**
     * To set the LTPA infrastructure, optionally create the LTPA file monitor and create keys.
     */
    private void setupRuntimeLTPAInfrastructure() {
        optionallyCreateFileMonitor();
        createTask = new LTPAKeyCreateTask(locationService.getService(), this);
        submitTaskToCreateLTPAKeys();
    }

    /**
     * Creates an LTPA file monitor when the monitor interval is greater than zero.
     */
    private void optionallyCreateFileMonitor() {
        if (monitorInterval > 0) {
            createFileMonitor();
        }
    }

    /**
     * Handles the creation of the LTPA file monitor.
     */
    private void createFileMonitor() {
        try {
            ltpaFileMonitor = new SecurityFileMonitor(this);
            setFileMonitorRegistration(ltpaFileMonitor.monitorFiles(Arrays.asList(keyImportFile), monitorInterval));
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception creating the LTPA file monitor.", e);
            }
        }
    }

    /**
     * Submits an asynchronous task to load the LTPA keys based on the current configuration state.
     * This method might also be called by the file monitor when there is an update to the LTPA keys file or
     * when the file is recreated after it is deleted.
     */
    @Override
    public void performFileBasedAction(Collection<File> files) {
        Tr.audit(tc, "LTPA_KEYS_TO_LOAD", keyImportFile);
        submitTaskToCreateLTPAKeys();
    }

    private void submitTaskToCreateLTPAKeys() {
        executorService.getService().execute(createTask);
    }

    /**
     * When the configuration is modified,
     *
     * <pre>
     * 1. If file name and expiration changed,
     * then remove the file monitor registration and reload LTPA keys.
     * 2. Else if only the monitor interval changed,
     * then remove the file monitor registration and optionally create a new file monitor.
     * 3. (Implicit)Else if only the key password changed,
     * then do not remove the file monitor registration and do not reload the LTPA keys.
     * </pre>
     */
    protected void modified(Map<String, Object> props) {
        String oldKeyImportFile = keyImportFile;
        Long oldKeyTokenExpiration = keyTokenExpiration;
        Long oldMonitorInterval = monitorInterval;

        loadConfig(props);

        if (isKeysConfigChanged(oldKeyImportFile, oldKeyTokenExpiration)) {
            unsetFileMonitorRegistration();
            Tr.audit(tc, "LTPA_KEYS_TO_LOAD", keyImportFile);
            setupRuntimeLTPAInfrastructure();
        } else if (isMonitorIntervalChanged(oldMonitorInterval)) {
            unsetFileMonitorRegistration();
            optionallyCreateFileMonitor();
        }
    }

    /**
     * The keys config is changed if the file or expiration were modified.
     * Changing the password by itself must not be considered a config change that should trigger a keys reload.
     */
    private boolean isKeysConfigChanged(String oldKeyImportFile, Long oldKeyTokenExpiration) {
        return ((oldKeyImportFile.equals(keyImportFile) == false) || (oldKeyTokenExpiration != keyTokenExpiration));
    }

    private boolean isMonitorIntervalChanged(Long oldMonitorInterval) {
        return oldMonitorInterval != monitorInterval;
    }

    protected void deactivate(ComponentContext context) {
        cc = null;
        if (registration != null) {
            registration.unregister();
            registration = null;
        }

        unsetFileMonitorRegistration();

        executorService.deactivate(context);
        locationService.deactivate(context);
        ltpaKeysChangeNotifierService.deactivate(context);
    }

    protected void unsetFileMonitorRegistration() {
        if (ltpaFileMonitorRegistration != null) {
            ltpaFileMonitorRegistration.unregister();
            ltpaFileMonitorRegistration = null;
        }
    }

    /**
     * Sets the LTPA file monitor registration.
     *
     * @param ltpaFileMonitorRegistration
     */
    protected void setFileMonitorRegistration(ServiceRegistration<FileMonitor> ltpaFileMonitorRegistration) {
        this.ltpaFileMonitorRegistration = ltpaFileMonitorRegistration;
    }

    /**
     * Retrieves the BundleContext, assuming we're still valid. If we've been
     * deactivated, then the registration no longer needs / can happen and in
     * that case return null.
     *
     * @return The BundleContext if available, {@code null} otherwise.
     */
    @Override
    public BundleContext getBundleContext() {
        if (cc != null) {
            return cc.getBundleContext();
        } else {
            return null;
        }
    }

    /**
     * Callback method for use by the LTPAKeyCreator class.
     * <p>
     * The LTPAKeyCreator register this instance as an LTPAConfiguration in
     * the OSGi service registry. When this instance is deactivated, we need
     * to unregister it.
     * <p>
     * It is possible this method never gets called if the LTPA keys could not
     * be read or created.
     *
     * @param registration ServiceRegistration to eventually unregister
     */
    void setRegistration(ServiceRegistration<LTPAConfiguration> registration) {
        this.registration = registration;
    }

    /**
     * Callback method for use by the LTPAKeyCreator class.
     * <p>
     * The LTPAKeyCreator will create the TokenFactory which corresponds to
     * this instance of the LTPAConfiguration.
     *
     * @param factory TokenFactory which corresponds to this configuration
     */
    void setTokenFactory(TokenFactory factory) {
        writeLock.lock();
        try {
            this.factory = factory;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TokenFactory getTokenFactory() {
        readLock.lock();
        try {
            return factory;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Callback method for use by the LTPAKeyCreator class.
     * <p>
     * The LTPAKeyCreator will create the LTPAKeyInfoManager and set it
     * within the LTPAConfigurationImpl instance.
     *
     * @param ltpaKeyInfoManager
     */
    void setLTPAKeyInfoManager(LTPAKeyInfoManager ltpaKeyInfoManager) {
        this.ltpaKeyInfoManager = ltpaKeyInfoManager;
    }

    /** {@inheritDoc} */
    @Override
    public LTPAKeyInfoManager getLTPAKeyInfoManager() {
        return ltpaKeyInfoManager;
    }

    /** {@inheritDoc} */
    @Override
    public String getKeyFile() {
        return keyImportFile;
    }

    /** {@inheritDoc} */
    @Sensitive
    @Override
    public String getKeyPassword() {
        return keyPassword;
    }

    /** {@inheritDoc} */
    @Override
    public long getTokenExpiration() {
        return keyTokenExpiration;
    }

    /**
     * Callback method for use by the LTPAKeyCreator class.
     * <p>
     * The LTPAKeyCreator will call this method when the configuration is ready.
     * <p>
     * It is possible this method never gets called if the LTPA keys could not
     * be read or created.
     */
    protected void configReady() {
        LTPAKeysChangeNotifier notifier = getLTPAKeysChangeNotifier();
        if (notifier != null) {
            notifier.notifyListeners();
        }
    }

    /**
     * Creates or returns a security change notifier.
     */
    protected LTPAKeysChangeNotifier getLTPAKeysChangeNotifier() {
        return ltpaKeysChangeNotifierService.getService();
    }

}

/*******************************************************************************
 * Copyright (c) 2012, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.internal;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import com.ibm.ws.config.xml.nester.Nester;
import com.ibm.ws.security.filemonitor.FileBasedActionable;
import com.ibm.ws.security.filemonitor.LTPAFileMonitor;
import com.ibm.ws.security.token.ltpa.LTPAConfiguration;
import com.ibm.ws.security.token.ltpa.LTPAKeyInfoManager;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.security.ltpa.TokenFactory;

import io.openliberty.checkpoint.spi.CheckpointPhase;

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
    static final String DEFAULT_CONFIG_DIRECTORY = "${server.config.dir}/resources/security/";
    static final String DEFAULT_OUTPUT_LOCATION = "${server.output.dir}/resources/security/ltpa.keys";
    static final String KEY_AUTH_FILTER_REF = "authFilterRef";
    static final String KEY_EXP_DIFF_ALLOWED = "expirationDifferenceAllowed";
    static protected final String KEY_SERVICE_PID = "service.pid";
    private final AtomicServiceReference<WsLocationAdmin> locationService = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_SERVICE);
    private final AtomicServiceReference<ExecutorService> executorService = new AtomicServiceReference<ExecutorService>(KEY_EXECUTOR_SERVICE);
    private final AtomicServiceReference<LTPAKeysChangeNotifier> ltpaKeysChangeNotifierService = new AtomicServiceReference<LTPAKeysChangeNotifier>(KEY_CHANGE_SERVICE);
    private ServiceRegistration<LTPAConfiguration> registration = null;
    private volatile ComponentContext cc = null;
    private LTPAKeyCreateTask createTask;
    private TokenFactory factory;
    private LTPAKeyInfoManager ltpaKeyInfoManager;
    private String primaryKeyImportFile;
    private String primaryKeyImportDir;
    @Sensitive
    private String primaryKeyPassword;
    private long keyTokenExpiration;
    private long monitorInterval;
    private LTPAFileMonitor ltpaFileMonitor;
    private ServiceRegistration<FileMonitor> ltpaFileMonitorRegistration;
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();;
    private final WriteLock writeLock = reentrantReadWriteLock.writeLock();
    private final ReadLock readLock = reentrantReadWriteLock.readLock();
    private String authFilterRef;
    private long expirationDifferenceAllowed;
    private boolean monitorValidationKeysDir;
    private String updateTrigger;
    private final List<Properties> validationKeys = new ArrayList<Properties>();
    // configValidationKeys are specified in the server xml configuration
    private List<Properties> configValidationKeys = null;
    // nonConfigValidationKeys are not specified in the server xml configuration
    // nonConfigValidationKeys are picked up by the directory monitor
    private List<Properties> nonConfigValidationKeys = null;
    private final Collection<File> currentlyDeletedFiles = new HashSet<File>();
    private static final Collection<File> allKeysFiles = new HashSet<File>();
    boolean isValidationKeysFileConfigured = false;

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

    /*
     * When FileMonitor is enabled, its onBaseline method will call performFileBasedAction(baselineFiles)
     * to process key files after loadConfig(props), but before submitTaskToCreateLTPAKeys().
     */
    protected void activate(ComponentContext context, Map<String, Object> props) {
        cc = context;
        locationService.activate(context);
        executorService.activate(context);
        ltpaKeysChangeNotifierService.activate(context);

        loadConfig(props);
        setupRuntimeLTPAInfrastructure();
        debugLTPAConfig(); //prints debug for current LTPA config
    }

    /**
     * When the configuration is modified,
     *
     * <pre>
     * 1. If file name, or expiration, or expirationDifferenceAllowed, monitorValidationKeysDir, validationKeys changed,
     * then remove the file monitor registration, reload primary and validation LTPA keys, and setup Runtime LTPA Infrastructure.
     * 2. Else if only the monitor interval changed,
     * then remove the file monitor registration and optionally create a new file monitor.
     * 3. (Implicit)Else if only the key password changed,
     * then do not remove the file monitor registration and do not reload the LTPA keys.
     * </pre>
     */
    protected void modified(Map<String, Object> props) {
        String oldKeyImportFile = primaryKeyImportFile;
        Long oldKeyTokenExpiration = keyTokenExpiration;
        Long oldMonitorInterval = monitorInterval;
        Long oldExpirationDifferenceAllowed = expirationDifferenceAllowed;
        boolean oldMonitorValidationKeysDir = monitorValidationKeysDir;
        String oldUpdateTrigger = updateTrigger;
        List<Properties> oldValidationKeys = new ArrayList<Properties>();
        oldValidationKeys.addAll(validationKeys);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "oldValidationKeys: " + maskKeysPasswords(oldValidationKeys));
        }

        loadConfig(props);

        if (isKeysConfigChanged(oldKeyImportFile, oldKeyTokenExpiration, oldExpirationDifferenceAllowed, oldMonitorValidationKeysDir, oldUpdateTrigger, oldValidationKeys)) {
            unsetFileMonitorRegistration();
            Tr.audit(tc, "LTPA_KEYS_TO_LOAD", primaryKeyImportFile);
            setupRuntimeLTPAInfrastructure();
        } else if (isMonitorIntervalChanged(oldMonitorInterval)) {
            unsetFileMonitorRegistration();
            optionallyCreateFileMonitor();
        }
        debugLTPAConfig(); //prints debug for current LTPA config
    }

    @Sensitive
    private void loadConfig(Map<String, Object> props) {
        primaryKeyImportFile = (String) props.get(CFG_KEY_IMPORT_FILE);
        SerializableProtectedString sps = (SerializableProtectedString) props.get(CFG_KEY_PASSWORD);
        primaryKeyPassword = sps == null ? null : new String(sps.getChars());
        keyTokenExpiration = (Long) props.get(CFG_KEY_TOKEN_EXPIRATION);
        monitorInterval = (Long) props.get(CFG_KEY_MONITOR_INTERVAL);
        authFilterRef = (String) props.get(KEY_AUTH_FILTER_REF);
        // expirationDifferenceAllowed is set to 3 seconds (3000ms) by default.
        // If expirationDifferenceAllowed is set to less than 0, then the two expiration values will not be compared in the LTPAToken2.decrypt() method.
        expirationDifferenceAllowed = (Long) props.get(KEY_EXP_DIFF_ALLOWED);
        monitorValidationKeysDir = (Boolean) props.get(CFG_KEY_MONITOR_VALIDATION_KEYS_DIR);
        updateTrigger = (String) props.get(CFG_KEY_UPDATE_TRIGGER);

        //get all validationKeys elements
        Map<String, List<Map<String, Object>>> validationKeysElements = Nester.nest(props, CFG_KEY_VALIDATION_KEYS);
        if (!validationKeysElements.isEmpty()) {
            isValidationKeysFileConfigured = !validationKeysElements.get(CFG_KEY_VALIDATION_KEYS).isEmpty();
        }

        resolveActualPrimaryKeysFileLocation();

        if (isValidationKeysFileConfigured) {
            configValidationKeys = getConfigValidationKeys(validationKeysElements, CFG_KEY_VALIDATION_KEYS, CFG_KEY_VALIDATION_FILE_NAME, CFG_KEY_VALIDATION_PASSWORD,
                                                           CFG_KEY_VALIDATION_VALID_UNTIL_DATE);
        } else {
            configValidationKeys = null;
        }

        if (updateTrigger.equalsIgnoreCase("disabled")) {
            if (monitorValidationKeysDir) {
                Tr.warning(tc, "LTPA_UPDATE_TRIGGER_DISABLED_AND_MONITOR_VALIDATION_KEYS_DIR_TRUE", monitorValidationKeysDir);
            }
            if (monitorInterval > 0) {
                Tr.warning(tc, "LTPA_UPDATE_TRIGGER_DISABLED_AND_MONITOR_INTERVAL_GREATER_THAN_ZERO", monitorInterval);
            }
        } else if (updateTrigger.equalsIgnoreCase("polled")) {
            if (monitorValidationKeysDir && monitorInterval <= 0) {
                Tr.warning(tc, "LTPA_UPDATE_TRIGGER_POLLED_MONITOR_VALIDATION_KEYS_DIR_TRUE_AND_MONITOR_INTERVAL_NOT_ENABLED", monitorInterval);
            } else if (!monitorValidationKeysDir) {
                nonConfigValidationKeys = null;
            }
        }

        combineValidationKeys();
    }

    /**
     *
     */
    private void debugLTPAConfig() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "primaryKeyImportFile: " + primaryKeyImportFile);
            //Tr.debug(tc, "primaryKeyPassword: " + primaryKeyPassword);
            Tr.debug(tc, "keyTokenExpiration: " + keyTokenExpiration);
            Tr.debug(tc, "monitorInterval: " + monitorInterval);
            Tr.debug(tc, "authFilterRef: " + authFilterRef);
            Tr.debug(tc, "monitorValidationKeysDir: " + monitorValidationKeysDir);
            Tr.debug(tc, "updateTrigger: " + updateTrigger);
            Tr.debug(tc, "validationKeys: " + (validationKeys == null ? "Null" : maskKeysPasswords(validationKeys)));
        }
    }

    /**
     *
     */
    private void combineValidationKeys() {
        if (validationKeys != null)
            validationKeys.clear();
        if (configValidationKeys != null) {
            validationKeys.addAll(configValidationKeys);
        }
        if (nonConfigValidationKeys != null) {
            validationKeys.addAll(nonConfigValidationKeys);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "configured ValidationKeys: " + maskKeysPasswords(configValidationKeys));
            Tr.debug(tc, "non-configured ValidationKeys: " + maskKeysPasswords(nonConfigValidationKeys));
            Tr.debug(tc, "combined ValidationKeys: " + maskKeysPasswords(validationKeys));
        }
    }

    /**
     * Input a list of properties for the validaiton keys
     * Output
     *
     * @return the same list with passwords masked for debugging.
     *         masked values: "*null*" or "*not null*"
     */
    protected List<Properties> maskKeysPasswords(@Sensitive List<Properties> originalList) {
        if (originalList == null)
            return null;

        List<Properties> maskedList = new ArrayList<Properties>();
        for (Properties props : originalList) {
            Properties tempProps = new Properties();
            tempProps.putAll(props);
            if (tempProps.getProperty(CFG_KEY_VALIDATION_PASSWORD) == null) {
                tempProps.setProperty(CFG_KEY_VALIDATION_PASSWORD, "*null*");
            } else {
                tempProps.setProperty(CFG_KEY_VALIDATION_PASSWORD, "*not null*");
            }
            maskedList.add(tempProps);
        }
        return maskedList;
    }

    /**
     * monitorValidationKeysDir set to true, we have to resolve all the validations keys file with suffix .keys in the directory except
     * the primary LTPA keys file and validation keys file that configured in the validationKeys element.
     * Use the primary LTPA keys password for these validation keys flie
     *
     **/
    @SuppressWarnings({ "static-access" })
    @Sensitive
    private List<Properties> getNonConfiguredValidationKeys() {
        List<Properties> validationKeysInDirectory = new ArrayList<Properties>();
        Iterator<File> keysFiles = this.allKeysFiles.iterator();

        if (keysFiles != null) {
            while (keysFiles.hasNext()) {
                File keyFile = keysFiles.next();

                String fileName = keyFile.getName();
                String fullFileName = null;

                if (primaryKeyImportDir != null) {
                    fullFileName = primaryKeyImportDir.concat(fileName);
                } else {
                    Tr.debug(tc, "primaryKeyImportDir is null. Validation keys will not be loaded.");
                    return validationKeysInDirectory;
                }

                // Skip the primary LTPA keys file or validationKeys file configured in the valicationKeys element
                if (primaryKeyImportFile.equals(fullFileName) || isConfiguredValidationKeys(fullFileName)) {
                    continue;
                }

                Properties properties = new Properties();
                properties.setProperty(CFG_KEY_VALIDATION_FILE_NAME, fullFileName);
                properties.setProperty(CFG_KEY_VALIDATION_PASSWORD, primaryKeyPassword);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Non-configured validationKeys file name: " + fullFileName);
                }

                validationKeysInDirectory.add(properties);
            }
        }

        return validationKeysInDirectory;
    }

    /**
     * @param fn
     * @return
     */
    private boolean isConfiguredValidationKeys(String fn) {
        if (configValidationKeys != null) {
            Iterator<Properties> configValidationKeysIterator = configValidationKeys.iterator();
            while (configValidationKeysIterator.hasNext()) {
                Properties vKeys = configValidationKeysIterator.next();
                if (vKeys.getProperty(CFG_KEY_VALIDATION_FILE_NAME).equals(fn))
                    return true;
            }
        }

        return false;
    }

    private void resolveActualPrimaryKeysFileLocation() {
        if (isInDefaultOutputLocation()) {
            WsResource keysFileInServerConfig = locationService.getServiceWithException().resolveResource(DEFAULT_CONFIG_LOCATION);
            if (keysFileInServerConfig != null && keysFileInServerConfig.exists()) {
                String expandedKeysFileInServerConfig = locationService.getServiceWithException().resolveString(DEFAULT_CONFIG_LOCATION);
                primaryKeyImportFile = expandedKeysFileInServerConfig;
            }
        }

        if (monitorValidationKeysDir || isValidationKeysFileConfigured) {
            try {
                // primaryKeyImportFile has already been resolved when the server loads the config, this includes variable and .. being resolved.
                // primaryKeyImportDir is required to be set to load any validation keys.
                primaryKeyImportDir = new File(primaryKeyImportFile).getCanonicalFile().getParent() + File.separator;
                Tr.debug(tc, "primaryKeyImportDir: " + primaryKeyImportDir);
            } catch (IOException e) {
                Tr.debug(tc, "An exception occurred in resolveActualPrimaryKeysFileLocation method", e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "primaryKeyImportDir: " + primaryKeyImportDir);
            Tr.debug(tc, "primaryKeyImportFile: " + primaryKeyImportFile);
        }
    }

    protected boolean isInDefaultOutputLocation() {
        String expandedKeysFileInServerOutput = locationService.getServiceWithException().resolveString(DEFAULT_OUTPUT_LOCATION);
        return primaryKeyImportFile.equals(expandedKeysFileInServerOutput);
    }

    /**
     * To set the LTPA infrastructure, optionally create the LTPA file monitor and create keys.
     */
    private void setupRuntimeLTPAInfrastructure() {
        optionallyCreateFileMonitor();
        //The fileMonitor onBaseline method will be called before the submitTaskToCreateLTPAKeys below.
        createTask = new LTPAKeyCreateTask(locationService.getService(), this);
        submitTaskToCreateLTPAKeys();
    }

    /**
     * Creates an LTPA file monitor when the monitor interval is greater than zero.
     */
    private void optionallyCreateFileMonitor() {
        if (updateTrigger.equalsIgnoreCase("polled")) {
            if (monitorInterval > 0 || monitorValidationKeysDir) {
                createFileMonitor();
            }
        } else if (updateTrigger.equalsIgnoreCase("mbean")) {
            createFileMonitor();
        }
    }

    /**
     * Handles the creation of the LTPA file monitor.
     */
    private void createFileMonitor() {
        try {
            ltpaFileMonitor = new LTPAFileMonitor(this);
            if (primaryKeyImportDir != null) { // monitor directory and file
                setFileMonitorRegistration(ltpaFileMonitor.monitorFiles(null,
                                                                        Arrays.asList(primaryKeyImportDir),
                                                                        Arrays.asList(primaryKeyImportFile),
                                                                        monitorInterval, updateTrigger));
            } else { // monitor only files
                if (monitorValidationKeysDir && primaryKeyImportDir == null) {
                    Tr.debug(tc, "Since primaryKeyImportDir is null, monitor the primaryKeyImportFile, and not the directory.");
                }
                setFileMonitorRegistration(ltpaFileMonitor.monitorFiles(null, null, Arrays.asList(primaryKeyImportFile), monitorInterval, updateTrigger));
            }
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
     *
     * If only the LTPA primary key is configured, keep the old behavior as the same as SecurityFileMonitor
     *
     */
    @SuppressWarnings("static-access")
    @Override
    public void performFileBasedAction(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
        Collection<File> allFiles = getAllFiles(createdFiles, modifiedFiles, deletedFiles);

        processAllKeysFiles(createdFiles, modifiedFiles, deletedFiles);

        processValidationKeys();

        if (noValidationKeys()) { // no validationKeys. Keep behavior the same as SecurityFileMonnitor
            if (deletedFiles.isEmpty() == false) {
                currentlyDeletedFiles.addAll(deletedFiles);
            }
            if (!isActionNeeded(createdFiles, modifiedFiles)) { // deleted file will not reload the primary keys
                return;
            }
        }
        Tr.audit(tc, "LTPA_KEYS_TO_LOAD", printLTPAKeys(allFiles));
        submitTaskToCreateLTPAKeys();
    }

    @Override
    public void performFileBasedAction(Collection<File> baselineFiles) {
        //load validation keys already in the monitored directory when the monitor is started
        if (!baselineFiles.isEmpty()) {
            Collection<File> emptyCollection = new HashSet<File>();
            processAllKeysFiles(baselineFiles, emptyCollection, emptyCollection);

            processValidationKeys();
        }
    }

    /**
     *
     */
    private void processValidationKeys() {
        // create, modified and deleted files with validation keys will reload all primary and validation keys.
        if (updateTrigger != null && monitorValidationKeysDir) {
            validationKeys.clear();

            nonConfigValidationKeys = getNonConfiguredValidationKeys();
            combineValidationKeys();
        }
    }

    /**
     * Action is needed if a file is modified or if it is recreated after it was deleted
     * Action is not needed for delete the file. Keep the same behavior as SecurityFileMonitor
     *
     * @param createdFiles
     * @param modifiedFiles
     * @param deletedFiles
     */
    private Boolean isActionNeeded(Collection<File> createdFiles, Collection<File> modifiedFiles) {
        boolean actionNeeded = false;

        for (File createdFile : createdFiles) {
            if (currentlyDeletedFiles.contains(createdFile)) {
                currentlyDeletedFiles.remove(createdFile);
                actionNeeded = true;
            }
        }

        if (modifiedFiles.isEmpty() == false) {
            actionNeeded = true;
        }
        return actionNeeded;
    }

    boolean noValidationKeys() {
        return (validationKeys == null || validationKeys.isEmpty());
    }

    @SuppressWarnings("static-access")
    private synchronized Collection<File> processAllKeysFiles(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
        this.allKeysFiles.addAll(createdFiles);
        this.allKeysFiles.addAll(modifiedFiles);
        this.allKeysFiles.removeAll(deletedFiles);
        return this.allKeysFiles;
    }

    /**
     * @param createdFiles
     * @param modifiedFiles
     * @param deletedFiles
     * @return
     */
    private Collection<File> getAllFiles(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
        Collection<File> allFiles = new HashSet<File>();
        if (!createdFiles.isEmpty()) {
            allFiles.addAll(createdFiles);
        }

        if (!modifiedFiles.isEmpty()) {
            allFiles.addAll(modifiedFiles);
        }

        if (!deletedFiles.isEmpty()) {
            allFiles.addAll(deletedFiles);
        }

        return allFiles;
    }

    private String printLTPAKeys(Collection<File> files) {

        StringBuffer strBuff = new StringBuffer();
        strBuff.append("(");
        for (Object file : files) {
            strBuff.append(file);
            strBuff.append(", ");
        }
        int currentIndex = strBuff.lastIndexOf(",");
        strBuff.delete(currentIndex, currentIndex + 2);
        strBuff.append(")");

        return strBuff.toString();
    }

    private void submitTaskToCreateLTPAKeys() {
        CheckpointPhase.onRestore(() -> executorService.getService().execute(createTask));
    }

    /**
     * The keys config is changed if the file, expiration, expirationDifferenceAllowed, moitorInterval, monitorValidationKeysDir, updateTrigger or validationKeys configured were
     * modified.
     * Changing the password by itself must not be considered a config change that should trigger a keys reload.
     *
     * @param oldKeyImportFile
     * @param oldKeyTokenExpiration
     * @param oldExpirationDifferenceAllowed
     * @param oldMonitorInterval
     * @param oldMonitorValidationKeysDir
     * @param oldUpdateTrigger
     * @param oldValidationKeys
     */
    private boolean isKeysConfigChanged(String oldKeyImportFile, Long oldKeyTokenExpiration, Long oldExpirationDifferenceAllowed, boolean oldMonitorValidationKeysDir,
                                        String oldUpdateTrigger,
                                        @Sensitive List<Properties> oldValidationKeys) {
        return ((oldKeyImportFile.equals(primaryKeyImportFile) == false)
                || (oldKeyTokenExpiration != keyTokenExpiration)
                || (oldExpirationDifferenceAllowed != expirationDifferenceAllowed)
                || (oldMonitorValidationKeysDir != monitorValidationKeysDir)
                || (oldUpdateTrigger != updateTrigger)
                || (isValidationKeysConfigured(oldValidationKeys)));
    }

    /**
     * validationKeys can be configured by validationKeys element and/or
     * enable monitorValidationKeysDir and drop the validationKeys in the directory
     *
     * @param oldValidationKeys
     * @return
     */
    private boolean isValidationKeysConfigured(@Sensitive List<Properties> oldValidationKeys) {
        if ((oldValidationKeys == null || oldValidationKeys.isEmpty()) && (validationKeys == null || validationKeys.isEmpty()))
            return false;
        else
            return true;

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
    public String getPrimaryKeyFile() {
        return primaryKeyImportFile;
    }

    /** {@inheritDoc} */
    @Sensitive
    @Override
    public String getPrimaryKeyPassword() {
        return primaryKeyPassword;
    }

    /** {@inheritDoc} */
    @Override
    public long getTokenExpiration() {
        return keyTokenExpiration;
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthFilterRef() {
        return authFilterRef;
    }

    /** {@inheritDoc} */
    @Override
    public long getExpirationDifferenceAllowed() {
        return expirationDifferenceAllowed;
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

    /** {@inheritDoc} */
    @Override
    public long getMonitorInterval() {
        return monitorInterval;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getMonitorValidationKeysDir() {
        return monitorValidationKeysDir;
    }

    /** {@inheritDoc} */
    @Override
    public String getUpdateTrigger() {
        return updateTrigger;
    }

    /** {@inheritDoc} */
    @Sensitive
    @Override
    public List<Properties> getValidationKeys() {
        return validationKeys;
    }

    /*
     *
     */
    @Sensitive
    public List<Properties> getConfigValidationKeys(Map<String, List<Map<String, Object>>> listOfNestedElements, String elementName, String... attrKeys) {
        List<Properties> listOfValidationKeysProps = new ArrayList<Properties>();
        List<Map<String, Object>> listOfElementMaps = listOfNestedElements.get(elementName);
        if (listOfElementMaps != null && !listOfElementMaps.isEmpty()) {
            for (Map<String, Object> elementProps : listOfElementMaps) {
                Properties properties = getValidationKeysProps(elementProps, elementName, attrKeys);
                if (properties != null && !properties.isEmpty()) {
                    listOfValidationKeysProps.add(properties);
                }
            }
        }
        return listOfValidationKeysProps;
    }

    /**
     * Get properties from the given element and/or it's subElements.
     * Ignore system generated props, add the user props to the given Properties object
     *
     * @param configProps properties from the configuration
     * @param elementName the element being processed
     * @param attrKeys    get attributes
     */
    @Sensitive
    private Properties getValidationKeysProps(Map<String, Object> configProps, String elementName, String... attrKeys) {
        Properties properties = new Properties();
        if (primaryKeyImportDir == null) {
            Tr.debug(tc, "primaryKeyImportDir is null. Validation keys will not be loaded.");
            return properties;
        }

        for (String attrKey : attrKeys) {
            String value = null;

            if (attrKey.equals(CFG_KEY_VALIDATION_PASSWORD)) {
                SerializableProtectedString sps = (SerializableProtectedString) configProps.get(CFG_KEY_VALIDATION_PASSWORD);
                value = sps == null ? null : new String(sps.getChars());
            } else {
                value = (String) configProps.get(attrKey);
            }

            if (value != null && value.length() > 0) {
                value = value.trim();
                if (attrKey.equals(CFG_KEY_VALIDATION_FILE_NAME)) {
                    value = primaryKeyImportDir.concat(value);
                }
                properties.put(attrKey, value);
            }
        }

        //if validationKeys element is configured, then it must have the filename and password
        if (properties.isEmpty() || properties.get(CFG_KEY_VALIDATION_FILE_NAME) == null || properties.get(CFG_KEY_VALIDATION_PASSWORD) == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.error(tc, "LTPA_VALIDATION_KEYS_MISSING_ATTR", elementName, printAttrKeys(attrKeys));
            }
            return null;
        } else {
            if (isValidUntilDateExpired(configProps)) {
                return null; //it can not be used so skip this validationKeys.
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Configured validationKeys file name: " + properties.get(CFG_KEY_VALIDATION_FILE_NAME));
            }
            return properties;
        }
    }

    /**
     * validUntilDate attribute is an optional and have no default value.
     * This function checks if the validUntilDate0dt has already passed the current time.
     * If so, then they key is expired, and will return true with a warning message.
     * Otherwise, the key is valid and will return false.
     * If the validUntilDateOdt is null, then the key is forever valid and will return false.
     *
     * @param configProps
     * @return
     */
    private boolean isValidUntilDateExpired(Map<String, Object> configProps) {
        String validUntilDate = (String) configProps.get(CFG_KEY_VALIDATION_VALID_UNTIL_DATE);
        if (validUntilDate == null) { // If not specified, then it is forever valid.
            return false;
        }
        OffsetDateTime noUserAfterDateOdt = null;
        try {
            noUserAfterDateOdt = OffsetDateTime.parse(validUntilDate);
        } catch (Exception e) {
            Tr.error(tc, "LTPA_VALIDATION_KEYS_VALID_UNTIL_DATE_INVALID_FORMAT", validUntilDate, configProps.get(CFG_KEY_VALIDATION_FILE_NAME));
            return true;
        }

        ZoneOffset zone = noUserAfterDateOdt.getOffset();

        OffsetDateTime currentDateTime = OffsetDateTime.now(zone);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "validUntilDate: " + noUserAfterDateOdt);
            Tr.debug(tc, "current date: " + currentDateTime);
        }

        if (noUserAfterDateOdt.isBefore(currentDateTime)) {
            Tr.warning(tc, "LTPA_VALIDATION_KEYS_VALID_UNTIL_DATE_IS_IN_THE_PAST", validUntilDate, configProps.get(CFG_KEY_VALIDATION_FILE_NAME));
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param attrKeys
     * @return
     */
    private String printAttrKeys(String... attrKeys) {
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("(");
        for (String attrKey : attrKeys) {
            strBuff.append(attrKey);
            strBuff.append(", ");
        }
        int currentIndex = strBuff.lastIndexOf(",");
        strBuff.delete(currentIndex, currentIndex + 2);
        strBuff.append(")");
        return strBuff.toString();
    }
}

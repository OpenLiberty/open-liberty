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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import com.ibm.ws.security.filemonitor.SecurityFileMonitor;
import com.ibm.ws.security.token.ltpa.LTPAConfiguration;
import com.ibm.ws.security.token.ltpa.LTPAKeyInfoManager;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.security.ltpa.TokenFactory;

import io.openliberty.checkpoint.spi.CheckpointHook;
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
    private SecurityFileMonitor ltpaFileMonitor;
    private ServiceRegistration<FileMonitor> ltpaFileMonitorRegistration;
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();;
    private final WriteLock writeLock = reentrantReadWriteLock.writeLock();
    private final ReadLock readLock = reentrantReadWriteLock.readLock();
    private String authFilterRef;
    private long expirationDifferenceAllowed;
    private boolean monitorDirectory;
    private final List<Properties> validationKeys = new ArrayList<Properties>();
    private List<Properties> configValidationKeys = null;
    private List<Properties> unConfigValidationKeys = null;

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
    //TODO: add sensitive to all password later.

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
        monitorDirectory = (Boolean) props.get(CFG_KEY_MONITOR_DIRECTORY);

        resolveActualPrimaryKeysFileLocation();

        //get all validationKeys elements
        Map<String, List<Map<String, Object>>> validationKeysElements = Nester.nest(props, CFG_KEY_VALIDATION_KEYS);
        if (!validationKeysElements.isEmpty()) {
            configValidationKeys = getConfigValidationKeys(validationKeysElements, CFG_KEY_VALIDATION_KEYS, CFG_KEY_VALIDATION_FILE_NAME, CFG_KEY_VALIDATION_PASSWORD,
                                                           CFG_KEY_VALIDATION_NOT_USE_AFTER_DATE);
        }

        if (monitorDirectory) {
            unConfigValidationKeys = getUnConfigValidationKeys();
        }

        combineValidationKeys();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "primaryKeyImportFile: " + primaryKeyImportFile);
            Tr.debug(tc, "primaryKeyPassword: " + primaryKeyPassword);
            Tr.debug(tc, "keyTokenExpiration: " + keyTokenExpiration);
            Tr.debug(tc, "monitorInterval: " + monitorInterval);
            Tr.debug(tc, "authFilterRef: " + authFilterRef);
            Tr.debug(tc, "monitorDirectory: " + monitorDirectory);
            Tr.debug(tc, "validationKeys: " + (validationKeys == null ? "Null" : validationKeys.toString()));
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
        if (unConfigValidationKeys != null) {
            validationKeys.addAll(unConfigValidationKeys);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "validationKeys", validationKeys);
        }
    }

    /**
     * Monitor directory set to true, we have to resolve all the validations key file with suffix .keys in this directory except
     * the primary LTPA keys file and validation keys file that specified in the validationKeys element.
     * and use the primary LTPA keys password.
     *
     **/
    @SuppressWarnings("unlikely-arg-type")
    private List<Properties> getUnConfigValidationKeys() {
        List<Properties> validationKeysInDirectory = new ArrayList<Properties>();
        WsResource keysFileInDirectory = locationService.getServiceWithException().resolveResource(primaryKeyImportDir);
        Iterator<String> keysFileNames = keysFileInDirectory.getChildren(".*\\.keys");

        while (keysFileNames.hasNext()) {
            Properties properties = new Properties();
            WsResource kfs = keysFileInDirectory.getChild(keysFileNames.next());
            String fn = kfs.getName();
            fn = primaryKeyImportDir.concat(fn);

            // skip the primary LTPA keys file or validationKeys file configured in the valicationKeys element
            if (primaryKeyImportFile.equals(kfs.getName()) || isConfiguredValidationKeys(fn)) {
                continue;
            }

            properties.setProperty(CFG_KEY_VALIDATION_FILE_NAME, fn);

            properties.setProperty(CFG_KEY_VALIDATION_PASSWORD, primaryKeyPassword);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unconfigured validationKeys file name: " + fn);
            }

            validationKeysInDirectory.add(properties);
        }

        return validationKeysInDirectory;
    }

    /**
     * @param fn
     * @return
     */
    private boolean isConfiguredValidationKeys(String fn) {
        Iterator<Properties> configValidationKeysIterator = configValidationKeys.iterator();
        while (configValidationKeysIterator.hasNext()) {
            Properties vKeys = configValidationKeysIterator.next();
            if (vKeys.getProperty(CFG_KEY_VALIDATION_FILE_NAME).equals(fn))
                return true;
        }

        return false;
    }

    private void resolveActualPrimaryKeysFileLocation() {
        WsResource keysFileInServerConfig = locationService.getServiceWithException().resolveResource(primaryKeyImportFile);
        primaryKeyImportFile = keysFileInServerConfig.getName();
        String dir = keysFileInServerConfig.getParent().toRepositoryPath();
        primaryKeyImportDir = locationService.getServiceWithException().resolveString(dir);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "primaryKeyImportDir: " + primaryKeyImportDir);
            Tr.debug(tc, "primaryKeyImportFile: " + primaryKeyImportFile);
        }
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
        if (monitorInterval > 0 || monitorDirectory) {
            createFileMonitor();
        }
    }

    /**
     * Handles the creation of the LTPA file monitor.
     */
    private void createFileMonitor() {
        try {
            ltpaFileMonitor = new SecurityFileMonitor(this);
            if (monitorDirectory) { // monitor directory and file
                setFileMonitorRegistration(ltpaFileMonitor.monitorFiles(Arrays.asList(primaryKeyImportDir), Arrays.asList(primaryKeyImportFile), monitorInterval));
            } else { // monitor only files
                setFileMonitorRegistration(ltpaFileMonitor.monitorFiles(null, Arrays.asList(primaryKeyImportFile), monitorInterval));
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
     */
    @Override
    public void performFileBasedAction(Collection<File> files) {
        //TODO:
        Tr.audit(tc, "LTPA_KEYS_TO_LOAD", printLTPAKeys(files));

        if (monitorDirectory) {
            validationKeys.clear();

            if (configValidationKeys != null || !configValidationKeys.isEmpty())
                validationKeys.addAll(configValidationKeys);
            unConfigValidationKeys = getUnConfigValidationKeys();
            combineValidationKeys();
        }

        submitTaskToCreateLTPAKeys();
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
        CheckpointPhase checkpointPhase = CheckpointPhase.getPhase();
        if (checkpointPhase != CheckpointPhase.INACTIVE) {
            // conditionally create hook if checkpoint phase is active
            CheckpointHook hook = new CheckpointHook() {
                @Override
                public void restore() {
                    executorService.getService().execute(createTask);
                }
            };
            if (checkpointPhase.addMultiThreadedHook(hook)) {
                // will run createTask later, upon restore
                return;
            }
        }
        // run the create task now, not in a checkpoint
        executorService.getService().execute(createTask);
    }

    /**
     * When the configuration is modified,
     *
     * <pre>
     * 1. If file name, or expiration, or expirationDifferenceAllowed changed,
     * then remove the file monitor registration, reload LTPA keys, and setup Runtime LTPA Infrastructure.
     * 2. Else if only the monitor interval changed,
     * then remove the file monitor registration and optionally create a new file monitor.
     * 3. (Implicit)Else if only the key password changed,
     * then do not remove the file monitor registration and do not reload the LTPA keys.
     * </pre>
     */
    //TODO: handle LTPA validation keys change - filename, password, notUseAfterDate
    protected void modified(Map<String, Object> props) {
        String oldKeyImportFile = primaryKeyImportFile;
        Long oldKeyTokenExpiration = keyTokenExpiration;
        Long oldMonitorInterval = monitorInterval;
        Long oldExpirationDifferenceAllowed = expirationDifferenceAllowed;
        boolean oldMonitorDirectory = monitorDirectory;
        List<Properties> oldValidationKeys = new ArrayList<Properties>();
        oldValidationKeys.addAll(validationKeys);

//        List<Properties> oldValidationKeys = validationKeys;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "oldValidationKeys: " + oldValidationKeys);
        }

        loadConfig(props);

        if (isKeysConfigChanged(oldKeyImportFile, oldKeyTokenExpiration, oldExpirationDifferenceAllowed, oldMonitorDirectory, oldValidationKeys)) {
            unsetFileMonitorRegistration();
            Tr.audit(tc, "LTPA_KEYS_TO_LOAD", primaryKeyImportFile);
            setupRuntimeLTPAInfrastructure();
        } else if (isMonitorIntervalChanged(oldMonitorInterval)) {
            unsetFileMonitorRegistration();
            optionallyCreateFileMonitor();
        }
    }

    /**
     * The keys config is changed if the file, expiration, expirationDifferenceAllowed or validationKeys configured were modified.
     * Changing the password by itself must not be considered a config change that should trigger a keys reload.
     *
     * @param oldValidationKeys
     * @param oldMonitorDirectory
     */
    private boolean isKeysConfigChanged(String oldKeyImportFile, Long oldKeyTokenExpiration, Long oldExpirationDifferenceAllowed, boolean oldMonitorDirectory,
                                        List<Properties> oldValidationKeys) {
        return ((oldKeyImportFile.equals(primaryKeyImportFile) == false)
                || (oldKeyTokenExpiration != keyTokenExpiration)
                || (oldExpirationDifferenceAllowed != expirationDifferenceAllowed)
                || (oldMonitorDirectory != monitorDirectory)
                || (isValidationKeysConfigured(oldValidationKeys)));
    }

    /**
     * If we have any validationKeys(configured or unConfigured), we want to reload all keys
     *
     * @param oldValidationKeys
     * @return
     */
    private boolean isValidationKeysConfigured(List<Properties> oldValidationKeys) {
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

    @Override
    public boolean getMonitorDirectory() {
        return monitorDirectory;
    }

    @Override
    public List<Properties> getValidationKeys() {
        return validationKeys;
    }

    /*
     *
     */
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
    private Properties getValidationKeysProps(Map<String, Object> configProps, String elementName, String... attrKeys) {
        Properties properties = new Properties();
        for (String attrKey : attrKeys) {
            String value = null;

            if (attrKey.equals(CFG_KEY_VALIDATION_PASSWORD)) {
                SerializableProtectedString sps = (SerializableProtectedString) configProps.get(CFG_KEY_VALIDATION_PASSWORD);
                value = sps == null ? null : new String(sps.getChars());
            } else {
                value = (String) configProps.get(attrKey);
            }

            if (value != null && value.length() > 0) {
                value = (String) getValue(value);
                if (attrKey.equals(CFG_KEY_VALIDATION_FILE_NAME)) {
                    value = primaryKeyImportDir.concat(value);
                }
                properties.put(attrKey, value);
            }
        }

        //if validationKeys element is specified, then it must have the filename and password
        if (properties.isEmpty() || properties.get(CFG_KEY_VALIDATION_FILE_NAME) == null || properties.get(CFG_KEY_VALIDATION_PASSWORD) == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.error(tc, "LTPA_VALIDATION_KEYS_MISSING_ATTR", elementName, printAttrKeys(attrKeys));
            }
            return null;
        } else {
            if (isNotUseAfterDate(configProps)) { // TODO: do we need to check it now or late? we will have to check it when we use it
                return null; //it can not be used so skip this validationKeys.
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Configured validationKeys file name: " + properties.get(CFG_KEY_VALIDATION_FILE_NAME));
            }
            return properties;
        }
    }

    /**
     * notUseAfterDate attribute is an optional and have no default value.
     * if notUseAfterDate is null, then it does not expired
     * if notUseAfterDate is greater then current date and time, then it can not be used.
     *
     * @param configProps
     * @return
     */
    private boolean isNotUseAfterDate(Map<String, Object> configProps) {
        String notUseAfterDate = (String) configProps.get(CFG_KEY_VALIDATION_NOT_USE_AFTER_DATE);
        boolean result = false;
        if (notUseAfterDate == null) { // Not specify so it is good to use
            return result;
        }
        OffsetDateTime noUserAfterDateOdt = null;
        try {
            noUserAfterDateOdt = OffsetDateTime.parse(notUseAfterDate);
        } catch (Exception e) {
            Tr.error(tc, "LTPA_VALIDATION_KEYS_NOT_USE_AFTER_DATE_INVALID_FORMAT", notUseAfterDate, configProps.get(CFG_KEY_VALIDATION_FILE_NAME));
            return true;
        }

        ZoneOffset zone = noUserAfterDateOdt.getOffset();

        OffsetDateTime currentDateTime = OffsetDateTime.now(zone);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "notUseAfterDate: " + noUserAfterDateOdt);
            Tr.debug(tc, "current date: " + currentDateTime);
        }

        if (currentDateTime.isAfter(noUserAfterDateOdt)) {
            Tr.warning(tc, "LTPA_VALIDATION_KEYS_PASSED_NOT_USE_AFTER_DATE", notUseAfterDate, configProps.get(CFG_KEY_VALIDATION_FILE_NAME));
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

    private Object getValue(Object value) {
        if (value instanceof String) {
            return ((String) value).trim();
        }
        return value;
    }

}

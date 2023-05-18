/*******************************************************************************
 * Copyright (c) 2009, 2023 IBM Corporation and others.
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
package com.ibm.ws.transaction.services;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.condition.Condition;

import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.RuntimeMetaDataProvider;
import com.ibm.tx.jta.config.DefaultConfigurationProvider;
import com.ibm.tx.jta.embeddable.TransactionSettingsProvider;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.launch.service.ForcedServerStop;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.resource.ResourceFactory;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

public class JTMConfigurationProvider extends DefaultConfigurationProvider implements ConfigurationProvider {

    private static final TraceComponent tc = Tr.register(JTMConfigurationProvider.class);

    private WsLocationAdmin locationService;

    private RuntimeMetaDataProvider _runtimeMetaDataProvider;

    private volatile Map<String, Object> _props;
    ComponentContext _cc;
    private static String logDir;
    private static final String defaultLogDir = "$(server.output.dir)/tranlog";
    private boolean activateHasBeenCalled; // Used for eyecatcher in trace for startup ordering.
    private boolean _dataSourceFactorySet;
    private static boolean _frameworkShutting = false;

    private final ConcurrentServiceReferenceSet<TransactionSettingsProvider> _transactionSettingsProviders = new ConcurrentServiceReferenceSet<TransactionSettingsProvider>("transactionSettingsProvider");
    /**
     * Active instance. May be null between deactivate and activate.
     */
    private static final AtomicServiceReference<ResourceFactory> dataSourceFactoryRef = new AtomicServiceReference<ResourceFactory>("dataSourceFactory");

    private static final int HEURISTIC_RETRY_INTERVAL_DEFAULT = 60;

    /**
     * Flag whether we are using a Transaction Log stored in the filesystem or a Transaction Log
     * stored in an RDBMS.
     */
    private static boolean _isSQLRecoveryLog;
    private ResourceFactory _theDataSourceFactory;

    private String _recoveryIdentity;
    private String _recoveryGroup;
    private TransactionManagerService tmsRef;
    private byte[] _applId;

    private boolean _setRetriableSqlcodes = false;
    private boolean _setNonRetriableSqlcodes = false;
    List<Integer> retriableSqlCodeList;
    List<Integer> nonRetriableSqlCodeList;

    private static Map<String, Object> _cpPropsOverrides = new HashMap<String, Object>() {
        {
            put("recoverOnStartup", false);
            put("waitForRecovery", true);
        }
    };

    boolean _checkpointRestore = CheckpointPhase.getPhase() != CheckpointPhase.INACTIVE && CheckpointPhase.getPhase().restored();
    boolean _checkpoint = CheckpointPhase.getPhase() != CheckpointPhase.INACTIVE && !CheckpointPhase.getPhase().restored();
    

    private boolean startupEnabled() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "_cc: " + _cc + ", tmsRef: " + tmsRef
                         + ", CP phase: " + CheckpointPhase.getPhase()
                         + ", CP restored: " + CheckpointPhase.getPhase().restored()
                         + ", CP running condition: " + ((_runningCondition == null) ? "null" : _runningCondition.getProperty(Condition.CONDITION_ID) + " "
                                                                                               + _runningCondition.getProperty(CheckpointPhase.CHECKPOINT_PROPERTY)));
        if (_cc == null || tmsRef == null)
            return false; // not activated or tms is unavailable
        if (CheckpointPhase.getPhase() == CheckpointPhase.INACTIVE)
            return true; // normal server
        if (!CheckpointPhase.getPhase().restored())
            return true; // checkpoint server
        if (_runningCondition == null)
            return true; // restore server, before config updates
        return true; // restore, after config updates
    }

    private volatile ServiceReference<Condition> _runningCondition = null;

    public void setRunningCondition(ServiceReference<Condition> runningCondition) {
        _runningCondition = runningCondition;

        if (startupEnabled()) {
            // TODO ??? RESET OVERRIDEN CONFIG PROPS TO ENABLE RECOVERY
            tmsRef.doStartup(this, _isSQLRecoveryLog);
        }
    }

    public JTMConfigurationProvider() {
    }

    /*
     * Called by DS to activate service
     */
    protected void activate(ComponentContext cc) {
        _runtimeMetaDataProvider = new LibertyRuntimeMetaDataProvider(this);
        activateHasBeenCalled = true;
        _transactionSettingsProviders.activate(cc);
        _cc = cc;

        if (_checkpointRestore) {
            // reset log dir if reactivated during restore
            // TODO WHY IS logDir STATIC? SAME FOR _isSQLRecoveryLog. MAKES NO SENSE.
            logDir = null;
        }

        // Irrespective of the logtype we need to get the properties

        // Make a copy of the properties and store it in a unmodifiable Map.
        // The properties are queried on each transaction so with using a
        // Dictionary (Hashtable), it becomes a bottleneck to read a property
        // with each thread getting a Hashtable lock to do a get operation.
        Dictionary<String, Object> props = cc.getProperties();
        Map<String, Object> properties = new HashMap<>();
        Enumeration<String> keys = props.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (/* CheckpointPhase.getPhase() != CheckpointPhase.INACTIVE && */ _cpPropsOverrides.containsKey(key)) {
                // override recovery behavior during restore until config updates complete
                properties.put(key, _cpPropsOverrides.get(key));
            } else {
            properties.put(key, props.get(key));
            }
        }
        properties = Collections.unmodifiableMap(properties);
        synchronized (this) {
            _props = properties;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "activate  properties set to " + _props);

        // There is additional work to do if we are storing transaction log in an RDBMS. The key
        // determinant that we are using an RDBMS is the specification of the dataSourceRef
        // attribute of the transaction stanza in the server.xml. So start by checking this
        // attribute. If it is present, set the _isSQLRecoveryLog flag and set the logDir
        // to "custom" <- this will allow compatibility with tWAS code.
        //
        // Drive the getTransactionLogDirectory() method if we're working against the filesys.
        checkDataSourceRef();

        if (_isSQLRecoveryLog) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "activate  working with Tran Log in an RDBMS");

            ServiceReference<ResourceFactory> serviceRef = dataSourceFactoryRef.getReference();

            if (tc.isDebugEnabled())
                Tr.debug(tc, "pre-activate  datasourceFactory ref " + dataSourceFactoryRef +
                             ", underlying reference: " + serviceRef);
            dataSourceFactoryRef.activate(_cc);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "post-activate  datasourceFactory ref " + dataSourceFactoryRef);

            //  If we already have a dataSourceFactory then we can startup (and drive recovery) now.
            if (tc.isDebugEnabled())
                Tr.debug(tc, "retrieved datasourceFactory service ref " + serviceRef);
            if (serviceRef != null) {
                // The DataSource is available, which means that we are able to drive recovery
                // processing. This is driven through the reference to the TransactionManagerService,
                // assuming that it is available
                if (startupEnabled())
                    tmsRef.doStartup(this, _isSQLRecoveryLog);
            }
        } else {
            getTransactionLogDirectory();
            if (startupEnabled())
                tmsRef.doStartup(this, _isSQLRecoveryLog);
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "activate  retrieved datasourceFactory is " + _theDataSourceFactory);

        addTransactionLogDirCheckpointHook();

        // Configuration has changed, may need to reset the lists of sqlcodes
        _setRetriableSqlcodes = false;
        _setNonRetriableSqlcodes = false;
    }

    protected void deactivate(int reason, ComponentContext cc, Map<String, Object> properties) {
        _transactionSettingsProviders.deactivate(cc);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "deactivate");
    }

    /*
     * Called by DS to modify service config properties
     */
    protected void modified(Map<String, Object> newProperties) {
        Map<String, Object> newProps = Collections.unmodifiableMap(new HashMap<>(newProperties));

        synchronized (this) {
            _props = newProps;
        }

        // Configuration has changed, may need to reset the lists of sqlcodes
        _setRetriableSqlcodes = false;
        _setNonRetriableSqlcodes = false;
    }

    /*
     * Called by DS to inject location service ref
     */
    protected synchronized void setLocationService(WsLocationAdmin locSvc) {
        this.locationService = locSvc;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setLocationService, locSvc " + locSvc);
    }

    /*
     * Called by DS to clear location service ref
     */
    protected synchronized void unsetLocationService(WsLocationAdmin locSvc) {
        if (locSvc == this.locationService) {
            this.locationService = null;
        }
    }

    /*
     * Called by DS to inject DataSourceFactory reference from the com.ibm.ws.jdbc component
     */
    protected void setDataSourceFactory(ServiceReference<ResourceFactory> ref) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "pre-setReference  datasourceFactory ref " + dataSourceFactoryRef);
        dataSourceFactoryRef.setReference(ref);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "post-setReference  datasourceFactory ref " + dataSourceFactoryRef);

        // Set the flag that says that this method has been called
        _dataSourceFactorySet = true;
        if (!activateHasBeenCalled)
            if (tc.isDebugEnabled())
                Tr.debug(tc, "setDataSourceFactory has been called before activate");

        // If the JTMConfigurationProvider has been activated, we can proceed to set
        // the DataSourceFactory and initiate recovery
        if (startupEnabled()) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "setDataSourceFactory and activate have been called, initiate recovery");
            tmsRef.doStartup(this, _isSQLRecoveryLog);
        }
    }

    /*
     * Called by DS to dereference DataSourceFactory
     */
    protected void unsetDataSourceFactory(ServiceReference<ResourceFactory> ref) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "unsetDataSourceFactory, ref " + ref);
        // The non-transactional DataSource is about to go away. We need to shutdown the
        // recovery log while we still have a database connection.
        if (tmsRef != null)
            tmsRef.doShutdown(_isSQLRecoveryLog);
        dataSourceFactoryRef.unsetReference(ref);
    }

    // methods to handle dependency injection in osgi environment
    public ConcurrentServiceReferenceSet<TransactionSettingsProvider> getTransactionSettingsProviders() {
        return _transactionSettingsProviders;
    }

    protected void setTransactionSettingsProvider(ServiceReference<TransactionSettingsProvider> ref) {
        _transactionSettingsProviders.addReference(ref);
    }

    protected void unsetTransactionSettingsProvider(ServiceReference<TransactionSettingsProvider> ref) {
        _transactionSettingsProviders.removeReference(ref);
    }

    @Override
    public int getClientInactivityTimeout() {
        // return Integer.valueOf(_props.get("client.inactivity.timeout"));
        Number num = (Number) _props.get("clientInactivityTimeout");
        return num.intValue();
    }

    @Override
    public int getHeuristicRetryInterval() {
        int interval = ((Number) _props.get("heuristicRetryInterval")).intValue();
        if (interval == HEURISTIC_RETRY_INTERVAL_DEFAULT) {
            // We got the default for heuristicRetryInterval but maybe
            // heuristicRetryWait was set like in the olden days
            int wait = ((Number) _props.get("heuristicRetryWait")).intValue();
            if (wait != HEURISTIC_RETRY_INTERVAL_DEFAULT) {
                // heuristicRetryWait was set
                interval = wait;
            }
        }

        return interval;
    }

    @Override
    public int getHeuristicRetryLimit() {
        Number num = (Number) _props.get("heuristicRetryLimit");
        return num.intValue();
    }

    @Override
    public int getMaximumTransactionTimeout() {
        //return ((Integer) _props.get("propogatedOrBMTTranLifetimeTimeout")).intValue();
        Number num = (Number) _props.get("propogatedOrBMTTranLifetimeTimeout");
        return num.intValue();
    }

    @Override
    public int getTotalTransactionLifetimeTimeout() {
        //return ((Integer) _props.get("totalTranLifetimeTimeout")).intValue();
        Number num = (Number) _props.get("totalTranLifetimeTimeout");
        return num.intValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.jta.config.DefaultConfigurationProvider#getTraceLevel()
     *
     * Use Tr configuration for 'transaction' group.
     */
    @Override
    public Level getTraceLevel() {
        return tc.getLoggerLevel();
    }

    @Override
    public String getTransactionLogDirectory() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getTransactionLogDirectory working with " + logDir);

        if (logDir == null) {
            logDir = parseTransactionLogDirectory();
        }
        return logDir;
    }

    @Override
    public String getLeaseCheckStrategy() {
        return (String) _props.get("leaseCheckStrategy");
    }

    @Override
    public int getLeaseCheckInterval() {
        Number num = (Number) _props.get("leaseCheckInterval");
        return num.intValue();
    }

    @Override
    public int getLeaseLength() {
        Number num = (Number) _props.get("leaseLength");
        return num.intValue();
    }

    @Override
    public int getLeaseRenewalThreshold() {
        Number num = (Number) _props.get("leaseRenewalThreshold");
        return num.intValue();
    }

    @Override
    public String getServerName() {
        String serverName = "";
        synchronized (this) {
            if (locationService != null)
                serverName = locationService.getServerName();
        }
        return serverName;
    }

    @Override
    public String getHeuristicCompletionDirectionAsString() {
        return (String) _props.get("lpsHeuristicCompletion");
    }

    @Override
    public int getHeuristicCompletionDirection() {
        final String hcd = getHeuristicCompletionDirectionAsString();

        if ("COMMIT".equalsIgnoreCase(hcd)) {
            return HEURISTIC_COMPLETION_DIRECTION_COMMIT;
        }

        if ("MANUAL".equalsIgnoreCase(hcd)) {
            return HEURISTIC_COMPLETION_DIRECTION_MANUAL;
        }

        return HEURISTIC_COMPLETION_DIRECTION_ROLLBACK;
    }

    @Override
    public int getTransactionLogSize() {
        return ((Integer) _props.get("transactionLogSize")).intValue();
    }

    @Override
    public int getDefaultMaximumShutdownDelay() {
        //return ((Integer) _props.get("defaultMaxShutdownDelay")).intValue();
        Number num = (Number) _props.get("defaultMaxShutdownDelay");
        return num.intValue();
    }

    @Override
    public boolean isLoggingForHeuristicReportingEnabled() {
        return (Boolean) _props.get("enableLoggingForHeuristicReporting");
    }

    @Override
    public boolean isAcceptHeuristicHazard() {
        return (Boolean) _props.get("acceptHeuristicHazard");
    }

    @Override
    public boolean isRecoverOnStartup() {
        Boolean isRoS = (Boolean) _props.get("recoverOnStartup");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "isRecoverOnStartup set to " + isRoS);
        return isRoS;
    }

    @Override
    public boolean isShutdownOnLogFailure() {
        Boolean isSoLF = (Boolean) _props.get("shutdownOnLogFailure");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "isShutdownOnLogFailure set to " + isSoLF);
        return isSoLF;
    }

    @Override
    public boolean isOnePCOptimization() {
        Boolean is1PC = (Boolean) _props.get("OnePCOptimization");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "OnePCOptimization set to " + is1PC);
        return is1PC;
    }

    @Override
    public boolean isForcePrepare() {
        Boolean forcePrepare = (Boolean) _props.get("forcePrepare");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "forcePrepare set to {0}", forcePrepare);
        return forcePrepare;
    }

    @Override
    public boolean isWaitForRecovery() {
        Boolean isWfR = (Boolean) _props.get("waitForRecovery");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "isWaitForRecovery set to " + isWfR);
        return isWfR;
    }

    @Override
    public ResourceFactory getResourceFactory() {

//WAS THIS        _theDataSourceFactory = dataSourceFactoryRef.getService();
        try {
            _theDataSourceFactory = dataSourceFactoryRef.getServiceWithException();
        } catch (Exception ex) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "getResourceFactory returned exc - " + ex);
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "getResourceFactory has factory " + _theDataSourceFactory);

        return _theDataSourceFactory;
    }

    @Override
    public RuntimeMetaDataProvider getRuntimeMetaDataProvider() {
        return _runtimeMetaDataProvider;
    }

    @Override
    public String getRecoveryIdentity() {

        _recoveryIdentity = (String) _props.get("recoveryIdentity");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getRecoveryIdentity " + _recoveryIdentity);
        return _recoveryIdentity;
    }

    @Override
    public String getRecoveryGroup() {

        _recoveryGroup = (String) _props.get("recoveryGroup");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getRecoveryGroup " + _recoveryGroup);
        return _recoveryGroup;
    }

    /**
     * The setTMS method call is used to alert the JTMConfigurationProvider to the presence of a
     * TransactionManagerService.
     *
     * @param tms
     */
    public void setTMS(TransactionManagerService tms) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setTMS " + tms);
        tmsRef = tms;
    }

    /**
     * The setTMRecoveryService method call is used to alert the JTMConfigurationProvider to the presence of a
     * TMRecoveryService. This Service was introduced under issue #5119 to break a potential circular reference in
     * DS as the TransactionManagerService and jdbc's DataSourceService are mutually dependent.
     *
     * @param tmrec
     */
    public void setTMRecoveryService(TMRecoveryService tmrec) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setTMRecoveryService " + tmrec);
        if (startupEnabled()) {
            if (!_isSQLRecoveryLog) {
                tmsRef.doStartup(this, _isSQLRecoveryLog);
            } else {
                // If the JTMConfigurationProvider has been activated, and if the DataSourceFactory
                // has been provided, we can initiate recovery
                ServiceReference<ResourceFactory> serviceRef = dataSourceFactoryRef.getReference();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "retrieved datasourceFactory service ref " + serviceRef);
                if (serviceRef != null) {
                    tmsRef.doStartup(this, _isSQLRecoveryLog);
                }
            }
        } else if (tc.isDebugEnabled())
            Tr.debug(tc, "tmsref is null");

    }

    /**
     * Is the Transaction Log hosted in a database?
     *
     * @return true if it is
     */
    @Override
    public boolean isSQLRecoveryLog() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "isSQLRecoveryLog " + _isSQLRecoveryLog);
        return _isSQLRecoveryLog;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#needToCoordinateServices()
     */
    @Override
    public boolean needToCoordinateServices() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "needToCoordinateServices");
        return true;
    }

    /**
     * Determine whether the server is configured to store Tran Logs in an RDBMS,
     */
    private void checkDataSourceRef() {

        Object configuredDSR = _props.get("dataSourceRef");
        if (configuredDSR == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "dataSourceRef is not specified, log to filesys");
            _isSQLRecoveryLog = false;
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "dataSourceRef is specified, log to RDBMS");
            // We'll set the logDir to maintain tWAS code compatibility. First we need to
            // check get the table suffix string if it is set in server.xml
            String suffixStr = (String) _props.get("transactionLogDBTableSuffix");

            if (tc.isDebugEnabled())
                Tr.debug(tc, "suffixStr is " + suffixStr + ", of length " + suffixStr.length());

            if (suffixStr != null && !suffixStr.trim().isEmpty()) {
                suffixStr = suffixStr.trim();
                logDir = "custom://com.ibm.rls.jdbc.SQLRecoveryLogFactory?datasource=Liberty" +
                         ",tablesuffix=" + suffixStr;
            } else
                logDir = "custom://com.ibm.rls.jdbc.SQLRecoveryLogFactory?datasource=Liberty";

            if (tc.isDebugEnabled())
                Tr.debug(tc, "logDir now set to ", logDir);
            _isSQLRecoveryLog = true;
        }
    }

    /**
     * This method should only be used where logging to a file system.
     *
     * @return the full path of the log directory
     */
    private String parseTransactionLogDirectory() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "parseTransactionLogDirectory", _props);

        String configuredLogDir = (String) _props.get("transactionLogDirectory");
        // don't allow null to be returned - it will result in use of a location
        // that is shared
        // across all local servers and thus risks log corruption
        if (configuredLogDir == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "using default log dir as config is null");
            // set default
            configuredLogDir = defaultLogDir;
        } else {
            // ensure dir string ends with a '/'
            if (!configuredLogDir.endsWith("/")) {
                configuredLogDir = configuredLogDir + "/";
            }
        }

        // resolve the configured value
        WsResource logDirResource = null;
        try {
            // Synchronize to ensure we see a valid locationService
            synchronized (this) {
                logDirResource = locationService.resolveResource(configuredLogDir);
            }
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "IllegalArgumentException from location service for dir string: " + configuredLogDir);
            if (!configuredLogDir.equals(defaultLogDir)) {
                // try using the default
                configuredLogDir = defaultLogDir;
                try {
                    // Synchronize to ensure we see a valid locationService
                    synchronized (this) {
                        logDirResource = locationService.resolveResource(configuredLogDir);
                    }
                } catch (IllegalArgumentException ex) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Secondary IllegalArgumentException " + ex + " from location service for dir string: " + configuredLogDir);
                    // if we can't establish a tran log dir, we need a way to disable
                    // the transaction service
                    // rethrow the original exception
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "parseTransactionLogDirectory", e);
                    throw e;
                }
            } else {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "parseTransactionLogDirectory", e);
                throw e;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "logDirResource: " + logDirResource);

        // get full path string from resource
        logDir = logDirResource.asFile().getPath().replaceAll("\\\\", "/");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "parseTransactionLogDirectory", logDir);
        return logDir;
    }

    @Override
    public void setApplId(byte[] name) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setApplId - " + Arrays.toString(name));
        // Store the applId.
        this._applId = name.clone();
    }

    @Override
    public byte[] getApplId() {
        // Determine the applId.
        final byte[] result = _applId;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "getApplId - " + Arrays.toString(result));
        return result;
    }

    @Override
    public void shutDownFramework() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "shutDownFramework", _frameworkShutting);
        if (!_frameworkShutting) {
            try {
                if (_cc != null) {
                    final Bundle bundle = _cc.getBundleContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION);

                    if (bundle != null)
                        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                            @Override
                            public Void run() throws BundleException {
                                // Force quick shutdown with no quiesce period
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "force quick shutdown");

                                bundle.getBundleContext().registerService(ForcedServerStop.class, new ForcedServerStop(), null);

                                try {
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "stop bundle");
                                    bundle.stop();
                                } catch (BundleException bex) {
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "caught bundlex - " + bex);
                                    throw bex;
                                }
                                return null;
                            }
                        });
                }
                _frameworkShutting = true;
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "shutDownFramework", e);

                // do not FFDC this.
                // exceptions during bundle stop occur if framework is already stopping or stopped
            } finally {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "shutDownFramework");
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#enableHADBPeerLocking()
     */
    @Override
    public boolean enableHADBPeerLocking() {
        return (Boolean) _props.get("enableHADBPeerLocking");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getTimeBetweenHeartbeats()
     */
    @Override
    public int getTimeBetweenHeartbeats() {
        Number num = (Number) _props.get("timeBetweenHeartbeats");
        return num.intValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getPeerTimeBeforeStale()
     */
    @Override
    public int getPeerTimeBeforeStale() {
        Number num = (Number) _props.get("peerTimeBeforeStale");
        return num.intValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getLightweightLogRetryInterval()
     */
    @Override
    public int getLightweightLogRetryInterval() {
        Number num = (Number) _props.get("lightweightLogRetryInterval");
        return num.intValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getLightweightLogRetryLimit()
     */
    @Override
    public int getLightweightLogRetryLimit() {
        Number num = (Number) _props.get("lightweightLogRetryLimit");
        return num.intValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getLogRetryInterval()
     */
    @Override
    public int getLogRetryInterval() {
        Number num = (Number) _props.get("logRetryInterval");
        return num.intValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getLogRetryLimit()
     */
    @Override
    public int getLogRetryLimit() {
        Number num = (Number) _props.get("logRetryLimit");
        return num.intValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#enableLogRetries()
     */
    @Override
    public boolean enableLogRetries() {
        return (Boolean) _props.get("enableLogRetries");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getRetriableSqlCodes()
     */
    @Override
    public List<Integer> getRetriableSqlCodes() {
        String sqlcodes = (String) _props.get("retriableSqlCodes");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getRetriableSqlCodes " + sqlcodes);

        if (!_setRetriableSqlcodes) {
            retriableSqlCodeList = parseSqlCodes(sqlcodes);
            _setRetriableSqlcodes = true;
        }

        return retriableSqlCodeList;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.config.ConfigurationProvider#getNonRetriableSqlCodes()
     */
    @Override
    public List<Integer> getNonRetriableSqlCodes() {
        String sqlcodes = (String) _props.get("nonRetriableSqlCodes");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getNonRetriableSqlCodes " + sqlcodes);

        if (!_setNonRetriableSqlcodes) {
            nonRetriableSqlCodeList = parseSqlCodes(sqlcodes);
            _setNonRetriableSqlcodes = true;
        }

        return nonRetriableSqlCodeList;
    }

    private List<Integer> parseSqlCodes(String sqlCodesStr) {
        List<Integer> sqlCodeList = new ArrayList<Integer>();
        if (sqlCodesStr != null && !sqlCodesStr.trim().isEmpty()) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "There are sqlcodes to parse " + sqlCodesStr);
            List<String> sqlCodeStringList = Arrays.asList(sqlCodesStr.split(","));

            for (String sqlcode : sqlCodeStringList) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Isolated string sqlcode " + sqlcode);
                int intSqlCode = 0;
                try {
                    intSqlCode = Integer.parseInt(sqlcode.trim());
                } catch (NumberFormatException nfe) {
                    Tr.audit(tc, "WTRN0107W: " +
                                 "Malformed sqlcode " + sqlcode + " in configuration " + sqlCodesStr);
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Isolated integer sqlcode " + intSqlCode);
                sqlCodeList.add(intSqlCode);
            }
        }
        return sqlCodeList;
    }

    @Override
    public boolean isDataSourceFactorySet() {
        return _dataSourceFactorySet;
    }

    /**
     * Fail checkpoint whenever the default or configured transaction log
     * directory exists and cannot be deleted.
     */
    protected void addTransactionLogDirCheckpointHook() {
        if (!CheckpointPhase.getPhase().restored()) {
            final String logDir = JTMConfigurationProvider.logDir;

            if (logDir == null)
                return;

            CheckpointPhase.getPhase().addSingleThreadedHook(new CheckpointHook() {
                @Override
                public void prepare() {
                    if (!recursiveDelete(new File(logDir))) {
                        throw new IllegalStateException(Tr.formatMessage(tc, "ERROR_CHECKPOINT_TRANLOGS_EXIST", logDir));
                    }
                }
            });
        }
    }

    /**
     * Delete a file or a directory, including all directory contents.
     *
     * @param fileToRemove The target File.
     * @return false iff fileToRemove exists and cannot be deleted, otherwise return true.
     */
    protected boolean recursiveDelete(final File fileToRemove) {
        if (fileToRemove == null)
            return true;

        if (!fileToRemove.exists())
            return true;

        boolean success = true;

        if (fileToRemove.isDirectory()) {
            File[] files = fileToRemove.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    success |= recursiveDelete(file);
                } else {
                    success |= file.delete();
                }
            }
            files = fileToRemove.listFiles();
            if (files.length == 0)
                success |= fileToRemove.delete();
        } else {
            success |= fileToRemove.delete();
        }
        return success;
    }
}

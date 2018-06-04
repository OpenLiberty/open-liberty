/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.security.AccessController;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.resource.ResourceException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.j2c.poolmanager.ConnectionPoolProperties;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.jca.adapter.PurgePolicy;
import com.ibm.ws.jca.cm.AbstractConnectionFactoryService;
import com.ibm.ws.jca.cm.ConnectionManagerService;
import com.ibm.ws.jca.cm.ConnectorService;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * Connection manager/pool configuration.
 */
public class ConnectionManagerServiceImpl extends ConnectionManagerService {

    private static final TraceComponent tc = Tr.register(ConnectionManagerServiceImpl.class, J2CConstants.traceSpec, J2CConstants.NLS_FILE);
    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    /**
     * Mapping of connection factory key to connection manager.
     */
    private final Map<String, ConnectionManager> cfKeyToCM = new ConcurrentHashMap<String, ConnectionManager>();

    /**
     * Utility that collects various core services needed by connection management.
     */
    private ConnectorService connectorSvc;

    /**
     * Lock for reading and updating connection manager service configuration.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Name that we display in messages. It is based on the id (or config.id),
     * but shortened for nested config.
     */
    private String name;

    /**
     * The pool manager.
     */
    private PoolManager pm;

    /**
     * The MBean associated with the PoolManager
     */
    private PoolManagerMBeanImpl pmMBean = null;

    /**
     * Connection manager configuration.
     */
    private Map<String, Object> properties;

    /**
     * ComponentContext for registering MBean with Declarative Services
     */
    BundleContext bndCtx = null;

    /**
     * The class loader for the RA.
     */
    private ClassLoader raClassLoader = null;

    /**
     * Default constructor for declarative services to use before activating the service.
     */
    public ConnectionManagerServiceImpl() {}

    /**
     * Constructor for a default connectionManager service.
     * This service is not managed by declarative services
     * and does not get activated/modified/deactivated.
     *
     * @param name name of the connection factory.
     */
    public ConnectionManagerServiceImpl(String name) {
        // Generate a name based on the connection factory name
        // For example,  dataSource[ds1]/connectionManager
        this.name = name + '/' + CONNECTION_MANAGER;
    }

    /**
     * Declarative Services method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "activate", properties);

        bndCtx = priv.getBundleContext(context);

        // config.displayId contains the Xpath identifier.
        name = (String) properties.get("config.displayId");

        lock.writeLock().lock();
        try {
            this.properties = properties;
        } finally {
            lock.writeLock().unlock();
        }

        if ("file".equals(properties.get("config.source"))) {
            if (name.startsWith("#APP-RESOURCE#")) // avoid conflicts with app-defined data sources
                throw new IllegalArgumentException(ConnectorService.getMessage("UNSUPPORTED_VALUE_J2CA8011", name, ID, CONNECTION_MANAGER));
        }
        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "activate");
    }

    /**
     * Add an observer for this connection manager service.
     *
     * @param observer ConnectionFactoryService that is using this ConnectionManagerService.
     */
    @Override
    public void addObserver(Observer observer) {
        super.addObserver(observer);
        if (countObservers() > 1) {
            super.deleteObserver(observer);
            AbstractConnectionFactoryService cfSvc = (AbstractConnectionFactoryService) observer;
            Object[] params = new Object[] { CONNECTION_MANAGER, name, cfSvc.getConfigElementName() };
            RuntimeException failure = connectorSvc.ignoreWarnOrFail(tc, null, UnsupportedOperationException.class, "CARDINALITY_ERROR_J2CA8040", params);
            if (failure != null)
                throw failure;
        }
    }

    /**
     * Create and initialize the connection manager/pool configuration
     * based on the connection factory configuration.
     * Precondition: invoker must have the write lock for this connection manager service.
     *
     * @param svc the connection factory service
     * @throws ResourceException if an error occurs
     */
    private void createPoolManager(AbstractConnectionFactoryService svc) throws ResourceException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "createPoolManager", svc, properties);

        J2CGlobalConfigProperties gConfigProps = processServerPoolManagerProperties(svc, properties);

        pm = new PoolManager(svc, null, // dsProps=null misses out on nondeferredreaper, but we don't have that anyways
                        gConfigProps, raClassLoader);

        if (bndCtx == null)
            bndCtx = priv.getBundleContext(FrameworkUtil.getBundle(getClass()));

        try {
            pmMBean = new PoolManagerMBeanImpl(pm, svc.getFeatureVersion());
            pmMBean.register(bndCtx);
        } catch (MalformedObjectNameException e) {
            pmMBean = null;
            throw new ResourceException(e);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "createPoolManager", this);
    }

    /**
     * Declarative Services method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context context for this component instance.
     * @throws InstanceNotFoundException
     * @throws MBeanRegistrationException
     */
    protected void deactivate(ComponentContext context) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "deactivate", name);

        lock.writeLock().lock();
        try {
            if (pmMBean != null) {
                pmMBean.unregister();
                pmMBean = null;
            }
            pm = null;
            properties = null;
        } finally {
            lock.writeLock().unlock();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "deactivate");
    }

    /**
     * Destroy all connection factories that are using this connection manager service.
     *
     * @param svc the connection factory service.
     * @throws InstanceNotFoundException
     * @throws MBeanRegistrationException
     */
    @Override
    public void destroyConnectionFactories() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled()) {
            final String pmName;
            if (pm != null)
                pmName = pm.getUniqueId();
            else
                pmName = "factory name not avaiable";
            Tr.entry(this, tc, "destroyConnectionFactories", pmName);
        }

        lock.writeLock().lock();
        try {
            if (pmMBean != null) {
                pmMBean.unregister();
                pmMBean = null;
            }
            if (pm != null) {
                try {
                    pm.serverShutDown();
                    pm = null;
                    cfKeyToCM.clear();
                } catch (Throwable x) {
                    FFDCFilter.processException(x, getClass().getName(), "263", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, x.getMessage(), CommonFunction.stackTraceToString(x));
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "destroyConnectionFactories");
    }

    /**
     * Construct the CMConfigData, including properties from the resource reference, if applicable.
     *
     * @param cfSvc connection factory service
     * @param ref resource reference.
     * @return com.ibm.ejs.j2c.CMConfigData
     */
    private final CMConfigData getCMConfigData(AbstractConnectionFactoryService cfSvc, ResourceInfo refInfo) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "getCMConfigData");

        // Defaults for direct lookup
        int auth = J2CConstants.AUTHENTICATION_APPLICATION;
        int branchCoupling = ResourceRefInfo.BRANCH_COUPLING_UNSET;
        int commitPriority = 0;
        int isolation = Connection.TRANSACTION_NONE;
        int sharingScope;
        String loginConfigName = null;
        HashMap<String, String> loginConfigProps = null;
        String resRefName = null;

        if (refInfo != null) {
            if (refInfo.getAuth() == ResourceRef.AUTH_CONTAINER)
                auth = J2CConstants.AUTHENTICATION_CONTAINER;

            branchCoupling = refInfo.getBranchCoupling();
            commitPriority = refInfo.getCommitPriority();
            isolation = refInfo.getIsolationLevel();
            loginConfigName = refInfo.getLoginConfigurationName();
            loginConfigProps = toHashMap(refInfo.getLoginPropertyList());
            resRefName = refInfo.getName();
            sharingScope = refInfo.getSharingScope();
        } else {
            if (properties != null) {
                Object enableSharingForDirectLookups = properties.get("enableSharingForDirectLookups");
                sharingScope = enableSharingForDirectLookups == null
                               || (Boolean) enableSharingForDirectLookups ? ResourceRefInfo.SHARING_SCOPE_SHAREABLE : ResourceRefInfo.SHARING_SCOPE_UNSHAREABLE;
            } else {
                sharingScope = ResourceRefInfo.SHARING_SCOPE_SHAREABLE;
            }
        }

        CMConfigData cmConfig = new CMConfigDataImpl(cfSvc.getJNDIName(), sharingScope, isolation, auth, cfSvc.getID(), loginConfigName, loginConfigProps, resRefName, commitPriority, branchCoupling, null // no mmProps
        );

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "getCMConfigData", cmConfig);
        return cmConfig;
    }

    /**
     * Returns the connection manager for this configuration.
     * This method lazily initializes the connection manager service if necessary.
     *
     * @param ref reference to the connection factory.
     * @param svc the connection factory service
     * @return the connection manager for this configuration.
     * @throws ResourceException if an error occurs
     */
    @Override
    public ConnectionManager getConnectionManager(ResourceInfo refInfo, AbstractConnectionFactoryService svc) throws ResourceException {

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "getConnectionManager", refInfo, svc);

        ConnectionManager cm;
        lock.readLock().lock();
        try {
            if (pm == null)
                try {
                    // Switch to write lock for lazy initialization
                    lock.readLock().unlock();
                    lock.writeLock().lock();

                    if (pm == null)
                        createPoolManager(svc);
                } finally {
                    // Downgrade to read lock for rest of method
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }

            CMConfigData cmConfigData = getCMConfigData(svc, refInfo);
            String cfDetailsKey = cmConfigData.getCFDetailsKey();
            cm = cfKeyToCM.get(cfDetailsKey);
            if (cm == null) {
                CommonXAResourceInfo xaResInfo = new EmbXAResourceInfo(cmConfigData);
                J2CGlobalConfigProperties gConfigProps = pm.getGConfigProps();
                synchronized (this) {
                    cm = cfKeyToCM.get(cfDetailsKey);
                    if (cm == null) {
                        cm = new ConnectionManager(svc, pm, gConfigProps, xaResInfo);
                        cfKeyToCM.put(cfDetailsKey, cm);
                    }
                }
            }

        } finally {
            lock.readLock().unlock();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "getConnectionManager", cm);
        return cm;
    }

    /**
     * Called by Declarative Services to modify service config properties
     *
     * @param newProperties the new configuration
     * @throws Exception if unable to complete the modifications
     */
    protected void modified(Map<String, Object> newProperties) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "modified", newProperties);
        boolean reCreateNeeded = false;

        lock.writeLock().lock();
        try {
            properties = newProperties;
            if (pm != null) {
                try {
                    processServerPoolManagerProperties(null, newProperties);
                } catch (IllegalStateException e1) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "modify failed, retrying", CommonFunction.stackTraceToString(e1));
                    try {
                        processServerPoolManagerProperties(null, newProperties);
                    } catch (IllegalStateException e2) {
                        reCreateNeeded = true;
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "retry failed", CommonFunction.stackTraceToString(e2));
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        if (reCreateNeeded)
            try {
                setChanged();
                notifyObservers();
            } catch (Throwable x) {
                FFDCFilter.processException(x, getClass().getName(), "402", this);
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, x.getMessage(), CommonFunction.stackTraceToString(x));
            }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "modified");
    }

    /**
     * Create or update properties to current values defined by customer.
     * If the pool manager (pm) is null, then J2CGlobalConfigProperties are created.
     * If the pool manager (pm) is not null, then the J2CGlobalConfigProperties of the pool manager are updated.
     *
     * Our goal of this method is to always be able to return false and dynamically update the properties
     * without needed to destroy and recreate objects.
     *
     * Precondition: invoker must have the write lock for this connection manager service.
     *
     * @param svc connection factory service - this not needed if the pool manager already exists.
     * @param properties properties for this connectionManager service.
     * @return gConfigProps J2CGlobalConfigProperties is returned if we created a new one. Null if we modified an existing pool manager.
     * @throws ResourceException
     */
    private J2CGlobalConfigProperties processServerPoolManagerProperties(AbstractConnectionFactoryService svc, Map<String, Object> properties) throws ResourceException {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = properties == null ? Collections.EMPTY_MAP : new HashMap<String, Object>(properties);

        int agedTimeout = validateProperty(map, J2CConstants.POOL_AgedTimeout, -1, TimeUnit.SECONDS, -1, Integer.MAX_VALUE, true, connectorSvc);
        int connectionTimeout = validateProperty(map, J2CConstants.POOL_ConnectionTimeout, 30, TimeUnit.SECONDS, -1, Integer.MAX_VALUE, true, connectorSvc);
        int maxIdleTime = validateProperty(map, MAX_IDLE_TIME, ConnectionPoolProperties.DEFAULT_UNUSED_TIMEOUT, TimeUnit.SECONDS, -1, Integer.MAX_VALUE, false, connectorSvc);
        int maxNumberOfMCsAllowableInThread = validateProperty(map, MAX_CONNECTIONS_PER_THREAD, 0, null, 0, Integer.MAX_VALUE, true, connectorSvc);
        int maxPoolSize = validateProperty(map, MAX_POOL_SIZE, 50, null, 0, Integer.MAX_VALUE, true, connectorSvc);
        int minPoolSize = 0;
        if (maxPoolSize == 0)
            minPoolSize = validateProperty(map, MIN_POOL_SIZE, 0, null, 0, Integer.MAX_VALUE, true, connectorSvc);
        else
            minPoolSize = validateProperty(map, MIN_POOL_SIZE, 0, null, 0, maxPoolSize, true, connectorSvc);

        int numConnectionsPerThreadLocal = validateProperty(map, NUM_CONNECTIONS_PER_THREAD_LOCAL, ConnectionPoolProperties.DEFAULT_numConnectionsPerThreadLocal,
                                                            null, 0, Integer.MAX_VALUE, true, connectorSvc);
        int reapTime = validateProperty(map, J2CConstants.POOL_ReapTime, ConnectionPoolProperties.DEFAULT_REAP_TIME, TimeUnit.SECONDS, -1, Integer.MAX_VALUE, false, connectorSvc);

        boolean throwExceptionOnMCThreadCheck = false;

        /*
         * The purge policy has three property values in Liberty. The three same combinations
         * can be set in twas, but two properties need to be used.
         * EntirePool --> EntirePool
         * FailingConnectionOnly --> FailingConnectionOnly with defaultPretestOptimizationOverride = false
         * ValidateAllConnections --> FailingConnectionOnly with defaultPretestOptimizationOverride = true
         */
        PurgePolicy purgePolicy = validateProperty(map, J2CConstants.POOL_PurgePolicy, PurgePolicy.EntirePool, PurgePolicy.class, connectorSvc);

        // Identify unrecognized properties - TODO: enable when we have a stricter variant of onError
        //map.remove(ID);
        //for (String name : map.keySet())
        //    if (!name.contains(".")) { // Ignore config service properties that contain .
        //        ResourceException failure = connectorSvc.ignoreWarnOrFail(tc, null, ResourceException.class, "UNKNOWN_PROP_J2CA8010",
        //                                                                          CONNECTION_MANAGER, name);
        //        if (failure != null)
        //            throw failure;
        //    }

        if (pm != null) {
            // Connection pool exists, dynamically update values that have changed.
            if (pm.gConfigProps.getAgedTimeout() != agedTimeout)
                pm.gConfigProps.setAgedTimeout(agedTimeout);

            if (pm.gConfigProps.getConnctionWaitTime() != connectionTimeout)
                pm.gConfigProps.setConnectionTimeout(connectionTimeout);

            if (pm.gConfigProps.getUnusedTimeout() != maxIdleTime)
                pm.gConfigProps.setUnusedTimeout(maxIdleTime);

            if (pm.gConfigProps.getMaxConnections() != maxPoolSize)
                pm.gConfigProps.setMaxConnections(maxPoolSize);

            if (pm.gConfigProps.getMinConnections() != minPoolSize)
                pm.gConfigProps.setMinConnections(minPoolSize);

            if (!pm.gConfigProps.getPurgePolicy().equals(purgePolicy))
                pm.gConfigProps.setPurgePolicy(purgePolicy);

            if (pm.gConfigProps.getReapTime() != reapTime)
                pm.gConfigProps.setReapTime(reapTime);

            if (pm.gConfigProps.getnumConnectionsPerThreadLocal() != numConnectionsPerThreadLocal)
                pm.gConfigProps.setnumConnectionsPerThreadLocal(numConnectionsPerThreadLocal);

            if (pm.gConfigProps.getMaxNumberOfMCsAllowableInThread() != maxNumberOfMCsAllowableInThread)
                pm.gConfigProps.setMaxNumberOfMCsAllowableInThread(maxNumberOfMCsAllowableInThread);

            return null;
        } else {
            // Connection pool does not exist, create j2c global configuration properties for creating pool.
            return new J2CGlobalConfigProperties(name, svc, false, // logMissingTranContext
                            200, // maxSharedBuckets,
                            100, // maxFreePoolHashSize,
                            false, // diagnoseConnectionUsage,
                            connectionTimeout, maxPoolSize, minPoolSize, purgePolicy, reapTime, maxIdleTime, agedTimeout, ConnectionPoolProperties.DEFAULT_HOLD_TIME_LIMIT, 0, // commit priority not supported
                            numConnectionsPerThreadLocal, maxNumberOfMCsAllowableInThread, throwExceptionOnMCThreadCheck);

        }
    }

    /**
     * Declarative services method to set the ConnectorService.
     */
    protected void setConnectorService(ConnectorService svc) {
        connectorSvc = svc;
    }

    /**
     * Declarative services method to unset the ConnectorService.
     */
    protected void unsetConnectorService(ConnectorService svc) {
        connectorSvc = null;
    }

    /**
     * Method tests whether a property's value is valid or not.<p>
     * Specify the range the value must fall in
     * with <code>minVal</code> and <code>maxVal</code>. <p>
     *
     * This method will also handle raising an exception or Tr message if the
     * property is invalid.
     *
     * @param map map of configured properties
     * @param propName the name of the property being tested
     * @param defaultVal the default value
     * @param units units for duration type. Null if not a duration type.
     * @param minVal the minimum value
     * @param maxVal the maximum value
     * @param immediateSupported weather or not property supports immediate action
     * @param connectorSvc connector service
     * @return the configured value if the value is valid, else the default value
     * @throws ResourceException
     */
    private int validateProperty(Map<String, Object> map, String propName, int defaultVal, TimeUnit units, Integer minVal, Integer maxVal, Boolean immediateSupported,
                                 ConnectorService connectorSvc) throws ResourceException {
        Object value = map.remove(propName);

        //Get property value and check if it is a number and convert it to long
        long val;
        if (value == null)
            return defaultVal;
        else if (value instanceof Number)
            val = ((Number) value).longValue();
        else
            try {
                val = units == null ? Integer.parseInt((String) value) : MetatypeUtils.evaluateDuration((String) value, units);
            } catch (Exception x) {
                ResourceException resX = connectorSvc.ignoreWarnOrFail(tc, x, ResourceException.class, "UNSUPPORTED_VALUE_J2CA8011", value, propName, name);
                if (resX == null)
                    return defaultVal;
                else
                    throw resX;
            }

        //Check if immediate is supported.  If not throw exception
        if (!immediateSupported && val == 0) {
            ResourceException immediateFailure = connectorSvc.ignoreWarnOrFail(tc, null, ResourceException.class, "UNSUPPORTED_VALUE_J2CA8011",
                                                                               val, propName, CONNECTION_MANAGER);
            if (immediateFailure != null)
                throw immediateFailure;
        }

        //Finally check if value is within tolerance
        if (val < minVal || val > maxVal) {
            ResourceException failure = connectorSvc.ignoreWarnOrFail(tc, null, ResourceException.class, "UNSUPPORTED_VALUE_J2CA8011",
                                                                      val, propName, CONNECTION_MANAGER);
            if (failure != null)
                throw failure;
            return defaultVal;
        }
        return (int) val;
    }

    /**
     * Method tests whether a property's value is valid or not.<p>
     *
     * This method will also handle raising an exception or Tr message if the
     * property is invalid.
     *
     * @param map map of configured properties
     * @param propName the name of the property being tested
     * @param defaultVal the default value
     * @param type enumeration consisting of the valid values
     * @param connectorSvc connector service
     * @return the configured value if the value is valid, else the default value
     */
    private static final <E extends Enum<E>> E validateProperty(Map<String, Object> map, String propName, E defaultVal, Class<E> type, ConnectorService connectorSvc) {
        String strVal = (String) map.remove(propName);
        if (strVal == null)
            return defaultVal;

        try {
            return E.valueOf(type, strVal);
        } catch (RuntimeException x) {
            x = connectorSvc.ignoreWarnOrFail(tc, x, x.getClass(), "UNSUPPORTED_VALUE_J2CA8011", strVal, propName, CONNECTION_MANAGER);
            if (x == null)
                return defaultVal;
            else
                throw x;
        }
    }

    /**
     * Utility method that converts a list of properties to HashMap.
     *
     * @param propList list of name/value pairs
     * @return mapping of name/value pairs
     */
    private final static HashMap<String, String> toHashMap(List<? extends ResourceInfo.Property> propList) {
        if (propList == null)
            return null;

        HashMap<String, String> propMap = new HashMap<String, String>();
        for (ResourceInfo.Property prop : propList)
            propMap.put(prop.getName(), prop.getValue());

        return propMap;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jca.cm.ConnectionManagerService#addRaClassLoader(java.lang.ClassLoader)
     */
    @Override
    public void addRaClassLoader(ClassLoader raClassLoader) {
        this.raClassLoader = raClassLoader;
    }

}
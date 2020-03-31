/*******************************************************************************
 * Copyright (c) 1997, 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.Serializable;
import java.util.Properties;

import javax.resource.spi.TransactionSupport;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jca.adapter.PurgePolicy;
import com.ibm.ws.jca.cm.AbstractConnectionFactoryService;
import com.ibm.ws.jca.cm.AppDefinedResource;

/**
 * The <B>J2CGlobalConfigProperties</B> will contain all the Configuration Related
 * properties used by the PoolManager/ConnectionManger related code with the
 * exception of those properties that are unique to a given CF/DS lookup, which
 * will be stored in a CMConfigData object.
 * <p>
 * The properties are organized into three groups:
 * <ul>
 * <li> <B>Read Only properties </B> - which will be public final properties so that
 * the code can have fast (performance wise) access to them.
 * <li> <B>Immediately Dynamically Updateable properties </B> - those which can have
 * their values changed from the outside at any time without the need for
 * Synchronization and without causing errors to the running code.
 * <li> <B>Dynamically Updateable - requires Property Change processing. </B>
 * </ul>
 *
 */
public final class J2CGlobalConfigProperties implements PropertyChangeListener, Serializable {
    /**  */

    private static final TraceComponent tc = Tr.register(J2CGlobalConfigProperties.class, J2CConstants.traceSpec, J2CConstants.messageFile);

    // TODO: consider if this class really needs to be serializable
    private static final long serialVersionUID = 3666445884103132373L;

    // The following group of properties are read only and can only be set via
    // the constructor.  There are no getter methods for performance reasons.
    // Code within the J2C Packages will be allowed to access these variable
    // directly.
    private final String jndiName; // This string contains the jndi name, null if there is none.
    private final String XpathId; // This will contain the xpath unique identifier.
    protected final String cfName; // The jndi name if there is one, if not the base of the xpath id.
                                   // Used for jca.cm messages, not passed out externally.

    protected final boolean logMissingTranContext;

    protected final boolean validatingMCFSupported;

    /*
     * Instead of claiming the victom, the RRA code can reauth the connection. So,
     * when this is enabled, the RRA supports an interesting for on reauthentication.
     *
     * Basically, the j2c code attempt to find a matching connection without thinking about
     * reauth. But, before we start the processing to claim the connection, we set a
     * flag on the managed connection and send the non-matching connection to getConnection.
     *
     * RRA will check the flag to see if it was a matching or non-matching connection.
     */
    protected final boolean sendClaimedVictomToGetConnection;
    protected final boolean raSupportsReauthentication;

    // The following properties are a subset of readonly properties that can't
    // be initialized via the constructor and thus should only be set once.
    // Set once behavior will be enforced with an exception.

    /*
     * Indicates the type of "TransactionResourceRegistration" support
     * provided by the configured MCF.
     *
     * This property may be null or may be set to "static" or dynamic".
     * "dynamic" indicates transaction resource registration is deferred.
     *
     * This property is applicable to both WAS/Distributed and WAS z/OS.
     */
    private String transactionResourceRegistration;

    protected boolean checkManagedConnectionInstanceof = true;
    //The following lock is NOT a config prop but I'm putting
    //it here for convience.
    protected final transient Integer checkManagedConnectionInstanceofLock = new Integer(0);
    protected boolean checkManagedConnectionInstanceofInitialized = false;

    protected boolean embeddedRa = false;
    protected boolean embeddedRaInitialized = false;
    protected boolean connectionSynchronizationProvider = false;
    protected boolean connectionSynchronizationProviderInitialized = false;
    protected boolean connectionPoolingEnabled = true;
    protected boolean connectionPoolingEnabledInitialized = false;
    protected boolean instanceOfDissociatableManagedConnection = false;
    protected boolean instanceOfDissociatableManagedConnectionInitialized = false;

    protected int commitPriority = 0;

    // JCC 1.6 7.13
    // Level of transaction support by a resource adapter. It is based on
    // merged result of transaction support in the resource adapter's
    // deployment descriptor and Connector annotation and it may be changed
    // at runtime, after completing configuring the MCF instance,  to a value
    // returned by getTransactionSupport method, if MCF implemented
    // TransactionSupport interface.
    // Enum constants for this property are:
    //   NoTransaction
    //   LocalTransaction
    //   XATransaction
    protected final TransactionSupport.TransactionSupportLevel transactionSupport;

    // The following properties are a subset of readonly properties that can
    // be initialized twice:  once as a result of properties that come in
    // from the getObjectInstance call and possibly as the result of dynamically
    // detecting a marker interface when the first ManagedConnection is
    // retrieved.

    private boolean dynamicEnlistmentSupported;

    private boolean smartHandleSupport;
    protected boolean cciLocalTranSupported;

    // The following group of properties are dynamically updateable, but require
    // the component code to stage the update at an appropriate time.  These
    // properties are defined as "bound" properties per the java bean definition.
    //
    // In order to get notified of the request to change the property,
    // an object must implement the java.beans.PropertyChangeListener interface,
    // and register with an instance of this object.
    //
    // IMPORTANT:  for this type of property, the user of the property MUST
    // keep a local copy and then update the local copy upon receiving a
    // PropertyChangeEvent.
    //
    // Note that a reference to an instance of this object will be passed to
    // both the CM and PM upon construction. This will be used to register
    // as a listener.

    /**
     * Connection timeout is the interval, in
     * seconds, after which a connection request times out and a
     * ConnectionWaitTimeoutException is thrown. The wait may be necessary if
     * the maximum value of connections to a particular connection pool has been
     * reached (Max Connections) . This value has no meaning if Max Connections
     * is set to -1 (infinite number of ManagedConnections). If Connection
     * Timeout is set to 0 or a negative number the Pool Manager waits until a
     * connection can be allocated (which happens when the number of connections
     * falls below Max Connections). The default value is 180.
     * <p>
     * For example, if Connection Timeout is set to 300 and the maximum number
     * of connection has been reached, the pool manager will wait for a managed
     * connection to become available for approximately 300 seconds, then throw
     * a ConnectionWaitTimeoutException.
     * <p>
     * Mbeans: The Connection timeout can be changed at any time while the pool
     * is active. All connection request that are waiting for a connection will
     * will be changed to the new interval time and they will be returned to the
     * wait state if there are no available connections.
     *
     */
    private int connectionTimeout = 0;
    /**
     * The maximum number of ManagedConnections that can be created in this
     * pool. ManagedConnections represent the physical connection to the backend
     * resource. Once this number is reached, no new ManagedConnections are
     * created and the requester waits until a ManagedConnection which is
     * currently in use is returned to the pool or a
     * ConnectionWaitTimeoutException is thrown. The default value is 10, which
     * allows the number of ManagedConnections to grow infinitely. If Max
     * Connections is set to 0, Connection Timeout will not be used. For example
     * if Max Connections is set to 5, and there are 5 ManagedConnections in use
     * the pool manager will wait for a managed connection become free for
     * Connection Timeout seconds. A ConnectionWaitTimeoutException will be
     * thrown, if a connection is not return to the pool in time.
     * <p><p>
     * MBeans: The max connections can be changed at any time.
     * <p><p>
     * The max connections will be changed to the new value. The number of connections will
     * increase to the new value, if the new value is greater than the current
     * value. The number of connections will decrease to the new value, if the
     * new value is less than the current value and agedTimeout or Reap Time is
     * used. No attempt will be made to reduce the total number of connection if
     * agedTimeout or Reap Time are not used.
     *
     */
    private int maxConnections = 0;
    /**
     * Min Connections is the minimum number of ManagedConnections that should
     * be maintained. Until this number is reached, the pool maintenance thread
     * will not discard any ManagedConnections. However no attempt will be made
     * to bring the number of connections up to this number. For example if Min
     * Connections is 3, and one managed connection has been created, that
     * managed connection will not be discarded by the Unused Timeout thread,
     * but two additional ManagedConnections will not be automatically created.
     * The default value is 1, which will maintain no connections.
     * <p><p>
     * MBeans: The min connections will be change to the new value and follow
     * the rules documented above.
     */
    private int minConnections = 0;
    /**
     * Purge Policy specifies how to purge connections when a 'stale connection'
     * or 'fatal connection error' is detected. Valid values are 'Entire Pool'
     * (default) and 'Failing Connection Only'. If Entire Pool is specified all
     * ManagedConnections in the pool will be destroyed when a stale connection
     * is detected. If Failing Connection Only is specified the pool will
     * attempt to destroy the stale connection, the other connections will
     * remain in the pool. Final destruction of connection which are in use at
     * the time of the error may be delayed, however, those connection will
     * never be returned to the free pool.
     *
     * MBeans: The purge policy will be changed to the new value and will follow
     * the rules above.
     */
    private PurgePolicy purgePolicy = PurgePolicy.EntirePool;
    /**
     * Reap Time (seconds - converted to milliseconds in TaskTimer)
     * <p><p> Reap time is
     * the interval, in seconds, between runs of the pool maintenance thread.
     * The default value is -1 which will disable the pool maintenance thread.
     * Another way to disable the pool maintenance thread is to set Unused
     * Timeout to -1 and Aged Timeout to -1.
     * <p><p>
     * When the pool maintenance thread runs
     * it discards any connections that have been unused longer than Unused
     * Timeout, down to Min Connections, and any connections that have been
     * active longer than Aged Timeout. If pool maintenance is enabled, Reap Time
     * should be less than Aged Timeout and Unused Timeout. For example if Reap
     * Time is set to 60, the pool maintenance thread will run every minute. The
     * Reap Time interval will affect the accuracy of the Unused Timeout and
     * Aged Timeout. The smaller the interval, the greater the accuracy. The
     * Reap Time interval will also affect performance. Smaller intervals mean
     * that the pool maintenance thread will run more often and degrade
     * performance.
     * <p><p>
     * MBeans: The reap time interval will be changed to the new value at the
     * next interval. Example: If the interval is 30 seconds and 10 second
     * remain and the value is changed to 60 seconds, the first interval will be
     * 70 seconds and there after 60 seconds.
     */
    private int reapTime = 0;
    /**
     * Unused Timeout (seconds)
     * <p><p>
     * Unused Timeout is the approximate interval in
     * seconds after which an unused, or idle, connection is discarded. The
     * default value is 1800. The recommended way to disable the pool maintenance
     * thread is to set Reap Time to -1, in which case Unused Timeout and Aged
     * Timeout will be ignored. However if Unused Timeout and Aged Timeout are
     * set to -1, the pool maintenance thread will run, but only
     * ManagedConnections which timeout due to non-zero timeout values will be
     * discarded. Unused Timeout should be set higher than Reap Timeout for
     * optimal performance. In addition, unused ManagedConnections will only be
     * discarded if the current number of connection not in use exceeds the Min
     * Connections setting. For example if unused timeout is set to 120, and the
     * pool maintenance thread is enabled (Reap Time is not -1), any managed
     * connection that has been unused for two minutes will be discarded
     * <p><p>
     * Note that accuracy of this timeout, as well as performance, is affect by
     * the Reap Time. See Reap Time for more information.
     * <p><p>
     * MBeans: The unused timeout will be changed to the new value and follows
     * the rules above.
     */
    private int unusedTimeout = 0;
    /**
     * Aged Timeout (seconds)
     * <p><p>
     * Aged Timeout is the approximate interval (or age
     * of a ManagedConnection), in seconds, before a ManagedConnection is
     * discarded. The default value is -1 which will allow active
     * ManagedConnections to remain in the pool indefinitely. The recommended
     * way to disable the pool maintenance thread is to set Reap Time to -1, in
     * which case Aged Timeout and Unused Timeout will be ignored. However if
     * Aged Timeout or Unused Timeout are set to -1, the pool maintenance thread
     * will run, but only ManagedConnections which timeout due to non-zero
     * Connection Timeout values will be discarded. Aged Timeout should be set
     * higher than Reap Timeout for optimal performance. For example if Aged
     * Timeout is set to 1200, and Reap Time is not -1, any ManagedConnection
     * that has been in use for 20 minutes will be discarded from the pool.
     * <p><p>
     * Note that accuracy of this timeout, as well as performance, is affect by
     * the Reap Time. See Reap Time for more information.
     * <p><p>
     * MBeans: The aged timeout will be changed to the new value and will follow
     * the rules above. (Which are wrong)
     */
    private int agedTimeout = 0;
    /**
     * Comment for <code>agedTimeoutMillis</code>
     */
    private long agedTimeoutMillis = 0;
    /**
     * Comment for <code>stopPoolRequests</code>
     */
    private final boolean stopPoolRequests = false;
    /**
     * The max number of shared buckets created. sharedPool[maxSharedBuckets]
     */
    private int maxSharedBuckets;
    /**
     * Comment for <code>maxFreePoolHashSize</code>
     */
    private int maxFreePoolHashSize;

    protected int holdTimeLimit = 10;
    private int numConnectionsPerThreadLocal = 0;

    private int orphanConnHoldTimeLimitSeconds = 10; // Dynamically Updateable

    // =============== End of Config Properties ===============================

    /**
     * <code>changeSupport</code> is used to delegate support for property
     * change notification.
     */
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    /**
     * <code>changeSupport</code> is used to delegate support for property
     * change notification.
     */
    private final VetoableChangeSupport vChangeSupport = new VetoableChangeSupport(this);
    protected Properties raMetaDataProps = null;
    protected Properties dsMetaDataProps = null;
    public boolean callResourceAdapterStatMethods = false;
    public int numberOfInuseConnections = 0;
    public transient Integer numberOfInuseConnectionsLockObject = new Integer(0);
    public int numberOfFreeConnections = 0;
    public transient Integer numberOfFreeConnectionsLockObject = new Integer(0);
    protected transient Integer maxNumberOfMCsAllowableInThread = null;
    protected transient Boolean throwExceptionOnMCThreadCheck = null;

    String appName;
    String modName;
    String compName;

    /**
     * Creates a traditional J2CGlobalConfigProperties object
     */
    public J2CGlobalConfigProperties(
                                     String _xpathId,
                                     AbstractConnectionFactoryService cfSvc,
                                     boolean _logMissingTranContext,
                                     int _maxSharedBuckets,
                                     int _maxFreePoolHashSize,
                                     boolean diagnoseConnectionUsage,
                                     int connectionTimeout,
                                     int maxConnections,
                                     int minConnections,
                                     PurgePolicy purgePolicy,
                                     int reapTime,
                                     int unusedTimeout,
                                     int agedTimeout,
                                     int holdTimeLimit,
                                     int commitPriority,
                                     int numConnectionsPerThreadLocal,
                                     Integer maxNumberOfMCsAllowableInThread,
                                     Boolean throwExceptionOnMCThreadCheck) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "<init>", "Full Constructor");
        }
        this.XpathId = _xpathId;
        this.jndiName = cfSvc.getJNDIName();
        if (jndiName != null)
            this.cfName = jndiName;
        else {
            int cmIndex = _xpathId.indexOf("/connectionManager");
            if (cmIndex < 0)
                this.cfName = _xpathId;
            else
                this.cfName = _xpathId.substring(0, cmIndex);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "xpath = " + XpathId + "  jndi = " + jndiName + "cfName = " + cfName);
        }

        dynamicEnlistmentSupported = false; // Will become true later if managed connection implements LazyEnlistableManagedConnection
        this.logMissingTranContext = _logMissingTranContext;
        this.transactionSupport = cfSvc.getTransactionSupport();
        this.smartHandleSupport = false; // Will become true later if managed connection implements DissociatableManagedConnection
        this.raSupportsReauthentication = cfSvc.getReauthenticationSupport();
        this.maxSharedBuckets = _maxSharedBuckets;
        this.maxFreePoolHashSize = _maxFreePoolHashSize;
        this.sendClaimedVictomToGetConnection = raSupportsReauthentication;
        this.connectionTimeout = connectionTimeout;
        this.maxConnections = maxConnections;
        this.minConnections = minConnections;
        this.purgePolicy = purgePolicy;
        this.reapTime = reapTime;
        this.unusedTimeout = unusedTimeout;
        this.agedTimeout = agedTimeout;
        this.agedTimeoutMillis = (long) agedTimeout * 1000;
        this.holdTimeLimit = holdTimeLimit;
        this.commitPriority = commitPriority;
        this.numConnectionsPerThreadLocal = numConnectionsPerThreadLocal;
        this.maxNumberOfMCsAllowableInThread = maxNumberOfMCsAllowableInThread;
        this.throwExceptionOnMCThreadCheck = throwExceptionOnMCThreadCheck;

        /*
         * This value will be checked in the fatelErrorNotification code. We
         * will attempt to cleanup and destroy the connections returned by the
         * method getInvalidConnections().
         *
         * If the connection is active, it will be marked stale.
         */
        validatingMCFSupported = cfSvc.getValidatingManagedConnectionFactorySupport();

        cciLocalTranSupported = true;

        // Set the application, module, and component names
        if (cfSvc instanceof AppDefinedResource) {
            AppDefinedResource resource = (AppDefinedResource) cfSvc;
            appName = resource.getApplication();
            modName = resource.getModule();
            compName = resource.getComponent();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "<init>");
        }
    }

    /**
     * @return the xpathId
     */
    public String getXpathId() {
        return XpathId;
    }

    public int getnumConnectionsPerThreadLocal() {
        return numConnectionsPerThreadLocal;
    }

    public void setnumConnectionsPerThreadLocal(
                                                int numConnectionsPerThreadLocal) {
        try {
            vChangeSupport.fireVetoableChange("numConnectionsPerThreadLocal", this.numConnectionsPerThreadLocal, numConnectionsPerThreadLocal);
        } catch (PropertyVetoException e) {
            Tr.info(tc, "UNABLE_TO_CHANGE_PROPERTY_J2CA0169", "numConnectionsPerThreadLocal", XpathId);
            IllegalStateException e2 = new IllegalStateException("Unable to set property numConnectionsPerThreadLocal at this time.  Please try again.");
            e2.initCause(e);
            throw e2;
        }

        this.numConnectionsPerThreadLocal = numConnectionsPerThreadLocal;
    }

    // Property Change Event Notification Registration Methods.
    public void addPropertyChangeListener(PropertyChangeListener pcListener) {
        changeSupport.addPropertyChangeListener(pcListener);
    }

    public void removePropertyChangeListerner(PropertyChangeListener pcListener) {
        changeSupport.removePropertyChangeListener(pcListener);
    }

    public void addVetoableChangeListener(VetoableChangeListener vListener) {
        vChangeSupport.addVetoableChangeListener(vListener);
    }

    public void removeVetoableChangeListerner(VetoableChangeListener vListener) {
        vChangeSupport.removeVetoableChangeListener(vListener);
    }

    // Dynamic Property access Methods.

    /**
     * @return Returns the orphanConnHoldTimeLimitSeconds.
     */
    public synchronized final int getOrphanConnHoldTimeLimitSeconds() {
        return orphanConnHoldTimeLimitSeconds;
    }

    /**
     * @param orphanConnHoldTimeLimitSeconds
     *            The orphanConnHoldTimeLimitSeconds to set.
     */
    public synchronized final void setOrphanConnHoldTimeLimitSeconds(int _holdTimeLimit) {
        changeSupport.firePropertyChange("orphanConnHoldTimeLimitSeconds", this.orphanConnHoldTimeLimitSeconds, _holdTimeLimit);
        this.orphanConnHoldTimeLimitSeconds = _holdTimeLimit;
    }

    /**
     * @return Returns the unusedTimeout.
     */
    public synchronized final int getUnusedTimeout() {
        return unusedTimeout;
    }

    /**
     * @param timeout
     *            The unusedTimeout to set.
     */
    public synchronized final void setUnusedTimeout(int _unusedTimeout) {
        changeSupport.firePropertyChange("unusedTimeout", this.unusedTimeout, _unusedTimeout);
        this.unusedTimeout = _unusedTimeout;
    }

    /**
     * @return Returns the agedTimeout.
     */
    public synchronized final int getAgedTimeout() {
        return agedTimeout;
    }

    /**
     * @param agedTimeout
     *            The agedTimeout to set.
     */
    public synchronized final void setAgedTimeout(int _agedTimeout) {
        changeSupport.firePropertyChange("agedTimeout", this.agedTimeout, _agedTimeout);
        this.agedTimeout = _agedTimeout;
    }

    /**
     * @return Returns the connectionTimeout.
     */
    public synchronized final int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @param connectionTimeout
     *            The connectionTimeout to set.
     */
    public synchronized final void setConnectionTimeout(int _connectionTimeout) {
        changeSupport.firePropertyChange("connectionTimeout", this.connectionTimeout, _connectionTimeout);
        this.connectionTimeout = _connectionTimeout;
    }

    /**
     * @return Returns the minConnections.
     */
    public synchronized final int getMinConnections() {
        return minConnections;
    }

    /**
     * @param minConnections
     *            The minConnections to set.
     */
    public synchronized final void setMinConnections(int _minConnections) {
        changeSupport.firePropertyChange("minConnections", this.minConnections, _minConnections);
        this.minConnections = _minConnections;
    }

    /**
     * @return Returns the purgePolicy.
     */
    public synchronized final PurgePolicy getPurgePolicy() {
        return purgePolicy;
    }

    /**
     * @param purgePolicy
     *            The purgePolicy to set.
     */
    public synchronized final void setPurgePolicy(PurgePolicy _purgePolicy) {
        changeSupport.firePropertyChange("purgePolicy", this.purgePolicy, _purgePolicy);
        this.purgePolicy = _purgePolicy;
    }

    /**
     * @return Returns the reapTime.
     */
    public synchronized final int getReapTime() {
        return reapTime;
    }

    /**
     * @param reapTime
     *            The reapTime to set.
     */
    public synchronized final void setReapTime(int _reapTime) {
        changeSupport.firePropertyChange("reapTime", this.reapTime, _reapTime);
        this.reapTime = _reapTime;
    }

    /**
     * @return Returns the stopPoolRequests.
     */
    public synchronized final boolean isStopPoolRequests() {
        return stopPoolRequests;
    }

    /**
     * @param cciLocalTransactionSupported
     *            The cciLocalTransactionSupported to set
     *            Intended to be called by ConnectionManager.initializeUOW()
     */
    protected void setLocalTranSupport(boolean _cciLocalTranSupported) {
        if (cciLocalTranSupported == _cciLocalTranSupported)
            return;
        this.cciLocalTranSupported = _cciLocalTranSupported;
    }

    /**
     *
     * @return cciLocalTransactionSupported
     */
    protected boolean isCciLocalTranSupported() {
        return cciLocalTranSupported;
    }

    // Access Methods for dynamic properties which can be Vetoed.

    /**
     * @return Returns the maxConnections.
     */
    public synchronized final int getMaxConnections() {
        return maxConnections;
    }

    /**
     * @param maxConnections
     *            The maxConnections to set.
     */
    public synchronized final void setMaxConnections(int _maxConnections) {
        try {
            vChangeSupport.fireVetoableChange("maxConnections", this.maxConnections, _maxConnections);
        } catch (PropertyVetoException e) {
            Tr.info(tc, "UNABLE_TO_CHANGE_PROPERTY_J2CA0169", "maxConnections", XpathId);
            IllegalStateException e2 = new IllegalStateException("Unable to set property maxConnections at this time.  Please try again.");
            e2.initCause(e);
            throw e2;
        }
        this.maxConnections = _maxConnections;
    }

    /**
     * @return Returns the maxFreePoolHashSize.
     */
    public synchronized final int getMaxFreePoolHashSize() {
        return maxFreePoolHashSize;
    }

    /**
     * @param maxFreePoolHashSize
     *            The maxFreePoolHashSize to set.
     */
    public synchronized final void setMaxFreePoolHashSize(int _maxFreePoolHashSize) {
        try {
            vChangeSupport.fireVetoableChange("maxFreePoolHashSize", this.maxFreePoolHashSize, _maxFreePoolHashSize);
        } catch (PropertyVetoException e) {
            Tr.info(tc, "UNABLE_TO_CHANGE_PROPERTY_J2CA0169", "maxFreePoolHashSize", XpathId);
            IllegalStateException e2 = new IllegalStateException("Unable to set property maxFreePoolHashSize at this time.  Please try again.");
            e2.initCause(e);
            throw e2;
        }
        this.maxFreePoolHashSize = _maxFreePoolHashSize;
    }

    /**
     * @return Returns the maxSharedBuckets.
     */
    public synchronized final int getMaxSharedBuckets() {
        return maxSharedBuckets;
    }

    /**
     * @param maxSharedBuckets
     *            The maxSharedBuckets to set.
     */
    public synchronized final void setMaxSharedBuckets(int _maxSharedBuckets) {
        try {
            vChangeSupport.fireVetoableChange("maxSharedBuckets", this.maxSharedBuckets, _maxSharedBuckets);
        } catch (PropertyVetoException e) {
            Tr.info(tc, "UNABLE_TO_CHANGE_PROPERTY_J2CA0169", "maxSharedBuckets", XpathId);
            IllegalStateException e2 = new IllegalStateException("Unable to set property maxSharedBuckets at this time.  Please try again.");
            e2.initCause(e);
            throw e2;
        }
        this.maxSharedBuckets = _maxSharedBuckets;
    }

    // Set Once Property access Methods.
    /*
     * removing method to prevent double checked locking problem in FreePool class.
     * protected boolean isCheckManagedConnectionInstanceof() {
     * return checkManagedConnectionInstanceof;
     * }
     */
    /**
     * @param checkManagedConnectionInstanceof Flag to indicate if the check has been done. True/False.
     *
     */
    protected void setCheckManagedConnectionInstanceof(boolean checkManagedConnectionInstanceof) {

        isInitialized(checkManagedConnectionInstanceofInitialized, "checkManagedConnectionInstanceof");
        checkManagedConnectionInstanceofInitialized = true;
        this.checkManagedConnectionInstanceof = checkManagedConnectionInstanceof;
    }

    /**
     * @return Returns the connectionPoolingEnabled.
     */
    protected boolean isConnectionPoolingEnabled() {
        return connectionPoolingEnabled;
    }

    /**
     * @param connectionPoolingEnabled The connectionPoolingEnabled to set.
     */
    protected void setConnectionPoolingEnabled(boolean _connectionPoolingEnabled) {
        /*
         * is really for enabling shared connection support when connection
         * pooling is disabled. This method needs to be fixed to enable
         *
         * There are other methods like this one in this class that need
         * to be reviewed and changed in
         * wasx. For set once methods, they should check if the value needs to be
         * changed and/or just change the value, and then set the initialize flag
         * so it cann't be changed again.
         * One suggestion, adding synchronize to some of the methods
         * may be a good idea. Additional trace would be nice.
         */
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "setConnectionPoolingEnabled");
        }
        isInitialized(connectionPoolingEnabledInitialized, "connectionPoolingEnabled");
        this.connectionPoolingEnabled = _connectionPoolingEnabled;
        connectionPoolingEnabledInitialized = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "connectionPoolingEnabled is " + this.connectionPoolingEnabled);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "setConnectionPoolingEnabled");
        }
    }

    /**
     * @return Returns the connectionSynchronizationProvider.
     */
    protected boolean isConnectionSynchronizationProvider() {
        return connectionSynchronizationProvider;
    }

    /**
     * @param connectionSynchronizationProvider The connectionSynchronizationProvider to set.
     */
    protected void setConnectionSynchronizationProvider(boolean _connectionSynchronizationProvider) {
        if (connectionSynchronizationProvider == _connectionSynchronizationProvider)
            return;
        isInitialized(connectionSynchronizationProviderInitialized, "connectionSynchronizationProvider");
        this.connectionSynchronizationProvider = _connectionSynchronizationProvider;
        connectionSynchronizationProviderInitialized = true;
    }

    /**
     * @return Returns the instanceOfDissociatableManagedConnection.
     */
    protected boolean isInstanceOfDissociatableManagedConnection() {
        return instanceOfDissociatableManagedConnection;
    }

    /**
     * @param instanceOfDissociatableManagedConnection The instanceOfDissociatableManagedConnection to set.
     */
    protected void setInstanceOfDissociatableManagedConnection(boolean _instanceOfDissociatableManagedConnection) {
        if (instanceOfDissociatableManagedConnection == _instanceOfDissociatableManagedConnection)
            return;
        isInitialized(instanceOfDissociatableManagedConnectionInitialized, "instanceOfDissociatableManagedConnection");
        this.instanceOfDissociatableManagedConnection = _instanceOfDissociatableManagedConnection;
        instanceOfDissociatableManagedConnectionInitialized = true;
    }

    // Utility method for set once methods.
    private void isInitialized(boolean condition, String propName) {
        if (condition == true) {
            IllegalStateException e = new IllegalStateException("J2CGlobalConfigProperties: internal error.  Set once property already set.");
            Tr.error(tc, "SET_ONCE_PROP_ALREADY_SET_J2CA0159", (Object) null);
            throw e;
        }
    }

    // Set "Twice" Properties.
    /**
     * @return Returns the smartHandleSupport.
     */
    protected boolean isSmartHandleSupport() {
        return smartHandleSupport;
    }

    /**
     * @param smartHandleSupport The smartHandleSupport to set.
     */
    protected void setSmartHandleSupport(boolean _smartHandleSupport) {
        this.smartHandleSupport = _smartHandleSupport;
    }

    /**
     * @return Returns the dynamicEnlistmentSupported.
     */
    protected boolean isDynamicEnlistmentSupported() {
        return dynamicEnlistmentSupported;
    }

    /**
     * @param dynamicEnlistmentSupported The dynamicEnlistmentSupported to set.
     */
    protected void setDynamicEnlistmentSupported(boolean dynamicEnlistmentSupported) {
        this.dynamicEnlistmentSupported = dynamicEnlistmentSupported;
    }

    /**
     * @return Returns the embeddedRa.
     */
    protected boolean isEmbeddedRa() {
        return embeddedRa;
    }

    /**
     * @param embeddedRa
     *            The embeddedRa to set.
     */
    protected void setEmbeddedRa(boolean embeddedRa) {
        embeddedRaInitialized = true;
        this.embeddedRa = embeddedRa;
    }

    // Other Methods.

    @Override
    public String toString() {

        StringBuffer buf = new StringBuffer();

        String nl = CommonFunction.nl;

        buf.append(nl + "J2CGlobalConfigProperties:" + nl);
        buf.append(nl + "  <-- Read Only -->" + nl);
        buf.append("  jndiName                        : " + jndiName + nl);
        buf.append("  transactionResourceRegistration : " + transactionResourceRegistration + nl);
        buf.append("  cciLocalTranSupported           : " + cciLocalTranSupported + nl);
        buf.append("  logMissingTranContext           : " + logMissingTranContext + nl);
        buf.append("  embeddedRa                      : " + embeddedRa + nl);
        buf.append("  validatingMCFSupported          : " + validatingMCFSupported + nl);
        buf.append("  cciLocalTranSupported           : " + cciLocalTranSupported + nl);
        buf.append("  sendClaimedVictomToGetConnection: " + sendClaimedVictomToGetConnection + nl);
        buf.append("  raSupportsReauthentication      : " + raSupportsReauthentication + nl);
        buf.append("  connectionSynchronizationProvider: " + connectionSynchronizationProvider + nl);
        buf.append("  connectionPoolingEnabled        : " + connectionPoolingEnabled + nl);
        buf.append("  instanceOfDissociatableManagedConnection: " + instanceOfDissociatableManagedConnection + nl);
        buf.append("  dynamicEnlistmentSupported      : " + dynamicEnlistmentSupported + nl);
        buf.append("  smartHandleSupport              : " + smartHandleSupport + nl);
        buf.append("  transactionSupport              : " + transactionSupport).append(nl);
        if (raMetaDataProps != null) {
            buf.append("  adapterName                     : " + raMetaDataProps.get("AdapterName") + nl);
            buf.append("  adapterShortDescription         : " + raMetaDataProps.get("AdapterShortDescription") + nl);
            buf.append("  adapterVenderName               : " + raMetaDataProps.get("AdapterVenderName") + nl);
            buf.append("  adapterVersion                  : " + raMetaDataProps.get("AdapterVersion") + nl);
            buf.append("  interationSpecsSupported        : " + raMetaDataProps.get("InterationSpecsSupported") + nl);
            buf.append("  specVersion                     : " + raMetaDataProps.get("SpecVersion") + nl);
        }
        if (dsMetaDataProps != null) {
            buf.append("  a                     : " + dsMetaDataProps.get("") + nl);
        }
        buf.append(nl + "  <-- Dynamic -->" + nl);
        buf.append("  connectionTimeout               : " + connectionTimeout + nl);
        buf.append("  maxConnections                  : " + maxConnections + nl);
        buf.append("  minConnections                  : " + minConnections + nl);
        buf.append("  purgePolicy                     : " + purgePolicy + nl);
        buf.append("  reapTime                        : " + reapTime + nl);
        buf.append("  unusedTimeout                   : " + unusedTimeout + nl);
        buf.append("  agedTimeout                     : " + agedTimeout + nl);
        buf.append("  agedTimeoutMillis               : " + agedTimeoutMillis + nl);
        buf.append("  holdTimeLimit                   : " + holdTimeLimit + nl);
        buf.append("  stopPoolRequests                : " + stopPoolRequests + nl);
        buf.append("  maxSharedBuckets                : " + maxSharedBuckets + nl);
        buf.append("  maxFreePoolHashSize             : " + maxFreePoolHashSize + nl);
        buf.append("  orphanConnHoldTimeLimitSeconds  : " + orphanConnHoldTimeLimitSeconds + nl);
        buf.append(" numConnectionsPerThreadLocal  : " + numConnectionsPerThreadLocal + nl);

        return buf.toString();
    }

    // Request Stat variable group

    public synchronized final void applyRequestGroupConfigChanges() {
        // Note: on the following call the "false,true" forces the change notification
        // to be sent.  The listener will then pick up all property values in the
        // group.
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "applyRequestGroupConfigChanges");

        changeSupport.firePropertyChange("applyRequestGroupConfigChanges", false, true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "applyRequestGroupConfigChanges");
    }

    // Misc - Note that when only one attribute is needed,
    // a normal property change event can be used.

    /*
     * (non-Javadoc)
     *
     * Listen for alert property change events.
     *
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {}

    public int getConnctionWaitTime() {
        return connectionTimeout;
    }

    /**
     * Returns the jndi name
     */
    public String getJNDIName() {
        return jndiName;
    }

    /*
     * If the value returned is zero, this function is disabled.
     */
    public int getMaxNumberOfMCsAllowableInThread() {
        if (maxNumberOfMCsAllowableInThread != null)
            return maxNumberOfMCsAllowableInThread.intValue();
        else
            return 0;
    }

    public void setMaxNumberOfMCsAllowableInThread(
                                                   Integer maxNumberOfMCsAllowableInThread) {
        changeSupport.firePropertyChange("maxNumberOfMCsAllowableInThread", this.maxNumberOfMCsAllowableInThread, maxNumberOfMCsAllowableInThread);
        this.maxNumberOfMCsAllowableInThread = maxNumberOfMCsAllowableInThread;
    }

    public boolean getThrowExceptionOnMCThreadCheck() {
        if (throwExceptionOnMCThreadCheck != null)
            return throwExceptionOnMCThreadCheck.booleanValue();
        else
            return false;
    }

    public void throwExceptionOnMCThreadCheck(
                                              Boolean throwExceptionOnMCThreadCheck) {
        changeSupport.firePropertyChange("throwExceptionOnMCThreadCheck", this.throwExceptionOnMCThreadCheck, throwExceptionOnMCThreadCheck);
        this.throwExceptionOnMCThreadCheck = throwExceptionOnMCThreadCheck;
    }
}

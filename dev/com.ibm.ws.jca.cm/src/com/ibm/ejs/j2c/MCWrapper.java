/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.DissociatableManagedConnection;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.SharingViolationException;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.osgi.framework.Version;

import com.ibm.tx.jta.OnePhaseXAResource;
import com.ibm.websphere.jca.pmi.JCAPMIHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.j2c.TranWrapper;
import com.ibm.ws.jca.adapter.WSManagedConnection;
import com.ibm.ws.jca.adapter.WSManagedConnectionFactory;
import com.ibm.ws.tx.rrs.RRSXAResourceFactory;

/**
 * Scope : Server
 * <p>
 * Object model : 1 instance per ManagedConnection
 * <p>
 * Encapsulates a <code>ManagedConnection</code> instance to aid in managing its
 * life cyle and use within the connection manager. This includes all PMI data
 * relative to the given <code>ManagedConnection</code>. Also acts as a central "hub"
 * which contains references and provides access to, a number of other objects
 * which are also related to managing an instance of a <code>ManagedConnection</code>.
 * The related objects are:
 * <ul>
 * <li>ConnectionEventListener
 * <li>UOWCoordinator
 * <li>XATransactionWrapper
 * <li>LocalTransactionWrapper
 * <li>NoTransactionWrapper
 * <li>RRSGlobalTransactionWrapper //WS14620.01
 * <li>PoolManager
 * <li>J2CPerf
 * <li>ConnectionManager
 * </ul>
 * <p>
 * <table BORDER=2 COLS=6 WIDTH="100%" >
 * <caption><b>MCWrapper State Transition Table</b></caption>
 * <tr>
 * <td><b><font size=-1>Method\State</font></b></td>
 * <td><b><font size=-1>STATE_<br>NEW</font></b></td>
 * <td><b><font size=-1>STATE_<br>ACTIVE_FREE</font></b></td>
 * <td><b><font size=-1>STATE_<br>ACTIVE_INUSE</font></b></td>
 * <td><b><font size=-1>STATE_TRAN_<br>WRAPPER_IN_USE</font></b></td>
 * <td><b><font size=-1>STATE_<br>INACTIVE</font></b></td>
 * </tr>
 * <tr>
 * <td><b><font size=-1>getManagedConnection</font></b></td>
 * <td><font size=-1>STATE_<br>ACTIVE_FREE</font></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>STATE_<br>ACTIVE_FREE</font></td>
 * </tr>
 * <tr>
 * <td><b><font size=-1>markInUse</font></b></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>STATE_<br>ACTIVE_INUSE</font></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>exception</font></td>
 * </tr>
 * <tr>
 * <td><br><b><font size=-1>getNoTranWrapper</font></b><b>
 * <br><b><font size=-1>getLocal<br>TranWrapper</font></b></td>
 * <br><b><font size=-1>getNoTranWrapper</font></b></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>STATE_TRAN_<br>WRAPPER_IN_USE</font></td>
 * <td><font size=-1>STATE_TRAN_<br>WRAPPER_IN_USE</font></td>
 * <td><font size=-1>exception</font></td>
 * </tr>
 * <tr>
 * <td><b><font size=-1>transaction<br>Complete</font></b></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>STATE_<br>ACTIVE_INUSE</font></td>
 * <td><font size=-1>exception</font></td>
 * </tr>
 * <tr>
 * <tr>
 * <td><b><font size=-1>cleanup</font></b></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>STATE_<br>ACTIVE_FREE</font></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>exception</font></td>
 * </tr>
 * <tr>
 * <td><b><font size=-1>destroy</font></b></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>STATE_<br>INACTIVE</font></td>
 * <td><font size=-1>STATE_<br>INACTIVE</font>
 * <br><font size=-1>via cleanup</font></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>exception</font></td>
 * </tr>
 * <tr>
 * <td><b><font size=-1>releaseTo<br>PoolManager</font></b></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>exception</font></td>
 * <td><font size=-1>STATE_<br>ACTIVE_FREE</font>
 * <br><font size=-1>via cleanup</font></td>
 * <td><font size=-1>STATE_<br>ACTIVE_FREE</font>
 * <br><font size=-1>via cleanup</font></td>
 * <td><font size=-1>exception</font></td>
 * </tr>
 * </table>
 */
public final class MCWrapper implements com.ibm.ws.j2c.MCWrapper, JCAPMIHelper {

    protected final HashMap<Object, HandleList> mcwHandleList = new HashMap<Object, HandleList>();

    private int fatalErrorValue;

    private boolean connectionSynchronizationProvider;

    private ManagedConnectionFactory _managedConnectionFactory = null;

    static final long serialVersionUID = -861999777608926414L;

    private int mcConnectionCount;

    // State Constants

    /**
     * This is the initial state set by the constructor. MCWrapper
     * is not usable in this state.
     */
    protected final static int STATE_NEW = 0;

    /**
     * In this state, the MCWrapper has at least a ManagedConnection reference
     * and a PoolManager Reference. It may also have references to
     * XATransactionWrapper, LocalTransactionWrapper, NoTransactionWrapper, and
     * ConnectionEventListener. In addition, the ManagedConnection will have had
     * the ConnectionEventListener registered with it. This MCWrapper is pooled in
     * the PoolManagers unused pool in the ACTIVE state. Also, all variables which
     * need to be reset every use should be at some default value when in the ACTIVE
     * state.
     */
    protected final static int STATE_ACTIVE_FREE = 1;

    /**
     * This MCWrapper exists in this state whenever it has been allocated to
     * user, via reserve, but has not been used in a transaction. The PoolManager
     * is responsible to call the <code>markInUse()</code> method on this object
     * when ever it is doing reserve processing which will allocate this object
     * to the user.
     */
    protected final static int STATE_ACTIVE_INUSE = 2;

    /**
     * In this state one of the transaction wrappers
     * has been created and is currently in use for this MCWrapper.
     */
    protected final static int STATE_TRAN_WRAPPER_INUSE = 3;

    /**
     * In this state, the MCWrapper's reference to the ManagedConnection
     * and PoolManager are null. It may still have references to
     * XATransactionWrapper, LocalTransactionWrapper, NoTransactionWrapper, and
     * ConnectionEventListener, but those wrappers are not allowed to hold connection
     * related resoures (for example: XAResoure). A MCWrapper is pooled in
     * the MCWrapperPool when in the INACTIVE state. In addition, all variables which
     * need to be reset every use should be at some default value when in the INACTIVE
     * state.
     */
    protected final static int STATE_INACTIVE = 4;

    /**
     * Keeps track of the internal state of this MCWrapper.
     */
    private int state = STATE_NEW;

    private static final String[] STATESTRINGS = {
                                                   "STATE_NEW",
                                                   "STATE_ACTIVE_FREE",
                                                   "STATE_ACTIVE_INUSE",
                                                   "STATE_TRAN_WRAPPER_INUSE",
                                                   "STATE_INACTIVE"
    };

    // TranWrapper Constants
    /**
     * No Transaction support
     */
    protected final static int NONE = 0;

    /**
     * XA Transaction wrapper
     */
    protected final static int XATXWRAPPER = 1;

    /**
     * Local Transaction wrapper
     */
    protected final static int LOCALTXWRAPPER = 2;

    /**
     * No Transaction wrapper
     */
    protected final static int NOTXWRAPPER = 3;

    /**
     * RRS Global Transaction wrapper (zOS only)
     */
    protected final static int RRSGLOBALTXWRAPPER = 4;

    /**
     * RRS Local Transaction wrapper (zOS only)
     */
    protected final static int RRSLOCALTXWRAPPER = 5;

    private int tranWrapperInUse = NONE;
    private static final String[] TRANWRAPPERSTRINGS = {
                                                         "NONE",
                                                         "XATXWRAPPER",
                                                         "LOCALTXWRAPPER",
                                                         "NOTXWRAPPER",
                                                         "RRSGLOBALTXWRAPPER",
                                                         "RRSLOCALTXWRAPPER"
    };

    // References to the other objects in the Cluster (associated objects).
    private ManagedConnection mc = null; // The ManagedConnection this object wrappers.
    private transient ConnectionEventListener eventListener = null;
    private XATransactionWrapper xaTranWrapper = null;
    private LocalTransactionWrapper localTranWrapper = null;
    private NoTransactionWrapper noTranWrapper = null;
    private RRSGlobalTransactionWrapper rrsGlobalTranWrapper = null;
    private RRSLocalTransactionWrapper rrsLocalTranWrapper = null;
    private UOWCoordinator uowCoord = null;
    protected transient PoolManager pm = null;
    private boolean _supportsReAuth = false;
    private int hashMapBucket = 0;
    private Object sharedPoolCoordinator = null;
    private Object mcWrapperList = null;

    private Object currentSharedPool = null;
    private boolean isParkedWrapper = false;
    private int _hashMapBucketReAuth = 0;
    private Subject _subject = null;
    private ConnectionRequestInfo _cri = null;

    // ConnectionManager Reference.
    private transient ConnectionManager cm = null;

    // The time the ManagedConnection was installed into this wrapper.
    private long createdTimeStamp;
    // The time this wrapper was released back to the PoolManager, and thus is unused.
    private long unusedTimeStamp;
    /**
     * Combined Subject and CRI hash code
     */
    private int subjectCRIHashCode = 0;

    /**
     * This will indicate where the connection is in the pool
     * code.
     *
     * Valid states are:
     * 0 = Not in any pool, currently in transition.
     * 1 = In free pool
     * 2 = In shared pool
     * 3 = In unshared pool
     * 4 = In waiter pool
     */
    private final AtomicInteger poolState = new AtomicInteger(0);

    /**
     * While afterCompletion (transaction end) is being processed, a resource adapter can use connection
     * closed event to close an open handle that may not have been closed by the application. If
     * the closed event is used at this time, we need to prevent a second release of the managed
     * connection. The alreadyBeingReleased false will be set to true when a managed connection
     * is being released in the connection pool. If another release is requested, the second release
     * will just return allowing the first release to continue its processing. This will avoid duplicate
     * managed connections in the free pool.
     */
    private final AtomicBoolean alreadyBeingReleased = new AtomicBoolean(false);

    /**
     * @return the poolLocation
     */
    protected boolean isAlreadyBeingReleased() {
        return alreadyBeingReleased.get();
    }

    protected void setAlreadyBeingReleased(boolean value) {
        alreadyBeingReleased.set(value);
    }

    /**
     * A destroy state of true for a connection will result in the connection being
     * destoryed when returned to the free pool. In addition, ANY connection pool requests
     * for this connection will result in a ResourceException.
     */
    private boolean destroyState = false;

    private boolean purgeState = false;

    // If stale is true, it means that the entire connection pool was purged because
    //  of an error somewhere, but this connection was in use at the time. By marking it stale it
    //  tells the CM two things: 1) if this connection is now processing an error, don't initiate
    //  another pool purge, and 2) if this connection is being released back to the pool, it
    //  should be destroyed instead of being put back into the free pool.
    private boolean stale = false;
    //  private int dbrequestMonitorPoolID = -1;
    /*
     * - When a resource adapter incorrectly uses connectionErrorOccurred during a
     * createManagedConnection, matchManagedConnection or getConnection
     * we can not reuse the mcw. Reuse of the mcw may results in duplicate
     * entries in the MCWrapperListPool. To be safe, all mcw connectionErrorOccurred
     * events will be marked not to be reused.
     */
    protected boolean do_not_reuse_mcw = false;
    private boolean inSharedPool = false;

    /**
     * There is one recoveryToken needed per ManagedConnectionFactory and thus one per PoolManager.
     * The <code>PoolManager</code> will get the token from the transaction service in it's constructor
     * and pass it into the MCWrapperPool. The MCWrapper pool will in turn set it into all the MCWrapper
     * instances for use by the XATransactionWrapper.
     */
    private int recoveryToken;

    protected String mcWrapperObject_hexString = null;

    private static final TraceComponent tc = Tr.register(MCWrapper.class,
                                                         J2CConstants.traceSpec,
                                                         J2CConstants.messageFile);

    private final String nl = CommonFunction.nl;
    private Throwable initialRequestStackTrace = null;
    private String threadId;
    private long lastAllocationTime;
    private String threadName;
    /*
     * The spec does specify that we can call cleanup many times, but
     * this was added so we don't waste a call to cleanup if we have
     * already called cleanup.
     */
    //  private boolean cleanupAllReadyCalled = false;

    /*
     * This value is used to determing whether we need
     * to call the setLogWriter method on the mc.
     */
    private boolean logWriterSet = false;

    private transient boolean _transactionErrorOccurred = false;

    private long holdTimeStart = 0;

    protected J2CGlobalConfigProperties gConfigProps;

    protected long currentUseStartTime = 0;
    protected boolean useStartTimeSet = false;
    protected boolean holdStartTimeSet = false;
    protected long totalUseTime = 0;
    protected long totalHoldTime = 0;
    private boolean pretestThisConnection = false;
    private boolean aborted = false;
    private boolean qmidenabled = true;

    /**
     * Constructor is protected and should only be used by
     * <code>MCWrapperPool</code>.
     *
     *
     * @param PoolManager
     * @param J2CPerf
     *
     */
    protected MCWrapper(PoolManager _pm, J2CGlobalConfigProperties _gConfigProps) {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "<init>");
        }

        // PoolManager is set once for the life of the MCWrapper since there is a 1-to-1 relationship
        //  between a given PM instance and the MCWrapper pool.
        pm = _pm;
        this.gConfigProps = _gConfigProps;
        mcWrapperObject_hexString = Integer.toHexString(this.hashCode());

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "<init>");
        }

    }

    /**
     * Delists a RRS XA resource information with the transaction manager.
     *
     * @param xaResource The XA resource to delist.
     *
     * @throws Exception if an error occurred during the enlistment process.
     */
    final void delistRRSXAResource(XAResource xaResource) throws Exception {
        RRSXAResourceFactory xaFactory = (RRSXAResourceFactory) pm.connectorSvc.rrsXAResFactorySvcRef.getService();

        // Make sure that the bundle is active.
        if (xaFactory == null) {
            throw new IllegalStateException("Native service for RRS transactional support is not active or available. Resource delistment is rejected.");
        }

        UOWCurrent currentUOW = (UOWCurrent) pm.connectorSvc.transactionManager;
        UOWCoordinator uowCoord = currentUOW.getUOWCoord();

        // If delisting for cleanup, notify the factory that the resource has been
        // delisted for cleanup with the transaction manager.
        if (!uowCoord.isGlobal()) {
            LocalTransactionCoordinator ltCoord = (LocalTransactionCoordinator) uowCoord;
            if (!ltCoord.isContainerResolved()) {
                ltCoord.delistFromCleanup((OnePhaseXAResource) xaResource);
                xaFactory.delist(uowCoord, xaResource);
            }
        }
    }

    /**
     * Enlists a RRS XA resource with the transaction manager.
     *
     * @param recoveryId The recovery id representing the registration of the resource with
     *            the transaction manager.
     * @param branchCoupling The resource's branch coupling support indicator.
     *
     * @return The XA resource enlisted with the transaction manager.
     *
     * @throws Exception if an error occurred during the enlistment process.
     */
    final XAResource enlistRRSXAResource(int recoveryId, int branchCoupling) throws Exception {
        RRSXAResourceFactory xaFactory = (RRSXAResourceFactory) pm.connectorSvc.rrsXAResFactorySvcRef.getService();

        // Make sure that the bundle is active.
        if (xaFactory == null) {
            throw new IllegalStateException("Native service for RRS transactional support is not active or available. Resource enlistment is rejected.");
        }

        XAResource xaResource = null;
        UOWCurrent currentUOW = (UOWCurrent) pm.connectorSvc.transactionManager;
        UOWCoordinator uowCoord = currentUOW.getUOWCoord();

        // Enlist XA resource.
        if (uowCoord.isGlobal()) {
            // Enlist a 2 phase XA resource in the global transaction.
            xaResource = xaFactory.getTwoPhaseXAResource(uowCoord.getXid());
            pm.connectorSvc.transactionManager.enlist(uowCoord, xaResource, recoveryId, branchCoupling);
        } else {
            // Enlist a one phase XA resource in the local transaction. If enlisting for
            // cleanup, notify the factory that the resource has been enlisted for cleanup
            // with the transaction manager.
            xaResource = xaFactory.getOnePhaseXAResource(uowCoord);
            LocalTransactionCoordinator ltCoord = (LocalTransactionCoordinator) uowCoord;
            if (ltCoord.isContainerResolved()) {
                ltCoord.enlist((OnePhaseXAResource) xaResource);
            } else {
                ltCoord.enlistForCleanup((OnePhaseXAResource) xaResource);
            }

            // Enlist with the native transaction manager (factory).
            xaFactory.enlist(uowCoord, xaResource);
        }

        return xaResource;
    }

    /**
     * Get the string representation of this MCWrapper's current state
     */
    @Override
    public String getStateString() {
        return STATESTRINGS[state];
    }

    /**
     * Set the shared pool coordinator
     */
    @Override
    public void setSharedPoolCoordinator(Object sharedPoolCoordinator) {
        this.sharedPoolCoordinator = sharedPoolCoordinator;
    }

    @Override
    public int getHashMapBucket() {
        return this.hashMapBucket;
    }

    @Override
    public void setHashMapBucket(int hashMapBucket) {
        this.hashMapBucket = hashMapBucket;
    }

    /**
     * Get shared pool coordinator
     */
    @Override
    public Object getSharedPoolCoordinator() {
        return this.sharedPoolCoordinator;
    }

    /**
     * Get the free pool bucket
     */
    @Override
    public Object getSharedPool() {
        return currentSharedPool;
    }

    @Override
    public void setSharedPool(Object sharedPool) {
        currentSharedPool = sharedPool;
    }

    /**
     * Return the connection event listener
     *
     * @return
     */
    public ConnectionEventListener getConnectionEventListener() {
        return eventListener;
    }

    /**
     * Set the pending user data, if the mc.getConnection
     * is successful, we need set the this.userData to
     * this.userDataPending. The switch is done in
     * updateUserDataForReauthentication.
     *
     * @param userDataPending
     */
    @Override
    public void setSupportsReAuth(boolean supportsReauth) {
        _supportsReAuth = supportsReauth;
    }

    /**
     * Get the MCWrapperList
     */
    @Override
    public Object getMCWrapperList() {
        return mcWrapperList;
    }

    /**
     * Set the MCWrapperList
     */
    @Override
    public void setMCWrapperList(Object mcWrapperList) {
        this.mcWrapperList = mcWrapperList;
    }

    /**
     * Get the string representation of this MCWrapper's current TranWrapper usage
     */
    protected final String getTranWrapperString() {
        return TRANWRAPPERSTRINGS[tranWrapperInUse];
    }

    /**
     * Get the integer representation of the MCWrapper's current TranWrapper usage
     */
    protected final int getTranWrapperId() {
        return tranWrapperInUse;
    }

    /**
     * Get MCWrapper's current state
     */
    protected int getState() {
        return state;
    }

    /**
     * Sets the <code>ManagedConnection</code>. Required.
     * The <code>ManagedConnection</code> is required by the <code>MCWrapper</code> and
     * related objects. Once the ManagedConnection has been set, subsequent calls to
     * this method will cause an exception.
     * <p>
     * Handles PMI notification of ManagedConnection creation.
     *
     * @param ManagedConnection
     *
     */
    @Override
    public void setManagedConnection(ManagedConnection newMC) {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "setManagedConnection");
        }

        if (state == MCWrapper.STATE_NEW || state == MCWrapper.STATE_INACTIVE) {

            mc = newMC;
            createdTimeStamp = java.lang.System.currentTimeMillis();
            unusedTimeStamp = createdTimeStamp;
            eventListener = new com.ibm.ejs.j2c.ConnectionEventListener(this);
            mc.addConnectionEventListener(eventListener);

            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Connection created time " + createdTimeStamp + " for mcw " + this.toString());
            }

            /*
             * Put the mc and mcw in the hash map
             */
            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Adding mc and mcw to hashMap " + mc + " in pool manager " + pm.hashCode());
            }

            pm.putMcToMCWMap(mc, this); //  moved here from MCWrapperPool.java

        } else {
            IllegalStateException e = new IllegalStateException("setManagedConnection: illegal state exception. State = " + getStateString() + " MCW = "
                                                                + mcWrapperObject_hexString);
            Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", "setManagedConnection", e);
            throw e;
        }

        state = MCWrapper.STATE_ACTIVE_FREE;

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "setManagedConnection");
        }

    }

    /**
     * Return the managed connection without checking the state.
     */
    @Override
    public ManagedConnection getManagedConnectionWithoutStateCheck() {
        return mc;
    }

    @Override
    public ManagedConnection getManagedConnection() {

        if (state != MCWrapper.STATE_NEW && state != MCWrapper.STATE_INACTIVE) {
            return mc;
        } else {
            IllegalStateException e = new IllegalStateException("getManagedConnection: illegal state exception. State = " + getStateString() + " MCW = "
                                                                + mcWrapperObject_hexString);
            Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", "getManagedConnection", e);
            throw e;
        }

    }

    /**
     * Marks the MCWrapper in use (STATE_ACTIVE_INUSE)
     */
    @Override
    public void markInUse() {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "markInUse");
        }

        if (state != STATE_ACTIVE_FREE) {
            IllegalStateException e = new IllegalStateException("markInUse: illegal state exception. State = " + getStateString() + " MCW = " + mcWrapperObject_hexString);
            Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", "markInUse", e);
            throw e;
        }

        state = MCWrapper.STATE_ACTIVE_INUSE;
        holdTimeStart = System.currentTimeMillis();
        holdStartTimeSet = true;

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "markInUse");
        }

    }

    /**
     * Sets the <code>ConnectionManager</code>. Required.
     * The <code>ConnectionManager</code> must call this method immediately upon
     * receiving the <code>MCWrapper</code> from the <code>PoolManager.reserve</code> call.
     * The <code>ConnectionManager</code> is required by the <code>MCWrapper</code> and
     * related objects.
     *
     * The recovery token is set at this time.
     */
    @FFDCIgnore(NoSuchMethodException.class)
    protected void setConnectionManager(ConnectionManager cm) {

        if (cm == null) {
            IllegalArgumentException e = new IllegalArgumentException("setConnectionManager: illegal argument exception. ConnectionManager is null.");
            Tr.error(tc, "ILLEGAL_ARGUMENT_EXCEPTION_J2CA0080", "setConnectionManager", e);
            throw e;
        }

        qmidenabled = !(mc instanceof WSManagedConnection);
        if (qmidenabled) {
            Class<? extends Object> mcImplClass = ((Object) mc).getClass();
            Integer recoveryToken = null;
            try {
                Method m = mcImplClass.getMethod("getQmid", (Class<?>[]) null);
                String qmid = (String) m.invoke((Object) mc, (Object[]) null);
                recoveryToken = cm.getQMIDRecoveryToken(qmid, pm);
            } catch (NoSuchMethodException nsme) {
                qmidenabled = false;
            } catch (InvocationTargetException ite) {
                qmidenabled = false;
            } catch (IllegalAccessException e) {
                qmidenabled = false;
            } catch (IllegalArgumentException e) {
                qmidenabled = false;
            }
            if (recoveryToken == null) {
                this.recoveryToken = cm.getRecoveryToken();
            } else {
                this.recoveryToken = recoveryToken.intValue();
            }
        } else {
            recoveryToken = cm.getRecoveryToken();
        }

        this.cm = cm;

    }

    /**
     * Get the reference to the Current CM instance associated with this MCWrapper.
     */
    public ConnectionManager getConnectionManager() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Connection manager is " + cm + " for managed connection " + this);
            if (cm == null && pm != null) {
                Tr.debug(this, tc, "Connection pool is " + this.pm.toString());
            }
        }
        if (cm != null) {
            return cm;
        } else {
            IllegalStateException e = new IllegalStateException("ConnectionManager is null");
            Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", "getConnectionManager", e);
            throw e;
        }
    }

    protected int getRecoveryToken() throws ResourceException {
        return recoveryToken;
    }

    public PoolManager getPoolManager() {
        return pm;
    }

    /**
     * Returns its <code>UOWCoordinator</code> instance..
     * If it doesn't have a current instace it will return null.
     *
     * @return UOWCoordinator
     *
     */
    public UOWCoordinator getUOWCoordinator() {
        return uowCoord;
    }

    /**
     * Sets its <code>UOWCoordinator</code> instance..
     * This will need to be set back to null at the completion of the current UOW.
     */
    protected void setUOWCoordinator(UOWCoordinator uowCoordinator) {
        uowCoord = uowCoordinator;
    }

    /**
     * Updates the <code>UOWCoordinator</code> instance.
     * This will be used to retrieve the current UOW and update the instance variable.
     * This will be used to re-initialize the uowCoord at a new transaction boundary.
     * This will need to be set back to null at the completion of the current UOW.
     *
     * @return UOWCoordinator
     */
    protected UOWCoordinator updateUOWCoordinator() {

        UOWCurrent uowCurrent = (UOWCurrent) pm.connectorSvc.transactionManager;
        uowCoord = uowCurrent == null ? null : uowCurrent.getUOWCoord();
        return uowCoord;

    }

    /**
     * Retrieves a <code>XATransactionWrapper</code> for use.
     */
    protected XATransactionWrapper getXATransactionWrapper() throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getXATransactionWrapper");
        }

        if (xaTranWrapper == null) {
            boolean isJDBC41 = false;
            if (mc instanceof WSManagedConnection) {
                if (_managedConnectionFactory instanceof WSManagedConnectionFactory
                    && (((WSManagedConnectionFactory) _managedConnectionFactory).getJDBCRuntimeVersion().compareTo(new Version(4, 1, 0))) >= 0) {
                    isJDBC41 = true;
                }
            }
            if (isJDBC41) {
                xaTranWrapper = new AbortableXATransactionWrapper(this);
            } else {
                xaTranWrapper = new XATransactionWrapper(this);
            }
        }

        // Move the xaTranWrapper.initialize() call outside of the above "xaTranWrapper == null"
        // conditional.  Prior to this change, the xaResource within the xaTranWrapper was not
        // getting re-initialized for MCWrappers that were pulled from the MCWrapperPool.  This
        // call to initialize() for existing xaTranWrappers is safe and quick since we check
        // for an existing xaResource as the first thing in initialize().
        //
        xaTranWrapper.initialize();

        if (state == STATE_ACTIVE_INUSE) {
            state = STATE_TRAN_WRAPPER_INUSE;
            tranWrapperInUse = XATXWRAPPER;
        } else { // state not STATE_ACTIVE_INUSE
            if (state != STATE_TRAN_WRAPPER_INUSE) {
                IllegalStateException e = new IllegalStateException("getXATransactionWrapper: illegal state exception. State = " + getStateString() + " MCW = "
                                                                    + mcWrapperObject_hexString);
                Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", "getXATransactionWrapper", e);
                throw e;
            } else { // state is STATE_TRAN_WRAPPER_INUSE
                if (tranWrapperInUse == NONE) {
                    tranWrapperInUse = XATXWRAPPER;
                } else { //tranWrapperInUse is NOT NONE
                    if (tranWrapperInUse != XATXWRAPPER) {
                        // Need RAS and Trace here.
                        IllegalStateException e = new IllegalStateException("getXATransactionWrapper: illegal transaction state exception. State = "
                                                                            + getTranWrapperString());
                        Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", "getXATransactionWrapper", e);
                        throw e;
                    }
                }
            }
        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "getXATransactionWrapper");
        }

        return xaTranWrapper;
    }

    /**
     * Retrieves a <code>LocalTransactionWrapper</code> for use. This includes creating a
     * new instance if one has not already been associated with this <code>MCWrapper</code>.
     */
    protected LocalTransactionWrapper getLocalTransactionWrapper() throws ResourceException {

        return getLocalTransactionWrapper(false); // not RRS Transacdtional

    }

    /**
     * Retrieves a <code>LocalTransactionWrapper</code> for use. This includes creating a
     * new instance if one has not already been associated with this <code>MCWrapper</code>.
     *
     * @param rrsTransactional Indicates whether RRS transactions are to be supported xxxx
     */
    protected LocalTransactionWrapper getLocalTransactionWrapper(boolean rrsTransactional) throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getLocalTransactionWrapper", rrsTransactional);
        }

        if (localTranWrapper == null) {
            localTranWrapper = new LocalTransactionWrapper(this);
        }

        // Move the localTranWrapper.initialize() call outside of the above "localTranWrapper == null"
        // conditional.  Prior to this change, the localTransaction within the localTranWrapper was not
        // getting re-initialized for MCWrappers that were pulled from the MCWrapperPool.  This
        // call to initialize() for existing localTranWrappers is safe and quick since we check
        // for an existing localTransaction as the first thing in initialize().
        //
        localTranWrapper.initialize();
        localTranWrapper.setRRSTransactional(rrsTransactional);
        tranWrapperInUse = LOCALTXWRAPPER;

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "getLocalTransactionWrapper");
        }

        return localTranWrapper;

    }

    /**
     * Called upon first use of the LocalTransactionWrapper, as determined by the wrapper itself,
     * this method is used to execute (and check) the appropriate state transitions.
     */
    protected void markLocalTransactionWrapperInUse() throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "markLocalTransactionWrapperInUse");
        }

        if (state == STATE_ACTIVE_INUSE) {
            state = STATE_TRAN_WRAPPER_INUSE;
            tranWrapperInUse = LOCALTXWRAPPER;
            /*
             * If we are not in the shared pool we need to move the mcWrapper from
             * the unshared pool to the shared pool. Under normal conditions, this should not
             * be needed. The non-normal condition is when a user starts a user thread and
             * gets a connection then starts a transaction.
             */
            if (!(isInSharedPool() || this.getPoolState() == MCWrapper.ConnectionState_sharedTLSPool)) {
                if (cm.shareable()) {
                    pm.moveMCWrapperFromUnSharedToShared(this, uowCoord);
                }
            }
        } else { // state not STATE_ACTIVE_INUSE
            if (state != STATE_TRAN_WRAPPER_INUSE) {
                IllegalStateException e = new IllegalStateException("markLocalTransactionWrapperInUse: illegal state exception. State = " + getStateString() + " MCW = "
                                                                    + mcWrapperObject_hexString);
                Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", "markLocalTransactionWrapperInUse", e);
                throw e;
            } else { // state is STATE_TRAN_WRAPPER_INUSE
                if (tranWrapperInUse == NONE) {
                    tranWrapperInUse = LOCALTXWRAPPER;
                } else { //tranWrapperInUse is NOT NONE
                    if (tranWrapperInUse != LOCALTXWRAPPER) {
                        // Need RAS and Trace here.
                        IllegalStateException e = new IllegalStateException("markLocalTransactionWrapperInUse: illegal transaction state exception. State = "
                                                                            + getTranWrapperString());
                        Object[] parms = new Object[] { "markLocalTransactionWrapperInUse", e };
                        Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
                        throw e;
                    }
                }
            }
        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "markLocalTransactionWrapperInUse");
        }

        return;

    }

    /**
     * Retrieves a <code>NoTransactionWrapper</code> for use. This includes creating a
     * new instance if one has not already been associated with this <code>MCWrapper</code>.
     */
    protected NoTransactionWrapper getNoTransactionWrapper() {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getNoTransactionWrapper");
        }

        if (noTranWrapper == null) {
            noTranWrapper = new NoTransactionWrapper();
        }

        if (state == STATE_ACTIVE_INUSE) {
            state = STATE_TRAN_WRAPPER_INUSE;
            tranWrapperInUse = NOTXWRAPPER;
        } else { // state not STATE_ACTIVE_INUSE
            if (state != STATE_TRAN_WRAPPER_INUSE) {
                IllegalStateException e = new IllegalStateException("getNoTransactionWrapper: illegal state exception. State = " + getStateString() + " MCW = "
                                                                    + mcWrapperObject_hexString);
                Object[] parms = new Object[] { "getNoTransactionWrapper", e };
                Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
                throw e;
            } else { // state is STATE_TRAN_WRAPPER_INUSE
                if (tranWrapperInUse == NONE) {
                    tranWrapperInUse = NOTXWRAPPER;
                } else { //tranWrapperInUse is NOT NONE
                    if (tranWrapperInUse != NOTXWRAPPER) {
                        // Need RAS and Trace here.
                        IllegalStateException e = new IllegalStateException("getNoTransactionWrapper: illegal transaction state exception. State = "
                                                                            + getTranWrapperString());
                        Object[] parms = new Object[] { "getNoTransactionWrapper", e };
                        Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
                        throw e;
                    }
                }
            }
        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "getNoTransactionWrapper");
        }

        return noTranWrapper;

    }

    /**
     * Retrieves a <code>RRSGlobalTransactionWrapper</code> for use.
     */
    protected RRSGlobalTransactionWrapper getRRSGlobalTransactionWrapper() throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getRRSGlobalTransactionWrapper");
        }

        if (rrsGlobalTranWrapper == null) {
            rrsGlobalTranWrapper = new RRSGlobalTransactionWrapper(this);
        }

        if (state == STATE_ACTIVE_INUSE) {
            state = STATE_TRAN_WRAPPER_INUSE;
            tranWrapperInUse = RRSGLOBALTXWRAPPER;
        } else { // state not STATE_ACTIVE_INUSE

            if (state != STATE_TRAN_WRAPPER_INUSE) {
                IllegalStateException e = new IllegalStateException("getRRSGlobalTransactionWrapper: illegal state exception. State = " + getStateString() + " MCW = "
                                                                    + mcWrapperObject_hexString);
                Object[] parms = new Object[] { "getRRSGlobalTransactionWrapper", e };
                Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
                throw e;
            } else { // state is STATE_TRAN_WRAPPER_INUSE
                if (tranWrapperInUse == NONE) {
                    tranWrapperInUse = RRSGLOBALTXWRAPPER;
                } else { //tranWrapperInUse is NOT NONE
                    if (tranWrapperInUse != RRSGLOBALTXWRAPPER) {
                        // Need RAS and Trace here.
                        IllegalStateException e = new IllegalStateException("getRRSGlobalTransactionWrapper: illegal transaction state exception. State = "
                                                                            + getTranWrapperString());
                        Object[] parms = new Object[] { "getRRSGlobalTransactionWrapper", e };
                        Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
                        throw e;
                    }
                }
            }

        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "getRRSGlobalTransactionWrapper");
        }

        return rrsGlobalTranWrapper;

    }

    /**
     * Retrieves a <code>RRSLocalTransactionWrapper</code> for use.
     */
    protected RRSLocalTransactionWrapper getRRSLocalTransactionWrapper() throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getRRSLocalTransactionWrapper");
        }

        if (rrsLocalTranWrapper == null) {
            rrsLocalTranWrapper = new RRSLocalTransactionWrapper(this);
        }

        tranWrapperInUse = RRSLOCALTXWRAPPER;

        // Deleted code to set the current state.
        // Hold off on setting the state until we register sync

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "getRRSLocalTransactionWrapper", rrsLocalTranWrapper);
        return rrsLocalTranWrapper;

    }

    /**
     * Called upon first use of the RRSLocalTransactionWrapper, as
     * determined by the wrapper itself. This method is used to
     * execute (and check) the appropriate state transitions.
     */
    protected void markRRSLocalTransactionWrapperInUse() throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "markRRSLocalTransactionWrapperInUse");
        }

        if (state == STATE_ACTIVE_INUSE) {
            state = STATE_TRAN_WRAPPER_INUSE;
            tranWrapperInUse = RRSLOCALTXWRAPPER;
        } else { // state not STATE_ACTIVE_INUSE

            if (state != STATE_TRAN_WRAPPER_INUSE) {
                IllegalStateException e = new IllegalStateException("getRRSLocalTransactionWrapper: illegal state exception. State = " + getStateString() + " MCW = "
                                                                    + mcWrapperObject_hexString);
                Object[] parms = new Object[] { "getRRSLocalTransactionWrapper", e };
                Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
                throw e;
            } else { // state is STATE_TRAN_WRAPPER_INUSE
                if (tranWrapperInUse == NONE) {
                    tranWrapperInUse = RRSLOCALTXWRAPPER;
                } else { //tranWrapperInUse is NOT NONE
                    if (tranWrapperInUse != RRSLOCALTXWRAPPER) {
                        // Need RAS and Trace here.
                        IllegalStateException e = new IllegalStateException("getRRSLocalTransactionWrapper: illegal transaction state exception. State = "
                                                                            + getTranWrapperString());
                        Object[] parms = new Object[] { "getRRSLocalTransactionWrapper", e };
                        Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
                        throw e;
                    }
                }
            }

        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "markRRSLocalTransactionWrapperInUse");
        }

        return;

    }

    /**
     * Retrieves the current <code>TranWrapper</code> in use. This includes creating a
     * new instance if one has not already been associated with this <code>MCWrapper</code>.
     */
    protected TranWrapper getCurrentTranWrapper() throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getCurrentTranWrapper");
        }

        // If the MCWrapper has not been completely initialized and we are working with an RA
        // that supports deferred enlistment of transactions, then let's re-initialize for the
        // UOW which we are already involved with.
        // This was a special fix that was put in for JMS Managed Sessions, but it
        // could apply to other internal connection types that might require this support.
        if ((state == STATE_ACTIVE_INUSE) && (gConfigProps.isDynamicEnlistmentSupported())) {
            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "State is STATE_ACTIVE_INUSE, calling initializeForUOW()...");
            }
            cm.initializeForUOW(this, true); // second parm indicates source of call is deferred enlistment.
        }

        if ((state != STATE_TRAN_WRAPPER_INUSE) && (state != STATE_ACTIVE_INUSE)) {
            IllegalStateException e = new IllegalStateException("getCurrentTranWrapper: illegal state exception. State = " + getStateString() + " MCW = "
                                                                + mcWrapperObject_hexString);
            Object[] parms = new Object[] { "getCurrentTranWrapper", e };
            Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
            throw e;
        }

        TranWrapper tranWrapper = null;
        switch (tranWrapperInUse) {
            case XATXWRAPPER:
                tranWrapper = xaTranWrapper;
                break;
            case LOCALTXWRAPPER:
                tranWrapper = localTranWrapper;
                break;
            case NOTXWRAPPER:
                tranWrapper = noTranWrapper;
                break;
            case RRSGLOBALTXWRAPPER:
                tranWrapper = rrsGlobalTranWrapper;
                break;
            case RRSLOCALTXWRAPPER:
                tranWrapper = rrsLocalTranWrapper;
                break;
            default:
                // Need RAS and Trace here.
                IllegalStateException e = new IllegalStateException("getCurrentTranWrapper: illegal transaction state exception. State = "
                                                                    + getTranWrapperString());
                Object[] parms = new Object[] { "getCurrentTranWrapper", e };
                Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
                throw e;
        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "getCurrentTranWrapper", getTranWrapperString());
        }

        return tranWrapper;
    }

    /**
     * Called by the TranWrapper during afterCompletion.
     */
    protected void transactionComplete() {

        if (state != STATE_TRAN_WRAPPER_INUSE) {
            IllegalStateException e = new IllegalStateException("transactionComplete: illegal state exception. State = " + getStateString() + " MCW = "
                                                                + mcWrapperObject_hexString);
            Object[] parms = new Object[] { "transactionComplete", e };
            Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
            throw e;
        }

        state = STATE_ACTIVE_INUSE;
    }

    protected void transactionWrapperComplete() {

        tranWrapperInUse = NONE;
    }

    /**
     * Called by the Connection manager during reassociate to check if an unshared connection
     * is currently involved in a transaction.
     */
    protected boolean involvedInTransaction() {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (state == STATE_TRAN_WRAPPER_INUSE) {

            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "involvedInTransaction: true");
            }

            return true;
        } else {

            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "involvedInTransaction: false");
            }

            return false;
        }

    }

    /*
     *
     * This is a copy of the cleanup() method with several lines of code commented out.
     *
     * For connection thread local storage, we do not want to call mc.cleanup() and several lines
     * of code around this area does not make sense to use.
     */
    public void tlsCleanup() throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "tlsCleanup");
        }

        this.initialRequestStackTrace = null;
        this.clearHandleList();

        // ease state checks for error cleanup code.    if ((state != STATE_ACTIVE_INUSE) && (state != STATE_ACTIVE_FREE)) {
        if ((state == STATE_NEW) || (state == STATE_INACTIVE)) {
            IllegalStateException e = new IllegalStateException("cleanup: illegal state exception. State = " + getStateString() + " MCW = " + mcWrapperObject_hexString);
            Object[] parms = new Object[] { "cleanup", e };
            Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
            throw e;
        }

        try {
            /*
             * DissociateConnection needs to be called before cleanup.
             *
             * Under the original implementation for smart handles, no
             * dissociateConnection was needed because it was cleaned up
             * during the cleanup call on the mc. Now with the new implementation
             * cleanup will not do the dissociateConnection. If a resource
             * adapter is a instanceof DissociatableManagedConnection, we
             * need to call the dissociateConnection
             *
             * Lazy Connection Association Optimization is the new name
             * for smart handles.
             */
            if (isTracingEnabled && tc.isDebugEnabled())
                Tr.debug(this, tc,
                         "InstanceOf DissociatableManagedConnection is " + gConfigProps.isInstanceOfDissociatableManagedConnection() + " In ConnectionManager " + this.toString());

            if (gConfigProps.isInstanceOfDissociatableManagedConnection()) {
                if (isTracingEnabled && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Calling mc.dissociateConnections()");
                ((DissociatableManagedConnection) mc).dissociateConnections();
                if (isTracingEnabled && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Returned from mc.dissociateConnections()");
            }
            if (mc != null && !aborted) {
                mc.cleanup();
            } else if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "mc is null, or mc is aborted, mc.cleanup() not called.", (mc == null));
            }
        } catch (ResourceException e) {

            String localMessage = e.getLocalizedMessage();
            if ((localMessage != null) && localMessage.equals("Skip logging for this failing connection")) {
                /*
                 * If the resource adapter throws a resource exception with the skip logging text,
                 * log this path only when debug is enabled.
                 *
                 * The resource apdater does not want normal logging of this failed managed connection.
                 */
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Connection failed, resource adapter requested skipping failure logging");
                }
                throw e;
            } else {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.cleanup", "706", this);
                /*
                 * We are here means there is an error occured during MC cleanup.
                 * Lets not log a error message, when we clearly know we are attempting to
                 * cleanup a bad connection. At this stage the error message could be misleading.
                 */
                if (!stale && !do_not_reuse_mcw) {
                    Object[] parms = new Object[] { "cleanup", "cleanup", mc, e, gConfigProps.cfName };
                    Tr.error(tc, "MCERROR_J2CA0081", parms);
                } else {
                    if (isTracingEnabled && tc.isDebugEnabled())
                        Tr.debug(this, tc, "got a SCE when doing cleanup on the mc, { mc, e, pmiName}; is:", new Object[] { mc, e, gConfigProps.cfName });
                }
                throw e;
            }

        } catch (Exception e) {

            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.cleanup", "712", this);
            ResourceException re = null;
            if (!stale && !do_not_reuse_mcw) {
                Object[] parms = new Object[] { "cleanup", "cleanup", mc, e, gConfigProps.cfName };
                Tr.error(tc, "MCERROR_J2CA0081", parms);
            } else {
                if (isTracingEnabled && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "got a SCE when doing cleanup on the mc, { mc, e, pmiName}; is:", new Object[] { mc, e, gConfigProps.cfName });
                }
            }
            re = new ResourceException("cleanup: Exception caught");
            re.initCause(e);
            throw re;

        } finally {
            if (mcConnectionCount != 0) {
                for (int i = mcConnectionCount; i > 0; i--) { //couldn't this just be while(mcConnectionCount > 0)
                    decrementHandleCount();
                }
            }

            // reset to STATE_ACTIVE state.
            //  remove  if(pm.unusedTimeoutEnabled){ //we only need to set the
            // unusedTimeStamp if unused timeout
            // is enabled.
            // we must unconditionally reset unusedTimeStamp here.

            unusedTimeStamp = java.lang.System.currentTimeMillis();
            switch (tranWrapperInUse) {
                /*
                 * Cleanup on the tran wrapper. This is not cleanup for
                 * the mc.
                 */
                case XATXWRAPPER:
                    xaTranWrapper.cleanup();
                    break;

                case LOCALTXWRAPPER:
                    localTranWrapper.cleanup();
                    break;

                case NOTXWRAPPER:
                    noTranWrapper.cleanup();
                    break;

                case RRSGLOBALTXWRAPPER:
                    rrsGlobalTranWrapper.cleanup();
                    break;

                case RRSLOCALTXWRAPPER:
                    rrsLocalTranWrapper.cleanup();
                    break;

                default:
                    break;

            }

            mcConnectionCount = 0;
            cm = null;

            boolean isAlreadyFreeActive = false;
            if (state == STATE_ACTIVE_FREE) {
                isAlreadyFreeActive = true;
            }

            state = STATE_ACTIVE_FREE;
            tranWrapperInUse = NONE;
            uowCoord = null;
            holdTimeStart = 0;
            holdStartTimeSet = false;
            // threadId = null; //  keep the thead info for tls connections
            // threadName = null; //  keep the thread name info for tls connections
            totalUseTime = 0;
            currentUseStartTime = 0;
            useStartTimeSet = false;
            totalHoldTime = 0;
            if (!isAlreadyFreeActive) {
                isNotAlreadyFreeActive();
            }
        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "tlsCleanup");
        }

    }

    /**
     * Calls <code>cleanup()</code> on the wrappered <code>ManagedConnection<code>.
     * Also reinitializes its own state such that it may be placed in the PoolManagers
     * pool for reuse. All objects will be retained. <code>cleanup()</code> will
     * as be propogated to all associated objects, such as the transaction wrappers,
     * so that they may reset their state for reuse.
     * <br> This should only be called by the PoolManager.
     * <br><br>
     * PMI notification of freeing the ManagedConnection back to pool.
     * <br><br>
     * unusedTimeStamp will be updated.
     *
     * @exception ResourceException
     */
    @Override
    public void cleanup() throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "cleanup");
        }

        this.initialRequestStackTrace = null;
        this.clearHandleList();

        // ease state checks for error cleanup code.    if ((state != STATE_ACTIVE_INUSE) && (state != STATE_ACTIVE_FREE)) {
        if ((state == STATE_NEW) || (state == STATE_INACTIVE)) {
            IllegalStateException e = new IllegalStateException("cleanup: illegal state exception. State = " + getStateString() + " MCW = " + mcWrapperObject_hexString);
            Object[] parms = new Object[] { "cleanup", e };
            Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
            throw e;
        }

        try {
            /*
             * DissociateConnection needs to be called before cleanup.
             *
             * Under the original implementation for smart handles, no
             * dissociateConnection was needed because it was cleaned up
             * during the cleanup call on the mc. Now with the new implementation
             * cleanup will not do the dissociateConnection. If a resource
             * adapter is a instanceof DissociatableManagedConnection, we
             * need to call the dissociateConnection
             *
             * Lazy Connection Association Optimization is the new name
             * for smart handles.
             */
            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc,
                         "InstanceOf DissociatableManagedConnection is " + gConfigProps.isInstanceOfDissociatableManagedConnection() + " In ConnectionManager " + this.toString());
            }

            if (gConfigProps.isInstanceOfDissociatableManagedConnection()) {
                if (isTracingEnabled && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Calling mc.dissociateConnections()");
                ((DissociatableManagedConnection) mc).dissociateConnections();
                if (isTracingEnabled && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Returned from mc.dissociateConnections()");

            }
            //     if(!cleanupAllReadyCalled) {
            if (mc != null) {
                mc.cleanup();
            } else if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "mc is null, mc.cleanup() not called.");
            }

        } catch (ResourceException e) {

            String localMessage = e.getLocalizedMessage();
            if ((localMessage != null) && localMessage.equals("Skip logging for this failing connection")) {
                /*
                 * If the resource adapter throws a resource exception with the skip logging text,
                 * log this path only when debug is enabled.
                 *
                 * The resource apdater does not want normal logging of this failed managed connection.
                 */
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Connection failed, resource adapter requested skipping failure logging");
                }
                throw e;
            } else {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.cleanup", "706", this);
                /*
                 * We are here means there is an error occured during MC cleanup.
                 * Lets not log a error message, when we clearly know we are attempting to
                 * cleanup a bad connection. At this stage the error message could be misleading.
                 */
                if (!stale && !do_not_reuse_mcw) {
                    Object[] parms = new Object[] { "cleanup", "cleanup", mc, e, gConfigProps.cfName };
                    Tr.error(tc, "MCERROR_J2CA0081", parms);
                } else {
                    if (isTracingEnabled && tc.isDebugEnabled())
                        Tr.debug(this, tc, "got a SCE when doing cleanup on the mc, { mc, e, pmiName}; is:", new Object[] { mc, e, gConfigProps.cfName });
                }
                throw e;
            }

        } catch (Exception e) {

            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.cleanup", "712", this);
            ResourceException re = null;
            if (!stale && !do_not_reuse_mcw) {
                Object[] parms = new Object[] { "cleanup", "cleanup", mc, e, gConfigProps.cfName };
                Tr.error(tc, "MCERROR_J2CA0081", parms);
            } else {
                if (isTracingEnabled && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "got a SCE when doing cleanup on the mc, { mc, e, pmiName}; is:", new Object[] { mc, e, gConfigProps.cfName });
                }
            }
            re = new ResourceException("cleanup: Exception caught");
            re.initCause(e);
            throw re;

        } finally {
            if (mcConnectionCount != 0) {
                for (int i = mcConnectionCount; i > 0; i--) { // couldn't this just be while(mcConnectionCount > 0)
                    decrementHandleCount();
                }
            }

            // reset to STATE_ACTIVE state.
            //  remove  if(pm.unusedTimeoutEnabled){ // we only need to set the
            // unusedTimeStamp if unused timeout
            // is enabled.
            // we must unconditionally reset unusedTimeStamp here.

            unusedTimeStamp = java.lang.System.currentTimeMillis();
            switch (tranWrapperInUse) {
                /*
                 * Cleanup on the tran wrapper. This is not cleanup for
                 * the mc.
                 */
                case XATXWRAPPER:
                    xaTranWrapper.cleanup();
                    break;

                case LOCALTXWRAPPER:
                    localTranWrapper.cleanup();
                    break;

                case NOTXWRAPPER:
                    noTranWrapper.cleanup();
                    break;

                case RRSGLOBALTXWRAPPER:
                    rrsGlobalTranWrapper.cleanup();
                    break;

                case RRSLOCALTXWRAPPER:
                    rrsLocalTranWrapper.cleanup();
                    break;

                default:
                    break;

            }

            mcConnectionCount = 0;
            cm = null;

            boolean isAlreadyFreeActive = false;
            if (state == STATE_ACTIVE_FREE) {
                isAlreadyFreeActive = true;
            }

            state = STATE_ACTIVE_FREE;
            tranWrapperInUse = NONE;
            uowCoord = null;
            holdTimeStart = 0;
            holdStartTimeSet = false;
            threadId = null;
            threadName = null;
            totalUseTime = 0;
            currentUseStartTime = 0;
            useStartTimeSet = false;
            totalHoldTime = 0;
            purgeState = false;
            if (!isAlreadyFreeActive) {
                isNotAlreadyFreeActive();
            }
        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "cleanup");
        }

    }

    void isNotAlreadyFreeActive() {
        return;
    }

    /**
    *
    */
    void processUseAndHoldTime() {

        if (holdStartTimeSet == true) {
            long currentTime = System.currentTimeMillis();
            totalHoldTime = currentTime - holdTimeStart;
        }

    }

    /**
     * Calls <code>destroy()</code> on the wrappered <code>ManagedConnection<code>.
     * Also nulls out its reference to the ManagedConnection and any other connection
     * related variable and resets internal state. <code>destroy()</code> will
     * as be propogated to all associated objects, such as the transaction wrappers,
     * so that they may release any connection related resources.
     * <br><br> This should only be called by the PoolManager.
     * <br><br>
     * Handles PMI notification of ManagedConnection destruction.
     *
     * @exception ResourceException
     */
    @Override
    public void destroy() throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "destroy");
        }

        if (state != STATE_ACTIVE_FREE) { // && state != STATE_ACTIVE_INUSE) {
            //IllegalStateException e = new IllegalStateException("destroy: illegal state exception. State = " + getStateString() + " MCW = " + mcWrapperObject_hexString);
            //- add pmiName to message
            String pmiName = "No longer available";
            if (cm != null) {
                pmiName = gConfigProps.cfName;
            }
            Tr.warning(tc, "ATTEMPT_TO_DESTORY_CONNECTION_IN_USE_J2CA0088", STATESTRINGS[state], pmiName);
            //throw e;
        }

        try {
            if (mc != null) {
                mc.removeConnectionEventListener(eventListener);
            } else if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "mc is null, mc.removeConnectionEventListener() not called.");
            }
        } catch (Exception e) {
            // Eat this exception since we are destroying the connection anyway.
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.destroy", "782", this);
            String pmiName = "No longer available";
            if (cm != null) {
                pmiName = gConfigProps.cfName;
            }
            if (!stale && !do_not_reuse_mcw) {
                Object[] parms = new Object[] { "destroy",
                                                "removeConnectionEventListener", mc, e, pmiName };
                Tr.error(tc, "MCERROR_J2CA0081", parms);
            } else {

                if (isTracingEnabled && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "got a SCE when doing removeConnectionEventListener on the mc, { mc, e, pmiName}; is:", new Object[] { mc, e, pmiName });
                }

            }
        }

        try {
            if (mc != null) {
                mc.destroy();
            } else if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "mc is null, mc.destroy() not called.");
            }
        } catch (ResourceException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.destroy", "791", this);
            String pmiName = "No longer available";
            if (cm != null) {
                pmiName = gConfigProps.cfName;
            }
            if (!stale && !do_not_reuse_mcw) {
                Object[] parms = new Object[] { "destroy", "destroy", mc, e, pmiName };
                Tr.error(tc, "MCERROR_J2CA0081", parms);
            } else {

                if (isTracingEnabled && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "got a SCE when doing destroy on the mc, { mc, e, pmiName}; is:", new Object[] { mc, e, pmiName });
                }

            }

            throw e;
        } catch (Exception e) {

            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.destroy", "797", this);
            ResourceException re = null;
            String pmiName = "No longer available";
            if (cm != null) {
                pmiName = gConfigProps.cfName;
            }
            if (!stale && !do_not_reuse_mcw) {
                Object[] parms = new Object[] { "destroy", "destroy", mc, e, pmiName };
                Tr.error(tc, "MCERROR_J2CA0081", parms);
            } else {

                if (isTracingEnabled && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "got a SCE when doing destroy on the mc, { mc, e, pmiName}; is:", new Object[] { mc, e, pmiName });
                }

            }
            re = new ResourceException("destroy: Exception caught");
            re.initCause(e);
            throw re;

        } finally {
            // The following call is added for the case when destroy is called without cleanup.
            for (int i = mcConnectionCount; i > 0; i--) {
                decrementHandleCount();
            }

            mc = null;

            try {
                if (xaTranWrapper != null && !isMCAborted()) {
                    xaTranWrapper.releaseResources();
                }
                if (localTranWrapper != null) {
                    localTranWrapper.releaseResources();
                }
                if (noTranWrapper != null) {
                    noTranWrapper.releaseResources();
                }
                if (rrsGlobalTranWrapper != null) {
                    rrsGlobalTranWrapper.releaseResources();
                }
                if (rrsLocalTranWrapper != null) {
                    rrsLocalTranWrapper.releaseResources();
                }
            } catch (Exception e) {

                switch (tranWrapperInUse) {

                    case XATXWRAPPER:
                        com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.destroy", "814", this);
                        if (isTracingEnabled && tc.isDebugEnabled()) {
                            String pmiName = "No longer available";
                            if (cm != null) {
                                pmiName = gConfigProps.cfName;
                            }
                            Tr.debug(this, tc, "destroy: xaTranWrapper.releaseResources() call for resource pool " + pmiName + " failed with exception", e);
                        }
                        break;

                    case LOCALTXWRAPPER:
                        com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.destroy", "823", this);
                        if (isTracingEnabled && tc.isDebugEnabled()) {
                            String pmiName = "No longer available";
                            if (cm != null) {
                                pmiName = gConfigProps.cfName;
                            }
                            Tr.debug(this, tc, "destroy: localTranWrapper.releaseResources() call for resource pool " + pmiName + " failed with exception", e);
                        }
                        break;

                    case NOTXWRAPPER:
                        com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.destroy", "832", this);
                        if (isTracingEnabled && tc.isDebugEnabled()) {
                            String pmiName = "No longer available";
                            if (cm != null) {
                                pmiName = gConfigProps.cfName;
                            }
                            Tr.debug(this, tc, "destroy: noTranWrapper.releaseResources() call for resource pool " + pmiName + " failed with exception", e);
                        }
                        break;

                    case RRSGLOBALTXWRAPPER:
                        com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.destroy", "825", this);
                        if (isTracingEnabled && tc.isDebugEnabled()) {
                            String pmiName = "No longer available";
                            if (cm != null) {
                                pmiName = gConfigProps.cfName;
                            }
                            Tr.debug(this, tc, "destroy: rrsGlobalTranWrapper.releaseResources() call for resource pool " + pmiName + " failed with exception", e);
                        }
                        break;

                    case RRSLOCALTXWRAPPER:
                        com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.destroy", "827", this);
                        if (isTracingEnabled && tc.isDebugEnabled()) {
                            String pmiName = "No longer available";
                            if (cm != null) {
                                pmiName = gConfigProps.cfName;
                            }
                            Tr.debug(this, tc, "destroy: rrsLocalTranWrapper.releaseResources() call for resource pool " + pmiName + " failed with exception", e);
                        }
                        break;

                    default:
                        break;

                }

            }

            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Resetting stale, tranFailed, and _transactionErrorOccurred flags");
            }

            stale = false;
            _transactionErrorOccurred = false;

            state = STATE_INACTIVE;
            destroyState = false;
            purgeState = false;

        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "destroy");
        }

    }

    /**
     * Retrieves a new connection handle from the <code>ManagedConnection</code>.
     * <p>
     * Also keeps track of the PMI data relative to the number of handles in use.
     *
     * @param subj Security Subject
     * @param cri ConnectionRequestInfo object.
     * @return Connection handle.
     * @exception ResourceException
     */
    protected java.lang.Object getConnection(Subject subj, ConnectionRequestInfo cri) throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getConnection");
        }

        Object connection = null;

        // Get the a Connection from the ManagedConnection to return to our caller, and increment the
        // number of open connections for this ManagedConnection.
        try {
            connection = mc.getConnection(subj, cri);
            if (_supportsReAuth) {
                /*
                 * If we have a userDataPending userData, we need
                 * to set the current userData to the userDataPending
                 * value. Right now we are doing this for every
                 * matchManagedConnection (MMC), since we do not know which
                 * resource adapters support reauthentication. The MMC
                 * does not reauthenticate, the reauthentication is done
                 * on the mc.getConnection. This is the reason for doing
                 * the update after the the mc.getConnection is successful.
                 * If the mc.getConnection fails, we have a better chance
                 * to recover corrently with the current userData values
                 * in the PoolManager code.
                 */
                _subject = subj;
                _cri = cri;
                hashMapBucket = _hashMapBucketReAuth;
            }
            incrementHandleCount();
            // Need PMI Call here.
        } catch (SharingViolationException e) {
            // If there is a sharing violation, it means that there is already at LEAST one connection
            // handle out.  Therefore we can't release the ManagedConnection yet.  Just log and rethrow.
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.getConnection", "1677", this);
            String cfName = "No longer available";
            if (cm != null) {
                cfName = gConfigProps.cfName;
            }
            Tr.error(tc, "FAILED_CONNECTION_J2CA0021", new Object[] { e, cfName });
            throw e;
        } catch (ResourceException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.getConnection", "873", this);
            String cfName = "No longer available";
            if (cm != null) {
                cfName = gConfigProps.cfName;
            }
            Tr.error(tc, "FAILED_CONNECTION_J2CA0021", new Object[] { e, cfName });
            /*
             * If the Resource Adapter throws a ResourceException and we are not in a
             * transaction and there are no other connections open on the ManagedConnection,
             * return the ManagedConnection to the pool before
             * rethrowing the exception. The ManagedConnection is probably OK - the exception
             * may only be a logon failure or similar so the MC shouldn't be 'lost'.
             *
             * If we are in a transaction, just throw the exception. Assume we will cleanup during
             * the aftercompletion call on the tran wrapper.
             */
            /*
             * if ((uowCoord == null) && (mcConnectionCount == 0)) {
             * try {
             * pm.release(this, uowCoord);
             * }
             * catch ( Exception exp ) { // don't rethrow, already on exception path
             * com.ibm.ws.ffdc.FFDCFilter.processException(exp, "com.ibm.ejs.j2c.MCWrapper.getConnection", "893", this);
             * // add pmiName to message
             * Tr.error(tc,"FAILED_CONNECTION_RELEASE_J2CA0022", new Object[] {exp, pmiName});
             * }
             * } end /
             */
            throw e;
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.getConnection", "901", this);
            String cfName = "No longer available";
            if (cm != null) {
                cfName = gConfigProps.cfName;
            }
            Tr.error(tc, "FAILED_CONNECTION_J2CA0021", new Object[] { e, cfName });
            /*
             * If the Resource Adapter throws an Exception and we are not in a
             * transaction and there are no other connections open on the ManagedConnection,
             * return the ManagedConnection to the pool before
             * rethrowing the exception. The ManagedConnection is probably OK - the exception
             * may only be a logon failure or similar so the MC shouldn't be 'lost'.
             *
             * If we are in a transaction, just throw the exception. Assume we will cleanup during
             * the aftercompletion call on the tran wrapper.
             */
            /*
             * if ((uowCoord == null) && (mcConnectionCount == 0)) {
             * try {
             * pm.release(this, uowCoord);
             * }
             * catch ( Exception exp ) { // don't rethrow, already on exception path
             * com.ibm.ws.ffdc.FFDCFilter.processException(exp, "com.ibm.ejs.j2c.MCWrapper.getConnection", "921", this);
             * //add pmiName to message
             * Tr.error(tc,"FAILED_CONNECTION_RELEASE_J2CA0022", new Object[] {exp, pmiName});
             * }
             * }/
             */

            // See if per chance the component-managed auth alias used on the first lookup has become invalid:
            String remsg = "";
            String xpathId = gConfigProps.getXpathId();
            if (xpathId != null) {
                final Object originalAlias = J2CUtilityClass.pmiNameToCompAlias.get(xpathId);
                if ((originalAlias != null) && (!originalAlias.equals(""))) {

                    remsg = "Component-managed authentication alias " + originalAlias +
                            " for connection factory or datasource " + cfName +
                            " is invalid.  It may be necessary to re-start the server(s) for " +
                            " previous configuration changes to take effect.";

                } // end originalAlias not null or blank

            } // end pmiName != null

            ResourceException re = new ResourceException(remsg);
            re.initCause(e);
            throw re;

        } // end catch Exception e

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "getConnection", connection);
        }

        return connection;

    }

    /**
     * Associates the <code>Connection</code> handle which is passed in with the
     * <code>ManagedConnection</code> which this object wraps. The from
     * <code>MCWrapper</code> is needed so that the PMI information about number
     * of handles in use can be decremented.
     * <p>
     * If the fromWrapper is null, then this indicates that Smart Handle support is
     * in effect and there is no previous wrapper to decrement. We took care of
     * the decrement during the parkHandle processing.
     * <p>
     * It is expected that this method will be used by the ConnectionManager
     * in processing a reassociate call or an associateConnection call.
     *
     * @param handle Connection handle to associate with this wrappers ManagedConnection.
     * @param fromWrapper MCWrapper which this handle is currently associated with.
     *
     * @exception ResourceException if the connection association fails.
     */
    protected void associateConnection(Object handle, MCWrapper fromWrapper) throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "associateConnection");
        }

        try {
            mc.associateConnection(handle);
            if (fromWrapper != null) {
                fromWrapper.decrementHandleCount();
            }
            incrementHandleCount();
        } catch (ResourceException e) {

            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.associateConnection", "965", this);
            String pmiName = "No longer available";
            if (cm != null) {
                pmiName = gConfigProps.cfName;
            }
            Tr.error(tc, "FAILED_TO_ASSOCIATE_CONNECTION_J2CA0058", new Object[] { handle, mc, e, pmiName });

            if (isTracingEnabled && tc.isEntryEnabled()) {
                Tr.exit(this, tc, "associateConnection", "Caught a ResourceException exception from mc.associateConnection()");
            }

            throw e;

        } catch (Exception e) {

            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.associateConnection", "972", this);
            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "associateConnection: Caught a Non resource exception from mc.associateConnection()");
            }
            String cfName = "No longer available";
            if (cm != null) {
                cfName = gConfigProps.cfName;
            }
            Tr.error(tc, "FAILED_CONNECTION_J2CA0021", new Object[] { e, cfName });
            ResourceException re = new ResourceException("associateConnection: Failed to associate connection. Exception caught.");
            re.initCause(re);
            throw re;

        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "associateConnection");
        }

    }

    /**
     * Releases itself to the <code>PoolManager</code>.
     * Since this object already knows it's PM it can release itself.
     */
    protected void releaseToPoolManager() throws ResourceException {

        if (inSharedPool) {
            pm.release(this, uowCoord);
        } else {
            // Non-shareable connections are Always reserved with a null coordinator,
            //  and thus must be release with one regardless of whether or not the
            //  current uowCoord is non-null.
            pm.release(this, null);
        }

    }

    /*
     * This method is called by the connectionEventListener when a connection error occurs.
     * This is method will, when possible, and as much as possible, initiate cleanup of all
     * stuff associated with this MCWrapper.
     */

    protected void connectionErrorOccurred() {
        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "connectionErrorOccurred");
        }
        connectionErrorOccurred(null);
        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "connectionErrorOccurred");
        }
    }

    protected void connectionErrorOccurred(ConnectionEvent connectionEvent) {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();
        if (poolState.get() == 50) {
            /*
             * When a resource adapter uses connectionErrorOccurred during a
             * createManagedConnection, matchManagedConnection or getConnection
             * we can not reuse the mcw. Reuse of the mcw may results in duplicate
             * entries in the MCWrapperListPool. To be safe, all mcw connectionErrorOccurred
             * events will be marked not to be reused. Note, the use of connectionErrorOccurred before
             * the managed connection is inuse, may not be spec compliant, but the customer for this request has
             * stated their resource adapter worked with this behavior on 5.0.1.
             */
            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "A connection error occurred is being called during matchManagedConnection for mcw " + this + " " +
                                   "Attempting to cleanup and destroy this connection cleanly");
            }
            do_not_reuse_mcw = true;

        } else {
            /*
             * We remove all trace of this ManagedConnection and its associated transaction
             * (if any) here. If the transaction gets completed the afterCompletion processing
             * won't find anything to do because of the cleanup performed here.
             */

            //if (involvedInTransaction()) {
            try {
                // Q:  Why aren't we checking for NULL_COORDINATOR before calling this method???
                // A:  Because PoolManager needs to check find it where ever it is.  A NULL_COORDINATOR
                //      object is legitimate for sharing.  If we pass in null, it tell the PM to
                //      not bother looking in the shared pool.

                // NOTE:  This should be the last thing done in this method.
                /*
                 * Adding check for id 51. We will skip the
                 * fatalErrorNotification since this event expects only the failing
                 * connection to be destroyed.
                 */
                if (!stale) { // Check for stale in case we have already been here before.
                    if (connectionEvent.getId() != com.ibm.websphere.j2c.ConnectionEvent.SINGLE_CONNECTION_ERROR_OCCURRED) {
                        pm.fatalErrorNotification(_managedConnectionFactory, this, uowCoord);
                    } else {
                        // Need to set stale since we skipped fatal error code.
                        this.markStale();
                    }
                }
            } catch (Exception e) {
                // Nothing to do here. PoolManager has already logged it.
                // Since we're in the cleanup stage, there is no reason to surface a Runtime exception to the Resource Adapter.
                if (isTracingEnabled && tc.isDebugEnabled()) {
                    String pmiName = "No longer available";
                    if (cm != null) {
                        pmiName = gConfigProps.cfName;
                    }
                    Tr.debug(this, tc, "connectionErrorOccurred: PoolManager.fatalErrorNotification call on resource pool "
                                       + pmiName + " failed with exception",
                             e);
                }
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.connectionErrorOccurred", "253", this);
            }

            /*
             * Added the following code to handle NOTXWRAPPER connection
             * or RRSLOCALTXWRAPPER connection
             * that need to be cleaned up now because there afterConpletion
             * will not be called.
             *
             * We can cleanup the connection if it has never been used in a transaction,
             * wrapperId == MCWrapper.NONE. This can only happen if the connection has been accessed from
             * a user created thread, and this is a common occurance for JMS so we have enabled this optimization.
             * if a connection error occurs before the tran wrapper is used, then we need
             * to cleanup the connection right away, we'll never be notified by the transaction service.
             * So, if the state is not TRAN_WRAPPER_INUSE or INACTIVE the connection will be cleaned up. (there
             * should be no way for an inactive connection to encounter a connection error)
             *
             * Currently, we are going to continue to use same transaction processing.
             * This may change and the code to unconditionally remove
             * this connection even if its in a transaction should be added here.
             */
            if ((this.getTranWrapperId() == MCWrapper.NOTXWRAPPER) ||
                (this.getTranWrapperId() == MCWrapper.RRSLOCALTXWRAPPER) ||
                (this.getTranWrapperId() == MCWrapper.NONE) ||
                (state != STATE_TRAN_WRAPPER_INUSE && state != STATE_INACTIVE)) {

                if (this.getTranWrapperId() == MCWrapper.NOTXWRAPPER ||
                    this.getTranWrapperId() == MCWrapper.RRSLOCALTXWRAPPER) {

                    if (isTracingEnabled && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Calling transactionComplete tranWrapperID = " + getTranWrapperString());
                    }

                    this.transactionComplete();

                }

                //  ConnectionHandleManager mychm = ConnectionHandleManager.getConnectionHandleManager();
                //  mychm.removeHandles(this,connectionEvent.getConnectionHandle());

                this.clearHandleList();
                try {
                    if (state != STATE_INACTIVE) { // If this MCWrapper is inactive then it has already been destroyed, likely because
                        //a connection error occurred event was called on this connection while it was in the free pool.  Skipping a second
                        //cleanup and destroy so the connection count is not double decremented and we don't get an IllegalStateException.
                        this.releaseToPoolManager();
                    } else {
                        if (isTracingEnabled && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Skipping release since the MCWrapper state was already STATE_INACTIVE");
                        }
                    }
                } catch (Exception ex) {
                    // Nothing to do here. PoolManager has already logged it.
                    // Since we are in cleanup mode, we will not surface a Runtime exception to the ResourceAdapter
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ejs.j2c.MCWrapper.connectionErrorOccurred", "197", this);

                    if (isTracingEnabled && tc.isDebugEnabled()) {
                        Tr.debug(this, tc,
                                 "connectionClosed: Closing connection in pool "
                                           + gConfigProps.cfName
                                           + " caught exception, but will continue processing: ",
                                 ex);
                    }
                }
            } else {
                if (isTracingEnabled && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Cannot release MCWrapper to the pool, waiting for transaction to complete");
                }
            }
        }
    }

    /**
     * Marks this connection for destruction. Used as part of purging the entire
     * connection pool. Called on connections which are in use at the time of
     * the pool being purged.
     * <p>
     * If this object is marked stale when cleanup() is called, a call to destroy() will
     * happen under the covers.
     * <p>
     */
    @Override
    public void markStale() {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "markStale");
        }

        // This update must be thread safe.  I'm assuming here that a boolean assignment
        //  is atomic.  If that's incorrect, then we'll need to add a synchronize(stale)
        //  gaurd here and on the isStale method.

        stale = true;

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "markStale");
        }

    }

    @Override
    public boolean isStale() {
        return stale;
    }

    @Override
    public long getCreatedTimeStamp() {
        return createdTimeStamp;
    }

    @Override
    public long getUnusedTimeStamp() {
        return unusedTimeStamp;
    }

    /**
     * Fatal Error Notification Occurred
     *
     * When a connection is created it will be assigned a
     * free pools fatal error notification value + 1. When a fatal
     * error occurs, the free pools fatal error notification value is
     * incremented by 1. Any connection with a fatal error
     * notification value less than or = to free pools fatal error
     * notification value will be cleaned up and destroyed
     */
    @Override
    public boolean hasFatalErrorNotificationOccurred(int fatalErrorNotificationTime) {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        /*
         * I have changed this from using a long based on a currentTimeMillis
         * to an int value.
         *
         * By using an int we will perform better and synchronization is
         * not required.
         */
        if (fatalErrorValue > fatalErrorNotificationTime) {
            return false;
        } else {

            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "hasFatalErrorNotificationOccurred is true");
            }

            return true;

        }

    }

    /**
     *
     * Aged Timeout
     * Aged Timeout is the approximate interval (or age of a ManagedConnection), in seconds, before
     * a ManagedConnection is discarded. The default value is 0 which will allow active ManagedConnections
     * to remain in the pool indefinitely. The recommended way to disable the pool maintenence thread is to
     * set Reap Time to 0, in which case Aged Timeout and Unused Timeout will be ignored. However if Aged
     * Timeout or Unused Timeout are set to 0, the pool maintenence thread will run, but only
     * ManagedConnections which timeout due to non-zero Connection Timeout values will be discarded.
     * Aged Timeout should be set higher than Reap Timeout for optimal performance.
     *
     * For example if Aged Timeout is set to 1200, and Reap Time is not 0, any ManagedConnection that has been
     * in use for 20 minutes will be discarded from the pool.
     *
     */
    @Override
    public boolean hasAgedTimedOut(long timeoutValue) {
        long currentTime = java.lang.System.currentTimeMillis();
        long timeDifference = currentTime - createdTimeStamp;
        if (timeDifference >= timeoutValue) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "hasAgedTimedOut is true");
                Tr.debug(this, tc, "The created time was " + new Date(createdTimeStamp) + " and the current time is " + new Date(currentTime));
                Tr.debug(this, tc, "Time difference " + timeDifference + " is greater than or equal to the aged timeout " + timeoutValue);
            }
            return true;
        } else {
            return false;
        }
    }

    /*
     * We need to reset the idle time out value to keep
     * this connection in the pool when
     * the prepopulate feature is enabled.
     */
    public void resetIdleTimeOut() {
        unusedTimeStamp = java.lang.System.currentTimeMillis();
    }

    /**
     * Unused Timeout
     * Unused Timeout is the approximate interval in seconds after which an unused,or idle, connection
     * is discarded. The default value is 0 which allows unused connections to remain in the pool
     * indefinitely. The recommended way to disable the pool maintenence thread is to set Reap Time
     * to 0, in which case Unused Timeout and Aged Timeout will be ignored. However if Unused Timeout
     * and Aged Timeout are set to 0, the pool maintenence thread will run, but only ManagedConnections
     * which timeout due to non-zero timeout values will be discarded. Unused Timeout should be set higher
     * than Reap Timeout for optimal performance. In addition, unused ManagedConnections will only be
     * discarded if the current number of connection not in use exceeds the Min Connections setting.
     * For example if unused timeout is set to 120, and the pool maintenence thread is enabled (Reap
     * Time is not 0), any managed connection that has been unused for two minutes will be discarded
     *
     * Note that accuracy of this timeout, as well as performance, is affect by the Reap Time. See
     * Reap Time for more information.
     *
     */
    @Override
    public boolean hasIdleTimedOut(long timeoutValue) {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        boolean booleanValue = false;
        long currentTime = java.lang.System.currentTimeMillis();
        long timeDifference = currentTime - unusedTimeStamp;
        if (timeDifference > timeoutValue) {
            booleanValue = true;

            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "hasIdleTimedOut is " + booleanValue);
            }

        }

        return booleanValue;

    }

    @Override
    public int getHandleCount() {
        return mcConnectionCount;
    }

    @Override
    public void decrementHandleCount() {
        mcConnectionCount--;
    }

    @Override
    public void incrementHandleCount() {
        mcConnectionCount++;
    }

    @Override
    public void clearMCWrapper() {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        pm = null;

        if (isTracingEnabled && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "ConnectionManager nulled PM ref");
        }

    }

    /*
     * Since we're convinced the MCWrapper instances are getting
     * garbage collected, we don't need the performance drag of calling
     * finalize(). It was only for debug purposes initially.
     *
     * protected void finalize() throws Throwable {
     *
     * super.finalize();
     *
     * if ( com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
     * Tr.debug(this, tc, "MCWrapper garbage collected");
     * }
     *
     * }
     */

    @Override
    public String toString() {

        StringBuffer buf = new StringBuffer(256);

        /*
         * Added the isStale check to mark stale connections in the
         * trace for easier problem determination.
         */
        if (isStale()) {
            buf.append("[STALE]  ");
        }

        buf.append("MCWrapper id ");
        buf.append(mcWrapperObject_hexString);
        buf.append("  Managed connection ");
        buf.append(mc);
        buf.append("  State:");
        buf.append(getStateString());
        if (threadId != null) {
            buf.append(" Thread Id: ");
            buf.append(threadId);
            buf.append(" Thread Name: ");
            buf.append(threadName);
            buf.append(" Connections being held ");
            buf.append(mcConnectionCount);
        }
        buf.append(nl);

        return buf.toString();
    }

    @Override
    public void setParkedWrapper(boolean parkedWrapper) {
        isParkedWrapper = parkedWrapper;
    }

    @Override
    public boolean isParkedWrapper() {
        return isParkedWrapper;
    }

    /**
     * Sets the hashMapBucket ReAuth
     *
     * @param hashMapBucket
     */
    @Override
    public void setHashMapBucketReAuth(int hashMapBucketReAuth) {
        _hashMapBucketReAuth = hashMapBucketReAuth;
    }

    /**
     * Gets the hashMapBucket
     */
    @Override
    public int getHashMapBucketReAuth() {
        return _hashMapBucketReAuth;
    }

    /**
     * getSubject
     */
    @Override
    public void setSubject(Subject subject) {
        _subject = subject;
    }

    /**
     * getSubject
     */
    @Override
    public Subject getSubject() {
        return _subject;
    }

    /**
     * setCRI
     */
    @Override
    public void setCRI(ConnectionRequestInfo cri) {
        _cri = cri;
    }

    /**
     * getCRI
     */
    @Override
    public ConnectionRequestInfo getCRI() {
        return _cri;
    }

    /**
     * @return
     */
    @Override
    public boolean isLogWriterSet() {
        return logWriterSet;
    }

    /**
     * @param b
     */
    @Override
    public void setLogWriterSet(boolean b) {
        logWriterSet = b;
    }

    /**
     * This will indicate where the connection is in the pool
     *
     * Valid states are:
     * 0 = Not in any pool, currently in transition.
     * 1 = In free pool
     * 2 = In shared pool
     * 3 = In unshared pool
     * 4 = In waiter pool
     *
     * @return
     */
    @Override
    public int getPoolState() {
        return poolState.get();
    }

    /**
     * This will indicate where the connection is in the pool
     *
     * Valid states are:
     * 0 = Not in any pool, currently in transition.
     * 1 = In free pool
     * 2 = In shared pool
     * 3 = In unshared pool
     * 4 = In waiter pool
     * 9 = parked connection
     *
     * @param i
     */
    @Override
    public void setPoolState(int i) {
        if (pm.gConfigProps.callResourceAdapterStatMethods) {
            if (poolState.get() == 0) {
                /*
                 * If current state is 0, we are a new connection, or we are in
                 * transition to one of the pools.
                 */
                if (i == 1) {
                    /*
                     * This connection is moving from new/transaction to free
                     */
                    synchronized (pm.gConfigProps.numberOfFreeConnectionsLockObject) {
                        ++pm.gConfigProps.numberOfFreeConnections;
                    }
                }
                if (i == 2) {
                    /*
                     * This connection is moving from new/transaction to shared
                     */
                    synchronized (pm.gConfigProps.numberOfInuseConnectionsLockObject) {
                        ++pm.gConfigProps.numberOfInuseConnections;
                    }
                }
                if (i == 3) {
                    /*
                     * This connection is moving from new/transaction to unshared
                     */
                    synchronized (pm.gConfigProps.numberOfInuseConnectionsLockObject) {
                        ++pm.gConfigProps.numberOfInuseConnections;
                    }
                }
                if (i == 4) {
                    /*
                     * This connection is moving from new/transaction to waiter
                     */
                    // do nothing here
                    //++pm.gConfigProps.numberOfInuseConnections;
                }
            }
            if (poolState.get() == 1) {
                /*
                 * We are in the free pool, moving to inuse.
                 */
                if (i == 0) {
                    /*
                     * This connection is moving from free to transition
                     */
                    synchronized (pm.gConfigProps.numberOfFreeConnectionsLockObject) {
                        --pm.gConfigProps.numberOfFreeConnections;
                    }
                }
                if (i == 2) {
                    /*
                     * This connection is moving from free to shared
                     */
                    synchronized (pm.gConfigProps.numberOfFreeConnectionsLockObject) {
                        --pm.gConfigProps.numberOfFreeConnections;
                    }
                    synchronized (pm.gConfigProps.numberOfInuseConnectionsLockObject) {
                        ++pm.gConfigProps.numberOfInuseConnections;
                    }
                }
                if (i == 3) {
                    /*
                     * This connection is moving from free to unshared
                     */
                    synchronized (pm.gConfigProps.numberOfFreeConnectionsLockObject) {
                        --pm.gConfigProps.numberOfFreeConnections;
                    }
                    synchronized (pm.gConfigProps.numberOfInuseConnectionsLockObject) {
                        ++pm.gConfigProps.numberOfInuseConnections;
                    }
                }
                if (i == 4) {
                    /*
                     * This connection is moving from free to waiter
                     */
                    synchronized (pm.gConfigProps.numberOfFreeConnectionsLockObject) {
                        --pm.gConfigProps.numberOfFreeConnections;
                    }
                    //--pm.gConfigProps.numberOfFreeConnections;
                    //++pm.gConfigProps.numberOfInuseConnections;
                }

            }
            if (poolState.get() == 2) {
                /*
                 * We are in the shared pool, moving to free/transition.
                 */
                if (i == 0) {
                    /*
                     * This connection is moving from shared to transition
                     */
                    synchronized (pm.gConfigProps.numberOfInuseConnectionsLockObject) {
                        --pm.gConfigProps.numberOfInuseConnections;
                    }
                }
                if (i == 1) {
                    /*
                     * This connection is moving from shared to free
                     */
                    synchronized (pm.gConfigProps.numberOfFreeConnectionsLockObject) {
                        ++pm.gConfigProps.numberOfFreeConnections;
                    }
                    synchronized (pm.gConfigProps.numberOfInuseConnectionsLockObject) {
                        --pm.gConfigProps.numberOfInuseConnections;
                    }
                }
                if (i == 3) {
                    /*
                     * This connection is moving from shared to unshared
                     */
                    // do nothing
                    //--pm.gConfigProps.numberOfFreeConnections;
                    //++pm.gConfigProps.numberOfInuseConnections;
                }
                if (i == 4) {
                    /*
                     * This connection is moving from shared to waiter
                     */
                    synchronized (pm.gConfigProps.numberOfInuseConnectionsLockObject) {
                        --pm.gConfigProps.numberOfInuseConnections;
                    }
                    //--pm.gConfigProps.numberOfFreeConnections;
                    //++pm.gConfigProps.numberOfInuseConnections;
                }

            }
            if (poolState.get() == 3) {
                /*
                 * We are in the unshared pool, moving to free/transition.
                 */
                if (i == 0) {
                    /*
                     * This connection is moving from unshared to transition
                     */
                    synchronized (pm.gConfigProps.numberOfInuseConnectionsLockObject) {
                        --pm.gConfigProps.numberOfInuseConnections;
                    }
                }
                if (i == 1) {
                    /*
                     * This connection is moving from unshared to free
                     */
                    synchronized (pm.gConfigProps.numberOfFreeConnectionsLockObject) {
                        ++pm.gConfigProps.numberOfFreeConnections;
                    }
                    synchronized (pm.gConfigProps.numberOfInuseConnectionsLockObject) {
                        --pm.gConfigProps.numberOfInuseConnections;
                    }
                }
                if (i == 2) {
                    /*
                     * This connection is moving from unshared to shared
                     */
                    // do nothing
                    //--pm.gConfigProps.numberOfFreeConnections;
                    //++pm.gConfigProps.numberOfInuseConnections;
                }
                if (i == 4) {
                    /*
                     * This connection is moving from unshared to waiter
                     */
                    synchronized (pm.gConfigProps.numberOfInuseConnectionsLockObject) {
                        --pm.gConfigProps.numberOfInuseConnections;
                    }
                    //--pm.gConfigProps.numberOfFreeConnections;
                    //++pm.gConfigProps.numberOfInuseConnections;
                }

            }
            if (poolState.get() == 4) {
                /*
                 * We are in the waiter pool, moving to free/transition.
                 */
                if (i == 0) {
                    /*
                     * This connection is moving from waiter to transition
                     */
                    // do nothing
                    //--pm.gConfigProps.numberOfInuseConnections;
                }
                if (i == 1) {
                    /*
                     * This connection is moving from waiter to free
                     */
                    synchronized (pm.gConfigProps.numberOfFreeConnectionsLockObject) {
                        ++pm.gConfigProps.numberOfFreeConnections;
                    }
                    //--pm.gConfigProps.numberOfInuseConnections;
                }
                if (i == 2) {
                    /*
                     * This connection is moving from waiter to shared
                     */
                    // do nothing
                    //--pm.gConfigProps.numberOfFreeConnections;
                    synchronized (pm.gConfigProps.numberOfInuseConnectionsLockObject) {
                        ++pm.gConfigProps.numberOfInuseConnections;
                    }
                }
                if (i == 3) {
                    /*
                     * This connection is moving from waiter to unshared
                     */
                    // do nothing here
                    //--pm.gConfigProps.numberOfFreeConnections;
                    synchronized (pm.gConfigProps.numberOfInuseConnectionsLockObject) {
                        ++pm.gConfigProps.numberOfInuseConnections;
                    }
                }
            }

        }
        poolState.set(i);
    }

    /**
     * LI3162-5 - setInitialRequestStackTrace(Throwable t)
     */
    void setInitialRequestStackTrace(Throwable t) {
        this.initialRequestStackTrace = t;
    }

    /**
     * LI3162-5 - getInitialRequestStackTrace()
     */
    public Throwable getInitialRequestStackTrace() {
        return this.initialRequestStackTrace;
    }

    /**
     * @see com.ibm.ws.j2c.MCWrapper#setInSharedPool(boolean)
     */
    @Override
    public void setInSharedPool(boolean value1) {
        inSharedPool = value1;
    }

    /**
     * @see com.ibm.ws.j2c.MCWrapper#isInSharePool()
     */
    @Override
    public boolean isInSharedPool() {
        return inSharedPool;
    }

    /**
     * Support for 1PC Optimization
     *
     * @return
     */
    public boolean isConnectionSynchronizationProvider() {
        return connectionSynchronizationProvider;
    }

    /**
     * Support for 1PC Optimization
     *
     * @param b
     */
    @Override
    public void setConnectionSynchronizationProvider(boolean b) {
        connectionSynchronizationProvider = b;
    }

    /**
     * This method is used for marking a connection to destroy.
     * The connection state does not matter. The connection still
     * can be useable. When the connection is returned to the
     * free pool, this connection will be cleaned up and destroyed.
     *
     * This method may be called when total connection count is being
     * decreased.
     */
    @Override
    public void setDestroyConnectionOnReturn() {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "setDestroyConnectionOnReturn");
        }

        --fatalErrorValue;
        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "setDestroyConnectionOnReturn", fatalErrorValue);
        }

    }

    /**
     * Changing the fatal error code from using a long with
     * a value of currentTimeMillis to an int value.
     *
     * This will perform better than comparing to long values
     * and I need a way to dynamically set the fatal error value
     * without adding synchronization.
     *
     * Initially this value will be 1 more then the free pools
     * fatal error value. When a fatal error occurs, the value in
     * the free pool is increased by 1. When this fatal error value
     * is check on the connection return to the free pool, if it is
     * not greater than the free pools fatal error value, the connection
     * will be cleaned up and destroyed.
     */
    @Override
    public void setFatalErrorValue(int value) {
        fatalErrorValue = value;
    }

    /**
     * Reset the coordinator value to null
     */
    public void resetCoordinator() {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Resetting uow coordinator to null");
        }

        uowCoord = null;

    }

    /**
     * A destroy state of true for a connection will result in the connection being
     * destoryed when returned to the free pool. In addition, ANY connection pool requests
     * for this connection will result in a ResourceException.
     *
     * @return
     */
    @Override
    public boolean isDestroyState() {
        return destroyState;
    }

    /**
     * A destroy state of true for a connection will result in the connection being
     * destoryed when returned to the free pool. In addition, ANY connection pool requests
     * for this connection will result in a ResourceException.
     *
     * The destroyState is set to true when this method is used.
     *
     * @return
     */
    @Override
    public void setDestroyState() {
        destroyState = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.j2c.MCWrapper#setSubjectCRIHashCode(int)
     */
    @Override
    public void setSubjectCRIHashCode(int hashCode) {
        subjectCRIHashCode = hashCode;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.j2c.MCWrapper#getSubjectCRIHashCode()
     */
    @Override
    public int getSubjectCRIHashCode() {
        return subjectCRIHashCode;
    }

    /**
     * @return Returns the isEnlistmentDisabled.
     */
    public boolean isEnlistmentDisabled() {
        return mc instanceof WSManagedConnection ? !((WSManagedConnection) mc).isTransactional() : false; // RRA is the only resource adapter that supports enlistment disabled
    }

    /**
     * @param ivThreadId
     */
    public void setThreadID(String ivThreadId) {
        threadId = ivThreadId;
    }

    /**
     * @return Returns threadId
     */
    public String getThreadID() {
        return this.threadId;
    }

    /**
     * @param l
     */
    public void setLastAllocationTime(long l) {
        lastAllocationTime = l;
    }

    /**
     * @param l
     */
    public long getLastAllocationTime() {
        return lastAllocationTime;
    }

    /**
     * @param tname
     */
    public void setThreadName(String tname) {
        threadName = tname;
    }

    /**
     * @param tname
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * Indicates that an error has occurred from the transaction service. At which point
     * we know that the MCWrapper is no longer usable and will be destroyed when the MCWrapper
     * is returned to the pool.
     *
     * If the pool needs to be purged as a result of the transaction error, we rely on the
     * ResourceAdapter to fire a ConnectionErrorEvent.
     *
     */
    public void markTransactionError() {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "TransactionError occurred on MCWrapper:" + toString());
        }

        _transactionErrorOccurred = true;

    }

    /**
     * ShouldBeDestroyed is used by the free pool when a connection is returned to
     * the free pool. It replaces a check for isStale() to account for the new
     * _transactionErrorOccurred flag.
     *
     * @return true if this MCWrapper should be destroyed.
     */
    @Override
    public boolean shouldBeDestroyed() {
        return (_transactionErrorOccurred || stale);
    }

    /**
     * @return Returns the holdTimeStart.
     */
    public long getHoldTimeStart() {
        return holdTimeStart;
    }

    /**
     * @return Returns the cm.
     */
    public ConnectionManager getCm() {
        return cm;
    }

    public boolean isPretestThisConnection() {
        return pretestThisConnection;
    }

    public void setPretestThisConnection(boolean pretestThisConnection) {
        this.pretestThisConnection = pretestThisConnection;
    }

    /**
     * @param handle and its handlelist
     */
    public void addToHandleList(Object h, HandleList HL) {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (HL != null) {

            mcwHandleList.put(h, HL);
            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Adding Connection handle: " + h +
                                   "and its handle list object: " + HL +
                                   " to the MCWrapper connection Handle to HandeList map "
                                   + "MCwrapper Handlelist size : " + mcwHandleList.size());
            }

        } else {

            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "The Handle List is null for connection handle: " + h +
                                   " This is a thread with no context so so this handle will only " +
                                   " be stored in the handlelist no_context_handle_list object.");
            }

        }

    }

    /**
     * @param handle and its handlelist
     */
    public HandleList removeFromHandleList(Object h) {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        HandleList hl = mcwHandleList.remove(h);

        if (isTracingEnabled && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Removing Connection handle: " + h +
                               " from the MCWrapper connection Handle to HandeList map "
                               + "MCwrapper Handlelist size : " + mcwHandleList.size());
        }

        return hl;
    }

    /**
     * clear the handle list
     */
    public void clearHandleList() {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Clear the McWrapper handlelist for  the following MCWrapper: " + this);
        }

        // since we know we are only in this method on a destroy or clean up
        // of a MCWrapper ,we can double check that all the handles that this MCWrapper
        // owns are removed from the handlelist on thread local storage before clearing the
        // handlelist in the MCWrapper class.  I tried to be really careful to avoid NPEs
        //
        // Liberty doesn't have real HandleList so don't need to remove anything
        //

        mcwHandleList.clear();

    }

    /**
     *
     * @return the _managedConnectionFactory
     */
    public ManagedConnectionFactory get_managedConnectionFactory() {
        return _managedConnectionFactory;
    }

    /**
     *
     * @param _managedConnectionFactory the _managedConnectionFactory to set
     */
    public void set_managedConnectionFactory(ManagedConnectionFactory _managedConnectionFactory) {
        this._managedConnectionFactory = _managedConnectionFactory;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMarkedForPurgeDestruction() {
        return purgeState;
    }

    /** {@inheritDoc} */
    @Override
    public void markForPurgeDestruction() {
        purgeState = true;
    }

    //The Below two  methods are added to support PMI data for connection pools.This can be avoid if we expose com.ibm.ejs.jca,but currently as per JIM
    //it should not be done as j2c code is partial implementation only for JDBC and JMS.In future when j2c code is fully implemented its better to
    //remove the interface JCAPPMIHelper and implemented methods and update ConnectionPoolMonitor.java to use the exposed j2c code.
    @Override
    public String getUniqueId() {
        return this.gConfigProps.getXpathId();
    }

    @Override
    public boolean getParkedValue() {
        return this.isParkedWrapper;
    }

    @Override
    public String getJNDIName() {
        return this.gConfigProps.getJNDIName();
    }

    //PMIHelper methods end here

    /**
     * Abort the manage connection associated with this MCWrapper.
     *
     * @return whether or not the connection was successfully aborted.
     */
    public boolean abortMC() {
        boolean trace = TraceComponent.isAnyTracingEnabled();
        if (!(mc instanceof WSManagedConnection)) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "abortMC", "Skipping MC abort because MC is not an instance of WSManagedConnection");
            return false;
        }

        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, "abortMC");

        WSManagedConnection wsmc = (WSManagedConnection) mc;
        try {
            do_not_reuse_mcw = true;
            wsmc.abort(pm.connectorSvc.execSvcRef.getServiceWithException());
            aborted = true;
            releaseToPoolManager(); // Get the connection out of the pool
        } catch (SQLFeatureNotSupportedException e) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "JDBC feature or driver does not support aborting connections.");
        } catch (ResourceException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.MCWrapper.abortMC", "3765", this);
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "Caught exception releasing aborted connection to the pool manager.");
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, "abortMC", aborted);
        return aborted;
    }

    public boolean isMCAborted() {
        if (aborted)
            return true;
        if (mc instanceof WSManagedConnection && ((WSManagedConnection) mc).isAborted()) {
            aborted = true;
            return true;
        }
        return false;
    }

    /**
     * @return
     */
    public ConnectionManager getConnectionManagerWithoutStateCheck() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Connection manager is " + cm + " for managed connection " + this);
            if (cm == null && pm != null) {
                Tr.debug(this, tc, "Connection pool is " + this.pm.toString());
            }
        }
        return cm;
    }
}
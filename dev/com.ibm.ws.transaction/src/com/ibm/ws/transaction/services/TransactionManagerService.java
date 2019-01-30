/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.services;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionRolledbackException;
import javax.transaction.xa.XAResource;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.OnePhaseXAResource;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.tx.jta.embeddable.impl.EmbeddableTranManagerSet;
import com.ibm.tx.util.TMHelper;
import com.ibm.tx.util.TMService;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.Transaction.UOWCallback;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.Transaction.test.XAFlowCallbackControl;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.uow.UOWScopeCallback;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.tx.UOWEventListener;

public class TransactionManagerService implements ExtendedTransactionManager, TransactionManager, EmbeddableWebSphereTransactionManager, UOWCurrent {

    private static final TraceComponent tc = Tr.register(TransactionManagerService.class);

    private boolean isClient;

    boolean xaFlowCallbacksInitialised;
    private BundleContext _bundleContext = null;

    private EmbeddableWebSphereTransactionManager etm() {
        return EmbeddableTransactionManagerFactory.getTransactionManager();
    }

    protected void activate(BundleContext ctxt) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "activate  context " + ctxt);
        // Force embeddable mode
        _bundleContext = ctxt;
        if (ctxt.getProperty(WsLocationConstants.LOC_PROCESS_TYPE).equals(WsLocationConstants.LOC_PROCESS_TYPE_CLIENT)) {
            isClient = true;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Not activating Transaction Service: " + WsLocationConstants.LOC_PROCESS_TYPE_CLIENT);
            return;
        }

        EmbeddableTransactionManagerFactory.getTransactionManager();

        final ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();

        //This needs tidying a little.
        if (cp != null) {
            if (cp instanceof JTMConfigurationProvider) {
                JTMConfigurationProvider jtmCP = (JTMConfigurationProvider) cp;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "its a jtmconfigurationprovider ");

                // Set a reference to this TransactionManagerService into the JTMConfigurationProvider.
                // If other resources are in place this method will also start recovery by calling
                // doStart()
                jtmCP.setTMS(this);

            }
        }
    }

    /**
     * This method will start log recovery processing.
     *
     * @param cp
     */
    public void doStartup(ConfigurationProvider cp, boolean isSQLRecoveryLog) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "doStartup with cp: " + cp + " and flag: " + isSQLRecoveryLog);

        // Create an AppId that will be unique for this server to be used in the generation of Xids.

        // Locate the user directory in the Liberty install
        String userDirEnv = System.getenv("WLP_USER_DIR");

        if (tc.isDebugEnabled())
            Tr.debug(tc, "TMS, WLP_USER_DIR env variable is - " + userDirEnv);
        // Retrieve the server name.
        String serverName = cp.getServerName();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "TMS, serverName is - " + serverName);
        // Retrieve the host name
        String hostName = "";
        hostName = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {

                String theHost = "";
                try {
                    InetAddress addr = InetAddress.getLocalHost();
                    theHost = addr.getCanonicalHostName().toLowerCase();
                } catch (UnknownHostException e) {
                    theHost = "localhost";
                }

                return theHost;
            }
        });

        byte[] theApplId = createApplicationId(userDirEnv, serverName, hostName);
        cp.setApplId(theApplId);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "TMS, cp - " + cp + " set applid - " + Util.toHexString(theApplId));
        if (!xaFlowCallbacksInitialised) {
            // -------------------------------------------------------
            // Initialize the XA Flow callbacks by
            // attempting to load the test class.
            // -------------------------------------------------------
            if (tc.isDebugEnabled())
                Tr.debug(tc, "initialise the XA Flow callbacks");
            XAFlowCallbackControl.initialize();
            xaFlowCallbacksInitialised = true;
        }
        if (cp.isRecoverOnStartup()) {
            try {
                TMHelper.start(cp.isWaitForRecovery());
            } catch (Exception e) {
                FFDCFilter.processException(e, "com.ibm.ws.transaction.services.TransactionManagerService.doStartup", "60", this);
            }
        }
    }

    /**
     * This method will shutdown log recovery processing if we are working with transaction logs stored in
     * an RDBMS.
     *
     * @param cp
     */
    public void doShutdown(boolean isSQLRecoveryLog) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "doShutdown with flag: " + isSQLRecoveryLog);

        if (isSQLRecoveryLog) {
            try {
                TMHelper.shutdown();
            } catch (Exception e) {
                FFDCFilter.processException(e, "com.ibm.ws.transaction.services.TransactionManagerService.doShutdown", "60", this);
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "doShutdown");
    }

    protected void deactivate(ComponentContext ctxt) {}

    protected void setTmService(TMService tm) {
        // dependency injection ... forces tran service to initialize
    }

    protected void unsetTmService(TMService tm) {}

    @Override
    public void setUOWEventListener(UOWEventListener el) {
        ((UOWCurrent) etm()).setUOWEventListener(el);
    }

    @Override
    public void unsetUOWEventListener(UOWEventListener el) {
        ((UOWCurrent) etm()).unsetUOWEventListener(el);
    }

    @Override
    public boolean delist(XAResource xaRes, int flag) {
        return false;
    }

    @Override
    public boolean enlist(XAResource xaRes, int recoveryId) throws RollbackException, IllegalStateException, SystemException {
        if (isClient) {
            throw new SystemException();
        }

        return etm().enlist(xaRes, recoveryId);
    }

    @Override
    public int registerResourceInfo(String xaResFactoryClassName, Serializable xaResInfo) {
        if (isClient) {
            return -1;
        }

        final int ret = etm().registerResourceInfo(xaResFactoryClassName, xaResInfo);
        if (!xaFlowCallbacksInitialised) {
            // -------------------------------------------------------
            // Initialize the XA Flow callbacks by
            // attempting to load the test class.
            // -------------------------------------------------------
            if (tc.isDebugEnabled())
                Tr.debug(tc, "initialise the XA Flow callbacks");
            XAFlowCallbackControl.initialize();
            xaFlowCallbacksInitialised = true;
        }
        return ret;
    }

    @Override
    public int registerResourceInfo(String xaResFactoryClassName, Serializable xaResInfo, int priority) {
        if (isClient) {
            return -1;
        }

        final int ret = etm().registerResourceInfo(xaResFactoryClassName, xaResInfo, priority);
        if (!xaFlowCallbacksInitialised) {
            // -------------------------------------------------------
            // Initialize the XA Flow callbacks by
            // attempting to load the test class.
            // -------------------------------------------------------
            if (tc.isDebugEnabled())
                Tr.debug(tc, "initialise the XA Flow callbacks");
            XAFlowCallbackControl.initialize();
            xaFlowCallbacksInitialised = true;
        }
        return ret;
    }

    @Override
    public void begin() throws NotSupportedException, SystemException {
        if (isClient) {
            throw new SystemException();
        }

        etm().begin();
    }

    @Override
    public void begin(int timeout) throws NotSupportedException, SystemException {
        if (isClient) {
            throw new SystemException();
        }

        etm().begin(timeout);
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        if (isClient) {
            throw new SystemException();
        }

        etm().commit();
    }

    @Override
    public int getStatus() throws SystemException {
        if (isClient) {
            return Status.STATUS_NO_TRANSACTION;
        }

        return etm().getStatus();
    }

    @Override
    public Transaction getTransaction() throws SystemException {
        if (isClient) {
            return null;
        }

        return etm().getTransaction();
    }

    @Override
    public void resume(Transaction t) throws InvalidTransactionException, IllegalStateException, SystemException {
        if (isClient) {
            throw new SystemException();
        }

        etm().resume(t);
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        if (isClient) {
            throw new SystemException();
        }

        etm().rollback();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        if (isClient) {
            throw new SystemException();
        }

        etm().setRollbackOnly();
    }

    @Override
    public void setTransactionTimeout(int arg0) throws SystemException {
        if (isClient) {
            throw new SystemException();
        }

        etm().setTransactionTimeout(arg0);
    }

    @Override
    public Transaction suspend() throws SystemException {
        if (isClient) {
            throw new SystemException();
        }

        return etm().suspend();
    }

    /** {@inheritDoc} */
    @Override
    public boolean enlist(UOWCoordinator coord, XAResource xaRes, int recoveryId, int branchCoupling) throws RollbackException, IllegalStateException, SystemException {
        if (isClient) {
            throw new SystemException();
        }

        return etm().enlist(coord, xaRes, recoveryId, branchCoupling);
    }

    /** {@inheritDoc} */
    @Override
    public boolean enlistOnePhase(UOWCoordinator coord, OnePhaseXAResource opXaRes) throws RollbackException, IllegalStateException, SystemException {
        if (isClient) {
            throw new SystemException();
        }

        return etm().enlistOnePhase(coord, opXaRes);
    }

    /** {@inheritDoc} */
    @Override
    public boolean startInactivityTimer(Transaction t, InactivityTimer iat) {
        if (isClient) {
            throw new IllegalStateException();
        }

        return etm().startInactivityTimer(t, iat);
    }

    /** {@inheritDoc} */
    @Override
    public void stopInactivityTimer(Transaction t) {
        if (isClient) {
            throw new IllegalStateException();
        }

        etm().stopInactivityTimer(t);
    }

    /** {@inheritDoc} */
    @Override
    public void registerSynchronization(UOWCoordinator coord, Synchronization sync) throws RollbackException, IllegalStateException, SystemException {
        if (isClient) {
            throw new SystemException();
        }

        etm().registerSynchronization(coord, sync);
    }

    /** {@inheritDoc} */
    @Override
    public void registerSynchronization(UOWCoordinator coord, Synchronization sync, int tier) throws RollbackException, IllegalStateException, SystemException {
        if (isClient) {
            throw new SystemException();
        }

        etm().registerSynchronization(coord, sync, tier);
    }

    /** {@inheritDoc} */
    @Override
    public void completeTxTimeout() throws TransactionRolledbackException {
        etm().completeTxTimeout();
    }

    /** {@inheritDoc} */
    @Override
    public void registerCallback(UOWScopeCallback callback) {
        if (isClient) {
            throw new IllegalStateException();
        }

        etm().registerCallback(callback);
    }

    /** {@inheritDoc} */
    @Override
    public UOWCoordinator getUOWCoord() {
        if (isClient) {
            return null;
        }

        return ((EmbeddableTranManagerSet) etm()).getUOWCoord();
    }

    /** {@inheritDoc} */
    @Override
    public int getUOWType() {
        if (isClient) {
            return UOWCurrent.UOW_NONE;
        }

        return ((EmbeddableTranManagerSet) etm()).getUOWType();
    }

    /** {@inheritDoc} */
    @Override
    public void registerLTCCallback(UOWCallback arg0) {
        if (isClient) {
            throw new IllegalStateException();
        }

        ((EmbeddableTranManagerSet) etm()).registerLTCCallback(arg0);
    }

    @FFDCIgnore({ org.osgi.framework.BundleException.class })
    public void shutDownFramework() {
//        Tr.audit(tc, "TransactionManagerService.shutDownFramework");
        try {
            if (_bundleContext != null) {
                Bundle bundle = _bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);

                if (bundle != null)
                    bundle.stop();
            }
        } catch (org.osgi.framework.BundleException e) {
            // do not FFDC this.
            // exceptions during bundle stop occur if framework is already stopping or stopped
        } catch (Exception e) {
            // do not FFDC this.
            // exceptions during bundle stop occur if framework is already stopping or stopped
        }
        throw new IllegalStateException("Shutting down framework due to startup problems");
    }

    /**
     * Returns an application identifier key which can be used as a unique component
     * within the global identifier and branch qualifier of an XID.
     * This method is derived from tWAS
     *
     * @param name The server name.
     *
     * @return The application identifier key.
     */
    private byte[] createApplicationId(String userDir, String serverName, String hostName) {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createApplicationId", new Object[] { userDir, serverName, hostName });
        byte[] result;

        try {
            // tWAS - Create a dummy IOR based on this host's IP address and server's port
            // tWAS - Note: the object must be remoteable, so use Current as an example.
            // tWAS - String s = CORBAUtils.getORB().object_to_string(CurrentImpl.instance());
            // On Liberty concatenate the user directory, the server name and the host name. Then add in the time.
            String s = userDir + serverName + hostName + System.currentTimeMillis();
            // Create a 20-byte hash value using a secure one-way hash function
            result = java.security.MessageDigest.getInstance("SHA").digest(s.getBytes());
        } catch (Throwable t) {
            FFDCFilter.processException(t, "com.ibm.ws.transaction.createApplicationId", "608", this);
            String tempStr = "j" + (System.currentTimeMillis() % 9997) + ":" + userDir + hostName;
            result = tempStr.getBytes();
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createApplicationId", Util.toHexString(result));
        return result;
    }

}
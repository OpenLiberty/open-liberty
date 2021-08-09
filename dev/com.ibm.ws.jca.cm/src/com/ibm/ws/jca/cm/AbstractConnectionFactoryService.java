/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.cm;

import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.TransactionSupport.TransactionSupportLevel;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

import com.ibm.ejs.j2c.CommonFunction;
import com.ibm.ejs.j2c.ConnectorServiceImpl;
import com.ibm.ejs.j2c.J2CConstants;
import com.ibm.ejs.j2c.WSXAResourceImpl;
import com.ibm.tx.jta.DestroyXAResourceException;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jca.adapter.WSXAResource;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.security.jca.AuthDataService;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * Abstraction of connection factory service that is common between dataSource and connectionFactory
 */
public abstract class AbstractConnectionFactoryService implements Observer, ResourceFactory, XAResourceFactory {
    private static final TraceComponent tc = Tr.register(AbstractConnectionFactoryService.class, J2CConstants.traceSpec, J2CConstants.NLS_FILE);

    /**
     * Name of optional id property.
     */
    public static final String ID = "id";

    private static final Pattern DEFAULT_NESTED_PATTERN = Pattern.compile(".*(\\[default-\\d*\\])$");
    private static final Pattern DEFAULT_PATTERN = Pattern.compile("(default-\\d*)$");

    /**
     * Thread identity support constants.
     */
    public static final int THREAD_IDENTITY_NOT_ALLOWED = 0,
                    THREAD_IDENTITY_ALLOWED = 1,
                    THREAD_IDENTITY_REQUIRED = 2;

    /**
     * Set of names of applications that have accessed this connection factory
     * (if the connection factory is defined in the server configuration, otherwise the list will remain empty).
     * The set supports concurrent modifications.
     */
    protected final Set<String> appsToRecycle = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * The connection manager service
     */
    protected ConnectionManagerService conMgrSvc;

    /**
     * Service reference to the default container managed auth alias (if any).
     * Must use the read/write lock when accessing and updating this value.
     */
    private ServiceReference<?> containerAuthDataRef;

    /**
     * Service reference to the configured jaasLoginContextEntry (if any).
     * Must use the read/write lock when accessing and updating this value.
     */
    private ServiceReference<?> jaasLoginContextEntryRef;

    /**
     * Indicates if the ConnectionFactoryService is initialized.
     * Initialization happens lazily, on first use, not during activation.
     * When configuration updates are made while the server is running,
     * the ConnectionFactoryService can go back to an uninitialized state if needed
     * to properly handle the updates.
     */
    protected final AtomicBoolean isInitialized = new AtomicBoolean();

    /**
     * Indicates whether or not this connection factory is defined in the server configuration (as opposed to defined in an application).
     */
    protected boolean isServerDefined;

    /**
     * Lock for reading and updating connection factory configuration.
     */
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Service reference to the auth alias (if any) for XA recovery.
     */
    private ServiceReference<?> recoveryAuthDataRef;

    /**
     * Create a connection factory.
     *
     * @param resInfo resource reference. Null if no resource reference.
     * @return the connection factory.
     * @throws Exception if an error occurs.
     */
    @Override
    public Object createResource(final ResourceInfo resInfo) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "createResource", resInfo);

        Object connectionFactory;

        lock.readLock().lock();
        try {
            if (!isInitialized.get()) {
                // Switch to write lock for lazy initialization
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    if (!isInitialized.get())
                        initPrivileged();
                } finally {
                    // Downgrade to read lock for rest of method
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }
            checkAccess();
            ConnectionManager conMgr = AccessController.doPrivileged(new PrivilegedExceptionAction<ConnectionManager>() {
                @Override
                public ConnectionManager run() throws Exception {
                    return conMgrSvc.getConnectionManager(resInfo, AbstractConnectionFactoryService.this);
                }
            });
            connectionFactory = getManagedConnectionFactory(null).createConnectionFactory(conMgr);
            // TODO fix this error path once updates to ExpectedFFDC in existing test case makes it into release
            //} catch (PrivilegedActionException x) {
            //    Throwable cause = x.getCause();
            //    if (trace && tc.isEntryEnabled())
            //        Tr.exit(this, tc, "createResource", x);
            //    if (cause instanceof Exception)
            //        throw (Exception) cause;
            //    else if (cause instanceof Error)
            //        throw (Error) cause;
            //    else
            //        throw x;
        } catch (Exception x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "createResource", x);
            throw x;
        } catch (Error x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "createResource", x);
            throw x;
        } finally {
            lock.readLock().unlock();
        }

        // Only connection factories defined in the server configuration are tracked for the app recycle coordinator.
        if (isServerDefined) {
            ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            if (cData != null)
                appsToRecycle.add(cData.getJ2EEName().getApplication());
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "createResource", connectionFactory);
        return connectionFactory;
    }

    /**
     * Destroy the XAResource object. Internally, the XAResource provider
     * should cleanup resources used by XAResource object. For example, JTA
     * should close XAConnection.
     *
     * @param xa resource to destroy.
     * @throws DestroyXAResourceException if an error occurs.
     */
    @Override
    public void destroyXAResource(XAResource xa) throws DestroyXAResourceException {

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "destroyXAResource", xa);

        ManagedConnection mc = ((WSXAResource) xa).getManagedConnection();
        try {
            mc.destroy();
        } catch (ResourceException x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(tc, "destroyXAResource", x);
            throw new DestroyXAResourceException(x);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "destroyXAResource");
    }

    /**
     * Returns the name of the config element used to configure this type of connection factory.
     * For example, dataSource or jmsConnectionFactory
     *
     * @return the name of the config element used to configure this type of connection factory.
     */
    public abstract String getConfigElementName();

    public abstract ConnectorService getConnectorService();

    /**
     * Returns the id of the authData element for the default container managed auth alias (if any).
     *
     * @return the id of the authData element for the default container managed auth alias (if any).
     */
    public String getContainerAuthDataID() {
        String authDataID = null;
        lock.readLock().lock();
        try {
            if (containerAuthDataRef != null) {
                authDataID = (String) containerAuthDataRef.getProperty(ID);
                if (authDataID == null ||
                    DEFAULT_PATTERN.matcher(authDataID).matches() ||
                    DEFAULT_NESTED_PATTERN.matcher(authDataID).matches()) {
                    authDataID = (String) containerAuthDataRef.getProperty("config.displayId");
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "default container auth data", authDataID);
        return authDataID;
    }

    /**
     * @return the id of the authData element for the recovery auth alias (if any).
     */
    public String getRecoveryAuthDataID() {
        String authDataID = null;
        lock.readLock().lock();
        try {
            if (recoveryAuthDataRef != null) {
                authDataID = (String) recoveryAuthDataRef.getProperty(ID);
                if (authDataID == null ||
                    DEFAULT_PATTERN.matcher(authDataID).matches() ||
                    DEFAULT_NESTED_PATTERN.matcher(authDataID).matches()) {
                    authDataID = (String) recoveryAuthDataRef.getProperty("config.displayId");
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "default recovery auth data", authDataID);
        return authDataID;
    }

    /**
     * Returns the version of the enabled feature that provides this connection factory.
     * For example, 4.2 from jdbc-4.2.
     *
     * @return version if a jdbc feature. Currently null in the case of jca features.
     */
    public Version getFeatureVersion() {
        return null;
    }

    /**
     * Returns the name attribute from the configured JAAS login context entry.
     *
     * @return the name attribute from the configured JAAS login context entry.
     */
    public String getJaasLoginContextEntryName() {
        String jaasLoginContextEntryName = null;
        lock.readLock().lock();
        try {
            if (jaasLoginContextEntryRef != null)
                jaasLoginContextEntryName = (String) jaasLoginContextEntryRef.getProperty("name");
        } finally {
            lock.readLock().unlock();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "JAAS login context entry name", jaasLoginContextEntryName);
        return jaasLoginContextEntryName;
    }

    /**
     * Returns the unique identifier for this connection factory configuration.
     *
     * @return the unique identifier for this connection factory configuration.
     */
    public abstract String getID();

    /**
     * Returns the JNDI name.
     *
     * @return the JNDI name.
     */
    public abstract String getJNDIName();

    /**
     * Returns the managed connection factory.
     *
     * Prerequisite: the invoker must hold a read or write lock on this connection factory service instance.
     *
     * @param identifier identifier for the class loader from which to load vendor classes (for XA recovery path). Otherwise, null.
     * @return the managed connection factory.
     * @throws Exception if an error occurs obtaining the managed connection factory.
     */
    public abstract ManagedConnectionFactory getManagedConnectionFactory(String identifier) throws Exception;

    /**
     * Indicates whether or not reauthentication of connections is enabled.
     *
     * @return true if reauthentication of connections is enabled. Otherwise false.
     */
    public abstract boolean getReauthenticationSupport();

    /**
     * Obtain a subject to use for recovery.
     * Precondition: the invoker must have a read lock on this connection factory service instance.
     *
     * @param mcf       the managed connection factory
     * @param xaresinfo serialized ArrayList<Byte> for the CMConfigData
     * @return subject to use for recovery. Null if the default user/password of the connection factory should be used.
     * @throws Exception if an error occurs.
     */
    private final Subject getSubjectForRecovery(ManagedConnectionFactory mcf, Serializable xaresinfo) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "getSubjectForRecovery", recoveryAuthDataRef);

        String authDataID = null;
        if (recoveryAuthDataRef != null) {
            authDataID = (String) recoveryAuthDataRef.getProperty(ID);
            if (authDataID == null ||
                DEFAULT_PATTERN.matcher(authDataID).matches() ||
                DEFAULT_NESTED_PATTERN.matcher(authDataID).matches()) {
                authDataID = (String) recoveryAuthDataRef.getProperty("config.displayId");
            }
        }

        // If recoveryAuthDataRef isn't specified, then use the container managed auth alias, if there is one
        if (authDataID == null && xaresinfo != null) {
            @SuppressWarnings("unchecked")
            ArrayList<Byte> byteList = (ArrayList<Byte>) xaresinfo;
            byte[] bytes = new byte[byteList.size()];
            int i = 0;
            for (Byte b : byteList)
                bytes[i++] = b;
            ResourceInfo info = (ResourceInfo) ConnectorService.deserialize(bytes);
            if (info.getAuth() == ResourceInfo.AUTH_CONTAINER) {
                for (ResourceInfo.Property loginConfigProp : info.getLoginPropertyList())
                    if ("DefaultPrincipalMapping".equals(loginConfigProp.getName()))
                        authDataID = loginConfigProp.getValue();
                // If no auth-data in recovery info, then use the default container managed auth alias (if any)
                if (authDataID == null)
                    authDataID = getContainerAuthDataID();
            }

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "container managed auth", authDataID);
        }

        Subject subject = null;
        if (authDataID != null) {
            AuthDataService authDataSvc = ((ConnectorServiceImpl) getConnectorService()).authDataServiceRef.getServiceWithException();
            Map<String, Object> loginData = Collections.singletonMap("com.ibm.mapping.authDataAlias", (Object) authDataID);
            subject = authDataSvc.getSubject(mcf, null, loginData);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "getSubjectForRecovery", CommonFunction.toString(subject));
        return subject;
    }

    /**
     * Indicates whether or not thread identity, sync-to-thread, and RRS transactions are supported.
     * The result is a 3 element array, of which,
     * <ul>
     * <li>The first element indicates support for thread identity. 2=REQUIRED, 1=ALLOWED, 0=NOT ALLOWED.</li>
     * <li>The second element indicates support for "synch to thread" for the
     * allocateConnection, i.e., push an ACEE corresponding to the current java
     * Subject on the native OS thread. 1=supported, 0=not supported.</li>
     * <li>The third element indicates support for RRS transactions. 1=supported, 0=not supported.</li>
     * </ul>
     *
     * Prerequisite: the invoker must hold a read or write lock on this connection factory service instance.
     *
     * @param identifier identifier for the class loader from which to load vendor classes (for XA recovery path). Otherwise, null.
     * @return boolean array indicating whether or not each of the aforementioned capabilities are supported.
     */
    public abstract int[] getThreadIdentitySecurityAndRRSSupport(String identifier);

    /**
     * Indicates the level of transaction support.
     *
     * @return constant indicating the transaction support of the resource adapter.
     */
    public abstract TransactionSupportLevel getTransactionSupport();

    /**
     * @return true if ManagedConnectionFactory instances created by this service implement ValidatingManagedConnectionFactory, otherwise false.
     */
    public abstract boolean getValidatingManagedConnectionFactorySupport();

    /**
     * Given XAResourceInfo, the XAResourceFactory produces a XAResource object.
     *
     * getXAResource may also be invoked during normal server running if a RM returns XAER_RMFAIL
     * on a completion method. The TM will attempt to retry the completion method after obtaining
     * a new XAResource. This retry processing will continue until either the retry limits are
     * exceeded, the resource manager allows completion to occur or a permanent error is reported.
     *
     * @param xaresinfo information about the XA resource.
     * @throws XAResourceNotAvailableException to indicate that the resource manager is not available
     *                                             and recovery may not complete. Any other exception raised by getXAResource will be
     *                                             caught by the TM and the server terminated as recovery cannot be guaranteed.
     */
    @Override
    public XAResource getXAResource(Serializable xaresinfo) throws XAResourceNotAvailableException {

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "getXAResource", getID(), xaresinfo);

        XAResource xa;
        ManagedConnection mc = null;
        lock.readLock().lock();
        try {
            if (!isInitialized.get())
                try {
                    // Switch to write lock for lazy initialization
                    lock.readLock().unlock();
                    lock.writeLock().lock();

                    if (!isInitialized.get())
                        initPrivileged();
                } finally {
                    // Downgrade to read lock for rest of method
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }

            // TODO supply class loader identifier if resource was loaded from application
            ManagedConnectionFactory mcf = getManagedConnectionFactory(null);

            setMQQueueManager(xaresinfo);

            Subject subject = getSubjectForRecovery(mcf, xaresinfo);
            mc = mcf.createManagedConnection(subject, null);
            xa = mc.getXAResource();
            xa = xa instanceof WSXAResource ? xa : new WSXAResourceImpl(mc, xa);
        } catch (Throwable x) {
            // Connections must be destroyed so they aren't leaked
            if (mc != null)
                try {
                    mc.destroy();
                } catch (Throwable t) {
                }

            FFDCFilter.processException(x, getClass().getName(), "328", this, new Object[] { xaresinfo });
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "getXAResource", x);

            if (x instanceof Error)
                throw (Error) x;
            if (x instanceof RuntimeException)
                throw (RuntimeException) x;
            throw new XAResourceNotAvailableException(x);
        } finally {
            lock.readLock().unlock();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "getXAResource", xa);
        return xa;
    }

    /**
     * Returns whether Liberty Connection Pooling should be disabled
     *
     */
    public abstract boolean isLibertyConnectionPoolingDisabled();

    @FFDCIgnore(PrivilegedActionException.class)
    private void initPrivileged() throws Exception {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    init();
                    return null;
                }
            });
        } catch (PrivilegedActionException paex) {
            Throwable cause = paex.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new Error(cause);
        }
    }

    /**
     * Lazy initialization.
     * Precondition: invoker must have write lock on this ConnectionFactoryService
     *
     * @throws Exception if an error occurs
     */
    protected abstract void init() throws Exception;

    /**
     * Checks whether the connection factory is accessible from the application
     * looking it up.
     *
     * @throws ResourceException if its not accessible
     */
    protected abstract void checkAccess() throws ResourceException;

    /**
     * Declarative Services method for setting the service reference for the default container auth data
     *
     * @param ref reference to the service
     */
    protected void setContainerAuthData(ServiceReference<?> ref) { // com.ibm.websphere.security.auth.data.AuthData
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setContainerAuthData", ref);
        lock.writeLock().lock();
        try {
            containerAuthDataRef = ref;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Declarative services method to set the JAASLoginContextEntry.
     */
    protected void setJaasLoginContextEntry(ServiceReference<?> ref) { // com.ibm.ws.security.jaas.common.JAASLoginContextEntry
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setJaasLoginContextEntry", ref);
        lock.writeLock().lock();
        try {
            jaasLoginContextEntryRef = ref;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Declarative Services method for setting the recovery auth data service reference
     *
     * @param ref reference to the service
     */
    protected void setRecoveryAuthData(ServiceReference<?> ref) { // com.ibm.websphere.security.auth.data.AuthData
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setRecoveryAuthData", ref);
        lock.writeLock().lock();
        try {
            recoveryAuthDataRef = ref;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Declarative Services method for unsetting the service reference for the default container auth data
     *
     * @param ref reference to the service
     */
    protected void unsetContainerAuthData(ServiceReference<?> ref) { // com.ibm.websphere.security.auth.data.AuthData
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "unsetContainerAuthData", ref);
        lock.writeLock().lock();
        try {
            if (containerAuthDataRef == ref)
                containerAuthDataRef = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Declarative services method to unset the JAASLoginContextEntry.
     */
    protected void unsetJaasLoginContextEntry(ServiceReference<?> ref) { // com.ibm.ws.security.jaas.common.JAASLoginContextEntry
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "unsetJaasLoginContextEntry", ref);
        lock.writeLock().lock();
        try {
            if (jaasLoginContextEntryRef == ref)
                jaasLoginContextEntryRef = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Declarative Services method for unsetting the recovery auth data service reference
     *
     * @param ref reference to the service
     */
    protected void unsetRecoveryAuthData(ServiceReference<?> ref) { // com.ibm.websphere.security.auth.data.AuthData
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "unsetRecoveryAuthData", ref);
        lock.writeLock().lock();
        try {
            if (recoveryAuthDataRef == ref)
                recoveryAuthDataRef = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Set the MQ QMID if one is available.
     *
     * @return constant indicating the transaction support of the resource adapter.
     */
    public abstract void setMQQueueManager(Serializable xaresinfo) throws Exception;

}

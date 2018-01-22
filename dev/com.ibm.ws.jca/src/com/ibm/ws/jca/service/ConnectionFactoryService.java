/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.service;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.Connector;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.TransactionSupport;
import javax.resource.spi.TransactionSupport.TransactionSupportLevel;
import javax.resource.spi.ValidatingManagedConnectionFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jca.cm.AbstractConnectionFactoryService;
import com.ibm.ws.jca.cm.ConnectionManagerService;
import com.ibm.ws.jca.cm.ConnectorService;
import com.ibm.ws.jca.internal.BootstrapContextImpl;
import com.ibm.ws.jca.internal.ResourceAdapterMetaData;
import com.ibm.ws.jca.internal.Utils;
import com.ibm.ws.kernel.service.util.PrivHelper;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * An instance of this service should be provided on behalf of each connection factory.
 * Each instance requires a nested properties element (to identify the resource adapter and connection factory interface)
 * and should be bound to a BootstrapContext, so that the connection factory
 * cannot be used unless the resource adapter is started.
 * An instance can optionally be bound to a ConnectionManagerService.
 * If not bound to a ConnectionManagerService then a default connection manager is created to manage connections.
 */
//as documentation only at this point:
//@Component(pid="com.ibm.ws.jca.connectionFactory.supertype")
public class ConnectionFactoryService extends AbstractConnectionFactoryService implements ApplicationRecycleComponent {
    private static final TraceComponent tc = Tr.register(ConnectionFactoryService.class);

    /**
     * Name of reference to the ConnectionManagerService
     */
    private static final String CONNECTION_MANAGER = "connectionManager";

    /**
     * Prefix for flattened config properties.
     */
    private static final String CONFIG_PROPS_PREFIX = "properties.0.";

    /**
     * Length of prefix for flattened config properties.
     */
    private static final int CONFIG_PROPS_PREFIX_LENGTH = CONFIG_PROPS_PREFIX.length();

    public static final String FACTORY_PID = "com.ibm.ws.jca.connectionFactory.supertype";

    public static final String CONNECTION_FACTORY = "connectionFactory";

    /**
     * Resource adapter constants from ra.xml
     */
    private static final String REAUTHENTICATION_SUPPORT = "reauthentication-support";
    private static final String TRANSACTION_SUPPORT = "transaction-support";

    /**
     * Reference to the resource adapter bootstrap context.
     */
    private final AtomicServiceReference<BootstrapContextImpl> bootstrapContextRef = new AtomicServiceReference<BootstrapContextImpl>("bootstrapContext");

    /**
     * Component context.
     */
    private ComponentContext componentContext;

    /**
     * Name of the config element used to configure this type of connection factory.
     * For example: connectionFactory, jmsConnectionFactory, jmsQueueConnectionFactory, or jmsTopicConnectionFactory
     */
    private String configElementName;

    /**
     * Reference to the connectionManager (if any) that is configured for this connection factory.
     */
    private ServiceReference<ConnectionManagerService> connectionManagerRef;

    /**
     * Unique identifier for this connection factory configuration.
     */
    private String id;

    /**
     * JNDI name for this connection factory.
     */
    private String jndiName;

    /**
     * The managed connection factory
     */
    private ManagedConnectionFactory mcf;

    private TransactionSupportLevel connectionFactoryTransactionSupport;

    /**
     * Implementation class name for the managed connection factory.
     */
    private String mcfImplClassName;

    /**
     * Config properties.
     */
    private final Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context DeclarativeService defined/populated component context
     */
    @Trivial
    protected void activate(ComponentContext context) throws Exception {
        Dictionary<String, ?> props = context.getProperties();
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "activate", props);

        String sourcePID = (String) props.get("ibm.extends.source.pid"); // com.ibm.ws.jca.jmsQueueConnectionFactory_gen_3f3cb305-4146-41f9-8a57-b231d09013e6
        configElementName = sourcePID == null ? "connectionFactory" : sourcePID.substring(15, sourcePID.indexOf('_', 15));

        mcfImplClassName = (String) props.get(CONFIG_PROPS_PREFIX + "managedconnectionfactory-class");
        jndiName = (String) props.get(JNDI_NAME);
        id = (String) props.get("config.displayId");

        componentContext = context;
        isServerDefined = true; // We don't support app-defined connection factories yet

        //Integer trlevel = props.get("transactionSupport");
        if (props.get("transactionSupport") != null)
            connectionFactoryTransactionSupport = TransactionSupportLevel.valueOf((String) props.get("transactionSupport"));

        // filter out actual config properties for the connection factory
        for (Enumeration<String> keys = props.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            if (key.length() > CONFIG_PROPS_PREFIX_LENGTH && key.charAt(CONFIG_PROPS_PREFIX_LENGTH - 1) == '.' && key.startsWith(CONFIG_PROPS_PREFIX)) {
                String propName = key.substring(CONFIG_PROPS_PREFIX_LENGTH);
                if (propName.indexOf('.') < 0 && propName.indexOf('-') < 0)
                    properties.put(propName, props.get(key));
            }
        }

        bootstrapContextRef.activate(context);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "activate");
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context DeclarativeService defined/populated component context
     */
    protected void deactivate(ComponentContext context) {
        destroyConnectionFactories(true);
        bootstrapContextRef.deactivate(context);
    }

    @Override
    public final ConnectorService getConnectorService() {
        return bootstrapContextRef.getServiceWithException().getConnectorService();
    }

    @Override
    public ApplicationRecycleContext getContext() {
        ApplicationRecycleContext context = bootstrapContextRef.getService();
        if (context != null) {
            return context;
        }
        return null;
    }

    @Override
    public Set<String> getDependentApplications() {
        Set<String> members = new HashSet<String>(appsToRecycle);
        appsToRecycle.removeAll(members);
        return members;
    }

    /**
     * Utility method to destroy connection factory instances.
     *
     * @param destroyImmediately indicates to immediately destroy instead of deferring to later.
     */
    private void destroyConnectionFactories(boolean destroyImmediately) {

        lock.writeLock().lock();
        try {
            if (isInitialized.get()) {
                // Mark all connection factories as disabled
                isInitialized.set(false);

                // Destroy the connection factories
                conMgrSvc.deleteObserver(this);
                conMgrSvc.destroyConnectionFactories();

                conMgrSvc = null;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the name of the config element used to configure this type of connection factory.
     * For example, jmsConnectionFactory or jmsTopicConnectionFactory
     *
     * @return the name of the config element used to configure this type of connection factory.
     */
    @Override
    @Trivial
    public String getConfigElementName() {
        return configElementName;
    }

    /**
     * Returns the unique identifier for this connection factory configuration.
     *
     * @return the unique identifier for this connection factory configuration.
     */
    @Override
    @Trivial
    public String getID() {
        return id;
    }

    /**
     * Returns the JNDI name.
     *
     * @return the JNDI name.
     */
    @Override
    @Trivial
    public String getJNDIName() {
        return jndiName;
    }

    /**
     * Returns the managed connection factory.
     *
     * Prerequisite: the invoker must hold a read or write lock on this connection factory service instance.
     *
     * @param identifier identifier for the class loader from which to load vendor classes (for XA recovery path). Otherwise, null.
     * @return the managed connection factory.
     */
    @Override
    @Trivial
    public ManagedConnectionFactory getManagedConnectionFactory(String identifier) {
        return mcf;
    }

    /**
     * Indicates whether or not reauthentication of connections is enabled.
     *
     * @return true if reauthentication of connections is enabled. Otherwise false.
     */
    @Override
    @Trivial
    public boolean getReauthenticationSupport() {
        return Boolean.TRUE.equals(bootstrapContextRef.getReference().getProperty(REAUTHENTICATION_SUPPORT));
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
    @Override
    @FFDCIgnore(NoSuchMethodException.class)
    public int[] getThreadIdentitySecurityAndRRSSupport(String identifier) {
        int rrsTransactional = 0;
        try {
            if (Boolean.TRUE.equals(mcf.getClass().getMethod("getRRSTransactional").invoke(mcf)))
                rrsTransactional = 1;
        } catch (NoSuchMethodException x) {
        } catch (Exception x) {
            FFDCFilter.processException(x, getClass().getName(), "327", new Object[] { mcf.getClass() });
        }

        return new int[] { 0, 0, rrsTransactional };
    }

    /**
     * Indicates the level of transaction support.
     *
     * @return constant indicating the transaction support of the resource adapter.
     */
    @Override
    public TransactionSupportLevel getTransactionSupport() {
        // If ManagedConnectionFactory implements TransactionSupport, that takes priority
        TransactionSupportLevel transactionSupport = mcf instanceof TransactionSupport ? ((TransactionSupport) mcf).getTransactionSupport() : null;

        // Otherwise get the value from the deployment descriptor
        String prop = (String) bootstrapContextRef.getReference().getProperty(TRANSACTION_SUPPORT);
        if (prop != null) {
            TransactionSupportLevel ddTransactionSupport = TransactionSupportLevel.valueOf(prop);
            if (transactionSupport == null)
                transactionSupport = ddTransactionSupport;
            else if (transactionSupport.ordinal() > ddTransactionSupport.ordinal())
                throw new IllegalArgumentException(ManagedConnectionFactory.class.getName() + ':' + transactionSupport
                                                   + ", " + Connector.class.getName() + ':' + ddTransactionSupport);
        }

        if (connectionFactoryTransactionSupport != null) {
            if (connectionFactoryTransactionSupport.ordinal() > transactionSupport.ordinal())
                throw new IllegalArgumentException(ManagedConnectionFactory.class.getName() + ':' + transactionSupport
                                                   + ", " + Connector.class.getName() + ':' + connectionFactoryTransactionSupport);
            else
                transactionSupport = connectionFactoryTransactionSupport;
        }

        // Otherwise choose NoTransaction
        return transactionSupport == null ? TransactionSupportLevel.NoTransaction : transactionSupport;
    }

    @Override
    public boolean getValidatingManagedConnectionFactorySupport() {
        return mcf instanceof ValidatingManagedConnectionFactory;
    }

    /**
     * Lazy initialization.
     * Precondition: invoker must have write lock on this ConnectionFactoryService
     *
     * @throws Exception if an error occurs
     */
    @Override
    protected void init() throws Exception {
        BootstrapContextImpl bootstrapContext = bootstrapContextRef.getServiceWithException();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "loading", mcfImplClassName);
        if (mcfImplClassName == null) // TODO: get the real config element name of the nested properties
            throw new IllegalArgumentException(Utils.getMessage("J2CA8504.incorrect.props.list", "properties", configElementName, id));
        mcf = (ManagedConnectionFactory) bootstrapContext.loadClass(mcfImplClassName).newInstance();
        bootstrapContext.configure(mcf, jndiName, properties, null, null, null);

        if (connectionManagerRef == null)
            conMgrSvc = ConnectionManagerService.createDefaultService(jndiName);
        else
            conMgrSvc = (ConnectionManagerService) PrivHelper.locateService(componentContext, CONNECTION_MANAGER);
        conMgrSvc.addObserver(this);
        isInitialized.set(true);

        conMgrSvc.addRaClassLoader(bootstrapContext.getRaClassLoader());
    }

    /**
     * Declarative Services method for setting the BootstrapContext reference
     *
     * @param ref reference to the service
     */
    protected void setBootstrapContext(ServiceReference<BootstrapContextImpl> ref) {
        bootstrapContextRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the ConnectionManagerService reference
     *
     * @param ref reference to the service
     */
    protected void setConnectionManager(ServiceReference<ConnectionManagerService> ref) {
        connectionManagerRef = ref;
    }

    /**
     * Declarative Services method for setting the SSLConfig reference
     *
     * @param ref reference to the service
     */
    protected void setSslConfig(ServiceReference<?> ref) {}

    /**
     * Declarative Services method for unsetting the BootstrapContext reference
     *
     * @param ref reference to the service
     */
    protected void unsetBootstrapContext(ServiceReference<BootstrapContextImpl> ref) {
        bootstrapContextRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the ConnectionManagerService reference
     *
     * @param ref reference to the service
     */
    protected void unsetConnectionManager(ServiceReference<ConnectionManagerService> ref) {
        connectionManagerRef = null;
    }

    /**
     * Declarative Services method for unsetting the SSLConfig reference
     *
     * @param ref reference to the service
     */
    protected void unsetSslConfig(ServiceReference<?> ref) {}

    /** {@inheritDoc} */
    @Override
    public void update(Observable observable, Object data) {
        destroyConnectionFactories(true);
    }

    /** {@inheritDoc} */
    @Override
    protected void checkAccess() throws ResourceException {
        BootstrapContextImpl bootstrapContext = bootstrapContextRef.getServiceWithException();
        ResourceAdapterMetaData metadata = bootstrapContext.getResourceAdapterMetaData();
        if (metadata != null && metadata.isEmbedded()) { // metadata is null for SIB/MQ
            ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            String currentApp = null;
            if (cData != null)
                currentApp = cData.getJ2EEName().getApplication();
            String adapterName = bootstrapContext.getResourceAdapterName();
            String embeddedApp = metadata.getJ2EEName().getApplication();
            Utils.checkAccessibility(jndiName, adapterName, embeddedApp, currentApp, false);
        }
    }
}
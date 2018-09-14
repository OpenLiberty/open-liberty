/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.BeanOFactory;
import com.ibm.ejs.container.BeanOFactory.BeanOFactoryType;
import com.ibm.ejs.container.ContainerEJBException;
import com.ibm.ejs.container.ContainerException;
import com.ibm.ejs.container.ContainerTx;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.container.EJBNotFoundException;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.EJSRemoteWrapper;
import com.ibm.ejs.container.EJSWrapperBase;
import com.ibm.ejs.container.HomeRecord;
import com.ibm.ejs.container.MessageEndpointCollaborator;
import com.ibm.ejs.container.PersistentTimer;
import com.ibm.ejs.container.PersistentTimerTaskHandler;
import com.ibm.ejs.container.TimerNpImpl;
import com.ibm.ejs.container.TimerNpRunnable;
import com.ibm.ejs.container.activator.ActivationStrategy;
import com.ibm.ejs.container.activator.Activator;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.ejs.csi.UOWControl;
import com.ibm.ejs.util.ByteArray;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.PassivationPolicy;
import com.ibm.ws.ejbcontainer.failover.SfFailoverKey;
import com.ibm.ws.ejbcontainer.jitdeploy.ClassDefiner;
import com.ibm.ws.metadata.ejb.WCCMMetaData;
import com.ibm.ws.uow.embeddable.SynchronizationRegistryUOWScope;
import com.ibm.wsspi.injectionengine.InjectionEngine;

/**
 * The interface between the core EJB container and the services provided by
 * the runtime environment that contains the EJB container.
 */
public interface EJBRuntime {
    /**
     * Returns a class loader for the server runtime. Specifically, this class
     * loader should not be an application class loader.
     *
     * @return the server class loader
     */
    ClassLoader getServerClassLoader();

    /**
     * Creates an activation strategy.
     *
     * @param activator the activator
     * @param type
     *            <ul>
     *            <li>{@link Activator#UNCACHED_ACTIVATION_STRATEGY} <li>{@link Activator#STATEFUL_ACTIVATE_ONCE_ACTIVATION_STRATEGY} <li>
     *            {@link Activator#STATEFUL_ACTIVATE_TRAN_ACTIVATION_STRATEGY} <li>{@link Activator#STATEFUL_ACTIVATE_SESSION_ACTIVATION_STRATEGY} <li>
     *            {@link Activator#OPTA_ENTITY_ACTIVATION_STRATEGY} <li>{@link Activator#OPTB_ENTITY_ACTIVATION_STRATEGY} <li>{@link Activator#OPTC_ENTITY_ACTIVATION_STRATEGY} <li>
     *            {@link Activator#ENTITY_SESSIONAL_TRAN_ACTIVATION_STRATEGY} <li>{@link Activator#READONLY_ENTITY_ACTIVATION_STRATEGY} </ul>
     * @return the activation strategy, or null
     */
    ActivationStrategy createActivationStrategy(Activator activator, int type, PassivationPolicy passivationPolicy);

    /**
     * Gets the slot size for the specified metadata class.
     */
    int getMetaDataSlotSize(Class<?> metaDataClass);

    /**
     * Initialize deferred EJB type indicated by the HomeRecord. This method is
     * only called when an EJB type whose initialization has been deferred is
     * first used. This method must be called with the application lock. This
     * method makes no assumptions about the thread context.
     *
     * @param hr this is the object that holds all information needed to
     *            initialize this EJB type.
     * @return HomeInternal (EJSHome) object representing the fully initialized EJB type.
     * @throws ContainerEJBException if an exception occurs while initializing
     */
    EJSHome initializeDeferredEJB(HomeRecord hr);

    /**
     * Notifies the runtime that the metadata for a bean of a particular type is
     * being processed. Only the type and J2EEName fields of the bean are valid
     * at the time this method is called.
     *
     * @param bmd the bean metadata
     * @param hasRemote <tt>true</tt> if the bean has a remote interface
     * @return a WCCMMetaData for this runtime
     * @throws ContainerException if the bean type is not allowed by this
     *             runtime environment.
     */
    WCCMMetaData setupBean(BeanMetaData bmd, boolean hasRemote) throws ContainerException;

    /**
     * Notifies the runtime that the metadata for an asynchronous bean has been
     * processed. If an exception is thrown, it is the callee's responsibility
     * to ensure that an appropriate message has been logged.
     *
     * @throws ContainerException if the container was unable to initialize the
     *             asynchronous component or if asynchronous methods are not allowed by this
     *             runtime environment
     */
    void setupAsync() throws ContainerException;

    /**
     * Schedules an asynchronous method to be called.
     *
     * @param wrapper the wrapper that originated the asynchronous method call
     * @param methodInfo the method info
     * @param methodId the method info id
     * @param args the arguments to the wrapper
     * @return the future to return to the client, or null if the method had a
     *         void return type
     * @throws EJBException if asynchronous EJBs are not allowed by this
     *             runtime environment or if any exception occurs while trying to schedule
     *             the async method
     * @throws RemoteException if the bean implements rmi remote business interface, wrap
     *             the exception in a RemoteException instead of EJBException
     */
    Future<?> scheduleAsync(EJSWrapperBase wrapper, EJBMethodInfoImpl methodInfo, int methodId, Object[] args) throws RemoteException;

    /**
     * Notifies the runtime that the metadata for a bean with timers has been
     * processed. If an exception is thrown, it is the callee's responsibility
     * to ensure that an appropriate message has been logged.
     *
     * @throws ContainerException if the container was unable to initialize the
     *             timer service component or if timers are not supported by this runtime
     *             environment
     */
    void setupTimers(BeanMetaData bmd) throws ContainerException;

    /**
     * Creates an EJB timer. If <tt>expiration</tt> is null, then the timer is
     * calendar-based, and <tt>schedule</tt> must be non-null. Otherwise, if
     * <tt>interval</tt> is <tt>0</tt>, then the timer is single-action.
     * Otherwise, the timer is interval-based.
     *
     * @param beanO the bean
     * @param expiration the initial expiration for a single-action or
     *            interval-based timer, or <tt>null</tt> for a calendar-based timer
     * @param interval the interval for an interval-based timer, or
     *            <tt>-1</tt> for a single-action or calendar-based timer
     * @param schedule the schedule for a calendar-based timer, or <tt>null</tt>
     *            for a single-action or interval-based timer
     * @param info application information to be delivered to the timeout
     *            method, or null
     * @param persistent true if the should be persistent
     */
    Timer createTimer(BeanO beanO, Date expiration, long interval, ScheduleExpression schedule, Serializable info, boolean persistent);

    /**
     * Creates the scheduler service implementation specific task handler
     * (Runnable) for non-persistent EJB timers. <p>
     *
     * The returned task handler interacts with the runtime provided
     * scheduler service and executes the timeout method when the
     * timer expiration is reached. <p>
     *
     * @param timer non-persistent EJB timer implementation
     *
     * @return the runtime specific timer task handler.
     */
    // RTC107334
    TimerNpRunnable createNonPersistentTimerTaskHandler(TimerNpImpl timer);

    /**
     * Platform specific method to obtain a persistent timer instance for
     * a deserialized timer task handler. <p>
     *
     * This method is intended for use by the timer task handler when it
     * has been deserialized and is invoking the timeout callback. <p>
     *
     * @param taskId unique identity of the Timer
     * @param j2eeName unique EJB name composed of Application-Module-Component
     * @param taskHandler persistent timer task handler associated with the taskId
     * @return persistent timer instance
     */
    PersistentTimer getPersistentTimer(long taskId, J2EEName j2eeName, PersistentTimerTaskHandler taskHandler);

    /**
     * Platform specific method to obtain a persistent timer without regard
     * to whether the timer still exists. <p>
     *
     * This method is intended for use by the Stateful activation code, when
     * a Stateful EJB is being activated and contained a Timer (that was
     * serialized as a TimerHandle). <p>
     *
     * A Timer will be returned even if there are no server features active
     * that enable EJB Timers. In this case, attempts to use the timer will
     * either fail (if EJB Timers are still not available) or will at that
     * time re-direct to the newly active EJB timer service. <p>
     *
     * @param taskId unique identity of the Timer
     *
     * @return Timer represented by the specified taskId
     */
    Timer getPersistentTimer(long taskId);

    /**
     * Platform specific method to restore a persistent timer from storage. <p>
     *
     * The EJB specification requires a NoSuchObjectLoaclException if the timer no longer
     * exists, but this method may return a Timer with cache information if the customer
     * has configured an option to allow stale data. <p>
     *
     * @return Timer represented by the specified taskId
     * @throws NoSuchObjectLocalException if the timer no longer exists in persistent store
     *             or has been cancelled in the current transaction.
     */
    Timer getPersistentTimerFromStore(long taskId) throws NoSuchObjectLocalException;

    /**
     * Gets all the timers associated with the specified bean.
     *
     * @param beanO the bean
     * @return the timers associated with the bean
     */
    Collection<Timer> getTimers(BeanO beanO);

    /**
     * Gets all the timers associated with the specified module.
     *
     * @param mmd the module
     * @return the timers associated with the module
     */
    Collection<Timer> getAllTimers(EJBModuleMetaDataImpl mmd);

    /**
     * Removes all timers associated with the specified bean.
     *
     * @param beanO the bean
     */
    void removeTimers(BeanO beanO);

    /**
     * Removes all timers associated with the specified application or module.
     *
     * @param j2eeName the application or module name
     */
    void removeTimers(J2EEName j2eeName);

    /**
     * Logs a warning if the current time is greater than the configured
     * lateTimerThreshold after the scheduled timer expiration.
     *
     * @param scheduledRuntime the scheduled expiration time
     * @param timerId unique timer identifier for the logged message
     * @param j2eeName unique identifier of the timer bean
     */
    void checkLateTimerThreshold(Date scheduledRunTime, String timerId, J2EEName j2eeName);

    /**
     * Returns the class definer used to define and load classes.
     */
    ClassDefiner getClassDefiner();

    /**
     * Create a new ObjectInputStream that loads classes from a class loader.
     */
    ObjectInputStream createObjectInputStream(InputStream in, ClassLoader loader) throws IOException;

    /**
     * Returns the declared created ContainerTx reference. Implementers should
     * create an instance of the necessary subclass if applicable.
     */
    ContainerTx createContainerTx(EJSContainer ejsContainer,
                                  boolean isLocal,
                                  SynchronizationRegistryUOWScope currTxKey,
                                  UOWControl uowCtrl);

    /**
     * Returns a reference to the injection engine; or null if the injection
     * engine is not currently enabled.
     */
    // F73338
    InjectionEngine getInjectionEngine();

    /**
     * Returns a reference to the Scheduled Executor Service object in use
     */
    // F73234
    ScheduledExecutorService getScheduledExecutorService();

    /**
     * Returns a reference to the deferrable Scheduled Executor Service if
     * available. Otherwise returns the non-deferrable one.
     */
    ScheduledExecutorService getDeferrableScheduledExecutorService();

    /**
     * Associate the server's identity with the local OS thread and return
     * a credential token that represents the current OS credential. The
     * token returned from this call should be passed to <code>pop</code>
     * in order to restore the OS identity that was current before this push.
     */
    Object pushServerIdentity();

    /**
     * Restore the OS thread identity that was active before the last push.
     * This method must only be called if {@link #pushServerIdentity} returned
     * non-null.
     *
     * @param credToken the result of {@link #pushServerIdentity}
     */
    void popServerIdentity(final Object credToken);

    /**
     * Returns the failover key which is used in various
     * maps to reference failover data for Stateful Bean Failover.
     */
    SfFailoverKey createFailoverKey(BeanId beanId);

    /**
     * Returns true if remote support is using org.omg.PortableServer.
     */
    boolean isRemoteUsingPortableServer();

    /**
     * Register the given object with the communications layer with the
     * specified key. <p>
     *
     * @param remoteObject the <code>EJSRemoteWrapper</code> object to register, must be
     *            ready to receive remote calls <p>
     *
     * @param key the <code>ByteArray</code> object
     *
     *
     * @exception CSIException thrown if the registration fails <p>
     */
    void registerServant(ByteArray key, EJSRemoteWrapper remoteObject) throws CSIException;

    /**
     * Register the given object with the communications layer with the
     * specified key. <p>
     *
     * This is a prefered method when registering a servant, since
     * the hashcode of the servant key does not need to be re-computed. <p>
     *
     * @param remoteObject the <code>EJSRemoteWrapper</code> object to register, must be
     *            ready to receive remote calls <p>
     *
     * @exception CSIException thrown if the registration fails <p>
     */
    void unregisterServant(EJSRemoteWrapper remoteObject) throws CSIException;

    /**
     * Return a client reference (stub) to a remote wrapper. The reference is
     * not guaranteed to extend or implement any specific interface, so the
     * caller is responsible for narrowing the reference as required.
     *
     * @param remoteObject the remote wrapper
     * @return the client reference (stub) connected to the remote wrapper
     */
    Object getRemoteReference(EJSRemoteWrapper remoteObject) throws NoSuchObjectException;

    /**
     * Returns the Cluster Identity for the specified J2EEName. <p>
     *
     * Null may be returned if the specified J2EEName is not WLMable. <p>
     *
     * @param j2eeName unique EJB name composed of Application-Module-Component.
     **/
    Object getClusterIdentity(J2EEName j2eeName);

    /**
     * Return the EJBJPAContainer object in the application server.
     */
    EJBJPAContainer getEJBJPAContainer();

    /**
     * Lookup a resource within the java: namespace. Supports the EJBContext method lookup,
     * but the resource name specified must start with java:. <p>
     *
     * @param name Resource name to lookup; starting with java:.
     * @param home Home of the bean performing the lookup.
     *
     * @throws IllegalArgumentException - The Container throws the exception
     *             if the given name does not match an entry within the
     *             component's environment.
     */
    Object javaColonLookup(String name, EJSHome home);

    /**
     * Returns the BeanOFactory for the the specified bean type. <p>
     *
     * @param type one of the supported types defined on {@link BeanOFactory}
     * @param bmd the corresponding BeanMetaData, or null for home of homes
     */
    // F88119
    BeanOFactory getBeanOFactory(BeanOFactoryType type, BeanMetaData bmd);

    /**
     * Loads the MessageEndpointFactory implementation class. This method
     * should not be called unless EJBRuntime.setupBean(MESSAGE_DRIVEN)
     * succeeds. <p>
     *
     * The returned class will vary depending on whether the MDB uses the
     * older style MessageListener or newer JCA MessageEndpoint. An exception
     * is thrown if the runtime does not support the appropriate implementation.
     *
     * @param bmd bean metadata that is used to determine if the MDB uses the
     *            older style MessageListener or newer JCA MessageEndpoint.
     */
    // F88119
    Class<?> getMessageEndpointFactoryImplClass(BeanMetaData bmd) throws ClassNotFoundException;

    /**
     * Loads the MessageEndpoint implementation class. This method should not
     * be called unless EJBRuntime.setupBean(MESSAGE_DRIVEN) succeeds.
     *
     * The returned class will vary depending on whether the MDB uses the
     * older style MessageListener or newer JCA MessageEndpoint. An exception
     * is thrown if the runtime does not support the appropriate implementation.
     *
     * @param bmd bean metadata that is used to determine if the MDB uses the
     *            older style MessageListener or newer JCA MessageEndpoint.
     * @return the MessageListener impl class or null to indicate JCA MessageEndpoint.
     */
    // F88119
    Class<?> getMessageEndpointImplClass(BeanMetaData bmd) throws ClassNotFoundException;

    /**
     * Retrieves the MessageEndpointCollaborator instance.
     *
     * @param bmd The bean metadata that is used to determine if the MDB uses the
     *            older style MessageListener or newer JCA MessageEndpoint.
     *
     * @return The MessageEndpointCollaborator instance.
     */
    public MessageEndpointCollaborator getMessageEndpointCollaborator(BeanMetaData bmd);

    /**
     * Determines the message destination JNDI name based on information from
     * the BeanMetaData and then updates {@link BeanMetaData#ivMessageDestinationJndiName}
     * with the final value. <p>
     *
     * This method must be called AFTER {@link BeanMetaData#ivMessageDestinationJndiName}
     * has been set from the message-driven bean binding in the ejb-jar binding file and
     * {@link BeanMetaData#ivActivationConfig} has been set based on information from the
     * activation spec in both XML and via annotations
     *
     * And this method must be called BEFORE either the message-destination-link map is
     * built in the injection engine or the MDB is activated. <p>
     *
     * @param bmd
     */
    void resolveMessageDestinationJndiName(BeanMetaData bmd);

    /**
     * Determines if the EJB runtime supports remote interfaces.
     *
     * @return true if the EJB runtime supports remote interfaces; otherwise false.
     */
    boolean isRemoteSupported();

    /**
     * Used to determine if looking up / injecting a remote interface is a
     * supported operation.
     *
     * @throws EJBNotFoundException thrown if looking up or injecting a remote
     *             interface is unsupported
     */
    void checkRemoteSupported(EJSHome home, String interfaceName) throws EJBNotFoundException;

    /**
     * Perform any platform specific processing that should be done immediately
     * prior to invoking the MDB method. This may include calling test
     * framework hooks or logging performance metrics (PMI)
     */
    void notifyMessageDelivered(Object proxy);

    /**
     * Method to get the XAResource corresponding to an ActivationSpec from the RRSXAResourceFactory
     *
     * @param bmd The BeanMetaData object for the MDB being handled
     * @param xid Transaction branch qualifier
     * @return the XAResource
     */
    public XAResource getRRSXAResource(BeanMetaData bmd, Xid xid) throws XAResourceNotAvailableException;
}

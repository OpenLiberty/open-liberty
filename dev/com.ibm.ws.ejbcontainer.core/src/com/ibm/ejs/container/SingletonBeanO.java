/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import static com.ibm.ejs.container.ContainerProperties.PersistentTimerSingletonDeadlockTimeout;
import static com.ibm.ejs.container.ContainerProperties.UseFairSingletonLockingPolicy;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.EnterpriseBean;
import javax.ejb.IllegalLoopbackException;
import javax.ejb.LockType;
import javax.ejb.RemoveException;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;

import com.ibm.ejs.container.interceptors.InterceptorMetaData;
import com.ibm.ejs.container.interceptors.InterceptorProxy;
import com.ibm.ejs.container.interceptors.InvocationContextImpl;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.ejs.j2c.HandleList;
import com.ibm.ejs.j2c.HandleListInterface;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.CallbackKind;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.traceinfo.ejbcontainer.TEBeanLifeCycleInfo;

/**
 * SingletonBeanO is a final class used for the SessionContext for a EJB 3.1
 * Singleton bean. It used for both bean managed transaction and container
 * managed transaction. Also, it used for both bean and container managed
 * concurrency control.
 * <p>
 * Note, the EJB 2.1 Remote and Local client view is not supported for singleton
 * session beans. You can find this in "3.6 Remote and Local Client View of
 * Session Beans Written to the EJB 2.1 Client View API" section of EJB 3.1
 * specification.
 */
final public class SingletonBeanO extends SessionBeanO {
    private static final String CLASS_NAME = SingletonBeanO.class.getName();
    private static final TraceComponent tc = Tr.register(SingletonBeanO.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * Legal state values for Singleton that are inherited from superclass.
     */
    // DESTROYED  = 0; after PreDestroy occurs or initial state, defined in SessionBeanO.
    // PRE_CREATE = 1; is prior to PostConstruct and defined in SessionBeanO.
    // CREATING   = 2; in PostConstruct and defined in SessionBeanO.

    /**
     * Additional legal state values that are declared in this class.
     */
    public static final int METHOD_READY = 3; // method ready state of Singleton.
    public static final int PRE_DESTROY = 4; // during PreDestroy, destroyed state once PreDestroy is done.

    /**
     * This table translates state of bean into printable string.
     */
    static final String StateStrs[] = { "DESTROYED", // 0
                                        "PRE_CREATE", // 1
                                        "CREATING", // 2
                                        "METHOD_READY", // 3
                                        "PRE_DESTROY" // 4
    };

    /**
     * Set to true if and only if container managed concurrency control is used
     * for this Singleton instance.
     */
    final private boolean ivContainerManagedConcurrency;

    /**
     * ivLock, ivReadLock, and ivWriteLock are set to a non-null reference only
     * when ivContainerManagedConcurrency is set to true.
     */
    final private ReentrantReadWriteLock ivLock;
    final private ReadLock ivReadLock;
    final private WriteLock ivWriteLock;

    /**
     * Set to true if and only if bean managed transaction is used for this
     * Singleton instance.
     */
    final private boolean ivBMT;

    /**
     * Count of threads that are currently in the method TimerService.getTimers().
     */
    private final AtomicInteger ivInGetTimers = new AtomicInteger();

    /**
     * Create new <code>SingletonBeanO</code> instance, which is the
     * SessionContext object for a EJB 3.1 Singleton bean instance.
     * <p>
     * <dl>
     * <dt>pre-condition
     * <dd>
     * EJSHome is the only caller SingletonBeanOFactory create method and it
     * ensures the factory create is never called more than once per Singleton
     * class.
     * </dl>
     * <p>
     *
     * @param c
     *            is the EJSContainer instance for this bean.
     * @param h
     *            is the home for this Singleton bean.
     */
    public SingletonBeanO(EJSContainer c, EJSHome h) {
        super(c, h);

        // Determine if BMT is used.
        BeanMetaData bmd = home.beanMetaData;
        ivBMT = bmd.usesBeanManagedTx;

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled()) {
            if (ivBMT) {
                Tr.debug(tc, bmd.j2eeName + " - initialized for bean managed transaction");
            } else {
                Tr.debug(tc, bmd.j2eeName + " - initialized for container managed transaction");
            }
        }

        // If concurrency control is container managed, then create the
        // lock objects needed for concurrency control.
        ivContainerManagedConcurrency = !bmd.ivSingletonUsesBeanManagedConcurrency;
        if (ivContainerManagedConcurrency) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, bmd.j2eeName + " - initializing for container managed concurrency control");
            }

            // Create the lock objects needed for container managed
            // concurrency control for this EJB 3.1 Singleton object.
            // Pass the system property UseFairSingletonLockPolicy to the ReentrantReadWriteLock constructor,
            // The default for the system property is false if not set specifically by the customer
            // which matches the ReentrantReadWriteLock constructor default of false which uses non-fair policy.  F743-9002
            ivLock = new ReentrantReadWriteLock(UseFairSingletonLockingPolicy);
            ivReadLock = ivLock.readLock();
            ivWriteLock = ivLock.writeLock();
        } else {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, bmd.j2eeName + " - initializing for bean managed concurrency control");
            }

            // It is bean managed concurrency control, so we do
            // not need to create any lock objects.
            ivLock = null;
            ivReadLock = null;
            ivWriteLock = null;
        }
    }

    @Override
    protected String getStateName(int state) {
        return StateStrs[state];
    }

    // F743-1751
    /**
     * Invoke PostConstruct or PreDestroy interceptors associated with this bean
     * using the transaction and security context specified in the method info.
     *
     * @param proxies the non-null reference to InterceptorProxy array that
     *            contains the PostConstruct interceptor methods to invoke.
     * @param methodInfo the method info for transaction and security context
     */
    private void callTransactionalLifecycleInterceptors(InterceptorProxy[] proxies, int methodId) throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        LifecycleInterceptorWrapper wrapper = new LifecycleInterceptorWrapper(container, this);

        EJSDeployedSupport s = new EJSDeployedSupport();
        // F743761.CodRv - Exceptions from lifecycle callback interceptors do not
        // throw application exceptions.
        s.ivIgnoreApplicationExceptions = true;

        try {
            container.preInvokeForLifecycleInterceptors(wrapper, methodId, s, this);

            // F743-1751CodRev - Inline callLifecycleInterceptors.  We need to
            // manage HandleList separately.

            if (isTraceOn) // d527372
            {
                if (TEBeanLifeCycleInfo.isTraceEnabled())
                    TEBeanLifeCycleInfo.traceEJBCallEntry(LifecycleInterceptorWrapper.TRACE_NAMES[methodId]);

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "callLifecycleInterceptors");
            }

            InvocationContextImpl<?> inv = getInvocationContext();
            BeanMetaData bmd = home.beanMetaData;
            inv.doLifeCycle(proxies, bmd._moduleMetaData); // F743-14982
        } catch (Throwable t) {
            s.setUncheckedLocalException(t);
        } finally {
            if (isTraceOn && TEBeanLifeCycleInfo.isTraceEnabled()) {
                TEBeanLifeCycleInfo.traceEJBCallExit(LifecycleInterceptorWrapper.TRACE_NAMES[methodId]);
            }

            container.postInvokeForLifecycleInterceptors(wrapper, methodId, s);
        }
    }

    /**
     * Initialize this <code>SingletonBeanO</code>, executing lifecycle callback
     * methods, etc.
     * <p>
     * This code was broken out of the Constructor so that this BeanO instance
     * could be placed on the ContainerTx as the CallbackBeanO for the lifecycle
     * callback methods. <p>
     */
    @Override
    protected final void initialize(boolean reactivate) throws RemoteException, InvocationTargetException {
        // ---------------------------------------------------------
        // Set state to PRE_CREATE so only methods that we permitted
        // to be called during dependency injection are allowed and
        // those methods that are not allowed (e.g. SessionContext.lookup)
        // result in IllegalStateException.
        // ---------------------------------------------------------
        state = PRE_CREATE;

        // Not a home bean, so set the BeanId for the SLSB.
        BeanMetaData bmd = home.beanMetaData;
        setId(home.ivStatelessId);

        // We may have InterceptorMetaData to process.
        InterceptorMetaData imd = bmd.ivInterceptorMetaData;

        // F743-21481
        // Created new try/catch block to ensure that both the creation of the interceptor
        // classes and the injecting of the bean class itself inside run inside the
        // scope of our location transaction.  Previously, the creation of the
        // interceptors and the injection of those interceptors took place at
        // different points in time, and so the creation of the interceptor
        // classes was not under the scope of the local tran.  Now, however, the
        // creation of the interceptor classes and the injection into them occur
        // at the same time, and so it must be covered by the local tran.
        //
        // d578360 - For other bean types, the local transaction surrounds
        // injection methods and PostConstruct lifecycle methods.  Because
        // singleton lifecycle methods run in a defined transaction context,
        // we only need the local transaction for injection methods.
        CallbackContextHelper contextHelper = new CallbackContextHelper(this);
        contextHelper.begin(CallbackContextHelper.Tx.LTC,
                            CallbackContextHelper.Contexts.CallbackBean);
        try {
            createInterceptorsAndInstance(contextHelper);

            // Singleton do not support 2.1 client view, so we do not need to ever
            // call setSessionContext method, so just perform dependency injection.
            // Note that dependency injection must occur while in PRE_CREATE state.
            injectInstance(ivManagedObject, ivEjbInstance, this);

        } finally {
            contextHelper.complete(true);
        }

        // ---------------------------------------------------------
        // Now that dependencies injection has occured, change state
        // to CREATING state to allow additional methods to be called by the
        // PostConstruct interceptor methods (e.g. lookup method).
        // ---------------------------------------------------------
        setState(CREATING);

        //-------------------------------------------------------------------
        // Call PostConstruct interceptor method if necessary. Note, Singleton
        // does not have any EJB 2.1 client view, so not reason to worry about
        // calling ejbCreate method.
        //-------------------------------------------------------------------
        if (imd != null && ivCallbackKind == CallbackKind.InvocationContext) // d402681 // d571981
        {
            // This is a 3.1 Singleton that may have one or more PostConstruct interceptors
            // methods. Invoke PostContruct interceptors if there is at least 1
            // PostConstruct interceptor.

            InterceptorProxy[] proxies = imd.ivPostConstructInterceptors;
            if (proxies != null) {
                callTransactionalLifecycleInterceptors(proxies, LifecycleInterceptorWrapper.MID_POST_CONSTRUCT); // F743-1751
            }
        }

        //---------------------------------------------------------
        // Now that create has completed, change state to METHOD_READY.
        //---------------------------------------------------------
        setState(METHOD_READY);
    }

    @Override
    HandleList getHandleList(boolean create) {
        HandleList hl;
        if (state == PRE_CREATE || state == CREATING) {
            // We are single-threaded during initialize(), so we can use the
            // per-bean handle list.
            hl = super.getHandleList(create);
        } else {
            EJSDeployedSupport s = EJSContainer.getMethodContext();

            // Singleton bean methods are reentrant and concurrent, so we cannot
            // use a per-bean handle list.  Instead, store the handle list on the
            // method context.  Note that this is also applicable to destroy,
            // which uses a method context as part of pre/post invoke.
            hl = s.ivHandleList;
            if (hl == null && create) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "getHandleList: created " + hl);

                hl = new HandleList();
                s.ivHandleList = hl;
            }
        }

        return hl;
    }

    @Override
    HandleListInterface reAssociateHandleList() {
        return HandleListProxy.INSTANCE;
    }

    @Override
    void parkHandleList() {
        if (state == PRE_CREATE || state == CREATING) {
            // The initialize() handle list is single use only.
            destroyHandleList();
        } else {
            EJSDeployedSupport s = EJSContainer.getMethodContext();

            HandleList hl = s.ivHandleList;
            if (hl != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "parkHandleList: closing " + hl);
                hl.close();
            }
        }
    }

    /**
     * Acquire the lock required for the method being invoked.
     *
     * @see com.ibm.ejs.container.BeanO#preInvoke(com.ibm.ejs.container.EJSDeployedSupport, com.ibm.ejs.container.ContainerTx)
     */
    @Override
    public Object preInvoke(EJSDeployedSupport s, ContainerTx tx) throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "preInvoke : " + this);

        // If this is container managed concurrency control, then use
        // then get the lock specified by the lock type found in the
        // EJB method info object.
        s.ivLockAcquired = false; // d571981
        if (ivContainerManagedConcurrency) {
            // Get the lock type to use for the method being invoked.
            // and AccessTimeout value to use.
            EJBMethodInfoImpl mInfo = s.methodInfo;
            LockType lockType = mInfo.ivLockType;

            // Ensure we are not trying to upgrade from a READ to a WRITE lock. We
            // must throw an exception if trying to upgrade from READ to WRITE.
            if (lockType == LockType.WRITE && ivLock.isWriteLockedByCurrentThread() == false) {
                // Requesting write lock and write lock is not currently held by the
                // calling thread. So check whether calling thread holds any read locks.
                if (ivLock.getReadHoldCount() > 0) {
                    throw new IllegalLoopbackException("A loopback method call is not allowed to upgrade from a READ to a WRITE lock.");
                }
            }

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "preInvoke attempting to acquire a " + mInfo.ivLockType.name() + " lock. " + ivLock.toString());

            // PMI startLockTime for read and write locks  F743-9002
            long lockStartTime = 0;
            int lockStatType = 0;

            try {
                long timeout = mInfo.ivAccessTimeout;

                // For a persistent timeout callback that will wait a significant amount of
                // time for the lock, perform a quick lock attempt in an effort to detect a
                // deadlock that can occur between the singleton lock and the timer database
                // if there is a concurrent thread calling getTimers().             RTC126471
                if (s.isPersistentTimeoutGlobalTx &&
                    PersistentTimerSingletonDeadlockTimeout >= 0 &&
                    (timeout == -1 || timeout > PersistentTimerSingletonDeadlockTimeout)) {
                    if (lockType == LockType.READ) {
                        if (pmiBean != null) {
                            lockStatType = EJBPMICollaborator.READ_LOCK_TIME;
                            lockStartTime = pmiBean.initialTime(lockStatType);
                        }
                        s.ivLockAcquired = ivReadLock.tryLock(PersistentTimerSingletonDeadlockTimeout, TimeUnit.MILLISECONDS);
                    } else {
                        if (pmiBean != null) {
                            lockStatType = EJBPMICollaborator.WRITE_LOCK_TIME;
                            lockStartTime = pmiBean.initialTime(lockStatType);
                        }
                        s.ivLockAcquired = ivWriteLock.tryLock(PersistentTimerSingletonDeadlockTimeout, TimeUnit.MILLISECONDS);
                    }

                    if (s.ivLockAcquired) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "preInvoke acquired a " + mInfo.ivLockType.name() + " lock. " + ivLock.toString());
                    } else {
                        // If the lock was not obtained and another thread is in getTimer,
                        // then a deadlock is very likely, so abort the timeout callback
                        // which will free up the database row lock.
                        if (ivInGetTimers.get() > 0) {
                            if (pmiBean != null) {
                                pmiBean.countCancelledLocks();
                            }
                            throw new ConcurrentAccessTimeoutException("preInvoke timed out in attempt to acquire a "
                                                                       + mInfo.ivLockType.name() + " lock for method signature = " + mInfo.getMethodSignature()
                                                                       + ".  Dead lock detected with timer database.");
                        }

                        // Otherwise, subtract the time waited, and proceed normally
                        if (timeout != -1) {
                            timeout = Math.max(0, timeout - PersistentTimerSingletonDeadlockTimeout);
                        }
                    }
                }

                // If the lock is not acquired, then either this is not for a persistent
                // timeout callback, or there is not a concurrent thread in getTimers(),
                // so just attempt to obtain a lock using the configured access timeout.
                if (!s.ivLockAcquired) {
                    if (timeout == -1) // -1 means wait forever     F743-21028.5
                    {
                        if (lockType == LockType.READ) {
                            if (pmiBean != null && lockStartTime == 0) // F743-9002
                            {
                                lockStatType = EJBPMICollaborator.READ_LOCK_TIME;
                                lockStartTime = pmiBean.initialTime(lockStatType);
                            }
                            ivReadLock.lock(); // d571981
                        } else {
                            if (pmiBean != null && lockStartTime == 0)// F743-9002
                            {
                                lockStatType = EJBPMICollaborator.WRITE_LOCK_TIME;
                                lockStartTime = pmiBean.initialTime(lockStatType);
                            }
                            ivWriteLock.lock(); // d571981
                        }

                        s.ivLockAcquired = true;
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "preInvoke acquired a " + mInfo.ivLockType.name() + " lock. " + ivLock.toString());
                    } else {
                        if (lockType == LockType.READ) {
                            if (pmiBean != null && lockStartTime == 0) // F743-9002
                            {
                                lockStatType = EJBPMICollaborator.READ_LOCK_TIME;
                                lockStartTime = pmiBean.initialTime(lockStatType);
                            }
                            s.ivLockAcquired = ivReadLock.tryLock(timeout, TimeUnit.MILLISECONDS); // d571981
                        } else {
                            if (pmiBean != null && lockStartTime == 0) // F743-9002
                            {
                                lockStatType = EJBPMICollaborator.WRITE_LOCK_TIME;
                                lockStartTime = pmiBean.initialTime(lockStatType);
                            }
                            s.ivLockAcquired = ivWriteLock.tryLock(timeout, TimeUnit.MILLISECONDS); // d571981
                        }

                        if (s.ivLockAcquired) {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "preInvoke acquired a " + mInfo.ivLockType.name() + " lock. " + ivLock.toString());
                        } else {
                            if (pmiBean != null) // F743-9002
                            {
                                pmiBean.countCancelledLocks();
                            }

                            throw new ConcurrentAccessTimeoutException("preInvoke timed out in attempt to acquire a "
                                                                       + mInfo.ivLockType.name() + " lock for method signature = " + mInfo.getMethodSignature()
                                                                       + ".  Access timeout value = " + timeout + " milli-seconds");
                        }
                    }
                }
            } catch (InterruptedException e) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "InterruptedException prevented lock from being acquired.");

                if (pmiBean != null) // F743-9002
                {
                    pmiBean.countCancelledLocks();
                }

                throw ExceptionUtil.EJBException("InterruptedException prevented lock from being acquired.", e);
            } finally {
                // F743-9002
                // Calculate the time to obtain the lock and adjust the pmiCookie
                // used for methodRT PMI counter to exclude the lock time.
                if (pmiBean != null) {
                    // d648142.2
                    long lockDuration = pmiBean.finalTime(lockStatType, lockStartTime);
                    if (lockDuration > 0) {
                        s.pmiCookie += lockDuration; // d724734
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "preInvoke");

        return ivEjbInstance;
    }

    /**
     * Release lock acquired by preInvoke.
     *
     * @see com.ibm.ejs.container.BeanO#postInvoke(int, com.ibm.ejs.container.EJSDeployedSupport)
     */
    @Override
    public void postInvoke(int id, EJSDeployedSupport s) throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "postInvoke");
        }

        // Release lock acquired if container managed concurrency control.
        if (ivContainerManagedConcurrency && s.ivLockAcquired) {
            // Get the lock type to use for the method being invoked.
            EJBMethodInfoImpl mInfo = s.methodInfo;
            LockType lockType = mInfo.ivLockType;

            if (lockType == LockType.READ) {
                ivReadLock.unlock();

                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "postInvoke released read lock: " + ivLock.toString());
                }
            } else {
                ivWriteLock.unlock();

                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "postInvoke released write lock: " + ivLock.toString());
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "postInvoke");
        }
    }

    /**
     * For Singleton objects, getRollbackOnly is only valid while in the method
     * ready state and there is a valid TX context (e.g. the method being invoked
     * has one of NotSupported, Supports or Never as its transaction attribute).
     *
     * @see com.ibm.ejs.container.BeanO#getRollbackOnly()
     */
    @Override
    public boolean getRollbackOnly() {
        if (ivBMT) {
            throw new IllegalStateException("getRollbackOnly not allowed for bean managed transaction.");
        } else if (state > PRE_CREATE) {
            // We must be in PostConstruct, method ready, or PreDestroy state. Have
            // past dependency injection. Have the super class do thegetRollbackOnly
            // processing. It will throw IllegalStateException if method TX
            // attribute caused method to be invoked without a global TX.
            return super.getRollbackOnly();
        } else {
            throw new IllegalStateException("setRollbackOnly operation not allowed in current state: " + StateStrs[state]);
        }
    }

    /**
     * For Singleton objects, setRollbackOnly is allowed once we are past
     * dependency injection (PRE_CREATE state) and there is a valid TX context
     * (e.g. the method being invoked has one of NotSupported, Supports or
     * Never as its transaction attribute).
     *
     * @see com.ibm.ejs.container.BeanO#setRollbackOnly()
     */
    @Override
    public void setRollbackOnly() {
        if (ivBMT) {
            throw new IllegalStateException("getRollbackOnly not allowed for bean managed transaction.");
        } else if (state > PRE_CREATE) {
            // We must be in PostConstruct, method ready, or PreDestroy state.
            // Have the super class do the setRollbackOnly processing. It will
            // throw IllegalStateException if method TX attribute
            // caused method to be invoked without a global TX.
            super.setRollbackOnly();
        } else {
            throw new IllegalStateException("setRollbackOnly operation not allowed in current state: " + StateStrs[state]);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ejs.container.SessionBeanO#getUserTransaction()
     */
    @Override
    final public UserTransaction getUserTransaction() throws IllegalStateException {
        if (ivBMT) {
            if (state <= PRE_CREATE) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Incorrect state: " + getStateName(state));
                throw new IllegalStateException(getStateName(state));
            }

            return UserTransactionWrapper.INSTANCE; // d631349
        } else {
            throw new IllegalStateException("UserTransaction not allowed for Singleton with container managed transactions."); // F743-1751
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ejs.container.BeanO#activate(com.ibm.ejs.container.BeanId,
     * com.ibm.ejs.container.ContainerTx)
     */
    @Override
    final public void activate(BeanId arg0, ContainerTx arg1) throws RemoteException {
        // ---------------------------------------------------
        // Activating a Singleton session bean is a no-op
        // ---------------------------------------------------
    }

    /**
     * Determines if timer service methods are allowed based on the current state
     * of this bean instance. This includes the methods on the javax.ejb.Timer
     * and javax.ejb.TimerService interfaces.
     * <P>
     *
     * Must be called by all Timer Service Methods to insure EJB Specification
     * compliance.
     * <p>
     *
     * Note: This method does not apply to the EJBContext.getTimerService()
     * method, as getTimerService may be called for more bean states.
     * getTimerServcie() must provide its own checking.
     * <p>
     *
     * @exception IllegalStateException
     *                If this instance is in a state that does not allow timer
     *                service method operations.
     *
     * @see com.ibm.ejs.container.BeanO#checkTimerServiceAccess()
     **/
    @Override
    final public void checkTimerServiceAccess() throws IllegalStateException {
        // Do not allow if prior to PostConstruct or after PreDestroy.
        if (state < CREATING) {
            IllegalStateException ise;

            ise = new IllegalStateException("Singleton Session Bean: Timer Service " // d571981
                                            + "methods not allowed from state = " + getStateName(state));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "checkTimerServiceAccess: " + ise);

            throw ise;
        }
    }

    /**
     * Get access to the EJB Timer Service.
     * <p>
     *
     * @return The EJB Timer Service.
     *
     * @exception IllegalStateException
     *                The Container throws the exception if the instance is not
     *                allowed to use this method.
     **/
    // LI2281.07
    @Override
    final public TimerService getTimerService() throws IllegalStateException {
        // Do not allow if prior to PostConstruct or after PreDestroy.
        if (state < CREATING) {
            IllegalStateException ise;

            ise = new IllegalStateException("Singleton: getTimerService not " + "allowed from state = " + getStateName(state));

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getTimerService: " + ise);

            throw ise;
        }

        return super.getTimerService();
    }

    @Override
    public Collection<Timer> getTimers() throws IllegalStateException, EJBException {
        // Keep track of when this method is called; to kick out timeout methods
        // that will hold locks on the timer database.                   RTC126471
        try {
            ivInGetTimers.incrementAndGet();
            return super.getTimers();
        } finally {
            ivInGetTimers.decrementAndGet();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ejs.container.BeanO#commit(com.ibm.ejs.container.ContainerTx)
     */
    @Override
    final public void commit(ContainerTx arg0) throws RemoteException {
        // ---------------------------------------------------
        // A Singleton session bean must never be committed since not enlisted.
        // ---------------------------------------------------

        throw new InvalidBeanOStateException(StateStrs[state], "NONE" + ": Singleton commit not allowed");
    }

    /**
     * Destroy this <code>CMSingletonBeanO</code> instance. Note, for Singleton
     * EJBs, the EJB 3.1 specification requires that the Singleton should NOT be
     * discarded when as a result of a unchecked or system exception. Also, the
     * only time a Singleton should be destroyed is when the application is being
     * stopped.
     */
    @Override
    public final synchronized void destroy() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "destroy");
        }

        if (state == DESTROYED) {
            return;
        }

        // For Singleton, 'destroy' is where the bean is removed and destroyed.
        // Remove time should include calling any lifecycle callbacks.   d626533.1
        long removeStartTime = -1;
        if (pmiBean != null) {
            removeStartTime = pmiBean.initialTime(EJBPMICollaborator.REMOVE_RT);
        }

        setState(PRE_DESTROY);

        if (ivCallbackKind == CallbackKind.InvocationContext) {
            InterceptorMetaData imd = home.beanMetaData.ivInterceptorMetaData;
            InterceptorProxy[] proxies = imd.ivPreDestroyInterceptors;
            if (proxies != null) {
                try {
                    // There is no need to push/pop CMD because singletons are
                    // always destroyed as part of uninstallBean, which uses
                    // ComponentMetaDataCollaborator.
                    callTransactionalLifecycleInterceptors(proxies, LifecycleInterceptorWrapper.MID_PRE_DESTROY); // F743-1751
                } catch (Throwable t) {
                    FFDCFilter.processException(t, CLASS_NAME + ".destroy", "824", this);

                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "destroy caught exception", new Object[] { this, t });
                    }
                }
            }
        }

        // Change the state to destroyed.
        setState(DESTROYED);

        // Release any JCDI creational contexts that may exist.         F743-29174
        this.releaseManagedObjectContext();

        // For Singleton, 'destroy' is where the bean is removed and destroyed.
        // Update both counters and end remove time.                     d626533.1
        if (pmiBean != null) {
            // TODO : When PMI stops counting beanDestroyed as remove... then add next line
            //         pmiBean.beanRemoved();
            pmiBean.beanDestroyed();
            pmiBean.finalTime(EJBPMICollaborator.REMOVE_RT, removeStartTime);
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "destroy");
        }
    } // destroy

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ejs.container.BeanO#discard()
     */
    @Override
    public void discard() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "discard: " + getStateName(state));

        // d639281 - Singleton instances are never actually discarded unless a
        // failure occurs during initialize().
        if (state == PRE_CREATE || state == CREATING) {
            setState(DESTROYED);

            // Release any JCDI creational contexts that may exist.      F743-29174
            this.releaseManagedObjectContext();

            if (pmiBean != null) {
                pmiBean.discardCount(); // F743-27070
                pmiBean.beanDestroyed();
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "discard");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ejs.container.BeanO#enlist(com.ibm.ejs.container.ContainerTx)
     */
    @Override
    final public boolean enlist(ContainerTx arg0) throws RemoteException {
        // -----------------------------------------------
        // Singleton is never enlisted in a TX.
        // -----------------------------------------------
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ejs.container.BeanO#getEnterpriseBean()
     */
    @Override
    final public EnterpriseBean getEnterpriseBean() throws RemoteException {
        // --------------------------------------------------------------
        // A Singleton session bean is never a EnterpriseBean,
        // so this method must never be called.
        // --------------------------------------------------------------

        throw new InvalidBeanOStateException(StateStrs[state], "NONE" + ": Singleton getEnterpriseBean not allowed");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ejs.container.BeanO#isDiscarded()
     */
    @Override
    final public boolean isDiscarded() {
        // Singleton is never discarded.
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ejs.container.BeanO#isRemoved()
     */
    @Override
    final public boolean isRemoved() {
        // ---------------------------------------------------
        // Singleton session beans are never marked removed.
        // ---------------------------------------------------
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ejs.container.BeanO#passivate()
     */
    @Override
    final public void passivate() throws RemoteException {
        // ----------------------------------------------------
        // A Singleton session bean must never be passivated.
        // ----------------------------------------------------

        throw new InvalidBeanOStateException(StateStrs[state], "NONE" + ": Singleton passivate not allowed");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ejs.container.BeanO#remove()
     */
    @Override
    final public void remove() throws RemoteException, RemoveException {
        // ---------------------------------------------------
        // Singleton session beans are never marked removed.
        // ---------------------------------------------------
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.ibm.ejs.container.BeanO#rollback(com.ibm.ejs.container.ContainerTx)
     */
    @Override
    final public void rollback(ContainerTx arg0) throws RemoteException {
        // ---------------------------------------------------
        // A Singleton session bean must never be rolled back since not enlisted.
        // ---------------------------------------------------

        throw new InvalidBeanOStateException(StateStrs[state], "NONE" + ": Singleton rollback not allowed");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ejs.container.BeanO#store()
     */
    @Override
    final public void store() throws RemoteException {
        // --------------------------------------------------------------
        // A Singleton session bean never supports EJB 2.1 client view,
        // so this method must never be called.
        // --------------------------------------------------------------

        throw new InvalidBeanOStateException(StateStrs[state], "NONE" + ": Singleton store not allowed");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ejs.container.BeanO#beforeCompletion()
     */
    @Override
    final public void beforeCompletion() throws RemoteException {
        // --------------------------------------------------------------
        // A Singleton session bean is never enlisted in a transaction,
        // so this method must never be called.
        // --------------------------------------------------------------

        throw new InvalidBeanOStateException(StateStrs[state], "NONE" + ": Singleton beforeCompletion is not allowed");
    }

    /**
     * Stateless session beans do not implement this method since they
     * are created on demand by the container, not by the client.
     *
     * @see com.ibm.ejs.container.BeanO#postCreate(boolean)
     */
    @Override
    final public void postCreate(boolean arg0) {
        // F743-1751 - This method would only be called via a create method on a
        // generated home, but singletons are always created directly.
        throw new UnsupportedOperationException();
    }

    // -------------------------------------------------------------------
    // SessionContext method overrides
    // -------------------------------------------------------------------

    /**
     * getCallerPrincipal is invalid if called within the setSessionContext
     * method
     */
    @Override
    final public Principal getCallerPrincipal() {
        if (state == METHOD_READY) // d571981
        {
            return super.getCallerPrincipal();
        } else {
            throw new IllegalStateException("For Singleton, getCallerPrincipal only allowed while in METHOD_READY state");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ejs.container.BeanO#isCallerInRole(java.lang.String)
     */
    @Override
    final public boolean isCallerInRole(String roleName) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "isCallerInRole, role = " + roleName + ", state = " + StateStrs[state]);
        }

        if (state == METHOD_READY) // d571981
        {
            boolean inRole = super.isCallerInRole(roleName, ivEjbInstance);
            if (isTraceOn && tc.isEntryEnabled()) {
                Tr.exit(tc, "isCallerInRole: " + inRole);
            }
            return inRole;
        } else {
            throw new IllegalStateException("For Singleton, isCallerInRole only allowed while in METHOD_READY state");
        }
    }

    /**
     * @deprecated In section 17.2.5 of EJB 3.1 specification, it says "The
     *             getCallerIdentity() and isCallerInRole(Identity role) methods
     *             were @deprecated in EJB 1.1. The Bean Provider must use the
     *             getCallerPrincipal() and isCallerInRole(String roleName)
     *             methods for new enterprise beans.
     */
    @Override
    @Deprecated
    final public java.security.Identity getCallerIdentity() {
        throw new IllegalStateException("getCallerIdentity is deprecated, getCallerPrincipal must be used.");
    }

    /**
     * @deprecated In section 17.2.5 of EJB 3.1 specification, it says "The
     *             getCallerIdentity() and isCallerInRole(Identity role) methods
     *             were @deprecated in EJB 1.1. The Bean Provider must use the
     *             getCallerPrincipal() and isCallerInRole(String roleName)
     *             methods for new enterprise beans.
     */
    @Override
    @Deprecated
    final public boolean isCallerInRole(java.security.Identity id) {
        throw new IllegalStateException("isCallerInRole(Identity) is deprecated, isCallerInRole(Strimg) must be used.");
    }

    /**
     * Since EJB 3.1 Singleton does NOT support EJB 2.1 client view, throw
     * IllegalStateException.
     *
     * @see com.ibm.ejs.container.SessionBeanO#getEJBLocalObject()
     */
    @Override
    final public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
        throw new IllegalStateException("getEJBLocalObject not allowed for Singleton.");
    }

    /**
     * Since EJB 3.1 Singleton does NOT support EJB 2.1 client view, throw
     * IllegalStateException.
     *
     * @see com.ibm.ejs.container.SessionBeanO#getEJBObject()
     */
    @Override
    final public EJBObject getEJBObject() throws IllegalStateException {
        throw new IllegalStateException("getEJBObject not allowed for Singleton.");
    }

    /**
     * As stated in section 3.6 of EJB 3.1 specificationSingleton does NOT
     * support EJB 2.1 client view, throw IllegalStateException.
     *
     * @see com.ibm.ejs.container.BeanO#getEJBHome()
     */
    @Override
    final public EJBHome getEJBHome() {
        throw new IllegalStateException("getEJBHome not allowed for Singleton.");
    }

    /**
     * Since EJB 3.1 Singleton does NOT support EJB 2.1 client view, throw
     * IllegalStateException.
     *
     * @see com.ibm.ejs.container.BeanO#getEJBLocalHome()
     */
    @Override
    final public EJBLocalHome getEJBLocalHome() {
        throw new IllegalStateException("getEJBLocalHome not allowed for Singleton.");
    }

    /**
     * Since EJB 3.1 Singleton does NOT support EJB 2.1 client view, there is no
     * home for it, there is no visible home for it. Thus, we will not support
     * this @deprecated method.
     *
     * @see com.ibm.ejs.container.BeanO#getEnvironment()
     */
    @Override
    final public Properties getEnvironment() {
        throw new IllegalStateException("deprecated getEnvironment not allowed for Singleton.");
    }

    /**
     * Obtain the business interface through which the current business
     * method invocation was made. <p>
     *
     * @throws IllegalStateException - Thrown if this method is called and
     *             the bean has not been invoked through a business interface.
     *
     **/
    // d571981 - added entire method.
    @Override
    final public Class<?> getInvokedBusinessInterface() throws IllegalStateException {
        if (state == METHOD_READY) {
            return super.getInvokedBusinessInterface();
        } else {
            throw new IllegalStateException("For Singleton, getInvokedBusinessInterface only allowed while in METHOD_READY state");
        }
    }

}

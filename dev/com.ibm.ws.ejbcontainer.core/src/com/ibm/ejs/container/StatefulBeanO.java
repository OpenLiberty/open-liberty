/*******************************************************************************
 * Copyright (c) 1998, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import static com.ibm.ejs.container.WrapperInterface.BUSINESS_LOCAL;
import static com.ibm.ejs.container.WrapperInterface.BUSINESS_REMOTE;
import static com.ibm.ejs.container.WrapperInterface.BUSINESS_RMI_REMOTE;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.security.Principal;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EnterpriseBean;
import javax.ejb.RemoveException;
import javax.ejb.TimerService;
import javax.xml.rpc.handler.MessageContext;

import com.ibm.ejs.container.interceptors.InterceptorMetaData;
import com.ibm.ejs.container.interceptors.InterceptorProxy;
import com.ibm.ejs.container.interceptors.InvocationContextImpl;
import com.ibm.ejs.container.passivator.StatefulPassivator;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.ejs.csi.UOWCookie;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.CacheElement;
import com.ibm.websphere.csi.ExceptionType;
import com.ibm.websphere.csi.StatefulSessionKey;
import com.ibm.websphere.csi.TransactionAttribute;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.CallbackKind;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.ejbcontainer.failover.SfFailoverClient;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.traceinfo.ejbcontainer.TEBeanLifeCycleInfo;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;

/**
 * A <code>StatefulBeanO</code> manages the lifecycle of a
 * single stateful session enterprise bean instance and provides the
 * session context implementation for the session bean. <p>
 *
 * Separate subclasses handle transaction management for container-
 * and bean-managed transactions.
 */
public abstract class StatefulBeanO extends SessionBeanO {
    private static final String CLASS_NAME = StatefulBeanO.class.getName();
    private static final TraceComponent tc = Tr.register(StatefulBeanO.class, "EJBContainer", "com.ibm.ejs.container.container"); //d118336

    /**
     * The activator cache lock table lock for {@link BeanO#ivCacheKey}.
     */
    // F61004.6
    public Object ivCacheLock;

    /**
     * The activator cache element holding this bean.
     */
    // F61004.6
    public CacheElement ivCacheElement;

    /**
     * The wrapper associated with this bean.
     */
    // F61004.6
    EJSWrapperCommon ivWrapperCommon;

    /**
     * True if this session bean is in the PreDestroy callback.
     */
    protected boolean destroying;

    /**
     * True iff this session bean instance has been removed. <p>
     */
    protected boolean removed = false;

    /**
     * True if the session bean instance has been discarded. <p>
     */
    protected boolean discarded = false;

    /**
     * True if the session bean instance is being uninstalled.
     **/
    protected boolean uninstalling = false; // d112866

    /**
     * Transaction this stateful session bean is associated with. <p>
     */
    protected ContainerTx currentTx;

    /**
     * Passivator used to swap this bean out
     */
    protected transient StatefulPassivator passivator;

    /**
     * On z/OS, <tt>true</tt> if an ENQ has been created for servant routing
     * affinity.
     */
    private boolean ivServantRoutingAffinity = false; // d646413.2

    /**
     * The TimeoutElement for this bean.
     */
    // F61004.5
    TimeoutElement ivTimeoutElement;

    /**
     * The DataXfer object to use when SFSB failover is enabled.
     */
    public transient SfFailoverClient ivSfFailoverClient; //LIDB2018-1 //d229518

    /**
     * JPA extended persistence context object.
     **/
    // d515803
    protected transient Object ivExPcContext;

    /**
     * The thread that is currently using this bean instance; or null.
     *
     * Bean is considered 'locked' when this field is set. When set, the
     * bean should only be accessed by the specified thread, unless the
     * active transaction matches the transaction the bean is currently
     * enlisted in (currentTx); in which case this field should be
     * reset to the new current thread for the transaction.
     *
     * Needed in addition to currentTx to avoid deadlocking when a
     * reentrant method call is made on the same thread.
     *
     * Should only be modified when the bucket lock is held.
     */
    // F743-22462
    private transient Thread ivActiveOnThread = null;

    /**
     * True when a thread enters the wait state for this bean instance.
     * Set back to false when a notifyAll is called.
     *
     * Should only be modified when the bucket lock is held.
     */
    // F743-22462
    private transient boolean ivThreadsWaiting = false;

    /**
     * Create new <code>StatefulBeanO</code>.
     *
     * @param c is the EJSContainer instance for this bean.
     * @param h is the home for this bean.
     */
    protected StatefulBeanO(EJSContainer c, EJSHome h) // d367572
    {
        super(c, h); // d367572

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "CTOR SFSB: " + this); // d367572.7
        }
    } // StatefulBeanO

    @Override
    protected String getStateName(int state) {
        return StateStrs[state];
    }

    /**
     * Initialize this <code>StatefulBeanO</code>, executing lifecycle
     * callback methods, etc.
     * This code was broken out of the Constructor so that this
     * BeanO instance could be placed on the ContainerTx as the
     * CallbackBeanO for the lifecycle callback methods. <p>
     */
    //d456222
    @Override
    protected void initialize(boolean reactivate) // d367572
                    throws RemoteException, InvocationTargetException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "initialize");
        }
        passivator = container.passivator;
        state = PRE_CREATE;

        // d367572.1 start
        BeanMetaData bmd = home.beanMetaData;
        ivSfFailoverClient = bmd.ivSfFailoverClient; //LIDB2018-1

        EJSDeployedSupport methodContext = null;

        // Activating or Creating a new SFSB?
        if (reactivate) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "Initialize called for a passivated SFSB: " + this); // d367572.7
            }

            // Indicate the bean is being activated, not created...
            // i.e. ejbActivate will be called, not ejbCreate.              d159152
            setState(ACTIVATING);
        } else {
            CallbackContextHelper contextHelper = null;
            try {
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Initialize called for a new SFSB: " + this); // d367572.7
                }

                // d515803
                if (bmd.ivHasCMExtendedPersistenceContext) {
                    ivExPcContext = container.getEJBRuntime().getEJBJPAContainer().onCreate(bmd.j2eeName.toString(),
                                                                                            bmd.usesBeanManagedTx,
                                                                                            bmd.ivExPcPuIds,
                                                                                            bmd.ivJpaPuIdSTUnsyncSet);
                }

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Extended PC = " + ivExPcContext);

                // Note that the local transaction surrounds injection methods and
                // PostConstruct lifecycle callbacks.
                contextHelper = new CallbackContextHelper(this); //528073, d578360
                contextHelper.begin(CallbackContextHelper.Tx.CompatLTC,
                                    CallbackContextHelper.Contexts.CallbackBean);

                // Save the BeanO being created in the method context for
                // extended-scoped persistence context management. The ExPc of the
                // bean being created may need to be inherited by any SF beans that
                // are created during injection.                             d515803
                methodContext = EJSContainer.getMethodContext();
                if (methodContext != null)
                    methodContext.ivCreateBeanO = this;

                // F743-21481
                // Moved the creation/injection of the interceptor classes after the
                // methodContext work done directly above, because the comment on it
                // implies this must be done before interceptor classes are injected.

                InterceptorMetaData imd = bmd.ivInterceptorMetaData;
                createInterceptorsAndInstance(contextHelper);

                // Now set the SessionContext and/or do the dependency injection.
                // Note that dependency injection must occur while in PRE_CREATE state.
                injectInstance(ivManagedObject, ivEjbInstance, this);

                //------------------------------------------------------------------
                // Stateful session beans need to have their id's set here so that
                // the wrapper is valid if ejbCreate() or PostConstruct calls
                // getEJBObject() or getBusinessObject().
                // Note: if activating, caller will set the correct id.      d451399
                //------------------------------------------------------------------
                setId(new BeanId(home, EJSContainer.sessionKeyFactory.create(), false));

                ivTimeoutElement = new TimeoutElement(beanId, getSessionTimeoutInMilliSeconds()); // F61004.5

                // Directly create the wrapper so we can cache bean<->wrapper.
                // Stateful beans are created with a unique primary key, so they
                // should not exist in the wrapper cache at this point.  However,
                // a call to getEJBObject, getEJBLocalObject, or getBusinessObject
                // would cause the wrapper to be added to the cache, so we do this
                // before calling the PostConstruct method.                  d729903
                ivWrapperCommon = container.wrapperManager.createWrapper(this);

                //------------------------------------------------------------------
                // Now that the SessionContext is set and/or dependencies
                // injection has occurred, change state to CREATING state to allow
                // additional methods to be called by the ejbCreate or
                // PostConstruct interceptor methods (e.g. getEJBObject).
                // This ensures CMVC defect 70091 is not reintroduced.
                //------------------------------------------------------------------
                setState(CREATING); // d367572.4

                // Determine the kind of life cycle callback to make if any. Note,
                // if CallbackKind is SessionBean, then the generated ejbdeploy wrapper
                // for a SFSB 1.x or 2.x bean will make the ejbCreate call and throw
                // the correct exception when necessary. Therefore, this code does not
                // need to handle that case.
                if (imd != null && ivCallbackKind == CallbackKind.InvocationContext) {
                    boolean globalTx = isLifecycleCallbackGlobalTx(LifecycleInterceptorWrapper.MID_POST_CONSTRUCT);
                    if (globalTx) {
                        // Complete the local transaction used for injection.
                        // Null the field so that if complete() fails it won't
                        // be called again from the finally block.
                        CallbackContextHelper injectionContextHelper = contextHelper;
                        contextHelper = null;
                        injectionContextHelper.complete(true);

                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "completed injection contexts, begin PostConstruct contexts");

                        // Begin the global transaction for the callback.
                        contextHelper = new CallbackContextHelper(this);
                        contextHelper.begin(CallbackContextHelper.Tx.Global,
                                            CallbackContextHelper.Contexts.CallbackBean);
                    }

                    // This is a 3.x SFSB that may have one or more PostConstruct interceptors
                    // methods. Invoke PostContruct interceptors if there is at least 1
                    // PostConstruct interceptor.
                    InterceptorProxy[] proxies = imd.ivPostConstructInterceptors;
                    if (proxies != null) {
                        callLifecycleInterceptors(proxies, LifecycleInterceptorWrapper.MID_POST_CONSTRUCT); // F743-1751
                    }
                }
            } // d367572.1 end
            finally // d399469
            {
                //TODO: Jim,  This code will need to change to account for
                // the possibility of a "sticky" global tran (UserTransaction)
                // that may have been started in the Callback methods when
                // the BeanO is Bean-Managed.

                // Clear the create beano if set above                          d515803
                if (methodContext != null) {
                    methodContext.ivCreateBeanO = null;
                }

                // Resume the TX context if it was suspended.
                if (contextHelper != null) // d399469
                {
                    contextHelper.complete(true);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "initialize"); // d367572.7
        }
    } // StatefulBeanO

    @Override
    protected void injectInstance(ManagedObject<?> managedObject, Object instance, InjectionTargetContext injectionContext) throws EJBException {
        // If present, setSessionContext should be called before performing injection
        if (sessionBean != null) {
            try {
                sessionBean.setSessionContext(this);
            } catch (RemoteException rex) {
                // RemoteException is only on the signature for backwards compatibility
                // with EJB 1.0; should throw an EJBException instead.
                throw ExceptionUtil.EJBException(rex);
            }
        }
        super.injectInstance(managedObject, instance, injectionContext);
    }

    /**
     * Initialize the timeout for this bean.
     *
     * @param elt the timeout element to use if the bean was passivated, or null
     *            if the bean was newly created
     */
    // F61004.5
    public void initializeTimeout(TimeoutElement elt) {
        if (elt == null) {
            elt = new TimeoutElement(beanId, getSessionTimeoutInMilliSeconds());
        }
        ivTimeoutElement = elt;
    }

    /**
     * Return true iff this <code>BeanO</code> has been removed. <p>
     */
    @Override
    public final boolean isRemoved() {
        return state == DESTROYED;
    } // isRemoved

    /**
     * Return true if this <code>BeanO</code> has been discarded. <p>
     */
    @Override
    public final boolean isDiscarded() {
        return discarded;
    }

    /**
     * Remove the cached reference to this bean from its wrapper. This prevents
     * leaking the BeanO in case user code maintains a reference to the wrapper.
     */
    // F61004.6
    private void disconnectWrapper() {
        EJSWrapperCommon wc = ivWrapperCommon;
        if (wc != null) {
            wc.ivCachedBeanO = null;
            ivWrapperCommon = null;
        }
    }

    /**
     * Destroy this <code>BeanO</code> instance. Note, the discard method
     * must be called instead of this method if bean needs to be destroyed
     * as a result of a unchecked or system exception. The discard method
     * ensures that no lifecycle callbacks will occur on the bean instance
     * if the bean is being discarded (as required by EJB spec). <p>
     *
     * This method must be called whenever this BeanO instance is no
     * longer valid. It transitions the BeanO to the DESTROYED state,
     * transitions the associated session bean (if any) to the
     * does not exist state, and releases the reference to the
     * associated session bean. <p>
     */
    @Override
    public final synchronized void destroy() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) // d144064
            Tr.entry(tc, "destroy - bean=" + this.toString()); // d161864

        if (state == DESTROYED) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "destroy");
            return;
        }

        if (ivSfFailoverClient != null) //LIDB2018-1
        {
            ivSfFailoverClient.removeEntry(beanId); //LIDB2018-1
        }

        if (isZOS) {
            removeServantRoutingAffinity(); // d646413.2
        }
        setState(DESTROYED);

        if (pmiBean != null) {
            pmiBean.beanDestroyed();
        }

        String lifeCycle = null; // d367572.4
        CallbackContextHelper contextHelper = null; // d399469, d630940
        try {
            // If the bean has already been removed, or failed to activate, then
            // there is nothing to invoke callbacks on... exit now.         d681978
            if (removed || ivEjbInstance == null) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "destroy : already removed or failed to activate");
            } else {
                if (ivCallbackKind != CallbackKind.None) {
                    BeanMetaData bmd = home.beanMetaData;
                    contextHelper = new CallbackContextHelper(this); // d399469, d630940
                    beginLifecycleCallback(LifecycleInterceptorWrapper.MID_PRE_DESTROY,
                                           contextHelper, CallbackContextHelper.Contexts.All);
                    destroying = true;

                    // d367572.1 start
                    // Invoke the PreDestroy callback if any needs to be called.
                    if (ivCallbackKind == CallbackKind.SessionBean) {
                        if (isTraceOn && // d527372
                            TEBeanLifeCycleInfo.isTraceEnabled()) // d367572.4
                        {
                            lifeCycle = "ejbRemove";
                            TEBeanLifeCycleInfo.traceEJBCallEntry(lifeCycle); // d161864
                        }
                        sessionBean.ejbRemove();
                    } else if (ivCallbackKind == CallbackKind.InvocationContext) {
                        // Invoke the PreDestroy interceptor methods.
                        InterceptorMetaData imd = home.beanMetaData.ivInterceptorMetaData;
                        InterceptorProxy[] proxies = imd.ivPreDestroyInterceptors;
                        if (proxies != null) {
                            if (isTraceOn && // d527372
                                TEBeanLifeCycleInfo.isTraceEnabled()) // d367572.4
                            {
                                lifeCycle = "PreDestroy";
                                TEBeanLifeCycleInfo.traceEJBCallEntry(lifeCycle);
                            }
                            InvocationContextImpl<?> inv = getInvocationContext();
                            inv.doLifeCycle(proxies, bmd._moduleMetaData); //d450431, F743-14982
                        }
                    } // d367572.1 end
                }

                if (pmiBean != null) {
                    pmiBean.beanRemoved();
                }
            }
        } catch (Exception ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".destroy", "176", this);

            // Just trace this event and continue so that BeanO is transitioned
            // to the DESTROYED state.  No other lifecycle callbacks on this bean
            // instance will occur once in the DESTROYED state, which is the same
            // affect as if bean was discarded as result of this exception.
            if (isTraceOn && tc.isEventEnabled()) // d144064
            {
                Tr.event(tc, "destroy caught exception:", new Object[] { this, ex }); // d367572.4 // d402681
            }
        } finally {
            if (isTraceOn && // d527372
                TEBeanLifeCycleInfo.isTraceEnabled()) {
                if (lifeCycle != null) // d367572.4
                {
                    TEBeanLifeCycleInfo.traceEJBCallExit(lifeCycle);
                }
            }

            if (contextHelper != null) {
                try {
                    contextHelper.complete(true);
                } catch (Throwable t) {
                    FFDCFilter.processException(t, CLASS_NAME + ".destroy", "585", this);

                    // Just trace this event and continue so that BeanO is transitioned
                    // to the DESTROYED state.  No other lifecycle callbacks on this bean
                    // instance will occur once in the DESTROYED state, which is the same
                    // affect as if bean was discarded as result of this exception.
                    if (isTraceOn && tc.isEventEnabled()) {
                        Tr.event(tc, "destroy caught exception: ", new Object[] { this, t });
                    }
                }
            }

            destroying = false;

            disconnectWrapper(); // F61004.6
            destroyHandleList();

            // Release any JCDI creational contexts that may exist.      F743-29174
            releaseManagedObjectContext();

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "destroy");
        }
    } // destroy

    /**
     * Destroy but do not remove this <code>BeanO</code> instance. This
     * method must be used instead of the destroy method when a uncheck
     * or system exception occurs to ensure no lifecycle callback
     * (e.g. ejbRemove or pre-destroy interceptor method) method is
     * invoked as required by the EJB spec. <p>
     *
     * This method must be called whenever this BeanO instance is no
     * longer valid. It transitions the BeanO to the DESTROYED state,
     * transitions the associated session bean (if any) to the
     * does not exist state, and releases the reference to the
     * associated session bean. <p>
     * It however does not remove the Session Bean as done by destroy
     */
    public final synchronized void destroyNotRemove() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) // d144064
            Tr.entry(tc, "destroyNotRemove : " + this);

        if (state == DESTROYED) {
            return;
        }

        // If running in an extended persistence context, the JEERuntime code
        // maintains a map of SFSBs that must be cleaned up.  This is
        // especially important if this method is called due to a timeout.
        // PM12927
        if (ivExPcContext != null) {
            container.getEJBRuntime().getEJBJPAContainer().onRemoveOrDiscard(ivExPcContext);
        }

        if (isZOS) {
            removeServantRoutingAffinity(); // d646413.2
        }
        setState(DESTROYED);

        // Release any JCDI creational contexts that may exist.     F743-29174
        releaseManagedObjectContext();

        if (pmiBean != null) {
            pmiBean.beanDestroyed();
        }

        disconnectWrapper(); // F61004.6
        destroyHandleList();

        if (isTraceOn && tc.isEntryEnabled()) // d144064
            Tr.exit(tc, "destroyNotRemove");

    } // destroyNotRemove

    /**
     * Check to see if this bean has timed out. A timed out bean which is not
     * in the METHOD_READY state should not be removed.
     */
    // F61004.5
    public final synchronized boolean isTimedOut() {
        return state == METHOD_READY && ivTimeoutElement.isTimedOut();
    }

    /**
     * Return enterprise bean associate with this <code>BeanO</code>. <p>
     *
     * Note, this method is for use solely by the deployed code during
     * creation. This method will raise an exception if this
     * <code>BeanO</code> is not in the CREATING state. <p>
     */

    @Override
    public final EnterpriseBean getEnterpriseBean() throws RemoteException {
        assertState(CREATING);
        return sessionBean;
    } // getEnterpriseBean

    /**
     * Return session timeout associate with this <code>BeanO</code>. <p>
     * in milliseconds
     */

    public final long getSessionTimeoutInMilliSeconds() //d204278
    {
        return (home.beanMetaData.sessionTimeout); // F743-6605
    }

    /**
     * Set enterprise bean associate with this <code>BeanO</code>. <p>
     *
     * @param sb the enterprise bean instance that represents this
     *            stateful session bean. Not required to implement
     *            SessionBean starting with EJB 3.0.
     */
    public void setEnterpriseBean(Object sb, ManagedObjectContext ejbContext) {
        setEnterpriseBean(sb);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setEnterpriseBean : ejbContext = " + ejbContext);

        ivEjbManagedObjectContext = ejbContext;
    }

    /**
     * This method is used by the EJB container when activating a previously
     * passivated SFSB that has interceptor instances that were also passivated.
     * The container will deserialize the interceptor instances and use this method
     * to reassociate the deserialized interceptor instances with this SessionBeanO.
     *
     * @param interceptors is the array of interceptor instances that were deserialized.
     */
    // d367572.7 - added entire method.
    public void setInterceptors(Object[] interceptors) // F87720
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.debug(tc, "setInterceptors interceptors = " + interceptors + ", for SFSB: " + this);
        }
        ivInterceptors = interceptors;
    }

    /**
     * PQ99986 - Allows access to the currentTx variable so that the
     * window of time where currentTx==null, but state is METHOD_READY can
     * be closed.
     */
    public void setCurrentTx(ContainerTx ctx) {
        currentTx = ctx;
    }

    /**
     * PQ99986 - Overrides BeanO method. This is so that no changes are needed
     * for StatefulASActivationStrategy.java, which is part of eex in 511.
     * If we didn't change this here, we'd need to change the eex release and
     * customers would have to apply both patches at once.
     */
    @Override
    public void setContainerTx(ContainerTx ctx) {
        //If we had more time, we'd look into collapsing the transaction
        //variables into the one stored in BeanO.java.

        //This may already be set, but it doesn't hurt to reset it.
        setCurrentTx(ctx);

        super.setContainerTx(ctx);
    }

    /**
     * Creates a routing affinity for this bean on the current servant. This
     * method should only be called on z/OS.
     */
    private void createServantRoutingAffinity() throws BeanNotReentrantException {
        if (!ivServantRoutingAffinity && container.ivStatefulBeanEnqDeq != null) {
            StatefulSessionKey sskey = (StatefulSessionKey) beanId.getPrimaryKey();
            byte[] pKeyBytes = sskey.getBytes();

            int retcode = container.ivStatefulBeanEnqDeq.SSBeanEnq(pKeyBytes, !home.beanMetaData.sessionActivateTran, true);
            if (retcode != 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Could not ENQ session bean with key = " + sskey);
                throw new BeanNotReentrantException("Could not ENQ session bean with key = " + sskey);
            }

            ivServantRoutingAffinity = true;
        }
    }

    /**
     * Removes the servant routing affinity for this bean. This method should
     * only be called on z/OS.
     *
     * @return <tt>false</tt> if removal fails, or <tt>true</tt> otherwise
     */
    private boolean removeServantRoutingAffinity() {
        if (ivServantRoutingAffinity) {
            StatefulSessionKey sskey = (StatefulSessionKey) beanId.getPrimaryKey();
            byte[] pKeyBytes = sskey.getBytes();

            int retcode = container.ivStatefulBeanEnqDeq.SSBeanDeq(pKeyBytes, !home.beanMetaData.sessionActivateTran, true);
            if (retcode != 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Could not DEQ session bean with key = " + sskey);
                return false;
            }

            ivServantRoutingAffinity = false;
        }

        return true;
    }

    /**
     * Complete the creation of this stateful session
     * <code>BeanO</code>. <p>
     *
     * @param supportEJBPostCreateChanges a <code>boolean</code> which is set to
     *            true if database inserts in ejbPostCreate will
     *            be supported. <p>
     *
     * @exception CreateException thrown if create-specific
     *                error occurs <p>
     * @exception RemoteException thrown if a container
     *                error occurs <p>
     */
    // d142250

    @Override
    public final void postCreate(boolean supportEJBPostCreateChanges) throws CreateException, RemoteException {
        if (isZOS) {
            createServantRoutingAffinity(); // d646413.2
        }

        setState(CREATING, METHOD_READY);
    } // postCreate

    /**
     * Activate this <code>SessionBeanO</code> and its associated
     * enterprise bean. <p>
     *
     * @param id the <code>BeanId</code> to use when activating this
     *            <code>SessionBeanO</code> <p>
     * @param tx the current <code>ContainerTx</code> when this instance is being
     *            activated.
     *
     * @exception RemoteException thrown if
     *                this <code>BeanO</code> instance cannot be activated <p>
     */
    @Override
    public final synchronized void activate(BeanId id, ContainerTx tx) // d139352-2
                    throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) // d144064
            Tr.entry(tc, "activate: " + this);

        long pmiCookie = -1;//lidb1117.4 d177661.1

        // This method is only valid for newly created beanOs, that were
        // created for the purpose of activating.                          d159152
        assertState(ACTIVATING);

        if (pmiBean != null) {
            pmiCookie = pmiBean.activationTime();//lidb1117.4
        }

        String lifeCycle = null; // d367572.7

        BeanMetaData bmd = home.beanMetaData; // d399469
        CallbackContextHelper contextHelper = null;
        try {
            // passivator needs to be called regardless of the callback kind, as
            // this is what restores the bean from the passivation file.    d427338
            passivator.activate(this, bmd); // d648122

            if (ivCallbackKind != CallbackKind.None) {
                contextHelper = new CallbackContextHelper(this); // d630940
                beginLifecycleCallback(LifecycleInterceptorWrapper.MID_POST_ACTIVATE,
                                       contextHelper, CallbackContextHelper.Contexts.CallbackBean);

                if (isTraceOn && tc.isDebugEnabled()) //d468174
                {
                    Tr.debug(tc, "extended persistence context bindID = " + ivExPcContext);
                }

                // d367572.1 start
                // Invoke the PostActivate callback if any needs to be called.
                if (ivCallbackKind == CallbackKind.SessionBean) {
                    if (isTraceOn && // d527372
                        TEBeanLifeCycleInfo.isTraceEnabled()) // d367572.7
                    {
                        lifeCycle = "ejbActivate";
                        TEBeanLifeCycleInfo.traceEJBCallEntry(lifeCycle); // d161864
                    }

                    sessionBean.ejbActivate();
                } else if (ivCallbackKind == CallbackKind.InvocationContext) {
                    // Invoke the PostActivate interceptor methods.
                    InterceptorMetaData imd = bmd.ivInterceptorMetaData;
                    InterceptorProxy[] proxies = imd.ivPostActivateInterceptors;
                    if (proxies != null) {
                        if (isTraceOn && // d527372
                            TEBeanLifeCycleInfo.isTraceEnabled()) // d367572.7
                        {
                            lifeCycle = "PostActivate";
                            TEBeanLifeCycleInfo.traceEJBCallEntry(lifeCycle);
                        }

                        InvocationContextImpl<?> inv = getInvocationContext();
                        inv.doLifeCycle(proxies, bmd._moduleMetaData); //d450431, F743-14982
                    }
                } // d367572.1 end
            }
        } finally {
            if (lifeCycle != null) // d367572.7
            {
                TEBeanLifeCycleInfo.traceEJBCallExit(lifeCycle); // d367572.7
            }

            if (contextHelper != null) {
                contextHelper.complete(true);
            }

            if (pmiBean != null) {
                pmiBean.activationTime(pmiCookie);//lidb1117.4
            }
        }

        if (isZOS) {
            createServantRoutingAffinity(); // d646413.2
        }

        setState(ACTIVATING, METHOD_READY);

        ivTimeoutElement.passivated = false; // F61004.5

        if (isTraceOn && tc.isEntryEnabled()) // d144064
            Tr.exit(tc, "activate: " + getStateName(state));
    } // activate

    /**
     * Passivate this <code>SessionBeanO</code> and its
     * associated enterprise bean. <p>
     *
     * @exception RemoteException thrown if
     *                this <code>BeanO</code> instance cannot be activated <p>
     */
    @Override
    public final synchronized void passivate() throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) // d144064
            Tr.entry(tc, "passivate: " + this);

        if (isTraceOn && tc.isDebugEnabled()) //d468174
        {
            Tr.debug(tc, "extended persistence context bindID = " + ivExPcContext);
        }

        long pmiCookie = -1; //d177661.1

        // beans can be passivated only between Xactions and not in a tx
        if (state == TX_IN_METHOD || state == TX_METHOD_READY) {
            if (isTraceOn && tc.isEventEnabled()) // d144064
                Tr.event(tc, "State: " + StateStrs[state] +
                             " Bean cannot be passivated in a transaction");
            throw new BeanOPassivationFailureException();
        }
        setState(METHOD_READY, PASSIVATING);

        String lifeCycle = null; // d367572.7
        CallbackContextHelper contextHelper = null; // d399469, d630940

        try {
            if (pmiBean != null) { //lidb1117.4
                pmiCookie = pmiBean.passivationTime();
            }

            if (ivCallbackKind != CallbackKind.None) {
                contextHelper = new CallbackContextHelper(this);
                beginLifecycleCallback(LifecycleInterceptorWrapper.MID_PRE_PASSIVATE,
                                       contextHelper, CallbackContextHelper.Contexts.All);

                // Invoke the PrePassivate callback if any needs to be called.
                if (ivCallbackKind == CallbackKind.SessionBean) {
                    if (isTraceOn && // d527372
                        TEBeanLifeCycleInfo.isTraceEnabled()) // d367572.7
                    {
                        lifeCycle = "ejbPassivate";
                        TEBeanLifeCycleInfo.traceEJBCallEntry(lifeCycle); // d161864
                    }
                    sessionBean.ejbPassivate();
                } else if (ivCallbackKind == CallbackKind.InvocationContext) {
                    // Invoke the PrePassivate interceptor methods.
                    BeanMetaData bmd = home.beanMetaData;
                    InterceptorMetaData imd = bmd.ivInterceptorMetaData; //d450431
                    InterceptorProxy[] proxies = imd.ivPrePassivateInterceptors;
                    if (proxies != null) {
                        if (isTraceOn && // d527372
                            TEBeanLifeCycleInfo.isTraceEnabled()) // d367572.7
                        {
                            lifeCycle = "PrePassivate";
                            TEBeanLifeCycleInfo.traceEJBCallEntry(lifeCycle); // d161864
                        }
                        InvocationContextImpl<?> inv = getInvocationContext();
                        inv.doLifeCycle(proxies, bmd._moduleMetaData); //d450431, F743-14982
                    }
                }
            }

            // If uninstalling this bean class, do not write this bean
            // instance out to a file, as it would just be deleted.         d112866
            if (!uninstalling)
                passivator.passivate(this, home.beanMetaData); // d648122

        } catch (RemoteException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".passivate", "425", this);
            pmiCookie = -1;//lidb1117.4
            if (isTraceOn && tc.isEventEnabled()) // d144064
                Tr.event(tc, "passivate failed! ",
                         new Object[] { this, ex });
            throw ex;
        } finally {
            if (lifeCycle != null) // d367572.7
            {
                TEBeanLifeCycleInfo.traceEJBCallExit(lifeCycle); // d367572.7
            }

            if (contextHelper != null) {
                contextHelper.complete(true);
            }

            if (pmiBean != null) {
                pmiBean.passivationTime(pmiCookie);
            }

            setState(PASSIVATING, PASSIVATED);
            ivTimeoutElement.passivated = true; // F61004.5

            // Finally, transition the beanO to the destroyed state, without
            // calling ejbRemove.                                           d730409
            destroyNotRemove();
        }

        if (isZOS && !removeServantRoutingAffinity()) // d646413.2
        {
            throw new InvalidBeanOStateException("Could not DEQ session bean with key = " + beanId.getPrimaryKey());
        }

        if (isTraceOn && tc.isEntryEnabled()) // d144064
            Tr.exit(tc, "passivate: " + getStateName(state));
    } // passivate

    /**
     * Enlist this <code>BeanO</code> instance in the given transaction. <p>
     *
     * This method is called with thread contexts established. <p>
     *
     * @param tx the <code>ContainerTx</code> this instance is being
     *            enlisted in.
     * @param txEnlist true if {@link ContainerTx#enlist} should be called.
     *
     * @return true if a reference must be taken on the BeanO, otherwise false.
     */
    // F61004.1
    public boolean enlist(ContainerTx tx, boolean txEnlist) throws RemoteException {
        return enlist(tx);
    }

    /**
     * Retrieve this <code>SessionBeanO's</code> associated
     * enterprise bean, and inform this <code>SessionBeanO</code>
     * that a method is about to be invoked on its associated enterprise
     * bean. <p>
     *
     * @param s the <code>EJSDeployedSupport</code> instance passed to
     *            both pre and postInvoke <p>
     * @param tx the <code>ContainerTx</code> for the transaction which
     *            this method is being invoked in.
     *
     * @return the Enterprise Bean instance the method will be invoke on.
     */
    // Chanced EnterpriseBean to Object. d366807.1

    @Override
    public final synchronized Object preInvoke(EJSDeployedSupport s,
                                               ContainerTx tx) // d139352-2
                    throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "preInvoke: " + this);

        switch (state) {
            case METHOD_READY:
                // For BMStatefulBeanO - pre Java EE 1.3 only
                setState(IN_METHOD);
                break;

            case TX_METHOD_READY:
                setState(TX_IN_METHOD);
                break;

            default:
                throw new InvalidBeanOStateException(StateStrs[state], "METHOD_READY | TX_METHOD_READY");
        }

        //------------------------------------------------------------
        // Set isolation level to that associated with method
        // being invoked on this instance, and save current isolation
        // level so it may be restored in postInvoke.
        //------------------------------------------------------------

        int tmp = currentIsolationLevel;
        currentIsolationLevel = s.currentIsolationLevel;
        s.currentIsolationLevel = tmp;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "preInvoke: " + getStateName(state));

        return ivEjbInstance;
    } // preInvoke

    /**
     * Inform this <code>SessionBeanO</code> that a method
     * invocation has completed on its associated enterprise bean. <p>
     *
     * @param id an <code>int</code> indicating which method is being
     *            invoked on this <code>BeanO</code> <p>
     *
     * @param s the <code>EJSDeployedSupport</code> instance passed to
     *            both pre and postInvoke <p>
     */
    @Override
    public synchronized void postInvoke(int id, EJSDeployedSupport s) //LIDB2018-1
                    throws RemoteException {
        currentIsolationLevel = s.oldIsolationLevel;

        if (removed) {
            return;
        }

        switch (state) {

            case IN_METHOD:
                setState(METHOD_READY);
                break;

            case TX_IN_METHOD:
                setState(TX_METHOD_READY);
                break;

            case DESTROYED:
                return;

            default:
                throw new InvalidBeanOStateException(StateStrs[state], "IN_METHOD | TX_IN_METHOD " +
                                                                       "| DESTROYED");
        }

        // If the current method exiting is a Remove method (@Remove) on a
        // business interface and there was no exception, or an app exception
        // and retain was not specified, then set the bean in the current tx
        // for removal after the transaction completes.                     390657
        if (s.methodInfo.ivSFSBRemove &&
            (s.ivWrapper.ivInterface == BUSINESS_LOCAL ||
             s.ivWrapper.ivInterface == BUSINESS_REMOTE ||
             s.ivWrapper.ivInterface == BUSINESS_RMI_REMOTE)
            &&
            (s.getExceptionType() == ExceptionType.NO_EXCEPTION ||
             (!s.methodInfo.ivRetainIfException &&
              s.getExceptionType() == ExceptionType.CHECKED_EXCEPTION))) {
            s.currentTx.ivRemoveBeanO = this;
        }
    } // postInvoke

    /**
     * Ask this <code>SessionBeanO</code> to write its
     * associated enterprise bean to persistent storage. <p>
     */

    @Override
    public final void store() throws RemoteException {

        // This method body intentionally left blank.

    } // store

    /**
     * Mark this <code>BeanO</code> instance as discarded. This method must
     * be used instead of the destroy method when a uncheck or system
     * exception occurs to ensure no lifecycle callback (e.g. ejbRemove
     * or pre-destroy interceptor method) method is invoked as required
     * by the EJB spec. <p>
     */
    @Override
    public final synchronized void discard() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "discard : " + this);

        if (removed || discarded)
            return;

        destroyNotRemove();

        if (pmiBean != null) {
            pmiBean.beanDiscarded(); // d647928
            pmiBean.discardCount(); // F743-27070
        }

        discarded = true;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "discard");
    }

    /**
     * Completes the removal of the bean after a Remove method has been called.
     */
    protected void completeRemoveMethod(ContainerTx tx) // d647928
                    throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "completeRemoveMethod: " + this);

        long pmiCookie = 0;
        if (pmiBean != null) {
            pmiCookie = pmiBean.initialTime(EJBPMICollaborator.REMOVE_RT);
        }

        EJBThreadData threadData = EJSContainer.getThreadData();

        // A remove method (@Remove) was called, and now that the method
        // (and transaction) have completed, remove the bean.            390657
        threadData.pushContexts(this);
        try {
            remove();
        } catch (RemoveException rex) {
            // This would be an internal EJB Container error, as RemoveException
            // should only be thrown if the bean is in a global tx, and since
            // this is afterCompletion, that cannot be true.  However, since this
            // is on the throws clause, it must be handled.... just wrap it.
            throw ExceptionUtil.EJBException("Remove Failed", rex);
        } finally {
            if (tx != null) {
                tx.ivRemoveBeanO = null;
            }

            threadData.popContexts();

            if (pmiBean != null) {
                pmiBean.finalTime(EJBPMICollaborator.REMOVE_RT, pmiCookie);
            }

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "completeRemoveMethod");
        }
    }

    /**
     * Remove this <code>SessionBeanO</code> instance. <p>
     */
    @Override
    public final synchronized void remove() throws RemoteException, RemoveException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "remove: " + this);

        //--------------------------------------------------------------------
        // It's debatable whether a session bean whose ejbRemove fails
        // should be kept around or whether the container should
        // unilaterally destroy it. For now, allow the remove to fail and
        // allow the client to "recover" from the failure, i.e. don't
        // destroy the session bean if it raises a checked exception
        // (any subclass of RemoteException).
        //--------------------------------------------------------------------

        canBeRemoved();

        setState(REMOVING); // d159152

        // d367572.1  start
        String lifeCycle = null; // d367572.4
        CallbackContextHelper contextHelper = null; // d399469
        try {
            BeanMetaData bmd = home.beanMetaData;

            if (ivExPcContext != null) // d515803
            {
                container.getEJBRuntime().getEJBJPAContainer().onRemoveOrDiscard(ivExPcContext); // d416151.3 d416151.3.5
            }

            if (ivCallbackKind != CallbackKind.None) {
                contextHelper = new CallbackContextHelper(this); // d630940
                beginLifecycleCallback(LifecycleInterceptorWrapper.MID_PRE_DESTROY,
                                       contextHelper, CallbackContextHelper.Contexts.All);

                // Invoke the PreDestroy callback if any needs to be called.
                if (ivCallbackKind == CallbackKind.SessionBean) {
                    if (isTraceOn && // d527372
                        TEBeanLifeCycleInfo.isTraceEnabled()) // d367572.4
                    {
                        lifeCycle = "ejbRemove";
                        TEBeanLifeCycleInfo.traceEJBCallEntry(lifeCycle); // d161864
                    }
                    sessionBean.ejbRemove();
                } else if (ivCallbackKind == CallbackKind.InvocationContext) {
                    // Invoke the PreDestroy interceptor methods.
                    InterceptorMetaData imd = home.beanMetaData.ivInterceptorMetaData;
                    InterceptorProxy[] proxies = imd.ivPreDestroyInterceptors;
                    if (proxies != null) {
                        if (isTraceOn && // d527372
                            TEBeanLifeCycleInfo.isTraceEnabled()) // d367572.4
                        {
                            lifeCycle = "PreDestroy";
                            TEBeanLifeCycleInfo.traceEJBCallEntry(lifeCycle);
                        }
                        InvocationContextImpl<?> inv = getInvocationContext();
                        inv.doLifeCycle(proxies, bmd._moduleMetaData); //d450431, F743-14982
                    }
                }
            }

            // d383586 - mark removed and destroy since no exception occurred.
            // If there is an exception, the bean will be removed in destroy().
            // We count beanRemoved (if any) in destroy() too.
            if (pmiBean != null) {
                pmiBean.beanRemoved();
            }
            removed = true;
            destroy();
        } catch (RemoteException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".remove", "611", this);
            if (isTraceOn && tc.isEventEnabled()) // d144064
            {
                Tr.event(tc, "remove caught exception", new Object[] { this, ex }); // d367572.4 // d402681
            }

            // d383586 - discard the instance.
            discard(); // d383586

            // It is wrong to convert RemoteException to RemoveException, but we
            // keep this for backwards compatibility. d591915
            throw new RemoveException(ex.getMessage());
        } catch (Throwable ex) // d367572.4
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".remove", "611", this);
            if (isTraceOn && tc.isEventEnabled()) // d144064
            {
                Tr.event(tc, "remove caught exception:", new Object[] { this, ex }); // d367572.4 // d402681
            }

            // d383586 - discard the instance.
            // d591915
            //
            // It looks like we are non spec-compliant here.  In all cases, we
            // bury the exception that we caught from the above try block.
            // However, the spec implies that there are many cases where we do
            // want to throw an exception here.
            //
            // In the above try block, we do a number of things, including the
            // SessionBean.ejbRemove() method on the user's underlying bean instance.
            // Presumably, this is a result of the user invoking the .ejbRemove()
            // method on the interface, and in this case it seems like we would
            // fall under the goverence of section 14.3.2 (tables 17 and 18) of the EJB 3.1 spec,
            // which indicates that in all cases where we get an exception, we
            // have to re-throw some type of exception (as opposed to what we
            // are doing here, which is to not throw any exception).
            //
            // Additionally, it appears that this .remove() method gets called into
            // when the transaction that the session bean is on is either committed
            // or rolled back, which presumably can happen as the result of user
            // invoked business method...and so the argument could be made that
            // this also falls under section 14.3.1 (tables 15 and 16) of the EJB 3.1 spec, which also
            // seems to indicate that in all cases where we get an exception, we
            // have to re-throw some type of exception.
            //
            // If we ever wanted to fix this, things are further complicated
            // because it seems like in some cases its the correct behavior to
            // not throw an exception here.  In the try block above we are also
            // executing the preDestroy interceptor methods, and so that seems to
            // fall under section 14.3.3 (table 19) of the EJB 3.1 spec, which
            // says that we should swallow the exception.
            //
            discard(); // d383586
        } finally {
            if (isTraceOn && // d527372
                TEBeanLifeCycleInfo.isTraceEnabled()) {
                if (lifeCycle != null) // d367572.4
                {
                    TEBeanLifeCycleInfo.traceEJBCallExit(lifeCycle);
                }
            }

            // Resume the TX context if it was suspended.
            if (contextHelper != null) // d399469
            {
                contextHelper.complete(true);
            }

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "remove: " + this);
        } // d367572.1  end

    } // remove

    /**
     * Transition the bean to the "does not exist" state during bean
     * class uninstall. This may occur as a result of server shutdown
     * or stop bean processing.
     *
     * <p>This method is NOT called with thread contexts established.
     **/
    // d112866
    public final synchronized void uninstall() {
        // To transition a Stateful bean to the "does not exist" state,
        // either ejbRemove or ejbPassivate must be called.
        // ejbPassivate has been chosen, as it will generally perform
        // better (it normally does less) assuming that the bean is not
        // actually passivated to a file.  Setting the "uninstalling"
        // flag to true avoids serializing to a file.
        // Calling ejbRemove is likely to cause a problem, as it is
        // typical for ejbRemove to call remove() on other beans...
        // which may already be disabled/uninstalled.
        // If the bean is not passivation capable, then just call
        // ejbRemove/PreDestroy rather than passivate.
        try {
            if (home.beanMetaData.isPassivationCapable()) {
                uninstalling = true;
                passivate();
            } else {
                completeRemoveMethod(null);
            }
        } catch (RemoteException rex) {
            FFDCFilter.processException(rex, CLASS_NAME + ".uninstall", "654", this);
            Tr.warning(tc, "IGNORING_UNEXPECTED_EXCEPTION_CNTR0033E", rex);
        }
    }

    /**
     * Determines if timer service methods are allowed based on the current state
     * of this bean instance. This includes the methods on the javax.ejb.Timer
     * and javax.ejb.TimerService interfaces. <P>
     *
     * Must be called by all Timer Service Methods to insure EJB Specification
     * compliance. <p>
     *
     * Note: This method does not apply to the EJBContext.getTimerService()
     * method, as getTimerService may be called for more bean states.
     * getTimerServcie() must provide its own checking. <p>
     *
     * @exception IllegalStateException If this instance is in a state that does
     *                not allow timer service method operations.
     **/
    // LI2281.07
    @Override
    public void checkTimerServiceAccess() throws IllegalStateException {
        // -----------------------------------------------------------------------
        // EJB Specification 2.1, 7.6.1 - Timer methods are only allowed during
        // business methods, afterBegin and beforeCompletion.
        // - Note that with Java EE 1.3 (EJB 2.0), business methods will not longer
        //   run in the IN_METHOD state, as there will be a Local Tran.
        // -----------------------------------------------------------------------
        if ((state == TX_IN_METHOD) || // business method
            (state == AFTER_BEGIN) || // afterBegin
            (state == COMMITTING_OUTSIDE_METHOD)) // beforeCompletion
        {
            // Timer Service operations are allowed.
            return;
        } else {
            IllegalStateException ise;

            ise = new IllegalStateException("StatefulBean: Timer Service methods " +
                                            "not allowed from state = " +
                                            getStateName(state));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "checkTimerServiceAccess: " + ise);

            throw ise;
        }
    }

    // --------------------------------------------------------------------------
    // EJBContext interfaces : Depending on the state this beanO is in
    // (and consequently the callback that is going to get executed), certain
    // methods in the EJBContext interface are not allowed
    // --------------------------------------------------------------------------

    /**
     * Returns true if the specific lifecycle callback should run with a global
     * transaction.
     *
     * @param methodId a method id from {@link LifecycleInterceptorWrapper}
     */
    private boolean isLifecycleCallbackGlobalTx(int methodId) {
        EJBMethodInfoImpl[] methodInfos = home.beanMetaData.lifecycleInterceptorMethodInfos;
        return methodInfos != null &&
               methodInfos[methodId].getTransactionAttribute() == TransactionAttribute.TX_REQUIRES_NEW;
    }

    /**
     * Begin contexts for a lifecycle callback.
     *
     * @param methodId a method from {@link LifecycleInterceptorWrapper}
     * @param contextHelper the context helper
     * @param pushContexts the contexts to push
     */
    protected void beginLifecycleCallback(int methodId,
                                          CallbackContextHelper contextHelper,
                                          CallbackContextHelper.Contexts pushContexts) throws CSIException {
        CallbackContextHelper.Tx beginTx = isLifecycleCallbackGlobalTx(methodId) ? CallbackContextHelper.Tx.Global : CallbackContextHelper.Tx.CompatLTC;
        contextHelper.begin(beginTx, pushContexts);
    }

    /**
     * Returns true if rollback only methods can be called in the current state.
     */
    private boolean isRollbackOnlyAllowed() {
        switch (state) {
            case PRE_CREATE:
            case AFTER_COMPLETION:
                return false;

            case CREATING:
                return isLifecycleCallbackGlobalTx(LifecycleInterceptorWrapper.MID_POST_CONSTRUCT);

            case ACTIVATING:
                return isLifecycleCallbackGlobalTx(LifecycleInterceptorWrapper.MID_PRE_PASSIVATE);

            case PASSIVATING:
                return isLifecycleCallbackGlobalTx(LifecycleInterceptorWrapper.MID_PRE_PASSIVATE);

            case REMOVING:
                return isLifecycleCallbackGlobalTx(LifecycleInterceptorWrapper.MID_PRE_DESTROY);

            case DESTROYED:
                return destroying && isLifecycleCallbackGlobalTx(LifecycleInterceptorWrapper.MID_PRE_DESTROY);
        }

        return true;
    }

    /**
     * setRollbackOnly - It is illegal to call this method during
     * ejbCreate, ejbActivate, ejbPassivate, ejbRemove and also if the
     * method being invoked has one of notsupported, supports or never
     * as its transaction attribute
     */
    @Override
    public void setRollbackOnly() {
        synchronized (this) {
            // The EJB Specification does not allow this operation from any
            // of the following states.
            if (!isRollbackOnlyAllowed()) // d159152
            {
                IllegalStateException ise;

                ise = new IllegalStateException("StatefulBean: setRollbackOnly() " +
                                                "not allowed from state = " +
                                                getStateName(state));
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "setRollbackOnly: " + ise);

                throw ise;
            }
        }

        super.setRollbackOnly();
    }

    /**
     * getRollbackOnly - It is illegal to call this method during
     * ejbCreate, ejbActivate, ejbPassivate, ejbRemove and also if the
     * method being invoked has one of notsupported, supports or never
     * as its transaction attribute
     */
    @Override
    public boolean getRollbackOnly() {
        synchronized (this) {
            // The EJB Specification does not allow this operation from any
            // of the following states.
            if (!isRollbackOnlyAllowed()) // d159152
            {
                IllegalStateException ise;

                ise = new IllegalStateException("StatefulBean: getRollbackOnly() " +
                                                "not allowed from state = " +
                                                getStateName(state));
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "getRollbackOnly: " + ise);

                throw ise;
            }
        }
        return super.getRollbackOnly();
    }

    /**
     * Obtain the business interface through which the current business
     * method invocation was made. <p>
     *
     * @throws IllegalStateException - Thrown if this method is called and
     *             the bean has not been invoked through a business interface.
     **/
    // d367572.1 added entire method.

    @Override
    public Class<?> getInvokedBusinessInterface() throws IllegalStateException {
        // Determine if in valid state for this method.
        boolean validState = false;
        int stateCopy;
        synchronized (this) {
            stateCopy = state;
            validState = (state == METHOD_READY)
                         || (state == IN_METHOD)
                         || (state == TX_METHOD_READY)
                         || (state == TX_IN_METHOD);
        }

        if (validState) {
            // Valid state, so perform function.
            return super.getInvokedBusinessInterface();
        } else {
            // Invalid state, so throw IllegalStateException.
            String stateString = getStateName(stateCopy);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Incorrect state: " + stateString);
            throw new IllegalStateException(stateString);
        }
    }

    /**
     * getCallerPrincipal is invalid if called within the setSessionContext
     * method
     */
    @Override
    public Principal getCallerPrincipal() {
        synchronized (this) {
            if (state == PRE_CREATE)
                throw new IllegalStateException();
        }

        return super.getCallerPrincipal();
    }

    @Override
    public boolean isCallerInRole(String s) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "isCallerInRole, role = " + s + ", state = " +
                         StateStrs[state]); //182011
        }

        Object bean = null; // LIDB2617.11 d366807.1
        synchronized (this) {
            if (state == PRE_CREATE)
                throw new IllegalStateException();

            if (state == IN_METHOD || state == TX_IN_METHOD) //LIDB2617.11
            {
                bean = ivEjbInstance; // LIDB2617.11 d366807.1
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "isCallerInRole");
        }

        return super.isCallerInRole(s, bean); //LIDB2617.11
    }

    /**
     * Get access to the EJB Timer Service. This function is not allowed
     * for Stateful Session Beans. <p>
     *
     * @return The EJB Timer Service.
     *
     * @exception IllegalStateException The Container throws the exception
     *                if the instance is not allowed to use this method (e.g. if the bean
     *                is a stateful session bean)
     **/
    // LI2281.07
    @Override
    public TimerService getTimerService() throws IllegalStateException {
        IllegalStateException ise;

        // Not an allowed method for Stateful beans per EJB Specification.
        ise = new IllegalStateException("StatefulBean: getTimerService not " +
                                        "allowed from Stateful Session Bean");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getTimerService: " + ise);

        throw ise;
    }

    /**
     * Obtain a reference to the JAX-RPC MessageContext. <p>
     *
     * An instance of a stateless session bean can call this method
     * from any business method invoked through its web service
     * endpoint interface. <p>
     *
     * This method is not allowed for Stateful Session Beans. <p>
     *
     * @return The MessageContext for this web service invocation.
     *
     * @exception IllegalStateException Thrown if this method is invoked
     *                while the instance is in a state that does not allow access
     *                to this method.
     **/
    // LI2281.07
    @Override
    public MessageContext getMessageContext() throws IllegalStateException {
        IllegalStateException ise;

        // Not an allowed method for Stateful beans per EJB Specification.
        ise = new IllegalStateException("StatefulBean: getMessageContext not " +
                                        "allowed from Stateful Session Bean");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getMessageContext: " + ise);

        throw ise;
    }

    // --------------------------------------------------------------------------
    //
    // Methods from EJBContextExtension interface
    //
    // --------------------------------------------------------------------------

    /**
     * Flush the persistent state of all entity EJB instances that have
     * been modified in the current transaction. <p>
     *
     * See EJBContextExtension.flushCache() for details. <p>
     *
     * Overridden to insure proper bean state. <p>
     */
    // LI3492-2
    @Override
    public void flushCache() {
        // Calling flushCache is not allowed from several states.
        if ((state == PRE_CREATE) || // prevent in setSessionContext
            (state == CREATING) ||
            (state == REMOVING) ||
            (state == ACTIVATING) ||
            (state == PASSIVATING) ||
            (state == DESTROYED) ||
            (state == AFTER_COMPLETION)) {
            IllegalStateException ise;

            ise = new IllegalStateException("StatefulBean: flushCache not " +
                                            "allowed from state = " +
                                            getStateName(state));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "flushCache: " + ise);

            throw ise;
        }

        super.flushCache();
    }

    // --------------------------------------------------------------------------
    //
    // End Interface Methods
    //
    // --------------------------------------------------------------------------

    // LIDB2775-23.4 Begins
    /**
     * Return session bean last access, this was set StatefullPassivator
     * during activate and by
     */

    public final long getLastAccessTime() {
        return ivTimeoutElement.lastAccessTime; // F61004.5
    }

    public final void setLastAccessTime(long lat) {
        ivTimeoutElement.lastAccessTime = lat; // F61004.5
    }

    // LIDB2775-23.4 Begins

    /**
     * Determine if SFSB failover is enabled for this bean.
     *
     * @return boolean true if SFSB failover is enabled.
     */
    final public boolean sfsbFailoverEnabled() //LIDB2018-1
    {
        return (ivSfFailoverClient != null);
    }

    /**
     * Create a failover entry for this SFSB and indicate the SFSB
     * status is activated.
     */
    final public void createFailoverEntry() throws RemoteException //LIDB2018-1
    {
        ivSfFailoverClient.createEntry(beanId, getSessionTimeoutInMilliSeconds());
    }

    /**
     * Update failover entry for this SFSB with the replicated data for this SFSB
     * and indicate SFSB status is passivated.
     *
     * @param beanData is the replicated data for this SFSB.
     * @param lastAccessTime is the last access time for this SFSB.
     */
    public void updateFailoverEntry(byte[] beanData, long lastAccessTime) throws RemoteException //LIDB2018
    {
        try {
            // Note, updating failover entry for a SFSB only occurs when
            // the bean is passivated.  Therefore, the updateEntry
            // method implicitly sets the passivated flag in reaper.
            ivSfFailoverClient.passivated(beanId, beanData, lastAccessTime); //d204278.2
        } catch (Exception e) {
            FFDCFilter.processException(e, CLASS_NAME + ".updateFailoverEntry", "1137", this);
            throw new RemoteException("Could not update SFSB Entry", e);
        }
    }

    /**
     * Update the failover entry to indicate the SFSB is now active.
     */
    public void updateFailoverSetActiveProp() {
        try {
            if (ivSfFailoverClient != null) {
                ivSfFailoverClient.activated(beanId, ivTimeoutElement.lastAccessTime); //d204278.2, F61004.5
            }
        } catch (Throwable e) {
            FFDCFilter.processException(e, CLASS_NAME + ".updateFailoverProp", "1158", this);
        }
    }

    /**
     * Get the JPAExPcBindingContext for this SFSB. Typically used by
     * the StatefulPassivator class when it passivates this SFSB.
     *
     * @return the JPAExPcBindingContext for this SFSB or null if a extended
     *         JPA persistence context is NOT bound this SFSB.
     */
    public Object getJPAExPcBindingContext() //d468174
    {
        return ivExPcContext;
    }

    /**
     * Set the JPAExPcBindingContext for this SFSB. Typically used by
     * the StatefulPassivator class when it activates a previously passivated SFSB.
     */
    public void setJPAExPcBindingContext(Object exPc) //d468174
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.debug(tc, "setJPAExPcBindingContext: " + exPc);

        ivExPcContext = exPc;
    }

    /**
     * Obtain the bean instance lock for the current thread and specified
     * transaction. The bean instance should not be used until the lock
     * has been obtained. <p>
     *
     * Note: This method should only be called while the bucket lock
     * for the bean is being held. <p>
     *
     * After obtaining this lock, {@link #unlock} must be called whenever
     * the bean is released from the transaction, or exits a method
     * invocation (for sticky transaction). <p>
     *
     * If the lock cannot be obtained (false returned) then the caller may
     * call the method {@link #addLockWaiter} to indicate the thread will be
     * waiting for availability and then wait on the bucket lock object.
     * When {@link #unlock} is called, all waiters on the bucket lock
     * will be notified of the bean availability. <p>
     *
     * @param methodContext EJB method context for the currently executing thread
     * @param tx the transaction in which the bean will be enlisted.
     * @return true if the bean has been successfully locked for use
     *         on the current thread, even if the bean has already
     *         been locked for the current thread; otherwise, false.
     */
    // F743-22462.CR
    public boolean lock(EJSDeployedSupport methodContext, ContainerTx tx) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "lock(" + tx + ", thread=" + Thread.currentThread().getId() +
                         ") : " + this + ", " + currentTx + ", activeOnThread=" +
                         ((ivActiveOnThread == null) ? "null" : ivActiveOnThread.getId()));

        // If the bean is eligible to be locked for use by the current thread,
        // and current transaction, then lock it now.                F743-22462.CR
        if (eligibleForLock(methodContext, tx)) // d704504
        {
            // lock the bean for use on the current thread.           F743-22462.CR
            ivActiveOnThread = Thread.currentThread();

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "lock : true");
            return true;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "lock : false");
        return false;
    }

    /**
     * Used by {@link #lock} to determine if the bean is in a state that
     * makes it eligible for use on the current thread, running with
     * the specified transaction. <p>
     *
     * @return true when the bean state makes it eligible for use on
     *         the current thread.
     */
    // F743-22462.CR
    boolean eligibleForLock(EJSDeployedSupport methodContext, ContainerTx tx) // d671368
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        // If the bean is active on a thread and the threads match, then allow
        // the bean to be locked. This is a reentrant call, and transaction
        // enlistment will fail, as required.
        if (ivActiveOnThread != null) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "eligibleForLock : ActiveOnThread : " + (ivActiveOnThread == Thread.currentThread()));
            return ivActiveOnThread == Thread.currentThread();
        }

        // If the bean is not active on a thread, then it has been unlocked
        // and is eligible for use on any thread. (Note that unlock will not
        // actually unlock if the bean is in a method and is switching between
        // local and global transactions for bean managed.) d648385 F743-22462.CR
        //
        // And, the bean is not in a transaction or the transactions match, then
        // the bean lock may be granted.
        //
        // Note that the transactions won't match if the bean is enlisted in a
        // sticky transaction, but the method on the current thread was started
        // and created a local tran prior to the sticky global being started.
        // For this scenario, don't grant the lock... this thread will wait
        // until the sticky transaction commits.
        if (currentTx == null || currentTx == tx) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "eligibleForLock : CurrentTx : true");
            return true;
        }

        // Finally, if the bean is not active on a thread, but it is enlisted in
        // a transaction that doesn't match, then it seems that it is not eligible.
        // And, if the bean's transaction was suspended on the current thread, and
        // access timeout is -1, then a deadlock will occur, which would normally
        // be an application problem. However, the spec requires that a call to
        // the 2.1 component remove result in an exception if the bean is enlisted
        // in a transaction. So check the current method context to see if the
        // bean's transaction was suspended and if so, report eligible. A reentrant
        // failure will be reported during enlistment.                     d704504
        UOWCookie uowCookie = methodContext.uowCookie;
        if (uowCookie != null) {
            if (currentTx.ivTxKey.equals(uowCookie.getSuspendedTransactionalUOW())) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "eligibleForLock : SuspendedTx : true");
                return true;
            }
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "eligibleForLock : false");
        return false;
    }

    /**
     * Notify the bean that the current thread will be waiting on the bucket
     * lock object, for notification when the bean becomes available for
     * use. When {@link #unlock} is called, all waiting threads will be notified
     * and may then attempt to obtain the lock again. <p>
     */
    public void addLockWaiter() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "addLockWaiter : " + this +
                         ", thread=" + Thread.currentThread().getId());

        ivThreadsWaiting = true;
    }

    /**
     * Make this bean instance available for use by waiting threads, if
     * eligible. For example, if the bean instance is still in the IN_METHOD
     * state when unlock is called, the bean instance may not be eligible for
     * use on other threads. Specific eligiblity will vary per StatefulBeanO
     * subclass. <p>
     *
     * All threads waiting on the bucket lock object specified will be notified
     * that the bean is available. Since multiple EJB instances may exist in
     * the same bucket, all threads waiting must be notified to insure at least
     * one waiting on this instance will acquire the lock. <p>
     *
     * Should only be called when the bucket lock for this bean instance
     * is being held. <p>
     *
     * @param lockObject the bucket lock object.
     */
    // d650932
    public final void unlock(Object lockObject) {
        // Only unlock the bean instance if it is in a state where it can be
        // made eligible for use on other threads. For example, for CM, this
        // might be when no longer in a transaction; for BM, this might be
        // when not in a method, etc.
        if (eligibleForUnlock()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "unlock(" + lockObject + ") : " + this +
                             ":waiters=" + ivThreadsWaiting +
                             ":ActiveOnThread=" + ivActiveOnThread);
            // Since the bean is eligible for use on other threads, notifying
            // all threads waiting on this bucket lock to check for bean
            // availability. All threads must be notified since there could
            // be some waiting threads that are looking for a different bean
            // in the same bucket.                                       F743-22462
            if (ivThreadsWaiting) {
                lockObject.notifyAll();
                ivThreadsWaiting = false;
            }
            ivActiveOnThread = null; // F743-22462.CR
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "unlock(" + lockObject + ") : " + this +
                             " : nothing to do for state " + StateStrs[state]);
        }
    }

    /**
     * Used by {@link #unlock} to determine if the bean is in a state that
     * makes it eligible for use on other threads. <p>
     *
     * @return true when the bean state makes it eligible for use on
     *         other threads.
     */
    // d650932
    protected abstract boolean eligibleForUnlock();

    /**
     * Legal state values.
     */
    // d367572.1 start
    // DESTROYED = 0; defined in SessionBeanO superclass
    // PRE_CREATE = 1; defined in SessionBeanO superclass
    // CREATING = 2; defined in SessionBeanO superclass
    public static final int METHOD_READY = 3;
    public static final int IN_METHOD = 4;
    public static final int TX_METHOD_READY = 5;
    public static final int TX_IN_METHOD = 6;
    public static final int COMMITTING_OUTSIDE_METHOD = 7;
    public static final int COMMITTING_IN_METHOD = 8;
    public static final int PASSIVATING = 9;
    public static final int PASSIVATED = 10;
    public static final int ACTIVATING = 11;
    public static final int REMOVING = 12; // d159152
    public static final int AFTER_BEGIN = 13; // d159152
    public static final int AFTER_COMPLETION = 14; // d159152

    /**
     * This table transalates state of bean into printable string.
     */

    protected static final String StateStrs[] = {
                                                  "DESTROYED", // 0
                                                  "PRE_CREATE", // 1  - setSessoinContext
                                                  "CREATING", // 2  - ejbCreate
                                                  "METHOD_READY", // 3
                                                  "IN_METHOD", // 4
                                                  "TX_METHOD_READY", // 5
                                                  "TX_IN_METHOD", // 6
                                                  "COMMITTING_OUTSIDE_METHOD", // 7  - beforeCompletion
                                                  "COMMITTING_IN_METHOD", // 8
                                                  "PASSIVATING", // 9  - ejbPassivate
                                                  "PASSIVATED", // 10
                                                  "ACTIVATING", // 11 - ejbActivate
                                                  "REMOVING", // 12 - ejbRemove               // d159152
                                                  "AFTER_BEGIN", // 13 - afterBegin              // d159152
                                                  "AFTER_COMPLETION", // 14 - afterCompletion         // d159152
    };
    // d367572.1 end

} // StatefulBeanO

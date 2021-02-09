/*******************************************************************************
 * Copyright (c) 1998, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.security.Principal;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EnterpriseBean;
import javax.ejb.RemoveException;
import javax.ejb.TimerService;

import com.ibm.ejs.container.interceptors.InterceptorMetaData;
import com.ibm.ejs.container.interceptors.InterceptorProxy;
import com.ibm.ejs.container.interceptors.InvocationContextImpl;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.CallbackKind;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.traceinfo.ejbcontainer.TEBeanLifeCycleInfo;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;

/**
 * A <code>StatelessBeanO</code> manages the lifecycle of a
 * single stateless session enterprise bean instance and provides the
 * session context implementation for the enterprise bean. <p>
 */

public class StatelessBeanO extends SessionBeanO {
    private static final String CLASS_NAME = StatelessBeanO.class.getName();
    private static final TraceComponent tc = Tr.register(StatelessBeanO.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * NOTE: Currently, the only stateless beanOs that can be
     * marked reentrant are the special ones associated
     * with the home beans. We know they are reentrant
     * since we wrote them.
     */
    protected boolean reentrant = false;

    /**
     * FIX ME : We have to use this additional variable to
     * determine if the get/setRollbackOnly calls are valid at this
     * time. For stateful beans we infer this from the state of the beanO
     * we should look at doing the same for stateless beans
     */
    protected boolean allowRollbackOnly = false;

    /**
     * True if the session bean instance has been discarded. <p>
     */
    protected boolean discarded = false; // PQ57408

    /**
     * Indicates whether or not the number of bean instances is limited. <p>
     *
     * This is a performance optimization to avoid accessing the similar
     * variable in BeanMetaData during critical path code.
     **/
    // PK20648
    transient protected boolean ivNumberOfBeansLimited;

    StatelessBeanO() {
        // A no-param constructor must exist for deserialization.       d724504
        super(null, null);
    }

    /**
     * Create new <code>StatelessBeanO</code>.
     * <p>
     *
     * @param c is the EJSContainer instance for this bean.
     * @param h is the home for this bean when the SLSB itself is not a home.
     *              When the SLSB is a home, then null must be passed for this parameter.
     */
    public StatelessBeanO(EJSContainer c, EJSHome h) // d367572
    {
        super(c, h); // d367572
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "CTOR SLSB: " + this);
    } // StatelessBeanO

    @Override
    protected String getStateName(int state) {
        return StateStrs[state];
    }

    /**
     * Initialize this <code>StatelessBeanO</code>, executing lifecycle
     * callback methods, etc.
     * This code was broken out of the Constructor so that this
     * BeanO instance could be placed on the ContainerTx as the
     * CallbackBeanO for the lifecycle callback methods. <p>
     */
    //456222
    @Override
    protected void initialize(boolean reactivate) throws RemoteException, InvocationTargetException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "initialize");

        //---------------------------------------------------------
        // Set state to PRE_CREATE so only methods that we permitted
        // to be called setSessionContext or dependency injection
        // (e.g. getEJBHome) are allowed and those methods that are
        // not allowed (e.g. getEJBObject) result in IllegalStateException.
        //---------------------------------------------------------
        state = PRE_CREATE;

        // d367572.1 start
        allowRollbackOnly = false;
        ivNumberOfBeansLimited = false; // PK20648

        CallbackContextHelper contextHelper = null; // d399469
        try // d399469
        {
            // Create the interceptor instances if there are interceptor
            // classes for this bean instance.
            BeanMetaData bmd = null;
            if (home == null) {
                // This SLSB must a home bean, so there is no InterceptorMetaData
                // to process.  Also, no need to ensure unspecified TX context since
                // a home is not required to be a EJB by EJB specs. Thus, we can
                // implement it however we desire to implement it.
            } else {
                // Note that the local transaction surrounds injection methods
                // and PostConstruct lifecycle callbacks.
                bmd = home.beanMetaData;
                contextHelper = new CallbackContextHelper(this); // d630940
                contextHelper.begin(CallbackContextHelper.Tx.CompatLTC,
                                    CallbackContextHelper.Contexts.CallbackBean);

                // Not a home bean, so set the BeanId for the SLSB.
                setId(home.ivStatelessId); // d140003.12

                // Cache whether or not the number of beans are limited.   PK20648
                if (home.beanMetaData.ivMaxCreation > 0) {
                    ivNumberOfBeansLimited = true;
                }

                createInterceptorsAndInstance(contextHelper);

                // Now set the SessionContext and/or do the dependency injection.
                // Note that dependency injection must occur while in PRE_CREATE state.
                injectInstance(ivManagedObject, ivEjbInstance, this);
            }

            //---------------------------------------------------------
            // Now that the SessionContext is set and/or dependencies
            // injection has occurred, change state to CREATING state to allow
            // additional methods to be called by the ejbCreate or
            // PostConstruct interceptor methods (e.g. getEJBObject).
            // This ensures CMVC defect 70091 is not reintroduced.
            //---------------------------------------------------------
            setState(CREATING);

            //-------------------------------------------------------------------
            // Call ejbCreate on PostConstruct interceptor method.
            // In EJB 3, the ejbCreate method is considered a PostContruct method
            // for Stateless session bean (which is not true for Stateful session
            // bean).  Also, for any given class, regardless of whether class
            // is the bean class or not, only 1 PostConstruct is allowed.
            //-------------------------------------------------------------------

            // Determine of life cycle callback to make if any.
            if (ivCallbackKind == CallbackKind.SessionBean) {
                // This is not a home bean since CallbackKind would be NONE for homes.
                // Therefore, the bmd variables should be set.
                Method m = bmd.ivEjbCreateMethod; //d453778
                if (m != null) //d453778
                {
                    // This is a 1.x/2.x SLSB that has a ejbCreate method in it.
                    try {
                        if (isTraceOn && // d527372
                            TEBeanLifeCycleInfo.isTraceEnabled()) {
                            TEBeanLifeCycleInfo.traceEJBCallEntry("ejbCreate"); // d161864
                        }
                        m.invoke(ivEjbInstance, new Object[] {});
                    } catch (InvocationTargetException itex) {
                        FFDCFilter.processException(itex, CLASS_NAME + ".StatelessBeanO",
                                                    "110", this);

                        // All exceptions returned through a reflect method call
                        // are wrapped... making it difficult to debug a problem
                        // since InovcationTargetException does not print the root
                        // exception. Unwrap the 'target' exception so that the
                        // client will see it as the cause of the
                        // CreateFailureException.                        d534353.1
                        Throwable targetEx = itex.getCause();
                        if (targetEx == null)
                            targetEx = itex;

                        // This kind of SLSB is allowed to throw application
                        // exceptions as well as javax.ejb.CreateException.
                        // Continue to wrap with a CreateFailureException
                        // to ensure no behavior change with prior releases.
                        throw new CreateFailureException(targetEx);
                    } catch (Throwable ex) {
                        // This kind of SLSB is allowed to throw application exceptions as well as
                        // javax.ejb.CreateException. Continue to wrap with a CreateFailureException
                        // to ensure no behavior change with prior releases.
                        FFDCFilter.processException(ex, CLASS_NAME + ".StatelessBeanO", "401", this);
                        throw new CreateFailureException(ex);
                    } finally {
                        if (isTraceOn && // d527372
                            TEBeanLifeCycleInfo.isTraceEnabled()) {
                            TEBeanLifeCycleInfo.traceEJBCallExit("ejbCreate"); // d161864
                        }
                    }
                }
            } else if (ivCallbackKind == CallbackKind.InvocationContext) {
                // This is a 3.x SLSB that may have one or more PostConstruct interceptors
                // methods. Invoke PostContruct interceptors if there is atleast 1
                // PostConstruct interceptor.

                InterceptorMetaData imd = bmd.ivInterceptorMetaData;

                if (imd != null) // d402681
                {
                    InterceptorProxy[] proxies = imd.ivPostConstructInterceptors;
                    if (proxies != null) {
                        callLifecycleInterceptors(proxies, LifecycleInterceptorWrapper.MID_POST_CONSTRUCT); // F743-1751
                    }
                }
            }

            //---------------------------------------------------------
            // Now that create has completed, allow setRollbackOnly to
            // be called on the SessionContext and change state to the
            // POOLED state to indicate the SLSB is in the method-ready
            // pool state and business methods are now allowed to be
            // invoked on this session bean.
            //---------------------------------------------------------
            allowRollbackOnly = true;
            setState(POOLED);
            // d367572.1 end of change.
        } finally // d399469
        {
            // Resume the TX context if it was suspended.
            if (contextHelper != null) // d399469
            {
                contextHelper.complete(true);
            }
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "initialize : " + this);
        }
    } // StatelessBeanO

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
        super.injectInstance(managedObject, instance, this);
    }

    /**
     * Return true iff this <code>BeanO</code> has been removed. <p>
     */
    @Override
    public boolean isRemoved() {

        //---------------------------------------------------
        // Stateless session beans are never marked removed.
        //---------------------------------------------------

        return false;

    } // isRemoved

    /**
     * Is this bean discarded? <code>BeanO</code> instance. <p>
     *
     * This method is called to determine if BeanO instance is no
     * longer valid, the discard state results in beanO set to
     * state set to DESTROYED.
     */

    @Override
    public boolean isDiscarded() {

        return discarded; // PQ57408
    } // isDiscarded

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
        if (isTraceOn && tc.isEntryEnabled()) // d367572.4
        {
            Tr.entry(tc, "destroy");
        }

        if (state == DESTROYED) {
            return;
        }

        // For Stateless, 'destroy' is where the bean is removed and destroyed.
        // Remove time should include calling any lifecycle callbacks.   d626533.1
        long removeStartTime = -1;
        if (pmiBean != null) {
            removeStartTime = pmiBean.initialTime(EJBPMICollaborator.REMOVE_RT);
        }

        allowRollbackOnly = false;

        if (ivCallbackKind != CallbackKind.None) {
            // d367572.4 start of change.
            String lifeCycle = null;
            CallbackContextHelper contextHelper = new CallbackContextHelper(this); // d399469, d630940
            BeanMetaData bmd = home.beanMetaData;

            try {
                // Suspend UOW to ensure everything in this method runs
                // in a unspecified TX context if this bean is in a EJB 3.0
                // module.  We are not doing for older modules to ensure we do
                // break existing working applications.
                //
                // Pre-Destroy is allowed to access java:comp/env, so all contexts
                // must be established around the Pre-Destroy calls. This must be done
                // for Stateless beans since they are often destroyed outside the scope
                // of a method invocation.                                      d546031
                contextHelper.begin(CallbackContextHelper.Tx.CompatLTC,
                                    CallbackContextHelper.Contexts.All);

                // d367572.1 start
                // Invoke either ejbRemove or PreDestroy lifecycle callback if necessary.
                if (ivCallbackKind == CallbackKind.SessionBean) {
                    if (isTraceOn && // d527372
                        TEBeanLifeCycleInfo.isTraceEnabled()) {
                        lifeCycle = "ejbRemove";
                        TEBeanLifeCycleInfo.traceEJBCallEntry(lifeCycle); // d161864
                    }

                    // pre-EJB3 SLSB, so invoke ejbRemove.
                    sessionBean.ejbRemove();
                } else if (ivCallbackKind == CallbackKind.InvocationContext) {
                    // Invoke the PreDestroy interceptor methods.
                    if (isTraceOn && // d527372
                        TEBeanLifeCycleInfo.isTraceEnabled()) {
                        lifeCycle = "preDestroy";
                        TEBeanLifeCycleInfo.traceEJBCallEntry(lifeCycle); // d161864
                    }
                    InterceptorMetaData imd = home.beanMetaData.ivInterceptorMetaData;
                    InterceptorProxy[] proxies = imd.ivPreDestroyInterceptors;
                    if (proxies != null) {
                        if (isTraceOn && tc.isDebugEnabled()) {
                            Tr.debug(tc, "invoking PreDestroy interceptors");
                        }
                        InvocationContextImpl<?> inv = getInvocationContext();
                        inv.doLifeCycle(proxies, bmd._moduleMetaData); //d450431, F743-14982
                    }
                } // d367572.1 end
            } catch (Exception ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".destroy", "164", this);

                // Just trace this event and continue so that BeanO is transitioned
                // to the DESTROYED state.  No other lifecycle callbacks on this bean
                // instance will occur once in the DESTROYED state, which is the same
                // affect as if bean was discarded as result of this exception.
                if (isTraceOn && tc.isEventEnabled()) {
                    Tr.event(tc, "destroy caught exception: ", new Object[] { this, ex }); // d402681
                }
            } finally {
                if (isTraceOn && // d527372
                    TEBeanLifeCycleInfo.isTraceEnabled()) {
                    if (lifeCycle != null) {
                        TEBeanLifeCycleInfo.traceEJBCallExit(lifeCycle); // d161864
                    }
                }

                try {
                    contextHelper.complete(true);
                } catch (Throwable t) {
                    FFDCFilter.processException(t, CLASS_NAME + ".destroy", "505", this);

                    // Just trace this event and continue so that BeanO is transitioned
                    // to the DESTROYED state.  No other lifecycle callbacks on this bean
                    // instance will occur once in the DESTROYED state, which is the same
                    // affect as if bean was discarded as result of this exception.
                    if (isTraceOn && tc.isEventEnabled()) {
                        Tr.event(tc, "destroy caught exception: ", new Object[] { this, t });
                    }
                }
            } // d367572.4 end of change.
        }

        setState(DESTROYED);

        destroyHandleList();

        // Release any JCDI creational contexts that may exist.         F743-29174
        releaseManagedObjectContext();

        // For Stateless, 'destroy' is where the bean is removed and destroyed.
        // Update both counters and end remove time.                     d626533.1
        if (pmiBean != null) {
            pmiBean.beanRemoved(); // d647928.4
            pmiBean.beanDestroyed();
            pmiBean.finalTime(EJBPMICollaborator.REMOVE_RT, removeStartTime);
        }

        // If the number of allowed bean instances is limited, then the number
        // of created instances needs to be decremented when an instance is
        // destroyed, and the next thread that may be waiting for an instance
        // must be notified.                                               PK20648
        if (ivNumberOfBeansLimited) {
            synchronized (beanPool) {
                --home.ivNumberBeansCreated;
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "destroy: BeanPool(" + home.ivNumberBeansCreated +
                                 "/" + home.beanMetaData.ivMaxCreation + ")");
                beanPool.notify();
            }
        }

        allowRollbackOnly = true;

        if (isTraceOn && tc.isEntryEnabled()) // d367572.4
        {
            Tr.exit(tc, "destroy");
        }
    } // destroy

    /**
     * Return enterprise bean associate with this <code>BeanO</code>. <p>
     *
     * Stateless session beans do not implement this method since they
     * are created on demand by the container, not by the client. <p>
     */

    @Override
    public final EnterpriseBean getEnterpriseBean() throws RemoteException {
        throw new UnsupportedOperationException(); // OK

    } // getEnterpriseBean

    /**
     * Obtain the business interface through which the current business
     * method invocation was made. <p>
     *
     * @throws IllegalStateException - Thrown if this method is called and
     *                                   the bean has not been invoked through a business interface.
     **/
    // d367572.1 added entire method.
    @Override
    public Class<?> getInvokedBusinessInterface() throws IllegalStateException {
        // Determine if in valid state for this method.
        boolean validState = false;
        int stateCopy;
        synchronized (this) {
            stateCopy = state;
            validState = (state == POOLED) || (state == IN_METHOD);
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
     * Complete the creation of this stateless session
     * <code>BeanO</code>. <p>
     *
     * @param supportEJBPostCreateChanges a <code>boolean</code> which is set to
     *                                        true if database inserts in ejbPostCreate will
     *                                        be supported. <p>
     *
     *                                        Stateless session beanos do not implement this method because they
     *                                        are created on-demand by the container. <p>
     */
    // d142250
    @Override
    public final void postCreate(boolean supportEJBPostCreateChanges) throws CreateException, RemoteException {
        throw new UnsupportedOperationException(); // OK

    } // postCreate

    /**
     * Activate this <code>SessionBeanO</code> and its associated
     * enterprise bean. <p>
     *
     * @param id the <code>BeanId</code> to use when activating this
     *               <code>SessionBeanO</code>.
     * @param tx the current <code>ContainerTx</code> when this instance is being
     *               activated.
     *
     * @exception BeanOActivationFailureException thrown if
     *                                                this <code>BeanO</code> instance cannot be activated <p>
     */
    @Override
    public final void activate(BeanId id, ContainerTx tx) // d114677 d139352-2
                    throws RemoteException {
        //---------------------------------------------------
        // Activating a stateless session bean is a no-op
        //---------------------------------------------------
    } // activate

    /**
     * Enlist this <code>SessionBeanO</code> instance in the
     * given transaction. <p>
     *
     * @param tx the <code>ContainerTx</code> this instance is being
     *               enlisted in <p>
     */

    @Override
    public final boolean enlist(ContainerTx tx) // d14677
                    throws RemoteException {
        if (!reentrant) {//this is not a home bean //d170394
            ivContainerTx = tx;
        }
        //-----------------------------------------------
        // Enlisting a stateless session bean is a no-op
        //-----------------------------------------------

        return false;

    } // enlist

    /**
     * Retrieve this <code>SessionBeanO's</code> associated
     * enterprise bean, and inform this <code>SessionBeanO</code>
     * that a method is about to be invoked on its associated enterprise
     * bean. <p>
     *
     * @param s  the <code>EJSDeployedSupport</code> instance passed to
     *               both pre and postInvoke <p>
     * @param tx the <code>ContainerTx</code> for the transaction which
     *               this method is being invoked in.
     *
     * @return the Enterprise Bean instance the method will be invoke on.
     */
    // Chanced EnterpriseBean to Object. d366807.1
    @Override
    public final Object preInvoke(EJSDeployedSupport s,
                                  ContainerTx tx) // d139352-2
                    throws RemoteException {
        //---------------------------------------------------------
        // If this bean is reentrant then its state is meaningless
        // since methods will be entering and exiting in no strict
        // order so don't bother tracking its state.
        //---------------------------------------------------------

        if (!reentrant) {
            setState(POOLED, IN_METHOD);
        }

        //------------------------------------------------------------
        // Set isolation level to that associated with method
        // being invoked on this instance, and save current isolation
        // level so it may be restored in postInvoke.
        //------------------------------------------------------------

        int tmp = currentIsolationLevel;
        currentIsolationLevel = s.currentIsolationLevel;
        s.currentIsolationLevel = tmp;

        return ivEjbInstance;
    } // preInvoke

    /**
     * Inform this <code>SessionBeanO</code> that a method
     * invocation has completed on its associated enterprise bean. <p>
     *
     * @param s the <code>EJSDeployedSupport</code> instance passed to
     *              both pre and postInvoke <p>
     */
    @Override
    public void postInvoke(int id, EJSDeployedSupport s) // d170394
                    throws RemoteException {
        ivContainerTx = null;//d170394
    }

    @Override
    public void returnToPool() // RTC107108
                    throws RemoteException {
        if (isDestroyed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "returnToPool: skipped: " + this);
        } else {
            setState(IN_METHOD, POOLED);

            // If the number of allowed bean instances is limited, then the
            // next thread waiting for a bean instance needs to be notified
            // when one is returned to the pool.                            PK20648
            if (ivNumberOfBeansLimited) {
                synchronized (beanPool) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "returnToPool: " + this + ": " +
                                     home.ivNumberBeansCreated + "/" +
                                     home.beanMetaData.ivMaxCreation);

                    beanPool.put(this);
                    beanPool.notify();
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "returnToPool: " + this);

                beanPool.put(this);
            }
        }
    }

    /**
     * Inform this <code>SessionBeanO</code> that the transaction
     * it was enlisted with has committed. <p>
     */

    @Override
    public final void commit(ContainerTx tx) throws RemoteException {
        //---------------------------------------------------
        // A stateless session bean must never be committed.
        //---------------------------------------------------

        throw new InvalidBeanOStateException(StateStrs[state], "NONE" +
                                                               ": Stateless commit not allowed");

    } // commit

    /**
     * Inform this <code>SessionBeanO</code> that the transaction
     * it was enlisted with has rolled back. <p>
     */

    @Override
    public final void rollback(ContainerTx tx) throws RemoteException {
        //-----------------------------------------------------
        // A stateless session bean must never be rolled back.
        //-----------------------------------------------------

        throw new InvalidBeanOStateException(StateStrs[state], "NONE" +
                                                               ": Stateless rollback not allowed");

    } // rollback

    /**
     * Ask this <code>SessionBeanO</code> to write its
     * associated enterprise bean to persistent storage. <p>
     */

    @Override
    public final void store() throws RemoteException {
        //------------------------------------------------
        // A stateless session bean must never be stored.
        //------------------------------------------------

        throw new InvalidBeanOStateException(StateStrs[state], "NONE" +
                                                               ": Stateless store not allowed");

    } // store

    /**
     * Ask this <code>SessionBeanO</code> to passivate its
     * associated enterprise bean. <p>
     */

    @Override
    public final void passivate() throws RemoteException {
        //----------------------------------------------------
        // A stateless session bean must never be passivated.
        //----------------------------------------------------

        throw new InvalidBeanOStateException(StateStrs[state], "NONE" +
                                                               ": Stateless passivate not allowed");

    } // passivate

    /**
     * Remove this <code>SessionBeanO</code> instance. <p>
     */

    @Override
    public final void remove() throws RemoteException, RemoveException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "remove");

        //canBeRemoved();//94781

        // For stateless beans, it is not upto the client to remove objects
        // so just return at this point. The container can decide to remove
        // objects at some point (the pool manager drives this decision)
        // at which point we will call ejbRemove on the bean

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "remove");
        return;
    } // remove

    /**
     * Discard this <code>SessionBeanO</code> instance. This method must
     * be used instead of the destroy method when a uncheck or system
     * exception occurs to ensure no lifecycle callback (e.g. ejbRemove
     * or pre-destroy interceptor method) method is invoked as required
     * by the EJB spec. <p>
     */
    @Override
    public void discard() // PQ57408 Implemented this function
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "discard : " + this);

        // If this is a BeanO for a home (i.e. home field will be null) then
        // the discard should be ignored. Homes work much like singletons in
        // that a system exception from a home method does not cause the
        // home to be discarded. And, since the BeanO is not transitioned
        // to the 'destroyed' state, normal postInvoke will run.           d661866
        if (home == null) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "discard : Home beans are never discarded");
            return;
        }

        discarded = true;

        if (state == DESTROYED) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "discard : Bean already destroyed");
            return;
        }

        setState(DESTROYED);

        destroyHandleList();

        // Release any JCDI creational contexts that may exist.         F743-29174
        this.releaseManagedObjectContext();

        if (pmiBean != null) {
            pmiBean.discardCount(); // F743-27070
            pmiBean.beanDestroyed();
        }

        // If the number of allowed bean instances is limited, then the number
        // of created instances needs to be decremented when an instance is
        // discarded, and the next thread that may be waiting for an instance
        // must be notified.                                               PK20648
        if (ivNumberOfBeansLimited) {
            synchronized (beanPool) {
                --home.ivNumberBeansCreated;
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "discard: BeanPool(" + home.ivNumberBeansCreated +
                                 "/" + home.beanMetaData.ivMaxCreation + ")");
                beanPool.notify();
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "discard");
        }
    } // discard

    /**
     * Ensure that this <code>StatelessBeanO</code> is prepared
     * for transaction completion. <p>
     *
     */

    @Override
    public final void beforeCompletion() throws RemoteException {
        //--------------------------------------------------------------
        // A stateless session bean is never enlisted in a transaction,
        // so this method must never be called.
        //--------------------------------------------------------------

        throw new InvalidBeanOStateException(StateStrs[state], "NONE" +
                                                               ": Stateless beforeCompletion not allowed");

    } // beforeCompletion

    /**
     * Determines if timer service methods are allowed based on the current state
     * of this bean instance. This includes the methods on the javax.ejb.Timer
     * and javax.ejb.TimerService interfaces. <P>
     *
     * Must be called by all Timer Servcie Methods to insure EJB Specification
     * compliance. <p>
     *
     * Note: This method does not apply to the EJBContext.getTimerService()
     * method, as getTimerService may be called for more bean states.
     * getTimerServcie() must provide its own checking. <p>
     *
     * @exception IllegalStateException If this instance is in a state that does
     *                                      not allow timer service method operations.
     **/
    // LI2281.07
    @Override
    public void checkTimerServiceAccess() throws IllegalStateException {
        // -----------------------------------------------------------------------
        // EJB Specification 2.1, 7.8.2 - Timer service methods are only
        // allowed during business methods and ejbTimeout.
        // -----------------------------------------------------------------------
        if ((state != IN_METHOD)) // business method
        {
            IllegalStateException ise;

            ise = new IllegalStateException("StatelessBean: Timer Service " +
                                            "methods not allowed from state = " +
                                            getStateName(state));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "checkTimerServiceAccess: " + ise);

            throw ise;
        }
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
            if (!allowRollbackOnly) {
                throw new IllegalStateException();
            }
        }

        super.setRollbackOnly();
    }

    @Override
    public boolean getRollbackOnly() {

        synchronized (this) {
            if (!allowRollbackOnly) {
                throw new IllegalStateException();
            }
        }
        return super.getRollbackOnly();
    }

    /**
     * getCallerPrincipal is invalid if called within the setSessionContext
     * method
     */
    @Override
    public Principal getCallerPrincipal() {
        synchronized (this) {
            if ((state == PRE_CREATE) || (state == CREATING) || (!allowRollbackOnly))
                throw new IllegalStateException();
        }

        return super.getCallerPrincipal();

    }

    @Override
    public boolean isCallerInRole(String roleName) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "isCallerInRole, role = " + roleName + ", state = " + StateStrs[state]); //182011
        }

        Object bean = null; // LIDB2617.11 d366807.1
        synchronized (this) {
            if ((state == PRE_CREATE || state == CREATING) || (!allowRollbackOnly)) // d367572.1
                throw new IllegalStateException();

            if (state == IN_METHOD) //LIDB2617.11
            {
                bean = ivEjbInstance; // LIDB2617.11 d366807.1
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "isCallerInRole");
        }

        return super.isCallerInRole(roleName, bean); //LIDB2617.11
    }

    /**
     * Get access to the EJB Timer Service. <p>
     *
     * @return The EJB Timer Service.
     *
     * @exception IllegalStateException The Container throws the exception
     *                                      if the instance is not allowed to use this method (e.g. if the bean
     *                                      is a stateful session bean)
     **/
    // LI2281.07
    @Override
    public TimerService getTimerService() throws IllegalStateException {
        // Calling getTimerService is not allowed from setSessionContext.
        if ((state == PRE_CREATE)) // prevent in setSessionContext
        {
            IllegalStateException ise;

            ise = new IllegalStateException("StatelessBean: getTimerService not " +
                                            "allowed from state = " +
                                            getStateName(state));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getTimerService: " + ise);

            throw ise;
        }

        return super.getTimerService();
    }

    // --------------------------------------------------------------------------
    //
    // End Interface Methods
    //
    // --------------------------------------------------------------------------

    /**
     * Legal state values.
     */

    // d367572.1 start
    // DESTROYED = 0; defined in SessionBeanO superclass
    // PRE_CREATE = 1; defined in SessionBeanO superclass
    // CREATING = 2; defined in SessionBeanO superclass
    public static final int POOLED = 3;
    public static final int IN_METHOD = 4;

    /**
     * This table translates state of bean into printable string.
     */

    protected static final String StateStrs[] = {
                                                  "DESTROYED", // 0
                                                  "PRE_CREATE", // 1
                                                  "CREATING", // 2
                                                  "POOLED", // 3
                                                  "IN_METHOD" // 4
    };
    // d367572.1 end

} // StatelessBeanO

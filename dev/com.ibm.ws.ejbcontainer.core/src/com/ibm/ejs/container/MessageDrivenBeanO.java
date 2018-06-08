/*******************************************************************************
 * Copyright (c) 2001, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EnterpriseBean;
import javax.ejb.MessageDrivenBean;
import javax.ejb.RemoveException;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;

import com.ibm.ejs.container.interceptors.InterceptorMetaData;
import com.ibm.ejs.container.interceptors.InterceptorProxy;
import com.ibm.ejs.container.interceptors.InvocationContextImpl;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.websphere.ejbcontainer.MessageDrivenContextExtension;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.CallbackKind;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.traceinfo.ejbcontainer.TEBeanLifeCycleInfo;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;

/**
 * MessageDrivenBeanO manages the lifecycle of a
 * single MessageDrivenBean instance and provides the
 * MessageDrivenConext implementation for the enterprise bean. <p>
 **/
public class MessageDrivenBeanO extends ManagedBeanOBase implements MessageDrivenContextExtension, // LI3492-2
                UserTransactionEnabledContext, Serializable {
    private static final long serialVersionUID = -7199444167428525287L;
    private static final TraceComponent tc = Tr.register(MessageDrivenBeanO.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    private static final String CLASS_NAME = "com.ibm.ejs.container.MessageDrivenBeanO";

    public MessageDrivenBean messageDrivenBean;

    protected boolean reentrant = false;

    protected boolean discarded = false; //167937

    protected int currentIsolationLevel = -1;

    /**
     * FIX ME : We have to use this additional variable to
     * determine if the get/setRollbackOnly calls are valid at this
     * time. For stateful beans we infer this from the state of the beanO
     * we should look at doing the same for stateless beans
     */
    protected boolean allowRollbackOnly = false;

    /*** new for EJB3 *********************************************************/

    /**
     * CallbackKind used to indicate whether or not to call MessageDrivenBean callback
     * methods, call interceptor methods, or do nothing (a bean is no longer
     * required to implement MessageDrivenBean and it is not required to have any
     * interceptor methods.
     */
    CallbackKind ivCallbackKind; // d367572

    // d367572 change CTOR signature for EJB3 changes.
    // d399469.2 moved code to initialize method.
    public MessageDrivenBeanO(EJSContainer c, EJSHome h) {
        super(c, h);
    }

    @Override
    public void setEnterpriseBean(Object bean) {
        super.setEnterpriseBean(bean);
        if (bean instanceof MessageDrivenBean) // d367572
        {
            messageDrivenBean = (MessageDrivenBean) bean;
        }
    }

    @Override
    protected String getStateName(int state) {
        return StateStrs[state];
    }

    /**
     * Initialize this <code>MessageDrivenBeanO</code>, executing lifecycle
     * callback methods, etc.
     * This code was broken out of the Constructor so that this
     * BeanO instance could be placed on the ContainerTx as the
     * CallbackBeanO for the lifecycle callback methods.
     */
    // d399469.2 - added entire method.
    @Override
    protected void initialize(boolean reactivate) throws RemoteException, InvocationTargetException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "initialize");
        }

        BeanMetaData bmd = home.beanMetaData;
        ivCallbackKind = bmd.ivCallbackKind;

        state = PRE_CREATE; // d159152

        CallbackContextHelper contextHelper = new CallbackContextHelper(this); // d630940
        try {
            // Disallow setRollbackOnly until create is done.
            allowRollbackOnly = false;

            // Set the BeanId for this MDB. Just like we do for SLSB,
            // we use the BeanId for the home bean so that the same ID
            // is used for all MDB instances. Note the ivStatelessId is
            // for the home bean, which is implemented as a SLSB.
            if (home != null) {
                setId(home.ivStatelessId); // d140003.12
            }

            // Note that the local transaction surrounds injection methods and
            // PostConstruct lifecycle callbacks.
            contextHelper.begin(CallbackContextHelper.Tx.CompatLTC,
                                CallbackContextHelper.Contexts.CallbackBean); // d630940

            InterceptorMetaData imd = bmd.ivInterceptorMetaData;
            createInterceptorsAndInstance(contextHelper);

            // Now set the MessageDrivenContext and/or do the dependency injection.
            // Note that dependency injection must occur while in PRE_CREATE state.
            injectInstance(ivManagedObject, ivEjbInstance, this);

            //---------------------------------------------------------
            // Now that the MessageDrivenContext is set and/or dependencies
            // injection has occured, change state to CREATING state to allow
            // additional methods to be called by the ejbCreate or
            // PostConstruct interceptor methods (e.g. getEJBObject).
            //---------------------------------------------------------
            setState(CREATING); //d399469.2

            // Determine of life cycle callback to make if any.
            if (ivCallbackKind == CallbackKind.MessageDrivenBean) {
                Method m = bmd.ivEjbCreateMethod; //d453778
                if (m != null) //d453778
                {
                    // This is a 2.x MDB that has a ejbCreate method in it.
                    try {
                        if (isTraceOn && // d527372
                            TEBeanLifeCycleInfo.isTraceEnabled()) {
                            TEBeanLifeCycleInfo.traceEJBCallEntry("ejbCreate");
                        }
                        m.invoke(ivEjbInstance, new Object[] {});
                    } catch (InvocationTargetException itex) {
                        //FFDCFilter.processException(itex, CLASS_NAME + ".MessageDrivenBeanO", "96", this);

                        // All exceptions returned through a reflect method call
                        // are wrapped... making it difficult to debug a problem
                        // since InovcationTargetException does not print the root
                        // exception. Unwrap the 'target' exception so that the
                        // client will see it as the cause of the
                        // CreateFailureException.                        d534353.1
                        Throwable targetEx = itex.getCause();
                        if (targetEx == null)
                            targetEx = itex;

                        // MDB 2.x is allowed to throw application exceptions as well as
                        // javax.ejb.CreateException. Continue to wrap with a CreateFailureException
                        // to ensure no behavior change with prior releases.
                        if (isTraceOn && tc.isDebugEnabled()) {
                            Tr.debug(tc, "MDB ejbCreate failure", targetEx);
                        }
                        throw new CreateFailureException(targetEx);
                    } catch (Throwable ex) {
                        //FFDCFilter.processException(ex, CLASS_NAME + ".MessageDrivenBeanO", "96", this);

                        // MDB 2.x is allowed to throw application exceptions as well as
                        // javax.ejb.CreateException. Continue to wrap with a CreateFailureException
                        // to ensure no behavior change with prior releases.
                        if (isTraceOn && tc.isDebugEnabled()) {
                            Tr.debug(tc, "MDB ejbCreate failure", ex);
                        }
                        throw new CreateFailureException(ex);
                    } finally {
                        if (isTraceOn && // d527372
                            TEBeanLifeCycleInfo.isTraceEnabled()) {
                            TEBeanLifeCycleInfo.traceEJBCallExit("ejbCreate");
                        }
                    }
                }
            } else if (ivCallbackKind == CallbackKind.InvocationContext) {
                // This is a MDB 3 that may have one or more PostConstruct interceptors
                // methods. Invoke PostContruct interceptors if there is atleast 1
                // PostConstruct interceptor.
                try {
                    if (imd != null) // d402681
                    {
                        InterceptorProxy[] proxies = imd.ivPostConstructInterceptors;
                        if (proxies != null) {
                            if (isTraceOn && // d527372
                                TEBeanLifeCycleInfo.isTraceEnabled()) {
                                TEBeanLifeCycleInfo.traceEJBCallEntry("PostConstruct");
                            }
                            InvocationContextImpl<?> inv = getInvocationContext();
                            inv.doLifeCycle(proxies, bmd._moduleMetaData); //d450431, F743-14982
                        }
                    }
                } catch (Throwable t) {
                    //FFDCFilter.processException(t, CLASS_NAME + ".MessageDrivenBeanO", "281", this);

                    // PostConstruct interceptors are allowed to throw system runtime exceptions,
                    // but NOT application exceptions. Therefore, wrap the caught Throwable
                    // in a javax.ejb.EJBException so that it gets handled as an unchecked
                    // exception.
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "MDB PostConstruct failure", t);
                    }
                    throw ExceptionUtil.EJBException("MDB PostConstruct failure", t);
                } finally {
                    if (isTraceOn && // d527372
                        TEBeanLifeCycleInfo.isTraceEnabled() &&
                        imd != null &&
                        imd.ivPostConstructInterceptors != null) {
                        TEBeanLifeCycleInfo.traceEJBCallExit("PostConstruct");
                    }
                }
            } // d367572.1 end

            //---------------------------------------------------------
            // Now that create has completed, allow setRollbackOnly to
            // be called on the SessionContext and change state to the
            // POOLED state to indicate the MDB is in the method-ready
            // pool state and message listener methods are now allowed to be
            // invoked on this MDB.
            //---------------------------------------------------------
            allowRollbackOnly = true;
            setState(POOLED);
        } finally // d399469
        {
            contextHelper.complete(true);
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "initialize");
        }
    }

    @Override
    protected void injectInstance(ManagedObject<?> managedObject, Object instance, InjectionTargetContext injectionContext) {
        if (messageDrivenBean != null) {
            messageDrivenBean.setMessageDrivenContext(this);
        }
        super.injectInstance(managedObject, instance, injectionContext);
    }

    /**
     * Complete the creation of this MessageDrivenBean
     * <code>BeanO</code>. <p>
     *
     * @param supportEJBPostCreateChanges a <code>boolean</code> which is set to
     *            true if database inserts in ejbPostCreate will
     *            be supported. <p>
     *
     *            MessageDrivenBean beanos do not implement this method because they
     *            are created on-demand by the container. <p>
     */
    // d142250
    @Override
    public final void postCreate(boolean supportEJBPostCreateChanges) throws CreateException, RemoteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "postCreate : NotImplementedException");
        throw new UnsupportedOperationException();
    }

    /**
     * Return true iff this <code>BeanO</code> has been removed. <p>
     */
    @Override
    public boolean isRemoved() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isRemoved : false");
        return false;
    }

    //167937 - rewrote entire method.
    @Override
    public void discard() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "discard");
        }

        discarded = true;

        if (state == DESTROYED) {
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

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "discard");
        }
    }

    @Override
    public boolean isDiscarded() {
        return discarded;
    }

    /**
     * Nothing to do in MessageDrivenBeans for invalidate.
     */
    @Override
    public final void invalidate() {
        //-------------------------------------------
        // This method body intentionally left blank
        //-------------------------------------------
    }

    /**
     * Activate this <code>BeanO</code> instance and its associated
     * enterprise bean. <p>
     *
     * @param id the <code>BeanId</code> to use when activating this
     *            <code>BeanO</code>.
     * @param tx the current <code>ContainerTx</code> when this instance is being
     *            activated.
     */
    @Override
    public final void activate(BeanId id, ContainerTx tx) // d114677 d139352-2
                    throws RemoteException {
        //-----------------------------------------------
        // activate on a message driven bean is a no-op
        //-----------------------------------------------
    }

    @Override
    public final boolean enlist(ContainerTx tx) // d114677
                    throws RemoteException {
        ivContainerTx = tx; //167937
        //---------------------------------------------------
        // Enlisting message driven beans should be a no-op
        //---------------------------------------------------
        return false;
    }

    /**
     * Retrieve this <code>BeanO's</code> associated enterprise bean, and
     * inform this <code>BeanO</code> that a method is about to be
     * invoked on its associated enterprise bean. <p>
     *
     * @param s the <code>EJSDeployedSupport</code> instance associated
     *            with the pre/postInvoke, which contains an indication of
     *            which method is being invoked on this <code>BeanO</code>.
     * @param tx the <code>ContainerTx</code> for the transaction which
     *            this method is being invoked in.
     *
     * @return the Enterprise Bean instance the method will be invoke on.
     */
    // Chanced EnterpriseBean to Object. d366807.1
    @Override
    public final Object preInvoke(EJSDeployedSupport s,
                                  ContainerTx tx) // d139352-2
                    throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "preInvoke");

        setState(POOLED, IN_METHOD);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "preInvoke");
        return ivEjbInstance;
    }

    @Override
    public final void postInvoke(int id, EJSDeployedSupport s) throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "postInvoke: " + this);

        if (state == DESTROYED) {
            return;
        }

        //167937 - discard bean if BMT was started and is still active.
        if (ivContainerTx != null && ivContainerTx.isBmtActive(s.methodInfo)) {
            // BMT is still active.  Discard bean and let the BeanManaged.postInvoke
            // do the rollback and throwing of the exception.
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "MDB method is not allowed to leave a BMT active.  Discarding bean.");
            discard();
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "postInvoke");
    }

    @Override
    public void returnToPool() // RTC107108
                    throws RemoteException {
        if (isDestroyed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "returnToPool: skipped: " + this);
        } else {
            setState(IN_METHOD, POOLED);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "returnToPool: " + this);

            beanPool.put(this);
        }
    }

    @Override
    public final void commit(ContainerTx tx) throws RemoteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "commit");
        throw new InvalidBeanOStateException(StateStrs[state], "NONE" +
                                                               ": Msg Bean commit not allowed");
    }

    @Override
    public final void rollback(ContainerTx tx) throws RemoteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "rollback");
        throw new InvalidBeanOStateException(StateStrs[state], "NONE" +
                                                               ": Msg Bean rollback not allowed");
    }

    @Override
    public final void store() throws RemoteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "store");
        throw new InvalidBeanOStateException(StateStrs[state], "NONE" +
                                                               ": Msg Bean store not allowed");
    }

    @Override
    public final void passivate() throws RemoteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "passivate");
        throw new InvalidBeanOStateException(StateStrs[state], "NONE" +
                                                               ": Msg Bean passivate not allowed");
    }

    /**
     * This method should never really get invoked for message driven
     * beans, there is no home interface on which to invoke it.
     */
    @Override
    public final void remove() throws RemoteException, RemoveException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "remove");
        throw new InvalidBeanOStateException(StateStrs[state], "NONE" +
                                                               ": Msg Bean remove not allowed");
    }

    @Override
    public final void beforeCompletion() throws RemoteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "beforeCompletion");
        //------------------------------------------------------
        // message driven beans do not enlist themselves in
        // transactions, consequently no transaction callbacks
        // should be fired on them
        //------------------------------------------------------
        throw new InvalidBeanOStateException(StateStrs[state], "NONE" +
                                                               ": Msg Bean beforeCompletion not allowed");
    }

    /**
     * Get isolation level currently associated with this session
     * bean instance. This is used for determining the isolation level
     * to use in a bean managed transaction.
     *
     * The current isolation level is maintained in a stack with the
     * assistance of the EJSDeployedSupport instance passed to
     * preInvoke/postInvoke.
     */
    @Override
    public int getIsolationLevel() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getIsolationLevel : " + currentIsolationLevel);
        return currentIsolationLevel;
    } // getIsolationLevel

    /**
     * Method to allow container to condition removal of EJB from the EJB
     * cache on whether the BeanO is in destroyed state (EJB's are not in
     * the cache if there state is destroyed).
     **/
    @Override
    public boolean isDestroyed() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "isDestroyed");
        if (state == DESTROYED) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "isDestroyed");
            return true;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "isDestroyed");
        return false;
    }

    /**
     * Destroy this <code>BeanO</code> instance. <p>
     *
     * This method must be called whenever this BeanO instance is no
     * longer valid. It transitions the BeanO to the DESTROYED state,
     * transitions the associated session bean (if any) to the
     * does not exist state, and releases the reference to the
     * associated MessageDrivenBean bean. <p>
     */
    @Override
    public final synchronized void destroy() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "destroy");
        if (state == DESTROYED) {
            return;
        }

        // For MessageDriven, 'destroy' is where the bean is removed and destroyed.
        // Remove time should include calling any lifecycle callbacks.   d626533.1
        long removeStartTime = -1;
        if (pmiBean != null) {
            removeStartTime = pmiBean.initialTime(EJBPMICollaborator.REMOVE_RT);
        }

        allowRollbackOnly = false;

        if (ivCallbackKind != CallbackKind.None) {
            // d367572.1 start
            String traceString = null;
            CallbackContextHelper contextHelper = new CallbackContextHelper(this); // d399469.2, d630940
            BeanMetaData bmd = home.beanMetaData;

            try {
                // Pre-Destroy is allowed to access java:comp/env, so all contexts
                // must be established around the Pre-Destroy calls. This must be done
                // for MessageDriven beans since they are often destroyed outside the
                // scope of a method invocation.                               d546031
                contextHelper.begin(CallbackContextHelper.Tx.CompatLTC,
                                    CallbackContextHelper.Contexts.All); // d630940

                // Determine kind of life cycle callback to make, if any.
                if (ivCallbackKind == CallbackKind.MessageDrivenBean) {
                    // MDB 2.x has a ejbRemove method in it, so it needs to be called.
                    if (isTraceOn && // d527372
                        TEBeanLifeCycleInfo.isTraceEnabled()) {
                        traceString = "ejbRemove";
                        TEBeanLifeCycleInfo.traceEJBCallEntry(traceString);
                    }
                    messageDrivenBean.ejbRemove();
                } else if (ivCallbackKind == CallbackKind.InvocationContext) {
                    // MDB 3 may have one or more PreDestroy interceptor methods.
                    // Invoke PreDestroy if at least 1 PreDestroy interceptor exists.
                    if (isTraceOn && // d527372
                        TEBeanLifeCycleInfo.isTraceEnabled()) {
                        traceString = "PreDestroy";
                        TEBeanLifeCycleInfo.traceEJBCallEntry(traceString);
                    }
                    InterceptorMetaData imd = bmd.ivInterceptorMetaData; //d450431
                    InterceptorProxy[] proxies = imd.ivPreDestroyInterceptors;
                    if (proxies != null) {
                        InvocationContextImpl<?> inv = getInvocationContext();
                        inv.doLifeCycle(proxies, bmd._moduleMetaData); //d450431, F743-14982
                    }
                }
            } catch (Throwable ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".destroy", "376", this);
                if (isTraceOn && tc.isEventEnabled()) // d144064
                {
                    Tr.event(tc, traceString + " threw and exception:", new Object[] { this, ex });
                }
            } finally {
                try {
                    contextHelper.complete(true);
                } catch (Throwable t) {
                    FFDCFilter.processException(t, CLASS_NAME + ".destroy", "848", this);

                    // Just trace this event and continue so that BeanO is transitioned
                    // to the DESTROYED state.  No other lifecycle callbacks on this bean
                    // instance will occur once in the DESTROYED state, which is the same
                    // affect as if bean was discarded as result of this exception.
                    if (isTraceOn && tc.isEventEnabled()) {
                        Tr.event(tc, "destroy caught exception: ", new Object[] { this, t });
                    }
                }

                if (isTraceOn && // d527372
                    TEBeanLifeCycleInfo.isTraceEnabled()) {
                    if (traceString != null) {
                        TEBeanLifeCycleInfo.traceEJBCallExit(traceString);
                    }
                }
            }
            // d367572.1 end
        }

        setState(DESTROYED);

        destroyHandleList();

        // Release any JCDI creational contexts that may exist.         F743-29174
        this.releaseManagedObjectContext();

        // For MessageDriven, 'destroy' is where the bean is removed and destroyed.
        // Update both counters and end remove time.                     d626533.1
        if (pmiBean != null) {
            pmiBean.beanRemoved();
            pmiBean.beanDestroyed();
            pmiBean.finalTime(EJBPMICollaborator.REMOVE_RT, removeStartTime);
        }

        allowRollbackOnly = true;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "destroy");
    } // destroy

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
        // EJB Specification 2.1, 15.5.1 - Timer service methods are only
        // allowed during business methods and ejbTimeout.
        // -----------------------------------------------------------------------
        if ((state != IN_METHOD)) // business method
        {
            IllegalStateException ise;

            ise = new IllegalStateException("MessageDrivenBean: Timer Service  " +
                                            "methods not allowed from state = " +
                                            getStateName(state));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "checkTimerServiceAccess: " + ise);

            throw ise;
        }
    }

    // --------------------------------------------------------------------------
    // MessageDrivenContext interface
    // --------------------------------------------------------------------------

    /**
     * Get user transaction object bean can use to demarcate transactions
     */
    @Override
    public synchronized UserTransaction getUserTransaction() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getUserTransaction");

        // Calling getUserTransaction is not allowed from setMessageDrivenContext
        // per the EJB Specification.                                      d159152
        if ((state == PRE_CREATE)) {
            IllegalStateException ise;

            ise = new IllegalStateException("MessageDrivenBean: getUserTransaction " +
                                            "not allowed from state = " +
                                            getStateName(state));
            if (isTraceOn && tc.isDebugEnabled())
                Tr.exit(tc, "getUserTransaction", ise);

            throw ise;
        }

        UserTransaction userTransactionWrapper = UserTransactionWrapper.INSTANCE; // d631349

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getUserTransaction", userTransactionWrapper);
        return userTransactionWrapper;
    }

    /**
     * setRollbackOnly - It is illegal to call this method during
     * ejbCreate, ejbActivate, ejbPassivate, ejbRemove and also if the
     * method being invoked has one of notsupported, supports or never
     * as its transaction attribute
     */
    @Override
    public void setRollbackOnly() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "setRollbackOnly");
        synchronized (this) {
            if (!allowRollbackOnly) {
                throw new IllegalStateException();
            }
        }

        super.setRollbackOnly();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "setRollbackOnly");
    }

    @Override
    public boolean getRollbackOnly() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getRollbackOnly");
        synchronized (this) {
            if (!allowRollbackOnly) {
                throw new IllegalStateException();
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getRollbackOnly");
        return super.getRollbackOnly();
    }

    /**
     * Return the bean instance associated with this BeanO
     */
    // Chanced EnterpriseBean to Object. d366807.1
    @Override
    public Object getBeanInstance() {
        return ivEjbInstance;
    } // getBeanInstance()

    /**
     * Return enterprise bean associate with this <code>BeanO</code>. <p>
     *
     * MessageDrivenBeans do not implement this method since they
     * are created on demand by the container, not by the client. <p>
     */
    @Override
    public final EnterpriseBean getEnterpriseBean() throws RemoteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getEnterpriseBean");

        throw new UnsupportedOperationException();
    }

    /**
     * Returns an array of Interceptor instances when ivCallbackKind is set to
     * CallbackKind.InvocationContext. For all other CallbackKind values,
     * null is returned.
     *
     * This includes around invoke, around timeout, and non-EnterpriseBean
     * lifecycle callback interceptors.
     **/
    // d630824
    @Override
    public Object[] getInterceptors() {
        return ivInterceptors;
    }

    /**
     * getCallerPrincipal - allowed for MessageDriven Beans, except from
     * setMessageDrivenContext (beginning with the EJB 2.1 Specification).
     */
    // d116376 d178396
    @Override
    public Principal getCallerPrincipal() {
        // Calling getCallerPrincipal is not allowed from setMessageDrivenContext.
        if ((state == PRE_CREATE)) // prevent in setMessageDrivenContext
        {
            IllegalStateException ise;

            ise = new IllegalStateException("MessageDrivenBean: getCallerPrincipal " +
                                            "not allowed from state = " +
                                            getStateName(state));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getCallerPrincipal: " + ise);

            throw ise;
        }

        return super.getCallerPrincipal();
    }

    @Override
    public boolean isCallerInRole(String roleName) // F743-6142
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "isCallerInRole, role = " + roleName + ", state = " + StateStrs[state]);
        }

        Object bean = null;
        synchronized (this) {
            if ((state == PRE_CREATE || state == CREATING) || (!allowRollbackOnly))
                throw new IllegalStateException();

            if (state == IN_METHOD) {
                bean = ivEjbInstance;
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "isCallerInRole");
        }

        return super.isCallerInRole(roleName, bean);

    }

    /**
     * getEJBHome - It is illegal to call this method
     * message-driven bean methods because there is no EJBHome object
     * for message-driven beans.
     */
    @Override
    public EJBHome getEJBHome() //d116376
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getEJBHome");
        Tr.error(tc, "METHOD_NOT_ALLOWED_CNTR0047E", "MessageDrivenBeanO.getEJBHome()");
        throw new IllegalStateException("Method Not Allowed Exception: See Message-drive Bean Component Contract section of the applicable EJB Specification.");
    } // getEJBHome

    /**
     * getEJBLocalHome - It is illegal to call this method
     * message-driven bean methods because there is no EJBLocalHome object
     * for message-driven beans.
     */
    @Override
    public EJBLocalHome getEJBLocalHome() //d116376
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getEJBLocalHome");
        Tr.error(tc, "METHOD_NOT_ALLOWED_CNTR0047E",
                 "MessageDrivenBeanO.getEJBLocalHome()");
        throw new IllegalStateException("Method Not Allowed Exception: See Message-drive Bean Component Contract section of the applicable EJB Specification.");
    } // getEJBLocalHome

    /**
     * Get access to the EJB Timer Service. <p>
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
        // Calling getTimerService is not allowed from setMessageDrivenContext.
        if ((state == PRE_CREATE)) // prevent in setMessageDrivenContext
        {
            IllegalStateException ise;

            ise = new IllegalStateException("MessageDrivenBean: getTimerService " +
                                            "not allowed from state = " +
                                            getStateName(state));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getTimerService: " + ise);

            throw ise;
        }

        return super.getTimerService();
    }

    /**
     * Returns the context data associated with this invocation or lifecycle
     * callback. If there is no context data, an empty Map object will be
     * returned.
     **/
    // F743-21028
    @Override
    public Map<String, Object> getContextData() {
        // Calling getContextData is not allowed from setMessageDrivenContext.
        if (state == PRE_CREATE || state == DESTROYED) {
            IllegalStateException ise;

            ise = new IllegalStateException("MessageDrivenBean: getContextData " +
                                            "not allowed from state = " +
                                            getStateName(state));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getContextData: " + ise);

            throw ise;
        }

        return super.getContextData();
    }

    // --------------------------------------------------------------------------
    // End MessageDrivenContext interface
    // --------------------------------------------------------------------------

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
        // Calling flushCache is not allowed from setMessageDrivenContext.
        if ((state == PRE_CREATE)) // prevent in setMessageDrivenContext
        {
            IllegalStateException ise;

            ise = new IllegalStateException("MessageDrivenBean: flushCache not " +
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

    /**
     * This method is called by the optc and optb activators to make sure that
     * load is called before a store for optimistic concurrency cases.
     *
     * @param tx ContainerTx for the current transaction.
     */
    // d132828
    @Override
    public void ensurePersistentState(ContainerTx tx) // d139352-2
                    throws RemoteException {
        // method intentionally left blank
        // provides implementation for those not implementing (non CMP 1.1)
    }

    //d140003.20
    // we need the module to tell whether to set isolation level for user transactions
    @Override
    public int getModuleVersion() {
        return home.beanMetaData.ivModuleVersion;
    }

    /**
     * Legal state values.
     */
    public static final int DESTROYED = 0; // d138680
    public static final int POOLED = 1;
    public static final int IN_METHOD = 2;
    public static final int PRE_CREATE = 3; // d159152
    public static final int CREATING = 4; //d399469.2

    /**
     * Translation of state into string values
     */
    protected static final String StateStrs[] = {
                                                  "DESTROYED", // 0
                                                  "POOLED", // 1
                                                  "IN_METHOD", // 2
                                                  "PRE_CREATE", // 3  - setMessageDrivenContext         // d159152
                                                  "CREATING" // 4                                    // d399469.2
    };
}

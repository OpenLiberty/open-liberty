/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.Map;
import java.util.Properties;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EnterpriseBean;
import javax.ejb.RemoveException;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;

import com.ibm.ejs.container.interceptors.InterceptorMetaData;
import com.ibm.ejs.container.interceptors.InterceptorProxy;
import com.ibm.ejs.container.interceptors.InvocationContextImpl;
import com.ibm.ejs.container.interceptors.ManagedBeanInvocationContext;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.CallbackKind;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.traceinfo.ejbcontainer.TEBeanLifeCycleInfo;

/**
 * A <code>ManagedBeanO</code> manages the lifecycle of a
 * single managed bean instance. A managed bean has no context,
 * so all context methods are overridden and will fail. <p>
 *
 * Managed beans could probably get buy without a BeanO, since
 * most of the methods are not implemented, however, this BeanO
 * implementation has been used for consistency with the other
 * bean types in regards to injection and lifecycle callbacks. <p>
 */
public final class ManagedBeanO extends ManagedBeanOBase {
    private static final String CLASS_NAME = ManagedBeanO.class.getName();
    private static final TraceComponent tc = Tr.register(ManagedBeanO.class, "EJBContainer", "com.ibm.ejs.container.container");

    public static final int DESTROYED = 0;
    public static final int PRE_CREATE = 1; // prior to PostConstruct
    public static final int CREATING = 2; // during PostConstruct
    public static final int ACTIVE = 3; // Injection & PostConstruct complete
    private static final String[] StateStrs = {
                                                "DESTROYED",
                                                "PRE_CREATE",
                                                "CREATING",
                                                "ACTIVE",
    };

    /**
     * CallbackKind used to indicate whether or not to call interceptor methods,
     * or do nothing.
     */
    public CallbackKind ivCallbackKind;

    /**
     * Create new <code>ManagedBeanO</code> instance. <p>
     *
     * @param c is the EJSContainer instance for this bean.
     * @param h is the home for this bean.
     */
    public ManagedBeanO(EJSContainer c, EJSHome h) {
        super(c, h);

        BeanMetaData bmd = home.beanMetaData;
        ivCallbackKind = bmd.ivCallbackKind;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "ManagedBeanO.<init> : " + bmd.j2eeName);
    }

    @Override
    public String toString() {
        // Managed bean don't have beanId, so include the J2EEName instead.
        return "ManagedBeanO(" + home.getJ2EEName() + ", " + getStateName(state) + ')';
    }

    @Override
    protected String getStateName(int state) {
        return StateStrs[state];
    }

    @Override
    protected InvocationContextImpl createInvocationContext() {
        return new ManagedBeanInvocationContext();
    }

    @Override
    void initialize(boolean reactivate) throws RemoteException, InvocationTargetException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "initialize");

        // -----------------------------------------------------------------------
        // Set state to PRE_CREATE for consistency with other bean types
        // including consistent trace output.
        // -----------------------------------------------------------------------
        state = PRE_CREATE;

        try {
            BeanMetaData bmd = home.beanMetaData;

            // Note that the UnspecifiedContextHelper is NOT used here, since
            // this activity should occur in the context of the caller and
            // the CallbackBeanO should NOT be updated.

            InterceptorMetaData imd = bmd.ivInterceptorMetaData;
            createInterceptorsAndInstance(null);

            // Note that dependency injection must occur while in PRE_CREATE state.
            // We need to pass the context so that CDI can inject properly,
            // getInjectionTargetContextData is overridden to only return the managed object context data
            if (!bmd.managedObjectManagesInjectionAndInterceptors) {
                injectInstance(ivManagedObject, ivEjbInstance, this);
            }

            // --------------------------------------------------------------------
            // Now that dependency injection has occurred, change state to CREATING
            // state to allow additional methods to be called by the PostConstruct
            // interceptor methods.
            // --------------------------------------------------------------------
            setState(CREATING);

            // --------------------------------------------------------------------
            // Call PostConstruct interceptor method.
            //-------------------------------------------------------------------
            if (ivCallbackKind == CallbackKind.InvocationContext) {
                // Invoke PostContruct interceptors if there is at least 1
                // PostConstruct interceptor.
                if (imd != null && !bmd.managedObjectManagesInjectionAndInterceptors) {
                    InterceptorProxy[] proxies = imd.ivPostConstructInterceptors;
                    if (proxies != null) {
                        callLifecycleInterceptors(proxies, LifecycleInterceptorWrapper.MID_POST_CONSTRUCT);
                    }
                }
            }

            // --------------------------------------------------------------------
            // Now that create has completed, change state to the ACTIVE state to
            // indicate the managed bean has completed creation and may now be
            // actively used by the application.
            //---------------------------------------------------------
            setState(ACTIVE);
        } finally {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "initialize : " + this);
        }
    }

    /**
     * Overridden to support PreDestroy callback. The PreDestroy callback
     * will run in an unspecified transaction context. <p>
     *
     * It is expected that this method will only be called one time and only
     * if there is a PreDestroy callback, however it will support extraneous
     * extra calls. <p>
     */
    @Override
    public void destroy() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "destroy : " + this);

        if (state == DESTROYED) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "destroy : already destroyed");
            return;
        }

        setState(DESTROYED);

        InterceptorMetaData imd = home.beanMetaData.ivInterceptorMetaData;
        InterceptorProxy[] proxies = (imd == null) ? null : imd.ivPreDestroyInterceptors;

        if (proxies == null) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "destroy : no PreDestroy");
            return;
        }

        BeanMetaData bmd = home.beanMetaData;
        CallbackContextHelper contextHelper = new CallbackContextHelper(this);
        try {
            contextHelper.begin(CallbackContextHelper.Tx.LTC,
                                CallbackContextHelper.Contexts.CallbackBean);

            // Invoke the PreDestroy interceptor methods.
            if (bmd.managedObjectManagesInjectionAndInterceptors) {

                ivManagedObject.release();
            } else {
                InvocationContextImpl<?> inv = getInvocationContext();
                inv.doLifeCycle(proxies, bmd._moduleMetaData);
            }
        } catch (Exception ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".destroy", "262", this);

            // Just trace this event and continue so that BeanO is transitioned
            // to the DESTROYED state.  No other lifecycle callbacks on this bean
            // instance will occur once in the DESTROYED state, which is the same
            // affect as if bean was discarded as result of this exception.
            if (isTraceOn && tc.isEventEnabled()) // d144064
                Tr.event(tc, "destroy caught exception:", new Object[] { this, ex });
        } finally {
            try {
                contextHelper.complete(true);
            } catch (Throwable t) {
                FFDCFilter.processException(t, CLASS_NAME + ".destroy", "279", this);

                // Just trace this event and continue so that BeanO is transitioned
                // to the DESTROYED state.  No other lifecycle callbacks on this bean
                // instance will occur once in the DESTROYED state, which is the same
                // affect as if bean was discarded as result of this exception.
                if (isTraceOn && tc.isEventEnabled())
                    Tr.event(tc, "destroy caught exception: ", new Object[] { this, t });
            }

            releaseManagedObjectContext();

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "destroy : completed PreDestroy");
        }
    }

    /**
     * Invoke any lifecycle interceptors associated with this bean instance.
     * <dl>
     * <dt>pre-condition
     * <li>
     * Called when creating a new managed bean instance or destroying an
     * existing managed bean instance. This implies that this.state must
     * be set to either PRE_CREATE, CREATING, or PRE_DESTROY.
     * <li>
     * this.ivInterceptors != null, which implies the interceptors instances
     * are already created and exist for this instance.
     * </dl>
     *
     * @param proxies is the non-null reference to InterceptorProxy array that
     *            contains the lifecycle interceptor methods to invoke.
     *
     * @throws javax.ejb.EJBException if any Throwable is thrown by a
     *             post construct interceptor method. Use the getCause to get
     *             the original Throwable that had occurred.
     */
    protected void callLifecycleInterceptors(InterceptorProxy[] proxies, int methodId) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2
        if (TraceComponent.isAnyTracingEnabled()) // d527372
        {
            if (isTraceOn)
                TEBeanLifeCycleInfo.traceEJBCallEntry(LifecycleInterceptorWrapper.TRACE_NAMES[methodId]);

            if (tc.isDebugEnabled())
                Tr.debug(tc, "callLifecycleInterceptors");
        }

        try {
            InvocationContextImpl<?> inv = getInvocationContext();
            BeanMetaData bmd = home.beanMetaData;
            inv.doLifeCycle(proxies, bmd._moduleMetaData);
        } catch (Throwable t) {
            // FFDCFilter.processException( t, CLASS_NAME + ".ManagedBeanO", "149", this );

            // Lifecycle interceptors are allowed to throw system runtime exceptions,
            // but NOT application exceptions. Therefore, wrap the caught Throwable
            // in a javax.ejb.EJBException and throw it so that it gets handled as
            // an unchecked exception.
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "ManagedBean PostConstruct failure", t);

            throw ExceptionUtil.EJBException("managed bean lifecycle interceptor failure", t);
        } finally {
            if (isTraceOn &&
                TEBeanLifeCycleInfo.isTraceEnabled()) {
                TEBeanLifeCycleInfo.traceEJBCallExit(LifecycleInterceptorWrapper.TRACE_NAMES[methodId]);
            }
        }
    }

    /**
     * Return the bean instance associated with this BeanO
     */
    @Override
    public Object getBeanInstance() {
        return ivEjbInstance;
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
     * Method to allow container to condition removal of EJB from the EJB
     * cache on whether the BeanO is in destroyed state (EJB's are not in
     * the cache if there state is destroyed).
     **/
    @Override
    public boolean isDestroyed() {
        if (state == DESTROYED) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ejs.container.ManagedBeanOBase#getInjectionTargetContextData(java.lang.Class)
     */
    @Override
    public <T> T getInjectionTargetContextData(Class<T> type) {
        // If we have a managed object, then see if the context data type is
        // available from its state.
        if (ivEjbManagedObjectContext != null) {
            T data = ivEjbManagedObjectContext.getContextData(type);
            if (data != null) {
                return data;
            }
        }

        // For managed beans, don't return any other EJB context data
        return null;
    }

    // --------------------------------------------------------------------------
    //
    // EJBContext interface - not supported for managed beans
    //
    // --------------------------------------------------------------------------

    @Override
    public synchronized UserTransaction getUserTransaction() {
        throw new IllegalStateException("Method not supported for ManagedBean.");
    }

    @Override
    public Map<String, Object> getContextData() {
        throw new IllegalStateException("Method not supported for ManagedBean.");
    }

    @Override
    @SuppressWarnings("deprecation")
    public java.security.Identity getCallerIdentity() {
        throw new IllegalStateException("Method not supported for ManagedBean.");
    }

    @Override
    public Principal getCallerPrincipal() {
        throw new IllegalStateException("Method not supported for ManagedBean.");
    }

    @Override
    public EJBHome getEJBHome() {
        throw new IllegalStateException("Method not supported for ManagedBean.");
    }

    @Override
    public EJBLocalHome getEJBLocalHome() {
        throw new IllegalStateException("Method not supported for ManagedBean.");
    }

    @Override
    public Properties getEnvironment() {
        throw new IllegalStateException("Method not supported for ManagedBean.");
    }

    @Override
    public boolean getRollbackOnly() {
        throw new IllegalStateException("Method not supported for ManagedBean.");
    }

    @Override
    public TimerService getTimerService() throws IllegalStateException {
        throw new IllegalStateException("Method not supported for ManagedBean.");
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isCallerInRole(java.security.Identity id) {
        throw new IllegalStateException("Method not supported for ManagedBean.");
    }

    @Override
    public boolean isCallerInRole(String roleName) {
        throw new IllegalStateException("Method not supported for ManagedBean.");
    }

    @Override
    public void setRollbackOnly() {
        throw new IllegalStateException("Method not supported for ManagedBean.");
    }

    @Override
    public Object lookup(String name) {
        throw new IllegalStateException("Method not supported for ManagedBean.");
    }

    // --------------------------------------------------------------------------
    //
    // BeanO abstract methods - not supported for managed beans
    //
    // --------------------------------------------------------------------------

    @Override
    public void activate(BeanId id, ContainerTx tx) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void beforeCompletion() throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkTimerServiceAccess() throws IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void commit(ContainerTx tx) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void discard() {
        // d705480 - Managed beans are never discarded.
    }

    @Override
    public boolean enlist(ContainerTx tx) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void ensurePersistentState(ContainerTx tx) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public EnterpriseBean getEnterpriseBean() throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invalidate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDiscarded() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRemoved() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void passivate() throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void postCreate(boolean supportEjbPostCreateChanges) throws CreateException, RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void postInvoke(int id, EJSDeployedSupport s) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object preInvoke(EJSDeployedSupport s, ContainerTx tx) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove() throws RemoteException, RemoveException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rollback(ContainerTx tx) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void store() throws RemoteException {
        throw new UnsupportedOperationException();
    }

}

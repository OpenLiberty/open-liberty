/*******************************************************************************
 * Copyright (c) 1998, 2015 IBM Corporation and others.
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
import java.util.Map;

import javax.ejb.EJBException;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.rmi.PortableRemoteObject;
import javax.transaction.UserTransaction;
import javax.xml.rpc.handler.MessageContext;

import com.ibm.ejs.container.interceptors.InterceptorProxy;
import com.ibm.ejs.container.interceptors.InvocationContextImpl;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.websphere.csi.MethodInterface;
import com.ibm.websphere.ejbcontainer.SessionContextExtension;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.CallbackKind;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.traceinfo.ejbcontainer.TEBeanLifeCycleInfo;

/**
 * A <code>SessionBeanO</code> manages the lifecycle of a
 * single session enterprise bean instance and provides the session
 * context implementation for its associated enterprise bean. <p>
 *
 * A <code>SessionBeanO</code> is an abstract class, designed to be
 * extended by concrete classes managing the bean lifecycle for the
 * two types of session bean, stateful and stateless. <p>
 */
public abstract class SessionBeanO
                extends ManagedBeanOBase
                implements SessionContextExtension, // LI3492-2
                UserTransactionEnabledContext
{
    private static final TraceComponent tc = Tr.register(SessionBeanO.class
                                                         , "EJBContainer"
                                                         , "com.ibm.ejs.container.container"); //p116170

    private static final String CLASS_NAME = "com.ibm.ejs.container.SessionBeanO";

    // Definition shared by StatefulBeanO and StatelessBeanO subclasses
    public static final int DESTROYED = 0;
    public static final int PRE_CREATE = 1; // d367572.1 prior to PostConstruct
    public static final int CREATING = 2; // d367572.1 during PostConstruct

    /**
     * Cached method instance for calling webservice engine
     * MessageContext.getCurrentThreadsContext
     */
    private static Method messageContextMethod;

    /**
     * Session bean associated with this <code>SessionBeanO</code>. <p>
     */

    public SessionBean sessionBean;

    /**
     * Isolation level currently associated with this instance.
     */

    //PQ56091
    protected int currentIsolationLevel = java.sql.Connection.TRANSACTION_NONE;
    //PQ56091

    /*** new for EJB3 *********************************************************/

    /**
     * CallbackKind used to indicate whether or not to call SessionBean callback
     * methods, call interceptor methods, or do nothing (a bean is no longer
     * required to implement SessionBean and it is not required to have any
     * interceptor methods.
     */
    public CallbackKind ivCallbackKind; // d367572.7

    /**
     * Create new <code>SessionBeanO</code> instance. <p>
     *
     * @param c is the EJSContainer instance for this bean.
     * @param b is the enterprise bean instance that is associated with this BeanO object
     *            or null. A null reference is used when this SessionBeanO is being used
     *            for a previously passivated SFSB. In which case, when the enterprise
     *            bean is deserialized from the passivation file, the setEnterpriseBean
     *            method is called at that point in time to associate this BeanO with it.
     * @param h is the home for this bean when the SessionBean itself is not a home.
     *            When the SessionBean is a home, then null must be passed for this parameter.
     */
    // d367572 - change signature and added code for EJB3.
    public SessionBeanO(EJSContainer c, EJSHome h) // d367572
    {
        super(c, h);

        // Is this SessionBeanO for a home object?
        if (home == null)
        {
            // Yep, this is a SessionBeanO for a home object. So set the lifecycle
            // callback kind to NONE since no reason to ever callback to the home bean.
            ivCallbackKind = CallbackKind.None;
        }
        else
        {
            // Nope, this SessionBeanO is not for a home object. So use
            // BeanMetaData to determine the life cycle callback kind to use.
            BeanMetaData bmd = home.beanMetaData;
            ivCallbackKind = bmd.ivCallbackKind;
        }
        // d367572.1 end

    } // SessionBeanO

    @Override
    public void setEnterpriseBean(Object bean)
    {
        super.setEnterpriseBean(bean);
        if (bean instanceof SessionBean)
        {
            sessionBean = (SessionBean) bean;
        }
    }

    /**
     * Invoke any lifecycle interceptors associated with this bean instance.
     * <dl>
     * <dt>pre-condition
     * <li>
     * Called when creating a new session bean instance or destroying an
     * existing session bean instance, not when activating a previously
     * passivated SFSB. This implies that this.state must be set to either
     * PRE_CREATE, CREATING, or PRE_DESTROY (for the relevant bean type).
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
    // d367572.1 added entire method.
    protected void callLifecycleInterceptors(InterceptorProxy[] proxies, int methodId)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        try
        {
            if (isTraceOn) // d527372
            {
                if (TEBeanLifeCycleInfo.isTraceEnabled())
                    TEBeanLifeCycleInfo.traceEJBCallEntry(LifecycleInterceptorWrapper.TRACE_NAMES[methodId]);

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "callLifecycleInterceptors");
            }
            InvocationContextImpl<?> inv = getInvocationContext();
            BeanMetaData bmd = home.beanMetaData; //d450431
            inv.doLifeCycle(proxies, bmd._moduleMetaData); //d450431, F743-14982
        } catch (Throwable t)
        {
            // FFDCFilter.processException( t, CLASS_NAME + ".SessionBeanO", "251", this );

            // Lifecycle interceptors are allowed to throw system runtime exceptions,
            // but NOT application exceptions. Therefore, wrap the caught Throwable
            // in a javax.ejb.EJBException and throw it so that it gets handled as
            // an unchecked exception.
            if (isTraceOn && tc.isDebugEnabled())
            {
                Tr.debug(tc, "SessionBean PostConstruct failure", t);
            }
            throw ExceptionUtil.EJBException("session bean lifecycle interceptor failure", t);
        } finally
        {
            if (isTraceOn && // d527372
                TEBeanLifeCycleInfo.isTraceEnabled())
            {
                TEBeanLifeCycleInfo.traceEJBCallExit(LifecycleInterceptorWrapper.TRACE_NAMES[methodId]);
            }
        }
    }

    /**
     * Invalidating a session bean is a no-op.
     */
    @Override
    public final void invalidate()
    {
        //--------------------------------------------
        // This method body intentionally left blank.
        //--------------------------------------------

    } // invalidate

    ////////////////////////////////////////////////////////////////////////
    //
    // EJBContext interface
    //

    /**
     * Get user transaction object that bean can use to demarcate
     * transactions.
     */

    @Override
    public synchronized UserTransaction getUserTransaction()
    {
        // d367572.1 start
        if (state == PRE_CREATE)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Incorrect state: " + getStateName(state));
            throw new IllegalStateException(getStateName(state));
        }
        // d367572.1 end

        return UserTransactionWrapper.INSTANCE; // d631349

    } // getUserTransaction

    /**
     * Returns the context data associated with this invocation or lifecycle
     * callback. If there is no context data, an empty Map object will be
     * returned.
     **/
    // F743-21028
    @Override
    public Map<String, Object> getContextData()
    {
        // Calling getContextData is not allowed from setSessionContext.
        if (state == PRE_CREATE || state == DESTROYED)
        {
            IllegalStateException ise;

            ise = new IllegalStateException("SessionBean: getContextData " +
                                            "not allowed from state = " +
                                            getStateName(state));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getContextData: " + ise);

            throw ise;
        }

        return super.getContextData();
    }

    /**
     * Checks if beanO can be removed. Throws RemoveException if
     * cannot be removed.
     */
    protected void canBeRemoved()
                    throws RemoveException
    {
        ContainerTx tx = container.getCurrentContainerTx();//d171654

        //-------------------------------------------------------------
        // If there is no current transaction then we are removing a
        // TX_BEAN_MANAGED session bean outside of a transaction which
        // is correct.
        //-------------------------------------------------------------

        if (tx == null)
        {
            return;
        }

        // Stateful beans cannot be removed in a global transaction,
        // unless this is an EJB 3.0 business method designated as a
        // removemethod                                                 d451675

        if (tx.isTransactionGlobal() &&
            tx.ivRemoveBeanO != this)
        {
            throw new RemoveException("Cannot remove session bean " +
                                      "within a transaction.");
        }
    }

    // --------------------------------------------------------------------------
    //
    // Methods from SessionContext interface
    //
    // --------------------------------------------------------------------------

    /**
     * Obtain a reference to the EJB object that is currently associated with
     * the instance. <p>
     *
     * An instance of a session enterprise Bean can call this method at anytime
     * between the ejbCreate() and ejbRemove() methods, including from within the
     * ejbCreate() and ejbRemove() methods. <p>
     *
     * An instance can use this method, for example, when it wants to pass a
     * reference to itself in a method argument or result.
     *
     * @return The EJB object currently associated with the instance.
     *
     * @exception IllegalStateException Thrown if the instance invokes this
     *                method while the instance is in a state that does not allow the
     *                instance to invoke this method, or if the instance does not
     *                have a remote interface.
     **/
    @Override
    public EJBObject getEJBObject()
    {
        EJBObject result = null;

        // d367572.1 start
        if (state == PRE_CREATE || state == DESTROYED)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Incorrect state: " + getStateName(state));
            throw new IllegalStateException(getStateName(state));
        }
        // d367572.1 end

        try
        {
            //
            // Convert this result to a stub. Stubs are essential
            // to our exception handling semantics as well as to other
            // ORB related behaviors.
            //                                                    d111679 d156807.1
            EJSWrapper wrapper = container.wrapperManager.getWrapper(beanId).getRemoteWrapper();
            Object wrapperRef = container.getEJBRuntime().getRemoteReference(wrapper);

            // "The container must implement the SessionContext.getEJBObject
            // method such that the bean instance can use the Java language cast
            // to convert the returned value to the session bean’s remote
            // component interface type. Specifically, the bean instance does
            // not have to use the PortableRemoteObject.narrow method for the
            // type conversion."
            result = (EJBObject) PortableRemoteObject.narrow(wrapperRef, home.beanMetaData.remoteInterfaceClass);
        } catch (IllegalStateException ise) { // d116480
            // FFDC not logged for this spec required scenario
            throw ise;
        } catch (Exception ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".getEJBObject", "204", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) // d144064
                Tr.debug(tc, "getEJBObject() failed", ex);
            throw new IllegalStateException("Failed to obtain EJBObject", ex);
        }
        return result;
    }

    /**
     * d111408, d112442 start
     * Get the EJBLocalObject for this <code>BeanO</code>. In the EJS,
     * the <code>EJBLocalObject</code> is the <code>EJSLocalWrapper</code>. <p>
     */
    @Override
    public EJBLocalObject getEJBLocalObject() throws java.lang.IllegalStateException
    {
        EJBLocalObject result = null;

        // d367572.1 start
        if (state == PRE_CREATE || state == DESTROYED)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Incorrect state: " + getStateName(state));
            throw new IllegalStateException(getStateName(state));
        }
        // d367572.1 end

        try
        {
            result = container.wrapperManager.getWrapper(beanId).getLocalObject();// d156807.1
        } catch (IllegalStateException ise) { // d116480
            // FFDC not logged for this spec required scenario
            throw ise;
        } catch (Throwable ex) {
            // p113724
            // Not expecting any Throwable to occur. Log unexpected Throwable and
            // throw a ContainerEJBException since this is a Java EE architected interface.
            FFDCFilter.processException(ex, CLASS_NAME + ".getEJBLocalObject", "236", this);
            ContainerEJBException cex = new ContainerEJBException("getEJBLocalObject() failed",
                            ex);
            Tr.error(tc
                     , "CAUGHT_EXCEPTION_THROWING_NEW_EXCEPTION_CNTR0035E"
                     , new Object[] { ex, cex.toString() }); // d194031
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) // d144064
                Tr.debug(tc, "getEJBLocalObject() failed", ex);
            throw cex;
        }

        return result;
    } // d111408, d112442 end

    /**
     * Return the bean instance associated with this BeanO
     */
    // LIDB2617.11
    // Chanced EnterpriseBean to Object.                                d366807.1
    @Override
    public Object getBeanInstance()
    {
        return ivEjbInstance;
    } // getBeanInstance()

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
    public Object[] getInterceptors()
    {
        return ivInterceptors;
    }

    /**
     * Obtain a reference to the JAX-RPC MessageContext. <p>
     *
     * An instance of a stateless session bean can call this method
     * from any business method invoked through its web service
     * endpoint interface. <p>
     *
     * @return The MessageContext for this web service invocation.
     *
     * @exception IllegalStateException Thrown if this method is invoked
     *                while the instance is in a state that does not allow access
     *                to this method.
     **/
    // LI2281
    @Override
    public MessageContext getMessageContext()
                    throws IllegalStateException
    {
        boolean isEndpointMethod = false; // LI2281.07

        // Get the method context for the current thread, and check to see if
        // the current method is from WebService Endpoint interface.       d174057
        Object context = EJSContainer.getMethodContext(); // d646139.1
        if (context instanceof EJSDeployedSupport)
        {
            EJSDeployedSupport s = (EJSDeployedSupport) context;
            if (s.methodInfo.ivInterface == MethodInterface.SERVICE_ENDPOINT)
            {
                isEndpointMethod = true;
            }
        }

        // getMessageContext is allowed only from within Web Service Endpoint
        // interface methods.  StatefulSessionBeanO overrides this method and
        // throws an exception for that spec restriction.
        if (!isEndpointMethod)
        {
            IllegalStateException ise;

            // Not an allowed method for Stateful beans per EJB Specification.
            ise = new IllegalStateException("SessionBean: getMessageContext not " +
                                            "allowed from Non-WebService " +
                                            "Endpoint method");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getMessageContext: " + ise);

            throw ise;
        }

        if (messageContextMethod == null) {
            try {
                messageContextMethod =
                                Class.forName("com.ibm.ws.webservices.engine.MessageContext")
                                                .getMethod("getCurrentThreadsContext", (Class[]) null);
            } catch (SecurityException e) {
                FFDCFilter.processException(
                                            e,
                                            CLASS_NAME + ".getMessageContext",
                                            "348",
                                            this);
            } catch (NoSuchMethodException e) {
                FFDCFilter.processException(
                                            e,
                                            CLASS_NAME + ".getMessageContext",
                                            "354",
                                            this);
            } catch (ClassNotFoundException e) {
                FFDCFilter.processException(
                                            e,
                                            CLASS_NAME + ".getMessageContext",
                                            "360",
                                            this);
            }
        }

        MessageContext theMessageContext = null;
        try {
            theMessageContext = (MessageContext) messageContextMethod.invoke(null, (Object[]) null);
        } catch (IllegalArgumentException e) {
            FFDCFilter.processException(
                                        e,
                                        CLASS_NAME + ".getMessageContext",
                                        "372",
                                        this);
        } catch (IllegalAccessException e) {
            FFDCFilter.processException(
                                        e,
                                        CLASS_NAME + ".getMessageContext",
                                        "378",
                                        this);
        } catch (InvocationTargetException e) {
            FFDCFilter.processException(
                                        e,
                                        CLASS_NAME + ".getMessageContext",
                                        "384",
                                        this);
        }

        return theMessageContext;

    }

    /**
     * Obtain an object that can be used to invoke the current bean through
     * the given business interface. <p>
     *
     * @param businessInterface One of the local business interfaces or remote
     *            business interfaces for this session bean.
     *
     * @return The business object corresponding to the given business interface.
     *
     * @throws IllegalStateException - Thrown if this method is invoked with an
     *             invalid business interface for the current bean.
     **/
    @Override
    // New for EJB 3.0    d366807.1
    @SuppressWarnings("unchecked")
    public <T> T getBusinessObject(Class<T> businessInterface)
                    throws IllegalStateException
    {
        Object result = null;

        // d367572.1 start
        if (state == PRE_CREATE || state == DESTROYED)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getBusinessObject: Incorrect state: " + getStateName(state));
            throw new IllegalStateException(getStateName(state));
        } // d367572.1 end

        if (businessInterface == null)
        {
            throw new IllegalStateException("Requested business interface not found : null");
        }

        try
        {
            EJSWrapperCommon common = container.wrapperManager.getWrapper(beanId);
            result = common.getBusinessObject(businessInterface.getName());
        } catch (IllegalStateException ise)
        {
            // FFDC not logged for this spec required scenario
            throw ise;
        } catch (Throwable ex)
        {
            // Not expecting any Throwable to occur. Log unexpected Throwable and
            // throw an EJBException since this is a Java EE architected interface.
            FFDCFilter.processException(ex, CLASS_NAME + ".getBusinessObject", "516", this);
            EJBException ejbex = ExceptionUtil.EJBException("getBusinessObject() failed",
                                                            ex);
            Tr.error(tc,
                     "CAUGHT_EXCEPTION_THROWING_NEW_EXCEPTION_CNTR0035E",
                     new Object[] { ex, ejbex.toString() });
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getBusinessObject() failed", ex);
            throw ejbex;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getBusinessObject : " + result.getClass().getName());

        return (T) result;
    }

    /**
     * Obtain the business interface through which the current business
     * method invocation was made. <p>
     *
     * @throws IllegalStateException - Thrown if this method is called and
     *             the bean has not been invoked through a business interface.
     **/
    // New for EJB 3.0    d366807.1
    @Override
    public Class<?> getInvokedBusinessInterface()
                    throws IllegalStateException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        // d367572.1 start
        synchronized (this)
        {
            if ((state == PRE_CREATE) ||
                (state == CREATING) ||
                (state == DESTROYED))
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Incorrect state: " + getStateName(state));
                throw new IllegalStateException(getStateName(state));
            }
        }
        // d367572.1 end

        Class<?> bInterface = null;
        EJSWrapperBase wrapper = null;

        // -----------------------------------------------------------------------
        // Obtain the Method Context for the current thread (EJSDeployedSupport),
        // which contains the 'wrapper' used to invoke the current method.
        //
        // The invoked 'business interface' is the first and only interface
        // for the wrapper class.
        //
        // Note that this method is NOT allowed for the EJB 2.1 component
        // interfaces... and results in an exception.
        // -----------------------------------------------------------------------

        EJSDeployedSupport methodContext = EJSContainer.getMethodContext();

        // d367572.7 start of eliminate potential NPE if methodContext is null.
        if (methodContext != null)
        {
            wrapper = methodContext.ivWrapper;

            if (wrapper != null)
            {
                // For an aggregate wrapper, all interfaces must be evaluated for
                // exposing the method. If a single interface is found, return it,
                // otherwise it is ambiguous.                             F743-34304
                if (wrapper.ivBusinessInterfaceIndex == EJSWrapperBase.AGGREGATE_LOCAL_INDEX)
                {
                    Method method = methodContext.methodInfo.ivMethod;
                    String methodName = method.getName();
                    Class<?>[] methodParams = method.getParameterTypes();
                    for (Class<?> curInterface : wrapper.bmd.ivBusinessLocalInterfaceClasses)
                    {
                        try
                        {
                            curInterface.getMethod(methodName, methodParams);
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "Method " + methodName + " found on interface " + curInterface);
                            if (bInterface != null)
                            {
                                throw new IllegalStateException("Ambiguous invoked business interface.");
                            }
                            bInterface = curInterface;
                        } catch (SecurityException ex) // d679175
                        {
                            // this normal and means the interface doesn't expose this method.
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "Method " + methodName + " not found on interface " + curInterface);
                        } catch (NoSuchMethodException ex) // d679175
                        {
                            // this normal and means the interface doesn't expose this method.
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "Method " + methodName + " not found on interface " + curInterface);
                        }
                    }
                }

                // For performance (and in case No-Interface view) obtain the
                // 'interface' using the index provided on the wrapper, rather
                // than using reflections as done in prior releases.       F743-1756
                else if (wrapper.ivInterface == WrapperInterface.BUSINESS_LOCAL)
                {
                    bInterface = wrapper.bmd.ivBusinessLocalInterfaceClasses[wrapper.ivBusinessInterfaceIndex];
                }
                else if (wrapper.ivInterface == WrapperInterface.BUSINESS_REMOTE ||
                         wrapper.ivInterface == WrapperInterface.BUSINESS_RMI_REMOTE)
                {
                    bInterface = wrapper.bmd.ivBusinessRemoteInterfaceClasses[wrapper.ivBusinessInterfaceIndex];
                }
            }
        } // d367572.7 end

        if (bInterface == null)
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "getInvokedBusinessInterface : IllegalStateException : " +
                             "Not invoked through business interface");

            throw new IllegalStateException("Not invoked through business interface");
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "getInvokedBusinessInterface : " + bInterface);

        return bInterface;
    }

    /**
     * Check whether a client invoked the cancel() method on the client Future
     * object corresponding to the currently executing asynchronous business
     * method.
     *
     * @return true if the client has invoked Future.cancel with a value of
     *         true for the mayInterruptIfRunning parameter.
     * @throws java.lang.IllegalStateException - Thrown if not invoked from
     *             within an asynchronous business method invocation with return
     *             type Future.
     */
    @Override
    public boolean wasCancelCalled() // F743-11774
    {
        EJSDeployedSupport methodContext = EJSContainer.getMethodContext();
        ServerAsyncResult asyncResult = methodContext == null ? null : methodContext.ivAsyncResult;

        if (asyncResult == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "wasCancelCalled : IllegalStateException : " +
                             "Not invoked from an asynchronous method with results");

            throw new IllegalStateException("Not invoked from an asynchronous method with results");
        }

        boolean result = asyncResult.wasCancelCalled();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "wasCancelCalled : " + result);

        return result;
    }

    /**
     * Get isolation level currently associated with this session
     * bean instance. This is used for determining the isolation level
     * to use in a bean managed transaction.
     *
     * The current isolation level is maintained in a stack with the
     * assistance of the EJSDeployedSupport instance passed to
     * preInvoke/postInvoke. TJB - This needs some more work!
     */

    @Override
    public int getIsolationLevel()
    {
        return currentIsolationLevel;
    } // getIsolationLevel

    /**
     * Method to allow container to condition removal of EJB from the EJB
     * cache on whether the BeanO is in destroyed state (EJB's are not in
     * the cache if there state is destroyed).
     **/
    // dPQ51806
    @Override
    public boolean isDestroyed()
    {
        if (state == DESTROYED)
        {
            return true;
        }
        return false;
    }

    /**
     * This method is called by the optc and optb activators to make sure that
     * load is called before a store for optimistic concurrency cases.
     *
     * @param tx ContainerTx for the current transaction.
     */
    // d132828
    @Override
    public void ensurePersistentState(ContainerTx tx) // d139352-2
    throws RemoteException
    {
        // method intentionally left blank
        // provides implementation for those not implementing (non CMP 1.1)
    }

    //d140003.20
    // we need the module to tell whether to set isolation level for user transactions
    @Override
    public int getModuleVersion()
    {
        return home.beanMetaData.ivModuleVersion;
    }
} // SessionBeanO

/*******************************************************************************
 * Copyright (c) 1998, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJBException;

import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.ejs.csi.UOWCookie;
import com.ibm.ejs.j2c.HandleList;
import com.ibm.websphere.csi.CSITransactionRolledbackException;
import com.ibm.websphere.csi.ExceptionType;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.csi.DispatchEventListenerCookie;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;
import com.ibm.ws.ejbcontainer.EJBRequestData;
import com.ibm.ws.ejbcontainer.EJBSecurityCollaborator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MethodMetaData;
import com.ibm.ws.util.InvocationCallback;
import com.ibm.ws.util.InvocationToken;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * This helper class encapsulates various information collected when a
 * bean method is invoked, including whether the invocation caused a
 * transaction to be started and whether the invocation threw an exception.
 */

public class EJSDeployedSupport implements EJBRequestData, InvocationToken {
    private static final TraceComponent tc = Tr.register(EJSDeployedSupport.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    private static final String CLASS_NAME = "com.ibm.ejs.container.EJSDeployedSupport"; //d194342.1.1

    /**
     * Thread data for the thread that owns this stack. This data does not need
     * to be reset in {@link #initializeInstanceData} because this object will
     * always be bound to the same thread.
     */
    final EJBThreadData ivThreadData; // d630940

    /**
     * Opaque data stacked for transaction collaborator.
     */
    UOWCookie uowCookie;

    boolean uowCtrlPreInvoked;
    long pmiCookie;
    boolean pmiPreInvoked;

    /**
     * The security collaborator on which preInvoke was called, or null if either
     * security is not enabled or preInvoke has not yet been called. It is held
     * on to so that the same EJBSecurityCollaborator can be used during
     * postInvoke, even if security has since been removed.
     */
    EJBSecurityCollaborator<?> ivSecurityCollaborator;

    /**
     * Number of BeforeActivation Collaborators successfully invoked
     * during preInvoke. postInvoke should only be called for those.
     **/
    int ivBeforeActivationPreInvoked;

    /**
     * Number of BeforeActivationAfterCompletion Collaborators successfully invoked
     * during preInvoke. postInvoke should only be called for those.
     **/
    int ivBeforeActivationAfterCompletionPreInvoked;

    /**
     * Number of AfterActivation Collaborators successfully invoked
     * during preInvoke. postInvoke should only be called for those.
     **/
    int ivAfterActivationPreInvoked;

    /**
     * Set by ContainerTx to current state of txStarted.
     */
    boolean began;

    /**
     * Used by ContainerTx to store previous state of txStarted.
     */
    boolean previousBegan;

    /**
     * The BeanO associated with the enterprise bean method is invoked on. Note
     * that this differs from {@link EJBThreadData#ivCallbackBeanO}, which
     * should be used instead of this field for code that wants to determine the
     * active bean for the thread.
     */
    BeanO beanO;

    /**
     * An exception occurred during preInvoke processing.
     **/
    boolean preInvokeException;

    /**
     * Indicates whether the wrapper needs to be unpinned on postInvoke.
     */
    boolean unpinOnPostInvoke = true;

    /**
     * Hold on to a reference to the EJBMethodInfo object useful for
     * printing out exception information.
     */
    EJBMethodInfoImpl methodInfo;

    /**
     * Method ID of the method associated with the EJBMethodInfo object.
     */
    int methodId; // d130230

    /**
     * Collaborator cookies returned by BeforeActivationCollaborator.preInvoke
     * and passed to BeforeActivationCollaborator.postInvoke. Lazily
     * initialized when a collaborator returns a non-null cookie.
     */
    Object[] ivBeforeActivationCookies;

    /**
     * Collaborator cookies returned by
     * BeforeActivationAfterCompletionCollaborator.preInvoke and passed to
     * BeforeActivationAfterCompletionCollaborator.postInvoke. Lazily
     * initialized when a collaborator returns a non-null cookie.
     */
    Object[] ivBeforeActivationAfterCompletionCookies;

    /**
     * Collaborator cookies returned by AfterActivationCollaborator.preInvoke
     * and passed to AfterActivationCollaborator.postInvoke. Lazily
     * initialized when a collaborator returns a non-null cookie.
     */
    Object[] ivAfterActivationCookies;

    /**
     * Security collaborator context returned by the SecurityCollaborator
     * .preInvoke method. This context is passed to the SecurityCollaborator
     * .postInvoke method.
     */
    Object securityCookie;

    /**
     * The ContainerTx instance for the transaction on which the
     * current method is being dispatched.
     */
    ContainerTx currentTx;

    /**
     * This pair of values provides a stack of isolation levels for
     * use with stateful session beans using bean managed transactions.
     */
    int currentIsolationLevel;
    int oldIsolationLevel;

    /**
     * Stores the result of ThreadContextAccessor.pushContextClassLoader.
     */
    Object oldClassLoader = ThreadContextAccessor.UNCHANGED;

    /**
     * Type of exception, if any, raised by the bean method invocation.
     */
    ExceptionType exType = ExceptionType.NO_EXCEPTION;

    /**
     * The exception, if any, thrown by the bean method.
     */
    Throwable ivException;

    /**
     * The root exception, if any, associated with the exception thrown
     * by the bean method. See ex.
     **/
    Throwable rootEx; // d109641.1

    /**
     * Any exception set here will be thrown by the calling wrapper.
     */
    Exception exceptionToBeThrown;

    /**
     * True for methods that never throw application exceptions. For example,
     * lifecycle callback interceptors or void asynchronous methods. If true,
     * then only setUncheckedException can be called, and it will throw an
     * EJBException.
     */
    // F743761.CodRv
    boolean ivIgnoreApplicationExceptions;

    /**
     * If security is enabled, JACC object array passed to
     * preinvoke or null (null if either security disabled
     * or old generated code called preinvoke).
     */
    protected Object[] ivEJBMethodArguments; // LIDB2617.11

    /**
     * Set to true if the EJB that began the transaction called
     * the EJB Context method setRollbackOnly().
     **/
    protected boolean ivBeginnerSetRollbackOnly;

    /**
     * A list of objects that implement the InvocationCallback interface.
     */
    ArrayList<InvocationCallback> ivEJBMethodCallback; // d194342.1.1

    /**
     * A list of cookie objects that must be passed to the postInvoke
     * method implementor of InvocationCallback interface. The cookie
     * corresponds to the callback object in the ivEJBMethodCallback array list.
     */
    ArrayList<Object> ivEJBMethodCallbackCookie; // d194342.1.1

    /**
     * Set to true if the current EJB Method invocation is running in
     * 'Lightweight' mode, where pre/postInvoke collaborators are skipped.
     */
    protected boolean isLightweight;

    /**
     * The 'Wrapper' object through which the current method invocation was made.
     **/
    protected EJSWrapperBase ivWrapper;

    /**
     * Either the JAX-WS MessageContext data, the interceptor InvocationContext
     * data, or an empty context data. If this method context was created for a
     * JAX-WS call, then this field is initialized prior to preinvoke.
     * Otherwise, this field is created lazily.
     **/
    Map<String, Object> ivContextData; // F743-21028

    /**
     * Set to a positive number if the method UOWManager.runUnderUOW() has been
     * called within the scope of this EJB method context, indicating that a
     * user initiated transaction is currently in effect.
     **/
    protected int ivRunUnderUOW;

    /**
     * When set to true, currentTx needs to be reset during postInvoke
     * because the value set during preInvoke or processTxContextChange
     * may no longer be valid.
     */
    protected boolean resetCurrentTx;

    /**
     * Set to the Stateful bean instance being created within the scope of
     * this method context. Typically, this would be when this method context
     * corresponds to a home create method... and the 'beanO' field is that
     * of the home. Provides access to the current extended-scoped persistence
     * context.
     **/
    protected StatefulBeanO ivCreateBeanO;

    /**
     * Set to the calling method context. May be null if this is the first
     * method called on the thread. Useful in determining if an extended-scoped
     * persistence context should be inherited.
     **/
    protected EJSDeployedSupport ivCallerContext;

    /**
     * Set to the z/OS dispatch cookies, if event listeners are active, at the
     * time the method is called.
     */
    protected DispatchEventListenerCookie[] ivDispatchEventListenerCookies; // d646413.2

    /**
     * The HandleList used by EJB 3.1 Singleton preInvoke of
     * HandleCollaborator. null when smart handles are being used or when
     * no postInvoke of HandleCollaborator is required. Note, since Singleton
     * is re-entrant, a thread local variable will not work. Thus, keeping
     * this field in EJSDeployedSupport is better since each method invocation
     * gets its own EJSDeployedSupport. J2C team reviewed this design and indicated
     * that is the best solution to use.
     */
    protected HandleList ivHandleList; //F743-509

    /**
     * Used when container managed concurrency control is used for a Singleton
     * session bean. Set to true if and only if preInvoke did acquire a lock
     * that postInvoke must release.
     */
    protected boolean ivLockAcquired; // F743-509

    /**
     * The result of the asynchronous method being invoked. This field will be
     * null if the current method is not being invoked asynchronously.
     */
    ServerAsyncResult ivAsyncResult; // F743-11774

    /**
     * Set to true during preInvoke if a pushCallbackBeanO has been performed,
     * requiring a corresponding popCallbackBeanO during postInvoke.
     */
    boolean ivPopCallbackBeanORequired;

    /**
     * True if the current method invocation is the timeout callback for a
     * persistent timer that will run in a global transaction.
     */
    boolean isPersistentTimeoutGlobalTx;

    /**
     * Messsage endpoint context. Set during MessageEndpointCollaborator processing.
     */
    Map<String, Object> messageEndpointContext;

    /**
     * Create a new instance.
     *
     * <p>NOTE: This constructor must exist to support ejbdeploy wrappers.
     */
    public EJSDeployedSupport() {
        ivThreadData = EJSContainer.getThreadData();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
               '[' + methodInfo.getMethodSignature() +
               ", " + methodInfo.getEJBMethodInterface() +
               ", " + (beanO != null ? beanO : ivWrapper.beanId) +
               ']';
    }

    @Override
    public EJBMethodMetaData getEJBMethodMetaData() {
        return methodInfo;
    }

    @Override
    public Object[] getMethodArguments() {
        return ivEJBMethodArguments;
    }

    @Override
    public BeanId getBeanId() {
        return ivWrapper.beanId;
    }

    @Override
    public Object getBeanInstance() {
        if (methodInfo.isHome()) {
            return null;
        }

        if (beanO == null) {
            // This method may only be called by after activation collaborators.
            IllegalStateException ex = new IllegalStateException();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getBeanInstance: called before activation", ex);
            throw ex;
        }

        return beanO.getBeanInstance();
    }

    /**
     * Returns exception, if any, raised by bean method invocation.
     */
    public final Throwable getException() {
        return ivException;
    } // getException

    /**
     * Returns root exception, if any, raised by bean method invocation.
     */
    public final Throwable getRootCause() {
        return rootEx;
    } // getRootCause

    /**
     * Capture a checked exception thrown by a bean method. <p>
     *
     * A checked exception is one declared in the signature of
     * the bean method. <p>
     *
     * @param ex the <code>Throwable</code> thrown by bean method <p>
     */
    public final void setCheckedException(Exception ex) {
        getExceptionMappingStrategy().setCheckedException(this, ex); // d135756
    } // setCheckedException

    /**
     * Capture an unchecked exception thrown by a bean method and
     * reraise it as a remote exception. <p>
     *
     * An unchecked exception is one that is not declared in the signature
     * of the bean method. <p>
     *
     * @param ex the <code>Exception</code> thrown by bean method <p>
     */
    // d395666 - rewrote entire method.
    public final void setUncheckedException(Throwable ex) throws RemoteException {
        ExceptionMappingStrategy exceptionStrategy = getExceptionMappingStrategy();
        Throwable mappedException = exceptionStrategy.setUncheckedException(this, ex);
        if (mappedException != null) {
            if (mappedException instanceof RemoteException) {
                throw (RemoteException) mappedException;
            } else if (mappedException instanceof RuntimeException) {
                throw (RuntimeException) mappedException;
            } else if (mappedException instanceof Error) {
                throw (Error) mappedException; // d395666
            } else {
                // Unless there is a defect in mapping strategy, this should
                // never happen.  But if it does, we are going to wrap
                // what is returned with a RemoteException. This is added
                // measure to ensure we do not break applications that
                // existed prior to EJB 3.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "unexpected Throwable returned by exception mapping strategy", new Object[] { mappedException, exceptionStrategy });
                }
                throw ExceptionUtil.RemoteException(mappedException);
            }
        }
    } // setUncheckedException

    //  For backward compatibility with previously deployed code.       d111077.1
    public final void setUncheckedException(Exception ex) throws RemoteException {
        setUncheckedException((Throwable) ex);
    } // setUncheckedException

    // d395666 - rewrote entire method.
    public final void setUncheckedLocalException(Throwable ex) throws EJBException {
        ExceptionMappingStrategy exceptionStrategy = getExceptionMappingStrategy();
        Throwable mappedException = exceptionStrategy.setUncheckedException(this, ex);
        if (mappedException != null) {
            if (mappedException instanceof EJBException) {
                throw (EJBException) mappedException;
            } else if (mappedException instanceof RuntimeException) {
                throw (RuntimeException) mappedException;
            } else if (mappedException instanceof Error) {
                throw (Error) mappedException;
            } else {
                // Unless there is a defect in mapping strategy, this should
                // never happen.  But if it does, we are going to
                // wrap what is returned with a EJBException. This is added
                // measure to ensure we do not break applications that
                // existed prior to EJB 3.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "unexpected Throwable returned by exception mapping strategy", new Object[] { mappedException, exceptionStrategy });
                }
                throw ExceptionUtil.EJBException(mappedException);
            }
        }
    } // setUncheckedException

    /**
     * Get <code>BeanO</code> instance associated with enterprise bean
     * method was invoked on. Note that this differs from
     * EJBThreadData.ivCallbackBeanO, which should usually be used instead of
     * this method.
     */
    public final BeanO getBeanO() {
        return beanO;
    } // getBeanO

    /**
     * Get the ContainerTx instance on which the current method is
     * being dispatched.
     */
    public final ContainerTx getCurrentTx() {
        return currentTx;
    }

    public final EJSContainer getContainer() {
        return ivWrapper == null ? EJSContainer.getDefaultContainer() : ivWrapper.container; //d583637
    }

    public StatefulBeanO getCachedWrapperBeanO() {
        EJSWrapperCommon wc = ivWrapper.ivCommon;
        return wc == null ? null : wc.ivCachedBeanO;
    }

    final void setEJBMethodInfo(EJBMethodInfoImpl m) {
        this.methodInfo = m;
    }

    /**
     * Internal method to allow EJB Container code to get the EJBMethodInfo
     * implementation object and access the instance variables directly.
     */
    public EJBMethodInfoImpl getEJBMethodInfoImpl() {
        return this.methodInfo;
    }

    /**
     * Returns the container managed concurrency access timeout value for
     * the current method invocation in milliseconds. <p>
     *
     * This method is only applicable to singleton beans with container managed
     * concurrency or stateful beans.
     */
    public final long getConcurrencyAccessTimeout() {
        return methodInfo.ivAccessTimeout;
    }

    public final ExceptionType getExceptionType() {
        return exType;
    }

    /**
     * Get the rollback setting that was specified in either a application-exception
     * deployment descriptor for a specified Throwable that occurred or in ApplicationException
     * annotation if Throwable is annotated.
     *
     * @param t is the Throwable.
     *
     * @return null if no application-exception DD or annotation was provided for the Throwable.
     *         static variable Boolean.TRUE is returned if rollback is "true".
     *         static variable Boolean.FALSE is returned if rollback is "false".
     */
    // d395666 - added entire method.
    protected Boolean getApplicationExceptionRollback(Throwable t) {
        Boolean rollback;
        if (ivIgnoreApplicationExceptions) {
            rollback = null;
        } else {
            ComponentMetaData cmd = getComponentMetaData();
            EJBModuleMetaDataImpl mmd = (EJBModuleMetaDataImpl) cmd.getModuleMetaData();
            rollback = mmd.getApplicationExceptionRollback(t);
        }
        return rollback;
    }

    ExceptionMappingStrategy getExceptionMappingStrategy() {
        return ivWrapper.ivInterface.ivExceptionStrategy;
    }

    public final Exception mapCSITransactionRolledBackException(CSITransactionRolledbackException ex) throws com.ibm.websphere.csi.CSIException {
        return getExceptionMappingStrategy().mapCSITransactionRolledBackException(this, ex);
    }

    /**
     * Set method ID of current method being processed by EJB container.
     *
     * @param methodid is the method ID for the current method
     *            being processed by the ejb container. For PM internal
     *            home methods, the methodId is < 0 and the constants
     *            for PM internal home methods are defined in EJSContainer.
     *            See MID_getLink, MID_REMOTE_HOME_INDEX,
     *            and MID_LOCAL_HOME_INDEX constants in EJSContainer.
     */
    final public void setEJBMethodId(int methodid) {
        this.methodId = methodid;
    }

    /**
     * Get te method ID of the current method being processed by EJB container.
     **/
    public final int getEJBMethodId() {
        return methodId;
    }

    /**
     * Returns boolean true if transaction began in the scope of
     * the current method invocation.
     *
     * Since the PM home internal methods are hidden from the bean
     * developer and client programmers, they are considered a
     * continuation of the prior method invocation.
     */
    final public boolean beganInThisScope() {
        // Return true if current tran began in the scope of this method
        // or the current method is a PM home internal call and a transaction
        // began in the scope of the prior method invocation.
        return ((this.began) || // d156688
                (this.previousBegan && (this.methodId < 0)));
    }

    /**
     * Returns boolean true if transaction began and end in the scope of
     * the current method invocation.
     *
     * Since the PM home internal methods are hidden from the bean
     * developer and client programmers, they are considered a
     * continuation of the prior method invocation.
     */
    final public boolean beganAndEndInThisScope() {
        // Return true if current tran began in the scope of this method
        // or the current method is a PM home internal call and a transaction
        // began in the scope of the prior method invocation.
        return ((this.began) || // d156688
                (this.previousBegan && (this.methodId < 0)));
    }

    /**
     * Enlist an InvocationCallback object for the EJB method that is
     * currently being executed by the calling thread. The postInvoke method
     * on the callback object is called when execution of current EJB method
     * completes and after the transaction has completed.
     *
     * <b>pre-condition</b>
     * <ul>
     * <li>
     * This method can only be called while a thread is executing a EJB method
     * invocation.
     * </ul>
     * <p>
     * <b>post-condition</b>
     * <ul>
     * <li>
     * The reference to the cookie object passed to this method will be the same
     * reference that will be passed to the postInvoke callback method.
     * <li>
     * EJB container will not modify or copy the cookie in any manner.
     * </ul>
     * <p>
     *
     * @param callback is an object that implements the InvocationCallback interface.
     *            The postInvoke method of this interface is called when the currently
     *            executing EJB method completes. No promise is made in regards to
     *            the ordering between different callback objects that enlist or between the
     *            callback objects and other EJB container collaborators.
     *
     * @param cookie is an object that is passed to the postInvoke method when
     *            the currently executing EJB method completes. Note, a null reference
     *            is valid if the callback does not have a useful cookie to use.
     */
    //d194342.1.1 - added entire method.
    @Override
    public void enlistInvocationCallback(InvocationCallback callback, Object cookie) {
        if (ivEJBMethodCallback == null) {
            ivEJBMethodCallback = new ArrayList<InvocationCallback>();
            ivEJBMethodCallbackCookie = new ArrayList<Object>();
        }

        ivEJBMethodCallback.add(callback);
        ivEJBMethodCallbackCookie.add(cookie);
    }

    /**
     * Call postInvoke on each of the InvocationCallback objects enlisted
     * by the enlistInvocationCallback method of this class.
     *
     * <b>pre-condition</b>
     * <ul>
     * <li>
     * EJB method execution completed and the transaction is completed.
     * </ul>
     */
    //d194342.1.1 - added entire method.
    public void invocationCallbackPostInvoke() {
        if (ivEJBMethodCallback != null) {
            int n = ivEJBMethodCallback.size();
            for (int i = 0; i < n; i++) {
                try {
                    InvocationCallback callback = ivEJBMethodCallback.get(i);
                    callback.postInvoke(ivEJBMethodCallbackCookie.get(i));
                } catch (Throwable t) {
                    FFDCFilter.processException(t, CLASS_NAME +
                                                   ".ejbMethodCallbackPostInvoke",
                                                "332", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc,
                                 "ignoring exception thrown by EJBMethodCallback.postInvoke",
                                 t);
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.util.InvocationToken#getContainerType()
     */
    // d194342.1.1 - added entire method.
    @Override
    final public int getContainerType() {
        return InvocationToken.EJB_CONTAINER;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.util.InvocationToken#getContainerType()
     */
    @Override
    final public ComponentMetaData getComponentMetaData() {
        return methodInfo.getComponentMetaData();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.util.InvocationToken#getContainerType()
     */
    @Override
    final public MethodMetaData getMethodMetaData() {
        return methodInfo;
    }

    /**
     * Returns the context data associated with this method invocation.
     **/
    public Map<String, Object> getContextData() {
        if (ivContextData == null) {
            ivContextData = new HashMap<String, Object>();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getContextData: created empty");
        }

        return ivContextData;
    }

    /**
     * Returns the binding context for the currently active extended-scoped
     * persistence context for the thread of execution. Null will be returned
     * if an extended-scoped persistence context is not currently active. <p>
     *
     * @return binding context for currently active extended-scoped
     *         persistence context.
     */
    public Object getExPcBindingContext() {
        Object jpaContext = null;

        // First, if a new bean instance is being created in this method context,
        // then the jpa context of that new bean instance is the current active
        // context... even if it is null.
        if (ivCreateBeanO != null) {
            jpaContext = ivCreateBeanO.ivExPcContext;
        } else if (beanO != null) {
            // Second, if the bean the current method was invoked on is NOT
            // a home (i.e. it has a home) then the jpa context is that of
            // the current bean... even if it is null.
            if (beanO.home != null) {
                if (beanO instanceof StatefulBeanO) {
                    jpaContext = ((StatefulBeanO) beanO).ivExPcContext;
                }
            }
            // Finally, if the bean associated with this method context is a
            // stateful home then the jpa context is that of the calling
            // bean (i.e. the bean that called home.create).
            else if (ivWrapper.bmd.isStatefulSessionBean() && ivCallerContext != null) {
                jpaContext = ivCallerContext.getExPcBindingContext();
            }
        }

        return jpaContext;
    }

    /**
     * Return true if the activated bean needs to be enlisted in the active
     * transaction for this method invocation.
     */
    public boolean isTxEnlistNeededForActivate() {
        // If uowCtrlPreInvoked wasn't called, this must be a lightweight method,
        // so the bean doesn't need to enlist with the transaction.
        return uowCtrlPreInvoked;
    }

    /**
     * Returns the messsage endpoint context set during MessageEndpointCollaborator processing.
     * 
     * @return The message endpoint context map.
     */
    public Map<String, Object> getMessageEndpointContext() {
        return messageEndpointContext;
    }
    
    /**
     * Sets the messsage endpoint context. Set during MessageEndpointCollaborator processing.
     * 
     * @return The message endpoint context map.
     */
    public void setMessageEndpointContext(Map<String, Object> meContext) {
         messageEndpointContext = meContext;
    }
    
} // EJSDeployedSupport

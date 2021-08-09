/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jbatch.jms.internal.listener.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;

import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jbatch.jms.internal.listener.BatchJmsEndpointListener;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.tx.embeddable.RecoverableXAResourceAccessor;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * This file is adapted from the MessageEndpointHandler in the
 * com.ibm.ws.ejbcontainer.mdb.internal component
 * 
 * This class is a java.lang.reflect.InvocationHandler that is used when
 * invoking a method on a JCA 1.5 MessageEndpoint proxy. A JCA 1.5
 * MessageEndpoint proxy must implement both the
 * javax.resource.spi.MessageEndpoint interface as well as the message listener
 * interface . Therefore, this handler must be able to handle both invocations
 * for the MessageEndpoint interface and the message listener interface and
 * perform whatever actions is needed prior to invoking method on the real
 * object.
 * 
 */

public class MessageEndpointHandler implements MessageEndpoint, InvocationHandler {

    private static final String CLASS_NAME = MessageEndpointHandler.class.getName();
    private static final TraceComponent tc = Tr.register(MessageEndpointHandler.class);

    // Method objects for the methods inherited from java.lang.Object
    final private static Method cvToStringMethod;
    final private static Method cvEqualsMethod;
    final private static Method cvHashCodeMethod;

    // static initializer for the Method objects in java.lang.Object
    static {
        Method toStringMethod = null;
        Method equalsMethod = null;
        Method hashCodeMethod = null;

        try {
            Class<?> c = Object.class;
            toStringMethod = c.getMethod("toString", (Class[]) null);
            equalsMethod = c.getMethod("equals", new Class[] { Object.class });
            hashCodeMethod = c.getMethod("hashCode", (Class[]) null);
        } catch (Throwable e) {
            FFDCFilter.processException(e, CLASS_NAME + ".<cinit>", "115");
        }

        // Save results in static class variables, which should be non-null
        // references provided
        // no exception has occurred.
        cvToStringMethod = toStringMethod;
        cvEqualsMethod = equalsMethod;
        cvHashCodeMethod = hashCodeMethod;
    }

    /**
     * The following static variables are used to indicate to the checkState
     * which Method this handler was invoked with.
     */

    // BEFORE_DELIVERY_METHOD indicates MessageEndpoint.beforeDelivery was
    // invoked by the caller.
    private final static byte BEFORE_DELIVERY_METHOD = 0;

    // AFTER_DELIVERY_METHOD indicates MessageEndpoint.afterDelivery was
    // invoked by the caller.
    private final static byte AFTER_DELIVERY_METHOD = 1;

    // RELEASE_METHOD indicates MessageEndpoint.release was invoked by the
    // caller.
    private final static byte RELEASE_METHOD = 2;

    /**
     * The following static variables are used to set the ivState instance
     * variable used to track what state this object is currently in.
     */

    // The RELEASED_STATE is the initial state and it indicates this object
    // is currently not in use by any resource adapter object. This state is
    // entered when this object is initially created and it is entered when
    // the MessageEndpoint.release method is called. If resource adapter
    // attempts to use this object while in this state, a IllegalStateException
    // is required to be thrown by the JCA spec.
    private final static short RELEASED_STATE = 0;

    // The READY state indicates this object is ready to handle invocation
    // and currently is not processing any prior invocation.
    private final static short READY_STATE = 1;

    // The IN_METHOD_OPTION_A_STATE indicates a MDB business method was
    // invoked while in the READY_STATE and this object is waiting for
    // MDB business method to return. In other words, OPTION A
    // message delivery option as defined in JCA 1.5 spec is being used.
    // A transition from the READY_STATE is the only valid transition
    // and a transition back to the READY_STATE is required once MDB business
    // method returns.
    private final static short IN_METHOD_OPTION_A_STATE = 2;

    // The BEFORE_DELIVERY_STATE indicates MessageEndpoint.delivery was invoked
    // while in the READY_STATE. In other words, OPTION B message delivery
    // option as defined in JCA 1.5 spec is being used. The next invocation
    // is required to be a MDB business method invocation, which will cause
    // this handler to transition to the IN_METHOD_OPTION_B_STATE.
    private final static short BEFORE_DELIVERY_STATE = 3;

    // The IN_METHOD_OPTION_B_STATE indicates a MDB business method was
    // invoked while in the BEFORE_DELIVERY_STATE. This object is waiting for
    // return from the MDB business method.
    private final static short IN_METHOD_OPTION_B_STATE = 4;

    // The AFTER_DELIVERY_PENDING_STATE indicates a MDB business method returned
    // to this handler object while in the IN_METHOD_OPTION_B_STATE. This state
    // is
    // used to enforce that afterDelivery must be the next invocation on this
    // handler.
    // When afterDelivery occurs, a transition back to the READY_STATE will
    // occur, which
    // indicates the handler is ready to process the next message delivery from
    // the resource adapter.
    private final static short AFTER_DELIVERY_PENDING_STATE = 5;

    // Discard state is only used if this class detects RA does
    // not comply with JCA 1.5 specification. This ensures this
    // instance will not be returned to the free pool for reuse of
    // future message deliveries.
    private final static short DISCARDED_STATE = Short.MAX_VALUE;

    /**
     * Must be set by using one of the above static variables for tracking the
     * state of this object.
     */
    private short ivState = RELEASED_STATE;

    /**
     * MessageEndpointFactory object. This object should be returned to when
     * release method is called. This is an immutable attribute of this object
     * (set when object is created and does not change for the life of this
     * object).
     */
    private BaseMessageEndpointFactory ivMessageEndpointFactory;

    public BaseMessageEndpointFactory getBaseMessageEndpointFactory() {
        return ivMessageEndpointFactory;
    }

    /**
     * Recovery ID to use whenever this object enlists a XAResource object with
     * the transaction service. This is an immutable attribute of this object
     * (set when object is created and does not change for the life of this
     * object).
     */
    private final int ivRecoveryId;

    /**
     * Set to true if ivXAResource is an instanceof RecoverableXAResource.
     */
    private boolean ivRecoverableXAResource;

    /**
     * The method being invoked on the proxy
     */
    private Method ivMethod = null;

    /**
     * Optional XAResource to be enlisted in the transaction. The
     * MessageEndpointFactory object will call the setXAResource method during
     * processing of a MessageEndpointFactory.createEndpoint invocation to
     * reinitialize this
     */
    private XAResource ivXAResource = null;

    /**
     * Proxy associated with this InvocationHandler. Must be set when this
     * object is created and remain set to the proxy instance until the discard
     * method is called. The discard instance is called whenever this object is
     * discarded by the PoolManager. At that time, discard method should null
     * out ivProxy.
     */
    Object ivProxy = null;

    /**
     * Thread instance currently using this MessageEndpoint proxy.
     */
    private Thread ivThread = null;

    /**
     * Set to true if release should discard the instance rather than return
     * instance to the pool for reuse. Usually this is a result of
     * IllegalStateException being thrown as a result of RA not complying with
     * JCA 1.5 spec.
     */
    private boolean ivDiscardRequired = false;

    /**
     * The JCA major version of the resource adapter that is using this object.
     * This allows this code to continue the old behavior of a given version if
     * a newer JCA version requires a difference in behavior from the prior JCA
     * version (e.g. beforeDeliver/afterDelivery behavior change).
     */
    private int majorJCAVersion;

    /**
     * The JCA minor version of the resource adapter that is using this object.
     * This allows this code to continue the old behavior of a given version if
     * a newer JCA version requires a difference in behavior from the prior JCA
     * version (e.g. beforeDeliver/afterDelivery behavior change).
     */
    private int minorJCAVersion;

    /**
     * Set to true if and only if afterDeliver processing is required to invoke
     * setRollbackOnly prior to calling container.postInvoke to force rollback
     * of the TX.
     */
    private boolean ivRollbackOnly; // f743-7046

    /**
     * Set to true if and only if beforeDelivery is called with an imported
     * global TX. afterDelivery must reset to false.
     */
    private boolean ivImportedTx; // f743-7046

    /**
     * Set to true to enable usage of RRS Transaction for transacted delivery.
     */
    private final boolean ivRRSTransactional;

    /**
     * Websphere TransactionManager.
     */
    private EmbeddableWebSphereTransactionManager ivTransactionManager = null;

    /**
     * CTOR
     * 
     * @param factory
     *            the MessageEndpointFactory that creates this handler.
     * @param recoveryId
     * @param rrsTransactional
     */
    public MessageEndpointHandler(BaseMessageEndpointFactory factory, int recoveryId, boolean rrsTransactional) {

        // Initialize this objects instance variables.
        ivRecoveryId = recoveryId;
        ivMessageEndpointFactory = factory;
        ivTransactionManager = EmbeddableTransactionManagerFactory.getTransactionManager();
        ivRRSTransactional = rrsTransactional;
    }

    /**
     * Processes a method invocation on a proxy instance and returns the result.
     * This method will be invoked on an invocation handler when a method is
     * invoked on a proxy instance that it is associated with.
     * 
     * @param proxy
     *            - the proxy instance that the method was invoked on.
     * 
     * @param method
     *            - the Method instance corresponding to the interface method
     *            invoked on the proxy. The declaring class of the Method object
     *            will be the interface that the method was declared in, which
     *            may be a superinterface of the proxy interface that the proxy
     *            class inherits the method through.
     * 
     * @param args
     *            -an array of objects containing the values of the arguments
     *            passed in the method invocation on the proxy instance, or null
     *            if interface method takes no arguments. Arguments of primitive
     *            types are wrapped in instances of the appropriate primitive
     *            wrapper class, such as java.lang.Integer or java.lang.Boolean.
     * 
     * @return the value to return from the method invocation on the proxy
     *         instance. If the declared return type of the interface method is
     *         a primitive type, then the value returned by this method must be
     *         an instance of the corresponding primitive wrapper class;
     *         otherwise, it must be a type assignable to the declared return
     *         type. If the value returned by this method is null and the
     *         interface method's return type is primitive, then a
     *         NullPointerException will be thrown by the method invocation on
     *         the proxy instance. If the value returned by this method is
     *         otherwise not compatible with the interface method's declared
     *         return type as described above, a ClassCastException will be
     *         thrown by the method invocation on the proxy instance.
     * 
     * @throws Throwable
     *             - the exception to throw from the method invocation on the
     *             proxy instance. The exception's type must be assignable
     *             either to any of the exception types declared in the throws
     *             clause of the interface method or to the unchecked exception
     *             types java.lang.RuntimeException or java.lang.Error. If a
     *             checked exception is thrown by this method that is not
     *             assignable to any of the exception types declared in the
     *             throws clause of the interface method, then an
     *             UndeclaredThrowableException containing the exception that
     *             was thrown by this method will be thrown by the method
     *             invocation on the proxy instance.
     */
    @Override
    @Trivial
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Class<?> declaringClass = method.getDeclaringClass();
        Object result = null;

        if (declaringClass == MessageEndpoint.class) {
            invokeMessageEndpointMethod(method, args);
        } else if (declaringClass == Object.class) {
            result = invokeObjectClassMethod(proxy, method, args);
        } else {
            invokeJMSMethod(method, args);
        }
        return result;
    }

    /**
     * Create instance of batch jms listener and invoke its onMesssage
     * 
     * @param methodToInvoke
     *            the method to invoke
     * @param args
     *            the arguments to pass on the method call
     */
    protected void invokeJMSMethod(Method methodToInvoke, Object[] args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        BatchJmsEndpointListener batchEndpointListener = new BatchJmsEndpointListener(ivMessageEndpointFactory.getConnectionFactory(), 
        																			  ivMessageEndpointFactory.getBatchExecutor().getBatchOperationGroup(), 
        																			  ivMessageEndpointFactory.getBatchExecutor().getWSJobRepository());
        methodToInvoke.invoke(batchEndpointListener, args);
    }

    private static final ThreadContextAccessor threadContextAccessor = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());
    private  Object origCL =null;
    @Override
    public void afterDelivery() throws ResourceException {
        threadContextAccessor.popContextClassLoader(origCL);
    }

    /**
     * Resource adapter calls this method prior to invoking a method message
     * endpoint to process message that is delivered.
     * 
     * @param method
     *            - description of a target method. This information about the
     *            intended target method allows an application server to decide
     *            whether to start a transaction during this method call,
     *            depending on the transaction preferences of the target method.
     */
    @Override
    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {
        
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "beforeDelivery push class loader=" + this.getClass().getClassLoader());
            }
            origCL = threadContextAccessor.pushContextClassLoader( this.getClass().getClassLoader());
            // Determine if beforeDelivery was called with an imported TX
            // context.
            // We need to remember this fact in case afterDelivery is called
            // without
            // ever calling a MDB method so that afterDelivery knows whether to
            // abort the TX or not.
            UOWCurrent uowCurrent = EmbeddableTransactionManagerFactory.getUOWCurrent();
            UOWCoordinator inBoundCoord = uowCurrent.getUOWCoord();
            if (inBoundCoord == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "beforeDelivery called without a imported transaction context");
                }
                ivImportedTx = false;
            } else {
                if (inBoundCoord.isGlobal()) // d174148
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "beforeDelivery called with a imported transaction context");
                    }
                    ivImportedTx = true;
                } else {
                    // We have a local transaction, so it can not be a imported
                    // transaction.
                    inBoundCoord = null;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "beforeDelivery called without a imported transaction context");
                    }
                    ivImportedTx = false;
                }
            }

            // If we have an XAResource and transaction is not an imported
            // transaction,
            // then enlist the XAResource if preInvoke started a global
            // transaction.
            if ((ivXAResource != null || ivRRSTransactional) && ivImportedTx == false) {
                UOWCoordinator coord = uowCurrent.getUOWCoord();
                if (coord.isGlobal()) {
                    int recoveryId;
                    if (ivRecoverableXAResource) {
                        recoveryId = RecoverableXAResourceAccessor.getXARecoveryToken(ivXAResource);
                    } else {
                        recoveryId = ivRecoveryId;
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "enlisting XAResource from RA, recovery ID is " + recoveryId);
                    }
                    if (!ivRRSTransactional) {
                        ivTransactionManager.enlist(ivXAResource, recoveryId);
                    } else {
                        ivTransactionManager.enlist(getNativeRRSXAR(coord), recoveryId);
                    }
                }
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, CLASS_NAME + ".beforeDelivery", "1244", this);
            throw new ResourceException("beforeDelivery failure", t);
        }

    }

    @Override
    public void release() {
        ivProxy = null;
    }

    /**
     * Process invocation of a MessageEndpoint interface method.
     */
    private void invokeMessageEndpointMethod(Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if (name.startsWith("b")) {
            checkState(method, args, BEFORE_DELIVERY_METHOD);
            Method m = (Method) args[0];
            beforeDelivery(m);
        } else if (name.startsWith("a")) {
            checkState(method, args, AFTER_DELIVERY_METHOD);
            afterDelivery();
        } else {
            checkState(method, args, RELEASE_METHOD);
            release();
        }
    }

    /**
     * Process invocation of a method inherited from java.lang.Object. Note,
     * only the non final methods of java.lang.Object need to be implemented.
     * The clone method does not need to be implemented since we are not
     * implementing the Clonable interface.
     * 
     * @throws Exception
     */
    @Trivial
    private Object invokeObjectClassMethod(Object proxy, Method method, Object[] args) throws Exception {
        if (method.equals(cvToStringMethod)) {
            return proxyToString(proxy);
        } else if (method.equals(cvEqualsMethod)) {
            return proxyEquals(proxy, args[0]);
        } else if (method.equals(cvHashCodeMethod)) {
            return proxyHashCode(proxy);
        } else {
            throw new Exception("Internal error, unexpected Object method dispatched: " + method);
        }
    }

    /**
     * Perform toString function on the proxy object.
     * 
     * TODO: Fix this: when trace in this file is turn on, this method will
     * eventually throw StackOverflow exception.
     */
    @Trivial
    private String proxyToString(Object proxy) {
        String retStr = ivMessageEndpointFactory.getJ2EEName() + "($Proxy@" + Integer.toHexString(proxy.hashCode()) + ")";
        return retStr;
    }

    /**
     * Perform equals function on the proxy object.
     */
    private Boolean proxyEquals(Object proxy, Object object) {
        return (proxy == object ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Perform the hashCode function on the Proxy object.
     */
    @Trivial
    private Integer proxyHashCode(Object proxy) {
        return Integer.valueOf(System.identityHashCode(proxy));
    }

    /**
     * Checks current state to determine if resource adapter has violated any
     * protocols defined by the by the JCA 1.5 specification. If a violation is
     * detected, an IllegalStateException is thrown. Here is a list of
     * assertions that are checked by this method:
     * <dl>
     * <dt>Assertion 1
     * <dd>
     * Section 12.5 - Any attempted use of the proxy endpoint (after its release
     * method is called) must result in a java.lang.IllegalStateException.
     * <dt>Assertion 2
     * <dd>
     * Section 12.5.1.1 - A resource adapter must not attempt to deliver
     * messages concurrently to a single endpoint instance.
     * <dt>Assertion 3
     * <dd>
     * Section 12.5.1.1 - The application server must reject concurrent usage of
     * an endpoint instance.
     * <dt>Assertion 4
     * <dd>
     * Section 12.5.6 - Not explicitly stated, but the implication of this
     * section is the Method object passed to the beforeDelivery method is the
     * Method that the resource adapter intends to invoke for message delivery.
     * <dt>Assertion 5
     * <dd>
     * Section 12.5.6 - For each message delivery to an endpoint instance, the
     * application server must match an afterDelivery call with a corresponding
     * beforeDelivery call; that is, beforeDelivery and afterDelivery calls are
     * treated as pairs. The release method call on a proxy endpoint instance
     * releases the state of the proxy instance and makes it available for
     * reuse. If the release method is called while a message delivery is
     * in-progress, the application server must throw a
     * java.lang.IllegalStateException, since concurrent calls on a proxy
     * endpoint instance is disallowed. In the case of option B, if the release
     * method is called inbetween beforeDelivery and afterDelivery method calls,
     * any transaction started during the corresponding beforeDelivery method
     * call must be aborted by the application server. Since this section
     * explicitly states the there must be a corresponding afterDelivery call
     * for each beforeDelivery call and multiple threads are not allowed to use
     * same MessageEndpoint instance concurrently, the assumption here is
     * IllegalStateException must still occur when release is called inbetween
     * beforeDelivery and afterDelivery and abortion of transaction should only
     * occur when called from the correct thread.
     * <dt>Assertion 6
     * <dd>
     * Section 12.5.6 - There must not be more than one message delivery per
     * pair of beforeDelivery/afterDelivery invocation.
     * <dt>Assertion 7
     * <dd>
     * Section 12.5.6 - For a single message delivery, the beforeDelivery, MDB
     * method invocation, and afterDelivery method calls must all occur from a
     * single thread of control.
     * </dl>
     * 
     * @param method
     *            is the Method object passed to the invoke method of this
     *            object.
     * 
     * @param args
     *            is the array of arguments passed to the invoke method of this
     *            object.
     * 
     * @param methodId
     *            is one of the constants defined in this class that indicates
     *            which method this invocation handler was invoked with.
     * 
     * @exception IllegalStateException
     *                is thrown if one of the assertions fails.
     */
    synchronized private void checkState(Method method, Object[] args, byte methodId) {
        // Assertion 3 and 7 checks.
        Thread thread = Thread.currentThread(); // d185161
        if (ivThread != null && ivThread != thread) {
            ivDiscardRequired = true;
            // Create an exception to nest in the ISE which will contain stack
            // trace of the conflicting thread
            Throwable t = new Exception("Conflicting with thread " + ivThread.getId() + ": " + ivThread.getName());
            t.setStackTrace(ivThread.getStackTrace());
            throwIllegalStateException("Multiple threads can not use same MessageEndpoint proxy instance concurrently", t);
        }

        // Okay, now we know the calling proxy is currently associated with this
        // object.
        // Determine if some other violation of JCA 1.5 spec has occurred. If
        // so,
        // throw IllegalStateException. Otherwise, make the appropriate state
        // change.
        switch (ivState) {
        case (READY_STATE):
            if (methodId == BEFORE_DELIVERY_METHOD) {
                // Resource adapter is using option B delivery.
                ivState = BEFORE_DELIVERY_STATE;
                ivMethod = (Method) args[0];

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "beforeDelivery: method is " + ivMethod);
                }
            } else if (methodId == RELEASE_METHOD) {
                // Resource adapter is releasing use of the endpoint proxy.
                ivState = RELEASED_STATE;
            } else {
                // Assertion 5 was violated since this state indicates all prior
                // beforeDelivery calls were already paired with a matching
                // afterDelivery call. So we have an unmatched afterDelivery
                // call.
                ivDiscardRequired = true;
                throwIllegalStateException("afterDelivery not paired with a prior beforeDelivery call"); // d185161
            }
            break;

        case (IN_METHOD_OPTION_A_STATE):
            // Assertion 2 and 3 was violated since this state indicates a prior
            // MDB method was invoked by this object and this object is waiting
            // for the MDB method to return.
            ivDiscardRequired = true;
            throwIllegalStateException("JCA requires resource adapter to ensure serial use of endpoint"); // d185161

        case (BEFORE_DELIVERY_STATE):
            if (methodId == AFTER_DELIVERY_METHOD) {
                // ivState = READY_STATE;

                // f743-7046 start
                // For JCA 1.5, continue to commit the TX if the RA invokes
                // beforeDelivery followed by afterDelivery without ever
                // invoking
                // a message listener interface method to deliver the message.
                // This ensures same behavior for JCA 1.5 RA that has always
                // occurred since WAS 6.0. For JCA 1.6 and later, set the
                // ivRollbackOnly
                // flag to force new behavior of aborting the TX by doing a
                // rollback.
                if (majorJCAVersion == 1 && minorJCAVersion == 5) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "MessageEndpoint.afterDelivery is committing TX for JCA version 1.5 RA. "
                                + "A message listener method was not called in between the before/afterDelivery methods.");
                    }
                    ivRollbackOnly = false;
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "MessageEndpoint.afterDelivery aborting TX as required by " + majorJCAVersion + "." + minorJCAVersion
                                + ".  A message listener method was not called in between the before/afterDelivery methods.");
                    }
                    // Only abort TX if it is NOT an imported TX.
                    ivRollbackOnly = (ivImportedTx == false);
                }
                // f743-7046 end
            } else if (methodId == BEFORE_DELIVERY_METHOD) {
                // Assertion 5 violation - 2 consecutive beforeDelivery calls.
                ivDiscardRequired = true;
                throwIllegalStateException("JCA requires resource adapter to call afterDelivery before the beforeDelivery can be called again on this endpoint"); // d185161
            } else {
                // d192893
                // Assertion 5 - release called after beforeDelivery in Option B
                // requires the
                // application server to abort transaction started by
                // beforeDelivery method.
                // So do not throw IllegalStateException at this time. Instead,
                // allow release
                // method to be invoked so that it aborts the transaction. Note,
                // since the
                // start of this method ensured release is called from the same
                // thread that
                // called beforeDelivery, we know it is okay to abort the
                // transaction.
                ivState = RELEASED_STATE;
            }
            break;

        case (IN_METHOD_OPTION_B_STATE):
            if (methodId == AFTER_DELIVERY_METHOD) {
                ivDiscardRequired = true;
                throwIllegalStateException("beforeDelivery called twice without afterDelivery between calls");
            } else if (methodId == BEFORE_DELIVERY_METHOD) {
                ivDiscardRequired = true;
                throwIllegalStateException("beforeDelivery called twice without afterDelivery between calls"); // d185161
            } else {
                ivDiscardRequired = true;
                throwIllegalStateException("release called without a prior afterDelivery call"); // d174179
            }

        case (AFTER_DELIVERY_PENDING_STATE):
            if (methodId == AFTER_DELIVERY_METHOD) {
                // Let afterDelivery method complete transaction.
            }
            if (methodId == BEFORE_DELIVERY_METHOD) {
                // Assertion 5 violation - beforeDelivery/afterDelivery must
                // come in pairs.
                ivDiscardRequired = true;
                throwIllegalStateException("JCA requires resource adapter to call afterDelivery before another beforeDelivery call can be made."); // d185161
            } else {
                // d192893
                // Assertion 5 - release called after beforeDelivery in Option B
                // requires the
                // application server to abort transaction started by
                // beforeDelivery method.
                // So do not throw IllegalStateException at this time. Instead,
                // allow release
                // method to be invoked so that it aborts the transaction. Note,
                // since the
                // start of this method ensured release is called from the same
                // thread that
                // called beforeDelivery, we know it is okay to abort the
                // transaction.
                ivState = RELEASED_STATE;
            }
            break;

        case (RELEASED_STATE):
        case (DISCARDED_STATE):
        default:
            // Assertion 1 violation.
            ivDiscardRequired = true;
            throwIllegalStateException("JCA requires resource adapter not to make any calls on endpoint once release is called on endpoint");

        }
        // If IllegalStateException did not occur, remember
        // what Thread instance is currently using this proxy.
        ivThread = thread;
    }

    /**
     * Initialize this object for option A message delivery. The
     * MessageEndpoinFactory that created this instance must call this method
     * during its createEndpoint processing to make the Proxy and XAResource
     * object known to this object.
     * <p>
     * <dl>
     * <dt>
     * <b>pre-condition</b>
     * <dd>
     * Must be called immediately after creating a new instance or after this
     * object is obtained from the Pool for reuse.
     * <dd>
     * Caller must ensure ivProxy is set to the MessageEndpoint proxy object
     * whenever it creates a new instance of a MessageEndpointHandler object.
     * <dt>
     * <b>post-condition</b>
     * <dd>
     * Ready for option A message delivery.
     * </dl>
     * 
     * @param xaResource
     *            is the XAResource object to enlist in the transaction when
     *            message delivery occurs. If a null reference is passed, then
     *            no XAResource object is enlisted.
     * 
     * @param recoverableXAResource
     *            must be set to true if and only if xaResource is an instanceof
     *            RecoverableXAResource.
     * 
     * @param jcaVersion
     *            is the JCA version of the RA that is using the MessageEndpoint
     *            object.
     * 
     * @throws IllegalStateException
     *             if pre-condition is violated.
     */
    synchronized public void initialize(XAResource xaResource, boolean recoverableXAResource, int majorJCAVer, int minorJCAVer) // f743-7046
    {
        if (ivState != RELEASED_STATE) {
            ivDiscardRequired = true;
            throwIllegalStateException("MessageEndpoint proxy used after MessageEndpoint.release was called." + " Internal state = " + ivState); // d185161
        }

        ivState = READY_STATE;
        ivXAResource = xaResource;
        ivRecoverableXAResource = recoverableXAResource;

        majorJCAVersion = majorJCAVer;
        minorJCAVersion = minorJCAVer;
    }

    /**
     * In this class there are many places where the following code is
     * performed: throw new IllegalStateException(<some message>)
     * 
     * @param msg
     *            A message which explains why the IllegalStateException is
     *            being thrown (likely due to a JCA 1.5 spec violation
     */
    private void throwIllegalStateException(String msg) {
        throwIllegalStateException(msg, null);
    }

    private void throwIllegalStateException(String msg, Throwable t) {
        throw new IllegalStateException(msg, t);
    }

    /**
     * This method is used for extending the transaction enlistment logic for
     * the lightweight server. It can be overridden to return a native RRS
     * XAResource
     * 
     * @param coord
     *            - The UOWCoordinator.
     * @throws XAResourceNotAvailableException
     */
    protected XAResource getNativeRRSXAR(UOWCoordinator coord) throws XAResourceNotAvailableException {
        return null;
    }
}

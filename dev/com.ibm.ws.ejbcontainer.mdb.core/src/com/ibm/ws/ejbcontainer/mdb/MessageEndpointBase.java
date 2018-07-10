/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Map;

import javax.ejb.EJBException;
import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.ContainerTx;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSDeployedSupport;
import com.ibm.ejs.container.EJSWrapperBase;
import com.ibm.ejs.container.MessageEndpointCollaborator;
import com.ibm.ejs.container.WrapperInterface;
import com.ibm.ejs.container.WrapperManager;
import com.ibm.ejs.container.util.MethodAttribUtils;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.tx.embeddable.RecoverableXAResourceAccessor;

/**
 * This class is a com.ibm.ejs.container.MDBInvocationHandler that is used when
 * invoking a method on a JCA 1.5 MessageEndpoint proxy. A JCA 1.5
 * MessageEndpoint proxy must implement both the javax.resource.spi.MessageEndpoint
 * interface as well as the message listener interface (as specified by messaging type
 * in the deployment descriptors for a MDB). Therefore, this handler must be able to
 * handle both invocations for the MessageEndpoint interface and the message listener
 * interface and perform whatever actions is needed prior to invoking method
 * on the real object (e.g the MDB method).
 * <p>
 * To fit into the current EJB container design and to use existing EJB container
 * internal interfaces, this class must also extend the com.ibm.ejs.container.EJSWrapperBase
 * class. Essentially, external to the ejb container this object is a MessageEndpoint
 * InvocationHandler and internally to EJB container this object is a EJSWrapperBase object.
 * for a MessageDrivenBean object.
 * <p>
 * Note: unlike most container wrapper objects, this wrapper will be managed by the
 * MessageEndpointFactoryImpl object rather than by the WrapperManager.
 */

public class MessageEndpointBase extends EJSWrapperBase implements MessageEndpoint {

    private static final String CLASS_NAME = MessageEndpointBase.class.getName();
    private static TraceComponent tc = Tr.register(MessageEndpointBase.class,
                                                   "EJBContainer", "com.ibm.ejs.container.container");

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

    // RELEASE_METHOD indicates MessageEndpoint.release was invoked by the caller.
    private final static byte RELEASE_METHOD = 2;

    // MDB_BUSINESS_METHOD indicates MDB business method was invoked by the caller.
    private final static byte MDB_BUSINESS_METHOD = 3;

    /**
     * The following static variables are used to set the ivState instance
     * variable used to track what state this object is currently in.
     */

    // The RELEASED_STATE is the initial state and it indicates this object
    // is currently not in use by any resource adapter object.  This state is
    // entered when this object is initially created and it is entered when
    // the MessageEndpoint.release method is called.  If resource adapter
    // attempts to use this object while in this state, a IllegalStateException
    // is required to be thrown by the JCA spec.
    private final static short RELEASED_STATE = 0;

    // The READY state indicates this object is ready to handle invocation
    // and currently is not processing any prior invocation.
    private final static short READY_STATE = 1;

    // The IN_METHOD_OPTION_A_STATE indicates a MDB business method was
    // invoked while in the READY_STATE and this object is waiting for
    // MDB business method to return.  In other words, OPTION A
    // message delivery option as defined in JCA 1.5 spec is being used.
    // A transition from the READY_STATE is the only valid transition
    // and a transition back to the READY_STATE is required once MDB business
    // method returns.
    private final static short IN_METHOD_OPTION_A_STATE = 2;

    // The BEFORE_DELIVERY_STATE indicates MessageEndpoint.delivery was invoked
    // while in the READY_STATE.  In other words, OPTION B message delivery
    // option as defined in JCA 1.5 spec is being used.  The next invocation
    // is required to be a MDB business method invocation, which will cause
    // this handler to transition to the IN_METHOD_OPTION_B_STATE.
    private final static short BEFORE_DELIVERY_STATE = 3;

    // The IN_METHOD_OPTION_B_STATE indicates a MDB business method was
    // invoked while in the BEFORE_DELIVERY_STATE.  This object is waiting for
    // return from the MDB business method.
    private final static short IN_METHOD_OPTION_B_STATE = 4;

    // The AFTER_DELIVERY_PENDING_STATE indicates a MDB business method returned
    // to this handler object while in the IN_METHOD_OPTION_B_STATE. This state is
    // used to enforce that afterDelivery must be the next invocation on this handler.
    // When afterDelivery occurs, a transition back to the READY_STATE will occur, which
    // indicates the handler is ready to process the next message delivery from
    // the resource adapter.
    private final static short AFTER_DELIVERY_PENDING_STATE = 5;

    // Discard state is only used if this class detects RA does
    // not comply with JCA 1.5 specification.  This ensures this
    // instance will not be returned to the free pool for reuse of
    // future message deliveries.
    private final static short DISCARDED_STATE = Short.MAX_VALUE;

    /**
     * Must be set by using one of the above static variables for tracking the
     * state of this object.
     */
    private short ivState = RELEASED_STATE;

    /**
     * MessageEndpointFactory object this object should be returned to
     * when release method is called. This is an immutable attribute of this
     * object (set when object is created and does not change for the life
     * of this object).
     */
    private BaseMessageEndpointFactory ivMessageEndpointFactory;

    /**
     * Recovery ID to use whenever this object enlists a XAResource object with
     * the transaction service. This is an immutable attribute of this
     * object (set when object is created and does not change for the life
     * of this object).
     */
    private int ivRecoveryId;

    /**
     * Set to true if ivXAResource is an instanceof RecoverableXAResource.
     */
    private boolean ivRecoverableXAResource;

    /**
     * The MDB instance associated with this endpoint. It is not known until
     * container calls the setMDB method on this object during the activation
     * of the MDB.
     */
    private Method ivMethod = null;

    /**
     * The MDB instance associated with this endpoint. It is not known until
     * container calls the setMDB method on this object during the activation
     * of the MDB.
     */
    private Object ivMDB = null; // d414873

    /**
     * Optional XAResource to be enlisted in the transaction. The
     * MessageEndpointFactory object will call the setXAResource method
     * during processing of a MessageEndpointFactory.createEndpoint invocation
     * to reinitialize this
     */
    private XAResource ivXAResource = null;

    /**
     * Proxy associated with this InvocationHandler. Must be set
     * when this object is created and remain set to the proxy
     * instance until the discard method is called. The discard
     * instance is called whenever this object is discarded by the
     * PoolManager. At that time, discard method should null out
     * ivProxy.
     */
    Object ivProxy = null;

    /**
     * The method ID to use for current Method being
     * invoked or about to be invoked. Note, a method ID of zero
     * is used for the onMessage method of a JMS message listener interface.
     */
    private int ivMethodId = 0; //d219252

    /**
     * Thread instance currently using this MessageEndpoint proxy.
     */
    private Thread ivThread = null;

    /**
     * Set to true if release should discard the instance rather
     * than return instance to the pool for reuse. Usually this
     * is a result of IllegalStateException being thrown as a
     * result of RA not complying with JCA 1.5 spec.
     */
    private boolean ivDiscardRequired = false;

    /**
     * EJSDeployedSupport to use for current Method being
     * invoked or about to be invoked.
     */
    public EJSDeployedSupport ivEJSDeployedSupport;

    /**
     * Websphere TransactionManager.
     */
    private EmbeddableWebSphereTransactionManager ivTransactionManager = null;

    /**
     * Set to true if and only if there is only 1 method in the
     * message listener interface. This is used to avoid searching
     * EJBMethodInfo array when we know there is only 1 entry in the array.
     * Note, this optimization is for any message listener interface that
     * has only 1 method, not just the JMS interface.
     */
    private boolean ivSingleMethodInterface = false; //d456256

    /**
     * The JCA major version of the resource adapter that is using this object.
     * This allows this code to continue the old behavior of a given
     * version if a newer JCA version requires a difference in behavior
     * from the prior JCA version (e.g. beforeDeliver/afterDelivery behavior change).
     */
    private int majorJCAVersion;

    /**
     * The JCA minor version of the resource adapter that is using this object.
     * This allows this code to continue the old behavior of a given
     * version if a newer JCA version requires a difference in behavior
     * from the prior JCA version (e.g. beforeDeliver/afterDelivery behavior change).
     */
    private int minorJCAVersion;

    /**
     * Set to true if and only if afterDeliver processing is required
     * to invoke setRollbackOnly prior to calling container.postInvoke to
     * force rollback of the TX.
     */
    private boolean ivRollbackOnly; // f743-7046

    /**
     * Set to true if and only if beforeDelivery is called with an
     * imported global TX. afterDelivery must reset to false.
     */
    private boolean ivImportedTx; // f743-7046

    /**
     * Set to true to enable usage of RRS Transaction for transacted delivery.
     */
    private boolean ivRRSTransactional;

    /**
     * Set to true whenever release() directly calls afterDelivery().
     * This is to preserve the original behavior of when this class
     * used a java reflection proxy and checkState() was located in
     * the proxy invoke() method logic. Now checkState() is directly
     * inside the afterDelivery() method, and it triggers different behavior
     * than when the checkState() was only called with outside invokes of
     * the proxy.
     */
    private boolean ivSkipCheckState;

    /**
     * Setup an instance of MessageEndpointBase that can be used as a
     * base for a MessageEndpoint proxy object.
     * 
     * @param factory - the MessageEndpointFactory object that owns this object.
     * @param recoveryId - the id to use when enlisting an XAResource with TransactionManager.
     * @param container - the EJB container for the MDB.
     * @param beanMetaData - the meta data for the MDB.
     * @param pmiBean - the PMI collaborator for the MDB.
     * @param wrapperManager - the wrapper manager for this wrapper.
     * @param rrsTransactional must be true for RRS support.
     */

    //add endpoint variable and add endpoint.throughout....
    public static void construct(BaseMessageEndpointFactory factory,
                                 MessageEndpointBase messageEndpointBase,
                                 int recoveryId,
                                 EJSContainer container,
                                 BeanMetaData beanMetaData,
                                 EJBPMICollaborator pmiBean,
                                 WrapperManager wrapperManager,
                                 boolean rrsTransactional) //d219252x
    {
        // Initialize superclass with the wrapper information.
        messageEndpointBase.container = container;
        messageEndpointBase.wrapperManager = wrapperManager;
        messageEndpointBase.beanId = new BeanId(factory, null, false);
        messageEndpointBase.bmd = beanMetaData;
        messageEndpointBase.ivPmiBean = pmiBean;
        messageEndpointBase.isolationAttrs = null;
        messageEndpointBase.ivCommon = null; // Not a cached wrapper.     d174057.2
        messageEndpointBase.isManagedWrapper = false; // Not a managed wrapper.    d174057.2
        messageEndpointBase.methodInfos = messageEndpointBase.bmd.localMethodInfos;
        messageEndpointBase.methodNames = messageEndpointBase.bmd.localMethodNames;
        messageEndpointBase.ivInterface = WrapperInterface.MESSAGE_LISTENER; // d366807

        // Initialize this objects instance variables.
        messageEndpointBase.ivRecoveryId = recoveryId;
        messageEndpointBase.ivMessageEndpointFactory = factory;
        messageEndpointBase.ivTransactionManager = EmbeddableTransactionManagerFactory.getTransactionManager();
        messageEndpointBase.ivRRSTransactional = rrsTransactional;

        //d456256 start
        // If there is only 1 method in the EJBMethodInfo for the
        // message listener interface, then set ivSingleMethodInterface
        // to true.
        if (messageEndpointBase.methodInfos.length == 1)
        {
            messageEndpointBase.ivSingleMethodInterface = true;
        }
        // d456256 end
    }

    /**
     * Checks current state to determine if resource adapter has violated
     * any protocols defined by the by the JCA 1.5 specification. If a violation
     * is detected, an IllegalStateException is thrown. Here is a list of
     * assertions that are checked by this method:
     * <dl>
     * <dt>Assertion 1
     * <dd>
     * Section 12.5 - Any attempted use of the proxy endpoint (after its
     * release method is called) must result in a java.lang.IllegalStateException.
     * <dt>Assertion 2
     * <dd>
     * Section 12.5.1.1 - A resource adapter must not attempt to deliver messages
     * concurrently to a single endpoint instance.
     * <dt>Assertion 3
     * <dd>
     * Section 12.5.1.1 - The application server must reject concurrent usage
     * of an endpoint instance.
     * <dt>Assertion 4
     * <dd>
     * Section 12.5.6 - Not explicitly stated, but the implication of this section
     * is the Method object passed to the beforeDelivery method is the Method that the
     * resource adapter intends to invoke for message delivery.
     * <dt>Assertion 5
     * <dd>
     * Section 12.5.6 - For each message delivery to an endpoint instance, the
     * application server must match an afterDelivery call with a corresponding beforeDelivery
     * call; that is, beforeDelivery and afterDelivery calls are treated as pairs.
     * The release method call on a proxy endpoint instance releases the state of the
     * proxy instance and makes it available for reuse. If the release method is called
     * while a message delivery is in-progress, the application server must throw a
     * java.lang.IllegalStateException, since concurrent calls on a proxy endpoint
     * instance is disallowed. In the case of option B, if the release method is called
     * inbetween beforeDelivery and afterDelivery method calls, any transaction
     * started during the corresponding beforeDelivery method call must be aborted by
     * the application server. Since this section explicitly states the there must be
     * a corresponding afterDelivery call for each beforeDelivery call and multiple threads
     * are not allowed to use same MessageEndpoint instance concurrently, the assumption here
     * is IllegalStateException must still occur when release is called inbetween beforeDelivery
     * and afterDelivery and abortion of transaction should only occur when called from the
     * correct thread.
     * <dt>Assertion 6
     * <dd>
     * Section 12.5.6 - There must not be more than one message delivery per pair of
     * beforeDelivery/afterDelivery invocation.
     * <dt>Assertion 7
     * <dd>
     * Section 12.5.6 - For a single message delivery, the beforeDelivery, MDB method invocation,
     * and afterDelivery method calls must all occur from a single thread of control.
     * </dl>
     * 
     * @param methodId is the number used to lookup the EJBMethodInfoImpl used for comparison
     *            to the method stored during beforeDelivery().
     * 
     * @param beforeDeliveryMethod is the method passed to MessageEndpoint.beforeDelivery().
     * 
     * @param invokingMethod is one of the constants defined in this class that indicates
     *            which method this message endpoint was invoked with.
     * 
     * @exception IllegalStateException is thrown if one of the assertions fails.
     */
    synchronized public void checkState(int methodId, Method beforeDeliveryMethod, byte invokingMethod)
    {
        // Assertion 3 and 7 checks.
        Thread thread = Thread.currentThread(); // d185161
        if (ivThread != null && ivThread != thread)
        {
            ivDiscardRequired = true;
            // Create an exception to nest in the ISE which will contain stack trace of the conflicting thread
            final Throwable t = new Exception("Conflicting with thread " + ivThread.getId() + ": " + ivThread.getName());

            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    t.setStackTrace(ivThread.getStackTrace());
                    return null;
                }
            });

            throwIllegalStateException("Multiple threads can not use same MessageEndpoint proxy instance concurrently", t);
        }

        // Okay, now we know the calling proxy is currently associated with this object.
        // Determine if some other violation of JCA 1.5 spec has occurred.  If so,
        // throw IllegalStateException. Otherwise, make the appropriate state change.
        switch (ivState)
        {
            case (READY_STATE):
                if (invokingMethod == MDB_BUSINESS_METHOD)
                {
                    // Resource adapter is using option A delivery.
                    ivState = IN_METHOD_OPTION_A_STATE;
                }
                else if (invokingMethod == BEFORE_DELIVERY_METHOD)
                {
                    // Resource adapter is using option B delivery.
                    ivState = BEFORE_DELIVERY_STATE;
                    ivMethod = beforeDeliveryMethod;

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        Tr.debug(tc, "beforeDelivery: method is " + ivMethod);
                    }
                }
                else if (invokingMethod == RELEASE_METHOD)
                {
                    // Resource adapter is releasing use of the endpoint proxy.
                    ivState = RELEASED_STATE;
                }
                else
                {
                    // Assertion 5 was violated since this state indicates all prior
                    // beforeDelivery calls were already paired with a matching
                    // afterDelivery call. So we have an unmatched afterDelivery call.
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
                if (invokingMethod == MDB_BUSINESS_METHOD)
                {
                    // Get EJBMethodInfoImpl object for the MDB method being invoked.
                    EJBMethodInfoImpl methodInfo = super.methodInfos[methodId];

                    if (!(methodInfo.methodsMatch(ivMethod)))
                    {
                        // Assertion 4 violated.
                        ivDiscardRequired = true;
                        throwIllegalStateException("JCA requires resource adapter to invoke same method that was passed to beforeDelivery"); //d174179
                    }

                    ivState = IN_METHOD_OPTION_B_STATE;
                }
                else if (invokingMethod == AFTER_DELIVERY_METHOD)
                {
                    // ivState = READY_STATE;

                    // f743-7046 start
                    // For JCA 1.5, continue to commit the TX if the RA invokes
                    // beforeDelivery followed by afterDelivery without ever invoking
                    // a message listener interface method to deliver the message.
                    // This ensures same behavior for JCA 1.5 RA that has always
                    // occurred since WAS 6.0.  For JCA 1.6 and later, set the ivRollbackOnly
                    // flag to force new behavior of aborting the TX by doing a rollback.
                    if (majorJCAVersion == 1 && minorJCAVersion == 5) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "MessageEndpoint.afterDelivery is committing TX for JCA version 1.5 RA. "
                                         + "A message listener method was not called in between the before/afterDelivery methods.");
                        }
                        ivRollbackOnly = false;
                    }
                    else
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        {
                            Tr.debug(tc, "MessageEndpoint.afterDelivery aborting TX as required by " + majorJCAVersion + "." + minorJCAVersion
                                         + ".  A message listener method was not called in between the before/afterDelivery methods.");
                        }
                        // Only abort TX if it is NOT an imported TX.
                        ivRollbackOnly = (ivImportedTx == false);
                    }
                    // f743-7046 end
                }
                else if (invokingMethod == BEFORE_DELIVERY_METHOD)
                {
                    // Assertion 5 violation - 2 consecutive beforeDelivery calls.
                    ivDiscardRequired = true;
                    throwIllegalStateException("JCA requires resource adapter to call afterDelivery before the beforeDelivery can be called again on this endpoint"); // d185161
                }
                else
                {
                    // d192893
                    // Assertion 5 - release called after beforeDelivery in Option B requires the
                    // application server to abort transaction started by beforeDelivery method.
                    // So do not throw IllegalStateException at this time.  Instead, allow release
                    // method to be invoked so that it aborts the transaction.  Note, since the
                    // start of this method ensured release is called from the same thread that
                    // called beforeDelivery, we know it is okay to abort the transaction.
                    ivState = RELEASED_STATE;
                }
                break;

            case (IN_METHOD_OPTION_B_STATE):
                // Assertion 2 and 3 was violated since this state indicates a prior
                // MDB method was invoked by this object and this object is waiting
                // for the MDB method to return.
                if (invokingMethod == MDB_BUSINESS_METHOD)
                {
                    ivDiscardRequired = true;
                    throwIllegalStateException("beforeDelivery called twice without afterDelivery between calls"); //d174179
                }
                else if (invokingMethod == AFTER_DELIVERY_METHOD)
                {
                    ivDiscardRequired = true;
                    throwIllegalStateException("beforeDelivery called twice without afterDelivery between calls");
                }
                else if (invokingMethod == BEFORE_DELIVERY_METHOD)
                {
                    ivDiscardRequired = true;
                    throwIllegalStateException("beforeDelivery called twice without afterDelivery between calls"); // d185161
                }
                else
                {
                    ivDiscardRequired = true;
                    throwIllegalStateException("release called without a prior afterDelivery call"); //d174179
                }

            case (AFTER_DELIVERY_PENDING_STATE):
                if (invokingMethod == AFTER_DELIVERY_METHOD)
                {
                    // Let afterDelivery method complete transaction.
                }
                else if (invokingMethod == MDB_BUSINESS_METHOD)
                {
                    // Assertion 6 violation - only 1 message delivery allowed per
                    // pair of beforeDelivery/afterDelivery calls.
                    ivDiscardRequired = true;
                    throwIllegalStateException("JCA requires resource adapter to ensure 1 message delivery per pair of before/after delivery calls."); //174179
                }
                else if (invokingMethod == BEFORE_DELIVERY_METHOD)
                {
                    // Assertion 5 violation - beforeDelivery/afterDelivery must come in pairs.
                    ivDiscardRequired = true;
                    throwIllegalStateException("JCA requires resource adapter to call afterDelivery before another beforeDelivery call can be made."); // d185161
                }
                else
                {
                    // d192893
                    // Assertion 5 - release called after beforeDelivery in Option B requires the
                    // application server to abort transaction started by beforeDelivery method.
                    // So do not throw IllegalStateException at this time.  Instead, allow release
                    // method to be invoked so that it aborts the transaction.  Note, since the
                    // start of this method ensured release is called from the same thread that
                    // called beforeDelivery, we know it is okay to abort the transaction.
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

    //PK37051 start - This method and the function it performs is in no way related to the issue in this APAR,
    //however, I'm using this APAR to simply sneak in this method since it is merely a simple servicability addition.
    /**
     * In this class there are many places where the following code is performed:
     * throw new IllegalStateException(<some message>);
     * This code is performed so many times in fact that, rather than duplicating this code throughout this class,
     * it is worth adding this code to its own method. This method will not only throw the exception, but will also
     * print a debug statement indicating that the exception will be thrown, in order to provide better servicability.
     * This servicability is necessary so that we (ejb container) know the cause of the IllegalStateException, rather
     * than relying on the caller (typically a Resource Adapter implementation) to print the cause of the exception.
     * 
     * @param msg - A message which explains why the IllegalStateException is being thrown (likely due to a JCA 1.5
     *            spec violation
     */
    private void throwIllegalStateException(String msg) {
        throwIllegalStateException(msg, null);
    }

    private void throwIllegalStateException(String msg, Throwable t) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Going to throw an IllegalStateException with the following message: " + msg);

        throw new IllegalStateException(msg, t);
    }

    //PK37051 end

    /**
     * Get the method ID associated with a specified Method object.
     * The method ID can be used as a index into the array of
     * EJBMethodInfo objects (super.methodInfos).
     * 
     * @param method - the target of this request.
     * 
     * @return method ID for the target method or -1 if not found.
     */
    private int getEJBMethodId(Method method)
    {
        // Get target method signature.
        String targetSignature = MethodAttribUtils.methodSignature(method);

        // Search array of EJBMethodInfo object until the one that matches
        // target signature is found or all array elements are processed.
        EJBMethodInfoImpl minfo = null;
        int n = super.methodInfos.length;
        int methodId = -1;
        for (int i = 0; i < n; ++i)
        {
            minfo = super.methodInfos[i];
            if (targetSignature.equals(minfo.getMethodSignature()))
            {
                methodId = i;
                break;
            }
        }

        // Return methodId to caller (or -1 if not found).
        return methodId;
    }

    /**
     * Initialize this object for option A message delivery. The MessageEndpoinFactory
     * that created this instance must call this method during its createEndpoint
     * processing to make the Proxy and XAResource object known to this object.
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
     * @param xaResource is the XAResource object to enlist in the transaction
     *            when message delivery occurs. If a null reference is passed, then
     *            no XAResource object is enlisted.
     * 
     * @param recoverableXAResource must be set to true if and only
     *            if xaResource is an instanceof RecoverableXAResource.
     * 
     * @param jcaVersion is the JCA version of the RA that is using the MessageEndpoint object.
     * 
     * @throws IllegalStateException if pre-condition is violated.
     */
    public static void initialize(MessageEndpointBase proxy, XAResource xaResource, boolean recoverableXAResource, int majorJCAVer, int minorJCAVer) {// f743-7046
        synchronized (proxy) {
            if (proxy.ivState != RELEASED_STATE) {
                proxy.ivDiscardRequired = true;
                proxy.throwIllegalStateException("MessageEndpoint proxy used after MessageEndpoint.release was called." + " Internal state = " + proxy.ivState); // d185161
            }
            proxy.ivState = READY_STATE;
            proxy.ivXAResource = xaResource;
            proxy.ivRecoverableXAResource = recoverableXAResource;
            proxy.majorJCAVersion = majorJCAVer;
            proxy.minorJCAVersion = minorJCAVer;
        }
    }

    public Object mdbMethodPreInvoke(int methodId, Object[] args) throws RemoteException, SystemException, RollbackException, XAResourceNotAvailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "mdbMethodPreInvoke entering with methodId "
                         + methodId
                         + " and args[] of: \n"
                         + Arrays.toString(args));
        }
        ivMethodId = methodId;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            // Get EJBMethodInfoImpl object for the MDB method being invoked.
            // It contains the Method object of the MDB class to be invoked.
            EJBMethodInfoImpl methodInfo = super.methodInfos[ivMethodId];
            Method ivMethodToInvoke = methodInfo.getMethod();

            Tr.debug(tc, "preInvokeMdbMethod called for method "
                         + ivMethodToInvoke.getDeclaringClass().getName()
                         + "." + ivMethodToInvoke.getName());
        }

        try
        {
            // Check whether option A or option B message delivery to determine
            // what preinvoke processing is needed.
            if (ivEJSDeployedSupport != null) //LIDB2617.11
            {
                // Option B message delivery being used.  Complete preinvoke processing now that
                // we have the EJB method argument.
                container.preInvokeMdbAfterActivate(this, ivEJSDeployedSupport, ivMDB, args); //182011
            }
            else
            {
                // Option A message delivery being used.
                // If an XAResource was provided by the RA when createEndpoint was
                // called, then determine if there is an inbound transaction already
                // associated with calling thread.
                UOWCoordinator inBoundCoord = null;
                UOWCurrent uowCurrent = null;
                if (ivXAResource != null)
                {
                    uowCurrent = EmbeddableTransactionManagerFactory.getUOWCurrent();
                    inBoundCoord = uowCurrent.getUOWCoord();
                    if (inBoundCoord == null) //d174148
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        {
                            Tr.debug(tc, "invokeMdbMethod called without a imported transaction context");
                        }
                    }
                    else
                    {
                        if (inBoundCoord.isGlobal())
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            {
                                Tr.debug(tc, "invokeMdbMethod called with a imported transaction context");
                            }
                        }
                        else
                        {
                            // We have a local transaction, so it can not be a imported transaction.
                            inBoundCoord = null;
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            {
                                Tr.debug(tc, "invokeMdbMethod called without a imported transaction context");
                            }
                        }
                    }

                }

                // LIDB2617.11
                // Perform all of preinvoke processing for option A message delivery.
                ivEJSDeployedSupport = new EJSDeployedSupport();
                ivMDB = container.preInvokeMdbActivate(this, ivMethodId, ivEJSDeployedSupport);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "MDB class is " + ivMDB.getClass().getName());
                }

                container.preInvokeMdbAfterActivate(this, ivEJSDeployedSupport, ivMDB, args); //182011

                // If we have an XAResource and transaction is not a inbound transaction,
                // then enlist the XAResource if running in a global transaction.
                if (uowCurrent != null && (ivXAResource != null || ivRRSTransactional) && inBoundCoord == null) // d414873
                {
                    UOWCoordinator coord = uowCurrent.getUOWCoord();
                    if (coord.isGlobal())
                    {
                        int recoveryId;
                        if (ivRecoverableXAResource) //LI2110.68
                        {
                            recoveryId = RecoverableXAResourceAccessor.getXARecoveryToken(ivXAResource);
                        }
                        else
                        {
                            recoveryId = ivRecoveryId;
                        }

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) //d174179
                        {
                            Tr.debug(tc, "enlisting XAResource from RA, recovery ID is " + recoveryId);
                        }
                        if (!ivRRSTransactional) {
                            ivTransactionManager.enlist(ivXAResource, recoveryId);
                        } else {
                            XAResource xaResource = container.getEJBRuntime().getRRSXAResource(bmd, coord.getXid());
                            ivTransactionManager.enlist(xaResource, recoveryId);

                        }
                    }
                }
            }

            // Now invoke the target method on the MDB instance returned by preinvoke.
            // But first determine if we need to store some FVT test results and do
            // so if necessary.
            // Do not move this code. It should remain just prior to the invocation of the MDB method.
            container.getEJBRuntime().notifyMessageDelivered(this);
            if (ivPmiBean != null) {
                ivPmiBean.messageDelivered();
            }
        } catch (Throwable t)
        {
            if (ivEJSDeployedSupport != null) {
                // Get EJBMethodInfoImpl object for the MDB method being invoked.
                EJBMethodInfoImpl methodInfo = super.methodInfos[ivMethodId];
                container.preinvokeHandleException(t, this, ivMethodId, ivEJSDeployedSupport, methodInfo);
            }

            EJBException ejbex = new EJBException();
            ejbex.initCause(t);
            throw ejbex;
        }
        return ivMDB;
    }

    /**
     * Must be called when MDB method completes so that internal
     * state is updated to reflect the completion of MDB method invocation.
     * 
     * <dl>
     * <dt>pre-condition
     * <dd>
     * Invoked MDB method has returned.
     * <dd>
     * ivState == IN_METHOD_OPTION_A_STATE or ivState == IN_METHOD_OPTION_B_STATE
     * <dt>post-condition
     * <dd>
     * if ivState == IN_METHOD_OPTION_A_STATE, then ivState is changed to READY_STATE.
     * Otherwise, ivState is changed to AFTER_DELIVERY_PENDING_STATE.
     * </dl>
     */
    public void mdbMethodPostInvoke() throws Throwable
    {
        // If there is a registered message endpoint collaborator, call it for postInvoke processing.
        Map<String, Object> meContext = ivEJSDeployedSupport.getMessageEndpointContext();
        if (meContext != null) {
            MessageEndpointCollaborator meCollaborator = container.getEJBRuntime().getMessageEndpointCollaborator(this.bmd);
            if (meCollaborator != null) {
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invoking MECollaborator " + meCollaborator + " for postInvoke processing with the following context data: " + meContext);
                    }

                    meCollaborator.postInvoke(meContext);
                } finally {
                    ivEJSDeployedSupport.setMessageEndpointContext(null);
                }
            }
        }

        // Since checkState method in this class preceeded the call
        // to this method, we know we are running in the correct
        // thread and the correct Proxy instance is using this
        // MessageEndpointBase.  Therefore, we can safely examine
        // ivState outside of a synchronization block.   However,
        // to change the state, we do need to make change inside a
        // synchronization block.  This is necessary to ensure the
        // checkState method throws IllegalStateException if some other
        // thread tries to use this InvocationHandler instance while
        // this thread is using the instance.  This should never happen, but
        // JCA 1.5 requires us to throw IllegalStateException if it does happen.
        // If it does happen, then resource adapter does not comply with
        // JCA 1.5 specification (or there is a bug in this class).
        if (ivState == IN_METHOD_OPTION_B_STATE)
        {
            // Option B message delivery was used. So
            // change the state to afterDelivery is pending.
            // postInvoke processing is deferred until the
            // afterDelivery method is invoked by RA.
            // Necessary since we are required to leave
            // transaction active until afterDelivery occurs.
            synchronized (this)
            {
                ivState = AFTER_DELIVERY_PENDING_STATE;
            }
        }
        else
        {
            // OPTION A message delivery was used,
            // so do the postInvoke processing now and
            // enter the READY_STATE.
            try
            {
                if (ivEJSDeployedSupport != null)
                {
                    // Preinvoke did occur, so do post invoke processing.
                    container.postInvoke(this, ivMethodId, ivEJSDeployedSupport);
                }
            } catch (EJBException e)
            {
                //FFDCFilter.processException(e, CLASS_NAME + "mdbMethodPostInvoke", "589", this);
                throw e;
            } catch (Throwable e)
            {
                FFDCFilter.processException(e, CLASS_NAME + ".mdbMethodPostInvoke", "1106", this);
                if (ivEJSDeployedSupport != null)
                {
                    ivEJSDeployedSupport.setUncheckedLocalException(e);
                }

                // if we get this far, then setUncheckedLocalException
                // for some reason did not throw an exception.  If that
                // happens, we will throw an EJBException since that is
                // what EJB spec requires for MDB.
                EJBException ejbex = new EJBException();
                ejbex.initCause(e);
                throw e;
            } finally
            {
                // Release objects no longer needed.
                ivEJSDeployedSupport = null;
                ivMDB = null;

                // Option A message processing completed, so re-enter the ready state
                // to indicate we are ready to handle the next message.
                synchronized (this)
                {
                    ivState = READY_STATE;
                    ivThread = null;
                }
            }
        }
    }

    /**
     * Resource adapter calls this method prior to invoking
     * a method on the MDB to process message that is delivered.
     * 
     * @param method - description of a target method. This information
     *            about the intended target method allows an application server
     *            to decide whether to start a transaction during this method
     *            call, depending on the transaction preferences of the target method.
     */
    @Override
    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "MessageEndpoint.beforeDelivery called for " + method.getName());
        }

        //method param for checkState can be null since it's only used when the method is a MDB_BUSINESS_METHOD
        checkState(-1, method, BEFORE_DELIVERY_METHOD);

        try
        {
            // f743-7046 start of change.
            // Determine if beforeDelivery was called with an imported TX context.
            // We need to remember this fact in case afterDelivery is called without
            // ever calling a MDB method so that afterDelivery knows whether to
            // abort the TX or not.
            UOWCurrent uowCurrent = EmbeddableTransactionManagerFactory.getUOWCurrent();
            UOWCoordinator inBoundCoord = uowCurrent.getUOWCoord();
            if (inBoundCoord == null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "beforeDelivery called without a imported transaction context");
                }
                ivImportedTx = false;
            }
            else
            {
                if (inBoundCoord.isGlobal()) //d174148
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        Tr.debug(tc, "beforeDelivery called with a imported transaction context");
                    }
                    ivImportedTx = true;
                }
                else
                {
                    // We have a local transaction, so it can not be a imported transaction.
                    inBoundCoord = null;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        Tr.debug(tc, "beforeDelivery called without a imported transaction context");
                    }
                    ivImportedTx = false;
                }
            }
            // f743-7046 end of change.

            // Is this message listener interface that has only 1 method?
            if (ivSingleMethodInterface) //d456256
            {
                // Yep, then method ID is zero.
                ivMethodId = 0;
            }
            else
            {
                // Nope, more than 1 method, so search EJBMethodInfo to get method ID.
                ivMethodId = getEJBMethodId(method); //d219252
            }

            // Now have container start a transaction if necessary
            // and set context class loader.  This is done by performing
            // preinvoke processing for the Method that is going to
            // be invoked by the RA once beforeDelivery processing has
            // completed. So we need to set ivMethodId to the method ID
            // for the method that will be invoked.
            if (ivMethodId < 0)
            {
                throw new NoSuchMethodException(this.getClass().getName()
                                                + "." + method.getName()
                                                + " not found.");
            }
            else
            {

                // Note, the preinvoke returns the MDB instance
                // to use when the RA invokes a method on the
                // messaging type interface (MDB method).
                ivEJSDeployedSupport = new EJSDeployedSupport();
                try {
                    ivMDB = container.preInvokeMdbActivate(this, ivMethodId, ivEJSDeployedSupport); //LIDB2617.11
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        Tr.debug(tc, "preinvoke called for MDB method "
                                     + ivMDB.getClass().getName()
                                     + "." + method.getName());
                    }
                } catch (Throwable t)
                {
                    container.preinvokeHandleException(t, this, ivMethodId, ivEJSDeployedSupport, super.methodInfos[ivMethodId]);

                    // This will never happen since the setUncheckException should
                    // always throw an exception.  But we need this line to make
                    // the compiler happy (it does know exception always thrown).
                    //return null;
                }
            }

            // If we have an XAResource and transaction is not an imported transaction,
            // then enlist the XAResource if preInvoke started a global transaction.
            if ((ivXAResource != null || ivRRSTransactional) && ivImportedTx == false) // d414873  // f743-7046
            {
                UOWCoordinator coord = uowCurrent.getUOWCoord();
                if (coord.isGlobal())
                {
                    int recoveryId;
                    if (ivRecoverableXAResource) //LI2110.68
                    {
                        recoveryId = RecoverableXAResourceAccessor.getXARecoveryToken(ivXAResource);
                    }
                    else
                    {
                        recoveryId = ivRecoveryId;
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) //d174179
                    {
                        Tr.debug(tc, "enlisting XAResource from RA, recovery ID is " + recoveryId);
                    }
                    if (!ivRRSTransactional) {
                        ivTransactionManager.enlist(ivXAResource, recoveryId);
                    } else {
                        XAResource xaResource = container.getEJBRuntime().getRRSXAResource(bmd, coord.getXid());
                        ivTransactionManager.enlist(xaResource, recoveryId);
                    }
                }
            }
        } catch (Throwable t)
        {
            FFDCFilter.processException(t, CLASS_NAME + ".beforeDelivery", "1244", this);
            throw new ResourceException("beforeDelivery failure", t);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "MessageEndpoint.beforeDelivery called for " + method.getName());
        }
    }

    /**
     * Resource adapter calls this method after a message
     * is delivered and processed by an MDB instance.
     * <p>
     * <dl>
     * <dt>
     * <b>pre-condition</b>
     * <dd>
     * Caller ensures this method is only called from the Thread
     * that called beforeDelivery method.
     * </dl>
     */
    @Override
    public void afterDelivery() throws ResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "MessageEndpoint.afterDelivery");
        }

        if (!ivSkipCheckState) {
            //method param for checkState can be null since it's only used when the method is a MDB_BUSINESS_METHOD
            checkState(-1, null, AFTER_DELIVERY_METHOD);
        }

        try
        {
            if (ivRollbackOnly) // f743-7046
            {
                ContainerTx currentTx = ivEJSDeployedSupport.getCurrentTx();
                if (currentTx != null) // d629697
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        Tr.debug(tc, "calling setRollbackOnly");
                    }
                    currentTx.setRollbackOnly(); // f743-7046
                }
            }

            // The container postInvoke will complete the TX if the TX is
            // not an imported global TX. Rollback occurs if setRollbackOnly
            // was invoked on the current TX.
            container.postInvoke(this, ivMethodId, ivEJSDeployedSupport);
        } catch (Throwable e)
        {
            //FFDCFilter.processException(e, CLASS_NAME + ".afterDelivery", "1280", this);
            // f743-7046 start
            if (!ivRollbackOnly) // f743-7046
            {
                FFDCFilter.processException(e, CLASS_NAME + ".afterDelivery", "1280", this);
                throw new ResourceException("afterDelivery failure", e);
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "MessageEndpoint.afterDelivery is eating Throwable: " + e.getMessage()
                                 + " since " + majorJCAVersion + "." + minorJCAVersion + " requires abort of TX and no exceptions to be thrown.", e);
                }
            } // f743-7046 end
        } finally
        {
            // Release objects no longer needed and ensure the
            // rollback only and imported TX flags are reset to false.
            ivRollbackOnly = false; // f743-7046
            ivImportedTx = false; // f743-7046
            ivEJSDeployedSupport = null;
            ivMDB = null;
            ivMethod = null;

            // Enter the ready state.
            synchronized (this)
            {
                ivState = READY_STATE;
                ivThread = null;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                Tr.exit(tc, "MessageEndpoint.afterDelivery");
            }
        }
    }

    /**
     * This method may be called by the resource adapter to indicate
     * that it no longer needs a proxy endpoint instance. This hint
     * may be used by the application server for endpoint pooling decisions.
     * <p>
     * <dl>
     * <dt>
     * <b>pre-condition</b>
     * <dd>
     * Caller ensures this method is only called from the Thread
     * that called beforeDelivery method.
     * </dl>
     */
    @Override
    public void release()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "MessageEndpoint.release");
        }

        //method param for checkState can be null since it's only used when the method is a MDB_BUSINESS_METHOD
        checkState(-1, null, RELEASE_METHOD);

        // d174179
        //d192893 - start of change
        if (ivEJSDeployedSupport != null)
        {
            // A call to afterCompletion did NOT occur prior to this release method call.
            // However, due to pre-condition of this method, we know the release method was
            // called by the same thread that called beforeDelivery.  For this case,
            // the JCA 1.5 spec requires release method to abort the transaction and
            // to throw IllegalStateException (since beforeDelivery and afterDelivery
            // must occur in pairs).
            try
            {
                ContainerTx currentTx = ivEJSDeployedSupport.getCurrentTx();
                if (currentTx != null) // d629697
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        Tr.debug(tc, "release is aborting transaction started by beforeDelivery since afterDelivery did not occur prior to release.");
                    }

                    // Force afterDelivery to rollback the transaction.
                    currentTx.setRollbackOnly();
                }

                // Using try/finally to preserve old reflection proxy behavior where
                // afterDelivery() used to not directly call checkState(), but since
                // the proxy invoke() method no longer exists it was moved down into
                // afterDelivery().
                try
                {
                    ivSkipCheckState = true;
                    afterDelivery();
                } finally
                {
                    ivSkipCheckState = false;
                }

                // Note that the afterDelivery call will have changed the
                // state out of the RELEASED state, but it will be changed
                // back to RELEASED below                                d354603
            } catch (ResourceException re)
            {
                // FFDCFilter.processException(t, CLASS_NAME + ".release", "1349", this);

                // Ignore ResourceException from afterDelivery since we expect it to occur
                // as a result of setting the rollback only flag.  However, set the discard
                // required flag since we are not sure whether MessageEndpoint proxy is in a
                // reusable state due to the transaction being aborted by RA.
                ivDiscardRequired = true;
            } catch (Throwable t)
            {
                FFDCFilter.processException(t, CLASS_NAME + ".release", "1359", this);

                // We have an unexpected Throwable occurring during the abort.  Log this
                // unexpected Throwable object and set the discard required flag since
                // we are not sure whether MessageEndpoint proxy is in a reusable
                // state due to the unexpected Throwable occurring.
                ivDiscardRequired = true;
                Tr.error(tc, "IGNORING_UNEXPECTED_EXCEPTION_CNTR0033E", t);
            }
        }

        // A call to afterCompletion did occur prior to this release method call.
        // In this case, check ivDiscardRequired flag to determine whether to
        // the discard instance or to return the instance to MessageEndpointFactory.
        if (ivDiscardRequired)
        {
            // Change state to remember this instance was discarded so that the checkState
            // method in this class will throw IllegalStateException if the RA
            // tries to use this instance after the release has occurred.
            synchronized (this)
            {
                ivState = DISCARDED_STATE;
            }

            ivMessageEndpointFactory.returnInvocationHandler(this, false); // LI2110.56

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                Tr.debug(tc, "MessageEndpoint proxy was discarded as result of prior IllegalStateException occuring.");
            }
        }
        else
        {
            // Discard is not required.  In this case, return the invocation handler to
            // the MessageEndpointFactory so that it can pool the MessageEndpoint proxy instance.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                Tr.debug(tc, "returning MessageEndpoint proxy to the free pool");
            }

            // Insure the MessageEndpoint is in the 'RELEASED' state prior
            // to returning to the free pool. A call to checkState will have
            // done that prior to invoking this method, but the above call
            // to afterDelivery may have changed that.                   d354603
            synchronized (this)
            {
                ivState = RELEASED_STATE;
            }

            ivMessageEndpointFactory.returnInvocationHandler(this, true); // LI2110.56
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "MessageEndpoint.release");
        }

        //d192893 - end of change.
    }

    /**
     * Called after the pool discards the object. This gives the
     * object an opportunity to perform any required clean up.
     * 
     */
    public static void discard(MessageEndpointBase proxy)
    {
        // Ensure we are no longer holding any object references.
        proxy.ivMDB = null;
        proxy.ivMessageEndpointFactory = null;
        proxy.ivMethod = null;
        proxy.ivXAResource = null;
        proxy.ivRecoverableXAResource = false;
        proxy.container = null;
        proxy.ivEJSDeployedSupport = null;
        proxy.ivTransactionManager = null;
        proxy.ivThread = null;
    }

    /**
     * Called prior to the object being placed back in the pool.
     */
    public static void reset(MessageEndpointBase proxy)
    {
        proxy.ivXAResource = null;
        proxy.ivThread = null;
    }
}

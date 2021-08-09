/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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

import javax.resource.ResourceException;
import javax.resource.spi.ApplicationServerInternalException;
import javax.resource.spi.RetryableUnavailableException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.MDBInternalHome;
import com.ibm.ejs.container.util.MethodAttribUtils;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBTransactionAttribute;
import com.ibm.ws.ejbcontainer.runtime.EJBApplicationEventListener;
import com.ibm.ws.ejbcontainer.util.Pool;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tx.embeddable.RecoverableXAResourceAccessor;

/**
 * This class provides the common implementation of the JCA MessageEndpointFactory
 * interface for all runtime environments (traditional, liberty, embeddable) and
 * is used by JCA component/resource adapters to create/release a MessageEndpoint
 * proxy object for JCA MessageEndpoint Inflows. <p>
 * 
 * Internally, this object is also a home object for a MessageDrivenBean. In the
 * EJB specification, a MDB does not have a home object, however, it is convenient
 * for current ejb container implementation to make this object a home so that it
 * fits into the current ejb container infrastructure. Also, by making this object
 * a home object, we can ensure the following JCA requirements are met:
 * <ul>
 * <li>A separate factory is needed for each MDB that implements the same messaging type.
 * <li>A separate factory is needed for each application that uses the same MDB.
 * </ul>
 * <p>
 * Creating a home object as the MessageEndpointFactory ensures above requirements are
 * met and that the J2EEName for the home is a unique key for each MessageEndpointFactory
 * instance.
 */
@SuppressWarnings("serial")
public abstract class BaseMessageEndpointFactory extends EJSHome
                implements MessageEndpointFactory, MDBInternalHome, EJBApplicationEventListener {// d450478
    private static final String CLASS_NAME = BaseMessageEndpointFactory.class.getName();
    private static TraceComponent tc = Tr.register(BaseMessageEndpointFactory.class,
                                                   "EJBContainer", "com.ibm.ejs.container.container");

    // d197017 begins
    /**
     * Constants for ivState.
     */
    protected static final byte INACTIVE_STATE = 0;
    protected static final byte ACTIVATING_STATE = 1;
    protected static final byte ACTIVE_STATE = 2;
    protected static final byte DEACTIVATING_STATE = 3;
    protected static final byte DEACTIVATE_PENDING_STATE = 4; //d450478

    /**
     * RA_DOES_NOT_SUPPORT_XATRANSACTIONS is a constant used to indicate that the
     * RA's DD indicated that it did NOT support XATransations, therefore
     * the JCA runtime will not setup for XARecovery.
     */
    protected static final int RA_DOES_NOT_SUPPORT_XATRANSACTIONS = 0;

    /**
     * ERROR_DURING_TRAN_RECOVERY_SETUP is a constant used to indicate that the
     * JCA runtime encountered an error while trying to setup for transaction
     * recovery, therefore the JCA runtime cannot support recovery for this endpoint.
     */
    protected static final int ERROR_DURING_TRAN_RECOVERY_SETUP = 1;

    /**
     * Finite state machine state.
     */
    protected byte ivState = INACTIVE_STATE;

    /**
     * The thread that is activating the endpoint.
     */
    protected Thread ivActivatingThread = null;

    // d197017 ends

    /**
     * Recovery ID needed when container enlists a XAResource with the transaction
     * manager service. The J2C code will make this known to container by calling
     * the setRecoveryId method prior to the createEndpoint method being called.
     */
    protected int ivRecoveryId;

    /**
     * Indicates whether J2C already made the recovery ID know to this factory object.
     */
    protected boolean ivRecoveryIdKnown = false;

    /**
     * Indicates whether or not XAResource must be enlisted into a transaction.
     */
    private boolean ivEnlistNotNeeded = false;

    /**
     * Reason passed to setTranEnlistmentNotNeeded method.
     */
    private int ivEnlistNotNeededReason;

    /**
     * Set to true if error message already logged for the reason specified by
     * ivEnlistNotNeededReason. This flag is used to ensure that we do not fill
     * up log file with same message if for some reason the RA keeps calling
     * createEndpoint method with a non-null reference to an XAResource object.
     */
    private boolean ivEnlistNotNeededMessageLogged = false;

    /**
     * Lock object to use for synchronization.
     */
    protected final Object ivStateLock = new Object() {};

    /**
     * Pool of MessageEndpointHandler objects.
     */
    private Pool ivInvocationHandlerPool = null;

    /**
     * The number of MessageEndpoint created proxy objects that are
     * either currently in use or in the free pool (ivInvocationHandlerPool).
     * Those created instances that are no longer in use and were
     * discarded rather than put back in free pool are not included
     * in this count (e.g. count is decremented when proxy is discarded). // f743-7046
     */
    private int ivNumberOfMessageEndpointsCreated; //LI2110.56

    /**
     * Maximum number of MessageEndpoint proxy objects allowed to be created
     * That is, the number of MessageEndpoint proxy objects created must
     * remain <= proxy objects in use + proxy objects in the free pool.
     * Note, if proxy object is discarded (no longer in use and is not
     * returned to free pool for reuse), then it is not included in the
     * ivnumberOfMessageEndpointsCreated count since it no longer exists once
     * it is garbage collected by the JVM.
     */
    protected volatile int ivMaxCreation; //LI2110.56

    /**
     * Array of EJBMethodInfo objects, one for each
     * method in the message listener interface.
     */
    private EJBMethodInfoImpl[] ivMdbMethods;

    /**
     * A unique String that identifies the Resource Adapter
     * so that it can be identified in error messages.
     */
    private String ivRAKey = null;

    /**
     * Defines the JCA specification major version implemented by the RA. Defaults to
     * JCA 1.5 if <code>setJCAVersion(fullJCAVersion)</code> in <code>MessageEndpointFactoryImpl</code>
     * class is not called to ensure JCA 1.5 behavior is used unless J2C calls the setJCAVersion method
     * to indicate the RA is JCA 1.6 or later.
     */
    protected int majorJCAVersion = 1;

    /**
     * Defines the JCA specification minor version implemented by the RA. Defaults to
     * JCA 1.5 if <code>setJCAVersion(fullJCAVersion)</code> in <code>MessageEndpointFactoryImpl</code>
     * class is not called to ensure JCA 1.5 behavior is used unless J2C calls the setJCAVersion method
     * to indicate the RA is JCA 1.6 or later.
     */
    protected int minorJCAVersion = 5;

    /**
     * Flag to represent whether the Resource Adapter using this MEF uses RRS Transactions.
     */
    protected boolean ivRRSTransactional = false;

    /**
     * Default constructor for a BaseMessageEndpointFactory object.
     * The initialize method must be called to initialize for a
     * specific MDB.
     * 
     * @throws RemoteException
     */
    public BaseMessageEndpointFactory() throws RemoteException
    {
        super();
    }

    /**
     * Activate the home for a MDB so that it can receive messages
     * from a message provider.
     */
    @Override
    public abstract void activateEndpoint() throws ResourceException;

    /**
     * Deactivate a previously activated MDB home.
     */
    @Override
    public abstract void deactivateEndpoint() throws ResourceException;

    /**
     * This is used to create a message endpoint. The message endpoint is expected
     * to implement the correct message listener type.
     * 
     * @param xaResource - is an optional XAResource instance used by resource adapter
     *            to get transaction notifications when the message delivery is transacted.
     * 
     * @return a message endpoint proxy instance.
     * 
     * @throws UnavailableException - is thrown to indicate a transient failure in creating
     *             a message endpoint. Subsequent attempts to create a message endpoint might succeed.
     * 
     * @see javax.resource.spi.endpoint.MessageEndpointFactory#createEndpoint
     */
    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource) throws UnavailableException
    {
        return createEndpoint(xaResource, 0L); // f743-7046
    }

    /**
     * This is used to create a message endpoint. The message endpoint is expected
     * to implement the correct message listener type.
     * 
     * @param xaResource - is an optional XAResource instance used by resource adapter
     *            to get transaction notifications when the message delivery is transacted.
     * 
     * @param timeout is an optional timeout value that when greater than zero indicates the
     *            maximum time to wait for resources to become available. If time limit is
     *            exceeded, a RetryUnavailableException is thrown if the condition is temporary.
     *            If not temporary, an UnavailableException is thrown.
     * 
     * @return a message endpoint proxy instance.
     * 
     * @throws RetryableUnavailableException - is thrown to indicate a transient failure in creating
     *             a message endpoint. Subsequent attempts to create a message endpoint might succeed.
     * 
     * @throws UnavailableException - is thrown to indicate a permanent failure in creating
     *             a message endpoint. Subsequent attempts to create a message endpoint will not succeed.
     * 
     * @see javax.resource.spi.endpoint.MessageEndpointFactory#createEndpoint
     */
    // f743-8212 added entire method.
    // f743-7046 moved code from other createEndpoint to this method and modified it for timeout parameter.
    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "MEF.createEndpoint for enterprise class " + beanMetaData.enterpriseBeanName);
        }

        boolean recoverableXAResource = false;
        // d197017 begins
        // Make sure endpoint is activated before proceeding.
        Object proxy = null;
        MessageEndpointBase endpoint = null;
        boolean newInstanceRequired = false; //LI2110.56
        synchronized (ivStateLock)
        {
            // If RRS then xaResource passed in may be null. We need to enlist the RRS XAResource and not the XAResource passed in by the
            // RA. So in that case we need to check for the recoveryId
            if (xaResource != null || ivRRSTransactional) //d197017
            {
                // Ensure setRecoveryID method is called prior to this method since
                // recovery ID is needed to enlist the XAResource with the
                // transaction service during beforeDelivery.
                if ((ivRecoveryIdKnown == false) || (ivRecoveryId == 0)) //d194602
                {
                    if (ivEnlistNotNeeded == false)
                    {
                        if (RecoverableXAResourceAccessor.isRecoverableXAResource(xaResource)) //d197017
                        {
                            recoverableXAResource = true;
                        }
                        else
                        {
                            // CNTR0082E: Can not enlist XAResource since recovery ID for
                            // resource adapter {0} for MDB {1} is not known.
                            Tr.error(tc, "ENDPOINT_RECOVERY_ID_UNKNOWN_CNTR0082E",
                                     ivRAKey, beanMetaData.j2eeName);
                            throw new UnavailableException("setRecoveryId must be called prior to createEndpoint");
                        }
                    }
                }
            }

            if (ivState == ACTIVE_STATE)
            {
                if (!isEndpointActive()) //d450478
                {
                    // This can happen when RALifeCycleManager forcefully deactivates an
                    // endpoint during an RA stop operation. See the
                    // messageEndpointForcefullyDeactivated method in this class.
                    throw new UnavailableException("endpoint needs to be activated.");
                }

                // Use pool only if one was created.
                if (ivInvocationHandlerPool != null) // d423445.1
                {
                    // LI2110.56 start of change.
                    // Endpoint is active, so proceed with creating the endpoint.
                    // First see if one is available in the free pool.
                    endpoint = (MessageEndpointBase) ivInvocationHandlerPool.get();
                    if (endpoint == null)
                    {
                        // Free pool empty, so check whether it okay to create a new one.
                        // f743-7046 start
                        if (ivNumberOfMessageEndpointsCreated < ivMaxCreation) //LI2110.56
                        {
                            // Limit was NOT reached, so just increment count of created endpoints.
                            ++ivNumberOfMessageEndpointsCreated;
                            newInstanceRequired = true;
                        }
                        else
                        {
                            // Max creation limit reached. Was a valid wait timeout value provided?
                            if (timeout <= 0)
                            {
                                if (majorJCAVersion == 1 && minorJCAVersion == 5)
                                {
                                    // JCA 1.5 RA does not provide a timeout value, so throw same exceptions
                                    // we have always thrown in past to ensure same behavior.  //PM16610
                                    throw new UnavailableException("limit for number of MessageEndpoint proxies reached. Limit = " + ivMaxCreation);
                                }
                                else
                                {
                                    // JCA 1.6 or later RA did not provide a valid wait timeout value, so throw the
                                    // retryable exception immediately.  //PM16610
                                    throw new RetryableUnavailableException("limit for number of MessageEndpoint proxies reached.  Limit = " + ivMaxCreation);
                                }
                            }
                            else
                            {
                                // A valid wait timeout value was provided, so it must be JCA 1.6 or later.
                                // Wait for some thread to call MessageEndpoint.release() method. When that happens
                                // the returnInvocationHandler method in this class is called and it will notify this
                                // thread that it can proceed with the create.
                                try
                                {
                                    ivStateLock.wait(timeout);
                                } catch (InterruptedException e)
                                {
                                    //FFDCFilter.processException(e, CLASS_NAME + ".createEndpoint", "519", this);
                                }

                                // Determine whether wait timeout or release occurred. If release occurred,
                                // then a handler will be retrieved from the pool.
                                endpoint = (MessageEndpointBase) ivInvocationHandlerPool.get();
                                if (endpoint == null)
                                {
                                    // Timed out waiting for a proxy to be released.
                                    throw new RetryableUnavailableException("timed out waiting for a MessageEndpoint proxy to become available.");
                                }
                                else
                                {
                                    // A MessageEndpoint proxy is now available. Ensure deactivate did not
                                    // occur while waiting for a MessageEndpoint to be released.
                                    if (ivState == ACTIVE_STATE)
                                    {
                                        // Still active and there is at least 1 MessageEndpoint proxy available.
                                        // handler was retrieved from the pool.  No need to create a new one.
                                        newInstanceRequired = false;
                                    }
                                    else if ((ivState == DEACTIVATING_STATE) || (ivState == DEACTIVATE_PENDING_STATE)) //d450478
                                    {
                                        throw new UnavailableException("deactivate of endpoint is in progress.");
                                    }
                                    else
                                    {
                                        throw new UnavailableException("endpoint needs to be activated.");
                                    }
                                }
                            }
                        } // f743-7046 end of change.
                    } // LI2110.56 end of change.
                }
            }
            else if (ivState == ACTIVATING_STATE)
            {
                // Endpoint is activating, so check if calling thread is the activating thread.
                if (ivActivatingThread == Thread.currentThread())
                {
                    // Calling thread is the activating thread, so throw
                    // exception since we can not block the activating thread and
                    // it is not retryable from this thread.
                    throw new UnavailableException("activating thread not allowed to create endpoint during activation.");
                }
                else
                {
                    // Not the activation thread, so block this thread until the activate completes.
                    try
                    {
                        if (timeout > 0) // f743-7046 start
                        {
                            ivStateLock.wait(timeout);
                        }
                        else
                        {
                            ivStateLock.wait();
                        } // f743-7046 end
                    } catch (InterruptedException i)
                    {
                        //FFDCFilter.processException(e, CLASS_NAME + ".createEndpoint", "406", this);
                    }

                    // Now verify activate was successful.
                    if (ivState != ACTIVE_STATE)
                    {
                        if (ivState == ACTIVATING_STATE) // f743-7046 start
                        {
                            // This can only happen if a JCA 1.6 or later RA provided a timeout value.
                            // So throw the retryable exception immediately.
                            throw new RetryableUnavailableException("timed out while waiting for activation to complete.");
                        }
                        else
                        {
                            // Neither in active nor activating state, so a deactivate must have occurred.
                            // This is not a transient condition, so throw non retryable exception.
                            throw new UnavailableException("endpoint needs to be activated.");
                        } // f743-7046 end
                    }
                }
            }
            else if ((ivState == DEACTIVATING_STATE) || (ivState == DEACTIVATE_PENDING_STATE)) //d450478
            {
                throw new UnavailableException("deactivate of endpoint is in progress.");
            }
            else
            {
                throw new UnavailableException("endpoint needs to be activated.");
            }
        }
        // d197017 ends

        // Create MessageEndpoint Proxy object using the Constructor Method that
        // was cached by the initialize method of this object.
        UnavailableException ex = null;
        try
        {
            // Was above code able to get a MessageEndpointHandler
            // from the free pool?
            if (endpoint != null)
            {
                // Yes, so get proxy object from the MessageEndpointHandler
                // object obtained from free pool.
                proxy = endpoint.ivProxy;
            }
            else
            {
                // Nope, so free pool must have been empty.  Since we got this
                // far, we know creation counter was incremented and newInstanceRequired
                // flag should have been set by above code.  So go ahead and try
                // to create a new instance.  Exception handler code below will decrement
                // counter if something goes wrong in the creation.
                proxy = beanMetaData.localImplClass.newInstance();

                // For no-method interface MDB, the 'proxy' will not
                // subclass MessageEndpointBase, but the MDB itself, so a separate
                // MessageEndpointBase must be created to hold the metadata and
                // associated with the real proxy.
                if (beanMetaData.ivIsNoMethodInterfaceMDB)
                {
                    endpoint = new MessageEndpointBase();
                    // Set the ivMessageEndpointBase on the generated proxy.
                    beanMetaData.ivMessageEndpointBaseField.set(proxy, endpoint);
                }
                // Otherwise, normal proxy - subclasses MessageEndpointBase
                else
                {
                    endpoint = (MessageEndpointBase) proxy;
                }

                MessageEndpointBase.construct(this,
                                              endpoint,
                                              ivRecoveryId,
                                              container,
                                              beanMetaData,
                                              pmiBean,
                                              wrapperManager,
                                              ivRRSTransactional);
                endpoint.ivProxy = proxy;
            }

            // Initialize InvocationHandler to be owner of this
            // proxy object and XAResource and determine if enlistment
            // of a XAResource is needed.
            if (ivEnlistNotNeeded)
            {
                if (xaResource == null)
                {
                    // Enlistment is not needed and no XAResource
                    // was passed by RA.  So everything is fine.
                    MessageEndpointBase.initialize(endpoint, null, false, majorJCAVersion, minorJCAVersion); // f743-7046
                }
                else
                {
                    // Enlistment is not needed, but the RA did pass an XAResource object.
                    // We need to throw an exception since transaction recovery setup was
                    // not completed successfully for this RA.
                    ex = mapAndLogTranEnlistmentNotNeeded();
                }
            }
            else
            {
                // Transaction recovery is setup for this RA, so
                // use whatever the RA passed as an XAResource.
                MessageEndpointBase.initialize(endpoint, xaResource, recoverableXAResource, majorJCAVersion, minorJCAVersion); // f743-7046
            }
        } catch (Throwable t)
        {
            // CNTR0083E: Creation of a message endpoint failed: \n {0}
            Tr.error(tc, "CREATE_ENDPOINT_FAILED_CNTR0083E", t);
            ex = new UnavailableException("Creation of MessageEndpoint Proxy failed", t);
        }

        // Check whether exception needs to be thrown.
        // If so, return invocation handler to pool if necessary
        // and throw the exception.
        if (ex != null)
        {
            // Use pool only if one was created.
            if (ivInvocationHandlerPool != null) // d423445.1
            {
                // Since proxy not being returned to caller,
                // Return InvocationHandler to the pool if both MessageEndpointHandler
                // and proxy object was created prior to the exception.
                if (endpoint != null && proxy != null) // LI2110.56
                {
                    MessageEndpointBase.reset(endpoint); // F73236 PooledObject removed, call reset() directly
                    ivInvocationHandlerPool.put(endpoint);
                }
                else
                {
                    // LI2110.56 beginning of change.
                    // Decrement creation counter so that it is not off by 1 as
                    // a result of a failure during creation of either MessageEndpointHandler
                    // or the proxy object itself (e.g. out of memory exception).
                    if (newInstanceRequired)
                    {
                        synchronized (ivStateLock)
                        {
                            --ivNumberOfMessageEndpointsCreated;
                        }
                    } // LI2110.56 end of change.
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                BeanMetaData bmd = super.beanMetaData;
                Tr.exit(tc, "createEndpoint for enterprise class " + bmd.enterpriseBeanName + " failed.");
            }

            // Now throw the UnavailableException
            throw ex;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "MEF.createEndpoint for enterprise class " + beanMetaData.enterpriseBeanName);
        }

        return (MessageEndpoint) proxy;
    }

    /**
     * This is used to find out whether message deliveries to a message endpoint
     * will be transacted or not. The message delivery preferences must not
     * change during the lifetime of a message endpoint. This information is
     * only a hint and may be useful to perform optimizations on message delivery.
     * 
     * @param method - description of a target method. This information about
     *            the intended target method allows an application server to
     *            find out whether the target method call will be transacted or not.
     * 
     * @return true, if message endpoint requires transacted message delivery.
     * 
     * @throws NoSuchMethodException if Method not found for this MessageEndpoint.
     * 
     */
    @Override
    public boolean isDeliveryTransacted(Method method) throws NoSuchMethodException
    {
        BeanMetaData bmd = super.beanMetaData;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "MEF.isDeliveryTransacted called for "
                         + bmd.enterpriseBeanName + "." + method.getName());
        }

        /*
         * Get EJBMethodInfo object for the specified method.
         */
        EJBMethodInfoImpl minfo = getEJBMethodInfo(method);
        if (minfo == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                Tr.exit(tc, "MEF.isDeliveryTransacted failed to find method "
                            + bmd.enterpriseBeanName + "." + method.getName());
            }

            // CNTR0085E: MDB {0} must implement method {1} of interface {2}.
            Tr.error(tc, "NO_SUCH_MDB_METHOD_CNTR0085E",
                     bmd.j2eeName, method.getName(), bmd.localInterfaceClass);

            throw new NoSuchMethodException(bmd.enterpriseBeanName + "." + method.getName()
                                            + " not found");
        }

        /*
         * Use EJBMethodInfo object to determine if transacted or not.
         * The assumption is true is returned only for TX_REQUIRED and
         * false is returned for both BMT and TX_NOT_SUPPORTED.
         */
        EJBTransactionAttribute txAttr = minfo.getEJBTransactionAttribute();
        //TransactionAttribute txAttr = minfo.getTransactionAttribute();
        boolean transacted;
        if (txAttr == EJBTransactionAttribute.REQUIRED)
        {
            transacted = true;
        }
        else if (txAttr == EJBTransactionAttribute.NOT_SUPPORTED)
        {
            transacted = false;
        }
        else if (txAttr == EJBTransactionAttribute.BEAN_MANAGED)
        {
            // JCA 1.5 only addresses CMT.  Our guess is BMT
            // should return false.  Would be nice to have a
            // clarification to JCA spec regarding this case.
            transacted = false;
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                Tr.exit(tc, "MEF.isDeliveryTransacted detected invalid TX attribute for "
                            + bmd.enterpriseBeanName + "." + method.getName()
                            + ", TX attribute is " + txAttr);
            }

            // Something is wrong since we should not have allowed a MDB to be installed
            // with invalid deployment for transaction attributes.

            // CNTR0084E: Method {0} of MDB {1} is deployed with an incorrect transaction attribute.
            Tr.error(tc, "INVALID_MDB_TX_ATTR_CNTR0084E",
                     method.getName(), bmd.j2eeName);

            ResourceException r;
            r = new ApplicationServerInternalException(
                            "Method exists, but TX attribute is neither REQUIRED, NOT_SUPPORTED, nor BEAN_MANAGED: "
                                            + txAttr);

            NoSuchMethodException ex = new NoSuchMethodException("see chained exception");
            ex.initCause(r);
            throw ex;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "MEF.isDeliveryTransacted returning " + transacted + " for "
                        + bmd.enterpriseBeanName + "." + method.getName());
        }

        return transacted;
    }

    /**
     * Get the EJBMethodInfo object associated with a specified Method object.
     * 
     * @param method - the target of this request.
     * 
     * @return EJBMethodInfo for target of this request. Note, a null reference
     *         is returned if Method is not a method of this EJB component.
     */
    private EJBMethodInfoImpl getEJBMethodInfo(Method method)
    {
        // Get target method signature.
        String targetSignature = MethodAttribUtils.methodSignature(method);

        // Search array of EJBMethodInfo object until the one that matches
        // target signature is found or all array elements are processed.
        EJBMethodInfoImpl minfo = null;
        int n = ivMdbMethods.length;
        for (int i = 0; i < n; ++i)
        {
            minfo = ivMdbMethods[i];
            if (targetSignature.equals(minfo.getMethodSignature()))
            {
                return minfo;
            }
        }

        // Method not found, so return null.
        return null;
    }

    /**
     * Extends EJSHome.initialize method with processing
     * specific to a MessageEndpointFactory for a specified MDB.
     * 
     * @param ejbContainer - the EJSContainer object that owns this factory.
     * @param id - the BeanId for this factory object (eg home bean ID).
     * @param bmd - the BeanMetaData for the MDB.
     * 
     * @throws RemoteException - thrown if a failure occurs during initialization.
     */
    @Override
    public void initialize(EJSContainer ejbContainer, BeanId id, BeanMetaData bmd) throws RemoteException //f743-8212
    {
        // First perform the normal EJSHome initialization that occurs.
        super.initialize(ejbContainer, id, bmd); //f743-8212

        // Now extend with initialization required for a
        // MessageEndpointFactoryImpl object.

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(
                     tc,
                     "MEF Initializing MessageEndpointFactory for enterprise class "
                                     + bmd.enterpriseBeanName
                                     + ", with message listener interface of "
                                     + bmd.localInterfaceClass.getName());
        }
        ivMdbMethods = beanMetaData.localMethodInfos;

        // Create pool of InvocationHandlers objects to be by createEndpoint.
        ivMaxCreation = bmd.maxPoolSize; //LI2110.56

        // Create pool only if NoEJBPool system property is set to false.
        // Note, ivMaxCreation is set to zero if if NoEJBPool = true.
        if (ivMaxCreation > 0) // d423445.1
        {
            MessageEndpointHandlerPool messageEndpointHandlerPool = new MessageEndpointHandlerPool(this);
            ivInvocationHandlerPool = container.poolManager.create(bmd.minPoolSize, bmd.maxPoolSize, null, messageEndpointHandlerPool);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(
                    tc,
                    "MessageEndpointFactory initialized for enterprise class "
                                    + bmd.enterpriseBeanName
                                    + " with messaging listener interface of "
                                    + bmd.localInterfaceClass.getName());
        }
    }

    /**
     * Sets the recovery ID assigned to this instance of a WsMessageEndpointFactory
     * object. The recovery ID is used so that a WsMessageEndpointFactory object is
     * able to enlist the XAResource passed to it on the createEndpoint method call
     * with the websphere transaction service when necessary. The websphere TX service
     * requires a recovery ID to be passed to it during the enlistment.
     * <dl>
     * <dt>
     * pre-condition
     * <dd>
     * This method must be called prior to the endpointActivation method being called
     * on the javax.resource.spi.ResourceAdapter interface. The reason for this requirement
     * is to ensure the WsMessageEndpointFactory object knows the recovery ID prior to
     * the createEndpoint method of the javax.resource.spi.endpoint.MessageEndpointFactory
     * interface being called by the ResourceAdapter object. This eliminates the need for
     * MessageEndpointFactory to throw a UnavailableException if createEndpoint is called
     * before recovery ID is known.
     * <dd>
     * This method can only be called once per WsMessageEndpointFactory object.
     * <dt>
     * post-condition
     * <dd>
     * </dl>
     * 
     * @throws ResourceException if this method was previously called for
     *             this MessageEndpointFactory instance.
     */
    public void setRecoveryID(int recoveryId) throws ResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "MEF.setRecoveryID for enterprise class " + beanMetaData.enterpriseBeanName);
        }

        if (ivRecoveryIdKnown)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                Tr.exit(tc, "MEF.setRecoveryID for enterprise class " + beanMetaData.enterpriseBeanName);
            }
            throw new ApplicationServerInternalException("setRecoveryId can only be called once per factory");
        }
        else
        {
            ivRecoveryId = recoveryId;
            ivRecoveryIdKnown = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "MEF.setRecoveryID for enterprise class " + beanMetaData.enterpriseBeanName);
        }
    }

    /**
     * Method setTranEnlistmentNotNeeded.
     * <p>
     * This method indicates that the MessageEndpointFactory should NOT enlist
     * an XAResource in a transaction.
     * 
     * @param reason This is the reason code for why the MessageEndpointFactory
     *            does not need to enlist. Valid reason codes are:
     *            <p>
     *            MessageEndpointFactory.RA_DOES_NOT_SUPPORT_XATRANSACTIONS - this indicates
     *            that the ResourceAdapter has indicated that it does not support XATransactions.
     *            <p>
     *            MessageEndpointFactory.ERROR_DURING_TRAN_RECOVERY_SETUP - this indicates
     *            that the RALifeCycleManager encounter an error which prevented it from
     *            setting up transaction recovery for this ResourceAdapter. The
     *            MessageEndpointFactory should throw an exception and log an appropriate
     *            message if the RA attempts to use an XAResource in a transaction. This
     *            can't be allowed since recovery setup has failed.
     * 
     */
    public void setTranEnlistmentNotNeeded(int reason)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "MEF.setTranEnlistmentNotNeeded for enterprise class "
                         + beanMetaData.enterpriseBeanName + ", reason =  " + reason);
        }

        ivEnlistNotNeeded = true;
        ivEnlistNotNeededReason = reason;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "MEF.setTranEnlistmentNotNeeded for enterprise class "
                        + beanMetaData.enterpriseBeanName);
        }
    }

    /**
     * Method mapTranEnlistmentNotNeededToException.
     * <p>
     * This method maps the reason code that was passed to the setTranEnlistmentNotNeeded method
     * to an appropriate UnavailableException method to throw if a RA had called createEndpoint
     * and passed a non-null XAResource object to it. Also, an appropriate error message is
     * written to the activity log file.
     * 
     */
    private UnavailableException mapAndLogTranEnlistmentNotNeeded()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "MEF.mapAndLogTranEnlistmentNotNeeded for enterprise class "
                         + beanMetaData.enterpriseBeanName);
        }

        UnavailableException ex = null;

        switch (ivEnlistNotNeededReason)
        {
            case (RA_DOES_NOT_SUPPORT_XATRANSACTIONS): {
                if (ivEnlistNotNeededMessageLogged == false)
                {
                    // CNTR0087E: Resource adapter {0} is not allowed to pass a non null XAResource to createEndpoint method for MDB {1}.
                    Tr.error(tc, "RA_DOES_NOT_SUPPORT_XATRANSACTIONS_CNTR0087E",
                             ivRAKey, beanMetaData.j2eeName);
                }
                ex = new UnavailableException("Transaction recovery not setup for this RA since RA does not support XA transactions");
                break;
            }

            case (ERROR_DURING_TRAN_RECOVERY_SETUP): {
                if (ivEnlistNotNeededMessageLogged == false)
                {
                    // CNTR0086E: Transaction recovery setup error occurred for resource adapter {0}, MDB {1}.
                    Tr.error(tc, "ERROR_DURING_TRAN_RECOVERY_SETUP_CNTR0086E",
                             ivRAKey, beanMetaData.j2eeName);
                }
                ex = new UnavailableException("Error occured during transaction recovery setup for this Resource Adapter");
                break;
            }

            default: {
                if (ivEnlistNotNeededMessageLogged == false)
                {
                    // CNTR0081E: setTranEnlistmentNotNeeded called with an unrecognized reason code of {0}.
                    Tr.error(tc, "REASON_CODE_NOT_RECOGNIZED_CNTR0081E", Integer.valueOf(ivEnlistNotNeededReason));
                }
                ex = new UnavailableException("Error occured during transaction recovery setup for this Resource Adapter");
                break;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "MEF.mapAndLogTranEnlistmentNotNeeded for enterprise class "
                        + beanMetaData.enterpriseBeanName);
        }

        // Indicate error message is logged to prevent logging next time this method is called
        // for this MessageEndpointFactory instance.
        ivEnlistNotNeededMessageLogged = true;

        // Return exception to the caller to throw.
        return ex;
    }

    /**
     * Return MessageEndpoint proxy to the pool managed by this factory. This
     * method is expected to be called as a result of the resource adapter
     * calling the release method on the MessageEndpoint proxy.
     * 
     * @param endpoint is the MessageEndpointHandler being returned to free pool.
     * @param reuse must be set to true if you want MessageEndpointHandler to
     *            be returned to free pool for future reuse. If false, the MessageEndpointHandler
     *            will be discarded so that it becomes eligible for garbage collection.
     */
    // LI2110.56 - rewrote method to use new reuse parameter.
    public void returnInvocationHandler(MessageEndpointBase endpoint, boolean reuse)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "MEF.returnInvocationHandler");

        // Use pool only if one was created.
        if (ivInvocationHandlerPool != null) // d423445.1
        {
            if (reuse)
            {
                MessageEndpointBase.reset(endpoint); // F73236 PooledObject removed, call reset() directly
                ivInvocationHandlerPool.put(endpoint);
            }

            //f743-7046 start
            // Notify any thread, but only 1 thread since release
            // only occurred on one MessageEndpoint, that is blocked
            // in createEndpoint method waiting for a release method
            // to be called on some MessageEndpoint proxy.
            synchronized (ivStateLock)
            {
                // d643869
                // According to the JCA 1.5 spec section 12.5 the proxy
                // instance can be pooled and reused by the same resource adapter.
                // The proxy instance is not freed here and the count is
                // not decremented.  The discard method handles freeing
                // proxy instances that are pooled and decrementing the count.
                // --ivNumberOfMessageEndpointsCreated;
                ivStateLock.notify();
            }
            //f743-7046 end
        }

        // d643869
        // Decrement the ivNumberOfMessageEndpointsCreated and null the proxy
        // because the MessageEndpointHandler was not returned to the pool
        // and will be garbage collected by the JVM
        if (ivInvocationHandlerPool == null || !reuse)
        {
            synchronized (ivStateLock)
            {
                --ivNumberOfMessageEndpointsCreated;
                //endpointBase.ivProxy = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "MEF.returnInvocationHandler"); // d643869

    }

    /**
     * Set the Resource Adapter key that uniquely identifies
     * the RA. This key is useful as a unique identifier in
     * error messages.
     */
    public void setRAKey(String raKey)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "MEF.setRAKey for enterprise class " + beanMetaData.enterpriseBeanName);
        }

        ivRAKey = raKey;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "MEF.setRAKey for enterprise class " + beanMetaData.enterpriseBeanName);
        }
    }

    /**
     * Returns true if the endpoint is active. <p>
     * 
     * Provides a mechanism to check if the endpoint has been deactivated
     * out from under the MessageEndpointFactory. <p>
     */
    protected abstract boolean isEndpointActive();

    /**
     * If the PoolManager for ivInvocationHandlerPool discards
     * an instance from free pool since it has not been used for
     * some period of time, then this method will be called to
     * indicate an instance was discarded. This method is called
     * for each instance that is discarded.
     */
    //LI2110.56 - added entire method.
    public void discard()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "MEF.discard");

        if (ivInvocationHandlerPool != null) // d423445.1
        {
            synchronized (ivStateLock)
            {
                --ivNumberOfMessageEndpointsCreated;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "MEF.discard");

    }

    /**
     * Called when runtime framework notifies the EJB container
     * that starting of an application has completed (e.g application
     * is started). Thus, we can change the state of this MEF to
     * the ACTIVE_STATE and notify any threads that were blocked
     * in createEndpoint method waiting for MEF to become active.
     * 
     * @param appName is the name of the application that was started.
     * 
     * @see EJBApplicationEventListener#applicationStarted(String)
     * @see #createEndpoint(XAResource)
     */
    // d450478 - added entire method
    @Override
    public void applicationStarted(String appName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "MEF.applicationStarted for application " + appName);
        }

        // Change state to the active state and notify all threads
        // that were blocked while we were activating this endpoint.
        synchronized (ivStateLock)
        {
            ivState = ACTIVE_STATE;
            ivStateLock.notifyAll();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "MEF.applicationStarted for application " + appName);
        }
    }

    /**
     * Called when runtime framework notifies the EJB container
     * application event listener that it is stopping an application.
     * Runtime framework does NOT begin to stop each module of the application
     * until after each application event listener returns.
     * 
     * @param appName is the name of the application being stopped.
     * 
     * @see EJBApplicationEventListener#applicationStopping(String)
     */
    // d450478 - added entire method
    @Override
    public void applicationStopping(String appName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "MEF.applicationStopping for application " + appName);
        }

        synchronized (ivStateLock)
        {
            if (ivState == ACTIVE_STATE)
            {
                // No thread is ever waiting for a notification while
                // in the active state, so simply change state to
                // indicate deactivate is pending.
                ivState = DEACTIVATE_PENDING_STATE;
            }
            else if (ivState == ACTIVATING_STATE)
            {
                // Change state to indicate deactivate is pending
                // and notify any thread that is waiting for the activate
                // to complete.
                ivState = DEACTIVATE_PENDING_STATE;
                ivStateLock.notifyAll();
            }
            else if (ivState == DEACTIVATE_PENDING_STATE)
            {
                // activateendpoint threw an exception and changed
                // state to deactivate pending.  So we need to notify
                // threads that are waiting for the activate
                // to complete (e.g. threads blocked by createEndpoint
                // during the activation of an endpoint).
                ivStateLock.notifyAll();
            }
            else if (ivState == INACTIVE_STATE)
            {
                // This is possible, but not likely. For example, we could create this
                // MEF object and initialized it to INACTIVE_STATE.  An exception occurs
                // before we make it to the activateEndpoint method for this MEF.  If
                // that happens, then it is possible to get to this method while in
                // the inactive state.  So simply trace fact that this MEF was called
                // in inactive state and nothing was actually done.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                {
                    Tr.event(tc, "MEF.applicationStopping for application " + appName
                                 + " was called for an inactive endpoint.");
                }
            }
            else
            {
                // The only other state is DEACTIVATING_STATE. This method should NEVER
                // be called while in the deactivating state. Runtime framework should call
                // this method before it ever attempts to stop the modules of an application.
                // Thus, we should not enter deactivating state until after this method is called.
                // Create FFDC log file for this problem and trace occurrence of this event.
                // No reason to throw the exception since we created it just for the purpose
                // of doing the FFDC.
                String msg = "Internal programming error - applicationStopping called for application \""
                             + appName + "\" while in deactivating state. This should NEVER occur.";
                IllegalStateException ex = new IllegalStateException(msg);
                FFDCFilter.processException(ex, CLASS_NAME + ".applicationStopping", "1208", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                {
                    Tr.event(tc, msg);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "MEF.applicationStopping for application " + appName);
        }
    }

    /**
     * Sets the maximum number of message endpoints allowed to be created.
     */
    // F96377
    protected void setMaxEndpoints(int maxEndpoints)
    {
        if (ivMaxCreation != maxEndpoints)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "setMaxEndpoints = " + maxEndpoints);

            ivMaxCreation = maxEndpoints;

            // It doesn't make sense to pool more handlers/beans than the maximum
            // number of active endpoints, so adjust max pool sizes as necessary
            if (beanPool != null) {
                beanPool.setMaxSize(Math.min(maxEndpoints, beanPool.getMaxSize()));
            }
            if (ivInvocationHandlerPool != null) {
                ivInvocationHandlerPool.setMaxSize(Math.min(maxEndpoints, ivInvocationHandlerPool.getMaxSize()));
            }
        }
    }

    /**
     * Returns a unique name for the message endpoint deployment represented by the MessageEndpointFactory.
     * If the message endpoint has been deployed into a clustered application server then this method must
     * return the same name for that message endpoint’s activation in each application server instance.
     * It is recommended that this name be human-readable since this name may be used by the resource adapter
     * in ways that may be visible to a user or administrator. It is also recommended that this name remain
     * unchanged even in cases when the application server is restarted or the message endpoint re-deployed.
     */
    // FYI: if the methods did have signatures that depended on new JCA 1.7 API, we would have to use
    // subclasses with factories, which would be much more complicated
    public String getActivationName() {
        return beanMetaData.getJ2EEName().toString();
    }

    /**
     * Return the Class object corresponding to the message endpoint class. For example, for a Message Driven
     * Bean this is the Class object corresponding to the application's MDB class. The resource adapter may
     * use this to introspect the message endpoint class to discover annotations, interfaces implemented, etc.
     * and modify the behavior of the resource adapter accordingly. A return value of null indicates that the
     * MessageEndpoint doesn't implement the business methods of underlying message endpoint class.
     * 
     */
    // FYI: if the methods did have signatures that depended on new JCA 1.7 API, we would have to use
    // subclasses with factories, which would be much more complicated
    public Class<?> getEndpointClass() {
        return beanMetaData.enterpriseBeanClass;
    }
}

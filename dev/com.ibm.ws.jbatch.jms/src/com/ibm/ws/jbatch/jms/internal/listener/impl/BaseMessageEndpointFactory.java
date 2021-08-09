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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.ConnectionFactory;
import javax.resource.ResourceException;
import javax.resource.spi.ApplicationServerInternalException;
import javax.resource.spi.RetryableUnavailableException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jbatch.jms.internal.BatchJmsConstants;
import com.ibm.ws.tx.embeddable.RecoverableXAResourceAccessor;

/**
 * This class implements the JCA MessageEndpointFactory interface and is used by
 * JCA component/resource adapter to create/release a MessageEndpoint proxy
 * object for JCA MessageEndpoint Inflows.
 */
public abstract class BaseMessageEndpointFactory implements MessageEndpointFactory {

    private static final TraceComponent tc = Tr.register(BaseMessageEndpointFactory.class);

    /**
     * Constants for ivState.
     */
    protected static final byte INACTIVE_STATE = 0;
    protected static final byte ACTIVATING_STATE = 1;
    protected static final byte ACTIVE_STATE = 2;
    protected static final byte DEACTIVATING_STATE = 3;
    protected static final byte DEACTIVATE_PENDING_STATE = 4; // d450478

    /**
     * RA_DOES_NOT_SUPPORT_XATRANSACTIONS is a constant used to indicate that
     * the RA's DD indicated that it did NOT support XATransations, therefore
     * the JCA runtime will not setup for XARecovery.
     */
    protected static final int RA_DOES_NOT_SUPPORT_XATRANSACTIONS = 0;

    /**
     * ERROR_DURING_TRAN_RECOVERY_SETUP is a constant used to indicate that the
     * JCA runtime encountered an error while trying to setup for transaction
     * recovery, therefore the JCA runtime cannot support recovery for this
     * endpoint.
     */
    protected static final int ERROR_DURING_TRAN_RECOVERY_SETUP = 1;

    /**
     * Finite state machine state.
     */
    protected byte ivState = INACTIVE_STATE;
    /**
     * Recovery ID needed when container enlists a XAResource with the
     * transaction manager service. The J2C code will make this known to
     * container by calling the setRecoveryId method prior to the createEndpoint
     * method being called.
     */
    protected int ivRecoveryId;

    /**
     * Indicates whether J2C already made the recovery ID know to this factory
     * object.
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
     * Constructor object for creating a new MessageEndpoint proxy object.
     */
    protected Constructor<?> ivProxyCTOR = null;
    /**
     * A unique String that identifies the Resource Adapter so that it can be
     * identified in error messages.
     */
    private String ivRAKey = null;

    /**
     * Set to true if and only if the MDB instance can be cast to a variable of
     * type javax.jms.MessageListener. This allows us to call the JMS onMessage
     * directly on the MessageListener interface rather than using java
     * reflection to invoke the method.
     */
    protected boolean ivJMS = false;

    /**
     * Defines the JCA specification major version implemented by the RA.
     * Defaults to JCA 1.5 if <code>setJCAVersion(fullJCAVersion)</code> in
     * <code>MessageEndpointFactoryImpl</code> class is not called to ensure JCA
     * 1.5 behavior is used unless J2C calls the setJCAVersion method to
     * indicate the RA is JCA 1.6 or later.
     */
    protected int majorJCAVersion = 1;

    /**
     * Defines the JCA specification minor version implemented by the RA.
     * Defaults to JCA 1.5 if <code>setJCAVersion(fullJCAVersion)</code> in
     * <code>MessageEndpointFactoryImpl</code> class is not called to ensure JCA
     * 1.5 behavior is used unless J2C calls the setJCAVersion method to
     * indicate the RA is JCA 1.6 or later.
     */
    protected int minorJCAVersion = 5;
    /**
     * Flag to represent whether the Resource Adapter using this MEF uses RRS
     * Transactions.
     */
    protected boolean ivRRSTransactional = false;

    /**
     * Artificial representation of batch jms endpoint listener application
     * Since our listener is a part of the feature, there is no reall J2EE
     * application
     */
    protected J2EEName j2eeName = null;

    /**
     * Reference to the batchExecutor that create this object
     */
    protected BatchJmsExecutor batchExecutor;

    public void setJ2eeName(J2EEName j2eeName) {
        this.j2eeName = j2eeName;
    }

    public BaseMessageEndpointFactory(BatchJmsExecutor batchExecutor) {
        this.batchExecutor = batchExecutor;
    }

    public BatchJmsExecutor getBatchExecutor() {
        return batchExecutor;
    }
    /**
     * This is used to create a message endpoint. The message endpoint is
     * expected to implement the correct message listener type.
     * 
     * @param xaResource
     *            - is an optional XAResource instance used by resource adapter
     *            to get transaction notifications when the message delivery is
     *            transacted.
     * 
     * @return a message endpoint proxy instance.
     * 
     * @throws UnavailableException
     *             - is thrown to indicate a transient failure in creating a
     *             message endpoint. Subsequent attempts to create a message
     *             endpoint might succeed.
     * 
     * @see javax.resource.spi.endpoint.MessageEndpointFactory#createEndpoint
     */
    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource) throws UnavailableException {
        return createEndpoint(xaResource, 0L);
    }

    /**
     * This is used to create a message endpoint. The message endpoint is
     * expected to implement the correct message listener type.
     * 
     * @param xaResource
     *            - is an optional XAResource instance used by resource adapter
     *            to get transaction notifications when the message delivery is
     *            transacted.
     * 
     * @param timeout
     *            is an optional timeout value that when greater than zero
     *            indicates the maximum time to wait for resources to become
     *            available. If time limit is exceeded, a
     *            RetryUnavailableException is thrown if the condition is
     *            temporary. If not temporary, an UnavailableException is
     *            thrown.
     * 
     * @return a message endpoint proxy instance.
     * 
     * @throws RetryableUnavailableException
     *             - is thrown to indicate a transient failure in creating a
     *             message endpoint. Subsequent attempts to create a message
     *             endpoint might succeed.
     * 
     * @throws UnavailableException
     *             - is thrown to indicate a permanent failure in creating a
     *             message endpoint. Subsequent attempts to create a message
     *             endpoint will not succeed.
     * 
     * @see javax.resource.spi.endpoint.MessageEndpointFactory#createEndpoint
     */
    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException {

        boolean recoverableXAResource = false;
        MessageEndpointHandler handler = null;
        Proxy proxy = null;

        synchronized (ivProxyCTOR) {
            // If RRS then xaResource passed in may be null. We need to enlist
            // the RRS XAResource and not the XAResource passed in by the
            // RA. So in that case we need to check for the recoveryId
            if (xaResource != null || ivRRSTransactional) // d197017
            {
                // Ensure setRecoveryID method is called prior to this method
                // since
                // recovery ID is needed to enlist the XAResource with the
                // transaction service during beforeDelivery.
                if ((ivRecoveryIdKnown == false) || (ivRecoveryId == 0)) // d194602
                {
                    if (ivEnlistNotNeeded == false) {
                        if (RecoverableXAResourceAccessor.isRecoverableXAResource(xaResource)) // d197017
                        {
                            recoverableXAResource = true;
                        } else {
                            // CNTR0082E: Can not enlist XAResource since
                            // recovery ID for
                            // resource adapter {0} for MDB {1} is not known.
                            throw new UnavailableException("setRecoveryId must be called prior to createEndpoint");
                        }
                    }
                }
            }

            if (ivState == ACTIVE_STATE) {
                if (!isEndpointActive()) {
                    // This can happen when RALifeCycleManager forcefully
                    // deactivates an
                    // endpoint during an RA stop operation. See the
                    // messageEndpointForcefullyDeactivated method in this
                    // class.
                    throw new UnavailableException("endpoint needs to be activated.");
                }
            } else if ((ivState == DEACTIVATING_STATE) || (ivState == DEACTIVATE_PENDING_STATE)) {
                throw new UnavailableException("deactivate of endpoint is in progress.");
            } else {
                throw new UnavailableException("endpoint needs to be activated.");
            }
        }

        // Create MessageEndpoint Proxy object using the Constructor Method
        // that
        // was cached by the initialize method of this object.
        UnavailableException ex = null;

        // create proxy
        handler = createEndpointHandler();

        try {
            proxy = (Proxy) ivProxyCTOR.newInstance(new Object[] { handler });
            handler.ivProxy = proxy;

            // Initialize InvocationHandler to be owner of this
            // proxy object and XAResource and determine if enlistment
            // of a XAResource is needed.
            if (ivEnlistNotNeeded) {
                if (xaResource == null) {
                    // Enlistment is not needed and no XAResource
                    // was passed by RA. So everything is fine.
                    handler.initialize(null, false, majorJCAVersion, minorJCAVersion); // f743-7046
                } else {
                    // Enlistment is not needed, but the RA did pass an
                    // XAResource object.
                    // We need to throw an exception since transaction
                    // recovery setup was
                    // not completed successfully for this RA.
                    ex = mapAndLogTranEnlistmentNotNeeded();
                }
            } else {
                // Transaction recovery is setup for this RA, so
                // use whatever the RA passed as an XAResource.
                handler.initialize(xaResource, recoverableXAResource, majorJCAVersion, minorJCAVersion); // f743-7046
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(BaseMessageEndpointFactory.this, tc, "error invoking proxy" + e.toString());
            }
        }

        return (MessageEndpoint) proxy;
    }

    /**
     * Creates a new instance of a MessageEnpointHandler for use by
     * {@link #createEndpoint(XAResource, long)}.
     * <p>
     * 
     * Provides a mechanism for platform specific extension to
     * MessageEndpointHandler.
     * <p>
     */
    protected MessageEndpointHandler createEndpointHandler() {
        return new MessageEndpointHandler(this, ivRecoveryId, ivRRSTransactional);
    }

    /**
     * This is used to find out whether message deliveries to a message endpoint
     * will be transacted or not. The message delivery preferences must not
     * change during the lifetime of a message endpoint. This information is
     * only a hint and may be useful to perform optimizations on message
     * delivery.
     * 
     * @param method
     *            - description of a target method. This information about the
     *            intended target method allows an application server to find
     *            out whether the target method call will be transacted or not.
     * 
     * @return true, if message endpoint requires transacted message delivery.
     * 
     * @throws NoSuchMethodException
     *             if Method not found for this MessageEndpoint.
     * 
     */
    @Override
    public boolean isDeliveryTransacted(Method method) throws NoSuchMethodException {
          return false;
    }

    /**
     * This is used to find out whether message deliveries to a message endpoint
     * will be transacted or not. The message delivery preferences must not
     * change during the lifetime of a message endpoint. This information is
     * only a hint and may be useful to perform optimizations on message
     * delivery.
     * 
     * @param method
     *            - description of a target method. This information about the
     *            intended target method allows an application server to find
     *            out whether the target method call will be transacted or not.
     * 
     * @return true, if message endpoint requires transacted message delivery.
     * 
     * @throws NoSuchMethodException
     *             if Method not found for this MessageEndpoint.
     * 
     */
    public void setRecoveryID(int recoveryId) throws ResourceException {

        if (ivRecoveryIdKnown) {

            throw new ApplicationServerInternalException("setRecoveryId can only be called once per factory");
        } else {
            ivRecoveryId = recoveryId;
            ivRecoveryIdKnown = true;
        }

    }

    /**
     * Method setTranEnlistmentNotNeeded.
     * <p>
     * This method indicates that the MessageEndpointFactory should NOT enlist
     * an XAResource in a transaction.
     * 
     * @param reason
     *            This is the reason code for why the MessageEndpointFactory
     *            does not need to enlist. Valid reason codes are:
     *            <p>
     *            MessageEndpointFactory.RA_DOES_NOT_SUPPORT_XATRANSACTIONS -
     *            this indicates that the ResourceAdapter has indicated that it
     *            does not support XATransactions.
     *            <p>
     *            MessageEndpointFactory.ERROR_DURING_TRAN_RECOVERY_SETUP - this
     *            indicates that the RALifeCycleManager encounter an error which
     *            prevented it from setting up transaction recovery for this
     *            ResourceAdapter. The MessageEndpointFactory should throw an
     *            exception and log an appropriate message if the RA attempts to
     *            use an XAResource in a transaction. This can't be allowed
     *            since recovery setup has failed.
     * 
     */
    public void setTranEnlistmentNotNeeded(int reason) {

        ivEnlistNotNeeded = true;
        ivEnlistNotNeededReason = reason;

    }

    /**
     * Method mapTranEnlistmentNotNeededToException.
     * <p>
     * This method maps the reason code that was passed to the
     * setTranEnlistmentNotNeeded method to an appropriate UnavailableException
     * method to throw if a RA had called createEndpoint and passed a non-null
     * XAResource object to it. Also, an appropriate error message is written to
     * the activity log file.
     * 
     */
    private UnavailableException mapAndLogTranEnlistmentNotNeeded() {
        UnavailableException ex = null;
        String exceptionMsg = null;

        switch (ivEnlistNotNeededReason) {
        case (RA_DOES_NOT_SUPPORT_XATRANSACTIONS): {
            if (ivEnlistNotNeededMessageLogged == false) {
                // CNTR0087E: Resource adapter {0} is not allowed to pass a non
                // null XAResource to createEndpoint method for MDB {1}.
                // Tr.error(tc, "RA_DOES_NOT_SUPPORT_XATRANSACTIONS_CNTR0087E"
                // , new Object[] { ivRAKey, j2eeName });
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "RA_DOES_NOT_SUPPORT_XATRANSACTIONS for " + ivRAKey + " " + j2eeName);
                }
            }
            exceptionMsg = "Transaction recovery not setup for this RA since RA does not support XA transactions";
            break;
        }

        case (ERROR_DURING_TRAN_RECOVERY_SETUP): {
            if (ivEnlistNotNeededMessageLogged == false) {
                // CNTR0086E: Transaction recovery setup error occurred for
                // resource adapter {0}, MDB {1}.
                // Tr.error(tc, "ERROR_DURING_TRAN_RECOVERY_SETUP_CNTR0086E"
                // , new Object[] { ivRAKey, j2eeName });
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ERROR_DURING_TRAN_RECOVERY_SETUP for " + ivRAKey + " " + j2eeName);
                }
            }
            exceptionMsg = "Error occured during transaction recovery setup for this Resource Adapter";
            break;
        }

        default: {
            if (ivEnlistNotNeededMessageLogged == false) {
                // CNTR0081E: setTranEnlistmentNotNeeded called with an
                // unrecognized reason code of {0}.
                // Tr.error(tc, "REASON_CODE_NOT_RECOGNIZED_CNTR0081E",
                // Integer.valueOf(ivEnlistNotNeededReason));
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "REASON_CODE_NOT_RECOGNIZED");
                }
            }
            exceptionMsg = "Error occured during transaction recovery setup for this Resource Adapter";
            break;
        }
        }

        // Indicate error message is logged to prevent logging next time this
        // method is called
        // for this MessageEndpointFactory instance.
        ivEnlistNotNeededMessageLogged = true;

        // Return exception to the caller to throw.
        ex = new UnavailableException(exceptionMsg);
        return ex;
    }

    /**
     * Set the Resource Adapter key that uniquely identifies the RA. This key is
     * useful as a unique identifier in error messages.
     */
    public void setRAKey(String raKey) {
        ivRAKey = raKey;
    }

    /**
     * Returns true if the endpoint is active.
     * <p>
     * 
     * Provides a mechanism to check if the endpoint has been deactivated out
     * from under the MessageEndpointFactory.
     * <p>
     */
    protected abstract boolean isEndpointActive();

    /**
     * Return j2ee name of batch listener
     */
    protected J2EEName getJ2EEName() {
        return j2eeName;
    }

    /**
     * Returns a unique name for the message endpoint deployment represented by
     * the MessageEndpointFactory. If the message endpoint has been deployed
     * into a clustered application server then this method must return the same
     * name for that message endpoint activation in each application server
     * instance. It is recommended that this name be human-readable since this
     * name may be used by the resource adapter in ways that may be visible to a
     * user or administrator. It is also recommended that this name remain
     * unchanged even in cases when the application server is restarted or the
     * message endpoint re-deployed.
     */
    public String getActivationName() {
        return getJ2EEName().toString();
    }

    /**
     * Return the Class object corresponding to the message endpoint class.
     * The resource adapter may use this to
     * introspect the message endpoint class to discover annotations, interfaces
     * implemented, etc. and modify the behavior of the resource adapter
     * accordingly. A return value of null indicates that the MessageEndpoint
     * doesn't implement the business methods of underlying message endpoint
     * class.
     */
    public Class<?> getEndpointClass() {
        return BatchJmsConstants.JBATCH_JMS_LISTENER_CLASS_NAME.getClass();
    }

	/**
	 * @return the connectionFactory
	 */
	protected ConnectionFactory getConnectionFactory() {
		return batchExecutor.getConnectionFactory();
	}

    
    
}

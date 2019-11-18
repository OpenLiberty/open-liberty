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
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
//import java.security.AccessController;
import java.util.Properties;

import javax.jms.MessageListener;
import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.xa.XAResource;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ejbcontainer.mdb.MDBMessageEndpointFactory;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jbatch.jms.internal.listener.BatchJmsEndpointListener;
import com.ibm.ws.jca.service.EndpointActivationService;
import com.ibm.ws.jca.service.WSMessageEndpointFactory;
//import com.ibm.ws.util.ThreadContextAccessor;

/**
 * This class implements the JCA MessageEndpointFactory interface and is used by
 * JCA component/resource adapter to create/release a MessageEndpoint proxy
 * object for JCA MessageEndpoint Inflows.
 * <p>
 * 
 * Also implement MDBMessageEndpointFactory because current sib has dependency
 * on this interface.
 * TODO remove MDBMessageEndpointFactory when Sib fixes the dependency
 */
public class MessageEndpointFactoryImpl extends BaseMessageEndpointFactory implements WSMessageEndpointFactory, MDBMessageEndpointFactory {

    private static final TraceComponent tc = Tr.register(MessageEndpointFactoryImpl.class);

    /**
     * Returned by endpoint activation service when this endpoint is activated.
     * This key must be passed to the endpoint activation service when this
     * endpoint is deactivated.
     */
    private Object activationSpec;

    /**
     * Flag to represent whether the Resource Adapter using this MEF uses RRS
     * Transactions.
     */
    protected boolean isRRSTransactional = false;

    /*
     * Number of max endpoints to be created.
     */
    private int maxEndpoints = 10;

    /**
     * runtime information about the activation service.
     */
    BatchJmsExecutor.EndpointActivationServiceInfo endpointActivationServiceInfo;

    /**
     * runtime information about the destination.
     */
    BatchJmsExecutor.NamedAdminObjectServiceInfo adminObjectServiceInfo;

    /**
     * True if the runtime has called activateEndpointInternal but has not
     * called deactivateEndpointInternal.
     */
    boolean runtimeActivated;

    /**
     * Use to set class loader of the endpoint listener when create the endpoint
     */
    /*private static final ThreadContextAccessor threadContextAccessor = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());
    */
    public MessageEndpointFactoryImpl(BatchJmsExecutor batchExecutor) throws RemoteException {
        super(batchExecutor);
        initProxy();
    }

    /**
     * Create the proxy object.  Proxy object will be used to create the MessageEndpoint
     * to return to resource adapter.
     * @throws RemoteException
     */
    private void initProxy() throws RemoteException {
        // Determine the Class object for the Proxy class.
        // A MessageEndpoint proxy needs to implement both the
        // Message Listener interface and the MessageEndpoint interface per JCA spec.        
        Class<?> proxyClass = createMessageEndpointProxy();

        // Get the java reflection Constructor object for the proxy
        // class and cache it so that it can be used by the createEndpoint
        // method without going through all the overhead every time a new
        // proxy instance is created. The Proxy Constructor takes a single
        // parameter of type InvocationHandler.
        try {
            ivProxyCTOR = proxyClass.getConstructor(new Class[] { InvocationHandler.class });
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(MessageEndpointFactoryImpl.this, tc, "MEF initialization for JmsEndpointListener " + " with messaging listener interface of "
                        + " javax.jmx.MessageListener" + " failed.");
            }

            throw new RemoteException("Unable to get Proxy Constructor Method object", t);
        }
    }

    /**
     * Perform the actual endpoint activation the provided endpoint activation
     * service.
     * <p>
     * 
     * The method is provided for use by the MDBRuntime, to be called after the
     * server has reached the 'started' state and the activation specification
     * is available.
     * <p>
     * 
     * This method relies on the caller for proper synchronization. This method
     * should not be called concurrently or concurrently with
     * deactivateEndpoint. Nor should this method be called while the provided
     * endpoint activation service is being removed.
     * <p>
     * 
     * @param eas
     *            endpoint activation service configured for the message
     *            endpoint
     * @param maxEndpoints
     *            maximum number of concurrently active endpoints
     * @param destinationJndi
     *            Jndi name of the activation spec destination
     * @throws ResourceException
     *             if a failure occurs activating the endpoint
     */
    public void activateEndpointInternal(EndpointActivationService eas, int maxEndpoints, String destinationJndi) throws ResourceException {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        boolean activate;
        Object asInstance = null;
        ResourceException rex = null;
       /* if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(MessageEndpointFactoryImpl.this, tc, "activateEndpointInternal class loader =" + this.getClass().getClassLoader());
        }
        Object origCL = threadContextAccessor.pushContextClassLoader(this.getClass().getClassLoader());*/
        try {
            synchronized (ivProxyCTOR) {
                if (ivState == INACTIVE_STATE) {
                    activate = true;
                    ivState = ACTIVATING_STATE;

                } else if (ivState == ACTIVE_STATE) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "endpoint already active");
                    activate = false;
                } else {
                    activate = false;
                    rex = new ResourceException("can not activate until deactivate completes");
                }
            }

            if (activate) {
                // can not pass in null for activation prop
                Properties actProp = new Properties();
                String authAlias = null;
                asInstance = eas.activateEndpoint(this, actProp, authAlias, destinationJndi, null, null);
                synchronized (ivProxyCTOR) {
                    activationSpec = asInstance;

                    // Set the state and notify waiting threads.
                    ivState = ACTIVE_STATE;
                    ivProxyCTOR.notifyAll();
                }

                setRRSTransactional();
                setMaxEndpoints(maxEndpoints);
            }
        } catch (ResourceException ex) {
            synchronized (ivProxyCTOR) {
                ivState = INACTIVE_STATE;
                activationSpec = null;
                unsetRecoveryID();
            }
            rex = ex;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(MessageEndpointFactoryImpl.this, tc, "error activateEndpointInternal " + ex.toString());
                Exception link1 = (Exception) ex.getCause();
                if (link1 != null) {
                    Tr.debug(MessageEndpointFactoryImpl.this, tc, "error activateEndpointInternal link1 " + link1.toString());
                    Exception link2 = (Exception) link1.getCause();
                    if (link2 != null) {
                        Tr.debug(MessageEndpointFactoryImpl.this, tc, "error activateEndpointInternal link2 " + link2.toString());
                    }
                }

            }
        } catch (Throwable ex) {
            synchronized (ivProxyCTOR) {
                ivState = INACTIVE_STATE;
                activationSpec = null;
                unsetRecoveryID();
            }
            rex = new ResourceException(ex);
        }/* finally {
            threadContextAccessor.popContextClassLoader(origCL);
        }*/
        if (rex != null) {
            throw rex;
        }
    }

    /**
     * If an RA wants to enable RRS Transactions, it should return true for the
     * method getRRSTransactional.
     */
    @FFDCIgnore(NoSuchMethodException.class)
    private void setRRSTransactional() {
        try {
            ivRRSTransactional = (Boolean) activationSpec.getClass().getMethod("getRRSTransactional").invoke(activationSpec);
        } catch (NoSuchMethodException x) {
            ivRRSTransactional = false;
        } catch (Exception x) {
            ivRRSTransactional = x == null; // always false - avoid a FindBugs
                                            // warning by using the value of x
                                            // in some trivial way
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(MessageEndpointFactoryImpl.this, tc, " setRRSTransactional set isRRSTransactional=" + isRRSTransactional);
        }
    }

    /**
     * Perform the actual endpoint deactivation using the provided endpoint
     * activation service.
     * <p>
     * 
     * The method is provided for use by the MDBRuntime, to be called whenever
     * the endpoint needs to be deactivated.
     * <p>
     * 
     * This method relies on the caller for proper synchronization. This method
     * should not be called concurrently or concurrently with activateEndpoint.
     * Nor should this method be called while the provided endpoint activation
     * service is being removed.
     * <p>
     * 
     * @param eas
     *            endpoint activation service configured for the message
     *            endpoint
     * @throws ResourceException
     *             if a failure occurs deactivating the endpoint
     */
    protected void deactivateEndpointInternal(EndpointActivationService eas) throws ResourceException {
        Object deactivationKey;
        ResourceException rex = null;

        synchronized (ivProxyCTOR) {
            if ((ivState == ACTIVE_STATE) || (ivState == DEACTIVATE_PENDING_STATE)) {
                ivState = DEACTIVATING_STATE;
                deactivationKey = activationSpec;
                if (deactivationKey == null) {
                    // This occurs when the endpoint activation service
                    // forcefully
                    // deactivates the endpoint. When this occurs all we need to
                    // do is change state since the endpoint was already
                    // deactivated.
                    ivState = INACTIVE_STATE; // d450478
                }

            } else if (ivState != INACTIVE_STATE) {
                throw new ResourceException("illegal state for deactivate");
            }
        }

        if (eas != null && isEndpointActive()) {
            try {
                eas.deactivateEndpoint(activationSpec, this);
            } catch (ResourceException ex) {
                rex = ex;
            } catch (Throwable ex) {
                rex = new ResourceException(ex);
            }
        }

        synchronized (ivProxyCTOR) {
            // Reset even if the EAS is null or there was a failure, the
            // endpoint is considered
            // deactivated (i.e. only try one time)
            activationSpec = null;
            unsetRecoveryID();
            ivState = INACTIVE_STATE;
        }

        // Tr.info(tc, "MDB_ENDPOINT_DEACTIVATED_CNTR4014I",
        // getJ2EEName().toString());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ENDPOINT_DEACTIVATED " + getJ2EEName().toString());
        }
        if (rex != null) {
            throw rex;
        }
    }

    /**
     * Resets the recovery ID assigned to this instance of a
     * MessageEndpointFactory.
     */
    void unsetRecoveryID() {
        ivRecoveryId = 0;
        ivRecoveryIdKnown = false;
    }

    @Override
    protected boolean isEndpointActive() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "activationSpec : " + activationSpec);
        }
        return activationSpec != null;
    }

    /**
     * Create instance of proxy.
     * Per JCA spec: Note that the endpoint instance
     * supplied by the createEndPoint method call is a proxy which implements
     * the endpoint message listener type and the MessageEndpoint interface and
     * it is not the actual endpoint. This is necessary because the application
     * server may need to intercept the message delivery in order to inject
     * transactions, depending on the actual endpoint preferences, and to
     * perform other checks.
     */
    protected Class<?> createMessageEndpointProxy() {
     
        // Note, make MessageEndpoint interface first in the array since
        // we want its method to be invoked if the method name happens to
        // be duplicated in message listener interface.   JCA 1.5 does not
        // address this problem, but we kind of think option B message
        // delivery is more likely to occur than option A.  So we want to
        // make sure beforeDelivery/afterDelivery methods of MessageEndpoint
        // do not get routed to the listener.
     
        // The JCA specification offers two possibility to intercept during the delivery of a message.
        // With the option A, the JCA container have the responsibility to control the execution of the
        // message delivery using a proxy in front of the message endpoint.
        // With the option B, the resource adapter invokes beforeDelivery / afterDelivery / release methods
        // on the MessageEndpoint interface.

        Class<?> interfaces[] = new Class[] { MessageEndpoint.class, MessageListener.class };
        
        //return Proxy.getProxyClass(Thread.currentThread().getContextClassLoader(), interfaces);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "createMessageEndpointProxy: CLASSLOADER= " + BatchJmsEndpointListener.class.getClassLoader());
        }
        
        return Proxy.getProxyClass(BatchJmsEndpointListener.class.getClassLoader(), interfaces);
    }

    @Trivial
    public void messageEndpointForcefullyDeactivated() {
        throw new UnsupportedOperationException();
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
        MessageEndpointHandler meh = null;
        if (getBatchExecutor().isResourceFactorySet()) {
            meh = new ExtendedMessageEndpointHandler(this, ivRecoveryId, ivRRSTransactional);

        } else {
            meh = super.createEndpointHandler();
        }

        return meh;
    }

    @Override
    public J2EEName getJ2EEName() {
        return super.getJ2EEName();
    }

    /**
     * This is used to create a message endpoint. The message endpoint is
     * expected to implement the correct message listener type.
     * 
     * @param xaResource
     *            - is an optional XAResource instance used by resource adapter
     *            to get transaction notifications when the message delivery is
     *            transacted.
     * @param timeout
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
    public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException {
        return super.createEndpoint(xaResource, 0L);
    }

    /**
     * Set maximum limit for number of endpoint listener
     * 
     * @param maxEndpoints
     */
    private void setMaxEndpoints(int maxEndpoints) {
        this.maxEndpoints = maxEndpoints;
    }

    /**
     * Returns the maximum number of message-driven beans that may be active
     * concurrently.
     * 
     * This method enables the JMS Resource Adapter to match the message
     * endpoint concurrency to the value used for the message-driven bean by the
     * EJB container. Note: The value returned may vary over the life of a
     * message endpoint factory in response to dynamic configuration updates.
     * 
     * TODO: method will be removed when MDBMessageEndpointFactory interface is
     * removed
     */
    @Override
    public int getMaxEndpoints() {
        return maxEndpoints;
    }

    /**
     * This method will be removed when interface MDBMessageEndpointFactory is
     * removed. Currently needed because SIB has dependency on this interface
     */
    public Object getMDBKey() {
        return j2eeName;
    }

    /**
     * Return activation spec id
     */
    public String getActivationSpecId() {
        return getBatchExecutor().getEndpointActivationSpecId();
    }

    /**
     * Return Jndi name of activation spec destination queue
     */
    public String getDestinationId() {
        return getBatchExecutor().getEndpointDestinationQueueJndi();
    }

    /**
     * Indicates what version of JCA specification the RA using this
     * MessageEndpointFactory requires compliance with.
     * 
     * @see com.ibm.ws.j2c.WSMessageEndpointFactory#setJCAVersion(int, int)
     */
    @Override
    public void setJCAVersion(int majorJCAVer, int minorJCAVer) {
        majorJCAVersion = majorJCAVer;
        minorJCAVersion = minorJCAVer;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "MessageEndpointFactoryImpl.setJCAVersionJCA: Version " + majorJCAVersion + "." + minorJCAVersion + " is set");
        }
    }
}

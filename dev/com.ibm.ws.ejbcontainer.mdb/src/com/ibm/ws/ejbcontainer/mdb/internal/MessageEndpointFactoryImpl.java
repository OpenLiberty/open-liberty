/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb.internal;

import java.rmi.RemoteException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.xa.XAResource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBThreadData;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ejbcontainer.mdb.BaseMessageEndpointFactory;
import com.ibm.ws.ejbcontainer.mdb.MDBMessageEndpointFactory;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jca.service.AdminObjectService;
import com.ibm.ws.jca.service.EndpointActivationService;
import com.ibm.ws.jca.service.WSMessageEndpointFactory;
import com.ibm.ws.kernel.launch.service.PauseableComponent;
import com.ibm.ws.kernel.launch.service.PauseableComponentException;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * This class implements the MDB MessageEndpointFactory interface and is used
 * by JCA component/resource adapter to create/release a MessageEndpoint proxy
 * object for JCA MessageEndpoint Inflows. <p>
 *
 * Internally, this object is also a home object for a MessageDrivenBean. In
 * the EJB specification, a MDB does not have a home object. However, it is
 * convenient for current ejb container implementation to make this object a
 * home so that it fits into the current ejb container infrastructure. Also, by
 * making this object a home object, we can ensure the following JCA
 * requirements are met:
 * <ul>
 * <li>A separate factory is needed for each MDB that implements the same messaging type.
 * <li>A separate factory is needed for each application that uses the same MDB.
 * </ul>
 * <p>
 * Creating a home object as the MessageEndpointFactory ensures above requirements are
 * met and that the J2EEName for the home is a unique key for each MessageEndpointFactory
 * instance.
 */
public class MessageEndpointFactoryImpl extends BaseMessageEndpointFactory implements WSMessageEndpointFactory, MDBMessageEndpointFactory, PauseableComponent {

    private static final long serialVersionUID = 5888307461965940506L;
    private static final TraceComponent tc = Tr.register(MessageEndpointFactoryImpl.class);
    private static final ThreadContextAccessor threadContextAccessor = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());

    /**
     * Returned by endpoint activation service when this endpoint is
     * activated. This key must be passed to the endpoint activation
     * service when this endpoint is deactivated.
     */
    private Object activationSpec;

    private final MDBRuntimeImpl mdbRuntime;
    private String activationSvcId;

    ServiceRegistration<PauseableComponent> registration = null;

    /**
     * MDB runtime information about the activation service.
     */
    MDBRuntimeImpl.EndpointActivationServiceInfo endpointActivationServiceInfo;

    /**
     * MDB runtime information about the destination.
     */
    MDBRuntimeImpl.NamedAdminObjectServiceInfo adminObjectServiceInfo;

    MDBRuntimeImpl.SchemeJndiNameInfo schemeJndiNameInfo;

    /**
     * True if the runtime has called activateEndpointInternal but has not
     * called deactivateEndpointInternal.
     */
    boolean runtimeActivated;

    public MessageEndpointFactoryImpl() throws RemoteException {
        super();
        mdbRuntime = MDBRuntimeImpl.instance();
    }

    @Trivial
    @Override
    public void initialize(EJSContainer ejsContainer,
                           BeanId id,
                           BeanMetaData bmd) throws RemoteException {
        super.initialize(ejsContainer, id, bmd);

        // Get the activation spec name. This will have been obtained from the
        // binding file, or defaulted to "[app/]module/bean" conforming to the
        // java:global rules for binding an EJB.
        activationSvcId = bmd.ivActivationSpecJndiName;

        //Register this instance as a PauseableComponent
        BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        registration = bundleContext.registerService(PauseableComponent.class, this, null);

    }

    @Override
    public void destroy() {
        if (registration != null)
            registration.unregister();
        super.destroy();
    }

    @Override
    public void activateEndpoint() throws ResourceException {
        // Call the MDB runtime to activate the endpoint when appropriate
        // based on activation specification availability and server
        // start status. This call may return with the real activation
        // deferred to a later time.
        try {
            mdbRuntime.activateEndpoint(this);
        } catch (ResourceException rex) {
            // TODO : Add a Tr.warning to explain that a config fix is needed

            // Should never occur before server started is complete since activation
            // is deferred. After the server has started, this is likely in response
            // to a bad config update, in which case the application should be
            // allowed to start so it can begin working once the bad config is
            // corrected and the new dynamic update occurs. This MEF will be
            // in the 'pending' list.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.entry(tc, "MEF.activateEndpoint failed with exception : " + rex);
            }
        }
    }

    /**
     * Perform the actual endpoint activation for the MDB using the provided endpoint
     * activation service. <p>
     *
     * The method is provided for use by the MDBRuntime, to be called after the
     * server has reached the 'started' state and the activation specification
     * is available. <p>
     *
     * This method relies on the caller for proper synchronization. This method
     * should not be called concurrently or concurrently with deactivateEndpoint.
     * Nor should this method be called while the provided endpoint activation
     * service is being removed. <p>
     *
     * @param eas endpoint activation service configured for the message endpoint
     * @param maxEndpoints maximum number of concurrently active endpoints
     * @param adminObjSvc admin object service located by the mdb runtime
     * @throws ResourceException if a failure occurs activating the endpoint
     */
    @Trivial
    @FFDCIgnore({ ResourceException.class, Throwable.class })
    protected void activateEndpointInternal(EndpointActivationService eas, int maxEndpoints, AdminObjectService adminObjSvc) throws ResourceException {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "MEF.activateEndpointInternal for MDB " + beanMetaData.enterpriseBeanName + "(" + eas + ", " + maxEndpoints + ")");

        boolean activate;
        Object asInstance = null;
        ResourceException rex = null;
        EJBThreadData threadData = EJSContainer.getThreadData();
        threadData.pushMetaDataContexts(beanMetaData);
        try {
            synchronized (ivStateLock) {
                if (ivState == INACTIVE_STATE) {
                    activate = true;
                    ivState = ACTIVATING_STATE;
                    ivActivatingThread = Thread.currentThread();
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
                asInstance = eas.activateEndpoint(this,
                                                  beanMetaData.ivActivationConfig,
                                                  beanMetaData.ivActivationSpecAuthAlias,
                                                  beanMetaData.ivMessageDestinationJndiName,
                                                  adminObjSvc,
                                                  adminObjectServiceInfo == null ? null : adminObjectServiceInfo.id);

                // Save key required to deactivate endpoint and change to the ACTIVE
                // state. Note that traditional WAS defers the move to ACTIVE until the application
                // has started to block createEndpoint calls, but that doesn't work well
                // with dynamic configuration updates. Instead of blocking on state,
                // Liberty blocks on checkIfEJBWorkAllowed.
                synchronized (ivStateLock) {
                    activationSpec = asInstance;

                    // Set the state and notify waiting threads.
                    ivState = ACTIVE_STATE;
                    ivStateLock.notifyAll();
                }

                setRRSTransactional();
                setMaxEndpoints(maxEndpoints);

                // TODO : enable this info message in the future when the text can be improved
                //        the endpoint wont truly be active until the jms connection is obtained
                // Tr.info(tc, "MDB_ENDPOINT_ACTIVATED_CNTR4013I", beanMetaData.enterpriseBeanName);
            }
        } catch (ResourceException ex) {
            synchronized (ivStateLock) {
                ivState = INACTIVE_STATE;
                activationSpec = null;
                unsetRecoveryID();
            }
            rex = ex;
        } catch (Throwable ex) {
            synchronized (ivStateLock) {
                ivState = INACTIVE_STATE;
                activationSpec = null;
                unsetRecoveryID();
            }
            rex = new ResourceException(ex);
        } finally {
            threadData.popMetaDataContexts();
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "MEF.activateEndpointInternal for MDB " + beanMetaData.enterpriseBeanName, rex);

        if (rex != null) {
            throw rex;
        }
    }

    /**
     * If an RA wants to enable RRS Transactions, it should return true for the method getRRSTransactional.
     */
    @FFDCIgnore(NoSuchMethodException.class)
    private void setRRSTransactional() {
        try {
            ivRRSTransactional = (Boolean) activationSpec.getClass().getMethod("getRRSTransactional").invoke(activationSpec);
        } catch (NoSuchMethodException x) {
            ivRRSTransactional = false;
        } catch (Exception x) {
            ivRRSTransactional = x == null; // always false - avoid a FindBugs warning by using the value of x in some trivial way
        }
    }

    @Override
    public void deactivateEndpoint() throws ResourceException {
        // Call the MDB runtime to deactivate and remove the endpoint,
        // which will also change the state to the inactive state.
        mdbRuntime.deactivateEndpoint(this);
    }

    /**
     * Perform the actual endpoint deactivation for the MDB using the provided endpoint
     * activation service. <p>
     *
     * The method is provided for use by the MDBRuntime, to be called whenever
     * the endpoint needs to be deactivated. <p>
     *
     * This method relies on the caller for proper synchronization. This method
     * should not be called concurrently or concurrently with activateEndpoint.
     * Nor should this method be called while the provided endpoint activation
     * service is being removed. <p>
     *
     * @param eas endpoint activation service configured for the message endpoint
     * @throws ResourceException if a failure occurs deactivating the endpoint
     */
    @Trivial
    protected void deactivateEndpointInternal(EndpointActivationService eas) throws ResourceException {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "MEF.deactivateEndpointInternal for MDB " + beanMetaData.enterpriseBeanName + "(" + eas + ")");

        Object deactivationKey;
        ResourceException rex = null;

        synchronized (ivStateLock) {
            if ((ivState == ACTIVE_STATE) || (ivState == DEACTIVATE_PENDING_STATE)) {
                ivState = DEACTIVATING_STATE;
                deactivationKey = activationSpec;
                if (deactivationKey == null) {
                    // This occurs when the endpoint activation service forcefully
                    // deactivates the endpoint. When this occurs all we need to
                    // do is change state since the endpoint was already deactivated.
                    ivState = INACTIVE_STATE; //d450478
                }

            } else if (ivState != INACTIVE_STATE) {
                throw new ResourceException("illegal state for deactivate");
            }
        }
        if (eas != null && isEndpointActive()) {
            EJBThreadData threadData = EJSContainer.getThreadData();
            threadData.pushMetaDataContexts(beanMetaData);
            try {
                eas.deactivateEndpoint(activationSpec, this);
            } catch (ResourceException ex) {
                rex = ex;
            } catch (Throwable ex) {
                rex = new ResourceException(ex);
            }
            threadData.popMetaDataContexts();
        }

        synchronized (ivStateLock) {
            // Reset even if the EAS is null or there was a failure, the endpoint is considered
            // deactivated (i.e. only try one time)
            activationSpec = null;
            unsetRecoveryID();
            ivState = INACTIVE_STATE;
        }

        Tr.info(tc, "MDB_ENDPOINT_DEACTIVATED_CNTR4014I", beanMetaData.enterpriseBeanName);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "MEF.deactivateEndpointInternal for MDB " + beanMetaData.enterpriseBeanName);

        if (rex != null) {
            throw rex;
        }
    }

    /**
     * Resets the recovery ID assigned to this instance of a MessageEndpointFactory.
     */
    void unsetRecoveryID() {
        ivRecoveryId = 0;
        ivRecoveryIdKnown = false;
    }

    @Trivial
    @Override
    protected boolean isEndpointActive() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "activationSpec : " + activationSpec);
        }
        return activationSpec != null;
    }

    @Override
    public void applicationStarted(String appName) {
        // Nothing to do here on Liberty. Endpoint activation occurs as
        // soon as the server has started, and createEndpoint is blocked
        // by checkIfEJBWorkAllowed
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException {
        // Traditional WAS blocked by not moving the endpoint to ACTIVE until the application
        // had started, but that doesn't work, so well with dynamic configuration
        // updates, so instead Liberty blocks here on application started, which
        // is done with checkIfEJBWorkAllowed in homeEnabled.
        try {
            homeEnabled();
        } catch (Throwable ex) {
            throw new UnavailableException(ex);
        }
        return super.createEndpoint(xaResource, timeout);
    }

    /**
     * Indicates what version of JCA specification the RA using
     * this MessageEndpointFactory requires compliance with.
     *
     * @see com.ibm.ws.jca.service.WSMessageEndpointFactory#setJCAVersion(int, int)
     */
    @Override
    public void setJCAVersion(int majorJCAVer, int minorJCAVer) {
        majorJCAVersion = majorJCAVer;
        minorJCAVersion = minorJCAVer;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "MessageEndpointFactoryImpl.setJCAVersionJCA: Version " + majorJCAVersion + "." + minorJCAVersion + " is set");
        }
    }

    @Trivial
    @Override
    public void messageEndpointForcefullyDeactivated() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void applicationStopping(String appName) {
        // Unlike traditional WAS, a message endpoint may be deactivated by either stopping the
        // application or removing the activation spec. If the activation spec has
        // already been removed and deactivated this endpoint, then skip the parent
        // processing as it will just log FFDC, since that is an error on traditional WAS.
        synchronized (ivStateLock) {
            if (ivState != DEACTIVATING_STATE) {
                super.applicationStopping(appName);
            }
        }
    }

    @Trivial
    @Override
    public int getMaxEndpoints() {
        return ivMaxCreation;
    }

    @Trivial
    @Override
    public Object getMDBKey() {
        return beanMetaData.j2eeName;
    }

    @Trivial
    @Override
    public String getActivationSpecId() {
        return activationSvcId;
    }

    @Trivial
    public String getDestinationId() {
        return beanMetaData.ivMessageDestinationJndiName;
    }

    //PausableComponent Methods

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.launch.service.PauseableComponent#getExtendedInfo()
     */
    @Override
    public HashMap<String, String> getExtendedInfo() {
        LinkedHashMap<String, String> info = new LinkedHashMap<String, String>();
        if (activationSvcId != null)
            info.put("activationSpec", activationSvcId);
        return info;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.launch.service.PauseableComponent#getName()
     */
    @Override
    public String getName() {
        return getActivationName();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.launch.service.PauseableComponent#isPaused()
     */
    @Override
    public boolean isPaused() {
        return !isEndpointActive();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.launch.service.PauseableComponent#pause()
     */
    @Override
    public void pause() throws PauseableComponentException {
        try {
            if (ivState == ACTIVE_STATE) {
                deactivateEndpoint();
            } else if (ivState == INACTIVE_STATE) {
                Tr.info(tc, "MDB_ENDPOINT_ALREADY_INACTIVE_CNTR4117I", beanMetaData.enterpriseBeanName, beanMetaData.getModuleMetaData().getName(),
                        beanMetaData.getModuleMetaData().getApplicationMetaData().getName());
            } else {
                throw new PauseableComponentException(Tr.formatMessage(tc, "MDB_ENDPOINT_DID_NOT_PAUSE_CNTR4119W", beanMetaData.enterpriseBeanName,
                                                                       beanMetaData.getModuleMetaData().getName(),
                                                                       beanMetaData.getModuleMetaData().getApplicationMetaData().getName()));
            }
        } catch (ResourceException re) {
            throw new PauseableComponentException(re);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.launch.service.PauseableComponent#resume()
     */
    @Override
    public void resume() throws PauseableComponentException {
        try {
            if (ivState == INACTIVE_STATE) {
                activateEndpoint();
            } else if (ivState == ACTIVE_STATE) {
                Tr.info(tc, "MDB_ENDPOINT_ALREADY_ACTIVE_CNTR4118I", beanMetaData.enterpriseBeanName, beanMetaData.getModuleMetaData().getName(),
                        beanMetaData.getModuleMetaData().getApplicationMetaData().getName());
            } else {
                throw new PauseableComponentException(Tr.formatMessage(tc, "MDB_ENDPOINT_DID_NOT_RESUME_CNTR4120W", beanMetaData.enterpriseBeanName,
                                                                       beanMetaData.getModuleMetaData().getName(),
                                                                       beanMetaData.getModuleMetaData().getApplicationMetaData().getName()));
            }
        } catch (ResourceException re) {
            throw new PauseableComponentException(re);
        }

    }
}

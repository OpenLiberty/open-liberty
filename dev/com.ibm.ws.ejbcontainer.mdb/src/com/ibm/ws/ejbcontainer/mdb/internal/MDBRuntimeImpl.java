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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.BeanOFactory;
import com.ibm.ejs.container.BeanOFactory.BeanOFactoryType;
import com.ibm.ejs.container.MessageEndpointCollaborator;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ejbcontainer.mdb.BMMessageDrivenBeanOFactory;
import com.ibm.ws.ejbcontainer.mdb.CMMessageDrivenBeanOFactory;
import com.ibm.ws.ejbcontainer.osgi.EJBContainer;
import com.ibm.ws.ejbcontainer.osgi.EJBRuntimeVersion;
import com.ibm.ws.ejbcontainer.osgi.MDBRuntime;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.injectionengine.osgi.util.JNDIHelper;
import com.ibm.ws.jca.service.AdminObjectService;
import com.ibm.ws.jca.service.EndpointActivationService;
import com.ibm.ws.kernel.feature.ServerStartedPhase2;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.tx.rrs.RRSXAResourceFactory;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

@Component(name = "com.ibm.ws.ejbcontainer.osgi.MDBRuntime", service = { MDBRuntime.class },
           configurationPolicy = org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class MDBRuntimeImpl implements MDBRuntime, ApplicationStateListener {
    private static final TraceComponent tc = Tr.register(MDBRuntimeImpl.class);

    static final String REFERENCE_ENDPOINT_ACTIVATION_SERVICES = "endpointActivationServices";
    static final String REFERENCE_ADMIN_OBJECT_SERVICES = "adminObjectServices";

    private ComponentContext context;

    private final AtomicServiceReference<EJBContainer> ejbContainerSR = new AtomicServiceReference<EJBContainer>("ejbContainer");
    // Used to store a temp MessageEndpoint ClassLoader on the MMD
    MetaDataSlot moduleMetaDataSlot;

    private final AtomicServiceReference<RRSXAResourceFactory> rrsXAResFactorySvcRef = new AtomicServiceReference<RRSXAResourceFactory>("rRSXAResourceFactory");

    // id unique per activation spec configuration
    private static final String ACT_SPEC_CFG_ID = "id";

    /**
     * Use the AtomicServiceReference class to the MessageEndpointCollaborator
     */
    private final AtomicServiceReference<MessageEndpointCollaborator> messageEndpointCollaboratorRef = new AtomicServiceReference<MessageEndpointCollaborator>("messageEndpointCollaborator");

    /**
     * ActivationSpec metatype constant for maxEndpoints
     */
    private static final String ACT_SPEC_CFG_MAX_ENDPOINTS = "maxEndpoints";

    /**
     * ActivationSpec metatype constant for autoStart
     */
    private static final String ACT_SPEC_CFG_AUTOSTART = "autoStart";

    private static final String ADMIN_OBJECT_CFG_ID = "id";
    private static final String ADMIN_OBJECT_CFG_JNDI_NAME = "jndiName";

    private static final Version DEFAULT_VERSION = EJBRuntimeVersion.VERSION_3_1;
    private static final String REFERENCE_RUNTIME_VERSION = "ejbRuntimeVersion";
    private Version runtimeVersion = DEFAULT_VERSION;
    private final AtomicServiceReference<EJBRuntimeVersion> runtimeVersionRef = new AtomicServiceReference<EJBRuntimeVersion>(REFERENCE_RUNTIME_VERSION);

    /**
     * A collection of MessageEndpointFactoryImpl that reference an object.
     */
    private static class ReferencingEndpointFactorySet {
        /**
         * The id of the object being referenced.
         */
        final String id;

        /**
         * The endpoints that reference this object.
         */
        final Set<MessageEndpointFactoryImpl> endpointFactories = new LinkedHashSet<MessageEndpointFactoryImpl>();

        ReferencingEndpointFactorySet(String id) {
            this.id = id;
        }

        void addReferencingEndpoint(MessageEndpointFactoryImpl mef) {
            endpointFactories.add(mef);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, id + " now has " + endpointFactories.size() + " endpoints");
            }
        }

        void removeReferencingEndpoint(MessageEndpointFactoryImpl mef) {
            endpointFactories.remove(mef);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, id + " now has " + endpointFactories.size() + " endpoints");
            }
        }
    }

    /**
     * Information about a set of AdminObjectService with the same id or jndiName.
     */
    static class NamedAdminObjectServiceInfo extends ReferencingEndpointFactorySet {
        /**
         * The services that share this id.
         */
        private final ConcurrentServiceReferenceSet<AdminObjectService> idServices;

        private final ConcurrentServiceReferenceSet<AdminObjectService> jndiNameServices;

        ServiceReference<AdminObjectService> serviceRef;

        NamedAdminObjectServiceInfo(String id,
                                    ConcurrentServiceReferenceSet<AdminObjectService> idServices,
                                    ConcurrentServiceReferenceSet<AdminObjectService> jndiNameServices) {
            super(id);
            this.idServices = idServices;
            this.jndiNameServices = jndiNameServices;
        }

        ConcurrentServiceReferenceSet<AdminObjectService> getServices(boolean jndiName) {
            return jndiName ? jndiNameServices : idServices;
        }

        ServiceReference<AdminObjectService> updateServiceRef() {
            // For consistency with JCA, either the "id" or "jndiName" of
            // the admin object can be used, and "id" has precedence.
            serviceRef = idServices.getHighestRankedReference();
            if (serviceRef == null) {
                serviceRef = jndiNameServices.getHighestRankedReference();
            }
            return serviceRef;
        }
    }

    /**
     * Information about a JNDI lookup we have seen on an endpoint.
     */
    static class SchemeJndiNameInfo extends ReferencingEndpointFactorySet {
        private final String jndiName;
        private Object lookupResult;
        private final BeanMetaData bmd;

        SchemeJndiNameInfo(String name, BeanMetaData bmd) {
            super(name);
            jndiName = name;
            this.bmd = bmd;
        }

        // Occasionally the lookup will fail because the objects haven't come up yet, and cause
        // a NamingException. Because we continually check the lookups throughout the endpoint lifecycle,
        // this exception is safe to ignore.
        @FFDCIgnore(NamingException.class)
        void updateLookupResult() {
            ComponentMetaDataAccessorImpl cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
            try {
                cmdai.beginContext(bmd);
                lookupResult = new InitialContext().lookup(jndiName);
            } catch (NamingException e) {
                lookupResult = null;
            } finally {
                cmdai.endContext();
            }

        }

        boolean lookupSucceeded() {
            return (lookupResult != null);
        }

        String getJndiName() {
            return jndiName;
        }

        BeanMetaData getBMD() {
            return bmd;
        }
    }

    /**
     * The set of AdminObjectService keyed by their id and jndiName. If an
     * AdminObjectService has both an id and a jndiName that are different, it
     * will be added to the set twice. Protected by "this" lock.
     */
    private final Map<String, NamedAdminObjectServiceInfo> adminObjectServices = new HashMap<String, NamedAdminObjectServiceInfo>();

    /**
     * The set of lookups specified on endpoints that we have seen, and their result.
     * This list will be used whenever new adminObjectServices are added or applications start
     * in order to check if any more lookups will resolve at that point.
     */
    private final Map<J2EEName, SchemeJndiNameInfo> schemeJndiNames = new HashMap<J2EEName, SchemeJndiNameInfo>();

    /**
     * Information about the highest ranked EndpointActivationService with a
     * particular id.
     */
    class EndpointActivationServiceInfo extends ReferencingEndpointFactorySet {
        /**
         * The highest ranked service.
         */
        private ServiceReference<EndpointActivationService> serviceRef;

        /**
         * The service that is lazily initialized from {@link #serviceRef}.
         */
        private EndpointActivationService service;

        /**
         * Value of the maxEndpoints property that is lazily initialized
         * from {@link #serviceRef}.
         */
        private Integer maxEndpoints;

        private Boolean autoStart;

        EndpointActivationServiceInfo(String id) {
            super(id);
        }

        void setReference(ServiceReference<EndpointActivationService> ref) {
            serviceRef = ref;
            service = null;
            maxEndpoints = null;
            autoStart = null;
        }

        EndpointActivationService getService() {
            if (serviceRef == null) {
                return null;
            }
            if (service == null) {
                service = (EndpointActivationService) context.locateService(REFERENCE_ENDPOINT_ACTIVATION_SERVICES, serviceRef);
            }
            return service;
        }

        int getMaxEndpoints() {
            if (maxEndpoints == null) {
                maxEndpoints = (Integer) serviceRef.getProperty(ACT_SPEC_CFG_MAX_ENDPOINTS);
            }
            return maxEndpoints;
        }

        boolean getAutoStart() {
            if (serviceRef == null) {
                //There is no ref to the endpoint activation service, so we are unable to get the
                //autoStart value.  Return true here to preserve current behavior, regardless of this
                //value the endpoint will not be brought up since the ref is null.
                return true;
            }
            if (autoStart == null) {
                autoStart = (Boolean) serviceRef.getProperty(ACT_SPEC_CFG_AUTOSTART);
            }
            return autoStart;
        }
    }

    /**
     * The EndpointActivationService keyed by their id. Protected by "this" lock.
     */
    private final Map<String, EndpointActivationServiceInfo> endpointActivationServices = new HashMap<String, EndpointActivationServiceInfo>();

    /**
     * True when the server is in the 'started' state.
     */
    private volatile boolean isServerStarted;

    /**
     * All endpoints factories being tracked by the runtime. Protected by "this" lock.
     */
    private final Set<MessageEndpointFactoryImpl> endpointFactories = new LinkedHashSet<MessageEndpointFactoryImpl>();

    //
    // Lazily initialized BeanOFactory instances that are supported
    // in addition to the core types.
    //
    private BeanOFactory ivCMMessageDrivenBeanOFactory;
    private BeanOFactory ivBMMessageDrivenBeanOFactory;

    private static MDBRuntimeImpl instance;

    @Reference(name = REFERENCE_RUNTIME_VERSION,
               service = EJBRuntimeVersion.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected synchronized void setEJBRuntimeVersion(ServiceReference<EJBRuntimeVersion> ref) {
        this.runtimeVersionRef.setReference(ref);
        runtimeVersion = Version.parseVersion((String) ref.getProperty(EJBRuntimeVersion.VERSION));
    }

    protected synchronized void unsetEJBRuntimeVersion(ServiceReference<EJBRuntimeVersion> ref) {
        this.runtimeVersionRef.unsetReference(ref);
        runtimeVersion = DEFAULT_VERSION;
    }

    @Reference(name = "ejbContainer", service = EJBContainer.class)
    protected void setEJBContainer(ServiceReference<EJBContainer> reference) {
        ejbContainerSR.setReference(reference);
    }

    protected void unsetEJBContainer(ServiceReference<EJBContainer> reference) {
        ejbContainerSR.unsetReference(reference);
    }

    /**
     * Sets the MessageEndpointCollaborator reference.
     *
     * @param reference The MessageEndpointCollaborator reference to set.
     */
    @Reference(name = "messageEndpointCollaborator",
               service = MessageEndpointCollaborator.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setMessageEndpointCollaborator(ServiceReference<MessageEndpointCollaborator> reference) {
        messageEndpointCollaboratorRef.setReference(reference);
    }

    /**
     * Unsets the message endpoint collaborator reference.
     *
     * @param reference The MessageEndpointCollaborator reference to unset.
     */
    protected void unsetMessageEndpointCollaborator(ServiceReference<MessageEndpointCollaborator> reference) {
        messageEndpointCollaboratorRef.unsetReference(reference);
    }

    @Trivial
    public static MDBRuntimeImpl instance() {
        return instance;
    }

    @Trivial
    private static void setInstance(MDBRuntimeImpl impl) {
        MDBRuntimeImpl.instance = impl;
    }

    public MDBRuntimeImpl() {
        setInstance(this);
    }

    /**
     * Declarative Services method for setting the RRS XA resource factory service implementation reference.
     *
     * @param ref reference to the service
     */
    @Reference(name = "rRSXAResourceFactory", service = RRSXAResourceFactory.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setRRSXAResourceFactory(ServiceReference<RRSXAResourceFactory> ref) {
        rrsXAResFactorySvcRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the RRS XA resource factory service implementation reference.
     *
     * @param ref reference to the service
     */
    protected void unsetRRSXAResourceFactory(ServiceReference<RRSXAResourceFactory> ref) {
        rrsXAResFactorySvcRef.unsetReference(ref);
    }

    /**
     * Method to get the XAResource corresponding to an ActivationSpec from the RRSXAResourceFactory
     *
     * @param activationSpecId The id of the ActivationSpec
     * @param xid Transaction branch qualifier
     * @return the XAResource
     */
    @Override
    public XAResource getRRSXAResource(String activationSpecId, Xid xid) throws XAResourceNotAvailableException {
        RRSXAResourceFactory factory = rrsXAResFactorySvcRef.getService();
        if (factory == null) {
            return null;
        } else {
            return factory.getTwoPhaseXAResource(xid);
        }

    }

    /**
     * dynamic/optional/multiple. May be called at any time and in any order
     *
     * @param reference reference to AdminObjectService service
     */
    @Reference(name = REFERENCE_ADMIN_OBJECT_SERVICES,
               service = AdminObjectService.class,
               policy = ReferencePolicy.DYNAMIC,
               cardinality = ReferenceCardinality.MULTIPLE)
    protected synchronized void addAdminObjectService(ServiceReference<AdminObjectService> reference) {
        String id = (String) reference.getProperty(ADMIN_OBJECT_CFG_ID);
        if (id != null) {
            String jndiName = (String) reference.getProperty(ADMIN_OBJECT_CFG_JNDI_NAME);

            addAdminObjectService(reference, id, false);
            if (jndiName != null && !jndiName.equals(id)) {
                addAdminObjectService(reference, jndiName, true);
            }
        }
    }

    /**
     * Internal method for adding an AdminObjectService with an id.
     *
     * <p>Caller must hold a lock on this object.
     *
     * @param reference the service reference
     * @param id the id or jndiName
     * @param jndiName true if the id is the jndiName
     */
    private void addAdminObjectService(ServiceReference<AdminObjectService> reference, String id, boolean jndiName) {
        NamedAdminObjectServiceInfo aosInfo = createNamedAdminObjectServiceInfo(id);
        aosInfo.getServices(jndiName).addReference(reference);

        // If the highest reference after updating is the one we just added,
        // then we need to deactivate any endpoints using the old admin object
        // and reactive them using the new admin object.
        ServiceReference<AdminObjectService> oldServiceRef = aosInfo.serviceRef;
        if (aosInfo.updateServiceRef().equals(reference)) {
            if (oldServiceRef != null) {
                deactivateEndpoints(aosInfo.endpointFactories);
            }
            activateDeferredEndpoints(aosInfo.endpointFactories);
        }

        // With a new adminObjectService, there is a possibility that lookups which
        // failed earlier could now succeed, allowing endpoint activation.
        updateSchemeJndiNames(true);
    }

    /**
     * Internal method for checking all known lookups and activating/deactivating
     * endpoints as needed in response to the results.
     *
     * @param activating true if new services have just come up, false if they are going down
     */
    private void updateSchemeJndiNames(boolean activating) {
        for (Map.Entry<J2EEName, SchemeJndiNameInfo> entry : schemeJndiNames.entrySet()) {
            SchemeJndiNameInfo info = entry.getValue();
            info.updateLookupResult();
            if (activating && info.lookupSucceeded()) {
                activateDeferredEndpoints(info.endpointFactories);
            } else if (!activating && !info.lookupSucceeded()) {
                deactivateEndpoints(info.endpointFactories);
            }
        }
    }

    /**
     * Declarative service method for removing an AdminObjectService.
     */
    protected synchronized void removeAdminObjectService(ServiceReference<AdminObjectService> reference) {
        String id = (String) reference.getProperty(ADMIN_OBJECT_CFG_ID);
        if (id != null) {
            removeAdminObjectService(reference, id, false);

            String jndiName = (String) reference.getProperty(ADMIN_OBJECT_CFG_JNDI_NAME);
            if (jndiName != null && !jndiName.equals(id)) {
                removeAdminObjectService(reference, jndiName, true);
            }
        }
    }

    /**
     * Internal method for removing an AdminObjectService with an id.
     *
     * <p>Caller must hold a lock on this object.
     *
     * @param reference the service reference
     * @param id the id or jndiName
     * @param jndiName true if the id is the jndiName
     */
    // Should be private, but findbugs complains about remove method with SR.
    protected void removeAdminObjectService(ServiceReference<AdminObjectService> reference, String id, boolean jndiName) {
        NamedAdminObjectServiceInfo aosInfo = adminObjectServices.get(id);
        if (aosInfo != null) {
            aosInfo.getServices(jndiName).removeReference(reference);

            // If the highest reference before updating is the one we just
            // removed, then we need to deactivate any endpoints using it.
            if (reference.equals(aosInfo.serviceRef)) {
                deactivateEndpoints(aosInfo.endpointFactories);

                // If there are no more references with this name, then clean
                // up the info object.  Otherwise, reactivate the endpoints
                // using the new admin object.
                if (aosInfo.updateServiceRef() == null) {
                    cleanupAdminObjectServiceInfo(aosInfo);
                } else {
                    activateDeferredEndpoints(aosInfo.endpointFactories);
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "unset reference not the primary reference");
                }
            }
        }

        // With the removal of an adminObjectService, there is a possibility
        // that some lookups could now fail, and endpoints which rely on them
        // need to deactivate until it becomes available again.
        updateSchemeJndiNames(false);

    }

    /**
     * Gets an existing NamedAdminObjectServiceInfo with the specified id, or
     * creates one and inserts it into {@link #adminObjectServices}.
     *
     * <p>Caller must hold a lock on this object.
     */
    private NamedAdminObjectServiceInfo createNamedAdminObjectServiceInfo(String id) {
        NamedAdminObjectServiceInfo aosInfo = adminObjectServices.get(id);
        if (aosInfo == null) {
            aosInfo = new NamedAdminObjectServiceInfo(id, new ConcurrentServiceReferenceSet<AdminObjectService>(REFERENCE_ADMIN_OBJECT_SERVICES), new ConcurrentServiceReferenceSet<AdminObjectService>(REFERENCE_ADMIN_OBJECT_SERVICES));
            adminObjectServices.put(id, aosInfo);
        }
        return aosInfo;
    }

    /**
     * Remove the NamedAdminObjectServiceInfo from {@link #adminObjectServices} if
     * it is no longer needed.
     *
     * <p>Caller must hold a lock on this object.
     */
    private void cleanupAdminObjectServiceInfo(NamedAdminObjectServiceInfo aosInfo) {
        if (aosInfo.serviceRef == null && aosInfo.endpointFactories.isEmpty()) {
            endpointActivationServices.remove(aosInfo.id);
        }
    }

    /**
     * dynamic/optional/multiple. May be called at any time and in any order
     *
     * @param reference reference to EndpointActivationService service
     */
    @Reference(name = REFERENCE_ENDPOINT_ACTIVATION_SERVICES,
               service = EndpointActivationService.class,
               policy = ReferencePolicy.DYNAMIC,
               cardinality = ReferenceCardinality.MULTIPLE)
    protected synchronized void addEndPointActivationService(ServiceReference<EndpointActivationService> reference) {
        String activationSvcId = (String) reference.getProperty(ACT_SPEC_CFG_ID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "activationSvcId : " + activationSvcId);
        }

        EndpointActivationServiceInfo easInfo = createEndpointActivationServiceInfo(activationSvcId);

        // Deactivate any endpoints that were using the old service.
        if (easInfo.service != null) {
            deactivateEndpoints(easInfo.endpointFactories);
        }

        // Activate any endpoints with the new service.
        easInfo.setReference(reference);
        activateDeferredEndpoints(easInfo.endpointFactories);
    }

    /**
     * Declarative service method for removing an EndpointActivationService.
     */
    protected synchronized void removeEndPointActivationService(ServiceReference<EndpointActivationService> reference) {
        String activationSvcId = (String) reference.getProperty(ACT_SPEC_CFG_ID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "activationSvcId : " + activationSvcId);
        }

        EndpointActivationServiceInfo easInfo = endpointActivationServices.get(activationSvcId);
        if (easInfo != null) {
            // If the service was being replaced, then the add method would
            // have been called first, and this reference would no longer be
            // set.  If it's still set, then the service is being removed and
            // there is no replacement, so just deactivate all endpoints.
            if (easInfo.serviceRef.equals(reference)) {
                deactivateEndpoints(easInfo.endpointFactories);

                easInfo.setReference(null);
                cleanupEndpointActivationServiceInfo(easInfo);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "unset reference already removed");
                }
            }
        }
    }

    /**
     * Gets an existing EndpointActivationServiceInfo with the specified id, or
     * creates one and inserts it into {@link #endpointActivationServices}.
     *
     * <p>Caller must hold a lock on this object.
     */
    private EndpointActivationServiceInfo createEndpointActivationServiceInfo(String id) {
        EndpointActivationServiceInfo easInfo = endpointActivationServices.get(id);
        if (easInfo == null) {
            easInfo = new EndpointActivationServiceInfo(id);
            endpointActivationServices.put(id, easInfo);
        }
        return easInfo;
    }

    /**
     * Remove the EndpointActivationServiceInfo from {@link #endpointActivationServices} if
     * it is no longer needed.
     *
     * <p>Caller must hold a lock on this object.
     */
    private void cleanupEndpointActivationServiceInfo(EndpointActivationServiceInfo easInfo) {
        if (easInfo.serviceRef == null && easInfo.endpointFactories.isEmpty()) {
            endpointActivationServices.remove(easInfo.id);
        }
    }

    /**
     * Declarative services method that is invoked once the ServerStarted service
     * is available. Only after this method is invoked are the activation
     * specifications activated thereby ensuring that endpoints are activated
     * only after server startup.
     *
     * @param serverStarted The server started instance
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected synchronized void setServerStartedPhase2(ServerStartedPhase2 serverStartedPhase2) {

        isServerStarted = true;

        // Now that the server has started, activate all of the message endpoints
        // for the MDBs that were started prior to server start completing and that
        // are associated with a known activation specification. Any pending
        // endpoints not associated with a known activation specification will
        // remain pending until the activation specification becomes available.
        activateDeferredEndpoints(endpointFactories);
    }

    /**
     * Declarative services method for unsetting the ServerStarted service instance.
     *
     * @param serverStarted The Started service instance
     */
    protected void unsetServerStartedPhase2(ServerStartedPhase2 serverStartedPhase2) {
        // No cleanup is needed since the server has stopped.
        isServerStarted = false;
    }

    protected void activate(ComponentContext cc) {
        context = cc;
        ejbContainerSR.activate(cc);
        rrsXAResFactorySvcRef.activate(cc);
        messageEndpointCollaboratorRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        ejbContainerSR.deactivate(cc);
        rrsXAResFactorySvcRef.deactivate(cc);
        messageEndpointCollaboratorRef.deactivate(cc);
    }

    @Override
    public BeanOFactory getBeanOFactory(BeanOFactoryType type) {
        BeanOFactory factory;
        switch (type) {
            case CM_MESSAGEDRIVEN_BEANO_FACTORY:
                factory = ivCMMessageDrivenBeanOFactory;
                if (factory == null) {
                    factory = new CMMessageDrivenBeanOFactory();
                    ivCMMessageDrivenBeanOFactory = factory;
                }
                break;

            case BM_MESSAGEDRIVEN_BEANO_FACTORY:
                factory = ivBMMessageDrivenBeanOFactory;
                if (factory == null) {
                    factory = new BMMessageDrivenBeanOFactory();
                    ivBMMessageDrivenBeanOFactory = factory;
                }
                break;

            default:
                // Not one of the MDB types
                factory = null;
        }

        return factory;
    }

    @Override
    public Class<?> getMessageEndpointFactoryImplClass(BeanMetaData bmd) throws ClassNotFoundException {
        return MessageEndpointFactoryImpl.class;
    }

    @Override
    public Class<?> getMessageEndpointImplClass(BeanMetaData bmd) throws ClassNotFoundException {
        // null means use JITDeploy.generateMDBProxy(...).
        return null;
    }

    @Override
    public MessageEndpointCollaborator getMessageEndpointCollaborator() {
        return messageEndpointCollaboratorRef.getService();
    }

    // declarative service
    @Reference(name = "metaDataSlotService", service = MetaDataSlotService.class)
    protected void setMetaDataSlotService(MetaDataSlotService slotService) {
        moduleMetaDataSlot = slotService.reserveMetaDataSlot(ModuleMetaData.class);
    }

    // declarative service
    protected void unsetMetaDataSlotService(MetaDataSlotService slotService) {}

    /**
     * Coordinates all of the resources necessary for activation of endpoints.
     * Specifically, an endpoint will be activated when all of the the following
     * occur:
     * <ul>
     * <li> the MessageEndpointFactory has been created
     * <li> the corresponding admin object service is available if a destination
     * was specified by the MDB activation configuration
     * <li> the corresponding endpoint activation service is available
     * <li> the server has reached the 'started' state
     * </ul>
     *
     * When this method is called before all of these have occurred, the
     * activation of this endpoint will be deferred until all of the
     * above have occurred. <p>
     *
     * This method also controls proper synchronization, to insure the necessary
     * state of all the required resources services.<p>
     *
     * @param mef message endpoint factory to activate
     * @throws ResourceException if a failure occurs activating the endpoint
     */
    synchronized void activateEndpoint(MessageEndpointFactoryImpl mef) throws ResourceException {
        mef.endpointActivationServiceInfo = createEndpointActivationServiceInfo(mef.getActivationSpecId());
        if (mef.endpointActivationServiceInfo.getAutoStart() == false && mef.shouldActivate == false) {
            Tr.info(tc, "MDB_ENDPOINT_NOT_ACTIVATED_AUTOSTART_CNTR4116I", mef.getJ2EEName().getComponent(), mef.getJ2EEName().getModule(), mef.getJ2EEName().getApplication(),
                    mef.endpointActivationServiceInfo.id);
            return;
        }
        mef.endpointActivationServiceInfo.addReferencingEndpoint(mef);

        String destId = mef.getDestinationId();

        // Process activation-config-properties destination and destinationLookup
        if (destId == null) {
            // If JEE 1.6, only use destination property; ignore destinationLookup.
            if (runtimeVersion.compareTo(DEFAULT_VERSION) == 0) {
                destId = mef.getBeanMetaData().ivActivationConfig.getProperty("destination");
            } else {
                // If JEE 1.7, look for both destinationLookup and destination; destinationLookup gets priority
                // for being the destination passed to the MEF.
                BeanMetaData bmd = mef.getBeanMetaData();
                String destLookup = bmd.ivActivationConfig.getProperty("destinationLookup");

                // If the JNDI name has a scheme, we need to perform a lookup to ensure we can access
                // the object it points to
                if (destLookup != null && JNDIHelper.hasJNDIScheme(destLookup)) {
                    // Perform the lookup, and save off the lookup and its result so
                    // we can refer back to whether it succeeded or not, and try again later.
                    SchemeJndiNameInfo jndiInfo = new SchemeJndiNameInfo(destLookup, bmd);
                    jndiInfo.updateLookupResult();

                    schemeJndiNames.put(bmd.j2eeName, new SchemeJndiNameInfo(destLookup, bmd));
                    mef.schemeJndiNameInfo = schemeJndiNames.get(bmd.j2eeName);
                    mef.schemeJndiNameInfo.addReferencingEndpoint(mef);

                } else if (destLookup != null) {
                    destId = destLookup;
                    mef.getBeanMetaData().ivMessageDestinationJndiName = destLookup;
                }
                // If destination is the only property of the two specified, then use it
                if (destId == null) {
                    destId = mef.getBeanMetaData().ivActivationConfig.getProperty("destination");
                }
            }
        }

        // Only create an admin object service info if the JNDI name did not have a scheme
        if (destId != null && !JNDIHelper.hasJNDIScheme(destId)) {
            mef.adminObjectServiceInfo = createNamedAdminObjectServiceInfo(destId);
            mef.adminObjectServiceInfo.addReferencingEndpoint(mef);
        }

        endpointFactories.add(mef);

        activateEndpointInternal(mef, true);
    }

    /**
     * Attempt to activate a set of endpoints if all services are available and
     * they are not already activated.
     *
     * @see #activateEndpoint
     */
    private void activateDeferredEndpoints(Set<MessageEndpointFactoryImpl> mefs) {
        for (MessageEndpointFactoryImpl mef : mefs) {
            if (!mef.runtimeActivated) {
                try {
                    if (mef.endpointActivationServiceInfo.getAutoStart() == false && mef.shouldActivate == false) {
                        Tr.info(tc, "MDB_ENDPOINT_NOT_ACTIVATED_AUTOSTART_CNTR4116I", mef.getJ2EEName().getComponent(), mef.getJ2EEName().getModule(),
                                mef.getJ2EEName().getApplication(),
                                mef.endpointActivationServiceInfo.id);
                        return;
                    }
                    activateEndpointInternal(mef, false);
                } catch (Throwable ex) {
                    // The endpoint has been placed back in the pending state,
                    // nothing else to do; hopefully the customer will see the
                    // errors in the log and correct the problem, and when the
                    // activation spec comes back up it will trigger another
                    // attempt to activate.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Ignoring unexpected exception : " + ex);
                    }
                }
            }
        }
    }

    /**
     * Attempt to activate an endpoint if all its services are available.
     *
     * @param explicit true if {@link #activateEndpoint} was called
     * @see #activateEndpoint
     */
    private void activateEndpointInternal(MessageEndpointFactoryImpl mef, boolean explicit) throws ResourceException {
        // Wait for the destination if specified by the MDB.  If it is instead
        // specified on the activationSpec, then the EndpointActivationService
        // won't be registered until the destination is available, so we
        // implicitly wait for the destination anyway.  If the destination isn't
        // specified at all, then the activation will fail.

        if (mef.adminObjectServiceInfo != null && mef.adminObjectServiceInfo.serviceRef == null) {
            if (explicit) {
                Tr.warning(tc, "MDB_DESTINATION_NOT_FOUND_CNTR4016W", mef.getJ2EEName().getComponent(), mef.adminObjectServiceInfo.id);
            }
            return;
        }

        if (mef.schemeJndiNameInfo != null && !mef.schemeJndiNameInfo.lookupSucceeded()) {
            if (explicit) {
                Tr.warning(tc, "MDB_DESTINATION_NOT_FOUND_CNTR4016W", mef.getJ2EEName().getComponent(), mef.schemeJndiNameInfo.getJndiName());
            }
        }

        EndpointActivationService eas = mef.endpointActivationServiceInfo.getService();
        if (eas == null) {
            if (explicit) {
                Tr.warning(tc, "MDB_ACTIVATION_SPEC_NOT_FOUND_CNTR4015W", mef.getJ2EEName().getComponent(), mef.endpointActivationServiceInfo.id);
            }
            return;
        }

        if (!isServerStarted) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "server is not started");
            }
            return;
        }

        mef.activateEndpointInternal(eas,
                                     mef.endpointActivationServiceInfo.getMaxEndpoints(),
                                     mef.adminObjectServiceInfo == null ? null : (AdminObjectService) context.locateService("adminObjectServices",
                                                                                                                            mef.adminObjectServiceInfo.serviceRef));
        mef.runtimeActivated = true;
    }

    /**
     * Coordinates all of the resources necessary for removal and deactivation
     * of endpoints. <p>
     *
     * This method controls proper synchronization, to insure the necessary
     * state of all the required resources services. <p>
     *
     * @param mef message endpoint factory to activate
     * @throws ResourceException if a failure occurs deactivating the endpoint
     */
    synchronized void deactivateEndpoint(MessageEndpointFactoryImpl mef) {
        if (mef.adminObjectServiceInfo != null) {
            mef.adminObjectServiceInfo.removeReferencingEndpoint(mef);
            cleanupAdminObjectServiceInfo(mef.adminObjectServiceInfo);
        }

        if (mef.schemeJndiNameInfo != null) {
            mef.schemeJndiNameInfo.removeReferencingEndpoint(mef);
            schemeJndiNames.remove(mef.schemeJndiNameInfo.getBMD().j2eeName);
        }

        mef.endpointActivationServiceInfo.removeReferencingEndpoint(mef);
        cleanupEndpointActivationServiceInfo(mef.endpointActivationServiceInfo);

        endpointFactories.remove(mef);

        if (mef.runtimeActivated) {
            deactivateEndpointInternal(mef);
        }
    }

    /**
     * Deactivates a set of endpoints if they have been activated.
     *
     * @see #activateEndpoint
     */
    private void deactivateEndpoints(Set<MessageEndpointFactoryImpl> mefs) {
        for (MessageEndpointFactoryImpl mef : mefs) {
            if (mef.runtimeActivated) {
                deactivateEndpointInternal(mef);
            }
        }
    }

    /**
     * Deactivates an endpoint that was previously activated by {@link #activateEndpointInternal}.
     */
    private void deactivateEndpointInternal(MessageEndpointFactoryImpl mef) {
        mef.runtimeActivated = false;

        EndpointActivationService eas = mef.endpointActivationServiceInfo.getService();
        try {
            mef.deactivateEndpointInternal(eas);
        } catch (Throwable ex) {
            // The endpoint has been placed back in the pending state,
            // nothing else to do; either the server is going down, or
            // the activation spec went down; hopefully the endpoint
            // will activate if the activation spec comes back up.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Ignoring unexpected exception : " + ex);
            }
        }
    }

    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {}

    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        updateSchemeJndiNames(true);
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        updateSchemeJndiNames(false);
    }

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {}
}

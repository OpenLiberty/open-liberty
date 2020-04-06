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

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.jms.ConnectionFactory;
import javax.resource.ResourceException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jbatch.jms.internal.BatchJmsConstants;
import com.ibm.ws.jbatch.jms.internal.BatchOperationGroup;
import com.ibm.ws.jca.service.AdminObjectService;
import com.ibm.ws.jca.service.EndpointActivationService;
import com.ibm.ws.kernel.feature.ServerStartedPhase2;
import com.ibm.ws.tx.rrs.RRSXAResourceFactory;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

/*
 * Start this component right away because it needs to create MEF
 * The main purpose of this component is on serverStarted event,
 * if there is activation spec configured in the server.xml, set up the MessageEndpoitFactory
 * for jbatch.
 * 
 * TODO: add required dep on BatchJmsDispatcher to ensure the dispatcher/connection factory
 *       is available (needed for multi-jvm partitions)
 */
@Component(configurationPid = "com.ibm.ws.jbatch.jms.executor", configurationPolicy = ConfigurationPolicy.REQUIRE, service = {}, property = { "service.vendor=IBM" })
public class BatchJmsExecutor {

    private static final TraceComponent tc = Tr.register(BatchJmsExecutor.class, "wsbatch", "com.ibm.ws.jbatch.jms.internal.resources.BatchJmsMessages");
    
    static final String REFERENCE_ENDPOINT_ACTIVATION_SERVICES = "JmsActivationSpec";
    static final String REFERENCE_ADMIN_OBJECT_SERVICES = "JmsQueue";
    static final String OPERATION_GROUP = "operationGroup";

    private ComponentContext cContext = null;

    private final AtomicServiceReference<RRSXAResourceFactory> rrsXAResFactorySvcRef = new AtomicServiceReference<RRSXAResourceFactory>("rRSXAResourceFactory");

    /**
     * Connection factory for dispatch queue
     */
    private ConnectionFactory jmsCF = null;

    /**
     * For creating jms dispatcher connnection factory
     */
    private ResourceFactory jmsConnectionFactory;
    
    /**
     * Resource configuration factory used to create a resource info object.
     */
    private ResourceConfigFactory resourceConfigFactory;
    
    private BatchOperationGroup batchOperationGroup;
        
    private WSJobRepository jobRepo;
 
    @Reference(service = ResourceConfigFactory.class)
    protected void setResourceConfigFactory(ResourceConfigFactory svc) {
        resourceConfigFactory = svc;
    }

    @Reference(target = "(id=unbound)", cardinality=ReferenceCardinality.OPTIONAL, policy=ReferencePolicy.STATIC, policyOption=ReferencePolicyOption.GREEDY )
    protected void setJMSConnectionFactory(ResourceFactory factory, Map<String, String> serviceProps) {
        jmsConnectionFactory = factory;
    }

    public BatchOperationGroup getBatchOperationGroup(){
        return batchOperationGroup;
    }
    
    @Reference
    protected void setWSJobRepository(WSJobRepository jobRepository){
        jobRepo = jobRepository;
    }
    
    public WSJobRepository getWSJobRepository(){
        return jobRepo;
    }
    
    protected void unsetJMSConnectionFactory(ResourceFactory svc) {
        if (svc == jmsConnectionFactory) {
            jmsConnectionFactory = null;
        }
    }

    protected void unsetResourceConfigFactory(ResourceConfigFactory svc) {
        if (svc == resourceConfigFactory) {
            resourceConfigFactory = null;
        }
    }
    
    /**
     * id unique per activation spec configuration
     */
    private static final String ACT_SPEC_CFG_ID = "id";

    /**
     * ActivationSpec metatype constant for maxEndpoints
     */
    private static final String ACT_SPEC_CFG_MAX_ENDPOINTS = "maxEndpoints";

    /**
     * id unique per jms queue configuration
     */
    private static final String ADMIN_OBJECT_CFG_ID = "id";

    /**
     * Queue metatype constant for jndiName
     */
    private static final String ADMIN_OBJECT_CFG_JNDI_NAME = "jndiName";

    /*
     * Since our listener is a part of the feature, there is no J2EE application
     * But since jca needs this, create an artificial one.
     */
    private J2EENameFactory j2eeNameFactory = null;
    private J2EEName j2eeName = null;

    /**
     * Configuration value from server.xml for activation spec
     */
    private String endpointActivationSpecId = null;

    /**
     * Configuration value from server.xml for destination queue
     */
    private String endpointDestinationQueueJndi = null;

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
        Set<MessageEndpointFactoryImpl> endpointFactories = new LinkedHashSet<MessageEndpointFactoryImpl>();

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
     * Information about a set of AdminObjectService with the same id or
     * jndiName.
     */
    static class NamedAdminObjectServiceInfo extends ReferencingEndpointFactorySet {

        @Override
        public String toString() {
            return "NamedAdminObjectServiceInfo [idServices=" + idServices + ", jndiNameServices=" + jndiNameServices + ", serviceRef=" + serviceRef + ", id=" + id
                    + ", endpointFactories=" + endpointFactories + "]";
        }

        /**
         * The services that share this id.
         */
        private final ConcurrentServiceReferenceSet<AdminObjectService> idServices;

        private final ConcurrentServiceReferenceSet<AdminObjectService> jndiNameServices;

        ServiceReference<AdminObjectService> serviceRef;

        NamedAdminObjectServiceInfo(String id, ConcurrentServiceReferenceSet<AdminObjectService> idServices, ConcurrentServiceReferenceSet<AdminObjectService> jndiNameServices) {
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
     * The set of AdminObjectService keyed by their id and jndiName. If an
     * AdminObjectService has both an id and a jndiName that are different, it
     * will be added to the set twice.
     */
    private final Map<String, NamedAdminObjectServiceInfo> adminObjectServices = new HashMap<String, NamedAdminObjectServiceInfo>();

    /**
     * Information about the highest ranked EndpointActivationService with a
     * particular id.
     */
    class EndpointActivationServiceInfo extends ReferencingEndpointFactorySet {

        @Override
        public String toString() {
            return "EndpointActivationServiceInfo [serviceRef=" + serviceRef + ", service=" + service + ", maxEndpoints=" + maxEndpoints + ", id=" + id + ", endpointFactories="
                    + endpointFactories + "]";
        }

        /**
         * The highest ranked service.
         */
        private ServiceReference<EndpointActivationService> serviceRef;

        /**
         * The service that is lazily initialized from {@link #serviceRef}.
         */
        private EndpointActivationService service;

        /**
         * Value of the maxEndpoints property that is lazily initialized from
         * {@link #serviceRef}.
         */
        private Integer maxEndpoints;

        EndpointActivationServiceInfo(String id) {
            super(id);
        }

        void setReference(ServiceReference<EndpointActivationService> ref) {
            serviceRef = ref;
            service = null;
            maxEndpoints = null;
        }

        EndpointActivationService getService() {
            if (serviceRef == null) {
                return null;
            }
            if (service == null) {
                service = (EndpointActivationService) cContext.locateService(REFERENCE_ENDPOINT_ACTIVATION_SERVICES, serviceRef);
            }
            return service;
        }

        int getMaxEndpoints() {
            if (maxEndpoints == null) {
                maxEndpoints = (Integer) serviceRef.getProperty(ACT_SPEC_CFG_MAX_ENDPOINTS);
            }
            return maxEndpoints;
        }
    }

    /**
     * The EndpointActivationService keyed by their id.
     */
    private final Map<String, EndpointActivationServiceInfo> endpointActivationServices = new HashMap<String, EndpointActivationServiceInfo>();

    /**
     * True when the server is in the 'started' state.
     */
    private volatile boolean isServerStarted = false;

    /**
     * 
     * All endpoints factories being tracked by the runtime.
     */
    private final Set<MessageEndpointFactoryImpl> endpointFactories = new LinkedHashSet<MessageEndpointFactoryImpl>();

    /**
     * Declarative Services method for setting the RRS XA resource factory
     * service implementation reference.
     * 
     * @param ref
     *            reference to the service
     */
    @Reference(name = "rRSXAResourceFactory", service = RRSXAResourceFactory.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setRRSXAResourceFactory(ServiceReference<RRSXAResourceFactory> ref) {
        rrsXAResFactorySvcRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the RRS XA resource factory
     * service implementation reference.
     * 
     * @param ref
     *            reference to the service
     */
    protected void unsetRRSXAResourceFactory(ServiceReference<RRSXAResourceFactory> ref) {
        rrsXAResFactorySvcRef.unsetReference(ref);
    }

    /**
     * Method to get the XAResource corresponding to an ActivationSpec from the
     * RRSXAResourceFactory
     * 
     * @param activationSpecId
     *            The id of the ActivationSpec
     * @param xid
     *            Transaction branch qualifier
     * @return the XAResource
     */
    public XAResource getRRSXAResource(String activationSpecId, Xid xid) throws XAResourceNotAvailableException {
        return rrsXAResFactorySvcRef.getServiceWithException().getTwoPhaseXAResource(xid);
    }

    public boolean isResourceFactorySet() {
        return rrsXAResFactorySvcRef.getService() != null;
    }

    /**
     * Initialize info for jms queue of the activation spec
     * 
     * @param reference
     */
    @Reference(service = AdminObjectService.class, target = "(id=unbound)")
    protected void setJmsQueue(ServiceReference<AdminObjectService> reference) {
        
        String id = (String) reference.getProperty(ADMIN_OBJECT_CFG_ID);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addAdminObjectService: id=" + id);
        }

        if (id != null) {
            String jndiName = (String) reference.getProperty(ADMIN_OBJECT_CFG_JNDI_NAME);
            endpointDestinationQueueJndi = jndiName;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "addAdminObjectService: jndiName=" + jndiName);
            }
            addAdminObjectService(reference, id, false);
            // If an AdminObjectService has both an id and a jndiName that are
            // different, it
            // will be added to the set twice.
            if (jndiName != null && !jndiName.equals(id)) {
                addAdminObjectService(reference, jndiName, true);
            }
        }
    }

    /**
     * Internal method for adding an AdminObjectService with an id.
     * 
     * @param reference
     *            the service reference
     * @param id
     *            the id or jndiName
     * @param jndiName
     *            true if the id is the jndiName
     */
    private synchronized void addAdminObjectService(ServiceReference<AdminObjectService> reference, String id, boolean jndiName) {
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
    }

    /**
     * Declarative service method for removing an AdminObjectService.
     */
    protected synchronized void unsetJmsQueue(ServiceReference<AdminObjectService> reference) {
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
     * @param reference
     *            the service reference
     * @param id
     *            the id or jndiName
     * @param jndiName
     *            true if the id is the jndiName
     */
    // Should be private, but findbugs complains about remove method with SR.
    protected synchronized void removeAdminObjectService(ServiceReference<AdminObjectService> reference, String id, boolean jndiName) {
        NamedAdminObjectServiceInfo aosInfo = adminObjectServices.get(id);
        if (aosInfo != null) {
            aosInfo.getServices(jndiName).removeReference(reference);

            // If the highest reference before updating is the one we just
            // removed, then we need to deactivate any endpoints using it.
            if (reference.equals(aosInfo.serviceRef)) {
                deactivateEndpoints(aosInfo.endpointFactories);

                // If there are no more references with this name, then clean
                // up the info object. Otherwise, reactivate the endpoints
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
    }

    /**
     * Gets an existing NamedAdminObjectServiceInfo with the specified id, or
     * creates one and inserts it into {@link #adminObjectServices}.
     */
    private synchronized NamedAdminObjectServiceInfo createNamedAdminObjectServiceInfo(String id) {
        NamedAdminObjectServiceInfo aosInfo = adminObjectServices.get(id);
        if (aosInfo == null) {
            aosInfo = new NamedAdminObjectServiceInfo(id, new ConcurrentServiceReferenceSet<AdminObjectService>(REFERENCE_ADMIN_OBJECT_SERVICES),
                    new ConcurrentServiceReferenceSet<AdminObjectService>(REFERENCE_ADMIN_OBJECT_SERVICES));
            adminObjectServices.put(id, aosInfo);
        }
        return aosInfo;
    }

    /**
     * Remove the NamedAdminObjectServiceInfo from {@link #adminObjectServices}
     * if it is no longer needed.
     */
    private synchronized void cleanupAdminObjectServiceInfo(NamedAdminObjectServiceInfo aosInfo) {
        if (aosInfo.serviceRef == null && aosInfo.endpointFactories.isEmpty()) {
            endpointActivationServices.remove(aosInfo.id);
        }
    }

    @Reference(service = EndpointActivationService.class, target = "(id=unbound)")
    protected void setJmsActivationSpec(ServiceReference<EndpointActivationService> reference) {
        String activationSvcId = (String) reference.getProperty(ACT_SPEC_CFG_ID);
        setEndpointActivationSpecId(activationSvcId);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(BatchJmsExecutor.this, tc, "activationSvcId : " + activationSvcId);
        }

        if (activationSvcId != null) {

            EndpointActivationServiceInfo easInfo = createEndpointActivationServiceInfo(activationSvcId);

            // Deactivate any endpoints that were using the old service.
            if (easInfo.service != null) {
                deactivateEndpoints(easInfo.endpointFactories);
            }

            // Activate any endpoints with the new service.
            easInfo.setReference(reference);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(BatchJmsExecutor.this, tc, "easInfo=" + easInfo.toString());
            }

            activateDeferredEndpoints(easInfo.endpointFactories);
        }
    }


    /**
     * Declarative service method for removing an EndpointActivationService.
     */
    protected synchronized void unsetJmsActivationSpec(ServiceReference<EndpointActivationService> reference) {
        String activationSvcId = (String) reference.getProperty(ACT_SPEC_CFG_ID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "activationSvcId : " + activationSvcId);
        }

        EndpointActivationServiceInfo easInfo = endpointActivationServices.get(activationSvcId);
        if (easInfo != null) {
            // If the service was being replaced, then the add method would
            // have been called first, and this reference would no longer be
            // set. If it's still set, then the service is being removed and
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
     */
    private synchronized EndpointActivationServiceInfo createEndpointActivationServiceInfo(String id) {
        EndpointActivationServiceInfo easInfo = endpointActivationServices.get(id);
        if (easInfo == null) {
            easInfo = new EndpointActivationServiceInfo(id);
            endpointActivationServices.put(id, easInfo);
        }
        return easInfo;
    }

    /**
     * Remove the EndpointActivationServiceInfo from
     * {@link #endpointActivationServices} if it is no longer needed.
     */
    private synchronized void cleanupEndpointActivationServiceInfo(EndpointActivationServiceInfo easInfo) {
        if (easInfo.serviceRef == null && easInfo.endpointFactories.isEmpty()) {
            endpointActivationServices.remove(easInfo.id);
        }
    }

    /**
     * Declarative services method that is invoked once the ServerStarted
     * service is available. Only after this method is invoked are the
     * activation specifications activated thereby ensuring that endpoints are
     * activated only after server startup.
     * 
     * @param serverStarted
     *            The server started instance
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected synchronized void setServerStartedPhase2(ServerStartedPhase2 serverStartedPhase2) {
        	isServerStarted = true;

	        // batch activation spec should be available already
	        activateDeferredEndpoints(endpointFactories);
    }

    /**
     * Declarative services method for unsetting the ServerStarted service
     * instance.
     * 
     * @param serverStarted
     *            The Started service instance
     */    
    protected void unsetServerStartedPhase2(ServerStartedPhase2 serverStartedPhase2) {
        // No cleanup is needed since the server has stopped.
        isServerStarted = false;
    }


    private void setOperationGroupFromConfig(String[] opGroups) {
    	BatchOperationGroup newBatchOperationGroup = new BatchOperationGroup();
    	if (opGroups != null) {
    		for (String group : opGroups) {
    			newBatchOperationGroup.addGroup(group);
    		}
    	}
    	// Doesn't seem to be any need for any synchronization here.  Up until this assignment is made it seems fine to use
    	// the old value, even though the modified method may have already been called.
    	this.batchOperationGroup = newBatchOperationGroup;
    }

    private boolean deactivated = false;

    /*
     * All services are ready, activate endpoint
     */
    @Activate
    protected void activate(ComponentContext context, Map<String, Object> config) throws Exception {
        
    	if(!FrameworkState.isStopping()) {
        
        	String[] opGroups = (String[])config.get(OPERATION_GROUP);
        	setOperationGroupFromConfig(opGroups);
    
            cContext = context;
            j2eeName = j2eeNameFactory.create(BatchJmsConstants.J2EE_APP_NAME, BatchJmsConstants.J2EE_APP_MODULE, BatchJmsConstants.J2EE_APP_COMPONENT);
    
            rrsXAResFactorySvcRef.activate(context);
                    
            try {
                activateEndpoint();
            } catch (Exception e) {
                Tr.error(tc, "error.batch.executor.activate.failure", new Object[] { e.toString() });
            }
    	}

    }

    @Modified
    protected void modified(ComponentContext context, Map<String, Object> config) throws Exception {
    	String[] opGroups = (String[])config.get(OPERATION_GROUP);
    	setOperationGroupFromConfig(opGroups);
    }

    @Deactivate
    protected void deactivate() {
    	deactivated = true;
    }

    public void setContext(ComponentContext cContext) {
        this.cContext = cContext;
    }

    // Declarative services bind method
    @Reference
    protected void setJEENameFactory(J2EENameFactory svc) {
        j2eeNameFactory = svc;
    }

    /**
     * Coordinates all of the resources necessary for activation of endpoints.
     * Specifically, an endpoint will be activated when all of the the following
     * occur:
     * <ul>
     * <li>the MessageEndpointFactory has been created
     * <li>the corresponding admin object service is available if a destination
     * was specified by the MDB activation configuration
     * <li>the corresponding endpoint activation service is available
     * <li>the server has reached the 'started' state
     * </ul>
     * 
     * When this method is called before all of these have occurred, the
     * activation of this endpoint will be deferred until all of the above have
     * occurred.
     * <p>
     * 
     * This method also controls proper synchronization, to insure the necessary
     * state of all the required resources services.
     * <p>
     * 
     * @param mef
     *            message endpoint factory to activate
     * @throws ResourceException
     *             if a failure occurs activating the endpoint
     */
    synchronized void activateEndpoint(MessageEndpointFactoryImpl mef) throws ResourceException {
        mef.endpointActivationServiceInfo = createEndpointActivationServiceInfo(mef.getActivationSpecId());
        mef.endpointActivationServiceInfo.addReferencingEndpoint(mef);

        String destId = mef.getDestinationId();
        if (destId != null) {
            mef.adminObjectServiceInfo = createNamedAdminObjectServiceInfo(destId);
            mef.adminObjectServiceInfo.addReferencingEndpoint(mef);
        }

        endpointFactories.add(mef);

        activateEndpointInternal(mef, true);
    }

    /**
     * Create MEF and activate the endpoint
     * 
     * @throws RemoteException
     * @throws ResourceException
     */
    public void activateEndpoint() throws RemoteException, ResourceException {
        MessageEndpointFactoryImpl mef = new MessageEndpointFactoryImpl(this);
        mef.setJ2eeName(j2eeName);

        activateEndpoint(mef);
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
                    //activateEndpointInternal(mef, false);
                    activateEndpointInternal(mef, true);
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
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(BatchJmsExecutor.this, tc, mef.toString() + " already activated");
                }
            }
        }
    }

    /**
     * Attempt to activate an endpoint if all its services are available.
     * 
     * @param explicit
     *            true if {@link #activateEndpoint} was called
     * @see #activateEndpoint
     */
    private void activateEndpointInternal(MessageEndpointFactoryImpl mef, boolean explicit) throws ResourceException {
        //
        // destination specified on the activationSpec, then the
        // EndpointActivationService
        // won't be registered until the destination is available, so we
        // implicitly wait for the destination. If the destination isn't
        // specified at all, then the activation will fail.

        if (mef.adminObjectServiceInfo != null && mef.adminObjectServiceInfo.id.equalsIgnoreCase(endpointDestinationQueueJndi) && mef.adminObjectServiceInfo.serviceRef == null) {
            //Tr.warning(tc, "warning.batch.destination.queue.not.found", new Object[] { mef.getJ2EEName().getComponent(), mef.adminObjectServiceInfo.id });
            //make this message debug because it could be possible we are here but doesn't mean the config is bad.
            //Since this process is asynchronous, this method could be call later when the config is avaible.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The message endpoint for the batch" +  mef.getJ2EEName().getComponent() +
                             " JMS listener cannot be activated because the " + mef.adminObjectServiceInfo.id +
                             " destination queue does not exist. The message endpoint will not receive batch JMS messages until the destination queue becomes available.");
            }
            return;
        }

//        if (mef.adminObjectServiceInfo != null && !mef.adminObjectServiceInfo.id.equalsIgnoreCase(endpointDestinationQueueId)) {
//            Tr.warning(tc, "warning.batch.destination.queue.not.found", new Object[]{mef.getJ2EEName().getComponent(), mef.adminObjectServiceInfo.id});


        EndpointActivationService eas = mef.endpointActivationServiceInfo.getService();
        if (eas == null && (mef.endpointActivationServiceInfo.id.endsWith(getEndpointActivationSpecId()))) {
            Tr.warning(tc, "warning.batch.activation.spec.not.found", new Object[] {mef.getJ2EEName().getComponent(), mef.endpointActivationServiceInfo.id});           
            return;
        }
        
        if (!isServerStarted) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "server is not started");
            }
            return;
        }

        /**
         * only activate batch activation spec
         */
        if (mef.endpointActivationServiceInfo.id.endsWith(endpointActivationSpecId)) {
            mef.activateEndpointInternal(eas, mef.endpointActivationServiceInfo.getMaxEndpoints(), mef.getDestinationId());
            mef.runtimeActivated = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Batch activation spec is " + mef.endpointActivationServiceInfo.id + " activated");
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ignore non batch activation spec: " + mef.endpointActivationServiceInfo.id + "mef.endpointActivationServiceInfo.id");
            }
        }
    }

    /**
     * Coordinates all of the resources necessary for removal and deactivation
     * of endpoints.
     * <p>
     * 
     * This method controls proper synchronization, to insure the necessary
     * state of all the required resources services.
     * <p>
     * 
     * @param mef
     *            message endpoint factory to activate
     * @throws ResourceException
     *             if a failure occurs deactivating the endpoint
     */
    synchronized void deactivateEndpoint(MessageEndpointFactoryImpl mef) {
        if (mef.adminObjectServiceInfo != null) {
            mef.adminObjectServiceInfo.removeReferencingEndpoint(mef);
            cleanupAdminObjectServiceInfo(mef.adminObjectServiceInfo);
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
     * Deactivates an endpoint that was previously activated by
     * {@link #activateEndpointInternal}.
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

    /**
     * Getter for activation spec id
     * 
     * @return
     */
    public String getEndpointActivationSpecId() {
        return endpointActivationSpecId;
    }

    /**
     * Setter for activation spec id
     * 
     * @return
     */
    public void setEndpointActivationSpecId(String id) {
        this.endpointActivationSpecId = id;
    }
    /**
     * Getter for queue id
     * 
     * @return
     */
    public String getEndpointDestinationQueueJndi() {
        return endpointDestinationQueueJndi;
    }

    /**
     * 
     * @return the jmsCf
     */
    protected ConnectionFactory getConnectionFactory() {
    	if (deactivated) {
    		throw new IllegalStateException("Executor = " + this + " has been deactivated, but getConnectionFactory() called.");
    	}

    	// Lazily instantiate the 'jmsCF' return value
    	if (jmsCF == null) {
    		if(jmsConnectionFactory != null) {
    			// If the optional reference is set.  Note that, because this is a static, not an optional reference,
    			// we don't have to worry about synchronizing the setting of the reference.  That is, we can assume
    			// it's already been set (or not) at this point, without worrying about another thread coming in.
    			createConnectionFactoryInstance();
            } else {
                 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                     Tr.debug(tc, "jmsConnectionFactory not set, exiting");
                 }
    		}
    	}
        return jmsCF;
    }
    
    /*
     * creates a ConnectionFactory configured in the server.xml
     * 
     * @throws Exception
     */
    private void createConnectionFactoryInstance() {
        try {
            ResourceConfig cfResourceConfig = resourceConfigFactory.createResourceConfig(ConnectionFactory.class.getName());
            cfResourceConfig.setResAuthType(ResourceInfo.AUTH_CONTAINER);
            jmsCF = (ConnectionFactory) jmsConnectionFactory.createResource(cfResourceConfig);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "jmsConnectionFactory = " + jmsConnectionFactory.toString() + ", jmsCf = " + jmsCF.toString());
            }
        } catch (Exception e) {
            Tr.error(tc, "error.batch.executor.jms.create.failure", new Object[] { e });
            throw new IllegalStateException("Problem creating batch executor reply CF", e);
        }
    }

}


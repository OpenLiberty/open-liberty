/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jbatch.jms.internal.listener.impl;

import static com.ibm.websphere.ras.Tr.debug;
import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.jbatch.jms.internal.BatchJmsConstants.J2EE_APP_COMPONENT;
import static com.ibm.ws.jbatch.jms.internal.BatchJmsConstants.J2EE_APP_MODULE;
import static com.ibm.ws.jbatch.jms.internal.BatchJmsConstants.J2EE_APP_NAME;
import static com.ibm.ws.jbatch.jms.internal.listener.impl.JmsExecutor.CONN_FACTORY_REF_NAME;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jbatch.jms.internal.BatchOperationGroup;
import com.ibm.ws.jca.service.AdminObjectService;
import com.ibm.ws.jca.service.EndpointActivationService;
import com.ibm.ws.kernel.feature.ServerStartedPhase2;
import com.ibm.ws.tx.rrs.RRSXAResourceFactory;
import com.ibm.wsspi.application.lifecycle.ApplicationStartBarrier;
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
@Component(configurationPid = "com.ibm.ws.jbatch.jms.executor",
           configurationPolicy = REQUIRE, 
           property = { CONN_FACTORY_REF_NAME+".cardinality.minimum="+Integer.MAX_VALUE, // Prevent this reference from being satisfied before metatype processing
                        "service.vendor=IBM" })
public class JmsExecutor {

    private static final TraceComponent tc = Tr.register(JmsExecutor.class, "wsbatch", "com.ibm.ws.jbatch.jms.internal.resources.BatchJmsMessages");
    
    public static final String ACTIVATION_SPEC_REF_NAME = "JmsActivationSpec";
    public static final String CONN_FACTORY_REF_NAME = "JMSConnectionFactory";
    public static final String JMS_QUEUE_REF_NAME = "JmsQueue";
    public static final String OPERATION_GROUP = "operationGroup";

    private final ComponentContext context;
    private final RRSXAResourceFactory xaResourceFactory;
    /**
     * For creating jms dispatcher connection factory
     */
    private final ResourceFactory resourceFactory;
    /**
     * Resource configuration factory used to create a resource info object.
     */
    private final ResourceConfigFactory resourceConfigFactory;      
    private final WSJobRepository jobRepository;
    private final ServiceReference<AdminObjectService> adminObjectServiceRef;
    
    /**
     * Connection factory for dispatch queue
     */
    private ConnectionFactory jmsConnectionFactory;
    private BatchOperationGroup batchOperationGroup;
    private boolean deactivated = false;
 
    @Activate
    public JmsExecutor(ComponentContext context, Map<String, Object> config,
            // Anonymous References
            @Reference ApplicationStartBarrier requiredButNotUsed,
            @Reference ServerStartedPhase2 requiredButNotUsed2,
            @Reference J2EENameFactory j2eeNameFactory,
            @Reference ResourceConfigFactory resourceConfigFactory,
            @Reference WSJobRepository jobRepository,
            @Reference(cardinality=OPTIONAL, policyOption=GREEDY) RRSXAResourceFactory xaResourceFactory,
            // Named References to tie up with metatype.xml, which replaces the target filters
            @Reference(name=CONN_FACTORY_REF_NAME, target="(id=unbound)", cardinality=OPTIONAL, policyOption=GREEDY) ResourceFactory jmsConnectionFactory,
            @Reference(name=JMS_QUEUE_REF_NAME, target="(id=unbound)") ServiceReference<AdminObjectService> adminObjectServiceRef,
            @Reference(name=ACTIVATION_SPEC_REF_NAME, target="(id=unbound)") ServiceReference<EndpointActivationService> jmsActivationSpecRef) {
            
        this.context = context;
        this.j2eeName = j2eeNameFactory.create(J2EE_APP_NAME, J2EE_APP_MODULE, J2EE_APP_COMPONENT);    
        this.resourceConfigFactory = resourceConfigFactory;
        this.jobRepository = jobRepository;
        this.xaResourceFactory = xaResourceFactory;
        this.resourceFactory = jmsConnectionFactory;
        this.adminObjectServiceRef = adminObjectServiceRef;         
        this.endpointActivationSpecId = (String) jmsActivationSpecRef.getProperty("id");
         
        final String adminId = (String) adminObjectServiceRef.getProperty("id");  
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "JmsExecutor: id=" + adminId);
       
        if (null == adminId) { 
            this.jmsQueueJndi = null;
        } else {
            this.jmsQueueJndi = (String) adminObjectServiceRef.getProperty("jndiName");
        
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "JmsExecutor: jndiName=" + jmsQueueJndi);
            }
            addAdminObjectService(adminObjectServiceRef, adminId, false);
            // If an AdminObjectService has both an id and a jndiName that are
            // different, it will be added to the set twice.
            if (jmsQueueJndi != null && !jmsQueueJndi.equals(adminId)) {
                addAdminObjectService(adminObjectServiceRef, jmsQueueJndi, true);
            }
        }
        
        if(FrameworkState.isStopping()) {
            debug(this, tc, "JmsExecutor" , "Framework stopping");
            return;             
        }
            
        setOperationGroup(config);
    
        
        try {
            activateEndpoint();
        } catch (Exception e) {
            Tr.error(tc, "error.batch.executor.activate.failure", new Object[] { e.toString() });
        }
               
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "activationSvcId : " + endpointActivationSpecId);
        
        if (null != endpointActivationSpecId) {
            EndpointActivationServiceInfo easInfo = createEndpointActivationServiceInfo(endpointActivationSpecId);
        
            // Deactivate any endpoints that were using the old service.
            if (easInfo.service != null) deactivateEndpoints(easInfo.endpointFactories);
        
            // Activate any endpoints with the new service.
            easInfo.setReference(jmsActivationSpecRef);
        
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "easInfo=" + easInfo);
        
            activateDeferredEndpoints(easInfo.endpointFactories);
        }
        
    }
    
    public BatchOperationGroup getBatchOperationGroup(){
        return batchOperationGroup;
    }
    
    public WSJobRepository getWSJobRepository() {
        return jobRepository;
    }
    
    /**
     * ActivationSpec metatype constant for maxEndpoints
     */
    private static final String ACT_SPEC_CFG_MAX_ENDPOINTS = "maxEndpoints";

    /*
     * Since our listener is a part of the batch feature, there is no J2EE application
     * but since JCA needs a name, create an artificial one.
     */
    private final J2EEName j2eeName;

    /**
     * Configuration value from server.xml for activation spec
     */
    private final String endpointActivationSpecId;

    /**
     * Configuration value from server.xml for destination queue
     */
    private final String jmsQueueJndi;

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
                service = (EndpointActivationService) context.locateService(ACTIVATION_SPEC_REF_NAME, serviceRef);
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
     * All endpoints factories being tracked by the runtime.
     */
    private final Set<MessageEndpointFactoryImpl> endpointFactories = new LinkedHashSet<MessageEndpointFactoryImpl>();

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
        return xaResourceFactory.getTwoPhaseXAResource(xid);
    }

    public boolean isResourceFactorySet() {
        return (null != xaResourceFactory);
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
            aosInfo = new NamedAdminObjectServiceInfo(id, new ConcurrentServiceReferenceSet<AdminObjectService>(JMS_QUEUE_REF_NAME),
                    new ConcurrentServiceReferenceSet<AdminObjectService>(JMS_QUEUE_REF_NAME));
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

    /**
     * Declarative service method for removing an EndpointActivationService.
     */
    protected synchronized void unsetJmsActivationSpec(ServiceReference<EndpointActivationService> reference) {
        String activationSvcId = (String) reference.getProperty("id");
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
   
    @Modified
    protected void setOperationGroup(Map<String, Object> config) {
    	String[] opGroups = (String[])config.get(OPERATION_GROUP);
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

    @Deactivate
    protected void deactivate() {     
        final String adminId = (String) adminObjectServiceRef.getProperty("id");
    	if (null != adminId) {
            removeAdminObjectService(adminObjectServiceRef, adminId, false);
            if (jmsQueueJndi != null && !jmsQueueJndi.equals(adminId)) {
                removeAdminObjectService(adminObjectServiceRef, jmsQueueJndi, true);
            }
        }
    	deactivated = true; 
    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "deactivate",  "deactivated adminId="+adminId+" jmsQueueJndi="+jmsQueueJndi);
        }	
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
                    Tr.debug(JmsExecutor.this, tc, mef.toString() + " already activated");
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

        if (mef.adminObjectServiceInfo != null && mef.adminObjectServiceInfo.id.equalsIgnoreCase(jmsQueueJndi) && mef.adminObjectServiceInfo.serviceRef == null) {
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

    public String getEndpointActivationSpecId() {
        return endpointActivationSpecId;
    }

    public String getEndpointDestinationQueueJndi() {
        return jmsQueueJndi;
    }

    protected ConnectionFactory getConnectionFactory() {
    	if (deactivated) {
    		throw new IllegalStateException("Executor = " + this + " has been deactivated, but getConnectionFactory() called.");
    	}

    	// Lazily instantiate the 'jmsConnectionFactory' return value
    	if (jmsConnectionFactory == null) {
    		if(resourceFactory != null) {
    			// If the optional reference is set.  Note that, because this is a static, not an optional reference,
    			// we don't have to worry about synchronizing the setting of the reference.  That is, we can assume
    			// it's already been set (or not) at this point, without worrying about another thread coming in.
    			createConnectionFactoryInstance();
            } else {
                 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                     Tr.debug(tc, "resourceFactory not set, exiting");
                 }
    		}
    	}
        return jmsConnectionFactory;
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
            jmsConnectionFactory = (ConnectionFactory) resourceFactory.createResource(cfResourceConfig);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "resourceFactory = " + resourceFactory.toString() + ", jmsCf = " + jmsConnectionFactory.toString());
            }
        } catch (Exception e) {
            Tr.error(tc, "error.batch.executor.jms.create.failure", new Object[] { e });
            throw new IllegalStateException("Problem creating batch executor reply CF", e);
        }
    }

}


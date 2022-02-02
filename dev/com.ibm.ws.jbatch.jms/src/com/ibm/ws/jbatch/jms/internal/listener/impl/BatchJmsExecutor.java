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
import static com.ibm.websphere.ras.Tr.error;
import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.jbatch.jms.internal.BatchJmsConstants.J2EE_APP_COMPONENT;
import static com.ibm.ws.jbatch.jms.internal.BatchJmsConstants.J2EE_APP_MODULE;
import static com.ibm.ws.jbatch.jms.internal.BatchJmsConstants.J2EE_APP_NAME;
import static com.ibm.ws.jbatch.jms.internal.listener.impl.BatchJmsExecutor.CONN_FACTORY_REF_NAME;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
import com.ibm.ws.jbatch.jms.internal.listener.impl.BatchJmsExecutor.NamedAdminObjectServiceInfo;
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
public class BatchJmsExecutor {

    private static final TraceComponent tc = Tr.register(BatchJmsExecutor.class, "wsbatch", "com.ibm.ws.jbatch.jms.internal.resources.BatchJmsMessages");

    public static final String ACTIVATION_SPEC_REF_NAME = "JmsActivationSpec";
    public static final String CONN_FACTORY_REF_NAME = "JMSConnectionFactory";
    public static final String JMS_QUEUE_REF_NAME = "JmsQueue";
    public static final String OPERATION_GROUP = "operationGroup";

    private final ComponentContext context;
    private final RRSXAResourceFactory xaResourceFactory;
    private final WSJobRepository jobRepository;
    private final ServiceReference<AdminObjectService> adminObjectServiceRef;
    private final ServiceReference<EndpointActivationService> endpointActivationServiceRef;
//    private final NamedAdminObjectServiceInfo idQueueInfo;
//    private final NamedAdminObjectServiceInfo jndiNameQueueInfo;
    

    /**
     * Connection factory for dispatch queue
     */
    private final ConnectionFactory jmsConnectionFactory;
    private BatchOperationGroup batchOperationGroup;
    private boolean deactivated = false;

    private static <T> Predicate<T> not(Predicate<T> predicate) { return predicate.negate(); }

    @Activate
    public BatchJmsExecutor(ComponentContext context, Map<String, Object> config,
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
        this.jobRepository = jobRepository;
        this.xaResourceFactory = xaResourceFactory;
        this.adminObjectServiceRef = adminObjectServiceRef;
        this.endpointActivationServiceRef = jmsActivationSpecRef;
        this.endpointActivationSpecId = (String) jmsActivationSpecRef.getProperty("id");
        this.jmsConnectionFactory = createConnectionFactoryInstance(resourceConfigFactory, jmsConnectionFactory);

        final Optional<String> adminId = Optional.ofNullable(adminObjectServiceRef.getProperty("id")).map(String.class::cast);
        final Optional<String> jndiName = adminId.map(id -> adminObjectServiceRef.getProperty("jndiName")).map(String.class::cast);
        this.jmsQueueJndi = jndiName.orElse(null);
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, String.format("j2eeName=%s actSpecId=%s, q.id=%s q.jndi=%s", j2eeName, endpointActivationSpecId, adminId.orElse(null), jmsQueueJndi));
//
//        
//        this.idQueueInfo = adminId
//                .map(id -> new NamedAdminObjectServiceInfo(id, adminObjectServiceRef, false))
//                .orElse(null);
//        this.jndiNameQueueInfo = jndiName.filter(not(adminId.get()::equals))
//                .map(id -> new NamedAdminObjectServiceInfo(id, adminObjectServiceRef, true))
//                .orElse(null);
//        Stream.of(idQueueInfo, jndiNameQueueInfo)
//                .filter(Objects::nonNull)
//                .map(info -> info.endpointFactories)
//                .forEach(this::activateDeferredEndpoints);

        if(FrameworkState.isStopping()) {
            debug(this, tc, "BatchJmsExecutor" , "Framework stopping");
            return;
        }

        this.batchOperationGroup = createBatchOperationGroup(config);

        try {
            MessageEndpointFactoryImpl mef = new MessageEndpointFactoryImpl(this);
            mef.setJ2eeName(j2eeName);
            activateEndpoint(mef);
        } catch (Exception e) {
            error(tc, "error.batch.executor.activate.failure", e);
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
        private static String ID_FORMAT = "NamedAdminObjectServiceInfo [id=%s service=%s]";
        private static String JNDI_FORMAT = "NamedAdminObjectServiceInfo [jndiName=%s service=%s]";
        private final ServiceReference<AdminObjectService> qRef;
        private final String format;
        NamedAdminObjectServiceInfo(String id, ServiceReference<AdminObjectService> qRef) {
            this(id, qRef, false);
        }
        NamedAdminObjectServiceInfo(String id, ServiceReference<AdminObjectService> qRef, boolean usingJndiName) {
            super(id);
            this.qRef = qRef;
            this.format = usingJndiName ? JNDI_FORMAT : ID_FORMAT;
        }
        @Override
        public String toString() { return String.format(format, id, qRef); }
    }
    

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

        private ServiceReference<EndpointActivationService> serviceRef = BatchJmsExecutor.this.endpointActivationServiceRef;

        /**
         * The service that is lazily initialized from {@link #serviceRef}.
         */
        private final EndpointActivationService service = context.locateService(ACTIVATION_SPEC_REF_NAME, serviceRef);

        /**
         * Value of the maxEndpoints property that is lazily initialized from
         * {@link #serviceRef}.
         */
        private final int maxEndpoints =  (Integer) serviceRef.getProperty(ACT_SPEC_CFG_MAX_ENDPOINTS);

        EndpointActivationServiceInfo(String id) {
            super(id);
        }

        EndpointActivationService getService() {
            return service;
        }

        int getMaxEndpoints() {
            return maxEndpoints;
        }
    }

    /**
     * The EndpointActivationService keyed by their id.
     */
    private final Map<String, EndpointActivationServiceInfo> endpointActivationServices = new ConcurrentHashMap<>();

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

//    /**
//     * Gets an existing NamedAdminObjectServiceInfo with the specified id, or
//     * creates one and inserts it into {@link #adminObjectServices}.
//     */
//    private NamedAdminObjectServiceInfo createNamedAdminObjectServiceInfo(String id) {
////        return adminObjectServices.computeIfAbsent(id, NamedAdminObjectServiceInfo::new);
//        return new NamedAdminObjectServiceInfo(id, null, false);
//    }

//    /**
//     * Remove the NamedAdminObjectServiceInfo from {@link #adminObjectServices}
//     * if it is no longer needed.
//     */
//    private synchronized void cleanupAdminObjectServiceInfo(NamedAdminObjectServiceInfo aosInfo) {
//        if (aosInfo.qRef == null && aosInfo.endpointFactories.isEmpty()) {
//            endpointActivationServices.remove(aosInfo.id);
//        }
//    }

//    /**
//     * Remove the EndpointActivationServiceInfo from
//     * {@link #endpointActivationServices} if it is no longer needed.
//     */
//    private synchronized void cleanupEndpointActivationServiceInfo(EndpointActivationServiceInfo easInfo) {
//        if (easInfo.serviceRef == null && easInfo.endpointFactories.isEmpty()) {
//            endpointActivationServices.remove(easInfo.id);
//        }
//    }

    @Modified
    protected void modified(Map<String, Object> config) {
    	// Doesn't seem to be any need for any synchronization here.  Up until this assignment is made it seems fine to use
        // the old value, even though the modified method may have already been called.
        this.batchOperationGroup = createBatchOperationGroup(config);
    }

    private static BatchOperationGroup createBatchOperationGroup(Map<String, Object> config) {
        return Optional.ofNullable(config.get(OPERATION_GROUP))
                .map(String[].class::cast)
                .map(BatchOperationGroup::new) // pass some strings to the constructor
                .orElseGet(BatchOperationGroup::new); // pass no strings to the constructor
    }

    @Deactivate
    protected void deactivate() {
//        Stream.of(idQueueInfo, jndiNameQueueInfo)
//                .filter(Objects::nonNull)
//                .peek(info -> deactivateEndpoints(info.endpointFactories))
//                .forEach(this::cleanupAdminObjectServiceInfo);

	deactivated = true;

        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "activationSvcId : " + endpointActivationSpecId);

        EndpointActivationServiceInfo easInfo = endpointActivationServices.get(endpointActivationSpecId);
        deactivateEndpoints(easInfo.endpointFactories);
//        easInfo.setReference(null);
//        cleanupEndpointActivationServiceInfo(easInfo);
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
        mef.endpointActivationServiceInfo = endpointActivationServices.computeIfAbsent(mef.getActivationSpecId(), EndpointActivationServiceInfo::new);
        mef.endpointActivationServiceInfo.addReferencingEndpoint(mef);

        Optional.of(mef)
                .map(MessageEndpointFactoryImpl::getDestinationId)
                .map(id -> new NamedAdminObjectServiceInfo(id, adminObjectServiceRef))
//                .map(info -> mef.adminObjectServiceInfo = info)
                .ifPresent(info -> info.addReferencingEndpoint(mef));

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
                    //activateEndpointInternal(mef, false);
                    activateEndpointInternal(mef, true);
                } catch (Throwable ex) {
                    // The endpoint has been placed back in the pending state,
                    // nothing else to do; hopefully the customer will see the
                    // errors in the log and correct the problem, and when the
                    // activation spec comes back up it will trigger another
                    // attempt to activate.
                    if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Ignoring unexpected exception : " + ex);
                }
            } else {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, mef.toString() + " already activated");
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

//        if (mef.adminObjectServiceInfo != null && mef.adminObjectServiceInfo.id.equalsIgnoreCase(jmsQueueJndi) && mef.adminObjectServiceInfo.qRef == null) {
//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, "The message endpoint for the batch" +  mef.getJ2EEName().getComponent() +
//                             " JMS listener cannot be activated because the " + mef.adminObjectServiceInfo.id +
//                             " destination queue does not exist. The message endpoint will not receive batch JMS messages until the destination queue becomes available.");
//            }
//            return;
//        }

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
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Batch activation spec is " + mef.endpointActivationServiceInfo.id + " activated");
        } else {
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "ignore non batch activation spec: " + mef.endpointActivationServiceInfo.id + "mef.endpointActivationServiceInfo.id");
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
//        if (mef.adminObjectServiceInfo != null) {
//            mef.adminObjectServiceInfo.removeReferencingEndpoint(mef);
//            cleanupAdminObjectServiceInfo(mef.adminObjectServiceInfo);
//        }

//        mef.endpointActivationServiceInfo.removeReferencingEndpoint(mef);
//        cleanupEndpointActivationServiceInfo(mef.endpointActivationServiceInfo);
//        endpointFactories.remove(mef);

        if (mef.runtimeActivated) deactivateEndpointInternal(mef);
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
        return jmsConnectionFactory;
    }

    /*
     * creates a ConnectionFactory configured in the server.xml
     *
     * @throws Exception
     */
    private static ConnectionFactory createConnectionFactoryInstance(ResourceConfigFactory rcf, ResourceFactory rf) {
        if (null == rf) return null;
        try {
            ResourceConfig cfResourceConfig = rcf.createResourceConfig(ConnectionFactory.class.getName());
            cfResourceConfig.setResAuthType(ResourceInfo.AUTH_CONTAINER);
            return (ConnectionFactory) rf.createResource(cfResourceConfig);
        } catch (Exception e) {
            error(tc, "error.batch.executor.jms.create.failure", new Object[] { e });
            throw new IllegalStateException("Problem creating batch executor reply CF", e);
        }
    }

}

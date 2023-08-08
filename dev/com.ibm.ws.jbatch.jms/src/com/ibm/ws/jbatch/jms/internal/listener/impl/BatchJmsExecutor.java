/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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

import java.rmi.RemoteException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactory;
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
import com.ibm.ws.jbatch.jms.internal.listener.impl.BatchJmsExecutor.EndpointActivationServiceInfo;
import com.ibm.ws.jca.service.AdminObjectService;
import com.ibm.ws.jca.service.EndpointActivationService;
import com.ibm.ws.kernel.feature.ServerStartedPhase2;
import com.ibm.ws.tx.rrs.RRSXAResourceFactory;
import com.ibm.wsspi.application.lifecycle.ApplicationStartBarrier;
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
 * TODO: add required dependency on BatchJmsDispatcher to ensure the dispatcher/connection factory
 *       is available (needed for multi-jvm partitions)
 */
@Component(
        configurationPid = "com.ibm.ws.jbatch.jms.executor", 
        configurationPolicy = REQUIRE, 
        property = { 
                // prevent the optional connection factory reference from being satisfied
                // until configuration and metatype processing is complete
                CONN_FACTORY_REF_NAME + ".cardinality.minimum=" + Integer.MAX_VALUE}
        )
public class BatchJmsExecutor {

    private static final TraceComponent tc = Tr.register(BatchJmsExecutor.class, "wsbatch", "com.ibm.ws.jbatch.jms.internal.resources.BatchJmsMessages");

    public static final String ACTIVATION_SPEC_REF_NAME = "JmsActivationSpec";
    public static final String CONN_FACTORY_REF_NAME = "JMSConnectionFactory";
    public static final String JMS_QUEUE_REF_NAME = "JmsQueue";
    public static final String OPERATION_GROUP = "operationGroup";

    private final ComponentContext context;
    private final ServiceReference<EndpointActivationService> endpointActivationSpecRef;
    private final MessageEndpointFactoryImpl endpointFactory;

    @Activate
    public BatchJmsExecutor(ComponentContext context, Map<String, Object> config,
            // Anonymous References
            @Reference ApplicationStartBarrier requiredButNotUsed,
            @Reference ServerStartedPhase2 requiredButNotUsed2,
            @Reference J2EENameFactory j2eeNameFactory,
            @Reference ResourceConfigFactory resourceConfigFactory,
            @Reference WSJobRepository jobRepository,
            @Reference(cardinality = OPTIONAL, policyOption = GREEDY) RRSXAResourceFactory xaResourceFactory,
            // Named refs to tie up with metatype, which replaces the target filters
            @Reference(name = CONN_FACTORY_REF_NAME, target = "(id=unbound)", cardinality = OPTIONAL, policyOption = GREEDY) ResourceFactory jmsConnectionFactory,
            @Reference(name = JMS_QUEUE_REF_NAME, target = "(id=unbound)") ServiceReference<AdminObjectService> adminObjectServiceRef,
            @Reference(name = ACTIVATION_SPEC_REF_NAME, target = "(id=unbound)") ServiceReference<EndpointActivationService> jmsActivationSpecRef) throws ResourceException {

        this(context, config, jmsActivationSpecRef,
                createMef(
                        config,
                        j2eeNameFactory,
                        adminObjectServiceRef,
                        xaResourceFactory,
                        resourceConfigFactory,
                        jmsConnectionFactory,
                        jobRepository));
    }

    BatchJmsExecutor(ComponentContext context, Map<String, Object> config, ServiceReference<EndpointActivationService> easRef, MessageEndpointFactoryImpl mef) throws ResourceException {
        this.context = context;
        this.endpointActivationSpecRef = easRef;
        this.endpointFactory = mef;
        mef.endpointActivationServiceInfo = new EndpointActivationServiceInfo();

        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "activation spec = " + easRef);
        if (FrameworkState.isStopping()) {
            debug(this, tc, "BatchJmsExecutor", "Framework stopping");
            return;
        }

        try {
            mef.activateEndpointInternal();
            if (isAnyTracingEnabled() && tc.isDebugEnabled())
                debug(tc, "Batch activation spec is " + mef.endpointActivationServiceInfo.id + " activated");
        } catch (ResourceException e) {
            error(tc, "error.batch.executor.activate.failure", e);
            throw e;
        }
    }

    private static MessageEndpointFactoryImpl createMef(
            Map<String, Object> config,
            J2EENameFactory j2eeNameFactory,
            ServiceReference<AdminObjectService> adminObjectServiceRef,
            RRSXAResourceFactory xarf,
            ResourceConfigFactory rcf,
            ResourceFactory rf,
            WSJobRepository repo) {
        try {
            // Since our listener is a part of the batch feature, there is no J2EE
            // application
            // but since JCA needs a name, create an artificial one.
            final J2EEName j2eeName = j2eeNameFactory.create(J2EE_APP_NAME, J2EE_APP_MODULE, J2EE_APP_COMPONENT);

            ConnectionFactory cf = createConnectionFactoryInstance(rcf, rf);
            BatchOperationGroup grp = createBatchOperationGroup(config);

            // The original logic (reproduced below) was as follows:
            // if id and jndiName exist, use jndiName; otherwise use null
            // TODO: should we just use jndiName anyway, regardless of the id property?
            final String jndiName = Optional.of(adminObjectServiceRef)
                    .filter(ref -> null != ref.getProperty("id"))
                    .map(id -> adminObjectServiceRef.getProperty("jndiName"))
                    .map(String.class::cast)
                    .orElse(null);

            MessageEndpointFactoryImpl mef = new MessageEndpointFactoryImpl(j2eeName, xarf, cf, grp, repo, jndiName);
            return mef;
        } catch (RemoteException e) {
            error(tc, "error.batch.executor.activate.failure", e);
            return null;
        }
    }

    /**
     * Information about the EndpointActivationService.
     */
    class EndpointActivationServiceInfo {
        final String id = (String) endpointActivationSpecRef.getProperty("id");
        final EndpointActivationService service = context.locateService(ACTIVATION_SPEC_REF_NAME, endpointActivationSpecRef);
        final int maxEndpoints = (Integer) endpointActivationSpecRef.getProperty("maxEndpoints");

        @Override
        public String toString() {
            return "EndpointActivationServiceInfo [service=" + service + ", id=" + id + ", maxEndpoints=" + maxEndpoints + "]";
        }
    }

    private static BatchOperationGroup createBatchOperationGroup(Map<String, Object> config) {
        return Optional.ofNullable(config.get(OPERATION_GROUP))
                .map(String[].class::cast)
                .map(BatchOperationGroup::new) // pass some strings to the constructor
                .orElseGet(BatchOperationGroup::new); // pass no strings to the constructor
    }

    @Deactivate
    protected void deactivate() {
        try {
            endpointFactory.deactivateEndpointInternal();
        } catch (Throwable ex) {
            // The endpoint has been placed back in the pending state,
            // nothing else to do; either the server is going down, or
            // the activation spec went down; hopefully the endpoint
            // will activate if the activation spec comes back up.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Ignoring unexpected exception : " + ex);
            }
        };
    }

    /*
     * creates a ConnectionFactory configured in the server.xml
     *
     * @throws Exception
     */
    private static ConnectionFactory createConnectionFactoryInstance(ResourceConfigFactory rcf, ResourceFactory rf) {
        if (null == rf)
            return null;
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

/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
import java.util.Map;

import javax.resource.ResourceException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.ws.container.service.metadata.internal.J2EENameImpl;
import com.ibm.ws.jbatch.jms.internal.BatchJmsConstants;
import com.ibm.ws.jca.service.AdminObjectService;
import com.ibm.ws.jca.service.EndpointActivationService;
import com.ibm.ws.kernel.feature.ServerStartedPhase2;
import com.ibm.wsspi.application.lifecycle.ApplicationStartBarrier;

import test.common.ComponentContextMockery;
import test.common.SharedOutputManager;

public class MessageEndpointFactoryImplTest {
    private class TestMessageEndpointFactory extends com.ibm.ws.jbatch.jms.internal.listener.impl.MessageEndpointFactoryImpl {
        private final String activationSpecId;
        private final String destinationId;
        boolean allowActivate;
        boolean activateThrows;
        boolean activated;
        boolean allowDeactivate;
        boolean deactivated;

        TestMessageEndpointFactory(String activationSpecId, BatchJmsExecutor batchExecutor) throws RemoteException {
            this(activationSpecId, null, batchExecutor);
        }

        TestMessageEndpointFactory(String activationSpecId, String destinationId, BatchJmsExecutor batchExecutor) throws RemoteException {
            super(batchExecutor);
            this.activationSpecId = activationSpecId;
            this.destinationId = destinationId;
            j2eeName = new J2EENameImpl("a", "m", "c");
        }

        @Override
        public void activateEndpointInternal(EndpointActivationService eas, int maxEndpoints, String jndi) throws ResourceException {
            try {
                Assert.assertNotNull("expected non-null EndpointActivationService", eas);
                Assert.assertTrue("unexpected activateEndpointInternal call", allowActivate);
                allowActivate = false;
                activated = true;
                if (activateThrows) {
                    throw new ResourceException("test");
                }
            } catch (Error e) {
                setCapturedError(e);
                throw e;
            }
        }

        void assertActivated() {
            Assert.assertTrue("expected activateInternal", activated);
            activated = false;
        }

        @Override
        protected void deactivateEndpointInternal(EndpointActivationService eas) {
            try {
                Assert.assertNotNull("expected non-null EndpointActivationService", eas);
                Assert.assertTrue("unexpected deactivateEndpointInternal call", allowDeactivate);
                allowDeactivate = false;
                deactivated = true;
            } catch (Error e) {
                setCapturedError(e);
                throw e;
            }
        }

        void assertDeactivated() {
            Assert.assertTrue("expected deactivateInternal", deactivated);
            deactivated = false;
        }

        @Override
        public String getActivationSpecId() {
            return activationSpecId;
        }

        @Override
        public String getDestinationId() {
            return destinationId;
        }
    }

    private static final class ServerStartedPhase2Impl implements ServerStartedPhase2 {}

    @Rule
    public final SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");
    private final Mockery mockery = new Mockery();
    private final ComponentContextMockery ccMockery = new ComponentContextMockery(mockery);
    private final ComponentContext cc = mockery.mock(ComponentContext.class);
    private final Map<String, Object> config = new HashMap<String, Object>();

    private Error capturedError;

    void setCapturedError(Error e) {
        if (capturedError == null) {
            capturedError = e;
        }
    }

    @Before
    public void before() {
        config.put(BatchJmsConstants.ACTIVATION_SPEC_REF_CONFIG, "batchActivationSpec");
        config.put(BatchJmsConstants.QUEUE_REF_CONFIG, "batchJobSubmissionQueue");
    }

    @After
    public void after() {
        if (capturedError != null) {
            throw new Error(capturedError);
        }
    }

    private ServiceReference<EndpointActivationService> mockEASSR(final String id, EndpointActivationService eas) {
        final ServiceReference<EndpointActivationService> easSR = ccMockery.mockService(cc, "JmsActivationSpec", eas);
        mockery.checking(new Expectations() {
            {
                allowing(easSR).getProperty("id");
                will(returnValue(id));
                allowing(easSR).getProperty("maxEndpoints");
                will(returnValue(0));
            }
        });
        return easSR;
    }

    private ServiceReference<EndpointActivationService> mockEASSR(String id) {
        return mockEASSR(id, new EndpointActivationService());
    }

    private ServiceReference<AdminObjectService> mockAOSSR(final String id, final String jndiName) {
        final ServiceReference<AdminObjectService> aosSR = ccMockery.mockService(cc, "JmsQueue", null);
        mockery.checking(new Expectations() {
            {
                allowing(aosSR).getProperty("id");
                will(returnValue(id));
                allowing(aosSR).getProperty("jndiName");
                will(returnValue(jndiName));
            }
        });
        return aosSR;
    }

    private J2EENameFactory mockJ2EENameFactory() {
        final J2EENameFactory mockJ2eeFactory = mockery.mock(J2EENameFactory.class);
        mockery.checking(new Expectations() {
            {
                allowing(mockJ2eeFactory).create("JBatchListenerApp", "JBatchListenerModule", "JBatchListenerComp");
                will(returnValue(new J2EENameImpl("JBatchListenerApp", "JBatchListenerModule", "JBatchListenerComp")));
            }
        });
        return mockJ2eeFactory;
    }

    @Test
    public void testAddRemoveEndpointActivationService() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor(cc, config, 
                new ApplicationStartBarrier() {},
                new ServerStartedPhase2() {},
                mockJ2EENameFactory(),
                null, // resource config factory
                null, // job repository
                null, // XA resource factory
                null, // connection factory
                mockAOSSR("batchJobSubmissionQueue", null),
                mockEASSR("as"));
        // new style executor no longer permits removal of activation spec
    }

    @Test
    public void testActivateEndpoint() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor(cc, config, 
                new ApplicationStartBarrier() {},
                new ServerStartedPhase2() {},
                mockJ2EENameFactory(),
                null, // resource config factory
                null, // job repository
                null, // XA resource factory
                null, // connection factory
                mockAOSSR("batchJobSubmissionQueue", null),
                mockEASSR("batchActivationSpec")); 

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", runtime);
        mef.allowActivate = true;

        runtime.activateEndpoint(mef);
        mef.assertActivated();
    }

    @Test(expected = ResourceException.class)
    public void testActivateEndpointException() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor(cc, config, 
                new ApplicationStartBarrier() {},
                new ServerStartedPhase2() {},
                mockJ2EENameFactory(),
                null, // resource config factory
                null, // job repository
                null, // XA resource factory
                null, // connection factory
                mockAOSSR("batchJobSubmissionQueue", null),
                mockEASSR("batchActivationSpec"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", runtime);
        mef.allowActivate = true;
        mef.activateThrows = true;
        runtime.activateEndpoint(mef);
    }

    @Test
    public void testDeactivateEndpoint() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor(cc, config, 
                new ApplicationStartBarrier() {},
                new ServerStartedPhase2() {},
                mockJ2EENameFactory(),
                null, // resource config factory
                null, // job repository
                null, // XA resource factory
                null, // connection factory
                mockAOSSR("batchJobSubmissionQueue", null),
                mockEASSR("batchActivationSpec"));
        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", runtime);
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.deactivate();
        mef.assertDeactivated();
    }

    @Test
    public void testActivateEndpointWithDestinationId() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor(cc, config, 
                new ApplicationStartBarrier() {},
                new ServerStartedPhase2() {},
                mockJ2EENameFactory(),
                null, // resource config factory
                null, // job repository
                null, // XA resource factory
                null, // connection factory
                mockAOSSR("batchJobSubmissionQueue", null),
                mockEASSR("batchActivationSpec")); 

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", "batchJobSubmissionQueue", runtime);
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();
    }

    @Test
    public void testActivateEndpointWithDestinationJndiName() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor(cc, config, 
                new ApplicationStartBarrier() {},
                new ServerStartedPhase2() {},
                mockJ2EENameFactory(),
                null, // resource config factory
                null, // job repository
                null, // XA resource factory
                null, // connection factory
                mockAOSSR("jbatchRequestsQueue", "jms/batch/jobSubmissionQueue"),
                mockEASSR("batchActivationSpec"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", "jms/batch/jobSubmissionQueue", runtime);
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();
    }

    @Test
    public void testDeactivateEndpointWithDestination() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor(cc, config, 
                new ApplicationStartBarrier() {},
                new ServerStartedPhase2() {},
                mockJ2EENameFactory(),
                null, // resource config factory
                null, // job repository
                null, // XA resource factory
                null, // connection factory
                mockAOSSR("batchJobSubmissionQueue", null),
                mockEASSR("batchActivationSpec"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", "batchJobSubmissionQueue", runtime);
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.deactivate();
        mef.assertDeactivated();
    }
}

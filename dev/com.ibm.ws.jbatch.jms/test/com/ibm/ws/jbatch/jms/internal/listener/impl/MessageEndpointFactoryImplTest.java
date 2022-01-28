/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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

/**
 *
 */
public class MessageEndpointFactoryImplTest {
    private class TestMessageEndpointFactory extends com.ibm.ws.jbatch.jms.internal.listener.impl.MessageEndpointFactoryImpl {
        private final String activationSpecId;
        private final String destinationId;
        boolean allowActivate;
        boolean activateThrows;
        boolean activated;
        boolean allowDeactivate;
        boolean deactivated;

        TestMessageEndpointFactory(String activationSpecId, JmsExecutor batchExecutor) throws RemoteException {
            this(activationSpecId, null, batchExecutor);
        }

        TestMessageEndpointFactory(String activationSpecId, String destinationId, JmsExecutor batchExecutor) throws RemoteException {
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
    private static final class ApplicationStartBarrierImpl implements ApplicationStartBarrier {}

    @Rule
    //public final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
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

        Map<String, Object> config = new HashMap<>();
        JmsExecutor runtime = new JmsExecutor(cc, config, new ApplicationStartBarrierImpl() , new ServerStartedPhase2Impl(), mockJ2EENameFactory(),);
       ServiceReference<EndpointActivationService> easSR = mockEASSR("as");
//        runtime.addEndPointActivationService(easSR);
//        runtime.removeEndPointActivationService(easSR);
        runtime.setJmsActivationSpec(easSR);
        runtime.unsetJmsActivationSpec(easSR);
    }

    @Test
    public void testActivateEndpoint() throws Exception {
        JmsExecutor runtime = new JmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());

        runtime.setContext(cc);

        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());

        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", runtime);
        mef.allowActivate = true;

        runtime.activateEndpoint(mef);
        mef.assertActivated();
    }

    @Test(expected = ResourceException.class)
    public void testActivateEndpointException() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", runtime);
        mef.allowActivate = true;
        mef.activateThrows = true;
        runtime.activateEndpoint(mef);
    }

    @Test
    public void testActivateEndpointNoEAS() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec", null));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", runtime);
        runtime.setEndpointActivationSpecId("batchActivationSpec");
        runtime.activateEndpoint(mef);

        mef.allowActivate = true;
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));
        mef.assertActivated();
    }

    @Ignore
    //fail
    public void testActivateEndpointBeforeEAS() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("", runtime);
        runtime.activateEndpoint(mef);

        //outputMgr.expectWarning("CNTR4015W");
        outputMgr.expectOutput("ActivationSpect not found:");
        mef.allowActivate = true;
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));
        mef.assertActivated();
    }

    @Ignore
    //fail
    public void testActivateEndpointExceptionBeforeEAS() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as", runtime);
        runtime.activateEndpoint(mef);

        mef.allowActivate = true;
        mef.activateThrows = true;
        runtime.setJmsActivationSpec(mockEASSR("as"));
        mef.assertActivated();
        // Exception from deferred activation should be swallowed.
    }

    @Test
    public void testActivateEndpointBeforeEASAndServerStarted() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", runtime);
        runtime.setEndpointActivationSpecId("batchActivationSpec");
        runtime.activateEndpoint(mef);

        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));

        mef.allowActivate = true;
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        mef.assertActivated();
    }

    @Test
    public void testActivateEndpointBeforeServerStarted() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", runtime);
        runtime.activateEndpoint(mef);

        mef.allowActivate = true;
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        mef.assertActivated();
    }

    @Test
    public void testActivateEndpointBeforeServerStartedAndEAS() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", runtime);
        runtime.setEndpointActivationSpecId("batchActivationSpec");
        runtime.activateEndpoint(mef);

        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());

        mef.allowActivate = true;
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));
        mef.assertActivated();
    }

    @Test
    public void testDeactivateEndpoint() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", runtime);
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.deactivateEndpoint(mef);
        mef.assertDeactivated();
    }

    @Test
    public void testDeactivateEndpointViaEAS() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        ServiceReference<EndpointActivationService> easSR = mockEASSR("batchActivationSpec");
        runtime.setJmsActivationSpec(easSR);

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", runtime);
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.unsetJmsActivationSpec(easSR);
        mef.assertDeactivated();
    }

    @Test
    public void testDeactivateEndpointViaReplaceEAS() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        ServiceReference<EndpointActivationService> easSR = mockEASSR("batchActivationSpec");
        runtime.setJmsActivationSpec(easSR);

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", runtime);
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        ServiceReference<EndpointActivationService> easSR2 = mockEASSR("batchActivationSpec");
        mef.allowDeactivate = true;
        mef.allowActivate = true;
        runtime.setJmsActivationSpec(easSR2);
        mef.assertDeactivated();
        mef.assertActivated();

        runtime.unsetJmsActivationSpec(easSR);
    }

    @Test
    public void testDeactivateEndpointBeforeActivate() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", runtime);
        runtime.setEndpointActivationSpecId("batchActivationSpec");
        runtime.activateEndpoint(mef);
        runtime.deactivateEndpoint(mef);

        // Ensure MEF is not activated when EAS becomes available.
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));
    }

    @Test
    public void testAddRemoveAdminObjectService() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);

        ServiceReference<AdminObjectService> aosSR = mockAOSSR("batchJobSubmissionQueue", null);
        runtime.setJmsQueue(aosSR);
        runtime.unsetJmsQueue(aosSR);

        aosSR = mockAOSSR("batchJobSubmissionQueue", "jms/batch/jobSubmissionQueue");
        runtime.setJmsQueue(aosSR);
        runtime.unsetJmsQueue(aosSR);
    }

    @Test
    public void testActivateEndpointWithDestinationId() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));
        runtime.setJmsQueue(mockAOSSR("batchJobSubmissionQueue", null));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", "batchJobSubmissionQueue", runtime);
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();
    }

    @Ignore
    public void testActivateEndpointWithDestinationJndiName() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));
        runtime.setJmsQueue(mockAOSSR("jbatchRequestsQueue", "jms/batch/jobSubmissionQueue"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", "jms/batch/jobSubmissionQueue", runtime);
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();
    }

    @Test
    public void testDeActivateEndpointWithDestination() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));
        runtime.setJmsQueue(mockAOSSR("batchJobSubmissionQueue", null));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", "batchJobSubmissionQueue", runtime);
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.deactivateEndpoint(mef);
        mef.assertDeactivated();
    }

    @Ignore
    public void testActivateEndpointBeforeAOS() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", "batchJobSubmissionQueue", runtime);
        //outputMgr.expectWarning("CNTR4016W");
        outputMgr.expectOutput("destination not found: ");
        runtime.activateEndpoint(mef);

        mef.allowActivate = true;
        runtime.setJmsQueue(mockAOSSR("batchJobSubmissionQueue", null));
        mef.assertActivated();
    }

    @Test
    public void testAddRemoveAdminObjectServiceNoop() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));
        runtime.setJmsQueue(mockAOSSR("batchJobSubmissionQueue", null));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", "batchJobSubmissionQueue", runtime);
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        ServiceReference<AdminObjectService> aosSR2 = mockAOSSR("batchJobSubmissionQueue", null);
        runtime.setJmsQueue(aosSR2);
        runtime.unsetJmsQueue(aosSR2);
    }

    @Test
    public void testDeactivateEndpointViaAOS() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));
        ServiceReference<AdminObjectService> aosSR = mockAOSSR("batchJobSubmissionQueue", null);
        runtime.setJmsQueue(aosSR);

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", "batchJobSubmissionQueue", runtime);
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.unsetJmsQueue(aosSR);
        mef.assertDeactivated();
    }

    @Ignore
    public void testDeactivateEndpointViaReplaceAOS() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));
        ServiceReference<AdminObjectService> aosSR = mockAOSSR("batchJobSubmissionQueue", null);
        ServiceReference<AdminObjectService> aosSR2 = mockAOSSR("batchJobSubmissionQueue", null);
        runtime.setJmsQueue(aosSR2);

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", "batchJobSubmissionQueue", runtime);
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        mef.allowActivate = true;
        runtime.setJmsQueue(aosSR);
        mef.assertDeactivated();
        mef.assertActivated();

        mef.allowDeactivate = true;
        mef.allowActivate = true;
        runtime.unsetJmsQueue(aosSR);
        mef.assertDeactivated();
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.unsetJmsQueue(aosSR2);
        mef.assertDeactivated();
    }

    @Ignore
    public void testDeactivateEndpointViaReplaceAOSIdVsJndiName() throws Exception {
        BatchJmsExecutor runtime = new BatchJmsExecutor();
        runtime.setJEENameFactory(mockJ2EENameFactory());
        runtime.setContext(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.setJmsActivationSpec(mockEASSR("batchActivationSpec"));
        ServiceReference<AdminObjectService> aosSR = mockAOSSR("batchJobSubmissionQueue", "jms/batch/jobSubmissionQueue");
        ServiceReference<AdminObjectService> aosSR2 = mockAOSSR("jms/batch/jobSubmissionQueue", null);
        runtime.setJmsQueue(aosSR);

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", "jms/batch/jobSubmissionQueue", runtime);
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        mef.allowActivate = true;
        runtime.setJmsQueue(aosSR2);
        mef.assertDeactivated();
        mef.assertActivated();

        mef.allowDeactivate = true;
        mef.allowActivate = true;
        runtime.unsetJmsQueue(aosSR2);
        mef.assertDeactivated();
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.unsetJmsQueue(aosSR);
        mef.assertDeactivated();
    }
}

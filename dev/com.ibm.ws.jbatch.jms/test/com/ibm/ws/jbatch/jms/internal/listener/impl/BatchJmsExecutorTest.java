/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.resource.ResourceException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.container.service.metadata.internal.J2EENameImpl;
import com.ibm.ws.jbatch.jms.internal.BatchJmsConstants;
import com.ibm.ws.jca.service.EndpointActivationService;

import test.common.ComponentContextMockery;
import test.common.SharedOutputManager;

@SuppressWarnings("restriction")
public class BatchJmsExecutorTest {
    private class TestMessageEndpointFactory extends com.ibm.ws.jbatch.jms.internal.listener.impl.MessageEndpointFactoryImpl {
        private final String activationSpecId;
        boolean allowActivate;
        boolean activateThrows;
        boolean activated;
        boolean allowDeactivate;
        boolean deactivated;

        TestMessageEndpointFactory(String activationSpecId) throws RemoteException {
            this(activationSpecId, null);
        }

        TestMessageEndpointFactory(String activationSpecId, String destinationId) throws RemoteException {
            super(new J2EENameImpl("a", "m", "c"), null, null, null, null, destinationId);
            this.activationSpecId = activationSpecId;
        }

        @Override
        public void activateEndpointInternal() throws ResourceException {
            try {
                Assert.assertNotNull("expected non-null EndpointActivationService", endpointActivationServiceInfo.service);
                Assert.assertTrue("unexpected activateEndpointInternal call", allowActivate);
                allowActivate = false;
                activated = true;
                if (activateThrows) throw new ResourceException(this.getClass().getSimpleName()+" activateThrows="+activateThrows);
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
        protected void deactivateEndpointInternal() {
            try {
                Assert.assertNotNull("expected non-null EndpointActivationService", endpointActivationServiceInfo.service);
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
    }

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

    @Test
    public void testActivateEndpoint() throws Exception {
        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec");
        mef.allowActivate = true;
        new BatchJmsExecutor(cc, config, mockEASSR("batchActivationSpec"), mef); 
        mef.assertActivated();
    }

    @Test(expected = ResourceException.class)
    public void testActivateEndpointException() throws Exception {
        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec");
        mef.allowActivate = true;
        mef.activateThrows = true;
        new BatchJmsExecutor(cc, config, mockEASSR("batchActivationSpec"), mef);
    }

    @Test
    public void testDeactivateEndpoint() throws Exception {
        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec");
        mef.allowActivate = true;
        BatchJmsExecutor runtime = new BatchJmsExecutor(cc, config, mockEASSR("batchActivationSpec"), mef);
        mef.assertActivated();
        mef.allowDeactivate = true;
        runtime.deactivate();
        mef.assertDeactivated();
    }

    @Test
    public void testActivateEndpointWithDestinationId() throws Exception {
        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", "batchJobSubmissionQueue");
        mef.allowActivate = true;
        new BatchJmsExecutor(cc, config, mockEASSR("batchActivationSpec"), mef); 
        mef.assertActivated();
    }

    @Test
    public void testActivateEndpointWithDestinationJndiName() throws Exception {
        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", "jms/batch/jobSubmissionQueue");
        mef.allowActivate = true;
        new BatchJmsExecutor(cc, config, mockEASSR("batchActivationSpec"), mef);
        mef.assertActivated();
    }

    @Test
    public void testDeactivateEndpointWithDestination() throws Exception {
        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("batchActivationSpec", "batchJobSubmissionQueue");
        mef.allowActivate = true;
        BatchJmsExecutor runtime = new BatchJmsExecutor(cc, config, mockEASSR("batchActivationSpec"), mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.deactivate();
        mef.assertDeactivated();
    }
}

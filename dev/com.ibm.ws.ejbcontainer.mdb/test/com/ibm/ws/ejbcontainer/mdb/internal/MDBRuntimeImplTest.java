/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import java.util.Properties;

import javax.resource.ResourceException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.csi.EJBApplicationMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.ws.container.service.metadata.internal.J2EENameImpl;
import com.ibm.ws.jca.service.AdminObjectService;
import com.ibm.ws.jca.service.EndpointActivationService;
import com.ibm.ws.kernel.feature.ServerStartedPhase2;

import test.common.ComponentContextMockery;
import test.common.SharedOutputManager;

public class MDBRuntimeImplTest {
    @SuppressWarnings("serial")
    private class TestMessageEndpointFactory extends MessageEndpointFactoryImpl {
        private final String activationSpecId;
        private final String destinationId;
        boolean allowActivate;
        boolean activateThrows;
        boolean activated;
        boolean allowDeactivate;
        boolean deactivated;

        TestMessageEndpointFactory(String activationSpecId) throws RemoteException {
            this(activationSpecId, null);
        }

        TestMessageEndpointFactory(String activationSpecId, String destinationId) throws RemoteException {
            this.activationSpecId = activationSpecId;
            this.destinationId = destinationId;
            j2eeName = new J2EENameImpl("a", "m", "c");
            createBMD();

        }

        @Override
        protected void activateEndpointInternal(EndpointActivationService eas, int maxEndpoints, AdminObjectService adminObjSvc) throws ResourceException {
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

        // Stub out a bare minimum BeanMetaData so that accessing the destination property
        // in the runtime succeeds.
        private void createBMD() {
            beanMetaData = new BeanMetaData(0);
            beanMetaData.ivModuleVersion = BeanMetaData.J2EE_EJB_VERSION_3_0;
            beanMetaData.ivActivationConfig = new Properties();
            if (destinationId != null) {
                beanMetaData.ivActivationConfig.put("destination", this.destinationId);
            }
            enabled = true;
            beanMetaData._moduleMetaData = new EJBModuleMetaDataImpl(0, new EJBApplicationMetaData(null, null, null, true, null, true, false));
        }
    }

    private static final class ServerStartedPhase2Impl implements ServerStartedPhase2 {}

    @Rule
    public final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final Mockery mockery = new Mockery();
    private final ComponentContextMockery ccMockery = new ComponentContextMockery(mockery);
    private final ComponentContext cc = mockery.mock(ComponentContext.class);
    private Error capturedError;

    void setCapturedError(Error e) {
        if (capturedError == null) {
            capturedError = e;
        }
    }

    @After
    public void after() {
        if (capturedError != null) {
            throw new Error(capturedError);
        }
    }

    private ServiceReference<EndpointActivationService> mockEASSR(final String id, EndpointActivationService eas) {
        final ServiceReference<EndpointActivationService> easSR = ccMockery.mockService(cc, MDBRuntimeImpl.REFERENCE_ENDPOINT_ACTIVATION_SERVICES, eas);
        mockery.checking(new Expectations() {
            {
                allowing(easSR).getProperty("id");
                will(returnValue(id));
                allowing(easSR).getProperty("maxEndpoints");
                will(returnValue(0));
                allowing(easSR).getProperty("autoStart");
                will(returnValue(true));
            }
        });
        return easSR;
    }

    private ServiceReference<EndpointActivationService> mockEASSR(String id) {
        return mockEASSR(id, new EndpointActivationService());
    }

    private ServiceReference<AdminObjectService> mockAOSSR(final String id, final String jndiName) {
        final ServiceReference<AdminObjectService> aosSR = ccMockery.mockService(cc, MDBRuntimeImpl.REFERENCE_ADMIN_OBJECT_SERVICES, null);
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

    @Test
    public void testAddRemoveEndpointActivationService() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);

        ServiceReference<EndpointActivationService> easSR = mockEASSR("as");
        runtime.addEndPointActivationService(easSR);
        runtime.removeEndPointActivationService(easSR);
    }

    @Test
    public void testActivateEndpoint() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.addEndPointActivationService(mockEASSR("as"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as");
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();
    }

    @Test(expected = ResourceException.class)
    public void testActivateEndpointException() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.addEndPointActivationService(mockEASSR("as"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as");
        mef.allowActivate = true;
        mef.activateThrows = true;
        runtime.activateEndpoint(mef);
    }

    @Test
    public void testActivateEndpointNoEAS() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.addEndPointActivationService(mockEASSR("as", null));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as");
        runtime.activateEndpoint(mef);

        mef.allowActivate = true;
        runtime.addEndPointActivationService(mockEASSR("as"));
        mef.assertActivated();
    }

    @Test
    public void testActivateEndpointBeforeEAS() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as");
        runtime.activateEndpoint(mef);

        outputMgr.expectWarning("CNTR4015W");
        mef.allowActivate = true;
        runtime.addEndPointActivationService(mockEASSR("as"));
        mef.assertActivated();
    }

    @Test
    public void testActivateEndpointExceptionBeforeEAS() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as");
        runtime.activateEndpoint(mef);

        mef.allowActivate = true;
        mef.activateThrows = true;
        runtime.addEndPointActivationService(mockEASSR("as"));
        mef.assertActivated();
        // Exception from deferred activation should be swallowed.
    }

    @Test
    public void testActivateEndpointBeforeEASAndServerStarted() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as");
        runtime.activateEndpoint(mef);

        runtime.addEndPointActivationService(mockEASSR("as"));

        mef.allowActivate = true;
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        mef.assertActivated();
    }

    @Test
    public void testActivateEndpointBeforeServerStarted() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.addEndPointActivationService(mockEASSR("as"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as");
        runtime.activateEndpoint(mef);

        mef.allowActivate = true;
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        mef.assertActivated();
    }

    @Test
    public void testActivateEndpointBeforeServerStartedAndEAS() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as");
        runtime.activateEndpoint(mef);

        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());

        mef.allowActivate = true;
        runtime.addEndPointActivationService(mockEASSR("as"));
        mef.assertActivated();
    }

    @Test
    public void testDeactivateEndpoint() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.addEndPointActivationService(mockEASSR("as"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as");
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.deactivateEndpoint(mef);
        mef.assertDeactivated();
    }

    @Test
    public void testDeactivateEndpointViaEAS() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        ServiceReference<EndpointActivationService> easSR = mockEASSR("as");
        runtime.addEndPointActivationService(easSR);

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as");
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.removeEndPointActivationService(easSR);
        mef.assertDeactivated();
    }

    @Test
    public void testDeactivateEndpointViaReplaceEAS() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        ServiceReference<EndpointActivationService> easSR = mockEASSR("as");
        runtime.addEndPointActivationService(easSR);

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as");
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        ServiceReference<EndpointActivationService> easSR2 = mockEASSR("as");
        mef.allowDeactivate = true;
        mef.allowActivate = true;
        runtime.addEndPointActivationService(easSR2);
        mef.assertDeactivated();
        mef.assertActivated();

        runtime.removeEndPointActivationService(easSR);
    }

    @Test
    public void testDeactivateEndpointBeforeActivate() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as");
        runtime.activateEndpoint(mef);
        runtime.deactivateEndpoint(mef);

        // Ensure MEF is not activated when EAS becomes available.
        runtime.addEndPointActivationService(mockEASSR("as"));
    }

    @Test
    public void testAddRemoveAdminObjectService() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);

        ServiceReference<AdminObjectService> aosSR = mockAOSSR("dest", null);
        runtime.addAdminObjectService(aosSR);
        runtime.removeAdminObjectService(aosSR);

        aosSR = mockAOSSR("dest", "destjn");
        runtime.addAdminObjectService(aosSR);
        runtime.removeAdminObjectService(aosSR);

        aosSR = mockAOSSR("dest", "dest");
        runtime.addAdminObjectService(aosSR);
        runtime.removeAdminObjectService(aosSR);
    }

    @Test
    public void testActivateEndpointWithDestinationId() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.addEndPointActivationService(mockEASSR("as"));
        runtime.addAdminObjectService(mockAOSSR("dest", null));
        runtime.addAdminObjectService(mockAOSSR("dest", null));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as", "dest");
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();
    }

    @Test
    public void testActivateEndpointWithDestinationJndiName() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.addEndPointActivationService(mockEASSR("as"));
        runtime.addAdminObjectService(mockAOSSR("dest", "destjn"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as", "destjn");
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();
    }

    @Test
    public void testDeActivateEndpointWithDestination() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.addEndPointActivationService(mockEASSR("as"));
        runtime.addAdminObjectService(mockAOSSR("dest", null));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as", "dest");
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.deactivateEndpoint(mef);
        mef.assertDeactivated();
    }

    @Test
    public void testActivateEndpointBeforeAOS() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.addEndPointActivationService(mockEASSR("as"));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as", "dest");
        outputMgr.expectWarning("CNTR4016W");
        runtime.activateEndpoint(mef);

        mef.allowActivate = true;
        runtime.addAdminObjectService(mockAOSSR("dest", null));
        mef.assertActivated();
    }

    @Test
    public void testAddRemoveAdminObjectServiceNoop() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.addEndPointActivationService(mockEASSR("as"));
        runtime.addAdminObjectService(mockAOSSR("dest", null));

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as", "dest");
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        ServiceReference<AdminObjectService> aosSR2 = mockAOSSR("dest", null);
        runtime.addAdminObjectService(aosSR2);
        runtime.removeAdminObjectService(aosSR2);
    }

    @Test
    public void testDeactivateEndpointViaAOS() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.addEndPointActivationService(mockEASSR("as"));
        ServiceReference<AdminObjectService> aosSR = mockAOSSR("dest", null);
        runtime.addAdminObjectService(aosSR);

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as", "dest");
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.removeAdminObjectService(aosSR);
        mef.assertDeactivated();
    }

    @Test
    public void testDeactivateEndpointViaReplaceAOS() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.addEndPointActivationService(mockEASSR("as"));
        ServiceReference<AdminObjectService> aosSR = mockAOSSR("dest", null);
        ServiceReference<AdminObjectService> aosSR2 = mockAOSSR("dest", null);
        runtime.addAdminObjectService(aosSR2);

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as", "dest");
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        mef.allowActivate = true;
        runtime.addAdminObjectService(aosSR);
        mef.assertDeactivated();
        mef.assertActivated();

        mef.allowDeactivate = true;
        mef.allowActivate = true;
        runtime.removeAdminObjectService(aosSR);
        mef.assertDeactivated();
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.removeAdminObjectService(aosSR2);
        mef.assertDeactivated();
    }

    @Test
    public void testDeactivateEndpointViaReplaceAOSIdVsJndiName() throws Exception {
        MDBRuntimeImpl runtime = new MDBRuntimeImpl();
        runtime.activate(cc);
        runtime.setServerStartedPhase2(new ServerStartedPhase2Impl());
        runtime.addEndPointActivationService(mockEASSR("as"));
        ServiceReference<AdminObjectService> aosSR = mockAOSSR("destid", "destjn");
        ServiceReference<AdminObjectService> aosSR2 = mockAOSSR("destjn", null);
        runtime.addAdminObjectService(aosSR);

        TestMessageEndpointFactory mef = new TestMessageEndpointFactory("as", "destjn");
        mef.allowActivate = true;
        runtime.activateEndpoint(mef);
        mef.assertActivated();

        mef.allowDeactivate = true;
        mef.allowActivate = true;
        runtime.addAdminObjectService(aosSR2);
        mef.assertDeactivated();
        mef.assertActivated();

        mef.allowDeactivate = true;
        mef.allowActivate = true;
        runtime.removeAdminObjectService(aosSR2);
        mef.assertDeactivated();
        mef.assertActivated();

        mef.allowDeactivate = true;
        runtime.removeAdminObjectService(aosSR);
        mef.assertDeactivated();
    }
}

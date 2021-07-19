/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.action.CustomAction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.zos.core.Angel;
import com.ibm.ws.zos.core.NativeService;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;

@RunWith(JMock.class)
public class NativeServiceTrackerTest {

    final Mockery context = new JUnit4Mockery();
    final NativeMethodManager mockNativeMethodManager = context.mock(NativeMethodManager.class);

    class TestVariableRegistry implements VariableRegistry {
        @Override
        public boolean addVariable(String variable, String value) {
            return false;
        }

        @Override
        public void replaceVariable(String variable, String value) {
        }

        @Override
        public String resolveString(String string) {
            return null;
        }

        @Override
        public String resolveRawString(String string) {
            return null;
        }

        @Override
        public void removeVariable(String variable) {
        }
    }

    class TestNativeServiceTracker extends NativeServiceTracker {
        List<String> loadUnauthorizedCalls = new ArrayList<String>();
        List<String> registerServerCalls = new ArrayList<String>();
        List<String> registerServerAngelNameCalls = new ArrayList<String>();

        int deregisterCount = 0;

        TestNativeServiceTracker() {
            super(mockNativeMethodManager, new TestVariableRegistry());
        }

        @Override
        boolean fileExists(final File file) {
            return true;
        }

        @Override
        protected ServiceResults ntv_loadUnauthorized(String unauthorizedModulePath) {
            loadUnauthorizedCalls.add(unauthorizedModulePath);
            return new ServiceResults(0, 0, 0);
        }

        @Override
        protected int ntv_registerServer(String authorizedModulePath, String angelName) {
            registerServerCalls.add(authorizedModulePath);
            registerServerAngelNameCalls.add(angelName);
            return 0;
        }

        @Override
        protected int ntv_deregisterServer() {
            deregisterCount++;
            return 0;
        }

        @Override
        protected int ntv_getNativeServiceEntries(List<String> permittedServices, List<String> deniedServices, List<String> permittedClientServices,
                                                  List<String> deniedClientServices) {
            return 0;
        }

        @Override
        protected int ntv_getAngelVersion() {
            return 0;
        }

        @Override
        protected int ntv_getExpectedAngelVersion() {
            return 0;
        }
    };

    @Before
    public void classSetup() {
        context.checking(new Expectations() {
            {
                ignoring(mockNativeMethodManager);
            }
        });
    }

    @Test
    public void testStartStop() throws Exception {
        TestNativeServiceTracker testTracker = new TestNativeServiceTracker();

        // activate
        final BundleContext bundleContext = context.mock(BundleContext.class);
        context.checking(new Expectations() {
            {
                ignoring(bundleContext);
            }
        });
        testTracker.start(bundleContext);
        assertNotNull(testTracker.nativeMethodManager);

        final String unauthorizedLib = new File(NativeServiceTracker.WAS_LIB_DIR, NativeServiceTracker.UNAUTHORIZED_FUNCTION_MODULE).getAbsolutePath();
        assertEquals(1, testTracker.loadUnauthorizedCalls.size());
        assertTrue(testTracker.loadUnauthorizedCalls.contains(unauthorizedLib));

        final String authorizedModule = new File(NativeServiceTracker.WAS_LIB_DIR, NativeServiceTracker.AUTHORIZED_FUNCTION_MODULE).getAbsolutePath();
        assertEquals(1, testTracker.registerServerCalls.size());
        assertTrue(testTracker.registerServerCalls.contains(authorizedModule));
        assertTrue(testTracker.registerServerAngelNameCalls.contains(null));

        // deactivate
        testTracker.stop(bundleContext);
        assertEquals(1, testTracker.deregisterCount);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterOSGiService() throws Exception {
        final ServiceRegistration<NativeService> serviceRegistration1 = context.mock(ServiceRegistration.class, "registration1");
        final List<Object> registerService1Parameters = new ArrayList<Object>();
        final String service1Name = "TESTSVC1";

        final ServiceRegistration<NativeService> serviceRegistration2 = context.mock(ServiceRegistration.class, "registration2");
        final List<Object> registerService2Parameters = new ArrayList<Object>();
        final String service2Name = "TEST";

        final BundleContext bundleContext = context.mock(BundleContext.class);
        context.checking(new Expectations() {
            {
                oneOf(bundleContext).getProperty(BootstrapConstants.LOC_INTERNAL_LIB_DIR);
                will(returnValue(""));

                oneOf(bundleContext).registerService(with(same(Angel.class)),
                                                     with(aNonNull(AngelImpl.class)),
                                                     with(aNonNull(Dictionary.class)));

                oneOf(bundleContext).registerService(with(same(NativeService.class)),
                                                     with(aNonNull(NativeServiceImpl.class)),
                                                     with(aNonNull(Dictionary.class)));
                will(doAll(captureParameters(registerService1Parameters), returnValue(serviceRegistration1)));

                oneOf(bundleContext).registerService(with(same(NativeService.class)),
                                                     with(aNonNull(NativeServiceImpl.class)),
                                                     with(aNonNull(Dictionary.class)));
                will(doAll(captureParameters(registerService2Parameters), returnValue(serviceRegistration2)));
            }
        });

        // Activate the component and provide mock bundle context
        TestNativeServiceTracker testTracker = new TestNativeServiceTracker();
        testTracker.start(bundleContext);

        assertSame(bundleContext, testTracker.bundleContext);
        assertTrue(testTracker.registrations.isEmpty());

        testTracker.registerOSGiService(service1Name, "GROUP1", true, false);
        assertEquals(1, testTracker.registrations.size());
        assertTrue(testTracker.registrations.contains(serviceRegistration1));
        assertNotNull(registerService1Parameters);
        assertEquals(3, registerService1Parameters.size());

        NativeService service1 = (NativeService) registerService1Parameters.get(1);
        assertNotNull(service1);
        assertEquals(true, service1.isPermitted());
        assertEquals(service1Name, service1.getServiceName());

        Dictionary<String, Object> service1Props = (Dictionary<String, Object>) registerService1Parameters.get(2);
        assertNotNull(service1Props);
        assertEquals("true", service1Props.get(NativeService.IS_AUTHORIZED));
        assertEquals(service1.isPermitted(), Boolean.valueOf((String) service1Props.get(NativeService.IS_AUTHORIZED)).booleanValue());
        assertEquals(service1Name, service1Props.get(NativeService.NATIVE_SERVICE_NAME));
        assertEquals(service1.getServiceName(), service1Props.get(NativeService.NATIVE_SERVICE_NAME));
        assertEquals("GROUP1", service1Props.get(NativeService.AUTHORIZATION_GROUP_NAME));
        assertEquals(service1.getAuthorizationGroup(), service1Props.get(NativeService.AUTHORIZATION_GROUP_NAME));

        testTracker.registerOSGiService(service2Name + "      ", "GROUP2   ", false, false);
        assertEquals(2, testTracker.registrations.size());
        assertTrue(testTracker.registrations.contains(serviceRegistration2));
        assertNotNull(registerService2Parameters);
        assertEquals(3, registerService2Parameters.size());

        NativeService service2 = (NativeService) registerService2Parameters.get(1);
        assertEquals(false, service2.isPermitted());
        assertEquals(service2Name, service2.getServiceName());

        Dictionary<String, Object> service2Props = (Dictionary<String, Object>) registerService2Parameters.get(2);
        assertNotNull(service2Props);
        assertEquals("false", service2Props.get(NativeService.IS_AUTHORIZED));
        assertEquals(service2Name, service2Props.get(NativeService.NATIVE_SERVICE_NAME));
        assertEquals("GROUP2", service2Props.get(NativeService.AUTHORIZATION_GROUP_NAME));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnregisterOSGiServices() throws Exception {
        final ServiceRegistration<NativeService> serviceRegistration = context.mock(ServiceRegistration.class);
        context.checking(new Expectations() {
            {
                oneOf(serviceRegistration).unregister();
            }
        });

        final ServiceRegistration<Angel> angelRegistration = context.mock(ServiceRegistration.class, "angelMock");
        context.checking(new Expectations() {
            {
                oneOf(angelRegistration).unregister();
            }
        });

        final BundleContext bundleContext = context.mock(BundleContext.class);
        context.checking(new Expectations() {
            {
                oneOf(bundleContext).getProperty(BootstrapConstants.LOC_INTERNAL_LIB_DIR);
                will(returnValue(""));

                oneOf(bundleContext).registerService(with(same(Angel.class)),
                                                     with(aNonNull(AngelImpl.class)),
                                                     with(aNonNull(Dictionary.class)));
                will(returnValue(angelRegistration));

                oneOf(bundleContext).registerService(with(same(NativeService.class)),
                                                     with(aNonNull(NativeServiceImpl.class)),
                                                     with(aNonNull(Dictionary.class)));
                will(returnValue(serviceRegistration));
            }
        });

        // Activate the component and provide mock bundle context
        TestNativeServiceTracker testTracker = new TestNativeServiceTracker();
        testTracker.start(bundleContext);

        // Register a service and verify it's the only one
        testTracker.registerOSGiService("JUNK", "JUNK", true, false);
        assertEquals(1, testTracker.registrations.size());
        assertTrue(testTracker.registrations.contains(serviceRegistration));

        // Drive unregisterOSGiServices
        testTracker.unregisterOSGiServices();
        assertTrue(testTracker.registrations.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPopulateServiceRegistry() throws Exception {
        final List<String> ntvPermittedServices = new ArrayList<String>(Arrays.asList("ENABLED1", "EPROFIL1",
                                                                                      "ENABLED2", "EPROFIL2",
                                                                                      "ENABLED3", "EPROFIL3"));
        final List<String> ntvDeniedServices = new ArrayList<String>(Arrays.asList("DENIED1", "DENPROF1",
                                                                                   "DENIED2", "DENPROF2"));

        final int expectedRegistrations = (ntvPermittedServices.size() + ntvDeniedServices.size()) / 2;

        TestNativeServiceTracker testTracker = new TestNativeServiceTracker() {
            @Override
            int getNativeServiceEntries(List<String> permittedServices, List<String> deniedServices, List<String> permittedClientServices, List<String> deniedClientServices) {
                permittedServices.addAll(ntvPermittedServices);
                deniedServices.addAll(ntvDeniedServices);
                return expectedRegistrations;
            }

            @Override
            void registerOSGiService(String name, String authorizationGroup, boolean isAuthorized, boolean isClient) {
                if (isAuthorized) {
                    assertTrue(ntvPermittedServices.remove(name));
                    assertTrue(ntvPermittedServices.remove(authorizationGroup));
                } else {
                    assertTrue(ntvDeniedServices.remove(name));
                    assertTrue(ntvDeniedServices.remove(authorizationGroup));
                }
                super.registerOSGiService(name, authorizationGroup, isAuthorized, isClient);
            }
        };

        final List<Object> allRegisterParameters = new ArrayList<Object>();
        final BundleContext bundleContext = context.mock(BundleContext.class);
        context.checking(new Expectations() {
            {
                oneOf(bundleContext).getProperty(BootstrapConstants.LOC_INTERNAL_LIB_DIR);
                will(returnValue(""));

                exactly(expectedRegistrations).of(bundleContext).registerService(with(same(NativeService.class)),
                                                                                 with(aNonNull(NativeServiceImpl.class)),
                                                                                 with(aNonNull(Dictionary.class)));
                will(doAll(captureParameters(allRegisterParameters), returnValue(context.mock(ServiceRegistration.class))));

                oneOf(bundleContext).registerService(with(same(Angel.class)),
                                                     with(aNonNull(AngelImpl.class)),
                                                     with(aNonNull(Dictionary.class)));
            }
        });

        // Start drives populate
        testTracker.start(bundleContext);
        assertTrue(ntvPermittedServices.isEmpty());
        assertTrue(ntvDeniedServices.isEmpty());
        for (int i = 0; i < allRegisterParameters.size(); i += 3) {
            NativeService service = (NativeService) allRegisterParameters.get(i + 1);
            if (service.getServiceName().startsWith("ENABLED")) {
                assertTrue(service.isPermitted());
            } else if (service.getServiceName().startsWith("DENIED")) {
                assertFalse(service.isPermitted());
            } else {
                fail("Unexpected service " + service);
            }
        }
    }

    @Test
    public void testFileExists() throws Exception {
        NativeServiceTracker tracker = new NativeServiceTracker(null, new TestVariableRegistry());
        File tempFile = File.createTempFile("zzz", ".tmp");

        try {
            assertTrue(tracker.fileExists(tempFile));
        } finally {
            tempFile.delete();
            assertFalse(tracker.fileExists(tempFile));
        }
    }

    // Custom jmock action to capture parameters for future verification.
    static Action captureParameters(final List<Object> parameters) {
        return new CustomAction("capture parameters") {
            @Override
            public Object invoke(Invocation invocation) throws Throwable {
                parameters.addAll(Arrays.asList(invocation.getParametersAsArray()));
                return null;
            }
        };
    }

}

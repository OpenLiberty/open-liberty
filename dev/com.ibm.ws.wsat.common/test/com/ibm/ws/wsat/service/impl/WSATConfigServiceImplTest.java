/*******************************************************************************
 * Copyright 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.wsat.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.wsat.service.Handler;
import com.ibm.ws.wsat.test.MockProxy;
import com.ibm.wsspi.http.VirtualHost;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Unit tests for the virtualHostRef support added to WSATConfigServiceImpl.
 *
 * Tests cover:
 * UT-1 getWSATUrl() returns URL from the configured virtual host, not default_host
 * UT-2 registerVirtualHostVariable() writes the correct value (custom VH, and null/empty fallback)
 * UT-3 unregisterVirtualHostVariable() is called during deactivate()
 * UT-4 fallback to default_host + warning when lookupVirtualHost() finds nothing
 * UT-5 externalURLPrefix overrides virtual host URL construction (regression guard)
 * UT-6 no redundant BundleContext lookup when virtualHostRef unchanged across modified() calls
 */
public class WSATConfigServiceImplTest {

    // -----------------------------------------------------------------------
    // Inner mock classes — all public so MockProxy reflection dispatch works
    // -----------------------------------------------------------------------

    /** Mock VirtualHost: getUrlString() returns configurable base URL + contextRoot. */
    public static class MockVirtualHost extends MockProxy {
        final String baseUrl;
        int getUrlStringCallCount = 0;

        public MockVirtualHost(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getUrlString(String contextRoot, boolean securedPreferred) {
            getUrlStringCallCount++;
            return baseUrl + contextRoot;
        }
    }

    /** Mock Handler: silently accepts all set*Endpoint() calls. */
    public static class MockHandler extends MockProxy {
        public void setCoordinatorEndpoint(EndpointReferenceType epr) {}
        public void setRegistrationEndpoint(EndpointReferenceType epr) {}
        public void setParticipantEndpoint(EndpointReferenceType epr) {}
    }

    /** Mock VariableRegistry: records addVariable/removeVariable in a Map. */
    public static class MockVariableRegistry extends MockProxy {
        public final Map<String, String> variables = new HashMap<>();

        public boolean addVariable(String name, String value) {
            variables.put(name, value);
            return true;
        }

        public void removeVariable(String name) {
            variables.remove(name);
        }
    }

    /**
     * Mock ServiceReference: extends MockProxy with compareTo() for stable ordering
     * in AtomicServiceReference's PriorityQueue.
     * Use mkRef() (not asMock()) to create the proxy — mkRef() uses ServiceReference's
     * own classloader so the proxy is visible to AtomicServiceReference.
     */
    public static class MockServiceRef<T> extends MockProxy {
        public boolean equals(Object other) {
            return this == other;
        }
        public int hashCode() {
            return System.identityHashCode(this);
        }
        public int compareTo(Object other) {
            return System.identityHashCode(this) - System.identityHashCode(other);
        }

        @SuppressWarnings("unchecked")
        public ServiceReference<T> mkRef() {
            // Use the thread context classloader (set by Gradle to the test classloader)
            // so the proxy is visible to AtomicServiceReference on the same classloader.
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) cl = ServiceReference.class.getClassLoader();
            if (cl == null) cl = MockServiceRef.class.getClassLoader();
            return (ServiceReference<T>) java.lang.reflect.Proxy.newProxyInstance(
                cl, new Class<?>[] { ServiceReference.class }, this);
        }
    }

    /**
     * Mock BundleContext: returns a configurable list of VirtualHost ServiceReferences.
     * Does NOT declare throws — MockProxy wraps checked exceptions as
     * UndeclaredThrowableException if the method throws, so we keep it clean.
     */
    public static class MockBundleContext extends MockProxy {
        public List<ServiceReference<VirtualHost>> vhRefs = Collections.emptyList();
        public int getServiceReferencesCallCount = 0;

        // Matches BundleContext.getServiceReferences(Class<S>, String) throws InvalidSyntaxException
        @SuppressWarnings("unchecked")
        public <S> Collection<ServiceReference<S>> getServiceReferences(
                Class<S> clazz, String filter) throws InvalidSyntaxException {
            getServiceReferencesCallCount++;
            if (VirtualHost.class.equals(clazz)) {
                return (Collection<ServiceReference<S>>) (Collection<?>) vhRefs;
            }
            return Collections.emptyList();
        }
    }

    /**
     * Mock ComponentContext: dispatches locateService() and getBundleContext()
     * by name. Each test registers its service objects via register().
     */
    public static class MockComponentContext extends MockProxy {
        private final MockBundleContext bundleContext;
        private final Map<String, Object> services = new HashMap<>();

        public MockComponentContext(MockBundleContext bundleContext) {
            this.bundleContext = bundleContext;
        }

        public void register(String refName, Object serviceInstance) {
            services.put(refName, serviceInstance);
        }

        public BundleContext getBundleContext() {
            return bundleContext.asMock(BundleContext.class);
        }

        public Object locateService(String name, ServiceReference<?> reference) {
            return services.get(name);
        }
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private WSATConfigServiceImpl impl;
    private MockComponentContext  mockCc;
    private MockBundleContext     mockBundleCc;
    private MockVariableRegistry  mockVarReg;
    private MockHandler           mockHandler;

    private AtomicServiceReference<VirtualHost>      httpOptionsRef;
    private AtomicServiceReference<Handler>          handlerServiceRef;
    private AtomicServiceReference<VariableRegistry> variableRegistryAtomicRef;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Reflectively retrieve a named static AtomicServiceReference field. */
    @SuppressWarnings("unchecked")
    private static <T> AtomicServiceReference<T> getStaticRef(String fieldName) throws Exception {
        Field f = WSATConfigServiceImpl.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (AtomicServiceReference<T>) f.get(null);
    }

    /** Build a base properties map suitable for passing to modified(). */
    private Map<String, Object> baseProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("virtualHostRef",        "default_host");
        props.put("sslEnabled",            Boolean.FALSE);
        props.put("sslRef",                "defaultSSLConfig");
        props.put("externalURLPrefix",     "");
        props.put("asyncResponseTimeout",  30000L);
        props.put("clientAuth",            Boolean.FALSE);
        return props;
    }

    /**
     * Wire httpOptions to a default VH (port 9080) so that modified() can complete
     * without NPE on the getWSATUrl() -> httpOptions.getService() call.
     * Returns the MockVirtualHost so tests can inspect call counts.
     */
    private MockVirtualHost setupDefaultHttpOptions() throws Exception {
        MockVirtualHost defaultVH = new MockVirtualHost("http://host:9080");
        @SuppressWarnings("unchecked")
        ServiceReference<VirtualHost> defaultVhRef =
            new MockServiceRef<VirtualHost>().mkRef();
        mockCc.register("httpOptions", defaultVH.asMock(VirtualHost.class));
        httpOptionsRef.setReference(defaultVhRef);
        httpOptionsRef.activate(mockCc.asMock(ComponentContext.class));
        return defaultVH;
    }

    // -----------------------------------------------------------------------
    // @Before / @After
    // -----------------------------------------------------------------------

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        impl = new WSATConfigServiceImpl();

        // Obtain the static AtomicServiceReference fields via reflection.
        // Field names must match the private static final field names in WSATConfigServiceImpl.
        httpOptionsRef            = getStaticRef("httpOptions");
        handlerServiceRef         = getStaticRef("handlerService");
        variableRegistryAtomicRef = getStaticRef("variableRegistryRef");

        // Build shared mocks
        mockVarReg   = new MockVariableRegistry();
        mockHandler  = new MockHandler();
        mockBundleCc = new MockBundleContext();
        mockCc       = new MockComponentContext(mockBundleCc);

        // Register handler and variableRegistry services in the ComponentContext mock
        mockCc.register("variableRegistry", mockVarReg.asMock(VariableRegistry.class));
        mockCc.register("handler",          mockHandler.asMock(Handler.class));

        ComponentContext cc = mockCc.asMock(ComponentContext.class);

        // Activate variableRegistry AtomicServiceReference
        ServiceReference<VariableRegistry> varRegSvcRef =
            new MockServiceRef<VariableRegistry>().mkRef();
        variableRegistryAtomicRef.setReference(varRegSvcRef);
        variableRegistryAtomicRef.activate(cc);

        // Activate handler AtomicServiceReference
        ServiceReference<Handler> handlerSvcRef =
            new MockServiceRef<Handler>().mkRef();
        handlerServiceRef.setReference(handlerSvcRef);
        handlerServiceRef.activate(cc);
    }

    @After
    public void tearDown() throws Exception {
        // Deactivate all static AtomicServiceReferences to reset their state between tests.
        // The fields are static final so they persist across test instances.
        ComponentContext cc = mockCc.asMock(ComponentContext.class);
        httpOptionsRef.deactivate(cc);
        handlerServiceRef.deactivate(cc);
        variableRegistryAtomicRef.deactivate(cc);
    }

    // -----------------------------------------------------------------------
    // UT-1: getWSATUrl() uses the configured virtual host, not default_host
    // -----------------------------------------------------------------------

    @Test
    public void testGetWSATUrlUsesConfiguredVirtualHost() throws Exception {
        // Wire default VH (port 9080) as the starting httpOptions
        setupDefaultHttpOptions();

        // Build a custom VH that answers on port 9090
        MockVirtualHost customVH = new MockVirtualHost("http://host:9090");
        @SuppressWarnings("unchecked")
        ServiceReference<VirtualHost> customVhRef =
            new MockServiceRef<VirtualHost>().mkRef();

        // BundleContext returns customVhRef when asked for the VH with id=wsatHost
        mockBundleCc.vhRefs = Collections.singletonList(customVhRef);
        // locateService("httpOptions", customVhRef) must return customVH
        mockCc.register("httpOptions", customVH.asMock(VirtualHost.class));

        Map<String, Object> props = baseProperties();
        props.put("virtualHostRef", "wsatHost");

        impl.modified(mockCc.asMock(ComponentContext.class), props);

        assertEquals("http://host:9090/ibm/wsatservice", impl.getWSATUrl());
    }

    // -----------------------------------------------------------------------
    // UT-2a: registerVirtualHostVariable() uses the configured value
    // -----------------------------------------------------------------------

    @Test
    public void testRegisterVirtualHostVariable_customValue() throws Exception {
        setupDefaultHttpOptions();

        Map<String, Object> props = baseProperties();
        props.put("virtualHostRef", "wsatHost");

        impl.modified(mockCc.asMock(ComponentContext.class), props);

        assertEquals("wsatHost",
            mockVarReg.variables.get("wsat.webservice.virtualHostRef"));
    }

    // -----------------------------------------------------------------------
    // UT-2b: registerVirtualHostVariable() falls back to default_host for null
    // -----------------------------------------------------------------------

    @Test
    public void testRegisterVirtualHostVariable_nullFallsBackToDefaultHost() throws Exception {
        setupDefaultHttpOptions();

        Map<String, Object> props = baseProperties();
        props.put("virtualHostRef", null);

        impl.modified(mockCc.asMock(ComponentContext.class), props);

        assertEquals("default_host",
            mockVarReg.variables.get("wsat.webservice.virtualHostRef"));
    }

    // -----------------------------------------------------------------------
    // UT-2c: registerVirtualHostVariable() falls back to default_host for ""
    // -----------------------------------------------------------------------

    @Test
    public void testRegisterVirtualHostVariable_emptyFallsBackToDefaultHost() throws Exception {
        setupDefaultHttpOptions();

        Map<String, Object> props = baseProperties();
        props.put("virtualHostRef", "");

        impl.modified(mockCc.asMock(ComponentContext.class), props);

        assertEquals("default_host",
            mockVarReg.variables.get("wsat.webservice.virtualHostRef"));
    }

    // -----------------------------------------------------------------------
    // UT-3: unregisterVirtualHostVariable() is called during deactivate()
    // -----------------------------------------------------------------------

    @Test
    public void testDeactivateRemovesVirtualHostVariable() throws Exception {
        setupDefaultHttpOptions();

        // Activate to register the variable
        impl.modified(mockCc.asMock(ComponentContext.class), baseProperties());
        assertTrue("Variable should be present after modified()",
            mockVarReg.variables.containsKey("wsat.webservice.virtualHostRef"));

        // Deactivate — unregisterVirtualHostVariable() must be called
        // before variableRegistryRef.deactivate(cc) so getService() still works
        impl.deactivate(mockCc.asMock(ComponentContext.class));

        assertFalse("Variable should be removed after deactivate()",
            mockVarReg.variables.containsKey("wsat.webservice.virtualHostRef"));
    }

    // -----------------------------------------------------------------------
    // UT-4: fallback to default_host when lookupVirtualHost() finds nothing
    // -----------------------------------------------------------------------

    @Test
    public void testLookupVirtualHostFallsBackToDefaultWhenNotFound() throws Exception {
        // Wire default VH (port 9080) — this should remain in use after fallback
        setupDefaultHttpOptions();

        // BundleContext returns nothing for the requested VH
        mockBundleCc.vhRefs = Collections.emptyList();

        Map<String, Object> props = baseProperties();
        props.put("virtualHostRef", "nonExistentHost");

        impl.modified(mockCc.asMock(ComponentContext.class), props);

        // configuredVirtualHostId should have been reset to "default_host"
        Field f = WSATConfigServiceImpl.class.getDeclaredField("configuredVirtualHostId");
        f.setAccessible(true);
        assertEquals("default_host", f.get(impl));

        // URL should still come from the default VH (port 9080)
        assertEquals("http://host:9080/ibm/wsatservice", impl.getWSATUrl());
    }

    // -----------------------------------------------------------------------
    // UT-5: externalURLPrefix overrides virtual host URL (regression guard)
    // -----------------------------------------------------------------------

    @Test
    public void testExternalURLPrefixOverridesVirtualHost() throws Exception {
        MockVirtualHost trackingVH = new MockVirtualHost("http://host:9080");
        @SuppressWarnings("unchecked")
        ServiceReference<VirtualHost> vhRef =
            new MockServiceRef<VirtualHost>().mkRef();
        mockCc.register("httpOptions", trackingVH.asMock(VirtualHost.class));
        httpOptionsRef.setReference(vhRef);
        httpOptionsRef.activate(mockCc.asMock(ComponentContext.class));

        Map<String, Object> props = baseProperties();
        props.put("externalURLPrefix", "https://proxy.example.com");

        impl.modified(mockCc.asMock(ComponentContext.class), props);

        assertEquals("https://proxy.example.com/ibm/wsatservice", impl.getWSATUrl());
        assertEquals("VirtualHost.getUrlString() must not be called when proxy is set",
            0, trackingVH.getUrlStringCallCount);
    }

    // -----------------------------------------------------------------------
    // UT-6: no redundant BundleContext lookup when virtualHostRef is unchanged
    // -----------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    public void testNoRedundantLookupWhenVirtualHostRefUnchanged() throws Exception {
        // Build a custom VH for port 9090
        MockVirtualHost customVH = new MockVirtualHost("http://host:9090");
        ServiceReference<VirtualHost> customVhRef =
            new MockServiceRef<VirtualHost>().mkRef();
        mockBundleCc.vhRefs = Collections.singletonList(customVhRef);

        // Wire a default VH first so modified() doesn't NPE before the VH lookup
        setupDefaultHttpOptions();
        // Register customVH as the service returned for httpOptions after modified() switches
        mockCc.register("httpOptions", customVH.asMock(VirtualHost.class));

        Map<String, Object> props = baseProperties();
        props.put("virtualHostRef", "wsatHost");

        // First call — must trigger exactly one BundleContext lookup
        impl.modified(mockCc.asMock(ComponentContext.class), props);
        int afterFirst = mockBundleCc.getServiceReferencesCallCount;
        assertEquals("Expected exactly one BundleContext lookup on first modified()", 1, afterFirst);

        // After modified() called setReference(customVhRef), re-activate httpOptions
        // so getService() resolves correctly on the second modified() call
        httpOptionsRef.setReference(customVhRef);
        httpOptionsRef.activate(mockCc.asMock(ComponentContext.class));

        // Second call with the same virtualHostRef — the guard must suppress the lookup
        impl.modified(mockCc.asMock(ComponentContext.class), props);
        assertEquals("Expected no additional BundleContext lookup on repeated modified()",
            afterFirst, mockBundleCc.getServiceReferencesCallCount);
    }
}

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

/**
 * Unit tests for WSATConfigServiceImpl, which is immutable: every scenario builds a new instance
 * through the DS constructor, exactly as DS does after any configuration or reference change.
 *
 * Tests cover:
 * UT-1 getWSATUrl() returns URL from the configured virtual host, not default_host
 * UT-2 registerVirtualHostVariable() writes the correct value (custom VH, and null/empty fallback)
 * UT-3 unregisterVirtualHostVariable() is called during deactivate()
 * UT-4 fallback to default_host + warning when lookupVirtualHost() finds nothing
 * UT-5 externalURLPrefix overrides virtual host URL construction (regression guard)
 * UT-6 deactivate() releases the directly-obtained virtual host service
 * UT-7 the beta propagation attribute is ignored outside a beta edition and honoured within one
 * UT-8 modified() preserves active configuration without mutating state
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
     * Mock BundleContext: returns a configurable list of VirtualHost ServiceReferences
     * and a configurable VirtualHost service instance for getService().
     * Does NOT declare throws — MockProxy wraps checked exceptions as
     * UndeclaredThrowableException if the method throws, so we keep it clean.
     */
    public static class MockBundleContext extends MockProxy {
        public List<ServiceReference<VirtualHost>> vhRefs = Collections.emptyList();
        public VirtualHost vhService = null; // returned by getService()
        public int getServiceReferencesCallCount = 0;
        public int ungetServiceCallCount = 0;

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

        // Matches BundleContext.getService(ServiceReference<S>)
        @SuppressWarnings("unchecked")
        public <S> S getService(ServiceReference<S> reference) {
            return (S) vhService;
        }

        // Matches BundleContext.ungetService(ServiceReference<?>)
        public boolean ungetService(ServiceReference<?> reference) {
            ungetServiceCallCount++;
            return true;
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

    private static final String BETA_PROPERTY = "com.ibm.ws.beta.edition";

    private MockComponentContext  mockCc;
    private MockBundleContext     mockBundleCc;
    private MockVariableRegistry  mockVarReg;
    private MockHandler           mockHandler;
    private MockVirtualHost       defaultVH;
    private String                savedBetaProperty;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Build a base properties map suitable for passing to the constructor. */
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

    /** Create the component the way DS does: one constructor call with the config and references. */
    private WSATConfigServiceImpl activate(Map<String, Object> props) {
        return new WSATConfigServiceImpl(mockCc.asMock(ComponentContext.class),
                                         props,
                                         mockHandler.asMock(Handler.class),
                                         defaultVH.asMock(VirtualHost.class),
                                         mockVarReg.asMock(VariableRegistry.class));
    }

    // -----------------------------------------------------------------------
    // @Before / @After
    // -----------------------------------------------------------------------

    @Before
    public void setUp() throws Exception {
        savedBetaProperty = System.getProperty(BETA_PROPERTY);
        System.clearProperty(BETA_PROPERTY);

        mockVarReg   = new MockVariableRegistry();
        mockHandler  = new MockHandler();
        mockBundleCc = new MockBundleContext();
        mockCc       = new MockComponentContext(mockBundleCc);
        defaultVH    = new MockVirtualHost("http://host:9080");
    }

    @After
    public void tearDown() throws Exception {
        if (savedBetaProperty == null) {
            System.clearProperty(BETA_PROPERTY);
        } else {
            System.setProperty(BETA_PROPERTY, savedBetaProperty);
        }
    }

    // -----------------------------------------------------------------------
    // UT-1: getWSATUrl() uses the configured virtual host, not default_host
    // -----------------------------------------------------------------------

    @Test
    public void testGetWSATUrlUsesConfiguredVirtualHost() throws Exception {
        MockVirtualHost customVH = new MockVirtualHost("http://host:9090");
        // BundleContext returns customVhRef when asked for the VH with id=wsatHost,
        // and getService(customVhRef) returns the customVH instance
        mockBundleCc.vhRefs = Collections.singletonList(new MockServiceRef<VirtualHost>().mkRef());
        mockBundleCc.vhService = customVH.asMock(VirtualHost.class);

        Map<String, Object> props = baseProperties();
        props.put("virtualHostRef", "wsatHost");

        assertEquals("http://host:9090/ibm/wsatservice", activate(props).getWSATUrl());
    }

    @Test
    public void testGetWSATUrlUsesDefaultVirtualHost() throws Exception {
        assertEquals("http://host:9080/ibm/wsatservice", activate(baseProperties()).getWSATUrl());
        assertEquals("No BundleContext lookup is needed for default_host", 0, mockBundleCc.getServiceReferencesCallCount);
    }

    // -----------------------------------------------------------------------
    // UT-2a: registerVirtualHostVariable() uses the configured value
    // -----------------------------------------------------------------------

    @Test
    public void testRegisterVirtualHostVariable_customValue() throws Exception {
        Map<String, Object> props = baseProperties();
        props.put("virtualHostRef", "wsatHost");

        activate(props);

        assertEquals("wsatHost", mockVarReg.variables.get("wsat.webservice.virtualHostRef"));
    }

    // -----------------------------------------------------------------------
    // UT-2b: registerVirtualHostVariable() falls back to default_host for null
    // -----------------------------------------------------------------------

    @Test
    public void testRegisterVirtualHostVariable_nullFallsBackToDefaultHost() throws Exception {
        Map<String, Object> props = baseProperties();
        props.put("virtualHostRef", null);

        activate(props);

        assertEquals("default_host", mockVarReg.variables.get("wsat.webservice.virtualHostRef"));
    }

    // -----------------------------------------------------------------------
    // UT-2c: registerVirtualHostVariable() falls back to default_host for ""
    // -----------------------------------------------------------------------

    @Test
    public void testRegisterVirtualHostVariable_emptyFallsBackToDefaultHost() throws Exception {
        Map<String, Object> props = baseProperties();
        props.put("virtualHostRef", "");

        activate(props);

        assertEquals("default_host", mockVarReg.variables.get("wsat.webservice.virtualHostRef"));
    }

    // -----------------------------------------------------------------------
    // UT-3: unregisterVirtualHostVariable() is called during deactivate()
    // -----------------------------------------------------------------------

    @Test
    public void testDeactivateRemovesVirtualHostVariable() throws Exception {
        WSATConfigServiceImpl impl = activate(baseProperties());
        assertTrue("Variable should be present after activation",
            mockVarReg.variables.containsKey("wsat.webservice.virtualHostRef"));

        impl.deactivate();

        assertFalse("Variable should be removed after deactivate()",
            mockVarReg.variables.containsKey("wsat.webservice.virtualHostRef"));
    }

    // -----------------------------------------------------------------------
    // UT-4: fallback to default_host when lookupVirtualHost() finds nothing
    // -----------------------------------------------------------------------

    @Test
    public void testLookupVirtualHostFallsBackToDefaultWhenNotFound() throws Exception {
        // BundleContext returns nothing for the requested VH
        mockBundleCc.vhRefs = Collections.emptyList();

        Map<String, Object> props = baseProperties();
        props.put("virtualHostRef", "nonExistentHost");

        // URL should come from the default VH (port 9080)
        assertEquals("http://host:9080/ibm/wsatservice", activate(props).getWSATUrl());
    }

    // -----------------------------------------------------------------------
    // UT-5: externalURLPrefix overrides virtual host URL (regression guard)
    // -----------------------------------------------------------------------

    @Test
    public void testExternalURLPrefixOverridesVirtualHost() throws Exception {
        Map<String, Object> props = baseProperties();
        props.put("externalURLPrefix", "https://proxy.example.com");

        WSATConfigServiceImpl impl = activate(props);

        assertEquals("https://proxy.example.com/ibm/wsatservice", impl.getWSATUrl());
        assertEquals("VirtualHost.getUrlString() must not be called when proxy is set",
            0, defaultVH.getUrlStringCallCount);
    }

    // -----------------------------------------------------------------------
    // UT-6: deactivate() releases the directly-obtained virtual host service
    // -----------------------------------------------------------------------

    @Test
    public void testDeactivateReleasesConfiguredVirtualHost() throws Exception {
        mockBundleCc.vhRefs = Collections.singletonList(new MockServiceRef<VirtualHost>().mkRef());
        mockBundleCc.vhService = new MockVirtualHost("http://host:9090").asMock(VirtualHost.class);

        Map<String, Object> props = baseProperties();
        props.put("virtualHostRef", "wsatHost");
        WSATConfigServiceImpl impl = activate(props);
        assertEquals(0, mockBundleCc.ungetServiceCallCount);

        impl.deactivate();

        assertEquals("Expected ungetService() to be called once", 1, mockBundleCc.ungetServiceCallCount);
    }

    @Test
    public void testDeactivateDoesNotReleaseWhenUsingDefaultVirtualHost() throws Exception {
        activate(baseProperties()).deactivate();

        assertEquals("Nothing was obtained directly, so nothing must be released", 0, mockBundleCc.ungetServiceCallCount);
    }

    // -----------------------------------------------------------------------
    // UT-7: the propagation attribute is beta
    // -----------------------------------------------------------------------

    @Test
    public void testPropagationIgnoredOutsideBetaEdition() throws Exception {
        Map<String, Object> props = baseProperties();
        props.put("propagation", "check_wsdl");

        assertFalse("propagation must be ignored when not running a beta edition",
            activate(props).isCheckWsdlMode());
    }

    @Test
    public void testPropagationCheckWsdlHonouredInBetaEdition() throws Exception {
        System.setProperty(BETA_PROPERTY, "true");
        Map<String, Object> props = baseProperties();
        props.put("propagation", "check_wsdl");

        assertTrue(activate(props).isCheckWsdlMode());
    }

    @Test
    public void testPropagationAlwaysAndUnsetAreNotCheckWsdlInBetaEdition() throws Exception {
        System.setProperty(BETA_PROPERTY, "true");

        Map<String, Object> always = baseProperties();
        always.put("propagation", "always");
        assertFalse(activate(always).isCheckWsdlMode());

        // No value at all: no default was supplied (the public feature is not enabled)
        assertFalse(activate(baseProperties()).isCheckWsdlMode());
    }

    // -----------------------------------------------------------------------
    // UT-8: modified() preserves active configuration for transport/security properties
    // -----------------------------------------------------------------------

    @Test
    public void testModifiedPreservesActiveConfigurationForImmutableProperties() throws Exception {
        System.setProperty(BETA_PROPERTY, "true");

        Map<String, Object> props = baseProperties();
        props.put("virtualHostRef", "default_host");
        props.put("sslEnabled", false);
        props.put("sslRef", "defaultSSLConfig");
        props.put("clientAuth", false);
        props.put("externalURLPrefix", "");

        WSATConfigServiceImpl impl = activate(props);
        assertEquals("http://host:9080/ibm/wsatservice", impl.getWSATUrl());
        assertFalse(impl.isSSLEnabled());
        assertEquals("defaultSSLConfig", impl.getSSLReferenceId());
        assertFalse(impl.isClientAuthEnabled());

        // Invoke modified with changed transport & security properties
        Map<String, Object> updatedProps = baseProperties();
        updatedProps.put("virtualHostRef", "wsatHost");
        updatedProps.put("sslEnabled", true);
        updatedProps.put("sslRef", "customSSLConfig");
        updatedProps.put("clientAuth", true);
        updatedProps.put("externalURLPrefix", "https://proxy.example.com");

        impl.modified(updatedProps);

        // Active instance must continue returning original transport/security configuration (deferred to restart)
        assertEquals("http://host:9080/ibm/wsatservice", impl.getWSATUrl());
        assertFalse(impl.isSSLEnabled());
        assertEquals("defaultSSLConfig", impl.getSSLReferenceId());
        assertFalse(impl.isClientAuthEnabled());
    }

    // -----------------------------------------------------------------------
    // UT-9: modified() applies propagation dynamically
    // -----------------------------------------------------------------------

    @Test
    public void testModifiedAppliesPropagationDynamicallyInBeta() throws Exception {
        System.setProperty(BETA_PROPERTY, "true");

        Map<String, Object> props = baseProperties();
        props.put("propagation", "always");

        WSATConfigServiceImpl impl = activate(props);
        assertFalse(impl.isCheckWsdlMode());

        // Dynamically switch to check_wsdl
        Map<String, Object> updatedProps = baseProperties();
        updatedProps.put("propagation", "check_wsdl");
        impl.modified(updatedProps);
        assertTrue("propagation change to check_wsdl should take effect immediately in beta", impl.isCheckWsdlMode());

        // Dynamically switch back to always
        Map<String, Object> revertProps = baseProperties();
        revertProps.put("propagation", "always");
        impl.modified(revertProps);
        assertFalse("propagation change back to always should take effect immediately", impl.isCheckWsdlMode());
    }

    @Test
    public void testModifiedIgnoresPropagationOutsideBeta() throws Exception {
        System.clearProperty(BETA_PROPERTY);

        Map<String, Object> props = baseProperties();
        props.put("propagation", "always");

        WSATConfigServiceImpl impl = activate(props);
        assertFalse(impl.isCheckWsdlMode());

        // Attempt dynamic switch to check_wsdl outside beta
        Map<String, Object> updatedProps = baseProperties();
        updatedProps.put("propagation", "check_wsdl");
        impl.modified(updatedProps);
        assertFalse("propagation attribute must be ignored outside beta", impl.isCheckWsdlMode());
    }

    // -----------------------------------------------------------------------
    // UT-10: modified() applies asyncResponseTimeout dynamically
    // -----------------------------------------------------------------------

    @Test
    public void testModifiedAppliesAsyncResponseTimeoutDynamically() throws Exception {
        Map<String, Object> props = baseProperties();
        props.put("asyncResponseTimeout", 30000L);

        WSATConfigServiceImpl impl = activate(props);
        assertEquals(30000L, impl.getAsyncResponseTimeout());

        // Dynamically update asyncResponseTimeout
        Map<String, Object> updatedProps = baseProperties();
        updatedProps.put("asyncResponseTimeout", 60000L);
        impl.modified(updatedProps);
        assertEquals(60000L, impl.getAsyncResponseTimeout());
    }
}

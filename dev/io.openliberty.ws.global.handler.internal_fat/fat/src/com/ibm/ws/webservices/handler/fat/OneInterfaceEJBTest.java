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
package com.ibm.ws.webservices.handler.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import javax.ejb.Singleton;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider.OneLocalInterfaceMyStuffResource;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.singleton.OneLocalInterfaceSingletonFieldInjectionEJB;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.singleton.OneLocalInterfaceSingletonFieldInjectionView;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.singleton.OneLocalInterfaceSingletonPropertyInjectionEJB;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.singleton.OneLocalInterfaceSingletonPropertyInjectionView;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.singleton.OneLocalInterfaceSingletonView;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.withbeanname.OneLocalInterfaceWithBeanNameView;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.withbeanname.OneLocalWithBeanNameFieldInjectionView;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.withbeanname.OneLocalWithBeanNamePropertyInjectionView;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class OneInterfaceEJBTest {
    @Server("RSEJBTestServer")
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("RSEJBTestServer");
    private static final String ejbjaxrsinwar = "ejbjaxrsinwar";
    private RestClient client;

    @Before
    public void setUp() {
        ClientConfig config = new ClientConfig();
        config.connectTimeout(120000);
        config.readTimeout(120000);
        client = new RestClient(config);
    }

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, ejbjaxrsinwar, "com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.*");
        server.addInstalledAppForValidation("ejbjaxrsinwar");
        //server.installUserBundle("RSHandler1_1.0.0");
        ShrinkHelper.defaultUserFeatureArchive(server, "rsUserBundle1", "com.ibm.ws.rsuserbundle1.myhandler");
        TestUtils.installUserFeature(server, "RSHandler1Feature");
        //server.installUserBundle("RSHandler2_1.0.0");
        ShrinkHelper.defaultUserFeatureArchive(server, "rsUserBundle2", "com.ibm.ws.rsuserbundle2.myhandler");
        TestUtils.installUserFeature(server, "RSHandler2Feature");
        server.startServer(true);
        // Make sure server is started
        assertNotNull("The server did not start", server.waitForStringInLog("CWWKF0011I"));

    }

    @AfterClass
    public static void tearDown()
                    throws Exception {
        server.stopServer();
    }

    // testOneLocalInterfaceEJB - testOneLocalInterfaceWithBeanNameEJB
    // from OneLocalInterfaceEJBTest
    /**
     * Tests that a {@link Singleton} EJB with a one local interface view.
     */
    @Test
    public void testOneLocalInterfaceEJB() {
        checkSingletonInvocations(OneLocalInterfaceSingletonView.class);
    }

    /**
     * Tests that a {@link Singleton} EJB with a one local interface view with a
     * customized bean name can be invoked.
     */
    @Test
    public void testOneLocalInterfaceWithBeanNameEJB() {
        checkSingletonInvocations(OneLocalInterfaceWithBeanNameView.class);
    }

    private void checkSingletonInvocations(Class<?> clazz) {
        // reset the counter just in case this test messed up along the way and
        // this is being re-run
        // URL is something like http://localhost:8010/ejbjaxrsinwar/oneLocalInterfaceSingletonEJB
        // or http://localhost:8010/ejbjaxrsinwar/oneLocalInterfaceWithBeanNameEJB
        String uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(clazz).build().toString();
        Resource res = client.resource(uri);
        assertEquals(204, res.delete().getStatusCode());
        assertNotNull("RSInHandler1 not invoked", server.waitForStringInLog("in RSInHandler1 handleMessage method"));
        assertNotNull("RSInHandler2 not invoked", server.waitForStringInLog("in RSInHandler2 handleMessage method"));
        res = client.resource(uri);

        for (int c = 1; c < 101; ++c) {
            ClientResponse resp = res.get();
            assertEquals(String.valueOf(c), resp.getEntity(String.class));
            assertEquals(200, resp.getStatusCode());
        }
    }

    // testProviderWithDirectImplementation - testProviderInjectionApplicationProperty from
    // OneLocalInterfaceProviderEJBTest
    private static final String providerTestUrl = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(OneLocalInterfaceMyStuffResource.class).build().toString();

    @Test
    public void testProviderWithDirectImplementation() {
        assertEquals("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider.OneLocalInterfaceMyStuffProvider wrote this.",
                     client.resource(providerTestUrl).accept("my/stuff").get(String.class));
    }

    @Test
    public void testProviderWithoutDirectImplementationOfInterface() {
        assertEquals("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider.OneLocalInterfaceMyOtherStuffProvider wrote this.",
                     client.resource(providerTestUrl).accept("my/otherstuff").get(String.class));
    }

    @Test
    public void testProviderInjectionUriInfoProperty() {
        assertEquals("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider.OneLocalInterfaceMyStuffProvider wrote this." + providerTestUrl,
                     client.resource(providerTestUrl).accept("my/stuff").header("Test-Name", "uriinfo").get(String.class));
    }

    @Test
    public void testProviderInjectionRequestProperty() {
        assertEquals("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider.OneLocalInterfaceMyStuffProvider wrote this.GET",
                     client.resource(providerTestUrl).accept("my/stuff").header("Test-Name", "request").get(String.class));
    }

    @Test
    public void testProviderInjectionSecurityContextProperty() {
        assertEquals("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider.OneLocalInterfaceMyStuffProvider wrote this.false",
                     client.resource(providerTestUrl).accept("my/stuff").header("Test-Name", "securitycontext").get(String.class));
    }

    @Test
    public void testProviderInjectionServletConfigProperty() {
        assertEquals("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider.OneLocalInterfaceMyStuffProvider wrote this.javax.ws.rs.core.Application",
                     client.resource(providerTestUrl).accept("my/stuff").header("Test-Name", "servletconfig").get(String.class));
    }

    @Test
    public void testProviderInjectionProvidersProperty() {
        String expected = "com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider.OneLocalInterfaceMyStuffProvider " +
                          "wrote this.org.apache.wink.server.internal.providers.exception.EJBAccessExceptionMapper";
        assertEquals(expected, client.resource(providerTestUrl).accept("my/stuff").header("Test-Name", "providers").get(String.class));
    }

    @Test
    public void testProviderInjectionHttpServletRequestProperty() {
        assertEquals("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider.OneLocalInterfaceMyStuffProvider wrote this.my/stuff",
                     client.resource(providerTestUrl).accept("my/stuff").header("Test-Name", "httpservletrequest").get(String.class));
    }

    @Test
    public void testProviderInjectionHttpServletResponseProperty() {
        ClientResponse response =
                        client.resource(providerTestUrl).accept("my/stuff").header("Test-Name", "httpservletresponse").get();
        assertEquals("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider.OneLocalInterfaceMyStuffProvider wrote this.", response.getEntity(String.class));
        assertNotNull(response.getHeaders().get("Test-Verification"));
        assertEquals(1, response.getHeaders().get("Test-Verification").size());
        assertEquals("verification", response.getHeaders().get("Test-Verification").get(0));
    }

    @Test
    public void testProviderInjectionServletContextProperty() {
        String expected = "com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider.OneLocalInterfaceMyStuffProvider " +
                          "wrote this." + ejbjaxrsinwar;
        assertEquals(expected, client.resource(providerTestUrl).accept("my/stuff").header("Test-Name", "servletcontext").get(String.class));
    }

    @Test
    public void testProviderInjectionApplicationProperty() {
        String response = client.resource(providerTestUrl).accept("my/stuff").header("Test-Name", "application").get(String.class);
        String expected = "com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider.OneLocalInterfaceMyStuffProvider " +
                          "wrote this.com.ibm.ws.jaxrs.injection.ApplicationInjectionProxy";
        assertTrue(response.startsWith(expected));
    }

    // testIsEJB - testLocalInterfaceWithBeanNameSingletonApplicationPropertyInjection
    // are from OneLocalInterfaceEJBJAXRSInjectionTest
    /**
     * Tests that the classes are in fact EJBs.
     */
    @Test
    public void testIsEJB() {
        assertTrue(OneLocalInterfaceSingletonFieldInjectionEJB.class.getAnnotation(Singleton.class) != null);
        assertTrue(OneLocalInterfaceSingletonPropertyInjectionEJB.class.getAnnotation(Singleton.class) != null);
    }

    /*
     * Singleton Field Injection
     */

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link UriInfo} field injections.
     */
    @Test
    public void testLocalInterfaceSingletonUriInfoFieldInjection() throws Exception {
        checkUriInfoInjection(OneLocalInterfaceSingletonFieldInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link HttpHeaders} field injections.
     */
    @Test
    public void testLocalInterfaceSingletonHttpHeadersFieldInjection() throws Exception {
        checkHttpHeadersInjection(OneLocalInterfaceSingletonFieldInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link Providers} field injections.
     */
    @Test
    public void testLocalInterfaceSingletonProvidersFieldInjection() throws Exception {
        checkProvidersInjection(OneLocalInterfaceSingletonFieldInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link HttpServletResponse} field injections.
     */
    @Test
    public void testLocalInterfaceSingletonHttpServletResponseFieldInjection() throws Exception {
        checkServletResponseInjection(OneLocalInterfaceSingletonFieldInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link Request} field injections.
     */
    @Test
    public void testLocalInterfaceSingletonRequestFieldInjection() throws Exception {
        checkRequestInjection(OneLocalInterfaceSingletonFieldInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link SecurityContext} field injections.
     */
    @Test
    public void testLocalInterfaceSingletonSecurityContextFieldInjection() throws Exception {
        checkSecurityContextInjection(OneLocalInterfaceSingletonFieldInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link HttpServletRequest} field injections.
     */
    @Test
    public void testLocalInterfaceSingletonServletRequestFieldInjection() throws Exception {
        checkServletRequestInjection(OneLocalInterfaceSingletonFieldInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link Application} field injections.
     */
    @Test
    public void testLocalInterfaceSingletonApplicationFieldInjection() throws Exception {
        checkApplicationInjection(OneLocalInterfaceSingletonFieldInjectionView.class);
    }

    /*
     * Singleton Property Injection
     */

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link UriInfo} property injections.
     */
    @Test
    public void testLocalInterfaceSingletonUriInfoPropertyInjection() throws Exception {
        checkUriInfoInjection(OneLocalInterfaceSingletonPropertyInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link HttpHeaders} property injections.
     */
    @Test
    public void testLocalInterfaceSingletonHttpHeadersPropertyInjection() throws Exception {
        checkHttpHeadersInjection(OneLocalInterfaceSingletonPropertyInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link Providers} property injections.
     */
    @Test
    public void testLocalInterfaceSingletonProvidersPropertyInjection() throws Exception {
        checkProvidersInjection(OneLocalInterfaceSingletonPropertyInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link HttpServletResponse} property injections.
     */
    @Test
    public void testLocalInterfaceSingletonHttpServletResponsePropertyInjection() throws Exception {
        checkServletResponseInjection(OneLocalInterfaceSingletonPropertyInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link Request} property injections.
     */
    @Test
    public void testLocalInterfaceSingletonRequestPropertyInjection() throws Exception {
        checkRequestInjection(OneLocalInterfaceSingletonPropertyInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link SecurityContext} property injections.
     */
    @Test
    public void testLocalInterfaceSingletonSecurityContextPropertyInjection() throws Exception {
        checkSecurityContextInjection(OneLocalInterfaceSingletonPropertyInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link HttpServletRequest} property injections.
     */
    @Test
    public void testLocalInterfaceSingletonServletRequestPropertyInjection() throws Exception {
        checkServletRequestInjection(OneLocalInterfaceSingletonPropertyInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link Application} property injections.
     */
    @Test
    public void testLocalInterfaceSingletonApplicationPropertyInjection() throws Exception {
        checkApplicationInjection(OneLocalInterfaceSingletonPropertyInjectionView.class);
    }

    /*
     * Local Interface Stateless With Bean Name Field Injection
     */

    /**
     * Tests that a {@link Stateless} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link UriInfo} field injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameStatelessUriInfoFieldInjection() throws Exception {
        checkUriInfoInjection(OneLocalWithBeanNameFieldInjectionView.class);
    }

    /**
     * Tests that a {@link Stateless} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link HttpHeaders} field injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameStatelessHttpHeadersFieldInjection() throws Exception {
        checkHttpHeadersInjection(OneLocalWithBeanNameFieldInjectionView.class);
    }

    /**
     * Tests that a {@link Stateless} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link Providers} field injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameStatelessProvidersFieldInjection() throws Exception {
        checkProvidersInjection(OneLocalWithBeanNameFieldInjectionView.class);
    }

    /**
     * Tests that a {@link Stateless} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link HttpServletResponse} field injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameStatelessHttpServletResponseFieldInjection()
                    throws Exception {
        checkServletResponseInjection(OneLocalWithBeanNameFieldInjectionView.class);
    }

    /**
     * Tests that a {@link Stateless} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link Request} field injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameStatelessRequestFieldInjection() throws Exception {
        checkRequestInjection(OneLocalWithBeanNameFieldInjectionView.class);
    }

    /**
     * Tests that a {@link Stateless} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link SecurityContext} field injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameStatelessSecurityContextFieldInjection()
                    throws Exception {
        checkSecurityContextInjection(OneLocalWithBeanNameFieldInjectionView.class);
    }

    /**
     * Tests that a {@link Stateless} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link HttpServletRequest} field injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameStatelessServletRequestFieldInjection()
                    throws Exception {
        checkServletRequestInjection(OneLocalWithBeanNameFieldInjectionView.class);
    }

    /**
     * Tests that a {@link Stateless} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link Application} field injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameStatelessApplicationFieldInjection() throws Exception {
        checkApplicationInjection(OneLocalWithBeanNameFieldInjectionView.class);
    }

    /*
     * Local Interface Singleton Property Injection
     */

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link UriInfo} property injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameSingletonUriInfoPropertyInjection() throws Exception {
        checkUriInfoInjection(OneLocalWithBeanNamePropertyInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link HttpHeaders} property injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameSingletonHttpHeadersPropertyInjection()
                    throws Exception {
        checkHttpHeadersInjection(OneLocalWithBeanNamePropertyInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link Providers} property injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameSingletonProvidersPropertyInjection()
                    throws Exception {
        checkProvidersInjection(OneLocalWithBeanNamePropertyInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link HttpServletResponse} property injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameSingletonHttpServletResponsePropertyInjection()
                    throws Exception {
        checkServletResponseInjection(OneLocalWithBeanNamePropertyInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link Request} property injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameSingletonRequestPropertyInjection() throws Exception {
        checkRequestInjection(OneLocalWithBeanNamePropertyInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link SecurityContext} property injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameSingletonSecurityContextPropertyInjection()
                    throws Exception {
        checkSecurityContextInjection(OneLocalWithBeanNamePropertyInjectionView.class);
    }

    /**
     * Tests that a {@link Singleton} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link HttpServletRequest} property injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameSingletonServletRequestPropertyInjection()
                    throws Exception {
        checkServletRequestInjection(OneLocalWithBeanNamePropertyInjectionView.class);
    }

    /**
     * Tests that a {@link Stateless} single local interface view EJB JAX-RS
     * resource can have JAX-RS {@link Application} field injections.
     */
    @Test
    public void testLocalInterfaceWithBeanNameSingletonApplicationPropertyInjection()
                    throws Exception {
        checkApplicationInjection(OneLocalWithBeanNamePropertyInjectionView.class);
    }

    /*
     * The actual methods that check things
     */

    private void checkUriInfoInjection(Class<?> cls) throws Exception {
        final Method m = cls.getMethod("getUriInfoResource", (Class<?>[]) null);
        final String uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(cls).path(m).build().toString();
        Resource res = client.resource(uri);
        assertEquals(uri, res.get(String.class));
    }

    private void checkHttpHeadersInjection(Class<?> cls) throws Exception {
        final Method m = cls.getMethod("getHttpHeadersResource", new Class<?>[] { String.class });
        final String uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(cls).path(m).build().toString();
        final String headerName = "MyCustomHeader";
        final String expectedValue = "MyCustomValue";
        Resource res = client.resource(uri).header(headerName, expectedValue).queryParam("q", headerName);
        assertEquals(expectedValue, res.get(String.class));
    }

    private void checkProvidersInjection(Class<?> cls) throws Exception {
        final Method m = cls.getMethod("getProvidersResource", (Class<?>[]) null);
        final String uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(cls).path(m).build().toString();
        Resource res = client.resource(uri);
        assertEquals("Hello World!", res.get(String.class));
    }

    private void checkServletResponseInjection(Class<?> cls) throws Exception {
        final Method m = cls.getMethod("getProvidersResource", (Class<?>[]) null);
        final String uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(cls).path(m).build().toString();
        Resource res = client.resource(uri);
        assertEquals("Hello World!", res.get(String.class));
    }

    private void checkRequestInjection(Class<?> cls) throws Exception {
        final Method m = cls.getMethod("getRequestResource", (Class<?>[]) null);
        final String uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(cls).path(m).build().toString();
        Resource res = client.resource(uri).header(HttpHeaders.IF_NONE_MATCH, "\"myetagvalue\"");
        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), res.get().getStatusCode());

        res = client.resource(uri);
        ClientResponse resp = res.get();
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatusCode());
        assertEquals("Hello", resp.getEntity(String.class));
    }

    private void checkSecurityContextInjection(Class<?> cls) throws Exception {
        final Method m = cls.getMethod("getSecurityContextResource", (Class<?>[]) null);
        final String uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(cls).path(m).build().toString();

        Resource res = client.resource(uri);
        assertEquals("Is over https: false", res.get(String.class));
    }

    private void checkServletRequestInjection(Class<?> cls) throws Exception {
        final Method m = cls.getMethod("getServletRequestResource", (Class<?>[]) null);
        final String uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(cls).path(m).build().toString();

        Resource res = client.resource(uri);
        assertEquals("GET", res.get(String.class));
    }

    protected void checkApplicationInjection(Class<?> cls) throws Exception {
        final Method m = cls.getMethod("getApplicationClasses", (Class<?>[]) null);
        final String uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(cls).path(m).build().toString();

        Resource res = client.resource(uri);
        String response = res.get(String.class);
        assertTrue(response
                        .startsWith("com.ibm.ws.jaxrs.injection.ApplicationInjectionProxy") &&
                   response.contains("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.singleton.OneLocalInterfaceSingletonPropertyInjectionView")
                   && response.contains("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.singleton.OneLocalInterfaceSingletonFieldInjectionView")
                   && response.contains("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.withbeanname.OneLocalWithBeanNameFieldInjectionView")
                   && response.contains("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.withbeanname.OneLocalWithBeanNamePropertyInjectionView"));
    }

}

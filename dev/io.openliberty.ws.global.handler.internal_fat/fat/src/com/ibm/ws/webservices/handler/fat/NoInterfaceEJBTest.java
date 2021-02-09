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

import static junit.framework.Assert.assertEquals;
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
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.basic.StatelessFieldInjectionResource;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.basic.StatelessPersonAsEJB;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.basic.StatelessPersonViaJNDILookup;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.basic.StatelessPropertyInjectionResource;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.provider.MyTextResource;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.provider.NoInterfaceViewMyTextProvider;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.singleton.SingletonFieldInjectionResource;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.singleton.SingletonPersonAsEJB;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.singleton.SingletonPersonViaJNDILookup;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.singleton.SingletonPropertyInjectionResource;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.singletonwithbeanname.SingletonWithBeanNameEJBResource;
import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.withbeanname.StatelessWithBeanNameEJBResource;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class NoInterfaceEJBTest {
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
        ShrinkHelper.defaultDropinApp(server, ejbjaxrsinwar, "com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.*");
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
        server.stopServer("CWWKW1102W");
    }

    // testStatelessPersonViaJNDILookup - testSingleWithBeanName taken from
    // ibm-itest-ejb-jaxrs-in-war's NoInterfaceEJBTest
    private final String namePrefix = "Hello ";

    private final String nameSuffix = "!";

    /**
     * Tests defect 103091, that a {@link Stateless} EJB resource can invoke SecurityContext.
     */

    @Test
    public void testStatelessSecurityContext() {
        final String uri = TestUtils.getBaseTestUri(ejbjaxrsinwar, "statelessEJBWithJAXRSSecurityContextResource");
        Resource res = client.resource(uri);
        assertNotNull(res.get(String.class));
    }

    /**
     * Tests that a {@link Stateless} EJB JAX-RS resource can have basic Java EE
     * injections.
     */
    @Test
    public void testStatelessPersonAsEJBResource() {
        checkStatelessPersonBean(StatelessPersonAsEJB.class);
    }

    /**
     * Tests that a {@link Stateless} EJB resource can have basic Java EE
     * injections. If this test fails, then the entire injection is wrong. If
     * this test passes and "testStatelessPersonAsEJBResource" fails, then
     * there's something wrong with the JAX-RS EJB resources.
     */
    @Test
    public void testStatelessPersonViaJNDILookup() {
        checkStatelessPersonBean(StatelessPersonViaJNDILookup.class);
    }

    /**
     * Tests that a {@link Singleton} EJB as a JAX-RS EJB resource works as
     * expected. A counter increments on every invocation. If JAX-RS is not
     * getting it as an EJB singleton, then the resource is treated as request
     * scoped so the counter will never iterate past 1.
     */
    @Test
    public void testSingletonPersonAsEJBResource() {
        checkSingletonInvocations(SingletonPersonAsEJB.class);
    }

    /**
     * Tests that a {@link Singleton} EJB can be invoked via a JNDI resource
     * lookup. If this test fails, then EJB singletons may be not working. If
     * this passes,and "testSingletonPersonAsEJBResource" fails, then the JAX-RS
     * runtime may not be doing a proper lookup.
     */
    @Test
    public void testSingletonPersonViaJNDILookup() {
        checkSingletonInvocations(SingletonPersonViaJNDILookup.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB with a customized
     * bean name can be invoked.
     */
    @Test
    public void testStatelessWithBeanNameEJB() {
        Resource res = client.resource(UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(StatelessWithBeanNameEJBResource.class).build());
        ClientResponse resp = res.get();
        assertEquals(StatelessWithBeanNameEJBResource.class.getName() + " is what resource you visited.", resp.getEntity(String.class));
        assertEquals(200, resp.getStatusCode());
    }

    /**
     * Tests that a {@link Singleton} EJB with a customized bean name can be
     * invoked.
     */
    @Test
    public void testSingletonWithBeanNameEJB() {
        checkSingletonInvocations(SingletonWithBeanNameEJBResource.class);
    }

    private void checkStatelessPersonBean(Class<?> clazz) {
        final String name = "bob";
        // URL is something like http://localhost:8010/ejbjaxrsinwar/statelessPersonAsEJB/bob
        final String uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(clazz).build(name).toString();
        Resource res = client.resource(uri);
        assertEquals(namePrefix + name + nameSuffix, res.get(String.class));
    }

    private void checkSingletonInvocations(Class<?> clazz) {
        // reset the counter just in case this test messed up along the way and
        // this is being re-run
        // URL is something like http://localhost:8010/ejbjaxrsinwar/singletonPersonAsEJB
        final String uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(clazz).build().toString();
        Resource res = client.resource(uri);
        assertEquals(204, res.delete().getStatusCode());
        res = client.resource(UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(clazz).build());

        for (int c = 1; c < 101; ++c) {
            ClientResponse resp = res.get();
            assertEquals(String.valueOf(c), resp.getEntity(String.class));
            assertEquals(200, resp.getStatusCode());
        }
    }

    /**
     * Utility for creating the URI client uses to send request
     * specifically for tests from NoInterfaceProviderEJBTest
     * 
     * @param Includes scheme, host, port, and context root
     * @param Resource class
     * @return
     */
    private static String getProviderTestURL(String base) {
        UriBuilder builder = UriBuilder.fromUri(base).path(MyTextResource.class);
        return builder.build().toString();
    }

    // url1 for the JAX-RS service in MyTextResource
    // url2 for the JAX-WS service--I think???
    private static String jaxrsUrl = getProviderTestURL(TestUtils.getBaseTestUri(ejbjaxrsinwar));
    private static String jaxwsUrl = TestUtils.getBaseTestUri(ejbjaxrsinwar, "services/jaxws");

    // testProviderEJBStatelessPersonAsEJBResource - testProviderInjectionApplicationField
    // taken from ejb-jaxrs-in-war's NoInterfaceProviderEJBTest

    @Test
    public void testProviderEJBStatelessPersonAsEJBResource() {
        ClientResponse resp = client.resource(jaxrsUrl).contentType("my/text").header(NoInterfaceViewMyTextProvider.REQUEST_URL, jaxwsUrl).post("Hi there");
        assertEquals("My text is Hi there", resp.getEntity(String.class));
    }

    @Test
    public void testProviderInjectionURIInfoField() {
        ClientResponse resp = client.resource(jaxrsUrl).contentType("my/text").header("Test-Name", "uriinfo")
                        .header(NoInterfaceViewMyTextProvider.REQUEST_URL, jaxwsUrl).post("uriinfo test ");
        assertEquals("My text is uriinfo test " + jaxrsUrl, resp.getEntity(String.class));
    }

    @Test
    public void testProviderInjectionRequestField() {
        ClientResponse resp = client.resource(jaxrsUrl).contentType("my/text").header("Test-Name", "request")
                        .header(NoInterfaceViewMyTextProvider.REQUEST_URL, jaxwsUrl).post("request test ");
        assertEquals("My text is request test POST", resp.getEntity(String.class));
    }

    @Test
    public void testProviderInjectionSecurityContextField() {
        ClientResponse resp = client.resource(jaxrsUrl).contentType("my/text").header("Test-Name", "securitycontext")
                        .header(NoInterfaceViewMyTextProvider.REQUEST_URL, jaxwsUrl).post("securitycontext test ");
        assertEquals("My text is securitycontext test false", resp.getEntity(String.class));
    }

    @Test
    public void testProviderInjectionServletConfigField() {
        ClientResponse resp = client.resource(jaxrsUrl).contentType("my/text").
                        header("Test-Name", "servletconfig").header(NoInterfaceViewMyTextProvider.REQUEST_URL, jaxwsUrl).post("servletconfig test ");
        assertEquals("My text is servletconfig test javax.ws.rs.core.Application", resp.getEntity(String.class));
    }

    @Test
    public void testProviderInjectionProvidersField() {
        ClientResponse resp = client.resource(jaxrsUrl).contentType("my/text").header("Test-Name", "providers")
                        .header(NoInterfaceViewMyTextProvider.REQUEST_URL, jaxwsUrl).post("providers test ");
        assertEquals("My text is providers test org.apache.wink.server.internal.providers.exception.EJBAccessExceptionMapper", resp.getEntity(String.class));
    }

    @Test
    public void testProviderInjectionHttpServletRequestField() {
        ClientResponse resp = client.resource(jaxrsUrl).contentType("my/text").header("Test-Name", "httpservletrequest")
                        .header(NoInterfaceViewMyTextProvider.REQUEST_URL, jaxwsUrl).post("httpservletrequest test ");
        assertEquals("My text is httpservletrequest test my/text", resp.getEntity(String.class));
    }

    @Test
    public void testProviderInjectionHttpServletResponseField() {
        ClientResponse resp = client.resource(jaxrsUrl).contentType("my/text")
                        .header("Test-Name", "httpservletresponse")
                        .header(NoInterfaceViewMyTextProvider.REQUEST_URL, jaxwsUrl)
                        .post("httpservletresponse test");
        assertEquals("My text is httpservletresponse test", resp.getEntity(String.class));
        assertNotNull(resp.getHeaders().get("Test-Verification"));
        assertEquals(1, resp.getHeaders().get("Test-Verification").size());
        assertEquals("verification", resp.getHeaders().get("Test-Verification").get(0));
    }

    @Test
    public void testProviderInjectionServletContextField() {
        ClientResponse resp = client.resource(jaxrsUrl).contentType("my/text").header("Test-Name", "servletcontext")
                        .header(NoInterfaceViewMyTextProvider.REQUEST_URL, jaxwsUrl).post("servletcontext test ");
        //assertEquals("My text is servletcontext test " + ejbjaxrsinwar +".war", resp.getEntity(String.class));
        assertEquals("My text is servletcontext test " + ejbjaxrsinwar, resp.getEntity(String.class));
    }

    @Test
    public void testProviderInjectionApplicationField() {
        String response = client.resource(jaxrsUrl).contentType("my/text")
                        .header("Test-Name", "application")
                        .header(NoInterfaceViewMyTextProvider.REQUEST_URL, jaxwsUrl)
                        .post("application test ").getEntity(String.class);
        assertTrue(response
                        .startsWith("My text is application test com.ibm.ws.jaxrs.injection.ApplicationInjectionProxy") && response
                        .contains("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.provider.NoInterfaceViewMyTextProvider"));
    }

    // Remaining tests from NoInterfaceEJBJAXRSInjectionTest

    /**
     * Tests that the classes are in fact EJBs.
     */
    @Test
    public void testIsEJB() {
        assertTrue(StatelessFieldInjectionResource.class.getAnnotation(Stateless.class) != null);
        assertTrue(StatelessPropertyInjectionResource.class.getAnnotation(Stateless.class) != null);
        assertTrue(SingletonFieldInjectionResource.class.getAnnotation(Singleton.class) != null);
        assertTrue(SingletonPropertyInjectionResource.class.getAnnotation(Singleton.class) != null);
    }

    /*
     * Stateless Field Injection
     */

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link UriInfo} field injections.
     */
    @Test
    public void testNoInterfaceStatelessUriInfoFieldInjection() throws Exception {
        checkUriInfoInjection(StatelessFieldInjectionResource.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link HttpHeaders} field injections.
     */
    @Test
    public void testNoInterfaceStatelessHttpHeadersFieldInjection() throws Exception {
        checkHttpHeadersInjection(StatelessFieldInjectionResource.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link Providers} field injections.
     */
    @Test
    public void testNoInterfaceStatelessProvidersFieldInjection() throws Exception {
        checkProvidersInjection(StatelessFieldInjectionResource.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link HttpServletResponse} field injections.
     */
    @Test
    public void testNoInterfaceStatelessHttpServletResponseFieldInjection() throws Exception {
        checkServletResponseInjection(StatelessFieldInjectionResource.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link Request} field injections.
     */
    @Test
    public void testNoInterfaceStatelessRequestFieldInjection() throws Exception {
        checkRequestInjection(StatelessFieldInjectionResource.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link SecurityContext} field injections.
     */
    @Test
    public void testNoInterfaceStatelessSecurityContextFieldInjection() throws Exception {
        checkSecurityContextInjection(StatelessFieldInjectionResource.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link HttpServletRequest} field injections.
     */
    @Test
    public void testNoInterfaceStatelessServletRequestFieldInjection() throws Exception {
        checkServletRequestInjection(StatelessFieldInjectionResource.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link Application} field injections.
     */
    @Test
    public void testNoInterfaceStatelessApplicationFieldInjection() throws Exception {
        checkApplicationInjection(StatelessFieldInjectionResource.class);
    }

    /*
     * Stateless Property Injection
     */

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link UriInfo} property injections.
     */
    @Test
    public void testNoInterfaceStatelessUriInfoPropertyInjection() throws Exception {
        checkUriInfoInjection(StatelessPropertyInjectionResource.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link HttpHeaders} property injections.
     */
    @Test
    public void testNoInterfaceStatelessHttpHeadersPropertyInjection() throws Exception {
        checkHttpHeadersInjection(StatelessPropertyInjectionResource.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link HttpServletResponse} property injections.
     */
    @Test
    public void testNoInterfaceStatelessHttpServletResponsePropertyInjection() throws Exception {
        checkServletResponseInjection(StatelessPropertyInjectionResource.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link Providers} property injections.
     */
    @Test
    public void testNoInterfaceStatelessProvidersPropertyInjection() throws Exception {
        checkProvidersInjection(StatelessPropertyInjectionResource.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link Request} property injections.
     */
    @Test
    public void testNoInterfaceStatelessRequestPropertyInjection() throws Exception {
        checkRequestInjection(StatelessPropertyInjectionResource.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link SecurityContext} property injections.
     */
    @Test
    public void testNoInterfaceStatelessSecurityContextPropertyInjection() throws Exception {
        checkSecurityContextInjection(StatelessPropertyInjectionResource.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link HttpServletRequest} property injections.
     */
    @Test
    public void testNoInterfaceStatelessServletRequestPropertyInjection() throws Exception {
        checkServletRequestInjection(StatelessPropertyInjectionResource.class);
    }

    /**
     * Tests that a {@link Stateless} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link Application} property injections.
     */
    @Test
    public void testNoInterfaceStatelessApplicationPropertyInjection() throws Exception {
        checkApplicationInjection(StatelessPropertyInjectionResource.class);
    }

    /*
     * Singleton Field Injection
     */

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link UriInfo} field injections.
     */
    @Test
    public void testNoInterfaceSingletonUriInfoFieldInjection() throws Exception {
        checkUriInfoInjection(SingletonFieldInjectionResource.class);
    }

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link HttpHeaders} field injections.
     */
    @Test
    public void testNoInterfaceSingletonHttpHeadersFieldInjection() throws Exception {
        checkHttpHeadersInjection(SingletonFieldInjectionResource.class);
    }

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link Providers} field injections.
     */
    @Test
    public void testNoInterfaceSingletonProvidersFieldInjection() throws Exception {
        checkProvidersInjection(SingletonFieldInjectionResource.class);
    }

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link HttpServletResponse} field injections.
     */
    @Test
    public void testNoInterfaceSingletonHttpServletResponseFieldInjection() throws Exception {
        checkServletResponseInjection(SingletonFieldInjectionResource.class);
    }

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link Request} field injections.
     */
    @Test
    public void testNoInterfaceSingletonRequestFieldInjection() throws Exception {
        checkRequestInjection(SingletonFieldInjectionResource.class);
    }

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link SecurityContext} field injections.
     */
    @Test
    public void testNoInterfaceSingletonSecurityContextFieldInjection() throws Exception {
        checkSecurityContextInjection(SingletonFieldInjectionResource.class);
    }

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link HttpServletRequest} field injections.
     */
    @Test
    public void testNoInterfaceSingletonServletRequestFieldInjection() throws Exception {
        checkServletRequestInjection(SingletonFieldInjectionResource.class);
    }

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link Application} field injections.
     */
    @Test
    public void testNoInterfaceSingletonApplicationFielddInjection() throws Exception {
        checkApplicationInjection(SingletonFieldInjectionResource.class);
    }

    /*
     * Singleton Property Injection
     */

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link UriInfo} property injections.
     */
    @Test
    public void testNoInterfaceSingletonUriInfoPropertyInjection() throws Exception {
        checkUriInfoInjection(SingletonPropertyInjectionResource.class);
    }

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link HttpHeaders} property injections.
     */
    @Test
    public void testNoInterfaceSingletonHttpHeadersPropertyInjection() throws Exception {
        checkHttpHeadersInjection(SingletonPropertyInjectionResource.class);
    }

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link Providers} property injections.
     */
    @Test
    public void testNoInterfaceSingletonProvidersPropertyInjection() throws Exception {
        checkProvidersInjection(SingletonPropertyInjectionResource.class);
    }

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link HttpServletResponse} property injections.
     */
    @Test
    public void testNoInterfaceSingletonHttpServletResponsePropertyInjection() throws Exception {
        checkServletResponseInjection(SingletonPropertyInjectionResource.class);
    }

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link Request} property injections.
     */
    @Test
    public void testNoInterfaceSingletonRequestPropertyInjection() throws Exception {
        checkRequestInjection(SingletonPropertyInjectionResource.class);
    }

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link SecurityContext} property injections.
     */
    @Test
    public void testNoInterfaceSingletonSecurityContextPropertyInjection() throws Exception {
        checkSecurityContextInjection(SingletonPropertyInjectionResource.class);
    }

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link HttpServletRequest} property injections.
     */
    @Test
    public void testNoInterfaceSingletonServletRequestPropertyInjection() throws Exception {
        checkServletRequestInjection(SingletonPropertyInjectionResource.class);
    }

    /**
     * Tests that a {@link Singleton} no-interface view EJB JAX-RS resource can
     * have JAX-RS {@link Application} property injections.
     */
    @Test
    public void testNoInterfaceSingletonApplicationPropertyInjection() throws Exception {
        checkApplicationInjection(SingletonPropertyInjectionResource.class);
    }

    /*
     * The actual methods that check things
     */

    private void checkUriInfoInjection(Class<?> cls) throws Exception {
        final Method m = cls.getMethod("getRequestURIResource", (Class<?>[]) null);
        // URL is something like http://localhost:8010/ejbjaxrsinwar/statelessEJBWithJAXRSFieldInjectionResource/uriinfo
        final String uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(cls).path(m).build().toString();
        Resource res = client.resource(uri);
        assertEquals(uri, res.get(String.class));
    }

    private void checkHttpHeadersInjection(Class<?> cls) throws Exception {
        final Method m = cls.getMethod("getHttpHeaderResource", new Class<?>[] { String.class });
        final String uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(cls).path(m).build().toString();
        final String headerName = "MyCustomHeader";
        final String expectedValue = "MyCustomValue";
        Resource res =
                        client.resource(uri).header(headerName, expectedValue).queryParam("q", headerName);
        assertEquals(expectedValue, res.get(String.class));
    }

    private void checkProvidersInjection(Class<?> cls) throws Exception {
        final Method m = cls.getMethod("getProvidersResponseResource", (Class<?>[]) null);
        final String uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(ejbjaxrsinwar)).path(cls).path(m).build().toString();
        Resource res = client.resource(uri);
        assertEquals("Hello World!", res.get(String.class));
    }

    private void checkServletResponseInjection(Class<?> cls) throws Exception {
        final Method m = cls.getMethod("getProvidersResponseResource", (Class<?>[]) null);
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
        System.out.println("application class: " + response);
        assertTrue(response.startsWith("com.ibm.ws.jaxrs.injection.ApplicationInjectionProxy")
                   && response.contains("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.basic.StatelessFieldInjectionResource")
                   && response.contains("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.basic.StatelessPropertyInjectionResource")
                   && response.contains("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.singleton.SingletonFieldInjectionResource")
                   && response.contains("com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.singleton.SingletonPropertyInjectionResource"));
    }
}

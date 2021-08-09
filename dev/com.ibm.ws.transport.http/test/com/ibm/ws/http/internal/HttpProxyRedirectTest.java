/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

/**
 *
 */
public class HttpProxyRedirectTest {

    private Mockery context;

    private final List<HttpProxyRedirect> httpProxyRedirects = new ArrayList<HttpProxyRedirect>();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        context = new Mockery();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        for (HttpProxyRedirect hpr : httpProxyRedirects) {
            hpr.deactivate(null);
        }
    }

    @Test
    public void testGetDefaultRedirectPort() {

        // check against wildcard
        Integer redirectPort = HttpProxyRedirect.getRedirectPort("*", 80);
        assertNotNull("Could not find default redirect port for *:80", redirectPort);
        assertEquals("Did not return expected redirect port of 443 for *:80", 443, (int) redirectPort);

        // now check against a specific host
        redirectPort = HttpProxyRedirect.getRedirectPort("localhost", 80);
        assertNotNull("Could not find default redirect port for localhost:80", redirectPort);
        assertEquals("Did not return expected redirect port of 443 for localhost:80", 443, (int) redirectPort);

        // now add a non-default proxy redirect and verify that the default redirect still exists
        getRedirectPort_wildcardHost();
        redirectPort = HttpProxyRedirect.getRedirectPort("*", 80);
        assertNotNull("Could not find default redirect port for *:80 when it should have been found", redirectPort);
        assertEquals("Did not return expected redirect port of 443 for *:80", 443, (int) redirectPort);

        redirectPort = HttpProxyRedirect.getRedirectPort("localhost", 80);
        assertNotNull("Could not find default redirect port for localhost:80 when it should have been found", redirectPort);
        assertEquals("Did not return expected redirect port of 443 for localhost:80", 443, (int) redirectPort);
    }

    @Test
    public void testGetRedirectPort_wildcardHost() {
        getRedirectPort_wildcardHost();
    }

    private HttpProxyRedirect getRedirectPort_wildcardHost() {
        HttpProxyRedirect hpr = new HttpProxyRedirect();
        httpProxyRedirects.add(hpr);
        ComponentContext cc = mockComponentContext("wildcardHost", true, "*", 4444, 5555);
        hpr.activate(cc);

        // check against the exact host specified in the config (wildcard)
        Integer redirectPort = HttpProxyRedirect.getRedirectPort("*", 4444);
        assertNotNull("Could not find redirect port for *:4444", redirectPort);
        assertEquals("Did not return expected redirect port for *:4444", 5555, (int) redirectPort);

        // now check against a specific host
        redirectPort = HttpProxyRedirect.getRedirectPort("myhost.ibm.com", 4444);
        assertNotNull("Could not find redirect port for myhost:4444", redirectPort);
        assertEquals("Did not return expected redirect port for myhost:4444", 5555, (int) redirectPort);

        return hpr;
    }

    @Test
    public void testGetRedirectPort_specificHost() {
        getRedirectPort_specificHost();
    }

    private HttpProxyRedirect getRedirectPort_specificHost() {
        HttpProxyRedirect hpr = new HttpProxyRedirect();
        httpProxyRedirects.add(hpr);
        ComponentContext cc = mockComponentContext("specificHost", true, "myhost.ibm.com", 6666, 7777);
        hpr.activate(cc);

        // check against the exact host specified in the config (wildcard)
        Integer redirectPort = HttpProxyRedirect.getRedirectPort("myhost.ibm.com", 6666);
        assertNotNull("Could not find redirect port for myhost.ibm.com:6666", redirectPort);
        assertEquals("Did not return expected redirect port for myhost.ibm.com:6666", 7777, (int) redirectPort);

        // now check that a different host fails to be found
        redirectPort = HttpProxyRedirect.getRedirectPort("myOtherHost.ibm.com", 6666);
        assertNull("Found an unexpected redirect port when none should have been found", redirectPort);

        return hpr;
    }

    @Test
    public void testGetRedirectPort_afterDeactivation() {
        // as a pre-condition, create proxy redirects and confirm that they work
        HttpProxyRedirect hpr1 = getRedirectPort_wildcardHost();
        httpProxyRedirects.add(hpr1);
        HttpProxyRedirect hpr2 = getRedirectPort_specificHost();
        httpProxyRedirects.add(hpr2);

        // now deactivate one
        hpr1.deactivate(null);
        httpProxyRedirects.remove(hpr1);

        //verify that both the wildcard and specific host lookup fails:
        Integer redirectPort = HttpProxyRedirect.getRedirectPort("*", 4444);
        assertNull("Found an unexpected redirect port when none should have been found", redirectPort);
        redirectPort = HttpProxyRedirect.getRedirectPort("myhost.ibm.com", 4444);
        assertNull("Found an unexpected redirect port when none should have been found", redirectPort);

        //verify that the second redirect is still working
        redirectPort = HttpProxyRedirect.getRedirectPort("myhost.ibm.com", 6666);
        assertNotNull("Could not find redirect port for *:6666", redirectPort);
        assertEquals("Did not return expected redirect port for *:6666", 7777, (int) redirectPort);

        // now deactivate the second one and verify that it too is gone
        hpr2.deactivate(null);
        httpProxyRedirects.remove(hpr2);
        redirectPort = HttpProxyRedirect.getRedirectPort("myhost.ibm.com", 6666);
        assertNull("Found an unexpected redirect port when none should have been found", redirectPort);

        //now that all configured redirects are deactivate, verify that the default redirect still works
        redirectPort = HttpProxyRedirect.getRedirectPort("*", 80);
        assertNotNull("Could not find default redirect port for *:80", redirectPort);
        assertEquals("Did not return expected redirect port of 443 for *:80", 443, (int) redirectPort);
        redirectPort = HttpProxyRedirect.getRedirectPort("localhost", 80);
        assertNotNull("Could not find default redirect port for localhost:80", redirectPort);
        assertEquals("Did not return expected redirect port of 443 for localhost:80", 443, (int) redirectPort);
    }

    @Test
    public void testModifyProxyRedirect() {
        // as a pre-condition, create a proxy redirect and confirm that it works
        HttpProxyRedirect hpr = getRedirectPort_wildcardHost();
        httpProxyRedirects.add(hpr);

        // now modify it by changing from a wildcard host to a specific hostname:
        Map<String, Object> configProps = new Hashtable<String, Object>();
        configProps.put(HttpProxyRedirect.PROP_ENABLED, true);
        configProps.put(HttpProxyRedirect.PROP_HOST, "mySpecificHost");
        configProps.put(HttpProxyRedirect.PROP_HTTP_PORT, 4444);
        configProps.put(HttpProxyRedirect.PROP_HTTPS_PORT, 5555);
        hpr.modified(configProps);

        // now verify that the specified host works and a different host fails
        Integer redirectPort = HttpProxyRedirect.getRedirectPort("mySpecificHost", 4444);
        assertNotNull("Could not find redirect port for mySpecificHost:4444", redirectPort);
        assertEquals("Did not return expected redirect port for mySpecificHost:4444", 5555, (int) redirectPort);
        // now check that a different host fails to be found
        redirectPort = HttpProxyRedirect.getRedirectPort("myOtherHost", 4444);
        assertNull("Found an unexpected redirect port when none should have been found", redirectPort);

        //now lets modify the secure port
        configProps.put(HttpProxyRedirect.PROP_HTTPS_PORT, 8888);
        hpr.modified(configProps);
        //and verify that we get the newly modified secure port
        redirectPort = HttpProxyRedirect.getRedirectPort("mySpecificHost", 4444);
        assertNotNull("Could not find redirect port for mySpecificHost:4444", redirectPort);
        assertEquals("Did not return expected redirect port for mySpecificHost:4444", 8888, (int) redirectPort);

        //now lets modify the non-secure (key) port
        configProps.put(HttpProxyRedirect.PROP_HTTP_PORT, 9999);
        hpr.modified(configProps);

        //and verify that we get the right secure port for the new key, and fail to get the secure port for the old key
        redirectPort = HttpProxyRedirect.getRedirectPort("mySpecificHost", 9999);
        assertNotNull("Could not find redirect port for mySpecificHost:9999", redirectPort);
        assertEquals("Did not return expected redirect port for mySpecificHost:9999", 8888, (int) redirectPort);
        redirectPort = HttpProxyRedirect.getRedirectPort("mySpecificHost", 4444);
        assertNull("Found an unexpected redirect port when none should have been found, since the key was modified", redirectPort);

        //now lets change everything at once, and see if it works :-)
        configProps.put(HttpProxyRedirect.PROP_HOST, "mySpecificHost2");
        configProps.put(HttpProxyRedirect.PROP_HTTP_PORT, 12345);
        configProps.put(HttpProxyRedirect.PROP_HTTPS_PORT, 23456);
        hpr.modified(configProps);

        //and verify
        redirectPort = HttpProxyRedirect.getRedirectPort("mySpecificHost2", 12345);
        assertNotNull("Could not find redirect port for mySpecificHost2:12345", redirectPort);
        assertEquals("Did not return expected redirect port for mySpecificHost2:12345", 23456, (int) redirectPort);
        redirectPort = HttpProxyRedirect.getRedirectPort("mySpecificHost", 9999);
        assertNull("Found an unexpected redirect port when none should have been found, since the key was modified", redirectPort);
    }

    @Test
    public void testEnableAndDisable() {
        // first, lets disable the default proxy redirect
        HttpProxyRedirect hpr = new HttpProxyRedirect();
        httpProxyRedirects.add(hpr);
        hpr.activate(mockComponentContext("disableDefault", false, "*", 80, 443));
        //and verify that the default no longer exists:
        Integer redirectPort = HttpProxyRedirect.getRedirectPort("*", 80);
        assertNull("Default proxy redirect still works even when disabled", redirectPort);

        //now deactivate the disabled the default redirect and verify that it now works
        hpr.deactivate(null);
        httpProxyRedirects.remove(hpr);
        redirectPort = HttpProxyRedirect.getRedirectPort("*", 80);
        assertNotNull("Default proxy redirect is not found after disabling the redirect that disabled it", redirectPort);
        assertEquals("Default proxy redirect points to wrong port after getting re-enabled", 443, (int) redirectPort);

    }

    @Test
    public void testDisabledRedirect() {
        HttpProxyRedirect hpr = new HttpProxyRedirect();
        httpProxyRedirects.add(hpr);
        ComponentContext cc = mockComponentContext("wildcardHost", false, "*", 4444, 5555);
        hpr.activate(cc);

        // check against the exact host specified in the config (wildcard)
        Integer redirectPort = HttpProxyRedirect.getRedirectPort("*", 4444);
        assertNull("Found redirect port for *:4444 when it should not be found", redirectPort);

        // now check against a specific host
        redirectPort = HttpProxyRedirect.getRedirectPort("myhost.ibm.com", 4444);
        assertNull("Found redirect port for myhost:4444 when it should not be found", redirectPort);

        // Test default port
        redirectPort = HttpProxyRedirect.getRedirectPort("*", 80);
        assertNotNull("Could not find default redirect port for *:80", redirectPort);
        assertEquals("Did not return expected redirect port of 443 for *:80", 443, (int) redirectPort);

        // now check default port against a specific host
        redirectPort = HttpProxyRedirect.getRedirectPort("localhost", 80);
        assertNotNull("Could not find default redirect port for localhost:80", redirectPort);
        assertEquals("Did not return expected redirect port of 443 for localhost:80", 443, (int) redirectPort);

    }

    private ComponentContext mockComponentContext(String id, final boolean enabled, final String host, final int httpPort, final int httpsPort) {
        final Dictionary<String, Object> configProps = new Hashtable<String, Object>();
        configProps.put(HttpProxyRedirect.PROP_ENABLED, enabled);
        configProps.put(HttpProxyRedirect.PROP_HOST, host);
        configProps.put(HttpProxyRedirect.PROP_HTTP_PORT, httpPort);
        configProps.put(HttpProxyRedirect.PROP_HTTPS_PORT, httpsPort);

        final ComponentContext cc = context.mock(ComponentContext.class, id);
        context.checking(new Expectations() {
            {
                allowing(cc).getProperties();
                will(returnValue(configProps));
            }
        });
        return cc;
    }
}

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
package com.ibm.ws.http.dispatcher.internal;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink;
import com.ibm.ws.http.dispatcher.internal.channel.HttpRequestImpl;
import com.ibm.wsspi.http.VirtualHostListener;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import junit.framework.Assert;
import test.common.SharedOutputManager;

/**
 *
 */
public class HttpDispatcherTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");

    static Field dispatcherInstance;
    static Field linkRequest;
    static Field linkRemoteContextAddress;
    static Field linkUsePrivateHeaders;

    @BeforeClass
    public static void setupInstance() throws Exception {
        dispatcherInstance = HttpDispatcher.class.getDeclaredField("instance");
        dispatcherInstance.setAccessible(true);

        linkRequest = HttpDispatcherLink.class.getDeclaredField("request");
        linkRequest.setAccessible(true);

        linkRemoteContextAddress = HttpDispatcherLink.class.getDeclaredField("remoteContextAddress");
        linkRemoteContextAddress.setAccessible(true);

        linkUsePrivateHeaders = HttpDispatcherLink.class.getDeclaredField("usePrivateHeaders");
        linkUsePrivateHeaders.setAccessible(true);
    }

    @AfterClass
    public static void teardownInstance() throws Exception {
        // cleanup on the way out.
        clearDispatcher();
    }

    private static void clearDispatcher() throws Exception {
        ((AtomicReference<HttpDispatcher>) dispatcherInstance.get(null)).set(null);
    }

    private static HttpDispatcher getDispatcher() throws Exception {
        return ((AtomicReference<HttpDispatcher>) dispatcherInstance.get(null)).get();
    }

    @Rule
    public TestRule rule = outputMgr;

    @Rule
    public TestName name = new TestName();

    @Before
    public void setUp() throws Exception {
        // make sure the default instance is clear before we start anything..
        clearDispatcher();
    }

    final Mockery context = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    final ServiceReference<VirtualHostListener> mockWCRef = context.mock(ServiceReference.class, "WebContainer");

    public static Map<String, Object> buildMap(boolean enableWelcomePage, String appOrContextRootMissingMessage, String trustedHeaderOrigin, String trustedSensitiveHeaderOrigin) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(HttpDispatcher.PROP_ENABLE_WELCOME_PAGE, enableWelcomePage);
        map.put(HttpDispatcher.PROP_TRUSTED_PRIVATE_HEADER_ORIGIN, trustedHeaderOrigin);
        map.put(HttpDispatcher.PROP_TRUSTED_SENSITIVE_HEADER_ORIGIN, trustedSensitiveHeaderOrigin);
        map.put(HttpDispatcher.PROP_VHOST_NOT_FOUND, appOrContextRootMissingMessage);
        return map;
    }

    @Test
    public void testDefaults() throws Exception {
        String wsprHeader = HttpHeaderKeys.HDR_$WSPR.getName();
        String wsraHeader = HttpHeaderKeys.HDR_$WSRA.getName();
        HttpDispatcher d = new HttpDispatcher();
        Map<String, Object> map = buildMap(true, null, (String) null, (String) null);
        d.activate(map);

        // the static instance value should be set to the newly activated dispatcher instance.
        Assert.assertSame("Dispatcher d should be set as active instance", d, getDispatcher());

        Assert.assertNull("Null value should be returned for missing context message", HttpDispatcher.getContextRootNotFoundMessage());
        Assert.assertTrue("Welcome page should be enabled", HttpDispatcher.isWelcomePageEnabled());

        // There is no webcontainer reference assigned w/ different properties, and
        // the trusted header origin was passed in as null, which should get defaulted to '*'
        Assert.assertTrue("Private headers should be enabled", HttpDispatcher.usePrivateHeaders("a.b.c", wsprHeader));
        Assert.assertFalse("Sensitive private headers should not be enabled", HttpDispatcher.usePrivateHeaders("a.b.c", wsraHeader));

        String missing = "missing context";
        map = buildMap(true, missing, "*", "*");
        HttpDispatcher d1 = new HttpDispatcher();
        d1.activate(map);

        // this should have no effect: as instance should have been re-set to d1 already
        d.deactivate(map, 0);

        // the static instance value should be set to the newly activated dispatcher instance.
        Assert.assertSame("Dispatcher d1 should be set as active instance", d1, getDispatcher());

        Assert.assertSame("Configured value should be returned for missing context message", missing, HttpDispatcher.getContextRootNotFoundMessage());
        Assert.assertTrue("Welcome page should be enabled", HttpDispatcher.isWelcomePageEnabled());

        // both private header lists have been set to "*"; all private headers are allowed
        Assert.assertTrue("Private headers should be enabled", HttpDispatcher.usePrivateHeaders("a.b.c", wsprHeader));
        Assert.assertTrue("Sensitive private headers should be enabled", HttpDispatcher.usePrivateHeaders("a.b.c", wsraHeader));

        d1.deactivate(map, 0);
        // the static instance value should be set to the newly activated dispatcher instance.
        Assert.assertNull("Dispatcher d1 should be removed as active instance", getDispatcher());

        // Now that there is no instance, there should be some default behavior for shutdown:
        Assert.assertNull("No default missing-context message", HttpDispatcher.getContextRootNotFoundMessage());
        Assert.assertFalse("Do not show welcome page when there is no dispatcher", HttpDispatcher.isWelcomePageEnabled());
        Assert.assertTrue("Trust private headers by default", HttpDispatcher.usePrivateHeaders("a.b.c", wsprHeader));
        Assert.assertFalse("Don't trust sensitive private headers by default", HttpDispatcher.usePrivateHeaders("a.b.c", wsraHeader));
    }

    @Test
    public void testRestrictPrivateHeaders() throws Exception {
        String wsprHeader = HttpHeaderKeys.HDR_$WSPR.getName();
        HttpDispatcher d = new HttpDispatcher();
        Map<String, Object> map = buildMap(true, null, "none", "none");
        d.activate(map);

        // both trusted header properties are set to "none", so no private headers are allowed from any host
        Assert.assertFalse("Private headers should be disabled for a.b.c", HttpDispatcher.usePrivateHeaders("a.b.c", wsprHeader));
        Assert.assertFalse("Private headers should be disabled for d.e.f", HttpDispatcher.usePrivateHeaders("d.e.f", wsprHeader));

        // "d.e.f" is now trusted with sensitive private headers
        // note the sensitive header list overrides the private header list, so "d.e.f" can pass either
        map = buildMap(true, null, "none", "d.e.f");
        d.modified(map);
        Assert.assertFalse("Private headers should be disabled for a.b.c", HttpDispatcher.usePrivateHeaders("a.b.c", wsprHeader));
        Assert.assertTrue("Private headers should be enabled for d.e.f", HttpDispatcher.usePrivateHeaders("d.e.f", wsprHeader));

        // set the WC "trusted" property to false; all private headers are disabled for all hosts
        // note the WC "trusted" property takes precedence over both of the dispatcher's private header properties
        context.checking(new Expectations() {
            {
                allowing(mockWCRef).getProperty("trusted");
                will(returnValue(false));
            }
        });

        try {
            d.setWebContainer(mockWCRef);
            Assert.assertFalse("Private headers should be disabled for a.b.c", HttpDispatcher.usePrivateHeaders("a.b.c", wsprHeader));
            Assert.assertFalse("Private headers should be enabled for d.e.f", HttpDispatcher.usePrivateHeaders("d.e.f", wsprHeader));
        } finally {
            context.assertIsSatisfied();
        }
    }

    @Test
    public void testGetRequestedHost() throws Exception {
        final String sourceAddr = "1.2.3.4";
        final String host = "a.b.c.d";
        final HttpRequestImpl mockRequest = context.mock(HttpRequestImpl.class);

        // No dispatcher, no private header
        context.checking(new Expectations() {
            {
                one(mockRequest).getHeader(HttpHeaderKeys.HDR_$WSSN.getName());
                will(returnValue(null));

                one(mockRequest).getVirtualHost();
                will(returnValue(host));
            }
        });

        HttpDispatcherLink link = new HttpDispatcherLink();
        linkRequest.set(link, mockRequest);
        linkRemoteContextAddress.set(link, sourceAddr);
        final Object unknownPrivateHeader = linkUsePrivateHeaders.get(link);

        try {
            Assert.assertSame("No registered dispatcher: should return the result of getVirtualHost",
                              host, link.getRequestedHost());
        } finally {
            context.assertIsSatisfied();
        }

        // No dispatcher, $WSSN header
        context.checking(new Expectations() {
            {
                one(mockRequest).getHeader(HttpHeaderKeys.HDR_$WSSN.getName());
                will(returnValue(host));
            }
        });

        try {
            Assert.assertSame("No registered dispatcher, but $WSSN header present: should return the header value",
                              host, link.getRequestedHost());
        } finally {
            context.assertIsSatisfied();
        }

        // Registered dispatcher, trust all headers
        HttpDispatcher d = new HttpDispatcher();
        d.activate(buildMap(true, null, "*", "*"));

        context.checking(new Expectations() {
            {
                one(mockRequest).getHeader(HttpHeaderKeys.HDR_$WSSN.getName());
                will(returnValue(host));
            }
        });

        try {
            Assert.assertSame("Registered dispatcher, trusted $WSSN header present: should return the header value",
                              host, link.getRequestedHost());
        } finally {
            context.assertIsSatisfied();
        }

        // Registered dispatcher, trust header from non-matching source (no invocation of getHeader)
        d.modified(buildMap(true, null, "2.3.4.5", "2.3.4.5"));

        // reset the cached value of use private headers for the connection
        // so it re-calculates
        linkUsePrivateHeaders.set(link, unknownPrivateHeader);

        context.checking(new Expectations() {
            {
                one(mockRequest).getVirtualHost();
                will(returnValue(host));
            }
        });

        try {
            Assert.assertSame("Registered dispatcher, no trusted headers: should return the value from getVirtualHost",
                              host, link.getRequestedHost());
        } finally {
            context.assertIsSatisfied();
        }

        // Registered dispatcher, trust header from matching source
        d.modified(buildMap(true, null, "1.2.3.4", "*"));

        // reset the cached value of use private headers for the connection
        // so it re-calculates
        linkUsePrivateHeaders.set(link, unknownPrivateHeader);

        context.checking(new Expectations() {
            {
                one(mockRequest).getHeader(HttpHeaderKeys.HDR_$WSSN.getName());
                will(returnValue(host));
            }
        });

        try {
            Assert.assertSame("Registered dispatcher, no trusted headers: should return the value from getVirtualHost",
                              host, link.getRequestedHost());
        } finally {
            context.assertIsSatisfied();
        }

        // Registered dispatcher, trust no headers
        d.modified(buildMap(true, null, "none", "none"));

        // reset the cached value of use private headers for the connection
        // so it re-calculates
        linkUsePrivateHeaders.set(link, unknownPrivateHeader);

        context.checking(new Expectations() {
            {
                one(mockRequest).getVirtualHost();
                will(returnValue(host));
            }
        });

        try {
            Assert.assertSame("Registered dispatcher, no trusted headers: should return the value from getVirtualHost",
                              host, link.getRequestedHost());
        } finally {
            context.assertIsSatisfied();
        }
    }

    @Test
    public void testGetRequestedPort() throws Exception {
        final String sourceAddr = "1.2.3.4";
        final String portStr = "1234";
        final HttpRequestImpl mockRequest = context.mock(HttpRequestImpl.class);

        // No dispatcher, no private headers
        context.checking(new Expectations() {
            {
                one(mockRequest).getHeader(HttpHeaderKeys.HDR_$WSSP.getName());
                will(returnValue(null));

                one(mockRequest).getVirtualPort();
                will(returnValue(1234));
            }
        });

        HttpDispatcherLink link = new HttpDispatcherLink();
        linkRequest.set(link, mockRequest);
        linkRemoteContextAddress.set(link, sourceAddr);
        final Object unknownPrivateHeader = linkUsePrivateHeaders.get(link);

        try {
            Assert.assertEquals("No registered dispatcher, no headers: should return the result of getVirtualPort",
                                1234, link.getRequestedPort());
        } finally {
            context.assertIsSatisfied();
        }

        // No dispatcher, no private headers
        context.checking(new Expectations() {
            {
                one(mockRequest).getHeader(HttpHeaderKeys.HDR_$WSSP.getName());
                will(returnValue(null));

                one(mockRequest).getVirtualPort();
                will(returnValue(-1));

                one(mockRequest).getHeader(HttpHeaderKeys.HDR_HOST.getName());
                will(returnValue(""));

                one(mockRequest).getScheme();
                will(returnValue("http"));
            }
        });

        try {
            Assert.assertEquals("No registered dispatcher, no headers: should return the result of getVirtualPort",
                                80, link.getRequestedPort());
        } finally {
            context.assertIsSatisfied();
        }

        // No dispatcher, WSSP: 1234
        context.checking(new Expectations() {
            {
                one(mockRequest).getHeader(HttpHeaderKeys.HDR_$WSSP.getName());
                will(returnValue("1234"));
            }
        });

        try {
            Assert.assertEquals("No registered dispatcher, WSSP",
                                1234, link.getRequestedPort());
        } finally {
            context.assertIsSatisfied();
        }

        // Registered dispatcher, trust all headers
        HttpDispatcher d = new HttpDispatcher();
        d.activate(buildMap(true, null, "none", "none"));

        // reset the cached value of use private headers for the connection
        // so it re-calculates
        linkUsePrivateHeaders.set(link, unknownPrivateHeader);

        context.checking(new Expectations() {
            {
                one(mockRequest).getVirtualPort();
                will(returnValue(-1));

                one(mockRequest).getHeader(HttpHeaderKeys.HDR_HOST.getName());
                will(returnValue(""));

                one(mockRequest).getScheme();
                will(returnValue("https"));

            }
        });

        try {
            Assert.assertEquals("No registered dispatcher, WSSP",
                                443, link.getRequestedPort());
        } finally {
            context.assertIsSatisfied();
        }
    }
}

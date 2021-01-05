/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
import java.net.Inet6Address;
import java.net.InetAddress;
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
    static Field linkRemoteAddress;
    final String wsprHeader = HttpHeaderKeys.HDR_$WSPR.getName();
    final String wsraHeader = HttpHeaderKeys.HDR_$WSRA.getName();

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

        linkRemoteAddress = HttpDispatcherLink.class.getDeclaredField("remoteAddress");
        linkRemoteAddress.setAccessible(true);
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
        HttpDispatcher d = new HttpDispatcher();
        Map<String, Object> map = buildMap(true, null, (String) null, (String) null);
        d.activate(map);

        // the static instance value should be set to the newly activated dispatcher instance.
        Assert.assertSame("Dispatcher d should be set as active instance", d, getDispatcher());

        Assert.assertNull("Null value should be returned for missing context message", HttpDispatcher.getContextRootNotFoundMessage());
        Assert.assertTrue("Welcome page should be enabled", HttpDispatcher.isWelcomePageEnabled());

        InetAddress testAddr = InetAddress.getByAddress("hostname", InetAddress.getByName("1.2.3.4").getAddress());

        // There is no webcontainer reference assigned w/ different properties, and
        // the trusted header origin was passed in as null, which should get defaulted to '*'
        Assert.assertTrue("Private headers should be enabled", HttpDispatcher.usePrivateHeaders(testAddr, wsprHeader));
        Assert.assertFalse("Sensitive private headers should not be enabled", HttpDispatcher.usePrivateHeaders(testAddr, wsraHeader));

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
        Assert.assertTrue("Private headers should be enabled", HttpDispatcher.usePrivateHeaders(testAddr, wsprHeader));
        Assert.assertTrue("Sensitive private headers should be enabled", HttpDispatcher.usePrivateHeaders(testAddr, wsraHeader));

        d1.deactivate(map, 0);
        // the static instance value should be set to the newly activated dispatcher instance.
        Assert.assertNull("Dispatcher d1 should be removed as active instance", getDispatcher());

        // Now that there is no instance, there should be some default behavior for shutdown:
        Assert.assertNull("No default missing-context message", HttpDispatcher.getContextRootNotFoundMessage());
        Assert.assertFalse("Do not show welcome page when there is no dispatcher", HttpDispatcher.isWelcomePageEnabled());
        Assert.assertTrue("Trust private headers by default", HttpDispatcher.usePrivateHeaders(testAddr, wsprHeader));
        Assert.assertFalse("Don't trust sensitive private headers by default", HttpDispatcher.usePrivateHeaders(testAddr, wsraHeader));
    }

    @Test
    public void testRestrictPrivateHeaders() throws Exception {
        InetAddress testAddr1 = InetAddress.getByAddress("hostname", InetAddress.getByName("1.2.3.4").getAddress());
        InetAddress testAddr2 = InetAddress.getByAddress("hostname", InetAddress.getByName("5.6.7.8").getAddress());
        InetAddress testAddr3 = InetAddress.getByAddress("IBM.com", InetAddress.getByName("1.2.3.4").getAddress());
        InetAddress testAddr4 = InetAddress.getByAddress("fake.com", InetAddress.getByName("127.0.0.1").getAddress());
        InetAddress testAddr5 = InetAddress.getByAddress("fake.com", InetAddress.getByName("5.6.7.8").getAddress());
        InetAddress testAddr6 = InetAddress.getByAddress("fake.com", InetAddress.getByName("5.6.7.8").getAddress());
        InetAddress testAddr7 = InetAddress.getByAddress("fake.com", InetAddress.getByName("1.2.3.4").getAddress());
        InetAddress testAddr8 = InetAddress.getByAddress("fake.com", InetAddress.getByName("1.2.3.5").getAddress());
        InetAddress testAddr9 = InetAddress.getByAddress("fake.com", InetAddress.getByName("127.0.0.2").getAddress());
        InetAddress testAddr10 = InetAddress.getByAddress("fake.com", InetAddress.getByName("192.168.10.0").getAddress());
        InetAddress testAddr11 = InetAddress.getByAddress("fake.com", InetAddress.getByName("192.168.10.100").getAddress());
        InetAddress testAddr12 = InetAddress.getByAddress("fake.com", InetAddress.getByName("192.168.10.255").getAddress());
        final Inet6Address testAddr13 = (Inet6Address) InetAddress.getByAddress("fake.com", InetAddress.getByName("d:e:a:d:0:0:0:0").getAddress());
        final Inet6Address testAddr14 = (Inet6Address) InetAddress.getByAddress("fake.com", InetAddress.getByName("d:e:a:d:0:0:0:1").getAddress());
        final Inet6Address testAddr15 = (Inet6Address) InetAddress.getByAddress("fake.com", InetAddress.getByName("d:e:a:d:0:0:0:2").getAddress());
        final Inet6Address testAddr16 = (Inet6Address) InetAddress.getByAddress("fake.com", InetAddress.getByName("d:e:a:d:0:0:0:3").getAddress());
        final Inet6Address testAddr17 = (Inet6Address) InetAddress.getByAddress("fake.com", InetAddress.getByName("d:e:a:d:0:0:1:0").getAddress());
        InetAddress testAddr18 = InetAddress.getByAddress("fake.com", InetAddress.getByName("192.168.9.0").getAddress());
        InetAddress testAddr19 = InetAddress.getByAddress("fake.com", InetAddress.getByName("192.168.9.255").getAddress());
        InetAddress testAddr20 = InetAddress.getByAddress("fake.com", InetAddress.getByName("127.0.0.3").getAddress());
        HttpDispatcher d = new HttpDispatcher();

        // both trusted header properties are set to "none", so no private headers are allowed from any host
        Map<String, Object> map = buildMap(true, null, "none", "none");
        d.activate(map);
        Assert.assertFalse("Private headers should be disabled for " + testAddr1.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr1, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr2.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr2, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr3.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr3, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr4.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr4, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr5.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr5, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr6.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr6, wsprHeader));

        // "5.6.7.8" is now trusted with sensitive private headers
        // note the sensitive header list overrides the private header list, so "5.6.7.8" can pass either
        map = buildMap(true, null, "none", "5.6.7.8");
        d.modified(map);
        Assert.assertFalse("Private headers should be disabled for " + testAddr1.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr1, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr2.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr2, wsprHeader));

        // note the sensitive header list overrides the private header list, any of "ibm.com, 5.6.7.8, 127.0.0.1" can pass either
        map = buildMap(true, null, "none", "ibm.com, 5.6.7.8, 127.0.0.1");
        d.modified(map);
        Assert.assertTrue("Private headers should be enabled for " + testAddr3.getHostName(), HttpDispatcher.usePrivateHeaders(testAddr3, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr4.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr4, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr5.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr5, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr6.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr6, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr7.getHostAddress() + " and " + testAddr7.getHostName(),
                           HttpDispatcher.usePrivateHeaders(testAddr7, wsprHeader));

        // note the sensitive header list overrides the private header list, any of "ibm.com, 5.6.7.8, 127.0.0.1" can pass either
        map = buildMap(true, null, "ibm.com, 5.6.7.8, 127.0.0.1", "none");
        d.modified(map);
        Assert.assertTrue("Private headers should be enabled for " + testAddr3.getHostName(), HttpDispatcher.usePrivateHeaders(testAddr3, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr4.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr4, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr5.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr5, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr6.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr6, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr7.getHostAddress() + " and " + testAddr7.getHostName(),
                           HttpDispatcher.usePrivateHeaders(testAddr7, wsprHeader));
        Assert.assertFalse("Sensitive rivate headers should be disabled for " + testAddr7.getHostAddress() + " and " + testAddr7.getHostName(),
                           HttpDispatcher.usePrivateHeaders(testAddr7, wsraHeader));
        Assert.assertFalse("Sensitive rivate headers should be disabled for " + testAddr7.getHostAddress() + " and " + testAddr3.getHostName(),
                           HttpDispatcher.usePrivateHeaders(testAddr7, wsraHeader));
        Assert.assertFalse("Sensitive rivate headers should be disabled for " + testAddr7.getHostAddress() + " and " + testAddr4.getHostName(),
                           HttpDispatcher.usePrivateHeaders(testAddr7, wsraHeader));
        Assert.assertFalse("Sensitive rivate headers should be disabled for " + testAddr7.getHostAddress() + " and " + testAddr5.getHostName(),
                           HttpDispatcher.usePrivateHeaders(testAddr7, wsraHeader));
        Assert.assertFalse("Sensitive rivate headers should be disabled for " + testAddr7.getHostAddress() + " and " + testAddr6.getHostName(),
                           HttpDispatcher.usePrivateHeaders(testAddr7, wsraHeader));

        // note the sensitive header list overrides the private header list, any of the addresses or hosts in the following list
        // can pass either type of header
        map = buildMap(true, null, "none",
                       "127.0.0.2, *.ibm.com, 5.6.7.8, 127.0.0.1, 192.168.10.*, 192.168.10.*, d:e:a:d:0:0:0:*, d:e:a:f:*:*:*:*, d:e:a:f:*:*:*:a,  d:e:a:f:0:0:*:*");
        d.modified(map);
        Assert.assertTrue("Private headers should be enabled for " + testAddr3.getHostName(), HttpDispatcher.usePrivateHeaders(testAddr3, wsprHeader));
        Assert.assertTrue("Sensitive private headers should be enabled for " + testAddr3.getHostName(), HttpDispatcher.usePrivateHeaders(testAddr3, wsraHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr4.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr4, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr5.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr5, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr6.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr6, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr7.getHostAddress() + " and " + testAddr7.getHostName(),
                           HttpDispatcher.usePrivateHeaders(testAddr7, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr9.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr9, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr10.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr10, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr11.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr11, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr12.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr12, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr13.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr13, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr14.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr14, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr15.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr15, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr16.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr16, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for d:e:a:d:0:0:1:0 and fake.com",
                           HttpDispatcher.usePrivateHeaders(testAddr17, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr18.getHostAddress() + " and " + testAddr18.getHostName(),
                           HttpDispatcher.usePrivateHeaders(testAddr18, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr19.getHostAddress() + " and " + testAddr19.getHostName(),
                           HttpDispatcher.usePrivateHeaders(testAddr19, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr20.getHostAddress() + " and " + testAddr19.getHostName(),
                           HttpDispatcher.usePrivateHeaders(testAddr20, wsprHeader));

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
            Assert.assertFalse("Private headers should be disabled for 1.2.3.4", HttpDispatcher.usePrivateHeaders(testAddr1, wsprHeader));
            Assert.assertFalse("Private headers should be enabled for 5.6.7.8", HttpDispatcher.usePrivateHeaders(testAddr2, wsprHeader));
        } finally {
            context.assertIsSatisfied();
        }
    }

    @Test
    public void testBadPrivateHeaderConfigs() throws Exception {
        InetAddress testAddr1 = InetAddress.getByAddress("hostname", InetAddress.getByName("1.2.3.4").getAddress());
        InetAddress testAddr2 = InetAddress.getByAddress("hostname", InetAddress.getByName("5.6.7.8").getAddress());
        InetAddress testAddr3 = InetAddress.getByAddress("IBM.com", InetAddress.getByName("1.2.3.4").getAddress());
        InetAddress testAddr4 = InetAddress.getByAddress("fake.com", InetAddress.getByName("127.0.0.1").getAddress());
        HttpDispatcher d = new HttpDispatcher();

        // setting "none, *" is contradictory; we'll use the first value we encounter, "none" in this case
        Map<String, Object> map = buildMap(true, null, "none, *", "none, *");
        d.activate(map);
        Assert.assertFalse("Private headers should be disabled for " + testAddr1.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr1, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr2.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr2, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr3.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr3, wsprHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr4.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr4, wsprHeader));
        Assert.assertFalse("Sensitive Private headers should be disabled for " + testAddr1.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr1, wsraHeader));
        Assert.assertFalse("Sensitive Private headers should be disabled for " + testAddr2.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr2, wsraHeader));
        Assert.assertFalse("Sensitive Private headers should be disabled for " + testAddr3.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr3, wsraHeader));
        Assert.assertFalse("Sensitive Private headers should be disabled for " + testAddr4.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr4, wsraHeader));

        // test an inverse contradictory configuration, "*, none", which should result in trust for all hosts
        map = buildMap(true, null, "*, none", "*, none");
        d.modified(map);
        Assert.assertTrue("Private headers should be enabled for " + testAddr1.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr1, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr2.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr2, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr3.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr3, wsprHeader));
        Assert.assertTrue("Private headers should be enabled for " + testAddr4.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr4, wsprHeader));
        Assert.assertTrue("Sensitive Private headers should be enabled for " + testAddr1.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr1, wsraHeader));
        Assert.assertTrue("Sensitive Private headers should be enabled for " + testAddr2.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr2, wsraHeader));
        Assert.assertTrue("Sensitive Private headers should be enabled for " + testAddr3.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr3, wsraHeader));
        Assert.assertTrue("Sensitive Private headers should be enabled for " + testAddr4.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr4, wsraHeader));

        // test invalid address format
        map = buildMap(true, null, "1.f.1.1.1", "none");
        d.modified(map);
        Assert.assertFalse("Private headers should be disabled for " + testAddr1.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr1, wsprHeader));
        Assert.assertFalse("Sensitive Private headers should be disabled for " + testAddr1.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr1, wsraHeader));

        // test invalid address wildcard format
        map = buildMap(true, null, "1.11.f.1.*", "none");
        d.modified(map);
        Assert.assertFalse("Private headers should be disabled for " + testAddr1.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr1, wsprHeader));
        Assert.assertFalse("Sensitive Private headers should be disabled for " + testAddr1.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr1, wsraHeader));

        // test invalid host format for sensitive list
        map = buildMap(true, null, "none", "invalidh@st");
        d.modified(map);
        Assert.assertFalse("Private headers should be disabled for " + testAddr1.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr1, wsprHeader));
        Assert.assertFalse("Sensitive Private headers should be disabled for " + testAddr1.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr1, wsraHeader));
        Assert.assertTrue(outputMgr.checkForMessages(".*invalidh@st.*trustedSensitiveHeaderOrigin.*invalid"));

        // test invalid host format for non-sensitive list, with valid sensitive list
        map = buildMap(true, null, "invalidh@st", "hostname");
        d.modified(map);
        Assert.assertTrue("Private headers should be enabled for " + testAddr1.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr1, wsprHeader));
        Assert.assertTrue("Sensitive Private headers should be enabled for " + testAddr1.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr1, wsraHeader));
        Assert.assertFalse("Private headers should be disabled for " + testAddr3.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr3, wsprHeader));
        Assert.assertFalse("Sensitive Private headers should be disabled for " + testAddr3.getHostAddress(), HttpDispatcher.usePrivateHeaders(testAddr3, wsraHeader));
        Assert.assertTrue(outputMgr.checkForMessages(".*invalidh@st.*trustedHeaderOrigin.*invalid"));
    }

    @Test
    public void testPrivateHeaderTrustNullHost() throws Exception {
        HttpDispatcher d = new HttpDispatcher();

        // pass in a null address: the default trusted*HeaderOrigin values should be returned
        Map<String, Object> map = buildMap(true, null, "*", "none");
        d.activate(map);
        Assert.assertTrue("Private headers should be enabled for null", HttpDispatcher.usePrivateHeaders((InetAddress) null));
        Assert.assertFalse("Sensitive private headers should be disabled for null", HttpDispatcher.usePrivateHeaders((InetAddress) null, wsraHeader));

        // configure trust for all headers and make sure that a null-address-host is still trusted
        map = buildMap(true, null, "*", "*");
        d.activate(map);
        Assert.assertTrue("Private headers should be enabled for null", HttpDispatcher.usePrivateHeaders((InetAddress) null, wsprHeader));
        Assert.assertTrue("Sensitive private headers should be enabled for null", HttpDispatcher.usePrivateHeaders((InetAddress) null, wsprHeader));

        // configure zero trust and make sure that a null-address-host is not trusted
        map = buildMap(true, null, "none", "none");
        d.activate(map);
        Assert.assertFalse("Private headers should be enabled for null", HttpDispatcher.usePrivateHeaders((InetAddress) null));
        Assert.assertFalse("Sensitive private headers should be disabled for null", HttpDispatcher.usePrivateHeaders((InetAddress) null, wsraHeader));

    }

    @Test
    public void testPrivateHeaderTrustAddressOnly() throws Exception {
        HttpDispatcher d = new HttpDispatcher();

        Map<String, Object> map = buildMap(true, null, "1.1.1.1", "1.1.1.2");
        d.activate(map);
        // verify that the legacy usePrivateHeaders(String hostAddr) methods work as expected
        Assert.assertTrue("Private headers should be enabled for 1.1.1.1", HttpDispatcher.usePrivateHeaders("1.1.1.1", wsprHeader));
        Assert.assertFalse("Sensitive private headers should be disabled for null", HttpDispatcher.usePrivateHeaders("1.1.1.1", wsraHeader));
        Assert.assertFalse("Private headers should be disabled for 1.1.1.0", HttpDispatcher.usePrivateHeaders("1.1.1.0", wsraHeader));
        Assert.assertTrue("Sensitive private headers should be enabled for 1.1.1.2", HttpDispatcher.usePrivateHeaders("1.1.1.2", wsraHeader));
        Assert.assertTrue("Private headers should be enabled for 1.1.1.2", HttpDispatcher.usePrivateHeaders("1.1.1.2"));
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
        final InetAddress testAddr = InetAddress.getByAddress("a.b.c.d", InetAddress.getByName("1.2.3.4").getAddress());
        linkRemoteAddress.set(link, testAddr);

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
        linkRemoteAddress.set(link, InetAddress.getByName("1.2.3.4"));

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

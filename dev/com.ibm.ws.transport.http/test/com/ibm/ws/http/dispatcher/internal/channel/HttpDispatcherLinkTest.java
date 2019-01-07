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
package com.ibm.ws.http.dispatcher.internal.channel;

import java.nio.charset.Charset;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;

import java.util.HashMap;
import java.util.Map;


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

import com.ibm.ws.http.dispatcher.internal.HttpDispatcherTest;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.values.ConnectionValues;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import junit.framework.Assert;
import test.common.SharedOutputManager;

/**
 *
 */
public class HttpDispatcherLinkTest {

    final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");

    @Rule
    public TestRule rule = outputMgr;

    static Field linkRequest;

    @BeforeClass
    public static void setupInstance() throws Exception {
        linkRequest = HttpDispatcherLink.class.getDeclaredField("request");
        linkRequest.setAccessible(true);
    }

    /**
     * Test method for {@link com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink#setResponseProperties(HttpResponseMessage, StatusCodes)}.
     */
    @Test
    public void setHttpResponseMessage() {
        final HttpDispatcherLink link = new HttpDispatcherLink();
        final HttpResponseMessage rMsg = mock.mock(HttpResponseMessage.class);
        final StatusCodes code = StatusCodes.OK;

        mock.checking(new Expectations() {
            {
                one(rMsg).setStatusCode(code);
                one(rMsg).setConnection(ConnectionValues.CLOSE);
                one(rMsg).setCharset(Charset.forName("UTF-8"));
                one(rMsg).setHeader("Content-Type", "text/html; charset=UTF-8");
            }
        });

        link.setResponseProperties(rMsg, StatusCodes.OK);
        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink#useTrustedHeaders(String headerName)}.
     */
    @Test
    public void checkUseTrustedHeadersDefaults() {
        String  wsprHeader = HttpHeaderKeys.HDR_$WSPR.getName();
        String  wsraHeader = HttpHeaderKeys.HDR_$WSRA.getName();
        final HttpDispatcherLink link = new HttpDispatcherLink();

        Assert.assertTrue("private headers should be trusted by default", link.useTrustedHeaders(wsprHeader));
        Assert.assertFalse("sensitive private headers should not be trusted by default", link.useTrustedHeaders(wsraHeader));
    }

    // sensitive private headers
    final String wscc = HttpHeaderKeys.HDR_$WSCC.getName();
    final String wsra = HttpHeaderKeys.HDR_$WSRA.getName();
    final String wsrh = HttpHeaderKeys.HDR_$WSRH.getName();
    final String wsat = HttpHeaderKeys.HDR_$WSAT.getName();
    final String wsru = HttpHeaderKeys.HDR_$WSRU.getName();

    // other private headers
    final String wspr = HttpHeaderKeys.HDR_$WSPR.getName();
    final String wssn = HttpHeaderKeys.HDR_$WSSN.getName();
    
    /**
     * Test method for {@link com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink#useTrustedHeaders(String headerName)}.
     */
    @Test
    public void checkGetTrustedHeaderDefaults() throws Exception{

        final HttpRequestImpl mockRequest = mock.mock(HttpRequestImpl.class);

        mock.checking(new Expectations() {
            {
                allowing(mockRequest).getHeader(wscc);
                will(returnValue("fake_certificate"));
                allowing(mockRequest).getHeader(wsra);
                will(returnValue("1.2.3.4"));
                allowing(mockRequest).getHeader(wsrh);
                will(returnValue("fake.host"));
                allowing(mockRequest).getHeader(wsat);
                will(returnValue("fake_auth"));
                allowing(mockRequest).getHeader(wsru);
                will(returnValue("fake_user"));

                allowing(mockRequest).getHeader(wspr);
                will(returnValue("http/9000"));
                allowing(mockRequest).getHeader(wssn);
                will(returnValue("fake_name"));

            }
        });

        final HttpDispatcherLink link = new HttpDispatcherLink();
        linkRequest.set(link, mockRequest);

        Assert.assertNull("sensitive private headers should not be trusted by default", link.getTrustedHeader(wscc));
        Assert.assertEquals("sensitive private headers should not be trusted by default", null, link.getTrustedHeader(wscc));

        Assert.assertNull("sensitive private headers should not be trusted by default", link.getTrustedHeader(wsra));
        Assert.assertEquals("sensitive private headers should not be trusted by default", null, link.getTrustedHeader(wsra));
        Assert.assertEquals("sensitive private headers should not be trusted by default", null, link.getRemoteHostAddress());

        Assert.assertNull("sensitive private headers should not be trusted by default", link.getTrustedHeader(wsrh));
        Assert.assertEquals("sensitive private headers should not be trusted by default", null, link.getTrustedHeader(wsrh));
        // NPEs
        //Assert.assertEquals("sensitive private headers should not be trusted by default", , link.getRemoteHostName(false));

        Assert.assertNull("sensitive private headers should not be trusted by default", link.getTrustedHeader(wsat));
        Assert.assertEquals("sensitive private headers should not be trusted by default", null, link.getTrustedHeader(wsat));

        Assert.assertNull("sensitive private headers should not be trusted by default", link.getTrustedHeader(wsru));
        Assert.assertEquals("sensitive private headers should not be trusted by default", null, link.getTrustedHeader(wsru));

        Assert.assertEquals("private header should be trusted by default", "http/9000", link.getTrustedHeader(wspr));

        Assert.assertEquals("private header should be trusted by default", "fake_name", link.getTrustedHeader(wssn));
        Assert.assertEquals("private header should be trusted by default", "fake_name", link.getRequestedHost());
    }

    // static Field linkUsePrivateHeaders;
    static Method dispatcherActivate;

    /**
     * Test method for {@link com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink#useTrustedHeaders(String headerName)}.
     */
    @Test
    public void checkGetTrustedHeader_allPrivateHeadersDisabled() throws Exception{

        // Activate and register a dispatcher which trusts no private headers
        HttpDispatcher d = new HttpDispatcher();
        dispatcherActivate = HttpDispatcher.class.getDeclaredMethod("activate", Map.class);
        dispatcherActivate.setAccessible(true);
        dispatcherActivate.invoke(d, HttpDispatcherTest.buildMap(true, null, "none", "none"));
        
        // linkUsePrivateHeaders = HttpDispatcherLink.class.getDeclaredField("usePrivateHeaders");
        // linkUsePrivateHeaders.setAccessible(true);

        final HttpDispatcherLink link = new HttpDispatcherLink();
        // final Object unknownPrivateHeader = linkUsePrivateHeaders.get(link);
        // linkUsePrivateHeaders.set(link, unknownPrivateHeader);

        final HttpRequestImpl mockRequest = mock.mock(HttpRequestImpl.class);

        mock.checking(new Expectations() {
            {
                allowing(mockRequest).getHeader(wscc);
                will(returnValue("fake_certificate"));
                allowing(mockRequest).getHeader(wsra);
                will(returnValue("1.2.3.4"));
                allowing(mockRequest).getHeader(wsrh);
                will(returnValue("fake.host"));
                allowing(mockRequest).getHeader(wsat);
                will(returnValue("fake_auth"));
                allowing(mockRequest).getHeader(wsru);
                will(returnValue("fake_user"));

                allowing(mockRequest).getHeader(wspr);
                will(returnValue("http/9000"));
                allowing(mockRequest).getHeader(wssn);
                will(returnValue("fake_name"));
                // getVirtualHost() is called if $WSSN is null in getTrustedHeade()
                allowing(mockRequest).getVirtualHost();
                will(returnValue("localhost"));
            }
        });

        linkRequest.set(link, mockRequest);

        Assert.assertNull("sensitive private headers should not be trusted", link.getTrustedHeader(wscc));
        Assert.assertEquals("sensitive private headers should not be trusted", null, link.getTrustedHeader(wscc));

        Assert.assertNull("sensitive private headers should not be trusted", link.getTrustedHeader(wsra));
        Assert.assertEquals("sensitive private headers should not be trusted", null, link.getTrustedHeader(wsra));
        Assert.assertEquals("sensitive private headers should not be trusted", null, link.getRemoteHostAddress());

        Assert.assertNull("sensitive private headers should not be trusted", link.getTrustedHeader(wsrh));
        Assert.assertEquals("sensitive private headers should not be trusted", null, link.getTrustedHeader(wsrh));

        Assert.assertNull("sensitive private headers should not be trusted", link.getTrustedHeader(wsat));
        Assert.assertEquals("sensitive private headers should not be trusted", null, link.getTrustedHeader(wsat));

        Assert.assertNull("sensitive private headers should not be trusted", link.getTrustedHeader(wsru));
        Assert.assertEquals("sensitive private headers should not be trusted", null, link.getTrustedHeader(wsru));

        Assert.assertEquals("private header should be trusted", null, link.getTrustedHeader(wspr));

        Assert.assertEquals("private header should be trusted", null, link.getTrustedHeader(wssn));
        Assert.assertEquals("private header should be trusted", mockRequest.getVirtualHost(), link.getRequestedHost());
    }

        /**
     * Test method for {@link com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink#useTrustedHeaders(String headerName)}.
     */
    @Test
    public void checkGetTrustedHeader_onlySensitiveDisabled() throws Exception{

        // Activate and register a dispatcher which trusts no private headers
        HttpDispatcher d = new HttpDispatcher();
        dispatcherActivate = HttpDispatcher.class.getDeclaredMethod("activate", Map.class);
        dispatcherActivate.setAccessible(true);
        dispatcherActivate.invoke(d, HttpDispatcherTest.buildMap(true, null, "*", "none"));
        
        // linkUsePrivateHeaders = HttpDispatcherLink.class.getDeclaredField("usePrivateHeaders");
        // linkUsePrivateHeaders.setAccessible(true);

        final HttpDispatcherLink link = new HttpDispatcherLink();
        // final Object unknownPrivateHeader = linkUsePrivateHeaders.get(link);
        // linkUsePrivateHeaders.set(link, unknownPrivateHeader);

        final HttpRequestImpl mockRequest = mock.mock(HttpRequestImpl.class);

        mock.checking(new Expectations() {
            {
                allowing(mockRequest).getHeader(wscc);
                will(returnValue("fake_certificate"));
                allowing(mockRequest).getHeader(wsra);
                will(returnValue("1.2.3.4"));
                allowing(mockRequest).getHeader(wsrh);
                will(returnValue("fake.host"));
                allowing(mockRequest).getHeader(wsat);
                will(returnValue("fake_auth"));
                allowing(mockRequest).getHeader(wsru);
                will(returnValue("fake_user"));

                allowing(mockRequest).getHeader(wspr);
                will(returnValue("http/9000"));
                allowing(mockRequest).getHeader(wssn);
                will(returnValue("fake_name"));
                // getVirtualHost() is called if $WSSN is null in getTrustedHeade()
                allowing(mockRequest).getVirtualHost();
                will(returnValue("localhost"));
            }
        });

        linkRequest.set(link, mockRequest);

        Assert.assertNull("sensitive private headers should not be trusted", link.getTrustedHeader(wscc));
        Assert.assertEquals("sensitive private headers should not be trusted", null, link.getTrustedHeader(wscc));

        Assert.assertNull("sensitive private headers should not be trusted", link.getTrustedHeader(wsra));
        Assert.assertEquals("sensitive private headers should not be trusted", null, link.getTrustedHeader(wsra));
        Assert.assertEquals("sensitive private headers should not be trusted", null, link.getRemoteHostAddress());

        Assert.assertNull("sensitive private headers should not be trusted", link.getTrustedHeader(wsrh));
        Assert.assertEquals("sensitive private headers should not be trusted", null, link.getTrustedHeader(wsrh));

        Assert.assertNull("sensitive private headers should not be trusted", link.getTrustedHeader(wsat));
        Assert.assertEquals("sensitive private headers should not be trusted", null, link.getTrustedHeader(wsat));

        Assert.assertNull("sensitive private headers should not be trusted", link.getTrustedHeader(wsru));
        Assert.assertEquals("sensitive private headers should not be trusted", null, link.getTrustedHeader(wsru));

        Assert.assertEquals("private header should be trusted by default", "http/9000", link.getTrustedHeader(wspr));

        Assert.assertEquals("private header should be trusted by default", "fake_name", link.getTrustedHeader(wssn));
        Assert.assertEquals("private header should be trusted by default", "fake_name", link.getRequestedHost());
    }

}

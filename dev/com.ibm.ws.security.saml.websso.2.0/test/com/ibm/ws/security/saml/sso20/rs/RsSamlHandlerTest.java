/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.rs;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.saml.SsoSamlService;

import test.common.SharedOutputManager;

public class RsSamlHandlerTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;

    private static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    ArrayList<String> headerNames = null;

    static final String hdrName1 = "Saml token";
    static final String hdrName2 = "saml_token";
    static final String hdrName3 = "saml3";
    static final String headerContent = "<saml:token fakedContent=\" Content\"/>";

    private static final HttpServletRequest request = mockery.mock(HttpServletRequest.class, "request");
    private static final HttpServletResponse response = mockery.mock(HttpServletResponse.class, "response");
    private static final SsoSamlService ssoSamlService = mockery.mock(SsoSamlService.class, "ssoSamlService");

    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        outputMgr.trace("*=all");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.trace("*=all=disabled");
    }

    @Before
    public void before() {
        headerNames = new ArrayList<String>();
        headerNames.add(hdrName1);
        headerNames.add(hdrName2);
        headerNames.add(hdrName3);
    }

    @Test
    public void testGetHeaderContentFromHeader() {
        RsSamlHandler rsSamlHandler = new RsSamlHandler(request, response, ssoSamlService);
        mockery.checking(new Expectations() {
            {
                one(request).getHeader(hdrName1);
                will(returnValue(headerContent));
            }
        });
        String tokenValue = rsSamlHandler.getHeaderContent(request, headerNames);
        assertEquals("Get tokenValue as '" + tokenValue + "' but not the expected " + headerContent, tokenValue, headerContent);
        assertEquals("headerName is supposed to '" + hdrName1 + "' but not the expected " + rsSamlHandler.strHeaderName,
                     hdrName1, rsSamlHandler.strHeaderName);
        //reset the strHeaderName
        rsSamlHandler.strHeaderName = null;

        mockery.checking(new Expectations() {
            {
                one(request).getHeader(hdrName1);
                will(returnValue(null));
                one(request).getHeader(hdrName2);
                will(returnValue(headerContent));
            }
        });
        tokenValue = rsSamlHandler.getHeaderContent(request, headerNames);
        assertEquals("Get tokenValue as '" + tokenValue + "' but not the expected " + headerContent, tokenValue, headerContent);
        assertEquals("headerName is supposed to '" + hdrName2 + "' but not the expected " + rsSamlHandler.strHeaderName,
                     hdrName2, rsSamlHandler.strHeaderName);
        //reset the strHeaderName
        rsSamlHandler.strHeaderName = null;

        mockery.checking(new Expectations() {
            {
                one(request).getHeader(hdrName1);
                will(returnValue(null));
                one(request).getHeader(hdrName2);
                will(returnValue(null));
                one(request).getHeader(hdrName3);
                will(returnValue(headerContent));
            }
        });
        tokenValue = rsSamlHandler.getHeaderContent(request, headerNames);
        assertEquals("Get tokenValue as '" + tokenValue + "' but not the expected " + headerContent, tokenValue, headerContent);
        assertEquals("headerName is supposed to '" + hdrName3 + "' but not the expected " + rsSamlHandler.strHeaderName,
                     hdrName3, rsSamlHandler.strHeaderName);
        //reset the strHeaderName
        rsSamlHandler.strHeaderName = null;
    }

    @Test
    public void testGetHeaderContentFromAuthorizationHeader() {
        RsSamlHandler rsSamlHandler = new RsSamlHandler(request, response, ssoSamlService);
        mockery.checking(new Expectations() {
            {
                one(request).getHeader(hdrName1);
                will(returnValue(null));
                one(request).getHeader(hdrName2);
                will(returnValue(null));
                one(request).getHeader(hdrName3);
                will(returnValue(null));
                one(request).getHeader("Authorization");
                will(returnValue(hdrName1 + " " + headerContent));
            }
        });
        String tokenValue = rsSamlHandler.getHeaderContent(request, headerNames);
        assertEquals("Get tokenValue as '" + tokenValue + "' but not the expected " + headerContent, tokenValue, headerContent);
        assertEquals("headerName is supposed to '" + hdrName1 + "' but not the expected " + rsSamlHandler.strHeaderName,
                     hdrName1, rsSamlHandler.strHeaderName);
        //reset the strHeaderName
        rsSamlHandler.strHeaderName = null;

        mockery.checking(new Expectations() {
            {
                one(request).getHeader(hdrName1);
                will(returnValue(null));
                one(request).getHeader(hdrName2);
                will(returnValue(null));
                one(request).getHeader(hdrName3);
                will(returnValue(null));
                one(request).getHeader("Authorization");
                will(returnValue(hdrName2 + "=" + headerContent));
            }
        });
        tokenValue = rsSamlHandler.getHeaderContent(request, headerNames);
        assertEquals("Get tokenValue as '" + tokenValue + "' but not the expected " + headerContent, tokenValue, headerContent);
        assertEquals("headerName is supposed to '" + hdrName2 + "' but not the expected " + rsSamlHandler.strHeaderName,
                     hdrName2, rsSamlHandler.strHeaderName);
        //reset the strHeaderName
        rsSamlHandler.strHeaderName = null;

        mockery.checking(new Expectations() {
            {
                one(request).getHeader(hdrName1);
                will(returnValue(null));
                one(request).getHeader(hdrName2);
                will(returnValue(null));
                one(request).getHeader(hdrName3);
                will(returnValue(null));
                one(request).getHeader("Authorization");
                will(returnValue(hdrName3 + "=\"" + headerContent + "\""));
            }
        });
        tokenValue = rsSamlHandler.getHeaderContent(request, headerNames);
        assertEquals("Get tokenValue as '" + tokenValue + "' but not the expected " + headerContent, tokenValue, headerContent);
        assertEquals("headerName is supposed to '" + hdrName3 + "' but not the expected " + rsSamlHandler.strHeaderName,
                     hdrName3, rsSamlHandler.strHeaderName);
        //reset the strHeaderName
        rsSamlHandler.strHeaderName = null;

        mockery.checking(new Expectations() {
            {
                one(request).getHeader(hdrName1);
                will(returnValue(null));
                one(request).getHeader(hdrName2);
                will(returnValue(null));
                one(request).getHeader(hdrName3);
                will(returnValue(null));
                one(request).getHeader("Authorization");
                will(returnValue(hdrName3 + "=\"" + headerContent + "\""));
            }
        });
        tokenValue = rsSamlHandler.getHeaderContent(request, headerNames);
        assertEquals("Get tokenValue as '" + tokenValue + "' but not the expected " + headerContent, tokenValue, headerContent);
        assertEquals("headerName is supposed to '" + hdrName3 + "' but not the expected " + rsSamlHandler.strHeaderName,
                     hdrName3, rsSamlHandler.strHeaderName);
        //reset the strHeaderName
        rsSamlHandler.strHeaderName = null;
    }

    @Test
    public void testGetHeaderContentFromAuthorizationHeaderBad() {
        RsSamlHandler rsSamlHandler = new RsSamlHandler(request, response, ssoSamlService);
        //reset the strHeaderName
        rsSamlHandler.strHeaderName = null;
        mockery.checking(new Expectations() {
            {
                one(request).getHeader(hdrName1);
                will(returnValue(null));
                one(request).getHeader(hdrName2);
                will(returnValue(null));
                one(request).getHeader(hdrName3);
                will(returnValue(null));
                one(request).getHeader("Authorization");
                will(returnValue("badHdr " + headerContent));
            }
        });
        String tokenValue = rsSamlHandler.getHeaderContent(request, headerNames);
        assertEquals("Get tokenValue as '" + tokenValue + "' but not the expected null ", tokenValue, null);
        assertEquals("headerName is supposed to null but not " + rsSamlHandler.strHeaderName,
                     rsSamlHandler.strHeaderName, null);

        //reset the strHeaderName
        rsSamlHandler.strHeaderName = null;
        mockery.checking(new Expectations() {
            {
                one(request).getHeader(hdrName1);
                will(returnValue(null));
                one(request).getHeader(hdrName2);
                will(returnValue(null));
                one(request).getHeader(hdrName3);
                will(returnValue(null));
                one(request).getHeader("Authorization");
                will(returnValue("=" + headerContent));
            }
        });
        tokenValue = rsSamlHandler.getHeaderContent(request, headerNames);
        assertEquals("Get tokenValue as '" + tokenValue + "' but not the expected null ", tokenValue, null);
        assertEquals("headerName is supposed to null but not " + rsSamlHandler.strHeaderName,
                     rsSamlHandler.strHeaderName, null);

        //reset the strHeaderName
        rsSamlHandler.strHeaderName = null;
        mockery.checking(new Expectations() {
            {
                one(request).getHeader(hdrName1);
                will(returnValue(null));
                one(request).getHeader(hdrName2);
                will(returnValue(null));
                one(request).getHeader(hdrName3);
                will(returnValue(null));
                one(request).getHeader("Authorization");
                will(returnValue(null));
            }
        });
        tokenValue = rsSamlHandler.getHeaderContent(request, headerNames);
        assertEquals("Get tokenValue as '" + tokenValue + "' but not the expected null ", tokenValue, null);
        assertEquals("headerName is supposed to null but not " + rsSamlHandler.strHeaderName,
                     rsSamlHandler.strHeaderName, null);

    }
}
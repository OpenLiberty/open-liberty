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
package com.ibm.ws.security.saml.sso20.internal.utils;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.ws.security.common.jwk.utils.JsonUtils;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;

import test.common.SharedOutputManager;

/**
 *
 */
public class InitialRequestUtilTest {
    
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.saml.sso20.*=all");
    @Rule
    public TestRule managerRule = outputMgr;

    public final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    
    private final HttpServletRequest HTTP_SERVLET_REQUEST_MCK = mockery.mock(SRTServletRequest.class);
    private final SsoSamlService ssoService = mockery.mock(SsoSamlService.class);
    
    @Rule
    public final TestName testName = new TestName();
    private InitialRequestUtil irUtil = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
        
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() {
        System.out.println("Entering test: " + testName.getMethodName());
        irUtil  = new InitialRequestUtil();
        //JwtUtils.setSSLSupportService(sslSupportRef);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
        System.out.println("Exiting test: " + testName.getMethodName());
    }


    /**
     * Test method for {@link com.ibm.ws.security.saml.sso20.internal.utils.InitialRequestUtil#updateInitialRequestCookieNameWithRelayState(java.lang.String)}.
     */
    @Test
    public void testUpdateInitialRequestCookieNameWithRelayState() {
        String append = null;
        String cookiename = irUtil.updateInitialRequestCookieNameWithRelayState(append);
        assertNull("Result was not null when it should have been. Result: " + cookiename, cookiename);
        append = "appendme";
        String expect = Constants.WAS_IR_COOKIE + append;
        cookiename = irUtil.updateInitialRequestCookieNameWithRelayState(append);
        assertEquals("Did not find expected cookie name.", expect, cookiename);
    }

    /**
     * Test method for {@link com.ibm.ws.security.saml.sso20.internal.utils.InitialRequestUtil#digestInitialRequestCookieValue(java.lang.String, com.ibm.ws.security.saml.SsoSamlService)}.
     */
    @Test
    public void testDigestInitialRequestCookieValue() {
        String irBytesStr = getInitialRequestString();
        mockery.checking(new Expectations() {
            {
                allowing(ssoService).getDefaultKeyStorePassword();
                will(returnValue(null));      
            }
        });
        String cookiedigest = irUtil.digestInitialRequestCookieValue(irBytesStr, ssoService);
        String expect = "rO0ABXNyADxjb20uaWJtLndzLnNlY3VyaXR5LnNhbWwuc3NvMjAuaW50ZXJuYWwudXRpbHMuSW5pdGlhbFJlcXVlc3QAAAAAAAAAAQMACFoAFGlzRm9ybUxvZ291dEV4aXRQYWdlTAASZm9ybUxvZ291dEV4aXRQYWdldAASTGphdmEvbGFuZy9TdHJpbmc7TAAGbWV0aG9kcQB-AAFMAApwb3N0UGFyYW1zcQB-AAFMAAZyZXFVcmxxAH4AAUwACnJlcXVlc3RVUkxxAH4AAUwAD3NhdmVkUG9zdFBhcmFtc3QAE0xqYXZhL3V0aWwvSGFzaE1hcDtMABFzdHJJblJlc3BvbnNlVG9JZHEAfgABeHB3gwAraHR0cHM6Ly9sb2NhbGhvc3Q6ODAyMC9zYW1sY2xpZW50L3NwMS9zbm9vcAAraHR0cHM6Ly9sb2NhbGhvc3Q6ODAyMC9zYW1sY2xpZW50L3NwMS9zbm9vcAADR0VUACFfMG5EU2czOVc5TU1NUkpNMGc1MUM1b3RHbFdLOVV4V3oAeA_YQEzoMCRqkVb4orBZ8pgJT5EomZsyeOgFjnTHChNNCE=";
        assertEquals("Did not find expected cookie digest.", expect, cookiedigest);
    }

    /**
     * @return
     */
    private InitialRequest getInitialRequest() {
        String requestUrl = "https://localhost:8020/samlclient/sp1/snoop";
        String method = "GET";
        String inResp = "_0nDSg39W9MMMRJM0g51C5otGlWK9UxWz";
        String logoutPage = null;
        InitialRequest ir = null;
        String irBytesStr = null;  
        try {
            ir = new InitialRequest(HTTP_SERVLET_REQUEST_MCK, requestUrl, requestUrl,method,inResp, logoutPage, null);
        } catch (SamlException e) {

        }
        return ir;
//        if (ir != null) {
//            byte[] irBytes = null;       
//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            ObjectOutputStream out = null;
//            try {
//              try {
//                out = new ObjectOutputStream(bos);
//                out.writeObject(ir);
//                out.flush();
//                irBytes = bos.toByteArray();
//                if (irBytes != null) {
//                    irBytesStr = JsonUtils.convertToBase64(irBytes);
//                }
//            } catch (IOException e) {
//               
//            }     
//             
//            } finally {
//              try {
//                bos.close();
//              } catch (IOException ex) {
//                
//              }
//            }
//        }
//        return irBytesStr;
    }
    
    /**
     * @return
     */
    private String getInitialRequestString() {
        InitialRequest ir = null;
        ir = getInitialRequest();
        String irBytesStr = null; 
        if (ir != null) {
            byte[] irBytes = null;       
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = null;
            try {
              try {
                out = new ObjectOutputStream(bos);
                out.writeObject(ir);
                out.flush();
                irBytes = bos.toByteArray();
                if (irBytes != null) {
                    irBytesStr = JsonUtils.convertToBase64(irBytes);
                }
            } catch (IOException e) {
               
            }     
             
            } finally {
              try {
                bos.close();
              } catch (IOException ex) {
                
              }
            }
        }
        return irBytesStr;
    }

    /**
     * Test method for {@link com.ibm.ws.security.saml.sso20.internal.utils.InitialRequestUtil#getInitialRequestCookie(java.lang.String, com.ibm.ws.security.saml.SsoSamlService)}.
     */
    @Test
    public void testGetInitialRequestCookie() {
        mockery.checking(new Expectations() {
            {
                allowing(ssoService).getDefaultKeyStorePassword();
                will(returnValue(null));      
            }
        });
        String ircookie = "rO0ABXNyADxjb20uaWJtLndzLnNlY3VyaXR5LnNhbWwuc3NvMjAuaW50ZXJuYWwudXRpbHMuSW5pdGlhbFJlcXVlc3QAAAAAAAAAAQMACFoAFGlzRm9ybUxvZ291dEV4aXRQYWdlTAASZm9ybUxvZ291dEV4aXRQYWdldAASTGphdmEvbGFuZy9TdHJpbmc7TAAGbWV0aG9kcQB-AAFMAApwb3N0UGFyYW1zcQB-AAFMAAZyZXFVcmxxAH4AAUwACnJlcXVlc3RVUkxxAH4AAUwAD3NhdmVkUG9zdFBhcmFtc3QAE0xqYXZhL3V0aWwvSGFzaE1hcDtMABFzdHJJblJlc3BvbnNlVG9JZHEAfgABeHB3gwAraHR0cHM6Ly9sb2NhbGhvc3Q6ODAyMC9zYW1sY2xpZW50L3NwMS9zbm9vcAAraHR0cHM6Ly9sb2NhbGhvc3Q6ODAyMC9zYW1sY2xpZW50L3NwMS9zbm9vcAADR0VUACFfMG5EU2czOVc5TU1NUkpNMGc1MUM1b3RHbFdLOVV4V3oAeA_YQEzoMCRqkVb4orBZ8pgJT5EomZsyeOgFjnTHChNNCE=";
        String initial = irUtil.getInitialRequestCookie(ircookie, ssoService);
        String expect = "rO0ABXNyADxjb20uaWJtLndzLnNlY3VyaXR5LnNhbWwuc3NvMjAuaW50ZXJuYWwudXRpbHMuSW5pdGlhbFJlcXVlc3QAAAAAAAAAAQMACFoAFGlzRm9ybUxvZ291dEV4aXRQYWdlTAASZm9ybUxvZ291dEV4aXRQYWdldAASTGphdmEvbGFuZy9TdHJpbmc7TAAGbWV0aG9kcQB-AAFMAApwb3N0UGFyYW1zcQB-AAFMAAZyZXFVcmxxAH4AAUwACnJlcXVlc3RVUkxxAH4AAUwAD3NhdmVkUG9zdFBhcmFtc3QAE0xqYXZhL3V0aWwvSGFzaE1hcDtMABFzdHJJblJlc3BvbnNlVG9JZHEAfgABeHB3gwAraHR0cHM6Ly9sb2NhbGhvc3Q6ODAyMC9zYW1sY2xpZW50L3NwMS9zbm9vcAAraHR0cHM6Ly9sb2NhbGhvc3Q6ODAyMC9zYW1sY2xpZW50L3NwMS9zbm9vcAADR0VUACFfMG5EU2czOVc5TU1NUkpNMGc1MUM1b3RHbFdLOVV4V3oAeA";
        assertEquals("Did not find expected cookie.", expect, initial);  
    }

    /**
     * Test method for {@link com.ibm.ws.security.saml.sso20.internal.utils.InitialRequestUtil#createHttpRequestInfoFromInitialRequest(com.ibm.ws.security.saml.sso20.internal.utils.InitialRequest)}.
     */
    @Test
    public void testCreateHttpRequestInfoFromInitialRequest() {
        InitialRequest ir = getInitialRequest();
        HttpRequestInfo requestInfo = irUtil.createHttpRequestInfoFromInitialRequest(ir);
        String url = requestInfo.getReqUrl();
        String expect = ir.getRequestUrl();       
        assertEquals("Did not find expected request url.", expect, url);
        String inresp = requestInfo.getInResponseToId();
        expect = ir.getInResponseToId();
        assertEquals("Did not find expected in response to id.", expect, inresp);
    }

    /**
     * Test method for {@link com.ibm.ws.security.saml.sso20.internal.utils.InitialRequestUtil#handleDeserializingInitialRequest(java.lang.String)}.
     */
    @Test
    public void testHandleDeserializingInitialRequest() {
        String irBytesStr = getInitialRequestString();
        InitialRequest ir = null;
        try {
            ir = irUtil.handleDeserializingInitialRequest(irBytesStr);
        } catch (ClassNotFoundException e) {

        } catch (IOException e) {

        }
        String url = null;
        String method = null;
        if (ir != null) {
            method = ir.getMethod();
            url = ir.getRequestUrl();
        }
        String expect = "GET";
        assertEquals("Did not find expected request method.", expect, method);
        expect = "https://localhost:8020/samlclient/sp1/snoop";
        assertEquals("Did not find expected request url.", expect, url);
    }
}

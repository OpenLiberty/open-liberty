/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.ws.webcontainer.srt.SRTServletRequest;

/**
 *
 */
public class PostParameterHelperTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class);
    private final SRTServletRequest srtReq = mock.mock(SRTServletRequest.class);
    private final HttpServletResponse res = mock.mock(HttpServletResponse.class);
    private final WebAppSecurityConfig webAppSecConfig = mock.mock(WebAppSecurityConfig.class);
    private final PostParameterHelper postParamHelper = new PostParameterHelper(webAppSecConfig);

    @After
    public void tearDown() throws Exception {
        mock.assertIsSatisfied();
    }

    /**
     * Test method for
     * {@link com.ibm.ws.webcontainer.security.PostParameterHelper#save(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, com.ibm.ws.webcontainer.security.AuthenticationResult)}
     * .
     */
    @Test
    public void save_notIServletRequest() {
        AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, "");
        mock.checking(new Expectations() {
            {
                one(req).getMethod();
                will(returnValue("POST"));
            }
        });
        postParamHelper.save(req, res, authResult);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.webcontainer.security.PostParameterHelper#save(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, com.ibm.ws.webcontainer.security.AuthenticationResult)}
     * .
     */
    @Test
    public void save_notPostMethod() {
        AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, "");
        mock.checking(new Expectations() {
            {
                one(srtReq).getMethod();
                will(returnValue("GET"));
            }
        });
        postParamHelper.save(srtReq, res, authResult);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.webcontainer.security.PostParameterHelper#save(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, com.ibm.ws.webcontainer.security.AuthenticationResult)}
     * .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void save_postMethodSaveToCookieDataTooBig() throws Exception {
        final Map map = new HashMap();
        AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, "");
        mock.checking(new Expectations() {
            {
                one(srtReq).getMethod();
                will(returnValue("POST"));
                one(srtReq).getAttribute(PostParameterHelper.ATTRIB_HASH_MAP); //
                will(returnValue(map)); //
                one(srtReq).setInputStreamData((HashMap) map); //
                one(srtReq).setAttribute(PostParameterHelper.ATTRIB_HASH_MAP, null);
                one(srtReq).getRequestURI();
                will(returnValue("/uri"));
                one(srtReq).getInputStreamData();
                will(returnValue(map));
                one(srtReq).sizeInputStreamData(map);
                will(returnValue(20000L));
                one(webAppSecConfig).getPostParamSaveMethod();
                will(returnValue(WebAppSecurityConfig.POST_PARAM_SAVE_TO_COOKIE));
                one(webAppSecConfig).getPostParamCookieSize();
                will(returnValue(16384));
            }
        });
        postParamHelper.save(srtReq, res, authResult);

        assertTrue("Expected message was not logged",
                   outputMgr.checkForStandardOut("CWWKS9107W: Post parameters are null or too large to store into a cookie."));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.PostParameterHelper#restore(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void restore_notIServletRequest() {
        postParamHelper.restore(req, res);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.PostParameterHelper#restore(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void restore_notGetMethod() {
        mock.checking(new Expectations() {
            {
                one(srtReq).getMethod();
                will(returnValue("POST"));
            }
        });
        postParamHelper.restore(srtReq, res);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.PostParameterHelper#restore(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void restore_getMethodNoCookieValue() {
        mock.checking(new Expectations() {
            {
                one(srtReq).getMethod();
                will(returnValue("GET"));
                one(srtReq).getRequestURI();
                will(returnValue("/uri"));
                one(webAppSecConfig).getPostParamSaveMethod();
                will(returnValue(WebAppSecurityConfig.POST_PARAM_SAVE_TO_COOKIE));
                one(srtReq).getCookieValueAsBytes(PostParameterHelper.POSTPARAM_COOKIE);
                will(returnValue(null));
            }
        });
        postParamHelper.restore(srtReq, res);
    }

}

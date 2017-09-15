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
package com.ibm.ws.webcontainer.security.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Test;

import com.ibm.ws.webcontainer.srt.SRTServletRequest;

/**
 *
 */
public class SRTServletRequestUtilsTest {
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class);
    private final SRTServletRequest srtReq = mock.mock(SRTServletRequest.class);
    private final HttpServletRequestWrapper reqWrapper1 = mock.mock(HttpServletRequestWrapper.class, "reqWrapper1");
    private final HttpServletRequestWrapper reqWrapper2 = mock.mock(HttpServletRequestWrapper.class, "reqWrapper2");
    private static final String key = "AUTH_TYPE";
    private static final String value = "FORM";

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#getPrivateAttribute(javax.servlet.http.HttpServletRequest, java.lang.String)}.
     * 
     * It's not an implementation of an IPrivateRequestAttributes object,
     * so it should unconditionally return null.
     */
    @Test
    public void getPrivateAttribute_not_IPrivateRequestAttributes_object() {
        mock.checking(new Expectations() {
            {
                never(req);
            }
        });
        assertNull("Expecting null for an unexpected request object type",
                   SRTServletRequestUtils.getPrivateAttribute(req, key));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#getPrivateAttribute(javax.servlet.http.HttpServletRequest, java.lang.String)}.
     * 
     * It's not an implementation of an IPrivateRequestAttributes object,
     * so it should unconditionally return null.
     */
    @Test
    public void getPrivateAttribute_wrapped_not_IPrivateRequestAttributes_object() {
        mock.checking(new Expectations() {
            {
                one(reqWrapper1).getRequest();
                will(returnValue(reqWrapper2));
                one(reqWrapper2).getRequest();
                will(returnValue(req));
                never(req);
            }
        });
        assertNull("Expecting null for an unexpected request object type",
                   SRTServletRequestUtils.getPrivateAttribute(reqWrapper1, key));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#getPrivateAttribute(javax.servlet.http.HttpServletRequest, java.lang.String)}.
     */
    @Test
    public void getPrivateAttribute_wrappedSRTServletRequest() {
        mock.checking(new Expectations() {
            {
                one(reqWrapper1).getRequest();
                will(returnValue(reqWrapper2));
                one(reqWrapper2).getRequest();
                will(returnValue(srtReq));
                one(srtReq).getPrivateAttribute(key);
                will(returnValue(value));
            }
        });

        assertEquals("Did not recieve the expected value",
                     value, SRTServletRequestUtils.getPrivateAttribute(reqWrapper1, key));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#getPrivateAttribute(javax.servlet.http.HttpServletRequest, java.lang.String)}.
     */
    @Test
    public void getPrivateAttribute() throws Exception {
        mock.checking(new Expectations() {
            {
                one(srtReq).getPrivateAttribute(key);
                will(returnValue(value));
            }
        });

        assertEquals("Did not recieve the expected value",
                     value, SRTServletRequestUtils.getPrivateAttribute(srtReq, key));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#setPrivateAttribute(HttpServletRequest, String, Object)}.
     * 
     * It's not an implementation of an IPrivateRequestAttributes object,
     * so nothing should happen.
     */
    @Test
    public void setPrivateAttribute_not_IPrivateRequestAttributes_object() {
        mock.checking(new Expectations() {
            {
                never(req);
            }
        });

        SRTServletRequestUtils.setPrivateAttribute(req, key, value);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#setPrivateAttribute(HttpServletRequest, String, Object)}.
     * 
     * It's not an implementation of an IPrivateRequestAttributes object,
     * so nothing should happen.
     */
    @Test
    public void setPrivateAttribute_wrapped_not_IPrivateRequestAttributes_object() {
        mock.checking(new Expectations() {
            {
                one(reqWrapper1).getRequest();
                will(returnValue(reqWrapper2));
                one(reqWrapper2).getRequest();
                will(returnValue(req));
                never(req);
            }
        });

        SRTServletRequestUtils.setPrivateAttribute(reqWrapper1, key, value);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#setPrivateAttribute(HttpServletRequest, String, Object)}.
     */
    @Test
    public void setPrivateAttribute_wrappedSRTServletRequest() {
        mock.checking(new Expectations() {
            {
                one(reqWrapper1).getRequest();
                will(returnValue(reqWrapper2));
                one(reqWrapper2).getRequest();
                will(returnValue(srtReq));
                one(srtReq).setPrivateAttribute(key, value);
            }
        });

        SRTServletRequestUtils.setPrivateAttribute(reqWrapper1, key, value);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#setPrivateAttribute(HttpServletRequest, String, Object)}.
     */
    @Test
    public void setPrivateAttribute() throws Exception {
        mock.checking(new Expectations() {
            {
                one(srtReq).setPrivateAttribute(key, value);
            }
        });

        SRTServletRequestUtils.setPrivateAttribute(srtReq, key, value);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#removePrivateAttribute(HttpServletRequest, String)}.
     * 
     * It's not an implementation of IPrivateRequestAttributes object,
     * so nothing should happen.
     */
    @Test
    public void removePrivateAttributes_not_IPrivateRequestAttributes_object() {
        mock.checking(new Expectations() {
            {
                never(req);
            }
        });

        SRTServletRequestUtils.removePrivateAttribute(req, key);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#removePrivateAttribute(HttpServletRequest, String)}.
     * 
     * It's not an implementation of IPrivateRequestAttributes object,
     * so nothing should happen.
     */
    @Test
    public void removePrivateAttributes_wrapped_not_IPrivateRequestAttributes_object() {
        mock.checking(new Expectations() {
            {
                one(reqWrapper1).getRequest();
                will(returnValue(reqWrapper2));
                one(reqWrapper2).getRequest();
                will(returnValue(req));
                never(req);
            }
        });

        SRTServletRequestUtils.removePrivateAttribute(reqWrapper1, key);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#removePrivateAttribute(HttpServletRequest, String)}.
     */
    @Test
    public void removePrivateAttribute_wrappedSRTServletRequest() throws Exception {
        mock.checking(new Expectations() {
            {
                one(reqWrapper1).getRequest();
                will(returnValue(reqWrapper2));
                one(reqWrapper2).getRequest();
                will(returnValue(srtReq));
                one(srtReq).removePrivateAttribute(key);
            }
        });

        SRTServletRequestUtils.removePrivateAttribute(reqWrapper1, key);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#removePrivateAttribute(HttpServletRequest, String)}.
     */
    @Test
    public void removePrivateAttribute() throws Exception {
        mock.checking(new Expectations() {
            {
                one(srtReq).removePrivateAttribute(key);
            }
        });

        SRTServletRequestUtils.removePrivateAttribute(srtReq, key);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#getPrivateAttribute(javax.servlet.http.HttpServletRequest, java.lang.String)}.
     */
    @Test
    public void getHeader_not_IPrivateRequestAttributes_object() {
        mock.checking(new Expectations()
        {
            {
                one(req).getHeader(key);
            }
        });
        SRTServletRequestUtils.getHeader(req, key);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#getPrivateAttribute(javax.servlet.http.HttpServletRequest, java.lang.String)}.
     */
    @Test
    public void getHeader_wrapped_not_IPrivateRequestAttributes_object() {
        mock.checking(new Expectations()
        {
            {
                one(reqWrapper1).getRequest();
                will(returnValue(reqWrapper2));
                one(reqWrapper2).getRequest();
                will(returnValue(req));
                one(req).getHeader(key);
            }
        });
        SRTServletRequestUtils.getHeader(reqWrapper1, key);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#getHeader(javax.servlet.http.HttpServletRequest, java.lang.String)}.
     */
    @Test
    public void getHeader_wrappedSRTServletRequest() throws Exception {
        mock.checking(new Expectations() {
            {
                one(reqWrapper1).getRequest();
                will(returnValue(reqWrapper2));
                one(reqWrapper2).getRequest();
                will(returnValue(srtReq));
                one(srtReq).getHeader(key);
            }
        });

        SRTServletRequestUtils.getHeader(reqWrapper1, key);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils#getHeader(javax.servlet.http.HttpServletRequest, java.lang.String)}.
     */
    @Test
    public void getHeader() throws Exception {
        mock.checking(new Expectations() {
            {
                one(srtReq).getHeader(key);
            }
        });

        SRTServletRequestUtils.getHeader(srtReq, key);
    }

}

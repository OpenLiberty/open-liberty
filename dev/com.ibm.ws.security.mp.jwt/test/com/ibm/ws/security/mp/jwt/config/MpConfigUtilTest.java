/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.ServletContext;
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

import com.ibm.ws.security.mp.jwt.MpConfigProxyService;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;

import test.common.SharedOutputManager;

/**
 *
 */
public class MpConfigUtilTest {
    protected final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.mp.jwt.*=all");

    private final MpConfigProxyService mpConfigProxyService = mockery.mock(MpConfigProxyService.class);
    private final HttpServletRequest req = mockery.mock(HttpServletRequest.class);
    private final ServletContext servletCtx = mockery.mock(ServletContext.class);
    private final SRTServletRequest srtReq = mockery.mock(SRTServletRequest.class);
    private final ClassLoader cl = mockery.mock(ClassLoader.class);

    MpConfigUtil mpConfigUtil = null;

    @Rule
    public final TestName testName = new TestName();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void beforeTest() throws Exception {
        mpConfigUtil = new MpConfigUtil();
        mpConfigUtil.setMpConfigProxyService(mpConfigProxyService);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        mpConfigUtil.unsetMpConfigProxyService(null);
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    /**
     * Tests getMpConfig method without having the proxy service.
     */
    @Test
    public void getMpConfigNoConfigProxyService() {
        mpConfigUtil.unsetMpConfigProxyService(null);
        Map<String, String> map = mpConfigUtil.getMpConfig(req);
        assertTrue("the map should be empty.", map.isEmpty());
    }

    /**
     * Tests getMpConfig method with the proxy service is available.
     * No SRTServletRequest
     */
    @Test
    public void getMpConfigWithConfigProxyServiceNoSrtReq() {
        mockery.checking(new Expectations() {
            {
                one(req).getServletContext();
                will(returnValue(servletCtx));
                one(servletCtx).getClassLoader();
                will(returnValue(null));
                one(mpConfigProxyService).getSupportedConfigPropertyNames();
                will(returnValue(getSupportedMpConfigProps()));
                one(mpConfigProxyService).getConfigValue(null, MpConstants.ISSUER, String.class);
                will(returnValue("value_" + MpConstants.ISSUER));
                one(mpConfigProxyService).getConfigValue(null, MpConstants.PUBLIC_KEY, String.class);
                will(returnValue("value_" + MpConstants.PUBLIC_KEY));
                one(mpConfigProxyService).getConfigValue(null, MpConstants.KEY_LOCATION, String.class);
                will(returnValue("value_" + MpConstants.KEY_LOCATION));
            }
        });
        Map<String, String> map = mpConfigUtil.getMpConfig(req);
        assertEquals("the map should be 3 items.", 3, map.size());
        assertTrue("the map should contain the key " + MpConstants.ISSUER, map.containsKey(MpConstants.ISSUER));
        assertTrue("the map should contain the key " + MpConstants.PUBLIC_KEY, map.containsKey(MpConstants.PUBLIC_KEY));
        assertTrue("the map should contain the key " + MpConstants.KEY_LOCATION, map.containsKey(MpConstants.KEY_LOCATION));
        assertTrue("the map should contain the value " + MpConstants.ISSUER, map.get(MpConstants.ISSUER).equals("value_" + MpConstants.ISSUER));
        assertTrue("the map should contain the value " + MpConstants.PUBLIC_KEY, map.get(MpConstants.PUBLIC_KEY).equals("value_" + MpConstants.PUBLIC_KEY));
        assertTrue("the map should contain the value" + MpConstants.KEY_LOCATION, map.get(MpConstants.KEY_LOCATION).equals("value_" + MpConstants.KEY_LOCATION));
    }

    /**
     * Tests getMpConfig method with the config proxy service is available.
     * SRTServletRequest
     */
    @Test
    public void getMpConfigWithConfigProxyServiceSrtReq() {
        mockery.checking(new Expectations() {
            {
                one(srtReq).getServletContext();
                will(returnValue(servletCtx));
                one(servletCtx).getClassLoader();
                will(returnValue(cl));
                one(mpConfigProxyService).getSupportedConfigPropertyNames();
                will(returnValue(getSupportedMpConfigProps()));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.ISSUER, String.class);
                will(returnValue("value_" + MpConstants.ISSUER));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.PUBLIC_KEY, String.class);
                will(returnValue("value_" + MpConstants.PUBLIC_KEY));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.KEY_LOCATION, String.class);
                will(returnValue("value_" + MpConstants.KEY_LOCATION));
            }
        });
        Map<String, String> map = mpConfigUtil.getMpConfig(srtReq);
        assertEquals("the map should be 3 items.", 3, map.size());
        assertTrue("the map should contain the key " + MpConstants.ISSUER, map.containsKey(MpConstants.ISSUER));
        assertTrue("the map should contain the key " + MpConstants.PUBLIC_KEY, map.containsKey(MpConstants.PUBLIC_KEY));
        assertTrue("the map should contain the key " + MpConstants.KEY_LOCATION, map.containsKey(MpConstants.KEY_LOCATION));
        assertTrue("the map should contain the value " + MpConstants.ISSUER, map.get(MpConstants.ISSUER).equals("value_" + MpConstants.ISSUER));
        assertTrue("the map should contain the value " + MpConstants.PUBLIC_KEY, map.get(MpConstants.PUBLIC_KEY).equals("value_" + MpConstants.PUBLIC_KEY));
        assertTrue("the map should contain the value" + MpConstants.KEY_LOCATION, map.get(MpConstants.KEY_LOCATION).equals("value_" + MpConstants.KEY_LOCATION));
    }

    /**
     * Tests getMpConfig method with the config proxy service is available.
     * The MpConstants.ISSUER does not exist.
     */
    @Test
    public void getMpConfigWithConfigProxyServiceSrtReqNoIssuer() {
        mockery.checking(new Expectations() {
            {
                one(srtReq).getServletContext();
                will(returnValue(servletCtx));
                one(servletCtx).getClassLoader();
                will(returnValue(cl));
                one(mpConfigProxyService).getSupportedConfigPropertyNames();
                will(returnValue(getSupportedMpConfigProps()));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.ISSUER, String.class);
                will(throwException(new NoSuchElementException()));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.PUBLIC_KEY, String.class);
                will(returnValue("value_" + MpConstants.PUBLIC_KEY));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.KEY_LOCATION, String.class);
                will(returnValue("value_" + MpConstants.KEY_LOCATION));
            }
        });
        Map<String, String> map = mpConfigUtil.getMpConfig(srtReq);
        assertEquals("the map should be 2 items.", 2, map.size());
        assertTrue("the map should contain the key " + MpConstants.PUBLIC_KEY, map.containsKey(MpConstants.PUBLIC_KEY));
        assertTrue("the map should contain the key " + MpConstants.KEY_LOCATION, map.containsKey(MpConstants.KEY_LOCATION));
        assertTrue("the map should contain the value " + MpConstants.PUBLIC_KEY, map.get(MpConstants.PUBLIC_KEY).equals("value_" + MpConstants.PUBLIC_KEY));
        assertTrue("the map should contain the value" + MpConstants.KEY_LOCATION, map.get(MpConstants.KEY_LOCATION).equals("value_" + MpConstants.KEY_LOCATION));
    }

    /**
     * Tests getMpConfig method with the config proxy service is available.
     * The MpConstants.PUBLIC_KEY does not exist.
     */
    @Test
    public void getMpConfigWithConfigProxyServiceSrtReqNoPublicKey() {
        mockery.checking(new Expectations() {
            {
                one(srtReq).getServletContext();
                will(returnValue(servletCtx));
                one(servletCtx).getClassLoader();
                will(returnValue(cl));
                one(mpConfigProxyService).getSupportedConfigPropertyNames();
                will(returnValue(getSupportedMpConfigProps()));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.ISSUER, String.class);
                will(returnValue("value_" + MpConstants.ISSUER));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.PUBLIC_KEY, String.class);
                will(throwException(new NoSuchElementException()));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.KEY_LOCATION, String.class);
                will(returnValue("value_" + MpConstants.KEY_LOCATION));
            }
        });
        Map<String, String> map = mpConfigUtil.getMpConfig(srtReq);
        assertEquals("the map should be 2 items.", 2, map.size());
        assertTrue("the map should contain the key " + MpConstants.ISSUER, map.containsKey(MpConstants.ISSUER));
        assertTrue("the map should contain the key " + MpConstants.KEY_LOCATION, map.containsKey(MpConstants.KEY_LOCATION));
        assertTrue("the map should contain the value " + MpConstants.ISSUER, map.get(MpConstants.ISSUER).equals("value_" + MpConstants.ISSUER));
        assertTrue("the map should contain the value" + MpConstants.KEY_LOCATION, map.get(MpConstants.KEY_LOCATION).equals("value_" + MpConstants.KEY_LOCATION));
    }

    /**
     * Tests getMpConfig method with the config proxy service is available.
     * The MpConstants.KEY_LOCATION does not exist.
     */
    @Test
    public void getMpConfigWithConfigProxyServiceSrtReqNoKeyLocation() {
        mockery.checking(new Expectations() {
            {
                one(srtReq).getServletContext();
                will(returnValue(servletCtx));
                one(servletCtx).getClassLoader();
                will(returnValue(cl));
                one(mpConfigProxyService).getSupportedConfigPropertyNames();
                will(returnValue(getSupportedMpConfigProps()));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.ISSUER, String.class);
                will(returnValue("value_" + MpConstants.ISSUER));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.PUBLIC_KEY, String.class);
                will(returnValue("value_" + MpConstants.PUBLIC_KEY));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.KEY_LOCATION, String.class);
                will(throwException(new NoSuchElementException()));
            }
        });
        Map<String, String> map = mpConfigUtil.getMpConfig(srtReq);
        assertEquals("the map should be 2 items.", 2, map.size());
        assertTrue("the map should contain the key " + MpConstants.ISSUER, map.containsKey(MpConstants.ISSUER));
        assertTrue("the map should contain the key " + MpConstants.PUBLIC_KEY, map.containsKey(MpConstants.PUBLIC_KEY));
        assertTrue("the map should contain the value " + MpConstants.ISSUER, map.get(MpConstants.ISSUER).equals("value_" + MpConstants.ISSUER));
        assertTrue("the map should contain the value " + MpConstants.PUBLIC_KEY, map.get(MpConstants.PUBLIC_KEY).equals("value_" + MpConstants.PUBLIC_KEY));
    }

    /**
     * Tests getMpConfig method with the config proxy service is available.
     * No data exists.
     */
    @Test
    public void getMpConfigWithConfigProxyServiceSrtReqNoProperties() {
        mockery.checking(new Expectations() {
            {
                one(srtReq).getServletContext();
                will(returnValue(servletCtx));
                one(servletCtx).getClassLoader();
                will(returnValue(cl));
                one(mpConfigProxyService).getSupportedConfigPropertyNames();
                will(returnValue(getSupportedMpConfigProps()));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.ISSUER, String.class);
                will(throwException(new NoSuchElementException()));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.PUBLIC_KEY, String.class);
                will(throwException(new NoSuchElementException()));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.KEY_LOCATION, String.class);
                will(throwException(new NoSuchElementException()));
            }
        });
        Map<String, String> map = mpConfigUtil.getMpConfig(srtReq);
        assertTrue("the map should be empty when none of the properties is available.", map.isEmpty());
    }

    /**
     * Tests getMpConfig method with the config proxy service is available.
     * make sure that empty data (after trim) is not put.
     */
    @Test
    public void getMpConfigWithConfigProxyServiceSrtReqTrim() {
        mockery.checking(new Expectations() {
            {
                one(srtReq).getServletContext();
                will(returnValue(servletCtx));
                one(servletCtx).getClassLoader();
                will(returnValue(cl));
                one(mpConfigProxyService).getSupportedConfigPropertyNames();
                will(returnValue(getSupportedMpConfigProps()));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.ISSUER, String.class);
                will(returnValue("\t\t\t\n"));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.PUBLIC_KEY, String.class);
                will(returnValue("               "));
                one(mpConfigProxyService).getConfigValue(cl, MpConstants.KEY_LOCATION, String.class);
                will(returnValue("     value_" + MpConstants.KEY_LOCATION + "          "));
            }
        });
        Map<String, String> map = mpConfigUtil.getMpConfig(srtReq);
        assertEquals("the map should be 1 item.", 1, map.size());
        assertTrue("the map should contain the value" + MpConstants.KEY_LOCATION, map.get(MpConstants.KEY_LOCATION).equals("value_" + MpConstants.KEY_LOCATION));
    }

    private Set<String> getSupportedMpConfigProps() {
        Set<String> supportedMpConfigProps = new HashSet<String>();
        supportedMpConfigProps.add(MpConstants.ISSUER);
        supportedMpConfigProps.add(MpConstants.PUBLIC_KEY);
        supportedMpConfigProps.add(MpConstants.KEY_LOCATION);
        return supportedMpConfigProps;
    }

}

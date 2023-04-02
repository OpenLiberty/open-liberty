/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

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

import com.ibm.ws.security.jwt.config.MpConfigProperties;
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
                one(mpConfigProxyService).getConfigProperties(null);
                will(returnValue(getConfigProperties()));
            }
        });
        Map<String, String> map = mpConfigUtil.getMpConfig(req);
        assertEquals("the map should be 3 items.", 3, map.size());
        assertTrue("the map should contain the key " + MpConfigProperties.ISSUER, map.containsKey(MpConfigProperties.ISSUER));
        assertTrue("the map should contain the key " + MpConfigProperties.PUBLIC_KEY, map.containsKey(MpConfigProperties.PUBLIC_KEY));
        assertTrue("the map should contain the key " + MpConfigProperties.KEY_LOCATION, map.containsKey(MpConfigProperties.KEY_LOCATION));
        assertTrue("the map should contain the value " + MpConfigProperties.ISSUER, map.get(MpConfigProperties.ISSUER).equals("value_" + MpConfigProperties.ISSUER));
        assertTrue("the map should contain the value " + MpConfigProperties.PUBLIC_KEY, map.get(MpConfigProperties.PUBLIC_KEY).equals("value_" + MpConfigProperties.PUBLIC_KEY));
        assertTrue("the map should contain the value" + MpConfigProperties.KEY_LOCATION, map.get(MpConfigProperties.KEY_LOCATION).equals("value_" + MpConfigProperties.KEY_LOCATION));
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
                one(mpConfigProxyService).getConfigProperties(cl);
                will(returnValue(getConfigProperties()));
            }
        });
        Map<String, String> map = mpConfigUtil.getMpConfig(srtReq);
        assertEquals("the map should be 3 items.", 3, map.size());
        assertTrue("the map should contain the key " + MpConfigProperties.ISSUER, map.containsKey(MpConfigProperties.ISSUER));
        assertTrue("the map should contain the key " + MpConfigProperties.PUBLIC_KEY, map.containsKey(MpConfigProperties.PUBLIC_KEY));
        assertTrue("the map should contain the key " + MpConfigProperties.KEY_LOCATION, map.containsKey(MpConfigProperties.KEY_LOCATION));
        assertTrue("the map should contain the value " + MpConfigProperties.ISSUER, map.get(MpConfigProperties.ISSUER).equals("value_" + MpConfigProperties.ISSUER));
        assertTrue("the map should contain the value " + MpConfigProperties.PUBLIC_KEY, map.get(MpConfigProperties.PUBLIC_KEY).equals("value_" + MpConfigProperties.PUBLIC_KEY));
        assertTrue("the map should contain the value" + MpConfigProperties.KEY_LOCATION, map.get(MpConfigProperties.KEY_LOCATION).equals("value_" + MpConfigProperties.KEY_LOCATION));
    }

    private MpConfigProperties getConfigProperties() {

        MpConfigProperties configProps = new MpConfigProperties();
        configProps.put(MpConfigProperties.ISSUER, "value_" + MpConfigProperties.ISSUER);
        configProps.put(MpConfigProperties.PUBLIC_KEY, "value_" + MpConfigProperties.PUBLIC_KEY);
        configProps.put(MpConfigProperties.KEY_LOCATION, "value_" + MpConfigProperties.KEY_LOCATION);
        return configProps;
    }

}

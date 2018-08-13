/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.config;

import com.ibm.ws.security.mp.jwt.MpJwtExtensionService;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.osgi.framework.ServiceReference;

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

    @SuppressWarnings("unchecked")
    private final AtomicServiceReference<MpJwtExtensionService> mpJwtExtensionServiceRef = mockery.mock(AtomicServiceReference.class, "mpJwtExtensionServiceRef");
    private final MpJwtExtensionService mpJwtExtensionService = mockery.mock(MpJwtExtensionService.class);
    private final HttpServletRequest req = mockery.mock(HttpServletRequest.class);
    private final SRTServletRequest srtReq = mockery.mock(SRTServletRequest.class);
    private final IWebAppDispatcherContext webAppDispatcherContext = mockery.mock(IWebAppDispatcherContext.class);
    private final WebApp webApp = mockery.mock(WebApp.class);
    private final ClassLoader cl = mockery.mock(ClassLoader.class);
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
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    /**
     * Tests getMpConfig method without having the extension service.
     */
    @Test
    public void getMpConfigNoExtensionService() {
        mockery.checking(new Expectations() {
            {
                one(mpJwtExtensionServiceRef).getService();
                will(returnValue(null));
            }
        });
        MpConfigUtil mpConfigUtil = new MpConfigUtil(mpJwtExtensionServiceRef);
        Map<String, String> map = mpConfigUtil.getMpConfig(req);
        assertTrue("the map should be empty.", map.isEmpty());
    }

    /**
     * Tests getMpConfig method with the extension service but mpConfig isn't available.
     */
    @Test
    public void getMpConfigWithExtensionServiceNoMpConfig() {
        mockery.checking(new Expectations() {
            {
                one(mpJwtExtensionServiceRef).getService();
                will(returnValue(mpJwtExtensionService));
                one(mpJwtExtensionService).isMpConfigAvailable();
                will(returnValue(false));
            }
        });
        MpConfigUtil mpConfigUtil = new MpConfigUtil(mpJwtExtensionServiceRef);
        Map<String, String> map = mpConfigUtil.getMpConfig(req);
        assertTrue("the map should be empty when mpConfig is not available.", map.isEmpty());
    }

    /**
     * Tests getMpConfig method with the extension service and mpConfig is available.
     * No SRTServletRequest
     */
    @Test
    public void getMpConfigWithExtensionServiceAndMpConfigNoSrtReq() {
        mockery.checking(new Expectations() {
            {
                one(mpJwtExtensionServiceRef).getService();
                will(returnValue(mpJwtExtensionService));
                one(mpJwtExtensionService).isMpConfigAvailable();
                will(returnValue(true));
                one(mpJwtExtensionService).getConfigValue(null, MpConstants.ISSUER, String.class);
                will(returnValue("value_" + MpConstants.ISSUER));
                one(mpJwtExtensionService).getConfigValue(null, MpConstants.PUBLIC_KEY, String.class);
                will(returnValue("value_" + MpConstants.PUBLIC_KEY));
                one(mpJwtExtensionService).getConfigValue(null, MpConstants.KEY_LOCATION, String.class);
                will(returnValue("value_" + MpConstants.KEY_LOCATION));
            }
        });
        MpConfigUtil mpConfigUtil = new MpConfigUtil(mpJwtExtensionServiceRef);
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
     * Tests getMpConfig method with the extension service and mpConfig is available.
     * SRTServletRequest
     */
    @Test
    public void getMpConfigWithExtensionServiceAndMpConfigSrtReq() {
        mockery.checking(new Expectations() {
            {
                one(mpJwtExtensionServiceRef).getService();
                will(returnValue(mpJwtExtensionService));
                one(mpJwtExtensionService).isMpConfigAvailable();
                will(returnValue(true));
                one(srtReq).getWebAppDispatcherContext();
                will(returnValue(webAppDispatcherContext));
                one(webAppDispatcherContext).getWebApp();
                will(returnValue(webApp));
                one(webApp).getClassLoader();
                will(returnValue(cl));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.ISSUER, String.class);
                will(returnValue("value_" + MpConstants.ISSUER));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.PUBLIC_KEY, String.class);
                will(returnValue("value_" + MpConstants.PUBLIC_KEY));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.KEY_LOCATION, String.class);
                will(returnValue("value_" + MpConstants.KEY_LOCATION));
            }
        });
        MpConfigUtil mpConfigUtil = new MpConfigUtil(mpJwtExtensionServiceRef);
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
     * Tests getMpConfig method with the extension service and mpConfig is available.
     * The MpConstants.ISSUER does not exist.
     */
    @Test
    public void getMpConfigWithExtensionServiceAndMpConfigSrtReqNoIssuer() {
        mockery.checking(new Expectations() {
            {
                one(mpJwtExtensionServiceRef).getService();
                will(returnValue(mpJwtExtensionService));
                one(mpJwtExtensionService).isMpConfigAvailable();
                will(returnValue(true));
                one(srtReq).getWebAppDispatcherContext();
                will(returnValue(webAppDispatcherContext));
                one(webAppDispatcherContext).getWebApp();
                will(returnValue(webApp));
                one(webApp).getClassLoader();
                will(returnValue(cl));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.ISSUER, String.class);
                will(throwException(new NoSuchElementException()));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.PUBLIC_KEY, String.class);
                will(returnValue("value_" + MpConstants.PUBLIC_KEY));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.KEY_LOCATION, String.class);
                will(returnValue("value_" + MpConstants.KEY_LOCATION));
            }
        });
        MpConfigUtil mpConfigUtil = new MpConfigUtil(mpJwtExtensionServiceRef);
        Map<String, String> map = mpConfigUtil.getMpConfig(srtReq);
        assertEquals("the map should be 2 items.", 2, map.size());
        assertTrue("the map should contain the key " + MpConstants.PUBLIC_KEY, map.containsKey(MpConstants.PUBLIC_KEY));
        assertTrue("the map should contain the key " + MpConstants.KEY_LOCATION, map.containsKey(MpConstants.KEY_LOCATION));
        assertTrue("the map should contain the value " + MpConstants.PUBLIC_KEY, map.get(MpConstants.PUBLIC_KEY).equals("value_" + MpConstants.PUBLIC_KEY));
        assertTrue("the map should contain the value" + MpConstants.KEY_LOCATION, map.get(MpConstants.KEY_LOCATION).equals("value_" + MpConstants.KEY_LOCATION));
    }

    /**
     * Tests getMpConfig method with the extension service and mpConfig is available.
     * The MpConstants.PUBLIC_KEY does not exist.
     */
    @Test
    public void getMpConfigWithExtensionServiceAndMpConfigSrtReqNoPublicKey() {
        mockery.checking(new Expectations() {
            {
                one(mpJwtExtensionServiceRef).getService();
                will(returnValue(mpJwtExtensionService));
                one(mpJwtExtensionService).isMpConfigAvailable();
                will(returnValue(true));
                one(srtReq).getWebAppDispatcherContext();
                will(returnValue(webAppDispatcherContext));
                one(webAppDispatcherContext).getWebApp();
                will(returnValue(webApp));
                one(webApp).getClassLoader();
                will(returnValue(cl));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.ISSUER, String.class);
                will(returnValue("value_" + MpConstants.ISSUER));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.PUBLIC_KEY, String.class);
                will(throwException(new NoSuchElementException()));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.KEY_LOCATION, String.class);
                will(returnValue("value_" + MpConstants.KEY_LOCATION));
            }
        });
        MpConfigUtil mpConfigUtil = new MpConfigUtil(mpJwtExtensionServiceRef);
        Map<String, String> map = mpConfigUtil.getMpConfig(srtReq);
        assertEquals("the map should be 2 items.", 2, map.size());
        assertTrue("the map should contain the key " + MpConstants.ISSUER, map.containsKey(MpConstants.ISSUER));
        assertTrue("the map should contain the key " + MpConstants.KEY_LOCATION, map.containsKey(MpConstants.KEY_LOCATION));
        assertTrue("the map should contain the value " + MpConstants.ISSUER, map.get(MpConstants.ISSUER).equals("value_" + MpConstants.ISSUER));
        assertTrue("the map should contain the value" + MpConstants.KEY_LOCATION, map.get(MpConstants.KEY_LOCATION).equals("value_" + MpConstants.KEY_LOCATION));
    }

    /**
     * Tests getMpConfig method with the extension service and mpConfig is available.
     * The MpConstants.KEY_LOCATION does not exist.
     */
    @Test
    public void getMpConfigWithExtensionServiceAndMpConfigSrtReqNoKeyLocation() {
        mockery.checking(new Expectations() {
            {
                one(mpJwtExtensionServiceRef).getService();
                will(returnValue(mpJwtExtensionService));
                one(mpJwtExtensionService).isMpConfigAvailable();
                will(returnValue(true));
                one(srtReq).getWebAppDispatcherContext();
                will(returnValue(webAppDispatcherContext));
                one(webAppDispatcherContext).getWebApp();
                will(returnValue(webApp));
                one(webApp).getClassLoader();
                will(returnValue(cl));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.ISSUER, String.class);
                will(returnValue("value_" + MpConstants.ISSUER));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.PUBLIC_KEY, String.class);
                will(returnValue("value_" + MpConstants.PUBLIC_KEY));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.KEY_LOCATION, String.class);
                will(throwException(new NoSuchElementException()));
            }
        });
        MpConfigUtil mpConfigUtil = new MpConfigUtil(mpJwtExtensionServiceRef);
        Map<String, String> map = mpConfigUtil.getMpConfig(srtReq);
        assertEquals("the map should be 2 items.", 2, map.size());
        assertTrue("the map should contain the key " + MpConstants.ISSUER, map.containsKey(MpConstants.ISSUER));
        assertTrue("the map should contain the key " + MpConstants.PUBLIC_KEY, map.containsKey(MpConstants.PUBLIC_KEY));
        assertTrue("the map should contain the value " + MpConstants.ISSUER, map.get(MpConstants.ISSUER).equals("value_" + MpConstants.ISSUER));
        assertTrue("the map should contain the value " + MpConstants.PUBLIC_KEY, map.get(MpConstants.PUBLIC_KEY).equals("value_" + MpConstants.PUBLIC_KEY));
    }

    /**
     * Tests getMpConfig method with the extension service and mpConfig is available.
     * No data exists.
     */
    @Test
    public void getMpConfigWithExtensionServiceAndMpConfigSrtReqNoProperties() {
        mockery.checking(new Expectations() {
            {
                one(mpJwtExtensionServiceRef).getService();
                will(returnValue(mpJwtExtensionService));
                one(mpJwtExtensionService).isMpConfigAvailable();
                will(returnValue(true));
                one(srtReq).getWebAppDispatcherContext();
                will(returnValue(webAppDispatcherContext));
                one(webAppDispatcherContext).getWebApp();
                will(returnValue(webApp));
                one(webApp).getClassLoader();
                will(returnValue(cl));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.ISSUER, String.class);
                will(throwException(new NoSuchElementException()));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.PUBLIC_KEY, String.class);
                will(throwException(new NoSuchElementException()));
                one(mpJwtExtensionService).getConfigValue(cl, MpConstants.KEY_LOCATION, String.class);
                will(throwException(new NoSuchElementException()));
            }
        });
        MpConfigUtil mpConfigUtil = new MpConfigUtil(mpJwtExtensionServiceRef);
        Map<String, String> map = mpConfigUtil.getMpConfig(srtReq);
        assertTrue("the map should be empty when none of the properties is available.", map.isEmpty());
    }
}

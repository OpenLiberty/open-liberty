/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.web;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

public class OidcSessionManagementUtilTest {

    private static SharedOutputManager outputMgr;

    private static String CLIENT_ID = "clientId";
    private static String BROWSER_STATE = "BROWSERSTATEVALUE";
    private static String SALT = "salt";
    private static String SALT_DELIMITER = ".";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void calculateSessionStateNormalParams() {
        // base64(sha256(CLIENT_ID + BROWSER_STATE + SALT)) + SALT_DELIMITER + SALT
        String expected = "SvQmlVGrmC4l8/kuwiSArKwoV+jAjdpIri2oT5Cdu1U=" + SALT_DELIMITER + SALT;
        String calculated = OidcSessionManagementUtil.calculateSessionState(CLIENT_ID, BROWSER_STATE, SALT);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateNullClientId() {
        // base64(sha256("null" + BROWSER_STATE + SALT)) + SALT_DELIMITER + SALT
        String expected = "iI2afTXH03JnPOfjqGdSuCQJp7AEt3mGBzo9km1mBdw=" + SALT_DELIMITER + SALT;
        String calculated = OidcSessionManagementUtil.calculateSessionState(null, BROWSER_STATE, SALT);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateEmptyClientId() {
        // base64(sha256(BROWSER_STATE + SALT)) + SALT_DELIMITER + SALT
        String expected = "PiuR8582PQ5UuLfkO2/JMPQSBomJdCHb/qM7B75R4M0=" + SALT_DELIMITER + SALT;
        String calculated = OidcSessionManagementUtil.calculateSessionState("", BROWSER_STATE, SALT);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateNullBrowserState() {
        // base64(sha256(CLIENT_ID + "null" + SALT)) + SALT_DELIMITER + SALT
        String expected = "1NKBvS+YJgfJm0/s69YpN8mOvl8z6pgo5jF7jqqrkCQ=" + SALT_DELIMITER + SALT;
        String calculated = OidcSessionManagementUtil.calculateSessionState(CLIENT_ID, null, SALT);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateEmptyBrowserState() {
        // base64(sha256(CLIENT_ID + SALT)) + SALT_DELIMITER + SALT
        String expected = "6P0kbtVuKEbf41Mi3MWN78LGB+GmyiDVbMj+3geCraI=" + SALT_DELIMITER + SALT;
        String calculated = OidcSessionManagementUtil.calculateSessionState(CLIENT_ID, "", SALT);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateNullSalt() {
        // base64(sha256(CLIENT_ID + BROWSER_STATE))
        String expected = "eikUpPhMlAS0wyehh8Wm4I+sjxluM5AhrDaJr9mIbco=";
        String calculated = OidcSessionManagementUtil.calculateSessionState(CLIENT_ID, BROWSER_STATE, null);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateEmptySalt() {
        // base64(sha256(CLIENT_ID + BROWSER_STATE))
        String expected = "eikUpPhMlAS0wyehh8Wm4I+sjxluM5AhrDaJr9mIbco=";
        String calculated = OidcSessionManagementUtil.calculateSessionState(CLIENT_ID, BROWSER_STATE, "");
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateNullClientIdNullBrowserState() {
        // base64(sha256("null" + "null" + SALT)) + SALT_DELIMITER + SALT
        String expected = "6OYejv0uqrGrwiu7aOaBy1VIJhMBHiFmO3z2KdzGdEI=" + SALT_DELIMITER + SALT;
        String calculated = OidcSessionManagementUtil.calculateSessionState(null, null, SALT);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateNullClientIdEmptyBrowserState() {
        // base64(sha256("null" + SALT)) + SALT_DELIMITER + SALT
        String expected = "9T8Cpe+C+5JenKUnFBukPdlosVPp6U++o/t5EAP6ekU=" + SALT_DELIMITER + SALT;
        String calculated = OidcSessionManagementUtil.calculateSessionState(null, "", SALT);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateEmptyClientIdNullBrowserState() {
        // base64(sha256("null" + SALT)) + SALT_DELIMITER + SALT
        String expected = "9T8Cpe+C+5JenKUnFBukPdlosVPp6U++o/t5EAP6ekU=" + SALT_DELIMITER + SALT;
        String calculated = OidcSessionManagementUtil.calculateSessionState("", null, SALT);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateEmptyClientIdEmptyBrowserState() {
        // base64(sha256(SALT)) + SALT_DELIMITER + SALT
        String expected = "Y0ea1poJCyWCd+yPum+ZQZov+ySJgVEGV8lEzNEUjpc=" + SALT_DELIMITER + SALT;
        String calculated = OidcSessionManagementUtil.calculateSessionState("", "", SALT);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateNullClientIdNullSalt() {
        // base64(sha256("null" + BROWSER_STATE))
        String expected = "1qIP/w9vzK/lOFMvrMQ1ZtM0wyM7+VKYnr5FSs1LnGI=";
        String calculated = OidcSessionManagementUtil.calculateSessionState(null, BROWSER_STATE, null);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateNullClientIdEmptySalt() {
        // base64(sha256("null" + BROWSER_STATE))
        String expected = "1qIP/w9vzK/lOFMvrMQ1ZtM0wyM7+VKYnr5FSs1LnGI=";
        String calculated = OidcSessionManagementUtil.calculateSessionState(null, BROWSER_STATE, "");
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateEmptyClientIdNullSalt() {
        // base64(sha256(BROWSER_STATE))
        String expected = "lwn2T4bHVv/3J/vYWS2Ik6XOc8COGQDjk7Kx9CQQHco=";
        String calculated = OidcSessionManagementUtil.calculateSessionState("", BROWSER_STATE, null);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateEmptyClientIdEmptySalt() {
        // base64(sha256(BROWSER_STATE))
        String expected = "lwn2T4bHVv/3J/vYWS2Ik6XOc8COGQDjk7Kx9CQQHco=";
        String calculated = OidcSessionManagementUtil.calculateSessionState("", BROWSER_STATE, "");
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateNullBrowserStateNullSalt() {
        // base64(sha256(CLIENT_ID + "null"))
        String expected = "gcb75Fgohqvl7zEkhg1JhUniypX5IjCsTphuW7FOWHg=";
        String calculated = OidcSessionManagementUtil.calculateSessionState(CLIENT_ID, null, null);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateNullBrowserStateEmptySalt() {
        // base64(sha256(CLIENT_ID + "null"))
        String expected = "gcb75Fgohqvl7zEkhg1JhUniypX5IjCsTphuW7FOWHg=";
        String calculated = OidcSessionManagementUtil.calculateSessionState(CLIENT_ID, null, "");
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateEmptyBrowserStateNullSalt() {
        // base64(sha256(CLIENT_ID))
        String expected = "kYkzYrAtAcAPtxvGvgIGXe3DNWWV/GSFgatZqUhkp+Q=";
        String calculated = OidcSessionManagementUtil.calculateSessionState(CLIENT_ID, "", null);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateEmptyBrowserStateEmptySalt() {
        // base64(sha256(CLIENT_ID))
        String expected = "kYkzYrAtAcAPtxvGvgIGXe3DNWWV/GSFgatZqUhkp+Q=";
        String calculated = OidcSessionManagementUtil.calculateSessionState(CLIENT_ID, "", "");
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateAllParamsNull() {
        // base64(sha256("null" + "null"))
        String expected = "LHvdr6b4JMsOaCCRqh2co5KIPLH1vP+VOJrcn+rnf80=";
        String calculated = OidcSessionManagementUtil.calculateSessionState(null, null, null);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateOneParamNullOneParamEmptyNullSalt() {
        // base64(sha256("null"))
        String expected = "dCNOmK/nSY+12vHzasLXiswzlGT5UHA7jAGYkvmCuQs=";
        String calculated = OidcSessionManagementUtil.calculateSessionState(null, "", null);
        assertEquals(expected, calculated);
    }

    @Test
    public void calculateSessionStateAllParamsEmpty() {
        // base64(sha256(""))
        String expected = "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=";
        String calculated = OidcSessionManagementUtil.calculateSessionState("", "", "");
        assertEquals(expected, calculated);
    }
}

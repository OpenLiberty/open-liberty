/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm.internal;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 *
 */
public class WLMServiceResultsTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    @Rule
    public TestName nameRule = new TestName();

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        // For diagnostics, uncomment this (or copy to the method being debugged):
        //outputMgr.dumpStreams();
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#setResults(int, int, java.lang.String, int, java.lang.String, byte[])}.
     */
    @Test
    public final void testSetResults() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        // Cause a Connect Error
        int wlmRC = 8;
        int wlmRSN = 0x81C;
        int wasRC = -1;
        String wlmRoutine = "CreateWorkUnit";
        String wasService = "ntv_le_createWorkUnit";
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        assertTrue(wlmRC == sr.getReturnCode());
        assertTrue(wlmRSN == sr.getReasonCode());

        assertTrue(wlmRoutine.compareTo(sr.getServiceName()) == 0);
        assertTrue(wasService.compareTo(sr.getWebSphereServiceName()) == 0);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_UnAuth_Create1() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        System.out.println("before setResults\n" + sr.toString());

        // Cause a Create Error
        int wlmRC = 8;
        int wlmRSN = 0x81C;
        int wasRC = -1;
        String wlmRoutine = "CreateWorkUnit";
        String wasService = "ntv_le_createWorkUnit";
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0153E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_UnAuth_CreateOOM() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        System.out.println("before setResults\n" + sr.toString());

        // Cause a create Error, wlm=0, was=ENOMEM
        int wlmRC = 0;
        int wlmRSN = 0;
        int wasRC = ERRNO.ENOMEM.errno();
        String wlmRoutine = "CreateWorkUnit";
        String wasService = "ntv_le_createWorkUnit";
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0151E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test(expected = SecurityException.class)
    public final void testIssueWLMServiceMessage_UnAuth_CreateWLM() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        System.out.println("before setResults\n" + sr.toString());

        // Cause a create Error
        int wlmRC = ERRNO.EMVSSAF2ERR.errno();
        int wlmRSN = 0;
        int wasRC = -1;
        String wlmRoutine = "CreateWorkUnit";
        String wasService = "ntv_le_createWorkUnit";
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        // Should throw
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_UnAuth_CreateWLMERROR() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        // Cause a create Error
        int wlmRC = ERRNO.EMVSWLMERROR.errno();
        int wlmRSN = 0x999;
        int wasRC = -1;
        String wlmRoutine = "CreateWorkUnit";
        String wasService = "ntv_le_createWorkUnit";
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0152E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_UnAuth_JoinWLM() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        // Cause a join Error
        int wlmRC = ERRNO.EMVSWLMERROR.errno();
        int wlmRSN = 777;
        int wasRC = -1;
        String wlmRoutine = "JoinWorkUnit";
        String wasService = WLMServiceResults.UNAUTH_JoinWorkUnit;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0152E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_UnAuth_JoinWLMSkip1() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        // Cause a join Error
        int wlmRC = ERRNO.EMVSWLMERROR.errno();
        int wlmRSN = WLMReasonCodes.IWMRSNCODEALREADYINENCLAVE.rsnCode();
        int wasRC = -1;
        String wlmRoutine = "JoinWorkUnit";
        String wasService = WLMServiceResults.UNAUTH_JoinWorkUnit;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(!(outputMgr.checkForStandardOut("CWWKB0152E")));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_UnAuth_JoinWLMSkip2() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        // Cause a join Error
        int wlmRC = ERRNO.EINVAL.errno();
        int wlmRSN = 777;
        int wasRC = -1;
        String wlmRoutine = "JoinWorkUnit";
        String wasService = WLMServiceResults.UNAUTH_JoinWorkUnit;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(!(outputMgr.checkForStandardOut("CWWKB0152E")));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test(expected = SecurityException.class)
    public final void testIssueWLMServiceMessage_UnAuth_LeaveSec() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        // Cause a leave Error
        int wlmRC = ERRNO.EMVSSAF2ERR.errno();
        int wlmRSN = 0;
        int wasRC = -1;
        String wlmRoutine = "LeaveWorkUnit";
        String wasService = WLMServiceResults.UNAUTH_LeaveWorkUnit;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();
        // Should throw
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_UnAuth_LeaveWLMERROR() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        // Cause a leave Error
        int wlmRC = ERRNO.EMVSWLMERROR.errno();
        int wlmRSN = 777;
        int wasRC = -1;
        String wlmRoutine = "LeaveWorkUnit";
        String wasService = WLMServiceResults.UNAUTH_LeaveWorkUnit;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0152E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test(expected = SecurityException.class)
    public final void testIssueWLMServiceMessage_UnAuth_DeleteSec() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        // Cause a delete Error
        int wlmRC = ERRNO.EMVSSAF2ERR.errno();
        int wlmRSN = 0;
        int wasRC = -1;
        String wlmRoutine = "DeleteWorkUnit";
        String wasService = WLMServiceResults.UNAUTH_DeleteWorkUnit;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();
        // Should throw
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_UnAuth_DeleteWLMERROR() {

        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        // Cause a delete Error
        int wlmRC = ERRNO.EMVSWLMERROR.errno();
        int wlmRSN = 777;
        int wasRC = -1;
        String wlmRoutine = "DeleteWorkUnit";
        String wasService = WLMServiceResults.UNAUTH_DeleteWorkUnit;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0152E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test(expected = SecurityException.class)
    public final void testIssueWLMServiceMessage_UnAuth_ConnectSec() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        // Cause an error
        int wlmRC = ERRNO.EMVSSAF2ERR.errno();
        int wlmRSN = 0;
        int wasRC = -1;
        String wlmRoutine = "ConnectWorkMgr";
        String wasService = WLMServiceResults.UNAUTH_ConnectAsWorkMgr;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();
        // Should throw
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_UnAuth_ConnectWLMERROR() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = ERRNO.EMVSWLMERROR.errno();
        int wlmRSN = 777;
        int wasRC = -1;
        String wlmRoutine = "ConnectWorkMgr";
        String wasService = WLMServiceResults.UNAUTH_ConnectAsWorkMgr;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0152E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_Auth_ConnectPC() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = 0;
        int wlmRSN = 0;
        int wasRC = -1;
        String wlmRoutine = "IWM4CON";
        String wasService = WLMServiceResults.AUTH_ConnectAsWorkMgr;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0155E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_Auth_ConnectWLMError() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = 8;
        int wlmRSN = 777;
        int wasRC = 0;
        String wlmRoutine = "IWM4CON";
        String wasService = WLMServiceResults.AUTH_ConnectAsWorkMgr;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0152E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_Auth_ConnectUnAuthStubs() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = 0;
        int wlmRSN = 0;
        int wasRC = WLMServiceResults.WASRETURNCODE_FAILED_TO_FIND_UNAUTH_FUNCTION_STUBS;
        String wlmRoutine = "IWM4CON";
        String wasService = WLMServiceResults.AUTH_ConnectAsWorkMgr;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0158E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_Auth_Connect_BPX4IPT() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = 0;
        int wlmRSN = 0;
        int wasRC = WLMServiceResults.WASRETURNCODE_FAILED_CALLING_BPX4IPT;
        String wlmRoutine = "IWM4CON";
        String wasService = WLMServiceResults.AUTH_ConnectAsWorkMgr;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0159E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_Auth_DisconnectPC() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = 0;
        int wlmRSN = 0;
        int wasRC = -1;
        String wlmRoutine = "IWMDISC";
        String wasService = WLMServiceResults.AUTH_DisconnectAsWorkMgr;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0155E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_Auth_DisconnectWLMError() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = 8;
        int wlmRSN = 777;
        int wasRC = 0;
        String wlmRoutine = "IWMDISC";
        String wasService = WLMServiceResults.AUTH_DisconnectAsWorkMgr;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0152E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_Auth_CreateOOM() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = 8;
        int wlmRSN = 777;
        int wasRC = ERRNO.ENOMEM.errno();
        String wlmRoutine = "IWM4ECRE";
        String wasService = WLMServiceResults.AUTH_CreateWorkUnit;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0151E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_Auth_CreatePC() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = 8;
        int wlmRSN = 777;
        int wasRC = -1;
        String wlmRoutine = "IWM4ECRE";
        String wasService = WLMServiceResults.AUTH_CreateWorkUnit;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0155E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_Auth_CreateWLMError() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = 8;
        int wlmRSN = 777;
        int wasRC = 0;
        String wlmRoutine = "IWM4ECRE";
        String wasService = WLMServiceResults.AUTH_CreateWorkUnit;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0152E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_Auth_DeletePC() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = 8;
        int wlmRSN = 777;
        int wasRC = -1;
        String wlmRoutine = "IWMEDELE";
        String wasService = WLMServiceResults.AUTH_DeleteWorkUnit;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0155E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_Auth_CreateRegFailure() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = 0;
        int wlmRSN = 0;
        int wasRC = WLMServiceResults.WASRETURNCODE_FAILED_TO_REGISTER;
        String wlmRoutine = "IWM4ECRE";
        String wasService = WLMServiceResults.AUTH_CreateWorkUnit;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0156E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_Auth_DeleteWLMError() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = 8;
        int wlmRSN = 777;
        int wasRC = 0;
        String wlmRoutine = "IWMEDELE";
        String wasService = WLMServiceResults.AUTH_DeleteWorkUnit;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0152E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_Auth_DeleteRegValidate() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = 0;
        int wlmRSN = 0;
        int wasRC = WLMServiceResults.WASRETURNCODE_FAILED_TO_VALIDATE_IN_REGISTRY;
        String wlmRoutine = "IWMEDELE";
        String wasService = WLMServiceResults.AUTH_DeleteWorkUnit;
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, null);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0157E"));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.WLMServiceResults#issueWLMServiceMessage()}.
     */
    @Test
    public final void testIssueWLMServiceMessage_invalidService() {
        WLMServiceResults sr = WLMServiceResults.getWLMServiceResult();

        assertTrue(sr != null);

        int wlmRC = 8;
        int wlmRSN = 777;
        int wasRC = 0;
        String wlmRoutine = "UnknownWLMRoutine";
        String wasService = "UnknownWasService";
        byte[] retData = "returndata goo".getBytes();
        WLMServiceResults.setResults(wlmRC, wlmRSN, wlmRoutine, wasRC, wasService, retData);

        System.out.println("before issue\n" + sr.toString());

        sr.issueWLMServiceMessage();

        assertTrue(outputMgr.checkForStandardOut("CWWKB0154E"));
    }
}

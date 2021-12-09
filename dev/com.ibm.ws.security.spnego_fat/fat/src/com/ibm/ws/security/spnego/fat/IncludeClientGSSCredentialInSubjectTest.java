/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.CommonTest;
import com.ibm.ws.security.spnego.fat.config.InitClass;
import com.ibm.ws.security.spnego.fat.config.MessageConstants;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
@Mode(TestMode.QUARANTINE)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class IncludeClientGSSCredentialInSubjectTest extends CommonTest {

    private static final Class<?> c = IncludeClientGSSCredentialInSubjectTest.class;

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setUp";
        Log.info(c, thisMethod, "Starting the server...");
        List<String> checkApps = new ArrayList<String>();
        checkApps.add("basicauth");

        commonSetUp("IncludeClientGSSCredentialInSubjectTest", null, checkApps, SPNEGOConstants.NO_PROPS, SPNEGOConstants.DONT_START_SERVER);
    }

    /**
     * Test description:
     * - includeClientGSSCredentialInSubject attribute is not set in server.xml (default value is true).
     * - Access a protected resource by including the SPNEGO token in the request.
     * - Check that the subject returned contains the client's GSS credentials.
     *
     * Expected results:
     * - Authentication should be successful and access to the protected resource should be granted.
     * - Client subject should contain GSS credentials.
     */

    @Test
    public void testIncludeClientGssCredentialsNotSpecified() {
        try {
            testHelper.reconfigureServer("includeClientGssCreds_notSpecified.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            commonSuccessfulSpnegoServletCall();
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - includeClientGSSCredentialInSubject attribute is set to true in server.xml.
     * - Access a protected resource by including the SPNEGO token in the request.
     * - Check that the subject returned contains the client's GSS credentials.
     *
     * Expected results:
     * - Authentication should be successful and access to the protected resource should be granted.
     * - Client subject should contain GSS credentials.
     */

    @Test
    public void testIncludeClientGssCredentialsTrue() {
        try {
            testHelper.reconfigureServer("includeClientGssCreds_true.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            commonSuccessfulSpnegoServletCall();
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - includeClientGSSCredentialInSubject attribute is set to false in server.xml.
     * - Access a protected resource by including the SPNEGO token in the request.
     * - Check that the subject returned does not contain the client's GSS credentials.
     *
     * Expected results:
     * - Authentication should be successful and access to the protected resource should be granted.
     * - Client subject should not contain GSS credentials.
     */

    @Test
    public void testIncludeClientGssCredentialsFalse() {
        try {
            testHelper.reconfigureServer("includeClientGssCreds_false.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            Map<String, String> headers = createCommonHeaders();
            String response = successfulServletCall(SPNEGOConstants.SIMPLE_SERVLET, headers, FATSuite.COMMON_TOKEN_USER,
                                                    FATSuite.COMMON_TOKEN_USER_IS_EMPLOYEE, FATSuite.COMMON_TOKEN_USER_IS_MANAGER);

            expectation.unsuccesfulSpnegoServletCall(response, SPNEGOConstants.OWNER_STRING + FATSuite.COMMON_TOKEN_USER);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - includeClientGSSCredentialInSubject attribute is set to true in server.xml.
     * - The SPNEGO token used specifies that GSS credentials should not be included in the subject.
     * - Access a protected resource by including the SPNEGO token in the request.
     * - Check that the subject returned does not contain the client's GSS credentials.
     *
     * Expected results:
     * - Authentication should be successful and access to the protected resource should be granted.
     * - CWWKS4310W message should be output saying the delegated GSS credentials were not found for the user.
     * - Client subject should not contain GSS credentials.
     */

    //When using jdk 11 we should allow this exception
    @Test
    @AllowedFFDC("org.ietf.jgss.GSSException")
    public void testClientDoesNotIncludeGSSCredentials() {
        try {
            testHelper.reconfigureServer("includeClientGssCreds_true.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4310W");

            String spnegoToken = testHelper.createSpnegoToken(InitClass.FIRST_USER, InitClass.FIRST_USER_PWD, TARGET_SERVER, null, null,
                                                              SPNEGOConstants.SERVER_KRB5_CONFIG_FILE, krb5Helper, false);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + spnegoToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = successfulServletCall(SPNEGOConstants.SIMPLE_SERVLET, headers, InitClass.FIRST_USER, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);

            expectation.unsuccesfulSpnegoServletCall(response, SPNEGOConstants.OWNER_STRING + InitClass.FIRST_USER);

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.GSSCREDENTIALS_NOT_RECEIVED_FOR_USER_CWWKS4310W + InitClass.FIRST_USER);
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.EXPECTED_MESSAGE);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

}

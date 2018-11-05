/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.fat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that contains tests for JWT Builder api configuration when using
 * a non supported JDK like 1.6 and 1.7. This test class will not run on Java 1.8 or 1.9.
 *
 * The main flow of the test is:
 * <OL>
 * <LI>Start the server that has one of the three forms of applications, that being Base Security Context,
 * Base Injection Request and Base Claim Injection.
 * <LI>Since the microprofile feature is not supported on JDK 1.6 or 1.7, we should receive an error while starting the
 * application
 * saying that the servlet did not start.
 * </OL>
 *
 **/

// TODO - need to finish on Windows - don't have a Java 7 for Mac right now.

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MPJwtJDKTests extends CommonMpJwtFat {

    public static Class<?> thisClass = MPJwtJDKTests.class;

    private static final String javaVersion = System.getProperty("java.version");
    private static boolean isJava80OrGreater = true;
    private static boolean shouldGetServletInitializationException = true;

    @Server("com.ibm.ws.security.mp.jwt.fat")
    public static LibertyServer resourceServer;

    private final TestValidationUtils validationUtils = new TestValidationUtils();

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartRSServerForTests(resourceServer, "rs_server_jdk.xml");

    }

    protected static void setUpAndStartRSServerForTests(LibertyServer server, String configFile) throws Exception {
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, SecurityFatHttpUtils.getServerHostName());
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, SecurityFatHttpUtils.getServerHostIp());
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "rsacert");
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", "");
        deployRSServerApiTestApps(server);
        serverTracker.addServer(server);
        // make sure we get error messages during startup
        server.startServerUsingExpandedConfiguration(configFile, getExpectedMsgsBasedOnJavaVersion());
        SecurityFatHttpUtils.saveServerPorts(server, MpJwtFatConstants.BVT_SERVER_1_PORT_NAME_ROOT);
        server.addIgnoredErrors(Arrays.asList(MpJwtMessageConstants.CWWKW1001W_CDI_RESOURCE_SCOPE_MISMATCH));
    }

    // this test should be done during startup...
    //    @AllowedFFDC("java.lang.NoClassDefFoundError")
    //    @Mode(TestMode.LITE)
    //    @Test
    //    public void MPJwtEarlyJavaVersionsTests_basicSetup_runtimeJDKVersion() throws Exception {
    //        resourceTestServer.reconfigServer("rs_server_jdk.xml", _testName, MpJwtConstants.JUNIT_REPORTING, getExpectedMsgsBasedOnJavaVersion());
    //        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, configFile, reconfigMsgs);
    //        expectedMessages();
    //    }

    @MaximumJavaLevel(javaLevel = 7)
    //    @AllowedFFDC(value = { "java.lang.NoClassDefFoundError", "javax.servlet.ServletException" })
    @Test
    public void MPJwtEarlyJavaVersionsTests_IncludeJWTToken_BasicApp() throws Exception {

        MPJwtEarlyJavaVersionsTests_mainPath_ContextNull_test(MpJwtFatConstants.MPJWT_APP_SEC_CONTEXT_REQUEST_SCOPE);

    }

    @MaximumJavaLevel(javaLevel = 7)
    //    @AllowedFFDC(value = { "java.lang.NoClassDefFoundError", "javax.servlet.ServletException" })
    @Test
    public void MPJwtEarlyJavaVersionsTests_IncludeJWTToken_usingInjection() throws Exception {

        MPJwtEarlyJavaVersionsTests_mainPath_ContextNull_test(MpJwtFatConstants.MPJWT_APP_TOKEN_INJECT_REQUEST_SCOPE);
    }

    @MaximumJavaLevel(javaLevel = 7)
    //    @AllowedFFDC(value = { "java.lang.NoClassDefFoundError", "javax.servlet.ServletException" })
    @Test
    public void MPJwtEarlyJavaVersionsTests_IncludeJWTToken_usingClaimInjection() throws Exception {

        MPJwtEarlyJavaVersionsTests_mainPath_ContextNull_test(MpJwtFatConstants.MPJWT_APP_CLAIM_INJECT_REQUEST_SCOPE);

    }

    public void MPJwtEarlyJavaVersionsTests_mainPath_ContextNull_test(String app) throws Exception {

        String builtToken = "eyJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwic3ViIjoidGVzdHVzZXIiLCJ1cG4iOiJ0ZXN0dXNlciIsImlzcyI6Imh0dHBzOi8vOS40MS4yNDQuMTgyOjg5NDcvand0L2RlZmF1bHRKV1QiLCJleHAiOjkyMjMzNzIwMzY4NTQ3NzYsImlhdCI6MTUwNTIzMTI5MH0.M1MZ8PCVvE5xrHCWGuk9h-9C3QUhOLKXaQjV3jknFXhV2DP7jT_hTqehVMG8bqYAw2aoRwLiDnXTyWAuenei-hDYKhDB4pEHBAKvSJUzL5CrCWkwlFV4uMAq2bZI5S9AkS_8JClrJJcOemvoLV-OXQU60BuCSevhdlv7rDdm75M_kHtDNiQQXi9LywgQM54nG4vCx7lVghWniLtLP8609VUpTAnMIwKrtu54eXJY4R906p9Po79_0NPVWPnS64C4bsi-H8ubYzB4QltiuWavgmt59C4ggHXQ8YsAntc_cFjZRTI3HDo4nxTCZC72aXoFThRkrsiw_VERgh0M7Bmyeg";

        String testUrl = buildAppUrl(resourceServer, MpJwtFatConstants.MICROPROFILE_SERVLET, app);

        WebClient webClient = actions.createWebClient();

        Page response = actions.invokeUrlWithBearerToken(_testName, webClient, testUrl, builtToken);
        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();

        validationUtils.validateResult(response, expectations);

        //        WebConversation wc = new WebConversation();
        //
        //        JwtBuilderSettings updatedJwtBuilderSettings = jwtBuilderSettings.copyTestSettings();
        //
        //        updatedJwtBuilderSettings.setRSProtectedResource(app);
        //
        //        updatedJwtBuilderSettings.setConfigId(null);
        //        // in order to consume the token, we need a subject, so add
        //        updatedJwtBuilderSettings.setUser("testuser");
        //        updatedJwtBuilderSettings.setClaims("upn#:#testuser");
        //        updatedJwtBuilderSettings.setSignatureAlgorithm(updatedJwtBuilderSettings.getSignatureAlg());
        //
        //        // get the token so that we can pass it in the header to the RS Protected App request
        //        String token = "eyJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwic3ViIjoidGVzdHVzZXIiLCJ1cG4iOiJ0ZXN0dXNlciIsImlzcyI6Imh0dHBzOi8vOS40MS4yNDQuMTgyOjg5NDcvand0L2RlZmF1bHRKV1QiLCJleHAiOjkyMjMzNzIwMzY4NTQ3NzYsImlhdCI6MTUwNTIzMTI5MH0.M1MZ8PCVvE5xrHCWGuk9h-9C3QUhOLKXaQjV3jknFXhV2DP7jT_hTqehVMG8bqYAw2aoRwLiDnXTyWAuenei-hDYKhDB4pEHBAKvSJUzL5CrCWkwlFV4uMAq2bZI5S9AkS_8JClrJJcOemvoLV-OXQU60BuCSevhdlv7rDdm75M_kHtDNiQQXi9LywgQM54nG4vCx7lVghWniLtLP8609VUpTAnMIwKrtu54eXJY4R906p9Po79_0NPVWPnS64C4bsi-H8ubYzB4QltiuWavgmt59C4ggHXQ8YsAntc_cFjZRTI3HDo4nxTCZC72aXoFThRkrsiw_VERgh0M7Bmyeg";
        //
        //        List<validationData> expectations = vData.addSuccessStatusCodesForActions(MpJwtConstants.INVOKE_RS_PROTECTED_RESOURCE_LOGIN_ACTIONS);
        //        if (shouldGetServletInitializationException) {
        //            shouldGetServletInitializationException = false;
        //            expectations = validationTools.addMessageExpectation(resourceTestServer, expectations, MpJwtConstants.INVOKE_RS_PROTECTED_RESOURCE, MpJwtConstants.MESSAGES_LOG, MpJwtConstants.STRING_CONTAINS, "Message log did not contain the exception while starting the servlet.", MpJwtMessageConstants.SERVLET_DID_NOT_START);
        //        }
        //        expectations = vData.addExpectation(expectations, MpJwtConstants.INVOKE_PROTECTED_RESOURCE, MpJwtConstants.RESPONSE_FULL, MpJwtConstants.STRING_CONTAINS, "Did not obtained the expected response: " + MpJwtConstants.JWT_CONTEXT_NULL, null, MpJwtConstants.JWT_CONTEXT_NULL);
        //
        //        WebResponse response = invokeMPJWTApp(_testName, wc, token, updatedJwtBuilderSettings, expectations);
        //
        //        List<validationData> expectations2 = vData.addSuccessStatusCodesForActions(MpJwtConstants.INVOKE_RS_PROTECTED_RESOURCE, MpJwtConstants.INVOKE_RS_PROTECTED_RESOURCE_ONLY_ACTIONS);
        //        expectations2 = vData.addExpectation(expectations2, MpJwtConstants.PERFORM_LOGIN, MpJwtConstants.RESPONSE_FULL, MpJwtConstants.STRING_CONTAINS, "Did not receive error 500 and ServletException in Response.", null, MpJwtConstants.RECV_ERROR_CODE);
        //        updatedJwtBuilderSettings.setAdminUser("testuser");
        //        updatedJwtBuilderSettings.setAdminPswd("testuserpwd");
        //        helper.performLogin(_testName, wc, response, updatedJwtBuilderSettings, expectations2);

    }

    public static boolean isLessThanJava80() {
        if (javaVersion.startsWith("1.6") || javaVersion.startsWith("1.7")) {
            isJava80OrGreater = false;
        }
        return isJava80OrGreater;
    }

    public static List<String> getExpectedMsgsBasedOnJavaVersion() {
        String thisMethod = "getExpectedMsgsBasedOnJavaVersion()";
        if (!isLessThanJava80()) {
            List<String> msgs = new ArrayList<String>();
            msgs.add(MpJwtMessageConstants.MIN_JDK_FEATURE_REQUIREMENT);
            msgs.add(MpJwtMessageConstants.UNRESOLVED_BUNDLE);
            return msgs;
        } else {
            Log.info(thisClass, thisMethod, "The test class will not run because this test class is designed to run on JDK 1.6 and 1.7");
            //            return MpJwtFatConstants.NO_EXTRA_MSGS;
            return null;
        }

    }

    //    public static void expectedMessages() throws Exception {
    //        if (!isLessThanJava80()) {
    //            ignoreMessages(resourceServer, getExpectedMsgsBasedOnJavaVersion().toArray(new String[getExpectedMsgsBasedOnJavaVersion().size()]));
    //            resourceServer.addIgnoredServerException(MpJwtMessageConstants.ANNOTATION_METHOD_WARNING);
    //            resourceServer.addIgnoredServerException(MpJwtMessageConstants.MICROPROFILE_NOCLASSDEFFOUND);
    //            resourceServer.addIgnoredServerException(MpJwtMessageConstants.CWWKW1001W_CDI_RESOURCE_SCOPE_MISMATCH);
    //            resourceServer.addIgnoredServerException(MpJwtMessageConstants.ANNOTATION_FIELD_WARNING);
    //            resourceServer.addIgnoredServerException(MpJwtMessageConstants.MICROPROFILE_APP_DID_NOT_START_EXCEPTION);
    //            resourceServer.addIgnoredServerException(MpJwtMessageConstants.MICROPROFILE_INITIALIZATION_EXCEPTION);
    //            resourceServer.addIgnoredServerException(MpJwtMessageConstants.MICROPROFILE_INITIALIZATION_EXCEPTION_2);
    //        } else {
    //            resourceServer.addIgnoredServerException(MpJwtMessageConstants.CWWKW1001W_CDI_RESOURCE_SCOPE_MISMATCH);
    //        }
    //
    //    }

}

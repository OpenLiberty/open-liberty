/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.test;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests appSecurity-5.0
 */
@RunWith(FATRunner.class)
public class SimplestAnnotatedTest extends CommonSecurityFat {

    protected static Class<?> thisClass = SimplestAnnotatedTest.class;

    private static final String APP_NAME = "SimplestAnnotated";

    @Server("io.openliberty.security.jakartasec-3.0_fat.op")
    public static LibertyServer opServer;
    @Server("io.openliberty.security.jakartasec-3.0_fat.rp")
    public static LibertyServer rpServer;

    private final TestActions actions = new TestActions();
    private final TestValidationUtils validationUtils = new TestValidationUtils();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(rpServer, APP_NAME + ".war", "oidc.servlets");

        transformAppsInDefaultDirs(opServer, "dropins", JakartaEE10Action.ID);
        transformAppsInDefaultDirs(rpServer, "dropins", JakartaEE10Action.ID);

        serverTracker.addServer(opServer);
        serverTracker.addServer(rpServer);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(opServer, Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        rpServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(rpServer, Constants.BVT_SERVER_2_PORT_NAME_ROOT);
    }

    @Test
    public void testSimplestAnnotatedServlet() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String url = "https://localhost:" + rpServer.getBvtSecurePort() + "/" + APP_NAME + "/OidcAnnotatedServlet";

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = CommonExpectations.successfullyReachedLoginPage(currentAction);
        Page response = actions.invokeUrl(_testName, webClient, url);

        validationUtils.validateResult(response, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;
        expectations = CommonExpectations.successfullyReachedUrl(null, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);
        // confirm protected resource was accessed
        validationUtils.validateResult(response, currentAction, expectations);

//        try {
//            HttpUtils.findStringInReadyUrl(rpServer, "/" + APP_NAME + "/OidcAnnotatedServlet", "Hello world!");
//        } catch (Exception e) {
//            throw e;
//        }
    }
}

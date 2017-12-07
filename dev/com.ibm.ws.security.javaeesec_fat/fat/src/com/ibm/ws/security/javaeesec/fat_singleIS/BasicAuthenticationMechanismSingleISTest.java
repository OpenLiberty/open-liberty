/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat_singleIS;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.javaeesec.fat.BasicAuthenticationMechanismTest;
import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class BasicAuthenticationMechanismSingleISTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = BasicAuthenticationMechanismTest.class;
    protected String queryString = "/JavaEESecBasicAuthServlet/JavaEESecBasic";
    protected static String[] warList = { "JavaEESecBasicAuthServlet.war", "JavaEESecAnnotatedBasicAuthServlet.war",
                                          "JavaEEsecFormAuth.war", "JavaEEsecFormAuthRedirect.war" };
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";

    protected DefaultHttpClient httpclient;

    public BasicAuthenticationMechanismSingleISTest() {
        super(myServer, logClass);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password in the javaeesec_basic role and verify that
     * <LI> Authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Verify response contains the appropiate required information.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testBasicAuthValidUserInRole_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser, Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);

        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.javaeesec_basicRoleUser, Constants.getRemoteUserFound + Constants.javaeesec_basicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with a valid userId and password where the user is in a group in javaeesec_basic role and verify that
     * <LI> JASPI authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed:
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Servlet is accessed and it prints information about the subject: getAuthType, getUserPrincipal, getRemoteUser.
     * </OL>
     */
    //@Test
    public void testJaspiBasicAuthValidGroupInRole_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleGroupUser, Constants.javaeesec_basicRoleGroupPwd,
                                                          HttpServletResponse.SC_OK);
        // verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.javaeesec_basicRoleGroupUser,
                           Constants.getRemoteUserFound + Constants.javaeesec_basicRoleGroupUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication using annotation.
     * <LI> Login with a valid userId and password in the javaeesec_basic role and verify that
     * <LI> Authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Verify response contains the appropiate required information.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAnnotatedBasicAuthValidUserInRole_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + "/JavaEESecAnnotatedBasicAuthServlet/JavaEESecAnnotatedBasic",
                                                          Constants.javaeesec_basicRoleLDAPUser,
                                                          Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.javaeesec_basicRoleLDAPUser, Constants.getRemoteUserFound + Constants.javaeesec_basicRoleLDAPUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication using annotation.
     * <LI> Login with an invalid userId and password in the javaeesec_basic role and verify that
     * <LI> Authentication Failure error occurs.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Verify response contains the appropiate required information.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testBasicAuthValidUserInRole_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.jaspi_invalidUser, Constants.jaspi_invalidPwd,
                                                          HttpServletResponse.SC_FORBIDDEN);
        verifyResponseAuthenticationFailed(response);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication using annotation.
     * <LI> Login with an valid userId but invalid password in the javaeesec_basic role and verify that
     * <LI> Authentication Failure error occurs.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Verify response contains the appropiate required information.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testBasicAuthValidUserInRole_DeniedAccess_WrongPassword() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser, Constants.jaspi_invalidPwd,
                                                          HttpServletResponse.SC_FORBIDDEN);

        verifyResponseAuthenticationFailed(response);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}

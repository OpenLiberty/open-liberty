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

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class FormHttpAuthenticationMechanismTestSingleISTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = FormHttpAuthenticationMechanismTestSingleISTest.class;
    protected static String[] warList = { "JavaEESecBasicAuthServlet.war", "JavaEESecAnnotatedBasicAuthServlet.war",
                                          "JavaEEsecFormAuth.war", "JavaEEsecFormAuthRedirect.war" };
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";

    protected DefaultHttpClient httpclient;
    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
    private static ServerConfiguration emptyConfiguration = null;

    private static final String BASE_DN = "o=ibm,c=us";
    private static final String USER = "jaspildapuser1";
    private static final String USER_DN = "uid=" + USER + "," + BASE_DN;
    private static final String PASSWORD = "s3cur1ty";

    public FormHttpAuthenticationMechanismTestSingleISTest() {
        super(myServer, logClass);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for FORM login with JASPI activated.
     * <LI> Login with a valid userId and password in the javaeesec_form role and verify that
     * <LI> JASPI authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Messages.log contains lines to show that JASPI authentication was processed on form display:
     * <LI> ---JASPI validateRequest called with auth provider=<provider_name>
     * <LI> ---JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Messages.log contains line to show validateRequest called submitting the form with valid user and password
     * <LI> ---JASPI validateRequest called with auth provider=<provider_name>
     * <LI> NOTE: Product design does not allow for secureResponse to be called here so it is not checked.
     * <LI> Servlet is accessed and it prints information about the subject: getAuthType, getUserPrincipal, getRemoteUser.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiFormLoginValidUserInRole_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        // Execute Form login expect a forward to happen.
        myServer.setMarkToEndOfLog();
        executeFormLogin(httpclient, urlBase + Constants.DEFAULT_FORM_LOGIN_PAGE, Constants.javaeesec_basicRoleLDAPUser,
                         Constants.javaeesec_basicRolePwd, false);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for FORM login with JASPI activated.
     * <LI> Login with a valid userId and password in the javaeesec_form role and verify that
     * <LI> JASPI authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Messages.log contains lines to show that JASPI authentication was processed on form display:
     * <LI> ---JASPI validateRequest called with auth provider=<provider_name>
     * <LI> ---JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Messages.log contains line to show validateRequest called submitting the form with valid user and password
     * <LI> ---JASPI validateRequest called with auth provider=<provider_name>
     * <LI> NOTE: Product design does not allow for secureResponse to be called here so it is not checked.
     * <LI> Servlet is accessed and it prints information about the subject: getAuthType, getUserPrincipal, getRemoteUser.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiFormLoginValidUserInRoleRedirect_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        executeFormLogin(httpclient, urlBase + Constants.DEFAULT_REDIRECT_FORM_LOGIN_PAGE, Constants.javaeesec_basicRoleLDAPUser,
                         Constants.javaeesec_basicRolePwd, true);
    }
}

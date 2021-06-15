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

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.CommonTest;
import com.ibm.ws.security.spnego.fat.config.CommonTestHelper;
import com.ibm.ws.security.spnego.fat.config.InitClass;
import com.ibm.ws.security.spnego.fat.config.KdcHelper;
import com.ibm.ws.security.spnego.fat.config.Krb5Helper;
import com.ibm.ws.security.spnego.fat.config.MsKdcHelper;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
@Mode(TestMode.QUARANTINE)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class TrustedActiveDirectoryDomainsTest extends CommonTest {

    private static final Class<?> c = TrustedActiveDirectoryDomainsTest.class;

    private static final String SERVER_NAME = "TrustedActiveDirectoryDomainsTest";
    private final String PROP_KDC_REALM_2 = "security.spnego.realm.2";

    private static final String NORMAL_CONFIG = "spnegoServer.xml";
    private final String CROSS_DOMAIN_CONFIG = "crossDomainSpn.xml";
    private String token = null;
    private String user = null;
    private boolean isUserEmployee;
    private boolean isUserManager;

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setUp";

        commonSetUp(SERVER_NAME, NORMAL_CONFIG, SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS, SPNEGOConstants.DONT_CREATE_SSL_CLIENT,
                    SPNEGOConstants.DONT_CREATE_SPN_AND_KEYTAB, SPNEGOConstants.DEFAULT_REALM, SPNEGOConstants.DONT_CREATE_SPNEGO_TOKEN, SPNEGOConstants.DONT_SET_AS_COMMON_TOKEN,
                    SPNEGOConstants.USE_CANONICAL_NAME, SPNEGOConstants.DONT_USE_COMMON_KEYTAB, SPNEGOConstants.DONT_START_SERVER);

    }

    @Override
    public void preTestCheck() {
        // Override the common pre-test check to avoid unknown caching issue during Kerberos login. Kerberos attempts to use a
        // principal in realm 2 despite the TGT being in realm 1 after testServerKeytabFromRealm1_ClientSendsTokenForRealm2 runs
        // and the common token expires, causing a GSSException.
    }

    @After
    public void afterTest() throws Exception {
        if (myServer.isStarted()) {
            Log.info(c, "afterTest", "Stopping server after test");
            myServer.stopServer(new String[0]);
        }
        myServer = LibertyServerFactory.getLibertyServer(SERVER_NAME, null, true);
        myClient = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, SPNEGOConstants.SIMPLE_SERVLET_NAME, BasicAuthClient.DEFAULT_CONTEXT_ROOT);
        testHelper = new CommonTestHelper(myServer, myClient, null);
    }

    /**
     * Test description:
     * - Use the SPN and keytab file created in realm 1.
     * - A new SPNEGO token created within realm 2 will be used in the protected resource request.
     * - Test makes sure that using a SPNEGO token from a trusted domain is valid and permissible.
     *
     * Expected results:
     * - Access to the protected resource should be granted based on the token from the trusted realm.
     * - GSS credentials for the appropriate user should be present in the resource response.
     */
    //TODO similar problem with testServerKeytabFromRealm2_ClientSendsTokenForRealm1()
//    @Test
    public void testServerKeytabFromRealm1_ClientSendsTokenForRealm2() {
        // KdcHelper for second realm is needed in order to create the matching user on the KDC in the second realm
        KdcHelper testKdcHelper = new MsKdcHelper(myServer, InitClass.KDC2_HOSTNAME, InitClass.KDC2_USER, InitClass.KDC2_USER_PWD, InitClass.KDC2_REALM);
        try {
            Log.info(c, name.getMethodName(), "Keytab will be from realm: " + InitClass.KDC_REALM + "\tToken will be for realm: " + InitClass.KDC2_REALM);

            Log.info(c, name.getMethodName(), "Copying common keytab file into " + SPNEGOConstants.KRB_RESOURCE_LOCATION);
            // Liberty infrastructure already adds leading and trailing '/' characters when copying
            String sanitizedKrbResourcePath = SPNEGOConstants.KRB_RESOURCE_LOCATION.substring(1, SPNEGOConstants.KRB_RESOURCE_LOCATION.length() - 1);
            myServer.copyFileToLibertyServerRoot(sanitizedKrbResourcePath, FATSuite.KEYTAB_FILE_LOCATION);

            // The short host name is used as the SPN in order to avoid Kerberos creating a TGT for a "closer" realm based on the SPN
            // provided. Since the canonical name is used as the SPN in realm 1, using the canonical name in the SPNEGO token for realm 2
            // can confuse the KDC; the TGS in realm 1 might create a TGT for realm 1 instead of realm 2 since realm 1 is "closer."
            // See Kerberos RFC spec, section 3.3.1. Generation of KRB_TGS_REQ Message (http://www.ietf.org/rfc/rfc4120.txt)
            Log.info(c, name.getMethodName(), "Creating the user and SPN on the second KDC machine");
            testKdcHelper.createUserAndSpn(testKdcHelper.getDefaultUserName(), null, InitClass.serverShortHostName);

            setAppropriateToken(false);

            testHelper.reconfigureServer(NORMAL_CONFIG, name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + token, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            successfulSpnegoServletCall(headers, user, isUserEmployee, isUserManager);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        } finally {
            try {
                Log.info(c, name.getMethodName(), "Deleting the matching user on the KDC in the second realm");
                testKdcHelper.deleteUser(testKdcHelper.getDefaultUserName());
            } catch (Exception e) {
                Log.error(c, name.getMethodName(), e, "Could not delete the user in the second realm.");
                fail("Failed to delete the user in the second realm: " + CommonTest.maskHostnameAndPassword(e.getMessage()));
            }
        }
    }

    /**
     * Test description:
     * - Create an SPN and keytab file on a KDC machine in realm 2.
     * - The common SPNEGO token, created within realm 1, will be used in the protected resource request.
     * - Test makes sure that using a SPNEGO token from a trusted domain is valid and permissible.
     *
     * Expected results:
     * - Access to the protected resource should be granted based on the token from the trusted realm.
     * - GSS credentials for the appropriate user should be present in the resource response.
     */
    // TODO: need to figure out why ktpass no longer works on secondary KDC TIVLAB2.TIVLAB1.AUSTIN.IBM.COM - it cannot resolve domain
    // //@Test
    public void testServerKeytabFromRealm2_ClientSendsTokenForRealm1() {
        String testName = "testServerKeytabFromRealm2_ClientSendsTokenForRealm1";
        KdcHelper testKdcHelper = new MsKdcHelper(myServer, InitClass.KDC2_HOSTNAME, InitClass.KDC2_USER, InitClass.KDC2_USER_PWD, InitClass.KDC2_REALM);
        try {
            // Add bootstrap property for the second realm
            Map<String, String> bootstrapProps = new HashMap<String, String>();
            bootstrapProps.put(SPNEGOConstants.PROP_TEST_SYSTEM_HOST_NAME, TARGET_SERVER);
            bootstrapProps.put(PROP_KDC_REALM_2, InitClass.KDC2_REALM);
            testHelper.addBootstrapProperties(myServer, bootstrapProps);

            String config = testHelper.buildFullServerConfigPath(myServer, CROSS_DOMAIN_CONFIG);
            testHelper.copyNewServerConfig(config);

            // Create the user, associated SPN, and keytab for realm 2
            Log.info(c, name.getMethodName(), "Creating keytab in realm: " + InitClass.KDC2_REALM + "\tToken will be for realm: " + InitClass.KDC_REALM);
            testKdcHelper.createSpnAndKeytab(SPNEGOConstants.USE_CANONICAL_NAME);

            setAppropriateToken(true);

            startServer();

            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + token, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            successfulSpnegoServletCall(headers, user, isUserEmployee, isUserManager);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        } finally {
            try {
                Log.info(c, name.getMethodName(), "Deleting user on second realm KDC");
                testKdcHelper.deleteUser();
            } catch (Exception e) {
                Log.error(c, name.getMethodName(), e, "Could not delete the user.");
                fail("Failed to delete the user from the KDC in the second realm: " + CommonTest.maskHostnameAndPassword(e.getMessage()));
            }
        }
    }

    /**
     * Sets the variables for the SPNEGO token to use, as well as the user and roles, based on whether the token should
     * be created using realm 1 or realm 2.
     *
     * @param isTokenForRealm1
     * @throws Exception
     */
    private void setAppropriateToken(boolean isTokenForRealm1) throws Exception {
        String thisMethod = "setAppropriateToken";
        if (isTokenForRealm1) {
            Log.info(c, thisMethod, "Setting SPNEGO token params to the common SPNEGO token values");
            token = FATSuite.COMMON_SPNEGO_TOKEN;
            user = FATSuite.COMMON_TOKEN_USER;
            isUserEmployee = FATSuite.COMMON_TOKEN_USER_IS_EMPLOYEE;
            isUserManager = FATSuite.COMMON_TOKEN_USER_IS_MANAGER;
            return;
        }
        String testSystemShortHostName = InitClass.serverShortHostName;
        Log.info(c, thisMethod, "Creating new SPNEGO token targeted for realm: " + InitClass.KDC2_REALM);
        String jaasLoginContextEntry = null;
        if (FATSuite.OTHER_SUPPORT_JDKS) {
            jaasLoginContextEntry = Krb5Helper.SUN_JDK_KRB5_LOGIN_REFRESH_KRB5_CONFIG;
        }

        token = testHelper.createToken(InitClass.FIRST_USER, InitClass.FIRST_USER_PWD, testSystemShortHostName, InitClass.KDC2_REALM, InitClass.KDC2_HOSTNAME, null, krb5Helper,
                                       true,
                                       Krb5Helper.SPNEGO_MECH_OID, jaasLoginContextEntry);
        user = InitClass.FIRST_USER;
        isUserEmployee = SPNEGOConstants.IS_EMPLOYEE;
        isUserManager = SPNEGOConstants.IS_NOT_MANAGER;
    }

    /**
     * Starts the server and waits for the necessary messages saying all features are ready. Expects a server.xml file
     * to exist within the server root.
     *
     * @param serverXml
     * @throws Exception
     */
    private void startServer() throws Exception {
        testHelper.startServer();

        // Wait for feature update to complete
        expectation.serverUpdate(myServer);
    }
}

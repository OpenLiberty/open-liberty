/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt11.fat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.fat.common.utils.MySkipRule;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.utils.CommonMpJwtFat;
import com.ibm.ws.security.mp.jwt11.fat.utils.MpJwtMessageConstants;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
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

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MPJwtJDKTests extends CommonMpJwtFat {

    public static Class<?> thisClass = MPJwtJDKTests.class;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat")
    public static LibertyServer resourceServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    public static class skipIfJava8OrHigher extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            try {
                JavaInfo javaInfo = JavaInfo.forServer(resourceServer);
                int majorVersion = javaInfo.majorVersion();
                if (majorVersion < 8) {
                    Log.info(thisClass, "skipIfJava8OrHigher", "Skip Tests: false,  Major version is: " + majorVersion);
                    return false;
                } else {
                    Log.info(thisClass, "skipIfJava8OrHigher", "Skip Tests: true,   Major version is: " + majorVersion);
                    return true;
                }

            } catch (Exception e) {
                Log.info(thisClass, "skipIfJava8OrHigher", "Exception occurred: " + e.getMessage());
                // if there is some problem assume we should run the tests
                // if there really is a problem with the setup, tests will fail and we can investigate
                // if we skip the test, we may be sweeping problems under the rug
                return false;
            }
        }
    }

    /**
     * Don't restore between tests
     * The server is not running between tests...
     */
    @Override
    public void restoreTestServers() {
        Log.info(thisClass, "restoreTestServersWithCheck", "* Skipping server restore **");
        logTestCaseInServerLogs("** Skipping server restore **");
        try {
            resourceServer.setStarted(false);
        } catch (Exception e) {
            Log.info(thisClass, "restoreTestServers", "Failed trying to reset server stared state: " + e.getMessage());
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartRSServerForTests(resourceServer, "rs_server_jdk.xml");

    }

    @AfterClass
    public static void commonAfterClass() throws Exception {
        serverTracker.stopAllRunningServers();
        Log.info(thisClass, "commonAfterClass", "Ending Class");
    }

    protected static void setUpAndStartRSServerForTests(LibertyServer server, String configFile) throws Exception {

        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, SecurityFatHttpUtils.getServerHostName());
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, SecurityFatHttpUtils.getServerHostIp());
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "rsacert");
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", "");
        serverTracker.addServer(server);
        server.addIgnoredErrors(Arrays.asList(MpJwtMessageConstants.CWWKZ0002E_EXCEPTION_WHILE_STARTING_APP));
        server.setStarted(false);
        Log.info(thisClass, "setUpAndStartRSServerForTests", "Is server started: " + server.isStarted());

    }

    // to run tests locally, we need java 8 in the gradle (and therefore junit client env).  The @MaximumJavaLevel rule will check the clients java version.  We're interested in the
    // servers java version, so, we have our own rule, skipIfJava8OrHigher
    //@MaximumJavaLevel(javaLevel = 7)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJava8OrHigher.class) // skip test if Java 8 or higher
    @AllowedFFDC(value = { "java.lang.UnsupportedClassVersionError" }) // framework checks for the ffdc even if the test is skipped, so, need to use Allowed instead of Expected
    @Test
    public void MPJwtEarlyJavaVersionsTests_IncludeJWTToken_BasicApp() throws Exception {

        MPJwtEarlyJavaVersionsTests_mainPath_ContextNull_test(MpJwtFatConstants.MPJWT_APP_SEC_CONTEXT_REQUEST_SCOPE,
                                                              createAppClassList("com.ibm.ws.jaxrs.fat.microProfileApp.SecurityContext.ApplicationScoped.MicroProfileApp",
                                                                                 "com.ibm.ws.jaxrs.fat.microProfileApp.SecurityContext.NotScoped.MicroProfileApp",
                                                                                 "com.ibm.ws.jaxrs.fat.microProfileApp.SecurityContext.RequestScoped.MicroProfileApp",
                                                                                 "com.ibm.ws.jaxrs.fat.microProfileApp.SecurityContext.SessionScoped.MicroProfileApp",
                                                                                 "com.ibm.ws.jaxrs.fat.microProfileApp.SecurityContextMicroProfileApp"));

    }

    // to run tests locally, we need java 8 in the gradle (and therefore junit client env).  The @MaximumJavaLevel rule will check the clients java version.  We're interested in the
    // servers java version, so, we have our own rule, skipIfJava8OrHigher
    //@MaximumJavaLevel(javaLevel = 7)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJava8OrHigher.class) // skip test if Java 8 or higher
    @AllowedFFDC(value = { "java.lang.UnsupportedClassVersionError" }) // framework checks for the ffdc even if the test is skipped, so, need to use Allowed instead of Expected
    @Test
    public void MPJwtEarlyJavaVersionsTests_IncludeJWTToken_usingInjection() throws Exception {

        MPJwtEarlyJavaVersionsTests_mainPath_ContextNull_test(MpJwtFatConstants.MPJWT_APP_TOKEN_INJECT_REQUEST_SCOPE,
                                                              createAppClassList("com.ibm.ws.jaxrs.fat.microProfileApp.Injection.ApplicationScoped.MicroProfileApp",
                                                                                 "com.ibm.ws.jaxrs.fat.microProfileApp.Injection.NotScoped.MicroProfileApp",
                                                                                 "com.ibm.ws.jaxrs.fat.microProfileApp.Injection.RequestScoped.MicroProfileApp",
                                                                                 "com.ibm.ws.jaxrs.fat.microProfileApp.Injection.SessionScoped.MicroProfileApp",
                                                                                 "com.ibm.ws.jaxrs.fat.microProfileApp.JsonWebTokenInjectionMicroProfileApp"));
    }

    // to run tests locally, we need java 8 in the gradle (and therefore junit client env).  The @MaximumJavaLevel rule will check the clients java version.  We're interested in the
    // servers java version, so, we have our own rule, skipIfJava8OrHigher
    //@MaximumJavaLevel(javaLevel = 7)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJava8OrHigher.class) // skip test if Java 8 or higher
    @AllowedFFDC(value = { "java.lang.UnsupportedClassVersionError" }) // framework checks for the ffdc even if the test is skipped, so, need to use Allowed instead of Expected
    @Test
    public void MPJwtEarlyJavaVersionsTests_IncludeJWTToken_usingClaimInjection() throws Exception {

        MPJwtEarlyJavaVersionsTests_mainPath_ContextNull_test(MpJwtFatConstants.MPJWT_APP_CLAIM_INJECT_REQUEST_SCOPE,
                                                              createAppClassList("com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjection.ApplicationScoped.Instance.MicroProfileApp",
                                                                                 "com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjection.NotScoped.MicroProfileApp",
                                                                                 "com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjection.RequestScoped.MicroProfileApp",
                                                                                 "com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjection.SessionScoped.Instance.MicroProfileApp",
                                                                                 "com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjectionAllTypesMicroProfileApp",
                                                                                 "com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjectionInstanceMicroProfileApp"));

    }

    public List<String> createAppClassList(String... apps) throws Exception {

        List<String> classList = new ArrayList<String>();
        for (String app : apps) {
            classList.add(app);
        }
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.CommonMicroProfileMarker");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.Utils");
        return classList;

    }

    public void tryToDeployApp(LibertyServer server, List<String> classList) throws Exception {

        ShrinkHelper.exportAppToServer(server, setupUtils.genericCreateArchiveWithJsps(MpJwtFatConstants.MICROPROFILE_SERVLET, classList));
    }

    public void MPJwtEarlyJavaVersionsTests_mainPath_ContextNull_test(String app, List<String> classList) throws Exception {

        try {
            tryToDeployApp(resourceServer, classList);
            resourceServer.startServerUsingExpandedConfiguration("rs_server_jdk.xml", getExpectedMsgs());
        } catch (Exception e) {
            Log.info(thisClass, _testName, "Server start: " + e.getMessage());
        } finally {
            try {
                resourceServer.stopServer((String[]) null);
            } catch (Exception e) {
                Log.info(thisClass, _testName, "Server stop: " + e.getMessage());
            }
        }
    }

    public static List<String> getExpectedMsgs() {
        List<String> msgs = new ArrayList<String>();
        msgs.add(MpJwtMessageConstants.MIN_JDK_FEATURE_REQUIREMENT);
        msgs.add(MpJwtMessageConstants.UNRESOLVED_BUNDLE);
        return msgs;
    }

}

/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.mp.jwt.sharedTests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt11FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwtFatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.utils.CommonMpJwtFat;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MPJwtAppSetupUtils;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MpJwtMessageConstants;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.utils.ServerFileUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;

/**
 * This is a common test class that will test the alternate placement of mp jwt config settings. They can be found in multiple
 * places within the application, or can be set as system or environment variables.
 * The extending test classes will do one of the following:
 * - request use of an app that has placement of MPConfig in "resources/META-INF/microprofile-config.properties"
 * - request use of an app that has placement of MPConfig in "resources/WEB-INF/classes/META-INF/microprofile-config.properties"
 * - request use of a server that has placement of MPConfig in jvm.options
 * - setup MPConfig as environment variables
 *
 **/

@RunWith(FATRunner.class)
public class MPJwtMPConfigTests extends CommonMpJwtFat {

    public static Class<?> thisClass = MPJwtMPConfigTests.class;

    protected final TestValidationUtils validationUtils = new TestValidationUtils();

    public static final String BadKey = "BadKey";
    public static final String KeyMismatch = "KeyMismatch";
    protected final static MPJwtAppSetupUtils baseSetupUtils = new MPJwtAppSetupUtils();
    private static ServerFileUtils fatUtils = new ServerFileUtils();

    public static enum MPConfigLocation {
        IN_APP, SYSTEM_PROP, ENV_VAR
    };

    public class TestApps {

        protected String url = null;
        protected String className = null;

        TestApps(String inUrl, String inClassName) {
            url = inUrl;
            className = inClassName;
        }

        public void setUrl(String inUrl) {
            url = inUrl;
        }

        public String getUrl() {
            return url;
        }

        public void setClassName(String inClassName) {
            url = inClassName;
        }

        public String getClassName() {
            return className;
        }
    }

    @SuppressWarnings("serial")
    List<String> reconfigMsgs = new ArrayList<String>() {
        {
            add("CWWKS5603E");
            add("CWWKZ0002E");
        }
    };

    /******************************************** helper methods **************************************/

    protected static void setupBootstrapPropertiesForMPTests(LibertyServer server, String jwksUri, boolean jwkEnabled) throws Exception {
        bootstrapUtils.writeBootstrapProperty(server, MPJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, SecurityFatHttpUtils.getServerHostName());
        bootstrapUtils.writeBootstrapProperty(server, MPJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, SecurityFatHttpUtils.getServerHostIp());
        if (jwkEnabled) {
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "");
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", jwksUri);
        } else {
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "rsacert");
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", "");
        }

    }

    protected static void startRSServerForMPTests(LibertyServer server, String configFile) throws Exception {
        fatUtils.updateFeatureFiles(server, setActionInstance(RepeatTestFilter.getRepeatActionsAsString()), "mpConfigFeatures", "rsFeatures");

        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration(configFile, commonStartMsgs);
        SecurityFatHttpUtils.saveServerPorts(server, MPJwtFatConstants.BVT_SERVER_1_PORT_NAME_ROOT);
        server.addIgnoredErrors(Arrays.asList(MpJwtMessageConstants.CWWKW1001W_CDI_RESOURCE_SCOPE_MISMATCH, MpJwtMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE));
    }

    /**
     * Gets the resource server up and running.
     * Sets properties in bootstrap.properties that will affect server behavior
     * Sets up and installs the test apps
     * Adds the server to the serverTracker (used for server restore and test class shutdown)
     * Starts the server using the provided configuration file
     * Saves the port info for this server (allows tests with multiple servers to know what ports each server uses)
     * Allow some failure messages that occur during startup (they're ok and doing this prevents the test framework from failing)
     *
     * @param rs_server
     *            - the server to process
     * @param configFile
     *            - the config file to start the server with
     * @param jwkEnabled
     *            - do we want jwk enabled (sets properties in bootstrap.properties that the configs will use)
     * @throws Exception
     */
    protected static void setUpAndStartRSServerForApiTests(LibertyServer rs_server, LibertyServer builderServer, String configFile, boolean jwkEnabled) throws Exception {
        setUpAndStartRSServerForApiTests(rs_server, builderServer, configFile, jwkEnabled, "defaultJWT");
    }

    protected static void setUpAndStartRSServerForApiTests(LibertyServer rs_server, LibertyServer builderServer, String configFile, boolean jwkEnabled, String builderId) throws Exception {
        setupBootstrapPropertiesForMPTests(rs_server, "\"" + SecurityFatHttpUtils.getServerSecureUrlBase(builderServer) + "jwt/ibm/api/" + builderId + "/jwk\"", jwkEnabled);

        bootstrapUtils.writeBootstrapProperty(rs_server, "mpJwt_authHeaderPrefix", MPJwt11FatConstants.TOKEN_TYPE_BEARER + " ");

        fatUtils.updateFeatureFiles(rs_server, setActionInstance(RepeatTestFilter.getRepeatActionsAsString()), "mpConfigFeatures", "rsFeatures");

        baseSetupUtils.deployMicroProfileApp(rs_server);
        serverTracker.addServer(rs_server);
        rs_server.startServerUsingExpandedConfiguration(configFile, commonStartMsgs);
        SecurityFatHttpUtils.saveServerPorts(rs_server, MPJwt11FatConstants.BVT_SERVER_1_PORT_NAME_ROOT);
        rs_server.addIgnoredErrors(Arrays.asList(MpJwtMessageConstants.CWWKW1001W_CDI_RESOURCE_SCOPE_MISMATCH));
    }

    /**
     * Builds the issuer value based on the test machine name and ports. THe issuer is a list of valid hostname:port,
     * hostname:securePort, ipAddr:port, ipAddr:securePort + "/jwt/defaultJWT"
     *
     * @param issuer - for negative issuer tests, we will already have set the issuer to something like "badIssuer", pass in a value
     *            to override the default value being set.
     * @return - return either the passed in override, or the generated valid for your test system value
     * @throws Exception
     */
    public static String buildIssuer(LibertyServer server, String issuer) throws Exception {
        if (issuer == null) {
            return SecurityFatHttpUtils.getServerUrlBase(server) + "jwt/defaultJWT, "
                   + SecurityFatHttpUtils.getServerIpUrlBase(server) + "jwt/defaultJWT, "
                   + SecurityFatHttpUtils.getServerSecureUrlBase(server) + "jwt/defaultJWT, "
                   + SecurityFatHttpUtils.getServerIpSecureUrlBase(server) + "jwt/defaultJWT, "
                   + "https://localhost:" + server.getBvtPort() + "/oidc/endpoint/OidcConfigSample, "
                   + "https://localhost:" + server.getBvtSecurePort() + "/oidc/endpoint/OidcConfigSample";

        } else {
            return issuer;
        }

    }

    /**
     * The jwksuri value in the server.xml contains variables for the server and port. The server can not resolve these
     * variables
     * in the microprofile.properties file, in system properties or in env variables. We need to use the fully resolved string
     * there. This method generates the resolved string.
     *
     * @param rawJwksUri - the raw value if we need to set a value, "" if we don't want to put a value into the mp-config
     *
     * @return - return the fully expanded jwksuri (no $variables in it)
     * @throws Exception
     */
    public static String resolvedJwksUri(LibertyServer server, String rawJwksUri) throws Exception {

        if (rawJwksUri != "" && rawJwksUri.contains("bvt.prop")) {
            return SecurityFatHttpUtils.getServerUrlBase(server) + "jwt/ibm/api/defaultJWT/jwk";

        } else {
            return rawJwksUri;
        }

    }

    /**
     * Initialize the list of test application urls and their associated classNames
     *
     * @throws Exception
     */
    protected List<TestApps> setTestAppArray(LibertyServer server) throws Exception {

        List<TestApps> testApps = new ArrayList<TestApps>();

        testApps.add(new TestApps(buildAppUrl(server, MPJwtFatConstants.MICROPROFILE_SERVLET,
                                              MPJwtFatConstants.MPJWT_APP_SEC_CONTEXT_REQUEST_SCOPE), MPJwtFatConstants.MPJWT_APP_CLASS_SEC_CONTEXT_REQUEST_SCOPE));
        testApps.add(new TestApps(buildAppUrl(server, MPJwtFatConstants.MICROPROFILE_SERVLET,
                                              MPJwtFatConstants.MPJWT_APP_TOKEN_INJECT_REQUEST_SCOPE), MPJwtFatConstants.MPJWT_APP_CLASS_TOKEN_INJECT_REQUEST_SCOPE));
        testApps.add(new TestApps(buildAppUrl(server, MPJwtFatConstants.MICROPROFILE_SERVLET,
                                              MPJwtFatConstants.MPJWT_APP_CLAIM_INJECT_REQUEST_SCOPE), MPJwtFatConstants.MPJWT_APP_CLASS_CLAIM_INJECT_REQUEST_SCOPE));

        return testApps;

    }

    /**
     * Setup expectations for a valid/good path (we get to invoke the app). Expectations set status codes to check and content to
     * validate in the response from the app.
     *
     * @param step - the step to check after
     * @param testUrl - the app that should have benn invoked
     * @param theClass - the class that should be invoked
     * @return - the formatted expectations - for a test with a good result
     * @throws Exception
     */
    public Expectations setGoodAppExpectations(String testUrl, String theClass) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(testUrl));
        expectations.addExpectation(new ResponseFullExpectation(MPJwtFatConstants.STRING_CONTAINS, theClass, "Did not invoke the app " + theClass + "."));

        return expectations;
    }

    /**
     * Set expectations for tests that have bad keyName/publicKey or jwksuri/keyLocations
     *
     * @param server - the server whose log we'll need to check for failure messages
     * @param failureCause - the cause of the failure (failures that occur validating each type of cert x509/jwk)
     * @return - an expectation object with a variety of errors to check for
     * @throws Exception
     */
    public Expectations setBadCertExpectations(LibertyServer server, String failureCause) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messages.log did not contain an error indicating a problem authenticating the request the provided token."));
        switch (failureCause) {
            case MPJwtFatConstants.X509_CERT:
                String invalidKeyName = "badKeyName";
                expectations.addExpectation(new ServerMessageExpectation(server, invalidKeyName
                                                                                 + ".*is not present in the KeyStore as a certificate entry", "Messages.log did not contain a message stating that the alias was NOT found in the keystore."));
                expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6007E_BAD_KEY_ALIAS + ".*"
                                                                                 + invalidKeyName, "Messages.log did not indicate that the signing key is NOT available."));
                expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6033E_JWT_CONSUMER_PUBLIC_KEY_NOT_RETRIEVED + ".*"
                                                                                 + invalidKeyName, "Message log did not indicate that the signing key is NOT available."));
                break;
            case MPJwtFatConstants.JWK_CERT:
                expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6029E_SIGNING_KEY_CANNOT_BE_FOUND, "Messages.log did not contain an error indicating that a signing key could not be found."));
                break;
            case BadKey:
                expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6029E_SIGNING_KEY_CANNOT_BE_FOUND, "Messages.log did not contain an error indicating that a signing key could not be found."));
                break;
            case KeyMismatch:
                expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6028E_SIG_ALG_MISMATCH, "Messages.log did not contain an error indicating that there was a mismatch in the signing keys."));
                break;
            default:
                break;
        }

        return expectations;

    }

}

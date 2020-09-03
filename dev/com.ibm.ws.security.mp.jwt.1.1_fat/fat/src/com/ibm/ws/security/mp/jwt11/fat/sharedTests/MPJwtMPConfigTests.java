/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt11.fat.sharedTests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.utils.CommonMpJwtFat;
import com.ibm.ws.security.mp.jwt11.fat.utils.MP11ConfigSettings;
import com.ibm.ws.security.mp.jwt11.fat.utils.MpJwtMessageConstants;

import componenttest.custom.junit.runner.FATRunner;
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

    public static enum MPConfigLocation {
        IN_APP, SYSTEM_VAR, ENV_VAR
    };

    @SuppressWarnings("serial")
    List<String> reconfigMsgs = new ArrayList<String>() {
        {
            add("CWWKS5603E");
            add("CWWKZ0002E");
        }
    };

    /******************************************** helper methods **************************************/

    protected static void setupBootstrapPropertiesForMPTests(LibertyServer server, MP11ConfigSettings mpConfigSettings) throws Exception {
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, SecurityFatHttpUtils.getServerHostName());
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, SecurityFatHttpUtils.getServerHostIp());
        if (mpConfigSettings.getCertType().equals(MpJwtFatConstants.JWK_CERT)) {
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "");
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", MP11ConfigSettings.jwksUri);
        } else {
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "rsacert");
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", "");
        }

    }

    protected static void startRSServerForMPTests(LibertyServer server, String configFile) throws Exception {
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration(configFile, commonStartMsgs);
        SecurityFatHttpUtils.saveServerPorts(server, MpJwtFatConstants.BVT_SERVER_1_PORT_NAME_ROOT);
        server.addIgnoredErrors(Arrays.asList(MpJwtMessageConstants.CWWKW1001W_CDI_RESOURCE_SCOPE_MISMATCH, MpJwtMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE));
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
        expectations.addExpectation(new ResponseFullExpectation(MpJwtFatConstants.STRING_CONTAINS, theClass, "Did not invoke the app " + theClass + "."));

        return expectations;
    }

}

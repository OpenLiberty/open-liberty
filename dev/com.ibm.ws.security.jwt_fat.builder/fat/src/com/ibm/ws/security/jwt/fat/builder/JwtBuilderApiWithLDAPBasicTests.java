/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.jwt.ClaimConstants;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.JwtMessageConstants;
import com.ibm.ws.security.fat.common.servers.ServerBootstrapUtils;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.buider.actions.JwtBuilderActions;
import com.ibm.ws.security.jwt.fat.builder.utils.BuilderHelpers;
import com.unboundid.ldap.sdk.Entry;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests that use the Consumer API when extending the ConsumeMangledJWTTests.
 * The server will be configured with the appropriate jwtConsumer's
 * We will validate that we can <use> (and the output is correct):
 * 1) create a JWTConsumer
 * 2) create a JwtToken object
 * 3) create a claims object
 * 4) use all of the get methods on the claims object
 * 5) use toJsonString method got get all attributes in the payload
 *
 */

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwtBuilderApiWithLDAPBasicTests extends CommonSecurityFat {

    @Server("com.ibm.ws.security.jwt_fat.builder")
    public static LibertyServer builderServer;

    private static final JwtBuilderActions actions = new JwtBuilderActions();
    public static final TestValidationUtils validationUtils = new TestValidationUtils();
    protected static ServerBootstrapUtils bootstrapUtils = new ServerBootstrapUtils();

    private static InMemoryLDAPServer ds;
    private static final String BASE_DN = "o=ibm,c=us";
    private static final String USER = "foo";
    private static final String USER_DN = "uid=" + USER + "," + BASE_DN;

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(thisClass, "setup", "In class specific setup");
        //        LDAPUtils.addLDAPVariables(builderServer);
        setupLdapServer();
        int ldapPort = ds.getListenPort();
        bootstrapUtils.writeBootstrapProperty(builderServer, "MY_LDAP_PORT", Integer.toString(ldapPort));
        serverTracker.addServer(builderServer);
        builderServer.startServerUsingExpandedConfiguration("server_LDAPRegistry.xml");
        SecurityFatHttpUtils.saveServerPorts(builderServer, JWTBuilderConstants.BVT_SERVER_1_PORT_NAME_ROOT);
        //        updateConfigDynamically(builderServer);

    }

    @AfterClass
    public static void commonAfterClass() throws Exception {
        Log.info(thisClass, "commonAfterClass", " from JwtBuilderAPILDAP");
        if (ds != null) {
            try {
                ds.shutDown(true);
            } catch (Exception e) {
                Log.error(thisClass, "teardown", e, "LDAP server threw error while shutting down. " + e.getMessage());
            }
        }
        CommonSecurityFat.commonAfterClass();
    }

    private static void setupLdapServer() throws Exception {
        ds = new InMemoryLDAPServer(BASE_DN);

        /*
         * Add the partition entries.
         */
        Entry entry = new Entry(BASE_DN);
        entry.addAttribute("objectclass", "organization");
        entry.addAttribute("o", "ibm");
        ds.add(entry);

        /*
         * Create the user.
         */
        entry = new Entry(USER_DN);
        entry.addAttribute("objectclass", "wiminetorgperson");
        entry.addAttribute("uid", USER);
        entry.addAttribute("sn", USER);
        entry.addAttribute("cn", USER);
        entry.addAttribute("homeStreet", "Burnet");
        entry.addAttribute("nickName", USER + " nick name");
        entry.addAttribute("userPassword", "testuserpwd");
        ds.add(entry);
    }

    //    /**
    //     * id="ldap"
    //     * realm="SampleLdapIDSRealm"
    //     * host="${ldap.server.4.name}"
    //     * port="${ldap.server.4.port}"
    //     * ignoreCase="true"
    //     * baseDN="o=ibm,c=us"
    //     * bindDN="cn=root"
    //     * bindPassword="rootpwd"
    //     * ldapType="IBM Tivoli Directory Server"
    //     * idsFilters="ibm_dir_server"
    //     * searchTimeout="8m"
    //     *
    //     * @param serverConfig
    //     * @return
    //     */
    //    private static LdapRegistry createLdapRegistry(ServerConfiguration serverConfig) {
    //        /*
    //         * Create and add the new LDAP registry to the server configuration.
    //         */
    //        LdapRegistry ldapRegistry = new LdapRegistry();
    //        serverConfig.getLdapRegistries().add(ldapRegistry);
    //
    //        /*
    //         * Configure the LDAP registry.
    //         */
    //        ldapRegistry.setBaseDN(BASE_DN);
    //        ldapRegistry.setLdapType("Custom");
    //        ldapRegistry.setRealm("LdapRealm");
    //        ldapRegistry.setHost("localhost");
    //        ldapRegistry.setPort(String.valueOf(ds.getListenPort()));
    //        ldapRegistry.setBindDN(ds.getBindDN());
    //        ldapRegistry.setBindPassword(ds.getBindPassword());
    //        ldapRegistry.setCustomFilters(new LdapFilters("(&(uid=%v)(objectclass=wiminetorgperson))", "(&(cn=%v)(objectclass=groupofnames))", null, null, null));
    //
    //        return ldapRegistry;
    //
    //    }
    //
    //    public static void updateConfigDynamically(LibertyServer server) throws Exception {
    //        ServerConfiguration config = server.getServerConfiguration();
    //        LdapRegistry ldap = createLdapRegistry(config);
    //        //        ldap.setLdapCache(new LdapCache(new AttributesCache(true, 4444, 2222, "5s"), new SearchResultsCache(true, 5555, 3333, "2s")));
    //        server.setMarkToEndOfLog(server.getDefaultLogFile());
    //        server.updateServerConfiguration(config);
    //        server.waitForStringInLogUsingMark("CWWKG001[7-8]I");
    //        //        if (waitForAppToStart) {
    //        //            server.waitForStringInLogUsingMark("CWWKZ0003I"); //CWWKZ0003I: The application userRegistry updated in 0.020 seconds.
    //        //        }
    //    }

    /**************************************************************
     * Test Builder create specific Tests
     **************************************************************/

    /**
     * <p>
     * Invoke the JWT Builder and run fetch for 3 different claims, "uid, sn, cn". Config is using LDAP, so we should be able to
     * retrieve all of these claims defined for the user specified.
     * <p>
     * Make sure that we create a valid JWT Token and that this token contains these 3 claims.
     *
     * @throws Exception
     */
    //    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIWithLDAPBasicTests_runFetch_subjectSet_existingClaims() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, USER);
        JSONArray claimsToFetch = new JSONArray();
        claimsToFetch.add("uid");
        claimsToFetch.add("sn");
        claimsToFetch.add("cn");
        claimsToFetch.add("homeStreet");
        testSettings.put(JwtConstants.JWT_BUILDER_FETCH_API, claimsToFetch);

        configSettings.put("overrideSettings", testSettings);

        // for validation purposes only, create a map of what we expect to find for the values returned from the registry
        JSONObject fetchSettings = new JSONObject();
        fetchSettings.put("uid", USER);
        fetchSettings.put("sn", USER);
        fetchSettings.put("cn", USER);
        fetchSettings.put("homeStreet", "Burnet");
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, fetchSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Invoke the JWT Builder and run fetch for with a null value.
     * <p>
     * Make sure that we get an invalid claim exception. Allow test client to continue to generate a JWT Token. Make sure that the
     * token has not been corrupted by the failed fetch request.
     *
     * @throws Exception
     */
    @Test
    public void JwtBuilderAPIWithLDAPBasicTests_runFetch_subjectSet_nullClaims() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, USER);
        JSONArray claimsToFetch = new JSONArray();
        claimsToFetch.add(null);
        testSettings.put(JwtConstants.JWT_BUILDER_FETCH_API, claimsToFetch);

        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Invoke the JWT Builder and run fetch for with an empty ("") value.
     * <p>
     * Make sure that we get an invalid claim exception. Allow test client to continue to generate a JWT Token. Make sure that the
     * token has not been corrupted by the failed fetch request.
     *
     * @throws Exception
     */
    @Test
    public void JwtBuilderAPIWithLDAPBasicTests_runFetch_subjectSet_emptyClaim() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, USER);
        JSONArray claimsToFetch = new JSONArray();
        claimsToFetch.add("");
        testSettings.put(JwtConstants.JWT_BUILDER_FETCH_API, claimsToFetch);

        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Invoke the JWT Builder and run fetch for a claim that doesn't exist. Config is using LDAP, we should NOT find the claim
     * specified.
     * <p>
     * Make sure that we create a valid JWT Token and that this token does not contain the undefined claim. Also, make sure that
     * no exceptions are raised because a claim can not be found.
     *
     * @throws Exception
     */
    @Test
    public void JwtBuilderAPIWithLDAPBasicTests_runFetch_subjectSet_nonExistingClaim() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, USER);
        JSONArray claimsToFetch = new JSONArray();
        claimsToFetch.add("doesntExistClaim");
        testSettings.put(JwtConstants.JWT_BUILDER_FETCH_API, claimsToFetch);

        configSettings.put("overrideSettings", testSettings);

        JSONObject fetchSettings = new JSONObject();
        fetchSettings.put("doesntExistClaim", null);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, fetchSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Invoke the JWT Builder, do NOT set the subject and run fetch for claims that do exist. Config is using LDAP, we should NOT
     * find the claim specified.
     * <p>
     * Make sure that we create a valid JWT Token and that this token does not contain the requested claims. Also, make sure that
     * no exceptions are raised because a claim can not be found.
     *
     * @throws Exception
     */
    @Test
    public void JwtBuilderAPIWithLDAPBasicTests_runFetch_subjectNotSet_existingClaims() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONArray claimsToFetch = new JSONArray();
        claimsToFetch.add("uid");
        claimsToFetch.add("sn");
        claimsToFetch.add("cn");
        testSettings.put(JwtConstants.JWT_BUILDER_FETCH_API, claimsToFetch);

        configSettings.put("overrideSettings", testSettings);

        // for validation purposes only, create a map of what we expect to find for the values returned from the registry
        JSONObject fetchSettings = new JSONObject();
        fetchSettings.put("uid", null); // shouldn't have uid
        fetchSettings.put("sn", null); // shouldn't have sn
        fetchSettings.put("cn", null); // shouldn't have cn
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, fetchSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }
}

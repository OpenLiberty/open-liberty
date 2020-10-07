/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.consumer;

import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseMessageExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.JwtMessageConstants;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;
import com.ibm.ws.security.fat.common.jwt.expectations.JwtApiExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.consumer.actions.JwtConsumerActions;
import com.ibm.ws.security.jwt.fat.consumer.utils.ConsumerHelpers;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

/**
 * These tests validate that the behavior is as expected based on the configuration used.
 * ie: audience in token is not one that is specfied in the config - we should fail
 * ie: signature algorithm in the config is RS256, but the token uses HS256 - we should fail
 */

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwtConsumerApiConfigTests extends CommonSecurityFat {

    @Server("com.ibm.ws.security.jwt_fat.consumer")
    public static LibertyServer consumerServer;

    public static final TestValidationUtils validationUtils = new TestValidationUtils();
    public static final ConsumerHelpers consumerHelpers = new ConsumerHelpers();
    private static final JwtConsumerActions actions = new JwtConsumerActions();
    private final String currentAction = null;

    private static final String ps256JwtToken = "";
    private static final String ps384JwtToken = "";
    private static final String ps512JwtToken = "";

    protected JWTTokenBuilder builder = null;

    @BeforeClass
    public static void setUp() throws Exception {

        serverTracker.addServer(consumerServer);
        consumerServer.addInstalledAppForValidation(JwtConsumerConstants.JWT_CONSUMER_SERVLET);
        consumerServer.startServerUsingExpandedConfiguration("server_configTests.xml");
        SecurityFatHttpUtils.saveServerPorts(consumerServer, JwtConsumerConstants.BVT_SERVER_1_PORT_NAME_ROOT);
        // one of the JWT Consumer configs has an empty SignatureAlg value which results in a CWWKG0032W warning - mark this as "OK"
        consumerServer.addIgnoredErrors(Arrays.asList(JwtMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".+" + "signatureAlgorithm", JwtMessageConstants.CWWKS6055W_BETA_SIGNATURE_ALGORITHM_USED));

        // set the default signing key for this test class (individual test cases can override if needed)
        consumerHelpers.setDefaultKeyFile(consumerServer, "rsa_privateKey.pem");
    }

    @Override
    @Before
    public void commonBeforeTest() {
        super.commonBeforeTest();
        try {
            builder = consumerHelpers.createBuilderWithDefaultClaims();
        } catch (Exception e) {
            Log.info(thisClass, "commonBeforeTest", e.toString());
            e.printStackTrace(System.out);
            // just set the builder to null - this will cause the test cases to blow up
            builder = null;
        }

    }

    /*
     * Wrap the call to the builder so that we can log the raw values and the generated token
     * for debug purposes and not have to duplicate 3 simple lines of code
     */
    public String buildToken() throws Exception {
        return consumerHelpers.buildToken(builder, _testName);
    }

    public Expectations addGoodConsumerClientResponseAndClaimsExpectations() throws Exception {
        return consumerHelpers.addGoodConsumerClientResponseAndClaimsExpectations(currentAction, builder, consumerServer);
    }

    public Expectations badConsumerExpectations(String errorMessage) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ResponseMessageExpectation(JwtConsumerConstants.STRING_CONTAINS, errorMessage, "Did not find the error message: " + errorMessage));

        return expectations;
    }

    /**************************************************************
     * Tests
     **************************************************************/
    /**
     *
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_defaultJwtConsumerId_noConsumerIdPassedInRequest() throws Exception {

        builder.setIssuer("client03");
        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, null, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a configId of "defaultJwtConsumer". The request to the consumer passes a blank ("") id.
     * We expect a successful outcome as the runtime should substiture "defaultJwtConsumre" for the blank ("") id passed.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_defaultJwtConsumerId_blankConsumerIdPassedInRequest() throws Exception {

        builder.setIssuer("client03");
        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a blank ("") configId. The request to the consumer passes a blank ("") id.
     * We expect a failure as the blank ("") configId passed will be substituted with the default "defaultJwtConfig".
     * This built in config does NOT have a trusted issuer, so, the test will fail with a trusted issuer not found error.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_blankId_blankConsumerIdPassedInRequest() throws Exception {

        consumerServer.reconfigureServerUsingExpandedConfiguration(_testName, "server_configTests2.xml");

        builder.setIssuer("testIssuer2");
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6052E_JWT_TRUSTED_ISSUERS_NULL + ".+\\[testIssuer2\\]", currentAction, consumerServer, JwtConsumerConstants.JWT_CONSUMER_DEFAULT_CONFIG);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * The consumer really does require some config information. The question is with nothing in the config, what will the failure
     * be?
     * It'll just be the first thing checked. That happens to be the trusted issuer. It doesn't matter what issuer we put in the
     * config, it won't be trusted because it won't match the trustedIssuer list which contains ""
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_emptyConfig() throws Exception {

        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6052E_JWT_TRUSTED_ISSUERS_NULL + ".+\\[" + builder.getRawClaims().getIssuer() + "\\]", currentAction, consumerServer, "emptyConfig");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "emptyConfig", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Specify a config id that does NOT exist. Make sure that we log an error message that is useful to the caller.
     * The message needs to explain what failed and why. The test will check what gets logged in the server side logs as well as
     * what is returned in the web response.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_idDoesntExist() throws Exception {

        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildConsumerClientAppExpectations(currentAction, consumerServer);
        expectations.addExpectation(new JwtApiExpectation(JwtConsumerConstants.STRING_MATCHES, JwtMessageConstants.CWWKS6030E_JWT_CONSUMER_ID_DOESNT_EXIST + ".*doesntExist", "Response did not show the expected " + JwtMessageConstants.CWWKS6030E_JWT_CONSUMER_ID_DOESNT_EXIST + " failure."));

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "doesntExist", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Test the various values that issuer can have in the server.xml
     * Other runtime tests validate matches and mis-matches between the configured
     * issuer an the value in the token.
     * These tests focus on blank "" and omitted issuer
     **/

    /**
     * server.xml has a config that has a blank issuer (""). The token will contain "some" issuer.
     * We expect a failure indicating that the issuer is not good.
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_issuerBlank_issuerInToken() throws Exception {

        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6052E_JWT_TRUSTED_ISSUERS_NULL + ".+\\[" + builder.getRawClaims().getIssuer() + "\\]", currentAction, consumerServer, "blankIssuer");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "blankIssuer", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a blank issuer (""). The token will NOT contain an issuer.
     * We expect a failure indicating that the issuer is not good.
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_issuerBlank_issuerNotInToken() throws Exception {

        // remove the issuer from the token
        builder.unsetClaim(PayloadConstants.ISSUER);
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6052E_JWT_TRUSTED_ISSUERS_NULL + ".+\\[" + builder.getRawClaims().getIssuer() + "\\]", currentAction, consumerServer, "blankIssuer");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "blankIssuer", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has omitted the issuer. The token will contain "some" issuer.
     * We expect a failure indicating that the issuer is not good.
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_issuerOmitted_issuerInToken() throws Exception {

        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6052E_JWT_TRUSTED_ISSUERS_NULL + ".+\\[" + builder.getRawClaims().getIssuer() + "\\]", currentAction, consumerServer, "omittedIssuer");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "omittedIssuer", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has omitted the issuer. The token will NOT contain an issuer.
     * We expect a failure indicating that the issuer is not good.
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_issuerOmitted_issuerNotInToken() throws Exception {

        // remove the issuer from the token
        builder.unsetClaim(PayloadConstants.ISSUER);
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6052E_JWT_TRUSTED_ISSUERS_NULL + ".+\\[" + builder.getRawClaims().getIssuer() + "\\]", currentAction, consumerServer, "omittedIssuer");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "omittedIssuer", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Test various combinations of blank and omitted sharedKey in the consumer config.
     * Other tests in this class and in the basic runtime test class will cover testing matching and mis-matching sharedKey.
     */

    /**
     * server.xml has a config that has a blank sharedKey (""). The token will be generated using a "shared" key.
     * We expect a failure indicating that the sharedKey could not be retrieved.
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.KeyException" })
    @Test
    public void JwtConsumerApiConfigTests_sharedKeyBlank_SigAlgHS256() throws Exception {

        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6032E_JWT_CONSUMER_SHARED_KEY_NOT_RETRIEVED + ".+" + JwtMessageConstants.CWWKS6034E_JWT_CONSUMER_SHARED_KEY_NOT_FOUND, currentAction, consumerServer, "sharedKeyBlank_HS256");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "sharedKeyBlank_HS256", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a blank sharedKey (""). The token will be generated using RS256
     * We expect a successful outcome as the sharedKey is NOT needed. (the consumer config is using trust that is appropriate for
     * the token)
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_sharedKeyBlank_SigAlgRS256() throws Exception {

        consumerHelpers.updateBuilderWithRSASettings(builder);
        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "sharedKeyBlank_RS256", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of RS256 - this test ensures that
     * after building a jwt using the RS256 private key, we can use the consumer with the matching algorithm
     * and trust alias (for the public key) to "consume" it.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgRS256() throws Exception {

        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_RS256", null);

        Expectations expectations = consumerHelpers.addGoodConsumerAlgExpectations(currentAction, consumerServer, JwtConsumerConstants.SIGALG_RS256);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, JwtConsumerConstants.SIGALG_RS256, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of RS256, but a trustAlias referring to an RS384 public key.
     * This test ensures that after building a jwt using the RS256 private key, we can NOT use a consumer with the same sigAlg,
     * but a public key of a different type.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void JwtConsumerApiConfigTests_SigAlgRS256_trustAliasMisMatchRS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_RS256 + "_trustedAliasRS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_RS256", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6041E_JWT_SIGNATURE_INVALID, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of RS256, but a trustAlias referring to an ES256 public key.
     * This test ensures that after building a jwt using the RS256 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgRS256_trustAliasMisMatchES() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_RS256 + "_trustedAliasES";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_RS256", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of RS256, but a trustAlias referring to a PS256 public key.
     * This test ensures that after building a jwt using the RS256 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO - enable when PS is added @Test
    public void JwtConsumerApiConfigTests_SigAlgRS256_trustAliasMisMatchPS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_RS256 + "_trustedAliasPS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_RS256", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of RS384 - this test ensures that
     * after building a jwt using the RS384 private key, we can use the consumer with the matching algorithm
     * and trust alias (for the public key) to "consume" it.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgRS384() throws Exception {

        consumerHelpers.updateBuilderWithRSASettings(builder, JwtConsumerConstants.SIGALG_RS384, consumerHelpers.getKeyFileWithPathForServer(consumerServer, "RS384private-key.pem"));

        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_RS384", null);

        Expectations expectations = consumerHelpers.addGoodConsumerAlgExpectations(currentAction, consumerServer, JwtConsumerConstants.SIGALG_RS384);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, JwtConsumerConstants.SIGALG_RS384, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of RS384, but a trustAlias referring to an RS512 public key.
     * This test ensures that after building a jwt using the RS384 private key, we can NOT use a consumer with the same sigAlg,
     * but a public key of a different type.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void JwtConsumerApiConfigTests_SigAlgRS384_trustAliasMisMatchRS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_RS384 + "_trustedAliasRS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_RS384", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6041E_JWT_SIGNATURE_INVALID, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of RS384, but a trustAlias referring to an ES384 public key.
     * This test ensures that after building a jwt using the RS384 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgRS384_trustAliasMisMatchES() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_RS384 + "_trustedAliasES";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_RS384", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of RS384, but a trustAlias referring to a PS384 public key.
     * This test ensures that after building a jwt using the RS384 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO - enable when PS is added @Test
    public void JwtConsumerApiConfigTests_SigAlgRS384_trustAliasMisMatchPS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_RS384 + "_trustedAliasPS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_RS384", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of RS512 - this test ensures that
     * after building a jwt using the RS512 private key, we can use the consumer with the matching algorithm
     * and trust alias (for the public key) to "consume" it.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgRS512() throws Exception {

        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_RS512", null);

        Expectations expectations = consumerHelpers.addGoodConsumerAlgExpectations(currentAction, consumerServer, JwtConsumerConstants.SIGALG_RS512);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, JwtConsumerConstants.SIGALG_RS512, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of RS512, but a trustAlias referring to an RS256 public key.
     * This test ensures that after building a jwt using the RS512 private key, we can NOT use a consumer with the same sigAlg,
     * but a public key of a different type.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void JwtConsumerApiConfigTests_SigAlgRS512_trustAliasMisMatchRS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_RS512 + "_trustedAliasRS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_RS512", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6041E_JWT_SIGNATURE_INVALID, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of RS512, but a trustAlias referring to an ES512 public key.
     * This test ensures that after building a jwt using the RS512 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgRS512_trustAliasMisMatchES() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_RS512 + "_trustedAliasES";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_RS512", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of RS512, but a trustAlias referring to a PS512 public key.
     * This test ensures that after building a jwt using the RS512 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO - enable when PS is added @Test
    public void JwtConsumerApiConfigTests_SigAlgRS512_trustAliasMisMatchPS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_RS512 + "_trustedAliasPS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_RS512", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of ES256 - this test ensures that
     * after building a jwt using the ES256 private key, we can use the consumer with the matching algorithm
     * and trust alias (for the public key) to "consume" it.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgES256() throws Exception {

        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_ES256", null);

        Expectations expectations = consumerHelpers.addGoodConsumerAlgExpectations(currentAction, consumerServer, JwtConsumerConstants.SIGALG_ES256);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, JwtConsumerConstants.SIGALG_ES256, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of ES256, but a trustAlias referring to an RS256 public key.
     * This test ensures that after building a jwt using the ES256 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgES256_trustAliasMisMatchRS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_ES256 + "_trustedAliasRS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_ES256", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of ES256, but a trustAlias referring to an ES384 public key.
     * This test ensures that after building a jwt using the ES256 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgES256_trustAliasMisMatchES() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_ES256 + "_trustedAliasES";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_ES256", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of ES256, but a trustAlias referring to a PS256 public key.
     * This test ensures that after building a jwt using the ES256 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO - enable when PS is added @Test
    public void JwtConsumerApiConfigTests_SigAlgES256_trustAliasMisMatchPS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_ES256 + "_trustedAliasPS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_ES256", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of ES384 - this test ensures that
     * after building a jwt using the ES384 private key, we can use the consumer with the matching algorithm
     * and trust alias (for the public key) to "consume" it.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgES384() throws Exception {

        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_ES384", null);

        Expectations expectations = consumerHelpers.addGoodConsumerAlgExpectations(currentAction, consumerServer, JwtConsumerConstants.SIGALG_ES384);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, JwtConsumerConstants.SIGALG_ES384, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of ES384, but a trustAlias referring to an RS384 public key.
     * This test ensures that after building a jwt using the ES384 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgES384_trustAliasMisMatchRS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_ES384 + "_trustedAliasRS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_ES384", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of ES384, but a trustAlias referring to an ES512 public key.
     * This test ensures that after building a jwt using the ES384 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgES384_trustAliasMisMatchES() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_ES384 + "_trustedAliasES";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_ES384", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of ES384, but a trustAlias referring to a PS384 public key.
     * This test ensures that after building a jwt using the ES384 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO - enable when PS is added @Test
    public void JwtConsumerApiConfigTests_SigAlgES384_trustAliasMisMatchPS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_ES384 + "_trustedAliasPS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_ES384", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of ES512 - this test ensures that
     * after building a jwt using the ES512 private key, we can use the consumer with the matching algorithm
     * and trust alias (for the public key) to "consume" it.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgES512() throws Exception {

        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_ES512", null);

        Expectations expectations = consumerHelpers.addGoodConsumerAlgExpectations(currentAction, consumerServer, JwtConsumerConstants.SIGALG_ES512);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, JwtConsumerConstants.SIGALG_ES512, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of ES512, but a trustAlias referring to an RS512 public key.
     * This test ensures that after building a jwt using the ES512 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgES512_trustAliasMisMatchRS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_ES512 + "_trustedAliasRS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_ES512", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of ES512, but a trustAlias referring to an ES256 public key.
     * This test ensures that after building a jwt using the ES512 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgES512_trustAliasMisMatchES() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_ES512 + "_trustedAliasES";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_ES512", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of ES512, but a trustAlias referring to a PS512 public key.
     * This test ensures that after building a jwt using the ES512 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO - enable when PS is added @Test
    public void JwtConsumerApiConfigTests_SigAlgES512_trustAliasMisMatchPS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_ES512 + "_trustedAliasPS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_ES512", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of PS256 - this test ensures that
     * after building a jwt using the PS256 private key, we can use the consumer with the matching algorithm
     * and trust alias (for the public key) to "consume" it.
     *
     * @throws Exception
     */
    //TODO  @Test
    public void JwtConsumerApiConfigTests_SigAlgPS256() throws Exception {

        String jwtToken = null;
        Expectations expectations = null;
        if (JavaInfo.JAVA_VERSION >= 11) {
            jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_PS256", null);
            expectations = consumerHelpers.addGoodConsumerAlgExpectations(currentAction, consumerServer, JwtConsumerConstants.SIGALG_PS256);
        } else {
            // set the token to a previously built long lived jwt token - we can't build a token without Java 11, but, we still want to test that we get the proper response/failure messages when we try to use a token build with PS256
            jwtToken = ps256JwtToken;
            expectations = badConsumerExpectations("");
        }

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, JwtConsumerConstants.SIGALG_PS256, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of PS256, but a trustAlias referring to an RS256 public key.
     * This test ensures that after building a jwt using the PS256 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO @Test
    public void JwtConsumerApiConfigTests_SigAlgPS256_trustAliasMisMatchRS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_PS256 + "_trustedAliasRS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_PS256", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of PS256, but a trustAlias referring to an ES256 public key.
     * This test ensures that after building a jwt using the PS256 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO @Test
    public void JwtConsumerApiConfigTests_SigAlgPS256_trustAliasMisMatchES() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_PS256 + "_trustedAliasES";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_PS256", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of PS256, but a trustAlias referring to a PS384 public key.
     * This test ensures that after building a jwt using the PS256 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO @Test
    public void JwtConsumerApiConfigTests_SigAlgPS256_trustAliasMisMatchPS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_PS256 + "_trustedAliasPS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_PS256", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of PS384 - this test ensures that
     * after building a jwt using the PS384 private key, we can use the consumer with the matching algorithm
     * and trust alias (for the public key) to "consume" it.
     *
     * @throws Exception
     */
    // TODO    @Test
    public void JwtConsumerApiConfigTests_SigAlgPS384() throws Exception {

        String jwtToken = null;
        Expectations expectations = null;
        if (JavaInfo.JAVA_VERSION >= 11) {
            jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_PS384", null);
            expectations = consumerHelpers.addGoodConsumerAlgExpectations(currentAction, consumerServer, JwtConsumerConstants.SIGALG_PS384);

        } else {
            // set the token to a previously built long lived jwt token - we can't build a token without Java 11, but, we still want to test that we get the proper response/failure messages when we try to use a token build with PS384
            jwtToken = ps384JwtToken;
            expectations = badConsumerExpectations("");
        }

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, JwtConsumerConstants.SIGALG_PS384, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of PS384, but a trustAlias referring to an RS384 public key.
     * This test ensures that after building a jwt using the PS384 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO @Test
    public void JwtConsumerApiConfigTests_SigAlgPS384_trustAliasMisMatchRS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_PS384 + "_trustedAliasRS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_PS384", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of PS384, but a trustAlias referring to an ES384 public key.
     * This test ensures that after building a jwt using the PS384 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO @Test
    public void JwtConsumerApiConfigTests_SigAlgPS384_trustAliasMisMatchES() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_PS384 + "_trustedAliasES";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_PS384", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of PS384, but a trustAlias referring to a PS512 public key.
     * This test ensures that after building a jwt using the PS384 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO @Test
    public void JwtConsumerApiConfigTests_SigAlgPS384_trustAliasMisMatchPS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_PS384 + "_trustedAliasPS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_PS384", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of PS512 - this test ensures that
     * after building a jwt using the PS512 private key, we can use the consumer with the matching algorithm
     * and trust alias (for the public key) to "consume" it.
     *
     * @throws Exception
     */
    // TODO    @Test
    public void JwtConsumerApiConfigTests_SigAlgPS512() throws Exception {

        String jwtToken = null;
        Expectations expectations = null;
        if (JavaInfo.JAVA_VERSION >= 11) {
            jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_PS512", null);
            expectations = consumerHelpers.addGoodConsumerAlgExpectations(currentAction, consumerServer, JwtConsumerConstants.SIGALG_PS512);
        } else {
            // set the token to a previously built long lived jwt token - we can't build a token without Java 11, but, we still want to test that we get the proper response/failure messages when we try to use a token build with PS512
            jwtToken = ps512JwtToken;
            expectations = badConsumerExpectations("");
        }

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, JwtConsumerConstants.SIGALG_PS512, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of PS512, but a trustAlias referring to an RS512 public key.
     * This test ensures that after building a jwt using the PS512 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO @Test
    public void JwtConsumerApiConfigTests_SigAlgPS512_trustAliasMisMatchRS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_PS512 + "_trustedAliasRS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_PS512", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of PS512, but a trustAlias referring to an ES512 public key.
     * This test ensures that after building a jwt using the PS512 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO @Test
    public void JwtConsumerApiConfigTests_SigAlgPS512_trustAliasMisMatchES() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_PS512 + "_trustedAliasES";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_PS512", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of PS512, but a trustAlias referring to a PS256 public key.
     * This test ensures that after building a jwt using the PS512 private key, we can NOT use a consumer with a mismatch between
     * its sigAlg and public key
     *
     * @throws Exception
     */
    //TODO @Test
    public void JwtConsumerApiConfigTests_SigAlgPS512_trustAliasMisMatchPS() throws Exception {

        String consumerId = JwtConsumerConstants.SIGALG_PS512 + "_trustedAliasPS";
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_PS512", null);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY, currentAction, consumerServer, consumerId);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, consumerId, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of HS256 - this test ensures that
     * after building a jwt using the HS256 private key, we can use the consumer with the matching algorithm
     * and trust alias (for the public key) to "consume" it.
     * the token)
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgHS256() throws Exception {

        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_HS256", null);

        Expectations expectations = consumerHelpers.addGoodConsumerAlgExpectations(currentAction, consumerServer, JwtConsumerConstants.SIGALG_HS256);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, JwtConsumerConstants.SIGALG_HS256, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of HS384 - this test ensures that
     * after building a jwt using the HS384 private key, we can use the consumer with the matching algorithm
     * and trust alias (for the public key) to "consume" it.
     * the token)
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgHS384() throws Exception {

        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_HS384", null);

        Expectations expectations = consumerHelpers.addGoodConsumerAlgExpectations(currentAction, consumerServer, JwtConsumerConstants.SIGALG_HS384);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, JwtConsumerConstants.SIGALG_HS384, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that specifies a signature algorithm of HS512 - this test ensures that
     * after building a jwt using the HS512 private key, we can use the consumer with the matching algorithm
     * and trust alias (for the public key) to "consume" it.
     * the token)
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_SigAlgHS512() throws Exception {

        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, consumerServer, "sigAlg_HS512", null);

        Expectations expectations = consumerHelpers.addGoodConsumerAlgExpectations(currentAction, consumerServer, JwtConsumerConstants.SIGALG_HS512);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, JwtConsumerConstants.SIGALG_HS512, jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has omitted the sharedKey. The token will be generated using a "shared" key.
     * We expect a failure indicating that the sharedKey could not be retrieved.
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.KeyException" })
    @Test
    public void JwtConsumerApiConfigTests_sharedKeyOmitted_SigAlgHS256() throws Exception {

        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6032E_JWT_CONSUMER_SHARED_KEY_NOT_RETRIEVED + ".+" + JwtMessageConstants.CWWKS6034E_JWT_CONSUMER_SHARED_KEY_NOT_FOUND, currentAction, consumerServer, "sharedKeyOmitted_HS256");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "sharedKeyOmitted_HS256", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has omitted the sharedKey.. The token will be generated using RS256
     * We expect a successful outcome as the sharedKey is NOT needed. (the consumer config is using trust that is appropriate for
     * the token)
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_sharedKeyOmitted_SigAlgRS256() throws Exception {

        consumerHelpers.updateBuilderWithRSASettings(builder);
        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "sharedKeyOmitted_RS256", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has xor'ed sharedKey. The token will be generated using HS256
     * We expect a successful outcome as the xor'ed sharedKey is supported. (the consumer config is using trust that is
     * appropriate for the token)
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_sharedKeyXor_SigAlgHS256() throws Exception {

        builder.setAudience(SecurityFatHttpUtils.getServerSecureUrlBase(consumerServer) + JwtConsumerConstants.JWT_CONSUMER_ENDPOINT);
        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "sharedKeyXor_HS256", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a bad xor'ed sharedKey.. The token will be generated using HS256
     * We expect a failure as the xor'ed sharedKey is not correct. (the consumer config is using trust that is
     * appropriate for the token)
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    @Test
    public void JwtConsumerApiConfigTests_sharedKeyBadXor_SigAlgHS256() throws Exception {

        builder.setAudience(SecurityFatHttpUtils.getServerSecureUrlBase(consumerServer) + JwtConsumerConstants.JWT_CONSUMER_ENDPOINT);
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6041E_JWT_SIGNATURE_INVALID, currentAction, consumerServer, "sharedKeyBadXor_HS256");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "sharedKeyBadXor_HS256", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Test various combinations of blank and omitted audiences in the consumer config.
     * Other tests in this class and in the basic runtime test class will cover testing matching and mis-matching audiences.
     */

    /**
     * server.xml has a config that has omitted audiences. The token will be generated with an audience.
     * We expect a failure indicating that the audience is not valid.
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_audienceOmitted_audienceInToken() throws Exception {

        builder.setAudience(SecurityFatHttpUtils.getServerSecureUrlBase(consumerServer) + JwtConsumerConstants.JWT_CONSUMER_ENDPOINT);
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6023E_BAD_AUDIENCE, currentAction, consumerServer, "audienceOmitted");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "audienceOmitted", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has omitted audiences. The token will be generated without an audience.
     * We expect a successful outcome as the audience is not set in either the token, or config.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_audienceOmitted_audienceNotInToken() throws Exception {

        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "audienceOmitted", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a blank audiences (""). The token will be generated with an audience
     * We expect a failure indicating that the audience is not valid.
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_audienceBlank_audienceInToken() throws Exception {

        builder.setAudience(SecurityFatHttpUtils.getServerSecureUrlBase(consumerServer) + JwtConsumerConstants.JWT_CONSUMER_ENDPOINT);
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6023E_BAD_AUDIENCE, currentAction, consumerServer, "audienceBlank");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "audienceBlank", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a blank audiences (""). The token will be generated without an audience.
     * We expect a successful outcome as the audience is not set in either the token, or config.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_audienceBlank_audienceNotInToken() throws Exception {

        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "audienceBlank", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has an audience with multiple values in it. The token has an audience value that is contained
     * in the list of the audience config.
     * We expect a successful outcome as the tokens audience is found in the list of the config.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_audienceMultiple_audienceInToken() throws Exception {

        builder.setAudience(SecurityFatHttpUtils.getServerSecureUrlBase(consumerServer) + JwtConsumerConstants.JWT_CONSUMER_ENDPOINT);
        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "audienceMultiple", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has an audience with multiple values in it. The token has an audience value that is contained
     * in the list of the audience config.
     * We expect a successful outcome as the tokens audience is found in the list of the config.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_audienceMultiple_audienceMultipleInToken() throws Exception {

        builder.setAudience(SecurityFatHttpUtils.getServerSecureUrlBase(consumerServer) + JwtConsumerConstants.JWT_CONSUMER_ENDPOINT, SecurityFatHttpUtils.getServerUrlBase(consumerServer) + JwtConsumerConstants.JWT_CONSUMER_ENDPOINT);
        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "audienceMultiple", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has an audience with multiple values in it. The token has an audience value that is not
     * contained in the list of the audience config.
     * We expect a failure indicating that the audience is not valid.
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_audienceMultiple_audienceNotInToken() throws Exception {

        builder.setAudience(SecurityFatHttpUtils.getServerSecureUrlBase(consumerServer) + "someString");
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6023E_BAD_AUDIENCE, currentAction, consumerServer, "audienceMultiple");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "audienceMultiple", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Test various combinations of blank and omitted signatureAlgorithm in the consumer config.
     * Other tests in this class and in the basic runtime test class will cover testing matching and mis-matching
     * signatureAlgorithm.
     */

    /**
     * server.xml has a config that has omitted signatureAlgorithm. The token will be generated using RS256.
     * We expect a successful outcome as the default signatureAlgorithm is RS256 and the truststore we're using has one cert that
     * what the token was created with.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_sigAlgOmitted_RS256InToken() throws Exception {

        consumerHelpers.updateBuilderWithRSASettings(builder);
        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "sigAlgOmitted", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * server.xml has a config that has omitted signatureAlgorithm. The token will be generated using a sharedKey and HS256.
     * We expect a failure indicating a signature algorithm mis-match.
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_sigAlgOmitted_HS256InToken() throws Exception {

        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6028E_BAD_ALGORITHM + ".+" + JwtConsumerConstants.SIGALG_HS256 + ".+" + JwtConsumerConstants.SIGALG_RS256, currentAction, consumerServer, "sigAlgOmitted");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "sigAlgOmitted", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a blank signatureAlgorithm (""). The token will be generated using RS256.
     * We expect a successful outcome as the default signatureAlgorithm is RS256 and the truststore we're using has one cert that
     * what the token was created with.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_sigAlgBlank_RS256InToken() throws Exception {

        consumerHelpers.updateBuilderWithRSASettings(builder);
        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "sigAlgBlank", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a blank signatureAlgorithm (""). The token will be generated using a sharedKey and HS256.
     * We expect a failure indicating a signature algorithm mis-match.
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_sigAlgBlank_HS256InToken() throws Exception {

        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6028E_BAD_ALGORITHM + ".+" + JwtConsumerConstants.SIGALG_HS256 + ".+" + JwtConsumerConstants.SIGALG_RS256, currentAction, consumerServer, "sigAlgBlank");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "sigAlgBlank", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    // moving JwtConsumerApiConfigTests_trustStoreRefOmitted_withTrustedAlias_globalTrust_oneCert, JwtConsumerApiConfigTests_trustStoreRefOmitted_withoutTrustedAlias_globalTrust_oneCert,
    // JwtConsumerApiConfigTests_trustStoreRefBlank_withTrustedAlias_globalTrust_oneCert, and JwtConsumerApiConfigTests_trustStoreRefBlank_withoutTrustedAlias_globalTrust_oneCert
    // to JWTConsumerApiConfigWithGlobalTrustTests saves about 40% on runtime (2 minutes).  They each required a reconfig when they were in this test class.  In
    // JWTConsumerApiConfigWithGlobalTrustTests, we can start the server and run all tests with the same config...

    /**
     * Test various combinations of blank and omitted trustStoreRef in the consumer config (we do and do not specify trustAlias).
     * Other tests in this class, the basic runtime test class and the global trust test class will cover testing other
     * combinations of trust.
     */

    /**
     * server.xml has a config that has omitted the trustStoreRef, but did specify a trustedAlias. The signatureAlgorithm is RS256
     * and there is NO global trust. The token will be generated using RS256.
     * We expect a failure indicating that we don't have a signing key. (We are NOT pointing to a trustStore)
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_trustStoreRefOmitted_withTrustedAlias_noGlobalTrust() throws Exception {

        consumerHelpers.updateBuilderWithRSASettings(builder);
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY + ".+" + JwtConsumerConstants.SIGALG_RS256, currentAction, consumerServer, "trustStoreRefOmitted_RS256_withTrustedAlias");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "trustStoreRefOmitted_RS256_withTrustedAlias", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has omitted the trustStoreRef, but did NOT specify a trustedAlias. The signatureAlgorithm is
     * RS256 and there is NO global trust. The token will be generated using RS256.
     * We expect a failure indicating that we don't have a signing key. (We are NOT pointing to a trustStore)
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_trustStoreRefOmitted_withoutTrustedAlias_noGlobalTrust() throws Exception {

        consumerHelpers.updateBuilderWithRSASettings(builder);
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY + ".+" + JwtConsumerConstants.SIGALG_RS256, currentAction, consumerServer, "trustStoreRefOmitted_RS256_withoutTrustedAlias");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "trustStoreRefOmitted_RS256_withoutTrustedAlias", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a blank trustStoreRef (""), but did specify a trustedAlias. The signatureAlgorithm is
     * RS256 and there is NO global trust. The token will be generated using RS256.
     * We expect a failure indicating that we don't have a signing key. (We are NOT pointing to a trustStore)
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_trustStoreRefBlank_withTrustedAlias_noGlobalTrust() throws Exception {

        consumerHelpers.updateBuilderWithRSASettings(builder);
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY + ".+" + JwtConsumerConstants.SIGALG_RS256, currentAction, consumerServer, "trustStoreRefBlank_RS256_withTrustedAlias");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "trustStoreRefBlank_RS256_withTrustedAlias", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a blank trustStoreRef (""), but did NOT specify a trustedAlias. The signatureAlgorithm is
     * RS256 and there is NO global trust. The token will be generated using RS256.
     * We expect a failure indicating that we don't have a signing key. (We are NOT pointing to a trustStore)
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_trustStoreRefBlank_withoutTrustedAlias_noGlobalTrust() throws Exception {

        consumerHelpers.updateBuilderWithRSASettings(builder);
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY + ".+" + JwtConsumerConstants.SIGALG_RS256, currentAction, consumerServer, "trustStoreRefBlank_RS256_withoutTrustedAlias");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "trustStoreRefBlank_RS256_withoutTrustedAlias", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a valid trustStoreRef, but did NOT specify a trustedAlias. The trust store contains 1 cert
     * that is valid for the token that will be passed.
     * The signatureAlgorithm is RS256 and there is NO global trust. The token will be generated using RS256.
     * We expect a successful outcome as the only cert in the trust store is the cert we need.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_trustStoreRefValid_withoutTrustedAlias_oneCert() throws Exception {

        consumerHelpers.updateBuilderWithRSASettings(builder);
        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "trustStoreRef_RS256_withoutTrustedAlias_oneCert", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a valid trustStoreRef, but did NOT specify a trustedAlias. The trust store contains
     * multiple certs including one that is valid for the token that will be passed.
     * The signatureAlgorithm is RS256 and there is NO global trust. The token will be generated using RS256.
     * We expect a failure indicating that we could not retrieve the public key - there are multiple and the config did NOT
     * specify which to use.
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException", "com.ibm.websphere.security.jwt.KeyException" })
    @Test
    public void JwtConsumerApiConfigTests_trustStoreRefValid_withoutTrustedAlias_multipleCert() throws Exception {

        consumerHelpers.updateBuilderWithRSASettings(builder);
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6033E_JWT_CONSUMER_PUBLIC_KEY_NOT_RETRIEVED + ".+" + JwtMessageConstants.CWWKS6007E_BAD_KEY_ALIAS + ".+" + JwtConsumerConstants.SIGALG_RS256 + ".+" + JwtMessageConstants.CWWKS6047E_MULTIKEY_NO_ALIAS, currentAction, consumerServer, "trustStoreRef_RS256_withoutTrustedAlias_multipleCert");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "trustStoreRef_RS256_withoutTrustedAlias_multipleCert", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a valid trustStoreRef, and a valid trustedAlias. The trust store contains the cert that is
     * valid for the token that will be passed.
     * The signatureAlgorithm is RS256 and there is NO global trust. The token will be generated using RS256.
     * We expect a successful outcome as the only cert in the trust store is the cert we need.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_trustStoreRefValid_trustedAliasValid() throws Exception {

        consumerHelpers.updateBuilderWithRSASettings(builder);
        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "trustStoreRef_RS256_trustedAliasValid", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a valid trustStoreRef, and an invalid trustedAlias. The trust store contains the cert that
     * is valid for the token that will be passed.
     * The signatureAlgorithm is RS256 and there is NO global trust. The token will be generated using RS256.
     * We expect a failure indicating that we could not retrieve the public key - the alias is not found
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "java.security.cert.CertificateException", "com.ibm.websphere.security.jwt.KeyException" })
    @Test
    public void JwtConsumerApiConfigTests_trustStoreRefValid_trustedAliasInValid() throws Exception {

        consumerHelpers.updateBuilderWithRSASettings(builder);
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6033E_JWT_CONSUMER_PUBLIC_KEY_NOT_RETRIEVED + ".+badAlias.+rsa_trust.+" + JwtMessageConstants.CWWKS6007E_BAD_KEY_ALIAS + ".+" + JwtConsumerConstants.SIGALG_RS256, currentAction, consumerServer, "trustStoreRef_RS256_trustedAliasInValid");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "trustStoreRef_RS256_trustedAliasInValid", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has an invalid trustStoreRef, and trustedAlias. The trust store contains the cert that is
     * valid for the token that will be passed.
     * The signatureAlgorithm is RS256 and there is NO global trust. The token will be generated using RS256.
     * We expect a failure indicating that we could not retrieve the public key - the alias is not found
     * Test will validate the message in the server side logs as well as the content of the web response
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "java.security.KeyStoreException", "com.ibm.websphere.security.jwt.KeyException" })
    @Test
    public void JwtConsumerApiConfigTests_trustStoreRefInValid_trustedAliasValid() throws Exception {

        consumerHelpers.updateBuilderWithRSASettings(builder);
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6033E_JWT_CONSUMER_PUBLIC_KEY_NOT_RETRIEVED + ".+rsacert.+badtrust.+" + JwtMessageConstants.CWWKS6007E_BAD_KEY_ALIAS + ".+" + JwtConsumerConstants.SIGALG_RS256, currentAction, consumerServer, "trustStoreRefInvalid_RS256_trustedAliasValid");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "trustStoreRefInvalid_RS256_trustedAliasValid", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Test various combinations of clock skew.
     */

    /**
     * server.xml has a config that has omitted clockSkew. The token has an expiration that represents a very short lifetime (5
     * seconds). The test sleeps beyond the token lifetime.
     * We expect a successful outcome as the clockSkew is 3 minutes. 3 minutes plus 5 seconds is longer than the time we slept,
     * therefore, the token will not be treated as expired
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_clockSkew_default() throws Exception {

        builder.setExpirationTimeSecondsFromNow(5);
        builder.setAudience(SecurityFatHttpUtils.getServerSecureUrlBase(consumerServer) + JwtConsumerConstants.JWT_CONSUMER_ENDPOINT);
        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        // sleep beyond token lifetime, but not beyond lifetime + clockskew
        Thread.sleep(10 * 1000);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "clockSkew_default", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has clockSkew set to 3 seconds. The token has an expiration that represents a very short
     * lifetime (5 seconds). The test sleeps beyond the token lifetime plus clockSkew.
     * We expect a failure indicating that the token is expired (token lifetime plus clockskew is less than the time we slept)
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_clockSkew_short() throws Exception {

        builder.setExpirationTimeSecondsFromNow(5);
        builder.setAudience(SecurityFatHttpUtils.getServerSecureUrlBase(consumerServer) + JwtConsumerConstants.JWT_CONSUMER_ENDPOINT);
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtMessageConstants.CWWKS6025E_TOKEN_EXPIRED + ".+exp.+clock skew.+3", currentAction, consumerServer, "clockSkew_short");

        // sleep beyond token lifetime + clockskew
        Thread.sleep(15 * 1000);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "clockSkew_short", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has authenticationMethodsReferences set to
     * "OTP iris, pwd kba". For multiple values (array) in server.xml, the
     * provided jwtClaim must be an exact match on one of the elements. We
     * expect a failure indicating that the provided amrClaim is not listed in
     * the trusted ones
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtConsumerApiConfigTests_multiple_invalid_amr() throws Exception {

        consumerServer.reconfigureServerUsingExpandedConfiguration(_testName, "server_jwtConsumerAmr.xml");
        builder.setClaim("amr", new String[] { "iris", "pwd" });
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(
                JwtMessageConstants.CWWKS6054E_INVALID_AMR_CLAIM + ".+" + "multipleJwtConsumer", currentAction,
                consumerServer, "multipleJwtConsumer");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "multipleJwtConsumer", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has authenticationMethodsReferences set to
     * "OTP iris, pwd kba". For multiple values (array) in server.xml, the
     * provided jwtClaim must be an exact match on one of the elements. We
     * expect a successful outcome since the provided values match the exact
     * element of the server.xml
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtConsumerApiConfigTests_multiple_valid_amr() throws Exception {

        consumerServer.reconfigureServerUsingExpandedConfiguration(_testName, "server_jwtConsumerAmr.xml");
        builder.setClaim("amr", new String[] { "pwd", "kba" });
        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "multipleJwtConsumer", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has authenticationMethodsReferences set to
     * "OTP iris". For single value (not array) in server.xml, the provided
     * jwtClaim must include all the required values from server and any other.
     * We expect a failure indicating that the provided amrClaim is not listed
     * since not all of the required values are specified
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtConsumerApiConfigTests_single_invalid_amr() throws Exception {

        consumerServer.reconfigureServerUsingExpandedConfiguration(_testName, "server_jwtConsumerAmr.xml");
        builder.setClaim("amr", new String[] { "OTP" });
        String jwtToken = buildToken();

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(
                JwtMessageConstants.CWWKS6054E_INVALID_AMR_CLAIM + ".+" + "singleJwtConsumer", currentAction,
                consumerServer, "singleJwtConsumer");

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "singleJwtConsumer", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has authenticationMethodsReferences set to
     * "OTP iris". For single value (not array) in server.xml, the provided
     * jwtClaim must include all the required values from server and any other.
     * We expect a successful outcome since the provided values includes the
     * required values
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtConsumerApiConfigTests_single_valid_amr() throws Exception {

        consumerServer.reconfigureServerUsingExpandedConfiguration(_testName, "server_jwtConsumerAmr.xml");
        builder.setClaim("amr", new String[] { "OTP", "iris", "pwd", "kba" });
        String jwtToken = buildToken();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "singleJwtConsumer", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

}

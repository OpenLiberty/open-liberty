/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.jwt.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.jmock.Expectations;
import org.jose4j.base64url.Base64;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.security.jwt.InvalidTokenException;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class JweHelperTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.jwt.*=all:com.ibm.ws.security.common.*=all");

    static final String MSG_UNSUPPORTED_KEY_MANAGEMENT_ALGORITHM = "CWWKS6056E";
    static final String MSG_CTY_NOT_JWT_FOR_NESTED_JWS = "CWWKS6057E";

    private JweHelper helper = null;

    private final JsonWebEncryption jwe = mockery.mock(JsonWebEncryption.class);
    private final JwtConsumerConfig config = mockery.mock(JwtConsumerConfig.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() {
        System.out.println("Entering test: " + testName.getMethodName());
        helper = new JweHelper();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_verifyContentType_ctyNull() {
        final String cty = null;
        mockery.checking(new Expectations() {
            {
                one(jwe).getContentTypeHeaderValue();
                will(returnValue(cty));
            }
        });
        try {
            helper.verifyContentType(jwe);
            fail("Should have thrown an InvalidTokenException but did not.");
        } catch (InvalidTokenException e) {
            verifyException(e, MSG_CTY_NOT_JWT_FOR_NESTED_JWS);
        }
    }

    @Test
    public void test_verifyContentType_ctyEmpty() {
        final String cty = "";
        mockery.checking(new Expectations() {
            {
                one(jwe).getContentTypeHeaderValue();
                will(returnValue(cty));
            }
        });
        try {
            helper.verifyContentType(jwe);
            fail("Should have thrown an InvalidTokenException but did not.");
        } catch (InvalidTokenException e) {
            verifyException(e, MSG_CTY_NOT_JWT_FOR_NESTED_JWS);
        }
    }

    @Test
    public void test_verifyContentType_ctyNotJwt() {
        final String cty = "nope";
        mockery.checking(new Expectations() {
            {
                one(jwe).getContentTypeHeaderValue();
                will(returnValue(cty));
            }
        });
        try {
            helper.verifyContentType(jwe);
            fail("Should have thrown an InvalidTokenException but did not.");
        } catch (InvalidTokenException e) {
            verifyException(e, MSG_CTY_NOT_JWT_FOR_NESTED_JWS);
        }
    }

    @Test
    public void test_verifyContentType_ctyIsJwt() throws InvalidTokenException {
        final String cty = "jwt";
        mockery.checking(new Expectations() {
            {
                one(jwe).getContentTypeHeaderValue();
                will(returnValue(cty));
            }
        });
        helper.verifyContentType(jwe);
    }

    @Test
    public void test_getKidFromJweString_justFourPeriods() {
        final String jwe = "....";
        String result = helper.getKidFromJweString(jwe);
        assertNull("Returned kid value should have been null, but was [" + result + "].", result);
    }

    @Test
    public void test_getKidFromJweString_onePeriod() {
        final String jwe = ".";
        String result = helper.getKidFromJweString(jwe);
        assertNull("Returned kid value should have been null, but was [" + result + "].", result);
    }

    @Test
    public void test_getKidFromJweString_notBase64Encoded() {
        final String jwe = "this should be the header....";
        String result = helper.getKidFromJweString(jwe);
        assertNull("Returned kid value should have been null, but was [" + result + "].", result);
    }

    @Test
    public void test_getKidFromJweString_notBase64EncodedJson() {
        final String jwe = "{\"kid\":\"some_id\"}....";
        String result = helper.getKidFromJweString(jwe);
        assertNull("Returned kid value should have been null, but was [" + result + "].", result);
    }

    @Test
    public void test_getKidFromJweString_notJson() {
        String encoded = Base64.encode("not json".getBytes());
        final String jwe = encoded + "....";
        String result = helper.getKidFromJweString(jwe);
        assertNull("Returned kid value should have been null, but was [" + result + "].", result);
    }

    @Test
    public void test_getKidFromJweString_emptyJson() {
        String encoded = Base64.encode("{}".getBytes());
        final String jwe = encoded + "....";
        String result = helper.getKidFromJweString(jwe);
        assertNull("Returned kid value should have been null, but was [" + result + "].", result);
    }

    @Test
    public void test_getKidFromJweString_missingKidEntry() {
        String encoded = Base64.encode("{\"alg\":\"RS256\"}".getBytes());
        final String jwe = encoded + "....";
        String result = helper.getKidFromJweString(jwe);
        assertNull("Returned kid value should have been null, but was [" + result + "].", result);
    }

    @Test
    public void test_getKidFromJweString_emptyKidEntry() {
        String kid = "";
        String encoded = Base64.encode(("{\"kid\":\"" + kid + "\"}").getBytes());
        final String jwe = encoded + "....";
        String result = helper.getKidFromJweString(jwe);
        assertEquals("Returned kid value did not match expected value.", kid, result);
    }

    @Test
    public void test_getKidFromJweString_nonEmptyKidEntry() {
        String kid = "this is the kid value";
        String encoded = Base64.encode(("{\"kid\":\"" + kid + "\"}").getBytes());
        final String jwe = encoded + "....";
        String result = helper.getKidFromJweString(jwe);
        assertEquals("Returned kid value did not match expected value.", kid, result);
    }

    /**
     * See https://github.com/advisories/GHSA-jgvc-jfgh-rjvv
     */
    @Test
    public void test_extractPayloadFromJweToken_invalidPkcsPadding() throws Exception {
        String jweStringInvalidPkcsPadding = "eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4R0NNIn0.ksmeZ6dBbP0UfDEaLXlqPl2XDaAA29kGlKtDb89x-4xN5-A6bx2umI_ToHK2GadzxUOgKROCACYb6rmKsqsQCOZaBsnq_4mDII1W0pja7Lz4zTnr7R3O4kALg4zXqG-gSlcDA7k1NgkpMDS15PjMmADqyqxbxQsXdfjstN324iqdvYGh6NsckkfTSWxDVAqiSR9fW8PsIbo3uSMokNaC-f64CDWIB9AsCxhF-3mnFbxXNxw7JE0upOgG4enQ8kZkwi_v54HBqAau1YNW7gPhFV8ElTQ71J6aHB3dja23lbWdaJmrK6PJE7gEeZmUbFkSYmuyzRUS-NGfXA23fYv5JQ.46AsIpPgnJCLH0Xm.u2rG.LyEHEGCWM8CXDEEHiaqhiQ";
        JSONObject jwkJson = new JSONObject();
        jwkJson.put("kty", "RSA");
        jwkJson.put("alg", "RSA1_5");
        jwkJson.put("use", "enc");
        jwkJson.put("n", "w2A4cbwOAK4ATnwXkGWereqv9dkEcgAGHc9g-cjo1HFeilYirvfD2Un2vQxW_6g2OKRPmmo46vMZFMYv_V57174j411y-NQlZGb7iFqMQADzo60VZ7vpvAX_NuxNGxYR-N2cBgvgqDiGAoO9ouNdhuHhxipTjGVfrPUpxmJtNPZpxsgxQWSpYCYMl304DD_5wWrnumNNIKOaVsAYmjFPV_wqxFCHbitPd1BG9SwXPk7wAHtXT6rYaUImS_OKaHkTO1OO0PNhd3-wJRNMCh_EGUwAghfWgFyAd20pQLZamamxgHvfL4-0hwuzndhHt0ye-gRVTtXDFEwABB--zwvlCw");
        jwkJson.put("e", "AQAB");
        jwkJson.put("kid", "rsa1_5");
        jwkJson.put("d", "EjMvbuDeyQ9sdeM3arscqgTXuWYq9Netui8sUHh3v_qDnQ1jE7t-4gny0y-IFy67RlGAHNlSTgixSG8h309i5_kNbMuyvx08EntJaS1OLVQpXhDskoo9vscsPBiNIj3PFMjIFQQcPG9vhGJzUu4tMzhtiME-oTB8VidMae-XTryPvozTu4rgfb4U7uauvLqESLz3A5xtzPnwNwqXAIlrdxU-MT_iln08on_QIF8afWUqCbsWWjEck_QDKLVpzh8VV9kkEVWwYfCFhHBwS-fgGJJTE3gK4HwOokydMtH95Dzj47MA2pLe600l7ioyGSPltcv967NtOpxMPM5ro751KQ");
        jwkJson.put("p", "-F1u3NAMWPu1TIuvIywIjh5fuiA3AVKLgS6Fw_hAi3M9c3T7E1zNJZuHgQExJEu06ZPfzye9m7taDzh-Vw4VGDED_MZedsE2jEsWa9EKeq3bZVf5j81FLCHH8BicFqrPjvoVUC35wrl9SGJzaOa7KXxD2jW22umYjJS_kcopvf0");
        jwkJson.put("q", "yWHG7jHqvfqT8gfhIlxpMbeJ02FrWIkgJC-zOJ26wXC6oxPeqhqEO7ulGqZPngNDdSGgWcQ7noGEU8O4MA9V3yhl91TFZy8unox0sGe0jDMwtxm3saXtTsjTE7FBxzcR0PubfyGiS0fJqQcj8oJSWzZPkUshzZ8rF3jTLc8UWac");
        jwkJson.put("dp", "Va9WWhPkzqY4TCo8x_OfF_jeqcYHdAtYWb8FIzD4g6PEZZrMLEft9rWLsDQLEiyUQ6lio4NgZOPkFDA3Vi1jla8DYyfE20-ZVBlrqNK7vMtST8pkLPpyjOEyq2CyKRfQ99DLnZfe_RElad2dV2mS1KMsfZHeffPtT0LaPJ_0erk");
        jwkJson.put("dq", "M8rA1cviun9yg0HBhgvMRiwU91dLu1Zw_L2D02DFgjCS35QhpQ_yyEYHPWZefZ4LQFmoms2cI7TdqolgmoOnKyCBsO2NY29AByjKbgAN8CzOL5kepEKvWJ7PonXpG-ou29eJ81VcHw5Ub_NVLG6V7b13E0AGbpKsC3pYnaRvcGs");
        jwkJson.put("qi", "8zIqISvddJYC93hP0sKkdHuVd-Mes_gsbi8xqSFYGqc-wSU12KjzHnZmBuJl_VTGy9CO9W4K2gejr588a3Ozf9U5hx9qCVkV0_ttxHcTRem5sFPe9z-HkQE5IMW3SdmL1sEcvkzD7z8QhcHRpp5aMptfuwnxBPY8U449_iNgXd4");
        PublicJsonWebKey publicJwk = PublicJsonWebKey.Factory.newPublicJwk(jwkJson.toString());
        final RsaJsonWebKey rsaJwk = (RsaJsonWebKey) publicJwk;
        mockery.checking(new Expectations() {
            {
                one(config).getJweDecryptionKey();
                will(returnValue(rsaJwk.getPrivateKey()));
                one(config).getId();
                will(returnValue(testName.getMethodName()));
            }
        });
        String expectedExceptionString = "org.jose4j.lang.InvalidAlgorithmException";
        String joseException1 = null;
        String joseException2 = null;
        try {
            String payload = JweHelper.extractPayloadFromJweToken(jweStringInvalidPkcsPadding, config, null);
            fail("Should have thrown an exception, but didn't. Got payload: " + payload);
        } catch (InvalidTokenException e) {
            verifyException(e, MSG_UNSUPPORTED_KEY_MANAGEMENT_ALGORITHM + ".*" + expectedExceptionString);
            int indexOfJoseException = e.getMessage().indexOf(expectedExceptionString);
            joseException1 = e.getMessage().substring(indexOfJoseException);
        }
        String jweStringValidPkcsPaddingEncodedKeySizeIncorrect = "eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4R0NNIn0.oyVTmkyoChxFtyCtiKhv8OpBJcV6C6s_gMFSSRJBNStpdHPzq2YmroTfXGj1J1plFG4BBQwIZtdt6rIS6YkCvTLGqP1hds9CAO1a_bgRyoAVuOVvH2vmz5U2r74_SRbAzD35M7yZ_tSnnEdMFlHMFbf5uNwmgArrtPgh0V5OLn5i4XIc154FLTiQlvAEhUxiPuYBkm_1GBiYEH4JjP2RKXAUx_TxAVwPsOfIPAVrO0Ev_nvdtVLCE-uOn8WQbxh4wwOztaXOV1HIaPrl7HN-YtDOA840QUHm97ZZLAPRgLzGlkMI0ZS8QkYdb9_FT3KMbNu60nBKEniv2uhBdIhM9g.46AsIpPgnJCLH0Xm.u2rG.LyEHEGCWM8CXDEEHiaqhiQ";
        mockery.checking(new Expectations() {
            {
                one(config).getJweDecryptionKey();
                will(returnValue(rsaJwk.getPrivateKey()));
                one(config).getId();
                will(returnValue(testName.getMethodName()));
            }
        });
        try {
            String payload = JweHelper.extractPayloadFromJweToken(jweStringValidPkcsPaddingEncodedKeySizeIncorrect, config, null);
            fail("Should have thrown an exception, but didn't. Got payload: " + payload);
        } catch (InvalidTokenException e) {
            verifyException(e, MSG_UNSUPPORTED_KEY_MANAGEMENT_ALGORITHM + ".*" + expectedExceptionString);
            int indexOfJoseException = e.getMessage().indexOf(expectedExceptionString);
            joseException2 = e.getMessage().substring(indexOfJoseException);
        }
        assertEquals("Exception from scenario 1 should match the exception from scenario 2, but it didn't", joseException1, joseException2);
    }

    /**
     * See https://github.com/advisories/GHSA-jgvc-jfgh-rjvv. Decrypted payload would be "foo", but decryption should fail because
     * the algorithm in the JWE header does not match the algorithm of the key used to do the decryption.
     */
    @Test
    public void test_extractPayloadFromJweToken_headerAlgDoesNotMatchAlgInKey() throws Exception {
        JSONObject jwkJson = new JSONObject();
        jwkJson.put("kty", "RSA");
        jwkJson.put("alg", "RSA-OAEP");
        jwkJson.put("use", "enc");
        jwkJson.put("n", "kqGboBfAWttWPCA-0cGRgsY6SaYoIARt0B_PkaEcIq9HPYNdu9n6UuWHuuTHrjF_ZoQW97r5HaAorNvrMEGTGdxCHZdEtkHvNVVmrtxTBLiQCbCozXhFoIrVcr3qUBrdGnNn_M3jJi7Wg7p_-x62nS5gNG875oyheRkutHsQXikFZwsN3q_TsPNOVlCiHy8mxzaFTUQGm-X8UYexFyAivlDSjgDJLAZSWfxd7k9Gxuwa3AUfQqQcVcegmgKGCaErQ3qQbh1x7WB6iopE3_-GZ8HMAVtR9AmrVscqYsnjhaCehfAI0iKKs8zXr8tISc0ORbaalrkk03H1ZrsEnDKEWQ");
        jwkJson.put("e", "AQAB");
        jwkJson.put("kid", "kid-rsa-enc-oaep");
        jwkJson.put("d", "YsfIRYN6rDqSz5KRf1E9q7HK1o6-_UK-j7S-asb0Y1FdVs1GuiRQhMPoOjmhY3Io93EI3_7vj8uzWzAUMsAaTxOY3sJnIbktYuqTcD0xGD8VmdGPBkx963db8B6M2UYfqZARf7dbzP9EuB1N1miMcTsqyGgfHGOk7CXQ1vkIv8Uww38KMtEdJ3iB8r-f3qcu-UJjE7Egw9CxKOMjArOXxZEr4VnoIXrImrcTxBfjdY8GbzXGATiPQLur5GT99ZDW78falsir-b5Ean6HNyOeuaJuceT-yjgCXn57Rd3oIHD94CrjNtjBusoLdjbr489L8K9ksCh1gynzLGkeeWgVGQ");
        jwkJson.put("p", "0xalbl1PJbSBGD4XOjIYJLwMYyHMiM06SBauMGzBfCask5DN5jH68Kw1yPS4wkLpx4ltGLuy0X5mMaZzrSOkBGb27-NizBgB2-L279XotznWeh2jbF05Kqzkoz3VaX_7dRhCHEhOopMQh619hA1bwaJyW1k8aNlLPTl3BotkP4M");
        jwkJson.put("q", "sdQsQVz3tI7hmisAgiIjppOssEnZaZO0ONeRRDxBHGLe3BCo1FJoMMQryOAlglayjQnnWjQ-BpwUpa0r9YQhVLweoNEIig6Beph7iYRZgOHEiiTTgUIGgXAL6xhsby1PueUfT0xsN1Y7qt5f5EwOfu7tnFqNyJXIp9W1NQgU6fM");
        jwkJson.put("dp", "kEpEnuJNfdqa-_VFb1RayJF6bjDmXQTcN_a47wUIZVMSWHR9KkMz41v0D_-oY7HVl73Kw0NagnVCaeH75HgeX5v6ZBQsrpIigynr3hl8T_LLNwIXebVnpFI2n5de0BTZ0DraxfZvOhYJEJV43NE8zWm7fdHLx2fxVFJ5mBGkXv0");
        jwkJson.put("dq", "U_xJCnXF51iz5AP7MXq-K6YDIR8_t0UzEMV-riNm_OkVKAoWMnDZFG8R3sU98djQaxwKT-fsg2KjvbuTz1igBUzzijAvQESpkiUB82i2fNAj6rqJybpNKESq3FWkoL1dsgYsS19knJ31gDWWRFRHZFujjPyXiexz4BBmjK1Mc1E");
        jwkJson.put("qi", "Uvb84tWiJF3fB-U9wZSPi7juGgrzeXS_LYtf5fcdV0fZg_h_5nSVpXyYyQ-PK218qEC5MlDkaHKRD9wBOe_eU_zJTNoXzB2oAcgl2MapBWUMytbiF84ghP_2K9UD63ZVsyrorSZhmsJIBBuqQjrmk0tIdpMdlMxLYhrbYwFxUqc");
        PublicJsonWebKey publicJwk = PublicJsonWebKey.Factory.newPublicJwk(jwkJson.toString());
        final RsaJsonWebKey rsaJwkForAlgMismatch = (RsaJsonWebKey) publicJwk;
        String jweStringHeaderAlgDoesNotMatchAlgInKey = "eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4R0NNIn0.CuUuY9PH2wWjuLXd5O9LLFanwyt5-y-NzEpy9rC3A63tFsvdp8GWP1kRt1d3zd0bGqakwls623VQxzxqQ25j5gdHh8dKMl67xTLHt1Qlg36nI9Ukn7syq25VrzfrRRwy0k7isqMncHpzuBQlmfzPrszW7d13z7_ex0Uha869RaP-W2NNBfHYw26xIXcCSVIPg8jTLA7h6QmOetEej-NXXcWrRKQgBRapYy4iWrij9Vr3JzAGSHVtIID74tFOm01FdJj4s1M4IXegDbvAdQb6Vao1Ln5GolnTki4IGvH5FDssDHz6MS2JG5QBcITzfuXU81vDC00xzNEuMat0AngmOw.UjPQbnakkZYUdoDa.vcbS.WQ_bOPiGKjPSq-qyGOIfjA";
        mockery.checking(new Expectations() {
            {
                one(config).getJweDecryptionKey();
                will(returnValue(rsaJwkForAlgMismatch.getPrivateKey()));
                one(config).getId();
                will(returnValue(testName.getMethodName()));
            }
        });
        try {
            String payload = JweHelper.extractPayloadFromJweToken(jweStringHeaderAlgDoesNotMatchAlgInKey, config, null);
            fail("Should have thrown an exception, but didn't. Got payload: " + payload);
        } catch (InvalidTokenException e) {
            // Expected
            verifyException(e, MSG_UNSUPPORTED_KEY_MANAGEMENT_ALGORITHM + ".*" + "org.jose4j.lang.InvalidAlgorithmException");
        }
    }

}

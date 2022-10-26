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
package io.openliberty.security.oidcclientcore.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.PrivilegedActionException;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wsspi.ssl.SSLSupport;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import io.openliberty.security.oidcclientcore.utils.CommonJose4jUtils.TokenSignatureValidationBuilder;
import test.common.SharedOutputManager;

/**
 *
 */
public class CommonJose4jUtilsTest {    
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    private CommonJose4jUtils jose4jutils = new CommonJose4jUtils();
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    protected final SSLSupport sslSupport =  mock.mock(SSLSupport.class, "sslSupport");
    
    String idToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImF0X2hhc2giOiJqQmZHX3pfWkJGRHRxaXdRamhmOU5BIiwicmVhbG1OYW1lIjoiQmFzaWNSZWFsbSIsInVuaXF1ZVNlY3VyaXR5TmFtZSI6InRlc3R1c2VyIiwic2lkIjoiOVFqTEJwZlU5V0NVS242QVc2eGkiLCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo4OTIwL29pZGMvZW5kcG9pbnQvT1AxIiwiYXVkIjoiY2xpZW50XzEiLCJleHAiOjE2NjMyNTk1MjEsImlhdCI6MTY2MzI1MjMyMSwibm9uY2UiOiJxdFBUSDVkSHVoNXFKTTZ0U0Q2VCJ9.belybPX9BOGsWTziulLgWrfqvyHrUe1wjONR5ozaOZc";
    JwtContext jwtcontext = null;
    JwtClaims jwtClaims = null;
    JsonWebStructure jsonStruct = null;
    TokenSignatureValidationBuilder tokenSignatureValidationBuilder;
    String secret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger";
    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        jwtcontext = JwtParsingUtils.parseJwtWithoutValidation(idToken);
        jwtClaims = jwtcontext.getJwtClaims();
        jsonStruct = JwtParsingUtils.getJsonWebStructureFromJwtContext(jwtcontext);
        tokenSignatureValidationBuilder = jose4jutils.signaturevalidationbuilder();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    /**
     * Test method for {@link io.openliberty.security.oidcclientcore.utils.CommonJose4jUtils#signaturevalidationbuilder()}.
     */
    @Test
    public void testSignaturevalidationbuilderExpiredToken() {
        
        tokenSignatureValidationBuilder.signature(jsonStruct)
                                       .sslsupport(sslSupport)
                                       .issuer("https://localhost:8920/oidc/endpoint/OP1")
                                       .clientid("client_1")
                                       .clientsecret(secret);
        try {
            tokenSignatureValidationBuilder.parseJwtWithValidation(idToken);
        } catch (KeyStoreException e) {
            outputMgr.failWithThrowable("signature validation", e);
        } catch (PrivilegedActionException e) {
            outputMgr.failWithThrowable("signature validation", e);
        } catch (IOException e) {
            outputMgr.failWithThrowable("signature validation", e);
        } catch (InterruptedException e) {
            outputMgr.failWithThrowable("signature validation", e);
        } catch (Exception e) {
            String message = e.getMessage();
            assertTrue("error msg should have something about no longer valid", message.contains("no longer valid"));
        }        
    }
    
    @Test
    public void testSignaturevalidationbuilderWrongIssuer() {
        
        tokenSignatureValidationBuilder.signature(jsonStruct)
                                       .sslsupport(sslSupport)
                                       .issuer("https://localhost:8920/oidc/endpoint/MISMATCHOP")
                                       .clientid("client_1")
                                       .clientsecret(secret);        
        try {
            tokenSignatureValidationBuilder.parseJwtWithValidation(idToken);
            fail("Expected an InvalidJwtException due to issuer mismatch");
        } catch (KeyStoreException e) {
            outputMgr.failWithThrowable("signature validation", e);
        } catch (PrivilegedActionException e) {
            outputMgr.failWithThrowable("signature validation", e);
        } catch (IOException e) {
            outputMgr.failWithThrowable("signature validation", e);
        } catch (InterruptedException e) {
            outputMgr.failWithThrowable("signature validation", e);
        } catch (Exception e) {
            String message = e.getMessage();
            assertTrue("error msg should have something about claims", message.contains("invalid claims"));  
        }      
    }
    
    @Test
    public void testSignaturevalidationbuilderWrongKey() {
        
        tokenSignatureValidationBuilder.signature(jsonStruct)
                                       .sslsupport(sslSupport)
                                       .issuer("https://localhost:8920/oidc/endpoint/MISMATCHOP")
                                       .clientid("client_1")
                                       .clientsecret(secret+"is not right");        
        try {
            tokenSignatureValidationBuilder.parseJwtWithValidation(idToken);
            fail("Expected an exception");
        } catch (KeyStoreException e) {
            outputMgr.failWithThrowable("signature validation", e);
        } catch (PrivilegedActionException e) {
            outputMgr.failWithThrowable("signature validation", e);
        } catch (IOException e) {
            outputMgr.failWithThrowable("signature validation", e);
        } catch (InterruptedException e) {
            outputMgr.failWithThrowable("signature validation", e);
        } catch (Exception e) {
            String message = e.getMessage();
            assertTrue("should be invalidjwtsignature exception", message.contains("JWT rejected due to invalid signature"));
        }      
    }
}

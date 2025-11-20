/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.acs;

import static com.ibm.ws.security.saml.sso20.common.CommonMockitoObjects.SAML20_AUTHENTICATION_FAIL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opensaml.saml.common.SAMLVersion.VERSION_11;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLProtocolContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.SignatureValidationParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.common.CommonMockitoObjects;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import test.common.SharedOutputManager;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ResponseValidatorTest {

    private final CommonMockitoObjects common = new CommonMockitoObjects();

    private final Assertion assertion = common.getAssertion();
    private final BasicMessageContext context = common.getBasicMessageContext();
    private final MessageContext messageContext = common.getMessageContext();
    private final SAMLPeerEntityContext samlPeerEntityContext = common.getSAMLPeerEntityContext();
    private final EncryptedAssertion encryptedAssertion = common.getEncryptedAssertion();
    private final EntityDescriptor entityDescriptor = common.getEntityDescriptor();
    private final HttpServletRequest request = common.getServletRequest();
    private final Issuer issuer = common.getIssuer();
    private final KeyInfoCredentialResolver keyInfoCredResolver = common.getKeyInfoCredResolver();
    private final AcsDOMMetadataProvider acsmetadataProvider = mock(AcsDOMMetadataProvider.class);
    private final Response samlResponse = common.getSamlResponse();
    private final SignatureValidationParameters signatureValidationParams = mock(SignatureValidationParameters.class);
    private final Signature signature = common.getSignature();
    private final SsoConfig ssoConfig = common.getSsoConfig();
    private final SsoSamlService ssoService = common.getSsoService();
    private final Status status = common.getStatus();
    private final StatusCode statusCode = common.getStatusCode();

    private final SAMLObject samlObject = mock(SAMLObject.class);
    private final SignableSAMLObject signableSAMLObject = mock(SignableSAMLObject.class);
    private final StatusMessage statusMessage = mock(StatusMessage.class);
    private final SecurityParametersContext securityParametersContext = mock(SecurityParametersContext.class);
    private final SAMLProtocolContext samlProtocolContext = mock(SAMLProtocolContext.class);

    private static final String INVALID_URI = "urn:oasis:names:tc:SAML:2.0:status:Invalid";
    private static final SAMLVersion INVALID_SAML_VERSION = VERSION_11;
    private static final String ISSUER_IDENTIFIER = "https://idp.example.org/SAML2";
    private static final String DESTINATION = "http://test.gdl.mex.ibm.com:9080/ibm/saml20/SAML2/acs";


    private static Instant date;

    private ResponseValidator validator;
    private String protocol = SAMLConstants.SAML20P_NS;
    private static final String DEFAULT_ELEMENT_LOCAL_NAME = "Response";
    private static final QName DEFAULT_ELEMENT_NAME = 
                    new QName(SAMLConstants.SAML20P_NS, DEFAULT_ELEMENT_LOCAL_NAME, SAMLConstants.SAML20P_PREFIX);
                
    private static List<Assertion> listAssertions = new ArrayList<Assertion>();
    private static List<EncryptedAssertion> listEncryptedAssertions = new ArrayList<EncryptedAssertion>();

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;
    @Rule
    public TestName currentTest = new TestName();

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
        date = Instant.now();
    }

    public void constructorExpectations(final long clockSkew) {
        when(samlResponse.getIssuer()).thenReturn(issuer);
        when(issuer.getValue()).thenReturn(ISSUER_IDENTIFIER);
        when(context.getSsoConfig()).thenReturn(ssoConfig);
        when(ssoConfig.getClockSkew()).thenReturn(clockSkew);
    }

    @Before
    public void before() {
        constructorExpectations(60000L);
        validator = new ResponseValidator(context, samlResponse);

        listAssertions.clear();
        listEncryptedAssertions.clear();
        listAssertions.add(assertion);
        listEncryptedAssertions.add(encryptedAssertion);
    }

    @After
    public void after() {
        // No need to verify expectations as in JMock
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testValidateStatus() {
        when(samlResponse.getStatus()).thenReturn(status);
        when(status.getStatusCode()).thenReturn(statusCode);
        when(statusCode.getValue()).thenReturn(INVALID_URI);
        when(status.getStatusMessage()).thenReturn(statusMessage);
        when(statusMessage.getMessage()).thenReturn("Invalid URI was found.");
        
        try {
            validator.validateStatus();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testValidateVersion_InvalidVersion() {
        when(samlResponse.getVersion()).thenReturn(INVALID_SAML_VERSION);
        
        try {
            validator.validateVersion();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testValidateDestination_DestinationExists() {
        when(samlResponse.getDestination()).thenReturn(DESTINATION);
        when(context.getHttpServletRequest()).thenReturn(request);
        when(context.getSsoService()).thenReturn(ssoService);
        when(ssoService.getProviderId()).thenReturn("SAML2");
        when(context.getSsoConfig()).thenReturn(ssoConfig);
        when(ssoConfig.getSpHostAndPort()).thenReturn("http://test.gdl.mex.ibm.com:9080");
        
        try {
            boolean valid = validator.validateDestination();
            assertTrue("Expected a 'true' value but it was not received.", valid);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testValidateDestination_UnexpectedDestination() {
        when(samlResponse.getDestination()).thenReturn(DESTINATION);
        when(context.getHttpServletRequest()).thenReturn(request);
        when(context.getSsoService()).thenReturn(ssoService);
        when(ssoService.getProviderId()).thenReturn("SAML2");
        when(context.getSsoConfig()).thenReturn(ssoConfig);
        when(ssoConfig.getSpHostAndPort()).thenReturn("http://test.bad.gdl.mex.ibm.com:9080");
        
        try {
            validator.validateDestination();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testValidateIssuer_InvalidFormat() {
        when(samlResponse.getIssuer()).thenReturn(issuer);
        when(issuer.getFormat()).thenReturn("invalid_format");
        
        try {
            validator.validateIssuer();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testValidateIssuer_EntityDescriptorIsNull() {
        when(samlResponse.getIssuer()).thenReturn(issuer);
        when(issuer.getFormat()).thenReturn(null);
        when(context.getPeerEntityMetadata()).thenReturn(null);
        when(issuer.getValue()).thenReturn(ISSUER_IDENTIFIER);
        when(context.getSsoConfig()).thenReturn(ssoConfig);
        when(ssoConfig.getPkixTrustedIssuers()).thenReturn(null);
        
        try {
            validator.validateIssuer();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testValidateIssuer_IncorrectIssuer() {
        when(samlResponse.getIssuer()).thenReturn(issuer);
        when(issuer.getFormat()).thenReturn(null);
        when(context.getPeerEntityMetadata()).thenReturn(entityDescriptor);
        when(entityDescriptor.getEntityID()).thenReturn(INVALID_URI);
        when(issuer.getValue()).thenReturn(ISSUER_IDENTIFIER);
        when(context.getSsoConfig()).thenReturn(ssoConfig);
        when(ssoConfig.getPkixTrustedIssuers()).thenReturn(null);
        
        try {
            validator.validateIssuer();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testValidateIssueInstant_InvalidTime() {
        date = Instant.now().plus(1000 * 365, ChronoUnit.DAYS); //date time isn't within laterOkTime and EarlierTime
        when(samlResponse.getIssueInstant()).thenReturn(date);
        
        try {
            validator.validateIssueInstant();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    /**
     * Set the issue instant and the current system time with the same value.
     * Set the clockskew to 1 minute.
     * Since the issue instant is within a valid later and earlier time, validations is successful.
     */
    @Test
    public void testFakeCurrentTime_ClockSkewSetTo1Min() {
        final Instant issueInstant = Instant.now();
        constructorExpectations(60000L);
        when(samlResponse.getIssueInstant()).thenReturn(issueInstant);

        try {
            validator = new ResponseValidator(context, samlResponse);
            boolean result = validator.validateIssueInstant();
            assertTrue("Expected to receive a true value but it was not received.", result);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    /**
     * Set the issue instant and the current system time with the same value.
     * Set the clockskew to 0.
     * Since the issue instant is not within a valid later and earlier time, a SamlException is thrown.
     */
    @Test
    public void testFakeCurrentTime_ClockSkewSetToZero() {
        final Instant issueInstant = Instant.now();
        constructorExpectations(0L);
        when(samlResponse.getIssueInstant()).thenReturn(issueInstant);

        try {
            validator = new ResponseValidator(context, samlResponse);
            validator.validateIssueInstant();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    /**
     * Set the issue instant 2 minutes before the current system time.
     * Set the clockskew to 3 minutes.
     * Since the issue instant is within a valid later and earlier time, validations is successful.
     */
    @Test
    public void testFakeCurrentTimeMinus2Min_ClockSkewSetTo3Min() {
        final Instant issueInstant = Instant.now().minus(120000L, ChronoUnit.MILLIS);
        constructorExpectations(180000L);
        when(samlResponse.getIssueInstant()).thenReturn(issueInstant);

        try {
            validator = new ResponseValidator(context, samlResponse);
            boolean result = validator.validateIssueInstant();
            assertTrue("Expected to receive a true value but it was not received.", result);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    /**
     * Set the issue instant 2 minutes after the current system time.
     * Set the clockskew to 3 minutes.
     * Since the issue instant is within a valid later and earlier time, validations is successful.
     */
    @Test
    public void testFakeCurrentTimePlus2Min_ClockSkewSetTo3Min() {
        final Instant issueInstant = Instant.now().plus(120000L, ChronoUnit.MILLIS);
        constructorExpectations(180000L);
        when(samlResponse.getIssueInstant()).thenReturn(issueInstant);

        try {
            validator = new ResponseValidator(context, samlResponse);
            boolean result = validator.validateIssueInstant();
            assertTrue("Expected to receive a true value but it was not received.", result);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    /**
     * Set the issue instant 2 minutes before the current system time.
     * Set the clockskew to 1 minute.
     * Since the issue instant is not within a valid later and earlier time, a SamlException is thrown.
     */
    @Test
    public void testFakeCurrentTimeMinus2Min_ClockSkewSetTo1Min() {
        final Instant issueInstant = Instant.now().minus(120000L, ChronoUnit.MILLIS);
        constructorExpectations(60000L);
        when(samlResponse.getIssueInstant()).thenReturn(issueInstant);

        try {
            validator = new ResponseValidator(context, samlResponse);
            validator.validateIssueInstant();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    /**
     * Set the issue instant 2 minutes after the current system time.
     * Set the clockskew to 1 minute.
     * Since the issue instant is not within a valid later and earlier time, a SamlException is thrown.
     */
    @Test
    public void testFakeCurrentTimePlus2Min_ClockSkewSetTo1Min() {
        final Instant issueInstant = Instant.now().plus(120000L, ChronoUnit.MILLIS);
        constructorExpectations(0L);
        when(samlResponse.getIssueInstant()).thenReturn(issueInstant);

        try {
            validator = new ResponseValidator(context, samlResponse);
            validator.validateIssueInstant();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testValidateResponseSignature() {
        final String LOW_VALUE = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
        setupSignatureProcessingExpectations(true, true);
        
        when(ssoConfig.getSignatureMethodAlgorithm()).thenReturn(LOW_VALUE);
        when(signature.getSignatureAlgorithm()).thenReturn(LOW_VALUE);
        when(messageContext.getMessage()).thenReturn(samlResponse);
        when(context.getInboundSamlMessageIssuer()).thenReturn(ISSUER_IDENTIFIER);
        when(samlResponse.getElementQName()).thenReturn(DEFAULT_ELEMENT_NAME);
        
        try {
            validator.validateResponseSignature();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testVerifyResponseSignature_SamlInboundMessageNotAuthenticated() {
        final String LOW_VALUE = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
        setupSignatureProcessingExpectations(true, false);
        
        when(ssoConfig.getSignatureMethodAlgorithm()).thenReturn(LOW_VALUE);
        when(signature.getSignatureAlgorithm()).thenReturn(LOW_VALUE);
        when(messageContext.getMessage()).thenReturn(samlResponse);
        when(context.getInboundSamlMessageIssuer()).thenReturn(ISSUER_IDENTIFIER);
        when(samlResponse.getElementQName()).thenReturn(DEFAULT_ELEMENT_NAME);

        try {
            validator.verifyResponseSignature();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            String received = ex.getErrorMessage(); //CWWKS5046E: There is an error while verifying the SAML response message Signature
            String expected = "CWWKS5046E:";
            assertTrue("Expected to receive " + expected + ", but did not..", received.contains(expected) && SAML20_AUTHENTICATION_FAIL.equals(ex.getMsgKey()));
        }
    }

    @Test
    public void testVerifyResponseSignature_ThrowsSecurityPolicyException() {
        final String LOW_VALUE = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
        final String HIGH_VALUE = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";

        setupSignatureProcessingExpectations(true, true);
        when(messageContext.getMessage()).thenReturn(signableSAMLObject);
        when(signableSAMLObject.isSigned()).thenReturn(true);
        when(signableSAMLObject.getSignature()).thenReturn(signature);
        when(ssoConfig.getSignatureMethodAlgorithm()).thenReturn(HIGH_VALUE);
        when(signature.getSignatureAlgorithm()).thenReturn(LOW_VALUE);
        
        try {
            validator.verifyResponseSignature();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            String received = ex.getErrorMessage();
            String expected = "CWWKS5007E:";
            String extra = "the signature method provided is weaker than the required";
            /*
             * CWWKS5007E: An internal server error occurred while processing SAML Web Single Sign-On (SSO) request 
             * [org.opensaml.messaging.handler.MessageHandlerException]. Cause:[The server is configured with the signature method http://www.w3.org/2001/04/xmldsig-more#rsa-sha512 but the received SAML assertion is signed with the signature method http://www.w3.org/2000/09/xmldsig#rsa-sha1, the signature method provided is weaker than the required.], StackTrace: [
             */
            assertTrue("Expected to receive the message for " + extra + "' but it was not received.",
                         received.contains(expected) && received.contains(extra));  
        }
    }
    
    private void setupSignatureProcessingExpectations(boolean trust, boolean authenticate) {
        final QName role = IDPSSODescriptor.DEFAULT_ELEMENT_NAME;
        final SignatureTrustEngine signatureTrustEngine = new MockSignatureTrustEngine(trust);
        
        when(context.getSsoConfig()).thenReturn(ssoConfig);
        when(ssoConfig.isPkixTrustEngineEnabled()).thenReturn(false);
        when(context.getMetadataProvider()).thenReturn(acsmetadataProvider);
        when(context.getMessageContext()).thenReturn(messageContext);
        
        when(messageContext.getSubcontext(SecurityParametersContext.class, true)).thenReturn(securityParametersContext);
        when(messageContext.getSubcontext(SecurityParametersContext.class)).thenReturn(securityParametersContext);
        when(securityParametersContext.getSignatureValidationParameters()).thenReturn(signatureValidationParams);
        when(signatureValidationParams.getSignatureTrustEngine()).thenReturn(signatureTrustEngine);
        
        when(messageContext.getSubcontext(SAMLPeerEntityContext.class)).thenReturn(samlPeerEntityContext);
        when(samlPeerEntityContext.getRole()).thenReturn(role);
        when(messageContext.getSubcontext(SAMLProtocolContext.class)).thenReturn(samlProtocolContext);
        when(samlProtocolContext.getProtocol()).thenReturn(protocol);
        
        when(samlResponse.isSigned()).thenReturn(true);
        when(samlResponse.getSignature()).thenReturn(signature);
        when(samlPeerEntityContext.isAuthenticated()).thenReturn(authenticate);
    }
    
    /** Mock trust engine. */
    private class MockSignatureTrustEngine implements SignatureTrustEngine {

        private Boolean trusted;

        private MockSignatureTrustEngine(Boolean flag) {
            trusted = flag;
        }

        @Override
        public boolean validate(Signature tok, CriteriaSet trustBasisCriteria) throws org.opensaml.security.SecurityException {
            if (trusted == null) {
                throw new org.opensaml.security.SecurityException("This means an error happened");
            }
            return trusted;
        }

        @Override
        public boolean validate(byte[] signature, byte[] content, String algorithmURI,
                CriteriaSet trustBasisCriteria, Credential candidateCredential)
                throws org.opensaml.security.SecurityException {
            if (trusted == null) {
                throw new org.opensaml.security.SecurityException("This means an error happened");
            }
            return trusted;
        }

        @Override
        public KeyInfoCredentialResolver getKeyInfoResolver() {
            return null;
        }
    }
}

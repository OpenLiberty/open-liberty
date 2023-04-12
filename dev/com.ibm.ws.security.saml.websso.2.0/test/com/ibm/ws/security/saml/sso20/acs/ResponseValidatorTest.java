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

import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.SAML20_AUTHENTICATION_FAIL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opensaml.saml.common.SAMLVersion.VERSION_11;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
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
//import org.opensaml.xmlsec.security.SecurityConfiguration;
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import test.common.SharedOutputManager;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ResponseValidatorTest {

    private final CommonMockObjects common = new CommonMockObjects();
    private final Mockery mockery = common.getMockery();

    private final Assertion assertion = common.getAssertion();
    private final BasicMessageContext context = common.getBasicMessageContext();
    private final MessageContext messageContext = common.getMessageContext();
    private final SAMLPeerEntityContext samlPeerEntityContext = common.getSAMLPeerEntityContext();
    private final SAMLProtocolContext samlProtocolContext = mockery.mock(SAMLProtocolContext.class);
    private final EncryptedAssertion encryptedAssertion = common.getEncryptedAssertion();
    private final EntityDescriptor entityDescriptor = common.getEntityDescriptor();
    private final HttpServletRequest request = common.getServletRequest();
    private final Issuer issuer = common.getIssuer();
    private final KeyInfoCredentialResolver keyInfoCredResolver = common.getKeyInfoCredResolver();
    //private final MetadataProvider metadataProvider = common.getMetadataProvider();
    private final AcsDOMMetadataProvider acsmetadataProvider = mockery.mock(AcsDOMMetadataProvider.class);
    private final Response samlResponse = common.getSamlResponse();
    private final SecurityParametersContext securityParametersContext = mockery.mock(SecurityParametersContext.class);
    private final SignatureValidationParameters signatureValidationParams = mockery.mock(SignatureValidationParameters.class);
    //private final SignatureTrustEngine signatureTrustEngine = mockery.mock(SignatureTrustEngine.class);
    //private final SecurityConfiguration securityConfig = common.getSecurityConfig();
    private final Signature signature = common.getSignature();
    private final SsoConfig ssoConfig = common.getSsoConfig();
    private final SsoSamlService ssoService = common.getSsoService();
    private final Status status = common.getStatus();
    private final StatusCode statusCode = common.getStatusCode();

    private final SAMLObject samlObject = mockery.mock(SAMLObject.class);
    private final SignableSAMLObject signableSAMLObject = mockery.mock(SignableSAMLObject.class, "signableSAMLObject");
    private final StatusMessage statusMessage = mockery.mock(StatusMessage.class, "statusMessage");

    private static final String INVALID_URI = "urn:oasis:names:tc:SAML:2.0:status:Invalid";
    private static final SAMLVersion INVALID_SAML_VERSION = VERSION_11;
    private static final String ISSUER_IDENTIFIER = "https://idp.example.org/SAML2";
    private static final String DESTINATION = "http://test.gdl.mex.ibm.com:9080/ibm/saml20/SAML2/acs";

    private static DateTime date;

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

        date = new DateTime();
    }

    public void constructorExpectations(final long clockSkew) {
        mockery.checking(new Expectations() {
            {
                one(samlResponse).getIssuer();
                will(returnValue(issuer));
                one(issuer).getValue();
                will(returnValue(ISSUER_IDENTIFIER));
                one(context).getSsoConfig();
                will(returnValue(ssoConfig));
                one(ssoConfig).getClockSkew();
                will(returnValue(clockSkew));
            }
        });
    }

    @Before
    public void before() {
        constructorExpectations(60000L);

        //Configuration.setGlobalSecurityConfiguration();

        validator = new ResponseValidator(context, samlResponse);

        listAssertions.clear();
        listEncryptedAssertions.clear();
        listAssertions.add(assertion);
        listEncryptedAssertions.add(encryptedAssertion);
    }

    @After
    public void after() {
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDown() {
        //Configuration.setGlobalSecurityConfiguration(null);
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testValidateStatus() {
        mockery.checking(new Expectations() {
            {
                allowing(samlResponse).getStatus();
                will(returnValue(status));
                allowing(status).getStatusCode();
                will(returnValue(statusCode));
                allowing(statusCode).getValue();
                will(returnValue(INVALID_URI));
                one(status).getStatusMessage();
                will(returnValue(statusMessage));
                one(statusMessage).getMessage();
                will(returnValue("Invalid URI was found."));
            }
        });
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
        mockery.checking(new Expectations() {
            {
                one(samlResponse).getVersion();
                will(returnValue(INVALID_SAML_VERSION));
            }
        });
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
        mockery.checking(new Expectations() {
            {
                one(samlResponse).getDestination();
                will(returnValue(DESTINATION));
                allowing(context).getHttpServletRequest();
                will(returnValue(request));
                allowing(context).getSsoService();
                will(returnValue(ssoService));
                allowing(ssoService).getProviderId();
                will(returnValue("SAML2"));
                allowing(context).getSsoConfig();
                will(returnValue(ssoConfig));
                one(ssoConfig).getSpHostAndPort();
                will(returnValue("http://test.gdl.mex.ibm.com:9080"));
            }
        });
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
        mockery.checking(new Expectations() {
            {
                one(samlResponse).getDestination();
                will(returnValue(DESTINATION));
                allowing(context).getHttpServletRequest();
                will(returnValue(request));
                allowing(context).getSsoService();
                will(returnValue(ssoService));
                allowing(ssoService).getProviderId();
                will(returnValue("SAML2"));
                allowing(context).getSsoConfig();
                will(returnValue(ssoConfig));
                one(ssoConfig).getSpHostAndPort();
                will(returnValue("http://test.bad.gdl.mex.ibm.com:9080"));
            }
        });
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
        mockery.checking(new Expectations() {
            {
                one(samlResponse).getIssuer();
                will(returnValue(issuer));
                allowing(issuer).getFormat();
                will(returnValue("invalid_format"));
            }
        });
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
        mockery.checking(new Expectations() {
            {
                one(samlResponse).getIssuer();
                will(returnValue(issuer));
                one(issuer).getFormat();
                will(returnValue(null));
                one(context).getPeerEntityMetadata();
                will(returnValue(null));
                allowing(issuer).getValue();
                will(returnValue(ISSUER_IDENTIFIER));
                one(context).getSsoConfig();
                will(returnValue(ssoConfig));
                one(ssoConfig).getPkixTrustedIssuers();
                will(returnValue(null));
            }
        });
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
        mockery.checking(new Expectations() {
            {
                one(samlResponse).getIssuer();
                will(returnValue(issuer));
                one(issuer).getFormat();
                will(returnValue(null));
                one(context).getPeerEntityMetadata();
                will(returnValue(entityDescriptor));
                one(entityDescriptor).getEntityID();
                will(returnValue(INVALID_URI));
                allowing(issuer).getValue();
                will(returnValue(ISSUER_IDENTIFIER));
                one(context).getSsoConfig();
                will(returnValue(ssoConfig));
                one(ssoConfig).getPkixTrustedIssuers();
                will(returnValue(null));
            }
        });
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
        date = new DateTime().plusYears(1000); //date time isn't within laterOkTime and EarlierTime

        mockery.checking(new Expectations() {
            {
                one(samlResponse).getIssueInstant();
                will(returnValue(date));
            }
        });
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
        final DateTime issueInstant = new DateTime();

        constructorExpectations(60000L);
        mockery.checking(new Expectations() {
            {
                one(samlResponse).getIssueInstant();
                will(returnValue(issueInstant));
            }
        });

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
        final DateTime issueInstant = new DateTime();

        constructorExpectations(0L);
        mockery.checking(new Expectations() {
            {
                one(samlResponse).getIssueInstant();
                will(returnValue(issueInstant));
            }
        });

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
        final DateTime issueInstant = new DateTime().minus(120000L);
        constructorExpectations(180000L);
        mockery.checking(new Expectations() {
            {
                one(samlResponse).getIssueInstant();
                will(returnValue(issueInstant));
            }
        });

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
        final DateTime issueInstant = new DateTime().plus(120000L);
        constructorExpectations(180000L);
        mockery.checking(new Expectations() {
            {
                one(samlResponse).getIssueInstant();
                will(returnValue(issueInstant));
            }
        });

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
        final DateTime issueInstant = new DateTime().minus(120000L);
        constructorExpectations(60000L);
        mockery.checking(new Expectations() {
            {
                one(samlResponse).getIssueInstant();
                will(returnValue(issueInstant));
            }
        });

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
        final DateTime issueInstant = new DateTime().plus(120000L);
        constructorExpectations(0L);
        mockery.checking(new Expectations() {
            {
                one(samlResponse).getIssueInstant();
                will(returnValue(issueInstant));
            }
        });

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
        signatureProcessingExpectations(true, true);
        mockery.checking(new Expectations() {
            {
                
                one(ssoConfig).getSignatureMethodAlgorithm();
                will(returnValue(LOW_VALUE));

                one(signature).getSignatureAlgorithm();
                will(returnValue(LOW_VALUE));
                
                allowing(messageContext).getMessage();
                will(returnValue(samlResponse));
                
                one(context).getInboundSamlMessageIssuer();
                will(returnValue(ISSUER_IDENTIFIER));
                one(samlResponse).getElementQName();
                will(returnValue(DEFAULT_ELEMENT_NAME));
                
            }
        });
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
        signatureProcessingExpectations(true, false);
        mockery.checking(new Expectations() {
            {
                
                one(ssoConfig).getSignatureMethodAlgorithm();
                will(returnValue(LOW_VALUE));

                one(signature).getSignatureAlgorithm();
                will(returnValue(LOW_VALUE));
                
                allowing(messageContext).getMessage();
                will(returnValue(samlResponse));
                
                one(context).getInboundSamlMessageIssuer();
                will(returnValue(ISSUER_IDENTIFIER));
                one(samlResponse).getElementQName();
                will(returnValue(DEFAULT_ELEMENT_NAME));

                one(samlPeerEntityContext).setAuthenticated(with(any(Boolean.class)));
            }
        });

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

        signatureProcessingExpectations(true, true);
        mockery.checking(new Expectations() {
            {
                allowing(messageContext).getMessage();
                will(returnValue(signableSAMLObject));
                allowing(signableSAMLObject).isSigned();
                will(returnValue(true));
                one(signableSAMLObject).getSignature();
                will(returnValue(signature));
                one(ssoConfig).getSignatureMethodAlgorithm();
                will(returnValue(HIGH_VALUE));
                one(signature).getSignatureAlgorithm();
                will(returnValue(LOW_VALUE));
            }
        });
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
            //assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
            //             SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
            assertTrue("Expected to receive the message for " + extra + "' but it was not received.",
                         received.contains(expected) && received.contains(extra));  
            
        }
    }
    
    void signatureProcessingExpectations(boolean trust, boolean authenticate) {
        
        final QName role = IDPSSODescriptor.DEFAULT_ELEMENT_NAME;
        final SignatureTrustEngine signatureTrustEngine = new MockSignatureTrustEngine(trust);
        mockery.checking(new Expectations() {
            {
                allowing(context).getSsoConfig();
                will(returnValue(ssoConfig));
                one(ssoConfig).isPkixTrustEngineEnabled();
                will(returnValue(false));
                one(context).getMetadataProvider();
                will(returnValue(acsmetadataProvider));

                atLeast(2).of(context).getMessageContext();
                will(returnValue(messageContext));
                one(messageContext).getSubcontext(SecurityParametersContext.class, true);
                will(returnValue(securityParametersContext));

                allowing(securityParametersContext).setSignatureValidationParameters(with(any(SignatureValidationParameters.class)));
                allowing(messageContext).getSubcontext(SecurityParametersContext.class);
                will(returnValue(securityParametersContext));
                atLeast(2).of(securityParametersContext).getSignatureValidationParameters();
                will(returnValue(signatureValidationParams));
                one(signatureValidationParams).getSignatureTrustEngine();
                will(returnValue(signatureTrustEngine));

                one(messageContext).getSubcontext(SAMLPeerEntityContext.class);
                will(returnValue(samlPeerEntityContext));

                allowing(samlPeerEntityContext).getRole();
                will(returnValue(role));
                one(messageContext).getSubcontext(SAMLProtocolContext.class);
                will(returnValue(samlProtocolContext));
                allowing(samlProtocolContext).getProtocol();
                will(returnValue(protocol));

                allowing(samlResponse).isSigned();
                will(returnValue(true));
  
                allowing(samlResponse).getSignature();
                will(returnValue(signature));

                allowing(samlPeerEntityContext).isAuthenticated();
                will(returnValue(authenticate));
            }
        });
        
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

       /* @Override
        public boolean validate(Signature arg0, CriteriaSet arg1) throws org.opensaml.security.SecurityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean validate(byte[] arg0, byte[] arg1, String arg2, CriteriaSet arg3, Credential arg4) throws org.opensaml.security.SecurityException {
            // TODO Auto-generated method stub
            return false;
        }*/

    }
}

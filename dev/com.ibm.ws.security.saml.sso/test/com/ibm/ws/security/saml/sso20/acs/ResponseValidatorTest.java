/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.acs;

import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.SAML20_AUTHENTICATION_FAIL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opensaml.common.SAMLVersion.VERSION_11;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

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
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.EncryptedAssertion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.StatusMessage;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.security.SecurityConfiguration;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.signature.Signature;

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;

import test.common.SharedOutputManager;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ResponseValidatorTest {

    private final CommonMockObjects common = new CommonMockObjects();
    private final Mockery mockery = common.getMockery();

    private final Assertion assertion = common.getAssertion();
    private final BasicMessageContext context = common.getBasicMessageContext();
    private final EncryptedAssertion encryptedAssertion = common.getEncryptedAssertion();
    private final EntityDescriptor entityDescriptor = common.getEntityDescriptor();
    private final HttpServletRequest request = common.getServletRequest();
    private final Issuer issuer = common.getIssuer();
    private final KeyInfoCredentialResolver keyInfoCredResolver = common.getKeyInfoCredResolver();
    private final MetadataProvider metadataProvider = common.getMetadataProvider();
    private final Response samlResponse = common.getSamlResponse();
    private final SecurityConfiguration securityConfig = common.getSecurityConfig();
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

        Configuration.setGlobalSecurityConfiguration(securityConfig);

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
        Configuration.setGlobalSecurityConfiguration(null);
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
            fail("Unexpected exception was thrown: " + ex.getMessage());
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
        mockery.checking(new Expectations() {
            {
                one(samlResponse).getSignature();
                will(returnValue(signature));
                one(context).getSsoConfig();
                will(returnValue(ssoConfig));
                one(ssoConfig).isPkixTrustEngineEnabled();
                will(returnValue(false));
                one(context).getMetadataProvider();
                will(returnValue(metadataProvider));
                one(securityConfig).getDefaultKeyInfoCredentialResolver();
                will(returnValue(keyInfoCredResolver));
                one(context).getInboundSAMLMessage();
                will(returnValue(samlObject));
                one(context).isInboundSAMLMessageAuthenticated();
                will(returnValue(true));
            }
        });
        try {
            validator.validateResponseSignature();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testVerifyResponseSignature_SamlMessageAuthenticatedIsNotInbound() {
        mockery.checking(new Expectations() {
            {
                one(context).getSsoConfig();
                will(returnValue(ssoConfig));
                one(ssoConfig).isPkixTrustEngineEnabled();
                will(returnValue(false));
                one(context).getMetadataProvider();
                will(returnValue(metadataProvider));
                one(securityConfig).getDefaultKeyInfoCredentialResolver();
                will(returnValue(keyInfoCredResolver));
                one(context).getInboundSAMLMessage();
                will(returnValue(samlObject));
                one(context).isInboundSAMLMessageAuthenticated();
                will(returnValue(false));
            }
        });

        try {
            validator.verifyResponseSignature();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testVerifyResponseSignature_ThrowsSecurityPolicyException() {
        final String LOW_VALUE = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
        final String HIGH_VALUE = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";

        mockery.checking(new Expectations() {
            {
                allowing(context).getSsoConfig();
                will(returnValue(ssoConfig));
                one(ssoConfig).isPkixTrustEngineEnabled();
                will(returnValue(false));
                one(context).getMetadataProvider();
                will(returnValue(metadataProvider));
                one(securityConfig).getDefaultKeyInfoCredentialResolver();
                will(returnValue(keyInfoCredResolver));
                one(context).getInboundSAMLMessage();
                will(returnValue(signableSAMLObject));
                one(signableSAMLObject).isSigned();
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
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }
}

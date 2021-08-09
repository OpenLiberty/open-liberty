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
package com.ibm.ws.security.saml.sso20.token;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.Audience;
import org.opensaml.saml2.core.AudienceRestriction;
import org.opensaml.saml2.core.AuthnContext;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.OneTimeUse;
import org.opensaml.saml2.core.ProxyRestriction;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.saml2.core.SubjectLocality;
import org.opensaml.saml2.core.impl.NameIDBuilder;
import org.opensaml.xml.Namespace;
import org.opensaml.xml.NamespaceManager;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.X509Certificate;
import org.opensaml.xml.signature.X509Data;
import org.opensaml.xml.util.Base64;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.xml.sax.SAXException;

import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.error.SamlException;

import test.common.SharedOutputManager;

/**
 * Unit test the {@link com.ibm.ws.security.saml.sso20.token.Saml20TokenImpl} class.
 */
public class Saml20TokenImplTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    //Mocked classes
    private static final Element ELEMENT_MCK = mockery.mock(Element.class, "element");
    private static final Document DOCUMENT_MCK = mockery.mock(Document.class, "document");
    private static final DOMImplementation DOMIMPLEMENTATION_MCK = mockery.mock(DOMImplementation.class, "domImplementation");
    private static final DOMImplementationLS DOMIMPLEMENTATIONLS_MCK = mockery.mock(DOMImplementationLS.class, "domImplementationLS");
    private static final KeyInfo KEYINFO_MCK = mockery.mock(KeyInfo.class, "keyInfo");
    private static final Signature SIGNATURE_MCK = mockery.mock(Signature.class, "signature");
    private static final Subject SUBJECT_MCK = mockery.mock(Subject.class, "subject");
    private static final X509Data X509DATA_MCK = mockery.mock(X509Data.class, "x509data");
    private static final X509Certificate X509CERTIFICATE_MCK = mockery.mock(X509Certificate.class, "x509certificate");
    private static final SubjectConfirmation SUBJECTCONFIRMATION_MCK = mockery.mock(SubjectConfirmation.class, "subjectConfirmation");
    private static final Conditions CONDITIONS_MCK = mockery.mock(Conditions.class, "conditions");
    private static final OneTimeUse ONETIMEUSE_MCK = mockery.mock(OneTimeUse.class, "oneTimeUse");
    private static final ProxyRestriction PROXYRESTRICTION_MCK = mockery.mock(ProxyRestriction.class, "proxyRestriction");
    private static final Audience AUDIENCE_MCK = mockery.mock(Audience.class, "audience");
    private static final AudienceRestriction AUDIENCERESTRICTION_MCK = mockery.mock(AudienceRestriction.class, "audienceRestriction");
    private static final AuthnStatement AUTHNSTATEMENT_MCK = mockery.mock(AuthnStatement.class, "authnStatement");
    private static final AuthnContext AUTHNCONTEXT_MCK = mockery.mock(AuthnContext.class, "authnContext");
    private static final AuthnContextClassRef AUTHNCONTEXTCLASSREF_MCK = mockery.mock(AuthnContextClassRef.class, "authnContextClassRef");
    private static final SubjectLocality SUBJECTLOCALITY_MCK = mockery.mock(SubjectLocality.class, "subjectLocality");
    private static final AttributeStatement ATTRIBUTESTATEMENT_MCK = mockery.mock(AttributeStatement.class, "attributeStatement");
    private static final Attribute ATTRIBUTE_MCK = mockery.mock(Attribute.class, "attribute");
    private static final Assertion ASSERTION_MCK = mockery.mock(Assertion.class, "assertion");
    private static final Issuer ISSUER_MCK = mockery.mock(Issuer.class, "issuer");

    //Constants to be used as parameters
    private static final String SAML_ID = "b07b804c-7c29-ea16-7300-4f3d6f7928ac";
    private static final QName SAML_QNAME = new QName("Assertion");
    private static final DateTimeFormatter SAML_DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTime SAML_ISSUE_INSTANT = SAML_DATE_FORMATTER.parseDateTime("2004-12-05T09:22:05");
    private static final String DOM_ELEMENT = "<saml:Assertion\r\n" +
                                              "   xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\r\n" +
                                              "   xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" +
                                              "   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
                                              "   ID=\"b07b804c-7c29-ea16-7300-4f3d6f7928ac\"\r\n" +
                                              "   Version=\"2.0\"\r\n" +
                                              "   IssueInstant=\"2004-12-05T09:22:05\">\r\n" +
                                              "   <saml:Issuer>https://idp.example.org/SAML2</saml:Issuer>\r\n" +
                                              "   <saml:Subject>\r\n" +
                                              "     <saml:NameID\r\n" +
                                              "       Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">\r\n" +
                                              "       3f7b3dcf-1674-4ecd-92c8-1544f346baf8\r\n" +
                                              "     </saml:NameID>\r\n" +
                                              "     <saml:SubjectConfirmation\r\n" +
                                              "       Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\r\n" +
                                              "       <saml:SubjectConfirmationData\r\n" +
                                              "         InResponseTo=\"aaf23196-1773-2113-474a-fe114412ab72\"\r\n" +
                                              "         Recipient=\"https://sp.example.com/SAML2/SSO/POST\"\r\n" +
                                              "         NotOnOrAfter=\"2004-12-05T09:27:05\"/>\r\n" +
                                              "     </saml:SubjectConfirmation>\r\n" +
                                              "   </saml:Subject>\r\n" +
                                              " </saml:Assertion>";

    private static final QName ISSUER_QNAME = new QName(Constants.LOCAL_NAME_Issuer);
    private static final String ISSUER_VALUE = "https://wlp-tfimidp1.austin.ibm.com:9443/sps/WlpTfimIdp1/saml20";
    private static final String ISSUER_FORMAT = "urn:oasis:names:tc:SAML:2.0:nameid-format:entity";

    private static final QName SIGNATURE_QNAME = new QName(Constants.LOCAL_NAME_Signature);
    private static final String SIGNATURE_X509CERTIFICATE_VALUE = "MIIElzCCA3+gAwIBAgIQNT2i6HKJtCXFUFRB8qYsZjANBgkqhkiG9w0BAQUFADB3MQswCQYDVQQG" +
                                                                  "EwJGUjEOMAwGA1UEBxMFUGFyaXMxDDAKBgNVBAoTA3BzYTEgMB4GA1UECxMXY2VydGlmaWNhdGUg" +
                                                                  "YXV0aG9yaXRpZXMxKDAmBgNVBAMTH0FDIFBTQSBQZXVnZW90IENpdHJvZW4gUHJvZ3JhbXMwHhcN" +
                                                                  "MDkwODE5MDcxNTE4WhcNMTEwODE5MDcxNTE5WjCBhjELMAkGA1UEBhMCZnIxHzAdBgkqhkiG9w0B" +
                                                                  "CQEWEHBhc3NleHRAbXBzYS5jb20xGDAWBgoJkiaJk/IsZAEBEwhtZGVtb2IwMDEMMAoGA1UEChMD" +
                                                                  "cHNhMREwDwYDVQQLEwhwcm9ncmFtczEbMBkGA1UEAxMSVGVzdCAtIFBBU1NFWFQgREVWMIGfMA0G" +
                                                                  "CSqGSIb3DQEBAQUAA4GNADCBiQKBgQCuY1nrepgACvDSTLWk5A1cFOJSwDbl6CWfYp3cNYR0K3YV" +
                                                                  "e07MDZn+Rv4jo3SusHVFds+mzKX2f8AeZjkA3Me/0yiS9UpS9LQZu9mnhFlZRhmUlDDoIZxovLXN" +
                                                                  "aOv/YHmPeTQMQmJZu5TjqraUq7La1c187AoJuNfpxt227N1vOQIDAQABo4IBkTCCAY0wDgYDVR0P" +
                                                                  "AQH/BAQDAgWgMB8GA1UdIwQYMBaAFLceWtTfVeRuVCTDQWkmwO4U01X/MAwGA1UdEwEB/wQCMAAw" +
                                                                  "gbYGA1UdIASBrjCBqzCBqAYKKoF6ARfOEAEBBDCBmTBBBggrBgEFBQcCARY1aHR0cDovL3JldW5p" +
                                                                  "cy5pbmV0cHNhLmNvbS9hdXRvcml0ZS9QQy1BQy1Qcm9ncmFtcy5wZGYwVAYIKwYBBQUHAgIwSDAK" +
                                                                  "FgNwc2EwAwIBARo6UG9saXRpcXVlIGRlIENlcnRpZmljYXRpb24gQUMgUFNBIFBldWdlb3QgQ2l0" +
                                                                  "cm9lbiBQcm9ncmFtczBcBgNVHR8EVTBTMFGgT6BNhktodHRwOi8vaW5mb2NlcnQucHNhLXBldWdl" +
                                                                  "b3QtY2l0cm9lbi5jb20vQUMtUFNBLVBldWdlb3QtQ2l0cm9lbi1Qcm9ncmFtcy5jcmwwHQYDVR0l" +
                                                                  "BBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMBYGA1UdDgQPBA1BVVRPX0dFTkVSQVRFMA0GCSqGSIb3" +
                                                                  "DQEBBQUAA4IBAQCvRtP6bFkOUEHcqc6yUX0Q1Gk2WaAcx4ziUB0tw2GR9I0276JRJR0EGuJ/N6Fn" +
                                                                  "3FhLQrSPmS97Xvc9XmiI66fQUdg64g9YqBecdiQlUkR20VLgI6Nq8pldQlWjU2iYlkP15U7VF4Qr" +
                                                                  "0Pb2QiIljZUCKdv3qdED2Ri33za46LfykrlwZB0uhTVUxI/AEtjkKVFaZaqanJg+vJyZI5b30z7g" +
                                                                  "Ff8L3ht4Z7SFKdmY3IQSGzElIAAUfduzTJX0cwnGSU9D4BJu1BS8hWnYPwhk+nBJ7OFhXdwYQFWq" +
                                                                  "fhpBLq+ciJti9OMhcdCSIi0PbrOqzqtX7hZUQOvfShhCTJnl5TJJ";

    private static final QName SUBJECT_QNAME = new QName(Constants.LOCAL_NAME_Subject);
    private static final NameID SUBJECT_NAME_ID = new NameIDBuilder().buildObject();
    private static final String SUBJECT_NAME_ID_VALUE = "user2";
    private static final String SUBJECT_NAME_ID_FORMAT = "urn:ibm:names:ITFIM:5.1:accessmanager";
    private static final String SUBJECT_CONFIRMATION_METHOD = "urn:oasis:names:tc:SAML:2.0:cm:bearer";

    private static final QName CONDITIONS_QNAME = new QName(Constants.LOCAL_NAME_Conditions);
    private static final DateTimeFormatter CONDITIONS_DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z");
    private static final DateTime CONDITIONS_NOT_ON_OR_AFTER = CONDITIONS_DATE_FORMATTER.parseDateTime("2014-11-20T21:11:34Z");
    private static final int PROXYRESTRICTION_PROXY_COUNT = 1;
    private static final String AUDIENCE_URI = "https://localhost:8020/ibm/saml20/sp/acs";

    private static final QName AUTHNSTATEMENT_QNAME = new QName(Constants.LOCAL_NAME_AuthnStatement);
    private static final DateTimeFormatter AUTHNSTATEMENTS_DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTime AUTHNSTATEMENT_AUTHN_INSTANT = AUTHNSTATEMENTS_DATE_FORMATTER.parseDateTime("2004-12-05T09:22:00");
    private static final DateTime AUTHNSTATEMENT_SESSION_NOT_ON_OR_AFTER = AUTHNSTATEMENT_AUTHN_INSTANT;
    private static final String AUTHNSTATEMENT_SESSION_INDEX = "_d81eb459-8f85-4b2e-8acd-e03a9053d85a";
    private static final String AUTHNCONTEXT_CLASSREF = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";
    private static final String SUBJECTLOCALITY_DNS_NAME = "www.example.com";
    private static final String SUBJECTLOCALITY_ADDRESS = "1.2.3.4";

    private static final QName ATTRIBUTESTATEMENT_QNAME = new QName(Constants.LOCAL_NAME_AttributeStatement);
    private static final String ATTRIBUTE_NAME = "urn:oid:1.3.6.1.4.1.5923.1.1.1.1";
    private static final String ATTRIBUTE_NAME_FORMAT = "urn:oasis:names:tc:SAML:2.0:attrname-format:uri";
    private static final String ATTRIBUTE_FRIENDLY_NAME = "eduPersonAffiliation";

    private static final QName NO_SUPPORTED_QNAME_IN_XMLOBJECT = new QName(Constants.LOCAL_NAME_Audience);

    private static final List<XMLObject> xmlObjectsList = new ArrayList<XMLObject>();
    private static final List<X509Data> x509DataList = new ArrayList<X509Data>();
    private static final List<X509Certificate> x509CertificatesList = new ArrayList<X509Certificate>();
    private static final List<SubjectConfirmation> subjectConfirmationList = new ArrayList<SubjectConfirmation>();
    private static final List<Audience> audienceList = new ArrayList<Audience>();
    private static final List<AudienceRestriction> audienceRestrictionList = new ArrayList<AudienceRestriction>();
    private static final List<Attribute> attributesList = new ArrayList<Attribute>();

    private static final byte[] EXPECTED_HOLDER_OF_KEY_BYTES = "testValue".getBytes();

    private static final String PROVIDER_ID = "b07b804c-7c29-ea16-7300-4f3d6f7928ac";

    private static Saml20TokenImpl samlToken;

    private static final NamespaceManager nsManager = mockery.mock(NamespaceManager.class, "nsManager"); //assertion.getNamespaceManager();

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    /**
     * This method add a SAML AttributeStatement XMLObject to the Assertion.
     */
    public static void loadSamlAttributeStatement() throws SamlException {
        xmlObjectsList.add(ATTRIBUTESTATEMENT_MCK);

        mockery.checking(new Expectations() {
            {
                one(ATTRIBUTESTATEMENT_MCK).getElementQName();
                will(returnValue(ATTRIBUTESTATEMENT_QNAME));

                attributesList.add(ATTRIBUTE_MCK);

                one(ATTRIBUTESTATEMENT_MCK).getAttributes();
                will(returnValue(attributesList));
                one(ATTRIBUTE_MCK).detach();
                one(ATTRIBUTE_MCK).getDOM();
                will(returnValue(ELEMENT_MCK));
                one(ELEMENT_MCK).getOwnerDocument();
                will(returnValue(DOCUMENT_MCK));
                one(DOCUMENT_MCK).getImplementation();
                will(returnValue(DOMIMPLEMENTATION_MCK));
                one(DOMIMPLEMENTATION_MCK).getFeature("LS", "3.0");
                will(returnValue(DOMIMPLEMENTATIONLS_MCK));
                ignoring(DOMIMPLEMENTATIONLS_MCK);
                ignoring(ATTRIBUTE_MCK).getName();
                will(returnValue(ATTRIBUTE_NAME));
                one(ATTRIBUTE_MCK).getNameFormat();
                will(returnValue(ATTRIBUTE_NAME_FORMAT));
                one(ATTRIBUTE_MCK).getFriendlyName();
                will(returnValue(ATTRIBUTE_FRIENDLY_NAME));
                ignoring(ATTRIBUTE_MCK);
            }
        });
    }

    /**
     * This method add a SAML AuthnStatement XMLObject to the Assertion.
     */
    public static void loadSamlAuthnStatement() throws SamlException {

        xmlObjectsList.add(AUTHNSTATEMENT_MCK);

        mockery.checking(new Expectations() {
            {
                one(AUTHNSTATEMENT_MCK).getElementQName();
                will(returnValue(AUTHNSTATEMENT_QNAME));
                one(AUTHNSTATEMENT_MCK).getAuthnInstant();
                will(returnValue(AUTHNSTATEMENT_AUTHN_INSTANT));
                atLeast(2).of(AUTHNSTATEMENT_MCK).getAuthnContext();
                will(returnValue(AUTHNCONTEXT_MCK));

                one(AUTHNCONTEXT_MCK).getAuthnContextClassRef();
                will(returnValue(AUTHNCONTEXTCLASSREF_MCK));
                one(AUTHNSTATEMENT_MCK).getAuthnContext();
                will(returnValue(AUTHNCONTEXT_MCK));
                one(AUTHNCONTEXT_MCK).getAuthnContextClassRef();
                will(returnValue(AUTHNCONTEXTCLASSREF_MCK));
                one(AUTHNCONTEXTCLASSREF_MCK).getAuthnContextClassRef();
                will(returnValue(AUTHNCONTEXT_CLASSREF));

                one(AUTHNSTATEMENT_MCK).getSubjectLocality();
                will(returnValue(SUBJECTLOCALITY_MCK));
                one(SUBJECTLOCALITY_MCK).getDNSName();
                will(returnValue(SUBJECTLOCALITY_DNS_NAME));
                one(SUBJECTLOCALITY_MCK).getAddress();
                will(returnValue(SUBJECTLOCALITY_ADDRESS));

                one(AUTHNSTATEMENT_MCK).getSessionNotOnOrAfter();
                will(returnValue(AUTHNSTATEMENT_SESSION_NOT_ON_OR_AFTER));
                allowing(AUTHNSTATEMENT_MCK).getSessionIndex();
                will(returnValue(AUTHNSTATEMENT_SESSION_INDEX));
            }
        });
    }

    /**
     * This method add a SAML Condition XMLObject to the Assertion.
     */
    public static void loadSamlConditions() throws SamlException {

        xmlObjectsList.add(CONDITIONS_MCK);

        mockery.checking(new Expectations() {
            {
                one(CONDITIONS_MCK).getElementQName();
                will(returnValue(CONDITIONS_QNAME));
                one(CONDITIONS_MCK).getNotOnOrAfter();
                will(returnValue(CONDITIONS_NOT_ON_OR_AFTER));
                one(CONDITIONS_MCK).getOneTimeUse();
                will(returnValue(ONETIMEUSE_MCK));
                one(CONDITIONS_MCK).getProxyRestriction();
                will(returnValue(PROXYRESTRICTION_MCK));
                one(PROXYRESTRICTION_MCK).getProxyCount();
                will(returnValue(PROXYRESTRICTION_PROXY_COUNT));

                audienceList.add(AUDIENCE_MCK);

                one(PROXYRESTRICTION_MCK).getAudiences();
                will(returnValue(audienceList));
                one(AUDIENCE_MCK).getAudienceURI();
                will(returnValue(AUDIENCE_URI));

                audienceRestrictionList.add(AUDIENCERESTRICTION_MCK);

                one(CONDITIONS_MCK).getAudienceRestrictions();
                will(returnValue(audienceRestrictionList));
                one(AUDIENCERESTRICTION_MCK).getAudiences();
                will(returnValue(audienceList));
                atLeast(2).of(AUDIENCE_MCK).getAudienceURI();
                will(returnValue(AUDIENCE_URI));

            }
        });
    }

    /**
     * This method add a SAML Issuer XMLObject to the Assertion.
     */
    public static void loadSamlIssuer() throws SamlException {

        xmlObjectsList.add(ISSUER_MCK);

        mockery.checking(new Expectations() {
            {
                one(ISSUER_MCK).getElementQName();
                will(returnValue(ISSUER_QNAME));
                one(ISSUER_MCK).getValue();
                will(returnValue(ISSUER_VALUE));
                one(ISSUER_MCK).getFormat();
                will(returnValue(ISSUER_FORMAT));
            }
        });

    }

    /**
     * This method add a null XMLObject to the Assertion.
     */
    public static void loadSamlNullXmlObject() throws SamlException {
        xmlObjectsList.add(null);
    }

    /**
     * This method add a SAML Signature XMLObject to the Assertion.
     */
    public static void loadSamlSignature() throws SamlException {

        xmlObjectsList.add(SIGNATURE_MCK);

        mockery.checking(new Expectations() {
            {
                one(SIGNATURE_MCK).getElementQName();
                will(returnValue(SIGNATURE_QNAME));
                one(SIGNATURE_MCK).getKeyInfo();
                will(returnValue(KEYINFO_MCK));

                x509DataList.add(X509DATA_MCK);

                one(KEYINFO_MCK).getX509Datas();
                will(returnValue(x509DataList));

                x509CertificatesList.add(X509CERTIFICATE_MCK);

                one(X509DATA_MCK).getX509Certificates();
                will(returnValue(x509CertificatesList));
                one(X509CERTIFICATE_MCK).getValue();
                will(returnValue(SIGNATURE_X509CERTIFICATE_VALUE));

            }
        });
    }

    /**
     * This method add a SAML Signature XMLObject with null KeyInfo to the
     * Assertion.
     */
    public static void loadSamlSignatureWithNullKeyInfo() throws SamlException {

        xmlObjectsList.add(SIGNATURE_MCK);

        mockery.checking(new Expectations() {
            {
                one(SIGNATURE_MCK).getElementQName();
                will(returnValue(SIGNATURE_QNAME));
                one(SIGNATURE_MCK).getKeyInfo();
                will(returnValue(null));
            }
        });
    }

    /**
     * This method add a SAML Subject XMLObject to the Assertion.
     */
    public static void loadSamlSubject() throws SamlException {

        xmlObjectsList.add(SUBJECT_MCK);

        mockery.checking(new Expectations() {
            {
                one(SUBJECT_MCK).getElementQName();
                will(returnValue(SUBJECT_QNAME));

                SUBJECT_NAME_ID.setValue(SUBJECT_NAME_ID_VALUE);
                SUBJECT_NAME_ID.setFormat(SUBJECT_NAME_ID_FORMAT);

                one(SUBJECT_MCK).getNameID();
                will(returnValue(SUBJECT_NAME_ID));

                subjectConfirmationList.add(SUBJECTCONFIRMATION_MCK);

                one(SUBJECT_MCK).getSubjectConfirmations();
                will(returnValue(subjectConfirmationList));
                one(SUBJECTCONFIRMATION_MCK).getMethod();
                will(returnValue(SUBJECT_CONFIRMATION_METHOD));
            }
        });
    }

    /**
     * This method add a SAML Subject XMLObject with null NameId to the
     * Assertion.
     */
    public static void loadSamlSubjectWithNullNameId() throws SamlException {

        xmlObjectsList.add(SUBJECT_MCK);

        mockery.checking(new Expectations() {
            {
                one(SUBJECT_MCK).getElementQName();
                will(returnValue(SUBJECT_QNAME));

                one(SUBJECT_MCK).getNameID();
                will(returnValue(null));
            }
        });
    }

    /**
     * This method add an unhandled XMLObject to the Assertion.
     */
    public static void loadUnhandledXmlObject() throws SamlException {

        xmlObjectsList.add(ISSUER_MCK);

        mockery.checking(new Expectations() {
            {
                one(ISSUER_MCK).getElementQName();
                will(returnValue(NO_SUPPORTED_QNAME_IN_XMLOBJECT));
            }
        });
    }

    /**
     * This method is used to initialize the test, it creates a new
     * Saml20TokenImpl object and add necessary expectations for the object. If
     * this method fails its execution then nothing else should be execute it
     * because it creates a common object for almost others test.
     */
    @BeforeClass
    public static void setUpClass() throws SamlException, SAXException, IOException, ParserConfigurationException {
        final Set<Namespace> namespaces = new HashSet<Namespace>();
        mockery.checking(new Expectations() {
            {
                allowing(ASSERTION_MCK).getNamespaceManager();//
                will(returnValue(nsManager));//
                allowing(nsManager).getAllNamespacesInSubtreeScope();//
                will(returnValue(namespaces));//
            }
        });

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        documentBuilder = documentBuilderFactory.newDocumentBuilder();

        InputStream in = new ByteArrayInputStream(DOM_ELEMENT.getBytes());

        Document document = documentBuilder.parse(in);
        final Element element = document.getDocumentElement();

        mockery.checking(new Expectations() {
            {
                one(ASSERTION_MCK).getDOM();
                will(returnValue(element));
                one(ASSERTION_MCK).detach();
                one(ASSERTION_MCK).getID();
                will(returnValue(SAML_ID));
                one(ASSERTION_MCK).getElementQName();
                will(returnValue(SAML_QNAME));
                one(ASSERTION_MCK).getIssueInstant();
                will(returnValue(SAML_ISSUE_INSTANT));
                one(ASSERTION_MCK).getOrderedChildren();
                will(returnValue(xmlObjectsList));
            }
        });

        loadSamlAttributeStatement();
        loadSamlAuthnStatement();
        loadSamlConditions();
        loadSamlIssuer();
        loadSamlSignature();
        loadSamlSubject();

        loadSamlSubjectWithNullNameId();
        loadSamlSignatureWithNullKeyInfo();
        loadSamlNullXmlObject();
        loadUnhandledXmlObject();

        samlToken = new Saml20TokenImpl(ASSERTION_MCK, PROVIDER_ID);

        samlToken.holderOfKeyBytes = EXPECTED_HOLDER_OF_KEY_BYTES;
    }

    /**
     * This method test that getAssertionQName can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as provided when
     * the object is created.
     */
    @Test
    public void getAssertionQNameShouldReturnExpectedValue() {
        Assert.assertEquals(SAML_QNAME, samlToken.getAssertionQName());
    }

    /**
     * This method test that getAudienceRestriction can be retrieved from a
     * Saml20TokenImpl object and fails if the first object from the list is not
     * the same as the specified when the object is created.
     */
    @Test
    public void getAudienceRestrictionShouldReturnExpectedValue() {
        Assert.assertEquals(1, samlToken.getAudienceRestriction().size());
        Assert.assertEquals(AUDIENCE_URI, samlToken.getAudienceRestriction().get(0));
    }

    /**
     * This method test that getAuthenticationInstant can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the specified
     * when the object is created.
     */
    @Test
    public void getAuthenticationInstantShouldReturnExpectedValue() {
        Assert.assertEquals(AUTHNSTATEMENT_AUTHN_INSTANT.toDate(),
                            samlToken.getAuthenticationInstant());
    }

    /**
     * This method test that getAuthenticationMethod can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the specified
     * when the object is created.
     */
    @Test
    public void getAuthenticationMethodShouldReturnExpectedValue() {
        Assert.assertEquals(AUTHNCONTEXT_CLASSREF,
                            samlToken.getAuthenticationMethod());
    }

    /**
     * This method test that getConfirmationMethod can be retrieved from a
     * Saml20TokenImpl object and fails if the first object from the list is not
     * the same as the specified when the object is created.
     */
    @Test
    public void getConfirmationMethodShouldReturnExpectedValue() {
        Assert.assertEquals(1, samlToken.getConfirmationMethod().size());
        Assert.assertEquals(SUBJECT_CONFIRMATION_METHOD, samlToken.getConfirmationMethod().get(0));
    }

    /**
     * This method test that getHolderOfKeyBytes can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the specified
     * when the object is created.
     */
    @Test
    public void getHolderOfKeyBytesShouldReturnExpectedValue() {
        Assert.assertArrayEquals(EXPECTED_HOLDER_OF_KEY_BYTES,
                                 samlToken.getHolderOfKeyBytes());
    }

    /**
     * This method test that getHolderOfKeyBytes should retrieve a null object
     * when holderOfKeyBytes is null.
     */
    @Test
    public void getHolderOfKeyBytesShouldReturnNull() {
        byte[] tmpArray = samlToken.holderOfKeyBytes;
        samlToken.holderOfKeyBytes = null;

        Assert.assertNull(samlToken.getHolderOfKeyBytes());

        samlToken.holderOfKeyBytes = tmpArray;
    }

    /**
     * This method test that getIssueInstant can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the specified
     * when the object is created.
     */
    @Test
    public void getIssueInstantShouldReturnExpectedValue() {
        Assert.assertEquals(SAML_ISSUE_INSTANT.toDate(),
                            samlToken.getIssueInstant());
    }

    /**
     * This method test that getProxyRestrictionAudience can be retrieved from a
     * Saml20TokenImpl object and fails if the first object from the list is not
     * the same as the specified when the object is created.
     */
    @Test
    public void getProxyRestrictionAudienceShouldReturnExpectedValue() {
        Assert.assertEquals(1, samlToken.getProxyRestrictionAudience().size());
        Assert.assertEquals(AUDIENCE_URI, samlToken.getProxyRestrictionAudience().get(0));
    }

    /**
     * This method test that getProxyRestrictionCount can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the specified
     * when the object is created.
     */
    @Test
    public void getProxyRestrictionCountShouldReturnExpectedValue() {
        Assert.assertEquals(PROXYRESTRICTION_PROXY_COUNT,
                            samlToken.getProxyRestrictionCount());
    }

    /**
     * This method test that getSAMLAsString can parse the xml representation of
     * the Assertion that is provided when the object is created. The test fails
     * if the representation is not the equal node as the provided when the
     * object is created.
     */
    @Test
    public void getSAMLAsStringShouldReturnExpectedValue() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        documentBuilder = documentBuilderFactory.newDocumentBuilder();

        InputStream inputStream = new ByteArrayInputStream(samlToken.getSAMLAsString().getBytes());
        Document actualDocument = documentBuilder.parse(inputStream);

        inputStream = new ByteArrayInputStream(DOM_ELEMENT.getBytes());
        Document expectedDocument = documentBuilder.parse(inputStream);

        Assert.assertTrue(expectedDocument.isEqualNode(actualDocument));
    }

    /**
     * This method test that getSAMLAttributes can be retrieved from a
     * Saml20TokenImpl object and fails if they are not the same as the
     * specified when the object is created.
     */
    @Test
    public void getSAMLAttributesShouldReturnExpectedValue() {
        Assert.assertEquals(1, samlToken.getSAMLAttributes().size());
        Assert.assertEquals(ATTRIBUTE_FRIENDLY_NAME, samlToken.getSAMLAttributes().get(0).getFriendlyName());
        Assert.assertEquals(ATTRIBUTE_NAME, samlToken.getSAMLAttributes().get(0).getName());
        Assert.assertEquals(ATTRIBUTE_NAME_FORMAT, samlToken.getSAMLAttributes().get(0).getNameFormat());
    }

    /**
     * This method test that getSamlExpires can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the specified
     * when the object is created.
     */
    @Test
    public void getSamlExpiresShouldReturnExpectedValue() {
        Assert.assertEquals(CONDITIONS_NOT_ON_OR_AFTER.toDate(),
                            samlToken.getSamlExpires());
    }

    /**
     * This method test that getSamlID can be retrieved from a Saml20TokenImpl
     * object and fails if it's not the same as the specified when the object is
     * created.
     */
    @Test
    public void getSamlIDShouldReturnExpectedValue() {
        Assert.assertEquals(SAML_ID, samlToken.getSamlID());
    }

    /**
     * This method test that getSAMLIssuerNameFormat can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the specified
     * when the object is created.
     */
    @Test
    public void getSAMLIssuerNameFormatShouldReturnExpectedValue() throws SamlException {
        Assert.assertEquals(ISSUER_FORMAT, samlToken.getSAMLIssuerNameFormat());
    }

    /**
     * This method test that getSAMLIssuerName can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the specified
     * when the object is created.
     */
    @Test
    public void getSAMLIssuerNameShouldReturnExpectedValue() {
        Assert.assertEquals(ISSUER_VALUE, samlToken.getSAMLIssuerName());
    }

    /**
     * This method test that getSAMLNameIDFormat can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the specified
     * when the object is created.
     */
    @Test
    public void getSAMLNameIDFormatShouldReturnExpectedValue() {
        Assert.assertEquals(SUBJECT_NAME_ID_FORMAT,
                            samlToken.getSAMLNameIDFormat());
    }

    /**
     * This method test that getSAMLNameID can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the specified
     * when the object is created.
     */
    @Test
    public void getSAMLNameIDShouldReturnExpectedValue() {
        Assert.assertEquals(SUBJECT_NAME_ID_VALUE, samlToken.getSAMLNameID());
    }

    /**
     * This method test that getServiceProviderID can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the specified
     * when the object is created.
     */
    @Test
    public void getServiceProviderIDShouldReturnExpectedValue() {
        Assert.assertEquals(PROVIDER_ID, samlToken.getServiceProviderID());
    }

    /**
     * This method test that getSessionNotOnOrAfter can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the specified
     * when the object is created.
     */
    @Test
    public void getSessionNotOnOrAfterShouldReturnExpectedValue() {
        Assert.assertEquals(AUTHNSTATEMENT_SESSION_NOT_ON_OR_AFTER.getMillis(),
                            samlToken.getSessionNotOnOrAfter());
    }

    /**
     * This method test that getSignerCertificate can be retrieved from a
     * Saml20TokenImpl object and fails if the first object from the list is not
     * the same as the specified when the object is created.
     */
    @Test
    public void getSignerCertificateShouldReturnExpectedValue() throws CertificateException {

        java.security.cert.X509Certificate x509Cert = null;
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            byte[] certbytes = Base64.decode(SIGNATURE_X509CERTIFICATE_VALUE);
            ByteArrayInputStream bais = new ByteArrayInputStream(certbytes);
            x509Cert = (java.security.cert.X509Certificate) certFactory.generateCertificate(bais);
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(1, samlToken.getSignerCertificate().size());
        Assert.assertEquals(x509Cert, samlToken.getSignerCertificate().get(0));
    }

    /**
     * This method test that getSubjectDNS can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the specified
     * when the object is created.
     */
    @Test
    public void getSubjectDNSShouldReturnExpectedValue() {
        Assert.assertEquals(SUBJECTLOCALITY_DNS_NAME, samlToken.getSubjectDNS());
    }

    /**
     * This method test that getSubjectIPAddress can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the specified
     * when the object is created.
     */
    @Test
    public void getSubjectIPAddressShouldReturnExpectedValue() {
        Assert.assertEquals(SUBJECTLOCALITY_ADDRESS,
                            samlToken.getSubjectIPAddress());
    }

    /**
     * This method test that handleSamlSignature throws a SamlException while
     * trying to generate a certificate.
     */
    @Test(expected = SamlException.class)
    public void handleSamlSignatureShouldThrowSamlExceptionWhileTryingToGenerateACertificateFromABadInput() throws SamlException {

        final List<XMLObject> testXmlObjectList = new ArrayList<XMLObject>();

        mockery.checking(new Expectations() {
            {
                one(ASSERTION_MCK).getDOM();
                will(returnValue(ELEMENT_MCK));
                one(ELEMENT_MCK).cloneNode(true);//
                will(returnValue(ELEMENT_MCK));//
                one(ELEMENT_MCK).getOwnerDocument();
                will(returnValue(DOCUMENT_MCK));
                one(DOCUMENT_MCK).getImplementation();
                will(returnValue(DOMIMPLEMENTATION_MCK));
                one(DOMIMPLEMENTATION_MCK).getFeature("LS", "3.0");
                will(returnValue(DOMIMPLEMENTATIONLS_MCK));
                ignoring(DOMIMPLEMENTATIONLS_MCK);
                one(ASSERTION_MCK).detach();
                one(ASSERTION_MCK).getID();
                will(returnValue(SAML_ID));
                one(ASSERTION_MCK).getElementQName();
                will(returnValue(SAML_QNAME));
                one(ASSERTION_MCK).getIssueInstant();
                will(returnValue(SAML_ISSUE_INSTANT));
                one(ASSERTION_MCK).getOrderedChildren();
                will(returnValue(testXmlObjectList));
            }
        });

        testXmlObjectList.add(SIGNATURE_MCK);

        mockery.checking(new Expectations() {
            {
                one(SIGNATURE_MCK).getElementQName();
                will(returnValue(SIGNATURE_QNAME));
                one(SIGNATURE_MCK).getKeyInfo();
                will(returnValue(KEYINFO_MCK));

                x509DataList.add(X509DATA_MCK);

                one(KEYINFO_MCK).getX509Datas();
                will(returnValue(x509DataList));

                x509CertificatesList.add(X509CERTIFICATE_MCK);

                one(X509DATA_MCK).getX509Certificates();
                will(returnValue(x509CertificatesList));
                one(X509CERTIFICATE_MCK).getValue();
                will(returnValue("wrongCertificate"));

            }
        });

        new Saml20TokenImpl(ASSERTION_MCK, PROVIDER_ID);
    }

    /**
     * This method test that hasProxyRestriction can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the expected.
     */
    @Test
    public void hasProxyRestrictionShouldReturnExpectedValue() {
        Assert.assertFalse(samlToken.hasProxyRestriction());
    }

    /**
     * This method test that isOneTimeUse can be retrieved from a
     * Saml20TokenImpl object and fails if it's not the same as the expected.
     */
    @Test
    public void isOneTimeUseShouldReturnExpectedValue() {
        Assert.assertTrue(samlToken.isOneTimeUse());
    }

    /**
     * This method test that toString contains the string representation of a
     * Saml20TokenImpl object and fails if it's not the same as the expected.
     */
    @Test
    public void toStringShouldReturnExpectedValue() {
        java.security.cert.X509Certificate x509Cert = null;
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            byte[] certbytes = Base64.decode(SIGNATURE_X509CERTIFICATE_VALUE);
            ByteArrayInputStream bais = new ByteArrayInputStream(certbytes);
            x509Cert = (java.security.cert.X509Certificate) certFactory.generateCertificate(bais);
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        String holderOfKey = "";
        try {
            holderOfKey = new String(EXPECTED_HOLDER_OF_KEY_BYTES, Constants.UTF8);
        } catch (Exception e) {
        }

        String expected = "Saml20Token\n samlID:" + SAML_ID
                          + "\n assertionQName:" + SAML_QNAME + "\n samlExpires:"
                          + CONDITIONS_NOT_ON_OR_AFTER.toDate() + "\n samlCreated:"
                          + SAML_ISSUE_INSTANT.toDate() + "\n confirmationMethod:["
                          + SUBJECT_CONFIRMATION_METHOD + "]" + "\n holderOfKeyBytes:"
                          + holderOfKey + "\n SAMLIssuerName:" + ISSUER_VALUE
                          + "\n authenticationMethod:" + AUTHNCONTEXT_CLASSREF
                          + "\n authenticationInstant:"
                          + AUTHNSTATEMENT_AUTHN_INSTANT.toDate() + "\n subjectDNS:"
                          + SUBJECTLOCALITY_DNS_NAME + "\n subjectIPAddress:"
                          + SUBJECTLOCALITY_ADDRESS + "\n audienceRestriction:["
                          + AUDIENCE_URI + "]" + "\n oneTimeUse:" + true
                          + "\n proxyRestriction:" + false + "\n proxyRestrictionCount:"
                          + PROXYRESTRICTION_PROXY_COUNT
                          + "\n proxyRestrictionAudience:[" + AUDIENCE_URI + "]"
                          + "\n signerCertificate:[" + x509Cert.getSubjectDN().getName()
                          + "]";

        Assert.assertEquals(expected, samlToken.toString());
    }

}

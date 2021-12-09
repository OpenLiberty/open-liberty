/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
import java.io.Serializable;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.joda.time.DateTime;
import org.opensaml.core.xml.Namespace;
import org.opensaml.core.xml.NamespaceManager;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.OneTimeUse;
import org.opensaml.saml.saml2.core.ProxyRestriction;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectLocality;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Data;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.saml2.Saml20Attribute;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;
import com.ibm.ws.security.saml.sso20.metadata.TraceConstants;

import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.xml.NamespaceSupport;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

public class Saml20TokenImpl implements Saml20Token, Serializable {
    /** */
    private static final long serialVersionUID = -862850937499495719L;
    private transient static TraceComponent tc = Tr.register(Saml20TokenImpl.class,
                                                             TraceConstants.TRACE_GROUP,
                                                             TraceConstants.MESSAGE_BUNDLE);
    transient Element assertionDOM; // this for the convenience of wssec samlToken
    transient XMLObject samlXMLObject = null;

    String samlString = null;
    String samlID;
    QName assertionQName;
    Date samlExpires;
    Date samlCreated;
    byte[] holderOfKeyBytes;
    String SAMLIssuerName;
    String issuerNameFormat = null;
    String authenticationMethod;
    Date authenticationInstant;
    String sessionIndex = null;
    String subjectDNS;
    String subjectIPAddress;
    boolean oneTimeUse = false;
    boolean proxyRestriction = false;
    long proxyRestrictionCount = 0;
    String nameId;
    String nameIdFormat;
    List<String> proxyRestrictionAudience = new ArrayList<String>();
    List<X509Certificate> signerCertificates = new ArrayList<X509Certificate>();
    List<String> signerCertificateDN = new ArrayList<String>();
    List<String> confirmationMethod = new ArrayList<String>();
    List<String> audienceRestriction = new ArrayList<String>();
    List<Saml20Attribute> attributes = new ArrayList<Saml20Attribute>();
    String providerId;
    long lSessionNotOnOrAfter = 0L; // Gets the milliseconds of the datetime instant from the Java epoch of 1970-01-01T00:00:00Z.
    HashMap<String, Object> maps = new HashMap<String, Object>();
    final static String samlElement = "samlElement";
    final static String SINDEX = "sessionIndex";
    final static String NAMEID = "NameID";
    final static String dsUri = "http://www.w3.org/2000/09/xmldsig#";

    public Saml20TokenImpl(Assertion assertion, String providerId) throws SamlException {
        this.providerId = providerId;
        init(assertion);
    }

    public Saml20TokenImpl(Assertion assertion) throws SamlException {
        init(assertion);
    }

    void init(Assertion assertion) throws SamlException {
        this.assertionDOM = getClonedAssertionDom(assertion);
        this.maps.put(samlElement, this.assertionDOM);
        //this.samlString = XMLHelper.nodeToString(this.assertionDOM);
        this.samlString = SerializeSupport.nodeToString(this.assertionDOM);//v3
        //  no need to check the validation of assertion (isSigned) at this point.
        // It had been verified already

        handleSamlAssertion(assertion);
        List<XMLObject> children = assertion.getOrderedChildren();
        Iterator<XMLObject> xmlObjects = children.iterator();
        while (xmlObjects.hasNext()) {
            XMLObject xmlObject = xmlObjects.next();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "XMLObject : " + xmlObject);
            }
            if (xmlObject == null) {
                // Strange enough to have null here
                continue;
            }
            QName qName = xmlObject.getElementQName();
            String localPart = qName.getLocalPart();
            String nsUri = qName.getNamespaceURI();
            if (localPart.equals(Constants.LOCAL_NAME_Issuer)) {
                handleSamlIssuer((Issuer) xmlObject);
            } else if (localPart.equals(Constants.LOCAL_NAME_Signature)) {
                handleSamlSignature((Signature) xmlObject);
            } else if (localPart.equals(Constants.LOCAL_NAME_Subject)) {
                handleSamlSubject((Subject) xmlObject);
            } else if (localPart.equals(Constants.LOCAL_NAME_Conditions)) {
                handleSamlConditions((Conditions) xmlObject);
            } else if (localPart.equals(Constants.LOCAL_NAME_AuthnStatement)) {
                handleSamlAuthnStatement((AuthnStatement) xmlObject);
            } else if (localPart.equals(Constants.LOCAL_NAME_AttributeStatement)) {
                handleSamlAttributeStatement((AttributeStatement) xmlObject);
            } else {
                // error handling
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unhandle XMLObject: " + localPart + "(" + nsUri + ")");
                }
                // Tr.warning(tc, "SAML20_UNHANDLED_XMLOBJECT", new Object[] { localPart, nsUri });
            }
        }
    }

    /**
     * @param xmlObject
     */
    private void handleSamlAuthnStatement(AuthnStatement authnStatement) throws SamlException {
        this.authenticationInstant = authnStatement.getAuthnInstant().toDate();
        this.sessionIndex = authnStatement.getSessionIndex();
        this.maps.put(SINDEX, this.sessionIndex);
        if (authnStatement.getAuthnContext() != null) {
            if (authnStatement.getAuthnContext().getAuthnContextClassRef() != null) {
                this.authenticationMethod = authnStatement.getAuthnContext().getAuthnContextClassRef().getAuthnContextClassRef();
            }
        }

        SubjectLocality locality = authnStatement.getSubjectLocality();
        if (locality != null) {
            this.subjectDNS = locality.getDNSName();
            this.subjectIPAddress = locality.getAddress();
        }

        DateTime sessionNotOnOrAfter = authnStatement.getSessionNotOnOrAfter();
        if (sessionNotOnOrAfter != null) {
            lSessionNotOnOrAfter = sessionNotOnOrAfter.getMillis();
        }
    }

    /**
     * Example:
     * <saml:Conditions NotBefore="2014-11-20T21:09:34Z"
     * --NotOnOrAfter="2014-11-20T21:11:34Z">
     * --<saml:AudienceRestriction>
     * ----<saml:Audience>
     * ------https://localhost:8020/ibm/saml20/sp/acs
     * ----</saml:Audience>
     * --</saml:AudienceRestriction>
     * </saml:Conditions>
     *
     * @param xmlObject
     * @throws SamlException
     */
    private void handleSamlConditions(Conditions conditions) throws SamlException {
        this.samlExpires = conditions.getNotOnOrAfter().toDate();
        OneTimeUse onetimeuse = conditions.getOneTimeUse();
        if (onetimeuse != null) {
            this.oneTimeUse = true;
        }
        ProxyRestriction proxyRestriction = conditions.getProxyRestriction();
        if (proxyRestriction != null) {
            Integer count = proxyRestriction.getProxyCount();
            if (count != null) {
                this.proxyRestrictionCount = count.intValue();
            }
            List<Audience> audiences = proxyRestriction.getAudiences();
            Iterator<Audience> it = audiences.iterator();
            while (it.hasNext()) {
                Audience audience = it.next();
                this.proxyRestrictionAudience.add(audience.getAudienceURI());
            }
        }

        List<AudienceRestriction> audienceRestrictions = conditions.getAudienceRestrictions();
        Iterator<AudienceRestriction> it = audienceRestrictions.iterator();
        while (it.hasNext()) {
            AudienceRestriction audienceRestriction = it.next();
            List<Audience> audiences = audienceRestriction.getAudiences();
            Iterator<Audience> audienceIt = audiences.iterator();
            while (audienceIt.hasNext()) {
                Audience audience = audienceIt.next();
                if (audience.getAudienceURI() != null) {
                    this.audienceRestriction.add(audience.getAudienceURI());
                }
            }
        }
    }

    /**
     * Example:
     * <saml:Subject>
     * --<saml:NameID Format="urn:ibm:names:ITFIM:5.1:accessmanager">
     * ----user2
     * --</saml:NameID>
     * --<saml:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
     * ----<saml:SubjectConfirmationData NotOnOrAfter="2014-11-20T21:11:34Z"
     * ------Recipient="https://localhost:8020/ibm/saml20/sp/acs"/>
     * --</saml:SubjectConfirmation>
     * </saml:Subject>
     *
     * @param xmlObject
     */
    private void handleSamlSubject(Subject subject) {

        NameID nameID = subject.getNameID();
        if (nameID == null)
            return;

        this.nameId = nameID.getValue();
        this.nameIdFormat = nameID.getFormat();
        List<SubjectConfirmation> subjectConfirmations = subject.getSubjectConfirmations();
        Iterator<SubjectConfirmation> it = subjectConfirmations.iterator();
        while (it.hasNext()) {
            SubjectConfirmation confirmation = it.next();
            this.confirmationMethod.add(confirmation.getMethod());
        }
        this.maps.put(NAMEID, nameID);

    }

    private void handleSamlAttributeStatement(AttributeStatement attributeState) {
        List<Attribute> attributes = attributeState.getAttributes();
        Iterator<Attribute> it = attributes.iterator();
        while (it.hasNext()) {
            Attribute attribute = it.next();
            Saml20AttributeImpl saml20Attribute = new Saml20AttributeImpl(attribute);
            this.attributes.add(saml20Attribute);
        }

    }

    /**
     * Example:
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="uuidcf0ad8d2-0149-157c-806f-8976c76e3139">
     * --<ds:SignedInfo>
     * ----<ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
     * ----<ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
     * ----<ds:Reference URI="#Assertion-uuidcf0ad8d1-0149-18b5-8077-8976c76e3139">
     * ------<ds:Transforms>
     * --------<ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
     * --------<ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * --------<xc14n:InclusiveNamespaces xmlns:xc14n="http://www.w3.org/2001/10/xml-exc-c14n#"
     * ----------PrefixList="saml"/>
     * ------</ds:Transform>
     * ----</ds:Transforms>
     * ----<ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * ----<ds:DigestValue>
     * ------V2iLLfPnyut/9GIWTKdHShI+fK4=
     * ----</ds:DigestValue>
     * ----</ds:Reference>
     * --</ds:SignedInfo>
     * --<ds:SignatureValue>
     * ----DFoh7ECOViZkZdvXbcEP95Zhi5gSCLxxqZTjy6XHsYdfYj+MnEjoyXgek1MVdC0NVnIV3tZGE3+Sb3h5CJVrLMfre0VXGg553eW+
     * mDPHwn6zchEM9r7LNk5rr07xTnczQcZc8H5mFBjSnDn7ppRl6tL8x0TsuM18EWqFuYSWeg8=
     * --</ds:SignatureValue>
     * --<ds:KeyInfo>
     * ----<ds:X509Data>
     * ------<ds:X509Certificate>
     * --------MIICBzCCAXCgAwIBAgIEQH26vjANBgkqhkiG9w0BAQQFADBIMQswCQYDVQQGEwJVUzEPMA0GA1UEChMGVGl2b2xpMQ4wDAYDVQQLEwVUQU1lQjEYMBYGA1UEAxMPZmlt
     * -------- ....
     * --------+g3YX+dyGc352TSKO8HvAIBkHHFFwIkzhNgO+zLhxg5UMkOg12X9ucW7leZ1IB0Z6+JXBrXIWmU3UPum+QxmlaE0OG9zhp9LEfzsE5+ff+7XpS0wpJklY6c+cqHj4aTGfOhSE6u7BLdI26cZNdzxdhikBMZPgdyQ==
     * ------</ds:X509Certificate>
     * ----</ds:X509Data>
     * --</ds:KeyInfo>
     * </ds:Signature>
     *
     * @param xmlObject
     */
    private void handleSamlSignature(Signature signature) throws SamlException {

        KeyInfo keyInfo = signature.getKeyInfo();
        if (keyInfo == null)
            return;
        List<X509Data> x509Datas = keyInfo.getX509Datas();
        Iterator<X509Data> it = x509Datas.iterator();
        while (it.hasNext()) {
            X509Data x509data = it.next();
            List<org.opensaml.xmlsec.signature.X509Certificate> certs = x509data.getX509Certificates(); //v3
            Iterator<org.opensaml.xmlsec.signature.X509Certificate> itc = certs.iterator();
            while (itc.hasNext()) {
                org.opensaml.xmlsec.signature.X509Certificate cert = itc.next();
                String certString = cert.getValue();
                try {
                    CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                    byte[] certbytes = Base64Support.decode(certString);
                    ByteArrayInputStream bais = new ByteArrayInputStream(certbytes);
                    X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(bais);
                    this.signerCertificates.add(x509Cert);
                    this.signerCertificateDN.add(x509Cert.getSubjectDN().getName());
                } catch (CertificateException e) {
                    // error handling
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "ERROR: Get an Exception while generate the X509Cerficate", e);
                    }
                    throw new SamlException(e); // Let SamlException handle the unexpected Exception
                }
            }
        }

    }

    /**
     * Example:
     * <saml:Issuer Format="urn:oasis:names:tc:SAML:2.0:nameid-format:entity">
     * --https://wlp-tfimidp1.austin.ibm.com:9443/sps/WlpTfimIdp1/saml20
     * </saml:Issuer>
     *
     * @param xmlObject
     */
    private void handleSamlIssuer(Issuer issuer) {
        SAMLIssuerName = issuer.getValue(); //
        issuerNameFormat = issuer.getFormat();
    }

    @Override
    public String getSAMLIssuerNameFormat() {
        return this.issuerNameFormat;
    }

    /**
     * Example:
     * <saml:Assertion ID="Assertion-uuidcf0ad8d1-0149-18b5-8077-8976c76e3139"
     * --IssueInstant="2014-11-20T21:10:34Z"
     * --Version="2.0">
     *
     * @param xmlAssertion
     */
    private void handleSamlAssertion(Assertion xmlAssertion) throws SamlException {

        this.samlID = xmlAssertion.getID();
        this.assertionQName = xmlAssertion.getElementQName();
        this.samlCreated = xmlAssertion.getIssueInstant().toDate();
    }

    /**
     * Retrieves the identifier associated with this SAML assertion.
     *
     * @return a string representing the ID for SAML 2.0, or AssertionID for SAML 1.1.
     */
    @Override
    public String getSamlID() {
        return samlID;
    };

    /**
     * Return SAML Assertion namespace, defined in a schema SAML-XSD.
     *
     * @return SAML Assertion namespace, defined in a schema SAML-XSD
     *         It is "urn:oasis:names:tc:SAML:1.0:assertion" for SAML 1.1, and
     *         "urn:oasis:names:tc:SAML:2.0:assertion" for SAML 2.0.
     */
    @Override
    public QName getAssertionQName() {
        return SamlUtil.cloneQName(assertionQName);
    };

    /**
     * Return SAML Expiration time.
     *
     * @return SAML Token expiration time, which is delimited by the NotOnOrAfter attribute in <Conditions> element.
     */
    @Override
    public Date getSamlExpires() {
        return (Date) samlExpires.clone();
    };

    /**
     * Retrieves the SAML assertion creation date.
     *
     * @return SAML Token creation Date based on the NotBefore attribute in <Conditions> element.
     *
     */
    @Override
    public Date getIssueInstant() {
        return (Date) samlCreated.clone();
    };

    /**
     * Retrieves the Subject Confirmation Method used in this SAML token.
     * based on the SAML token profile for versions 1.1 and 2.0.
     *
     * @see <a href="http://www.oasis-open.org/committees/download.php/16768/wss-v1.1-spec-os-SAMLTokenProfile.pdf"> OASIS SAML Token Profile 1.1</a>
     * @see <a href="http://docs.oasis-open.org/security/saml/v2.0/saml-profiles-2.0-os.pdf"> OASIS SAML Token Profile 2.0</a>
     *
     * @return SAML SubjectConfirmation Method, and valid method is holder-of-key, bearer, or sender-vouches.
     *         the returned string is based on the OASIS SAML token profile 1.1 and SAML token profile 2.0.
     */
    @Override
    public List<String> getConfirmationMethod() {
        return Collections.unmodifiableList(this.confirmationMethod);
    };

    /**
     * Retrieves the key bytes from the Holder-of-Key Element of this SAML token.
     *
     * @return the shared secret key bytes for a symmetric holder-of-key assertion.
     */
    @Override
    public byte[] getHolderOfKeyBytes() {
        return holderOfKeyBytes == null ? null : holderOfKeyBytes.clone();
    };

    /**
     * Retrieves the name of NameID.
     *
     * @return NameID in the SAML assertion.
     */
    @Override
    public String getSAMLNameID() {
        return this.nameId;
    }

    @Override
    public String getSAMLNameIDFormat() {
        return this.nameIdFormat;
    }

    /**
     * Retrieves the name of issuer.
     *
     * @return issuer name of the SAML authority responsible for the claims in the SAML assertion.
     */
    @Override
    public String getSAMLIssuerName() {
        return SAMLIssuerName;
    };

    /**
     * Retrieves the authentication method that was used to authenticate the token holder.
     *
     * @return the authentication method that took place prior to the token's creation.
     *         For example "password", "kerberos", "ltpa".
     */
    @Override
    public String getAuthenticationMethod() {
        return authenticationMethod;
    };

    /**
     * Retrieves the authentication time when the token holder is authenticated.
     *
     * @return the authentication time when the token holder is authenticated.
     */
    @Override
    public Date getAuthenticationInstant() {
        return (Date) authenticationInstant.clone();
    };

    /**
     * Retrieves DNSAddress in SubjectLocality.
     *
     * @return DNSAddress in SubjectLocality.
     */
    @Override
    public String getSubjectDNS() {
        return subjectDNS;
    };

    /**
     * Retrieves IPAddress in SubjectLocality.
     *
     * @return IPAddress in SubjectLocality.
     */
    @Override
    public String getSubjectIPAddress() {
        return subjectIPAddress;
    };

    /**
     * Retrieves AudienceRestriction String name list.
     *
     * @return AudienceRestriction String name list.
     */
    @Override
    public List<String> getAudienceRestriction() {
        return Collections.unmodifiableList(audienceRestriction);
    };

    /**
     * Retrieves flag to indicate OneTimeUse or DoNotCacheCondition.
     *
     * @return flag to indicate OneTimeUse or DoNotCacheCondition.
     */
    @Override
    public boolean isOneTimeUse() {
        return oneTimeUse;
    };

    /**
     * Retrieves flag to indicate ProxyRestriction.
     *
     * @return flag to indicate ProxyRestriction.
     */
    @Override
    public boolean hasProxyRestriction() {
        return proxyRestriction;
    };

    /**
     * Retrieves number of ProxyRestriction Count.
     *
     * @return number of ProxyRestriction Count.
     */
    @Override
    public long getProxyRestrictionCount() {
        return proxyRestrictionCount;
    };

    /**
     * Retrieves String list of ProxyRestriction Audience.
     *
     * @return String list of ProxyRestriction Audience.
     */
    @Override
    public List<String> getProxyRestrictionAudience() {
        return Collections.unmodifiableList(this.proxyRestrictionAudience);
    };

    /**
     * Retrieves SAML signer's X.509 Certificate
     *
     * @return SAML signer's X.509 Certificate
     */
    @Override
    public List<X509Certificate> getSignerCertificate() {
        return Collections.unmodifiableList(this.signerCertificates);
    };


    /**
     * Gets the serializable representation of this SAML XML.
     *
     * @return the String representation of this SAML
     */
    @Override
    public String getSAMLAsString() {
        return formatSamlString(this.samlString);
        //return this.samlString;
    }

    /**
     * @param saml
     */
    private String formatSamlString(String saml) {
        // TODO Auto-generated method stub
        String samlString = saml;
        //String line_separator = System.getProperty("line.separator");
        if (saml != null) {
            //On Mac and Sun JDK, we see that these two lines are separated by new line
            //<?xml version="1.0" encoding="UTF-8"?>
            //<saml:Assertion ID="Assertion-uuid425e1163-0153-1711-9985-95bb9b289022" IssueInstant=......
            String[] lines = saml.split("\r\n|\r|\n");

            if (lines.length == 2 && lines[0].startsWith("<?") && lines[0].endsWith("?>")) {
                samlString = lines[0].concat(lines[1]);
            }
        }
        return samlString;
    }

    @Override
    public List<Saml20Attribute> getSAMLAttributes() {
        return Collections.unmodifiableList(this.attributes);
    }

    @Override
    public String toString() {
        String holderOfKey = "";
        try {
            holderOfKey = new String(holderOfKeyBytes, Constants.UTF8);
        } catch (Exception e) {

        }
        String result = "Saml20Token\n samlID:" + samlID +
                        "\n assertionQName:" + assertionQName +
                        "\n samlExpires:" + samlExpires +
                        "\n samlCreated:" + samlCreated +
                        "\n confirmationMethod:" + confirmationMethod +
                        "\n holderOfKeyBytes:" + holderOfKey +
                        "\n SAMLIssuerName:" + SAMLIssuerName +
                        "\n authenticationMethod:" + authenticationMethod +
                        "\n authenticationInstant:" + authenticationInstant +
                        "\n subjectDNS:" + subjectDNS +
                        "\n subjectIPAddress:" + subjectIPAddress +
                        "\n audienceRestriction:" + audienceRestriction +
                        "\n oneTimeUse:" + oneTimeUse +
                        "\n proxyRestriction:" + proxyRestriction +
                        "\n proxyRestrictionCount:" + proxyRestrictionCount +
                        "\n proxyRestrictionAudience:" + proxyRestrictionAudience +
                        // "\n attributes: ommitted" + attributes +
                        "\n signerCertificate:" + signerCertificateDN;
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.security.saml.token.Saml20Token#getServiceProviderlID()
     */
    @Override
    public String getServiceProviderID() {
        return providerId;
    }

    // This is only in use when disableLtpaToken is true
    // return the milliseconds of the datetime instant from the Java epoch of 1970-01-01T00:00:00Z.
    public long getSessionNotOnOrAfter() {
        return lSessionNotOnOrAfter; // new DateTime(lSessionNotOnOrAfter)
    }

    /**
     * Gets customized properties.
     *
     * @return the Map of properties
     */
    @Override
    public Map<String, Object> getProperties() {
        return maps;
    };

    /**
     * @param assertion
     * @return
     */
    Element getClonedAssertionDom(Assertion assertion) {
        Node node = assertion.getDOM();
        Element dom = (Element) node.cloneNode(true); // Do not touch the original DOM
        NamespaceManager nsManager = assertion.getNamespaceManager();
        Set<Namespace> namespaces = nsManager.getAllNamespacesInSubtreeScope();
        for (Namespace namespace : namespaces) {
            if (!dsUri.equals(namespace.getNamespaceURI())) { // do not add the Signature namespace
//                XMLHelper.appendNamespaceDeclaration(dom, namespace.getNamespaceURI(), namespace.getNamespacePrefix());
                NamespaceSupport.appendNamespaceDeclaration(dom, namespace.getNamespaceURI(), namespace.getNamespacePrefix());//v3
            }
        }
        return dom;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.saml2.Saml20Token#getSessionIndex()
     */
//    @Override
//    public String getSessionIndex() {
//        // TODO Auto-generated method stub
//        return this.sessionIndex;
//    }

}

/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.saml2;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

public interface Saml20Token {

    /**
     * Retrieves the identifier associated with this SAML assertion.
     * 
     * @return a string representing the ID for SAML 2.0, or AssertionID for SAML 1.1.
     */
    public String getSamlID();

    /**
     * Return SAML Assertion namespace, defined in a schema SAML-XSD.
     * 
     * @return SAML Assertion namespace, defined in a schema SAML-XSD
     *         It is "urn:oasis:names:tc:SAML:1.0:assertion" for SAML 1.1, and
     *         "urn:oasis:names:tc:SAML:2.0:assertion" for SAML 2.0.
     */
    public QName getAssertionQName();

    /**
     * Return SAML Expiration time.
     * 
     * @return SAML Token expiration time, which is delimited by the NotOnOrAfter attribute in <Conditions> element.
     */
    public Date getSamlExpires();

    /**
     * Retrieves the SAML assertion creation date.
     * 
     * @return SAML Token creation Date based on the IssueInstant attribute in <Assertion> element.
     * 
     */
    public Date getIssueInstant();

    /**
     * Retrieves the Subject Confirmation Method used in this SAML token.
     * based on the SAML token profile for versions 1.1 and 2.0.
     * 
     * @see <a href="http://www.oasis-open.org/committees/download.php/16768/wss-v1.1-spec-os-SAMLTokenProfile.pdf"> OASIS SAML Token Profile 1.1</a>
     * @see <a href="http://docs.oasis-open.org/security/saml/v2.0/saml-profiles-2.0-os.pdf"> OASIS SAML Token Profile 2.0</a>
     * 
     * @return List of SAML SubjectConfirmation Method, and valid method is holder-of-key, bearer, or sender-vouches.
     */
    public List<String> getConfirmationMethod();

    /**
     * Retrieves the key bytes from the Holder-of-Key Element of this SAML token.
     * 
     * @return the shared secret key bytes for a symmetric holder-of-key assertion.
     */
    public byte[] getHolderOfKeyBytes();

    /**
     * Retrieves the name of NameID.
     * 
     * @return NameID in the SAML assertion.
     */
    public String getSAMLNameID();

    /**
     * Retrieves the format of NameID.
     * 
     * @return NameID format in the SAML assertion.
     */
    public String getSAMLNameIDFormat();

    /**
     * Retrieves the name of issuer.
     * 
     * @return issuer name of the SAML authority responsible for the claims in the SAML assertion.
     */
    public String getSAMLIssuerName();

    /**
     * Retrieves the issuer name format.
     * 
     * @return name format of the SAML authority responsible for the claims in the SAML assertion.
     */
    public String getSAMLIssuerNameFormat();

    /**
     * Retrieves the authentication method that was used to authenticate the token holder.
     * 
     * @return the authentication method that took place prior to the token's creation.
     *         For example "password", "kerberos", "ltpa".
     */
    public String getAuthenticationMethod();

    /**
     * Retrieves the authentication time when the token holder is authenticated.
     * 
     * @return the authentication time when the token holder is authenticated.
     */
    public Date getAuthenticationInstant();

    /**
     * Retrieves DNSAddress in SubjectLocality.
     * 
     * @return DNSAddress in SubjectLocality.
     */
    public String getSubjectDNS();

    /**
     * Retrieves IPAddress in SubjectLocality.
     * 
     * @return IPAddress in SubjectLocality.
     */
    public String getSubjectIPAddress();

    /**
     * Retrieves AudienceRestriction String name list.
     * 
     * @return AudienceRestriction String name list.
     */
    public List<String> getAudienceRestriction();

    /**
     * Retrieves flag to indicate OneTimeUse or DoNotCacheCondition.
     * 
     * @return flag to indicate OneTimeUse or DoNotCacheCondition.
     */
    public boolean isOneTimeUse();

    /**
     * Retrieves flag to indicate ProxyRestriction.
     * 
     * @return flag to indicate ProxyRestriction.
     */
    public boolean hasProxyRestriction();

    /**
     * Retrieves number of ProxyRestriction Count.
     * 
     * @return number of ProxyRestriction Count.
     */
    public long getProxyRestrictionCount();

    /**
     * Retrieves String list of ProxyRestriction Audience.
     * 
     * @return String list of ProxyRestriction Audience.
     */
    public List<String> getProxyRestrictionAudience();

    /**
     * Retrieves SAML signer's X.509 Certificate
     * 
     * @return SAML signer's X.509 Certificate
     */
    public List<X509Certificate> getSignerCertificate();

    /**
     * Gets the serializable representation of this SAML XML.
     * 
     * @return the String representation of this SAML
     */
    public String getSAMLAsString();

    public List<Saml20Attribute> getSAMLAttributes();

    /**
     * Retrieves the id of the SAML Service Provider
     * Such as: the "ibmSP01"
     * in &lt;samlWebSso20 id="ibmSP01" authFilterRef="requestFilter01" ... \&gt;
     * 
     * @return the ID of the SAML Service Provider
     */
    public String getServiceProviderID();

    /**
     * Gets customized properties.
     * 
     * @return the Map of properties
     */
    public Map<String, Object> getProperties();
}

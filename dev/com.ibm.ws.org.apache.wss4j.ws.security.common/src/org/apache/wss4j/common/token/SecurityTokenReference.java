/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.wss4j.common.token;

import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.common.util.XMLUtils;

/**
 * Security Token Reference.
 */
public class SecurityTokenReference {
    public static final String SECURITY_TOKEN_REFERENCE = "SecurityTokenReference";
    public static final QName STR_QNAME =
        new QName(WSS4JConstants.WSSE_NS, SECURITY_TOKEN_REFERENCE);
    public static final String SKI_URI =
        WSS4JConstants.X509TOKEN_NS + "#X509SubjectKeyIdentifier";
    public static final String THUMB_URI =
        WSS4JConstants.SOAPMESSAGE_NS11 + "#" + WSS4JConstants.THUMBPRINT;
    public static final String ENC_KEY_SHA1_URI =
        WSS4JConstants.SOAPMESSAGE_NS11 + "#" + WSS4JConstants.ENC_KEY_SHA1_URI;
    public static final String X509_V3_TYPE = WSS4JConstants.X509TOKEN_NS + "#X509v3";

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(SecurityTokenReference.class);

    private Element element;
    private DOMX509IssuerSerial issuerSerial;
    private byte[] skiBytes;
    private Reference reference;

    /**
     * Constructor.
     *
     * @param elem A SecurityTokenReference element
     * @param bspEnforcer a BSPEnforcer instance to enforce BSP rules
     * @throws WSSecurityException
     */
    public SecurityTokenReference(Element elem, BSPEnforcer bspEnforcer) throws WSSecurityException {
        element = elem;
        QName el = new QName(element.getNamespaceURI(), element.getLocalName());
        if (!STR_QNAME.equals(el)) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "badElement",
                                          new Object[] {STR_QNAME, el});
        }

        checkBSPCompliance(bspEnforcer);

        if (containsReference()) {
            Node node = element.getFirstChild();
            while (node != null) {
                if (Node.ELEMENT_NODE == node.getNodeType()
                    && WSS4JConstants.WSSE_NS.equals(node.getNamespaceURI())
                    && "Reference".equals(node.getLocalName())) {
                    reference = new Reference((Element)node);
                    break;
                }
                node = node.getNextSibling();
            }
        }
    }

    /**
     * Constructor.
     *
     * @param doc The Document
     */
    public SecurityTokenReference(Document doc) {
        element = doc.createElementNS(WSS4JConstants.WSSE_NS, "wsse:SecurityTokenReference");
    }

    /**
     * Add the WSSE Namespace to this STR. The namespace is not added by default for
     * efficiency purposes.
     */
    public void addWSSENamespace() {
        XMLUtils.setNamespace(element, WSS4JConstants.WSSE_NS, WSS4JConstants.WSSE_PREFIX);
    }

    /**
     * Add the WSU Namespace to this STR. The namespace is not added by default for
     * efficiency purposes.
     */
    public void addWSUNamespace() {
        element.setAttributeNS(XMLUtils.XMLNS_NS, "xmlns:" + WSS4JConstants.WSU_PREFIX, WSS4JConstants.WSU_NS);
    }

    /**
     * Add a wsse11:TokenType attribute to this SecurityTokenReference
     * @param tokenType the wsse11:TokenType attribute to add
     */
    public void addTokenType(String tokenType) {
        if (tokenType != null) {
            XMLUtils.setNamespace(element, WSS4JConstants.WSSE11_NS, WSS4JConstants.WSSE11_PREFIX);
            element.setAttributeNS(
                WSS4JConstants.WSSE11_NS,
                WSS4JConstants.WSSE11_PREFIX + ":" + WSS4JConstants.TOKEN_TYPE,
                tokenType
            );
        }
    }

    /**
     * Get the wsse11:TokenType attribute of this SecurityTokenReference
     * @return the value of the wsse11:TokenType attribute
     */
    public String getTokenType() {
        return element.getAttributeNS(
            WSS4JConstants.WSSE11_NS, WSS4JConstants.TOKEN_TYPE
        );
    }

    /**
     * set the reference.
     *
     * @param ref
     */
    public void setReference(Reference ref) {
        Element elem = getFirstElement();
        if (elem != null) {
            element.replaceChild(ref.getElement(), elem);
        } else {
            element.appendChild(ref.getElement());
        }
        this.reference = ref;
    }

    /**
     * Gets the Reference.
     *
     * @return the <code>Reference</code> element contained in this
     *         SecurityTokenReference
     * @throws WSSecurityException
     */
    public Reference getReference() throws WSSecurityException {
        return reference;
    }

    /**
     * Sets the KeyIdentifier Element as a X509 certificate.
     * Takes a X509 certificate, converts its data into base 64 and inserts
     * it into a <code>wsse:KeyIdentifier</code> element, which is placed
     * in the <code>wsse:SecurityTokenReference</code> element.
     *
     * @param cert is the X509 certificate to be inserted as key identifier
     */
    public void setKeyIdentifier(X509Certificate cert)
        throws WSSecurityException {
        Document doc = element.getOwnerDocument();
        byte[] data = null;
        try {
            data = cert.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e, "encodeError"
            );
        }
        Text text = doc.createTextNode(org.apache.xml.security.utils.XMLUtils.encodeToString(data));

        createKeyIdentifier(doc, X509_V3_TYPE, text, true);
    }

    /**
     * Sets the KeyIdentifier Element as a X509 Subject-Key-Identifier (SKI).
     * Takes a X509 certificate, gets the SKI data, converts it into base 64 and
     * inserts it into a <code>wsse:KeyIdentifier</code> element, which is placed
     * in the <code>wsse:SecurityTokenReference</code> element.
     *
     * @param cert   is the X509 certificate to get the SKI
     * @param crypto is the Crypto implementation. Used to read SKI info bytes from certificate
     */
    public void setKeyIdentifierSKI(X509Certificate cert, Crypto crypto)
        throws WSSecurityException {
        //
        // As per the 1.1 specification, SKI can only be used for a V3 certificate
        //
        if (cert.getVersion() != 3) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN,
                "invalidCertForSKI", new Object[] {cert.getVersion()});
        }

        Document doc = element.getOwnerDocument();
        // Fall back to Merlin if crypto parameter is null
        Crypto skiCrypto = crypto;
        if (skiCrypto == null) {
            skiCrypto = new Merlin();
        }
        byte[] data = skiCrypto.getSKIBytesFromCert(cert);

        Text text = doc.createTextNode(org.apache.xml.security.utils.XMLUtils.encodeToString(data));
        createKeyIdentifier(doc, SKI_URI, text, true);
    }

    /**
     * Sets the KeyIdentifier Element as a Thumbprint.
     *
     * Takes a X509 certificate, computes its thumbprint using SHA-1, converts
     * into base 64 and inserts it into a <code>wsse:KeyIdentifier</code>
     * element, which is placed in the <code>wsse:SecurityTokenReference</code>
     * element.
     *
     * @param cert is the X509 certificate to get the thumbprint
     */
    public void setKeyIdentifierThumb(X509Certificate cert) throws WSSecurityException {
        Document doc = element.getOwnerDocument();
        byte[] encodedCert = null;
        try {
            encodedCert = cert.getEncoded();
        } catch (CertificateEncodingException e1) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e1, "encodeError"
            );
        }
        try {
            byte[] encodedBytes = KeyUtils.generateDigest(encodedCert);
            Text text = doc.createTextNode(org.apache.xml.security.utils.XMLUtils.encodeToString(encodedBytes));
            createKeyIdentifier(doc, THUMB_URI, text, true);
        } catch (WSSecurityException e1) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, e1, "decoding.general"
            );
        }
    }

    public void setKeyIdentifierEncKeySHA1(String value) throws WSSecurityException {
        Document doc = element.getOwnerDocument();
        Text text = doc.createTextNode(value);
        createKeyIdentifier(doc, ENC_KEY_SHA1_URI, text, true);
    }

    public void setKeyIdentifier(String valueType, String keyIdVal) throws WSSecurityException {
        setKeyIdentifier(valueType, keyIdVal, false);
    }

    public void setKeyIdentifier(String valueType, String keyIdVal, boolean base64)
        throws WSSecurityException {
        Document doc = element.getOwnerDocument();
        createKeyIdentifier(doc, valueType, doc.createTextNode(keyIdVal), base64);
    }

    private void createKeyIdentifier(Document doc, String uri, Node node, boolean base64) {
        Element keyId = doc.createElementNS(WSS4JConstants.WSSE_NS, "wsse:KeyIdentifier");
        keyId.setAttributeNS(null, "ValueType", uri);
        if (base64) {
            keyId.setAttributeNS(null, "EncodingType", WSS4JConstants.BASE64_ENCODING);
        }

        keyId.appendChild(node);
        Element elem = getFirstElement();
        if (elem != null) {
            element.replaceChild(keyId, elem);
        } else {
            element.appendChild(keyId);
        }
    }

    /**
     * get the first child element.
     *
     * @return the first <code>Element</code> child node
     */
    public Element getFirstElement() {
        for (Node currentChild = element.getFirstChild();
             currentChild != null;
             currentChild = currentChild.getNextSibling()
        ) {
            if (Node.ELEMENT_NODE == currentChild.getNodeType()) {
                return (Element) currentChild;
            }
        }
        return null;
    }

    /**
     * Gets the KeyIdentifier.
     *
     * @return the the X509 certificate or zero if a unknown key identifier
     *         type was detected.
     */
    public X509Certificate[] getKeyIdentifier(Crypto crypto) throws WSSecurityException {
        if (crypto == null) {
            return new X509Certificate[0];
        }

        Element elem = getFirstElement();
        String value = elem.getAttributeNS(null, "ValueType");

        if (X509_V3_TYPE.equals(value)) {
            X509Security token = new X509Security(elem, new BSPEnforcer(true));
            X509Certificate cert = token.getX509Certificate(crypto);
            return new X509Certificate[]{cert};
        } else if (SKI_URI.equals(value)) {
            X509Certificate cert = getX509SKIAlias(crypto);
            if (cert != null) {
                return new X509Certificate[]{cert};
            }
        } else if (THUMB_URI.equals(value)) {
            String text = XMLUtils.getElementText(getFirstElement());
            if (text != null) {
                byte[] thumb = org.apache.xml.security.utils.XMLUtils.decode(text);

                CryptoType cryptoType = new CryptoType(CryptoType.TYPE.THUMBPRINT_SHA1);
                cryptoType.setBytes(thumb);
                X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
                if (certs != null && certs.length > 0) {
                    return new X509Certificate[]{certs[0]};
                }
            }
        }

        return new X509Certificate[0];
    }

    public String getKeyIdentifierValue() {
        if (containsKeyIdentifier()) {
            return XMLUtils.getElementText(getFirstElement());
        }
        return null;
    }

    public String getKeyIdentifierValueType() {
        if (containsKeyIdentifier()) {
            Element elem = getFirstElement();
            return elem.getAttributeNS(null, "ValueType");
        }
        return null;
    }

    public String getKeyIdentifierEncodingType() {
        if (containsKeyIdentifier()) {
            Element elem = getFirstElement();
            return elem.getAttributeNS(null, "EncodingType");
        }
        return null;
    }

    public X509Certificate getX509SKIAlias(Crypto crypto) throws WSSecurityException {
        if (crypto == null) {
            return null;
        }

        if (skiBytes == null) {
            skiBytes = getSKIBytes();
            if (skiBytes == null) {
                return null;
            }
        }
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.SKI_BYTES);
        cryptoType.setBytes(skiBytes);
        X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
        if (certs != null && certs.length > 0) {
            return certs[0];
        }
        return null;
    }

    public byte[] getSKIBytes() {
        if (skiBytes != null) {
            return skiBytes;
        }
        String text = XMLUtils.getElementText(getFirstElement());
        if (text != null) {
            skiBytes = org.apache.xml.security.utils.XMLUtils.decode(text);
        }
        return skiBytes;
    }


    /**
     * Set an unknown element.
     *
     * @param unknownElement the org.w3c.dom.Element to put into this
     *        SecurityTokenReference
     */
    public void setUnknownElement(Element unknownElement) {
        Element elem = getFirstElement();
        if (elem != null) {
            element.replaceChild(unknownElement, elem);
        } else {
            element.appendChild(unknownElement);
        }
    }

    /**
     * Gets the certificate identified with X509 issuerSerial data.
     *
     * @return a certificate array or null if nothing found
     */
    public X509Certificate[] getX509IssuerSerial(Crypto crypto) throws WSSecurityException {
        if (crypto == null) {
            return new X509Certificate[0];
        }

        if (issuerSerial == null) {
            issuerSerial = getIssuerSerial();
            if (issuerSerial == null) {
                return new X509Certificate[0];
            }
        }
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ISSUER_SERIAL);
        cryptoType.setIssuerSerial(issuerSerial.getIssuer(), issuerSerial.getSerialNumber());
        return crypto.getX509Certificates(cryptoType);
    }

    private DOMX509IssuerSerial getIssuerSerial() throws WSSecurityException {
        if (issuerSerial != null) {
            return issuerSerial;
        }
        Element elem = getFirstElement();
        if (elem == null) {
            return null;
        }
        if (WSS4JConstants.X509_DATA_LN.equals(elem.getLocalName())) {
            elem =
                XMLUtils.findElement(
                    elem, WSS4JConstants.X509_ISSUER_SERIAL_LN, WSS4JConstants.SIG_NS
                );
        }
        issuerSerial = new DOMX509IssuerSerial(elem);

        return issuerSerial;
    }

    /**
     * Method containsReference
     *
     * @return true if the <code>SecurityTokenReference</code> contains
     *         a <code>wsse:Reference</code> element
     */
    public boolean containsReference() {
        return containsElement(WSS4JConstants.WSSE_NS, "Reference");
    }

    /**
     * Method containsX509IssuerSerial
     *
     * @return true if the <code>SecurityTokenReference</code> contains
     *         a <code>ds:IssuerSerial</code> element
     */
    public boolean containsX509IssuerSerial() {
        return containsElement(WSS4JConstants.SIG_NS, WSS4JConstants.X509_ISSUER_SERIAL_LN);
    }

    /**
     * Method containsX509Data
     *
     * @return true if the <code>SecurityTokenReference</code> contains
     *         a <code>ds:X509Data</code> element
     */
    public boolean containsX509Data() {
        return containsElement(WSS4JConstants.SIG_NS, WSS4JConstants.X509_DATA_LN);
    }

    /**
     * Method containsKeyIdentifier.
     *
     * @return true if the <code>SecurityTokenReference</code> contains
     *         a <code>wsse:KeyIdentifier</code> element
     */
    public boolean containsKeyIdentifier() {
        return containsElement(WSS4JConstants.WSSE_NS, "KeyIdentifier");
    }

    private boolean containsElement(String namespace, String localname) {
        Node node = element.getFirstChild();
        while (node != null) {
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                String ns = node.getNamespaceURI();
                String name = node.getLocalName();
                if ((namespace != null && namespace.equals(ns)
                    || namespace == null && ns == null)
                    && localname.equals(name)
                ) {
                    return true;
                }
            }
            node = node.getNextSibling();
        }
        return false;
    }

    /**
     * Get the DOM element.
     *
     * @return the DOM element
     */
    public Element getElement() {
        return element;
    }

    /**
     * set the id.
     *
     * @param id
     */
    public void setID(String id) {
        element.setAttributeNS(WSS4JConstants.WSU_NS, WSS4JConstants.WSU_PREFIX + ":Id", id);
    }

    /**
     * Get the id
     * @return the wsu ID of the element
     */
    public String getID() {
        return element.getAttributeNS(WSS4JConstants.WSU_NS, "Id");
    }

    /**
     * return the string representation.
     *
     * @return a representation of this SecurityTokenReference element as a String
     */
    public String toString() {
        return DOM2Writer.nodeToString(element);
    }

    /**
     * A method to check that the SecurityTokenReference is compliant with the BSP spec.
     * @throws WSSecurityException
     */
    private void checkBSPCompliance(BSPEnforcer bspEnforcer) throws WSSecurityException {
        // We can only have one token reference
        int result = 0;
        Node node = element.getFirstChild();
        Element child = null;
        while (node != null) {
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                result++;
                child = (Element)node;
            }
            node = node.getNextSibling();
        }
        if (result != 1) {
            bspEnforcer.handleBSPRule(BSPRule.R3061);
        }
        if ("KeyIdentifier".equals(child.getLocalName())
            && WSS4JConstants.WSSE_NS.equals(child.getNamespaceURI())) {

            String valueType = getKeyIdentifierValueType();
            // ValueType cannot be null
            if (valueType == null || valueType.length() == 0) {
                bspEnforcer.handleBSPRule(BSPRule.R3054);
            }
            String encodingType = getFirstElement().getAttributeNS(null, "EncodingType");
            // Encoding Type must be equal to Base64Binary if it's specified
            if (encodingType.length() != 0 && !WSS4JConstants.BASE64_ENCODING.equals(encodingType)) {
                bspEnforcer.handleBSPRule(BSPRule.R3071);
            }
            // Encoding type must be specified other than for a SAML Assertion

            if (!WSS4JConstants.WSS_SAML_KI_VALUE_TYPE.equals(valueType)
                && !WSS4JConstants.WSS_SAML2_KI_VALUE_TYPE.equals(valueType)
                && encodingType.length() == 0) {
                bspEnforcer.handleBSPRule(BSPRule.R3070);
            }
        } else if ("Embedded".equals(child.getLocalName())) {
            result = 0;
            node = child.getFirstChild();
            while (node != null) {
                if (Node.ELEMENT_NODE == node.getNodeType()) {
                    result++;
                    // We cannot have a SecurityTokenReference child element
                    if ("SecurityTokenReference".equals(node.getLocalName())
                        && WSS4JConstants.WSSE_NS.equals(node.getNamespaceURI())) {
                        bspEnforcer.handleBSPRule(BSPRule.R3056);
                    }
                }
                node = node.getNextSibling();
            }
            // We can only have one embedded child
            if (result != 1) {
                bspEnforcer.handleBSPRule(BSPRule.R3060);
            }
        }
    }

    @Override
    public int hashCode() {
        int result = 17;
        try {
            Reference reference = getReference();
            if (reference != null) {
                result = 31 * result + reference.hashCode();
            }
        } catch (WSSecurityException e) {
            LOG.error(e.getMessage(), e);
        }
        String keyIdentifierEncodingType = getKeyIdentifierEncodingType();
        if (keyIdentifierEncodingType != null) {
            result = 31 * result + keyIdentifierEncodingType.hashCode();
        }
        String keyIdentifierValueType = getKeyIdentifierValueType();
        if (keyIdentifierValueType != null) {
            result = 31 * result + keyIdentifierValueType.hashCode();
        }
        String keyIdentifierValue = getKeyIdentifierValue();
        if (keyIdentifierValue != null) {
            result = 31 * result + keyIdentifierValue.hashCode();
        }
        String tokenType = getTokenType();
        if (tokenType != null) {
            result = 31 * result + tokenType.hashCode();
        }
        byte[] skiBytes = getSKIBytes();
        if (skiBytes != null) {
            result = 31 * result + Arrays.hashCode(skiBytes);
        }
        String issuer = null;
        BigInteger serialNumber = null;

        try {
            issuer = getIssuerSerial().getIssuer();
            serialNumber = getIssuerSerial().getSerialNumber();
        } catch (WSSecurityException e) {
           LOG.error(e.getMessage(), e);
        }
        if (issuer != null) {
            result = 31 * result + issuer.hashCode();
        }
        if (serialNumber != null) {
            result = 31 * result + serialNumber.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SecurityTokenReference)) {
            return false;
        }
        SecurityTokenReference tokenReference = (SecurityTokenReference)object;
        try {
            if (!getReference().equals(tokenReference.getReference())) {
                return false;
            }
        } catch (WSSecurityException e) {
           LOG.error(e.getMessage(), e);
           return false;
        }
        if (!compare(getKeyIdentifierEncodingType(), tokenReference.getKeyIdentifierEncodingType())) {
            return false;
        }
        if (!compare(getKeyIdentifierValueType(), tokenReference.getKeyIdentifierValueType())) {
            return false;
        }
        if (!compare(getKeyIdentifierValue(), tokenReference.getKeyIdentifierValue())) {
            return false;
        }
        if (!compare(getTokenType(), tokenReference.getTokenType())) {
            return false;
        }
        if (!Arrays.equals(getSKIBytes(), tokenReference.getSKIBytes())) {
            return false;
        }
        try {
            if (getIssuerSerial() != null && tokenReference.getIssuerSerial() != null) {
                if (!compare(getIssuerSerial().getIssuer(), tokenReference.getIssuerSerial().getIssuer())) {
                    return false;
                }
                if (!compare(getIssuerSerial().getSerialNumber(), tokenReference.getIssuerSerial().getSerialNumber())) {
                    return false;
                }
            }
        } catch (WSSecurityException e) {
           LOG.error(e.getMessage(), e);
           return false;
        }

        return true;
    }

    private boolean compare(String item1, String item2) {
        if (item1 == null && item2 != null) {
            return false;
        } else if (item1 != null && !item1.equals(item2)) {
            return false;
        }
        return true;
    }

    private boolean compare(BigInteger item1, BigInteger item2) {
        if (item1 == null && item2 != null) {
            return false;
        } else if (item1 != null && !item1.equals(item2)) {
            return false;
        }
        return true;
    }
}

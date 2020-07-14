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

package org.apache.cxf.ws.security.wss4j.policyvalidators;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.Header;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedElements;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedParts;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.KerberosSecurity;
import org.apache.ws.security.message.token.PKIPathSecurity;
import org.apache.ws.security.message.token.X509Security;
import org.apache.ws.security.saml.SAMLKeyInfo;
import org.apache.ws.security.saml.ext.AssertionWrapper;

/**
 * A base class to use to validate various SupportingToken policies.
 */
public abstract class AbstractSupportingTokenPolicyValidator
                extends AbstractTokenPolicyValidator implements SupportingTokenPolicyValidator {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractSupportingTokenPolicyValidator.class);

    private Message message;
    private List<WSSecurityEngineResult> results;
    private List<WSSecurityEngineResult> signedResults;
    private List<WSSecurityEngineResult> encryptedResults;
    private List<WSSecurityEngineResult> utResults;
    private List<WSSecurityEngineResult> samlResults;
    private boolean validateUsernameToken = true;
    private Element timestamp;
    private boolean signed;
    private boolean encrypted;
    private boolean derived;
    private boolean endorsed;
    private SignedEncryptedElements signedElements;
    private SignedEncryptedElements encryptedElements;
    private SignedEncryptedParts signedParts;
    private SignedEncryptedParts encryptedParts;

    /**
     * Set the list of UsernameToken results
     */
    public void setUsernameTokenResults(
                                        List<WSSecurityEngineResult> utResultsList,
                                        boolean valUsernameToken) {
        utResults = utResultsList;
        validateUsernameToken = valUsernameToken;
    }

    /**
     * Set the list of SAMLToken results
     */
    public void setSAMLTokenResults(List<WSSecurityEngineResult> samlResultsList) {
        samlResults = samlResultsList;
    }

    /**
     * Set the Timestamp element
     */
    public void setTimestampElement(Element timestampElement) {
        timestamp = timestampElement;
    }

    public void setMessage(Message msg) {
        message = msg;
    }

    public void setResults(List<WSSecurityEngineResult> results) {
        this.results = results;
    }

    public void setSignedResults(List<WSSecurityEngineResult> signedResults) {
        this.signedResults = signedResults;
    }

    public void setEncryptedResults(List<WSSecurityEngineResult> encryptedResults) {
        this.encryptedResults = encryptedResults;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public void setDerived(boolean derived) {
        this.derived = derived;
    }

    public void setEndorsed(boolean endorsed) {
        this.endorsed = endorsed;
    }

    /**
     * Process UsernameTokens.
     */
    protected boolean processUsernameTokens() {
        if (!validateUsernameToken) {
            return true;
        }

        List<WSSecurityEngineResult> tokenResults = new ArrayList<WSSecurityEngineResult>();
        tokenResults.addAll(utResults);
        List<WSSecurityEngineResult> dktResults = new ArrayList<WSSecurityEngineResult>();
        for (WSSecurityEngineResult wser : utResults) {
            if (derived) {
                byte[] secret = (byte[]) wser.get(WSSecurityEngineResult.TAG_SECRET);
                WSSecurityEngineResult dktResult = getMatchingDerivedKey(secret);
                if (dktResult != null) {
                    dktResults.add(dktResult);
                }
            }
        }

        if (tokenResults.isEmpty()) {
            return false;
        }

        if (signed && !areTokensSigned(tokenResults)) {
            return false;
        }
        if (encrypted && !areTokensEncrypted(tokenResults)) {
            return false;
        }
        tokenResults.addAll(dktResults);
        if ((endorsed && !checkEndorsed(tokenResults)) || !validateSignedEncryptedPolicies(tokenResults)) {
            return false;
        }

        return true;
    }

    /**
     * Process SAML Tokens. Only signed results are supported.
     */
    protected boolean processSAMLTokens() {
        if (samlResults.isEmpty()) {
            return false;
        }

        if (signed && !areTokensSigned(samlResults)) {
            return false;
        }
        if (encrypted && !areTokensEncrypted(samlResults)) {
            return false;
        }
        if (endorsed && !checkEndorsed(samlResults)) {
            return false;
        }

        if (!validateSignedEncryptedPolicies(samlResults)) {
            return false;
        }

        return true;
    }

    /**
     * Process Kerberos Tokens.
     */
    protected boolean processKerberosTokens() {
        List<WSSecurityEngineResult> tokenResults = new ArrayList<WSSecurityEngineResult>();
        List<WSSecurityEngineResult> dktResults = new ArrayList<WSSecurityEngineResult>();
        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer) wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.BST) {
                BinarySecurity binarySecurity =
                                (BinarySecurity) wser.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                if (binarySecurity instanceof KerberosSecurity) {
                    if (derived) {
                        byte[] secret = (byte[]) wser.get(WSSecurityEngineResult.TAG_SECRET);
                        WSSecurityEngineResult dktResult = getMatchingDerivedKey(secret);
                        if (dktResult != null) {
                            dktResults.add(dktResult);
                        }
                    }
                    tokenResults.add(wser);
                }
            }
        }

        if (tokenResults.isEmpty()) {
            return false;
        }

        if (signed && !areTokensSigned(tokenResults)) {
            return false;
        }
        if (encrypted && !areTokensEncrypted(tokenResults)) {
            return false;
        }
        tokenResults.addAll(dktResults);
        if (endorsed && !checkEndorsed(tokenResults)) {
            return false;
        }

        if (!validateSignedEncryptedPolicies(tokenResults)) {
            return false;
        }

        return true;
    }

    /**
     * Process X509 Tokens.
     */
    protected boolean processX509Tokens() {
        List<WSSecurityEngineResult> tokenResults = new ArrayList<WSSecurityEngineResult>();
        List<WSSecurityEngineResult> dktResults = new ArrayList<WSSecurityEngineResult>();
        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer) wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.BST) {
                BinarySecurity binarySecurity =
                                (BinarySecurity) wser.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                if (binarySecurity instanceof X509Security
                    || binarySecurity instanceof PKIPathSecurity) {
                    if (derived) {
                        WSSecurityEngineResult resultToStore = processX509DerivedTokenResult(wser);
                        if (resultToStore != null) {
                            dktResults.add(resultToStore);
                        }
                    }
                    tokenResults.add(wser);
                }
            }
        }

        if (tokenResults.isEmpty()) {
            return false;
        }

        if (signed && !areTokensSigned(tokenResults)) {
            return false;
        }
        if (encrypted && !areTokensEncrypted(tokenResults)) {
            return false;
        }
        tokenResults.addAll(dktResults);
        if (endorsed && !checkEndorsed(tokenResults)) {
            return false;
        }

        if (!validateSignedEncryptedPolicies(tokenResults)) {
            return false;
        }

        return true;
    }

    /**
     * Process KeyValue Tokens.
     */
    protected boolean processKeyValueTokens() {
        List<WSSecurityEngineResult> tokenResults = new ArrayList<WSSecurityEngineResult>();
        for (WSSecurityEngineResult wser : signedResults) {
            PublicKey publicKey =
                            (PublicKey) wser.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);
            if (publicKey != null) {
                tokenResults.add(wser);
            }
        }

        if (tokenResults.isEmpty()) {
            return false;
        }

        if (signed && !areTokensSigned(tokenResults)) {
            return false;
        }
        if (encrypted && !areTokensEncrypted(tokenResults)) {
            return false;
        }
        if (endorsed && !checkEndorsed(tokenResults)) {
            return false;
        }

        if (!validateSignedEncryptedPolicies(tokenResults)) {
            return false;
        }

        return true;
    }

    /**
     * Validate (SignedParts|SignedElements|EncryptedParts|EncryptedElements) policies of this
     * SupportingToken.
     */
    private boolean validateSignedEncryptedPolicies(List<WSSecurityEngineResult> tokenResults) {
        if (!validateSignedEncryptedParts(signedParts, false, signedResults, tokenResults)) {
            return false;
        }

        if (!validateSignedEncryptedParts(encryptedParts, true, encryptedResults, tokenResults)) {
            return false;
        }

        if (!validateSignedEncryptedElements(signedElements, false, signedResults, tokenResults)) {
            return false;
        }

        if (!validateSignedEncryptedElements(encryptedElements, false, encryptedResults, tokenResults)) {
            return false;
        }

        return true;
    }

    /**
     * Process Security Context Tokens.
     */
    protected boolean processSCTokens() {
        List<WSSecurityEngineResult> tokenResults = new ArrayList<WSSecurityEngineResult>();
        List<WSSecurityEngineResult> dktResults = new ArrayList<WSSecurityEngineResult>();
        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer) wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.SCT) {
                if (derived) {
                    byte[] secret = (byte[]) wser.get(WSSecurityEngineResult.TAG_SECRET);
                    WSSecurityEngineResult dktResult = getMatchingDerivedKey(secret);
                    if (dktResult != null) {
                        dktResults.add(dktResult);
                    }
                }
                tokenResults.add(wser);
            }
        }

        if (tokenResults.isEmpty()) {
            return false;
        }

        if (signed && !areTokensSigned(tokenResults)) {
            return false;
        }
        if (encrypted && !areTokensEncrypted(tokenResults)) {
            return false;
        }
        tokenResults.addAll(dktResults);
        if (endorsed && !checkEndorsed(tokenResults)) {
            return false;
        }

        if (!validateSignedEncryptedPolicies(tokenResults)) {
            return false;
        }

        return true;
    }

    /**
     * Find an EncryptedKey element that has a cert that matches the cert of the signature, then
     * find a DerivedKey element that matches that EncryptedKey element.
     */
    private WSSecurityEngineResult processX509DerivedTokenResult(WSSecurityEngineResult result) {
        X509Certificate cert =
                        (X509Certificate) result.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        WSSecurityEngineResult encrResult = getMatchingEncryptedKey(cert);
        if (encrResult != null) {
            byte[] secret = (byte[]) encrResult.get(WSSecurityEngineResult.TAG_SECRET);
            WSSecurityEngineResult dktResult = getMatchingDerivedKey(secret);
            if (dktResult != null) {
                return dktResult;
            }
        }
        return null;
    }

    /**
     * Get a security result representing a Derived Key that has a secret key that
     * matches the parameter.
     */
    private WSSecurityEngineResult getMatchingDerivedKey(byte[] secret) {
        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer) wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.DKT) {
                byte[] dktSecret = (byte[]) wser.get(WSSecurityEngineResult.TAG_SECRET);
                if (Arrays.equals(secret, dktSecret)) {
                    return wser;
                }
            }
        }
        return null;
    }

    /**
     * Get a security result representing an EncryptedKey that matches the parameter.
     */
    private WSSecurityEngineResult getMatchingEncryptedKey(X509Certificate cert) {
        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer) wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.ENCR) {
                X509Certificate encrCert =
                                (X509Certificate) wser.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                if (cert.equals(encrCert)) {
                    return wser;
                }
            }
        }
        return null;
    }

    private boolean isTLSInUse() {
        // See whether TLS is in use or not
        TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
        if (tlsInfo != null) {
            return true;
        }
        return false;
    }

    //
    // newly added by IBM
    //
    private boolean isTransportBinding() {
        // See whether TLS is in use or not
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        boolean isTransportBinding = false;
        Collection<AssertionInfo> ais = aim.get(SP12Constants.TRANSPORT_BINDING);
        if (ais != null && !ais.isEmpty()) {
            isTransportBinding = true;
        }

        return isTransportBinding;
    }

    /**
     * Check the endorsing supporting token policy. If we're using the Transport Binding then
     * check that the Timestamp is signed. Otherwise, check that the signature is signed.
     * 
     * @return true if the endorsed supporting token policy is correct
     */
    private boolean checkEndorsed(List<WSSecurityEngineResult> tokenResults) {
        if (isTransportBinding()) {
            return checkTimestampIsSigned(tokenResults);
        }
        return checkSignatureIsSigned(tokenResults);
    }

    /**
     * Return true if a list of tokens were signed, false otherwise.
     */
    private boolean areTokensSigned(List<WSSecurityEngineResult> tokens) {
        if (!isTLSInUse()) {
            for (WSSecurityEngineResult wser : tokens) {
                Element tokenElement = (Element) wser.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
                if (tokenElement == null || !isTokenSigned(tokenElement)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return true if a list of tokens were encrypted, false otherwise.
     */
    private boolean areTokensEncrypted(List<WSSecurityEngineResult> tokens) {
        if (!isTLSInUse()) {
            for (WSSecurityEngineResult wser : tokens) {
                Element tokenElement = (Element) wser.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
                if (tokenElement == null || !isTokenEncrypted(tokenElement)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return true if the Timestamp is signed by one of the token results
     * 
     * @param tokenResults A list of WSSecurityEngineResults corresponding to tokens
     * @return true if the Timestamp is signed
     */
    private boolean checkTimestampIsSigned(List<WSSecurityEngineResult> tokenResults) {
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> sl =
                            CastUtils.cast((List<?>) signedResult.get(
                                            WSSecurityEngineResult.TAG_DATA_REF_URIS
                ));
            if (sl != null) {
                for (WSDataRef dataRef : sl) {
                    if (timestamp == dataRef.getProtectedElement()
                        && checkSignatureOrEncryptionResult(signedResult, tokenResults)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Return true if the Signature is itself signed by one of the token results
     * 
     * @param tokenResults A list of WSSecurityEngineResults corresponding to tokens
     * @return true if the Signature is itself signed
     */
    private boolean checkSignatureIsSigned(List<WSSecurityEngineResult> tokenResults) {
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> sl =
                            CastUtils.cast((List<?>) signedResult.get(
                                            WSSecurityEngineResult.TAG_DATA_REF_URIS
                ));
            if (sl != null && sl.size() == 1) {
                for (WSDataRef dataRef : sl) {
                    QName signedQName = dataRef.getName();
                    if (WSSecurityEngine.SIGNATURE.equals(signedQName)
                        && checkSignatureOrEncryptionResult(signedResult, tokenResults)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check that a WSSecurityEngineResult corresponding to a signature or encryption uses the same
     * signing/encrypting credential as one of the tokens.
     * 
     * @param signatureResult a WSSecurityEngineResult corresponding to a signature or encryption
     * @param tokenResult A list of WSSecurityEngineResults corresponding to tokens
     * @return
     */
    private boolean checkSignatureOrEncryptionResult(
                                                     WSSecurityEngineResult result,
                                                     List<WSSecurityEngineResult> tokenResult) {
        // See what was used to sign/encrypt this result
        X509Certificate cert =
                        (X509Certificate) result.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        byte[] secret = (byte[]) result.get(WSSecurityEngineResult.TAG_SECRET);
        PublicKey publicKey =
                        (PublicKey) result.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);

        // Now see if the same credential exists in the tokenResult list
        for (WSSecurityEngineResult token : tokenResult) {
            Integer actInt = (Integer) token.get(WSSecurityEngineResult.TAG_ACTION);
            BinarySecurity binarySecurity =
                            (BinarySecurity) token.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
            if (binarySecurity instanceof X509Security
                || binarySecurity instanceof PKIPathSecurity) {
                X509Certificate foundCert =
                                (X509Certificate) token.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                if (foundCert.equals(cert)) {
                    return true;
                }
            } else if (actInt.intValue() == WSConstants.ST_SIGNED
                       || actInt.intValue() == WSConstants.ST_UNSIGNED) {
                AssertionWrapper assertionWrapper =
                                (AssertionWrapper) token.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                SAMLKeyInfo samlKeyInfo = assertionWrapper.getSubjectKeyInfo();
                if (samlKeyInfo != null) {
                    X509Certificate[] subjectCerts = samlKeyInfo.getCerts();
                    byte[] subjectSecretKey = samlKeyInfo.getSecret();
                    PublicKey subjectPublicKey = samlKeyInfo.getPublicKey();
                    if ((cert != null && subjectCerts != null && cert.equals(subjectCerts[0]))
                        || (subjectSecretKey != null && Arrays.equals(subjectSecretKey, secret))
                        || (subjectPublicKey != null && subjectPublicKey.equals(publicKey))) {
                        return true;
                    }
                }
            } else if (publicKey != null) {
                PublicKey foundPublicKey =
                                (PublicKey) token.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);
                if (publicKey.equals(foundPublicKey)) {
                    return true;
                }
            } else {
                byte[] foundSecret = (byte[]) token.get(WSSecurityEngineResult.TAG_SECRET);
                byte[] derivedKey =
                                (byte[]) token.get(WSSecurityEngineResult.TAG_ENCRYPTED_EPHEMERAL_KEY);
                if ((foundSecret != null && Arrays.equals(foundSecret, secret))
                    || (derivedKey != null && Arrays.equals(derivedKey, secret))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Validate the SignedParts or EncryptedParts policies
     */
    private boolean validateSignedEncryptedParts(
                                                 SignedEncryptedParts parts,
                                                 boolean content,
                                                 List<WSSecurityEngineResult> protResults,
                                                 List<WSSecurityEngineResult> tokenResults) {
        if (parts == null) {
            return true;
        }

        if (parts.isBody()) {
            SOAPMessage soapMessage = message.getContent(SOAPMessage.class);
            Element soapBody = null;
            try {
                soapBody = soapMessage.getSOAPBody();
            } catch (SOAPException ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
                return false;
            }

            if (!checkProtectionResult(soapBody, content, protResults, tokenResults)) {
                return false;
            }
        }

        for (Header h : parts.getHeaders()) {
            SOAPMessage soapMessage = message.getContent(SOAPMessage.class);
            Element soapHeader = null;
            try {
                soapHeader = soapMessage.getSOAPHeader();
            } catch (SOAPException ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
                return false;
            }

            final List<Element> elements;
            if (h.getName() == null) {
                elements = DOMUtils.getChildrenWithNamespace(soapHeader, h.getNamespace());
            } else {
                elements = DOMUtils.getChildrenWithName(soapHeader, h.getNamespace(), h.getName());
            }

            for (Element el : elements) {
                el = (Element)DOMUtils.getDomElement(el);
                if (!checkProtectionResult(el, false, protResults, tokenResults)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check that an Element is signed or encrypted by one of the token results
     */
    private boolean checkProtectionResult(
                                          Element elementToProtect,
                                          boolean content,
                                          List<WSSecurityEngineResult> protResults,
                                          List<WSSecurityEngineResult> tokenResults) {
        elementToProtect = (Element)DOMUtils.getDomElement(elementToProtect);
        for (WSSecurityEngineResult result : protResults) {
            List<WSDataRef> dataRefs =
                            CastUtils.cast((List<?>) result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            if (dataRefs != null) {
                for (WSDataRef dataRef : dataRefs) {
                    if (elementToProtect == dataRef.getProtectedElement()
                        && content == dataRef.isContent()
                        && checkSignatureOrEncryptionResult(result, tokenResults)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Validate SignedElements or EncryptedElements policies
     */
    private boolean validateSignedEncryptedElements(
                                                    SignedEncryptedElements elements,
                                                    boolean content,
                                                    List<WSSecurityEngineResult> protResults,
                                                    List<WSSecurityEngineResult> tokenResults) {
        if (elements == null) {
            return true;
        }

        Map<String, String> namespaces = elements.getDeclaredNamespaces();
        List<String> xpaths = elements.getXPathExpressions();

        if (xpaths != null) {
            SOAPMessage soapMessage = message.getContent(SOAPMessage.class);
            Element soapEnvelope = soapMessage.getSOAPPart().getDocumentElement();

            for (String xPath : xpaths) {
                if (!checkXPathResult(soapEnvelope, xPath, namespaces, protResults, tokenResults)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check a particular XPath result
     */
    private boolean checkXPathResult(
                                     Element soapEnvelope,
                                     String xPath,
                                     Map<String, String> namespaces,
                                     List<WSSecurityEngineResult> protResults,
                                     List<WSSecurityEngineResult> tokenResults) {
        // XPathFactory and XPath are not thread-safe so we must recreate them
        // each request.
        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();

        if (namespaces != null) {
            xpath.setNamespaceContext(new MapNamespaceContext(namespaces));
        }

        // For each XPath
        for (String xpathString : Arrays.asList(xPath)) {
            // Get the matching nodes
            NodeList list;
            try {
                list = (NodeList) xpath.evaluate(
                                                 xpathString,
                                                 soapEnvelope,
                                                 XPathConstants.NODESET);
            } catch (XPathExpressionException e) {
                LOG.log(Level.FINE, e.getMessage(), e);
                return false;
            }

            // If we found nodes then we need to do the check.
            if (list.getLength() != 0) {
                // For each matching element, check for a ref that
                // covers it.
                for (int x = 0; x < list.getLength(); x++) {
                    final Element el = (Element) list.item(x);

                    if (!checkProtectionResult(el, false, protResults, tokenResults)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Return true if a token was signed, false otherwise.
     */
    private boolean isTokenSigned(Element token) {
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> dataRefs =
                            CastUtils.cast((List<?>) signedResult.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            for (WSDataRef dataRef : dataRefs) {
                if (token == dataRef.getProtectedElement()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return true if a token was encrypted, false otherwise.
     */
    private boolean isTokenEncrypted(Element token) {
        for (WSSecurityEngineResult signedResult : encryptedResults) {
            List<WSDataRef> dataRefs =
                            CastUtils.cast((List<?>) signedResult.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            if (dataRefs == null) {
                return false;
            }
            for (WSDataRef dataRef : dataRefs) {
                if (token == dataRef.getProtectedElement()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setUtResults(List<WSSecurityEngineResult> utResults) {
        this.utResults = utResults;
    }

    public void setValidateUsernameToken(boolean validateUsernameToken) {
        this.validateUsernameToken = validateUsernameToken;
    }

    public void setTimestamp(Element timestamp) {
        this.timestamp = timestamp;
    }

    public void setSignedElements(SignedEncryptedElements signedElements) {
        this.signedElements = signedElements;
    }

    public void setEncryptedElements(SignedEncryptedElements encryptedElements) {
        this.encryptedElements = encryptedElements;
    }

    public void setSignedParts(SignedEncryptedParts signedParts) {
        this.signedParts = signedParts;
    }

    public void setEncryptedParts(SignedEncryptedParts encryptedParts) {
        this.encryptedParts = encryptedParts;
    }

}

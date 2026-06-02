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

package org.apache.wss4j.dom.message;

import java.security.NoSuchProviderException;
import java.security.Provider;
import java.util.List;

import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.ExcC14NParameterSpec;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.derivedKey.ConversationConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.Reference;
import org.apache.wss4j.common.token.SecurityTokenReference;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.transform.STRTransform;
import org.apache.wss4j.dom.util.WSSecurityUtil;

import com.ibm.ws.common.crypto.CryptoUtils;

/**
 * Builder to sign with derived keys
 */
public class WSSecDKSign extends WSSecDerivedKeyBase {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(WSSecDKSign.class);

    private String sigAlgo = WSConstants.HMAC_SHA1;

    // Liberty Change Start: set FIPS default for digest algorithm
    private String digestAlgo = CryptoUtils.isFips140_3Enabled() ? WSConstants.SHA256 : WSConstants.SHA1;
    // Liberty Change End

    private String canonAlgo = WSConstants.C14N_EXCL_OMIT_COMMENTS;
    private byte[] signatureValue;

    private String keyInfoUri;
    private SecurityTokenReference secRef;
    private String strUri;
    private WSDocInfo wsDocInfo;

    private XMLSignatureFactory signatureFactory;
    private XMLSignature sig;
    private KeyInfo keyInfo;
    private CanonicalizationMethod c14nMethod;
    private int derivedKeyLength = -1;
    private boolean addInclusivePrefixes = true;

    public WSSecDKSign(WSSecHeader securityHeader) {
        super(securityHeader);
        init(null);
    }

    public WSSecDKSign(Document doc) {
        this(doc, null);
    }

    public WSSecDKSign(Document doc, Provider provider) {
        super(doc);
        init(provider);
    }

    private void init(Provider provider) {
        if (provider == null) {
            // Try to install the Santuario Provider - fall back to the JDK provider if this does
            // not work
            try {
                signatureFactory = XMLSignatureFactory.getInstance("DOM", "ApacheXMLDSig");
            } catch (NoSuchProviderException ex) {
                signatureFactory = XMLSignatureFactory.getInstance("DOM");
            }
        } else {
            signatureFactory = XMLSignatureFactory.getInstance("DOM", provider);
        }
    }

    public Document build(byte[] ephemeralKey) throws WSSecurityException {

        prepare(ephemeralKey);
        if (getParts().isEmpty()) {
            getParts().add(WSSecurityUtil.getDefaultEncryptionPart(getDocument()));
        } else {
            for (WSEncryptionPart part : getParts()) {
                if ("STRTransform".equals(part.getName()) && part.getId() == null) {
                    part.setId(strUri);
                }
            }
        }

        List<javax.xml.crypto.dsig.Reference> referenceList = addReferencesToSign(getParts());
        computeSignature(referenceList);

        //
        // prepend elements in the right order to the security header
        //
        prependDKElementToHeader();

        return getDocument();
    }

    public void prepare(byte[] ephemeralKey) throws WSSecurityException {
        super.prepare(ephemeralKey);
        wsDocInfo = new WSDocInfo(getDocument());
        sig = null;

        try {
            C14NMethodParameterSpec c14nSpec = null;
            if (addInclusivePrefixes && canonAlgo.equals(WSConstants.C14N_EXCL_OMIT_COMMENTS)) {
                Element securityHeaderElement = getSecurityHeader().getSecurityHeaderElement();
                List<String> prefixes =
                    getInclusivePrefixes(securityHeaderElement, false);
                c14nSpec = new ExcC14NParameterSpec(prefixes);
            }

           c14nMethod = signatureFactory.newCanonicalizationMethod(canonAlgo, c14nSpec);
        } catch (Exception ex) {
            LOG.error("", ex);
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILED_SIGNATURE, ex, "noXMLSig"
            );
        }

        keyInfoUri = getIdAllocator().createSecureId("KI-", keyInfo);

        secRef = new SecurityTokenReference(getDocument());
        strUri = getIdAllocator().createSecureId("STR-", secRef);
        secRef.setID(strUri);
        if (addWSUNamespace) {
            secRef.addWSUNamespace();
        }

        Reference ref = new Reference(getDocument());
        ref.setURI("#" + getId());
        String ns =
            ConversationConstants.getWSCNs(getWscVersion())
            + ConversationConstants.TOKEN_TYPE_DERIVED_KEY_TOKEN;
        ref.setValueType(ns);
        secRef.setReference(ref);

        XMLStructure structure = new DOMStructure(secRef.getElement());
        wsDocInfo.addTokenElement(secRef.getElement(), false);
        KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
        keyInfo =
            keyInfoFactory.newKeyInfo(
                java.util.Collections.singletonList(structure), keyInfoUri
            );

    }

    /**
     * Returns the SignatureElement.
     * The method can be called any time after <code>prepare()</code>.
     * @return The DOM Element of the signature.
     */
    public Element getSignatureElement() {
        Element securityHeaderElement = getSecurityHeader().getSecurityHeaderElement();
        return
            XMLUtils.getDirectChildElement(
                securityHeaderElement, WSConstants.SIG_LN, WSConstants.SIG_NS
            );
    }

    /**
     * This method adds references to the Signature.
     *
     * @param references The list of references to sign
     * @throws WSSecurityException
     */
    public List<javax.xml.crypto.dsig.Reference> addReferencesToSign(
        List<WSEncryptionPart> references
    ) throws WSSecurityException {
        return
            addReferencesToSign(
                getDocument(),
                references,
                wsDocInfo,
                signatureFactory,
                addInclusivePrefixes,
                digestAlgo
            );
    }

    /**
     * Compute the Signature over the references.
     *
     * After references are set this method computes the Signature for them.
     * This method can be called any time after the references were set. See
     * <code>addReferencesToSign()</code>.
     *
     * @throws WSSecurityException
     */
    public void computeSignature(
        List<javax.xml.crypto.dsig.Reference> referenceList
    ) throws WSSecurityException {
        computeSignature(referenceList, true, null);
    }

    /**
     * Compute the Signature over the references.
     *
     * After references are set this method computes the Signature for them.
     * This method can be called any time after the references were set. See
     * <code>addReferencesToSign()</code>.
     *
     * @throws WSSecurityException
     */
    public void computeSignature(
        List<javax.xml.crypto.dsig.Reference> referenceList,
        boolean prepend,
        Element siblingElement
    ) throws WSSecurityException {
        try {
            java.security.Key key = getDerivedKey(sigAlgo);
            SignatureMethod signatureMethod =
                signatureFactory.newSignatureMethod(sigAlgo, null);
            SignedInfo signedInfo =
                signatureFactory.newSignedInfo(c14nMethod, signatureMethod, referenceList);

            sig = signatureFactory.newXMLSignature(
                    signedInfo,
                    keyInfo,
                    null,
                    getIdAllocator().createId("SIG-", null),
                    null);

            //
            // Figure out where to insert the signature element
            //
            XMLSignContext signContext = null;
            Element securityHeaderElement = getSecurityHeader().getSecurityHeaderElement();
            if (prepend) {
                if (siblingElement == null) {
                    siblingElement = (Element)securityHeaderElement.getFirstChild();
                }
                if (siblingElement == null) {
                    signContext = new DOMSignContext(key, securityHeaderElement);
                } else {
                    signContext = new DOMSignContext(key, securityHeaderElement, siblingElement);
                }
            } else {
                signContext = new DOMSignContext(key, securityHeaderElement);
            }

            signContext.putNamespacePrefix(WSConstants.SIG_NS, WSConstants.SIG_PREFIX);
            if (WSConstants.C14N_EXCL_OMIT_COMMENTS.equals(canonAlgo)) {
                signContext.putNamespacePrefix(
                    WSConstants.C14N_EXCL_OMIT_COMMENTS,
                    WSConstants.C14N_EXCL_OMIT_COMMENTS_PREFIX
                );
            }
            signContext.setProperty(STRTransform.TRANSFORM_WS_DOC_INFO, wsDocInfo);
            wsDocInfo.setCallbackLookup(callbackLookup);

            // Add the elements to sign to the Signature Context
            wsDocInfo.setTokensOnContext((DOMSignContext)signContext);

            sig.sign(signContext);

            signatureValue = sig.getSignatureValue().getValue();
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILED_SIGNATURE, ex
            );
        }
    }

    protected int getDerivedKeyLength() throws WSSecurityException {
        return derivedKeyLength > 0 ? derivedKeyLength : KeyUtils.getKeyLength(sigAlgo);
    }

    public void setDerivedKeyLength(int keyLength) {
        derivedKeyLength = keyLength;
    }

    /**
     * Set the signature algorithm to use. The default is WSConstants.SHA1.
     * @param algorithm the signature algorithm to use.
     */
    public void setSignatureAlgorithm(String algorithm) {
        sigAlgo = algorithm;
    }

    /**
     * @return the signature algorithm to use
     */
    public String getSignatureAlgorithm() {
        return sigAlgo;
    }

    /**
     * Returns the the value of wsu:Id attribute of the Signature element.
     *
     * @return Return the wsu:Id of this token or null if the signature has not been generated.
     */
    public String getSignatureId() {
        if (sig == null) {
            return null;
        }
        return sig.getId();
    }

    /**
     * Set the digest algorithm to use. The default is WSConstants.SHA1.
     * @param algorithm the digest algorithm to use.
     */
    public void setDigestAlgorithm(String algorithm) {
        digestAlgo = algorithm;
    }

    /**
     * @return the digest algorithm to use
     */
    public String getDigestAlgorithm() {
        return digestAlgo;
    }

    /**
     * @return Returns the signatureValue.
     */
    public byte[] getSignatureValue() {
        return signatureValue;
    }

    /**
     * Set the canonicalization method to use.
     *
     * If the canonicalization method is not set then the recommended Exclusive
     * XML Canonicalization is used by default Refer to WSConstants which
     * algorithms are supported.
     *
     * @param algo Is the name of the signature algorithm
     * @see WSConstants#C14N_OMIT_COMMENTS
     * @see WSConstants#C14N_WITH_COMMENTS
     * @see WSConstants#C14N_EXCL_OMIT_COMMENTS
     * @see WSConstants#C14N_EXCL_WITH_COMMENTS
     */
    public void setSigCanonicalization(String algo) {
        canonAlgo = algo;
    }

    /**
     * Get the canonicalization method.
     *
     * If the canonicalization method was not set then Exclusive XML
     * Canonicalization is used by default.
     *
     * @return The string describing the canonicalization algorithm.
     */
    public String getSigCanonicalization() {
        return canonAlgo;
    }

    public boolean isAddInclusivePrefixes() {
        return addInclusivePrefixes;
    }

    public void setAddInclusivePrefixes(boolean addInclusivePrefixes) {
        this.addInclusivePrefixes = addInclusivePrefixes;
    }
}

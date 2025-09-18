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

package org.apache.wss4j.dom.processor;

import java.security.Key;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.Provider;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.crypto.Data;
import javax.xml.crypto.NodeSetData;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.dsig.Manifest;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.XMLValidateContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.spec.ExcC14NParameterSpec;
import javax.xml.crypto.dsig.spec.HMACParameterSpec;

import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.crypto.AlgorithmSuite;
import org.apache.wss4j.common.crypto.AlgorithmSuiteValidator;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.PublicKeyPrincipalImpl;
import org.apache.wss4j.common.principal.UsernameTokenPrincipal;
import org.apache.wss4j.common.principal.WSDerivedKeyTokenPrincipal;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.common.token.SecurityTokenReference;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.callback.CallbackLookup;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.Timestamp;
import org.apache.wss4j.dom.str.STRParser;
import org.apache.wss4j.dom.str.STRParser.REFERENCE_TYPE;
import org.apache.wss4j.dom.str.STRParserParameters;
import org.apache.wss4j.dom.str.STRParserResult;
import org.apache.wss4j.dom.str.SignatureSTRParser;
import org.apache.wss4j.dom.transform.AttachmentContentSignatureTransform;
import org.apache.wss4j.dom.transform.STRTransform;
import org.apache.wss4j.dom.transform.STRTransformUtil;
import org.apache.wss4j.dom.util.EncryptionUtils;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.dom.util.X509Util;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SignatureProcessor implements Processor {
    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(SignatureProcessor.class);

    private XMLSignatureFactory signatureFactory;

    public SignatureProcessor() {
        init(null);
    }

    public SignatureProcessor(Provider provider) {
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

    public List<WSSecurityEngineResult> handleToken(
        Element elem,
        RequestData data
    ) throws WSSecurityException {
        LOG.debug("Found signature element");
        Element keyInfoElement =
            XMLUtils.getDirectChildElement(
                elem,
                "KeyInfo",
                WSConstants.SIG_NS
            );
        X509Certificate[] certs = null;
        Principal principal = null;
        PublicKey publicKey = null;
        byte[] secretKey = null;
        String signatureMethod = getSignatureMethod(elem);
        REFERENCE_TYPE referenceType = null;

        Credential credential = new Credential();
        Validator validator = data.getValidator(WSConstants.SIGNATURE);
        if (keyInfoElement == null) {
            certs = getDefaultCerts(data.getSigVerCrypto());
            principal = certs[0].getSubjectX500Principal();
        } else {
            int result = 0;
            Node node = keyInfoElement.getFirstChild();
            Element child = null;
            while (node != null) {
                if (Node.ELEMENT_NODE == node.getNodeType()) {
                    result++;
                    child = (Element)node;
                }
                node = node.getNextSibling();
            }
            if (result != 1) {
                data.getBSPEnforcer().handleBSPRule(BSPRule.R5402);
            }

            if (!(SecurityTokenReference.SECURITY_TOKEN_REFERENCE.equals(child.getLocalName())
                && WSConstants.WSSE_NS.equals(child.getNamespaceURI()))) {
                data.getBSPEnforcer().handleBSPRule(BSPRule.R5417);

                publicKey = X509Util.parseKeyValue(keyInfoElement, signatureFactory);
                if (validator != null) {
                    credential.setPublicKey(publicKey);
                    principal = new PublicKeyPrincipalImpl(publicKey);
                    credential.setPrincipal(principal);
                    credential = validator.validate(credential, data);
                }
            } else {
                STRParserParameters parameters = new STRParserParameters();
                parameters.setData(data);
                parameters.setStrElement(child);
                if (signatureMethod != null) {
                    parameters.setDerivationKeyLength(KeyUtils.getKeyLength(signatureMethod));
                }

                STRParser strParser = new SignatureSTRParser();
                STRParserResult parserResult = strParser.parseSecurityTokenReference(parameters);
                principal = parserResult.getPrincipal();
                certs = parserResult.getCertificates();
                publicKey = parserResult.getPublicKey();
                secretKey = parserResult.getSecretKey();
                referenceType = parserResult.getCertificatesReferenceType();

                boolean trusted = parserResult.isTrustedCredential();
                if (trusted) {
                    LOG.debug("Direct Trust for SAML/BST credential");
                }
                if (!trusted && (publicKey != null || certs != null) && validator != null) {
                    credential.setPublicKey(publicKey);
                    credential.setCertificates(certs);
                    credential.setPrincipal(principal);
                    credential = validator.validate(credential, data);
                }
            }
        }

        //
        // Check that we have a certificate, a public key or a secret key with which to
        // perform signature verification
        //
        if ((certs == null || certs.length == 0 || certs[0] == null)
            && secretKey == null
            && publicKey == null) {
            LOG.debug("No certificates or keys were found with which to validate the signature");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK);
        }

        // Check for compliance against the defined AlgorithmSuite
        AlgorithmSuite algorithmSuite = data.getAlgorithmSuite();
        if (algorithmSuite != null) {
            AlgorithmSuiteValidator algorithmSuiteValidator = new
                AlgorithmSuiteValidator(algorithmSuite);

            if (principal instanceof WSDerivedKeyTokenPrincipal) {
                algorithmSuiteValidator.checkDerivedKeyAlgorithm(
                    ((WSDerivedKeyTokenPrincipal)principal).getAlgorithm()
                );
                algorithmSuiteValidator.checkSignatureDerivedKeyLength(
                    ((WSDerivedKeyTokenPrincipal)principal).getLength()
                );
            } else {
                if (certs != null && certs.length > 0) {
                    algorithmSuiteValidator.checkAsymmetricKeyLength(certs);
                } else if (publicKey != null) {
                    algorithmSuiteValidator.checkAsymmetricKeyLength(publicKey);
                } else if (secretKey != null) {
                    algorithmSuiteValidator.checkSymmetricKeyLength(secretKey.length);
                }
            }
        }

        XMLSignature xmlSignature =
            verifyXMLSignature(elem, certs, publicKey, secretKey, signatureMethod, data, data.getWsDocInfo());
        byte[] signatureValue = xmlSignature.getSignatureValue().getValue();
        String c14nMethod = xmlSignature.getSignedInfo().getCanonicalizationMethod().getAlgorithm();

        List<WSDataRef> dataRefs =
            buildProtectedRefs(
                elem.getOwnerDocument(), xmlSignature.getSignedInfo(), data, data.getWsDocInfo()
            );
        if (dataRefs.isEmpty()) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK);
        }

        int actionPerformed = WSConstants.SIGN;
        if (principal instanceof UsernameTokenPrincipal) {
            actionPerformed = WSConstants.UT_SIGN;
        }

        WSSecurityEngineResult result = new WSSecurityEngineResult(
                actionPerformed, principal,
                certs, dataRefs, signatureValue);
        result.put(WSSecurityEngineResult.TAG_SIGNATURE_METHOD, signatureMethod);
        result.put(WSSecurityEngineResult.TAG_CANONICALIZATION_METHOD, c14nMethod);
        String tokenId = elem.getAttributeNS(null, "Id");
        if (tokenId.length() != 0) {
            result.put(WSSecurityEngineResult.TAG_ID, tokenId);
        }
        result.put(WSSecurityEngineResult.TAG_SECRET, secretKey);
        result.put(WSSecurityEngineResult.TAG_PUBLIC_KEY, publicKey);
        result.put(WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE, referenceType);
        result.put(WSSecurityEngineResult.TAG_TOKEN_ELEMENT, elem);
        if (validator != null) {
            result.put(WSSecurityEngineResult.TAG_VALIDATED_TOKEN, Boolean.TRUE);
            if (credential != null) {
                result.put(WSSecurityEngineResult.TAG_SUBJECT, credential.getSubject());
            }
        }
        data.getWsDocInfo().addResult(result);
        data.getWsDocInfo().addTokenElement(elem);
        return java.util.Collections.singletonList(result);
    }

    /**
     * Get the default certificates from the KeyStore
     * @param crypto The Crypto object containing the default alias
     * @return The default certificates
     * @throws WSSecurityException
     */
    private X509Certificate[] getDefaultCerts(
        Crypto crypto
    ) throws WSSecurityException {
        if (crypto == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noSigCryptoFile");
        }
        if (crypto.getDefaultX509Identifier() != null) {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias(crypto.getDefaultX509Identifier());
            return crypto.getX509Certificates(cryptoType);
        } else {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.INVALID_SECURITY, "unsupportedKeyInfo"
            );
        }
    }

    /**
     * Verify the WS-Security signature.
     *
     * The functions at first checks if then <code>KeyInfo</code> that is
     * contained in the signature contains standard X509 data. If yes then
     * get the certificate data via the standard <code>KeyInfo</code> methods.
     *
     * Otherwise, if the <code>KeyInfo</code> info does not contain X509 data, check
     * if we can find a <code>wsse:SecurityTokenReference</code> element. If yes, the next
     * step is to check how to get the certificate. Two methods are currently supported
     * here:
     * <ul>
     * <li> A URI reference to a binary security token contained in the <code>wsse:Security
     * </code> header.  If the dereferenced token is
     * of the correct type the contained certificate is extracted.
     * </li>
     * <li> Issuer name an serial number of the certificate. In this case the method
     * looks up the certificate in the keystore via the <code>crypto</code> parameter.
     * </li>
     * </ul>
     *
     * @param elem        the XMLSignature DOM Element.
     * @return the subject principal of the validated X509 certificate (the
     *         authenticated subject). The calling function may use this
     *         principal for further authentication or authorization.
     * @throws WSSecurityException
     */
    private XMLSignature verifyXMLSignature(
        Element elem,
        X509Certificate[] certs,
        PublicKey publicKey,
        byte[] secretKey,
        String signatureMethod,
        final RequestData data,
        WSDocInfo wsDocInfo
    ) throws WSSecurityException {
        LOG.debug("Verify XML Signature");

        //
        // Perform the signature verification and build up a List of elements that the
        // signature refers to
        //
        Key key = null;
        if (certs != null && certs[0] != null) {
            key = certs[0].getPublicKey();
        } else if (publicKey != null) {
            key = publicKey;
        } else {
            key = KeyUtils.prepareSecretKey(signatureMethod, secretKey);
        }

        if (data.isExpandXopInclude()) {
            // Look for xop:Include Nodes and expand them
            List<Element> includeElements =
                XMLUtils.findElements(elem.getFirstChild(), "Include", WSConstants.XOP_NS);
            WSSecurityUtil.inlineAttachments(includeElements, data.getAttachmentCallbackHandler(), true);
        }

        XMLValidateContext context = new DOMValidateContext(key, elem);
        context.setProperty("javax.xml.crypto.dsig.cacheReference", Boolean.TRUE);
        context.setProperty("org.apache.jcp.xml.dsig.secureValidation", Boolean.TRUE);
        context.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
        context.setProperty(STRTransform.TRANSFORM_WS_DOC_INFO, wsDocInfo);
        if (data.getSignatureProvider() != null) {
            context.setProperty("org.jcp.xml.dsig.internal.dom.SignatureProvider", data.getSignatureProvider());
        }

        context.setProperty(AttachmentContentSignatureTransform.ATTACHMENT_CALLBACKHANDLER,
                            data.getAttachmentCallbackHandler());

        try {
            XMLSignature xmlSignature = signatureFactory.unmarshalXMLSignature(context);
            checkBSPCompliance(xmlSignature, data.getBSPEnforcer());

            // Check for compliance against the defined AlgorithmSuite
            AlgorithmSuite algorithmSuite = data.getAlgorithmSuite();
            if (algorithmSuite != null) {
                AlgorithmSuiteValidator algorithmSuiteValidator = new
                    AlgorithmSuiteValidator(algorithmSuite);
                algorithmSuiteValidator.checkSignatureAlgorithms(xmlSignature);
            }

            // Test for replay attacks
            testMessageReplay(elem, xmlSignature.getSignatureValue().getValue(), key, data, wsDocInfo);

            setElementsOnContext(xmlSignature, (DOMValidateContext)context, data, wsDocInfo);

            boolean signatureOk = xmlSignature.validate(context);
            if (signatureOk) {
                return xmlSignature;
            }
            //
            // Log the exact signature error
            //
            if (LOG.isDebugEnabled()) {
                LOG.warn("XML Signature verification has failed");
                boolean signatureValidationCheck =
                    xmlSignature.getSignatureValue().validate(context);
                LOG.debug("Signature Validation check: " + signatureValidationCheck);
                java.util.Iterator<?> referenceIterator =
                    xmlSignature.getSignedInfo().getReferences().iterator();
                while (referenceIterator.hasNext()) {
                    Reference reference = (Reference)referenceIterator.next();
                    boolean referenceValidationCheck = reference.validate(context);
                    String id = reference.getId();
                    if (id == null) {
                        id = reference.getURI();
                    }
                    LOG.debug("Reference " + id + " check: " + referenceValidationCheck);
                }
            }
        } catch (WSSecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILED_CHECK, ex
            );
        }
        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK);
    }

    /**
     * Retrieve the Reference elements and set them on the ValidateContext
     * @param xmlSignature the XMLSignature object to get the references from
     * @param context the ValidateContext
     * @param data The RequestData object
     * @param wsDocInfo the WSDocInfo object where tokens are stored
     * @throws WSSecurityException
     */
    private void setElementsOnContext(
        XMLSignature xmlSignature,
        DOMValidateContext context,
        RequestData data,
        WSDocInfo wsDocInfo
    ) throws WSSecurityException {
        java.util.Iterator<?> referenceIterator =
            xmlSignature.getSignedInfo().getReferences().iterator();
        CallbackLookup callbackLookup = wsDocInfo.getCallbackLookup();
        while (referenceIterator.hasNext()) {
            Reference reference = (Reference)referenceIterator.next();
            String uri = reference.getURI();
            Element element = callbackLookup.getAndRegisterElement(uri, null, true, context);
            if (element == null) {
                wsDocInfo.setTokenOnContext(uri, context);
            } else if ("BinarySecurityToken".equals(element.getLocalName())
                && WSConstants.WSSE_NS.equals(element.getNamespaceURI())
                && isXopInclude(element)) {
                // We don't write out the xop:Include bytes into the BinarySecurityToken by default
                // But if the BST is signed, then we have to, or else Signature validation fails...
                handleXopInclude(element, wsDocInfo);
            } else if (data.isExpandXopInclude() && element.getFirstChild() != null) {
                // Look for xop:Include Nodes
                List<Element> includeElements =
                    XMLUtils.findElements(element.getFirstChild(), "Include", WSConstants.XOP_NS);
                WSSecurityUtil.inlineAttachments(includeElements, data.getAttachmentCallbackHandler(), true);
            }
        }
    }

    private boolean isXopInclude(Element element) {
        Element elementChild =
            XMLUtils.getDirectChildElement(element, "Include", WSConstants.XOP_NS);
        if (elementChild != null && elementChild.hasAttributeNS(null, "href")) {
            String xopUri = elementChild.getAttributeNS(null, "href");
            if (xopUri != null && xopUri.startsWith("cid:")) {
                return true;
            }
        }
        return false;
    }

    private void handleXopInclude(Element element, WSDocInfo wsDocInfo) {
        Map<Integer, List<WSSecurityEngineResult>> actionResults = wsDocInfo.getActionResults();
        if (actionResults != null && actionResults.containsKey(WSConstants.BST)) {
            for (WSSecurityEngineResult result : actionResults.get(WSConstants.BST)) {
                Element token = (Element)result.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
                if (element.equals(token)) {
                    BinarySecurity binarySecurity =
                        (BinarySecurity)result.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                    binarySecurity.encodeRawToken();
                    return;
                }
            }
        }
    }

    /**
     * Get the signature method algorithm URI from the associated signature element.
     * @param signatureElement The signature element
     * @return the signature method URI
     */
    private static String getSignatureMethod(
        Element signatureElement
    ) {
        Element signedInfoElement =
            XMLUtils.getDirectChildElement(
                signatureElement,
                "SignedInfo",
                WSConstants.SIG_NS
            );
        if (signedInfoElement != null) {
            Element signatureMethodElement =
                XMLUtils.getDirectChildElement(
                    signedInfoElement,
                    "SignatureMethod",
                    WSConstants.SIG_NS
                );
            if (signatureMethodElement != null) {
                return signatureMethodElement.getAttributeNS(null, "Algorithm");
            }
        }
        return null;
    }


    /**
     * This method digs into the Signature element to get the elements that
     * this Signature covers. Build the QName of these Elements and return them
     * to caller
     * @param doc The owning document
     * @param signedInfo The SignedInfo object
     * @param requestData A RequestData instance
     * @return A list of protected references
     * @throws WSSecurityException
     */
    private List<WSDataRef> buildProtectedRefs(
        Document doc,
        SignedInfo signedInfo,
        RequestData requestData,
        WSDocInfo wsDocInfo
    ) throws WSSecurityException {
        List<WSDataRef> protectedRefs = new ArrayList<>(signedInfo.getReferences().size());
        for (Object reference : signedInfo.getReferences()) {
            Reference siRef = (Reference)reference;
            String uri = siRef.getURI();

            if (uri.length() != 0) {
                Element se = dereferenceSTR(doc, siRef, requestData, wsDocInfo);
                // If an STR Transform is not used then just find the cached element
                boolean attachment = false;
                if (se == null) {
                    Data dereferencedData = siRef.getDereferencedData();
                    if (dereferencedData instanceof NodeSetData) {
                        NodeSetData data = (NodeSetData)dereferencedData;
                        java.util.Iterator<?> iter = data.iterator();

                        while (iter.hasNext()) {
                            Node n = (Node)iter.next();
                            if (n instanceof Element) {
                                se = (Element)n;
                                break;
                            }
                        }
                    } else if (dereferencedData instanceof OctetStreamData) {
                        se = doc.createElementNS("http://docs.oasis-open.org/wss/oasis-wss-SwAProfile-1.1",
                                                 "attachment");
                        attachment = true;
                    }
                }
                if (se == null) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK);
                }

                WSDataRef ref = new WSDataRef();
                ref.setWsuId(uri);
                ref.setProtectedElement(se);
                ref.setAlgorithm(signedInfo.getSignatureMethod().getAlgorithm());
                ref.setDigestAlgorithm(siRef.getDigestMethod().getAlgorithm());
                ref.setDigestValue(siRef.getDigestValue());
                ref.setAttachment(attachment);

                // Set the Transform algorithms as well
                @SuppressWarnings("unchecked")
                List<Transform> transforms = siRef.getTransforms();
                List<String> transformAlgorithms = new ArrayList<>(transforms.size());
                for (Transform transform : transforms) {
                    transformAlgorithms.add(transform.getAlgorithm());
                }
                ref.setTransformAlgorithms(transformAlgorithms);

                ref.setXpath(EncryptionUtils.getXPath(se));
                protectedRefs.add(ref);
            }
        }
        return protectedRefs;
    }

    /**
     * Check to see if a SecurityTokenReference transform was used, if so then return the
     * dereferenced element.
     */
    private Element dereferenceSTR(
        Document doc,
        Reference siRef,
        RequestData requestData,
        WSDocInfo wsDocInfo
    ) throws WSSecurityException {

        for (Object transformObject : siRef.getTransforms()) {

            Transform transform = (Transform)transformObject;

            if (STRTransform.TRANSFORM_URI.equals(transform.getAlgorithm())) {
                NodeSetData data = (NodeSetData)siRef.getDereferencedData();
                if (data != null) {
                    java.util.Iterator<?> iter = data.iterator();

                    Node securityTokenReference = null;
                    while (iter.hasNext()) {
                        Node node = (Node)iter.next();
                        if ("SecurityTokenReference".equals(node.getLocalName())) {
                            securityTokenReference = node;
                            break;
                        }
                    }

                    if (securityTokenReference != null) {
                        SecurityTokenReference secTokenRef =
                            new SecurityTokenReference(
                                (Element)securityTokenReference,
                                requestData.getBSPEnforcer()
                            );
                        Element se = STRTransformUtil.dereferenceSTR(doc, secTokenRef, wsDocInfo);
                        if (se != null) {
                            return se;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Test for a replayed message. The cache key is the Timestamp Created String, the signature
     * value, and the encoded value of the signing key.
     * @param signatureElement
     * @param signatureValue
     * @param key
     * @param requestData
     * @param wsDocInfo
     * @throws WSSecurityException
     */
    private void testMessageReplay(
        Element signatureElement,
        byte[] signatureValue,
        Key key,
        RequestData requestData,
        WSDocInfo wsDocInfo
    ) throws WSSecurityException {
        ReplayCache replayCache = requestData.getTimestampReplayCache();
        if (replayCache == null) {
            return;
        }

        // Find the Timestamp
        List<WSSecurityEngineResult> foundResults = wsDocInfo.getResultsByTag(WSConstants.TS);
        Timestamp timeStamp = null;
        if (foundResults.isEmpty()) {
            // Search for a Timestamp below the Signature
            Node sibling = signatureElement.getNextSibling();
            while (sibling != null) {
                if (sibling instanceof Element
                    && WSConstants.TIMESTAMP_TOKEN_LN.equals(sibling.getLocalName())
                    && WSConstants.WSU_NS.equals(sibling.getNamespaceURI())) {
                    timeStamp = new Timestamp((Element)sibling, requestData.getBSPEnforcer());
                    break;
                }
                sibling = sibling.getNextSibling();
            }
        } else {
            timeStamp = (Timestamp)foundResults.get(0).get(WSSecurityEngineResult.TAG_TIMESTAMP);
        }
        if (timeStamp == null) {
            return;
        }

        // Test for replay attacks
        String identifier = timeStamp.getCreatedString() + "" + Arrays.hashCode(signatureValue)
            + "" + Arrays.hashCode(key.getEncoded());

        if (replayCache.contains(identifier)) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.INVALID_SECURITY,
                "invalidTimestamp",
                new Object[] {"A replay attack has been detected"});
        }

        // Store the Timestamp/SignatureValue/Key combination in the cache
        if (timeStamp.getExpires() != null) {
            replayCache.add(identifier, timeStamp.getExpires());
        } else {
            replayCache.add(identifier);
        }
    }

    /**
     * Check BSP compliance (Note some other checks are done elsewhere in this class)
     * @throws WSSecurityException
     */
    private void checkBSPCompliance(
        XMLSignature xmlSignature,
        BSPEnforcer bspEnforcer
    ) throws WSSecurityException {
        // Check for Manifests
        for (Object object : xmlSignature.getObjects()) {
            if (object instanceof XMLObject) {
                XMLObject xmlObject = (XMLObject)object;
                for (Object xmlStructure : xmlObject.getContent()) {
                    if (xmlStructure instanceof Manifest) {
                        bspEnforcer.handleBSPRule(BSPRule.R5403);
                    }
                }
            }
        }

        // Check the c14n algorithm
        String c14nMethod =
            xmlSignature.getSignedInfo().getCanonicalizationMethod().getAlgorithm();
        if (!WSConstants.C14N_EXCL_OMIT_COMMENTS.equals(c14nMethod)) {
            bspEnforcer.handleBSPRule(BSPRule.R5404);
        }

        // Not allowed HMAC OutputLength
        AlgorithmParameterSpec parameterSpec =
            xmlSignature.getSignedInfo().getSignatureMethod().getParameterSpec();
        if (parameterSpec instanceof HMACParameterSpec) {
            bspEnforcer.handleBSPRule(BSPRule.R5401);
        }

        // Must have exclusive C14N without comments
        parameterSpec =
            xmlSignature.getSignedInfo().getCanonicalizationMethod().getParameterSpec();
        if (parameterSpec != null && !(parameterSpec instanceof ExcC14NParameterSpec)) {
            bspEnforcer.handleBSPRule(BSPRule.R5404);
        }

        // Check References
        for (Object refObject : xmlSignature.getSignedInfo().getReferences()) {
            Reference reference = (Reference)refObject;
            if (reference.getTransforms().isEmpty()) {
                bspEnforcer.handleBSPRule(BSPRule.R5416);
            }
            for (int i = 0; i < reference.getTransforms().size(); i++) {
                Transform transform = (Transform)reference.getTransforms().get(i);
                String algorithm = transform.getAlgorithm();
                if (!(WSConstants.C14N_EXCL_OMIT_COMMENTS.equals(algorithm)
                    || STRTransform.TRANSFORM_URI.equals(algorithm)
                    || WSConstants.NS_XMLDSIG_FILTER2.equals(algorithm)
                    || WSConstants.NS_XMLDSIG_ENVELOPED_SIGNATURE.equals(algorithm)
                    || WSConstants.SWA_ATTACHMENT_COMPLETE_SIG_TRANS.equals(algorithm)
                    || WSConstants.SWA_ATTACHMENT_CONTENT_SIG_TRANS.equals(algorithm))) {
                    bspEnforcer.handleBSPRule(BSPRule.R5423);
                }
                if (i == (reference.getTransforms().size() - 1)
                    && !(WSConstants.C14N_EXCL_OMIT_COMMENTS.equals(algorithm)
                        || STRTransform.TRANSFORM_URI.equals(algorithm)
                        || WSConstants.SWA_ATTACHMENT_COMPLETE_SIG_TRANS.equals(algorithm)
                        || WSConstants.SWA_ATTACHMENT_CONTENT_SIG_TRANS.equals(algorithm))) {
                    bspEnforcer.handleBSPRule(BSPRule.R5412);
                }

                if (WSConstants.C14N_EXCL_OMIT_COMMENTS.equals(algorithm)) {
                    parameterSpec = transform.getParameterSpec();
                    if (parameterSpec != null && !(parameterSpec instanceof ExcC14NParameterSpec)) {
                        bspEnforcer.handleBSPRule(BSPRule.R5407);
                    }
                }
            }
        }
    }

}

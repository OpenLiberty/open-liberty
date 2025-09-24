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

import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.XMLValidateContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.namespace.QName;

import org.apache.wss4j.common.crypto.AlgorithmSuite;
import org.apache.wss4j.common.crypto.AlgorithmSuiteValidator;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.SAMLTokenPrincipalImpl;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.saml.WSSSAMLKeyInfoProcessor;
import org.apache.wss4j.dom.util.EncryptionUtils;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.w3c.dom.Element;

public class SAMLTokenProcessor implements Processor {
    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(SAMLTokenProcessor.class);
    private XMLSignatureFactory signatureFactory;

    public SAMLTokenProcessor() {
        init(null);
    }

    public SAMLTokenProcessor(Provider provider) {
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
        LOG.debug("Found SAML Assertion element");

        Validator validator =
            data.getValidator(new QName(elem.getNamespaceURI(), elem.getLocalName()));

        SamlAssertionWrapper samlAssertion = new SamlAssertionWrapper(elem);
        XMLSignature xmlSignature = verifySignatureKeysAndAlgorithms(samlAssertion, data);
        List<WSDataRef> dataRefs = createDataRefs(elem, samlAssertion, xmlSignature);

        Credential credential = handleSAMLToken(samlAssertion, data, validator);
        samlAssertion = credential.getSamlAssertion();
        if (LOG.isDebugEnabled()) {
            LOG.debug("SAML Assertion issuer " + samlAssertion.getIssuerString());
            LOG.debug(DOM2Writer.nodeToString(elem));
        }

        // See if the token has been previously processed
        String id = samlAssertion.getId();
        Element foundElement = data.getWsDocInfo().getTokenElement(id);
        if (elem.equals(foundElement)) {
            WSSecurityEngineResult result = data.getWsDocInfo().getResult(id);
            return java.util.Collections.singletonList(result);
        } else if (foundElement != null) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, "duplicateError"
            );
        }

        data.getWsDocInfo().addTokenElement(elem);
        WSSecurityEngineResult result = null;
        if (samlAssertion.isSigned()) {
            result = new WSSecurityEngineResult(WSConstants.ST_SIGNED, samlAssertion);
            result.put(WSSecurityEngineResult.TAG_DATA_REF_URIS, dataRefs);
            result.put(WSSecurityEngineResult.TAG_SIGNATURE_VALUE, samlAssertion.getSignatureValue());
        } else {
            result = new WSSecurityEngineResult(WSConstants.ST_UNSIGNED, samlAssertion);
        }

        if (id.length() != 0) {
            result.put(WSSecurityEngineResult.TAG_ID, id);
        }

        if (validator != null) {
            result.put(WSSecurityEngineResult.TAG_VALIDATED_TOKEN, Boolean.TRUE);
            if (credential.getTransformedToken() != null) {
                result.put(
                    WSSecurityEngineResult.TAG_TRANSFORMED_TOKEN, credential.getTransformedToken()
                );
                if (credential.getPrincipal() != null) {
                    result.put(WSSecurityEngineResult.TAG_PRINCIPAL, credential.getPrincipal());
                } else {
                    SAMLTokenPrincipalImpl samlPrincipal =
                        new SAMLTokenPrincipalImpl(credential.getTransformedToken());
                    result.put(WSSecurityEngineResult.TAG_PRINCIPAL, samlPrincipal);
                }
            } else if (credential.getPrincipal() != null) {
                result.put(WSSecurityEngineResult.TAG_PRINCIPAL, credential.getPrincipal());
            } else {
                result.put(WSSecurityEngineResult.TAG_PRINCIPAL, new SAMLTokenPrincipalImpl(samlAssertion));
            }
            result.put(WSSecurityEngineResult.TAG_SUBJECT, credential.getSubject());
        }
        data.getWsDocInfo().addResult(result);
        return java.util.Collections.singletonList(result);
    }

    public Credential handleSAMLToken(
        SamlAssertionWrapper samlAssertion,
        RequestData data,
        Validator validator
    ) throws WSSecurityException {
        // Parse the subject if it exists
        samlAssertion.parseSubject(
            new WSSSAMLKeyInfoProcessor(data), data.getSigVerCrypto()
        );

        // Now delegate the rest of the verification to the Validator
        Credential credential = new Credential();
        credential.setSamlAssertion(samlAssertion);
        if (validator != null) {
            return validator.validate(credential, data);
        }
        return credential;
    }

    private XMLSignature verifySignatureKeysAndAlgorithms(
        SamlAssertionWrapper samlAssertion,
        RequestData data
    ) throws WSSecurityException {
        if (samlAssertion.isSigned()) {
            Signature sig = samlAssertion.getSignature();
            KeyInfo keyInfo = sig.getKeyInfo();
            if (keyInfo == null) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity",
                    new Object[] {"cannot get certificate or key"}
                );
            }
            SAMLKeyInfo samlKeyInfo =
                SAMLUtil.getCredentialFromKeyInfo(
                    keyInfo.getDOM(), new WSSSAMLKeyInfoProcessor(data), data.getSigVerCrypto()
                );

            PublicKey key = null;
            if (samlKeyInfo.getCerts() != null && samlKeyInfo.getCerts()[0] != null) {
                key = samlKeyInfo.getCerts()[0].getPublicKey();
            } else if (samlKeyInfo.getPublicKey() != null) {
                key = samlKeyInfo.getPublicKey();
            } else {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity",
                    new Object[] {"cannot get certificate or key"});
            }

            // Not checking signature here, just marshalling into an XMLSignature
            // structure for testing the transform/digest algorithms etc.
            XMLValidateContext context = new DOMValidateContext(key, sig.getDOM());
            context.setProperty("org.apache.jcp.xml.dsig.secureValidation", Boolean.TRUE);
            context.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
            if (data.getSignatureProvider() != null) {
                context.setProperty("org.jcp.xml.dsig.internal.dom.SignatureProvider", data.getSignatureProvider());
            }

            XMLSignature xmlSignature;
            try {
                xmlSignature = signatureFactory.unmarshalXMLSignature(context);
            } catch (MarshalException ex) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILED_CHECK, ex, "invalidSAMLsecurity",
                    new Object[] {"cannot get certificate or key"}
                );
            }

            // Check for compliance against the defined AlgorithmSuite
            AlgorithmSuite algorithmSuite = data.getSamlAlgorithmSuite();
            if (algorithmSuite != null) {
                AlgorithmSuiteValidator algorithmSuiteValidator = new
                    AlgorithmSuiteValidator(algorithmSuite);

                algorithmSuiteValidator.checkSignatureAlgorithms(xmlSignature);

                if (samlKeyInfo.getCerts() != null && samlKeyInfo.getCerts().length > 0) {
                    algorithmSuiteValidator.checkAsymmetricKeyLength(samlKeyInfo.getCerts());
                } else {
                    algorithmSuiteValidator.checkAsymmetricKeyLength(key);
                }
            }

            samlAssertion.verifySignature(samlKeyInfo);

            return xmlSignature;
        }

        return null;
    }

    private List<WSDataRef> createDataRefs(
        Element token, SamlAssertionWrapper samlAssertion, XMLSignature xmlSignature
    ) {
        if (xmlSignature == null) {
            return Collections.emptyList();
        }

        List<WSDataRef> protectedRefs = new ArrayList<>();
        String signatureMethod =
            xmlSignature.getSignedInfo().getSignatureMethod().getAlgorithm();

        for (Object refObject : xmlSignature.getSignedInfo().getReferences()) {
            Reference reference = (Reference)refObject;

            if (reference.getURI() == null || reference.getURI().length() == 0
                || reference.getURI().equals(samlAssertion.getId())
                || reference.getURI().equals("#" + samlAssertion.getId())) {
                WSDataRef ref = new WSDataRef();
                ref.setWsuId(reference.getURI());
                ref.setProtectedElement(token);
                ref.setAlgorithm(signatureMethod);
                ref.setDigestAlgorithm(reference.getDigestMethod().getAlgorithm());
                ref.setDigestValue(reference.getDigestValue());

                // Set the Transform algorithms as well
                @SuppressWarnings("unchecked")
                List<Transform> transforms = reference.getTransforms();
                List<String> transformAlgorithms = new ArrayList<>(transforms.size());
                for (Transform transform : transforms) {
                    transformAlgorithms.add(transform.getAlgorithm());
                }
                ref.setTransformAlgorithms(transformAlgorithms);

                ref.setXpath(EncryptionUtils.getXPath(token));
                protectedRefs.add(ref);
            }
        }

        return protectedRefs;
    }
}

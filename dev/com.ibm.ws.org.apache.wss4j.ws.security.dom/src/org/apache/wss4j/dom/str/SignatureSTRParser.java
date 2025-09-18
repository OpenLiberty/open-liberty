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

package org.apache.wss4j.dom.str;

import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.principal.SAMLTokenPrincipalImpl;
import org.apache.wss4j.common.principal.WSDerivedKeyTokenPrincipal;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.common.token.Reference;
import org.apache.wss4j.common.token.SecurityTokenReference;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.DerivedKeyToken;
import org.apache.wss4j.dom.message.token.SecurityContextToken;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.wss4j.dom.saml.WSSSAMLKeyInfoProcessor;
import org.w3c.dom.Element;

/**
 * This implementation of STRParser is for parsing a SecurityTokenReference element, found in the
 * KeyInfo element associated with a Signature element.
 */
public class SignatureSTRParser implements STRParser {

    /**
     * Parse a SecurityTokenReference element and extract credentials.
     *
     * @param parameters The parameters to parse
     * @return the STRParserResult Object containing the parsing results
     * @throws WSSecurityException
     */
    public STRParserResult parseSecurityTokenReference(STRParserParameters parameters) throws WSSecurityException {

        if (parameters == null || parameters.getData() == null || parameters.getData().getWsDocInfo() == null
            || parameters.getStrElement() == null) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, "invalidSTRParserParameter"
            );
        }

        SecurityTokenReference secRef =
            new SecurityTokenReference(parameters.getStrElement(), parameters.getData().getBSPEnforcer());
        //
        // Here we get some information about the document that is being
        // processed, in particular the crypto implementation, and already
        // detected BST that may be used later during dereferencing.
        //
        String uri = null;
        if (secRef.getReference() != null) {
            uri = secRef.getReference().getURI();
            uri = XMLUtils.getIDFromReference(uri);
        } else if (secRef.containsKeyIdentifier()) {
            uri = secRef.getKeyIdentifierValue();
        }

        WSSecurityEngineResult result = parameters.getData().getWsDocInfo().getResult(uri);
        if (result != null) {
            return processPreviousResult(result, secRef, parameters);
        }

        return processSTR(secRef, uri, parameters);
    }

    /**
     * A method to create a Principal from a SAML Assertion
     * @param samlAssertion An SamlAssertionWrapper object
     * @return A principal
     */
    private Principal createPrincipalFromSAML(
        SamlAssertionWrapper samlAssertion, STRParserResult parserResult
    ) {
        SAMLTokenPrincipalImpl samlPrincipal = new SAMLTokenPrincipalImpl(samlAssertion);
        String confirmMethod = null;
        List<String> methods = samlAssertion.getConfirmationMethods();
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        if (OpenSAMLUtil.isMethodHolderOfKey(confirmMethod) && samlAssertion.isSigned()) {
            parserResult.setTrustedCredential(true);
        }
        return samlPrincipal;
    }

    /**
     * Parse the KeyIdentifier for a SAML Assertion
     */
    private void parseSAMLKeyIdentifier(
        SecurityTokenReference secRef,
        RequestData data,
        STRParserResult parserResult
    ) throws WSSecurityException {
        String valueType = secRef.getKeyIdentifierValueType();
        byte[] secretKey = STRParserUtil.getSecretKeyFromToken(secRef.getKeyIdentifierValue(), valueType,
                                                               WSPasswordCallback.SECRET_KEY, data);
        if (secretKey == null) {
            SamlAssertionWrapper samlAssertion =
                STRParserUtil.getAssertionFromKeyIdentifier(
                    secRef, secRef.getElement(), data
                );
            STRParserUtil.checkSamlTokenBSPCompliance(secRef, samlAssertion, data.getBSPEnforcer());

            SAMLKeyInfo samlKi =
                SAMLUtil.getCredentialFromSubject(samlAssertion,
                        new WSSSAMLKeyInfoProcessor(data),
                        data.getSigVerCrypto(), data.getCallbackHandler());
            X509Certificate[] foundCerts = samlKi.getCerts();
            if (foundCerts != null && foundCerts.length > 0) {
                parserResult.setCerts(new X509Certificate[]{foundCerts[0]});
            }
            secretKey = samlKi.getSecret();
            parserResult.setPublicKey(samlKi.getPublicKey());
            parserResult.setPrincipal(createPrincipalFromSAML(samlAssertion, parserResult));
        }
        parserResult.setSecretKey(secretKey);
    }

    /**
     * Parse the KeyIdentifier for a BinarySecurityToken
     */
    private void parseBSTKeyIdentifier(
        SecurityTokenReference secRef,
        Crypto crypto,
        RequestData data,
        STRParserResult parserResult
    ) throws WSSecurityException {
        STRParserUtil.checkBinarySecurityBSPCompliance(secRef, null, data.getBSPEnforcer());

        String valueType = secRef.getKeyIdentifierValueType();
        if (WSConstants.WSS_KRB_KI_VALUE_TYPE.equals(valueType)) {
            byte[] secretKey =
                STRParserUtil.getSecretKeyFromToken(secRef.getKeyIdentifierValue(), valueType,
                                                    WSPasswordCallback.SECRET_KEY, data);
            if (secretKey == null) {
                byte[] keyBytes = secRef.getSKIBytes();
                List<WSSecurityEngineResult> resultsList =
                    data.getWsDocInfo().getResultsByTag(WSConstants.BST);
                for (WSSecurityEngineResult bstResult : resultsList) {
                    BinarySecurity bstToken =
                        (BinarySecurity)bstResult.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                    byte[] tokenDigest = KeyUtils.generateDigest(bstToken.getToken());
                    if (Arrays.equals(tokenDigest, keyBytes)) {
                        secretKey = (byte[])bstResult.get(WSSecurityEngineResult.TAG_SECRET);
                        parserResult.setPrincipal((Principal)bstResult.get(WSSecurityEngineResult.TAG_PRINCIPAL));
                        break;
                    }
                }
            } else {
                parserResult.setPrincipal(new CustomTokenPrincipal(secRef.getKeyIdentifierValue()));
            }
            parserResult.setSecretKey(secretKey);
        } else {
            X509Certificate[] foundCerts = secRef.getKeyIdentifier(crypto);
            if (foundCerts == null) {
                // The reference may be to a BST in the security header rather than in the keystore
                if (SecurityTokenReference.SKI_URI.equals(valueType)) {
                    byte[] skiBytes = secRef.getSKIBytes();
                    List<WSSecurityEngineResult> resultsList =
                        data.getWsDocInfo().getResultsByTag(WSConstants.BST);
                    for (WSSecurityEngineResult bstResult : resultsList) {
                        X509Certificate[] certs =
                            (X509Certificate[])bstResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATES);
                        if (certs != null
                            && Arrays.equals(skiBytes, crypto.getSKIBytesFromCert(certs[0]))) {
                            parserResult.setPrincipal((Principal)bstResult.get(WSSecurityEngineResult.TAG_PRINCIPAL));
                            foundCerts = certs;
                            break;
                        }
                    }
                } else if (SecurityTokenReference.THUMB_URI.equals(valueType)) {
                    String kiValue = secRef.getKeyIdentifierValue();
                    List<WSSecurityEngineResult> resultsList =
                        data.getWsDocInfo().getResultsByTag(WSConstants.BST);
                    for (WSSecurityEngineResult bstResult : resultsList) {
                        X509Certificate[] certs =
                            (X509Certificate[])bstResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATES);
                        if (certs != null) {
                            try {
                                byte[] digest = KeyUtils.generateDigest(certs[0].getEncoded());
                                if (Arrays.equals(org.apache.xml.security.utils.XMLUtils.decode(kiValue), digest)) {
                                    parserResult.setPrincipal((Principal)bstResult.get(WSSecurityEngineResult.TAG_PRINCIPAL));
                                    foundCerts = certs;
                                    break;
                                }
                            } catch (CertificateEncodingException ex) {
                                throw new WSSecurityException(
                                    WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, ex, "encodeError"
                                );
                            }
                        }
                    }
                }
            }
            if (foundCerts != null) {
                parserResult.setCerts(new X509Certificate[]{foundCerts[0]});
            }
        }
    }

    /**
     * Process a previous security result
     */
    private STRParserResult processPreviousResult(
        WSSecurityEngineResult result,
        SecurityTokenReference secRef,
        STRParserParameters parameters
    ) throws WSSecurityException {

        STRParserResult parserResult = new STRParserResult();
        RequestData data = parameters.getData();

        Integer action = (Integer) result.get(WSSecurityEngineResult.TAG_ACTION);
        if (action != null
            && (WSConstants.UT_NOPASSWORD == action.intValue() || WSConstants.UT == action.intValue())) {
            STRParserUtil.checkUsernameTokenBSPCompliance(secRef, data.getBSPEnforcer());

            UsernameToken usernameToken =
                (UsernameToken)result.get(WSSecurityEngineResult.TAG_USERNAME_TOKEN);

            parserResult.setSecretKey((byte[])result.get(WSSecurityEngineResult.TAG_SECRET));

            parserResult.setPrincipal(usernameToken.createPrincipal());
        } else if (action != null && WSConstants.BST == action.intValue()) {
            BinarySecurity token =
                (BinarySecurity)result.get(
                    WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN
                );
            STRParserUtil.checkBinarySecurityBSPCompliance(secRef, token, data.getBSPEnforcer());

            parserResult.setCerts(
                (X509Certificate[])result.get(WSSecurityEngineResult.TAG_X509_CERTIFICATES));
            parserResult.setSecretKey((byte[])result.get(WSSecurityEngineResult.TAG_SECRET));
            Boolean validatedToken =
                (Boolean)result.get(WSSecurityEngineResult.TAG_VALIDATED_TOKEN);
            if (validatedToken) {
                parserResult.setTrustedCredential(true);
            }
        } else if (action != null && WSConstants.ENCR == action.intValue()) {
            STRParserUtil.checkEncryptedKeyBSPCompliance(secRef, data.getBSPEnforcer());

            parserResult.setSecretKey((byte[])result.get(WSSecurityEngineResult.TAG_SECRET));
            String id = (String)result.get(WSSecurityEngineResult.TAG_ID);
            parserResult.setPrincipal(new CustomTokenPrincipal(id));
        } else if (action != null && WSConstants.SCT == action.intValue()) {
            parserResult.setSecretKey((byte[])result.get(WSSecurityEngineResult.TAG_SECRET));
            SecurityContextToken sct =
                (SecurityContextToken)result.get(
                        WSSecurityEngineResult.TAG_SECURITY_CONTEXT_TOKEN
                );
            parserResult.setPrincipal(new CustomTokenPrincipal(sct.getIdentifier()));
        } else if (action != null && WSConstants.DKT == action.intValue()) {
            DerivedKeyToken dkt =
                (DerivedKeyToken)result.get(WSSecurityEngineResult.TAG_DERIVED_KEY_TOKEN);
            int keyLength = dkt.getLength();
            if (keyLength <= 0 && parameters.getDerivationKeyLength() > 0) {
                keyLength = parameters.getDerivationKeyLength();
            }
            byte[] secret = (byte[])result.get(WSSecurityEngineResult.TAG_SECRET);
            Principal principal = dkt.createPrincipal();
            ((WSDerivedKeyTokenPrincipal)principal).setSecret(secret);
            parserResult.setPrincipal(principal);
            parserResult.setSecretKey(dkt.deriveKey(keyLength, secret));
        } else if (action != null
            && (WSConstants.ST_UNSIGNED == action.intValue() || WSConstants.ST_SIGNED == action.intValue())) {
            SamlAssertionWrapper samlAssertion =
                (SamlAssertionWrapper)result.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
            STRParserUtil.checkSamlTokenBSPCompliance(secRef, samlAssertion, data.getBSPEnforcer());

            SAMLKeyInfo keyInfo = samlAssertion.getSubjectKeyInfo();
            if (keyInfo == null) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity"
                );
            }
            X509Certificate[] foundCerts = keyInfo.getCerts();
            if (foundCerts != null) {
                parserResult.setCerts(new X509Certificate[]{foundCerts[0]});
            }
            parserResult.setSecretKey(keyInfo.getSecret());
            parserResult.setPublicKey(keyInfo.getPublicKey());
            parserResult.setPrincipal(createPrincipalFromSAML(samlAssertion, parserResult));
        }

        REFERENCE_TYPE referenceType = getReferenceType(secRef);
        if (referenceType != null) {
            parserResult.setReferenceType(referenceType);
        }

        return parserResult;
    }

    private STRParserResult processSTR(
        SecurityTokenReference secRef,
        String uri,
        STRParserParameters parameters
    ) throws WSSecurityException {
        STRParserResult parserResult = new STRParserResult();
        RequestData data = parameters.getData();
        WSDocInfo wsDocInfo = data.getWsDocInfo();
        Element strElement = parameters.getStrElement();

        if (secRef.containsReference()) {
            Reference reference = secRef.getReference();
            // Try asking the CallbackHandler for the secret key
            byte[] secretKey = STRParserUtil.getSecretKeyFromToken(uri, reference.getValueType(),
                                                                   WSPasswordCallback.SECRET_KEY,
                                                                   data);
            Principal principal = new CustomTokenPrincipal(uri);

            if (secretKey == null) {
                Element token =
                    STRParserUtil.getTokenElement(strElement.getOwnerDocument(), wsDocInfo, data.getCallbackHandler(),
                                                  uri, reference.getValueType());
                QName el = new QName(token.getNamespaceURI(), token.getLocalName());
                if (el.equals(WSConstants.BINARY_TOKEN)) {
                    Processor proc = data.getWssConfig().getProcessor(WSConstants.BINARY_TOKEN);
                    List<WSSecurityEngineResult> bstResult = proc.handleToken(token, parameters.getData());
                    BinarySecurity bstToken =
                        (BinarySecurity)bstResult.get(0).get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                    STRParserUtil.checkBinarySecurityBSPCompliance(
                        secRef, bstToken, data.getBSPEnforcer()
                    );

                    parserResult.setCerts(
                        (X509Certificate[])bstResult.get(0).get(WSSecurityEngineResult.TAG_X509_CERTIFICATES));
                    secretKey = (byte[])bstResult.get(0).get(WSSecurityEngineResult.TAG_SECRET);
                    principal = (Principal)bstResult.get(0).get(WSSecurityEngineResult.TAG_PRINCIPAL);
                } else if (el.equals(WSConstants.SAML_TOKEN) || el.equals(WSConstants.SAML2_TOKEN)) {
                    Processor proc = data.getWssConfig().getProcessor(WSConstants.SAML_TOKEN);
                    //
                    // Just check to see whether the token was processed or not
                    //
                    Element processedToken =
                        STRParserUtil.findProcessedTokenElement(
                            strElement.getOwnerDocument(), wsDocInfo,
                            data.getCallbackHandler(), uri, secRef.getReference().getValueType()
                        );
                    SamlAssertionWrapper samlAssertion = null;
                    if (processedToken == null) {
                        List<WSSecurityEngineResult> samlResult = proc.handleToken(token, data);
                        samlAssertion =
                            (SamlAssertionWrapper)samlResult.get(0).get(
                                WSSecurityEngineResult.TAG_SAML_ASSERTION
                            );
                    } else {
                        samlAssertion = new SamlAssertionWrapper(processedToken);
                        samlAssertion.parseSubject(
                            new WSSSAMLKeyInfoProcessor(data),
                            data.getSigVerCrypto(), data.getCallbackHandler()
                        );
                    }
                    STRParserUtil.checkSamlTokenBSPCompliance(secRef, samlAssertion, data.getBSPEnforcer());

                    SAMLKeyInfo keyInfo = samlAssertion.getSubjectKeyInfo();
                    X509Certificate[] foundCerts = keyInfo.getCerts();
                    if (foundCerts != null && foundCerts.length > 0) {
                        parserResult.setCerts(new X509Certificate[]{foundCerts[0]});
                    }
                    secretKey = keyInfo.getSecret();
                    principal = createPrincipalFromSAML(samlAssertion, parserResult);
                } else if (el.equals(WSConstants.ENCRYPTED_KEY)) {
                    STRParserUtil.checkEncryptedKeyBSPCompliance(secRef, data.getBSPEnforcer());
                    Processor proc = data.getWssConfig().getProcessor(WSConstants.ENCRYPTED_KEY);
                    List<WSSecurityEngineResult> encrResult = proc.handleToken(token, data);
                    secretKey =
                        (byte[])encrResult.get(0).get(WSSecurityEngineResult.TAG_SECRET);
                    principal = new CustomTokenPrincipal(token.getAttributeNS(null, "Id"));
                }
            }

            parserResult.setSecretKey(secretKey);
            parserResult.setPrincipal(principal);
        } else if (secRef.containsX509Data() || secRef.containsX509IssuerSerial()) {
            parserResult.setReferenceType(REFERENCE_TYPE.ISSUER_SERIAL);
            Crypto crypto = data.getSigVerCrypto();
            X509Certificate[] foundCerts = secRef.getX509IssuerSerial(crypto);
            if (foundCerts != null && foundCerts.length > 0) {
                parserResult.setCerts(new X509Certificate[]{foundCerts[0]});
            }
        } else if (secRef.containsKeyIdentifier()) {
            if (secRef.getKeyIdentifierValueType().equals(SecurityTokenReference.ENC_KEY_SHA1_URI)) {
                STRParserUtil.checkEncryptedKeyBSPCompliance(secRef, data.getBSPEnforcer());

                String id = secRef.getKeyIdentifierValue();
                parserResult.setSecretKey(
                    STRParserUtil.getSecretKeyFromToken(id, SecurityTokenReference.ENC_KEY_SHA1_URI,
                                                        WSPasswordCallback.SECRET_KEY, data));
                parserResult.setPrincipal(new CustomTokenPrincipal(id));
            } else if (WSConstants.WSS_SAML_KI_VALUE_TYPE.equals(secRef.getKeyIdentifierValueType())
                || WSConstants.WSS_SAML2_KI_VALUE_TYPE.equals(secRef.getKeyIdentifierValueType())) {
                parseSAMLKeyIdentifier(secRef, data, parserResult);
            } else {
                Crypto crypto = data.getSigVerCrypto();
                parseBSTKeyIdentifier(secRef, crypto, data, parserResult);
            }
        } else {
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.INVALID_SECURITY,
                    "unsupportedKeyInfo", new Object[] {strElement.toString()});
        }

        REFERENCE_TYPE referenceType = getReferenceType(secRef);
        if (referenceType != null) {
            parserResult.setReferenceType(referenceType);
        }

        return parserResult;
    }

    private REFERENCE_TYPE getReferenceType(SecurityTokenReference secRef) {
        if (secRef.containsReference()) {
            return REFERENCE_TYPE.DIRECT_REF;
        } else if (secRef.containsKeyIdentifier()) {
            if (SecurityTokenReference.THUMB_URI.equals(secRef.getKeyIdentifierValueType())) {
                return REFERENCE_TYPE.THUMBPRINT_SHA1;
            } else {
                return REFERENCE_TYPE.KEY_IDENTIFIER;
            }
        }

        return null;
    }

}

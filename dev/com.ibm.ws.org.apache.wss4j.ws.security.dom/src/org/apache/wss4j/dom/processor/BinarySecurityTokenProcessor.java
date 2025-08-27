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

import java.security.cert.X509Certificate;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.SAMLTokenPrincipalImpl;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.common.token.PKIPathSecurity;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.KerberosSecurity;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;
import org.w3c.dom.Element;

/**
 * Processor implementation to handle wsse:BinarySecurityToken elements
 */
public class BinarySecurityTokenProcessor implements Processor {

    /**
     * {@inheritDoc}
     */
    public List<WSSecurityEngineResult> handleToken(
        Element elem,
        RequestData data
    ) throws WSSecurityException {
        // See if the token has been previously processed
        String id = elem.getAttributeNS(WSConstants.WSU_NS, "Id");
        if (id.length() != 0) {
            Element foundElement = data.getWsDocInfo().getTokenElement(id);
            if (elem.equals(foundElement)) {
                WSSecurityEngineResult result = data.getWsDocInfo().getResult(id);
                return java.util.Collections.singletonList(result);
            } else if (foundElement != null) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, "duplicateError"
                );
            }
        }

        BinarySecurity token = createSecurityToken(elem, data);
        X509Certificate[] certs = null;
        Validator validator = data.getValidator(new QName(elem.getNamespaceURI(),
                                                          elem.getLocalName()));

        if (data.getSigVerCrypto() == null) {
            certs = getCertificatesTokenReference(token, data.getDecCrypto());
        } else {
            certs = getCertificatesTokenReference(token, data.getSigVerCrypto());
        }

        WSSecurityEngineResult result =
            new WSSecurityEngineResult(WSConstants.BST, token, certs);
        data.getWsDocInfo().addTokenElement(elem);
        if (id.length() != 0) {
            result.put(WSSecurityEngineResult.TAG_ID, id);
        }

        if (validator != null) {
            // Hook to allow the user to validate the BinarySecurityToken
            Credential credential = new Credential();
            credential.setBinarySecurityToken(token);
            credential.setCertificates(certs);

            Credential returnedCredential = validator.validate(credential, data);
            result.put(WSSecurityEngineResult.TAG_VALIDATED_TOKEN, Boolean.TRUE);
            result.put(WSSecurityEngineResult.TAG_SECRET, returnedCredential.getSecretKey());

            if (returnedCredential.getTransformedToken() != null) {
                result.put(
                    WSSecurityEngineResult.TAG_TRANSFORMED_TOKEN,
                    returnedCredential.getTransformedToken()
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
            } else if (certs != null && certs.length > 0 && certs[0] != null) {
                result.put(WSSecurityEngineResult.TAG_PRINCIPAL, certs[0].getSubjectX500Principal());
            }
            result.put(WSSecurityEngineResult.TAG_SUBJECT, credential.getSubject());

            if (credential.getDelegationCredential() != null) {
                result.put(WSSecurityEngineResult.TAG_DELEGATION_CREDENTIAL,
                           credential.getDelegationCredential());
            }
        }

        data.getWsDocInfo().addResult(result);
        return java.util.Collections.singletonList(result);
    }

    /**
     * Extracts the certificate(s) from the Binary Security token reference.
     *
     * @param token The BinarySecurity instance corresponding to either X509Security or
     *              PKIPathSecurity
     * @return The X509Certificates associated with this reference
     * @throws WSSecurityException
     */
    private X509Certificate[] getCertificatesTokenReference(BinarySecurity token, Crypto crypto)
        throws WSSecurityException {
        if (token instanceof PKIPathSecurity) {
            return ((PKIPathSecurity) token).getX509Certificates(crypto);
        } else if (token instanceof X509Security) {
            X509Certificate cert = ((X509Security) token).getX509Certificate(crypto);
            return new X509Certificate[]{cert};
        }
        return new X509Certificate[0];
    }

    /**
     * Checks the <code>element</code> and creates appropriate binary security object.
     *
     * @param element The XML element that contains either a <code>BinarySecurityToken
     *                </code> or a <code>PKIPath</code> element.
     * @param data A RequestData instance
     * @return a BinarySecurity token element
     * @throws WSSecurityException
     */
    private BinarySecurity createSecurityToken(
        Element element,
        RequestData data
    ) throws WSSecurityException {
        String type = element.getAttributeNS(null, "ValueType");
        BinarySecurity token = null;
        if (X509Security.X509_V3_TYPE.equals(type)) {
            token = new X509Security(element, data.getBSPEnforcer());
        } else if (PKIPathSecurity.getType().equals(type)) {
            token = new PKIPathSecurity(element, data.getBSPEnforcer());
        } else if (KerberosSecurity.isKerberosToken(type)) {
            token = new KerberosSecurity(element, data.getBSPEnforcer());
        } else {
            token = new BinarySecurity(element, data.getBSPEnforcer());
        }

        // Now see if the Element content is actually referenced via xop:Include
        Element elementChild =
            XMLUtils.getDirectChildElement(element, "Include", WSConstants.XOP_NS);
        if (elementChild != null && elementChild.hasAttributeNS(null, "href")) {
            String xopUri = elementChild.getAttributeNS(null, "href");
            if (xopUri != null && xopUri.startsWith("cid:")) {
                byte[] content = WSSecurityUtil.getBytesFromAttachment(xopUri, data);
                token.setRawToken(content);
            }
        }

        return token;
    }

}

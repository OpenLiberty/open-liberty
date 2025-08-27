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

package org.apache.wss4j.dom.transform;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.Reference;
import org.apache.wss4j.common.token.SecurityTokenReference;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.str.STRParserUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * Utility class exposing the dereferencing LOG.c of the {@link STRTransform} implementation.
 */
public final class STRTransformUtil {
    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(STRTransformUtil.class);

    /**
     * Retrieves the element representing the referenced content of a STR.
     *
     * @return the element representing the referenced content. The element is either
     *         extracted from {@code doc} or a new element is created in the
     *         case of a key identifier or issuer serial STR.  {@code null} if
     *         {@code secRef} does not contain a direct reference, key identifier, or
     *         issuer serial.
     * @throws WSSecurityException
     *             If an issuer serial or key identifier is used in the STR and
     *             the certificate cannot be resolved from the crypto
     *             configuration or if there is an error working with the resolved
     *             cert
     */
    public static Element dereferenceSTR(Document doc,
            SecurityTokenReference secRef, WSDocInfo wsDocInfo) throws WSSecurityException {
        //
        // First case: direct reference, according to chap 7.2 of OASIS WS
        // specification (main document). Only in this case return a true
        // reference to the BST or Assertion. Copying is done by the caller.
        //
        if (secRef.containsReference()) {
            LOG.debug("STR: Reference");

            Reference reference = secRef.getReference();
            return STRParserUtil.getTokenElement(doc, wsDocInfo, null, reference.getURI(), reference.getValueType());
        } else if (secRef.containsX509Data() || secRef.containsX509IssuerSerial()) {
            //
            // second case: IssuerSerial, lookup in keystore, wrap in BST according
            // to specification
            //
            LOG.debug("STR: IssuerSerial");
            X509Certificate[] certs =
                secRef.getX509IssuerSerial(wsDocInfo.getCrypto());
            if (certs == null || certs.length == 0 || certs[0] == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK);
            }
            return createBSTX509(doc, certs[0], secRef.getElement(), secRef.getKeyIdentifierEncodingType());
        } else if (secRef.containsKeyIdentifier()) {
            //
            // third case: KeyIdentifier. For SKI, lookup in keystore, wrap in
            // BST according to specification. Otherwise if it's a wsse:KeyIdentifier it could
            // be a SAML assertion, so try and find the referenced element.
            //
            LOG.debug("STR: KeyIdentifier");
            if (WSConstants.WSS_SAML_KI_VALUE_TYPE.equals(secRef.getKeyIdentifierValueType())
                || WSConstants.WSS_SAML2_KI_VALUE_TYPE.equals(secRef.getKeyIdentifierValueType())) {
                return STRParserUtil.getTokenElement(doc, wsDocInfo, null, secRef.getKeyIdentifierValue(),
                                                     secRef.getKeyIdentifierValueType());
            } else {
                X509Certificate[] certs = secRef.getKeyIdentifier(wsDocInfo.getCrypto());
                if (certs == null || certs.length == 0 || certs[0] == null) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK);
                }
                return createBSTX509(doc, certs[0], secRef.getElement());
            }
        }
        return null;
    }

    public static Element createBSTX509(Document doc, X509Certificate cert, Element secRefE)
        throws WSSecurityException {
        return createBSTX509(doc, cert, secRefE, null);
    }

    public static Element createBSTX509(Document doc, X509Certificate cert, Element secRefE,
                                        String secRefEncType)
        throws WSSecurityException {
        byte[] data;
        try {
            data = cert.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e, "encodeError"
            );
        }
        String prefix = XMLUtils.getPrefixNS(WSConstants.WSSE_NS, secRefE);
        if (prefix == null) {
            prefix = WSConstants.WSSE_PREFIX;
        }
        Element elem = doc.createElementNS(WSConstants.WSSE_NS, prefix + ":BinarySecurityToken");
        XMLUtils.setNamespace(elem, WSConstants.WSSE_NS, prefix);
        // elem.setAttributeNS(WSConstants.XMLNS_NS, "xmlns", "");
        elem.setAttributeNS(null, "ValueType", X509Security.X509_V3_TYPE);
        if (secRefEncType != null) {
            elem.setAttributeNS(null, "EncodingType", secRefEncType);
        }
        Text certText = doc.createTextNode(org.apache.xml.security.utils.XMLUtils.encodeToString(data)); // no line wrap
        elem.appendChild(certText);
        return elem;
    }

    /**
     * Hidden in utility class.
     */
    private STRTransformUtil() {
    }

}

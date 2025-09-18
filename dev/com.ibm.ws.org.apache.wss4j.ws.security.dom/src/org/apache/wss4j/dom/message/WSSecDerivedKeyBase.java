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

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.crypto.SecretKey;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.derivedKey.AlgoFactory;
import org.apache.wss4j.common.derivedKey.ConversationConstants;
import org.apache.wss4j.common.derivedKey.DerivationAlgorithm;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.Reference;
import org.apache.wss4j.common.token.SecurityTokenReference;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.token.DerivedKeyToken;
import org.apache.wss4j.dom.message.token.KerberosSecurity;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Base class for DerivedKey encryption and signature
 */
public abstract class WSSecDerivedKeyBase extends WSSecSignatureBase {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(WSSecDerivedKeyBase.class);

    /**
     * DerivedKeyToken of this builder
     */
    private DerivedKeyToken dkt;

    /**
     * Client's label value
     */
    private String clientLabel = ConversationConstants.DEFAULT_LABEL;

    /**
     * Service's label value
     */
    private String serviceLabel = ConversationConstants.DEFAULT_LABEL;

    /**
     * The Token identifier of the token that the <code>DerivedKeyToken</code>
     * is (or to be) derived from.
     */
    private String tokenIdentifier;

    /**
     * True if the tokenIdentifier is a direct reference to a key identifier
     * instead of a URI to a key
     */
    private boolean tokenIdDirectId;

    /**
     * The wsse:SecurityTokenReference element to be used
     */
    private Element strElem;

    /**
     * wsu:Id of the wsc:DerivedKeyToken
     */
    private String dktId;

    /**
     * Raw bytes of the derived key
     */
    private byte[] derivedKeyBytes;

    private int wscVersion = ConversationConstants.DEFAULT_VERSION;

    private String customValueType;
    private X509Certificate useThisCert;
    private Crypto crypto;

    public WSSecDerivedKeyBase(WSSecHeader securityHeader) {
        super(securityHeader);
        setKeyIdentifierType(0);
    }

    public WSSecDerivedKeyBase(Document doc) {
        super(doc);
        setKeyIdentifierType(0);
    }

    /**
     * The derived key will change depending on the sig/encr algorithm.
     * Therefore the child classes are expected to provide this value.
     * @return the derived key length
     * @throws WSSecurityException
     */
    protected abstract int getDerivedKeyLength() throws WSSecurityException;

    public Element getStrElem() {
        return strElem;
    }

    public void setStrElem(Element strElem) {
        this.strElem = strElem;
    }

    public void setTokenIdentifier(String tokenIdentifier) {
        this.tokenIdentifier = tokenIdentifier;
    }

    /**
     * @return Returns the tokenIdentifier.
     */
    public String getTokenIdentifier() {
        return tokenIdentifier;
    }

    /**
     * Set the X509 Certificate to use
     * @param cer the X509 Certificate to use
     */
    public void setX509Certificate(X509Certificate cer) {
        this.useThisCert = cer;
    }

    /**
     * Get the id generated during <code>prepare()</code>.
     *
     * Returns the the value of wsu:Id attribute of the DerivedKeyToken element.
     *
     * @return Return the wsu:Id of this token or null if <code>prepare()</code>
     *         was not called before.
     */
    public String getId() {
        return dktId;
    }

    /**
     * Set the label value of the client.
     * @param clientLabel
     */
    public void setClientLabel(String clientLabel) {
        this.clientLabel = clientLabel;
    }

    /**
     * Set the label value of the service.
     * @param serviceLabel
     */
    public void setServiceLabel(String serviceLabel) {
        this.serviceLabel = serviceLabel;
    }

    /**
     * Initialize a WSSec Derived key.
     *
     * The method prepares and initializes a WSSec derived key structure after the
     * relevant information was set. This method also creates and initializes the
     * derived token using the ephemeral key. After preparation references
     * can be added, encrypted and signed as required.
     *
     * This method does not add any element to the security header. This must be
     * done explicitly.
     *
     * @param ephemeralKey The ephemeral key to use for derivation
     * @throws WSSecurityException
     */
    public void prepare(byte[] ephemeralKey) throws WSSecurityException {

        if (ephemeralKey == null || ephemeralKey.length == 0) {
            LOG.debug("No ephemeral key is supplied for id: " + tokenIdentifier);
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE);
        }

        // Create the derived keys
        // At this point figure out the key length according to the symencAlgo
        int offset = 0;
        int length = getDerivedKeyLength();
        byte[] label;
        String labelText = clientLabel + serviceLabel;
        label = labelText.getBytes(StandardCharsets.UTF_8);
        byte[] nonce = WSSecurityUtil.generateNonce(16);

        byte[] seed = new byte[label.length + nonce.length];
        System.arraycopy(label, 0, seed, 0, label.length);
        System.arraycopy(nonce, 0, seed, label.length, nonce.length);

        DerivationAlgorithm algo =
            AlgoFactory.getInstance(ConversationConstants.DerivationAlgorithm.P_SHA_1);
        derivedKeyBytes = algo.createKey(ephemeralKey, seed, offset, length);

        // Add the DKTs
        dkt = new DerivedKeyToken(wscVersion, getDocument());
        dktId = getIdAllocator().createId("DK-", dkt);

        dkt.setOffset(offset);
        dkt.setLength(length);
        dkt.setNonce(XMLUtils.encodeToString(nonce));
        dkt.setID(dktId);
        if (addWSUNamespace) {
            dkt.addWSUNamespace();
        }

        if (strElem == null) {
            SecurityTokenReference secRef = new SecurityTokenReference(getDocument());
            String strUri = getIdAllocator().createSecureId("STR-", secRef);
            secRef.setID(strUri);
            if (addWSUNamespace) {
                secRef.addWSUNamespace();
            }

            X509Certificate[] certs = getSigningCerts();

            switch (keyIdentifierType) {

            case WSConstants.X509_KEY_IDENTIFIER:
                secRef.setKeyIdentifier(certs[0]);
                break;

            case WSConstants.SKI_KEY_IDENTIFIER:
                secRef.setKeyIdentifierSKI(certs[0], crypto);
                break;

            case WSConstants.THUMBPRINT_IDENTIFIER:
                secRef.setKeyIdentifierThumb(certs[0]);
                break;

            case WSConstants.CUSTOM_KEY_IDENTIFIER:
                secRef.setKeyIdentifier(customValueType, tokenIdentifier);
                if (WSConstants.WSS_SAML_KI_VALUE_TYPE.equals(customValueType)) {
                    secRef.addTokenType(WSConstants.WSS_SAML_TOKEN_TYPE);
                } else if (WSConstants.WSS_SAML2_KI_VALUE_TYPE.equals(customValueType)) {
                    secRef.addTokenType(WSConstants.WSS_SAML2_TOKEN_TYPE);
                } else if (WSConstants.WSS_ENC_KEY_VALUE_TYPE.equals(customValueType)) {
                    secRef.addTokenType(WSConstants.WSS_ENC_KEY_VALUE_TYPE);
                }
                break;
            default:
                Reference ref = new Reference(getDocument());

                if (tokenIdDirectId) {
                    ref.setURI(tokenIdentifier);
                } else {
                    ref.setURI("#" + tokenIdentifier);
                }
                if (customValueType != null && customValueType.length() != 0) {
                    ref.setValueType(customValueType);
                }
                if (WSConstants.WSS_SAML_KI_VALUE_TYPE.equals(customValueType)) {
                    secRef.addTokenType(WSConstants.WSS_SAML_TOKEN_TYPE);
                    ref.setValueType(customValueType);
                } else if (WSConstants.WSS_SAML2_KI_VALUE_TYPE.equals(customValueType)) {
                    secRef.addTokenType(WSConstants.WSS_SAML2_TOKEN_TYPE);
                } else if (WSConstants.WSS_ENC_KEY_VALUE_TYPE.equals(customValueType)) {
                    secRef.addTokenType(WSConstants.WSS_ENC_KEY_VALUE_TYPE);
                    ref.setValueType(customValueType);
                } else if (KerberosSecurity.isKerberosToken(customValueType)) {
                    secRef.addTokenType(customValueType);
                    ref.setValueType(customValueType);
                } else if (WSConstants.WSC_SCT.equals(customValueType)
                    || WSConstants.WSC_SCT_05_12.equals(customValueType)) {
                    ref.setValueType(customValueType);
                } else if (!WSConstants.WSS_USERNAME_TOKEN_VALUE_TYPE.equals(customValueType)) {
                    secRef.addTokenType(WSConstants.WSS_ENC_KEY_VALUE_TYPE);
                }

                secRef.setReference(ref);
            }

            dkt.setSecurityTokenReference(secRef);
        } else {
            dkt.setSecurityTokenReference(strElem);
        }
    }


    /**
     * Prepend the DerivedKey element to the elements already in the Security
     * header.
     *
     * The method can be called any time after <code>prepare()</code>. This
     * allows to insert the DerivedKey element at any position in the Security
     * header.
     */
    public void prependDKElementToHeader() {
        Element securityHeaderElement = getSecurityHeader().getSecurityHeaderElement();
        WSSecurityUtil.prependChildElement(securityHeaderElement, dkt.getElement());
    }

    public void appendDKElementToHeader() {
        Element securityHeaderElement = getSecurityHeader().getSecurityHeaderElement();
        securityHeaderElement.appendChild(dkt.getElement());
    }

    /**
     * @param wscVersion The wscVersion to set.
     */
    public void setWscVersion(int wscVersion) {
        this.wscVersion = wscVersion;
    }

    public int getWscVersion() {
        return wscVersion;
    }

    public Element getdktElement() {
        return dkt.getElement();
    }

    public void setCustomValueType(String customValueType) {
        this.customValueType = customValueType;
    }

    public void setTokenIdDirectId(boolean b) {
        tokenIdDirectId = b;
    }

    /**
     * Set up the X509 Certificate(s) for signing.
     */
    private X509Certificate[] getSigningCerts() throws WSSecurityException {
        X509Certificate[] certs = null;
        if (keyIdentifierType == WSConstants.ISSUER_SERIAL
            || keyIdentifierType == WSConstants.ISSUER_SERIAL_QUOTE_FORMAT
            || keyIdentifierType == WSConstants.X509_KEY_IDENTIFIER
            || keyIdentifierType == WSConstants.SKI_KEY_IDENTIFIER
            || keyIdentifierType == WSConstants.THUMBPRINT_IDENTIFIER) {
            if (useThisCert == null) {
                CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
                cryptoType.setAlias(user);
                if (crypto == null) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noSigCryptoFile");
                }
                certs = crypto.getX509Certificates(cryptoType);
            } else {
                certs = new X509Certificate[] {useThisCert};
            }
            if (certs == null || certs.length <= 0) {
                throw new WSSecurityException(
                        WSSecurityException.ErrorCode.FAILURE,
                        "noUserCertsFound",
                        new Object[] {user, "signature"});
            }
        }
        return certs;
    }

    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }

    protected SecretKey getDerivedKey(String algorithm) {
        return KeyUtils.prepareSecretKey(algorithm, derivedKeyBytes);
    }

    @Override
    public void clean() {
        super.clean();
        if (derivedKeyBytes != null) {
            Arrays.fill(derivedKeyBytes, (byte)0);
        }
    }
}

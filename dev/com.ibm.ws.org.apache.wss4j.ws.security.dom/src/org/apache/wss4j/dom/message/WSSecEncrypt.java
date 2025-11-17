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

import java.security.Provider;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.Reference;
import org.apache.wss4j.common.token.SecurityTokenReference;
import org.apache.wss4j.common.util.FIPSUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.token.KerberosSecurity;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.encryption.Serializer;
import org.apache.xml.security.keys.KeyInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Encrypts a parts of a message according to WS Specification, X509 profile,
 * and adds the encryption data.
 */
public class WSSecEncrypt extends WSSecEncryptedKey {
    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(WSSecEncrypt.class);

    /**
     * SecurityTokenReference to be inserted into EncryptedData/keyInfo element.
     */
    private SecurityTokenReference securityTokenReference;

    /**
     * Indicates whether to encrypt the symmetric key into an EncryptedKey
     * or not.
     */
    private boolean encryptSymmKey = true;

    /**
     * Custom reference value
     */
    private String customReferenceValue;

    /**
     * True if the encKeyId is a direct reference to a key identifier instead of a URI to a key
     */
    private boolean encKeyIdDirectId;

    private boolean embedEncryptedKey;

    private List<Element> attachmentEncryptedDataElements;

    private Serializer encryptionSerializer;

    // Liberty Change Start
    /**
     * Algorithm to be used with the ephemeral key
     */
    private String symEncAlgo = FIPSUtils.isFIPSEnabled()
        ? WSConstants.AES_128_GCM : WSConstants.AES_128; 
    // Liberty Change End

    public WSSecEncrypt(WSSecHeader securityHeader) {
        super(securityHeader);
    }

    public WSSecEncrypt(Document doc) {
        super(doc);
    }

    // Liberty Change Start
    public WSSecEncrypt(Document doc, Provider provider) {
        super(doc, provider);
    }
    // Liberty Change End
    /**
     * Initialize a WSSec Encrypt.
     *
     * The method prepares and initializes a WSSec Encrypt structure after the
     * relevant information was set. After preparation of the token references
     * can be added and encrypted.
     *
     * This method does not add any element to the security header. This must be
     * done explicitly.
     *
     * @param crypto An instance of the Crypto API to handle keystore and certificates
     * @param symmetricKey The symmetric key to use for encryption
     * @throws WSSecurityException
     */
    public void prepare(Crypto crypto, SecretKey symmetricKey) throws WSSecurityException {
        attachmentEncryptedDataElements = new ArrayList<>();

        if (encryptSymmKey) {
            super.prepare(crypto, symmetricKey);
        } else {
            setEncryptedKeySHA1(symmetricKey.getEncoded());
        }
    }


    /**
     * Builds the SOAP envelope with encrypted Body and adds encrypted key.
     *
     * This is a convenience method and for backward compatibility. The method
     * calls the single function methods in order to perform a <i>one shot
     * encryption</i>.
     *
     * @param crypto an instance of the Crypto API to handle keystore and Certificates
     * @param symmetricKey The symmetric key to use for encryption
     * @return the SOAP envelope with encrypted Body as <code>Document</code>
     * @throws WSSecurityException
     */
    public Document build(Crypto crypto, SecretKey symmetricKey)
        throws WSSecurityException {

        prepare(crypto, symmetricKey);

        LOG.debug("Beginning Encryption...");

        Element refs = encrypt(symmetricKey);

        addAttachmentEncryptedDataElements();
        if (getEncryptedKeyElement() != null) {
            addInternalRefElement(refs);
            prependToHeader();
        } else {
            addExternalRefElement(refs);
        }

        prependBSTElementToHeader();

        LOG.debug("Encryption complete.");
        return getDocument();
    }

    /**
     * Perform encryption using the given symmetric key
     * @param symmetricKey The symmetric key to use for encryption
     * @return the EncryptedData element
     * @throws WSSecurityException
     */
    public Element encrypt(SecretKey symmetricKey) throws WSSecurityException {
        if (getParts().isEmpty()) {
            getParts().add(WSSecurityUtil.getDefaultEncryptionPart(getDocument()));
        }

        return encryptForRef(null, getParts(), symmetricKey);
    }

    /**
     * Encrypt one or more parts or elements of the message.
     *
     * This method takes a list of <code>WSEncryptionPart</code> object that
     * contain information about the elements to encrypt. The method call the
     * encryption method, takes the reference information generated during
     * encryption and add this to the <code>xenc:Reference</code> element.
     * This method can be called after <code>prepare()</code> and can be
     * called multiple times to encrypt a number of parts or elements.
     *
     * The method generates a <code>xenc:Reference</code> element that <i>must</i>
     * be added to this token. See <code>addInternalRefElement()</code>.
     *
     * If the <code>dataRef</code> parameter is <code>null</code> the method
     * creates and initializes a new Reference element.
     *
     * @param dataRef A <code>xenc:Reference</code> element or <code>null</code>
     * @param references A list containing WSEncryptionPart objects
     * @param symmetricKey The symmetric key to use for encryption
     * @return Returns the updated <code>xenc:Reference</code> element
     * @throws WSSecurityException
     */
    @FFDCIgnore(DestroyFailedException.class) // Liberty Change
    public Element encryptForRef(
        Element dataRef,
        List<WSEncryptionPart> references,
        SecretKey symmetricKey
    ) throws WSSecurityException {
        KeyInfo keyInfo = createKeyInfo();
        //the sun/oracle jce provider doesn't like a foreign SecretKey impl.
        //this occurs e.g. with a kerberos session-key. It doesn't matter for the bouncy-castle provider
        //so create a new secretKeySpec to make everybody happy.
        String keyAlgorithm = JCEMapper.getJCEKeyAlgorithmFromURI(getSymmetricEncAlgorithm());
        SecretKeySpec secretKeySpec = new SecretKeySpec(symmetricKey.getEncoded(), keyAlgorithm);

        Encryptor encryptor = new Encryptor();
        encryptor.setDoc(getDocument());
        encryptor.setSecurityHeader(getSecurityHeader());
        encryptor.setIdAllocator(getIdAllocator());
        encryptor.setCallbackLookup(callbackLookup);
        encryptor.setAttachmentCallbackHandler(attachmentCallbackHandler);
        encryptor.setStoreBytesInAttachment(storeBytesInAttachment);
        encryptor.setEncryptionSerializer(getEncryptionSerializer());
        encryptor.setExpandXopInclude(isExpandXopInclude());
        encryptor.setWsDocInfo(getWsDocInfo());
        List<String> encDataRefs =
            encryptor.doEncryption(keyInfo, secretKeySpec, getSymmetricEncAlgorithm(), references, attachmentEncryptedDataElements);

        // Clean the secret key from memory now that we're done with it
        try {
            secretKeySpec.destroy();
        } catch (DestroyFailedException e) {
            LOG.debug("Error destroying key: {}", e.getMessage());
        }

        if (encDataRefs.isEmpty()) {
            return null;
        }

        if (dataRef == null) {
            dataRef =
                getDocument().createElementNS(
                    WSConstants.ENC_NS,
                    WSConstants.ENC_PREFIX + ":ReferenceList"
                );
            //
            // If we're not placing the ReferenceList in an EncryptedKey structure,
            // then add the ENC namespace
            //
            if (!encryptSymmKey) {
                XMLUtils.setNamespace(
                    dataRef, WSConstants.ENC_NS, WSConstants.ENC_PREFIX
                );
            }
        }
        return createDataRefList(getDocument(), dataRef, encDataRefs);
    }

    /**
     * Adds the internal Reference element to this Encrypt data.
     *
     * The reference element <i>must</i> be created by the
     * <code>encryptForInternalRef()</code> method. The reference element is
     * added to the <code>EncryptedKey</code> element of this encrypt block.
     *
     * @param dataRef The internal <code>enc:Reference</code> element
     */
    public void addInternalRefElement(Element dataRef) {
        if (dataRef != null) {
            getEncryptedKeyElement().appendChild(dataRef);
        }
    }

    /**
     * Adds (prepends) the external Reference element to the Security header.
     *
     * The reference element <i>must</i> be created by the
     * <code>encryptForExternalRef() </code> method. The method prepends the
     * reference element in the SecurityHeader.
     *
     * @param dataRef The external <code>enc:Reference</code> element
     */
    public void addExternalRefElement(Element dataRef) {
        if (dataRef != null) {
            Element secHeaderElement = getSecurityHeader().getSecurityHeaderElement();
            WSSecurityUtil.prependChildElement(secHeaderElement, dataRef);
        }
    }

    public void addAttachmentEncryptedDataElements() {
        if (attachmentEncryptedDataElements != null) {
            for (Element encryptedData : attachmentEncryptedDataElements) {
                Element secHeaderElement = getSecurityHeader().getSecurityHeaderElement();
                WSSecurityUtil.prependChildElement(secHeaderElement, encryptedData);
            }
        }
    }

    /**
     * Create a KeyInfo object
     */
    private KeyInfo createKeyInfo() throws WSSecurityException {

        KeyInfo keyInfo = new KeyInfo(getDocument());
        if (embedEncryptedKey) {
            keyInfo.addUnknownElement(getEncryptedKeyElement());
        } else if (keyIdentifierType == WSConstants.ENCRYPTED_KEY_SHA1_IDENTIFIER) {
            SecurityTokenReference secToken = new SecurityTokenReference(getDocument());
            if (addWSUNamespace) {
                secToken.addWSUNamespace();
            }
            secToken.addWSSENamespace();
            if (customReferenceValue != null) {
                secToken.setKeyIdentifierEncKeySHA1(customReferenceValue);
            } else {
                secToken.setKeyIdentifierEncKeySHA1(getEncryptedKeySHA1());
            }
            secToken.addTokenType(WSConstants.WSS_ENC_KEY_VALUE_TYPE);
            keyInfo.addUnknownElement(secToken.getElement());
        } else if (WSConstants.WSS_SAML_KI_VALUE_TYPE.equals(customReferenceValue)) {
            SecurityTokenReference secToken = new SecurityTokenReference(getDocument());
            if (addWSUNamespace) {
                secToken.addWSUNamespace();
            }
            secToken.addWSSENamespace();
            secToken.addTokenType(WSConstants.WSS_SAML_TOKEN_TYPE);
            secToken.setKeyIdentifier(WSConstants.WSS_SAML_KI_VALUE_TYPE, getId());
            keyInfo.addUnknownElement(secToken.getElement());
        } else if (WSConstants.WSS_SAML2_KI_VALUE_TYPE.equals(customReferenceValue)) {
            SecurityTokenReference secToken = new SecurityTokenReference(getDocument());
            if (addWSUNamespace) {
                secToken.addWSUNamespace();
            }
            secToken.addWSSENamespace();
            secToken.addTokenType(WSConstants.WSS_SAML2_TOKEN_TYPE);
            secToken.setKeyIdentifier(WSConstants.WSS_SAML2_KI_VALUE_TYPE, getId());
            keyInfo.addUnknownElement(secToken.getElement());
        } else if (WSConstants.WSS_KRB_KI_VALUE_TYPE.equals(customReferenceValue)) {
            SecurityTokenReference secToken = new SecurityTokenReference(getDocument());
            if (addWSUNamespace) {
                secToken.addWSUNamespace();
            }
            secToken.addWSSENamespace();
            secToken.addTokenType(WSConstants.WSS_GSS_KRB_V5_AP_REQ);
            secToken.setKeyIdentifier(customReferenceValue, getId(), true);
            keyInfo.addUnknownElement(secToken.getElement());
        } else if (securityTokenReference != null) {
            Element tmpE = securityTokenReference.getElement();
            tmpE.setAttributeNS(
                WSConstants.XMLNS_NS, "xmlns:" + tmpE.getPrefix(), tmpE.getNamespaceURI()
            );
            keyInfo.addUnknownElement(securityTokenReference.getElement());
        } else if (getId() != null) {
            SecurityTokenReference secToken = new SecurityTokenReference(getDocument());
            if (addWSUNamespace) {
                secToken.addWSUNamespace();
            }
            secToken.addWSSENamespace();
            Reference ref = new Reference(getDocument());
            if (encKeyIdDirectId) {
                ref.setURI(getId());
            } else {
                ref.setURI("#" + getId());
            }
            if (customReferenceValue != null) {
                ref.setValueType(customReferenceValue);
            }
            secToken.setReference(ref);
            if (KerberosSecurity.isKerberosToken(customReferenceValue)) {
                secToken.addTokenType(customReferenceValue);
            } else if (!WSConstants.WSS_USERNAME_TOKEN_VALUE_TYPE.equals(customReferenceValue)) {
                secToken.addTokenType(WSConstants.WSS_ENC_KEY_VALUE_TYPE);
            }
            keyInfo.addUnknownElement(secToken.getElement());
        } else if (!encryptSymmKey && (keyIdentifierType == WSConstants.ISSUER_SERIAL
                || keyIdentifierType == WSConstants.ISSUER_SERIAL_QUOTE_FORMAT)) {
            SecurityTokenReference secToken = new SecurityTokenReference(getDocument());
            if (addWSUNamespace) {
                secToken.addWSUNamespace();
            }
            secToken.addWSSENamespace();
            if (customReferenceValue != null) {
                secToken.setKeyIdentifierEncKeySHA1(customReferenceValue);
            } else {
                secToken.setKeyIdentifierEncKeySHA1(getEncryptedKeySHA1());
            }
            secToken.addTokenType(WSConstants.WSS_ENC_KEY_VALUE_TYPE);
            keyInfo.addUnknownElement(secToken.getElement());
        }

        Element keyInfoElement = keyInfo.getElement();
        keyInfoElement.setAttributeNS(
            WSConstants.XMLNS_NS, "xmlns:" + WSConstants.SIG_PREFIX, WSConstants.SIG_NS
        );

        return keyInfo;
    }

    /**
     * Create DOM subtree for <code>xenc:EncryptedKey</code>
     *
     * @param doc the SOAP envelope parent document
     * @param referenceList
     * @param encDataRefs
     * @return an <code>xenc:EncryptedKey</code> element
     */
    public static Element createDataRefList(
        Document doc,
        Element referenceList,
        List<String> encDataRefs
    ) {
        for (String dataReferenceUri : encDataRefs) {
            Element dataReference =
                doc.createElementNS(
                    WSConstants.ENC_NS, WSConstants.ENC_PREFIX + ":DataReference"
                );
            dataReference.setAttributeNS(null, "URI", dataReferenceUri);
            referenceList.appendChild(dataReference);
        }
        return referenceList;
    }

    /**
     * @return Return the SecurityTokenRefernce
     */
    public SecurityTokenReference getSecurityTokenReference() {
        return securityTokenReference;
    }

    /**
     * @param reference
     */
    public void setSecurityTokenReference(SecurityTokenReference reference) {
        securityTokenReference = reference;
    }

    public boolean isEncryptSymmKey() {
        return encryptSymmKey;
    }

    public void setEncryptSymmKey(boolean encryptSymmKey) {
        this.encryptSymmKey = encryptSymmKey;
    }

    public void setCustomReferenceValue(String customReferenceValue) {
        this.customReferenceValue = customReferenceValue;
    }

    public void setEncKeyIdDirectId(boolean b) {
        encKeyIdDirectId = b;
    }

    public void setEmbedEncryptedKey(boolean embedEncryptedKey) {
        this.embedEncryptedKey = embedEncryptedKey;
    }

    public boolean isEmbedEncryptedKey() {
        return embedEncryptedKey;
    }

    public List<Element> getAttachmentEncryptedDataElements() {
        return attachmentEncryptedDataElements;
    }

    public Serializer getEncryptionSerializer() {
        return encryptionSerializer;
    }

    public void setEncryptionSerializer(Serializer encryptionSerializer) {
        this.encryptionSerializer = encryptionSerializer;
    }

    /**
     * Set the name of the symmetric encryption algorithm to use.
     *
     * This encryption algorithm is used to encrypt the data. If the algorithm
     * is not set then AES128 is used. Refer to WSConstants which algorithms are
     * supported.
     *
     * @param algo Is the name of the encryption algorithm
     * @see WSConstants#TRIPLE_DES
     * @see WSConstants#AES_128
     * @see WSConstants#AES_192
     * @see WSConstants#AES_256
     */
    public void setSymmetricEncAlgorithm(String algo) {
        symEncAlgo = algo;
    }


    /**
     * Get the name of symmetric encryption algorithm to use.
     *
     * The name of the encryption algorithm to encrypt the data, i.e. the SOAP
     * Body. Refer to WSConstants which algorithms are supported.
     *
     * @return the name of the currently selected symmetric encryption algorithm
     * @see WSConstants#TRIPLE_DES
     * @see WSConstants#AES_128
     * @see WSConstants#AES_192
     * @see WSConstants#AES_256
     */
    public String getSymmetricEncAlgorithm() {
        return symEncAlgo;
    }

}

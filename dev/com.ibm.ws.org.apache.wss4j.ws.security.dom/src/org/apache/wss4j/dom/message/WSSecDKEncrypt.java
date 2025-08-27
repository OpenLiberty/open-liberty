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

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.derivedKey.ConversationConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.Reference;
import org.apache.wss4j.common.token.SecurityTokenReference;
import org.apache.wss4j.common.util.FIPSUtils;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.xml.security.encryption.Serializer;
import org.apache.xml.security.keys.KeyInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Encrypts and signs parts of a message with derived keys derived from a
 * symmetric key. This symmetric key will be included as an EncryptedKey
 */
public class WSSecDKEncrypt extends WSSecDerivedKeyBase {

    private String symEncAlgo = FIPSUtils.isFIPSEnabled()
        ? WSConstants.AES_128_GCM : WSConstants.AES_128;
    private int derivedKeyLength = -1;

    private List<Element> attachmentEncryptedDataElements;

    private Serializer encryptionSerializer;

    public WSSecDKEncrypt(WSSecHeader securityHeader) {
        super(securityHeader);
    }

    public WSSecDKEncrypt(Document doc) {
        super(doc);
    }

    @Override
    public void prepare(byte[] ephemeralKey) throws WSSecurityException {
        super.prepare(ephemeralKey);

        attachmentEncryptedDataElements = new ArrayList<>();
    }

    public Document build(byte[] ephemeralKey) throws WSSecurityException {

        //
        // Setup the encrypted key
        //
        prepare(ephemeralKey);
        //
        // prepend elements in the right order to the security header
        //
        prependDKElementToHeader();

        Element externRefList = encrypt();

        addAttachmentEncryptedDataElements();

        addExternalRefElement(externRefList);

        return getDocument();
    }

    public void addAttachmentEncryptedDataElements() {
        if (attachmentEncryptedDataElements != null) {
            for (Element encryptedData : attachmentEncryptedDataElements) {
                Element securityHeaderElement = getSecurityHeader().getSecurityHeaderElement();
                WSSecurityUtil.prependChildElement(securityHeaderElement, encryptedData);
            }
        }
    }

    public Element encrypt() throws WSSecurityException {
        if (getParts().isEmpty()) {
            getParts().add(WSSecurityUtil.getDefaultEncryptionPart(getDocument()));
        }

        return encryptForExternalRef(null, getParts());
    }

    /**
     * Encrypt one or more parts or elements of the message (external).
     *
     * This method takes a vector of <code>WSEncryptionPart</code> object that
     * contain information about the elements to encrypt. The method call the
     * encryption method, takes the reference information generated during
     * encryption and add this to the <code>xenc:Reference</code> element.
     * This method can be called after <code>prepare()</code> and can be
     * called multiple times to encrypt a number of parts or elements.
     *
     * The method generates a <code>xenc:Reference</code> element that <i>must</i>
     * be added to the SecurityHeader. See <code>addExternalRefElement()</code>.
     *
     * If the <code>dataRef</code> parameter is <code>null</code> the method
     * creates and initializes a new Reference element.
     *
     * @param dataRef A <code>xenc:Reference</code> element or <code>null</code>
     * @param references A list containing WSEncryptionPart objects
     * @return Returns the updated <code>xenc:Reference</code> element
     * @throws WSSecurityException
     */
    public Element encryptForExternalRef(Element dataRef, List<WSEncryptionPart> references)
        throws WSSecurityException {

        KeyInfo keyInfo = createKeyInfo();

        SecretKey key = getDerivedKey(symEncAlgo);

        Encryptor encryptor = new Encryptor();
        encryptor.setDoc(getDocument());
        encryptor.setSecurityHeader(getSecurityHeader());
        encryptor.setIdAllocator(getIdAllocator());
        encryptor.setCallbackLookup(callbackLookup);
        encryptor.setAttachmentCallbackHandler(attachmentCallbackHandler);
        encryptor.setStoreBytesInAttachment(storeBytesInAttachment);
        encryptor.setEncryptionSerializer(encryptionSerializer);
        encryptor.setWsDocInfo(getWsDocInfo());
        List<String> encDataRefs =
            encryptor.doEncryption(keyInfo, key, symEncAlgo, references, attachmentEncryptedDataElements);

        if (dataRef == null) {
            dataRef =
                getDocument().createElementNS(
                    WSConstants.ENC_NS, WSConstants.ENC_PREFIX + ":ReferenceList"
                );
        }
        return WSSecEncrypt.createDataRefList(getDocument(), dataRef, encDataRefs);
    }

    /**
     * Create a KeyInfo object.
     *
     * @return Returns the created KeyInfo object.
     */
    private KeyInfo createKeyInfo() {
        KeyInfo keyInfo = new KeyInfo(getDocument());
        SecurityTokenReference secToken = new SecurityTokenReference(getDocument());
        secToken.addWSSENamespace();
        if (addWSUNamespace) {
            secToken.addWSUNamespace();
        }
        Reference ref = new Reference(getDocument());
        ref.setURI("#" + getId());
        String ns =
            ConversationConstants.getWSCNs(getWscVersion())
                + ConversationConstants.TOKEN_TYPE_DERIVED_KEY_TOKEN;
        ref.setValueType(ns);
        secToken.setReference(ref);

        keyInfo.addUnknownElement(secToken.getElement());
        Element keyInfoElement = keyInfo.getElement();
        keyInfoElement.setAttributeNS(
            WSConstants.XMLNS_NS, "xmlns:" + WSConstants.SIG_PREFIX, WSConstants.SIG_NS
        );

        return keyInfo;
    }

    /**
     * Adds (prepends) the external Reference element to the Security header.
     *
     * The reference element <i>must</i> be created by the
     * <code>encryptForExternalRef() </code> method. The method adds the
     * reference element in the SecurityHeader.
     *
     * @param referenceList The external <code>enc:Reference</code> element
     */
    public void addExternalRefElement(Element referenceList) {
        if (referenceList != null) {
            Node node = getdktElement().getNextSibling();
            Element securityHeaderElement = getSecurityHeader().getSecurityHeaderElement();
            if (node != null && Node.ELEMENT_NODE == node.getNodeType()) {
                securityHeaderElement.insertBefore(referenceList, node);
            } else {
                // If (at this moment) DerivedKeyToken is the LAST element of
                // the security header
                securityHeaderElement.appendChild(referenceList);
            }
        }
    }


    /**
     * Set the symmetric encryption algorithm URI to use
     * @param algo the symmetric encryption algorithm URI to use
     */
    public void setSymmetricEncAlgorithm(String algo) {
        symEncAlgo = algo;
    }

    protected int getDerivedKeyLength() throws WSSecurityException {
        return derivedKeyLength > 0 ? derivedKeyLength : KeyUtils.getKeyLength(symEncAlgo);
    }

    public void setDerivedKeyLength(int keyLength) {
        derivedKeyLength = keyLength;
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
}

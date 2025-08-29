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

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.WsuIdAllocator;
import org.apache.wss4j.dom.callback.CallbackLookup;
import org.apache.wss4j.dom.callback.DOMCallbackLookup;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.security.auth.callback.CallbackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the base class for WS Security messages. It provides common functions
 * and fields used by the specific message classes such as sign, encrypt, and
 * username token.
 */
public class WSSecBase {
    protected String user;
    protected String password;
    protected int keyIdentifierType = WSConstants.ISSUER_SERIAL;
    protected CallbackLookup callbackLookup;
    protected CallbackHandler attachmentCallbackHandler;
    protected boolean storeBytesInAttachment;
    protected boolean expandXopInclude;
    protected boolean addWSUNamespace;

    private WsuIdAllocator idAllocator;
    private final List<WSEncryptionPart> parts = new ArrayList<>();
    private final WSSecHeader securityHeader;
    private final Document doc;
    private WSDocInfo wsDocInfo;

    public WSSecBase(WSSecHeader securityHeader) {
        this.securityHeader = securityHeader;
        if (securityHeader != null && securityHeader.getSecurityHeaderElement() != null) {
            doc = securityHeader.getSecurityHeaderElement().getOwnerDocument();
        } else {
            doc = null;
        }

        // Explicitly add the WSU Namespace if we already have a different prefix
        addWSUNamespace = securityHeader != null && securityHeader.getWsuPrefix() != null
            && !WSConstants.WSU_PREFIX.equals(securityHeader.getWsuPrefix());
    }

    public WSSecBase(Document doc) {
        this.doc = doc;
        securityHeader = new WSSecHeader(doc);
    }

    protected Document getDocument() {
        return doc;
    }

    public WSSecHeader getSecurityHeader() {
        return securityHeader;
    }

    /**
     * @param callbackLookup The CallbackLookup object to retrieve elements
     */
    public void setCallbackLookup(CallbackLookup callbackLookup) {
        this.callbackLookup = callbackLookup;
    }

    /**
     * Get which parts of the message to encrypt/sign.
     */
    public List<WSEncryptionPart> getParts() {
        return parts;
    }

    /**
     * Sets which key identifier to use.
     *
     * <p/>
     *
     * Defines the key identifier type to
     * use in the {@link WSSecSignature#prepare(Document, Crypto, WSSecHeader) method} or
     * the {@link WSSecEncrypt#prepare(Document, Crypto) method} function to
     * set up the key identification elements.
     *
     * @param keyIdType
     * @see WSConstants#ISSUER_SERIAL
     * @see WSConstants#ISSUER_SERIAL_QUOTE_FORMAT
     * @see WSConstants#BST_DIRECT_REFERENCE
     * @see WSConstants#X509_KEY_IDENTIFIER
     * @see WSConstants#THUMBPRINT_IDENTIFIER
     * @see WSConstants#SKI_KEY_IDENTIFIER
     * @see WSConstants#KEY_VALUE
     */
    public void setKeyIdentifierType(int keyIdType) {
        keyIdentifierType = keyIdType;
    }

    /**
     * Gets the value of the <code>keyIdentifierType</code>.
     *
     * @return The <code>keyIdentifyerType</code>.
     * @see WSConstants#ISSUER_SERIAL
     * @see WSConstants#ISSUER_SERIAL_QUOTE_FORMAT
     * @see WSConstants#BST_DIRECT_REFERENCE
     * @see WSConstants#X509_KEY_IDENTIFIER
     * @see WSConstants#SKI_KEY_IDENTIFIER
     */
    public int getKeyIdentifierType() {
        return keyIdentifierType;
    }

    public void setAttachmentCallbackHandler(CallbackHandler attachmentCallbackHandler) {
        this.attachmentCallbackHandler = attachmentCallbackHandler;
    }

    public void setStoreBytesInAttachment(boolean storeBytesInAttachment) {
        this.storeBytesInAttachment = storeBytesInAttachment;
    }

    /**
     * Looks up or adds a body id. <p/> First try to locate the
     * <code>wsu:Id</code> in the SOAP body element. If one is found, the
     * value of the <code>wsu:Id</code> attribute is returned. Otherwise the
     * method generates a new <code>wsu:Id</code> and an appropriate value.
     *
     * @param doc The SOAP envelope as <code>Document</code>
     * @return The value of the <code>wsu:Id</code> attribute of the SOAP body
     * @throws Exception
     */
    protected String setBodyID(Document doc) throws Exception {
        if (callbackLookup == null) {
            callbackLookup = new DOMCallbackLookup(doc);
        }
        Element bodyElement = callbackLookup.getSOAPBody();
        if (bodyElement == null) {
            throw new Exception("SOAP Body Element node not found");
        }
        return setWsuId(bodyElement);
    }

    protected String setWsuId(Element bodyElement) {
        String id = bodyElement.getAttributeNS(WSConstants.WSU_NS, "Id");

        String newAttrNs = WSConstants.WSU_NS;
        String newAttrPrefix = WSConstants.WSU_PREFIX;

        if (id == null || id.length() == 0) {
            if (WSConstants.ENC_NS.equals(bodyElement.getNamespaceURI())
                && (WSConstants.ENC_DATA_LN.equals(bodyElement.getLocalName())
                    || WSConstants.ENC_KEY_LN.equals(bodyElement.getLocalName()))
                ) {
                // If it is an XML-Enc derived element, it may already have an ID,
                // plus it is not schema valid to add an additional ID.
                id = bodyElement.getAttributeNS(null, "Id");
                newAttrPrefix = WSConstants.ENC_PREFIX;
                newAttrNs = WSConstants.ENC_NS;
            } else if (WSConstants.SAML_NS.equals(bodyElement.getNamespaceURI())
                && "Assertion".equals(bodyElement.getLocalName())) {
                id = bodyElement.getAttributeNS(null, "AssertionID");
            } else if (WSConstants.SAML2_NS.equals(bodyElement.getNamespaceURI())
                && "Assertion".equals(bodyElement.getLocalName())) {
                id = bodyElement.getAttributeNS(null, "ID");
            } else if (WSConstants.SIG_NS.equals(bodyElement.getNamespaceURI())
                && "KeyInfo".equals(bodyElement.getLocalName())) {
                id = bodyElement.getAttributeNS(null, "Id");
            }
        }

        if (id == null || id.length() == 0) {
            id = getIdAllocator().createId("id-", bodyElement);
            String prefix = XMLUtils.setNamespace(bodyElement, newAttrNs, newAttrPrefix);
            bodyElement.setAttributeNS(newAttrNs, prefix + ":Id", id);
        }
        return id;
    }

    /**
     * Set the user and password info.
     *
     * Both information is used to get the user's private signing key.
     *
     * @param user
     *            This is the user's alias name in the keystore that identifies
     *            the private key to sign the document
     * @param password
     *            The user's password to get the private signing key from the
     *            keystore
     */
    public void setUserInfo(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public WsuIdAllocator getIdAllocator() {
        if (idAllocator != null) {
            return idAllocator;
        }
        return WSSConfig.DEFAULT_ID_ALLOCATOR;
    }

    public void setIdAllocator(WsuIdAllocator idAllocator) {
        this.idAllocator = idAllocator;
    }

    public boolean isExpandXopInclude() {
        return expandXopInclude;
    }

    public void setExpandXopInclude(boolean expandXopInclude) {
        this.expandXopInclude = expandXopInclude;
    }

    public WSDocInfo getWsDocInfo() {
        return wsDocInfo;
    }

    public void setWsDocInfo(WSDocInfo wsDocInfo) {
        this.wsDocInfo = wsDocInfo;
    }

    public void clean() {
        user = null;
        password = null;
    }
}

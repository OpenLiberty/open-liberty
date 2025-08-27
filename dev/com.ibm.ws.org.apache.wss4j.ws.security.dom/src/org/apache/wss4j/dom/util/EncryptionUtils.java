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

package org.apache.wss4j.dom.util;

import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.ext.AttachmentResultCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.callback.CallbackLookup;
import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.encryption.Serializer;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.parser.XMLParserException;
import org.apache.xml.security.utils.JavaUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public final class EncryptionUtils {

    private EncryptionUtils() {
        // complete
    }

    /**
     * Look up the encrypted data. First try Id="someURI". If no such Id then try
     * wsu:Id="someURI".
     *
     * @param wsDocInfo The WSDocInfo object to use
     * @param dataRefURI The URI of EncryptedData
     * @return The EncryptedData element
     * @throws WSSecurityException if the EncryptedData element referenced by dataRefURI is
     * not found
     */
    public static Element
    findEncryptedDataElement(
        WSDocInfo wsDocInfo,
        String dataRefURI
    ) throws WSSecurityException {
        CallbackLookup callbackLookup = wsDocInfo.getCallbackLookup();
        Element encryptedDataElement =
            callbackLookup.getElement(dataRefURI, null, true);
        if (encryptedDataElement == null) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.INVALID_SECURITY, "dataRef",
                new Object[] {dataRefURI});
        }
        if (encryptedDataElement.getLocalName().equals(WSConstants.ENCRYPTED_HEADER)
            && encryptedDataElement.getNamespaceURI().equals(WSConstants.WSSE11_NS)) {
            Node child = encryptedDataElement.getFirstChild();
            while (child != null && child.getNodeType() != Node.ELEMENT_NODE) {
                child = child.getNextSibling();
            }
            return (Element)child;
        }
        return encryptedDataElement;
    }

    /**
     * Decrypt the EncryptedData argument using a SecretKey.
     * @param doc The (document) owner of EncryptedData
     * @param dataRefURI The URI of EncryptedData
     * @param encData The EncryptedData element
     * @param symmetricKey The SecretKey with which to decrypt EncryptedData
     * @param symEncAlgo The symmetric encryption algorithm to use
     * @param attachmentCallbackHandler The CallbackHandler from which to get attachments
     * @throws WSSecurityException
     */
    public static WSDataRef
    decryptEncryptedData(
        Document doc,
        String dataRefURI,
        Element encData,
        SecretKey symmetricKey,
        String symEncAlgo,
        CallbackHandler attachmentCallbackHandler
    ) throws WSSecurityException {
        return decryptEncryptedData(doc, dataRefURI, encData, symmetricKey,
                                    symEncAlgo, attachmentCallbackHandler, null);

    }
    /**
     * Decrypt the EncryptedData argument using a SecretKey.
     * @param doc The (document) owner of EncryptedData
     * @param dataRefURI The URI of EncryptedData
     * @param encData The EncryptedData element
     * @param symmetricKey The SecretKey with which to decrypt EncryptedData
     * @param symEncAlgo The symmetric encryption algorithm to use
     * @param attachmentCallbackHandler The CallbackHandler from which to get attachments
     * @throws WSSecurityException
     */
    public static WSDataRef
    decryptEncryptedData(
        Document doc,
        String dataRefURI,
        Element encData,
        SecretKey symmetricKey,
        String symEncAlgo,
        CallbackHandler attachmentCallbackHandler,
        Serializer encryptionSerializer
    ) throws WSSecurityException {

        // See if it is an attachment, and handle that differently
        String typeStr = encData.getAttributeNS(null, "Type");
        String xopURI = getXOPURIFromEncryptedData(encData);
        if (typeStr != null
            && (WSConstants.SWA_ATTACHMENT_ENCRYPTED_DATA_TYPE_CONTENT_ONLY.equals(typeStr)
                || WSConstants.SWA_ATTACHMENT_ENCRYPTED_DATA_TYPE_COMPLETE.equals(typeStr))) {

            Element cipherData = XMLUtils.getDirectChildElement(encData, "CipherData", WSConstants.ENC_NS);
            if (cipherData == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK);
            }
            Element cipherReference = XMLUtils.getDirectChildElement(cipherData, "CipherReference", WSConstants.ENC_NS);
            if (cipherReference == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK);
            }
            String uri = cipherReference.getAttributeNS(null, "URI");

            return decryptAttachment(dataRefURI, uri, encData, symmetricKey, symEncAlgo, attachmentCallbackHandler);
        }

        WSDataRef dataRef = new WSDataRef();
        dataRef.setEncryptedElement(encData);
        dataRef.setWsuId(dataRefURI);
        dataRef.setAlgorithm(symEncAlgo);

        boolean content = X509Util.isContent(encData);
        dataRef.setContent(content);

        Element encDataOrig = encData;
        Node parent = encData.getParentNode();
        Node previousSibling = encData.getPreviousSibling();
        if (content) {
            encData = (Element) encData.getParentNode();
            parent = encData.getParentNode();
        }

        XMLCipher xmlCipher = null;
        try {
            if (encryptionSerializer != null) {
                xmlCipher = XMLCipher.getInstance(encryptionSerializer, symEncAlgo);
            } else {
                xmlCipher = XMLCipher.getInstance(symEncAlgo);
            }
            xmlCipher.setSecureValidation(true);
            xmlCipher.init(XMLCipher.DECRYPT_MODE, symmetricKey);
        } catch (XMLEncryptionException ex) {
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.UNSUPPORTED_ALGORITHM, ex
            );
        }

        Node decryptedNode = null;
        try {
            if (xopURI != null) {
                Element tempEncData;

                //if content == true, use encDataOrig (i.e., actual EncryptedData element instead of parent)
                //We will replace the EncryptedData element itself with the decrypted data found in attachment
                if (content) {
                    tempEncData = encDataOrig;
                } else {
                    tempEncData = encData;
                }
                decryptedNode = decryptXopAttachment(symmetricKey, symEncAlgo, attachmentCallbackHandler,
                                                     xopURI, tempEncData);
            } else {
                //in this case, the XMLCipher knows how to handle encData when it's the parent node
                // (i.e., when content == true)
                xmlCipher.doFinal(doc, encData, content);
            }
        } catch (Exception ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK, ex);
        }

        if (parent.getLocalName().equals(WSConstants.ENCRYPTED_HEADER)
            && parent.getNamespaceURI().equals(WSConstants.WSSE11_NS)
            || parent.getLocalName().equals(WSConstants.ENCRYPED_ASSERTION_LN)
            && parent.getNamespaceURI().equals(WSConstants.SAML2_NS)) {

            Node decryptedHeader = parent.getFirstChild();
            Node soapHeader = parent.getParentNode();
            soapHeader.replaceChild(decryptedHeader, parent);

            dataRef.setProtectedElement((Element)decryptedHeader);
            dataRef.setXpath(getXPath(decryptedHeader));
        } else if (content) {
            dataRef.setProtectedElement(encData);
            dataRef.setXpath(getXPath(encData));
        } else {
            if (decryptedNode == null) {
                if (previousSibling == null) {
                    decryptedNode = parent.getFirstChild();
                } else {
                    decryptedNode = previousSibling.getNextSibling();
                }
            }
            if (decryptedNode != null && Node.ELEMENT_NODE == decryptedNode.getNodeType()) {
                dataRef.setProtectedElement((Element)decryptedNode);
            }
            dataRef.setXpath(getXPath(decryptedNode));
        }

        return dataRef;
    }

    private static String getXOPURIFromEncryptedData(Element encData) {
        Element cipherValue = getCipherValueFromEncryptedData(encData);
        if (cipherValue != null) {
            return getXOPURIFromCipherValue(cipherValue);
        }

        return null;
    }

    public static Element getCipherValueFromEncryptedData(Element encData) {
        Element cipherData = XMLUtils.getDirectChildElement(encData, "CipherData", WSConstants.ENC_NS);
        if (cipherData != null) {
            return XMLUtils.getDirectChildElement(cipherData, "CipherValue", WSConstants.ENC_NS);
        }

        return null;
    }

    public static String getXOPURIFromCipherValue(Element cipherValue) {
        if (cipherValue != null) {
            Element cipherValueChild =
                XMLUtils.getDirectChildElement(cipherValue, "Include", WSConstants.XOP_NS);
            if (cipherValueChild != null && cipherValueChild.hasAttributeNS(null, "href")) {
                return cipherValueChild.getAttributeNS(null, "href");
            }
        }

        return null;
    }


    private static WSDataRef
    decryptAttachment(
        String dataRefURI,
        String uri,
        Element encData,
        SecretKey symmetricKey,
        String symEncAlgo,
        CallbackHandler attachmentCallbackHandler
    ) throws WSSecurityException {
        WSDataRef dataRef = new WSDataRef();
        dataRef.setWsuId(dataRefURI);
        dataRef.setAlgorithm(symEncAlgo);

        try {
            if (uri == null || uri.length() < 5 || !uri.startsWith("cid:")) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK);
            }
            dataRef.setWsuId(uri);
            dataRef.setAttachment(true);

            if (attachmentCallbackHandler == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK);
            }

            final String attachmentId = AttachmentUtils.getAttachmentId(uri);

            AttachmentRequestCallback attachmentRequestCallback = new AttachmentRequestCallback();
            attachmentRequestCallback.setAttachmentId(attachmentId);

            attachmentCallbackHandler.handle(new Callback[]{attachmentRequestCallback});
            List<Attachment> attachments = attachmentRequestCallback.getAttachments();
            if (attachments == null || attachments.isEmpty() || !attachmentId.equals(attachments.get(0).getId())) {
                throw new WSSecurityException(
                        WSSecurityException.ErrorCode.INVALID_SECURITY,
                        "empty", new Object[] {"Attachment not found"}
                );
            }
            Attachment attachment = attachments.get(0);

            final String encAlgo = X509Util.getEncAlgo(encData);
            final String jceAlgorithm =
                    JCEMapper.translateURItoJCEID(encAlgo);
            final Cipher cipher = Cipher.getInstance(jceAlgorithm);

            InputStream attachmentInputStream = //NOPMD
                    AttachmentUtils.setupAttachmentDecryptionStream(
                            encAlgo, cipher, symmetricKey, attachment.getSourceStream());

            Attachment resultAttachment = new Attachment();
            resultAttachment.setId(attachment.getId());
            resultAttachment.setMimeType(encData.getAttributeNS(null, "MimeType"));
            resultAttachment.setSourceStream(attachmentInputStream);
            resultAttachment.addHeaders(attachment.getHeaders());

            String typeStr = encData.getAttributeNS(null, "Type");
            if (WSConstants.SWA_ATTACHMENT_ENCRYPTED_DATA_TYPE_COMPLETE.equals(typeStr)) {
                AttachmentUtils.readAndReplaceEncryptedAttachmentHeaders(
                        resultAttachment.getHeaders(), attachmentInputStream);
            }

            AttachmentResultCallback attachmentResultCallback = new AttachmentResultCallback();
            attachmentResultCallback.setAttachment(resultAttachment);
            attachmentResultCallback.setAttachmentId(resultAttachment.getId());
            attachmentCallbackHandler.handle(new Callback[]{attachmentResultCallback});

        } catch (UnsupportedCallbackException | IOException
            | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILED_CHECK, e);
        }

        dataRef.setContent(true);
        // Remove this EncryptedData from the security header to avoid processing it again
        encData.getParentNode().removeChild(encData);

        return dataRef;
    }


    private static Node decryptXopAttachment(
       SecretKey symmetricKey, String symEncAlgo, CallbackHandler attachmentCallbackHandler,
       String xopURI, Element encData
   ) throws WSSecurityException, IOException, UnsupportedCallbackException, NoSuchAlgorithmException,
        NoSuchPaddingException, XMLParserException {

        if (attachmentCallbackHandler == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK);
        }
        final String attachmentId = AttachmentUtils.getAttachmentId(xopURI);

        AttachmentRequestCallback attachmentRequestCallback = new AttachmentRequestCallback();
        attachmentRequestCallback.setAttachmentId(attachmentId);

        attachmentCallbackHandler.handle(new Callback[]{attachmentRequestCallback});
        List<Attachment> attachments = attachmentRequestCallback.getAttachments();
        if (attachments == null || attachments.isEmpty() || !attachmentId.equals(attachments.get(0).getId())) {
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.INVALID_SECURITY,
                    "empty", new Object[] {"Attachment not found"}
            );
        }
        Attachment attachment = attachments.get(0);

        final String jceAlgorithm =
                JCEMapper.translateURItoJCEID(symEncAlgo);
        final Cipher cipher = Cipher.getInstance(jceAlgorithm);

        InputStream attachmentInputStream = //NOPMD
                AttachmentUtils.setupAttachmentDecryptionStream(
                        symEncAlgo, cipher, symmetricKey, attachment.getSourceStream());

        // For the xop:Include case, we need to replace the xop:Include Element with the
        // decrypted Element
        byte[] bytes = JavaUtils.getBytesFromStream(attachmentInputStream);

        Document document = null;
        try {
            document = org.apache.xml.security.utils.XMLUtils.read(new ByteArrayInputStream(bytes), true);
        } catch (XMLParserException ex) {
            if (ex.getCause() instanceof SAXException) {
                // A prefix may not have been bound, try to fix the DOM Element in this case.
                String fixedElementStr = setParentPrefixes(encData, new String(bytes));
                document = org.apache.xml.security.utils.XMLUtils.read(
                    new ByteArrayInputStream(fixedElementStr.getBytes()), true);
            } else {
                throw ex;
            }
        }

        Node decryptedNode =
            encData.getOwnerDocument().importNode(document.getDocumentElement(), true);
        encData.getParentNode().appendChild(decryptedNode);
        encData.getParentNode().removeChild(encData);
        return decryptedNode;
    }

    /**
     * Set the parent prefix definitions on the "String" (representation of the Element to be parsed)
     */
    private static String setParentPrefixes(Element target, String str) {
        Node parent = target;

        // Get the point at where to insert new prefix definitions
        int insertionIndex = str.indexOf('>');
        StringBuilder prefix = new StringBuilder(str.substring(0, insertionIndex));
        StringBuilder suffix = new StringBuilder(str.substring(insertionIndex, str.length()));

        // Don't add more than 20 prefixes
        int prefixAddedCount = 0;
        while (parent.getParentNode() != null && prefixAddedCount < 20
            && Node.DOCUMENT_NODE != parent.getParentNode().getNodeType()) {
            parent = parent.getParentNode();
            NamedNodeMap attributes = parent.getAttributes();
            int length = attributes.getLength();
            for (int i = 0; i < length; i++) {
                Node attribute = attributes.item(i);
                String attrDef = "xmlns:" + attribute.getLocalName();
                if (WSConstants.XMLNS_NS.equals(attribute.getNamespaceURI()) && !prefix.toString().contains(attrDef)) {
                    attrDef += "=\"" + attribute.getNodeValue() + "\"";
                    prefix.append(' ').append(attrDef);
                    prefixAddedCount++;
                }
                if (prefixAddedCount >= 20) {
                    break;
                }
            }
        }

        return prefix.toString() + suffix.toString();
    }

    /**
     * @param decryptedNode the decrypted node
     * @return a fully built xpath
     *        (eg. &quot;/soapenv:Envelope/soapenv:Body/ns:decryptedElement&quot;)
     *        if the decryptedNode is an Element or an Attr node and is not detached
     *        from the document. <code>null</code> otherwise
     */
    public static String getXPath(Node decryptedNode) {
        if (decryptedNode == null) {
            return null;
        }

        String result = "";
        if (Node.ELEMENT_NODE == decryptedNode.getNodeType()) {
            result = decryptedNode.getNodeName();
            result = prependFullPath(result, decryptedNode.getParentNode());
        } else if (Node.ATTRIBUTE_NODE == decryptedNode.getNodeType()) {
            result = "@" + decryptedNode.getNodeName();
            result = prependFullPath(result, ((Attr)decryptedNode).getOwnerElement());
        } else {
            return null;
        }

        return result;
    }


    /**
     * Recursively build an absolute xpath (starting with the root &quot;/&quot;)
     *
     * @param xpath the xpath expression built so far
     * @param node the current node whose name is to be prepended
     * @return a fully built xpath
     */
    private static String prependFullPath(String xpath, Node node) {
        if (node == null) {
            // probably a detached node... not really useful
            return null;
        } else if (Node.ELEMENT_NODE == node.getNodeType()) {
            xpath = node.getNodeName() + "/" + xpath;
            return prependFullPath(xpath, node.getParentNode());
        } else if (Node.DOCUMENT_NODE == node.getNodeType()) {
            return "/" + xpath;
        } else {
            return prependFullPath(xpath, node.getParentNode());
        }
    }

    public static String getDigestAlgorithm(Node encBodyData) throws WSSecurityException {
        Element tmpE =
            XMLUtils.getDirectChildElement(
                encBodyData, "EncryptionMethod", WSConstants.ENC_NS
            );
        if (tmpE != null) {
            Element digestElement =
                XMLUtils.getDirectChildElement(tmpE, "DigestMethod", WSConstants.SIG_NS);
            if (digestElement != null) {
                return digestElement.getAttributeNS(null, "Algorithm");
            }
        }
        return null;
    }

    public static String getMGFAlgorithm(Node encBodyData) throws WSSecurityException {
        Element tmpE =
            XMLUtils.getDirectChildElement(
                        encBodyData, "EncryptionMethod", WSConstants.ENC_NS
                );
        if (tmpE != null) {
            Element mgfElement =
                XMLUtils.getDirectChildElement(tmpE, "MGF", WSConstants.ENC11_NS);
            if (mgfElement != null) {
                return mgfElement.getAttributeNS(null, "Algorithm");
            }
        }
        return null;
    }

    public static byte[] getPSource(Node encBodyData) throws WSSecurityException {
        Element tmpE =
            XMLUtils.getDirectChildElement(
                        encBodyData, "EncryptionMethod", WSConstants.ENC_NS
                );
        if (tmpE != null) {
            Element pSourceElement =
                XMLUtils.getDirectChildElement(tmpE, "OAEPparams", WSConstants.ENC_NS);
            if (pSourceElement != null) {
                return getDecodedBase64EncodedData(pSourceElement);
            }
        }
        return new byte[0];
    }

    /**
     * Method getDecodedBase64EncodedData
     *
     * @param element
     * @return a byte array containing the decoded data
     * @throws WSSecurityException
     */
    public static byte[] getDecodedBase64EncodedData(Element element) throws WSSecurityException {
        String text = XMLUtils.getElementText(element);
        if (text == null) {
            return new byte[0];
        }
        return org.apache.xml.security.utils.XMLUtils.decode(text);
    }
}

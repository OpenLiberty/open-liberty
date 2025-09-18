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

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.spec.ExcC14NParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;


import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.callback.DOMCallbackLookup;
import org.apache.wss4j.dom.transform.AttachmentTransformParameterSpec;
import org.apache.wss4j.dom.transform.STRTransform;
import org.apache.wss4j.dom.util.SignatureUtils;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This is the base class for WS Security messages that are used for signature generation or
 * verification.
 */
public class WSSecSignatureBase extends WSSecBase {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(WSSecSignatureBase.class);

    private List<Element> clonedElements = new ArrayList<>();

    public WSSecSignatureBase(WSSecHeader securityHeader) {
        super(securityHeader);
    }

    public WSSecSignatureBase(Document doc) {
        super(doc);
    }

    /**
     * This method adds references to the Signature.
     *
     * @param doc The parent document
     * @param references The list of references to sign
     * @param wsDocInfo The WSDocInfo object to store protection elements in
     * @param signatureFactory The XMLSignature object
     * @param addInclusivePrefixes Whether to add inclusive prefixes or not
     * @param digestAlgo The digest algorithm to use
     * @throws WSSecurityException
     */
    public List<javax.xml.crypto.dsig.Reference> addReferencesToSign(
        Document doc,
        List<WSEncryptionPart> references,
        WSDocInfo wsDocInfo,
        XMLSignatureFactory signatureFactory,
        boolean addInclusivePrefixes,
        String digestAlgo
    ) throws WSSecurityException {
        DigestMethod digestMethod;
        try {
            digestMethod = signatureFactory.newDigestMethod(digestAlgo, null);
        } catch (Exception ex) {
            LOG.error("", ex);
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILED_SIGNATURE, ex, "noXMLSig"
            );
        }

        //create separate list for attachment and append it after same document references
        //are processed.
        List<javax.xml.crypto.dsig.Reference> attachmentReferenceList = null;
        List<javax.xml.crypto.dsig.Reference> referenceList = new ArrayList<>();

        for (WSEncryptionPart encPart : references) {
            String idToSign = encPart.getId();
            String elemName = encPart.getName();
            Element element = encPart.getElement();

            //
            // Set up the elements to sign. There is one reserved element
            // names: "STRTransform": Setup the ds:Reference to use STR Transform
            //
            try {
                if ("cid:Attachments".equals(idToSign) && attachmentReferenceList == null) {
                    attachmentReferenceList =
                        addAttachmentReferences(encPart, digestMethod, signatureFactory);
                    continue;
                }
                if (idToSign != null) {
                    Transform transform = null;
                    if ("STRTransform".equals(elemName)) {
                        Element ctx = createSTRParameter(doc);

                        XMLStructure structure = new DOMStructure(ctx);
                        transform =
                            signatureFactory.newTransform(
                                STRTransform.TRANSFORM_URI,
                                structure
                            );
                    } else {
                        TransformParameterSpec transformSpec = null;
                        if (element == null) {
                            if (callbackLookup == null) {
                                callbackLookup = new DOMCallbackLookup(doc);
                            }
                            element = callbackLookup.getElement(idToSign, null, false);
                        }
                        if (addInclusivePrefixes && element != null) {
                            List<String> prefixes = getInclusivePrefixes(element);
                            if (!prefixes.isEmpty()) {
                                transformSpec = new ExcC14NParameterSpec(prefixes);
                            }
                        }
                        transform =
                            signatureFactory.newTransform(
                                WSConstants.C14N_EXCL_OMIT_COMMENTS,
                                transformSpec
                            );
                    }
                    if (element != null) {
                        cloneElement(element);

                        wsDocInfo.addTokenElement(element, false);
                    } else if (!encPart.isRequired()) {
                        continue;
                    }
                    javax.xml.crypto.dsig.Reference reference =
                        signatureFactory.newReference(
                            "#" + idToSign,
                            digestMethod,
                            Collections.singletonList(transform),
                            null,
                            null
                        );
                    referenceList.add(reference);
                } else {
                    String nmSpace = encPart.getNamespace();
                    List<Element> elementsToSign = null;
                    if (element != null) {
                        elementsToSign = Collections.singletonList(element);
                    } else {
                        if (callbackLookup == null) {
                            callbackLookup = new DOMCallbackLookup(doc);
                        }
                        elementsToSign =
                            WSSecurityUtil.findElements(encPart, callbackLookup, doc);
                    }
                    if (elementsToSign == null || elementsToSign.isEmpty()) {
                        if (!encPart.isRequired()) {
                            continue;
                        }
                        throw new WSSecurityException(
                            WSSecurityException.ErrorCode.FAILURE,
                            "noEncElement",
                            new Object[] {nmSpace + ", " + elemName});
                    }
                    for (Element elementToSign : elementsToSign) {
                        String wsuId = setWsuId(elementToSign);

                        cloneElement(elementToSign);

                        TransformParameterSpec transformSpec = null;
                        if (addInclusivePrefixes) {
                            List<String> prefixes = getInclusivePrefixes(elementToSign);
                            if (!prefixes.isEmpty()) {
                                transformSpec = new ExcC14NParameterSpec(prefixes);
                            }
                        }
                        Transform transform =
                            signatureFactory.newTransform(
                                WSConstants.C14N_EXCL_OMIT_COMMENTS,
                                transformSpec
                            );
                        javax.xml.crypto.dsig.Reference reference =
                            signatureFactory.newReference(
                                "#" + wsuId,
                                digestMethod,
                                Collections.singletonList(transform),
                                null,
                                null
                            );
                        referenceList.add(reference);
                        wsDocInfo.addTokenElement(elementToSign, false);
                    }
                }
            } catch (Exception ex) {
                LOG.error("", ex);
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILED_SIGNATURE, ex, "noXMLSig"
                );
            }
        }

        //append attachment references now
        if (attachmentReferenceList != null) {
            referenceList.addAll(attachmentReferenceList);
        }
        return referenceList;
    }

    private void cloneElement(Element element) throws WSSecurityException {
        if (expandXopInclude) {
            // Look for xop:Include Nodes
            List<Element> includeElements =
                XMLUtils.findElements(element.getFirstChild(), "Include", WSConstants.XOP_NS);
            if (includeElements != null && !includeElements.isEmpty()) {
                // Clone the Element to be signed + insert the clone into the tree at the same level
                // We will expand the xop:Include for one of the nodes + sign that (and then remove it),
                // while leaving the original in the tree to be sent in the message

                clonedElements.add(element);
                Document doc = this.getSecurityHeader().getSecurityHeaderDoc();
                element.getParentNode().appendChild(WSSecurityUtil.cloneElement(doc, element));
                WSSecurityUtil.inlineAttachments(includeElements, attachmentCallbackHandler, false);
            }
        }
    }

    private List<javax.xml.crypto.dsig.Reference> addAttachmentReferences(
        WSEncryptionPart encPart,
        DigestMethod digestMethod,
        XMLSignatureFactory signatureFactory
    ) throws WSSecurityException {

        if (attachmentCallbackHandler == null) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE,
                "empty", new Object[] {"no attachment callbackhandler supplied"}
            );
        }

        AttachmentRequestCallback attachmentRequestCallback = new AttachmentRequestCallback();
        //no mime type must be set for signature:
        //attachmentCallback.setResultingMimeType(null);
        String id = AttachmentUtils.getAttachmentId(encPart.getId());
        attachmentRequestCallback.setAttachmentId(id);
        try {
            attachmentCallbackHandler.handle(new Callback[]{attachmentRequestCallback});
        } catch (Exception e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
        }

        List<javax.xml.crypto.dsig.Reference> attachmentReferenceList = new ArrayList<>();
        if (attachmentRequestCallback.getAttachments() != null) {
            for (Attachment attachment : attachmentRequestCallback.getAttachments()) {
                try {
                    List<Transform> transforms = new ArrayList<>();

                    AttachmentTransformParameterSpec attachmentTransformParameterSpec =
                        new AttachmentTransformParameterSpec(
                            attachmentCallbackHandler, attachment
                        );

                    String attachmentSignatureTransform = WSConstants.SWA_ATTACHMENT_CONTENT_SIG_TRANS;
                    if ("Element".equals(encPart.getEncModifier())) {
                        attachmentSignatureTransform = WSConstants.SWA_ATTACHMENT_COMPLETE_SIG_TRANS;
                    }

                    transforms.add(
                        signatureFactory.newTransform(
                            attachmentSignatureTransform, attachmentTransformParameterSpec)
                        );

                    javax.xml.crypto.dsig.Reference reference =
                        signatureFactory.newReference(
                            "cid:" + attachment.getId(), digestMethod, transforms, null, null
                        );

                    attachmentReferenceList.add(reference);
                } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
                }
            }
        }

        return attachmentReferenceList;
    }

    /**
     * Get the List of inclusive prefixes from the DOM Element argument
     */
    public List<String> getInclusivePrefixes(Element target) {
        return getInclusivePrefixes(target, true);
    }


    /**
     * Get the List of inclusive prefixes from the DOM Element argument
     */
    public List<String> getInclusivePrefixes(Element target, boolean excludeVisible) {
        return SignatureUtils.getInclusivePrefixes(target, excludeVisible);
    }

    /**
     * Create an STRTransformationParameters element
     */
    public Element createSTRParameter(Document doc) {
        Element transformParam =
            doc.createElementNS(
                WSConstants.WSSE_NS,
                WSConstants.WSSE_PREFIX + ":TransformationParameters"
            );

        Element canonElem =
            doc.createElementNS(
                WSConstants.SIG_NS,
                WSConstants.SIG_PREFIX + ":CanonicalizationMethod"
            );

        canonElem.setAttributeNS(null, "Algorithm", WSConstants.C14N_EXCL_OMIT_COMMENTS);
        transformParam.appendChild(canonElem);
        return transformParam;
    }

    protected void cleanup() {
        if (!clonedElements.isEmpty()) {
            for (Element clonedElement : clonedElements) {
                clonedElement.getParentNode().removeChild(clonedElement);
            }
            clonedElements.clear();
        }
    }

}

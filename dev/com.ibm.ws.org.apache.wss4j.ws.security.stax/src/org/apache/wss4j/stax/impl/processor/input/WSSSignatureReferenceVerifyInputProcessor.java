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
package org.apache.wss4j.stax.impl.processor.input;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.wss4j.binding.wss10.TransformationParametersType;
import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.ext.AttachmentResultCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.stax.ext.WSInboundSecurityContext;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.impl.transformer.AttachmentContentSignatureTransform;
import org.apache.wss4j.stax.securityEvent.SignedPartSecurityEvent;
import org.apache.wss4j.stax.securityEvent.TimestampSecurityEvent;
import org.apache.wss4j.stax.securityToken.SecurityTokenReference;
import org.apache.wss4j.stax.utils.WSSUtils;
import org.apache.xml.security.binding.excc14n.InclusiveNamespaces;
import org.apache.xml.security.binding.xmldsig.CanonicalizationMethodType;
import org.apache.xml.security.binding.xmldsig.ReferenceType;
import org.apache.xml.security.binding.xmldsig.SignatureType;
import org.apache.xml.security.binding.xmldsig.TransformType;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.DocumentContext;
import org.apache.xml.security.stax.ext.InputProcessorChain;
import org.apache.xml.security.stax.ext.Transformer;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.XMLSecurityProperties;
import org.apache.xml.security.stax.ext.XMLSecurityUtils;
import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
import org.apache.xml.security.stax.ext.stax.XMLSecStartElement;
import org.apache.xml.security.stax.impl.processor.input.AbstractSignatureReferenceVerifyInputProcessor;
import org.apache.xml.security.stax.impl.transformer.canonicalizer.Canonicalizer20010315_Excl;
import org.apache.xml.security.stax.impl.util.DigestOutputStream;
import org.apache.xml.security.stax.securityEvent.AlgorithmSuiteSecurityEvent;
import org.apache.xml.security.stax.securityEvent.SignedElementSecurityEvent;
import org.apache.xml.security.stax.securityToken.InboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityTokenProvider;
import org.apache.xml.security.utils.UnsyncBufferedOutputStream;

public class WSSSignatureReferenceVerifyInputProcessor extends AbstractSignatureReferenceVerifyInputProcessor {

    private boolean replayChecked = false;

    public WSSSignatureReferenceVerifyInputProcessor(InputProcessorChain inputProcessorChain,
            SignatureType signatureType, InboundSecurityToken inboundSecurityToken,
            XMLSecurityProperties securityProperties) throws XMLSecurityException {
        super(inputProcessorChain, signatureType, inboundSecurityToken, securityProperties);
        this.addAfterProcessor(WSSSignatureReferenceVerifyInputProcessor.class.getName());

        checkBSPCompliance((WSInboundSecurityContext)inputProcessorChain.getSecurityContext());
    }

    @Override
    protected void verifyExternalReference(
            InputProcessorChain inputProcessorChain, InputStream inputStream,
            final ReferenceType referenceType) throws XMLSecurityException, XMLStreamException {

        if (referenceType.getURI().startsWith("cid:")) {

            CallbackHandler attachmentCallbackHandler =
                    ((WSSSecurityProperties) getSecurityProperties()).getAttachmentCallbackHandler();
            if (attachmentCallbackHandler == null) {
                throw new WSSecurityException(
                        WSSecurityException.ErrorCode.INVALID_SECURITY,
                        "empty", new Object[] {"no attachment callbackhandler supplied"}
                );
            }

            String attachmentId = AttachmentUtils.getAttachmentId(referenceType.getURI());

            AttachmentRequestCallback attachmentRequestCallback = new AttachmentRequestCallback();
            attachmentRequestCallback.setAttachmentId(attachmentId);
            try {
                attachmentCallbackHandler.handle(new Callback[]{attachmentRequestCallback});
            } catch (Exception e) {
                throw new WSSecurityException(
                        WSSecurityException.ErrorCode.INVALID_SECURITY, e);
            }
            List<Attachment> attachments = attachmentRequestCallback.getAttachments();
            if (attachments == null || attachments.isEmpty() || !attachmentId.equals(attachments.get(0).getId())) {
                throw new WSSecurityException(
                        WSSecurityException.ErrorCode.INVALID_SECURITY,
                        "empty", new Object[] {"Attachment not found"}
                );
            }

            final Attachment attachment = attachments.get(0);

            InputStream attachmentInputStream = attachment.getSourceStream();   //NOPMD
            if (!attachmentInputStream.markSupported()) {
                attachmentInputStream = new BufferedInputStream(attachmentInputStream);
            }
            //todo workaround 2GB limit somehow?
            attachmentInputStream.mark(Integer.MAX_VALUE); //we can process at maximum 2G with the standard jdk streams

            try {
                DigestOutputStream digestOutputStream =
                        createMessageDigestOutputStream(referenceType, inputProcessorChain.getSecurityContext());
                try (UnsyncBufferedOutputStream bufferedDigestOutputStream =
                        new UnsyncBufferedOutputStream(digestOutputStream)) {
                    if (referenceType.getTransforms() != null) {
                        Transformer transformer =
                                buildTransformerChain(referenceType, bufferedDigestOutputStream, inputProcessorChain, null);
                        if (!(transformer instanceof AttachmentContentSignatureTransform)) {
                            throw new WSSecurityException(
                                    WSSecurityException.ErrorCode.INVALID_SECURITY,
                                    "empty",
                                    new Object[]{"First transform must be Attachment[Content|Complete]SignatureTransform"}
                            );
                        }
                        Map<String, Object> transformerProperties = new HashMap<>(2);
                        transformerProperties.put(
                                AttachmentContentSignatureTransform.ATTACHMENT, attachment);
                        transformer.setProperties(transformerProperties);

                        transformer.transform(attachmentInputStream);
                    } else {
                        XMLSecurityUtils.copy(attachmentInputStream, bufferedDigestOutputStream);
                    }
                }
                compareDigest(digestOutputStream.getDigestValue(), referenceType);

                //reset the inputStream to be able to reuse it
                attachmentInputStream.reset();

            } catch (IOException e) {
                throw new XMLSecurityException(e);
            }

            //create a new attachment and do the result callback
            final Attachment resultAttachment = new Attachment();
            resultAttachment.setId(attachmentId);
            resultAttachment.setMimeType(attachment.getMimeType());
            resultAttachment.addHeaders(attachment.getHeaders());
            resultAttachment.setSourceStream(attachmentInputStream);

            AttachmentResultCallback attachmentResultCallback = new AttachmentResultCallback();
            attachmentResultCallback.setAttachmentId(attachmentId);
            attachmentResultCallback.setAttachment(resultAttachment);
            try {
                attachmentCallbackHandler.handle(new Callback[]{attachmentResultCallback});
            } catch (Exception e) {
                throw new WSSecurityException(
                        WSSecurityException.ErrorCode.INVALID_SECURITY, e);
            }

            // Create a security event for this signed Attachment
            final DocumentContext documentContext = inputProcessorChain.getDocumentContext();
            SignedPartSecurityEvent signedPartSecurityEvent =
                new SignedPartSecurityEvent(getInboundSecurityToken(), true, documentContext.getProtectionOrder());
            signedPartSecurityEvent.setAttachment(true);
            signedPartSecurityEvent.setCorrelationID(referenceType.getId());
            inputProcessorChain.getSecurityContext().registerSecurityEvent(signedPartSecurityEvent);
        } else {
            super.verifyExternalReference(
                    inputProcessorChain, inputStream, referenceType);
        }
    }

    private void checkBSPCompliance(WSInboundSecurityContext securityContext) throws WSSecurityException {
        List<ReferenceType> references = getSignatureType().getSignedInfo().getReference();
        for (int i = 0; i < references.size(); i++) {
            ReferenceType referenceType = references.get(i);
            if (referenceType.getTransforms() == null) {
                securityContext.handleBSPRule(BSPRule.R5416);
            } else if (referenceType.getTransforms().getTransform().isEmpty()) {
                securityContext.handleBSPRule(BSPRule.R5411);
            } else {
                List<TransformType> transformTypes = referenceType.getTransforms().getTransform();
                for (int j = 0; j < transformTypes.size(); j++) {
                    TransformType transformType = transformTypes.get(j);
                    final String algorithm = transformType.getAlgorithm();
                    if (!WSSConstants.NS_C14N_EXCL.equals(algorithm)
                            && !WSSConstants.NS_XMLDSIG_FILTER2.equals(algorithm)
                            && !WSSConstants.SOAPMESSAGE_NS10_STR_TRANSFORM.equals(algorithm)
                            && !WSSConstants.NS_XMLDSIG_ENVELOPED_SIGNATURE.equals(algorithm)
                            && !WSSConstants.SWA_ATTACHMENT_CONTENT_SIG_TRANS.equals(algorithm)
                            && !WSSConstants.SWA_ATTACHMENT_COMPLETE_SIG_TRANS.equals(algorithm)) {
                        securityContext.handleBSPRule(BSPRule.R5423);
                        if (j == transformTypes.size() - 1
                            && !WSSConstants.NS_C14N_EXCL.equals(algorithm)
                            && !WSSConstants.SOAPMESSAGE_NS10_STR_TRANSFORM.equals(algorithm)
                            && !WSSConstants.SWA_ATTACHMENT_CONTENT_SIG_TRANS.equals(algorithm)
                            && !WSSConstants.SWA_ATTACHMENT_COMPLETE_SIG_TRANS.equals(algorithm)) {
                            securityContext.handleBSPRule(BSPRule.R5412);
                        }
                        InclusiveNamespaces inclusiveNamespacesType =
                            XMLSecurityUtils.getQNameType(transformType.getContent(),
                                                          XMLSecurityConstants.TAG_c14nExcl_InclusiveNamespaces);
                        if (WSSConstants.NS_C14N_EXCL.equals(algorithm)
                                && inclusiveNamespacesType != null
                                && inclusiveNamespacesType.getPrefixList().isEmpty()) {
                            securityContext.handleBSPRule(BSPRule.R5407);
                        }
                        if (WSSConstants.SOAPMESSAGE_NS10_STR_TRANSFORM.equals(algorithm)) {
                            if (inclusiveNamespacesType != null
                                    && inclusiveNamespacesType.getPrefixList().isEmpty()) {
                                securityContext.handleBSPRule(BSPRule.R5413);
                            }
                            TransformationParametersType transformationParametersType =
                                    XMLSecurityUtils.getQNameType(transformType.getContent(),
                                                                  WSSConstants.TAG_WSSE_TRANSFORMATION_PARAMETERS);
                            if (transformationParametersType == null) {
                                securityContext.handleBSPRule(BSPRule.R3065);
                            } else {
                                CanonicalizationMethodType canonicalizationMethodType =
                                        XMLSecurityUtils.getQNameType(transformationParametersType.getAny(),
                                                                      WSSConstants.TAG_dsig_CanonicalizationMethod);
                                if (canonicalizationMethodType == null) {
                                    securityContext.handleBSPRule(BSPRule.R3065);
                                }
                            }
                        }
                    }
                }
            }
            if (!(WSSConstants.NS_XMLDSIG_SHA1.equals(referenceType.getDigestMethod().getAlgorithm())
                || WSSConstants.NS_XENC_SHA256.equals(referenceType.getDigestMethod().getAlgorithm())
                || WSSConstants.NS_XENC_SHA512.equals(referenceType.getDigestMethod().getAlgorithm()))) {
                // Weakening this a bit to allow SHA > 1
                securityContext.handleBSPRule(BSPRule.R5420);
            }
        }
    }

    @Override
    public XMLSecEvent processEvent(InputProcessorChain inputProcessorChain) throws XMLStreamException, XMLSecurityException {

        //this is the earliest possible point to check for an replay attack
        if (!replayChecked) {
            replayChecked = true;
            detectReplayAttack(inputProcessorChain);
        }
        return super.processEvent(inputProcessorChain);
    }

    @Override
    protected void processElementPath(List<QName> elementPath, InputProcessorChain inputProcessorChain,
                                      XMLSecEvent xmlSecEvent, ReferenceType referenceType)
            throws XMLSecurityException {
        //fire a SecurityEvent:
        final DocumentContext documentContext = inputProcessorChain.getDocumentContext();
        if (elementPath.size() == 3 && WSSUtils.isInSOAPHeader(elementPath)
                || elementPath.size() == 2 && WSSUtils.isInSOAPBody(elementPath)) {
            SignedPartSecurityEvent signedPartSecurityEvent =
                    new SignedPartSecurityEvent(getInboundSecurityToken(), true, documentContext.getProtectionOrder());
            signedPartSecurityEvent.setElementPath(elementPath);
            signedPartSecurityEvent.setXmlSecEvent(xmlSecEvent);
            signedPartSecurityEvent.setCorrelationID(referenceType.getId());
            inputProcessorChain.getSecurityContext().registerSecurityEvent(signedPartSecurityEvent);
        } else {
            SignedElementSecurityEvent signedElementSecurityEvent =
                    new SignedElementSecurityEvent(getInboundSecurityToken(), true, documentContext.getProtectionOrder());
            signedElementSecurityEvent.setElementPath(elementPath);
            signedElementSecurityEvent.setXmlSecEvent(xmlSecEvent);
            signedElementSecurityEvent.setCorrelationID(referenceType.getId());
            inputProcessorChain.getSecurityContext().registerSecurityEvent(signedElementSecurityEvent);
        }
    }

    @Override
    protected InternalSignatureReferenceVerifier getSignatureReferenceVerifier(
            XMLSecurityProperties securityProperties, InputProcessorChain inputProcessorChain,
            ReferenceType referenceType, XMLSecStartElement startElement) throws XMLSecurityException {
        return new WSS4JInternalSignatureReferenceVerifier((WSSSecurityProperties) securityProperties,
                inputProcessorChain, referenceType, startElement);
    }

    private void detectReplayAttack(InputProcessorChain inputProcessorChain) throws WSSecurityException {
        TimestampSecurityEvent timestampSecurityEvent =
                inputProcessorChain.getSecurityContext().get(WSSConstants.PROP_TIMESTAMP_SECURITYEVENT);
        ReplayCache replayCache =   //NOPMD
            ((WSSSecurityProperties)getSecurityProperties()).getTimestampReplayCache();
        if (timestampSecurityEvent != null && replayCache != null) {
            final String cacheKey =
                    timestampSecurityEvent.getCreated().get(ChronoField.MILLI_OF_SECOND)
                    + "" + Arrays.hashCode(getSignatureType().getSignatureValue().getValue());
            if (replayCache.contains(cacheKey)) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.MESSAGE_EXPIRED);
            }

            // Store the Timestamp/SignatureValue combination in the cache
            Instant expires = timestampSecurityEvent.getExpires();
            if (expires != null) {
                replayCache.add(cacheKey, expires);
            } else {
                replayCache.add(cacheKey);
            }
        }
    }

    @Override
    protected Transformer buildTransformerChain(
            ReferenceType referenceType, OutputStream outputStream,
            InputProcessorChain inputProcessorChain,
            AbstractSignatureReferenceVerifyInputProcessor.InternalSignatureReferenceVerifier internalSignatureReferenceVerifier)
            throws XMLSecurityException {

        if (referenceType.getTransforms() == null || referenceType.getTransforms().getTransform().isEmpty()) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }
        List<TransformType> transformTypeList = referenceType.getTransforms().getTransform();

        if (transformTypeList.size() > maximumAllowedTransformsPerReference) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY,
                    "secureProcessing.MaximumAllowedTransformsPerReference",
                    new Object[] {transformTypeList.size(), maximumAllowedTransformsPerReference});
        }

        String algorithm = null;
        Transformer parentTransformer = null;
        for (int i = transformTypeList.size() - 1; i >= 0; i--) {
            TransformType transformType = transformTypeList.get(i);
            TransformationParametersType transformationParametersType =
                    XMLSecurityUtils.getQNameType(transformType.getContent(), WSSConstants.TAG_WSSE_TRANSFORMATION_PARAMETERS);
            if (transformationParametersType != null) {
                CanonicalizationMethodType canonicalizationMethodType =
                        XMLSecurityUtils.getQNameType(transformationParametersType.getAny(),
                                                      WSSConstants.TAG_dsig_CanonicalizationMethod);
                if (canonicalizationMethodType != null) {

                    algorithm = canonicalizationMethodType.getAlgorithm();

                    InclusiveNamespaces inclusiveNamespacesType =
                            XMLSecurityUtils.getQNameType(canonicalizationMethodType.getContent(),
                                                          XMLSecurityConstants.TAG_c14nExcl_InclusiveNamespaces);

                    Map<String, Object> transformerProperties = null;
                    if (inclusiveNamespacesType != null) {
                        transformerProperties = new HashMap<>();
                        transformerProperties.put(
                                Canonicalizer20010315_Excl.INCLUSIVE_NAMESPACES_PREFIX_LIST,
                                inclusiveNamespacesType.getPrefixList());
                    }
                    parentTransformer = WSSUtils.getTransformer(
                            null, outputStream, transformerProperties, algorithm, XMLSecurityConstants.DIRECTION.IN);
                }
            }
            algorithm = transformType.getAlgorithm();
            AlgorithmSuiteSecurityEvent algorithmSuiteSecurityEvent = new AlgorithmSuiteSecurityEvent();
            algorithmSuiteSecurityEvent.setAlgorithmURI(algorithm);
            algorithmSuiteSecurityEvent.setAlgorithmUsage(WSSConstants.SigTransform);
            algorithmSuiteSecurityEvent.setCorrelationID(referenceType.getId());
            inputProcessorChain.getSecurityContext().registerSecurityEvent(algorithmSuiteSecurityEvent);

            InclusiveNamespaces inclusiveNamespacesType =
                    XMLSecurityUtils.getQNameType(transformType.getContent(),
                                                  XMLSecurityConstants.TAG_c14nExcl_InclusiveNamespaces);

            Map<String, Object> transformerProperties = null;
            if (inclusiveNamespacesType != null) {
                transformerProperties = new HashMap<>();
                transformerProperties.put(
                        Canonicalizer20010315_Excl.INCLUSIVE_NAMESPACES_PREFIX_LIST,
                        inclusiveNamespacesType.getPrefixList());
            }

            if (parentTransformer != null) {
                parentTransformer = WSSUtils.getTransformer(
                        parentTransformer, null, transformerProperties, algorithm, XMLSecurityConstants.DIRECTION.IN);
            } else {
                parentTransformer = WSSUtils.getTransformer(
                        null, outputStream, transformerProperties, algorithm, XMLSecurityConstants.DIRECTION.IN);
            }
        }

        if (WSSConstants.SOAPMESSAGE_NS10_STR_TRANSFORM.equals(algorithm)) {

            internalSignatureReferenceVerifier.setTransformer(parentTransformer);

            String uri = XMLSecurityUtils.dropReferenceMarker(referenceType.getURI());
            SecurityTokenProvider<? extends InboundSecurityToken> securityTokenProvider =
                    inputProcessorChain.getSecurityContext().getSecurityTokenProvider(uri);
            if (securityTokenProvider == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, "noReference");
            }
            SecurityToken securityToken = securityTokenProvider.getSecurityToken();
            if (!(securityToken instanceof SecurityTokenReference)) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN);
            }
            SecurityTokenReference securityTokenReference = (SecurityTokenReference) securityToken;
            //todo analyse and fix me: the following statement could be problematic
            int index = inputProcessorChain.getProcessors().indexOf(internalSignatureReferenceVerifier);
            inputProcessorChain.getDocumentContext().setIsInSignedContent(index, internalSignatureReferenceVerifier);
            XMLSecStartElement xmlSecStartElement = securityTokenReference.getXmlSecEvents().getLast().asStartElement();
            internalSignatureReferenceVerifier.setStartElement(xmlSecStartElement);
            Iterator<XMLSecEvent> xmlSecEventIterator = securityTokenReference.getXmlSecEvents().descendingIterator();
            try {
                while (xmlSecEventIterator.hasNext()) {
                    internalSignatureReferenceVerifier.processEvent(xmlSecEventIterator.next(), inputProcessorChain);
                }
            } catch (XMLStreamException e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, e);
            }
        }
        return parentTransformer;
    }

    class WSS4JInternalSignatureReferenceVerifier
            extends AbstractSignatureReferenceVerifyInputProcessor.InternalSignatureReferenceVerifier {

        WSS4JInternalSignatureReferenceVerifier(WSSSecurityProperties securityProperties, InputProcessorChain inputProcessorChain,
                                           ReferenceType referenceType, XMLSecStartElement startElement) throws XMLSecurityException {
            super(securityProperties, inputProcessorChain, referenceType, startElement);
            this.addAfterProcessor(WSSSignatureReferenceVerifyInputProcessor.class.getName());
        }
    }
}

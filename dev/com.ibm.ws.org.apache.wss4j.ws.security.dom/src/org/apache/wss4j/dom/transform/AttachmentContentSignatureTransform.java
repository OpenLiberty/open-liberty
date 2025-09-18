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

import org.apache.jcp.xml.dsig.internal.dom.ApacheNodeSetData;
import org.apache.jcp.xml.dsig.internal.dom.ApacheOctetStreamData;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.ext.AttachmentResultCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.common.util.CRLFOutputStream;
import org.apache.wss4j.dom.WSConstants;
import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.apache.xml.security.parser.XMLParserException;
import org.apache.xml.security.signature.XMLSignatureInput;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.crypto.Data;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.TransformException;
import javax.xml.crypto.dsig.TransformService;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.List;

public class AttachmentContentSignatureTransform extends TransformService {

    public static final String TRANSFORM_URI = WSConstants.SWA_ATTACHMENT_CONTENT_SIG_TRANS;
    public static final String ATTACHMENT_CALLBACKHANDLER = "AttachmentContentTransform.attachmentCallbackHandler";

    private AttachmentTransformParameterSpec attachmentTransformParameterSpec;

    @Override
    public void init(TransformParameterSpec params) throws InvalidAlgorithmParameterException {
        if (!(params instanceof AttachmentTransformParameterSpec)) {
            throw new InvalidAlgorithmParameterException("Expected AttachmentTransformParameterSpec");
        }
        this.attachmentTransformParameterSpec = (AttachmentTransformParameterSpec) params;
    }

    protected AttachmentTransformParameterSpec getAttachmentTransformParameterSpec() {
        return attachmentTransformParameterSpec;
    }

    @Override
    public void init(XMLStructure parent, XMLCryptoContext context) throws InvalidAlgorithmParameterException {
    }

    @Override
    public void marshalParams(XMLStructure parent, XMLCryptoContext context) throws MarshalException {
    }

    @Override
    public AlgorithmParameterSpec getParameterSpec() {
        return attachmentTransformParameterSpec;
    }

    @Override
    public Data transform(Data data, XMLCryptoContext context) throws TransformException {
        return transform(data, context, null);
    }

    /*
     * http://docs.oasis-open.org/wss-m/wss/v1.1.1/os/wss-SwAProfile-v1.1.1-os.html
     * 5.2 Referencing Attachments
     * This profile assumes, since it is not defined in RFC 2396 Section 4.2, that
     * all cid: references are not same-document references and that therefore, under
     * XMLDSIG, dereferencing a cid: URI always yields an octet stream as input to the
     * transform chain [RFC2396], [XMLDSIG].
     */
    @Override
    public Data transform(Data data, XMLCryptoContext context, OutputStream os) throws TransformException {

        String attachmentUri = ((ApacheOctetStreamData) data).getURI();
        String attachmentId = null;
        try {
            attachmentId = AttachmentUtils.getAttachmentId(attachmentUri);
        } catch (WSSecurityException e) {
            throw new TransformException(e);
        }

        Attachment attachment;
        if (attachmentTransformParameterSpec != null) {
            attachment = attachmentTransformParameterSpec.getAttachment();
            context.setProperty(ATTACHMENT_CALLBACKHANDLER,
                                attachmentTransformParameterSpec.getAttachmentCallbackHandler());
        } else {
            attachment = attachmentRequestCallback(context, attachmentId);
        }
        return processAttachment(context, os, attachmentUri, attachment);
    }

    protected Attachment attachmentRequestCallback(XMLCryptoContext context, String attachmentId)
        throws TransformException {
        CallbackHandler attachmentCallbackHandler =
            (CallbackHandler) context.getProperty(ATTACHMENT_CALLBACKHANDLER);
        if (attachmentCallbackHandler == null) {
            throw new TransformException("No attachment callbackhandler supplied");
        }
        AttachmentRequestCallback attachmentRequestCallback = new AttachmentRequestCallback();
        attachmentRequestCallback.setAttachmentId(attachmentId);
        try {
            attachmentCallbackHandler.handle(new Callback[]{attachmentRequestCallback});
        } catch (Exception e) {
            throw new TransformException(e);
        }
        List<Attachment> attachments = attachmentRequestCallback.getAttachments();
        if (attachments == null || attachments.isEmpty() || !attachmentId.equals(attachments.get(0).getId())) {
            throw new TransformException("Attachment not found");
        }
        return attachments.get(0);
    }

    protected void attachmentResultCallback(XMLCryptoContext context, Attachment attachment)
        throws TransformException {
        CallbackHandler attachmentCallbackHandler =
            (CallbackHandler) context.getProperty(ATTACHMENT_CALLBACKHANDLER);
        if (attachmentCallbackHandler == null) {
            throw new TransformException("No attachment callbackhandler supplied");
        }
        AttachmentResultCallback attachmentResultCallback = new AttachmentResultCallback();
        attachmentResultCallback.setAttachmentId(attachment.getId());
        attachmentResultCallback.setAttachment(attachment);
        try {
            attachmentCallbackHandler.handle(new Callback[]{attachmentResultCallback});
        } catch (Exception e) {
            throw new TransformException(e);
        }
    }

    @SuppressWarnings("resource")
    protected Data processAttachment(XMLCryptoContext context, OutputStream os, String attachmentUri,
                                     Attachment attachment) throws TransformException {
        try {
            //try to reuse the inputStream in the hope that the provided inputStream is backed by a disk storage
            InputStream inputStream = attachment.getSourceStream();
            if (!inputStream.markSupported()) {
                inputStream = new BufferedInputStream(inputStream);
            }
            inputStream.mark(Integer.MAX_VALUE); //we can process at maximum 2G with the standard jdk streams
            inputStream = new FilterInputStream(inputStream) {
                @Override
                public void close() throws IOException {
                    //I hate stuff which are closing _my_ streams!
                }
            };

            OutputStream outputStream = os;
            if (outputStream == null) {
                outputStream = new ByteArrayOutputStream();
            }

            String mimeType = attachment.getMimeType();

            if (mimeType != null
                && (mimeType.matches("(?i)(text/xml).*")
                    || mimeType.matches("(?i)(application/xml).*")
                    || mimeType.matches("(?i)(application|image)/.*\\+xml.*"))) {
                /* 5.4.2:
                 * Content of an XML Content-Type MUST be XML canonicalized using
                 * Exclusive XML Canonicalization without comments, as specified by
                 * the URI http://www.w3.org/2001/10/xml-exc-c14n# [Excl-Canon].
                 * The reason for requiring Exclusive Canonicalization is that many
                 * implementations will support Exclusive Canonicalization for other
                 * XML Signature purposes, since this form of canonicalization
                 * supports context changes. The InclusiveNamespace PrefixList
                 * attribute SHOULD be empty or not present.
                 */
                Canonicalizer canon = Canonicalizer.getInstance(WSConstants.C14N_EXCL_OMIT_COMMENTS);

                XMLSignatureInput xmlSignatureInput = new XMLSignatureInput(inputStream);
                canon.canonicalizeXPathNodeSet(xmlSignatureInput.getNodeSet(), outputStream);

            } else if (mimeType != null && mimeType.matches("(?i)(text/).*")) {
                CRLFOutputStream crlfOutputStream = new CRLFOutputStream(outputStream);
                int numBytes;
                byte[] buf = new byte[8192];
                while ((numBytes = inputStream.read(buf)) != -1) {
                    crlfOutputStream.write(buf, 0, numBytes);
                }

            } else {
                int numBytes;
                byte[] buf = new byte[8192];
                while ((numBytes = inputStream.read(buf)) != -1) {
                    outputStream.write(buf, 0, numBytes);
                }
            }

            //reset the inputStream to be able to reuse it
            inputStream.reset();

            //create a new attachment and do the result callback
            final Attachment resultAttachment = new Attachment();
            resultAttachment.setId(attachment.getId());
            resultAttachment.setMimeType(mimeType);
            resultAttachment.addHeaders(attachment.getHeaders());
            resultAttachment.setSourceStream(inputStream);
            attachmentResultCallback(context, resultAttachment);

            if (os == null) {
                return new OctetStreamData(
                        new ByteArrayInputStream(
                                ((ByteArrayOutputStream)outputStream).toByteArray()
                        ),
                        attachmentUri, mimeType);
            }
            return new ApacheNodeSetData(new XMLSignatureInput((byte[])null));
        } catch (IOException | InvalidCanonicalizerException | CanonicalizationException
            | XMLParserException e) {
            throw new TransformException(e);
        }
    }

    @Override
    public boolean isFeatureSupported(String feature) {
        if (feature == null) {
            throw new NullPointerException();
        } else {
            return false;
        }
    }
}

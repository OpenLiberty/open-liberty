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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.jcp.xml.dsig.internal.dom.ApacheOctetStreamData;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.dom.WSConstants;

import javax.xml.crypto.Data;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.TransformException;

public class AttachmentCompleteSignatureTransform extends AttachmentContentSignatureTransform {

    public static final String TRANSFORM_URI = WSConstants.SWA_ATTACHMENT_COMPLETE_SIG_TRANS;

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
        String attachmentId = attachmentUri.substring(4);

        AttachmentTransformParameterSpec attachmentTransformParameterSpec = getAttachmentTransformParameterSpec();

        Attachment attachment;
        if (attachmentTransformParameterSpec != null) {
            attachment = attachmentTransformParameterSpec.getAttachment();
            context.setProperty(ATTACHMENT_CALLBACKHANDLER,
                                attachmentTransformParameterSpec.getAttachmentCallbackHandler());
        } else {
            attachment = attachmentRequestCallback(context, attachmentId);
        }

        try {
            OutputStream outputStream = os;
            if (outputStream == null) {
                outputStream = new ByteArrayOutputStream(); //NOPMD
            }
            AttachmentUtils.canonizeMimeHeaders(os, attachment.getHeaders());
            processAttachment(context, os, attachmentUri, attachment);

            if (os == null) {
                String mimeType = attachment.getMimeType();
                return new OctetStreamData(
                        new ByteArrayInputStream(
                                ((ByteArrayOutputStream)outputStream).toByteArray()
                        ),
                        attachmentUri, mimeType);
            }
            return null;
        } catch (IOException e) {
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

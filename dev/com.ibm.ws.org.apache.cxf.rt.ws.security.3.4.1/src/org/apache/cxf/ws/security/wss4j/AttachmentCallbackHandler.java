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

package org.apache.cxf.ws.security.wss4j;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.attachment.AttachmentDataSource;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.wss4j.common.ext.AttachmentRemovalCallback;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.ext.AttachmentResultCallback;

/**
 * A CallbackHandler to be used to sign/encrypt SOAP Attachments.
 */
public class AttachmentCallbackHandler implements CallbackHandler {

    private final Collection<org.apache.cxf.message.Attachment> attachments;

    public AttachmentCallbackHandler(Message message) {
        if (message.getAttachments() == null) {
            message.setAttachments(new ArrayList<Attachment>());
        }
        attachments = message.getAttachments();
    }

    public AttachmentCallbackHandler(Collection<org.apache.cxf.message.Attachment> attachments) {
        this.attachments = attachments;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof AttachmentRequestCallback) {
                AttachmentRequestCallback attachmentRequestCallback = (AttachmentRequestCallback) callback;

                List<org.apache.wss4j.common.ext.Attachment> attachmentList = new ArrayList<>();
                attachmentRequestCallback.setAttachments(attachmentList);

                String attachmentId = attachmentRequestCallback.getAttachmentId();
                if ("Attachments".equals(attachmentId)) {
                    // Load all attachments
                    attachmentId = null;
                }
                loadAttachments(attachmentList, attachmentId, attachmentRequestCallback.isRemoveAttachments());
            } else if (callback instanceof AttachmentResultCallback) {
                AttachmentResultCallback attachmentResultCallback = (AttachmentResultCallback) callback;

                org.apache.cxf.attachment.AttachmentImpl securedAttachment =
                    new org.apache.cxf.attachment.AttachmentImpl(
                        attachmentResultCallback.getAttachmentId(),
                        new DataHandler(
                            new AttachmentDataSource(
                                attachmentResultCallback.getAttachment().getMimeType(),
                                attachmentResultCallback.getAttachment().getSourceStream())
                        )
                    );

                Map<String, String> headers = attachmentResultCallback.getAttachment().getHeaders();
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    securedAttachment.setHeader(entry.getKey(), entry.getValue());
                }
                attachments.add(securedAttachment);

            } else if (callback instanceof AttachmentRemovalCallback) {
                AttachmentRemovalCallback attachmentRemovalCallback = (AttachmentRemovalCallback) callback;
                String attachmentId = attachmentRemovalCallback.getAttachmentId();
                if (attachmentId != null) {
                    // Calling LazyAttachmentCollection.size() here to force it to load the attachments
                    if (attachments != null && attachments.size() > 0) {  // NOPMD
                        for (Iterator<org.apache.cxf.message.Attachment> iterator = attachments.iterator();
                            iterator.hasNext();) {
                            org.apache.cxf.message.Attachment attachment = iterator.next();

                            if (attachmentId.equals(attachment.getId())) {
                                iterator.remove();
                                break;
                            }
                        }
                    }
                }
            } else {
                throw new UnsupportedCallbackException(callback, "Unsupported callback");
            }
        }
    }

    private void loadAttachments(
        List<org.apache.wss4j.common.ext.Attachment> attachmentList,
        String attachmentId,
        boolean removeAttachments
    ) throws IOException {
        // Calling LazyAttachmentCollection.size() here to force it to load the attachments
        if (attachments != null && attachments.size() > 0) { // NOPMD
            for (Iterator<org.apache.cxf.message.Attachment> iterator = attachments.iterator();
                iterator.hasNext();) {
                org.apache.cxf.message.Attachment attachment = iterator.next();

                if (attachmentId != null
                        && !(attachmentId.equals(attachment.getId())
                            || attachmentId.equals(getDecodedAttachmentId(attachment.getId())))) {
                    continue;
                }

                org.apache.wss4j.common.ext.Attachment att =
                    new org.apache.wss4j.common.ext.Attachment();
                att.setMimeType(attachment.getDataHandler().getContentType());
                att.setId(attachmentId == null ? attachment.getId() : attachmentId);
                att.setSourceStream(attachment.getDataHandler().getInputStream());
                Iterator<String> headerIterator = attachment.getHeaderNames();
                while (headerIterator.hasNext()) {
                    String next = headerIterator.next();
                    att.addHeader(next, attachment.getHeader(next));
                }
                attachmentList.add(att);

                if (removeAttachments) {
                    iterator.remove();
                }
            }
        }
    }

    private static String getDecodedAttachmentId(String attachmentId) throws IOException {
        return URLDecoder.decode(attachmentId, StandardCharsets.UTF_8.name());
    }

}

/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.transport.client.test;

import java.io.File;
import java.util.Locale;

import com.ibm.ws.repository.common.enums.AttachmentLinkType;
import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.AttachmentSummary;

/**
 *
 */
public class MockAttachmentSummary implements AttachmentSummary {

    private final Attachment attachment;
    private final File file;

    public MockAttachmentSummary(File file, String name, AttachmentType type, long crc, String url) {
        this.file = file;
        attachment = new Attachment();

        attachment.setName(name);

        if (type == null) {
            // if we explicitly passed a null Attachment.Type do not set the attachment
            // type ... this is only for testing purposes
        } else {
            attachment.setType(type); // must set a Type
        }

        attachment.getWlpInformation().setCRC(crc);

        if (url != null && !url.isEmpty()) {
            attachment.setLinkType(AttachmentLinkType.DIRECT);
            attachment.setUrl(url);
        }
    }

    @Override
    public String getName() {
        return attachment.getName();
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public String getURL() {
        return attachment.getUrl();
    }

    @Override
    public Attachment getAttachment() {
        return attachment;
    }

    @Override
    public Locale getLocale() {
        return attachment.getLocale();
    }

}

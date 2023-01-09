/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.jboss.resteasy.plugins.providers.multipart;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MediaType;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;
import com.ibm.websphere.jaxrs20.multipart.IMultipartBody;


public class IMultipartBodyImpl implements IMultipartBody {

    private final MultipartInputImpl multipartInput;
    private final List<IAttachment> attachments;

    public IMultipartBodyImpl(MultipartInputImpl multipartInput) throws IOException {
        this.multipartInput = multipartInput;
        attachments = new ArrayList<>();
        for (InputPart inputPart : multipartInput.getParts()) {
            attachments.add(new IAttachmentImpl(inputPart));
        }
    }

    @Override
    public MediaType getType() {
        return multipartInput.contentType;
    }

    @Override
    public List<IAttachment> getAllAttachments() {
        return attachments;
    }

    @Override
    public List<IAttachment> getChildAttachments() {
        int size = attachments.size();
        return size > 1 ? attachments.subList(1, size-1) : Collections.emptyList();
    }

    @Override
    public IAttachment getRootAttachment() {
        return attachments.isEmpty() ? null : attachments.get(0);
    }

    @Override
    public IAttachment getAttachment(String contentId) {
        return attachments.stream()
                          .filter(a -> a.getContentId().equals(contentId))
                          .findFirst()
                          .orElse(null);
    }

    public MultipartInput getMultipartInput() {
        return multipartInput;
    }
}

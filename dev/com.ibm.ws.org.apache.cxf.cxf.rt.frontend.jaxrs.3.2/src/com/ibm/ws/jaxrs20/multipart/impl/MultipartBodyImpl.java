/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.multipart.impl;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;
import com.ibm.websphere.jaxrs20.multipart.IMultipartBody;

public class MultipartBodyImpl implements IMultipartBody {

    /**
     * @return the multipartBody
     */
    public MultipartBody getMultipartBody() {
        return multipartBody;
    }

    private final MultipartBody multipartBody;

    public MultipartBodyImpl(MultipartBody multipartBody) {
        this.multipartBody = multipartBody;
    }

    @Override
    public MediaType getType() {
        return this.multipartBody.getType();
    }

    @Override
    public List<IAttachment> getAllAttachments() {
        List<Attachment> attList = this.multipartBody.getAllAttachments();
        return convert2IAttachmentList(attList);
    }

    /**
     * @param attList
     * @return
     */
    private List<IAttachment> convert2IAttachmentList(List<Attachment> attList) {
        // TODO Auto-generated method stub
        List<IAttachment> iattList = new ArrayList<IAttachment>();
        for (Attachment att : attList) {
            IAttachment iatt = new AttachmentImpl(att);
            iattList.add(iatt);
        }
        return iattList;
    }

    @Override
    public List<IAttachment> getChildAttachments() {
        return (convert2IAttachmentList(multipartBody.getChildAttachments()));
    }

    @Override
    public IAttachment getRootAttachment() {
        Attachment att = this.multipartBody.getRootAttachment();
        return new AttachmentImpl(att);
    }

    @Override
    public IAttachment getAttachment(String contentId) {
        Attachment att = this.multipartBody.getAttachment(contentId);
        if (att != null) {
            return new AttachmentImpl(att);
        }
        return null;
    }

    public <T> T getAttachmentObject(String contentId, Class<T> cls) {
        IAttachment att = getAttachment(contentId);
        if ((att != null) && AttachmentImpl.class.isAssignableFrom(att.getClass())) {
            AttachmentImpl attImp = (AttachmentImpl) att;
            return attImp.getObject(cls);
        }
        return null;
    }
}

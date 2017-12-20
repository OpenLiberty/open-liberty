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

import java.io.File;
import java.io.IOException;

import javax.activation.DataHandler;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.io.Transferable;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;

/**
 * This class represents an attachment; generally a multipart part.
 */
public class AttachmentImpl implements Transferable, IAttachment {

    private final Attachment attachment;

    /**
     * @return the attachment
     */
    public Attachment getAttachment() {
        return attachment;
    }

    public AttachmentImpl(Attachment attachment) {
        this.attachment = attachment;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.jaxrs20.multipart.IAttachment#getContentId()
     */
    @Override
    public String getContentId() {
        // TODO Auto-generated method stub
        return attachment.getContentId();
    }

    public Object getObject() {
        // TODO Auto-generated method stub
        return attachment.getObject();
    }

    public <T> T getObject(Class<T> cls) {
        // TODO Auto-generated method stub
        return (T) attachment.getObject();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.jaxrs20.multipart.IAttachment#getContentType()
     */
    @Override
    public MediaType getContentType() {
        // TODO Auto-generated method stub
        return attachment.getContentType();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.jaxrs20.multipart.IAttachment#getDataHandler()
     */
    @Override
    public DataHandler getDataHandler() {
        // TODO Auto-generated method stub
        return attachment.getDataHandler();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.jaxrs20.multipart.IAttachment#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String name) {
        // TODO Auto-generated method stub
        return attachment.getHeader(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.jaxrs20.multipart.IAttachment#getHeaders()
     */
    @Override
    public MultivaluedMap<String, String> getHeaders() {
        // TODO Auto-generated method stub
        return attachment.getHeaders();
    }

    public ContentDisposition getContentDisposition() {
        return attachment.getContentDisposition();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.io.Transferable#transferTo(java.io.File)
     */
    @Override
    public void transferTo(File file) throws IOException {
        // TODO Auto-generated method stub
        attachment.transferTo(file);
    }

    @Override
    public int hashCode() {
        return attachment.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AttachmentImpl)) {
            return false;
        }
        return attachment.equals(((AttachmentImpl) o).getAttachment());
    }

}

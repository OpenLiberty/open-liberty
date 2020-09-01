/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.jboss.resteasy.plugins.providers.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInputImpl.PartImpl;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;


public class IAttachmentImpl implements IAttachment {

    private final InputPart inputPart;
    private final DataHandler dataHandler = new DataHandler(new DataSource() {

        @Override
        public InputStream getInputStream() throws IOException {
            return (inputPart instanceof PartImpl) ? ((PartImpl)inputPart).getBody() : null;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getContentType() {
            return getContentType().toString();
        }

        @Override
        public String getName() {
            return getContentId();
        }});

    IAttachmentImpl(InputPart inputPart) {
        this.inputPart = inputPart;
    }

    @Override
    public String getContentId() {
        return getHeader("Content-ID");
    }

    @Override
    public MediaType getContentType() {
        return inputPart.getMediaType();
    }

    @Override
    public DataHandler getDataHandler() {
        return dataHandler;
    }

    @Override
    public String getHeader(String name) {
        return inputPart.getHeaders().getFirst(name);
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return inputPart.getHeaders();
    }

}

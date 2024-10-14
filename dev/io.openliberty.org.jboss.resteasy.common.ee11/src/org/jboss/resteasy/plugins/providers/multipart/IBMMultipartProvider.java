/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;
import com.ibm.websphere.jaxrs20.multipart.IMultipartBody;

import org.jboss.resteasy.spi.InternalServerErrorException;

@Provider
@Consumes({"multipart/related", "multipart/mixed", "multipart/alternative", "multipart/form-data" })
@Produces({"multipart/related", "multipart/mixed", "multipart/alternative", "multipart/form-data" })
public class IBMMultipartProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

    private static final Set<Class<?>> MULTIPART_CLASSES = new HashSet<>();
    private static final Set<String> MULTIPART_SUBTYPES = new HashSet<>();
    static {
        MULTIPART_CLASSES.add(IMultipartBody.class);
        MULTIPART_CLASSES.add(IAttachment.class);

        MULTIPART_SUBTYPES.add("form-data");
        MULTIPART_SUBTYPES.add("mixed");
        MULTIPART_SUBTYPES.add("related");
        MULTIPART_SUBTYPES.add("alternative");
    }

    @Context
    Providers providers;

    private LibertyMultipartWriter writer = new LibertyMultipartWriter();

    @Override
    public boolean isReadable(Class<?> clazz, Type type, Annotation[] anns, MediaType mt) {
        return isSupported(clazz, anns, mt);
    }

    @Override
    public boolean isWriteable(Class<?> clazz, Type type, Annotation[] anns, MediaType mt) {
        return isSupported(clazz, anns, mt);
    }

    private boolean isSupported(Class<?> type, Annotation[] anns, MediaType mt) {
        return mediaTypeSupported(mt)
            && (MULTIPART_CLASSES.contains(type));
    }

    private boolean mediaTypeSupported(MediaType mt) {
        return "multipart".equals(mt.getType()) && MULTIPART_SUBTYPES.contains(mt.getSubtype());
    }

    @Override
    public Object readFrom(Class<Object> clazz, Type genericType, Annotation[] anns, MediaType mt, MultivaluedMap<String, String> headers,
                           InputStream entityStream) throws IOException, WebApplicationException {
        MultipartReader reader = new MultipartReader();
        reader.workers = providers;
        MultipartInput multiInput = reader.readFrom(MultipartInput.class,  genericType, anns, mt, headers, entityStream);
        if (clazz.equals(IMultipartBody.class)) {
            return new IMultipartBodyImpl((MultipartInputImpl)multiInput);
        }
        String genericTypeStr = genericType == null ? "null" : genericType.getTypeName();
        throw new InternalServerErrorException("Unexpected multipart type: " + clazz.getName() + " / " + genericTypeStr);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void writeTo(Object entity, Class<?> clazz, Type genericType, Annotation[] anns, MediaType mt, MultivaluedMap<String, Object> headers,
                        OutputStream outputStream) throws IOException, WebApplicationException {

        List<IAttachment> attachments;
        if (entity instanceof IMultipartBody) {
            attachments = ((IMultipartBody)entity).getAllAttachments();
        } else {
            throw new WebApplicationException("Unexpected output type");
        }
        MultipartOutput outputObj = new MultipartOutput();
        for (IAttachment attachment : attachments) {
            Object content;
            boolean java2SecurityEnabled = System.getSecurityManager() != null;
            if (java2SecurityEnabled) {
                try {
                    content = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                       @Override
                       public Object run() throws Exception
                       {        return attachment.getDataHandler().getContent();        }

                    });
                 } catch (PrivilegedActionException pae)
                 {      throw new RuntimeException(pae);    }
            } else {
                content = attachment.getDataHandler().getContent();
            }
            
            OutputPart part = outputObj.addPart(content, content.getClass(), null, attachment.getContentType(),
                                                ((IAttachmentImpl)attachment).getFileName());
            attachment.getHeaders().entrySet().stream().forEach(entry -> {part.getHeaders().put(entry.getKey(), (List)entry.getValue());});
           // part.getHeaders().putAll((Map<? extends String, ? extends List<Object>>) attachment.getHeaders());
        }
        writer.init();
        writer.writeTo(outputObj, clazz, genericType, anns, mt, headers, outputStream);
    }

    class LibertyMultipartWriter extends MultipartWriter {

        void init() {
            workers = providers;
        }
    }
}

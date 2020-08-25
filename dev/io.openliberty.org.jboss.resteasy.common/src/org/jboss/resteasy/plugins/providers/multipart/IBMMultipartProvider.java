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
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;
import com.ibm.websphere.jaxrs20.multipart.IMultipartBody;

import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartOutput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartReader;
import org.jboss.resteasy.plugins.providers.multipart.MultipartWriter;
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
    
    private MessageBodyReader<MultipartInput> reader = new MultipartReader();
    private MessageBodyWriter<MultipartOutput> writer = new MultipartWriter();

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
            && (MULTIPART_CLASSES.contains(type)
                || Collection.class.isAssignableFrom(type)
                /*|| Map.class.isAssignableFrom(type) && type != MultivaluedMap.class
                || AnnotationUtils.getAnnotation(anns, Multipart.class) != null
                || PropertyUtils.isTrue(mc.getContextualProperty(SUPPORT_TYPE_AS_MULTIPART))*/);
    }

    private boolean mediaTypeSupported(MediaType mt) {
        return "multipart".equals(mt.getType()) && MULTIPART_SUBTYPES.contains(mt.getSubtype());
    }

    @Override
    public Object readFrom(Class<Object> clazz, Type genericType, Annotation[] anns, MediaType mt, MultivaluedMap<String, String> headers,
                           InputStream entityStream) throws IOException, WebApplicationException {
        /*if (clazz.equals(IMultipartBody.class)) {
            Class<MultipartInput> specClazz = (Class<MultipartInput>) clazz.asSubclass(MultipartInput.class);
            MessageBodyReader<MultipartInput> reader = new MultipartReader();
            reader.workers = providers;
            MultipartInput multiInput = reader.readFrom(MultipartInput.class,  genericType, anns, mt, headers, entityStream);
            return new IMultipartBodyImpl((MultipartInputImpl)multiInput);
        }*/

        MultipartReader reader = new MultipartReader();
        reader.workers = providers;
        MultipartInput multiInput = reader.readFrom(MultipartInput.class,  genericType, anns, mt, headers, entityStream);
        if (clazz.equals(IMultipartBody.class)) {
            return new IMultipartBodyImpl((MultipartInputImpl)multiInput);
        }
        if (Collection.class.isAssignableFrom(clazz) && genericType instanceof ParameterizedType &&
            ((ParameterizedType)genericType).getActualTypeArguments()[0].getTypeName().equals(IAttachment.class.getName())) {
            List<IAttachment> attachments = new ArrayList<>();
            for (InputPart inputPart : multiInput.getParts()) {
                attachments.add(new IAttachmentImpl(inputPart));
            }
            return attachments;
        }
        String genericTypeStr = genericType == null ? "null" : genericType.getTypeName();
        throw new InternalServerErrorException("Unexpected multipart type: " + clazz.getName() + " / " + genericTypeStr);
    }

    @Override
    public void writeTo(Object entity, Class<?> clazz, Type genericType, Annotation[] anns, MediaType mt, MultivaluedMap<String, Object> headers,
                        OutputStream outputStream) throws IOException, WebApplicationException {

        if (!(entity instanceof IMultipartBody)) {
            throw new WebApplicationException("Unexpected output type");
        }
        MultipartOutput outputObj = new MultipartOutput();
        for (IAttachment attachment : ((IMultipartBody)entity).getAllAttachments()) {
            outputObj.addPart(attachment.getDataHandler().getContent(), attachment.getContentType());
        }
        writer.writeTo(outputObj, clazz, genericType, anns, mt, headers, outputStream);
    }

    

    

}

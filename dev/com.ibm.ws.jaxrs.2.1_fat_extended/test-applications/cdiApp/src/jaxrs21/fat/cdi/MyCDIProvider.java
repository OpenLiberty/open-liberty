/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.cdi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@ApplicationScoped
@Produces({ MediaType.MEDIA_TYPE_WILDCARD })
@Consumes({ MediaType.MEDIA_TYPE_WILDCARD })
@Provider
public class MyCDIProvider implements MessageBodyWriter<MyCar>, MessageBodyReader<MyCar> {

    @Inject
    private CDIObject cdiObject;

    private final Jsonb jsonb;

    public MyCDIProvider() {
        JsonbBuilder builder = JsonbBuilder.newBuilder();
        jsonb = builder.build();
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean readable = true;
        if (cdiObject != null) {
//            javax.enterprise.inject.spi.CDI.current().select(CDIObject.class).get();
            readable = true;
        }
        return readable;
    }

    @Override
    public MyCar readFrom(Class<MyCar> clazz, Type genericType, Annotation[] annotations,
                          MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return jsonb.fromJson(entityStream, genericType);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean writeable = true;
        if (cdiObject != null) {
//            javax.enterprise.inject.spi.CDI.current().select(CDIObject.class).get();
            writeable = true;
        }
        return writeable;
    }

    @Override
    public void writeTo(MyCar car, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        car.setModel(cdiObject.getCar());
        this.jsonb.toJson(car, entityStream);
    }
}

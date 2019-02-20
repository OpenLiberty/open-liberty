/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package remoteApp.basic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@Provider
public class WidgetListEntityProvider implements MessageBodyReader<List<Widget>>, MessageBodyWriter<List<Widget>> {
    private static final Logger LOG = Logger.getLogger(WidgetListEntityProvider.class.getName());

    /* (non-Javadoc)
     * @see javax.ws.rs.ext.MessageBodyWriter#getSize(java.lang.Object, java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType)
     */
    @Override
    public long getSize(List<Widget> widgets, Class<?> clazz, Type type, Annotation[] annos, MediaType mediaType) {
        return 0;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.ext.MessageBodyWriter#isWriteable(java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType)
     */
    @Override
    public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annos, MediaType mediaType) {
        boolean b = List.class.equals(clazz) && Widget.class.equals(type) && mediaType.equals(MediaType.APPLICATION_JSON_TYPE);
        LOG.info("isWriteable: " + b);
        return b;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.ext.MessageBodyWriter#writeTo(java.lang.Object, java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType, javax.ws.rs.core.MultivaluedMap, java.io.OutputStream)
     */
    @Override
    public void writeTo(List<Widget> widgets, Class<?> clazz, Type type, Annotation[] annos, MediaType mediaType, MultivaluedMap<String, Object> headers,
                        OutputStream out) throws IOException, WebApplicationException {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        JsonObjectBuilder objectBuilder;
        for (Widget widget : widgets) {
            objectBuilder = Json.createObjectBuilder();
            objectBuilder.add("name", widget.getName());
            objectBuilder.add("quantity", widget.getQuantity());
            objectBuilder.add("weight", widget.getWeight());
            arrayBuilder.add(objectBuilder.build());
        }
        JsonArray array = arrayBuilder.build();
        Json.createGenerator(out).write(array);
        
        LOG.info("writeTo wrote " + widgets.size() + " widgets to stream: " + array.toString());
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.ext.MessageBodyReader#isReadable(java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType)
     */
    @Override
    public boolean isReadable(Class<?> clazz, Type type, Annotation[] anoos, MediaType mediaType) {
        boolean genericTypeMatches = false;
        if ((type instanceof ParameterizedType) &&
            Widget.class.equals(((ParameterizedType)type).getActualTypeArguments()[0])){
            genericTypeMatches = true;
        }
        boolean b =  List.class.equals(clazz) && genericTypeMatches && mediaType.equals(MediaType.APPLICATION_JSON_TYPE);
        LOG.info("isReadable: " + b);
        return b;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.ext.MessageBodyReader#readFrom(java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType, javax.ws.rs.core.MultivaluedMap, java.io.InputStream)
     */
    @Override
    public List<Widget> readFrom(Class<List<Widget>> clazz, Type type, Annotation[] annos, MediaType mediaType, MultivaluedMap<String, String> headers,
                                 InputStream in) throws IOException, WebApplicationException {
        List<Widget> widgets = new ArrayList<>();
        JsonReader reader = Json.createReader(in);
        JsonArray array = reader.readArray();
        array.forEach(v -> {
            JsonObject obj = (JsonObject) v;
            widgets.add(new Widget(obj.getString("name"),
                                   obj.getInt("quantity"),
                                   obj.getJsonNumber("weight").doubleValue()));
        });
        return widgets;
    }

}

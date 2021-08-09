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
package org.apache.cxf.jaxrs.sse;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.sse.OutboundSseEvent;

import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

@Provider
public class OutboundSseEventBodyWriter implements MessageBodyWriter<OutboundSseEvent> {
    public static final String SERVER_SENT_EVENTS = "text/event-stream";
    public static final MediaType SERVER_SENT_EVENTS_TYPE = MediaType.valueOf(SERVER_SENT_EVENTS);

    private static final byte[] COMMENT = ": ".getBytes(StandardCharsets.UTF_8);
    private static final byte[] EVENT = "event: ".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ID = "id: ".getBytes(StandardCharsets.UTF_8);
    private static final byte[] RETRY = "retry: ".getBytes(StandardCharsets.UTF_8);
    private static final byte[] DATA = "data: ".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NEW_LINE = "\n".getBytes(StandardCharsets.UTF_8);

    private ServerProviderFactory factory;
    private Message message;

    protected OutboundSseEventBodyWriter() {
    }

    public OutboundSseEventBodyWriter(final ServerProviderFactory factory, final Exchange exchange) {
        this.factory = factory;
        this.message = new MessageImpl();
        this.message.setExchange(exchange);
    }


    @Override
    public boolean isWriteable(Class<?> cls, Type type, Annotation[] anns, MediaType mt) {
        return OutboundSseEvent.class.isAssignableFrom(cls) || SERVER_SENT_EVENTS_TYPE.isCompatible(mt);
    }

    @Override
    public void writeTo(OutboundSseEvent p, Class<?> cls, Type t, Annotation[] anns,
            MediaType mt, MultivaluedMap<String, Object> headers, OutputStream os)
                throws IOException, WebApplicationException {

        if (p.getName() != null) {
            os.write(EVENT);
            os.write(p.getName().getBytes(StandardCharsets.UTF_8));
            os.write(NEW_LINE);
        }

        if (p.getId() != null) {
            os.write(ID);
            os.write(p.getId().getBytes(StandardCharsets.UTF_8));
            os.write(NEW_LINE);
        }

        if (p.getComment() != null) {
            os.write(COMMENT);
            os.write(p.getComment().getBytes(StandardCharsets.UTF_8));
            os.write(NEW_LINE);
        }

        if (p.getReconnectDelay() > 0) {
            os.write(RETRY);
            os.write(Long.toString(p.getReconnectDelay()).getBytes(StandardCharsets.UTF_8));
            os.write(NEW_LINE);
        }

        if (p.getData() != null) {
            Class<?> payloadClass = p.getType();
            Type payloadType = p.getGenericType();
            if (payloadType == null) {
                payloadType = payloadClass;
            }

            if (payloadType == null && payloadClass == null) {
                payloadType = Object.class;
                payloadClass = Object.class;
            }

            os.write(DATA);
            writePayloadTo(payloadClass, payloadType, anns, p.getMediaType(), headers, p.getData(), os);
            os.write(NEW_LINE);
        }
    }

    @SuppressWarnings("unchecked")
    private<T> void writePayloadTo(Class<T> cls, Type type, Annotation[] anns, MediaType mt,
            MultivaluedMap<String, Object> headers, Object data, OutputStream os)
                throws IOException, WebApplicationException {

        MessageBodyWriter<T> writer = null;
        if (message != null && factory != null) {
            writer = factory.createMessageBodyWriter(cls, type, anns, mt, message);
        }

        if (writer == null) {
            throw new NoSuitableMessageBodyWriterException("No suitable message body writer for class: " + cls.getName());
        }

        writer.writeTo((T)data, cls, type, anns, mt, headers, os);
    }

    @Override
    public long getSize(OutboundSseEvent t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }
}

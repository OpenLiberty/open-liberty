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
package org.apache.cxf.jaxrs.sse.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.OptionalLong;
import java.util.logging.Logger;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.sse.InboundSseEvent;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.client.ClientProviderFactory;
import org.apache.cxf.message.Message;

public final class InboundSseEventImpl implements InboundSseEvent {
    private final String id;
    private final String name;
    private final String comment;
    private final long reconnectDelay;
    private final boolean reconnectDelaySet;
    private final String data;
    private final ClientProviderFactory factory;
    private final Message message;
    
    static class Builder {
        private static final Logger LOG = LogUtils.getL7dLogger(Builder.class);

        private String name; /* the default event type would be "message" */
        private String id;
        private String comment;
        private OptionalLong reconnectDelay = OptionalLong.empty();
        private String data;

        Builder() {
        }

        Builder id(String i) {
            this.id = i;
            return this;
        }
        
        Builder name(String n) {
            this.name = n;
            return this;
        }

        Builder comment(String cmt) {
            this.comment = cmt;
            return this;
        }

        Builder reconnectDelay(String rd) {
            try {
                this.reconnectDelay = OptionalLong.of(Long.parseLong(rd));
            } catch (final NumberFormatException ex) {
                LOG.warning("Unable to parse reconnectDelay, long number expected: " + ex.getMessage());
            }
            
            return this;
        }
        
        Builder appendData(String d) {
            this.data = this.data == null ? d : this.data + '\n' + d;
            return this;
        }

        InboundSseEvent build(ClientProviderFactory factory, Message message) {
            return new InboundSseEventImpl(id, name, comment, reconnectDelay.orElse(RECONNECT_NOT_SET), 
                reconnectDelay.isPresent(), data, factory, message);
        }
    }
    
    //CHECKSTYLE:OFF
    InboundSseEventImpl(String id, String name, String comment, long reconnectDelay, boolean reconnectDelaySet,   
            String data, ClientProviderFactory factory, Message message) {
        //CHECKSTYLE:ON
        this.id = id;
        this.name = name;
        this.comment = comment;
        this.reconnectDelay = reconnectDelay;
        this.reconnectDelaySet = reconnectDelaySet;
        this.data = data;
        this.factory = factory;
        this.message = message;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public long getReconnectDelay() {
        return reconnectDelay;
    }

    @Override
    public boolean isReconnectDelaySet() {
        return reconnectDelaySet;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String readData() {
        return data;
    }

    @Override
    public <T> T readData(Class<T> type) {
        return read(type, type, MediaType.TEXT_PLAIN_TYPE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readData(GenericType<T> type) {
        return read((Class<T>)type.getRawType(), type.getType(), MediaType.TEXT_PLAIN_TYPE);
    }

    @Override
    public <T> T readData(Class<T> messageType, MediaType mediaType) {
        return read(messageType, messageType, mediaType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readData(GenericType<T> type, MediaType mediaType) {
        return read((Class<T>)type.getRawType(), type.getType(), mediaType);
    }
    
    private <T> T read(Class<T> messageType, Type type, MediaType mediaType) {
        if (data == null) {
            return null;
        }

        final Annotation[] annotations = new Annotation[0];
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>(0);
        
        final MessageBodyReader<T> reader = factory.createMessageBodyReader(messageType, type, 
            annotations, mediaType, message);
            
        if (reader == null) {
            throw new RuntimeException("No suitable message body reader for class: " + messageType.getName());
        }

        try (ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
            return reader.readFrom(messageType, type, annotations, mediaType, headers, is);
        } catch (final IOException ex) {
            throw new RuntimeException("Unable to read data of type " + messageType.getName(), ex);
        }
    }
}

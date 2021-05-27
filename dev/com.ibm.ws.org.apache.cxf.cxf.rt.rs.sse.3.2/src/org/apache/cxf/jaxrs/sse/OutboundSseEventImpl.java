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

import java.lang.reflect.Type;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;

public final class OutboundSseEventImpl implements OutboundSseEvent {
    private final String id;
    private final String name;
    private final String comment;
    private final long reconnectDelay;
    private final Class<?> type;
    private final Type genericType;
    private final MediaType mediaType;
    private final Object data;

    public static class BuilderImpl implements Builder {
        private String id;
        private String name;
        private String comment;
        private long reconnectDelay = -1;
        private Class<?> type;
        private Type genericType;
        private MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;
        private Object data;

        @Override
        public Builder id(String newId) {
            this.id = newId;
            return this;
        }

        @Override
        public Builder name(String newName) {
            this.name = newName;
            return this;
        }

        @Override
        public Builder reconnectDelay(long milliseconds) {
            this.reconnectDelay = milliseconds;
            return this;
        }

        @Override
        public Builder mediaType(MediaType newMediaType) {
            this.mediaType = newMediaType;
            return this;
        }

        @Override
        public Builder comment(String newComment) {
            this.comment = newComment;
            return this;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Builder data(Class newType, Object newData) {
            if (newType == null || newData == null) {
                throw new IllegalArgumentException("Parameters 'type' and 'data' must not be null.");
            }
            this.type = newType;
            this.data = newData;
            return this;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Builder data(GenericType newType, Object newData) {
            if (newType == null || newData == null) {
                throw new IllegalArgumentException("Parameters 'type' and 'data' must not be null.");
            }
            this.genericType = newType.getType();
            this.data = newData;
            return this;
        }

        @Override
        public Builder data(Object newData) {
            if (newData == null) {
                throw new IllegalArgumentException("Parameter 'data' must not be null.");
            }
            this.type = newData.getClass();
            this.data = newData;
            return this;
        }

        @Override
        public OutboundSseEvent build() {
            return new OutboundSseEventImpl(
                id,
                name,
                comment,
                reconnectDelay,
                type,
                genericType,
                mediaType,
                data
            );
        }

    }
    //CHECKSTYLE:OFF
    private OutboundSseEventImpl(String id, String name, String comment, long reconnectDelay,
            Class<?> type, Type genericType, MediaType mediaType, Object data) {
        this.id = id;
        this.name = name;
        this.comment = comment;
        this.reconnectDelay = reconnectDelay;
        this.type = type;
        this.genericType = genericType;
        this.mediaType = mediaType;
        this.data = data;
    }
    //CHECKSTYLE:ON

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
        return reconnectDelay != -1;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public Type getGenericType() {
        return genericType;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public Object getData() {
        return data;
    }
}

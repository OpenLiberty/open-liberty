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
package com.ibm.ws.microprofile.openapi.impl.core.jackson;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.models.media.Schema;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;

public class SchemaSerializer extends JsonSerializer<Schema> implements ResolvableSerializer {

    private final JsonSerializer<Object> defaultSerializer;

    public SchemaSerializer(JsonSerializer<Object> serializer) {
        defaultSerializer = serializer;
    }

    @Override
    public void resolve(SerializerProvider serializerProvider) throws JsonMappingException {
        if (defaultSerializer instanceof ResolvableSerializer) {
            ((ResolvableSerializer) defaultSerializer).resolve(serializerProvider);
        }
    }

    @Override
    public void serialize(
                          Schema value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {

        // handle ref schema serialization skipping all other props
        if (StringUtils.isBlank(value.getRef())) {
            defaultSerializer.serialize(value, jgen, provider);
        } else {
            jgen.writeStartObject();
            jgen.writeStringField("$ref", value.getRef());
            jgen.writeEndObject();
        }
    }
}
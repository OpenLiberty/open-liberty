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

package com.ibm.ws.microprofile.openapi.impl.core.util;

import java.io.IOException;

import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;

public class ModelDeserializer extends JsonDeserializer<Schema> {
    @Override
    public Schema deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        JsonNode allOf = node.get("allOf");
        JsonNode anyOf = node.get("anyOf");
        JsonNode oneOf = node.get("oneOf");

        Schema schema = null;

        if (allOf != null || anyOf != null || oneOf != null) {

            Schema composedSchema = Json.mapper().convertValue(node, Schema.class);
            return composedSchema;

        } else {

            JsonNode type = node.get("type");
            String format = node.get("format") == null ? "" : node.get("format").textValue();
            if (type != null && "array".equals(((TextNode) type).textValue())) {
                schema = Json.mapper().convertValue(node, Schema.class);
            } else if (type != null) {
                if (type.textValue().equals("object")) {
                    JsonNode additionalProperties = node.get("additionalProperties");
                    if (additionalProperties != null) {
                        Schema innerSchema = Json.mapper().convertValue(additionalProperties, Schema.class);
                        Schema ms = Json.mapper().convertValue(node, Schema.class);
                        ms.setAdditionalProperties(innerSchema);
                        schema = ms;
                    } else {
                        schema = Json.mapper().convertValue(node, Schema.class);
                    }
                }
            } else if (node.get("$ref") != null) {
                schema = new SchemaImpl().ref(node.get("$ref").asText());
            } else { // assume object
                schema = Json.mapper().convertValue(node, Schema.class);
                schema.type(SchemaType.OBJECT);
            }
        }
        return schema;
    }
}

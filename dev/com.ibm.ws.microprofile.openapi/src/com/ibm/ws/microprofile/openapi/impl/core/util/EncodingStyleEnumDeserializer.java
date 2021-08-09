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
import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.models.media.Encoding;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class EncodingStyleEnumDeserializer extends JsonDeserializer<Encoding.Style> {
    @Override
    public Encoding.Style deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (node != null) {
            String value = node.asText();
            return getStyleEnum(value);
        }
        return null;
    }

    private Encoding.Style getStyleEnum(String value) {
        return Arrays.stream(
                             Encoding.Style.values()).filter(i -> i.toString().equals(value)).findFirst().orElseThrow(() -> new RuntimeException(String.format("Can not deserialize value of type Encoding.StyleEnum from String \"%s\": value not one of declared Enum instance names: %s",
                                                                                                                                                               value,
                                                                                                                                                               Arrays.stream(Encoding.Style.values()).map(v -> v.toString()).collect(Collectors.joining(", ",
                                                                                                                                                                                                                                                        "[",
                                                                                                                                                                                                                                                        "]")))));
    }
}

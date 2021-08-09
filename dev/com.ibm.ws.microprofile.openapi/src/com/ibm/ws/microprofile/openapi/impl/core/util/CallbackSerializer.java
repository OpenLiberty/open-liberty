/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 *
 */
public class CallbackSerializer extends JsonSerializer<Callback> {

    /** {@inheritDoc} */
    @Override
    public void serialize(Callback value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        if (StringUtils.isBlank(value.getRef())) {
            jgen.writeStartObject();
            for (Entry<String, PathItem> entry : value.entrySet()) {
                jgen.writeObjectField(entry.getKey(), entry.getValue());
            }
            jgen.writeEndObject();

        } else {
            jgen.writeStartObject();
            jgen.writeStringField("$ref", value.getRef());
            jgen.writeEndObject();
        }
    }

}

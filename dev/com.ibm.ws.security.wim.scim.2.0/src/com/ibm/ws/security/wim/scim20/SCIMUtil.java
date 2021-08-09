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

package com.ibm.ws.security.wim.scim20;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SCIMUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Serialize the object instance to a JSON String that contains the
     * unmodified representation of the object.
     *
     * <p>
     * WARNING! The value returned from this method may contain clear text
     * passwords and therefore should NOT be used in trace or other diagnostic
     * output. Instead use {@link #serializeForTrace(Object)} for trace or other
     * diagnostic output.
     *
     * @param obj
     *            The object to serialize.
     * @return The JSON String representation of the object, unmodified.
     * @throws JsonProcessingException
     */
    public static String serialize(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * Serialize the object instance to a JSON String that is suitable for
     * trace. Some values may be obfuscated.
     *
     * @param obj
     *            The object to serialize.
     * @return The JSON String representation of the object, suitable for trace.
     * @throws JsonProcessingException
     */
    public static String serializeForTrace(Object obj) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().withView(Object.class).writeValueAsString(obj);
    }

    /**
     * Deserialize the String JSON representation into a object instance of type
     * 'type'.
     *
     * @param obj
     *            The JSON String to deserialize.
     * @param type
     *            The type to deserialize the JSON String to.
     * @return The deserialized object.
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public static <T> T deserialize(String json, Class<T> type) throws JsonParseException, JsonMappingException, IOException {
        return objectMapper.readValue(json, type);
    }
}

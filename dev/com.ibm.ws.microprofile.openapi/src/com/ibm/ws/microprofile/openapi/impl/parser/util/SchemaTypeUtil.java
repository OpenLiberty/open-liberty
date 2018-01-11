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

package com.ibm.ws.microprofile.openapi.impl.parser.util;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;

public class SchemaTypeUtil {

    private static final String TYPE = "type";
    private static final String FORMAT = "format";

    public static final String INTEGER_TYPE = "integer";
    public static final String NUMBER_TYPE = "number";
    public static final String STRING_TYPE = "string";
    public static final String BOOLEAN_TYPE = "boolean";
    public static final String OBJECT_TYPE = "object";

    public static final String INTEGER32_FORMAT = "int32";
    public static final String INTEGER64_FORMAT = "int64";
    public static final String FLOAT_FORMAT = "float";
    public static final String DOUBLE_FORMAT = "double";
    public static final String BYTE_FORMAT = "byte";
    public static final String BINARY_FORMAT = "binary";
    public static final String DATE_FORMAT = "date";
    public static final String DATE_TIME_FORMAT = "date-time";
    public static final String PASSWORD_FORMAT = "password";
    public static final String EMAIL_FORMAT = "email";
    public static final String UUID_FORMAT = "uuid";

    public static Schema createSchemaByType(ObjectNode node) {
        if (node == null) {
            return new SchemaImpl();
        }
        final String type = getNodeValue(node, TYPE);
        if (StringUtils.isBlank(type)) {
            return new SchemaImpl();
        }
        final String format = getNodeValue(node, FORMAT);

        return createSchema(type, format);
    }

    public static Schema createSchema(String type, String format) {

        if (INTEGER_TYPE.equals(type)) {
            if (INTEGER64_FORMAT.equals(format)) {
                return new SchemaImpl().type(SchemaType.INTEGER).format(INTEGER64_FORMAT);
            } else {
                return new SchemaImpl().type(SchemaType.INTEGER);
            }
        } else if (NUMBER_TYPE.equals(type)) {
            if (FLOAT_FORMAT.equals(format)) {
                return new SchemaImpl().type(SchemaType.NUMBER).format(FLOAT_FORMAT);
            } else if (DOUBLE_FORMAT.equals(format)) {
                return new SchemaImpl().type(SchemaType.NUMBER).format(DOUBLE_FORMAT);
            } else {
                return new SchemaImpl().type(SchemaType.NUMBER);
            }
        } else if (BOOLEAN_TYPE.equals(type)) {
            return new SchemaImpl().type(SchemaType.BOOLEAN);
        } else if (STRING_TYPE.equals(type)) {
            return new SchemaImpl().type(SchemaType.STRING).format(format);

        } else if (OBJECT_TYPE.equals(type)) {
            return new SchemaImpl().type(SchemaType.OBJECT);
        } else {
            return new SchemaImpl();
        }
    }

    private static String getNodeValue(ObjectNode node, String field) {
        final JsonNode jsonNode = node.get(field);
        if (jsonNode == null) {
            return null;
        }
        return jsonNode.textValue();
    }

}

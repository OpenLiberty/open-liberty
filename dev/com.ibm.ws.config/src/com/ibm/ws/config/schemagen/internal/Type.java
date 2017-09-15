/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.schemagen.internal;

import static com.ibm.ws.config.schemagen.internal.SchemaWriter.XSD;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.osgi.service.metatype.AttributeDefinition;

import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
public enum Type {
    STRING(AttributeDefinition.STRING, "xsd:string", "xsd:string", MinMaxWriter.STRING) {
        @Override
        public void writeGlobalType(XMLStreamWriter writer) throws XMLStreamException {}
    },
    BOOLEAN(AttributeDefinition.BOOLEAN, "booleanType", "xsd:boolean", null) {
        @Override
        public void writeType(XMLStreamWriter writer, String min, String max, DocumentationWriter documentationWriter) throws XMLStreamException {
            writeType(writer, documentationWriter);
        }
    },
    BYTE(AttributeDefinition.BYTE, "byteType", "xsd:byte", MinMaxWriter.NUMBER),
    CHAR(AttributeDefinition.CHARACTER, "charType", "char", null) {

        @Override
        public void writeGlobalType(XMLStreamWriter writer) throws XMLStreamException {
            writer.writeStartElement(XSD, "simpleType");
            writer.writeAttribute("name", "char");

            writer.writeStartElement(XSD, "restriction");
            writer.writeAttribute("base", "xsd:string");

            writer.writeEmptyElement(XSD, "length");
            writer.writeAttribute("value", "1");

            writer.writeEndElement();

            writer.writeEndElement();
            super.writeGlobalType(writer);
        }
    },
    DOUBLE(AttributeDefinition.DOUBLE, "doubleType", "xsd:double", MinMaxWriter.NUMBER),
    FLOAT(AttributeDefinition.FLOAT, "floatType", "xsd:float", MinMaxWriter.NUMBER),
    INTEGER(AttributeDefinition.INTEGER, "intType", "xsd:int", MinMaxWriter.NUMBER),
    LONG(AttributeDefinition.LONG, "longType", "xsd:long", MinMaxWriter.NUMBER),
    SHORT(AttributeDefinition.SHORT, "shortType", "xsd:short", MinMaxWriter.NUMBER),
    PASSWORD(AttributeDefinition.PASSWORD, "password", "xsd:string", MinMaxWriter.STRING) {
        // Don't write this here, we write it as a part of the PASSWORD_IBM type. We are treating these as identical
        @Override
        public void writeGlobalType(XMLStreamWriter writer) throws XMLStreamException {}
    },
    PASSWORD_IBM(MetaTypeFactory.PASSWORD_TYPE, "password", "xsd:string", MinMaxWriter.STRING),
    PASSWORD_HASH(MetaTypeFactory.HASHED_PASSWORD_TYPE, "passwordHash", "xsd:string", MinMaxWriter.STRING),
    DURATION(MetaTypeFactory.DURATION_TYPE, "duration", "xsd:string", null),
    DURATION_HOUR(MetaTypeFactory.DURATION_H_TYPE, "hourDuration", "xsd:string", null),
    DURATION_MINUTE(MetaTypeFactory.DURATION_M_TYPE, "minuteDuration", "xsd:string", null),
    DURATION_SECOND(MetaTypeFactory.DURATION_S_TYPE, "secondDuration", "xsd:string", null),
    ON_ERROR(MetaTypeFactory.ON_ERROR_TYPE, "xsd:string", "xsd:string", null) {
        // Don't write this here. It is just an xsd:string so we don't need a global type for this.
        @Override
        public void writeGlobalType(XMLStreamWriter writer) throws XMLStreamException {}
    },
    LOCATION(MetaTypeFactory.LOCATION_TYPE, "location", "xsd:string", null),
    LOCATION_DIR(MetaTypeFactory.LOCATION_DIR_TYPE, "dirLocation", "xsd:string", null),
    LOCATION_FILE(MetaTypeFactory.LOCATION_FILE_TYPE, "fileLocation", "xsd:string", null),
    LOCATION_URL(MetaTypeFactory.LOCATION_URL_TYPE, "urlLocation", "xsd:string", null),
    TOKEN(MetaTypeFactory.TOKEN_TYPE, "tokenType", "xsd:token", MinMaxWriter.STRING),
    PID(MetaTypeFactory.PID_TYPE, "pidType", "singlePidType", null) {
        @Override
        public void writeGlobalType(XMLStreamWriter writer) throws XMLStreamException {
            writePatternType(writer, getSchemaBaseType(), "[^, ]+");
            super.writeGlobalType(writer);
        }
    },
    PID_LIST(-1, "pidListType", "multiplePidType", null) {
        @Override
        public void writeGlobalType(XMLStreamWriter writer) throws XMLStreamException {
            writePatternType(writer, getSchemaBaseType(), "[^, ]+(\\s*,\\s*[^, ]+)*");
            super.writeGlobalType(writer);
        }
    },
    VARIABLE(-2, "variableType", "xsd:string", null) {
        @Override
        public void writeGlobalType(XMLStreamWriter writer) throws XMLStreamException {
            writePatternType(writer, getGlobalSchemaType(), ".*$\\{[^\\s\\}]*\\}.*");
        }
    };

    private enum MinMaxWriter {
        NUMBER {
            @Override
            public void writeLimits(XMLStreamWriter writer, String min, String max) throws XMLStreamException {
                if (min != null) {
                    writer.writeStartElement(SchemaWriter.XSD, "minInclusive");
                    writer.writeAttribute("value", min);
                    writer.writeEndElement(); // minInclusive
                }
                if (max != null) {
                    writer.writeStartElement(SchemaWriter.XSD, "maxInclusive");
                    writer.writeAttribute("value", max);
                    writer.writeEndElement(); // maxInclusive
                }
            }
        },
        STRING {
            @Override
            public void writeLimits(XMLStreamWriter writer, String min, String max) throws XMLStreamException {
                if (min != null) {
                    writer.writeStartElement(XSD, "minLength");
                    writer.writeAttribute("value", min);
                    writer.writeEndElement(); // minLength
                }
                if (max != null) {
                    writer.writeStartElement(XSD, "maxLength");
                    writer.writeAttribute("value", max);
                    writer.writeEndElement(); // maxLength
                }
            }
        };

        public void writeType(XMLStreamWriter writer, String baseType, String min, String max) throws XMLStreamException {
            writer.writeStartElement(XSD, "simpleType");
            writer.writeStartElement(SchemaWriter.XSD, "union");
            writer.writeAttribute("memberTypes", "variableType");
            writer.writeStartElement(SchemaWriter.XSD, "simpleType");

            writer.writeStartElement(SchemaWriter.XSD, "restriction");

            writer.writeAttribute("base", baseType);

            writeLimits(writer, min, max);

            writer.writeEndElement(); // restriction
            writer.writeEndElement(); // simpleType
            writer.writeEndElement(); // union
            writer.writeEndElement(); // simpleType
        }

        public abstract void writeLimits(XMLStreamWriter writer, String min, String max) throws XMLStreamException;
    }

    private int typeId;
    private String globalSchemaType;
    private String schemaBaseType;
    private MinMaxWriter minMaxWriter;

    private Type(int id, String globalType, String schemaType, MinMaxWriter mmw) {
        typeId = id;
        globalSchemaType = globalType;
        schemaBaseType = schemaType;
        minMaxWriter = mmw;
    }

    public static Type fromId(int id) {
        Type[] types = values();
        for (int i = 0; i < types.length; i++) {
            if (types[i].getTypeId() == id) {
                return types[i];
            }
        }

        return null;
    }

    public static Type fromId(String globalType) {
        Type[] types = values();
        for (int i = 0; i < types.length; i++) {
            if (types[i].getGlobalSchemaType().equals(globalType)) {
                return types[i];
            }
        }

        return null;
    }

    public int getTypeId() {
        return typeId;
    }

    public String getGlobalSchemaType() {
        return globalSchemaType;
    }

    public String getSchemaBaseType() {
        return schemaBaseType;
    }

    /**
     * @param writer
     * @throws XMLStreamException
     */
    public void writeGlobalType(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(SchemaWriter.XSD, "simpleType");
        writer.writeAttribute("name", getGlobalSchemaType());

        writer.writeEmptyElement(SchemaWriter.XSD, "union");
        writer.writeAttribute("memberTypes", getSchemaBaseType() + " variableType");

        writer.writeEndElement();
    }

    public void writeType(XMLStreamWriter writer, DocumentationWriter documentationWriter) throws XMLStreamException {
        writer.writeAttribute("type", getGlobalSchemaType());
        documentationWriter.writeDoc();
    }

    @FFDCIgnore(NumberFormatException.class)
    public void writeType(XMLStreamWriter writer, String min, String max, DocumentationWriter documentationWriter) throws XMLStreamException {
        if (min != null) {
            try {
                Integer.parseInt(min);
            } catch (NumberFormatException e) {
                min = null;
            }
        }

        if (max != null) {
            try {
                Integer.parseInt(max);
            } catch (NumberFormatException e) {
                max = null;
            }
        }

        if ((min == null && max == null) || minMaxWriter == null) {
            writeType(writer, documentationWriter);
        } else {
            documentationWriter.writeDoc();
            minMaxWriter.writeType(writer, getSchemaBaseType(), min, max);
        }
    }

    private static void writePatternType(XMLStreamWriter writer, String name, String pattern) throws XMLStreamException {
        writer.writeStartElement(XSD, "simpleType");
        writer.writeAttribute("name", name);

        writer.writeStartElement(XSD, "restriction");
        writer.writeAttribute("base", "xsd:string");

        writer.writeEmptyElement(XSD, "pattern");
        writer.writeAttribute("value", pattern);

        writer.writeEndElement();

        writer.writeEndElement();
    }
}
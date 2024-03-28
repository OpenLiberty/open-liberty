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

package org.apache.cxf.common.jaxb;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import org.w3c.dom.Document;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;

/**
 *
 */
public class SchemaCollectionContextProxy implements JAXBContextProxy {
    private static final Map<Class<?>, QName> TYPE_MAP = new HashMap<>();

    final JAXBContext context;
    final SchemaCollection schemas;
    final String defaultNamespace;


    static {
        defaultRegister(BigDecimal.class, Constants.XSD_DECIMAL);
        defaultRegister(BigInteger.class, Constants.XSD_INTEGER);
        defaultRegister(Boolean.class, Constants.XSD_BOOLEAN);
        defaultRegister(Calendar.class, Constants.XSD_DATETIME);
        defaultRegister(Date.class, Constants.XSD_DATETIME);
        defaultRegister(Float.class, Constants.XSD_FLOAT);
        defaultRegister(Double.class, Constants.XSD_DOUBLE);
        defaultRegister(Integer.class, Constants.XSD_INT);
        defaultRegister(Long.class, Constants.XSD_LONG);
        defaultRegister(Object.class, Constants.XSD_ANYTYPE);
        defaultRegister(Byte.class, Constants.XSD_BYTE);
        defaultRegister(Short.class, Constants.XSD_SHORT);
        defaultRegister(Source.class, Constants.XSD_ANYTYPE);
        defaultRegister(String.class, Constants.XSD_STRING);
        defaultRegister(Time.class, Constants.XSD_TIME);
        defaultRegister(Timestamp.class, Constants.XSD_DATETIME);
        defaultRegister(URI.class, Constants.XSD_ANYURI);
        defaultRegister(XMLStreamReader.class, Constants.XSD_ANYTYPE);

        defaultRegister(boolean.class, Constants.XSD_BOOLEAN);
        defaultRegister(Date.class, Constants.XSD_DATETIME);
        defaultRegister(Float.class, Constants.XSD_FLOAT);
        defaultRegister(Double.class, Constants.XSD_DOUBLE);
        defaultRegister(Integer.class, Constants.XSD_INT);
        defaultRegister(Long.class, Constants.XSD_LONG);
        defaultRegister(Object.class, Constants.XSD_ANYTYPE);
        defaultRegister(Byte.class, Constants.XSD_BYTE);
        defaultRegister(Short.class, Constants.XSD_SHORT);
        defaultRegister(Source.class, Constants.XSD_ANYTYPE);
        defaultRegister(String.class, Constants.XSD_STRING);
        defaultRegister(Time.class, Constants.XSD_TIME);
        defaultRegister(Timestamp.class, Constants.XSD_DATETIME);
        defaultRegister(URI.class, Constants.XSD_ANYURI);
        defaultRegister(XMLStreamReader.class, Constants.XSD_ANYTYPE);

        defaultRegister(boolean.class, Constants.XSD_BOOLEAN);
        defaultRegister(byte[].class, Constants.XSD_BASE64);
        defaultRegister(double.class, Constants.XSD_DOUBLE);
        defaultRegister(float.class, Constants.XSD_FLOAT);
        defaultRegister(int.class, Constants.XSD_INT);
        defaultRegister(short.class, Constants.XSD_SHORT);
        defaultRegister(byte.class, Constants.XSD_BYTE);
        defaultRegister(long.class, Constants.XSD_LONG);

        defaultRegister(java.sql.Date.class, Constants.XSD_DATETIME);
        defaultRegister(java.sql.Date.class, Constants.XSD_DATE);
        defaultRegister(Number.class, Constants.XSD_DECIMAL);

        defaultRegister(DataSource.class, Constants.XSD_BASE64);
        defaultRegister(DataHandler.class, Constants.XSD_BASE64);
        defaultRegister(Document.class, Constants.XSD_ANYTYPE);
    }

    public SchemaCollectionContextProxy(JAXBContext ctx, SchemaCollection c, String defaultNs) {
        schemas = c;
        context = ctx;
        defaultNamespace = defaultNs;
    }

    private static void defaultRegister(Class<?> cls, QName name) {
        TYPE_MAP.put(cls, name);
    }

    public Object getBeanInfo(Class<?> cls) {
        Class<?> origCls = cls;
        String postfix = "";
        while (cls.isArray()) {
            cls = cls.getComponentType();
            postfix = "Array";
        }
        XmlRootElement xre = cls.getAnnotation(XmlRootElement.class);
        String name = xre == null ? "##default" : xre.name();
        String namespace = xre == null ? "##default" : xre.namespace();
        if ("##default".equals(name)) {
            name = java.beans.Introspector.decapitalize(cls.getSimpleName());
        }
        if ("##default".equals(namespace) && cls.getPackage() != null) {
            XmlSchema sc = cls.getPackage().getAnnotation(XmlSchema.class);
            if (sc != null) {
                namespace = sc.namespace();
            }
        }
        if ("##default".equals(namespace) || StringUtils.isEmpty(namespace)) {
            namespace = JAXBUtils.getPackageNamespace(cls);
            if (namespace == null) {
                namespace = defaultNamespace;
            }
        }
        final QName qname = new QName(namespace, name + postfix);
        final XmlSchemaElement el = schemas.getElementByQName(qname);
        XmlSchemaType type = null;
        if (el != null) {
            type = el.getSchemaType();
        }
        if (type == null) {
            type = schemas.getTypeByQName(getTypeQName(origCls, namespace));
            if (type == null) {
                type = schemas.getTypeByQName(qname);
            }
        }
        if (type == null) {
            type = mapToSchemaType(origCls, namespace);
            /*
            if (type == null) {
                type = mapToSchemaType(cls, namespace);
            }
            */
        }
        if (el == null && type == null) {
            return null;
        }
        final QName typeName = type == null ? null : type.getQName();

        return new JAXBBeanInfo() {
            public boolean isElement() {
                return el == null ? false : true;
            }
            public Collection<QName> getTypeNames() {
                return Collections.singletonList(typeName);
            }
            public String getElementNamespaceURI(Object object) {
                return qname.getNamespaceURI();
            }
            public String getElementLocalName(Object object) {
                return qname.getLocalPart();
            }
        };
    }

    private QName getTypeQName(Class<?> cls, String namespace) {
        QName qn = TYPE_MAP.get(cls);
        if (qn != null) {
            return qn;
        }
        XmlType xtype = cls.getAnnotation(XmlType.class);
        String tn = xtype == null ? "##default" : xtype.name();
        String tns = xtype == null ? "##default" : xtype.namespace();
        if ("##default".equals(tn)) {
            tn = java.beans.Introspector.decapitalize(cls.getSimpleName());
        }
        if ("##default".equals(tns) || StringUtils.isEmpty(tns)) {
            tns = JAXBUtils.getPackageNamespace(cls);
        }
        if ("##default".equals(tns) || StringUtils.isEmpty(tns)) {
            tns = namespace;
        }
        return new QName(tns, tn);
    }
    private XmlSchemaType mapToSchemaType(Class<?> cls, String namespace) {
        QName qn = getTypeQName(cls, namespace);
        XmlSchemaType type = schemas.getTypeByQName(qn);
        if (type == null && cls.isArray()) {
            Class<?> compType = cls.getComponentType();
            int count = 1;
            while (compType.isArray()) {
                compType = compType.getComponentType();
                count++;
            }
            QName aqn = getTypeQName(compType, namespace);
            while (count > 0) {
                aqn = new QName(aqn.getNamespaceURI(), aqn.getLocalPart() + "Array");
                count--;
            }
            type = schemas.getTypeByQName(aqn);
            if (type == null) {
                type = schemas.getTypeByQName(new QName("http://jaxb.dev.java.net/array", aqn.getLocalPart()));
            }
        }
        /*
        if (type == null) {
            System.out.println("HELP: " + cls.getName());
        }
        */
        return type;
    }

}

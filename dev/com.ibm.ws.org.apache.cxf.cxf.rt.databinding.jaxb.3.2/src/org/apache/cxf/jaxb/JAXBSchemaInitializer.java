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

package org.apache.cxf.jaxb;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.jaxb.JAXBBeanInfo;
import org.apache.cxf.common.jaxb.JAXBContextProxy;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.ServiceModelVisitor;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeList;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.utils.NamespaceMap;

/**
 * Walks the service model and sets up the element/type names.
 */
class JAXBSchemaInitializer extends ServiceModelVisitor {
    private static final Logger LOG = LogUtils.getLogger(JAXBSchemaInitializer.class);

    private SchemaCollection schemas;
    private JAXBContextProxy context;
    private final boolean qualifiedSchemas;

    JAXBSchemaInitializer(ServiceInfo serviceInfo,
                          SchemaCollection col,
                          JAXBContext context,
                          boolean q,
                          String defaultNs) {
        super(serviceInfo);
        schemas = col;
        this.context = JAXBUtils.createJAXBContextProxy(context, serviceInfo.getXmlSchemaCollection(), defaultNs);
        this.qualifiedSchemas = q;
    }

    static Class<?> getArrayComponentType(Type cls) {
        if (cls instanceof Class) {
            if (((Class<?>)cls).isArray()) {
                return ((Class<?>)cls).getComponentType();
            }
            return (Class<?>)cls;
        } else if (cls instanceof ParameterizedType) {
            for (Type t2 : ((ParameterizedType)cls).getActualTypeArguments()) {
                return getArrayComponentType(t2);
            }
        } else if (cls instanceof GenericArrayType) {
            GenericArrayType gt = (GenericArrayType)cls;
            Class<?> ct = (Class<?>) gt.getGenericComponentType();
            return Array.newInstance(ct, 0).getClass();
        }
        return null;
    }

    public JAXBBeanInfo getBeanInfo(Type cls) {
        if (cls instanceof Class) {
            if (((Class<?>)cls).isArray()) {
                return getBeanInfo(((Class<?>)cls).getComponentType());
            }
            return getBeanInfo((Class<?>)cls);
        } else if (cls instanceof ParameterizedType) {
            for (Type t2 : ((ParameterizedType)cls).getActualTypeArguments()) {
                return getBeanInfo(t2);
            }
        } else if (cls instanceof GenericArrayType) {
            GenericArrayType gt = (GenericArrayType)cls;
            Class<?> ct = (Class<?>) gt.getGenericComponentType();
            ct = Array.newInstance(ct, 0).getClass();

            return getBeanInfo(ct);
        }

        return null;
    }

    public JAXBBeanInfo getBeanInfo(Class<?> cls) {
        return getBeanInfo(context, cls);
    }

    public static JAXBBeanInfo getBeanInfo(JAXBContextProxy context, Class<?> cls) {
        return JAXBUtils.getBeanInfo(context, cls);
    }

    @Override
    public void begin(MessagePartInfo part) {
        // Check to see if the WSDL information has been filled in for us.
        if (part.getTypeQName() != null || part.getElementQName() != null) {
            checkForExistence(part);
            return;
        }

        Class<?> clazz = part.getTypeClass();
        if (clazz == null) {
            return;
        }

        boolean isFromWrapper = part.getMessageInfo().getOperation().isUnwrapped();
        boolean isList = false;
        if (clazz.isArray()) {
            if (isFromWrapper && !Byte.TYPE.equals(clazz.getComponentType())) {
                clazz = clazz.getComponentType();
            } else if (!isFromWrapper) {
                Annotation[] anns = (Annotation[])part.getProperty("parameter.annotations");
                for (Annotation a : anns) {
                    if (a instanceof XmlList) {
                        part.setProperty("honor.jaxb.annotations", Boolean.TRUE);
                        clazz = clazz.getComponentType();
                        isList = true;
                    }
                }
            }
        }

        Annotation[] anns = (Annotation[])part.getProperty("parameter.annotations");
        XmlJavaTypeAdapter jta = findFromTypeAdapter(context, clazz, anns);
        JAXBBeanInfo jtaBeanInfo = null;
        if (jta != null) {
            jtaBeanInfo = findFromTypeAdapter(context, jta.value());
        }
        JAXBBeanInfo beanInfo = getBeanInfo(clazz);
        if (jtaBeanInfo != beanInfo && jta != null) {
            beanInfo = jtaBeanInfo;
            if (anns == null) {
                anns = new Annotation[] {jta};
            } else {
                boolean found = false;
                for (Annotation t : anns) {
                    if (t == jta) {
                        found = true;
                    }
                }
                if (!found) {
                    Annotation[] tmp = new Annotation[anns.length + 1];
                    System.arraycopy(anns, 0, tmp, 0, anns.length);
                    tmp[anns.length] = jta;
                    anns = tmp;
                }
            }
            part.setProperty("parameter.annotations", anns);
            part.setProperty("honor.jaxb.annotations", Boolean.TRUE);
        }
        if (beanInfo == null) {
            if (Exception.class.isAssignableFrom(clazz)) {
                QName name = (QName)part.getMessageInfo().getProperty("elementName");
                part.setElementQName(name);
                buildExceptionType(part, clazz);
            }
            return;
        }
        boolean isElement = beanInfo.isElement()
            && !Boolean.TRUE.equals(part.getMessageInfo().getOperation()
                                        .getProperty("operation.force.types"));
        boolean hasType = !beanInfo.getTypeNames().isEmpty();
        if (isElement && isFromWrapper && hasType) {
            //if there is both a Global element and a global type, AND we are in a wrapper,
            //make sure we use the type instead of a ref to the element to
            //match the rules for wrapped/unwrapped
            isElement = false;
        }

        part.setElement(isElement);

        if (isElement) {
            QName name = new QName(beanInfo.getElementNamespaceURI(null),
                                   beanInfo.getElementLocalName(null));
            XmlSchemaElement el = schemas.getElementByQName(name);
            if (el != null && el.getRef().getTarget() != null) {
                part.setTypeQName(el.getRef().getTargetQName());
            } else {
                part.setElementQName(name);
            }
            part.setXmlSchema(el);
        } else  {
            QName typeName = getTypeName(beanInfo);
            if (typeName != null) {
                XmlSchemaType type = schemas.getTypeByQName(typeName);
                if  (isList && type instanceof XmlSchemaSimpleType) {
                    XmlSchemaSimpleType simpleType = new XmlSchemaSimpleType(type.getParent(), false);
                    XmlSchemaSimpleTypeList list = new XmlSchemaSimpleTypeList();
                    XmlSchemaSimpleType stype = (XmlSchemaSimpleType)type;
                    list.setItemTypeName(stype.getQName());
                    simpleType.setContent(list);
                    part.setXmlSchema(simpleType);
                    if (part.getConcreteName() == null) {
                        part.setConcreteName(new QName(null, part.getName().getLocalPart()));
                    }
                } else {
                    part.setTypeQName(typeName);
                    part.setXmlSchema(type);
                }
            }
        }
    }

    static XmlJavaTypeAdapter findFromTypeAdapter(JAXBContextProxy context, Class<?> clazz, Annotation[] anns) {
        if (anns != null) {
            for (Annotation a : anns) {
                if (XmlJavaTypeAdapter.class.isAssignableFrom(a.annotationType())) {
                    JAXBBeanInfo ret = findFromTypeAdapter(context, ((XmlJavaTypeAdapter)a).value());
                    if (ret != null) {
                        return (XmlJavaTypeAdapter)a;
                    }
                }
            }
        }
        if (clazz != null) {
            XmlJavaTypeAdapter xjta = clazz.getAnnotation(XmlJavaTypeAdapter.class);
            if (xjta != null) {
                JAXBBeanInfo ret = findFromTypeAdapter(context, xjta.value());
                if (ret != null) {
                    return xjta;
                }
            }
        }
        return null;
    }

    static JAXBBeanInfo findFromTypeAdapter(JAXBContextProxy context,
                                            @SuppressWarnings("rawtypes")
                                             Class<? extends XmlAdapter> aclass) {
        Class<?> c2 = aclass;
        Type sp = c2.getGenericSuperclass();
        while (!XmlAdapter.class.equals(c2) && c2 != null) {
            sp = c2.getGenericSuperclass();
            c2 = c2.getSuperclass();
        }
        if (sp instanceof ParameterizedType) {
            Type tp = ((ParameterizedType)sp).getActualTypeArguments()[0];
            if (tp instanceof Class) {
                return getBeanInfo(context, (Class<?>)tp);
            }
        }
        return null;
    }

    private QName getTypeName(JAXBBeanInfo beanInfo) {
        Iterator<QName> itr = beanInfo.getTypeNames().iterator();
        if (!itr.hasNext()) {
            return null;
        }

        return itr.next();
    }
    public void checkForExistence(MessagePartInfo part) {
        QName qn = part.getElementQName();
        if (qn != null) {
            XmlSchemaElement el = schemas.getElementByQName(qn);
            if (el == null) {
                Class<?> clazz = part.getTypeClass();
                if (clazz == null) {
                    return;
                }

                boolean isFromWrapper = part.getMessageInfo().getOperation().isUnwrapped();
                if (isFromWrapper && clazz.isArray() && !Byte.TYPE.equals(clazz.getComponentType())) {
                    clazz = clazz.getComponentType();
                }
                JAXBBeanInfo beanInfo = getBeanInfo(clazz);
                if (beanInfo == null) {
                    if (Exception.class.isAssignableFrom(clazz)) {
                        QName name = (QName)part.getMessageInfo().getProperty("elementName");
                        part.setElementQName(name);
                        buildExceptionType(part, clazz);
                    }
                    return;
                }

                QName typeName = getTypeName(beanInfo);

                createBridgeXsElement(part, qn, typeName);
            } else if (part.getXmlSchema() == null) {
                part.setXmlSchema(el);
            }
        }
    }

    private void createBridgeXsElement(MessagePartInfo part, QName qn, QName typeName) {
        SchemaInfo schemaInfo = serviceInfo.getSchema(qn.getNamespaceURI());
        if (schemaInfo != null) {
            XmlSchemaElement el = schemaInfo.getElementByQName(qn);
            if (el == null) {
                createXsElement(schemaInfo.getSchema(), part, typeName, schemaInfo);

            } else if (!typeName.equals(el.getSchemaTypeName())) {
                throw new Fault(new Message("CANNOT_CREATE_ELEMENT", LOG,
                                            qn, typeName, el.getSchemaTypeName()));
            }
            return;
        }

        XmlSchema schema = schemas.newXmlSchemaInCollection(qn.getNamespaceURI());
        if (qualifiedSchemas) {
            schema.setElementFormDefault(XmlSchemaForm.QUALIFIED);
        }
        schemaInfo = new SchemaInfo(qn.getNamespaceURI(), qualifiedSchemas, false);
        schemaInfo.setSchema(schema);

        createXsElement(schema, part, typeName, schemaInfo);

        NamespaceMap nsMap = new NamespaceMap();
        nsMap.add(WSDLConstants.CONVENTIONAL_TNS_PREFIX, schema.getTargetNamespace());
        nsMap.add(WSDLConstants.NP_SCHEMA_XSD, WSDLConstants.NS_SCHEMA_XSD);
        schema.setNamespaceContext(nsMap);

        serviceInfo.addSchema(schemaInfo);
    }

    private XmlSchemaElement createXsElement(XmlSchema schema,
                                             MessagePartInfo part,
                                             QName typeName, SchemaInfo schemaInfo) {
        XmlSchemaElement el = new XmlSchemaElement(schema, true);
        el.setName(part.getElementQName().getLocalPart());
        el.setNillable(true);
        el.setSchemaTypeName(typeName);
        part.setXmlSchema(el);
        schemaInfo.setElement(null);
        return el;
    }

    public void end(FaultInfo fault) {
        MessagePartInfo part = fault.getFirstMessagePart();
        Class<?> cls = part.getTypeClass();
        Class<?> cl2 = (Class<?>)fault.getProperty(Class.class.getName());
        if (cls != cl2) {
            QName name = (QName)fault.getProperty("elementName");
            part.setElementQName(name);
            JAXBBeanInfo beanInfo = getBeanInfo(cls);
            if (beanInfo == null) {
                throw new Fault(new Message("NO_BEAN_INFO", LOG, cls.getName()));
            }
            SchemaInfo schemaInfo = serviceInfo.getSchema(part.getElementQName().getNamespaceURI());
            if (schemaInfo != null
                && !isExistSchemaElement(schemaInfo.getSchema(), part.getElementQName())) {

                XmlSchemaElement el = new XmlSchemaElement(schemaInfo.getSchema(), true);
                el.setName(part.getElementQName().getLocalPart());
                el.setNillable(true);

                schemaInfo.setElement(null);

                Iterator<QName> itr = beanInfo.getTypeNames().iterator();
                if (!itr.hasNext()) {
                    return;
                }
                QName typeName = itr.next();
                el.setSchemaTypeName(typeName);
            }
        } else if (part.getXmlSchema() == null) {
            try {
                cls.getConstructor(new Class[] {String.class});
            } catch (Exception e) {
                try {
                    cls.getConstructor(new Class[0]);
                } catch (Exception e2) {
                    //no String or default constructor, we cannot use it
                    return;
                }
            }

            //not mappable in JAXBContext directly, we'll have to do it manually :-(
            SchemaInfo schemaInfo = serviceInfo.getSchema(part.getElementQName().getNamespaceURI());
            if (schemaInfo == null
                || isExistSchemaElement(schemaInfo.getSchema(), part.getElementQName())) {
                return;
            }

            XmlSchemaElement el = new XmlSchemaElement(schemaInfo.getSchema(), true);
            el.setName(part.getElementQName().getLocalPart());

            schemaInfo.setElement(null);

            part.setXmlSchema(el);

            XmlSchemaComplexType ct = new XmlSchemaComplexType(schemaInfo.getSchema(), false);
            el.setSchemaType(ct);
            XmlSchemaSequence seq = new XmlSchemaSequence();
            ct.setParticle(seq);

            Method[] methods = cls.getMethods();
            for (Method m : methods) {
                if (m.getName().startsWith("get")
                    || m.getName().startsWith("is")) {
                    int beginIdx = m.getName().startsWith("get") ? 3 : 2;
                    try {
                        m.getDeclaringClass().getMethod("set" + m.getName().substring(beginIdx),
                                                        m.getReturnType());

                        JAXBBeanInfo beanInfo = getBeanInfo(m.getReturnType());
                        if (beanInfo != null) {
                            el = new XmlSchemaElement(schemaInfo.getSchema(), false);
                            el.setName(m.getName().substring(beginIdx));
                            Iterator<QName> itr = beanInfo.getTypeNames().iterator();
                            if (!itr.hasNext()) {
                                return;
                            }
                            QName typeName = itr.next();
                            el.setSchemaTypeName(typeName);
                        }

                        seq.getItems().add(el);
                    } catch (Exception e) {
                        //not mappable
                    }
                }
            }
        }
    }


    private void buildExceptionType(MessagePartInfo part, Class<?> cls) {
        SchemaInfo schemaInfo = null;
        for (SchemaInfo s : serviceInfo.getSchemas()) {
            if (s.getNamespaceURI().equals(part.getElementQName().getNamespaceURI())) {
                schemaInfo = s;
                break;
            }
        }
        XmlAccessorOrder xmlAccessorOrder = cls.getAnnotation(XmlAccessorOrder.class);
        XmlType xmlTypeAnno = cls.getAnnotation(XmlType.class);
        String[] propertyOrder = null;
        boolean respectXmlTypeNS = false;
        XmlSchema faultBeanSchema = null;
        if (xmlTypeAnno != null && !StringUtils.isEmpty(xmlTypeAnno.namespace())
            && !xmlTypeAnno.namespace().equals(part.getElementQName().getNamespaceURI())) {
            respectXmlTypeNS = true;
            NamespaceMap nsMap = new NamespaceMap();
            nsMap.add(WSDLConstants.CONVENTIONAL_TNS_PREFIX, xmlTypeAnno.namespace());
            nsMap.add(WSDLConstants.NP_SCHEMA_XSD, WSDLConstants.NS_SCHEMA_XSD);

            SchemaInfo faultBeanSchemaInfo = createSchemaIfNeeded(xmlTypeAnno.namespace(), nsMap);
            faultBeanSchema = faultBeanSchemaInfo.getSchema();
        }

        if (xmlTypeAnno != null &&  xmlTypeAnno.propOrder().length > 0) {
            propertyOrder = xmlTypeAnno.propOrder();
            //TODO: handle @XmlAccessOrder
        }

        if (schemaInfo == null) {
            NamespaceMap nsMap = new NamespaceMap();
            nsMap.add(WSDLConstants.CONVENTIONAL_TNS_PREFIX, part.getElementQName().getNamespaceURI());
            nsMap.add(WSDLConstants.NP_SCHEMA_XSD, WSDLConstants.NS_SCHEMA_XSD);
            schemaInfo = createSchemaIfNeeded(part.getElementQName().getNamespaceURI(), nsMap);

        }
        XmlSchema schema = schemaInfo.getSchema();


        // Before updating everything, make sure we haven't added this
        // type yet.  Multiple methods that throw the same exception
        // types will cause duplicates.
        String faultTypeName = xmlTypeAnno != null && !StringUtils.isEmpty(xmlTypeAnno.name())
               ? xmlTypeAnno.name()  :  part.getElementQName().getLocalPart();
        XmlSchemaType existingType = schema.getTypeByName(faultTypeName);
        if (existingType != null) {
            return;
        }

        XmlSchemaElement el = new XmlSchemaElement(schema, true);
        el.setName(part.getElementQName().getLocalPart());
        part.setXmlSchema(el);
        schemaInfo.setElement(null);

        if (respectXmlTypeNS) {
            schema = faultBeanSchema; //create complexType in the new created schema for xmlType
        }

        XmlSchemaComplexType ct = new XmlSchemaComplexType(schema, true);
        ct.setName(faultTypeName);

        el.setSchemaTypeName(ct.getQName());

        XmlSchemaSequence seq = new XmlSchemaSequence();
        ct.setParticle(seq);
        String namespace = part.getElementQName().getNamespaceURI();
        XmlAccessType accessType = Utils.getXmlAccessType(cls);
//
        for (Field f : Utils.getFields(cls, accessType)) {
            //map field
            Type type = Utils.getFieldType(f);
            //we want to return the right type for collections so if we get null
            //from the return type we check if it's ParameterizedType and get the
            //generic return type.
            if ((type == null) && (f.getGenericType() instanceof ParameterizedType)) {
                type = f.getGenericType();
            }
            if (generateGenericType(type)) {
                buildGenericElements(schema, seq, f);
            } else {
                JAXBBeanInfo beanInfo = getBeanInfo(type);
                if (beanInfo != null) {
                    XmlElement xmlElementAnno = f.getAnnotation(XmlElement.class);
                    addElement(schema, seq, beanInfo, new QName(namespace, f.getName()), isArray(type), xmlElementAnno);
                }
            }
        }
        for (Method m : Utils.getGetters(cls, accessType)) {
            //map method
            Type type = Utils.getMethodReturnType(m);
            // we want to return the right type for collections so if we get null
            // from the return type we check if it's ParameterizedType and get the
            // generic return type.
            if ((type == null) && (m.getGenericReturnType() instanceof ParameterizedType)) {
                type = m.getGenericReturnType();
            }

            if (generateGenericType(type)) {
                buildGenericElements(schema, seq, m, type);
            } else {
                JAXBBeanInfo beanInfo = getBeanInfo(type);
                if (beanInfo != null) {
                    int idx = m.getName().startsWith("get") ? 3 : 2;
                    String name = m.getName().substring(idx);
                    name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                    XmlElement xmlElementAnno = m.getAnnotation(XmlElement.class);
                    addElement(schema, seq, beanInfo, new QName(namespace, name), isArray(type), xmlElementAnno);
                }
            }
        }
        // Create element in xsd:sequence for Exception.class
        if (Exception.class.isAssignableFrom(cls)) {
            addExceptionMessage(cls, schema, seq);
        }

        if (propertyOrder != null) {
            if (propertyOrder.length == seq.getItems().size()) {
                sortItems(seq, propertyOrder);
            } else if (propertyOrder.length > 1
                || (propertyOrder.length == 1 && !propertyOrder[0].isEmpty())) {
                LOG.log(Level.WARNING, "propOrder in @XmlType doesn't define all schema elements :"
                    + Arrays.toString(propertyOrder));
            }
        }

        if (xmlAccessorOrder != null && xmlAccessorOrder.value().equals(XmlAccessOrder.ALPHABETICAL)
            && propertyOrder == null) {
            sort(seq);
        }

        schemas.addCrossImports();
        part.setProperty(JAXBDataBinding.class.getName() + ".CUSTOM_EXCEPTION", Boolean.TRUE);
    }
    private void addExceptionMessage(Class<?> cls, XmlSchema schema, XmlSchemaSequence seq) {
        try {
            //a subclass could mark the message method as transient
            Method m = cls.getMethod("getMessage");
            if (!m.isAnnotationPresent(XmlTransient.class)
                && m.getDeclaringClass().equals(Throwable.class)) {
                JAXBBeanInfo beanInfo = getBeanInfo(java.lang.String.class);
                XmlSchemaElement exEle = new XmlSchemaElement(schema, false);
                exEle.setName("message");
                exEle.setSchemaTypeName(getTypeName(beanInfo));
                exEle.setMinOccurs(0);
                seq.getItems().add(exEle);
            }
        } catch (Exception e) {
            //ignore, just won't have the message element
        }
    }

    private boolean generateGenericType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType)type;
            if (paramType.getActualTypeArguments().length > 1) {
                return true;

            }
        }
        return false;
    }

    private void buildGenericElements(XmlSchema schema, XmlSchemaSequence seq, Field f) {
        XmlSchemaComplexType generics = new XmlSchemaComplexType(schema, true);
        Type type = f.getGenericType();
        String rawType = ((ParameterizedType)type).getRawType().toString();
        String typeName = StringUtils.uncapitalize(rawType.substring(rawType.lastIndexOf('.') + 1));
        generics.setName(typeName);

        Class<?> genericsClass = f.getType();
        buildGenericSeq(schema, generics, genericsClass);

        String name = Character.toLowerCase(f.getName().charAt(0)) + f.getName().substring(1);
        XmlSchemaElement newel = new XmlSchemaElement(schema, false);
        newel.setName(name);
        newel.setSchemaTypeName(generics.getQName());
        newel.setMinOccurs(0);
        if (!seq.getItems().contains(newel)) {
            seq.getItems().add(newel);
        }
    }

    private void buildGenericElements(XmlSchema schema, XmlSchemaSequence seq, Method m, Type type) {
        String rawType = ((ParameterizedType)type).getRawType().toString();
        String typeName = StringUtils.uncapitalize(rawType.substring(rawType.lastIndexOf('.') + 1));

        XmlSchemaComplexType generics = (XmlSchemaComplexType)schema.getTypeByName(typeName);
        if (generics == null) {
            generics = new XmlSchemaComplexType(schema, true);
            generics.setName(typeName);
        }

        Class<?> genericsClass = m.getReturnType();
        buildGenericSeq(schema, generics, genericsClass);

        int idx = m.getName().startsWith("get") ? 3 : 2;
        String name = m.getName().substring(idx);
        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        XmlSchemaElement newel = new XmlSchemaElement(schema, false);
        newel.setName(name);
        newel.setSchemaTypeName(generics.getQName());
        newel.setMinOccurs(0);
        if (!seq.getItems().contains(newel)) {
            seq.getItems().add(newel);
        }
    }

    private void buildGenericSeq(XmlSchema schema, XmlSchemaComplexType generics, Class<?> genericsClass) {
        XmlSchemaSequence genericsSeq = new XmlSchemaSequence();
        generics.setParticle(genericsSeq);
        XmlAccessType accessType = Utils.getXmlAccessType(genericsClass);

        for (Field f : Utils.getFields(genericsClass, accessType)) {
            if (f.getGenericType() instanceof TypeVariable) {
                String genericName = Character.toLowerCase(f.getName().charAt(0)) + f.getName().substring(1);
                XmlSchemaElement genericEle = new XmlSchemaElement(schema, false);
                genericEle.setName(genericName);
                genericEle.setMinOccurs(0);
                JAXBBeanInfo anyBean = getBeanInfo(context, f.getType());
                Iterator<QName> itr = anyBean.getTypeNames().iterator();
                if (!itr.hasNext()) {
                    return;
                }
                QName typeName = itr.next();
                genericEle.setSchemaTypeName(typeName);
                genericsSeq.getItems().add(genericEle);
            }
        }

        for (Method genericMethod : Utils.getGetters(genericsClass, accessType)) {
            if (genericMethod.getGenericReturnType() instanceof TypeVariable) {
                int idx = genericMethod.getName().startsWith("get") ? 3 : 2;
                String genericName = genericMethod.getName().substring(idx);
                genericName = Character.toLowerCase(genericName.charAt(0)) + genericName.substring(1);
                XmlSchemaElement genericEle = new XmlSchemaElement(schema, false);
                genericEle.setName(genericName);
                genericEle.setMinOccurs(0);
                JAXBBeanInfo anyBean = getBeanInfo(context, genericMethod.getReturnType());
                Iterator<QName> itr = anyBean.getTypeNames().iterator();
                if (!itr.hasNext()) {
                    return;
                }
                QName typeName = itr.next();
                genericEle.setSchemaTypeName(typeName);
                genericsSeq.getItems().add(genericEle);
            }

        }
    }


    static boolean isArray(Type cls) {
        if (cls instanceof Class) {
            return ((Class<?>)cls).isArray();
        } else if (cls instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType)cls;
            return pt.getActualTypeArguments().length == 1
                && pt.getRawType() instanceof Class
                && Collection.class.isAssignableFrom((Class<?>)pt.getRawType());
        } else if (cls instanceof GenericArrayType) {
            return true;
        }
        return false;
    }

    protected void addElement(XmlSchema schema,
                              XmlSchemaSequence seq, JAXBBeanInfo beanInfo,
                              QName name, boolean isArray, XmlElement xmlElementAnno) {
        XmlSchemaElement el = new XmlSchemaElement(schema, false);
        if (isArray) {
            el.setMinOccurs(0);
            el.setMaxOccurs(Long.MAX_VALUE);
        } else {
            if (xmlElementAnno == null) {
                el.setMinOccurs(0);
                el.setNillable(false);
            } else {
                el.setNillable(xmlElementAnno.nillable());
                int minOccurs = xmlElementAnno.required() ? 1 : 0;
                el.setMinOccurs(minOccurs);
            }
        }

        if (beanInfo.isElement()) {
            QName ename = new QName(beanInfo.getElementNamespaceURI(null),
                                   beanInfo.getElementLocalName(null));
            XmlSchemaElement el2 = schemas.getElementByQName(ename);
            el.setNillable(false);
            el.getRef().setTargetQName(el2.getQName());
        } else {
            if (xmlElementAnno != null && !StringUtils.isEmpty(xmlElementAnno.name())) {
                el.setName(xmlElementAnno.name());
            } else {
                el.setName(name.getLocalPart());
            }
            Iterator<QName> itr = beanInfo.getTypeNames().iterator();
            if (!itr.hasNext()) {
                return;
            }
            QName typeName = itr.next();
            el.setSchemaTypeName(typeName);
        }

        seq.getItems().add(el);
    }

    private SchemaInfo createSchemaIfNeeded(String namespace, NamespaceMap nsMap) {
        SchemaInfo schemaInfo = serviceInfo.getSchema(namespace);
        if (schemaInfo == null) {
            XmlSchema xmlSchema = schemas.newXmlSchemaInCollection(namespace);

            if (qualifiedSchemas) {
                xmlSchema.setElementFormDefault(XmlSchemaForm.QUALIFIED);
            }

            xmlSchema.setNamespaceContext(nsMap);

            schemaInfo = new SchemaInfo(namespace);
            schemaInfo.setSchema(xmlSchema);
            serviceInfo.addSchema(schemaInfo);
        }
        return schemaInfo;
    }

    private boolean isExistSchemaElement(XmlSchema schema, QName qn) {
        return schema.getElementByName(qn) != null;
    }

    private void sortItems(final XmlSchemaSequence seq, final String[] propertyOrder) {
        final List<String> propList = Arrays.asList(propertyOrder);
        Collections.sort(seq.getItems(), new Comparator<XmlSchemaSequenceMember>() {
            public int compare(XmlSchemaSequenceMember o1, XmlSchemaSequenceMember o2) {
                XmlSchemaElement element1 = (XmlSchemaElement)o1;
                XmlSchemaElement element2 = (XmlSchemaElement)o2;
                int index1 = propList.indexOf(element1.getName());
                int index2 = propList.indexOf(element2.getName());
                return index1 - index2;
            }

        });
    }
    //sort to Alphabetical order
    private void sort(final XmlSchemaSequence seq) {
        Collections.sort(seq.getItems(), new Comparator<XmlSchemaSequenceMember>() {
            public int compare(XmlSchemaSequenceMember o1, XmlSchemaSequenceMember o2) {
                XmlSchemaElement element1 = (XmlSchemaElement)o1;
                XmlSchemaElement element2 = (XmlSchemaElement)o2;
                return element1.getName().compareTo(element2.getName());
            }

        });
    }

}

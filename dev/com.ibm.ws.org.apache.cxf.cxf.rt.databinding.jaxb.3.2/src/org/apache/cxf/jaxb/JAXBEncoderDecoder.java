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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.util.StreamReaderDelegate;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.staxutils.W3CNamespaceContext;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeList;
import org.apache.ws.commons.schema.constants.Constants;

/**
 * Utility functions for JAXB.
 */
public final class JAXBEncoderDecoder {
    private static final class AddXSITypeStreamReader extends StreamReaderDelegate {
        private boolean first = true;
        private int offset = 1;
        private final QName typeQName;

        private AddXSITypeStreamReader(XMLStreamReader reader, QName typeQName) {
            super(reader);
            this.typeQName = typeQName;
        }

        public int getAttributeCount() {
            return super.getAttributeCount() + offset;
        }

        public String getAttributeLocalName(int index) {
            if (first && index == 0) {
                return "type";
            }
            return super.getAttributeLocalName(index - offset);
        }

        public QName getAttributeName(int index) {
            if (first && index == 0) {
                return new QName(Constants.URI_2001_SCHEMA_XSI, "type");
            }
            return super.getAttributeName(index - offset);
        }

        public String getAttributeNamespace(int index) {
            if (first && index == 0) {
                return Constants.URI_2001_SCHEMA_XSI;
            }
            return super.getAttributeNamespace(index - offset);
        }

        public String getAttributePrefix(int index) {
            if (first && index == 0) {
                return "xsi";
            }
            return super.getAttributePrefix(index - offset);
        }

        public String getAttributeType(int index) {
            if (first && index == 0) {
                return "#TEXT";
            }
            return super.getAttributeType(index - offset);
        }

        public String getAttributeValue(int index) {
            if (first && index == 0) {
                String pfx = this.getNamespaceContext().getPrefix(typeQName.getNamespaceURI());
                if (StringUtils.isEmpty(pfx)) {
                    return typeQName.getLocalPart();
                }
                return pfx + ":" + typeQName.getLocalPart();
            }
            return super.getAttributeValue(index - offset);
        }

        public int next()  throws XMLStreamException {
            first = false;
            offset = 0;
            return super.next();
        }

        public String getAttributeValue(String namespaceUri,
                                        String localName) {
            if (first
                && Constants.URI_2001_SCHEMA_XSI.equals(namespaceUri)
                && "type".equals(localName)) {
                String pfx = this.getNamespaceContext().getPrefix(typeQName.getNamespaceURI());
                if (StringUtils.isEmpty(pfx)) {
                    return typeQName.getLocalPart();
                }
                return pfx + ":" + typeQName.getLocalPart();
            }
            return super.getAttributeValue(namespaceUri, localName);
        }
    }

    private static final Logger LOG = LogUtils.getLogger(JAXBEncoderDecoder.class);

    private JAXBEncoderDecoder() {
    }

    public static void marshall(Marshaller marshaller,
                                Object elValue,
                                MessagePartInfo part,
                                Object source) {
        try {
            // The Marshaller.JAXB_FRAGMENT will tell the Marshaller not to
            // generate the xml declaration.
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        } catch (javax.xml.bind.PropertyException e) {
            // intentionally empty.
        }

        Class<?> cls = null;
        if (part != null) {
            cls = part.getTypeClass();
        }

        if (cls == null) {
            cls = null != elValue ? elValue.getClass() : null;
        }

        if (cls != null && cls.isArray() && elValue instanceof Collection) {
            Collection<?> col = (Collection<?>)elValue;
            elValue = col.toArray((Object[])Array.newInstance(cls.getComponentType(), col.size()));
        }

        try {
            Object mObj = elValue;
            QName elName = null;
            if (part != null) {
                elName = part.getConcreteName();
            }

            if (null != elName) {

                if (part != null && part.getXmlSchema() instanceof XmlSchemaElement) {

                    XmlSchemaElement el = (XmlSchemaElement)part.getXmlSchema();

                    if (mObj.getClass().isArray()
                        && el.getSchemaType() instanceof XmlSchemaSimpleType
                        && ((XmlSchemaSimpleType)el.getSchemaType()).
                        getContent() instanceof XmlSchemaSimpleTypeList) {
                        mObj = Arrays.asList((Object[])mObj);
                        writeObject(marshaller, source, newJAXBElement(elName, cls, mObj));
                    } else if (part.getMessageInfo().getOperation().isUnwrapped()
                               && (mObj.getClass().isArray() || mObj instanceof List)
                               && el.getMaxOccurs() != 1) {
                        writeArrayObject(marshaller,
                                         source,
                                         elName,
                                         mObj);
                    } else {
                        writeObject(marshaller, source, newJAXBElement(elName, cls, mObj));
                    }
                } else if (byte[].class == cls && part.getTypeQName() != null
                           && "hexBinary".equals(part.getTypeQName().getLocalPart())) {
                    mObj = new HexBinaryAdapter().marshal((byte[])mObj);
                    writeObject(marshaller, source, newJAXBElement(elName, String.class, mObj));
                } else if (mObj instanceof JAXBElement) {
                    writeObject(marshaller, source, mObj);
                } else if (marshaller.getSchema() != null) {
                    //force xsi:type so types can be validated instead of trying to
                    //use the RPC/lit element names that aren't in the schema
                    writeObject(marshaller, source, newJAXBElement(elName, Object.class, mObj));
                } else {
                    writeObject(marshaller, source, newJAXBElement(elName, cls, mObj));
                }
            } else {
                writeObject(marshaller, source, mObj);
            }
        } catch (Fault ex) {
            throw ex;
        } catch (javax.xml.bind.MarshalException ex) {
            Message faultMessage = new Message("MARSHAL_ERROR", LOG, ex.getLinkedException()
                .getMessage());
            throw new Fault(faultMessage, ex);
        } catch (Exception ex) {
            throw new Fault(new Message("MARSHAL_ERROR", LOG, ex.getMessage()), ex);
        }
    }
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static JAXBElement<?> newJAXBElement(QName elName, Class<?> cls, Object mObj) {
        if (mObj instanceof JAXBElement) {
            return (JAXBElement)mObj;
        }
        if (cls == null && mObj != null) {
            cls = mObj.getClass();
        }
        return new JAXBElement(elName, cls, mObj);
    }

    //TODO: cache the JAXBRIContext
    public static void marshalWithBridge(QName qname,
                                         Class<?> cls,
                                         Annotation[] anns,
                                         Set<Class<?>> ctxClasses,
                                         Object elValue,
                                         Object source, AttachmentMarshaller am) {
        try {
            JAXBUtils.BridgeWrapper bridge = JAXBUtils.createBridge(ctxClasses, qname, cls, anns);

            if (source instanceof XMLStreamWriter) {
                bridge.marshal(elValue, (XMLStreamWriter)source, am);
            } else if (source instanceof OutputStream) {
                //the namespace is missing when marshal the xsd:QName type
                //to the OutputStream directly
                java.io.StringWriter sw = new java.io.StringWriter();
                StreamResult s1 = new StreamResult(sw);
                bridge.marshal(elValue, s1);
                ((OutputStream)source).write(sw.toString().getBytes());
            } else if (source instanceof Node) {
                bridge.marshal(elValue, (Node)source, am);
            } else {
                throw new Fault(new Message("UNKNOWN_SOURCE", LOG, source.getClass().getName()));
            }
        } catch (javax.xml.bind.MarshalException ex) {
            Message faultMessage = new Message("MARSHAL_ERROR", LOG, ex.getLinkedException()
                .getMessage());
            throw new Fault(faultMessage, ex);
        } catch (Exception ex) {
            throw new Fault(new Message("MARSHAL_ERROR", LOG, ex.getMessage()), ex);
        }

    }

//  TODO: cache the JAXBRIContext
    public static Object unmarshalWithBridge(QName qname,
                                             Class<?> cls,
                                             Annotation[] anns,
                                             Set<Class<?>> ctxClasses,
                                             Object source,
                                             AttachmentUnmarshaller am) {

        try {
            JAXBUtils.BridgeWrapper bridge = JAXBUtils.createBridge(ctxClasses, qname, cls, anns);

            if (source instanceof XMLStreamReader) {
                //DOMUtils.writeXml(StaxUtils.read((XMLStreamReader)source), System.out);
                return bridge.unmarshal((XMLStreamReader)source, am);
            } else if (source instanceof InputStream) {
                return bridge.unmarshal((InputStream)source);
            } else if (source instanceof Node) {
                return bridge.unmarshal((Node)source, am);
            } else {
                throw new Fault(new Message("UNKNOWN_SOURCE", LOG, source.getClass().getName()));
            }
        } catch (javax.xml.bind.MarshalException ex) {
            Message faultMessage = new Message("MARSHAL_ERROR", LOG, ex.getLinkedException()
                .getMessage());
            throw new Fault(faultMessage, ex);
        } catch (Exception ex) {
            throw new Fault(new Message("MARSHAL_ERROR", LOG, ex.getMessage()), ex);
        }

    }

    public static void marshallException(Marshaller marshaller, Exception elValue,
                                         MessagePartInfo part, Object source) {
        XMLStreamWriter writer = getStreamWriter(source);
        QName qn = part.getElementQName();
        try {
            writer.writeStartElement("ns1", qn.getLocalPart(), qn.getNamespaceURI());
            Class<?> cls = part.getTypeClass();
            XmlAccessType accessType = Utils.getXmlAccessType(cls);
            String namespace = part.getElementQName().getNamespaceURI();
            String attNs = namespace;

            SchemaInfo sch = part.getMessageInfo().getOperation().getInterface()
                .getService().getSchema(namespace);
            if (sch == null) {
                LOG.warning("Schema associated with " + namespace + " is null");
                namespace = null;
                attNs = null;
            } else {
                if (!sch.isElementFormQualified()) {
                    namespace = null;
                }
                if (!sch.isAttributeFormQualified()) {
                    attNs = null;
                }
            }
            List<Member> combinedMembers = new ArrayList<>();

            for (Field f : Utils.getFields(cls, accessType)) {
                XmlAttribute at = f.getAnnotation(XmlAttribute.class);
                if (at == null) {
                    combinedMembers.add(f);
                } else {
                    QName fname = new QName(attNs, StringUtils.isEmpty(at.name()) ? f.getName() : at.name());
                    ReflectionUtil.setAccessible(f);
                    Object o = Utils.getFieldValue(f, elValue);
                    DocumentFragment frag = DOMUtils.getEmptyDocument().createDocumentFragment();
                    writeObject(marshaller, frag, newJAXBElement(fname, String.class, o));

                    if (attNs != null) {
                        writer.writeAttribute(attNs, fname.getLocalPart(),
                                              DOMUtils.getAllContent(frag));
                    } else {
                        writer.writeAttribute(fname.getLocalPart(), DOMUtils.getAllContent(frag));
                    }
                }
            }
            for (Method m : Utils.getGetters(cls, accessType)) {
                if (!m.isAnnotationPresent(XmlAttribute.class)) {
                    combinedMembers.add(m);
                } else {
                    int idx = m.getName().startsWith("get") ? 3 : 2;
                    String name = m.getName().substring(idx);
                    name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                    XmlAttribute at = m.getAnnotation(XmlAttribute.class);
                    QName mname = new QName(namespace, StringUtils.isEmpty(at.name()) ? name : at.name());
                    DocumentFragment frag = DOMUtils.getEmptyDocument().createDocumentFragment();
                    Object o = Utils.getMethodValue(m, elValue);
                    writeObject(marshaller, frag, newJAXBElement(mname, String.class, o));
                    if (attNs != null) {
                        writer.writeAttribute(attNs, mname.getLocalPart(),
                                              DOMUtils.getAllContent(frag));
                    } else {
                        writer.writeAttribute(mname.getLocalPart(), DOMUtils.getAllContent(frag));
                    }
                }
            }

            XmlAccessorOrder xmlAccessorOrder = cls.getAnnotation(XmlAccessorOrder.class);
            if (xmlAccessorOrder != null && xmlAccessorOrder.value().equals(XmlAccessOrder.ALPHABETICAL)) {
                Collections.sort(combinedMembers, new Comparator<Member>() {
                    public int compare(Member m1, Member m2) {
                        return m1.getName().compareTo(m2.getName());
                    }
                });
            }
            XmlType xmlType = cls.getAnnotation(XmlType.class);
            if (xmlType != null && xmlType.propOrder().length > 1 && !xmlType.propOrder()[0].isEmpty()) {
                final List<String> orderList = Arrays.asList(xmlType.propOrder());
                Collections.sort(combinedMembers, new Comparator<Member>() {
                    public int compare(Member m1, Member m2) {
                        String m1Name = getName(m1);
                        String m2Name = getName(m2);
                        int m1Index = orderList.indexOf(m1Name);
                        int m2Index = orderList.indexOf(m2Name);
                        if (m1Index != -1 && m2Index != -1) {
                            return m1Index - m2Index;
                        }
                        if (m1Index == -1 && m2Index != -1) {
                            return 1;
                        }
                        if (m1Index != -1 && m2Index == -1) {
                            return -1;
                        }
                        return 0;
                    }
                });
            }
            for (Member member : combinedMembers) {
                if (member instanceof Field) {
                    Field f = (Field)member;
                    QName fname = new QName(namespace, f.getName());
                    ReflectionUtil.setAccessible(f);
                    if (JAXBSchemaInitializer.isArray(f.getGenericType())) {
                        writeArrayObject(marshaller, writer, fname, f.get(elValue));
                    } else {
                        Object o = Utils.getFieldValue(f, elValue);
                        writeObject(marshaller, writer, newJAXBElement(fname, String.class, o));
                    }
                } else { // it's a Method
                    Method m = (Method)member;
                    int idx = m.getName().startsWith("get") ? 3 : 2;
                    String name = m.getName().substring(idx);
                    name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                    QName mname = new QName(namespace, name);
                    if (JAXBSchemaInitializer.isArray(m.getGenericReturnType())) {
                        writeArrayObject(marshaller, writer, mname, m.invoke(elValue));
                    } else {
                        Object o = Utils.getMethodValue(m, elValue);
                        writeObject(marshaller, writer, newJAXBElement(mname, String.class, o));
                    }
                }
            }

            writer.writeEndElement();
            writer.flush();
        } catch (Exception e) {
            throw new Fault(new Message("MARSHAL_ERROR", LOG, e.getMessage()), e);
        } finally {
            StaxUtils.close(writer);
        }
    }

    private static String getName(Member m1) {
        final String m1Name;
        if (m1 instanceof Field) {
            m1Name = ((Field)m1).getName();
        } else {
            int idx = m1.getName().startsWith("get") ? 3 : 2;
            String name = m1.getName().substring(idx);
            m1Name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return m1Name;
    }
    private static void writeArrayObject(Marshaller marshaller,
                                         Object source,
                                         QName mname,
                                         Object mObj) throws Fault, JAXBException {
        // Have to handle this ourselves.... which really
        // sucks.... but what can we do?
        if (mObj == null) {
            return;
        }
        Object objArray;
        final Class<?> cls;
        if (mObj instanceof List) {
            List<?> l = (List<?>)mObj;
            objArray = l.toArray();
            cls = null;
        } else {
            objArray = mObj;
            cls = objArray.getClass().getComponentType();
        }
        int len = Array.getLength(objArray);
        for (int x = 0; x < len; x++) {
            Object o = Array.get(objArray, x);
            writeObject(marshaller, source,
                        newJAXBElement(mname, cls == null ? o.getClass() : cls, o));
        }
    }

    public static Exception unmarshallException(Unmarshaller u,
                                                Object source,
                                                MessagePartInfo part) {
        XMLStreamReader reader;
        if (source instanceof XMLStreamReader) {
            reader = (XMLStreamReader)source;
        } else if (source instanceof Element) {
            reader = StaxUtils.createXMLStreamReader((Element)source);
            try {
                // advance into the node
                reader.nextTag();
            } catch (XMLStreamException e) {
                // ignore
            }
        } else {
            throw new Fault(new Message("UNKNOWN_SOURCE", LOG, source.getClass().getName()));
        }
        try {
            QName qn = part.getElementQName();
            if (!qn.equals(reader.getName())) {
                throw new Fault(new Message("ELEMENT_NAME_MISMATCH", LOG, qn, reader.getName()));
            }

            Class<?> cls = part.getTypeClass();
            Object obj;
            try {
                Constructor<?> cons = cls.getConstructor();
                obj = cons.newInstance();
            } catch (NoSuchMethodException nse) {
                Constructor<?> cons = cls.getConstructor(new Class[] {String.class});
                obj = cons.newInstance(new Object[1]);
            }

            XmlAccessType accessType = Utils.getXmlAccessType(cls);
            reader.nextTag();
            while (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
                QName q = reader.getName();
                String fieldName = q.getLocalPart();
                Field f = Utils.getField(cls, accessType, fieldName);
                if (f != null) {
                    Type type = f.getGenericType();
                    ReflectionUtil.setAccessible(f);
                    if (JAXBSchemaInitializer.isArray(type)) {
                        Class<?> compType = JAXBSchemaInitializer.getArrayComponentType(type);
                        List<Object> ret = unmarshallArray(u, reader, q, compType, createList(type));
                        Object o = ret;
                        if (!isList(type)) {
                            if (compType.isPrimitive()) {
                                o = java.lang.reflect.Array.newInstance(compType, ret.size());
                                for (int x = 0; x < ret.size(); x++) {
                                    Array.set(o, x, ret.get(x));
                                }
                            } else {
                                o = ret.toArray((Object[]) Array.newInstance(compType, ret.size()));
                            }
                        }

                        f.set(obj, o);
                    } else {
                        Object o = getElementValue(u.unmarshal(reader, Utils.getFieldType(f)));
                        Utils.setFieldValue(f, obj, o);
                    }
                } else {
                    String s = StringUtils.capitalize(q.getLocalPart());
                    Method m = Utils.getMethod(cls, accessType, "get" + s);
                    if (m == null) {
                        m = Utils.getMethod(cls, accessType, "is" + s);
                    }
                    Type type = m.getGenericReturnType();
                    Object o;
                    if (JAXBSchemaInitializer.isArray(type)) {
                        Class<?> compType = JAXBSchemaInitializer
                            .getArrayComponentType(type);
                        List<Object> ret = unmarshallArray(u, reader,
                                                           q,
                                                           compType,
                                                           createList(type));
                        o = ret;
                        if (!isList(type)) {
                            if (compType.isPrimitive()) {
                                o = java.lang.reflect.Array.newInstance(compType, ret.size());
                                for (int x = 0; x < ret.size(); x++) {
                                    Array.set(o, x, ret.get(x));
                                }
                            } else {
                                o = ret.toArray((Object[])Array.newInstance(compType, ret.size()));
                            }
                        }
                    } else {
                        o = getElementValue(u.unmarshal(reader, Utils.getMethodReturnType(m)));
                    }
                    Method m2 = Utils.getMethod(cls, accessType, "set" + s, m.getReturnType());
                    if (m2 != null) {
                        if (JAXBSchemaInitializer.isArray(type)) {
                            m2.invoke(obj, o);
                        } else {
                            Utils.setMethodValue(m, m2, obj, o);
                        }
                    } else {
                        Field fn = ReflectionUtil.getDeclaredField(cls, q.getLocalPart());
                        if (fn != null) {
                            ReflectionUtil.setAccessible(fn);
                            fn.set(obj, o);
                        }
                    }
                }
                if (reader.getEventType() == XMLStreamConstants.END_ELEMENT && q.equals(reader.getName())) {
                    reader.next();
                }
            }
            return (Exception)obj;
        } catch (Exception e) {
            throw new Fault(new Message("MARSHAL_ERROR", LOG, e.getMessage()), e);
        }
    }

    private static void writeObject(Marshaller u, Object source, Object mObj) throws Fault, JAXBException {
        if (source instanceof XMLStreamWriter) {
            // allows the XML Stream Writer to adjust it's behaviour based on the state of the unmarshaller
            if (source instanceof MarshallerAwareXMLWriter) {
                ((MarshallerAwareXMLWriter) source).setMarshaller(u);
            }
            u.marshal(mObj, (XMLStreamWriter)source);
        } else if (source instanceof OutputStream) {
            u.marshal(mObj, (OutputStream)source);
        } else if (source instanceof Node) {
            u.marshal(mObj, (Node)source);
        } else if (source instanceof XMLEventWriter) {
            // allows the XML Event Writer to adjust it's behaviour based on the state of the unmarshaller
            if (source instanceof MarshallerAwareXMLWriter) {
                ((MarshallerAwareXMLWriter) source).setMarshaller(u);
            }

            u.marshal(mObj, (XMLEventWriter)source);
        } else {
            throw new Fault(new Message("UNKNOWN_SOURCE", LOG, source.getClass().getName()));
        }
    }

    private static XMLStreamWriter getStreamWriter(Object source) throws Fault {
        if (source instanceof XMLStreamWriter) {
            return (XMLStreamWriter)source;
        } else if (source instanceof OutputStream) {
            return StaxUtils.createXMLStreamWriter((OutputStream)source);
        } else if (source instanceof Node) {
            return new W3CDOMStreamWriter((Element)source);
        }
        throw new Fault(new Message("UNKNOWN_SOURCE", LOG, source.getClass().getName()));
    }


    public static void marshallNullElement(Marshaller marshaller,
                                           Object source,
                                           MessagePartInfo part) {
        Class<?> clazz = part != null ? part.getTypeClass() : null;
        try {
            writeObject(marshaller, source, newJAXBElement(part.getElementQName(), clazz, null));
        } catch (JAXBException e) {
            throw new Fault(new Message("MARSHAL_ERROR", LOG, e.getMessage()), e);
        }
    }


    public static Object unmarshall(Unmarshaller u,
                                    Object source,
                                    MessagePartInfo part,
                                    boolean unwrap) {
        Class<?> clazz = part != null ? part.getTypeClass() : null;
        if (clazz != null && Exception.class.isAssignableFrom(clazz)
            && Boolean.TRUE.equals(part.getProperty(JAXBDataBinding.class.getName() + ".CUSTOM_EXCEPTION"))) {
            return unmarshallException(u, source, part);
        }

        QName elName = part != null ? part.getConcreteName() : null;
        if (clazz != null && clazz.isArray()
            && part.getXmlSchema() instanceof XmlSchemaElement) {
            XmlSchemaElement el = (XmlSchemaElement)part.getXmlSchema();

            if (el.getSchemaType() instanceof XmlSchemaSimpleType
                && ((XmlSchemaSimpleType)el.getSchemaType()).getContent()
                instanceof XmlSchemaSimpleTypeList) {

                Object obj = unmarshall(u, source, elName, null, unwrap);
                if (clazz.isArray() && obj instanceof List) {
                    return ((List<?>)obj).toArray((Object[])Array.newInstance(clazz.getComponentType(),
                                                                           ((List<?>)obj).size()));
                }

                return obj;
            } else if (part.getMessageInfo().getOperation().isUnwrapped() && el.getMaxOccurs() != 1) {
                // must read ourselves....
                List<Object> ret = unmarshallArray(u, source, elName, clazz.getComponentType(),
                                                   createList(part));
                Object o = ret;
                if (!isList(part)) {
                    if (isSet(part)) {
                        o = createSet(part, ret);
                    } else if (clazz.getComponentType().isPrimitive()) {
                        o = java.lang.reflect.Array.newInstance(clazz.getComponentType(), ret.size());
                        for (int x = 0; x < ret.size(); x++) {
                            Array.set(o, x, ret.get(x));
                        }
                    } else {
                        o = ret.toArray((Object[])Array.newInstance(clazz.getComponentType(), ret.size()));
                    }
                }
                return o;
            }
        } else if (byte[].class == clazz && part.getTypeQName() != null
                   && "hexBinary".equals(part.getTypeQName().getLocalPart())) {

            String obj = (String)unmarshall(u, source, elName, String.class, unwrap);
            return new HexBinaryAdapter().unmarshal(obj);
        } else if (part != null && u.getSchema() != null
            && !(part.getXmlSchema() instanceof XmlSchemaElement)) {
            //Validating RPC/Lit, make sure we don't try a root element name thing
            source = updateSourceWithXSIType(source, part.getTypeQName());
        }

        Object o = unmarshall(u, source, elName, clazz, unwrap);
        if (o != null && o.getClass().isArray() && isList(part)) {
            List<Object> ret = createList(part);
            Collections.addAll(ret, (Object[])o);
            o = ret;
        }
        return o;
    }

    private static Object updateSourceWithXSIType(Object source, final QName typeQName) {
        if (source instanceof XMLStreamReader
            && typeQName != null) {
            XMLStreamReader reader = (XMLStreamReader)source;
            String type = reader.getAttributeValue(Constants.URI_2001_SCHEMA_XSI, "type");
            if (StringUtils.isEmpty(type)) {
                source = new AddXSITypeStreamReader(reader, typeQName);
            }
        }
        return source;
    }

    private static Object createSet(MessagePartInfo part, List<Object> ret) {
        Type genericType = (Type)part.getProperty("generic.type");
        Class<?> tp2 = (Class<?>)((ParameterizedType)genericType).getRawType();
        if (tp2.isInterface()) {
            return new HashSet<>(ret);
        }
        Collection<Object> c;
        try {
            c = CastUtils.cast((Collection<?>)tp2.newInstance());
        } catch (Exception e) {
            c = new HashSet<>();
        }

        c.addAll(ret);
        return c;
    }

    private static boolean isSet(MessagePartInfo part) {
        if (part.getTypeClass().isArray() && !part.getTypeClass().getComponentType().isPrimitive()) {
            // && Collection.class.isAssignableFrom(part.getTypeClass())) {
            // it's List Para
            //
            Type genericType = (Type)part.getProperty("generic.type");

            if (genericType instanceof ParameterizedType) {
                Type tp2 = ((ParameterizedType)genericType).getRawType();
                if (tp2 instanceof Class) {
                    return Set.class.isAssignableFrom((Class<?>)tp2);
                }
            }
        }
        return false;
    }

    private static List<Object> createList(MessagePartInfo part) {
        Type genericType = (Type)part.getProperty("generic.type");
        return createList(genericType);
    }
    private static List<Object> createList(Type genericType) {
        if (genericType instanceof ParameterizedType) {
            Type tp2 = ((ParameterizedType)genericType).getRawType();
            if (tp2 instanceof Class) {
                Class<?> cls = (Class<?>)tp2;
                if (!cls.isInterface() && List.class.isAssignableFrom(cls)) {
                    try {
                        return CastUtils.cast((List<?>)cls.newInstance());
                    } catch (Exception e) {
                        // ignore, just return an ArrayList
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    private static boolean isList(Type cls) {
        return cls instanceof ParameterizedType;
    }
    private static boolean isList(MessagePartInfo part) {
        if (part.getTypeClass().isArray() && !part.getTypeClass().getComponentType().isPrimitive()) {
            // && Collection.class.isAssignableFrom(part.getTypeClass())) {
            // it's List Para
            //
            Type genericType = (Type)part.getProperty("generic.type");

            if (genericType instanceof ParameterizedType) {
                Type tp2 = ((ParameterizedType)genericType).getRawType();
                if (tp2 instanceof Class) {
                    return List.class.isAssignableFrom((Class<?>)tp2);
                }
            }
        }
        return false;
    }

    private static Object doUnmarshal(final Unmarshaller u,
                                      final Object source,
                                      final QName elName,
                                      final Class<?> clazz,
                                      final boolean unwrap) throws Exception {

        final Object obj;
        boolean unmarshalWithClass = true;

        if (clazz == null
            || (!clazz.isPrimitive()
                && !clazz.isArray()
                && !clazz.isEnum()
                && !clazz.equals(Calendar.class)
                && (Modifier.isAbstract(clazz.getModifiers())
                    || Modifier.isInterface(clazz.getModifiers())))) {
            unmarshalWithClass = false;
        }

        if (clazz != null
            && ("javax.xml.datatype.XMLGregorianCalendar".equals(clazz.getName())
                || "javax.xml.datatype.Duration".equals(clazz.getName()))) {
            // special treat two jaxb defined built-in abstract types
            unmarshalWithClass = true;
        }
        if (source instanceof Node) {
            obj = unmarshalWithClass ? u.unmarshal((Node)source, clazz)
                : u.unmarshal((Node)source);
        } else if (source instanceof DepthXMLStreamReader) {
            // JAXB optimizes a ton of stuff depending on the StreamReader impl. Thus,
            // we REALLY want to pass the original reader in.   This is OK with JAXB
            // as it doesn't read beyond the end so the DepthXMLStreamReader state
            // would be OK when it returns.   The main winner is FastInfoset where parsing
            // a testcase I have goes from about 300/sec to well over 1000.

            DepthXMLStreamReader dr = (DepthXMLStreamReader)source;
            XMLStreamReader reader = dr.getReader();

            // allows the XML Stream Reader to adjust it's behaviour based on the state of the unmarshaller
            if (reader instanceof UnmarshallerAwareXMLReader) {
                ((UnmarshallerAwareXMLReader) reader).setUnmarshaller(u);
            }

            if (u.getSchema() != null) {
                //validating, but we may need more namespaces
                reader = findExtraNamespaces(reader);
            }
            obj = unmarshalWithClass ? u.unmarshal(reader, clazz) : u
                .unmarshal(dr.getReader());
        } else if (source instanceof XMLStreamReader) {
            XMLStreamReader reader = (XMLStreamReader)source;

            // allows the XML Stream Reader to adjust it's behaviour based on the state of the unmarshaller
            if (reader instanceof UnmarshallerAwareXMLReader) {
                ((UnmarshallerAwareXMLReader) reader).setUnmarshaller(u);
            }

            if (u.getSchema() != null) {
                //validating, but we may need more namespaces
                reader = findExtraNamespaces(reader);
            }
            obj = unmarshalWithClass ? u.unmarshal(reader, clazz) : u
                .unmarshal(reader);
        } else if (source instanceof XMLEventReader) {
            // allows the XML Event Reader to adjust it's behaviour based on the state of the unmarshaller
            if (source instanceof UnmarshallerAwareXMLReader) {
                ((UnmarshallerAwareXMLReader) source).setUnmarshaller(u);
            }

            obj = unmarshalWithClass ? u.unmarshal((XMLEventReader)source, clazz) : u
                .unmarshal((XMLEventReader)source);
        } else if (source == null) {
            throw new Fault(new Message("UNKNOWN_SOURCE", LOG, "null"));
        } else {
            throw new Fault(new Message("UNKNOWN_SOURCE", LOG, source.getClass().getName()));
        }
        return unwrap ? getElementValue(obj) : obj;
    }
    public static Object unmarshall(final Unmarshaller u,
                                    final Object source,
                                    final QName elName,
                                    final Class<?> clazz,
                                    final boolean unwrap) {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    return doUnmarshal(u, source, elName, clazz, unwrap);
                }
            });
        } catch (PrivilegedActionException e) {
            Exception ex = e.getException();
            if (ex instanceof Fault) {
                throw (Fault)ex;
            }
            if (ex instanceof javax.xml.bind.UnmarshalException) {
                javax.xml.bind.UnmarshalException unmarshalEx = (javax.xml.bind.UnmarshalException)ex;
                if (unmarshalEx.getLinkedException() != null) {
                    throw new Fault(new Message("UNMARSHAL_ERROR", LOG,
                                            unmarshalEx.getLinkedException().getMessage()), ex);
                }
                throw new Fault(new Message("UNMARSHAL_ERROR", LOG,
                                            unmarshalEx.getMessage()), ex);
            }
            throw new Fault(new Message("UNMARSHAL_ERROR", LOG, ex.getMessage()), ex);
        }
    }

    private static XMLStreamReader findExtraNamespaces(XMLStreamReader source) {
        //due to a deficiency in the Stax API, there isn't a way to get all
        //the namespace prefixes that are "valid" at this point.  Thus, JAXB
        //cannot set all the prefixes into the validator (which also doesn't allow
        //setting a NSContext, just allows declaring of prefixes) so resolving
        //prefixes and such will fail if they were declared on any of the parent
        //elements.
        //
        //We'll use some reflection to grab the known namespaces from woodstox
        //or the xerces parser and fake extra namespace decls on the root elements.
        //slight performance penalty, but there already is a penalty if you are validating
        //anyway.

        NamespaceContext c = source.getNamespaceContext();
        final Map<String, String> nsMap = new TreeMap<>();
        try {
            if (c instanceof W3CNamespaceContext) {
                Element element = ((W3CNamespaceContext)c).getElement();
                while (element != null) {
                    NamedNodeMap namedNodeMap = element.getAttributes();
                    for (int i = 0; i < namedNodeMap.getLength(); i++) {
                        Attr attr = (Attr)namedNodeMap.item(i);
                        if (attr.getPrefix() != null && "xmlns".equals(attr.getPrefix())) {
                            nsMap.put(attr.getLocalName(), attr.getValue());
                        }
                    }
                    element = (Element)element.getParentNode();
                }
            } else {
                try {
                    //Woodstox version
                    c = (NamespaceContext)c.getClass().getMethod("createNonTransientNsContext",
                                                                 Location.class)
                        .invoke(c, new Object[1]);
                } catch (Throwable t) {
                    //ignore
                }
                Field f = ReflectionUtil.getDeclaredField(c.getClass(), "mNamespaces");
                ReflectionUtil.setAccessible(f);
                String[] ns = (String[])f.get(c);
                for (int x = 0; x < ns.length; x += 2) {
                    if (ns[x] == null) {
                        nsMap.put("", ns[x + 1]);
                    } else {
                        nsMap.put(ns[x], ns[x + 1]);
                    }
                }
            }
        } catch (Throwable t) {
            //internal JDK/xerces version
            try {
                Field f = ReflectionUtil.getDeclaredField(c.getClass(), "fNamespaceContext");
                ReflectionUtil.setAccessible(f);
                Object c2 = f.get(c);
                Enumeration<?> enm = (Enumeration<?>)c2.getClass().getMethod("getAllPrefixes").invoke(c2);
                while (enm.hasMoreElements()) {
                    String s = (String)enm.nextElement();
                    if (s == null) {
                        nsMap.put("", c.getNamespaceURI(null));
                    } else {
                        nsMap.put(s, c.getNamespaceURI(s));
                    }
                }
            } catch (Throwable t2) {
                //ignore
            }
        }
        if (!nsMap.isEmpty()) {
            for (int x = 0; x < source.getNamespaceCount(); x++) {
                String pfx = source.getNamespacePrefix(x);
                if (pfx == null) {
                    nsMap.remove("");
                } else {
                    nsMap.remove(pfx);
                }
            }
            if (!nsMap.isEmpty()) {
                @SuppressWarnings("unchecked")
                final Map.Entry<String, String>[] namespaces
                    = nsMap.entrySet().toArray(new Map.Entry[nsMap.size()]);
                //OK. we have extra namespaces.  We'll need to wrapper the reader
                //with a new one that will fake extra namespace events
                source = new DepthXMLStreamReader(source) {
                    public int getNamespaceCount() {
                        if (getDepth() == 0 && isStartElement()) {
                            return super.getNamespaceCount() + nsMap.size();
                        }
                        return super.getNamespaceCount();
                    }

                    public String getNamespacePrefix(int arg0) {
                        if (getDepth() == 0 && isStartElement()) {
                            int i = super.getNamespaceCount();
                            if (arg0 >= i) {
                                arg0 -= i;
                                return namespaces[arg0].getKey();
                            }
                        }
                        return super.getNamespacePrefix(arg0);
                    }

                    public String getNamespaceURI(int arg0) {
                        if (getDepth() == 0 && isStartElement()) {
                            int i = super.getNamespaceCount();
                            if (arg0 >= i) {
                                arg0 -= i;
                                return namespaces[arg0].getValue();
                            }
                        }
                        return super.getNamespaceURI(arg0);
                    }

                };
            }
        }

        return source;
    }

    public static Object getElementValue(Object obj) {
        if (null == obj) {
            return null;
        }

        if (obj instanceof JAXBElement) {
            return ((JAXBElement<?>)obj).getValue();
        }
        return obj;
    }

    public static Class<?> getClassFromType(Type t) {
        if (t instanceof Class) {
            return (Class<?>)t;
        } else if (t instanceof GenericArrayType) {
            GenericArrayType g = (GenericArrayType)t;
            return Array.newInstance(getClassFromType(g.getGenericComponentType()), 0).getClass();
        } else if (t instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType)t;
            return getClassFromType(p.getRawType());
        }
        // TypeVariable and WildCardType are not handled as it is unlikely such
        // Types will
        // JAXB Code Generated.
        assert false;
        throw new IllegalArgumentException("Cannot get Class object from unknown Type");
    }

    public static List<Object> unmarshallArray(Unmarshaller u, Object source,
                                               QName elName, Class<?> clazz,
                                               List<Object> ret) {
        try {
            XMLStreamReader reader;
            if (source instanceof XMLStreamReader) {
                reader = (XMLStreamReader)source;
            } else if (source instanceof Element) {
                reader = StaxUtils.createXMLStreamReader((Element)source);
            } else {
                throw new Fault(new Message("UNKNOWN_SOURCE", LOG, source.getClass().getName()));
            }
            while (reader.getName().equals(elName)) {
                JAXBElement<?> type = u.unmarshal(reader, clazz);
                if (type != null) {
                    ret.add(type.getValue());
                }
                while (reader.getEventType() != XMLStreamConstants.START_ELEMENT
                    && reader.getEventType() != XMLStreamConstants.END_ELEMENT) {
                    reader.nextTag();
                }
            }
            return ret;
        } catch (Fault ex) {
            throw ex;
        } catch (javax.xml.bind.MarshalException ex) {
            throw new Fault(new Message("UNMARSHAL_ERROR", LOG, ex.getLinkedException()
                .getMessage()), ex);
        } catch (Exception ex) {
            throw new Fault(new Message("UNMARSHAL_ERROR", LOG, ex.getMessage()), ex);
        }
    }
}

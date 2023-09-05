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

package org.apache.cxf.jaxrs.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.Nullable;
import org.apache.cxf.jaxrs.ext.xml.XMLInstruction;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.jaxrs.ext.xml.XSISchemaLocation;
import org.apache.cxf.jaxrs.ext.xml.XSLTTransform;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXBUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.DepthExceededStaxException;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.transform.TransformUtils;

@Produces({"application/xml", "application/*+xml", "text/xml" })
@Consumes({"application/xml", "application/*+xml", "text/xml" })
@Provider
public class JAXBElementProvider<T> extends AbstractJAXBProvider<T>  {
    private static final String XML_PI_START = "<?xml version=\"1.0\" encoding=\"";
    private static final String XML_PI_PROPERTY_RI = "com.sun.xml.bind.xmlHeaders";
    private static final String XML_PI_PROPERTY_RI_INT = "com.sun.xml.internal.bind.xmlHeaders";

    private static final List<String> MARSHALLER_PROPERTIES =
        Arrays.asList(new String[] {Marshaller.JAXB_ENCODING,
                                    Marshaller.JAXB_FORMATTED_OUTPUT,
                                    Marshaller.JAXB_FRAGMENT,
                                    Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION,
                                    Marshaller.JAXB_SCHEMA_LOCATION,
                                    NS_MAPPER_PROPERTY_RI,
                                    NS_MAPPER_PROPERTY_RI_INT,
                                    XML_PI_PROPERTY_RI,
                                    XML_PI_PROPERTY_RI_INT});

    private Map<String, Object> mProperties = Collections.emptyMap();
    private Map<String, String> nsPrefixes = Collections.emptyMap();
    private String xmlResourceOffset = "";
    private String xmlPiPropertyName;

    public JAXBElementProvider() {

    }

    protected boolean objectFactoryOrIndexAvailable(Class<?> type) {
        return !Document.class.isAssignableFrom(type) && super.objectFactoryOrIndexAvailable(type);
    }

    public void setXmlResourceOffset(String value) {
        xmlResourceOffset = value;
    }

    public void setNamespacePrefixes(Map<String, String> prefixes) {
        nsPrefixes = prefixes;
    }

    protected void setXmlPiProperty(Marshaller ms, String value) throws Exception {
        if (xmlPiPropertyName != null) {
            setMarshallerProp(ms, value, xmlPiPropertyName, null);
        } else {
            setMarshallerProp(ms, value, XML_PI_PROPERTY_RI, XML_PI_PROPERTY_RI_INT);
        }
    }

    @Override
    protected boolean canBeReadAsJaxbElement(Class<?> type) {
        return super.canBeReadAsJaxbElement(type)
            && type != XMLSource.class && !Source.class.isAssignableFrom(type);
    }

    @Context
    public void setMessageContext(MessageContext mc) {
        super.setContext(mc);
    }

    public void setMarshallerProperties(Map<String, Object> marshallProperties) {
        mProperties = marshallProperties;
    }

    public void setSchemaLocation(String schemaLocation) {
        mProperties.put(Marshaller.JAXB_SCHEMA_LOCATION, schemaLocation);
    }

    @FFDCIgnore({JAXBException.class, DepthExceededStaxException.class, WebApplicationException.class,
                 Exception.class, XMLStreamException.class})
    public T readFrom(Class<T> type, Type genericType, Annotation[] anns, MediaType mt,
        MultivaluedMap<String, String> headers, InputStream is)
        throws IOException {
        if (isPayloadEmpty(headers)) {
            if (AnnotationUtils.getAnnotation(anns, Nullable.class) != null) {
                return null;
            }
            reportEmptyContentLength();
        }

        XMLStreamReader reader = null;
        Unmarshaller unmarshaller = null;
        try {

            boolean isCollection = InjectionUtils.isSupportedCollectionOrArray(type);
            Class<?> theGenericType = isCollection ? InjectionUtils.getActualType(genericType) : type;
            Class<?> theType = getActualType(theGenericType, genericType, anns);

            unmarshaller = createUnmarshaller(theType, genericType, isCollection);
            addAttachmentUnmarshaller(unmarshaller);
            Object response = null;
            if (JAXBElement.class.isAssignableFrom(type)
                || !isCollection && (unmarshalAsJaxbElement
                || jaxbElementClassMap != null && jaxbElementClassMap.containsKey(theType.getName()))) {
                reader = getStreamReader(is, type, mt);
                reader = TransformUtils.createNewReaderIfNeeded(reader, is);
                if (JAXBElement.class.isAssignableFrom(type) && type == theType) {
                    response = unmarshaller.unmarshal(reader);
                } else {
                    response = unmarshaller.unmarshal(reader, theType);
                }
            } else {
                response = doUnmarshal(unmarshaller, type, is, anns, mt);
            }
            if (response instanceof JAXBElement && !JAXBElement.class.isAssignableFrom(type)) {
                response = ((JAXBElement<?>)response).getValue();
            }
            if (isCollection) {
                response = ((CollectionWrapper)response).getCollectionOrArray(
                                 unmarshaller, theType, type, genericType,
                                 org.apache.cxf.jaxrs.utils.JAXBUtils.getAdapter(theGenericType, anns));
            } else {
                response = checkAdapter(response, type, anns, false);
            }
            return type.cast(response);

        } catch (JAXBException e) {
            handleJAXBException(e, true);
        } catch (DepthExceededStaxException e) {
            throw ExceptionUtils.toWebApplicationException(null, JAXRSUtils.toResponse(413));
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            LOG.warning(ExceptionUtils.getStackTrace(e));
            throw ExceptionUtils.toBadRequestException(e, null);
        } finally {
            try {
                StaxUtils.close(reader);
            } catch (XMLStreamException e) {
                // Ignore
            }
            JAXBUtils.closeUnmarshaller(unmarshaller);
        }
        // unreachable
        return null;
    }

    @FFDCIgnore({JAXBException.class, XMLStreamException.class})
    protected Object doUnmarshal(Unmarshaller unmarshaller, Class<?> type, InputStream is,
                                 Annotation[] anns, MediaType mt)
        throws JAXBException {
        XMLStreamReader reader = getStreamReader(is, type, mt);
        if (reader != null) {
            try {
                return unmarshalFromReader(unmarshaller, reader, anns, mt);
            } catch (JAXBException e) {
                throw e;
            } finally {
                try {
                    StaxUtils.close(reader);
                } catch (XMLStreamException e) {
                    // Ignore
                }
            }
        }
        return unmarshalFromInputStream(unmarshaller, is, anns, mt);
    }

    @FFDCIgnore({XMLStreamException.class})
    protected XMLStreamReader getStreamReader(InputStream is, Class<?> type, MediaType mt) {
        MessageContext mc = getContext();
        XMLStreamReader reader = mc != null ? mc.getContent(XMLStreamReader.class) : null;
        if (reader == null && mc != null) {
            XMLInputFactory factory = (XMLInputFactory)mc.get(XMLInputFactory.class.getName());
            if (factory != null) {
                try {
                    reader = factory.createXMLStreamReader(is);
                } catch (XMLStreamException e) {
                    throw ExceptionUtils.toInternalServerErrorException(
                        new RuntimeException("Can not create XMLStreamReader", e), null);
                }
            }
        }

        if (reader == null && is == null) {
            reader = getStreamHandlerFromCurrentMessage(XMLStreamReader.class);
        }

        reader = createTransformReaderIfNeeded(reader, is);
        reader = createDepthReaderIfNeeded(reader, is);
        if (InjectionUtils.isSupportedCollectionOrArray(type)) {
            return new JAXBCollectionWrapperReader(TransformUtils.createNewReaderIfNeeded(reader, is));
        }
        return reader;

    }

    @FFDCIgnore({PrivilegedActionException.class, XMLStreamException.class})
    protected Object unmarshalFromInputStream(Unmarshaller unmarshaller, InputStream is,
                                              Annotation[] anns, MediaType mt)
        throws JAXBException {
        // Try to create the read before unmarshalling the stream
        XMLStreamReader xmlReader = null;
        try {
            if (is == null) {
                Reader reader = getStreamHandlerFromCurrentMessage(Reader.class);
                if (reader == null) {
                    LOG.severe("No InputStream, Reader, or XMLStreamReader is available");
                    throw ExceptionUtils.toInternalServerErrorException(null, null);
                }
                xmlReader = StaxUtils.createXMLStreamReader(reader);
            } else {
                xmlReader = StaxUtils.createXMLStreamReader(is);
            }
            configureReaderRestrictions(xmlReader);
            //Liberty change start 
            //return unmarshaller.unmarshal(xmlReader);
            final XMLStreamReader fReader = xmlReader;
            return System.getSecurityManager() == null ? unmarshaller.unmarshal(xmlReader) :
                AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                    return unmarshaller.unmarshal(fReader);
            });
        } catch (PrivilegedActionException pae) {
            Throwable t = pae.getCause();
            if (t instanceof JAXBException) {
                throw (JAXBException) t;
            }
            throw new RuntimeException(t);
            //Liberty change end
        } finally {
            try {
                StaxUtils.close(xmlReader);
            } catch (XMLStreamException e) {
                // Ignore
            }
        }
    }

    protected Object unmarshalFromReader(Unmarshaller unmarshaller, XMLStreamReader reader,
                                         Annotation[] anns, MediaType mt)
        throws JAXBException {
        return unmarshaller.unmarshal(reader);
    }

    @FFDCIgnore({JAXBException.class, WebApplicationException.class, Exception.class})
    public void writeTo(T obj, Class<?> cls, Type genericType, Annotation[] anns,
        MediaType m, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        try {
            String encoding = HttpUtils.getSetEncoding(m, headers, StandardCharsets.UTF_8.name());
            if (InjectionUtils.isSupportedCollectionOrArray(cls)) {
                marshalCollection(cls, obj, genericType, encoding, os, m, anns);
            } else {
                Object actualObject = checkAdapter(obj, cls, anns, true);
                Class<?> actualClass = obj != actualObject || cls.isInterface()
                    ? actualObject.getClass() : cls;
                marshal(actualObject, actualClass, genericType, encoding, os, m, anns);
            }
        } catch (JAXBException e) {
            handleJAXBException(e, false);
        }  catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            LOG.warning(ExceptionUtils.getStackTrace(e));
            throw ExceptionUtils.toInternalServerErrorException(e, null);
        }
    }

    protected void marshalCollection(Class<?> originalCls, Object collection,
                                     Type genericType, String enc, OutputStream os,
                                     MediaType m, Annotation[] anns)
        throws Exception {

        Class<?> actualClass = InjectionUtils.getActualType(genericType);
        actualClass = getActualType(actualClass, genericType, anns);

        Collection<?> c = originalCls.isArray() ? Arrays.asList((Object[]) collection)
                                             : (Collection<?>) collection;

        Iterator<?> it = c.iterator();

        Object firstObj = it.hasNext() ? it.next() : null;

        QName qname = null;
        if (firstObj instanceof JAXBElement) {
            JAXBElement<?> el = (JAXBElement<?>)firstObj;
            qname = el.getName();
            actualClass = el.getDeclaredType();
        } else {
            qname = getCollectionWrapperQName(actualClass, genericType, firstObj, true);
        }
        if (qname == null) {
            String message = new org.apache.cxf.common.i18n.Message("NO_COLLECTION_ROOT",
                                                                    BUNDLE).toString();
            throw new WebApplicationException(Response.serverError()
                                              .entity(message).build());
        }

        StringBuilder pi = new StringBuilder();
        pi.append(XML_PI_START + (enc == null ? StandardCharsets.UTF_8.name() : enc) + "\"?>");
        os.write(pi.toString().getBytes());
        String startTag = null;
        String endTag = null;

        if (qname.getNamespaceURI().length() > 0) {
            String prefix = nsPrefixes.get(qname.getNamespaceURI());
            if (prefix == null) {
                prefix = "ns1";
            }
            startTag = "<" + prefix + ":" + qname.getLocalPart() + " xmlns:" + prefix + "=\""
                + qname.getNamespaceURI() + "\">";
            endTag = "</" + prefix + ":" + qname.getLocalPart() + ">";
        } else {
            startTag = "<" + qname.getLocalPart() + ">";
            endTag = "</" + qname.getLocalPart() + ">";
        }
        os.write(startTag.getBytes());
        if (firstObj != null) {
            XmlJavaTypeAdapter adapter =
                org.apache.cxf.jaxrs.utils.JAXBUtils.getAdapter(firstObj.getClass(), anns);
            marshalCollectionMember(JAXBUtils.useAdapter(firstObj, adapter, true),
                                    actualClass, genericType, enc, os, anns, m,
                                    qname.getNamespaceURI());
            while (it.hasNext()) {
                marshalCollectionMember(JAXBUtils.useAdapter(it.next(), adapter, true), actualClass,
                                        genericType, enc, os, anns, m,
                                        qname.getNamespaceURI());
            }
        }
        os.write(endTag.getBytes());
    }
    //CHECKSTYLE:OFF
    protected void marshalCollectionMember(Object obj,
                                           Class<?> cls,
                                           Type genericType,
                                           String enc,
                                           OutputStream os,
                                           Annotation[] anns,
                                           MediaType mt,
                                           String ns) throws Exception {
    //CHECKSTYLE:ON
        if (!(obj instanceof JAXBElement)) {
            obj = convertToJaxbElementIfNeeded(obj, cls, genericType);
        }

        if (obj instanceof JAXBElement && cls != JAXBElement.class) {
            cls = JAXBElement.class;
        }

        Marshaller ms = createMarshaller(obj, cls, genericType, enc);
        ms.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        if (ns.length() > 0) {
            Map<String, String> map = new HashMap<>();
            // set the default just in case
            if (!nsPrefixes.containsKey(ns)) {
                map.put(ns, "ns1");
            }
            map.putAll(nsPrefixes);
            setNamespaceMapper(ms, map);
        }
        marshal(obj, cls, genericType, enc, os, anns, mt, ms);
    }

    protected void marshal(Object obj, Class<?> cls, Type genericType,
                           String enc, OutputStream os, MediaType mt) throws Exception {
        marshal(obj, cls, genericType, enc, os, mt, new Annotation[]{});
    }

    protected void marshal(Object obj, Class<?> cls, Type genericType,
                           String enc, OutputStream os, MediaType mt,
                           Annotation[] anns) throws Exception {
        obj = convertToJaxbElementIfNeeded(obj, cls, genericType);
        if (obj instanceof JAXBElement && cls != JAXBElement.class) {
            cls = JAXBElement.class;
        }

        Marshaller ms = createMarshaller(obj, cls, genericType, enc);
        if (!nsPrefixes.isEmpty()) {
            setNamespaceMapper(ms, nsPrefixes);
        }
        addAttachmentMarshaller(ms);
        processXmlAnnotations(ms, mt, anns);
        marshal(obj, cls, genericType, enc, os, anns, mt, ms);
    }

    private void processXmlAnnotations(Marshaller ms, MediaType mt, Annotation[] anns) throws Exception {
        if (anns == null) {
            return;
        }
        for (Annotation ann : anns) {
            if (ann.annotationType() == XMLInstruction.class) {
                addProcessingInstructions(ms, (XMLInstruction)ann);
            } else if (ann.annotationType() == XSISchemaLocation.class) {
                addSchemaLocation(ms, (XSISchemaLocation)ann);
            } else if (ann.annotationType() == XSLTTransform.class) {
                addXslProcessingInstruction(ms, mt, (XSLTTransform)ann);
            }
        }
    }

    private void addProcessingInstructions(Marshaller ms, XMLInstruction pi) throws Exception {
        String value = pi.value();
        int ind = value.indexOf("href='");
        if (ind > 0) {
            String relRef = value.substring(ind + 6);
            relRef = relRef.substring(0, relRef.length() - 3).trim();
            if (relRef.endsWith("'")) {
                relRef = relRef.substring(0, relRef.length() - 1);
            }
            String absRef = resolveXMLResourceURI(relRef);
            value = value.substring(0, ind + 6) + absRef + "'?>";
        }
        setXmlPiProperty(ms, value);
    }

    private void addXslProcessingInstruction(Marshaller ms, MediaType mt, XSLTTransform ann)
        throws Exception {
        if (ann.type() == XSLTTransform.TransformType.CLIENT
            || ann.type() == XSLTTransform.TransformType.BOTH && ann.mediaTypes().length > 0) {
            for (String s : ann.mediaTypes()) {
                if (mt.isCompatible(JAXRSUtils.toMediaType(s))) {
                    return;
                }
            }
            String absRef = resolveXMLResourceURI(ann.value());
            String xslPi = "<?xml-stylesheet type=\"text/xsl\" href=\"" + absRef + "\"?>";
            setXmlPiProperty(ms, xslPi);
        }
    }

    private void addSchemaLocation(Marshaller ms, XSISchemaLocation sl) throws Exception {
        String value = sl.resolve() ? resolveXMLResourceURI(sl.value()) : sl.value();
        String propName = !sl.noNamespace()
            ? Marshaller.JAXB_SCHEMA_LOCATION : Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION;
        ms.setProperty(propName, value);
    }

    protected String resolveXMLResourceURI(String path) {
        MessageContext mc = getContext();
        if (mc != null) {
            String httpBasePath = (String)mc.get("http.base.path");
            UriBuilder builder = null;
            if (httpBasePath != null) {
                builder = UriBuilder.fromPath(httpBasePath);
            } else {
                builder = mc.getUriInfo().getBaseUriBuilder();
            }
            return builder.path(path).path(xmlResourceOffset).build().toString();
        }
        return path;
    }



    protected void addAttachmentMarshaller(Marshaller ms) {
        Collection<Attachment> attachments = getAttachments(true);
        if (attachments != null) {
            Object value = getContext().getContextualProperty(Message.MTOM_THRESHOLD);
            Integer threshold = value != null ? Integer.valueOf(value.toString()) : Integer.valueOf(0);
            ms.setAttachmentMarshaller(new JAXBAttachmentMarshaller(
                attachments, threshold));
        }
    }

    protected void addAttachmentUnmarshaller(Unmarshaller um) {
        Collection<Attachment> attachments = getAttachments(false);
        if (attachments != null) {
            um.setAttachmentUnmarshaller(new JAXBAttachmentUnmarshaller(
                attachments));
        }
    }

    private Collection<Attachment> getAttachments(boolean write) {
        MessageContext mc = getContext();
        if (mc != null) {
            // TODO: there has to be a better fix
            String propertyName = write ? "WRITE-" + Message.ATTACHMENTS : Message.ATTACHMENTS;
            return CastUtils.cast((Collection<?>)mc.get(propertyName));
        }
        return null;
    }
    //CHECKSTYLE:OFF
    protected final void marshal(Object obj, Class<?> cls, Type genericType,
                           String enc, OutputStream os,
                           Annotation[] anns, MediaType mt, Marshaller ms)
        throws Exception {
    //CHECKSTYLE:ON
        for (Map.Entry<String, Object> entry : mProperties.entrySet()) {
            ms.setProperty(entry.getKey(), entry.getValue());
        }
        MessageContext mc = getContext();
        if (mc != null) {
            // check Marshaller properties which might've been set earlier on,
            // they'll overwrite statically configured ones
            for (String key : MARSHALLER_PROPERTIES) {
                Object value = mc.get(key);
                if (value != null) {
                    ms.setProperty(key, value);
                }
            }

        }
        XMLStreamWriter writer = getStreamWriter(obj, os, mt);
        if (writer != null) {
            if (os == null) {
                ms.setProperty(Marshaller.JAXB_FRAGMENT, true);
            } else if (mc != null) {
                if (mc.getContent(XMLStreamWriter.class) != null) {
                    ms.setProperty(Marshaller.JAXB_FRAGMENT, true);
                }
                mc.put(XMLStreamWriter.class.getName(), writer);
            }
            marshalToWriter(ms, obj, writer, anns, mt);
            if (mc != null) {
                writer.writeEndDocument();
            }
        } else {
            marshalToOutputStream(ms, obj, os, anns, mt);
        }
    }

    @FFDCIgnore({XMLStreamException.class})
    protected XMLStreamWriter getStreamWriter(Object obj, OutputStream os, MediaType mt) {
        XMLStreamWriter writer = null;
        MessageContext mc = getContext();
        if (mc != null) {
            writer = mc.getContent(XMLStreamWriter.class);
            if (writer == null) {
                XMLOutputFactory factory = (XMLOutputFactory)mc.get(XMLOutputFactory.class.getName());
                if (factory != null) {
                    try {
                        writer = factory.createXMLStreamWriter(os);
                    } catch (XMLStreamException e) {
                        throw ExceptionUtils.toInternalServerErrorException(
                            new RuntimeException("Cant' create XMLStreamWriter", e), null);
                    }
                }
            }
            if (writer == null && getEnableStreaming()) {
                writer = StaxUtils.createXMLStreamWriter(os);
            }
        }

        if (writer == null && os == null) {
            writer = getStreamHandlerFromCurrentMessage(XMLStreamWriter.class);
        }
        return createTransformWriterIfNeeded(writer, os, true);
    }

    protected void marshalToOutputStream(Marshaller ms, Object obj, OutputStream os,
                                         Annotation[] anns, MediaType mt)
        throws Exception {
        org.apache.cxf.common.jaxb.JAXBUtils.setMinimumEscapeHandler(ms);
        if (os == null) {
            Writer writer = getStreamHandlerFromCurrentMessage(Writer.class);
            if (writer == null) {
                LOG.severe("No OutputStream, Writer, or XMLStreamWriter is available");
                throw ExceptionUtils.toInternalServerErrorException(null, null);
            }
            ms.marshal(obj, writer);
            writer.flush();
        } else {
            ms.marshal(obj, os);
        }
    }

    protected void marshalToWriter(Marshaller ms, Object obj, XMLStreamWriter writer,
                                   Annotation[] anns, MediaType mt)
        throws Exception {
        org.apache.cxf.common.jaxb.JAXBUtils.setNoEscapeHandler(ms);
        ms.marshal(obj, writer);
    }

    public void setXmlPiPropertyName(String xmlPiPropertyName) {
        this.xmlPiPropertyName = xmlPiPropertyName;
    }

}

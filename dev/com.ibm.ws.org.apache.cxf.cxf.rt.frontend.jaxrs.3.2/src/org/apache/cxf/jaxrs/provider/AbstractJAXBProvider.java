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
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;

import org.w3c.dom.Element;

import org.xml.sax.helpers.DefaultHandler;

import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.staxutils.DepthExceededStaxException;
import org.apache.cxf.staxutils.DepthRestrictingStreamReader;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.DocumentDepthProperties;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.transform.TransformUtils;

public abstract class AbstractJAXBProvider<T> extends AbstractConfigurableProvider
    implements MessageBodyReader<T>, MessageBodyWriter<T> {

    protected static final String NS_MAPPER_PROPERTY_RI = "com.sun.xml.bind.namespacePrefixMapper";
    protected static final String NS_MAPPER_PROPERTY_RI_INT = "com.sun.xml.internal.bind.namespacePrefixMapper";
    private static final String JAXB_DEFAULT_NAMESPACE = "##default";
    private static final String JAXB_DEFAULT_NAME = "##default";
    private static final Set<Class<?>> UNSUPPORTED_CLASSES = 
        new HashSet<Class<?>>(Arrays.asList(InputStream.class,
                                            OutputStream.class,
                                            StreamingOutput.class));
    protected Set<Class<?>> collectionContextClasses = new HashSet<Class<?>>();

    protected Map<String, String> jaxbElementClassMap = Collections.emptyMap();
    protected boolean unmarshalAsJaxbElement;
    protected boolean marshalAsJaxbElement;
    protected boolean xmlTypeAsJaxbElementOnly;

    protected Map<String, String> outElementsMap;
    protected Map<String, String> outAppendMap;
    protected List<String> outDropElements;
    protected List<String> inDropElements;
    protected Map<String, String> inElementsMap;
    protected Map<String, String> inAppendMap;
    protected Map<String, JAXBContext> packageContexts = new HashMap<>();
    protected Map<Class<?>, JAXBContext> classContexts = new HashMap<>();
    private boolean attributesToElements;

    private MessageContext mc;

    private Schema schema;
    private String catalogLocation;
    private Map<String, SchemaHandler> schemaHandlers;

    private String collectionWrapperName;
    private Map<String, String> collectionWrapperMap;
    private List<String> jaxbElementClassNames;
    private boolean xmlRootAsJaxbElement;
    private Map<String, Object> cProperties;
    private Map<String, Object> uProperties;

    private boolean skipJaxbChecks;
    private boolean singleJaxbContext;
    private boolean useSingleContextForPackages;

    private Class<?>[] extraClass;

    private boolean validateInputIfPossible = true;
    private boolean validateOutputIfPossible;
    private boolean validateBeforeWrite;
    private ValidationEventHandler eventHandler;
    private Unmarshaller.Listener unmarshallerListener;
    private Marshaller.Listener marshallerListener;
    private DocumentDepthProperties depthProperties;
    private String namespaceMapperPropertyName;

    public void setXmlRootAsJaxbElement(boolean xmlRootAsJaxbElement) {
        this.xmlRootAsJaxbElement = xmlRootAsJaxbElement;
    }


    protected void setNamespaceMapper(Marshaller ms,
                                      Map<String, String> map) throws Exception {
        Object nsMapper = JAXBUtils.setNamespaceMapper(map, ms);
        if (nsMapper != null && namespaceMapperPropertyName != null) {
            setMarshallerProp(ms, nsMapper, namespaceMapperPropertyName, null);
        }
    }

    protected static void setMarshallerProp(Marshaller ms, Object value,
                                          String name1, String name2) throws Exception {
        try {
            ms.setProperty(name1, value);
        } catch (PropertyException ex) {
            if (name2 != null) {
                ms.setProperty(name2, value);
            } else {
                throw ex;
            }
        }

    }

    public void setValidationHandler(ValidationEventHandler handler) {
        eventHandler = handler;
    }

    public void setSingleJaxbContext(boolean useSingleContext) {
        singleJaxbContext = useSingleContext;
    }

    public void setUseSingleContextForPackages(boolean use) {
        useSingleContextForPackages = use;
    }

    public void setExtraClass(Class<?>[] userExtraClass) {
        extraClass = userExtraClass;
    }

    @Override
    public void init(List<ClassResourceInfo> cris) {
        if (singleJaxbContext) {
            JAXBContext context = null;
            Set<Class<?>> allTypes = null;
            if (cris != null) {
                allTypes = new HashSet<Class<?>>(ResourceUtils.getAllRequestResponseTypes(cris, true)
                    .getAllTypes().keySet());
                context = ResourceUtils.createJaxbContext(allTypes, extraClass, cProperties);
            } else if (extraClass != null) {
                allTypes = new HashSet<Class<?>>(Arrays.asList(extraClass));
                context = ResourceUtils.createJaxbContext(allTypes, null, cProperties);
            }

            if (context != null) {
                for (Class<?> cls : allTypes) {
                    if (useSingleContextForPackages) {
                        packageContexts.put(PackageUtils.getPackageName(cls), context);
                    } else {
                        classContexts.put(cls, context);
                    }
                }
            }
        }
        if (cris != null) {
            List<String> schemaLocs = new LinkedList<String>();
            SchemaValidation sv = null;
            for (ClassResourceInfo cri : cris) {
                sv = cri.getServiceClass().getAnnotation(SchemaValidation.class);
                if (sv != null && sv.schemas() != null && sv.type() != SchemaValidation.SchemaValidationType.NONE) {
                    for (String s : sv.schemas()) {
                        String theSchema = s;
                        if (!theSchema.startsWith("classpath:")) {
                            theSchema = "classpath:" + theSchema;
                        }
                        schemaLocs.add(theSchema);
                    }
                }
            }
            if (!schemaLocs.isEmpty()) {
                this.setSchemaLocations(schemaLocs);
                if (cris.isEmpty() && schema != null && sv != null) {
                    SchemaValidation.SchemaValidationType type = sv.type();
                    if (type == SchemaValidation.SchemaValidationType.OUT) {
                        validateInputIfPossible = false;
                        validateOutputIfPossible = true;
                    } else if (type == SchemaValidation.SchemaValidationType.BOTH) {
                        validateOutputIfPossible = true;
                    }
                }
            }
        }
    }

    public void setContextProperties(Map<String, Object> contextProperties) {
        cProperties = contextProperties;
    }

    public void setUnmarshallerProperties(Map<String, Object> unmarshalProperties) {
        uProperties = unmarshalProperties;
    }

    public void setUnmarshallAsJaxbElement(boolean value) {
        unmarshalAsJaxbElement = value;
    }

    public void setMarshallAsJaxbElement(boolean value) {
        marshalAsJaxbElement = value;
    }

    public void setXmlTypeAsJaxbElementOnly(boolean value) {
        this.xmlTypeAsJaxbElementOnly = value;
    }

    public void setJaxbElementClassNames(List<String> names) {
        jaxbElementClassNames = names;
    }

    public void setJaxbElementClassMap(Map<String, String> map) {
        jaxbElementClassMap = map;
    }

    protected <X> X getStreamHandlerFromCurrentMessage(Class<X> staxCls) {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        if (m != null) {
            return staxCls.cast(m.getContent(staxCls));
        }
        return null;
    }

    protected boolean isXmlRoot(Class<?> cls) {
        return cls.getAnnotation(XmlRootElement.class) != null;
    }

    protected boolean isXmlType(Class<?> cls) {
        return cls.getAnnotation(XmlType.class) != null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Object convertToJaxbElementIfNeeded(Object obj, Class<?> cls, Type genericType)
        throws Exception {

        Class<?> jaxbElementCls = jaxbElementClassNames == null ? null : getJaxbElementClass(cls);
        boolean asJaxbElement = jaxbElementCls != null;
        if (!asJaxbElement && isXmlRoot(cls) && !xmlRootAsJaxbElement) {
            return obj;
        }
        if (jaxbElementCls == null) {
            jaxbElementCls = cls;
        }
        QName name = null;
        String expandedName = jaxbElementClassMap.get(jaxbElementCls.getName());
        if (expandedName != null) {
            name = JAXRSUtils.convertStringToQName(expandedName);
        } else if (marshalAsJaxbElement || asJaxbElement) {
            name = getJaxbQName(jaxbElementCls, genericType, obj, false);
        }
        return name != null ? new JAXBElement<Object>(name, (Class)jaxbElementCls, null, obj) : obj;
    }

    protected Class<?> getJaxbElementClass(Class<?> cls) {
        if (cls == Object.class) {
            return null;
        }
        if (jaxbElementClassNames.contains(cls.getName())) {
            return cls;
        }
        return getJaxbElementClass(cls.getSuperclass());

    }

    public void setCollectionWrapperName(String wName) {
        collectionWrapperName = wName;
    }

    public void setCollectionWrapperMap(Map<String, String> map) {
        collectionWrapperMap = map;
    }

    protected void setContext(MessageContext context) {
        mc = context;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {

        if (InjectionUtils.isSupportedCollectionOrArray(type)) {
            type = InjectionUtils.getActualType(genericType);
            if (type == null) {
                return false;
            }
        }
        return marshalAsJaxbElement && (!xmlTypeAsJaxbElementOnly || isXmlType(type))
            || isSupported(type, genericType, anns);
    }
    public void writeTo(T t, Type genericType, Annotation annotations[],
                 MediaType mediaType,
                 MultivaluedMap<String, Object> httpHeaders,
                 OutputStream entityStream) throws IOException, WebApplicationException {
        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>)t.getClass();
        writeTo(t, type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }

    protected JAXBContext getCollectionContext(Class<?> type) throws JAXBException {
        synchronized (collectionContextClasses) {
            if (!collectionContextClasses.contains(type)) {
                collectionContextClasses.add(CollectionWrapper.class);
                collectionContextClasses.add(type);
            }
            return JAXBContext.newInstance(
                collectionContextClasses.toArray(new Class[0]), cProperties);
        }
    }

    protected QName getCollectionWrapperQName(Class<?> cls, Type type, Object object, boolean pluralName)
        throws Exception {
        String name = getCollectionWrapperName(cls);
        if (name == null) {
            return getJaxbQName(cls, type, object, pluralName);
        }

        return JAXRSUtils.convertStringToQName(name);
    }

    private String getCollectionWrapperName(Class<?> cls) {
        if (collectionWrapperName != null) {
            return collectionWrapperName;
        }
        if (collectionWrapperMap != null) {
            return collectionWrapperMap.get(cls.getName());
        }

        return null;
    }

    protected QName getJaxbQName(Class<?> cls, Type type, Object object, boolean pluralName)
        throws Exception {

        if (cls == JAXBElement.class) {
            return object != null ? ((JAXBElement<?>)object).getName() : null;
        }

        XmlRootElement root = cls.getAnnotation(XmlRootElement.class);
        if (root != null) {
            return getQNameFromNamespaceAndName(root.namespace(), root.name(), cls, pluralName);
        } else if (isXmlType(cls)) {
            XmlType xmlType = cls.getAnnotation(XmlType.class);
            return getQNameFromNamespaceAndName(xmlType.namespace(), xmlType.name(), cls, pluralName);
        } else {
            return new QName(getPackageNamespace(cls), cls.getSimpleName());
        }
    }

    private static QName getQNameFromNamespaceAndName(String ns, String localName, Class<?> cls, boolean plural) {
        String name = getLocalName(localName, cls.getSimpleName(), plural);
        String namespace = getNamespace(ns);
        if ("".equals(namespace)) {
            namespace = getPackageNamespace(cls);
        }
        return new QName(namespace, name);
    }

    private static String getLocalName(String name, String clsName, boolean pluralName) {
        if (JAXB_DEFAULT_NAME.equals(name)) {
            name = clsName;
            if (name.length() > 1) {
                name = name.substring(0, 1).toLowerCase() + name.substring(1);
            } else {
                name = name.toLowerCase();
            }
        }
        if (pluralName) {
            name += 's';
        }
        return name;
    }

    private static String getPackageNamespace(Class<?> cls) {
        String packageNs = JAXBUtils.getPackageNamespace(cls);
        return packageNs != null ? getNamespace(packageNs) : "";
    }

    private static String getNamespace(String namespace) {
        if (JAXB_DEFAULT_NAMESPACE.equals(namespace)) {
            return "";
        }
        return namespace;
    }

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        if (InjectionUtils.isSupportedCollectionOrArray(type)) {
            type = InjectionUtils.getActualType(genericType);
            if (type == null) {
                return false;
            }
        }
        return canBeReadAsJaxbElement(type) || isSupported(type, genericType, anns);
    }

    protected boolean canBeReadAsJaxbElement(Class<?> type) {
        return unmarshalAsJaxbElement && type != Response.class;
    }

    public void setSchemaLocations(List<String> locations) {
        schema = SchemaHandler.createSchema(locations, catalogLocation, getBus());
    }

    public void setCatalogLocation(String name) {
        this.catalogLocation = name;
    }

    public void setSchemaHandler(SchemaHandler handler) {
        setSchema(handler.getSchema());
    }

    public void setSchemaHandlers(Map<String, SchemaHandler> handlers) {
        schemaHandlers = handlers;
    }

    protected void setSchema(Schema s) {
        schema = s;
    }

    public long getSize(T o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return -1;
    }

    protected MessageContext getContext() {
        return mc;
    }

    @SuppressWarnings("unchecked")
    public JAXBContext getJAXBContext(Class<?> type, Type genericType) throws JAXBException {
        if (mc != null) {
            ContextResolver<JAXBContext> resolver =
                mc.getResolver(ContextResolver.class, JAXBContext.class);
            if (resolver != null) {
                JAXBContext customContext = resolver.getContext(type);
                if (customContext != null) {
                    return customContext;
                }
            }
        }

        synchronized (classContexts) {
            JAXBContext context = classContexts.get(type);
            if (context != null) {
                return context;
            }
        }

        JAXBContext context = getPackageContext(type, genericType);

        return context != null ? context : getClassContext(type, genericType);
    }
    public JAXBContext getClassContext(Class<?> type) throws JAXBException {
        return getClassContext(type, type);
    }
    protected JAXBContext getClassContext(Class<?> type, Type genericType) throws JAXBException {
        synchronized (classContexts) {
            JAXBContext context = classContexts.get(type);
            if (context == null) {
                Class<?>[] classes = null;
                if (extraClass != null) {
                    classes = new Class[extraClass.length + 1];
                    classes[0] = type;
                    System.arraycopy(extraClass, 0, classes, 1, extraClass.length);
                } else {
                    classes = new Class[] {type};
                }

                context = JAXBContext.newInstance(classes, cProperties);
                classContexts.put(type, context);
            }
            return context;
        }
    }
    public JAXBContext getPackageContext(Class<?> type) {
        return getPackageContext(type, type);
    }
    protected JAXBContext getPackageContext(final Class<?> type, Type genericType) {
        if (type == null || type == JAXBElement.class) {
            return null;
        }
        synchronized (packageContexts) {
            String packageName = PackageUtils.getPackageName(type);
            JAXBContext context = packageContexts.get(packageName);
            if (context == null) {
                try {
                    final ClassLoader loader = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) 
                        () -> {
                            return type.getClassLoader();
                        });
                    if (loader != null && objectFactoryOrIndexAvailable(type)) {

                        String contextName = packageName;
                        if (extraClass != null) {
                            StringBuilder sb = new StringBuilder(contextName);
                            for (Class<?> extra : extraClass) {
                                String extraPackage = PackageUtils.getPackageName(extra);
                                if (!extraPackage.equals(packageName)) {
                                    sb.append(":").append(extraPackage);
                                }
                            }
                            contextName = sb.toString();
                        }

                        context = JAXBContext.newInstance(contextName, loader, cProperties);
                        packageContexts.put(packageName, context);
                    }
                } catch (JAXBException ex) {
                    LOG.fine("Error creating a JAXBContext using ObjectFactory : "
                                + ex.getMessage());
                    return null;
                }
            }
            return context;
        }
    }

    protected boolean isSupported(Class<?> type, Type genericType, Annotation[] anns) {
        if (jaxbElementClassMap != null && jaxbElementClassMap.containsKey(type.getName())
            || isSkipJaxbChecks()) {
            return true;
        }
        if (UNSUPPORTED_CLASSES.contains(type)) {
            return false;
        }
        return isXmlRoot(type)
            || JAXBElement.class.isAssignableFrom(type)
            || objectFactoryOrIndexAvailable(type)
            || (type != genericType && objectFactoryForType(genericType))
            || org.apache.cxf.jaxrs.utils.JAXBUtils.getAdapter(type, anns) != null;

    }

    protected boolean objectFactoryOrIndexAvailable(Class<?> type) {
        return type.getResource("ObjectFactory.class") != null
               || type.getResource("jaxb.index") != null;
    }

    private boolean objectFactoryForType(Type genericType) {
        return objectFactoryOrIndexAvailable(InjectionUtils.getActualType(genericType));
    }

    protected Unmarshaller createUnmarshaller(Class<?> cls, Type genericType)
        throws JAXBException {
        return createUnmarshaller(cls, genericType, false);
    }

    protected Unmarshaller createUnmarshaller(Class<?> cls, Type genericType, boolean isCollection)
        throws JAXBException {
        JAXBContext context = isCollection ? getCollectionContext(cls)
                                           : getJAXBContext(cls, genericType);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        if (validateInputIfPossible) {
            Schema theSchema = getSchema(cls);
            if (theSchema != null) {
                unmarshaller.setSchema(theSchema);
            }
        }
        if (eventHandler != null) {
            unmarshaller.setEventHandler(eventHandler);
        }
        if (unmarshallerListener != null) {
            unmarshaller.setListener(unmarshallerListener);
        }
        if (uProperties != null) {
            for (Map.Entry<String, Object> entry : uProperties.entrySet()) {
                unmarshaller.setProperty(entry.getKey(), entry.getValue());
            }
        }
        return unmarshaller;
    }

    protected Marshaller createMarshaller(Object obj, Class<?> cls, Type genericType, String enc)
        throws JAXBException {

        Class<?> objClazz = JAXBElement.class.isAssignableFrom(cls)
                            ? ((JAXBElement<?>)obj).getDeclaredType() : cls;

        JAXBContext context = getJAXBContext(objClazz, genericType);
        Marshaller marshaller = context.createMarshaller();
        if (enc != null) {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, enc);
        }
        if (marshallerListener != null) {
            marshaller.setListener(marshallerListener);
        }
        validateObjectIfNeeded(marshaller, cls, obj);
        return marshaller;
    }

    protected void validateObjectIfNeeded(Marshaller marshaller, Class<?> cls, Object obj)
        throws JAXBException {
        if (validateOutputIfPossible) {
            Schema theSchema = getSchema(cls);
            if (theSchema != null) {
                marshaller.setEventHandler(eventHandler);
                marshaller.setSchema(theSchema);
                if (validateBeforeWrite) {
                    marshaller.marshal(obj, new DefaultHandler());
                    marshaller.setSchema(null);
                }
            }
        }
    }

    protected Class<?> getActualType(Class<?> type, Type genericType, Annotation[] anns) {
        Class<?> theType = null;
        if (JAXBElement.class.isAssignableFrom(type)) {
            theType = InjectionUtils.getActualType(genericType);
        } else {
            theType = type;
        }
        XmlJavaTypeAdapter adapter = org.apache.cxf.jaxrs.utils.JAXBUtils.getAdapter(theType, anns);
        theType = org.apache.cxf.jaxrs.utils.JAXBUtils.getTypeFromAdapter(adapter, theType, false);

        return theType;
    }

    protected static Object checkAdapter(Object obj, Class<?> cls, Annotation[] anns, boolean marshal) {
        XmlJavaTypeAdapter adapter = org.apache.cxf.jaxrs.utils.JAXBUtils.getAdapter(cls, anns);
        return org.apache.cxf.jaxrs.utils.JAXBUtils.useAdapter(obj, adapter, marshal);
    }

    protected Schema getSchema() {
        return getSchema(null);
    }

    protected Schema getSchema(Class<?> cls) {
        // deal with the typical default case first
        if (schema == null && schemaHandlers == null) {
            return null;
        }

        if (schema != null) {
            return schema;
        }
        SchemaHandler handler = schemaHandlers.get(cls.getName());
        return handler != null ? handler.getSchema() : null;
    }


    public void clearContexts() {
        classContexts.clear();
        packageContexts.clear();
    }

    //TODO: move these methods into the dedicated utility class
    protected static StringBuilder handleExceptionStart(Exception e) {
        LOG.warning(ExceptionUtils.getStackTrace(e));
        StringBuilder sb = new StringBuilder();
        if (e.getMessage() != null) {
            sb.append(e.getMessage()).append(". ");
        }
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            sb.append(e.getCause().getMessage()).append(". ");
        }
        return sb;
    }

    protected static void handleExceptionEnd(Throwable t, String message, boolean read) {
        Response.Status status = read
            ? Response.Status.BAD_REQUEST : Response.Status.INTERNAL_SERVER_ERROR;
        Response r = JAXRSUtils.toResponseBuilder(status)
            .type(MediaType.TEXT_PLAIN).entity(message).build();
        WebApplicationException ex = read ? ExceptionUtils.toBadRequestException(t, r)
            : ExceptionUtils.toInternalServerErrorException(t, r);
        throw ex;
    }

    protected void handleJAXBException(JAXBException e, boolean read) {
        StringBuilder sb = handleExceptionStart(e);
        Throwable linked = e.getLinkedException();
        if (linked != null && linked.getMessage() != null) {
            Throwable cause = linked;
            while (read && cause != null) {
                if (cause instanceof XMLStreamException && cause.getMessage().startsWith("Maximum Number")) {
                    throw ExceptionUtils.toWebApplicationException(null, JAXRSUtils.toResponse(413));
                }
                if (cause instanceof DepthExceededStaxException) {
                    throw ExceptionUtils.toWebApplicationException(null, JAXRSUtils.toResponse(413));
                }
                cause = cause.getCause();
            }
            String msg = linked.getMessage();
            if (sb.lastIndexOf(msg) == -1) {
                sb.append(msg).append(". ");
            }
        }
        Throwable t = linked != null ? linked : e.getCause() != null ? e.getCause() : e;
        String message = new org.apache.cxf.common.i18n.Message("JAXB_EXCEPTION",
                             BUNDLE, sb.toString()).toString();
        handleExceptionEnd(t, message, read);
    }

    protected void handleXMLStreamException(XMLStreamException e, boolean read) {
        StringBuilder sb = handleExceptionStart(e);
        handleExceptionEnd(e, sb.toString(), read);
    }

    public void setOutTransformElements(Map<String, String> outElements) {
        this.outElementsMap = outElements;
    }

    public void setInAppendElements(Map<String, String> inElements) {
        this.inAppendMap = inElements;
    }

    public void setInTransformElements(Map<String, String> inElements) {
        this.inElementsMap = inElements;
    }

    public void setOutAppendElements(Map<String, String> map) {
        this.outAppendMap = map;
    }

    public void setOutDropElements(List<String> dropElementsSet) {
        this.outDropElements = dropElementsSet;
    }

    public void setInDropElements(List<String> dropElementsSet) {
        this.inDropElements = dropElementsSet;
    }

    public void setAttributesToElements(boolean value) {
        this.attributesToElements = value;
    }

    public void setSkipJaxbChecks(boolean skipJaxbChecks) {
        this.skipJaxbChecks = skipJaxbChecks;
    }

    public boolean isSkipJaxbChecks() {
        return skipJaxbChecks;
    }

    protected XMLStreamWriter createTransformWriterIfNeeded(XMLStreamWriter writer,
                                                            OutputStream os,
                                                            boolean dropAtXmlLevel) {
        return TransformUtils.createTransformWriterIfNeeded(writer, os,
                                                      outElementsMap,
                                                      dropAtXmlLevel ? outDropElements : null,
                                                      outAppendMap,
                                                      attributesToElements,
                                                      null);
    }

    protected XMLStreamReader createTransformReaderIfNeeded(XMLStreamReader reader, InputStream is) {
        return TransformUtils.createTransformReaderIfNeeded(reader, is,
                                                            inDropElements,
                                                            inElementsMap,
                                                            inAppendMap,
                                                            true);
    }

    protected XMLStreamReader createDepthReaderIfNeeded(XMLStreamReader reader, InputStream is) {
        DocumentDepthProperties props = getDepthProperties();
        if (props != null && props.isEffective()) {
            reader = TransformUtils.createNewReaderIfNeeded(reader, is);
            reader = new DepthRestrictingStreamReader(reader, props);
        } else if (reader != null) {
            reader = configureReaderRestrictions(reader);
        }
        return reader;
    }

    protected XMLStreamReader configureReaderRestrictions(XMLStreamReader reader) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        if (message != null) {
            try {
                return StaxUtils.configureReader(reader, message);
            } catch (XMLStreamException ex) {
                throw ExceptionUtils.toInternalServerErrorException(ex, null);
            }
        }
        return reader;
    }

    protected DocumentDepthProperties getDepthProperties() {
        return depthProperties;
    }

    public void setValidateBeforeWrite(boolean validateBeforeWrite) {
        this.validateBeforeWrite = validateBeforeWrite;
    }

    public void setValidateOutput(boolean validateOutput) {
        this.validateOutputIfPossible = validateOutput;
    }
    public void setValidateInput(boolean validateInput) {
        this.validateInputIfPossible = validateInput;
    }

    public void setDepthProperties(DocumentDepthProperties depthProperties) {
        this.depthProperties = depthProperties;
    }

    public void setUnmarshallerListener(Unmarshaller.Listener unmarshallerListener) {
        this.unmarshallerListener = unmarshallerListener;
    }

    public void setMarshallerListener(Marshaller.Listener marshallerListener) {
        this.marshallerListener = marshallerListener;
    }

    public void setNamespaceMapperPropertyName(String namespaceMapperProperty) {
        this.namespaceMapperPropertyName = namespaceMapperProperty;
    }

    @XmlRootElement
    protected static class CollectionWrapper {

        @XmlAnyElement(lax = true)
        private List<?> l;

        public void setList(List<?> list) {
            l = list;
        }

        public List<?> getList() {
            if (l == null) {
                l = new ArrayList<>();
            }
            return l;
        }

        @SuppressWarnings("unchecked")
        public <T> Object getCollectionOrArray(Unmarshaller unm,
                                               Class<T> type,
                                               Class<?> collectionType,
                                               Type genericType,
                                               XmlJavaTypeAdapter adapter) throws JAXBException {
            List<?> theList = getList();
            boolean adapterChecked = false;
            if (!theList.isEmpty()) {
                Object first = theList.get(0);

                if (first instanceof Element) {
                    List<Object> newList = new ArrayList<>(theList.size());
                    for (Object o : theList) {
                        newList.add(unm.unmarshal((Element)o, type));
                    }
                    theList = newList;
                }

                first = theList.get(0);
                Type[] types = InjectionUtils.getActualTypes(genericType);
                boolean isJaxbElement = types != null && types.length > 0
                    && InjectionUtils.getRawType(types[0]) == JAXBElement.class;

                if (first instanceof JAXBElement && !isJaxbElement && !JAXBElement.class.isAssignableFrom(type)) {
                    adapterChecked = true;
                    List<Object> newList = new ArrayList<>(theList.size());
                    for (Object o : theList) {
                        newList.add(org.apache.cxf.jaxrs.utils.JAXBUtils.useAdapter(
                                        ((JAXBElement<?>)o).getValue(), adapter, false));
                    }
                    theList = newList;
                } else if (!(first instanceof JAXBElement) && isJaxbElement) {
                    List<Object> newList = new ArrayList<>(theList.size());
                    XmlRootElement root = type.getAnnotation(XmlRootElement.class);
                    QName qname = getQNameFromNamespaceAndName(root.namespace(), root.name(), type, false);
                    @SuppressWarnings("rawtypes")
                    Class theType = type;
                    for (Object o : theList) {
                        newList.add(new JAXBElement<Object>(qname, theType, null, o));
                    }
                    theList = newList;
                }
            }
            if (collectionType.isArray()) {
                T[] values = (T[])Array.newInstance(type, theList.size());
                for (int i = 0; i < theList.size(); i++) {
                    values[i] = (T)org.apache.cxf.jaxrs.utils.JAXBUtils.useAdapter(
                                       theList.get(i), adapter, false);
                }
                return values;
            }
            if (!adapterChecked && adapter != null) {
                List<Object> newList = new ArrayList<>(theList.size());
                for (Object o : theList) {
                    newList.add(org.apache.cxf.jaxrs.utils.JAXBUtils.useAdapter(o, adapter, false));
                }
                theList = newList;
            }
            if (collectionType == Set.class) {
                return new HashSet<>(theList);
            }
            return theList;
        }

    }

    protected static class JAXBCollectionWrapperReader extends DepthXMLStreamReader {

        private boolean firstName;
        private boolean firstNs;

        public JAXBCollectionWrapperReader(XMLStreamReader reader) {
            super(reader);
        }

        @Override
        public String getNamespaceURI() {
            if (!firstNs) {
                firstNs = true;
                return "";
            }
            return super.getNamespaceURI();
        }

        @Override
        public String getLocalName() {
            if (!firstName) {
                firstName = true;
                return "collectionWrapper";
            }

            return super.getLocalName();
        }

    }


}

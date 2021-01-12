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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.jaxb.JAXBContextCache.CachedContextAndSchemas;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.databinding.AbstractInterceptorProvidingDataBinding;
import org.apache.cxf.databinding.AbstractWrapperHelper;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.databinding.WrapperCapableDatabinding;
import org.apache.cxf.databinding.WrapperHelper;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxb.attachment.JAXBAttachmentSchemaValidationHack;
import org.apache.cxf.jaxb.io.DataReaderImpl;
import org.apache.cxf.jaxb.io.DataWriterImpl;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.addressing.ObjectFactory;

@NoJSR250Annotations
public class JAXBDataBinding extends AbstractInterceptorProvidingDataBinding
    implements WrapperCapableDatabinding, InterceptorProvider {

    public static final String READER_VALIDATION_EVENT_HANDLER = "jaxb-reader-validation-event-handler";
    public static final String VALIDATION_EVENT_HANDLER = "jaxb-validation-event-handler";
    public static final String SET_VALIDATION_EVENT_HANDLER = "set-jaxb-validation-event-handler";
    public static final String WRITER_VALIDATION_EVENT_HANDLER = "jaxb-writer-validation-event-handler";

    public static final String SCHEMA_RESOURCE = "SCHEMRESOURCE";
    public static final String MTOM_THRESHOLD = "org.apache.cxf.jaxb.mtomThreshold";

    public static final String UNWRAP_JAXB_ELEMENT = "unwrap.jaxb.element";

    public static final String USE_JAXB_BRIDGE = "use.jaxb.bridge";

    //public static final String JAXB_SCAN_PACKAGES = "jaxb.scanPackages"; Liberty change: removed field

    private static final Logger LOG = LogUtils.getLogger(JAXBDataBinding.class);

    private static final Class<?>[] SUPPORTED_READER_FORMATS = new Class<?>[] {Node.class,
                                                                               XMLEventReader.class,
                                                                               XMLStreamReader.class};
    private static final Class<?>[] SUPPORTED_WRITER_FORMATS = new Class<?>[] {OutputStream.class,
                                                                               Node.class,
                                                                               XMLEventWriter.class,
                                                                               XMLStreamWriter.class};

    private static final boolean ENABLE_MARSHALL_POOLING = true;
    private static final boolean ENABLE_UNMARSHALL_POOLING = true;
    private static final int MAX_LOAD_FACTOR = 50;

    private static class DelayedDOMResult extends DOMResult {
        private final URL resource;
        private final String publicId;
        DelayedDOMResult(URL url, String sysId, String pId) {
            super(null, sysId);
            resource = url;
            publicId = pId;
        }
        public synchronized Node getNode() {
            Node nd = super.getNode();
            if (nd == null) {
                try {
                    InputSource src = new InputSource(resource.openStream());
                    src.setSystemId(this.getSystemId());
                    src.setPublicId(publicId);
                    Document doc = StaxUtils.read(src);
                    setNode(doc);
                    nd = super.getNode();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            return nd;
        }
    }
    private static final Map<String, DOMResult> BUILT_IN_SCHEMAS = new HashMap<>();
    static {
        URIResolver resolver = new URIResolver();
        try {
            resolver.resolve("", "classpath:/schemas/wsdl/ws-addr-wsdl.xsd", JAXBDataBinding.class);
            if (resolver.isResolved()) {
                resolver.getInputStream().close();
                DOMResult dr = new DelayedDOMResult(resolver.getURL(),
                                                    "classpath:/schemas/wsdl/ws-addr-wsdl.xsd",
                                                    "http://www.w3.org/2005/02/addressing/wsdl");
                BUILT_IN_SCHEMAS.put("http://www.w3.org/2005/02/addressing/wsdl", dr);
                resolver.unresolve();
            }
        } catch (Exception e) {
            //IGNORE
        }
        try {
            resolver.resolve("", "classpath:/schemas/wsdl/ws-addr.xsd", JAXBDataBinding.class);
            if (resolver.isResolved()) {
                resolver.getInputStream().close();
                DOMResult dr = new DelayedDOMResult(resolver.getURL(),
                                                    "classpath:/schemas/wsdl/ws-addr.xsd",
                                                    "http://www.w3.org/2005/08/addressing");
                BUILT_IN_SCHEMAS.put("http://www.w3.org/2005/08/addressing", dr);
                resolver.unresolve();
            }
        } catch (Exception e) {
            //IGNORE
        }
        try {
            resolver.resolve("", "classpath:/schemas/wsdl/wsrm.xsd", JAXBDataBinding.class);
            if (resolver.isResolved()) {
                resolver.getInputStream().close();
                DOMResult dr = new DelayedDOMResult(resolver.getURL(),
                                                    "classpath:/schemas/wsdl/wsrm.xsd",
                                                    "http://schemas.xmlsoap.org/ws/2005/02/rm");
                BUILT_IN_SCHEMAS.put("http://schemas.xmlsoap.org/ws/2005/02/rm", dr);
                resolver.unresolve();
            }
        } catch (Exception e) {
            //IGNORE
        }
    }

    Class<?>[] extraClass;

    JAXBContext context;
    Set<Class<?>> contextClasses;
    Collection<Object> typeRefs = new ArrayList<>();

    Class<?> cls;

    private Map<String, Object> contextProperties = new HashMap<>();
    private List<XmlAdapter<?, ?>> adapters = new ArrayList<>();
    private Map<String, Object> marshallerProperties = new HashMap<>();
    private Map<String, Object> unmarshallerProperties = new HashMap<>();
    private Unmarshaller.Listener unmarshallerListener;
    private Marshaller.Listener marshallerListener;
    private ValidationEventHandler validationEventHandler;
    private Object escapeHandler;
    private Object noEscapeHandler;

    private boolean unwrapJAXBElement = true;
    //private boolean scanPackages = true; Liberty change: removed
    private boolean qualifiedSchemas;

    // Liberty change: below 2 lines are added
    private Deque<SoftReference<Marshaller>> marshallers = new LinkedBlockingDeque<SoftReference<Marshaller>>(MAX_LOAD_FACTOR);
    private Deque<SoftReference<Unmarshaller>> unmarshallers = new LinkedBlockingDeque<SoftReference<Unmarshaller>>(MAX_LOAD_FACTOR);
    // Liberty change: end

    public JAXBDataBinding() {
    }

    public JAXBDataBinding(boolean q) {
        this.qualifiedSchemas = q;
    }

    public JAXBDataBinding(Class<?>... classes) throws JAXBException {
        contextClasses = new LinkedHashSet<>(Arrays.asList(classes));
        setContext(createJAXBContext(contextClasses)); //NOPMD - specifically allow this
    }
    public JAXBDataBinding(boolean qualified, Map<String, Object> props) throws JAXBException {
        this(qualified);
        if (props != null && props.get("jaxb.additionalContextClasses") != null) {
            Object o = props.get("jaxb.additionalContextClasses");
            if (o instanceof Class) {
                o = new Class[] {(Class<?>)o};
            }
            extraClass = (Class[])o;
        }

        // the default for scan packages is true, so the jaxb scan packages
        // property must be explicitly set to false to disable it
        // Liberty change: 3 lines below are removed
        // if (PropertyUtils.isFalse(props, JAXB_SCAN_PACKAGES)) {
        //     scanPackages = false;
        // } Liberty change: end
    }

    public JAXBDataBinding(JAXBContext context) {
        this();
        setContext(context);
    }

    public JAXBContext getContext() {
        return context;
    }

    public final void setContext(JAXBContext ctx) {
        context = ctx;
        //create default MininumEscapeHandler
        escapeHandler = JAXBUtils.createMininumEscapeHandler(ctx.getClass());
        noEscapeHandler = JAXBUtils.createNoEscapeHandler(ctx.getClass());
    }

    public Object getEscapeHandler() {
        return escapeHandler;
    }

    public void setEscapeHandler(Object handler) {
        escapeHandler = handler;
    }

    public void applyEscapeHandler(boolean escape, Consumer<Object> consumer) {
        if (escape) {
            consumer.accept(escapeHandler);
        } else if (noEscapeHandler != null) {
            consumer.accept(noEscapeHandler);
        }
    }


    @SuppressWarnings("unchecked")
    public <T> DataWriter<T> createWriter(Class<T> c) {

        Integer mtomThresholdInt = Integer.valueOf(getMtomThreshold());
        if (c == XMLStreamWriter.class) {
            DataWriterImpl<XMLStreamWriter> r
                = new DataWriterImpl<>(this, true);
            r.setMtomThreshold(mtomThresholdInt);
            return (DataWriter<T>)r;
        } else if (c == OutputStream.class) {
            DataWriterImpl<OutputStream> r = new DataWriterImpl<>(this, false);
            r.setMtomThreshold(mtomThresholdInt);
            return (DataWriter<T>)r;
        } else if (c == XMLEventWriter.class) {
            DataWriterImpl<XMLEventWriter> r = new DataWriterImpl<>(this, true);
            r.setMtomThreshold(mtomThresholdInt);
            return (DataWriter<T>)r;
        } else if (c == Node.class) {
            DataWriterImpl<Node> r = new DataWriterImpl<>(this, false);
            r.setMtomThreshold(mtomThresholdInt);
            return (DataWriter<T>)r;
        }
        return null;
    }

    public Class<?>[] getSupportedWriterFormats() {
        return SUPPORTED_WRITER_FORMATS;
    }

    @SuppressWarnings("unchecked")
    public <T> DataReader<T> createReader(Class<T> c) {
        DataReader<T> dr = null;
        if (c == XMLStreamReader.class) {
            dr = (DataReader<T>)new DataReaderImpl<XMLStreamReader>(this, unwrapJAXBElement);
        } else if (c == XMLEventReader.class) {
            dr = (DataReader<T>)new DataReaderImpl<XMLEventReader>(this, unwrapJAXBElement);
        } else if (c == Node.class) {
            dr = (DataReader<T>)new DataReaderImpl<Node>(this, unwrapJAXBElement);
        }

        return dr;
    }

    public Class<?>[] getSupportedReaderFormats() {
        return SUPPORTED_READER_FORMATS;
    }

    @SuppressWarnings("unchecked")
    public synchronized void initialize(Service service) {

        inInterceptors.addIfAbsent(JAXBAttachmentSchemaValidationHack.INSTANCE);
        inFaultInterceptors.addIfAbsent(JAXBAttachmentSchemaValidationHack.INSTANCE);

        // context is already set, don't redo it
        if (context != null) {
            return;
        }

        contextClasses = new LinkedHashSet<>();

        for (ServiceInfo serviceInfo : service.getServiceInfos()) {
            //Liberty change: below line, this.getUnmarshallerProperties() is removed from JAXBContextInitializer comstructor as last parameter
            JAXBContextInitializer initializer= new JAXBContextInitializer(serviceInfo, contextClasses, typeRefs);
            initializer.walk();
            if (serviceInfo.getProperty("extra.class") != null) {
                Set<Class<?>> exClasses = serviceInfo.getProperty("extra.class", Set.class);
                contextClasses.addAll(exClasses);
            }

        }

        String tns = getNamespaceToUse(service);
        CachedContextAndSchemas cachedContextAndSchemas = null;
        JAXBContext ctx = null;
        try {
            cachedContextAndSchemas = createJAXBContextAndSchemas(contextClasses, tns);
        } catch (JAXBException e1) {
            throw new ServiceConstructionException(e1);
        }
        ctx = cachedContextAndSchemas.getContext();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "CREATED_JAXB_CONTEXT", new Object[] {ctx, contextClasses});
        }
        setContext(ctx);

        for (ServiceInfo serviceInfo : service.getServiceInfos()) {
            SchemaCollection col = serviceInfo.getXmlSchemaCollection();

            if (col.getXmlSchemas().length > 1) {
                // someone has already filled in the types
                // justCheckForJAXBAnnotations(serviceInfo); Liberty change: removed line
                continue;
            }

            boolean schemasFromCache = false;
            Collection<DOMSource> schemas = getSchemas();
            if (schemas == null || schemas.isEmpty()) {
                schemas = cachedContextAndSchemas.getSchemas();
                if (schemas != null) {
                    schemasFromCache = true;
                }
            } else {
                schemasFromCache = true;
            }
            Set<DOMSource> bi = new LinkedHashSet<>();
            if (schemas == null) {
                schemas = new LinkedHashSet<>();
                try {
                    for (DOMResult r : generateJaxbSchemas()) {
                        DOMSource src = new DOMSource(r.getNode(), r.getSystemId());
                        if (BUILT_IN_SCHEMAS.containsValue(r)) {
                            bi.add(src);
                        } else {
                            schemas.add(src);
                        }
                    }
                    //put any builtins at the end.   Anything that DOES import them
                    //will cause it to load automatically and we'll skip them later
                    schemas.addAll(bi);
                } catch (IOException e) {
                    throw new ServiceConstructionException("SCHEMA_GEN_EXC", LOG, e);
                }
            }
            for (DOMSource r : schemas) {
                if (bi.contains(r)) {
                    String ns = ((Document)r.getNode()).getDocumentElement().getAttribute("targetNamespace");
                    if (serviceInfo.getSchema(ns) != null) {
                        continue;
                    }
                }
                //StaxUtils.print(r.getNode());
                //System.out.println();
                addSchemaDocument(serviceInfo,
                                  col,
                                 (Document)r.getNode(),
                                  r.getSystemId());
            }

            JAXBSchemaInitializer schemaInit = new JAXBSchemaInitializer(serviceInfo, col, context,
                                                                         this.qualifiedSchemas, tns);
            schemaInit.walk();
            if (cachedContextAndSchemas != null && !schemasFromCache) {
                cachedContextAndSchemas.setSchemas(schemas);
            }
        }
    }
    // Liberty change: 2 methods below are removed
    // private void justCheckForJAXBAnnotations(ServiceInfo serviceInfo) {
    //     for (MessageInfo mi: serviceInfo.getMessages().values()) {
    //         for (MessagePartInfo mpi : mi.getMessageParts()) {
    //             checkForJAXBAnnotations(mpi, serviceInfo.getXmlSchemaCollection(), serviceInfo.getTargetNamespace());
    //         }
    //     }
    // }
    // private void checkForJAXBAnnotations(MessagePartInfo mpi, SchemaCollection schemaCollection, String ns) {
    //     Annotation[] anns = (Annotation[])mpi.getProperty("parameter.annotations");
    //     JAXBContextProxy ctx = JAXBUtils.createJAXBContextProxy(context, schemaCollection, ns);
    //     XmlJavaTypeAdapter jta = JAXBSchemaInitializer.findFromTypeAdapter(ctx, mpi.getTypeClass(), anns);
    //     if (jta != null) {
    //         JAXBBeanInfo jtaBeanInfo = JAXBSchemaInitializer.findFromTypeAdapter(ctx, jta.value());
    //         JAXBBeanInfo beanInfo = JAXBSchemaInitializer.getBeanInfo(ctx, mpi.getTypeClass());
    //         if (jtaBeanInfo != beanInfo) {
    //             mpi.setProperty("parameter.annotations", anns);
    //             mpi.setProperty("honor.jaxb.annotations", Boolean.TRUE);
    //         }
    //     }
    // }// Liberty change: end

    private String getNamespaceToUse(Service service) {
        if ("true".equals(service.get("org.apache.cxf.databinding.namespace"))) {
            return null;
        }
        String tns = null;
        if (service.getServiceInfos().size() > 0) {
            tns = service.getServiceInfos().get(0).getInterface().getName().getNamespaceURI();
        } else {
            tns = service.getName().getNamespaceURI();
        }
        return tns;
    }

    public void setExtraClass(Class<?>[] userExtraClass) {
        extraClass = userExtraClass;
    }

    public Class<?>[] getExtraClass() {
        return extraClass;
    }

    // default access for tests.
    List<DOMResult> generateJaxbSchemas() throws IOException {
        return JAXBUtils.generateJaxbSchemas(context, BUILT_IN_SCHEMAS);
    }

    public JAXBContext createJAXBContext(Set<Class<?>> classes) throws JAXBException {
        return createJAXBContext(classes, null);
    }

    public JAXBContext createJAXBContext(Set<Class<?>> classes, String defaultNs) throws JAXBException {
        return createJAXBContextAndSchemas(classes, defaultNs).getContext();
    }

    public CachedContextAndSchemas createJAXBContextAndSchemas(Set<Class<?>> classes,
                                                               String defaultNs)
        throws JAXBException {
        //add user extra class into jaxb context
        if (extraClass != null && extraClass.length > 0) {
            for (Class<?> clz : extraClass) {
                classes.add(clz);
            }
        }
        //if (scanPackages) { // Liberty change: removed if clause
            JAXBContextCache.scanPackages(classes);
        //}
        addWsAddressingTypes(classes);

        return JAXBContextCache.getCachedContextAndSchemas(classes, defaultNs,
                                                          contextProperties,
                                                          typeRefs, true);
    }


    private void addWsAddressingTypes(Set<Class<?>> classes) {
        if (classes.contains(ObjectFactory.class)) {
            // ws-addressing is used, lets add the specific types
            try {
                classes.add(Class.forName("org.apache.cxf.ws.addressing.wsdl.ObjectFactory"));
                classes.add(Class.forName("org.apache.cxf.ws.addressing.wsdl.AttributedQNameType"));
                classes.add(Class.forName("org.apache.cxf.ws.addressing.wsdl.ServiceNameType"));
            } catch (ClassNotFoundException unused) {
                // REVISIT - ignorable if WS-ADDRESSING not available?
                // maybe add a way to allow interceptors to add stuff to the
                // context?
            }
        }
    }

    public Set<Class<?>> getContextClasses() {
        return Collections.unmodifiableSet(this.contextClasses);
    }

    /**
     * Return a map of properties. These properties are passed to
     * JAXBContext.newInstance when this object creates a context.
     *
     * @return the map of JAXB context properties.
     */
    public Map<String, Object> getContextProperties() {
        return contextProperties;
    }

    /**
     * Set a map of JAXB context properties. These properties are passed to
     * JAXBContext.newInstance when this object creates a context. Note that if
     * you create a JAXB context elsewhere, you will not respect these
     * properties unless you handle it manually.
     *
     * @param contextProperties map of properties.
     */
    public void setContextProperties(Map<String, Object> contextProperties) {
        this.contextProperties = contextProperties;
    }

    public List<XmlAdapter<?, ?>> getConfiguredXmlAdapters() {
        return adapters;
    }

    public void setConfiguredXmlAdapters(List<XmlAdapter<?, ?>> adpters) {
        this.adapters = adpters;
    }

    /**
     * Return a map of properties. These properties are set into the JAXB
     * Marshaller (via Marshaller.setProperty(...) when the marshaller is
     * created.
     *
     * @return the map of JAXB marshaller properties.
     */
    public Map<String, Object> getMarshallerProperties() {
        return marshallerProperties;
    }

    /**
     * Set a map of JAXB marshaller properties. These properties are set into
     * the JAXB Marshaller (via Marshaller.setProperty(...) when the marshaller
     * is created.
     *
     * @param marshallerProperties map of properties.
     */
    public void setMarshallerProperties(Map<String, Object> marshallerProperties) {
        this.marshallerProperties = marshallerProperties;
    }


    /**
     * Return a map of properties. These properties are set into the JAXB
     * Unmarshaller (via Unmarshaller.setProperty(...) when the unmarshaller is
     * created.
     *
     * @return the map of JAXB unmarshaller properties.
     */
    public Map<String, Object> getUnmarshallerProperties() {
        return unmarshallerProperties;
    }

    /**
     * Set a map of JAXB unmarshaller properties. These properties are set into
     * the JAXB Unmarshaller (via Unmarshaller.setProperty(...) when the unmarshaller
     * is created.
     *
     * @param unmarshallerProperties map of properties.
     */
    public void setUnmarshallerProperties(Map<String, Object> unmarshallerProperties) {
        this.unmarshallerProperties = unmarshallerProperties;
    }

    /**
     * Returns the Unmarshaller.Listener that will be registered on the Unmarshallers
     * @return
     */
    public Unmarshaller.Listener getUnmarshallerListener() {
        return unmarshallerListener;
    }

    /**
     * Sets the Unmarshaller.Listener that will be registered on the Unmarshallers
     * @param unmarshallerListener
     */
    public void setUnmarshallerListener(Unmarshaller.Listener unmarshallerListener) {
        this.unmarshallerListener = unmarshallerListener;
    }
    /**
     * Returns the Marshaller.Listener that will be registered on the Marshallers
     * @return
     */
    public Marshaller.Listener getMarshallerListener() {
        return marshallerListener;
    }

    /**
     * Sets the Marshaller.Listener that will be registered on the Marshallers
     * @param marshallerListener
     */
    public void setMarshallerListener(Marshaller.Listener marshallerListener) {
        this.marshallerListener = marshallerListener;
    }


    public ValidationEventHandler getValidationEventHandler() {
        return validationEventHandler;
    }

    public void setValidationEventHandler(ValidationEventHandler validationEventHandler) {
        this.validationEventHandler = validationEventHandler;
    }


    public boolean isUnwrapJAXBElement() {
        return unwrapJAXBElement;
    }

    public void setUnwrapJAXBElement(boolean unwrapJAXBElement) {
        this.unwrapJAXBElement = unwrapJAXBElement;
    }

    public WrapperHelper createWrapperHelper(Class<?> wrapperType, QName wrapperName, List<String> partNames,
                                             List<String> elTypeNames, List<Class<?>> partClasses) {
        List<Method> getMethods = new ArrayList<>(partNames.size());
        List<Method> setMethods = new ArrayList<>(partNames.size());
        List<Method> jaxbMethods = new ArrayList<>(partNames.size());
        List<Field> fields = new ArrayList<>(partNames.size());

        Method[] allMethods = wrapperType.getMethods();
        String packageName = PackageUtils.getPackageName(wrapperType);

        //if wrappertype class is generated by ASM, getPackage() always return null
        if (wrapperType.getPackage() != null) {
            packageName = wrapperType.getPackage().getName();
        }

        String objectFactoryClassName = packageName + ".ObjectFactory";

        Object objectFactory = null;
        try {
            objectFactory = wrapperType.getClassLoader().loadClass(objectFactoryClassName).newInstance();
        } catch (Exception e) {
            //ignore, probably won't need it
        }
        Method[] allOFMethods;
        if (objectFactory != null) {
            allOFMethods = objectFactory.getClass().getMethods();
        } else {
            allOFMethods = new Method[0];
        }

        for (int x = 0; x < partNames.size(); x++) {
            String partName = partNames.get(x);
            if (partName == null) {
                getMethods.add(null);
                setMethods.add(null);
                fields.add(null);
                jaxbMethods.add(null);
                continue;
            }

            String elementType = elTypeNames.get(x);

            String getAccessor = JAXBUtils.nameToIdentifier(partName, JAXBUtils.IdentifierType.GETTER);
            String setAccessor = JAXBUtils.nameToIdentifier(partName, JAXBUtils.IdentifierType.SETTER);
            Method getMethod = null;
            Method setMethod = null;
            Class<?> valueClass = wrapperType;

            try {
                getMethod = valueClass.getMethod(getAccessor, AbstractWrapperHelper.NO_CLASSES);
            } catch (NoSuchMethodException ex) {
                //ignore for now
            }

            Field elField = getElField(partName, valueClass);
            if (getMethod == null
                && elementType != null
                && "boolean".equals(elementType.toLowerCase())
                && (elField == null
                    || (!Collection.class.isAssignableFrom(elField.getType())
                    && !elField.getType().isArray()))) {

                try {
                    String newAcc = getAccessor.replaceFirst("get", "is");
                    getMethod = wrapperType.getMethod(newAcc, AbstractWrapperHelper.NO_CLASSES);
                } catch (NoSuchMethodException ex) {
                    //ignore for now
                }
            }
            if (getMethod == null
                && "return".equals(partName)) {
                //RI generated code uses this
                try {
                    getMethod = valueClass.getMethod("get_return", AbstractWrapperHelper.NO_CLASSES);
                } catch (NoSuchMethodException ex) {
                    try {
                        getMethod = valueClass.getMethod("is_return",
                                                          new Class[0]);
                    } catch (NoSuchMethodException ex2) {
                        //ignore for now
                    }
                }
            }
            if (getMethod == null && elField != null) {
                getAccessor = JAXBUtils.nameToIdentifier(elField.getName(), JAXBUtils.IdentifierType.GETTER);
                setAccessor = JAXBUtils.nameToIdentifier(elField.getName(), JAXBUtils.IdentifierType.SETTER);
                try {
                    getMethod = valueClass.getMethod(getAccessor, AbstractWrapperHelper.NO_CLASSES);
                } catch (NoSuchMethodException ex) {
                    //ignore for now
                }
            }
            String setAccessor2 = setAccessor;
            if ("return".equals(partName)) {
                //some versions of jaxb map "return" to "set_return" instead of "setReturn"
                setAccessor2 = "set_return";
            }

            for (Method method : allMethods) {
                if (method.getParameterTypes() != null && method.getParameterTypes().length == 1
                    && (setAccessor.equals(method.getName())
                        || setAccessor2.equals(method.getName()))) {
                    setMethod = method;
                    break;
                }
            }

            getMethods.add(getMethod);
            setMethods.add(setMethod);
            if (setMethod != null
                && JAXBElement.class.isAssignableFrom(setMethod.getParameterTypes()[0])) {

                Type t = setMethod.getGenericParameterTypes()[0];
                Class<?> pcls = null;
                if (t instanceof ParameterizedType) {
                    t = ((ParameterizedType)t).getActualTypeArguments()[0];
                }
                if (t instanceof Class) {
                    pcls = (Class<?>)t;
                }

                String methodName = "create" + wrapperType.getSimpleName()
                    + setMethod.getName().substring(3);

                for (Method m : allOFMethods) {
                    if (m.getName().equals(methodName)
                        && m.getParameterTypes().length == 1
                        && (pcls == null
                            || pcls.equals(m.getParameterTypes()[0]))) {
                        jaxbMethods.add(m);
                    }
                }
            } else {
                jaxbMethods.add(null);
            }

            if (elField != null) {
                // JAXB Type get XmlElement Annotation
                XmlElement el = elField.getAnnotation(XmlElement.class);
                if (el != null
                    && (partName.equals(el.name())
                        || "##default".equals(el.name()))) {
                    ReflectionUtil.setAccessible(elField);
                    fields.add(elField);
                } else {
                    if (getMethod == null && setMethod == null) {
                        if (el != null) {
                            LOG.warning("Could not create accessor for property " + partName
                                        + " of type " + wrapperType.getName() + " as the @XmlElement "
                                        + "defines the name as " + el.name());
                        } else {
                            LOG.warning("Could not create accessor for property " + partName
                                        + " of type " + wrapperType.getName());
                        }
                    }
                    fields.add(null);
                }
            } else {
                fields.add(null);
            }

        }

        return createWrapperHelper(wrapperType,
                                 setMethods.toArray(new Method[0]),
                                 getMethods.toArray(new Method[0]),
                                 jaxbMethods.toArray(new Method[0]),
                                 fields.toArray(new Field[0]),
                                 objectFactory);
    }

    private static Field getElField(String partName, final Class<?> wrapperType) {
        String fieldName = JAXBUtils.nameToIdentifier(partName, JAXBUtils.IdentifierType.VARIABLE);
        Field[] fields = ReflectionUtil.getDeclaredFields(wrapperType);
        for (Field field : fields) {
            XmlElement el = field.getAnnotation(XmlElement.class);
            if (el != null
                && partName.equals(el.name())) {
                return field;
            }

            XmlElementRef xmlElementRefAnnotation = field.getAnnotation(XmlElementRef.class);
            if (xmlElementRefAnnotation != null && partName.equals(xmlElementRefAnnotation.name())) {
                return field;
            }

            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }


    private static WrapperHelper createWrapperHelper(Class<?> wrapperType, Method[] setMethods,
                                                     Method[] getMethods, Method[] jaxbMethods,
                                                     Field[] fields, Object objectFactory) {

        WrapperHelper wh = compileWrapperHelper(wrapperType, setMethods, getMethods, jaxbMethods, fields,
                                                objectFactory);

        if (wh == null) {
            wh = new JAXBWrapperHelper(wrapperType, setMethods, getMethods, jaxbMethods, fields,
                                       objectFactory);
        }
        return wh;
    }

    private static WrapperHelper compileWrapperHelper(Class<?> wrapperType, Method[] setMethods,
                                                      Method[] getMethods, Method[] jaxbMethods,
                                                      Field[] fields, Object objectFactory) {
        return WrapperHelperCompiler.compileWrapperHelper(wrapperType, setMethods, getMethods,
                                                          jaxbMethods, fields, objectFactory);
    }

    // Liberty change: releaseJAXBMarshaller, getJAXBMarshaller, getJAXBUnmarshaller, releaseJAXBUnmarshaller methods are added
    /**
     * releaseJAXBMarshalller
     * Do not call this method if an exception occurred while using the
     * Marshaller. We don't want an object in an invalid state.
     *
     * @param marshaller Marshaller
     */
    public void releaseJAXBMarshaller(Marshaller marshaller) {
        if (ENABLE_MARSHALL_POOLING && marshaller != null) {
            marshallers.offerFirst(new SoftReference<Marshaller>(marshaller));
        }
    }

    /**
     * Get JAXBMarshaller
     *
     * @param context JAXBContext
     * @throws JAXBException
     */
    public Marshaller getJAXBMarshaller() throws JAXBException {
        Marshaller m = null;

        if (!ENABLE_MARSHALL_POOLING) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Marshaller created [no pooling]");
            }
            m = getContext().createMarshaller();
        } else {
            SoftReference<Marshaller> ref = marshallers.poll();
            while (ref != null && ref.get() == null) {
                ref = marshallers.poll();
            }
            if (ref != null) {
                m = ref.get();
            }
            if (m == null) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Marshaller created [not in pool]");
                }
                m = getContext().createMarshaller();
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Marshaller obtained [from  pool]");
                }
            }
        }
        return m;
    }

    /**
     * Get the unmarshaller. You must call releaseUnmarshaller to put it back into the pool
     *
     * @param binding JAXBDataBinding
     * @return Unmarshaller
     * @throws JAXBException
     */
    public Unmarshaller getJAXBUnmarshaller() throws JAXBException {
        if (!ENABLE_UNMARSHALL_POOLING) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Unmarshaller created [no pooling]");
            }
            return getContext().createUnmarshaller();
        }

        Unmarshaller unm = null;
        SoftReference<Unmarshaller> ref = unmarshallers.poll();
        while (ref != null && ref.get() == null) {
            ref = unmarshallers.poll();
        }
        if (ref != null) {
            unm = ref.get();
        }
        if (unm == null) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Unmarshaller created [not in pool]");
            }
            unm = getContext().createUnmarshaller();
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Unmarshaller obtained [from  pool]");
            }
        }
        return unm;
    }

    /**
     * Release Unmarshaller Do not call this method if an exception occurred while using the
     * Unmarshaller. We object my be in an invalid state.
     *
     * @param context JAXBContext
     * @param unmarshaller Unmarshaller
     */
    public void releaseJAXBUnmarshaller(Unmarshaller unmarshaller) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Unmarshaller placed back into pool");
        }
        if (ENABLE_UNMARSHALL_POOLING && unmarshaller != null) {
            try {
                //defect 176959
                //Don't remove the event handler
                //unmarshaller.setEventHandler(null);
                unmarshallers.offerFirst(new SoftReference<Unmarshaller>(unmarshaller));
            } catch (Throwable t) {
                // Log the problem, and continue without pooling
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("The following exception is ignored. Processing continues " + t);
                }
            }
        }
    }  // Liberty change: end
}

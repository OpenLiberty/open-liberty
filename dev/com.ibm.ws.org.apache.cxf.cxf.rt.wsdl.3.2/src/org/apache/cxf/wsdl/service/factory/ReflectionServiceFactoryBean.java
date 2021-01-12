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

package org.apache.cxf.wsdl.service.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Operation;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;

import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;

import org.apache.cxf.BusException;
import org.apache.cxf.annotations.EvaluateAllEndpoints;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.catalog.CatalogXmlSchemaURIResolver;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.common.xmlschema.XmlSchemaUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.source.mime.MimeAttribute;
import org.apache.cxf.databinding.source.mime.MimeSerializer;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.endpoint.ServiceContractResolverRegistry;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.FaultOutInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.ServiceModelSchemaValidator;
import org.apache.cxf.service.factory.FactoryBeanListener;
import org.apache.cxf.service.factory.FactoryBeanListener.Event;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.factory.SimpleMethodDispatcher;
import org.apache.cxf.service.invoker.FactoryInvoker;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.invoker.SingletonFactory;
import org.apache.cxf.service.model.AbstractMessageContainer;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.UnwrappedOperationInfo;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;



/**
 * Introspects a class and builds a {@link Service} from it. If a WSDL URL is
 * specified, a Service model will be directly from the WSDL and then metadata
 * will be filled in from the service class. If no WSDL URL is specified, the
 * Service will be constructed directly from the class structure.
 */
//CHECKSTYLE:OFF:NCSS    -   This class is just huge and complex
public class ReflectionServiceFactoryBean extends org.apache.cxf.service.factory.AbstractServiceFactoryBean {

    public static final String ENDPOINT_CLASS = "endpoint.class";
    public static final String GENERIC_TYPE = "generic.type";
    public static final String RAW_CLASS = "rawclass";
    public static final String WRAPPERGEN_NEEDED = "wrapper.gen.needed";
    public static final String EXTRA_CLASS = "extra.class";
    public static final String MODE_OUT = "messagepart.mode.out";
    public static final String MODE_INOUT = "messagepart.mode.inout";
    public static final String HOLDER = "messagepart.isholder";
    public static final String HEADER = "messagepart.isheader";
    public static final String ELEMENT_NAME = "messagepart.elementName";
    public static final String METHOD = "operation.method";
    public static final String FORCE_TYPES = "operation.force.types";
    public static final String METHOD_PARAM_ANNOTATIONS = "method.parameters.annotations";
    public static final String METHOD_ANNOTATIONS = "method.return.annotations";
    public static final String PARAM_ANNOTATION = "parameter.annotations";
    private static final Logger LOG = LogUtils.getL7dLogger(ReflectionServiceFactoryBean.class);
/*  Liberty change: 2 private field below are removed
    private static final boolean DO_VALIDATE = SystemPropertyAction.getProperty("cxf.validateServiceSchemas", "false")
            .equals("true");

    private static Class<? extends DataBinding> defaultDatabindingClass;  Liberty change: end */

    protected String wsdlURL;

    protected Class<?> serviceClass;
    protected ParameterizedType serviceType;
    protected Map<Type, Map<String, Class<?>>> parameterizedTypes;

    protected final Map<String, String> schemaLocationMapping = new HashMap<>();

    private List<AbstractServiceConfiguration> serviceConfigurations =
        new ArrayList<>();
    private QName serviceName;
    private Invoker invoker;
    private Executor executor;
    private List<String> ignoredClasses = new ArrayList<>(); /*
    (Arrays.asList( Liberty change: List population is moved to
            "java.lang.Object",
            "java.lang.Throwable",
            "org.omg.CORBA_2_3.portable.ObjectImpl",
            "org.omg.CORBA.portable.ObjectImpl",
            "javax.ejb.EJBObject",
            "javax.rmi.CORBA.Stub"
        )); */
    private List<Method> ignoredMethods = new ArrayList<>();
    private MethodDispatcher methodDispatcher = new SimpleMethodDispatcher();
    private Boolean wrappedStyle;
    private Map<String, Object> properties;
    private QName endpointName;
    private boolean populateFromClass;
    private boolean anonymousWrappers;
    private boolean qualifiedSchemas = true;
    private boolean validate;

    private List<Feature> features;

    private Map<Method, Boolean> wrappedCache = new HashMap<>();
    private Map<Method, Boolean> isRpcCache = new HashMap<>();
    private String styleCache;
    private Boolean defWrappedCache;

    public ReflectionServiceFactoryBean() {
        getServiceConfigurations().add(0, new DefaultServiceConfiguration());
        // Liberty change: ignoredClasses.add calls below are added
        ignoredClasses.add("java.lang.Object");
        ignoredClasses.add("java.lang.Throwable");
        ignoredClasses.add("org.omg.CORBA_2_3.portable.ObjectImpl");
        ignoredClasses.add("org.omg.CORBA.portable.ObjectImpl");
        ignoredClasses.add("javax.ejb.EJBObject");
        ignoredClasses.add("javax.rmi.CORBA.Stub");
    }

    protected DataBinding createDefaultDataBinding() {
        Object obj = null;
        Class<? extends DataBinding> cls = null;

        if (getServiceClass() != null) {
            org.apache.cxf.annotations.DataBinding db
                = getServiceClass().getAnnotation(org.apache.cxf.annotations.DataBinding.class);
            if (db != null) {
                try {
                    if (!StringUtils.isEmpty(db.ref())) {
                        return getBus().getExtension(ResourceManager.class).resolveResource(db.ref(),
                                                                                            db.value());
                    }
                    cls = db.value();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Could not create databinding "
                            + db.value().getName(), e);
                }
            }
        }
        if (cls == null && getBus() != null) {
            obj = getBus().getProperty(DataBinding.class.getName());
        }
        // Liberty change: if block below is added
        if (obj == null) {
            obj = "org.apache.cxf.jaxb.JAXBDataBinding";
        } // Liberty change: end
        try {
/*            if (obj == null && cls == null) {
                cls = getJAXBClass();
            } */
            if (obj instanceof String) {
                cls = ClassLoaderUtils.loadClass(obj.toString(), getClass(), DataBinding.class);
            } else if (obj instanceof Class) {
                cls = ((Class<?>)obj).asSubclass(DataBinding.class);
            }
            try {
                return cls.getConstructor(Boolean.TYPE, Map.class)
                    .newInstance(this.isQualifyWrapperSchema(), this.getProperties());
            } catch (NoSuchMethodException nsme) {
                //ignore, use the no-arg constructor
            }
            return cls.newInstance();
        } catch (Exception e) {
            throw new ServiceConstructionException(e);
        }
    }
/*  Liberty change: method below is removed
    private static synchronized Class<? extends DataBinding> getJAXBClass() throws ClassNotFoundException {
        if (defaultDatabindingClass == null) {
            defaultDatabindingClass = ClassLoaderUtils.loadClass("org.apache.cxf.jaxb.JAXBDataBinding",
                                                                 ReflectionServiceFactoryBean.class,
                                                                 DataBinding.class);
        }
        return defaultDatabindingClass;
    } */

    public void reset() {
        if (!dataBindingSet) {
            setDataBinding(null);
        }
        setService(null);
    }

    @Override
    public synchronized Service create() {
        reset();
        sendEvent(Event.START_CREATE);
        initializeServiceConfigurations();

        initializeServiceModel();

        initializeDefaultInterceptors();

        if (invoker != null) {
            getService().setInvoker(getInvoker());
        } else {
            getService().setInvoker(createInvoker());
        }

        if (getExecutor() != null) {
            getService().setExecutor(getExecutor());
        }
        if (getDataBinding() != null) {
            getService().setDataBinding(getDataBinding());
        }

        MethodDispatcher m = getMethodDispatcher(); // Liberty change: line added
        getService().put(MethodDispatcher.class.getName(), m);  // Liberty change: getMethodDispatcher() is replaced by m
        createEndpoints();

        fillInSchemaCrossreferences();

        Service serv = getService();
        sendEvent(Event.END_CREATE, serv);
        return serv;
    }




    /**
     * Code elsewhere in this function will fill in the name of the type of an
     * element but not the reference to the type. This function fills in the
     * type references. This does not set the type reference for elements that
     * are declared as refs to other elements. It is a giant pain to find them,
     * since they are not (generally) root elements and the code would have to
     * traverse all the types to find all of them. Users should look them up
     * through the collection, that's what it is for.
     */
    private void fillInSchemaCrossreferences() {
        Service service = getService();
        for (ServiceInfo serviceInfo : service.getServiceInfos()) {
            SchemaCollection schemaCollection = serviceInfo.getXmlSchemaCollection();

            // First pass, fill in any types for which we have a name but no
            // type.
            for (SchemaInfo schemaInfo : serviceInfo.getSchemas()) {
                Map<QName, XmlSchemaElement> elementsTable = schemaInfo.getSchema().getElements();
                for (XmlSchemaElement element : elementsTable.values()) {
                    if (element.getSchemaType() == null) {
                        QName typeName = element.getSchemaTypeName();
                        if (typeName != null) {
                            XmlSchemaType type = schemaCollection.getTypeByQName(typeName);
                            if (type == null) {
                                Message message = new Message("REFERENCE_TO_UNDEFINED_TYPE", LOG, element
                                    .getQName(), typeName, service.getName());
                                LOG.severe(message.toString());
                            } else {
                                element.setSchemaType(type);
                            }
                        }
                    }
                }

            }
        }
    }

    protected void createEndpoints() {
        Service service = getService();

        BindingFactoryManager bfm = getBus().getExtension(BindingFactoryManager.class);

        for (ServiceInfo inf : service.getServiceInfos()) {
            for (EndpointInfo ei : inf.getEndpoints()) {

                for (BindingOperationInfo boi : ei.getBinding().getOperations()) {
                    updateBindingOperation(boi);
                }
                try {
                    bfm.getBindingFactory(ei.getBinding().getBindingId());
                } catch (BusException e1) {
                    continue;
                }

                try {
                    Endpoint ep = createEndpoint(ei);

                    service.getEndpoints().put(ei.getName(), ep);
                } catch (EndpointException e) {
                    throw new ServiceConstructionException(e);
                }
            }
        }
    }

    public void updateBindingOperation(BindingOperationInfo boi) {
        Method m = getMethodDispatcher().getMethod(boi);
        sendEvent(FactoryBeanListener.Event.BINDING_OPERATION_CREATED, boi.getBinding(), boi, m);
    }

    public Endpoint createEndpoint(EndpointInfo ei) throws EndpointException {
        Endpoint ep = new EndpointImpl(getBus(), getService(), ei);
        sendEvent(Event.ENDPOINT_CREATED, ei, ep, getServiceClass());
        return ep;
    }

    protected void initializeServiceConfigurations() {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            c.setServiceFactory(this);
        }
    }

    protected void setServiceProperties() {
        MethodDispatcher md = getMethodDispatcher();
        getService().put(MethodDispatcher.class.getName(), md);
        for (Class<?> c : md.getClass().getInterfaces()) {
            getService().put(c.getName(), md);
        }
        if (properties != null) {
            getService().putAll(properties);
        }
    }

    protected void buildServiceFromWSDL(String url) {
        sendEvent(Event.CREATE_FROM_WSDL, url);

        if (LOG.isLoggable(Level.FINE)) { // Liberty change: log level is changed to FINE from INFO
            LOG.fine("Creating Service " + getServiceQName() + " from WSDL: " + url); // Liberty change: log level is changed to FINE from INFO
        }
        populateFromClass = false;
        WSDLServiceFactory factory = new WSDLServiceFactory(getBus(), url, getServiceQName());
        boolean setEPName = true;
        if (features != null) {
            for (Feature f : features) {
                if (f.getClass().isAnnotationPresent(EvaluateAllEndpoints.class)) {
                    setEPName = false;
                }
            }
        }
        if (setEPName) {
            // CXF will only evaluate this endpoint
            factory.setEndpointName(getEndpointName(false));
        }
        sendEvent(Event.WSDL_LOADED, factory.getDefinition());
        setService(factory.create());
        setServiceProperties();

        sendEvent(Event.SERVICE_SET, getService());

        EndpointInfo epInfo = getEndpointInfo();
        if (epInfo != null) {
            serviceConfigurations.add(new WSDLBasedServiceConfiguration(getEndpointInfo().getBinding()));
        }

        initializeWSDLOperations();

        Set<Class<?>> cls = getExtraClass();
        for (ServiceInfo si : getService().getServiceInfos()) { // Liberty change: for and if block
            if (cls != null && !cls.isEmpty()) {                // Liberty change: swapped places
                si.setProperty(EXTRA_CLASS, cls);
            }
        }
        initializeDataBindings();
    }

    protected void buildServiceFromClass() {
        Object o = getBus().getProperty("requireExplicitContractLocation");
        if (o != null
            && ("true".equals(o) || Boolean.TRUE.equals(o))) {
            throw new ServiceConstructionException(new Message("NO_WSDL_PROVIDED", LOG,
                                                               getServiceClass().getName()));
        }
        if (LOG.isLoggable(Level.FINE)) { // Liberty change: log level is changed to FINE from INFO
            LOG.fine("Creating Service " + getServiceQName() + " from class " + getServiceClass().getName()); // Liberty change: log level is changed to FINE from INFO
        }
        populateFromClass = true;

        if (Proxy.isProxyClass(this.getServiceClass())) {
            LOG.log(Level.WARNING, "USING_PROXY_FOR_SERVICE", getServiceClass());
        }

        sendEvent(Event.CREATE_FROM_CLASS, getServiceClass());

        ServiceInfo serviceInfo = new ServiceInfo();
        SchemaCollection col = serviceInfo.getXmlSchemaCollection();
        col.getXmlSchemaCollection().setSchemaResolver(new CatalogXmlSchemaURIResolver(this.getBus()));
        col.getExtReg().registerSerializer(MimeAttribute.class, new MimeSerializer());

        ServiceImpl service = new ServiceImpl(serviceInfo);
        setService(service);
        setServiceProperties();

        serviceInfo.setName(getServiceQName());
        serviceInfo.setTargetNamespace(serviceInfo.getName().getNamespaceURI());

        sendEvent(Event.SERVICE_SET, getService());

        createInterface(serviceInfo);


        Set<?> wrapperClasses = this.getExtraClass();
        for (ServiceInfo si : getService().getServiceInfos()) {
            if (wrapperClasses != null && !wrapperClasses.isEmpty()) {
                si.setProperty(EXTRA_CLASS, wrapperClasses);
            }
        }
        initializeDataBindings();

        boolean isWrapped = isWrapped() || hasWrappedMethods(serviceInfo.getInterface());
        if (isWrapped) {
            initializeWrappedSchema(serviceInfo);
        }

        for (OperationInfo opInfo : serviceInfo.getInterface().getOperations()) {
            Method m = (Method)opInfo.getProperty(METHOD);
            if (!isWrapped(m) && !isRPC(m) && opInfo.getInput() != null) {
                createBareMessage(serviceInfo, opInfo, false);
            }

            if (!isWrapped(m) && !isRPC(m) && opInfo.getOutput() != null) {
                createBareMessage(serviceInfo, opInfo, true);
            }

            if (opInfo.hasFaults()) {
                // check to make sure the faults are elements
                for (FaultInfo fault : opInfo.getFaults()) {
                    QName qn = (QName)fault.getProperty("elementName");
                    MessagePartInfo part = fault.getFirstMessagePart();
                    if (!part.isElement()) {
                        part.setElement(true);
                        part.setElementQName(qn);
                        checkForElement(serviceInfo, part);
                    }
                }
            }
        }
        if (LOG.isLoggable(Level.FINE) || isValidate()) {
            ServiceModelSchemaValidator validator = new ServiceModelSchemaValidator(serviceInfo);
            validator.walk();
            String validationComplaints = validator.getComplaints();
            if (!"".equals(validationComplaints)) {
                if (isValidate()) {
                    LOG.warning(validationComplaints);
                } else {
                    LOG.fine(validationComplaints);
                }
            }
        }
    }
    public boolean hasWrappedMethods(InterfaceInfo interfaceInfo) {
        for (OperationInfo opInfo : interfaceInfo.getOperations()) {
            if (opInfo.isUnwrappedCapable()) {
                return true;
            }
        }
        return false;
    }

    protected boolean isFromWsdl() {
        return !populateFromClass && !StringUtils.isEmpty(getWsdlURL());
    }

    protected void initializeServiceModel() {
      String wsdlurl = getWsdlURL();  // Liberty change: line added
        if (isFromWsdl()) {
            buildServiceFromWSDL(wsdlurl); // Liberty change: getWsdlURL() method is replaced by wsdlurl
        } else if (getServiceClass() != null) {
            buildServiceFromClass();
        } //else {  Liberty change: else block is removed
        //     throw new ServiceConstructionException(new Message("NO_WSDL_NO_SERVICE_CLASS_PROVIDED", LOG, getWsdlURL()));
        // }

        if (isValidate()) {
            validateServiceModel();
        }
    }

    public void validateServiceModel() {

        for (ServiceInfo si : getService().getServiceInfos()) {
            validateSchemas(si.getXmlSchemaCollection());

            for (OperationInfo opInfo : si.getInterface().getOperations()) {
                for (MessagePartInfo mpi : opInfo.getInput().getMessageParts()) {
                    assert mpi.getXmlSchema() != null;
                    if (mpi.isElement()) {
                        assert mpi.getXmlSchema() instanceof XmlSchemaElement;
                    } else {
                        assert !(mpi.getXmlSchema() instanceof XmlSchemaElement);
                    }
                }
                if (opInfo.hasOutput()) {
                    for (MessagePartInfo mpi : opInfo.getOutput().getMessageParts()) {
                        assert mpi.getXmlSchema() != null;
                        if (mpi.isElement()) {
                            assert mpi.getXmlSchema() instanceof XmlSchemaElement;
                        } else {
                            assert !(mpi.getXmlSchema() instanceof XmlSchemaElement);
                        }
                    }
                }
                if (opInfo.isUnwrappedCapable()) {
                    opInfo = opInfo.getUnwrappedOperation();
                    for (MessagePartInfo mpi : opInfo.getInput().getMessageParts()) {
                        assert mpi.getXmlSchema() != null;
                        if (mpi.isElement()) {
                            assert mpi.getXmlSchema() instanceof XmlSchemaElement;
                        } else {
                            assert !(mpi.getXmlSchema() instanceof XmlSchemaElement);
                        }
                    }
                    if (opInfo.hasOutput()) {
                        for (MessagePartInfo mpi : opInfo.getOutput().getMessageParts()) {
                            assert mpi.getXmlSchema() != null;
                            if (mpi.isElement()) {
                                assert mpi.getXmlSchema() instanceof XmlSchemaElement;
                            } else {
                                assert !(mpi.getXmlSchema() instanceof XmlSchemaElement);
                            }
                        }
                    }
                }
                if (opInfo.hasFaults()) {
                    // check to make sure the faults are elements
                    for (FaultInfo fault : opInfo.getFaults()) {
                        MessagePartInfo mpi = fault.getFirstMessagePart();
                        assert mpi != null;
                        assert mpi.getXmlSchema() != null;
                        assert mpi.isElement();
                        assert mpi.getXmlSchema() instanceof XmlSchemaElement;
                    }
                }

            }
        }
    }

    private void validateSchemas(SchemaCollection xmlSchemaCollection) {
        final boolean[] anyErrors = new boolean[1];  // Liberty change: line is added
        final StringBuilder errorBuilder = new StringBuilder();
        anyErrors[0] = false;  // Liberty change: line is added
        XercesXsdValidationImpl v = new XercesXsdValidationImpl();
        v.validateSchemas(xmlSchemaCollection.getXmlSchemaCollection(), new DOMErrorHandler() {
            public boolean handleError(DOMError error) {
                anyErrors[0] = true;   // Liberty change: line is added
                errorBuilder.append(error.getMessage());
                LOG.warning(error.getMessage());
                return true;
            }
        });
        if (anyErrors[0]) {  // Liberty change: errorBuilder.length() > 0 is replaced by anyErrors[0]
            throw new ServiceConstructionException(new Message("XSD_VALIDATION_ERROR", LOG,
                                                               errorBuilder.toString()));
        }
    }

    public boolean isPopulateFromClass() {
        return populateFromClass;
    }

    public void setPopulateFromClass(boolean fomClass) {
        this.populateFromClass = fomClass;
    }

    protected InterfaceInfo getInterfaceInfo() {
        if (getEndpointInfo() != null) {
            return getEndpointInfo().getInterface();
        }
        QName qn = this.getInterfaceName();
        for (ServiceInfo si : getService().getServiceInfos()) {
            if (qn.equals(si.getInterface().getName())) {
                return si.getInterface();
            }
        }
        throw new ServiceConstructionException(new Message("COULD_NOT_FIND_PORTTYPE", LOG, qn));
    }

    protected void initializeWSDLOperations() {
        List<OperationInfo> removes = new ArrayList<>();
        Method[] methods = serviceClass.getMethods();
        Arrays.sort(methods, new MethodComparator());

        InterfaceInfo intf = getInterfaceInfo();

        Map<QName, Method> validMethods = new HashMap<>();
        for (Method m : methods) {
            if (isValidMethod(m)) {
                QName opName = getOperationName(intf, m);
                validMethods.put(opName, m);
            }
        }

        for (OperationInfo o : intf.getOperations()) {
            Method selected = null;
            for (Map.Entry<QName, Method> m : validMethods.entrySet()) {
                QName opName = m.getKey();

                if (o.getName().getNamespaceURI().equals(opName.getNamespaceURI())
                    && isMatchOperation(o.getName().getLocalPart(), opName.getLocalPart())) {
                    selected = m.getValue();
                    break;
                }
            }

            if (selected == null) {
                LOG.log(Level.WARNING, "NO_METHOD_FOR_OP", o.getName());
                removes.add(o);
            } else {
                initializeWSDLOperation(intf, o, selected);
            }
        }
        for (OperationInfo op : removes) {
            intf.removeOperation(op);
        }

        // Some of the operations may have switched from unwrapped to wrapped.
        // Update the bindings.
        for (ServiceInfo service : getService().getServiceInfos()) {
            for (BindingInfo bi : service.getBindings()) {
                List<BindingOperationInfo> biremoves = new ArrayList<>();
                for (BindingOperationInfo binfo : bi.getOperations()) {
                    if (removes.contains(binfo.getOperationInfo())) {
                        biremoves.add(binfo);
                    } else {
                        binfo.updateUnwrappedOperation();
                    }
                }
                for (BindingOperationInfo binfo : biremoves) {
                    bi.removeOperation(binfo);
                }
            }
        }
        sendEvent(Event.INTERFACE_CREATED, intf, getServiceClass());
    }

    protected void initializeWSDLOperation(InterfaceInfo intf, OperationInfo o, Method method) {
        // rpc out-message-part-info class mapping
        Operation op = (Operation)o.getProperty(WSDLServiceBuilder.WSDL_OPERATION);

        if (initializeClassInfo(o, method, op == null ? null
            : CastUtils.cast(op.getParameterOrdering(), String.class))) {
            bindOperation(o, method);
            o.setProperty(ReflectionServiceFactoryBean.METHOD, method);
            sendEvent(Event.INTERFACE_OPERATION_BOUND, o, method);
        } else {
            LOG.log(Level.WARNING, "NO_METHOD_FOR_OP", o.getName());
        }
    }

    /**
     * set the holder generic type info into message part info
     *
     * @param o
     * @param method
     */
    protected boolean initializeClassInfo(OperationInfo o, Method method, List<String> paramOrder) {
        OperationInfo origOp = o;
        if (isWrapped(method)) {
            if (o.getUnwrappedOperation() == null) {
                //the "normal" algorithm didn't allow for unwrapping,
                //but the annotations say unwrap this.   We'll need to
                //make it.
                WSDLServiceBuilder.checkForWrapped(o, true);
            }
            if (o.getUnwrappedOperation() != null) {
                if (o.hasInput()) {
                    MessageInfo input = o.getInput();
                    MessagePartInfo part = input.getFirstMessagePart();
                    part.setTypeClass(getRequestWrapper(method));
                    part.setProperty("REQUEST.WRAPPER.CLASSNAME", getRequestWrapperClassName(method));
                    part.setIndex(0);
                }

                if (o.hasOutput()) {
                    MessageInfo input = o.getOutput();
                    MessagePartInfo part = input.getFirstMessagePart();
                    part.setTypeClass(getResponseWrapper(method));
                    part.setProperty("RESPONSE.WRAPPER.CLASSNAME", getResponseWrapperClassName(method));
                    part.setIndex(0);
                }
                setFaultClassInfo(o, method);
                o = o.getUnwrappedOperation();
            } else {
                LOG.warning(new Message("COULD_NOT_UNWRAP", LOG, o.getName(), method).toString());
                setFaultClassInfo(o, method);
            }
        } else if (o.isUnwrappedCapable()) {
            // remove the unwrapped operation because it will break the
            // the WrapperClassOutInterceptor, and in general makes
            // life more confusing
            o.setUnwrappedOperation(null);

            setFaultClassInfo(o, method);
        }
        o.setProperty(METHOD_PARAM_ANNOTATIONS, method.getParameterAnnotations());
        o.setProperty(METHOD_ANNOTATIONS, method.getAnnotations());
        //Set all out of band indexes to MAX_VALUE, anything left at MAX_VALUE after this is unmapped
        for (MessagePartInfo mpi : o.getInput().getOutOfBandParts()) {
            mpi.setIndex(Integer.MAX_VALUE);
        }
        if (o.hasOutput()) {
            for (MessagePartInfo mpi : o.getOutput().getOutOfBandParts()) {
                mpi.setIndex(Integer.MAX_VALUE);
            }
        }
        Class<?>[] paramTypes = method.getParameterTypes();
        Type[] genericTypes = method.getGenericParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (Exchange.class.equals(paramTypes[i])) {
                continue;
            }
            Class<?> paramType = paramTypes[i];
            Type genericType = genericTypes[i];
            if (!initializeParameter(o, method, i, paramType, genericType)) {
                return false;
            }
        }
        setIndexes(o.getInput());
        sendEvent(Event.OPERATIONINFO_IN_MESSAGE_SET, origOp, method, origOp.getInput());
        // Initialize return type
        Class<?> paramType = method.getReturnType();      // Liberty change: line added
        Type genericType = method.getGenericReturnType(); // Liberty change: line added
        if (o.hasOutput()
            // Liberty change: method.getReturnType() and method.getGenericReturnType() are replaced by paramType and  genericType below
            && !initializeParameter(o, method, -1, method.getReturnType(), method.getGenericReturnType())) {
            return false;
        }
        if (o.hasOutput()) {
            setIndexes(o.getOutput());
        }
        if (origOp.hasOutput()) {
            sendEvent(Event.OPERATIONINFO_OUT_MESSAGE_SET, origOp, method, origOp.getOutput());
        }

        setFaultClassInfo(o, method);
        return true;
    }
    private void setIndexes(MessageInfo m) {
        int max = -1;
        for (MessagePartInfo mpi : m.getMessageParts()) {
            if (mpi.getIndex() > max && mpi.getIndex() != Integer.MAX_VALUE) {
                max = mpi.getIndex();
            }
        }
        for (MessagePartInfo mpi : m.getMessageParts()) {
            if (mpi.getIndex() == Integer.MAX_VALUE) {
                max++;
                mpi.setIndex(max);
            }
        }
    }
    private boolean initializeParameter(OperationInfo o, Method method, int i,
                                     Class<?> paramType, Type genericType) {
        boolean isIn = isInParam(method, i);
        boolean isOut = isOutParam(method, i);
        boolean isHeader = isHeader(method, i);
        Annotation[] paraAnnos = null;
        if (i != -1 && o.getProperty(METHOD_PARAM_ANNOTATIONS) != null) {
            Annotation[][] anns = (Annotation[][])o.getProperty(METHOD_PARAM_ANNOTATIONS);
            paraAnnos = anns[i];
        } else if (i == -1 && o.getProperty(METHOD_ANNOTATIONS) != null) {
            paraAnnos = (Annotation[])o.getProperty(METHOD_ANNOTATIONS);
        }

        MessagePartInfo part = null;
        if (isIn && !isOut) {
            QName name = getInPartName(o, method, i);
            part = o.getInput().getMessagePart(name);
            if (part == null && isFromWsdl()) {
                part = o.getInput().getMessagePartByIndex(i);
            }
            if (part == null && isHeader && o.isUnwrapped()) {
                part = ((UnwrappedOperationInfo)o).getWrappedOperation().getInput().getMessagePart(name);
                boolean add = true;
                /* Liberty change: if block below is removed
                if (part == null) {
                    QName name2 = this.getInParameterName(o, method, i);
                    part = o.getInput().getMessagePart(name2);
                    if (part != null) {
                        add = false;
                        name = name2;
                    }
                } Liberty change: end */
                if (part != null) {
                    //header part in wsdl, need to get this mapped in to the unwrapped form
                    if (paraAnnos != null) {
                        part.setProperty(PARAM_ANNOTATION, paraAnnos);
                    }
                    if (add) {
                        MessagePartInfo inf = o.getInput().addMessagePart(part.getName());
                        inf.setTypeQName(part.getTypeQName());
                        inf.setElement(part.isElement());
                        inf.setElementQName(part.getElementQName());
                        inf.setConcreteName(part.getConcreteName());
                        inf.setXmlSchema(part.getXmlSchema());
                        part = inf;
                        /* Liberty change: if block below is removed
                        if (paraAnnos != null) {
                            part.setProperty(PARAM_ANNOTATION, paraAnnos);
                        } Liberty change: end */
                    }
                    part.setProperty(HEADER, Boolean.TRUE);
                }
            }
            if (part == null) {
                return false;
            }
            initializeParameter(part, paramType, genericType);

            part.setIndex(i);
        } else if (!isIn && isOut) {
            QName name = getOutPartName(o, method, i);
            part = o.getOutput().getMessagePart(name);
            if (part == null && isFromWsdl()) {
                part = o.getOutput().getMessagePartByIndex(i + 1);
            }
            if (part == null) {
                return false;
            }
            part.setProperty(ReflectionServiceFactoryBean.MODE_OUT, Boolean.TRUE);
            initializeParameter(part, paramType, genericType);
            part.setIndex(i + 1);
        } else if (isIn && isOut) {
            QName name = getInPartName(o, method, i);
            part = o.getInput().getMessagePart(name);
            /* Liberty change: if block below is removed
            if (part == null && isHeader && o.isUnwrapped()) {
                QName name2 = this.getInParameterName(o, method, i);
                part = o.getInput().getMessagePart(name2);
                if (part != null) {
                    name = name2;
                }
            } Liberty change: end */
            if (part == null && this.isFromWsdl()) {
                part = o.getInput().getMessagePartByIndex(i);
            }
            // Liberty change: if block below is added
            if (part == null && isHeader && o.isUnwrapped()) {
                part = o.getUnwrappedOperation().getInput().getMessagePart(name);
            } // Liberty change: end
            if (part == null) {
                return false;
            }
            part.setProperty(ReflectionServiceFactoryBean.MODE_INOUT, Boolean.TRUE);
            initializeParameter(part, paramType, genericType);
            part.setIndex(i);

            QName inName = part.getConcreteName();
            part = o.getOutput().getMessagePart(name);

            if (part == null) {
                part = o.getOutput().getMessagePart(inName);
            }
            if (part == null && isHeader && o.isUnwrapped()) {
                part = o.getUnwrappedOperation().getOutput().getMessagePart(name);
                if (part == null) {
                    part = o.getUnwrappedOperation().getOutput().getMessagePart(inName);
                }
            }

            if (part == null) {
                return false;
            }
            part.setProperty(ReflectionServiceFactoryBean.MODE_INOUT, Boolean.TRUE);
            initializeParameter(part, paramType, genericType);
            part.setIndex(i + 1);
        }
        if (paraAnnos != null && part != null) {
            part.setProperty(PARAM_ANNOTATION, paraAnnos);
        }

        return true;
    }
    private void setFaultClassInfo(OperationInfo o, Method selected) {
        Class<?>[] types = selected.getExceptionTypes();
/*
        Map<FaultInfo, List<MessagePartInfo>> mpiMap = null;
        if (types.length > 0) {
            //early iterate over FaultInfo before cycling on exception types
            //as fi.getMessageParts() is very time-consuming due to elements
            //copy in ArrayList constructor
            mpiMap = new HashMap<>();
            for (FaultInfo fi : o.getFaults()) {
                mpiMap.put(fi, fi.getMessageParts());
            }
        } */

        for (int i = 0; i < types.length; i++) {
            Class<?> exClass = types[i];
            Class<?> beanClass = getBeanClass(exClass);
            if (beanClass == null) {
                continue;
            }

            QName name = getFaultName(o.getInterface(), o, exClass, beanClass);

            // for (Entry<FaultInfo, List<MessagePartInfo>> entry : mpiMap.entrySet()) {  Liberty change: line removed
            for (FaultInfo fi : o.getFaults()) {  // Liberty change: line added
                // FaultInfo fi = entry.getKey(); Liberty change: line removed
                // List<MessagePartInfo> mpis = entry.getValue(); Liberty change: line removed
                List<MessagePartInfo> mpis = fi.getMessageParts();
                if (mpis.size() != 1) {
                    Message message = new Message("NO_FAULT_PART", LOG, fi.getFaultName());
                    LOG.log(Level.WARNING, message.toString());
                }
                for (MessagePartInfo mpi : mpis) {
                    String ns = null;
                    if (mpi.isElement()) {
                        ns = mpi.getElementQName().getNamespaceURI();
                    } else {
                        ns = mpi.getTypeQName().getNamespaceURI();
                    }
                    if (mpi.getConcreteName().getLocalPart().equals(name.getLocalPart())
                        && name.getNamespaceURI().equals(ns)) {
                        fi.setProperty(Class.class.getName(), exClass);
                        mpi.setTypeClass(beanClass);
                        sendEvent(Event.OPERATIONINFO_FAULT, o, exClass, fi);
                    }
                }
            }
        }
    }
    protected Invoker createInvoker() {
        Class<?> cls = getServiceClass();
        if (cls.isInterface()) {
            return null;
        }
        return new FactoryInvoker(new SingletonFactory(getServiceClass()));
    }

    protected ServiceInfo createServiceInfo(InterfaceInfo intf) {
        ServiceInfo svcInfo = new ServiceInfo();
        svcInfo.setInterface(intf);

        return svcInfo;
    }

    protected InterfaceInfo createInterface(ServiceInfo serviceInfo) {
        QName intfName = getInterfaceName();
        InterfaceInfo intf = new InterfaceInfo(serviceInfo, intfName);

        Method[] methods = getServiceClass().getMethods();

        // The BP profile states we can't have operations of the same name
        // so we have to append numbers to the name. Different JVMs sort methods
        // differently.
        // We need to keep them ordered so if we have overloaded methods, the
        // wsdl is generated the same every time across JVMs and across
        // client/servers.
        Arrays.sort(methods, new MethodComparator());

        for (Method m : methods) {
            if (isValidMethod(m)) {
                createOperation(serviceInfo, intf, m);
            }
        }
        sendEvent(Event.INTERFACE_CREATED, intf, getServiceClass());
        return intf;
    }

    protected OperationInfo createOperation(ServiceInfo serviceInfo, InterfaceInfo intf, Method m) {
        OperationInfo op = intf.addOperation(getOperationName(intf, m));
        op.setProperty(m.getClass().getName(), m);
        op.setProperty("action", getAction(op, m));
        final Annotation[] annotations = m.getAnnotations();
        final Annotation[][] parAnnotations = m.getParameterAnnotations();
        op.setProperty(METHOD_ANNOTATIONS, annotations);
        op.setProperty(METHOD_PARAM_ANNOTATIONS, parAnnotations);

        boolean isrpc = isRPC(m);
        if (!isrpc && isWrapped(m)) {
            UnwrappedOperationInfo uOp = new UnwrappedOperationInfo(op);
            uOp.setProperty(METHOD_ANNOTATIONS, annotations);
            uOp.setProperty(METHOD_PARAM_ANNOTATIONS, parAnnotations);
            op.setUnwrappedOperation(uOp);

            createMessageParts(intf, uOp, m);

            if (uOp.hasInput()) {
                MessageInfo msg = new MessageInfo(op, MessageInfo.Type.INPUT, uOp.getInput().getName());
                op.setInput(uOp.getInputName(), msg);

                createInputWrappedMessageParts(uOp, m, msg);

                for (MessagePartInfo p : uOp.getInput().getMessageParts()) {
                    p.setConcreteName(p.getName());
                }
            }

            if (uOp.hasOutput()) {

                QName name = uOp.getOutput().getName();
                MessageInfo msg = new MessageInfo(op, MessageInfo.Type.OUTPUT, name);
                op.setOutput(uOp.getOutputName(), msg);

                createOutputWrappedMessageParts(uOp, m, msg);

                for (MessagePartInfo p : uOp.getOutput().getMessageParts()) {
                    p.setConcreteName(p.getName());
                }
            }
        } else {
            if (isrpc) {
                op.setProperty(FORCE_TYPES, Boolean.TRUE);
            }
            createMessageParts(intf, op, m);
        }

        bindOperation(op, m);

        sendEvent(Event.INTERFACE_OPERATION_BOUND, op, m);
        return op;
    }

    protected void bindOperation(OperationInfo op, Method m) {
        getMethodDispatcher().bind(op, m);
    }

    protected void initializeWrappedSchema(ServiceInfo serviceInfo) {
        for (OperationInfo op : serviceInfo.getInterface().getOperations()) {
            if (op.getUnwrappedOperation() != null) {
                if (op.hasInput()) {
                    MessagePartInfo fmpi = op.getInput().getFirstMessagePart();
                    if (fmpi.getTypeClass() == null) {

                        QName wrapperBeanName = fmpi.getElementQName();
                        XmlSchemaElement e = null;
                        for (SchemaInfo s : serviceInfo.getSchemas()) {
                            e = s.getElementByQName(wrapperBeanName);
                            if (e != null) {
                                fmpi.setXmlSchema(e);
                                break;
                            }
                        }
                        if (e == null) {
                            createWrappedSchema(serviceInfo, op.getInput(), op.getUnwrappedOperation()
                                .getInput(), wrapperBeanName);
                        }
                    }

                    for (MessagePartInfo mpi : op.getInput().getMessageParts()) {
                        if (Boolean.TRUE.equals(mpi.getProperty(HEADER))) {
                            QName qn = (QName)mpi.getProperty(ELEMENT_NAME);
                            mpi.setElement(true);
                            mpi.setElementQName(qn);

                            checkForElement(serviceInfo, mpi);
                        }
                    }

                }
                if (op.hasOutput()) {
                    MessagePartInfo fmpi = op.getOutput().getFirstMessagePart();
                    if (fmpi.getTypeClass() == null) {

                        QName wrapperBeanName = fmpi.getElementQName();
                        XmlSchemaElement e = null;
                        for (SchemaInfo s : serviceInfo.getSchemas()) {
                            e = s.getElementByQName(wrapperBeanName);
                            if (e != null) {
                                break;
                            }
                        }
                        if (e == null) {
                            createWrappedSchema(serviceInfo, op.getOutput(), op.getUnwrappedOperation()
                                .getOutput(), wrapperBeanName);
                        }
                    }
                    for (MessagePartInfo mpi : op.getOutput().getMessageParts()) {
                        if (Boolean.TRUE.equals(mpi.getProperty(HEADER))) {
                            QName qn = (QName)mpi.getProperty(ELEMENT_NAME);
                            mpi.setElement(true);
                            mpi.setElementQName(qn);

                            checkForElement(serviceInfo, mpi);
                        }
                    }
                }
            }
        }

    }

    protected void checkForElement(ServiceInfo serviceInfo, MessagePartInfo mpi) {
        SchemaInfo si = getOrCreateSchema(serviceInfo, mpi.getElementQName().getNamespaceURI(),
                                          getQualifyWrapperSchema());
        XmlSchemaElement e = si.getSchema().getElementByName(mpi.getElementQName().getLocalPart());
        if (e != null) {
            mpi.setXmlSchema(e);
            return;
        }
        XmlSchema schema = si.getSchema();
        si.setElement(null); //cached element is now invalid

        XmlSchemaElement el = new XmlSchemaElement(schema, true);
        el.setName(mpi.getElementQName().getLocalPart());
        el.setNillable(true);

        XmlSchemaType tp = (XmlSchemaType)mpi.getXmlSchema();
        if (tp == null) {
            throw new ServiceConstructionException(new Message("INTRACTABLE_PART", LOG,
                                                               mpi.getName(),
                                                               mpi.getMessageInfo().getName()));
        }
        el.setSchemaTypeName(tp.getQName());
        mpi.setXmlSchema(el);
    }

    public boolean getAnonymousWrapperTypes() {
        return anonymousWrappers;
    }

    public boolean isAnonymousWrapperTypes() {
        return anonymousWrappers;
    }

    public void setAnonymousWrapperTypes(boolean b) {
        anonymousWrappers = b;
    }

    public final boolean getQualifyWrapperSchema() {
        return qualifiedSchemas;
    }

    public boolean isQualifyWrapperSchema() {
        return qualifiedSchemas;
    }

    public void setQualifyWrapperSchema(boolean b) {
        qualifiedSchemas = b;
    }

    protected void createWrappedSchema(ServiceInfo serviceInfo, AbstractMessageContainer wrappedMessage,
                                       AbstractMessageContainer unwrappedMessage, QName wrapperBeanName) {
        SchemaInfo schemaInfo = getOrCreateSchema(serviceInfo, wrapperBeanName.getNamespaceURI(),
                                                  getQualifyWrapperSchema());

        createWrappedMessageSchema(serviceInfo, wrappedMessage, unwrappedMessage, schemaInfo,
                                   wrapperBeanName);
    }

    protected void createBareMessage(ServiceInfo serviceInfo, OperationInfo opInfo, boolean isOut) {

        MessageInfo message = isOut ? opInfo.getOutput() : opInfo.getInput();

        final List<MessagePartInfo> messageParts = message.getMessageParts();
        if (messageParts.isEmpty()) {
            return;
        }

        Method method = (Method)opInfo.getProperty(METHOD);
        int paraNumber = 0;
        for (MessagePartInfo mpi : messageParts) {
            SchemaInfo schemaInfo = null;
            XmlSchema schema = null;

            QName qname = (QName)mpi.getProperty(ELEMENT_NAME);
            if (messageParts.size() == 1 && qname == null) {
                qname = !isOut ? getInParameterName(opInfo, method, -1)
                        : getOutParameterName(opInfo, method, -1);

                if (qname.getLocalPart().startsWith("arg") || qname.getLocalPart().startsWith("return")) {
                    qname = isOut
                        ? new QName(qname.getNamespaceURI(), method.getName() + "Response") : new QName(qname
                            .getNamespaceURI(), method.getName());
                }
            } else if (isOut && messageParts.size() > 1 && qname == null) {
                while (!isOutParam(method, paraNumber)) {
                    paraNumber++;
                }
                qname = getOutParameterName(opInfo, method, paraNumber);
            } else if (qname == null) {
                qname = getInParameterName(opInfo, method, paraNumber);
            }

            for (SchemaInfo s : serviceInfo.getSchemas()) {
                if (s.getNamespaceURI().equals(qname.getNamespaceURI())) {
                    schemaInfo = s;
                    break;
                }
            }

            if (schemaInfo == null) {
                schemaInfo = getOrCreateSchema(serviceInfo, qname.getNamespaceURI(), true);
                schema = schemaInfo.getSchema();
            } else {
                schema = schemaInfo.getSchema();
                if (schema != null && schema.getElementByName(qname) != null) {
                    mpi.setElement(true);
                    mpi.setElementQName(qname);
                    mpi.setXmlSchema(schema.getElementByName(qname));
                    paraNumber++;
                    continue;
                }
            }

            schemaInfo.setElement(null); //cached element is now invalid
            XmlSchemaElement el = new XmlSchemaElement(schema, true);
            el.setName(qname.getLocalPart());
            el.setNillable(true);

            if (mpi.isElement()) {
                XmlSchemaElement oldEl = (XmlSchemaElement)mpi.getXmlSchema();
                if (null != oldEl && !oldEl.getQName().equals(qname)) {
                    el.setSchemaTypeName(oldEl.getSchemaTypeName());
                    el.setSchemaType(oldEl.getSchemaType());
                    if (oldEl.getSchemaTypeName() != null) {
                        addImport(schema, oldEl.getSchemaTypeName().getNamespaceURI());
                    }
                }
                mpi.setElement(true);
                mpi.setXmlSchema(el);
                mpi.setElementQName(qname);
                mpi.setConcreteName(qname);
                continue;
            }
            if (null == mpi.getTypeQName() && mpi.getXmlSchema() == null) {
                throw new ServiceConstructionException(new Message("UNMAPPABLE_PORT_TYPE", LOG,
                                                                   method.getDeclaringClass().getName(),
                                                                   method.getName(),
                                                                   mpi.getName()));
            }
            if (mpi.getTypeQName() != null) {
                el.setSchemaTypeName(mpi.getTypeQName());
            } else {
                el.setSchemaType((XmlSchemaType)mpi.getXmlSchema());
            }
            mpi.setXmlSchema(el);
            mpi.setConcreteName(qname);
            if (mpi.getTypeQName() != null) {
                addImport(schema, mpi.getTypeQName().getNamespaceURI());
            }

            mpi.setElement(true);
            mpi.setElementQName(qname);
            paraNumber++;
        }
    }

    private void addImport(XmlSchema schema, String ns) {
        if (!ns.equals(schema.getTargetNamespace())
            && !ns.equals(WSDLConstants.NS_SCHEMA_XSD)
            && !isExistImport(schema, ns)) {
            XmlSchemaImport is = new XmlSchemaImport(schema);
            is.setNamespace(ns);
            if (this.schemaLocationMapping.get(ns) != null) {
                is.setSchemaLocation(this.schemaLocationMapping.get(ns));
            }
            if (!schema.getItems().contains(is)) {
                schema.getItems().add(is);
            }
        }
    }

    private boolean isExistImport(XmlSchema schema, String ns) {
        boolean isExist = false;

        for (XmlSchemaExternal ext : schema.getExternals()) {
            if (ext instanceof XmlSchemaImport) {
                XmlSchemaImport xsImport = (XmlSchemaImport)ext;
                if (xsImport.getNamespace().equals(ns)) {
                    isExist = true;
                    break;
                }
            }
        }
        return isExist;

    }

    private XmlSchemaElement getExistingSchemaElement(XmlSchema schema, QName qn) {
        return schema.getElements().get(qn);
    }

    private boolean isExistSchemaElement(XmlSchema schema, QName qn) {
        return getExistingSchemaElement(schema, qn) != null;
    }

    private void createWrappedMessageSchema(ServiceInfo serviceInfo, AbstractMessageContainer wrappedMessage,
                                            AbstractMessageContainer unwrappedMessage, SchemaInfo info,
                                            QName wrapperName) {

        XmlSchema schema = info.getSchema();
        info.setElement(null); // the cached schema will be no good
        XmlSchemaElement el = new XmlSchemaElement(schema, true);
        el.setName(wrapperName.getLocalPart());

        wrappedMessage.getFirstMessagePart().setXmlSchema(el);

        boolean anonymousType = isAnonymousWrapperTypes();
        XmlSchemaComplexType ct = new XmlSchemaComplexType(schema,
                /*CXF-6783: don't create anonymous top-level types*/!anonymousType);

        if (!anonymousType) {
            ct.setName(wrapperName.getLocalPart());
            el.setSchemaTypeName(wrapperName);
        }
        el.setSchemaType(ct);

        XmlSchemaSequence seq = new XmlSchemaSequence();
        ct.setParticle(seq);

        for (MessagePartInfo mpi : unwrappedMessage.getMessageParts()) {
            el = new XmlSchemaElement(schema, Boolean.TRUE.equals(mpi.getProperty(HEADER)));
            // We hope that we can't have parts that different only in namespace.
            el.setName(mpi.getName().getLocalPart()); // Liberty change: line added
            Map<Class<?>, Boolean> jaxbAnnoMap = getJaxbAnnoMap(mpi);
            if (mpi.isElement()) {
                addImport(schema, mpi.getElementQName().getNamespaceURI());
                el.setName(null);   // Liberty change: line added
                XmlSchemaUtils.setElementRefName(el, mpi.getElementQName());
            } else {
                // We hope that we can't have parts that different only in namespace.
                // el.setName(mpi.getName().getLocalPart());  Liberty change: line removed
                if (mpi.getTypeQName() != null && !jaxbAnnoMap.containsKey(XmlList.class)) {
                    el.setSchemaTypeName(mpi.getTypeQName());
                    addImport(schema, mpi.getTypeQName().getNamespaceURI());
                }

                el.setSchemaType((XmlSchemaType)mpi.getXmlSchema());

                if (schema.getElementFormDefault().equals(XmlSchemaForm.UNQUALIFIED)) {
                    mpi.setConcreteName(new QName(null, mpi.getName().getLocalPart()));
                } else {
                    mpi.setConcreteName(mpi.getName());
                }
            }
            if (!Boolean.TRUE.equals(mpi.getProperty(HEADER))) {
                boolean wasType = !mpi.isElement();
                if (wasType) {
                    QName concreteName = mpi.getConcreteName();
                    mpi.setElement(true);
                    mpi.setElementQName(el.getQName());
                    mpi.setConcreteName(concreteName);
                }

                addMimeType(el, getMethodParameterAnnotations(mpi));
                Annotation[] methodAnnotations = getMethodAnnotations(mpi);
                if (methodAnnotations != null) {
                    addMimeType(el, methodAnnotations);
                }

                long min = getWrapperPartMinOccurs(mpi);
                long max = getWrapperPartMaxOccurs(mpi);
                boolean nillable = isWrapperPartNillable(mpi);
                Boolean qualified = isWrapperPartQualified(mpi);
                if (qualified == null) {
                    qualified = this.isQualifyWrapperSchema();
                }
                if (qualified
                    && StringUtils.isEmpty(mpi.getConcreteName().getNamespaceURI())) {
                    QName newName = new QName(wrapperName.getNamespaceURI(),
                                              mpi.getConcreteName().getLocalPart());
                    mpi.setElement(true);
                    mpi.setElementQName(newName);
                    mpi.setConcreteName(newName);
                    el.setName(newName.getLocalPart());
                    el.setForm(XmlSchemaForm.QUALIFIED);
                }

                if (Collection.class.isAssignableFrom(mpi.getTypeClass())
                           && mpi.getTypeClass().isInterface()) {
                    Type type = (Type)mpi.getProperty(GENERIC_TYPE);

                    if (!(type instanceof java.lang.reflect.ParameterizedType)
                        && el.getSchemaTypeName() == null && el.getSchemaType() == null) {
                        max = Long.MAX_VALUE;
                        el.setSchemaTypeName(Constants.XSD_ANYTYPE);
                    }
                }
                el.setMinOccurs(min);
                el.setMaxOccurs(max);
                if (nillable) {
                    el.setNillable(nillable);
                }
                seq.getItems().add(el);
                mpi.setXmlSchema(el);
            }
            if (Boolean.TRUE.equals(mpi.getProperty(HEADER))) {
                QName qn = (QName)mpi.getProperty(ELEMENT_NAME);
                el.setName(qn.getLocalPart());

                SchemaInfo headerSchemaInfo = getOrCreateSchema(serviceInfo, qn.getNamespaceURI(),
                                                                getQualifyWrapperSchema());
                if (!isExistSchemaElement(headerSchemaInfo.getSchema(), qn)) {
                    headerSchemaInfo.getSchema().getItems().add(el);
                }
            }
        }

    }

    private Annotation[] getMethodParameterAnnotations(final MessagePartInfo mpi) {
        Annotation[][] paramAnno = (Annotation[][])mpi.getProperty(METHOD_PARAM_ANNOTATIONS);
        int index = mpi.getIndex();
        if (paramAnno != null && index < paramAnno.length && index >= 0) {
            return paramAnno[index];
        }
        return null;
    }

    private Annotation[] getMethodAnnotations(final MessagePartInfo mpi) {
        return (Annotation[])mpi.getProperty(METHOD_ANNOTATIONS);
    }

    private void addMimeType(final XmlSchemaElement element, final Annotation[] annotations) {
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof XmlMimeType) {
                    MimeAttribute attr = new MimeAttribute();
                    attr.setValue(((XmlMimeType)annotation).value());
                    element.addMetaInfo(MimeAttribute.MIME_QNAME, attr);
                }
            }
        }
    }

    private Map<Class<?>, Boolean> getJaxbAnnoMap(MessagePartInfo mpi) {
        Map<Class<?>, Boolean> map = new ConcurrentHashMap<>(4, 0.75f, 1);
        Annotation[] anns = getMethodParameterAnnotations(mpi);

        if (anns != null) {
            for (Annotation anno : anns) {
                if (anno instanceof XmlList) {
                    map.put(XmlList.class, true);
                }
                if (anno instanceof XmlAttachmentRef) {
                    map.put(XmlAttachmentRef.class, true);
                }
                if (anno instanceof XmlJavaTypeAdapter) {
                    map.put(XmlJavaTypeAdapter.class, true);
                }
                if (anno instanceof XmlElementWrapper) {
                    map.put(XmlElementWrapper.class, true);
                }
            }
        }
        return map;
    }

    private SchemaInfo getOrCreateSchema(ServiceInfo serviceInfo, String namespaceURI, boolean qualified) {
        for (SchemaInfo s : serviceInfo.getSchemas()) {
            if (s.getNamespaceURI().equals(namespaceURI)) {
                return s;
            }
        }

        SchemaInfo schemaInfo = new SchemaInfo(namespaceURI);
        SchemaCollection col = serviceInfo.getXmlSchemaCollection();
        XmlSchema schema = col.getSchemaByTargetNamespace(namespaceURI);

        if (schema != null) {
            schemaInfo.setSchema(schema);
            serviceInfo.addSchema(schemaInfo);
            return schemaInfo;
        }

        schema = col.newXmlSchemaInCollection(namespaceURI);
        if (qualified) {
            schema.setElementFormDefault(XmlSchemaForm.QUALIFIED);
        }
        schemaInfo.setSchema(schema);

        Map<String, String> explicitNamespaceMappings = this.getDataBinding().getDeclaredNamespaceMappings();
        if (explicitNamespaceMappings == null) {
            explicitNamespaceMappings = Collections.emptyMap();
        }
        NamespaceMap nsMap = new NamespaceMap();
        for (Map.Entry<String, String> mapping : explicitNamespaceMappings.entrySet()) {
            nsMap.add(mapping.getValue(), mapping.getKey());
        }

        if (!explicitNamespaceMappings.containsKey(WSDLConstants.NS_SCHEMA_XSD)) {
            nsMap.add(WSDLConstants.NP_SCHEMA_XSD, WSDLConstants.NS_SCHEMA_XSD);
        }
        if (!explicitNamespaceMappings.containsKey(serviceInfo.getTargetNamespace())) {
            nsMap.add(WSDLConstants.CONVENTIONAL_TNS_PREFIX, serviceInfo.getTargetNamespace());
        }
        schema.setNamespaceContext(nsMap);
        serviceInfo.addSchema(schemaInfo);
        return schemaInfo;
    }
    // CHECKSTYLE:OFF
    protected void createMessageParts(InterfaceInfo intf, OperationInfo op, Method method) {
        final Class<?>[] paramClasses = method.getParameterTypes();
        // Setup the input message
        op.setProperty(METHOD, method);
        MessageInfo inMsg = op.createMessage(this.getInputMessageName(op, method), MessageInfo.Type.INPUT);
        op.setInput(inMsg.getName().getLocalPart(), inMsg);
        final Annotation[][] parAnnotations = method.getParameterAnnotations();
        final Type[] genParTypes = method.getGenericParameterTypes();
        for (int j = 0; j < paramClasses.length; j++) {
            if (Exchange.class.equals(paramClasses[j])) {
                continue;
            }
            if (isInParam(method, j)) {
                QName q = getInParameterName(op, method, j);
                QName partName = getInPartName(op, method, j);
                if (!isRPC(method) && !isWrapped(method)
                    && inMsg.getMessagePartsMap().containsKey(partName)) {
                    LOG.log(Level.WARNING, "INVALID_BARE_METHOD", getServiceClass() + "." + method.getName());
                    partName = new QName(partName.getNamespaceURI(), partName.getLocalPart() + j);
                    q = new QName(q.getNamespaceURI(), q.getLocalPart() + j);
                }
                MessagePartInfo part = inMsg.addMessagePart(partName);

                if (isHolder(paramClasses[j], genParTypes[j]) && !isInOutParam(method, j)) {
                    LOG.log(Level.WARNING, "INVALID_WEBPARAM_MODE", getServiceClass().getName() + "."
                                                                    + method.getName());
                }
                initializeParameter(part, paramClasses[j], genParTypes[j]);
                part.setProperty(METHOD_PARAM_ANNOTATIONS, parAnnotations);
                part.setProperty(PARAM_ANNOTATION, parAnnotations[j]);
                if (!getJaxbAnnoMap(part).isEmpty()) {
                    op.setProperty(WRAPPERGEN_NEEDED, true);
                }
                if (!isWrapped(method) && !isRPC(method)) {
                    part.setProperty(ELEMENT_NAME, q);
                }

                if (isHeader(method, j)) {
                    part.setProperty(HEADER, Boolean.TRUE);
                    if (isRPC(method) || !isWrapped(method)) {
                        part.setElementQName(q);
                    } else {
                        part.setProperty(ELEMENT_NAME, q);
                    }
                }
                part.setIndex(j);
            }
        }
        sendEvent(Event.OPERATIONINFO_IN_MESSAGE_SET, op, method, inMsg);

        boolean hasOut = hasOutMessage(method);
        if (hasOut) {
            // Setup the output message
            MessageInfo outMsg = op.createMessage(createOutputMessageName(op, method),
                                                  MessageInfo.Type.OUTPUT);
            op.setOutput(outMsg.getName().getLocalPart(), outMsg);
            final Class<?> returnType = method.getReturnType();
            if (!returnType.isAssignableFrom(void.class)) {
                final QName q = getOutPartName(op, method, -1);
                final QName q2 = getOutParameterName(op, method, -1);
                MessagePartInfo part = outMsg.addMessagePart(q);
                initializeParameter(part, method.getReturnType(), method.getGenericReturnType());
                if (!isRPC(method) && !isWrapped(method)) {
                    part.setProperty(ELEMENT_NAME, q2);
                }
                final Annotation[] annotations = method.getAnnotations();
                part.setProperty(METHOD_ANNOTATIONS, annotations);
                part.setProperty(PARAM_ANNOTATION, annotations);
                if (isHeader(method, -1)) {
                    part.setProperty(HEADER, Boolean.TRUE);
                    if (isRPC(method) || !isWrapped(method)) {
                        part.setElementQName(q2);
                    } else {
                        part.setProperty(ELEMENT_NAME, q2);
                    }
                }

                part.setIndex(0);
            }

            for (int j = 0; j < paramClasses.length; j++) {
                if (Exchange.class.equals(paramClasses[j])) {
                    continue;
                }
                if (isOutParam(method, j)) {
                    if (outMsg == null) {
                        outMsg = op.createMessage(createOutputMessageName(op, method),
                                                  MessageInfo.Type.OUTPUT);
                    }
                    QName q = getOutPartName(op, method, j);
                    QName q2 = getOutParameterName(op, method, j);

                    if (isInParam(method, j)) {
                        MessagePartInfo mpi = op.getInput().getMessagePartByIndex(j);
                        q = mpi.getName();
                        q2 = (QName)mpi.getProperty(ELEMENT_NAME);
                        if (q2 == null) {
                            q2 = mpi.getElementQName();
                        }
                    }

                    MessagePartInfo part = outMsg.addMessagePart(q);
                    part.setProperty(METHOD_PARAM_ANNOTATIONS, parAnnotations);
                    part.setProperty(PARAM_ANNOTATION, parAnnotations[j]);
                    initializeParameter(part, paramClasses[j], genParTypes[j]);
                    part.setIndex(j + 1);

                    if (!isRPC(method) && !isWrapped(method)) {
                        part.setProperty(ELEMENT_NAME, q2);
                    }

                    if (isInParam(method, j)) {
                        part.setProperty(MODE_INOUT, Boolean.TRUE);
                    }
                    if (isHeader(method, j)) {
                        part.setProperty(HEADER, Boolean.TRUE);
                        if (isRPC(method) || !isWrapped(method)) {
                            part.setElementQName(q2);
                        } else {
                            part.setProperty(ELEMENT_NAME, q2);
                        }
                    }
                }
            }
            sendEvent(Event.OPERATIONINFO_OUT_MESSAGE_SET, op, method, outMsg);
        }

        //setting the parameterOrder that
        //allows preservation of method signatures
        //when doing java->wsdl->java
        setParameterOrder(method, paramClasses, op);

        if (hasOut) {
            // Faults are only valid if not a one-way operation
            initializeFaults(intf, op, method);
        }
    }

    private void setParameterOrder(Method method, Class<?>[] paramClasses, OperationInfo op) {
        if (isRPC(method) || !isWrapped(method)) {
            List<String> paramOrdering = new LinkedList<>();
            boolean hasOut = false;
            for (int j = 0; j < paramClasses.length; j++) {
                if (Exchange.class.equals(paramClasses[j])) {
                    continue;
                }
                if (isInParam(method, j)) {
                    paramOrdering.add(getInPartName(op, method, j).getLocalPart());
                    if (isOutParam(method, j)) {
                        hasOut = true;
                    }
                } else if (isOutParam(method, j)) {
                    hasOut = true;
                    paramOrdering.add(getOutPartName(op, method, j).getLocalPart());
                }
            }
            if (!paramOrdering.isEmpty() && hasOut) {
                op.setParameterOrdering(paramOrdering);
            }
        }
    }


    protected void createInputWrappedMessageParts(OperationInfo op, Method method, MessageInfo inMsg) {
        String partName = null;
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            partName = c.getRequestWrapperPartName(op, method);
            if (partName != null) {
                break;
            }
        }
        if (partName == null) {
            partName = "parameters";
        }
        MessagePartInfo part = inMsg.addMessagePart(partName);
        part.setElement(true);
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            QName q = c.getRequestWrapperName(op, method);
            if (q != null) {
                part.setElementQName(q);
            }
        }
        if (part.getElementQName() == null) {
            part.setElementQName(inMsg.getName());
        } else if (!part.getElementQName().equals(op.getInput().getName())) {
            op.getInput().setName(part.getElementQName());
        }
        if (getRequestWrapper(method) != null) {
            part.setTypeClass(this.getRequestWrapper(method));
        } else if (getRequestWrapperClassName(method) != null) {
            part.setProperty("REQUEST.WRAPPER.CLASSNAME", getRequestWrapperClassName(method));
        }

        int partIdx = 0;
        int maxIdx = 0;
        for (MessagePartInfo mpart : op.getInput().getMessageParts()) {
            if (Boolean.TRUE.equals(mpart.getProperty(HEADER))) {
                int idx = mpart.getIndex();
                inMsg.addMessagePart(mpart);
                mpart.setIndex(idx);

                //make sure the header part and the wrapper part don't share the
                //same index.   We can move the wrapper part around a bit
                //if need be
                if (maxIdx < idx) {
                    maxIdx = idx;
                }
                if (idx == partIdx) {
                    maxIdx++;
                    partIdx = maxIdx;
                }
            }
        }
        part.setIndex(partIdx);

    }

    protected void createOutputWrappedMessageParts(OperationInfo op, Method method, MessageInfo outMsg) {
        String partName = null;
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            partName = c.getResponseWrapperPartName(op, method);
            if (partName != null) {
                break;
            }
        }
        for (MessagePartInfo mpart : op.getOutput().getMessageParts()) {
            if (Boolean.TRUE.equals(mpart.getProperty(HEADER))) {
                partName = "result";
                break;
            }
        }

        if (partName == null) {
            partName = "parameters";
        }

        MessagePartInfo part = outMsg.addMessagePart(partName);
        part.setElement(true);
        part.setIndex(0);
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            QName q = c.getResponseWrapperName(op, method);
            if (q != null) {
                part.setElementQName(q);
                break;
            }
        }

        if (part.getElementQName() == null) {
            part.setElementQName(outMsg.getName());
        } else if (!part.getElementQName().equals(op.getOutput().getName())) {
            op.getOutput().setName(part.getElementQName());
        }

        if (this.getResponseWrapper(method) != null) {
            part.setTypeClass(this.getResponseWrapper(method));
        } else if (getResponseWrapperClassName(method) != null) {
            part.setProperty("RESPONSE.WRAPPER.CLASSNAME", getResponseWrapperClassName(method));
        }

        for (MessagePartInfo mpart : op.getOutput().getMessageParts()) {
            if (Boolean.TRUE.equals(mpart.getProperty(HEADER))) {
                int idx = mpart.getIndex();
                outMsg.addMessagePart(mpart);
                mpart.setIndex(idx);
            }
        }
    }

    private static Class<?> createArrayClass(GenericArrayType atp) {
        Type tp = atp.getGenericComponentType();
        Class<?> rawClass = null;
        if (tp instanceof Class) {
            rawClass = (Class<?>)tp;
        } else if (tp instanceof GenericArrayType) {
            rawClass = createArrayClass((GenericArrayType)tp);
        } else if (tp instanceof ParameterizedType) {
            rawClass = (Class<?>)((ParameterizedType)tp).getRawType();
            if (List.class.isAssignableFrom(rawClass)) {
                rawClass = getClass(tp);
                rawClass = Array.newInstance(rawClass, 0).getClass();
            }
        }
        return Array.newInstance(rawClass, 0).getClass();
    }

    private static Class<?> getClass(Type paramType) {
        Class<?> rawClass = null;
        if (paramType instanceof Class) {
            rawClass = (Class<?>)paramType;
        } else if (paramType instanceof GenericArrayType) {
            rawClass = createArrayClass((GenericArrayType)paramType);
        } else if (paramType instanceof ParameterizedType) {
            rawClass = (Class<?>)((ParameterizedType)paramType).getRawType();
        }
        return rawClass;
    }


    protected void initializeParameter(MessagePartInfo part, Class<?> rawClass, Type type) {
        if (isHolder(rawClass, type)) {
            Type c = getHolderType(rawClass, type);
            if (c != null) {
                type = c;
                rawClass = getClass(type);
            }
        }
        if (type instanceof TypeVariable) {
            if (parameterizedTypes == null) {
                processParameterizedTypes();
            }
            TypeVariable<?> var = (TypeVariable<?>)type;
            final Object gd = var.getGenericDeclaration();
            Map<String, Class<?>> mp = parameterizedTypes.get(gd);
            if (mp != null) {
                Class<?> c = parameterizedTypes.get(gd).get(var.getName());
                if (c != null) {
                    rawClass = c;
                    type = c;
                    part.getMessageInfo().setProperty("parameterized", Boolean.TRUE);
                }
            }
        }
        part.setProperty(GENERIC_TYPE, type);
        // if rawClass is List<String>, it will be converted to array
        // and set it to type class
        if (Collection.class.isAssignableFrom(rawClass)) {
            part.setProperty(RAW_CLASS, rawClass);
        }
        part.setTypeClass(rawClass);

        if (part.getMessageInfo().getOperation().isUnwrapped()
            && Boolean.TRUE.equals(part.getProperty(HEADER))) {
            //header from the unwrapped operation, make sure the type is set for the
            //approriate header in the wrapped operation
            OperationInfo o = ((UnwrappedOperationInfo)part.getMessageInfo().getOperation())
                .getWrappedOperation();

            if (Boolean.TRUE.equals(part.getProperty(ReflectionServiceFactoryBean.MODE_OUT))
                || Boolean.TRUE.equals(part.getProperty(ReflectionServiceFactoryBean.MODE_INOUT))) {
                MessagePartInfo mpi = o.getOutput().getMessagePart(part.getName());
                if (mpi != null) {
                    mpi.setTypeClass(rawClass);
                    mpi.setProperty(GENERIC_TYPE, type);
                    if (Collection.class.isAssignableFrom(rawClass)) {
                        mpi.setProperty(RAW_CLASS, type);
                    }
                }
            }
            if (!Boolean.TRUE.equals(part.getProperty(ReflectionServiceFactoryBean.MODE_OUT))) {
                MessagePartInfo mpi = o.getInput().getMessagePart(part.getName());
                if (mpi != null) {
                    mpi.setTypeClass(rawClass);
                    mpi.setProperty(GENERIC_TYPE, type);
                    if (Collection.class.isAssignableFrom(rawClass)) {
                        mpi.setProperty(RAW_CLASS, type);
                    }
                }
            }
        }
    }


    public QName getServiceQName() {
        return getServiceQName(true);
    }
    public QName getServiceQName(boolean lookup) {
        if (serviceName == null && lookup) {
            serviceName = new QName(getServiceNamespace(), getServiceName());
        }

        return serviceName;
    }

    public QName getEndpointName() {
        return getEndpointName(true);
    }
    public QName getEndpointName(boolean lookup) {
        if (endpointName != null || !lookup) {
            return endpointName;
        }

        for (AbstractServiceConfiguration c : serviceConfigurations) {
            QName name = c.getEndpointName();
            if (name != null) {
                endpointName = name;
                return name;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    public EndpointInfo getEndpointInfo() {
        return getService().getEndpointInfo(getEndpointName());
    }

    public void setEndpointName(QName en) {
        this.endpointName = en;
    }

    protected String getServiceName() {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            String name = c.getServiceName();
            if (name != null) {
                return name;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected String getServiceNamespace() {
        if (serviceName != null) {
            return serviceName.getNamespaceURI();
        }

        for (AbstractServiceConfiguration c : serviceConfigurations) {
            String name = c.getServiceNamespace();
            if (name != null) {
                return name;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    public QName getInterfaceName() {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            QName name = c.getInterfaceName();
            if (name != null) {
                return name;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected boolean isValidMethod(final Method method) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Boolean b = c.isOperation(method);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return true;
    }

    public boolean isHolder(Class<?> cls, Type type) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Boolean b = c.isHolder(cls, type);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return false;
    }

    public Type getHolderType(Class<?> cls, Type type) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Type b = c.getHolderType(cls, type);
            if (b != null) {
                return b;
            }
        }
        return null;
    }

    protected boolean isWrapped(final Method method) {
        Boolean b = wrappedCache.get(method);
        if (b == null) {
            if (isRPC(method)) {
                wrappedCache.put(method, Boolean.FALSE);
                return false;
            }

            for (AbstractServiceConfiguration c : serviceConfigurations) {
                b = c.isWrapped(method);
                if (b != null) {
                    wrappedCache.put(method, b);
                    return b.booleanValue();
                }
            }

            wrappedCache.put(method, Boolean.TRUE);
            return true;
        }
        return b;
    }

    protected boolean isMatchOperation(String methodNameInClass, String methodNameInWsdl) {
        // checks to make sure the operation names match ignoring the case of the first character
        boolean ret = false;
        String initOfMethodInClass = methodNameInClass.substring(0, 1);
        String initOfMethodInWsdl = methodNameInWsdl.substring(0, 1);
        if (initOfMethodInClass.equalsIgnoreCase(initOfMethodInWsdl)
            && methodNameInClass.substring(1, methodNameInClass.length())
                .equals(methodNameInWsdl.substring(1, methodNameInWsdl.length()))) {
            ret = true;
        }
        return ret;
    }

    protected boolean isOutParam(Method method, int j) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Boolean b = c.isOutParam(method, j);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return true;
    }

    protected boolean isInParam(Method method, int j) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Boolean b = c.isInParam(method, j);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return true;
    }

    protected boolean isInOutParam(Method method, int j) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Boolean b = c.isInOutParam(method, j);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return true;
    }

    protected QName getInputMessageName(final OperationInfo op, final Method method) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            QName q = c.getInputMessageName(op, method);
            if (q != null) {
                return q;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected QName createOutputMessageName(final OperationInfo op, final Method method) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            QName q = c.getOutputMessageName(op, method);
            if (q != null) {
                return q;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected boolean hasOutMessage(Method m) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Boolean b = c.hasOutMessage(m);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return true;
    }

    protected void initializeFaults(final InterfaceInfo service,
                                    final OperationInfo op, final Method method) {
        // Set up the fault messages
        final Class<?>[] exceptionClasses = method.getExceptionTypes();
        for (int i = 0; i < exceptionClasses.length; i++) {
            Class<?> exClazz = exceptionClasses[i];

            // Ignore XFireFaults because they don't need to be declared
            if (Fault.class.isAssignableFrom(exClazz)
                || exClazz.equals(RuntimeException.class) || exClazz.equals(Throwable.class)) {
                continue;
            }

            addFault(service, op, exClazz);
        }
    }

    protected void initializeDefaultInterceptors() {
        super.initializeDefaultInterceptors();

        initializeFaultInterceptors();
    }

    protected void initializeFaultInterceptors() {
        getService().getOutFaultInterceptors().add(new FaultOutInterceptor());
    }

    protected FaultInfo addFault(final InterfaceInfo service, final OperationInfo op,
                                 Class<?> exClass) {
        Class<?> beanClass = getBeanClass(exClass);
        if (beanClass == null) {
            return null;
        }
        String faultMsgName = null;
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            faultMsgName = c.getFaultMessageName(op, exClass, beanClass);
            if (faultMsgName != null) {
                break;
            }
        }
        if (faultMsgName == null) {
            faultMsgName = exClass.getSimpleName();
        }

        QName faultName = getFaultName(service, op, exClass, beanClass);
        FaultInfo fi = op.addFault(new QName(op.getName().getNamespaceURI(), faultMsgName),
                                   new QName(op.getName().getNamespaceURI(), faultMsgName));
        fi.setProperty(Class.class.getName(), exClass);
        fi.setProperty("elementName", faultName);
        MessagePartInfo mpi = fi.addMessagePart(new QName(faultName.getNamespaceURI(),
                                                          exClass.getSimpleName()));
        mpi.setElementQName(faultName);
        mpi.setTypeClass(beanClass);
        sendEvent(Event.OPERATIONINFO_FAULT, op, exClass, fi);
        return fi;
    }

    protected void createFaultForException(Class<?> exClass, FaultInfo fi) {
        Field[] fields = exClass.getDeclaredFields();
        for (Field field : fields) {
            MessagePartInfo mpi = fi
                .addMessagePart(new QName(fi.getName().getNamespaceURI(), field.getName()));
            mpi.setProperty(Class.class.getName(), field.getType());
        }

        MessagePartInfo mpi = fi.addMessagePart(new QName(fi.getName().getNamespaceURI(), "message"));
        mpi.setProperty(Class.class.getName(), String.class);
    }

    protected Class<?> getBeanClass(Class<?> exClass) {
        if (java.rmi.RemoteException.class.isAssignableFrom(exClass)) {
            return null;
        }

        if (FaultOutInterceptor.FaultInfoException.class.isAssignableFrom(exClass)) {
            try {
                Method m = exClass.getMethod("getFaultInfo");
                return m.getReturnType();
            } catch (SecurityException | NoSuchMethodException e) {
                throw new ServiceConstructionException(e);
            }
        }

        return exClass;
    }

    protected QName getFaultName(InterfaceInfo service, OperationInfo o,
                                 Class<?> exClass, Class<?> beanClass) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            QName q = c.getFaultName(service, o, exClass, beanClass);
            if (q != null) {
                return q;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected String getAction(OperationInfo op, Method method) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            String s = c.getAction(op, method);
            if (s != null) {
                return s;
            }
        }
        return "";
    }

    public boolean isHeader(Method method, int j) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Boolean b = c.isHeader(method, j);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return true;
    }

    /**
     * Creates a name for the operation from the method name. If an operation
     * with that name already exists, a name is create by appending an integer
     * to the end. I.e. if there is already two methods named
     * <code>doSomething</code>, the first one will have an operation name of
     * "doSomething" and the second "doSomething1".
     *
     * @param service
     * @param method
     */
    protected QName getOperationName(InterfaceInfo service, Method method) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            QName s = c.getOperationName(service, method);
            if (s != null) {
                return s;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected boolean isAsync(final Method method) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Boolean b = c.isAsync(method);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return true;
    }

    protected QName getInPartName(final OperationInfo op, final Method method, final int paramNumber) {
        if (paramNumber == -1) {
            return null;
        }

        if (isWrapped(method) && !isHeader(method, paramNumber)) {
            return getInParameterName(op, method, paramNumber);
        }

        for (AbstractServiceConfiguration c : serviceConfigurations) {
            QName q = c.getInPartName(op, method, paramNumber);
            if (q != null) {
                return q;
            }

        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected QName getInParameterName(final OperationInfo op, final Method method, final int paramNumber) {
        if (paramNumber == -1) {
            return null;
        }
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            QName q = c.getInParameterName(op, method, paramNumber);
            if (q != null) {
                return q;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected QName getOutParameterName(final OperationInfo op, final Method method, final int paramNumber) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            QName q = c.getOutParameterName(op, method, paramNumber);
            if (q != null) {
                return q;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected QName getOutPartName(final OperationInfo op, final Method method, final int paramNumber) {
        if (isWrapped(method)) {
            return getOutParameterName(op, method, paramNumber);
        }

        for (AbstractServiceConfiguration c : serviceConfigurations) {
            QName q = c.getOutPartName(op, method, paramNumber);
            if (q != null) {
                return q;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected Class<?> getResponseWrapper(Method selected) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Class<?> cls = c.getResponseWrapper(selected);
            if (cls != null) {
                return cls;
            }
        }
        return null;
    }

    protected String getResponseWrapperClassName(Method selected) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            String cls = c.getResponseWrapperClassName(selected);
            if (cls != null) {
                return cls;
            }
        }
        return null;
    }

    protected Class<?> getRequestWrapper(Method selected) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Class<?> cls = c.getRequestWrapper(selected);
            if (cls != null) {
                return cls;
            }
        }
        return null;
    }

    protected String getRequestWrapperClassName(Method selected) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            String cls = c.getRequestWrapperClassName(selected);
            if (cls != null) {
                return cls;
            }
        }
        return null;
    }

    public Boolean isWrapperPartQualified(MessagePartInfo mpi) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Boolean b = c.isWrapperPartQualified(mpi);
            if (b != null) {
                return b;
            }
        }
        return null;
    }
    public boolean isWrapperPartNillable(MessagePartInfo mpi) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Boolean b = c.isWrapperPartNillable(mpi);
            if (b != null) {
                return b;
            }
        }
        return false;
    }
    public long getWrapperPartMaxOccurs(MessagePartInfo mpi) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Long b = c.getWrapperPartMaxOccurs(mpi);
            if (b != null) {
                return b;
            }
        }
        return 1;
    }
    public long getWrapperPartMinOccurs(MessagePartInfo mpi) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Long b = c.getWrapperPartMinOccurs(mpi);
            if (b != null) {
                return b;
            }
        }
        return 1;
    }

    public MethodDispatcher getMethodDispatcher() {
        return methodDispatcher;
    }
    protected void setMethodDispatcher(MethodDispatcher m) {
        methodDispatcher = m;
    }

    public List<AbstractServiceConfiguration> getConfigurations() {
        return serviceConfigurations;
    }

    public void setConfigurations(List<AbstractServiceConfiguration> configurations) {
        this.serviceConfigurations = configurations;
    }

    public Class<?> getServiceClass() {
        return serviceClass;
    }
    private void processParameterizedTypes() {
        parameterizedTypes = new HashMap<>();
        if (serviceClass.isInterface()) {
            processTypes(serviceClass, serviceType);
        } else {
            final Class<?>[] interfaces = serviceClass.getInterfaces();
            final Type[] genericInterfaces = serviceClass.getGenericInterfaces();
            for (int x = 0; x < interfaces.length; x++) {
                processTypes(interfaces[x], genericInterfaces[x]);
            }
            processTypes(serviceClass.getSuperclass(), serviceClass.getGenericSuperclass());
        }
    }
    protected void processTypes(Class<?> sc, Type tp) {
        if (tp instanceof ParameterizedType) {
            ParameterizedType ptp = (ParameterizedType)tp;
            Type c = ptp.getRawType();
            Map<String, Class<?>> m = new HashMap<>();
            parameterizedTypes.put(c, m);
            final Type[] ptpActualTypeArgs = ptp.getActualTypeArguments();
            final TypeVariable<?>[] scTypeArgs = sc.getTypeParameters();
            for (int x = 0; x < ptpActualTypeArgs.length; x++) {
                Type t = ptpActualTypeArgs[x];
                TypeVariable<?> tv = scTypeArgs[x];
                if (t instanceof Class) {
                    m.put(tv.getName(), (Class<?>)t);
                }
            }
        }
    }
    public void setServiceType(ParameterizedType servicetype) {
        serviceType = servicetype;
    }
    public void setServiceClass(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
        checkServiceClassAnnotations(serviceClass);
    }
    protected void checkServiceClassAnnotations(Class<?> sc) {
        Annotation[] anns = serviceClass.getAnnotations();
        if (anns != null) {
            for (Annotation ann : anns) {
                String pkg = ann.annotationType().getPackage().getName();
                if ("javax.xml.ws".equals(pkg)
                    || "javax.jws".equals(pkg)) {

                    LOG.log(Level.WARNING, "JAXWS_ANNOTATION_FOUND", serviceClass.getName());
                    return;
                }
            }
        }
        for (Method m : serviceClass.getMethods()) {
            anns = m.getAnnotations();
            if (anns != null) {
                for (Annotation ann : anns) {
                    String pkg = ann.annotationType().getPackage().getName();
                    if ("javax.xml.ws".equals(pkg)
                        || "javax.jws".equals(pkg)) {

                        LOG.log(Level.WARNING, "JAXWS_ANNOTATION_FOUND", serviceClass.getName());
                        return;
                    }
                }
            }
        }
    }

    public String getWsdlURL() {
        if (wsdlURL == null) {
            for (AbstractServiceConfiguration c : serviceConfigurations) {
                wsdlURL = c.getWsdlURL();
                if (wsdlURL != null) {
                    break;
                }
            }
            if (null == wsdlURL && getBus() != null) {
                ServiceContractResolverRegistry registry = getBus()
                    .getExtension(ServiceContractResolverRegistry.class);
                if (null != registry) {
                    URI uri = registry.getContractLocation(this.getServiceQName());
                    if (null != uri) {
                        try {
                            wsdlURL = uri.toURL().toString();
                        } catch (MalformedURLException e) {
                            LOG.log(Level.FINE, "resolve qname failed", this.getServiceQName());
                        }
                    }
                }
            }
            if (wsdlURL != null) {
                // create a unique string so if its an interned string (like
                // from an annotation), caches will clear
                wsdlURL = new String(wsdlURL);
            }
        }

        return wsdlURL;
    }

    public void setWsdlURL(String wsdlURL) {
        // create a unique string so if its an interned string (like
        // from an annotation), caches will clear
        // this.wsdlURL = new String(wsdlURL);  Liberty change: line removed
        this.wsdlURL = wsdlURL.toString();  // Liberty change: line added
    }

    public void setWsdlURL(URL wsdlURL) {
        setWsdlURL(wsdlURL.toString());
    }

    public List<AbstractServiceConfiguration> getServiceConfigurations() {
        return serviceConfigurations;
    }

    public void setServiceConfigurations(List<AbstractServiceConfiguration> serviceConfigurations) {
        this.serviceConfigurations = serviceConfigurations;
    }

    public void setServiceName(QName serviceName) {
        this.serviceName = serviceName;
    }

    public Invoker getInvoker() {
        return invoker;
    }

    public void setInvoker(Invoker invoker) {
        this.invoker = invoker;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public List<String> getIgnoredClasses() {
        return ignoredClasses;
    }

    public void setIgnoredClasses(List<String> ignoredClasses) {
        this.ignoredClasses = ignoredClasses;
    }

    protected Set<Class<?>> getExtraClass() {
        return null;
    }

    public boolean isWrapped() {
        if (this.wrappedStyle != null) {
            defWrappedCache = wrappedStyle;
        }
        if (this.defWrappedCache == null) {
            for (AbstractServiceConfiguration c : serviceConfigurations) {
                defWrappedCache = c.isWrapped();
                if (defWrappedCache != null) {
                    return defWrappedCache;
                }
            }
            defWrappedCache = Boolean.TRUE;
        }
        return defWrappedCache;
    }

    public String getStyle() {
        if (styleCache == null) {
            for (AbstractServiceConfiguration c : serviceConfigurations) {
                styleCache = c.getStyle();
                if (styleCache != null) {
                    return styleCache;
                }
            }
            styleCache = "document";
        }
        return styleCache;
    }

    public boolean isRPC(Method method) {
        Boolean b = isRpcCache.get(method);
        if (b == null) {
            for (AbstractServiceConfiguration c : serviceConfigurations) {
                b = c.isRPC(method);
                if (b != null) {
                    isRpcCache.put(method, b);
                    return b.booleanValue();
                }
            }
            b = "rpc".equals(getStyle());
            isRpcCache.put(method, b);
        }
        return b;
    }

    public void setWrapped(boolean style) {
        this.wrappedStyle = style;
    }

    /**
     * Returns non-null if wrapped mode was explicitely disabled or enabled.
     */
    public Boolean getWrapped() {
        return this.wrappedStyle;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public List<Method> getIgnoredMethods() {
        return ignoredMethods;
    }

    public void setIgnoredMethods(List<Method> ignoredMethods) {
        this.ignoredMethods = ignoredMethods;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public void setFeatures(List<? extends Feature> features2) {
        this.features = CastUtils.cast(features2);
    }

    private boolean isValidate() {
        // return validate || DO_VALIDATE Liberty change: line removed
        return validate || SystemPropertyAction.getProperty("cxf.validateServiceSchemas", "false").equals("true");  // Liberty change: line is added
    }

    /**
     * If 'validate' is true, this class will validate the service. It will report problems
     * with the service model and the XML schema for the service.
     * @param validate
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public void setSchemaLocations(List<String> schemaLocations) {
        this.schemaLocations = schemaLocations;
    }
}

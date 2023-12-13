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

package org.apache.cxf.wsdl11;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Import;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.WSDLElement;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.AbstractBindingFactory;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.service.model.AbstractMessageContainer;
import org.apache.cxf.service.model.AbstractPropertiesHolder;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.DescriptionInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.ServiceSchemaInfo;
import org.apache.cxf.service.model.UnwrappedOperationInfo;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.wsdl.JAXBExtensibilityElement;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl.binding.WSDLBindingFactory;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaType;

import static org.apache.cxf.helpers.CastUtils.cast;

public class WSDLServiceBuilder {

    public static final String WSDL_SCHEMA_LIST = WSDLServiceBuilder.class.getName() + ".SCHEMA";
    public static final String WSDL_DEFINITION = WSDLServiceBuilder.class.getName() + ".DEFINITION";
    public static final String WSDL_SERVICE = WSDLServiceBuilder.class.getName() + ".SERVICE";
    public static final String WSDL_PORTTYPE = WSDLServiceBuilder.class.getName() + ".WSDL_PORTTYPE";
    public static final String WSDL_PORT = WSDLServiceBuilder.class.getName() + ".PORT";
    public static final String WSDL_BINDING = WSDLServiceBuilder.class.getName() + ".BINDING";
    public static final String WSDL_SCHEMA_ELEMENT_LIST = WSDLServiceBuilder.class.getName()
                                                          + ".SCHEMA_ELEMENTS";
    public static final String WSDL_OPERATION = WSDLServiceBuilder.class.getName() + ".OPERATION";
    public static final String WSDL_BINDING_OPERATION = WSDLServiceBuilder.class.getName()
                                                        + ".BINDING_OPERATION";

    private static final Logger LOG = LogUtils.getL7dLogger(WSDLServiceBuilder.class);
    private Bus bus;
    private Map<String, Element> schemaList = new HashMap<>();
    private boolean recordOriginal = true;
    private boolean allowRefs;
    private boolean ignoreUnknownBindings;
    private Level unwrapLogLevel = Level.FINE;

    public WSDLServiceBuilder(Bus bus) {
        this.bus = bus;
    }
    public WSDLServiceBuilder(Bus bus, boolean record) {
        this.bus = bus;
        recordOriginal = record;
    }

    public void setUnwrapLogLevel(Level l) {
        unwrapLogLevel = l;
    }
    public void setIgnoreUnknownBindings(boolean b) {
        ignoreUnknownBindings = b;
    }
    public void setAllowElementRefs(boolean b) {
        allowRefs = b;
    }

    private void copyExtensors(AbstractPropertiesHolder info, List<?> extList) {
        if (info != null) {
            for (ExtensibilityElement ext : cast(extList, ExtensibilityElement.class)) {
                Object o = ext;
                if (ext instanceof JAXBExtensibilityElement) {
                    o = ((JAXBExtensibilityElement)ext).getValue();
                }
                if (!info.containsExtensor(o)) {
                    info.addExtensor(o);
                }
            }
        }
    }

    private void copyExtensionAttributes(AbstractPropertiesHolder info,
                                         javax.wsdl.extensions.AttributeExtensible ae) {
        Map<QName, Object> attrs = CastUtils.cast(ae.getExtensionAttributes());
        if (!attrs.isEmpty()) {
            info.setExtensionAttributes(attrs);
        }
    }

    private void copyDocumentation(AbstractPropertiesHolder info, WSDLElement el) {
        if (el.getDocumentationElement() != null) {
            String doc = DOMUtils.getRawContent(el.getDocumentationElement());
            info.setDocumentation(doc);
        }
    }

    public List<ServiceInfo> buildServices(Definition d) {
        DescriptionInfo description = new DescriptionInfo();
        if (recordOriginal) {
            description.setProperty(WSDL_DEFINITION, d);
        }
        description.setName(d.getQName());
        description.setBaseURI(d.getDocumentBaseURI());
        copyExtensors(description, d.getExtensibilityElements());
        copyExtensionAttributes(description, d);

        List<ServiceInfo> serviceList = new ArrayList<>();
        for (java.util.Iterator<QName> ite = CastUtils.cast(d.getServices().keySet().iterator()); ite
            .hasNext();) {
            QName qn = ite.next();
            serviceList.addAll(buildServices(d, qn, null, description));
        }
        return serviceList;
    }

    public List<ServiceInfo> buildServices(Definition d, QName name) {
        return buildServices(d, name, null, null);
    }
    public List<ServiceInfo> buildServices(Definition d, QName name, QName endpointName) {
        return buildServices(d, name, endpointName, null);
    }

    private List<ServiceInfo> buildServices(Definition d,
                                            QName name,
                                            QName endpointName,
                                            DescriptionInfo description) {
        Service service = d.getService(name);
        if (service == null) {
            org.apache.cxf.common.i18n.Message msg =
                new org.apache.cxf.common.i18n.Message("MISSING_SERVICE",
                                                       LOG,
                                                       name);
            throw new WSDLRuntimeException(msg);
        }
        return buildServices(d, service, endpointName, description);
    }

    public List<ServiceInfo> buildServices(Definition def, Service serv) {
        return buildServices(def, serv, null, null);
    }
    public List<ServiceInfo> buildServices(Definition def, Service serv, QName endpointName) {
        return buildServices(def, serv, endpointName, null);
    }

    public List<ServiceInfo> buildMockServices(Definition d) {
        List<ServiceInfo> serviceList = new ArrayList<>();
        List<Definition> defList = new ArrayList<>();
        defList.add(d);
        parseImports(d, defList);
        for (Definition def : defList) {

            for (Iterator<?> ite = def.getPortTypes().entrySet().iterator(); ite.hasNext();) {
                Entry<?, ?> entry = (Entry<?, ?>)ite.next();
                PortType portType = def.getPortType((QName)entry.getKey());
                ServiceInfo serviceInfo = this.buildMockService(def, portType);
                serviceList.add(serviceInfo);

                for (Iterator<?> it2 = d.getAllBindings().values().iterator(); it2.hasNext();) {
                    Binding b = (Binding)it2.next();
                    if (b.getPortType() == portType) {
                        this.buildBinding(serviceInfo, b);
                        break;
                    }
                }
            }

            if (def.getPortTypes().isEmpty()) {

                DescriptionInfo description = new DescriptionInfo();
                if (recordOriginal) {
                    description.setProperty(WSDL_DEFINITION, def);
                }
                description.setName(def.getQName());
                description.setBaseURI(def.getDocumentBaseURI());
                copyExtensors(description, def.getExtensibilityElements());
                copyExtensionAttributes(description, def);

                ServiceInfo service = new ServiceInfo();
                service.setDescription(description);
                if (recordOriginal) {
                    service.setProperty(WSDL_DEFINITION, def);
                }
                getSchemas(def, service);

                service.setProperty(WSDL_SCHEMA_ELEMENT_LIST, this.schemaList);
                serviceList.add(service);
            }
        }
        return serviceList;
    }

    public ServiceInfo buildMockService(Definition def, PortType p) {
        DescriptionInfo description = new DescriptionInfo();
        if (recordOriginal) {
            description.setProperty(WSDL_DEFINITION, def);
        }
        description.setName(def.getQName());
        description.setBaseURI(def.getDocumentBaseURI());
        copyExtensors(description, def.getExtensibilityElements());
        copyExtensionAttributes(description, def);

        ServiceInfo service = new ServiceInfo();
        service.setDescription(description);
        if (recordOriginal) {
            service.setProperty(WSDL_DEFINITION, def);
        }
        getSchemas(def, service);

        service.setProperty(WSDL_SCHEMA_ELEMENT_LIST, this.schemaList);

        buildInterface(service, p);

        return service;
    }

    private List<ServiceInfo> buildServices(Definition def,
                                            Service serv,
                                            QName endpointName,
                                            DescriptionInfo d) {
        Map<QName, ServiceInfo> services = new LinkedHashMap<>();

        DescriptionInfo description = d;
        if (null == description) {
            description = new DescriptionInfo();
            if (recordOriginal) {
                description.setProperty(WSDL_DEFINITION, def);
            }
            description.setName(def.getQName());
            description.setBaseURI(def.getDocumentBaseURI());
            copyExtensors(description, def.getExtensibilityElements());
            copyExtensionAttributes(description, def);

            Set<Definition> done = new HashSet<>();
            done.add(def);
            Collection<List<Import>> values = CastUtils.cast(def.getImports().values());
            for (List<Import> imports : values) {
                for (Import imp : imports) {
                    if (!done.contains(imp.getDefinition())) {
                        done.add(imp.getDefinition());
                        copyExtensors(description, imp.getExtensibilityElements());
                        copyExtensionAttributes(description, imp);
                        copyExtensors(description, imp.getDefinition().getExtensibilityElements());
                        copyExtensionAttributes(description, imp.getDefinition());
                    }
                }
            }
        }
        for (Port port : cast(serv.getPorts().values(), Port.class)) {
            if (endpointName != null
                && !endpointName.getLocalPart().equals(port.getName())) {
                continue;
            }
            Binding binding = port.getBinding();
            PortType bindingPt = binding.getPortType();
            if (bindingPt == null) {
                org.apache.cxf.common.i18n.Message msg = new
                    org.apache.cxf.common.i18n.Message("BINDING_MISSING_TYPE",
                                                       LOG,
                                                       binding.getQName());
                throw new WSDLRuntimeException(msg);
            }

            //TODO: wsdl4j's bug. if there is recursive import,
            //wsdl4j can not get operation input message
            PortType pt = def.getPortType(bindingPt.getQName());
            if (pt == null) {
                pt = bindingPt;
            }
            ServiceInfo service = services.get(pt.getQName());
            if (service == null) {
                service = new ServiceInfo();
                service.setDescription(description);
                description.getDescribed().add(service);
                if (recordOriginal) {
                    service.setProperty(WSDL_DEFINITION, def);
                    service.setProperty(WSDL_SERVICE, serv);
                }
                getSchemas(def, service);
                copyDocumentation(service, serv);
                service.setProperty(WSDL_SCHEMA_ELEMENT_LIST, this.schemaList);
                service.setTargetNamespace(def.getTargetNamespace());
                service.setName(serv.getQName());
                copyExtensors(service, serv.getExtensibilityElements());
                copyExtensionAttributes(service, serv);

                buildInterface(service, pt);

                services.put(pt.getQName(), service);
            }

            BindingInfo bi = service.getBinding(binding.getQName());
            if (bi == null) {
                bi = buildBinding(service, binding);
            }
            buildEndpoint(service, bi, port);
        }

        return new ArrayList<>(services.values());
    }


    private void getSchemas(Definition def, ServiceInfo serviceInfo) {
        ServiceSchemaInfo serviceSchemaInfo = null;
        WSDLManager wsdlManager = bus.getExtension(WSDLManager.class);
        if (wsdlManager != null) {
            serviceSchemaInfo = wsdlManager.getSchemasForDefinition(def);
        }

        if (serviceSchemaInfo == null) {
            SchemaUtil schemaUtil = new SchemaUtil(bus, this.schemaList);
            schemaUtil.getSchemas(def, serviceInfo);
            serviceSchemaInfo = new ServiceSchemaInfo();
            serviceSchemaInfo.setSchemaElementList(this.schemaList);
            serviceSchemaInfo.setSchemaCollection(serviceInfo.getXmlSchemaCollection());
            serviceSchemaInfo.setSchemaInfoList(serviceInfo.getSchemas());
            if (wsdlManager != null) {
                wsdlManager.putSchemasForDefinition(def, serviceSchemaInfo);
            }
        } else {
            serviceInfo.setServiceSchemaInfo(serviceSchemaInfo);
            schemaList.putAll(serviceSchemaInfo.getSchemaElementList());
        }
    }


    private void parseImports(Definition def, List<Definition> defList) {
        List<Import> importList = new ArrayList<>();

        Collection<List<Import>> ilist = cast(def.getImports().values());
        for (List<Import> list : ilist) {
            importList.addAll(list);
        }
        for (Import impt : importList) {
            if (!defList.contains(impt.getDefinition())) {
                defList.add(impt.getDefinition());
                parseImports(impt.getDefinition(), defList);
            }
        }
    }

    public EndpointInfo buildEndpoint(ServiceInfo service, BindingInfo bi, Port port) {
        List<?> elements = port.getExtensibilityElements();
        String ns = null;

        DestinationFactory factory = null;
        EndpointInfo ei = null;

        if (null != elements && !elements.isEmpty()) {
            for (ExtensibilityElement el : CastUtils.cast(elements, ExtensibilityElement.class)) {
                ns = el.getElementType().getNamespaceURI();
                try {
                    factory = bus.getExtension(DestinationFactoryManager.class).getDestinationFactory(ns);
                } catch (BusException e) {
                    // do nothing
                }
                if (factory != null) {
                    break;
                }
            }
            if (factory == null) {
                ns = ((ExtensibilityElement)elements.get(0)).getElementType().getNamespaceURI();
            }
        }

        if (factory == null) { // get the transport id from bindingInfo
            elements = port.getBinding().getExtensibilityElements();
            if (null != elements && !elements.isEmpty()) {
                for (ExtensibilityElement el : CastUtils.cast(elements, ExtensibilityElement.class)) {
                    if (el instanceof SOAPBinding) {
                        ns = ((SOAPBinding)el).getTransportURI();
                        break;
                    } else if (el instanceof SOAP12Binding) {
                        ns = ((SOAP12Binding)el).getTransportURI();
                        break;
                        // TODO: this is really ugly, but how to link between
                        // this binding and this transport ?
                    }
                }
            }
            if (ns == null) {
                if (ignoreUnknownBindings) {
                    return null;
                }
                org.apache.cxf.common.i18n.Message msg = new
                    org.apache.cxf.common.i18n.Message("MISSING_DESTINATION_FACTORY",
                                                       LOG,
                                                       port.getName());
                throw new WSDLRuntimeException(msg);
            }
            try {
                factory = bus.getExtension(DestinationFactoryManager.class).getDestinationFactory(ns);
            } catch (BusException e) {
                // do nothing
            }
        }
        if (factory instanceof WSDLEndpointFactory) {
            WSDLEndpointFactory wFactory = (WSDLEndpointFactory)factory;
            ei = wFactory.createEndpointInfo(bus, service, bi,
                                             port.getExtensibilityElements());
        }

        if (ei == null) {
            ei = new EndpointInfo(service, ns);
        }
        copyDocumentation(ei, port);
        ei.setName(new QName(service.getName().getNamespaceURI(), port.getName()));
        ei.setBinding(bi);
        copyExtensors(ei, port.getExtensibilityElements());
        copyExtensionAttributes(ei, port);

        service.addEndpoint(ei);
        DescriptionInfo d = service.getDescription();
        if (null != d) {
            d.getDescribed().add(ei);
        }
        return ei;
    }

    public BindingInfo buildBinding(ServiceInfo service, Binding binding) {
        BindingInfo bi = null;
        StringBuilder ns = new StringBuilder(100);
        BindingFactory factory = WSDLServiceUtils.getBindingFactory(binding, bus, ns);
        if (factory instanceof WSDLBindingFactory) {
            WSDLBindingFactory wFactory = (WSDLBindingFactory)factory;
            bi = wFactory.createBindingInfo(service, binding, ns.toString());
            copyExtensors(bi, binding.getExtensibilityElements());
            copyExtensionAttributes(bi, binding);
        }
        if (bi == null) {
            boolean onlyExtensors = false;
            if (factory instanceof AbstractBindingFactory) {
                bi = ((AbstractBindingFactory)factory).createBindingInfo(service, ns.toString(), null);
                onlyExtensors = true;
            } else {
                bi = new BindingInfo(service, ns.toString());
            }
            bi.setName(binding.getQName());
            copyExtensors(bi, binding.getExtensibilityElements());
            copyExtensionAttributes(bi, binding);

            for (BindingOperation bop : cast(binding.getBindingOperations(), BindingOperation.class)) {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("binding operation name is " + bop.getName());
                }
                String inName = null;
                String outName = null;
                if (bop.getBindingInput() != null) {
                    inName = bop.getBindingInput().getName();
                }
                if (bop.getBindingOutput() != null) {
                    outName = bop.getBindingOutput().getName();
                }
                final BindingOperationInfo bop2;
                if (onlyExtensors) {
                    bop2 = bi.getOperation(new QName(binding.getQName().getNamespaceURI(),
                                                       bop.getName()));
                } else {
                    bop2 = bi.buildOperation(new QName(binding.getQName().getNamespaceURI(),
                                                       bop.getName()), inName, outName);
                    if (bop2 != null) {
                        bi.addOperation(bop2);
                    }
                }
                if (bop2 != null) {

                    copyExtensors(bop2, bop.getExtensibilityElements());
                    copyExtensionAttributes(bop2, bop);
                    if (bop.getBindingInput() != null) {
                        copyExtensors(bop2.getInput(), bop.getBindingInput().getExtensibilityElements());
                        copyExtensionAttributes(bop2.getInput(), bop.getBindingInput());
                        handleHeader(bop2.getInput());
                    }
                    if (bop.getBindingOutput() != null) {
                        copyExtensors(bop2.getOutput(), bop.getBindingOutput().getExtensibilityElements());
                        copyExtensionAttributes(bop2.getOutput(), bop.getBindingOutput());
                        handleHeader(bop2.getOutput());
                    }
                    for (BindingFault f : cast(bop.getBindingFaults().values(), BindingFault.class)) {
                        BindingFaultInfo bif = bop2.getFault(new QName(service.getTargetNamespace(), f
                            .getName()));
                        copyExtensors(bif, bop.getBindingFault(f.getName()).getExtensibilityElements());
                        copyExtensionAttributes(bif, bop.getBindingFault(f.getName()));
                    }
                }

            }
        }

        service.addBinding(bi);
        DescriptionInfo d = service.getDescription();
        if (null != d) {
            d.getDescribed().add(bi);
        }
        return bi;
    }

    private void handleHeader(BindingMessageInfo bindingMessageInfo) {
        // mark all message part which should be in header
        List<ExtensibilityElement> extensiblilityElement = bindingMessageInfo
            .getExtensors(ExtensibilityElement.class);
        // for non-soap binding, the extensiblilityElement could be null
        if (extensiblilityElement == null) {
            return;
        }
        // for (ExtensibilityElement element : extensiblilityElement) {
        // LOG.info("the extensibility is " + element.getClass().getName());
        // if (element instanceof SOAPHeader) {
        // LOG.info("the header is " + ((SOAPHeader)element).getPart());
        // }
        // }
    }

    public void buildInterface(ServiceInfo si, PortType p) {
        InterfaceInfo inf = si.createInterface(p.getQName());
        DescriptionInfo d = si.getDescription();
        if (null != d) {
            d.getDescribed().add(inf);
        }
        copyDocumentation(inf, p);
        this.copyExtensors(inf, p.getExtensibilityElements());
        this.copyExtensionAttributes(inf, p);
        if (recordOriginal) {
            inf.setProperty(WSDL_PORTTYPE, p);
        }
        for (Operation op : cast(p.getOperations(), Operation.class)) {
            buildInterfaceOperation(inf, op);
        }

    }

    private void buildInterfaceOperation(InterfaceInfo inf, Operation op) {
        OperationInfo opInfo = inf.addOperation(new QName(inf.getName().getNamespaceURI(), op.getName()));
        if (recordOriginal) {
            opInfo.setProperty(WSDL_OPERATION, op);
        }
        copyDocumentation(opInfo, op);
        List<String> porderList = CastUtils.cast((List<?>)op.getParameterOrdering());
        opInfo.setParameterOrdering(porderList);
        this.copyExtensors(opInfo, op.getExtensibilityElements());
        this.copyExtensionAttributes(opInfo, op);
        Input input = op.getInput();
        if (input != null) {
            if (input.getMessage() == null) {
                throw new WSDLRuntimeException(LOG, "NO_MESSAGE", "input", op.getName(), input.getName());
            }
            MessageInfo minfo = opInfo.createMessage(input.getMessage().getQName(), MessageInfo.Type.INPUT);
            opInfo.setInput(input.getName(), minfo);
            buildMessage(minfo, input.getMessage());
            copyExtensors(minfo, input.getExtensibilityElements());
            copyExtensionAttributes(minfo, input);
        }
        Output output = op.getOutput();
        if (output != null) {
            if (output.getMessage() == null) {
                throw new WSDLRuntimeException(LOG, "NO_MESSAGE", "output", op.getName(), output.getName());
            }
            MessageInfo minfo = opInfo.createMessage(output.getMessage().getQName(), MessageInfo.Type.OUTPUT);
            opInfo.setOutput(output.getName(), minfo);
            buildMessage(minfo, output.getMessage());
            copyExtensors(minfo, output.getExtensibilityElements());
            copyExtensionAttributes(minfo, output);
        }
        Map<?, ?> m = op.getFaults();
        for (Map.Entry<?, ?> rawentry : m.entrySet()) {
            Map.Entry<String, Fault> entry = cast(rawentry, String.class, Fault.class);
            FaultInfo finfo = opInfo.addFault(new QName(inf.getName().getNamespaceURI(), entry.getKey()),
                                              entry.getValue().getMessage().getQName());
            copyDocumentation(finfo, entry.getValue());
            buildMessage(finfo, entry.getValue().getMessage());
            copyExtensors(finfo, entry.getValue().getExtensibilityElements());
            copyExtensionAttributes(finfo, entry.getValue());
        }
        checkForWrapped(opInfo, allowRefs, false, unwrapLogLevel);
    }

    public static void checkForWrapped(OperationInfo opInfo, boolean relaxed) {
        checkForWrapped(opInfo, relaxed, relaxed);
    }
    public static void checkForWrapped(OperationInfo opInfo, boolean allowRefs, boolean relaxed) {
        checkForWrapped(opInfo, allowRefs, relaxed, Level.FINE);
    }

    public static void checkForWrapped(OperationInfo opInfo, boolean allowRefs,
                                       boolean relaxed, Level logLevel) {
        MessageInfo inputMessage = opInfo.getInput();
        MessageInfo outputMessage = opInfo.getOutput();
        boolean passedRule = true;
        // RULE No.1:
        // The operation's input and output message (if present) each contain
        // only a single part
        // input message must exist
        if (inputMessage == null || inputMessage.size() == 0
            || (inputMessage.size() > 1 && !relaxed)) {
            passedRule = false;
        }
        if (outputMessage != null && outputMessage.size() > 1) {
            passedRule = false;
        }

        if (!passedRule) {
            org.apache.cxf.common.i18n.Message message
                = new org.apache.cxf.common.i18n.Message("WRAPPED_RULE_1", LOG, opInfo.getName());
            LOG.log(logLevel, message.toString());
            return;
        }
        SchemaCollection schemas = opInfo.getInterface().getService().getXmlSchemaCollection();
        XmlSchemaElement inputEl = null;
        XmlSchemaElement outputEl = null;

        // RULE No.2:
        // The input message part refers to a global element declaration whose
        // local name is equal to the operation name.
        MessagePartInfo inputPart = inputMessage.getMessagePartByIndex(0);
        if (!inputPart.isElement()) {
            passedRule = false;
        } else {
            QName inputElementName = inputPart.getElementQName();
            inputEl = schemas.getElementByQName(inputElementName);
            if (inputEl == null) {
                passedRule = false;
            } else if (!opInfo.getName().getLocalPart().equals(inputElementName.getLocalPart())) {
                passedRule = relaxed;
            }
        }

        if (!passedRule) {
            org.apache.cxf.common.i18n.Message message
                = new org.apache.cxf.common.i18n.Message("WRAPPED_RULE_2", LOG, opInfo.getName());
            LOG.log(logLevel, message.toString());
            return;
        }
        // RULE No.3:
        // The output message part refers to a global element declaration
        if (outputMessage != null && outputMessage.size() == 1) {
            final MessagePartInfo outputPart = outputMessage.getMessagePartByIndex(0);
            if (outputPart != null) {
                if (!outputPart.isElement()
                    || schemas.getElementByQName(outputPart.getElementQName()) == null) {
                    passedRule = false;
                } else {
                    outputEl = schemas.getElementByQName(outputPart.getElementQName());
                }
            }
        }

        if (!passedRule) {
            org.apache.cxf.common.i18n.Message message
                = new org.apache.cxf.common.i18n.Message("WRAPPED_RULE_3", LOG, opInfo.getName());
            LOG.log(logLevel, message.toString());
            return;
        }
        // RULE No.4 and No5:
        // wrapper element should be pure complex type

        // Now lets see if we have any attributes...
        // This should probably look at the restricted and substitute types too.
        OperationInfo unwrapped = new UnwrappedOperationInfo(opInfo);
        MessageInfo unwrappedInput = new MessageInfo(unwrapped, MessageInfo.Type.INPUT,
                                                     inputMessage.getName());
        MessageInfo unwrappedOutput = null;

        XmlSchemaComplexType xsct;
        if (inputEl.getSchemaType() instanceof XmlSchemaComplexType) {

            xsct = (XmlSchemaComplexType)inputEl.getSchemaType();
            if (hasAttributes(xsct)
                || (inputEl.isNillable() && !relaxed)
                || !isWrappableSequence(xsct, inputEl.getQName().getNamespaceURI(),
                                        unwrappedInput, allowRefs)) {
                passedRule = false;
            }
        } else {
            passedRule = false;
        }

        if (!passedRule) {
            org.apache.cxf.common.i18n.Message message
                = new org.apache.cxf.common.i18n.Message("WRAPPED_RULE_4", LOG, opInfo.getName());
            LOG.log(logLevel, message.toString());
            return;
        }

        if (outputMessage != null) {
            unwrappedOutput = new MessageInfo(unwrapped, MessageInfo.Type.OUTPUT, outputMessage.getName());

            if (outputEl != null && outputEl.getSchemaType() instanceof XmlSchemaComplexType) {
                xsct = (XmlSchemaComplexType)outputEl.getSchemaType();
                if (xsct.isAbstract()) {
                    passedRule = false;
                }
                if (hasAttributes(xsct)
                    || (outputEl.isNillable() && !relaxed)
                    || !isWrappableSequence(xsct, outputEl.getQName().getNamespaceURI(), unwrappedOutput,
                                            allowRefs)) {
                    passedRule = false;
                }
            } else {
                passedRule = false;
            }
        }

        if (!passedRule) {
            org.apache.cxf.common.i18n.Message message
                = new org.apache.cxf.common.i18n.Message("WRAPPED_RULE_5", LOG, opInfo.getName());
            LOG.log(logLevel, message.toString());
            return;
        }
        // we are wrappable!!
        opInfo.setUnwrappedOperation(unwrapped);
        unwrapped.setInput(opInfo.getInputName(), unwrappedInput);
        if (outputMessage != null) {
            unwrapped.setOutput(opInfo.getOutputName(), unwrappedOutput);
        }
    }

    private static boolean hasAttributes(XmlSchemaComplexType complexType) {
        // Now lets see if we have any attributes...
        // This should probably look at the restricted and substitute types too.
        return complexType.getAnyAttribute() != null || !complexType.getAttributes().isEmpty();
    }

    private static boolean isWrappableSequence(XmlSchemaComplexType type, String namespaceURI,
                                               MessageInfo wrapper, boolean allowRefs) {
        if (!hasAttributes(type)) {
            if (type.getParticle() instanceof XmlSchemaSequence) {
                XmlSchemaSequence seq = (XmlSchemaSequence)type.getParticle();
                return buildMessageParts(seq, namespaceURI, wrapper, allowRefs);
            } else if (type.getParticle() == null) {
                if (type.getContentModel() == null) {
                    return true;
                }
                if (type.getContentModel().getContent() instanceof XmlSchemaComplexContentExtension) {
                    XmlSchemaComplexContentExtension extension = (XmlSchemaComplexContentExtension)type
                        .getContentModel().getContent();
                    QName baseTypeName = extension.getBaseTypeName();
                    ServiceInfo serviceInfo = wrapper.getOperation().getInterface().getService();
                    XmlSchemaType schemaType = serviceInfo.getXmlSchemaCollection().getTypeByQName(baseTypeName);
                    if (!(schemaType instanceof XmlSchemaComplexType)
                        || !isWrappableSequence((XmlSchemaComplexType)schemaType, namespaceURI, wrapper,
                                                allowRefs)) {
                        return false;
                    }

                    if (extension.getParticle() instanceof XmlSchemaSequence) {
                        XmlSchemaSequence seq = (XmlSchemaSequence)extension.getParticle();
                        return buildMessageParts(seq, namespaceURI, wrapper, allowRefs);
                    }

                }
                return true;
            }
        }
        return false;
    }


    private static boolean buildMessageParts(XmlSchemaSequence seq, String namespaceURI, MessageInfo wrapper,
                                             boolean allowRefs) {
        List<XmlSchemaSequenceMember> items = seq.getItems();
        boolean ret = true;
        for (XmlSchemaSequenceMember seqItem : items) {
            if (!(seqItem instanceof XmlSchemaElement)) {
                return false;
            }
            XmlSchemaElement el = (XmlSchemaElement)seqItem;

            if (el.getSchemaTypeName() != null) {
                MessagePartInfo mpi = wrapper.addMessagePart(new QName(namespaceURI, el.getName()));
                mpi.setTypeQName(el.getSchemaTypeName());
                mpi.setElement(true);
                mpi.setElementQName(el.getWireName());
                mpi.setConcreteName(el.getWireName());
                mpi.setXmlSchema(el);
            } else if (el.getRef().getTargetQName() != null) {
                MessagePartInfo mpi = wrapper.addMessagePart(el.getRef().getTargetQName());
                mpi.setTypeQName(el.getRef().getTargetQName());
                mpi.setElementQName(el.getRef().getTargetQName());
                mpi.setElement(true);
                mpi.setXmlSchema(el);
                mpi.setProperty("isRefElement", true);
                // element reference is not permitted for wrapper element
                if (!allowRefs) {
                    ret = false;
                }
            } else {
                // anonymous type
                MessagePartInfo mpi = wrapper.addMessagePart(new QName(namespaceURI, el.getName()));
                mpi.setElementQName(mpi.getName());
                mpi.setConcreteName(el.getWireName());
                mpi.setElement(true);
                mpi.setXmlSchema(el);
            }
        }
        return ret;
    }


    private void buildMessage(AbstractMessageContainer minfo, Message msg) {
        SchemaCollection schemas = minfo.getOperation().getInterface().getService()
            .getXmlSchemaCollection();
        List<?> orderedParam = msg.getOrderedParts(null);
        for (Part part : cast(orderedParam, Part.class)) {
            MessagePartInfo pi = minfo.addMessagePart(new QName(minfo.getName().getNamespaceURI(), part
                .getName()));
            if (part.getTypeName() != null) {
                pi.setTypeQName(part.getTypeName());
                pi.setElement(false);
                pi.setXmlSchema(schemas.getTypeByQName(part.getTypeName()));
            } else if (part.getElementName() != null) {
                pi.setElementQName(part.getElementName());
                XmlSchemaElement schemaElement = schemas.getElementByQName(part.getElementName());
                if (null == schemaElement) {
                    org.apache.cxf.common.i18n.Message errorMessage =
                        new org.apache.cxf.common.i18n.Message("WSDL4J_BAD_ELEMENT_PART",
                                                               LOG,
                                                               part.getName(),
                                                               part.getElementName());
                    throw new WSDLRuntimeException(errorMessage);
                }
                pi.setElement(true);
                pi.setXmlSchema(schemaElement);
            } else {
                org.apache.cxf.common.i18n.Message errorMessage =
                    new org.apache.cxf.common.i18n.Message("PART_NO_NAME_NO_TYPE",
                                                           LOG,
                                                           part.getName());
                throw new WSDLRuntimeException(errorMessage);

            }
        }
    }

}

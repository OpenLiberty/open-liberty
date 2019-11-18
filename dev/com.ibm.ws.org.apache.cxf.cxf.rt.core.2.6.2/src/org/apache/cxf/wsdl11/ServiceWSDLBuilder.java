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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Import;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLElement;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.AttributeExtensible;
import javax.wsdl.extensions.ElementExtensible;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.extensions.schema.SchemaReference;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.service.model.AbstractMessageContainer;
import org.apache.cxf.service.model.AbstractPropertiesHolder;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.ws.addressing.NSManager;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaInclude;
import org.apache.ws.commons.schema.XmlSchemaRedefine;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;

/**
 * Consume a set of service definitions and produce a WSDL model. The ServiceInfo objects
 * contain the bindings, operations, and ports, plus XMLSchema schemas. 
 * 
 * Each wsdl:definition has to have a single target namespace. The first service in the list
 * defines the TNS of the overall WSDL. If a subsequent service has a divergent TNS, then
 * the code creates a new definition element (i.e., Definition object), and imports it into
 * the top-level object.
 */
public class ServiceWSDLBuilder {

    private final Map<String, String> ns2prefix;
    private final List<ServiceInfo> services;
    private boolean useSchemaImports;
    private String baseFileName;
    private int xsdCount;
    private final Bus bus;
    private final NSManager nsMan;
    private Document docDoc;
    
    /**
     * Sets up the builder on a bus with a list of services.
     * @param b the bus.
     * @param services the services.
     */
    public ServiceWSDLBuilder(Bus b, List<ServiceInfo> services) {
        this.services = services;
        bus = b;
        ns2prefix = new HashMap<String, String>();
        nsMan = new NSManager();
    }
    
    /**
     * For callers who prefer varargs, an inline list of ServiceInfo objects instead of 
     * a List. Primarily used for tests or other callers with only one service in hand. 
     * @param b the bus.
     * @param services the services.
     */
    public ServiceWSDLBuilder(Bus b, ServiceInfo ... services) {
        this(b, Arrays.asList(services));
    }
    
    /**
     * Set whether to emit references to imported schema files.
     * This is only effective for {@link #build(Map)}, which is passed additional schemas for 
     * import. {@link #build()} resets this flag to false.
     * @param b true to use imports.
     */
    public void setUseSchemaImports(boolean b) {
        useSchemaImports = b;
    }
    
    /**
     * Base filename for imported files.
     * @param s pathname.
     */
    public void setBaseFileName(String s) {
        baseFileName = s;
    }
    
    /**
     * Create the WSDL Definition object and return it. This function will never create
     * imports to schemas.
     * @return the WSDL definition.
     * @throws WSDLException
     */
    public Definition build() throws WSDLException {
        useSchemaImports = false;
        return build(null);
    }
    
    /**
     * Create the WSDL Definition object and return it. This function respects the 
     * setting of {@link #setUseSchemaImports(boolean)}.
     * @param imports A set of schema imports to either reference as imports or read and 
     * then inline.
     * @return the WSDL definition
     * @throws WSDLException
     */
    public Definition build(Map<String, SchemaInfo> imports) throws WSDLException {
        Definition definition = null;
        try {
            definition = services.get(0).getProperty(WSDLServiceBuilder.WSDL_DEFINITION, Definition.class);
        } catch (ClassCastException e) {
            //ignore
        }
        if (definition == null) {
            ServiceInfo si = services.get(0);
            definition = newDefinition(si.getName(), si.getTargetNamespace());
            addNamespace(WSDLConstants.CONVENTIONAL_TNS_PREFIX, si.getTargetNamespace(), definition);
            addExtensibilityElements(definition, definition, getWSDL11Extensors(si.getDescription()));

            Collection<PortType> portTypes = new HashSet<PortType>();
            for (ServiceInfo service : services) {
                Definition portTypeDef = definition;
                Definition orig = definition;
                if (!isSameTNS(service)) {
                    portTypeDef = newDefinition(service.getInterface().getName(),
                                                service.getInterface().getName().getNamespaceURI());
                    Import wsdlImport = definition.createImport();
                    String tns = service.getInterface().getName().getNamespaceURI();
                    wsdlImport.setDefinition(portTypeDef);
                    wsdlImport.setNamespaceURI(tns);
                    wsdlImport.setLocationURI(service.getInterface().getName().getLocalPart() + ".wsdl");
                    definition.addImport(wsdlImport);
                    addNamespace(getPrefix(tns), tns, definition);
                    definition = portTypeDef;
                }
                portTypes.add(buildPortType(service.getInterface(), portTypeDef));
                
                if (service.getSchemas() != null && service.getSchemas().size() > 0) {
                    if (!useSchemaImports) {
                        buildTypes(service.getSchemas(), imports, portTypeDef);
                    } else {
                        buildTypesWithSchemaImports(service.getSchemas(),
                                imports, portTypeDef);
                    }
                }
                definition = orig;
            }
            
            for (ServiceInfo service : services) {
                buildBinding(definition, service.getBindings(), portTypes);
                buildService(service, definition);
            }
        }
        return definition;
    }

    private boolean isSameTNS(final ServiceInfo service) {
        return service.getName().getNamespaceURI().equals(service.getInterface().getName().getNamespaceURI());
    }

    private Definition newDefinition(final QName name, String targetNamespace) {
        Definition d = bus.getExtension(WSDLManager.class).getWSDLFactory().newDefinition();
        d.setExtensionRegistry(bus.getExtension(WSDLManager.class).getExtensionRegistry());
        d.setQName(name);
        d.setTargetNamespace(targetNamespace);
        addNamespace(WSDLConstants.NP_SCHEMA_XSD, WSDLConstants.NS_SCHEMA_XSD, d);
        return d;
    }

    /** 
     * Return a list of ExtensibilityElements for a particular component, such as a BindingFaultInfo.
     * This perhaps should be protected.
     * @param holder The item containing the extensibility elements.
     * @return the extensibility elements.
     */
    public List<ExtensibilityElement> getWSDL11Extensors(AbstractPropertiesHolder holder) {
        if (holder == null) {
            return null;
        }
        return holder.getExtensors(ExtensibilityElement.class);
    }
    
    protected void addDocumentation(WSDLElement wsdlel, String text) {
        if (text == null) {
            return;
        }
        if (docDoc == null) {
            try {
                docDoc = XMLUtils.newDocument();
            } catch (ParserConfigurationException e) {
                //ignore
            }
        }
        Element el = docDoc.createElementNS(WSDLConstants.NS_WSDL11, "wsdl:documentation");
        el.setTextContent(text);
        wsdlel.setDocumentationElement(el);
    }
    protected void addExtensibilityElements(Definition def,
                                            ElementExtensible elementExtensible, 
                                            List<ExtensibilityElement> extensibilityElements) {
        if (extensibilityElements != null) {
            for (ExtensibilityElement element : extensibilityElements) {
                if (element instanceof UnknownExtensibilityElement) {
                    UnknownExtensibilityElement uee = (UnknownExtensibilityElement)element;
                    String pfx = uee.getElement().getPrefix();
                    addNamespace(pfx, element.getElementType().getNamespaceURI(), def);
                } else {
                    QName qn = element.getElementType();
                    addNamespace(qn.getNamespaceURI(), def);
                }
                elementExtensible.addExtensibilityElement(element);
            }
        }
    }
    
    private void addExtensibilityAttributes(Definition def,
                                            AttributeExtensible attributeExtensible, 
                                            Map<QName, Object> attributes) {
        if (attributes == null) {
            return;
        }
        for (QName qname : attributes.keySet()) {
            addNamespace(qname.getNamespaceURI(), def);
            attributeExtensible.setExtensionAttribute(qname, attributes.get(qname));
        }
    }
    
    private Schema getSchemaImplementation(Definition def) {
        ExtensionRegistry reg = def.getExtensionRegistry();
        ExtensibilityElement extension;
        try {
            extension = reg.createExtension(javax.wsdl.Types.class, WSDLConstants.QNAME_SCHEMA);
        } catch (WSDLException e) {
            throw new RuntimeException("Problem creating schema implementation", e);
        }
        //  try to cast the resulting extension:
        try {
            return Schema.class.cast(extension);
        } catch (ClassCastException e) {
            throw new RuntimeException("Schema implementation problem", e);
        }
    }
    
    protected void buildTypes(final Collection<SchemaInfo> schemas,
                              final Map<String, SchemaInfo> imports,
                              final Definition def) {
        Types types = def.createTypes();
        
        for (SchemaInfo schemaInfo : schemas) {
            Schema schemaImpl = getSchemaImplementation(def);
            schemaImpl.setRequired(true);
            schemaImpl.setElementType(WSDLConstants.QNAME_SCHEMA);
            schemaImpl.setElement(schemaInfo.getElement());
            for (XmlSchemaExternal ext : schemaInfo.getSchema().getExternals()) {
                if (ext.getSchema() == null) {
                    continue;
                }
                if (ext instanceof XmlSchemaImport) {
                    SchemaImport imp = schemaImpl.createImport();
                    imp.setNamespaceURI(((XmlSchemaImport)ext).getNamespace());
                    imp.setSchemaLocationURI(((XmlSchemaImport)ext).getSchemaLocation());
                    
                    Schema schemaImpl2 = getSchemaImplementation(def);
                    schemaImpl2.setRequired(true);
                    schemaImpl2.setElementType(WSDLConstants.QNAME_SCHEMA);
                    schemaImpl2.setDocumentBaseURI(ext.getSchema().getSourceURI());
                    try {
                        schemaImpl2.setElement(ext.getSchema().getSchemaDocument().getDocumentElement());
                    } catch (XmlSchemaSerializerException e) {
                        //ignore
                    }
                    imp.setReferencedSchema(schemaImpl2);

                    schemaImpl.addImport(imp);
                } else if (ext instanceof XmlSchemaInclude) {
                    SchemaReference imp = schemaImpl.createInclude();
                    imp.setSchemaLocationURI(((XmlSchemaInclude)ext).getSchemaLocation());

                    Schema schemaImpl2 = getSchemaImplementation(def);
                    schemaImpl2.setRequired(true);
                    schemaImpl2.setElementType(WSDLConstants.QNAME_SCHEMA);
                    schemaImpl2.setDocumentBaseURI(ext.getSchema().getSourceURI());
                    try {
                        schemaImpl2.setElement(ext.getSchema().getSchemaDocument().getDocumentElement());
                    } catch (XmlSchemaSerializerException e) {
                        //ignore
                    }
                    imp.setReferencedSchema(schemaImpl2);
                    
                    schemaImpl.addInclude(imp);
                } else if (ext instanceof XmlSchemaRedefine) {
                    SchemaReference imp = schemaImpl.createRedefine();
                    imp.setSchemaLocationURI(((XmlSchemaRedefine)ext).getSchemaLocation());
                    
                    Schema schemaImpl2 = getSchemaImplementation(def);
                    schemaImpl2.setRequired(true);
                    schemaImpl2.setElementType(WSDLConstants.QNAME_SCHEMA);
                    schemaImpl2.setDocumentBaseURI(ext.getSchema().getSourceURI());
                    try {
                        schemaImpl2.setElement(ext.getSchema().getSchemaDocument().getDocumentElement());
                    } catch (XmlSchemaSerializerException e) {
                        //ignore
                    }
                    imp.setReferencedSchema(schemaImpl2);
                    
                    schemaImpl.addRedefine(imp);
                }
            }
            types.addExtensibilityElement(schemaImpl);
        }
        def.setTypes(types);
    }
    
    /**
     * @param schemas
     * @param imports
     * @param def
     */
    protected void buildTypesWithSchemaImports(final Collection<SchemaInfo> schemas,
                                               final Map<String, SchemaInfo> imports, final Definition def) {

        Types types = def.createTypes();

        Map<String, Schema> namespaceToSchemaMap = new HashMap<String, Schema>();
        Map<String, SchemaInfo> namespaceToSchemaInfo = new HashMap<String, SchemaInfo>();
        
        for (SchemaInfo schemaInfo : schemas) {
            Schema schema = getSchemaImplementation(def);
            schema.setRequired(true);
            schema.setElementType(WSDLConstants.QNAME_SCHEMA);

            String name = baseFileName + "_schema" + (++xsdCount) + ".xsd";
            schema.setDocumentBaseURI(name);

            schema.setElement(schemaInfo.getElement());

            namespaceToSchemaMap.put(schemaInfo.getNamespaceURI(), schema);
            namespaceToSchemaInfo.put(schemaInfo.getNamespaceURI(), schemaInfo);
            
            imports.put(name, schemaInfo);
        }
        
        for (Schema schema : namespaceToSchemaMap.values()) {
            Element docElement = schema.getElement();

            List<Element> elementList = DOMUtils
                .findAllElementsByTagNameNS(docElement, "http://www.w3.org/2001/XMLSchema", "import");

            for (Element el : elementList) {
                String sn = el.getAttribute("namespace");

                Schema referencedSchema = namespaceToSchemaMap.get(sn);
                if (referencedSchema != null) {
                    SchemaInfo schemaInfo = namespaceToSchemaInfo.get(sn);
                    
                    el.setAttribute("schemaLocation", referencedSchema.getDocumentBaseURI());
    
                    addSchemaImport(schema, schemaInfo, referencedSchema);
                }
            }
        }
        
        Document doc = createDocument();
        Element nd = XMLUtils.createElementNS(doc, new QName(WSDLConstants.NS_SCHEMA_XSD, "schema"));
        nd.setAttribute("xmlns", WSDLConstants.NS_SCHEMA_XSD);
        doc.appendChild(nd);
        
        Schema schema = getSchemaImplementation(def);
        schema.setRequired(true);
        schema.setElementType(WSDLConstants.QNAME_SCHEMA);
        
        Collection<String> defNamespaces = CastUtils.cast(def.getNamespaces().values());
        
        for (SchemaInfo schemaInfo : schemas) {
            Schema referencedSchema = namespaceToSchemaMap.get(schemaInfo.getNamespaceURI());
            
            // this ensures only the schemas directly referenced by the wsdl are included.
            if (defNamespaces.contains(schemaInfo.getNamespaceURI())) {
                Element impElement = XMLUtils.createElementNS(doc, new QName(WSDLConstants.NS_SCHEMA_XSD,
                                                                             "import"));
                
                impElement.setAttribute("schemaLocation", referencedSchema.getDocumentBaseURI());
                impElement.setAttribute("namespace", schemaInfo.getNamespaceURI());
                nd.appendChild(impElement);
                
                addSchemaImport(schema, schemaInfo, referencedSchema);
            }
        }
        
        schema.setElement(nd);
        types.addExtensibilityElement(schema);

        def.setTypes(types);
    }

    private Document createDocument() {
        Document doc = null;
        try {
            doc = XMLUtils.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("DOM configuration problem", e);
        }
        return doc;
    }

    private void addSchemaImport(Schema schema, SchemaInfo schemaInfo, Schema referencedSchema) {
        SchemaImport imp = schema.createImport();
        imp.setId(schemaInfo.getSystemId());
        imp.setNamespaceURI(schemaInfo.getNamespaceURI());
        imp.setSchemaLocationURI(referencedSchema.getDocumentBaseURI());
        imp.setReferencedSchema(referencedSchema);
        schema.addImport(imp);
    }
    
    protected void buildBinding(Definition definition,
                                Collection<BindingInfo> bindingInfos,
                                Collection<PortType> portTypes) {
        Binding binding = null;
        for (BindingInfo bindingInfo : bindingInfos) {
            binding = definition.createBinding();
            addDocumentation(binding, bindingInfo.getDocumentation());
            binding.setUndefined(false);
            for (PortType portType : portTypes) {
                if (portType.getQName().equals(bindingInfo.getInterface().getName())) {
                    binding.setPortType(portType);
                    break;
                }
            }
            binding.setQName(bindingInfo.getName());
            if (!bindingInfo.getName().getNamespaceURI().equals(definition.getTargetNamespace())) {
                addNamespace(bindingInfo.getName().getNamespaceURI(), definition);
            }
            buildBindingOperation(definition, binding, bindingInfo.getOperations());
            addExtensibilityElements(definition, binding, getWSDL11Extensors(bindingInfo));
            definition.addBinding(binding);
        }
    }

    protected void buildBindingOperation(Definition def, Binding binding, 
                                       Collection<BindingOperationInfo> bindingOperationInfos) {
        BindingOperation bindingOperation = null;
        for (BindingOperationInfo bindingOperationInfo : bindingOperationInfos) {
            bindingOperation = def.createBindingOperation();
            addDocumentation(bindingOperation, bindingOperationInfo.getDocumentation());
            bindingOperation.setName(bindingOperationInfo.getName().getLocalPart());
            for (Operation operation 
                    : CastUtils.cast(binding.getPortType().getOperations(), Operation.class)) {
                if (operation.getName().equals(bindingOperation.getName())) {
                    bindingOperation.setOperation(operation);
                    break;
                }
            }
            buildBindingInput(def, bindingOperation, bindingOperationInfo.getInput());
            buildBindingOutput(def, bindingOperation, bindingOperationInfo.getOutput());
            buildBindingFault(def, bindingOperation, bindingOperationInfo.getFaults());
            addExtensibilityAttributes(def, bindingOperation, bindingOperationInfo.getExtensionAttributes());
            addExtensibilityElements(def, bindingOperation, getWSDL11Extensors(bindingOperationInfo));
            binding.addBindingOperation(bindingOperation);
        }
    }

    protected void buildBindingFault(Definition def, BindingOperation bindingOperation, 
                                   Collection<BindingFaultInfo> bindingFaultInfos) {
        BindingFault bindingFault = null;
        for (BindingFaultInfo bindingFaultInfo 
            : bindingFaultInfos) {
            bindingFault = def.createBindingFault();
            addDocumentation(bindingFault, bindingFaultInfo.getDocumentation());
            bindingFault.setName(bindingFaultInfo.getFaultInfo().getFaultName().getLocalPart());
            bindingOperation.addBindingFault(bindingFault);
            addExtensibilityAttributes(def, bindingFault, bindingFaultInfo.getExtensionAttributes());
            addExtensibilityElements(def, bindingFault, getWSDL11Extensors(bindingFaultInfo));
        }
        
    }

    protected void buildBindingInput(Definition def, BindingOperation bindingOperation, 
                                         BindingMessageInfo bindingMessageInfo) {
        BindingInput bindingInput = null;
        if (bindingMessageInfo != null) {
            bindingInput = def.createBindingInput();
            addDocumentation(bindingInput, bindingMessageInfo.getDocumentation());
            bindingInput.setName(bindingMessageInfo.getMessageInfo().getName().getLocalPart());
            bindingOperation.setBindingInput(bindingInput);
            addExtensibilityAttributes(def, bindingInput, bindingMessageInfo.getExtensionAttributes());
            addExtensibilityElements(def, bindingInput, getWSDL11Extensors(bindingMessageInfo));
        }
    }
    
    protected void buildBindingOutput(Definition def, BindingOperation bindingOperation, 
                                   BindingMessageInfo bindingMessageInfo) {
        BindingOutput bindingOutput = null;
        if (bindingMessageInfo != null) {
            bindingOutput = def.createBindingOutput();
            addDocumentation(bindingOutput, bindingMessageInfo.getDocumentation());
            bindingOutput.setName(bindingMessageInfo.getMessageInfo().getName().getLocalPart());
            bindingOperation.setBindingOutput(bindingOutput);
            addExtensibilityAttributes(def, bindingOutput, bindingMessageInfo.getExtensionAttributes());
            addExtensibilityElements(def, bindingOutput, getWSDL11Extensors(bindingMessageInfo));
        }
    }

    protected void buildService(ServiceInfo serviceInfo, Definition definition) {
        
        Map<QName, MessageInfo> messages = serviceInfo.getMessages();
        for (Map.Entry<QName, MessageInfo> mie : messages.entrySet()) {
            if (!mie.getKey().getNamespaceURI().equals(definition.getTargetNamespace())) {
                continue;
            }
            if (definition.getMessage(mie.getKey()) != null) {
                continue;
            }
            Message message = definition.createMessage();
            addDocumentation(message, mie.getValue().getMessageDocumentation());
            message.setUndefined(false);
            message.setQName(mie.getKey());
            for (MessagePartInfo mpi : mie.getValue().getMessageParts()) {
                Part part = definition.createPart();
                boolean elemental = mpi.isElement();
                // RFSB will turn on isElement bogusly.
                if (elemental 
                    && null == serviceInfo.getXmlSchemaCollection().
                        getElementByQName(mpi.getElementQName())) {
                    elemental = false;
                }
                if (elemental) {
                    part.setElementName(mpi.getElementQName());
                } else {
                    part.setTypeName(mpi.getTypeQName());
                }
                part.setName(mpi.getName().getLocalPart());
                message.addPart(part);
            }
            
            definition.addMessage(message);
        }
        
        addDocumentation(definition, serviceInfo.getTopLevelDoc());
        Service serv = definition.createService();
        addDocumentation(serv, serviceInfo.getDocumentation());
        serv.setQName(serviceInfo.getName());
        addNamespace(serviceInfo.getName().getNamespaceURI(), definition);
        addExtensibilityElements(definition, serv, getWSDL11Extensors(serviceInfo));
        definition.addService(serv);

        for (EndpointInfo ei : serviceInfo.getEndpoints()) {
            addNamespace(ei.getTransportId(), definition);
            Port port = definition.createPort();
            addDocumentation(port, ei.getDocumentation());
            port.setName(ei.getName().getLocalPart());
            port.setBinding(definition.getBinding(ei.getBinding().getName()));
            addExtensibilityElements(definition, port, getWSDL11Extensors(ei));
            serv.addPort(port);
        }
    }

    protected PortType buildPortType(InterfaceInfo intf, final Definition def) {
        PortType portType = null;
        try {
            portType = intf.getProperty(WSDLServiceBuilder.WSDL_PORTTYPE, PortType.class);
        } catch (ClassCastException e) {
            // do nothing
        }
        
        if (portType == null) {
            portType = def.createPortType();
            portType.setQName(intf.getName());
            addDocumentation(portType, intf.getDocumentation());
            addNamespace(intf.getName().getNamespaceURI(), def);
            addExtensibilityElements(def, portType, getWSDL11Extensors(intf));
            addExtensibilityAttributes(def, portType, intf.getExtensionAttributes());
            portType.setUndefined(false);
            buildPortTypeOperation(portType, intf.getOperations(), def);
        }

        def.addPortType(portType);
        return portType;
    }

    protected void addNamespace(String namespaceURI, Definition def) {
        addNamespace(getPrefix(namespaceURI), namespaceURI, def);
    }

    protected void addNamespace(String prefix, String namespaceURI, Definition def) {
        ns2prefix.put(namespaceURI, prefix);
        def.addNamespace(prefix, namespaceURI);
    }
    
    private OperationInfo getOperationInfo(OperationInfo operation) {
        if (operation.getUnwrappedOperation() != null) {
            return operation.getUnwrappedOperation();
        }
        return operation;
    }
    
    private Map<QName, Object> getInputExtensionAttributes(OperationInfo operation) {
        return getOperationInfo(operation).getInput().getExtensionAttributes();
    }
    
    private Map<QName, Object> getOutputExtensionAttributes(OperationInfo operation) {
        return getOperationInfo(operation).getOutput().getExtensionAttributes();
    }    

    protected void buildPortTypeOperation(PortType portType,
                                          Collection<OperationInfo> operationInfos,
                                          final Definition def) {
        for (OperationInfo operationInfo : operationInfos) {
            Operation operation = null;
            try {
                operation = operationInfo.getProperty(
                    WSDLServiceBuilder.WSDL_OPERATION, Operation.class);
            } catch (ClassCastException e) {
                // do nothing
            }
            
            if (operation == null) {
                operation = def.createOperation();
                addDocumentation(operation, operationInfo.getDocumentation());
                operation.setUndefined(false);
                operation.setName(operationInfo.getName().getLocalPart());
                addNamespace(operationInfo.getName().getNamespaceURI(), def);
                if (operationInfo.isOneWay()) {
                    operation.setStyle(OperationType.ONE_WAY);
                }
                addExtensibilityElements(def, operation, getWSDL11Extensors(operationInfo));
                Input input = def.createInput();
                addDocumentation(input, operationInfo.getInput().getDocumentation());
                input.setName(operationInfo.getInputName());
                Message message = def.createMessage();
                buildMessage(message, operationInfo.getInput(), def);
                this.addExtensibilityAttributes(def, input, getInputExtensionAttributes(operationInfo));
                this.addExtensibilityElements(def, input, getWSDL11Extensors(operationInfo.getInput()));
                input.setMessage(message);
                operation.setInput(input);
                operation.setParameterOrdering(operationInfo.getParameterOrdering());
                
                if (operationInfo.getOutput() != null) {
                    Output output = def.createOutput();
                    addDocumentation(output, operationInfo.getOutput().getDocumentation());
                    output.setName(operationInfo.getOutputName());
                    message = def.createMessage();
                    buildMessage(message, operationInfo.getOutput(), def);
                    this.addExtensibilityAttributes(def, output, getOutputExtensionAttributes(operationInfo));
                    this.addExtensibilityElements(def, output, getWSDL11Extensors(operationInfo.getOutput()));
                    output.setMessage(message);
                    operation.setOutput(output);
                }
                //loop to add fault
                Collection<FaultInfo> faults = operationInfo.getFaults();
                Fault fault = null;
                for (FaultInfo faultInfo : faults) {
                    fault = def.createFault();
                    addDocumentation(fault, faultInfo.getDocumentation());
                    fault.setName(faultInfo.getFaultName().getLocalPart());
                    message = def.createMessage();
                    buildMessage(message, faultInfo, def);
                    this.addExtensibilityAttributes(def, fault, faultInfo.getExtensionAttributes());
                    this.addExtensibilityElements(def, fault, getWSDL11Extensors(faultInfo));
                    fault.setMessage(message);
                    operation.addFault(fault);
                }
            }
            portType.addOperation(operation);
        }
    }

    private String getPrefix(String ns) {
        for (String namespace : nsMan.getNamespaces()) {
            if (namespace.equals(ns)) {
                return nsMan.getPrefixFromNS(namespace);
            }
        }
        String prefix = ns2prefix.get(ns);
        if (prefix == null) {
            prefix = getNewPrefix();
            ns2prefix.put(ns, prefix);
        }
        return prefix;
    }
    
    private String getNewPrefix() {
        String prefix = "ns1";
        int i = 0;
        while (ns2prefix.containsValue(prefix)) {
            i++;
            prefix = "ns" + i;
        }
        return prefix;
    }

    protected void buildMessage(Message message,
                                AbstractMessageContainer messageContainer,
                                final Definition def) {
        addDocumentation(message, messageContainer.getMessageDocumentation());
        message.setQName(messageContainer.getName());
        message.setUndefined(false);
        def.addMessage(message);
        
        List<MessagePartInfo> messageParts = messageContainer.getMessageParts();
        Part messagePart = null;
        for (MessagePartInfo messagePartInfo : messageParts) {
            messagePart = def.createPart();
            messagePart.setName(messagePartInfo.getName().getLocalPart());
            if (messagePartInfo.isElement()) {
                messagePart.setElementName(messagePartInfo.getElementQName());
                addNamespace(messagePartInfo.getElementQName().getNamespaceURI(), def);
            } else if (messagePartInfo.getTypeQName() != null) {
                messagePart.setTypeName(messagePartInfo.getTypeQName());
                addNamespace(messagePartInfo.getTypeQName().getNamespaceURI(), def);
            }
            message.addPart(messagePart);
        }
    }
          
}

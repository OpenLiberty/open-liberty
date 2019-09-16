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

package org.apache.cxf.wsdl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import org.xml.sax.InputSource;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.LSInputImpl;
import org.apache.cxf.endpoint.EndpointResolverRegistry;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.resource.ExtendedURIResolver;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MultiplexDestination;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.MetadataType;
import org.apache.cxf.ws.addressing.wsdl.AttributedQNameType;
import org.apache.cxf.ws.addressing.wsdl.ServiceNameType;
import org.apache.ws.commons.schema.XmlSchema;

/**
 * Provides utility methods for obtaining endpoint references, wsdl definitions, etc.
 */
public final class EndpointReferenceUtils {

    /**
     * We want to load the schemas, including references to external schemas, into a SchemaFactory
     * to validate. There seem to be bugs in resolving inter-schema references in Xerces, so even when we are 
     * handing the factory all the schemas, interrelated with &lt;import&gt; elements, we need
     * to also hand over extra copies (!) as character images when requested.
     * 
     * To do this, we use the DOM representation kept in the SchemaInfo. This has the bonus
     * of benefiting from the use of the catalog resolver in there, which is missing from
     * the code in here.
     */
    private static final class SchemaLSResourceResolver implements LSResourceResolver {
        private final Map<String, byte[]> schemas;
        private final Set<String> done = new HashSet<String>();
        private final ExtendedURIResolver resolver = new ExtendedURIResolver();
        private final Bus bus;
        
        private SchemaLSResourceResolver(Map<String, byte[]> schemas, Bus b) {
            this.schemas = schemas;
            this.bus = b;
        }
        
        public LSInput resolveResource(String type, String namespaceURI, String publicId,
                                       String systemId, String baseURI) {

            String newId = systemId;
            if (baseURI != null && systemId != null) {  //add additional systemId null check
                try {
                    URI uri = new URI(baseURI);
                    uri = uri.resolve(systemId);
                    newId = uri.toString();
                    if (newId.equals(systemId)) {
                        URL url = new URL(baseURI);
                        url = new URL(url, systemId);
                        newId = url.toExternalForm();
                    }
                } catch (IllegalArgumentException e) {
                    //ignore - systemId not a valid URI
                } catch (URISyntaxException e) {
                    //ignore - baseURI not a valid URI
                } catch (MalformedURLException e) {
                    //ignore - baseURI or systemId not a URL either
                }
            }
            if (done.contains(newId + ":" + namespaceURI)) {
                return null;
            }
            LSInputImpl impl = null;
            
            if (schemas.containsKey(newId + ":" + namespaceURI)) {
                byte[] ds = schemas.remove(newId + ":" + namespaceURI);
                impl = new LSInputImpl();
                impl.setSystemId(newId);
                impl.setBaseURI(newId);
                impl.setByteStream(new ByteArrayInputStream(ds));
                done.add(newId + ":" + namespaceURI);
            }
            if (impl == null && schemas.containsKey(newId + ":null")) {
                byte[] ds = schemas.get(newId + ":null");
                impl = new LSInputImpl();
                impl.setSystemId(newId);
                impl.setBaseURI(newId);
                impl.setByteStream(new ByteArrayInputStream(ds));
                done.add(newId + ":" + namespaceURI);
            }
            if (impl == null && bus != null && systemId != null) {
                ResourceManager rm = bus.getExtension(ResourceManager.class);
                URL url = rm == null ? null : rm.resolveResource(systemId, URL.class);
                if (url != null) {
                    newId = url.toString();
                    if (done.contains(newId + ":" + namespaceURI)) {
                        return null;
                    }
                    if (schemas.containsKey(newId + ":" + namespaceURI)) {
                        byte[] ds = schemas.remove(newId + ":" + namespaceURI);
                        impl = new LSInputImpl();
                        impl.setSystemId(newId);
                        impl.setBaseURI(newId);
                        impl.setByteStream(new ByteArrayInputStream(ds));
                        done.add(newId + ":" + namespaceURI);
                    }
                }
            }
            if (impl != null) {
                return impl;
            }
            for (Map.Entry<String, byte[]> ent : schemas.entrySet()) {
                if (ent.getKey().endsWith(systemId + ":" + namespaceURI)) {
                    schemas.remove(ent.getKey());
                    impl = new LSInputImpl();
                    impl.setSystemId(newId);
                    impl.setBaseURI(newId);
                    impl.setCharacterStream(
                        new InputStreamReader(
                            new ByteArrayInputStream(ent.getValue())));
                    done.add(newId + ":" + namespaceURI);
                    return impl;
                }
            }
            // handle case where given systemId is null (so that
            // direct key lookup fails) by scanning through map
            // searching for a namespace match
            if (namespaceURI != null) {
                for (Map.Entry<String, byte[]> ent : schemas.entrySet()) {
                    if (ent.getKey().endsWith(namespaceURI)) {
                        schemas.remove(ent.getKey());
                        impl = new LSInputImpl();
                        impl.setSystemId(newId);
                        impl.setBaseURI(newId);
                        impl.setCharacterStream(
                            new InputStreamReader(
                                new ByteArrayInputStream(ent.getValue())));
                        done.add(newId + ":" + namespaceURI);
                        return impl;
                    }
                }
            }
                
            //REVIST - we need to get catalogs in here somehow  :-(
            if (systemId == null) {
                systemId = publicId;
            }
            if (systemId != null) {
                InputSource source = resolver.resolve(systemId, baseURI);
                if (source != null) {
                    impl = new LSInputImpl();
                    impl.setByteStream(source.getByteStream());
                    impl.setSystemId(source.getSystemId());
                    impl.setPublicId(source.getPublicId());
                }
            }
            LOG.warning("Could not resolve Schema for " + systemId);
            return impl;
        }
    }

    public static final String ANONYMOUS_ADDRESS = WSAEndpointReferenceUtils.ANONYMOUS_ADDRESS;
    
    private static final Logger LOG = LogUtils.getL7dLogger(EndpointReferenceUtils.class);

    private static final String NS_WSAW_2005 = "http://www.w3.org/2005/02/addressing/wsdl";
    private static final String WSDL_INSTANCE_NAMESPACE2 = 
        "http://www.w3.org/2006/01/wsdl-instance";
    private static final String WSDL_INSTANCE_NAMESPACE = 
            "http://www.w3.org/ns/wsdl-instance";
    
    private static final QName WSA_WSDL_NAMESPACE_NS =
        new QName("xmlns:" + JAXWSAConstants.WSAW_PREFIX);
    private static final String XML_SCHEMA_NAMESPACE =
        "http://www.w3.org/2001/XMLSchema";
    private static final String XML_SCHEMA_NAMESPACE_PREFIX = "xs";
    private static final QName XML_SCHEMA_NAMESPACE_NS =
        new QName("xmlns:" + XML_SCHEMA_NAMESPACE_PREFIX);
    private static final String XML_SCHEMA_INSTANCE_NAMESPACE =
        "http://www.w3.org/2001/XMLSchema-instance";
    private static final QName WSDL_LOCATION2 =
        new QName(WSDL_INSTANCE_NAMESPACE2, "wsdlLocation");
    private static final QName WSDL_LOCATION =
        new QName(WSDL_INSTANCE_NAMESPACE, "wsdlLocation");
    private static final QName XSI_TYPE = 
        new QName(XML_SCHEMA_INSTANCE_NAMESPACE, "type", "xsi");
    
    private static final org.apache.cxf.ws.addressing.wsdl.ObjectFactory WSA_WSDL_OBJECT_FACTORY = 
        new org.apache.cxf.ws.addressing.wsdl.ObjectFactory();
    
    
    private static final Set<Class<?>> ADDRESSING_CLASSES = new HashSet<Class<?>>();
    private static final AtomicReference<Reference<JAXBContext>> ADDRESSING_CONTEXT 
        = new AtomicReference<Reference<JAXBContext>>(new SoftReference<JAXBContext>(null));
    static {
        ADDRESSING_CLASSES.add(WSA_WSDL_OBJECT_FACTORY.getClass());
        ADDRESSING_CLASSES.add(WSAEndpointReferenceUtils.WSA_OBJECT_FACTORY.getClass());
    }
    
    private EndpointReferenceUtils() {
        // Utility class - never constructed
    }
    
    /**
     * Sets the service and port name of the provided endpoint reference. 
     * @param ref the endpoint reference.
     * @param serviceName the name of service.
     * @param portName the port name.
     */
    public static void setServiceAndPortName(EndpointReferenceType ref, 
                                             QName serviceName, 
                                             String portName) {
        if (null != serviceName) {
            JAXBElement<ServiceNameType> jaxbElement = getServiceNameType(serviceName, portName);
            MetadataType mt = WSAEndpointReferenceUtils.getSetMetadata(ref);

            mt.getAny().add(jaxbElement);
        }
    }
    
    public static JAXBElement<ServiceNameType> getServiceNameType(QName serviceName, String portName) {
        ServiceNameType serviceNameType = WSA_WSDL_OBJECT_FACTORY.createServiceNameType();
        serviceNameType.setValue(serviceName);
        serviceNameType.setEndpointName(portName);
        serviceNameType.getOtherAttributes().put(WSA_WSDL_NAMESPACE_NS, JAXWSAConstants.NS_WSAW);
        serviceNameType.getOtherAttributes().put(XSI_TYPE, 
                                                 JAXWSAConstants.WSAW_PREFIX + ":" 
                                                 + serviceNameType.getClass().getSimpleName());
        return WSA_WSDL_OBJECT_FACTORY.createServiceName(serviceNameType);
    }
    
    /**
     * Gets the service name of the provided endpoint reference. 
     * @param ref the endpoint reference.
     * @return the service name.
     */
    public static QName getServiceName(EndpointReferenceType ref, Bus bus) {
        MetadataType metadata = ref.getMetadata();
        if (metadata == null) {
            return null;
        }
        for (Object obj : metadata.getAny()) {
            if (obj instanceof Element) {
                Node node = (Element)obj;
                if ((node.getNamespaceURI().equals(JAXWSAConstants.NS_WSAW)
                    || node.getNamespaceURI().equals(NS_WSAW_2005)
                    || node.getNamespaceURI().equals(JAXWSAConstants.NS_WSAM))
                    && node.getLocalName().equals("ServiceName")) {
                    String content = node.getTextContent();
                    String namespaceURI = node.getFirstChild().getNamespaceURI();
                    String service = content;
                    if (content.contains(":")) {
                        namespaceURI = getNameSpaceUri(node, content, namespaceURI);
                        if (StringUtils.isEmpty(namespaceURI)) {
                            namespaceURI = findNamespaceHack(ref, bus);                                
                        }
                        service = getService(content);
                    } else {
                        Node nodeAttr = node.getAttributes().getNamedItem("xmlns");
                        namespaceURI = nodeAttr.getNodeValue();
                    }
                    
                    return new QName(namespaceURI, service);
                }
            } else if (obj instanceof JAXBElement) {
                Object val = ((JAXBElement<?>)obj).getValue();
                if (val instanceof ServiceNameType) {
                    return ((ServiceNameType)val).getValue();
                }
            } else if (obj instanceof ServiceNameType) {
                return ((ServiceNameType)obj).getValue();
            }
        }
        return null;
    }
    
    /**
     * Gets the port name of the provided endpoint reference.
     * @param ref the endpoint reference.
     * @return the port name.
     */
    public static String getPortName(EndpointReferenceType ref) {
        MetadataType metadata = ref.getMetadata();
        if (metadata != null) {
            for (Object obj : metadata.getAny()) {
                if (obj instanceof Element) {
                    Node node = (Element)obj;
                    if ((node.getNamespaceURI().equals(JAXWSAConstants.NS_WSAW)
                        || node.getNamespaceURI().equals(NS_WSAW_2005)
                        || node.getNamespaceURI().equals(JAXWSAConstants.NS_WSAM))
                        && node.getNodeName().contains("ServiceName")) {
                        Node item = node.getAttributes().getNamedItem("EndpointName");
                        return item != null ? item.getTextContent() : null;
                    }
                } else if (obj instanceof JAXBElement) {
                    Object val = ((JAXBElement<?>)obj).getValue();
                    if (val instanceof ServiceNameType) {
                        return ((ServiceNameType)val).getEndpointName();
                    }
                } else if (obj instanceof ServiceNameType) {
                    return ((ServiceNameType)obj).getEndpointName();
                }
            }
        }
        return null;
    }
    
    public static QName getPortQName(EndpointReferenceType ref, Bus bus) {
        QName serviceName = getServiceName(ref, bus); 
        return new QName(serviceName.getNamespaceURI(), getPortName(ref));
    }
    
    public static void setPortName(EndpointReferenceType ref, String portName) {
        MetadataType metadata = ref.getMetadata();
        if (metadata != null) {
            for (Object obj : metadata.getAny()) {
                if (obj instanceof Element) {
                    Element node = (Element)obj;
                    if (node.getNodeName().contains("ServiceName")
                        && (node.getNamespaceURI().equals(JAXWSAConstants.NS_WSAW)
                        || node.getNamespaceURI().equals(NS_WSAW_2005)
                        || node.getNamespaceURI().equals(JAXWSAConstants.NS_WSAM))) {
                        node.setAttribute(JAXWSAConstants.WSAM_ENDPOINT_NAME, portName);
                    }
                } else if (obj instanceof JAXBElement) {
                    Object val = ((JAXBElement<?>)obj).getValue();
                    if (val instanceof ServiceNameType) {
                        ((ServiceNameType)val).setEndpointName(portName);
                    }
                } else if (obj instanceof ServiceNameType) {
                    ((ServiceNameType)obj).setEndpointName(portName);
                }
            }
        }
    }
    
    public static void setInterfaceName(EndpointReferenceType ref, QName portTypeName) {
        if (null != portTypeName) {
            AttributedQNameType interfaceNameType =
                WSA_WSDL_OBJECT_FACTORY.createAttributedQNameType();
            
            interfaceNameType.setValue(portTypeName);
            interfaceNameType.getOtherAttributes().put(XML_SCHEMA_NAMESPACE_NS, 
                                                       XML_SCHEMA_NAMESPACE);
            interfaceNameType.getOtherAttributes().put(XSI_TYPE,
                                                       XML_SCHEMA_NAMESPACE_PREFIX + ":"
                                                       + portTypeName.getClass().getSimpleName());
            
            JAXBElement<AttributedQNameType> jaxbElement = 
                WSA_WSDL_OBJECT_FACTORY.createInterfaceName(interfaceNameType);

            MetadataType mt = WSAEndpointReferenceUtils.getSetMetadata(ref);
            mt.getAny().add(jaxbElement);
        }
    }
  
    public static QName getInterfaceName(EndpointReferenceType ref, Bus bus) {
        MetadataType metadata = ref.getMetadata();
        if (metadata == null) {
            return null;
        }
        for (Object obj : metadata.getAny()) {
            if (obj instanceof Element) {
                Node node = (Element)obj;
                if ((node.getNamespaceURI().equals(JAXWSAConstants.NS_WSAW)
                    || node.getNamespaceURI().equals(JAXWSAConstants.NS_WSAM))
                    && node.getNodeName().contains("InterfaceName")) {
                    
                    String content = node.getTextContent();
                    String namespaceURI = node.getFirstChild().getNamespaceURI();
                    //String service = content;
                    if (content.contains(":")) {
                        namespaceURI = getNameSpaceUri(node, content, namespaceURI);
                        if (StringUtils.isEmpty(namespaceURI)) {
                            namespaceURI = findNamespaceHack(ref, bus);                                
                        }
                        content = getService(content);
                    } else {
                        Node nodeAttr = node.getAttributes().getNamedItem("xmlns");
                        namespaceURI = nodeAttr.getNodeValue();
                    }

                    return new QName(namespaceURI, content);
                }
            } else if (obj instanceof JAXBElement) {
                Object val = ((JAXBElement<?>)obj).getValue();
                if (val instanceof AttributedQNameType) {
                    return ((AttributedQNameType)val).getValue();
                }
            } else if (obj instanceof AttributedQNameType) {
                return ((AttributedQNameType)obj).getValue();
            }
        }

        return null;
    }
    
    private static String findNamespaceHack(EndpointReferenceType ref, Bus bus) {
        //probably a broken version of Xalan, we'll have to 
        //try a hack to figure out the namespace as xalan
        //dropped the namespace declaration so there isn't 
        //a way to map the namespace prefix to the real namespace.
        //This is fixed in xalan 2.7.1, but older versions may 
        //be used
        if (bus == null) {
            return "";
        }
        String wsdlLocation = getWSDLLocation(ref);
        if (StringUtils.isEmpty(wsdlLocation)) {
            return "";
        }
        WSDLManager manager = bus.getExtension(WSDLManager.class);
        if (manager != null) {
            try {
                Definition def = manager.getDefinition(wsdlLocation);
                return def.getTargetNamespace();
            } catch (WSDLException e) {
                //ignore
            }
        }
        return "";
    }

    public static void setWSDLLocation(EndpointReferenceType ref, String... wsdlLocation) {
        
        MetadataType metadata = WSAEndpointReferenceUtils.getSetMetadata(ref);

        //wsdlLocation attribute is a list of anyURI.
        StringBuilder strBuf = new StringBuilder();
        for (String str : wsdlLocation) {
            strBuf.append(str);
            strBuf.append(" ");
        }

        metadata.getOtherAttributes().put(WSDL_LOCATION, strBuf.toString().trim());
    }
    
    public static String getWSDLLocation(EndpointReferenceType ref) {
        String wsdlLocation = null;
        MetadataType metadata = ref.getMetadata();

        if (metadata != null) {
            wsdlLocation = metadata.getOtherAttributes().get(WSDL_LOCATION);
            if (wsdlLocation == null) {
                wsdlLocation = metadata.getOtherAttributes().get(WSDL_LOCATION2);
            }
        }

        if (null == wsdlLocation) {
            return null;
        }

        //TODO The wsdlLocation inserted should be a valid URI 
        //before doing a split. So temporarily return the string
        //return wsdlLocation.split(" ");
        return wsdlLocation;
    }

    /**
     * Sets the metadata on the provided endpoint reference.
     * @param ref the endpoint reference.
     * @param metadata the list of metadata source.
     */
    public static void setMetadata(EndpointReferenceType ref, List<Source> metadata)
        throws EndpointUtilsException {
        
        if (null != ref) {
            MetadataType mt = WSAEndpointReferenceUtils.getSetMetadata(ref);
            List<Object> anyList = mt.getAny();
            try {
                for (Source source : metadata) {
                    Node node = null;
                    boolean doTransform = true;
                    if (source instanceof StreamSource) {
                        StreamSource ss = (StreamSource)source;
                        if (null == ss.getInputStream()
                            && null == ss.getReader()) {
                            setWSDLLocation(ref, ss.getSystemId());
                            doTransform = false;
                        }
                    } else if (source instanceof DOMSource) {
                        node = ((DOMSource)source).getNode();
                        doTransform = false;
                    } 
                    
                    if (doTransform) {
                        DOMResult domResult = new DOMResult();
                        domResult.setSystemId(source.getSystemId());
                        node = StaxUtils.read(source);
    
                        node = domResult.getNode();
                    }
                    
                    if (null != node) {
                        if (node instanceof Document) {
                            try {
                                ((Document)node).setDocumentURI(source.getSystemId());
                            } catch (Exception ex) {
                                //ignore - not DOM level 3
                            }
                            node =  node.getFirstChild();
                        }
                        
                        while (node.getNodeType() != Node.ELEMENT_NODE) {
                            node = node.getNextSibling();
                        }
                        
                        anyList.add(node);
                    }
                }
            } catch (XMLStreamException te) {
                throw new EndpointUtilsException(new Message("COULD_NOT_POPULATE_EPR", LOG),
                                                 te);
            }
        }
    }
   
    /**
     * Gets the WSDL definition for the provided endpoint reference.
     * @param manager - the WSDL manager 
     * @param ref - the endpoint reference
     * @return Definition the wsdl definition
     * @throws WSDLException
     */
    public static Definition getWSDLDefinition(WSDLManager manager, EndpointReferenceType ref)
        throws WSDLException {

        if (null == manager) {
            return null;
        }

        MetadataType metadata = ref.getMetadata();
        String location = getWSDLLocation(ref);

        if (null != location) {
            //Pick up the first url to obtain the wsdl defintion
            return manager.getDefinition(location);
        }

        for (Object obj : metadata.getAny()) {
            if (obj instanceof Element) {
                Element el = (Element)obj;
                if (StringUtils.isEqualUri(el.getNamespaceURI(), WSDLConstants.NS_WSDL11)
                    && "definitions".equals(el.getLocalName())) {
                    return manager.getDefinition(el);
                }
            }
        }

        return null;
    }
    
    
    private static synchronized Schema createSchema(ServiceInfo serviceInfo, Bus b) {
        if (b == null) {
            b = BusFactory.getThreadDefaultBus(false);
        }
        Schema schema = serviceInfo.getProperty(Schema.class.getName(), Schema.class);
        if (schema == null) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Map<String, byte[]> schemaSourcesMap = new LinkedHashMap<String, byte[]>();
            Map<String, Source> schemaSourcesMap2 = new LinkedHashMap<String, Source>();

            XMLStreamWriter writer = null;
            try {
                for (SchemaInfo si : serviceInfo.getSchemas()) {
                    Element el = si.getElement();
                    unsetReadonly(el);
                    String baseURI = null;
                    try {
                        baseURI = el.getBaseURI();
                    } catch (Exception ex) {
                        //ignore - not DOM level 3
                    }
                    if (baseURI == null) {
                        baseURI = si.getSystemId();
                    }
                    DOMSource ds = new DOMSource(el, baseURI);   
                    schemaSourcesMap2.put(si.getSystemId() + ":" + si.getNamespaceURI(), ds);
                    LoadingByteArrayOutputStream out = new LoadingByteArrayOutputStream();
                    writer = StaxUtils.createXMLStreamWriter(out);
                    StaxUtils.copy(el, writer);
                    writer.flush();
                    schemaSourcesMap.put(si.getSystemId() + ":" + si.getNamespaceURI(), out.toByteArray());
                }

                
                for (XmlSchema sch : serviceInfo.getXmlSchemaCollection().getXmlSchemas()) {
                    if (sch.getSourceURI() != null
                        && !schemaSourcesMap.containsKey(sch.getSourceURI() + ":" 
                                                         + sch.getTargetNamespace())) { 
                        
                        InputStream ins = null;
                        try {
                            URL url = new URL(sch.getSourceURI());
                            ins = url.openStream();
                        } catch (Exception e) {
                            //ignore, we'll just use what we have.  (though
                            //bugs in XmlSchema could make this less useful)
                        }
                        
                        LoadingByteArrayOutputStream out = new LoadingByteArrayOutputStream();
                        if (ins == null) {
                            sch.write(out);
                        } else {
                            IOUtils.copyAndCloseInput(ins, out);
                        }

                        schemaSourcesMap.put(sch.getSourceURI() + ":" 
                                             + sch.getTargetNamespace(), out.toByteArray());
                        
                        Source source = new StreamSource(out.createInputStream(), sch.getSourceURI());
                        schemaSourcesMap2.put(sch.getSourceURI() + ":" 
                                              + sch.getTargetNamespace(), source);
                    }
                } 


                factory.setResourceResolver(new SchemaLSResourceResolver(schemaSourcesMap, b));
                schema = factory.newSchema(schemaSourcesMap2.values()
                                           .toArray(new Source[schemaSourcesMap2.size()]));
                
                
            } catch (Exception ex) {
                // Something not right with the schema from the wsdl.
                LOG.log(Level.WARNING, "SAXException for newSchema()", ex);
                for (SchemaInfo schemaInfo : serviceInfo.getSchemas()) {
                    String s = XMLUtils.toString(schemaInfo.getElement(), 4);
                    LOG.log(Level.INFO, "Schema for: " + schemaInfo.getNamespaceURI() + "\n" + s);
                }
            } finally { 
                for (Source src : schemaSourcesMap2.values()) {
                    if (src instanceof DOMSource) {
                        Node nd = ((DOMSource)src).getNode();
                        unsetReadonly(nd);
                    }
                }
                StaxUtils.close(writer);
            }
            serviceInfo.setProperty(Schema.class.getName(), schema);
        }
        return schema;
    }
    
    private static void unsetReadonly(Node nd) {
        try {
            //work around a bug in the version of Xerces that is in the JDK
            //that only allows the Element to be used to create a schema once.
            nd.getClass().getMethod("setReadOnly", Boolean.TYPE, Boolean.TYPE).invoke(nd, false, true);
        } catch (Throwable ex) {
            //ignore
        }
    }

    public static Schema getSchema(ServiceInfo serviceInfo) {
        return getSchema(serviceInfo, null);
    }
    public static Schema getSchema(ServiceInfo serviceInfo, Bus b) {
        if (serviceInfo == null) {
            return null;
        }
        Schema schema = serviceInfo.getProperty(Schema.class.getName(), Schema.class);
        if (schema == null && !serviceInfo.hasProperty(Schema.class.getName())) {
            synchronized (serviceInfo) {
                return createSchema(serviceInfo, b);
            }
        }
        return schema;
    }
    

    /**
     * Gets the WSDL port for the provided endpoint reference.
     * @param manager - the WSDL manager 
     * @param ref - the endpoint reference
     * @return Port the wsdl port
     * @throws WSDLException
     */
    public static Port getPort(WSDLManager manager, EndpointReferenceType ref) throws WSDLException {

        Definition def = getWSDLDefinition(manager, ref);
        if (def == null) {
            throw new WSDLException(WSDLException.OTHER_ERROR, "unable to find definition for reference");
        }

        MetadataType metadata = ref.getMetadata();
        for (Object obj : metadata.getAny()) {
            
            if (obj instanceof JAXBElement) {
                Object jaxbVal = ((JAXBElement<?>)obj).getValue();

                if (jaxbVal instanceof ServiceNameType) {
                    Port port = null;
                    ServiceNameType snt = (ServiceNameType)jaxbVal;
                    LOG.log(Level.FINEST, "found service name " + snt.getValue().getLocalPart());
                    Service service = def.getService(snt.getValue());
                    if (service == null) {
                        LOG.log(Level.WARNING, "can't find the service name ["
                                + snt.getValue()
                                + "], using the default service name in wsdl");
                        service = (Service)def.getServices().values().iterator().next();
                        if (service == null) {
                            return null;
                        }
                    }
                    String endpoint = snt.getEndpointName();
                    if ("".equals(endpoint) && service.getPorts().size() == 1) {
                        port = (Port)service.getPorts().values().iterator().next();
                    } else {
                        port = service.getPort(endpoint);
                    }
                    // FIXME this needs to be looked at service.getPort(endpoint)
                    //should not return null when endpoint is valid
                    if (port == null) {
                        LOG.log(Level.WARNING, "can't find the port name ["
                                + endpoint
                                + "], using the default port name in wsdl");
                        port = (Port)service.getPorts().values().iterator().next();
                    }
                    return port;
                }
            }
        }

        if (def.getServices().size() == 1) {
            Service service = (Service)def.getServices().values().iterator().next();
            if (service.getPorts().size() == 1) { 
                return (Port)service.getPorts().values().iterator().next();
            }
        }
        
        QName serviceName = getServiceName(ref, null);
        if (null != serviceName) {
            if (StringUtils.isEmpty(serviceName.getNamespaceURI())) {
                serviceName = new QName(def.getTargetNamespace(), serviceName.getLocalPart());
            }
            Service service = def.getService(serviceName);
            if (service == null) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Cannot find service for " + serviceName);
            }
            if (service.getPorts().size() == 1) {
                return (Port)service.getPorts().values().iterator().next();
            }
            String str = getPortName(ref);
            LOG.log(Level.FINE, "getting port " + str + " from service " + service.getQName());
            Port port = service.getPort(str);
            if (port == null) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "unable to find port " + str);
            }
            return port;
        }
        // TODO : throw exception here
        return null;
    }

    /**
     * Get the address from the provided endpoint reference.
     * @param ref - the endpoint reference
     * @return String the address of the endpoint
     */
    public static String getAddress(EndpointReferenceType ref) {
        return WSAEndpointReferenceUtils.getAddress(ref);
    }

    /**
     * Set the address of the provided endpoint reference.
     * @param ref - the endpoint reference
     * @param address - the address
     */
    public static void setAddress(EndpointReferenceType ref, String address) {
        WSAEndpointReferenceUtils.setAddress(ref, address);
    }
    /**
     * Create an endpoint reference for the provided wsdl, service and portname.
     * @param wsdlUrl - url of the wsdl that describes the service.
     * @param serviceName - the <code>QName</code> of the service.
     * @param portName - the name of the port.
     * @return EndpointReferenceType - the endpoint reference
     */
    public static EndpointReferenceType getEndpointReference(URL wsdlUrl, 
                                                             QName serviceName,
                                                             String portName) {
        EndpointReferenceType reference = 
            WSAEndpointReferenceUtils.createEndpointReferenceWithMetadata();
        setServiceAndPortName(reference, serviceName, portName);
        //TODO To Ensure it is a valid URI syntax.
        setWSDLLocation(reference, wsdlUrl.toString());

        return reference;
    }
    
    
    /**
     * Create a duplicate endpoint reference sharing all atributes
     * @param ref the reference to duplicate
     * @return EndpointReferenceType - the duplicate endpoint reference
     */
    public static EndpointReferenceType duplicate(EndpointReferenceType ref) {

        return WSAEndpointReferenceUtils.duplicate(ref);
    }
    
    /**
     * Create an endpoint reference for the provided address.
     * @param address - address URI
     * @return EndpointReferenceType - the endpoint reference
     */
    public static EndpointReferenceType getEndpointReference(String address) {

        return WSAEndpointReferenceUtils.getEndpointReference(address);
    }
    
    public static EndpointReferenceType getEndpointReference(AttributedURIType address) {

        return WSAEndpointReferenceUtils.getEndpointReference(address);
    }    
    
    /**
     * Create an anonymous endpoint reference.
     * @return EndpointReferenceType - the endpoint reference
     */
    public static EndpointReferenceType getAnonymousEndpointReference() {
        
        return WSAEndpointReferenceUtils.getAnonymousEndpointReference();
    }
    
    /**
     * Resolve logical endpoint reference via the Bus EndpointResolverRegistry.
     * 
     * @param logical the abstract EPR to resolve
     * @return the resolved concrete EPR if appropriate, null otherwise
     */
    public static EndpointReferenceType resolve(EndpointReferenceType logical, Bus bus) {
        EndpointReferenceType physical = null;
        if (bus != null) {
            EndpointResolverRegistry registry =
                bus.getExtension(EndpointResolverRegistry.class);
            if (registry != null) {
                physical = registry.resolve(logical);
            }
        }
        return physical != null ? physical : logical;
    }

    
    /**
     * Renew logical endpoint reference via the Bus EndpointResolverRegistry.
     * 
     * @param logical the original abstract EPR (if still available)
     * @param physical the concrete EPR to renew
     * @return the renewed concrete EPR if appropriate, null otherwise
     */
    public static EndpointReferenceType renew(EndpointReferenceType logical,
                                              EndpointReferenceType physical,
                                              Bus bus) {
        EndpointReferenceType renewed = null;
        if (bus != null) {
            EndpointResolverRegistry registry =
                bus.getExtension(EndpointResolverRegistry.class);
            if (registry != null) {
                renewed = registry.renew(logical, physical);
            }
        }
        return renewed != null ? renewed : physical;
    }

    /**
     * Mint logical endpoint reference via the Bus EndpointResolverRegistry.
     * 
     * @param serviceName the given serviceName
     * @return the newly minted EPR if appropriate, null otherwise
     */
    public static EndpointReferenceType mint(QName serviceName, Bus bus) {
        EndpointReferenceType logical = null;
        if (bus != null) {
            EndpointResolverRegistry registry =
                bus.getExtension(EndpointResolverRegistry.class);
            if (registry != null) {
                logical = registry.mint(serviceName);
            }
        }
        return logical;
    }
    
    /**
     * Mint logical endpoint reference via the Bus EndpointResolverRegistry.
     * 
     * @param physical the concrete template EPR 
     * @return the newly minted EPR if appropriate, null otherwise
     */
    public static EndpointReferenceType mint(EndpointReferenceType physical, Bus bus) {
        EndpointReferenceType logical = null;
        if (bus != null) {
            EndpointResolverRegistry registry =
                bus.getExtension(EndpointResolverRegistry.class);
            if (registry != null) {
                logical = registry.mint(physical);
            }
        }
        return logical != null ? logical : physical;
    }
                                             
    private static String getNameSpaceUri(Node node, String content, String namespaceURI) {
        if (namespaceURI == null) {
            namespaceURI =  node.lookupNamespaceURI(content.substring(0, 
                                                                  content.indexOf(":")));
        }
        return namespaceURI;
    }

    private static String getService(String content) {
        return content.substring(content.indexOf(":") + 1, content.length());
    }

    /**
     * Obtain a multiplexed endpoint reference for the deployed service that contains the provided id
     * @param serviceQName identified the target service
     * @param portName identifies a particular port of the service, may be null
     * @param id that must be embedded in the returned reference
     * @param bus the current bus
     * @return a new reference or null if the target destination does not support destination mutiplexing  
     */
    public static EndpointReferenceType getEndpointReferenceWithId(QName serviceQName, 
                                                                   String portName, 
                                                                   String id, 
                                                                   Bus bus) {
        EndpointReferenceType epr = null;        
        MultiplexDestination destination = getMatchingMultiplexDestination(serviceQName, portName, bus);
        if (null != destination) {
            epr = destination.getAddressWithId(id);
        }
        return epr;
    }
    
    /**
     * Obtain the id String from the endpoint reference of the current dispatch. 
     * @param messageContext the current message context 
     * @return the id embedded in the current endpoint reference or null if not found
     */
    public static String getEndpointReferenceId(Map<String, Object> messageContext) {
        String id = null;
        Destination destination = (Destination) messageContext.get(Destination.class.getName());
        if (destination instanceof MultiplexDestination) {
            id = ((MultiplexDestination) destination).getId(messageContext);
        }
        return id;
    }
    

    private static synchronized JAXBContext createContextForEPR() throws JAXBException {
        Reference<JAXBContext> rctx = ADDRESSING_CONTEXT.get();
        JAXBContext ctx = rctx.get();
        if (ctx == null) {
            ctx = JAXBContextCache.getCachedContextAndSchemas(ADDRESSING_CLASSES,
                                                              null, null, null,
                                                              true).getContext();
            ADDRESSING_CONTEXT.set(new SoftReference<JAXBContext>(ctx));
        }    
        return ctx;
    }
    private static JAXBContext getJAXBContextForEPR() throws JAXBException {
        Reference<JAXBContext> rctx = ADDRESSING_CONTEXT.get();
        JAXBContext ctx = rctx.get();
        if (ctx == null) {
            ctx = createContextForEPR();
        }
        return ctx;
    }
    public static Source convertToXML(EndpointReferenceType epr) {
        try {
            javax.xml.bind.Marshaller jm = getJAXBContextForEPR().createMarshaller();
            jm.setProperty(Marshaller.JAXB_FRAGMENT, true);
            QName qname = new QName("http://www.w3.org/2005/08/addressing", "EndpointReference");
            JAXBElement<EndpointReferenceType> 
            jaxEle = new JAXBElement<EndpointReferenceType>(qname, EndpointReferenceType.class, epr);
            
            
            W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
            jm.marshal(jaxEle, writer); 
            return new DOMSource(writer.getDocument());
        } catch (JAXBException e) {
            //ignore
        } 
        return null;
    }
    
    
    private static MultiplexDestination getMatchingMultiplexDestination(QName serviceQName, String portName,
                                                                        Bus bus) {
        MultiplexDestination destination = null;
        ServerRegistry serverRegistry = bus.getExtension(ServerRegistry.class);
        if (null != serverRegistry) {
            List<Server> servers = serverRegistry.getServers();
            for (Server s : servers) {
                QName targetServiceQName = s.getEndpoint().getEndpointInfo().getService().getName();
                if (serviceQName.equals(targetServiceQName) && portNameMatches(s, portName)) {
                    Destination dest = s.getDestination();
                    if (dest instanceof MultiplexDestination) {
                        destination = (MultiplexDestination)dest;
                        break;
                    }
                }
            }
        } else {
            LOG.log(Level.WARNING,
                    "Failed to locate service matching " + serviceQName 
                    + ", because the bus ServerRegistry extension provider is null");
        }
        return destination;
    }

    private static boolean portNameMatches(Server s, String portName) {
        boolean ret = false;
        if (null == portName 
            || portName.equals(s.getEndpoint().getEndpointInfo().getName().getLocalPart())) {
            return true;
        }
        return ret;
    }
    
   
    
}

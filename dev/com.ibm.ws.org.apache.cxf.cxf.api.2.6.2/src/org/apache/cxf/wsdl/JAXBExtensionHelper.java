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

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionDeserializer;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.ExtensionSerializer;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.util.StreamReaderDelegate;

import org.w3c.dom.Element;

import com.ibm.websphere.ras.annotation.Trivial;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.jaxb.JAXBContextCache.CachedContextAndSchemas;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.staxutils.StaxUtils;


/**
 * JAXBExtensionHelper
 * @author dkulp
 *
 */
public class JAXBExtensionHelper implements ExtensionSerializer, ExtensionDeserializer {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXBExtensionHelper.class);

    final Class<? extends ExtensibilityElement> typeClass;
    final String namespace;
    String jaxbNamespace;

    private JAXBContext context;
    private Set<Class<?>> classes;

    @Trivial
    public JAXBExtensionHelper(Class<? extends ExtensibilityElement> cls,
                               String ns) {
        typeClass = cls;
        namespace = ns;
    }
    @Trivial
    void setJaxbNamespace(String ns) {
        jaxbNamespace = ns;
    }
    
    @Trivial
    public static void addExtensions(ExtensionRegistry registry, String parentType, String elementType)
        throws JAXBException, ClassNotFoundException {
        Class<?> parentTypeClass = ClassLoaderUtils.loadClass(parentType, JAXBExtensionHelper.class);

        Class<? extends ExtensibilityElement> elementTypeClass = 
            ClassLoaderUtils.loadClass(elementType, JAXBExtensionHelper.class)
                .asSubclass(ExtensibilityElement.class);
        addExtensions(registry, parentTypeClass, elementTypeClass, null);
    }
    @Trivial
    public static void addExtensions(ExtensionRegistry registry,
                                     String parentType, 
                                     String elementType,
                                     String namespace)
        throws JAXBException, ClassNotFoundException {
        Class<?> parentTypeClass = ClassLoaderUtils.loadClass(parentType, JAXBExtensionHelper.class);

        Class<? extends ExtensibilityElement> elementTypeClass = 
            ClassLoaderUtils.loadClass(elementType, JAXBExtensionHelper.class)
                .asSubclass(ExtensibilityElement.class);
        addExtensions(registry, parentTypeClass, elementTypeClass, namespace);
    }
    @Trivial
    public static void addExtensions(ExtensionRegistry registry,
                                     Class<?> parentType,
                                     Class<? extends ExtensibilityElement> cls)
        throws JAXBException {
        addExtensions(registry, parentType, cls, null);
    }
    
    public static void addExtensions(ExtensionRegistry registry,
                                     Class<?> parentType,
                                     Class<? extends ExtensibilityElement> cls,
                                     String namespace) throws JAXBException {
        
        JAXBExtensionHelper helper = new JAXBExtensionHelper(cls, namespace);
        boolean found = false;
        try {
            Class<?> objectFactory = Class.forName(PackageUtils.getPackageName(cls) + ".ObjectFactory",
                                                   true, cls.getClassLoader());
            Method methods[] = objectFactory.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0].equals(cls)) {
                    
                    XmlElementDecl elementDecl = method.getAnnotation(XmlElementDecl.class);
                    if (null != elementDecl) {
                        String name = elementDecl.name();
                        String ns = namespace != null ? namespace : elementDecl.namespace();
                        if (namespace != null) {
                            helper.setJaxbNamespace(elementDecl.namespace());
                        }
                        QName elementType = new QName(ns, name);
                        registry.registerDeserializer(parentType, elementType, helper); 
                        registry.registerSerializer(parentType, elementType, helper);                         
                        registry.mapExtensionTypes(parentType, elementType, cls);
                        found = true;
                    }                    
                }
            }        
            
        } catch (ClassNotFoundException ex) {
            //ignore
        }        
        if (!found) {
            //not in object factory or no object factory, try other annotations
            XmlRootElement elAnnot = cls.getAnnotation(XmlRootElement.class);
            if (elAnnot != null) {
                String name = elAnnot.name();
                String ns = elAnnot.namespace();
                if (StringUtils.isEmpty(ns)
                    || "##default".equals(ns)) {
                    XmlSchema schema = null;
                    if (cls.getPackage() != null) {
                        schema = cls.getPackage().getAnnotation(XmlSchema.class);
                    }
                    if (schema != null) {
                        ns = schema.namespace();
                    }
                }
                if (!StringUtils.isEmpty(ns) && !StringUtils.isEmpty(name)) {
                    if (namespace != null) {
                        helper.setJaxbNamespace(ns);
                        ns = namespace;
                    }
                    QName elementType = new QName(ns, name);
                    registry.registerDeserializer(parentType, elementType, helper); 
                    registry.registerSerializer(parentType, elementType, helper);                         
                    registry.mapExtensionTypes(parentType, elementType, cls);

                    found = true;
                }
            }
        }
        
        if (!found) {
            LOG.log(Level.WARNING, "EXTENSION_NOT_REGISTERED", 
                    new Object[] {cls.getName(), parentType.getName()});
        }
    }

    @Trivial
    private synchronized JAXBContext getContext() throws JAXBException {
        if (context == null || classes == null) {
            try {
                CachedContextAndSchemas ccs 
                    = JAXBContextCache.getCachedContextAndSchemas(typeClass);
                classes = ccs.getClasses();
                context = ccs.getContext();
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
        return context;
    }
    
    /* (non-Javadoc)
     * @see javax.wsdl.extensions.ExtensionSerializer#marshall(java.lang.Class,
     *  javax.xml.namespace.QName, javax.wsdl.extensions.ExtensibilityElement,
     *   java.io.PrintWriter, javax.wsdl.Definition, javax.wsdl.extensions.ExtensionRegistry)
     */
    public void marshall(@SuppressWarnings("rawtypes") Class parent, QName qname,
                         ExtensibilityElement obj, PrintWriter pw,
                         final Definition wsdl, ExtensionRegistry registry) throws WSDLException {
        // TODO Auto-generated method stub
        try {
            Marshaller u = getContext().createMarshaller();
            u.setProperty("jaxb.encoding", "UTF-8");
            u.setProperty("jaxb.fragment", Boolean.TRUE);
            u.setProperty("jaxb.formatted.output", Boolean.TRUE);
            
            Object mObj = obj;
            
            Class<?> objectFactory = Class.forName(PackageUtils.getPackageName(typeClass) + ".ObjectFactory",
                                                   true,
                                                   obj.getClass().getClassLoader());
            Method methods[] = objectFactory.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0].equals(typeClass)) {
                    
                    mObj = method.invoke(objectFactory.newInstance(), new Object[] {obj});
                }
            }

            javax.xml.stream.XMLOutputFactory fact = javax.xml.stream.XMLOutputFactory.newInstance();
            XMLStreamWriter writer =
                new PrettyPrintXMLStreamWriter(fact.createXMLStreamWriter(pw), parent);
            writer.setNamespaceContext(new javax.xml.namespace.NamespaceContext() {
                
                public String getNamespaceURI(String arg) {
                    return wsdl.getNamespace(arg);
                }
                                
                public String getPrefix(String arg) {
                    if (arg.equals(jaxbNamespace)) {
                        arg = namespace;
                    }
                    
                    for (Object ent : wsdl.getNamespaces().entrySet()) {
                        Map.Entry<?, ?> entry = (Map.Entry<?, ?>)ent;
                        if (arg.equals(entry.getValue())) {
                            return (String)entry.getKey();
                        }
                    }
                    return null;
                }
                
                public Iterator<String> getPrefixes(String arg) {
                    if (arg.equals(jaxbNamespace)) {
                        arg = namespace;
                    }
                    Iterator<String> ret = CastUtils.cast(wsdl.getNamespaces().keySet().iterator());
                    return ret;
                }
            });
            
            u.marshal(mObj, writer);
            writer.flush();            
        } catch (Exception ex) {
            throw new WSDLException(WSDLException.PARSER_ERROR,
                                    "",
                                    ex);
        }

    }

    /* (non-Javadoc)
     * @see javax.wsdl.extensions.ExtensionDeserializer#unmarshall(java.lang.Class,
     *  javax.xml.namespace.QName, org.w3c.dom.Element,
     *   javax.wsdl.Definition,
     *   javax.wsdl.extensions.ExtensionRegistry)
     */
    @Trivial
    public ExtensibilityElement unmarshall(@SuppressWarnings("rawtypes") Class parent, 
                                           QName qname, Element element, Definition wsdl,
                                           ExtensionRegistry registry) throws WSDLException {
        XMLStreamReader reader = null;
        try {
            Unmarshaller u = getContext().createUnmarshaller();
        
            Object o = null;
            if (namespace == null) {
                o = u.unmarshal(element);
            } else {
                reader = StaxUtils.createXMLStreamReader(element);
                reader = new MappingReaderDelegate(reader);
                o = u.unmarshal(reader);
            }
            if (o instanceof JAXBElement<?>) {
                JAXBElement<?> el = (JAXBElement<?>)o;
                o = el.getValue();
            }
            
            ExtensibilityElement el = o instanceof ExtensibilityElement ? (ExtensibilityElement)o : null;
            if (null != el) {
                el.setElementType(qname);
            }
            return el;
        } catch (Exception ex) {
            throw new WSDLException(WSDLException.PARSER_ERROR,
                                    "Error reading element " + qname,
                                    ex);
        } finally {
            StaxUtils.close(reader);
        }
    }
    
    


    @Trivial
    class MappingReaderDelegate extends StreamReaderDelegate {
        MappingReaderDelegate(XMLStreamReader reader) {
            super(reader);
        }
        
        @Override
        public NamespaceContext getNamespaceContext() {
            final NamespaceContext ctx = super.getNamespaceContext();
            return new NamespaceContext() {
                public String getNamespaceURI(String prefix) {
                    String ns = ctx.getNamespaceURI(prefix);
                    if (namespace.equals(ns)) {
                        ns = jaxbNamespace;
                    }                        
                    return ns;
                }

                public String getPrefix(String namespaceURI) {
                    if (jaxbNamespace.equals(namespaceURI)) {
                        return ctx.getPrefix(namespace);
                    }
                    return ctx.getPrefix(namespaceURI);
                }

                @SuppressWarnings("rawtypes")
                public Iterator getPrefixes(String namespaceURI) {
                    if (jaxbNamespace.equals(namespaceURI)) {
                        return ctx.getPrefixes(namespace);
                    }
                    return ctx.getPrefixes(namespaceURI);
                }
            };
        }

        @Override
        public String getNamespaceURI(int index) {
            String ns = super.getNamespaceURI(index);
            if (namespace.equals(ns)) {
                ns = jaxbNamespace;
            }                        
            return ns;                     
        }

        @Override
        public String getNamespaceURI(String prefix) {
            String ns = super.getNamespaceURI(prefix);
            if (namespace.equals(ns)) {
                ns = jaxbNamespace;
            }                        
            return ns;
        }

        @Override
        public QName getName() {
            QName qn = super.getName();
            if (namespace.equals(qn.getNamespaceURI())) {
                qn = new QName(jaxbNamespace, qn.getLocalPart());
            }
            return qn;
        }

        @Override
        public String getNamespaceURI() {
            String ns = super.getNamespaceURI();
            if (namespace.equals(ns)) {
                ns = jaxbNamespace;
            }                        
            return ns; 
        }
        
    };
    

}

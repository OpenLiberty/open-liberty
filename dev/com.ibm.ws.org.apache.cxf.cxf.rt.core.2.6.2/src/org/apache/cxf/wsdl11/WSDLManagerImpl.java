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

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.wsdl.BindingInput;
import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.InputSource;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.catalog.CatalogWSDLLocator;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.CacheMap;
import org.apache.cxf.common.util.PropertiesLoaderUtils;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.service.model.ServiceSchemaInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.wsdl.JAXBExtensionHelper;
import org.apache.cxf.wsdl.WSDLExtensionLoader;
import org.apache.cxf.wsdl.WSDLManager;

/**
 * WSDLManagerImpl
 * 
 * @author dkulp
 */
@NoJSR250Annotations(unlessNull = "bus")
public class WSDLManagerImpl implements WSDLManager {

    private static final Logger LOG = LogUtils.getL7dLogger(WSDLManagerImpl.class);

    private static final String EXTENSIONS_RESOURCE = "META-INF/cxf/extensions.xml";
    private static final String EXTENSIONS_RESOURCE_COMPAT = "META-INF/extensions.xml";

    final ExtensionRegistry registry;
    final WSDLFactory factory;
    final Map<Object, Definition> definitionsMap;
    
    /**
     * The schemaCacheMap is used as a cache of SchemaInfo against the WSDLDefinitions.
     * The key is the same key that is used to hold the definition object into the definitionsMap 
     */
    final Map<Object, ServiceSchemaInfo> schemaCacheMap;
    private boolean disableSchemaCache;
    
    private Bus bus;

    public WSDLManagerImpl() throws BusException {
        try {
            factory = WSDLFactory.newInstance();
            registry = factory.newPopulatedExtensionRegistry();
            registry.registerSerializer(Types.class, 
                                        WSDLConstants.QNAME_SCHEMA,
                                        new SchemaSerializer());
            // these will replace whatever may have already been registered
            // in these places, but there's no good way to check what was 
            // there before.
            QName header = new QName(WSDLConstants.NS_SOAP, "header");
            registry.registerDeserializer(MIMEPart.class, 
                                          header, 
                                          registry.queryDeserializer(BindingInput.class, header));
            registry.registerSerializer(MIMEPart.class, 
                                        header, 
                                        registry.querySerializer(BindingInput.class, header));
            // get the original classname of the SOAPHeader
            // implementation that was stored in the registry.  
            Class<? extends ExtensibilityElement> clazz = 
                registry.createExtension(BindingInput.class, header).getClass();
            registry.mapExtensionTypes(MIMEPart.class, header, clazz);
        } catch (WSDLException e) {
            throw new BusException(e);
        }
        definitionsMap = new CacheMap<Object, Definition>();
        schemaCacheMap = new CacheMap<Object, ServiceSchemaInfo>();

        registerInitialExtensions();
    }
    public WSDLManagerImpl(Bus b) throws BusException {
        this();
        setBus(b);
    }
    
    @Resource
    public final void setBus(Bus b) {
        bus = b;
        if (null != bus) {
            bus.setExtension(this, WSDLManager.class);
            ConfiguredBeanLocator loc = bus.getExtension(ConfiguredBeanLocator.class);
            if (loc != null) {
                loc.getBeansOfType(WSDLExtensionLoader.class);
            }
        }
    }

    public WSDLFactory getWSDLFactory() {
        return factory;
    }
    
    public Map<Object, Definition> getDefinitions() {
        synchronized (definitionsMap) {
            return Collections.unmodifiableMap(definitionsMap);
        }
    }
    

    /*
     * (non-Javadoc)
     * 
     * XXX - getExtensionRegistry()
     * 
     * @see org.apache.cxf.wsdl.WSDLManager#getExtenstionRegistry()
     */
    public ExtensionRegistry getExtensionRegistry() {
        return registry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.wsdl.WSDLManager#getDefinition(java.net.URL)
     */
    public Definition getDefinition(URL url) throws WSDLException { 
        synchronized (definitionsMap) {
            if (definitionsMap.containsKey(url)) {
                return definitionsMap.get(url);
            }
        }
        Definition def = loadDefinition(url.toString());
        synchronized (definitionsMap) {
            definitionsMap.put(url, def);
        }
        return def;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.wsdl.WSDLManager#getDefinition(java.lang.String)
     */
    public Definition getDefinition(String url) throws WSDLException {
        synchronized (definitionsMap) {
            if (definitionsMap.containsKey(url)) {
                return definitionsMap.get(url);
            }
        }
        return loadDefinition(url);
    }

    public Definition getDefinition(Element el) throws WSDLException {
        synchronized (definitionsMap) {
            if (definitionsMap.containsKey(el)) {
                return definitionsMap.get(el);
            }
        }
        WSDLReader reader = factory.newWSDLReader();
        reader.setFeature("javax.wsdl.verbose", false);
        reader.setExtensionRegistry(registry);       
        Definition def = reader.readWSDL("", el);
        synchronized (definitionsMap) {
            definitionsMap.put(el, def);
        }
        return def;
    }


    public void addDefinition(Object key, Definition wsdl) {
        synchronized (definitionsMap) {
            definitionsMap.put(key, wsdl);
        }
    }

    private Definition loadDefinition(String url) throws WSDLException {
        WSDLReader reader = factory.newWSDLReader();
        reader.setFeature("javax.wsdl.verbose", false);
        reader.setFeature("javax.wsdl.importDocuments", true);
        reader.setExtensionRegistry(registry);
        CatalogWSDLLocator catLocator = new CatalogWSDLLocator(url, bus);
        ResourceManagerWSDLLocator wsdlLocator = new ResourceManagerWSDLLocator(url,
                                                                                catLocator,
                                                                                bus);
        InputSource src = wsdlLocator.getBaseInputSource();
        Definition def = null;
        if (src.getByteStream() != null || src.getCharacterStream() != null) {
            Document doc;
            XMLStreamReader xmlReader = null;
            try {
                xmlReader = StaxUtils.createXMLStreamReader(src);
                doc = StaxUtils.read(xmlReader, true);
                if (src.getSystemId() != null) {
                    try {
                        doc.setDocumentURI(new String(src.getSystemId()));
                    } catch (Exception e) {
                        //ignore - probably not DOM level 3
                    }
                }
            } catch (Exception e) {
                throw new WSDLException(WSDLException.PARSER_ERROR, e.getMessage(), e);
            } finally {
                StaxUtils.close(xmlReader);
            }
            def = reader.readWSDL(wsdlLocator, doc.getDocumentElement());
        } else {
            def = reader.readWSDL(wsdlLocator);
        }
        
        synchronized (definitionsMap) {
            definitionsMap.put(url, def);
        }
        return def;
    }

    private void registerInitialExtensions() throws BusException {
        registerInitialXmlExtensions(EXTENSIONS_RESOURCE_COMPAT);
        registerInitialXmlExtensions(EXTENSIONS_RESOURCE);
    }
    
    private void registerInitialXmlExtensions(String resource) throws BusException {
        Properties initialExtensions = null;
        try {
            ClassLoader cl = null;
            if (bus != null) {
                cl = bus.getExtension(ClassLoader.class);
            }
            if (cl != null) {
                initialExtensions = PropertiesLoaderUtils.loadAllProperties(resource, cl);
            }
            //use TCCL as fallback so that can load resources from other bundles in OSGi
            if (initialExtensions == null || initialExtensions.size() == 0) {
                ClassLoader pcl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                });
                initialExtensions = PropertiesLoaderUtils.loadAllProperties(resource, pcl);
            }
        } catch (IOException ex) {
            throw new BusException(ex);
        }

        for (Iterator<?> it = initialExtensions.keySet().iterator(); it.hasNext();) {
            StringTokenizer st = new StringTokenizer(initialExtensions.getProperty((String) it.next()), "=");
            String parentType = st.nextToken();
            String elementType = st.nextToken();
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Registering extension: " + elementType + " for parent: " + parentType);
                }
                JAXBExtensionHelper.addExtensions(registry, parentType, elementType);
            } catch (ClassNotFoundException ex) {
                LOG.log(Level.WARNING, "EXTENSION_ADD_FAILED_MSG", ex);
            } catch (JAXBException ex) {
                LOG.log(Level.WARNING, "EXTENSION_ADD_FAILED_MSG", ex);
            }
        }
    }

    public ServiceSchemaInfo getSchemasForDefinition(Definition wsdl) {
        if (disableSchemaCache) {
            return null;
        }
        synchronized (definitionsMap) {
            for (Map.Entry<Object, Definition> e : definitionsMap.entrySet()) {
                if (e.getValue() == wsdl) {
                    ServiceSchemaInfo info = schemaCacheMap.get(e.getKey());
                    if (info != null) {
                        return info;
                    }
                }
            }
        }
        return null;
    }

    public void putSchemasForDefinition(Definition wsdl, ServiceSchemaInfo schemas) {
        if (!disableSchemaCache) {
            synchronized (definitionsMap) {
                for (Map.Entry<Object, Definition> e : definitionsMap.entrySet()) {
                    if (e.getValue() == wsdl) {
                        schemaCacheMap.put(e.getKey(), schemas);
                    }
                }
            }            
        }
    }

    public boolean isDisableSchemaCache() {
        return disableSchemaCache;
    }

    /**
     * There's a test that 'fails' by succeeding if the cache is operational.
     * @param disableSchemaCache
     */
    public void setDisableSchemaCache(boolean disableSchemaCache) {
        this.disableSchemaCache = disableSchemaCache;
    }

    public void removeDefinition(Definition wsdl) {
        synchronized (definitionsMap) {
            List<Object> keys = new ArrayList<Object>();
            for (Map.Entry<Object, Definition> e : definitionsMap.entrySet()) {
                if (e.getValue() == wsdl) {
                    keys.add(e.getKey());
                }
            }
            for (Object o : keys) {
                definitionsMap.remove(o);
                schemaCacheMap.remove(o);
            }
        }
    }
    

}

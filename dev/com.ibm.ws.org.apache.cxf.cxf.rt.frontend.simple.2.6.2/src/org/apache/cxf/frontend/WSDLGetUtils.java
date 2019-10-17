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

package org.apache.cxf.frontend;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.extensions.schema.SchemaReference;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.InputSource;

import org.apache.cxf.Bus;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.catalog.OASISCatalogManagerHelper;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.ResourceManagerWSDLLocator;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;

/**
 * 
 */
public class WSDLGetUtils {
    
    public static final String AUTO_REWRITE_ADDRESS = "autoRewriteSoapAddress";
    public static final String AUTO_REWRITE_ADDRESS_ALL = "autoRewriteSoapAddressForAllServices";
    public static final String PUBLISHED_ENDPOINT_URL = "publishedEndpointUrl";
    public static final String WSDL_CREATE_IMPORTS = "org.apache.cxf.wsdl.create.imports";
    
    private static final String WSDLS_KEY = WSDLGetUtils.class.getName() + ".WSDLs";
    private static final String SCHEMAS_KEY = WSDLGetUtils.class.getName() + ".Schemas";
    
    private static final Logger LOG = LogUtils.getL7dLogger(WSDLGetInterceptor.class);
    
    public WSDLGetUtils() {
        //never constructed
    }


    public Set<String> getWSDLIds(Message message,
                            String base,
                            String ctxUri,
                            EndpointInfo endpointInfo) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("wsdl", "");
        new WSDLGetUtils().getDocument(message, base, 
                                       params, ctxUri, 
                                       endpointInfo);
        
        Map<String, Definition> mp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
                                                    .getProperty(WSDLS_KEY));
        return mp.keySet();
    }
    private String buildUrl(String base, String ctxUri, String s) {
        return base + ctxUri + "?" + s;
    }
    public Map<String, String> getSchemaLocations(Message message,
                                                  String base,
                                                  String ctxUri,
                                                  EndpointInfo endpointInfo) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("wsdl", "");
        new WSDLGetUtils().getDocument(message, base, 
                                       params, ctxUri, 
                                       endpointInfo);
      
        Map<String, SchemaReference> mp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
                                                         .getProperty(SCHEMAS_KEY));
        
        Map<String, String> schemas = new HashMap<String, String>();
        for (Map.Entry<String, SchemaReference> ent : mp.entrySet()) {
            params.clear();
            params.put("xsd", ent.getKey());
            Document doc = getDocument(message, base, params, ctxUri, endpointInfo);
            schemas.put(doc.getDocumentElement().getAttribute("targetNamespace"), 
                        buildUrl(base, ctxUri, "xsd=" + ent.getKey()));
        }
        return schemas;
    }

    public Document getDocument(Message message,
                            String base,
                            Map<String, String> params,
                            String ctxUri,
                            EndpointInfo endpointInfo) {
        try {
            Bus bus = message.getExchange().getBus();
            Object prop = message.getContextualProperty(PUBLISHED_ENDPOINT_URL);
            if (prop == null) {
                prop = endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL);
            }
            if (prop != null) {
                base = String.valueOf(prop);
            }

            String wsdl = params.get("wsdl");
            if (wsdl != null) {
                // Always use the URL decoded version to ensure that we have a
                // canonical representation of the import URL for lookup.
                wsdl = URLDecoder.decode(wsdl, "utf-8");
            }
            
            String xsd =  params.get("xsd");
            if (xsd != null) {
                // Always use the URL decoded version to ensure that we have a
                // canonical representation of the import URL for lookup.
                xsd = URLDecoder.decode(xsd, "utf-8");
            }
            
            Map<String, Definition> mp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
                                                        .getProperty(WSDLS_KEY));
            Map<String, SchemaReference> smp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
                                                        .getProperty(SCHEMAS_KEY));

            if (mp == null) {
                endpointInfo.getService().setProperty(WSDLS_KEY,
                                                      new ConcurrentHashMap<String, Definition>(8, 0.75f, 4));
                mp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
                                    .getProperty(WSDLS_KEY));
            }
            if (smp == null) {
                endpointInfo.getService().setProperty(SCHEMAS_KEY,
                                                      new ConcurrentHashMap<String, SchemaReference>(8, 0.75f, 4));
                smp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
                                    .getProperty(SCHEMAS_KEY));
            }
            
            if (!mp.containsKey("")) {
                ServiceWSDLBuilder builder = 
                    new ServiceWSDLBuilder(bus, endpointInfo.getService());

                builder.setUseSchemaImports(
                     MessageUtils.getContextualBoolean(message, WSDL_CREATE_IMPORTS, false));
                
                // base file name is ignored if createSchemaImports == false!
                builder.setBaseFileName(endpointInfo.getService().getName().getLocalPart());
                
                Definition def = builder.build(new HashMap<String, SchemaInfo>());

                mp.put("", def);
                updateDefinition(bus, def, mp, smp, base, endpointInfo);
            }
            
            
            Document doc;
            if (xsd == null) {
                Definition def = mp.get(wsdl);
                if (def == null) {
                    String wsdl2 = resolveWithCatalogs(OASISCatalogManager.getCatalogManager(bus),
                                                       wsdl,
                                                       base);
                    if (wsdl2 != null) {
                        def = mp.get(wsdl2);
                    }
                }
                if (def == null) {
                    throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("WSDL_NOT_FOUND",
                                                                                        LOG, wsdl), null);
                }
                
                synchronized (def) {
                    //writing a def is not threadsafe.  Sync on it to make sure
                    //we don't get any ConcurrentModificationExceptions
                    if (endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL) != null) {
                        String epurl = 
                            String.valueOf(endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL));
                        updatePublishedEndpointUrl(epurl, def, endpointInfo.getName());
                        base = epurl;
                    }
        
                    WSDLWriter wsdlWriter = bus.getExtension(WSDLManager.class)
                        .getWSDLFactory().newWSDLWriter();
                    def.setExtensionRegistry(bus.getExtension(WSDLManager.class).getExtensionRegistry());
                    doc = wsdlWriter.getDocument(def);
                }
            } else {
                SchemaReference si = smp.get(xsd);
                if (si == null) {
                    String xsd2 = resolveWithCatalogs(OASISCatalogManager.getCatalogManager(bus),
                                                       xsd,
                                                       base);
                    if (xsd2 != null) { 
                        si = smp.get(xsd2);
                    }
                }
                if (si == null) {
                    throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("SCHEMA_NOT_FOUND", 
                                                                                        LOG, wsdl), null);
                }
                
                String uri = si.getReferencedSchema().getDocumentBaseURI();
                uri = resolveWithCatalogs(OASISCatalogManager.getCatalogManager(bus),
                                          uri,
                                          si.getReferencedSchema().getDocumentBaseURI());
                if (uri == null) {
                    uri = si.getReferencedSchema().getDocumentBaseURI();
                }
                ResourceManagerWSDLLocator rml = new ResourceManagerWSDLLocator(uri,
                                                                                bus);
                
                InputSource src = rml.getBaseInputSource();
                if (src.getByteStream() != null || src.getCharacterStream() != null) {
                    doc = StaxUtils.read(src);
                } else { // last resort lets try for the referenced schema itself.
                    // its not thread safe if we use the same document
                    doc = StaxUtils.read(
                            new DOMSource(si.getReferencedSchema().getElement().getOwnerDocument()));
                }
            }
            
            updateDoc(doc, base, mp, smp, message);
            
            return doc;
        } catch (WSDLQueryException wex) {
            throw wex;
        } catch (Exception wex) {
            throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                                                     LOG,
                                                     base), wex);
        }
    }
    
    protected String mapUri(String base, Map<String, SchemaReference> smp, String loc)
        throws UnsupportedEncodingException {
        SchemaReference ref = smp.get(URLDecoder.decode(loc, "utf-8"));
        if (ref != null) {
            return base + "?xsd=" + ref.getSchemaLocationURI().replace(" ", "%20");
        }
        return null;
    }
    
    protected void updateDoc(Document doc, 
                           String base,
                           Map<String, Definition> mp,
                           Map<String, SchemaReference> smp,
                           Message message) {        
        List<Element> elementList = null;
        
        try {
            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                                           "http://www.w3.org/2001/XMLSchema",
                                                                           "import");
            for (Element el : elementList) {
                String sl = el.getAttribute("schemaLocation");
                sl = mapUri(base, smp, sl);
                if (sl != null) {
                    el.setAttribute("schemaLocation", sl);
                }
            }
            
            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://www.w3.org/2001/XMLSchema",
                                                              "include");
            for (Element el : elementList) {
                String sl = el.getAttribute("schemaLocation");
                if (smp.containsKey(URLDecoder.decode(sl, "utf-8"))) {
                    el.setAttribute("schemaLocation", base + "?xsd=" + sl.replace(" ", "%20"));
                }
            }
            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://www.w3.org/2001/XMLSchema",
                                                              "redefine");
            for (Element el : elementList) {
                String sl = el.getAttribute("schemaLocation");
                if (smp.containsKey(URLDecoder.decode(sl, "utf-8"))) {
                    el.setAttribute("schemaLocation", base + "?xsd=" + sl.replace(" ", "%20"));
                }
            }
            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://schemas.xmlsoap.org/wsdl/",
                                                              "import");
            for (Element el : elementList) {
                String sl = el.getAttribute("location");
                if (mp.containsKey(URLDecoder.decode(sl, "utf-8"))) {
                    el.setAttribute("location", base + "?wsdl=" + sl.replace(" ", "%20"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                    LOG,
                    base), e);
        }

        boolean rewriteAllSoapAddress = MessageUtils.isTrue(message.getContextualProperty(AUTO_REWRITE_ADDRESS_ALL));
        if (rewriteAllSoapAddress) {
            List<Element> portList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                                         "http://schemas.xmlsoap.org/wsdl/",
                                                                         "port");
            String basePath = (String) message.get("http.base.path");
            for (Element el : portList) {
                rewriteAddressProtocolHostPort(base, el, basePath, "http://schemas.xmlsoap.org/wsdl/soap/");
                rewriteAddressProtocolHostPort(base, el, basePath, "http://schemas.xmlsoap.org/wsdl/soap12/");
            }
        }
        Object rewriteSoapAddress = message.getContextualProperty(AUTO_REWRITE_ADDRESS);
        if (rewriteSoapAddress == null || MessageUtils.isTrue(rewriteSoapAddress) || rewriteAllSoapAddress) {
            List<Element> serviceList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://schemas.xmlsoap.org/wsdl/",
                                                              "service");
            for (Element serviceEl : serviceList) {
                String serviceName = serviceEl.getAttribute("name");
                if (serviceName.equals(message.getExchange().getService().getName().getLocalPart())) {
                    elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                                      "http://schemas.xmlsoap.org/wsdl/",
                                                                      "port");
                    for (Element el : elementList) {
                        String name = el.getAttribute("name");
                        if (name.equals(message.getExchange().getEndpoint().getEndpointInfo()
                                            .getName().getLocalPart())) {
                            rewriteAddress(base, el, "http://schemas.xmlsoap.org/wsdl/soap/");
                            rewriteAddress(base, el, "http://schemas.xmlsoap.org/wsdl/soap12/");
                        }
                    }
                }
            }
        }
        try {
            doc.setXmlStandalone(true);
        } catch (Exception ex) {
            //likely not DOM level 3
        }
    }

    protected void rewriteAddress(String base, Element el, String soapNS) {
        List<Element> sadEls = DOMUtils.findAllElementsByTagNameNS(el,
                                             soapNS,
                                             "address");
        for (Element soapAddress : sadEls) {
            soapAddress.setAttribute("location", base);
        }
    }

    protected void rewriteAddressProtocolHostPort(String base, Element el, String httpBasePathProp, String soapNS) {
        List<Element> sadEls = DOMUtils.findAllElementsByTagNameNS(el,
                                             soapNS,
                                             "address");
        for (Element soapAddress : sadEls) {
            String location = soapAddress.getAttribute("location").trim();
            try {
                URI locUri = new URI(location);
                if (locUri.isAbsolute()) {
                    URL baseUrl = new URL(base);
                    StringBuilder sb = new StringBuilder(baseUrl.getProtocol());
                    sb.append("://").append(baseUrl.getHost()).append(":").append(baseUrl.getPort())
                        .append(locUri.getPath());
                    soapAddress.setAttribute("location", sb.toString());
                } else if (httpBasePathProp != null) {
                    soapAddress.setAttribute("location", httpBasePathProp + location);
                }
            } catch (Exception e) {
                //ignore
            }
        }
    }

    protected String resolveWithCatalogs(OASISCatalogManager catalogs, String start, String base) {
        try {
            return new OASISCatalogManagerHelper().resolve(catalogs, start, base);
        } catch (Exception ex) {
            //ignore
        }
        return null;
    }
    
    protected void updateDefinition(Bus bus,
                                 Definition def, Map<String, Definition> done,
                                 Map<String, SchemaReference> doneSchemas,
                                 String base, EndpointInfo ei) {
        OASISCatalogManager catalogs = OASISCatalogManager.getCatalogManager(bus);    
        
        Collection<List<?>> imports = CastUtils.cast((Collection<?>)def.getImports().values());
        for (List<?> lst : imports) {
            List<Import> impLst = CastUtils.cast(lst);
            for (Import imp : impLst) {
                
                String start = imp.getLocationURI();
                String decodedStart = null;
                // Always use the URL decoded version to ensure that we have a
                // canonical representation of the import URL for lookup. 
                try {
                    decodedStart = URLDecoder.decode(start, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    throw new WSDLQueryException(
                            new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                            LOG,
                            start), e);
                }
                
                String resolvedSchemaLocation = resolveWithCatalogs(catalogs, start, base);
                
                if (resolvedSchemaLocation == null) {
                    try {
                        //check to see if it's already in a URL format.  If so, leave it.
                        new URL(start);
                    } catch (MalformedURLException e) {
                        if (done.put(decodedStart, imp.getDefinition()) == null) {
                            updateDefinition(bus, imp.getDefinition(), done, doneSchemas, base, ei);
                        }
                    }
                } else {
                    if (done.put(decodedStart, imp.getDefinition()) == null) {
                        done.put(resolvedSchemaLocation, imp.getDefinition());
                        updateDefinition(bus, imp.getDefinition(), done, doneSchemas, base, ei);
                    }
                }
            }
        }      
        
        
        /* This doesn't actually work.   Setting setSchemaLocationURI on the import
        * for some reason doesn't actually result in the new URI being written
        * */
        Types types = def.getTypes();
        if (types != null) {
            for (ExtensibilityElement el 
                : CastUtils.cast(types.getExtensibilityElements(), ExtensibilityElement.class)) {
                if (el instanceof Schema) {
                    Schema see = (Schema)el;
                    updateSchemaImports(bus, see, doneSchemas, base);
                }
            }
        }
    }    

    public void updateWSDLPublishedEndpointAddress(Definition def, EndpointInfo endpointInfo) {
        synchronized (def) {
            //writing a def is not threadsafe.  Sync on it to make sure
            //we don't get any ConcurrentModificationExceptions
            if (endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL) != null) {
                String epurl = String.valueOf(endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL));
                updatePublishedEndpointUrl(epurl, def, endpointInfo.getName());
            }
        }
    }
    
    protected void updatePublishedEndpointUrl(String publishingUrl, Definition def, QName name) {
        Collection<Service> services = CastUtils.cast(def.getAllServices().values());
        for (Service service : services) {
            Collection<Port> ports = CastUtils.cast(service.getPorts().values());
            if (ports.isEmpty()) {
                continue;
            }
            
            if (name == null) {
                setSoapAddressLocationOn(ports.iterator().next(), publishingUrl);
                break; // only update the first port since we don't target any specific port
            } else {
                for (Port port : ports) {
                    if (name.getLocalPart().equals(port.getName())) {
                        setSoapAddressLocationOn(port, publishingUrl);
                    }
                }
            }
        }
    }
    
    protected void setSoapAddressLocationOn(Port port, String url) {
        List<?> extensions = port.getExtensibilityElements();
        for (Object extension : extensions) {
            if (extension instanceof SOAP12Address) {
                ((SOAP12Address)extension).setLocationURI(url);
            } else if (extension instanceof SOAPAddress) {
                ((SOAPAddress)extension).setLocationURI(url);
            }
        }
    }
    
    protected void updateSchemaImports(Bus bus, 
                                       Schema schema,
                                       Map<String, SchemaReference> doneSchemas,
                                       String base) {
        OASISCatalogManager catalogs = OASISCatalogManager.getCatalogManager(bus);    
        Collection<List<?>>  imports = CastUtils.cast((Collection<?>)schema.getImports().values());
        for (List<?> lst : imports) {
            List<SchemaImport> impLst = CastUtils.cast(lst);
            for (SchemaImport imp : impLst) {
                String start = findSchemaLocation(doneSchemas, imp);
                
                if (start != null) {
                    String decodedStart = null;
                    // Always use the URL decoded version to ensure that we have a
                    // canonical representation of the import URL for lookup. 
                    try {
                        decodedStart = URLDecoder.decode(start, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new WSDLQueryException(
                                new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                                LOG,
                                start), e);
                    }
                    
                    if (!doneSchemas.containsKey(decodedStart)) {
                        String resolvedSchemaLocation = resolveWithCatalogs(catalogs, start, base);
                        if (resolvedSchemaLocation == null) {
                            try {
                                //check to see if it's already in a URL format.  If so, leave it.
                                new URL(start);
                            } catch (MalformedURLException e) {
                                if (doneSchemas.put(decodedStart, imp) == null) {
                                    updateSchemaImports(bus, imp.getReferencedSchema(), doneSchemas, base);
                                }
                            }
                        } else {
                            if (doneSchemas.put(decodedStart, imp) == null) {
                                doneSchemas.put(resolvedSchemaLocation, imp);
                                updateSchemaImports(bus, imp.getReferencedSchema(), doneSchemas, base);
                            }
                        }
                    }
                }
            }
        }
        
        List<SchemaReference> includes = CastUtils.cast(schema.getIncludes());
        for (SchemaReference included : includes) {
            String start = findSchemaLocation(doneSchemas, included);

            if (start != null) {
                String decodedStart = null;
                // Always use the URL decoded version to ensure that we have a
                // canonical representation of the import URL for lookup. 
                try {
                    decodedStart = URLDecoder.decode(start, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    throw new WSDLQueryException(
                            new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                            LOG,
                            start), e);
                }
                
                String resolvedSchemaLocation = resolveWithCatalogs(catalogs, start, base);
                if (resolvedSchemaLocation == null) {
                    if (!doneSchemas.containsKey(decodedStart)) {
                        try {
                            //check to see if it's aleady in a URL format.  If so, leave it.
                            new URL(start);
                        } catch (MalformedURLException e) {
                            if (doneSchemas.put(decodedStart, included) == null) {
                                updateSchemaImports(bus, included.getReferencedSchema(), doneSchemas, base);
                            }
                        }
                    }
                } else if (!doneSchemas.containsKey(decodedStart) 
                    || !doneSchemas.containsKey(resolvedSchemaLocation)) {
                    doneSchemas.put(decodedStart, included);
                    doneSchemas.put(resolvedSchemaLocation, included);
                    updateSchemaImports(bus, included.getReferencedSchema(), doneSchemas, base);
                }
            }
        }
        List<SchemaReference> redefines = CastUtils.cast(schema.getRedefines());
        for (SchemaReference included : redefines) {
            String start = findSchemaLocation(doneSchemas, included);

            if (start != null) {
                String decodedStart = null;
                // Always use the URL decoded version to ensure that we have a
                // canonical representation of the import URL for lookup. 
                try {
                    decodedStart = URLDecoder.decode(start, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    throw new WSDLQueryException(
                            new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                            LOG,
                            start), e);
                }
                
                String resolvedSchemaLocation = resolveWithCatalogs(catalogs, start, base);
                if (resolvedSchemaLocation == null) {
                    if (!doneSchemas.containsKey(decodedStart)) {
                        try {
                            //check to see if it's aleady in a URL format.  If so, leave it.
                            new URL(start);
                        } catch (MalformedURLException e) {
                            if (doneSchemas.put(decodedStart, included) == null) {
                                updateSchemaImports(bus, included.getReferencedSchema(), doneSchemas, base);
                            }
                        }
                    }
                } else if (!doneSchemas.containsKey(decodedStart) 
                    || !doneSchemas.containsKey(resolvedSchemaLocation)) {
                    doneSchemas.put(decodedStart, included);
                    doneSchemas.put(resolvedSchemaLocation, included);
                    updateSchemaImports(bus, included.getReferencedSchema(), doneSchemas, base);
                }
            }
        }
    }

    private String findSchemaLocation(Map<String, SchemaReference> doneSchemas, SchemaReference imp) {
        if (imp.getReferencedSchema() != null) {
            for (Map.Entry<String, SchemaReference> e : doneSchemas.entrySet()) {
                if (e.getValue().getReferencedSchema().getElement() 
                    == imp.getReferencedSchema().getElement()) {
                    doneSchemas.put(imp.getSchemaLocationURI(), imp);
                    imp.setSchemaLocationURI(e.getKey());
                    return e.getKey();
                }
            }
        }
        return imp.getSchemaLocationURI();
    }

}

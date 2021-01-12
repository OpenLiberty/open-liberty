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
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.extensions.schema.SchemaReference;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.InputSource;

import org.apache.cxf.Bus;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.catalog.OASISCatalogManagerHelper;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.common.util.URIParserUtil;
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
    }


    public Set<String> getWSDLIds(Message message,
                            String base,
                            String ctxUri,
                            EndpointInfo endpointInfo) {
        Map<String, String> params = new HashMap<>();
        params.put("wsdl", "");
        getDocument(message, base,
                    params, ctxUri,
                    endpointInfo);

        Map<String, Definition> mp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
                                                    .getProperty(WSDLS_KEY));
        return mp.keySet();
    }
    public Map<String, String> getSchemaLocations(Message message,
                                                  String base,
                                                  String ctxUri,
                                                  EndpointInfo endpointInfo) {
        Map<String, String> params = new HashMap<>();
        params.put("wsdl", "");
        getDocument(message, base,
                    params, ctxUri,
                    endpointInfo);

        Map<String, SchemaReference> mp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
                                                         .getProperty(SCHEMAS_KEY));

        Map<String, String> schemas = new HashMap<>();
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
        Document doc = null;
        try {
            Bus bus = message.getExchange().getBus();
            base = getPublishedEndpointURL(message, base, endpointInfo);
            //making sure this are existing map objects for the endpoint.
            Map<String, Definition> mp = getWSDLKeyDefinition(endpointInfo);
            Map<String, SchemaReference> smp = getSchemaKeySchemaReference(endpointInfo);
            updateWSDLKeyDefinition(bus, mp, message, smp, base, endpointInfo);

            //
            if (params.containsKey("wsdl")) {
                String wsdl = URLDecoder.decode(params.get("wsdl"), "utf-8");
                doc = writeWSDLDocument(message, mp, smp, wsdl, base, endpointInfo);
            } else if (params.get("xsd") != null) {
                String xsd = URLDecoder.decode(params.get("xsd"), "utf-8");
                doc = readXSDDocument(bus, xsd, smp, base);
                updateDoc(doc, base, mp, smp, message); // Liberty change: xsd parameter is removed
            }
        } catch (WSDLQueryException wex) {
            throw wex;
        } catch (Exception wex) {
            LOG.log(Level.SEVERE, wex.getMessage(), wex);
            throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                                                     LOG,
                                                     base), wex);
        }
        return doc;
    }

    protected String mapUri(Bus bus, String base, Map<String, SchemaReference> smp,
                            String loc) // Liberty change: String xsd and String resolvedXsd parameters are removed
        throws UnsupportedEncodingException {
/*      Liberty change: 21 lines below are removed
        String key = loc;
        try {
            boolean absoluteLocUri = new URI(loc).isAbsolute();
            if (!absoluteLocUri && xsd != null) { // XSD request
                // resolve requested location with relative import path
                key = new URI(xsd).resolve(loc).toString();

                SchemaReference ref = smp.get(URLDecoder.decode(key, "utf-8"));
                if (ref == null) {
                    // if the result is not known, check if we can resolve it into something known
                    String resolved = resolveWithCatalogs(OASISCatalogManager.getCatalogManager(bus), key, base);
                    if (resolved != null  && smp.containsKey(URLDecoder.decode(resolved, "utf-8"))) {
                        // if it is resolvable, we can use it
                        return base + "?xsd=" + key.replace(" ", "%20");
                    }
                }
            } else if (!absoluteLocUri && xsd == null) { // WSDL request
                key = new URI(".").resolve(loc).toString();
            }
        } catch (URISyntaxException e) {
           //ignore
        } Liberty change: end */
        SchemaReference ref = smp.get(URLDecoder.decode(loc, "utf-8")); // Liberty change: key is replaced by loc
/*      Liberty change: 28 lines below are removed
        if (ref == null && resolvedXsd != null) {
            try {
                String key2 = new URI(resolvedXsd).resolve(loc).toString();
                SchemaReference ref2 = smp.get(URLDecoder.decode(key2, "utf-8"));
                if (ref2 == null) {
                    // if the result is not known, check if we can resolve it into something known
                    String resolved = resolveWithCatalogs(OASISCatalogManager.getCatalogManager(bus), key2, base);
                    if (resolved != null  && smp.containsKey(URLDecoder.decode(resolved, "utf-8"))) {
                        // if it is resolvable, we can use it
                        ref = smp.get(URLDecoder.decode(resolved, "utf-8"));
                    }
                } else {
                    ref = smp.get(URLDecoder.decode(key2, "utf-8"));
                }
            } catch (URISyntaxException e) {
                //ignore, ref can remain null
            }
            if (ref != null) {
                // we are able to map this, but for some reason the default key passed in cannot
                // be used for a direct lookup, we need to create a unique import key
                int count = 1;
                while (smp.containsKey("_import" + count + ".xsd")) {
                    count++;
                }
                key = "_import" + count + ".xsd";
                smp.put(key, ref);
            }
        } Liberty change: end */
        if (ref != null) {
            return base + "?xsd=" + ref.getSchemaLocationURI().replace(" ", "%20");  // Liberty change: key is replaced by ref.getSchemaLocationURI()
        }
        return null;
    }
/*
    @Deprecated
    protected void updateDoc(Document doc,
                             String base,
                             Map<String, Definition> mp,
                             Map<String, SchemaReference> smp,
                             Message message) { // Liberty change: String xsd and String wsdl parameters are removed
        updateDoc(doc, base, mp, smp, message); // Liberty change: xsd != null ? xsd : wsdl parameters are removed
    } */

    protected void updateDoc(Document doc,
                             String base,
                             Map<String, Definition> mp,
                             Map<String, SchemaReference> smp,
                             Message message) { // Liberty change: String xsdWsdlPar parameter is removed
        Bus bus = message.getExchange().getBus();
        List<Element> elementList = null;

        try {
            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://www.w3.org/2001/XMLSchema", "import");
            for (Element el : elementList) {
                String sl = el.getAttribute("schemaLocation");
                sl = mapUri(bus, base, smp, sl); // Liberty change: xsdWsdlPar and doc.getDocumentURI() parameters are removed
                 if (sl != null) {
                    el.setAttribute("schemaLocation", sl);
                }
            }

            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://www.w3.org/2001/XMLSchema",
                                                              "include");
            for (Element el : elementList) {
                String sl = el.getAttribute("schemaLocation");
/*              Liberty change: 5 lines below are removed
                sl = mapUri(bus, base, smp, sl, xsdWsdlPar, doc.getDocumentURI());
                if (sl != null) {
                    el.setAttribute("schemaLocation", sl);
                } Liberty change: end */
                // Liberty change: if block below is added
                if (smp.containsKey(URLDecoder.decode(sl, "utf-8"))) {
                    el.setAttribute("schemaLocation", base + "?xsd=" + sl.replace(" ", "%20"));
                } // Liberty change: end
            }
            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://www.w3.org/2001/XMLSchema",
                                                              "redefine");
            for (Element el : elementList) {
                String sl = el.getAttribute("schemaLocation");
/*              Liberty change: 5 lines below are removed
                sl = mapUri(bus, base, smp, sl, xsdWsdlPar, doc.getDocumentURI());
                if (sl != null) {
                    el.setAttribute("schemaLocation", sl);
                } Liberty change: end */
                // Liberty change: if block below is added
                if (smp.containsKey(URLDecoder.decode(sl, "utf-8"))) {
                    el.setAttribute("schemaLocation", base + "?xsd=" + sl.replace(" ", "%20"));
                } // Liberty change: end
            }
            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://schemas.xmlsoap.org/wsdl/",
                                                              "import");
            for (Element el : elementList) {
                String sl = el.getAttribute("location");
/*              Liberty change: 5 lines below are removed
                try {
                    sl = getLocationURI(sl, xsdWsdlPar);
                } catch (URISyntaxException e) {
                    //ignore
                }  Liberty change: end */
                if (mp.containsKey(URLDecoder.decode(sl, "utf-8"))) {
                    el.setAttribute("location", base + "?wsdl=" + sl.replace(" ", "%20"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                    LOG,
                    base), e);
        }

        boolean rewriteAllSoapAddress = MessageUtils.getContextualBoolean(message, AUTO_REWRITE_ADDRESS_ALL, false);
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
        if (MessageUtils.getContextualBoolean(message, AUTO_REWRITE_ADDRESS, true) || rewriteAllSoapAddress) {
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

    protected void rewriteAddress(String base,
                                  Element el,
                                  String soapNS) {
        List<Element> sadEls = DOMUtils.findAllElementsByTagNameNS(el, soapNS, "address");
        for (Element soapAddress : sadEls) {
            soapAddress.setAttribute("location", base);
        }
    }

    protected void rewriteAddressProtocolHostPort(String base,
                                                  Element el,
                                                  String httpBasePathProp,
                                                  String soapNS) {
        List<Element> sadEls = DOMUtils.findAllElementsByTagNameNS(el, soapNS, "address");
        for (Element soapAddress : sadEls) {
            String location = soapAddress.getAttribute("location").trim();
            try {
                URI locUri = new URI(location);
                if (locUri.isAbsolute()) {
                    URL baseUrl = new URL(base);
                    StringBuilder sb = new StringBuilder(baseUrl.getProtocol());
                    sb.append("://").append(baseUrl.getHost()).append(":").append(baseUrl.getPort()).append(locUri.getPath());
/*                  Liberty change: 6 lines below are removed
                    sb.append("://").append(baseUrl.getHost());
                    int port = baseUrl.getPort();
                    if (port > 0) {
                        sb.append(':').append(port);
                    }
                    sb.append(locUri.getPath()); Liberty change: end */
                    soapAddress.setAttribute("location", sb.toString());
                } else if (httpBasePathProp != null) {
                    soapAddress.setAttribute("location", httpBasePathProp + location);
                }
            } catch (Exception e) {
                //ignore
            }
        }
    }

    protected String resolveWithCatalogs(OASISCatalogManager catalogs,
                                         String start,
                                         String base) {
        try {
            return new OASISCatalogManagerHelper().resolve(catalogs, start, base);
        } catch (Exception ex) {
            //ignore
        }
        return null;
    }

    protected void updateDefinition(Bus bus,
                                    Definition def,
                                    Map<String, Definition> done,
                                    Map<String, SchemaReference> doneSchemas,
                                    String base) {  // String docBase and String parentResolvedLocation parameters are removed
        OASISCatalogManager catalogs = OASISCatalogManager.getCatalogManager(bus);

        Collection<List<?>> imports = CastUtils.cast((Collection<?>)def.getImports().values());
        for (List<?> lst : imports) {
            List<Import> impLst = CastUtils.cast(lst);
            for (Import imp : impLst) {
                String start = imp.getLocationURI();
                String decodedStart;
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
/*                      Liberty change: try block is removed
                        try {
                            start = getLocationURI(start, docBase);
                            decodedStart = URLDecoder.decode(start, "utf-8");
                        } catch (Exception e1) {
                            //ignore
                        } Liberty change: end */
                        if (done.put(decodedStart, imp.getDefinition()) == null) {
                            if (imp.getDefinition() != null && imp.getDefinition().getDocumentBaseURI() != null) {
                                done.put(imp.getDefinition().getDocumentBaseURI(), imp.getDefinition());
                            }
                            updateDefinition(bus, imp.getDefinition(), done, doneSchemas, base); // start and null parameters are removed as last parameters
                        }
                    }
                } else {
                    if (done.put(decodedStart, imp.getDefinition()) == null) {
                        done.put(resolvedSchemaLocation, imp.getDefinition());
                        if (imp.getDefinition() != null && imp.getDefinition().getDocumentBaseURI() != null) {
                            done.put(imp.getDefinition().getDocumentBaseURI(), imp.getDefinition());
                        }
                        updateDefinition(bus, imp.getDefinition(), done, doneSchemas, base); // start and resolvedSchemaLocation parameters are removed as last parameters
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
                    updateSchemaImports(bus, (Schema)el, doneSchemas, base); // Liberty change: docBase and parentResolvedLocation parameter is removed
                }
            }
        }
    }

    public void updateWSDLPublishedEndpointAddress(Definition def, EndpointInfo endpointInfo) {
        synchronized (def) {
            //writing a def is not threadsafe.  Sync on it to make sure
            //we don't get any ConcurrentModificationExceptions
            if (endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL) != null) {
                String epurl =
                    String.valueOf(endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL));
                updatePublishedEndpointUrl(epurl, def, endpointInfo.getName());
            }
        }
    }

    protected void updatePublishedEndpointUrl(String publishingUrl,
                                              Definition def,
                                              QName name) {
        Collection<Service> services = CastUtils.cast(def.getAllServices().values());
        for (Service service : services) {
            Collection<Port> ports = CastUtils.cast(service.getPorts().values());
            if (ports.isEmpty()) {
                continue;
            }

            if (name == null) {
                setSoapAddressLocationOn(ports.iterator().next(), publishingUrl);
                break; // only update the first port since we don't target any specific port
            }
            for (Port port : ports) {
                if (name.getLocalPart().equals(port.getName())) {
                    setSoapAddressLocationOn(port, publishingUrl);
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
                                       String base) { // Liberty change: String docBase and String parentResolved parameters are removed
        Collection<List<?>>  imports = CastUtils.cast((Collection<?>)schema.getImports().values());
        for (List<?> lst : imports) {
            List<SchemaImport> impLst = CastUtils.cast(lst);
            for (SchemaImport imp : impLst) {
                processSchemaReference(imp, bus, schema, doneSchemas, base); // Liberty change: String docBase and String parentResolved parameters are removed
            }
        }

        List<SchemaReference> includes = CastUtils.cast(schema.getIncludes());
        for (SchemaReference included : includes) {
            processSchemaReference(included, bus, schema, doneSchemas, base); // Liberty change: String docBase and String parentResolved parameters are removed
        }
        List<SchemaReference> redefines = CastUtils.cast(schema.getRedefines());
        for (SchemaReference included : redefines) {
            processSchemaReference(included, bus, schema, doneSchemas, base); // Liberty change: String docBase and String parentResolved parameters are removed
        }
    }

    private void processSchemaReference(SchemaReference schemaReference,
                                        Bus bus,
                                        Schema schema,
                                        Map<String, SchemaReference> doneSchemas,
                                        String base) {  // Liberty change: String docBase and String parentResolved parameters are removed
        OASISCatalogManager catalogs = OASISCatalogManager.getCatalogManager(bus);
        String start = findSchemaLocation(doneSchemas, schemaReference); // Liberty change: String docBase parameter is removed
        String origLocation = schemaReference.getSchemaLocationURI();

        if (start != null) {
            String decodedStart;
            String decodedOrigLocation;
            // Always use the URL decoded version to ensure that we have a
            // canonical representation of the import URL for lookup.
            try {
                decodedStart = URLDecoder.decode(start, "utf-8");
                decodedOrigLocation = URLDecoder.decode(origLocation, "utf-8");
            } catch (UnsupportedEncodingException e) {
                throw new WSDLQueryException(
                        new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                                LOG,
                                start), e);
            }

            if (!doneSchemas.containsKey(decodedStart)) {
                String resolvedSchemaLocation = resolveWithCatalogs(catalogs, start, base);
                if (resolvedSchemaLocation == null) {
                    resolvedSchemaLocation =
                            resolveWithCatalogs(catalogs, schemaReference.getSchemaLocationURI(), base);
                }
                if (resolvedSchemaLocation == null) {
                    try {
                        //check to see if it's already in a URL format.  If so, leave it.
                        new URL(start);
                    } catch (MalformedURLException e) {

                        doneSchemas.put(decodedStart, schemaReference);
                        doneSchemas.put(schemaReference.getReferencedSchema().getDocumentBaseURI(), schemaReference);
                        if (!doneSchemas.containsKey(decodedOrigLocation)) {
                            doneSchemas.put(decodedOrigLocation, schemaReference);
                        }
/*                      // Liberty change: try catch block is removed
                        try {
                            if (!(new URI(origLocation).isAbsolute()) && parentResolved != null) {
                                resolvedSchemaLocation = resolveRelativePath(parentResolved, decodedOrigLocation);
                                doneSchemas.put(resolvedSchemaLocation, schemaReference);
                            }
                        } catch (URISyntaxException e1) {
                            // ignore
                        } // Liberty change: end */
                        updateSchemaImports(bus, schemaReference.getReferencedSchema(), doneSchemas, base);  // Liberty change: start and resolvedSchemaLocation parameters are removed
                    }
                } else if (doneSchemas.put(decodedStart, schemaReference) == null) {
                    doneSchemas.put(resolvedSchemaLocation, schemaReference);
                    String p = getAndSaveRelativeSchemaLocationIfCatalogResolved(doneSchemas,
                            resolvedSchemaLocation,
                            schema,
                            schemaReference);
                    updateSchemaImports(bus, schemaReference.getReferencedSchema(), doneSchemas, base);  // Liberty change: p and resolvedSchemaLocation parameters are removed
                }
            }
        }
    }

    /**
     * When the imported schema location has been resolved through catalog, we need to:
     * 1) get a valid relative location to use for recursion into the imported schema
     * 2) add an entry to the doneSchemas map using such a valid relative location, as that's
     *    what will be used later for import links
     *
     * The valid relative location for the imported schema is computed by first obtaining the
     * relative uri that maps the importing schema resolved location into the imported schema
     * resolved location, then such value is resolved on top of the valid relative location
     * that's saved in the doneSchemas map for the importing schema.
     *
     * @param doneSchemas
     * @param resolvedSchemaLocation
     * @param currentSchema
     * @param schemaReference
     * @return
     */
    private String getAndSaveRelativeSchemaLocationIfCatalogResolved(Map<String, SchemaReference> doneSchemas,
                                                                     String resolvedSchemaLocation,
                                                                     Schema currentSchema,
                                                                     SchemaReference schemaReference) {
        String path = null;
        for (Map.Entry<String, SchemaReference> entry : doneSchemas.entrySet()) {
            Schema rs = entry.getValue().getReferencedSchema();
            String k = entry.getKey();
            String rsURI = rs.getDocumentBaseURI();
            if (currentSchema.equals(rs) && !rsURI.equals(k)) {
                try {
                    String p = URIParserUtil.relativize(rsURI, resolvedSchemaLocation);
                    if (p != null) {
                        path = new URI(k).resolve(p).toString();
                        break;
                    }
                } catch (URISyntaxException e) {
                    // ignore
                }
            }
        }
        if (path != null) {
            doneSchemas.put(path, schemaReference);
        }
        return path;
    }

    private String findSchemaLocation(Map<String, SchemaReference> doneSchemas,
                                      SchemaReference imp) {// Liberty change: String docBase parameter is removed
/*      Liberty change: 8 lines below are removed
        String schemaLocationURI = imp.getSchemaLocationURI();
        if (docBase != null && schemaLocationURI != null) {
            try {
                schemaLocationURI = getLocationURI(schemaLocationURI, docBase);
            } catch (Exception e) {
                //ignore
            }
        }  Liberty change: end */

        if (imp.getReferencedSchema() != null) {
            for (Map.Entry<String, SchemaReference> e : doneSchemas.entrySet()) {
                // Liberty change: && schemaLocationURI.equals(e.getKey()) is removed from if clause below after the changes above
                if ((e.getValue().getReferencedSchema().getElement() == imp.getReferencedSchema().getElement())) {
                    doneSchemas.put(imp.getSchemaLocationURI(), imp);  // Liberty change: schemaLocationURI is replaced by imp.getSchemaLocationURI()
                    imp.setSchemaLocationURI(e.getKey());
                    return e.getKey();
                }
            }
        }
        return imp.getSchemaLocationURI();
    }

    private String resolveRelativePath(String parentUri, String relativePath) {
        // can not use `new URI(uri).resolve(path)`, because that doesn't work with "jar:file:x!y" kind of URIs
        String base = UrlUtils.getStem(parentUri);
        return base + '/' + relativePath;
    }

    /**
     * Write the contents of a wsdl Definition object to a file.
     *
     * @param message
     * @param mp  a map of known wsdl Definition objects
     * @param smp a map of known xsd SchemaReference objects
     * @param wsdl name of the wsdl file to write
     * @param base the request URL
     * @param endpointInfo information for a web service 'port' inside of a service
     * @return Document
     * @throws WSDLException
     */
    public Document writeWSDLDocument(Message message,
                                      Map<String, Definition> mp,
                                      Map<String, SchemaReference> smp,
                                      String wsdl,
                                      String base,
                                      EndpointInfo endpointInfo) throws WSDLException {

        Document doc;
        Bus bus = message.getExchange().getBus();
        Definition def = lookupDefinition(bus, mp, wsdl, base);
        String epurl = base;

        synchronized (def) {
            //writing a def is not threadsafe.  Sync on it to make sure
            //we don't get any ConcurrentModificationExceptions
            epurl = getPublishableEndpointUrl(def, epurl, endpointInfo);

            WSDLWriter wsdlWriter = bus.getExtension(WSDLManager.class)
                .getWSDLFactory().newWSDLWriter();
            def.setExtensionRegistry(bus.getExtension(WSDLManager.class).getExtensionRegistry());
            doc = wsdlWriter.getDocument(def);
        }

        updateDoc(doc, epurl, mp, smp, message);  // Liberty change: wsdl parameter is removed
        return doc;
    }

    /**
     * Retrieve the published endpoint url from the working information set.
     *
     * @param def a wsdl as class objects
     * @param epurl the request URL
     * @param endpointInfo information for a web service 'port' inside of a service
     * @return String
     */
    public String getPublishableEndpointUrl(Definition def,
                                            String epurl,
                                            EndpointInfo endpointInfo) {

        if (endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL) != null) {
            epurl = String.valueOf(
                endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL));
            updatePublishedEndpointUrl(epurl, def, endpointInfo.getName());
        }
        return epurl;
    }

    /**
     * Read the schema file and return as a Document object.
     *
     * @param bus CXF's hub for access to internal constructs
     * @param xsd name of xsd file to be read
     * @param smp a map of known xsd SchemaReference objects
     * @param base the request URL
     * @return Document
     * @throws XMLStreamException
     */
    protected Document readXSDDocument(Bus bus,
                                       String xsd,
                                       Map<String, SchemaReference> smp,
                                       String base) throws XMLStreamException {
        Document doc = null;
        SchemaReference si = lookupSchemaReference(bus, xsd, smp, base);

        String uri = si.getReferencedSchema().getDocumentBaseURI();
        uri = resolveWithCatalogs(OASISCatalogManager.getCatalogManager(bus),
            uri, si.getReferencedSchema().getDocumentBaseURI());
        if (uri == null) {
            uri = si.getReferencedSchema().getDocumentBaseURI();
        }
        ResourceManagerWSDLLocator rml = new ResourceManagerWSDLLocator(uri, bus);

        InputSource src = rml.getBaseInputSource();
        if (src.getByteStream() != null || src.getCharacterStream() != null) {
            doc = StaxUtils.read(src);
        } else { // last resort lets try for the referenced schema itself.
            // its not thread safe if we use the same document
            doc = StaxUtils.read(
                new DOMSource(si.getReferencedSchema().getElement().getOwnerDocument()));
        }

        return doc;
    }

    /**
     * Create a wsdl Definition object from the endpoint information and register
     * it in the local data structure for future reference.
     *
     * @param bus CXF's hub for access to internal constructs
     * @param mp  a map of known wsdl Definition objects
     * @param message
     * @param smp a map of known xsd SchemaReference objects
     * @param base the request URL
     * @param endpointInfo information for a web service 'port' inside of a service
     * @throws WSDLException
     */
    protected void updateWSDLKeyDefinition(Bus bus,
                                           Map<String, Definition> mp,
                                           Message message,
                                           Map<String, SchemaReference> smp,
                                           String base,
                                           EndpointInfo endpointInfo) throws WSDLException {
        if (!mp.containsKey("")) {
            ServiceWSDLBuilder builder =
                new ServiceWSDLBuilder(bus, endpointInfo.getService());

            builder.setUseSchemaImports(
                MessageUtils.getContextualBoolean(message, WSDL_CREATE_IMPORTS, false));

            // base file name is ignored if createSchemaImports == false!
            builder.setBaseFileName(endpointInfo.getService().getName().getLocalPart());

            Definition def = builder.build(new HashMap<String, SchemaInfo>());

            mp.put("", def);
            updateDefinition(bus, def, mp, smp, base); // Liberty change: last parameter , "" is removed
        }

    }

    /**
     * Retrieve the map of known xsd SchemaReference objects for this endpoint.
     *
     * @param endpointInfo information for a web service 'port' inside of a service
     * @return Map<String, SchemaReference>
     */
    protected Map<String, SchemaReference> getSchemaKeySchemaReference(EndpointInfo endpointInfo) {
        Map<String, SchemaReference> smp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
            .getProperty(SCHEMAS_KEY));
        if (smp == null) {
            endpointInfo.getService().setProperty(SCHEMAS_KEY,
                new ConcurrentHashMap<String, SchemaReference>(8, 0.75f, 4));
            smp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
                .getProperty(SCHEMAS_KEY));
        }
        return smp;
    }

    /**
     * Retrieve the map of known wsdl Definition objects for this endpoint.
     *
     * @param endpointInfo  information for a web service 'port' inside of a service
     * @return Map<String, Definition>
     */
    protected Map<String, Definition> getWSDLKeyDefinition(EndpointInfo endpointInfo) {
        Map<String, Definition> mp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
            .getProperty(WSDLS_KEY));
        if (mp == null) {
            endpointInfo.getService().setProperty(WSDLS_KEY,
                new ConcurrentHashMap<String, Definition>(8, 0.75f, 4));
            mp = CastUtils.cast((Map<?, ?>)endpointInfo.getService().getProperty(WSDLS_KEY));
        }
        return mp;
    }

    /**
     * Retrieve the published endpoint url from the working information set.
     *
     * @param message
     * @param base the request URL
     * @param endpointInfo information for a web service 'port' inside of a service
     * @return String or NULL if none found
     */
    protected String getPublishedEndpointURL(Message message,
                                             String base,
                                             EndpointInfo endpointInfo) {

        Object prop = message.getContextualProperty(PUBLISHED_ENDPOINT_URL);
        if (prop == null) {
            prop = endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL);
        }
        if (prop != null) {
            base = String.valueOf(prop);
        }
        return base;
    }

    /**
     * Look for the schema filename in existing data structures and in system catalog
     * and register it in the local data structure.
     *
     * @param bus CXF's hub for access to internal constructs
     * @param mp  local structure of found wsdl files.
     * @param wsdl name of wsdl file for lookup
     * @param base the request URL
     * @return Definition
     */
    private Definition lookupDefinition(Bus bus,
                                        Map<String, Definition> mp,
                                        String wsdl,
                                        String base) {
        Definition def = mp.get(wsdl);
        if (def == null) {
            String wsdl2 = resolveWithCatalogs(
                OASISCatalogManager.getCatalogManager(bus), wsdl, base);
            if (wsdl2 != null) {
                def = mp.get(wsdl2);
            }
        }

        if (def == null) {
            throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("WSDL_NOT_FOUND",
                LOG, wsdl), null);
        }
        return def;
    }

    /**
     * Look for the schema filename in existing data structures and in system catalog
     * and register it in the local data structure.
     *
     * @param bus CXF's hub for access to internal constructs
     * @param xsd name of xsd file for lookup
     * @param smp local structure of found xsd files.
     * @param base the request URL
     * @return SchemaReference
     */
    private SchemaReference lookupSchemaReference(Bus bus,
                                                  String xsd,
                                                  Map<String, SchemaReference> smp,
                                                  String base) {
        SchemaReference si = smp.get(xsd);
        if (si == null) {
            String xsd2 = resolveWithCatalogs(OASISCatalogManager.getCatalogManager(bus),
                xsd, base);
            if (xsd2 != null) {
                si = smp.get(xsd2);
            }
        }
        if (si == null) {
            throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("SCHEMA_NOT_FOUND",
                LOG, xsd), null);
        }
        return si;
    }

    /**
     * Utility that generates either a relative URI path if the start path
     * is not an absolute path.
     * @param startLoc   start path
     * @param docBase  path to be adjusted as required
     * @return String
     * @throws URISyntaxException
     */
/*  Liberty change: this method relocated
    private String getLocationURI(String startLoc, String docBase) throws URISyntaxException {

        if (!(new URI(startLoc).isAbsolute())) {
            if (StringUtils.isEmpty(docBase)) {
                startLoc = new URI(".").resolve(startLoc).toString();
            } else {
                startLoc = new URI(docBase).resolve(startLoc).toString();
            }
        }
        return startLoc;
    } // Liberty change: end */

    /**
     * Utility that generates a URL query.
     * @param base  the request URL
     * @param ctxUri  the path information
     * @param s  the query text
     * @return String
     */
    private String buildUrl(String base, String ctxUri, String s) {
        return base + ctxUri + "?" + s;
    }
}

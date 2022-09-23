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
package org.apache.cxf.transport.servlet.servicelist;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.commons_text.StringEscapeUtils;

public class FormattedServiceListWriter implements ServiceListWriter {
    private String styleSheetPath;
    private String title;
    private Map<String, String> atomMap;
    private boolean showForeignContexts;
    private Bus bus;

    public FormattedServiceListWriter(String styleSheetPath,
                                      String title,
                                      boolean showForeignContexts,
                                      Bus bus) {
        this.styleSheetPath = StringEscapeUtils.escapeHtml4(styleSheetPath); // Liberty Change
        this.title = title;
        this.showForeignContexts = showForeignContexts;
        this.bus = bus;
        if (this.bus != null) {
            this.atomMap =
                CastUtils.cast((Map<?, ?>)this.bus.getProperty("org.apache.cxf.extensions.logging.atom.pull"));
        }
    }

    public String getContentType() {
        return "text/html; charset=UTF-8";
    }

    public void writeServiceList(PrintWriter writer,
                                 String basePath,
                                 AbstractDestination[] soapDestinations,
                                 AbstractDestination[] restDestinations) throws IOException {
        writer.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" "
                     + "\"http://www.w3.org/TR/html4/loose.dtd\">");
        writer.write("<HTML><HEAD>");
        writer.write("<LINK type=\"text/css\" rel=\"stylesheet\" href=\"" + styleSheetPath + "\">");
        writer.write("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">");
        if (title != null) {
            writer.write("<title>" + title + "</title>");
        } else {
            writer.write("<title>CXF - Service list</title>");
        }
        writer.write("</head><body>");

        if (soapDestinations.length > 0 || restDestinations.length > 0) {
            if (soapDestinations.length > 0) {
                writeSOAPEndpoints(writer, basePath, soapDestinations);
            }
            if (restDestinations.length > 0) {
                writeRESTfulEndpoints(writer, basePath, restDestinations);
            }
        } else {
            writer.write("<span class=\"heading\">No services have been found.</span>");
        }

        writer.write("</body></html>");
    }

    private void writeSOAPEndpoints(PrintWriter writer,
                                    String basePath,
                                    AbstractDestination[] destinations)
        throws IOException {
        writer.write("<span class=\"heading\">Available SOAP services:</span><br/>");
        writer.write("<table " + (styleSheetPath.endsWith("stylesheet=1")
                            ? "cellpadding=\"1\" cellspacing=\"1\" border=\"1\" width=\"100%\"" : "") + ">");
        for (AbstractDestination sd : destinations) {
            writerSoapEndpoint(writer, basePath, sd);
        }
        writer.write("</table><br/><br/>");
    }

    private void writerSoapEndpoint(PrintWriter writer,
                                    String basePath,
                                    AbstractDestination sd) {
        String absoluteURL = getAbsoluteAddress(basePath, sd);
        if (absoluteURL == null) {
            return;
        }

        writer.write("<tr><td>");
        writer.write("<span class=\"porttypename\">"
                     + sd.getEndpointInfo().getInterface().getName().getLocalPart() + "</span>");
        writer.write("<ul>");
        for (OperationInfo oi : sd.getEndpointInfo().getInterface().getOperations()) {
            if (!Boolean.TRUE.equals(oi.getProperty("operation.is.synthetic"))) {
                writer.write("<li>" + oi.getName().getLocalPart() + "</li>");
            }
        }
        writer.write("</ul>");
        writer.write("</td><td>");


        writer.write("<span class=\"field\">Endpoint address:</span> " + "<span class=\"value\">"
                     + absoluteURL + "</span>");
        writer.write("<br/><span class=\"field\">WSDL :</span> " + "<a href=\"" + absoluteURL
                     + "?wsdl\">" + sd.getEndpointInfo().getService().getName() + "</a>");
        writer.write("<br/><span class=\"field\">Target namespace:</span> "
                     + "<span class=\"value\">"
                     + sd.getEndpointInfo().getService().getTargetNamespace() + "</span>");
        addAtomLinkIfNeeded(absoluteURL, atomMap, writer);
        writer.write("</td></tr>");
    }

    private String getAbsoluteAddress(String basePath, AbstractDestination d) {
        String endpointAddress = (String)d.getEndpointInfo().getProperty("publishedEndpointUrl");
        if (endpointAddress != null) {
            return endpointAddress;
        }
        endpointAddress = d.getEndpointInfo().getAddress();
        if (endpointAddress.startsWith("http://") || endpointAddress.startsWith("https://")) {
            if (endpointAddress.startsWith(basePath) || showForeignContexts) {
                return endpointAddress;
            } else {
                return null;
            }
        } else {
            String address = basePath;
            if (address.endsWith("/") && endpointAddress.startsWith("/")) {
                address = address.substring(0, address.length() - 1);
            }
            return address + endpointAddress;
        }
    }

    private void writeRESTfulEndpoints(PrintWriter writer,
                                       String basePath,
                                       AbstractDestination[] restfulDests)
        throws IOException {
        writer.write("<span class=\"heading\">Available RESTful services:</span><br/>");
        writer.write("<table " + (styleSheetPath.endsWith("stylesheet=1")
            ? "cellpadding=\"1\" cellspacing=\"1\" border=\"1\" width=\"100%\"" : "") + ">");
        for (AbstractDestination sd : restfulDests) {
            writeRESTfulEndpoint(writer, basePath, sd);
        }
        writer.write("</table>");
    }

    private void writeRESTfulEndpoint(PrintWriter writer,
                                      String basePath,
                                      AbstractDestination sd) {
        String absoluteURL = getAbsoluteAddress(basePath, sd);
        if (absoluteURL == null) {
            return;
        }

        writer.write("<tr><td>");
        writer.write("<span class=\"field\">Endpoint address:</span> " + "<span class=\"value\">"
                     + absoluteURL + "</span>");
        if (bus != null && PropertyUtils.isTrue(bus.getProperty("wadl.service.description.available"))) {
            writer.write("<br/><span class=\"field\">WADL :</span> " + "<a href=\"" + absoluteURL
                     + "?_wadl\">" + absoluteURL + "?_wadl" + "</a>");
        }
        if (bus != null && PropertyUtils.isTrue(bus.getProperty("swagger.service.description.available"))) {
            String swaggerPath = "swagger.json";
            if (PropertyUtils.isTrue(bus.getProperty("swagger.service.ui.available"))) {
                URI uri = URI.create(absoluteURL);
                String schemePath = uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
                String relPath = absoluteURL.substring(schemePath.length());
                if (!relPath.endsWith("/")) {
                    relPath += "/";
                }
                swaggerPath = "api-docs?url=" + relPath + swaggerPath;
            }
            if (!absoluteURL.endsWith("/")) {
                swaggerPath = "/" + swaggerPath;
            }
            writer.write("<br/><span class=\"field\">Swagger :</span> " + "<a href=\"" + absoluteURL
                     + swaggerPath + "\">" + absoluteURL + swaggerPath + "</a>");
        }
        addAtomLinkIfNeeded(absoluteURL, atomMap, writer);
        writer.write("</td></tr>");
    }

    private static void addAtomLinkIfNeeded(String address, Map<String, String> extMap, PrintWriter pw) {
        String atomAddress = getExtensionEndpointAddress(address, extMap);
        if (atomAddress != null) {
            pw.write("<br/><span class=\"field\">Atom Log Feed :</span> " + "<a href=\"" + atomAddress
                     + "\">" + atomAddress + "</a>");
        }
    }

    private static String getExtensionEndpointAddress(String endpointAddress, Map<String, String> extMap) {
        if (extMap != null) {
            for (Map.Entry<String, String> entry : extMap.entrySet()) {
                if (endpointAddress.endsWith(entry.getKey())) {
                    endpointAddress = endpointAddress.substring(0, endpointAddress.length()
                                                                   - entry.getKey().length());
                    endpointAddress += entry.getValue();
                    return endpointAddress;
                }
            }
        }
        return null;
    }

}

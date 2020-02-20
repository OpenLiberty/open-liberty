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

import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.apache.cxf.transport.commons_text.StringEscapeUtils;

public class UnformattedServiceListWriter implements ServiceListWriter {
    boolean renderWsdlList;

    public UnformattedServiceListWriter(boolean renderWsdlList) {
        this.renderWsdlList = renderWsdlList;
    }

    public String getContentType() {
        return "text/html; charset=UTF-8";
    }

    public void writeServiceList(PrintWriter writer,
                                 String baseAddress,
                                 AbstractDestination[] soapDestinations,
                                 AbstractDestination[] restDestinations) throws IOException {
        if (soapDestinations.length > 0 || restDestinations.length > 0) {
            writeUnformattedSOAPEndpoints(writer, baseAddress, soapDestinations);
            writeUnformattedRESTfulEndpoints(writer, baseAddress, restDestinations);
        } else {
            writer.write("No services have been found.");
        }
    }
    
    private void writeUnformattedSOAPEndpoints(PrintWriter writer,
                                               String baseAddress,
                                               AbstractDestination[] destinations) throws IOException {
        for (AbstractDestination sd : destinations) {
            String address = getAbsoluteAddress(baseAddress, sd);
            address = StringEscapeUtils.escapeHtml4(address);
            
            writer.write(address);

            if (renderWsdlList) {
                writer.write("?wsdl");
            }
            writer.write('\n');
        }
        writer.write('\n');
    }

    private void writeUnformattedRESTfulEndpoints(PrintWriter writer,
                                                  String baseAddress,
                                                  AbstractDestination[] destinations) throws IOException {
        for (AbstractDestination sd : destinations) {
            String address = getAbsoluteAddress(baseAddress, sd);
            address = StringEscapeUtils.escapeHtml4(address);
            writer.write(address + "?_wadl\n");
        }
    }

    private String getAbsoluteAddress(String basePath, AbstractDestination d) {
        String endpointAddress = (String)d.getEndpointInfo().getProperty("publishedEndpointUrl");
        if (endpointAddress != null) {
            return endpointAddress;
        }
        endpointAddress = d.getEndpointInfo().getAddress();
        if (d instanceof ServletDestination
            && (endpointAddress.startsWith("http://") || endpointAddress.startsWith("https://"))) {
            String path = ((ServletDestination)d).getPath();
            return basePath + path;
        } else if (basePath == null || endpointAddress.startsWith(basePath)) {
            return endpointAddress;
        } else {
            return basePath + endpointAddress;
        }
    }
}
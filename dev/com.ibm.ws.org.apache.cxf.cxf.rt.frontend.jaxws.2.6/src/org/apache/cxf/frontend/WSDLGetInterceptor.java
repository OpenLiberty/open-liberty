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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;


import org.apache.cxf.binding.soap.interceptor.EndpointSelectionInterceptor;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.Conduit;

/**
 * 
 */
public class WSDLGetInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final WSDLGetInterceptor INSTANCE = new WSDLGetInterceptor();
       
    private static final Logger LOG = LogUtils.getL7dLogger(WSDLGetInterceptor.class);
    
    public WSDLGetInterceptor() {
        super(Phase.READ);
        getAfter().add(EndpointSelectionInterceptor.class.getName());
    }
    
    public void doOutput(Message message, String base, Document doc, OutputStream out)
        throws WSDLQueryException {
        String enc = null;
        try {
            enc = doc.getXmlEncoding();
        } catch (Exception ex) {
            //ignore - not dom level 3
        }
        if (enc == null) {
            enc = "utf-8";
        }

        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(out,
                                                                 enc);
        try {
            StaxUtils.writeNode(doc, writer, true);
            writer.flush();
        } catch (XMLStreamException e) {
            throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                                                                                LOG,
                                                                                base), e);
        } finally {
            StaxUtils.close(writer);
        }
    }


    public void handleMessage(Message message) throws Fault {
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        String query = (String)message.get(Message.QUERY_STRING);
        if (!"GET".equals(method) || StringUtils.isEmpty(query)) {
            return;
        }
        String baseUri = (String)message.get(Message.REQUEST_URL);
        String ctx = (String)message.get(Message.PATH_INFO);
        
        
        //cannot have two wsdl's being written for the same endpoint at the same
        //time as the addresses may get mixed up
        synchronized (message.getExchange().getEndpoint()) {
            Map<String, String> map = UrlUtils.parseQueryString(query);
            if (isRecognizedQuery(map, baseUri, ctx, 
                                  message.getExchange().getEndpoint().getEndpointInfo())) {
                
                try {
                    Conduit c = message.getExchange().getDestination().getBackChannel(message, null, null);
                    Message mout = new MessageImpl();
                    mout.setExchange(message.getExchange());
                    message.getExchange().setOutMessage(mout);
                    mout.put(Message.CONTENT_TYPE, "text/xml");
                    c.prepare(mout);
                    OutputStream os = mout.getContent(OutputStream.class);
                    Document doc = getDocument(message,
                                  baseUri,
                                  map,
                                  ctx,
                                  message.getExchange().getEndpoint().getEndpointInfo());
                    String enc = null;
                    try {
                        enc = doc.getXmlEncoding();
                    } catch (Exception ex) {
                        //ignore - not dom level 3
                    }
                    if (enc == null) {
                        enc = "utf-8";
                    }

                    XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os,
                                                                             enc);
                    StaxUtils.writeNode(doc, writer, true);
                    message.getInterceptorChain().abort();
                    try {
                        writer.flush();
                        writer.close();
                        os.flush();
                        os.close();
                    } catch (IOException ex) {
                        LOG.log(Level.FINE, "Failure writing full wsdl to the stream", ex);
                        //we can ignore this.   Likely, whatever has requested the WSDL
                        //has closed the connection before reading the entire wsdl.  
                        //WSDL4J has a tendency to not read the closing tags and such
                        //and thus can sometimes hit this.   In anycase, it's 
                        //pretty much ignorable and nothing we can do about it (cannot
                        //send a fault or anything anyway
                    }
                } catch (IOException e) {
                    throw new Fault(e);
                } catch (XMLStreamException e) {
                    throw new Fault(e);
                } finally {
                    message.getExchange().setOutMessage(null);
                }
            }
        }
    }
    public Document getDocument(Message message,
                                String base,
                                Map<String, String> params,
                                String ctxUri,
                                EndpointInfo endpointInfo) {
        return new WSDLGetUtils().getDocument(message, base, params, ctxUri, endpointInfo);
    }
    public boolean isRecognizedQuery(Map<String, String> map,
                                     String baseUri,
                                     String ctx, 
                                     EndpointInfo endpointInfo) {
        if (map.containsKey("wsdl")
            || map.containsKey("xsd")) {
            return true;
        }
        return false;
    }

}

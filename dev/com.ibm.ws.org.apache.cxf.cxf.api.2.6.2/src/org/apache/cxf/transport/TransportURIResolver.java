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

package org.apache.cxf.transport;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.ConnectException;

import javax.xml.namespace.QName;

import org.xml.sax.InputSource;

import com.ibm.websphere.ras.annotation.Trivial;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.resource.ExtendedURIResolver;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.common.logging.LogUtils;


/**
 * 
 */
public class TransportURIResolver extends ExtendedURIResolver {
    private static final Set<String> DEFAULT_URI_RESOLVER_HANDLES = new HashSet<String>();
    private static final Logger LOG = LogUtils.getL7dLogger(TransportURIResolver.class);

    static {
        //bunch we really don't want to have the conduits checked for
        //as we know the conduits don't handle.  No point
        //wasting the time checking/loading conduits and such
        DEFAULT_URI_RESOLVER_HANDLES.add("file");
        DEFAULT_URI_RESOLVER_HANDLES.add("classpath");
        DEFAULT_URI_RESOLVER_HANDLES.add("wsjar");
        DEFAULT_URI_RESOLVER_HANDLES.add("jar");
        DEFAULT_URI_RESOLVER_HANDLES.add("zip");
    }
    protected Bus bus;
    
    @Trivial
    public TransportURIResolver(Bus b) {
        super();
        bus = b;
    }
    
    public InputSource resolve(String curUri, String baseUri) throws ConnectException, SocketTimeoutException {
        
        LOG.log(Level.FINE, "Enter resolve: curUri: " + curUri + " and baseUri: " + baseUri);
        // Spaces must be encoded or URI.resolve() will choke
        curUri = curUri.replace(" ", "%20");
        
        InputSource is = null;
        URI base;
        try {
            if (baseUri == null) {
                base = new URI(curUri);
            } else {
                base = new URI(baseUri);
                base = base.resolve(curUri);
            }
        } catch (URISyntaxException use) {
            // ignore
            LOG.log(Level.FINE, "Ignoring URISyntaxException: " + use);
            base = null;
        }
        try {
            if (base == null 
                || DEFAULT_URI_RESOLVER_HANDLES.contains(base.getScheme())) {
                is = super.resolve(curUri, baseUri);
            }
        } catch (Exception ex) {
            //nothing
            LOG.log(Level.FINE, "Ignoring Exception 1: " + ex);
        }
        if (is == null && base != null 
            && base.getScheme() != null
            && !DEFAULT_URI_RESOLVER_HANDLES.contains(base.getScheme())) {
            try {
                ConduitInitiatorManager mgr = bus.getExtension(ConduitInitiatorManager.class);
                ConduitInitiator ci = null;
                if ("http".equals(base.getScheme()) || "https".equals(base.getScheme())) {
                    //common case, don't "search"
                    ci = mgr.getConduitInitiator("http://cxf.apache.org/transports/http");
                } 
                if (ci == null) {
                    ci = mgr.getConduitInitiatorForUri(base.toString());
                }
                if (ci != null) {
                    EndpointInfo info = new EndpointInfo();
                    // set the endpointInfo name which could be used for configuration
                    info.setName(new QName("http://cxf.apache.org", "TransportURIResolver"));
                    info.setAddress(base.toString());
                    final Conduit c = ci.getConduit(info);
                    Message message = new MessageImpl();
                    Exchange exch = new ExchangeImpl();
                    message.setExchange(exch);
                    
                    message.put(Message.HTTP_REQUEST_METHOD, "GET");
                    c.setMessageObserver(new MessageObserver() {
                        public void onMessage(Message message) {
                            LoadingByteArrayOutputStream bout = new LoadingByteArrayOutputStream();
                            try {
                                IOUtils.copy(message.getContent(InputStream.class), bout);
                                message.getExchange().put(InputStream.class, bout.createInputStream());
                                c.close(message);
                            } catch (IOException e) {
                                LOG.log(Level.FINE, "Ignoring IOException: " + e);
                                //ignore
                            }
                        }
                    });
                    c.prepare(message);
                    c.close(message);
                    InputStream ins = exch.get(InputStream.class);
                    resourceOpened.addElement(ins);
                    InputSource src = new InputSource(ins);
                    String u = (String)message.get("http.retransmit.url");
                    if (u == null) {
                        u = base.toString();
                    }
                    src.setPublicId(u);
                    src.setSystemId(u);
                    lastestImportUri = u;
                    currentResolver.unresolve();
                    return src;
                }
            // Liberty Change:
            // Catch ConnectionException and SocketTimeoutException when thrown in ExtenedURIResolver
            } catch (ConnectException ce1) {
                LOG.log(Level.FINE, "Throwing ConnectException: " + ce1);
                throw ce1;
            } catch (SocketTimeoutException se1) {
                LOG.log(Level.FINE, "Throwing SocketTimeoutException: " + se1);
                throw se1;
            } catch (Exception e) {
                //ignore
                LOG.log(Level.FINE, "Ignoring Exception 2: " + e);
            }
        }
        if (is == null 
            && (base == null 
                || base.getScheme() == null 
                || !DEFAULT_URI_RESOLVER_HANDLES.contains(base.getScheme()))) {
            is = super.resolve(curUri, baseUri);            
        }
        return is;
    }
}

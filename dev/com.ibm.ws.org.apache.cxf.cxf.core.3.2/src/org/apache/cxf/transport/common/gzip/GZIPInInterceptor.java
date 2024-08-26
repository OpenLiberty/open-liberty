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
package org.apache.cxf.transport.common.gzip;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * CXF interceptor that uncompresses those incoming messages that have "gzip"
 * content-encoding. An instance of this class should be added as an in and
 * inFault interceptor on clients that need to talk to a service that returns
 * gzipped responses or on services that want to accept gzipped requests. For
 * clients, you probably also want to use
 * {@link org.apache.cxf.transports.http.configuration.HTTPClientPolicy#setAcceptEncoding}
 * to let the server know you can handle compressed responses. To compress
 * outgoing messages, see {@link GZIPOutInterceptor}. This class was originally
 * based on one of the CXF samples (configuration_interceptor).
 */
public class GZIPInInterceptor extends AbstractPhaseInterceptor<Message> {


    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(GZIPInInterceptor.class);
    private static final Logger LOG = LogUtils.getL7dLogger(GZIPInInterceptor.class);

    public GZIPInInterceptor() {
        super(Phase.RECEIVE);
        addBefore(AttachmentInInterceptor.class.getName());
    }

    public void handleMessage(Message message) {
        if (isGET(message)) {
            return;
        }
        // check for Content-Encoding header - we are only interested in
        // messages that say they are gzipped.
        Map<String, List<String>> protocolHeaders = CastUtils.cast((Map<?, ?>)message
            .get(Message.PROTOCOL_HEADERS));
        if (protocolHeaders != null) {
            List<String> contentEncoding = HttpHeaderHelper.getHeader(protocolHeaders,
                                                                      HttpHeaderHelper.CONTENT_ENCODING);
            if (contentEncoding == null) {
                contentEncoding = protocolHeaders.get(GZIPOutInterceptor.SOAP_JMS_CONTENTENCODING);
            }
            if (contentEncoding != null
                && (contentEncoding.contains("gzip") || contentEncoding.contains("x-gzip"))) {
                try {
                    LOG.fine("Uncompressing response");
                    InputStream is = message.getContent(InputStream.class);
                    if (is == null) {
                        return;
                    }

                    // wrap an unzipping stream around the original one
                    GZIPInputStream zipInput = new GZIPInputStream(is);
                    message.setContent(InputStream.class, zipInput);

                    // remove content encoding header as we've now dealt with it
                    for (String key : protocolHeaders.keySet()) {
                        if ("Content-Encoding".equalsIgnoreCase(key)) {
                            protocolHeaders.remove(key);
                            break;
                        }
                    }

                    if (isRequestor(message)) {
                        //record the fact that is worked so future requests will
                        //automatically be FI enabled
                        Endpoint ep = message.getExchange().getEndpoint();
                        ep.put(GZIPOutInterceptor.USE_GZIP_KEY, GZIPOutInterceptor.UseGzip.YES);
                    }
                } catch (IOException ex) {
                    throw new Fault(new org.apache.cxf.common.i18n.Message("COULD_NOT_UNZIP", BUNDLE), ex);
                }
            }
        }
    }

}

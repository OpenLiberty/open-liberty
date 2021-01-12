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

package org.apache.cxf.interceptor;


import java.io.InputStream;

import javax.xml.stream.XMLStreamReader;

import com.ibm.websphere.ras.annotation.Trivial;
import com.sun.xml.fastinfoset.stax.StAXDocumentParser;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 */
 @Trivial // Liberty change: line added
public class FIStaxInInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String FI_GET_SUPPORTED = "org.apache.cxf.fastinfoset.get.supported";

    public FIStaxInInterceptor() {
        this(Phase.POST_STREAM);
    }

    public FIStaxInInterceptor(String phase) {
        super(phase);
        addBefore(StaxInInterceptor.class.getName());
    }

    protected boolean isRequestor(Message message) {
        return Boolean.TRUE.equals(message.containsKey(Message.REQUESTOR_ROLE));
    }

    private StAXDocumentParser getParser(InputStream in) {
        StAXDocumentParser parser = new StAXDocumentParser(in);
        parser.setStringInterning(true);
        parser.setForceStreamClose(true);
        parser.setInputStream(in);
        return parser;
    }

    public void handleMessage(Message message) {
        if (message.getContent(XMLStreamReader.class) != null
            || !isHttpVerbSupported(message)) {
            return;
        }

        String ct = (String)message.get(Message.CONTENT_TYPE);
        if (ct != null && ct.indexOf("fastinfoset") != -1
            && message.getContent(InputStream.class) != null
            && message.getContent(XMLStreamReader.class) == null) {
            message.setContent(XMLStreamReader.class, getParser(message.getContent(InputStream.class)));
            //add the StaxInEndingInterceptor which will close the reader
            message.getInterceptorChain().add(StaxInEndingInterceptor.INSTANCE);

            ct = ct.replace("fastinfoset", "xml");
            if (ct.contains("application/xml")) {
                ct = ct.replace("application/xml", "text/xml");
            }
            message.put(Message.CONTENT_TYPE, ct);

            message.getExchange().put(FIStaxOutInterceptor.FI_ENABLED, Boolean.TRUE);
            if (isRequestor(message)) {
                //record the fact that is worked so future requests will
                //automatically be FI enabled
                Endpoint ep = message.getExchange().getEndpoint();
                ep.put(FIStaxOutInterceptor.FI_ENABLED, Boolean.TRUE);
            }
        }
    }

    protected boolean isHttpVerbSupported(Message message) {
        if (isGET(message)) {
            return isRequestor(message)
                && MessageUtils.getContextualBoolean(message, FI_GET_SUPPORTED, false);
        }
        return true;
    }
}

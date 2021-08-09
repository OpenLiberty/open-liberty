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

package com.ibm.ws.jaxws.globalhandler;

import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Service;

import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.handler.soap.SOAPMessageContextImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.W3CDOMStreamReader;

import com.ibm.wsdl.util.xml.DOMUtils;

public class GlobalhandlerSOAPMessageContextImpl extends SOAPMessageContextImpl {
    /**
     * @param m
     */
    public GlobalhandlerSOAPMessageContextImpl(Message m) {
        super(m);
    }

    @Override
    public void setMessage(SOAPMessage message) {
        if (getWrappedMessage().getContent(Object.class) instanceof SOAPMessage) {
            getWrappedMessage().setContent(Object.class, message);
        } else {
            getWrappedMessage().setContent(SOAPMessage.class, message);
        }

        Service.Mode mode = (Service.Mode) getWrappedMessage()
                        .getContextualProperty(Service.Mode.class.getName());
        if (message != null) {
            Source s;
            try {
                if (mode == Service.Mode.MESSAGE) {
                    s = new DOMSource(message.getSOAPPart());
                } else {
                    s = new DOMSource(SAAJUtils.getBody(message).getFirstChild());
                }
                W3CDOMStreamReader r = new W3CDOMStreamReader(DOMUtils.getFirstChildElement(SAAJUtils.getBody(message)));
                getWrappedMessage().setContent(XMLStreamReader.class, r);
            } catch (Exception e) {
                throw new Fault(e);
            }
            getWrappedMessage().setContent(Source.class, s);
        }

    }

}

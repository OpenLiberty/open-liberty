/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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

    GlobalhandlerSOAPMessageContextImpl(Message m) {
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

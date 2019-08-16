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
package com.ibm.ws.jaxws.cdi.handler;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.ibm.ws.jaxws.cdi.beans.Teacher;

public class SoapHandlerImpl implements SOAPHandler<SOAPMessageContext> {

    @Inject
    private Teacher teacher;

    @Override
    public boolean handleMessage(SOAPMessageContext messageContext) {
        Boolean outboundProperty = (Boolean) messageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (outboundProperty.booleanValue()) {

            System.out.println(teacher.talk() + " in handleMessage");
//			SOAPMessage msg = messageContext.getMessage();
//			try {
//				NodeList nodes = msg.getSOAPBody().getElementsByTagName("arg0");
//				String original = nodes.item(0).getTextContent();
//				nodes.item(0).setTextContent(original + ". Verified by " + teacher.talk() + " SoapHandler");
//			} catch (SOAPException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
        }

//		System.out.println("** Response: "
//				+ messageContext.getMessage().toString());
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {

    }

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }

}

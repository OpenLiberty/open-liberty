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

import javax.inject.Inject;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;

import com.ibm.ws.jaxws.cdi.beans.Teacher;

public class LogicalHandlerImpl implements LogicalHandler<LogicalMessageContext> {

    @Inject
    private Teacher teacher;

    @Override
    public boolean handleMessage(LogicalMessageContext messageContext) {
        Boolean outboundProperty = (Boolean) messageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (outboundProperty.booleanValue()) {
            System.out.println(teacher.talk() + " in LogicalHandlerImpl");

//			LogicalMessageContext msg = messageContext.getMessage();
//			try {
//				NodeList nodes = msg.getMessage().getPayload().getElementsByTagName("arg0");
//				String original = nodes.item(0).getTextContent();
//				nodes.item(0).setTextContent(original + ". Verified by " + teacher.talk() + " LogicalHandler");
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
    public boolean handleFault(LogicalMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
        // TODO Auto-generated method stub

    }

}

/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.jaxws.attachment.mtom;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class ServiceRequestHandler implements SOAPHandler<SOAPMessageContext> {

    Logger logger = Logger.getLogger(ServiceRequestHandler.class.getName());

    @Override
    public boolean handleMessage(SOAPMessageContext smc) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            smc.getMessage().writeTo(bos);
            smc.getMessage().saveChanges();
            writeMessage(smc, "Outgoing");
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        return null;
    }

    public void writeMessage(SOAPMessageContext smc,
                             String msgType) throws JAXBException, SOAPException, IOException, TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError {
        SOAPMessage soapMessage = smc.getMessage();
        // Do modification stuff
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        soapMessage.writeTo(baos);

        SOAPEnvelope se = smc.getMessage().getSOAPPart().getEnvelope();
        DOMSource source = new DOMSource(se);
        StringWriter stringResult = new StringWriter();
        TransformerFactory.newInstance().newTransformer().transform(source, new StreamResult(stringResult));

        smc.setMessage(soapMessage);
    }

    public String replaceString(String iString, String oldString, String newString) {
        StringBuffer strOutput = new StringBuffer(32768);
        int nPos = 0;
        while (true) {
            int nIndex = iString.indexOf(oldString, nPos);
            // if args[1] can no longer be found, then copy the rest of the input
            if (nIndex < 0) {
                strOutput.append(iString.substring(nPos));
                break;
            }
            // otherwise, replace it with args[2] and continue
            else {
                strOutput.append(iString.substring(nPos, nIndex));
                strOutput.append(newString);
                nPos = nIndex + oldString.length();
            }
        }
        return strOutput.toString();
    }

    public void writeToFile(byte[] byteData, String filepath, String filename) throws IOException {

        File file = new File(filepath + filename);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(byteData);
        fileOutputStream.close();
    }
}

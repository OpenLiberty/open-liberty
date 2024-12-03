/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/

package fats.cxf.jaxws22.saaj.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SAAJTestServlet")
public class SAAJTestServlet extends FATServlet {

    /**
     * Test Description:
     * Test to make sure that a SAAJ meta-factory successfully created
     * and we don't have duplicated namespace caused by a bug located in SAAJ
     * 
     * Expected Result:
     * SoapBody should be created and an echo response given as a reply.
     *
     */
    @Test
    public void testSAAJ() throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        // Set the SOAP version
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("s", "http://www.w3.org/2003/05/soap-envelope");
        envelope.addNamespaceDeclaration("t", "http://www.example.org/types");

        // Create a SOAP body
        SOAPBody soapBody = envelope.getBody();
        SOAPElement soapBodyElement = soapBody.addChildElement("echo", "t", "http://www.example.org/types");
        SOAPElement soapBodyText = soapBodyElement.addChildElement("text", "t", "http://www.example.org/types");
        soapBodyText.addTextNode("Hello, world!");

        SOAPFactory SOAP_FACTORY = null;
        // Check if SAAJ meta-factory is successfully created
        try {
            SOAP_FACTORY = SOAPFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
        } catch (SOAPException se) {
            assertFalse(se.getMessage(), se.getMessage().startsWith("Unable to create SAAJ meta-factory: Provider"));
            return;
        }
        SOAPElement getEchoElement = SOAP_FACTORY.createElement("getEcho", "ns0", "http://objects.get.echo.io");
        SOAPElement echoElement = SOAP_FACTORY.createElement("echo", null, "");
        getEchoElement.addChildElement(echoElement);
        soapBody.addChildElement(getEchoElement);

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        soapMessage.writeTo(byteOut);
        String soapMessageString = new String(byteOut.toByteArray());

        // Check if duplicated namespace SAAJ bug is recessed
        assertTrue("Duplicated namespace in SOAP Body!", validateMessageString(soapMessageString));

    }

    private boolean validateMessageString(String soapMessageString) {
        if (soapMessageString == null) {
            return false;
        }
        if (soapMessageString.isEmpty()) {
            return false;
        }
        if (!soapMessageString.contains("getEcho")) {
            return false;
        }
        int indexOf = soapMessageString.indexOf("objects.get.echo.io");
        if (indexOf == -1)      {
            return false;
        }
        indexOf = soapMessageString.indexOf("objects.get.echo.io", ++indexOf);
        if (indexOf == -1)      {
            return false;
        }
        return true;
    }
}

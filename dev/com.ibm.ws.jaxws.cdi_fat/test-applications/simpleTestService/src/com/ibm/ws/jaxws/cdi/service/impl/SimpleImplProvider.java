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
package com.ibm.ws.jaxws.cdi.service.impl;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Provider;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

import org.w3c.dom.NodeList;

import com.ibm.ws.jaxws.cdi.beans.Student;

@WebServiceProvider
@ServiceMode(value = javax.xml.ws.Service.Mode.MESSAGE)
public class SimpleImplProvider implements Provider<SOAPMessage> {

    @Inject
    Student student;

//    @Inject
//    public void setStudent(Student st) {
//        student = st;
//    }

    @PostConstruct
    public void postCostructor() {
        System.out.println("SimpleImplProvider service's post constructor called");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("SimpleImplProvider service's pre destroy called");
    }

    @Override
    public SOAPMessage invoke(SOAPMessage request) {
//        student.talk();
        try {
//        	request.writeTo(System.out);
            NodeList nodeList = request.getSOAPBody().getElementsByTagName("arg0");
            String requestEchoValue = nodeList.item(0).getTextContent();
            MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
            SOAPMessage soapMessage = messageFactory.createMessage();

            soapMessage.getSOAPBody().addChildElement("invoke").setTextContent(requestEchoValue + "," + student.talk());
            soapMessage.writeTo(System.out);
            return soapMessage;
        } catch (SOAPException e) {
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
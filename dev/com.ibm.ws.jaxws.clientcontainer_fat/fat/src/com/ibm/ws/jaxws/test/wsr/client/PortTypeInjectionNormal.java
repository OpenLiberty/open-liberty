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
package com.ibm.ws.jaxws.test.wsr.client;

import java.io.StringReader;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.SOAPBinding;

import com.ibm.ws.jaxws.test.wsr.server.stub.People;
import com.ibm.ws.jaxws.test.wsr.server.stub.PeopleService;

//This test covered the two kinds of unmanaged jaxws clients as well.
public class PortTypeInjectionNormal {

    // test normal port type injection
    @WebServiceRef(name = "services/portTypeInjectionNormal", value = PeopleService.class)
    static People bill;

    public static void main(String[] args) {

        try {

            //workaround for the hard-coded server addr and port in wsdl
            TestUtils.setEndpointAddressProperty((BindingProvider) bill, args[0], Integer.parseInt(args[1]));
            System.out.println(bill.hello("Response from PortTypeInjectionNormal"));
        } catch (Throwable t) {
            System.out.println("throw able: " + t.getMessage());
        }

        //dynamic proxy clients--need stubs
        PeopleService people = new PeopleService();
        People peoplePort = people.getBillPort();
        Map<String, Object> requestContext = ((BindingProvider) peoplePort).getRequestContext();
        String urlString = "http://" + args[0] + ":" + args[1] + "/helloServer/PeopleService?wsdl";
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, urlString);
        String result = peoplePort.hello("received result from unmanged dynamic proxy client");
        System.out.println(result);

        //dispatch client --no need stubs
        QName qs = new QName("http://server.wsr.test.jaxws.ws.ibm.com", "PeopleService");
        QName qp = new QName("http://server.wsfvt.faultbean/", "Hello");

        try {
            // this wsdl has to be for real.
            URL u = new URL(urlString);
            u = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // invoke the basic Service creator directly, don't use anything generated.
        Service service = Service.create(qs);
        service.addPort(qp, SOAPBinding.SOAP11HTTP_BINDING, urlString);

        // now create a dispatch object from it
        Dispatch<SOAPMessage> dispatch = service.createDispatch(qp, SOAPMessage.class, Service.Mode.MESSAGE);

        // now it would be too easy to just send the soapmsgrequest string down the wire.
        // We have to make a soapmessage out of it using SAAJ's rather convoluted approach here..
        String soapMsgReqString = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://server.wsr.test.jaxws.ws.ibm.com\"> <soapenv:Header/> <soapenv:Body> <ser:hello> <arg0>Hello</arg0> </ser:hello></soapenv:Body></soapenv:Envelope>";
        SOAPMessage soapMsg = null;
        try {
            MessageFactory mf = MessageFactory.newInstance();
            soapMsg = mf.createMessage();
            soapMsg.getSOAPPart().setContent(new StreamSource(new StringReader(soapMsgReqString)));
            soapMsg.saveChanges();
            // wasn't that simplifying...
        } catch (SOAPException e) {
            e.printStackTrace();
        }

        try {

            if (dispatch == null || soapMsg == null) {
                throw new RuntimeException("dispatch or soapMsg is null!");
            }

            SOAPMessage response = dispatch.invoke(soapMsg);

            // imho, SOAPMessage = too cute for real world OOP
            String responseStr = response.getSOAPPart().getEnvelope().getBody().getTextContent().toString();
            System.out.println("received body content from unmanged dispath client:" + responseStr);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

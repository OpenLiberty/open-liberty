/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.special.characters.servlet;

import java.net.URL;
import java.io.StringReader;

import javax.servlet.annotation.WebServlet;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.SOAPBinding;

import org.junit.Test;

import com.ibm.ws.jaxws.special.characters.____.__or.WebServiceWithSpecialCharacters;
import com.ibm.ws.jaxws.special.characters.____.__or.WebServiceWithSpecialCharactersPortType;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SpecialCharacterTestServlet")
public class SpecialCharacterTestServlet extends FATServlet {
    
    @WebServiceRef(name="service/WebServiceWithSpecialCharacters")
    WebServiceWithSpecialCharacters wSWSC;
    
    private static final String ENDPOINT_ADDRESS = new StringBuilder().append("http://localhost:").append(Integer.getInteger("bvt.prop.HTTP_default")).append("/specialcharacters/WebServiceWithSpecialCharacters").toString();
                    
     /*
     *  Not having IllegalArgumentException is enough for this test to pass 
     */
    @Test
    public void SpecialCharacterSkipTest() throws Exception {
        WebServiceWithSpecialCharacters service = new WebServiceWithSpecialCharacters();
        WebServiceWithSpecialCharactersPortType port = service.getPort(WebServiceWithSpecialCharactersPortType.class);
        port.sc("Test");
    }
    
    /*
     *  Not having IllegalArgumentException is enough for this test to pass 
     */
    @Test
    public void SpecialCharacterSkipTestDispatch() throws Exception {
        URL WSDL_URL = new URL(ENDPOINT_ADDRESS + "?wsdl");

        //dispatch client --no need stubs
                              
        QName qs = new QName("http://characters.special.jaxws.ws.ibm.com/:.!?$()or.=*#,", "WebServiceWithSpecialCharacters");
        QName qp = new QName("http://characters.special.jaxws.ws.ibm.com/:.!?$()or.=*#,", "WebServiceWithSpecialCharactersPortType");

        Service service = Service.create(qs);
        service.addPort(qp, SOAPBinding.SOAP11HTTP_BINDING, WSDL_URL.toString());

       
        Dispatch<StreamSource> dispatch = service.createDispatch(qp, StreamSource.class, Service.Mode.MESSAGE);

        String msgString = "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                           + "  <Body>\n"
                           + "    <SC xmlns=\"http://characters.special.jaxws.ws.ibm.com/:.!?$()or.=*#,\">\n"
                           + "      <input1>Test</input1>\n"
                           + "    </SC>\n"
                           + "  </Body>\n"
                           + "</Envelope>";

        if (dispatch == null) {
            throw new RuntimeException("dispatch  is null!");
        }

        dispatch.invoke(new StreamSource(new StringReader(msgString)));
    }
    
    /*
     *  Not having IllegalArgumentException is enough for this test to pass 
     */
    @Test
    public void SpecialCharacterSkipTestWebServiceRef() throws Exception {
        WebServiceWithSpecialCharactersPortType port = wSWSC.getPort(WebServiceWithSpecialCharactersPortType.class);
        ((BindingProvider) port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ENDPOINT_ADDRESS);
        port.sc("Test");
    }
}

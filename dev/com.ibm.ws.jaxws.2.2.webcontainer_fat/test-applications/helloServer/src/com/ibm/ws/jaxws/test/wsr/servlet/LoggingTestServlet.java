/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.test.wsr.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.SOAPBinding;

import com.ibm.ws.jaxws.test.wsr.server.stub.People;
import com.ibm.ws.jaxws.test.wsr.server.stub.PeopleService;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/LoggingTestServlet")
public class LoggingTestServlet extends HttpServlet {

    private static Logger log = Logger.getLogger(LoggingTestServlet.class.getName());

    @WebServiceRef(name = "services/PeopleService", wsdlLocation = "http://localhost:8010/helloApp/PeopleService?wsdl")
    PeopleService serviceWithREF;

    public LoggingTestServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String wsdlURLString = new StringBuilder("http://").append("localhost:").append(req.getServerPort()).append("/helloApp/PeopleService?wsdl").toString();

        String wsType = req.getParameter("WSType"); // Web service type

        String result = null;
        switch (wsType) {
            case "proxy":
                PeopleService service = new PeopleService(new URL(wsdlURLString));
                People bill = service.getBillPort();
                result = bill.hello("World");
                break;
            case "dispatch":
                //dispatch client --no need stubs
                QName qs = new QName("http://server.wsr.test.jaxws.ws.ibm.com", "PeopleService");
                QName qp = new QName("http://server.wsr.test.jaxws.ws.ibm.com", "BillPort");

                // invoke the basic Service creator directly, don't use anything generated.
                Service dispatchService = Service.create(qs);
                dispatchService.addPort(qp, SOAPBinding.SOAP11HTTP_BINDING, wsdlURLString);

                // now create a dispatch object from it
                Dispatch<Source> dispatch = dispatchService.createDispatch(qp, Source.class, Service.Mode.PAYLOAD);
                String msgString = "<ser:hello xmlns:ser=\"http://server.wsr.test.jaxws.ws.ibm.com\"> <arg0>from dispatch World</arg0> </ser:hello>";

                if (dispatch == null) {
                    throw new RuntimeException("dispatch  is null!");
                }

                Source invoke = dispatch.invoke(new StreamSource(new StringReader(msgString)));
                result = "Dispatch invoke success: " + (invoke != null);
                break;
            case "wsref":
                People billRef = serviceWithREF.getBillPort();
                result = billRef.hello("World");
                break;
        }
        PrintWriter pw = resp.getWriter();
        pw.write(result);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}

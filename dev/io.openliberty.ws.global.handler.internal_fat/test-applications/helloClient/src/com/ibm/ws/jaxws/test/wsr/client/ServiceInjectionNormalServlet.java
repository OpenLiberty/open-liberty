/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.jaxws.test.wsr.client;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.test.wsr.server.stub.People;
import com.ibm.ws.jaxws.test.wsr.server.stub.PeopleService;
import com.ibm.ws.webservices.handler.fat.TestUtils;

@WebServlet("/ServiceInjectionNormalServlet")
public class ServiceInjectionNormalServlet extends HttpServlet {
    private static final long serialVersionUID = -1L;

    // test normal service injection
    @WebServiceRef
    PeopleService service;

    public ServiceInjectionNormalServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        String target = request.getParameter("target");

        People bill = service.getBillPort();
        //workaround for the hard-coded server addr and port in wsdl
        TestUtils.setEndpointAddressProperty((BindingProvider) bill, request.getLocalAddr(), request.getLocalPort());
        out.println(bill.hello(target));

    }

}

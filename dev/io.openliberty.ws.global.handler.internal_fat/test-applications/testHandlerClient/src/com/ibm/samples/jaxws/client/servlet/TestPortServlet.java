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
package com.ibm.samples.jaxws.client.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jws.HandlerChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.samples.jaxws.client.SayHelloService;

/**
 *
 */
//@WebServlet("/TestPortServlet")
public class TestPortServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @WebServiceRef(name = "services/portServlet/HelloFromWSRef", value = com.ibm.samples.jaxws.client.ClientSayHelloService.class)
    @HandlerChain(file = "handler/handler-test-client.xml")
    private SayHelloService portFromRef;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setCharacterEncoding("utf-8");

        // re-configurate the ports
        reConfigPorts(req);

        PrintWriter out = null;
        String target = req.getParameter("target");

        try {
            out = resp.getWriter();

            out.println("The greeting from @WebServiceRef: " + portFromRef.sayHello(target));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    private void reConfigPorts(HttpServletRequest request) {
        String host = request.getLocalAddr();
        int port = request.getLocalPort();
        ((BindingProvider) portFromRef).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://" + host + ":" + port + "/testHandlerProvider/SayHelloService");

    }
}

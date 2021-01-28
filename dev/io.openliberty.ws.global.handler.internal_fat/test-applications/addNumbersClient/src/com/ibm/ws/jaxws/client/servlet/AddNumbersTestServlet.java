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
package com.ibm.ws.jaxws.client.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.client.AddNumbers;
import com.ibm.ws.jaxws.client.AddNumbers_Service;

/**
 *
 */
@WebServlet("/AddNumbersTestServlet")
public class AddNumbersTestServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @WebServiceRef
    private AddNumbers_Service serviceFromRef;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setCharacterEncoding("utf-8");

        AddNumbers addNumbers = serviceFromRef.getAddNumbersPort();

        reConfigPorts(req, (BindingProvider) addNumbers);

        PrintWriter out = null;
        int num1 = Integer.parseInt(req.getParameter("number1"));
        int num2 = Integer.parseInt(req.getParameter("number2"));

        try {
            out = resp.getWriter();
            out.println("Result = " + addNumbers.addNumbers(num1, num2));
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

    private void reConfigPorts(HttpServletRequest request, BindingProvider portFromRef) {
        String host = request.getLocalAddr();
        int port = request.getLocalPort();

        // Config portFromRef
        portFromRef.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://" + host + ":" + port + "/addNumbersProvider/AddNumbers");

    }

}

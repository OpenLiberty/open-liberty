/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.ejbsNoBeansXml;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet("/SimpleServlet")
public class SimpleServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @EJB(beanName = "SimpleEJB")
    private ManagedSimpleBean bean;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();

        bean.setValue("value");
        out.println(getFieldInjectionResult());
    }

    public String getFieldInjectionResult() {
        String result;
        String beanValue = bean.getValue();
        if (beanValue.equals("value")) {
            result = ("Test PASSED bean value is " + beanValue);
        } else {
            result = ("Test FAILED bean value is " + beanValue);
        }
        return result;
    }
}
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
@WebServlet("/ConstructorInjectionServlet")
public class ConstructorInjectionServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @EJB(beanName = "OtherSimpleEJB")
    private FirstManagedBeanInterface bean1;

    @EJB(beanName = "FinalEJB")
    private SecondManagedBeanInterface bean2;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();

        bean1.setValue1("value1");
        bean2.setValue2("value2");
        out.println(getConstructorResult());
    }

    public String getConstructorResult() {
        String result;
        String bean1value = bean1.getValue1();
        String bean2value = bean2.getValue2();
        if (bean1value.equals("value1") && bean2value.equals("value2")) {
            result = ("Test SUCCESSFUL bean values are " + bean1value + " and " + bean2value);
        } else {
            result = ("Test FAILED bean values are " + bean1value + " and " + bean2value);
        }
        return result;
    }
}
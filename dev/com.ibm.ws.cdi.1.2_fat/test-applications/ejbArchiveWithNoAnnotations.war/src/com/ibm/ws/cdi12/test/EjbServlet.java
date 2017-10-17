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
package com.ibm.ws.cdi12.test;

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
@WebServlet("/ejbServlet")
public class EjbServlet extends HttpServlet {

    @EJB(beanName = "EjbBean")
    SimpleEjbBean bean1;

    @EJB(beanName = "EjbBean2")
    SimpleEjbBean2 bean2;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();

        bean1.setMessage("Message1");
        bean2.setMessage("Message2");
        out.println(getMessage());
    }

    public String getMessage() {
        String message;
        String message1 = bean1.getMessage();
        String message2 = bean2.getMessage();
        if (message1.equals("Message1") && message2.equals("Message2")) {
            message = ("PASSED messages are " + message1 + " and " + message2);
        }
        else {
            message = ("FAILED messages are " + message1 + " and " + message2);
        }
        return message;
    }
}

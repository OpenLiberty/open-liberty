/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.ejbJarInWarNoAnnotations;

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
        bean2.setMessage2("Message2");
        out.println(getMessage());
    }

    public String getMessage() {
        String message;
        String message1 = bean1.getMessage();
        String message2 = bean2.getMessage2();
        if (message1.equals("Message1") && message2.equals("Message2")) {
            message = ("PASSED messages are " + message1 + " and " + message2);
        }
        else {
            message = ("FAILED messages are " + message1 + " and " + message2);
        }
        return message;
    }
}

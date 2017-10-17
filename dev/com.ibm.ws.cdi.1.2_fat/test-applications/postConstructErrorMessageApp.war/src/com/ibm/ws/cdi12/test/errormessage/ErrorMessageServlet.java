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
package com.ibm.ws.cdi12.test.errormessage;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/errorMessageTestServlet")
public class ErrorMessageServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    ErrorMessageTestEjb errEjb;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        errEjb.doSomething();
    }

}

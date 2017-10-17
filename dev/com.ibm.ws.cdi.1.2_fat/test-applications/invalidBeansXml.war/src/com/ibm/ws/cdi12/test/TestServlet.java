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

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet("/TestServlet")
public class TestServlet extends HttpServlet {

    private final String message = "Hello World!";

    @Inject
    TestBean bean;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        bean.setMessage(this.message);
        PrintWriter out = response.getWriter();
        out.println(getResponse());
    }

    private String getResponse() {
        if (bean.getMessage().equals(this.message)) {
            return "PASSED";
        }
        else {
            return ("FAILED message received was " + bean.getMessage());
        }
    }
}

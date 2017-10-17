/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.defaultdecorator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import javax.enterprise.context.Conversation;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/")
public class DefaultDecoratorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    Conversation c;

    private static List<String> output = new LinkedList<String>();

    public static void addOutput(String s) {
        output.add(s);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        c.isTransient();

        PrintWriter pw = response.getWriter();
        for (String s : output) {
            pw.write(s);
        }
        pw.flush();
        pw.close();

    }

}

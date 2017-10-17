package com.ibm.ws.cdi12.aftertypediscovery.test;

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

/**
 *
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/")
public class AfterTypeServlet extends HttpServlet {

    private static final long serialVersionUID = 8549700799591343964L;

    @Inject
    AfterTypeInterface b;

    @Inject
    InterceptedBean ib;

    @Inject
    AfterTypeAlternativeInterface altOne;

    @Inject
    @UseAlternative
    AfterTypeAlternativeInterface altTwo;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        ib.doNothing();

        PrintWriter pw = response.getWriter();

        pw.write(b.getMsg() + System.getProperty("line.separator"));

        for (String s : GlobalState.getOutput()) {
            pw.write(s + System.getProperty("line.separator"));
        }

        pw.write("expecting one: " + altOne.getMsg() + System.getProperty("line.separator"));
        pw.write("expecting two: " + altTwo.getMsg() + System.getProperty("line.separator"));

        pw.flush();
        pw.close();
    }
}

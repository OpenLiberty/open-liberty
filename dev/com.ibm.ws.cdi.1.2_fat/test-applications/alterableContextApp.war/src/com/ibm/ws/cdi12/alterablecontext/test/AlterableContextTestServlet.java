package com.ibm.ws.cdi12.alterablecontext.test;

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

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi12.alterablecontext.test.extension.DirtySingleton;

@WebServlet("/")
public class AlterableContextTestServlet extends HttpServlet {

    private static final long serialVersionUID = 8549700799591343964L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();

        for (String s : DirtySingleton.getStrings()) {
            pw.write(s + System.getProperty("line.separator"));
        }

        pw.flush();
        pw.close();
    }

}

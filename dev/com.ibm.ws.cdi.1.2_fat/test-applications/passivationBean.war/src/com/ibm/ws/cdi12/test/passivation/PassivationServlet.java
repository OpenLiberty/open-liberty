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
package com.ibm.ws.cdi12.test.passivation;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.inject.TransientReference;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/")
public class PassivationServlet extends HttpServlet {

    @Inject
    BeanHolder bh;

    int i = 0;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        bh.doNothing();

        PrintWriter pw = response.getWriter();

        for (String s : GlobalState.getOutput()) {
            pw.write(s);
        }

        pw.flush();
        pw.close();

    }

    @Inject
    public void transientVisit(@TransientReference TransiantDependentScopedBeanTwo bean) {
        bean.doNothing();
    }

}

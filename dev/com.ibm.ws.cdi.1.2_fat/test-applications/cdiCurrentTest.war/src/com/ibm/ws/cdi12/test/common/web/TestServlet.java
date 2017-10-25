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
package com.ibm.ws.cdi12.test.common.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi12.test.current.extension.MyDeploymentVerifier;

@WebServlet("/")
public class TestServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();
        for (String s : MyDeploymentVerifier.getMessages()) {
            pw.write(s);
            pw.write("\n");
        }

        SimpleBean sb = CDI.current().select(SimpleBean.class).get();

        pw.write(sb.test());

        pw.flush();
        pw.close();

    }

}

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
package com.ibm.ws.cdi12.test.warLibAccessBeansInWar;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar.TestInjectionClass;
import com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar2.TestInjectionClass2;

@WebServlet("/TestServlet")
public class TestServlet extends HttpServlet {

    @Inject
    TestInjectionClass injection;

    @Inject
    TestInjectionClass2 injection2;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter out = response.getWriter();
        out.println(getMessages());
    }

    public String getMessages() {

        String message1 = injection.getMessage();
        String message2 = injection2.getMessage();

        return (message1 + " " + message2);
    }
}

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

package cdi12.helloworld.jeeResources.test;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cdi12.helloworld.jeeResources.ejb.MySessionBean1;
import cdi12.helloworld.jeeResources.ejb.MySessionBean2;

@WebServlet("/")
public class JEEResourceTestServlet extends HttpServlet {

    @Inject
    HelloWorldExtensionBean2 hello;

    @EJB
    MySessionBean1 bean1;

    @EJB
    MySessionBean2 bean2;

    private static final long serialVersionUID = 8549700799591343964L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();
        pw.write(hello.hello());
        pw.write(bean1.hello());
        pw.write(bean2.hello());
        pw.flush();
        pw.close();
    }

}

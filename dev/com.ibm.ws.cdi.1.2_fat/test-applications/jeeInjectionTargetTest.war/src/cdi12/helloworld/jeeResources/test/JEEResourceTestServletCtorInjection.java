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

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/ctorInjection")
public class JEEResourceTestServletCtorInjection extends HttpServlet {

    private static final long serialVersionUID = 8549700799591343964L;

    private final HelloWorldExtensionBean2 hello;

    @Inject
    public JEEResourceTestServletCtorInjection(HelloWorldExtensionBean2 bean2) {
        super();
        hello = bean2;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();
        pw.write(hello.hello());
        pw.flush();
        pw.close();
    }

}

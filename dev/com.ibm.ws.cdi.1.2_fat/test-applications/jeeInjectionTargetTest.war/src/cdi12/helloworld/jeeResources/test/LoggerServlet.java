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
import java.util.Iterator;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/log")
public class LoggerServlet extends HttpServlet {

    @Inject
    private BeanManager beanManager;

    private static final long serialVersionUID = 8549700799591343964L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        JEEResourceExtension extension = beanManager.getExtension(JEEResourceExtension.class);

        PrintWriter pw = response.getWriter();
        Iterator<String> itr = extension.logger.iterator();
        while (itr.hasNext()) {
            pw.write(itr.next());
            if (itr.hasNext()) {
                pw.write(",");
            }
        }
        pw.flush();
        pw.close();
    }

}

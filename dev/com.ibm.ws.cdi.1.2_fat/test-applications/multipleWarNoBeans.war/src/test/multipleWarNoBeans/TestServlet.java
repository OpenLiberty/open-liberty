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
package test.multipleWarNoBeans;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.extension.impl.RTExtensionReqScopedBean;

/**
 *
 */
@WebServlet("/")
public class TestServlet extends HttpServlet {
    @Inject
    Instance<RTExtensionReqScopedBean> rtExtensionReqScopedBean;

    /**  */
    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        PrintWriter pw = response.getWriter();
        try {
            pw.println(rtExtensionReqScopedBean.isUnsatisfied() ? "null" : rtExtensionReqScopedBean.get().getName());
        } catch (ContextNotActiveException e) {
            pw.println("ContextNotActiveException ... as expected");
        }

    }
}

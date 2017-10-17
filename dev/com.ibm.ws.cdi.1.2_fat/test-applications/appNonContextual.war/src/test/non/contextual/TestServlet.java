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
package test.non.contextual;

import java.io.IOException;

import javax.enterprise.inject.spi.Unmanaged;
import javax.enterprise.inject.spi.Unmanaged.UnmanagedInstance;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/test")
public class TestServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        Unmanaged<NonContextualBean> unmanaged = new Unmanaged<NonContextualBean>(NonContextualBean.class);
        UnmanagedInstance<NonContextualBean> instance = unmanaged.newInstance();
        NonContextualBean nonContextualBean = instance.produce().inject().postConstruct().get();

        try {
            nonContextualBean.testNonContextualEjbInjectionPointGetBean();
            nonContextualBean.testContextualEjbInjectionPointGetBean();

            resp.getWriter().print("PASSED");
        } finally {
            instance.preDestroy().dispose();
        }
    }

}

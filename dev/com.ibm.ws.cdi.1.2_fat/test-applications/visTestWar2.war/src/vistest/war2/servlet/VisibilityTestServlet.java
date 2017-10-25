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
package vistest.war2.servlet;

import java.io.IOException;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import vistest.war2.War2TestingBean;

/**
 * Another test servlet which is only used to test visibility from war2 to everywhere else.
 * <p>
 * The majority of testing is done in {@link vistest.war.servlet.VisibilityTestServlet}
 */
@WebServlet("/")
public class VisibilityTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private Instance<War2TestingBean> war2TestingInstance;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String qualifier = req.getParameter("location");

        String result;
        try {
            if (qualifier == null) {
                result = "ERROR: No qualifier provided\n";
            }
            else if (qualifier.equals("InWar2")) {
                result = war2TestingInstance.get().doTest();
            }
            else {
                result = "ERROR: unrecognised qualifier\n";
            }
        } catch (UnsatisfiedResolutionException ex) {
            result = "ERROR: unable to resolve test class\n" + ex.toString() + "\n";
        }

        resp.getOutputStream().print(result);
    }
}

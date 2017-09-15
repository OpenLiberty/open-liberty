/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.user.prod.extensions;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * UserProductExtension1TestServlet.
 */
@WebServlet(urlPatterns = { "test" })
public class UserProductExtension1TestServlet extends HttpServlet {
    private static final long serialVersionUID = -4913608474531994493L;
    public static final String SAY_HELLO_INPUT = "HelloYou";
    public static final String EXPECTED_SAY_HELLO = "you.said." + SAY_HELLO_INPUT + ".i.say." + SAY_HELLO_INPUT + ".back";
    public static final Long EXPECTED_ATTRB1 = 999999L;
    public static final String EXPECTED_ATTRB2 = "HELLOWORLD";

    /**
     * doGet.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServiceReference<?> prod1SvcRef = null;
        BundleContext bundleContext = (BundleContext) req.getServletContext().getAttribute("osgi-bundlecontext");

        // Unable to get bundle context.
        if (bundleContext == null) {
            String reply = "TEST_FAILED: Unable to obtain a bundle context reference.";
            System.out.println(reply);
            resp.getWriter().print(reply);
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        try {
            prod1SvcRef = bundleContext.getServiceReference("test.user.prod.extensions.UserProductExtension1");
            if (prod1SvcRef == null) {
                printOutput(resp, "TEST_FAILED: Unable to get a UserProductExtension1 service reference.");
                return;
            }

            final Object prod1Svc = bundleContext.getService(prod1SvcRef);
            if (prod1Svc != null) {
                final Method sayHello_method = prod1Svc.getClass().getMethod("sayHello", String.class);
                final Method getAtrib1Method = prod1Svc.getClass().getMethod("getAttribute1");
                final Method getAtrib2Method = prod1Svc.getClass().getMethod("getAttribute2");

                if (sayHello_method == null || getAtrib1Method == null || getAtrib2Method == null) {
                    printOutput(resp, "TEST_FAILED: Unable get a method reference from the UserProductExtension1 service.");
                    return;
                }

                // Call sayHello on the service.
                String sayHelloOut = (String) sayHello_method.invoke(prod1Svc, SAY_HELLO_INPUT);
                if (!sayHelloOut.equals(EXPECTED_SAY_HELLO)) {
                    printOutput(resp, "TEST_FAILED: sayHello returned: " + sayHelloOut + ". Expected: " + EXPECTED_SAY_HELLO);
                    return;
                }

                // Call getAttribute1 on the service.
                Long attrib1Out = (Long) getAtrib1Method.invoke(prod1Svc);
                if (attrib1Out == null || attrib1Out.longValue() != EXPECTED_ATTRB1.longValue()) {
                    printOutput(resp, "TEST_FAILED: getAttribute1 returned: " + attrib1Out + ". Expected: " + EXPECTED_ATTRB1
                                      + ". Possible configuration error. The OCD alias under which attribute1 is defined must be prefixed by: \"productName_\".");
                    return;
                }

                // Call getAttribute2 on the service.
                String attrib2Out = (String) getAtrib2Method.invoke(prod1Svc);
                if (attrib2Out == null || !attrib2Out.equals(EXPECTED_ATTRB2)) {
                    printOutput(resp, "TEST_FAILED: getAttribute2 returned: " + attrib2Out + ". Expected: " + EXPECTED_ATTRB2
                                      + ". Possible configuration error. The OCD alias under which attribute2 is defined must be prefixed by: \"productName_\".");
                    return;
                }

                printOutput(resp, "TEST_PASSED: sayHello returned: " + sayHelloOut + ". getAttribute2 returned: " + attrib2Out);
            } else {
                printOutput(resp, "TEST_FAILED:Product1 service not found.");
            }
        } catch (Throwable t) {
            printOutput(resp, "TEST_FAILED: Exception while processing test: " + t);
        } finally {
            if (prod1SvcRef != null)
                bundleContext.ungetService(prod1SvcRef);
        }
    }

    /**
     * Prints the output to the HTTP call.
     * 
     * @param resp HttpServletResponse
     * @param reply The repply.
     */
    private void printOutput(HttpServletResponse resp, String reply) {
        System.out.println(reply);

        try {
            resp.getWriter().print(reply);
        } catch (IOException ioe) {
            System.out.println("Failed setting the reply in the HttpServletResponse" + ioe);
        }
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}

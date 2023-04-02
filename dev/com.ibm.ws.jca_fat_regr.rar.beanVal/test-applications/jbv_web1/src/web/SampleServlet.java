/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ejb.SampleSessionLocal;

@SuppressWarnings("serial")
public class SampleServlet extends HttpServlet {
    private final static String CLASSNAME = SampleServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @EJB(beanName = "SampleSessionImpl")
    private SampleSessionLocal sampleSessionBean;

    //@Override
    protected void doGet(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
        processRequest(arg0, arg1);
    }

    //@Override
    protected void doPost(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
        processRequest(arg0, arg1);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter pw = response.getWriter();
        String meth = "processRequest";
        try {
            String testNumString = request.getParameter("testNum");
            int testNum = (testNumString == null) ? 0 : Integer.parseInt(testNumString);

            String name = request.getParameter("name");
            if (name == null)
                name = "Apache Geronimo";

            boolean result;

            switch (testNum) {
                case 0:
                    svLogger.logp(Level.INFO, CLASSNAME, meth, "Test number 0: Invoke EJB greeting");
                    pw.println(sampleSessionBean.greeting(name));
                    break;
                case 1:
                    svLogger.logp(Level.INFO, CLASSNAME, meth, "Test number 1: Invoke EJB validation");
                    try {
                        result = sampleSessionBean.validGreeting(name);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        result = false;
                    }
                    // Result format is "<pass|fail>:<assert-condition-string>"
                    pw.println((result ? "pass" : "fail") + ":EJB validator injection and validation completed for test number 1");
                    break;
                default:
                    pw.println("fail:Test number " + testNumString + " is valid");
                    break;
            }
        } catch (Throwable ex) {
            pw.println("fail:Unexpected Exception : " + ex);
            ex.printStackTrace();
        }
    }

}

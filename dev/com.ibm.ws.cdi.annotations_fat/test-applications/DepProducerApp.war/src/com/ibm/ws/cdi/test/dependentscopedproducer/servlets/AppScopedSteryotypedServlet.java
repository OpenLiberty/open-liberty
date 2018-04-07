/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.test.dependentscopedproducer.servlets;
import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.test.dependentscopedproducer.NonNullBeanThree;

//This servlet should return a resource injection exception when accessed. 
@WebServlet("/failAppSteryotypedMethod")
public class AppScopedSteryotypedServlet extends HttpServlet {
    
    @Inject NonNullBeanThree nullBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter pw = response.getWriter();

        try {
            nullBean.toString(); //calling a method as a proxy gets injected.
            pw.write("A nullBean was injected. Test Failed");
        }
        catch (Exception e) { //I'm doing it this way to avoid adding a dependency on weld. 
            if (e.getMessage().contains("WELD-000052")) {
                pw.write("Test Passed");
            } else {
                 pw.write("The wrong exception was thrown: " + e.getMessage());
            }
        }
        
        pw.flush();
        pw.close();
    }

}

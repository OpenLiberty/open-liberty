/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.spi.test.app;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.misplaced.spi.test.bundle.extension.MyExtensionString;
import com.ibm.ws.cdi.misplaced.spi.test.bundle.getclass.beaninjection.MyBeanInjectionString;
import com.ibm.ws.cdi.misplaced.spi.test.bundle.getclass.producer.MyProducedString;

@WebServlet("/misplaced")
public class MisplacedTestServlet extends HttpServlet {

    @Inject
    MyProducedString classString;

    @Inject
    MyBeanInjectionString beanInjectedString;

    @Inject
    AppBean appBean;

    @Inject
    CustomBDABean customBDABean;

    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        String unregString = "";
        try {
            //This will fail because while the extension will run as expected and add MyExtensionString to the BDA, it will be filtered out later because it cannot be found in the bundle.
            MyExtensionString ub = javax.enterprise.inject.spi.CDI.current().select(MyExtensionString.class).get();
            unregString = "Bean registered via an extension when both the bean and the extension are in a different bundle to the SPI impl class. This is unexpected";
        } catch (UnsatisfiedResolutionException e) {
            unregString = "Could not find bean registered via an extension when both the bean and the extension are in a different bundle to the SPI impl class";
        }

        PrintWriter pw = response.getWriter();
        pw.println("Test Results:");
        pw.println(unregString);
        pw.println(beanInjectedString.toString());
        pw.println(classString.toString());
        pw.println(appBean.toString());
        pw.println(customBDABean.toString());

    }
}

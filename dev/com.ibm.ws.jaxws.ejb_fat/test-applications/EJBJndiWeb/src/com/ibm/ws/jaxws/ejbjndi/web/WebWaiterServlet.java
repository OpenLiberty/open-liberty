/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejbjndi.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.ejbjndi.webejb.client.CoffeeMachine;
import com.ibm.ws.jaxws.ejbjndi.webejb.client.MixedCoffeeMachineService;

/**
 * With the current design, this servlet will not be invoked directory, and it is only used for declaring the WebServiceRef
 */
@WebServlet("/WebWaiterServlet")
public class WebWaiterServlet extends HttpServlet {

    private static final long serialVersionUID = 5771469072243880591L;

    @WebServiceRef(name = "java:module/env/services/coffeeMachine", value = MixedCoffeeMachineService.class)
    private CoffeeMachine moduleScopedCoffeeMachine;

    @WebServiceRef(name = "java:app/env/services/web/coffeeMachine", value = MixedCoffeeMachineService.class)
    private CoffeeMachine appScopedCoffeeMachine;

    @WebServiceRef(name = "java:global/env/services/web/coffeeMachine", value = MixedCoffeeMachineService.class)
    private CoffeeMachine globalScopedCoffeeMachine;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        writer.write("FAILED No Test should invoke this servlet");
    }

}

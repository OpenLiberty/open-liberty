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
package com.ibm.ws.jaxws.ejbjndi.webejb;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.ejbjndi.webejb.client.Coffee;
import com.ibm.ws.jaxws.ejbjndi.webejb.client.CoffeeMachine;
import com.ibm.ws.jaxws.ejbjndi.webejb.client.MixedCoffeeMachineService;

@WebServlet("/MixedWaiterServlet")
public class MixedWaiterServlet extends HttpServlet {

    private static final long serialVersionUID = -6237417105081723959L;

    @EJB
    private MixedWaiter mixedWaiter;

    @WebServiceRef(value = MixedCoffeeMachineService.class)
    private CoffeeMachine compScopedCoffeeMachine;

    /*
     * For EJB-IN-WAR, web module should share the same comp and module context with EJB bean
     */
    @WebServiceRef(lookup = "java:comp/env/com.ibm.ws.jaxws.ejbjndi.webejb.MixedWaiter/compScopedCoffeeMachine")
    private CoffeeMachine compScopedCoffeeMachine2;

    @WebServiceRef(name = "java:module/env/services/coffeeMachine", value = MixedCoffeeMachineService.class)
    private CoffeeMachine moduleScopedCoffeeMachine;

    @WebServiceRef(name = "java:app/env/services/mixed/coffeeMachine", value = MixedCoffeeMachineService.class)
    private CoffeeMachine appScopedCoffeeMachine;

    @WebServiceRef(name = "java:global/env/services/mixed/coffeeMachine", value = MixedCoffeeMachineService.class)
    private CoffeeMachine globalScopedCoffeeMachine;

    @WebServiceRef(lookup = "java:app/env/services/ejb/coffeeMachine")
    private CoffeeMachine nonLocalAppScopedEJBCoffeeMachine;

    @WebServiceRef(lookup = "java:global/env/services/ejb/coffeeMachine")
    private CoffeeMachine nonLocalGlobalEJBCoffeeMachine;

    @WebServiceRef(lookup = "java:app/env/services/web/coffeeMachine")
    private CoffeeMachine nonLocalAppScopedWebCoffeeMachine;

    @WebServiceRef(lookup = "java:global/env/services/web/coffeeMachine")
    private CoffeeMachine nonLocalGlobalWebCoffeeMachine;

    private Coffee order(String type, String hostname, String port) {

        EndpointInfoHolder.COFFEEMATE_PROVIDER_ENDPOINT_URL = "http://" + hostname + ":" + port + "/EJBJndiEJB/EJBCoffeemateProviderService";
        EndpointInfoHolder.MILK_PROVIDER_ENDPOINT_URL = "http://" + hostname + ":" + port + "/EJBJndiWeb/WebMilkProviderService";

        if (type.equals("testWebEJBComp")) {
            configureEndpointAddress(compScopedCoffeeMachine, hostname, port);
            return compScopedCoffeeMachine.make(type);
        } else if (type.equals("testWebEJBModule")) {
            configureEndpointAddress(moduleScopedCoffeeMachine, hostname, port);
            return moduleScopedCoffeeMachine.make(type);
        } else if (type.equals("testWebEJBApp")) {
            configureEndpointAddress(appScopedCoffeeMachine, hostname, port);
            return appScopedCoffeeMachine.make(type);
        } else if (type.equals("testWebEJBGlobal")) {
            configureEndpointAddress(globalScopedCoffeeMachine, hostname, port);
            return globalScopedCoffeeMachine.make(type);
        } else if (type.equals("testWebEJBNonLocalAppEJB")) {
            configureEndpointAddress(nonLocalAppScopedEJBCoffeeMachine, hostname, port);
            return nonLocalAppScopedEJBCoffeeMachine.make(type);
        } else if (type.equals("testWebEJBNonLocalGlobalEJB")) {
            configureEndpointAddress(nonLocalGlobalEJBCoffeeMachine, hostname, port);
            return nonLocalGlobalEJBCoffeeMachine.make(type);
        } else if (type.equals("testWebEJBNonLocalAppWeb")) {
            configureEndpointAddress(nonLocalAppScopedWebCoffeeMachine, hostname, port);
            return nonLocalAppScopedWebCoffeeMachine.make(type);
        } else if (type.equals("testWebEJBNonLocalGlobalWeb")) {
            configureEndpointAddress(nonLocalGlobalWebCoffeeMachine, hostname, port);
            return nonLocalGlobalWebCoffeeMachine.make(type);
        } else if (type.equals("testWebEJBEJBComp")) {
            configureEndpointAddress(compScopedCoffeeMachine2, hostname, port);
            return compScopedCoffeeMachine2.make(type);
        }

        return null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();

        String type = req.getParameter("type");
        if (type == null) {
            writer.write("FAILED no type input");
            return;
        }

        String target = req.getParameter("target");
        if (target == null) {
            writer.write("FAILED no target input");
            return;
        }

        String hostname = req.getParameter("hostname");
        String port = req.getParameter("port");

        String response = null;
        if (target.equals("Servlet")) {
            response = order(type, hostname, port).toString();
        } else if (target.equals("EJB")) {
            response = mixedWaiter.order(type, hostname, port).toString();
        } else {
            writer.write("FAILED unrecognized target = " + target);
            return;
        }

        if (response == null) {
            writer.write("FAILED type = " + type + " target =" + target + " coffee = " + response);
        } else {
            writer.write("PASS type = " + type + " target =" + target + " coffee = " + response);
        }
    }

    private void configureEndpointAddress(CoffeeMachine coffeeMachine, String hostname, String port) {
        BindingProvider bindingProvider = (BindingProvider) coffeeMachine;
        String endpointURL = "http://" + hostname + ":" + port + "/EJBJndiWebEJB/MixedCoffeeMachineService";
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointURL);
    }
}

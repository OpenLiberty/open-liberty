/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxws22.respectbinding.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.RespectBinding;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceRef;

/**
 *
 */
public class EchoServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 7492748394618809256L;

    @WebServiceRef(type = EchoService.class)
    EchoService noAnnoService;

    @RespectBinding(enabled = true)
    @WebServiceRef(type = EchoService.class)
    EchoService annoTrueService;

    @RespectBinding(enabled = false)
    @WebServiceRef(type = EchoService.class)
    EchoService annoFalseService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Obtain the serviceType that's passed by the test so that we can know which
        // one of the EchoService objects we should use for this test
        String serviceType = request.getParameter("serviceType");

        System.out.println("EchoServlet received a request to use testService: " + serviceType);

        PrintWriter writer = response.getWriter();
        String host = request.getLocalAddr();
        int port = request.getLocalPort();
        String newTarget = "http://" + host + ":" + port + "/respectBindingService/EchoImpl";

        Echo proxy;
        if (serviceType == "annotationTrue") {
            proxy = annoTrueService.getEchoPort();
        } else if (serviceType == "annotationFalse") {
            proxy = annoFalseService.getEchoPort();
        } else {
            proxy = noAnnoService.getEchoPort();
        }

        BindingProvider bp = (BindingProvider) proxy;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, newTarget);

        String respString;
        try {
            respString = proxy.echo("Hello World");
            writer.println(respString);

        } catch (Exception_Exception x) {

        } catch (Throwable e) {
            if (e instanceof WebServiceException) {
                System.out.println("Caught expected Exception");
                writer.println(e.getMessage());
            } else
                writer.println(e.getMessage());
            //throw e;
        }
        writer.flush();
        writer.close();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }
}

/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.was.cxfsample.client;

import java.io.IOException;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.WebServlet;

import javax.xml.ws.WebServiceRef;
import com.ibm.was.cxfsample.sei.echo.EchoService;
import java.net.URL;

/**
 * Servlet implementation class 
 */
@WebServlet("CxfClientServlet")
public class CxfClientServlet extends HttpServlet {
    private static final long serialVersionUID = -1;
    private static final String OUTPUT_JSP_LOCATION = "/output.jsp";
    private static final String INDEX_JSP_LOCATION = "/default.jsp";

    // Currently, the Echo.wsdl is put in the server.config.dir directory
    // We need to handle the Echo.wsdl. especially the service-provider endpoint
    // Such as: http://localhost:9080/cxfsampleSei/EchoService in this example
    // <wsdl:service name="EchoService">
    //  <wsdl:port binding="tns:EchoSOAP" name="EchoServicePort">
    //      <soap:address
    //          location="http://localhost:9080/cxfsampleSei/EchoService" />
    //  </wsdl:port>
    // </wsdl:service>
    //
    @WebServiceRef(value=EchoService.class, wsdlLocation="Echo.wsdl")
    EchoService echoService;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CxfClientServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        System.out.println("doget():EchoService:'" + echoService + "'");
        processRequest(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * processRequest Reads the posted parameters and calls the service
     */
    private void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Set up defaults
        String uriString = req.getParameter("serviceURL");
        String scenarioString = req.getParameter("scenario");
        String testString = req.getParameter("test");
        String cntString = "";
        String optionsString = "";
        String result = "";
        SampleClient client = new SampleClient();
        client.setEchoService(echoService); // pass EchoSerivce on since it only gets injected in this class

        // These three are required parms. Check they are all there
        if ((null == uriString) || (uriString.isEmpty()) ||
            (null == scenarioString) || (scenarioString.isEmpty()) ||
            (null == testString) || (testString.isEmpty())  ) {
            // Not a good request, return default page
            getServletContext().getRequestDispatcher(INDEX_JSP_LOCATION).forward(req, resp);
        } else {
            // Get the parms from the request
            String soapString = req.getQueryString();
            String contextString = getServletContext().getContextPath();
            cntString = req.getParameter("msgcount");
            if (null == cntString || cntString.isEmpty()) {
                cntString = "1";
            }
                    
            // Get the options if any
            optionsString = req.getParameter("options");
            if (null == optionsString) {
                optionsString = "soap11";
            }
    
            // Get the soap settings
            if (soapString.contains("&soap11") || soapString.contains("?soap11"))
            {
                optionsString = "soap11";
            }

            if (soapString.contains("&soap12") || soapString.contains("?soap12"))
            {
                optionsString = "soap12";
            }
            
            // Get message
            String messageString = req.getParameter("message");
            if ((null == messageString) || (messageString.isEmpty())) {
                Random random = new Random();
                messageString = "Hello"+Double.toString(random.nextDouble());
            }

            // Set the values to be on the refreshed page
            req.setAttribute("serviceURL", uriString);
            req.setAttribute("msgcount", cntString);

            // Now call the service
            System.out.println(">> SERVLET: Request count = " + cntString);
            result = client.CallService(contextString, uriString, scenarioString, testString, optionsString, messageString, new Integer(cntString));

            // Format the output and refresh the panel
            req.setAttribute("messageR", result);
            getServletContext().getRequestDispatcher(OUTPUT_JSP_LOCATION).forward(req, resp);
        }
    }

    public URL getResource() {

        return getClass().getResource("Echo.wsdl");
    }
}

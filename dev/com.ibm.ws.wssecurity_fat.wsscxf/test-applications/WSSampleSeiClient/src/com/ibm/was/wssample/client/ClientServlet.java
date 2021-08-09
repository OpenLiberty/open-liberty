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

/**
 * This program may be used, executed, copied, modified and distributed
 * without royalty for the purpose of developing, using, marketing, or distributing.
 *
 *
- test client URLs should expect URLs of the form:
        <test url from endpoints page>?serviceURL="..."&scenario="..."&test="..."&<options>
        where <options> will be vary between the scenarios but will usually be like this:
        &soap11
        &soap12
        &wsarec
        &wsa04

- test client results need to be of the form:
<testResult scenario="sc002" serviceURL="..." status="pass" test="1.2" options="..."? xmlns="http://www.wstf.org/"; ...>
  ...anything of interest from the execution...
</testResult>


<testResults xmlns='http://www.wstf.org' status='pass' scenario='sc002' test='1.2' serviceURL='...'>
  <testResult scenario="sc002" serviceURL="http://www.soaphub.org/wstf/services/sc002"; status="pass" test="1.2"
    options="soap11" xmlns="http://www.wstf.org/"/>;
  <testResult scenario="sc002" serviceURL="http://www.soaphub.org/wstf/services/sc002"; status="pass" test="1.2"
    options="soap12" xmlns="http://www.wstf.org/"/>;
</testResults>

 *
 *
 **/
package com.ibm.was.wssample.client;

import java.io.IOException;
import java.net.URL;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;

import com.ibm.was.wssample.sei.echo.Echo11Service;
import com.ibm.was.wssample.sei.echo.Echo12Service;
import com.ibm.was.wssample.sei.echo.Echo13Service;
import com.ibm.was.wssample.sei.echo.Echo14Service;
import com.ibm.was.wssample.sei.echo.Echo1Service;
import com.ibm.was.wssample.sei.echo.Echo21Service;
import com.ibm.was.wssample.sei.echo.Echo22Service;
import com.ibm.was.wssample.sei.echo.Echo23Service;
import com.ibm.was.wssample.sei.echo.Echo2Service;
import com.ibm.was.wssample.sei.echo.Echo3Service;
import com.ibm.was.wssample.sei.echo.Echo4Service;
import com.ibm.was.wssample.sei.echo.Echo5Service;
import com.ibm.was.wssample.sei.echo.Echo6Service;
import com.ibm.was.wssample.sei.echo.Echo7Service;
import com.ibm.was.wssample.sei.echo.EchoService;

/**
 * Servlet implementation class
 */
@WebServlet("ClientServlet")
public class ClientServlet extends HttpServlet {
    private static final long serialVersionUID = -1;
    private static final String OUTPUT_JSP_LOCATION = "/output.jsp";
    private static final String INDEX_JSP_LOCATION = "/default.jsp";

    @WebServiceRef(value = EchoService.class, wsdlLocation = "Echo.wsdl")
    EchoService echoService;

    @WebServiceRef(value = Echo1Service.class, wsdlLocation = "Echo.wsdl")
    Echo1Service echo1Service;

    @WebServiceRef(value = Echo2Service.class, wsdlLocation = "Echo.wsdl")
    Echo2Service echo2Service;

    @WebServiceRef(value = Echo3Service.class, wsdlLocation = "Echo.wsdl")
    Echo3Service echo3Service;

    @WebServiceRef(value = Echo4Service.class, wsdlLocation = "Echo.wsdl")
    Echo4Service echo4Service;

    @WebServiceRef(value = Echo5Service.class, wsdlLocation = "Echo.wsdl")
    Echo5Service echo5Service;

    @WebServiceRef(value = Echo6Service.class, wsdlLocation = "Echo.wsdl")
    Echo6Service echo6Service;

    @WebServiceRef(value = Echo7Service.class, wsdlLocation = "Echo.wsdl")
    Echo7Service echo7Service;

    @WebServiceRef(value = Echo11Service.class, wsdlLocation = "EchoBsp.wsdl")
    Echo11Service echo11Service;

    @WebServiceRef(value = Echo12Service.class, wsdlLocation = "EchoBsp.wsdl")
    Echo12Service echo12Service;

    @WebServiceRef(value = Echo13Service.class, wsdlLocation = "EchoBsp.wsdl")
    Echo13Service echo13Service;

    @WebServiceRef(value = Echo14Service.class, wsdlLocation = "EchoBsp.wsdl")
    Echo14Service echo14Service;

    @WebServiceRef(value = Echo21Service.class, wsdlLocation = "EchoX509.wsdl")
    Echo21Service echo21Service;

    @WebServiceRef(value = Echo22Service.class, wsdlLocation = "EchoX509.wsdl")
    Echo22Service echo22Service;

    @WebServiceRef(value = Echo23Service.class, wsdlLocation = "EchoX509.wsdl")
    Echo23Service echo23Service;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ClientServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        // debug
        System.out.println("doget(): WSSampleSei ClientServerlet");
        processRequest(request, response);
    }

    public Service getService(String scenario) {
        if (scenario.equals("EchoService"))
            return echoService;
        else if (scenario.equals("Echo1Service"))
            return echo1Service;
        else if (scenario.equals("Echo2Service"))
            return echo2Service;
        else if (scenario.equals("Echo3Service"))
            return echo3Service;
        else if (scenario.equals("Echo4Service"))
            return echo4Service;
        else if (scenario.equals("Echo5Service"))
            return echo5Service;
        else if (scenario.equals("Echo6Service"))
            return echo6Service;
        else if (scenario.equals("Echo7Service"))
            return echo7Service;
        else if (scenario.equals("Echo11Service"))
            return echo11Service;
        else if (scenario.equals("Echo12Service"))
            return echo12Service;
        else if (scenario.equals("Echo13Service"))
            return echo13Service;
        else if (scenario.equals("Echo14Service"))
            return echo14Service;
        else if (scenario.equals("Echo21Service"))
            return echo21Service;
        else if (scenario.equals("Echo22Service"))
            return echo22Service;
        else if (scenario.equals("Echo23Service"))
            return echo23Service;
        return null;
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * processRequest Reads the posted parameters and calls the service
     */
    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Set up defaults
        String uriString = req.getParameter("serviceURL");
        String scenarioString = req.getParameter("scenario");
        String testString = req.getParameter("test");
        String cntString = "";
        String optionsString = "";
        String result = "";
        SampleClient client = new SampleClient();

        // These three are required parms. Check they are all there
        if ((null == uriString) || (uriString.isEmpty()) ||
            (null == scenarioString) || (scenarioString.isEmpty()) ||
            (null == testString) || (testString.isEmpty())) {
            // Not a good request, return default page
            getServletContext().getRequestDispatcher(INDEX_JSP_LOCATION).forward(req, resp);
        } else {

            Service service = getService(scenarioString);
            System.out.println(scenarioString + " instance is '" + service + "'(can not be null)");
            client.setEchoService(service);

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
            if (soapString.contains("&soap11") || soapString.contains("?soap11")) {
                optionsString = "soap11";
            }

            if (soapString.contains("&soap12") || soapString.contains("?soap12")) {
                optionsString = "soap12";
            }

            // Get message
            String messageString = req.getParameter("message");
            if ((null == messageString) || (messageString.isEmpty())) {
                Random random = new Random();
                messageString = "Hello" + Double.toString(random.nextDouble());
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

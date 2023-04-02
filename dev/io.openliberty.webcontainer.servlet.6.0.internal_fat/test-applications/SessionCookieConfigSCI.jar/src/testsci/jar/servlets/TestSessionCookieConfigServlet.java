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
package testsci.jar.servlets;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *  Test servlet to retrieve session cookie data from web.xml or application (via SCI)
 *
 *  When both configs are presented, SCI takes precedence.
 *  In the test, use jvm.options set.cookie.config.sci.setCookieConfigViaSCI to control the SCI or web.xml
 */
public class TestSessionCookieConfigServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestSessionCookieConfigServlet.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    public TestSessionCookieConfigServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();
        sos.println("Hello World from TestSessionCookieConfigServlet");
        
        //TestSessionCookieConfigServlet?testName=TestIllegalOperation
        String testName = request.getParameter("testName");

        if (testName != null){
            //setAttribute after app started will cause IllegalStateException
            if (testName.equals("TestIllegalOperation")){
                testIllegalOperation(response);
            }
        }
        else if (ServletContainerInitializerImpl.isSetCookieConfigViaSCI){
            LOG.info("JVM property set.cookie.config.sci.setCookieConfigViaSCI set to true");
            testGetSessionCookieConfigSCI(response);
        }
        else {
            testGetSessionCookieConfigXML(response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    //Change values in SCI files to simulate the wrong returned values.
    //SCI values will override the web.xml setting
    private void testGetSessionCookieConfigSCI(HttpServletResponse response)  {
        
        ServletContext context = getServletContext();
        SessionCookieConfig scc = context.getSessionCookieConfig();

        LOG.info(" testGetSessionCookieConfigSCI | ServletContext [" + context + " | SessionCookieConfig [" + scc + "]");
       
        String cookieName;
        String cookieValue;
        int cookieInt;
        boolean testPass = true;

        StringBuilder sBuilderResponse = new StringBuilder("TEST testGetSessionCookieConfigSCI . Message [");

        //Validate Domain via specific getDomain()
        if (!(cookieValue = scc.getDomain()).equals("setAttDomain_viaSCI")) {
            testPass = false;
            sBuilderResponse.append(" FAIL getDomain() expecting [setAttDomain_viaSCI] , actual [" + cookieValue + "] |");
            LOG.info("Test scc.getDomain() FAIL");
        }
        else
            sBuilderResponse.append(" getDomain() PASS |");
        
        if ( !((cookieInt = scc.getMaxAge()) == 2022) ) {
            testPass = false;
            sBuilderResponse.append(" FAIL getMaxAge() expecting [2022] , actual [" + cookieInt + "] |");
            LOG.info("Test scc.getMaxAge() FAIL");
        }
        else
            sBuilderResponse.append(" getMaxAge() PASS |");
        
        if (!(cookieValue = scc.getName()).equals("SessionCookieConfig6_viaSCI")) {
            testPass = false;
            sBuilderResponse.append(" FAIL getName() expecting [SessionCookieConfig6_viaSCI] , actual [" + cookieValue + "] |");
            LOG.info("Test scc.getName() FAIL");
        }
        else
            sBuilderResponse.append(" getName() PASS |");
        
        if (!(cookieValue = scc.getPath()).equals("setPATH_viaSCI")) {
            testPass = false;
            sBuilderResponse.append(" FAIL getPath() expecting [setPATH_viaSCI] , actual [" + cookieValue + "] |");
            LOG.info("Test scc.getPath() FAIL");
        }
        else
            sBuilderResponse.append(" getPath() PASS |");
        
        if (!scc.isHttpOnly()) {
            testPass = false;
            sBuilderResponse.append(" FAIL isHttpOnly() expecting [true] , actual [" + scc.isHttpOnly() + "] |");
            LOG.info("Test scc.isHttpOnly() FAIL");
        }
        else
            sBuilderResponse.append(" isHttpOnly() PASS |");
        
        //Test precedence - SCI overwrite web.xml
        if (!(cookieValue = scc.getAttribute("AttName1")).equals("AttValue1SCI")) {
            testPass = false;
            sBuilderResponse.append(" FAIL getAttribute(\"AttName1\") expecting [AttValue1SCI] , actual [" + cookieValue + "] |");
            LOG.info("Test precedence order FAIL");
        }
        else
            sBuilderResponse.append(" Test precedence order PASS |");
       
        //Test getAttribute from web.xml
        if (!(cookieValue = scc.getAttribute("AttNameUnique")).equals("AttValueUnique_viaWebXML")) {
            testPass = false;
            sBuilderResponse.append(" FAIL getAttribute(\"AttNameUnique\") expecting [AttValueUnique_viaWebXML] , actual [" + cookieValue + "] |");
            LOG.info("Test getAttribute web.xml FAIL");
        }
        else
            sBuilderResponse.append(" Test getAttribute web.xml PASS |"); 
        
        //Note the lower case att name
        if (!(cookieValue = scc.getAttribute("attname5")).equals("AttValue5")) {
            testPass = false;
            sBuilderResponse.append(" FAIL getAttribute(\"attname5\") expecting [AttValue5] , actual [" + cookieValue + "] |");
            LOG.info(" Test lower case att name FAIL");
        } 
        else
            sBuilderResponse.append(" Test lower case att name PASS |");
        
        //Test specific Domain that set via setAttribute and retrieve via getAttribute
        if (!(cookieValue = scc.getAttribute("DOMAIN")).equals("setAttDomain_viaSCI")) {
            testPass = false;
            sBuilderResponse.append(" FAIL getAttribute(\"DOMAIN\") expecting [setAttDomain_viaSCI] , actual [" + cookieValue + "] |");
            LOG.info(" Test getAttribute(\"DOMAIN\") FAIL");
        }
        else
            sBuilderResponse.append(" Test getAttribute(\"DOMAIN\") PASS |");
        
        if (!(cookieValue = scc.getAttribute("path")).equals("setPATH_viaSCI")) {
            testPass = false;
            sBuilderResponse.append(" FAIL getAttribute(\"path\") expecting [setPATH_viaSCI] , actual [" + cookieValue + "] |");
            LOG.info("Test getAttribute(\"path\") FAIL");
        }
        else
            sBuilderResponse.append(" Test getAttribute(\"path\") PASS |");
        
        //Validate setAttribute with null name.  Servlet has verified the translated message code and reports whether PASS or FAIL.
        if (!(cookieValue = scc.getAttribute("ReportedNullAttName")).equals("PASS")) {
            testPass = false;
            sBuilderResponse.append(" FAIL getAttribute(\"ReportedNullAttName\") expecting [PASS] , actual [" + cookieValue + "] |");
            LOG.info(" Test NULL att name FAIL");
        }
        else
            sBuilderResponse.append(" Test NULL att name PASS |");
       
        //Validate setAttribute with invalid name.
        if (!(cookieValue = scc.getAttribute("ReportedInvalidAttName")).equals("PASS")) {
            testPass = false;
            sBuilderResponse.append(" FAIL getAttribute(\"ReportedInvalidAttName\") expecting [PASS] , actual [" + cookieValue + "] |");
            LOG.info(" Test report invalid att name FAIL");
        }
        else
            sBuilderResponse.append(" Test report invalid att name PASS |");
      
        
        //Final result - any of the above tests fail will make test fail
        if (testPass)
            sBuilderResponse.append("]  Result [PASS]");
        else
            sBuilderResponse.append("]  Result [FAIL]");
       
        //Client check this header.
        response.setHeader("TestResult", sBuilderResponse.toString());                   
       
        LOG.info(sBuilderResponse.toString());
    }


    //Test session cookie-config from web.xml
    private void testGetSessionCookieConfigXML(HttpServletResponse response)  {
        ServletContext context = getServletContext();
        SessionCookieConfig scc = context.getSessionCookieConfig();

        LOG.info(" testGetSessionCookieConfigXML | ServletContext [" + context + " . SessionCookieConfig [" + scc + "]");
       
        String cookieName;
        String cookieValue;
        int cookieInt;
        boolean testPass = true;

        StringBuilder sBuilderResponse = new StringBuilder("TEST testGetSessionCookieConfigXML . Message [");

        //Test Domain via specific getDomain()
        if (!(cookieValue = scc.getDomain()).equals("CookieConfigDomain_viaWebXML")) {
            testPass = false;
            sBuilderResponse.append(" FAIL getDomain() expecting [CookieConfigDomain_viaWebXML] , actual [" + cookieValue + "] ||");
            LOG.info("Test scc.getDomain() FAIL");
        }
        else
            sBuilderResponse.append(" getDomain() PASS |");
            
        
        if ( !((cookieInt = scc.getMaxAge()) == 2021) ) {
            testPass = false;
            sBuilderResponse.append(" FAIL getMaxAge() expecting [2021] , actual [" + cookieInt + "] ||");
            LOG.info("Test scc.getMaxAge() FAIL");
        }
        else
            sBuilderResponse.append(" getMaxAge() PASS |");
            
        
        if (!(cookieValue = scc.getName()).equals("CookieConfigName_viaWebXML")) {
            testPass = false;
            sBuilderResponse.append(" FAIL getName() expecting [CookieConfigName_viaWebXML] , actual [" + cookieValue + "] ||");
            LOG.info("Test scc.getName() FAIL");
        }
        else
            sBuilderResponse.append(" getName() PASS |");
            
        
        if (!(cookieValue = scc.getPath()).equals("CookieConfigPath_viaWebXML")) {
            testPass = false;
            sBuilderResponse.append(" FAIL getPath() expecting [CookieConfigPath_viaWebXML] , actual [" + cookieValue + "] ||");
            LOG.info("Test scc.getPath() FAIL");
        }
        else
            sBuilderResponse.append(" getPath() PASS |");
            
        
        if (!scc.isHttpOnly()) {
            testPass = false;
            sBuilderResponse.append(" FAIL isHttpOnly() expecting [true] , actual [" + scc.isHttpOnly() + "] ||");
            LOG.info("Test scc.isHttpOnly() FAIL");
        }
        else
            sBuilderResponse.append(" isHttpOnly() PASS |");
            

         //Test Domain via getAttribute
        if (!(cookieValue = scc.getAttribute("DOMAIN")).equals("CookieConfigDomain_viaWebXML")) {
            testPass = false;
            sBuilderResponse.append(" FAIL getAttribute(\"DOMAIN\") expecting [CookieConfigDomain_viaWebXML] , actual [" + cookieValue + "] ||");
            LOG.info("Test scc.getAttribute(\"DOMAIN\") FAIL");
        }
        else
            sBuilderResponse.append(" getAttribute(\"DOMAIN\") PASS |");

        //Test set/getAttribute from web.xml.  NPE can happen if the web.xml cannot parse for some reasons; thus the try/catch
        try {
            if (!(cookieValue = scc.getAttribute("AttName1")).equals("AttValue1_viaWebXML")) {
                testPass = false;
                sBuilderResponse.append(" FAIL getAttribute(\"AttName1\")) expecting [AttValue1_viaWebXML] , actual [" + cookieValue + "] ||");
                LOG.info("Test scc.getAttribute(\"AttName1\") FAIL");
            }
            else
                sBuilderResponse.append(" getAttribute(\"AttName1\") PASS |");
           
            //test getAttributes
            if (!(scc.getAttributes().get("AttName2")).equals("AttValue2_viaWebXML")) {
                testPass = false;
                sBuilderResponse.append(" FAIL  getAttributes().get(\"AttName2\") expecting [AttValue2_viaWebXML] , actual [" + cookieValue + "] ||");
                LOG.info("Test scc.getAttributes.get(\"AttName2\") FAIL");
            }
            else
                sBuilderResponse.append(" getAttributes().get(\"AttName2\") PASS |");
                
            //test getAttributes.get("path") with lower case path
            if (!(scc.getAttributes().get("path")).equals("CookieConfigPath_viaWebXML")) {
                testPass = false;
                sBuilderResponse.append(" FAIL getAttributes().get(\"path\") expecting [CookieConfigPath_viaWebXML] , actual [" + cookieValue + "] ||");
                LOG.info("Test scc.getAttributes.get(\"AttName2\") FAIL");
            }
            else
                sBuilderResponse.append(" getAttributes().get(\"path\") PASS |");
        }
        catch (Exception e) { 
            testPass = false;
            sBuilderResponse.append(" getAttribute() tests FAIL with an exception [" + e + "] ||");
            LOG.info("Test scc.getAttribute() FAIL with Exception " + e);
        }
        
        if (testPass)
            sBuilderResponse.append("]  Result [PASS]");
        else
            sBuilderResponse.append("]  Result [FAIL]");
       
        //Client check this header.
        response.setHeader("TestResult", sBuilderResponse.toString());                   
       
        LOG.info(sBuilderResponse.toString());
    }
    
    //Test setAttribute after ServletContext has been initialized to cause IllegalStateException
    private void testIllegalOperation(HttpServletResponse response)  {
        ServletContext context = getServletContext();
        SessionCookieConfig scc = context.getSessionCookieConfig();

        LOG.info(" testIllegalOperation | ServletContext [" + context + " . SessionCookieConfig [" + scc + "]");
        
        StringBuilder sBuilderResponse = new StringBuilder("TEST testIllegalOperation . Message [");
        try {
            scc.setAttribute("AttName" , "AttValue");
            sBuilderResponse.append(" setAttribute after ServletContext already initialized does NOT cause IllegalException.  Result [FAIL]");
        }
        catch (IllegalStateException e) {
            LOG.info("Test setAttribute after ServletContext intialized causes IllegalStateException , e [" + e + "] . Test PASS");
            sBuilderResponse.append(" setAttribute after ServletContext already initialized causes IllegalStateException.  Result [PASS]");
        }

        //Client check this header.
        response.setHeader("TestResult", sBuilderResponse.toString());                   
        
        LOG.info(sBuilderResponse.toString());
    }
}

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
package characterencoding.servlets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.Locale;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test Response setCharacterEncoding(null), setContenType(null), setLocale(null)
 * 
 * request URL: /TestResponseNullCharacterEncoding?testName=<setMethodName>
 */
@WebServlet("/TestResponseNullCharacterEncoding")
public class TestResponseNullCharacterEncoding extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestResponseNullCharacterEncoding.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    public TestResponseNullCharacterEncoding() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String testName = request.getParameter("testName");

        if (testName == null) {
            return;
        }
        else if (testName.equalsIgnoreCase("setCharacterEncoding")) {
            testSetCharacterEncoding(request,  response);
        }
        else if (testName.equalsIgnoreCase("setContentType")) {
            testSetContentType(request, response); 
        }
        else if (testName.equalsIgnoreCase("setLocale")) {
            testSetLocale(request, response); 
        }
        else if (testName.equalsIgnoreCase("invalidEncoding")) {
            testInvalidEncoding(request, response); 
        }
    }

  
    /*
     * Test response.setCharacterEncoding("UTF-8") - Expect: UTF-8 encoding
     *      response.setCharacterEncoding(null) - Expect: default (ISO-8859-1) encoding
     */
    private void testSetCharacterEncoding(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info("Test testSetCharacterEncoding");
        
        StringBuilder sbReport = new StringBuilder("TEST testSetCharacterEncoding . Message [[[-- ");
        String defaultEnc = response.getCharacterEncoding();    //save the default encoding
        String afterSetEncoding;
        boolean pass = true;

        
        response.setCharacterEncoding("UTF-8");
        afterSetEncoding = response.getCharacterEncoding();
        
        if ("UTF-8".equalsIgnoreCase(afterSetEncoding)) {
            sbReport.append("Set with UTF-8 Pass | ");
        } else {
            LOG.info("Set with UTF-8 Fail");
            pass = false;
            sbReport.append("Set with UTF-8 Fail ; expecting [UTF-8] , found ["+ afterSetEncoding + "] | ");
        }

        //set to null
        response.setCharacterEncoding(null);
        afterSetEncoding = response.getCharacterEncoding();

        if ((defaultEnc == null && afterSetEncoding == null) || defaultEnc != null && defaultEnc.equalsIgnoreCase(afterSetEncoding)) {
            sbReport.append("Set with null Pass | ");
        } else {
            LOG.info("Set with null Fail");
            pass = false;
            sbReport.append("Set with null Fail ; expecting ["+ defaultEnc +"] , found ["+ afterSetEncoding +"] | ");
        }


        //Final result - any of the above tests fail will make test fail
        if (pass)
            sbReport.append(" --]]]. Result [PASS]");
        else
            sbReport.append(" --]]]. Result [FAIL]");


        //Client check this header.
        response.setHeader("TestResult", sbReport.toString());  

        LOG.info("Test testSetCharacterEncoding END");
    }

    /*
     * 1st: Test response.setContentType("UTF-8") - Expect: UTF-8 encoding; then response.setCharacterEncoding(null) - Expect: default encoding
     * 2nd: Test response.setContentType("UTF-8") - Expect: UTF-8 encoding; then response.setContentType(null) - Expect: default encoding
     */
    private void testSetContentType(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info("Test testSetContentType");

        StringBuilder sbReport = new StringBuilder("TEST testSetContentType . Message [[[-- ");
        String defaultEnc = response.getCharacterEncoding();    //save the default encoding
        String afterSetEncoding;
        boolean pass = true;

        //1st
        response.setContentType("text/plain; charset=UTF-8");
        afterSetEncoding = response.getCharacterEncoding();
        
        if ("UTF-8".equalsIgnoreCase(afterSetEncoding)) {
            sbReport.append("Set via Content-Type UTF-8 Pass | ");
        } else {
            LOG.info("Set via Content-Type UTF-8 Fail");
            pass = false;
            sbReport.append("Set via Content-Type Fail ; expecting enc [UTF-8] , found ["+ afterSetEncoding + "] | ");
        }

        response.setCharacterEncoding(null);
        afterSetEncoding = response.getCharacterEncoding();
        
        if ((defaultEnc == null && afterSetEncoding == null) || defaultEnc != null && defaultEnc.equalsIgnoreCase(afterSetEncoding)) {
            sbReport.append("Set with null Pass | ");
        } else {
            LOG.info("Set with null Fail");
            pass = false;
            sbReport.append("Set with null Fail ; expecting enc ["+ defaultEnc +"] , found ["+ afterSetEncoding +"] | ");
        }
        
        response.reset();
       
        //2nd 
        LOG.info("Test testSetContentType, 2nd");
        defaultEnc = response.getCharacterEncoding(); // get default enc again to make sure it is populated afteer response.reset
        response.setContentType("text/plain; charset=UTF-8");
        afterSetEncoding = response.getCharacterEncoding();
        
        if ("UTF-8".equalsIgnoreCase(afterSetEncoding)) {       //make sure enc is UTF-8
            
            response.setContentType(null);
            afterSetEncoding = response.getCharacterEncoding();
            
            if ((defaultEnc == null && afterSetEncoding == null) || defaultEnc != null && defaultEnc.equalsIgnoreCase(afterSetEncoding)) {
                sbReport.append("Set ContentType(NULL) Pass | ");
            } else {
                LOG.info("Set ContentType with null Fail");
                pass = false;
                sbReport.append("Set ContentType(NULL) Fail ; expecting enc ["+ defaultEnc +"] , found ["+ afterSetEncoding +"] | ");
            }

        } else { 
            LOG.info("Set ContentType(text/plain; charset=UTF-8) Fail");
            pass = false;
            sbReport.append("Set ContentType(text/plain; charset=UTF-8) Fail ; expecting enc [UTF-8] , found ["+ afterSetEncoding +"] | ");
            
        }
        
        if (pass)
            sbReport.append(" --]]]. Result [PASS]");
        else
            sbReport.append(" --]]]. Result [FAIL]");
       
        response.setHeader("TestResult", sbReport.toString());  
        
        LOG.info("Test testSetContentType END");
    }

    /*
    * 1st - Test response.setLocale("ja"), then setCharacterEncoding(null). - enc set back to default
    * 2nd - Test response.setLocale("ja"), then setLocale(null) - enc is set back to default
    * 3rd - Test response.setCharacterEncoding(UTF-8), then setLocale(null) - the enc remains UTF-8
    */
   private void testSetLocale(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
       LOG.info("Test testSetLocale");

       StringBuilder sbReport = new StringBuilder("TEST testSetLocale . Message [[[-- ");
       String defaultEnc = response.getCharacterEncoding();    //save the default encoding
       String afterSetEncoding;
       boolean pass = true;
       
       //1st
       response.setLocale(new Locale("ja"));
       afterSetEncoding = response.getCharacterEncoding();

       if ("Shift_Jis".equalsIgnoreCase(afterSetEncoding)) {
           sbReport.append("Set via Locale Pass | ");
       } else {
           LOG.info("Set via Locale (Shift_Jis) Fail |");
           pass = false;
           sbReport.append("Set via Locale (Shift_Jis) Fail ; expecting enc [Shift_Jis] , found ["+ afterSetEncoding + "] | ");
       }

       response.setCharacterEncoding(null);
       afterSetEncoding = response.getCharacterEncoding();

       if ((defaultEnc == null && afterSetEncoding == null) || defaultEnc != null && defaultEnc.equalsIgnoreCase(afterSetEncoding)) {
           sbReport.append("Set with null Pass | ");
       } else {
           LOG.info("Set with null Fail");
           pass = false;
           sbReport.append("Set with null Fail ; expecting enc ["+ defaultEnc +"] , found ["+ afterSetEncoding +"] | ");
       }

       response.reset();
       
      //2n case 
       LOG.info("Test testSetLocale, 2nd");
       defaultEnc = response.getCharacterEncoding(); 
       response.setLocale(new Locale("ja"));            //set again
       afterSetEncoding = response.getCharacterEncoding();
       
       if ("Shift_Jis".equalsIgnoreCase(afterSetEncoding)) {   //ensure ja trigger encoding setting to Shift_Jis
           response.setLocale(null); 
           afterSetEncoding = response.getCharacterEncoding();
           
           if ((defaultEnc == null && afterSetEncoding == null) || defaultEnc != null && defaultEnc.equalsIgnoreCase(afterSetEncoding)) {
               sbReport.append("2nd setLocale(NULL) Pass | ");
           } else {
               LOG.info("2nd setLocale(NULL) Fail");
               pass = false;
               sbReport.append("2nd setLocale(NULL) Fail ; expecting enc ["+ defaultEnc +"] , found ["+ afterSetEncoding +"] | ");
           }
       } else {
           LOG.info("2nd set via Locale (Shift_Jis) did not set the encoding. Fail |");
           pass = false;
           sbReport.append("2nd set via Locale (Shift_Jis) Fail ; expecting enc [Shift_Jis] , found ["+ afterSetEncoding + "] | ");
       }

       response.reset();
       
       //3rd case
       LOG.info("Test testSetLocale, 3rd");
       defaultEnc = response.getCharacterEncoding(); 
       response.setCharacterEncoding("UTF-8");                  //set via setCharacterEncoding
       afterSetEncoding = response.getCharacterEncoding();
       
       if ("UTF-8".equalsIgnoreCase(afterSetEncoding)) {
           response.setLocale(null);                    //setLocale(null) after setCharacterEncoding does not have any encoding effect
           afterSetEncoding = response.getCharacterEncoding();

           if ("UTF-8".equalsIgnoreCase(afterSetEncoding)) {    //enc does not change
               sbReport.append("3rd Set via Locale Pass | "); 
           }
           else {
               LOG.info("3rd set via Locale (null) Fail |");
               pass = false;
               sbReport.append("3rd set via Locale(null) Fail ; expecting enc [UTF-8] , found ["+ afterSetEncoding + "] | ");
           }
       }
       else {
           LOG.info("3rd Set with UTF-8 Fail |");
           pass = false;
           sbReport.append("3rd Set with UTF-8 Fail ; expecting [UTF-8] , found ["+ afterSetEncoding + "] | ");
       }
           
       if (pass)
           sbReport.append(" --]]]. Result [PASS]");
       else
           sbReport.append(" --]]]. Result [FAIL]");
      
       response.setHeader("TestResult", sbReport.toString());  
       
       LOG.info("Test testSetLocale END");
       
   }
    
   /*
    * Test response.setCharacterEncoding("invalid-encoding"), then call getWriter()
    * Verify the response.getCharacterEncoding() returns "invalid-encoding" but does not throw exeception 
    * until response.getWriter() is called then UnsupportedEncodingException is thrown.
    */
   private void testInvalidEncoding(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
       LOG.info("Test testInvalidEncoding Start");
       
       StringBuilder sbReport = new StringBuilder("TEST testInvalidEncoding . Message [[[-- ");
       String afterSetEncoding;
       boolean pass = true;
       
       response.setCharacterEncoding("invalid-encoding");
       afterSetEncoding = response.getCharacterEncoding();

       if ("invalid-encoding".equalsIgnoreCase(afterSetEncoding)) {
           sbReport.append("Set with invalid encoding Pass | ");
       } else {
           pass = false;
           sbReport.append("Set with invalid encoding Fail ; expecting enc [invalid-encoding] , found ["+ afterSetEncoding +"] | ");
       }
       
       try {
           response.getWriter();
           pass = false;
           sbReport.append("getWriter() did not throw UnsupportedEncodingException Fail | ");
       }
       catch (UnsupportedEncodingException uee) {
           sbReport.append("getWriter() threw UnsupportedEncodingException Pass | ");
       }
       catch (Exception e) {
           pass = false;
           sbReport.append("getWriter() threw Exception but NOT the expected UnsupportedEncodingException Fail | ");
       }

       if (pass)
           sbReport.append(" --]]]. Result [PASS]");
       else
           sbReport.append(" --]]]. Result [FAIL]");
      
       response.setHeader("TestResult", sbReport.toString());  
       
       LOG.info("Test testInvalidEncoding END");
   }
}

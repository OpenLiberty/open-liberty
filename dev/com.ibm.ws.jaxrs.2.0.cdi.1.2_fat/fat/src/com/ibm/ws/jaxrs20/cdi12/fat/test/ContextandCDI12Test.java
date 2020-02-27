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
package com.ibm.ws.jaxrs20.cdi12.fat.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class ContextandCDI12Test extends AbstractTest {

    public static final String[] ignore_messages =  new String[] { "CWWKW1001W" , "CWWKW1002W" , "CWWKE1102W", "CWWKE1106W" , "CWWKE1107W" };    

    @Server("com.ibm.ws.jaxrs20.cdi12.fat.contextandCDI")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        appname = "contextandCDI";
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs20.cdi12.fat.contextandCDI");
//        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(ignore_messages);
    }

    @Before
    public void preTest() throws Exception {
        serverRef = server;        
        server.startServer(true);
    }

    @After
    public void afterTest() throws Exception {
        server.stopServer(ignore_messages);
        serverRef = null;
    }
    
    
    public void verifySuccess(String filterName, int messageSize) throws Exception {
        String message = filterName + "#init: servletContext.getServletContextName contextandCDI";        
        List<String> messages = serverRef.findStringsInLogs(message);
        assertEquals("Expect to get CDI init test message: " + message, messageSize, messages.size());
        message = filterName + "#filter#requestContext: servletContext.getServletContextName contextandCDI";        
        messages = serverRef.findStringsInLogs(message);
        assertEquals("Expect to get CDI request test message: " + message, messageSize, messages.size());
        if (filterName.contentEquals("CDIFilter")) {
            message = filterName + "#init: servletContext.getServletContextName2 contextandCDI";        
            messages = serverRef.findStringsInLogs(message);
            assertEquals("Expect to get CDI init test message: " + message, messageSize, messages.size());
            message = filterName + "#filter#requestContext: servletContext.getServletContextName2 contextandCDI";        
            messages = serverRef.findStringsInLogs(message);
            assertEquals("Expect to get CDI request test message: " + message, messageSize, messages.size()); 
        }
    }
    
    public void verifySuccess2(String resourceName, int messageSize) throws Exception {        
        String message = resourceName + "#get: servletContext.getServletContextName contextandCDI";        
        List<String> messages = serverRef.findStringsInLogs(message);
        assertEquals("Expect to get resource test message: " + message, messageSize, messages.size());       
        message = resourceName + "#get: servletContext.getServletContextName2 contextandCDI";        
        messages = serverRef.findStringsInLogs(message);
        assertEquals("Expect to get resource test message: " + message, messageSize, messages.size());        
    }

    @Test
    public void testContextandCDIResource1() throws Exception {
        
        String testName = "testContextandCDIResource1";
        
        runGetMethod("/contextandCDI1/resource/" + testName, 200, "ok", true);
        verifySuccess("CDIFilter", 1);
        verifySuccess2("TestResource", 1);
        runGetMethod("/contextandCDI1/resource2/" + testName, 200, "ok", true);
        verifySuccess("CDIFilter2", 2);
        verifySuccess2("TestResource2", 1);
        //@Dependent scope providers call @PostConstruct method to be called twice.  https://github.com/OpenLiberty/open-liberty/issues/10633 
        //runGetMethod("/contextandCDI1/resource3", 200, "ok", true);
        //verifySuccess("CDIFilter3");
        //verifySuccess2("TestResource3", 1);
        runGetMethod("/contextandCDI1/resource4/" + testName, 200, "ok", true);
        verifySuccess("CDIFilter4", 3);
        verifySuccess2("TestResource4", 1);
    }
    
    @Test
    public void testContextandCDIResource2() throws Exception {
        
        String testName = "testContextandCDIResource2";
        
        runGetMethod("/contextandCDI2/resource/" + testName, 200, "ok", true);
        verifySuccess("CDIFilter", 1);
        verifySuccess2("TestResource", 1);
        runGetMethod("/contextandCDI2/resource2/" + testName, 200, "ok", true);
        // See https://github.com/OpenLiberty/open-liberty/issues/10725
        //verifySuccess("CDIFilter2", 2);
        verifySuccess2("TestResource2", 1);
        //@Dependent scope providers call @PostConstruct method to be called twice.  https://github.com/OpenLiberty/open-liberty/issues/10633 
        //runGetMethod("/contextandCDI2/resource3", 200, "ok", true);
        //verifySuccess("CDIFilter3");
        //verifySuccess2("TestResource3", 1);
        runGetMethod("/contextandCDI2/resource4/" + testName, 200, "ok", true);
        // See https://github.com/OpenLiberty/open-liberty/issues/10725
        //verifySuccess("CDIFilter4", 3);
        verifySuccess2("TestResource4", 1);
    }
    
  @Test
  public void testContextandCDIResource3() throws Exception {
      
      String testName = "testContextandCDIResource3";
      
      runGetMethod("/contextandCDI3/resource/" + testName, 200, "ok", true);
      verifySuccess("CDIFilter", 1);
      verifySuccess2("TestResource", 1);
  }
  
  @Test
  public void testContextandCDIResource4() throws Exception {
      
      String testName = "testContextandCDIResource4";
       
      runGetMethod("/contextandCDI4/resource/" + testName, 200, "ok", true);
      verifySuccess("CDIFilter", 1);
      verifySuccess2("TestResource", 1);
  }
  
  @Test
  public void testContextandCDIResource5() throws Exception {
      
      String testName = "testContextandCDIResource5";
        
      runGetMethod("/contextandCDI5/resource/" + testName, 200, "ok", true);
      verifySuccess("CDIFilter", 1);
      verifySuccess2("TestResource", 1);
  }
  
  @Test
  public void testContextandCDIResource6() throws Exception {
      
      String testName = "testContextandCDIResource6";
      
      runGetMethod("/contextandCDI6/resource/" + testName, 200, "ok", true);
      verifySuccess("CDIFilter", 1);
      verifySuccess2("TestResource", 1);
  }
  
  @Test
  public void testContextandCDIResource11() throws Exception {
      
      String testName = "testContextandCDIResource11";
             
      runGetMethod("/contextandCDI11/resource/" + testName, 200, "ok", true);
      verifySuccess("CDIFilter", 1);
      verifySuccess2("TestResource", 1);
  }
  
  @Test
  public void testContextandCDIResource12() throws Exception {
      
      String testName = "testContextandCDIResource12";
      
      runGetMethod("/contextandCDI12/resource/" + testName, 200, "ok", true);
      verifySuccess("CDIFilter", 1);
      verifySuccess2("TestResource", 1);
  }
  
  @Test
  public void testContextandCDIResource21() throws Exception {
      
      String testName = "testContextandCDIResource21";
      
      runGetMethod("/contextandCDI21/resource/" + testName, 200, "ok", true);
      verifySuccess("CDIFilter", 1);
      verifySuccess2("TestResource", 1);
  }
  
  @Test
  public void testContextandCDIResource22() throws Exception {
      
      String testName = "testContextandCDIResource22";
      
      runGetMethod("/contextandCDI22/resource/" + testName, 200, "ok", true);
      verifySuccess("CDIFilter", 1);
      verifySuccess2("TestResource", 1);
  }
  
  @Test
  public void testContextandCDIResource31() throws Exception {
      
      String testName = "testContextandCDIResource31";
      
      runGetMethod("/contextandCDI31/resource/" + testName, 200, "ok", true);
      verifySuccess("CDIFilter", 1);
      verifySuccess2("TestResource", 1);
  }
  
  @Test
  public void testContextandCDIResource32() throws Exception {
      
      String testName = "testContextandCDIResource32";
      
      runGetMethod("/contextandCDI32/resource/" + testName, 200, "ok", true);
      verifySuccess("CDIFilter", 1);
      verifySuccess2("TestResource", 1);
  }

}
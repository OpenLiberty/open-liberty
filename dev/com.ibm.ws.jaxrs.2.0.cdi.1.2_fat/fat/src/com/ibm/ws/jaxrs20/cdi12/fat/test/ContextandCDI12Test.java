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

import static org.junit.Assert.assertTrue;

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
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }
    
    
    public void verifySuccess(String filterName, int messageSize) throws Exception {
        String message = filterName + "#init: servletContext.getServletContextName contextandCDI";        
        List<String> messages = serverRef.findStringsInLogs(message);
        assertTrue("Expect to get CDI test message: " + message, messages.size() == messageSize);
        message = filterName + "#filter#requestContext: servletContext.getServletContextName contextandCDI";        
        messages = serverRef.findStringsInLogs(message);
        assertTrue("Expect to get CDI test message: " + message, messages.size() == messageSize); 
    }

    @Test
    public void testContextandCDIResource1() throws Exception {
        if (server.isStarted()) {
            server.stopServer(ignore_messages); 
        }        
        server.startServer(true);        
        runGetMethod("/contextandCDI1/resource", 200, "ok", true);
        verifySuccess("CDIFilter", 1);
        runGetMethod("/contextandCDI1/resource2", 200, "ok", true);
        verifySuccess("CDIFilter2", 2);
        //@Dependent scope providers call @PostConstruct method to be called twice.  https://github.com/OpenLiberty/open-liberty/issues/10633 
        //runGetMethod("/contextandCDI1/resource3", 200, "ok", true);
        //verifySuccess("CDIFilter3");;
        runGetMethod("/contextandCDI1/resource4", 200, "ok", true);
        verifySuccess("CDIFilter4", 3);
    }
    
    @Test
    public void testContextandCDIResource2() throws Exception {
        if (server.isStarted()) {
            server.stopServer(ignore_messages); 
        }        
        server.startServer(true);        
        runGetMethod("/contextandCDI2/resource", 200, "ok", true);
        verifySuccess("CDIFilter", 1);
        runGetMethod("/contextandCDI1/resource2", 200, "ok", true);
        verifySuccess("CDIFilter2", 2);
        //@Dependent scope providers call @PostConstruct method to be called twice.  https://github.com/OpenLiberty/open-liberty/issues/10633 
        //runGetMethod("/contextandCDI1/resource3", 200, "ok", true);
        //verifySuccess("CDIFilter3");;
        runGetMethod("/contextandCDI1/resource4", 200, "ok", true);
        verifySuccess("CDIFilter4", 3);
    }
    
  @Test
  public void testContextandCDIResource3() throws Exception {
      if (server.isStarted()) {
          server.stopServer(ignore_messages); 
      }        
      server.startServer(true);        
      runGetMethod("/contextandCDI3/resource", 200, "ok", true);
      verifySuccess("CDIFilter", 1);
  }
  
  @Test
  public void testContextandCDIResource4() throws Exception {
      if (server.isStarted()) {
          server.stopServer(ignore_messages); 
      }        
      server.startServer(true);        
      runGetMethod("/contextandCDI4/resource", 200, "ok", true);
      verifySuccess("CDIFilter", 1);
  }
  
  @Test
  public void testContextandCDIResource5() throws Exception {
      if (server.isStarted()) {
          server.stopServer(ignore_messages); 
      }        
      server.startServer(true);        
      runGetMethod("/contextandCDI5/resource", 200, "ok", true);
      verifySuccess("CDIFilter", 1);
  }
  
  @Test
  public void testContextandCDIResource6() throws Exception {
      if (server.isStarted()) {
          server.stopServer(ignore_messages); 
      }        
      server.startServer(true);        
      runGetMethod("/contextandCDI6/resource", 200, "ok", true);
      verifySuccess("CDIFilter", 1);
  }
  
  @Test
  public void testContextandCDIResource11() throws Exception {
      if (server.isStarted()) {
          server.stopServer(ignore_messages); 
      }        
      server.startServer(true);        
      runGetMethod("/contextandCDI11/resource", 200, "ok", true);
      verifySuccess("CDIFilter", 1);
  }
  
  @Test
  public void testContextandCDIResource12() throws Exception {
      if (server.isStarted()) {
          server.stopServer(ignore_messages); 
      }        
      server.startServer(true);        
      runGetMethod("/contextandCDI12/resource", 200, "ok", true);
      verifySuccess("CDIFilter", 1);
  }
  
  @Test
  public void testContextandCDIResource21() throws Exception {
      if (server.isStarted()) {
          server.stopServer(ignore_messages); 
      }        
      server.startServer(true);        
      runGetMethod("/contextandCDI21/resource", 200, "ok", true);
      verifySuccess("CDIFilter", 1);
  }
  
  @Test
  public void testContextandCDIResource22() throws Exception {
      if (server.isStarted()) {
          server.stopServer(ignore_messages); 
      }        
      server.startServer(true);        
      runGetMethod("/contextandCDI22/resource", 200, "ok", true);
      verifySuccess("CDIFilter", 1);
  }
  
  @Test
  public void testContextandCDIResource31() throws Exception {
      if (server.isStarted()) {
          server.stopServer(ignore_messages); 
      }        
      server.startServer(true);        
      runGetMethod("/contextandCDI31/resource", 200, "ok", true);
      verifySuccess("CDIFilter", 1);
  }
  
  @Test
  public void testContextandCDIResource32() throws Exception {
      if (server.isStarted()) {
          server.stopServer(ignore_messages); 
      }        
      server.startServer(true);        
      runGetMethod("/contextandCDI32/resource", 200, "ok", true);
      verifySuccess("CDIFilter", 1);
  }

}
/* ============================================================================
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 * ============================================================================
 */
package com.ibm.ws.messaging.open_comms.fat;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import com.ibm.websphere.simplicity.ShrinkHelper;
import static org.junit.Assert.assertNotNull;

public class FATBase extends FATServletClient {
  public static LibertyServer server_ = null;
  public static LibertyServer client_ = null;

  protected static boolean setServerProps(LibertyServer server,String[] ... args) {
    boolean successFlag = true;
    Util.TRACE_ENTRY();
    try {
      java.util.Properties bsp = new java.util.Properties();
      String fileName = server.getServerRoot()+"/bootstrap.properties";
      try (java.io.InputStream input = new java.io.FileInputStream(fileName)) {
        Util.CODEPATH();
        bsp.load(input);
      } catch (java.io.FileNotFoundException e) {
        // silently ignore if there isn't a file already
        Util.CODEPATH();
      }
      for (String[] setting:args) {
        if (1==setting.length) {
          Util.TRACE("Removing "+setting[0]);
          bsp.remove(setting[0]);
        } else if (2<=setting.length && null!=setting[1]) {
          Util.TRACE("Setting "+setting[0]+"="+setting[1]);
          bsp.put(setting[0],setting[1]);
        }
      }
      try (java.io.OutputStream output = new java.io.FileOutputStream(fileName)) {
        Util.CODEPATH();
        bsp.store(output,null);
      }
    } catch (Exception e) {
      Util.LOG(e);
      successFlag = false;
    } finally {
      Util.TRACE_EXIT(successFlag);
    }
    return successFlag;
  }

  protected static void setup() throws Exception {
    Util.TRACE_ENTRY("server_="+server_+",client_="+client_);

    setServerProps(server_,new String[]{"fat.test.debug",System.getProperty("fat.test.debug")});
    setServerProps(client_,new String[]{"fat.test.debug",System.getProperty("fat.test.debug")});

    // Exposes tracing package used in servlet; this is global to the liberty install and not specific to the LibertyServer
    client_.copyFileToLibertyInstallRoot("lib/features","testjmsinternals-1.0.mf");
    Util.CODEPATH();

    server_.startServer();
    Util.CODEPATH();

    String message = server_.waitForStringInLog("CWWKF0011I:.*", server_.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the server start info message in the message log", message);

    message = server_.waitForStringInLog("CWWKO0219I:.*InboundJmsCommsEndpoint.*", server_.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the JMS port ready message in the message log", message);

    message = server_.waitForStringInLog("CWWKO0219I:.*InboundJmsCommsEndpoint-ssl.*", server_.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the JMS SSL port ready message in the message log", message);

    client_.startServer();
    Util.CODEPATH();

    message = client_.waitForStringInLog("CWWKF0011I:.*", client_.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the server start info message in the message log", message);

    ShrinkHelper.exportDropinAppToServer(client_
                                        ,ShrinkHelper.buildDefaultAppFromPath("CommsLP",null,new String[]{})
                                         .addPackages(false
                                                     ,org.jboss.shrinkwrap.api.Filters.include(CommsLPServlet.class,Util.class)
                                                     ,"com.ibm.ws.messaging.open_comms.fat"
                                                     )
                                        );
    Util.TRACE_EXIT();
  }

  protected static void cleanup() throws Exception {
    Util.TRACE_ENTRY("server_="+server_+",client_="+client_);
    Exception e = null;
    try {
      client_.stopServer();
    } catch (Exception ce) {
      Util.TRACE("Exception caught whilst stopping client Liberty server.");
      e = ce;
    }
    try {
      server_.stopServer();
    } catch (Exception se) {
      Util.TRACE("Exception caught whilst stopping server Liberty server.");
      if (null==e) e = se;
    }
    if (null!=e) {
      Util.LOG(e);
      Util.TRACE_EXIT();
      throw e;
    }

    Util.CODEPATH();
    // be a good citizen and clean-up our global change
    client_.deleteFileFromLibertyInstallRoot("lib/features/testjmsinternals-1.0.mf");

    Util.TRACE_EXIT();
  }
}

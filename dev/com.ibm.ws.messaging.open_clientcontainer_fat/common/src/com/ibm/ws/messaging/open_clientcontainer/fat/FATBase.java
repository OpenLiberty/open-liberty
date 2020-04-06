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
package com.ibm.ws.messaging.open_clientcontainer.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyServer;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;


public class FATBase {
  protected static EnterpriseArchive ear_;
  protected static LibertyClient client_;
  protected static LibertyServer server_;

  protected static boolean setClientProps(String[] ... args) {
    boolean successFlag = true;
    Util.TRACE_ENTRY();
    try {
      java.util.Properties bsp = new java.util.Properties();
      String fileName = client_.getClientRoot()+"/bootstrap.properties";
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

  protected void runTest(String ... fn) throws Exception {
    String testName;
    if (null==fn||0==fn.length) {
     testName = Util.getCaller().getMethodName();
    } else {
      testName = fn[0];
    }
    try {
      Util.TRACE_ENTRY(testName);

      // if running individual tests then let the client know, else it has already been run and we're just reaping results
      if (null!=System.getProperty("fat.test.method.name")) {
        Util.CODEPATH();
        if (!setClientProps(new String[]{"fat.test.method.name",testName}
                           ,new String[]{"fat.test.debug",System.getProperty("fat.test.debug")}
                           )
           ) {
          fail("Unable to set test property.");
        }
        Util.CODEPATH();
        client_.startClient();
      }

      Util.CODEPATH();
      String lookForString = "Test '"+testName+"' passed.";
      java.util.List<String> strings = client_.findStringsInCopiedLogs(lookForString);
      Util.TRACE(testName+" - Found in logs: " + strings);
      if (null==strings||1!=strings.size()) {
        // instead of just saying "didn't find the passed message" report something about the failure, if available.
        lookForString = "Test '"+testName+"' failed";
        strings = client_.findStringsInCopiedLogs(lookForString);
        if (null==strings||1>strings.size())  {
          fail("Test '"+testName+"' does not appear to have run.");
        } else {
          fail(strings.get(0).substring(strings.get(0).indexOf("Test '")));
        }
      }
    } finally {
      Util.TRACE_EXIT(testName);
    }
  }

  protected static void deployApplication(String appName) throws Exception {
    try {
      Util.TRACE_ENTRY();
      JavaArchive jar = ShrinkHelper.buildJavaArchive(appName+"Client.jar","com.ibm.ws.messaging.open_clientcontainer.fat.*");
      Util.CODEPATH();
      ear_ = ShrinkWrap.create(EnterpriseArchive.class,appName+".ear");
      Util.CODEPATH();
      ear_.addAsModule(jar);
      Util.CODEPATH();
      ShrinkHelper.addDirectory(ear_,"test-applications/"+appName+".ear/resources");
      Util.CODEPATH();
      ShrinkHelper.exportToClient(client_,"apps",ear_);
    } finally {
      Util.TRACE_EXIT();
    }
  }

  protected static void start() throws Exception {
    try {
      Util.TRACE_ENTRY();
      ProgramOutput po = server_.startServer();
      assertEquals("server did not start correctly", 0, po.getReturnCode());

      if (null==System.getProperty("fat.test.method.name")) {
        Util.CODEPATH();
        setClientProps(new String[]{"fat.test.debug",System.getProperty("fat.test.debug")});
        Util.CODEPATH();
        client_.startClient();
      }
    } finally {
      Util.TRACE_EXIT();
    }
  }
}

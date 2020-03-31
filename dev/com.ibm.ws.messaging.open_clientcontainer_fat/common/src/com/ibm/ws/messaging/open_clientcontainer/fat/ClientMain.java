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

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface ClientTest { }

public class ClientMain {
  protected final int     WAIT_TIME = 8000;

  // Writes marker noting test's success. This is used by the JUnit driver to determine the outcome.
  protected void reportSuccess() {
    StackTraceElement e = Util.getCaller();
    Util.ALWAYS("Test '" + e.getMethodName() + "' passed.");
	}

  // Writes an informative message that a test failed.
  protected void reportFailure() {
    StackTraceElement e = Util.getCaller();
    Util.ALWAYS("Test '" + e.getMethodName() + "' failed. (" + e.getFileName() + ":" + e.getLineNumber() + ")");
	}

  protected void reportFailure(String msg) {
    StackTraceElement e = Util.getCaller();
    Util.ALWAYS("Test '" + e.getMethodName() + "' failed. (" + e.getFileName() + ":" + e.getLineNumber() + ") " + msg);
	}

  // overridden in derived classes to do preparatory work
  protected void setup() throws Exception {}

  // invokes all methods starting with "test"
  public void run() {
    Util.TRACE_ENTRY();
    String className = this.getClass().getCanonicalName();
    String methodName = "";
    String testToRun = System.getProperty("fat.test.method.name","");

    Util.TRACE("fat.test.method.name="+testToRun);

    try {
      setup();
    } catch (Exception e) {
      Util.LOG("FAT0001E: Client setup failed."+Util.LS,e);
      Util.TRACE_EXIT();
      return;
    }
    Util.CODEPATH();
    for (Method m : this.getClass().getDeclaredMethods()) {
      methodName = m.getName();
      boolean testMethod = m.isAnnotationPresent(ClientTest.class);
      Util.TRACE("methodName="+methodName+",isAnnotationPresent(ClientTest.class)="+testMethod);
      if (true==testMethod&&(0==testToRun.length()||methodName.equals(testToRun))) {
        try {
          // trace entry on behalf of the called method
          Util.getLogger().entering(className,methodName);
          m.invoke(this,null);
        } catch (Throwable t) {
          if (t instanceof java.lang.reflect.InvocationTargetException) t = t.getCause();
          Util.LOG("Test '"+methodName+"' failed with an exception: ",t);
        }
        finally {
          // trace entry on behalf of the called method
          Util.getLogger().exiting(className,methodName);
        }
        if (0!=testToRun.length()) break; // no point continuing unless we're running multiple methods
      }
    }
    Util.TRACE_EXIT();
  }
}

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

import java.util.logging.Level;
import java.util.logging.Logger;

public class Util {
  public static final String    LS = System.lineSeparator();
  protected static Logger       logger_ = Logger.getLogger("FAT");
  static {
    Level l;
    String p = System.getProperty("fat.test.debug","INFO").toUpperCase();;
    // 3 settings: no output, default output, trace output
    if (!"INFO".equals(p)&&!"OFF".equals(p)) p = "FINEST";
    try {
      l = Level.parse(p);
    } catch (IllegalArgumentException e) {
      l = Level.INFO;
    }
    logger_.setLevel(l);
  }

  public static void setLevel(Level l) {
    logger_.setLevel(l);                  //  Requires java.util.logging.LoggingPermission
  }

  public static Logger getLogger() {
    return logger_;
  }

  private static StackTraceElement getCaller(int tiers)
  {
    StackTraceElement[] elements = Thread.currentThread().getStackTrace();
    for (int i = 1; i < elements.length; ++i)
    {
      if (elements[i].getMethodName().equals("getCaller")&&elements.length>i+tiers+1)
      {
        return elements[i + tiers + 1];
      }
    }
    if (elements.length<2+tiers)
    {
      // If it isn't found just return the 3rd element
      return elements[2 + tiers];
    }
    else
    {
      return new StackTraceElement("<unknown>","<unknown>","<unknown>",0);
    }
  }

  // This variant returns the frame for "getCaller"+2 - +2 so "getCaller" makes sense in the place it is used
  public static StackTraceElement getCaller()
  {
    // Not calling other getCaller method to avoid two getCaller methods on the stack
    StackTraceElement[] elements = Thread.currentThread().getStackTrace();
    for (int i = 1; i < elements.length; ++i)
    {
      if (elements[i].getMethodName().equals("getCaller")&&elements.length>i+2)
      {
        return elements[i + 2]; // +2 for correct meaning where getCaller is used
      }
    }
    if (4<elements.length)
    {
      // If it isn't found just return the 4th element
      return elements[4];
    }
    else
    {
      return new StackTraceElement("<unknown>","<unknown>","<unknown>",0);
    }
  }
  
  // tracing/logging helper methods so we can simplify and keep consistent
  // only bother to test level here as we're getting a stack trace for the method name which we can avoid if it won't be logged
  public static void TRACE_ENTRY() {
    if (logger_.isLoggable(Level.FINEST)) {
      StackTraceElement e = getCaller();
      logger_.entering(e.getClassName(),e.getMethodName());
    }
  }

  public static void TRACE_ENTRY(Object obj) {
    if (logger_.isLoggable(Level.FINEST)) {
      StackTraceElement e = getCaller();
      logger_.entering(e.getClassName(),e.getMethodName(),obj);
    }
  }

  public static void TRACE_EXIT() {
    if (logger_.isLoggable(Level.FINEST)) {
      StackTraceElement e = getCaller();
      logger_.exiting(e.getClassName(),e.getMethodName());
    }
  }

  public static void TRACE_EXIT(Object obj) {
    if (logger_.isLoggable(Level.FINEST)) {
      StackTraceElement e = getCaller();
      logger_.exiting(e.getClassName(),e.getMethodName(),obj);
    }
  }

  public static void LOG_STACK() {
    if (logger_.isLoggable(Level.INFO)) {
      LOG(gatherStack());
    }
  }

  public static void TRACE_STACK() {
    if (logger_.isLoggable(Level.FINEST)) {
      TRACE(gatherStack());
    }
  }

  private static String getThrowableStack(Throwable t) {
    String textStack = "";
    while (null!=t) {
      textStack += t.getClass().getName();
      if (null!=t.getMessage()) textStack += ": "+t.getMessage();
      for (StackTraceElement elem:t.getStackTrace()) textStack = textStack+LS+"\tat "+elem.toString();
      t = t.getCause();
      if (null!=t) textStack += LS+"Caused by: ";
    }
    return textStack;
  }

  private static StackTraceElement[] gatherStack() {
    StackTraceElement[] raw = Thread.currentThread().getStackTrace();
    return java.util.Arrays.copyOfRange(raw,3,raw.length);  // skip getStackTrace,gatherStack,LOG_STACK/TRACE_STACK
  }

  public static void ALWAYS(Object ... args) { doAlways(args); }
  public static void LOG(Object ...  args) { if (logger_.isLoggable(Level.INFO)) doLog(args); }
  public static void TRACE(Object ... args) { if (logger_.isLoggable(Level.FINEST)) doTrace(args); }
  public static void CODEPATH() { if (logger_.isLoggable(Level.FINEST)) doTrace("CODEPATH"); }

  private static void doAlways(Object ... args) {
    Level keep = logger_.getLevel();
    boolean changed = false;
    if (!logger_.isLoggable(Level.INFO)) {
      logger_.setLevel(Level.INFO);
      changed = true;
    }
    StackTraceElement frame = Util.getCaller(2);
    logger_.logp(Level.INFO,frame.getClassName(),frame.getMethodName(),assembleMsg(args));
    if (changed) logger_.setLevel(keep);
  }

  private static void doLog(Object ... args) {
    StackTraceElement e = Util.getCaller(2);
    logger_.logp(Level.INFO,e.getClassName(),e.getMethodName(),assembleMsg(args));
  }

  private static void doTrace(Object ... args) {
    StackTraceElement e = Util.getCaller(2);
    logger_.logp(Level.FINEST,e.getClassName(),e.getMethodName(),"["+e.getFileName()+":"+e.getLineNumber()+"] "+assembleMsg(args));
  }

  private static String assembleMsg(Object ... args) {
    String msg = "";
    for (Object o:args) {
      if (o instanceof Throwable) {
        if (null!=((Throwable)o).getMessage()) msg+=((Throwable)o).getMessage()+LS;
        msg+=getThrowableStack((Throwable)o);
      } else if (o instanceof StackTraceElement[]) {
        for (StackTraceElement elem:(StackTraceElement[])o) msg+=msg+LS+"\tat "+elem.toString();
      } else if (null==o) {
        msg+="null";
      } else {
        msg+=o.toString();
      }
    }
    return msg;
  }
}

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
package web;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import java.util.logging.Level;
import java.util.logging.Logger;
import componenttest.topology.impl.LibertyServer;

public class MessagingFATServletUtilities {
  public final String       LS = System.lineSeparator();
  protected Logger          logger = null;

  // Since com.ibm.websphere.ras.Tr doesn't provide un-translated message logging apart from at debug and event levels we're 
  // using a Logger directly, but we use the common trace specification to determine if debug messages should be displayed or not.
  protected TraceComponent  tc = null;

  public MessagingFATServletUtilities(Class<?> c) {
    tc = Tr.register(c,"FAT");
    logger = Logger.getLogger(c.getName());
    if (tc.isDebugEnabled()) {
      logger.setLevel(Level.FINER);       // Requires java.util.logging.LoggingPermission
    } else if (tc.isInfoEnabled()) {
      logger.setLevel(Level.INFO);        // Requires java.util.logging.LoggingPermission
    } else {
      logger.setLevel(Level.OFF);         // Requires java.util.logging.LoggingPermission
    }
  }

  // tracing/logging helper methods so we can simplify and keep consistent
  
  public void ENTRY() {
    if (logger.isLoggable(Level.FINER)) {
      StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
      logger.entering(frame.getClassName(),frame.getMethodName());
    }
  }

  public void ENTRY(Object obj) {
    if (logger.isLoggable(Level.FINER)) {
      StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
      logger.entering(frame.getClassName(),frame.getMethodName(),obj);
    }
  }

  public void EXIT() {
    if (logger.isLoggable(Level.FINER)) {
      StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
      logger.exiting(frame.getClassName(),frame.getMethodName());
    }
  }

  public void EXIT(Object obj) {
    if (logger.isLoggable(Level.FINER)) {
      StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
      logger.exiting(frame.getClassName(),frame.getMethodName(),obj);
    }
  }

  public void THROW_EXIT(Exception e) throws Exception {
    if (logger.isLoggable(Level.FINER)) {
      StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
      logger.exiting(frame.getClassName(),frame.getMethodName(),e);
    }
    throw e;
  }

  public void LOG_STACK() {
    if (logger.isLoggable(Level.INFO)) {
      outputStack(Level.INFO,gatherStack(),"");
    }
  }

  public void LOG_STACK(Throwable t) {
    if (logger.isLoggable(Level.INFO)) {
      outputThrowableStack(Level.INFO,t);
    }
  }

  public void LOG_STACK(StackTraceElement[] stack) {
    if (logger.isLoggable(Level.INFO)) {
      outputStack(Level.INFO,stack,"");
    }
  }

  public void TRACE_STACK() {
    if (logger.isLoggable(Level.FINER)) {
      outputStack(Level.FINER,gatherStack(),"");
    }
  }

  public void TRACE_STACK(Throwable t) {
    if (logger.isLoggable(Level.FINER)) {
      outputThrowableStack(Level.FINEST,t);
    }
  }

  public void TRACE_STACK(StackTraceElement[] stack) {
    if (logger.isLoggable(Level.FINER)) {
      outputStack(Level.FINER,stack,"");
    }
  }

  private void outputThrowableStack(Level level,Throwable t) {
    String textStack = "";
    while (null!=t) {
      textStack += t.getClass().getName();
      if (null!=t.getMessage()) textStack += ": "+t.getMessage();
      for (StackTraceElement elem:t.getStackTrace()) textStack = textStack+LS+"\tat "+elem.toString();
      t = t.getCause();
      if (null!=t) textStack += LS+"Caused by: ";
    }
    StackTraceElement frame = Thread.currentThread().getStackTrace()[3]; // skip getStackTrace,outputThrowableStack,LOG_/TRACE_STACK
    logger.logp(level,frame.getClassName(),frame.getMethodName(),textStack);
  }

  private StackTraceElement[] gatherStack() {
    StackTraceElement[] raw = Thread.currentThread().getStackTrace();
    return java.util.Arrays.copyOfRange(raw,3,raw.length);  // skip getStackTrace,gatherStack,LOG_STACK/TRACE_STACK
  }

  private void outputStack(Level level,StackTraceElement[] stack,String textStack) {
    for (StackTraceElement elem:stack) textStack = textStack+LS+"\tat "+elem.toString();
    StackTraceElement frame = Thread.currentThread().getStackTrace()[3];  // skip getStackTrace,outputStack,LOG_STACK/TRACE_STACK
    logger.logp(level,frame.getClassName(),frame.getMethodName(),textStack);
  }

  public void ALWAYS(boolean arg) { doAlways(""+arg); }
  public void ALWAYS(char arg) { doAlways(""+arg); }
  public void ALWAYS(char[] arg) { doAlways(""+arg); }
  public void ALWAYS(double arg) { doAlways(""+arg); }
  public void ALWAYS(float arg) { doAlways(""+arg); }
  public void ALWAYS(int arg) { doAlways(""+arg); }
  public void ALWAYS(long arg) { doAlways(""+arg); }
  public void ALWAYS(Object o) { doAlways((null==o?"null":o.toString())); }
  public void ALWAYS(String arg) { doAlways(arg); }

  public void LOG(boolean arg) { if (logger.isLoggable(Level.INFO)) doLog(""+arg); }
  public void LOG(char arg) { if (logger.isLoggable(Level.INFO)) doLog(""+arg); }
  public void LOG(char[] arg) { if (logger.isLoggable(Level.INFO)) doLog(""+arg); }
  public void LOG(double arg) { if (logger.isLoggable(Level.INFO)) doLog(""+arg); }
  public void LOG(float arg) { if (logger.isLoggable(Level.INFO)) doLog(""+arg); }
  public void LOG(int arg) { if (logger.isLoggable(Level.INFO)) doLog(""+arg); }
  public void LOG(long arg) { if (logger.isLoggable(Level.INFO)) doLog(""+arg); }
  public void LOG(Object o) { if (logger.isLoggable(Level.INFO)) doLog((null==o?"null":o.toString())); }
  public void LOG(String arg) { if (logger.isLoggable(Level.INFO)) doLog(arg); }

  public void CODEPATH() { if (logger.isLoggable(Level.FINER)) doTrace("CODEPATH"); }
  public void TRACE(boolean arg) { if (logger.isLoggable(Level.FINER)) doTrace(""+arg); }
  public void TRACE(char arg) { if (logger.isLoggable(Level.FINER)) doTrace(""+arg); }
  public void TRACE(char[] arg) { if (logger.isLoggable(Level.FINER)) doTrace(""+arg); }
  public void TRACE(double arg) { if (logger.isLoggable(Level.FINER)) doTrace(""+arg); }
  public void TRACE(float arg) { if (logger.isLoggable(Level.FINER)) doTrace(""+arg); }
  public void TRACE(int arg) { if (logger.isLoggable(Level.FINER)) doTrace(""+arg); }
  public void TRACE(long arg) { if (logger.isLoggable(Level.FINER)) doTrace(""+arg); }
  public void TRACE(Object o) { if (logger.isLoggable(Level.FINER)) doTrace((null==o?"null":o.toString())); }
  public void TRACE(String arg) { if (logger.isLoggable(Level.FINER)) doTrace(arg); }

  protected void doAlways(String msg) {
    Level keep = logger.getLevel();
    boolean changed = false;
    if (!logger.isLoggable(Level.INFO)) {
      logger.setLevel(Level.INFO);
      changed = true;
    }
    StackTraceElement frame = Thread.currentThread().getStackTrace()[3];
    logger.logp(Level.INFO,frame.getClassName(),frame.getMethodName(),msg);
    if (changed) logger.setLevel(keep);
  }

  protected void doLog(String msg) {
    StackTraceElement frame = Thread.currentThread().getStackTrace()[3];
    logger.logp(Level.INFO,frame.getClassName(),frame.getMethodName(),msg);
  }

  protected void doTrace(String msg) {
    StackTraceElement frame = Thread.currentThread().getStackTrace()[3];
    logger.logp(Level.FINER,frame.getClassName(),frame.getMethodName(),"["+frame.getFileName()+":"+frame.getLineNumber()+"] "+msg);
  }

  public Logger getLogger() {
    return logger;
  }
}

/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.mfp.jmf;


import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;


/**
 * <P>JmfTr is a wrapper class for SibTr, introduced primarily to switch JMF tracing on and off
 * while higher level code (e.g. JSFormatter) is trying to format some sensible output to trace.
 * If JMF trace is not switched off during this activity then all the nice formatted trace is interspersed
 * with humungous amounts of jmf trace and the whole thing becomes unreadable.  The switching off
 * of JMF trace needs to be done for only the thread that doing formatting.  To turn it off
 * globally would risk losing useful information on other threads.  We don't have a way of communicating
 * this state via arguments in the calling stack, so a ThreadLocal approach has been used.
 * </P>
 * <P>
 * Each thread maintains state to decide whether JMF tracing should be done or not.  That state is in the form of
 * a non-negative integer.  An integer is used, rather than a boolean, in order that any method may request trace
 * to be suspended, regardless of whether it has already been supressed by a call higher up the stack.
 * </P>
 * <P>
 *  To cater for one facet of the possibility that the turning on and off may not be done properly for a given method, the working
 * suppression count has a lower limit of zero.  The true count is preserved so that test code can detect whether there
 * is an assymetry between requests to suppress and reenable tracing.
 * </P>
 **/
public class JmfTr {


  private static ThreadLocal jmfThreadLocal;

  static {
    jmfThreadLocal = new ThreadLocal() {


        /* ---------------------------------------------------------------------- */
        /* initialValue method
        /* ---------------------------------------------------------------------- */
        /**
         * Creates an instance of DiagnosticData
         *
         * @see ThreadLocal#initialValue()
         * @return a new instance of the DiagnosticData
         */
        public Object initialValue()
        {
          JmfThreadData data = new JmfThreadData();
          data.clear();
          return data;
        }
      };
  }


  private static class JmfThreadData
  {
    // maintain true count for tracking problems in use
    private int trueCount = 0;
    // lower limited at 0
    private int workingCount = 0;
    /**
     *
     */
    public void clear() {
      trueCount = 0;
      workingCount = 0;
    }

    /**
     * @return Returns the suppressTrace.
     */
    public boolean isTracing() {
      return workingCount == 0;
    }

    public void setTracing(boolean tracing) {
      if(tracing) {
        trueCount--;
        workingCount--;
        workingCount = (workingCount < 0) ? 0 : workingCount;
      } else {
        trueCount++;
        workingCount++;
      }
    }
    /**
     * @return Returns the trueCount.
     */
    public int getTrueCount() {
      return trueCount;
    }
  }

  public static boolean isTracing()
  {
    return ((JmfThreadData)jmfThreadLocal.get()).isTracing();
  }

  public static int traceSuppressionCount()
  {
    return ((JmfThreadData)jmfThreadLocal.get()).getTrueCount();
  }

  /**
   * Call this method with argument <em>trace</em> set to false to suppress JMF trace. Be sure
   * to cause a matching re-enablement call on the way out of the method.  Note that a call with
   * value true may still not immediately re-enable trace, since there may be a higher level suppression call in scope.
   * @param trace call this method with argument equal to false to suppress trace,  true to enable it.
   */
  public static void setTracing(boolean trace)
  {
    ((JmfThreadData)jmfThreadLocal.get()).setTracing(trace);
  }


  public static TraceComponent register(Class aClass, String group, String resourceBundleName) {
    TraceComponent tc = SibTr.register(aClass, group, resourceBundleName);
    return tc;
  }

  public static final void debug(TraceComponent tc, String msg) {
    SibTr.debug(null, tc, msg);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#getMEName(java.lang.Object)
   */
  public static String getMEName(Object o) {
    return SibTr.getMEName(o);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#push(java.lang.Object)
   */
  public static void push(Object jsme) {
    SibTr.push(jsme);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#pop()
   */
  public static void pop() {
    SibTr.pop();
  }


  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#audit(TraceComponent, java.lang.String)
   */
  public static void audit(TraceComponent tc, String msgKey) {
    SibTr.audit(tc, msgKey);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#audit(TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void audit(TraceComponent tc, String msgKey, Object objs) {
     SibTr.audit(tc, msgKey, objs);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#debug(com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void debug(TraceComponent tc, String msg, Object objs) {
    if(isTracing()) SibTr.debug(tc, msg, objs);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#debug(java.lang.Object, com.ibm.websphere.ras.TraceComponent, java.lang.String)
   */
  public static void debug(Object o, TraceComponent tc, String msg) {
    if(isTracing()) SibTr.debug(o, tc, msg);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#debug(java.lang.Object, com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void debug(Object o, TraceComponent tc, String msg, Object objs) {
    if(isTracing()) SibTr.debug(o, tc, msg, objs);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#dump(com.ibm.websphere.ras.TraceComponent, java.lang.String)
   */
  public static void dump(TraceComponent tc, String msg) {
    if(isTracing()) SibTr.dump(tc, msg);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#dump(com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void dump(TraceComponent tc, String msg, Object objs) {
    if(isTracing()) SibTr.debug(tc, msg, objs);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#dump(java.lang.Object, com.ibm.websphere.ras.TraceComponent, java.lang.String)
   */
  public static void dump(Object o, TraceComponent tc, String msg) {
    if(isTracing()) SibTr.dump(o, tc, msg);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#dump(java.lang.Object, com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void dump(Object o, TraceComponent tc, String msg, Object objs) {
    if(isTracing()) SibTr.dump(o, tc, msg, objs);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#error(com.ibm.websphere.ras.TraceComponent, java.lang.String)
   */
  public static void error(TraceComponent tc, String msgKey) {
    if(isTracing()) SibTr.error(tc, msgKey);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#error(com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void error(TraceComponent tc, String msgKey, Object objs) {
    if(isTracing()) SibTr.error(tc, msgKey, objs);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#event(com.ibm.websphere.ras.TraceComponent, java.lang.String)
   */
  public static void event(TraceComponent tc, String msg) {
    if(isTracing()) SibTr.event(tc, msg);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#event(com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void event(TraceComponent tc, String msg, Object objs) {
    if(isTracing()) SibTr.event(tc, msg, objs);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#event(java.lang.Object, com.ibm.websphere.ras.TraceComponent, java.lang.String)
   */
  public static void event(Object o, TraceComponent tc, String msg) {
    if(isTracing()) SibTr.event(o, tc, msg);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#event(java.lang.Object, com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void event(Object o, TraceComponent tc, String msg, Object objs) {
    if(isTracing()) SibTr.event(o, tc, msg, objs);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#entry(com.ibm.websphere.ras.TraceComponent, java.lang.String)
   */
  public static void entry(TraceComponent tc, String methodName) {
    if(isTracing()) SibTr.entry(tc, methodName);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#entry(com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void entry(TraceComponent tc, String methodName, Object obj) {
    if(isTracing()) {
      SibTr.entry(tc, methodName, obj);
    }

  }



  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#entry(java.lang.Object, com.ibm.websphere.ras.TraceComponent, java.lang.String)
   */
  public static void entry(Object o, TraceComponent tc, String methodName) {
    if(isTracing()) SibTr.entry(o, tc, methodName);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#entry(java.lang.Object, com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void entry(Object o, TraceComponent tc, String methodName, Object obj) {
    if(isTracing()) {
       SibTr.entry(o, tc, methodName, obj);
    }

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#exit(com.ibm.websphere.ras.TraceComponent, java.lang.String)
   */
  public static void exit(TraceComponent tc, String methodName) {
    if(isTracing()) SibTr.exit(tc, methodName);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#exit(com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void exit(TraceComponent tc, String methodName, Object objs) {
    if(isTracing()) {
      SibTr.exit(tc, methodName, objs);
    }

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#exit(java.lang.Object, com.ibm.websphere.ras.TraceComponent, java.lang.String)
   */
  public static void exit(Object o, TraceComponent tc, String methodName) {
    if(isTracing()) SibTr.exit(o, tc, methodName);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#exit(java.lang.Object, com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void exit(Object o, TraceComponent tc, String methodName, Object objs) {
    if(isTracing()) SibTr.exit(o, tc, methodName, objs);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#fatal(com.ibm.websphere.ras.TraceComponent, java.lang.String)
   */
  public static void fatal(TraceComponent tc, String msgKey) {
    if(isTracing()) SibTr.fatal(tc, msgKey);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#fatal(com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void fatal(TraceComponent tc, String msgKey, Object objs) {
    if(isTracing()) SibTr.fatal(tc, msgKey, objs);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#info(com.ibm.websphere.ras.TraceComponent, java.lang.String)
   */
  public static void info(TraceComponent tc, String msgKey) {
    if(isTracing()) SibTr.info(tc, msgKey);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#info(com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void info(TraceComponent tc, String msgKey, Object objs) {
    if(isTracing()) SibTr.info(tc, msgKey, objs);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#service(com.ibm.websphere.ras.TraceComponent, java.lang.String)
   */
  public static void service(TraceComponent tc, String msgKey) {
    if(isTracing()) SibTr.service(tc, msgKey);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#service(com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void service(TraceComponent tc, String msgKey, Object objs) {
    if(isTracing()) SibTr.service(tc, msgKey,objs);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#warning(com.ibm.websphere.ras.TraceComponent, java.lang.String)
   */
  public static void warning(TraceComponent tc, String msgKey) {
    if(isTracing()) SibTr.warning(tc, msgKey);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#warning(com.ibm.websphere.ras.TraceComponent, java.lang.String, java.lang.Object)
   */
  public static void warning(TraceComponent tc, String msgKey, Object objs) {
    if(isTracing()) SibTr.warning(tc, msgKey, objs);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#bytes(com.ibm.websphere.ras.TraceComponent, byte[])
   */
  public static void bytes(TraceComponent tc, byte[] data) {
    if(isTracing()) SibTr.bytes(tc, data);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#bytes(java.lang.Object, com.ibm.websphere.ras.TraceComponent, byte[])
   */
  public static void bytes(Object o, TraceComponent tc, byte[] data) {
    if(isTracing()) SibTr.bytes(o, tc, data);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#bytes(com.ibm.websphere.ras.TraceComponent, byte[], int)
   */
  public static void bytes(TraceComponent tc, byte[] data, int start) {
    if(isTracing()) SibTr.bytes(tc, data, start);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#bytes(java.lang.Object, com.ibm.websphere.ras.TraceComponent, byte[], int)
   */
  public static void bytes(Object o, TraceComponent tc, byte[] data, int start) {
    if(isTracing()) SibTr.bytes(o, tc, data, start);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#bytes(com.ibm.websphere.ras.TraceComponent, byte[], int, int)
   */
  public static void bytes(TraceComponent tc, byte[] data, int start, int count) {
    if(isTracing()) SibTr.bytes(tc, data, start, count);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#bytes(java.lang.Object, com.ibm.websphere.ras.TraceComponent, byte[], int, int)
   */
  public static void bytes(Object o, TraceComponent tc, byte[] data, int start,
      int count) {
    if(isTracing()) SibTr.bytes(o, tc, data, start, count);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#bytes(com.ibm.websphere.ras.TraceComponent, byte[], int, int, java.lang.String)
   */
  public static void bytes(TraceComponent tc, byte[] data, int start, int count,
      String comment) {
    if(isTracing()) SibTr.bytes(tc, data, start, count, comment);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#bytes(java.lang.Object, com.ibm.websphere.ras.TraceComponent, byte[], int, int, java.lang.String)
   */
  public static void bytes(Object o, TraceComponent tc, byte[] data, int start,
      int count, String comment) {
    if(isTracing()) SibTr.bytes(o, tc, data, start, count, comment);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#formatBytes(byte[], int, int)
   */
  public static String formatBytes(byte[] data, int start, int count) {
    if(isTracing())
      return SibTr.formatBytes(data, start, count);
    else
      return "";
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#formatBytes(byte[], int, int, boolean)
   */
  public static String formatBytes(byte[] data, int start, int count,
      boolean displayCharRepresentations) {
    if(isTracing())
      return SibTr.formatBytes(data, start, count, displayCharRepresentations);
    else
      return "";
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#exception(com.ibm.websphere.ras.TraceComponent, java.lang.Exception)
   */
  public static void exception(TraceComponent tc, Exception e) {
    if(isTracing()) SibTr.exception(tc, e);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#exception(java.lang.Object, com.ibm.websphere.ras.TraceComponent, java.lang.Exception)
   */
  public static void exception(Object o, TraceComponent tc, Exception e) {
    if(isTracing()) SibTr.exception(o, tc, e);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#exception(com.ibm.websphere.ras.TraceComponent, java.lang.Throwable)
   */
  public static void exception(TraceComponent tc, Throwable t) {
    if(isTracing()) SibTr.exception(tc, t);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.ras.SibTr#exception(java.lang.Object, com.ibm.websphere.ras.TraceComponent, java.lang.Throwable)
   */
  public static void exception(Object o, TraceComponent tc, Throwable t) {
    if(isTracing()) SibTr.exception(o, tc, t);

  }


}

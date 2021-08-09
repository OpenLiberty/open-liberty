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

package com.ibm.ws.sib.matchspace.utils;
import com.ibm.websphere.ras.TraceComponent;

/**
 * @author Neil Young
 *
 * Wrapper for First Failure Data Capture.
 *
 */
public class FFDC
{
  private static final Class cclass = FFDC.class;
  private static Trace trace = TraceFactory.getTrace(FFDC.class,
                                                     MatchSpaceConstants.MSG_GROUP_UTILS);

  public static java.io.PrintWriter printWriter = null;
  // public static java.io.PrintWriter printWriter = new java.io.PrintWriter(System.out);

  private static Diagnostics diagnostics = new Diagnostics();

  /**
   * Process an exception for a static class.
   *
   * @param Class of the source object.
   * @param String the method where the FFDC is being requested from.
   * @param Throwable the cause of the exception.
   * @param String the probe identifying the source of the exception.
   */
  public static void processException(Class sourceClass
                                     ,String methodName
                                     ,Throwable throwable
                                     ,String probe
                                     )
  {
    if (TraceComponent.isAnyTracingEnabled() && trace.isEntryEnabled())
      trace.entry(cclass
                 ,"processException"
                 ,new Object[]{sourceClass
                              ,methodName
                              ,throwable
                              ,probe
                              }
                 );
    if (TraceComponent.isAnyTracingEnabled() && trace.isEventEnabled())
      trace.event(cclass,
                  "processException",
                  throwable);

    print(null
         ,sourceClass
         ,methodName
         ,throwable
         ,probe
         ,null);

    com.ibm.ws.ffdc.FFDCFilter.processException(throwable
                                               ,sourceClass.getName()+":"+methodName
                                               ,probe
                                               );
    if (TraceComponent.isAnyTracingEnabled() && trace.isEntryEnabled())
      trace.exit(cclass,"processException");

  } // processException().

  /**
   * Process an exception for a static class.
   *
   * @param Class of the source object.
   * @param String the method where the FFDC is being requested from.
   * @param Throwable the cause of the exception.
   * @param String the probe identifying the source of the exception.
   * @param Object[] obje cts to be dumped in addition to the source.
   */
  public static void processException(Class sourceClass
                                     ,String methodName
                                     ,Throwable throwable
                                     ,String probe
                                     ,Object[] objects
                                     )
  {
    if (TraceComponent.isAnyTracingEnabled() && trace.isEntryEnabled())
      trace.entry(cclass
                 ,"processException"
                 ,new Object[]{sourceClass
                              ,methodName
                              ,throwable
                              ,probe
                              ,objects
                              }
                 );
    if (TraceComponent.isAnyTracingEnabled() && trace.isEventEnabled())
      trace.event(cclass,
                  "processException",
                  throwable);

    print(null
         ,sourceClass
         ,methodName
         ,throwable
         ,probe
         ,objects);

    com.ibm.ws.ffdc.FFDCFilter.processException(throwable
                                               ,sourceClass.getName()+":"+methodName
                                               ,probe
                                               ,objects
                                               );
    if (TraceComponent.isAnyTracingEnabled() && trace.isEntryEnabled())
      trace.exit(cclass,"processException");

  } // processException().

  /**
   * Process an exception for a non static class.
   *
   * @param Object the source class requesting the FFDC.
   * @param Class of the source object.
   * @param String the method where the FFDC is being requested from.
   * @param Throwable the cause of the exception.
   * @param String the probe identifying the source of the exception.
   */
  public static void processException(Object source
                                     ,Class sourceClass
                                     ,String methodName
                                     ,Throwable throwable
                                     ,String probe
                                     )
  {
    if (TraceComponent.isAnyTracingEnabled() && trace.isEntryEnabled())
      trace.entry(cclass
                 ,"processException"
                 ,new Object[]{source
                              ,sourceClass
                              ,methodName
                              ,throwable
                              ,probe
                              }
                 );
    if (TraceComponent.isAnyTracingEnabled() && trace.isEventEnabled())
      trace.event(cclass,
                  "processException",
                  throwable);

    print(source
         ,sourceClass
         ,methodName
         ,throwable
         ,probe
         ,null
         );

    com.ibm.ws.ffdc.FFDCFilter.processException(throwable
                                                ,sourceClass.getName()+":"+methodName
                                                ,probe
                                                ,source);

    if (TraceComponent.isAnyTracingEnabled() && trace.isEntryEnabled())
      trace.exit(cclass,"processException");
  } // processException().

  /**
   * Process an exception for a non static class.
   *
   * @param Object the source class requesting the FFDC.
   * @param Class of the source object.
   * @param String the method where the FFDC is being requested from.
   * @param Throwable the cause of the exception.
   * @param String the probe identifying the source of the exception.
   * @param Object[] obje cts to be dumped in addition to the source.
   */
  public static void processException(Object source
                                     ,Class sourceClass
                                     ,String methodName
                                     ,Throwable throwable
                                     ,String probe
                                     ,Object[] objects
                                     )
  {
    if (TraceComponent.isAnyTracingEnabled() && trace.isEntryEnabled())
      trace.entry(cclass
                 ,"processException"
                 ,new Object[]{source
                              ,sourceClass
                              ,methodName
                              ,throwable
                              ,probe
                              ,objects
                              }
                 );
    if (TraceComponent.isAnyTracingEnabled() && trace.isEventEnabled())
      trace.event(cclass,
                  "processException",
                  throwable);

    print(source
          ,sourceClass
          ,methodName
          ,throwable
          ,probe
          ,objects);

    com.ibm.ws.ffdc.FFDCFilter.processException(throwable
                                                ,sourceClass.getName()+":"+methodName
                                                ,probe
                                                ,source
                                                ,objects);

    if (TraceComponent.isAnyTracingEnabled() && trace.isEntryEnabled())
      trace.exit(cclass,"processException");
  } // processException().

  /**
   * Print the FFDC to the printWriter if it is not null.
   *
   * @param Object the source class requesting the FFDC.
   * @param Class of the source object.
   * @param String the method where the FFDC is being requested from.
   * @param Throwable the cause of the exception.
   * @param String the probe identifying the source of the exception.
   * @param Object or Object[] or null object(s) to be dumped in addition to the source.
   */
  private static void print(Object source
                           ,Class sourceClass
                           ,String methodName
                           ,Throwable throwable
                           ,String probe
                           ,Object object
                           )
  {
    if (printWriter != null) {
      printWriter.println("========================FFDC Start==========================");
      if (source != null)
        printWriter.println("Source="+source);
      printWriter.println("Source="+sourceClass+":"+methodName+" probe="+probe
                         +"\n throwable="+throwable);
      print(source,printWriter);

      if (object == null);
      else if (object instanceof Object[]) {
        Object[] objects = (Object[])object;
        for (int i =0; i < objects.length; i++) {
          printWriter.println("------------------------------------------------------------");
          print(objects[i],printWriter);
        } // for...
      } else {
        printWriter.println("------------------------------------------------------------");
        print(object,printWriter);
      }
      printWriter.println();
      throwable.printStackTrace(printWriter);
      printWriter.println("=========================FFDC End===========================");
      printWriter.flush();
    } // if (printWriter != null).

  } // print().

  /**
   * Dump an Object to a java.io.PrintWriter.
   * @param Object to be dumped to printWriter
   * @param java.io.PeritWriter to dump the Object to.
   */
  static void print(Object object,java.io.PrintWriter printWriter) {
    if (object instanceof Printable) {
      ((Printable)object).print(printWriter);
    } else {
      printWriter.print(object);
    } // if (object instanceof Printable).

  } // print().

  // --------------------------------------------------------------------------------------------------
  // Inner classes.
  // --------------------------------------------------------------------------------------------------
 /**
   * This is a diagnostic module for the ObjectManager.  It will be invoked when any
   * call to FFDCFilter is made for the classes or packages for which it is registered.
   * This allows extra information to be written to the FFDC logs when an error
   * occurs.
   */

  public static class Diagnostics extends com.ibm.ws.ffdc.DiagnosticModule
  {

    // Prevent public use of the constructor.
    // This constructor should be called once only.
    private Diagnostics() {
       // We should only do this once.
      if (diagnostics == null ) {
        com.ibm.ws.ffdc.FFDC.registerDiagnosticModule(this, "com.ibm.ws.objectManager");
        com.ibm.ws.ffdc.FFDC.registerDiagnosticModule(this, "com.ibm.ws.objectManager.utils");
      }
    } // Diagnostics().

    /*
     * ffdcDump methods.
     *
     * Any method whose name starts with "ffdcDumpDefault" will get invoked for _every_
     * FFDCFilter that occurs in the registered packages/classes.  There must be at least
     * one such default method defined, or the registration with the diagnostic engine
     * will fail.
     *
     * Methods whose names start "ffdcDump" (but don't have the "Default" bit) will only
     * get invoked if something, somewhere in the depths websphere has been configured
     * to enable them.  Beats me how you configure this stuff, but we don't want any of
     * these so I guess it doesn't matter.
     */

    /**
     * Default ffdc dump routine - always invoked
     */
    public void ffdcDumpDefault(Throwable throwable
                               ,com.ibm.ws.ffdc.IncidentStream incidentStream
                               ,Object object
                               ,Object[] objects
                               ,String sourceId) {
      if (TraceComponent.isAnyTracingEnabled() && trace.isEntryEnabled())
        trace.entry(this
                    ,cclass
                    ,"ffdcDumpDefault"
                    ,new Object[] {throwable,incidentStream,object,objects,sourceId}
                    );

      // Capture default information.
      // TODO: This was removed when we moved from depending on SIB
      // TODO: If we want to regain the function we will have to code it.
      //super.captureDefaultInformation(incidentStream);
      dump(object,incidentStream);

      for (int i=0; i< objects.length; i++)
        dump(objects[i],incidentStream);

      if (TraceComponent.isAnyTracingEnabled() && trace.isEntryEnabled())
      trace.exit(this,cclass,"ffdcDumpDefault");
    } // ffdcDumpDefault().

    private void dump(Object object
                      ,com.ibm.ws.ffdc.IncidentStream incidentStream)
    {
      java.io.StringWriter stringWriter = new java.io.StringWriter();
      java.io.PrintWriter printWriter = new java.io.PrintWriter(stringWriter);
      if (object instanceof Printable) {
        try {
          ((Printable)object).print(printWriter);
        }
        catch (Exception exception)
        {
          // No FFDC Code Needed.
          printWriter.print(object);
          printWriter.println();
          printWriter.print(exception);
        } // try...
      } else {
        printWriter.print(object);
      } // if (object instanceof Printable).
      incidentStream.write(object.getClass().getName(),stringWriter.toString());
    } // dump().

  } // class Diagnostics.

} // Of class FFDC.

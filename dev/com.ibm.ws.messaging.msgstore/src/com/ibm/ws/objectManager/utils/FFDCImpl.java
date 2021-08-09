package com.ibm.ws.objectManager.utils;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * @author Andrew_Banks
 * 
 *         Wrapper for First Failure Data Capture.
 * 
 */
public class FFDCImpl
                extends FFDC {
    private static final Class cclass = FFDCImpl.class;
    private static Trace trace = Utils.traceFactory.getTrace(cclass,
                                                             UtilsConstants.MSG_GROUP_UTILS);

    private static Diagnostics diagnostics = (new FFDCImpl()).new Diagnostics();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.FFDC#processException(java.lang.Class, java.lang.String, java.lang.Throwable, java.lang.String)
     */
    public void processException(Class sourceClass,
                                 String methodName,
                                 Throwable throwable,
                                 String probe) {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(cclass,
                        "processException",
                        new Object[] { sourceClass,
                                      methodName,
                                      throwable,
                                      probe });

        print(null
              , sourceClass
              , methodName
              , throwable
              , probe
              , null
              , null);

        com.ibm.ws.ffdc.FFDCFilter.processException(throwable
                                                    , sourceClass.getName() + ":" + methodName
                                                    , probe
                        );
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(cclass, "processException");
    } // processException().               

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.FFDC#processException(java.lang.Class, java.lang.String, java.lang.Throwable, java.lang.String, java.lang.Object[])
     */
    public void processException(Class sourceClass,
                                 String methodName,
                                 Throwable throwable,
                                 String probe,
                                 Object[] objects) {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(cclass,
                        "processException",
                        new Object[] { sourceClass,
                                      methodName,
                                      throwable,
                                      probe,
                                      objects });

        print(null
              , sourceClass
              , methodName
              , throwable
              , probe
              , null
              , objects);

        com.ibm.ws.ffdc.FFDCFilter.processException(throwable
                                                    , sourceClass.getName() + ":" + methodName
                                                    , probe
                                                    , objects
                        );
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(cclass, "processException");
    } // processException().           

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.FFDC#processException(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Throwable, java.lang.String)
     */
    public void processException(Object source,
                                 Class sourceClass,
                                 String methodName,
                                 Throwable throwable,
                                 String probe) {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(cclass
                        , "processException"
                        , new Object[] { source
                                        , sourceClass
                                        , methodName
                                        , throwable
                                        , probe
                        }
                            );

        print(source
              , sourceClass
              , methodName
              , throwable
              , probe
              , null
              , null);

        com.ibm.ws.ffdc.FFDCFilter.processException(throwable,
                                                    sourceClass.getName() + ":" + methodName,
                                                    probe,
                                                    source);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(cclass, "processException");
    } // processException().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.FFDC#processException(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Throwable, java.lang.String, java.lang.Object[])
     */
    public void processException(Object source,
                                 Class sourceClass,
                                 String methodName,
                                 Throwable throwable,
                                 String probe,
                                 Object[] objects) {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(cclass,
                        "processException",
                        new Object[] { source,
                                      sourceClass,
                                      methodName,
                                      throwable,
                                      probe,
                                      objects });

        print(source
              , sourceClass
              , methodName
              , throwable
              , probe
              , null
              , objects);

        com.ibm.ws.ffdc.FFDCFilter.processException(throwable
                                                    , sourceClass.getName() + ":" + methodName
                                                    , probe
                                                    , source
                                                    , objects);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(cclass, "processException");
    } // processException().   

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.FFDC#processException(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Throwable, java.lang.String, java.lang.String,
     * java.lang.Object[])
     */
    public void processException(Object source,
                                 Class sourceClass,
                                 String methodName,
                                 Throwable throwable,
                                 String probe,
                                 String fileInformation,
                                 Object[] objects) {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(cclass,
                        "processException",
                        new Object[] { source,
                                      sourceClass,
                                      methodName,
                                      throwable,
                                      probe,
                                      objects });

        print(source
              , sourceClass
              , methodName
              , throwable
              , probe
              , fileInformation
              , objects);

        com.ibm.ws.ffdc.FFDCFilter.processException(throwable
                                                    , sourceClass.getName() + ":" + methodName
                                                    , probe
                                                    , source
                                                    , objects);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(cclass, "processException");
    } // processException().   

    // --------------------------------------------------------------------------------------------------
    // Inner classes.
    // --------------------------------------------------------------------------------------------------
    /**
     * This is a diagnostic module for the ObjectManager. It will be invoked when any
     * call to FFDCFilter is made for the classes or packages for which it is registered.
     * This allows extra information to be written to the FFDC logs when an error
     * occurs.
     */

    public class Diagnostics extends com.ibm.ws.ffdc.DiagnosticModule
    {
        // TODO We should not refer to the objectManagerPackage directly but take its name as an argument.
        // Prevent public use of the constructor.
        // This constructor should be called once only.
        private Diagnostics() {
            // We should only do this once.
            if (diagnostics == null) {
                com.ibm.ws.ffdc.FFDC.registerDiagnosticModule(this, "com.ibm.ws.objectManager");
                com.ibm.ws.ffdc.FFDC.registerDiagnosticModule(this, "com.ibm.ws.objectManager.utils");
            }
        } // Diagnostics().

        /**
         * Default ffdc dump routine - always invoked.
         * <p>
         * Any method whose name starts with "ffdcDumpDefault" will get invoked for _every_ FFDCFilter that occurs in the
         * registered packages/classes. There must be at least one such default method defined, or the registration with the
         * diagnostic engine will fail.
         * <p>
         * Methods whose names start "ffdcDump" (but don't have the "Default" bit) will only get invoked if something,
         * somewhere in the depths websphere has been configured to enable them. Beats me how you configure this stuff, but
         * we don't want any of these so I guess it doesn't matter.
         * <p>
         * FFDC default dump method for the Handler Framework. Dumps data available in the Handler Framework if an object is
         * available.
         * <p>
         * Since this is a default dump method, it will be invoked for every exception that is captured for the packages
         * listed above. Information dumped here is in addition to all other default dump methods, and the basic information
         * dumped by DiagnosticEngine.
         * <p>
         * 
         * @param throwable The exception which triggered the FFDC capture process.
         * @param incidentStream Data to be captured is written to this stream.
         * @param caller The 'this' pointer for the object which invoked the filter. The value will be null if the method
         *            which invoked the filter was static, or if the method which invoked the filter does not correspond to
         *            the DM being invoked.
         * @param objects The value of the array may be null. If not null, it contains an array of objects which the caller
         *            to the filter provided. Since the information in the array may vary depending upon the location in the
         *            code, the first index of the array may contain hints as to the content of the rest of the array.
         * @param sourceId The sourceId passed to the filter.
         */
        public void ffdcDumpDefault(Throwable throwable,
                                    com.ibm.ws.ffdc.IncidentStream incidentStream,
                                    Object caller,
                                    Object[] objects,
                                    String sourceId)
        {
            final String methodName = "ffdcDumpDefault";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass, methodName, new Object[] { throwable,
                                                                    incidentStream,
                                                                    caller,
                                                                    objects,
                                                                    sourceId });

            // Capture stack Ssccid information.
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            java.util.Set<String> classNames = new java.util.HashSet<String>();
            for (int i = 0; i < stackTraceElements.length; i++) {
                final String elementClassName = stackTraceElements[i].getClassName();

                // We only care about .objectManager. classes that we have not already seen.
                if ((elementClassName.indexOf(".objectManager.") >= 0)
                    && !classNames.contains(elementClassName)) {

                    Object sccsid = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<Object>() {
                        public Object run() {
                            try {
                                Class elementClass = Class.forName(elementClassName);
                                java.lang.reflect.Field declaredField = elementClass.getDeclaredField("Ssccid");
                                if (declaredField != null) {
                                    declaredField.setAccessible(true);
                                    return declaredField.get(null);
                                }
                            } catch (Exception exception) {
                                // No FFDC code needed.
                            } // try...
                            return null;
                        } // run().
                    });
                    if (sccsid != null)
                        incidentStream.writeLine(elementClassName, sccsid.toString());

                    classNames.add(elementClassName);
                } // if (   (elementClassName.indexOf...

            } // for...

            if (caller != null)
                dump(caller, incidentStream);

            if (objects != null) {
                for (int i = 0; i < objects.length; i++)
                    dump(objects[i], incidentStream);
            } // if (objects != null).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName);
        } // ffdcDumpDefault().

        private void dump(Object object,
                          com.ibm.ws.ffdc.IncidentStream incidentStream) {
            java.io.StringWriter stringWriter = new java.io.StringWriter();
            java.io.PrintWriter printWriter = new java.io.PrintWriter(stringWriter);
            if (object instanceof Printable) {
                try {
                    ((Printable) object).print(printWriter);
                } catch (Exception exception) {
                    // No FFDC code needed.
                    printWriter.print(object);
                    printWriter.println();
                    printWriter.print(exception);
                } // try...
            } else {
                printWriter.print(object);
            } // if (object instanceof Printable).
            incidentStream.write(object.getClass().getName(), stringWriter.toString());
        } // dump().

    } // inner class Diagnostics.

} // Of class FFDCImpl.

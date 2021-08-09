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
 * Make concrete instances of Trace, starter implementation, to be used
 * in conjunction with AbstractTrace.
 * 
 * @version @(#) 1/25/13
 * @author Andrew_Banks
 */
public abstract class AbstractTraceFactory
                extends TraceFactory {
    private static final Class cclass = AbstractTraceFactory.class;

    // Map of registered Trace indexed by sourceClass.
    final java.util.Map activeTrace = new java.util.HashMap();

    // java.io.FileWriter traceFileWriter;
    //static { 
    //    try {
    //      java.io.FileWriter traceFileWriter = new java.io.FileWriter("C:\\temp\\trace.txt");
    //      printWriter = new java.io.PrintWriter(traceFileWriter);
    //    } catch (java.io.IOException e){}
    //  }

    /**
     * @param nls for info tracing.
     */
    public AbstractTraceFactory(NLS nls) {
        super(nls);
        setPrintWriter(new java.io.PrintWriter(System.out, true));
    } // AbstractTraceFactoryImpl().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.TraceFactory#getTrace(java.lang.Class, java.lang.String)
     */
    public Trace getTrace(Class sourceClass,
                          String traceGroup) {
        synchronized (activeTrace) {
            TraceImpl traceImpl = new TraceImpl(sourceClass, traceGroup, this);
            activeTrace.put(sourceClass, traceImpl);
            try {
                applyActiveTrace();
            } catch (java.io.IOException exception) {
                System.out.println(cclass + ":getTrace() exception" + exception);
                exception.printStackTrace();
            } // try.

            return traceImpl;
        } // synchronized (activeTrace).
    } // getTrace().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.TraceFactory#setActiveTrace(java.lang.String, int)
     */
    public final void setActiveTrace(String activeNames,
                                     int traceLevel)
                    throws java.io.IOException {
        super.setActiveTrace(activeNames, traceLevel);
        applyActiveTrace();
    } // setActiveTrace().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.AbstractTraceFactory#applyActiveTrace()
     */
    void applyActiveTrace()
                    throws java.io.IOException {
        String[] names = activeNames.split(":");

        // Loop over existing Components.
        for (java.util.Iterator traceIterator = activeTrace.values().iterator(); traceIterator.hasNext();) {
            AbstractTrace abstractTrace = (AbstractTrace) traceIterator.next();

            int traceLevelToSet = Trace.Level_None;
            // Loop over active Components, testing to see if any of them is a match.
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                if (name.equals("*"))
                    name = ".*";
                if (java.util.regex.Pattern.matches(name, abstractTrace.getSourceClass().getName()))
                    traceLevelToSet = traceLevel;
            } // for names...
            abstractTrace.setLevel(traceLevelToSet);
        } // for ... activeTrace Map.

    } // applyActiveTrace().

} // class abstractTraceFactory.

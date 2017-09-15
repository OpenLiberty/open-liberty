/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.ffdc.DiagnosticModule;
import com.ibm.ws.ffdc.FFDC;
import com.ibm.ws.ffdc.IncidentStream;

/**
 * An IncidentStreamImpl is a lightweight implementation of an Incident stream
 *
 */
public final class IncidentLogger {

    public void logIncident(IncidentStream iStream, IncidentImpl incident, Throwable th, Object callerThis, Object[] objectArray) {
        logIncident(iStream, incident, th, callerThis, objectArray, false);
    }

    public void logIncident(IncidentStream iStream, IncidentImpl incident, Throwable th, Object callerThis, Object[] objectArray, boolean callerDumpOnly) {

        final String time = formatTime();

        if (!callerDumpOnly) {
            iStream.writeLine("------Start of DE processing------", time);
            iStream.writeLine("Exception", incident.getExceptionName());
            iStream.writeLine("Source", incident.getSourceId());
            iStream.writeLine("probeid", incident.getProbeId());
            iStream.writeLine("Stack Dump", getStackTrace(th));
        }

        // Find a diagnostic module to call (note: like the real FFDC code we
        // assume that the FFDCFilter.processException is called from the code
        // that called the code that failed
        StackTraceElement[] exceptionCallStack = th.getStackTrace();
        int commonIndex = compare(exceptionCallStack);

        // Look down the common part of the stack trace looking for a DM to run
        Map<String, DiagnosticModule> modules = FFDC.getDiagnosticModuleMap();
        boolean foundAnyDM = false;
        if (commonIndex != -1) {
            boolean tryNextDM = true;
            Set<DiagnosticModule> calledModules = null;
            String[] dmsCallStack = null;
            for (int i = commonIndex; (i < exceptionCallStack.length) && (tryNextDM); i++) {
                String packageName = getPackageName(exceptionCallStack[i].getClassName());
                while ((packageName.length() != 0) && (tryNextDM)) {
                    DiagnosticModule module = modules.get(packageName);
                    if (module != null && (calledModules == null || calledModules.add(module))) {
                        if (calledModules == null) {
                            calledModules = Collections.newSetFromMap(new IdentityHashMap<DiagnosticModule, Boolean>());
                            calledModules.add(module);
                        }

                        // Have we already found a DM (and hence done the level
                        // 0 introspection of callerThis?
                        if (!foundAnyDM)
                            iStream.introspectAndWriteLine("Dump of callerThis", callerThis, 0);
                        foundAnyDM = true;

                        if (dmsCallStack == null) {
                            dmsCallStack = getCallStackFromStackTraceElement(exceptionCallStack);
                        }

                        tryNextDM = module.dumpComponentData(new String[] {}, th, iStream, callerThis, objectArray, incident.getSourceId(), dmsCallStack);
                    }

                    int lastDot = packageName.lastIndexOf('.');
                    if (lastDot == -1)
                        packageName = "";
                    else
                        packageName = packageName.substring(0, lastDot);
                }
            }
        }

        if (!foundAnyDM) {
            // OK, we need to really dump objectThis to a deeper level than 0
            iStream.introspectAndWriteLine("Dump of callerThis", callerThis, 3);

            // We also need to introspect the objects in the objectArray
            if (objectArray != null) {
                for (int i = 0; i < objectArray.length; i++) {
                    iStream.introspectAndWriteLine("Dump of objectArray[" + i + "]", objectArray[i], 3);
                }
            }
        }

    }

    /**
     * Return the current time formatted in a standard way
     *
     * @return The current time
     */
    static String formatTime() {
        Date date = new Date();
        DateFormat formatter = BaseTraceFormatter.useIsoDateFormat ? new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ") : DateFormatProvider.getDateFormat();
        StringBuffer answer = new StringBuffer();
        answer.append('[');
        formatter.format(date, answer, new FieldPosition(0));
        answer.append(']');
        return answer.toString();
    }

    /**
     * Return the index in a StackTraceElement[] in exceptionCallStack that is
     * the first common point with the current point of execution (i.e. elements
     * with this index or higher are common with the current point of execution)
     *
     * @param exceptionCallStack
     *            The call stack to be examined
     * @return The index of the first common call point (-1 if there is NO
     *         common point)
     */
    private static int compare(StackTraceElement[] exceptionCallStack) {
        // Weed out the very easy cases
        if (exceptionCallStack == null || exceptionCallStack.length == 0)
            return -1;

        StackTraceElement[] currentCallStack = Thread.currentThread().getStackTrace();

        // Handle the cases where the JIT has destroyed all the relevant
        // information
        if (currentCallStack == null || currentCallStack.length == 0)
            return -1;

        int proposedExceptionIndex = exceptionCallStack.length - 1;
        int proposedCurrentIndex = currentCallStack.length - 1;

        while ((proposedExceptionIndex >= 0) && (proposedCurrentIndex >= 0) && (exceptionCallStack[proposedExceptionIndex] != null)
               && (exceptionCallStack[proposedExceptionIndex].getClassName().equals(currentCallStack[proposedCurrentIndex].getClassName()))) {
            proposedExceptionIndex--;
            proposedCurrentIndex--;
        }

        proposedExceptionIndex++;

        if (proposedExceptionIndex > exceptionCallStack.length)
            return -1; // No common point - can't ever be true? mkv
        else
            return proposedExceptionIndex;
    }

    /**
     * Create the call stack array expected by diagnostic modules from an array
     * of StackTraceElements
     *
     * @param exceptionCallStack
     *            The stack trace elements
     * @return The call stack
     */
    private static String[] getCallStackFromStackTraceElement(StackTraceElement[] exceptionCallStack) {
        if (exceptionCallStack == null)
            return null;

        String[] answer = new String[exceptionCallStack.length];
        for (int i = 0; i < exceptionCallStack.length; i++) {
            answer[exceptionCallStack.length - 1 - i] = exceptionCallStack[i].getClassName();
        }
        return answer;
    }

    /**
     * Return the package name of a given class name
     *
     * @param className
     *            The class name from which to find the package name
     * @return The package name of the class (the empty string is returned for
     *         the default package
     */
    private static String getPackageName(String className) {
        int end = className.lastIndexOf('.');
        return (end > 0) ? className.substring(0, end) : "";
    }

    /**
     * Return the stack trace of an exception as a string
     *
     * @param ex
     *            The exception
     * @return the stack trace as a string
     */
    static String getStackTrace(Throwable ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));

        return sw.toString();
    }

}
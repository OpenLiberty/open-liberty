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
package com.ibm.ws.sib.ffdc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.ibm.ws.ffdc.DiagnosticModule;
import com.ibm.ws.ffdc.FFDC;
import com.ibm.ws.ffdc.IncidentStream;

/* ************************************************************************** */
/**
 * An IncidentStreamImpl is a lightweight implementation of an Incident stream
 * 
 */
/* ************************************************************************** */
public final class IncidentStreamImpl implements IncidentStream {

    private static final int DEFAULT_DEPTH = 1;
    private static final int DEFAULT_MAX_SIZE = 1024 * 1024;

    private static final String EQUALS = " = ";

    private final StringWriter sw = new StringWriter();

    private final PrintWriter ps = new PrintWriter(sw);

    /* -------------------------------------------------------------------------- */
    /*
     * IncidentStreamImpl constructor
     * /* --------------------------------------------------------------------------
     */
    /**
     * Construct a new IncidentStreamImpl.
     */
    public IncidentStreamImpl() {
    // No actual construction work needed - it's all done by the initialization of the variables!
    }

    /* -------------------------------------------------------------------------- */
    /*
     * introspectAndWrite method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#introspectAndWrite(java.lang.String, java.lang.Object)
     * @param text
     * @param value
     */
    public void introspectAndWrite(String text, Object value) {
        ps.println(text);
        introspect(value, DEFAULT_DEPTH, DEFAULT_MAX_SIZE);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * introspectAndWrite method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#introspectAndWrite(java.lang.String, java.lang.Object, int)
     * @param text
     * @param value
     * @param depth
     */
    public void introspectAndWrite(String text, Object value, int depth) {
        ps.println(text);
        introspect(value, depth, DEFAULT_MAX_SIZE);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * introspectAndWrite method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#introspectAndWrite(java.lang.String, java.lang.Object, int, int)
     * @param text
     * @param value
     * @param depth
     * @param maxBytes
     */
    public void introspectAndWrite(String text, Object value, int depth,
                                   int maxBytes) {
        ps.println(text);
        introspect(value, depth, maxBytes);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * introspectAndWriteLine method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#introspectAndWriteLine(java.lang.String, java.lang.Object)
     * @param text
     * @param value
     */
    public void introspectAndWriteLine(String text, Object value) {
        introspectAndWrite(text, value);
        ps.println();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * introspectAndWriteLine method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#introspectAndWriteLine(java.lang.String, java.lang.Object, int)
     * @param text
     * @param value
     * @param depth
     */
    public void introspectAndWriteLine(String text, Object value, int depth) {
        introspectAndWrite(text, value, depth);
        ps.println();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * introspectAndWriteLine method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#introspectAndWriteLine(java.lang.String, java.lang.Object, int, int)
     * @param text
     * @param value
     * @param depth
     * @param maxBytes
     */
    public void introspectAndWriteLine(String text, Object value, int depth,
                                       int maxBytes) {
        introspectAndWrite(text, value, depth, maxBytes);
        ps.println();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * write method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, boolean)
     * @param text
     * @param value
     */
    public void write(String text, boolean value) {
        printValueIntro(text);
        ps.print(value);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * write method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, byte)
     * @param text
     * @param value
     */
    public void write(String text, byte value) {
        printValueIntro(text);
        ps.print(value);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * write method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, char)
     * @param text
     * @param value
     */
    public void write(String text, char value) {
        printValueIntro(text);
        ps.print(value);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * write method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, short)
     * @param text
     * @param value
     */
    public void write(String text, short value) {
        printValueIntro(text);
        ps.print(value);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * write method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, int)
     * @param text
     * @param value
     */
    public void write(String text, int value) {
        printValueIntro(text);
        ps.print(value);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * write method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, long)
     * @param text
     * @param value
     */
    public void write(String text, long value) {
        printValueIntro(text);
        ps.print(value);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * write method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, float)
     * @param text
     * @param value
     */
    public void write(String text, float value) {
        printValueIntro(text);
        ps.print(value);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * write method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, double)
     * @param text
     * @param value
     */
    public void write(String text, double value) {
        printValueIntro(text);
        ps.print(value);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * write method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, java.lang.String)
     * @param text
     * @param value
     */
    public void write(String text, String value) {
        printValueIntro(text);
        ps.print(value);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * write method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, java.lang.Object)
     * @param text
     * @param value
     */
    public void write(String text, Object value) {
        printValueIntro(text);
        ps.print(value);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * printValueIntro method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Print the introductory text for a value
     * 
     * @param text The introductory text (null if there is to be no intro)
     */
    private void printValueIntro(String text) {
        if (text != null) {
            ps.print(text);
            ps.print(EQUALS);
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * writeLine method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, boolean)
     * @param text
     * @param value
     */
    public void writeLine(String text, boolean value) {
        write(text, value);
        ps.println();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * writeLine method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, byte)
     * @param text
     * @param value
     */
    public void writeLine(String text, byte value) {
        write(text, value);
        ps.println();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * writeLine method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, char)
     * @param text
     * @param value
     */
    public void writeLine(String text, char value) {
        write(text, value);
        ps.println();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * writeLine method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, short)
     * @param text
     * @param value
     */
    public void writeLine(String text, short value) {
        write(text, value);
        ps.println();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * writeLine method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, int)
     * @param text
     * @param value
     */
    public void writeLine(String text, int value) {
        write(text, value);
        ps.println();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * writeLine method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, long)
     * @param text
     * @param value
     */
    public void writeLine(String text, long value) {
        write(text, value);
        ps.println();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * writeLine method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, float)
     * @param text
     * @param value
     */
    public void writeLine(String text, float value) {
        write(text, value);
        ps.println();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * writeLine method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, double)
     * @param text
     * @param value
     */
    public void writeLine(String text, double value) {
        write(text, value);
        ps.println();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * writeLine method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, java.lang.String)
     * @param text
     * @param value
     */
    public void writeLine(String text, String value) {
        write(text, value);
        ps.println();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * writeLine method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, java.lang.Object)
     * @param text
     * @param value
     */
    public void writeLine(String text, Object value) {
        write(text, value);
        ps.println();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * introspect method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param value The object to be introspected
     * @param max_depth The maximum depth to introspect
     * @param max_size The maximum amount of data to be produced
     */
    private void introspect(Object value, int max_depth, int max_size) {
        if (value == null) {
            // Easy case!
            ps.print("null");
        } else {
            IntrospectionLevel rootLevel = new IntrospectionLevel(value);

            IntrospectionLevel currentLevel = rootLevel;
            IntrospectionLevel nextLevel = rootLevel.getNextLevel();
            int totalBytes = currentLevel.getNumberOfBytesinJustThisLevel();
            int actualDepth = 0;
            while (actualDepth < max_depth && nextLevel.hasMembers() &&
                   totalBytes <= max_size) {
                totalBytes -= currentLevel.getNumberOfBytesinJustThisLevel();
                totalBytes += currentLevel.getNumberOfBytesInAllLevelsIncludingThisOne();
                currentLevel = nextLevel;
                nextLevel = nextLevel.getNextLevel();
                totalBytes += currentLevel.getNumberOfBytesinJustThisLevel();
                actualDepth++;
            }
            boolean exceededMaxBytes = false;
            if (totalBytes > max_size && actualDepth > 0) {
                actualDepth--;
                exceededMaxBytes = true;
            }
            rootLevel.print(this, actualDepth);
            if (exceededMaxBytes == true) {
                ps.println("Only " + actualDepth
                           + " levels of object introspection were performed because performing the next level would have exceeded the specified maximum bytes of " + max_size);
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * processIncident method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Process a single incident and log it in this incident stream
     * 
     * @param txt A description of the incident (the class name of the exception)
     * @param sourceId The source of the incident
     * @param probeId The probe id of the incident
     * @param th The exception that caused the incident
     * @param callerThis The object detecting the incident
     * @param objectArray Other objects involved in the incident
     */
    void processIncident(String txt, String sourceId, String probeId, Throwable th, Object callerThis, Object[] objectArray) {
        write("------Start of DE processing------", formatTime());
        writeLine(", key", txt);
        writeLine("Exception", (th != null ? th.getClass().getName() : "null"));
        writeLine("Source", sourceId);
        writeLine("probeid", probeId);
        writeLine("Stack Dump", getStackTrace(th));
        introspectAndWriteLine("Dump of callerThis", callerThis, 0);

        // Find a diagnostic module to call (note: like the real FFDC code we assume that
        // the FFDCFilter.processException is called from the code that called the code that failed
        StackTraceElement[] exceptionCallStack;

        if (th != null)
            exceptionCallStack = th.getStackTrace();
        else
            exceptionCallStack = Thread.currentThread().getStackTrace();
        int commonIndex = compare(exceptionCallStack);

        // Look down the common part of the stack trace looking for a DM to run
        Map<String, DiagnosticModule> modules = FFDC.getDiagnosticModuleMap();
        boolean foundAnyDM = false;

        if (commonIndex != -1) // i.e. there was a common index
        {
            boolean tryNextDM = true;
            String[] dmsCallStack = getCallStackFromStackTraceElement(exceptionCallStack);
            for (int i = commonIndex; (i < exceptionCallStack.length) && (tryNextDM); i++) {
                String packageName = getPackageName(exceptionCallStack[i].getClassName());
                while ((packageName.length() != 0) && (tryNextDM)) {
                    DiagnosticModule module = modules.get(packageName);
                    if (module != null) {
                        foundAnyDM = true;
                        tryNextDM = module.dumpComponentData(new String[] {}, th, this,
                                                             callerThis, objectArray, sourceId, dmsCallStack);
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
            introspectAndWriteLine("Dump of callerThis", callerThis, 3);
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getCallStackFromStackTraceElement method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Create the call stack array expected by diagnostic modules from an array
     * of StackTraceElements
     * 
     * @param exceptionCallStack The stack trace elements
     * @return The call stack
     */
    private static String[] getCallStackFromStackTraceElement(StackTraceElement[] exceptionCallStack) {
        if (exceptionCallStack == null)
            return null;
        else {
            String[] answer = new String[exceptionCallStack.length];
            for (int i = 0; i < exceptionCallStack.length; i++) {
                answer[exceptionCallStack.length - 1 - i] = exceptionCallStack[i].getClassName();
            }
            return answer;
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getPackageName method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Return the package name of a given class name
     * 
     * @param className The class name from which to find the package name
     * @return The package name of the class (the empty string is returned for the default package
     */
    private static String getPackageName(String className) {
        int end = className.lastIndexOf('.');
        if (end > 0)
            return className.substring(0, end);
        else
            return "";
    }

    /* -------------------------------------------------------------------------- */
    /*
     * compare method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Return the index in a StackTraceElement[] in exceptionCallStack that is the
     * first common point with the current point of execution (i.e. elements with
     * this index or higher are common with the current point of execution)
     * 
     * @param exceptionCallStack The call stack to be examined
     * @return The index of the first common call point (-1 if there is NO common point)
     */
    private static int compare(StackTraceElement[] exceptionCallStack) {
        // Weed out the very easy cases
        if (exceptionCallStack == null || exceptionCallStack.length == 0)
            return -1;

        StackTraceElement[] currentCallStack = Thread.currentThread().getStackTrace();

        // Handle the cases where the JIT has destroyed all the relevant information
        if (currentCallStack == null || currentCallStack.length == 0)
            return -1;

        int proposedExceptionIndex = exceptionCallStack.length - 1;
        int proposedCurrentIndex = currentCallStack.length - 1;

        while ((proposedExceptionIndex >= 0)
               && (proposedCurrentIndex >= 0)
               && (exceptionCallStack[proposedExceptionIndex] != null)
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

    /* -------------------------------------------------------------------------- */
    /*
     * formatTime method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Return the current time formatted in a standard way
     * 
     * @return The current time
     */
    private static String formatTime() {
        Date date = new Date();
        DateFormat formatter = getBasicDateFormatter();
        StringBuffer answer = new StringBuffer();
        answer.append("[");
        formatter.format(date, answer, new FieldPosition(0));
        answer.append("]");
        return answer.toString();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getBasicDateFormatter method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Return a format string that will produce a reasonable standard way for
     * formatting time (but still using the current locale)
     * 
     * @return The format string
     */
    private static DateFormat getBasicDateFormatter() {
        String pattern;
        int patternLength;
        int endOfSecsIndex;
        // Retrieve a standard Java DateFormat object with desired format.
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
        if (formatter instanceof SimpleDateFormat) {
            // Retrieve the pattern from the formatter, since we will need to modify it.
            SimpleDateFormat sdFormatter = (SimpleDateFormat) formatter;
            pattern = sdFormatter.toPattern();
            // Append milliseconds and timezone after seconds
            patternLength = pattern.length();
            endOfSecsIndex = pattern.lastIndexOf('s') + 1;
            String newPattern = pattern.substring(0, endOfSecsIndex) + ":SSS z";
            if (endOfSecsIndex < patternLength)
                newPattern += pattern.substring(endOfSecsIndex, patternLength);
            // 0-23 hour clock (get rid of any other clock formats and am/pm)
            newPattern = newPattern.replace('h', 'H');
            newPattern = newPattern.replace('K', 'H');
            newPattern = newPattern.replace('k', 'H');
            newPattern = newPattern.replace('a', ' ');
            newPattern = newPattern.trim();
            sdFormatter.applyPattern(newPattern);
            formatter = sdFormatter;
        } else {
            formatter = new SimpleDateFormat("yy.MM.dd HH:mm:ss:SSS z");
        }
        return formatter;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getStackTrace method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Return the stack trace of an exception as a string
     * 
     * @param ex The exception
     * @return the stack trace as a string
     */
    static String getStackTrace(Throwable ex) {
        StringWriter sw = new StringWriter();
        if (ex != null) {
            ex.printStackTrace(new PrintWriter(sw));
        }

        return sw.toString();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * toString method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see java.lang.Object#toString()
     * @return A string representation of the IncidentStream (in this case the contents!)
     */
    @Override
    public String toString() {
        return sw.toString();
    }
}
// End of file
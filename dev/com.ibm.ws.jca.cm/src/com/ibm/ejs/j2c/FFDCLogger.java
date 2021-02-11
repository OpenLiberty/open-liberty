/*******************************************************************************
 * Copyright (c) 2001, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.util.ArrayList;

import com.ibm.ws.ffdc.FFDCSelfIntrospectable;

/**
 * This class was created to convert our existing FFDC logging methods to use the standard
 * FFDCSelfIntrospectable interface instead. Methods for adding data are called 'append' since
 * our old way of doing FFDC involved appending everything to a StringBuffer instead of the
 * String arrays used by FFDCSelfIntrospectable. The sole purpose of this class is to minimize
 * the changes needed to our old way of recording FFDC data.
 */
public class FFDCLogger {
    private final ArrayList<String> lines;
    public static final String TAB = "  ";
    /** The end-of-line character. */
    public static final String EOLN = String.format("%n");

    /**
     * Create a new FFDCLogger with a default size of 40 lines.
     *
     * @param invoker the object requesting the FFDC data.
     */
    public FFDCLogger(Object invoker) {
        this(40, invoker);
    }

    /**
     * Create a new FFDCLogger with the specified number of lines.
     *
     * @param numLines the expected number of lines.
     * @param invoker  the object requesting the FFDC data.
     */
    public FFDCLogger(int numLines, Object invoker) {
        lines = new ArrayList<String>(numLines);
        createFFDCHeader(invoker);
    }

    /**
     * Appends FFDC information to the log.
     *
     * @param description a description of the value.
     * @param value       the value.
     *
     * @return this FFDC logger.
     */
    public final FFDCLogger append(String description, Object value) {
        lines.add(description);
        lines.add(new StringBuffer().append(TAB).append(value).toString());

        return this;
    }

    /**
     * Appends FFDC information to the log.
     *
     * @param description a description of the value.
     * @param value       the value.
     *
     * @return this FFDC logger.
     */
    public final FFDCLogger appendWithEOLN(String description, Object value) {
        lines.add(description);
        lines.add(new StringBuffer().append(TAB).append(value).append(EOLN).toString());

        return this;
    }

    /**
     * Appends a single line of FFDC information.
     *
     * @param info the information to add.
     *
     * @return this FFDC logger.
     */
    public final FFDCLogger append(String info) {
        lines.add(new StringBuffer().append(info).toString());

        return this;
    }

    /**
     * Appends a single line of FFDC information.
     *
     * @param info the information to add.
     *
     * @return this FFDC logger.
     */
    public final FFDCLogger appendWithEOLN(String info) {
        lines.add(new StringBuffer().append(info).append(EOLN).toString());

        return this;
    }

    /**
     * Appends output from another FFDC self introspect method.
     *
     * @param moreLines the output from the other method.
     *
     * @return this FFDC logger.
     */
    public final FFDCLogger append(String[] moreLines) {
        int numLines = moreLines.length;
        for (int i = 0; i < numLines; i++)
            lines.add(moreLines[i]);

        return this;
    }

    /**
     * Creates a header for FFDC output.
     *
     * @param invoker the object we are recording FFDC information for.
     *
     * @return this FFDC logger.
     */
    public FFDCLogger createFFDCHeader(Object invoker) {
        lines.add("JMS20190315a - Start of createFFDCHeader dump");
        lines.add("_______________________________________________________________________");
        lines.add("");
        lines.add("    First Failure Data Capture information for");
        lines.add(new StringBuffer().append("          ").append(this.toString(invoker)).toString());
        lines.add("_______________________________________________________________________");
        lines.add("");

        return this;
    }

    /**
     * Append only a line break.
     *
     * @return this FFDC logger.
     */
    public final FFDCLogger eoln() {
        lines.add("");
        return this;
    }

    /**
     * Appends an indented line of FFDC information.
     *
     * @param value the information to add.
     *
     * @return this FFDC logger.
     */
    public final FFDCLogger indent(Object value) {
        lines.add(new StringBuffer().append(TAB).append(value).toString());
        return this;
    }

    /**
     * Appends the FFDC self introspection information for the object provided. If none is
     * available then we delegate to the normal append method.
     *
     * @param description a description of the value.
     * @param value       the value.
     *
     * @return this FFDC logger.
     */
    public final FFDCLogger introspect(String description, Object value) {
        if (value instanceof FFDCSelfIntrospectable)
            append(((FFDCSelfIntrospectable) value).introspectSelf());
        else
            append(description, value);

        return this;
    }

    /**
     * @return a String array containing the FFDC information.
     */
    public final String[] toStringArray() {
        int numLines = lines.size();
        String[] output = new String[numLines];

        for (int i = 0; i < numLines; i++)
            output[i] = lines.get(i);

        return output;
    }

    /**
     * Implements the standard toString method for objects on which toString has been
     * overridden.
     *
     * @param obj the Object to convert to a String.
     *
     * @return String equivalent to what would be returned by Object.toString(),
     *         or null, if the object is null.
     */
    public String toString(Object obj) {
        if (obj == null)
            return null;

//        if (Proxy.isProxyClass(obj.getClass())
//            && Proxy.getInvocationHandler(obj) instanceof WSJdbcTracer)
//            return obj.toString();

        return new StringBuffer(obj.getClass().getName()).append('@').append(Integer.toHexString(System.identityHashCode(obj))).toString();
    }
}

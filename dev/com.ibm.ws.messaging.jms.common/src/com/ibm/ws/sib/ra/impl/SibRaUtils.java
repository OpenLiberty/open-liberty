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

package com.ibm.ws.sib.ra.impl;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Utility class.
 */
public final class SibRaUtils {

    /**
     * The group to use for trace.
     */
    private final static String TRACE_GROUP = TraceGroups.TRGRP_RA;

    /**
     * The NLS message bundle.
     */
    private final static String MSG_BUNDLE = "com.ibm.ws.sib.ra.CWSIVMessages";

    /**
     * Prime number to use in the construction of hash codes.
     */
    private final static int HASH_CODE_PRIME = 1000003;

    /**
     * Private constructor to prevent instantiation.
     */
    private SibRaUtils() {
        // Should never be called
    }

    /**
     * Gets a trace component for the given class.
     * 
     * @param clazz
     *            the class to get a trace component for
     * @return the trace component
     */
    public static TraceComponent getTraceComponent(final Class clazz) {

        return SibTr.register(clazz, TRACE_GROUP, MSG_BUNDLE);

    }

    /**
     * Gets the <code>TraceNLS</code> for this component.
     * 
     * @return the <code>TraceNLS</code>
     */
    public static TraceNLS getTraceNls() {

        return TraceNLS.getTraceNLS(MSG_BUNDLE);

    }

    /**
     * Compares two objects for equality.
     * 
     * @param one
     *            the first object
     * @param two
     *            the second object
     * @return <code>true</code> if the two objects are <b>not </b> equal,
     *         othewise <code>false</code>
     */
    public static boolean objectsNotEqual(final Object one, final Object two) {

        return (one == null) ? (two != null) : (!one.equals(two));

    }

    /**
     * Add the hash code for the given object to the current hash code.
     * 
     * @param hashCode
     *            the current hash code
     * @param object
     *            the object to add
     * @return the new hash code
     */
    public static int addObjectToHashCode(final int hashCode,
            final Object object) {

        return hashCode + (HASH_CODE_PRIME * objectHashCode(object));

    }

    /**
     * Returns a hash code for the given object or zero if it is
     * <code>null</code>.
     * 
     * @param object
     *            the object to return the hash code for
     * @return the hash code
     */
    public static int objectHashCode(final Object object) {

        return (object == null) ? 0 : object.hashCode();

    }

    /**
     * Converts a subject to a printable form taking care not access the private
     * credentials which requires additional permissions.
     * 
     * @param subject
     *            the subject (may be <code>null</code>)
     * @return a printable from
     */
    public static String subjectToString(final Subject subject) {

        final String result;

        if (subject == null) {
            
            result = "null";
            
        } else {
            
            final StringBuffer buffer = startToString(subject);
            addFieldToString(buffer, "principals", subject.getPrincipals());
            endToString(buffer);
            result = buffer.toString();
            
        }

        return result;

    }

    /**
     * Creates a <code>StringBuffer</code> for the string representation of
     * the given object.
     * 
     * @param object
     *            the object to create the buffer for
     * @return the buffer
     */
    public static StringBuffer startToString(final Object object) {

        StringBuffer buffer = new StringBuffer("[");
        buffer.append(object.getClass().getName());
        buffer.append("@");
        buffer.append(Integer.toHexString(System.identityHashCode(object)));
        return buffer;

    }

    /**
     * Adds a string representation of the given field to the buffer.
     * 
     * @param buffer
     *            the buffer
     * @param name
     *            the name of the field
     * @param value
     *            the value of the field
     */
    public static void addFieldToString(final StringBuffer buffer,
            final String name, final Object value) {

        buffer.append(" <");
        buffer.append(name);
        buffer.append("=");
        buffer.append(value);
        buffer.append(">");

    }

    /**
     * Adds a string representation of the given boolean field to the buffer.
     * 
     * @param buffer
     *            the buffer
     * @param name
     *            the name of the field
     * @param value
     *            the value of the field
     */
    public static void addFieldToString(final StringBuffer buffer,
            final String name, final boolean value) {

        buffer.append(" <");
        buffer.append(name);
        buffer.append("=");
        buffer.append(value);
        buffer.append(">");

    }

    /**
     * Adds a string representation of the given password to the buffer. This
     * will contain a row of asterisks if the password is non-null.
     * 
     * @param buffer
     *            the buffer
     * @param name
     *            the name of the field
     * @param value
     *            the value of the field
     */
    public static void addPasswordFieldToString(final StringBuffer buffer,
            final String name, final Object value) {

        addFieldToString(buffer, name, (value == null) ? null : "*****");

    }

    /**
     * Ends the string representation of the object in the given buffer.
     * 
     * @param buffer
     *            the buffer
     */
    public static void endToString(final StringBuffer buffer) {

        buffer.append("]");

    }

}

/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.anno.util;

import java.util.Collection;
import com.ibm.websphere.ras.TraceComponent;

// Several names for packages, classes, fields, and methods are available:
//
// For packages and classes, three names are available:
//
// A class or package name from a java class file description field.
// Such a name has "L" at the beginning and ";" at the end, and has "/" delimiters.
//
// A class or package name as a resource name.
// Such a name is a trailing ".class" and has "/" delimiters.
//
// A java class name.
// Such a name has "." delimiters.
//
// For fields and methods, a single name is available, with no possible adornments.

public interface Util_InternMap {
    //

    String getHashText();

    int getLogThreshHold();

    void logState();

    void log(TraceComponent tc);

    //

    public enum ValueType {
        VT_CLASS_RESOURCE,
        VT_CLASS_REFERENCE,
        VT_CLASS_NAME,
        VT_FIELD_NAME,
        VT_METHOD_NAME,
        VT_OTHER;
    }

    //

    Util_Factory getFactory();

    String getName();

    //

    /**
     * Check the input value for syntax errors based on the type of value.
     * <p>
     * The validate method will return a message key as the string if a
     * validation error is found. The message key should exist in the NLS
     * resource bundle used by the caller. The message key can then be passed
     * to com.ibm.websphere.ras.Tr along with any substitution parameters
     * for display in the log file.
     * 
     * @param value a String that contains the value to be validated
     * @param valueType a ValueType that identifies the type of 'value'
     * @return String if 'value' contains an error, return the message key to the
     *         corresponding error message from the resource bundle, else
     *         return 'null'.
     */
    String validate(String value, ValueType valueType);

    ValueType getValueType();

    //

    Collection<String> getValues();

    int getSize();

    int getTotalLength();

    boolean DO_FORCE = true;
    boolean DO_NOT_FORCE = false;

    // TODO: Propagate the 'doForce' option to all lookups.
    //       A failed lookup should not add an element to the intern map.

    boolean contains(String name);

    String intern(String name);

    String intern(String name, boolean doForce);
}

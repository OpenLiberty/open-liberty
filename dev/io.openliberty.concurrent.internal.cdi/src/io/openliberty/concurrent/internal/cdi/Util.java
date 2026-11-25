/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.internal.cdi;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * Various utility methods.
 */
@Trivial
public class Util {

    /**
     * Returns a String containing the names of classes delimited by commas.
     *
     * @param classes Java classes.
     * @return the names of classes delimited by commas.
     */
    static String classNames(Collection<? extends Class<?>> classes) {
        StringBuilder b = new StringBuilder();
        for (Class<?> c : classes)
            b.append(b.isEmpty() ? "" : ", ").append(c.getName());
        return b.toString();
    }

    /**
     * Converts the given value to a hexadecimal String consistint of 0x
     * followed by at least 8 hexadecimal digits.
     *
     * @param value value to convert
     * @return hexadecimal representation of the value
     */
    public static String hex(long value) {
        String hex = Long.toHexString(value);
        StringBuilder s = new StringBuilder(10).append("0x");
        for (int i = 8 - hex.length(); i > 0; i--)
            s.append('0');
        return s.append(hex).toString();
    }

    /**
     * Constructs a new modifiable list.
     *
     * @param <T>    type of list elements
     * @param ignore ignored value
     * @return a new modifiable list.
     */
    static final <T> ArrayList<T> newList(Object ignore) {
        return new ArrayList<T>();
    }

    /**
     * Utility method to obtain the JEE name for a MetaData instance if possible,
     * and otherwise the name.
     *
     * @param metadata ComponentMetaData, ModuleMetaData, or ApplicationMetaData.
     * @return the artifact name, preferably as a JEE name string.
     */
    static String toArtifactName(MetaData metadata) {
        if (metadata instanceof ComponentMetaData)
            return ((ComponentMetaData) metadata).getJ2EEName().toString();
        else if (metadata instanceof ModuleMetaData)
            return ((ModuleMetaData) metadata).getJ2EEName().toString();
        else if (metadata instanceof ApplicationMetaData)
            return ((ApplicationMetaData) metadata).getJ2EEName().toString();
        else
            return metadata.getName();
    }

    /**
     * Utility method that converts an exception to a stack trace string.
     *
     * @param x exception.
     * @return stack trace string.
     */
    static String toStackTrace(Throwable x) {
        StringWriter s = new StringWriter();
        x.printStackTrace(new PrintWriter(s));
        return s.toString();
    }

}
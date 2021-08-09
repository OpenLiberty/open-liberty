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
package com.ibm.ws.kernel.feature;

import java.util.EnumSet;
import java.util.Locale;

/**
 * An enum indicating process type information
 */
public enum ProcessType {
    /** Can be used in a server process */
    SERVER,
    /** Can be used in a client process */
    CLIENT;

    // do not ingore FFDC for IllegalArgumentException
    public static EnumSet<ProcessType> fromString(String s) {
        if (s == null)
            return EnumSet.of(SERVER);

        EnumSet<ProcessType> result = EnumSet.noneOf(ProcessType.class);
        String[] values = s.split(",");
        for (String part : values) {
            try {
                result.add(ProcessType.valueOf(part.trim().toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException iae) {
            }
        }
        if (result.isEmpty()) {
            result.add(SERVER);
        }
        return result;
    }

    public static String toString(EnumSet<ProcessType> processTypes) {
        StringBuilder result = new StringBuilder();
        for (ProcessType processType : processTypes) {
            if (result.length() != 0) {
                result.append(',');
            }
            result.append(processType.toString());
        }
        return result.toString();
    }
}

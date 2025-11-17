/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

public class HeaderValidation {
    public static boolean acceptContains(String acceptHeader, String mime) {
        if (acceptHeader == null || mime == null) {
            return false;
        }
        if (acceptHeader.equals("*/*")) {
            return true;
        }
        String target = mime.toLowerCase(java.util.Locale.ROOT);
        for (String part : acceptHeader.split(",")) {
            String value = part.trim().toLowerCase(java.util.Locale.ROOT);
            value = value.split(";")[0];
            if (value.equals("*/*"))
                return true;

            if (value.startsWith(target))
                return true;

            // Match type/* e.g., text/*
            if (value.endsWith("/*")) {
                String type = value.substring(0, value.indexOf("/"));
                if (target.startsWith(type + "/"))
                    return true;
            }
        }
        return false;
    }

}

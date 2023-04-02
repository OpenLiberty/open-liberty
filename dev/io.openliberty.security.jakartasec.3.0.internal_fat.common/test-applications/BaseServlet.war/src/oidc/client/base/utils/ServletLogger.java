/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.client.base.utils;

import java.io.IOException;

import jakarta.servlet.ServletOutputStream;

public class ServletLogger {

    public static void printLine(ServletOutputStream ps, String caller, String msg) throws IOException {

        printLine(ps, caller + msg);

    }

    public static void printLine(ServletOutputStream ps, String msg) throws IOException {

        System.out.println(msg);
        if (ps != null) {
            ps.println(msg);
        } else {
            System.out.println("output stream is null");
        }

    }

    public static void printSeparator(ServletOutputStream ps) throws IOException {
        printLine(ps, "**************************************************************************************************************");
    }

    public static String getShortName(String longClassName) throws IOException {

        if (longClassName != null) {
            String[] splitClassName = longClassName.split("\\.");
            return splitClassName[splitClassName.length - 1];
        }
        return null;

    }

    public static void printBlankLine(ServletOutputStream ps) throws IOException {
        printLine(ps, "");
    }

}

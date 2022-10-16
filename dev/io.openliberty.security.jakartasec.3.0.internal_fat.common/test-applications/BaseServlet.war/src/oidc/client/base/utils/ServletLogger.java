/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
        ps.println(msg);

    }

    public static void printSeparator(ServletOutputStream ps) throws IOException {
        printLine(ps, "**************************************************************************************************************");
    }

}
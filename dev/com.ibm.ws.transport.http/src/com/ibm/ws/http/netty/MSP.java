/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

/**
 *
 */
public final class MSP {

    static long counter = 0;
    static boolean enabled = false;

    public static void log(String msg) {
        if (enabled)
            System.out.println("MSP: " + msg);
    }

    public static void debug(String probe) {
        counter++;
        if (enabled)
            System.out.println("MSP: " + probe + "-" + counter);
    }

    public static void stack() {

        if (!enabled)
            return;

        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        System.out.println(" MSP -> Current Stack:");
        for (int i = 2; i < stack.length; i++) {
            System.out.println(stack[i]);
        }
    }

}

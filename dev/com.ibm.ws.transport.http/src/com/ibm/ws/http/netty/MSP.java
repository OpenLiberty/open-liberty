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

    public static void log(String msg) {
        System.out.println("MSP: " + msg);
    }

    public static void debug(String probe) {
        counter++;
        System.out.println("MSP: " + probe + "-" + counter);
    }

}

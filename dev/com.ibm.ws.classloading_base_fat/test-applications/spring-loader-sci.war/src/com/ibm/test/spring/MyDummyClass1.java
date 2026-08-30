/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package com.ibm.test.spring;

/**
 *
 */
public class MyDummyClass1 {

    private static String HELLO = "hello";

    private final String world = "world";

    public static void printHello() {
        System.out.println("MyDummyClass1: " + HELLO);
    }

    public String getWorld() {
        return world;
    }
}

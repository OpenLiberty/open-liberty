/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.clientcontainer.HelloAppClient.test.cdi;

import javax.inject.Inject;

public class HelloAppClient {

    @Inject
    private static NamedManagedBean ivMB;

    public static void main(String[] args) {
        System.out.println("\nHello Application Client.");
        System.out.println(ivMB.getValue());
        System.out.println("Good bye\n");
    }

    public static void postConstruct() {
        System.out.println("I have been in postConstruct of main.");
    }
}

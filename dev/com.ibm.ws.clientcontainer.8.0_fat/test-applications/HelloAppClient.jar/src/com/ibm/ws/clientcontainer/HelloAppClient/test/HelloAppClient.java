/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.HelloAppClient.test;

import javax.annotation.Resource;

public class HelloAppClient {

    @Resource(name = "ManagedBean")
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

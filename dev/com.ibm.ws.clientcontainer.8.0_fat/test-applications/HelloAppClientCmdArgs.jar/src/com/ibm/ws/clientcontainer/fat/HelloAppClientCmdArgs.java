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
package com.ibm.ws.clientcontainer.fat;

public class HelloAppClientCmdArgs {

    public static void main(String[] args) {
        System.out.println("\nHello Application Client.");
        for (int i = 0; i < args.length; i++) {
            System.out.println("arg[" + i + "]: " + args[i]);
        }
        System.out.println("Good bye\n");
    }

}

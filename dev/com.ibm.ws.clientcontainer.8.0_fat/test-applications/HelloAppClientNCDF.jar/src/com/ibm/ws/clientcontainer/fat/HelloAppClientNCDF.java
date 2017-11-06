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

// Triggers NoClassDefFoundError for callback handler
public class HelloAppClientNCDF {

    public static void main(String[] args) {
        System.out.println("\nHello Application Client.");
        System.out.println("Good bye\n");
    }

}

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

import javax.annotation.Resource;

public class ErrorInjectionAppClient {

    @Resource(lookup = "java:app/AppName")
    private static void setAppName(String appName) {
        System.out.println("ErrorInjectionAppClient - setAppName " + appName);
        throw new RuntimeException("Expected exception to simulate a user's setter method throwing an exception");
    }

    public static void main(String[] args) {
        // since injection should fail, this method should never be invoked.
        System.out.println("ErrorInjectionAppClient - main - if you see this, then we didn't prevent the main method execution despite an injection failure");
    }
}

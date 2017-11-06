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

public class SlowInjectionAppClient {
    private static String appName;

    @Resource(lookup = "java:app/AppName")
    private static void setAppName(String appName) {
        try {
            System.out.println("SlowInjectionAppClient - sleeping for 45 seconds");
            Thread.sleep(45000);
            System.out.println("SlowInjectionAppClient - waking up now");
        } catch (Throwable t) {
            t.printStackTrace();
        }
        SlowInjectionAppClient.appName = appName;
    }

    public static void main(String[] args) {
        // since injection takes 15 seconds, and the server is set to only wait for 10 seconds,
        // this method should never be invoked.
        System.out.println("SlowInjectionAppClient - AppName = " + appName);
    }

}

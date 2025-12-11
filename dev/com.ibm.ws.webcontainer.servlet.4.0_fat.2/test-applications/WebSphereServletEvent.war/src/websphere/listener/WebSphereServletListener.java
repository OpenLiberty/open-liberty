/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package websphere.listener;

import com.ibm.websphere.servlet.event.ServletEvent;
import com.ibm.websphere.servlet.event.ServletListener;

/*
 * ServletListener is called ONCE during the init and destroy.
 */

public class WebSphereServletListener implements ServletListener {
    private static final String CLASS_NAME = WebSphereServletListener.class.getName();
    String temp;

    public WebSphereServletListener() {
        log("\t\t>> WebSphereServletListener constructor");
    }

    @Override
    public void onServletAvailableForService(ServletEvent arg0) {
        temp = "\t\t>> 2. WebSphereServletListener.onServletAvailableForService for ServletEvent.getServletName [" + arg0.getServletName() + "]\n";

        log(temp);
        WebSphereApplicationListener.OUTBUFFER.append(temp);
    }

    @Override
    public void onServletFinishDestroy(ServletEvent arg0) {
        log("onServletFinishDestroy, ServletEvent.getServletName [" + arg0.getServletName() + "]");
    }

    @Override
    public void onServletFinishInit(ServletEvent arg0) {
        temp = "\t\t<< 2. WebSphereServletListener.onServletFinishInit for ServletEvent.getServletName [" + arg0.getServletName() + "]\n"; 
        log(temp);
        WebSphereApplicationListener.OUTBUFFER.append(temp);
    }

    @Override
    public void onServletStartDestroy(ServletEvent arg0) {
        log("onServletStartDestroy, ServletEvent.getServletName [" + arg0.getServletName() + "]");
    }

    @Override
    public void onServletStartInit(ServletEvent arg0) {
        temp = "\t\t>> 2. WebSphereServletListener.onServletStartInit for ServletEvent.getServletName [" + arg0.getServletName() + "]\n";
        log(temp);

        WebSphereApplicationListener.OUTBUFFER.append(temp);
    }

    @Override
    public void onServletUnavailableForService(ServletEvent arg0) {
        log("onServletUnavailableForService, ServletEvent.getServletName [" + arg0.getServletName() + "]");
    }

    @Override
    public void onServletUnloaded(ServletEvent arg0) {
        log("onServletUnloaded, ServletEvent.getServletName [" + arg0.getServletName() + "]");
    }

    private void log(String s) {
        System.out.println("\t" + CLASS_NAME + ": " + s);
    }
}

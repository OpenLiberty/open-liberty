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

import com.ibm.websphere.servlet.event.ServletErrorEvent;
import com.ibm.websphere.servlet.event.ServletErrorListener;

public class WebSphereServletErrorListener implements ServletErrorListener {
    private static final String CLASS_NAME = WebSphereServletErrorListener.class.getName();
    String temp;

    public WebSphereServletErrorListener() {
        log("\t\t>> 3. WebSphereServletErrorListener constructor");
    }

    @Override
    public void onServletDestroyError(ServletErrorEvent arg0) {
        log("onServletDestroyError, ServletErrorEvent.getRootCause [" + arg0.getRootCause()+ "]");
    }

    @Override
    public void onServletInitError(ServletErrorEvent arg0) {
        log("onServletInitError, ServletErrorEvent.getRootCause [" + arg0.getRootCause()+ "]");
    }

    @Override
    public void onServletServiceDenied(ServletErrorEvent arg0) {
        log("onServletServiceDenied, ServletErrorEvent.getRootCause [" + arg0.getRootCause()+ "]");
    }

    @Override
    public void onServletServiceError(ServletErrorEvent arg0) {
        temp = "\t\t\t\t>> 3. WebSphereServletErrorListener.onServletServiceError , error [" + arg0.getRootCause()+ "]\n";

        log(temp);
        WebSphereApplicationListener.OUTBUFFER.append(temp);
    }

     private void log(String s) {
        System.out.println("\t" + CLASS_NAME + ": " + s);
    }
}

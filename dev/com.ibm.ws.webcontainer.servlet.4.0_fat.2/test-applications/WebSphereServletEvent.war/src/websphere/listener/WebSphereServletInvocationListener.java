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

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;

import com.ibm.websphere.servlet.event.ServletInvocationEvent;
import com.ibm.websphere.servlet.event.ServletInvocationListener;

/*
 *  ServletInvocationLister is invoked on every request.
 *
 *  OutputStream is obtained for every request.  The final write out is in the FilterInvocationListener.
 */

public class WebSphereServletInvocationListener implements ServletInvocationListener {
    private static final String CLASS_NAME = WebSphereServletInvocationListener.class.getName();
    public static final String WEBSPHERE_ATT = "WEBSPHERE_API";

    static ServletOutputStream outputStream = null;

    public WebSphereServletInvocationListener() {
        log("\t\t\t>>> 2.2 WebSphereServletInvocationListener constructor");
    }

    @Override
    public void onServletFinishService(ServletInvocationEvent arg0) {
        String temp = "\t\t\t<<< 2.2 WebSphereServletInvocationListener.onServletFinishService, for request URL [" + arg0.getRequestURL() + "]\n";
        log(temp);

        WebSphereApplicationListener.OUTBUFFER.append(temp);
    }

    @Override
    public void onServletStartService(ServletInvocationEvent arg0) {
        String temp = "\t\t\t>>> 2.2 WebSphereServletInvocationListener.onServletStartService, for request URL [" + arg0.getRequestURL() + "]. OutputStream obtained.\n";
        log(temp);

        WebSphereApplicationListener.OUTBUFFER.append(temp);

        try {
            outputStream = arg0.getResponse().getOutputStream();
        } catch (IOException e) {
            log("CAN NOT OBTAIN outputStream!");
            e.printStackTrace();
        } 
    }

     private void log(String s) {
        System.out.println("\t" + CLASS_NAME + ": " + s);
    }
}

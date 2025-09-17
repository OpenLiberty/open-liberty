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

import com.ibm.websphere.servlet.event.FilterInvocationEvent;
import com.ibm.websphere.servlet.event.FilterInvocationListener;

/*
 * InvocationListener is invoked during the service of request.
 * Format: use the \t and < mainly for the readablity from the client side.
 */

public class WebSphereFilterInvocationListener implements FilterInvocationListener {
    private static final String CLASS_NAME = WebSphereServletInvocationListener.class.getName();

    public WebSphereFilterInvocationListener() {
        log("\t\t\t>>> WebSphereFilterInvocationListener constructor");
    }

    @Override
    public void onFilterFinishDoFilter(FilterInvocationEvent arg0) {
        log("onFilterFinishDoFilter, filter [" + arg0.getFilterName() + "]");

        WebSphereApplicationListener.OUTBUFFER.append("\t\t\t<<< 1.1 WebSphereFilterInvocationListener.onFilterFinishDoFilter for filter [" + arg0.getFilterName() + "]\n");

        // WRITE OUT all data to client!
        try {
            log("######## WRITE ALL DATA TO CLIENT");
            WebSphereServletInvocationListener.outputStream.println(WebSphereApplicationListener.OUTBUFFER.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            WebSphereServletInvocationListener.outputStream = null;
        }
    }

    @Override
    public void onFilterStartDoFilter(FilterInvocationEvent arg0) {
        log("onFilterStartDoFilter, filter [" + arg0.getFilterName() + "]");

        WebSphereApplicationListener.OUTBUFFER.append("\t\t\t>>> 1.1 WebSphereFilterInvocationListener.onFilterStartDoFilter for filter [" + arg0.getFilterName() + "]\n");
    }

    private void log(String s) {
        System.out.println("\t" + CLASS_NAME + ": " + s);
    }
}

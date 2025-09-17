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

import com.ibm.websphere.servlet.event.FilterErrorEvent;
import com.ibm.websphere.servlet.event.FilterErrorListener;

public class WebSphereFilterErrorListener implements FilterErrorListener {
    private static final String CLASS_NAME = WebSphereFilterErrorListener.class.getName();
    String temp;

    public WebSphereFilterErrorListener() {
        log("\t\t>> 4. WebSphereFilterErrorListener constructor");
    }

    private void log(String s) {
        System.out.println("\t" + CLASS_NAME + ": " + s);
    }

    @Override
    public void onFilterDestroyError(FilterErrorEvent arg0) {
        log("onFilterDestroyError, FilterErrorEvent.getRootCause [" + arg0.getRootCause()+ "]");
    }

    //Trigger on ServletException, FNFException, IOException 
    @Override
    public void onFilterDoFilterError(FilterErrorEvent arg0) {
        temp = "\t\t\t\t>> 4. WebSphereFilterErrorListener.onFilterDoFilterError , filter error [" + arg0.getRootCause()+ "]\n";

        log(temp);
        WebSphereApplicationListener.OUTBUFFER.append(temp);
    }

    @Override
    public void onFilterInitError(FilterErrorEvent arg0) {
        log("onFilterInitError, FilterErrorEvent.getRootCause [" + arg0.getRootCause()+ "]");
    }
}

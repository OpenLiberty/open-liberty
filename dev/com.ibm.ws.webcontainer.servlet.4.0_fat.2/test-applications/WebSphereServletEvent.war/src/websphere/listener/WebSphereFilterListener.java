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

import com.ibm.websphere.servlet.event.FilterEvent;
import com.ibm.websphere.servlet.event.FilterListener;

/*
 *  FilterListener is invoked ONCE during the init and destroy
 */

public class WebSphereFilterListener implements FilterListener{
    private static final String CLASS_NAME = WebSphereFilterListener.class.getName();
    String temp;

    public WebSphereFilterListener() {
        log("\t\t>> WebSphereFilterListener constructor");
    }

    @Override
    public void onFilterFinishDestroy(FilterEvent arg0) {
        log("onFilterFinishDestroy, filter [" + arg0.getFilterName() + "]");
    }

    @Override
    public void onFilterFinishInit(FilterEvent arg0) {
        temp = "\t\t<< 1. WebSphereFilterListener.onFilterFinishInit for filter [" + arg0.getFilterName() + "]\n"; 
        log(temp);
        WebSphereApplicationListener.OUTBUFFER.append(temp);
    }

    @Override
    public void onFilterStartDestroy(FilterEvent arg0) {
        log("onFilterStartDestroy, filter [" + arg0.getFilterName() + "]");
    }

    @Override
    public void onFilterStartInit(FilterEvent arg0) {
        temp = "\t\t>> 1. WebSphereFilterListener.onFilterStartInit for filter [" + arg0.getFilterName() + "]\n";
        log(temp);
        WebSphereApplicationListener.OUTBUFFER.append(temp);
    }

    private void log(String s) {
        System.out.println("\t" + CLASS_NAME + ": " + s);
    }
} 

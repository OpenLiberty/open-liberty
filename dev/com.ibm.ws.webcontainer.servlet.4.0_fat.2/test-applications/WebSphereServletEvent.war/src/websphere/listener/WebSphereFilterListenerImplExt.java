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
import com.ibm.websphere.servlet.event.FilterListenerImpl;

/*
 * Test the extension of FilterListenerImpl (implements FilterInvocationListener, FilterListener, FilterErrorListener)
 * Override one method is probably enough.
 */

public class WebSphereFilterListenerImplExt extends FilterListenerImpl{
    private static final String CLASS_NAME = WebSphereFilterListenerImplExt.class.getName();
    String temp;

    public WebSphereFilterListenerImplExt() {
        log("\t\t>> 5. WebSphereFilterListenerImplExt constructor");
    }

    @Override
    public void onFilterStartInit(FilterEvent arg0) {
        temp = "\t\t>> 5. WebSphereFilterListenerImplExt.onFilterStartInit for filter [" + arg0.getFilterName() + "]\n";
        log(temp);
        WebSphereApplicationListener.OUTBUFFER.append(temp);
    }
    
    private void log(String s) {
        System.out.println("\t" + CLASS_NAME + ": " + s);
    }
} 

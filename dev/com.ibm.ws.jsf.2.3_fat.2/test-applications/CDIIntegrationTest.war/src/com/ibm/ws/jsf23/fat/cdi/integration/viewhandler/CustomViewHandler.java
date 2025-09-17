/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.cdi.integration.viewhandler;

import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;

/**
 * Custom View Handler to make sure it is possible to use it
 * in an application.
 */
public class CustomViewHandler extends ViewHandlerWrapper {

    private final ViewHandler wrapped;

    public CustomViewHandler(ViewHandler viewHandler) {
        wrapped = viewHandler;
        System.out.println("CustomViewHandler was invoked!");
    }

    @Override
    public ViewHandler getWrapped() {
        return wrapped;
    }

}

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
package com.ibm.ws.wsoc.injection;

/**
 * Need for keeping track of more than one class dealing with pushing and popping contexts
 */
public class InjectionThings {

    private ClassLoader originalCL = null;
    private boolean appActivateResult = false;

    public ClassLoader getOriginalCL() {
        return originalCL;
    }

    public void setOriginalCL(ClassLoader x) {
        originalCL = x;
    }

    public boolean getAppActivateResult() {
        return appActivateResult;
    }

    public void setAppActivateResult(boolean x) {
        appActivateResult = x;
    }

}

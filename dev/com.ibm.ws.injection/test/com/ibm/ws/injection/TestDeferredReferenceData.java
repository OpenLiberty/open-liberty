/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
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
package com.ibm.ws.injection;

import java.io.PrintWriter;

import com.ibm.ws.injectionengine.osgi.internal.DeferredReferenceData;
import com.ibm.wsspi.injectionengine.InjectionException;

public class TestDeferredReferenceData implements DeferredReferenceData {
    private final Boolean returnValue;
    boolean called;

    public TestDeferredReferenceData(Boolean returnValue) {
        this.returnValue = returnValue;
    }

    @Override
    public boolean processDeferredReferenceData() throws InjectionException {
        called = true;
        if (returnValue == null) {
            throw new InjectionException("test exception");
        }
        return returnValue;
    }

    @Override
    public void introspectDeferredReferenceData(PrintWriter writer, String indent) {}
}

/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal;

import java.io.PrintWriter;

import com.ibm.ws.injectionengine.ReferenceContextImpl;
import com.ibm.wsspi.injectionengine.InjectionException;

public class OSGiReferenceContextImpl extends ReferenceContextImpl implements DeferredReferenceData {
    private final OSGiInjectionScopeData scopeData;

    OSGiReferenceContextImpl(OSGiInjectionEngineImpl injectionEngine, OSGiInjectionScopeData scopeData) {
        super(injectionEngine);
        this.scopeData = scopeData;
        scopeData.addDeferredReferenceData(this);
    }

    @Override
    public synchronized void process() throws InjectionException {
        scopeData.removeDeferredReferenceData(this);
        super.process();
    }

    @Override
    public boolean processDeferredReferenceData() throws InjectionException {
        process();
        return true;
    }

    @Override
    public void introspectDeferredReferenceData(PrintWriter writer, String indent) {
        writer.println(indent + toString());
    }
}

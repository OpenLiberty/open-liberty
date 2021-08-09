/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ras.instrument.internal.bci;

import org.objectweb.asm.ClassVisitor;

import com.ibm.ws.ras.instrument.internal.model.ClassInfo;
import com.ibm.ws.ras.instrument.internal.model.TraceOptionsData;

public abstract class AbstractTracingRasClassAdapter extends AbstractRasClassAdapter {

    public AbstractTracingRasClassAdapter(ClassVisitor visitor, ClassInfo classInfo) {
        super(visitor, classInfo);
    }

    public boolean isTraceExceptionOnThrow() {
        TraceOptionsData data = getTraceOptionsData();
        if (data != null) {
            return getTraceOptionsData().isTraceExceptionThrow();
        }
        return false;
    }

    public boolean isTraceExceptionOnHandling() {
        TraceOptionsData data = getTraceOptionsData();
        if (data != null) {
            return data.isTraceExceptionHandling();
        }
        return false;
    }
}

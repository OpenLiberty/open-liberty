/*******************************************************************************
 * Copyright (c) 2010, 2023 IBM Corporation and others.
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
package com.ibm.ws.ras.instrument.internal.bci;

import org.objectweb.asm.ClassVisitor;

import com.ibm.ws.ras.instrument.internal.main.LibertyTracePreprocessInstrumentation.ClassTraceInfo;
import com.ibm.ws.ras.instrument.internal.model.ClassInfo;
import com.ibm.ws.ras.instrument.internal.model.TraceOptionsData;

public abstract class AbstractTracingRasClassAdapter extends AbstractRasClassAdapter {

    public AbstractTracingRasClassAdapter(ClassVisitor visitor,
    		ClassInfo classInfo, ClassTraceInfo traceInfo,
    		boolean throwComputeFrames) {

    	super(visitor, classInfo, traceInfo, throwComputeFrames);
    }

    public AbstractTracingRasClassAdapter(ClassVisitor visitor, ClassInfo classInfo, ClassTraceInfo traceInfo) {
    	super(visitor, classInfo, traceInfo);
    }

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

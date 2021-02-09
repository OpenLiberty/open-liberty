/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.introspect;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import com.ibm.ws.ras.instrument.internal.model.TraceOptionsData;

public class TraceOptionsAnnotationVisitor extends AnnotationVisitor {

    protected List<String> traceGroups = new ArrayList<String>();
    protected String messageBundle;
    protected boolean traceExceptionThrow;
    protected boolean traceExceptionHandling;
    private TraceOptionsData packageData;

    public TraceOptionsAnnotationVisitor() {
        super(Opcodes.ASM8);
    }

    public TraceOptionsAnnotationVisitor(AnnotationVisitor av) {
        super(Opcodes.ASM8, av);
    }

    public TraceOptionsAnnotationVisitor(AnnotationVisitor av, TraceOptionsData od) {
    	super(Opcodes.ASM8, av);
    	packageData = od;
	}

	@Override
    public void visit(String name, Object value) {
        if ("traceGroup".equals(name)) {
            String traceGroup = String.class.cast(value);
            if (!traceGroups.contains(traceGroup)) {
                traceGroups.add(traceGroup);
            }
        } else if ("messageBundle".equals(name)) {
            messageBundle = String.class.cast(value);
        } else if ("traceExceptionThrow".equals(name)) {
            traceExceptionThrow = Boolean.class.cast(value).booleanValue();
        } else if ("traceExceptionHandling".equals(name)) {
            traceExceptionHandling = Boolean.class.cast(value).booleanValue();
        }
        super.visit(name, value);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        AnnotationVisitor av = super.visitArray(name);
        if ("traceGroups".equals(name)) {
            av = new TraceGroupsValueArrayVisitor(av);
        }
        return av;
    }

    private final class TraceGroupsValueArrayVisitor extends AnnotationVisitor {

        private TraceGroupsValueArrayVisitor(AnnotationVisitor av) {
            super(Opcodes.ASM8, av);
        }

        @Override
        public void visit(String name, Object value) {
            String traceGroup = String.class.cast(value);
            if (!"".equals(traceGroup) && !traceGroups.contains(traceGroup)) {
                traceGroups.add(traceGroup);
            }
            super.visit(name, value);
        }
    }

    public TraceOptionsData getTraceOptionsData() {
    	if (traceGroups.isEmpty() && packageData != null)
    		return packageData;
    	else
    		return new TraceOptionsData(traceGroups, messageBundle, traceExceptionThrow, traceExceptionHandling);
    }
}

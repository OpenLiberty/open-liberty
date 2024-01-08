/*******************************************************************************
 * Copyright (c) 2010,2023 IBM Corporation and others.
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

package com.ibm.ws.ras.instrument.internal.introspect;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import io.openliberty.asm.ASMHelper;

import com.ibm.ws.ras.instrument.internal.bci.RasMethodAdapter;

public class InjectedTraceAnnotationVisitor extends AnnotationVisitor {

    private final List<String> methodAdapters = new ArrayList<String>();
    private Class<? extends RasMethodAdapter> currentMethodVisitor;
    private boolean visitedValueArray = false;

    public InjectedTraceAnnotationVisitor() {
        super(ASMHelper.getCurrentASM());
    }

    public InjectedTraceAnnotationVisitor(AnnotationVisitor av) {
        super(ASMHelper.getCurrentASM(), av);
    }

    public <T extends RasMethodAdapter> InjectedTraceAnnotationVisitor(AnnotationVisitor av, Class<T> currentMethodVisitor) {
        super(ASMHelper.getCurrentASM(), av);
        this.currentMethodVisitor = currentMethodVisitor;
    }

    @Override
    public void visit(String name, Object value) {
        super.visit(name, value);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        AnnotationVisitor av = super.visitArray(name);
        if ("value".equals(name)) {
            av = new ValueArrayVisitor(av);
        }
        return av;
    }

    private class ValueArrayVisitor extends AnnotationVisitor {
        private ValueArrayVisitor(AnnotationVisitor av) {
            super(ASMHelper.getCurrentASM(), av);
        }

        @Override
        public void visit(String name, Object value) {
            visitedValueArray = true;
            String methodAdapter = String.class.cast(value);
            if (!methodAdapters.contains(value)) {
                methodAdapters.add(methodAdapter);
            }
            super.visit(name, value);
        }

        @Override
        public void visitEnd() {
            if (currentMethodVisitor != null) {
                // Force the current visitor into the annotation w/o putting in list
                if (!methodAdapters.contains(currentMethodVisitor.getName())) {
                    super.visit(null, currentMethodVisitor.getName());
                }
            }
            super.visitEnd();
        }
    }

    @Override
    public void visitEnd() {
        if (!visitedValueArray && currentMethodVisitor != null) {
            visitArray("value").visitEnd();
        }
    }

    public List<String> getMethodAdapters() {
        return methodAdapters;
    }
}

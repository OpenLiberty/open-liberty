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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import io.openliberty.asm.ASMHelper;

public class TraceObjectFieldAnnotationVisitor extends AnnotationVisitor {

    private String fieldName;
    private String fieldDescriptor;

    public TraceObjectFieldAnnotationVisitor() {
        super(ASMHelper.getCurrentASM());
    }

    public TraceObjectFieldAnnotationVisitor(AnnotationVisitor av) {
        super(ASMHelper.getCurrentASM(), av);
    }

    @Override
    public void visit(String name, Object value) {
        super.visit(name, value);
        if ("fieldName".equals(name)) {
            fieldName = String.class.cast(value);
        } else if ("fieldDesc".equals(name)) {
            fieldDescriptor = String.class.cast(value);
        }
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldDescriptor() {
        return fieldDescriptor;
    }
}

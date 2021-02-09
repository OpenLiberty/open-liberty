/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ras.instrument.internal.introspect;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public class TraceObjectFieldAnnotationVisitor extends AnnotationVisitor {

    private String fieldName;
    private String fieldDescriptor;

    public TraceObjectFieldAnnotationVisitor() {
        super(Opcodes.ASM8);
    }

    public TraceObjectFieldAnnotationVisitor(AnnotationVisitor av) {
        super(Opcodes.ASM8, av);
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

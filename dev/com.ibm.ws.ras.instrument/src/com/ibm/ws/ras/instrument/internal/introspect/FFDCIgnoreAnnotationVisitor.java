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

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class FFDCIgnoreAnnotationVisitor extends AnnotationVisitor {

    public final static Type FFDC_IGNORE_TYPE = Type.getObjectType("com/ibm/ws/ffdc/annotation/FFDCIgnore");

    private final Set<Type> ignoredExceptionTypes = new HashSet<Type>();

    public FFDCIgnoreAnnotationVisitor(AnnotationVisitor delegate) {
        super(Opcodes.ASM8, delegate);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        AnnotationVisitor av = super.visitArray(name);
        if ("value".equals(name)) {
            av = new IgnoredExceptionVisitor(av, ignoredExceptionTypes);
        }
        return av;
    }

    public Set<Type> getIgnoredExceptionTypes() {
        return ignoredExceptionTypes;
    }
}

class IgnoredExceptionVisitor extends AnnotationVisitor {
    Set<Type> ignoredExceptionTypes;

    IgnoredExceptionVisitor(AnnotationVisitor delegate, Set<Type> ignoredExceptionTypes) {
        super(Opcodes.ASM8, delegate);
        this.ignoredExceptionTypes = ignoredExceptionTypes;
    }

    @Override
    public void visit(String name, Object value) {
        ignoredExceptionTypes.add(Type.class.cast(value));
        super.visit(name, value);
    }
}

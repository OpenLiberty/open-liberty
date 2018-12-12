/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.digest;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class MethodDigester extends MethodVisitor implements Digester {
    private final Processor processor = new Processor();
    private DigesterSortedMap<AnnotationDigester> annotations = new DigesterSortedMap<AnnotationDigester>();

    MethodDigester(int access) {
        super(Opcodes.ASM7);
        processor.consider(access);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationDigester digester = new AnnotationDigester();
        annotations.put(desc, digester);
        return digester;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        AnnotationDigester digester = new AnnotationDigester();
        annotations.put(parameter + desc, digester);
        return digester;
    }

    @Override
    public void visitEnd() {
        processor.consider(annotations);
        annotations = null;
    }

    @Override
    public byte[] getDigest() {
        return processor.getDigest();
    }
}

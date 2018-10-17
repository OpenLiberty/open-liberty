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
import org.objectweb.asm.Opcodes;

import java.util.Map;
import java.util.TreeMap;


class AnnotationDigester extends AnnotationVisitor implements Digester {
    private final Processor processor = new Processor();
    private Map<String, Object> values = new TreeMap<String, Object>();
    private DigesterSortedMap<AnnotationDigester> annotations = new DigesterSortedMap<AnnotationDigester>();

    public AnnotationDigester() {
        super(Opcodes.ASM7);
    }

    @Override
    public void visit(String name, Object value) {
        values.put(name, value);
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        values.put(name, desc + "#" + value);
    }

    @Override
    public AnnotationDigester visitAnnotation(String name, String desc) {
        AnnotationDigester result = new AnnotationDigester();
        annotations.put(name, result);
        return result;
    }

    @Override
    public AnnotationArrayDigester visitArray(String name) {
        AnnotationArrayDigester result = new AnnotationArrayDigester();
        annotations.put(name, result);
        return result;
    }

    @Override
    public final void visitEnd() {
        processor
                .consider(values)
                .consider(annotations);
        values = null;
        annotations = null;
    }

    @Override
    public byte[] getDigest() {
        return processor.getDigest();
    }

    /** Used purely for visiting an array within an annotation, where elements are sequenced and not named */
    static class AnnotationArrayDigester extends AnnotationDigester {

        int index = 0;

        private String getIndex() {
            return String.format("%08x", index++);
        }

        @Override
        public void visit(String name, Object value) {
            super.visit(getIndex(), value);
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            super.visitEnum(getIndex(), desc, value);
        }

        @Override
        public AnnotationDigester visitAnnotation(String name, String desc) {
            return super.visitAnnotation(getIndex(), desc);
        }

        @Override
        public AnnotationArrayDigester visitArray(String name) {
            return super.visitArray(getIndex());
        }
    }
}


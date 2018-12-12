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

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;

class ClassDigester extends ClassVisitor implements Digester {
    private final Processor processor = new Processor();
    private final DigesterSortedMap<AnnotationDigester> annotations = new DigesterSortedMap<AnnotationDigester>();
    private final DigesterSortedMap<FieldDigester> fields = new DigesterSortedMap<FieldDigester>();
    private final DigesterSortedMap<MethodDigester> methods = new DigesterSortedMap<MethodDigester>();

    public ClassDigester() {
        super(Opcodes.ASM7);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        // not hashing version since the same source can be compiled to multiple class versions
        // not hashing access since this does not affect the behaviour of the class
        // not hashing name since hash will be used alongside class name anyway
        // not hashing signature since this only applies to generics
        processor
                .consider(superName)
                .consider(interfaces);
    }

    @Override
    public AnnotationDigester visitAnnotation(String desc, boolean visible) {
        AnnotationDigester digester = new AnnotationDigester();
        annotations.put(desc, digester);
        return digester;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        return null; // ignoring since only relates to generics
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        // ignore signature since this only applies to generics, which have no effect at runtime
        FieldDigester digester = new FieldDigester(access, desc, value);
        fields.put(name, digester);
        return digester;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        // ignore signature since this only applies to generics, which have no effect at runtime
        // ignore exceptions since declared exceptions have no effect on behaviour
        if ((access & ACC_SYNTHETIC) == ACC_SYNTHETIC)
            return null;
        MethodDigester digester = new MethodDigester(access);
        methods.put(name + "#" + desc, digester);
        return digester;
    }

    @Override
    public final void visitEnd() {
        processor
                .consider(annotations)
                .consider(fields)
                .consider(methods);
    }

    @Override
    public byte[] getDigest() {
        return processor.getDigest();
    }

    public String getDigestAsString() {
        return processor.getDigestAsString();
    }
}

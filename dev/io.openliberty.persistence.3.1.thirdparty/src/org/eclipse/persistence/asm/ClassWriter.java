/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation
package org.eclipse.persistence.asm;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.persistence.asm.internal.Util;

public abstract class ClassWriter extends ClassVisitor {

    //This block must be first - begin
    private final static String ASM_CLASSWRITER_ECLIPSELINK = "org.eclipse.persistence.internal.libraries.asm.ClassWriter";
    private final static String ASM_CLASSWRITER_OW2 = "org.objectweb.asm.ClassWriter";

    private final static Map<String, String> ASM_CLASSWRITER_MAP = new HashMap<>();

    static {
        ASM_CLASSWRITER_MAP.put(ASMFactory.ASM_SERVICE_OW2, ASM_CLASSWRITER_OW2);
        ASM_CLASSWRITER_MAP.put(ASMFactory.ASM_SERVICE_ECLIPSELINK, ASM_CLASSWRITER_ECLIPSELINK);
    }
    //This block must be first - end

    public static final int COMPUTE_FRAMES = valueInt("COMPUTE_FRAMES");

    private ClassWriter cw;
    protected ClassWriter customClassWriter;

    public ClassWriter() {
    }

    public ClassWriter(final int flags) {
        this(null, flags);
    }

    public ClassWriter(final ClassReader classReader, final int flags) {
        super(ASMFactory.ASM_API_SELECTED);
        cw = ASMFactory.createClassWriter(flags);
    }

    public void setCustomClassWriter(ClassWriter classWriter) {
        this.customClassWriter = classWriter;
    }

    public void setCustomClassWriterInImpl(ClassWriter classWriter) {
        this.cw.setCustomClassWriter(classWriter);
    }

    public ClassWriter getInternal() {
        return cw;
    }

    private static int valueInt(String fieldName) {
        return ((int) Util.getFieldValue(ASM_CLASSWRITER_MAP, fieldName, Integer.TYPE));
    }

    public abstract String getCommonSuperClass(final String type1, final String type2);

    public void visit(final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        this.cw.visit(access, name, signature, superName, interfaces);
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        this.cw.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        return this.cw.visitAnnotation(descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitAnnotationSuper(final String descriptor, final boolean visible) {
        return this.cw.visitAnnotationSuper(descriptor, visible);
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
        return this.cw.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public FieldVisitor visitFieldSuper(final int access, final String name, final String descriptor, final String signature, final Object value) {
        return this.cw.visitFieldSuper(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return this.cw.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public MethodVisitor visitMethodSuper(int access, String name, String descriptor, String signature, String[] exceptions) {
        return this.cw.visitMethodSuper(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        this.cw.visitEnd();
    }

    public byte[] toByteArray() {
        return this.cw.toByteArray();
    }

    public byte[] toByteArraySuper() {
        return this.cw.toByteArraySuper();
    }

    @Override
    public abstract <T> T unwrap();
}

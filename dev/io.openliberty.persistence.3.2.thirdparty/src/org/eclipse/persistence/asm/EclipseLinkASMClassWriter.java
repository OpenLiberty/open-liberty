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

/**
 * EclipseLink specific {@link ClassVisitor} that generates a corresponding ClassFile structure
 * for currently running Java VM.
 */
public class EclipseLinkASMClassWriter extends ClassWriter {

    private ClassWriter classWriter;

    public EclipseLinkASMClassWriter() {
        this(ClassWriter.COMPUTE_FRAMES);
    }

    public EclipseLinkASMClassWriter(final int flags) {
        super();
        this.classWriter = ASMFactory.createClassWriter(flags);
    }

    /**
   * Visits the header of the class with {@code version} set
   * equal to currently running Java SE version.
   *
   * @param access the class's access flags (see {@link org.eclipse.persistence.internal.libraries.asm.Opcodes}). This parameter also indicates if
   *     the class is deprecated {@link org.eclipse.persistence.internal.libraries.asm.Opcodes#ACC_DEPRECATED} or a record {@link
   *     org.eclipse.persistence.internal.libraries.asm.Opcodes#ACC_RECORD}.
   * @param name the internal name of the class (see {@link org.eclipse.persistence.internal.libraries.asm.Type#getInternalName()}).
   * @param signature the signature of this class. May be {@literal null} if the class is not a
   *     generic one, and does not extend or implement generic classes or interfaces.
   * @param superName the internal of name of the super class (see {@link org.eclipse.persistence.internal.libraries.asm.Type#getInternalName()}).
   *     For interfaces, the super class is {@link Object}. May be {@literal null}, but only for the
   *     {@link Object} class.
   * @param interfaces the internal names of the class's interfaces (see {@link
   *     Type#getInternalName()}). May be {@literal null}.
   * @see #visit(int, int, String, String, String, String[])
   */
    @Override
    public final void visit(final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        this.classWriter.visit(ASMFactory.JAVA_CLASS_LATEST_VERSION, access, name, signature, superName, interfaces);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.classWriter.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return this.classWriter.visitAnnotation(descriptor, visible);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        return this.classWriter.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return this.classWriter.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        this.classWriter.visitEnd();
    }

    @Override
    public byte[] toByteArray() {
        return this.classWriter.toByteArray();
    }

    @Override
    public String getCommonSuperClass(final String type1, final String type2) {
        return this.classWriter.getCommonSuperClass(type1, type2);
    }

    @Override
    public <T> T unwrap() {
        if (this.classWriter instanceof org.eclipse.persistence.asm.internal.platform.ow2.ClassWriterImpl) {
            return (T)((org.eclipse.persistence.asm.internal.platform.ow2.ClassWriterImpl)this.classWriter).getInternal(this.customClassWriter);
        } else if (this.classWriter instanceof org.eclipse.persistence.asm.internal.platform.eclipselink.ClassWriterImpl) {
            return (T)((org.eclipse.persistence.asm.internal.platform.eclipselink.ClassWriterImpl)this.classWriter).getInternal(this.customClassWriter);
        } else {
            return null;
        }
    }
}

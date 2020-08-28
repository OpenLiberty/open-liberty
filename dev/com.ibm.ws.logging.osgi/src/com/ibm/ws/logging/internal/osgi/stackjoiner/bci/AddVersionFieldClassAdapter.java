/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi.stackjoiner.bci;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Simple adapter that adds a field to hold a version string. This is used
 * where mapping the template classes into the bootstrap package so we can
 * detect stale versions.
 * <p>
 * This implementation will <em>not<em> overwrite an existing field.
 * 
 * This class is borrowed from com.ibm.ws.monitor/src/com/ibm/ws/monitor/internal/bci/remap/AddVersionFieldClassAdapter.java.
 */
public class AddVersionFieldClassAdapter extends ClassVisitor {

    /**
     * The name of the version field.
     */
    final String versionFieldName;

    /**
     * The {@link String} value of the version field.
     */
    final String versionFieldValue;

    /**
     * Inidication that the field existed before augmentation.
     */
    boolean fieldAlreadyExists = false;

    /**
     * Create a class visitor that will create a field to hold a version {@link String}.
     * 
     * @param delegate the chained {@link ClassVisitor}
     * @param versionFieldName the name of the version field
     * @param versionFieldValue the value to associate with the version field
     */
    public AddVersionFieldClassAdapter(ClassVisitor delegate, String versionFieldName, String versionFieldValue) {
        super(Opcodes.ASM8, delegate);
        this.versionFieldName = versionFieldName;
        this.versionFieldValue = versionFieldValue;
    }

    /**
     * Field visitor that observes existing fields before chaining to the
     * delegate {@ClassVisitor}.
     * <p> {@inheritDoc}
     */
    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        fieldAlreadyExists |= name.equals(versionFieldName);

        return super.visitField(access, name, desc, signature, value);
    }

    /**
     * End of class visitor that creates a version field definition if an
     * existing definition wasn't observed.
     */
    @Override
    public void visitEnd() {
        if (!fieldAlreadyExists) {
            FieldVisitor fv = super.visitField(
                                               Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC,
                                               versionFieldName,
                                               Type.getDescriptor(String.class),
                                               null,
                                               versionFieldValue);
            fv.visitEnd();
        }

        super.visitEnd();
    }

    /**
     * Indication of whether or not a version field existed. Existing fields
     * are not overriden by this adapter.
     * 
     * @return true if the version field named existed before adapter execution
     */
    public boolean doesFieldAlreadyExist() {
        return fieldAlreadyExists;
    }
}

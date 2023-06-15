/*
 * Copyright (c) 1998, 2023 Oracle and/or its affiliates. All rights reserved.
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
//     dclarke - Metadata driven Dynamic Persistence
//
package org.eclipse.persistence.internal.jpa.metadata;

import org.eclipse.persistence.asm.ASMFactory;
import org.eclipse.persistence.asm.ClassWriter;
import org.eclipse.persistence.asm.MethodVisitor;
import org.eclipse.persistence.asm.Opcodes;
import org.eclipse.persistence.asm.Type;
import org.eclipse.persistence.dynamic.DynamicClassWriter;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.MappingAccessor;

/**
 * Custom {@link DynamicClassWriter} adding getter methods for virtual
 * attributes so that 3rd party frameworks such as jakarta.validation can access
 * the attribute values.
 *
 * @author dclarke
 * @since EclipseLink 2.4.1
 */
public class MetadataDynamicClassWriter extends DynamicClassWriter {

    private static final String LDYNAMIC_ENTITY = "Lorg/eclipse/persistence/dynamic/DynamicEntity;";
    private static final String SET = "set";
    private static final String LJAVA_LANG_OBJECT = "Ljava/lang/Object;";
    private static final String LJAVA_LANG_STRING = "Ljava/lang/String;";
    private static final String DYNAMIC_EXCEPTION = "org/eclipse/persistence/exceptions/DynamicException";
    private static final String GET = "get";

    /**
     * The {@link MetadataDescriptor} for the dynamic entity
     */
    private MetadataDescriptor descriptor;

    public MetadataDynamicClassWriter(MetadataDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public MetadataDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Add get methods for all virtual attributes
     */
    @Override
    protected void addMethods(ClassWriter cw, String parentClassType) {
        for (MappingAccessor accessor : getDescriptor().getMappingAccessors()) {
            String propertyName = propertyName(accessor.getAttributeName());
            Type returnType = getAsmType(accessor);

            // Add getter
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, GET + propertyName, "()" + returnType.getDescriptor(), null, new String[] { DYNAMIC_EXCEPTION });
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitLdcInsn(accessor.getAttributeName());
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, parentClassType, "get", "(" + LJAVA_LANG_STRING + ")" + LJAVA_LANG_OBJECT, false);
            mv.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();

            // Add setter
            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, SET + propertyName, "(" + returnType.getDescriptor() + ")V", null, new String[] { DYNAMIC_EXCEPTION });
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitLdcInsn("id");
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, parentClassType, SET, "(" + LJAVA_LANG_STRING + LJAVA_LANG_OBJECT + ")" + LDYNAMIC_ENTITY, false);
            mv.visitInsn(Opcodes.POP);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }
    }

    /**
     * Get the {@link Type} for the accessor. If the accessor's type is
     * primitive return the the non-primitive type.
     */
    private Type getAsmType(MappingAccessor accessor) {
        String attributeType = accessor.getFullyQualifiedClassName(accessor.getAttributeType());

        Class<?> primClass = accessor.getPrimitiveClassForName(attributeType);

        if (primClass != null) {
            Type asmType = ASMFactory.createType(primClass);

            int asmTypeSort = asmType.getSort();
            if (asmTypeSort == Type.BOOLEAN) {
                return Type.getType(ClassConstants.BOOLEAN);
            } else if (asmTypeSort == Type.BYTE) {
                return Type.getType(ClassConstants.BYTE);
            } else if (asmTypeSort == Type.CHAR) {
                return Type.getType(ClassConstants.CHAR);
            } else if (asmTypeSort == Type.DOUBLE) {
                return Type.getType(ClassConstants.DOUBLE);
            } else if (asmTypeSort == Type.FLOAT) {
                return Type.getType(ClassConstants.FLOAT);
            } else if (asmTypeSort == Type.INT) {
                return Type.getType(ClassConstants.INTEGER);
            } else if (asmTypeSort == Type.LONG) {
                return Type.getType(ClassConstants.LONG);
            } else if (asmTypeSort == Type.SHORT) {
                return Type.getType(ClassConstants.SHORT);
            }
        }

        return Type.getType("L" + attributeType.replace(".", "/") + ";");
    }

    /**
     * Convert attribute name into property name to be used in get/set method
     * names by upper casing first letter.
     */
    private String propertyName(String attributeName) {
        char string[] = attributeName.toCharArray();
        string[0] = Character.toUpperCase(string[0]);
        return new String(string);
    }
}

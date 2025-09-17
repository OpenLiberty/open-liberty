/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence.service;

import static io.openliberty.data.internal.persistence.EntityManagerBuilder.getClassNames;
import static io.openliberty.data.internal.persistence.cdi.DataExtension.exc;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V17;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.CheckClassAdapter;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.exceptions.DataException;

/**
 * Transforms records to entity classes
 */
public class RecordTransformer {

    private static final TraceComponent tc = Tr.register(RecordTransformer.class);

    /**
     * An information class that takes a RecordComponent and generates
     * descriptor, type, and signature fields to be used during
     * entity class generation.
     */
    @Trivial
    private static final class RecordComponentInfo {
        // About the component
        final public String name;
        final public org.objectweb.asm.Type type;

        // Descriptor for type / getter / setter
        final public String descriptor;
        final public String getterDescriptor;
        final public String setterDescriptor;

        // Signature for field / getter / setter when component is generic
        final public String fieldSignature;
        final public String setterSignature;
        final public String getterSignature;

        public RecordComponentInfo(RecordComponent component) {
            this.name = component.getName();
            this.type = org.objectweb.asm.Type.getType(component.getType());

            this.descriptor = this.type.getDescriptor();
            this.getterDescriptor = "()" + descriptor;
            this.setterDescriptor = "(" + descriptor + ")V";

            String genericSignature = component.getGenericSignature(); // Ljava/util/List<*>;

            if (genericSignature == null) {
                this.setterSignature = this.fieldSignature = this.getterSignature = genericSignature;
            } else {
                this.fieldSignature = genericSignature;
                this.setterSignature = "(" + fieldSignature + ")V";
                this.getterSignature = "()" + fieldSignature;
            }
        }

        public String getMethodName(String prefix) {
            return prefix + name.substring(0, 1).toUpperCase() + name.substring(1);
        }

        @Override
        public String toString() {
            return "RecordComponentInfo [name=" + name + ", type=" + type + ", descriptor=" + descriptor + ", getterDescriptor=" + getterDescriptor + ", setterDescriptor="
                   + setterDescriptor + ", fieldSignature=" + fieldSignature + ", setterSignature=" + setterSignature + ", getterSignature=" + getterSignature + "]";
        }

    }

    /**
     * Generates an entity class from a record class.
     *
     * The generated entity class will have a public field, getter method, and setter method
     * for each record component.
     *
     * The generated entity class will have a public no-args constructor, a public constructor
     * that accepts the corresponding record, and a public toRecord() method that returns a
     * new instance of the record.
     *
     * Initially copied from @nmittles pull #25248
     *
     * @param recordClass          - the record class that will be transformed
     *                                 into an entity class
     * @param entityClassName      - the name of the entity class
     * @param jeeName              - Java EE name (for error logging)
     * @param repositoryInterfaces - repository interfaces (for error logging)
     *
     * @return the byte array representation of the generated entity class
     */
    public static byte[] generateEntityClassBytes(Class<?> recordClass,
                                                  String entityClassName,
                                                  J2EEName jeeName,
                                                  Set<Class<?>> repositoryInterfaces) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        final String CTOR = "<init>";
        final String TO_RECORD = "toRecord";

        // Names of classes used in generating an entity class from a record
        final String _recordClassName = recordClass.getName().replace(".", "/");
        final String _recordClassNameDesc = org.objectweb.asm.Type.getDescriptor(recordClass);

        final String _entityClassName = entityClassName.replace('.', '/');
        final String _objectClassName = Object.class.getName().replace('.', '/');

        final String _sourceFileName = entityClassName.substring(entityClassName.lastIndexOf(".") + 1) + ".java";

        if (recordClass.getTypeParameters().length != 0)
            throw exc(DataException.class,
                      "CWWKD1071.record.with.type.var",
                      recordClass.getName(),
                      getClassNames(repositoryInterfaces),
                      jeeName,
                      Arrays.asList(recordClass.getTypeParameters()));

        // Collect record component information, must preserve order
        Map<String, RecordComponentInfo> recordComponents = new LinkedHashMap<>();
        for (RecordComponent component : recordClass.getRecordComponents()) {
            RecordComponentInfo info = new RecordComponentInfo(component);
            RecordComponentInfo previous = recordComponents.put(info.getMethodName(""), info);

            if (previous != null)
                throw exc(DataException.class,
                          "CWWKD1072.record.comp.conflict",
                          recordClass.getName(),
                          getClassNames(repositoryInterfaces),
                          jeeName,
                          info.name,
                          previous.name);

            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, info.toString());
        }

        // ------------------------------
        // public class [recordClass]Entity {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        cw.visit(V17, ACC_PUBLIC | ACC_SUPER, _entityClassName, null, _objectClassName, null);
        cw.visitSource(_sourceFileName, null);

        // ------------------------------
        //   private [fieldType] [fieldName];
        FieldVisitor fieldVisitor;
        for (RecordComponentInfo rcInfo : recordComponents.values()) {
            if (trace && tc.isEntryEnabled()) {
                Tr.debug(tc, "visitField: " + Arrays.asList("ACC_PRIVATE", rcInfo.name, rcInfo.descriptor, rcInfo.fieldSignature, null));
            }

            fieldVisitor = cw.visitField(ACC_PRIVATE, rcInfo.name, rcInfo.descriptor, rcInfo.fieldSignature, null);
            fieldVisitor.visitEnd();
        }
        // ------------------------------

        // ------------------------------
        //   public [recordClass]Entity() {
        MethodVisitor mv;
        if (trace && tc.isEntryEnabled()) {
            Tr.debug(tc, "visitMethod(ctor): " + Arrays.asList("ACC_PUBLIC", CTOR, "()V", null, null));
        }
        mv = cw.visitMethod(ACC_PUBLIC, CTOR, "()V", null, null);
        mv.visitCode();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, _objectClassName, CTOR, "()V", false);

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0); // Computed
        mv.visitEnd();
        //   }
        // ------------------------------

        // ------------------------------
        //   public [recordClass]Entity([recordClass] record) {
        if (trace && tc.isEntryEnabled()) {
            Tr.debug(tc, "visitMethod(ctor): " + Arrays.asList("ACC_PUBLIC", CTOR, "(" + _recordClassNameDesc + ")V", null, null));
        }
        mv = cw.visitMethod(ACC_PUBLIC, CTOR, "(" + _recordClassNameDesc + ")V", null, null);
        mv.visitCode();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, _objectClassName, CTOR, "()V", false);

        //    this.[entityField] = [record].[component](); ...
        for (RecordComponentInfo rcInfo : recordComponents.values()) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, _recordClassName, rcInfo.name, "()" + rcInfo.descriptor, false);
            mv.visitFieldInsn(PUTFIELD, _entityClassName, rcInfo.name, rcInfo.descriptor);
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0); // Computed
        mv.visitEnd();
        //  }
        // ------------------------------

        // ------------------------------
        //   public [recordClass] toRecord() {
        if (trace && tc.isEntryEnabled()) {
            Tr.debug(tc, "visitMethod: " + Arrays.asList("ACC_PUBLIC", TO_RECORD, "()" + _recordClassNameDesc, null, null));
        }
        mv = cw.visitMethod(ACC_PUBLIC, TO_RECORD, "()" + _recordClassNameDesc, null, null);
        mv.visitCode();

        //     return new [recordClass](
        mv.visitTypeInsn(NEW, _recordClassName);
        mv.visitInsn(DUP);

        //       this.[recordComponent], ...
        StringBuffer paramDesc = new StringBuffer();
        paramDesc.append("(");
        for (RecordComponentInfo rcInfo : recordComponents.values()) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, _entityClassName, rcInfo.name, rcInfo.descriptor);
            paramDesc.append(rcInfo.descriptor);
        }
        paramDesc.append(")V");

        //     );
        mv.visitMethodInsn(INVOKESPECIAL, _recordClassName, CTOR, paramDesc.toString(), false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0); // Computed
        mv.visitEnd();
        //   }
        // ------------------------------

        for (RecordComponentInfo rcInfo : recordComponents.values()) {
            // ------------------------------
            //   public void set[recordComponent]([componentType] arg) {
            if (trace && tc.isEntryEnabled()) {
                Tr.debug(tc, "visitMethod(set): " + Arrays.asList("ACC_PUBLIC", rcInfo.getMethodName("set"), rcInfo.setterDescriptor, rcInfo.setterSignature, null));
            }
            mv = cw.visitMethod(ACC_PUBLIC, rcInfo.getMethodName("set"), rcInfo.setterDescriptor, rcInfo.setterSignature, null);
            mv.visitCode();

            //     this.[entityField] = arg;
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(rcInfo.type.getOpcode(ILOAD), 1);
            mv.visitFieldInsn(PUTFIELD, _entityClassName, rcInfo.name, rcInfo.descriptor);

            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0); // Computed
            mv.visitEnd();
            //   }
            // ------------------------------

            // ------------------------------
            //   public [componentType] get[recordComponent]() {
            if (trace && tc.isEntryEnabled()) {
                Tr.debug(tc, "visitMethod(get): " + Arrays.asList("ACC_PUBLIC", rcInfo.getMethodName("get"), rcInfo.getterDescriptor, rcInfo.getterSignature, null));
            }
            mv = cw.visitMethod(ACC_PUBLIC, rcInfo.getMethodName("get"), rcInfo.getterDescriptor, rcInfo.getterSignature, null);
            mv.visitCode();

            //     return this.[entityField];
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, _entityClassName, rcInfo.name, rcInfo.descriptor);

            mv.visitInsn(rcInfo.type.getOpcode(IRETURN));
            mv.visitMaxs(0, 0); // Computed
            mv.visitEnd();
            //   }
            // ------------------------------
        }

        cw.visitEnd();
        //   }
        // ------------------------------

        byte[] classBytes = cw.toByteArray();

        StringWriter sw = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(classBytes), false, new PrintWriter(sw));
        String result = sw.toString();

        if (!result.isBlank())
            Tr.warning(tc, "CWWKD1112.record.entity.verify.err",
                       recordClass.getName(),
                       getClassNames(repositoryInterfaces),
                       jeeName,
                       result);

        return classBytes;
    }

}

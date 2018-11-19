/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.diagnostics.class_scanner.ano;

import java.math.BigInteger;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.AnnotationInfoType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.AnnotationValueType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.AnnotationsType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ArrayEntryType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ArrayInstanceType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInfoType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ExceptionType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ExceptionsType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.FieldInfoType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.FieldsType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.InnerClassesType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.InterfacesType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.MethodInfoType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.MethodsType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ModifiersType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ParameterType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ParametersType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ValueInstanceType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ValueType;

public class AsmClassAnalyzer {
    private final static String JavaLangObject = "java.lang.Object";
    private final static int ASM_LEVEL = Opcodes.ASM7;

    public static final ClassInfoType analyzeClass(String targetClass, byte[] bytecode, InnerOuterResolver ioResolver) throws ClassScannerException {
        if (bytecode == null || bytecode.length == 0) {
            throw new ClassScannerException("Bytecode is required.");
        }
        AsmClassAnalyzer ca = new AsmClassAnalyzer(bytecode, ioResolver);
        return ca.analyze();
    }

    final private byte[] bytecode;
    final private InnerOuterResolver ioResolver;
    final private ClassInfoType cit = new ClassInfoType();

    private AsmClassAnalyzer(byte[] bytecode, InnerOuterResolver ioResolver) {
        this.bytecode = bytecode;
        this.ioResolver = ioResolver;
    }

    private ClassInfoType analyze() throws ClassScannerException {
        ClassReader cr = new ClassReader(bytecode);
        CAClassVisitor cacv = new CAClassVisitor();
        cr.accept(cacv, 0);

        return cit;
    }

    private class CAClassVisitor extends ClassVisitor {
        public CAClassVisitor() {
            super(ASM_LEVEL);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                          String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);

            final String adjustedName = AsmHelper.normalizeClassName(name);
            cit.setClassName(adjustedName);
            cit.setPackageName(AsmHelper.extractPackageName(adjustedName));
            if (cit.getPackageName() != null && !cit.getPackageName().isEmpty()) {
                cit.setName(adjustedName.substring(cit.getPackageName().length() + 1));
            } else {
                cit.setName(adjustedName);
            }

            if (superName != null) {
                cit.setSuperclassName(AsmHelper.normalizeClassName(superName));
            } else {
                cit.setSuperclassName(JavaLangObject);
            }

            final ModifiersType modTypes = new ModifiersType();
            modTypes.getModifier().addAll(AsmHelper.resolveAsmOpcode(AsmHelper.RoleFilter.CLASS, (access)));
            cit.setModifiers(modTypes);

            if (interfaces != null && interfaces.length > 0) {
                InterfacesType it = new InterfacesType();
                cit.setInterfaces(it);

                for (String iface : interfaces) {
                    it.getInterface().add(iface);
                }
            }

            cit.setVersion(version);
            cit.setIsAnonymous(false);
            cit.setIsEnum((access & Opcodes.ACC_ENUM) != 0);
            cit.setIsInterface((access & Opcodes.ACC_INTERFACE) != 0);
            cit.setIsSynthetic((access & Opcodes.ACC_SYNTHETIC) != 0);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationsType annosType = cit.getAnnotations();
            if (annosType == null) {
                annosType = new AnnotationsType();
                cit.setAnnotations(annosType);
            }

            final List<AnnotationInfoType> annoList = annosType.getAnnotation();
            final AnnotationInfoType ait = new AnnotationInfoType();
            annoList.add(ait);

            Type type = Type.getType(desc);
            if (type != null) {
                String processedName = AsmHelper.normalizeClassName(type.getClassName());
                ait.setName(AsmHelper.extractSimpleClassName(processedName));
                ait.setType(processedName);
            }

            return new CAAnnotationVisitor(ait, desc, visible);
        }

        @Override
        public void visitAttribute(Attribute attr) {
            super.visitAttribute(attr);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            FieldsType ft = cit.getFields();
            if (ft == null) {
                ft = new FieldsType();
                cit.setFields(ft);
            }
            final List<FieldInfoType> fieldList = ft.getField();
            final FieldInfoType fit = new FieldInfoType();
            fieldList.add(fit);

            fit.setName(name); // Field Name
            if (desc != null) { // Field Type
                Type type = Type.getType(desc);
                fit.setType(AsmHelper.normalizeClassName(type.getClassName()));
            }

            // Field Modifiers
            ModifiersType modTypes = new ModifiersType();
            modTypes.getModifier().addAll(AsmHelper.resolveAsmOpcode(AsmHelper.RoleFilter.FIELD, (access)));
            fit.setModifiers(modTypes);

            fit.setIsSynthetic((access & Opcodes.ACC_SYNTHETIC) != 0);

            return new CAFieldVisitor(fit);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodsType mt = cit.getMethods();
            if (mt == null) {
                mt = new MethodsType();
                cit.setMethods(mt);;
            }
            final List<MethodInfoType> methodList = mt.getMethod();
            final MethodInfoType mit = new MethodInfoType();
            methodList.add(mit);

            mit.setMethodName(name); // Method Name
            mit.setIsCtor("<init>".equals(name)); // Check if ctor

            if (desc != null) {
                final Type type = Type.getType(desc);

                // Method Arguments
                Type[] argTypes = type.getArgumentTypes();
                if (argTypes != null && argTypes.length > 0) {
                    ParametersType pt = mit.getParameters();
                    if (pt == null) {
                        pt = new ParametersType();
                        mit.setParameters(pt);
                    }

                    final List<ParameterType> parmList = pt.getParameter();
                    for (Type argType : argTypes) {
                        final ParameterType parmType = new ParameterType();
                        parmList.add(parmType);

                        parmType.setType(AsmHelper.normalizeClassName(argType.getClassName()));
                        parmType.setIsPrimitive(AsmHelper.isPrimitiveType(argType));

                        if (argType.getSort() == Type.ARRAY) {
                            parmType.setIsArray(true);
                            parmType.setArrayDimensions(BigInteger.valueOf(argType.getDimensions()));
                            parmType.setType(AsmHelper.normalizeClassName(argType.getElementType().getClassName()));
                        } else {
                            parmType.setIsArray(false);
                        }

                        parmType.setIsSynthetic(false);
                    }
                }

                // Method Return Type
                final Type returnType = type.getReturnType();
                mit.setReturnType(AsmHelper.normalizeClassName(returnType.getClassName()));
            }

            // Method Modifiers
            ModifiersType modTypes = new ModifiersType();
            modTypes.getModifier().addAll(AsmHelper.resolveAsmOpcode(AsmHelper.RoleFilter.METHOD, (access)));
            mit.setModifiers(modTypes);

            // Method Declared Exceptions
            if (exceptions != null && exceptions.length > 0) {
                ExceptionsType et = mit.getExceptions();
                if (et == null) {
                    et = new ExceptionsType();
                    mit.setExceptions(et);
                }

                final List<ExceptionType> exList = et.getException();
                for (String cName : exceptions) {
                    ExceptionType exType = new ExceptionType();
                    exList.add(exType);
                    exType.setExceptionType(AsmHelper.normalizeClassName(cName));
                }
            }

            return new CAMethodVisitor(mit);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }

        @Override
        public void visitOuterClass(String owner, String name, String desc) {
            super.visitOuterClass(owner, name, desc);

            // Name is the name of the method enclosing the class if there is one, null
            // otherwise.
            if (ioResolver != null) {
                ioResolver.addUnresolvedOuterClassReference(cit, AsmHelper.normalizeClassName(owner));
            }
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            super.visitInnerClass(name, outerName, innerName, access);

            if (outerName == null || !AsmHelper.normalizeClassName(outerName).equals(cit.getClassName())) {
                return;
            }

            InnerClassesType ict = cit.getInnerclasses();
            if (ict == null) {
                ict = new InnerClassesType();
                cit.setInnerclasses(ict);
            }
            final List<ClassInfoType> innerClassesList = ict.getInnerclass();

            ClassInfoType innerCit = new ClassInfoType();
            innerClassesList.add(innerCit);

            innerCit.setClassName(AsmHelper.normalizeClassName(name));

            if (ioResolver != null) {
                ioResolver.addUnresolvedInnerClassReference(cit, innerCit.getClassName());
            }
        }
    }

    private class CAFieldVisitor extends FieldVisitor {
        final FieldInfoType fit;

        public CAFieldVisitor(FieldInfoType fit) {
            super(ASM_LEVEL);
            this.fit = fit;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationsType annosType = fit.getAnnotations();
            if (annosType == null) {
                annosType = new AnnotationsType();
                fit.setAnnotations(annosType);
            }

            final List<AnnotationInfoType> annoList = annosType.getAnnotation();
            final AnnotationInfoType ait = new AnnotationInfoType();
            annoList.add(ait);

            final Type type = Type.getType(desc);
            if (type != null) {
                String processedName = AsmHelper.normalizeClassName(type.getClassName());
                ait.setName(AsmHelper.extractSimpleClassName(processedName));
                ait.setType(processedName);
            }

            return new CAAnnotationVisitor(ait, desc, visible);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
        }

        @Override
        public void visitAttribute(Attribute attr) {
            super.visitAttribute(attr);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }

    }

    private class CAMethodVisitor extends MethodVisitor {
        private final MethodInfoType mit;

        public CAMethodVisitor(MethodInfoType mit) {
            super(ASM_LEVEL);
            this.mit = mit;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationsType annosType = mit.getAnnotations();
            if (annosType == null) {
                annosType = new AnnotationsType();
                mit.setAnnotations(annosType);
            }

            final List<AnnotationInfoType> annoList = annosType.getAnnotation();
            final AnnotationInfoType ait = new AnnotationInfoType();
            annoList.add(ait);

            final Type type = Type.getType(desc);
            if (type != null) {
                String processedName = AsmHelper.normalizeClassName(type.getClassName());
                ait.setName(AsmHelper.extractSimpleClassName(processedName));
                ait.setType(processedName);
            }

            return new CAAnnotationVisitor(ait, desc, visible);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }
    }

    private class CAAnnotationVisitor extends AnnotationVisitor {
        private final AnnotationInfoType ait;
        private final String desc;
        private final boolean visible;

        final List<AnnotationValueType> eList;

        public CAAnnotationVisitor(AnnotationInfoType ait, String desc, boolean visible) {
            super(ASM_LEVEL);
            this.ait = ait;
            this.desc = desc;
            this.visible = visible;

            eList = ait.getAnnoKeyVal();
        }

        private AnnotationValueType newAVT() {
            final AnnotationValueType avt = new AnnotationValueType();
            eList.add(avt);
            return avt;
        }

        private AnnotationValueType newAVT(String name) {
            final AnnotationValueType avt = newAVT();
            avt.setName(name);
            return avt;
        }

        @Override
        public void visit(String name, Object value) {
            super.visit(name, value);

            final AnnotationValueType avt = newAVT(name);
            avt.setValue(AsmObjectValueAnalyzer.processValue(value));
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            super.visitEnum(name, desc, value);

            final AnnotationValueType avt = newAVT(name);
            avt.setValue(AsmObjectValueAnalyzer.processEnum(name, desc, value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            AnnotationVisitor av = super.visitAnnotation(name, desc);

            final AnnotationValueType avt = newAVT(name);
            final AnnotationInfoType ait = new AnnotationInfoType();
            avt.setAnnotation(ait);

            final Type type = Type.getType(desc);
            if (type != null) {
                String processedName = AsmHelper.normalizeClassName(type.getClassName());
                ait.setName(AsmHelper.extractSimpleClassName(processedName));
                ait.setType(processedName);
            }

            return new CAAnnotationVisitor(ait, desc, visible);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            super.visitArray(name);

            final AnnotationValueType avt = newAVT(name);
            CAElementAnnotationVisitor av = new CAElementAnnotationVisitor(avt, name);
            return av;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }
    }

    private class CAElementAnnotationVisitor extends AnnotationVisitor {
        private final AnnotationValueType aet;
        private final String name;

        public CAElementAnnotationVisitor(AnnotationValueType aet, String name) {
            super(ASM_LEVEL);
            this.aet = aet;
            this.name = name;
        }

        @Override
        public void visit(String name, Object value) {
            super.visit(name, value);

            final ValueInstanceType vit = AsmObjectValueAnalyzer.processValue(value);

            ValueInstanceType arrVit = aet.getValue();
            if (arrVit == null) {
                arrVit = new ValueInstanceType();
                arrVit.setType(ValueType.ARRAY);
                aet.setValue(arrVit);
            }

            ArrayInstanceType arit = arrVit.getArray();
            if (arit == null) {
                arit = new ArrayInstanceType();
                arrVit.setArray(arit);
                arit.setLength(0);
                arit.setType(vit.getType());
            } else {
                if (arit.getEntry().size() > 0 && !vit.getType().equals(arit.getType())) {
                    arit.setType(ValueType.UNKNOWN);
                }
            }

            final List<ArrayEntryType> aetList = arit.getEntry();
            final int index = aetList.size();

            final ArrayEntryType aret = new ArrayEntryType();
            aret.setIndex(index);
            aret.setValue(vit);

            arit.getEntry().add(aret);
            arit.setLength(arit.getEntry().size());
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            super.visitEnum(name, desc, value);

            ValueInstanceType vit = AsmObjectValueAnalyzer.processEnum(name, desc, value);

            ValueInstanceType arrVit = aet.getValue();
            if (arrVit == null) {
                arrVit = new ValueInstanceType();
                arrVit.setType(ValueType.ARRAY);
                aet.setValue(arrVit);
            }

            ArrayInstanceType arit = arrVit.getArray();
            if (arit == null) {
                arit = new ArrayInstanceType();
                arrVit.setArray(arit);
                arit.setLength(0);
                arit.setType(vit.getType());
            } else {
                if (arit.getEntry().size() > 0 && !vit.getType().equals(arit.getType())) {
                    arit.setType(ValueType.UNKNOWN);
                }
            }

            final List<ArrayEntryType> aetList = arit.getEntry();
            final int index = aetList.size();

            final ArrayEntryType aret = new ArrayEntryType();
            aret.setIndex(index);
            aret.setValue(vit);

            arit.getEntry().add(aret);
            arit.setLength(arit.getEntry().size());
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            super.visitAnnotation(name, desc);

            AnnotationsType at = aet.getAnnotations();
            if (at == null) {
                at = new AnnotationsType();
                aet.setAnnotations(at);
            }

            final List<AnnotationInfoType> eList = at.getAnnotation();
            AnnotationInfoType ait = new AnnotationInfoType();
            eList.add(ait);

            ait.setName(name);

            final Type type = Type.getType(desc);
            if (type != null) {
                String processedName = AsmHelper.normalizeClassName(type.getClassName());
                ait.setName(AsmHelper.extractSimpleClassName(processedName));
                ait.setType(processedName);
            }

            return new CAAnnotationVisitor(ait, desc, false);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return super.visitArray(name);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }
    }
}

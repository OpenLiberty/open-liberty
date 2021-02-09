/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.info.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.info.internal.InfoVisitor_Annotation.AnnotationInfoVisitor;

// Visit rules:
//
// Class Visitor:
//   visit
//   [ visitSource ] [ visitOuterClass ]
//   ( visitAnnotation | visitAttribute )*
//   (visitInnerClass | visitField | visitMethod )*
//   visitEnd
//
//   visitAnnotation --> AnnotationVisitor
//   visitField --> FieldVisitor
//   visitMethod --> MethodVisitor
//
// Field Visitor:
//   ( visitAnnotation | visitAttribute )*
//   visitEnd
//
//   visitAnnotation --> AnnotationVisitor
//
// Method Visitor:
//   [ visitAnnotationDefault ]
//   ( visitAnnotation | visitParameterAnnotation | visitAttribute )*
//   [ visitCode ( visitXInsn | visitLabel | visitTryCatchBlock | visitLocalVariable | visitLineNumber)* visitMaxs ]
//   visitEnd
//
//   visitAnnotationDefault --> AnnotationVisitor
//   visitAnnotation --> AnnotationVisitor
//   visitParameterAnnotation --> AnnotationVisitor
//
// Annotation Visitor:
//   (visit | visitEnum | visitAnnotation | visitArray)*
//   visitEnd
//
//   visitAnnotation --> AnnotationVisitor
//   visitArray --> AnnotationVisitor

// Steps for visiting annotations, annotation values, and annotation default values.
//
// The direct annotation targets are packages, classes, methods, and fields.
//
// Indirect annotation targets are method parameters, annotation child values,
// and annotation method default values.
//
// The direct target cases are initiated by a call to 'visitAnnotation'.
//
// The parameter annotation cases are initiated by a call to 'visitParameterAnnotation'.
//
// The annotation method default cases are initiated by a call to 'visitAnnotationDefault'.
//
// All cases use a new visitor instance, typed as 'InfoVisitor_Annotation'.
//
// TODO: The top level annotation visitor could be reused.  New annotation visitor
// instances need only be created for handling child annotation cases.  At this
// time, the extra cost for creating a new visitor for the top level entries is
// not high enough to put in code to use a shared top level visitor.
//
// The three annotation visitor cases have different parameters:
//
// The direct cases have an implied parameter -- the current package, class, field,
// or method -- and two actual parameters -- the annotation description and the
// annotation visibility.
//
// The annotation description encodes the annotation class name.
//
// The annotation visibility tells the runtime visibility of the annotation type.
// The visibility value is not used at this time.  All annotation occurrences
// are processed and recorded.
//
// The parameter case adds an additional parameter: The name of the method
// parameter to which to attach the annotation occurrence.
//
// The annotation method default value case has no parameters.
//
// For direct targeted annotations, a new annotation info object is created
// using the annotation class name which was obtained from the description
// parameter, and the new annotation info object is attached as a declared
// annotation to the annotation target.  Attaching the new annotation info
// causes the annotation to be recorded as present on the target of the
// annotation.
//
// For parameter annotations, a new annotation info object is created and
// is stored in a method parameters annotations table of the method info
// object.  No record is made of parameter annotation occurrences.  That is,
// there is no query mechanism provided for parameter annotation occurrences.
//
// For the annotation method default value case, no additional annotation
// related values are supplied to the annotation default value visitor.
// However, to provide a link to the info store context of the processing, the
// new annotation default value visitor is created with a reference to the
// parent visitor.
//
// Additional processing, as driven by the ASM class reader, occurs through
// the returned annotation visitor.

public class InfoVisitor extends ClassVisitor {
    private static final TraceComponent tc = Tr.register(InfoVisitor.class);
    public static final String CLASS_NAME = InfoVisitor.class.getName();

    //
    class InfoMethodVisitor extends MethodVisitor {
        private MethodInfoImpl methodInfo;
        private List<AnnotationInfoImpl> annotations;
        private List<AnnotationInfoImpl>[] paramAnnotations;

        public InfoMethodVisitor() {
            super(Opcodes.ASM8);
        }

        @SuppressWarnings("unchecked")
        void setMethodInfo(MethodInfoImpl mii) {
            methodInfo = mii;
            annotations = new LinkedList<AnnotationInfoImpl>();
            paramAnnotations = new List[mii.getParameterTypeNames().size()];
        }

        // This is necessary for use of the receiver as a method visitor.
        //
        // Processing of a default annotation creates a new (visit ... visitEnd) sequence,
        // which must be ended in a visitor other than the core info visitor.  Otherwise,
        // there will be an extra 'visitEnd', which will clear an extra level of data
        // of the core visitor.
        //
        // Prime the method info with a new annotation value.  That new annotation value
        // is retrieved by the annotation visitor and is used to store the method default
        // value.

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            final MethodInfoImpl mii = methodInfo;
            return new InfoVisitor_Annotation(mii.getInfoStore()) {
                @Override
                protected void storeAnnotationValue(String name, AnnotationValueImpl newAnnotationValue) {
                    mii.setAnnotationDefaultValue(newAnnotationValue);
                }
            };
        }

        /** {@inheritDoc} */
        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationInfoVisitor av = InfoVisitor.visitAnnotation(methodInfo, desc, visible);
            annotations.add(av.getAnnotationInfo());
            return av;
        }

        // This is necessary for use of the receiver as a method visitor.
        //
        // Processing of the parameter annotation creates a new (visit ... visitEnd) sequence,
        // which must be ended in a visitor other than the core info visitor.  Otherwise,
        // there will be an extra 'visitEnd', which will clear an extra level of data
        // of the core visitor.

        @Override
        public AnnotationVisitor visitParameterAnnotation(int param, String desc, boolean visible) {
            Type annotationType = Type.getType(desc);
            String annotationClassName = annotationType.getClassName();

            AnnotationInfoImpl annotationInfo = new AnnotationInfoImpl(annotationClassName, getInfoStore());
            List<AnnotationInfoImpl> annoList = paramAnnotations[param];
            if (annoList == null) {
                annoList = new LinkedList<AnnotationInfoImpl>();
                paramAnnotations[param] = annoList;
            }
            annoList.add(annotationInfo);

            return new InfoVisitor_Annotation.AnnotationInfoVisitor(annotationInfo);
        }

        @Override
        public void visitEnd() {
            if (logParms != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, MessageFormat.format("[ {0} ] Method [ {1} ]", logParms));
                }

                logParms[1] = methodInfo.getDeclaringClass().getName();
                logParms[2] = methodInfo.getDeclaringClass().getHashText();
            }

            methodInfo.setParameterAnnotations(paramAnnotations);
            methodInfo.setDeclaredAnnotations(annotations);

            this.methodInfo = null;
            paramAnnotations = null;
            annotations = null;
        }
    }

    class InfoFieldVisitor extends FieldVisitor {
        private FieldInfoImpl fieldInfo;
        private List<AnnotationInfoImpl> annotations;

        public InfoFieldVisitor() {
            super(Opcodes.ASM8);
        }

        void setFieldInfo(FieldInfoImpl fii) {
            fieldInfo = fii;
            annotations = new LinkedList<AnnotationInfoImpl>();
        }

        /** {@inheritDoc} */
        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationInfoVisitor av = InfoVisitor.visitAnnotation(fieldInfo, desc, visible);
            annotations.add(av.getAnnotationInfo());
            return av;

        }

        /** {@inheritDoc} */
        @Override
        public void visitEnd() {
            if (logParms != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, MessageFormat.format("[ {0} ] Field [ {1} ]", logParms));
                }
                logParms[1] = fieldInfo.getDeclaringClass().getName();
                logParms[2] = fieldInfo.getDeclaringClass().getHashText();
            }

            fieldInfo.setDeclaredAnnotations(annotations);

            fieldInfo = null;
            annotations = null;
        }
    }

    private final InfoMethodVisitor methodVisitor = new InfoMethodVisitor();
    private final InfoFieldVisitor fieldVisitor = new InfoFieldVisitor();

    protected final String hashText;

    @Trivial
    public String getHashText() {
        return hashText;
    }

    protected final Object[] logParms;

    //

    protected static String fixName(String name) {
        return ((name == null) ? null : name.replace('/', '.'));
    }

    protected static String[] fixNames(String[] names) {
        String[] fixedNames = new String[names.length];

        for (int nameNo = 0; nameNo < names.length; nameNo++) {
            fixedNames[nameNo] = fixName(names[nameNo]);
        }

        return fixedNames;
    }

    //

    public InfoVisitor(InfoStoreImpl infoStore, String externalName) {
        super(Opcodes.ASM8);

        this.infoStore = infoStore;
        this.externalName = externalName;

        this.hashText = getClass().getName() + "@" + Integer.toString((new Object()).hashCode());

        if (tc.isDebugEnabled()) {
            this.logParms = new Object[] { getHashText(),
                                          getInfoStore().getHashText(),
                                          this.externalName };

            Tr.debug(tc, MessageFormat.format("[ {0} ] Info Store [ {1} ] External [ {2} ]",
                                              logParms));

        } else {
            this.logParms = null;
        }
    }

    //

    protected final InfoStoreImpl infoStore;

    public InfoStoreImpl getInfoStore() {
        return infoStore;
    }

    //

    // Either the package info is set, or the class info is set,
    // or the class info and one of the field info or the method info is set.

    private PackageInfoImpl packageInfo;
    private NonDelayedClassInfo classInfo;
    private final List<FieldInfoImpl> fieldInfos = new LinkedList<FieldInfoImpl>();
    private final List<MethodInfoImpl> constructorInfos = new LinkedList<MethodInfoImpl>();
    private final List<MethodInfoImpl> methodInfos = new LinkedList<MethodInfoImpl>();
    private final List<AnnotationInfoImpl> annotationInfos = new LinkedList<AnnotationInfoImpl>();

    public InfoImpl getInfo() {
        if (this.packageInfo != null) {
            return this.packageInfo;
        } else {
            return this.classInfo;
        }
    }

    //

    protected final String externalName;

    public String getExternalName() {
        return externalName;
    }

    //

    @Override
    public void visit(int version, int access, String partialResourceName, String signature, String superName, String[] interfaceNames) {
        String name = fixName(partialResourceName);

        if (PackageInfoImpl.isPackageName(name)) {
            String packageName = PackageInfoImpl.stripPackageNameFromClassName(name);
            visitPackage(version, access, packageName);

        } else {
            visitClass(version, access, name, signature, superName, interfaceNames);
        }
    }

    protected void visitPackage(int version, int access, String packageName) {
        String useExternalName = getExternalName();
        if (PackageInfoImpl.isPackageName(useExternalName)) {
            useExternalName = PackageInfoImpl.stripPackageNameFromClassName(useExternalName);
        }

        if (this.logParms != null) {
            logParms[1] = packageName;
            Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ] ENTER External name [ {2} ]",
                                              logParms));
        }

        if (!packageName.equals(useExternalName)) {
            if (logParms != null) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ] RETURN External name [ {2} ] mismatch",
                                                  logParms));
            }
            throw VISIT_ENDED_PACKAGE_MISMATCH;
        }

        if (logParms != null) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ] External name match; continuing",
                                              logParms));
        }

        InfoStoreImpl useInfoStore = getInfoStore();

        if (useInfoStore.basicGetPackageInfo(packageName) != null) {
            Tr.warning(tc, "ANNO_INFOVISITOR_VISIT1", getHashText(), packageName); // CWWKC0038W

            String eMsg = "Duplicate package [" + packageName + "]";
            com.ibm.ws.ffdc.FFDCFilter.processException(new Exception(eMsg), CLASS_NAME, "379");

            if (logParms != null) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ] RETURN Duplicate package",
                                                  logParms));
            }
            throw VISIT_ENDED_DUPLICATE_PACKAGE;
        }

        this.packageInfo = getInfoStore().basicAddPackageInfo(packageName, access);

        if (logParms != null) {
            logParms[2] = this.packageInfo.getHashText();
            Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ] RETURN Move to package [ {2} ]",
                                              logParms));
        }
    }

    protected void visitClass(int version, int access, String name, String signature, String superName, String[] interfaceNames) {
        String useExternalName = getExternalName();

        if (this.logParms != null) {
            this.logParms[1] = name;
            Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ] External name [ {2} ]",
                                              logParms));
        }

        if (!name.equals(useExternalName)) {
            if (logParms != null) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ] RETURN External name [ {2} ] does not match",
                                                  logParms));
            }
            throw VISIT_ENDED_CLASS_MISMATCH;
        }

        if (logParms != null) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ] External name match; continuing",
                                              logParms));
        }

        this.classInfo = getInfoStore().createClassInfo(name, fixName(superName), access, fixNames(interfaceNames));

        if (logParms != null) {
            logParms[2] = this.classInfo.getHashText();
            Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ] RETURN Move to class [ {2} ]",
                                              logParms));
        }
    }

    // See the class comment for details of annotation processing.
    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationInfoVisitor av = visitAnnotation(getInfo(), desc, visible);
        annotationInfos.add(av.getAnnotationInfo());
        return av;
    }

    static AnnotationInfoVisitor visitAnnotation(InfoImpl info, String desc, boolean visible) {
        Type annotationType = Type.getType(desc);
        String annotationClassName = annotationType.getClassName();

        AnnotationInfoImpl annotationInfo = new AnnotationInfoImpl(annotationClassName, info.getInfoStore());
        return new InfoVisitor_Annotation.AnnotationInfoVisitor(annotationInfo);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object defaultValue) {
        if (name.toLowerCase().equals("enum$values")) {
            // This is the built-in values() method of every enumeration and
            // is an array type.  For some reason, the classloader can't
            // load that class, so we will ignore this field.

            return null;
        }

        FieldInfoImpl fieldInfo = new FieldInfoImpl(name, desc, access, defaultValue, classInfo);

        fieldInfos.add(fieldInfo);

        if (logParms != null) {
            logParms[1] = name;
            logParms[2] = fieldInfo.getHashText();
        }

        fieldVisitor.setFieldInfo(fieldInfo);
        return fieldVisitor;
    }

    // [ visitAnnotationDefault ]
    // ( visitAnnotation | visitParameterAnnotation | visitAttribute )*
    // [ visitCode ( visitXInsn | visitLabel | visitTryCatchBlock | visitLocalVariable | visitLineNumber)* visitMaxs ] visitEnd.

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String exceptions[]) {
        // skip static initializers
        if (name.equals("<clinit>")) {
            return null;
        }

        MethodInfoImpl methodInfo = new MethodInfoImpl(name, desc, exceptions, access, classInfo);

        if (name.equals("<init>")) {
            constructorInfos.add(methodInfo);
        } else {
            methodInfos.add(methodInfo);
        }

        if (logParms != null) {
            logParms[1] = name;
            logParms[2] = methodInfo.getHashText();
        }

        methodVisitor.setMethodInfo(methodInfo);
        return methodVisitor;
    }

    @Override
    public void visitEnd() {
        if (this.packageInfo != null) {
            visitEndPackage();

        } else if (this.classInfo != null) {
            visitEndClass();

        } else {
            Tr.warning(tc, "ANNO_INFOVISITOR_VISIT4", getHashText()); // CWWKC0041W
            com.ibm.ws.ffdc.FFDCFilter.processException(new Exception("The visitor object can not be identified"), "com.ibm.wsspi.anno.info.internal.InfoVisitor", "559");
        }
    }

    public void visitEndPackage() {
        if (logParms != null) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] Package [ {1} ]", logParms));
        }

        packageInfo.setDeclaredAnnotations(annotationInfos);

        packageInfo = null;
    }

    public void visitEndClass() {
        if (logParms != null) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] ENTER Class [ {1} ]", logParms));
        }

        classInfo.setFields(new ArrayList<FieldInfoImpl>(fieldInfos));
        classInfo.setConstructors(new ArrayList<MethodInfoImpl>(constructorInfos));
        classInfo.setMethods(new ArrayList<MethodInfoImpl>(methodInfos));
        classInfo.setDeclaredAnnotations(new ArrayList<AnnotationInfoImpl>(annotationInfos));

        boolean didAdd = getInfoStore().addClassInfo(this.classInfo);

        if (!didAdd) {
            if (logParms != null) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] RETURN Already added [ {1} ]", logParms));
            }

        } else {
            if (logParms != null) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] Added [ {1} ]", logParms));
                Tr.debug(tc, MessageFormat.format("[ {0} ] Applying rules to class [ {1} ]", logParms));
            }

            // Process inherited values:
            //
            // There are three cases:
            //
            // 1) Inherited class annotations.
            //    -- Class annotations defined with the @Inherited meta-annotation.
            //    -- Such class annotations are inherited by subclasses, except when the subclass
            //       provides a new occurrence of the annotation.
            //    -- All subclasses of a class that has an attached inherited class annotation
            //       will have that class annotation.
            //
            // 2) Annotations on inherited methods.
            //    -- Fundamentally different than inheritance of class annotations.
            //    -- The notion of inheritable annotations of methods is not a part of the
            //       java annotations framework.
            //    -- A method which is inherited carries with it its annotations.
            //    -- If the method is overridden, the new method implementation provides
            //       entirely new annotations.  All of the annotations on the method in
            //       the superclass are replaced.  (If the new method implementation has no
            //       annotations, then still none of the annotations are acquired from the
            //       method on the superclass.)
            //
            // 3) Annotations on inherited fields.
            //    -- Fundamentally different than inheritance of class annotations, and
            //       the precisely the same as annotations on inherited methods.
            //
            // Additional notes:
            //
            // *) Annotations associated with interfaces are never inherited.
            //
            // *) Domain specific standards (for example, the JavaEE EJB specification), may define
            //    processing rules which require a view of the annotations on a class and on its superclass.
            //    Such rules are extra to the base java annotations rules.

            // Inheritance processing visits the super classes of the current class.  All processing
            // (other than these processing rules) must be complete before performing the inheritance
            // processing.  Normal processing steps must not branch to referenced classes, which may
            // include the current class.  Branching to referenced classes, if performed, would cause
            // an infinite loop.

            if (logParms != null) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] RETURN Applied rules to class [ {1} ]", logParms));
            }
        }

        classInfo = null;
    }

    //

    public static enum VisitEndCase {
        VISIT_END_DUPLICATE_CLASS,
        VISIT_END_CLASS_MISMATCH,
        VISIT_END_DUPLICATE_PACKAGE,
        VISIT_END_PACKAGE_MISMATCH;
    }

    public static final VisitEnded VISIT_ENDED_DUPLICATE_CLASS = new VisitEnded(VisitEndCase.VISIT_END_DUPLICATE_CLASS);
    public static final VisitEnded VISIT_ENDED_CLASS_MISMATCH = new VisitEnded(VisitEndCase.VISIT_END_CLASS_MISMATCH);
    public static final VisitEnded VISIT_ENDED_DUPLICATE_PACKAGE = new VisitEnded(VisitEndCase.VISIT_END_DUPLICATE_PACKAGE);
    public static final VisitEnded VISIT_ENDED_PACKAGE_MISMATCH = new VisitEnded(VisitEndCase.VISIT_END_PACKAGE_MISMATCH);

    public static class VisitEnded extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public VisitEnded(VisitEndCase endCase) {
            super();

            this.endCase = endCase;
        }

        // Overload: This is an expensive step, and problematic for
        //           use with the static, which won't have a useful
        //           stack anyways.

        @Override
        public Throwable fillInStackTrace() {
            return null;
        }

        //

        protected final VisitEndCase endCase;

        public VisitEndCase getEndCase() {
            return endCase;
        }
    }
}

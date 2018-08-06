/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.info.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.ibm.ws.anno.info.internal.AnnotationVisitorImpl_Info.AnnotationInfoVisitor;
import com.ibm.ws.anno.util.internal.UtilImpl_EmptyCollections;

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

public class ClassVisitorImpl_Info extends ClassVisitor {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.anno.info");

    private static final String CLASS_NAME = "ClassVisitorImpl_Info";

    //
    class InfoMethodVisitor extends MethodVisitor {
        private MethodInfoImpl methodInfo;
        private List<AnnotationInfoImpl> annotations;
        private List<AnnotationInfoImpl>[] paramAnnotations;

        public InfoMethodVisitor() {
            super(Opcodes.ASM7_EXPERIMENTAL);
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
            return new AnnotationVisitorImpl_Info(mii.getInfoStore()) {
                @Override
                protected void storeAnnotationValue(String name, AnnotationValueImpl newAnnotationValue) {
                    mii.setAnnotationDefaultValue(newAnnotationValue);
                }
            };
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationInfoVisitor av = ClassVisitorImpl_Info.visitAnnotation(methodInfo, desc, visible);
            annotations.add(av.getAnnotationInfo());
            return av;
        }

        @Override
        public void visitParameter(String parmName, int parmNo) {
            super.visitParameter(parmName, parmNo);
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

            return new AnnotationVisitorImpl_Info.AnnotationInfoVisitor(annotationInfo);
        }

        @Override
        public void visitEnd() {
            String methodName = "visitEnd";

            if (logParms != null) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Method [ {1} ]", logParms);
                }

                logParms[1] = methodInfo.getDeclaringClass().getName();
                logParms[2] = methodInfo.getDeclaringClass().getHashText();
            }

            AnnotationInfoImpl[][] arrAnnos = new AnnotationInfoImpl[paramAnnotations.length][];

            for (int i = 0; i < paramAnnotations.length; ++i) {
                List<AnnotationInfoImpl> annos = paramAnnotations[i];

                AnnotationInfoImpl[] aAnnos;
                if (annos == null || annos.isEmpty()) {
                    aAnnos = UtilImpl_EmptyCollections.emptyAnnotationInfoArray;
                } else {
                    aAnnos = annos.toArray(new AnnotationInfoImpl[annos.size()]);
                }

                arrAnnos[i] = aAnnos;
            }
            methodInfo.setParameterAnnotations(arrAnnos);

            AnnotationInfoImpl[] annos = UtilImpl_EmptyCollections.emptyAnnotationInfoArray;
            if (annotations.size() > 0) {
                annos = annotations.toArray(new AnnotationInfoImpl[annotations.size()]);
            }
            methodInfo.setDeclaredAnnotations(annos);

            this.methodInfo = null;
            paramAnnotations = null;
            annotations = null;
        }
    }

    class InfoFieldVisitor extends FieldVisitor {
        private FieldInfoImpl fieldInfo;
        private List<AnnotationInfoImpl> annotations;

        public InfoFieldVisitor() {
            super(Opcodes.ASM7_EXPERIMENTAL);
        }

        void setFieldInfo(FieldInfoImpl fii) {
            fieldInfo = fii;
            annotations = new LinkedList<AnnotationInfoImpl>();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationInfoVisitor av = ClassVisitorImpl_Info.visitAnnotation(fieldInfo, desc, visible);
            annotations.add(av.getAnnotationInfo());
            return av;

        }

        @Override
        public void visitEnd() {
            String methodName = "visitEnd";

            if (logParms != null) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Field [ {1} ]", logParms);
                }
                logParms[1] = fieldInfo.getDeclaringClass().getName();
                logParms[2] = fieldInfo.getDeclaringClass().getHashText();
            }

            AnnotationInfoImpl[] annos = UtilImpl_EmptyCollections.emptyAnnotationInfoArray;
            if (annotations.size() > 0) {
                annos = annotations.toArray(new AnnotationInfoImpl[annotations.size()]);
            }
            fieldInfo.setDeclaredAnnotations(annos);

            fieldInfo = null;
            annotations = null;
        }
    }

    private final InfoMethodVisitor methodVisitor = new InfoMethodVisitor();
    private final InfoFieldVisitor fieldVisitor = new InfoFieldVisitor();

    protected final String hashText;

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
        super(Opcodes.ASM7_EXPERIMENTAL);

        this.infoStore = infoStore;
        this.externalName = externalName;

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        if (logger.isLoggable(Level.FINER)) {
            this.logParms = new Object[] { getHashText(),
                                          getInfoStore().getHashText(),
                                          this.externalName };

            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Info Store [ {1} ] External [ {2} ]", logParms);

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
    private NonDelayedClassInfoImpl classInfo;
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
        String methodName = "visitPackage";

        String useExternalName = getExternalName();
        if (PackageInfoImpl.isPackageName(useExternalName)) {
            useExternalName = PackageInfoImpl.stripPackageNameFromClassName(useExternalName);
        }

        if (this.logParms != null) {
            logParms[1] = packageName;
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] [ {1} ] ENTER External name [ {2} ]", logParms);
        }

        if (!packageName.equals(useExternalName)) {
            if (logParms != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] [ {1} ] RETURN External name [ {2} ] mismatch", logParms);
            }
            throw VISIT_ENDED_PACKAGE_MISMATCH;
        }

        if (logParms != null) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] [ {1} ] External name match; continuing", logParms);
        }

        InfoStoreImpl useInfoStore = getInfoStore();

        if (useInfoStore.basicGetPackageInfo(packageName) != null) {
            logger.logp(Level.WARNING, CLASS_NAME, methodName,
                        "[ {0} ] ANNO_INFOVISITOR_VISIT1 {1} ]",
                        new Object[] { getHashText(), packageName }); // CWWKC0038W

            String eMsg = "Duplicate package [" + packageName + "]";
            logger.logp(Level.WARNING, CLASS_NAME, methodName, eMsg);

            if (logParms != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] [ {1} ] RETURN Duplicate package", logParms);
            }
            throw VISIT_ENDED_DUPLICATE_PACKAGE;
        }

        this.packageInfo = getInfoStore().basicAddPackageInfo(packageName, access);

        if (logParms != null) {
            logParms[2] = this.packageInfo.getHashText();
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] [ {1} ] RETURN Move to package [ {2} ]", logParms);
        }
    }

    protected void visitClass(int version, int access, String name, String signature, String superName, String[] interfaceNames) {
        String methodName = "visitClass";

        String useExternalName = getExternalName();

        if (this.logParms != null) {
            this.logParms[1] = name;
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] [ {1} ] External name [ {2} ]", logParms);
        }

        if (!name.equals(useExternalName)) {
            if (logParms != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] [ {1} ] RETURN External name [ {2} ] does not match", logParms);
            }
            throw VISIT_ENDED_CLASS_MISMATCH;
        }

        if (logParms != null) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] [ {1} ] External name match; continuing", logParms);
        }

        this.classInfo = getInfoStore().createClassInfo(name, fixName(superName), access, fixNames(interfaceNames));

        if (logParms != null) {
            logParms[2] = this.classInfo.getHashText();
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] [ {1} ] RETURN Move to class [ {2} ]", logParms);
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
        return new AnnotationVisitorImpl_Info.AnnotationInfoVisitor(annotationInfo);
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

// public static final int ACC_BRIDGE = 0x00000040;
// public static final int ACC_SYNTHETIC = 0x00001000;

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String exceptions[]) {
        // skip static initializers
        if (name.equals("<clinit>")) {
            return null;
        }

// Methods overridden and given a specialized return type cause the addition of a SYNTHETIC BRIDGE method
// In the subtype.
//
// For example:
//     Number Super.getValue();
//     Integer Sub.getValue();
//
// The class file of "Sub" contains a synthetic, bridge, implementation of "Number getValue()".
//
//        if ( (access & ACC_BRIDGE) != 0 ) {
//            System.out.println("BRIDGE Method [ " + name + " ] [ " + signature + " ]");
//        }
//        if ( (access & ACC_SYNTHETIC) != 0 ) {
//            System.out.println("SYNTHETIC Method [ " + name + " ] [ " + signature + " ]");
//        }

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
        String methodName = "visitEnd";

        if (this.packageInfo != null) {
            visitEndPackage();

        } else if (this.classInfo != null) {
            visitEndClass();

        } else {
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "[ {0} ] ANNO_INFOVISITOR_VISIT4", getHashText());
        }
    }

    public void visitEndPackage() {
        String methodName = "visitEndPackage";
        if (logParms != null) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Package [ {1} ]", logParms);
        }

        packageInfo.setDeclaredAnnotations(annotationInfos.toArray(new AnnotationInfoImpl[annotationInfos.size()]));

        packageInfo = null;
    }

    public void visitEndClass() {
        String methodName = "visitEndClass";
        if (logParms != null) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER Class [ {1} ]", logParms);
        }

        classInfo.setFields(fieldInfos.toArray(new FieldInfoImpl[fieldInfos.size()]));
        classInfo.setConstructors(constructorInfos.toArray(new MethodInfoImpl[constructorInfos.size()]));
        classInfo.setMethods(methodInfos.toArray(new MethodInfoImpl[methodInfos.size()]));
        classInfo.setDeclaredAnnotations(annotationInfos.toArray(new AnnotationInfoImpl[annotationInfos.size()]));

        boolean didAdd = getInfoStore().addClassInfo(this.classInfo);

        if (!didAdd) {
            if (logParms != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN Already added [ {1} ]", logParms);
            }

        } else {
            if (logParms != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Added [ {1} ]", logParms);
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Applying rules to class [ {1} ]", logParms);
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
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN Applied rules to class [ {1} ]", logParms);
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

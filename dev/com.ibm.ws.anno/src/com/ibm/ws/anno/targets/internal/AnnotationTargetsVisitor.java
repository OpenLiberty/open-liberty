/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.targets.internal;

import java.text.MessageFormat;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets.AnnotationCategory;
import com.ibm.wsspi.anno.util.Util_InternMap;

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
//
// [ visitAnnotationDefault ]
// ( visitAnnotation | visitParameterAnnotation | visitAttribute )*
// [ visitCode ( visitXInsn | visitLabel | visitTryCatchBlock | visitLocalVariable | visitLineNumber)* visitMaxs ] visitEnd.

public class AnnotationTargetsVisitor extends ClassVisitor {
    // Trace ...

    private static final TraceComponent tc = Tr.register(AnnotationTargetsVisitor.class);

    public static final String CLASS_NAME = AnnotationTargetsVisitor.class.getName();

    protected void reportFFDC(String eMsg, String eLine) {
        com.ibm.ws.ffdc.FFDCFilter.processException(new Exception(eMsg), CLASS_NAME, eLine);
    }

    // Static package naming helpers ...

    public static final String PACKAGE_INFO_CLASS_NAME = "package-info";

    public static boolean isPackageName(String className) {
        return (className.endsWith(PACKAGE_INFO_CLASS_NAME));
    }

    public static String stripPackageNameFromClassName(String className) {
        return (className.substring(0, className.length() - (PACKAGE_INFO_CLASS_NAME.length() + 1)));
    }

    // Static resource naming helpers ...

    // TODO: The call through 'Type.getType' is rather inefficient, as the
    //       description is already very close to the required resource name.
    //
    //       That call should be inlined.

    protected static String getResourceNameFromDescription(String desc) {
        Type type = Type.getType(desc);
        String className = type.getClassName();

        String resourceName = className.replace(".", "/");

        return resourceName;
    }

    // TODO: This is a simplification of 'getResourceNameFromDescription', usable only
    //       for descriptions of annotation classes.  The simplification works because
    //       annotation classes cannot be typed as arrays or as simple types.  This
    //       simplification should be reviewed to make sure there are no outside cases
    //       which are not handled correctly.

    protected static String getClassResourceNameFromAnnotationDescription(String desc) {
        return desc.substring(1, desc.length() - 1);
    }

    protected static String getClassNameFromPartialResourceName(String partialResourceName) {
        return partialResourceName.replace("/", ".");
    }

    // Top of the world ...

    // The annotation targets visitor is coded to maintain visitor state, and to translate
    // visitor data into annotation targets data.
    //
    // Before processing a class, the class source name, scan policy, and external name
    // of the class must be set into the visitor.
    //
    // These values are not already set since a single visitor instance is used for
    // an entire scan sweep.

    public AnnotationTargetsVisitor(AnnotationTargetsImpl_Targets annotationTargets) {
        super(Opcodes.ASM8);

        this.hashText = AnnotationServiceImpl_Logging.getBaseHash(this);

        // Basic identity: Bind the visitor to specific targets,
        // and cache some referenced values of those targets.

        this.annotationTargets = annotationTargets;
        this.isDetailEnabled = annotationTargets.getIsDetailEnabled();

        // Values set prior to beginning a new class or package:
        //
        // this.i_classSourceName;
        //
        // this.scanPolicy;
        // this.scanPolicyIsExternal;
        //
        // this.externalName;

        // Values set while processing a class or package:
        //
        // isClass
        // i_className

        if (tc.isDebugEnabled()) {
            String trMsg = MessageFormat.format("[ {0} ] Created on [ {1} ]",
                                                getHashText(),
                                                getAnnotationTargets().getHashText());
            Tr.debug(tc, trMsg);
        }
    }

    // Trace ...

    protected final String hashText;

    public String getHashText() {
        return hashText;
    }

    // The target which is updated by this visitor and associated methods.

    protected final AnnotationTargetsImpl_Targets annotationTargets;
    protected final boolean isDetailEnabled; // This enables field and method targets.

    public AnnotationTargetsImpl_Targets getAnnotationTargets() {
        return annotationTargets;
    }

    protected boolean isDetailEnabled() {
        return isDetailEnabled;
    }

    protected String internClassName(String name) {
        return getAnnotationTargets().internClassName(name);
    }

    protected String internClassName(String name, boolean doForce) {
        return getAnnotationTargets().internClassName(name, doForce);
    }

    // START: 69175
    //
    // Field and method names should not be interned.
    // Those names are not used as in the targets table.

    // public String internField(String name) {
    //     return getAnnotationTargets().internFieldName(name);
    // }

    // public String internMethodName(String name) {
    //     return getAnnotationTargets().internMethodName(name);
    // }

    // END: 69175

    // The class source of a class or package which is to be scanned.  The class
    // source must be specified before processing a new class or package.
    //
    // The class source name is used when recording classes
    // and class hierarchy information.

    protected String i_classSourceName;

    protected void i_setClassSourceName(String use_i_classSourceName) {
        this.i_classSourceName = use_i_classSourceName;
    }

    protected String i_getClassSourceName() {
        return i_classSourceName;
    }

    // The policy of the class source of a class or package which is to
    // be scanned.  The policy must be set before processing a new class.
    //
    // The scan policy is used when recording classes and annotations.

    protected ScanPolicy scanPolicy;
    protected boolean scanPolicyIsExternal;

    protected void setScanPolicy(ScanPolicy scanPolicy) {
        this.scanPolicy = scanPolicy;
        this.scanPolicyIsExternal = (this.scanPolicy == ScanPolicy.EXTERNAL);
    }

    protected ScanPolicy getScanPolicy() {
        return scanPolicy;
    }

    protected boolean scanPolicyIsExternal() {
        return scanPolicyIsExternal;
    }

    // The class name as computed from the resource which provided the class
    // input stream.  This is a path from a root location, for example,
    // "com/company/package/class.class" relative to "WEB-INF/classes".
    // 
    // The class name is also recorded inside the class byte data.
    //
    // The resource based class name (the "external" name) must match
    // the class byte data based name (the "internal" name).

    protected String externalName;

    public void setExternalName(String externalName) {
        this.externalName = externalName;
    }

    private String getExternalName() {
        return externalName;
    }

    // Values set while processing a class or package.  These are recorded during
    // the initial visit call and are used by the record operations.

    // Setting of whether the current target is for a package or for a package.
    //
    // Needed because package and class annotations are stored separately.

    protected boolean isClass;

    // The interned name of the class or package.  Annotations data is stored in
    // associated with this name.

    protected String i_className; // A package name when 'isClass' is false!

    // Main record methods ...

    protected boolean i_recordScannedClassName(String use_i_className) {
        boolean didPlaceClass = annotationTargets.i_placeClass(i_getClassSourceName(), use_i_className);
        if (!didPlaceClass) {
            return false;
        }

        boolean didAddClass = annotationTargets.i_addScannedClassName(use_i_className, getScanPolicy());
        return didAddClass;
    }

    protected void i_recordSuperclassName(String use_i_className, String use_i_superclassName) {
        annotationTargets.i_setSuperclassName(use_i_className, use_i_superclassName);
    }

    protected void i_recordInterfaceNames(String use_i_className, String[] i_interfaceNames) {
        annotationTargets.i_setInterfaceNames(use_i_className, i_interfaceNames);
    }

    protected void i_recordReferencedClassName(String use_i_className) {
        annotationTargets.i_addReferencedClassName(use_i_className);
    }

    protected void i_removeReferencedClassName(String use_i_className) {
        annotationTargets.i_removeReferencedClassName(use_i_className);
    }

    protected void recordAnnotation(AnnotationCategory annotationCategory, String desc) {
        // Skip class annotations if only enabled for partial scans.

        String annotationResourceName = getClassResourceNameFromAnnotationDescription(desc);

        String annotationClassName = getClassNameFromPartialResourceName(annotationResourceName);

        String i_annotationClassName = internClassName(annotationClassName);

        annotationTargets.i_recordAnnotation(getScanPolicy(), annotationCategory, i_className, i_annotationClassName);
    }

    // Main visit entry point ... package and class processing begin with a call to visit.
    //
    // See the main class comments for the visit sequence.
    //
    // Expected followups are to visitAnnotation, visitField, visitMethod, and finally
    // visitEnd.  (Only visitEnd is guaranteed.)
    //
    // Note that the names are resource names.

    @Override
    public void visit(int version,
                      int access,
                      String classResourceName,
                      String signature,
                      String superClassResourceName,
                      String interfaceResourceNames[]) {

        Object[] logParms;
        if (tc.isDebugEnabled()) {
            logParms = new Object[] { getHashText(), classResourceName };
            Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ]", logParms));
        } else {
            logParms = null;
        }

        String className = getClassNameFromPartialResourceName(classResourceName);

        if (logParms != null) {
            logParms[1] = className + ".class";
        }

        if (isPackageName(className)) {
            if ((logParms != null) && tc.isDebugEnabled()) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ] Package load", logParms));
            }

            isClass = false; // Need to remember this for when recording package annotations.

            className = stripPackageNameFromClassName(className);

            String externalClassName = stripPackageNameFromClassName(getExternalName());

            // Note: The package is NOT recorded as a scanned package.
            //       if the package name does not match.

            if (!className.equals(externalClassName)) {
                //  When allowable, we need to add a new message to the repository for the following warning...
                // Tr.warning(tc, "ANNO_TARGETS_CLASS_MISMATCH", getHashText(), externalClassName);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Class name mismatch", getHashText(), externalClassName);
                }
                throw VISIT_ENDED_PACKAGE_MISMATCH;
            }

            i_className = internClassName(className, Util_InternMap.DO_FORCE); // Remember this for recording annotations.

            // if ( !i_recordScannedPackageName(name) ) {
            //     name = null;
            //
            //     Tr.warning(tc, "ANNO_TARGETS_DUPLICATE_PACKAGE", getHashText(), name); // CWWKC0054W
            //     throw VISIT_ENDED_DUPLICATE_PACKAGE;
            // }

        } else {
            if (logParms != null && tc.isDebugEnabled()) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] [ {1} ] Class load", logParms));
            }

            isClass = true; // Need to remember this for when recording class annotations.

            // Problem here:
            //
            // JSP pre-compilation does not create the appropriate directory
            // structure for generated classes.
            //
            // However, when a class is not in its appropriate location, a call to
            // to detail the class information will fail to locate the class.
            //
            // That failure results in null class information, or results in artificial
            // class information.  Neither will obtain the annotation which is expected
            // to be present on the class.  That can lead to a null pointer exception
            // at WebAppConfiguratorHelper.configureServletAnnotation:2414.

            // Leaving this test in causes a failure of the servlet 3.1 tests;
            // Removing this test cases failures in the EBA WAB fats.

            if (!className.equals(getExternalName())) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Class name mismatch", getHashText(), className);
                }

                // Disabling the warning for now, as it breaks FAT tests.
                //
                // ANNO_TARGETS_CLASS_NAME_MISMATCH=
                // CWWKC0105W: 
                // Class scanning internal error: Visitor {0} read class name {1}
                // from a resource which should have class name {2}.
                // Tr.warning(tc, "ANNO_TARGETS_CLASS_NAME_MISMATCH",
                //     getHashText(), className, getExternalName() );

                throw VISIT_ENDED_CLASS_MISMATCH;
            }

            i_className = internClassName(className); // Remember this for recording annotations.

            if (!i_recordScannedClassName(i_className)) {
                // ANNO_TARGETS_DUPLICATE_CLASS=
                // CWWKC0055W: 
                // Class scanning internal error: The visitor [ {0} ] 
                // attempted a second scan of class [ {1} ].

                Tr.warning(tc, "ANNO_TARGETS_DUPLICATE_CLASS", getHashText(), i_className); // CWWKC055W
                throw VISIT_ENDED_DUPLICATE_CLASS;
            }

            i_removeReferencedClassName(i_className);

            if (superClassResourceName != null) {
                superClassResourceName = getClassNameFromPartialResourceName(superClassResourceName);

                String i_superclassName = internClassName(superClassResourceName);

                i_recordSuperclassName(i_className, i_superclassName);
                i_recordReferencedClassName(i_superclassName);
            }

            if ((interfaceResourceNames != null) && (interfaceResourceNames.length > 0)) {
                String[] i_interfaceNames = new String[interfaceResourceNames.length];

                for (int nameNo = 0; nameNo < interfaceResourceNames.length; nameNo++) {
                    String nextInterfaceResourceName = interfaceResourceNames[nameNo];

                    String nextInterfaceName = getClassNameFromPartialResourceName(nextInterfaceResourceName);

                    String i_nextInterfaceName = internClassName(nextInterfaceName);

                    i_interfaceNames[nameNo] = i_nextInterfaceName;
                }

                i_recordInterfaceNames(i_className, i_interfaceNames);

                for (String i_interfaceName : i_interfaceNames) {
                    i_recordReferencedClassName(i_interfaceName);
                }
            }
        }
    }

    // Exception used to stop visiting.  The exception is needed because
    // the visitor API as no setting to cause immediate return.

    public static enum VisitEndCase {
        VISIT_END_DETAIL,
        VISIT_END_DUPLICATE_CLASS,
        VISIT_END_CLASS_MISMATCH,
        VISIT_END_DUPLICATE_PACKAGE,
        VISIT_END_PACKAGE_MISMATCH;
    }

    public static final VisitEnded VISIT_ENDED_DETAIL = new VisitEnded(VisitEndCase.VISIT_END_DETAIL);
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

        public boolean isDetailCase() {
            return (getEndCase() == VisitEndCase.VISIT_END_DETAIL);
        }
    }

    // Visit a class or package annotation.  Record the annotation then return null to
    // complete the annotation processing.  Only the name of the class of the annotation
    // is recorded.

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationCategory category = (isClass ? AnnotationCategory.CLASS : AnnotationCategory.PACKAGE);

        recordAnnotation(category, desc);

        return null;
    }

    // Field visiting ...

    // Visitor for fields.  Tied to the current class visitor to
    // enable field annotation storage through the enclosing class.

    protected class AnnoFieldVisitor extends FieldVisitor {
        public AnnoFieldVisitor() {
            super(Opcodes.ASM8);
        }

        // A field annotation.  Needs to be recorded.

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            recordAnnotation(AnnotationCategory.FIELD, desc);
            return null;
        }

        // End visiting a field.  Just end this field visit stage,
        // don't end the class as a whole.

        @Override
        public void visitEnd() {
            // NO-OP
        }
    }

    protected final FieldVisitor fieldVisitor = new AnnoFieldVisitor();

    // Visit a field.  If detail is disabled, or if the class is an external class,
    // end the processing of the current class.  Otherwise, start a new stage of
    // visiting the field.

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object defaultValue) {
        if (scanPolicyIsExternal() || !isDetailEnabled()) {
            visitEnd();
            throw VISIT_ENDED_DETAIL;
        }

        return fieldVisitor;
    }

    // Method visiting ...

    // Visitor for methods.  Tied to the current class visitor to
    // enable method annotation storage through the enclosing class.

    protected class AnnoMethodVisitor extends MethodVisitor {
        public AnnoMethodVisitor() {
            super(Opcodes.ASM8);
        }

        // A method annotation.  Needs to be recorded.

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            recordAnnotation(AnnotationCategory.METHOD, desc);
            return null;
        }

        // Parameter annotations are not being recorded.

        @Override
        public AnnotationVisitor visitParameterAnnotation(int param, String desc, boolean visible) {
            return null;
        }

        // Don't care about annotation default values during the target scan.

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return null;
        }

        // End visiting a method.  Just end this method visit stage,
        // don't end the class as a whole.

        @Override
        public void visitEnd() {
            // NO-OP
        }
    }

    protected final MethodVisitor methodVisitor = new AnnoMethodVisitor();

    // Visit a method.  If detail is disabled, or if the class is an external class,
    // end the processing of the current class.  Otherwise, start a new stage of
    // visiting the method.

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String exceptions[]) {
        if (scanPolicyIsExternal() || !isDetailEnabled()) {
            visitEnd();
            throw VISIT_ENDED_DETAIL;
        }

        return methodVisitor;
    }

    // End visiting a class or package.

    @Override
    public void visitEnd() {
        // NO-OP
    }
}

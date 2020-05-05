/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.internal;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.util.internal.UtilImpl_IdentityStringSet;
import com.ibm.wsspi.annocache.util.Util_InternMap;

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

public class TargetsVisitorClassImpl extends ClassVisitor {
    // Trace ...

    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    public static final String CLASS_NAME = TargetsVisitorClassImpl.class.getSimpleName();

    // Static package naming helpers ...

    public static final String PACKAGE_INFO_CLASS_NAME = "package-info";

    public static boolean isPackageName(String className) {
        return (className.endsWith(PACKAGE_INFO_CLASS_NAME));
    }

    public static String stripPackageNameFromClassName(String className) {
        // if ( className == null ) {
        //     System.out.println("NULL");
        // }
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

    public static final Set<String> SELECT_ALL_ANNOTATIONS = null;
    public static final Set<String> SELECT_NO_ANNOTATIONS = Collections.emptySet();

    public static final boolean DO_RECORD_ANNOTATION_DETAIL = true;
    public static final boolean DO_NOT_RECORD_ANNOTATION_DETAIL = false;

    // The annotation targets visitor is coded to maintain visitor state, and to translate
    // visitor data into annotation targets data.
    //
    // Before processing a class, the class source name, scan policy, and external name
    // of the class must be set into the visitor.
    //
    // These values are not already set since a single visitor instance is used for
    // an entire scan sweep.

    public TargetsVisitorClassImpl(
        TargetsTableImpl parentData,
        String classSourceName,
        Set<String> i_newResolvedClassNames, Set<String> i_resolvedClassNames,
        Set<String> i_newUnresolvedClassNames, Set<String> i_unresolvedClassNames,
        Set<String> i_selectAnnotationClassNames,
        boolean recordDetail) {

        super(Opcodes.ASM8);

        String methodName = "<init>";
        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.targetsData = parentData;

        this.classData = new ClassData(classSourceName);

        if ( (i_resolvedClassNames == null) != (i_unresolvedClassNames == null) ) {
            throw new IllegalArgumentException("Both or neither of the class names stores must be null.");
        }

        this.i_newResolvedClassNames = i_newResolvedClassNames;
        this.i_resolvedClassNames = i_resolvedClassNames;

        this.i_newUnresolvedClassNames = i_newUnresolvedClassNames;
        this.i_unresolvedClassNames = i_unresolvedClassNames;

        this.i_selectAnnotationClassNames = i_selectAnnotationClassNames;

        this.fieldVisitor = new AnnoFieldVisitor();
        this.methodVisitor = new AnnoMethodVisitor();
        this.annotationVisitor = (recordDetail ? new TargetsVisitorAnnotationImpl(this) : null );

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] on [ {1} ]",
                    new Object[] { this.hashText, this.targetsData.getHashText() });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] detail [ {1} ]",
                    new Object[] { this.hashText, Boolean.valueOf(recordDetail) });
        }
    }

    protected void reset() {
        externalName = null;

        classData.reset();

        if (annotationVisitor != null) {
            annotationVisitor.reset();
        }
    }

    // Trace ...

    protected final String hashText;

    @Trivial
    public String getHashText() {
        return hashText;
    }

    // The target which is updated by this visitor and associated methods.

    protected final TargetsTableImpl targetsData;

    @Trivial
    public TargetsTableImpl geTargetsData() {
        return targetsData;
    }

    protected String internClassName(String className) {
        return geTargetsData().internClassName(className);
    }

    protected String internClassName(String className, boolean doForce) {
        return geTargetsData().internClassName(className, doForce);
    }

    public String internFieldName(String fieldName) {
        return geTargetsData().internFieldName(fieldName);
    }

    public String internMethodSignature(String methodSignature) {
        return geTargetsData().internMethodSignature(methodSignature);
    }

    @Trivial
    protected Map<String, String> createSmallMap() {
        return new IdentityHashMap<String, String>(3);
    }

    @Trivial
    protected Set<String> createIdentitySet() {
        return new UtilImpl_IdentityStringSet();
    }

    //

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

    @Trivial
    public String getExternalName() {
        return externalName;
    }

    //

    protected class ClassData {
        public ClassData(String classSourceName) {
            this.classSourceName = classSourceName;
        }

        protected String classSourceName;

        protected boolean isClass;

        protected String className;
        protected String i_className;

        protected String i_superclassName;
        protected String[] i_interfaceNames;

        protected int modifiers;
        
        protected String i_annotationClassName;

        protected Set<String> i_classAnnotations;
        protected Map<String, String> i_classAnnotationsDetail;

        protected String fieldName;
        protected String i_fieldName;

        protected Map<String, Set<String>> i_classFieldAnnotations;
        protected Map<String, Map<String, String>> i_classFieldAnnotationsDetail;

        protected String methodSignature;
        protected String i_methodSignature;

        protected Map<String, Set<String>> i_classMethodAnnotations;
        protected Map<String, Map<String, String>> i_classMethodAnnotationsDetail;

        public static final boolean IS_CLASS = true;
        public static final boolean IS_NOT_CLASS = false;

        public String setClassName(boolean isClass, String className) {
            this.isClass = isClass;

            this.className = className;
            this.i_className = internClassName(className, Util_InternMap.DO_FORCE);

            return this.i_className;
        }

        public void setModifiers(int modifiers) {
            this.modifiers = modifiers;
        }

        protected void setFieldName(String fieldName) {
            this.fieldName = fieldName;

            i_fieldName = null;

            // The field type is not recorded as a referenced class.
            // Field types are not available through the annotation tables.
        }

        // Store the field name, but don't intern it unless an annotation must
        // be recorded to the field.

        protected String i_getFieldName() {
            if (fieldName == null) {
                return null;
            }

            if (i_fieldName == null) {
                i_fieldName = internFieldName(fieldName);
            }

            return i_fieldName;
        }


        // Store the method signature, but don't intern it unless an annotation must
        // be recorded to the method.
        //
        // The method signature includes all of the method details,
        // that is, the method return type, name, and parameter types.
        //
        // The return type is more than is necessary, but will always be unique
        // for a single class for a specific name and parameter types.

        protected void setMethodSignature(String methodSignature) {
            this.methodSignature = methodSignature;

            i_methodSignature = null;

            // None of the method parameter types, exception types, or return type,
            // is recorded as a referenced class.
            //
            // These method details are not available in the annotation results.
        }

        protected String i_getMethodSignature() {
            if (methodSignature == null) {
                return null;
            }

            if (i_methodSignature == null) {
                i_methodSignature = internMethodSignature(methodSignature);
            }

            return i_methodSignature;
        }

        //

        protected void recordAnnotation() {
            if ( i_getFieldName() != null ) {
                recordFieldAnnotation();
            } else if ( i_getMethodSignature() != null ) {
                recordMethodAnnotation();
            } else {
                recordClassAnnotation();
            }
        }

        protected void recordClassAnnotation() {
            if ( i_classAnnotations == null ) {
                i_classAnnotations = createIdentitySet();
            }

            i_classAnnotations.add(i_annotationClassName);

            i_annotationClassName = null;
        }

        protected void recordFieldAnnotation() {
            Set<String> i_fieldAnnotations;
            if ( i_classFieldAnnotations == null ) {
                i_classFieldAnnotations = new IdentityHashMap<String, Set<String>>();
                i_fieldAnnotations = null;
            } else {
                i_fieldAnnotations = i_classFieldAnnotations.get(i_fieldName);
            }

            if (i_fieldAnnotations == null) {
                i_fieldAnnotations = createIdentitySet();
                i_classFieldAnnotations.put(i_fieldName, i_fieldAnnotations);
            }

            i_fieldAnnotations.add(i_annotationClassName);

            i_annotationClassName = null;
        }

        protected void recordMethodAnnotation() {
            Set<String> i_methodAnnotations;
            if ( i_classMethodAnnotations == null ) {
                i_classMethodAnnotations = new IdentityHashMap<String, Set<String>>();
                i_methodAnnotations = null;
            } else {
                i_methodAnnotations = i_classMethodAnnotations.get(i_methodSignature);
            }

            if (i_methodAnnotations == null) {
                i_methodAnnotations = createIdentitySet();
                i_classMethodAnnotations.put(i_methodSignature, i_methodAnnotations);
            }

            i_methodAnnotations.add(i_annotationClassName);

            i_annotationClassName = null;
        }

        //

        protected void recordAnnotation(String annotationDetail) {
            if ( i_getFieldName() != null ) {
                recordFieldAnnotation(annotationDetail);
            } else if ( i_getMethodSignature() != null ) {
                recordMethodAnnotation(annotationDetail);
            } else {
                recordClassAnnotation(annotationDetail);
            }
        }

        protected void recordClassAnnotation(String annotationDetail) {
            if ( i_classAnnotationsDetail == null ) {
                i_classAnnotationsDetail = createSmallMap();
            }

            i_classAnnotationsDetail.put(i_annotationClassName, annotationDetail);

            i_annotationClassName = null;
        }

        protected void recordFieldAnnotation(String annotationDetail) {
            Map<String, String> i_fieldAnnotations;
            if ( i_classFieldAnnotationsDetail == null ) {
                i_classFieldAnnotationsDetail = new IdentityHashMap<String, Map<String, String>>();
                i_fieldAnnotations = null;
            } else {
                i_fieldAnnotations = i_classFieldAnnotationsDetail.get(i_fieldName);
            }

            if (i_fieldAnnotations == null) {
                i_fieldAnnotations = createSmallMap();
                i_classFieldAnnotationsDetail.put(i_fieldName, i_fieldAnnotations);
            }

            i_fieldAnnotations.put(i_annotationClassName, annotationDetail);

            i_annotationClassName = null;
        }

        protected void recordMethodAnnotation(String annotationDetail) {
            Map<String, String> i_methodAnnotations;
            if ( i_classMethodAnnotationsDetail == null ) {
                i_classMethodAnnotationsDetail = new IdentityHashMap<String, Map<String, String>>();
                i_methodAnnotations = null;
            } else {
                i_methodAnnotations = i_classMethodAnnotationsDetail.get(i_methodSignature);
            }

            if (i_methodAnnotations == null) {
                i_methodAnnotations = createSmallMap();
                i_classMethodAnnotationsDetail.put(i_methodSignature, i_methodAnnotations);
            }

            i_methodAnnotations.put(i_annotationClassName, annotationDetail);

            i_annotationClassName = null;
        }

        protected void reset() {
            isClass = false;

            className = null;
            i_className = null;

            i_superclassName = null;
            i_interfaceNames = null;

            modifiers = 0;

            fieldName = null;
            i_fieldName = null;

            methodSignature = null;
            i_methodSignature = null;

            i_annotationClassName = null;

            i_classAnnotations = null;
            i_classAnnotationsDetail = null;

            i_classMethodAnnotations = null;
            i_classMethodAnnotationsDetail = null;

            i_classFieldAnnotations = null;
            i_classFieldAnnotationsDetail = null;
        }
    }

    protected final ClassData classData;

    protected void recordAnnotation() {
        classData.recordAnnotation();
    }

    protected void recordAnnotation(String annotationDetail) {
        classData.recordAnnotation(annotationDetail);
    }

    // Class name tracking ...
    //
    // * Resolved and unresolved have no common members.
    // * New unresolved is a subset of unresolved.
    // * New resolved is a subset of resolved.

    public static final Set<String> DONT_RECORD_NEW_UNRESOLVED = null;
    public static final Set<String> DONT_RECORD_UNRESOLVED = null;

    protected final Set<String> i_newUnresolvedClassNames;
    protected final Set<String> i_unresolvedClassNames;

    public Set<String> i_getNewUnresolvedClassNames() {
        return i_newUnresolvedClassNames;
    }

    public Set<String> i_getUnresolvedClassNames() {
        return i_unresolvedClassNames;
    }

    public static final Set<String> DONT_RECORD_NEW_RESOLVED = null;
    public static final Set<String> DONT_RECORD_RESOLVED = null;

    protected final Set<String> i_newResolvedClassNames;
    protected final Set<String> i_resolvedClassNames;

    @Trivial
    public Set<String> i_getNewResolvedClassNames() {
        return i_newResolvedClassNames;
    }

    @Trivial
    public Set<String> i_getResolvedClassNames() {
        return i_resolvedClassNames;
    }

    /**
     * <p>Record the definition of a class.  Update the resolved and
     * unresolved collections appropriately.</p>
     *
     * <p>No updates are performed if the class is already marked
     * as resolved.</p>
     *
     * @param i_className The class which was defined.
     */
    protected void recordDefinition(String i_className) {
        String methodName = "recordDefinition";

        String defCase;

        if ( i_resolvedClassNames == null ) {
            defCase = "No resolved or unresolved; not recording";
        } else if ( !i_resolvedClassNames.add(i_className) ) {
            defCase = "Already resolved";
        } else {
            if ( i_newResolvedClassNames != null ) {
                i_newResolvedClassNames.add(i_className);
            }
            if ( i_unresolvedClassNames.remove(i_className) ) {
                if ( i_newUnresolvedClassNames != null ) {
                    i_newUnresolvedClassNames.remove(i_className);
                }
                defCase = "Newly resolved (with references)";
            } else {
                defCase = "Newly resolved (no references)";
            }
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "Class [ {0} ] ({1})",
                        new Object[] { i_className, defCase });
        }
    }

    /**
     * <p>Record a reference to a class.</p>
     *
     * <p>There is nothing to do if the class is already defined.</p>
     *
     * <p>Only mark the class as new unresolved if it wasn't already
     * recorded as unresolved.</p>
     *
     * @param i_className The class name which is being recorded.
     */
    protected void recordReference(String i_className) {
        String methodName = "recordReference";

        String referenceCase;

        if ( i_resolvedClassNames == null ) {
            referenceCase = "Null resolved or unresolved; no recording";
        } else if ( i_resolvedClassNames.contains(i_className) ) {
            referenceCase = "Already resolved";
        } else if ( !i_unresolvedClassNames.add(i_className) ) {
            referenceCase = "Already unresolved";
        } else {
            if ( i_newUnresolvedClassNames != null ) {
                i_newUnresolvedClassNames.add(i_className);
            }
            referenceCase = "New unresolved";
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "Class [ {0} ] ({1})",
                        new Object[] { i_className, referenceCase });

        }
    }

    //

    protected final Set<String> i_selectAnnotationClassNames;

    @Trivial
    protected Set<String> i_getSelectAnnotationClassNames() {
        return i_selectAnnotationClassNames;
    }

    /**
     * <p>Select the annotation class name as one of interest.  That is,
     * if specific annotation class names are present and the annotation
     * is one of the specified names, or, if all annotations are being
     * processed.</p>
     *
     * @param annotationClassName The target annotation class name.
     *
     * @return Null if the annotation is not of interest.  The interned
     *         annotation class name if the annotation is of interest.
     */
    protected String i_selectAnnotationClassName(String annotationClassName) {
        // Case 1: All annotations are selected.  Do a forcing intern of
        //         the candidate class name.

        if ( i_selectAnnotationClassNames == null ) {
            return internClassName(annotationClassName);
        }

        // Case 2: Selections are specified.  Do a non-forcing intern of the candidate
        //         class name.  An interned copy of the candidate class name will be present
        //         if the class name is selected, since all of the specified class names
        //         were interned.
        //
        //         Since the candidate class name will successfully intern when it matches
        //         *any* interned class name, a check within the specified class names is
        //         still necessary.

        String i_annotationClassName = internClassName(annotationClassName, Util_InternMap.DO_NOT_FORCE);
        if ( i_annotationClassName == null ) {
            return null;
        } else if ( !i_selectAnnotationClassNames.contains(i_annotationClassName) ) {
            return null;
        } else {
            return i_annotationClassName;
        }
    }

    //

    // Common annotation visiting:

    protected final FieldVisitor fieldVisitor;
    protected final MethodVisitor methodVisitor;

    protected final TargetsVisitorAnnotationImpl annotationVisitor;

    @Trivial
    public boolean getRecordDetail() {
        return ( annotationVisitor != null );
    }

    //

    // Main visit entry point ... package and class processing begin with a call to visit.
    //
    // See the main class comments for the visit sequence.
    //
    // Expected followups are to visitAnnotation, visitField, visitMethod, and finally
    // visitEnd.  (Only visitEnd is guaranteed.)
    //
    // Note that the names are resource names.

    @Override
    public void visit(
        int version,
        int access,
        String classResourceName,
        String signature, String superClassResourceName, String interfaceResourceNames[]) {

        String methodName = "visit";
        
        Object[] logParms;
        if (logger.isLoggable(Level.FINER)) {
            logParms = new Object[] { getHashText(), classResourceName };
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Class [ {1} ]", logParms);
            logParms[1] = superClassResourceName;
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Superclass [ {1} ]", logParms);
            logParms[1] = interfaceResourceNames;
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Interfaces [ {1} ]", logParms);
        } else {
            logParms = null;
        }

        String className = getClassNameFromPartialResourceName(classResourceName);

        if (logParms != null) {
            logParms[1] = className + ".class";
        }

        if (isPackageName(className)) {
            if ((logParms != null) && logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] [ {1} ] Package load", logParms);
            }

            className = stripPackageNameFromClassName(className);

            String externalClassName = stripPackageNameFromClassName(getExternalName());

            // Note: The package is NOT recorded as a scanned package.
            //       if the package name does not match.

            if (!className.equals(externalClassName)) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] Class name mismatch [ {1} ]",
                            new Object[] { getHashText(), externalClassName });
                }
                throw VISIT_ENDED_PACKAGE_MISMATCH;
            }

            recordDefinition( classData.setClassName(ClassData.IS_NOT_CLASS, className) );

        } else {
            if (logParms != null && logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] [ {1} ] Class load", logParms);
            }

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
                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] Class name mismatch [ {1} ]",
                            new Object[] { getHashText(), className });
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

            recordDefinition( classData.setClassName(ClassData.IS_CLASS, className) );

            classData.setModifiers(access);
            
            if (superClassResourceName != null) {
                String superClassName = getClassNameFromPartialResourceName(superClassResourceName);
                recordReference( classData.i_superclassName = internClassName(superClassName) );
            }

            if ((interfaceResourceNames != null) && (interfaceResourceNames.length > 0)) {
                String[] i_interfaceNames = new String[interfaceResourceNames.length];

                for (int nameNo = 0; nameNo < interfaceResourceNames.length; nameNo++) {
                    String nextInterfaceResourceName = interfaceResourceNames[nameNo];
                    String nextInterfaceName = getClassNameFromPartialResourceName(nextInterfaceResourceName);
                    String i_nextInterfaceName = internClassName(nextInterfaceName);

                    recordReference( i_interfaceNames[nameNo] = i_nextInterfaceName );
                }

                classData.i_interfaceNames = i_interfaceNames;
            }
        }
    }

    @Override
    public void visitEnd() {
        targetsData.record(classData);
    }

    //

    // Exception used to stop visiting.  The exception is needed because
    // the visitor API as no setting to cause immediate return.

    public static enum VisitEndCase {
        VISIT_END_CLASS_MISMATCH,
        VISIT_END_PACKAGE_MISMATCH;
    }

    public static final VisitEnded VISIT_ENDED_CLASS_MISMATCH = new VisitEnded(VisitEndCase.VISIT_END_CLASS_MISMATCH);
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

    // Visit a class or package annotation.  Record the annotation then return null to
    // complete the annotation processing.  Only the name of the class of the annotation
    // is recorded.

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        // String methodName = "visitAnnotation";

        // Set the annotation class name for both the targets table
        // and for the details table.  The targets table is updated
        // immediately.  The details table is recorded after the
        // annotation is fully read, by 'visitEnd' of the annotation
        // visitor.  The annotation class name is cleared when the
        // annotation detail is recorded.

        String annotationResourceName = getClassResourceNameFromAnnotationDescription(desc);
        String annotationClassName = getClassNameFromPartialResourceName(annotationResourceName);

        String i_annotationClassName = i_selectAnnotationClassName(annotationClassName);
        if ( i_annotationClassName == null ) {
            return null; // Annotation filtering is in effect, and this annotation is filtered.
        }

        recordReference(classData.i_annotationClassName = i_annotationClassName);

        if ( annotationVisitor == null ) {
            classData.recordClassAnnotation();
        }

        // logger.logp(Level.INFO, CLASS_NAME, methodName, "Annotation [ {0} ]", i_annotationClassName);
        
        return annotationVisitor;
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
            // Set the annotation class name for both the targets table
            // and for the details table.  The targets table is updated
            // immediately.  The details table is recorded after the
            // annotation is fully read, by 'visitEnd' of the annotation
            // visitor.  The annotation class name is cleared when the
            // annotation detail is recorded.

            String annotationResourceName = getClassResourceNameFromAnnotationDescription(desc);
            String annotationClassName = getClassNameFromPartialResourceName(annotationResourceName);

            String i_annotationClassName = i_selectAnnotationClassName(annotationClassName);
            if ( i_annotationClassName == null ) {
                return null; // Annotation filtering is in effect, and this annotation is filtered.
            }

            recordReference( classData.i_annotationClassName = i_annotationClassName );

            if ( annotationVisitor == null ) {
                classData.recordFieldAnnotation();
            }

            return annotationVisitor;
        }

        // End visiting a field.  Just end this field visit stage,
        // don't end the class as a whole.

        @Override
        public void visitEnd() {
            classData.setFieldName(null);
        }
    }

    // Visit a field.  If detail is disabled, or if the class is an external class,
    // end the processing of the current class.  Otherwise, start a new stage of
    // visiting the field.

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object defaultValue) {
        // Set the field name : This will be needed for any annotations
        // detected on the field.

        classData.setFieldName(name);

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
            // Set the annotation class name for both the targets table
            // and for the details table.  The targets table is updated
            // immediately.  The details table is recorded after the
            // annotation is fully read, by 'visitEnd' of the annotation
            // visitor.  The annotation class name is cleared when the
            // annotation detail is recorded.

            String annotationResourceName = getClassResourceNameFromAnnotationDescription(desc);
            String annotationClassName = getClassNameFromPartialResourceName(annotationResourceName);

            String i_annotationClassName = i_selectAnnotationClassName(annotationClassName);
            if ( i_annotationClassName == null ) {
                return null; // Annotation filtering is in effect, and this annotation is filtered.
            }

            recordReference( classData.i_annotationClassName = i_annotationClassName );

            if ( annotationVisitor == null ) {
                classData.recordMethodAnnotation();
            }

            return annotationVisitor;
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
            classData.setMethodSignature(null);
        }
    }

    // Visit a method.  If detail is disabled, or if the class is an external class,
    // end the processing of the current class.  Otherwise, start a new stage of
    // visiting the method.

    // Per: ASM 4 (ASM8-guide.pdf):
    //
    // 2.1.4. Method descriptors
    // A method descriptor is a list of type descriptors that describe the parameter
    // types and the return type of a method, in a single string. A method descriptor
    // starts with a left parenthesis, followed by the type descriptors of each formal
    // parameter, followed by a right parenthesis, followed by the type descriptor of
    // the return type, or V if the method returns void (a method descriptor does
    // not contain the method’s name or the argument names).
    //
    // Method declaration in source file Method descriptor
    //   void m(int i, float f) (IF)V
    //   int m(Object o) (Ljava/lang/Object;)I
    //   int[] m(int i, String s) (ILjava/lang/String;)[I
    //   Object m(int[] i) ([I)Ljava/lang/Object;

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String exceptions[]) {
        // Set the method signature: This will be needed for any annotations
        // detected on the method.

        if ( signature == null ) {
            signature = name + desc;
            // System.out.println("Using [ " + name + " ] [ " + desc + " ] as the method signature [ " + signature + " ]");
        }

        classData.setMethodSignature(signature);

        return methodVisitor;
    }

    // End visiting a class or package.
}

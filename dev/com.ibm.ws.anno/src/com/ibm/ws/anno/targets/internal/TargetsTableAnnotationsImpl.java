/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2014, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.targets.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.ws.anno.targets.TargetsTableAnnotations;
import com.ibm.ws.anno.targets.cache.TargetCache_ParseError;
import com.ibm.ws.anno.targets.cache.TargetCache_Readable;
import com.ibm.ws.anno.targets.cache.TargetCache_Reader;
import com.ibm.ws.anno.util.internal.UtilImpl_BidirectionalMap;
import com.ibm.ws.anno.util.internal.UtilImpl_Factory;
import com.ibm.ws.anno.util.internal.UtilImpl_InternMap;
import com.ibm.ws.anno.util.internal.UtilImpl_NonInternSet;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets.AnnotationCategory;
import com.ibm.wsspi.anno.util.Util_InternMap;

/**
 * <p>Class to annotation targets tables.</p>
 */
public class TargetsTableAnnotationsImpl
    implements TargetsTableAnnotations, TargetCache_Readable {

    // Logging ...

    protected static final Logger logger = AnnotationServiceImpl_Logging.ANNO_LOGGER;

    public static final String CLASS_NAME = TargetsTableAnnotationsImpl.class.getSimpleName();

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    protected TargetsTableAnnotationsImpl(TargetsTableAnnotationsImpl otherTable, UtilImpl_InternMap classNameInternMap) {
        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.utilFactory = otherTable.getUtilFactory();
        this.classNameInternMap = classNameInternMap;

        this.i_packageAnnotationData = internAnnotationData( "Packages with annotations", otherTable.i_packageAnnotationData );

        this.i_classAnnotationData = internAnnotationData( "Classes with annotations", otherTable.i_classAnnotationData );

        this.i_fieldAnnotationData = internAnnotationData( "Classes with field annotations", otherTable.i_fieldAnnotationData );
        this.i_methodAnnotationData = internAnnotationData( "Classes with method annotations", otherTable.i_methodAnnotationData );

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    protected UtilImpl_BidirectionalMap internAnnotationData(String description, UtilImpl_BidirectionalMap otherData) {
        UtilImpl_InternMap useClassNameInternMap = getClassNameInternMap();

        UtilImpl_BidirectionalMap thisData = getUtilFactory().createBidirectionalMap(description, useClassNameInternMap,
                                                                                     "annotations", useClassNameInternMap);

        for ( String i_otherClassName : otherData.getHolderSet() ) {
            Set<String> i_otherAnnotationClassNames = otherData.i_selectHeldOf(i_otherClassName);

            for ( String i_otherAnnotationClassName : i_otherAnnotationClassNames ) {
                thisData.record(i_otherClassName, i_otherAnnotationClassName);
            }
        }

        return thisData;
    }

    //

    protected TargetsTableAnnotationsImpl(UtilImpl_Factory utilFactory, UtilImpl_InternMap classNameInternMap) {
        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.utilFactory = utilFactory;
        this.classNameInternMap = classNameInternMap;

        this.i_packageAnnotationData = utilFactory.createBidirectionalMap("Packages with annotations", classNameInternMap,
                                                                          "annotations", classNameInternMap);
        this.i_classAnnotationData = utilFactory.createBidirectionalMap("Classes with annotations", classNameInternMap,
                                                                        "annotations", classNameInternMap);
        this.i_fieldAnnotationData = utilFactory.createBidirectionalMap("Classes with field annotations", classNameInternMap,
                                                                        "annotations", classNameInternMap);
        this.i_methodAnnotationData = utilFactory.createBidirectionalMap("Classes with method annotations", classNameInternMap,
                                                                         "annotations", classNameInternMap);

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    //

    protected final UtilImpl_Factory utilFactory;

    public UtilImpl_Factory getUtilFactory() {
        return utilFactory;
    }

    protected final UtilImpl_InternMap classNameInternMap;

    public UtilImpl_InternMap getClassNameInternMap() {
        return classNameInternMap;
    }

    @Override
    public String internClassName(String className) {
        return getClassNameInternMap().intern(className);
    }

    @Override
    public String internClassName(String className, boolean doForce) {
        return getClassNameInternMap().intern(className, doForce);
    }

    //

    protected final UtilImpl_BidirectionalMap i_packageAnnotationData;

    @Override
    public UtilImpl_BidirectionalMap i_getPackageAnnotationData() {
        return i_packageAnnotationData;
    }

    protected final UtilImpl_BidirectionalMap i_classAnnotationData;

    @Override
    public UtilImpl_BidirectionalMap i_getClassAnnotationData() {
        return i_classAnnotationData;
    }

    protected final UtilImpl_BidirectionalMap i_fieldAnnotationData;

    @Override
    public UtilImpl_BidirectionalMap i_getFieldAnnotationData() {
        return i_fieldAnnotationData;
    }

    protected final UtilImpl_BidirectionalMap i_methodAnnotationData;

    @Override
    public UtilImpl_BidirectionalMap i_getMethodAnnotationData() {
        return i_methodAnnotationData;
    }

    //

    @Override
    public Set<String> i_getPackagesWithAnnotations() {
        return i_packageAnnotationData.getHolderSet();
    }

    @Override
    public Set<String> i_getPackagesWithAnnotation(String i_annotationName) {
        return i_packageAnnotationData.selectHoldersOf(i_annotationName);
    }

    @Override
    public Set<String> i_getPackageAnnotations() {
        return i_packageAnnotationData.getHeldSet();
    }

    @Override
    public Set<String> i_getPackageAnnotations(String i_packageName) {
        return i_packageAnnotationData.selectHeldOf(i_packageName);
    }

    //

    @Override
    public Set<String> i_getClassesWithClassAnnotations() {
        return i_classAnnotationData.getHolderSet();
    }

    @Override
    public Set<String> i_getClassesWithClassAnnotation(String i_annotationName) {
        return i_classAnnotationData.selectHoldersOf(i_annotationName);
    }

    @Override
    public Set<String> i_getClassAnnotations() {
        return i_classAnnotationData.getHeldSet();
    }

    @Override
    public Set<String> i_getClassAnnotations(String i_className) {
        return i_classAnnotationData.selectHeldOf(i_className);
    }

    //

    @Override
    public Set<String> i_getClassesWithFieldAnnotations() {
        return i_fieldAnnotationData.getHolderSet();
    }

    @Override
    public Set<String> i_getClassesWithFieldAnnotation(String i_annotationName) {
        return i_fieldAnnotationData.selectHoldersOf(i_annotationName);
    }

    @Override
    public Set<String> i_getFieldAnnotations() {
        return i_fieldAnnotationData.getHeldSet();
    }

    @Override
    public Set<String> i_getFieldAnnotations(String i_className) {
        return i_fieldAnnotationData.selectHeldOf(i_className);
    }

    //

    @Override
    public Set<String> i_getClassesWithMethodAnnotations() {
        return i_methodAnnotationData.getHolderSet();
    }

    @Override
    public Set<String> i_getClassesWithMethodAnnotation(String i_annotationName) {
        return i_methodAnnotationData.selectHoldersOf(i_annotationName);
    }

    @Override
    public Set<String> i_getMethodAnnotations() {
        return i_methodAnnotationData.getHeldSet();
    }

    @Override
    public Set<String> i_getMethodAnnotations(String i_className) {
        return i_methodAnnotationData.selectHeldOf(i_className);
    }

    //

    @Override
    public Set<String> i_getAnnotatedTargets(AnnotationCategory category) {
        return i_getAnnotationData(category).getHolderSet();
    }

    @Override
    public Set<String> i_getAnnotatedTargets(AnnotationCategory category, String i_annotationName) {
        return i_getAnnotationData(category).selectHoldersOf(i_annotationName);
    }

    @Override
    public Set<String> i_getAnnotations(AnnotationCategory category) {
        return i_getAnnotationData(category).getHeldSet();
    }

    @Override
    public Set<String> i_getAnnotations(AnnotationCategory category, String i_classOrPackageName) {
        return i_getAnnotationData(category).selectHeldOf(i_classOrPackageName);
    }

    //

    @Override
    public UtilImpl_BidirectionalMap i_getAnnotationData(AnnotationCategory category) {
        if (category == AnnotationCategory.PACKAGE) {
            return (i_packageAnnotationData);
        } else if (category == AnnotationCategory.CLASS) {
            return (i_classAnnotationData);
        } else if (category == AnnotationCategory.METHOD) {
            return (i_methodAnnotationData);
        } else if (category == AnnotationCategory.FIELD) {
            return (i_fieldAnnotationData);
        } else {
            throw new IllegalArgumentException("Category [ " + category + " ]");
        }
    }

    //

    @Override
    public void log(Logger useLogger) {
        String methodName = "<init>";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Annotations Data: START: [ {0} ]", getHashText());

        i_packageAnnotationData.log(useLogger);
        i_classAnnotationData.log(useLogger);
        i_fieldAnnotationData.log(useLogger);
        i_methodAnnotationData.log(useLogger);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Annotations Data: END: [ {0} ]", getHashText());
    }

    //

    @Override
    public void recordPackageAnnotation(String i_packageName, String i_annotationClassName) {
        i_packageAnnotationData.i_record(i_packageName, i_annotationClassName);
    }

    @Override
    public void recordClassAnnotation(String i_className, String i_annotationClassName) {
        i_classAnnotationData.i_record(i_className, i_annotationClassName);
    }

    @Override
    public void recordFieldAnnotation(String i_className, String i_annotationClassName) {
        i_fieldAnnotationData.i_record(i_className, i_annotationClassName);
    }

    @Override
    public void recordMethodAnnotation(String i_className, String i_annotationClassName) {
        i_methodAnnotationData.i_record(i_className, i_annotationClassName);
    }

    public void record(TargetsVisitorClassImpl.ClassData classData) {
        if ( !classData.isClass ) {
            Set<String> i_packageAnnotations = classData.i_classAnnotations;
            if ( i_packageAnnotations != null ) {
                String i_packageName = classData.i_className;

                for (String i_annotationClassName : i_packageAnnotations ) {
                    recordPackageAnnotation(i_packageName, i_annotationClassName);
                }
            }

            Map<String, String> i_packageAnnotationsDetail = classData.i_classAnnotationsDetail;
            if ( i_packageAnnotationsDetail != null ) {
                String i_packageName = classData.i_className;

                for (String i_annotationClassName : i_packageAnnotationsDetail.keySet() ) {
                    recordPackageAnnotation(i_packageName, i_annotationClassName);
                }
            }

        } else {
            String i_className = classData.i_className;

            Set<String> i_classAnnotations = classData.i_classAnnotations;
            if ( i_classAnnotations != null ) {
                for (String i_annotationClassName : i_classAnnotations ) {
                    recordClassAnnotation(i_className, i_annotationClassName);
                }
            }

            Map<String, String> i_classAnnotationsDetail = classData.i_classAnnotationsDetail;
            if ( i_classAnnotationsDetail != null ) {
                for (String i_annotationClassName : i_classAnnotationsDetail.keySet() ) {
                    recordClassAnnotation(i_className, i_annotationClassName);
                }
            }

            Map<String, Set<String>> i_classFieldAnnotations = classData.i_classFieldAnnotations;
            if ( i_classFieldAnnotations != null ) {
                for ( Set<String> fieldAnnotations : i_classFieldAnnotations.values() ) {
                    for ( String i_annotationClassName : fieldAnnotations ) {
                        recordFieldAnnotation(i_className, i_annotationClassName);
                    }
                }
            }

            Map<String, Map<String, String>> i_classFieldAnnotationsDetail = classData.i_classFieldAnnotationsDetail;
            if ( i_classFieldAnnotationsDetail != null ) {
                // Don't care about the field names
                for ( Map<String, String> fieldAnnotations : i_classFieldAnnotationsDetail.values() ) {
                    for ( String i_annotationClassName : fieldAnnotations.keySet() ) {
                        recordFieldAnnotation(i_className, i_annotationClassName);
                    }
                }
            }

            Map<String, Set<String>> i_classMethodAnnotations = classData.i_classMethodAnnotations;
            if ( i_classMethodAnnotations != null ) {
                // Don't care about the method names
                for ( Set<String> methodAnnotations : i_classMethodAnnotations.values() ) {
                    for ( String i_annotationClassName : methodAnnotations ) {
                        recordMethodAnnotation(i_className, i_annotationClassName);
                    }
                }
            }

            Map<String, Map<String, String>> i_classMethodAnnotationsDetail = classData.i_classMethodAnnotationsDetail;
            if ( i_classMethodAnnotationsDetail != null ) {
                // Don't care about the method names
                for ( Map<String, String> methodAnnotations : i_classMethodAnnotationsDetail.values() ) {
                    for ( String i_annotationClassName : methodAnnotations.keySet() ) {
                        recordMethodAnnotation(i_className, i_annotationClassName);
                    }
                }
            }
        }
    }

    /**
     * <p>Add data from a table into this table.  Restrict additions to
     * named packages and classes.</p>
     *
     * @param table The table which is to be added to this table.
     * @param i_addedPackageNames The names of packages for which to added data.
     * @param i_addedClassNames The names of classes for which to added data.
     */
    protected void restrictedAdd(TargetsTableAnnotationsImpl targetTable,
                                 Set<String> i_addedPackageNames,
                                 Set<String> i_addedClassNames) {

        i_packageAnnotationData.i_record( targetTable.i_getPackageAnnotationData(),
                                          i_addedPackageNames );

        i_classAnnotationData.i_record( targetTable.i_getClassAnnotationData(),
                                        i_addedClassNames );
        i_fieldAnnotationData.i_record( targetTable.i_getFieldAnnotationData(),
                                        i_addedClassNames );
        i_methodAnnotationData.i_record( targetTable.i_getMethodAnnotationData(),
                                         i_addedClassNames );
    }

    //

    public boolean sameAs(TargetsTableAnnotationsImpl otherTable) {
        if ( otherTable == null ) {
            return false;
        } else if ( otherTable == this ) {
            return true;
        }

        if ( !i_getPackageAnnotationData().i_equals( otherTable.i_getPackageAnnotationData() ) ) {
            return false;
        }

        if ( !i_getClassAnnotationData().i_equals( otherTable.i_getClassAnnotationData() ) ) {
            return false;
        }

        if ( !i_getFieldAnnotationData().i_equals( otherTable.i_getFieldAnnotationData() ) ) {
            return false;
        }

        if ( !i_getMethodAnnotationData().i_equals( otherTable.i_getMethodAnnotationData() ) ) {
            return false;
        }

        return true;
    }

    //

    @Override
    public List<TargetCache_ParseError> readUsing(TargetCache_Reader reader) throws IOException {
        return reader.read(this);
    }

    public void updateClassNames(Set<String> i_resolvedClassNames, Set<String> i_newlyResolvedClassNames,
                                 Set<String> i_unresolvedClassNames, Set<String> i_newlyUnresolvedClassNames) {

        i_getPackageAnnotationData().update(i_resolvedClassNames, i_newlyResolvedClassNames,
                                            i_unresolvedClassNames, i_newlyUnresolvedClassNames);

        i_getClassAnnotationData().update(i_resolvedClassNames, i_newlyResolvedClassNames,
                                          i_unresolvedClassNames, i_newlyUnresolvedClassNames);

        i_getFieldAnnotationData().update(i_resolvedClassNames, i_newlyResolvedClassNames,
                                          i_unresolvedClassNames, i_newlyUnresolvedClassNames);

        i_getMethodAnnotationData().update(i_resolvedClassNames, i_newlyResolvedClassNames,
                                           i_unresolvedClassNames, i_newlyUnresolvedClassNames);
    }

    //

    public Set<String> uninternClassNames(Set<String> classNames) {
        if ( (classNames == null) || classNames.isEmpty() ) {
            return Collections.emptySet();
        } else {
            return new UtilImpl_NonInternSet( getClassNameInternMap(), classNames );
        }
    }

    @Override
    public Set<String> getAnnotatedTargets(AnnotationCategory category) {
        return uninternClassNames( i_getAnnotatedTargets(category) ) ;
    }

    @Override
    public Set<String> getAnnotatedTargets(AnnotationCategory category, String annotationName) {
        String i_annotationName = internClassName(annotationName, Util_InternMap.DO_NOT_FORCE);
        if ( i_annotationName == null ) {
            return Collections.emptySet();
        } else {
            return uninternClassNames( i_getAnnotatedTargets(category, i_annotationName) ) ;
        }
    }

    @Override
    public Set<String> getAnnotations(AnnotationCategory category) {
        return uninternClassNames( i_getAnnotations(category) ) ;
    }

    @Override
    public Set<String> getAnnotations(AnnotationCategory category, String className) {
        String i_className = internClassName(className, Util_InternMap.DO_NOT_FORCE);
        if ( i_className == null ) {
            return Collections.emptySet();
        } else {
            return uninternClassNames( i_getAnnotations(category, i_className) ) ;
        }
    }

    @Override
    public Set<String> getPackagesWithAnnotations() {
        return uninternClassNames( i_getPackagesWithAnnotations() );
    }

    @Override
    public Set<String> getPackagesWithAnnotation(String annotationName) {
        String i_annotationName = internClassName(annotationName, Util_InternMap.DO_NOT_FORCE);
        if ( i_annotationName == null ) {
            return Collections.emptySet();
        } else {
            return uninternClassNames( i_getPackagesWithAnnotation(i_annotationName) );
        }
    }

    @Override
    public Set<String> getPackageAnnotations() {
        return uninternClassNames( i_getPackageAnnotations() );
    }

    @Override
    public Set<String> getPackageAnnotations(String packageName) {
        String i_packageName = internClassName(packageName, Util_InternMap.DO_NOT_FORCE);
        if ( i_packageName == null ) {
            return Collections.emptySet();
        } else {
            return uninternClassNames( i_getPackageAnnotations(i_packageName) );
        }
    }

    @Override
    public Set<String> getClassesWithClassAnnotations() {
        return uninternClassNames( i_getClassesWithClassAnnotations() );
    }

    @Override
    public Set<String> getClassesWithClassAnnotation(String annotationName) {
        String i_annotationName = internClassName(annotationName, Util_InternMap.DO_NOT_FORCE);
        if ( i_annotationName == null ) {
            return Collections.emptySet();
        } else {
            return uninternClassNames( i_getClassesWithClassAnnotation(i_annotationName) );
        }
    }

    @Override
    public Set<String> getClassAnnotations() {
        return uninternClassNames( i_getClassAnnotations() );
    }

    @Override
    public Set<String> getClassAnnotations(String className) {
        String i_className = internClassName(className, Util_InternMap.DO_NOT_FORCE);
        if ( i_className == null ) {
            return Collections.emptySet();
        } else {
            return uninternClassNames( i_getClassAnnotations(i_className) );
        }
    }

    @Override
    public Set<String> getClassesWithFieldAnnotations() {
        return uninternClassNames( i_getClassesWithFieldAnnotations() );
    }

    @Override
    public Set<String> getClassesWithFieldAnnotation(String annotationName) {
        String i_annotationName = internClassName(annotationName, Util_InternMap.DO_NOT_FORCE);
        if ( i_annotationName == null ) {
            return Collections.emptySet();
        } else {
            return uninternClassNames( i_getClassesWithFieldAnnotation(i_annotationName) );
        }
    }

    @Override
    public Set<String> getFieldAnnotations() {
        return uninternClassNames( i_getFieldAnnotations() );
    }

    @Override
    public Set<String> getFieldAnnotations(String className) {
        String i_className = internClassName(className, Util_InternMap.DO_NOT_FORCE);
        if ( i_className == null ) {
            return Collections.emptySet();
        } else {
            return uninternClassNames( i_getFieldAnnotations(i_className) );
        }
    }

    @Override
    public Set<String> getClassesWithMethodAnnotations() {
        return uninternClassNames( i_getClassesWithMethodAnnotations() );
    }

    @Override
    public Set<String> getClassesWithMethodAnnotation(String annotationName) {
        String i_annotationName = internClassName(annotationName, Util_InternMap.DO_NOT_FORCE);
        if ( i_annotationName == null ) {
            return Collections.emptySet();
        } else {
            return uninternClassNames( i_getClassesWithMethodAnnotation(i_annotationName) );
        }
    }

    @Override
    public Set<String> getMethodAnnotations() {
        return uninternClassNames( i_getMethodAnnotations() );
    }

    @Override
    public Set<String> getMethodAnnotations(String className) {
        String i_className = internClassName(className, Util_InternMap.DO_NOT_FORCE);
        if ( i_className == null ) {
            return Collections.emptySet();
        } else {
            return uninternClassNames( i_getMethodAnnotations(i_className) );
        }
    }

    //

    public void jandex_i_recordAnnotation(
        String i_className,
        AnnotationCategory annotationCategory, String i_annotationClassName) {

        if     ( annotationCategory == AnnotationCategory.PACKAGE) {
            recordPackageAnnotation(i_className, i_annotationClassName);
        } else if ( annotationCategory == AnnotationCategory.CLASS ) {
            recordClassAnnotation(i_className, i_annotationClassName);
        } else if ( annotationCategory == AnnotationCategory.METHOD ) {
            recordMethodAnnotation(i_className, i_annotationClassName);
        } else if ( annotationCategory == AnnotationCategory.FIELD ) {
            recordFieldAnnotation(i_className, i_annotationClassName);
        } else {
            throw new IllegalArgumentException("Unknown annotation category [ " + annotationCategory + " ]");
        }
    }
}

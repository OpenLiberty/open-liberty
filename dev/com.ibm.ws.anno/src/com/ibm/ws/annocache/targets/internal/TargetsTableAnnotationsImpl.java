/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.TargetsTableAnnotations;
import com.ibm.ws.annocache.targets.cache.TargetCache_ParseError;
import com.ibm.ws.annocache.targets.cache.TargetCache_Readable;
import com.ibm.ws.annocache.targets.cache.TargetCache_Reader;
import com.ibm.ws.annocache.util.internal.UtilImpl_BidirectionalMap;
import com.ibm.ws.annocache.util.internal.UtilImpl_Factory;
import com.ibm.ws.annocache.util.internal.UtilImpl_InternMap;
import com.ibm.ws.annocache.util.internal.UtilImpl_NonInternSet;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets.AnnotationCategory;
import com.ibm.wsspi.annocache.util.Util_InternMap;

/**
 * <p>Class to annotation targets tables.</p>
 */
public class TargetsTableAnnotationsImpl
    implements TargetsTableAnnotations, TargetCache_Readable {

    // Logging ...

    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    public static final String CLASS_NAME = TargetsTableAnnotationsImpl.class.getSimpleName();

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    //

    protected TargetsTableAnnotationsImpl(TargetsTableAnnotationsImpl otherTable, UtilImpl_InternMap classNameInternMap) {
        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.utilFactory = otherTable.getUtilFactory();
        this.classNameInternMap = classNameInternMap;

        this.i_packageAnnotations = internAnnotations( "Packages with annotations", otherTable.i_packageAnnotations );

        this.i_classAnnotations = internAnnotations( "Classes with annotations", otherTable.i_classAnnotations );

        this.i_fieldAnnotations = internAnnotations( "Classes with field annotations", otherTable.i_fieldAnnotations );
        this.i_methodAnnotations = internAnnotations( "Classes with method annotations", otherTable.i_methodAnnotations );

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    protected UtilImpl_BidirectionalMap internAnnotations(String description, UtilImpl_BidirectionalMap otherAnnos) {
        UtilImpl_InternMap useClassNameInternMap = getClassNameInternMap();

        UtilImpl_BidirectionalMap theseAnnos = getUtilFactory().createBidirectionalMap(description, useClassNameInternMap,
                                                                                       "annotations", useClassNameInternMap);

        for ( String i_otherClassName : otherAnnos.getHolderSet() ) {
            Set<String> i_otherAnnotationClassNames = otherAnnos.i_selectHeldOf(i_otherClassName);

            for ( String i_otherAnnotationClassName : i_otherAnnotationClassNames ) {
                theseAnnos.record(i_otherClassName, i_otherAnnotationClassName);
            }
        }

        return theseAnnos;
    }

    //

    protected TargetsTableAnnotationsImpl(UtilImpl_Factory utilFactory, UtilImpl_InternMap classNameInternMap) {
        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.utilFactory = utilFactory;
        this.classNameInternMap = classNameInternMap;

        this.i_packageAnnotations = utilFactory.createBidirectionalMap("Packages with annotations", classNameInternMap,
                                                                          "annotations", classNameInternMap);
        this.i_classAnnotations = utilFactory.createBidirectionalMap("Classes with annotations", classNameInternMap,
                                                                        "annotations", classNameInternMap);
        this.i_fieldAnnotations = utilFactory.createBidirectionalMap("Classes with field annotations", classNameInternMap,
                                                                        "annotations", classNameInternMap);
        this.i_methodAnnotations = utilFactory.createBidirectionalMap("Classes with method annotations", classNameInternMap,
                                                                         "annotations", classNameInternMap);

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    //

    protected final UtilImpl_Factory utilFactory;

    @Trivial
    public UtilImpl_Factory getUtilFactory() {
        return utilFactory;
    }

    protected final UtilImpl_InternMap classNameInternMap;

    @Trivial
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

    protected final UtilImpl_BidirectionalMap i_packageAnnotations;

    @Override
    @Trivial
    public UtilImpl_BidirectionalMap i_getPackageAnnotations() {
        return i_packageAnnotations;
    }

    protected final UtilImpl_BidirectionalMap i_classAnnotations;

    @Override
    @Trivial
    public UtilImpl_BidirectionalMap i_getClassAnnotations() {
        return i_classAnnotations;
    }

    protected final UtilImpl_BidirectionalMap i_fieldAnnotations;

    @Override
    @Trivial
    public UtilImpl_BidirectionalMap i_getFieldAnnotations() {
        return i_fieldAnnotations;
    }

    protected final UtilImpl_BidirectionalMap i_methodAnnotations;

    @Override
    @Trivial
    public UtilImpl_BidirectionalMap i_getMethodAnnotations() {
        return i_methodAnnotations;
    }

    //

    @Override
    @Trivial
    public Set<String> i_getPackagesWithAnnotations() {
        return i_packageAnnotations.getHolderSet();
    }

    @Override
    public Set<String> i_getPackagesWithAnnotation(String i_annotationName) {
        return i_packageAnnotations.selectHoldersOf(i_annotationName);
    }

    @Override
    public Set<String> i_getPackageAnnotationNames() {
        return i_packageAnnotations.getHeldSet();
    }

    @Override
    public Set<String> i_getPackageAnnotations(String i_packageName) {
        return i_packageAnnotations.selectHeldOf(i_packageName);
    }

    //

    @Override
    @Trivial
    public Set<String> i_getClassesWithClassAnnotations() {
        return i_classAnnotations.getHolderSet();
    }

    @Override
    public Set<String> i_getClassesWithClassAnnotation(String i_annotationName) {
        return i_classAnnotations.selectHoldersOf(i_annotationName);
    }

    @Override
    public Set<String> i_getClassAnnotationNames() {
        return i_classAnnotations.getHeldSet();
    }

    @Override
    public Set<String> i_getClassAnnotations(String i_className) {
        return i_classAnnotations.selectHeldOf(i_className);
    }

    //

    @Override
    @Trivial
    public Set<String> i_getClassesWithFieldAnnotations() {
        return i_fieldAnnotations.getHolderSet();
    }

    @Override
    public Set<String> i_getClassesWithFieldAnnotation(String i_annotationName) {
        return i_fieldAnnotations.selectHoldersOf(i_annotationName);
    }

    @Override
    public Set<String> i_getFieldAnnotationNames() {
        return i_fieldAnnotations.getHeldSet();
    }

    @Override
    public Set<String> i_getFieldAnnotations(String i_className) {
        return i_fieldAnnotations.selectHeldOf(i_className);
    }

    //

    @Override
    @Trivial
    public Set<String> i_getClassesWithMethodAnnotations() {
        return i_methodAnnotations.getHolderSet();
    }

    @Override
    public Set<String> i_getClassesWithMethodAnnotation(String i_annotationName) {
        return i_methodAnnotations.selectHoldersOf(i_annotationName);
    }

    @Override
    public Set<String> i_getMethodAnnotationNames() {
        return i_methodAnnotations.getHeldSet();
    }

    @Override
    public Set<String> i_getMethodAnnotations(String i_className) {
        return i_methodAnnotations.selectHeldOf(i_className);
    }

    //

    @Override
    @Trivial
    public Set<String> i_getAnnotatedTargets(AnnotationCategory category) {
        return i_getAnnotations(category).getHolderSet();
    }

    @Override
    public Set<String> i_getAnnotatedTargets(AnnotationCategory category, String i_annotationName) {
        return i_getAnnotations(category).selectHoldersOf(i_annotationName);
    }

    @Override
    public Set<String> i_getAnnotationNames(AnnotationCategory category) {
        return i_getAnnotations(category).getHeldSet();
    }

    @Override
    public Set<String> i_getAnnotations(AnnotationCategory category, String i_classOrPackageName) {
        return i_getAnnotations(category).selectHeldOf(i_classOrPackageName);
    }

    //

    @Override
    public UtilImpl_BidirectionalMap i_getAnnotations(AnnotationCategory category) {
        if (category == AnnotationCategory.PACKAGE) {
            return (i_packageAnnotations);
        } else if (category == AnnotationCategory.CLASS) {
            return (i_classAnnotations);
        } else if (category == AnnotationCategory.METHOD) {
            return (i_methodAnnotations);
        } else if (category == AnnotationCategory.FIELD) {
            return (i_fieldAnnotations);
        } else {
            throw new IllegalArgumentException("Category [ " + category + " ]");
        }
    }

    //

    @Override
    @Trivial
    public void log(Logger useLogger) {
        String methodName = "<init>";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Annotations: START: [ {0} ]", getHashText());

        i_packageAnnotations.log(useLogger);
        i_classAnnotations.log(useLogger);
        i_fieldAnnotations.log(useLogger);
        i_methodAnnotations.log(useLogger);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Annotations: END: [ {0} ]", getHashText());
    }

    //

    @Override
    public void recordPackageAnnotation(String i_packageName, String i_annotationClassName) {
        i_packageAnnotations.i_record(i_packageName, i_annotationClassName);
    }

    @Override
    public void recordClassAnnotation(String i_className, String i_annotationClassName) {
        i_classAnnotations.i_record(i_className, i_annotationClassName);
    }

    @Override
    public void recordFieldAnnotation(String i_className, String i_annotationClassName) {
        i_fieldAnnotations.i_record(i_className, i_annotationClassName);
    }

    @Override
    public void recordMethodAnnotation(String i_className, String i_annotationClassName) {
        i_methodAnnotations.i_record(i_className, i_annotationClassName);
    }

    public void record(TargetsVisitorClassImpl.ClassData classData) {
        if ( !classData.isClass ) {
            Set<String> use_i_packageAnnotations = classData.i_classAnnotations;
            if ( use_i_packageAnnotations != null ) {
                String i_packageName = classData.i_className;

                for (String i_annotationClassName : use_i_packageAnnotations ) {
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

            Set<String> use_i_classAnnotations = classData.i_classAnnotations;
            if ( use_i_classAnnotations != null ) {
                for (String i_annotationClassName : use_i_classAnnotations ) {
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
     * @param annoTable The table which is to be added to this table.
     * @param i_addedPackageNames The names of packages for which to added data.
     * @param i_addedClassNames The names of classes for which to added data.
     */
    @Trivial
    protected void restrictedAdd(TargetsTableAnnotationsImpl annoTable,
                                 Set<String> i_addedPackageNames,
                                 Set<String> i_addedClassNames) {

        String methodName = "restrictedAdd";

        Object[] logParms;

        if ( logger.isLoggable(Level.FINER) ) {
            logParms = new Object[] { getHashText(), null };

            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", logParms);

            logParms[1] = printString(i_addedClassNames);
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Added classes [ {1} ]", logParms);

            logParms[1] = printString( i_classAnnotations.getHolderSet() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Initial annotated classes [ {1} ]", logParms);
            logParms[1] = printString( i_classAnnotations.getHeldSet() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Initial class annotations [ {1} ]", logParms);

            logParms[1] = printString( annoTable.i_classAnnotations.getHolderSet() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Other annotated classes [ {1} ]", logParms);
            logParms[1] = printString( annoTable.i_classAnnotations.getHeldSet() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Other class annotations [ {1} ]", logParms);

        } else {
            logParms = null;
        }

        i_packageAnnotations.i_record(
            annoTable.i_getPackageAnnotations(),
            i_addedPackageNames );

        i_classAnnotations.i_record(
            annoTable.i_getClassAnnotations(),
            i_addedClassNames );

        i_fieldAnnotations.i_record(
            annoTable.i_getFieldAnnotations(),
            i_addedClassNames );

        i_methodAnnotations.i_record(
            annoTable.i_getMethodAnnotations(),
            i_addedClassNames );

        if ( logParms != null ) {
            logParms[1] = printString( i_classAnnotations.getHolderSet() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Final annotated classes [ {1} ]", logParms);
            logParms[1] = printString( i_classAnnotations.getHeldSet() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Final class annotations [ {1} ]", logParms);

            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN", getHashText());
        }
    }

    //

    @Trivial
    private String printString(Set<String> values) {
        if ( values.isEmpty() ) {
            return "{ }";

        } else if ( values.size() == 1 ) {
            for ( String value : values ) {
                return "{ " + value + " }";
            }
            return null; // Unreachable

        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("{ ");
            boolean first = true;
            for ( String value : values ) {
                if ( !first ) {
                    builder.append(", ");
                } else {
                    first = false;
                }
                builder.append(value);
            }
            builder.append(" }");
            return builder.toString();
        }
    }

    //

    public boolean sameAs(TargetsTableAnnotationsImpl otherTable, boolean isCongruent) {
        if ( otherTable == null ) {
            return false;
        } else if ( otherTable == this ) {
            return true;
        }

        if ( !i_getPackageAnnotations().i_equals( otherTable.i_getPackageAnnotations(), isCongruent ) ) {
            return false;
        }

        if ( !i_getClassAnnotations().i_equals( otherTable.i_getClassAnnotations(), isCongruent ) ) {
            return false;
        }

        if ( !i_getFieldAnnotations().i_equals( otherTable.i_getFieldAnnotations(), isCongruent ) ) {
            return false;
        }

        if ( !i_getMethodAnnotations().i_equals( otherTable.i_getMethodAnnotations(), isCongruent ) ) {
            return false;
        }

        return true;
    }

    //

    @Override
    public List<TargetCache_ParseError> readUsing(TargetCache_Reader reader) throws IOException {
        return reader.read(this);
    }

    @Trivial
    public void updateClassNames(
        Set<String> i_allResolvedClassNames, Set<String> i_newlyResolvedClassNames,
        Set<String> i_allUnresolvedClassNames, Set<String> i_newlyUnresolvedClassNames) {

        i_getPackageAnnotations().update(
            i_allResolvedClassNames, i_newlyResolvedClassNames,
            i_allUnresolvedClassNames, i_newlyUnresolvedClassNames);

        i_getClassAnnotations().update(
            i_allResolvedClassNames, i_newlyResolvedClassNames,
            i_allUnresolvedClassNames, i_newlyUnresolvedClassNames);

        i_getFieldAnnotations().update(
            i_allResolvedClassNames, i_newlyResolvedClassNames,
            i_allUnresolvedClassNames, i_newlyUnresolvedClassNames);

        i_getMethodAnnotations().update(
            i_allResolvedClassNames, i_newlyResolvedClassNames,
            i_allUnresolvedClassNames, i_newlyUnresolvedClassNames);
    }

    //

    @Trivial
    public Set<String> uninternClassNames(Set<String> classNames) {
        if ( (classNames == null) || classNames.isEmpty() ) {
            return Collections.emptySet();
        } else {
            return new UtilImpl_NonInternSet( getClassNameInternMap(), classNames );
        }
    }

    @Override
    @Trivial
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
        return uninternClassNames( i_getAnnotationNames(category) ) ;
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
        return uninternClassNames( i_getPackageAnnotationNames() );
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
    @Trivial
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
    @Trivial
    public Set<String> getClassAnnotations() {
        return uninternClassNames( i_getClassAnnotationNames() );
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
    @Trivial
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
        return uninternClassNames( i_getFieldAnnotationNames() );
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
    @Trivial
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
        return uninternClassNames( i_getMethodAnnotationNames() );
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

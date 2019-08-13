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
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.ClassReader;

import org.jboss.jandex.Index;
import org.jboss.jandex.ClassInfo;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.jandex.internal.SparseClassInfo;
import com.ibm.ws.annocache.jandex.internal.SparseIndex;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.TargetsTable;
import com.ibm.ws.annocache.util.internal.UtilImpl_BidirectionalMap;
import com.ibm.ws.annocache.util.internal.UtilImpl_InternMap;
import com.ibm.ws.annocache.util.internal.UtilImpl_NonInternSet;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.classsource.ClassSource;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Streamer;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets.AnnotationCategory;
import com.ibm.wsspi.annocache.util.Util_BidirectionalMap;

/**
 * Annotation and class information:
 *
 * A targets table comprises class information, 
 */
public class TargetsTableImpl implements TargetsTable {
    // Logging ...

    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;
    protected static final Logger stateLogger = AnnotationCacheServiceImpl_Logging.ANNO_STATE_LOGGER;

    public static final String CLASS_NAME = TargetsTableImpl.class.getSimpleName();

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    //

    /**
     * Create targets data as a copy of source data.
     * 
     * This differs from the standard constructor in that the new targets data
     * creates its class and targets tables as copies of the tables from the source
     * data.
     *
     * The new targets data uses the same factory as the the source data.
     *
     * @param sourceData Source data from which to create new targets data.
     * @param classNameInternMap The class name intern map which the new data is to use.
     * @param fieldNameInternMap The field name intern map which the new data is to use.
     * @param methodSignatureInternMap The method signature intern map which the new data
     *     is to use.
     */
    public TargetsTableImpl(TargetsTableImpl sourceData,
                            UtilImpl_InternMap classNameInternMap,
                            UtilImpl_InternMap fieldNameInternMap,
                            UtilImpl_InternMap methodSignatureInternMap) {

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.factory = sourceData.getFactory();

        this.classNameInternMap = classNameInternMap;
        this.fieldNameInternMap = fieldNameInternMap;
        this.methodSignatureInternMap = methodSignatureInternMap;

        //

        this.classSourceName = sourceData.getClassSourceName();
        this.usedJandex = sourceData.getUsedJandex();

        this.useJandexFormat = sourceData.getUseJandexFormat();
        this.jandexIndex = sourceData.getJandexIndex();

        //

        this.stampTable = new TargetsTableTimeStampImpl( classSourceName, sourceData.getStamp() );
        this.classTable = new TargetsTableClassesImpl( sourceData.getClassTable(), classNameInternMap, classSourceName );
        this.annotationTable = new TargetsTableAnnotationsImpl( sourceData.getAnnotationTable(), classNameInternMap );

        //

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] [ {1} ]",
                        new Object[] { this.hashText, this.classSourceName });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] from [ {1} ]",
                        new Object[] { this.hashText, sourceData.getHashText() });
        }
    }

    /**
     * Create new targets data.
     *
     * Create new intern maps for class names, method names, and method signatures
     * for the new data.
     *
     * @param factory The factory used by the new targets data.
     * @param classSourceName The name to assign to the new targets data.
     */
    public TargetsTableImpl(AnnotationTargetsImpl_Factory factory,
                           String classSourceName,
                           boolean useJandexFormat) {

        this( factory,
              factory.createClassNameInternMap(),
              factory.createFieldNameInternMap(),
              factory.createMethodSignatureInternMap(),
              classSourceName,
              useJandexFormat );
    }

    /** Control parameter: Scanning will drive processing through a jandex index. */
    public static final boolean DO_USE_JANDEX_FORMAT = true;

    /** Control parameter: Scanning will not drive processing through a jandex index. */
    public static final boolean DO_NOT_USE_JANDEX_FORMAT = false;

    /**
     * Create new targets data.
     * 
     * @param factory The factory used by the new targets data.
     * 
     * @param classNameInternMap The class name intern map 
     * @param classNameInternMap The class name intern map which the new data is to use.
     * @param fieldNameInternMap The field name intern map which the new data is to use.
     * @param methodSignatureInternMap The method signature intern map which the new data
     *     is to use.
     * @param classSourceName The name to assign to the new targets data.
     * @param useJandexFormat True or false telling if Jandex format data is being used.
     */
    public TargetsTableImpl(AnnotationTargetsImpl_Factory factory,

                           UtilImpl_InternMap classNameInternMap,
                           UtilImpl_InternMap fieldNameInternMap,
                           UtilImpl_InternMap methodSignatureInternMap,

                           String classSourceName,

                           boolean useJandexFormat) {

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.factory = factory;

        this.classNameInternMap = classNameInternMap;
        this.fieldNameInternMap = fieldNameInternMap;
        this.methodSignatureInternMap = methodSignatureInternMap;

        //

        this.classSourceName = classSourceName;
        this.usedJandex = false;

        this.useJandexFormat = useJandexFormat;
        this.jandexIndex = null;

        //

        this.stampTable = new TargetsTableTimeStampImpl(classSourceName);
        this.classTable = new TargetsTableClassesImpl(factory.getUtilFactory(), classNameInternMap, classSourceName);
        this.annotationTable = new TargetsTableAnnotationsImpl(factory.getUtilFactory(), classNameInternMap);

        //

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] [ {1} ]", 
                        new Object[] { this.hashText, this.classSourceName });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] [ {1} ]",
                        new Object[] { this.hashText, this.stampTable.getHashText() });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] [ {1} ]",
                        new Object[] { this.hashText, this.classTable.getHashText() });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] [ {1} ]",
                        new Object[] { this.hashText, this.annotationTable.getHashText() });
        }
    }

    //

    protected final AnnotationTargetsImpl_Factory factory;

    @Trivial
    protected AnnotationTargetsImpl_Factory getFactory() {
        return factory;
    }

    protected Set<String> createIdentityStringSet() {
        return getFactory().getUtilFactory().createIdentityStringSet();
    }

    protected Set<String> createIdentityStringSet(int size) {
        return getFactory().getUtilFactory().createIdentityStringSet(size);
    }

    @Trivial
    protected Set<String> i_dupClassNames(Set<String> initialElements) {
        return getFactory().getUtilFactory().createIdentityStringSet(initialElements);
    }

    //

    protected final UtilImpl_InternMap classNameInternMap;

    @Trivial
    protected UtilImpl_InternMap getClassNameInternMap() {
        return classNameInternMap;
    }

    @Trivial
    protected Set<String> uninternClassNames(Set<String> i_classNames) {
        if ( i_classNames == null ) {
            return null;
        } else if ( i_classNames.isEmpty() ) {
            return Collections.emptySet();
        } else {
            return new UtilImpl_NonInternSet( getClassNameInternMap(), i_classNames );
        }
    }

    @Override
    public String internClassName(String className) {
        return getClassNameInternMap().intern(className);
    }

    public String internClassName(String className, boolean doForce) {
        return getClassNameInternMap().intern(className, doForce);
    }

    protected final UtilImpl_InternMap fieldNameInternMap;

    @Trivial
    protected UtilImpl_InternMap getFieldNameInternMap() {
        return fieldNameInternMap;
    }

    @Trivial
    protected Set<String> uninternFieldNames(Set<String> i_fieldNames) {
        if ( i_fieldNames == null ) {
            return null;
        } else if ( i_fieldNames.isEmpty() ) {
            return Collections.emptySet();
        } else {
            return new UtilImpl_NonInternSet( getFieldNameInternMap(), i_fieldNames );
        }
    }

    @Override
    public String internFieldName(String fieldName) {
        return getFieldNameInternMap().intern(fieldName);
    }

    protected String internFieldName(String fieldName, boolean doForce) {
        return getFieldNameInternMap().intern(fieldName, doForce);
    }

    protected final UtilImpl_InternMap methodSignatureInternMap;

    @Trivial
    protected UtilImpl_InternMap getMethodSignatureInternMap() {
        return methodSignatureInternMap;
    }

    @Override
    public String internMethodSignature(String methodSignature) {
        return getMethodSignatureInternMap().intern(methodSignature);
    }

    public String internMethodSignature(String methodSignature, boolean doForce) {
        return getMethodSignatureInternMap().intern(methodSignature, doForce);
    }

    //

    protected final String classSourceName;

    @Trivial
    public String getClassSourceName() {
        return classSourceName;
    }

    protected boolean usedJandex;

    @Trivial
    @Override
    public boolean getUsedJandex() {
        return usedJandex;
    }

    protected void setUsedJandex(boolean usedJandex) {
        this.usedJandex = usedJandex;
    }

    // Data fan-outs ...

    protected final TargetsTableTimeStampImpl stampTable;

    @Override
    @Trivial
    public TargetsTableTimeStampImpl getStampTable() {
        return stampTable;
    }

    @Override
    public String setName(String name) {
        return getStampTable().setName(name);
    }

    @Override
    @Trivial
    public String getName() {
        return getStampTable().getName();
    }

    @Override
    public String setStamp(String stamp) {
        return getStampTable().setStamp(stamp);
    }

    @Override
    @Trivial
    public String getStamp() {
        return getStampTable().getStamp();
    }

    //

    protected final TargetsTableClassesImpl classTable;

    @Override
    @Trivial
    public TargetsTableClassesImpl getClassTable() {
        return classTable;
    }

    @Override
    public Set<String> getPackageNames() {
        return getClassTable().getPackageNames();
    }

    @Override
    public Set<String> i_getPackageNames() {
        return getClassTable().i_getPackageNames();
    }

    @Override
    public boolean containsPackageName(String packageName) {
        return getClassTable().containsPackageName(packageName);
    }

    @Override
    public boolean i_containsPackageName(String i_packageName) {
        return getClassTable().i_containsPackageName(i_packageName);
    }

    @Override
    @Trivial
    public Set<String> getClassNames() {
        return getClassTable().getClassNames();
    }

    @Override
    @Trivial
    public Set<String> i_getClassNames() {
        return getClassTable().i_getClassNames();
    }

    @Override
    public boolean containsClassName(String className) {
        return getClassTable().containsClassName(className);
    }

    @Override
    public boolean i_containsClassName(String i_className) {
        return getClassTable().i_containsClassName(i_className);
    }

    //

    @Trivial
    protected Map<String, String> i_getSuperclassNameMap() {
        return getClassTable().i_getSuperclassNames();
    }

    @Override
    public String getSuperclassName(String subclassName) {
        return getClassTable().getSuperclassName(subclassName);
    }

    @Override
    public String i_getSuperclassName(String i_subclassName) {
        return getClassTable().i_getSuperclassName(i_subclassName);
    }

    //

    @Trivial
    protected Map<String, String[]> i_getInterfaceNamesMap() {
        return getClassTable().i_getInterfaceNames();
    }

    @Override
    public String[] getInterfaceNames(String classOrInterfaceName) {
        return getClassTable().getInterfaceNames(classOrInterfaceName);
    }

    @Override
    public String[] i_getInterfaceNames(String i_classOrInterfaceName) {
        return getClassTable().i_getInterfaceNames(i_classOrInterfaceName);
    }

    //

    protected final TargetsTableAnnotationsImpl annotationTable;

    //

    @Override
    public TargetsTableAnnotationsImpl getAnnotationTable() {
        return annotationTable;
    }

    //

    @Override
    public UtilImpl_BidirectionalMap i_getAnnotations(AnnotationCategory category) {
        return getAnnotationTable().i_getAnnotations(category);
    }

    @Override
    public Util_BidirectionalMap i_getPackageAnnotations() {
        return getAnnotationTable().i_getPackageAnnotations();
    }

    @Override
    public Util_BidirectionalMap i_getClassAnnotations() {
        return getAnnotationTable().i_getClassAnnotations();
    }

    @Override
    public Util_BidirectionalMap i_getFieldAnnotations() {
        return getAnnotationTable().i_getFieldAnnotations();
    }

    @Override
    public Util_BidirectionalMap i_getMethodAnnotations() {
        return getAnnotationTable().i_getMethodAnnotations();
    }

    //

    @Override
    @Trivial
    public Set<String> i_getAnnotatedTargets(AnnotationCategory category) {
        return getAnnotationTable().i_getAnnotatedTargets(category);
    }

    @Override
    public Set<String> i_getAnnotatedTargets(AnnotationCategory category, String i_annotationName) {
        return getAnnotationTable().i_getAnnotatedTargets(category, i_annotationName);
    }

    @Override
    @Trivial
    public Set<String> i_getAnnotationNames(AnnotationCategory category) {
        return getAnnotationTable().i_getAnnotationNames(category);
    }

    @Override
    public Set<String> i_getAnnotations(AnnotationCategory category, String i_classOrPackageName) {
        return getAnnotationTable().i_getAnnotations(category, i_classOrPackageName);
    }

    //

    @Override
    @Trivial
    public Set<String> getAnnotatedTargets(AnnotationCategory category) {
        return getAnnotationTable().getAnnotatedTargets(category);
    }

    @Override
    public Set<String> getAnnotatedTargets(AnnotationCategory category, String annotationName) {
        return getAnnotationTable().getAnnotatedTargets(category, annotationName);
    }

    @Override
    @Trivial
    public Set<String> getAnnotations(AnnotationCategory category) {
        return getAnnotationTable().getAnnotations(category);
    }

    @Override
    public Set<String> getAnnotations(AnnotationCategory category, String classOrPackageName) {
        return getAnnotationTable().getAnnotations(category, classOrPackageName);
    }

    //

    @Override
    @Trivial
    public Set<String> getPackagesWithAnnotations() {
        return getAnnotationTable().getPackagesWithAnnotations();
    }

    @Override
    @Trivial
    public Set<String> i_getPackagesWithAnnotations() {
        return getAnnotationTable().i_getPackagesWithAnnotations();
    }

    @Override
    public Set<String> getPackagesWithAnnotation(String annotationName) {
        return getAnnotationTable().getPackagesWithAnnotation(annotationName);
    }

    @Override
    public Set<String> i_getPackagesWithAnnotation(String i_annotationName) {
        return getAnnotationTable().i_getPackagesWithAnnotation(i_annotationName);
    }

    @Override
    public Set<String> getPackageAnnotations() {
        return getAnnotationTable().getPackageAnnotations();
    }

    @Override
    public Set<String> i_getPackageAnnotationNames() {
        return getAnnotationTable().i_getPackageAnnotationNames();
    }

    @Override
    public Set<String> getPackageAnnotations(String packageName) {
        return getAnnotationTable().getPackageAnnotations(packageName);
    }

    @Override
    public Set<String> i_getPackageAnnotations(String i_packageName) {
        return getAnnotationTable().i_getPackageAnnotations(i_packageName);
    }

    //

    @Override
    @Trivial
    public Set<String> getClassesWithClassAnnotations() {
        return getAnnotationTable().getClassesWithClassAnnotations();
    }

    @Override
    @Trivial
    public Set<String> i_getClassesWithClassAnnotations() {
        return getAnnotationTable().i_getClassesWithClassAnnotations();
    }

    @Override
    public Set<String> getClassesWithClassAnnotation(String annotationName) {
        return getAnnotationTable().getClassesWithClassAnnotation(annotationName);
    }

    @Override
    public Set<String> i_getClassesWithClassAnnotation(String i_annotationName) {
        return getAnnotationTable().i_getClassesWithClassAnnotation(i_annotationName);
    }

    @Override
    @Trivial
    public Set<String> getClassAnnotations() {
        return getAnnotationTable().getClassAnnotations();
    }

    @Override
    @Trivial
    public Set<String> i_getClassAnnotationNames() {
        return getAnnotationTable().i_getClassAnnotationNames();
    }

    @Override
    public Set<String> getClassAnnotations(String className) {
        return getAnnotationTable().getClassAnnotations(className);
    }

    @Override
    public Set<String> i_getClassAnnotations(String i_className) {
        return getAnnotationTable().i_getClassAnnotations(i_className);
    }

    //

    @Override
    @Trivial
    public Set<String> getClassesWithFieldAnnotations() {
        return getAnnotationTable().getClassesWithFieldAnnotations();
    }

    @Override
    @Trivial
    public Set<String> i_getClassesWithFieldAnnotations() {
        return getAnnotationTable().i_getClassesWithFieldAnnotations();
    }

    @Override
    public Set<String> getClassesWithFieldAnnotation(String annotationName) {
        return getAnnotationTable().getClassesWithFieldAnnotation(annotationName);
    }

    @Override
    public Set<String> i_getClassesWithFieldAnnotation(String i_annotationName) {
        return getAnnotationTable().i_getClassesWithFieldAnnotation(i_annotationName);
    }

    @Override
    @Trivial
    public Set<String> getFieldAnnotations() {
        return getAnnotationTable().getFieldAnnotations();
    }

    @Override
    @Trivial
    public Set<String> i_getFieldAnnotationNames() {
        return getAnnotationTable().i_getFieldAnnotationNames();
    }

    @Override
    public Set<String> getFieldAnnotations(String className) {
        return getAnnotationTable().getFieldAnnotations(className);
    }

    @Override
    public Set<String> i_getFieldAnnotations(String i_className) {
        return getAnnotationTable().i_getFieldAnnotations(i_className);
    }

    //

    @Override
    @Trivial
    public Set<String> getClassesWithMethodAnnotations() {
        return getAnnotationTable().getClassesWithMethodAnnotations();
    }

    @Override
    @Trivial
    public Set<String> i_getClassesWithMethodAnnotations() {
        return getAnnotationTable().i_getClassesWithMethodAnnotations();
    }

    @Override
    public Set<String> getClassesWithMethodAnnotation(String annotationName) {
        return getAnnotationTable().getClassesWithMethodAnnotation(annotationName);
    }

    @Override
    public Set<String> i_getClassesWithMethodAnnotation(String i_annotationName) {
        return getAnnotationTable().i_getClassesWithMethodAnnotation(i_annotationName);
    }

    @Override
    @Trivial
    public Set<String> getMethodAnnotations() {
        return getAnnotationTable().getMethodAnnotations();
    }

    @Override
    @Trivial
    public Set<String> i_getMethodAnnotationNames() {
        return getAnnotationTable().i_getMethodAnnotationNames();
    }

    @Override
    public Set<String> getMethodAnnotations(String className) {
        return getAnnotationTable().getMethodAnnotations(className);
    }

    @Override
    public Set<String> i_getMethodAnnotations(String i_className) {
        return getAnnotationTable().i_getMethodAnnotations(i_className);
    }

    //

    public int countAnnotations() {
        return ( getPackageAnnotations().size() +
                 getClassAnnotations().size() +
                 getFieldAnnotations().size() +
                 getMethodAnnotations().size() );
    }

    //

    @Override
    @Trivial
    public void logState() {
        if (stateLogger.isLoggable(Level.FINER)) {
            log(stateLogger);
        }
    }

    @Override
    @Trivial
    public void log(Logger useLogger) {
        String methodName = "log";

        if (!useLogger.isLoggable(Level.FINER)) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BEGIN STATE [ {0} ]", getHashText());

        getClassTable().log(useLogger);
        getAnnotationTable().log(useLogger);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "END STATE [ {0} ]", getHashText());
    }

    //

    // Always close the class source, even if the open
    // failed with an exception.  That is to guard against
    // partial opens.

    /**
     * Top scanning entry point: Scan a class source.  Do not record
     * class reference data (resolved and unresolved class names).  Do
     * record all annotation occurances.
     * 
     * See {@link #scanInternal(ClassSource, Set, Set, Set, Set, Set)}.
     *
     * @param classSource The class source which is to be scanned.
     *
     * @throws AnnotationTargets_Exception Thrown if an error occurs during
     *     the scan.
     */
    protected void scanInternal(ClassSource classSource)
        throws AnnotationTargets_Exception {

        scanInternal( classSource,

                      TargetsVisitorClassImpl.DONT_RECORD_NEW_RESOLVED,
                      TargetsVisitorClassImpl.DONT_RECORD_RESOLVED,

                      TargetsVisitorClassImpl.DONT_RECORD_NEW_UNRESOLVED,
                      TargetsVisitorClassImpl.DONT_RECORD_UNRESOLVED,

                      TargetsVisitorClassImpl.SELECT_ALL_ANNOTATIONS ); // throws AnnotationTargets_Exception
    }

    /**
     * <p>Scan all of the classes of a class source.</p>
     *
     * @param classSource The class source which is to be scanned.
     *
     * @param i_newResolvedClassNames The names of new unresolved classes.
     * @param i_resolvedClassNames The names of previously resolved classes.
     *
     * @param i_newUnresolvedClassNames The names of new unresolved classes.
     * @param i_unresolvedClassNames The names of unresolved classes.
     *
     * @param i_selectAnnotationClassNames Filter of annotation class names which are to be recorded.
     *
     * @throws AnnotationTargets_Exception Thrown in case of a scan error.
     */
    @Trivial
    protected void scanInternal(final ClassSource classSource,

                                final Set<String> i_newResolvedClassNames,
                                final Set<String> i_resolvedClassNames,

                                final Set<String> i_newUnresolvedClassNames,
                                final Set<String> i_unresolvedClassNames,

                                final Set<String> i_selectAnnotationClassNames)

        throws AnnotationTargets_Exception {

        String methodName = "scanInternal";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] ENTER [ {1} ]",
                        new Object[] { getHashText(), classSource });
        }

        try {
            openClassSource(classSource); // throws AnnotationTargets_Exception

            ClassSource_Streamer useStreamer = new ClassSource_Streamer() {
                @Override
                public boolean doProcess(String className) {
                    return true;
                }

                private final TargetsVisitorClassImpl visitor =
                    new TargetsVisitorClassImpl(
                        TargetsTableImpl.this,
                        classSource.getCanonicalName(),
                        i_newResolvedClassNames, i_resolvedClassNames,
                        i_newUnresolvedClassNames, i_unresolvedClassNames,
                        i_selectAnnotationClassNames,
                        TargetsVisitorClassImpl.DO_NOT_RECORD_ANNOTATION_DETAIL );

                @Override
                public boolean process(String i_className, InputStream inputStream) throws ClassSource_Exception {
                    return apply(visitor, i_className, inputStream); // throws ClassSource_Exception
                }

                @Override
                public boolean supportsJandex() {
                    return true;
                }

                @Override
                public boolean createJandex() {
                    return TargetsTableImpl.this.getUseJandexFormat();
                }

                @Override
                public void storeJandex(Object useJandexIndex) {
                    TargetsTableImpl.this.setJandexIndex((Index) useJandexIndex);
                }

                @Override
                public void processJandex() {
                    setUsedJandex(true);
                }

                private final TargetsVisitorJandexConverterImpl jandexConverter =
                    new TargetsVisitorJandexConverterImpl(TargetsTableImpl.this);

                @Override
                public boolean processJandex(Object jandexClassInfo) throws ClassSource_Exception {
                    return jandexConverter.convertClassInfo(classSourceName, jandexClassInfo);
                }

                private final TargetsVisitorSparseJandexConverterImpl sparseJandexConverter =
                    new TargetsVisitorSparseJandexConverterImpl(TargetsTableImpl.this);

                @Override
                public boolean processSparseJandex(Object jandexClassInfo) throws ClassSource_Exception {
                    return sparseJandexConverter.convertClassInfo(classSourceName, jandexClassInfo);
                }

                // Obsolete
                @Override
                public boolean doProcess(String className, ScanPolicy scanPolicy) {
                    return false;
                }

                @Override
                public boolean process(String sourceName, String className, InputStream inputStream,
                    ScanPolicy scanPolicy) throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
                    return false;
                }

                @Override
                public boolean process(String sourceName, Object jandexClassInfo, ScanPolicy scanPolicy)
                    throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
                    return false;
                }
            };

            try {
                classSource.process(useStreamer); // throws ClassSource_Exception
            } catch ( ClassSource_Exception e ) {
                throw getFactory().wrapIntoAnnotationTargetsException(logger, CLASS_NAME, methodName,
                                                                      "Failed to scan class source", e);
            }

        } finally {
            closeClassSource(classSource); // throws AnnotationTargets_Exception
        }

        logState();

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN", getHashText());
        }
    }

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

    /**
     * <p>Scan unresolved classes of the tail external class source.  Don't
     * record any annotations of this class source.</p>
     *
     * @param classSource The class source which is to be scanned.
     * @param i_resolvedClassNames The names of previously resolved classes.
     * @param i_unresolvedClassNames The names of unresolved classes.
     *
     * @throws AnnotationTargets_Exception Thrown in case of a scan error.
     */
    @Trivial
    protected void scanExternal(ClassSource classSource,
                                Set<String> i_resolvedClassNames,
                                Set<String> i_unresolvedClassNames)
        throws AnnotationTargets_Exception {

        String methodName = "scanExternal";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] [ {1} ] ENTER Resolved [ {2} ] Unresolved [ {3} ]",
                        new Object[] { getHashText(), classSource.getCanonicalName(),
                                       Integer.valueOf(i_resolvedClassNames.size()),
                                       Integer.valueOf(i_unresolvedClassNames.size()) });
        }

        if ( i_unresolvedClassNames.isEmpty() ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] [ {1} ] RETURN Immedate; Empty Unresolved",
                            new Object[] { getHashText(), classSource.getCanonicalName() });
            }
            return;
        }

        boolean didOpen = false;

        try {
            Set<String> i_nextUnresolvedClassNames = i_dupClassNames(i_unresolvedClassNames);

            while ( !i_nextUnresolvedClassNames.isEmpty() ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] [ {1} ] Next Unresolved [ {2} ]",
                            new Object[] { getHashText(), classSource.getName(),
                                           printString(i_nextUnresolvedClassNames) });

                Set<String> i_newResolvedClassNames = createIdentityStringSet();
                Set<String> i_newUnresolvedClassNames = createIdentityStringSet();

                if ( !didOpen ) {
                    openClassSource(classSource); // throws AnnotationTargets_Exception
                    didOpen = true;
                }

                final Logger innerLogger = logger;

                final TargetsVisitorClassImpl visitor =
                    new TargetsVisitorClassImpl(
                        this,
                        classSource.getName(),
                        i_newResolvedClassNames, i_resolvedClassNames,
                        i_newUnresolvedClassNames, i_unresolvedClassNames,
                        TargetsVisitorClassImpl.SELECT_NO_ANNOTATIONS,
                        TargetsVisitorClassImpl.DO_NOT_RECORD_ANNOTATION_DETAIL );

                ClassSource_Streamer useStreamer = new ClassSource_Streamer() {
                    @Override
                    public boolean doProcess(String className) {
                        return true;
                    }

                    @Override
                    public boolean process(String i_className, InputStream inputStream) throws ClassSource_Exception {
                        String interMethodName = "scanExternal.process";
                        if ( innerLogger.isLoggable(Level.FINER) ) {
                            innerLogger.logp(Level.FINER, CLASS_NAME, interMethodName, "Class [ {0} ]", i_className);
                        }
                        return apply(visitor, i_className, inputStream); // throws ClassSource_Exception
                    }

                    @Override
                    public boolean supportsJandex() {
                        return false;
                    }

                    @Override
                    public boolean createJandex() {
                        return false;
                    }

                    @Override
                    public void storeJandex(Object useJandexIndex) {
                        throw new UnsupportedOperationException("External scans do not use Jandex");
                    }

                    @Override
                    public void processJandex() {
                        // 'supportsJandex' answering false should prevent calls to 'processJandex'.
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean processJandex(Object jandexClassInfo) throws ClassSource_Exception {
                        throw new UnsupportedOperationException("External scans do not use Jandex");
                    }

                    @Override
                    public boolean processSparseJandex(Object jandexClassInfo) throws ClassSource_Exception {
                        throw new UnsupportedOperationException("External scans do not use Jandex");
                    }

                    // Obsolete
                    @Override
                    public boolean doProcess(String className, ScanPolicy scanPolicy) {
                        return false;
                    }

                    @Override
                    public boolean process(String sourceName, String className, InputStream inputStream,
                        ScanPolicy scanPolicy) throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
                        return false;
                    }

                    @Override
                    public boolean process(String sourceName, Object jandexClassInfo, ScanPolicy scanPolicy)
                        throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
                        return false;
                    }
                };

                try {
                    classSource.processSpecific(useStreamer, i_nextUnresolvedClassNames); // throws ClassSource_Exception

                } catch ( ClassSource_Exception e ) {
                    throw getFactory().wrapIntoAnnotationTargetsException(logger, CLASS_NAME, methodName,
                                                                          "Failed to scan class source", e);
                }

                i_nextUnresolvedClassNames.clear();
                i_nextUnresolvedClassNames.addAll(i_newUnresolvedClassNames);
            }

        } finally {
            if ( didOpen ) {
                closeClassSource(classSource); // throws AnnotationTargets_Exception
            }
        }

        logState();

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] [ {1} ] RETURN Resolved [ {2} ] Unresolved [ {3} ]",
                        new Object[] { getHashText(), classSource.getName(),
                                       Integer.valueOf(i_resolvedClassNames.size()),
                                       Integer.valueOf(i_unresolvedClassNames.size()) });
        }
    }

    // Always close the class source, even if the open failed
    // with an exception.  That is to guard against partial opens.

    /**
     * <p>Scan all of the classes of a class source.</p>
     *
     * @param classSource The class source which is to be scanned.
     * @param i_unresolvedClassNames The names of classes which are to be scanned.
     * @param i_resolvedClassNames The names of previously resolved classes.
     * @param i_specificAnnotationClassNames The names annotation classes which
     *     are to be recorded.
     *
     * @throws AnnotationTargets_Exception Thrown in case of a scan error.
     */
    @Trivial
    protected void scanSpecific(ClassSource classSource,
                                Set<String> i_unresolvedClassNames,
                                Set<String> i_resolvedClassNames,
                                Set<String> i_specificAnnotationClassNames)
        throws AnnotationTargets_Exception {

        String methodName = "scanSpecific";
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] ENTER [ {1} ]",
                        new Object[] { getHashText(), classSource });
        }

        try {
            openClassSource(classSource); // throws AnnotationTargets_Exception

            final TargetsVisitorClassImpl visitor =
                new TargetsVisitorClassImpl(
                    this,
                    classSource.getCanonicalName(),
                    TargetsVisitorClassImpl.DONT_RECORD_NEW_UNRESOLVED, i_unresolvedClassNames,
                    TargetsVisitorClassImpl.DONT_RECORD_NEW_RESOLVED, i_resolvedClassNames,
                    i_specificAnnotationClassNames,
                    TargetsVisitorClassImpl.DO_NOT_RECORD_ANNOTATION_DETAIL );

            // Need to dup the collection to enable iteration;
            // the unresolved class names set is updated by the visitor.
            Set<String> i_useUnresolvedClassNames = i_dupClassNames(i_unresolvedClassNames);

            ClassSource_Streamer streamer = new ClassSource_Streamer() {
                @Override
                public boolean doProcess(String className) {
                    return true;
                }

                @Override
                public boolean process(String i_className, InputStream inputStream) throws ClassSource_Exception {
                    return apply(visitor, i_className, inputStream); // throws ClassSource_Exception
                }
                
                @Override
                public boolean supportsJandex() {
                    return false;
                }

                @Override
                public boolean createJandex() {
                    return false;
                }

                @Override
                public void storeJandex(Object useJandexIndex) {
                    throw new UnsupportedOperationException("Specific scans do not use Jandex");
                }

                @Override
                public void processJandex() {
                    // 'supportsJandex' answering false should prevent calls to 'processJandex'.
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean processJandex(Object jandexClassInfo) throws ClassSource_Exception {
                    throw new UnsupportedOperationException("Specific scans do not use Jandex");
                }

                @Override
                public boolean processSparseJandex(Object jandexClassInfo) throws ClassSource_Exception {
                    throw new UnsupportedOperationException("Specific scans do not use Jandex");
                }

                // Obsolete

                @Override
                public boolean doProcess(String className, ScanPolicy scanPolicy) {
                    return false;
                }

                @Override
                public boolean process(String sourceName, String className, InputStream inputStream,
                    ScanPolicy scanPolicy) throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
                    return false;
                }

                @Override
                public boolean process(String sourceName, Object jandexClassInfo, ScanPolicy scanPolicy)
                    throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
                    return false;
                }
            };

            try {
                classSource.processSpecific(streamer, i_useUnresolvedClassNames);
                // throws ClassSource_Exception

            } catch ( ClassSource_Exception e ) {
                throw getFactory().wrapIntoAnnotationTargetsException(logger, CLASS_NAME, methodName,
                                                                      "Failed to scan class source", e);
            }

        } finally {
            closeClassSource(classSource); // throws AnnotationTargets_Exception
        }

        logState();

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN", getHashText());
        }
    }

    protected void openClassSource(ClassSource classSource) throws AnnotationTargets_Exception {
        String methodName = "openClassSource";

        // Open failures should be rare: The default aggregate class source
        // implementation handles failures to open child class sources by
        // masking out those child class sources.  Processing is allowed
        // to continue.

        try {
            classSource.open(); // 'open' throws ClassSource_Exception
        } catch ( ClassSource_Exception e ) {
            throw getFactory().wrapIntoAnnotationTargetsException(logger, CLASS_NAME, methodName,
                                                                  "Failed to open class source", e);
        }
    }

    protected void closeClassSource(ClassSource classSource) throws AnnotationTargets_Exception {
        String methodName = "closeClassSource";

        // Close failures should be rare: The default aggregate class source
        // implementation internally handles failures to close child class sources.

        try {
            classSource.close(); // 'close' throws ClassSource_Exception
        } catch ( ClassSource_Exception e ) {
            throw getFactory().wrapIntoAnnotationTargetsException(logger, CLASS_NAME, methodName,
                                                                  "Failed to close class source", e);
        }
    }

    //

    // Do not emit FFDC for VisitEnded or ArrayIndexOutOfBounds:
    // 
    // VisitEnded occurs most often when the fully qualified name of a class does not match the location of
    // the class.  The first occurs frequently with generated JSP classes, which are by default written to a
    // location which does not match the fully qualified name of the generated JSP class.
    //
    // The second occurs when processing non-valid class data.
    //
    // For ArrayIndexOutOfBounds, while FFDC is not emitted, a warning *is* emitted.  This is a compromise
    // between either fully ignoring the non-valid class or emitting a full FFDC with stack.
    //
    // Possibly, the failure to process the class is not a problem, and the application deployer wants to
    // ignore the error.  Equally possible, the failure is a serious problem which will impact
    // application function.  Data for the non-valid class, including annotations data, will not be
    // recorded, and the class is not likely loadable by the server runtime.

    @FFDCIgnore({ TargetsVisitorClassImpl.VisitEnded.class, ArrayIndexOutOfBoundsException.class })
    @Trivial
    protected boolean apply(TargetsVisitorClassImpl visitor, String i_className, InputStream inputStream) {
        String methodName = "apply";
        Object[] logParms;
        if ( logger.isLoggable(Level.FINER) ) {
            logParms = new Object[] { getHashText(), i_className, null };
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER [ {1} ]", logParms);
        } else {
            logParms = null;
        }

        visitor.reset();
        visitor.setExternalName(i_className);

        boolean failedScan;

        try {
            // Both IOException and Exception have been seen;
            // in particular ArrayindexOutOfBoundsException for a non-valid class.

            ClassReader classReader = new ClassReader(inputStream); // throws IOException, Exception

            classReader.accept(visitor,
                               (ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE));
            // throws IOException, VisitEnded, Exception

            failedScan = false;

            if ( logParms != null ) {
                logParms[2] = "Success";
            }

        } catch ( IOException e ) {
            failedScan = true;

            // CWWKC0049W
            logger.logp(Level.WARNING, CLASS_NAME, methodName,
                    "[ {0} ] ANNO_TARGETS_FAILED_TO_CREATE_READER [ {1} ] ({2} : {3})",
                    new Object[] { getHashText(), i_className, e, e.getMessage() });
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Failed to create reader", e);

        } catch ( TargetsVisitorClassImpl.VisitEnded e ) {
            failedScan = true;

            if ( logParms != null ) {
                logParms[2] = "Halted: " + e.getEndCase();
            }

        } catch ( ArrayIndexOutOfBoundsException e ) {
            failedScan = true;

            // CWWKC0049W
            logger.logp(Level.WARNING, CLASS_NAME, methodName,
                    "[ {0} ] ANNO_TARGETS_CORRUPT_CLASS [ {1} ] ({2} : {3})",
                    new Object[] { getHashText(), i_className, e, e.getMessage() });
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Corrupt class", e);

        } catch ( Exception e ) {
            failedScan = true;

            // CWWKC0044W
            logger.logp(Level.WARNING, CLASS_NAME, methodName,
                    "[ {0} ] ANNO_TARGETS_SCAN_EXCEPTION [ {1} ] ({2} : {3})",
                    new Object[] { getHashText(), i_className, e, e.getMessage() });
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Scan exception", e);

            if ( logParms != null ) {
                logParms[2] = "Exception: " + e.getMessage();
            }
        }

        if ( logParms != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN [ {1} ]: [ {2} ]", logParms);
        }

        return ( !failedScan );
    }

    //

    protected void record(TargetsVisitorClassImpl.ClassData classData) {
        String methodName = "record";

        if ( getClassTable().record(classData) ) {
             getAnnotationTable().record(classData);

        } else {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    " [ {0} ] Unexpected duplication of class or package [ {1} ]",
                    new Object[] { getHashText(), classData.className });
        }
    }

    //
    
    
    /**
     * <p>Add data from a table into this table.</p>
     *
     * <p>Record new additions to the added packages and added
     * classes collections.</p>

     * @param table The table which is to be added to this table.
     * @param i_allAddedClassNames Names of classes previously added to this table.
     *     Updated with newly added class names.
     * @param i_allAddedPackageNames Names of packages previously added to this table
     *     Updated with newly added package names.
     */
    @Trivial
    protected void restrictedAdd(TargetsTableImpl table,
                                 Set<String> i_allAddedClassNames,
                                 Set<String> i_allAddedPackageNames) {

        // Use the 'allAdded' collections to know which packages and classes are new,
        // and which were previously added.
        //
        // Use the 'newlyAdded' collections to know which packages and classes were
        // just added.
        //
        // The 'newlyAdded' collections are poplulated during the initial merge of
        // the package and class names.
        //
        // Subsequent operations transfer additional data, but just for the newly
        // added packages and classes.

        Set<String> i_newlyAddedPackageNames = createIdentityStringSet();
        Set<String> i_newlyAddedClassNames = createIdentityStringSet();

        getClassTable().restrictedAdd( table.getClassTable(),
                                       i_newlyAddedPackageNames, i_allAddedPackageNames,
                                       i_newlyAddedClassNames, i_allAddedClassNames );

        getAnnotationTable().restrictedAdd( table.getAnnotationTable(),
                                            i_newlyAddedPackageNames, i_newlyAddedClassNames );
    }

    //

    public boolean sameAs(TargetsTableImpl otherTable, boolean isCongruent) {
        if ( otherTable == null ) {
            return false;
        } else if ( otherTable == this ) {
            return true;
        }

        return ( getClassTable().sameAs( otherTable.getClassTable(), isCongruent ) &&
                 getAnnotationTable().sameAs( otherTable.getAnnotationTable(), isCongruent ) );
    }

    //

    @Trivial
    public void updateClassNames(
        Set<String> i_allResolvedClassNames, Set<String> i_allUnresolvedClassNames) {

        Set<String> i_newlyResolvedClassNames = createIdentityStringSet();
        Set<String> i_newlyUnresolvedClassNames = createIdentityStringSet();

        getClassTable().updateClassNames(
            i_allResolvedClassNames, i_newlyResolvedClassNames,
            i_allUnresolvedClassNames, i_newlyUnresolvedClassNames);

        getAnnotationTable().updateClassNames(
            i_allResolvedClassNames, i_newlyResolvedClassNames,
            i_allUnresolvedClassNames, i_newlyUnresolvedClassNames);
    }

    //

    private final boolean useJandexFormat;

    public boolean getUseJandexFormat() {
        return useJandexFormat;
    }

    private Index jandexIndex;

    public void setJandexIndex(Index jandexIndex) {
        String methodName = "setJandexIndex";

        if ( logger.isLoggable(Level.FINER) ) {
            String sizeText;
            if ( jandexIndex == null ) {
                sizeText = "***NULL***";
            } else {
                sizeText =Integer.toString(jandexIndex.getKnownClasses().size());
            }
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ " + getName() + " ]" + " Classes [ " + sizeText + " ]"); 
        }

        this.jandexIndex = jandexIndex;
    }

    public Index getJandexIndex() {
        return jandexIndex;
    }

    public Index consumeJandexIndex() {
        Index useJandexIndex = jandexIndex;
        jandexIndex = null;
        return useJandexIndex;
    }

    //

    public void transfer(Index useIndex) {
        TargetsVisitorJandexConverterImpl jandexConverter =
            new TargetsVisitorJandexConverterImpl(this);

        String useClassSourceName = classSourceName;
        for ( ClassInfo jandexClassInfo : useIndex.getKnownClasses() ) {
            jandexConverter.convertClassInfo(useClassSourceName, jandexClassInfo);
        }
    }

    public void transfer(SparseIndex sparseIndex) {
        TargetsVisitorSparseJandexConverterImpl sparseJandexConverter =
            new TargetsVisitorSparseJandexConverterImpl(this);

        String useClassSourceName = classSourceName;
        for ( SparseClassInfo jandexClassInfo : sparseIndex.getKnownClasses() ) {
            sparseJandexConverter.convertClassInfo(useClassSourceName, jandexClassInfo);
        }
    }
}

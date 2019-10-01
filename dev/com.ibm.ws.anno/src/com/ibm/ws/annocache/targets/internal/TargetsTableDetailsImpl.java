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
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.TargetsTableDetails;
import com.ibm.ws.annocache.targets.cache.TargetCache_ParseError;
import com.ibm.ws.annocache.targets.cache.TargetCache_Readable;
import com.ibm.ws.annocache.targets.cache.TargetCache_Reader;

/**
 * <p>Annotation target details implementation.</p>
 */
public class TargetsTableDetailsImpl
    implements TargetsTableDetails, TargetCache_Readable {

    // Logging ...

    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    public static final String CLASS_NAME = TargetsTableDetailsImpl.class.getSimpleName();

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    // Core state ...

    @Trivial
    protected TargetsTableDetailsImpl(TargetsTableImpl parentData) {
        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.parentData = parentData;

        this.i_packageDetails = new IdentityHashMap<String, Map<String, String>>();
        this.i_classDetails = new IdentityHashMap<String, Map<String, String>>();
        this.i_methodDetails = new IdentityHashMap<String, Map<String, Map<String, String>>>();
        this.i_fieldDetails = new IdentityHashMap<String, Map<String, Map<String, String>>>();

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] of [ {1} ]",
                        new Object[] { this.hashText, this.parentData.getHashText() });
        }
    }

    //

    protected final TargetsTableImpl parentData;

    @Override
    @Trivial
    public TargetsTableImpl getParentData() {
        return parentData;
    }

    // Package details ...

    // package name -> annotation name -> annotation details
    protected final Map<String, Map<String, String>> i_packageDetails;

    @Trivial
    public Map<String, Map<String, String>> i_getPackageDetails() {
        return i_packageDetails;
    }

    @Override
    public Map<String, String> i_getPackageAnnotationDetails(String packageName) {
        return i_packageDetails.get(packageName);
    }

    @Override
    public String i_getPackageAnnotationDetail(String packageName, String annotationClassName) {
        Map<String, String> packageAnnotations = i_packageDetails.get(packageName);
        return ((packageAnnotations == null) ? null : packageAnnotations.get(annotationClassName));
    }

    // Class details ...

    // class name -> annotation name -> annotation details
    protected final Map<String, Map<String, String>> i_classDetails;

    @Trivial
    public Map<String, Map<String, String>> i_getClassDetails() {
        return i_classDetails;
    }

    @Override
    public Map<String, String> i_getClassAnnotationDetails(String i_className) {
        return i_classDetails.get(i_className);
    }

    @Override
    public String i_getClassAnnotationDetail(String i_className, String i_annotationClassName) {
        Map<String, String> classAnnotations = i_classDetails.get(i_className);
        return ((classAnnotations == null) ? null : classAnnotations.get(i_annotationClassName));
    }

    // Method details ...

    // class name -> method signature -> annotation name -> annotation details
    protected final Map<String, Map<String, Map<String, String>>> i_methodDetails;

    @Trivial
    public Map<String, Map<String, Map<String, String>>> i_getMethodDetails() {
        return i_methodDetails;
    }

    @Override
    public Map<String, Map<String, String>> i_getMethodAnnotationDetails(String i_className) {
        return i_methodDetails.get(i_className);
    }

    @Override
    public Map<String, String> i_getMethodAnnotationDetails(String i_className, String i_methodSignature) {
        Map<String, Map<String, String>> methodDataForClass = i_methodDetails.get(i_className);
        return ((methodDataForClass == null) ? null : methodDataForClass.get(i_methodSignature));
    }

    @Override
    public String i_getMethodAnnotationDetails(String i_className, String i_methodSignature, String i_annotationClassName) {
        Map<String, Map<String, String>> methodDataForClass = i_methodDetails.get(i_className);
        if (methodDataForClass == null) {
            return null;
        }

        Map<String, String> annotationDataForMethod = methodDataForClass.get(i_methodSignature);
        return ((annotationDataForMethod == null) ? null : annotationDataForMethod.get(i_methodSignature));
    }

    // Field details ...

    protected final Map<String, Map<String, Map<String, String>>> i_fieldDetails;

    @Trivial
    public Map<String, Map<String, Map<String, String>>> i_getFieldDetails() {
        return i_fieldDetails;
    }

    @Override
    public Map<String, Map<String, String>> i_getFieldAnnotationDetails(String i_className) {
        return i_fieldDetails.get(i_className);
    }

    @Override
    public Map<String, String> i_getFieldAnnotationDetails(String i_className, String i_fieldName) {
        Map<String, Map<String, String>> fieldDataForClass = i_fieldDetails.get(i_className);
        return ((fieldDataForClass == null) ? null : fieldDataForClass.get(i_fieldName));
    }

    @Override
    public String i_getFieldAnnotationDetails(String i_className, String i_fieldName, String i_annotationClassName) {
        Map<String, Map<String, String>> fieldDataForClass = i_fieldDetails.get(i_className);
        if (fieldDataForClass == null) {
            return null;
        }

        Map<String, String> annotationDataForMethod = fieldDataForClass.get(i_fieldName);
        return ((annotationDataForMethod == null) ? null : annotationDataForMethod.get(i_fieldName));
    }

    // Un-consolidated logging:
    //
    // Unsorted.
    // Emit the class, field, and method details in entirely separate blocks,
    // each which is across all classes.

    @Override
    @Trivial
    public void log(Logger useLogger) {
        String methodName = "log";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Source Annotations Details:");

        logPackageDetails(useLogger);
        logClassDetails(useLogger);
        logFieldDetails(useLogger);
        logMethodDetails(useLogger);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Annotations Details: Complete");
    }

    @Trivial
    protected void logPackageDetails(Logger useLogger) {
        String methodName = "logPackageDetails";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Package Annotation Details:");

        logAnnotations(useLogger, "Package", "", i_packageDetails);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Package Details: Complete");
    }

    @Trivial
    protected void logClassDetails(Logger useLogger) {
        String methodName = "logClassDetails";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Annotation Details:");

        logAnnotations(useLogger, "Class", "", i_classDetails);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Details: Complete");
    }

    @Trivial
    protected void logFieldDetails(Logger useLogger) {
        String methodName = "logFieldDetails";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Field Annotation Details:");

        for (Map.Entry<String, Map<String, Map<String, String>>> i_classFieldsEntry : i_fieldDetails.entrySet()) {
            String i_className = i_classFieldsEntry.getKey();
            Map<String, Map<String, String>> i_fieldEntries = i_classFieldsEntry.getValue();

            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class [ " + i_className + " ]");
            logAnnotations(useLogger, "Field", "  ", i_fieldEntries);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Field Annotation Details: Complete");
    }

    @Trivial
    protected void logMethodDetails(Logger useLogger) {
        String methodName = "logMethodDetails";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Method Annotation Details:");

        for (Map.Entry<String, Map<String, Map<String, String>>> i_classMethodsEntry : i_methodDetails.entrySet()) {
            String i_className = i_classMethodsEntry.getKey();
            Map<String, Map<String, String>> i_methodEntries = i_classMethodsEntry.getValue();

            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class [ " + i_className + " ]");
            logAnnotations(useLogger, "Method", "  ", i_methodEntries);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Method Annotation Details: Complete");
    }

    @Trivial
    protected void logAnnotations(Logger useLogger,
                                  String entryTypeName, String prefix,
                                  Map<String, Map<String, String>> i_entries) {

        String methodName = "logAnnotations";

        for (Map.Entry<String, Map<String, String>> i_entry : i_entries.entrySet()) {
            String i_entryName = i_entry.getKey();
            Map<String, String> i_entryAnnotations = i_entry.getValue();

            useLogger.logp(Level.FINER, CLASS_NAME, methodName, prefix + entryTypeName + " [ " + i_entryName + " ]");

            for (Map.Entry<String, String> i_annotationEntry : i_entryAnnotations.entrySet()) {
                String i_annotationClassName = i_annotationEntry.getKey();
                String annotationDetailText = i_annotationEntry.getValue();

                useLogger.logp(Level.FINER, CLASS_NAME, methodName, prefix + "  [ " + i_annotationClassName + " ] [ " + annotationDetailText + " ]");
            }
        }
    }

    // Consolidated logging:
    //
    // Sort on package, class, field, method, and annotation names.
    // Put the class details for each class in a single block.

    @Override
    @Trivial
    public void logConsolidated(Logger useLogger) {
        String methodName = "logConsolidated";
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Annotations Details:");

        logConsolidatedPackageDetails(useLogger);
        logConsolidatedClassDetails(useLogger);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Annotation Details: Complete");
    }

    @Trivial
    protected void logConsolidatedPackageDetails(Logger useLogger) {
        String methodName = "logConsolidatedPackageDetails";
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Package Annotation Details:");

        logConsolidatedAnnotations(useLogger, "Package", "", i_packageDetails);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Package Details: Complete");
    }

    @Trivial
    protected void logConsolidatedClassDetails(Logger useLogger) {
        String methodName = "logConsolidatedClassDetails";
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Details:");

        Set<String> i_classNames = new HashSet<String>();
        i_classNames.addAll(i_classDetails.keySet());
        i_classNames.addAll(i_fieldDetails.keySet());
        i_classNames.addAll(i_methodDetails.keySet());

        String[] i_sortedClassNames = i_classNames.toArray(new String[i_classNames.size()]);
        Arrays.sort(i_sortedClassNames);

        for (String i_className : i_sortedClassNames) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class [ " + i_className + " ]");

            Map<String, String> i_classAnnotations = i_classDetails.get(i_className);
            if (i_classAnnotations == null) {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  No class annotations");
            } else {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Class annotations:");
                logConsolidatedAnnotations(useLogger, "  ", i_classAnnotations);
            }

            Map<String, Map<String, String>> i_fieldAnnotations = i_fieldDetails.get(i_className);
            if (i_fieldAnnotations == null) {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  No field annotations");
            } else {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Field annotations:");
                logConsolidatedAnnotations(useLogger, "Field", "  ", i_fieldAnnotations);
            }

            Map<String, Map<String, String>> i_methodAnnotations = i_methodDetails.get(i_className);
            if (i_methodAnnotations == null) {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  No method annotations");
            } else {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Method annotations:");
                logConsolidatedAnnotations(useLogger, "Method", "  ", i_methodAnnotations);
            }
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Details: Complete");
    }

    // Consolidated logging:
    //
    // Sort on the entry names and the annotation class names.

    @Trivial
    protected void logConsolidatedAnnotations(Logger useLogger,
                                              String entryTypeName, String prefix,
                                              Map<String, Map<String, String>> i_entries) {
        String methodName = "logConsolidatedAnnotations";

        String[] i_sortedEntryNames = i_entries.keySet().toArray(new String[i_entries.size()]);
        Arrays.sort(i_sortedEntryNames);

        for (String i_entryName : i_sortedEntryNames) {
            Map<String, String> i_entryAnnotations = i_entries.get(i_entryName);

            useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        prefix + entryTypeName + " [ " + i_entryName + " ]");

            logConsolidatedAnnotations(useLogger, prefix, i_entryAnnotations);
        }
    }

    // Consolidated logging:
    //
    // Sort on the annotation class names.

    @Trivial
    protected void logConsolidatedAnnotations(Logger useLogger,
                                              String prefix,
                                              Map<String, String> i_annotations) {

        String methodName = "logConsolidatedAnnotations";

        String[] i_sortedAnnotationClassNames = i_annotations.keySet().toArray(new String[i_annotations.size()]);
        Arrays.sort(i_sortedAnnotationClassNames);

        for (String i_annotationClassName : i_sortedAnnotationClassNames) {
            String annotationDetailText = i_annotations.get(i_annotationClassName);

            useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        prefix + "  [ " + i_annotationClassName + " ] [ " + annotationDetailText + " ]");
        }
    }

    //

    protected void record(TargetsVisitorClassImpl.ClassData classData) {
        if ( !classData.isClass ) {
            Map<String, String> i_packageAnnotations = classData.i_classAnnotationsDetail;
            if ( i_packageAnnotations != null ) {
                i_putPackageAnnotations(classData.i_className, i_packageAnnotations);
            }

        } else {
            String i_className = classData.i_className;

            Map<String, String> i_classAnnotations = classData.i_classAnnotationsDetail;
            if ( i_classAnnotations != null ) {
                i_putClassAnnotations(i_className, i_classAnnotations);
            }

            Map<String, Map<String, String>> i_classFieldAnnotations = classData.i_classFieldAnnotationsDetail;
            if ( i_classFieldAnnotations != null ) {
                i_putFieldAnnotations(i_className, i_classFieldAnnotations);
            }

            Map<String, Map<String, String>> i_classMethodAnnotations = classData.i_classMethodAnnotationsDetail;
            if ( i_classMethodAnnotations != null ) {
                i_putMethodAnnotations(i_className, i_classMethodAnnotations);
            }
        }
    }

    protected void i_putPackageAnnotations(String i_packageName, Map<String, String> i_annotations) {
        i_packageDetails.put(i_packageName, i_annotations);
    }

    protected void i_putClassAnnotations(String i_className, Map<String, String> i_annotations) {
        i_classDetails.put(i_className, i_annotations);
    }

    protected void i_putFieldAnnotations(String i_className,
                                         Map<String, Map<String, String>> i_classFieldAnnotations) {
        i_fieldDetails.put(i_className, i_classFieldAnnotations);
    }

    protected void i_putMethodAnnotations(String i_className,
                                          Map<String, Map<String, String>> i_classMethodAnnotations) {
        i_methodDetails.put(i_className, i_classMethodAnnotations);
    }

    @Override
    public void i_putPackageAnnotation(String i_packageName,
                                       String i_annotationClassName, String annotationDetail) {
        Map<String, String> i_packageAnnotations = i_packageDetails.get(i_packageName);
        if ( i_packageAnnotations == null ) {
            i_packageAnnotations = new IdentityHashMap<String, String>();
            i_packageDetails.put(i_packageName, i_packageAnnotations);
        }

        i_packageAnnotations.put(i_annotationClassName, annotationDetail);
    }

    @Override
    public void i_putClassAnnotation(String i_className,
                                     String i_annotationClassName, String annotationDetail) {
        Map<String, String> i_classAnnotations = i_classDetails.get(i_className);
        if ( i_classAnnotations == null ) {
            i_classAnnotations = new IdentityHashMap<String, String>();
            i_classDetails.put(i_className, i_classAnnotations);
        }

        i_classAnnotations.put(i_annotationClassName, annotationDetail);
    }

    @Override
    public void i_putFieldAnnotation(String i_className, String i_fieldName,
                                     String i_annotationClassName, String annotationDetail) {

        Map<String, Map<String, String>> i_fieldAnnotationSuite = i_fieldDetails.get(i_className);
        if ( i_fieldAnnotationSuite == null ) {
            i_fieldAnnotationSuite = new IdentityHashMap<String, Map<String, String>>();
            i_fieldDetails.put(i_className, i_fieldAnnotationSuite);
        }

        Map<String, String> i_fieldAnnotations = i_fieldAnnotationSuite.get(i_fieldName);
        if ( i_fieldAnnotations == null ) {
            i_fieldAnnotations = new IdentityHashMap<String, String>();
            i_fieldAnnotationSuite.put(i_fieldName, i_fieldAnnotations);
        }

        i_fieldAnnotations.put(i_annotationClassName, annotationDetail);
    }

    @Override
    public void i_putMethodAnnotation(String i_className, String i_methodSignature,
                                      String i_annotationClassName, String annotationDetail) {
        Map<String, Map<String, String>> i_methodAnnotationSuite = i_methodDetails.get(i_className);
        if ( i_methodAnnotationSuite == null ) {
            i_methodAnnotationSuite = new IdentityHashMap<String, Map<String, String>>();
            i_methodDetails.put(i_className, i_methodAnnotationSuite);
        }

        Map<String, String> i_methodAnnotations = i_methodAnnotationSuite.get(i_methodSignature);
        if ( i_methodAnnotations == null ) {
            i_methodAnnotations = new IdentityHashMap<String, String>();
            i_methodAnnotationSuite.put(i_methodSignature, i_methodAnnotations);
        }

        i_methodAnnotations.put(i_annotationClassName, annotationDetail);
    }

    /**
     * <p>Add data from a table into this table.  Restrict additions to
     * named packages and classes.</p>
     *
     * @param otherTable The table which is to be added to this table.
     * @param i_addedPackageNames The names of packages for which to added data.
     * @param i_addedClassNames The names of classes for which to added data.
     */
    public void restrictedAdd(TargetsTableDetailsImpl otherTable,
                              Set<String> i_addedPackageNames,
                              Set<String> i_addedClassNames) {
        i_addDetails( i_getPackageDetails(), otherTable.i_getPackageDetails(), i_addedPackageNames );
        i_addDetails( i_getClassDetails(), otherTable.i_getClassDetails(), i_addedClassNames );

        i_addDetailsSuite( i_getFieldDetails(), otherTable.i_getFieldDetails(), i_addedClassNames );
        i_addDetailsSuite( i_getMethodDetails(), otherTable.i_getMethodDetails(), i_addedClassNames );
    }

    protected static final Set<String> ADD_ALL = null;

    protected void i_addDetails(Map<String, Map<String, String>> i_otherDetails,
                                Map<String, Map<String, String>> i_theseDetails,
                                Set<String> i_addedNames) {

        // Package, class, field, or method name -->
        //   Annotation class name ->
        //     Annotation detail

        // Ensure a bucket for the package, class, field, or method,
        // then add the other details to that bucket.

        for ( Map.Entry<String, Map<String, String>> i_otherDetailEntry : i_otherDetails.entrySet() ) {
            String i_name = i_otherDetailEntry.getKey();
            if ( (i_addedNames != null) && !i_addedNames.contains(i_name) ) {
                continue;
            }

            Map<String, String> i_otherAnnotations = i_otherDetailEntry.getValue();

            Map<String, String> i_theseAnnotations = i_theseDetails.get(i_name);
            if ( i_theseAnnotations == null ) {
                i_theseAnnotations = new IdentityHashMap<String, String>();
                i_theseDetails.put(i_name, i_theseAnnotations);
            }

            i_theseAnnotations.putAll(i_otherAnnotations);
        }
    }

    protected void i_addDetailsSuite(Map<String, Map<String, Map<String, String>>> i_otherDetailSuite,
                                     Map<String, Map<String, Map<String, String>>> i_thisDetailSuite,
                                     Set<String> i_addedClassNames) {

        // Details suite for fields and methods, which add a level of mapping relative to
        // package and class annotations.

        // Package and class annotations map:
        //
        // Package or class name -->
        //   Annotation class name -->
        //     Annotation detail

        // For fields and methods, mapping structure is instead:
        //
        // Class name -->
        //   Field or method name -->
        //     Annotation class name -->
        //       Annotation detail

        for ( Map.Entry<String, Map<String, Map<String, String>>> i_otherSuiteEntry : i_otherDetailSuite.entrySet() ) {
            String i_className = i_otherSuiteEntry.getKey();
            if ( !i_addedClassNames.contains(i_className) ) {
                continue;
            }

            Map<String, Map<String, String>> i_otherDetails = i_otherSuiteEntry.getValue();

            Map<String, Map<String, String>> i_theseDetails = i_thisDetailSuite.get(i_className);

            if ( i_theseDetails == null ) {
                i_theseDetails = new IdentityHashMap<String, Map<String, String>>();
                i_thisDetailSuite.put(i_className, i_theseDetails);
            }

            i_addDetails(i_otherDetails, i_theseDetails, ADD_ALL);
        }
    }

    //

    /**
     * <p>Tell if two detail tables have equivalent contents.</p>
     *
     * <p>As a special case, handle any absent table as being equivalent to
     * an empty table.</p>
     *
     * <p>All key values are interned strings.  All tables use identity semantics
     * for key matching.  All leaf values are non-interned strings.  (Annotation detail
     * text is not interned.)</p>
     *
     * @param otherTable The table to compare to this table.  Null is handled.
     *
     * @return True if the tables are the same.  Otherwise, false.
     */
    public boolean sameAs(TargetsTableDetailsImpl otherTable) {
        if ( otherTable == this ) {
            return true;
        }

        if ( !equalsSuite(i_getPackageDetails(), ((otherTable == null) ? null : otherTable.i_getPackageDetails()) ) ) {
            return false;
        }

        if ( !equalsSuite(i_getClassDetails(), ((otherTable == null) ? null : otherTable.i_getClassDetails()) ) ) {
            return false;
        }

        if ( !equalsClassSuite(i_getFieldDetails(), ((otherTable == null) ? null : otherTable.i_getFieldDetails()) ) ) {
            return false;
        }

        if ( !equalsClassSuite(i_getMethodDetails(), ((otherTable == null) ? null : otherTable.i_getMethodDetails()) ) ) {
            return false;
        }

        return true;
    }

    /**
     * <p>Tell if two class detail suites have equivalent contents.</p>
     *
     * <p>As a special case, handle any absent table as being equivalent to
     * an empty table.</p>
     *
     * <p>All key values are interned strings.  All tables use identity semantics
     * for key matching.  All leaf values are non-interned strings.  (Annotation detail
     * text is not interned.)</p>
     *
     * @param finalClassSuite The final class suite which is to be compared.
     * @param initialClassSuite The initial class suite which is to be compared.
     *
     * @return True if the class suites are the same.  Otherwise, false.
     */
    protected boolean equalsClassSuite(Map<String, Map<String, Map<String, String>>> finalClassSuite,
                                       Map<String, Map<String, Map<String, String>>> initialClassSuite) {
        if ( finalClassSuite == null ) {
            return isEmptyClassSuite(initialClassSuite);

        } else if ( initialClassSuite == null ) {
            return isEmptyClassSuite(finalClassSuite);

        } else {
            for ( Map.Entry<String, Map<String, Map<String, String>>> classSuiteEntry : finalClassSuite.entrySet() ) {
                String finalClassName = classSuiteEntry.getKey();
                Map<String, Map<String, String>> finalDetailsSuite = classSuiteEntry.getValue();

                Map<String, Map<String, String>> initialDetailsSuite = initialClassSuite.get(finalClassName);

                if ( !equalsSuite(finalDetailsSuite, initialDetailsSuite) ) {
                    return false;
                }
            }

            for ( Map.Entry<String, Map<String, Map<String, String>>> initialSuiteEntry : initialClassSuite.entrySet() ) {
                String initialClassName = initialSuiteEntry.getKey();

                // If the key is present, the elements were tested by the first loop.
                if ( finalClassSuite.containsKey(initialClassName) ) {
                    continue;
                } else if ( !isEmptySuite(initialSuiteEntry.getValue() ) ) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * <p>Tell if a class details suite is empty.</p>
     *
     * <p>Handle an absent value as an empty table.</p>
     *
     * @param classSuite The class details suite which is to be tested.
     *
     * @return True if the suite is empty.  Otherwise, false.
     */
    protected boolean isEmptyClassSuite(Map<String, Map<String, Map<String, String>>> classSuite) {
        if ( classSuite == null ) {
            // Absent from the parent mapping: Treat as empty.
            return true;

        } else if ( classSuite.isEmpty() ) {
            return true;

        } else {
            // Considered empty if all of the child mappings is empty.

            for ( Map<String, Map<String, String>> suite : classSuite.values() ) {
                // Must use 'isEmptySuite', since there might be empty
                // mappings under individual suites.
                if ( !isEmptySuite(suite) ) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * <p>Tell if two detail suites are equal.</p>
     *
     * <p>Absent tables are handled as empty tables.</p>
     *
     * <p>All keys are interned strings.  Leaf values are not interned strings.</p>
     *
     * @param finalSuite The final detail suite which is to be compared.
     * @param initialSuite The initial detail suite which is to be compared.
     *
     * @return True if the suites are equal.  Otherwise, false.
     */
    protected boolean equalsSuite(Map<String, Map<String, String>> finalSuite,
                                  Map<String, Map<String, String>> initialSuite) {

        if ( finalSuite == null ) {
            // Handle a null table as an empty table.
            return isEmptySuite(initialSuite);

        } else if ( initialSuite == null ) {
            // Handle a null table as an empty table.
            return ( isEmptySuite(finalSuite) );

        } else {
            // The sizes cannot be used for a quick check because absent tables are tolerated
            // and handled as empty tables.

            // The keys are either package or class names, or field or method names.

            for ( Map.Entry<String, Map<String, String>> finalDetailsEntry : finalSuite.entrySet() ) {
                String finalKey = finalDetailsEntry.getKey();
                Map<String, String> finalValue = finalDetailsEntry.getValue();

                Map<String, String> initialValue = initialSuite.get(finalKey);

                if ( !equalsMap(finalValue, initialValue) ) {
                    return false;
                }
            }

            // Still can't check the table sizes: An element of the final table might have been
            // empty and matched against a value absent from the initial table, or, the initial
            // table might have an empty value matched against a value absent from the final table.

            for ( Map.Entry<String, Map<String, String>> initialDetailsEntry : initialSuite.entrySet() ) {
                String initialKey = initialDetailsEntry.getKey();

                if ( finalSuite.containsKey(initialKey) ) {
                    // Already checked by the first loop; no need to check again.
                    continue;

                } else if ( !initialDetailsEntry.getValue().isEmpty() ) {
                    // Absent in the final suite; only matches if the initial value is empty.
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * <p>Tell is a detail suite is empty.  That is, if it is null,
     * or an empty map, or contains only empty maps.</p>
     *
     * @param suite The detail suite which is to be tested.
     *
     * @return True if the suite is empty.  Otherwise, false.
     */
    protected boolean isEmptySuite(Map<String, Map<String, String>> suite) {
        if ( suite == null ) {
            // A null suite is treated as being empty.
            return true;

        } else if ( suite.isEmpty() ) {
            return true;

        } else {
            // A non-empty suite with all empty values is treated
            // as being empty.
            for ( Map<String, String> detailValue : suite.values() ) {
                if ( !detailValue.isEmpty() ) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * <p>Compare two mappings.  Keys are interned strings; values are not interned.</p>
     *
     * <p>Null values are not tolerated.</p>
     *
     * @param finalMap The final map which is to be compared.
     * @param initialMap The initial map which is to be compared.
     *
     * @return True if the maps are equal.  Otherwise, false.
     */
    protected boolean equalsMap(Map<String, String> finalMap, Map<String, String> initialMap) {
        if ( finalMap == null ) {
            // A null mapping is handled as being equal to an empty map.
            return ( (initialMap == null) || initialMap.isEmpty() );

        } else if ( initialMap == null ) {
            // A null mapping is handled as being equal to an empty map.
            // return ( (finalMap == null) || finalMap.isEmpty() ); // Can't be null, per prior test.
            return ( finalMap.isEmpty() );

        } else {
            // The maps cannot be equal if they don't have the same size.
            if ( finalMap.size() != initialMap.size() ) {
                return false;
            }

            // The tables can be different because either they have
            // dissimilar key sets, or if a key maps to a different values.

            for ( Map.Entry<String, String> finalEntry : finalMap.entrySet() ) {
                String finalKey = finalEntry.getKey();
                String finalValue = finalEntry.getValue(); // Won't be null.

                String initialValue = initialMap.get(finalKey);
                if ( initialValue == null ) {
                    return false; // The key is absent.  The key sets are dissimilar.
                }

                if ( !finalValue.equals(initialValue) ) {
                    return false; // The key is present, but maps to a different value.
                }

                // The key is present and maps to the same value ...
                // Keep checking.
            }

            // All of the elements of the final table match elements in the initial table.
            // ... AND ...
            // There are no extra elements in the initial table.
            // ... THEREFORE ...
            // The tables must be equal.
            return true;
        }
    }

    //

    @Override
    public List<TargetCache_ParseError> readUsing(TargetCache_Reader reader) throws IOException {
        return reader.read(this);
    }
}

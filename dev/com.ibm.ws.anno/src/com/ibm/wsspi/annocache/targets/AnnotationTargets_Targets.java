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
package com.ibm.wsspi.annocache.targets;

import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;

/**
 * <p>Annotation targets tables. Results include package source information,
 * class source information, class relationship information and annotation targets information.</p>
 *
 * <p>Package source information consists of a listing of packages partitioned by the class
 * source which provided each package.</p>
 *
 * <p>Class source information consists of a listing of classes partitioned by the class source
 * which provided each class.</p>
 *
 * <p>Class relationship information consists of class to interface relationships and class to
 * superclass relationships.</p>
 *
 * <p>Annotation information consists of the recording of package, class, field, and method
 * annotation occurrences. Results are stored as mappings of package and class names to
 * annotation names. Detail values for the annotation occurrences are not recorded. The
 * particular target field or methods of the occurrences are not recorded.</p>
 *
 * <p>Results are partitioned by scan policy (see {@link ClassSource_Aggregate.ScanPolicy}),
 * respectively, SEED, PARTIAL, EXCLUDED, and EXTERNAL. For non-web module cases, only SEED and
 * EXTERNAL apply. Web module cases use PARTIAL and EXCLUDED for particular cases involving
 * metadata-complete fragment jars and for particular cases involving web module jars which
 * were excluded by their omittion from an absolute ordering specified for the web module.</p>
 *
 * <p>Classes are recorded for SEED, PARTIAL, and EXCLUDED regions. Classes are recorded for
 * EXTERNAL regions only as needed to complete class relationship information.</p>
 *
 * <p>Annotations occurrences are recorded for SEED, PARTIAL, and EXCLUDED regions. Annotation
 * occurrences are omitted from EXTERNAL regions.</p>
 *
 * <p>Annotation selection queries default to select SEED results, unless an explicit selection
 * policy is specified. When specified, the selection policy is the bitwise OR of scan policy
 * values. Results are selected from all regions which are included in specified policy.</p>
 *
 * <p>Helpers are available for selecting classes which are the target of inherited class
 * annotations. Particular care must be used when selecting such classes. The helpers may
 * be used for non-inheritable class annotations, but will obtain meaningless results. Using the
 * helpers, usually, the regions used to select the classes declaring annotations is the same
 * as the region used to select the inheriting classes. However, the helpers allow these
 * regions to be different.</p>
 */
public interface AnnotationTargets_Targets extends com.ibm.wsspi.anno.targets.AnnotationTargets_Targets {
    // Logging ...

    void log(Logger logger);

    void logState();

    // Factory ...

    /**
     * <p>Answer the factory which was used to create this targets table.
     * Objects created from this targets table use this factory.</p>
     *
     * @return The factory used to create this targets table.
     */
    AnnotationTargets_Factory getFactory();

    // Cache ...
    
    /**
     * Control value: When true, module level data is not cached.
     * This value does not affect caching of container level data.
     *
     * This setting has no effect when the application or module of the
     * targets is unnamed.
     *
     * @return True or false telling if module level data is not to be
     *     cached.
     */
	boolean getIsLightweight();

    // Scanning ...

    /**
     * <p>Perform a scan of a class source.</p>
     *
     * @param rootClassSource The class source which is to be scanned.
     */
    void scan(ClassSource_Aggregate rootClassSource);

    /**
     * <p>Perform a limited scan of a class source.</p>
     *
     * @param rootClassSource The class source which is to be scanned.
     */
    void scanLimited(ClassSource_Aggregate rootClassSource);

    /**
     * <p>Perform a scan of the specified class source, injecting scan data
     * into the annotation targets. Scan only the specified classes. Record only
     * the specified annotations. Do not complete the class reference information.</p>
     *
     * @param rootClassSource The class source which is to be scanned.
     * @param specificClassNames The names of the specific classes which are to be scanned.
     * @param specificAnnotationClassNames The names of the specific annotations classes which are of interest.
     *
     * @throws AnnotationTargets_Exception Thrown if an error occurred during scanning.
     */
    void scan(ClassSource_Aggregate rootClassSource,
        Set<String> specificClassNames,
        Set<String> specificAnnotationClassNames) throws AnnotationTargets_Exception;

    /**
     * <p>Perform a scan of the specified class source, injecting scan data
     * into the annotation targets. Scan only the specified classes. Do not complete
     * the class reference information.</p>
     *
     * @param rootClassSource The class source which is to be scanned.
     * @param specificClassNames The names of the specific classes which are to be scanned.
     * @param specificClassNames The names of the specific annotations classes which are of interest.
     *
     * @throws AnnotationTargets_Exception Thrown if an error occurred during scanning.
     */
    void scan(ClassSource_Aggregate rootClassSource, Set<String> specificClassNames)
        throws AnnotationTargets_Exception;

    // Scanned classes ...

    /**
     * Tell if the targets are for scan of specific classes.
     *
     * @return True or false telling if this scan is of specific classes.
     */
    boolean isSpecific();

    /**
     * Answer the specific classes which were scanned to generate the target
     * information.  Answer null unless the scan was for specific classes.
     *
     * @return The specific classes of this scan.
     */
    Set<String> getSpecificClassNames();

    /**
     * <p>Tell if a particular named class is a seed class name.</p>
     *
     * @param className The class name to test.
     *
     * @return True if the named class was scanned as a seed class. Otherwise, false.
     *
     * @see #getSeedClassNames()
     */
    boolean isSeedClassName(String className);

    /**
     * <p>Answer the seed class names.</p>
     *
     * @return The seed class names.
     */
    Set<String> getSeedClassNames();

    /**
     * <p>Tell if a particular named class is a partial class.</p>
     *
     * @param className The class name to test.
     *
     * @return True if the named class is a partial class. Otherwise, false.
     *
     * @see #getPartialClassNames()
     */
    boolean isPartialClassName(String className);

    /**
     * <p>Answer the partial class names.</p>
     *
     * @return The partial class names.
     */
    Set<String> getPartialClassNames();

    /**
     * <p>Tell if a particular named class is an excluded class.</p>
     *
     * @param className The class name to test.
     *
     * @return True if the named class is an excluded class. Otherwise, false.
     *
     * @see #getExcludedClassNames()
     */
    boolean isExcludedClassName(String className);

    /**
     * <p>Answer the excluded class names.</p>
     *
     * @return The seed class names.
     */
    Set<String> getExcludedClassNames();

    /**
     * <p>Tell if a particular named class is an external class.</p>
     *
     * @param className The class name to test.
     *
     * @return True if the named class is an external class. Otherwise, false.
     *
     * @see #getExternalClassNames()
     */
    boolean isExternalClassName(String className);

    /**
     * <p>Answer the external class names.</p>
     *
     * @return The external class names.
     */
    Set<String> getExternalClassNames();

    // Class relationships (interfaces and superclasses) results ...

    /**
     * <p>Tell if a candidate class is an instance of a specified class.</p>
     *
     * @param candidateClassName The name of the class to test.
     * @param criterionClass The class to test against.
     *
     * @return True if the candidate class is an instance of the criterion class.
     *         Otherwise, false.
     */
    boolean isInstanceOf(String candidateClassName, Class<?> criterionClass);

    /**
     * <p>Tell if a candidate class is an instance of a specified class.</p>
     *
     * @param candidateClassName The name of the class to test.
     * @param criterionClassName The name of the class to test against.
     * @param isInterface Control parameter: Ist the criterion class an interface.
     *
     * @return True if the candidate class is an instance of the criterion class.
     *         Otherwise, false.
     */
    boolean isInstanceOf(String candidateClassName, String criterionClassName, boolean isInterface);
    
    /**
     * <p>Answer the name of the super class of a target class.
     * Answer null if the target class is java.lang.Object, or is
     * an interface.</p>
     *
     * @param className The name of the target class.
     *
     * @return The name of the superclass of the target class.
     */
    String getSuperclassName(String className);

    /**
     * <p>Answer the name of all of the subclasses of a target class.</p>
     *
     * @param className The class for which to obtain subclass names.
     *
     * @return The names of all subclasses of the target class.
     */
    Set<String> getSubclassNames(String className);

    /**
     * <p>Answer the names of the interfaces of a target class.</p>
     *
     * <p>The names are answered in the order in which they were
     * declared.</p>
     *
     * @param className The class for which to obtain interface names.
     *
     * @return The interface names of the target class.
     */
    String[] getInterfaceNames(String className);

    /**
     * <p>Answer the names of all the implementors of a target interface.</p>
     *
     * @param interfaceName The name of the target interface.
     *
     * @return The names of all implementers of the target interface.
     */
    Set<String> getAllImplementorsOf(String interfaceName);

    // Package results ...

    /**
     * <p>Answer the names of packages having annotations. Limit results
     * to SEED packages.</p>
     *
     * @return The names of the packages having annotations.
     */
    Set<String> getAnnotatedPackages();

    /**
     * <p>Answer the names of packages having the specified annotation. Limit results
     * to SEED packages.</p>
     *
     * @param annotationName The name of the annotation on which to select.
     *
     * @return The names of the packages having the specified annotation.
     */
    Set<String> getAnnotatedPackages(String annotationName);

    /**
     * <p>Answer all recorded SEED package annotations.</p>
     *
     * @return The names of all recorded SEED package annotations.
     */
    Set<String> getPackageAnnotations();

    /**
     * <p>Answer the annotations of a specified package. Select from SEED results.</p>
     *
     * @param packageName The name of the package for which to select annotations.
     *
     * @return The names of annotations of the named package, selected from SEED results.
     */
    Set<String> getPackageAnnotations(String packageName);

    // Class results ...

    /**
     * <p>Answer the names of classes having class annotations. Limit results
     * to SEED classes.</p>
     *
     * @return The names of the classes having class annotations.
     */
    Set<String> getAnnotatedClasses();

    /**
     * <p>Answer the names of classes having the specified annotation. Limit results
     * to SEED classes.</p>
     *
     * @param annotationName The name of the class annotation on which to select.
     *
     * @return The names of the classes having the specified class annotation.
     */
    Set<String> getAnnotatedClasses(String annotationName);

    /**
     * <p>Answer all recorded SEED class annotations.</p>
     *
     * @return The names of all recorded SEED class annotations.
     */
    Set<String> getClassAnnotations();

    /**
     * <p>Answer the class annotations of a specified class. Select from SEED results.</p>
     *
     * @param className The name of the class for which to select class annotations.
     *
     * @return The names of class annotations of the named class, selected from SEED results.
     */
    Set<String> getClassAnnotations(String className);

    // Field results ...

    /**
     * <p>Answer the names of the classes having any declared occurrence of
     * the specified field annotation. Select from within SEED results.</p>
     *
     * @param annotationName The name of the field annotation on which to select.
     *
     * @return The names of the classes having the specified field annotation.
     */
    Set<String> getClassesWithFieldAnnotation(String annotationName);

    /**
     * <p>Answer the names of recorded field annotations. Select from within SEED results.</p>
     *
     * @return The names of field annotations of the named class, selected from SEED results.
     */
    Set<String> getFieldAnnotations();

    /**
     * <p>Answer the names of the field annotations recorded on the specified class.
     * Select from within SEED results.</p>
     *
     * @param className The name of the class for which to select field annotations.
     *
     * @return The names of field annotations of the named class, selected from SEED results.
     */
    Set<String> getFieldAnnotations(String className);

    // Method results ...

    /**
     * <p>Answer the names of the classes having any declared occurrence of
     * the specified method annotation. Select from within SEED results.</p>
     *
     * @param annotationName The name of the method annotation on which to select.
     *
     * @return The names of the classes having the specified method annotation.
     */
    Set<String> getClassesWithMethodAnnotation(String annotationName);

    /**
     * <p>Answer the names of the recorded annotations. Select from within SEED results.</p>
     *
     * @return The names of method annotations, selected from SEED results.
     */
    Set<String> getMethodAnnotations();

    /**
     * <p>Answer the names of the method annotations recorded on the specified class.
     * Select from within SEED results.</p>
     *
     * @param className The name of the class for which to select method annotations.
     *
     * @return The names of method annotations of the named class, selected from SEED results.
     */
    Set<String> getMethodAnnotations(String className);

    // Class source bridge ...

    /**
     * <p>Answer the names of the classes having the specified class annotation.
     * Limit the results to classes within the specified class source and to SEED results.</p>
     *
     * @param classSourceName The name of the class source used to restrict the results.
     * @param annotationName The name of the class annotation on which to select.
     *
     * @return The names of the classes having the specified class annotation.
     */
    Set<String> getAnnotatedClasses(String classSourceName, String annotationName);

    /**
     * <p>Answer the names of the classes having the specified class annotation.
     * Limit the results to classes within the specified class source and to the
     * specified results.</p>
     *
     * @param classSourceName The name of the class source used to restrict the results.
     * @param annotationName The name of the class annotation on which to select.
     * @param scanPolicies The policies for which to select annotated classes, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of the classes having the specified class annotation.
     */
    Set<String> getAnnotatedClasses(String classSourceName, String annotationName, int scanPolicies);

    // Policy driven result selection ...

    /** <p>Synonym for {@link ClassSource_Aggregate.ScanPolicy#ALL_EXCEPT_EXTERNAL}.</p> */
    public static final int POLICY_ALL_EXCEPT_EXTERNAL =
        ClassSource_Aggregate.ScanPolicy.ALL_EXCEPT_EXTERNAL;

    /**
     * <p>Synonym for {@link ClassSource_Aggregate.ScanPolicy#SEED} OR'ed with
     * {@link ClassSource_Aggregate.ScanPolicy#PARTIAL}.</p>
     */
    public static final int POLICY_SEED_AND_PARTIAL =
        ClassSource_Aggregate.ScanPolicy.SEED.getValue() |
        ClassSource_Aggregate.ScanPolicy.PARTIAL.getValue();

    /** <p>Synonym for the value of {@link ClassSource_Aggregate.ScanPolicy#SEED}.</p> */
    public static final int POLICY_SEED =
        ClassSource_Aggregate.ScanPolicy.SEED.getValue();

    /** <p>Synonym for the value of {@link ClassSource_Aggregate.ScanPolicy#PARTIAL}.</p> */
    public static final int POLICY_PARTIAL =
        ClassSource_Aggregate.ScanPolicy.PARTIAL.getValue();

    /** <p>Synonym for the value of {@link ClassSource_Aggregate.ScanPolicy#EXCLUDED}.</p> */
    public static final int POLICY_EXCLUDED =
        ClassSource_Aggregate.ScanPolicy.EXCLUDED.getValue();

    /** <p>Synonym for the value of {@link ClassSource_Aggregate.ScanPolicy#EXTERNAL}.</p> */
    public static final int POLICY_EXTERNAL =
        ClassSource_Aggregate.ScanPolicy.EXTERNAL.getValue();

    /**
     * <p>Enumeration used for the several categories of annotations.</p>
     */
    public enum AnnotationCategory {
        PACKAGE, CLASS, METHOD, FIELD;
    }

    /**
     * <p>Answer the class names in the specified regions.</p>
     *
     * @param scanPolicies The scan policies for which to select class names.
     *
     * @return The names of all classes in the specified regions.
     */
    Set<String> getClassNames(int scanPolicies);

    //

    /**
     * <p>Answer the names of packages having annotations. Limit results
     * to the specified regions.</p>
     *
     * @param scanPolicies The policies for which to select annotated packages, as bitwise
     *            OR of scan policy values.
     * @return The names of the packages having annotations.
     */
    Set<String> getAnnotatedPackages(int scanPolicies);

    /**
     * <p>Answer the names of packages having the specified annotation. Limit results
     * to the specified regions.</p>
     *
     * @param annotationName The name of the package annotation on which to select.
     * @param scanPolicies The policies for which to select annotated packages, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of the packages having the specified annotation.
     */
    Set<String> getAnnotatedPackages(String annotationName, int scanPolicies);

    /**
     * <p>Answer the names of recorded package annotations. Restrict results
     * to only those recorded in the specified regions.</p>
     *
     * @param scanPolicies The policies for which to select package annotations, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of recorded package annotations selected from the specified results.
     */
    Set<String> getPackageAnnotations(int scanPolicies);

    /**
     * <p>Answer the names of annotations on the specified package. Restrict results
     * to only those recorded in the specified regions.</p>
     *
     * @param packageName The name of the package for which to select annotations.
     * @param scanPolicies The policies for which to select package annotations, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of annotations of the named package, selected from the
     *         specified results.
     */
    Set<String> getPackageAnnotations(String packageName, int scanPolicies);

    /**
     * <p>Answer the names of classes having class annotations. Limit results
     * to the specified regions.</p>
     *
     * @param scanPolicies The policies for which to select annotated classes, as bitwise
     *            OR of scan policy values.
     * @return The names of the classes having class annotations.
     */
    Set<String> getAnnotatedClasses(int scanPolicies);

    /**
     * <p>Answer the names of classes having the specified annotation. Limit results
     * to the specified regions.</p>
     *
     * @param annotationName The name of the class annotation on which to select.
     * @param scanPolicies The policies for which to select annotated classes, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of the classes having the specified class annotation.
     */
    Set<String> getAnnotatedClasses(String annotationName, int scanPolicies);

    /**
     * <p>Answer the names of recorded class annotations. Restrict results
     * to only those recorded in the specified regions.</p>
     *
     * @param scanPolicies The policies for which to select class annotations, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of recorded class annotations selected from the specified results.
     */
    Set<String> getClassAnnotations(int scanPolicies);

    /**
     * <p>Answer the names of annotations on the specified class. Restrict results
     * to only those recorded in the specified regions.</p>
     *
     * @param className The name of the class for which to select class annotations.
     * @param scanPolicies The policies for which to select class annotations, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of class annotations of the named class, selected from the
     *         specified results.
     */
    Set<String> getClassAnnotations(String className, int scanPolicies);

    //

    /**
     * <p>Answer the names of the classes having any declared occurrence of
     * a field annotation. Select from within the specified results.</p>
     *
     * @param scanPolicies The policies for which to select class annotations, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of the classes having a field annotation.
     */
    Set<String> getClassesWithFieldAnnotations(int scanPolicies);

    /**
     * <p>Answer the names of the classes having any declared occurrence of
     * the specified field annotation. Select from within the specified results.</p>
     *
     * @param annotationName The name of the field annotation on which to select.
     * @param scanPolicies The policies for which to select class annotations, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of the classes having the specified field annotation.
     */
    Set<String> getClassesWithFieldAnnotation(String annotationName, int scanPolicies);

    /**
     * <p>Answer the names of recorded field annotations. Select from within the
     * specified results.</p>
     *
     * @param scanPolicies The policies for which to select class annotations, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of field annotations of the named class, selected from the
     *         specified results.
     */
    Set<String> getFieldAnnotations(int scanPolicies);

    /**
     * <p>Answer the names of the field annotations recorded on the specified class.
     * Select from within the specified results.</p>
     *
     * @param className The name of the class for which to select field annotations.
     *
     * @param scanPolicies The policies for which to select class annotations, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of field annotations of the named class, selected from the
     *         specified results.
     */
    Set<String> getFieldAnnotations(String className, int scanPolicies);

    //

    /**
     * <p>Answer the names of the classes having any method annotations. Select
     * Select from within the specified results.</p>
     *
     * @param scanPolicies The policies for which to select class annotations, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of the classes having the specified method annotation.
     */
    Set<String> getClassesWithMethodAnnotations(int scanPolicies);

    /**
     * <p>Answer the names of the classes having any declared occurrence of
     * the specified method annotation. Select from within the specified results.</p>
     *
     * @param annotationName The name of the method annotation on which to select.
     * @param scanPolicies The policies for which to select class annotations, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of the classes having the specified method annotation.
     */
    Set<String> getClassesWithMethodAnnotation(String annotationName, int scanPolicies);

    /**
     * <p>Answer the names of the recorded method annotations. Select from
     * within the specified results.</p>
     *
     * @param scanPolicies The policies for which to select class annotations, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of method annotations of the named class, selected from the
     *         specified results.
     */
    Set<String> getMethodAnnotations(int scanPolicies);

    /**
     * <p>Answer the names of the method annotations recorded on the specified class.
     * Select from within the specified results.</p>
     *
     * @param className The name of the class for which to select method annotations.
     * @param scanPolicies The policies for which to select class annotations, as bitwise
     *            OR of scan policy values.
     *
     * @return The names of method annotations of the named class, selected from the
     *         specified results.
     */
    Set<String> getMethodAnnotations(String className, int scanPolicies);

    // Class and class inheritance bridge ...

    /**
     * <p>Utility for inherited class annotations: Find classes which
     * have the specified class annotation, or which are a subclass of
     * a class which has the specified class annotation.</p>
     *
     * <p>Restrict the result to annotations specified in SEED classes
     * and inherited by SEED classes. (Inheritance information may
     * traverse non SEED classes.)</p>
     *
     * <p>Do NOT use this method if the specified annotation type
     * is not an inherited class annotation, as it is inefficient for
     * that purpose. Use {@link #getAnnotatedClasses(String)} instead.</p>
     *
     * <p>The operation does not require that the specified annotation
     * type be an inherited class annotation. The result, however, is
     * meaningless if the class annotation is not actually inherited.</p>
     *
     * <p>No similar capability is provided for method annotations. Method
     * annotations are not inheritable. For the related handling of
     * annotations on inherited methods, more comprehensive processing
     * must be performed, as method overloading must be taken into account.
     * Use java reflection or the class info store to process annotations
     * on inherited methods.</p>
     *
     * @param annotationName The class annotation for which to obtain annotations.
     *
     * @return All targets of a specified class annotation,
     *         including both declared and inheriting targets
     */
    Set<String> getAllInheritedAnnotatedClasses(String annotationName);

    /**
     * <p>Utility for inherited class annotations. Similar to {@link #getAllInheritedAnnotatedClasses(String)},
     * except that the results are restricted to annotations declared in the specified
     * results and inherited by classes in the specified results.</p>
     *
     * @param annotationName The class annotation for which to obtain annotations.
     * @param scanPolicies The policies for which to select classes, as bitwise
     *            OR of scan policy values. Applies to both declaring and inheriting
     *            classes considered for the results.
     *
     * @return All targets of a specified class annotation, including both declared
     *         and inheriting targets, but limited by the selection policy.
     */
    Set<String> getAllInheritedAnnotatedClasses(String annotationName, int scanPolicies);

    /**
     * <p>Utility for inherited class annotations. Similar to {@link #getAllInheritedAnnotatedClasses(String)},
     * except that the results are restricted to annotations declared the specified
     * results and inherited by classes in distinct specified results.</p>
     *
     * @param annotationName The class annotation for which to obtain annotations.
     * @param declarerScanPolicies The policies for which to select classes, as bitwise
     *            OR of scan policy values. Used to select classes which declare an occurrence
     *            of the specified annotation.
     * @param inheritorScanPolicies The policies for which to select classes, as bitwise
     *            OR of scan policy values. Used to select classes which inherit from the
     *            selected annotated classes.
     *
     * @return All targets of a specified class annotation, including both declared
     *         and inheriting targets, but limited by selection policies.
     */
    Set<String> getAllInheritedAnnotatedClasses(String annotationName,
                                                int declarerScanPolicies,
                                                int inheritorScanPolicies);

    /**
     * Answer the modifiers of a specified class.
     * 
     * Answer null if no data is recorded for the class, or if it's modifiers are zero.
     * 
     * @param i_className The interned class name for which to retrieve modifiers.
     * 
     * @return The modifiers of the specified class.
     */
    Integer i_getModifiers(String i_className);

    /**
     * Answer the modifiers value for a specified class.
     * 
     * Answer zero (0) if no data is recorded for the class, or if it's modifiers are zero.
     * 
     * @param i_className The interned class name for which to retrieve modifiers.
     * 
     * @return The modifiers value for the specified class.
     */
    int i_getModifiersValue(String i_className);
    
    /**
     * Answer the modifiers value for a specified class.
     * 
     * Answer null if no data is recorded for the class, or if it's modifiers are zero.
     * 
     * @param className The class name for which to retrieve modifiers.
     * 
     * @return The modifiers value for the specified class.
     */
    Integer getModifiers(String className);

    /**
     * Answer the modifiers value for a specified class.
     *
     * Answer zero (0) if no data is recorded for the class, or if it's modifiers are zero.
     *
     * @param className The class name for which to retrieve modifiers.
     *
     * @return The modifiers value for the specified class.
     */
    int getModifiersValue(String className);

    /**
     * Answer the modifiers value for a specified class as an enum set.
     * 
     * Answer null if no data is recorded for the class.
     * 
     * @param i_className The interned class name for which to retrieve modifiers.
     * 
     * @return The modifiers of the specified class as an enum set.
     */
    EnumSet<AnnotationTargets_OpCodes> i_getModifiersSet(String i_className);

    /**
     * Answer the modifiers value for a specified class as an enum set.
     * 
     * Answer null if no data is recorded for the class.
     * 
     * @param className The class name for which to retrieve modifiers.
     * 
     * @return The modifiers of the specified class as an enum set.
     */
    EnumSet<AnnotationTargets_OpCodes> getModifiersSet(String className);

    /**
     * Place modifiers for a specified class into an enum set.
     * 
     * Place no elements and answer null if no data is recorded for the class.
     * 
     * @param i_className The interned class name for which to retrieve modifiers.
     * @param modifiers The modifiers set which is to be populated.
     *
     * @return The initial enum set.  Null if no data is recorded for the class.
     */
    EnumSet<AnnotationTargets_OpCodes> i_getModifiersSet(String i_className, EnumSet<AnnotationTargets_OpCodes> modifiers); 

    /**
     * Place modifiers for a specified class into an enum set.
     * 
     * Place no elements and answer null if no data is recorded for the class.
     * 
     * @param className The class name for which to retrieve modifiers.
     * @param modifiers The set of modifiers which is to be filled.
     * 
     * @return The initial enum set.  Null if no data is recorded for the class.
     */
    EnumSet<AnnotationTargets_OpCodes> getModifiersSet(String className, EnumSet<AnnotationTargets_OpCodes> modifiers); 

    /**
     * Tell if the specified class is abstract.
     * 
     * Answer false if no data is recorded for the class.
     * 
     * See {@link #getModifiers(String)}.
     *
     * @param className The class name to test.
     * 
     * @return True or false telling if the class is abstract.
     */
    boolean isAbstract(String className);
    
    /**
     * Tell if the specified class is an interface.
     * 
     * Answer false if no data is recorded for the class.
     * 
     * @param className The class name to test.
     * 
     * @return True or false telling if the class is an interface.
     */
    boolean isInterface(String className);
}

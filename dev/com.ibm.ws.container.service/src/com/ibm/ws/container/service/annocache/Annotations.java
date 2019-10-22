/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annocache;

import java.util.Collection;
import java.util.Set;

import com.ibm.ws.container.service.annocache.SpecificAnnotations;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.info.ClassInfo;
import com.ibm.wsspi.annocache.info.InfoStore;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets;

/**
 * Common annotations access type.
 *
 * Access to annotations depends on several key parameters:
 * 
 * <ul><li>Application name</li>
 *     <li>Module name</li>
 *     <li>Module category name</li>
 *     <li>Root Container</li>
 * </ul>
 * 
 * The application name may be left unspecified, in which case, the
 * annotations data is not cached, and the module name and the module
 * category name are unused.
 * 
 * If the application name is specified, a module name and a module
 * category name must be specified.  The annotations data is cached
 * in a two tier storage structure which has a single folder for each
 * unique application name and a single folder for each unique combination
 * of module name and module category name.
 * 
 * Annotations data for a module is assembled from a collection of
 * containers, each of which has a scan policy -- SEED, PARTIAL, EXCLUDED --
 * plus a class loader (which has the scan policy EXTERNAL.
 * 
 * The results for each container is written to a cache location keyed
 * to the full path of the container scoped to the application.  That
 * enables sharing of container results between modules.  (Container
 * results obtained with an unspecified application name are not cached.)
 *
 * The access layer is responsible for assigning the correct path to
 * containers, including adjustments for processing WEB-INF/classes
 * within a web module, which has special processing.
 * 
 * The processing of WEB-INF/classes uses a mapped container class source,
 * with the enclosing module container as the container of the class source,
 * with the class source specified with entry prefix of "WEB-INF/classes", and
 * with the class source path assigned as the full path to the WEB-INF/classes
 * container.
 */
public interface Annotations {
    /**
     * Answer the name of the enclosing application.  Answer
     * {@link ClassSource_Factory#UNNAMED_APP} if there is no enclosing
     * application.
     *
     * @return The name of the enclosing application.
     */
    String getAppName();

    /**
     * Set the name of the enclosing application.  Set
     * {@link ClassSource_Factory#UNNAMED_APP} if there is no enclosing 
     * application.
     *
     * Annotations data is not cached when the application is un-named.
     *
     * @param appName The name of the enclosing application.
     */    
    void setAppName(String appName);

    /**
     * Answer the name of the enclosing module.  Answer
     * {@link ClassSource_Factory#UNNAMED_MOD} if there is no enclosing
     * module.
     *
     * The module name is unused when the application is unnamed.
     *
     * @return The name of the enclosing module.
     */
    String getModName();

    /**
     * Set the name of the enclosing module.  Set
     * {@link ClassSource_Factory#UNNAMED_MOD} if there is no enclosing
     * module.
     *
     * The module name is unused when the application is unnamed.
     *
     * @param modName The name of the enclosing module.
     */
    void setModName(String modName);

    /**
     * Mark these annotations do not have a named module.
     */
    void setIsUnnamedMod(boolean isUnnamedMod);

    /**
     * Tell if these annotations do not have a named module.
     */
    boolean getIsUnnamedMod();

    /**
     * Answer the category name for the results.  The category name
     * is null except for CDI container annotations.
     *
     * The module category name is unused when the application
     * is unnamed.
     *
     * @return The category name for the results.
     */
    String getModCategoryName();

    //

    /** Control parameter: Module level data of annotation targets should not be cached. */
    boolean IS_LIGHTWEIGHT = AnnotationTargets_Factory.IS_LIGHTWEIGHT;

    /** Control parameter: Module level data of annotation targets should be cached. */
    boolean IS_NOT_LIGHTWEIGHT = AnnotationTargets_Factory.IS_NOT_LIGHTWEIGHT;

    /**
     * Control value: When true, module level data of the annotations not cached.
     *
     * Defaults to false.
     *
     * @return True or false telling if module level data is not to be cached.
     */
    boolean getIsLightweight();

    /**
     * Set whether module level data of the annotations is not to be cached.
     *
     * This must be set before accessing any annotations result data.
     *
     * @param isLightweight True to disable saves of module level data.  False
     *     to enable saves of module level data.
     */
    void setIsLightweight(boolean isLightweight);

    //

    /**
     * Answer the root container of the annotations.
     *
     * @return The container of the annotations.
     */
    Container getContainer();

    /**
     * Answer the name of the container of the annotations.
     * 
     * @return The name of the container of the annotations.
     */
    String getContainerName();

    /**
     * Answer the path of the container of the annotations.
     * 
     * This will often be "/", which is when the container is
     * a root container.
     *
     * @return The path of the container of the annotations.
     */
    String getContainerPath();

    //

    /** Control parameter.  Used to enable Jandex index reads. */
    boolean USE_JANDEX = true;

    /**
     * Tell if Jandex index reads are enabled.
     *
     * @return True or false telling if Jandex index reads are enabled.
     */
    boolean getUseJandex();

    /**
     * Set if Jandex index reads are enabled.  This must be done before
     * obtaining either of the three main data structures, as the setting
     * is used to create the main data structures.
     *
     * @param useJandex True or false telling Jandex index reads are enabled.
     */
    void setUseJandex(boolean useJandex);

    //

    /**
     * Set the class loader which is used by the class source.  This must be
     * done before obtaining either of the three main data structures, as the
     * setting is used to create the main data structures.
     *
     * @param classLoader The class loader which is to be used by the class source.
     */
    void setClassLoader(ClassLoader classLoader);

    /**
     * Answer the class loader which is to be used by the class source.
     *
     * @return The class loader which is to be used by the class source.
     */
    ClassLoader getClassLoader();

    //

    /**
     * Answer the class source of the the annotations data.  Create and
     * store if necessary.
     * 
     * @return The class source of the annotations data.
     */
    ClassSource_Aggregate getClassSource();

    /**
     * Remove the class source from the in-memory cache.  Do nothing
     * if the class source was not obtained.
     *
     * @return The class source which was cleared. 
     */
    ClassSource_Aggregate releaseClassSource();

    /**
     * Answer the annotation targets of the annotations data.  Create
     * and store if necessary.
     *
     * @return The main annotation targets for the annotations data.
     */
    AnnotationTargets_Targets getTargets();

    /**
     * Synonym of {@link #getTargets()}.
     *
     * @return The main annotation targets for the annotations data.
     */
    AnnotationTargets_Targets getAnnotationTargets();

    /**
     * Remove the targets from the in-memory cache.  Do nothing
     * if the targets were not obtained.
     * 
     * @return The targets which were cleared.
     */
    AnnotationTargets_Targets releaseTargets();

    /**
     * Answer the info store of the annotations data.  Create
     * and store if necessary.
     *
     * @return The info store of the annotations data.
     */
    InfoStore getInfoStore();

    /**
     * Remove the info store from the in-memory cache.  Do nothing
     * if the info store was not obtained.
     *
     * @return The info store which was removed.
     */
    InfoStore releaseInfoStore();

    // Targets derived APIs ...

    /**
     * Tell if the target class was scanned from an included location.
     *
     * @param className The name of the target class.
     *
     * @return True if the target class was scanned from an included location. Otherwise, false.
     */
    boolean isIncludedClass(String className);

    /**
     * Tell if the target class was scanned from a partial (metadata-complete) location.
     *
     * @param className The name of the target class.
     *
     * @return True if the target class was scanned from a partial location. Otherwise, false.
     */
    boolean isPartialClass(String className);

    /**
     * Tell if the target class was scanned from an excluded location.
     * 
     * @param className The name of the target class.
     * 
     * @return True if the target class was scanned from an excluded location. Otherwise, false.
     */
    boolean isExcludedClass(String className);

    /**
     * Tell if the target class was scanned from an external location.
     *
     * @param className The name of the target class.
     *
     * @return True if the target class was scanned from an external location. Otherwise, false.
     */
    boolean isExternalClass(String className);

    //
   
    /**
     * Tell if any class is present with any of a specified collection
     * of annotations.
     * 
     * @param annotationClassNames The names of the annotation classes
     *     for which to test.
     *
     * @return True or false, telling if any classes are present
     *     with any of the annotation classes.
     */
    boolean hasSpecifiedAnnotations(Collection<String> annotationClassNames);

    /**
     * Answer the names of classes which have specified non-inherited annotations.
     * 
     * @param annotationClassNames The names of the annotation classes
     *     for which to test.
     *
     * @return The set of classes having at least one of the annotations.
     */
    Set<String> getClassesWithAnnotations(Collection<String> annotationClassNames);    

    /**
     * Answer the names of classes which have a specified non-inherited annotation.
     * 
     * @param annotationClassName The name of the annotation class for which to test.
     *
     * @return The set of classes having the annotation.
     */
    Set<String> getClassesWithAnnotation(String annotationClassName);

    /**
     * Answer the classes which have any of a specified collection of annotations.
     * Use class inheritance rules for the annotations.
     * 
     * @param annotationClassNames The names of the annotation classes for which
     *     to test.
     *
     * @return The names of classes which have any of the specified annotations. 
     */
    Set<String> getClassesWithSpecifiedInheritedAnnotations(Collection<String> annotationClassNames);

    // Info store derived APIs ...

    /**
     * Helper to open the info store. This is recommended if many calls
     * are expected to {@link #getClassInfo(String)}. Opening the info store
     * speeds class lookups, with a cost of retaining resources in the underlying
     * file access layer.
     * 
     * A call to open the info store requires a call to close the info store
     * following all accesses.
     */
    void openInfoStore();

    /**
     * Helper to close the info store. This is required at the completion of
     * using the info store.
     */
    void closeInfoStore();

    /**
     * Answer a class info object from the web module info store.
     *
     * @param className The name of the class for which to retrieve a class info object.
     *
     * @return The class info object for the named class.
     */
    ClassInfo getClassInfo(String className);

    //

    /**
     * Answer annotation targets generated from the module class source. Scan only the
     * specific extra classes.
     *
     * Use the common class source for this scan.
     *
     * The specific targets is not stored in the non-persistent cache!
     *
     * @param specificClassNames The names of the specific class which are to be scanned.
     *
     * @return Class list specific annotation targets for the module.
     *
     * @throws UnableToAdaptException Thrown by an error processing fragment paths.
     *
     * @see #getClassSource()
     */
    SpecificAnnotations getSpecificAnnotations(Set<String> specificClassNames) throws UnableToAdaptException;
}

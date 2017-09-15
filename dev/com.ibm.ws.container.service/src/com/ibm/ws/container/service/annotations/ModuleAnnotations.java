/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annotations;

import java.util.Set;

import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;

/**
 * Main link for web module annotation related services.
 * 
 * The annotation services type acts as a Future<ClassSource_Aggregate>,
 * as a Future<AnnotationTargets_Targets>, and as a Future<InfoStore>, with
 * the sharing of a single class source between the other two futures.
 * 
 * Current references are from:
 * 
 * com.ibm.ws.webcontainer.osgi.DeployedModImpl.adapt(Class<T>)
 * 
 * That adapt implementation provides three entries into the annotation
 * services:
 * 
 * <ol>
 * <li>DeployedModule adapt to ClassSource_Aggregate</li>
 * <li>DeployedModule adapt to AnnotationTargets_Targets</li>
 * <li>DeployedModule adapt to ClassSource</li>
 * </ol>
 * 
 * Notification plan:
 * 
 * Adaptation to annotation targets requires a possibly time consuming scan.
 * 
 * Informational messages are generated for the initiation of a scan, and for the
 * completion of a scan.
 * 
 * <p>Note on fragment paths:
 * 
 * <p>These are specified through a complete, ordered, list,
 * and through the partition of that list into <b>included</b>
 * <b>partial</b> and <b>excluded</b> locations. Seed locations
 * are fragment jars which are neither metadata-complete nor
 * were excluded because they were omitted from an absolute
 * ordering. Partial locations are those which are metadata-complete
 * and not omitted from an absolute ordering. Excluded locations
 * are those which were omitted because an absolute ordering is
 * specified, and no others element is specified. The excluded
 * locations are those which were not specified in the absolute
 * ordering explicit listing.</p>
 * 
 * <p>Note that exclusion has precedence over metadata-complete.</p>
 * 
 * <p>Fragment paths are relative to the "WEB-INF/lib" folder, not
 * to the rot web module container.</p>
 * 
 * <p>Included locations are scanned for annotations and for class
 * relationship information. Partial locations are scanned only for
 * class relationship information. Excluded locations are scanned only
 * for class relationship information, and only as required to complete
 * class relationship information for referenced classes.</p>
 * 
 * <p>Class relationship information is the class to superclass and
 * the class to implements relationships.</p>
 */
public interface ModuleAnnotations {

    // Intermediate values ...

    /**
     * <p>Answer the class source of the module. This includes the WEB-INF/classes
     * location and all fragments, in their proper order. Only WEB-INF/classes and
     * included fragments paths are marked as seed locations.</p>
     * 
     * @return The web module class source.
     * 
     * @throws UnableToAdaptException Thrown by an error processing fragment paths.
     */
    ClassSource_Aggregate getClassSource() throws UnableToAdaptException;

    // Step three is to complete the adapt call.  Several completions are available:

    /**
     * <p>Answer the main annotation targets of the module. This uses the common
     * web module class source.</p>
     * 
     * @return The main annotation targets for the module.
     * 
     * @throws UnableToAdaptException Thrown by an error processing fragment paths.
     * 
     * @see #getClassSource()
     */
    AnnotationTargets_Targets getAnnotationTargets() throws UnableToAdaptException;

    void addAppClassLoader(ClassLoader appClassLoader);

    //

    /**
     * <p>Answer annotation targets generated from the module class source. Scan only the
     * specific extra classes.</p>
     * 
     * <p>Use the common class source for this scan.</p>
     * 
     * <p>The specific targets is not stored in the non-persistent cache!</p>
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

    // Info access ...

    /**
     * <p>Answer the common info store for the module. This uses the common web module
     * class source.</p>
     * 
     * @return The common info store for the module.
     * 
     * @throws UnableToAdaptException Thrown by an error processing fragment paths.
     */
    InfoStore getInfoStore() throws UnableToAdaptException;

    /**
     * <p>Helper to open the info store. This is recommended if many calls
     * are expected to {@link #getClassInfo(String)}. Opening the info store
     * speeds class lookups, with a cost of retaining resources in the underlying
     * file access layer.</p>
     * 
     * <p>A call to open the info store requires a call to close the info store
     * following all accesses.</p>
     * 
     * @throws UnableToAdaptException Thrown if the open failed. Often, because of
     *             a problem opening a ZIP or a JAR file.
     */
    void openInfoStore() throws UnableToAdaptException;

    /**
     * <p>Helper to close the info store. This is required at the completion of
     * using the info store.</p>
     * 
     * @throws UnableToAdaptException Thrown if the open failed. Often, because of
     *             a problem opening a ZIP or a JAR file.
     */
    void closeInfoStore() throws UnableToAdaptException;

    /**
     * <p>Answer a class info object from the web module info store.</p>
     * 
     * @param className The name of the class for which to retrieve a class info object.
     * 
     * @return The class info object for the named class.
     * 
     * @throws UnableToAdaptException Thrown if a non-recoverable error occurred while
     *             retrieving the class info object.
     */
    ClassInfo getClassInfo(String className) throws UnableToAdaptException;
}

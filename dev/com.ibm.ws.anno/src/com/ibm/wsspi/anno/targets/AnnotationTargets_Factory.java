/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

package com.ibm.wsspi.anno.targets;

import java.util.Set;
import java.util.logging.Logger;

import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.util.Util_Factory;

public interface AnnotationTargets_Factory {
    // Logging ...

    String getHashText();

    // Main factory linkage ...
    //
    // A utility factory will be needed for supporting widgets.

    Util_Factory getUtilFactory();

    // Error handling assists ...

    AnnotationTargets_Exception newAnnotationTargetsException(Logger logger, String message);

    AnnotationTargets_Exception wrapIntoAnnotationTargetsException(Logger logger,
                                                                   String callingClassName,
                                                                   String callingMethodName,
                                                                   String message, Throwable th);

    // Target constructors ...

    AnnotationTargets_Targets createTargets() throws AnnotationTargets_Exception;

    // Utilities for annotation target validation: Annotation targets tables may be
    // compared.  Fault objects are used to record differences.

	AnnotationTargets_Fault createFault(String unresolvedText);
    AnnotationTargets_Fault createFault(String unresolvedText, String parameter);
    AnnotationTargets_Fault createFault(String unresolvedText, String... parameters);

    //

    /**
     * <p>Create targets for a class source.  Immediately scan the class source
     * for annotations.</p>
     *
     * @param classSource The class source which is to be scanned.
     *
     * @return Annotation targets for the class source.
     *
     * @throws AnnotationTargets_Exception Thrown in case of a scan failure.
     */
    AnnotationTargets_Targets createTargets(ClassSource_Aggregate classSource)
        throws AnnotationTargets_Exception;
    //

    /**
     * <p>Create targets for a class source.  Immediately scan the class source
     * for annotations.  Scan only particular specified classes, and record only
     * occurrences of specified annotations.  Do not record class reference information
     * to the results.</p>
     *
     * @param classSource The class source which is to be scanned.
     * @param specificClassNames The specific classes which are to be scanned.
     * @param specificAnnotationClassNames The specific annotations which are to be recorded.
     *
     * @return Annotation targets for the class source.
     *
     * @throws AnnotationTargets_Exception Thrown in case of a scan failure.
     */
    AnnotationTargets_Targets createTargets(ClassSource_Aggregate classSource,
                                            Set<String> specificClassNames,
                                            Set<String> specificAnnotationClassNames)
        throws AnnotationTargets_Exception;

    /**
     * <p>Create targets for a class source.  Immediately scan the class source
     * for annotations.  Scan only particular specified classes.  Do not record class
     * reference information to the results.</p>
     *
     * @param classSource The class source which is to be scanned.
     * @param specificClassNames The specific classes which are to be scanned.
     *
     * @return Annotation targets for the class source.
     *
     * @throws AnnotationTargets_Exception Thrown in case of a scan failure.
     */
    AnnotationTargets_Targets createTargets(ClassSource_Aggregate classSource,
                                            Set<String> specificClassNames)
        throws AnnotationTargets_Exception;
}

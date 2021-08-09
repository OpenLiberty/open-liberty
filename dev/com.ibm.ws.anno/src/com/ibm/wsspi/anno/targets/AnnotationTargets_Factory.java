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

package com.ibm.wsspi.anno.targets;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.anno.util.Util_Factory;

public interface AnnotationTargets_Factory {
    // Logging ...

    String getHashText();

    // Main factory linkage ...
    //
    // A utility factory will be needed for supporting widgets.

    Util_Factory getUtilFactory();

    // Error handling assists ...

    AnnotationTargets_Exception newAnnotationTargetsException(TraceComponent logger, String message);

    AnnotationTargets_Exception wrapIntoAnnotationTargetsException(TraceComponent logger,
                                                                   String callingClassName,
                                                                   String callingMethodName,
                                                                   String message, Throwable th);

    // Target constructors ...
    //
    // The usual mode is to have detail enabled.  'non-detail' turns off field and method level
    // details.  That capability was provided as a potential performance optimization.  The default
    // constructors enable detail processing, as testing has found no significant performance gain
    // with detail disabled.
    //
    // The reader based constructors obtain the target details from a text format file, and
    // make use of no class source.

    boolean DETAIL_IS_ENABLED = true;
    boolean DETAIL_IS_NOT_ENABLED = false;

    AnnotationTargets_Targets createTargets()
                    throws AnnotationTargets_Exception;

    AnnotationTargets_Targets createTargets(boolean isDetailEnabled)
                    throws AnnotationTargets_Exception;

    // Utility for annotation target validation: Annotation targets tables may be
    // compared.  Fault objects are used to record differences.

    AnnotationTargets_Fault createFault(String unresolvedText, String[] parameters);
}

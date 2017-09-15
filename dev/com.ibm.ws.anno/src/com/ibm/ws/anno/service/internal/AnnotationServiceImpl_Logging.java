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

package com.ibm.ws.anno.service.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.wsspi.anno.service.AnnotationService_Logging;

@TraceOptions(traceGroup = AnnotationService_Logging.ANNO_LOGGER_STATE)
public class AnnotationServiceImpl_Logging implements AnnotationService_Logging {

    /**
     * Annotation Scan logging conversion notes.
     * 
     * The com.ibm.ws.anno package uses com.ibm.websphere.ras.Tr for all logging now.
     * 
     * Tr.entry/exit trace uses the java.util.Level of FINER,
     * Tr.debug uses the java.util.Level FINEST
     * Tr.dump is a level lower that Tr.debug.
     * 
     * Liberty trace injection will automatically inject entry/exit trace for all methods. To override
     * this, use the @com.ibm.websphere.ras.annotation.Trivial annotation before the method name. This has
     * been used at some points in the anno code to reduce trace for methods which only do a simple 'get',
     * and where the old Logger Entry/Return trace was converted to Tr.debug in order to preserve information that
     * was being displayed in the trace message.
     * 
     * "@Trivial" is not needed on methods which have Tr.enter and Tr.exit; the instrumented trace injection process
     * detects these statements and avoided added duplicate statements.
     * 
     * For more information on the instrumented logging see:
     * http://was.pok.ibm.com/xwiki/bin/view/Liberty/Logging
     * 
     * Conversion from java.util.Logger to using the Liberty standard Tr followed these standards:
     * - change Logger(FINER) entries to use Tr(debug)
     * - change Logger(FINEST) entries to use Tr(dump)
     * 
     * - The AnnotationService_Logging.ANNO_LOGGER_STATE logging was implemented by registering a TraceComponent
     * under this class. Therefore, to restrict tracing to see only 'State' traces, the Liberty trace specification
     * should be set to enable only 'com.ibm.wsspi.anno.service.internal.AnnotationServiceImpl_Logging' traces.
     * 
     */

    // Special category loggers ... these cut across the function loggers.

    /** <p>Common reference to the annotations state logger.</p> */
    public static final TraceComponent stateLogger = Tr.register(AnnotationServiceImpl_Logging.class);

    /**
     * <p>Answer a base hash code for a target object.</p>
     * 
     * <p>This override is provided to avoid issues with container types, which often
     * override {@link Object#hashCode()} to aggregate the hash codes of contained
     * elements. That creates an unstable hash value.</p>
     * 
     * @param object A target object for which to obtain a base hash code.
     * @return A hash code for the object.
     */
    public static String getBaseHash(Object object) {
        return object.getClass().getSimpleName() + "@" + Integer.toString((new Object()).hashCode());
    }

    // Utilities for avoiding warnings ... for cases where
    // return values are not used, and must be consumed through a function
    // call to avoid a compiler unused value warning.

    /**
     * <p>Utility to explicitly consume a boolean value.</p>
     * 
     * <p>This method is intended to be used to indicate a value
     * which is being disregarded. The method call on the value
     * causes a compiler warning to be ignored.</p>
     * 
     * <p>All such cases should be examined to determined if the
     * value which is being ignored is correct to ignore.</p>
     * 
     * @param booleanValue A boolean value which is to be consumed.
     */
    public static void consume(boolean booleanValue) {
        // NO-OP
    }

    /**
     * <p>Utility to explicitly consume an object reference.</p>
     * 
     * <p>This method is intended to be used to indicate a value
     * which is being disregarded. The method call on the value
     * causes a compiler warning to be ignored.</p>
     * 
     * <p>All such cases should be examined to determined if the
     * value which is being ignored is correct to ignore.</p>
     * 
     * @param objectRef An object reference which is to be consumed.
     */
    public static void consumeRef(Object objectRef) {
        // NO-OP
    }
}

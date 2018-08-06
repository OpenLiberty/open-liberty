/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.targets.delta;

import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.wsspi.anno.util.Util_BidirectionalMapDelta;
import com.ibm.wsspi.anno.util.Util_PrintLogger;

public interface TargetsDelta_Annotations {
    String getHashText();

    void log(Logger useLogger);
    void log(PrintWriter writer);
    void log(Util_PrintLogger useLogger);

    void describe(String prefix, List<String> nonNull);

    //

    AnnotationTargetsImpl_Factory getFactory();

    //

    Util_BidirectionalMapDelta getPackageAnnotationDelta();
    Util_BidirectionalMapDelta getClassAnnotationDelta();
    Util_BidirectionalMapDelta getFieldAnnotationDelta();
    Util_BidirectionalMapDelta getMethodAnnotationDelta();

    boolean isNull();
    boolean isNull(boolean ignoreRemovedPackages);
}
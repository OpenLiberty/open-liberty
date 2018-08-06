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
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.wsspi.anno.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.anno.util.Util_IdentityMapDelta;
import com.ibm.wsspi.anno.util.Util_IdentitySetDelta;
import com.ibm.wsspi.anno.util.Util_PrintLogger;

public interface TargetsDelta_Classes {

    String getHashText();

    void log(Logger useLogger);
    void log(PrintWriter writer);
    void log(Util_PrintLogger useLogger);

    void describe(String prefix, List<String> nonNull);

    //

    AnnotationTargets_Factory getFactory();

    //

    Util_IdentitySetDelta getPackageDelta();
    Util_IdentitySetDelta getClassDelta();
    Util_IdentityMapDelta getSuperclassDelta();

    //

    Map<String, String[]> i_getAddedInterfaceNames();
    Map<String, String[]> i_getRemovedInterfaceNames();
    boolean isNullInterfaceChanges();
    boolean isNullInterfaceChanges(boolean ignoreRemovedInterfaces);

    //

    boolean isNull();
    boolean isNull(boolean ignoreRemovedPackages, boolean ignoreRemovedInterfaces);
}
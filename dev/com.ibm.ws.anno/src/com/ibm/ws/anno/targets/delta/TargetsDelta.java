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

import com.ibm.wsspi.anno.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.anno.util.Util_PrintLogger;

public interface TargetsDelta {

    String getHashText();

    void log(Logger useLogger);
    void log(PrintWriter writer);
    void log(Util_PrintLogger useLogger);

    void describe(String prefix, List<String> nonNull);

    //

    AnnotationTargets_Factory getFactory();

    //

    String getAppName();
    String getModName();
    String getModCatName();

    TargetsDelta_Targets getSeedDelta();
    TargetsDelta_Targets getPartialDelta();
    TargetsDelta_Targets getExcludedDelta();
    TargetsDelta_Targets getExternalDelta();

    //

    boolean isNull();

    boolean DO_IGNORE_REMOVED_PACKAGES = true;
    boolean DO_IGNORE_REMOVED_INTERFACES = true;

    /**
     * JANDEX does not generate package information, and
     * can remove redundant interface declarations.  This results
     * in differences in scan results.
     * 
     * As a work-around, allow tests to ignore this omissions.
      
     * @param ignoreRemovedPackages Control parameter: Tell if removed packages are to be ignored.
     * @param ignoreRemovedInterfaces Control parameter: Tell if removed interfaces are to be ignored.
     * 
     * @return True or false telling if the difference is null, taking into account the exceptions
     *     specified by the two control parameters.
     */
    boolean isNull(boolean ignoreRemovedPackages, boolean ignoreRemovedInterfaces);
}

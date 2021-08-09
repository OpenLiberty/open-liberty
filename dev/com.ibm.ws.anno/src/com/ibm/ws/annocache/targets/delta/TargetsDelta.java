/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.delta;

import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import com.ibm.wsspi.annocache.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.annocache.util.Util_PrintLogger;

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

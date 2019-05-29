/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
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
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.wsspi.annocache.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.annocache.util.Util_IdentityMapDelta;
import com.ibm.wsspi.annocache.util.Util_IdentitySetDelta;
import com.ibm.wsspi.annocache.util.Util_PrintLogger;

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

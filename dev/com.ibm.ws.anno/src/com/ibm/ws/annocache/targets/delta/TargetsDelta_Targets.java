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

import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.annocache.util.Util_PrintLogger;

public interface TargetsDelta_Targets {

    String getHashText();

    void log(Logger useLogger);
    void log(PrintWriter writer);
    void log(Util_PrintLogger useLogger);

    void describe(String prefix, List<String> nonNull);

    //

    AnnotationTargets_Factory getFactory();

    //

    ScanPolicy getScanPolicy();

    //

    TargetsDelta_Classes getClassesDelta();
    TargetsDelta_Annotations getAnnotationsDelta();

    //

    boolean isNull();
    boolean isNull(boolean ignoreRemovedPackages, boolean ignoreRemovedInterfaces);
}

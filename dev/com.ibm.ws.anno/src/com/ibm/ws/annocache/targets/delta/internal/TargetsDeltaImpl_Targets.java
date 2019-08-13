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
package com.ibm.ws.annocache.targets.delta.internal;

import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.delta.TargetsDelta_Targets;
import com.ibm.ws.annocache.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.ws.annocache.targets.internal.TargetsTableImpl;
import com.ibm.ws.annocache.util.delta.internal.UtilImpl_PrintLogger;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.util.Util_PrintLogger;

public class TargetsDeltaImpl_Targets implements TargetsDelta_Targets {
    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    public static final String CLASS_NAME = TargetsDeltaImpl_Targets.class.getSimpleName();

    private final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public TargetsDeltaImpl_Targets(
        AnnotationTargetsImpl_Factory factory,
        ScanPolicy scanPolicy,
        TargetsTableImpl finalTable, TargetsTableImpl initialTable) {

        String methodName = "<init>";

        this.factory = factory;

        this.hashText =
            getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) +
            "(" + scanPolicy + ")";

        this.scanPolicy = scanPolicy;

        this.classesDelta = new TargetsDeltaImpl_Classes(this.factory,
            finalTable.getClassTable(), initialTable.getClassTable());
        this.annotationsDelta = new TargetsDeltaImpl_Annotations(this.factory,
            finalTable.getAnnotationTable(), initialTable.getAnnotationTable());

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Null [ {1} ]",
                new Object[] { this.hashText, Boolean.valueOf(this.isNull()) });
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Classes isNull [ {1} ]",
                new Object[] { this.hashText, Boolean.valueOf(this.classesDelta.isNull()) });
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Annotations isNull [ {1} ]",
                new Object[] { this.hashText, Boolean.valueOf(this.annotationsDelta.isNull()) });
        }
    }

    //

    public AnnotationTargetsImpl_Factory factory;

    @Override
    public  AnnotationTargetsImpl_Factory getFactory() {
        return factory;
    }

    //

    private final ScanPolicy scanPolicy;

    @Override
    public ScanPolicy getScanPolicy() {
        return scanPolicy;
    }

    //

    private final TargetsDeltaImpl_Classes classesDelta;

    public TargetsDeltaImpl_Classes getClassesDelta() {
        return classesDelta;
    }

    private final TargetsDeltaImpl_Annotations annotationsDelta;

    public TargetsDeltaImpl_Annotations getAnnotationsDelta() {
        return annotationsDelta;
    }

    //

    @Override
    public boolean isNull() {
        return ( getClassesDelta().isNull() &&
                 getAnnotationsDelta().isNull() );
    }

    @Override
    public boolean isNull(boolean ignoreRemovedPackages, boolean ignoreRemovedInterfaces) {
        return ( getClassesDelta().isNull(ignoreRemovedPackages, ignoreRemovedInterfaces) &&
                 getAnnotationsDelta().isNull(ignoreRemovedPackages) );
    }

    @Override
    public void describe(String prefix, List<String> nonNull) {
        getClassesDelta().describe(prefix + ": Classes", nonNull);
        getAnnotationsDelta().describe(prefix + ": Annotations", nonNull);
    }

    //

    @Override
    public void log(Logger useLogger) {
        if ( useLogger.isLoggable(Level.FINER) ) {
            log( new UtilImpl_PrintLogger(useLogger) );
        }
    }

    @Override
    public void log(PrintWriter writer) {
        log( new UtilImpl_PrintLogger(writer) );
    }

    @Override
    public void log(Util_PrintLogger useLogger) {
        String methodName = "log";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
             "Annotation Targets Category Delta [ {0} ] [ {1} ] BEGIN",
             getScanPolicy(), getHashText());

        getClassesDelta().log(useLogger);
        getAnnotationsDelta().log(useLogger);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
             "Annotation Targets Category Delta [ {0} ] [ {1} ] END",
             getScanPolicy(), getHashText());
    }
}

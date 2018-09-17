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
package com.ibm.ws.anno.targets.delta.internal;

import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.ws.anno.targets.delta.TargetsDelta_Annotations;
import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.ws.anno.targets.internal.TargetsTableAnnotationsImpl;
import com.ibm.ws.anno.util.delta.internal.UtilImpl_BidirectionalMapDelta;
import com.ibm.ws.anno.util.delta.internal.UtilImpl_PrintLogger;
import com.ibm.ws.anno.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.anno.util.Util_PrintLogger;

public class TargetsDeltaImpl_Annotations implements TargetsDelta_Annotations {

    protected static final Logger logger = AnnotationServiceImpl_Logging.ANNO_LOGGER;

    public static final String CLASS_NAME = TargetsDeltaImpl_Annotations.class.getSimpleName();

    //

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public TargetsDeltaImpl_Annotations(
        AnnotationTargetsImpl_Factory factory,
        TargetsTableAnnotationsImpl finalTargets,
        TargetsTableAnnotationsImpl initialTargets) {

        String methodName = "<init>";
        
        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.factory = factory;

        UtilImpl_Factory utilFactory = factory.getUtilFactory();

        this.packageAnnotationDelta = utilFactory.subtractBiMap(
            finalTargets.i_getPackageAnnotations(),
            initialTargets.i_getPackageAnnotations() );

        this.classAnnotationDelta = utilFactory.subtractBiMap(
            finalTargets.i_getClassAnnotations(),
            initialTargets.i_getClassAnnotations() );

        this.fieldAnnotationDelta = utilFactory.subtractBiMap(
            finalTargets.i_getFieldAnnotations(),
            initialTargets.i_getFieldAnnotations() );

        this.methodAnnotationDelta = utilFactory.subtractBiMap(
            finalTargets.i_getMethodAnnotations(),
            initialTargets.i_getMethodAnnotations() );

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);

            int addedPackageAnnotations = this.packageAnnotationDelta.getAddedMap().getHeldSet().size();
            if ( addedPackageAnnotations != 0 ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Packages with added annotations [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(addedPackageAnnotations) });
            }

            int removedPackageAnnotations = this.packageAnnotationDelta.getRemovedMap().getHeldSet().size();
            if ( removedPackageAnnotations != 0 ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Packages with removed annotations [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(removedPackageAnnotations) });
            }

            int addedClassAnnotations = this.classAnnotationDelta.getAddedMap().getHeldSet().size();
            if ( addedClassAnnotations != 0 ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Classes with added annotations [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(addedClassAnnotations) } );
            }
            int removedClassAnnotations = this.classAnnotationDelta.getRemovedMap().getHeldSet().size();
            if ( removedClassAnnotations != 0 ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Classes with removed annotations [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(removedClassAnnotations) });
            }

            int addedFieldAnnotations = this.fieldAnnotationDelta.getAddedMap().getHeldSet().size();
            if ( addedFieldAnnotations != 0 ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Classes with added field annotations [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(addedFieldAnnotations) });
            }
            int removedFieldAnnotations = this.fieldAnnotationDelta.getRemovedMap().getHeldSet().size();
            if ( removedFieldAnnotations != 0 ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Classes with removed field annotations [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(removedFieldAnnotations) });
            }

            int addedMethodAnnotations = this.methodAnnotationDelta.getAddedMap().getHeldSet().size();
            if ( addedMethodAnnotations != 0 ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Classes with added method annotations [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(addedMethodAnnotations) });
            }

            int removedMethodAnnotations = this.methodAnnotationDelta.getRemovedMap().getHeldSet().size();
            if ( removedMethodAnnotations != 0 ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Classes with removed method annotations [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(removedMethodAnnotations) });
            }
        }
    }

    //

    protected final AnnotationTargetsImpl_Factory factory;

    @Override
    public AnnotationTargetsImpl_Factory getFactory() {
        return factory;
    }

    //

    protected final UtilImpl_BidirectionalMapDelta packageAnnotationDelta;
    protected final UtilImpl_BidirectionalMapDelta classAnnotationDelta;
    protected final UtilImpl_BidirectionalMapDelta methodAnnotationDelta;
    protected final UtilImpl_BidirectionalMapDelta fieldAnnotationDelta;

    @Override
    public UtilImpl_BidirectionalMapDelta getPackageAnnotationDelta() {
        return packageAnnotationDelta;
    }

    @Override
    public UtilImpl_BidirectionalMapDelta getClassAnnotationDelta() {
        return classAnnotationDelta;
    }

    @Override
    public UtilImpl_BidirectionalMapDelta getFieldAnnotationDelta() {
        return fieldAnnotationDelta;
    }

    @Override
    public UtilImpl_BidirectionalMapDelta getMethodAnnotationDelta() {
        return methodAnnotationDelta;

    }

    //

    @Override
    public boolean isNull() {
        return ( getPackageAnnotationDelta().isNull() &&
                 getClassAnnotationDelta().isNull() &&
                 getFieldAnnotationDelta().isNull() &&
                 getMethodAnnotationDelta().isNull() );
    }

    @Override
    public boolean isNull(boolean ignoreRemovedPackages) {
        return ( getPackageAnnotationDelta().isNull(ignoreRemovedPackages) &&
                 getClassAnnotationDelta().isNull() &&
                 getFieldAnnotationDelta().isNull() &&
                 getMethodAnnotationDelta().isNull() );

    }

    @Override
    public void describe(String prefix, List<String> nonNull) {
        getPackageAnnotationDelta().describe(prefix + ": Package Annotations", nonNull);
        getClassAnnotationDelta().describe(prefix + ": Class Annotations", nonNull);
        getFieldAnnotationDelta().describe(prefix + ": Field Annotations", nonNull);
        getMethodAnnotationDelta().describe(prefix + ": Method Annotations", nonNull);
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

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Annotations Delta: BEGIN: [ {0} ]", getHashText());

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Package Annotation Delta: BEGIN: [ {0} ]", getHashText());
        getPackageAnnotationDelta().log(useLogger);
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Package Annotation Delta: END: [ {0} ]", getHashText());
        
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Annotation Delta: BEGIN: [ {0} ]", getHashText());
        getClassAnnotationDelta().log(useLogger);
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Annotation Delta: END: [ {0} ]", getHashText());

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Field Annotation Delta: BEGIN: [ {0} ]", getHashText());
        getFieldAnnotationDelta().log(useLogger);
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Field Annotation Delta: END: [ {0} ]", getHashText());
        
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Method Annotation Delta: BEGIN: [ {0} ]", getHashText());
        getMethodAnnotationDelta().log(useLogger);
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Method Annotation Delta: END: [ {0} ]", getHashText());

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Annotations Delta: END: [ {0} ]", getHashText());
    }
}

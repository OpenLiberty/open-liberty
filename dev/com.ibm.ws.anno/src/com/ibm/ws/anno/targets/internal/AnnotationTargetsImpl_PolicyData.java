/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.targets.internal;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.util.internal.UtilImpl_BidirectionalMap;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets.AnnotationCategory;
import com.ibm.wsspi.anno.util.Util_BidirectionalMap;
import com.ibm.wsspi.anno.util.Util_InternMap;

/**
 * Wrapper for policy data for a single scan policy. Used for SEED, PARTIAL, and EXCLUDED.
 * Not used for EXTERNAL, which only records classes.
 */
public class AnnotationTargetsImpl_PolicyData {

    protected AnnotationTargetsImpl_PolicyData(AnnotationTargets_Targets targets,
                                               Util_InternMap classInternMap,
                                               ScanPolicy scanPolicy,
                                               boolean isDetailEnabled) {

        this.targets = (AnnotationTargetsImpl_Targets) targets;
        this.scanPolicy = scanPolicy;

        this.i_classNames = new IdentityHashMap<String, String>();

        this.packageAnnotationData = this.targets.createBidiMap(scanPolicy + " packages with annotations", classInternMap,
                                                           "annotations", classInternMap,
                                                           Util_BidirectionalMap.IS_ENABLED);

        this.classAnnotationData = this.targets.createBidiMap(scanPolicy + " class with annotations", classInternMap,
                                                         "annotations", classInternMap,
                                                         Util_BidirectionalMap.IS_ENABLED);

        this.fieldAnnotationData = this.targets.createBidiMap(scanPolicy + " class with field annotations", classInternMap,
                                                         "annotations", classInternMap,
                                                         isDetailEnabled);

        this.methodAnnotationData = this.targets.createBidiMap(scanPolicy + " class with method annotations", classInternMap,
                                                          "annotations", classInternMap,
                                                          isDetailEnabled);
    }

    //

    protected final AnnotationTargetsImpl_Targets targets;

    public AnnotationTargets_Targets getTargets() {
        return targets;
    }

    protected ScanPolicy scanPolicy;

    public ScanPolicy getScanPolicy() {
        return scanPolicy;
    }

    //

    protected final Map<String, String> i_classNames;

    public Set<String> getClassNames() {
        return i_classNames.keySet();
    }

    protected boolean i_isClassName(String i_className) {
        return i_classNames.containsKey(i_className);
    }

    protected void i_addScannedClassName(String i_className) {
        i_classNames.put(i_className, i_className);
    }

    //

    protected final UtilImpl_BidirectionalMap packageAnnotationData;

    public UtilImpl_BidirectionalMap getPackageAnnotationData() {
        return packageAnnotationData;
    }

    protected final UtilImpl_BidirectionalMap classAnnotationData;

    public UtilImpl_BidirectionalMap getClassAnnotationData() {
        return classAnnotationData;
    }

    protected final UtilImpl_BidirectionalMap fieldAnnotationData;

    public UtilImpl_BidirectionalMap getFieldAnnotationData() {
        return fieldAnnotationData;
    }

    protected final UtilImpl_BidirectionalMap methodAnnotationData;

    public UtilImpl_BidirectionalMap getMethodAnnotationData() {
        return methodAnnotationData;
    }

    //

    public Set<String> getAnnotatedPackages() {
        return packageAnnotationData.getHolderSet();
    }

    public Set<String> getPackageAnnotations() {
        return packageAnnotationData.getHeldSet();
    }

    public Set<String> getPackageAnnotations(String packageName) {
        return packageAnnotationData.selectHeldOf(packageName);
    }

    //

    public Set<String> getAnnotatedClasses() {
        return classAnnotationData.getHolderSet();
    }

    public Set<String> getClassAnnotations() {
        return classAnnotationData.getHeldSet();
    }

    public Set<String> getClassAnnotations(String className) {
        return classAnnotationData.selectHeldOf(className);
    }

    //

    public Set<String> getClassesWithFieldAnnotations() {
        return fieldAnnotationData.getHolderSet();
    }

    public Set<String> getFieldAnnotations() {
        return fieldAnnotationData.getHeldSet();
    }

    public Set<String> getFieldAnnotations(String className) {
        return fieldAnnotationData.selectHeldOf(className);
    }

    //

    public Set<String> getClassesWithMethodAnnotations() {
        return methodAnnotationData.getHolderSet();
    }

    public Set<String> getMethodAnnotations() {
        return methodAnnotationData.getHeldSet();
    }

    public Set<String> getMethodAnnotations(String className) {
        return methodAnnotationData.selectHeldOf(className);
    }

    //

    public Set<String> getAnnotatedTargets(AnnotationCategory category) {
        return getTargetData(category).getHolderSet();
    }

    public Set<String> getAnnotatedTargets(String annotationName, AnnotationCategory category) {
        return getTargetData(category).selectHoldersOf(annotationName);
    }

    public Set<String> getAnnotations(AnnotationCategory category) {
        return getTargetData(category).getHeldSet();
    }

    public Set<String> getAnnotations(String targetName, AnnotationCategory category) {
        return getTargetData(category).selectHeldOf(targetName);
    }

    //

    protected UtilImpl_BidirectionalMap getTargetData(AnnotationCategory category) {
        if (category == AnnotationCategory.PACKAGE) {
            return (packageAnnotationData);
        } else if (category == AnnotationCategory.CLASS) {
            return (classAnnotationData);
        } else if (category == AnnotationCategory.METHOD) {
            return (methodAnnotationData);
        } else if (category == AnnotationCategory.FIELD) {
            return (fieldAnnotationData);
        } else {
            throw new IllegalArgumentException("Category [ " + category + " ]");
        }
    }

    //

    protected void log(TraceComponent logger) {
        Tr.debug(logger, "Annotations Data [ " + getScanPolicy() + " ]:");

        packageAnnotationData.log(logger);
        classAnnotationData.log(logger);
        fieldAnnotationData.log(logger);
        methodAnnotationData.log(logger);

        Tr.debug(logger, "Annotations Data [ " + getScanPolicy() + " ]: Complete");
    }
}

/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.test.scan;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;

import com.ibm.ws.annocache.targets.delta.internal.TargetsDeltaImpl;
import com.ibm.ws.annocache.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.ws.annocache.targets.internal.AnnotationTargetsImpl_Targets;
import com.ibm.ws.annocache.targets.internal.TargetsTableImpl;
import com.ibm.ws.annocache.test.utils.TestUtils;
import com.ibm.ws.annocache.util.internal.UtilImpl_InternMap;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.info.AnnotationInfo;
import com.ibm.wsspi.annocache.info.ClassInfo;
import com.ibm.wsspi.annocache.info.FieldInfo;
import com.ibm.wsspi.annocache.info.InfoStore;
import com.ibm.wsspi.annocache.info.MethodInfo;
import com.ibm.wsspi.annocache.info.PackageInfo;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Fault;
import com.ibm.wsspi.annocache.util.Util_BidirectionalMap;
import com.ibm.wsspi.annocache.util.Util_InternMap;

public class Test_Base_Result {
    public static long getTime() {
        return System.currentTimeMillis();
    }

    //

    public String appName;
    public String modName;
    public String fullName;

    public boolean testFailed;
    public LinkedList<AnnotationTargets_Fault> faults;

    public long scanTime;

    public int classInternCount;
    public int classInternTotalSize;

    public int fieldInternCount;
    public int fieldInternTotalSize;

    public int methodInternCount;
    public int methodInternTotalSize;

    public int uniquePackages;
    public int uniquePackageAnnotations;

    public int uniqueClasses;
    public int uniqueClassAnnotations;

    public int uniqueClassesWithFieldAnnotations;
    public int uniqueFieldAnnotations;

    public int uniqueClassesWithMethodAnnotations;
    public int uniqueMethodAnnotations;

    public int seedClassCount;
    public int partialClassCount;
    public int excludedClassCount;
    public int externalClassCount;

    public long infoTime;

    public TargetsDeltaImpl difference;

    //

    public Test_Base_Result(String appName, String modName) {
        super();

        this.appName = appName;
        this.modName = modName;
        this.fullName = this.appName + "." + this.modName;

        this.testFailed = false;
        this.faults = new LinkedList<AnnotationTargets_Fault>();

        this.scanTime = 0L;

        this.classInternCount = 0;
        this.classInternTotalSize = 0;

        this.fieldInternCount = 0;
        this.fieldInternTotalSize = 0;

        this.methodInternCount = 0;
        this.methodInternTotalSize = 0;

        this.uniquePackages = 0;
        this.uniquePackageAnnotations = 0;

        this.uniqueClasses = 0;
        this.uniqueClassAnnotations = 0;

        this.uniqueClassesWithFieldAnnotations = 0;
        this.uniqueFieldAnnotations = 0;

        this.uniqueClassesWithMethodAnnotations = 0;
        this.uniqueMethodAnnotations = 0;

        this.seedClassCount = 0;
        this.partialClassCount = 0;
        this.excludedClassCount = 0;
        this.externalClassCount = 0;

        this.infoTime = 0L;
        
        this.difference = null;
    }

    //

    public void addFault(AnnotationTargets_Fault fault) {
        faults.add(fault);
        testFailed = true;
    }

    public boolean getTestFailed() {
        return testFailed;
    }

    public void setTestFailed(boolean value) {
        testFailed = value;
    }

    //

    public void setScanTime(long scanTime) {
        this.scanTime = scanTime;
    }

    public void setCounts(
        ClassSource_Aggregate classSource,
        AnnotationTargetsImpl_Targets targets) {

        setInternCounts(targets);
        setClassCounts(targets);
        setAnnotationCounts(targets);
    }

    public void setInternCounts(AnnotationTargetsImpl_Targets targets) {
        Util_InternMap classInternMap = targets.getClassNameInternMap();

        classInternCount = classInternMap.getSize();
        classInternTotalSize = classInternMap.getTotalLength();
    }

    public void setClassCounts(AnnotationTargetsImpl_Targets targets) {
        seedClassCount = targets.getSeedClassNames().size();
        partialClassCount = targets.getPartialClassNames().size();
        excludedClassCount = targets.getExcludedClassNames().size();
        externalClassCount = targets.getExternalClassNames().size();
    }

    public void setAnnotationCounts(AnnotationTargetsImpl_Targets targets) {
        Util_BidirectionalMap packageData = targets.i_getPackageAnnotations();
        uniquePackages = packageData.getHolderSet().size();
        uniquePackageAnnotations = packageData.getHeldSet().size();

        Util_BidirectionalMap classData = targets.i_getClassAnnotations();
        uniqueClasses = classData.getHolderSet().size();
        uniqueClassAnnotations = classData.getHeldSet().size();

        Util_BidirectionalMap fieldData = targets.i_getFieldAnnotations();
        uniqueClassesWithFieldAnnotations = fieldData.getHolderSet().size();
        uniqueFieldAnnotations = fieldData.getHeldSet().size();

        Util_BidirectionalMap methodData = targets.i_getMethodAnnotations();
        uniqueClassesWithMethodAnnotations = methodData.getHolderSet().size();
        uniqueMethodAnnotations = methodData.getHeldSet().size();
    }

    //

    public void setInfoTime(long infoTime) {
        this.infoTime = infoTime;
    }

    //

    private ClassSource_Aggregate.TimingData timingData;

    public void setTimingData(ClassSource_Aggregate.TimingData timingData) {
        this.timingData = timingData;
    }

    public ClassSource_Aggregate.TimingData getTimingData() {
        return timingData;
    }    

    //

    public void display(PrintWriter writer) {
        writer.println("============================================================");
        writer.println("Target [ " + appName + "." + modName + " ]");

        writer.println("------------------------------------------------------------");
        displayTimes(writer);

        writer.println("------------------------------------------------------------");
        displayInternCounts(writer);

        writer.println("------------------------------------------------------------");
        displayClassCounts(writer);

        writer.println("------------------------------------------------------------");
        displayAnnotationCounts(writer);

        writer.println("------------------------------------------------------------");
        displayTimingData(writer);

        writer.println("------------------------------------------------------------");
        displayFaults(writer);

        writer.println("============================================================");

        Assert.assertFalse(testFailed);
    }

    public void displayTimes(PrintWriter writer) {
        writer.println("Scan time [ " + scanTime + " ]");
        writer.println("Info time [ " + infoTime + " ]");
    }

    public void displayInternCounts(PrintWriter writer) {
        writer.println("Class intern map:");
        writer.println("  Size [ " + Integer.valueOf(classInternCount) + " ]");
        writer.println("  Total Length [ " + Integer.valueOf(classInternTotalSize) + " ]");

        writer.println("Field intern map:");
        writer.println("  Size [ " + Integer.valueOf(fieldInternCount) + " ]");
        writer.println("  Total Length [ " + Integer.valueOf(fieldInternTotalSize) + " ]");

        writer.println("Method intern map:");
        writer.println("  Size [ " + Integer.valueOf(methodInternCount) + " ]");
        writer.println("  Total Length [ " + Integer.valueOf(methodInternTotalSize) + " ]");
    }

    public void displayClassCounts(PrintWriter writer) {
        writer.println("Scanned classes:");
        writer.println("  Seed     [ " + Integer.valueOf(seedClassCount) + " ]");
        writer.println("  Partial  [ " + Integer.valueOf(partialClassCount) + " ]");
        writer.println("  Excluded [ " + Integer.valueOf(excludedClassCount) + " ]");
        writer.println("  External [ " + Integer.valueOf(externalClassCount) + " ]");
    }

    public void displayAnnotationCounts(PrintWriter writer) {
        writer.println("Packages with annotations       [ " + uniquePackages + " ]");
        writer.println("Unique packages annotations     [ " + uniquePackageAnnotations + " ]");

        writer.println("Classes with class annotations  [ " + uniqueClasses + " ]");
        writer.println("Unique class annotations        [ " + uniqueClassAnnotations + " ]");

        writer.println("Classes with field annotations  [ " + uniqueClassesWithFieldAnnotations + " ]");
        writer.println("Unique field annotations        [ " + uniqueFieldAnnotations + " ]");

        writer.println("Classes with method annotations [ " + uniqueClassesWithMethodAnnotations + " ]");
        writer.println("Unique method annotations       [ " + uniqueMethodAnnotations + " ]");
    }

    public void displayTimingData(PrintWriter writer) {
        writer.println("Scan Sources [ " + timingData.getScanSources() + " ]");
        writer.println("Scan Time [ " + timingData.getScanTime() + " ]");
        writer.println("Scan Classes [ " + timingData.getScanClasses() + " ]");

        writer.println("Jandex Sources [ " + timingData.getJandexSources() + " ]");
        writer.println("Jandex Time [ " + timingData.getJandexTime() + " ]");
        writer.println("Jandex Classes [ " + timingData.getJandexClasses() + " ]");

        writer.println("External Sources [ " + timingData.getExternalSources() + " ]");
        writer.println("External Time [ " + timingData.getExternalTime() + " ]");
        writer.println("External Classes [ " + timingData.getExternalClasses() + " ]");

        writer.println("Cache Read Time [ " + timingData.getCacheReadTime() + " ]");
        writer.println("Cache Write Time [ " + timingData.getCacheWriteTime() + " ]");
    }

    public void displayFaults(PrintWriter writer) {
        writer.println("Faults [ " + faults.size() + " ]");
        for ( AnnotationTargets_Fault message : faults ) {
            writer.println( "Fault: " + message.getResolvedText() );
        }
    }

    //

    public void validateInterns(PrintWriter writer, AnnotationTargetsImpl_Targets targets) {
        writer.println("Validating target interns ...");

        UtilImpl_InternMap classNames = targets.getClassNameInternMap();

        writer.println("  Interned class names [ " + classNames.getSize() + " ]");

        validateInterns(writer, targets, classNames, ScanPolicy.SEED, targets.getSeedTable());
        validateInterns(writer, targets, classNames, ScanPolicy.PARTIAL, targets.getPartialTable());
        validateInterns(writer, targets, classNames, ScanPolicy.EXCLUDED, targets.getExcludedTable());
        validateInterns(writer, targets, classNames, ScanPolicy.EXTERNAL, targets.getExternalTable());

        writer.println("Validating target interns ... done");    
    }

    public void validateInterns(
        PrintWriter writer, AnnotationTargetsImpl_Targets targets,
        UtilImpl_InternMap classNames,
        ScanPolicy scanPolicy, TargetsTableImpl targetsData) {

        writer.println("  Validating [ " + scanPolicy + " ] data ...");

        validateInterns(
            writer, targets, classNames,
            scanPolicy, targetsData.i_getClassNames(),
            "Class names", "Class name [ {0} ] in [ {1} ] is not interned!");

        validateInterns(
            writer, targets, classNames,
            scanPolicy, targetsData.i_getPackageNames(),
            "Package names", "Package name [ {0} ] in [ {1} ] is not interned!");

        validateInterns(
            writer, targets, classNames,
            scanPolicy, targetsData.i_getClassAnnotationNames(),
            "Class annotation class names",
            "Class annotation class name [ {0} ] in [ {1} ] is not interned!");

        validateInterns(
            writer, targets, classNames,
            scanPolicy, targetsData.i_getPackageAnnotationNames(),
            "Package annotation class names",
            "Package annotation class name [ {0} ] in [ {1} ] is not interned!");

        writer.println("  Validating [ " + scanPolicy + " ] data ... done");
    }

    public void validateInterns(
        PrintWriter writer,
        AnnotationTargetsImpl_Targets targets,
        UtilImpl_InternMap i_classNames, 
        ScanPolicy scanPolicy, Set<String> i_names, String banner, String faultText) {

        writer.println("  Validating [ " + banner + " ] [ " + i_names.size() + " ]");

        for ( String i_name : i_names ) {
            if ( i_classNames.intern(i_name, Util_InternMap.DO_NOT_FORCE) == null ) {
                AnnotationTargets_Fault fault = targets.getFactory().createFault(
                    faultText,
                    new String[] { i_name, scanPolicy.name() });
                addFault(fault);
                writer.println(fault.getResolvedText());
            }
        }
    }

    public void validateAnnotations(PrintWriter writer, AnnotationTargetsImpl_Targets targets) {
        writer.println("Begin Validate Annotations");

        validateBidiMap(writer, "Package Annotations", targets, targets.i_getPackageAnnotations());
        validateBidiMap(writer, "Class Annotations", targets, targets.i_getClassAnnotations());
        validateBidiMap(writer, "Field Annotations", targets, targets.i_getFieldAnnotations());
        validateBidiMap(writer, "Method Annotations", targets, targets.i_getMethodAnnotations());

        writer.println("End Validate Annotations");
    }

    public void validateBidiMap(
        PrintWriter writer, String banner,
        AnnotationTargetsImpl_Targets targets,
        Util_BidirectionalMap map) {

        AnnotationTargetsImpl_Factory targetFactory = targets.getFactory();

        String holderTag = map.getHolderTag();
        String heldTag = map.getHeldTag();

        writer.println("Validating mapping [ " + banner + " ] Mapping [ " + holderTag + " ] to [ " + heldTag + " ] ...");

        Set<String> holderSet = map.getHolderSet();
        writer.println("Holders [ " + holderTag + " ] [ " + holderSet.size() + " ]");

        for ( String className : holderSet ) {
            Set<String> held = map.selectHeldOf(className);
            for ( String annotationClassName : held ) {
                if ( !map.selectHoldersOf(annotationClassName).contains(className) ) {
                    AnnotationTargets_Fault fault = targetFactory.createFault(
                        "Mapping [ {0} ] does not find reverse of holder [ {1} ] [ {2} ] to held [ {3} [ {4} ]",
                        new String[] { banner, holderTag, className, heldTag, annotationClassName });
                    addFault(fault);
                    writer.println(fault.getResolvedText());
                }
            }
        }

        Set<String> heldSet = map.getHeldSet();
        writer.println("Held [ " + heldTag + " ] [ " + heldSet.size() + " ]");

        for ( String annotationClassName : heldSet ) {
            Set<String> holders = map.selectHoldersOf(annotationClassName);
            for ( String className : holders ) {
                if ( !map.selectHeldOf(className).contains(annotationClassName) ) {
                    AnnotationTargets_Fault fault = targetFactory.createFault(
                        "Mapping [ {0} ] does not find forward of holder [ {1} ] [ {2} ] to held [ {3} [ {4} ]",
                        new String[] { banner, holderTag, className, heldTag, annotationClassName });
                    addFault(fault);
                    writer.println(fault.getResolvedText());
                }
            }
        }

        writer.println("Validating mapping [ " + banner + " ] Mapping [ " + holderTag + " ] to [ " + heldTag + " ] ... done");
    }

    public void validateClasses(
        PrintWriter writer,
        AnnotationTargetsImpl_Targets targets,
        String[] validClassNames, String[] nonValidClassNames,
        List<AnnotationTargets_Fault> useFaults) {

        AnnotationTargets_Factory targetFactory = targets.getFactory();

        writer.println("Validate [ " + validClassNames.length + " ] valid and [ " + nonValidClassNames.length + " ] non-valid classes ...");

        for ( String validClassName : validClassNames ) {
            if ( !isScannedClassName(targets, validClassName) ) {
                AnnotationTargets_Fault fault = targetFactory.createFault(
                    "Valid class [ {0} ] was not found!", validClassName);
                useFaults.add(fault);
                writer.println(fault.getResolvedText());
            }
        }

        for ( String nonValidClassName : nonValidClassNames ) {
            if ( isScannedClassName(targets, nonValidClassName) ) {
                AnnotationTargets_Fault fault = targetFactory.createFault(
                    "Non-valid class [ {0} ] was found!", nonValidClassName);
                useFaults.add(fault);
                writer.println(fault.getResolvedText());
            }
        }

        writer.println("Verifying [ " + validClassNames.length + " ] valid and [ " + nonValidClassNames.length + " ] non-valid classes ... done");
    }

    public static boolean isScannedClassName(AnnotationTargetsImpl_Targets targets, String className) {
        String i_className = targets.getClassNameInternMap().intern(className, Util_InternMap.DO_NOT_FORCE);
        if ( i_className == null ) {
            return false;
        }

        return ( targets.isSeedClassName(i_className) ||
                 targets.isPartialClassName(i_className) ||
                 targets.isExcludedClassName(i_className) ||
                 targets.isExternalClassName(i_className) );
    }
    
    public void validateSubclasses(PrintWriter writer, AnnotationTargetsImpl_Targets targets) {
        AnnotationTargets_Factory targetFactory = targets.getFactory();

        long startTime = displayStamp("Begin Validate subclasses", writer);

        long firstCallTime = -1;

        Set<String> alreadyProcessed = new HashSet<String>();
        int newProblemCount = 0;
        int processedSubclassCount = 0;

        //   getSuperclassNames() returns a Map of classes and their direct superclass:
        //       Key      Values
        //       -----     ------
        //       class   superclass
        //
        //   Iterate through the super classes (the values column).
        //   Verify each superclass has at least one subclass.
        //   The superclass may appear multiple times in the values column since
        //      multiple classes can have the same superclass. Skip duplicates.
        //   Get all subclasses of the superclass.  For each of the subclasses, verify
        //     that it is a descendant of the superclass.

        for ( String i_superclassName : targets.i_getSuperclassNames().values() ) {
            if ( alreadyProcessed.contains(i_superclassName) ) {
                continue;
            } else {
                alreadyProcessed.add(i_superclassName);
            }

            Set<String> subclassNameSet = targets.getSubclassNames(i_superclassName);

            if ( firstCallTime == -1 ) {
                firstCallTime = displayStamp(startTime, "First call subclass scan [ " + fullName + " ]", writer);
            }

            // Test for no subclasses.  Since "className" is a superclass it should have at least 1 subclass
            if (subclassNameSet.size() == 0) {
                AnnotationTargets_Fault fault = targetFactory.createFault(
                    "Class [ {0} ] has no subclasses!", i_superclassName);
                addFault(fault);
                writer.println(fault.getResolvedText());

                newProblemCount++;
                continue;
            }

            processedSubclassCount += subclassNameSet.size();
        }

        writer.println(
            "Processed [ " + alreadyProcessed.size() + " ] unique superclasses," +
            " and found [ " + processedSubclassCount + " ] subclasses." +
            "  Found [ " + newProblemCount + " ] problems.");

        displayStamp(startTime, "End validate subclasses", writer);
    }

    public void validateImplements(PrintWriter writer, AnnotationTargetsImpl_Targets targets) {
        AnnotationTargets_Factory targetFactory = targets.getFactory();

        long startTime = displayStamp("Begin Validate Implements", writer);

        int newProblemCount = 0;

        Set<String> seedClassNames = targets.getSeedClassNames();
        int numSeed = seedClassNames.size();

        Set<String> partialClassNames = targets.getPartialClassNames();
        int numPartial = partialClassNames.size();

        Set<String> excludedClassNames = targets.getExcludedClassNames();
        int numExcluded = excludedClassNames.size();

        Set<String> scannedClassNames = new HashSet<String>(numSeed + numPartial + numExcluded);
        scannedClassNames.addAll(seedClassNames);
        scannedClassNames.addAll(partialClassNames);
        scannedClassNames.addAll(excludedClassNames);

        Set<String> externalClassNames = targets.getExternalClassNames();

        writer.println("  Seed     [ " + seedClassNames.size() + " ]");
        writer.println("  Partial  [ " + partialClassNames.size() + " ]");
        writer.println("  Excluded [ " + excludedClassNames.size() + " ]");
        writer.println("  External [ " + externalClassNames.size() + " ]");

        for ( String scannedClassName : scannedClassNames ) {
            String[] interfaceNames = targets.getInterfaceNames(scannedClassName);
            if ( interfaceNames == null ) {
                continue;
            }

            for ( String interfaceName : interfaceNames ) {
                Set<String> implementorNames = targets.getAllImplementorsOf(interfaceName);
                if ( !implementorNames.contains(scannedClassName) ) {
                    AnnotationTargets_Fault fault = targetFactory.createFault(
                        "Class [ {0} ] with interface [ {1} ] is not in in the all implementors map!",
                        new String[] { scannedClassName, interfaceName });
                    addFault(fault);
                    writer.println(fault.getResolvedText());

                    newProblemCount++;
                }
            }
        }

        writer.println(
            "Processed [ " + scannedClassNames.size() + " ] classes." +
            "  Found [ " + newProblemCount + " ] problems.");

        displayStamp(startTime, "End Validate Implements", writer);
    }

    public void validateInfoStore(
        PrintWriter writer,
        AnnotationTargetsImpl_Targets targets,
        InfoStore infoStore) {

        AnnotationTargets_Factory targetFactory = targets.getFactory();

        writer.println("Begin Validate InfoStore");

        Set<String> packagesWithAnnotations = targets.getAnnotatedPackages();
        writer.println("Packages with annotations: [" + packagesWithAnnotations.size() + "]");

        for ( String packageName : packagesWithAnnotations ) {
            PackageInfo packageInfo = infoStore.getPackageInfo(packageName);
            if ( packageInfo == null ) {
                AnnotationTargets_Fault fault = targetFactory.createFault(
                        "Package [ {0} ] not found", new String[] { packageName });
                addFault(fault);
                writer.println(fault.getResolvedText());
                continue;
            }

            Collection<? extends AnnotationInfo> infoPackageAnnotations = packageInfo.getDeclaredAnnotations();

            Set<String> targetsPackageAnnotations = targets.getPackageAnnotations(packageName);

            if ( targetsPackageAnnotations.size() != infoPackageAnnotations.size() ) {
                AnnotationTargets_Fault fault = targetFactory.createFault(
                    "Package [ {0} ] target annotations [ {1} ] info annotations [ {2} ]",
                    new String[] { packageName, toString(targetsPackageAnnotations), toString(infoPackageAnnotations) });
                addFault(fault);
                writer.println(fault.getResolvedText());
            }

            for ( AnnotationInfo annotationInfo : infoPackageAnnotations ) {
                if ( !targets.i_packageHasAnnotation(packageName, annotationInfo.getAnnotationClassName()) ) {
                    AnnotationTargets_Fault fault = targetFactory.createFault(
                        "Package [ {0} ] annotation [ {1} ] not found in targets",
                        new String[] { packageName, annotationInfo.getAnnotationClassName() });
                    addFault(fault);
                    writer.println(fault.getResolvedText());
                }
            }
        }

        Set<String> classesWithAnnotations = targets.getAnnotatedClasses();
        writer.println("Classes with annotations: [" + classesWithAnnotations.size() + "]");

        for ( String className : classesWithAnnotations ) {
            ClassInfo classInfo = infoStore.getDelayableClassInfo(className);
            if ( classInfo == null ) {
                AnnotationTargets_Fault fault = targetFactory.createFault(
                        "Class [ {0} ] not found", new String[] { className });
                addFault(fault);
                writer.println(fault.getResolvedText());
                continue;
            }

            Collection<? extends AnnotationInfo> annotationsInClass = classInfo.getDeclaredAnnotations();
            for ( AnnotationInfo annotation : annotationsInClass ) {
                if ( !targets.i_classHasAnnotation(className, annotation.getAnnotationClassName()) ) {
                    AnnotationTargets_Fault fault = targetFactory.createFault(
                        "Class [ {0} ] annotation [ {1} ] not found in targets",
                        new String[] { className, annotation.getAnnotationClassName() });
                    addFault(fault);
                    writer.println(fault.getResolvedText());
                }
            }
        }

        Set<String> classesWithMethodAnnotations = targets.getClassesWithMethodAnnotations();
        writer.println("Classes with method annotations: [ " + classesWithMethodAnnotations.size() + " ]");

        for ( String className : classesWithMethodAnnotations ) {
            ClassInfo classInfo = infoStore.getDelayableClassInfo(className);
            Collection<? extends MethodInfo> methods = classInfo.getDeclaredMethods();
            for ( MethodInfo method : methods ) {
                Collection<? extends AnnotationInfo> annotationsInClass = method.getDeclaredAnnotations();
                for ( AnnotationInfo annotation : annotationsInClass ) {
                    if ( !targets.i_classHasMethodAnnotation(className, annotation.getAnnotationClassName()) ) {
                        AnnotationTargets_Fault fault = targetFactory.createFault(
                            "Class [ {0} ] method [ {1} ] annotation [ {2} ] not found in targets",
                            new String[] { className, method.getName(), annotation.getAnnotationClassName() });
                        addFault(fault);
                        writer.println(fault.getResolvedText());
                    }
                }
            }
        }

        Set<String> classesWithFieldAnnotations = targets.getClassesWithFieldAnnotations();
        writer.println("Classes with field annotations: [" + classesWithFieldAnnotations.size() + "]");

        for ( String className : classesWithFieldAnnotations ) {
            ClassInfo classInfo = infoStore.getDelayableClassInfo(className);
            Collection<? extends FieldInfo> fields = classInfo.getDeclaredFields();
            for ( FieldInfo field : fields ) {
                Collection<? extends AnnotationInfo> annotationsInClass = field.getDeclaredAnnotations();
                for ( AnnotationInfo annotation : annotationsInClass ) {
                    if ( !targets.i_classHasFieldAnnotation(className, annotation.getAnnotationClassName()) ) {
                        AnnotationTargets_Fault fault = targetFactory.createFault(
                            "Class [ {0} ] field [ {1} ] annotation [ {2} ] not found in targets",
                            new String[] { className, field.getName(), annotation.getAnnotationClassName() });
                        addFault(fault);
                        writer.println(fault.getResolvedText());
                    }
                }
            }
        }

        writer.println("End Validate InfoStore");
    }

    public static String toString(Collection<? extends Object> values) {
        return TestUtils.toString(values);
    }

    public long displayStamp(String banner, PrintWriter writer) {
        long endTime = getTime();
        writer.println(banner + " [ ****** ] [ " + endTime + " ]");
        return endTime;
    }

    public long displayStamp(long startTime, String banner, PrintWriter writer) {
        long endTime = getTime();
        writer.println(banner + " [ " + startTime + " ] [ " + endTime + " ] [ " + (endTime - startTime) + " ]");
        return endTime;
    }
}

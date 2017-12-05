package com.ibm.ws.anno.targets.internal;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.util.Util_BidirectionalMap;
import com.ibm.wsspi.anno.util.Util_InternMap;

public class Utility_Validation {

    public static void addFault(
        String valueName, int oldValue, int newValue,
        List<String> faults) {

        if ( oldValue == newValue ) {
            return;
        }

        String message = valueName + " error: " +
                         " Old [ " + Integer.toString(oldValue) + " ]" +
                         " New [ " + Integer.toString(newValue) + " ]";

        faults.add(message);
    }

    public static void compare(
            AnnotationTargetsImpl_Targets annotationTargets1,
            AnnotationTargetsImpl_Targets annotationTargets2,
            List<String> faults) {

        compareAnnotationCounts(annotationTargets1, annotationTargets2, faults);
        compareInternCounts(annotationTargets1, annotationTargets2, faults);
    }

//    public static void compareFileCounts(ClassSource_Aggregate classSource) {
//        int newResourceExclusionCount = classSource.getResourceExclusionCount();
//        addVerificationFault("resourceExclusionCount", resourceExclusionCount, newResourceExclusionCount);
//
//        int newClassExclusionCount = classSource.getClassExclusionCount();
//        addVerificationFault("classExclusionCount", classExclusionCount, newClassExclusionCount );
//
//        int newClassInclusionCount = classSource.getClassInclusionCount();
//        addVerificationFault("classInclusionCount", classInclusionCount, newClassInclusionCount );
//    }

    public static void compareAnnotationCounts(
        AnnotationTargetsImpl_Targets annotationTargets1,
        AnnotationTargetsImpl_Targets annotationTargets2,
        List<String> faults) {

        Util_BidirectionalMap packageData1 = annotationTargets1.getPackageAnnotationData();
        int uniquePackages1 = packageData1.getHolderSet().size();
        int uniquePackageAnnotations1 = packageData1.getHeldSet().size();
        
        Util_BidirectionalMap packageData2 = annotationTargets2.getPackageAnnotationData();        
        int uniquePackages2 = packageData2.getHolderSet().size();        
        int uniquePackageAnnotations2 = packageData2.getHeldSet().size();

        addFault(
            "uniquePackages",
            uniquePackages1, uniquePackages2,
            faults);
        
        addFault(
            "uniquePackageAnnotations",
            uniquePackageAnnotations1, uniquePackageAnnotations2,
            faults);

        //

        Util_BidirectionalMap classData1 = annotationTargets1.getClassAnnotationData();
        int uniqueClasses1 = classData1.getHolderSet().size();
        int uniqueClassAnnotations1 = classData1.getHeldSet().size();
        
        Util_BidirectionalMap fieldData1 = annotationTargets1.getFieldAnnotationData();
        int uniqueClassesWithFieldAnnotations1 = fieldData1.getHolderSet().size();
        int uniqueFieldAnnotations1 = fieldData1.getHeldSet().size();
        
        Util_BidirectionalMap methodData1 = annotationTargets1.getMethodAnnotationData();
        int uniqueClassesWithMethodAnnotations1 = methodData1.getHolderSet().size();
        int uniqueMethodAnnotations1 = methodData1.getHeldSet().size();

        Util_BidirectionalMap classData2 = annotationTargets2.getClassAnnotationData();
        int uniqueClasses2 = classData2.getHolderSet().size();
        int uniqueClassAnnotations2 = classData2.getHeldSet().size();
        
        Util_BidirectionalMap fieldData2 = annotationTargets2.getFieldAnnotationData();
        int uniqueClassesWithFieldAnnotations2 = fieldData2.getHolderSet().size();
        int uniqueFieldAnnotations2 = fieldData2.getHeldSet().size();
        
        Util_BidirectionalMap methodData2 = annotationTargets2.getMethodAnnotationData();
        int uniqueClassesWithMethodAnnotations2 = methodData2.getHolderSet().size();
        int uniqueMethodAnnotations2 = methodData2.getHeldSet().size();

        addFault(
            "uniqueClasses",
            uniqueClasses1, uniqueClasses2,
            faults);
        addFault(
            "uniqueClassAnnotations",
            uniqueClassAnnotations1, uniqueClassAnnotations2,
            faults);

        addFault(
            "uniqueClassesWithFieldAnnotations",
            uniqueClassesWithFieldAnnotations1, uniqueClassesWithFieldAnnotations2,
            faults);
        addFault(
            "uniqueFieldAnnotations",
            uniqueFieldAnnotations1, uniqueFieldAnnotations2,
            faults);

        addFault(
            "uniqueClassesWithMethodAnnotations",
            uniqueClassesWithMethodAnnotations1, uniqueClassesWithMethodAnnotations2,
            faults);
        addFault(
            "uniqueMethodAnnotations",
            uniqueMethodAnnotations1, uniqueMethodAnnotations2,
            faults);
    }

    public static void compareInternCounts(
        AnnotationTargetsImpl_Targets annotationTargets1,
        AnnotationTargetsImpl_Targets annotationTargets2,
        List<String> faults) {

        Util_InternMap classInternMap1 = annotationTargets1.getClassInternMap();
        int classInternCount1 = classInternMap1.getSize();
        int classInternTotalSize1 = classInternMap1.getTotalLength();

        Util_InternMap classInternMap2 = annotationTargets2.getClassInternMap();
        int classInternCount2 = classInternMap2.getSize();
        int classInternTotalSize2 = classInternMap2.getTotalLength();        

        addFault(
            "classInternCount",
            classInternCount1, classInternCount2,
            faults);
        addFault(
            "classInternTotalSize",
            classInternTotalSize1, classInternTotalSize2,
            faults);
    }

    //

    public static void display(
        String targetName,
        AnnotationTargetsImpl_Targets targets,
        PrintWriter writer) {

        writer.println("============================================================");
        writer.println("Target [ " + targetName + " ]");
        // writer.println("------------------------------------------------------------");
        // displayClassSourceData(targets, writer);
        writer.println("------------------------------------------------------------");
        displayTargetsData(targets, writer);
        writer.println("------------------------------------------------------------");
        displayInternCounts(targets, writer);
        writer.println("============================================================");
    }

//    public static void displayClassSourceData(
//        ClassSource_Aggregate classSource,
//        PrintWriter writer) {
//
//        writer.println("Class source data:");
//        writer.println("  Classes scanned [ " + Integer.toString(classInclusionCount) + " ]");
//        writer.println("  Classes masked  [ " + Integer.toString(classExclusionCount) + " ]");
//        writer.println("  Non-class files [ " + Integer.toString(resourceExclusionCount) + " ]");
//    }

    public static void displayTargetsData(
        AnnotationTargetsImpl_Targets annotationTargets,
        PrintWriter writer) {

        Util_BidirectionalMap packageData = annotationTargets.getPackageAnnotationData();
        int uniquePackages = packageData.getHolderSet().size();
        int uniquePackageAnnotations = packageData.getHeldSet().size();

        Util_BidirectionalMap classData = annotationTargets.getClassAnnotationData();
        int uniqueClasses = classData.getHolderSet().size();
        int uniqueClassAnnotations = classData.getHeldSet().size();
        
        Util_BidirectionalMap fieldData = annotationTargets.getFieldAnnotationData();
        int uniqueClassesWithFieldAnnotations = fieldData.getHolderSet().size();
        int uniqueFieldAnnotations = fieldData.getHeldSet().size();
        
        Util_BidirectionalMap methodData = annotationTargets.getMethodAnnotationData();
        int uniqueClassesWithMethodAnnotations = methodData.getHolderSet().size();
        int uniqueMethodAnnotations = methodData.getHeldSet().size();

        writer.println("Targets data:");

        writer.println("  Packages with annotations       [ " + Integer.toString(uniquePackages) + " ]");
        writer.println("  Unique packages annotations     [ " + Integer.toString(uniquePackageAnnotations) + " ]");

        writer.println("  Classes with class annotations  [ " + Integer.toString(uniqueClasses) + " ]");
        writer.println("  Unique class annotations        [ " + Integer.toString(uniqueClassAnnotations) + " ]");

        writer.println("  Classes with field annotations  [ " + Integer.toString(uniqueClassesWithFieldAnnotations) + " ]");
        writer.println("  Unique field annotations        [ " + Integer.toString(uniqueFieldAnnotations) + " ]");

        writer.println("  Classes with method annotations [ " + Integer.toString(uniqueClassesWithMethodAnnotations) + " ]");
        writer.println("  Unique method annotations       [ " + Integer.toString(uniqueMethodAnnotations) + " ]");
    }

    public static void displayInternCounts(
        AnnotationTargetsImpl_Targets annotationTargets,
        PrintWriter writer) {

        writer.println("Intern map data:");

        Util_InternMap classInternMap = annotationTargets.getClassInternMap();
        int classInternCount = classInternMap.getSize();
        int classInternTotalSize = classInternMap.getTotalLength();

        writer.println("  Count of classes [ " + Integer.toString(classInternCount) + " ]");
        writer.println("  Size of classes  [ " + Integer.toString(classInternTotalSize) + " ]");
    }

    public static void displayFaults(List<String> faults, PrintWriter writer) {
        writer.println("Verification errors     [ " + faults.size() + " ]");
        for ( String fault : faults ) {
            writer.println("  [ " + fault + " ]");
        }
    }

    //

    public static void validateClassDetail(
        AnnotationTargetsImpl_Targets targets1,
        AnnotationTargetsImpl_Targets targets2,
        List<String> faults) {

        Set<String> classNames_only1 = new HashSet<String>();
        Set<String> classNames_only2 = new HashSet<String>();
        Set<String> classNames_both = new HashSet<String>();

        Set<String> interfaceNames_only1 = new HashSet<String>();
        Set<String> interfaceNames_only2 = new HashSet<String>();
        Set<String> interfaceNames_both = new HashSet<String>();

        Set<String> classAnnotations_only1 = new HashSet<String>();
        Set<String> classAnnotations_only2 = new HashSet<String>();
        Set<String> classAnnotations_both = new HashSet<String>();

        Set<String> fieldAnnotations_only1 = new HashSet<String>();
        Set<String> fieldAnnotations_only2 = new HashSet<String>();
        Set<String> fieldAnnotations_both = new HashSet<String>();

        Set<String> methodAnnotations_only1 = new HashSet<String>();
        Set<String> methodAnnotations_only2 = new HashSet<String>();
        Set<String> methodAnnotations_both = new HashSet<String>();

        for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
            Set<String> classNames_1 = targets1.getClassNames( scanPolicy.getValue() );
            Set<String> classNames_2 = targets2.getClassNames( scanPolicy.getValue() );            

            classNames_only1.clear();
            classNames_only2.clear();
            classNames_both.clear();

            symmetricDifference(
                classNames_1, classNames_2,
                classNames_only1, classNames_only2, classNames_both);

            for ( String className_only1 : classNames_only1 ) {
                faults.add("Class [ " + scanPolicy + " ] [ " + className_only1 + " ]: Only in first results");
            }
            for ( String className_only2 : classNames_only2 ) {
                faults.add("Class [ " + scanPolicy + " ] [ " + className_only2 + " ]: Only in second results");
            }

            for ( String className_both : classNames_both ) {
                String prefix = "Class [ " + scanPolicy + " ] [ " + className_both + " ]";

                String superClassName_1 = targets1.getSuperclassName(className_both);
                String superClassName_2 = targets2.getSuperclassName(className_both);

                boolean superClassMismatch;
                if ( superClassName_1 != null ) {
                    if ( superClassName_2 == null ) {
                        superClassMismatch = true;
                    } else if ( !superClassName_1.equals(superClassName_2) ) {
                        superClassMismatch = true;
                    } else {
                        superClassMismatch = false;
                    }
                } else if ( superClassName_2 != null ) {
                    superClassMismatch = true;
                } else {
                    superClassMismatch = false;
                }

                if ( superClassMismatch ) {
                    faults.add( prefix + ": " +
                                " First superclass [ " + superClassName_1 + " ]" +
                                " Second superclass [ " + superClassName_2 + " ]" );
                }

                String[] interfaceNames_1 = targets1.getInterfaceNames(className_both);
                String[] interfaceNames_2 = targets2.getInterfaceNames(className_both);

                interfaceNames_only1.clear();
                interfaceNames_only2.clear();
                interfaceNames_both.clear();

                symmetricDifference(
                    interfaceNames_1, interfaceNames_2,
                    interfaceNames_only1, interfaceNames_only2, interfaceNames_both);

                for ( String interfaceName_only1 : interfaceNames_only1 ) {
                    faults.add( prefix + " Interface [ " + interfaceName_only1 + " ]: Only in first results" );
                }
                for ( String interfaceName_only2 : interfaceNames_only2 ) {
                    faults.add( prefix + " Interface [ " + interfaceName_only2 + " ]: Only in second results" );
                }

                Set<String> classAnnotations_1 = targets1.getClassAnnotations(className_both);
                Set<String> classAnnotations_2 = targets2.getClassAnnotations(className_both);

                classAnnotations_only1.clear();
                classAnnotations_only2.clear();
                classAnnotations_both.clear();

                symmetricDifference(
                    classAnnotations_1, classAnnotations_2,
                    classAnnotations_only1, classAnnotations_only2, classAnnotations_both);

                for ( String classAnnotation_only1 : classAnnotations_only1 ) {
                    faults.add( prefix + " Class Annotation [ " + classAnnotation_only1 + " ]: Only in first results" );
                }
                for ( String classAnnotation_only2 : classAnnotations_only2 ) {
                    faults.add( prefix + " Class Annotation [ " + classAnnotation_only2 + " ]: Only in second results" );
                }

                Set<String> fieldAnnotations_1 = targets1.getFieldAnnotations(className_both);
                Set<String> fieldAnnotations_2 = targets2.getFieldAnnotations(className_both);

                fieldAnnotations_only1.clear();
                fieldAnnotations_only2.clear();
                fieldAnnotations_both.clear();

                symmetricDifference(
                    fieldAnnotations_1, fieldAnnotations_2,
                    fieldAnnotations_only1, fieldAnnotations_only2, fieldAnnotations_both);

                for ( String fieldAnnotation_only1 : fieldAnnotations_only1 ) {
                    faults.add( prefix + " Field Annotation [ " + fieldAnnotation_only1 + " ]: Only in first results" );
                }
                for ( String fieldAnnotation_only2 : fieldAnnotations_only2 ) {
                    faults.add( prefix + " Field Annotation [ " + fieldAnnotation_only2 + " ]: Only in second results" );
                }

                Set<String> methodAnnotations_1 = targets1.getFieldAnnotations(className_both);
                Set<String> methodAnnotations_2 = targets2.getFieldAnnotations(className_both);

                methodAnnotations_only1.clear();
                methodAnnotations_only2.clear();
                methodAnnotations_both.clear();

                symmetricDifference(
                    methodAnnotations_1, methodAnnotations_2,
                    methodAnnotations_only1, methodAnnotations_only2, methodAnnotations_both);

                for ( String methodAnnotation_only1 : methodAnnotations_only1 ) {
                    faults.add( prefix + " Method Annotation [ " + methodAnnotation_only1 + " ]: Only in first results" );
                }
                for ( String methodAnnotation_only2 : methodAnnotations_only2 ) {
                    faults.add( prefix + " Method Annotation [ " + methodAnnotation_only2 + " ]: Only in second results" );
                }
            }
        }

        Set<String> unresolvedClassNames_1 = targets1.getUnresolvedClassNames();
        Set<String> unresolvedClassNames_2 = targets2.getUnresolvedClassNames();

        Set<String> unresolvedClassNames_only1 = new HashSet<String>();
        Set<String> unresolvedClassNames_only2 = new HashSet<String>();
        Set<String> unresolvedClassNames_both = new HashSet<String>();

        symmetricDifference(
            unresolvedClassNames_1, unresolvedClassNames_2,
            unresolvedClassNames_only1, unresolvedClassNames_only2, unresolvedClassNames_both);

        for ( String unresolvedClassName_only1 : unresolvedClassNames_only1 ) {
            faults.add("Unresolved [ " + unresolvedClassName_only1 + " ] only in first results");
        }
        for ( String unresolvedClassName_only2 : unresolvedClassNames_only2 ) {
            faults.add("Unresolved [ " + unresolvedClassName_only2 + " ] only in second results");
        }

        Set<String> referencedClassNames_1 = targets1.getReferencedClassNames();
        Set<String> referencedClassNames_2 = targets2.getReferencedClassNames();

        Set<String> referencedClassNames_only1 = new HashSet<String>();
        Set<String> referencedClassNames_only2 = new HashSet<String>();
        Set<String> referencedClassNames_both = new HashSet<String>();

        symmetricDifference(
                referencedClassNames_1, referencedClassNames_2,
                referencedClassNames_only1, referencedClassNames_only2, referencedClassNames_both);

        for ( String referencedClassName_only1 : referencedClassNames_only1 ) {
            faults.add("Referenced [ " + referencedClassName_only1 + " ] only in first results");
        }
        for ( String referencedClassName_only2 : referencedClassNames_only2 ) {
            faults.add("Referenced [ " + referencedClassName_only2 + " ] only in second results");
        }
    }

    public static void symmetricDifference(
        Set<String> values_1, Set<String> values_2,
        Set<String> values_only1, Set<String> values_only2, Set<String> values_both) {

        for ( String value_1 : values_1 ) {
            if ( values_2.contains(value_1) )  {
                values_both.add(value_1);
            } else {
                values_only1.add(value_1);
            }
        }

        for ( String value_2 : values_2 ) {
            if ( !values_1.contains(value_2) )  {
                values_only2.add(value_2);
            } else {
                // Ignore: Already added by the values_1 loop.
            }
        }
    }

    public static void symmetricDifference(
        String[] values_1, String[] values_2,
        Set<String> values_only1, Set<String> values_only2, Set<String> values_both) {

        for ( String value_1 : values_1 ) {
            if ( contains( values_2, value_1) )  {
                values_both.add(value_1);
            } else {
                values_only1.add(value_1);
            }
        }

        for ( String value_2 : values_2 ) {
            if ( !contains(values_1, value_2) )  {
                values_only2.add(value_2);
            } else {
                // Ignore: Already added by the values_1 loop.
            }
        }
    }

    public static boolean contains(String[] values, String value) {
        for ( String candidateValue : values ) {
            if ( candidateValue.equals(value) ) {
                return true;
            }
        }
        return false;
    }
}


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

package com.ibm.ws.anno.test.cases;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_EJB;
import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Targets;
import com.ibm.ws.anno.test.data.AnnotationTest_SubclassMap_jar_Data;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Fault;

public class AnnotationTest_SubclassMap extends AnnotationTest_BaseClass {

    @Override
    public ClassSource_Specification_Direct_EJB createClassSourceSpecification() {
        return AnnotationTest_SubclassMap_jar_Data.createClassSourceSpecification(getClassSourceFactory(),
                                                                                  getProjectPath(),
                                                                                  getDataPath());
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(); // throws Exception

        setDataPath(AnnotationTest_SubclassMap_jar_Data.EAR_NAME);
    }

    //

    @Override
    public String getTargetName() {
        return AnnotationTest_SubclassMap_jar_Data.EJBJAR_NAME;
    }

    @Override
    public int getIterations() {
        return 5;
    }

    // Flag used to set the baseline results.  When set, the first run will store the
    // annotation targets to the specified storage location.
    //
    // Set this once to compute and store a baseline.  Then copy the baseline into the main
    // publish location, and change the value back to false.

    //    @Override
    //    public boolean getSeedStorage() {
    //        return true;
    //    }

    //

    @Test
    public void testAnnotationTest_SubclassMap_nodetail() throws Exception {
        runScanTest(DETAIL_IS_NOT_ENABLED,
                    getStoragePath(COMMON_TEMP_STORAGE_PATH), STORAGE_NAME_NO_DETAIL,
                    getSeedStorage(), getStoragePath(COMMON_STORAGE_PATH), STORAGE_NAME_NO_DETAIL,
                    new PrintWriter(System.out, true));
    }

    @Test
    public void testAnnotationTest_SubclassMap_JAR_detail() throws Exception {
        runScanTest(DETAIL_IS_ENABLED,
                    getStoragePath(COMMON_TEMP_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    getSeedStorage(), getStoragePath(COMMON_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    new PrintWriter(System.out, true));
    }

    //  The test data class hierarchy:
    //
    //                       Object
    //                         |
    //                         A
    //                        /  \
    //                       /    \
    //                      /      \
    //                     /        \
    //                    /          \
    //                   B            C
    //                 /   \         /  \
    //                /     \       /    \
    //               D       E     F      G
    //              / \       \
    //             /   \       \
    //            H     I       J

    protected Map<String, Set<String>> classToSubclassesMap = null;
    protected Map<String, String> trackExpectedClassesFoundMap = null;

    protected void initClassHierarchy() {
        final String packageName = "subclassMapTest";

        // abbreviations for bull class names
        final String A = packageName + ".A";
        final String B = packageName + ".B";
        final String C = packageName + ".C";
        final String D = packageName + ".D";
        final String E = packageName + ".E";
        final String F = packageName + ".F";
        final String G = packageName + ".G";
        final String H = packageName + ".H";
        final String I = packageName + ".I";
        final String J = packageName + ".J";
        final String OBJECT = "java.lang.Object";

        final String classesWithSubclasses[] = { OBJECT, A, B, C, D, E };

        // See drawing of hierarchy
        // Note the additions of the annotation types as subclasses.
        // The new scans of extra referenced classes adds these.
        final String Object_Subclasses[] = { A, B, C, D, E, F, G, H, I, J,
                                            "javax.servlet.annotation.ServletSecurity", // 'javax.*' and 'java.*' cases were added
                                            "java.lang.annotation.Annotation", // by the extended referenced classes scan.
                                            "java.lang.annotation.Documented",
                                            "java.lang.annotation.Inherited",
                                            "java.lang.annotation.Retention",
                                            "java.lang.annotation.Target" };

        final String A_Subclasses[] = { B, C, D, E, F, G, H, I, J };
        final String B_Subclasses[] = { D, E, H, I, J };
        final String C_Subclasses[] = { F, G };
        final String D_Subclasses[] = { H, I };
        final String E_Subclasses[] = { J };

        // For those classes that have subclasses, put them in the map
        classToSubclassesMap = new HashMap<String, Set<String>>();
        classToSubclassesMap.put(A, new HashSet<String>(Arrays.asList(A_Subclasses)));
        classToSubclassesMap.put(B, new HashSet<String>(Arrays.asList(B_Subclasses)));
        classToSubclassesMap.put(C, new HashSet<String>(Arrays.asList(C_Subclasses)));
        classToSubclassesMap.put(D, new HashSet<String>(Arrays.asList(D_Subclasses)));
        classToSubclassesMap.put(E, new HashSet<String>(Arrays.asList(E_Subclasses)));
        classToSubclassesMap.put(OBJECT, new HashSet<String>(Arrays.asList(Object_Subclasses)));

        trackExpectedClassesFoundMap = new HashMap<String, String>();
        for (int i = 0; i < classesWithSubclasses.length; i++) {
            trackExpectedClassesFoundMap.put(classesWithSubclasses[i], "not found");
        }

    }

    /**
     * Verify the subclasses found for a class match the known subclasses of the class.
     * 
     * @param className The class name
     * @param subclassNameSet The set of subclasses that are found for the class.
     * @param writer something to write with
     * @return
     */
    protected boolean subclassesAreCorrect(String className, HashSet<String> subclassNameSet, PrintWriter writer) {
        if (null == subclassNameSet) {
            writer.println("Unexpected conditition: [ " + className + " ] has no subclasses");
            return false;
        }

        HashSet<String> a = (HashSet<String>) classToSubclassesMap.get(className);
        if (a.equals(subclassNameSet)) {
            return true;
        } else {
            writer.println("Expected subclasses of [ " + className + " ]");
            writer.println("    " + a.toString());
            writer.println("But actually found these subclasses:");
            writer.println("    " + subclassNameSet.toString());
            return false;
        }

    }

    @Override
    protected void verifySubclassMap(PrintWriter writer, AnnotationTargetsImpl_Targets annotationTargets, AnnotationTest_TestResult scanResult) {
        super.verifySubclassMap(writer, annotationTargets, scanResult);
        writer.println("Validating subclass map using known class hierarchy ...");
        initClassHierarchy();

        //   getSuperclassNames() returns a Map of classes and their direct superclass:
        //                                    Key      Values
        //                                   -----     ------
        //                                   class   superclass
        //   Iterate through the super classes (the values column).
        //   Verify each superclass has at least one subclass.

        Set<String> alreadyProcessed = new HashSet<String>();

        for (String className : annotationTargets.getSuperclassNames().values()) {

            if (alreadyProcessed.contains(className)) {
                continue;
            } else {
                alreadyProcessed.add(className);
            }

            if (!trackExpectedClassesFoundMap.containsKey(className)) {
                AnnotationTargets_Fault mapFault = createFault(" Was not expecting [ {0} ] to have any subclasses, but it does",
                                                               new String[] { className });
                scanResult.addVerificationMessage(mapFault);
                writer.println("Mapping fault: [ " + mapFault.getResolvedText() + " ]");
            } else {
                trackExpectedClassesFoundMap.put(className, "found");
            }

            HashSet<String> subclassNameSet = new HashSet<String>(annotationTargets.getSubclassNames(className));

            // Verify that the subclass name set for this class matches the set of know subclasses of the class
            if (!subclassesAreCorrect(className, subclassNameSet, writer)) {
                AnnotationTargets_Fault mapFault = createFault("List of subclasses is not correct for [ {0} ]",
                                                               new String[] { className });
                scanResult.addVerificationMessage(mapFault);
                writer.println("Mapping fault: [ " + mapFault.getResolvedText() + " ]");
            }
        }

        // Verify that we found all of the classes known to have subclasses
        for (String className : trackExpectedClassesFoundMap.keySet()) {
            if (!trackExpectedClassesFoundMap.get(className).equals("found")) {
                AnnotationTargets_Fault mapFault = createFault(" Was expecting [ {0} ] to have any subclasses, but it does not",
                                                               new String[] { className });
                scanResult.addVerificationMessage(mapFault);
                writer.println("Mapping fault: [ " + mapFault.getResolvedText() + " ]");
            }
        }

        writer.println("Validating subclass map using known class hierarchy ... done");
    }
}

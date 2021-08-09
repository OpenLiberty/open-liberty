/*******************************************************************************
 * Copyright (c) 2011,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.test.scan.basic;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct_EJB;
import com.ibm.ws.annocache.targets.internal.AnnotationTargetsImpl_Targets;
import com.ibm.ws.annocache.test.scan.Test_Base;
import com.ibm.ws.annocache.test.utils.TestUtils;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Fault;

public class Test_Subclass_Test extends Test_Base {

    @Override
    public ClassSourceImpl_Specification_Direct_EJB createClassSourceSpecification(ClassSourceImpl_Factory factory) {
        return Test_Subclass_Data.createClassSourceSpecification(factory);
    }

    //

    @Override
    public String getAppName() {
        return Test_Subclass_Data.EAR_NAME;
    }

    @Override
    public String getAppSimpleName() {
        return Test_Subclass_Data.EAR_SIMPLE_NAME;
    }

    @Override
    public String getModName() {
        return Test_Subclass_Data.EJBJAR_NAME;
    }

    @Override
    public String getModSimpleName() {
        return Test_Subclass_Data.EJBJAR_SIMPLE_NAME;
    }

    //

    @Test
    public void testSubclassMap() throws Exception {
        runSuiteTest( getBaseCase() ); // 'runSuiteTest' throws Exception
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

    protected static final Map<String, Set<String>> subclassNames = initSubclassNames();

    public static final String TEST_PACKAGE_NAME = "com.ibm.ws.annocache.test.classes.subclasses.";

    public Set<String> selectTestClassNames(Set<String> classNames) {
    	Set<String> selectedClassNames = new HashSet<String>();
    	
    	for ( String className : classNames ) {
    		if ( className.equals("java.lang.Object") ||
    		     className.startsWith(TEST_PACKAGE_NAME) ) {
    			selectedClassNames.add(className);
    		}
    	}
    	
    	return selectedClassNames;
    }
    
    protected static Map<String, Set<String>> initSubclassNames() {
        String OBJECT = "java.lang.Object";

        String A = TEST_PACKAGE_NAME + "A";
        String B = TEST_PACKAGE_NAME + "B";
        String C = TEST_PACKAGE_NAME + "C";
        String D = TEST_PACKAGE_NAME + "D";
        String E = TEST_PACKAGE_NAME + "E";
        String F = TEST_PACKAGE_NAME + "F";
        String G = TEST_PACKAGE_NAME + "G";
        String H = TEST_PACKAGE_NAME + "H";
        String I = TEST_PACKAGE_NAME + "I";
        String J = TEST_PACKAGE_NAME + "J";

        String object_Subclasses[] = { A, B, C, D, E, F, G, H, I, J };

        // Note the additions of the annotation types as subclasses.
        // The new scans of extra referenced classes adds these.
        // Partial list of additional classes picked up by the scan.
        // "javax.servlet.annotation.ServletSecurity", // 'javax.*' and 'java.*' cases were added
        // "java.lang.annotation.Annotation", // by the extended referenced classes scan.
        // "java.lang.annotation.Documented",
        // "java.lang.annotation.Inherited",
        // "java.lang.annotation.Retention",
        // "java.lang.annotation.Target" };

        String a_Subclasses[] = { B, C, D, E, F, G, H, I, J };
        String b_Subclasses[] = { D, E, H, I, J };
        String c_Subclasses[] = { F, G };
        String d_Subclasses[] = { H, I };
        String e_Subclasses[] = { J };

        Map<String, Set<String>> useSubclasses = new HashMap<String, Set<String>>();
        useSubclasses.put(OBJECT, asSet(object_Subclasses));
        useSubclasses.put(A, asSet(a_Subclasses));
        useSubclasses.put(B, asSet(b_Subclasses));
        useSubclasses.put(C, asSet(c_Subclasses));
        useSubclasses.put(D, asSet(d_Subclasses));
        useSubclasses.put(E, asSet(e_Subclasses));

        return useSubclasses;
    }

    public static Set<String> asSet(String[] values) {
    	return TestUtils.asSet(values);
    }

    @Test
    public void verifySubclasses() {
        println("Begin Verify Subclasses");

        AnnotationTargetsImpl_Targets useBaseTargets = getBaseTargets();
        
    	List<AnnotationTargets_Fault> faults = new LinkedList<AnnotationTargets_Fault>();

        Set<String> expectedSuperclassNames =
        	subclassNames.keySet();
        Set<String> i_actualSubclassNames =
        	selectTestClassNames( useBaseTargets.i_getSuperclassNames().keySet() );
        Set<String> i_actualSuperclassNames = new HashSet<String>();
        for ( String i_subclassName : i_actualSubclassNames ) {
        	i_actualSuperclassNames.add( useBaseTargets.i_getSuperclassName(i_subclassName) );
        }

        for ( String i_superclassName : i_actualSuperclassNames ) {
            if ( !verifySubclasses(i_superclassName) ) {
                AnnotationTargets_Fault fault =
                    createFault("Class [ {0} ] has inconsistent subclass data", i_superclassName);
                faults.add(fault);
                println("Fault: [ " + fault.getResolvedText() + " ]");
            }
        }

        if ( !expectedSuperclassNames.equals(i_actualSuperclassNames) ) {
            println("Expected superclasses [ " + toString(expectedSuperclassNames) + " ]");
            println("Actual subclasses [ " + toString(i_actualSuperclassNames) + " ]");

        	AnnotationTargets_Fault fault = createFault(
        		"Expected superclasses [ {0} ] Actual superclasses [ {1} ]",
        		new String[] { Integer.toString(expectedSuperclassNames.size()),
        				       Integer.toString(i_actualSuperclassNames.size()) });
            faults.add(fault);
        	println("Fault: [ " + fault.getResolvedText() + " ]");
        }

        Assert.assertTrue("No faults", faults.isEmpty());

        println("End Verify Subclasses");
    }

    protected boolean verifySubclasses(String superclassName) {
    	Set<String> actualSubclassNames = getBaseTargets().getSubclassNames(superclassName);
    	if ( actualSubclassNames == null ) {
    		actualSubclassNames = Collections.emptySet();
    	}

    	actualSubclassNames = selectTestClassNames(actualSubclassNames);

        Set<String> expectedSubclassNames = subclassNames.get(superclassName);
        if ( expectedSubclassNames == null ) {
        	expectedSubclassNames = Collections.emptySet();
        }

        if ( expectedSubclassNames.equals(actualSubclassNames) ) {
            return true;

        } else {
            println("Class [ " + superclassName + " ]");
            println("Expected subclasses [ " + toString(expectedSubclassNames) + " ]");
            println("Actual subclasses [ " + toString(actualSubclassNames) + " ]");
            return false;
        }
    }
}

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

import java.io.PrintStream;
import java.util.Set;

import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Targets;
import com.ibm.wsspi.anno.util.Util_BidirectionalMap;

public class AnnotationTest_Printer {
    protected PrintStream printStream;

    public void println() {
        printStream.println();
    }

    public void println(Object arg) {
        printStream.println(arg);
    }

    public void print(Object arg) {
        printStream.print(arg);
    }

    public AnnotationTest_Printer() {
        this(System.out);
    }

    public AnnotationTest_Printer(PrintStream printStream) {
        super();

        this.printStream = printStream;
    }

    public void printClasses(String banner, AnnotationTargetsImpl_Targets annotationTargets) {

        println(banner + " BEGIN");

        for (String className : annotationTargets.getSeedClassNames()) {
            String superclassName = annotationTargets.getSuperclassName(className);
            if (superclassName != null) {
                println("  Class " + className + " extends " + superclassName);
            } else {
                println("Class: " + className);
            }

            String[] interfaceNames = annotationTargets.getInterfaceNames(className);
            if ((interfaceNames != null) && (interfaceNames.length != 0)) {
                println("  implements " + interfaceNames);
            }
        }

        println(banner + " END");
    }

    public void printTargets(String banner, AnnotationTargetsImpl_Targets targets) {
        println(banner + " BEGIN");

        System.out.println("Detail is enabled: " + targets.getIsDetailEnabled());

        printTargets("Package Level Annotations", targets.getPackageAnnotationData());
        printTargets("Class Level Annotations", targets.getClassAnnotationData());
        printTargets("Field Level Annotations (by Class)", targets.getFieldAnnotationData());
        printTargets("Method Level Annotations (by Class)", targets.getMethodAnnotationData());

        println(banner + " END");
    }

    public void printTargets(String banner, Util_BidirectionalMap targets) {
        println(banner + " BEGIN");

        println("  Enabled: " + targets.getIsEnabled());

        if (targets.getIsEnabled()) {
            Set<String> holderSet = targets.getHolderSet();

            for (String className : holderSet) {
                println("  Class " + className);

                Set<String> held = targets.selectHeldOf(className);
                for (String annotationClassName : held) {
                    println("    @" + annotationClassName);
                }
            }

            Set<String> heldSet = targets.getHeldSet();
            for (String annotationClassName : heldSet) {
                println("  Annotation @" + annotationClassName);

                Set<String> holders = targets.selectHoldersOf(annotationClassName);
                for (String className : holders) {
                    println("    Class " + className);
                }
            }
        }

        println(banner + " END");
    }
}

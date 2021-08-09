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

package com.ibm.ws.annocache.test.utils;

import java.io.PrintStream;
import java.util.Set;

import com.ibm.ws.annocache.targets.internal.AnnotationTargetsImpl_Targets;
import com.ibm.wsspi.annocache.util.Util_BidirectionalMap;

public class TestPrinter {
//  public boolean getDoPrint() {
//      return false;
//  }
//
//  if ( getDoPrint() ) {
//      TestPrinter printer = new TestPrinter();
//
//      printer.println("BEGIN [ " + fullName + " ]");
//
//      printer.printClasses(targets);
//      printer.printTargets(targets);
//
//      printer.println("END[ " + fullName + " ]");
//  }

    //

    public TestPrinter() {
        this(System.out);
    }

    public TestPrinter(PrintStream printStream) {
        super();

        this.printStream = printStream;
    }

    //

    private final PrintStream printStream;

    public void print(Object arg) {
        printStream.print(arg);
    }

    public void println() {
        printStream.println();
    }

    public void println(Object arg) {
        printStream.println(arg);
    }

    //

    public void printClasses(AnnotationTargetsImpl_Targets annotationTargets) {
        println("BEGIN [ Classes ]");

        printClasses( annotationTargets, "Seed Classes", annotationTargets.getSeedClassNames() );
        printClasses( annotationTargets, "Partial Classes", annotationTargets.getPartialClassNames() );
        printClasses( annotationTargets, "Excluded Classes", annotationTargets.getExcludedClassNames() );
        printClasses( annotationTargets, "External Classes", annotationTargets.getExternalClassNames() );

        println("END [ Classes ]");
    }

    public void printClasses(
        AnnotationTargetsImpl_Targets annotationTargets,
        String policyTag,
        Set<String> classNames) {

        println("BEGIN [ " + policyTag + " ]");

        for ( String className : classNames ) {
            String superclassName = annotationTargets.getSuperclassName(className);
            if ( superclassName != null ) {
                println("Class " + className + " extends " + superclassName);
            } else {
                println("Class: " + className);
            }

            String[] interfaceNames = annotationTargets.getInterfaceNames(className);
            if ( (interfaceNames != null) && (interfaceNames.length != 0) ) {
                println("  implements " + interfaceNames);
            }
        }

        println("END [ " + policyTag + " ]");
    }

    public void printTargets(AnnotationTargetsImpl_Targets targets) {
        println("BEGIN [ Annotations ]");

        printTargets("Package Annotations", targets.i_getPackageAnnotations(), IS_PACKAGE);
        printTargets("Class Annotations", targets.i_getClassAnnotations(), IS_CLASS);
        printTargets("Field Annotations", targets.i_getFieldAnnotations(), IS_CLASS);
        printTargets("Method Annotations", targets.i_getMethodAnnotations(), IS_CLASS);

        println("END [ Annotation Targets ]");
    }

    public static boolean IS_CLASS = true;
    public static boolean IS_PACKAGE = false;

    public void printTargets(String banner, Util_BidirectionalMap targets, boolean isClass) {
        println("BEGIN [ " + banner + " ]");

        String tag = ( isClass ? "Class" : "Package" );

        for (String className : targets.getHolderSet() ) {
            println("  " + tag + ": " + className);
            for ( String annotationClassName : targets.selectHeldOf(className) ) {
                println("    @" + annotationClassName);
            }
        }

        for ( String annotationClassName : targets.getHeldSet() ) {
            println("  @" + annotationClassName);
            for ( String className : targets.selectHoldersOf(annotationClassName) ) {
                println("    " + tag +": " + className);
            }
        }

        println("END [ " + banner + " ]");
    }
}

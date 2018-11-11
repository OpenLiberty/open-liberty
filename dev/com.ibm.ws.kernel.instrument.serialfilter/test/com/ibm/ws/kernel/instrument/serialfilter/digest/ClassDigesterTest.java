/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.digest;

import org.junit.Test;

import static com.ibm.ws.kernel.instrument.serialfilter.digest.Assert.assertMatch;
import static com.ibm.ws.kernel.instrument.serialfilter.digest.Assert.assertNoMatch;

public class ClassDigesterTest {

    //////////////////////////////////////////////
    // the following checksums should all match //
    //////////////////////////////////////////////
    @Test
    public void testSameClassTwice() {assertMatch(Simple.class, Simple.class);}

    @Test
    public void testNesterClass() {assertMatch(Simple.class, Nester.class);}

    @Test
    public void testNestedClass() {assertMatch(Simple.class, Nester.Nested.class);}

    @Test
    public void testOuterClass() {assertMatch(Simple.class, Outer.class);}

    @Test
    public void testIdenticalClasses() {assertMatch(Simple.class, Simple2.class);}

    @Test
    public void testGenericClass() {assertMatch(Simple.class, Generic.class);}

    //////////////////////////////////////////////
    // the following checksums should NOT match //
    //////////////////////////////////////////////
    @Test
    public void testProtectedClass() {assertNoMatch(Simple.class, Protected.class);}

    @Test
    public void testPackagePrivateClass() {assertNoMatch(Simple.class, PackagePrivate.class);}

    @Test
    public void testPrivateClass() {assertNoMatch(Simple.class, Private.class);}

    @Test
    public void testInnerClass() {assertNoMatch(Simple.class, Inner.class);}

    @Test
    public void testAnnotatedClass() {assertNoMatch(Simple.class, Annotated1.class);}

    @Test
    public void testAnnotatedClassWithValue() {assertNoMatch(Simple.class, Annotated2.class);}

    @Test
    public void testAnnotatedClassesWithDifferentValues() {assertNoMatch(Annotated2.class, Annotated3.class);}

    @Test
    public void testAnnotatedClassesSameValueOnDifferentAnnotations() {assertNoMatch(Annotated3.class, Annotated4.class);}

    @Test
    public void testAnnotatedClassesWithDifferentlyOrderedArrays() {assertNoMatch(Annotated3.class, Annotated4.class);}


    // Equivalent Classes
    public static class Simple {}
    public static class Simple2 {}
    public static class Nester{public static class Nested{}}
    public static class Outer{public class Inner{}}
    protected static class Protected {}
    static class PackagePrivate {}
    private static class Private {}
    public static class Generic<T> {}
    // Non-equivalent classes
    public class Inner {}
    @Annotation
    public static class Annotated1 {}
    @Annotation(myClass = Object.class)
    public static class Annotated2{}
    @Annotation(myClass = String.class)
    public static class Annotated3{}
    @Annotation(myOtherClass = String.class)
    public static class Annotated4{}
    @Annotation(myClasses = {String.class, Object.class})
    public static class Annotated5{}
    @Annotation(myClasses = {Object.class, String.class})
    public static class Annotated6{}

}

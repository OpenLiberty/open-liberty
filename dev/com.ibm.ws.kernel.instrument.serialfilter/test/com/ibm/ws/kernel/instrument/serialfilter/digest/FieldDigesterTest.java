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

public class FieldDigesterTest {
    @Test
    public void testNoFieldsVsOneField() throws Exception {
        Assert.assertNoMatch(NoFields.class, StringNameField.class);
    }

    @Test
    public void testFieldsThatDifferByName() throws Exception {
        Assert.assertNoMatch(StringNameField.class, StringIdField.class);
    }

    @Test
    public void testFieldsThatDifferByType() throws Exception {
        Assert.assertNoMatch(IntIdField.class, StringIdField.class);
    }

    @Test
    public void testFieldsThatDifferByAnnotatedness() throws Exception {
        Assert.assertNoMatch(IntIdField.class, AnnotatedIntIdField1.class);
    }

    @Test
    public void testFieldsThatDifferByAnnotationValuePresence() throws Exception {
        Assert.assertNoMatch(AnnotatedIntIdField1.class, AnnotatedIntIdField2.class);
    }

    @Test
    public void testFieldsThatDifferByAnnotationValueContent() throws Exception {
        Assert.assertNoMatch(AnnotatedIntIdField2.class, AnnotatedIntIdField3.class);
    }

    @Test
    public void testFieldsThatDifferByAnnotationValueArrayContent() throws Exception {
        Assert.assertNoMatch(AnnotatedIntIdField4.class, AnnotatedIntIdField5.class);
    }

    @Test
    public void testStaticIntFieldsThatDifferByInitializer() throws Exception {
        Assert.assertNoMatch(Initializer1.class, Initializer2.class);
    }

    class NoFields{}
    class StringNameField{String name;}
    class StringIdField{String id;}
    class IntIdField{int id;}
    class AnnotatedIntIdField1{@Annotation int id;}
    class AnnotatedIntIdField2{@Annotation(myClass=Integer.class) int id;}
    class AnnotatedIntIdField3{@Annotation(myClass=Number.class) int id;}
    class AnnotatedIntIdField4{@Annotation(myClasses={Integer.class, Number.class}) int id;}
    class AnnotatedIntIdField5{@Annotation(myClasses={Integer.class, Object.class}) int id;}
    static class Initializer1{public static final int NUM = 7;}
    static class Initializer2{public static final int NUM = 9;}



//    @Test
//    public void testStaticIntArrayFieldsThatDifferByInitializer() throws Exception {
//        // these are initialized in <clinit>
//        assertNoMatch(Initializer3.class, Initializer4.class);
//    }
//    static class Initializer3{public static final int[] NUM = {7};}
//    static class Initializer4{public static final int[] NUM = {9};}
}

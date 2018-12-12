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

import static com.ibm.ws.kernel.instrument.serialfilter.digest.Assert.assertNoMatch;

public class MethodDigesterTest {
    @Test
    public void testAddingAMethodChangesChecksum() {assertNoMatch(NoMethod.class, SimpleClass.class);}
    @Test
    public void testMethodReturnTypeAffectsChecksum() {assertNoMatch(SimpleClass.class, IntReturnType.class);}
    @Test
    public void testParameterAffectsChecksum() {assertNoMatch(SimpleClass.class, StringParam.class);}
    @Test
    public void testParameterTypeAffectsChecksum() {assertNoMatch(StringParam.class, ObjectParam.class);}
    @Test
    public void testVarargsVsArrayParameterAffectsChecksum() {assertNoMatch(StringArrayParam.class, StringVarargsParam.class);}
    @Test
    public void testMethodAnnotationAffectsChecksum() {assertNoMatch(SimpleClass.class, AnnotatedMethod.class);}
    @Test
    public void testParamAnnotationAffectsChecksum() {assertNoMatch(StringParam.class, AnnotatedStringParam.class);}
    @Test
    public void testParamAnnotationPositionAffectsChecksum() {assertNoMatch(StringParam1Annotated.class, StringParam2Annotated.class);}

    static class SimpleClass {void method(){}}
    static class NoMethod {}
    static class IntReturnType {int method(){return 0;}}
    static class StringParam {void method(String arg){}}
    static class ObjectParam {void method(Object arg){}}
    static class StringArrayParam {void method(String[] arg){}}
    static class StringVarargsParam {void method(String... arg){}}
    static class AnnotatedMethod {@Annotation void method(){}}
    static class AnnotatedStringParam {void method(@Annotation String arg){}}
    static class StringParam1Annotated {void method(@Annotation String arg1, String arg2){}}
    static class StringParam2Annotated {void method(String arg1, @Annotation String arg2){}}
}

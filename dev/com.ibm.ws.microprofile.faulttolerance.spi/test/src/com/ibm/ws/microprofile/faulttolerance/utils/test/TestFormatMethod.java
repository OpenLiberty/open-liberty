/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.utils.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.utils.FTDebug;
import com.ibm.ws.microprofile.faulttolerance.utils.test.MethodFormatSample.InnerClass;

/**
 * Test {@link FTConstants#formatMethod(Method)}
 */
public class TestFormatMethod {

    @Test
    public void testSimpleMethod() {
        String methodString = FTDebug.formatMethod(getMethod(MethodFormatSample.class, "simpleMethod"));
        assertThat(methodString, is("com.ibm.ws.microprofile.faulttolerance.utils.test.MethodFormatSample.simpleMethod()"));
    }

    @Test
    public void testMethodWithParameters() {
        String methodString = FTDebug.formatMethod(getMethod(MethodFormatSample.class, "methodWithParameters"));
        assertThat(methodString, is("com.ibm.ws.microprofile.faulttolerance.utils.test.MethodFormatSample.methodWithParameters(String, int)"));
    }

    @Test
    public void testInnerClassMethod() {
        String methodString = FTDebug.formatMethod(getMethod(InnerClass.class, "innerClassMethod"));
        assertThat(methodString, is("com.ibm.ws.microprofile.faulttolerance.utils.test.MethodFormatSample$InnerClass.innerClassMethod()"));
    }

    @Test
    public void testGenericParameter() {
        String methodString = FTDebug.formatMethod(getMethod(MethodFormatSample.class, "genericParameter"));
        assertThat(methodString, is("com.ibm.ws.microprofile.faulttolerance.utils.test.MethodFormatSample.genericParameter(Future)"));
    }

    @Test
    public void testInheritedMethod() {
        String methodString = FTDebug.formatMethod(getMethod(MethodFormatSample.class, "toString"));
        assertThat(methodString, is("java.lang.Object.toString()"));
    }

    @Test
    public void testNull() {
        assertThat(FTDebug.formatMethod(null), is("null"));
    }

    private Method getMethod(Class<?> clazz, String methodName) {
        return Arrays.stream(clazz.getMethods()).filter((m) -> m.getName().equals(methodName)).findFirst().get();
    }

}

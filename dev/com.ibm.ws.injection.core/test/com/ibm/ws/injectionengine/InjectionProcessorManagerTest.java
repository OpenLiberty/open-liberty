/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Assert;
import org.junit.Test;

public class InjectionProcessorManagerTest {
    @Retention(RetentionPolicy.RUNTIME)
    @interface TestPasswordAnnotation {
        String password() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface TestPropertiesAnnotation {
        String[] properties() default "";
    }

    @Test
    public void testToString() throws Exception {
        Assert.assertEquals('@' + TestPasswordAnnotation.class.getName() + "(password=)",
                            InjectionProcessorManager.toStringSecure(TestToString.class.getMethod("emptyPassword").getAnnotation(TestPasswordAnnotation.class)));
        Assert.assertEquals('@' + TestPasswordAnnotation.class.getName() + "(password=********)",
                            InjectionProcessorManager.toStringSecure(TestToString.class.getMethod("password").getAnnotation(TestPasswordAnnotation.class)));
        Assert.assertEquals('@' + TestPropertiesAnnotation.class.getName() + "(properties=[])",
                            InjectionProcessorManager.toStringSecure(TestToString.class.getMethod("emptyProperties").getAnnotation(TestPropertiesAnnotation.class)));
        Assert.assertEquals('@' + TestPropertiesAnnotation.class.getName() + "(properties=[a=b, b=c])",
                            InjectionProcessorManager.toStringSecure(TestToString.class.getMethod("properties").getAnnotation(TestPropertiesAnnotation.class)));
    }

    public static class TestToString {
        @TestPasswordAnnotation
        public void emptyPassword() {}

        @TestPasswordAnnotation(password = "abc")
        public void password() {}

        @TestPropertiesAnnotation
        public void emptyProperties() {}

        @TestPropertiesAnnotation(properties = { "a=b", "b=c" })
        public void properties() {}
    }
}

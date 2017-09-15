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
package com.ibm.ws.container.util;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.ejbcontainer.util.FieldClassValue;
import com.ibm.ws.ejbcontainer.util.FieldClassValueFactory;

public class FieldClassValueFactoryTest {
    private static class TestClass {
        @SuppressWarnings("unused")
        private String value;
    }

    @Test
    public void test() throws Exception {
        FieldClassValue cv = FieldClassValueFactory.create("value");
        Assert.assertEquals(TestClass.class.getDeclaredField("value"), cv.get(TestClass.class));
        // Again to test caching if any.
        Assert.assertEquals(TestClass.class.getDeclaredField("value"), cv.get(TestClass.class));
    }
}

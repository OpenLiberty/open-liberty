/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.event;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 */
public class ParameterUtilsTest {

    @Test
    public void convertWithString() throws Exception {
        String param = "string data";
        StringBuffer output = ParameterUtils.format(param);
        assertEquals("The output does not match.", param, output.toString());
    }

    @Test
    public void convertWithInt() throws Exception {
        int param = 10;
        StringBuffer output = ParameterUtils.format(param);
        assertEquals("The output does not match.", "10", output.toString());
    }

    @Test
    public void convertWithStringArray() throws Exception {
        String[] param = { "item1", "item2" };
        StringBuffer output = ParameterUtils.format(param);
        assertEquals("The output does not match.", "[item1, item2]", output.toString());
    }

    @Test
    public void convertWithLongArray() throws Exception {
        long[] param = { 100L, 200L };
        StringBuffer output = ParameterUtils.format(param);
        assertEquals("The output does not match.", "[100, 200]", output.toString());
    }

    @Test
    public void convertWithNestedStringArray() throws Exception {
        String[] nestedparam = { "nesteditem1", "nesteditem2" };
        Object[] param = { "item1", nestedparam };
        StringBuffer output = ParameterUtils.format(param);
        assertEquals("The output does not match.", "[item1, [nesteditem1, nesteditem2]]", output.toString());
    }

    @Test
    public void convertWithNestedByteArray() throws Exception {
        byte[] nestedparam = { 10, 20 };
        Object[] param = { nestedparam, (byte) 30 };
        StringBuffer output = ParameterUtils.format(param);
        assertEquals("The output does not match.", "[[10, 20], 30]", output.toString());
    }

    @Test
    public void convertWithNestedObjectArray() throws Exception {
        TestObject[] nestedparam = { new TestObject("nested1"), new TestObject("nested2") };
        Object[] param = { nestedparam, new TestObject("root1") };
        StringBuffer output = ParameterUtils.format(param);
        assertEquals("The output does not match.", "[[TestObject-nested1, TestObject-nested2], TestObject-root1]", output.toString());
    }

    @Test
    public void convertWithNull() throws Exception {
        StringBuffer output = ParameterUtils.format(null);
        assertEquals("The output does not match.", "", output.toString());
    }

    @Test
    public void convertWithNestedStringArrayWithNull() throws Exception {
        String[] nestedparam = { "nesteditem1", null, "nesteditem3" };
        Object[] param = { "item1", nestedparam };
        StringBuffer output = ParameterUtils.format(param);
        assertEquals("The output does not match.", "[item1, [nesteditem1, nesteditem3]]", output.toString());
    }

    public class TestObject {
        private final String _value;

        public TestObject(String value) {
            _value = value;
        }

        @Override
        public String toString() {
            return "TestObject-" + _value;
        }
    }
}

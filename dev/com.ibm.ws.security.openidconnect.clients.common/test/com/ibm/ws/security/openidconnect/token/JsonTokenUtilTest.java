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
package com.ibm.ws.security.openidconnect.token;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

public class JsonTokenUtilTest extends CommonTestClass {

    @Test
    public void test_toDotFormat_nullParts() {
        String[] parts = null;
        String result = JsonTokenUtil.toDotFormat(parts);
        String expected = "";
        assertEquals("Did not get expected result.", expected, result);
    }

    @Test
    public void test_toDotFormat_emptyParts() {
        String[] parts = new String[0];
        String result = JsonTokenUtil.toDotFormat(parts);
        String expected = "";
        assertEquals("Did not get expected result.", expected, result);
    }

    @Test
    public void test_toDotFormat_singleNullEntry() {
        String[] parts = new String[] { null };
        String result = JsonTokenUtil.toDotFormat(parts);
        String expected = "";
        assertEquals("Did not get expected result.", expected, result);
    }

    @Test
    public void test_toDotFormat_multipleNullEntries() {
        String[] parts = new String[] { null, null, null };
        String result = JsonTokenUtil.toDotFormat(parts);
        String expected = "" + "." + "" + "." + "";
        assertEquals("Did not get expected result.", expected, result);
    }

    @Test
    public void test_toDotFormat_singleEmptyEntry() {
        String[] parts = new String[] { "" };
        String result = JsonTokenUtil.toDotFormat(parts);
        String expected = "";
        assertEquals("Did not get expected result.", expected, result);
    }

    @Test
    public void test_toDotFormat_multipleSimpleEntries() {
        String entry1 = "a";
        String entry2 = "";
        String entry3 = "bcd";
        String entry4 = null;
        String entry5 = "E F G";
        String[] parts = new String[] { entry1, entry2, entry3, entry4, entry5 };
        String result = JsonTokenUtil.toDotFormat(parts);

        String expected = entry1 + "." + entry2 + "." + entry3 + "." + "" + "." + entry5;
        assertEquals("Did not get expected result.", expected, result);
    }

    @Test
    public void test_toDotFormat_multipleComplexEntries() {
        String entry1 = "    my \t entry with leading spaces and    tabs\t";
        String entry2 = "Another.`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?.Entry.With.Special.Chars..";
        String entry3 = "Includes tabs \t, new lines \n, carriage returns \r";
        String[] parts = new String[] { entry1, entry2, entry3 };
        String result = JsonTokenUtil.toDotFormat(parts);

        String expected = entry1 + "." + entry2 + "." + entry3;
        assertEquals("Did not get expected result.", expected, result);
    }

}

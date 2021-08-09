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
package com.ibm.ws.collector.manager.buffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.ibm.ws.collector.manager.buffer.SimpleRotatingQueue;

/**
 *
 */
public class SimpleRotatingQueueTest {

    /**
     * 
     */
    @Test
    public void test1() {

        SimpleRotatingQueue<String> srq = new SimpleRotatingQueue<String>(new String[4]);

        srq.add("str1");
        srq.add("str2");
        srq.add("str3");

        String[] strs = srq.toArray(new String[0]);

        assertEquals(4, strs.length);
        assertArrayEquals(new String[] { null, "str1", "str2", "str3" }, strs);

        srq.add("str4");
        String[] strs2 = srq.toArray(strs);

        assertSame(strs, strs2);
        assertArrayEquals(new String[] { "str1", "str2", "str3", "str4" }, strs);

        srq.add("str5");
        srq.add("str6");

        strs2 = srq.toArray(strs2);
        assertArrayEquals(new String[] { "str3", "str4", "str5", "str6" }, strs);
    }

    /**
     * 
     */
    @Test
    public void test2() {

        SimpleRotatingQueue<String> srq = new SimpleRotatingQueue<String>(new String[2]);

        srq.add("str1");
        srq.add("str2");
        srq.add("str3");
        srq.add("str4");
        srq.add("str5");
        srq.add("str6");
        srq.add("str7");
        srq.add("str8");

        String[] strs = srq.toArray(new String[3]);

        assertArrayEquals(new String[] { "str7", "str8", null }, strs);
    }
}

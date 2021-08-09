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
package com.ibm.ws.microprofile.config.internals.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.Test;

import com.ibm.ws.microprofile.config.impl.SortedSourcesImpl;
import com.ibm.ws.microprofile.config.interfaces.SortedSources;
import com.ibm.ws.microprofile.test.AbstractConfigTest;

/**
 *
 */
public class SortedSourcesTest extends AbstractConfigTest {

    @Test
    public void testSimpleOrdinalSort() {
        SortedSources sources = new SortedSourcesImpl();
        TestSource testSourceMIN = new TestSource(Integer.MIN_VALUE, "TestSourceMIN");
        TestSource testSourceM1 = new TestSource(-1, "TestSourceM1");
        TestSource testSource0 = new TestSource(0, "TestSource0");
        TestSource testSource1 = new TestSource(1, "TestSource1");
        TestSource testSource2 = new TestSource(2, "TestSource2");
        TestSource testSource3 = new TestSource(3, "TestSource3");
        TestSource testSource400 = new TestSource(400, "TestSource400");
        TestSource testSourceMAX = new TestSource(Integer.MAX_VALUE, "TestSourceMAX");

        sources.add(testSource2);
        sources.add(testSourceMAX);
        sources.add(testSourceM1);
        sources.add(testSource3);
        sources.add(testSource0);
        sources.add(testSource400);
        sources.add(testSource1);
        sources.add(testSourceMIN);

        Iterator<ConfigSource> itr = sources.iterator();
        ConfigSource src = itr.next();
        assertEquals(sources.toString(), testSourceMAX, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource400, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource3, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource2, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource1, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource0, src);

        src = itr.next();
        assertEquals(sources.toString(), testSourceM1, src);

        src = itr.next();
        assertEquals(sources.toString(), testSourceMIN, src);
    }

    @Test
    public void testIDSort() {
        SortedSources sources = new SortedSourcesImpl();
        TestSource testSource0A = new TestSource(0, "TestSource0A");
        TestSource testSource0B = new TestSource(0, "TestSource0B");
        TestSource testSource0C = new TestSource(0, "TestSource0C");
        TestSource testSource500A = new TestSource(500, "TestSource500A");
        TestSource testSource500B = new TestSource(500, "TestSource500B");
        TestSource testSource500C = new TestSource(500, "TestSource500C");

        sources.add(testSource500B);
        sources.add(testSource0C);
        sources.add(testSource500C);
        sources.add(testSource500A);
        sources.add(testSource0B);
        sources.add(testSource0A);

        Iterator<ConfigSource> itr = sources.iterator();
        ConfigSource src = itr.next();
        assertEquals(sources.toString(), testSource500A, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource500B, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource500C, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource0A, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource0B, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource0C, src);
    }

    @Test
    public void testHashCodeSort() {
        SortedSources sources = new SortedSourcesImpl();
        TestSource testSource0A1 = new TestSource(0, "TestSource0A", 1);
        TestSource testSource0A2 = new TestSource(0, "TestSource0A", 2);

        TestSource testSource0B1 = new TestSource(0, "TestSource0B", 1);
        TestSource testSource0B2 = new TestSource(0, "TestSource0B", 2);

        TestSource testSource0C1 = new TestSource(0, "TestSource0C", 1);
        TestSource testSource0C2 = new TestSource(0, "TestSource0C", 2);

        sources.add(testSource0C2);
        sources.add(testSource0A1);
        sources.add(testSource0B2);
        sources.add(testSource0C1);
        sources.add(testSource0A2);
        sources.add(testSource0B1);

        Iterator<ConfigSource> itr = sources.iterator();
        ConfigSource src = itr.next();
        assertEquals(sources.toString(), testSource0A1, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource0A2, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource0B1, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource0B2, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource0C1, src);

        src = itr.next();
        assertEquals(sources.toString(), testSource0C2, src);
    }

    @Test
    public void testUnmodifiable() {
        SortedSources sources = new SortedSourcesImpl();
        TestSource testSource0 = new TestSource(0, "TestSource0");
        TestSource testSource1 = new TestSource(1, "TestSource1");
        TestSource testSource2 = new TestSource(2, "TestSource2");

        sources.add(testSource0);
        sources.add(testSource1);

        sources = sources.unmodifiable();
        assertEquals(2, sources.size());

        try {
            sources.add(testSource2);
            fail("Exception not thrown");
        } catch (UnsupportedOperationException e) {
            //expected
        }
    }
}

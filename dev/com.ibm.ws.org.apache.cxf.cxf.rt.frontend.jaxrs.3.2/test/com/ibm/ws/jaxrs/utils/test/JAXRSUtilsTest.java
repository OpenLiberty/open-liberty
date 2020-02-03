/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.utils.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.junit.Test;


public class JAXRSUtilsTest {


    @Test
    public void testSortCharsets_nullAndEmpty() throws Exception {
        assertEquals(Collections.EMPTY_LIST, JAXRSUtils.sortCharsets(null));
        assertEquals(Collections.EMPTY_LIST, JAXRSUtils.sortCharsets(new ArrayList<Object>()));
    }

    @Test
    public void testSortCharsets_singleEntryNoQ() throws Exception {
        String cs = "UTF-16";
        List<Charset> sortedCharsets = JAXRSUtils.sortCharsets(Collections.singletonList(cs));
        assertNotNull(sortedCharsets);
        assertEquals(1, sortedCharsets.size());
        assertEquals(Charset.forName(cs), sortedCharsets.get(0));
    }

    @Test
    public void testSortCharsets_singleEntryWithQ() throws Exception {
        String cs = "UTF-16;q=0.5";
        List<Charset> sortedCharsets = JAXRSUtils.sortCharsets(Collections.singletonList(cs));
        assertNotNull(sortedCharsets);
        assertEquals(1, sortedCharsets.size());
        assertEquals(Charset.forName("UTF-16"), sortedCharsets.get(0));

        cs = "UTF-16;q=.005";
        sortedCharsets = JAXRSUtils.sortCharsets(Collections.singletonList(cs));
        assertNotNull(sortedCharsets);
        assertEquals(1, sortedCharsets.size());
        assertEquals(Charset.forName("UTF-16"), sortedCharsets.get(0));
        
        cs = "UTF-16;q=0"; // 0 == disabled
        sortedCharsets = JAXRSUtils.sortCharsets(Collections.singletonList(cs));
        assertNotNull(sortedCharsets);
        assertEquals(0, sortedCharsets.size());

        cs = "UTF-16;q=1";
        sortedCharsets = JAXRSUtils.sortCharsets(Collections.singletonList(cs));
        assertNotNull(sortedCharsets);
        assertEquals(1, sortedCharsets.size());
        assertEquals(Charset.forName("UTF-16"), sortedCharsets.get(0));
    }

    @Test
    public void testSortCharsets_multipleEntries() throws Exception {
        List<String> charsetHeaderValues = Arrays.asList("UTF-8;q=0.3", "UTF-16BE;q=0.5", "UTF-16LE;Q=.6",
                                                         "UTF-16;q=.4", "US-ASCII;q=15.7", "ISO-8859-1;Q=-20");
        List<Charset> sortedCharsets = JAXRSUtils.sortCharsets(charsetHeaderValues);
        assertNotNull(sortedCharsets);
        assertEquals(5, sortedCharsets.size());
        assertEquals(Charset.forName("US-ASCII"), sortedCharsets.get(0));
        assertEquals(Charset.forName("UTF-16LE"), sortedCharsets.get(1));
        assertEquals(Charset.forName("UTF-16BE"), sortedCharsets.get(2));
        assertEquals(Charset.forName("UTF-16"), sortedCharsets.get(3));
        assertEquals(Charset.forName("UTF-8"), sortedCharsets.get(4));

        charsetHeaderValues = Arrays.asList("UTF-8;q=0.999", "UTF-16BE", "UTF-16LE;Q=0");
        sortedCharsets = JAXRSUtils.sortCharsets(charsetHeaderValues);
        assertNotNull(sortedCharsets);
        assertEquals(2, sortedCharsets.size());
        assertEquals(Charset.forName("UTF-16BE"), sortedCharsets.get(0));
        assertEquals(Charset.forName("UTF-8"), sortedCharsets.get(1));
    }

    @Test
    public void testSortCharsets_invalidWeightEntries() throws Exception {
        assertEquals(Collections.EMPTY_LIST, JAXRSUtils.sortCharsets(Collections.singletonList("UTF-16;q=ALOT")));
        assertEquals(Collections.EMPTY_LIST, JAXRSUtils.sortCharsets(Collections.singletonList("UTF-16;q= ")));
        assertEquals(Collections.EMPTY_LIST, JAXRSUtils.sortCharsets(Collections.singletonList("UTF-16;q=MILLIONS")));
        assertEquals(Collections.EMPTY_LIST, JAXRSUtils.sortCharsets(Collections.singletonList("UTF-16;q=%20")));
        assertEquals(Collections.EMPTY_LIST, JAXRSUtils.sortCharsets(Collections.singletonList("UTF-16 ; q = 0.5")));
        assertEquals(Collections.EMPTY_LIST, JAXRSUtils.sortCharsets(Collections.singletonList("UTF-16;q==0.3")));
    }
}

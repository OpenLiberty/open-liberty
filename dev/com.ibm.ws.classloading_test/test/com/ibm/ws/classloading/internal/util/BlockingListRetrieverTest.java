/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal.util;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.classloading.internal.util.BlockingList.Retriever;

/**
 * Test the blocking list blocks until a requested element is available.
 * Test the list times out if a timeout has been specified.
 * Test that the list degrades to a condensed list after a timeout.
 * Test that the iterators work consistently in the face of timeouts.
 */
public class BlockingListRetrieverTest {
    private static boolean MISSING_ELEMENTS_ARE_INVALID;

    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");

    private static final Retriever<Integer, String> INTS_TO_STRINGS = new Retriever<Integer, String>() {
        @Override
        public String fetch(Integer k) {
            return "" + k;
        }
    };

    private static final Retriever<Integer, String> ODD_INTS_TO_STRINGS = new Retriever<Integer, String>() {
        @Override
        public String fetch(Integer k) throws ElementNotReadyException, ElementNotValidException {
            if (k % 2 == 0)
                if (MISSING_ELEMENTS_ARE_INVALID)
                    throw new ElementNotValidException();
                else
                    throw new ElementNotReadyException();
            return "" + k;
        }
    };

    private static final Retriever<Integer, String> EVEN_INTS_TO_STRINGS = new Retriever<Integer, String>() {
        @Override
        public String fetch(Integer k) throws ElementNotReadyException, ElementNotValidException {
            if (k % 2 != 0)
                if (MISSING_ELEMENTS_ARE_INVALID)
                    throw new ElementNotValidException();
                else
                    throw new ElementNotReadyException();
            return "" + k;
        }
    };

    @Before
    public void reset() {
        MISSING_ELEMENTS_ARE_INVALID = false;
    }

    @Test
    public void testRetriever() {
        assertEquals(list("1", "2", "3", "4"), BlockingListMaker
                        .defineList()
                        .waitFor(2, MILLISECONDS)
                        .fetchElements(INTS_TO_STRINGS)
                        .useKeys(1, 2, 3, 4)
                        .make());
    }

    @Test
    public void testRetrieverWithEvenElementsMissing() {
        BlockingList<Integer, String> bList = BlockingListMaker
                        .defineList()
                        .waitFor(2, MILLISECONDS)
                        .fetchElements(ODD_INTS_TO_STRINGS)
                        .useKeys(1, 2, 3, 4)
                        .make();
        assertEquals(bList.size(), 2);
        assertEquals(list("1", "3"), bList);
    }

    @Test
    public void testRetrieverWithOddElementsMissing() {
        BlockingList<Integer, String> bList = BlockingListMaker
                        .defineList()
                        .waitFor(2, MILLISECONDS)
                        .fetchElements(EVEN_INTS_TO_STRINGS)
                        .useKeys(1, 2, 3, 4)
                        .make();
        assertEquals(bList.size(), 2);
        assertEquals(list("2", "4"), new ArrayList<String>(bList));
    }

    @Test
    public void testRetrieverWithOddElementsInvalid() {
        MISSING_ELEMENTS_ARE_INVALID = true;
        testRetrieverWithOddElementsMissing();
    }

    @Test
    public void testRetrieverWithEvenElementsInvalid() {
        MISSING_ELEMENTS_ARE_INVALID = true;
        testRetrieverWithEvenElementsMissing();
    }

    private <E> List<E> list(E... elements) {
        return Arrays.asList(elements);
    }
}

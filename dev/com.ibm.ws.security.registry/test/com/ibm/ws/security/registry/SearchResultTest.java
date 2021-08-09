/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * 
 */
public class SearchResultTest {

    /**
     * Test method for {@link com.ibm.ws.security.registry.SearchResult#SearchResult()}.
     */
    @Test
    public void constructorDefault() {
        SearchResult result = new SearchResult();
        assertFalse("Default constructor should set hasMore to false",
                    result.hasMore());
        assertNotNull("Default constructor should list to non-null List",
                      result.getList());
        assertEquals("Default constructor should list to empty List",
                     0, result.getList().size());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.SearchResult#SearchResult(java.util.List, boolean)}.
     */
    @Test
    public void constructorListOfStringBoolean() {
        List<String> list = new ArrayList<String>();
        boolean hasMore = true;
        SearchResult result = new SearchResult(list, hasMore);
        assertEquals("Argument constructor should set hasMore to specified value",
                     hasMore, result.hasMore());
        assertSame("Argument constructor should set list to specified value",
                   list, result.getList());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.SearchResult#toString()}
     */
    @Test
    public void toStringNoValues() {
        SearchResult result = new SearchResult();
        assertEquals("SearchResult hasMore=false []", result.toString());
    }

    @Test
    public void toStringWithValues() {
        List<String> list = new ArrayList<String>();
        list.add("user1");
        list.add("user2");
        SearchResult result = new SearchResult(list, true);
        assertEquals("SearchResult hasMore=true [user1, user2]", result.toString());
    }

}

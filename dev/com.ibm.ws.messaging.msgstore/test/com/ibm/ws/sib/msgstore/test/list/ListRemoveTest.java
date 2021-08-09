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
/*
 * Change activity:
 *
 * Reason        Date        Origin       Description
 * ------------  --------    ----------   ---------------------------------------
 *               11/11/05    schofiel     Original
 * 278082        03/01/06    schofiel     Make jumpback explicit on subcursor constructor
 * ============================================================================
 */
package com.ibm.ws.sib.msgstore.test.list;

import com.ibm.ws.sib.msgstore.list.Link;
import com.ibm.ws.sib.msgstore.list.LinkedList;
import com.ibm.ws.sib.msgstore.list.Subcursor;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ListRemoveTest extends TestCase
{
    public static final void main(String[] args)
    {
        new ListRemoveTest().test();
    }

    public static TestSuite suite()
    {
        return new TestSuite(ListRemoveTest.class);
    }

    public final void test()
    {
        _remove();
        _removeCursored();
        _removeAfterCursored();

    }

    private final void _remove()
    {
        final int COUNT = 10;
        LinkedList list = new LinkedList();
        assertEquals("list should be empty", 0, list.countLinks());
        Link[] links = new Link[COUNT];
        for (int i = 0; i < COUNT; i++)
        {
            Link link = new Link();
            list.append(link);
            links[i] = link;
        }
        assertEquals("incorrect number of links added", COUNT, list.countLinks());

        // Link first = list.getHead();
        // Link second = first.getNextLink();
        links[1].unlink();
        assertEquals("", COUNT - 1, list.countLinks());
    }

    private final void _removeCursored()
    {
        final int COUNT = 10;
        LinkedList list = new LinkedList();
        assertEquals("list should be empty", 0, list.countLinks());

        for (int i = 0; i < COUNT; i++)
        {
            Link link = new Link();
            list.append(link);
        }
        assertEquals("incorrect number of links added", COUNT, list.countLinks());

        Subcursor cursor = new Subcursor(list, null, true);
        Link first = cursor.advance();
        Link second = cursor.advance();
        second.unlink();
        assertEquals("link should be pinned by cursor", COUNT, list.countLinks());
        cursor.finished();
        assertEquals("link should be released", COUNT - 1, list.countLinks());
    }

    private final void _removeAfterCursored()
    {
        final int COUNT = 10;
        LinkedList list = new LinkedList();
        assertEquals("list should be empty", 0, list.countLinks());

        for (int i = 0; i < COUNT; i++)
        {
            Link link = new Link();
            list.append(link);
        }
        assertEquals("incorrect number of links added", COUNT, list.countLinks());

        Subcursor cursor = new Subcursor(list, null, true);
        Link link = cursor.advance();
        int counted = 0;
        while (null != link)
        {
            counted = counted + 1;
            link = cursor.advance();
        }
        assertEquals("incorrect number of links counted", COUNT, counted);

        link = list.getHead();
        int removed = 0;
        while (null != link)
        {
            removed = removed + 1;
            link.unlink();
            link = list.getHead();
        }
        assertEquals("incorrect number of links removed", COUNT, removed);
        cursor.finished();
        assertEquals("list should be empty", 0, list.countLinks());

    }
}

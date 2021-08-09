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
package com.ibm.ws.sib.msgstore.test.list;
/*
 * Change activity:
 *
 * Reason        Date      Origin   Description
 * ------------  --------  -------- -------------------------------------------
 *               11/11/05  schofiel Original
 * 278082        03/01/06  schofiel Make jumpback explicit on subcursor constructor
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import com.ibm.ws.sib.msgstore.list.LinkedList;
import com.ibm.ws.sib.msgstore.list.Subcursor;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;

import junit.framework.TestSuite;

public class ListNextTest extends MessageStoreTestCase
{
    private long counter = 0;

    public static TestSuite suite()
    {
        return new TestSuite(ListNextTest.class);
    }

    public ListNextTest(String name)
    {
        super(name);

        //turnOnTrace();
    }

    public final void testNextCursored()
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
        Subcursor cursor = new Subcursor(list, null, true);
        for (int i = 0; i < links.length; i++)
        {
            Link link = (Link) cursor.advance();
            if (links[i] != link)
            {
                assertEquals(links[i], link);
            }
        }
    }

    public final void testNextStepped()
    {
        StringBuffer buffer;

        final int COUNT = 10;
        LinkedList list = new LinkedList();
        assertEquals("list should be empty", 0, list.countLinks());
        Link[] links = new Link[COUNT];

        buffer = new StringBuffer("Add Links: ");
        for (int i = 0; i < COUNT; i++)
        {
            Link link = new Link();
            list.append(link);
            links[i] = link;
            buffer.append(link.getSequence());
            buffer.append(" ");
        }
        print(buffer.toString());

        assertEquals("incorrect number of links added", COUNT, list.countLinks());
        Link link = (Link) list.getNextLink(null);

        buffer = new StringBuffer("Get Links: ");
        for (int i = 0; i < links.length; i++)
        {
            buffer.append(link.getSequence());
            buffer.append(" ");
            if (links[i] != link)
            {
                assertEquals(links[i].getSequence(), link.getSequence());
            }
            link = (Link) list.getNextLink(link);
        }
        print(buffer.toString());
    }

    public final void testAddRemoveAddRemove()
    {
        StringBuffer buffer;

        final int COUNT = 10;
        LinkedList list = new LinkedList();
        Subcursor cursor = new Subcursor(list, null, true);
        Link[] links = new Link[COUNT];
        assertEquals("list should be empty", 0, list.countLinks());

        buffer = new StringBuffer("  Adding: ");
        for (int i = 0; i < links.length; i++)
        {
            Link link = new Link();
            list.append(link);
            links[i] = link;
            buffer.append(link.getSequence());
            buffer.append(" ");
        }
        print(buffer.toString());

        buffer = new StringBuffer("Removing: ");
        for (int i = 0; i < links.length; i++)
        {
            Link link = (Link) cursor.advance();
            buffer.append(link.getSequence());
            buffer.append(" ");
            if (links[i] != link)
            {
                assertEquals(links[i], link);
            }
            link.unlink();
        }
        print(buffer.toString());

        buffer = new StringBuffer("  Adding: ");
        for (int i = 0; i < links.length; i++)
        {
            Link link = new Link();
            list.append(link);
            links[i] = link;
            buffer.append(link.getSequence());
            buffer.append(" ");
        }
        print(buffer.toString());

        buffer = new StringBuffer("Removing: ");
        for (int i = 0; i < links.length; i++)
        {
            Link link = (Link) cursor.advance();
            buffer.append(link.getSequence());
            buffer.append(" ");
            if (links[i] != link)
            {
                assertEquals(links[i], link);
            }
            link.unlink();
        }
        print(buffer.toString());
    }

    private final class Link extends com.ibm.ws.sib.msgstore.list.Link
    {
        private final long _seq = counter++;

        public long getSequence()
        {
            return _seq;
        }
    }
}

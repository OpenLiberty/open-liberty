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

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.list.Link;
import com.ibm.ws.sib.msgstore.list.LinkedList;
import com.ibm.ws.sib.msgstore.list.Subcursor;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;

public final class ListConcurrencyTest extends MessageStoreTestCase
{
    private static final int BATCH_SIZE = 1000;

    private static final int CURSORED_GETTERS = 50;

    private static final int GETTERS = 50;

    private static final int PUTTERS = 100;

    private int _cursoredGot = 0;

    private boolean _keepRunning = true;

    private final LinkedList _list = new LinkedList();

    private int _totalPut = 0;

    private int _uncursoredGot = 0;

    public static TestSuite suite()
    {
        return new TestSuite(ListConcurrencyTest.class);
    }

    public ListConcurrencyTest(String name)
    {
        super(name);

        //turnOnTrace();
    }

    private final boolean _checkResults()
    {
        int puttersFinished = 0;
        int gettersFinished = 0;
        int cursoredGettersFinished = 0;
        int totalPut = 0;
        int uncursoredGot = 0;
        int cursoredGot = 0;
        int totalGot = 0;

        synchronized (this)
        {
            totalPut = _totalPut;
            uncursoredGot = _uncursoredGot;
            cursoredGot = _cursoredGot;
            totalGot = cursoredGot + uncursoredGot;
            final int expected = PUTTERS * BATCH_SIZE;
            if (expected == _totalPut && expected == totalGot)
            {
                _keepRunning = false;
            }
        }

        print("Put:         " + _format(totalPut) + " (" + (PUTTERS * BATCH_SIZE) + ")");
        print("Got:         " + _format(totalGot) + " (" + (PUTTERS * BATCH_SIZE) + ")");
        print("Un/cursored: " + _format(uncursoredGot) + "/" + _format(cursoredGot));

        return _keepRunning;
    }

    private final synchronized void _cursoredGetterGot()
    {
        _cursoredGot++;
    }

    private final synchronized boolean _keepRunning()
    {
        return _keepRunning;
    }

    private final String _format(int num)
    {
        if (true)
        {
            return Integer.toString(num);
        }
        else
        {
            final int MILS = 4;
            int[] digits = new int[MILS * 3];
            for (int digit = digits.length - 1; num > 0 && digit > 0; digit--)
            {
                digits[digit] = num % 10;
                num = num / 10;
            }
            StringBuffer buf = new StringBuffer();
            boolean started = false;
            int digit = 0;
            for (int i = 0; i < MILS; i++)
            {
                for (int j = 0; j < 3; j++)
                {
                    if (started || digits[digit] > 0)
                    {
                        started = true;
                        buf.append(digits[digit]);
                    }
                    digit++;
                }
                if (started)
                {
                    buf.append(",");
                }
            }
            return buf.toString();
        }
    }

    private final synchronized void _putterPut()
    {
        _totalPut++;
    }

    private final synchronized void _uncursoredGetterGot()
    {
        _uncursoredGot++;
    }

    public final void testListConcurrency()
    {
        final int CYCLES = 60;

        print("Creating threads");
        final Putter[] putters = new Putter[PUTTERS];
        final Getter[] getters = new Getter[GETTERS];
        final CursoredGetter[] cursoredGetters = new CursoredGetter[CURSORED_GETTERS];
        for (int i = 0; i < putters.length; i++)
        {
            putters[i] = new Putter();
        }
        for (int i = 0; i < getters.length; i++)
        {
            getters[i] = new Getter();
        }
        for (int i = 0; i < cursoredGetters.length; i++)
        {
            cursoredGetters[i] = new CursoredGetter();
        }

        print("Starting threads");
        for (int i = 0; i < putters.length; i++)
        {
            putters[i].start();
        }
        for (int i = 0; i < getters.length; i++)
        {
            getters[i].start();
        }
        for (int i = 0; i < cursoredGetters.length; i++)
        {
            cursoredGetters[i].start();
        }

        print("Monitoring threads");
        boolean keepRunning = true;
        for (int i = 0; i < CYCLES && keepRunning; i++)
        {
            print("");
            print("Cycle " + i + "/" + CYCLES);
            keepRunning = _checkResults();
            if (keepRunning)
            {
                try
                {
                    Thread.sleep(1000);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
        synchronized (this)
        {
            _keepRunning = false;
        }
        print("Finished");
    }

    /**
     * Get links from the list using a cursor and unlink
     * them until told to stop. If a link cannot be got,
     * or cannot be unlinked then yield the thread.
     * Note that the actual numbe got by cursor cannot
     * be predicted.
     */
    private final class CursoredGetter extends Thread
    {
        @Override
        public void run()
        {
            Subcursor cursor = new Subcursor(_list, null, true);
            while (_keepRunning())
            {
                Link link = cursor.advance();
                if (null != link && link.unlink())
                {
                    _cursoredGetterGot();
                }
                else
                {
                    Thread.yield();
                }
            }
            cursor.finished();
        }
    }

    /**
     * Get links from the head of the list and unlink
     * them until told to stop. If a link cannot be got, or
     * cannot be unlinked then yield the thread.
     * Note that the actual numbe got cannot be predicted.
     */
    private final class Getter extends Thread
    {
        @Override
        public void run()
        {
            while (_keepRunning())
            {
                Link link = _list.getHead();
                if (null != link && link.unlink())
                {
                    _uncursoredGetterGot();
                }
                else
                {
                    Thread.yield();
                }
            }
        }
    }

    /**
     * Put BATCH_SIZE links onto the list.
     */
    private final class Putter extends Thread
    {
        @Override
        public void run()
        {
            for (int count = 0; count < BATCH_SIZE && _keepRunning(); count++)
            {
                Link link = new Link();
                _list.append(link);
                _putterPut();
            }
        }
    }
}

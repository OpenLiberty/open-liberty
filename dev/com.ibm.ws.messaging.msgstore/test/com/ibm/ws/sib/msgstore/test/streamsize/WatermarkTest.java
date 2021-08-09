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
package com.ibm.ws.sib.msgstore.test.streamsize;
/*
 * Change activity:
 *
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 *                 27/10/03 drphil   Original
 * 581491          06/04/09 gareth   Modify to fit new behaviour introduced in 510343
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.admin.JsHealthState;

import com.ibm.ws.sib.msgstore.Configuration;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.Statistics;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

public class WatermarkTest extends MessageStoreTestCase
{
    private MessageStore _messageStore = null;

    public WatermarkTest(String name)
    {
        super(name);

        //turnOnTrace();
    }

    public static TestSuite suite(String persistence)
    {
        TestSuite suite = new TestSuite();

        WatermarkTest test = new WatermarkTest("testWatermarks");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    public void testWatermarks()
    {
        try
        {
            _messageStore = createMessageStore(true, PERSISTENCE);
            _messageStore.start();

            JsHealthState state = _messageStore.getHealthState();
            if (!state.isOK())
            {
                fail("Failed to start message store. Health State: " + state);
            }

            _testCountHiLoItemStream();
            _testBytesHiLoItemStream();
            _testBothHiLoItemStream();
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail("exception: " + e);
        }
        finally
        {
            if (null != _messageStore)
            {
                stopMessageStore(_messageStore);
            }
        }
    }

    /**
     * Test breaches of the count watermarks.
     * Set byte watermarks to -1 to disable them.
     * 
     * @throws MessageStoreException
     */
    private final void _testCountHiLoItemStream() throws MessageStoreException 
    {
        Transaction transaction = _messageStore.getTransactionFactory().createAutoCommitTransaction();
        ItemStream itemStream = new ItemStream(-1, -1, 5, 10);
        _messageStore.add(itemStream, transaction);
        Statistics listStatistics = itemStream.getStatistics();
        assertEquals(0, listStatistics.getTotalItemCount());
        assertEquals(0, itemStream.getWatermarkBreachedCount());

        // add ten and show that they are there, but no watermark breach
        for (int i = 0; i < 9; i++)
        {
            itemStream.addItem(new Item(10), transaction);
        }
        assertEquals(9, listStatistics.getTotalItemCount());
        assertEquals(0, itemStream.getWatermarkBreachedCount());

        // add one to show watermark breach
        itemStream.addItem(new Item(10), transaction);
        assertEquals(10, listStatistics.getTotalItemCount());
        assertEquals(1, itemStream.getWatermarkBreachedCount());

        // toggle around limit
        itemStream.removeFirstMatchingItem(null, transaction);
        assertEquals(9, listStatistics.getTotalItemCount());
        assertEquals(1, itemStream.getWatermarkBreachedCount());
        itemStream.addItem(new Item(10), transaction);
        assertEquals(10, listStatistics.getTotalItemCount());
        assertEquals(2, itemStream.getWatermarkBreachedCount());

        // take count down to low watermark, but not past it., show
        // things are as we expect
        // Defect 581491
        // Low watermark is now breached when equal to the watermark
        // so we need one less removal to breach it.
        for (int i = 0; i < 4; i++)
        {
            itemStream.removeFirstMatchingItem(null, transaction);
        }
        assertEquals(6, listStatistics.getTotalItemCount());
        assertEquals(2, itemStream.getWatermarkBreachedCount());

        // remove one more to trigger watermark breach
        itemStream.removeFirstMatchingItem(null, transaction);
        assertEquals(5, listStatistics.getTotalItemCount());
        assertEquals(3, itemStream.getWatermarkBreachedCount());

        // toggle around limit
        itemStream.addItem(new Item(10), transaction);
        assertEquals(6, listStatistics.getTotalItemCount());
        assertEquals(3, itemStream.getWatermarkBreachedCount());
        itemStream.removeFirstMatchingItem(null, transaction);
        assertEquals(5, listStatistics.getTotalItemCount());
        assertEquals(4, itemStream.getWatermarkBreachedCount());

        itemStream.empty();
        itemStream.remove(transaction, itemStream.getLockID());
    }

    /**
     * Test breaches of the byte watermark.
     * Set count watermarks to -1 to disable them.
     * 
     * @throws MessageStoreException
     */
    private final void _testBytesHiLoItemStream() throws MessageStoreException 
    {
        int itemSize = 10;
        int watermarkLow  = 5 * itemSize * MessageStoreTestCase.ITEM_SIZE_MULTIPLIER;
        int watermarkHigh = 10 * itemSize * MessageStoreTestCase.ITEM_SIZE_MULTIPLIER;

        Transaction transaction = _messageStore.getTransactionFactory().createAutoCommitTransaction();
        ItemStream itemStream = new ItemStream(watermarkLow, watermarkHigh, -1, -1);
        _messageStore.add(itemStream, transaction);
        Statistics listStatistics = itemStream.getStatistics();
        assertEquals(0, listStatistics.getTotalItemCount());
        assertEquals(0, itemStream.getWatermarkBreachedCount());

        // add ten and show that they are there, but no watermark breach
        for (int i = 0; i < 9; i++)
        {
            itemStream.addItem(new Item(itemSize), transaction);
        }
        assertEquals(9, listStatistics.getTotalItemCount());
        assertEquals(0, itemStream.getWatermarkBreachedCount());

        // add one to show watermark breach
        itemStream.addItem(new Item(itemSize), transaction);
        assertEquals(10, listStatistics.getTotalItemCount());
        assertEquals(1, itemStream.getWatermarkBreachedCount());

        // toggle around limit
        itemStream.removeFirstMatchingItem(null, transaction);
        assertEquals(9, listStatistics.getTotalItemCount());
        assertEquals(1, itemStream.getWatermarkBreachedCount());
        itemStream.addItem(new Item(itemSize), transaction);
        assertEquals(10, listStatistics.getTotalItemCount());
        assertEquals(2, itemStream.getWatermarkBreachedCount());

        // take count down to low watermark, but not past it., show
        // things are as we expect
        for (int i = 0; i < 5; i++)
        {
            itemStream.removeFirstMatchingItem(null, transaction);
        }
        assertEquals(5, listStatistics.getTotalItemCount());
        assertEquals(2, itemStream.getWatermarkBreachedCount());

        // remove one more to trigger watermark breach
        itemStream.removeFirstMatchingItem(null, transaction);
        assertEquals(4, listStatistics.getTotalItemCount());
        assertEquals(3, itemStream.getWatermarkBreachedCount());

        // toggle around limit
        itemStream.addItem(new Item(itemSize), transaction);
        assertEquals(5, listStatistics.getTotalItemCount());
        assertEquals(3, itemStream.getWatermarkBreachedCount());
        itemStream.removeFirstMatchingItem(null, transaction);
        assertEquals(4, listStatistics.getTotalItemCount());
        assertEquals(4, itemStream.getWatermarkBreachedCount());

        itemStream.empty();
        itemStream.remove(transaction, itemStream.getLockID());
    }

    /**
     * Test the simultaneous breaking of of limits
     * 
     * @throws MessageStoreException
     */
    private final void _testBothHiLoItemStream() throws MessageStoreException 
    {
        int itemSize = 10;
        int watermarkLow  = 5 * itemSize * MessageStoreTestCase.ITEM_SIZE_MULTIPLIER;
        int watermarkHigh = 10 * itemSize * MessageStoreTestCase.ITEM_SIZE_MULTIPLIER;

        Transaction transaction = _messageStore.getTransactionFactory().createAutoCommitTransaction();
        ItemStream itemStream = new ItemStream(watermarkLow, watermarkHigh, 5, 10);
        _messageStore.add(itemStream, transaction);
        Statistics listStatistics = itemStream.getStatistics();
        assertEquals(0, listStatistics.getTotalItemCount());
        assertEquals(0, itemStream.getWatermarkBreachedCount());

        // add ten and show that they are there, but no watermark breach
        for (int i = 0; i < 9; i++)
        {
            itemStream.addItem(new Item(itemSize), transaction);
        }
        assertEquals(9, listStatistics.getTotalItemCount());
        assertEquals(0, itemStream.getWatermarkBreachedCount());

        // add one to show watermark breach
        itemStream.addItem(new Item(itemSize), transaction);
        assertEquals(10, listStatistics.getTotalItemCount());
        assertEquals(1, itemStream.getWatermarkBreachedCount());

        // toggle around limit
        itemStream.removeFirstMatchingItem(null, transaction);
        assertEquals(9, listStatistics.getTotalItemCount());
        assertEquals(1, itemStream.getWatermarkBreachedCount());
        itemStream.addItem(new Item(itemSize), transaction);
        assertEquals(10, listStatistics.getTotalItemCount());
        assertEquals(2, itemStream.getWatermarkBreachedCount());

        // take count down to low watermark, but not past it., show
        // things are as we expect
        // Defect 581491
        // Low watermark is now breached when equal to the watermark
        // so we need one less removal to breach it.
        for (int i = 0; i < 4; i++)
        {
            itemStream.removeFirstMatchingItem(null, transaction);
        }
        assertEquals(6, listStatistics.getTotalItemCount());
        assertEquals(2, itemStream.getWatermarkBreachedCount());

        // remove one more to trigger watermark breach
        itemStream.removeFirstMatchingItem(null, transaction);
        assertEquals(5, listStatistics.getTotalItemCount());
        assertEquals(3, itemStream.getWatermarkBreachedCount());

        // toggle around limit
        itemStream.addItem(new Item(itemSize), transaction);
        assertEquals(6, listStatistics.getTotalItemCount());
        assertEquals(3, itemStream.getWatermarkBreachedCount());
        itemStream.removeFirstMatchingItem(null, transaction);
        assertEquals(5, listStatistics.getTotalItemCount());
        assertEquals(4, itemStream.getWatermarkBreachedCount());

        itemStream.empty();
        itemStream.remove(transaction, itemStream.getLockID());
    }
}

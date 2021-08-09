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
package com.ibm.ws.sib.msgstore.test;

/*
 * Change activity:
 * 
 * Reason          Date    Origin    Description
 * ------------  -------- --------- -------------------------------------------
 * F 170900      11/07/03  corrigk  Original
 * 258179        06/04/05  schofiel Indoubt transaction reference counts
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.InvalidAddOperation;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemReference;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.ReferenceConsistencyViolation;
import com.ibm.ws.sib.msgstore.ReferenceStream;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;

/**
 * These tests are designed to ensure that the basic operations on ItemReferences and
 * ReferenceStreams are working as designed. Note that BEFORE a reference can be added
 * to a reference stream, the referred-to item must have been added to an ItemStream AND
 * the reference stream must also have been added to the SAME ItemStream.
 * 
 * ----------------------- Summary ----------------------------
 * 01 Add non-persistent items to streams and references to reference streams
 * 02 Add persistent items to streams and references to reference streams
 * 03 Verify that item reference count drops to zero correctly
 * 04 Attempt to add items and their references to different streams
 * 05 Attempt to add items and their references under different (new) transactions
 * 06 Attempt to add items and their references under different (begin/commit) transactions
 * 07 Extract references from a stream which was itself obtained from a ref
 * 08 Test that incorrect sequences of adding items/refs/refstream are reported correctly
 * 
 * @author corrigk
 */
public class ReferencesTest extends MessageStoreTestCase {
    public ReferencesTest(String name) {
        super(name);
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        /*
         * TestReferences test = new TestReferences("testAPersistentItemRefs");
         * test.setPersistence(persistence);
         * suite.addTest(test);
         * 
         * test = new TestReferences("testItemRefCount");
         * test.setPersistence(persistence);
         * suite.addTest(test);
         */

        ReferencesTest test = new ReferencesTest("testNonPersistentItemRefs");
        test.setPersistence(persistence);
        suite.addTest(test);

        /*
         * test = new TestReferences("testRefsFromStreamFromRef");
         * test.setPersistence(persistence);
         * suite.addTest(test);
         */

        test = new ReferencesTest("testRefsInOtherStream");
        test.setPersistence(persistence);
        suite.addTest(test);

        test = new ReferencesTest("testRefsInOtherTransaction");
        test.setPersistence(persistence);
        suite.addTest(test);

        test = new ReferencesTest("testRefsInOtherTransaction2");
        test.setPersistence(persistence);
        suite.addTest(test);

        test = new ReferencesTest("testSequences");
        test.setPersistence(persistence);
        suite.addTest(test);

        /*
         * test = new TestReferences("testXAPersistentItemRefs");
         * test.setPersistence(persistence);
         * suite.addTest(test);
         */

        return suite;
    }

    //----------------------------------------------------------------------------
    // 02 - Test persistent ItemReferences. Must use local transaction for this test
    // because items and their references must be put under the same transaction.
    //----------------------------------------------------------------------------
    /*
     * public void testAPersistentItemRefs() {
     * // Create some items
     * PersistentItem item1 = new PersistentItem();
     * PersistentItem item2 = new PersistentItem();
     * 
     * MessageStore ms = null;
     * try {
     * print("PersistenceManager used: " + PERSISTENCE);
     * ms = createAndStartMessageStore(true, PERSISTENCE);
     * ExternalLocalTransaction localtran = ms.getTransactionFactory().createLocalTransaction();
     * 
     * PersistentItemStream is = new PersistentItemStream();
     * PersistentRefStream rs = new PersistentRefStream();
     * 
     * // Add the stream to the store
     * ms.add(is, localtran);
     * localtran.commit();
     * localtran = ms.getTransactionFactory().createLocalTransaction();
     * 
     * // Add the ref stream to the item stream
     * is.addReferenceStream(rs, localtran);
     * localtran.commit();
     * localtran = ms.getTransactionFactory().createLocalTransaction();
     * 
     * // Add several items to the stream
     * is.addItem(item1, localtran);
     * is.addItem(item2, localtran);
     * 
     * // Create the references
     * PersistentRef ref1 = new PersistentRef(item1);
     * PersistentRef ref2 = new PersistentRef(item2);
     * PersistentRef ref3 = new PersistentRef(item1);
     * 
     * // Add the refs to the ref stream
     * rs.add(ref1, localtran);
     * rs.add(ref2, localtran);
     * rs.add(ref3, localtran);
     * 
     * assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());
     * assertEquals("Ref stream size incorrect", 3, rs.getStatistics().getTotalItemCount());
     * 
     * localtran.commit();
     * 
     * // Stop and restart the message store
     * stopMessageStore(ms);
     * ms = createAndStartMessageStore(false, PERSISTENCE);
     * 
     * localtran = ms.getTransactionFactory().createLocalTransaction();
     * 
     * // Get the ItemStream
     * is = (PersistentItemStream) ms.findFirstMatching(filter);
     * assertNotNull("ItemStream not found", is);
     * 
     * assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());
     * 
     * // Restore the reference stream
     * rs = (PersistentRefStream) is.findFirstMatchingReferenceStream(filter);
     * 
     * // Restore the references
     * PersistentRef ref1a = (PersistentRef) rs.removeFirstMatching(filter, localtran);
     * PersistentRef ref1b = (PersistentRef) rs.removeFirstMatching(filter, localtran);
     * PersistentRef ref1c = (PersistentRef) rs.removeFirstMatching(filter, localtran);
     * 
     * // Get the items via the refs
     * PersistentItem item3a = (PersistentItem) ref1a.getReferredItem();
     * PersistentItem item3b = (PersistentItem) ref1b.getReferredItem();
     * PersistentItem item3c = (PersistentItem) ref1c.getReferredItem();
     * 
     * localtran.commit();
     * localtran = ms.getTransactionFactory().createLocalTransaction();
     * 
     * // Restore the items
     * PersistentItem item3 = (PersistentItem) is.removeFirstMatchingItem(filterItem, localtran);
     * PersistentItem item4 = (PersistentItem) is.removeFirstMatchingItem(filterItem, localtran);
     * 
     * assertEquivalent(item1, item3);
     * assertEquivalent(item2, item4);
     * 
     * assertEquivalent(item3a, item3);
     * assertEquivalent(item3b, item4);
     * assertEquivalent(item3c, item4);
     * 
     * localtran.commit();
     * 
     * // The ref stream should be empty but the item stream should
     * // still have one item in it (the ref stream) as we did not remove it.
     * assertEquals("Stream size incorrect", 1, is.getStatistics().getTotalItemCount());
     * assertEquals("Stream size incorrect", 0, rs.getStatistics().getTotalItemCount());
     * } catch (Exception e) {
     * e.printStackTrace();
     * fail(e.toString());
     * } finally {
     * try {
     * if (ms != null) {
     * stopMessageStore(ms);
     * }
     * } catch (Exception e) {
     * fail(e.toString());
     * }
     * }
     * }
     * 
     * //----------------------------------------------------------------------------
     * // 03 - Test ItemReference count. Check that the item ref count drops to zero
     * // at the correct time.
     * //----------------------------------------------------------------------------
     * public void testItemRefCount() {
     * 
     * MessageStore ms = null;
     * 
     * try {
     * print("PersistenceManager used: " + PERSISTENCE);
     * ms = createAndStartMessageStore(true, PERSISTENCE);
     * ExternalLocalTransaction localtran = ms.getTransactionFactory().createLocalTransaction();
     * 
     * PersistentItemStream is = new PersistentItemStream();
     * PersistentRefStream rs = new PersistentRefStream();
     * 
     * PersistentItem item1 = new PersistentItem();
     * PersistentItem item2 = new PersistentItem();
     * 
     * ms.add(is, localtran);
     * localtran.commit();
     * localtran = ms.getTransactionFactory().createLocalTransaction();
     * 
     * // Add the ref stream to the item stream
     * is.addReferenceStream(rs, localtran);
     * localtran.commit();
     * localtran = ms.getTransactionFactory().createLocalTransaction();
     * 
     * // Clear the flag which has been added to the PersistentItem object. This
     * // flag gets set true when the itemReferencesDroppedToZero() method is called.
     * item1.itemRefsZero = false;
     * item2.itemRefsZero = false;
     * 
     * // Add several items to the item stream
     * is.addItem(item1, localtran);
     * is.addItem(item2, localtran);
     * 
     * // Create the references
     * PersistentRef r1 = new PersistentRef(item1);
     * PersistentRef r2 = new PersistentRef(item2);
     * PersistentRef r3 = new PersistentRef(item2); // Note 2nd ref to item4
     * 
     * // Add the refs to the ref stream
     * rs.add(r1, localtran);
     * rs.add(r2, localtran);
     * rs.add(r3, localtran);
     * 
     * // Check both stream sizes
     * assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());
     * assertEquals("Stream size incorrect", 3, rs.getStatistics().getTotalItemCount());
     * 
     * // At this point, the items' ref counts should be non-zero. Our itemsRefZero flag should be false.
     * assertEquals("Item ref count not zero", false, item1.itemRefsZero);
     * assertEquals("Item ref count not zero", false, item2.itemRefsZero);
     * 
     * // Now we commit the transaction which should NOT trigger the itemReferencesDroppedToZero()
     * // method. Our itemRefsZero flag should remain false.
     * localtran.commit();
     * 
     * assertEquals("Item ref count not zero", false, item1.itemRefsZero);
     * assertEquals("Item ref count not zero", false, item2.itemRefsZero);
     * 
     * // Stop and restart the message store
     * stopMessageStore(ms);
     * ms = createAndStartMessageStore(false, PERSISTENCE);
     * 
     * // Now, we start another transaction and get the items and their references out.
     * // The item ref count should NOT drop to zero while this is done.
     * localtran = ms.getTransactionFactory().createLocalTransaction();
     * 
     * // Get the ItemStream
     * is = (PersistentItemStream) ms.findFirstMatching(filter);
     * assertNotNull("ItemStream not found", is);
     * 
     * rs = (PersistentRefStream) is.findFirstMatchingReferenceStream(filter);
     * assertNotNull("RefStream not found", rs);
     * 
     * r1 = (PersistentRef) rs.removeFirstMatching(filter, localtran);
     * assertNotNull("Ref not found", r1);
     * assertEquals("Item ref count dropped to zero", false, item1.itemRefsZero);
     * assertEquals("Item ref count dropped to zero", false, item2.itemRefsZero);
     * 
     * r2 = (PersistentRef) rs.removeFirstMatching(filter, localtran);
     * assertNotNull("Ref not found", r2);
     * assertEquals("Item ref count dropped to zero", false, item1.itemRefsZero);
     * assertEquals("Item ref count dropped to zero", false, item2.itemRefsZero);
     * 
     * r3 = (PersistentRef) rs.removeFirstMatching(filter, localtran);
     * assertNotNull("Ref not found", r3);
     * assertEquals("Item ref count dropped to zero", false, item1.itemRefsZero);
     * assertEquals("Item ref count dropped to zero", false, item2.itemRefsZero);
     * 
     * item1 = (PersistentItem) is.findFirstMatchingItem(filterItem);
     * 
     * assertNotNull("Item not found", item1);
     * assertEquals("Item ref count dropped to zero", false, item1.itemRefsZero);
     * assertEquals("Item ref count dropped to zero", false, item2.itemRefsZero);
     * 
     * localtran.commit();
     * 
     * // Only now that we commit, does the item ref count drop to zero.
     * assertEquals("Item ref count not zero", true, item1.itemRefsZero);
     * 
     * localtran = ms.getTransactionFactory().createLocalTransaction();
     * 
     * item1 = (PersistentItem) is.removeFirstMatchingItem(filterItem, localtran);
     * assertNotNull("Item not found", item1);
     * 
     * item2 = (PersistentItem) is.removeFirstMatchingItem(filterItem, localtran);
     * assertNotNull("Item not found", item2);
     * 
     * assertEquals("Item ref count not zero", true, item1.itemRefsZero);
     * assertEquals("Item ref count not zero", true, item2.itemRefsZero);
     * 
     * localtran.commit();
     * 
     * } catch (Exception e) {
     * e.printStackTrace();
     * fail(e.toString());
     * } finally {
     * try {
     * stopMessageStore(ms);
     * } catch (Exception e) {
     * fail(e.toString());
     * }
     * }
     * }
     */

    //----------------------------------------------------------------------------
    // 01 - Test non-persistent item references. Must use local transaction for this test
    // because items and their references must be put under the same transaction.
    //----------------------------------------------------------------------------
    public void testNonPersistentItemRefs() {

        // Create some items
        Item item1 = new Item();
        Item item2 = new Item();

        MessageStore ms = null;
        try {
            print("PersistenceManager used: " + PERSISTENCE);
            ms = createAndStartMessageStore(true, PERSISTENCE);
            ExternalLocalTransaction localtran = ms.getTransactionFactory().createLocalTransaction();

            ItemStream is = new ItemStream();
            ReferenceStream rs = new ReferenceStream();

            // Add the stream to the store
            ms.add(is, localtran);
            localtran.commit();
            localtran = ms.getTransactionFactory().createLocalTransaction();

            // Add the ref stream to the item stream
            is.addReferenceStream(rs, localtran);
            localtran.commit();
            localtran = ms.getTransactionFactory().createLocalTransaction();

            // Add several items to the stream
            is.addItem(item1, localtran);
            is.addItem(item2, localtran);

            // Add their references
            ItemReference ref1 = new ItemReference(item1);
            ItemReference ref2 = new ItemReference(item2);
            ItemReference ref3 = new ItemReference(item1);

            rs.add(ref1, localtran);
            rs.add(ref2, localtran);
            rs.add(ref3, localtran);

            assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());
            assertEquals("Ref stream size incorrect", 3, rs.getStatistics().getTotalItemCount());

            localtran.commit();
            localtran = ms.getTransactionFactory().createLocalTransaction();
            // Get the ref stream back from the item stream

            // Get the refs back from the ref stream
            ItemReference ref1a = rs.removeFirstMatching(filter, localtran);
            ItemReference ref2a = rs.removeFirstMatching(filter, localtran);
            ItemReference ref3a = rs.removeFirstMatching(filter, localtran);

            // Obtain the items via the refs
            Item item1b = ref1a.getReferredItem();
            Item item2b = ref2a.getReferredItem();
            Item item3b = ref3a.getReferredItem();

            localtran.commit();
            localtran = ms.getTransactionFactory().createLocalTransaction();

            // Get the items back from the item stream
            Item item1a = is.removeFirstMatchingItem(filterItem, localtran);
            Item item2a = is.removeFirstMatchingItem(filterItem, localtran);
            localtran.commit();

            // Compare the original items with those got back from the stream
            // and those obtained via the refs from the ref stream		
            assertEquivalent(item1, item1a);
            assertEquivalent(item2, item2a);
            assertEquivalent(item1, item1b);
            assertEquivalent(item2, item2b);
            assertEquivalent(item1, item3b);

            assertEquals("Item stream should contain only reference stream", 1, is.getStatistics().getTotalItemCount());
            assertEquals("Reference Stream not empty", 0, rs.getStatistics().getTotalItemCount());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        } finally {
            try {
                if (ms != null) {
                    stopMessageStore(ms);
                }
            } catch (Exception e) {
                fail(e.toString());
            }
        }
    }

    //----------------------------------------------------------------------------
    // 07 - Test extracting references from a stream which was itself obtained from 
    // a reference.
    //----------------------------------------------------------------------------
    /*
     * public void testRefsFromStreamFromRef() {
     * 
     * MessageStore ms = null;
     * 
     * try {
     * print("PersistenceManager used: " + PERSISTENCE);
     * ms = createAndStartMessageStore(true, PERSISTENCE);
     * ExternalLocalTransaction localtran = ms.getTransactionFactory().createLocalTransaction();
     * 
     * PersistentItemStream is = new PersistentItemStream();
     * PersistentRefStream rs = new PersistentRefStream();
     * 
     * PersistentItem item1 = new PersistentItem();
     * PersistentItem item2 = new PersistentItem();
     * 
     * // Add item stream to the store
     * ms.add(is, localtran);
     * localtran.commit();
     * localtran = ms.getTransactionFactory().createLocalTransaction();
     * 
     * // Add the ref stream to item stream
     * is.addReferenceStream(rs, localtran);
     * localtran.commit();
     * localtran = ms.getTransactionFactory().createLocalTransaction();
     * 
     * // Add items to stream
     * is.addItem(item1, localtran);
     * is.addItem(item2, localtran);
     * 
     * // Create the references, must do this after the items have been added to the stream
     * PersistentRef r1 = new PersistentRef(item1);
     * PersistentRef r2 = new PersistentRef(item2);
     * 
     * // Add the refs to the ref stream
     * rs.add(r1, localtran);
     * rs.add(r2, localtran);
     * 
     * localtran.commit();
     * 
     * assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());
     * assertEquals("Stream size incorrect", 2, rs.getStatistics().getTotalItemCount());
     * 
     * stopMessageStore(ms);
     * ms = createAndStartMessageStore(false, PERSISTENCE);
     * 
     * ExternalLocalTransaction localtran2 = ms.getTransactionFactory().createLocalTransaction();
     * 
     * PersistentItemStream is2 = new PersistentItemStream();
     * PersistentRefStream rs2 = new PersistentRefStream();
     * 
     * // Get the item stream from the store
     * is2 = (PersistentItemStream) ms.findFirstMatching(filter);
     * assertNotNull("Item stream is null", is2);
     * 
     * // Get the ref stream from the item stream
     * rs2 = (PersistentRefStream) is2.findFirstMatchingReferenceStream(filter);
     * assertNotNull("Ref stream is null", rs2);
     * 
     * // Get the first ref from the ref stream
     * PersistentRef r1a = (PersistentRef) rs2.findFirstMatching(filter);
     * assertNotNull("Ref is null", r1a);
     * 
     * // Now, get the ref stream (again) from the ref
     * PersistentRefStream rs3 = (PersistentRefStream) r1a.getReferenceStream();
     * assertNotNull("Ref stream is null", rs3);
     * 
     * // Extract any remaining refs from the ref stream
     * int count = 0;
     * PersistentRef r9[] = new PersistentRef[99];
     * while ((r9[count] = (PersistentRef) rs3.removeFirstMatching(filter, localtran2)) != null) {
     * count++;
     * }
     * assertEquals("Incorrect number of refs remaining", 2, count);
     * 
     * } catch (Exception e) {
     * e.printStackTrace();
     * fail(e.toString());
     * } finally {
     * try {
     * stopMessageStore(ms);
     * } catch (Exception e) {
     * fail(e.toString());
     * }
     * }
     * }
     */

    //----------------------------------------------------------------------------
    // 04 - Test adding references to a stream other than the one containing the
    // referred-to items.
    //----------------------------------------------------------------------------
    public void testRefsInOtherStream() {

        MessageStore ms = null;

        try {
            print("PersistenceManager used: " + PERSISTENCE);
            ms = createAndStartMessageStore(true, PERSISTENCE);
            ExternalLocalTransaction localtran = ms.getTransactionFactory().createLocalTransaction();

            PersistentItemStream is1 = new PersistentItemStream();
            PersistentItemStream is2 = new PersistentItemStream();
            PersistentRefStream rs = new PersistentRefStream();

            PersistentItem item1 = new PersistentItem();
            PersistentItem item2 = new PersistentItem();

            // Add both item streams to the store
            ms.add(is1, localtran);
            ms.add(is2, localtran);
            localtran.commit();
            localtran = ms.getTransactionFactory().createLocalTransaction();

            // Add the ref stream to item stream (1)
            is1.addReferenceStream(rs, localtran);
            localtran.commit();
            localtran = ms.getTransactionFactory().createLocalTransaction();

            // Add an item to each item stream
            is1.addItem(item1, localtran);
            is2.addItem(item2, localtran);

            // Create the references, must do this after the items have been added to the stream
            PersistentRef r1 = new PersistentRef(item1);
            PersistentRef r2 = new PersistentRef(item2);

            // Now attempt to add the refs to the ref stream. Because the ref stream is
            // contained within item stream (1), it should be possible to add the
            // ref to item1 (which is also contained in item stream (1). It should NOT
            // be possible to add the ref to item2 because that is contained in item stream (2). 
            rs.add(r1, localtran);
            try {
                rs.add(r2, localtran); // This should raise exception
                //assertTrue("Should never get here", false);
            } catch (ReferenceConsistencyViolation e) {
                // expected exception
            }

            localtran.commit();

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        } finally {
            try {
                stopMessageStore(ms);
            } catch (Exception e) {
                fail(e.toString());
            }
        }
    }

    //----------------------------------------------------------------------------
    // 05 - Test adding references to a stream in a different transaction to the one
    // containing the referred-to items.
    //----------------------------------------------------------------------------
    public void testRefsInOtherTransaction() {

        MessageStore ms = null;

        try {
            print("PersistenceManager used: " + PERSISTENCE);
            ms = createAndStartMessageStore(true, PERSISTENCE);
            ExternalLocalTransaction localtran1 = ms.getTransactionFactory().createLocalTransaction();

            ExternalLocalTransaction localtran2 = ms.getTransactionFactory().createLocalTransaction();

            PersistentItemStream is = new PersistentItemStream();
            PersistentRefStream rs = new PersistentRefStream();

            PersistentItem item1 = new PersistentItem();
            PersistentItem item2 = new PersistentItem();

            // Add item stream to the store
            ms.add(is, localtran1);
            localtran1.commit();
            localtran1 = ms.getTransactionFactory().createLocalTransaction();

            // Add the ref stream to item stream 
            is.addReferenceStream(rs, localtran1);
            localtran1.commit();
            localtran1 = ms.getTransactionFactory().createLocalTransaction();

            // Add items to stream
            is.addItem(item1, localtran1);
            is.addItem(item2, localtran1);

            // Create the references, must do this after the items have been added to the stream
            PersistentRef r1 = new PersistentRef(item1);
            PersistentRef r2 = new PersistentRef(item2);

            // Now attempt to add the refs to the ref stream. We will add the first ref under the
            // same transaction as that used to add the items. The second ref is added under
            // a different transaction - this should fail.
            rs.add(r1, localtran1);
            try {
                rs.add(r2, localtran2); // This should raise exception
                // this is now allowed
                //assertTrue("Should never get here", false);
            } catch (ReferenceConsistencyViolation e) {
                // expected exception
            }

            localtran1.commit();
            localtran2.commit();

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        } finally {
            try {
                stopMessageStore(ms);
            } catch (Exception e) {
                fail(e.toString());
            }
        }
    }

    //----------------------------------------------------------------------------
    // 06 - Test adding references to a stream in a different transaction to the one
    // containing the referred-to items. This is mostly the same as 05 except that
    // we use two begin/commit(s) of the same tran instead of two separate trans.
    //----------------------------------------------------------------------------
    public void testRefsInOtherTransaction2() {

        MessageStore ms = null;

        try {
            print("PersistenceManager used: " + PERSISTENCE);
            ms = createAndStartMessageStore(true, PERSISTENCE);
            ExternalLocalTransaction localtran = ms.getTransactionFactory().createLocalTransaction();

            PersistentItemStream is = new PersistentItemStream();
            PersistentRefStream rs = new PersistentRefStream();

            PersistentItem item1 = new PersistentItem();
            PersistentItem item2 = new PersistentItem();

            // Add item stream to the store
            ms.add(is, localtran);
            localtran.commit();
            localtran = ms.getTransactionFactory().createLocalTransaction();

            // Add the ref stream to item stream 
            is.addReferenceStream(rs, localtran);
            localtran.commit();
            localtran = ms.getTransactionFactory().createLocalTransaction();

            // Add items to stream
            is.addItem(item1, localtran);
            is.addItem(item2, localtran);

            // Create the references, must do this after the items have been added to the stream
            PersistentRef r1 = new PersistentRef(item1);
            PersistentRef r2 = new PersistentRef(item2);

            // Now attempt to add the refs to the ref stream. We will add the first ref under the
            // same transaction as that used to add the items. The second ref is added under
            // a different transaction - this should fail.
            rs.add(r1, localtran);

            // Now commit and restart the transaction
            localtran.commit();

            localtran = ms.getTransactionFactory().createLocalTransaction();

            try {
                rs.add(r2, localtran); // This should raise exception
                // this is now allowed
                //assertTrue("Should never get here", false);
            } catch (ReferenceConsistencyViolation e) {
                // expected exception
            }

            localtran.commit();

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        } finally {
            try {
                stopMessageStore(ms);
            } catch (Exception e) {
                fail(e.toString());
            }
        }
    }

    //----------------------------------------------------------------------------
    // 08 - Test that incorrect sequences of adding items/refs/refstreams are
    // reported correctly. Sequences tested:-
    //           08A - 1:Add items to item stream
    //                 2:Add refs to ref stream
    //                 3:Add ref stream to item stream
    //           08B - 1:Add refs to ref stream  
    //                 2:Add items to item stream
    //                 3:Add ref stream to item stream
    //           08C - 1:Add refs to ref stream
    //                 2:Add ref stream to item stream
    //                 3:Add items to item stream
    //           08D - 1:Add ref stream to item stream
    //                 2:Add refs to ref stream
    //                 3:Add items to item stream     	
    //----------------------------------------------------------------------------
    public void testSequences() {

        MessageStore ms = null;
        PersistentItemStream is = null;
        PersistentRefStream rs = null;

        try {
            print("PersistenceManager used: " + PERSISTENCE);
            ms = createAndStartMessageStore(true, PERSISTENCE);
            ExternalLocalTransaction localtran = ms.getTransactionFactory().createLocalTransaction();

            is = new PersistentItemStream();
            rs = new PersistentRefStream();

            PersistentItem item1 = new PersistentItem();
            PersistentItem item2 = new PersistentItem();

            // Sequence 08A

            // Add item stream to the store
            ms.add(is, localtran);
            localtran.commit();
            localtran = ms.getTransactionFactory().createLocalTransaction();

            // Add items to stream
            is.addItem(item1, localtran);
            is.addItem(item2, localtran);

            // Create the references, must do this after the items have been added to the stream
            PersistentRef r1 = new PersistentRef(item1);
            PersistentRef r2 = new PersistentRef(item2);

            try {
                // Attempt to add the refs to the refstream
                rs.add(r1, localtran);
                //assertTrue("Should never get here", false);
                rs.add(r2, localtran);
                // Add the refstream to the item stream
                is.addReferenceStream(rs, localtran);
            } catch (InvalidAddOperation e) {
                // expected exception
            }

            localtran.commit();

            // Stop and restart with an empty message store
            stopMessageStore(ms);
            ms = createAndStartMessageStore(true, PERSISTENCE);
            localtran = ms.getTransactionFactory().createLocalTransaction();

            is = new PersistentItemStream();
            rs = new PersistentRefStream();
            item1 = new PersistentItem();
            item2 = new PersistentItem();

            // Sequence 08B

            // Add item stream to the store
            ms.add(is, localtran);

            // Create the references, should do this after the items have been added to the stream
            // so this should cause an error.
            r1 = new PersistentRef(item1);
            r2 = new PersistentRef(item2);

            try {
                rs.add(r1, localtran);
                assertTrue("Should never get here", false);
                rs.add(r2, localtran);

                // Add items to stream
                is.addItem(item1, localtran);
                is.addItem(item2, localtran);

                // Add the refstream to the item stream
                is.addReferenceStream(rs, localtran);
            } catch (InvalidAddOperation e) {
                // expected exception
            }

            localtran.commit();

            // Stop and restart with an empty message store
            stopMessageStore(ms);
            ms = createAndStartMessageStore(true, PERSISTENCE);
            localtran = ms.getTransactionFactory().createLocalTransaction();

            is = new PersistentItemStream();
            rs = new PersistentRefStream();
            item1 = new PersistentItem();
            item2 = new PersistentItem();

            // Sequence 08C

            // Add item stream to the store
            ms.add(is, localtran);

            // Create the references, should do this after the items have been added to the stream
            // so this should cause an error.
            r1 = new PersistentRef(item1);
            r2 = new PersistentRef(item2);

            try {
                // Attempt to add the refs to the refstream
                rs.add(r1, localtran);
                assertTrue("Should never get here", false);
                rs.add(r2, localtran);

                // Add the refstream to the item stream
                is.addReferenceStream(rs, localtran);

                // Add items to stream
                is.addItem(item1, localtran);
                is.addItem(item2, localtran);
            } catch (InvalidAddOperation e) {
                // expected exception
            }

            localtran.commit();
            // Stop and restart with an empty message store
            stopMessageStore(ms);
            ms = createAndStartMessageStore(true, PERSISTENCE);
            localtran = ms.getTransactionFactory().createLocalTransaction();

            is = new PersistentItemStream();
            rs = new PersistentRefStream();
            item1 = new PersistentItem();
            item2 = new PersistentItem();

            // Sequence 08D

            // Add item stream to the store
            ms.add(is, localtran);
            localtran.commit();
            localtran = ms.getTransactionFactory().createLocalTransaction();

            // Add the refstream to the item stream
            is.addReferenceStream(rs, localtran);
            localtran.commit();
            localtran = ms.getTransactionFactory().createLocalTransaction();

            // Create the references, should do this after the items have been added to the stream
            // so this should cause an error.
            r1 = new PersistentRef(item1);
            r2 = new PersistentRef(item2);

            try {
                // Attempt to add the refs to the refstream
                rs.add(r1, localtran);
                assertTrue("Should never get here", false);
                rs.add(r2, localtran);

                // Add items to stream
                is.addItem(item1, localtran);
                is.addItem(item2, localtran);
            } catch (Exception e) {
                // expected exception
            }

            localtran.commit();

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        } finally {
            try {
                stopMessageStore(ms);
            } catch (Exception e) {
                fail(e.toString());
            }
        }
    }

    //----------------------------------------------------------------------------
    // 09 - Test persistent ItemReferences with an XA transaction.
    //----------------------------------------------------------------------------
    /*
     * public void testXAPersistentItemRefs() {
     * // Create some items
     * PersistentItem item1 = new PersistentItem();
     * 
     * MessageStore ms = null;
     * try {
     * print("PersistenceManager used: " + PERSISTENCE);
     * ms = createAndStartMessageStore(true, PERSISTENCE);
     * ExternalAutoCommitTransaction auto = ms.getTransactionFactory().createAutoCommitTransaction();
     * MSDelegatingXAResource global = (MSDelegatingXAResource) ms.getTransactionFactory().createXAResource();
     * NullXid xid1 = new NullXid("XID1");
     * 
     * PersistentItemStream is = new PersistentItemStream();
     * PersistentRefStream rs = new PersistentRefStream();
     * 
     * // Add the stream to the store
     * ms.add(is, auto);
     * 
     * // Add the ref stream to the item stream
     * is.addReferenceStream(rs, auto);
     * 
     * // Add an item to the stream
     * is.addItem(item1, auto);
     * 
     * global.start(xid1, XAResource.TMNOFLAGS);
     * 
     * // Create a reference
     * PersistentRef ref1 = new PersistentRef(item1);
     * PersistentRef ref2 = new PersistentRef(item1);
     * 
     * // Add the refs to the ref stream
     * rs.add(ref1, global);
     * rs.add(ref2, auto);
     * ref2.remove(global, AbstractItem.NO_LOCK_ID);
     * 
     * assertEquals("Stream size incorrect", 2, is.getStatistics().getTotalItemCount());
     * assertEquals("Ref stream size incorrect", 2, rs.getStatistics().getTotalItemCount());
     * 
     * global.end(xid1, XAResource.TMSUCCESS);
     * 
     * global.prepare(xid1);
     * 
     * // Stop and restart the message store
     * stopMessageStore(ms);
     * 
     * ms = createAndStartMessageStore(false, PERSISTENCE);
     * 
     * auto = ms.getTransactionFactory().createAutoCommitTransaction();
     * global = (MSDelegatingXAResource) ms.getTransactionFactory().createXAResource();
     * 
     * global.commit(xid1, false);
     * 
     * // Get the ItemStream
     * is = (PersistentItemStream) ms.findFirstMatching(filter);
     * assertNotNull("ItemStream not found", is);
     * 
     * assertEquals("Stream size incorrect", 2, is.getStatistics().getTotalItemCount());
     * 
     * // Restore the reference stream
     * rs = (PersistentRefStream) is.findFirstMatchingReferenceStream(filter);
     * 
     * // Restore the references
     * PersistentRef ref1a = (PersistentRef) rs.removeFirstMatching(filter, auto);
     * 
     * // Get the items via the refs
     * PersistentItem item3a = (PersistentItem) ref1a.getReferredItem();
     * 
     * // Restore the items
     * PersistentItem item3 = (PersistentItem) is.removeFirstMatchingItem(filterItem, auto);
     * 
     * assertEquivalent(item1, item3);
     * 
     * assertEquivalent(item3a, item3);
     * 
     * assertEquals("Stream size incorrect", 1, is.getStatistics().getTotalItemCount());
     * assertEquals("Stream size incorrect", 0, rs.getStatistics().getTotalItemCount());
     * } catch (Exception e) {
     * e.printStackTrace();
     * fail(e.toString());
     * } finally {
     * try {
     * if (ms != null) {
     * stopMessageStore(ms);
     * }
     * } catch (Exception e) {
     * fail(e.toString());
     * }
     * }
     * }
     */

    public static class PersistentItem extends Item {
        public boolean itemRefsZero = false;

        public PersistentItem() {
            super();
        }

        @Override
        public int getStorageStrategy() {
            return STORE_ALWAYS;
        }

        @Override
        public void itemReferencesDroppedToZero() {
            itemRefsZero = true;
        }
    }

    public static class PersistentItemStream extends ItemStream {
        public PersistentItemStream() throws MessageStoreException {
            super();
        }

        @Override
        public int getStorageStrategy() {
            return STORE_ALWAYS;
        }
    }

    public static class PersistentRef extends ItemReference {
        public PersistentRef() {
            super();
        }

        public PersistentRef(Item i) {
            super(i);
        }

        @Override
        public int getStorageStrategy() {
            return STORE_ALWAYS;
        }
    }

    public static class PersistentRefStream extends ReferenceStream {
        public PersistentRefStream() throws MessageStoreException {
            super();
        }

        @Override
        public int getStorageStrategy() {
            return STORE_ALWAYS;
        }
    }

    // A filter to return everything
    Filter filter = new Filter()
    {
        public boolean filterMatches(AbstractItem item) throws MessageStoreException {
            return true;
        }
    };

    // A filter to return only items
    Filter filterItem = new Filter()
    {
        public boolean filterMatches(AbstractItem item) throws MessageStoreException {
            return
                    (item instanceof Item)
                                    && !(item instanceof ReferenceStream)
                                    && !(item instanceof ItemStream);
                }
    };
}
